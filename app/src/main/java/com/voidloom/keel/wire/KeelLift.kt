package com.voidloom.keel.wire

import android.graphics.Rect
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max
import kotlin.math.min

/**
 * Landscape-safe keyboard pan. Slides the WebView; never shrinks it.
 */
internal class KeelLift(private val host: View) {

    private var web: WebView? = null
    private var fieldTop = -1f
    private var fieldBottom = -1f
    private var framed = false
    private var kb = 0
    private var riding = false
    private var declared = 0
    private var settled = 0

    private val ping = Runnable {
        web?.evaluateJavascript("window.__vlWinchPing&&window.__vlWinchPing();", null)
    }

    fun install() {
        ViewCompat.setOnApplyWindowInsetsListener(host) { view, insets ->
            val raw = insets.toWindowInsets()?.displayCutout
            val cut = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            view.setPadding(
                maxOf(cut.left, raw?.safeInsetLeft ?: 0),
                maxOf(cut.top, raw?.safeInsetTop ?: 0),
                maxOf(cut.right, raw?.safeInsetRight ?: 0),
                maxOf(cut.bottom, raw?.safeInsetBottom ?: 0),
            )
            if (!riding) {
                settle(measuredKb(insets))
                slide(smooth = kb > 0)
                if (kb > 0) ask(160L)
            }
            stripIme(insets)
        }
        ViewCompat.setWindowInsetsAnimationCallback(
            host,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) riding = true
                }

                override fun onStart(
                    animation: WindowInsetsAnimationCompat,
                    bounds: WindowInsetsAnimationCompat.BoundsCompat,
                ): WindowInsetsAnimationCompat.BoundsCompat {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        declared = bounds.upperBound.bottom
                    }
                    return bounds
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    running: MutableList<WindowInsetsAnimationCompat>,
                ): WindowInsetsCompat {
                    if (riding) {
                        rise(measuredKb(insets))
                        slide(smooth = false)
                    }
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() == 0) return
                    riding = false
                    declared = 0
                    ViewCompat.requestApplyInsets(host)
                    if (kb > 0) ask(160L) else slide(smooth = false)
                }
            },
        )
        host.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (riding) return@addOnLayoutChangeListener
            val next = measuredKb(ViewCompat.getRootWindowInsets(host))
            if (next != kb) {
                settle(next)
                slide(smooth = false)
                if (kb > 0) ask(90L)
            }
        }
        ViewCompat.requestApplyInsets(host)
    }

    fun bind(view: WebView) {
        web = view
        wipe()
        view.translationY = 0f
        view.addJavascriptInterface(WinchBridge(), BRIDGE)
    }

    fun wipe() {
        host.removeCallbacks(ping)
        fieldTop = -1f
        fieldBottom = -1f
        framed = false
        slide(smooth = false)
    }

    fun afterTurn() {
        wipe()
        settled = 0
        if (kb > 0) ask(160L)
    }

    val sheet: String = SCRIPT

    private fun ask(delay: Long) {
        host.removeCallbacks(ping)
        host.postDelayed(ping, delay)
    }

    private fun settle(height: Int) {
        kb = height
        if (height > 0) settled = height
    }

    private fun rise(height: Int) {
        val rest = if (settled > 0) settled else declared
        kb = if (rest > 0) min(height, rest) else height
    }

    private fun measuredKb(insets: WindowInsetsCompat?): Int {
        val fromIme = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
        if (fromIme > 0) return fromIme
        val frame = Rect()
        host.getWindowVisibleDisplayFrame(frame)
        val gap = host.rootView.height - frame.bottom
        val floor = (76f * host.resources.displayMetrics.density).toInt()
        return if (gap >= floor) gap else 0
    }

    private fun stripIme(insets: WindowInsetsCompat): WindowInsetsCompat =
        runCatching {
            WindowInsetsCompat.Builder(insets)
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.NONE)
                .setVisible(WindowInsetsCompat.Type.ime(), false)
                .build()
        }.getOrDefault(insets)

    private fun slide(smooth: Boolean) {
        val view = web ?: return
        val target = -shift(view)
        view.animate().cancel()
        if (smooth && view.translationY != target) {
            view.animate().translationY(target).setDuration(140L).start()
        } else {
            view.translationY = target
        }
    }

    private fun shift(view: View): Float {
        val height = kb
        val span = view.height
        if (height <= 0 || span <= 0 || fieldBottom < 0f) return 0f
        val aim: Float
        val cap: Float
        if (framed) {
            aim = fieldBottom
            cap = min(height.toFloat(), max(0f, fieldTop))
        } else {
            aim = min(fieldBottom, fieldTop + 92f * view.resources.displayMetrics.density)
            cap = height.toFloat()
        }
        return (aim - (span - height)).coerceIn(0f, cap)
    }

    private inner class WinchBridge {
        @JavascriptInterface
        fun mark(frame: Boolean, top: Double, bottom: Double) {
            val view = web ?: return
            view.post {
                framed = frame
                fieldTop = top.toFloat()
                fieldBottom = bottom.toFloat()
                if (kb > 0) slide(smooth = !riding)
            }
        }
    }

    companion object {
        const val BRIDGE = "VlWinch"
        private val SCRIPT = """
        (function(){
          if(window.__vlWinchKb) return; window.__vlWinchKb=1;
          function ok(el){
            if(!el) return false;
            var t=el.tagName;
            if(t==='INPUT'){
              var k=(el.type||'text').toLowerCase();
              return k!=='checkbox'&&k!=='radio'&&k!=='button'&&k!=='submit'&&k!=='reset'&&k!=='file'&&k!=='range'&&k!=='hidden';
            }
            return t==='TEXTAREA'||el.isContentEditable===true;
          }
          function box(el,win){
            if(el.isContentEditable){
              try{
                var s=win.getSelection();
                if(s&&s.rangeCount){
                  var r=s.getRangeAt(0).getBoundingClientRect();
                  if(r&&r.height>0) return r;
                }
              }catch(e){}
            }
            return el.getBoundingClientRect();
          }
          function find(){
            var el=document.activeElement, win=window, off=0, n=0;
            while(el&&(el.tagName==='IFRAME'||el.tagName==='FRAME')&&n++<4){
              var out=el.getBoundingClientRect(), doc=null;
              try{doc=el.contentDocument;}catch(e){doc=null;}
              var inner=doc?doc.activeElement:null;
              if(!inner||inner===doc.body){
                return {f:true,a:off+out.top,b:off+out.bottom};
              }
              hook(doc);
              win=el.contentWindow||win;
              off+=out.top;
              el=inner;
            }
            if(!ok(el)) return null;
            var r=box(el,win);
            return {f:false,a:off+r.top,b:off+r.bottom};
          }
          function ping(){
            var at=find();
            if(!at) return;
            var vv=window.visualViewport;
            var lift=vv?vv.offsetTop:0;
            var zoom=(vv&&vv.scale)?vv.scale:1;
            var px=(window.devicePixelRatio||1)*zoom;
            try{ VlWinch.mark(at.f,(at.a-lift)*px,(at.b-lift+12)*px); }catch(e){}
          }
          window.__vlWinchPing=ping;
          function hook(doc){
            try{
              if(!doc||doc.__vlWinchDoc) return;
              doc.__vlWinchDoc=1;
              doc.addEventListener('focusin', function(){ ping(); setTimeout(ping,190); }, true);
            }catch(e){}
          }
          hook(document);
        })();
        """.trimIndent()
    }
}
