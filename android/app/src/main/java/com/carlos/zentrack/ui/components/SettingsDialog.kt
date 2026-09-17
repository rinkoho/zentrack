package com.carlos.zentrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mouse
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.carlos.zentrack.audio.ZenSoundEngine
import com.carlos.zentrack.theme.*

@Composable
fun SettingsDialog(
    show: Boolean,
    sensitivity: Float,
    scrollSensitivity: Float,
    mouseAccelEnabled: Boolean,
    mouseAccelProfile: String = com.carlos.zentrack.preferences.ZenPreferences.mouseAccelProfile,
    naturalScroll: Boolean,
    stickyKeysEnabled: Boolean,
    onSensitivityChanged: (Float) -> Unit,
    onScrollSensitivityChanged: (Float) -> Unit,
    onMouseAccelEnabledChanged: (Boolean) -> Unit,
    onMouseAccelProfileChanged: (String) -> Unit = {},
    onNaturalScrollChanged: (Boolean) -> Unit,
    onStickyKeysChanged: (Boolean) -> Unit,
    themeAnimSpeedMs: Int,
    onThemeAnimSpeedChanged: (Int) -> Unit,
    trackpadPhysicalButtonsEnabled: Boolean = com.carlos.zentrack.preferences.ZenPreferences.trackpadPhysicalButtonsEnabled,
    trackpadButtonsPosition: String = com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsPosition,
    trackpadScrollPosition: String = com.carlos.zentrack.preferences.ZenPreferences.trackpadScrollPosition,
    trackpadScrollWidth: Int = com.carlos.zentrack.preferences.ZenPreferences.trackpadScrollWidth,
    trackpadButtonsSidebarWidth: Int = com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsSidebarWidth,
    trackpadButtonsBottomHeight: Int = com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsBottomHeight,
    hapticTrackpadIntensity: Float = com.carlos.zentrack.preferences.ZenPreferences.hapticTrackpadIntensity,
    hapticKeyboardIntensity: Float = com.carlos.zentrack.preferences.ZenPreferences.hapticKeyboardIntensity,
    onTrackpadPhysicalButtonsEnabledChanged: (Boolean) -> Unit = {},
    onTrackpadButtonsPositionChanged: (String) -> Unit = {},
    onTrackpadScrollPositionChanged: (String) -> Unit = {},
    onTrackpadScrollWidthChanged: (Int) -> Unit = {},
    onTrackpadButtonsSidebarWidthChanged: (Int) -> Unit = {},
    onTrackpadButtonsBottomHeightChanged: (Int) -> Unit = {},
    onHapticTrackpadIntensityChanged: (Float) -> Unit = {},
    onHapticKeyboardIntensityChanged: (Float) -> Unit = {},
    onDismiss: () -> Unit
) {
    if (!show) return

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(16.dp),
                color = TokyoBackground,
                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = TokyoCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Mouse,
                                        contentDescription = null,
                                        tint = TokyoCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PANEL DE CONTROL ZEN",
                                    color = TokyoCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Ajustes de Motor, Trackpad y Hápticos",
                                    color = TokyoMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = TokyoMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2-Column Split Dashboard Body
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // LEFT COLUMN: NAVEGACIÓN & TRACKPAD
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Section Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = TokyoCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PUNTERO & TRACKPAD", color = TokyoCyan, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }

                            // Card 1: Sensibilidad del Cursor
                            Surface(
                                color = TokyoCard,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Sensibilidad del Cursor", color = TokyoText, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                        Surface(color = TokyoCyan.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                text = "${String.format("%.1f", sensitivity)}x",
                                                color = TokyoCyan,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                                        colors = SliderDefaults.colors(thumbColor = TokyoCyan, activeTrackColor = TokyoCyan)
                                    )
                                }
                            }

                            // Card 2: Botones Físicos (Posición + Redimensionamiento)
                            Surface(
                                color = TokyoCard,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Botones Físicos de Click", color = TokyoText, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Mostrar Izq, Centro y Der", color = TokyoMuted, fontSize = 8.sp)
                                        }
                                        Switch(
                                            checked = currentPhysicalButtonsEnabled,
                                            onCheckedChange = {
                                                currentPhysicalButtonsEnabled = it
                                                onTrackpadPhysicalButtonsEnabledChanged(it)
                                                com.carlos.zentrack.preferences.ZenPreferences.trackpadPhysicalButtonsEnabled = it
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = TokyoCyan, checkedTrackColor = TokyoCyan.copy(alpha = 0.4f))
                                        )
                                    }

                                    if (currentPhysicalButtonsEnabled) {
                                        HorizontalDivider(color = TokyoCyan.copy(alpha = 0.15f), thickness = 1.dp)

                                        Text("Posición de los Botones", color = TokyoText, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)

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
                                                    color = if (isSelected) TokyoCyan.copy(alpha = 0.2f) else TokyoBackground.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, if (isSelected) TokyoCyan else Color.Transparent),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                                                        Text(
                                                            text = label,
                                                            color = if (isSelected) TokyoCyan else TokyoText,
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
                                                Text("Altura Barra Inferior", color = TokyoText, fontSize = 9.sp)
                                                Text("${currentButtonsBottomHeight}dp", color = TokyoCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Slider(
                                                value = currentButtonsBottomHeight.toFloat(),
                                                onValueChange = {
                                                    val h = it.toInt()
                                                    currentButtonsBottomHeight = h
                                                    onTrackpadButtonsBottomHeightChanged(h)
                                                    com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsBottomHeight = h
                                                },
                                                valueRange = 34f..75f,
                                                colors = SliderDefaults.colors(thumbColor = TokyoCyan, activeTrackColor = TokyoCyan)
                                            )
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Ancho Botones Laterales", color = TokyoText, fontSize = 9.sp)
                                                Text("${currentButtonsSidebarWidth}dp", color = TokyoCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Slider(
                                                value = currentButtonsSidebarWidth.toFloat(),
                                                onValueChange = {
                                                    val w = it.toInt()
                                                    currentButtonsSidebarWidth = w
                                                    onTrackpadButtonsSidebarWidthChanged(w)
                                                    com.carlos.zentrack.preferences.ZenPreferences.trackpadButtonsSidebarWidth = w
                                                },
                                                valueRange = 55f..140f,
                                                colors = SliderDefaults.colors(thumbColor = TokyoCyan, activeTrackColor = TokyoCyan)
                                            )
                                        }
                                    }
                                }
                            }

                            // Card 3: Barra Lateral de Scroll (Posición + Ancho)
                            Surface(
                                color = TokyoCard,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("Barra Lateral de Scroll", color = TokyoText, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)

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
                                                color = if (isSelected) TokyoCyan.copy(alpha = 0.2f) else TokyoBackground.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, if (isSelected) TokyoCyan else Color.Transparent),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                                                    Text(
                                                        text = label,
                                                        color = if (isSelected) TokyoCyan else TokyoText,
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
                                        Text("Ancho Barra Scroll", color = TokyoText, fontSize = 9.sp)
                                        Text("${currentScrollWidth}dp", color = TokyoCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = currentScrollWidth.toFloat(),
                                        onValueChange = {
                                            val w = it.toInt()
                                            currentScrollWidth = w
                                            onTrackpadScrollWidthChanged(w)
                                            com.carlos.zentrack.preferences.ZenPreferences.trackpadScrollWidth = w
                                        },
                                        valueRange = 16f..45f,
                                        colors = SliderDefaults.colors(thumbColor = TokyoCyan, activeTrackColor = TokyoCyan)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Scroll Natural", color = TokyoText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Invertir dirección", color = TokyoMuted, fontSize = 7.5.sp)
                                        }
                                        Switch(
                                            checked = naturalScroll,
                                            onCheckedChange = {
                                                onNaturalScrollChanged(it)
                                                com.carlos.zentrack.preferences.ZenPreferences.naturalScroll = it
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = TokyoCyan, checkedTrackColor = TokyoCyan.copy(alpha = 0.4f))
                                        )
                                    }
                                }
                            }
                        }

                        // RIGHT COLUMN: HÁPTICOS & AUDIO MECÁNICO & SISTEMA
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Section Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Vibration, contentDescription = null, tint = TokyoCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("HÁPTICOS & SISTEMA", color = TokyoCyan, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }

                            // Card 1: Intensidad de Vibración Háptica
                            Surface(
                                color = TokyoCard,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("Intensidad de Vibración", color = TokyoText, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Trackpad / Gestos / Scroll", color = TokyoText, fontSize = 9.sp)
                                        Text("${(currentHapticTrackpad * 100).toInt()}%", color = TokyoCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = currentHapticTrackpad,
                                        onValueChange = {
                                            currentHapticTrackpad = it
                                            onHapticTrackpadIntensityChanged(it)
                                            com.carlos.zentrack.preferences.ZenPreferences.hapticTrackpadIntensity = it
                                        },
                                        valueRange = 0f..1.5f,
                                        colors = SliderDefaults.colors(thumbColor = TokyoCyan, activeTrackColor = TokyoCyan)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Teclado / Botones / Gamepad", color = TokyoText, fontSize = 9.sp)
                                        Text("${(currentHapticKeyboard * 100).toInt()}%", color = TokyoCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = currentHapticKeyboard,
                                        onValueChange = {
                                            currentHapticKeyboard = it
                                            onHapticKeyboardIntensityChanged(it)
                                            com.carlos.zentrack.preferences.ZenPreferences.hapticKeyboardIntensity = it
                                        },
                                        valueRange = 0f..1.5f,
                                        colors = SliderDefaults.colors(thumbColor = TokyoCyan, activeTrackColor = TokyoCyan)
                                    )
                                }
                            }

                            // Card 2: Audio Mecánico
                            Surface(
                                color = TokyoCard,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Audio Mecánico <1ms", color = TokyoText, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                        Switch(
                                            checked = keySoundEnabled,
                                            onCheckedChange = {
                                                keySoundEnabled = it
                                                ZenSoundEngine.setEnabled(it)
                                                com.carlos.zentrack.preferences.ZenPreferences.keySoundEnabled = it
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = TokyoCyan, checkedTrackColor = TokyoCyan.copy(alpha = 0.4f))
                                        )
                                    }

                                    if (keySoundEnabled) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick = { dropdownExpanded = true },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.4f)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = profiles.find { it.first == soundProfile }?.second ?: soundProfile,
                                                    color = TokyoCyan,
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
                                                        text = { Text(label, fontSize = 10.sp) },
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
                                    }
                                }
                            }

                            // Card 3: Teclas Adhesivas & Sync Tema
                            Surface(
                                color = TokyoCard,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Teclas Adhesivas (Sticky)", color = TokyoText, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Ctrl/Alt/Shift en 1 toque", color = TokyoMuted, fontSize = 8.sp)
                                        }
                                        Switch(
                                            checked = stickyKeysEnabled,
                                            onCheckedChange = {
                                                onStickyKeysChanged(it)
                                                com.carlos.zentrack.preferences.ZenPreferences.stickyKeysEnabled = it
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = TokyoCyan, checkedTrackColor = TokyoCyan.copy(alpha = 0.4f))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TokyoCyan),
                            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("APLICAR Y CERRAR", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
