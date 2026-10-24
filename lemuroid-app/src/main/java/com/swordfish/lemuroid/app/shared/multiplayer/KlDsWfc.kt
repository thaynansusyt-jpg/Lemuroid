package com.swordfish.lemuroid.app.shared.multiplayer

import android.content.Context
import com.swordfish.lemuroid.lib.core.CoreVariable
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import java.io.File

object KlDsWfc {
    const val LIBRARY = "libkl_melondsds.so"
    const val ENABLED = "kl_ds_wfc_enabled"
    fun available(context: Context) = File(context.applicationInfo.nativeLibraryDir, LIBRARY).isFile
    fun enabled(context: Context) = context.getSharedPreferences("kl_play_settings", 0).getBoolean(ENABLED, false)
    fun preservePreviousStates(context: Context, game: com.swordfish.lemuroid.lib.library.db.entity.Game) {
        com.swordfish.lemuroid.app.shared.game.GameProcessLock.withMaintenance(context) {
            val directories = com.swordfish.lemuroid.lib.storage.DirectoriesManager(context)
            val folder = File(directories.getStatesDirectory(), "melonds")
            val states = listOf(File(folder, "${game.fileName}.state"), File(folder, "${game.fileName}.state.metadata")).filter { it.exists() }
            if (states.isNotEmpty()) {
                val backup = File(directories.getSavesDirectory(), "wfc-state-backups/${java.util.UUID.randomUUID()}").apply { check(mkdirs()) }
                states.forEach { it.copyTo(File(backup, it.name)) }
                states.forEach { check(it.renameTo(File(backup, "archived-${it.name}"))) }
            }
        }
    }
    fun config(system: GameSystem) = system.systemCoreConfigs.first { it.coreID == CoreID.MELONDS }.copy(
        coreLibraryFileNameOverride = LIBRARY,
        // Online sessions always cold boot; states from the old core are incompatible.
        statesSupported = false,
        exposedSettings = emptyList(), exposedAdvancedSettings = emptyList(),
        defaultSettings = listOf(
            CoreVariable("melonds_touch_mode", "touch"),
            CoreVariable("melonds_firmware_wfc_dns", "178.62.43.212"),
            CoreVariable("melonds_render_mode", "software"),
            CoreVariable("melonds_number_of_screen_layouts", "1"),
        ),
    )
}
