package com.carlos.zentrack.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

data class ZenThemeConfig(
    val name: String,
    val background: Color,
    val surface: Color,
    val card: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val chasisBg: Color,
    val chasisBorder: Color,

    // Keycap Colors (Cherry MX Profile 3-Color Groups)
    val keyAlphaBg: Color,
    val keyAlphaText: Color,
    val keyAlphaShadow: Color,
    val keyAlphaActiveBg: Color,

    val keyModBg: Color,
    val keyModText: Color,
    val keyModShadow: Color,
    val keyModActiveBg: Color,

    val keyAccentBg: Color,
    val keyAccentText: Color,
    val keyAccentShadow: Color,
    val keyAccentActiveBg: Color
)

@Composable
fun rememberAnimatedZenTheme(
    targetTheme: ZenThemeConfig,
    durationMs: Int = 450
): ZenThemeConfig {
    val spec = tween<Color>(durationMillis = durationMs, easing = FastOutSlowInEasing)

    val background by animateColorAsState(targetTheme.background, spec)
    val surface by animateColorAsState(targetTheme.surface, spec)
    val card by animateColorAsState(targetTheme.card, spec)
    val primaryAccent by animateColorAsState(targetTheme.primaryAccent, spec)
    val secondaryAccent by animateColorAsState(targetTheme.secondaryAccent, spec)
    val textPrimary by animateColorAsState(targetTheme.textPrimary, spec)
    val textMuted by animateColorAsState(targetTheme.textMuted, spec)
    val chasisBg by animateColorAsState(targetTheme.chasisBg, spec)
    val chasisBorder by animateColorAsState(targetTheme.chasisBorder, spec)

    val keyAlphaBg by animateColorAsState(targetTheme.keyAlphaBg, spec)
    val keyAlphaText by animateColorAsState(targetTheme.keyAlphaText, spec)
    val keyAlphaShadow by animateColorAsState(targetTheme.keyAlphaShadow, spec)
    val keyAlphaActiveBg by animateColorAsState(targetTheme.keyAlphaActiveBg, spec)

    val keyModBg by animateColorAsState(targetTheme.keyModBg, spec)
    val keyModText by animateColorAsState(targetTheme.keyModText, spec)
    val keyModShadow by animateColorAsState(targetTheme.keyModShadow, spec)
    val keyModActiveBg by animateColorAsState(targetTheme.keyModActiveBg, spec)

    val keyAccentBg by animateColorAsState(targetTheme.keyAccentBg, spec)
    val keyAccentText by animateColorAsState(targetTheme.keyAccentText, spec)
    val keyAccentShadow by animateColorAsState(targetTheme.keyAccentShadow, spec)
    val keyAccentActiveBg by animateColorAsState(targetTheme.keyAccentActiveBg, spec)

    return targetTheme.copy(
        background = background,
        surface = surface,
        card = card,
        primaryAccent = primaryAccent,
        secondaryAccent = secondaryAccent,
        textPrimary = textPrimary,
        textMuted = textMuted,
        chasisBg = chasisBg,
        chasisBorder = chasisBorder,
        keyAlphaBg = keyAlphaBg,
        keyAlphaText = keyAlphaText,
        keyAlphaShadow = keyAlphaShadow,
        keyAlphaActiveBg = keyAlphaActiveBg,
        keyModBg = keyModBg,
        keyModText = keyModText,
        keyModShadow = keyModShadow,
        keyModActiveBg = keyModActiveBg,
        keyAccentBg = keyAccentBg,
        keyAccentText = keyAccentText,
        keyAccentShadow = keyAccentShadow,
        keyAccentActiveBg = keyAccentActiveBg
    )
}

// 1. Carbon (Blanco & Naranja Neón Industrial)
val ClassicWhiteOrangeTheme = ZenThemeConfig(
    name = "Blanco & Naranja",
    background = Color(0xFF181D26),
    surface = Color(0xFF27272A),
    card = Color(0xFF3F3F46),
    primaryAccent = Color(0xFFFF6B00),
    secondaryAccent = Color(0xFFFFA500),
    textPrimary = Color(0xFFF4F4F5),
    textMuted = Color(0xFFA1A1AA),
    chasisBg = Color(0xFF181D26),
    chasisBorder = Color(0xFFE2E8F0),

    keyAlphaBg = Color(0xFFFFFFFF),
    keyAlphaText = Color(0xFF1A202C),
    keyAlphaShadow = Color(0xFFCBD5E1),
    keyAlphaActiveBg = Color(0xFFF1F5F9),

    keyModBg = Color(0xFF475569),
    keyModText = Color(0xFFF8FAFC),
    keyModShadow = Color(0xFF1E293B),
    keyModActiveBg = Color(0xFF334155),

    keyAccentBg = Color(0xFFDC2626),
    keyAccentText = Color(0xFFFFFFFF),
    keyAccentShadow = Color(0xFF991B1B),
    keyAccentActiveBg = Color(0xFFB91C1C)
)

