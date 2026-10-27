package com.swordfish.lemuroid.lib.game

import android.content.Context
import android.util.AtomicFile
import java.io.File

/** Last completed launch checkpoint, also readable after the game process crashes. */
object KlLaunchTrace {
    fun record(context: Context, stage: String) {
        runCatching {
            val file = AtomicFile(File(context.filesDir, "kl-last-launch.txt"))
            val stream = file.startWrite()
            try { stream.write(stage.toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
            catch (error: Exception) { file.failWrite(stream); throw error }
        }
    }
    fun read(context: Context): String = runCatching {
        AtomicFile(File(context.filesDir, "kl-last-launch.txt")).readFully().toString(Charsets.UTF_8)
    }.getOrDefault("Sem etapa registrada")
}
