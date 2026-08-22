package com.gravitysiege.gravitysiegegame.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.gravitysiege.gravitysiegegame.ui.components.FundsReadout
import com.gravitysiege.gravitysiegegame.ui.components.HazardTape
import com.gravitysiege.gravitysiegegame.ui.components.HazardYellow
import com.gravitysiege.gravitysiegegame.ui.components.PlateButton
import com.gravitysiege.gravitysiegegame.ui.components.SiteDivider
import com.gravitysiege.gravitysiegegame.ui.components.SiteHeader
import com.gravitysiege.gravitysiegegame.ui.components.SiteStat
import com.gravitysiege.gravitysiegegame.ui.components.SteelEdge
import com.gravitysiege.gravitysiegegame.ui.components.SteelKey
import com.gravitysiege.gravitysiegegame.ui.components.SteelPlate
import com.gravitysiege.gravitysiegegame.ui.components.SteelStep
import com.gravitysiege.gravitysiegegame.ui.components.SteelText
import com.gravitysiege.gravitysiegegame.ui.components.SteelWell
import com.gravitysiege.gravitysiegegame.ui.components.YardCanvas
import com.gravitysiege.gravitysiegegame.ui.theme.BuildGreen
import com.gravitysiege.gravitysiegegame.ui.theme.Danger
import com.gravitysiege.gravitysiegegame.ui.theme.Ink
import com.gravitysiege.gravitysiegegame.ui.theme.formatCoins
import com.gravitysiege.gravitysiegegame.ui.theme.formatMult
import kotlinx.coroutines.delay

