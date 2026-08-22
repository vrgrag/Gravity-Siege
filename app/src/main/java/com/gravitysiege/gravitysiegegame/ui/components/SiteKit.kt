package com.gravitysiege.gravitysiegegame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravitysiege.gravitysiegegame.ui.theme.formatCoins

val SiteInk = Color(0xFF15181B)
val SteelHigh = Color(0xFF3C444D)
val SteelLow = Color(0xFF1F242A)
val SteelEdge = Color(0xFF5C666F)
val SteelText = Color(0xFF9AA4AE)
val HazardYellow = Color(0xFFF5C012)

/** Diagonal warning tape, used as trim on every site panel. */
@Composable
fun HazardTape(modifier: Modifier, stripe: Dp = 14.dp) {
    Canvas(modifier) {
        drawRect(SiteInk)
        val band = stripe.toPx()
        var x = -size.height
        while (x < size.width + size.height) {
            val wedge = Path().apply {
                moveTo(x, size.height)
                lineTo(x + size.height, 0f)
                lineTo(x + size.height + band, 0f)
                lineTo(x + band, size.height)
                close()
            }
            drawPath(wedge, HazardYellow)
            x += band * 2
        }
    }
}

/** Brushed steel plate with a bevelled edge and a rivet in each corner. */
@Composable
fun SteelPlate(
    modifier: Modifier = Modifier,
    corner: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(SteelHigh, SteelLow)))
            .border(2.dp, SteelEdge, shape),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val r = 3.5.dp.toPx()
            val inset = 13.dp.toPx()
            listOf(
                Offset(inset, inset),
                Offset(size.width - inset, inset),
                Offset(inset, size.height - inset),
                Offset(size.width - inset, size.height - inset),
            ).forEach { spot ->
                drawCircle(Color(0xFF8B949E), r, spot)
                drawCircle(Color(0xFF15181B), r * 0.42f, spot)
            }
        }
        content()
    }
}

/** Round steel key for icon actions. A [badge] pins an alert dot to its corner. */
@Composable
fun SteelKey(
    icon: ImageVector,
    label: String,
    size: Dp = 46.dp,
    badge: Boolean = false,
    onClick: () -> Unit,
) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(SteelHigh, SteelLow)))
                .border(2.dp, SteelEdge, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = HazardYellow,
                modifier = Modifier.size(size * 0.5f),
            )
        }
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(size * 0.26f)
                    .clip(CircleShape)
                    .background(Color(0xFFE23B3B))
                    .border(1.5.dp, SiteInk, CircleShape),
            )
        }
    }
}

/** A steel key with its purpose stencilled underneath. */
@Composable
fun SiteAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    badge: Boolean = false,
    onClick: () -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SteelKey(icon, label, size = 48.dp, badge = badge, onClick = onClick)
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            color = SteelText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}

/** The game's currency: a plain struck coin. */
@Composable
fun Coin(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val middle = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(Color(0xFF8A5E05), r, middle)
        drawCircle(Color(0xFFF5C012), r * 0.88f, middle)
        drawCircle(Color(0xFFC48A00), r * 0.62f, middle)
        drawCircle(Color(0xFFFFDC63), r * 0.54f, middle)
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = r * 0.16f,
            center = Offset(middle.x - r * 0.3f, middle.y - r * 0.36f),
        )
    }
}

/** Recessed dark field for values sitting inside a steel plate. */
@Composable
fun SteelWell(
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .clip(shape)
            .background(Color(0xFF14171A))
            .border(1.5.dp, SteelEdge.copy(alpha = 0.7f), shape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Square steel key for the bet stepper. */
@Composable
fun SteelStep(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(44.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(SteelHigh, SteelLow)))
            .border(1.5.dp, SteelEdge, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) Color.White else SteelText.copy(alpha = 0.45f),
        )
    }
}

/** Tab in the risk selector: filled in its own colour when it is the live one. */
@Composable
fun ModePill(
    label: String,
    active: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (active) tint else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = when {
                active -> SiteInk
                enabled -> SteelText
                else -> SteelText.copy(alpha = 0.4f)
            },
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Black else FontWeight.Bold,
        )
    }
}

/** Compact pill action, used for ALL IN and the stake doubler. */
@Composable
fun ChipButton(
    label: String,
    face: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (enabled) face else face.copy(alpha = 0.3f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.45f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
        )
    }
}

/** Small stencilled readout used across the site panels. */
@Composable
fun SiteStat(label: String, value: String, tint: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            color = SteelText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
        )
        Text(value, color = tint, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

/** Thin vertical rule between readouts. */
@Composable
fun SiteDivider(height: Dp = 30.dp) {
    Box(
        Modifier
            .width(1.dp)
            .height(height)
            .background(SteelEdge.copy(alpha = 0.6f)),
    )
}

/** The balance display: a coin and a number, nothing else. */
@Composable
fun FundsReadout(coins: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Coin(size = 26.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            formatCoins(coins),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
        )
    }
}

/** A coin next to an amount, for prices and rewards inside panels. */
@Composable
fun CoinAmount(
    amount: Int,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    fontSize: TextUnit = 18.sp,
    coinSize: Dp = 18.dp,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Coin(size = coinSize)
        Spacer(Modifier.width(6.dp))
        Text(formatCoins(amount), color = tint, fontSize = fontSize, fontWeight = FontWeight.Black)
    }
}

/** Dark header bar with hazard trim along its bottom edge. */
@Composable
fun SiteHeader(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF2C333B), Color(0xFF171B20)))),
            content = content,
        )
        HazardTape(
            Modifier
                .fillMaxWidth()
                .height(9.dp),
            stripe = 11.dp,
        )
    }
}

/**
 * The header's mirror image for the bottom of the screen: hazard trim on top
 * and steel running edge to edge, so the controls read as one solid dock
 * rather than a panel floating over the yard.
 */
@Composable
fun SiteDock(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        HazardTape(
            Modifier
                .fillMaxWidth()
                .height(9.dp),
            stripe = 11.dp,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF2C333B), Color(0xFF13171C)))),
            content = content,
        )
    }
}
