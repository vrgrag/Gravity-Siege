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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravitysiege.gravitysiegegame.GameStore
import com.gravitysiege.gravitysiegegame.audio.Sfx
import com.gravitysiege.gravitysiegegame.game.Skin
import com.gravitysiege.gravitysiegegame.game.Skins
import com.gravitysiege.gravitysiegegame.game.Yard
import com.gravitysiege.gravitysiegegame.ui.components.AssetImage
import com.gravitysiege.gravitysiegegame.ui.components.CoinAmount
import com.gravitysiege.gravitysiegegame.ui.components.HazardYellow
import com.gravitysiege.gravitysiegegame.ui.components.PlateButton
import com.gravitysiege.gravitysiegegame.ui.components.SiteScaffold
import com.gravitysiege.gravitysiegegame.ui.components.SteelEdge
import com.gravitysiege.gravitysiegegame.ui.components.SteelText
import com.gravitysiege.gravitysiegegame.ui.theme.Ink

@Composable
fun SkinsScreen(store: GameStore, sfx: Sfx, back: () -> Unit) {
    var note by remember { mutableStateOf<String?>(null) }

    SiteScaffold("CREWS", store.coins, back) {
        note?.let {
            Text(
                it,
                color = HazardYellow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF15181B).copy(alpha = 0.92f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(10.dp))
        }

        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(Skins.ALL, key = { it.id }) { skin ->
                CrewCard(
                    skin = skin,
                    owned = store.owns(skin),
                    equipped = store.skinId == skin.id,
                    affordable = store.coins >= skin.price,
                    onPick = {
                        when {
                            store.owns(skin) -> {
                                if (store.equipSkin(skin)) {
                                    sfx.click()
                                    note = "${skin.title} ON SHIFT"
                                }
                            }
                            store.buySkin(skin) -> {
                                sfx.success()
                                note = "${skin.title} SIGNED ON"
                            }
                            else -> {
                                sfx.error()
                                note = "NOT ENOUGH COINS FOR ${skin.title}"
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CrewCard(
    skin: Skin,
    owned: Boolean,
    equipped: Boolean,
    affordable: Boolean,
    onPick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF1B2026).copy(alpha = 0.94f))
            .border(if (equipped) 2.dp else 1.dp, if (equipped) skin.accent else SteelEdge, shape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF10141A)),
                contentAlignment = Alignment.Center,
            ) {
                AssetImage(
                    Yard.HOUSES[1],
                    Modifier.size(56.dp),
                    ContentScale.Fit,
                    colorFilter = skin.paint?.let { ColorFilter.tint(it, BlendMode.Modulate) },
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        skin.title,
                        color = skin.accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                    )
                    if (equipped) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "ON SHIFT",
                            color = Color(0xFF2EAE4F),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(skin.blurb, color = SteelText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            skin.traits.forEach { trait ->
                Text(
                    trait.uppercase(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2B333B))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        when {
            equipped -> Text(
                "CURRENTLY WORKING THIS CREW",
                color = SteelText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            owned -> PlateButton(
                label = "PUT ON SHIFT",
                onClick = onPick,
                modifier = Modifier.fillMaxWidth(),
                height = 50.dp,
                ink = Ink,
            )
            else -> Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoinAmount(
                    skin.price,
                    tint = if (affordable) Color.White else Color(0xFFE23B3B),
                    fontSize = 17.sp,
                )
                PlateButton(
                    label = "HIRE",
                    onClick = onPick,
                    modifier = Modifier.width(150.dp),
                    height = 50.dp,
                    enabled = affordable,
                    ink = Ink,
                )
            }
        }
    }
}