// 2. Tokyo Night Premium Theme (emilia)
val TokyoNightZenTheme = ZenThemeConfig(
    name = "Tokyo Night",
    background = Color(0xFF1A1B26),
    surface = Color(0xFF16161E),
    card = Color(0xFF24283B),
    primaryAccent = Color(0xFF7AA2F7),
    secondaryAccent = Color(0xFFBB9AF7),
    textPrimary = Color(0xFFC0CAF5),
    textMuted = Color(0xFF565F89),
    chasisBg = Color(0xFF1A1B26),
    chasisBorder = Color(0xFF7AA2F7),

    keyAlphaBg = Color(0xFF20212E),
    keyAlphaText = Color(0xFFC0CAF5),
    keyAlphaShadow = Color(0xFF12131C),
    keyAlphaActiveBg = Color(0xFF2A2B3B),

    keyModBg = Color(0xFF16161E),
    keyModText = Color(0xFF7AA2F7),
    keyModShadow = Color(0xFF0B0B12),
    keyModActiveBg = Color(0xFF1F202B),

    keyAccentBg = Color(0xFFBB9AF7),
    keyAccentText = Color(0xFF16161E),
    keyAccentShadow = Color(0xFF9273C9),
    keyAccentActiveBg = Color(0xFFA685E3)
)

// 3. Pamela (gh0stzk Rice - Dark Slate & Electric Cyan)
val PamelaZenTheme = ZenThemeConfig(
    name = "Pamela",
    background = Color(0xFF1D1F28),
    surface = Color(0xFF282A36),
    card = Color(0xFF353748),
    primaryAccent = Color(0xFF8897F4),
    secondaryAccent = Color(0xFF5ADECD),
    textPrimary = Color(0xFFFDFDFD),
    textMuted = Color(0xFF6C7086),
    chasisBg = Color(0xFF1D1F28),
    chasisBorder = Color(0xFF8897F4),

    keyAlphaBg = Color(0xFF282A36),
    keyAlphaText = Color(0xFFFDFDFD),
    keyAlphaShadow = Color(0xFF15161D),
    keyAlphaActiveBg = Color(0xFF353748),

    keyModBg = Color(0xFF1D1F28),
    keyModText = Color(0xFF5ADECD),
    keyModShadow = Color(0xFF0D0E12),
    keyModActiveBg = Color(0xFF2D303F),

    keyAccentBg = Color(0xFF8897F4),
    keyAccentText = Color(0xFF1D1F28),
    keyAccentShadow = Color(0xFF5B69C4),
    keyAccentActiveBg = Color(0xFF707ECC)
)

// 4. Catppuccin Mocha (daniela)
val CatppuccinMochaTheme = ZenThemeConfig(
    name = "Catppuccin Mocha",
    background = Color(0xFF181825),
    surface = Color(0xFF1E1E2E),
    card = Color(0xFF313244),
    primaryAccent = Color(0xFF89B4FA),
    secondaryAccent = Color(0xFF94E2D5),
    textPrimary = Color(0xFFCDD6F4),
    textMuted = Color(0xFF6C7086),
    chasisBg = Color(0xFF181825),
    chasisBorder = Color(0xFF89B4FA),

    keyAlphaBg = Color(0xFF1E1E2E),
    keyAlphaText = Color(0xFFCDD6F4),
    keyAlphaShadow = Color(0xFF11111B),
    keyAlphaActiveBg = Color(0xFF252538),

    keyModBg = Color(0xFF11111B),
    keyModText = Color(0xFF89B4FA),
    keyModShadow = Color(0xFF09090F),
    keyModActiveBg = Color(0xFF161622),

    keyAccentBg = Color(0xFF94E2D5),
    keyAccentText = Color(0xFF11111B),
    keyAccentShadow = Color(0xFF73BFAE),
    keyAccentActiveBg = Color(0xFF82D6C6)
)

