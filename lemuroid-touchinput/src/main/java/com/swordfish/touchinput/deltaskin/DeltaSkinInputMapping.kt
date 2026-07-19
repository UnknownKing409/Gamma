package com.swordfish.touchinput.deltaskin

import android.view.KeyEvent
import com.swordfish.touchinput.radial.layouts.shared.ComposeTouchLayouts
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Translates Delta skin input identifiers into Lemuroid's keycodes / motion sources and classifies
 * each skin item into a renderable control type.
 */
object DeltaSkinInputMapping {
    /**
     * Sentinel keycode meaning "open the Lemuroid menu". Delta's `menu` input maps here, matching the
     * PadKit menu button which also uses [KeyEvent.KEYCODE_BUTTON_MODE].
     */
    const val MENU_KEYCODE = KeyEvent.KEYCODE_BUTTON_MODE

    /**
     * Standard button names shared across all supported systems. Genesis 6-button `c`/`z`/`mode`
     * are best-effort (Lemuroid's Genesis core uses non-obvious keycodes for those); the common
     * systems (NES/SNES/GB/GBA/N64) only rely on the standard names below.
     */
    private val BUTTON_KEYCODES: Map<String, Int> =
        mapOf(
            "a" to KeyEvent.KEYCODE_BUTTON_A,
            "b" to KeyEvent.KEYCODE_BUTTON_B,
            "c" to KeyEvent.KEYCODE_BUTTON_C,
            "x" to KeyEvent.KEYCODE_BUTTON_X,
            "y" to KeyEvent.KEYCODE_BUTTON_Y,
            "z" to KeyEvent.KEYCODE_BUTTON_Z,
            "l" to KeyEvent.KEYCODE_BUTTON_L1,
            "r" to KeyEvent.KEYCODE_BUTTON_R1,
            "l2" to KeyEvent.KEYCODE_BUTTON_L2,
            "r2" to KeyEvent.KEYCODE_BUTTON_R2,
            "l3" to KeyEvent.KEYCODE_BUTTON_THUMBL,
            "r3" to KeyEvent.KEYCODE_BUTTON_THUMBR,
            "start" to KeyEvent.KEYCODE_BUTTON_START,
            "select" to KeyEvent.KEYCODE_BUTTON_SELECT,
            "mode" to KeyEvent.KEYCODE_BUTTON_SELECT,
        )

    private const val INPUT_MENU = "menu"

    private val C_BUTTON_MOTION: Map<String, Pair<Float, Float>> =
        mapOf(
            // Right analog stick directions; emulator convention: up is negative y.
            "cup" to (0f to -1f),
            "cdown" to (0f to 1f),
            "cleft" to (-1f to 0f),
            "cright" to (1f to 0f),
        )

    /** Classified, renderable representation of a single skin item's inputs. */
    sealed class Control {
        /** One or more regular buttons pressed together. */
        data class Buttons(
            val keyCodes: List<Int>,
        ) : Control()

        /** Opens the Lemuroid in-game menu. */
        object Menu : Control()

        /** Directional pad -> [ComposeTouchLayouts.MOTION_SOURCE_DPAD]. */
        object Dpad : Control()

        /** Analog stick -> the given motion source. */
        data class Thumbstick(
            val motionSource: Int,
        ) : Control()

        /**
         * A button that emits a fixed motion on an analog stick (e.g. N64 C-buttons on the right stick).
         */
        data class AnalogButton(
            val motionSource: Int,
            val x: Float,
            val y: Float,
        ) : Control()

        /** Unsupported input (e.g. NDS touch screen in v1). */
        object Unsupported : Control()
    }

    fun classify(item: DeltaSkinItem): Control {
        val inputs = item.inputs ?: return Control.Unsupported

        val names = extractInputNames(inputs)
        if (names.isEmpty()) return Control.Unsupported

        val lower = names.map { it.lowercase() }

        // Touch screen (NDS) - not supported in v1.
        if (lower.any { it.startsWith("touchscreen") }) return Control.Unsupported

        // Menu.
        if (lower.any { it == INPUT_MENU }) return Control.Menu

        // Directional inputs (dpad or analog thumbstick).
        val isDirectional = lower.any { it in setOf("up", "down", "left", "right") }
        if (isDirectional) {
            return if (item.thumbstickSize != null) {
                Control.Thumbstick(ComposeTouchLayouts.MOTION_SOURCE_LEFT_STICK)
            } else {
                Control.Dpad
            }
        }

        // N64 C-buttons -> right analog stick.
        val cButton = lower.firstOrNull { it in C_BUTTON_MOTION.keys }
        if (cButton != null) {
            val (x, y) = C_BUTTON_MOTION.getValue(cButton)
            return Control.AnalogButton(ComposeTouchLayouts.MOTION_SOURCE_RIGHT_STICK, x, y)
        }

        // Regular buttons.
        val keyCodes = lower.mapNotNull { BUTTON_KEYCODES[it] }
        return if (keyCodes.isNotEmpty()) Control.Buttons(keyCodes) else Control.Unsupported
    }

    /**
     * Delta `inputs` is either a JSON array of button names (`["a","b"]`) or an object whose values are
     * input names (dpad `{"up":"up",...}`, touch screen `{"x":"touchScreenX",...}`).
     */
    private fun extractInputNames(inputs: kotlinx.serialization.json.JsonElement): List<String> =
        when (inputs) {
            is JsonArray -> inputs.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            is JsonObject -> {
                // Include both keys (up/down/left/right) and values (touchScreenX/...) to classify.
                val keys = inputs.keys.toList()
                val values = inputs.values.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                keys + values
            }
            is JsonPrimitive -> listOfNotNull(inputs.contentOrNull)
            else -> emptyList()
        }
}
