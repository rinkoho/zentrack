package com.carlos.zentrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carlos.zentrack.theme.ZenThemeConfig
import com.carlos.zentrack.ui.components.KeycasterFloatingHUD
import com.carlos.zentrack.ui.components.KeycasterState
import com.carlos.zentrack.ui.components.rememberKeycasterState

@Composable
fun HybridScreen(
    currentTheme: ZenThemeConfig,
    isConnected: Boolean,
    statusText: String,
    sensitivity: Float,
    scrollSensitivity: Float,
    mouseAccelEnabled: Boolean,
    mouseAccelProfile: String = com.carlos.zentrack.preferences.ZenPreferences.mouseAccelProfile,
    naturalScroll: Boolean,
    scrollPosition: String = com.carlos.zentrack.preferences.ZenPreferences.trackpadScrollPosition,
    scrollWidth: Int = com.carlos.zentrack.preferences.ZenPreferences.trackpadScrollWidth,
    invertThreeFingerSwipe: Boolean = com.carlos.zentrack.preferences.ZenPreferences.invertThreeFingerSwipe,
    stickyKeysEnabled: Boolean = true,
    hybridKeyboardHeightRatio: Float = com.carlos.zentrack.preferences.ZenPreferences.hybridKeyboardHeightRatio,
    keyCasterEnabled: Boolean = true,
    keycasterState: KeycasterState = rememberKeycasterState(),
    onOpenDrawer: () -> Unit,
    onReconnect: () -> Unit,
    onOpenThemeDialog: () -> Unit,
    onSendBinary: (Short, Int, Int) -> Unit,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    val topWeight = (1.0f - hybridKeyboardHeightRatio).coerceIn(0.1f, 0.9f)
    val bottomWeight = hybridKeyboardHeightRatio.coerceIn(0.1f, 0.9f)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP SECTION: Trackpad Surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(topWeight)
            ) {
                TrackpadScreen(
                    currentTheme = currentTheme,
                    isConnected = isConnected,
                    statusText = statusText,
                    sensitivity = sensitivity,
                    scrollSensitivity = scrollSensitivity,
                    mouseAccelEnabled = mouseAccelEnabled,
                    mouseAccelProfile = mouseAccelProfile,
                    naturalScroll = naturalScroll,
                    scrollPosition = scrollPosition,
                    scrollWidth = scrollWidth,
                    invertThreeFingerSwipe = invertThreeFingerSwipe,
                    isCompactMode = true,
                    onOpenDrawer = onOpenDrawer,
                    onReconnect = onReconnect,
                    onSendBinary = onSendBinary,
                    onSendJson = onSendJson,
                    onVibrate = onVibrate
                )
            }

            // BOTTOM SECTION: Mechanical Keyboard
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(bottomWeight)
            ) {
                KeyboardScreen(
                    currentTheme = currentTheme,
                    isConnected = isConnected,
                    statusText = statusText,
                    showTopBar = false,
                    stickyKeysEnabled = stickyKeysEnabled,
                    keyCasterEnabled = keyCasterEnabled,
                    keycasterState = keycasterState,
                    onOpenDrawer = onOpenDrawer,
                    onReconnect = onReconnect,
                    onOpenThemeDialog = onOpenThemeDialog,
                    onSendJson = onSendJson,
                    onVibrate = onVibrate
                )
            }
        }

        // Floating Growing Keycaster HUD for Hybrid Mode (Osu!lazer spring animation)
        KeycasterFloatingHUD(
            keycasterState = keycasterState,
            theme = currentTheme,
            isEnabled = keyCasterEnabled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        )
    }
}
