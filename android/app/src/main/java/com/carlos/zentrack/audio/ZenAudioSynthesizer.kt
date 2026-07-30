package com.carlos.zentrack.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object ZenAudioSynthesizer {
    private const val SAMPLE_RATE = 44100
    private var isEnabled = true
    private var volume = 0.8f
    private var soundProfile = "cherry-blue"

    private val executor = Executors.newFixedThreadPool(4)

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
    }

    fun setProfile(profile: String) {
        soundProfile = profile
    }

    fun playSwitchSound(isPress: Boolean = true, keyCode: String = "") {
        if (!isEnabled || volume <= 0f) return

        executor.execute {
            try {
                val pcmData = generatePcmData(soundProfile, isPress, keyCode, volume)
                playPcm(pcmData)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun generatePcmData(profile: String, isPress: Boolean, keyCode: String, vol: Float): ShortArray {
        val durationMs = when (keyCode) {
            "space" -> 40
            "Return", "BackSpace", "Shift_L", "Shift_R" -> 35
            else -> 25
        }

        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        val pitchScale = when (keyCode) {
            "space" -> 0.70f
            "Return", "BackSpace", "Shift_L", "Shift_R" -> 0.85f
            else -> 1.0f
        }

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var sample = 0.0

            when (profile) {
                "cherry-blue" -> {
                    // High frequency click + low thock
                    val clickFreq = (if (isPress) 1100.0 else 1400.0) * pitchScale
                    val clickEnv = exp(-t * 180.0)
                    val clickSignal = sin(2.0 * PI * clickFreq * t) * clickEnv * 0.45

                    val thockFreq = (if (isPress) 120.0 else 90.0) * pitchScale
                    val thockEnv = exp(-t * 90.0)
                    val thockSignal = sin(2.0 * PI * thockFreq * t) * thockEnv * 0.55

                    sample = (clickSignal + thockSignal)
                }

                "cherry-red" -> {
                    // Deep linear thock (no click)
                    val thockFreq = (if (isPress) 100.0 else 130.0) * pitchScale
                    val thockEnv = exp(-t * 110.0)
                    sample = sin(2.0 * PI * thockFreq * t) * thockEnv * 0.85
                }

                "chocolate" -> {
                    // Dry wooden snap
                    val snapFreq = (if (isPress) 1250.0 else 1400.0) * pitchScale
                    val snapEnv = exp(-t * 220.0)
                    val snapSignal = sin(2.0 * PI * snapFreq * t) * snapEnv * 0.6

                    val woodFreq = 140.0 * pitchScale
                    val woodEnv = exp(-t * 130.0)
                    val woodSignal = sin(2.0 * PI * woodFreq * t) * woodEnv * 0.4

                    sample = (snapSignal + woodSignal)
                }

                "box-navy" -> {
                    // Heavy click bar + thick thock
                    val clickFreq = (if (isPress) 1600.0 else 1500.0) * pitchScale
                    val clickEnv = exp(-t * 240.0)
                    val clickSignal = sin(2.0 * PI * clickFreq * t) * clickEnv * 0.65

                    val thockFreq = 130.0 * pitchScale
                    val thockEnv = exp(-t * 80.0)
                    val thockSignal = sin(2.0 * PI * thockFreq * t) * thockEnv * 0.5

                    sample = (clickSignal + thockSignal)
                }

                "buckling-spring" -> {
                    // IBM Model M click + spring ping resonance
                    val clickFreq = 1000.0 * pitchScale
                    val clickEnv = exp(-t * 160.0)
                    val clickSignal = sin(2.0 * PI * clickFreq * t) * clickEnv * 0.5

                    val pingFreq = 2600.0 * pitchScale
                    val pingEnv = exp(-t * 40.0)
                    val pingSignal = sin(2.0 * PI * pingFreq * t) * pingEnv * 0.15

                    sample = (clickSignal + pingSignal)
                }

                "topre" -> {
                    // Topre Capacitive Dome "Plop"
                    val plopFreq = (if (isPress) 130.0 else 170.0) * pitchScale
                    val plopEnv = exp(-t * 100.0)
                    sample = sin(2.0 * PI * plopFreq * t) * plopEnv * 0.8
                }

                "bubble-wrap" -> {
                    // Bubble Wrap Pop
                    val popFreq = (400.0 + t * 40000.0) * pitchScale
                    val popEnv = exp(-t * 250.0)
                    sample = sin(2.0 * PI * popFreq * t) * popEnv * 0.75
                }

                else -> { // Membrane
                    val domeFreq = 160.0 * pitchScale
                    val domeEnv = exp(-t * 140.0)
                    sample = sin(2.0 * PI * domeFreq * t) * domeEnv * 0.5
                }
            }

            val pcmSample = (sample * vol * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = pcmSample.toShort()
        }

        return buffer
    }

    private fun playPcm(buffer: ShortArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()

        // Clean up track after duration
        executor.execute {
            Thread.sleep(60)
            try {
                track.stop()
                track.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
