package com.gravitysiege.gravitysiegegame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravitysiege.gravitysiegegame.game.Yard
import com.gravitysiege.gravitysiegegame.ui.theme.SkyDeep

/**
 * The shell every site screen shares: city skyline behind, dark header with a
 * back key and the balance, and hazard trim under it.
 */
@Composable
fun SiteScaffold(
    title: String,
    coins: Int,
    back: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(SkyDeep),
    ) {
        AssetImage("bg_sky_asset.webp", Modifier.fillMaxSize(), ContentScale.Crop)
        AssetImage(
            Yard.STREET,
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(160.dp),
            ContentScale.FillBounds,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF10141A).copy(alpha = 0.55f)),
        )

        Column(Modifier.fillMaxSize()) {
            SiteHeader {
                Row(
                    Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SteelKey(Icons.AutoMirrored.Filled.ArrowBack, "Back", size = 42.dp, onClick = back)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.6.sp,
                        )
                    }
                    FundsReadout(coins)
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                content = content,
            )
        }
    }
}
