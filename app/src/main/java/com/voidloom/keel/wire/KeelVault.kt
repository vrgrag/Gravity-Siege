package com.voidloom.keel.wire

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.voidloom.keel.core.KeelMark
import com.voidloom.keel.core.KeelTrail

internal class KeelVault(context: Context) {

    private val app = context.applicationContext

    private val plain: SharedPreferences =
        app.getSharedPreferences(PLAIN_FILE, Context.MODE_PRIVATE)

    private val sealed: SharedPreferences? = runCatching {
        val key = MasterKey.Builder(app)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            app,
            SECRET_FILE,
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrNull()

    private val volatileStore = HashMap<String, String?>()

    private fun readSealed(key: String): String? =
        sealed?.getString(key, null) ?: volatileStore[key]

    private fun writeSealed(key: String, value: String?) {
        val store = sealed
        if (store == null) {
            volatileStore[key] = value
            return
        }
        val editor = store.edit()
        if (value.isNullOrEmpty()) editor.remove(key) else editor.putString(key, value)
        editor.apply()
    }

    fun readTrail(): KeelTrail = KeelTrail.fromWire(plain.getString(K_TRAIL, null))

    fun writeTrail(trail: KeelTrail) {
        plain.edit().putString(K_TRAIL, trail.wire).apply()
    }

    fun readCachedLink(): String? = readSealed(K_CACHED_LINK)

    fun writeCachedLink(link: String) = writeSealed(K_CACHED_LINK, link)

    fun readLinkTtl(): Long? =
        if (plain.contains(K_LINK_TTL)) plain.getLong(K_LINK_TTL, 0L) else null

    fun writeLinkTtl(unixSeconds: Long) {
        plain.edit().putLong(K_LINK_TTL, unixSeconds).apply()
    }

    fun hasUsableLink(): Boolean {
        if (readCachedLink().isNullOrEmpty()) return false
        val ttl = readLinkTtl() ?: return true
        return ttl == 0L || nowSeconds() < ttl
    }

    fun stashPushLink(link: String?) = writeSealed(K_PUSH_LINK, link)

    fun takePushLink(): String? {
        val link = readSealed(K_PUSH_LINK) ?: return null
        writeSealed(K_PUSH_LINK, null)
        return link.ifEmpty { null }
    }

    fun readPushToken(): String? = readSealed(K_PUSH_TOKEN)

    fun writePushToken(token: String?) = writeSealed(K_PUSH_TOKEN, token)

    fun isPushAllowed(): Boolean = plain.getBoolean(K_PUSH_ALLOWED, false)

    fun markPushAllowed(value: Boolean) {
        plain.edit().putBoolean(K_PUSH_ALLOWED, value).commit()
    }

    fun isPushBlockedByOs(): Boolean = plain.getBoolean(K_PUSH_BLOCKED_OS, false)

    fun markPushBlockedByOs() {
        plain.edit().putBoolean(K_PUSH_BLOCKED_OS, true).commit()
    }

    fun wasOsAsked(): Boolean = plain.getBoolean(K_PUSH_OS_ASKED, false)

    fun markOsAsked() {
        plain.edit().putBoolean(K_PUSH_OS_ASKED, true).commit()
    }

    fun armInviteCooldown() {
        plain.edit()
            .putLong(K_INVITE_UNTIL, nowSeconds() + KeelMark.INVITE_COOLDOWN_SECONDS)
            .commit()
    }

    fun shouldOfferInvite(host: Activity): Boolean {
        if (osGranted()) {
            markPushAllowed(true)
            return false
        }
        if (isPushAllowed() || isPushBlockedByOs()) return false
        val until = if (plain.contains(K_INVITE_UNTIL)) plain.getLong(K_INVITE_UNTIL, 0L) else 0L
        return nowSeconds() >= until
    }

    fun osGranted(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000L

    private companion object {
        const val PLAIN_FILE = "vl_crane_plain"
        const val SECRET_FILE = "vl_crane_seal"
        const val K_TRAIL = "yard_mark"
        const val K_CACHED_LINK = "hoist_href"
        const val K_LINK_TTL = "hoist_ttl"
        const val K_PUSH_LINK = "bell_href"
        const val K_PUSH_TOKEN = "bell_tok"
        const val K_INVITE_UNTIL = "ask_dawn"
        const val K_PUSH_ALLOWED = "bell_ok"
        const val K_PUSH_BLOCKED_OS = "bell_halt"
        const val K_PUSH_OS_ASKED = "bell_asked"
    }
}
