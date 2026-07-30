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
import com.carlos.zentrack.theme.*
import com.carlos.zentrack.ui.components.SettingsDialog
import com.carlos.zentrack.ui.components.ThemeSelectionDialog

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
    onVibrate: (Long) -> Unit
) {
    var activeAppMode by remember { mutableStateOf("Trackpad") } // "Trackpad" or "Keyboard"
    var isSidebarExpanded by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showThemeSelectionDialog by remember { mutableStateOf(false) }
    var sensitivity by remember { mutableFloatStateOf(com.carlos.zentrack.preferences.ZenPreferences.sensitivity) }
    var scrollSensitivity by remember { mutableFloatStateOf(com.carlos.zentrack.preferences.ZenPreferences.scrollSensitivity) }
    var mouseAccelEnabled by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.mouseAccelEnabled) }
    var naturalScroll by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.naturalScroll) }
    var themeAnimSpeedMs by remember { mutableIntStateOf(com.carlos.zentrack.preferences.ZenPreferences.themeAnimSpeedMs) }

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
                        naturalScroll = naturalScroll,
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
                        onOpenDrawer = { isSidebarExpanded = true },
                        onReconnect = onReconnect,
                        onOpenThemeDialog = { showThemeSelectionDialog = true },
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
                        naturalScroll = naturalScroll,
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
                    Column {
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

                        // Control Mode Selector (Pad | Teclado | Híbrido)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(animatedTheme.card, RoundedCornerShape(8.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Surface(
                                color = if (activeAppMode == "Trackpad") animatedTheme.primaryAccent else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        activeAppMode = "Trackpad"
                                        onVibrate(15L)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.TouchApp,
                                        contentDescription = null,
                                        tint = if (activeAppMode == "Trackpad") Color.Black else animatedTheme.textPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "Pad",
                                        color = if (activeAppMode == "Trackpad") Color.Black else animatedTheme.textPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                color = if (activeAppMode == "Keyboard") animatedTheme.primaryAccent else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        activeAppMode = "Keyboard"
                                        onVibrate(15L)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Keyboard,
                                        contentDescription = null,
                                        tint = if (activeAppMode == "Keyboard") Color.Black else animatedTheme.textPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "Teclado",
                                        color = if (activeAppMode == "Keyboard") Color.Black else animatedTheme.textPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                color = if (activeAppMode == "Hybrid") animatedTheme.primaryAccent else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .clickable {
                                        activeAppMode = "Hybrid"
                                        onVibrate(15L)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Dashboard,
                                        contentDescription = null,
                                        tint = if (activeAppMode == "Hybrid") Color.Black else animatedTheme.textPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "Híbrido",
                                        color = if (activeAppMode == "Hybrid") Color.Black else animatedTheme.textPrimary,
                                        fontSize = 10.sp,
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
                                showSettingsDialog = true
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

        // Settings Dialog Modal
        SettingsDialog(
            show = showSettingsDialog,
            sensitivity = sensitivity,
            scrollSensitivity = scrollSensitivity,
            mouseAccelEnabled = mouseAccelEnabled,
            naturalScroll = naturalScroll,
            onSensitivityChanged = { sensitivity = it },
            onScrollSensitivityChanged = { scrollSensitivity = it },
            onMouseAccelEnabledChanged = { mouseAccelEnabled = it },
            onNaturalScrollChanged = { naturalScroll = it },
            syncTheme = syncTheme,
            onSyncThemeChanged = onSyncThemeChanged,
            themeAnimSpeedMs = themeAnimSpeedMs,
            onThemeAnimSpeedChanged = { themeAnimSpeedMs = it },
            onDismiss = { showSettingsDialog = false }
        )
    }
}
