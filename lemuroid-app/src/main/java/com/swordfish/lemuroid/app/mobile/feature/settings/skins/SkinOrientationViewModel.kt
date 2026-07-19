package com.swordfish.lemuroid.app.mobile.feature.settings.skins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.skin.ControllerSkinPreferences
import com.swordfish.lemuroid.lib.library.skin.DeltaSkinManager
import com.swordfish.lemuroid.lib.library.skin.DeltaSkinSystemMapping
import com.swordfish.touchinput.deltaskin.DeltaSkinInfo
import com.swordfish.touchinput.deltaskin.DeltaSkinRepresentation
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager.Orientation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.io.File

/** The artwork needed to render a full-width [com.swordfish.touchinput.deltaskin.DeltaSkinPreview]. */
data class SkinPreview(
    val directory: File,
    val representation: DeltaSkinRepresentation,
)

/**
 * Backs the per-orientation skin picker (the second settings layer): it lists every skin compatible
 * with the system, each with a preview for the selected [orientation], and tracks which one is active.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SkinOrientationViewModel(
    private val deltaSkinManager: DeltaSkinManager,
    private val controllerSkinPreferences: ControllerSkinPreferences,
    private val systemID: SystemID?,
    private val orientation: Orientation,
    private val isTablet: Boolean,
) : ViewModel() {
    class Factory(
        private val deltaSkinManager: DeltaSkinManager,
        private val controllerSkinPreferences: ControllerSkinPreferences,
        private val systemDbName: String?,
        private val orientation: Orientation,
        private val isTablet: Boolean,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val systemID = SystemID.values().firstOrNull { it.dbname == systemDbName }
            return SkinOrientationViewModel(
                deltaSkinManager,
                controllerSkinPreferences,
                systemID,
                orientation,
                isTablet,
            ) as T
        }
    }

    data class SkinOption(
        val id: String,
        val name: String,
        val preview: SkinPreview?,
    )

    data class State(
        val options: List<SkinOption> = emptyList(),
        val selectedId: String? = null,
    )

    private val refreshTrigger = MutableStateFlow(0)

    val uiState =
        refreshTrigger
            .mapLatest { load() }
            .stateIn(viewModelScope, SharingStarted.Lazily, State())

    private suspend fun load(): State {
        val system = systemID ?: return State()
        val orientationName = orientationName()
        val options =
            deltaSkinManager
                .listSkins()
                .filter { DeltaSkinSystemMapping.isCompatible(it.info.gameTypeIdentifier, system) }
                .map { handle ->
                    val representation =
                        deltaSkinManager.resolveRepresentation(handle.info, isTablet, orientationName)
                    SkinOption(
                        id = handle.id,
                        name = handle.info.name,
                        preview = representation?.let { SkinPreview(handle.directory, it) },
                    )
                }
        return State(
            options = options,
            selectedId = controllerSkinPreferences.getSelectedSkinId(system, orientation),
        )
    }

    fun setSelection(skinId: String?) {
        val system = systemID ?: return
        controllerSkinPreferences.setSelectedSkinId(system, orientation, skinId)
        refresh()
    }

    private fun orientationName(): String =
        when (orientation) {
            Orientation.LANDSCAPE -> DeltaSkinInfo.ORIENTATION_LANDSCAPE
            Orientation.PORTRAIT -> DeltaSkinInfo.ORIENTATION_PORTRAIT
        }

    private fun refresh() {
        refreshTrigger.value += 1
    }
}
