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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravitysiege.gravitysiegegame.GameStore
import com.gravitysiege.gravitysiegegame.audio.Sfx
import com.gravitysiege.gravitysiegegame.ui.components.Coin
import com.gravitysiege.gravitysiegegame.ui.components.HazardTape
import com.gravitysiege.gravitysiegegame.ui.components.HazardYellow
import com.gravitysiege.gravitysiegegame.ui.components.PlateButton
import com.gravitysiege.gravitysiegegame.ui.components.SiteScaffold
import com.gravitysiege.gravitysiegegame.ui.components.SteelEdge
import com.gravitysiege.gravitysiegegame.ui.components.SteelText
import com.gravitysiege.gravitysiegegame.ui.theme.Ink
import com.gravitysiege.gravitysiegegame.ui.theme.formatCoins

@Composable
fun DailyDropScreen(store: GameStore, sfx: Sfx, back: () -> Unit) {
    var claimed by remember { mutableIntStateOf(0) }
    val ready = store.dailyReady
    val day = store.nextStreakDay

    SiteScaffold("DAILY DROP", store.coins, back) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF1B2026).copy(alpha = 0.94f))
                .border(1.dp, SteelEdge, RoundedCornerShape(18.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (ready) "TODAY'S DROP IS WAITING" else "DROP COLLECTED",
                color = if (ready) HazardYellow else SteelText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
            )
            Spacer(Modifier.height(14.dp))
            Coin(size = 76.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                formatCoins(if (ready) store.nextDailyReward else claimed.takeIf { it > 0 } ?: store.nextDailyReward),
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (ready) "DAY $day OF THE RUN" else "COME BACK TOMORROW FOR DAY ${day + 1}",
                color = SteelText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            HazardTape(
                Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                stripe = 9.dp,
            )
            Spacer(Modifier.height(16.dp))
            PlateButton(
                label = if (ready) "Collect" else "Collected",
                onClick = {
                    val paid = store.claimDaily()
                    if (paid > 0) {
                        claimed = paid
                        sfx.success()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                height = 62.dp,
                enabled = ready,
                ink = Ink,
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "THE RUN",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.6.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Collect on consecutive days and the drop grows. Miss a day and the run starts over.",
            color = SteelText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GameStore.DAILY_STEPS.forEachIndexed { index, reward ->
                val stepDay = index + 1
                RunStep(
                    day = stepDay,
                    reward = reward,
                    done = stepDay < day || (!ready && stepDay <= day),
                    current = ready && stepDay == day,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RunStep(
    day: Int,
    reward: Int,
    done: Boolean,
    current: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val face = when {
        current -> HazardYellow.copy(alpha = 0.22f)
        done -> Color(0xFF2EAE4F).copy(alpha = 0.20f)
        else -> Color(0xFF10141A).copy(alpha = 0.85f)
    }
    val edge = when {
        current -> HazardYellow
        done -> Color(0xFF2EAE4F)
        else -> SteelEdge.copy(alpha = 0.5f)
    }
    Box(
        modifier
            .clip(shape)
            .background(face)
            .border(1.dp, edge, shape)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "D$day",
                color = SteelText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(3.dp))
            Coin(size = 14.dp)
            Spacer(Modifier.height(3.dp))
            Text(
                shortCoins(reward),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private fun shortCoins(value: Int): String =
    if (value >= 1000) "${value / 1000}.${(value % 1000) / 100}k" else value.toString()
