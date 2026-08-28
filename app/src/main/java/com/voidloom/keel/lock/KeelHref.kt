package com.voidloom.keel.lock

/**
 * Normalizes a backend URL for WebView. No host allowlist — any http(s)
 * destination from config must load as-is.
 */
internal object KeelHref {

    fun accepts(raw: String?): Boolean = pick(raw) != null

    fun pick(raw: String?): String? {
        var candidate = raw?.trim()?.trim('"', '\'', '`') ?: return null
        if (candidate.isEmpty()) return null
        candidate = candidate.replace("&amp;", "&").replace("\\/", "/")
        if (candidate.startsWith("//")) candidate = "https:$candidate"
        if (!candidate.contains("://")) {
            candidate = "https://$candidate"
        }
        val scheme = candidate.substringBefore(':').lowercase()
        if (scheme != "http" && scheme != "https") return null
        val rest = candidate.substring(scheme.length + 3)
        if (rest.isEmpty() || rest.startsWith("/")) return null
        return candidate
    }
}
