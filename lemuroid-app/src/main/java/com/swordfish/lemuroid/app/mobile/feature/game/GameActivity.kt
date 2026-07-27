package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.runtime.Composable
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuActivity
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel

class GameActivity : BaseGameActivity() {
    @Composable
    override fun GameScreen(viewModel: BaseGameScreenViewModel) {
        MobileGameScreen(viewModel)
    }

    override fun getDialogClass() = GameMenuActivity::class.java

    /**
     * The menu draws its own entrance: the sheet slides up and its scrim fades. A window fade on
     * top of that would cross fade the sheet while it slides, so this window stays still.
     */
    override fun applyGameMenuOpenTransition() {
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
