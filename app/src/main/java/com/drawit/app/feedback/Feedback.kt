package com.drawit.app.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.drawit.app.R
import kotlin.math.pow

/**
 * Every sound and buzz in the app. Sounds are short synthesised clips (see
 * tools/make_sounds.py) played through one SoundPool; haptics use the phone's
 * tuned primitives where it has them, so a "thud" feels like a thud.
 */
class Feedback(context: Context) {

    enum class Sound(val res: Int, val volume: Float) {
        Pop(R.raw.pop, 0.55f),
        Tick(R.raw.tick, 0.45f),
        Ding(R.raw.ding, 0.9f),
        Ignite(R.raw.ignite, 0.95f),
        Streak(R.raw.streak, 1f),
        DayFill(R.raw.dayfill, 0.7f),
        Coin(R.raw.coin, 0.75f),
        Fanfare(R.raw.fanfare, 1f),
        Rattle(R.raw.rattle, 0.85f),
        EvolveRise(R.raw.evolve_rise, 0.9f),
        EvolveBoom(R.raw.evolve_boom, 1f),
        Purchase(R.raw.purchase, 0.9f),
        Equip(R.raw.equip, 0.75f),
        Freeze(R.raw.freeze, 0.9f),
        Lost(R.raw.lost, 0.8f),
        Error(R.raw.error, 0.7f),
        Whoosh(R.raw.whoosh, 0.6f),
        Unlock(R.raw.unlock, 0.9f)
    }

    private val app = context.applicationContext

    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = HashMap<Sound, Int>()
    private val ready = HashSet<Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, id, status -> if (status == 0) synchronized(ready) { ready += id } }
        Sound.entries.forEach { ids[it] = soundPool.load(app, it.res, 1) }
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

    // -- sound --------------------------------------------------------------------

    /**
     * Plays [sound]. [semitones] shifts the pitch (SoundPool allows about an
     * octave each way), which is how a week of days climbs a scale.
     */
    fun play(sound: Sound, semitones: Float = 0f, gain: Float = 1f) {
        if (!soundEnabled) return
        val id = ids[sound] ?: return
        if (synchronized(ready) { id !in ready }) return
        val rate = 2f.pow(semitones / 12f).coerceIn(0.5f, 2f)
        val v = (sound.volume * gain).coerceIn(0f, 1f)
        runCatching { soundPool.play(id, v, v, 1, 0, rate) }
    }

    // -- haptics ------------------------------------------------------------------

    enum class Buzz { Tap, Tick, Pop, Thud, Rise, Success, Big, Soft, Error }

    fun buzz(kind: Buzz) {
        if (!hapticsEnabled) return
        val device = vibrator ?: return
        if (!device.hasVibrator()) return
        runCatching { device.vibrate(effectFor(kind, device)) }
    }

    private fun effectFor(kind: Buzz, device: Vibrator): VibrationEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            composed(kind, device)?.let { return it }
        }
        return when (kind) {
            Buzz.Tap -> wave(longArrayOf(0, 12), intArrayOf(0, 90))
            Buzz.Tick -> wave(longArrayOf(0, 8), intArrayOf(0, 70))
            Buzz.Pop -> wave(longArrayOf(0, 18), intArrayOf(0, 140))
            Buzz.Thud -> wave(longArrayOf(0, 45), intArrayOf(0, 255))
            Buzz.Rise -> wave(longArrayOf(0, 40, 40, 40, 40, 60), intArrayOf(0, 60, 0, 120, 0, 200))
            Buzz.Success -> wave(longArrayOf(0, 30, 50, 60), intArrayOf(0, 150, 0, 255))
            Buzz.Big -> wave(longArrayOf(0, 30, 30, 30, 30, 120), intArrayOf(0, 120, 0, 180, 0, 255))
            Buzz.Soft -> wave(longArrayOf(0, 60, 60, 40), intArrayOf(0, 90, 0, 60))
            Buzz.Error -> wave(longArrayOf(0, 25, 60, 25), intArrayOf(0, 160, 0, 160))
        }
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.R)
    private fun composed(kind: Buzz, device: Vibrator): VibrationEffect? {
        val c = VibrationEffect.Composition.PRIMITIVE_CLICK
        val tick = VibrationEffect.Composition.PRIMITIVE_TICK
        val rise = VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
        val slowRise = VibrationEffect.Composition.PRIMITIVE_SLOW_RISE
        val thud = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) VibrationEffect.Composition.PRIMITIVE_THUD else c
        val needed = when (kind) {
            Buzz.Tap, Buzz.Pop, Buzz.Error -> intArrayOf(c)
            Buzz.Tick -> intArrayOf(tick)
            Buzz.Thud -> intArrayOf(thud)
            Buzz.Rise -> intArrayOf(slowRise, c)
            Buzz.Success -> intArrayOf(c, rise)
            Buzz.Big -> intArrayOf(rise, thud, c)
            Buzz.Soft -> intArrayOf(tick)
        }
        if (!device.areAllPrimitivesSupported(*needed)) return null
        val comp = VibrationEffect.startComposition()
        when (kind) {
            Buzz.Tap -> comp.addPrimitive(c, 0.5f)
            Buzz.Tick -> comp.addPrimitive(tick, 0.6f)
            Buzz.Pop -> comp.addPrimitive(c, 0.85f)
            Buzz.Thud -> comp.addPrimitive(thud, 1f)
            Buzz.Rise -> comp.addPrimitive(slowRise, 0.7f).addPrimitive(c, 1f, 20)
            Buzz.Success -> comp.addPrimitive(c, 0.7f).addPrimitive(rise, 1f, 60)
            Buzz.Big -> comp.addPrimitive(rise, 0.9f).addPrimitive(thud, 1f, 30).addPrimitive(c, 1f, 120)
            Buzz.Soft -> comp.addPrimitive(tick, 0.4f).addPrimitive(tick, 0.25f, 90)
            Buzz.Error -> comp.addPrimitive(c, 0.6f).addPrimitive(c, 0.6f, 70)
        }
        return comp.compose()
    }

    private fun wave(timings: LongArray, amplitudes: IntArray): VibrationEffect =
        VibrationEffect.createWaveform(timings, amplitudes, -1)

    // -- the named moments ----------------------------------------------------------

    /** Every chunky button press: haptic only, sound would get tiring. */
    fun tap() = buzz(Buzz.Tap)

    fun tick() = buzz(Buzz.Tick)

    fun select() {
        play(Sound.Pop)
        buzz(Buzz.Tick)
    }

    fun error() {
        play(Sound.Error)
        buzz(Buzz.Error)
    }

    fun release() {
        runCatching { soundPool.release() }
    }
}
