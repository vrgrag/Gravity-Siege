package com.gravitysiege.gravitysiegegame.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravitysiege.gravitysiegegame.GameStore
import com.gravitysiege.gravitysiegegame.audio.Sfx
import com.gravitysiege.gravitysiegegame.game.Yard
import com.gravitysiege.gravitysiegegame.ui.Routes
import com.gravitysiege.gravitysiegegame.ui.components.AssetImage
import com.gravitysiege.gravitysiegegame.ui.components.CraneRig
import com.gravitysiege.gravitysiegegame.ui.components.FundsReadout
import com.gravitysiege.gravitysiegegame.ui.components.GameTitle
import com.gravitysiege.gravitysiegegame.ui.components.HazardTape
import com.gravitysiege.gravitysiegegame.ui.components.HazardYellow
import com.gravitysiege.gravitysiegegame.ui.components.PlateButton
import com.gravitysiege.gravitysiegegame.ui.components.SiteDivider
import com.gravitysiege.gravitysiegegame.ui.components.SiteHeader
import com.gravitysiege.gravitysiegegame.ui.components.SiteStat
import com.gravitysiege.gravitysiegegame.ui.components.SteelKey
import com.gravitysiege.gravitysiegegame.ui.components.SteelPlate
import com.gravitysiege.gravitysiegegame.ui.components.SteelText
import com.gravitysiege.gravitysiegegame.ui.theme.Ink
import com.gravitysiege.gravitysiegegame.ui.theme.SkyDeep
import com.gravitysiege.gravitysiegegame.ui.theme.formatCoins

private const val STREET_RATIO = 0.412f

@Composable
fun MenuScreen(store: GameStore, sfx: Sfx, open: (String) -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(SkyDeep),
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val cityH = screenW * STREET_RATIO

        AssetImage("bg_sky_asset.webp", Modifier.fillMaxSize(), ContentScale.Crop)
        DriftingSky(screenW)
        AssetImage(
            Yard.STREET,
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(cityH),
            ContentScale.FillBounds,
        )

        Column(Modifier.fillMaxSize()) {
            SiteHeader {
                Row(
                    Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FundsReadout(store.coins)
                    SteelKey(Icons.Filled.Settings, "Settings") {
                        sfx.click()
                        open(Routes.SETTINGS)
                    }
                }
            }

            CraneRig(
                houseArt = Yard.HOUSES[1],
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            GameTitle(
                Modifier.padding(horizontal = 24.dp),
                height = screenH * 0.11f,
            )
            Spacer(Modifier.height(14.dp))

            SiteBoard(
                bestWin = store.biggestWin,
                tallest = store.tallestTower,
                onPlay = {
                    sfx.transition()
                    open(Routes.GAME)
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            // Only a sliver of the city stays clear under the board, so the panel
            // sits low on the screen and the crane keeps the room above it.
            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(cityH * 0.34f),
            )
        }
    }
}

@Composable
private fun SiteBoard(
    bestWin: Int,
    tallest: Int,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteelPlate(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SiteStat("BEST HAUL", formatCoins(bestWin), HazardYellow)
                SiteDivider()
                SiteStat("TALLEST", "$tallest FL")
                SiteDivider()
                SiteStat("PAYOUT", "VIRTUAL")
            }
            Spacer(Modifier.height(12.dp))
            HazardTape(
                Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                stripe = 9.dp,
            )
            Spacer(Modifier.height(12.dp))
            PlateButton(
                label = "START SHIFT",
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
                height = 74.dp,
                ink = Ink,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "VIRTUAL COINS ONLY · NO REAL MONEY",
                color = SteelText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
        }
    }
}

@Composable
private fun BoxScope.DriftingSky(screenW: Dp) {
    val motion = rememberInfiniteTransition(label = "sky")
    val near by motion.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Restart),
        label = "near",
    )
    val far by motion.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(38000, easing = LinearEasing), RepeatMode.Restart),
        label = "far",
    )
    AssetImage(
        "trim_cloud_asset_02.webp",
        Modifier
            .align(Alignment.TopStart)
            .offset(x = screenW * (far * 1.6f - 0.6f), y = 150.dp)
            .width(screenW * 0.68f)
            .height(screenW * 0.68f * 0.436f),
        ContentScale.Fit,
    )
    AssetImage(
        "trim_cloud_asset_01.webp",
        Modifier
            .align(Alignment.TopStart)
            .offset(x = screenW * (1.1f - near * 1.7f), y = 300.dp)
            .width(screenW * 0.52f)
            .height(screenW * 0.52f * 0.465f),
        ContentScale.Fit,
    )
}
