package com.gravitysiege.gravitysiegegame.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

/**
 * Layout and motion numbers for the yard, expressed in field units instead of
 * pixels so the scene reads the same on any screen. Y grows downward and the
 * tower grows upward, toward smaller Y.
 */
object Yard {
    const val SPAN = 16.0

    /**
     * Shortest slice of world the camera must show. On a stubby screen the
     * scale drops out of this instead of the width, so the rig, the drop and
     * the tower still fit between the header and the controls.
     */
    const val MIN_VIEW = 30.0

    const val HOUSE_SPAN = 4.6
    const val MIN_RATIO = 0.62
    const val MAX_RATIO = 1.32

    const val PLINTH_SPAN = 5.6
    const val PLINTH_RISE = 5.8
    const val PAVEMENT_Y = 8.8
    val plinthTop: Double get() = PAVEMENT_Y - PLINTH_RISE

    const val STREET_SPAN = 18.5
    const val STREET_DROP = 1.6

    /**
     * Only the rig is a pendulum: the hook rides a circular arc about a hinge
     * that sits well above the visible area, plain simple-harmonic motion that
     * is fastest through the middle and pauses at each extreme.
     */
    const val TILT_LIMIT = 0.24
    const val PIVOT_LIFT = 20.0
    const val ARM_HOOK = 11.6
    const val HOOK_SPAN = 1.3

    /** Where down the hook sprite the rigging is shackled on. */
    const val HOOK_SHACKLE = 0.42

    /**
     * The house is not bolted to the hook — it hangs off the rigging on its own
     * shorter, stiffer pendulum and is dragged along by the hook's sideways
     * acceleration, so it lags behind the rig and keeps swaying at the extremes.
     */
    const val RIG_ROPE = 1.6
    const val LOAD_PULL = 70.0
    const val LOAD_DRAG = 1.8

    /** Seconds for the winch to pay a house out, and for the arc to cross once. */
    const val WINCH_SECONDS = 0.32
    const val SWEEP_SECONDS = 1.55
    const val SWEEP_QUICKEN = 0.035
    const val SWEEP_FLOOR = 0.70

    const val GRAVITY = 32.0
    const val LAUNCH_SPEED = 1.8
    const val AIR_DRAG = 2.4

    /** A house beds in slightly off centre so the tower looks hand built. */
    const val BED_SHIFT = 0.8
    const val BED_LEAN = 0.055

    const val LENS_LEAD = 4.0
    const val LENS_CHASE = 5.5
    const val LOST_DEPTH = 16.0

    val HOUSES = listOf(
        "trim_block_asset_01.webp",
        "trim_block_asset_02.webp",
        "trim_block_asset_03.webp",
        "trim_block_asset_04.webp",
    )
    const val PLINTH = "trim_start_block_asset.webp"
    const val HOOK = "trim_hook_asset.webp"
    const val STREET = "trim_street.webp"
}

/** What the last released house did. */
enum class Verdict { SEATED, SLIPPED }

/** A house that made it onto the tower. */
class Storey(
    val art: String,
    val cx: Double,
    val top: Double,
    val span: Double,
    val rise: Double,
    val lean: Double,
    var wobble: Double,
    /** Impact compression, 1 right after landing and decaying to zero. */
    var squash: Double,
) {
    val cy: Double get() = top + rise / 2
}

/** The house currently in the air. */
class Airborne(
    val art: String,
    var x: Double,
    var y: Double,
    var vx: Double,
    var vy: Double,
    var turn: Double,
    var spin: Double,
    val span: Double,
    val rise: Double,
    val bedY: Double,
    val bedX: Double,
    val bedLean: Double,
    val holds: Boolean,
    /** Distance to fall, captured at release, so the approach can be blended. */
    val drop: Double,
)

