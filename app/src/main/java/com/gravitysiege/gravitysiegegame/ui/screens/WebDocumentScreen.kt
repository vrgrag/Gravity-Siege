package com.gravitysiege.gravitysiegegame.ui.screens

import android.graphics.Color
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gravitysiege.gravitysiegegame.ui.components.ScreenHeader

object WebPages {
    const val PRIVACY = "privacy-policy"
    const val SUPPORT = "support"
    const val PRIVACY_URL = "https://vrgrag.github.io/pp/"
    const val SUPPORT_URL = "https://gravitysiegge.com/support.html"

    fun title(page: String): String = when (page) {
        PRIVACY -> "Privacy Policy"
        SUPPORT -> "Support"
        else -> "Document"
    }

    fun loadUrl(page: String): String? = when (page) {
        PRIVACY -> PRIVACY_URL
        SUPPORT -> "file:///android_asset/web/support.html"
        else -> null
    }

    /** Privacy is fetched from the web; support stays bundled for offline use. */
    fun needsNetwork(page: String): Boolean = page == PRIVACY
}

@Composable
fun WebDocumentScreen(page: String, back: () -> Unit) {
    val url = WebPages.loadUrl(page)
    Column(
        Modifier
            .fillMaxSize()
            .background(ComposeColor.White)
            .statusBarsPadding(),
    ) {
        ScreenHeader(
            title = WebPages.title(page),
            onBack = back,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (url != null) {
            val online = WebPages.needsNetwork(page)
            val allowJavaScript = page == WebPages.SUPPORT
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        setBackgroundColor(Color.WHITE)
                        settings.apply {
                            javaScriptEnabled = allowJavaScript
                            domStorageEnabled = allowJavaScript
                            allowFileAccess = true
                            allowContentAccess = true
                            blockNetworkLoads = !online
                            blockNetworkImage = !online
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean = !online
                        }
                        loadUrl(url)
                    }
                },
            )
        }
    }
}
