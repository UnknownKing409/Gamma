package com.swordfish.touchinput.deltaskin

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.io.File
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renders a Delta controller skin: draws the (letterboxed) skin artwork, lays out invisible touch
 * regions for each item, and reports the rectangle where the emulator video should be shown.
 *
 * Coordinate semantics (matching Delta):
 * - When the skin defines `screens`, `mappingSize` is the full canvas: it is letterboxed to fit the
 *   screen and the emulator video is placed at the screen's `outputFrame`.
 * - Otherwise, in portrait the skin is a bottom control strip fitted to the screen width with the
 *   video filling the space above it; in landscape the skin fills the (letterboxed) screen and the
 *   video is shown behind the (often translucent) artwork.
 */
@Composable
fun DeltaSkinOverlay(
    skinDir: File,
    representation: DeltaSkinRepresentation,
    isLandscape: Boolean,
    assetLoader: DeltaSkinAssetLoader,
    onGameScreenRect: (Rect) -> Unit,
    onButton: (keyCodes: List<Int>, pressed: Boolean) -> Unit,
    onMenu: (pressed: Boolean) -> Unit,
    onMotion: (source: Int, x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val mapW = representation.mappingSize.width
        val mapH = representation.mappingSize.height

        if (mapW <= 0f || mapH <= 0f || screenW <= 0f || screenH <= 0f) return@BoxWithConstraints

        val layout =
            remember(screenW, screenH, mapW, mapH, isLandscape, representation) {
                computeLayout(screenW, screenH, mapW, mapH, isLandscape, representation)
            }

        LaunchedEffect(layout.gameRect) { onGameScreenRect(layout.gameRect) }

        // Background artwork.
        val assetName = representation.assets.preferredAsset()
        val targetW = layout.drawnW.roundToInt()
        val targetH = layout.drawnH.roundToInt()
        if (assetName != null) {
            val bitmap by
                produceState<android.graphics.Bitmap?>(null, skinDir, assetName, targetW, targetH) {
                    value = assetLoader.loadBitmap(skinDir, assetName, targetW, targetH)
                }
            bitmap?.let { bmp ->
                val image = remember(bmp) { bmp.asImageBitmap() }
                Box(
                    modifier =
                        Modifier
                            .absoluteOffset {
                                IntOffset(layout.originX.roundToInt(), layout.originY.roundToInt())
                            }
                            .size(
                                width = with(density) { layout.drawnW.toDp() },
                                height = with(density) { layout.drawnH.toDp() },
                            ),
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // Touch regions.
        representation.items.forEach { item ->
            val control = remember(item) { DeltaSkinInputMapping.classify(item) }
            if (control is DeltaSkinInputMapping.Control.Unsupported) return@forEach

            val rect = layout.mapItemRect(item)
            Box(
                modifier =
                    Modifier
                        .absoluteOffset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                        .size(
                            width = with(density) { rect.width.toDp() },
                            height = with(density) { rect.height.toDp() },
                        )
                        .controlInput(control, onButton, onMenu, onMotion),
            )
        }
    }
}

private fun Modifier.controlInput(
    control: DeltaSkinInputMapping.Control,
    onButton: (List<Int>, Boolean) -> Unit,
    onMenu: (Boolean) -> Unit,
    onMotion: (Int, Float, Float) -> Unit,
): Modifier =
    when (control) {
        is DeltaSkinInputMapping.Control.Buttons ->
            pointerInput(control) {
                pressGesture(
                    onDown = { onButton(control.keyCodes, true) },
                    onUp = { onButton(control.keyCodes, false) },
                )
            }

        is DeltaSkinInputMapping.Control.Menu ->
            pointerInput(control) {
                pressGesture(onDown = { onMenu(true) }, onUp = { onMenu(false) })
            }

        is DeltaSkinInputMapping.Control.AnalogButton ->
            pointerInput(control) {
                pressGesture(
                    onDown = { onMotion(control.motionSource, control.x, control.y) },
                    onUp = { onMotion(control.motionSource, 0f, 0f) },
                )
            }

        is DeltaSkinInputMapping.Control.Dpad ->
            pointerInput(control) {
                directionGesture(snapToEightWay = true) { x, y ->
                    onMotion(com.swordfish.touchinput.radial.layouts.shared.ComposeTouchLayouts.MOTION_SOURCE_DPAD, x, y)
                }
            }

        is DeltaSkinInputMapping.Control.Thumbstick ->
            pointerInput(control) {
                directionGesture(snapToEightWay = false) { x, y ->
                    onMotion(control.motionSource, x, y)
                }
            }

        DeltaSkinInputMapping.Control.Unsupported -> this
    }

/** Fires [onDown] on press and [onUp] on release or cancellation. */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.pressGesture(
    onDown: () -> Unit,
    onUp: () -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        onDown()
        waitForUpOrCancellation()
        onUp()
    }
}

/**
 * Tracks a finger within the control bounds and emits a motion vector (emulator convention: up is
 * negative y). When [snapToEightWay] the vector is quantised to 8 directions with a centre deadzone.
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.directionGesture(
    snapToEightWay: Boolean,
    onMotion: (Float, Float) -> Unit,
) {
    val width = size.width.toFloat()
    val height = size.height.toFloat()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        emitDirection(down.position.x, down.position.y, width, height, snapToEightWay, onMotion)
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            emitDirection(change.position.x, change.position.y, width, height, snapToEightWay, onMotion)
        }
        onMotion(0f, 0f)
    }
}

private fun emitDirection(
    px: Float,
    py: Float,
    width: Float,
    height: Float,
    snapToEightWay: Boolean,
    onMotion: (Float, Float) -> Unit,
) {
    val nx = ((px / width) * 2f - 1f).coerceIn(-1f, 1f)
    // Screen y grows downward; emulator "up" is negative y, so the sign already matches.
    val ny = ((py / height) * 2f - 1f).coerceIn(-1f, 1f)
    if (snapToEightWay) {
        val threshold = 0.35f
        val dx = if (abs(nx) > threshold) (if (nx > 0) 1f else -1f) else 0f
        val dy = if (abs(ny) > threshold) (if (ny > 0) 1f else -1f) else 0f
        onMotion(dx, dy)
    } else {
        onMotion(nx, ny)
    }
}

private class SkinLayout(
    val scale: Float,
    val originX: Float,
    val originY: Float,
    val drawnW: Float,
    val drawnH: Float,
    val gameRect: Rect,
) {
    /** Maps an item's frame (plus its extended edges) from skin points to screen pixels. */
    fun mapItemRect(item: DeltaSkinItem): Rect {
        val f = item.frame
        val e = item.extendedEdges
        val left = originX + (f.x - e.left) * scale
        val top = originY + (f.y - e.top) * scale
        val right = originX + (f.x + f.width + e.right) * scale
        val bottom = originY + (f.y + f.height + e.bottom) * scale
        return Rect(left, top, right, bottom)
    }
}

private fun computeLayout(
    screenW: Float,
    screenH: Float,
    mapW: Float,
    mapH: Float,
    isLandscape: Boolean,
    representation: DeltaSkinRepresentation,
): SkinLayout {
    val hasScreens = representation.screens.isNotEmpty()

    if (hasScreens) {
        // Full-canvas semantics: letterbox mappingSize, place video at the output frame(s).
        val scale = min(screenW / mapW, screenH / mapH)
        val drawnW = mapW * scale
        val drawnH = mapH * scale
        val originX = (screenW - drawnW) / 2f
        val originY = (screenH - drawnH) / 2f
        val bounds = unionOutputFrames(representation)
        val gameRect =
            if (bounds != null) {
                Rect(
                    originX + bounds.x * scale,
                    originY + bounds.y * scale,
                    originX + (bounds.x + bounds.width) * scale,
                    originY + (bounds.y + bounds.height) * scale,
                )
            } else {
                Rect(originX, originY, originX + drawnW, originY + drawnH)
            }
        return SkinLayout(scale, originX, originY, drawnW, drawnH, gameRect)
    }

    if (!isLandscape) {
        // Portrait: skin is a bottom control strip fitted to the screen width; video fills the top.
        var scale = screenW / mapW
        if (mapH * scale > screenH) scale = screenH / mapH
        val drawnW = mapW * scale
        val drawnH = mapH * scale
        val originX = (screenW - drawnW) / 2f
        val originY = screenH - drawnH
        val gameRect = Rect(0f, 0f, screenW, originY)
        return SkinLayout(scale, originX, originY, drawnW, drawnH, gameRect)
    }

    // Landscape: skin fills the letterboxed screen; video shown behind the (usually translucent) art.
    val scale = min(screenW / mapW, screenH / mapH)
    val drawnW = mapW * scale
    val drawnH = mapH * scale
    val originX = (screenW - drawnW) / 2f
    val originY = (screenH - drawnH) / 2f
    val gameRect = Rect(originX, originY, originX + drawnW, originY + drawnH)
    return SkinLayout(scale, originX, originY, drawnW, drawnH, gameRect)
}

private fun unionOutputFrames(representation: DeltaSkinRepresentation): DeltaSkinFrame? {
    val frames = representation.screens.mapNotNull { it.outputFrame }
    if (frames.isEmpty()) return null
    val left = frames.minOf { it.x }
    val top = frames.minOf { it.y }
    val right = frames.maxOf { it.x + it.width }
    val bottom = frames.maxOf { it.y + it.height }
    return DeltaSkinFrame(left, top, right - left, bottom - top)
}
