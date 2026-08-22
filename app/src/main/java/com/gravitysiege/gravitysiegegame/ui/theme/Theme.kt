package com.gravitysiege.gravitysiegegame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

val Sky = Color(0xFF4EB6F0)
val SkyDeep = Color(0xFF1E7FBF)
val Night = Color(0xFF0B1A2A)
val Gold = Color(0xFFF2B705)
val GoldDark = Color(0xFFC48A00)
val Ink = Color(0xFF1B1B1B)
val InkMuted = Color(0xFF5C5C5C)
val Panel = Color(0xFFF4F6F8)
val Line = Color(0xFFE1E4E8)
val BuildGreen = Color(0xFF2EAE4F)
val CashOrange = Color(0xFFFF8A1F)
val Danger = Color(0xFFE23B3B)

private val scheme = lightColorScheme(
    primary = SkyDeep,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
)

@Composable
fun SiegeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

private val symbols = DecimalFormatSymbols(Locale.US)
private val coinFormat = DecimalFormat("#,###", symbols)
private val multFormat = DecimalFormat("0.00", symbols)

fun formatCoins(value: Int): String = coinFormat.format(value)
fun formatMult(value: Double): String = "x" + multFormat.format(value)
