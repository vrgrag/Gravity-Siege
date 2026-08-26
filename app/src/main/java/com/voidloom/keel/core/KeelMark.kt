package com.voidloom.keel.core

import com.voidloom.keel.mix.KeelHidden

internal object KeelMark {

    const val PACKAGE_ID: String = "com.voidloom.gravitysiege"
    const val DISPLAY_NAME: String = "Gravity Siege"

    val configEndpoint: String get() = KeelHidden.configEndpoint()
    val attributionKey: String get() = KeelHidden.attributionKey()
    val messagingProject: String get() = KeelHidden.messagingProject()

    const val INVITE_COOLDOWN_SECONDS: Long = 3L * 24L * 60L * 60L

    const val REACH_PROBE_TIMEOUT_MS: Int = 2_310
    const val OFFLINE_DEBOUNCE_MS: Long = 840L
    const val CONNECT_GRACE_MS: Long = 2_540L

    const val ATTRIBUTION_TIMEOUT_MS: Long = 30_100L
    const val ATTRIBUTION_TIMEOUT_MS_RESUME: Long = 10_150L
    const val ATTRIBUTION_TIMEOUT_MS_RETRACE: Long = 8_150L
    const val DEEP_LINK_TIMEOUT_MS: Long = 5_120L
    const val GATE_TIMEOUT_MS: Long = 15_180L
    const val TOKEN_WAIT_MS: Long = 7_620L
    const val ORGANIC_GCD_PAUSE_MS: Long = 5_080L
    const val GCD_TIMEOUT_MS: Long = 8_150L

    const val HEARTBEAT_MS: Long = 6_180L
    const val REDIRECT_RETRY_MAX: Int = 6
    const val RENDERER_RECOVERY_MAX: Int = 2
    const val SAFE_AREA_DELAY_MS: Long = 790L
    const val BAR_FOLD_MS: Long = 1_500L
}
