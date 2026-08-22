package com.gravitysiege.gravitysiegegame.game

import kotlin.random.Random

/** One line on the crew board. */
data class Standing(
    val rank: Int,
    val name: String,
    val haul: Int,
    val floors: Int,
    val you: Boolean,
)

/**
 * The crew board.
 *
 * There is no server behind this: the rival hauls are made up on the spot from
 * a seed seeded with the calendar day, so the board is stable all day, shuffles
 * overnight and needs no network. The player's own line is their real best
 * haul, slotted in wherever it lands.
 */
object Leaderboard {

    const val RIVALS = 19

    private val NAMES = listOf(
        "IRON MIKE", "CRANE KATE", "REBAR RAY", "HARD HAT HAL", "TOWER TOM",
        "SLING SAL", "GIRDER GUS", "CEMENT CEL", "HOIST HANK", "BOLT BEA",
        "DERRICK DEE", "SCAFFOLD SAM", "MORTAR MO", "WELD WANDA", "PLUMB PETE",
        "JIB JUNO", "TROWEL TED", "ANCHOR ANA", "CHISEL CHAZ", "LADDER LUZ",
        "GRAVEL GREG", "SPIRE SONIA", "BEAM BRUNO", "WINCH WILLA",
    )

    /**
     * The board for [day], with the player's [haul] and [floors] folded in.
     * Ties break in the player's favour so a matching score reads as a climb.
     */
    fun board(day: Long, haul: Int, floors: Int): List<Standing> {
        val rng = Random(day * 31 + 7)
        val roster = NAMES.shuffled(rng).take(RIVALS)

        var mark = 52_000 + rng.nextInt(78_000)
        val rivals = roster.map { name ->
            val entry = Standing(0, name, mark, floorsFor(mark, rng), you = false)
            mark = (mark * (0.72 + rng.nextDouble() * 0.22)).toInt().coerceAtLeast(120)
            entry
        }

        return (rivals + Standing(0, "YOU", haul, floors, you = true))
            .sortedWith(compareByDescending<Standing> { it.haul }.thenByDescending { it.you })
            .mapIndexed { index, line -> line.copy(rank = index + 1) }
    }

    /** Where the player sits today, 1 being the top of the board. */
    fun rankOf(day: Long, haul: Int, floors: Int): Int =
        board(day, haul, floors).first { it.you }.rank

    /** Roughly how tall a tower that haul would have taken. */
    private fun floorsFor(haul: Int, rng: Random): Int {
        var floors = 2
        var reach = 400.0
        while (reach < haul && floors < 46) {
            reach *= 1.36
            floors++
        }
        return (floors + rng.nextInt(-1, 2)).coerceAtLeast(1)
    }
}
