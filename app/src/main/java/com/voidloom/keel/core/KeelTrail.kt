package com.voidloom.keel.core

import com.voidloom.keel.lock.KeelHref
import org.json.JSONObject

internal enum class KeelTrail(val wire: String) {
    Web("hoist"),
    Native("slab"),
    Pending("void");

    companion object {
        fun fromWire(raw: String?): KeelTrail = when (raw) {
            "hoist" -> Web
            "slab" -> Native
            else -> Pending
        }
    }
}

/**
 * Config reply. [answered] is false when nothing reached the wire
 * (timeout / DNS / 403). Only a real HTTP "no" may lock Native.
 */
internal data class KeelVerdict(
    val allowed: Boolean,
    val link: String? = null,
    val note: String? = null,
    val ttl: Long? = null,
    val answered: Boolean = true,
) {
    val hasLink: Boolean get() = !link.isNullOrEmpty()

    companion object {
        fun parse(body: String): KeelVerdict {
            val trimmed = body.trim()
            if (trimmed.isEmpty()) return locked("empty body")
            extractBareUrl(trimmed)?.let { link ->
                return KeelVerdict(allowed = true, link = link, answered = true)
            }
            return try {
                val raw = unwrap(JSONObject(trimmed))
                val link = extractLink(raw)
                val ttl = when {
                    !raw.has("expires") || raw.isNull("expires") -> null
                    else -> raw.optLong("expires").takeIf { it != 0L }
                        ?: raw.optString("expires").toLongOrNull()
                }
                KeelVerdict(
                    allowed = link != null || parseOk(raw),
                    link = link,
                    note = raw.optString("message").takeIf { it.isNotEmpty() },
                    ttl = ttl,
                    answered = true,
                )
            } catch (t: Throwable) {
                extractBareUrl(trimmed)?.let { link ->
                    KeelVerdict(allowed = true, link = link, answered = true)
                } ?: locked("parse: ${t.message}")
            }
        }

        private val LINK_KEYS = listOf(
            "url", "link", "href", "target_url", "offer_url", "offer",
            "webview_url", "web_url", "goto", "redirect", "destination",
        )
        private val WRAP_KEYS = listOf("data", "result", "payload", "response", "body")

        private fun unwrap(raw: JSONObject): JSONObject {
            for (key in WRAP_KEYS) {
                val nested = raw.optJSONObject(key) ?: continue
                if (LINK_KEYS.any { nested.has(it) && !nested.isNull(it) }) return nested
            }
            return raw
        }

        private fun extractLink(raw: JSONObject): String? {
            for (key in LINK_KEYS) {
                if (!raw.has(key) || raw.isNull(key)) continue
                when (val value = raw.opt(key)) {
                    is String -> extractBareUrl(value)?.let { return it }
                    is JSONObject -> extractLink(value)?.let { return it }
                }
            }
            for (key in WRAP_KEYS) {
                extractLink(raw.optJSONObject(key) ?: continue)?.let { return it }
            }
            return null
        }

        private fun extractBareUrl(raw: String): String? {
            val cleaned = raw.trim().trim('"', '\'')
            if (cleaned.isEmpty()) return null
            if (cleaned.startsWith("{") || cleaned.startsWith("[")) return null
            return KeelHref.pick(cleaned)
        }

        private fun parseOk(raw: JSONObject): Boolean {
            val value = when {
                raw.has("ok") -> raw.opt("ok")
                raw.has("success") -> raw.opt("success")
                raw.has("status") -> raw.opt("status")
                else -> return false
            }
            return when (value) {
                is Boolean -> value
                is Number -> value.toInt() != 0
                is String -> value.equals("true", true) ||
                    value == "1" ||
                    value.equals("yes", true) ||
                    value.equals("ok", true)
                else -> false
            }
        }

        fun locked(note: String): KeelVerdict =
            KeelVerdict(allowed = false, note = note, answered = true)

        fun mute(note: String): KeelVerdict =
            KeelVerdict(allowed = false, note = note, answered = false)
    }
}
