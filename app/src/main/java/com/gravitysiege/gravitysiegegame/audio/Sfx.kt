package com.gravitysiege.gravitysiegegame.audio

import android.content.Context
import android.media.MediaPlayer

class Sfx(private val context: Context) {
    var enabled: Boolean = true
    private var player: MediaPlayer? = null

    fun click() = play("button_click_asset.mp3")
    fun open() = play("menu_open_asset.mp3")
    fun close() = play("menu_close_asset.mp3")
    fun back() = play("back_button_asset.mp3")
    fun success() = play("success_asset.mp3")
    fun error() = play("error_asset.mp3")
    fun chime() = play("notification_asset.mp3")
    fun transition() = play("screen_transition_asset.mp3")

    fun play(asset: String) {
        if (!enabled) return
        try {
            player?.release()
            val afd = context.assets.openFd(asset)
            player = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnCompletionListener { mp ->
                    mp.reset()
                    mp.release()
                    if (player === mp) player = null
                }
                prepare()
                start()
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        player?.release()
        player = null
    }
}
