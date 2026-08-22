package com.gravitysiege.gravitysiegegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravitysiege.gravitysiegegame.GameStore
import com.gravitysiege.gravitysiegegame.audio.Sfx
import com.gravitysiege.gravitysiegegame.ui.Routes
import com.gravitysiege.gravitysiegegame.ui.components.AssetImage
import com.gravitysiege.gravitysiegegame.ui.components.AssetTextButton
import com.gravitysiege.gravitysiegegame.ui.components.CoinPill
import com.gravitysiege.gravitysiegegame.ui.components.SettingsButton
import com.gravitysiege.gravitysiegegame.ui.theme.Ink
import com.gravitysiege.gravitysiegegame.ui.theme.InkMuted

@Composable
fun MenuScreen(store: GameStore, sfx: Sfx, open: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoinPill(store.coins)
                SettingsButton {
                    sfx.click()
                    open(Routes.SETTINGS)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "GRAVITY",
                color = Color(0xFFF2A100),
                fontWeight = FontWeight.Black,
                fontSize = 42.sp,
                letterSpacing = 2.sp,
            )
            Text(
                "SIEGE",
                color = Color(0xFF3AA0E8),
                fontWeight = FontWeight.Black,
                fontSize = 42.sp,
                letterSpacing = 6.sp,
            )
            Text(
                "Stack houses. Cash out before the tower falls.",
                color = InkMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
            )
            Spacer(Modifier.weight(1f))
            AssetImage(
                "start_block_asset.webp",
                Modifier
                    .fillMaxWidth(0.82f)
                    .height(210.dp),
                ContentScale.Fit,
            )
            Spacer(Modifier.weight(1f))
            AssetTextButton(
                label = "PLAY",
                onClick = {
                    sfx.transition()
                    open(Routes.GAME)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                height = 78.dp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Virtual coins only",
                color = Ink.copy(alpha = 0.45f),
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
