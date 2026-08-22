package com.gravitysiege.gravitysiegegame.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravitysiege.gravitysiegegame.GameStore
import com.gravitysiege.gravitysiegegame.audio.Sfx
import com.gravitysiege.gravitysiegegame.game.FloorKind
import com.gravitysiege.gravitysiegegame.game.PendingDrop
import com.gravitysiege.gravitysiegegame.game.RoundPhase
import com.gravitysiege.gravitysiegegame.game.TowerEngine
import com.gravitysiege.gravitysiegegame.ui.components.AssetImage
import com.gravitysiege.gravitysiegegame.ui.components.BackButton
import com.gravitysiege.gravitysiegegame.ui.components.CoinPill
import com.gravitysiege.gravitysiegegame.ui.theme.BuildGreen
import com.gravitysiege.gravitysiegegame.ui.theme.CashOrange
import com.gravitysiege.gravitysiegegame.ui.theme.Danger
import com.gravitysiege.gravitysiegegame.ui.theme.Gold
import com.gravitysiege.gravitysiegegame.ui.theme.Ink
import com.gravitysiege.gravitysiegegame.ui.theme.InkMuted
import com.gravitysiege.gravitysiegegame.ui.theme.formatCoins
import com.gravitysiege.gravitysiegegame.ui.theme.formatMult
import kotlin.math.sin

@Composable
fun GameScreen(store: GameStore, sfx: Sfx, back: () -> Unit) {
    val activity = LocalContext.current as Activity
    val engine = remember { TowerEngine() }
    val dropAnim = remember { Animatable(0f) }
    var wheelLabel by remember { mutableStateOf<String?>(null) }
    var banner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    LaunchedEffect(engine.pending) {
        val drop = engine.pending ?: return@LaunchedEffect
        dropAnim.snapTo(0f)
        dropAnim.animateTo(1f, tween(560))
        engine.commitDrop()
        store.recordHeight(engine.floors.size)
        when {
            engine.phase == RoundPhase.COLLAPSED -> {
                if (engine.lastPayout > 0) store.credit(engine.lastPayout)
                sfx.error()
                banner = if (engine.lastPayout > 0) {
                    "Tower down · Frozen save ${formatCoins(engine.lastPayout)}"
                } else {
                    "Tower collapsed"
                }
            }
            !drop.success -> Unit
            drop.kind == FloorKind.FROZEN -> {
                sfx.chime()
                banner = "Frozen Floor · win locked at ${formatMult(engine.frozenMult ?: engine.multiplier)}"
            }
            drop.kind == FloorKind.TRIPLE -> {
                sfx.success()
                banner = "Triple Build · three safe floors"
            }
            drop.kind == FloorKind.TEMPLE -> {
                sfx.chime()
                banner = "Temple Floor"
            }
            else -> {
                sfx.success()
                banner = "${formatMult(drop.odds)} landed"
            }
        }
        if (engine.phase == RoundPhase.CASHED && engine.lastPayout > 0) {
            store.credit(engine.lastPayout)
        }
    }

    LaunchedEffect(engine.phase, engine.tripleLeft) {
        if (engine.phase == RoundPhase.TRIPLE && engine.tripleLeft > 0 && engine.pending == null) {
            kotlinx.coroutines.delay(280)
            engine.prepareTripleDrop()
        }
        if (engine.phase == RoundPhase.WHEEL) {
            val seg = engine.spinWheel()
            wheelLabel = seg.label
            kotlinx.coroutines.delay(1400)
            engine.applyWheel(seg)
            if (engine.phase == RoundPhase.CASHED) {
                store.credit(engine.lastPayout)
            }
            banner = if (seg.freeze) "Wheel · Frozen lock" else "Wheel · ${seg.label}"
            sfx.success()
            wheelLabel = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        AssetImage("bg_sky_asset.webp", Modifier.fillMaxSize(), ContentScale.Crop)
        DriftingClouds()
        TowerStage(engine, dropAnim.value)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onClick = {
                    if (engine.phase == RoundPhase.LIVE && engine.floors.isNotEmpty()) {
                        val paid = engine.cashOut()
                        store.credit(paid)
                    }
                    back()
                })
                CoinPill(store.coins)
            }
            Spacer(Modifier.height(10.dp))
            StatusStrip(engine, banner)
            Spacer(Modifier.weight(1f))
            if (engine.phase == RoundPhase.COLLAPSED || engine.phase == RoundPhase.CASHED) {
                ResultCard(engine) {
                    engine.resetToIdle()
                    banner = null
                }
                Spacer(Modifier.height(10.dp))
            }
            Controls(
                store = store,
                engine = engine,
                busy = engine.phase == RoundPhase.DROPPING ||
                    engine.phase == RoundPhase.WHEEL ||
                    engine.phase == RoundPhase.TRIPLE,
                onBuild = {
                    when (engine.phase) {
                        RoundPhase.IDLE, RoundPhase.COLLAPSED, RoundPhase.CASHED -> {
                            engine.resetToIdle()
                            if (store.coins < store.bet) {
                                val gift = store.grantRelief()
                                if (gift > 0) {
                                    sfx.chime()
                                    banner = "Bank gift +${formatCoins(gift)}"
                                } else {
                                    sfx.error()
                                    banner = "Not enough coins"
                                }
                                return@Controls
                            }
                            if (store.spend(store.bet)) {
                                engine.start(store.bet)
                                engine.prepareDrop()
                                banner = null
                            }
                        }
                        RoundPhase.LIVE -> engine.prepareDrop()
                        else -> Unit
                    }
                },
                onCash = {
                    if (engine.phase == RoundPhase.LIVE && engine.floors.isNotEmpty()) {
                        val paid = engine.cashOut()
                        store.credit(paid)
                        sfx.success()
                        banner = "Cashed out ${formatCoins(paid)}"
                    }
                },
                sfx = sfx,
            )
        }
        wheelLabel?.let { label ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("TEMPLE WHEEL", fontWeight = FontWeight.Black, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text(label, fontSize = 40.sp, fontWeight = FontWeight.Black, color = CashOrange)
                }
            }
        }
    }
}

