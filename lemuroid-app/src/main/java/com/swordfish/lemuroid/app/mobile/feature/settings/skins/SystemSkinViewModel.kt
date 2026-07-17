package com.swordfish.lemuroid.app.mobile.feature.settings.skins

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.skin.ControllerSkinPreferences
import com.swordfish.lemuroid.lib.library.skin.DeltaSkinHandle
import com.swordfish.lemuroid.lib.library.skin.DeltaSkinManager
import com.swordfish.touchinput.deltaskin.DeltaSkinInfo
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager.Orientation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the per-system skin screen (the first settings layer): it exposes the currently selected
 * portrait and landscape skins, each with a full-width preview, and drills into the per-orientation
 * picker from there.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SystemSkinViewModel(
    private val deltaSkinManager: DeltaSkinManager,
    private val controllerSkinPreferences: ControllerSkinPreferences,
    private val systemID: SystemID?,
    private val isTablet: Boolean,
) : ViewModel() {
    class Factory(
        private val deltaSkinManager: DeltaSkinManager,
        private val controllerSkinPreferences: ControllerSkinPreferences,
        private val systemDbName: String?,
        private val isTablet: Boolean,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val systemID = SystemID.values().firstOrNull { it.dbname == systemDbName }
            return SystemSkinViewModel(deltaSkinManager, controllerSkinPreferences, systemID, isTablet) as T
        }
    }

    /** The skin selected for one orientation: its display name and preview, both null when default. */
    data class OrientationSelection(
        val skinName: String? = null,
        val preview: SkinPreview? = null,
    )

    data class State(
        val portrait: OrientationSelection = OrientationSelection(),
        val landscape: OrientationSelection = OrientationSelection(),
    )

    private val refreshTrigger = MutableStateFlow(0)

    val uiState =
        refreshTrigger
            .mapLatest { load() }
            .stateIn(viewModelScope, SharingStarted.Lazily, State())

    private suspend fun load(): State {
        val system = systemID ?: return State()
        val handlesById = deltaSkinManager.listSkins().associateBy { it.id }
        return State(
            portrait = orientationSelection(handlesById, system, Orientation.PORTRAIT),
            landscape = orientationSelection(handlesById, system, Orientation.LANDSCAPE),
        )
    }

    private fun orientationSelection(
        handlesById: Map<String, DeltaSkinHandle>,
        systemID: SystemID,
        orientation: Orientation,
    ): OrientationSelection {
        val id = controllerSkinPreferences.getSelectedSkinId(systemID, orientation) ?: return OrientationSelection()
        val handle = handlesById[id] ?: return OrientationSelection()
        val orientationName =
            when (orientation) {
                Orientation.LANDSCAPE -> DeltaSkinInfo.ORIENTATION_LANDSCAPE
                Orientation.PORTRAIT -> DeltaSkinInfo.ORIENTATION_PORTRAIT
            }
        val representation = deltaSkinManager.resolveRepresentation(handle.info, isTablet, orientationName)
        return OrientationSelection(
            skinName = handle.info.name,
            preview = representation?.let { SkinPreview(handle.directory, it) },
        )
    }

    fun importSkin(uri: Uri) {
        viewModelScope.launch {
            deltaSkinManager.importSkin(uri)
            refresh()
        }
    }

    fun refresh() {
        refreshTrigger.value += 1
    }
}
