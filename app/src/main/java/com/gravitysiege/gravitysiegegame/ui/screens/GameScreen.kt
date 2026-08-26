package com.gravitysiege.gravitysiegegame.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravitysiege.gravitysiegegame.GameStore
import com.gravitysiege.gravitysiegegame.audio.Sfx
import com.gravitysiege.gravitysiegegame.game.FloorKind
import com.gravitysiege.gravitysiegegame.game.RoundPhase
import com.gravitysiege.gravitysiegegame.game.SiegeStage
import com.gravitysiege.gravitysiegegame.game.TowerEngine
import com.gravitysiege.gravitysiegegame.game.Verdict
import com.gravitysiege.gravitysiegegame.ui.components.HazardTape
import com.gravitysiege.gravitysiegegame.ui.components.HazardYellow
import com.gravitysiege.gravitysiegegame.ui.components.SiteDock
import com.gravitysiege.gravitysiegegame.ui.components.SiteHeader
import com.gravitysiege.gravitysiegegame.ui.components.SteelText
import com.gravitysiege.gravitysiegegame.ui.components.YardCanvas
import com.gravitysiege.gravitysiegegame.ui.theme.formatCoins
import com.gravitysiege.gravitysiegegame.ui.theme.formatMult
import kotlinx.coroutines.delay

private val MultGold = Color(0xFFE9B93F)
private val MultRed = Color(0xFFE03131)
private val ResultFill = Color(0xFF8B9583).copy(alpha = 0.62f)
private val ResultEdge = Color(0xFFC9C24F)
private val ChipBlueHigh = Color(0xFF57A2DA)
private val ChipBlueLow = Color(0xFF2A6BA8)

@Composable
fun GameScreen(store: GameStore, sfx: Sfx, back: () -> Unit) {
    val activity = LocalContext.current as Activity
    val engine = remember { TowerEngine() }
    val stage = remember { SiegeStage() }
    var banner by remember { mutableStateOf<String?>(null) }
    val results = remember { mutableStateListOf<Double>() }
    val skin = store.skin
    val mode = store.mode

    LaunchedEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    LaunchedEffect(skin.id, mode) {
        stage.tempo = skin.tempo
        engine.riskBias = skin.risk + mode.risk
        engine.payoutBias = skin.payout * mode.payout
    }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) stage.step((now - last) / 1_000_000_000.0)
                last = now
            }
            when (stage.takeVerdict()) {
                Verdict.SEATED -> {
                    val drop = engine.pending
                    engine.commitDrop()
                    store.recordHeight(engine.floors.size)
                    results.add(0, drop?.odds ?: engine.multiplier)
                    if (results.size > 8) results.removeRange(8, results.size)
                    when (drop?.kind) {
                        FloorKind.FROZEN -> {
                            sfx.chime()
                            banner = "Frozen lock ${formatMult(engine.frozenMult ?: engine.multiplier)}"
                        }
                        FloorKind.TEMPLE -> {
                            sfx.chime()
                            banner = "Temple Floor"
                        }
                        FloorKind.TRIPLE -> {
                            sfx.success()
                            banner = "Triple Build"
                        }
                        else -> {
                            sfx.success()
                            banner = null
                        }
                    }
                    if (engine.phase == RoundPhase.CASHED) {
                        store.credit(engine.lastPayout)
                        stage.showerCoins()
                    }
                }
                Verdict.SLIPPED -> {
                    engine.commitDrop()
                    results.add(0, 0.0)
                    if (results.size > 8) results.removeRange(8, results.size)
                    if (engine.lastPayout > 0) store.credit(engine.lastPayout)
                    sfx.error()
                    banner = if (engine.lastPayout > 0) {
                        "Frozen save ${formatCoins(engine.lastPayout)}"
                    } else {
                        null
                    }
                }
                null -> Unit
            }
        }
    }

    LaunchedEffect(engine.phase, engine.tripleLeft) {
        if (engine.phase == RoundPhase.TRIPLE && engine.tripleLeft > 0 && engine.pending == null) {
            delay(340)
            engine.prepareTripleDrop()
            stage.release(true)
        }
        if (engine.phase == RoundPhase.WHEEL) {
            val seg = engine.spinWheel()
            delay(900)
            engine.applyWheel(seg)
            results.add(0, if (seg.freeze) engine.multiplier else seg.mult)
            if (engine.phase == RoundPhase.CASHED) {
                store.credit(engine.lastPayout)
                stage.showerCoins()
            }
            banner = if (seg.freeze) "Wheel · Frozen" else "Wheel · ${seg.label}"
            sfx.success()
        }
    }

    val hanging = engine.phase == RoundPhase.LIVE
    val busy = engine.phase == RoundPhase.DROPPING ||
        engine.phase == RoundPhase.WHEEL ||
        engine.phase == RoundPhase.TRIPLE
    val canBank = hanging && engine.floors.isNotEmpty() && stage.hooked

    Box(Modifier.fillMaxSize()) {
        YardCanvas(stage, Modifier.fillMaxSize(), tint = skin.paint)
        MultiplierBurst(engine)
        ResultsRail(results)
        OptionsKey(onClick = { sfx.click() })

        Column(Modifier.fillMaxSize()) {
            SiteHeader(tape = false) {
                Row(
                    Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clickable {
                                if (canBank) store.credit(engine.cashOut())
                                back()
                            },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            formatCoins(store.coins),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "COINS",
                            color = SteelText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 1.dp),
                        )
                    }
                }
            }

            banner?.let {
                Text(
                    it,
                    color = HazardYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF15181B).copy(0.88f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            if (engine.phase == RoundPhase.COLLAPSED || engine.phase == RoundPhase.CASHED) {
                VerdictBanner(engine)
            }

            Controls(
                store = store,
                engine = engine,
                stage = stage,
                sfx = sfx,
                busy = busy,
                hanging = hanging,
                canBank = canBank,
                onBanner = { banner = it },
            )
        }
    }
}

