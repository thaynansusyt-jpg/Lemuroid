package com.swordfish.lemuroid.app.shared.saves

import org.junit.Assert.*
import org.junit.Test

class KlSaveFormatTest {
    private val footer = "|<--Snip above here to create a raw sav by excluding this DeSmuME savedata footer:".toByteArray()
    @Test fun drasticRawBlackSaveIsUnchanged() {
        val data = ByteArray(512 * 1024) { (it % 251).toByte() }
        assertArrayEquals(data, KlSaveFormat.decode("Pokemon Black.DSV", data, "nds"))
    }
    @Test fun removesOnlyFooterAfterValidSize() {
        val raw = ByteArray(512 * 1024) { 42 }
        assertArrayEquals(raw, KlSaveFormat.decode("black.dsv", raw + footer + ByteArray(32), "nds"))
    }
    @Test fun footerLikeBytesInsideSaveDoNotTruncate() {
        val raw = ByteArray(512 * 1024)
        footer.copyInto(raw, 1024)
        assertArrayEquals(raw, KlSaveFormat.decode("black.dsv", raw, "nds"))
    }
    @Test fun rejectsStatesEmptyAndInvalidSizes() {
        for ((name, bytes) in listOf("black.dss" to ByteArray(512), "black.sav" to ByteArray(0), "black.srm" to ByteArray(12345))) {
            try { KlSaveFormat.decode(name, bytes, "nds"); fail("Accepted $name") }
            catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun rejectsOversizedSaveAndDsvForGba() {
        for ((system, bytes) in listOf("nds" to ByteArray(KlSaveFormat.MAX_BYTES + 1), "gba" to ByteArray(512))) {
            try { KlSaveFormat.decode("game.dsv", bytes, system); fail("Accepted $system") }
            catch (_: IllegalArgumentException) { }
        }
    }
}
