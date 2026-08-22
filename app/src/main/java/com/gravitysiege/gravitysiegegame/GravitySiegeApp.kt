package com.gravitysiege.gravitysiegegame

import android.app.Application

class GravitySiegeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GameStore.get(this)
    }
}
