package com.swordfish.lemuroid.app.mobile.feature.settings.skins

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.touchinput.deltaskin.DeltaSkinAssetLoader

@Composable
fun SkinOrientationScreen(
    title: String,
    modifier: Modifier = Modifier,
    viewModel: SkinOrientationViewModel,
) {
    val state = viewModel.uiState.collectAsState(SkinOrientationViewModel.State()).value
    val context = LocalContext.current
    val assetLoader = remember { DeltaSkinAssetLoader(context.cacheDir) }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.importSkin(uri)
        }

    LemuroidSettingsPage(modifier = modifier) {
        LemuroidCardSettingsGroup(title = { Text(text = title) }) {
            PreviewOption(
                label = stringResource(R.string.controller_skins_default),
                preview = null,
                assetLoader = assetLoader,
                selected = state.selectedId == null,
                onClick = { viewModel.setSelection(null) },
            )
            state.options.forEach { option ->
                PreviewOption(
                    label = option.name,
                    preview = option.preview,
                    assetLoader = assetLoader,
                    selected = state.selectedId == option.id,
                    onClick = { viewModel.setSelection(option.id) },
                )
            }
        }

        LemuroidCardSettingsGroup(
            title = { Text(text = stringResource(R.string.controller_skins_manage)) },
        ) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(R.string.controller_skins_import)) },
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
            )
            state.options.forEach { option ->
                LemuroidSettingsMenuLink(
                    title = { Text(text = option.name) },
                    subtitle = { Text(text = stringResource(R.string.controller_skins_delete)) },
                    action = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.controller_skins_delete),
                        )
                    },
                    onClick = { viewModel.deleteSkin(option.id) },
                )
            }
        }
    }
}

@Composable
private fun PreviewOption(
    label: String,
    preview: SkinPreview?,
    assetLoader: DeltaSkinAssetLoader,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        SelectedSkinPreview(preview = preview, assetLoader = assetLoader)
        ListItem(
            headlineContent = { Text(text = label) },
            trailingContent = {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                    )
                }
            },
        )
    }
}
