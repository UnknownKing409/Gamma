package com.swordfish.lemuroid.app.mobile.feature.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidEmptyView
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameCard
import com.swordfish.lemuroid.app.utils.android.ComposableLifecycle
import com.swordfish.lemuroid.common.displayDetailsSettingsScreen
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    searchQuery: String,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenCoreSelection: () -> Unit,
) {
    val context = LocalContext.current
    val applicationContext = context.applicationContext

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                viewModel.updatePermissions(applicationContext)
            }
            else -> { }
        }
    }

    LaunchedEffect(searchQuery) {
        viewModel.changeSearchQuery(searchQuery)
    }

    val permissionsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (!isGranted) {
                context.displayDetailsSettingsScreen()
            }
        }

    val state = viewModel.getViewStates().collectAsState(HomeViewModel.UIState())
    HomeScreen(
        modifier,
        state.value,
        onGameClick,
        onGameLongClick,
        onOpenCoreSelection,
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return@HomeScreen
            }

            permissionsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        { permissionsLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        { viewModel.changeLocalStorageFolder(context) },
    )
}

/** A single cell in the flattened grid, used to compute spans and section jump offsets. */
private sealed interface GridEntry {
    val isFullSpan: Boolean
    val key: String

    data class Notification(val notification: HomeNotificationType) : GridEntry {
        override val isFullSpan = true
        override val key = "notification_${notification.name}"
    }

    data class Header(val sectionId: String, val iconRes: Int?, val title: String, val count: Int) : GridEntry {
        override val isFullSpan = true
        override val key = "header_$sectionId"
    }

    data class GameCell(val game: Game) : GridEntry {
        override val isFullSpan = false
        override val key = "game_${game.id}"
    }
}

private enum class HomeNotificationType {
    NOTIFICATIONS,
    NO_GAMES,
    MICROPHONE,
    DESMUME,
}

/** A jump target on the fast-scroll rail. [systemImageRes] is null for the Favorites entry. */
private data class RailSection(val itemIndex: Int, val label: String, val systemImageRes: Int?)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeViewModel.UIState,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenCoreSelection: () -> Unit,
    onEnableNotificationsClicked: () -> Unit,
    onEnableMicrophoneClicked: () -> Unit,
    onSetDirectoryClicked: () -> Unit,
) {
    val context = LocalContext.current

    val (entries, railSections) =
        remember(state) {
            val entries = mutableListOf<GridEntry>()

            if (state.showNoNotificationPermissionCard) {
                entries += GridEntry.Notification(HomeNotificationType.NOTIFICATIONS)
            }
            if (state.showNoGamesCard) {
                entries += GridEntry.Notification(HomeNotificationType.NO_GAMES)
            }
            if (state.showNoMicrophonePermissionCard) {
                entries += GridEntry.Notification(HomeNotificationType.MICROPHONE)
            }
            if (state.showDesmumeDeprecatedCard) {
                entries += GridEntry.Notification(HomeNotificationType.DESMUME)
            }

            val railSections = mutableListOf<RailSection>()
            state.sections.forEach { section ->
                val headerIndex = entries.size
                when (section) {
                    is HomeViewModel.Section.Favorites -> {
                        val title = context.getString(R.string.favorites)
                        entries += GridEntry.Header("favorites", null, title, section.games.size)
                        railSections += RailSection(headerIndex, title, null)
                    }
                    is HomeViewModel.Section.System -> {
                        val title = context.getString(section.metaSystem.titleResId)
                        entries +=
                            GridEntry.Header(
                                section.metaSystem.name,
                                section.metaSystem.imageResId,
                                title,
                                section.games.size,
                            )
                        railSections += RailSection(headerIndex, title, section.metaSystem.imageResId)
                    }
                }
                section.games.forEach { entries += GridEntry.GameCell(it) }
            }

            entries to railSections
        }

    if (entries.isEmpty()) {
        LemuroidEmptyView(modifier = modifier)
        return
    }

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    var activeRailIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            columns = GridCells.Adaptive(96.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 40.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                count = entries.size,
                key = { index -> entries[index].key },
                span = { index ->
                    if (entries[index].isFullSpan) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                },
            ) { index ->
                when (val entry = entries[index]) {
                    is GridEntry.Notification ->
                        HomeNotificationCard(
                            notification = entry.notification,
                            indexInProgress = state.indexInProgress,
                            onEnableNotificationsClicked = onEnableNotificationsClicked,
                            onEnableMicrophoneClicked = onEnableMicrophoneClicked,
                            onSetDirectoryClicked = onSetDirectoryClicked,
                            onOpenCoreSelection = onOpenCoreSelection,
                        )
                    is GridEntry.Header ->
                        SectionHeader(
                            iconRes = entry.iconRes,
                            title = entry.title,
                            count = entry.count,
                        )
                    is GridEntry.GameCell ->
                        LemuroidGameCard(
                            modifier = Modifier.animateItem(),
                            game = entry.game,
                            onClick = { onGameClicked(entry.game) },
                            onLongClick = { onGameLongClick(entry.game) },
                        )
                }
            }
        }

        if (railSections.isNotEmpty()) {
            SystemScrollRail(
                modifier = Modifier.align(Alignment.CenterEnd),
                railSections = railSections,
                activeIndex = activeRailIndex,
                onSelect = { index ->
                    activeRailIndex = index
                    coroutineScope.launch {
                        gridState.scrollToItem(railSections[index].itemIndex)
                    }
                },
                onRelease = { activeRailIndex = null },
            )
        }

        val active = activeRailIndex
        if (active != null) {
            RailSelectionBubble(
                modifier = Modifier.align(Alignment.Center),
                railSection = railSections[active],
            )
        }
    }
}

