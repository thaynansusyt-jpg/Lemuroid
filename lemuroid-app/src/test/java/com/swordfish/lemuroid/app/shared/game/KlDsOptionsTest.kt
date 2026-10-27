package com.swordfish.lemuroid.app.shared.game

import org.junit.Assert.*
import org.junit.Test

class KlDsOptionsTest {
    @Test fun defaultAndUnknownPreserveExistingOptions() {
        assertTrue(KlDsOptions.performance("CORE").isEmpty())
        assertTrue(KlDsOptions.performance("unknown").isEmpty())
    }
    @Test fun compatibilityDisablesJitAndThreading() {
        val options = KlDsOptions.performance("COMPATIBLE")
        assertEquals("disabled", options["melonds_jit_enable"])
        assertEquals("disabled", options["melonds_threaded_renderer"])
    }
    @Test fun fastUsesBoundedBlockSize() {
        assertEquals("32", KlDsOptions.performance("FAST")["melonds_jit_block_size"])
    }
}
