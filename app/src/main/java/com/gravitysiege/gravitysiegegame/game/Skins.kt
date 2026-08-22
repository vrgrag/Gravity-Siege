package com.gravitysiege.gravitysiegegame.game

import androidx.compose.ui.graphics.Color

/**
 * A crew the player signs on with.
 *
 * A crew is more than paint on the houses: each one changes how a shift
 * actually plays. [tempo] stretches or squeezes the time the rig needs to
 * cross its arc, [risk] shifts how often a house slips off the stack and
 * [payout] scales every multiplier a seated house pays. Faster and riskier
 * crews pay more, so the choice is a real trade and not a costume.
 */
data class Skin(
    val id: String,
    val title: String,
    val blurb: String,
    val price: Int,
    /** Paint laid over the house sprites; null leaves the art as drawn. */
    val paint: Color?,
    val accent: Color,
    /** Multiplies the seconds the rig takes to sweep across. Below 1 is quicker. */
    val tempo: Double,
    /** Added to the chance a released house slips. */
    val risk: Double,
    /** Multiplies the odds every seated house pays. */
    val payout: Double,
) {
    val free: Boolean get() = price <= 0

    /** Short human summary of what the crew does to a shift. */
    val traits: List<String>
        get() = listOf(
            when {
                tempo > 1.05 -> "Slow rig"
                tempo < 0.95 -> "Fast rig"
                else -> "Steady rig"
            },
            when {
                risk > 0.005 -> "Higher risk"
                risk < -0.005 -> "Lower risk"
                else -> "Even risk"
            },
            when {
                payout > 1.005 -> "Bigger pay"
                payout < 0.995 -> "Smaller pay"
                else -> "Standard pay"
            },
        )
}

object Skins {

    val DAY = Skin(
        id = "day",
        title = "DAY SHIFT",
        blurb = "The standard crew. Nothing bent, nothing gained.",
        price = 0,
        paint = null,
        accent = Color(0xFFF5C012),
        tempo = 1.0,
        risk = 0.0,
        payout = 1.0,
    )

    val NIGHT = Skin(
        id = "night",
        title = "NIGHT SHIFT",
        blurb = "Floodlights and patience. The rig crawls and the stack holds.",
        price = 2_500,
        paint = Color(0xFF93AEDC),
        accent = Color(0xFF7FD8FF),
        tempo = 1.28,
        risk = -0.03,
        payout = 0.86,
    )

    val RUST = Skin(
        id = "rust",
        title = "RUST CREW",
        blurb = "Old gear, loose cables, fat bonuses for anyone who dares.",
        price = 8_000,
        paint = Color(0xFFE8A070),
        accent = Color(0xFFFF8A1F),
        tempo = 0.78,
        risk = 0.045,
        payout = 1.22,
    )

    val GOLD = Skin(
        id = "gold",
        title = "GOLD CREW",
        blurb = "Everything plated, everything on the line. Top rates, thin margins.",
        price = 25_000,
        paint = Color(0xFFFFD070),
        accent = Color(0xFFF2B705),
        tempo = 0.66,
        risk = 0.075,
        payout = 1.45,
    )

    val ALL = listOf(DAY, NIGHT, RUST, GOLD)

    val DEFAULT_ID = DAY.id

    fun byId(id: String?): Skin = ALL.firstOrNull { it.id == id } ?: DAY
}
