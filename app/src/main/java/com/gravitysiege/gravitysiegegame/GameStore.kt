package com.gravitysiege.gravitysiegegame

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class GameStore private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("gravity_siege", Context.MODE_PRIVATE)

    var coins by mutableIntStateOf(prefs.getInt("coins", STARTING_COINS))
        private set
    var betIndex by mutableIntStateOf(prefs.getInt("betIndex", 2))
        private set
    var soundOn by mutableStateOf(prefs.getBoolean("sound", true))
        private set
    var biggestWin by mutableIntStateOf(prefs.getInt("biggest", 0))
        private set
    var tallestTower by mutableIntStateOf(prefs.getInt("tallest", 0))
        private set

    val bet: Int get() = BET_STEPS[betIndex.coerceIn(0, BET_STEPS.lastIndex)]

    fun setSound(value: Boolean) {
        soundOn = value
        save()
    }

    fun cycleBet(delta: Int) {
        betIndex = (betIndex + delta).coerceIn(0, BET_STEPS.lastIndex)
        save()
    }

    fun doubleBet() {
        betIndex = (betIndex + 1).coerceAtMost(BET_STEPS.lastIndex)
        save()
    }

    fun spend(amount: Int): Boolean {
        if (amount <= 0 || amount > coins) return false
        coins -= amount
        save()
        return true
    }

    fun credit(amount: Int) {
        if (amount <= 0) return
        coins += amount
        if (amount > biggestWin) biggestWin = amount
        save()
    }

    fun recordHeight(floors: Int) {
        if (floors > tallestTower) {
            tallestTower = floors
            save()
        }
    }

    fun grantRelief(): Int {
        if (coins >= BET_STEPS.first()) return 0
        coins += RELIEF_COINS
        save()
        return RELIEF_COINS
    }

    private fun save() {
        prefs.edit()
            .putInt("coins", coins)
            .putInt("betIndex", betIndex)
            .putBoolean("sound", soundOn)
            .putInt("biggest", biggestWin)
            .putInt("tallest", tallestTower)
            .apply()
    }

    companion object {
        const val STARTING_COINS = 2500
        const val RELIEF_COINS = 400
        val BET_STEPS = listOf(10, 25, 50, 100, 250, 500, 1000)

        @Volatile
        private var instance: GameStore? = null

        fun get(context: Context): GameStore {
            return instance ?: synchronized(this) {
                instance ?: GameStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
