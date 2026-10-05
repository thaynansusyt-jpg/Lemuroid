package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Cheats belong to the ROM URI, independent of its displayed title or cover. */
object KlCheats {
    const val MAX_ENTRIES = 64

    data class Entry(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val code: String,
        val enabled: Boolean = false,
    )

    private fun preferences(context: Context) =
        context.getSharedPreferences("kl_gba_cheats_v1", Context.MODE_PRIVATE)

    private fun key(game: Game): String =
        MessageDigest.getInstance("SHA-256").digest(game.fileUri.toByteArray())
            .joinToString("") { "%02x".format(it) }

    /** Keep complete multiline cheats together, including their master codes. */
    fun normalizeCode(input: String, systemId: String = "gba"): String = KlCheatFormats.normalize(input, systemId)

    fun read(context: Context, game: Game): List<Entry> {
        val raw = preferences(context).getString(key(game), null) ?: return emptyList()
        val array = JSONArray(raw)
        require(array.length() <= MAX_ENTRIES) { "A lista de cheats não pôde ser lida." }
        val entries = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            Entry(
                id = item.getString("id"),
                name = item.getString("name"),
                code = normalizeCode(item.getString("code"), game.systemId),
                enabled = item.getBoolean("enabled"),
            )
        }
        require(entries.map { it.id }.distinct().size == entries.size) { "A lista de cheats não pôde ser lida." }
        return entries
    }

    suspend fun save(context: Context, game: Game, entries: List<Entry>) =
        withContext(Dispatchers.IO) {
            require(entries.size <= MAX_ENTRIES) { "Use até $MAX_ENTRIES cheats por jogo." }
            require(entries.map { it.id }.distinct().size == entries.size)
            val array = JSONArray()
            entries.forEach { entry ->
                require(entry.name.isNotBlank() && entry.name.length <= 80) { "Use um nome de até 80 caracteres." }
                array.put(
                    JSONObject()
                        .put("id", entry.id)
                        .put("name", entry.name.trim())
                        .put("code", normalizeCode(entry.code, game.systemId))
                        .put("enabled", entry.enabled),
                )
            }
            val previous = if (game.systemId == "3ds") read(context, game) else emptyList()
            if (game.systemId == "3ds") KlCitraCheats.write(context, game, entries)
            if (!preferences(context).edit().putString(key(game), array.toString()).commit()) {
                if (game.systemId == "3ds") KlCitraCheats.write(context, game, previous)
                error("Não foi possível salvar os cheats. Tente novamente.")
            }
        }

}
