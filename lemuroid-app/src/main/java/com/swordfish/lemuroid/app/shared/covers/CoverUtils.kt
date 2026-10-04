package com.swordfish.lemuroid.app.shared.covers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.widget.ImageView
import coil.ImageLoader
import coil.disk.DiskCache
import coil.imageLoader
import coil.load
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.swordfish.lemuroid.common.drawable.TextDrawable
import com.swordfish.lemuroid.common.graphics.ColorUtils
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient

object CoverUtils {
    private val coverChanges = MutableStateFlow(0L)
    val coverRevision = coverChanges.asStateFlow()

    private fun coverKey(game: Game): String =
        MessageDigest.getInstance("SHA-256").digest(game.fileUri.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun preferences(context: Context) =
        context.getSharedPreferences("kl_custom_covers", Context.MODE_PRIVATE)

    private fun customCover(context: Context, game: Game): File? {
        val name = preferences(context).getString(coverKey(game), null) ?: return null
        return File(context.filesDir, "kl_covers/$name").takeIf { it.isFile }
    }

    fun coverModel(context: Context, game: Game): Any? =
        customCover(context, game) ?: game.coverFrontUrl

    suspend fun saveCustomCover(context: Context, game: Game, uri: Uri) =
        withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, "kl_covers")
            check(directory.isDirectory || directory.mkdirs()) { "Não foi possível criar a pasta de capas." }
            val inputFile = File.createTempFile("cover-input-", ".tmp", context.cacheDir)
            val outputFile = File(directory, UUID.randomUUID().toString() + ".png")
            var saved = false
            try {
                val input = requireNotNull(context.contentResolver.openInputStream(uri)) {
                    "Não foi possível abrir essa imagem."
                }
                input.use { source ->
                    inputFile.outputStream().use { target ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val count = source.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 25L * 1024 * 1024) { "Escolha uma imagem menor que 25 MB." }
                            target.write(buffer, 0, count)
                        }
                    }
                }
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(inputFile)) { decoder, info, _ ->
                        val scale = minOf(1.0, 1024.0 / maxOf(info.size.width, info.size.height))
                        decoder.setTargetSize(
                            maxOf(1, (info.size.width * scale).toInt()),
                            maxOf(1, (info.size.height * scale).toInt()),
                        )
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(inputFile.path, options)
                    require(options.outWidth > 0 && options.outHeight > 0) { "Essa imagem não pôde ser lida." }
                    options.inSampleSize = 1
                    while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 1024) {
                        options.inSampleSize *= 2
                    }
                    options.inJustDecodeBounds = false
                    requireNotNull(BitmapFactory.decodeFile(inputFile.path, options)) {
                        "Essa imagem não pôde ser lida."
                    }
                }
                try {
                    outputFile.outputStream().use {
                        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) { "Não foi possível salvar a capa." }
                    }
                } finally {
                    bitmap.recycle()
                }
                val previous = customCover(context, game)
                check(preferences(context).edit().putString(coverKey(game), outputFile.name).commit()) {
                    "Não foi possível salvar a escolha."
                }
                saved = true
                previous?.delete()
                coverChanges.value += 1
            } finally {
                inputFile.delete()
                if (!saved) outputFile.delete()
            }
        }

    suspend fun removeCustomCover(context: Context, game: Game) =
        withContext(Dispatchers.IO) {
            val previous = customCover(context, game)
            check(preferences(context).edit().remove(coverKey(game)).commit()) {
                "Não foi possível restaurar a capa."
            }
            previous?.delete()
            coverChanges.value += 1
        }

    fun loadCover(
        game: Game,
        imageView: ImageView?,
    ) {
        if (imageView == null) return

        imageView.load(coverModel(imageView.context, game), imageView.context.imageLoader) {
            val fallbackDrawable = getFallbackDrawable(game)
            fallback(fallbackDrawable)
            error(fallbackDrawable)
        }
    }

    fun buildImageLoader(applicationContext: Context): ImageLoader {
        return ImageLoader.Builder(applicationContext)
            .diskCache(
                DiskCache.Builder()
                    .directory(applicationContext.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.20)
                    .build(),
            )
            .memoryCache {
                MemoryCache.Builder(applicationContext)
                    .maxSizePercent(0.20)
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .addNetworkInterceptor(ThrottleFailedThumbnailsInterceptor)
                    .build()
            }
            .crossfade(true)
            .interceptorDispatcher(Dispatchers.IO)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .build()
    }

    fun getFallbackDrawable(game: Game) = TextDrawable(computeTitle(game), computeColor(game))

    fun getFallbackRemoteUrl(game: Game): String {
        val color = Integer.toHexString(computeColor(game)).substring(2)
        val title = computeTitle(game)
        return "https://fakeimg.pl/512x512/$color/fff/?font=bebas&text=$title"
    }

    private fun computeTitle(game: Game): String {
        val sanitizedName =
            game.title
                .replace(Regex("\\(.*\\)"), "")

        return sanitizedName.asSequence()
            .filter { it.isDigit() or it.isUpperCase() or (it == '&') }
            .take(3)
            .joinToString("")
            .ifBlank { game.title.first().toString() }
            .capitalize()
    }

    private fun computeColor(game: Game): Int {
        return ColorUtils.randomColor(game.title)
    }
}
