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
    private const val KEY_MOUSE_ACCEL_PROFILE = "mouse_accel_profile"
    private const val KEY_STICKY_KEYS_ENABLED = "sticky_keys_enabled"
    private const val KEY_HYBRID_KEYBOARD_HEIGHT_RATIO = "hybrid_keyboard_height_ratio"
    private const val KEY_KEY_CASTER_ENABLED = "key_caster_enabled"
    private const val KEY_USB_ADB_MODE_ENABLED = "usb_adb_mode_enabled"
    private const val KEY_INVERT_THREE_FINGER_SWIPE = "invert_three_finger_swipe"
    private const val KEY_TRACKPAD_PHYSICAL_BUTTONS_ENABLED = "trackpad_physical_buttons_enabled"
    private const val KEY_TRACKPAD_BUTTONS_POSITION = "trackpad_buttons_position"
    private const val KEY_TRACKPAD_SCROLL_POSITION = "trackpad_scroll_position"
    private const val KEY_TRACKPAD_SCROLL_WIDTH = "trackpad_scroll_width"
    private const val KEY_TRACKPAD_BUTTONS_SIDEBAR_WIDTH = "trackpad_buttons_sidebar_width"
    private const val KEY_TRACKPAD_BUTTONS_BOTTOM_HEIGHT = "trackpad_buttons_bottom_height"
    private const val KEY_HAPTIC_TRACKPAD_INTENSITY = "haptic_trackpad_intensity"
    private const val KEY_HAPTIC_KEYBOARD_INTENSITY = "haptic_keyboard_intensity"
    private const val KEY_KEYCAP_BORDERS_ENABLED = "keycap_borders_enabled"

    private const val KEY_GAMEPAD_MODE = "gamepad_mode"
    private const val KEY_GAMEPAD_PC_SENSITIVITY = "gamepad_pc_sensitivity"
    private const val KEY_GAMEPAD_PC_ACCEL_ENABLED = "gamepad_pc_accel_enabled"
    private const val KEY_GAMEPAD_XBOX_SENSITIVITY = "gamepad_xbox_sensitivity"
    private const val KEY_GAMEPAD_XBOX_RIGHT_STICK_MODE = "gamepad_xbox_right_stick_mode"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    var gamepadMode: String
        get() = prefs.getString(KEY_GAMEPAD_MODE, "pc") ?: "pc"
        set(value) = prefs.edit().putString(KEY_GAMEPAD_MODE, value).apply()

    var gamepadPcSensitivity: Float
        get() = prefs.getFloat(KEY_GAMEPAD_PC_SENSITIVITY, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_GAMEPAD_PC_SENSITIVITY, value).apply()

    var gamepadPcAccelEnabled: Boolean
        get() = prefs.getBoolean(KEY_GAMEPAD_PC_ACCEL_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_GAMEPAD_PC_ACCEL_ENABLED, value).apply()

    var gamepadXboxSensitivity: Float
        get() = prefs.getFloat(KEY_GAMEPAD_XBOX_SENSITIVITY, 1.8f)
        set(value) = prefs.edit().putFloat(KEY_GAMEPAD_XBOX_SENSITIVITY, value).apply()

    var gamepadXboxRightStickMode: String
        get() = prefs.getString(KEY_GAMEPAD_XBOX_RIGHT_STICK_MODE, "autocenter") ?: "autocenter"
        set(value) = prefs.edit().putString(KEY_GAMEPAD_XBOX_RIGHT_STICK_MODE, value).apply()

    var stickyKeysEnabled: Boolean
        get() = prefs.getBoolean(KEY_STICKY_KEYS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_STICKY_KEYS_ENABLED, value).apply()

    var sensitivity: Float
        get() = prefs.getFloat(KEY_SENSITIVITY, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SENSITIVITY, value).apply()

    var scrollSensitivity: Float
        get() = prefs.getFloat(KEY_SCROLL_SENSITIVITY, 0.5f)
        set(value) = prefs.edit().putFloat(KEY_SCROLL_SENSITIVITY, value).apply()

    var mouseAccelEnabled: Boolean
        get() = mouseAccelProfile != "none"
        set(value) {
            if (!value) {
                mouseAccelProfile = "none"
            } else if (mouseAccelProfile == "none") {
                mouseAccelProfile = "linear_offset_cap"
            }
        }

    var mouseAccelProfile: String
        get() = prefs.getString(KEY_MOUSE_ACCEL_PROFILE, "linear_offset_cap") ?: "linear_offset_cap"
        set(value) = prefs.edit().putString(KEY_MOUSE_ACCEL_PROFILE, value).apply()

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
        get() = prefs.getString(KEY_ACTIVE_THEME, "Carbon Orange") ?: "Carbon Orange"
        set(value) = prefs.edit().putString(KEY_ACTIVE_THEME, value).apply()

    var hybridKeyboardHeightRatio: Float
        get() = prefs.getFloat(KEY_HYBRID_KEYBOARD_HEIGHT_RATIO, 0.65f)
        set(value) = prefs.edit().putFloat(KEY_HYBRID_KEYBOARD_HEIGHT_RATIO, value).apply()

    var keyCasterEnabled: Boolean
        get() = prefs.getBoolean(KEY_KEY_CASTER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_KEY_CASTER_ENABLED, value).apply()

    var usbAdbModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_USB_ADB_MODE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_USB_ADB_MODE_ENABLED, value).apply()

    var invertThreeFingerSwipe: Boolean
        get() = prefs.getBoolean(KEY_INVERT_THREE_FINGER_SWIPE, false)
        set(value) = prefs.edit().putBoolean(KEY_INVERT_THREE_FINGER_SWIPE, value).apply()

    var trackpadPhysicalButtonsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TRACKPAD_PHYSICAL_BUTTONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TRACKPAD_PHYSICAL_BUTTONS_ENABLED, value).apply()

    var trackpadButtonsPosition: String
        get() = prefs.getString(KEY_TRACKPAD_BUTTONS_POSITION, "right") ?: "right" // "right", "left", "bottom"
        set(value) = prefs.edit().putString(KEY_TRACKPAD_BUTTONS_POSITION, value).apply()

    var trackpadScrollPosition: String
        get() = prefs.getString(KEY_TRACKPAD_SCROLL_POSITION, "right") ?: "right" // "right", "left"
        set(value) = prefs.edit().putString(KEY_TRACKPAD_SCROLL_POSITION, value).apply()

    var trackpadScrollWidth: Int
        get() = prefs.getInt(KEY_TRACKPAD_SCROLL_WIDTH, 24)
        set(value) = prefs.edit().putInt(KEY_TRACKPAD_SCROLL_WIDTH, value).apply()

    var trackpadButtonsSidebarWidth: Int
        get() = prefs.getInt(KEY_TRACKPAD_BUTTONS_SIDEBAR_WIDTH, 85)
        set(value) = prefs.edit().putInt(KEY_TRACKPAD_BUTTONS_SIDEBAR_WIDTH, value).apply()

    var trackpadButtonsBottomHeight: Int
        get() = prefs.getInt(KEY_TRACKPAD_BUTTONS_BOTTOM_HEIGHT, 48)
        set(value) = prefs.edit().putInt(KEY_TRACKPAD_BUTTONS_BOTTOM_HEIGHT, value).apply()

    var hapticTrackpadIntensity: Float
        get() = prefs.getFloat(KEY_HAPTIC_TRACKPAD_INTENSITY, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_HAPTIC_TRACKPAD_INTENSITY, value).apply()

    var hapticKeyboardIntensity: Float
        get() = prefs.getFloat(KEY_HAPTIC_KEYBOARD_INTENSITY, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_HAPTIC_KEYBOARD_INTENSITY, value).apply()

    var keycapBordersEnabled: Boolean
        get() = prefs.getBoolean(KEY_KEYCAP_BORDERS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_KEYCAP_BORDERS_ENABLED, value).apply()

    // Xbox Custom Layout Preferences
    var xboxBtnScale: Float
        get() = prefs.getFloat("xbox_btn_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_btn_scale", value).apply()

    var xboxAbxyOffsetX: Float
        get() = prefs.getFloat("xbox_abxy_offset_x", 32f)
        set(value) = prefs.edit().putFloat("xbox_abxy_offset_x", value).apply()

    var xboxAbxyOffsetY: Float
        get() = prefs.getFloat("xbox_abxy_offset_y", 32f)
        set(value) = prefs.edit().putFloat("xbox_abxy_offset_y", value).apply()

    var xboxDpadOffsetX: Float
        get() = prefs.getFloat("xbox_dpad_offset_x", 32f)
        set(value) = prefs.edit().putFloat("xbox_dpad_offset_x", value).apply()

    var xboxDpadOffsetY: Float
        get() = prefs.getFloat("xbox_dpad_offset_y", 32f)
        set(value) = prefs.edit().putFloat("xbox_dpad_offset_y", value).apply()

    var xboxBumperLOffsetX: Float
        get() = prefs.getFloat("xbox_bumper_l_offset_x", 16f)
        set(value) = prefs.edit().putFloat("xbox_bumper_l_offset_x", value).apply()

    var xboxBumperLOffsetY: Float
        get() = prefs.getFloat("xbox_bumper_l_offset_y", 42f)
        set(value) = prefs.edit().putFloat("xbox_bumper_l_offset_y", value).apply()

    var xboxBumperROffsetX: Float
        get() = prefs.getFloat("xbox_bumper_r_offset_x", 16f)
        set(value) = prefs.edit().putFloat("xbox_bumper_r_offset_x", value).apply()

    var xboxBumperROffsetY: Float
        get() = prefs.getFloat("xbox_bumper_r_offset_y", 42f)
        set(value) = prefs.edit().putFloat("xbox_bumper_r_offset_y", value).apply()

    var xboxCenterMenuOffsetY: Float
        get() = prefs.getFloat("xbox_center_menu_offset_y", 42f)
        set(value) = prefs.edit().putFloat("xbox_center_menu_offset_y", value).apply()

    var xboxDpadScale: Float
        get() = prefs.getFloat("xbox_dpad_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_dpad_scale", value).apply()

    var xboxAbxyScale: Float
        get() = prefs.getFloat("xbox_abxy_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_abxy_scale", value).apply()

    var xboxAbxyInnerScale: Float
        get() = prefs.getFloat("xbox_abxy_inner_scale", 1.05f)
        set(value) = prefs.edit().putFloat("xbox_abxy_inner_scale", value).apply()

    var xboxBumperLScale: Float
        get() = prefs.getFloat("xbox_bumper_l_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_bumper_l_scale", value).apply()

    var xboxBumperRScale: Float
        get() = prefs.getFloat("xbox_bumper_r_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_bumper_r_scale", value).apply()

    var xboxCenterMenuScale: Float
        get() = prefs.getFloat("xbox_center_menu_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_center_menu_scale", value).apply()

    // Individual Button Offsets & Scales
    var xboxBtnSelectOffsetX: Float
        get() = prefs.getFloat("xbox_btn_select_offset_x", -60f)
        set(value) = prefs.edit().putFloat("xbox_btn_select_offset_x", value).apply()
    var xboxBtnSelectOffsetY: Float
        get() = prefs.getFloat("xbox_btn_select_offset_y", 42f)
        set(value) = prefs.edit().putFloat("xbox_btn_select_offset_y", value).apply()
    var xboxBtnSelectScale: Float
        get() = prefs.getFloat("xbox_btn_select_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_btn_select_scale", value).apply()

    var xboxBtnL3OffsetX: Float
        get() = prefs.getFloat("xbox_btn_l3_offset_x", -20f)
        set(value) = prefs.edit().putFloat("xbox_btn_l3_offset_x", value).apply()
    var xboxBtnL3OffsetY: Float
        get() = prefs.getFloat("xbox_btn_l3_offset_y", 42f)
        set(value) = prefs.edit().putFloat("xbox_btn_l3_offset_y", value).apply()
    var xboxBtnL3Scale: Float
        get() = prefs.getFloat("xbox_btn_l3_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_btn_l3_scale", value).apply()

    var xboxBtnR3OffsetX: Float
        get() = prefs.getFloat("xbox_btn_r3_offset_x", 20f)
        set(value) = prefs.edit().putFloat("xbox_btn_r3_offset_x", value).apply()
    var xboxBtnR3OffsetY: Float
        get() = prefs.getFloat("xbox_btn_r3_offset_y", 42f)
        set(value) = prefs.edit().putFloat("xbox_btn_r3_offset_y", value).apply()
    var xboxBtnR3Scale: Float
        get() = prefs.getFloat("xbox_btn_r3_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_btn_r3_scale", value).apply()

    var xboxBtnStartOffsetX: Float
        get() = prefs.getFloat("xbox_btn_start_offset_x", 60f)
        set(value) = prefs.edit().putFloat("xbox_btn_start_offset_x", value).apply()
    var xboxBtnStartOffsetY: Float
        get() = prefs.getFloat("xbox_btn_start_offset_y", 42f)
        set(value) = prefs.edit().putFloat("xbox_btn_start_offset_y", value).apply()
    var xboxBtnStartScale: Float
        get() = prefs.getFloat("xbox_btn_start_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_btn_start_scale", value).apply()

    var xboxBtnLTOffsetX: Float
        get() = prefs.getFloat("xbox_btn_lt_offset_x", 14f)
        set(value) = prefs.edit().putFloat("xbox_btn_lt_offset_x", value).apply()
    var xboxBtnLTOffsetY: Float
        get() = prefs.getFloat("xbox_btn_lt_offset_y", 42f)
        set(value) = prefs.edit().putFloat("xbox_btn_lt_offset_y", value).apply()
    var xboxBtnLTScale: Float
        get() = prefs.getFloat("xbox_btn_lt_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_btn_lt_scale", value).apply()

    var xboxBtnLBOffsetX: Float
        get() = prefs.getFloat("xbox_btn_lb_offset_x", 70f)
        set(value) = prefs.edit().putFloat("xbox_btn_lb_offset_x", value).apply()
    var xboxBtnLBOffsetY: Float
        get() = prefs.getFloat("xbox_btn_lb_offset_y", 42f)
        set(value) = prefs.edit().putFloat("xbox_btn_lb_offset_y", value).apply()
    var xboxBtnLBScale: Float
        get() = prefs.getFloat("xbox_btn_lb_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_btn_lb_scale", value).apply()

    var xboxBtnRBOffsetX: Float
        get() = prefs.getFloat("xbox_btn_rb_offset_x", 70f)
        set(value) = prefs.edit().putFloat("xbox_btn_rb_offset_x", value).apply()
    var xboxBtnRBOffsetY: Float
        get() = prefs.getFloat("xbox_btn_rb_offset_y", 42f)
        set(value) = prefs.edit().putFloat("xbox_btn_rb_offset_y", value).apply()
    var xboxBtnRBScale: Float
        get() = prefs.getFloat("xbox_btn_rb_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_btn_rb_scale", value).apply()

    var xboxBtnRTOffsetX: Float
        get() = prefs.getFloat("xbox_btn_rt_offset_x", 14f)
        set(value) = prefs.edit().putFloat("xbox_btn_rt_offset_x", value).apply()
    var xboxBtnRTOffsetY: Float
        get() = prefs.getFloat("xbox_btn_rt_offset_y", 42f)
        set(value) = prefs.edit().putFloat("xbox_btn_rt_offset_y", value).apply()
    var xboxBtnRTScale: Float
        get() = prefs.getFloat("xbox_btn_rt_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_btn_rt_scale", value).apply()

    var xboxGroupLinks: String
        get() = prefs.getString("xbox_group_links", "") ?: ""
        set(value) = prefs.edit().putString("xbox_group_links", value).apply()

    // ABXY Layout Mode: "grouped" (diamond housing) or "freeform" (individual A, B, X, Y buttons)
    var xboxAbxyMode: String
        get() = prefs.getString("xbox_abxy_mode", "grouped") ?: "grouped"
        set(value) = prefs.edit().putString("xbox_abxy_mode", value).apply()

    // Individual Freeform ABXY Button Offsets & Scales
    var xboxBtnAOffsetX: Float
        get() = prefs.getFloat("xbox_btn_a_offset_x", 32f)
        set(value) = prefs.edit().putFloat("xbox_btn_a_offset_x", value).apply()
    var xboxBtnAOffsetY: Float
        get() = prefs.getFloat("xbox_btn_a_offset_y", 32f)
        set(value) = prefs.edit().putFloat("xbox_btn_a_offset_y", value).apply()
    var xboxBtnAScale: Float
        get() = prefs.getFloat("xbox_btn_a_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_btn_a_scale", value).apply()

    var xboxBtnBOffsetX: Float
        get() = prefs.getFloat("xbox_btn_b_offset_x", 78f)
        set(value) = prefs.edit().putFloat("xbox_btn_b_offset_x", value).apply()
    var xboxBtnBOffsetY: Float
        get() = prefs.getFloat("xbox_btn_b_offset_y", 78f)
        set(value) = prefs.edit().putFloat("xbox_btn_b_offset_y", value).apply()
    var xboxBtnBScale: Float
        get() = prefs.getFloat("xbox_btn_b_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_btn_b_scale", value).apply()

    var xboxBtnXOffsetX: Float
        get() = prefs.getFloat("xbox_btn_x_offset_x", 14f)
        set(value) = prefs.edit().putFloat("xbox_btn_x_offset_x", value).apply()
    var xboxBtnXOffsetY: Float
        get() = prefs.getFloat("xbox_btn_x_offset_y", 78f)
        set(value) = prefs.edit().putFloat("xbox_btn_x_offset_y", value).apply()
    var xboxBtnXScale: Float
        get() = prefs.getFloat("xbox_btn_x_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_btn_x_scale", value).apply()

    var xboxBtnYOffsetX: Float
        get() = prefs.getFloat("xbox_btn_y_offset_x", 32f)
        set(value) = prefs.edit().putFloat("xbox_btn_y_offset_x", value).apply()
    var xboxBtnYOffsetY: Float
        get() = prefs.getFloat("xbox_btn_y_offset_y", 124f)
        set(value) = prefs.edit().putFloat("xbox_btn_y_offset_y", value).apply()
    var xboxBtnYScale: Float
        get() = prefs.getFloat("xbox_btn_y_scale", 1.0f)
        set(value) = prefs.edit().putFloat("xbox_btn_y_scale", value).apply()

    // Xbox Custom Layout Profiles Engine
    var xboxProfilesJson: String
        get() = prefs.getString("xbox_profiles_json", "") ?: ""
        set(value) = prefs.edit().putString("xbox_profiles_json", value).apply()

    var xboxActiveProfileId: String
        get() = prefs.getString("xbox_active_profile_id", "default") ?: "default"
        set(value) = prefs.edit().putString("xbox_active_profile_id", value).apply()

    // Server Connection Preferences
    var serverIp: String
        get() = prefs.getString("server_ip", "") ?: ""
        set(value) = prefs.edit().putString("server_ip", value).apply()

    var serverPort: Int
        get() = prefs.getInt("server_port", 3000)
        set(value) = prefs.edit().putInt("server_port", value).apply()

    var serverToken: String
        get() = prefs.getString("server_token", "") ?: ""
        set(value) = prefs.edit().putString("server_token", value).apply()

    val isConfigured: Boolean
        get() = serverToken.isNotBlank() && (serverIp.isNotBlank() || usbAdbModeEnabled)
}


