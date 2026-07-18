package com.swordfish.touchinput.deltaskin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/**
 * A static, non-interactive preview of a Delta controller skin: draws the skin's background artwork at
 * full container width, letterboxed to the skin's own aspect ratio. Unlike [DeltaSkinOverlay] it lays
 * out no touch regions and reports no game screen rect — it is purely for the settings skin picker.
 */
@Composable
fun DeltaSkinPreview(
    skinDir: File,
    representation: DeltaSkinRepresentation,
    assetLoader: DeltaSkinAssetLoader,
    modifier: Modifier = Modifier,
) {
    val mapW = representation.mappingSize.width
    val mapH = representation.mappingSize.height
    val assetName = representation.assets.preferredAsset()
    if (mapW <= 0f || mapH <= 0f || assetName == null) return

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(mapW / mapH),
    ) {
        val targetW = this.constraints.maxWidth
        val targetH = this.constraints.maxHeight
        val bitmap by
            produceState<android.graphics.Bitmap?>(null, skinDir, assetName, targetW, targetH) {
                value = assetLoader.loadBitmap(skinDir, assetName, targetW, targetH)
            }
        bitmap?.let { bmp ->
            val image = remember(bmp) { bmp.asImageBitmap() }
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
