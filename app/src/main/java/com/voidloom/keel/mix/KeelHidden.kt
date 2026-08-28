package com.voidloom.keel.mix

/** Packed credentials. Plaintext must never appear in this file. */
internal object KeelHidden {

    val CONFIG_ENDPOINT_BYTES = intArrayOf(
        49, 22, 112, 151, 3, 12, 247, 206, 219, 190, 170, 110,
        149, 46, 126, 70, 21, 143, 153, 105, 149, 143, 146, 33,
        30, 88, 181, 0, 169, 141, 153, 204, 211, 72, 97, 163,
    )

    val ATTRIBUTION_KEY_BYTES = intArrayOf(
        31, 85, 67, 180, 62, 94, 162, 176, 229, 185, 129, 115,
        136, 63, 69, 102, 38, 187, 182, 127, 201, 153,
    )

    val MESSAGING_PROJECT_BYTES = intArrayOf(
        104, 85, 61, 211, 70, 0, 236, 213, 139, 245, 255, 43,
    )

    val CHROME_VERSION_BYTES = intArrayOf(
        104, 87, 53, 201, 64, 24, 239, 214, 136, 254, 229, 46,
        196,
    )

    val WEBKIT_VERSION_BYTES = intArrayOf(
        108, 81, 51, 201, 67, 0,
    )

    fun configEndpoint(): String = KeelMix.unveil(CONFIG_ENDPOINT_BYTES)
    fun attributionKey(): String = KeelMix.unveil(ATTRIBUTION_KEY_BYTES)
    fun messagingProject(): String = KeelMix.unveil(MESSAGING_PROJECT_BYTES)
    fun chromeVersion(): String = KeelMix.unveil(CHROME_VERSION_BYTES)
    fun webkitVersion(): String = KeelMix.unveil(WEBKIT_VERSION_BYTES)
}
