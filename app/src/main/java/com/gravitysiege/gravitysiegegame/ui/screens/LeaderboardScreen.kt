package com.gravitysiege.gravitysiegegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravitysiege.gravitysiegegame.GameStore
import com.gravitysiege.gravitysiegegame.game.Leaderboard
import com.gravitysiege.gravitysiegegame.game.Standing
import com.gravitysiege.gravitysiegegame.ui.components.Coin
import com.gravitysiege.gravitysiegegame.ui.components.HazardTape
import com.gravitysiege.gravitysiegegame.ui.components.HazardYellow
import com.gravitysiege.gravitysiegegame.ui.components.SiteScaffold
import com.gravitysiege.gravitysiegegame.ui.components.SiteStat
import com.gravitysiege.gravitysiegegame.ui.components.SteelEdge
import com.gravitysiege.gravitysiegegame.ui.components.SteelPlate
import com.gravitysiege.gravitysiegegame.ui.components.SteelText
import com.gravitysiege.gravitysiegegame.ui.theme.formatCoins
import java.time.LocalDate

@Composable
fun LeaderboardScreen(store: GameStore, back: () -> Unit) {
    val haul = store.biggestWin
    val floors = store.tallestTower
    val board = remember(haul, floors) {
        Leaderboard.board(LocalDate.now().toEpochDay(), haul, floors)
    }
    val you = board.first { it.you }

    SiteScaffold("CREW BOARD", store.coins, back) {
        SteelPlate(Modifier.fillMaxWidth(), corner = 14.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SiteStat("YOUR RANK", "#${you.rank}", HazardYellow)
                    SiteStat("BEST HAUL", formatCoins(haul))
                    SiteStat("TALLEST", "$floors FL")
                }
                Spacer(Modifier.height(10.dp))
                HazardTape(
                    Modifier
                        .fillMaxWidth()
                        .height(7.dp),
                    stripe = 9.dp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Board shuffles at midnight. Beat your best haul to climb.",
                    color = SteelText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(board, key = { it.rank }) { line -> BoardLine(line) }
        }
    }
}

@Composable
private fun BoardLine(line: Standing) {
    val shape = RoundedCornerShape(12.dp)
    val face = if (line.you) HazardYellow.copy(alpha = 0.16f) else Color(0xFF1B2026).copy(alpha = 0.92f)
    val edge = if (line.you) HazardYellow else SteelEdge.copy(alpha = 0.55f)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(face)
            .border(if (line.you) 2.dp else 1.dp, edge, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${line.rank}",
            color = medalOf(line.rank),
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(30.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                line.name,
                color = if (line.you) HazardYellow else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
            Text(
                "${line.floors} floors",
                color = SteelText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Coin(size = 15.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                formatCoins(line.haul),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private fun medalOf(rank: Int): Color = when (rank) {
    1 -> Color(0xFFF5C012)
    2 -> Color(0xFFC9D2DA)
    3 -> Color(0xFFCE8946)
    else -> SteelText
}
