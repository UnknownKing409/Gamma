package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.util.function.Consumer
import kotlin.math.roundToInt

/** How far the game behind the menu is blurred once the sheet has finished opening. */
private val BLUR_RADIUS = 24.dp

/**
 * Reports whether the game behind the menu can be blurred.
 *
 * Cross window blurs arrived in Android 12, and even there the system turns them off on devices
 * that cannot afford them and while battery saver is on, so this can change while the menu is open.
 */
@Composable
internal fun rememberBackgroundBlurEnabled(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberCrossWindowBlurEnabled()
    } else {
        false
    }

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun rememberCrossWindowBlurEnabled(): Boolean {
    val windowManager = LocalContext.current.getSystemService(WindowManager::class.java)
    var enabled by remember(windowManager) { mutableStateOf(windowManager.isCrossWindowBlurEnabled) }

    DisposableEffect(windowManager) {
        val listener = Consumer<Boolean> { enabled = it }
        windowManager.addCrossWindowBlurEnabledListener(listener)
        onDispose { windowManager.removeCrossWindowBlurEnabledListener(listener) }
    }

    return enabled
}

/**
 * Blurs everything drawn behind this activity's window, in step with [progress] going from 0 to 1.
 *
 * The menu is a translucent activity of its own, so the game is not part of the hierarchy the sheet
 * draws into and cannot be blurred with a render effect. A window blur is handled further down, by
 * the compositor, which is also why it reaches the game's surface at all. The radius is pushed
 * through the window attributes every time it changes, so the blur ramps up alongside the scrim
 * instead of snapping to full strength the moment the menu appears.
 */
@Composable
internal fun BackgroundBlur(progress: () -> Float) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val window = LocalContext.current.findActivityWindow() ?: return
    val maxRadiusPx = with(LocalDensity.current) { BLUR_RADIUS.roundToPx() }

    DisposableEffect(window) {
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.setBlurBehindRadius(0)
        }
    }

    // snapshotFlow only emits when the rounded radius actually changes, so a slow ramp does not
    // push the same value through the window attributes on every frame.
    LaunchedEffect(window, maxRadiusPx) {
        snapshotFlow { (progress() * maxRadiusPx).roundToInt() }
            .collect { window.setBlurBehindRadius(it) }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Window.setBlurBehindRadius(radius: Int) {
    attributes = attributes.also { it.blurBehindRadius = radius }
}

private fun Context.findActivityWindow(): Window? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context.window
        context = context.baseContext
    }
    return null
}
