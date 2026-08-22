package com.gravitysiege.gravitysiegegame.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardTest {

    @Test
    fun theBoardHoldsEveryRivalPlusThePlayer() {
        val board = Leaderboard.board(day = 20_000, haul = 5_000, floors = 6)

        assertEquals(Leaderboard.RIVALS + 1, board.size)
        assertEquals(1, board.count { it.you })
        assertEquals(List(board.size) { it + 1 }, board.map { it.rank })
    }

    @Test
    fun theSameDayAlwaysDealsTheSameRivals() {
        val first = Leaderboard.board(day = 19_876, haul = 0, floors = 0)
        val second = Leaderboard.board(day = 19_876, haul = 0, floors = 0)

        assertEquals(first.map { it.name to it.haul }, second.map { it.name to it.haul })
    }

    @Test
    fun overnightTheRivalsChange() {
        val today = Leaderboard.board(day = 19_876, haul = 0, floors = 0).filterNot { it.you }
        val tomorrow = Leaderboard.board(day = 19_877, haul = 0, floors = 0).filterNot { it.you }

        assertTrue(today.map { it.haul } != tomorrow.map { it.haul })
    }

    @Test
    fun hallsAreListedFromRichestToPoorest() {
        val board = Leaderboard.board(day = 20_001, haul = 9_999, floors = 9)

        board.zipWithNext { above, below ->
            assertTrue("${above.haul} should not sit below ${below.haul}", above.haul >= below.haul)
        }
    }

    @Test
    fun aBiggerHaulNeverCostsThePlayerRank() {
        val day = 20_002L
        val modest = Leaderboard.rankOf(day, haul = 1_000, floors = 4)
        val huge = Leaderboard.rankOf(day, haul = 500_000, floors = 40)

        assertEquals(1, huge)
        assertTrue(huge <= modest)
    }

    @Test
    fun anEmptyRecordLandsAtTheFootOfTheBoard() {
        val board = Leaderboard.board(day = 20_003, haul = 0, floors = 0)

        assertEquals(board.size, board.first { it.you }.rank)
    }
}
