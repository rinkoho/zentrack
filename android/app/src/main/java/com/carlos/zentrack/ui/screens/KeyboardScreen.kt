package com.carlos.zentrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.zentrack.theme.ZenThemeConfig
import com.carlos.zentrack.ui.components.BatteryBadge
import com.carlos.zentrack.ui.components.KeyCap

@Composable
fun KeyboardScreen(
    currentTheme: ZenThemeConfig,
    isConnected: Boolean,
    statusText: String,
    showTopBar: Boolean = true,
    onOpenDrawer: () -> Unit,
    onReconnect: () -> Unit,
    onOpenThemeDialog: () -> Unit,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    var isFnActive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.background)
            .padding(horizontal = 8.dp, vertical = if (showTopBar) 6.dp else 2.dp),
        verticalArrangement = Arrangement.spacedBy(if (showTopBar) 6.dp else 2.dp)
    ) {
        if (showTopBar) {
            // -------------------------------------------------------------
            // TOP OLED DASHBOARD BAR (Integrated Hamburger Menu & Status)
            // -------------------------------------------------------------
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
            shape = RoundedCornerShape(8.dp),
            color = currentTheme.surface,
            border = BorderStroke(1.dp, currentTheme.card)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Integrated Menu Button + Status Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            onOpenDrawer()
                            onVibrate(15L)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Menu",
                            tint = currentTheme.primaryAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                color = if (isConnected) currentTheme.primaryAccent else Color(0xFFF7768E),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ZEN-65% // $statusText",
                        color = currentTheme.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    BatteryBadge(currentTheme = currentTheme)
                    Spacer(modifier = Modifier.width(8.dp))
                    // Right Theme Pill Badge (Clickable Theme Selector)
                    Surface(
                        color = currentTheme.primaryAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            onOpenThemeDialog()
                            onVibrate(12L)
                        }
                    ) {
                        Text(
                            text = "🎨 ${currentTheme.name}",
                            color = currentTheme.primaryAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
        }

        // -------------------------------------------------------------
        // 65% MECHANICAL KEYBOARD SURFACE & ALUMINUM CASE FRAME
        // (Exact replica of web_keyboard.jpg & style.css)
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f), // Keeps clean 2.75:1 mechanical keyboard aspect ratio
                shape = RoundedCornerShape(14.dp),
                color = currentTheme.chasisBg,
                border = BorderStroke(3.dp, currentTheme.chasisBorder),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    // ROW 1: Esc (Accent), 1-0 (Fn -> F1-F12), -, =, Backspace (Mod), DEL (Mod)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        KeyCap("Esc", "Escape", currentTheme, modifier = Modifier.weight(1f), isAccent = true, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F1" else "1", if (isFnActive) "F1" else "1", currentTheme, sublabel = if (isFnActive) "1" else "!", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F2" else "2", if (isFnActive) "F2" else "2", currentTheme, sublabel = if (isFnActive) "2" else "@", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F3" else "3", if (isFnActive) "F3" else "3", currentTheme, sublabel = if (isFnActive) "3" else "#", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F4" else "4", if (isFnActive) "F4" else "4", currentTheme, sublabel = if (isFnActive) "4" else "$", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F5" else "5", if (isFnActive) "F5" else "5", currentTheme, sublabel = if (isFnActive) "5" else "%", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F6" else "6", if (isFnActive) "F6" else "6", currentTheme, sublabel = if (isFnActive) "6" else "^", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F7" else "7", if (isFnActive) "F7" else "7", currentTheme, sublabel = if (isFnActive) "7" else "&", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F8" else "8", if (isFnActive) "F8" else "8", currentTheme, sublabel = if (isFnActive) "8" else "*", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F9" else "9", if (isFnActive) "F9" else "9", currentTheme, sublabel = if (isFnActive) "9" else "(", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F10" else "0", if (isFnActive) "F10" else "0", currentTheme, sublabel = if (isFnActive) "0" else ")", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F11" else "-", if (isFnActive) "F11" else "minus", currentTheme, sublabel = if (isFnActive) "-" else "_", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F12" else "=", if (isFnActive) "F12" else "equal", currentTheme, sublabel = if (isFnActive) "=" else "+", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("Backspace", "BackSpace", currentTheme, modifier = Modifier.weight(2f), isModifier = true, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("DEL", "Delete", currentTheme, modifier = Modifier.weight(1f), isModifier = true, onSendJson = onSendJson, onVibrate = onVibrate)
                    }

                    // ROW 2: Tab, QWERTYUIOP, [], \, PGUP (Mod)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        KeyCap("Tab", "Tab", currentTheme, modifier = Modifier.weight(1.5f), isModifier = true, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("Q", "q", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("W", "w", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("E", "e", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("R", "r", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("T", "t", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("Y", "y", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("U", "u", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("I", "i", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("O", "o", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("P", "p", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("[", "bracketleft", currentTheme, sublabel = "{", modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("]", "bracketright", currentTheme, sublabel = "}", modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("\\", "backslash", currentTheme, sublabel = "|", modifier = Modifier.weight(1.5f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("PGUP", "Prior", currentTheme, modifier = Modifier.weight(1f), isModifier = true, onSendJson = onSendJson, onVibrate = onVibrate)
                    }

                    // ROW 3: Caps Lock, ASDFGHJKL, ;, ', Enter (ACCENT), PGDN (Mod)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        KeyCap("Caps Lock", "Caps_Lock", currentTheme, modifier = Modifier.weight(1.75f), isModifier = true, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("A", "a", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("S", "s", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("D", "d", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("F", "f", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("G", "g", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("H", "h", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("J", "j", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("K", "k", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("L", "l", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap(";", "semicolon", currentTheme, sublabel = ":", modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("'", "apostrophe", currentTheme, sublabel = "\"", modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("Enter", "Return", currentTheme, modifier = Modifier.weight(2.25f), isAccent = true, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("PGDN", "Next", currentTheme, modifier = Modifier.weight(1f), isModifier = true, onSendJson = onSendJson, onVibrate = onVibrate)
                    }

                    // ROW 4: Shift, ZXCVBNM, ,, ., /, Shift, Up Arrow (ACCENT), END (Mod)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        KeyCap("Shift", "Shift_L", currentTheme, modifier = Modifier.weight(2.25f), isModifier = true, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("Z", "z", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("X", "x", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("C", "c", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("V", "v", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("B", "b", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("N", "n", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("M", "m", currentTheme, modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap(",", "comma", currentTheme, sublabel = "<", modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap(".", "period", currentTheme, sublabel = ">", modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("/", "slash", currentTheme, sublabel = "?", modifier = Modifier.weight(1f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("Shift", "Shift_R", currentTheme, modifier = Modifier.weight(1.75f), isModifier = true, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("↑", "Up", currentTheme, modifier = Modifier.weight(1f), isAccent = true, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("END", "End", currentTheme, modifier = Modifier.weight(1f), isModifier = true, onSendJson = onSendJson, onVibrate = onVibrate)
                    }

                    // ROW 5: Ctrl, Arch Linux Super, Alt, SPACEBAR, Alt, Fn, Ctrl, Left (ACCENT), Down (ACCENT), Right (ACCENT)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.1f),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        KeyCap("Ctrl", "Control_L", currentTheme, modifier = Modifier.weight(1.25f), isModifier = true, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("Super", "Super_L", currentTheme, nerdSymbol = "\uF303", modifier = Modifier.weight(1.25f), isModifier = true, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("Alt", "Alt_L", currentTheme, modifier = Modifier.weight(1.25f), isModifier = true, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("", "space", currentTheme, modifier = Modifier.weight(6.25f), onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("AltGr", "Alt_R", currentTheme, modifier = Modifier.weight(1.25f), isModifier = true, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("Fn", "", currentTheme, isModifier = true, modifier = Modifier.weight(1f), onPressStateChanged = { isFnActive = it }, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("Ctrl", "Control_R", currentTheme, modifier = Modifier.weight(1.25f), isModifier = true, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("←", "Left", currentTheme, modifier = Modifier.weight(1f), isAccent = true, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("↓", "Down", currentTheme, modifier = Modifier.weight(1f), isAccent = true, onSendJson = onSendJson, onVibrate = onVibrate)
                        KeyCap("→", "Right", currentTheme, modifier = Modifier.weight(1f), isAccent = true, onSendJson = onSendJson, onVibrate = onVibrate)
                    }
                }
            }
        }
    }
}



