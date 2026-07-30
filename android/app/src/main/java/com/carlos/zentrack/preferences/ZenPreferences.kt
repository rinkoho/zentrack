package com.carlos.zentrack.preferences

import android.content.Context
import android.content.SharedPreferences

object ZenPreferences {
    private const val PREF_NAME = "zentrack_prefs"

    private const val KEY_SENSITIVITY = "sensitivity"
    private const val KEY_NATURAL_SCROLL = "natural_scroll"
    private const val KEY_KEY_SOUND_ENABLED = "key_sound_enabled"
    private const val KEY_SOUND_PROFILE = "sound_profile"
    private const val KEY_SOUND_VOLUME = "sound_volume"
    private const val KEY_THEME_ANIM_SPEED = "theme_anim_speed"
    private const val KEY_SYNC_THEME = "sync_theme"
    private const val KEY_ACTIVE_THEME = "active_theme"

    private const val KEY_SCROLL_SENSITIVITY = "scroll_sensitivity"
    private const val KEY_MOUSE_ACCEL_ENABLED = "mouse_accel_enabled"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    var sensitivity: Float
        get() = prefs.getFloat(KEY_SENSITIVITY, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SENSITIVITY, value).apply()

    var scrollSensitivity: Float
        get() = prefs.getFloat(KEY_SCROLL_SENSITIVITY, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SCROLL_SENSITIVITY, value).apply()

    var mouseAccelEnabled: Boolean
        get() = prefs.getBoolean(KEY_MOUSE_ACCEL_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MOUSE_ACCEL_ENABLED, value).apply()

    var naturalScroll: Boolean
        get() = prefs.getBoolean(KEY_NATURAL_SCROLL, false)
        set(value) = prefs.edit().putBoolean(KEY_NATURAL_SCROLL, value).apply()

    var keySoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_KEY_SOUND_ENABLED, value).apply()

    var soundProfile: String
        get() = prefs.getString(KEY_SOUND_PROFILE, "cherry-blue") ?: "cherry-blue"
        set(value) = prefs.edit().putString(KEY_SOUND_PROFILE, value).apply()

    var soundVolume: Float
        get() = prefs.getFloat(KEY_SOUND_VOLUME, 0.8f)
        set(value) = prefs.edit().putFloat(KEY_SOUND_VOLUME, value).apply()

    var themeAnimSpeedMs: Int
        get() = prefs.getInt(KEY_THEME_ANIM_SPEED, 450)
        set(value) = prefs.edit().putInt(KEY_THEME_ANIM_SPEED, value).apply()

    var syncTheme: Boolean
        get() = prefs.getBoolean(KEY_SYNC_THEME, true)
        set(value) = prefs.edit().putBoolean(KEY_SYNC_THEME, value).apply()

    var activeThemeName: String
        get() = prefs.getString(KEY_ACTIVE_THEME, "Blanco & Naranja") ?: "Blanco & Naranja"
        set(value) = prefs.edit().putString(KEY_ACTIVE_THEME, value).apply()
}
