package com.gravitysiege.gravitysiegegame.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskModeTest {

    @Test
    fun cruiseIsTheNeutralSetting() {
        assertEquals(0.0, RiskMode.CRUISE.risk, 0.0)
        assertEquals(1.0, RiskMode.CRUISE.payout, 0.0)
    }

    @Test
    fun everyStepUpRaisesBothTheRiskAndTheReward() {
        RiskMode.entries.zipWithNext { tamer, wilder ->
            assertTrue("${wilder.label} should be riskier than ${tamer.label}", wilder.risk > tamer.risk)
            assertTrue("${wilder.label} should pay more than ${tamer.label}", wilder.payout > tamer.payout)
        }
    }

    @Test
    fun anUnknownNameFallsBackToCruise() {
        assertEquals(RiskMode.CRUISE, RiskMode.byName(null))
        assertEquals(RiskMode.CRUISE, RiskMode.byName("SLEEPWALK"))
        assertEquals(RiskMode.MELTDOWN, RiskMode.byName("MELTDOWN"))
    }

    @Test
    fun aWilderModeStacksOnTopOfTheCrewHandicap() {
        val crew = Skins.RUST
        val cruise = crew.risk + RiskMode.CRUISE.risk
        val meltdown = crew.risk + RiskMode.MELTDOWN.risk

        assertTrue(meltdown > cruise)
        assertTrue(crew.payout * RiskMode.MELTDOWN.payout > crew.payout * RiskMode.CRUISE.payout)
    }
}
