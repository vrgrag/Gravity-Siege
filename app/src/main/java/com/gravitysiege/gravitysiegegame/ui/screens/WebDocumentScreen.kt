package com.gravitysiege.gravitysiegegame.ui.screens

import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gravitysiege.gravitysiegegame.ui.components.ScreenHeader

object WebPages {
    const val PRIVACY = "privacy-policy"
    const val SUPPORT = "support"
    const val PRIVACY_URL = "https://gravittysiege.com/privacy-policy.html"
    const val SUPPORT_URL = "https://gravittysiege.com/support.html"

    fun title(page: String): String = when (page) {
        PRIVACY -> "Privacy Policy"
        SUPPORT -> "Support"
        else -> "Document"
    }

    fun assetFile(page: String): String? = when (page) {
        PRIVACY -> "web/privacy-policy.html"
        SUPPORT -> "web/support.html"
        else -> null
    }
}

/**
 * Renders a bundled document. Everything is served from the app's assets and the
 * WebView is kept offline, so the pages read the same with or without a connection.
 */
@Composable
fun WebDocumentScreen(page: String, back: () -> Unit) {
    val asset = WebPages.assetFile(page)
    var web by remember(page) { mutableStateOf<WebView?>(null) }

    BackHandler {
        val view = web
        if (view != null && view.canGoBack()) view.goBack() else back()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(ComposeColor.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = WebPages.title(page),
            onBack = back,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (asset != null) {
            val allowJavaScript = page == WebPages.SUPPORT
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        setBackgroundColor(Color.WHITE)
                        isVerticalScrollBarEnabled = true
                        isHorizontalScrollBarEnabled = false
                        overScrollMode = WebView.OVER_SCROLL_NEVER
                        settings.apply {
                            javaScriptEnabled = allowJavaScript
                            domStorageEnabled = allowJavaScript
                            allowFileAccess = true
                            allowContentAccess = true
                            blockNetworkLoads = true
                            blockNetworkImage = true
                            cacheMode = WebSettings.LOAD_NO_CACHE
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                            setSupportMultipleWindows(false)
                            javaScriptCanOpenWindowsAutomatically = false
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean = true
                        }
                        loadUrl("file:///android_asset/$asset")
                    }
                },
                update = { view -> web = view },
                onRelease = { view ->
                    web = null
                    view.stopLoading()
                    view.destroy()
                },
            )
        }
    }
}
