package com.swordfish.touchinput.radial

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.util.LruCache
import java.io.File
import java.security.MessageDigest

/** Fonts are private, content-addressed copies; saved skins never depend on an external URI. */
object KlFontStore {
    private const val MAX_BYTES = 4 * 1024 * 1024
    private const val MAX_FONTS = 32
    private val cache = LruCache<String, Typeface>(8)

    fun importFont(context: Context, uri: Uri): String {
        val folder = File(context.filesDir, "kl-fonts").apply { check(mkdirs() || isDirectory) }
        val temporary = File.createTempFile("import-", ".tmp", folder)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temporary.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var size = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        size += count
                        require(size <= MAX_BYTES) { "A fonte precisa ter no máximo 4 MB." }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error("Não foi possível abrir o arquivo.")
            require(temporary.length() >= 12) { "Arquivo de fonte inválido." }
            val signature = temporary.inputStream().use { stream -> ByteArray(4).also { check(stream.read(it) == 4) } }
            require(signature.contentEquals(byteArrayOf(0, 1, 0, 0)) || signature.contentEquals("OTTO".toByteArray())) {
                "Escolha uma fonte TTF ou OTF válida."
            }
            val typeface = runCatching { Typeface.createFromFile(temporary) }.getOrElse {
                error("O Android não conseguiu carregar esta fonte.")
            }
            val digest = MessageDigest.getInstance("SHA-256")
            temporary.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
            }
            val name = digest.digest().joinToString("") { "%02x".format(it) } + ".ttf"
            val target = File(folder, name)
            if (!target.exists()) {
                require((folder.listFiles()?.count { it.name.endsWith(".ttf") } ?: 0) < MAX_FONTS) {
                    "Você já importou 32 fontes neste aparelho."
                }
                check(temporary.renameTo(target)) { "Não foi possível guardar a fonte." }
            }
            val id = "FILE:${target.absolutePath}"
            cache.put(id, typeface)
            return id
        } finally { temporary.delete() }
    }

    fun typeface(id: String): Typeface? {
        cache.get(id)?.let { return it }
        if (!id.startsWith("FILE:")) return null
        val file = File(id.removePrefix("FILE:"))
        if (file.parentFile?.name != "kl-fonts" || !file.name.matches(Regex("[0-9a-f]{64}\\.ttf")) ||
            !file.isFile || file.length() !in 12..MAX_BYTES.toLong()) return null
        return runCatching { Typeface.createFromFile(file).also { cache.put(id, it) } }.getOrNull()
    }
}