@Composable
fun GameScreen(store: GameStore, sfx: Sfx, back: () -> Unit) {
    val activity = LocalContext.current as Activity
    val engine = remember { TowerEngine() }
    val stage = remember { SiegeStage() }
    var wheelLabel by remember { mutableStateOf<String?>(null) }
    var banner by remember { mutableStateOf<String?>(null) }
    val skin = store.skin

    LaunchedEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    // The crew on shift decides the rig's tempo and the risk-to-reward trade.
    LaunchedEffect(skin.id) {
        stage.tempo = skin.tempo
        engine.riskBias = skin.risk
        engine.payoutBias = skin.payout
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
                    when (drop?.kind) {
                        FloorKind.FROZEN -> {
                            sfx.chime()
                            banner = "FROZEN FLOOR · LOCKED AT ${formatMult(engine.frozenMult ?: engine.multiplier)}"
                        }
                        FloorKind.TEMPLE -> {
                            sfx.chime()
                            banner = "TEMPLE FLOOR"
                        }
                        FloorKind.TRIPLE -> {
                            sfx.success()
                            banner = "TRIPLE BUILD · THREE SAFE FLOORS"
                        }
                        else -> {
                            sfx.success()
                            banner = "${formatMult(drop?.odds ?: 1.0)} SEATED"
                        }
                    }
                    if (engine.phase == RoundPhase.CASHED) {
                        store.credit(engine.lastPayout)
                        stage.showerCoins()
                    }
                }
                Verdict.SLIPPED -> {
                    engine.commitDrop()
                    if (engine.lastPayout > 0) store.credit(engine.lastPayout)
                    sfx.error()
                    banner = if (engine.lastPayout > 0) {
                        "TOWER DOWN · FROZEN SAVE ${formatCoins(engine.lastPayout)}"
                    } else {
                        "TOWER COLLAPSED"
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
            wheelLabel = seg.label
            delay(1400)
            engine.applyWheel(seg)
            if (engine.phase == RoundPhase.CASHED) {
                store.credit(engine.lastPayout)
                stage.showerCoins()
            }
            banner = if (seg.freeze) "WHEEL · FROZEN LOCK" else "WHEEL · ${seg.label}"
            sfx.success()
            wheelLabel = null
        }
    }

    val hanging = engine.phase == RoundPhase.LIVE
    val resolved = engine.phase == RoundPhase.COLLAPSED || engine.phase == RoundPhase.CASHED
    val busy = engine.phase == RoundPhase.DROPPING ||
        engine.phase == RoundPhase.WHEEL ||
        engine.phase == RoundPhase.TRIPLE

    Box(Modifier.fillMaxSize()) {
        YardCanvas(stage, Modifier.fillMaxSize(), tint = skin.paint)

        Column(Modifier.fillMaxSize()) {
            SiteHeader {
                Column(
                    Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SteelKey(Icons.AutoMirrored.Filled.ArrowBack, "Back", size = 42.dp) {
                                if (hanging && engine.floors.isNotEmpty()) {
                                    store.credit(engine.cashOut())
                                }
                                back()
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                skin.title,
                                color = skin.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                            )
                        }
                        FundsReadout(store.coins)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SiteStat("MULTIPLIER", formatMult(engine.multiplier), HazardYellow)
                        SiteDivider(26.dp)
                        SiteStat("WIN", formatCoins(engine.potentialWin))
                        SiteDivider(26.dp)
                        SiteStat("FLOORS", engine.floors.size.toString())
                    }
                    engine.frozenMult?.let {
                        Text(
                            "FROZEN LOCK ${formatMult(it)} · ${formatCoins(engine.frozenWin)}",
                            color = Color(0xFF7FD8FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 4.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            banner?.let {
                Text(
                    it,
                    color = HazardYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF15181B).copy(alpha = 0.92f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
            if (resolved) {
                ResultPlate(engine, Modifier.padding(horizontal = 12.dp))
                Spacer(Modifier.height(8.dp))
            }
            Controls(
                store = store,
                engine = engine,
                stage = stage,
                sfx = sfx,
                busy = busy,
                hanging = hanging,
                onBanner = { banner = it },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(14.dp))
        }

        wheelLabel?.let { label ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                SteelPlate(Modifier.padding(horizontal = 40.dp)) {
                    Column(
                        Modifier.padding(horizontal = 34.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "TEMPLE WHEEL",
                            color = SteelText,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 2.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            label,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = HazardYellow,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultPlate(engine: TowerEngine, modifier: Modifier = Modifier) {
    val cashed = engine.phase == RoundPhase.CASHED
    SteelPlate(modifier.fillMaxWidth(), corner = 14.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (cashed) "SHIFT BANKED" else "TOWER DOWN",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 2.sp,
                color = if (cashed || engine.lastPayout > 0) BuildGreen else Danger,
            )
            Text(
                if (engine.lastPayout > 0) "+${formatCoins(engine.lastPayout)}" else "NO PAYOUT",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
            Text(
                "${engine.floors.size} FLOORS · ${formatMult(engine.multiplier)}",
                color = SteelText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
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
    onBanner: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val canBank = hanging && engine.floors.isNotEmpty() && stage.hooked
    val betLocked = busy || hanging
    SteelPlate(modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SteelStep(Icons.Filled.Remove, "Lower bet", !betLocked) {
                    sfx.click()
                    store.cycleBet(-1)
                }
                SteelWell(
                    Modifier
                        .weight(1f)
                        .height(44.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "BET",
                            color = SteelText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            formatCoins(store.bet),
                            color = HazardYellow,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                SteelStep(Icons.Filled.Add, "Raise bet", !betLocked) {
                    sfx.click()
                    store.cycleBet(1)
                }
                Text(
                    "x2",
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(HazardYellow.copy(alpha = if (betLocked) 0.25f else 0.9f))
                        .clickable(enabled = !betLocked) {
                            sfx.click()
                            store.doubleBet()
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Ink,
                )
            }
            Spacer(Modifier.height(10.dp))
            HazardTape(
                Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                stripe = 9.dp,
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlateButton(
                    label = if (hanging) "DROP" else "BUILD",
                    onClick = {
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
                                        onBanner("BANK GIFT +${formatCoins(gift)}")
                                    } else {
                                        sfx.error()
                                        onBanner("NOT ENOUGH COINS · LOWER THE BET")
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
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    height = 60.dp,
                    ink = Ink,
                )
                PlateButton(
                    label = "CASH OUT",
                    onClick = {
                        val paid = engine.cashOut()
                        store.credit(paid)
                        stage.showerCoins()
                        sfx.success()
                        onBanner("CASHED OUT ${formatCoins(paid)}")
                    },
                    enabled = canBank,
                    modifier = Modifier.weight(1f),
                    height = 60.dp,
                    ink = Ink,
                )
            }
        }
    }
}
