package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** The smallest width a tile can be squeezed into before it stops being tappable. */
val GAME_MENU_TILE_MIN_WIDTH = 88.dp

const val TABLET_SMALLEST_WIDTH_DP = 600
const val PHONE_PORTRAIT_COLUMNS = 3
const val TABLET_PORTRAIT_COLUMNS = 4

@Composable
fun isGameMenuLandscape(): Boolean = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

private val TILE_ROW_SPACING = 4.dp
private val TILE_ICON_BOX_SIZE = 64.dp
private val TILE_ICON_SIZE = 32.dp
private val TILE_LABEL_SPACING = 8.dp

private const val DISABLED_ALPHA = 0.38f

/** A single option displayed as a large icon in a rounded square with a label underneath. */
sealed class GameMenuEntry {
    abstract val labelId: Int
    abstract val icon: Painter
    abstract val enabled: Boolean
    abstract val active: Boolean

    class Action(
        override val labelId: Int,
        override val icon: Painter,
        override val enabled: Boolean = true,
        override val active: Boolean = false,
        val onClick: () -> Unit,
    ) : GameMenuEntry()

    class Options(
        override val labelId: Int,
        override val icon: Painter,
        override val enabled: Boolean = true,
        override val active: Boolean = false,
        val options: List<String>,
        val selectedIndex: Int,
        val onOptionSelected: (Int) -> Unit,
    ) : GameMenuEntry()
}

@Composable
fun GameMenuGrid(
    entries: List<GameMenuEntry>,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Icons sit centered in equally wide cells, so a cell carries half the gap between two
        // icons on either side. Asking for that half gap to also be the gap to the grid's edges
        // gives a cell width of (width + iconSize) / (columns + 1), with the remaining half cell
        // split between the two edges. Labels get the whole cell width and wrap within it.
        val cellWidth = (maxWidth + TILE_ICON_BOX_SIZE) / (columns + 1)
        val edgePadding = ((cellWidth - TILE_ICON_BOX_SIZE) / 2).coerceAtLeast(0.dp)

        Column(
            modifier = Modifier.padding(horizontal = edgePadding),
            verticalArrangement = Arrangement.spacedBy(TILE_ROW_SPACING),
        ) {
            entries.chunked(columns).forEach { rowEntries ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowEntries.forEach { entry ->
                        GameMenuTile(entry = entry, modifier = Modifier.weight(1f))
                    }
                    repeat(columns - rowEntries.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GameMenuTile(
    entry: GameMenuEntry,
    modifier: Modifier = Modifier,
) {
    var showOptions by remember { mutableStateOf(false) }

    val label = stringResource(entry.labelId)
    val colors = tileColors(entry)
    val shape = MaterialTheme.shapes.large

    val onTileClick = {
        when (entry) {
            is GameMenuEntry.Action -> entry.onClick()
            is GameMenuEntry.Options -> showOptions = true
        }
    }

    Column(
        modifier =
            modifier
                .clip(shape)
                .clickable(enabled = entry.enabled, onClick = onTileClick)
                .padding(vertical = TILE_LABEL_SPACING),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(TILE_ICON_BOX_SIZE)
                    .clip(shape)
                    .background(colors.container)
                    .border(width = 1.dp, color = colors.border, shape = shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(TILE_ICON_SIZE),
                painter = entry.icon,
                contentDescription = label,
                tint = colors.content,
            )
        }
        Spacer(modifier = Modifier.height(TILE_LABEL_SPACING))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.content,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }

    if (showOptions && entry is GameMenuEntry.Options) {
        GameMenuOptionsDialog(
            entry = entry,
            onDismissRequest = { showOptions = false },
        )
    }
}

@Composable
private fun GameMenuOptionsDialog(
    entry: GameMenuEntry.Options,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(entry.labelId)) },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .selectableGroup(),
            ) {
                entry.options.forEachIndexed { index, option ->
                    val isSelected = index == entry.selectedIndex
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    role = Role.RadioButton,
                                    selected = isSelected,
                                    onClick = {
                                        onDismissRequest()
                                        if (!isSelected) entry.onOptionSelected(index)
                                    },
                                ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Text(
                            modifier = Modifier.padding(start = 16.dp),
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}

private data class TileColors(
    val container: Color,
    val border: Color,
    val content: Color,
)

@Composable
private fun tileColors(entry: GameMenuEntry): TileColors {
    val scheme = MaterialTheme.colorScheme
    return when {
        !entry.enabled ->
            TileColors(
                container = scheme.surfaceVariant.copy(alpha = DISABLED_ALPHA),
                border = scheme.outlineVariant.copy(alpha = DISABLED_ALPHA),
                content = scheme.onSurface.copy(alpha = DISABLED_ALPHA),
            )
        entry.active ->
            TileColors(
                container = scheme.primaryContainer,
                border = scheme.primary,
                content = scheme.onPrimaryContainer,
            )
        else ->
            TileColors(
                container = scheme.surfaceVariant,
                border = scheme.outlineVariant,
                content = scheme.onSurfaceVariant,
            )
    }
}
