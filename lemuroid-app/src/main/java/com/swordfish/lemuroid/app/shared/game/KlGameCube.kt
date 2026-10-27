package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import android.os.Build
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import java.io.File

object KlGameCube {
    fun prepare(context: Context) {
        check(Build.VERSION.SDK_INT >= 26) { "GameCube requer Android 8 ou mais recente." }
        check(Build.SUPPORTED_ABIS.contains("arm64-v8a")) { "GameCube requer Android ARM64." }
        check(File(context.applicationInfo.nativeLibraryDir, "libdolphin_libretro_android.so").isFile) { "O núcleo Dolphin não está incluído nesta compilação." }
        val target = File(DirectoriesManager(context).getSystemDirectory(), "dolphin-emu/Sys")
        val marker = File(target, ".kl-assets-rc5")
        if (marker.exists()) return
        fun copy(source: String, destination: File) {
            val children = context.assets.list(source).orEmpty()
            if (children.isNotEmpty()) {
                check(destination.isDirectory || destination.mkdirs())
                children.forEach { copy("$source/$it", File(destination, it)) }
            } else {
                destination.parentFile?.mkdirs()
                val atomic = android.util.AtomicFile(destination)
                val stream = atomic.startWrite()
                try { context.assets.open(source).use { it.copyTo(stream) }; atomic.finishWrite(stream) }
                catch (error: Exception) { atomic.failWrite(stream); throw error }
            }
        }
        copy("dolphin-emu/Sys", target)
        check(File(target, "GC").isDirectory) { "Arquivos de sistema do Dolphin incompletos." }
        marker.writeText("rc5")
    }
}
