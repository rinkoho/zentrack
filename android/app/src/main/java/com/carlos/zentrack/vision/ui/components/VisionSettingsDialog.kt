package com.carlos.zentrack.vision.ui.components

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.carlos.zentrack.theme.ZenThemeConfig
import com.carlos.zentrack.vision.data.ZenVisionPreferences
import com.carlos.zentrack.vision.engine.HandLandmarkerHelper

@Composable
fun VisionSettingsDialog(
    currentTheme: ZenThemeConfig,
    lensFacing: Int,
    showSkeleton: Boolean,
    showTelemetry: Boolean,
    mirrorFront: Boolean,
    delegate: Int,
    minConfidence: Float,
    maxHands: Int,
    renderMode: String,
    isAirMouseEnabled: Boolean = true,
    visionSensitivity: Float = 1.0f,
    onLensFacingChanged: (Int) -> Unit,
    onShowSkeletonChanged: (Boolean) -> Unit,
    onShowTelemetryChanged: (Boolean) -> Unit,
    onMirrorFrontChanged: (Boolean) -> Unit,
    onDelegateChanged: (Int) -> Unit,
    onMinConfidenceChanged: (Float) -> Unit,
    onMaxHandsChanged: (Int) -> Unit,
    onRenderModeChanged: (String) -> Unit,
    onAirMouseEnabledChanged: (Boolean) -> Unit,
    onVisionSensitivityChanged: (Float) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .width(440.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = currentTheme.surface,
            border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.4f)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            tint = currentTheme.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AJUSTES ZENVISIÓN",
                            color = currentTheme.primaryAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = currentTheme.textMuted
                        )
                    }
                }

                HorizontalDivider(color = currentTheme.primaryAccent.copy(alpha = 0.2f), thickness = 1.dp)

                // 1. Modo de Renderizado & Rendimiento
                Text(
                    text = "MODO DE RENDERIZADO & LATENCIA",
                    color = currentTheme.textMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // HD Mode
                    val isHd = renderMode == "hd"
                    Button(
                        onClick = { onRenderModeChanged("hd") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHd) currentTheme.primaryAccent else currentTheme.card,
                            contentColor = if (isHd) Color.Black else currentTheme.textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🎥 HD (1080p)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Performance Mode
                    val isPerf = renderMode == "performance"
                    Button(
                        onClick = { onRenderModeChanged("performance") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPerf) currentTheme.primaryAccent else currentTheme.card,
                            contentColor = if (isPerf) Color.Black else currentTheme.textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⚡ 480p Ágil", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Skeleton Only (Gaming)
                    val isSkel = renderMode == "skeleton"
                    Button(
                        onClick = { onRenderModeChanged("skeleton") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSkel) currentTheme.secondaryAccent else currentTheme.card,
                            contentColor = if (isSkel) Color.Black else currentTheme.textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🎮 Solo Esqueleto", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 2. Selector de Cámara
                Text(
                    text = "LENTE DE CÁMARA",
                    color = currentTheme.textMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isFront = lensFacing == CameraSelector.LENS_FACING_FRONT
                    Button(
                        onClick = { onLensFacingChanged(CameraSelector.LENS_FACING_FRONT) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFront) currentTheme.primaryAccent else currentTheme.card,
                            contentColor = if (isFront) Color.Black else currentTheme.textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraFront, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Selfie (Frontal)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onLensFacingChanged(CameraSelector.LENS_FACING_BACK) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isFront) currentTheme.primaryAccent else currentTheme.card,
                            contentColor = if (!isFront) Color.Black else currentTheme.textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraRear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Principal (Trasera)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 3. Aceleración por Hardware (Delegate)
                Text(
                    text = "MOTOR DE INFERENCIA (EDGE ML)",
                    color = currentTheme.textMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isGpu = delegate == HandLandmarkerHelper.DELEGATE_GPU
                    Button(
                        onClick = { onDelegateChanged(HandLandmarkerHelper.DELEGATE_GPU) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGpu) currentTheme.secondaryAccent else currentTheme.card,
                            contentColor = if (isGpu) Color.Black else currentTheme.textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GPU (Mali-G615)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onDelegateChanged(HandLandmarkerHelper.DELEGATE_CPU) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isGpu) currentTheme.secondaryAccent else currentTheme.card,
                            contentColor = if (!isGpu) Color.Black else currentTheme.textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CPU (A78 Cores)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 3. Cantidad de Manos (Single vs Dual)
                Text(
                    text = "MODO DE SEGUIMIENTO",
                    color = currentTheme.textMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isSingle = maxHands == 1
                    Button(
                        onClick = { onMaxHandsChanged(1) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSingle) currentTheme.primaryAccent else currentTheme.card,
                            contentColor = if (isSingle) Color.Black else currentTheme.textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("1 Mano (Ultra Speed osu!)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onMaxHandsChanged(2) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isSingle) currentTheme.primaryAccent else currentTheme.card,
                            contentColor = if (!isSingle) Color.Black else currentTheme.textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("2 Manos (Gestos Duales)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 4. Conmutadores de Visualización
                Text(
                    text = "OPCIONES VISUALES",
                    color = currentTheme.textMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(currentTheme.card, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingToggleRow(
                        title = "Esqueleto Neón y Puntos 3D",
                        checked = showSkeleton,
                        onCheckedChange = onShowSkeletonChanged,
                        theme = currentTheme
                    )
                    SettingToggleRow(
                        title = "Telemetría HUD & Métricas en Vivo",
                        checked = showTelemetry,
                        onCheckedChange = onShowTelemetryChanged,
                        theme = currentTheme
                    )
                    SettingToggleRow(
                        title = "Modo Espejo (Cámara Frontal)",
                        checked = mirrorFront,
                        onCheckedChange = onMirrorFrontChanged,
                        theme = currentTheme
                    )
                }

                // 5. Opciones de Control Periférico (Air-Mouse)
                Text(
                    text = "CONTROL ESPACIAL DE MOUSE (500 HZ)",
                    color = currentTheme.textMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(currentTheme.card, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingToggleRow(
                        title = "Emisión Air-Mouse a PC (500 Hz)",
                        checked = isAirMouseEnabled,
                        onCheckedChange = onAirMouseEnabledChanged,
                        theme = currentTheme
                    )
                }

                // Sensibilidad del Puntero Espacial
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SENSIBILIDAD DEL PUNTERO",
                        color = currentTheme.textMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "%.1fx".format(visionSensitivity),
                        color = currentTheme.primaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Slider(
                    value = visionSensitivity,
                    onValueChange = onVisionSensitivityChanged,
                    valueRange = 0.3f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = currentTheme.primaryAccent,
                        activeTrackColor = currentTheme.primaryAccent,
                        inactiveTrackColor = currentTheme.textMuted.copy(alpha = 0.3f)
                    )
                )

                // 6. Umbral de Confianza
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UMBRAL DE DETECCIÓN",
                        color = currentTheme.textMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(minConfidence * 100).toInt()}%",
                        color = currentTheme.primaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Slider(
                    value = minConfidence,
                    onValueChange = onMinConfidenceChanged,
                    valueRange = 0.2f..0.9f,
                    colors = SliderDefaults.colors(
                        thumbColor = currentTheme.primaryAccent,
                        activeTrackColor = currentTheme.primaryAccent,
                        inactiveTrackColor = currentTheme.textMuted.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    theme: ZenThemeConfig
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = theme.textPrimary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = theme.primaryAccent,
                uncheckedThumbColor = theme.textMuted,
                uncheckedTrackColor = theme.surface
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}
