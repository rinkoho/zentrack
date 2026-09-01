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
import android.os.Handler
import android.os.Looper
import com.carlos.zentrack.theme.ZenThemeConfig
import com.carlos.zentrack.ui.components.BatteryBadge
import com.carlos.zentrack.ui.components.KeyCap
import com.carlos.zentrack.ui.components.ModifierKeyCap
import com.carlos.zentrack.ui.components.ModifierMode
import org.json.JSONObject

import com.carlos.zentrack.ui.components.KeycasterState
import com.carlos.zentrack.ui.components.KeycasterTopBarContent
import com.carlos.zentrack.ui.components.rememberKeycasterState

@Composable
fun KeyboardScreen(
    currentTheme: ZenThemeConfig,
    isConnected: Boolean,
    statusText: String,
    showTopBar: Boolean = true,
    stickyKeysEnabled: Boolean = true,
    keyCasterEnabled: Boolean = true,
    keycasterState: KeycasterState = rememberKeycasterState(),
    onOpenDrawer: () -> Unit,
    onReconnect: () -> Unit,
    onOpenThemeDialog: () -> Unit,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var isFnActive by remember { mutableStateOf(false) }
    var isCapsLockActive by remember { mutableStateOf(false) }
    val modifierStates = remember { mutableStateMapOf<String, ModifierMode>() }

    fun sendKeyPayload(rawJson: String) {
        keycasterState.processEvent(rawJson, scope, keyCasterEnabled)
        val payload = com.carlos.zentrack.security.ZenCrypto.encryptKeyboardPayload(rawJson) ?: rawJson
        onSendJson(payload)
    }

    // Auto-flush & release all active sticky modifiers if user turns Sticky Mode OFF in Settings
    LaunchedEffect(stickyKeysEnabled) {
        if (!stickyKeysEnabled && modifierStates.isNotEmpty()) {
            val keysToRelease = modifierStates.keys.toList()
            modifierStates.clear()
            for (modKey in keysToRelease) {
                val upJson = JSONObject().put("type", "keyup").put("key", modKey).toString()
                sendKeyPayload(upJson)
            }
        }
    }

    fun releaseAllStickyModifiers() {
        if (!stickyKeysEnabled) return
        val stickyToRelease = modifierStates.filter { it.value == ModifierMode.STICKY }.keys.toList()
        for (modKey in stickyToRelease) {
            modifierStates.remove(modKey)
        }
        
        if (stickyToRelease.isNotEmpty()) {
            mainHandler.postDelayed({
                for (modKey in stickyToRelease) {
                    val upJson = JSONObject().put("type", "keyup").put("key", modKey).toString()
                    sendKeyPayload(upJson)
                }
            }, 50L)
        }
    }

    fun onNormalKeySent(rawJson: String) {
        sendKeyPayload(rawJson)
        if (stickyKeysEnabled) {
            releaseAllStickyModifiers()
        }
    }

    @Composable
    fun RowScope.StickyModKeyCap(
        label: String,
        keyCode: String,
        weight: Float,
        nerdSymbol: String? = null
    ) {
        if (!stickyKeysEnabled) {
            // Standard Physical KeyCap when Sticky Mode is OFF in Settings
            KeyCap(
                label = label,
                keyCode = keyCode,
                theme = currentTheme,
                nerdSymbol = nerdSymbol,
                modifier = Modifier.weight(weight),
                isModifier = true,
                onSendJson = ::onNormalKeySent,
                onVibrate = onVibrate
            )
        } else {
            val currentMode = modifierStates[keyCode] ?: ModifierMode.OFF
            ModifierKeyCap(
                label = label,
                keyCode = keyCode,
                theme = currentTheme,
                mode = currentMode,
                modifier = Modifier.weight(weight),
                nerdSymbol = nerdSymbol,
                onTap = {
                    if (currentMode != ModifierMode.OFF) {
                        modifierStates.remove(keyCode)
                        val upJson = JSONObject().put("type", "keyup").put("key", keyCode).toString()
                        sendKeyPayload(upJson)
                    } else {
                        modifierStates[keyCode] = ModifierMode.STICKY
                        val downJson = JSONObject().put("type", "keydown").put("key", keyCode).toString()
                        sendKeyPayload(downJson)
                    }
                },
                onLongPress = {
                    if (currentMode == ModifierMode.LOCKED) {
                        modifierStates.remove(keyCode)
                        val upJson = JSONObject().put("type", "keyup").put("key", keyCode).toString()
                        sendKeyPayload(upJson)
                    } else {
                        modifierStates[keyCode] = ModifierMode.LOCKED
                        if (currentMode == ModifierMode.OFF) {
                            val downJson = JSONObject().put("type", "keydown").put("key", keyCode).toString()
                            sendKeyPayload(downJson)
                        }
                    }
                },
                onVibrate = onVibrate
            )
        }
    }

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
                    .height(32.dp),
                shape = RoundedCornerShape(10.dp),
                color = currentTheme.surface,
                border = BorderStroke(1.5.dp, currentTheme.primaryAccent.copy(alpha = 0.45f)),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT: Menu & Branding
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
                                contentDescription = "Menu",
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
                            text = "ZEN-65%",
                            color = currentTheme.textPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // CENTER: OLED Keycaster Display stretching horizontally!
                    KeycasterTopBarContent(
                        keycasterState = keycasterState,
                        theme = currentTheme,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // RIGHT: Battery & Theme selector
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BatteryBadge(currentTheme = currentTheme)
                        Spacer(modifier = Modifier.width(8.dp))
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
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(14.dp),
                color = currentTheme.chasisBg,
                border = BorderStroke(3.dp, currentTheme.chasisBorder),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // ROW 1: Esc (Accent), 1-0 (Fn -> F1-F12), -, =, Backspace (Mod), DEL (Mod)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        KeyCap(if (isFnActive) "`" else "Esc", if (isFnActive) "grave" else "Escape", currentTheme, sublabel = if (isFnActive) "~" else null, modifier = Modifier.weight(1f), isAccent = true, onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F1" else "1", if (isFnActive) "F1" else "1", currentTheme, sublabel = if (isFnActive) "1" else "!", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F2" else "2", if (isFnActive) "F2" else "2", currentTheme, sublabel = if (isFnActive) "2" else "@", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F3" else "3", if (isFnActive) "F3" else "3", currentTheme, sublabel = if (isFnActive) "3" else "#", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F4" else "4", if (isFnActive) "F4" else "4", currentTheme, sublabel = if (isFnActive) "4" else "$", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F5" else "5", if (isFnActive) "F5" else "5", currentTheme, sublabel = if (isFnActive) "5" else "%", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F6" else "6", if (isFnActive) "F6" else "6", currentTheme, sublabel = if (isFnActive) "6" else "^", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F7" else "7", if (isFnActive) "F7" else "7", currentTheme, sublabel = if (isFnActive) "7" else "&", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F8" else "8", if (isFnActive) "F8" else "8", currentTheme, sublabel = if (isFnActive) "8" else "*", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F9" else "9", if (isFnActive) "F9" else "9", currentTheme, sublabel = if (isFnActive) "9" else "(", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F10" else "0", if (isFnActive) "F10" else "0", currentTheme, sublabel = if (isFnActive) "0" else ")", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F11" else "-", if (isFnActive) "F11" else "minus", currentTheme, sublabel = if (isFnActive) "-" else "_", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "F12" else "=", if (isFnActive) "F12" else "equal", currentTheme, sublabel = if (isFnActive) "=" else "+", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("Backspace", "BackSpace", currentTheme, modifier = Modifier.weight(2f), isModifier = true, onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("DEL", "Delete", currentTheme, modifier = Modifier.weight(1f), isModifier = true, onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                    }

                    // ROW 2: Tab, QWERTYUIOP, [], \, PGUP (Mod)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        KeyCap("Tab", "Tab", currentTheme, modifier = Modifier.weight(1.5f), isModifier = true, onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("Q", "q", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("W", "w", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("E", "e", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("R", "r", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("T", "t", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "Prt" else "Y", if (isFnActive) "Print" else "y", currentTheme, isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "Scr" else "U", if (isFnActive) "Scroll_Lock" else "u", currentTheme, isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "Pau" else "I", if (isFnActive) "Pause" else "i", currentTheme, isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("O", "o", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("P", "p", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("[", "bracketleft", currentTheme, sublabel = "{", modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("]", "bracketright", currentTheme, sublabel = "}", modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("\\", "backslash", currentTheme, sublabel = "|", modifier = Modifier.weight(1.5f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("PGUP", "Prior", currentTheme, modifier = Modifier.weight(1f), isModifier = true, onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                    }

                    // ROW 3: Caps Lock, ASDFGHJKL, ;, ', Enter (ACCENT), PGDN (Mod)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        KeyCap("Caps Lock", "Caps_Lock", currentTheme, modifier = Modifier.weight(1.75f), isModifier = true, isLedOn = isCapsLockActive, onPressStateChanged = { isDown -> if (isDown) { isCapsLockActive = !isCapsLockActive } }, onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("A", "a", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("S", "s", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("D", "d", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("F", "f", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("G", "g", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "Ins" else "H", if (isFnActive) "Insert" else "h", currentTheme, isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "Home" else "J", if (isFnActive) "Home" else "j", currentTheme, isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "PgUp" else "K", if (isFnActive) "Prior" else "k", currentTheme, isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("L", "l", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(";", "semicolon", currentTheme, sublabel = ":", modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("'", "apostrophe", currentTheme, sublabel = "\"", modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("Enter", "Return", currentTheme, modifier = Modifier.weight(2.25f), isAccent = true, onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("PGDN", "Next", currentTheme, modifier = Modifier.weight(1f), isModifier = true, onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                    }

                    // ROW 4: Shift, ZXCVBNM, ,, ., /, Shift, Up Arrow (ACCENT), END (Mod)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        StickyModKeyCap("Shift", "Shift_L", 2.25f)
                        KeyCap("Z", "z", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("X", "x", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("C", "c", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("V", "v", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("B", "b", currentTheme, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "Del" else "N", if (isFnActive) "Delete" else "n", currentTheme, isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "End" else "M", if (isFnActive) "End" else "m", currentTheme, isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(if (isFnActive) "PgDn" else ",", if (isFnActive) "Next" else "comma", currentTheme, sublabel = if (isFnActive) null else "<", isAccent = isFnActive, modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap(".", "period", currentTheme, sublabel = ">", modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("/", "slash", currentTheme, sublabel = "?", modifier = Modifier.weight(1f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        StickyModKeyCap("Shift", "Shift_R", 1.75f)
                        KeyCap("▲", "Up", currentTheme, modifier = Modifier.weight(1f), isAccent = true, onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("END", "End", currentTheme, modifier = Modifier.weight(1f), isModifier = true, onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                    }

                    // ROW 5: Ctrl, Arch Linux Super, Alt, SPACEBAR, Alt, Fn, Ctrl, Left (ACCENT), Down (ACCENT), Right (ACCENT)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.1f),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        StickyModKeyCap("Ctrl", "Control_L", 1.25f)
                        StickyModKeyCap("Super", "Super_L", 1.25f, nerdSymbol = "\uF303")
                        StickyModKeyCap("Alt", "Alt_L", 1.25f)
                        KeyCap("", "space", currentTheme, modifier = Modifier.weight(6.25f), onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        StickyModKeyCap("AltGr", "Alt_R", 1.25f)
                        ModifierKeyCap("Fn", "Fn", currentTheme, mode = if (isFnActive) ModifierMode.LOCKED else ModifierMode.OFF, modifier = Modifier.weight(1f), onTap = { isFnActive = !isFnActive }, onLongPress = { isFnActive = !isFnActive }, onVibrate = onVibrate)
                        StickyModKeyCap("Ctrl", "Control_R", 1.25f)
                        KeyCap("◀", "Left", currentTheme, modifier = Modifier.weight(1f), isAccent = true, onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("▼", "Down", currentTheme, modifier = Modifier.weight(1f), isAccent = true, onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                        KeyCap("▶", "Right", currentTheme, modifier = Modifier.weight(1f), isAccent = true, onSendJson = ::onNormalKeySent, onVibrate = onVibrate)
                    }
                }
            }
        }
    }
}
