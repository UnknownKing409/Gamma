package com.swordfish.lemuroid.lib.library.skin

import android.content.Context
import android.net.Uri
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import com.swordfish.touchinput.deltaskin.DeltaSkinAssetLoader
import com.swordfish.touchinput.deltaskin.DeltaSkinInfo
import com.swordfish.touchinput.deltaskin.DeltaSkinRepresentation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream

/** A skin installed in the app's skins directory. */
data class DeltaSkinHandle(
    val id: String,
    val directory: File,
    val info: DeltaSkinInfo,
)

/**
 * Manages installed Delta skins: importing (unzipping) `.deltaskin` files into the app skins folder,
 * listing them, deleting them, and resolving the representation to render for a device/orientation.
 */
class DeltaSkinManager(
    private val context: Context,
    private val directoriesManager: DirectoriesManager,
) {
    private companion object {
        const val INFO_FILE = "info.json"
        const val MACOSX_PREFIX = "__MACOSX"
    }

    suspend fun importSkin(uri: Uri): Result<DeltaSkinHandle> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tempDir = File(directoriesManager.getSkinsDirectory(), ".import_${UUID.randomUUID()}")
                try {
                    unzip(uri, tempDir)
                    val infoFile = File(tempDir, INFO_FILE)
                    require(infoFile.exists()) { "Skin is missing $INFO_FILE" }
                    val info = DeltaSkinInfo.parse(infoFile.readText())
                    require(DeltaSkinSystemMapping.systemForGameType(info.gameTypeIdentifier) != null) {
                        "Unsupported gameTypeIdentifier: ${info.gameTypeIdentifier}"
                    }

                    val id = folderNameFor(info)
                    val targetDir = File(directoriesManager.getSkinsDirectory(), id)
                    if (targetDir.exists()) targetDir.deleteRecursively()
                    check(tempDir.renameTo(targetDir)) { "Failed to move imported skin into place" }

                    DeltaSkinHandle(id, targetDir, info)
                } finally {
                    if (tempDir.exists()) tempDir.deleteRecursively()
                }
            }.onFailure { Timber.e(it, "Failed to import Delta skin") }
        }

    suspend fun listSkins(): List<DeltaSkinHandle> =
        withContext(Dispatchers.IO) {
            directoriesManager
                .getSkinsDirectory()
                .listFiles { file -> file.isDirectory && !file.name.startsWith(".") }
                ?.mapNotNull { dir -> loadHandle(dir) }
                ?.sortedBy { it.info.name.lowercase() }
                ?: emptyList()
        }

    suspend fun deleteSkin(id: String) =
        withContext(Dispatchers.IO) {
            File(directoriesManager.getSkinsDirectory(), id).deleteRecursively()
            DeltaSkinAssetLoader.evictSkin(context.cacheDir, id)
        }

    suspend fun loadHandle(id: String): DeltaSkinHandle? =
        withContext(Dispatchers.IO) {
            loadHandle(File(directoriesManager.getSkinsDirectory(), id))
        }

    private fun loadHandle(dir: File): DeltaSkinHandle? {
        val infoFile = File(dir, INFO_FILE)
        if (!infoFile.exists()) return null
        return runCatching { DeltaSkinHandle(dir.name, dir, DeltaSkinInfo.parse(infoFile.readText())) }
            .onFailure { Timber.w(it, "Skipping invalid skin at %s", dir.absolutePath) }
            .getOrNull()
    }

    /**
     * Resolves the representation to render, picking the closest device/style to this device and
     * falling back gracefully. Returns null if the skin has no representation for [orientation].
     */
    fun resolveRepresentation(
        info: DeltaSkinInfo,
        isTablet: Boolean,
        orientation: String,
    ): DeltaSkinRepresentation? {
        val devicePreference =
            if (isTablet) {
                listOf(DeltaSkinInfo.DEVICE_IPAD, DeltaSkinInfo.DEVICE_IPHONE)
            } else {
                listOf(DeltaSkinInfo.DEVICE_IPHONE, DeltaSkinInfo.DEVICE_IPAD)
            }
        val stylePreference =
            if (isTablet) {
                listOf(DeltaSkinInfo.STYLE_STANDARD, DeltaSkinInfo.STYLE_SPLIT_VIEW, DeltaSkinInfo.STYLE_EDGE_TO_EDGE)
            } else {
                listOf(DeltaSkinInfo.STYLE_EDGE_TO_EDGE, DeltaSkinInfo.STYLE_STANDARD, DeltaSkinInfo.STYLE_SPLIT_VIEW)
            }

        for (device in devicePreference) {
            val styles = info.representations[device] ?: continue
            val orderedStyles = stylePreference.filter { it in styles.keys } + (styles.keys - stylePreference.toSet())
            for (style in orderedStyles) {
                styles[style]?.get(orientation)?.let { return it }
            }
        }
        return null
    }

    private fun folderNameFor(info: DeltaSkinInfo): String {
        val base = info.identifier.ifBlank { info.name }.ifBlank { UUID.randomUUID().toString() }
        return base.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private fun unzip(
        uri: Uri,
        targetDir: File,
    ) {
        targetDir.mkdirs()
        val canonicalTarget = targetDir.canonicalPath
        val input =
            context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open skin file")
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory &&
                    !name.startsWith(
                        MACOSX_PREFIX,
                    ) &&
                    !name.substringAfterLast('/').startsWith(".")
                ) {
                    val outFile = File(targetDir, name)
                    // Guard against Zip Slip.
                    if (outFile.canonicalPath.startsWith(canonicalTarget)) {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { zip.copyTo(it) }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
}
