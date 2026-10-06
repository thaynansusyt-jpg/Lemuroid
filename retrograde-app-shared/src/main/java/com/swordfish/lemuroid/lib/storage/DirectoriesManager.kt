package com.swordfish.lemuroid.lib.storage

import android.content.Context
import java.io.File

class DirectoriesManager(private val appContext: Context) {
    fun syncPublicSave(file: File) = KlPublicSaves.syncFile(appContext, file)
    @Deprecated("Use the external states directory")
    fun getInternalStatesDirectory(): File =
        File(appContext.filesDir, "states").apply {
            mkdirs()
        }

    fun getCoresDirectory(): File =
        File(appContext.filesDir, "cores").apply {
            mkdirs()
        }

    fun getSystemDirectory(): File =
        File(appContext.filesDir, "system").apply {
            mkdirs()
        }

    fun getStatesDirectory(): File = workingDirectory("states")
    fun getStatesPreviewDirectory(): File = workingDirectory("state-previews")
    fun getSavesDirectory(): File = workingDirectory("saves")

    private fun workingDirectory(name: String): File {
        val old=File(appContext.getExternalFilesDir(null),name).apply {mkdirs()}
        if(KlPublicSaves.selected(appContext)==null)return old
        val target=File(appContext.filesDir,"kl-save-work/$name")
        val marker=File(appContext.filesDir,"kl-save-work/$name.migrated")
        if(marker.exists())return target.apply {mkdirs()}
        return runCatching {
            java.io.RandomAccessFile(File(appContext.filesDir,"kl-save-migration.lock"),"rw").channel.use { channel ->
                channel.lock().use {
                    if(!marker.exists()) {
                        KlSaveMigration.copyMissing(old,target)
                        marker.writeText("complete")
                    }
                }
            }
            target
        }.getOrElse { old } // Storage failure keeps the original working directory usable.
    }

    fun getInternalRomsDirectory(): File =
        File(appContext.getExternalFilesDir(null), "roms").apply {
            mkdirs()
        }
}
