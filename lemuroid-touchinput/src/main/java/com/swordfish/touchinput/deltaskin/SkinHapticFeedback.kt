package com.swordfish.touchinput.deltaskin

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

/**
 * Plays the same press/release vibration effects PadKit uses for the default touch controls, so custom
 * (Delta) skins feel identical. PadKit's own haptic generator is `internal`, hence this parallel copy.
 */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class SkinHapticFeedback(applicationContext: Context) {
    private val vibrator = buildVibrator(applicationContext)
    private val strongEffect = buildStrongVibrationEffect()
    private val weakEffect = buildWeakVibrationEffect()
    private val scope = CoroutineScope(newSingleThreadContext("SkinHaptics"))

    /** Stronger tick when a control is pressed. */
    fun press() = vibrate(strongEffect)

    /** Lighter tick when a control is released. */
    fun release() = vibrate(weakEffect)

    private fun vibrate(effect: VibrationEffect?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && effect != null) {
            scope.launch { vibrator.vibrate(effect) }
        }
    }

    private fun buildVibrator(applicationContext: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applicationContext.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            applicationContext.getSystemService(Vibrator::class.java)
        }

    private fun buildStrongVibrationEffect(): VibrationEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        return VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
    }

    private fun buildWeakVibrationEffect(): VibrationEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        return VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
    }
}

@Composable
fun rememberSkinHapticFeedback(): SkinHapticFeedback {
    val applicationContext = LocalContext.current.applicationContext
    return remember { SkinHapticFeedback(applicationContext) }
}
