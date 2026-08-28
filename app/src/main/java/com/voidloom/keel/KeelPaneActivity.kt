package com.voidloom.keel

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.gravitysiege.gravitysiegegame.BuildConfig
import com.voidloom.keel.core.KeelImmersion
import com.voidloom.keel.core.KeelMark
import com.voidloom.keel.lock.KeelHref
import com.voidloom.keel.wire.KeelChrome
import com.voidloom.keel.wire.KeelLift
import com.voidloom.keel.wire.KeelPass
import com.voidloom.keel.wire.KeelSpan
import com.voidloom.keel.wire.KeelVault
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KeelPaneActivity : ComponentActivity() {

    private lateinit var shell: FrameLayout
    private lateinit var container: FrameLayout
    private lateinit var web: WebView
    private lateinit var vault: KeelVault
    private lateinit var span: KeelSpan
    private lateinit var lift: KeelLift

    private var settledUrl: String? = null
    private var deepestHop: String? = null
    private var landingUrl: String? = null
    private var redirectRetries = 0
    private var entryPointRetried = false
    private var rendererRecoveries = 0
    private var loadFailed = false
    private var chainSettled = false
    /** Keeps the cover raised across the reload a redirect retry queues up. */
    private var retryPending = false

    private var cover: View? = null
    private var coverJob: Job? = null

    @Volatile private var resumed = false
    @Volatile private var leftForOffline = false
    @Volatile private var offlineDeferred = false

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val callback = filePathCallback ?: return@registerForActivityResult
        filePathCallback = null
        callback.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
                ?: emptyArray(),
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KeelImmersion.apply(this)

        vault = KeelVault(this)
        span = KeelSpan(this)
        KeelPass.paneAlive = true

        shell = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            fitsSystemWindows = false
            clipToPadding = false
            clipChildren = false
        }
        container = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            fitsSystemWindows = false
        }
        shell.addView(
            container,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        setContentView(shell)
        lift = KeelLift(container)
        lift.install()

        buildWebView()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = retreatTowardLanding()
            },
        )

        val initial = resolveInitialUrl()
        if (initial == null) {
            finish()
            return
        }
        web.loadUrl(initial)
        watchConnectivity()
        startHeartbeat()
        lifecycleScope.launch {
            delay(KeelMark.SAFE_AREA_DELAY_MS)
            injectCutSheet()
            injectLiftSheet()
        }
    }

    private fun resolveInitialUrl(): String? {
        val fromIntent = intent.getStringExtra(EXTRA_TARGET_URL)
        val pushed = vault.takePushLink()
        return KeelHref.pick(pushed) ?: pushed?.takeIf { it.isNotBlank() }
            ?: KeelHref.pick(fromIntent) ?: fromIntent?.takeIf { it.isNotBlank() }
            ?: KeelHref.pick(vault.readCachedLink()) ?: vault.readCachedLink()
    }

    override fun onStart() {
        super.onStart()
        leftForOffline = false
        KeelPass.attach { url ->
            runOnUiThread { applyDestination(url) }
        }
        KeelPass.drain()?.let { url ->
            info("parked URL delivered")
            applyDestination(url)
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        KeelImmersion.apply(this)
        val deferred = offlineDeferred
        offlineDeferred = false
        lifecycleScope.launch {
            val alive = span.isReachable()
            when {
                !alive -> goOffline("offline on resume")
                deferred -> restoreAfterLoss()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) KeelImmersion.apply(this)
    }

    override fun onPause() {
        resumed = false
        super.onPause()
    }

    override fun onStop() {
        KeelPass.detach()
        super.onStop()
    }

    override fun onDestroy() {
        KeelPass.detach()
        KeelPass.paneAlive = false
        coverJob?.cancel()
        cover = null
        runCatching {
            web.stopLoading()
            container.removeView(web)
            web.destroy()
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        leftForOffline = false
        val rawPush = vault.takePushLink()
        val fromIntent = intent.getStringExtra(EXTRA_TARGET_URL)
        val pushed = KeelHref.pick(rawPush) ?: rawPush?.takeIf { it.isNotBlank() }
            ?: KeelHref.pick(fromIntent) ?: fromIntent?.takeIf { it.isNotBlank() }
            ?: return
        applyDestination(pushed)
    }

    private fun applyDestination(url: String) {
        val target = KeelHref.pick(url) ?: url.trim().takeIf { it.isNotEmpty() } ?: return
        val current = if (::web.isInitialized) web.url else null
        if (current != null && current != BLANK && current == target) return
        landingUrl = null
        chainSettled = false
        retryPending = false
        loadFailed = false
        redirectRetries = 0
        entryPointRetried = false
        raiseCover()
        runCatching { web.loadUrl(target) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildWebView() {
        val view = WebView(this)
        val s: WebSettings = view.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.allowFileAccess = false
        s.allowContentAccess = true
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.setSupportZoom(false)
        s.builtInZoomControls = false
        s.displayZoomControls = false
        s.mediaPlaybackRequiresUserGesture = false
        s.textZoom = 100
        s.loadsImagesAutomatically = true
        s.blockNetworkImage = false
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        s.userAgentString = KeelChrome.value
        s.setSupportMultipleWindows(false)
        s.javaScriptCanOpenWindowsAutomatically = true

        view.setBackgroundColor(Color.BLACK)
        view.isHorizontalScrollBarEnabled = false
        view.isVerticalScrollBarEnabled = false
        view.webViewClient = pageClient
        view.webChromeClient = chromeClient
        web = view
        lift.bind(view)
        container.addView(
            view,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(view, true)
    }

    private fun replaceWebView() {
        val resumeAt = settledUrl ?: deepestHop ?: vault.readCachedLink() ?: return
        val dead = web
        container.removeView(dead)
        runCatching { dead.destroy() }
        buildWebView()
        web.loadUrl(resumeAt)
    }

    private val pageClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
            val target = req.url?.toString() ?: return false
            val scheme = target.substringBefore(':').lowercase()
            if (req.isForMainFrame && req.hasGesture()) chainSettled = true
            return when {
                scheme in WEB_SCHEMES -> {
                    if (req.isForMainFrame) {
                        deepestHop = target
                    }
                    false
                }
                scheme == "intent" -> {
                    openIntentUri(target)
                    true
                }
                else -> {
                    openExternally(target)
                    true
                }
            }
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            loadFailed = false
            retryPending = false
            if (url != BLANK) deepestHop = url
            if (::lift.isInitialized) lift.wipe()
            if (url != BLANK && !chainSettled) raiseCover()
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (loadFailed || url == BLANK) {
                return
            }
            redirectRetries = 0
            entryPointRetried = false
            retryPending = false
            settledUrl = url
            deepestHop = url
            chainSettled = true
            pinLandingPane()
            CookieManager.getInstance().flush()
            injectCutSheet()
            injectLiftSheet()
            dropCover()
        }

        override fun onReceivedError(view: WebView, req: WebResourceRequest, err: WebResourceError) {
            if (!req.isForMainFrame) return
            loadFailed = true
            val code = err.errorCode
            val description = runCatching { err.description?.toString() }.getOrNull().orEmpty()
            info("main-frame error $code on ${req.url?.host}")
            if (code == ERROR_UNSUPPORTED_SCHEME) {
                dropCover(0L)
                return
            }
            val looping = code == ERROR_REDIRECT_LOOP ||
                code == ERROR_TOO_MANY_REQUESTS ||
                code == -1007 ||
                description.contains("too_many", ignoreCase = true)
            if (looping) {
                resumeChain(view, req.url?.toString().orEmpty())
                return
            }
            if (code in NETWORK_ERRORS || !span.hasAnyAdapter()) {
                view.stopLoading()
                view.loadUrl(BLANK)
                goOffline("main-frame network error $code")
                return
            }
            dropCover(0L)
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            if (isFinishing || view !== web) {
                runCatching { view.destroy() }
                return true
            }
            if (rendererRecoveries >= KeelMark.RENDERER_RECOVERY_MAX) {
                goOffline("renderer recovery budget exhausted")
                return true
            }
            rendererRecoveries++
            replaceWebView()
            return true
        }
    }

    private val chromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            if (newProgress < 100 || view.url == BLANK) return
            if (loadFailed || retryPending) return
            dropCover()
        }

        override fun onShowFileChooser(
            view: WebView,
            callback: ValueCallback<Array<Uri>>,
            params: FileChooserParams,
        ): Boolean {
            filePathCallback?.onReceiveValue(emptyArray())
            filePathCallback = callback
            return try {
                filePicker.launch(params.createIntent())
                true
            } catch (_: Throwable) {
                filePathCallback = null
                false
            }
        }
    }

    private fun resumeChain(view: WebView, failedUrl: String) {
        if (redirectRetries < KeelMark.REDIRECT_RETRY_MAX) {
            redirectRetries++
            retryPending = true
            raiseCover()
            info("redirect loop, resume $redirectRetries")
            queueLoad(view, deepestHop ?: failedUrl)
            return
        }
        val entryPoint = vault.readCachedLink()
        if (!entryPointRetried && !entryPoint.isNullOrEmpty() && entryPoint != deepestHop) {
            entryPointRetried = true
            retryPending = true
            raiseCover()
            queueLoad(view, entryPoint)
            return
        }
        retryPending = false
        dropCover(0L)
    }

    private fun queueLoad(view: WebView, url: String) {
        view.postDelayed({
            if (!isFinishing && !isDestroyed) view.loadUrl(url)
        }, 110L)
    }

    private fun openExternally(url: String) {
        val intent = runCatching {
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.getOrNull() ?: return
        launchOrIgnore(intent)
    }

    private fun openIntentUri(url: String) {
        val parsed = runCatching {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        }.getOrNull() ?: return
        val fallback = parsed.getStringExtra("browser_fallback_url")
        parsed.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        parsed.addCategory(Intent.CATEGORY_BROWSABLE)
        parsed.component = null
        parsed.selector = null
        if (launchOrIgnore(parsed)) return
        parsed.`package` = null
        if (launchOrIgnore(parsed)) return
        KeelHref.pick(fallback)?.let { web.loadUrl(it) }
    }

    private fun launchOrIgnore(intent: Intent): Boolean =
        runCatching { startActivity(intent) }.isSuccess

    private fun watchConnectivity() {
        lifecycleScope.launch {
            span.statusStream().collect { status ->
                if (status == KeelSpan.Status.Offline) goOffline("default network lost")
            }
        }
    }

    private fun startHeartbeat() {
        lifecycleScope.launch {
            while (true) {
                delay(KeelMark.HEARTBEAT_MS)
                if (!resumed || leftForOffline) continue
                if (!span.isReachable()) goOffline("network unreachable")
            }
        }
    }

    private fun goOffline(why: String) {
        if (leftForOffline) return
        retryPending = false
        dropCover(0L)
        if (!resumed) {
            offlineDeferred = true
            info("offline while backgrounded ($why)")
            return
        }
        leftForOffline = true
        info("offline ($why)")
        val resumeAt = settledUrl ?: web.url
        runCatching {
            web.stopLoading()
            web.loadUrl(BLANK)
        }
        startActivity(
            Intent(this, KeelDryActivity::class.java).apply {
                if (!resumeAt.isNullOrEmpty() && resumeAt != BLANK) {
                    putExtra(KeelDryActivity.EXTRA_RESUME_URL, resumeAt)
                }
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
    }

    private fun restoreAfterLoss() {
        val current = web.url
        if (!current.isNullOrEmpty() && current != BLANK) return
        val resumeAt = settledUrl ?: deepestHop ?: vault.readCachedLink() ?: return
        web.loadUrl(resumeAt)
    }

    private fun retreatTowardLanding() {
        if (!::web.isInitialized) return
        pinLandingPane()
        val home = landingUrl
        val current = web.url
        if (current.isNullOrEmpty() || current == BLANK) return
        if (home != null && samePane(current, home)) return
        val list = web.copyBackForwardList()
        val idx = list.currentIndex
        var homeIdx = -1
        if (home != null) {
            for (i in 0 until list.size) {
                val item = list.getItemAtIndex(i)?.url ?: continue
                if (samePane(item, home)) {
                    homeIdx = i
                    break
                }
            }
        }
        if (homeIdx >= 0 && idx > 0 && idx - 1 < homeIdx) {
            web.goBackOrForward(homeIdx - idx)
            return
        }
        if (web.canGoBack()) {
            web.goBack()
            return
        }
        if (home != null && !samePane(current, home)) web.loadUrl(home)
    }

    private fun pinLandingPane() {
        if (landingUrl != null) return
        if (!::web.isInitialized) return
        val now = settledUrl ?: web.url ?: return
        if (now.isEmpty() || now == BLANK) return
        val site = siteKey(now)
        val list = web.copyBackForwardList()
        var firstOnSite: String? = null
        for (i in 0 until list.size) {
            val item = list.getItemAtIndex(i)?.url ?: continue
            if (item == BLANK) continue
            if (siteKey(item) == site) {
                firstOnSite = item
                break
            }
        }
        landingUrl = firstOnSite ?: now
    }

    private fun siteKey(url: String): String {
        val host = runCatching { Uri.parse(url).host }.getOrNull()
            ?.lowercase()
            ?.removePrefix("www.")
            ?: return url
        val parts = host.split('.')
        return if (parts.size >= 2) parts.takeLast(2).joinToString(".") else host
    }

    private fun samePane(a: String, b: String): Boolean {
        fun strip(raw: String): String {
            var s = raw
            val hash = s.indexOf('#')
            if (hash >= 0) s = s.substring(0, hash)
            if (s.endsWith('/') && s.count { it == '/' } > 2) s = s.dropLast(1)
            return s
        }
        return strip(a) == strip(b)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        KeelImmersion.apply(this)
        if (::container.isInitialized) container.requestApplyInsets()
        if (::lift.isInitialized) lift.afterTurn()
    }

    private fun raiseCover() {
        if (!::shell.isInitialized) return
        coverJob?.cancel()
        coverJob = null
        val existing = cover
        if (existing != null) {
            existing.animate().cancel()
            existing.alpha = 1f
            return
        }
        val spinnerPx = (56 * resources.displayMetrics.density + 0.5f).toInt()
        val fresh = FrameLayout(this).apply {
            setBackgroundColor(COVER_FILL)
            isClickable = true
            addView(
                ProgressBar(this@KeelPaneActivity).apply {
                    isIndeterminate = true
                    indeterminateTintList = ColorStateList.valueOf(COVER_SPINNER)
                },
                FrameLayout.LayoutParams(spinnerPx, spinnerPx, Gravity.CENTER),
            )
        }
        cover = fresh
        shell.addView(
            fresh,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        lifecycleScope.launch {
            delay(COVER_MAX_MS)
            if (cover === fresh) {
                info("loading cover timed out")
                dropCover(0L)
            }
        }
    }

    private fun dropCover(after: Long = COVER_LINGER_MS) {
        if (retryPending) return
        val current = cover ?: return
        coverJob?.cancel()
        coverJob = lifecycleScope.launch {
            delay(after)
            if (cover !== current) return@launch
            cover = null
            current.animate().alpha(0f).setDuration(150L).withEndAction {
                if (::shell.isInitialized && !isDestroyed) shell.removeView(current)
            }.start()
        }
    }

    /**
     * Cutout is applied natively on the shell. Do not rewrite viewport or
     * padding/margin on html/body/#app — that strips partner frames.
     */
    private fun injectCutSheet() {
        val js = """(function(){
          if(window.__vlCutBound) return; window.__vlCutBound=1;
        })();"""
        runCatching { web.evaluateJavascript(js, null) }
    }

    private fun injectLiftSheet() {
        if (!::lift.isInitialized) return
        runCatching { web.evaluateJavascript(lift.sheet, null) }
    }

    private fun info(message: String) {
        if (BuildConfig.DEBUG) Log.i(TAG, message)
    }

    companion object {
        const val EXTRA_TARGET_URL = "vl_pane_href"
        private const val TAG = "KeelPane"
        private const val BLANK = "about:blank"
        private val WEB_SCHEMES = setOf("http", "https", "about", "data", "blob", "file", "javascript")
        private val NETWORK_ERRORS = setOf(-2, -6, -7, -8, -11)
        private const val COVER_LINGER_MS = 120L
        private const val COVER_MAX_MS = 20_000L
        private const val COVER_FILL = 0xFF0B0B0F.toInt()
        private const val COVER_SPINNER = 0xFFF2C464.toInt()
    }
}