// 5. Laser (jan)
val LaserZenTheme = ZenThemeConfig(
    name = "Laser",
    background = Color(0xFF070219),
    surface = Color(0xFF0E062E),
    card = Color(0xFF1E1B4B),
    primaryAccent = Color(0xFFFB007A),
    secondaryAccent = Color(0xFF27FBFE),
    textPrimary = Color(0xFF27FBFE),
    textMuted = Color(0xFF6B7280),
    chasisBg = Color(0xFF070219),
    chasisBorder = Color(0xFF27FBFE),

    keyAlphaBg = Color(0xFF0E062E),
    keyAlphaText = Color(0xFF27FBFE),
    keyAlphaShadow = Color(0xFF02000C),
    keyAlphaActiveBg = Color(0xFF180B4E),

    keyModBg = Color(0xFF472575),
    keyModText = Color(0xFF19BFFE),
    keyModShadow = Color(0xFF240F40),
    keyModActiveBg = Color(0xFF5E329C),

    keyAccentBg = Color(0xFFFB007A),
    keyAccentText = Color(0xFFFFFFFF),
    keyAccentShadow = Color(0xFFBC005B),
    keyAccentActiveBg = Color(0xFFDB006B)
)

// 6. Olivia
val OliviaZenTheme = ZenThemeConfig(
    name = "Olivia",
    background = Color(0xFF1C1917),
    surface = Color(0xFF27272A),
    card = Color(0xFF3F3F46),
    primaryAccent = Color(0xFFE99F91),
    secondaryAccent = Color(0xFFFDFBF7),
    textPrimary = Color(0xFFFDFBF7),
    textMuted = Color(0xFF71717A),
    chasisBg = Color(0xFF1C1917),
    chasisBorder = Color(0xFFE99F91),

    keyAlphaBg = Color(0xFFFDFBF7),
    keyAlphaText = Color(0xFF1C1917),
    keyAlphaShadow = Color(0xFFCBD5E1),
    keyAlphaActiveBg = Color(0xFFF1F5F9),

    keyModBg = Color(0xFF27272A),
    keyModText = Color(0xFFFDFBF7),
    keyModShadow = Color(0xFF09090B),
    keyModActiveBg = Color(0xFF3F3F46),

    keyAccentBg = Color(0xFFE99F91),
    keyAccentText = Color(0xFF1C1917),
    keyAccentShadow = Color(0xFFB26558),
    keyAccentActiveBg = Color(0xFFD18A7D)
)

// 7. Botanical
val BotanicalZenTheme = ZenThemeConfig(
    name = "Botanical",
    background = Color(0xFF1E2D24),
    surface = Color(0xFF26382D),
    card = Color(0xFF2F3E35),
    primaryAccent = Color(0xFFA3B899),
    secondaryAccent = Color(0xFFF8FAFC),
    textPrimary = Color(0xFFF8FAFC),
    textMuted = Color(0xFF6B7C70),
    chasisBg = Color(0xFF1E2D24),
    chasisBorder = Color(0xFFA3B899),

    keyAlphaBg = Color(0xFFF8FAFC),
    keyAlphaText = Color(0xFF1E2D24),
    keyAlphaShadow = Color(0xFFCBD5E1),
    keyAlphaActiveBg = Color(0xFFF1F5F9),

    keyModBg = Color(0xFF2F3E35),
    keyModText = Color(0xFFF8FAFC),
    keyModShadow = Color(0xFF1A2620),
    keyModActiveBg = Color(0xFF3B4D42),

    keyAccentBg = Color(0xFFA3B899),
    keyAccentText = Color(0xFF1E2D24),
    keyAccentShadow = Color(0xFF7C9073),
    keyAccentActiveBg = Color(0xFF8EAB82)
)

// 8. Retro Classic
val RetroClassicZenTheme = ZenThemeConfig(
    name = "Retro Classic",
    background = Color(0xFF2A2D34),
    surface = Color(0xFF373B44),
    card = Color(0xFF4B505C),
    primaryAccent = Color(0xFFB91C1C),
    secondaryAccent = Color(0xFFD1D5DB),
    textPrimary = Color(0xFFF3F4F6),
    textMuted = Color(0xFF9CA3AF),
    chasisBg = Color(0xFFD1D5DB),
    chasisBorder = Color(0xFF4B5563),

    keyAlphaBg = Color(0xFFE5E7EB),
    keyAlphaText = Color(0xFF111827),
    keyAlphaShadow = Color(0xFFBABCBE),
    keyAlphaActiveBg = Color(0xFFD1D5DB),

    keyModBg = Color(0xFF9CA3AF),
    keyModText = Color(0xFF111827),
    keyModShadow = Color(0xFF717478),
    keyModActiveBg = Color(0xFF878F99),

    keyAccentBg = Color(0xFFB91C1C),
    keyAccentText = Color(0xFFFFFFFF),
    keyAccentShadow = Color(0xFF7C1212),
    keyAccentActiveBg = Color(0xFF991B1B)
)

