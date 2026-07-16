package com.swordfish.touchinput.deltaskin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Loads and rasterises Delta skin background assets (PDF or PNG) into bitmaps.
 *
 * Two caching tiers keep this fast:
 * - an in-memory [LruCache], process-wide, so an asset is decoded once per session; and
 * - a persistent PNG disk cache under [cacheDir], so the (expensive) PDF rasterisation survives across
 *   game launches. The emulator runs in its own short-lived process, so without the disk cache every
 *   launch would re-render the vector PDF from scratch.
 *
 * PDF assets are rendered with [PdfRenderer], which is not thread-safe, so all PDF work is serialised
 * behind a [Mutex].
 */
class DeltaSkinAssetLoader(private val cacheDir: File) {
    suspend fun loadBitmap(
        skinDir: File,
        assetName: String,
        targetWidthPx: Int,
        targetHeightPx: Int,
    ): Bitmap? {
        if (targetWidthPx <= 0 || targetHeightPx <= 0) return null

        val key = "${skinDir.name}/$assetName@${targetWidthPx}x$targetHeightPx"
        memoryCache.get(key)?.let { return it }

        return withContext(Dispatchers.IO) {
            val diskFile = File(diskCacheDir(), diskFileName(key))
            if (diskFile.exists()) {
                BitmapFactory.decodeFile(diskFile.absolutePath)?.let { cached ->
                    memoryCache.put(key, cached)
                    return@withContext cached
                }
            }

            val file = File(skinDir, assetName)
            if (!file.exists()) {
                Timber.w("Delta skin asset not found: %s", file.absolutePath)
                return@withContext null
            }

            val bitmap =
                runCatching {
                    if (file.extension.equals("pdf", ignoreCase = true)) {
                        renderPdf(file, targetWidthPx, targetHeightPx)
                    } else {
                        decodePng(file, targetWidthPx, targetHeightPx)
                    }
                }.onFailure { Timber.e(it, "Failed to load skin asset %s", assetName) }
                    .getOrNull()

            bitmap?.also {
                memoryCache.put(key, it)
                writeToDisk(it, diskFile)
            }
        }
    }

    private fun diskCacheDir(): File = File(cacheDir, DISK_CACHE_SUBFOLDER).apply { mkdirs() }

    private fun diskFileName(key: String): String = "${key.hashCode().toUInt().toString(16)}.png"

    private fun writeToDisk(
        bitmap: Bitmap,
        target: File,
    ) {
        runCatching {
            // Write to a temp file then rename, so a crash mid-write can't leave a corrupt cache entry.
            val tmp = File(target.parentFile, "${target.name}.tmp")
            tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            tmp.renameTo(target)
        }.onFailure { Timber.w(it, "Failed to write skin asset to disk cache") }
    }

    private fun decodePng(
        file: File,
        targetWidthPx: Int,
        targetHeightPx: Int,
    ): Bitmap? {
        val decoded = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        if (decoded.width == targetWidthPx && decoded.height == targetHeightPx) return decoded
        return Bitmap.createScaledBitmap(decoded, targetWidthPx, targetHeightPx, true).also {
            if (it !== decoded) decoded.recycle()
        }
    }

    private suspend fun renderPdf(
        file: File,
        targetWidthPx: Int,
        targetHeightPx: Int,
    ): Bitmap =
        pdfMutex.withLock {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    renderer.openPage(0).use { page ->
                        val bitmap =
                            Bitmap.createBitmap(targetWidthPx, targetHeightPx, Bitmap.Config.ARGB_8888)
                        // transform == null scales the page to fill the destination bitmap.
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        }

    companion object {
        private const val DISK_CACHE_SUBFOLDER = "skin-cache"

        private val pdfMutex = Mutex()

        // Process-wide cache so bitmaps survive recomposition and orientation changes.
        private val memoryCache: LruCache<String, Bitmap> =
            object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
                override fun sizeOf(
                    key: String,
                    value: Bitmap,
                ): Int = value.byteCount
            }
    }
}
