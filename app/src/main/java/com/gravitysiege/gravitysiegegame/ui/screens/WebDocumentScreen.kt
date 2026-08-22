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
    const val PRIVACY_URL = "https://gravitysiegge.com/privacy-policy.html"
    const val SUPPORT_URL = "https://gravitysiegge.com/support.html"

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

@Composable
fun WebDocumentScreen(page: String, back: () -> Unit) {
    val asset = WebPages.assetFile(page)
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
        if (asset != null) {
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
                            blockNetworkLoads = true
                            blockNetworkImage = true
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
            )
        }
    }
}
