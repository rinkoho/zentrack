package com.carlos.zentrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.carlos.zentrack.theme.*

@Composable
fun ThemeSelectionDialog(
    show: Boolean,
    activeTheme: ZenThemeConfig,
    onThemeSelected: (ZenThemeConfig) -> Unit,
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
                .windowInsetsPadding(WindowInsets(0, 0, 0, 0)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .fillMaxHeight(0.82f),
                shape = RoundedCornerShape(18.dp),
                color = activeTheme.surface,
                border = BorderStroke(1.dp, activeTheme.primaryAccent.copy(alpha = 0.4f)),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header Title & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🎨 TEMAS DEL TECLADO & PC RICES",
                                color = activeTheme.primaryAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = activeTheme.textMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2-Column Grid of 23 gh0stzk Themes
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(AvailableZenThemes) { theme ->
                            val isSelected = theme.name == activeTheme.name

                            Surface(
                                color = if (isSelected) activeTheme.primaryAccent.copy(alpha = 0.18f) else activeTheme.card,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) activeTheme.primaryAccent else activeTheme.primaryAccent.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onThemeSelected(theme) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 3-Color Swatch Preview
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(theme.keyAlphaBg, CircleShape)
                                                .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(theme.keyModBg, CircleShape)
                                                .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(theme.keyAccentBg, CircleShape)
                                                .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = theme.name,
                                            color = if (isSelected) activeTheme.primaryAccent else theme.textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
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
