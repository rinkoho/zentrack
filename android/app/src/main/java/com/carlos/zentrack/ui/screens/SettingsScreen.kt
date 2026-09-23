package com.carlos.zentrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.zentrack.audio.ZenSoundEngine
import com.carlos.zentrack.theme.ZenThemeConfig
import com.carlos.zentrack.ui.components.BatteryBadge

@Composable
fun SettingsScreen(
    currentTheme: ZenThemeConfig,
    sensitivity: Float,
    scrollSensitivity: Float,
    mouseAccelEnabled: Boolean,
    mouseAccelProfile: String = com.carlos.zentrack.preferences.ZenPreferences.mouseAccelProfile,
    naturalScroll: Boolean,
    stickyKeysEnabled: Boolean,
    themeAnimSpeedMs: Int,
    hybridKeyboardHeightRatio: Float = com.carlos.zentrack.preferences.ZenPreferences.hybridKeyboardHeightRatio,
    keyCasterEnabled: Boolean = com.carlos.zentrack.preferences.ZenPreferences.keyCasterEnabled,
    usbAdbModeEnabled: Boolean = com.carlos.zentrack.preferences.ZenPreferences.usbAdbModeEnabled,
    invertThreeFingerSwipe: Boolean = com.carlos.zentrack.preferences.ZenPreferences.invertThreeFingerSwipe,
    trackpadPhysicalButtonsEnabled: Boolean = com.carlos.zentrack.preferences.ZenPreferences.trackpadPhysicalButtonsEnabled,
    trackpadButtonsPosition: String = com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsPosition,
    trackpadScrollPosition: String = com.carlos.zentrack.preferences.ZenPreferences.trackpadScrollPosition,
    trackpadScrollWidth: Int = com.carlos.zentrack.preferences.ZenPreferences.trackpadScrollWidth,
    trackpadButtonsSidebarWidth: Int = com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsSidebarWidth,
    trackpadButtonsBottomHeight: Int = com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsBottomHeight,
    hapticTrackpadIntensity: Float = com.carlos.zentrack.preferences.ZenPreferences.hapticTrackpadIntensity,
    hapticKeyboardIntensity: Float = com.carlos.zentrack.preferences.ZenPreferences.hapticKeyboardIntensity,
    onSensitivityChanged: (Float) -> Unit,
    onScrollSensitivityChanged: (Float) -> Unit,
    onMouseAccelEnabledChanged: (Boolean) -> Unit,
    onMouseAccelProfileChanged: (String) -> Unit = {},
    onNaturalScrollChanged: (Boolean) -> Unit,
    onStickyKeysChanged: (Boolean) -> Unit,
    onThemeAnimSpeedChanged: (Int) -> Unit,
    onHybridKeyboardHeightRatioChanged: (Float) -> Unit = {},
    onKeyCasterEnabledChanged: (Boolean) -> Unit = {},
    onUsbAdbModeChanged: (Boolean) -> Unit = {},
    onInvertThreeFingerSwipeChanged: (Boolean) -> Unit = {},
    onTrackpadPhysicalButtonsEnabledChanged: (Boolean) -> Unit = {},
    onTrackpadButtonsPositionChanged: (String) -> Unit = {},
    onTrackpadScrollPositionChanged: (String) -> Unit = {},
    onTrackpadScrollWidthChanged: (Int) -> Unit = {},
    onTrackpadButtonsSidebarWidthChanged: (Int) -> Unit = {},
    onTrackpadButtonsBottomHeightChanged: (Int) -> Unit = {},
    onHapticTrackpadIntensityChanged: (Float) -> Unit = {},
    onHapticKeyboardIntensityChanged: (Float) -> Unit = {},
    onOpenBluetoothDialog: () -> Unit = {},
    onBack: () -> Unit
) {
    var keySoundEnabled by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.keySoundEnabled) }
    var soundVolume by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.soundVolume) }
    var soundProfile by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.soundProfile) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var currentPhysicalButtonsEnabled by remember { mutableStateOf(trackpadPhysicalButtonsEnabled) }
    var currentButtonsPosition by remember { mutableStateOf(trackpadButtonsPosition) }
    var currentScrollPosition by remember { mutableStateOf(trackpadScrollPosition) }
    var currentScrollWidth by remember { mutableIntStateOf(trackpadScrollWidth) }
    var currentButtonsSidebarWidth by remember { mutableIntStateOf(trackpadButtonsSidebarWidth) }
    var currentButtonsBottomHeight by remember { mutableIntStateOf(trackpadButtonsBottomHeight) }
    var currentHapticTrackpad by remember { mutableFloatStateOf(hapticTrackpadIntensity) }
    var currentHapticKeyboard by remember { mutableFloatStateOf(hapticKeyboardIntensity) }

    val profiles = listOf(
        "cherry-blue" to "Cherry MX Blue (Clicky)",
        "cherry-red" to "Cherry MX Red (Linear Thock)",
        "chocolate" to "Chocolate Crujiente (Wood)",
        "box-navy" to "Kailh Box Navy (Heavy)",
        "buckling-spring" to "Buckling Spring (IBM M)",
        "topre" to "Topre Capacitive (Plop)",
        "membrane" to "Membrana Silenciosa",
        "bubble-wrap" to "Bubble Wrap (Pop)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.background)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // TOP HEADER BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.clickable { onBack() },
                shape = RoundedCornerShape(8.dp),
                color = currentTheme.card,
                border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = currentTheme.primaryAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VOLVER",
                        color = currentTheme.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = currentTheme.primaryAccent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "PANEL DE AJUSTES",
                        color = currentTheme.primaryAccent,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.5.sp,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Configuración Global & Motor ZenTrack",
                        color = currentTheme.textMuted,
                        fontSize = 8.sp
                    )
                }
            }

            BatteryBadge(currentTheme = currentTheme)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // MAIN CONTENT: 3-Column Balanced Dashboard
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // COLUMN 1: MOTOR DE CURSOR & ACELERACIÓN & STICKY KEYS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mouse, contentDescription = null, tint = currentTheme.primaryAccent, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PUNTERO & ACELERACIÓN", color = currentTheme.primaryAccent, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }

                // Sensibilidad Cursor
                Surface(
                    color = currentTheme.card.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sensibilidad Cursor", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Surface(color = currentTheme.primaryAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "${String.format("%.1f", sensitivity)}x",
                                    color = currentTheme.primaryAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Slider(
                            value = sensitivity,
                            onValueChange = {
                                onSensitivityChanged(it)
                                com.carlos.zentrack.preferences.ZenPreferences.sensitivity = it
                            },
                            valueRange = 0.5f..3.0f,
                            colors = SliderDefaults.colors(thumbColor = currentTheme.primaryAccent, activeTrackColor = currentTheme.primaryAccent)
                        )
                    }
                }

                // Algoritmo de Aceleración
                Surface(
                    color = currentTheme.card.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = currentTheme.primaryAccent, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Algoritmo de Aceleración", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }

                        val accelProfiles = listOf(
                            Triple("none", "Sin Aceleración (1:1)", "Direct Input 100% lineal"),
                            Triple("linear_offset_cap", "Linear + Offset + Cap", "Pro RawAccel: Offset base + Cap flicks"),
                            Triple("exponential", "Exponencial", "Curva sigmoidea progresiva original")
                        )

                        accelProfiles.forEach { (id, title, description) ->
                            val isSelected = mouseAccelProfile == id
                            Surface(
                                onClick = {
                                    onMouseAccelProfileChanged(id)
                                    onMouseAccelEnabledChanged(id != "none")
                                    com.carlos.zentrack.preferences.ZenPreferences.mouseAccelProfile = id
                                    com.carlos.zentrack.preferences.ZenPreferences.mouseAccelEnabled = (id != "none")
                                },
                                color = if (isSelected) currentTheme.primaryAccent.copy(alpha = 0.18f) else currentTheme.surface.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(5.dp),
                                border = BorderStroke(1.dp, if (isSelected) currentTheme.primaryAccent else Color.Transparent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            color = if (isSelected) currentTheme.primaryAccent else currentTheme.textPrimary,
                                            fontSize = 9.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = description,
                                            color = currentTheme.textMuted,
                                            fontSize = 7.5.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = currentTheme.primaryAccent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Sticky Keys
                Surface(
                    color = currentTheme.card.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Teclas Adhesivas (Sticky)", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text("Acumular Ctrl/Alt/Shift/Super/Fn en 1 toque", color = currentTheme.textMuted, fontSize = 7.5.sp)
                        }
                        Switch(
                            checked = stickyKeysEnabled,
                            onCheckedChange = {
                                onStickyKeysChanged(it)
                                com.carlos.zentrack.preferences.ZenPreferences.stickyKeysEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = currentTheme.primaryAccent, checkedTrackColor = currentTheme.primaryAccent.copy(alpha = 0.4f))
                        )
                    }
                }

                // Polling Rate (Hz)
                Surface(
                    color = currentTheme.card.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = currentTheme.primaryAccent, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Polling Rate (Hz)", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        val hzOptions = listOf(
                            500 to "Ilimitado (Max HW)",
                            250 to "Alto (Equilibrado)",
                            125 to "Medio (Red Inestable)",
                            60 to "Bajo (Ahorro Batería)"
                        )
                        var currentHz by remember { mutableIntStateOf(com.carlos.zentrack.preferences.ZenPreferences.networkHz) }

                        hzOptions.forEach { (hzValue, label) ->
                            val isSelected = currentHz == hzValue
                            Surface(
                                onClick = {
                                    currentHz = hzValue
                                    com.carlos.zentrack.preferences.ZenPreferences.networkHz = hzValue
                                },
                                color = if (isSelected) currentTheme.primaryAccent.copy(alpha = 0.18f) else currentTheme.surface.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(5.dp),
                                border = BorderStroke(1.dp, if (isSelected) currentTheme.primaryAccent else Color.Transparent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) currentTheme.primaryAccent else currentTheme.textPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = currentTheme.primaryAccent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "* Los Hz reales están limitados por el hardware digitalizador de tu pantalla.",
                            color = currentTheme.textMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            // COLUMN 2: DESPLAZAMIENTO, BOTONES & SCROLL
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mouse, contentDescription = null, tint = currentTheme.secondaryAccent, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DESPLAZAMIENTO & BOTONES", color = currentTheme.secondaryAccent, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }

                // Card 1: Sensibilidad de Scroll & Dirección
                Surface(
                    color = currentTheme.card.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, currentTheme.secondaryAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sensibilidad Scroll", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Surface(color = currentTheme.secondaryAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "${String.format("%.2f", scrollSensitivity)}x",
                                    color = currentTheme.secondaryAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Slider(
                            value = scrollSensitivity,
                            onValueChange = {
                                onScrollSensitivityChanged(it)
                                com.carlos.zentrack.preferences.ZenPreferences.scrollSensitivity = it
                            },
                            valueRange = 0.1f..0.9f,
                            colors = SliderDefaults.colors(thumbColor = currentTheme.secondaryAccent, activeTrackColor = currentTheme.secondaryAccent)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Scroll Natural", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("Invertir dirección de desplazamiento", color = currentTheme.textMuted, fontSize = 7.5.sp)
                            }
                            Switch(
                                checked = naturalScroll,
                                onCheckedChange = {
                                    onNaturalScrollChanged(it)
                                    com.carlos.zentrack.preferences.ZenPreferences.naturalScroll = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = currentTheme.secondaryAccent, checkedTrackColor = currentTheme.secondaryAccent.copy(alpha = 0.4f))
                            )
                        }

                        HorizontalDivider(color = currentTheme.secondaryAccent.copy(alpha = 0.15f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Invertir Gesto 3 Dedos", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("Invertir dirección de escritorios", color = currentTheme.textMuted, fontSize = 7.5.sp)
                            }
                            Switch(
                                checked = invertThreeFingerSwipe,
                                onCheckedChange = {
                                    onInvertThreeFingerSwipeChanged(it)
                                    com.carlos.zentrack.preferences.ZenPreferences.invertThreeFingerSwipe = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = currentTheme.secondaryAccent, checkedTrackColor = currentTheme.secondaryAccent.copy(alpha = 0.4f))
                            )
                        }
                    }
                }

                // Card 2: Botones Físicos de Click (Posición + Redimensionamiento)
                Surface(
                    color = currentTheme.card.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, currentTheme.secondaryAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Botones Físicos de Click", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("Mostrar clicks Izq, Centro y Der", color = currentTheme.textMuted, fontSize = 7.5.sp)
                            }
                            Switch(
                                checked = currentPhysicalButtonsEnabled,
                                onCheckedChange = {
                                    currentPhysicalButtonsEnabled = it
                                    onTrackpadPhysicalButtonsEnabledChanged(it)
                                    com.carlos.zentrack.preferences.ZenPreferences.trackpadPhysicalButtonsEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = currentTheme.secondaryAccent, checkedTrackColor = currentTheme.secondaryAccent.copy(alpha = 0.4f))
                            )
                        }

                        if (currentPhysicalButtonsEnabled) {
                            HorizontalDivider(color = currentTheme.secondaryAccent.copy(alpha = 0.15f), thickness = 1.dp)

                            Text("Posición de los Botones", color = currentTheme.textPrimary, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)

                            val positions = listOf(
                                "left" to "Izquierda",
                                "bottom" to "Abajo (Laptop)",
                                "right" to "Derecha"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                positions.forEach { (id, label) ->
                                    val isSelected = currentButtonsPosition == id
                                    Surface(
                                        onClick = {
                                            currentButtonsPosition = id
                                            onTrackpadButtonsPositionChanged(id)
                                            com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsPosition = id
                                        },
                                        color = if (isSelected) currentTheme.secondaryAccent.copy(alpha = 0.2f) else currentTheme.surface.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(5.dp),
                                        border = BorderStroke(1.dp, if (isSelected) currentTheme.secondaryAccent else Color.Transparent),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) currentTheme.secondaryAccent else currentTheme.textPrimary,
                                                fontSize = 8.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }

                            if (currentButtonsPosition == "bottom") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Altura Barra Inferior", color = currentTheme.textPrimary, fontSize = 9.5.sp)
                                    Text("${currentButtonsBottomHeight}dp", color = currentTheme.secondaryAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = currentButtonsBottomHeight.toFloat(),
                                    onValueChange = {
                                        val h = it.toInt()
                                        currentButtonsBottomHeight = h
                                        onTrackpadButtonsBottomHeightChanged(h)
                                        com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsBottomHeight = h
                                    },
                                    valueRange = 34f..100f,
                                    colors = SliderDefaults.colors(thumbColor = currentTheme.secondaryAccent, activeTrackColor = currentTheme.secondaryAccent)
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Ancho Botones Laterales", color = currentTheme.textPrimary, fontSize = 9.5.sp)
                                    Text("${currentButtonsSidebarWidth}dp", color = currentTheme.secondaryAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = currentButtonsSidebarWidth.toFloat(),
                                    onValueChange = {
                                        val w = it.toInt()
                                        currentButtonsSidebarWidth = w
                                        onTrackpadButtonsSidebarWidthChanged(w)
                                        com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsSidebarWidth = w
                                    },
                                    valueRange = 50f..160f,
                                    colors = SliderDefaults.colors(thumbColor = currentTheme.secondaryAccent, activeTrackColor = currentTheme.secondaryAccent)
                                )
                            }
                        }
                    }
                }

                // Card 3: Barra Lateral de Scroll (Posición + Ancho)
                Surface(
                    color = currentTheme.card.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, currentTheme.secondaryAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Barra Lateral de Scroll", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)

                        val scrollPositions = listOf(
                            "left" to "Izquierda",
                            "right" to "Derecha"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            scrollPositions.forEach { (id, label) ->
                                val isSelected = currentScrollPosition == id
                                Surface(
                                    onClick = {
                                        currentScrollPosition = id
                                        onTrackpadScrollPositionChanged(id)
                                        com.carlos.zentrack.preferences.ZenPreferences.trackpadScrollPosition = id
                                    },
                                    color = if (isSelected) currentTheme.secondaryAccent.copy(alpha = 0.2f) else currentTheme.surface.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(5.dp),
                                    border = BorderStroke(1.dp, if (isSelected) currentTheme.secondaryAccent else Color.Transparent),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) currentTheme.secondaryAccent else currentTheme.textPrimary,
                                            fontSize = 8.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ancho Barra de Scroll", color = currentTheme.textPrimary, fontSize = 9.5.sp)
                            Text("${currentScrollWidth}dp", color = currentTheme.secondaryAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = currentScrollWidth.toFloat(),
                            onValueChange = {
                                val w = it.toInt()
                                currentScrollWidth = w
                                onTrackpadScrollWidthChanged(w)
                                com.carlos.zentrack.preferences.ZenPreferences.trackpadScrollWidth = w
                            },
                            valueRange = 16f..75f,
                            colors = SliderDefaults.colors(thumbColor = currentTheme.secondaryAccent, activeTrackColor = currentTheme.secondaryAccent)
                        )
                    }
                }

                // Card 4: Altura Teclado Híbrido
                Surface(
                    color = currentTheme.card.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, currentTheme.secondaryAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Altura Teclado Híbrido", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("Proporción de pantalla del teclado", color = currentTheme.textMuted, fontSize = 7.5.sp)
                            }
                            Surface(color = currentTheme.secondaryAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "${(hybridKeyboardHeightRatio * 100).toInt()}%",
                                    color = currentTheme.secondaryAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Slider(
                            value = hybridKeyboardHeightRatio,
                            onValueChange = {
                                onHybridKeyboardHeightRatioChanged(it)
                                com.carlos.zentrack.preferences.ZenPreferences.hybridKeyboardHeightRatio = it
                            },
                            valueRange = 0.55f..0.70f,
                            colors = SliderDefaults.colors(thumbColor = currentTheme.secondaryAccent, activeTrackColor = currentTheme.secondaryAccent)
                        )
                    }
                }

                // Card 5: Bordes de Teclas & Botones (Toggleable)
                Surface(
                    color = currentTheme.card.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, currentTheme.secondaryAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Bordes en Teclas / Botones", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("Trazos semitransparentes en teclas y botones", color = currentTheme.textMuted, fontSize = 7.5.sp)
                            }
                            var bordersEnabled by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.keycapBordersEnabled) }
                            Switch(
                                checked = bordersEnabled,
                                onCheckedChange = {
                                    bordersEnabled = it
                                    com.carlos.zentrack.preferences.ZenPreferences.keycapBordersEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = currentTheme.secondaryAccent, checkedTrackColor = currentTheme.secondaryAccent.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }

            // COLUMN 3: AUDIO, HÁPTICOS, HYPRLAND & RICE SYNC
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = currentTheme.primaryAccent, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AUDIO, HÁPTICOS & SISTEMA", color = currentTheme.primaryAccent, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }

                // Card 1: Intensidad de Vibración Háptica
                Surface(
                    color = currentTheme.card.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = currentTheme.primaryAccent, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Intensidad de Vibración", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Trackpad / Gestos / Scroll", color = currentTheme.textPrimary, fontSize = 9.sp)
                            Text("${(currentHapticTrackpad * 100).toInt()}%", color = currentTheme.primaryAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = currentHapticTrackpad,
                            onValueChange = {
                                currentHapticTrackpad = it
                                onHapticTrackpadIntensityChanged(it)
                                com.carlos.zentrack.preferences.ZenPreferences.hapticTrackpadIntensity = it
                            },
                            valueRange = 0f..2.5f,
                            colors = SliderDefaults.colors(thumbColor = currentTheme.primaryAccent, activeTrackColor = currentTheme.primaryAccent)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Teclado / Botones / Gamepad", color = currentTheme.textPrimary, fontSize = 9.sp)
                            Text("${(currentHapticKeyboard * 100).toInt()}%", color = currentTheme.primaryAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = currentHapticKeyboard,
                            onValueChange = {
                                currentHapticKeyboard = it
                                onHapticKeyboardIntensityChanged(it)
                                com.carlos.zentrack.preferences.ZenPreferences.hapticKeyboardIntensity = it
                            },
                            valueRange = 0f..2.5f,
                            colors = SliderDefaults.colors(thumbColor = currentTheme.primaryAccent, activeTrackColor = currentTheme.primaryAccent)
                        )
                    }
                }

                // Card 2: Audio Mecánico
                Surface(
                    color = currentTheme.card.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Audio Mecánico <1ms", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = keySoundEnabled,
                                onCheckedChange = {
                                    keySoundEnabled = it
                                    ZenSoundEngine.setEnabled(it)
                                    com.carlos.zentrack.preferences.ZenPreferences.keySoundEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = currentTheme.primaryAccent, checkedTrackColor = currentTheme.primaryAccent.copy(alpha = 0.4f))
                            )
                        }

                        if (keySoundEnabled) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { dropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(5.dp),
                                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.4f)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = profiles.find { it.first == soundProfile }?.second ?: soundProfile,
                                        color = currentTheme.primaryAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    profiles.forEach { (id, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label, fontSize = 9.5.sp) },
                                            onClick = {
                                                soundProfile = id
                                                ZenSoundEngine.setProfile(id)
                                                ZenSoundEngine.playSwitchSound(true)
                                                com.carlos.zentrack.preferences.ZenPreferences.soundProfile = id
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Volumen Audio", color = currentTheme.textPrimary, fontSize = 9.5.sp)
                                Text("${(soundVolume * 100).toInt()}%", color = currentTheme.primaryAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            Slider(
                                value = soundVolume,
                                onValueChange = {
                                    soundVolume = it
                                    ZenSoundEngine.setVolume(it)
                                    com.carlos.zentrack.preferences.ZenPreferences.soundVolume = it
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = currentTheme.primaryAccent, activeTrackColor = currentTheme.primaryAccent)
                            )
                        }
                    }
                }

                // Card 3: Hyprland Transición & Rice Sync & Keycaster & USB
                Surface(
                    color = currentTheme.card.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Transición Hyprland", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Surface(color = currentTheme.primaryAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "${themeAnimSpeedMs}ms",
                                    color = currentTheme.primaryAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Slider(
                            value = themeAnimSpeedMs.toFloat(),
                            onValueChange = {
                                 val speed = it.toInt()
                                 onThemeAnimSpeedChanged(speed)
                                 com.carlos.zentrack.preferences.ZenPreferences.themeAnimSpeedMs = speed
                            },
                            valueRange = 100f..2000f,
                            colors = SliderDefaults.colors(thumbColor = currentTheme.primaryAccent, activeTrackColor = currentTheme.primaryAccent)
                        )

                        HorizontalDivider(color = currentTheme.primaryAccent.copy(alpha = 0.15f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Visualizador Keycaster", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("HUD en tiempo real & animaciones osu!", color = currentTheme.textMuted, fontSize = 7.5.sp)
                            }
                            Switch(
                                checked = keyCasterEnabled,
                                onCheckedChange = {
                                    onKeyCasterEnabledChanged(it)
                                    com.carlos.zentrack.preferences.ZenPreferences.keyCasterEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = currentTheme.primaryAccent, checkedTrackColor = currentTheme.primaryAccent.copy(alpha = 0.4f))
                            )
                        }

                        HorizontalDivider(color = currentTheme.primaryAccent.copy(alpha = 0.15f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = currentTheme.primaryAccent, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Conexión USB / ADB (Túnel)", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text("Túnel local 127.0.0.1 (latencia 0.1ms)", color = currentTheme.textMuted, fontSize = 7.5.sp)
                            }
                            Switch(
                                checked = usbAdbModeEnabled,
                                onCheckedChange = {
                                    onUsbAdbModeChanged(it)
                                    com.carlos.zentrack.preferences.ZenPreferences.usbAdbModeEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = currentTheme.primaryAccent, checkedTrackColor = currentTheme.primaryAccent.copy(alpha = 0.4f))
                            )
                        }

                        HorizontalDivider(color = currentTheme.primaryAccent.copy(alpha = 0.15f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = currentTheme.primaryAccent, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bluetooth HID Universal", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    if (com.carlos.zentrack.bluetooth.ZenInputRouter.isBluetoothConnected) {
                                        "Conectado: ${com.carlos.zentrack.bluetooth.ZenInputRouter.connectedBluetoothDeviceName} (${com.carlos.zentrack.bluetooth.ZenInputRouter.activeBluetoothSubModeName})"
                                    } else {
                                        "Smart TV, PC, Mac, iPad (0 Servidor)"
                                    },
                                    color = if (com.carlos.zentrack.bluetooth.ZenInputRouter.isBluetoothConnected) currentTheme.primaryAccent else currentTheme.textMuted,
                                    fontSize = 7.5.sp
                                )
                            }
                            Button(
                                onClick = onOpenBluetoothDialog,
                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryAccent),
                                shape = RoundedCornerShape(5.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Emparejar", color = Color.Black, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // FOOTER ACTION BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryAccent),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("VOLVER", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
