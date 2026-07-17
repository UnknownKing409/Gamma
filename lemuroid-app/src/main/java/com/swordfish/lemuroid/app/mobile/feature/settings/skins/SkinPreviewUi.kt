package com.swordfish.lemuroid.app.mobile.feature.settings.skins

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.swordfish.lemuroid.R
import com.swordfish.touchinput.deltaskin.DeltaSkinAssetLoader
import com.swordfish.touchinput.deltaskin.DeltaSkinPreview

/**
 * Full-width preview of a selected skin. Renders the skin artwork when [preview] is present, otherwise
 * a neutral placeholder standing in for Lemuroid's default touch controls.
 */
@Composable
fun SelectedSkinPreview(
    preview: SkinPreview?,
    assetLoader: DeltaSkinAssetLoader,
    modifier: Modifier = Modifier,
) {
    if (preview != null) {
        DeltaSkinPreview(
            skinDir = preview.directory,
            representation = preview.representation,
            assetLoader = assetLoader,
            modifier = modifier.fillMaxWidth(),
        )
    } else {
        Surface(
            modifier =
                modifier
                    .fillMaxWidth()
                    .aspectRatio(DEFAULT_PREVIEW_ASPECT_RATIO),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.controller_skins_default),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val DEFAULT_PREVIEW_ASPECT_RATIO = 16f / 9f
