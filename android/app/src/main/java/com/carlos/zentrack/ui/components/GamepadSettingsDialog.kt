package com.carlos.zentrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.SportsEsports
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
import com.carlos.zentrack.theme.ZenThemeConfig

@Composable
fun GamepadSettingsDialog(
    show: Boolean,
    currentTheme: ZenThemeConfig,
    gamepadMode: String, // "pc" or "xbox"
    pcSensitivity: Float,
    pcAccelEnabled: Boolean,
    xboxSensitivity: Float,
    xboxRightStickMode: String, // "autocenter" or "native_stick"
    onGamepadModeChanged: (String) -> Unit,
    onPcSensitivityChanged: (Float) -> Unit,
    onPcAccelEnabledChanged: (Boolean) -> Unit,
    onXboxSensitivityChanged: (Float) -> Unit,
    onXboxRightStickModeChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

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
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .fillMaxHeight(0.85f)
                    .clickable(enabled = false) {},
                color = currentTheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.4f)),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Modal Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = currentTheme.primaryAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CONFIGURACIÓN DE GAMEPAD",
                                color = currentTheme.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = currentTheme.textMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Section 1: Selector de Modo Principal (PC vs Xbox)
                        Text(
                            text = "MODO DE CONTROLADOR ACTIVO",
                            color = currentTheme.primaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(currentTheme.card, RoundedCornerShape(10.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isPcSelected = gamepadMode == "pc"
                            Surface(
                                color = if (isPcSelected) currentTheme.primaryAccent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onGamepadModeChanged("pc") }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Computer,
                                        contentDescription = null,
                                        tint = if (isPcSelected) Color.Black else currentTheme.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Modo PC",
                                            color = if (isPcSelected) Color.Black else currentTheme.textPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "WASD + Mouse Aim",
                                            color = if (isPcSelected) Color.Black.copy(alpha = 0.75f) else currentTheme.textMuted,
                                            fontSize = 9.5.sp
                                        )
                                    }
                                }
                            }

                            val isXboxSelected = gamepadMode == "xbox"
                            Surface(
                                color = if (isXboxSelected) currentTheme.primaryAccent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onGamepadModeChanged("xbox") }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.SportsEsports,
                                        contentDescription = null,
                                        tint = if (isXboxSelected) Color.Black else currentTheme.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Modo Xbox Nativo",
                                            color = if (isXboxSelected) Color.Black else currentTheme.textPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Kernel /dev/uinput",
                                            color = if (isXboxSelected) Color.Black.copy(alpha = 0.75f) else currentTheme.textMuted,
                                            fontSize = 9.5.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // SECTION 2: CONFIGURACIÓN MANDO PC (SEPARADA)
                        Surface(
                            color = currentTheme.card.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🎮 CONFIGURACIÓN MANDO PC (WASD + MOUSE)",
                                    color = currentTheme.primaryAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // PC Sensibility Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Sensibilidad Mouse PC",
                                        color = currentTheme.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = String.format("%.2fx", pcSensitivity),
                                        color = currentTheme.primaryAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Slider(
                                    value = pcSensitivity,
                                    onValueChange = onPcSensitivityChanged,
                                    valueRange = 0.3f..4.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = currentTheme.primaryAccent,
                                        activeTrackColor = currentTheme.primaryAccent,
                                        inactiveTrackColor = currentTheme.surface
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // PC Acceleration Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Aceleración de Mouse PC",
                                            color = currentTheme.textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (pcAccelEnabled) "Activada (Progresiva)" else "Desactivada por defecto (Precisión 1:1)",
                                            color = currentTheme.textMuted,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Switch(
                                        checked = pcAccelEnabled,
                                        onCheckedChange = onPcAccelEnabledChanged,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = currentTheme.primaryAccent,
                                            checkedTrackColor = currentTheme.primaryAccent.copy(alpha = 0.4f),
                                            uncheckedThumbColor = currentTheme.textMuted,
                                            uncheckedTrackColor = currentTheme.surface
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // SECTION 3: CONFIGURACIÓN MANDO XBOX (SEPARADA)
                        Surface(
                            color = currentTheme.card.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🕹️ CONFIGURACIÓN MANDO XBOX NATIVO",
                                    color = currentTheme.primaryAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Xbox Right Stick Sensitivity Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Sensibilidad Stick Derecho (Cámara Xbox)",
                                        color = currentTheme.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = String.format("%.2fx", xboxSensitivity),
                                        color = currentTheme.primaryAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Slider(
                                    value = xboxSensitivity,
                                    onValueChange = onXboxSensitivityChanged,
                                    valueRange = 0.5f..6.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = currentTheme.primaryAccent,
                                        activeTrackColor = currentTheme.primaryAccent,
                                        inactiveTrackColor = currentTheme.surface
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Xbox Right Stick Mode Selector (Autocenter vs Native Stick)
                                Text(
                                    text = "Comportamiento del Stick Derecho (Cámara)",
                                    color = currentTheme.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(currentTheme.surface, RoundedCornerShape(8.dp))
                                        .padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val isAutocenter = xboxRightStickMode == "autocenter"
                                    Surface(
                                        color = if (isAutocenter) currentTheme.primaryAccent else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onXboxRightStickModeChanged("autocenter") }
                                    ) {
                                        Text(
                                            text = "Swipe Autocentrado",
                                            color = if (isAutocenter) Color.Black else currentTheme.textPrimary,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }

                                    val isNativeStick = xboxRightStickMode == "native_stick"
                                    Surface(
                                        color = if (isNativeStick) currentTheme.primaryAccent else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onXboxRightStickModeChanged("native_stick") }
                                    ) {
                                        Text(
                                            text = "Joystick Analógico Derecho",
                                            color = if (isNativeStick) Color.Black else currentTheme.textPrimary,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
