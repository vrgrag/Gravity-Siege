package com.gravitysiege.gravitysiegegame.game

import androidx.compose.ui.graphics.Color

/**
 * How hard the crew is pushed on a shift.
 *
 * Picked right above the build button and free to change between rounds. The
 * tame modes keep the tower standing, the wild ones pay far more for houses
 * that are far more likely to slip. This stacks on top of whatever handicap
 * the hired crew already carries.
 */
enum class RiskMode(
    val label: String,
    /** Added to the chance a released house slips. */
    val risk: Double,
    /** Multiplies the odds every seated house pays. */
    val payout: Double,
    val tint: Color,
) {
    CRUISE("Cruise", 0.00, 1.00, Color(0xFF3DDC97)),
    HUSTLE("Hustle", 0.05, 1.25, Color(0xFFF5C012)),
    RECKLESS("Reckless", 0.11, 1.60, Color(0xFFFF8A1F)),
    MELTDOWN("Meltdown", 0.18, 2.10, Color(0xFFE23B3B)),
    ;

    companion object {
        fun byName(name: String?): RiskMode = entries.firstOrNull { it.name == name } ?: CRUISE
    }
}