@Composable
private fun SectionHeader(
    iconRes: Int?,
    title: String,
    count: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (iconRes != null) {
            Image(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = iconRes),
                contentDescription = null,
            )
        } else {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SystemScrollRail(
    modifier: Modifier = Modifier,
    railSections: List<RailSection>,
    activeIndex: Int?,
    onSelect: (Int) -> Unit,
    onRelease: () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .width(32.dp)
                .padding(vertical = 8.dp)
                .pointerInput(railSections.size) {
                    fun selectAt(y: Float) {
                        val height = size.height
                        if (height <= 0 || railSections.isEmpty()) return
                        val index =
                            ((y / height) * railSections.size)
                                .toInt()
                                .coerceIn(0, railSections.size - 1)
                        onSelect(index)
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        selectAt(down.position.y)
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { change ->
                                if (change.pressed) {
                                    selectAt(change.position.y)
                                    change.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                        onRelease()
                    }
                },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        railSections.forEachIndexed { index, railSection ->
            RailIcon(railSection = railSection, active = index == activeIndex)
        }
    }
}

@Composable
private fun RailIcon(
    railSection: RailSection,
    active: Boolean,
) {
    val tint =
        if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    val iconSize = if (active) 24.dp else 18.dp

    if (railSection.systemImageRes != null) {
        Image(
            modifier = Modifier.size(iconSize),
            painter = painterResource(id = railSection.systemImageRes),
            contentDescription = railSection.label,
        )
    } else {
        Icon(
            modifier = Modifier.size(iconSize),
            imageVector = Icons.Filled.Star,
            contentDescription = railSection.label,
            tint = tint,
        )
    }
}

@Composable
private fun RailSelectionBubble(
    modifier: Modifier = Modifier,
    railSection: RailSection,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (railSection.systemImageRes != null) {
                Image(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(id = railSection.systemImageRes),
                    contentDescription = null,
                )
            } else {
                Icon(
                    modifier = Modifier.size(32.dp),
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = railSection.label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeNotificationCard(
    notification: HomeNotificationType,
    indexInProgress: Boolean,
    onEnableNotificationsClicked: () -> Unit,
    onEnableMicrophoneClicked: () -> Unit,
    onSetDirectoryClicked: () -> Unit,
    onOpenCoreSelection: () -> Unit,
) {
    when (notification) {
        HomeNotificationType.NOTIFICATIONS ->
            HomeNotification(
                titleId = R.string.home_notification_title,
                messageId = R.string.home_notification_message,
                actionId = R.string.home_notification_action,
                onAction = onEnableNotificationsClicked,
            )
        HomeNotificationType.NO_GAMES ->
            HomeNotification(
                titleId = R.string.home_empty_title,
                messageId = R.string.home_empty_message,
                actionId = R.string.home_empty_action,
                onAction = onSetDirectoryClicked,
                enabled = !indexInProgress,
            )
        HomeNotificationType.MICROPHONE ->
            HomeNotification(
                titleId = R.string.home_microphone_title,
                messageId = R.string.home_microphone_message,
                actionId = R.string.home_microphone_action,
                onAction = onEnableMicrophoneClicked,
            )
        HomeNotificationType.DESMUME ->
            HomeNotification(
                titleId = R.string.home_notification_desmume_deprecated_title,
                messageId = R.string.home_notification_desmume_deprecated_message,
                actionId = R.string.home_notification_desmume_deprecated_action,
                onAction = onOpenCoreSelection,
            )
    }
}

@Composable
private fun HomeNotification(
    titleId: Int,
    messageId: Int,
    actionId: Int,
    enabled: Boolean = true,
    onAction: () -> Unit = { },
) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(titleId),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(messageId),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                modifier = Modifier.align(Alignment.End),
                onClick = onAction,
                enabled = enabled,
            ) {
                Text(stringResource(id = actionId))
            }
        }
    }
}
