package com.gravitysiege.gravitysiegegame.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TowerEngineTest {
    @Test
    fun collapseChanceRisesWithHeight() {
        assertEquals(0.09, TowerEngine.collapseChance(0), 0.0001)
        assertTrue(TowerEngine.collapseChance(5) > TowerEngine.collapseChance(0))
        assertEquals(0.40, TowerEngine.collapseChance(80), 0.0001)
    }

    @Test
    fun successfulFloorsCompoundMultiplier() {
        val engine = TowerEngine(AlwaysLandRandom())
        engine.start(100)
        repeat(3) {
            engine.prepareDrop()
            engine.commitDrop()
        }
        assertEquals(3, engine.floors.size)
        val product = engine.floors.fold(1.0) { acc, floor -> acc * floor.odds }
        assertEquals(product.coerceAtMost(TowerEngine.MAX_MULT), engine.multiplier, 0.0001)
        assertEquals(RoundPhase.LIVE, engine.phase)
        val payout = engine.cashOut()
        assertEquals(engine.lastPayout, payout)
        assertTrue(payout >= 100)
        assertEquals(RoundPhase.CASHED, engine.phase)
    }

    @Test
    fun missedFloorCollapsesWithoutPayout() {
        val engine = TowerEngine(AlwaysMissRandom())
        engine.start(50)
        engine.prepareDrop()
        engine.commitDrop()
        assertTrue(engine.floors.isEmpty())
        assertEquals(RoundPhase.COLLAPSED, engine.phase)
        assertEquals(0, engine.lastPayout)
    }

    @Test
    fun frozenFloorPaysOnCollapse() {
        val engine = TowerEngine(AlwaysLandRandom())
        engine.start(200)
        engine.prepareDrop()
        engine.commitDrop()
        val locked = engine.multiplier
        engine.lockFrozenForTest()
        engine.collapseForTest()
        assertEquals(RoundPhase.COLLAPSED, engine.phase)
        assertTrue(engine.lastPayout > 0)
        assertEquals((200 * locked).toInt(), engine.lastPayout)
    }
}

private class AlwaysLandRandom : Random() {
    override fun nextBits(bitCount: Int): Int = 1.shl(bitCount - 1) - 1
    override fun nextDouble(): Double = 0.99
    override fun nextInt(until: Int): Int = 0
}

private class AlwaysMissRandom : Random() {
    override fun nextBits(bitCount: Int): Int = 0
    override fun nextDouble(): Double = 0.0
    override fun nextInt(until: Int): Int = 0
}

