package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val ENTER_ANIMATION = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)
private val EXIT_ANIMATION = tween<Float>(durationMillis = 200, easing = FastOutLinearInEasing)

/** How far down the sheet has to be dragged before letting go dismisses it. */
private const val DRAG_DISMISS_FRACTION = 0.4f

/** Downwards fling speed, in pixels per second, that dismisses regardless of how far it moved. */
private const val DRAG_DISMISS_VELOCITY = 1000f

@Stable
class GameMenuSheetState {
    /** How far the sheet currently sits below its resting position, in pixels. */
    internal val offset = Animatable(0f)

    internal var heightPx by mutableFloatStateOf(0f)

    internal var hasEntered by mutableStateOf(false)

    /** Slides the sheet back down below the bottom edge and suspends until it is gone. */
    suspend fun hide() {
        if (heightPx <= 0f) return
        offset.animateTo(heightPx, EXIT_ANIMATION)
    }

    internal suspend fun settle() {
        offset.animateTo(0f, ENTER_ANIMATION)
    }
}

@Composable
fun rememberGameMenuSheetState(): GameMenuSheetState = remember { GameMenuSheetState() }

/**
 * A bottom sheet that slides in and out with no cross fade.
 *
 * Material's ModalBottomSheet renders into its own dialog window, and Material themes that window
 * with the platform dialog animation. The window fades in while the sheet slides up, and because
 * the dialog is shown from a DisposableEffect before any content composes there is no point at
 * which the animation can be cleared in time. Drawing in the activity's own window instead leaves
 * the slide as the only motion. The activity theme dims the game behind it, so no scrim is drawn
 * here, but taps outside the sheet still dismiss it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameMenuSheet(
    state: GameMenuSheetState,
    onDismissRequest: () -> Unit,
    sheetMaxWidth: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    // The sheet is content sized, so its travel is only known once it has been measured. It is
    // parked off screen until then and slides up from there. This must not be keyed on the height:
    // window insets land a few frames in and resize the sheet, which would cancel the animation
    // partway and leave the sheet stranded off screen.
    LaunchedEffect(Unit) {
        snapshotFlow { state.heightPx }.first { it > 0f }
        state.offset.snapTo(state.heightPx)
        state.hasEntered = true
        state.settle()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    ),
        )

        Surface(
            modifier =
                Modifier
                    .widthIn(max = sheetMaxWidth)
                    .fillMaxWidth()
                    .onSizeChanged { state.heightPx = it.height.toFloat() }
                    .graphicsLayer {
                        translationY = if (state.hasEntered) state.offset.value else state.heightPx
                        alpha = if (state.heightPx > 0f) 1f else 0f
                    },
            shape = BottomSheetDefaults.ExpandedShape,
            color = BottomSheetDefaults.ContainerColor,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .draggable(
                                orientation = Orientation.Vertical,
                                state =
                                    rememberDraggableState { delta ->
                                        coroutineScope.launch {
                                            val target = state.offset.value + delta
                                            state.offset.snapTo(target.coerceIn(0f, state.heightPx))
                                        }
                                    },
                                onDragStopped = { velocity ->
                                    val draggedFarEnough =
                                        state.offset.value > state.heightPx * DRAG_DISMISS_FRACTION
                                    if (draggedFarEnough || velocity > DRAG_DISMISS_VELOCITY) {
                                        onDismissRequest()
                                    } else {
                                        state.settle()
                                    }
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    BottomSheetDefaults.DragHandle()
                }
                content()
            }
        }
    }
}
