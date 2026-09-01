package com.carlos.zentrack.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.carlos.zentrack.preferences.ZenPreferences

enum class HapticFeedbackType {
    KEYBOARD_TAP,       // Gboard-like crisp physical key strike (Zero buzz, instant braking)
    KEYBOARD_HEAVY,     // Spacebar, Enter, Shift (Authoritative mechanical thud)
    TRACKPAD_TICK,      // Scroll wheel micro-step / gesture notch
    TRACKPAD_CLICK,     // Trackpad tap click / physical mouse button click
    TRACKPAD_HEAVY      // Drag lock / 3-finger desktop switch
}

/**
 * High-Performance Native LRA / X-Axis Linear Actuator Haptics Engine (Xiaomi RichTap & Android 11+ Composition)
 */
object ZenHapticsEngine {
    private var vibrator: Vibrator? = null
    private var decorView: View? = null

    fun init(context: Context, view: View? = null) {
        decorView = view
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrateKeyboard(type: HapticFeedbackType = HapticFeedbackType.KEYBOARD_TAP) {
        val intensity = ZenPreferences.hapticKeyboardIntensity
        if (intensity <= 0.01f) return
        performHaptic(type, intensity)
    }

    fun vibrateTrackpad(type: HapticFeedbackType = HapticFeedbackType.TRACKPAD_CLICK) {
        val intensity = ZenPreferences.hapticTrackpadIntensity
        if (intensity <= 0.01f) return
        performHaptic(type, intensity)
    }

    fun vibrateRaw(durationMs: Long, isKeyboard: Boolean = false) {
        val intensity = if (isKeyboard) ZenPreferences.hapticKeyboardIntensity else ZenPreferences.hapticTrackpadIntensity
        if (intensity <= 0.01f) return

        // Smart map: Map duration timestamps to exact RichTap / LRA primitives
        val type = when {
            isKeyboard && durationMs >= 25L -> HapticFeedbackType.KEYBOARD_HEAVY
            isKeyboard -> HapticFeedbackType.KEYBOARD_TAP
            durationMs <= 10L -> HapticFeedbackType.TRACKPAD_TICK
            durationMs >= 30L -> HapticFeedbackType.TRACKPAD_HEAVY
            else -> HapticFeedbackType.TRACKPAD_CLICK
        }
        performHaptic(type, intensity)
    }

    private fun performHaptic(type: HapticFeedbackType, intensity: Float) {
        try {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return

            val scale = intensity.coerceIn(0.05f, 1.0f)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // 1. Android 11+ Composition Primitives (Xiaomi RichTap & LRA Motor native wave profiles)
                val composition = VibrationEffect.startComposition()
                when (type) {
                    HapticFeedbackType.KEYBOARD_TAP -> {
                        if (v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                            composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, scale)
                            v.vibrate(composition.compose())
                            return
                        }
                    }
                    HapticFeedbackType.KEYBOARD_HEAVY -> {
                        val prim = if (v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD)) {
                            VibrationEffect.Composition.PRIMITIVE_THUD
                        } else if (v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                            VibrationEffect.Composition.PRIMITIVE_CLICK
                        } else null

                        if (prim != null) {
                            composition.addPrimitive(prim, scale)
                            v.vibrate(composition.compose())
                            return
                        }
                    }
                    HapticFeedbackType.TRACKPAD_TICK -> {
                        val prim = if (v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)) {
                            VibrationEffect.Composition.PRIMITIVE_TICK
                        } else if (v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)) {
                            VibrationEffect.Composition.PRIMITIVE_LOW_TICK
                        } else null

                        if (prim != null) {
                            composition.addPrimitive(prim, (scale * 0.85f).coerceIn(0.05f, 1.0f))
                            v.vibrate(composition.compose())
                            return
                        }
                    }
                    HapticFeedbackType.TRACKPAD_CLICK -> {
                        if (v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                            composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, scale)
                            v.vibrate(composition.compose())
                            return
                        }
                    }
                    HapticFeedbackType.TRACKPAD_HEAVY -> {
                        val prim = if (v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD)) {
                            VibrationEffect.Composition.PRIMITIVE_THUD
                        } else if (v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                            VibrationEffect.Composition.PRIMITIVE_CLICK
                        } else null

                        if (prim != null) {
                            composition.addPrimitive(prim, scale)
                            v.vibrate(composition.compose())
                            return
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 2. Android 10 Predefined Effects (Hardware Braking Profiles)
                val effectId = when (type) {
                    HapticFeedbackType.KEYBOARD_TAP -> VibrationEffect.EFFECT_CLICK
                    HapticFeedbackType.KEYBOARD_HEAVY -> VibrationEffect.EFFECT_HEAVY_CLICK
                    HapticFeedbackType.TRACKPAD_TICK -> VibrationEffect.EFFECT_TICK
                    HapticFeedbackType.TRACKPAD_CLICK -> VibrationEffect.EFFECT_CLICK
                    HapticFeedbackType.TRACKPAD_HEAVY -> VibrationEffect.EFFECT_HEAVY_CLICK
                }
                v.vibrate(VibrationEffect.createPredefined(effectId))
                return
            }

            // 3. Fallback to HyperOS / View System Haptic Tap
            val viewFeedbackConstant = when (type) {
                HapticFeedbackType.KEYBOARD_TAP -> HapticFeedbackConstants.KEYBOARD_TAP
                HapticFeedbackType.KEYBOARD_HEAVY -> HapticFeedbackConstants.LONG_PRESS
                HapticFeedbackType.TRACKPAD_TICK -> HapticFeedbackConstants.CLOCK_TICK
                HapticFeedbackType.TRACKPAD_CLICK -> HapticFeedbackConstants.VIRTUAL_KEY
                HapticFeedbackType.TRACKPAD_HEAVY -> HapticFeedbackConstants.CONFIRM
            }
            decorView?.performHapticFeedback(viewFeedbackConstant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}