@Composable
private fun MultiplierBurst(engine: TowerEngine) {
    val shown = when (engine.phase) {
        RoundPhase.COLLAPSED -> 0.0
        RoundPhase.IDLE -> null
        RoundPhase.CASHED -> engine.multiplier
        else -> if (engine.floors.isEmpty()) null else engine.multiplier
    } ?: return
    val color = if (shown <= 0.0) MultRed else MultGold
    val label = formatMult(shown)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.offset(y = (-140).dp), contentAlignment = Alignment.Center) {
            // Four offset copies stand in for the reference build's white outline.
            listOf(
                (-3).dp to (-3).dp,
                3.dp to (-3).dp,
                (-3).dp to 3.dp,
                3.dp to 3.dp,
            ).forEach { (dx, dy) ->
                Text(
                    label,
                    color = Color.White,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.offset(x = dx, y = dy),
                )
            }
            Text(label, color = color, fontSize = 58.sp, fontWeight = FontWeight.Black)
        }
    }
}

/** The round blue key the reference build parks under the header. */
@Composable
private fun OptionsKey(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 58.dp, end = 12.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF6FCBEC), Color(0xFF2F9AC8))))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.MoreHoriz,
                contentDescription = "Options",
                tint = Color.White,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun ResultsRail(results: List<Double>) {
    if (results.isEmpty()) return
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 150.dp, end = 12.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text("Results", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        results.take(8).forEach { value ->
            Box(
                Modifier
                    .padding(bottom = 6.dp)
                    .widthIn(min = 58.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(ResultFill)
                    .border(1.5.dp, ResultEdge, RoundedCornerShape(9.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    formatMult(value),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun Controls(
    store: GameStore,
    engine: TowerEngine,
    stage: SiegeStage,
    sfx: Sfx,
    busy: Boolean,
    hanging: Boolean,
    canBank: Boolean,
    onBanner: (String?) -> Unit,
) {
    val betLocked = busy || hanging

    fun startOrDrop() {
        sfx.click()
        when (engine.phase) {
            RoundPhase.IDLE, RoundPhase.COLLAPSED, RoundPhase.CASHED -> {
                engine.resetToIdle()
                stage.clear()
                onBanner(null)
                if (store.coins < store.bet) {
                    val gift = store.grantRelief()
                    if (gift > 0) {
                        sfx.chime()
                        onBanner("Bank gift +${formatCoins(gift)}")
                    } else {
                        sfx.error()
                        onBanner("Not enough coins")
                    }
                } else if (store.spend(store.bet)) {
                    engine.start(store.bet)
                }
            }
            RoundPhase.LIVE -> if (stage.ready) {
                val drop = engine.prepareDrop()
                stage.release(drop.success)
            }
            else -> Unit
        }
    }

    SiteDock {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!hanging) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    BlueChip("ALL IN", Modifier.weight(1f), enabled = !betLocked) {
                        sfx.click()
                        store.allIn()
                    }
                    BetWell(
                        bet = store.bet,
                        enabled = !betLocked,
                        modifier = Modifier.weight(1.7f),
                        onStep = { step ->
                            sfx.click()
                            store.cycleBet(step)
                        },
                    )
                    BlueChip("x2", Modifier.weight(0.7f), enabled = !betLocked) {
                        sfx.click()
                        store.doubleBet()
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (canBank) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CashoutKey(
                        amount = engine.potentialWin,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        val paid = engine.cashOut()
                        store.credit(paid)
                        stage.showerCoins()
                        sfx.success()
                        onBanner(null)
                    }
                    BuildKey(
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        onClick = { startOrDrop() },
                    )
                }
            } else {
                BuildKey(
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                    onClick = { startOrDrop() },
                )
            }
        }
    }
}

/** Blue pill used for ALL IN and the stake doubler. */
@Composable
private fun BlueChip(label: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier
            .height(46.dp)
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.verticalGradient(listOf(ChipBlueHigh, ChipBlueLow))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF3E4C58), Color(0xFF2A343C)))
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) Color.White else SteelText.copy(alpha = 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** Recessed stake field with the two round steppers tucked inside it. */
@Composable
private fun BetWell(bet: Int, enabled: Boolean, modifier: Modifier, onStep: (Int) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .height(46.dp)
            .clip(shape)
            .background(Color(0xFF23282E))
            .border(1.dp, Color(0xFF4B535B), shape)
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepKey(Icons.Filled.Remove, "Lower bet", enabled) { onStep(-1) }
        Text(
            formatCoins(bet),
            color = if (enabled) Color.White else SteelText.copy(alpha = 0.6f),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        StepKey(Icons.Filled.Add, "Raise bet", enabled) { onStep(1) }
    }
}

@Composable
private fun StepKey(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFF16191D))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) Color(0xFFB9C1C9) else SteelText.copy(alpha = 0.4f),
            modifier = Modifier.size(19.dp),
        )
    }
}

