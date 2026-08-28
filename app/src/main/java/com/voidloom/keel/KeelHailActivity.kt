package com.voidloom.keel

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.voidloom.keel.core.KeelImmersion
import com.voidloom.keel.face.KeelHailView
import com.voidloom.keel.wire.KeelVault

class KeelHailActivity : ComponentActivity() {

    private lateinit var vault: KeelVault
    private var targetUrl: String? = null

    private val askOs = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) vault.markPushAllowed(true) else vault.markPushBlockedByOs()
        proceed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KeelImmersion.apply(this)
        vault = KeelVault(this)
        targetUrl = intent.getStringExtra(EXTRA_TARGET_URL)
        setContentView(
            KeelHailView(
                this,
                onAccept = ::onAccept,
                onSkip = {
                    vault.armInviteCooldown()
                    proceed()
                },
            ),
        )
    }

    private fun onAccept() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            vault.markPushAllowed(true)
            proceed()
            return
        }
        if (vault.osGranted()) {
            vault.markPushAllowed(true)
            proceed()
            return
        }
        vault.markOsAsked()
        askOs.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun proceed() {
        startActivity(
            Intent(this, KeelPaneActivity::class.java)
                .putExtra(KeelPaneActivity.EXTRA_TARGET_URL, targetUrl)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        overridePendingTransition(0, 0)
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

    companion object {
        const val EXTRA_TARGET_URL = "vl_hail_href"
    }
}
