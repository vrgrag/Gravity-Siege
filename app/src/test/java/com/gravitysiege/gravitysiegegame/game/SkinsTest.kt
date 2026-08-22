package com.gravitysiege.gravitysiegegame.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SkinsTest {

    @Test
    fun theStarterCrewIsFreeAndUnpainted() {
        assertTrue(Skins.DAY.free)
        assertNull(Skins.DAY.paint)
        assertEquals(1.0, Skins.DAY.tempo, 0.0)
        assertEquals(0.0, Skins.DAY.risk, 0.0)
        assertEquals(1.0, Skins.DAY.payout, 0.0)
    }

    @Test
    fun everyCrewHasItsOwnId() {
        assertEquals(Skins.ALL.size, Skins.ALL.map { it.id }.toSet().size)
    }

    @Test
    fun anUnknownIdFallsBackToTheStarterCrew() {
        assertEquals(Skins.DAY, Skins.byId("nobody"))
        assertEquals(Skins.DAY, Skins.byId(null))
        assertEquals(Skins.GOLD, Skins.byId("gold"))
    }

    @Test
    fun quickerCrewsPayMoreAndRiskMore() {
        val paid = Skins.ALL.filterNot { it.free }
        paid.forEach { crew ->
            if (crew.tempo < 1.0) {
                assertTrue("${crew.id} should pay above standard", crew.payout > 1.0)
                assertTrue("${crew.id} should carry extra risk", crew.risk > 0.0)
            } else {
                assertTrue("${crew.id} should pay below standard", crew.payout < 1.0)
                assertTrue("${crew.id} should be safer", crew.risk < 0.0)
            }
        }
    }

    @Test
    fun aCrewBonusRaisesWhatASeatedHousePays() {
        fun floorsAfter(payout: Double): Double {
            val engine = TowerEngine(Random(7))
            engine.payoutBias = payout
            engine.start(100)
            repeat(6) {
                if (engine.phase == RoundPhase.LIVE) {
                    engine.prepareDrop()
                    engine.commitDrop()
                }
            }
            return engine.multiplier
        }

        assertTrue(floorsAfter(1.45) > floorsAfter(1.0))
    }

    @Test
    fun aCrewHandicapMakesHousesSlipMoreOften() {
        fun slipsOutOf(rounds: Int, risk: Double): Int {
            val engine = TowerEngine(Random(11))
            var slips = 0
            repeat(rounds) {
                engine.riskBias = risk
                engine.resetToIdle()
                engine.start(100)
                if (!engine.prepareDrop().success) slips++
                engine.commitDrop()
            }
            return slips
        }

        assertTrue(slipsOutOf(400, risk = 0.20) > slipsOutOf(400, risk = 0.0))
    }
}
