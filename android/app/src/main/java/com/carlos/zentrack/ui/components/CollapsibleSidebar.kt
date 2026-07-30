package com.carlos.zentrack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.zentrack.theme.*

@Composable
fun CollapsibleSidebar(
    isExpanded: Boolean,
    isConnected: Boolean,
    statusText: String,
    activeProfile: String,
    onProfileSelected: (String) -> Unit,
    onToggleCollapse: () -> Unit,
    onReconnect: () -> Unit,
    onOpenSettings: () -> Unit,
    onVibrate: (Long) -> Unit
) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = expandHorizontally(),
        exit = shrinkHorizontally()
    ) {
        Column(
            modifier = Modifier
                .width(170.dp)
                .fillMaxHeight()
                .background(TokyoSurface)
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Header Title & Collapse Icon Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ZEN-TRACK",
                        color = TokyoCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = {
                        onToggleCollapse()
                        onVibrate(10L)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.MenuOpen, contentDescription = "Collapse", tint = TokyoMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Connection Status Card with Manual Reconnect Button
                Surface(
                    color = TokyoCard,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (isConnected) TokyoCyan else TokyoPink,
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                color = TokyoText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = {
                                onReconnect()
                                onVibrate(15L)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reconectar",
                                tint = TokyoCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "MODOS Y PERFILES",
                    color = TokyoMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    val profiles = listOf(
                        Triple("Trackpad", Icons.Default.Mouse, "Trackpad"),
                        Triple("Keyboard", Icons.Default.Keyboard, "Teclado 65%"),
                        Triple("Hybrid", Icons.Default.Dashboard, "Híbrido (Dual)"),
                        Triple("Gaming", Icons.Default.SportsEsports, "Mando Gaming"),
                        Triple("BSPWM", Icons.AutoMirrored.Filled.ViewQuilt, "Mosaico BSPWM")
                    )
                    for (item in profiles) {
                        val profileName = item.first
                        val icon = item.second
                        val label = item.third
                        val isSelected = activeProfile == profileName

                        Surface(
                            color = if (isSelected) TokyoPurple.copy(alpha = 0.25f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = if (isSelected) BorderStroke(1.dp, TokyoPurple.copy(alpha = 0.5f)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable {
                                    onProfileSelected(profileName)
                                    onVibrate(15L)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) TokyoPurple else TokyoMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    color = if (isSelected) TokyoPurple else TokyoText.copy(alpha = 0.8f),
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Settings Button
            IconButton(
                onClick = {
                    onOpenSettings()
                    onVibrate(15L)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = TokyoBlue)
            }
        }
    }
}
