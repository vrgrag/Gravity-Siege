package com.gravitysiege.gravitysiegegame

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gravitysiege.gravitysiegegame.game.RiskMode
import com.gravitysiege.gravitysiegegame.game.Skin
import com.gravitysiege.gravitysiegegame.game.Skins
import java.time.LocalDate

class GameStore private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("gravity_siege", Context.MODE_PRIVATE)

    var coins by mutableIntStateOf(prefs.getInt("coins", STARTING_COINS))
        private set
    var bet by mutableIntStateOf(prefs.getInt("betValue", BET_STEPS[2]))
        private set
    var mode by mutableStateOf(RiskMode.byName(prefs.getString("mode", null)))
        private set
    var soundOn by mutableStateOf(prefs.getBoolean("sound", true))
        private set
    var biggestWin by mutableIntStateOf(prefs.getInt("biggest", 0))
        private set
    var tallestTower by mutableIntStateOf(prefs.getInt("tallest", 0))
        private set

    var skinId by mutableStateOf(prefs.getString("skin", Skins.DEFAULT_ID) ?: Skins.DEFAULT_ID)
        private set
    var ownedSkins by mutableStateOf(
        prefs.getStringSet("ownedSkins", null)?.toSet() ?: setOf(Skins.DEFAULT_ID),
    )
        private set

    private var claimedDay by mutableLongStateOf(prefs.getLong("dailyDay", NEVER))
    var streak by mutableIntStateOf(prefs.getInt("dailyStreak", 0))
        private set

    val skin: Skin get() = Skins.byId(skinId)

    /** The most that can be staked right now: never more than is on hand. */
    val betCeiling: Int get() = maxOf(BET_STEPS.first(), coins)

    fun owns(skin: Skin): Boolean = skin.free || skin.id in ownedSkins

    fun setSound(value: Boolean) {
        soundOn = value
        save()
    }

    /**
     * Walks the stake up or down. Past the top of the ladder it keeps doubling,
     * so a rich player is not stuck at the largest printed step.
     */
    fun cycleBet(delta: Int) {
        bet = if (delta > 0) {
            BET_STEPS.firstOrNull { it > bet } ?: bet * 2
        } else {
            BET_STEPS.lastOrNull { it < bet } ?: (bet / 2)
        }
        settleBet()
    }

    fun doubleBet() {
        bet *= 2
        settleBet()
    }

    fun allIn() {
        bet = betCeiling
        settleBet()
    }

    fun pickMode(value: RiskMode) {
        mode = value
        save()
    }

    private fun settleBet() {
        bet = bet.coerceIn(BET_STEPS.first(), betCeiling)
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

    // --- crews --------------------------------------------------------------- #

    /** Buys [skin] outright and puts the crew straight to work. */
    fun buySkin(skin: Skin): Boolean {
        if (owns(skin)) return false
        if (!spend(skin.price)) return false
        ownedSkins = ownedSkins + skin.id
        skinId = skin.id
        save()
        return true
    }

    fun equipSkin(skin: Skin): Boolean {
        if (!owns(skin) || skinId == skin.id) return false
        skinId = skin.id
        save()
        return true
    }

    // --- daily drop ----------------------------------------------------------- #

    /** Today as a calendar day in the device's own zone, so midnight is local. */
    private fun today(): Long = LocalDate.now().toEpochDay()

    val dailyReady: Boolean get() = claimedDay != today()

    /**
     * The streak the next claim would land on. Claiming on consecutive days
     * carries the run forward; missing a day starts it over at one.
     */
    val nextStreakDay: Int
        get() = when {
            !dailyReady -> streak.coerceAtLeast(1)
            claimedDay == today() - 1 -> streak + 1
            else -> 1
        }

    val nextDailyReward: Int get() = rewardFor(nextStreakDay)

    /** Pays out today's drop. Returns zero when it has already been taken. */
    fun claimDaily(): Int {
        if (!dailyReady) return 0
        val day = nextStreakDay
        val reward = rewardFor(day)
        streak = day
        claimedDay = today()
        coins += reward
        save()
        return reward
    }

    private fun save() {
        prefs.edit()
            .putInt("coins", coins)
            .putInt("betValue", bet)
            .putString("mode", mode.name)
            .putBoolean("sound", soundOn)
            .putInt("biggest", biggestWin)
            .putInt("tallest", tallestTower)
            .putString("skin", skinId)
            .putStringSet("ownedSkins", ownedSkins)
            .putLong("dailyDay", claimedDay)
            .putInt("dailyStreak", streak)
            .apply()
    }

    companion object {
        const val STARTING_COINS = 2500
        const val RELIEF_COINS = 400
        val BET_STEPS = listOf(10, 25, 50, 100, 250, 500, 1000)

        /** A week of drops; the run keeps paying the last step after that. */
        val DAILY_STEPS = listOf(200, 350, 550, 800, 1200, 1800, 3000)

        fun rewardFor(streakDay: Int): Int =
            DAILY_STEPS[(streakDay - 1).coerceIn(0, DAILY_STEPS.lastIndex)]

        private const val NEVER = Long.MIN_VALUE

        @Volatile
        private var instance: GameStore? = null

        fun get(context: Context): GameStore {
            return instance ?: synchronized(this) {
                instance ?: GameStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
