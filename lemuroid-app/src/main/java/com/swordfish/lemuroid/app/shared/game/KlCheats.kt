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
    fun normalizeCode(input: String): String {
        require(input.length <= 8192) { "O código é grande demais. Use até 8192 caracteres." }
        val lines = input.replace("\r", "").split('\n', '+')
            .map { it.trim() }.filter { it.isNotEmpty() }
        require(lines.isNotEmpty()) { "Cole o código do cheat." }
        require(lines.size <= 128) { "Use até 128 linhas por cheat." }
        return lines.mapIndexed { index, line ->
            val compact = line.replace(Regex("\\s+"), "").uppercase(Locale.ROOT)
            require(compact.matches(Regex("[0-9A-F]{12}|[0-9A-F]{16}"))) {
                "Confira a linha ${index + 1}: use 8 dígitos + 4 ou 8 dígitos, somente de 0 a 9 e A a F."
            }
            compact.take(8) + " " + compact.drop(8)
        }.joinToString("\n")
    }

    fun read(context: Context, game: Game): List<Entry> {
        val raw = preferences(context).getString(key(game), null) ?: return emptyList()
        val array = JSONArray(raw)
        require(array.length() <= MAX_ENTRIES) { "A lista de cheats não pôde ser lida." }
        val entries = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            Entry(
                id = item.getString("id"),
                name = item.getString("name"),
                code = normalizeCode(item.getString("code")),
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
                        .put("code", normalizeCode(entry.code))
                        .put("enabled", entry.enabled),
                )
            }
            check(preferences(context).edit().putString(key(game), array.toString()).commit()) {
                "Não foi possível salvar os cheats. Tente novamente."
            }
        }

    fun coreCode(entry: Entry): String = normalizeCode(entry.code).replace("\n", "+")
}
