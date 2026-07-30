package com.carlos.zentrack.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

object ZenSoundEngine {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<String, Int>()

    private var isEnabled = true
    private var volume = 0.8f
    private var currentProfile = "cherry-blue"

    fun init(context: Context) {
        if (soundPool != null) return

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(16)
            .setAudioAttributes(attributes)
            .build()

        val profiles = listOf(
            "cherry-blue", "cherry-red", "chocolate", "box-navy",
            "buckling-spring", "topre", "membrane", "bubble-wrap"
        )

        for (profile in profiles) {
            for (isPress in listOf(true, false)) {
                val filename = "${profile}_${if (isPress) "press" else "release"}.wav"
                val key = "${profile}_${if (isPress) "press" else "release"}"
                try {
                    val afd = context.assets.openFd("sounds/$filename")
                    val soundId = soundPool?.load(afd, 1) ?: 0
                    soundMap[key] = soundId
                    afd.close()
                } catch (e: Exception) {
                    Log.e("ZenSoundEngine", "Error loading asset sound: $filename", e)
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
    }

    fun setProfile(profile: String) {
        currentProfile = profile
    }

    fun playSwitchSound(isPress: Boolean = true, keyCode: String = "") {
        if (!isEnabled || volume <= 0f) return

        val pool = soundPool ?: return
        val key = "${currentProfile}_${if (isPress) "press" else "release"}"
        val soundId = soundMap[key] ?: return

        // Acoustic Pitch Scaling matching web (Spacebar = lower pitch, Modifiers = mid pitch)
        val pitch = when (keyCode) {
            "space" -> 0.78f
            "Return", "BackSpace", "Shift_L", "Shift_R", "Caps_Lock", "Tab" -> 0.88f
            else -> 1.0f
        }

        pool.play(soundId, volume, volume, 1, 0, pitch)
    }
}
