package com.gravitysiege.gravitysiegegame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.gravitysiege.gravitysiegegame.AssetBitmaps
import com.gravitysiege.gravitysiegegame.game.SiegeStage
import com.gravitysiege.gravitysiegegame.game.Yard
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val SKY = "bg_sky_asset.webp"
private const val CLOUD_A = "trim_cloud_asset_01.webp"
private const val CLOUD_B = "trim_cloud_asset_02.webp"

private val sceneArt = Yard.HOUSES + listOf(Yard.PLINTH, Yard.HOOK, Yard.STREET, SKY, CLOUD_A, CLOUD_B)

@Composable
fun rememberSceneArt(): Map<String, ImageBitmap?> {
    val context = LocalContext.current
    return remember {
        sceneArt.associateWith { name ->
            AssetBitmaps.get(context, name)
            AssetBitmaps.imageBitmap(name)
        }
    }
}

fun Map<String, ImageBitmap?>.ratioOf(name: String): Double {
    val img = this[name] ?: return 1.0
    if (img.width == 0) return 1.0
    return img.height.toDouble() / img.width.toDouble()
}

@Composable
fun YardCanvas(stage: SiegeStage, modifier: Modifier = Modifier) {
    val art = rememberSceneArt()
    remember(art) {
        stage.measureWith { name -> art.ratioOf(name) }
        true
    }

    Canvas(modifier) {
        val tick = stage.pulse
        val scale = minOf(size.width / Yard.SPAN.toFloat(), size.height / Yard.MIN_VIEW.toFloat())
        val horizon = size.height * 0.50f
        val jolt = (stage.quake * sin(tick * 1.9) * scale * 0.16).toFloat()

        fun px(x: Double) = size.width / 2f + (x * scale).toFloat()
        fun py(y: Double) = horizon + ((y - stage.lensY) * scale).toFloat() + jolt

        art[SKY]?.let { fill(it) }

        val climb = ((stage.lensY - Yard.plinthTop) * scale).toFloat()
        val loop = size.height + 420f
        art[CLOUD_A]?.let {
            val w = size.width * 0.62f
            val y = ((size.height * 0.22f - climb * 0.18f) % loop + loop) % loop - 210f
            drawSprite(it, size.width * 0.28f, y, w, w * art.ratioOf(CLOUD_A).toFloat(), alpha = 0.9f)
        }
        art[CLOUD_B]?.let {
            val w = size.width * 0.74f
            val y = ((size.height * 0.52f - climb * 0.28f) % loop + loop) % loop - 210f
            drawSprite(it, size.width * 0.74f, y, w, w * art.ratioOf(CLOUD_B).toFloat(), alpha = 0.82f)
        }

        art[Yard.STREET]?.let {
            val w = (Yard.STREET_SPAN * scale).toFloat()
            val h = w * art.ratioOf(Yard.STREET).toFloat()
            val bottom = py(Yard.PAVEMENT_Y + Yard.STREET_DROP)
            drawSprite(it, size.width / 2f, bottom - h / 2f, w, h)
        }

        art[Yard.PLINTH]?.let {
            val w = (Yard.PLINTH_SPAN * scale).toFloat()
            val h = (Yard.PLINTH_RISE * scale).toFloat()
            drawSprite(it, px(0.0), py(Yard.PAVEMENT_Y) - h / 2f, w, h)
        }

        stage.storeys.forEach { storey ->
            art[storey.art]?.let {
                val w = (storey.span * scale).toFloat() * (1f + 0.11f * storey.squash.toFloat())
                val h = (storey.rise * scale).toFloat() * (1f - 0.11f * storey.squash.toFloat())
                val bottom = py(storey.top + storey.rise)
                drawSprite(
                    img = it,
                    cx = px(storey.cx),
                    cy = bottom - h / 2f,
                    w = w,
                    h = h,
                    deg = Math.toDegrees(storey.lean + storey.wobble).toFloat(),
                )
            }
        }

        val hookDeg = Math.toDegrees(stage.tilt).toFloat()
        val hookW = (Yard.HOOK_SPAN * scale).toFloat()
        val hookH = hookW * art.ratioOf(Yard.HOOK).toFloat()
        val pivot = Offset(px(0.0), py(stage.pivotY))
        val hook = Offset(px(stage.hookX), py(stage.hookY))
        val armX = sin(stage.tilt).toFloat()
        val armY = cos(stage.tilt).toFloat()

        // Two parallel cables running down the arm from the off-screen hinge.
        val gap = hookW * 0.23f
        for (side in intArrayOf(-1, 1)) {
            val shift = Offset(side * gap * armY, -side * gap * armX)
            drawLine(
                color = Color(0xFF23272B),
                start = pivot + shift,
                end = hook + shift,
                strokeWidth = scale * 0.075f,
                cap = StrokeCap.Round,
            )
        }

        art[Yard.HOOK]?.let { drawSprite(it, hook.x, hook.y, hookW, hookH, hookDeg) }

        if (stage.hooked) {
            val houseW = (Yard.HOUSE_SPAN * scale).toFloat()
            val houseH = (stage.hangRise * scale).toFloat()
            val house = Offset(px(stage.hangX), py(stage.hangY))
            val swayDeg = Math.toDegrees(stage.sway).toFloat()
            val tip = hook + Offset(armX * hookH * 0.42f, armY * hookH * 0.42f)
            val cs = cos(stage.sway).toFloat()
            val sn = sin(stage.sway).toFloat()
            for (side in intArrayOf(-1, 1)) {
                val lx = side * houseW * 0.28f
                val ly = -houseH / 2f
                val corner = Offset(house.x + lx * cs - ly * sn, house.y + lx * sn + ly * cs)
                drawLine(
                    color = Color(0xFF23272B),
                    start = tip,
                    end = corner,
                    strokeWidth = scale * 0.055f,
                    cap = StrokeCap.Round,
                )
            }
            art[stage.hangArt]?.let { drawSprite(it, house.x, house.y, houseW, houseH, swayDeg) }
        }

        stage.airborne?.let { flying ->
            art[flying.art]?.let {
                drawSprite(
                    img = it,
                    cx = px(flying.x),
                    cy = py(flying.y),
                    w = (flying.span * scale).toFloat(),
                    h = (flying.rise * scale).toFloat(),
                    deg = Math.toDegrees(flying.turn).toFloat(),
                )
            }
        }

        stage.motes.forEach { mote ->
            val fade = (mote.life / mote.born).toFloat().coerceIn(0f, 1f)
            val radius = (mote.span * scale).toFloat() * if (mote.soft) 1.4f else 1f
            drawCircle(
                color = Color(mote.tint).copy(alpha = if (mote.soft) fade * 0.5f else fade),
                radius = radius,
                center = Offset(px(mote.x), py(mote.y)),
            )
        }
    }
}

private fun DrawScope.fill(img: ImageBitmap) {
    drawImage(
        image = img,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(img.width, img.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        filterQuality = FilterQuality.High,
    )
}

internal fun DrawScope.drawSprite(
    img: ImageBitmap,
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    deg: Float = 0f,
    alpha: Float = 1f,
) {
    val paint = {
        drawImage(
            image = img,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(img.width, img.height),
            dstOffset = IntOffset((cx - w / 2f).roundToInt(), (cy - h / 2f).roundToInt()),
            dstSize = IntSize(w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1)),
            alpha = alpha,
            filterQuality = FilterQuality.High,
        )
    }
    if (deg == 0f) paint() else rotate(deg, Offset(cx, cy)) { paint() }
}
