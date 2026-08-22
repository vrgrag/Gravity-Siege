package com.gravitysiege.gravitysiegegame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.gravitysiege.gravitysiegegame.game.Yard
import kotlin.math.cos
import kotlin.math.sin

private const val CABLE = 0xFF23272B

/**
 * The rig on its own, for screens that want the crane without the whole yard.
 *
 * The hook is the only pendulum: it rides a circular arc about a hinge above
 * the box. The house underneath hangs off the rigging on its own shorter,
 * stiffer pendulum, hauled along by the hook's sideways acceleration, so it
 * lags through the middle of the arc and keeps swaying at the extremes.
 */
@Composable
fun CraneRig(
    houseArt: String,
    modifier: Modifier = Modifier,
    swingDegrees: Float = 12f,
    cycleMillis: Int = 2700,
) {
    val art = rememberSceneArt()
    var tilt by remember { mutableFloatStateOf(0f) }
    var sway by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(swingDegrees, cycleMillis) {
        val limit = Math.toRadians(swingDegrees.toDouble())
        val omega = 2 * Math.PI / (cycleMillis / 1000.0)
        var clock = 0.0
        var angle = 0.0
        var rate = 0.0
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1_000_000_000.0).coerceIn(0.0, 1.0 / 30.0)
                    clock += dt
                    val hook = limit * sin(omega * clock)
                    // Pivot acceleration for small angles, which is what drags
                    // the load around under the hook.
                    val haul = -omega * omega * hook
                    rate += (-LOAD_W2 * sin(angle) - LOAD_DRAG * rate - haul * cos(angle)) * dt
                    angle += rate * dt
                    tilt = hook.toFloat()
                    sway = angle.toFloat()
                }
                last = now
            }
        }
    }

    Canvas(modifier) {
        val hookImg = art[Yard.HOOK] ?: return@Canvas
        val houseImg = art[houseArt] ?: return@Canvas

        // Size from the width first, then shrink the whole rig if the hook,
        // rigging and house together would not fit the height it was given.
        var hookW = size.width * 0.17f
        var hookH = hookW * art.ratioOf(Yard.HOOK).toFloat()
        var houseW = size.width * 0.40f
        var houseH = houseW * art.ratioOf(houseArt).toFloat()
        var rope = houseW * 0.34f
        val needed = hookH * 0.92f + rope + houseH
        val room = size.height * 0.96f
        if (needed > room) {
            val shrink = room / needed
            hookW *= shrink
            hookH *= shrink
            houseW *= shrink
            houseH *= shrink
            rope *= shrink
        }

        val armX = sin(tilt)
        val armY = cos(tilt)
        val reach = size.height * 1.1f
        val pivot = Offset(size.width / 2f, size.height * 0.02f + hookH / 2f - reach)
        val hook = Offset(pivot.x + reach * armX, pivot.y + reach * armY)

        val gap = hookW * 0.23f
        for (side in intArrayOf(-1, 1)) {
            val shift = Offset(side * gap * armY, -side * gap * armX)
            drawLine(
                color = Color(CABLE),
                start = pivot + shift,
                end = hook + shift,
                strokeWidth = size.width * 0.014f,
                cap = StrokeCap.Round,
            )
        }
        drawSprite(hookImg, hook.x, hook.y, hookW, hookH, Math.toDegrees(tilt.toDouble()).toFloat())

        val slung = rope + houseH / 2f
        val cs = cos(sway)
        val sn = sin(sway)
        val tip = hook + Offset(armX * hookH * 0.42f, armY * hookH * 0.42f)
        val house = Offset(tip.x + slung * sn, tip.y + slung * cs)
        for (side in intArrayOf(-1, 1)) {
            val lx = side * houseW * 0.28f
            val ly = -houseH / 2f
            drawLine(
                color = Color(CABLE),
                start = tip,
                end = Offset(house.x + lx * cs - ly * sn, house.y + lx * sn + ly * cs),
                strokeWidth = size.width * 0.010f,
                cap = StrokeCap.Round,
            )
        }
        drawSprite(houseImg, house.x, house.y, houseW, houseH, Math.toDegrees(sway.toDouble()).toFloat())
    }
}

/** Natural frequency squared and drag for the load slung under the hook. */
private const val LOAD_W2 = 16.0
private const val LOAD_DRAG = 1.5
