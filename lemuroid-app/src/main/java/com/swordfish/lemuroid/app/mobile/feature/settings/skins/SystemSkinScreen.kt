package com.swordfish.lemuroid.app.mobile.feature.settings.skins

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.main.navigateToSkinOrientation
import com.swordfish.lemuroid.app.utils.android.ComposableLifecycle
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.touchinput.deltaskin.DeltaSkinAssetLoader
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager.Orientation

@Composable
fun SystemSkinScreen(
    systemId: String?,
    modifier: Modifier = Modifier,
    viewModel: SystemSkinViewModel,
    navController: NavController,
) {
    val state = viewModel.uiState.collectAsState(SystemSkinViewModel.State()).value
    val context = LocalContext.current
    val assetLoader = remember { DeltaSkinAssetLoader(context.cacheDir) }

    // Refresh when returning from the per-orientation picker so the previews reflect a new selection.
    ComposableLifecycle { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
    }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.importSkin(uri)
        }

    LemuroidSettingsPage(modifier = modifier) {
        OrientationCard(
            title = stringResource(R.string.controller_skins_portrait),
            selection = state.portrait,
            assetLoader = assetLoader,
            onClick = { systemId?.let { navController.navigateToSkinOrientation(it, Orientation.PORTRAIT) } },
        )
        OrientationCard(
            title = stringResource(R.string.controller_skins_landscape),
            selection = state.landscape,
            assetLoader = assetLoader,
            onClick = { systemId?.let { navController.navigateToSkinOrientation(it, Orientation.LANDSCAPE) } },
        )

        LemuroidCardSettingsGroup(
            title = { Text(text = stringResource(R.string.controller_skins_manage)) },
        ) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(R.string.controller_skins_import)) },
                subtitle = { Text(text = stringResource(R.string.controller_skins_import_description)) },
                onClick = { importLauncher.launch(arrayOf("*/*")) },
            )
        }
    }
}

@Composable
private fun OrientationCard(
    title: String,
    selection: SystemSkinViewModel.OrientationSelection,
    assetLoader: DeltaSkinAssetLoader,
    onClick: () -> Unit,
) {
    LemuroidCardSettingsGroup(title = { Text(text = title) }) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
        ) {
            SelectedSkinPreview(preview = selection.preview, assetLoader = assetLoader)
            ListItem(
                headlineContent = {
                    Text(text = selection.skinName ?: stringResource(R.string.controller_skins_default))
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}
