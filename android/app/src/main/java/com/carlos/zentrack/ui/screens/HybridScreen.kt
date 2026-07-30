package com.carlos.zentrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.carlos.zentrack.theme.ZenThemeConfig

@Composable
fun HybridScreen(
    currentTheme: ZenThemeConfig,
    isConnected: Boolean,
    statusText: String,
    sensitivity: Float,
    scrollSensitivity: Float,
    mouseAccelEnabled: Boolean,
    naturalScroll: Boolean,
    onOpenDrawer: () -> Unit,
    onReconnect: () -> Unit,
    onOpenThemeDialog: () -> Unit,
    onSendBinary: (Short, Int, Int) -> Unit,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // TOP SECTION (~38% HEIGHT): Compact High-Precision Trackpad Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.38f)
        ) {
            TrackpadScreen(
                currentTheme = currentTheme,
                isConnected = isConnected,
                statusText = statusText,
                sensitivity = sensitivity,
                scrollSensitivity = scrollSensitivity,
                mouseAccelEnabled = mouseAccelEnabled,
                naturalScroll = naturalScroll,
                isCompactMode = true,
                onOpenDrawer = onOpenDrawer,
                onReconnect = onReconnect,
                onSendBinary = onSendBinary,
                onSendJson = onSendJson,
                onVibrate = onVibrate
            )
        }

        // BOTTOM SECTION (~62% HEIGHT): 65% Mechanical Keyboard
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.62f)
        ) {
            KeyboardScreen(
                currentTheme = currentTheme,
                isConnected = isConnected,
                statusText = statusText,
                showTopBar = false,
                onOpenDrawer = onOpenDrawer,
                onReconnect = onReconnect,
                onOpenThemeDialog = onOpenThemeDialog,
                onSendJson = onSendJson,
                onVibrate = onVibrate
            )
        }
    }
}
