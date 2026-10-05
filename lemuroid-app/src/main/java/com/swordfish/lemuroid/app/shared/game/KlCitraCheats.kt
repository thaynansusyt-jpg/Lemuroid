package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import android.util.AtomicFile
import com.swordfish.lemuroid.lib.game.GameLoader
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import com.swordfish.lemuroid.lib.storage.RomFiles
import java.io.File
import java.io.InputStream
import java.util.Locale

/** Citra's libretro cheat callbacks are empty; use the native cheat engine. */
object KlCitraCheats {
    private fun prefs(context: Context) = context.getSharedPreferences("kl_citra_cheats_v1", Context.MODE_PRIVATE)
    fun titleId(context: Context, game: Game): String = prefs(context).getString(game.fileUri, "") ?: ""
    fun validTitleId(value: String) = value.matches(Regex("[0-9A-Fa-f]{16}")) && value.any { it != '0' }

    fun storeTitleId(context: Context, game: Game, value: String) {
        val normalized = value.trim().uppercase(Locale.ROOT)
        require(validTitleId(normalized)) { "Use o Title ID do jogo: 16 dígitos de 0 a 9 e A a F." }
        val previous = titleId(context, game)
        val entries = KlCheats.read(context, game)
        require(previous.isEmpty() || previous == normalized || entries.isEmpty()) {
            "O jogo já tem cheats associados a outro Title ID. Exclua-os antes de trocar o ID."
        }
        check(prefs(context).edit().putString(game.fileUri, normalized).commit()) { "Não foi possível salvar o Title ID." }
    }

    private fun readExactly(stream: InputStream, size: Int): ByteArray {
        val result = ByteArray(size)
        var position = 0
        while (position < size) {
            val count = stream.read(result, position, size - position)
            check(count > 0) { "Cabeçalho incompleto." }
            position += count
        }
        return result
    }

    /** Read NCCH directly or the first NCSD partition; no ROM modifications. */
    fun detectTitleId(stream: InputStream): String? {
        var header = readExactly(stream, 0x200)
        fun magic(bytes: ByteArray) = String(bytes, 0x100, 4, Charsets.US_ASCII)
        if (magic(header) == "NCSD") {
            var sector = 0L
            for (i in 0..3) sector = sector or ((header[0x120 + i].toLong() and 255L) shl (8 * i))
            val offset = sector * 0x200L
            if (offset !in 0x200L..(16L * 1024 * 1024)) return null
            var remaining = offset - 0x200
            while (remaining > 0) {
                val skipped = stream.skip(remaining)
                if (skipped > 0) remaining -= skipped
                else { if (stream.read() < 0) return null; remaining-- }
            }
            header = readExactly(stream, 0x200)
        }
        if (magic(header) != "NCCH") return null
        val id = header.sliceArray(0x118 until 0x120).reversed().joinToString("") { "%02X".format(it.toInt() and 255) }
        return id.takeIf { validTitleId(it) }
    }

    /** Called after the loader has extracted/opened the ROM, before the core starts. */
    fun prepare(context: Context, data: GameLoader.GameData) {
        if (data.game.systemId != "3ds") return
        if (titleId(context, data.game).isEmpty()) {
            val detected = when (val files = data.gameFiles) {
                is RomFiles.Standard -> files.files.first().inputStream().use { runCatching { detectTitleId(it) }.getOrNull() }
                is RomFiles.Virtual -> {
                    val fd = files.files.first().fd.fileDescriptor
                    val previousOffset = Os.lseek(fd, 0, OsConstants.SEEK_CUR)
                    try {
                        Os.lseek(fd, 0, OsConstants.SEEK_SET)
                        ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.dup(fd)).use {
                            runCatching { detectTitleId(it) }.getOrNull()
                        }
                    } finally { Os.lseek(fd, previousOffset, OsConstants.SEEK_SET) }
                }
            }
            detected?.let { storeTitleId(context, data.game, it) }
        }
        val entries = KlCheats.read(context, data.game)
        if (entries.isEmpty()) return
        val location = data.coreVariables.firstOrNull { it.key == "citra_use_libretro_save_path" }?.value
        require(location == null || location == "LibRetro Default") {
            "Cheats 3DS: selecione LibRetro Default na opção de local dos saves do núcleo."
        }
        write(context, data.game, entries)
    }

    fun fileContents(entries: List<KlCheats.Entry>): String = entries.joinToString("\n\n") { entry ->
        val name = entry.name.replace(Regex("[\\r\\n\\[\\]]"), " ").trim()
        "[$name]\n" + (if (entry.enabled) "*citra_enabled\n" else "") + KlCheatFormats.normalize(entry.code, "3ds")
    } + "\n"

    fun write(context: Context, game: Game, entries: List<KlCheats.Entry>) {
        val id = titleId(context, game)
        require(validTitleId(id)) { "Informe o Title ID do jogo antes de salvar os cheats de 3DS." }
        val directory = File(DirectoriesManager(context).getSavesDirectory(), "Citra/cheats")
        check(directory.mkdirs() || directory.isDirectory) { "Não foi possível preparar a pasta de cheats 3DS." }
        val target = File(directory, "$id.txt")
        val original = File(directory, "$id.kl-original.txt")
        if (target.exists() && !prefs(context).getBoolean("managed_$id", false) && !original.exists()) {
            target.copyTo(original, overwrite = false)
        }
        val atomic = AtomicFile(target)
        val stream = atomic.startWrite()
        try {
            stream.write(fileContents(entries).toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
        prefs(context).edit().putBoolean("managed_$id", true).apply()
    }
}
