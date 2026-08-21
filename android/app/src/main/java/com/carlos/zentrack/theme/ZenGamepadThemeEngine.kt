package com.carlos.zentrack.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

/**
 * DEDICATED GAMEPAD THEME ENGINE & COLOR CONFIGURATION
 * Completely isolated standalone engine supporting Rhombus, Circular, and Rectangular keycaps
 * with 8-way D-Pad diagonals, 3D press depth feedback, and theme responsiveness.
 */
data class ZenGamepadThemeConfig(
    val name: String,
    val chasisBg: Color,
    val chasisBorder: Color,
    val surfaceBg: Color,
    val accentGlow: Color,

    // 1. D-Pad Rhombus & Diagonal Keycap Engine Colors
    val dpadHousingBg: Color,
    val dpadHousingBorder: Color,
    val dpadKeyBg: Color,
    val dpadKeyText: Color,
    val dpadKeyShadow: Color,
    val dpadKeyActiveBg: Color,
    val dpadKeyActiveText: Color,

    // 2. ABXY Circular Keycap Engine Colors (Identical Housing to D-Pad for 1:1 Symmetry)
    val abxyHousingBg: Color,
    val abxyHousingBorder: Color,
    val abxyKeyBg: Color,
    val abxyKeyText: Color,
    val abxyKeyShadow: Color,
    val abxyKeyActiveBg: Color,
    val abxyKeyActiveText: Color,

    // 3. Rectangular Bumpers & Triggers (LB / LT / RB / RT)
    val bumperKeyBg: Color,
    val bumperKeyText: Color,
    val bumperKeyShadow: Color,
    val bumperKeyActiveBg: Color,
    val bumperKeyActiveText: Color,

    // 4. Special Pill / Center Buttons (SELECT / START / L3 / R3)
    val pillKeyBg: Color,
    val pillKeyText: Color,
    val pillKeyShadow: Color,
    val pillKeyActiveBg: Color,
    val pillKeyActiveText: Color
)

/**
 * Derived Mapping from global ZenThemeConfig to a dedicated, high-contrast Gamepad Theme
 */
fun ZenThemeConfig.toGamepadTheme(): ZenGamepadThemeConfig {
    return ZenGamepadThemeConfig(
        name = this.name,
        chasisBg = this.chasisBg,
        chasisBorder = this.chasisBorder,
        surfaceBg = this.surface,
        accentGlow = this.primaryAccent,

        // D-Pad Rhombus Setup
        dpadHousingBg = this.card.copy(alpha = 0.88f),
        dpadHousingBorder = this.chasisBorder.copy(alpha = 0.45f),
        dpadKeyBg = this.keyModBg,
        dpadKeyText = this.keyModText,
        dpadKeyShadow = this.keyModShadow,
        dpadKeyActiveBg = this.keyAccentBg,
        dpadKeyActiveText = this.keyAccentText,

        // ABXY Circular Setup (Identical Chassis Base Housing as D-Pad)
        abxyHousingBg = this.card.copy(alpha = 0.88f),
        abxyHousingBorder = this.chasisBorder.copy(alpha = 0.45f),
        abxyKeyBg = this.keyModBg,
        abxyKeyText = this.keyModText,
        abxyKeyShadow = this.keyModShadow,
        abxyKeyActiveBg = this.keyAccentBg,
        abxyKeyActiveText = this.keyAccentText,

        // Rectangular Bumpers & Triggers
        bumperKeyBg = this.keyModBg,
        bumperKeyText = this.keyModText,
        bumperKeyShadow = this.keyModShadow,
        bumperKeyActiveBg = this.keyAccentBg,
        bumperKeyActiveText = this.keyAccentText,

        // Special Pill / Center Buttons (Bright Accent Highlight!)
        pillKeyBg = this.keyAccentBg,
        pillKeyText = this.keyAccentText,
        pillKeyShadow = this.keyAccentShadow,
        pillKeyActiveBg = this.keyModBg,
        pillKeyActiveText = this.keyModText
    )
}

/**
 * Animated State composable for smooth color transitions on Gamepad Theme changes
 */
