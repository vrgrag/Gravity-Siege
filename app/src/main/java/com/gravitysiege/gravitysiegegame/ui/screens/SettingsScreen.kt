package com.gravitysiege.gravitysiegegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravitysiege.gravitysiegegame.GameStore
import com.gravitysiege.gravitysiegegame.audio.Sfx
import com.gravitysiege.gravitysiegegame.ui.Routes
import com.gravitysiege.gravitysiegegame.ui.components.ScreenHeader
import com.gravitysiege.gravitysiegegame.ui.components.SettingRow
import com.gravitysiege.gravitysiegegame.ui.theme.Ink
import com.gravitysiege.gravitysiegegame.ui.theme.InkMuted
import com.gravitysiege.gravitysiegegame.ui.theme.Panel
import com.gravitysiege.gravitysiegegame.ui.theme.SkyDeep
import com.gravitysiege.gravitysiegegame.ui.theme.formatCoins

@Composable
fun SettingsScreen(store: GameStore, sfx: Sfx, open: (String) -> Unit, back: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(20.dp),
    ) {
        ScreenHeader("Settings", back)
        Spacer(Modifier.height(22.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Panel)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Sound effects", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Buttons, drops and alerts", color = InkMuted, fontSize = 13.sp)
            }
            Switch(
                checked = store.soundOn,
                onCheckedChange = {
                    store.setSound(it)
                    sfx.enabled = it
                    if (it) sfx.click()
                },
                colors = SwitchDefaults.colors(checkedTrackColor = SkyDeep),
            )
        }
        Spacer(Modifier.height(12.dp))
        SettingRow("Privacy Policy", WebPages.PRIVACY_URL) {
            sfx.click()
            open("${Routes.WEB}/${WebPages.PRIVACY}")
        }
        Spacer(Modifier.height(12.dp))
        SettingRow("Support", WebPages.SUPPORT_URL) {
            sfx.click()
            open("${Routes.WEB}/${WebPages.SUPPORT}")
        }
        Spacer(Modifier.height(24.dp))
        Text("Bank", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Balance ${formatCoins(store.coins)} coins  ·  Best cash-out ${formatCoins(store.biggestWin)}  ·  Tallest tower ${store.tallestTower}",
            color = InkMuted,
            fontSize = 13.sp,
        )
    }
}
