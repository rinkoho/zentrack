package com.carlos.zentrack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.carlos.zentrack.theme.*
import com.carlos.zentrack.ui.components.SettingsDialog
import com.carlos.zentrack.ui.components.ThemeSelectionDialog
import com.carlos.zentrack.ui.components.rememberKeycasterState
import com.carlos.zentrack.vision.ui.VisionScreen

@Composable
fun MainContainerScreen(
    isConnected: Boolean,
    statusText: String,
    activeTheme: ZenThemeConfig,
    onThemeChanged: (ZenThemeConfig) -> Unit,
    syncTheme: Boolean,
    onSyncThemeChanged: (Boolean) -> Unit,
    onReconnect: () -> Unit,
    onSendBinary: (Short, Int, Int) -> Unit,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit,
    onVibrateKeyboard: (Long) -> Unit = onVibrate
) {
    var activeAppMode by remember { mutableStateOf("Trackpad") } // "Trackpad", "Keyboard", "Hybrid", or "Settings"
    var previousAppMode by remember { mutableStateOf("Trackpad") }
    var isSidebarExpanded by remember { mutableStateOf(false) }
    var showThemeSelectionDialog by remember { mutableStateOf(false) }
    var sensitivity by remember { mutableFloatStateOf(com.carlos.zentrack.preferences.ZenPreferences.sensitivity) }
    var scrollSensitivity by remember { mutableFloatStateOf(com.carlos.zentrack.preferences.ZenPreferences.scrollSensitivity) }
    var mouseAccelProfile by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.mouseAccelProfile) }
    var mouseAccelEnabled by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.mouseAccelEnabled) }
    var naturalScroll by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.naturalScroll) }
    var stickyKeysEnabled by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.stickyKeysEnabled) }
    var themeAnimSpeedMs by remember { mutableIntStateOf(com.carlos.zentrack.preferences.ZenPreferences.themeAnimSpeedMs) }
    var hybridKeyboardHeightRatio by remember { mutableFloatStateOf(com.carlos.zentrack.preferences.ZenPreferences.hybridKeyboardHeightRatio) }
    var keyCasterEnabled by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.keyCasterEnabled) }
    var usbAdbModeEnabled by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.usbAdbModeEnabled) }
    var invertThreeFingerSwipe by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.invertThreeFingerSwipe) }
    var trackpadPhysicalButtonsEnabled by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.trackpadPhysicalButtonsEnabled) }
    var trackpadButtonsPosition by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsPosition) }
    var trackpadScrollPosition by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.trackpadScrollPosition) }
    var trackpadScrollWidth by remember { mutableIntStateOf(com.carlos.zentrack.preferences.ZenPreferences.trackpadScrollWidth) }
    var trackpadButtonsSidebarWidth by remember { mutableIntStateOf(com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsSidebarWidth) }
    var trackpadButtonsBottomHeight by remember { mutableIntStateOf(com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsBottomHeight) }
    var hapticTrackpadIntensity by remember { mutableFloatStateOf(com.carlos.zentrack.preferences.ZenPreferences.hapticTrackpadIntensity) }
    var hapticKeyboardIntensity by remember { mutableFloatStateOf(com.carlos.zentrack.preferences.ZenPreferences.hapticKeyboardIntensity) }

    val keycasterState = rememberKeycasterState()

    // Smooth Hyprland-style Geometric Color Transition
    val animatedTheme = rememberAnimatedZenTheme(activeTheme, durationMs = themeAnimSpeedMs)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedTheme.background)
    ) {
        // LAYER 0: BASE SCREEN CONTENT
        Box(modifier = Modifier.fillMaxSize()) {
            when (activeAppMode) {
                "Trackpad" -> {
                    TrackpadScreen(
                        currentTheme = animatedTheme,
                        isConnected = isConnected,
                        statusText = statusText,
                        sensitivity = sensitivity,
                        scrollSensitivity = scrollSensitivity,
                        mouseAccelEnabled = mouseAccelEnabled,
                        mouseAccelProfile = mouseAccelProfile,
                        naturalScroll = naturalScroll,
                        invertThreeFingerSwipe = invertThreeFingerSwipe,
                        physicalButtonsEnabled = trackpadPhysicalButtonsEnabled,
                        buttonsPosition = trackpadButtonsPosition,
                        scrollPosition = trackpadScrollPosition,
                        scrollWidth = trackpadScrollWidth,
                        buttonsSidebarWidth = trackpadButtonsSidebarWidth,
                        buttonsBottomHeight = trackpadButtonsBottomHeight,
                        onOpenDrawer = { isSidebarExpanded = true },
                        onReconnect = onReconnect,
                        onSendBinary = onSendBinary,
                        onSendJson = onSendJson,
                        onVibrate = onVibrate
                    )
                }
                "Keyboard" -> {
                    KeyboardScreen(
                        currentTheme = animatedTheme,
                        isConnected = isConnected,
                        statusText = statusText,
                        stickyKeysEnabled = stickyKeysEnabled,
                        keyCasterEnabled = keyCasterEnabled,
                        keycasterState = keycasterState,
                        onOpenDrawer = { isSidebarExpanded = true },
                        onReconnect = onReconnect,
                        onOpenThemeDialog = { showThemeSelectionDialog = true },
                        onSendJson = onSendJson,
                        onVibrate = onVibrateKeyboard
                    )
                }
                "Settings" -> {
                    SettingsScreen(
                        currentTheme = animatedTheme,
                        sensitivity = sensitivity,
                        scrollSensitivity = scrollSensitivity,
                        mouseAccelEnabled = mouseAccelEnabled,
                        mouseAccelProfile = mouseAccelProfile,
                        naturalScroll = naturalScroll,
                        stickyKeysEnabled = stickyKeysEnabled,
                        syncTheme = syncTheme,
                        themeAnimSpeedMs = themeAnimSpeedMs,
                        hybridKeyboardHeightRatio = hybridKeyboardHeightRatio,
                        keyCasterEnabled = keyCasterEnabled,
                        usbAdbModeEnabled = usbAdbModeEnabled,
                        invertThreeFingerSwipe = invertThreeFingerSwipe,
                        trackpadPhysicalButtonsEnabled = trackpadPhysicalButtonsEnabled,
                        trackpadButtonsPosition = trackpadButtonsPosition,
                        trackpadScrollPosition = trackpadScrollPosition,
                        trackpadScrollWidth = trackpadScrollWidth,
                        trackpadButtonsSidebarWidth = trackpadButtonsSidebarWidth,
                        trackpadButtonsBottomHeight = trackpadButtonsBottomHeight,
                        hapticTrackpadIntensity = hapticTrackpadIntensity,
                        hapticKeyboardIntensity = hapticKeyboardIntensity,
                        onSensitivityChanged = { sensitivity = it },
                        onScrollSensitivityChanged = { scrollSensitivity = it },
                        onMouseAccelEnabledChanged = { mouseAccelEnabled = it },
                        onMouseAccelProfileChanged = { 
                            mouseAccelProfile = it
                            mouseAccelEnabled = (it != "none")
                        },
                        onNaturalScrollChanged = { naturalScroll = it },
                        onStickyKeysChanged = { stickyKeysEnabled = it },
                        onSyncThemeChanged = onSyncThemeChanged,
                        onThemeAnimSpeedChanged = { themeAnimSpeedMs = it },
                        onHybridKeyboardHeightRatioChanged = { hybridKeyboardHeightRatio = it },
                        onKeyCasterEnabledChanged = { keyCasterEnabled = it },
                        onUsbAdbModeChanged = { 
                            usbAdbModeEnabled = it
                            onReconnect()
                        },
                        onInvertThreeFingerSwipeChanged = { invertThreeFingerSwipe = it },
                        onTrackpadPhysicalButtonsEnabledChanged = { trackpadPhysicalButtonsEnabled = it },
                        onTrackpadButtonsPositionChanged = { trackpadButtonsPosition = it },
                        onTrackpadScrollPositionChanged = { trackpadScrollPosition = it },
                        onTrackpadScrollWidthChanged = { trackpadScrollWidth = it },
                        onTrackpadButtonsSidebarWidthChanged = { trackpadButtonsSidebarWidth = it },
                        onTrackpadButtonsBottomHeightChanged = { trackpadButtonsBottomHeight = it },
                        onHapticTrackpadIntensityChanged = { hapticTrackpadIntensity = it },
                        onHapticKeyboardIntensityChanged = { hapticKeyboardIntensity = it },
                        onBack = { activeAppMode = previousAppMode }
                    )
                }
                "Gaming", "Gamepad" -> {
                    GamepadScreen(
                        currentTheme = animatedTheme,
                        isConnected = isConnected,
                        statusText = statusText,
                        onOpenDrawer = { isSidebarExpanded = true },
                        onReconnect = onReconnect,
                        onSendBinary = onSendBinary,
                        onSendJson = onSendJson,
                        onVibrate = onVibrateKeyboard
                    )
                }
                "Vision", "ZenVision" -> {
                    VisionScreen(
                        currentTheme = animatedTheme,
                        isConnected = isConnected,
                        statusText = statusText,
                        onOpenDrawer = { isSidebarExpanded = true },
                        onReconnect = onReconnect,
                        onSendBinary = onSendBinary,
                        onSendJson = onSendJson,
                        onVibrate = onVibrate
                    )
                }
                else -> { // "Hybrid"
                    HybridScreen(
                        currentTheme = animatedTheme,
                        isConnected = isConnected,
                        statusText = statusText,
                        sensitivity = sensitivity,
                        scrollSensitivity = scrollSensitivity,
                        mouseAccelEnabled = mouseAccelEnabled,
                        mouseAccelProfile = mouseAccelProfile,
                        naturalScroll = naturalScroll,
                        scrollPosition = trackpadScrollPosition,
                        scrollWidth = trackpadScrollWidth,
                        invertThreeFingerSwipe = invertThreeFingerSwipe,
                        stickyKeysEnabled = stickyKeysEnabled,
                        hybridKeyboardHeightRatio = hybridKeyboardHeightRatio,
                        keyCasterEnabled = keyCasterEnabled,
                        keycasterState = keycasterState,
                        onOpenDrawer = { isSidebarExpanded = true },
                        onReconnect = onReconnect,
                        onOpenThemeDialog = { showThemeSelectionDialog = true },
                        onSendBinary = onSendBinary,
                        onSendJson = onSendJson,
                        onVibrate = onVibrate
                    )
                }
            }
        }

        // LAYER 1: SCRIM BACKDROP
        if (isSidebarExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable {
                        isSidebarExpanded = false
                        onVibrate(10L)
                    }
            )
        }

        // LAYER 2: OVERLAY SIDEBAR DRAWER
        AnimatedVisibility(
            visible = isSidebarExpanded,
            enter = expandHorizontally(),
            exit = shrinkHorizontally(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Surface(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight(),
                color = animatedTheme.surface,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Title Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ZEN-TRACK",
                                color = animatedTheme.primaryAccent,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            IconButton(onClick = {
                                isSidebarExpanded = false
                                onVibrate(10L)
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuOpen,
                                    contentDescription = "Collapse",
                                    tint = animatedTheme.textMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Connection Status Card
                        Surface(
                            color = animatedTheme.card,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onReconnect() }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (isConnected) animatedTheme.primaryAccent else Color(0xFFF7768E),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = statusText,
                                    color = animatedTheme.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "MODO DE CONTROL",
                            color = animatedTheme.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Control Mode Selector (Organized Vertically)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(animatedTheme.card, RoundedCornerShape(10.dp))
                                .padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 1. Pad Tactil
                            val isPad = activeAppMode == "Trackpad"
                            Surface(
                                color = if (isPad) animatedTheme.primaryAccent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeAppMode = "Trackpad"
                                        onVibrate(15L)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.TouchApp,
                                        contentDescription = null,
                                        tint = if (isPad) Color.Black else animatedTheme.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Pad Táctil",
                                        color = if (isPad) Color.Black else animatedTheme.textPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 2. Teclado 65%
                            val isKb = activeAppMode == "Keyboard"
                            Surface(
                                color = if (isKb) animatedTheme.primaryAccent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeAppMode = "Keyboard"
                                        onVibrate(15L)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Keyboard,
                                        contentDescription = null,
                                        tint = if (isKb) Color.Black else animatedTheme.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Teclado 65%",
                                        color = if (isKb) Color.Black else animatedTheme.textPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 3. Vista Híbrida
                            val isHybrid = activeAppMode == "Hybrid"
                            Surface(
                                color = if (isHybrid) animatedTheme.primaryAccent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeAppMode = "Hybrid"
                                        onVibrate(15L)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Dashboard,
                                        contentDescription = null,
                                        tint = if (isHybrid) Color.Black else animatedTheme.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Híbrido (Dual)",
                                        color = if (isHybrid) Color.Black else animatedTheme.textPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 4. Mando Gaming
                            val isGaming = activeAppMode == "Gaming" || activeAppMode == "Gamepad"
                            Surface(
                                color = if (isGaming) animatedTheme.primaryAccent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeAppMode = "Gaming"
                                        onVibrate(15L)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.SportsEsports,
                                        contentDescription = null,
                                        tint = if (isGaming) Color.Black else animatedTheme.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Mando Gaming",
                                        color = if (isGaming) Color.Black else animatedTheme.textPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 5. ZenVision (Control Espacial)
                            val isVision = activeAppMode == "Vision" || activeAppMode == "ZenVision"
                            Surface(
                                color = if (isVision) animatedTheme.primaryAccent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeAppMode = "Vision"
                                        onVibrate(15L)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = if (isVision) Color.Black else animatedTheme.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "ZenVisión (Spatial)",
                                        color = if (isVision) Color.Black else animatedTheme.textPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Theme Dialog Button (Uncluttered Drawer)
                        Text(
                            text = "APARIENCIA & TEMAS",
                            color = animatedTheme.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            color = animatedTheme.card,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, animatedTheme.primaryAccent.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showThemeSelectionDialog = true
                                    isSidebarExpanded = false
                                    onVibrate(15L)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = "Temas",
                                    tint = animatedTheme.primaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Temas & PC Rices",
                                        color = animatedTheme.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = activeTheme.name,
                                        color = animatedTheme.primaryAccent,
                                        fontSize = 9.5.sp
                                    )
                                }
                            }
                        }
                    }

                    // Settings Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                previousAppMode = activeAppMode
                                activeAppMode = "Settings"
                                isSidebarExpanded = false
                                onVibrate(15L)
                            }
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Ajustes",
                                tint = animatedTheme.primaryAccent
                            )
                        }
                    }
                }
            }
        }

        // Theme Selection Dialog Modal
        ThemeSelectionDialog(
            show = showThemeSelectionDialog,
            activeTheme = animatedTheme,
            onThemeSelected = { newTheme ->
                onThemeChanged(newTheme)
                showThemeSelectionDialog = false
            },
            onDismiss = { showThemeSelectionDialog = false }
        )
    }
}