/** Dust puff, spark or coin fleck. */
class Mote(
    var x: Double,
    var y: Double,
    var vx: Double,
    var vy: Double,
    var life: Double,
    val born: Double,
    val span: Double,
    val tint: Long,
    val pull: Double,
    val soft: Boolean,
)

/**
 * The moving part of the game: the swinging rig, the house slung under it, the
 * one in free fall, the tower it lands on and the camera that rides up with it.
 * Rules and money live in [TowerEngine]; this class only decides where things
 * are drawn.
 */
class SiegeStage(private val rng: Random = Random.Default) {

    /** Bumped every frame so the canvas repaints. */
    var pulse by mutableIntStateOf(0)
        private set

    val storeys = ArrayList<Storey>()
    val motes = ArrayList<Mote>()

    var airborne: Airborne? = null
        private set
    var lensY: Double = Yard.plinthTop - Yard.LENS_LEAD
        private set
    var hangArt: String = Yard.HOUSES.first()
        private set
    var quake: Double = 0.0
        private set

    private var sweep = 0.0
    private var winch = 0.0
    private var loadAngle = 0.0
    private var loadRate = 0.0
    private var verdict: Verdict? = null
    private var ratioOf: (String) -> Double = { 1.0 }

    init {
        hangArt = pickArt()
    }

    /** Real bitmap proportions arrive once the images are decoded. */
    fun measureWith(source: (String) -> Double) {
        ratioOf = source
    }

    val stackTop: Double get() = storeys.lastOrNull()?.top ?: Yard.plinthTop
    val hooked: Boolean get() = airborne == null
    val ready: Boolean get() = hooked && winch > 0.85

    private val winchEase: Double
        get() {
            val t = winch.coerceIn(0.0, 1.0)
            return t * t * (3 - 2 * t)
        }

    private val crossSeconds: Double
        get() = max(Yard.SWEEP_FLOOR, Yard.SWEEP_SECONDS - storeys.size * Yard.SWEEP_QUICKEN)

    private val sweepRate: Double get() = Math.PI / crossSeconds

    // --- the rig ------------------------------------------------------------ #

    /** Hook angle off vertical; zero hangs straight down. */
    val tilt: Double get() = sin(sweep) * Yard.TILT_LIMIT * winchEase
    private val tiltRate: Double get() = cos(sweep) * Yard.TILT_LIMIT * winchEase * sweepRate

    val pivotY: Double get() = lensY - Yard.PIVOT_LIFT
    private val hookArm: Double get() = Yard.ARM_HOOK * winchEase

    val hookX: Double get() = hookArm * sin(tilt)
    val hookY: Double get() = pivotY + hookArm * cos(tilt)

    // --- the load slung under it -------------------------------------------- #

    /** Rigging angle, tracked separately from the rig so the house sways. */
    val sway: Double get() = loadAngle

    val hangRise: Double get() = riseOf(hangArt)

    /** Hook sprite height, so the rigging hangs off its nose and not its middle. */
    val hookRise: Double get() = Yard.HOOK_SPAN * ratioOf(Yard.HOOK)
    private val shackle: Double get() = hookRise * Yard.HOOK_SHACKLE
    private val loadArm: Double get() = (shackle + Yard.RIG_ROPE + hangRise / 2) * winchEase

    val hangX: Double get() = hookX + loadArm * sin(loadAngle)
    val hangY: Double get() = hookY + loadArm * cos(loadAngle)

    fun riseOf(art: String): Double =
        Yard.HOUSE_SPAN * ratioOf(art).coerceIn(Yard.MIN_RATIO, Yard.MAX_RATIO)

    private fun pickArt(): String {
        var art = Yard.HOUSES[rng.nextInt(Yard.HOUSES.size)]
        if (art == hangArt) art = Yard.HOUSES[rng.nextInt(Yard.HOUSES.size)]
        return art
    }