/** The site's main key: a yellow plate with warning tape along both lips. */
@Composable
private fun BuildKey(enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(58.dp)
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.verticalGradient(listOf(Color(0xFFDDAE33), Color(0xFFB9860F)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF6E6247), Color(0xFF554B37)))
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.fillMaxSize()) {
            HazardTape(Modifier.fillMaxWidth().height(11.dp), stripe = 9.dp)
            Spacer(Modifier.weight(1f))
            HazardTape(Modifier.fillMaxWidth().height(11.dp), stripe = 9.dp)
        }
        Text(
            "BUILD",
            color = if (enabled) Color(0xFFE4E4E4) else SteelText.copy(alpha = 0.6f),
            fontWeight = FontWeight.Black,
            fontSize = 19.sp,
            letterSpacing = 1.5.sp,
        )
    }
}

/** The banner the reference build slams over the street once a round settles. */
@Composable
private fun VerdictBanner(engine: TowerEngine, modifier: Modifier = Modifier) {
    val cashed = engine.phase == RoundPhase.CASHED
    val won = cashed || engine.lastPayout > 0
    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (won) {
                    Brush.verticalGradient(listOf(Color(0xFF2A7F63), Color(0xFF10362F)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF1E5F6E), Color(0xFF10333E)))
                },
            )
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (won) "YOU WIN" else "OOPS",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            letterSpacing = 3.sp,
        )
        if (won) {
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatCoins(engine.lastPayout),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                )
                Spacer(Modifier.width(5.dp))
                Text("COINS", color = Color.White.copy(0.7f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CashoutKey(amount: Int, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .height(58.dp)
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.verticalGradient(listOf(Color(0xFF4C8CBE), Color(0xFF27577F)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF44515C), Color(0xFF2C353D)))
                },
            )
            .border(1.5.dp, Color(0xFF7FB4DA).copy(if (enabled) 0.8f else 0.2f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "CASHOUT",
            color = Color(0xFFDCE6EE),
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            letterSpacing = 0.5.sp,
        )
        Spacer(Modifier.height(1.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatCoins(amount), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.width(4.dp))
            Text("COINS", color = Color(0xFF9FB3C4), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}