@Composable
fun rememberAnimatedGamepadTheme(
    targetTheme: ZenGamepadThemeConfig,
    durationMs: Int = 450
): ZenGamepadThemeConfig {
    val spec = tween<Color>(durationMillis = durationMs, easing = FastOutSlowInEasing)

    val chasisBg by animateColorAsState(targetTheme.chasisBg, spec)
    val chasisBorder by animateColorAsState(targetTheme.chasisBorder, spec)
    val surfaceBg by animateColorAsState(targetTheme.surfaceBg, spec)
    val accentGlow by animateColorAsState(targetTheme.accentGlow, spec)

    val dpadHousingBg by animateColorAsState(targetTheme.dpadHousingBg, spec)
    val dpadHousingBorder by animateColorAsState(targetTheme.dpadHousingBorder, spec)
    val dpadKeyBg by animateColorAsState(targetTheme.dpadKeyBg, spec)
    val dpadKeyText by animateColorAsState(targetTheme.dpadKeyText, spec)
    val dpadKeyShadow by animateColorAsState(targetTheme.dpadKeyShadow, spec)
    val dpadKeyActiveBg by animateColorAsState(targetTheme.dpadKeyActiveBg, spec)
    val dpadKeyActiveText by animateColorAsState(targetTheme.dpadKeyActiveText, spec)

    val abxyHousingBg by animateColorAsState(targetTheme.abxyHousingBg, spec)
    val abxyHousingBorder by animateColorAsState(targetTheme.abxyHousingBorder, spec)
    val abxyKeyBg by animateColorAsState(targetTheme.abxyKeyBg, spec)
    val abxyKeyText by animateColorAsState(targetTheme.abxyKeyText, spec)
    val abxyKeyShadow by animateColorAsState(targetTheme.abxyKeyShadow, spec)
    val abxyKeyActiveBg by animateColorAsState(targetTheme.abxyKeyActiveBg, spec)
    val abxyKeyActiveText by animateColorAsState(targetTheme.abxyKeyActiveText, spec)

    val bumperKeyBg by animateColorAsState(targetTheme.bumperKeyBg, spec)
    val bumperKeyText by animateColorAsState(targetTheme.bumperKeyText, spec)
    val bumperKeyShadow by animateColorAsState(targetTheme.bumperKeyShadow, spec)
    val bumperKeyActiveBg by animateColorAsState(targetTheme.bumperKeyActiveBg, spec)
    val bumperKeyActiveText by animateColorAsState(targetTheme.bumperKeyActiveText, spec)

    val pillKeyBg by animateColorAsState(targetTheme.pillKeyBg, spec)
    val pillKeyText by animateColorAsState(targetTheme.pillKeyText, spec)
    val pillKeyShadow by animateColorAsState(targetTheme.pillKeyShadow, spec)
    val pillKeyActiveBg by animateColorAsState(targetTheme.pillKeyActiveBg, spec)
    val pillKeyActiveText by animateColorAsState(targetTheme.pillKeyActiveText, spec)

    return targetTheme.copy(
        chasisBg = chasisBg,
        chasisBorder = chasisBorder,
        surfaceBg = surfaceBg,
        accentGlow = accentGlow,
        dpadHousingBg = dpadHousingBg,
        dpadHousingBorder = dpadHousingBorder,
        dpadKeyBg = dpadKeyBg,
        dpadKeyText = dpadKeyText,
        dpadKeyShadow = dpadKeyShadow,
        dpadKeyActiveBg = dpadKeyActiveBg,
        dpadKeyActiveText = dpadKeyActiveText,
        abxyHousingBg = abxyHousingBg,
        abxyHousingBorder = abxyHousingBorder,
        abxyKeyBg = abxyKeyBg,
        abxyKeyText = abxyKeyText,
        abxyKeyShadow = abxyKeyShadow,
        abxyKeyActiveBg = abxyKeyActiveBg,
        abxyKeyActiveText = abxyKeyActiveText,
        bumperKeyBg = bumperKeyBg,
        bumperKeyText = bumperKeyText,
        bumperKeyShadow = bumperKeyShadow,
        bumperKeyActiveBg = bumperKeyActiveBg,
        bumperKeyActiveText = bumperKeyActiveText,
        pillKeyBg = pillKeyBg,
        pillKeyText = pillKeyText,
        pillKeyShadow = pillKeyShadow,
        pillKeyActiveBg = pillKeyActiveBg,
        pillKeyActiveText = pillKeyActiveText
    )
}
