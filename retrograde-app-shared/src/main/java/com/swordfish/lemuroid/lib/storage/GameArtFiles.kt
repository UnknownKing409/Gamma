package com.swordfish.lemuroid.lib.storage

/** Helpers for custom game artwork stored as an image file alongside the game file. */
object GameArtFiles {
    /** Image extensions we recognize as custom cover art, in priority order. */
    val SUPPORTED_EXTENSIONS = listOf("png", "jpg", "jpeg", "webp")

    fun extensionForMimeType(mimeType: String?): String {
        return when (mimeType?.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    }

    fun mimeTypeForExtension(extension: String): String {
        return when (extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
    }
}
