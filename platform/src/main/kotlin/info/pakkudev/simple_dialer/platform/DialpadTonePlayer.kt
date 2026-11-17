package info.pakkudev.simple_dialer.platform

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.Settings
import android.util.Log

/** Local number-entry feedback. Telecom owns in-call DTMF and disconnect tones. */
class DialpadTonePlayer(context: Context) {
    private val context = context.applicationContext
    private val audio = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var generator: ToneGenerator? = null

    fun play(digit: Char): Boolean {
        val tone = when (digit) {
            in '0'..'9' -> ToneGenerator.TONE_DTMF_0 + (digit - '0')
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> return false
        }
        return try {
            if (Settings.System.getInt(context.contentResolver, Settings.System.DTMF_TONE_WHEN_DIALING, 1) != 1 ||
                audio.ringerMode != AudioManager.RINGER_MODE_NORMAL || audio.getStreamVolume(AudioManager.STREAM_DTMF) == 0) {
                generator?.stopTone()
                false
            } else {
                val player = generator ?: ToneGenerator(AudioManager.STREAM_DTMF, 80).also { generator = it }
                player.startTone(tone, 150)
            }
        } catch (error: RuntimeException) {
            Log.w("DialpadTonePlayer", "Local dialpad audio unavailable", error)
            release()
            false
        }
    }

    fun release() {
        val player = generator
        generator = null
        runCatching { player?.release() }
    }
}
