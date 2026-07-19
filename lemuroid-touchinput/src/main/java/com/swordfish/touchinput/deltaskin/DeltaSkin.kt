package com.swordfish.touchinput.deltaskin

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Data model for the Delta emulator controller skin format (`.deltaskin`).
 *
 * A `.deltaskin` is a zip containing an `info.json` (described here) plus PDF/PNG assets. Only the
 * subset of the format that Lemuroid renders is modelled; unknown fields are ignored during parsing.
 *
 * See https://noah978.gitbook.io/delta-docs/skins
 */
@Serializable
data class DeltaSkinInfo(
    val name: String = "",
    val identifier: String = "",
    val gameTypeIdentifier: String = "",
    val debug: Boolean = false,
    // device ("iphone"/"ipad") -> style ("standard"/"edgeToEdge"/"splitView") -> orientation -> representation
    val representations: Map<String, Map<String, Map<String, DeltaSkinRepresentation>>> = emptyMap(),
) {
    companion object {
        const val DEVICE_IPHONE = "iphone"
        const val DEVICE_IPAD = "ipad"

        const val STYLE_STANDARD = "standard"
        const val STYLE_EDGE_TO_EDGE = "edgeToEdge"
        const val STYLE_SPLIT_VIEW = "splitView"

        const val ORIENTATION_PORTRAIT = "portrait"
        const val ORIENTATION_LANDSCAPE = "landscape"

        private val json = Json { ignoreUnknownKeys = true }

        fun parse(content: String): DeltaSkinInfo = json.decodeFromString(serializer(), content)
    }

    /** Orientations that expose a representation for the given device/style, e.g. ["portrait", "landscape"]. */
    fun availableOrientations(
        device: String,
        style: String,
    ): Set<String> = representations[device]?.get(style)?.keys ?: emptySet()

    /** True if any representation (any device/style) provides the given orientation. */
    fun supportsOrientation(orientation: String): Boolean =
        representations.values.any { styles ->
            styles.values.any { orientations -> orientation in orientations }
        }
}

@Serializable
data class DeltaSkinRepresentation(
    val assets: DeltaSkinAssets = DeltaSkinAssets(),
    val items: List<DeltaSkinItem> = emptyList(),
    val screens: List<DeltaSkinScreen> = emptyList(),
    val mappingSize: DeltaSkinSize = DeltaSkinSize(),
    val extendedEdges: DeltaSkinEdges = DeltaSkinEdges(),
    val translucent: Boolean = false,
)

@Serializable
data class DeltaSkinAssets(
    val resizable: String? = null,
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null,
) {
    /** The best asset filename to use, preferring the resizable (PDF) then the largest PNG variant. */
    fun preferredAsset(): String? = resizable ?: large ?: medium ?: small
}

@Serializable
data class DeltaSkinItem(
    val inputs: JsonElement? = null,
    val frame: DeltaSkinFrame = DeltaSkinFrame(),
    val extendedEdges: DeltaSkinEdges = DeltaSkinEdges(),
    // Present on analog thumbstick items; distinguishes them from a directional pad.
    val thumbstickSize: DeltaSkinSize? = null,
)

@Serializable
data class DeltaSkinScreen(
    val inputFrame: DeltaSkinFrame? = null,
    val outputFrame: DeltaSkinFrame? = null,
)

@Serializable
data class DeltaSkinFrame(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
)

@Serializable
data class DeltaSkinSize(
    val width: Float = 0f,
    val height: Float = 0f,
)

@Serializable
data class DeltaSkinEdges(
    val top: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f,
    val right: Float = 0f,
)
