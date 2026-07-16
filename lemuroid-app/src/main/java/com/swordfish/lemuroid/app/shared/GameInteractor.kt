package com.swordfish.lemuroid.app.shared

import android.net.Uri
import coil.imageLoader
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.shortcuts.ShortcutsGenerator
import com.swordfish.lemuroid.app.shared.game.GameLauncher
import com.swordfish.lemuroid.app.shared.main.BusyActivity
import com.swordfish.lemuroid.common.displayToast
import com.swordfish.lemuroid.lib.library.LemuroidLibrary
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.GameArtFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class GameInteractor(
    private val activity: BusyActivity,
    private val retrogradeDb: RetrogradeDatabase,
    private val useLeanback: Boolean,
    private val shortcutsGenerator: ShortcutsGenerator,
    private val gameLauncher: GameLauncher,
    private val lemuroidLibrary: LemuroidLibrary,
) {
    fun onGamePlay(game: Game) {
        if (!ensureNotBusy()) {
            return
        }
        if (!ensureNotificationsPermissionAvailable()) {
            return
        }
        gameLauncher.launchGameAsync(activity.activity(), game, true, useLeanback)
    }

    fun onGameRestart(game: Game) {
        if (!ensureNotBusy()) {
            return
        }
        if (!ensureNotificationsPermissionAvailable()) {
            return
        }
        gameLauncher.launchGameAsync(activity.activity(), game, false, useLeanback)
    }

    fun onFavoriteToggle(
        game: Game,
        isFavorite: Boolean,
    ) {
        GlobalScope.launch {
            retrogradeDb.gameDao().update(game.copy(isFavorite = isFavorite))
        }
    }

    fun onCreateShortcut(game: Game) {
        GlobalScope.launch {
            shortcutsGenerator.pinShortcutForGame(game)
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun onSetCustomArtwork(
        game: Game,
        imageUri: Uri,
    ) {
        GlobalScope.launch {
            val context = activity.activity()
            val resolver = context.contentResolver

            val artUri =
                runCatching {
                    val extension = GameArtFiles.extensionForMimeType(resolver.getType(imageUri))
                    resolver.openInputStream(imageUri)?.use { input ->
                        lemuroidLibrary.writeGameCover(game, extension, input)
                    }
                }.getOrElse {
                    Timber.e(it, "Error while copying custom artwork")
                    null
                }

            if (artUri == null) {
                withContext(Dispatchers.Main) {
                    context.displayToast(R.string.game_interactor_custom_artwork_failed)
                }
                return@launch
            }

            retrogradeDb.gameDao().update(game.copy(coverFrontUrl = artUri.toString()))

            withContext(Dispatchers.Main) {
                // Drop any cached copy so the freshly picked image is displayed immediately.
                val imageLoader = context.imageLoader
                imageLoader.diskCache?.remove(artUri.toString())
                imageLoader.memoryCache?.clear()
            }
        }
    }

    fun supportShortcuts(): Boolean {
        return shortcutsGenerator.supportShortcuts()
    }

    private fun ensureNotificationsPermissionAvailable(): Boolean {
        if (useLeanback || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        val permissionResult =
            ContextCompat.checkSelfPermission(
                activity.activity(),
                Manifest.permission.POST_NOTIFICATIONS,
            )

        if (permissionResult == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        activity.activity().displayToast(R.string.game_interactor_notification_permission_required)
        return false
    }

    private fun ensureNotBusy(): Boolean {
        if (activity.isBusy()) {
            activity.activity().displayToast(R.string.game_interactory_busy)
            return false
        }
        return true
    }
}