@Composable
private fun StatusStrip(engine: TowerEngine, banner: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.88f))
            .padding(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Multiplier", formatMult(engine.multiplier))
            Stat("Win", formatCoins(engine.potentialWin))
            Stat("Floors", engine.floors.size.toString())
        }
        engine.frozenMult?.let {
            Text(
                "Frozen lock ${formatMult(it)} · ${formatCoins(engine.frozenWin)}",
                color = SkyFreeze,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        banner?.let {
            Text(it, color = InkMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

private val SkyFreeze = Color(0xFF1E7FBF)

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, color = InkMuted, fontSize = 11.sp)
        Text(value, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
    }
}

@Composable
private fun ResultCard(engine: TowerEngine, onContinue: () -> Unit) {
    val win = engine.phase == RoundPhase.CASHED || engine.lastPayout > 0
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (engine.phase == RoundPhase.CASHED) "CASHED OUT" else "TOWER DOWN",
            fontWeight = FontWeight.Black,
            color = if (win) BuildGreen else Danger,
        )
        Text(
            if (engine.lastPayout > 0) "+${formatCoins(engine.lastPayout)}" else "No payout",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Ink,
        )
        Text(
            "${engine.floors.size} floors · ${formatMult(engine.multiplier)}",
            color = InkMuted,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Tap Build to play again",
            color = InkMuted,
            modifier = Modifier.clickable(onClick = onContinue),
        )
    }
}

@Composable
private fun Controls(
    store: GameStore,
    engine: TowerEngine,
    busy: Boolean,
    onBuild: () -> Unit,
    onCash: () -> Unit,
    sfx: Sfx,
) {
    val live = engine.phase == RoundPhase.LIVE && engine.floors.isNotEmpty()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.94f))
            .padding(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CircleAction(enabled = !busy && !live, onClick = { sfx.click(); store.cycleBet(-1) }) {
                Icon(Icons.Filled.Remove, null, tint = Ink)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BET", color = InkMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(formatCoins(store.bet), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
            CircleAction(enabled = !busy && !live, onClick = { sfx.click(); store.cycleBet(1) }) {
                Icon(Icons.Filled.Add, null, tint = Ink)
            }
            Text(
                "x2",
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Gold.copy(alpha = 0.25f))
                    .clickable(enabled = !busy && !live) {
                        sfx.click()
                        store.doubleBet()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                fontWeight = FontWeight.Black,
                color = Ink,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionKey(
                title = if (live) "BUILD" else "BUILD",
                color = BuildGreen,
                enabled = !busy && engine.phase != RoundPhase.WHEEL,
                modifier = Modifier.weight(1f),
                onClick = {
                    sfx.click()
                    onBuild()
                },
            )
            ActionKey(
                title = "CASH OUT",
                color = CashOrange,
                enabled = !busy && live,
                modifier = Modifier.weight(1f),
                onClick = onCash,
            )
        }
    }
}

