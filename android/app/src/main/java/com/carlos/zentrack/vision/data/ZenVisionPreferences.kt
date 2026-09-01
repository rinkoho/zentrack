package com.carlos.zentrack.vision.data

import android.content.Context
import android.content.SharedPreferences

object ZenVisionPreferences {
    private const val PREF_NAME = "zentrack_vision_prefs"

    private const val KEY_IS_PIP_MODE = "is_pip_mode"
    private const val KEY_LENS_FACING = "lens_facing" // 1 = FRONT, 0 = BACK
    private const val KEY_SHOW_SKELETON = "show_skeleton"
    private const val KEY_SHOW_TELEMETRY = "show_telemetry"
    private const val KEY_SHOW_FINGERTIP_LABELS = "show_fingertip_labels"
    private const val KEY_DELEGATE = "delegate" // 0 = GPU, 1 = CPU
    private const val KEY_MIN_DETECTION_CONFIDENCE = "min_detection_confidence"
    private const val KEY_MIN_TRACKING_CONFIDENCE = "min_tracking_confidence"
    private const val KEY_MAX_NUM_HANDS = "max_num_hands"
    private const val KEY_MIRROR_FRONT = "mirror_front"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    var isPipMode: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean(KEY_IS_PIP_MODE, false) else false
        set(value) { if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_IS_PIP_MODE, value).apply() }

    var lensFacing: Int
        get() = if (::prefs.isInitialized) prefs.getInt(KEY_LENS_FACING, 1) else 1 // 1 = CameraSelector.LENS_FACING_FRONT
        set(value) { if (::prefs.isInitialized) prefs.edit().putInt(KEY_LENS_FACING, value).apply() }

    var showSkeleton: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean(KEY_SHOW_SKELETON, true) else true
        set(value) { if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_SKELETON, value).apply() }

    var showTelemetry: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean(KEY_SHOW_TELEMETRY, true) else true
        set(value) { if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_TELEMETRY, value).apply() }

    var showFingertipLabels: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean(KEY_SHOW_FINGERTIP_LABELS, true) else true
        set(value) { if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_FINGERTIP_LABELS, value).apply() }

    var delegate: Int
        get() = if (::prefs.isInitialized) prefs.getInt(KEY_DELEGATE, 0) else 0 // 0 = GPU
        set(value) { if (::prefs.isInitialized) prefs.edit().putInt(KEY_DELEGATE, value).apply() }

    var minDetectionConfidence: Float
        get() = if (::prefs.isInitialized) prefs.getFloat(KEY_MIN_DETECTION_CONFIDENCE, 0.5f) else 0.5f
        set(value) { if (::prefs.isInitialized) prefs.edit().putFloat(KEY_MIN_DETECTION_CONFIDENCE, value).apply() }

    var minTrackingConfidence: Float
        get() = if (::prefs.isInitialized) prefs.getFloat(KEY_MIN_TRACKING_CONFIDENCE, 0.5f) else 0.5f
        set(value) { if (::prefs.isInitialized) prefs.edit().putFloat(KEY_MIN_TRACKING_CONFIDENCE, value).apply() }

    var maxNumHands: Int
        get() = if (::prefs.isInitialized) prefs.getInt(KEY_MAX_NUM_HANDS, 1) else 1
        set(value) { if (::prefs.isInitialized) prefs.edit().putInt(KEY_MAX_NUM_HANDS, value).apply() }

    var mirrorFront: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean(KEY_MIRROR_FRONT, true) else true
        set(value) { if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_MIRROR_FRONT, value).apply() }

    // Render Mode: "hd", "performance", "skeleton"
    var renderMode: String
        get() = if (::prefs.isInitialized) prefs.getString("render_mode", "performance") ?: "performance" else "performance"
        set(value) { if (::prefs.isInitialized) prefs.edit().putString("render_mode", value).apply() }

    var isAirMouseEnabled: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean("air_mouse_enabled", true) else true
        set(value) { if (::prefs.isInitialized) prefs.edit().putBoolean("air_mouse_enabled", value).apply() }

    var visionSensitivity: Float
        get() = if (::prefs.isInitialized) prefs.getFloat("vision_sensitivity", 1.0f) else 1.0f
        set(value) { if (::prefs.isInitialized) prefs.edit().putFloat("vision_sensitivity", value).apply() }
}
