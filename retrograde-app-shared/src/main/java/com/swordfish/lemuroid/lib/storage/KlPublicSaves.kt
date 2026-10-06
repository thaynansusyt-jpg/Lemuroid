package com.swordfish.lemuroid.lib.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AtomicFile
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID

/** User-visible SAF copies. Cores retain their File-based working files for compatibility.
 * Reload configuration under an OS lock: the library and game run in separate processes.
 * Never discard working files when the provider is unavailable or a write fails. */
object KlPublicSaves {
    private const val CONFIG = "kl-public-saves.uri"
    @Synchronized private fun <T> locked(context: Context, action: () -> T): T =
        RandomAccessFile(File(context.filesDir,"kl-public-saves.lock"),"rw").channel.use { channel ->
            channel.lock().use { action() }
        }
    private fun config(context: Context) = AtomicFile(File(context.filesDir,CONFIG))
    fun selected(context: Context): String? = runCatching { config(context).openRead().use { it.bufferedReader().readText() }.takeIf { it.isNotBlank() } }.getOrNull()
    fun status(context: Context): String = runCatching { File(context.filesDir,"kl-public-saves.status").readText() }.getOrDefault("")
    private fun root(context: Context) = selected(context)?.let { DocumentFile.fromTreeUri(context,Uri.parse(it)) }
    private fun note(context: Context, text: String) { runCatching { File(context.filesDir,"kl-public-saves.status").writeText(text) } }

    fun configure(context: Context, uri: Uri): Int = locked(context) {
        context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val folder = requireNotNull(DocumentFile.fromTreeUri(context,uri))
        require(folder.isDirectory && folder.canWrite()) { "Escolha uma pasta gravável, como Documentos/KL Play." }
        val probe = requireNotNull(folder.createFile("application/octet-stream", ".kl-test-${UUID.randomUUID()}"))
        try { check(probe.renameTo(probe.name + "-ok")) { "Essa pasta não permite renomear arquivos. Escolha uma pasta local." } } finally { probe.delete() }
        val atomic = config(context); val stream = atomic.startWrite()
        try { stream.write(uri.toString().toByteArray()); atomic.finishWrite(stream) } catch(e:Exception) {atomic.failWrite(stream);throw e}
        var imported = 0
        for ((name, directory) in categories(context)) {
            folder.findFile(name)?.let { imported += importMissing(context,it,directory,0) }
        }
        val copied = syncUnlocked(context,folder)
        note(context,"Pasta conectada • $copied arquivos copiados • $imported recuperados. Originais preservados.")
        copied
    }

