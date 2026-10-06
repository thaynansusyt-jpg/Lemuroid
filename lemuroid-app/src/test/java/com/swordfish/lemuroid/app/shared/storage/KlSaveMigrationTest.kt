package com.swordfish.lemuroid.app.shared.storage

import com.swordfish.lemuroid.lib.storage.KlSaveMigration
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class KlSaveMigrationTest {
    @Test fun migrationPreservesProgressAndCanResumeWithoutReplacingNewerWorkingSaves() {
        val root=Files.createTempDirectory("kl-save-migration").toFile()
        try {
            val old=File(root,"old").apply{mkdirs()};val next=File(root,"new")
            File(old,"Pokemon.srm").writeBytes(byteArrayOf(1,2,3))
            File(old,"Citra/sdmc/Nintendo 3DS/id/id/title/game/data/save").apply{parentFile.mkdirs();writeText("3ds progress")}
            KlSaveMigration.copyMissing(old,next)
            assertArrayEquals(byteArrayOf(1,2,3),File(next,"Pokemon.srm").readBytes())
            assertEquals("3ds progress",File(next,"Citra/sdmc/Nintendo 3DS/id/id/title/game/data/save").readText())
            File(next,"Pokemon.srm").writeText("new progress")
            File(old,"slot.state").writeText("slot")
            KlSaveMigration.copyMissing(old,next)
            assertEquals("new progress",File(next,"Pokemon.srm").readText())
            assertEquals("slot",File(next,"slot.state").readText())
            assertArrayEquals(byteArrayOf(1,2,3),File(old,"Pokemon.srm").readBytes())
            assertFalse(next.walkTopDown().any {it.name.startsWith("kl-migration-")})
        } finally {root.deleteRecursively()}
    }
}
