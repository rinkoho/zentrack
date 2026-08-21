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
import androidx.compose.material.icons.filled.Check
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
    syncTheme: Boolean,
    themeAnimSpeedMs: Int,
    hybridKeyboardHeightRatio: Float = com.carlos.zentrack.preferences.ZenPreferences.hybridKeyboardHeightRatio,
    keyCasterEnabled: Boolean = com.carlos.zentrack.preferences.ZenPreferences.keyCasterEnabled,
    usbAdbModeEnabled: Boolean = com.carlos.zentrack.preferences.ZenPreferences.usbAdbModeEnabled,
    invertThreeFingerSwipe: Boolean = com.carlos.zentrack.preferences.ZenPreferences.invertThreeFingerSwipe,
    onSensitivityChanged: (Float) -> Unit,
    onScrollSensitivityChanged: (Float) -> Unit,
    onMouseAccelEnabledChanged: (Boolean) -> Unit,
    onMouseAccelProfileChanged: (String) -> Unit = {},
    onNaturalScrollChanged: (Boolean) -> Unit,
    onStickyKeysChanged: (Boolean) -> Unit,
    onSyncThemeChanged: (Boolean) -> Unit,
    onThemeAnimSpeedChanged: (Int) -> Unit,
    onHybridKeyboardHeightRatioChanged: (Float) -> Unit = {},
    onKeyCasterEnabledChanged: (Boolean) -> Unit = {},
    onUsbAdbModeChanged: (Boolean) -> Unit = {},
    onInvertThreeFingerSwipeChanged: (Boolean) -> Unit = {},
    onBack: () -> Unit
) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.background)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // TOP HEADER BAR (Full-Width, Compact 0.85x scale)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // VOLVER Button
            Surface(
                modifier = Modifier
                    .clickable { onBack() },
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

            // Title & Subtitle
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

            // Battery Badge
            BatteryBadge(currentTheme = currentTheme)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // MAIN CONTENT: 3-Column Balanced Dashboard (Scale ~0.85x, 0% squish)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // COLUMN 1: MOTOR DE CURSOR & ACELERACIÓN
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

                // Sensibilidad
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

                // Algoritmo Aceleración
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
                            Triple("none", "Sin Aceleración (1:1)", "Ideal OSU! (Direct Input 100% lineal)"),
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
            }

            // COLUMN 2: DESPLAZAMIENTO & NAVEGACIÓN
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
                    Text("DESPLAZAMIENTO & SCROLL", color = currentTheme.secondaryAccent, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }

                // Scroll Sensitivity & Natural
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

                // Sticky Keys
                Surface(
                    color = currentTheme.card.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, currentTheme.secondaryAccent.copy(alpha = 0.2f)),
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
                            colors = SwitchDefaults.colors(checkedThumbColor = currentTheme.secondaryAccent, checkedTrackColor = currentTheme.secondaryAccent.copy(alpha = 0.4f))
                        )
                    }
                }

                // Altura Teclado Híbrido
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
                                Text("Proporción de pantalla del teclado (55% a 70%)", color = currentTheme.textMuted, fontSize = 7.5.sp)
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
            }

            // COLUMN 3: AUDIO MECÁNICO & RICE SYNC
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
                    Text("AUDIO & SISTEMA", color = currentTheme.primaryAccent, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }

                // Audio Mecánico
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

                // Hyprland Transición & Rice Sync
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
                            valueRange = 100f..1000f,
                            colors = SliderDefaults.colors(thumbColor = currentTheme.primaryAccent, activeTrackColor = currentTheme.primaryAccent)
                        )

                        HorizontalDivider(color = currentTheme.primaryAccent.copy(alpha = 0.15f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = currentTheme.primaryAccent, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sincronizar Tema PC", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text("Auto-sync con gh0stzk dotfiles rices", color = currentTheme.textMuted, fontSize = 7.5.sp)
                            }
                            Switch(
                                checked = syncTheme,
                                onCheckedChange = onSyncThemeChanged,
                                colors = SwitchDefaults.colors(checkedThumbColor = currentTheme.primaryAccent, checkedTrackColor = currentTheme.primaryAccent.copy(alpha = 0.4f))
                            )
                        }

                        HorizontalDivider(color = currentTheme.primaryAccent.copy(alpha = 0.15f), thickness = 1.dp)

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
                                    Text("Conexión USB / ADB (Túnel Estable)", color = currentTheme.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text("Túnel local 127.0.0.1 (latencia 0.1ms sin interferencias)", color = currentTheme.textMuted, fontSize = 7.5.sp)
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
