package com.voidloom.keel.wire

import android.os.Build
import com.voidloom.keel.mix.KeelHidden
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * One Chrome-on-Android UA for config, GCD and WebView.
 * Chrome 151, no `; wv`, no okhttp/Dart, no appid/appname.
 */
internal object KeelChrome {

    val value: String by lazy(::build)

    val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(11, TimeUnit.SECONDS)
            .readTimeout(17, TimeUnit.SECONDS)
            .callTimeout(22, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request: Request = chain.request()
                chain.proceed(
                    if (request.header("User-Agent") == null) {
                        request.newBuilder().header("User-Agent", value).build()
                    } else {
                        request
                    },
                )
            }
            .build()
    }

    private fun build(): String {
        val chrome = KeelHidden.chromeVersion().ifEmpty { "151.0.7742.68" }
        val webkit = KeelHidden.webkitVersion().ifEmpty { "537.36" }
        val release = Build.VERSION.RELEASE ?: "14"
        val manufacturer = (Build.MANUFACTURER ?: "Google").replaceFirstChar { it.uppercaseChar() }
        val model = Build.MODEL ?: "Pixel 8"
        val buildTag = (Build.ID ?: "AP3A").take(18)
        return "Mozilla/5.0 (Linux; Android $release; $manufacturer $model Build/$buildTag) " +
            "AppleWebKit/$webkit (KHTML, like Gecko) " +
            "Chrome/$chrome Mobile Safari/$webkit"
    }
}
