package com.voidloom.keel.wire

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gravitysiege.gravitysiegegame.BuildConfig
import com.gravitysiege.gravitysiegegame.R
import com.voidloom.keel.KeelApp
import com.voidloom.keel.KeelBootActivity
import com.voidloom.keel.core.KeelTrail
import com.voidloom.keel.lock.KeelHref
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

internal object KeelPing {

    const val CHANNEL_ID = "keel_yard_bell"
    private const val CHANNEL_NAME = "Yard alerts"
    private const val CHANNEL_DESC = "Siege bonuses and yard updates"
    private const val TAG = "KeelPing"

    const val EXTRA_URL = "vl_href"
    const val EXTRA_FROM_PUSH = "vl_from_bell"

    private val RAW_URL_KEYS = listOf("url", "link", "target_url")

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            },
        )
    }

    suspend fun fetchToken(): String? = suspendCancellableCoroutine { cont ->
        runCatching {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        }.onFailure {
            if (cont.isActive) cont.resume(null)
        }
    }

    fun extractUrl(intent: Intent?): String? {
        if (intent == null) return null
        val own = intent.getStringExtra(EXTRA_URL)
        val raw = RAW_URL_KEYS.firstNotNullOfOrNull { intent.getStringExtra(it) }
        return KeelHref.pick(own ?: raw)
    }

    internal fun buildTapIntent(ctx: Context, url: String?): Intent =
        Intent(ctx, KeelBootActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_FROM_PUSH, true)
            if (!url.isNullOrEmpty()) putExtra(EXTRA_URL, url)
        }

    internal fun render(ctx: Context, title: String, body: String, url: String?, imageUrl: String?) {
        ensureChannel(ctx)
        val pending = PendingIntent.getActivity(
            ctx,
            System.currentTimeMillis().toInt(),
            buildTapIntent(ctx, url),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_keel_flame)
            .setColor(ContextCompat.getColor(ctx, R.color.keel_signal))
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val bitmap = imageUrl?.takeIf { it.isNotEmpty() }?.let(::loadBitmap)
        if (bitmap != null) {
            builder
                .setLargeIcon(bitmap)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null as Bitmap?),
                )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        val id = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
        val mgr = NotificationManagerCompat.from(ctx)
        if (!mgr.areNotificationsEnabled()) return
        runCatching { mgr.notify(id, builder.build()) }
            .onFailure {
                if (BuildConfig.DEBUG) Log.w(TAG, "notify failed: ${it.message}")
            }
    }

    suspend fun relayFreshToken(ctx: Context, token: String) {
        if (token.isEmpty()) return
        val app = ctx.applicationContext as? KeelApp ?: return
        val tracker = runCatching { app.tracker }.getOrNull() ?: return
        val vault = KeelVault(ctx)
        val body = tracker.collectBody(firstLaunch = false)
        val ask = KeelBolt()
        ask.decorate(body, ctx, tracker, forcedToken = token)
        val reply = ask.query(body)
        if (reply.hasLink) {
            vault.writeTrail(KeelTrail.Web)
            reply.link?.let(vault::writeCachedLink)
            reply.ttl?.let(vault::writeLinkTtl)
        }
    }

    private fun loadBitmap(url: String): Bitmap? = runCatching {
        KeelChrome.http.newCall(okhttp3.Request.Builder().url(url).get().build())
            .execute().use { resp ->
                val body = resp.body
                if (!resp.isSuccessful || body == null) null
                else body.byteStream().use { BitmapFactory.decodeStream(it) }
            }
    }.getOrNull()
}

internal object KeelPass {
    private val sink = AtomicReference<((String) -> Unit)?>(null)
    @Volatile var paneAlive: Boolean = false
    private val parked = AtomicReference<String?>(null)

    fun attach(sinkFn: (String) -> Unit) { sink.set(sinkFn) }
    fun detach() { sink.set(null) }

    fun offer(url: String): Boolean {
        sink.get()?.let { deliver ->
            deliver(url)
            return true
        }
        if (!paneAlive) return false
        parked.set(url)
        return true
    }

    fun drain(): String? = parked.getAndSet(null)
}

class KeelPingService : FirebaseMessagingService() {

    private val bg = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onDestroy() {
        bg.cancel()
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val vault = KeelVault(applicationContext)
        vault.writePushToken(token)
        bg.launch { KeelPing.relayFreshToken(applicationContext, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val notification = message.notification
        val title = data["title"] ?: notification?.title.orEmpty()
        val body = data["body"] ?: notification?.body.orEmpty()
        if (title.isEmpty() && body.isEmpty()) return

        val url = KeelHref.pick(data["url"] ?: data["link"] ?: data["target_url"])
        val image = data["image"] ?: notification?.imageUrl?.toString()
        val vault = KeelVault(applicationContext)
        val trail = vault.readTrail()

        if (url != null && trail != KeelTrail.Native) {
            if (KeelPass.offer(url)) {
                bg.launch { KeelPing.render(applicationContext, title, body, null, image) }
                return
            }
            vault.stashPushLink(url)
        }
        val tapUrl = if (trail == KeelTrail.Native) null else url
        bg.launch { KeelPing.render(applicationContext, title, body, tapUrl, image) }
    }
}
