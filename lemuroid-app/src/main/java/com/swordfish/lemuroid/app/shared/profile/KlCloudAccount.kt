package com.swordfish.lemuroid.app.shared.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Public HTTPS API. Passwords are never persisted; device sessions are encrypted by Android Keystore. */
object KlCloudAccount {
    const val SITE = "https://kl-gba-play.emilysousa65477.chatgpt.site"
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS).followRedirects(false).build()
    private val monitor = Any()
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey("kl_cloud_session_v1", null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("kl_cloud_session_v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun <T> session(context: Context, block: (AtomicFile, JSONObject?) -> T): T = synchronized(monitor) {
        val folder = File(context.filesDir, "kl-cloud").apply { mkdirs() }
        RandomAccessFile(File(folder, "session.lock"), "rw").use { lock -> lock.channel.lock().use {
            val file = AtomicFile(File(folder, "session.bin"))
            val value = if (file.baseFile.exists()) {
                val bytes = file.readFully()
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
                JSONObject(cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8))
            } else null
            block(file, value)
        } }
    }
    private fun save(file: AtomicFile, value: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val bytes = cipher.iv + cipher.doFinal(value.toString().toByteArray(Charsets.UTF_8))
        val output = file.startWrite()
        try { output.write(bytes); file.finishWrite(output) } catch (e: Exception) { file.failWrite(output); throw e }
    }
    private fun api(path: String, method: String, token: String? = null, body: JSONObject? = null): JSONObject {
        val builder = Request.Builder().url("$SITE/api/kl/$path").header("Accept", "application/json").header("User-Agent", "KL-Play-Plus")
        token?.let { builder.header("Authorization", "Bearer $it") }
        builder.method(method, if (method in setOf("POST", "PUT")) (body ?: JSONObject()).toString().toRequestBody("application/json".toMediaType()) else null)
        return client.newCall(builder.build()).execute().use { r ->
            val text = r.body?.string() ?: error("O site não respondeu. Seus dados locais foram preservados.")
            val data = runCatching { JSONObject(text) }.getOrElse { error("O serviço de contas está indisponível. Tente novamente.") }
            check(r.isSuccessful) { data.optString("error", "Não foi possível acessar a conta.") }
            data
        }
    }
    fun openSite(context: Context) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$SITE/conta.html"))) }

    suspend fun login(context: Context, username: String, password: String) = withContext(Dispatchers.IO) {
        session(context) { file, _ ->
            val login = api("login", "POST", body = JSONObject().put("username", username.trim()).put("password", password.trim()))
            val cloud = api("profile", "GET", login.getString("token"))
            KlProfileStore.signInKl(context, login.getString("id"), cloud.optJSONObject("profile"))
            val state = JSONObject().put("token", login.getString("token")).put("id", login.getString("id"))
                .put("username", login.getString("username")).put("revision", cloud.getLong("revision")).put("updated", cloud.optLong("updated"))
            save(file, state)
            // First account backup only. Existing remote data is restored instead of silently overwritten.
            if (cloud.isNull("profile")) upload(context, file, state)
        }
    }
    private fun upload(context: Context, file: AtomicFile, state: JSONObject): Long {
        val current = KlProfileStore.snapshot(context)
        check(current.key == "kl:" + state.getString("id")) { "Entre na conta KL para salvar o perfil." }
        val result = api("profile", "PUT", state.getString("token"), JSONObject().put("revision", state.getLong("revision"))
            .put("profile", KlProfileStore.exportCurrent(context)))
        state.put("revision", result.getLong("revision")).put("updated", result.getLong("updated"))
        save(file, state)
        return result.getLong("updated")
    }
    suspend fun backup(context: Context): Long = withContext(Dispatchers.IO) {
        session(context) { file, state -> upload(context, file, state ?: error("Entre na conta KL novamente.")) }
    }
    suspend fun restore(context: Context) = withContext(Dispatchers.IO) {
        session(context) { file, state ->
            check(state != null) { "Entre na conta KL novamente." }
            val current = KlProfileStore.snapshot(context)
            check(current.key == "kl:" + state.getString("id")) { "Esta sessão pertence a outro perfil." }
            val cloud = api("profile", "GET", state.getString("token"))
            val profile = cloud.optJSONObject("profile") ?: error("Ainda não existe um backup.")
            KlProfileStore.restoreKl(context, profile)
            state.put("revision", cloud.getLong("revision")).put("updated", cloud.optLong("updated")); save(file, state)
        }
    }
    suspend fun lastBackup(context: Context): Long = withContext(Dispatchers.IO) { session(context) { _, state -> state?.optLong("updated") ?: 0L } }
    suspend fun logout(context: Context) = withContext(Dispatchers.IO) {
        session(context) { file, state ->
            // Local logout is possible offline; the server session expires automatically.
            if (state != null) runCatching { api("logout", "POST", state.getString("token")) }
            file.delete(); KlProfileStore.signOut(context)
        }
    }
    suspend fun eraseRemote(context: Context) = withContext(Dispatchers.IO) {
        session(context) { file, state ->
            check(state != null) { "Entre na conta KL novamente." }
            api("account", "DELETE", state.getString("token")); file.delete(); KlProfileStore.signOut(context)
        }
    }
}
