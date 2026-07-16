package com.swordfish.lemuroid.app.mobile.feature.settings.skins

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager.Orientation

@Composable
fun SystemSkinScreen(
    modifier: Modifier = Modifier,
    viewModel: SystemSkinViewModel,
) {
    val state = viewModel.uiState.collectAsState(SystemSkinViewModel.State()).value

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.importSkin(uri)
        }

    LemuroidSettingsPage(modifier = modifier) {
        OrientationPicker(
            title = stringResource(R.string.controller_skins_portrait),
            options = state.options,
            selectedId = state.portraitSelectedId,
            onSelect = { viewModel.setSelection(Orientation.PORTRAIT, it) },
        )
        OrientationPicker(
            title = stringResource(R.string.controller_skins_landscape),
            options = state.options,
            selectedId = state.landscapeSelectedId,
            onSelect = { viewModel.setSelection(Orientation.LANDSCAPE, it) },
        )

        LemuroidCardSettingsGroup(
            title = { Text(text = stringResource(R.string.controller_skins_manage)) },
        ) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(R.string.controller_skins_import)) },
                onClick = { importLauncher.launch(arrayOf("*/*")) },
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
private fun OrientationPicker(
    title: String,
    options: List<SystemSkinViewModel.SkinOption>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    LemuroidCardSettingsGroup(title = { Text(text = title) }) {
        SelectableRow(
            label = stringResource(R.string.controller_skins_default),
            selected = selectedId == null,
            onClick = { onSelect(null) },
        )
        options.forEach { option ->
            SelectableRow(
                label = option.name,
                selected = selectedId == option.id,
                onClick = { onSelect(option.id) },
            )
        }
    }
}

@Composable
private fun SelectableRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    LemuroidSettingsMenuLink(
        title = { Text(text = label) },
        action = {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                )
            }
        },
        onClick = onClick,
    )
}