// 9. Dolch
val DolchZenTheme = ZenThemeConfig(
    name = "Dolch",
    background = Color(0xFF111827),
    surface = Color(0xFF1F2937),
    card = Color(0xFF374151),
    primaryAccent = Color(0xFF06B6D4),
    secondaryAccent = Color(0xFFF3F4F6),
    textPrimary = Color(0xFFF3F4F6),
    textMuted = Color(0xFF6B7280),
    chasisBg = Color(0xFF111827),
    chasisBorder = Color(0xFF4B5563),

    keyAlphaBg = Color(0xFF4B5563),
    keyAlphaText = Color(0xFFF3F4F6),
    keyAlphaShadow = Color(0xFF374151),
    keyAlphaActiveBg = Color(0xFF5D6B7B),

    keyModBg = Color(0xFF1F2937),
    keyModText = Color(0xFF06B6D4),
    keyModShadow = Color(0xFF111827),
    keyModActiveBg = Color(0xFF2C3A4E),

    keyAccentBg = Color(0xFF06B6D4),
    keyAccentText = Color(0xFF111827),
    keyAccentShadow = Color(0xFF087F94),
    keyAccentActiveBg = Color(0xFF0891B2)
)

// 10. Isabel (gh0stzk Rice)
val IsabelZenTheme = ZenThemeConfig(
    name = "Isabel",
    background = Color(0xFF14171C),
    surface = Color(0xFF1D212A),
    card = Color(0xFF2A303D),
    primaryAccent = Color(0xFF7560D3),
    secondaryAccent = Color(0xFF708491),
    textPrimary = Color(0xFFABB2BF),
    textMuted = Color(0xFF5C6370),
    chasisBg = Color(0xFF14171C),
    chasisBorder = Color(0xFF4889BE),

    keyAlphaBg = Color(0xFF1D212A),
    keyAlphaText = Color(0xFFABB2BF),
    keyAlphaShadow = Color(0xFF0C0E11),
    keyAlphaActiveBg = Color(0xFF2A303D),

    keyModBg = Color(0xFF708491),
    keyModText = Color(0xFF14171C),
    keyModShadow = Color(0xFF495760),
    keyModActiveBg = Color(0xFF5E707C),

    keyAccentBg = Color(0xFF7560D3),
    keyAccentText = Color(0xFFFFFFFF),
    keyAccentShadow = Color(0xFF513FA3),
    keyAccentActiveBg = Color(0xFF614CB8)
)

// 11. Andrea (gh0stzk Rice Claro)
val AndreaZenTheme = ZenThemeConfig(
    name = "Andrea",
    background = Color(0xFFE6DFD7),
    surface = Color(0xFFF5EEE6),
    card = Color(0xFFCBD2C9),
    primaryAccent = Color(0xFFC5ABFF),
    secondaryAccent = Color(0xFF151515),
    textPrimary = Color(0xFF151515),
    textMuted = Color(0xFF717171),
    chasisBg = Color(0xFFE6DFD7),
    chasisBorder = Color(0xFF161616),

    keyAlphaBg = Color(0xFFFFFFFF),
    keyAlphaText = Color(0xFF151515),
    keyAlphaShadow = Color(0xFFF5EEE6),
    keyAlphaActiveBg = Color(0xFFF5EEE6),

    keyModBg = Color(0xFFF5EEE6),
    keyModText = Color(0xFF151515),
    keyModShadow = Color(0xFFCBD2C9),
    keyModActiveBg = Color(0xFFE6DFD7),

    keyAccentBg = Color(0xFFC5ABFF),
    keyAccentText = Color(0xFF151515),
    keyAccentShadow = Color(0xFF9C84E2),
    keyAccentActiveBg = Color(0xFFB394F7)
)