    private fun categories(context: Context): Map<String,File> {
        val manager=DirectoriesManager(context)
        return mapOf("saves" to manager.getSavesDirectory(),"states" to manager.getStatesDirectory(),
            "state-previews" to manager.getStatesPreviewDirectory(),"link-backups" to File(context.filesDir,"kl-link-backups"))
    }
    private fun safeName(name: String) = name.isNotBlank() && name !in listOf(".","..") && '/' !in name && '\\' !in name && '\u0000' !in name
    private fun importMissing(context: Context, source: DocumentFile, destination: File, depth: Int): Int {
        check(depth<=16) {"Pasta muito profunda."}
        if(source.isDirectory) {
            destination.mkdirs();var count=0
            for(child in source.listFiles()) {
                val name=child.name ?: continue
                if(safeName(name) && !name.startsWith(".kl-") && !name.endsWith(".previous")) count+=importMissing(context,child,File(destination,name),depth+1)
            }
            return count
        }
        if(destination.exists() || source.length()<=0) return 0
        check(source.length()<=512L*1024*1024) {"Save muito grande."}
        destination.parentFile?.mkdirs()
        val atomic=AtomicFile(destination);val output=atomic.startWrite()
        try {
            requireNotNull(context.contentResolver.openInputStream(source.uri)).use { input ->
                val buffer=ByteArray(65536);var total=0L
                while(true) {val n=input.read(buffer);if(n<0)break;total+=n;check(total<=512L*1024*1024);output.write(buffer,0,n)}
            }
            atomic.finishWrite(output)
        } catch(e:Exception) {atomic.failWrite(output);throw e}
        return 1
    }
    private fun digest(file: File): String = file.inputStream().use { input ->
        val digest=MessageDigest.getInstance("SHA-256");val buffer=ByteArray(65536)
        while(true) {val n=input.read(buffer);if(n<0)break;digest.update(buffer,0,n)}
        digest.digest().joinToString("") {"%02x".format(it)}
    }
    private fun copy(context: Context, source: File, destination: DocumentFile): Boolean {
        val name=source.name; require(safeName(name))
        val checksum=digest(source)
        val marks=File(context.filesDir,"kl-public-saves-hashes").apply{mkdirs()}
        val marker=File(marks,MessageDigest.getInstance("SHA-256").digest((destination.uri.toString()+"/"+name).toByteArray()).joinToString(""){"%02x".format(it)})
        val old=destination.findFile(name)
        if(old!=null && old.length()==source.length() && marker.exists() && marker.readText()==checksum) return false
        // A conflicting file in a newly selected folder is preserved as .previous.
        val temporary=requireNotNull(destination.createFile("application/octet-stream",".kl-${UUID.randomUUID()}"))
        try {
            requireNotNull(context.contentResolver.openOutputStream(temporary.uri,"wt")).use { out ->source.inputStream().use { it.copyTo(out) } }
            check(temporary.length()==source.length()) {"Cópia incompleta."}
            val verified=MessageDigest.getInstance("SHA-256")
            requireNotNull(context.contentResolver.openInputStream(temporary.uri)).use { input ->
                val buffer=ByteArray(65536)
                while(true) {val n=input.read(buffer);if(n<0)break;verified.update(buffer,0,n)}
            }
            check(verified.digest().joinToString(""){"%02x".format(it)}==checksum && digest(source)==checksum) {"O save mudou durante a cópia. Será tentado novamente."}
            if(old!=null) {
                val previous=destination.findFile("$name.previous")
                if(previous!=null) check(previous.delete()) {"Não foi possível atualizar a cópia anterior."}
                check(old.renameTo("$name.previous")) {"Não foi possível preservar o save anterior."}
            }
            if(!temporary.renameTo(name)) {
                old?.renameTo(name)
                error("Não foi possível concluir a cópia. O save local foi preservado.")
            }
            marker.writeText(checksum)
        } finally { if(temporary.name?.startsWith(".kl-")==true) temporary.delete() }
        return true
    }
    private fun syncDirectory(context: Context, local: File, remote: DocumentFile, depth: Int): Int {
        check(depth<=16);var copied=0
        for(file in local.listFiles().orEmpty()) {
            if(file.isDirectory && "Citra" in file.invariantSeparatorsPath.split('/') && file.name in setOf("content","shaders","cache","load","sysdata")) continue
            if(file.name.endsWith(".new") || file.name.endsWith(".bak"))continue
            if(file.isDirectory) {
                val child=remote.findFile(file.name) ?: requireNotNull(remote.createDirectory(file.name))
                check(child.isDirectory); copied+=syncDirectory(context,file,child,depth+1)
            } else if(file.length()>0 && copy(context,file,remote))copied++
        }
        return copied
    }
    private fun syncUnlocked(context: Context, remote: DocumentFile): Int {
        var copied=0
        for((name,local) in categories(context)) {
            if(!local.exists())continue
            val directory=remote.findFile(name) ?: requireNotNull(remote.createDirectory(name))
            check(directory.isDirectory);copied+=syncDirectory(context,local,directory,0)
        }
        return copied
    }
    fun syncFile(context: Context, file: File) {
        if(selected(context)==null || !file.isFile)return
        runCatching { locked(context) {
            val folder=requireNotNull(root(context));check(folder.canWrite())
            val category=categories(context).entries.firstOrNull { file.absolutePath.startsWith(it.value.absolutePath+File.separator) } ?: return@locked
            val relative=file.relativeTo(category.value).invariantSeparatorsPath.split('/')
            var directory=folder.findFile(category.key) ?: requireNotNull(folder.createDirectory(category.key))
            for(name in relative.dropLast(1)) {require(safeName(name));directory=directory.findFile(name) ?: requireNotNull(directory.createDirectory(name))}
            copy(context,file,directory)
            note(context,"Saves copiados para a pasta escolhida. Originais preservados.")
        }}.onFailure {note(context,"Cópia pendente: ${it.message ?: "pasta indisponível"}. Saves locais preservados.")}
    }
    /** Native cores may save directly into their save directory, without SRAM. */
    fun syncNative(context: Context) {
        if(selected(context)==null)return
        runCatching { locked(context) {
            val folder=requireNotNull(root(context));check(folder.canWrite())
            val local=DirectoriesManager(context).getSavesDirectory()
            val remote=folder.findFile("saves") ?: requireNotNull(folder.createDirectory("saves"))
            syncDirectory(context,local,remote,0)
        }}.onFailure {note(context,"Cópia de saves nativos pendente: ${it.message}. Originais preservados.")}
    }
    fun sync(context: Context) {
        if(selected(context)==null)return
        runCatching { locked(context) {
            val folder=requireNotNull(root(context));check(folder.canWrite())
            syncUnlocked(context,folder);note(context,"Saves copiados para a pasta escolhida. Originais preservados.")
        }}.onFailure {note(context,"Cópia pendente: ${it.message ?: "pasta indisponível"}. Saves locais preservados.")}
    }
}