@Composable
private fun CircleAction(enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF0F2F5))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ActionKey(
    title: String,
    color: Color,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) color else color.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

@Composable
private fun DriftingClouds() {
    val motion = rememberInfiniteTransition(label = "clouds")
    val a by motion.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "c1",
    )
    val b by motion.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "c2",
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        AssetImage(
            "cloud_asset_01.webp",
            Modifier
                .width(160.dp)
                .height(70.dp)
                .offset(x = w * (a * 1.3f - 0.2f), y = 90.dp),
            ContentScale.Fit,
        )
        AssetImage(
            "cloud_asset_02.webp",
            Modifier
                .width(190.dp)
                .height(80.dp)
                .offset(x = w * (0.9f - b * 1.2f), y = 160.dp),
            ContentScale.Fit,
        )
    }
}

@Composable
private fun TowerStage(engine: TowerEngine, dropProgress: Float) {
    val motion = rememberInfiniteTransition(label = "hook")
    val swing by motion.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "swing",
    )
    val hookShift = if (engine.pending != null) {
        engine.pending!!.wobble
    } else {
        (sin(swing * Math.PI * 2.0) * 0.22).toFloat()
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(bottom = 210.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val density = LocalDensity.current
        val stageW = maxWidth
        val stageH = maxHeight
        val floorH = 92.dp
        val baseH = 118.dp
        val visibleFloors = engine.floors.takeLast(5)
        val hidden = (engine.floors.size - visibleFloors.size).coerceAtLeast(0)
        val stackH = baseH + floorH * visibleFloors.size
        val collapse = engine.phase == RoundPhase.COLLAPSED

        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                visibleFloors.asReversed().forEachIndexed { visualIndex, floor ->
                    val fall = if (collapse) (visualIndex + 1) * 18f else 0f
                    val tilt = if (collapse) (if (visualIndex % 2 == 0) -14f else 16f) else 0f
                    FloorSprite(
                        path = floor.asset,
                        height = floorH,
                        wobble = floor.wobble,
                        fall = fall,
                        tilt = tilt,
                        badge = floorBadge(floor.kind, floor.odds),
                    )
                }
                AssetImage(
                    TowerEngine.BASE_ASSET,
                    Modifier
                        .width(220.dp)
                        .height(baseH),
                    ContentScale.Fit,
                )
            }
            val pending = engine.pending
            val hookY = 8.dp
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .offset(x = stageW * hookShift * 0.5f, y = hookY),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AssetImage("hook_asset.webp", Modifier.size(86.dp), ContentScale.Fit)
                    if (pending != null) {
                        val travel = with(density) { (stageH - stackH - 96.dp).coerceAtLeast(80.dp).toPx() }
                        val y = travel * dropProgress
                        FloorSprite(
                            path = pending.asset,
                            height = floorH,
                            wobble = 0f,
                            fall = y / density.density,
                            tilt = if (pending.success) 0f else dropProgress * 18f,
                            badge = if (dropProgress > 0.85f && pending.success) {
                                floorBadge(pending.kind, pending.odds)
                            } else null,
                            ghost = dropProgress < 1f,
                        )
                    } else if (engine.phase == RoundPhase.LIVE || engine.phase == RoundPhase.IDLE) {
                        AssetImage(
                            "block_asset_02.webp",
                            Modifier
                                .width(120.dp)
                                .height(72.dp),
                            ContentScale.Fit,
                        )
                    }
                }
            }
        }
        if (hidden > 0) {
            Text(
                "+$hidden below",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(0.35f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun FloorSprite(
    path: String,
    height: androidx.compose.ui.unit.Dp,
    wobble: Float,
    fall: Float,
    tilt: Float,
    badge: String?,
    ghost: Boolean = false,
) {
    Box(
        Modifier
            .offset(x = (wobble * 48).dp, y = fall.dp)
            .rotate(tilt)
            .width(150.dp)
            .height(height),
        contentAlignment = Alignment.Center,
    ) {
        AssetImage(path, Modifier.fillMaxSize(), ContentScale.Fit)
        badge?.let {
            Text(
                it,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(if (ghost) 0.25f else 0.55f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

private fun floorBadge(kind: FloorKind, odds: Double): String = when (kind) {
    FloorKind.FROZEN -> "FROZEN"
    FloorKind.TEMPLE -> "TEMPLE"
    FloorKind.TRIPLE -> "TRIPLE"
    FloorKind.NORMAL -> formatMult(odds)
}