// 12. Aline (gh0stzk Rice - Rose Pine Dawn Light)
val AlineZenTheme = ZenThemeConfig(
    name = "Aline",
    background = Color(0xFFFAF4ED),
    surface = Color(0xFFF2E9E1),
    card = Color(0xFFE4DCD3),
    primaryAccent = Color(0xFF907AA9),
    secondaryAccent = Color(0xFF575279),
    textPrimary = Color(0xFF575279),
    textMuted = Color(0xFF9893A5),
    chasisBg = Color(0xFFFAF4ED),
    chasisBorder = Color(0xFF286983),

    keyAlphaBg = Color(0xFFFFFFFF),
    keyAlphaText = Color(0xFF575279),
    keyAlphaShadow = Color(0xFFF2E9E1),
    keyAlphaActiveBg = Color(0xFFF2E9E1),

    keyModBg = Color(0xFFF2E9E1),
    keyModText = Color(0xFF575279),
    keyModShadow = Color(0xFFE4DCD3),
    keyModActiveBg = Color(0xFFE4DCD3),

    keyAccentBg = Color(0xFF907AA9),
    keyAccentText = Color(0xFFFFFFFF),
    keyAccentShadow = Color(0xFF715E87),
    keyAccentActiveBg = Color(0xFF7F6C96)
)

// 13. Brenda (gh0stzk Rice - Everforest Dark)
val BrendaZenTheme = ZenThemeConfig(
    name = "Brenda",
    background = Color(0xFF2D353B),
    surface = Color(0xFF343F44),
    card = Color(0xFF3D494E),
    primaryAccent = Color(0xFFA7C080),
    secondaryAccent = Color(0xFF7FBBB3),
    textPrimary = Color(0xFFD3C6AA),
    textMuted = Color(0xFF859289),
    chasisBg = Color(0xFF2D353B),
    chasisBorder = Color(0xFF7FBBB3),

    keyAlphaBg = Color(0xFF343F44),
    keyAlphaText = Color(0xFFD3C6AA),
    keyAlphaShadow = Color(0xFF232A2E),
    keyAlphaActiveBg = Color(0xFF3D494E),

    keyModBg = Color(0xFF272F35),
    keyModText = Color(0xFFA7C080),
    keyModShadow = Color(0xFF1D2327),
    keyModActiveBg = Color(0xFF313A40),

    keyAccentBg = Color(0xFFA7C080),
    keyAccentText = Color(0xFF2D353B),
    keyAccentShadow = Color(0xFF8BA36C),
    keyAccentActiveBg = Color(0xFF97AF76)
)

// 14. Cristina (gh0stzk Rice - Rose Pine Dark)
val CristinaZenTheme = ZenThemeConfig(
    name = "Cristina",
    background = Color(0xFF232136),
    surface = Color(0xFF2A283E),
    card = Color(0xFF33314C),
    primaryAccent = Color(0xFFC3A5E6),
    secondaryAccent = Color(0xFF9BCED7),
    textPrimary = Color(0xFFE0DEF4),
    textMuted = Color(0xFF6E6A86),
    chasisBg = Color(0xFF232136),
    chasisBorder = Color(0xFF34738E),

    keyAlphaBg = Color(0xFF2A283E),
    keyAlphaText = Color(0xFFE0DEF4),
    keyAlphaShadow = Color(0xFF1A1829),
    keyAlphaActiveBg = Color(0xFF33314C),

    keyModBg = Color(0xFF1F1D2E),
    keyModText = Color(0xFF9BCED7),
    keyModShadow = Color(0xFF12111D),
    keyModActiveBg = Color(0xFF2A283C),

    keyAccentBg = Color(0xFFC3A5E6),
    keyAccentText = Color(0xFF232136),
    keyAccentShadow = Color(0xFF9C80BD),
    keyAccentActiveBg = Color(0xFFAF92D6)
)

// 15. Cynthia (gh0stzk Rice - Kanagawa)
val CynthiaZenTheme = ZenThemeConfig(
    name = "Cynthia",
    background = Color(0xFF181616),
    surface = Color(0xFF222020),
    card = Color(0xFF2D2A2A),
    primaryAccent = Color(0xFF938AA9),
    secondaryAccent = Color(0xFF8A9A7B),
    textPrimary = Color(0xFFC5C9C5),
    textMuted = Color(0xFF717C7C),
    chasisBg = Color(0xFF181616),
    chasisBorder = Color(0xFF8BA4B0),

    keyAlphaBg = Color(0xFF222020),
    keyAlphaText = Color(0xFFC5C9C5),
    keyAlphaShadow = Color(0xFF0F0E0E),
    keyAlphaActiveBg = Color(0xFF2D2A2A),

    keyModBg = Color(0xFF111010),
    keyModText = Color(0xFF8A9A7B),
    keyModShadow = Color(0xFF050505),
    keyModActiveBg = Color(0xFF191818),

    keyAccentBg = Color(0xFF938AA9),
    keyAccentText = Color(0xFF181616),
    keyAccentShadow = Color(0xFF716987),
    keyAccentActiveBg = Color(0xFF827899)
)

