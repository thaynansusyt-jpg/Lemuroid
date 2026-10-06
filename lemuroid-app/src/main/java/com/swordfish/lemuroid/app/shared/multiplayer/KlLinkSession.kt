package com.swordfish.lemuroid.app.shared.multiplayer

import android.content.Context
import android.content.Intent
import com.swordfish.lemuroid.lib.game.GameLoader
import java.io.File
import java.io.Serializable

/** Explicit Intent handoff: the room and emulation run in different processes. */
object KlLinkSession {
    const val EXTRA = "kl_gba_link_launch"
    data class Launch(val role: Int, val address: String, val key: String, val mode: String = "MULTIPAK") : Serializable
    @Volatile var launch: Launch? = null
        private set
    @Volatile var statusFile: File? = null
        private set
    val active: Boolean get() = launch != null

    fun readIntent(intent: Intent) {
        @Suppress("DEPRECATION")
        val value = intent.getSerializableExtra(EXTRA) as? Launch
        launch = value?.takeIf {
            it.mode in setOf("MULTIPAK", "POKEMON", "WIRELESS") && it.role in 0..1 && KlWifiRoom.validAddress(it.address) &&
                it.key.matches(Regex("[0-9a-f]{32}"))
        }
        statusFile = null
    }

    fun prepare(context: Context, data: GameLoader.GameData): String {
        val config = File(data.systemDirectory, "kl-link-session.cfg")
        val current = launch
        config.delete()
        if (current == null) return data.coreLibrary
        check(data.game.systemId in setOf("gba", "gb", "gbc")) { "Este cabo Wi-Fi funciona no GB, GBC e GBA." }
        val mode = current.mode
        val name = when {
            data.game.systemId != "gba" -> "libkl_gambatte_link.so"
            mode in setOf("POKEMON", "WIRELESS") -> "libkl_gpsp_link.so"
            else -> "libkl_mgba_link.so"
        }
        val core = File(context.applicationInfo.nativeLibraryDir, name)
        check(core.isFile) { "O cabo Wi-Fi precisa de um celular Android de 64 bits nesta versão." }
        // Preserve an independent pre-session SRAM backup before any emulation begins.
        data.saveRAMData?.let { bytes ->
            val folder = File(context.filesDir, "kl-link-backups").apply { mkdirs() }
            File(folder, "${data.game.id}-${System.currentTimeMillis()}.sav").writeBytes(bytes)
        }
        data.systemDirectory.mkdirs()
        statusFile = File(data.systemDirectory, "kl-link-status.txt").apply {
            writeText("Preparando cabo Wi-Fi…")
        }
        config.writeText("${current.role} ${current.address} ${current.key} ${current.mode}\n")
        return core.absolutePath
    }
}
