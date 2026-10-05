package com.swordfish.lemuroid.app.shared.profile

import android.app.Activity
import android.content.Context
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.swordfish.lemuroid.BuildConfig
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.math.BigInteger
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.RSAPublicKeySpec
import java.util.concurrent.TimeUnit

/** Tokens stay in memory for identity verification; no token is logged or written to disk. */
object KlAccountAuth {
    val googleAvailable get() = BuildConfig.KL_GOOGLE_CLIENT_ID.isNotBlank()
    val githubAvailable get() = BuildConfig.KL_GITHUB_CLIENT_ID.isNotBlank()
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS).followRedirects(false).build()

    suspend fun google(activity: Activity) {
        check(googleAvailable) { "O login Google ainda não está disponível nesta versão." }
        val nonceBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val nonce = Base64.encodeToString(nonceBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.KL_GOOGLE_CLIENT_ID).setNonce(nonce).build()
        val result = CredentialManager.create(activity).getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
        val credential = result.credential
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "O Google não retornou uma credencial válida."
        }
        val id = GoogleIdTokenCredential.createFrom(credential.data)
        withContext(Dispatchers.IO) {
            val claims = verifyGoogle(id.idToken, nonce)
            currentCoroutineContext().ensureActive()
            KlProfileStore.signIn(activity.applicationContext, "google", claims.getString("sub"), id.displayName ?: "Jogador Google")
        }
    }

    private fun decode(value: String) = Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    private fun verifyGoogle(token: String, nonce: String): JSONObject {
        val pieces = token.split('.')
        require(pieces.size == 3) { "Credencial Google inválida." }
        val header = JSONObject(decode(pieces[0]).toString(Charsets.UTF_8))
        val claims = JSONObject(decode(pieces[1]).toString(Charsets.UTF_8))
        require(header.getString("alg") == "RS256") { "Assinatura Google incompatível." }
        val certs = response(Request.Builder().url("https://www.googleapis.com/oauth2/v3/certs").get().build()).getJSONArray("keys")
        val key = (0 until certs.length()).map { certs.getJSONObject(it) }.firstOrNull { it.optString("kid") == header.getString("kid") }
            ?: error("Não foi possível confirmar a assinatura do Google.")
        require(key.getString("kty") == "RSA")
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(BigInteger(1, decode(key.getString("n"))), BigInteger(1, decode(key.getString("e")))))
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(publicKey)
        verifier.update("${pieces[0]}.${pieces[1]}".toByteArray(Charsets.US_ASCII))
        require(verifier.verify(decode(pieces[2]))) { "Assinatura Google inválida." }
        val now = System.currentTimeMillis() / 1000
        require(claims.optString("iss") in setOf("accounts.google.com", "https://accounts.google.com") &&
            claims.optString("aud") == BuildConfig.KL_GOOGLE_CLIENT_ID && claims.optString("nonce") == nonce &&
            claims.optLong("exp") > now && claims.optLong("iat") <= now+60 && claims.optString("sub").isNotBlank()) {
            "A credencial expirou ou não pertence ao KL Play. Tente novamente."
        }
        return claims
    }

    data class DevicePrompt(val code: String, val verificationUrl: String = "https://github.com/login/device")
    suspend fun github(context: Context, onPrompt: suspend (DevicePrompt?) -> Unit) {
        check(githubAvailable) { "O login GitHub ainda não está disponível nesta versão." }
        try {
            val grant = withContext(Dispatchers.IO) { post("https://github.com/login/device/code", mapOf("client_id" to BuildConfig.KL_GITHUB_CLIENT_ID)) }
            val deviceCode = grant.optString("device_code").also { check(it.isNotBlank()) { "Não foi possível iniciar o login GitHub." } }
            // Use the fixed official URL; do not open an arbitrary URL from a remote response.
            onPrompt(DevicePrompt(grant.getString("user_code")))
            var interval = grant.optInt("interval", 5).coerceAtLeast(5)
            withTimeout(grant.optInt("expires_in", 900).coerceIn(60, 900)*1000L) {
                while (true) {
                    delay(interval*1000L)
                    val token = withContext(Dispatchers.IO) { post("https://github.com/login/oauth/access_token", mapOf(
                        "client_id" to BuildConfig.KL_GITHUB_CLIENT_ID, "device_code" to deviceCode,
                        "grant_type" to "urn:ietf:params:oauth:grant-type:device_code")) }
                    when (token.optString("error")) {
                        "authorization_pending" -> continue
                        "slow_down" -> { interval = maxOf(interval+5, token.optInt("interval", interval+5)); continue }
                        "access_denied" -> error("Você cancelou a autorização no GitHub.")
                        "expired_token" -> error("O código expirou. Inicie o login novamente.")
                        "device_flow_disabled" -> error("O login GitHub ainda precisa ser ativado pelo projeto.")
                        "" -> Unit
                        else -> error("Não foi possível confirmar o login GitHub.")
                    }
                    val access = token.optString("access_token").also { check(it.isNotBlank()) { "O GitHub não confirmou o acesso." } }
                    withContext(Dispatchers.IO) {
                        val person = response(Request.Builder().url("https://api.github.com/user").header("Authorization", "Bearer $access")
                            .header("Accept", "application/vnd.github+json").header("X-GitHub-Api-Version", "2022-11-28").header("User-Agent", "KL-Play").build())
                        val id = person.getLong("id").also { require(it > 0) }
                        currentCoroutineContext().ensureActive()
                        KlProfileStore.signIn(context, "github", id.toString(), person.optString("name").takeUnless { it.isBlank() || it == "null" } ?: person.getString("login"))
                    }
                    break
                }
            }
        } finally { onPrompt(null) }
    }

    suspend fun signOut(context: Context) {
        try { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
        catch (error: kotlinx.coroutines.CancellationException) { throw error }
        catch (_: Exception) { /* Local logout remains available without a Google provider. */ }
        withContext(Dispatchers.IO) { KlProfileStore.signOut(context) }
    }
    private fun post(url: String, fields: Map<String, String>): JSONObject {
        val body = FormBody.Builder().apply { fields.forEach { (name, value) -> add(name, value) } }.build()
        return response(Request.Builder().url(url).header("Accept", "application/json").header("User-Agent", "KL-Play").post(body).build())
    }
    private fun response(request: Request): JSONObject = client.newCall(request).execute().use {
        check(it.isSuccessful) { "O serviço de login não respondeu. Tente novamente mais tarde." }
        JSONObject(it.body?.string() ?: error("Resposta de login vazia."))
    }
}