// 16. H4ck3r (gh0stzk Rice - Matrix Green)
val HackerZenTheme = ZenThemeConfig(
    name = "H4ck3r",
    background = Color(0xFF0C1018),
    surface = Color(0xFF121824),
    card = Color(0xFF1B2333),
    primaryAccent = Color(0xFF76EA00),
    secondaryAccent = Color(0xFF00FF59),
    textPrimary = Color(0xFF00FF59),
    textMuted = Color(0xFF4E5D78),
    chasisBg = Color(0xFF0C1018),
    chasisBorder = Color(0xFF1947E0),

    keyAlphaBg = Color(0xFF121824),
    keyAlphaText = Color(0xFF00FF59),
    keyAlphaShadow = Color(0xFF06080D),
    keyAlphaActiveBg = Color(0xFF1B2335),

    keyModBg = Color(0xFF1B2333),
    keyModText = Color(0xFF1947E0),
    keyModShadow = Color(0xFF0E121B),
    keyModActiveBg = Color(0xFF243047),

    keyAccentBg = Color(0xFF76EA00),
    keyAccentText = Color(0xFF0C1018),
    keyAccentShadow = Color(0xFF5AB300),
    keyAccentActiveBg = Color(0xFF6BD400)
)

// 17. Karla (gh0stzk Rice - Purple Crimson)
val KarlaZenTheme = ZenThemeConfig(
    name = "Karla",
    background = Color(0xFF0E1113),
    surface = Color(0xFF161B1F),
    card = Color(0xFF20272C),
    primaryAccent = Color(0xFFE7034A),
    secondaryAccent = Color(0xFF5884D4),
    textPrimary = Color(0xFFAFB1DB),
    textMuted = Color(0xFF535870),
    chasisBg = Color(0xFF0E1113),
    chasisBorder = Color(0xFF5884D4),

    keyAlphaBg = Color(0xFF161B1F),
    keyAlphaText = Color(0xFFAFB1DB),
    keyAlphaShadow = Color(0xFF050608),
    keyAlphaActiveBg = Color(0xFF20272C),

    keyModBg = Color(0xFF353C52),
    keyModText = Color(0xFFE7034A),
    keyModShadow = Color(0xFF1D212E),
    keyModActiveBg = Color(0xFF454F6B),

    keyAccentBg = Color(0xFFE7034A),
    keyAccentText = Color(0xFFFFFFFF),
    keyAccentShadow = Color(0xFFA30234),
    keyAccentActiveBg = Color(0xFFC4023E)
)

// 18. Marisol (gh0stzk Rice - Dracula)
val MarisolZenTheme = ZenThemeConfig(
    name = "Marisol",
    background = Color(0xFF282A36),
    surface = Color(0xFF343746),
    card = Color(0xFF44475A),
    primaryAccent = Color(0xFFBD93F9),
    secondaryAccent = Color(0xFFFF79C6),
    textPrimary = Color(0xFFF8F8F2),
    textMuted = Color(0xFF6272A4),
    chasisBg = Color(0xFF282A36),
    chasisBorder = Color(0xFFBD93F9),

    keyAlphaBg = Color(0xFF343746),
    keyAlphaText = Color(0xFFF8F8F2),
    keyAlphaShadow = Color(0xFF1A1B23),
    keyAlphaActiveBg = Color(0xFF3F4355),

    keyModBg = Color(0xFF21222C),
    keyModText = Color(0xFFFF79C6),
    keyModShadow = Color(0xFF121319),
    keyModActiveBg = Color(0xFF2F313F),

    keyAccentBg = Color(0xFFBD93F9),
    keyAccentText = Color(0xFF282A36),
    keyAccentShadow = Color(0xFF926BCA),
    keyAccentActiveBg = Color(0xFFA479EB)
)

