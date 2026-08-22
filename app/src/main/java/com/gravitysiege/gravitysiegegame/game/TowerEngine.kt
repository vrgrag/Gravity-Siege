package com.gravitysiege.gravitysiegegame.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.floor
import kotlin.random.Random

enum class FloorKind { NORMAL, FROZEN, TEMPLE, TRIPLE }

enum class RoundPhase {
    IDLE,
    LIVE,
    DROPPING,
    WHEEL,
    TRIPLE,
    COLLAPSED,
    CASHED,
}

data class PlacedFloor(
    val asset: String,
    val kind: FloorKind,
    val odds: Double,
    val wobble: Float,
)

data class PendingDrop(
    val asset: String,
    val kind: FloorKind,
    val odds: Double,
    val success: Boolean,
    val wobble: Float,
)

data class WheelSeg(
    val label: String,
    val mult: Double,
    val freeze: Boolean,
)

class TowerEngine(private val rng: Random = Random.Default) {
    var phase by mutableStateOf(RoundPhase.IDLE)
        private set
    var bet by mutableIntStateOf(0)
        private set
    var multiplier by mutableDoubleStateOf(1.0)
        private set
    var frozenMult by mutableStateOf<Double?>(null)
        private set
    var lastPayout by mutableIntStateOf(0)
        private set
    var pending by mutableStateOf<PendingDrop?>(null)
        private set
    var tripleLeft by mutableIntStateOf(0)
        private set
    val floors = mutableStateListOf<PlacedFloor>()

    val potentialWin: Int
        get() = if (bet <= 0) 0 else floor(bet * multiplier).toInt()

    val frozenWin: Int
        get() = frozenMult?.let { floor(bet * it).toInt() } ?: 0

    fun canStart(balance: Int, stake: Int): Boolean {
        return phase in listOf(RoundPhase.IDLE, RoundPhase.COLLAPSED, RoundPhase.CASHED) &&
            stake > 0 &&
            balance >= stake
    }

    fun start(stake: Int) {
        require(stake > 0)
        bet = stake
        multiplier = 1.0
        frozenMult = null
        lastPayout = 0
        pending = null
        tripleLeft = 0
        floors.clear()
        phase = RoundPhase.LIVE
    }

    fun resetToIdle() {
        phase = RoundPhase.IDLE
        pending = null
        tripleLeft = 0
    }

    fun prepareDrop(): PendingDrop {
        check(phase == RoundPhase.LIVE) { "Build is only available while the round is live" }
        val drop = rollDrop(guaranteed = false)
        pending = drop
        phase = RoundPhase.DROPPING
        return drop
    }

    fun prepareTripleDrop(): PendingDrop {
        check(phase == RoundPhase.TRIPLE && tripleLeft > 0)
        val drop = rollDrop(guaranteed = true)
        pending = drop
        phase = RoundPhase.DROPPING
        return drop
    }

    fun commitDrop() {
        val drop = pending ?: return
        pending = null
        if (!drop.success) {
            lastPayout = frozenWin
            phase = RoundPhase.COLLAPSED
            return
        }
        floors.add(PlacedFloor(drop.asset, drop.kind, drop.odds, drop.wobble))
        multiplier = (multiplier * drop.odds).coerceAtMost(MAX_MULT)
        if (drop.kind == FloorKind.FROZEN) {
            lockFrozen()
        }
        if (multiplier >= MAX_MULT) {
            cashOut()
            return
        }
        when {
            drop.kind == FloorKind.TEMPLE -> phase = RoundPhase.WHEEL
            drop.kind == FloorKind.TRIPLE -> {
                tripleLeft = TRIPLE_FLOORS
                phase = RoundPhase.TRIPLE
            }
            tripleLeft > 0 -> {
                tripleLeft -= 1
                phase = if (tripleLeft > 0) RoundPhase.TRIPLE else RoundPhase.LIVE
            }
            else -> phase = RoundPhase.LIVE
        }
    }

    fun applyWheel(seg: WheelSeg) {
        check(phase == RoundPhase.WHEEL)
        if (seg.freeze) {
            lockFrozen()
        } else {
            multiplier = (multiplier * seg.mult).coerceAtMost(MAX_MULT)
        }
        if (multiplier >= MAX_MULT) {
            cashOut()
        } else {
            phase = RoundPhase.LIVE
        }
    }

    fun cashOut(): Int {
        check(phase == RoundPhase.LIVE || phase == RoundPhase.WHEEL)
        val payout = potentialWin
        lastPayout = payout
        phase = RoundPhase.CASHED
        pending = null
        tripleLeft = 0
        return payout
    }

    fun spinWheel(): WheelSeg = WHEEL_SEGS[rng.nextInt(WHEEL_SEGS.size)]

    internal fun lockFrozenForTest() {
        lockFrozen()
    }

    internal fun collapseForTest() {
        lastPayout = frozenWin
        pending = null
        tripleLeft = 0
        phase = RoundPhase.COLLAPSED
    }

    private fun lockFrozen() {
        frozenMult = maxOf(frozenMult ?: 0.0, multiplier)
    }

    private fun rollDrop(guaranteed: Boolean): PendingDrop {
        val success = guaranteed || rng.nextDouble() >= collapseChance(floors.size)
        val kind = when {
            !success -> FloorKind.NORMAL
            guaranteed -> FloorKind.NORMAL
            else -> rollKind()
        }
        val odds = when {
            !success -> 0.0
            guaranteed -> rollOdds(min = 1.0)
            else -> rollOdds(min = if (kind == FloorKind.TRIPLE) 1.0 else 0.7)
        }
        return PendingDrop(
            asset = HOUSES[rng.nextInt(HOUSES.size)],
            kind = kind,
            odds = odds,
            success = success,
            wobble = (rng.nextFloat() - 0.5f) * 0.22f,
        )
    }

    private fun rollKind(): FloorKind {
        val roll = rng.nextDouble()
        return when {
            roll < 0.07 -> FloorKind.FROZEN
            roll < 0.12 -> FloorKind.TEMPLE
            roll < 0.16 -> FloorKind.TRIPLE
            else -> FloorKind.NORMAL
        }
    }

    private fun rollOdds(min: Double): Double {
        val roll = rng.nextDouble()
        val raw = when {
            roll < 0.16 -> rng.nextDouble(0.70, 0.96)
            roll < 0.62 -> rng.nextDouble(1.10, 1.45)
            roll < 0.88 -> rng.nextDouble(1.45, 1.90)
            else -> rng.nextDouble(1.90, 2.40)
        }
        val stepped = (raw * 100.0).toInt() / 100.0
        return stepped.coerceAtLeast(min)
    }

    companion object {
        const val MAX_MULT = 100.0
        const val TRIPLE_FLOORS = 3
        const val BASE_ASSET = Yard.PLINTH
        val HOUSES = Yard.HOUSES
        val WHEEL_SEGS = listOf(
            WheelSeg("x1.5", 1.5, false),
            WheelSeg("x2", 2.0, false),
            WheelSeg("x3", 3.0, false),
            WheelSeg("x5", 5.0, false),
            WheelSeg("x7", 7.0, false),
            WheelSeg("x1.5", 1.5, false),
            WheelSeg("x2", 2.0, false),
            WheelSeg("x3", 3.0, false),
            WheelSeg("FREEZE", 1.0, true),
            WheelSeg("x1.5", 1.5, false),
        )

        fun collapseChance(floorIndex: Int): Double {
            return (0.09 + floorIndex * 0.028).coerceAtMost(0.40)
        }
    }
}
