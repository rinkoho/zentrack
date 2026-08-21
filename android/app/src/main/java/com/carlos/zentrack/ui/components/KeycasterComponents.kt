package com.carlos.zentrack.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.zentrack.theme.ZenThemeConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Data class representing a typed character with an immutable unique ID
 * to prevent Compose re-rendering / flickering when max scroll buffer length is reached.
 */
data class TypedChar(val id: Long, val char: Char)

/**
 * Keycaster State Manager with unique character ID tracking, AltGr (ñ/accents) mapping,
 * and accurate Shift symbol resolution.
 */
class KeycasterState {
    var typedCharBuffer = mutableStateListOf<TypedChar>()
    var lastShortcut by mutableStateOf("")
    var recentShortcuts = mutableStateListOf<String>()
    var activeModifiers = mutableStateListOf<String>()
    var isVisible by mutableStateOf(false)
    var isCapsActive by mutableStateOf(false)
    var isAltGrActive by mutableStateOf(false)

    private var charCounter = 0L
    private var hideJob: Job? = null

    private val shiftSymbolMap = mapOf(
        "1" to '!', "2" to '@', "3" to '#', "4" to '$', "5" to '%',
        "6" to '^', "7" to '&', "8" to '*', "9" to '(', "0" to ')',
        "minus" to '_', "equal" to '+', "bracketleft" to '{', "bracketright" to '}',
        "backslash" to '|', "semicolon" to ':', "apostrophe" to '"',
        "comma" to '<', "period" to '>', "slash" to '?', "grave" to '~'
    )

    private val normalSymbolMap = mapOf(
        "minus" to '-', "equal" to '=', "bracketleft" to '[', "bracketright" to ']',
        "backslash" to '\\', "semicolon" to ';', "apostrophe" to '\'',
        "comma" to ',', "period" to '.', "slash" to '/', "grave" to '`'
    )

    private val altGrMap = mapOf(
        "n" to 'ñ', "N" to 'Ñ',
        "a" to 'á', "A" to 'Á',
        "e" to 'é', "E" to 'É',
        "i" to 'í', "I" to 'Í',
        "o" to 'ó', "O" to 'Ó',
        "u" to 'ú', "U" to 'Ú',
        "1" to '|', "2" to '@', "3" to '#'
    )

    private fun hasActiveCommandModifiers(): Boolean {
        return activeModifiers.any { it in listOf("Ctrl", "Alt", "Super", "Fn") }
    }