    /** Clears the yard for a fresh run; the camera eases home on its own. */
    fun clear() {
        storeys.clear()
        motes.clear()
        airborne = null
        verdict = null
        winch = 0.0
        loadAngle = 0.0
        loadRate = 0.0
        hangArt = pickArt()
    }

    /**
     * Lets the hanging house go. [holds] is decided by the rules, not by aim.
     * The house leaves with the momentum it actually had — the hook's arc speed
     * plus whatever the rigging was doing — so it keeps drifting and turning on
     * the way down instead of dropping straight.
     */
    fun release(holds: Boolean) {
        if (!hooked) return
        val rise = hangRise
        val arm = loadArm
        val hookVx = hookArm * cos(tilt) * tiltRate
        val hookVy = -hookArm * sin(tilt) * tiltRate
        val vx = hookVx + arm * loadRate * cos(loadAngle)
        val vy = hookVy - arm * loadRate * sin(loadAngle)
        val away = if (loadAngle == 0.0) 1.0 else sign(loadAngle)
        val from = hangY
        val bed = stackTop - rise / 2
        airborne = Airborne(
            art = hangArt,
            x = hangX,
            y = from,
            vx = if (holds) vx * 0.75 else vx * 0.6 + away * 1.6,
            vy = Yard.LAUNCH_SPEED + max(0.0, vy),
            turn = loadAngle,
            spin = if (holds) loadRate * 0.6 else loadRate * 0.4 + away * 2.4,
            span = Yard.HOUSE_SPAN,
            rise = rise,
            bedY = bed,
            bedX = (rng.nextDouble() * 2 - 1) * Yard.BED_SHIFT,
            bedLean = (rng.nextDouble() * 2 - 1) * Yard.BED_LEAN,
            holds = holds,
            drop = max(1.0, bed - from),
        )
        loadAngle = 0.0
        loadRate = 0.0
    }

    fun takeVerdict(): Verdict? = verdict.also { verdict = null }

    fun step(rawDt: Double) {
        val dt = rawDt.coerceIn(0.0, 1.0 / 30.0)
        if (quake > 0) quake = max(0.0, quake - dt * 1.3)

        val target = if (hooked) 1.0 else 0.0
        val stride = dt / Yard.WINCH_SECONDS
        winch = if (winch < target) min(target, winch + stride) else max(target, winch - stride)

        if (hooked) {
            sweep += dt * sweepRate
            if (sweep > Math.PI * 2) sweep -= Math.PI * 2
            swayLoad(dt)
        }

        airborne?.let { fall(it, dt) }

        val aim = stackTop - Yard.LENS_LEAD
        lensY += (aim - lensY) * (1 - exp(-dt * Yard.LENS_CHASE))

        driftMotes(dt)

        storeys.lastOrNull()?.let { top ->
            if (abs(top.wobble) > 0.0005) top.wobble *= 0.0025.pow(dt) else top.wobble = 0.0
        }
        storeys.forEach { s ->
            if (s.squash > 0.0005) s.squash *= 0.0009.pow(dt) else s.squash = 0.0
        }

        pulse++
    }

    /**
     * The rigging is a driven pendulum: the hook's sideways acceleration hauls
     * the house around, gravity pulls it back and drag settles it.
     */
    private fun swayLoad(dt: Double) {
        val arm = max(0.7, loadArm)
        val hookPush = -sweepRate * sweepRate * hookX
        val turn = -(Yard.LOAD_PULL / arm) * sin(loadAngle) -
            Yard.LOAD_DRAG * loadRate -
            (hookPush / arm) * cos(loadAngle)
        loadRate += turn * dt
        loadAngle += loadRate * dt
    }

