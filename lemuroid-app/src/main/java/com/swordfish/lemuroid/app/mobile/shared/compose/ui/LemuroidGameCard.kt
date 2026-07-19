package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LemuroidGameCard(
    modifier: Modifier = Modifier,
    game: Game,
    onClick: () -> Unit = { },
    onLongClick: () -> Unit = { },
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
    ) {
        LemuroidGameImage(game = game)
        LemuroidGameGridTexts(game = game)
    }
}
