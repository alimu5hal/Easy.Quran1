package com.easyquran

import android.media.AudioAttributes
import android.media.MediaPlayer

/** Streams one ayah at a time (Mishary Alafasy, everyayah.com). */
object Audio {
    private var mp: MediaPlayer? = null
    var playing = -1
        private set

    fun play(a: Ayah, idx: Int, onDone: () -> Unit, onError: () -> Unit) {
        stop()
        playing = idx
        val url = "https://everyayah.com/data/Alafasy_128kbps/%03d%03d.mp3".format(a.s, a.a)
        try {
            mp = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setUsage(AudioAttributes.USAGE_MEDIA).build())
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnCompletionListener { stop(); onDone() }
                setOnErrorListener { _, _, _ -> stop(); onError(); true }
                prepareAsync()
            }
        } catch (e: Exception) { stop(); onError() }
    }

    fun stop() { try { mp?.release() } catch (e: Exception) { }; mp = null; playing = -1 }
}
