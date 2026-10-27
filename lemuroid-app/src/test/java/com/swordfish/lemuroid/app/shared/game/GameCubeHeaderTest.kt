package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.lib.storage.scanner.GameCubeHeader
import org.junit.Assert.*
import org.junit.Test

class GameCubeHeaderTest {
    @Test fun rvzRoutesToDolphinCore() {
        val system = com.swordfish.lemuroid.lib.library.GameSystem.findByUniqueFileExtension("RVZ")!!
        assertEquals(com.swordfish.lemuroid.lib.library.SystemID.GAMECUBE, system.id)
        assertEquals("libdolphin_libretro_android.so", system.systemCoreConfigs.first().coreID.libretroFileName)
    }

    @Test fun recognizesGameCubeDiscHeader() {
        val header = ByteArray(32)
        byteArrayOf(0xC2.toByte(), 0x33, 0x9F.toByte(), 0x3D).copyInto(header, 28)
        assertTrue(GameCubeHeader.matches(header))
        header[28] = 0
        assertFalse(GameCubeHeader.matches(header))
    }
    @Test fun rejectsShortHeadersAndOtherConsoles() {
        assertFalse(GameCubeHeader.matches(ByteArray(0)))
        assertFalse(GameCubeHeader.matches(ByteArray(31)))
        assertFalse(GameCubeHeader.matches(ByteArray(64 * 1024)))
    }
}
