package com.voidloom.keel.core

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gravitysiege.gravitysiegegame.R
import java.lang.ref.WeakReference

/**
 * Bars stay on screen, then fold after [KeelMark.BAR_FOLD_MS].
 * A swipe that brings them back starts the same delay again.
 */
internal object KeelImmersion {

    private val BAR_MASK: Int =
        WindowInsetsCompat.Type.statusBars() or
            WindowInsetsCompat.Type.navigationBars() or
            WindowInsetsCompat.Type.captionBar()

    fun apply(host: Activity) {
        paint(host)
        watch(host)
        scheduleFold(host)
    }

    fun foldNow(host: Activity) {
        val decor = host.window.decorView
        val task = decor.getTag(R.id.keel_fold_task) as? Runnable
        if (task != null) decor.removeCallbacks(task)
        fold(host)
    }

    private fun paint(host: Activity) {
        val window = host.window
        val decor = window.decorView
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        @Suppress("DEPRECATION")
        decor.systemUiVisibility = decor.systemUiVisibility or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    private fun watch(host: Activity) {
        val decor = host.window.decorView
        if (decor.getTag(R.id.keel_fold_watch) == true) return
        decor.setTag(R.id.keel_fold_watch, true)
        @Suppress("DEPRECATION")
        decor.setOnSystemUiVisibilityChangeListener { vis ->
            val hidden = vis and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION != 0
            if (!hidden) scheduleFold(host)
        }
        decor.setOnApplyWindowInsetsListener { v, insets ->
            val wrapped = WindowInsetsCompat.toWindowInsetsCompat(insets, v)
            val shown = wrapped.isVisible(WindowInsetsCompat.Type.statusBars()) ||
                wrapped.isVisible(WindowInsetsCompat.Type.navigationBars())
            val wasShown = v.getTag(R.id.keel_fold_shown) == true
            v.setTag(R.id.keel_fold_shown, shown)
            if (shown && !wasShown) scheduleFold(host)
            @Suppress("DEPRECATION")
            v.onApplyWindowInsets(insets)
        }
    }

    private fun scheduleFold(host: Activity) {
        val decor = host.window.decorView
        val task = taskOn(host, decor)
        decor.removeCallbacks(task)
        decor.postDelayed(task, KeelMark.BAR_FOLD_MS)
    }

    private fun taskOn(host: Activity, decor: View): Runnable {
        val tagged = decor.getTag(R.id.keel_fold_task) as? FoldTask
        if (tagged != null) return tagged
        val created = FoldTask(WeakReference(host))
        decor.setTag(R.id.keel_fold_task, created)
        return created
    }

    private fun fold(host: Activity) {
        if (host.isFinishing || host.isDestroyed) return
        val window = host.window
        val decor = window.decorView
        @Suppress("DEPRECATION")
        decor.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        WindowInsetsControllerCompat(window, decor).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            hide(BAR_MASK)
        }
    }

    private class FoldTask(private val ref: WeakReference<Activity>) : Runnable {
        override fun run() {
            fold(ref.get() ?: return)
        }
    }
}
