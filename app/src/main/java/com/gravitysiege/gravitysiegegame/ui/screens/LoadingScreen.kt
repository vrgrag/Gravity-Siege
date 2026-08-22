package com.gravitysiege.gravitysiegegame.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravitysiege.gravitysiegegame.AssetBitmaps
import com.gravitysiege.gravitysiegegame.ui.components.AssetImage
import com.gravitysiege.gravitysiegegame.ui.theme.Gold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val preload = listOf(
    "Horizontal_Loading_Screen.webp",
    "Vertical_Loading_Screen.webp",
    "bg_sky_asset.webp",
    "start_bg_asset.webp",
    "start_block_asset.webp",
    "Game_Name.webp",
    "hook_asset.webp",
    "block_asset_01.webp",
    "block_asset_02.webp",
    "block_asset_03.webp",
    "block_asset_04.webp",
    "cloud_asset_01.webp",
    "cloud_asset_02.webp",
    "button_asset.webp",
    "button_blank.webp",
)

@Composable
fun LoadingScreen(activity: Activity, onReady: () -> Unit) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var progress by remember { mutableFloatStateOf(0f) }
    var dots by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        while (true) {
            delay(380)
            dots = dots % 3 + 1
        }
    }

    LaunchedEffect(Unit) {
        preload.forEachIndexed { index, path ->
            withContext(Dispatchers.IO) { AssetBitmaps.get(activity, path) }
            progress = (index + 1f) / preload.size
            delay(36)
        }
        progress = 1f
        delay(240)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onReady()
    }

    Box(Modifier.fillMaxSize()) {
        AssetImage(
            if (landscape) "Horizontal_Loading_Screen.webp" else "Vertical_Loading_Screen.webp",
            Modifier.fillMaxSize(),
            ContentScale.Crop,
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Loading" + ".".repeat(dots),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.42f))
                    .border(1.4.dp, Gold, RoundedCornerShape(20.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(16.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF7FE7FF), Gold, Color(0xFFFFF2A8)),
                            ),
                        ),
                )
            }
        }
    }
}