    private fun fall(house: Airborne, dt: Double) {
        house.vy += Yard.GRAVITY * dt
        house.vx -= house.vx * min(1.0, dt * Yard.AIR_DRAG)
        house.y += house.vy * dt
        house.x += house.vx * dt
        house.turn += house.spin * dt

        if (house.holds) {
            // Close to the tower the crane's guidance takes over, so the house
            // stops drifting and squares up just before it beds in.
            val approach = (1 - (house.bedY - house.y) / house.drop).coerceIn(0.0, 1.0)
            val grip = min(1.0, dt * (1.5 + 15.0 * approach * approach))
            house.x += (house.bedX - house.x) * grip
            house.turn += (house.bedLean - house.turn) * grip
            house.spin -= house.spin * grip
            if (house.y >= house.bedY) seat(house)
        } else {
            house.spin += house.spin * dt * 0.6
            if (verdict == null && house.y > house.bedY + house.rise * 0.9) {
                verdict = Verdict.SLIPPED
                quake = 0.6
            }
            if (house.y > lensY + Yard.LOST_DEPTH) airborne = null
        }
    }

    private fun seat(house: Airborne) {
        val placed = Storey(
            art = house.art,
            cx = house.bedX,
            top = stackTop - house.rise,
            span = house.span,
            rise = house.rise,
            lean = house.bedLean,
            wobble = house.turn - house.bedLean,
            squash = 1.0,
        )
        storeys.add(placed)
        airborne = null
        quake = 0.32
        puffDust(placed)
        flashSparks(placed)
        verdict = Verdict.SEATED
        hangArt = pickArt()
        winch = 0.0
        loadAngle = 0.0
        loadRate = 0.0
    }

    private fun driftMotes(dt: Double) {
        val it = motes.iterator()
        while (it.hasNext()) {
            val m = it.next()
            m.vy += m.pull * dt
            m.x += m.vx * dt
            m.y += m.vy * dt
            m.life -= dt
            if (m.life <= 0) it.remove()
        }
    }

    private fun puffDust(storey: Storey) {
        val edge = storey.top + storey.rise
        val half = storey.span / 2
        repeat(22) { i ->
            val side = if (i % 2 == 0) -1 else 1
            val spread = rng.nextDouble()
            val life = 0.5 + rng.nextDouble() * 0.5
            motes.add(
                Mote(
                    x = storey.cx + side * half * (0.25 + spread * 0.9),
                    y = edge - rng.nextDouble() * 0.35,
                    vx = side * (1.3 + rng.nextDouble() * 3.2),
                    vy = -0.3 - rng.nextDouble() * 1.0,
                    life = life,
                    born = life,
                    span = 0.3 + rng.nextDouble() * 0.5,
                    tint = 0xFFF3EEE4,
                    pull = 2.5,
                    soft = true,
                ),
            )
        }
    }

    private fun flashSparks(storey: Storey) {
        repeat(14) { i ->
            val a = (i / 14.0) * Math.PI * 2
            val life = 0.35 + rng.nextDouble() * 0.3
            motes.add(
                Mote(
                    x = storey.cx,
                    y = storey.top,
                    vx = cos(a) * (2.0 + rng.nextDouble()),
                    vy = sin(a) * (2.0 + rng.nextDouble()),
                    life = life,
                    born = life,
                    span = 0.1 + rng.nextDouble() * 0.12,
                    tint = 0xFFFFD764,
                    pull = 1.4,
                    soft = false,
                ),
            )
        }
    }

    fun showerCoins() {
        val top = stackTop
        repeat(22) {
            val a = -Math.PI / 2 + (rng.nextDouble() - 0.5) * 1.6
            val speed = 3.0 + rng.nextDouble() * 4.0
            val life = 0.7 + rng.nextDouble() * 0.5
            motes.add(
                Mote(
                    x = (rng.nextDouble() - 0.5) * 2,
                    y = top,
                    vx = cos(a) * speed,
                    vy = sin(a) * speed,
                    life = life,
                    born = life,
                    span = 0.16 + rng.nextDouble() * 0.12,
                    tint = 0xFFFFC24B,
                    pull = 9.0,
                    soft = false,
                ),
            )
        }
        quake = 0.3
    }
}
