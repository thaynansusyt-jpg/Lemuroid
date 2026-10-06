package com.swordfish.lemuroid.app.shared.multiplayer

import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig

/** gpSP is a dedicated cable core, not a selectable singleplayer GBA core. */
object KlLinkCoreConfig {
    fun resolve(system: GameSystem, mode: String): SystemCoreConfig {
        require(system.id.dbname in setOf("gba", "gb", "gbc"))
        val gba = system.id.dbname == "gba"
        val pokemon = gba && mode in setOf("POKEMON", "WIRELESS")
        val base = system.systemCoreConfigs.first { it.coreID == if (gba) CoreID.MGBA else CoreID.GAMBATTE }
        return base.copy(
            coreID = if (pokemon) CoreID.GPSP else base.coreID,
            coreLibraryFileNameOverride = if (!gba) "libkl_gambatte_link.so" else if (pokemon) "libkl_gpsp_link.so" else "libkl_mgba_link.so",
            exposedSettings = if (pokemon) emptyList() else base.exposedSettings,
            exposedAdvancedSettings = if (pokemon) emptyList() else base.exposedAdvancedSettings,
            defaultSettings = if (pokemon) emptyList() else base.defaultSettings,
            statesSupported = false,
            supportsLibretroVFS = false,
        )
    }
}
