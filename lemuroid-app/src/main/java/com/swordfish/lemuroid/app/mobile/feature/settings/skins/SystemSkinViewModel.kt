package com.swordfish.lemuroid.app.mobile.feature.settings.skins

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.skin.ControllerSkinPreferences
import com.swordfish.lemuroid.lib.library.skin.DeltaSkinManager
import com.swordfish.lemuroid.lib.library.skin.DeltaSkinSystemMapping
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager.Orientation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SystemSkinViewModel(
    private val deltaSkinManager: DeltaSkinManager,
    private val controllerSkinPreferences: ControllerSkinPreferences,
    private val systemID: SystemID?,
) : ViewModel() {
    class Factory(
        private val deltaSkinManager: DeltaSkinManager,
        private val controllerSkinPreferences: ControllerSkinPreferences,
        private val systemDbName: String?,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val systemID = SystemID.values().firstOrNull { it.dbname == systemDbName }
            return SystemSkinViewModel(deltaSkinManager, controllerSkinPreferences, systemID) as T
        }
    }

    data class SkinOption(val id: String, val name: String)

    data class State(
        val options: List<SkinOption> = emptyList(),
        val portraitSelectedId: String? = null,
        val landscapeSelectedId: String? = null,
    )

    private val refreshTrigger = MutableStateFlow(0)

    val uiState =
        refreshTrigger
            .mapLatest { load() }
            .stateIn(viewModelScope, SharingStarted.Lazily, State())

    private suspend fun load(): State {
        val system = systemID ?: return State()
        val options =
            deltaSkinManager
                .listSkins()
                .filter { DeltaSkinSystemMapping.isCompatible(it.info.gameTypeIdentifier, system) }
                .map { SkinOption(it.id, it.info.name) }
        return State(
            options = options,
            portraitSelectedId = controllerSkinPreferences.getSelectedSkinId(system, Orientation.PORTRAIT),
            landscapeSelectedId = controllerSkinPreferences.getSelectedSkinId(system, Orientation.LANDSCAPE),
        )
    }

    fun setSelection(
        orientation: Orientation,
        skinId: String?,
    ) {
        val system = systemID ?: return
        controllerSkinPreferences.setSelectedSkinId(system, orientation, skinId)
        refresh()
    }

    fun importSkin(uri: Uri) {
        viewModelScope.launch {
            deltaSkinManager.importSkin(uri)
            refresh()
        }
    }

    fun deleteSkin(id: String) {
        viewModelScope.launch {
            val system = systemID
            if (system != null) {
                if (controllerSkinPreferences.getSelectedSkinId(system, Orientation.PORTRAIT) == id) {
                    controllerSkinPreferences.setSelectedSkinId(system, Orientation.PORTRAIT, null)
                }
                if (controllerSkinPreferences.getSelectedSkinId(system, Orientation.LANDSCAPE) == id) {
                    controllerSkinPreferences.setSelectedSkinId(system, Orientation.LANDSCAPE, null)
                }
            }
            deltaSkinManager.deleteSkin(id)
            refresh()
        }
    }

    private fun refresh() {
        refreshTrigger.value += 1
    }
}