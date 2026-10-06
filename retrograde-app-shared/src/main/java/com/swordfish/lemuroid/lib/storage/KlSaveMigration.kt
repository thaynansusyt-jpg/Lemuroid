package com.swordfish.lemuroid.lib.storage

import java.io.File
import java.io.FileOutputStream

/** A completed working copy is published only after durable writes. Originals stay intact. */
object KlSaveMigration {
    fun copyMissing(source: File, destination: File) {
        if (!source.exists()) { destination.mkdirs(); return }
        if (source.isDirectory) {
            check(destination.isDirectory || destination.mkdirs()) {"Não foi possível criar a pasta dos saves."}
            for(file in source.listFiles() ?: error("Não foi possível ler os saves antigos.")) {
                copyMissing(file,File(destination,file.name))
            }
        } else if (!destination.exists()) {
            destination.parentFile?.mkdirs()
            val temporary=File.createTempFile("kl-migration-",".tmp",destination.parentFile)
            try {
                FileOutputStream(temporary).use { output -> source.inputStream().use {it.copyTo(output)}; output.fd.sync() }
                check(temporary.length()==source.length()) {"Cópia incompleta dos saves."}
                check(temporary.renameTo(destination)) {"Não foi possível concluir a migração."}
            } finally {temporary.delete()}
        }
    }
}
