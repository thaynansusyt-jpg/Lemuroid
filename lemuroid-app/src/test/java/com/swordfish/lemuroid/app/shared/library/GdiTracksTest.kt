package com.swordfish.lemuroid.app.shared.library

import com.swordfish.lemuroid.lib.library.GdiTracks
import org.junit.Assert.*
import org.junit.Test

class GdiTracksTest {
    @Test fun quotedTrackNamesAndAudioTracksRemainTogether() {
        assertEquals(listOf("track 01.bin", "track02.raw", "track03.bin"), GdiTracks.parse(listOf(
            "3", "1 0 4 2352 \"track 01.bin\" 0", "2 450 0 2352 track02.raw 0", "3 45000 4 2352 track03.bin 0",
        )))
    }
    @Test fun incompleteDescriptorsAndTracksOutsideTheirDirectoryAreRejected() {
        assertNull(GdiTracks.parse(listOf("3", "1 0 4 2352 track01.bin 0")))
        assertNull(GdiTracks.parse(listOf("1", "1 0 4 2352 ../track01.bin 0")))
        assertNull(GdiTracks.parse(listOf("1", "1 0 4 2352 \"folder/track01.bin\" 0")))
        assertNull(GdiTracks.parse(listOf("1", "invalid")))
    }
}
