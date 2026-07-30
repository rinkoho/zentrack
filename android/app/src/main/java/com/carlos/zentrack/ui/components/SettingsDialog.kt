package com.carlos.zentrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
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
    naturalScroll: Boolean,
    onSensitivityChanged: (Float) -> Unit,
    onScrollSensitivityChanged: (Float) -> Unit,
    onMouseAccelEnabledChanged: (Boolean) -> Unit,
    onNaturalScrollChanged: (Boolean) -> Unit,
    syncTheme: Boolean,
    onSyncThemeChanged: (Boolean) -> Unit,
    themeAnimSpeedMs: Int,
    onThemeAnimSpeedChanged: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    var keySoundEnabled by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.keySoundEnabled) }
    var soundVolume by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.soundVolume) }
    var soundProfile by remember { mutableStateOf(com.carlos.zentrack.preferences.ZenPreferences.soundProfile) }
    var dropdownExpanded by remember { mutableStateOf(false) }

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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets(0, 0, 0, 0)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.76f)
                    .fillMaxHeight(0.80f),
                shape = RoundedCornerShape(20.dp),
                color = TokyoCard,
                border = BorderStroke(1.5.dp, TokyoCyan.copy(alpha = 0.45f)),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Header Bar (Centered Title + Close)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = TokyoCyan.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = TokyoCyan,
                                    modifier = Modifier.padding(6.dp).size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PANEL DE CONFIGURACIÓN",
                                    color = TokyoCyan,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "ZenTrack Engine & Customization",
                                    color = TokyoMuted,
                                    fontSize = 9.5.sp
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TokyoMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2-Column Balanced Grid Layout
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // LEFT COLUMN: TRACKPAD & ANIMATIONS
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Section Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mouse, contentDescription = null, tint = TokyoCyan, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("NAVEGACIÓN & INTERFAZ", color = TokyoCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Card 1: Sensibilidad Trackpad
                            Surface(
                                color = TokyoBackground.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Sensibilidad Cursor", color = TokyoText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        Surface(color = TokyoCyan.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "${String.format("%.1f", sensitivity)}x",
                                                color = TokyoCyan,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
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

                            // Card 2: Aceleración de Pointer (Curva Sigmoide)
                            Surface(
                                color = TokyoBackground.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Speed, contentDescription = null, tint = TokyoCyan, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Aceleración del Cursor", color = TokyoText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text("Curva Sigmoide (macOS/Gaming precision)", color = TokyoMuted, fontSize = 9.sp)
                                    }
                                    Switch(
                                        checked = mouseAccelEnabled,
                                        onCheckedChange = {
                                            onMouseAccelEnabledChanged(it)
                                            com.carlos.zentrack.preferences.ZenPreferences.mouseAccelEnabled = it
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = TokyoCyan, checkedTrackColor = TokyoCyan.copy(alpha = 0.4f))
                                    )
                                }
                            }

                            // Card 3: Sensibilidad de Scroll
                            Surface(
                                color = TokyoBackground.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Sensibilidad Desplazamiento", color = TokyoText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        Surface(color = TokyoCyan.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "${String.format("%.1f", scrollSensitivity)}x",
                                                color = TokyoCyan,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Slider(
                                        value = scrollSensitivity,
                                        onValueChange = {
                                            onScrollSensitivityChanged(it)
                                            com.carlos.zentrack.preferences.ZenPreferences.scrollSensitivity = it
                                        },
                                        valueRange = 0.5f..3.0f,
                                        colors = SliderDefaults.colors(thumbColor = TokyoCyan, activeTrackColor = TokyoCyan)
                                    )
                                }
                            }

                            // Card 2: Scroll Natural
                            Surface(
                                color = TokyoBackground.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Scroll Natural", color = TokyoText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Invertir dirección de desplazamiento", color = TokyoMuted, fontSize = 9.sp)
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

                            // Card 3: Velocidad Transición Hyprland
                            Surface(
                                color = TokyoBackground.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Speed, contentDescription = null, tint = TokyoCyan, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Transición Hyprland", color = TokyoText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Surface(color = TokyoCyan.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "${themeAnimSpeedMs}ms",
                                                color = TokyoCyan,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text("Suavizado de cambio de color entre temas", color = TokyoMuted, fontSize = 9.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Slider(
                                        value = themeAnimSpeedMs.toFloat(),
                                        onValueChange = {
                                            val speed = it.toInt()
                                            onThemeAnimSpeedChanged(speed)
                                            com.carlos.zentrack.preferences.ZenPreferences.themeAnimSpeedMs = speed
                                        },
                                        valueRange = 100f..1000f,
                                        colors = SliderDefaults.colors(thumbColor = TokyoCyan, activeTrackColor = TokyoCyan)
                                    )
                                }
                            }
                        }

                        // RIGHT COLUMN: AUDIO & RICE SYNC
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Section Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = TokyoCyan, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AUDIO MECÁNICO & RICE SYNC", color = TokyoCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Card 1: Sonido de Teclas & Perfiles
                            Surface(
                                color = TokyoBackground.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Audio Mecánico <1ms", color = TokyoText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
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
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.4f)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = profiles.find { it.first == soundProfile }?.second ?: soundProfile,
                                                    color = TokyoCyan,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = dropdownExpanded,
                                                onDismissRequest = { dropdownExpanded = false }
                                            ) {
                                                profiles.forEach { (id, label) ->
                                                    DropdownMenuItem(
                                                        text = { Text(label, fontSize = 11.sp) },
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
                                            Text("Volumen Audio", color = TokyoText, fontSize = 11.sp)
                                            Text("${(soundVolume * 100).toInt()}%", color = TokyoCyan, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Slider(
                                            value = soundVolume,
                                            onValueChange = {
                                                soundVolume = it
                                                ZenSoundEngine.setVolume(it)
                                                com.carlos.zentrack.preferences.ZenPreferences.soundVolume = it
                                            },
                                            valueRange = 0f..1f,
                                            colors = SliderDefaults.colors(thumbColor = TokyoCyan, activeTrackColor = TokyoCyan)
                                        )
                                    }
                                }
                            }

                            // Card 2: PC Rice Auto-Sync
                            Surface(
                                color = TokyoBackground.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, TokyoCyan.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Sync, contentDescription = null, tint = TokyoCyan, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sincronizar Tema PC", color = TokyoText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text("Auto-sync con gh0stzk dotfiles rices", color = TokyoMuted, fontSize = 9.sp)
                                    }
                                    Switch(
                                        checked = syncTheme,
                                        onCheckedChange = onSyncThemeChanged,
                                        colors = SwitchDefaults.colors(checkedThumbColor = TokyoCyan, checkedTrackColor = TokyoCyan.copy(alpha = 0.4f))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Action Button (Centered)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TokyoCyan),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("APLICAR Y CERRAR", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