    fun processEvent(jsonString: String, scope: kotlinx.coroutines.CoroutineScope, isEnabled: Boolean) {
        if (!isEnabled) return
        try {
            val json = JSONObject(jsonString)
            val type = json.optString("type")
            val key = json.optString("key")
            if (key.isEmpty()) return

            if (type == "keydown") {
                isVisible = true
                hideJob?.cancel()
                hideJob = scope.launch {
                    delay(1200L) // 1.2s inactivity auto-hide
                    isVisible = false
                    delay(300L) // Wait for exit animation to complete
                    typedCharBuffer.clear()
                    lastShortcut = ""
                }

                when (key) {
                    "Control_L", "Control_R" -> addMod("Ctrl")
                    "Alt_L" -> addMod("Alt") // Left Alt = Command modifier for shortcuts
                    "Alt_R" -> isAltGrActive = true // Right Alt = AltGr character modifier
                    "Shift_L", "Shift_R" -> addMod("Shift")
                    "Super_L", "Super_R" -> addMod("Super")
                    "Fn" -> addMod("Fn")
                    "Caps_Lock" -> isCapsActive = !isCapsActive
                    "BackSpace" -> {
                        if (typedCharBuffer.isNotEmpty()) {
                            typedCharBuffer.removeAt(typedCharBuffer.size - 1)
                        }
                    }
                    "Return", "Escape" -> {
                        typedCharBuffer.clear()
                        lastShortcut = ""
                    }
                    "space" -> {
                        appendChar(' ')
                    }
                    else -> {
                        val friendlyName = mapFriendlyName(key)
                        if (hasActiveCommandModifiers()) {
                            val combo = (activeModifiers + friendlyName).joinToString(" + ") { "[$it]" }
                            lastShortcut = combo
                            if (recentShortcuts.isEmpty() || recentShortcuts.first() != combo) {
                                recentShortcuts.add(0, combo)
                                if (recentShortcuts.size > 3) recentShortcuts.removeLast()
                            }
                        } else {
                            val isShiftPressed = activeModifiers.contains("Shift")

                            // 1. Check AltGr international mapping (e.g. AltGr + n -> ñ)
                            if (isAltGrActive && altGrMap.containsKey(key)) {
                                val mappedChar = altGrMap[key]!!
                                val finalChar = if (isCapsActive xor isShiftPressed) {
                                    mappedChar.uppercaseChar()
                                } else {
                                    mappedChar.lowercaseChar()
                                }
                                appendChar(finalChar)
                                lastShortcut = ""
                            }
                            // 2. Check Shifted / Normal symbols (e.g. equal -> =, Shift+equal -> +)
                            else if (isShiftPressed && shiftSymbolMap.containsKey(key)) {
                                appendChar(shiftSymbolMap[key]!!)
                                lastShortcut = ""
                            } else if (!isShiftPressed && normalSymbolMap.containsKey(key)) {
                                appendChar(normalSymbolMap[key]!!)
                                lastShortcut = ""
                            }
                            // 3. Letters / Alphabetic characters
                            else if (friendlyName.length == 1) {
                                val char = if (isCapsActive xor isShiftPressed) {
                                    friendlyName.uppercase()[0]
                                } else {
                                    friendlyName.lowercase()[0]
                                }
                                appendChar(char)
                                lastShortcut = ""
                            } else {
                                lastShortcut = "[$friendlyName]"
                            }
                        }
                    }
                }
            } else if (type == "keyup") {
                when (key) {
                    "Control_L", "Control_R" -> removeMod("Ctrl")
                    "Alt_L" -> removeMod("Alt")
                    "Alt_R" -> isAltGrActive = false
                    "Shift_L", "Shift_R" -> removeMod("Shift")
                    "Super_L", "Super_R" -> removeMod("Super")
                    "Fn" -> removeMod("Fn")
                }
            }
        } catch (_: Exception) {}
    }

    private fun appendChar(c: Char) {
        typedCharBuffer.add(TypedChar(charCounter++, c))
        if (typedCharBuffer.size > 80) { // Extended 80-char buffer for wide full-keyboard display
            typedCharBuffer.removeAt(0)
        }
    }

    private fun addMod(mod: String) {
        if (!activeModifiers.contains(mod)) activeModifiers.add(mod)
    }

    private fun removeMod(mod: String) {
        activeModifiers.remove(mod)
    }

    private fun mapFriendlyName(code: String): String = when (code) {
        "Escape" -> "Esc"
        "BackSpace" -> "Backspace"
        "Return" -> "Enter"
        "minus" -> "-"
        "equal" -> "="
        "bracketleft" -> "["
        "bracketright" -> "]"
        "backslash" -> "\\"
        "semicolon" -> ";"
        "apostrophe" -> "'"
        "comma" -> ","
        "period" -> "."
        "slash" -> "/"
        "grave" -> "`"
        "Prior" -> "PgUp"
        "Next" -> "PgDn"
        "Delete" -> "Del"
        "Print" -> "PrtSc"
        "Scroll_Lock" -> "ScrLk"
        "Pause" -> "Pause"
        else -> code
    }
}

@Composable
fun rememberKeycasterState(): KeycasterState = remember { KeycasterState() }

/**
 * osu!lazer Per-Letter Pop-In Animation using stable character ID tracking.
 */
@Composable
fun OsuLazerChar(
    char: Char,
    theme: ZenThemeConfig,
    modifier: Modifier = Modifier
) {
    var isSpawned by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isSpawned = true }

    val scale by animateFloatAsState(
        targetValue = if (isSpawned) 1.0f else 0.45f,
        animationSpec = spring(
            dampingRatio = 0.42f,
            stiffness = 650f
        ),
        label = "osuCharScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isSpawned) 1.0f else 0.0f,
        animationSpec = tween(80),
        label = "osuCharAlpha"
    )

    Text(
        text = if (char == ' ') " " else char.toString(),
        color = theme.textPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    )
}

