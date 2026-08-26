package com.voidloom.keel.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import com.gravitysiege.gravitysiegegame.R

internal class KeelBootView(context: Context) : View(context) {
    var progress: Float = 0.04f
        set(value) {
            field = value.coerceIn(0f, 1f)
            postInvalidateOnAnimation()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var port: Bitmap? = null
    private var land: Bitmap? = null
    private var started = System.currentTimeMillis()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (port == null) port = BitmapFactory.decodeResource(resources, R.drawable.keel_boot_port)
        if (land == null) land = BitmapFactory.decodeResource(resources, R.drawable.keel_boot_land)
    }

    override fun onDraw(c: Canvas) {
        val bmp = if (width > height) land else port
        if (bmp != null) drawCropped(c, bmp)
        else c.drawColor(PIT)

        val barTop = height * 0.83f
        val barBot = height * 0.888f
        paint.color = Color.argb(210, 8, 22, 42)
        c.drawRoundRect(width * 0.11f, barTop, width * 0.89f, barBot, 18f, 18f, paint)
        paint.color = GOLD
        c.drawRoundRect(
            width * 0.12f,
            barTop + (barBot - barTop) * 0.22f,
            width * 0.12f + width * 0.76f * progress,
            barBot - (barBot - barTop) * 0.22f,
            14f,
            14f,
            paint,
        )
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = minOf(width, height) * 0.044f
        paint.typeface = Typeface.DEFAULT_BOLD
        val dots = ((System.currentTimeMillis() - started) / 400 % 3 + 1).toInt()
        c.drawText("Loading" + ".".repeat(dots), width / 2f, height * 0.805f, paint)
        if (progress < 1f) postInvalidateDelayed(130)
    }
}

internal class KeelDryView(
    context: Context,
    private val onRetry: () -> Unit,
) : View(context) {
    var busy: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var port: Bitmap? = null
    private var land: Bitmap? = null
    private var retryRect = RectF()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (port == null) port = BitmapFactory.decodeResource(resources, R.drawable.keel_dry_port)
        if (land == null) land = BitmapFactory.decodeResource(resources, R.drawable.keel_dry_land)
    }

    override fun onDraw(c: Canvas) {
        val bmp = if (width > height) land else port
        if (bmp != null) drawCropped(c, bmp)
        else c.drawColor(PIT)
        val wide = width > height
        val bw = if (wide) width * 0.32f else width * 0.62f
        val bh = if (wide) height * 0.138f else height * 0.068f
        val top = if (wide) height * 0.79f else height * 0.83f
        retryRect.set((width - bw) / 2f, top, (width + bw) / 2f, top + bh)
        drawPill(c, retryRect, if (busy) "..." else "Retry", filled = true)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && retryRect.contains(event.x, event.y) && !busy) {
            onRetry()
            return true
        }
        return event.action == MotionEvent.ACTION_DOWN && retryRect.contains(event.x, event.y)
    }
}

internal class KeelHailView(
    context: Context,
    private val onAccept: () -> Unit,
    private val onSkip: () -> Unit,
) : View(context) {
    private var port: Bitmap? = null
    private var land: Bitmap? = null
    private var acceptRect = RectF()
    private var skipRect = RectF()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (port == null) port = BitmapFactory.decodeResource(resources, R.drawable.keel_hail_port)
        if (land == null) land = BitmapFactory.decodeResource(resources, R.drawable.keel_hail_land)
    }

    override fun onDraw(c: Canvas) {
        val bmp = if (width > height) land else port
        if (bmp != null) drawCropped(c, bmp)
        else c.drawColor(PIT)
        val wide = width > height
        val bw = if (wide) width * 0.3f else width * 0.58f
        val bh = if (wide) height * 0.078f else height * 0.056f
        val gap = if (wide) height * 0.018f else height * 0.015f
        val acceptTop = if (wide) height * 0.782f else height * 0.808f
        acceptRect.set((width - bw) / 2f, acceptTop, (width + bw) / 2f, acceptTop + bh)
        skipRect.set((width - bw) / 2f, acceptRect.bottom + gap, (width + bw) / 2f, acceptRect.bottom + gap + bh)
        drawPill(c, acceptRect, "Accept", filled = true)
        drawPill(c, skipRect, "Skip", filled = false)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) {
            return acceptRect.contains(event.x, event.y) || skipRect.contains(event.x, event.y)
        }
        when {
            acceptRect.contains(event.x, event.y) -> onAccept()
            skipRect.contains(event.x, event.y) -> onSkip()
            else -> return false
        }
        return true
    }
}

private const val PIT = 0xFF0B1A2A.toInt()
private const val GOLD = 0xFFF2B705.toInt()

private fun View.drawCropped(c: Canvas, bmp: Bitmap) {
    val scale = maxOf(width / bmp.width.toFloat(), height / bmp.height.toFloat())
    val dw = bmp.width * scale
    val dh = bmp.height * scale
    c.drawBitmap(
        bmp,
        null,
        RectF((width - dw) / 2f, (height - dh) / 2f, (width + dw) / 2f, (height + dh) / 2f),
        null,
    )
}

private fun View.drawPill(c: Canvas, rect: RectF, label: String, filled: Boolean) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val radius = rect.height() / 2f
    if (filled) {
        paint.color = GOLD
        c.drawRoundRect(rect, radius, radius, paint)
        paint.color = PIT
    } else {
        paint.color = Color.argb(180, 11, 26, 42)
        c.drawRoundRect(rect, radius, radius, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.WHITE
        c.drawRoundRect(rect, radius, radius, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
    }
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = rect.height() * 0.38f
    paint.typeface = Typeface.DEFAULT_BOLD
    val y = rect.centerY() - (paint.descent() + paint.ascent()) / 2f
    c.drawText(label, rect.centerX(), y, paint)
}
