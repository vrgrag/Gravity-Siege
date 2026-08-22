package com.gravitysiege.gravitysiegegame.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravitysiege.gravitysiegegame.AssetBitmaps
import com.gravitysiege.gravitysiegegame.ui.theme.Gold
import com.gravitysiege.gravitysiegegame.ui.theme.Ink
import com.gravitysiege.gravitysiegegame.ui.theme.InkMuted
import com.gravitysiege.gravitysiegegame.ui.theme.Line
import com.gravitysiege.gravitysiegegame.ui.theme.Panel
import com.gravitysiege.gravitysiegegame.ui.theme.formatCoins

@Composable
fun AssetImage(
    path: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
) {
    val context = LocalContext.current
    val image = remember(path) {
        AssetBitmaps.get(context, path)
        AssetBitmaps.imageBitmap(path)
    }
    image?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
            alignment = alignment,
        )
    }
}

@Composable
fun WhiteScreen(content: @Composable () -> Unit) {
    Box(
        Modifier
            .background(Color.White)
            .then(Modifier),
    ) { content() }
}

@Composable
fun CoinPill(coins: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Panel)
            .border(1.dp, Line, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.MonetizationOn, null, tint = Gold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(formatCoins(coins), color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Panel)
            .border(1.dp, Line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun BackButton(onClick: () -> Unit, dark: Boolean = true) {
    CircleIconButton(onClick) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = if (dark) Ink else Color.White,
        )
    }
}

@Composable
fun SettingsButton(onClick: () -> Unit) {
    CircleIconButton(onClick) {
        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Ink)
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onBack)
        Text(
            title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = Ink,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
        )
        Spacer(Modifier.size(46.dp))
    }
}

@Composable
fun AssetTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AssetImage("button_blank.webp", Modifier.matchParentSize(), ContentScale.FillBounds)
        Text(
            label,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
        )
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(subtitle, color = InkMuted, fontSize = 13.sp)
        }
    }
}