/**
 * osu!lazer Style Animated Shortcut Pill Badge matching keyboard key aesthetic.
 */
@Composable
fun OsuLazerPill(
    text: String,
    theme: ZenThemeConfig,
    modifier: Modifier = Modifier,
    isShortcut: Boolean = false
) {
    var isSpawned by remember { mutableStateOf(false) }
    LaunchedEffect(text) { isSpawned = true }

    val scale by animateFloatAsState(
        targetValue = if (isSpawned) 1.0f else 0.75f,
        animationSpec = spring(
            dampingRatio = 0.45f,
            stiffness = 500f
        ),
        label = "osuPillScale"
    )

    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(6.dp),
        color = if (isShortcut) theme.keyAccentBg.copy(alpha = 0.85f) else theme.keyModBg,
        border = BorderStroke(1.dp, if (isShortcut) theme.primaryAccent else theme.chasisBorder)
    ) {
        Text(
            text = text,
            color = if (isShortcut) theme.keyAccentText else theme.keyModText,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

/**
 * Blinking Terminal Cursor Component (_ / |)
 */
@Composable
fun BlinkingCursor(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    Text(
        text = "_",
        color = color.copy(alpha = alpha),
        fontSize = 11.sp,
        fontWeight = FontWeight.Black
    )
}

/**
 * Top Bar Keycaster (Integrated in KeyboardScreen top bar with 100% horizontal expansion)
 */
@Composable
fun KeycasterTopBarContent(
    keycasterState: KeycasterState,
    theme: ZenThemeConfig,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Auto-scroll to end as user types
    LaunchedEffect(keycasterState.typedCharBuffer.size) {
        if (keycasterState.typedCharBuffer.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (keycasterState.lastShortcut.isNotEmpty()) {
                OsuLazerPill(text = keycasterState.lastShortcut, theme = theme, isShortcut = true)
                Spacer(modifier = Modifier.width(6.dp))
            }

            if (keycasterState.typedCharBuffer.isNotEmpty()) {
                keycasterState.typedCharBuffer.forEach { item ->
                    key(item.id) {
                        OsuLazerChar(char = item.char, theme = theme)
                    }
                }
                BlinkingCursor(color = theme.primaryAccent)
            } else if (keycasterState.lastShortcut.isEmpty()) {
                Text(
                    text = "READY // KEYCASTER ACTIVE",
                    color = theme.textMuted.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Integrated Mechanical OLED Display Module for Hybrid Screen.
 * Uses exact keyboard chassis colors (surface, chasisBorder, keyBg) for perfect visual harmony.
 */
@Composable
fun KeycasterFloatingHUD(
    keycasterState: KeycasterState,
    theme: ZenThemeConfig,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isEnabled) return

    val hasContent = keycasterState.typedCharBuffer.isNotEmpty() || keycasterState.lastShortcut.isNotEmpty()

    AnimatedVisibility(
        visible = keycasterState.isVisible && hasContent,
        enter = slideInVertically(initialOffsetY = { -40 }) + scaleIn(initialScale = 0.90f) + fadeIn(tween(120)),
        exit = slideOutVertically(targetOffsetY = { -40 }) + scaleOut(targetScale = 0.90f) + fadeOut(tween(180)),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = theme.surface.copy(alpha = 0.95f), // Integrated dark chassis OLED surface
            border = BorderStroke(1.5.dp, theme.chasisBorder), // Matches mechanical keyboard border
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (keycasterState.lastShortcut.isNotEmpty()) {
                    OsuLazerPill(text = keycasterState.lastShortcut, theme = theme, isShortcut = true)
                }

                if (keycasterState.typedCharBuffer.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        keycasterState.typedCharBuffer.forEach { item ->
                            key(item.id) {
                                OsuLazerChar(char = item.char, theme = theme)
                            }
                        }
                        BlinkingCursor(color = theme.primaryAccent)
                    }
                }
            }
        }
    }
}
