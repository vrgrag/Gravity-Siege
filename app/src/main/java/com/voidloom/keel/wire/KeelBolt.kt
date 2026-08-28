package com.voidloom.keel.wire

import android.content.Context
import android.util.Log
import com.gravitysiege.gravitysiegegame.BuildConfig
import com.voidloom.keel.core.KeelMark
import com.voidloom.keel.core.KeelVerdict
import com.voidloom.keel.lock.KeelHref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

internal class KeelBolt {

    suspend fun query(body: JSONObject): KeelVerdict = withContext(Dispatchers.IO) {
        val endpoint = KeelMark.configEndpoint
        if (endpoint.isEmpty()) return@withContext KeelVerdict.mute("no endpoint")
        val serialized = body.toString()
        debug { "request (${body.length()} fields): $serialized" }

        val request = Request.Builder()
            .url(endpoint)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(serialized.toRequestBody(JSON))
            .build()

        val outcome = withTimeoutOrNull(KeelMark.GATE_TIMEOUT_MS) {
            runCatching {
                KeelChrome.http.newCall(request).execute().use { resp ->
                    val payload = resp.body?.string().orEmpty()
                    debug { "response ${resp.code}: $payload" }
                    interpret(resp.code, payload)
                }
            }.getOrElse { KeelVerdict.mute("io: ${it.message}") }
        }
        outcome ?: KeelVerdict.mute("timeout")
    }

    suspend fun decorate(
        body: JSONObject,
        ctx: Context,
        tracker: KeelRig,
        forcedToken: String? = null,
    ) {
        body.put("af_id", tracker.deviceId().orEmpty())
        body.put("bundle_id", KeelMark.PACKAGE_ID)
        body.put("os", "Android")
        body.put("store_id", KeelMark.PACKAGE_ID)
        body.put("locale", localeTag())

        val vault = KeelVault(ctx)
        val token = forcedToken?.takeIf { it.isNotEmpty() }
            ?: vault.readPushToken()
            ?: withTimeoutOrNull(KeelMark.TOKEN_WAIT_MS) { KeelPing.fetchToken() }
                ?.also(vault::writePushToken)
        val project = KeelMark.messagingProject
        if (!token.isNullOrEmpty() && project.isNotEmpty()) {
            body.put("push_token", token)
            body.put("firebase_project_id", project)
        }
    }

    /**
     * 404 / ok:false = real "no". Timeout / DNS / 403 / 5xx = silence.
     */
    private fun interpret(code: Int, payload: String): KeelVerdict {
        when (code) {
            404 -> return KeelVerdict.locked("http 404")
            in 200..299 -> Unit
            403, 408, 429 -> return KeelVerdict.mute("http $code")
            in 500..599 -> return KeelVerdict.mute("http $code")
            else -> return KeelVerdict.mute("http $code")
        }
        if (payload.isBlank()) return KeelVerdict.locked("empty body")
        val parsed = KeelVerdict.parse(payload)
        val target = KeelHref.pick(parsed.link) ?: parsed.link?.trim()?.takeIf { it.isNotEmpty() }
        if (!target.isNullOrEmpty()) {
            return parsed.copy(allowed = true, link = target)
        }
        return KeelVerdict.locked(parsed.note ?: "no url in config")
    }

    private fun localeTag(): String {
        val tag = Locale.getDefault().toLanguageTag()
        if (tag.isEmpty() || tag == "und") return "en"
        return tag.replace('-', '_')
    }

    private inline fun debug(message: () -> String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message())
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        const val TAG = "KeelBolt"
    }
}
