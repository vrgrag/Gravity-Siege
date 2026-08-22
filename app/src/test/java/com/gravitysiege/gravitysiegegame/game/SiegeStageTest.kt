package com.gravitysiege.gravitysiegegame.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class SiegeStageTest {

    private fun settle(stage: SiegeStage, seconds: Double = 3.0) {
        var t = 0.0
        while (t < seconds) {
            stage.step(1.0 / 60.0)
            t += 1.0 / 60.0
        }
    }

    @Test
    fun winchWidensTheArcFromRest() {
        val stage = SiegeStage(Random(7))
        assertEquals(0.0, stage.tilt, 1e-9)
        settle(stage, 1.0)
        var widest = 0.0
        repeat(200) {
            stage.step(1.0 / 60.0)
            widest = maxOf(widest, abs(stage.tilt))
        }
        assertTrue("arc should open up to the limit", widest > Yard.TILT_LIMIT * 0.9)
        assertTrue("arc must not exceed the limit", widest <= Yard.TILT_LIMIT + 1e-6)
    }

    @Test
    fun aHouseThatHoldsSeatsOnTheStack() {
        val stage = SiegeStage(Random(3))
        settle(stage, 0.5)
        stage.release(holds = true)
        assertNotNull(stage.airborne)
        settle(stage, 2.0)
        assertEquals(Verdict.SEATED, stage.takeVerdict())
        assertEquals(1, stage.storeys.size)
        assertNull(stage.airborne)
        assertTrue("tower grew upward", stage.stackTop < Yard.plinthTop)
    }

    @Test
    fun aHouseThatSlipsEndsTheRunAndLeavesNothingBehind() {
        val stage = SiegeStage(Random(11))
        settle(stage, 0.5)
        stage.release(holds = false)
        settle(stage, 3.0)
        assertEquals(Verdict.SLIPPED, stage.takeVerdict())
        assertTrue(stage.storeys.isEmpty())
        assertEquals(Yard.plinthTop, stage.stackTop, 1e-9)
    }

    @Test
    fun theCameraRidesTheStackUp() {
        val stage = SiegeStage(Random(5))
        settle(stage, 0.5)
        val start = stage.lensY
        repeat(3) {
            stage.release(holds = true)
            settle(stage, 1.6)
        }
        assertEquals(3, stage.storeys.size)
        assertTrue("camera followed the tower", stage.lensY < start - 1.0)
        assertEquals(stage.stackTop - Yard.LENS_LEAD, stage.lensY, 0.25)
    }

    @Test
    fun eachStoreySitsDirectlyOnTheOneBelow() {
        val stage = SiegeStage(Random(19))
        settle(stage, 0.5)
        repeat(4) {
            stage.release(holds = true)
            settle(stage, 1.6)
        }
        var floor = Yard.plinthTop
        stage.storeys.forEach { storey ->
            assertEquals(floor, storey.top + storey.rise, 1e-9)
            assertTrue("stays near the tower centre", abs(storey.cx) <= Yard.BED_SHIFT)
            floor = storey.top
        }
    }
}