// 19. Melissa (gh0stzk Rice - Nord)
val MelissaZenTheme = ZenThemeConfig(
    name = "Melissa",
    background = Color(0xFF2E3440),
    surface = Color(0xFF3B4252),
    card = Color(0xFF434C5E),
    primaryAccent = Color(0xFF88C0D0),
    secondaryAccent = Color(0xFF81A1C1),
    textPrimary = Color(0xFFD8DEE9),
    textMuted = Color(0xFF4C566A),
    chasisBg = Color(0xFF2E3440),
    chasisBorder = Color(0xFF81A1C1),

    keyAlphaBg = Color(0xFF3B4252),
    keyAlphaText = Color(0xFFD8DEE9),
    keyAlphaShadow = Color(0xFF242932),
    keyAlphaActiveBg = Color(0xFF475063),

    keyModBg = Color(0xFF2E3440),
    keyModText = Color(0xFF81A1C1),
    keyModShadow = Color(0xFF1C2027),
    keyModActiveBg = Color(0xFF3C4454),

    keyAccentBg = Color(0xFF88C0D0),
    keyAccentText = Color(0xFF2E3440),
    keyAccentShadow = Color(0xFF5C9AA9),
    keyAccentActiveBg = Color(0xFF70B2C4)
)

// 20. Silvia (gh0stzk Rice - Gruvbox)
val SilviaZenTheme = ZenThemeConfig(
    name = "Silvia",
    background = Color(0xFF282828),
    surface = Color(0xFF3C3836),
    card = Color(0xFF504945),
    primaryAccent = Color(0xFFFABD2F),
    secondaryAccent = Color(0xFFFE8019),
    textPrimary = Color(0xFFFBF1C7),
    textMuted = Color(0xFF928374),
    chasisBg = Color(0xFF282828),
    chasisBorder = Color(0xFFEBDBB2),

    keyAlphaBg = Color(0xFF3C3836),
    keyAlphaText = Color(0xFFFBF1C7),
    keyAlphaShadow = Color(0xFF201E1D),
    keyAlphaActiveBg = Color(0xFF4D4845),

    keyModBg = Color(0xFF1D2021),
    keyModText = Color(0xFFFE8019),
    keyModShadow = Color(0xFF0C0D0E),
    keyModActiveBg = Color(0xFF2E3234),

    keyAccentBg = Color(0xFFFABD2F),
    keyAccentText = Color(0xFF282828),
    keyAccentShadow = Color(0xFFC29220),
    keyAccentActiveBg = Color(0xFFDBAB32)
)

// 21. Varinka (gh0stzk Rice - Monochrome Dark)
val VarinkaZenTheme = ZenThemeConfig(
    name = "Varinka",
    background = Color(0xFF212529),
    surface = Color(0xFF2C3136),
    card = Color(0xFF3B4249),
    primaryAccent = Color(0xFFF8F9FA),
    secondaryAccent = Color(0xFFCED4DA),
    textPrimary = Color(0xFFF8F9FA),
    textMuted = Color(0xFF6C757D),
    chasisBg = Color(0xFF212529),
    chasisBorder = Color(0xFFCED4DA),

    keyAlphaBg = Color(0xFF2C3136),
    keyAlphaText = Color(0xFFF8F9FA),
    keyAlphaShadow = Color(0xFF16191C),
    keyAlphaActiveBg = Color(0xFF3B4249),

    keyModBg = Color(0xFF1A1D20),
    keyModText = Color(0xFFF8F9FA),
    keyModShadow = Color(0xFF0D0E10),
    keyModActiveBg = Color(0xFF272C30),

    keyAccentBg = Color(0xFFF8F9FA),
    keyAccentText = Color(0xFF212529),
    keyAccentShadow = Color(0xFFCED4DA),
    keyAccentActiveBg = Color(0xFFE9ECEF)
)

// 22. Yael (gh0stzk Rice - Minimal Black & White)
val YaelZenTheme = ZenThemeConfig(
    name = "Yael",
    background = Color(0xFF161616),
    surface = Color(0xFF262626),
    card = Color(0xFF353535),
    primaryAccent = Color(0xFF42BE65),
    secondaryAccent = Color(0xFF3DBDDB),
    textPrimary = Color(0xFFFFFFFF),
    textMuted = Color(0xFF666666),
    chasisBg = Color(0xFF161616),
    chasisBorder = Color(0xFFFF7EB6),

    keyAlphaBg = Color(0xFF262626),
    keyAlphaText = Color(0xFFFFFFFF),
    keyAlphaShadow = Color(0xFF0C0C0C),
    keyAlphaActiveBg = Color(0xFF353535),

    keyModBg = Color(0xFF161616),
    keyModText = Color(0xFF3DBDDB),
    keyModShadow = Color(0xFF040404),
    keyModActiveBg = Color(0xFF2A2A2A),

    keyAccentBg = Color(0xFF42BE65),
    keyAccentText = Color(0xFF161616),
    keyAccentShadow = Color(0xFF2E8546),
    keyAccentActiveBg = Color(0xFF37A155)
)

