package com.gravitysiege.gravitysiegegame.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravitysiege.gravitysiegegame.AssetBitmaps
import com.gravitysiege.gravitysiegegame.ui.theme.Gold
import com.gravitysiege.gravitysiegegame.ui.theme.Ink
import com.gravitysiege.gravitysiegegame.ui.theme.InkMuted
import com.gravitysiege.gravitysiegegame.ui.theme.Line
import com.gravitysiege.gravitysiegegame.ui.theme.Night
import com.gravitysiege.gravitysiegegame.ui.theme.Panel
import com.gravitysiege.gravitysiegegame.ui.theme.formatCoins

@Composable
fun AssetImage(
    path: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    colorFilter: ColorFilter? = null,
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
            colorFilter = colorFilter,
        )
    }
}

@Composable
fun GameTitle(modifier: Modifier = Modifier, height: Dp = 150.dp) {
    AssetImage(
        "trim_Game_Name.webp",
        modifier
            .fillMaxWidth()
            .height(height),
        ContentScale.Fit,
    )
}

@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    light: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(size)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(if (light) Color.White.copy(0.9f) else Panel)
            .border(1.4.dp, if (light) Gold else Line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun BackButton(onClick: () -> Unit, light: Boolean = false) {
    CircleIconButton(onClick, light = light) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
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
fun PlateButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
    enabled: Boolean = true,
    ink: Color = Color.White,
    fontSize: TextUnit = 22.sp,
    /** Optional second line spelling out what the button will actually do. */
    note: String? = null,
) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AssetImage("button_blank.webp", Modifier.matchParentSize(), ContentScale.FillBounds)
        if (!enabled) {
            Box(Modifier.matchParentSize().background(Night.copy(alpha = 0.35f)))
        }
        if (note == null) {
            Text(
                label,
                color = ink,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(
                    label,
                    color = ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    note,
                    color = ink.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
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
            .border(1.dp, Line, RoundedCornerShape(16.dp))
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
