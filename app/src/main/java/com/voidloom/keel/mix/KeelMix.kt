package com.voidloom.keel.mix

/**
 * Per-binary string hider. Seed and stream stay unique to Gravity Siege.
 */
internal object KeelMix {

    private const val SEED_PHRASE = "Gs9#vL2r_qW8n"
    private const val STREAM_LEN = 23

    private val stream: IntArray by lazy(::buildStream)

    private fun buildStream(): IntArray {
        var h = 6143
        for (c in SEED_PHRASE) {
            h = (h shl 5) + h + c.code
        }
        var state = if (h == 0) 0xA5A5A5A5.toInt() else h
        val out = IntArray(STREAM_LEN)
        for (i in 0 until STREAM_LEN) {
            state = state xor (state shl 7)
            state = state xor (state ushr 9)
            state = state xor (state shl 8)
            out[i] = (state ushr 11) and 0xFF
        }
        return out
    }

    fun unveil(packed: IntArray): String {
        if (packed.isEmpty()) return ""
        val chars = CharArray(packed.size)
        for (i in packed.indices) {
            val byte = (packed[i] xor stream[i % STREAM_LEN] xor ((i * 23 + 11) and 0xFF)) and 0xFF
            chars[i] = byte.toChar()
        }
        return String(chars)
    }
}
