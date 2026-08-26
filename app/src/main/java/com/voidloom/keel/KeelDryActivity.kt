package com.voidloom.keel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.voidloom.keel.core.KeelImmersion
import com.voidloom.keel.face.KeelDryView
import com.voidloom.keel.wire.KeelPing
import com.voidloom.keel.wire.KeelSpan
import com.voidloom.keel.wire.KeelVault
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class KeelDryActivity : ComponentActivity() {

    private lateinit var span: KeelSpan
    private lateinit var dryView: KeelDryView
    private var resumeUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KeelImmersion.apply(this)
        span = KeelSpan(this)
        resumeUrl = intent.getStringExtra(EXTRA_RESUME_URL)
        dryView = KeelDryView(this) {
            if (!dryView.busy) {
                dryView.busy = true
                lifecycleScope.launch {
                    if (!attemptResume()) dryView.busy = false
                }
            }
        }
        setContentView(dryView)
        lifecycleScope.launch {
            span.statusStream().collect { status ->
                if (status == KeelSpan.Status.Online) attemptResume()
            }
        }
    }

    private suspend fun attemptResume(): Boolean {
        if (!span.isReachable()) return false
        withTimeoutOrNull(2_650L) { KeelPing.fetchToken() }
            ?.also { KeelVault(this).writePushToken(it) }
        val saved = resumeUrl
        val next = if (!saved.isNullOrEmpty()) {
            Intent(this, KeelPaneActivity::class.java)
                .putExtra(KeelPaneActivity.EXTRA_TARGET_URL, saved)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } else {
            Intent(this, KeelBootActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(next)
        overridePendingTransition(0, 0)
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        KeelImmersion.apply(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) KeelImmersion.apply(this)
    }

    companion object {
        const val EXTRA_RESUME_URL = "vl_dry_href"
    }
}
