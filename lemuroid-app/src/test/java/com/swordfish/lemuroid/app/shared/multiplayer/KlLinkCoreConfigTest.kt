package com.swordfish.lemuroid.app.shared.multiplayer

import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import org.junit.Assert.*
import org.junit.Test

class KlLinkCoreConfigTest {
    @Test fun pokemonAndWirelessOpenWithoutRegisteredSingleplayerGpsp() {
        val gba = GameSystem.findById("gba")
        assertFalse(gba.systemCoreConfigs.any { it.coreID == CoreID.GPSP })
        for (mode in listOf("POKEMON", "WIRELESS")) {
            val config = KlLinkCoreConfig.resolve(gba, mode)
            assertEquals(CoreID.GPSP, config.coreID)
            assertEquals("libkl_gpsp_link.so", config.coreLibraryFileNameOverride)
            assertFalse(config.statesSupported)
            assertTrue(config.controllerConfigs.isNotEmpty())
            assertTrue(config.defaultSettings.none { it.key.startsWith("mgba_") })
        }
    }
    @Test fun existingCableAndSingleplayerStayOnTheirCores() {
        val gba = GameSystem.findById("gba")
        assertEquals(CoreID.MGBA, KlLinkCoreConfig.resolve(gba, "MULTIPAK").coreID)
        assertEquals("libkl_mgba_link.so", KlLinkCoreConfig.resolve(gba, "MULTIPAK").coreLibraryFileNameOverride)
        assertNull(gba.systemCoreConfigs.first().coreLibraryFileNameOverride)
        for (id in listOf("gb", "gbc")) {
            val config = KlLinkCoreConfig.resolve(GameSystem.findById(id), "POKEMON")
            assertEquals(CoreID.GAMBATTE, config.coreID)
            assertEquals("libkl_gambatte_link.so", config.coreLibraryFileNameOverride)
        }
    }
}
