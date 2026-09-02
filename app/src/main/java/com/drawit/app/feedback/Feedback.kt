package com.drawit.app.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.drawit.app.R

/**
 * The buzz and the chime. Both are opt-out, and both are cheap enough to build
 * once and keep for the life of the process.
 */
class Feedback(context: Context) {

    private val app = context.applicationContext

    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var loaded = false
    private val streakUp: Int
    private val milestone: Int
    private val freeze: Int

    init {
        soundPool.setOnLoadCompleteListener { _, _, status -> if (status == 0) loaded = true }
        streakUp = soundPool.load(app, R.raw.streak_up, 1)
        milestone = soundPool.load(app, R.raw.streak_milestone, 1)
        freeze = soundPool.load(app, R.raw.streak_freeze, 1)
    }

    private val vibrator: Vibrator? by lazy {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        }.getOrNull()
    }

    // -- the moments ------------------------------------------------------

    /** One more day on the streak. */
    fun streakExtended() {
        play(streakUp)
        // Two taps that grow into one confident pulse.
        vibrate(
            timings = longArrayOf(0, 34, 46, 34, 46, 110),
            amplitudes = intArrayOf(0, 110, 0, 165, 0, 255)
        )
    }

    /** A milestone: longer, with a roll before the final hit. */
    fun milestoneReached() {
        play(milestone)
        vibrate(
            timings = longArrayOf(0, 26, 34, 26, 34, 26, 34, 40, 60, 190),
            amplitudes = intArrayOf(0, 90, 0, 130, 0, 170, 0, 210, 0, 255)
        )
    }

    /** A freeze was spent to keep the streak alive. Softer, a touch apologetic. */
    fun freezeSpent() {
        play(freeze)
        vibrate(
            timings = longArrayOf(0, 90, 70, 60),
            amplitudes = intArrayOf(0, 120, 0, 70)
        )
    }

    /** The streak ended. A single low thud, no sound. */
    fun streakLost() {
        vibrate(timings = longArrayOf(0, 140), amplitudes = intArrayOf(0, 90))
    }

    /** Small confirmation for taps that change something. */
    fun tick() {
        vibrate(timings = longArrayOf(0, 18), amplitudes = intArrayOf(0, 90))
    }

    fun release() {
        runCatching { soundPool.release() }
    }

    // -- plumbing ---------------------------------------------------------

    private fun play(id: Int) {
        if (!soundEnabled || !loaded) return
        runCatching { soundPool.play(id, 0.85f, 0.85f, 1, 0, 1f) }
    }

    private fun vibrate(timings: LongArray, amplitudes: IntArray) {
        if (!hapticsEnabled) return
        val device = vibrator ?: return
        if (!device.hasVibrator()) return
        runCatching {
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.vibrate(CombinedVibration.createParallel(effect))
            } else {
                device.vibrate(effect)
            }
        }
    }
}
