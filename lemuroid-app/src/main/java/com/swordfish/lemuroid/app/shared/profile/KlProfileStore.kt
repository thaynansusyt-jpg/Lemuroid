package com.swordfish.lemuroid.app.shared.profile

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** Atomic disk transactions and an OS file lock protect both Android processes. */
object KlProfileStore {
    data class Sii(val ink: String = "#243447", val shirt: String = "#0789FF", val clothes: String = "kl", val face: String = "happy")
    data class Day(val date: String, val millis: Long, val sessions: Int, val games: Map<String, Long>, val note: String)
    data class Profile(val key: String, val id: String, val name: String, val provider: String, val sii: Sii, val days: List<Day>) {
        val totalMillis get() = days.sumOf { it.millis }
        val streak get() = KlDiaryMath.streak(days.associate { it.date to it.millis }, LocalDate.now())
        val bestStreak get() = KlDiaryMath.bestStreak(days.associate { it.date to it.millis })
    }
    private val monitor = Any()

    private fun <T> transaction(context: Context, write: Boolean = false, block: (JSONObject) -> T): T = synchronized(monitor) {
        val folder = File(context.filesDir, "kl-profiles").apply { mkdirs() }
        RandomAccessFile(File(folder, "store.lock"), "rw").use { lockFile ->
            lockFile.channel.lock().use {
                val file = AtomicFile(File(folder, "profiles.json"))
                val root = if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) JSONObject(file.readFully().toString(Charsets.UTF_8)) else JSONObject()
                if (!root.has("profiles")) root.put("profiles", JSONObject())
                if (!root.has("guest")) root.put("guest", "local:" + UUID.randomUUID().toString())
                if (!root.has("current")) root.put("current", root.getString("guest"))
                val result = block(root)
                // Initialization is persisted even when the first caller requested a snapshot.
                if (write || !file.baseFile.exists()) {
                    val stream = file.startWrite()
                    try {
                        stream.write(root.toString().toByteArray(Charsets.UTF_8))
                        file.finishWrite(stream)
                    } catch (error: Throwable) {
                        file.failWrite(stream)
                        throw error
                    }
                }
                result
            }
        }
    }

    private fun data(root: JSONObject, key: String): JSONObject {
        val profiles = root.getJSONObject("profiles")
        return profiles.optJSONObject(key) ?: JSONObject().apply {
            put("name", "Jogador KL")
            put("provider", "local")
            put("days", JSONObject())
            put("sii", JSONObject())
            profiles.put(key, this)
        }
    }

    fun snapshot(context: Context): Profile = transaction(context) { root ->
        val key = root.getString("current")
        val p = data(root, key)
        val avatar = p.optJSONObject("sii") ?: JSONObject()
        val days = p.getJSONObject("days")
        Profile(key, publicId(key), p.optString("name", "Jogador KL"), p.optString("provider", "local"),
            Sii(avatar.optString("ink", "#243447"), avatar.optString("shirt", "#0789FF"), avatar.optString("clothes", "kl"), avatar.optString("face", "happy")),
            days.keys().asSequence().sortedDescending().map { date ->
                val d = days.getJSONObject(date)
                val games = d.optJSONObject("games") ?: JSONObject()
                Day(date, d.optLong("millis"), d.optInt("sessions"), games.keys().asSequence().associateWith { games.getJSONObject(it).optLong("millis") }
                    .entries.groupBy { games.getJSONObject(it.key).optString("title", "Jogo") }.mapValues { (_, entries) -> entries.sumOf { it.value } }, d.optString("note"))
            }.toList())
    }

    fun publicId(key: String): String = "KL-" + MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        .take(8).joinToString("") { "%02X".format(it.toInt() and 255) }

    fun update(context: Context, name: String, sii: Sii) = transaction(context, true) { root ->
        val p = data(root, root.getString("current"))
        p.put("name", name.trim().take(32).ifBlank { "Jogador KL" })
        p.put("sii", JSONObject().put("ink", color(sii.ink, "#243447")).put("shirt", color(sii.shirt, "#0789FF"))
            .put("clothes", sii.clothes.takeIf { it in listOf("kl", "tee", "hoodie", "sport", "sii_plus") } ?: "kl")
            .put("face", sii.face.takeIf { it in listOf("happy", "cool", "calm") } ?: "happy"))
    }

    private fun color(value: String, fallback: String) = value.takeIf { it.matches(Regex("#[0-9a-fA-F]{6}")) } ?: fallback

    fun note(context: Context, date: String, value: String) = transaction(context, true) { root ->
        LocalDate.parse(date)
        val days = data(root, root.getString("current")).getJSONObject("days")
        val day = days.optJSONObject(date) ?: JSONObject().also { days.put(date, it) }
        day.put("note", value.take(2000))
    }

    fun record(context: Context, key: String, session: String, gameId: String, title: String, endWall: Long, elapsed: Long, zone: ZoneId) = transaction(context, true) { root ->
        val days = data(root, key).getJSONObject("days")
        KlDiaryMath.split(endWall, elapsed.coerceIn(0, 3_600_000L), zone).forEach { portion ->
            val d = days.optJSONObject(portion.date) ?: JSONObject().also { days.put(portion.date, it) }
            val sessions = d.optJSONArray("recentSessions") ?: JSONArray()
            val ids = (0 until sessions.length()).map { sessions.getString(it) }.toMutableList()
            if (session !in ids) {
                d.put("sessions", d.optInt("sessions") + 1)
                ids.add(session)
                d.put("recentSessions", JSONArray(ids.takeLast(128)))
            }
            d.put("millis", d.optLong("millis") + portion.millis)
            val games = d.optJSONObject("games") ?: JSONObject().also { d.put("games", it) }
            val game = games.optJSONObject(gameId) ?: JSONObject().also { games.put(gameId, it) }
            game.put("title", title.take(160))
            game.put("millis", game.optLong("millis") + portion.millis)
        }
    }

    fun signIn(context: Context, provider: String, subject: String, name: String) = transaction(context, true) { root ->
        require(provider in setOf("google", "github") && subject.isNotBlank())
        val key = "$provider:$subject"
        val profiles = root.getJSONObject("profiles")
        if (!profiles.has(key)) {
            val guest = data(root, root.getString("guest"))
            val imported = if (!guest.optBoolean("imported")) JSONObject(guest.toString()) else JSONObject().put("days", JSONObject()).put("sii", JSONObject())
            imported.put("provider", provider).put("name", name.trim().take(32).ifBlank { "Jogador KL" })
            profiles.put(key, imported)
            guest.put("imported", true)
        }
        root.put("current", key)
    }

    fun exportCurrent(context: Context): JSONObject = transaction(context) { root ->
        val p = JSONObject(data(root, root.getString("current")).toString())
        val a = p.optJSONObject("sii") ?: JSONObject()
        p.put("sii", JSONObject().put("ink", a.optString("ink", "#243447")).put("shirt", a.optString("shirt", "#0789FF"))
            .put("clothes", a.optString("clothes", "kl")).put("face", a.optString("face", "happy")))
        p.remove("provider"); p.remove("imported"); p
    }

    fun signInKl(context: Context, id: String, cloud: JSONObject?) = transaction(context, true) { root ->
        require(id.matches(Regex("[0-9a-f]{32}")))
        val key = "kl:$id"
        val profiles = root.getJSONObject("profiles")
        val p = cloud?.let { JSONObject(it.toString()) } ?: profiles.optJSONObject(key)
            ?: JSONObject(data(root, root.getString("current")).toString())
        p.put("provider", "kl")
        profiles.put(key, p)
        root.put("current", key)
    }

    fun restoreKl(context: Context, cloud: JSONObject) = transaction(context, true) { root ->
        val key = root.getString("current")
        require(key.startsWith("kl:"))
        // Keep the previous local profile in the store before restoring remote data.
        root.getJSONObject("profiles").put("backup:$key:${System.currentTimeMillis()}", JSONObject(data(root, key).toString()))
        val p = JSONObject(cloud.toString()).put("provider", "kl")
        root.getJSONObject("profiles").put(key, p)
    }

    fun signOut(context: Context) = transaction(context, true) { it.put("current", it.getString("guest")) }
    fun eraseCurrent(context: Context) = transaction(context, true) { root ->
        root.getJSONObject("profiles").remove(root.getString("current"))
        root.put("current", root.getString("guest"))
    }
}
