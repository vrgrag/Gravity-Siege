package com.voidloom.keel

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.gravitysiege.gravitysiegegame.BuildConfig
import com.voidloom.keel.wire.KeelRig

class KeelApp : Application() {

    lateinit var tracker: KeelRig
        private set

    override fun onCreate() {
        super.onCreate()
        runCatching { FirebaseApp.initializeApp(this) }
            .onFailure { if (BuildConfig.DEBUG) Log.w(TAG, "Firebase unavailable: ${it.message}") }
        tracker = KeelRig(this)
        tracker.prime()
    }

    private companion object {
        const val TAG = "KeelApp"
    }
}
