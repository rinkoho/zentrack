package com.carlos.zentrack.bluetooth

/**
 * Ultra-Fast O(1) USB HID Keyboard Scancode & Modifier Mapper (Usage Page 0x07)
 */
object HidKeycodeMapper {

    // Modifiers bitmask
    const val MOD_LCTRL: Byte  = 0x01
    const val MOD_LSHIFT: Byte = 0x02
    const val MOD_LALT: Byte   = 0x04
    const val MOD_LGUI: Byte   = 0x08 // Windows / Super
    const val MOD_RCTRL: Byte  = 0x10
    const val MOD_RSHIFT: Byte = 0x20
    const val MOD_RALT: Byte   = 0x40 // AltGr
    const val MOD_RGUI: Byte   = 0x80.toByte()

    private val KEY_MAP = mapOf(
        // Letters (A-Z)
        "a" to 0x04.toByte(), "A" to 0x04.toByte(),
        "b" to 0x05.toByte(), "B" to 0x05.toByte(),
        "c" to 0x06.toByte(), "C" to 0x06.toByte(),
        "d" to 0x07.toByte(), "D" to 0x07.toByte(),
        "e" to 0x08.toByte(), "E" to 0x08.toByte(),
        "f" to 0x09.toByte(), "F" to 0x09.toByte(),
        "g" to 0x0A.toByte(), "G" to 0x0A.toByte(),
        "h" to 0x0B.toByte(), "H" to 0x0B.toByte(),
        "i" to 0x0C.toByte(), "I" to 0x0C.toByte(),
        "j" to 0x0D.toByte(), "J" to 0x0D.toByte(),
        "k" to 0x0E.toByte(), "K" to 0x0E.toByte(),
        "l" to 0x0F.toByte(), "L" to 0x0F.toByte(),
        "m" to 0x10.toByte(), "M" to 0x10.toByte(),
        "n" to 0x11.toByte(), "N" to 0x11.toByte(),
        "o" to 0x12.toByte(), "O" to 0x12.toByte(),
        "p" to 0x13.toByte(), "P" to 0x13.toByte(),
        "q" to 0x14.toByte(), "Q" to 0x14.toByte(),
        "r" to 0x15.toByte(), "R" to 0x15.toByte(),
        "s" to 0x16.toByte(), "S" to 0x16.toByte(),
        "t" to 0x17.toByte(), "T" to 0x17.toByte(),
        "u" to 0x18.toByte(), "U" to 0x18.toByte(),
        "v" to 0x19.toByte(), "V" to 0x19.toByte(),
        "w" to 0x1A.toByte(), "W" to 0x1A.toByte(),
        "x" to 0x1B.toByte(), "X" to 0x1B.toByte(),
        "y" to 0x1C.toByte(), "Y" to 0x1C.toByte(),
        "z" to 0x1D.toByte(), "Z" to 0x1D.toByte(),

        // Numbers (1-9, 0)
        "1" to 0x1E.toByte(),
        "2" to 0x1F.toByte(),
        "3" to 0x20.toByte(),
        "4" to 0x21.toByte(),
        "5" to 0x22.toByte(),
        "6" to 0x23.toByte(),
        "7" to 0x24.toByte(),
        "8" to 0x25.toByte(),
        "9" to 0x26.toByte(),
        "0" to 0x27.toByte(),

        // Controls & Function Keys
        "Return" to 0x28.toByte(), "Enter" to 0x28.toByte(), "enter" to 0x28.toByte(),
        "Escape" to 0x29.toByte(), "Esc" to 0x29.toByte(), "esc" to 0x29.toByte(),
        "BackSpace" to 0x2A.toByte(), "Backspace" to 0x2A.toByte(), "backspace" to 0x2A.toByte(),
        "Tab" to 0x2B.toByte(), "tab" to 0x2B.toByte(),
        "space" to 0x2C.toByte(), "Space" to 0x2C.toByte(), " " to 0x2C.toByte(),
        "minus" to 0x2D.toByte(), "-" to 0x2D.toByte(), "_" to 0x2D.toByte(),
        "equal" to 0x2E.toByte(), "=" to 0x2E.toByte(), "+" to 0x2E.toByte(),
        "bracketleft" to 0x2F.toByte(), "[" to 0x2F.toByte(), "{" to 0x2F.toByte(),
        "bracketright" to 0x30.toByte(), "]" to 0x30.toByte(), "}" to 0x30.toByte(),
        "backslash" to 0x31.toByte(), "\\" to 0x31.toByte(), "|" to 0x31.toByte(),
        "semicolon" to 0x33.toByte(), ";" to 0x33.toByte(), ":" to 0x33.toByte(),
        "apostrophe" to 0x34.toByte(), "'" to 0x34.toByte(), "\"" to 0x34.toByte(),
        "grave" to 0x35.toByte(), "`" to 0x35.toByte(), "~" to 0x35.toByte(),
        "comma" to 0x36.toByte(), "," to 0x36.toByte(), "<" to 0x36.toByte(),
        "period" to 0x37.toByte(), "." to 0x37.toByte(), ">" to 0x37.toByte(),
        "slash" to 0x38.toByte(), "/" to 0x38.toByte(), "?" to 0x38.toByte(),
        "Caps_Lock" to 0x39.toByte(), "CapsLock" to 0x39.toByte(), "Caps" to 0x39.toByte(),

        // Function Keys (F1-F12)
        "F1" to 0x3A.toByte(), "F2" to 0x3B.toByte(), "F3" to 0x3C.toByte(), "F4" to 0x3D.toByte(),
        "F5" to 0x3E.toByte(), "F6" to 0x3F.toByte(), "F7" to 0x40.toByte(), "F8" to 0x41.toByte(),
        "F9" to 0x42.toByte(), "F10" to 0x43.toByte(), "F11" to 0x44.toByte(), "F12" to 0x45.toByte(),

        // Navigation & Editing
        "Print" to 0x46.toByte(), "PrintScreen" to 0x46.toByte(),
        "Scroll_Lock" to 0x47.toByte(),
        "Pause" to 0x48.toByte(),
        "Insert" to 0x49.toByte(),
        "Home" to 0x4A.toByte(),
        "Page_Up" to 0x4B.toByte(), "Prior" to 0x4B.toByte(), "PGUP" to 0x4B.toByte(), "PgUp" to 0x4B.toByte(),
        "Delete" to 0x4C.toByte(), "Delete_Forward" to 0x4C.toByte(), "Del" to 0x4C.toByte(),
        "End" to 0x4D.toByte(), "END" to 0x4D.toByte(),
        "Page_Down" to 0x4E.toByte(), "Next" to 0x4E.toByte(), "PGDN" to 0x4E.toByte(), "PgDn" to 0x4E.toByte(),

        // Directional Arrow Keys
        "Right" to 0x4F.toByte(), "▶" to 0x4F.toByte(), "right" to 0x4F.toByte(),
        "Left" to 0x50.toByte(), "◀" to 0x50.toByte(), "left" to 0x50.toByte(),
        "Down" to 0x51.toByte(), "▼" to 0x51.toByte(), "down" to 0x51.toByte(),
        "Up" to 0x52.toByte(), "▲" to 0x52.toByte(), "up" to 0x52.toByte()
    )

    fun getModifierMask(key: String): Byte? {
        return when (key) {
            "Control_L", "Ctrl_L", "Control", "ctrl_l" -> MOD_LCTRL
            "Shift_L", "shift_l"                       -> MOD_LSHIFT
            "Alt_L", "alt_l"                           -> MOD_LALT
            "Super_L", "Super", "super_l", "Meta_L"    -> MOD_LGUI
            "Control_R", "Ctrl_R", "ctrl_r"            -> MOD_RCTRL
            "Shift_R", "shift_r"                       -> MOD_RSHIFT
            "Alt_R", "AltGr", "alt_r"                  -> MOD_RALT
            "Super_R", "super_r", "Meta_R"             -> MOD_RGUI
            else                                       -> null
        }
    }

    fun getScancode(key: String): Byte? {
        return KEY_MAP[key]
    }
}
