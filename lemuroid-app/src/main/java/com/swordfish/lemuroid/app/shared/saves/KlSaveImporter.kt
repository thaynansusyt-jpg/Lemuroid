package com.swordfish.lemuroid.app.shared.saves

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object KlSaveImporter {
    val systems = setOf("nds", "gba", "gb", "gbc", "snes", "nes")
    suspend fun importSave(context: Context, game: Game, uri: Uri): String = withContext(Dispatchers.IO) {
        require(game.systemId in systems) { "Importação de save de bateria indisponível para este console." }
        val resolver = context.contentResolver
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: error("Não foi possível identificar o arquivo.")
        val bytes = requireNotNull(resolver.openInputStream(uri)).use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(65536)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= KlSaveFormat.MAX_BYTES) { "Save grande demais." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val raw = KlSaveFormat.decode(name, bytes, game.systemId)
        GameProcessLock.withMaintenance(context) {
            val directories = DirectoriesManager(context)
            val base = game.fileName.substringBeforeLast('.')
            require(base.isNotBlank() && '/' !in base && '\\' !in base) { "Nome do jogo inválido." }
            val saves = directories.getSavesDirectory()
            val target = File(saves, "$base.srm")
            val backup = File(saves, "import-backups/${UUID.randomUUID()}").apply { check(mkdirs()) }
            // Back up all variants before changing any working data.
            val variants = listOf("srm", "sav", "dsv").map { File(saves, "$base.$it") }
            variants.filter { it.exists() }.forEach { it.copyTo(File(backup, it.name)) }
            val autos = directories.getStatesDirectory().listFiles().orEmpty()
                .filter { it.isDirectory }.flatMap { folder ->
                    listOf(File(folder, "${game.fileName}.state"), File(folder, "${game.fileName}.state.metadata"))
                }.filter { it.exists() }
            autos.forEach { it.copyTo(File(backup, "${it.parentFile!!.name}-${it.name}")) }
            // Old automatic states must not overwrite the imported battery save on Resume.
            autos.forEach { check(it.renameTo(File(backup, "archived-${it.parentFile!!.name}-${it.name}"))) }
            // The old DeSmuME migration otherwise prefers a newer .dsv over .srm.
            val oldDsv = File(saves, "$base.dsv")
            if (oldDsv.exists()) check(oldDsv.renameTo(File(backup, "archived-${oldDsv.name}")))
            val atomic = AtomicFile(target)
            val stream = atomic.startWrite()
            try { stream.write(raw); atomic.finishWrite(stream) }
            catch (error: Exception) { atomic.failWrite(stream); throw error }
            directories.syncPublicSave(target)
            "Save importado. Abra o jogo desde o início e use Continuar no próprio jogo. Backup: import-backups/${backup.name}."
        }
    }
}