// 23. Z0mbi3 (gh0stzk Rice - Red Cyberpunk)
val ZombieZenTheme = ZenThemeConfig(
    name = "Z0mbi3",
    background = Color(0xFF0D0F18),
    surface = Color(0xFF171A29),
    card = Color(0xFF24283F),
    primaryAccent = Color(0xFF93CEE9),
    secondaryAccent = Color(0xFFDD6777),
    textPrimary = Color(0xFFA5B6CF),
    textMuted = Color(0xFF56627A),
    chasisBg = Color(0xFF0D0F18),
    chasisBorder = Color(0xFF86AAEC),

    keyAlphaBg = Color(0xFF171A29),
    keyAlphaText = Color(0xFFA5B6CF),
    keyAlphaShadow = Color(0xFF06070A),
    keyAlphaActiveBg = Color(0xFF24283F),

    keyModBg = Color(0xFF0D0F18),
    keyModText = Color(0xFFDD6777),
    keyModShadow = Color(0xFF030305),
    keyModActiveBg = Color(0xFF1A1E30),

    keyAccentBg = Color(0xFF93CEE9),
    keyAccentText = Color(0xFF0D0F18),
    keyAccentShadow = Color(0xFF6DA5BF),
    keyAccentActiveBg = Color(0xFF7DBAD6)
)

// List of all 23 available themes matching public/style.css
val AvailableZenThemes = listOf(
    ClassicWhiteOrangeTheme,
    TokyoNightZenTheme,
    PamelaZenTheme,
    CatppuccinMochaTheme,
    LaserZenTheme,
    OliviaZenTheme,
    BotanicalZenTheme,
    RetroClassicZenTheme,
    DolchZenTheme,
    IsabelZenTheme,
    AndreaZenTheme,
    AlineZenTheme,
    BrendaZenTheme,
    CristinaZenTheme,
    CynthiaZenTheme,
    HackerZenTheme,
    KarlaZenTheme,
    MarisolZenTheme,
    MelissaZenTheme,
    SilviaZenTheme,
    VarinkaZenTheme,
    YaelZenTheme,
    ZombieZenTheme
)

fun findZenThemeByName(name: String): ZenThemeConfig {
    val cleanName = name.lowercase().replace("-", "").replace(" ", "").replace("_", "")
    return AvailableZenThemes.find {
        val tName = it.name.lowercase().replace("-", "").replace(" ", "").replace("_", "")
        tName.contains(cleanName) || cleanName.contains(tName)
    } ?: when {
        cleanName.contains("pamela") -> PamelaZenTheme
        cleanName.contains("emilia") || cleanName.contains("tokyo") -> TokyoNightZenTheme
        cleanName.contains("daniela") || cleanName.contains("catppuccin") -> CatppuccinMochaTheme
        cleanName.contains("jan") || cleanName.contains("laser") -> LaserZenTheme
        cleanName.contains("isabel") -> IsabelZenTheme
        cleanName.contains("andrea") -> AndreaZenTheme
        cleanName.contains("aline") -> AlineZenTheme
        cleanName.contains("brenda") -> BrendaZenTheme
        cleanName.contains("cristina") -> CristinaZenTheme
        cleanName.contains("cynthia") -> CynthiaZenTheme
        cleanName.contains("karla") -> KarlaZenTheme
        cleanName.contains("marisol") || cleanName.contains("dracula") -> MarisolZenTheme
        cleanName.contains("melissa") || cleanName.contains("nord") -> MelissaZenTheme
        cleanName.contains("silvia") || cleanName.contains("gruvbox") -> SilviaZenTheme
        cleanName.contains("varinka") -> VarinkaZenTheme
        cleanName.contains("yael") -> YaelZenTheme
        cleanName.contains("z0mbi3") || cleanName.contains("zombie") -> ZombieZenTheme
        cleanName.contains("hacker") || cleanName.contains("matrix") -> HackerZenTheme
        cleanName.contains("dolch") -> DolchZenTheme
        cleanName.contains("botanical") -> BotanicalZenTheme
        cleanName.contains("olivia") -> OliviaZenTheme
        cleanName.contains("retro") -> RetroClassicZenTheme
        else -> ClassicWhiteOrangeTheme
    }
}

val LocalZenTheme = compositionLocalOf { ClassicWhiteOrangeTheme }
