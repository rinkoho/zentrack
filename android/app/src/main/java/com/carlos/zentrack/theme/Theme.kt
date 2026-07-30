package com.carlos.zentrack.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- Tokyo Night Premium Color Palette ---
val TokyoBackground = Color(0xFF1A1B26)
val TokyoSurface = Color(0xFF16161E)
val TokyoCard = Color(0xFF24283B)
val TokyoCyan = Color(0xFF7DCFFF)
val TokyoPurple = Color(0xFFBB9AF7)
val TokyoBlue = Color(0xFF7AA2F7)
val TokyoPink = Color(0xFFF7768E)
val TokyoText = Color(0xFFC0CAF5)
val TokyoMuted = Color(0xFF565F89)

@Composable
fun TokyoNightTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = TokyoBackground,
            surface = TokyoSurface,
            primary = TokyoCyan,
            secondary = TokyoPurple,
            onBackground = TokyoText
        ),
        content = content
    )
}
