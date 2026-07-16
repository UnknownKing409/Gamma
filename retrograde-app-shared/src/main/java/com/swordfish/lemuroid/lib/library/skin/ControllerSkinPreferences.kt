package com.swordfish.lemuroid.lib.library.skin

import android.content.SharedPreferences
import androidx.core.content.edit
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager.Orientation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Persists the controller skin chosen for each (system, orientation) slot. A missing value means the
 * default (PadKit) controls are used. Backed by the multi-process shared preferences so a selection
 * made in the main process is visible in the emulator process.
 */
class ControllerSkinPreferences(private val sharedPreferences: SharedPreferences) {
    fun getSelectedSkinId(
        systemID: SystemID,
        orientation: Orientation,
    ): String? = sharedPreferences.getString(key(systemID, orientation), null)

    fun setSelectedSkinId(
        systemID: SystemID,
        orientation: Orientation,
        skinId: String?,
    ) {
        sharedPreferences.edit {
            if (skinId == null) {
                remove(key(systemID, orientation))
            } else {
                putString(key(systemID, orientation), skinId)
            }
        }
    }

    fun observeSelectedSkinId(
        systemID: SystemID,
        orientation: Orientation,
    ): Flow<String?> =
        callbackFlow {
            val prefKey = key(systemID, orientation)
            trySend(sharedPreferences.getString(prefKey, null))
            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { prefs, changedKey ->
                    if (changedKey == prefKey) {
                        trySend(prefs.getString(prefKey, null))
                    }
                }
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
        }

    private fun key(
        systemID: SystemID,
        orientation: Orientation,
    ): String = "controller_skin_${systemID.dbname}_${orientation.ordinal}"
}
