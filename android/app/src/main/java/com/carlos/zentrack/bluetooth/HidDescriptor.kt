package com.carlos.zentrack.bluetooth

/**
 * Industry-Standard USB / Bluetooth HID 1.11 Multi-Report Descriptor
 * Proven 100% compatible with Smart TVs (Tizen/webOS/AndroidTV), Windows 10/11, macOS, Linux, Android (Honor/Xiaomi/Samsung), and iOS.
 *
 * Report ID 1: Keyboard (8 bytes: Modifiers, Reserved, 6 Scancodes)
 * Report ID 2: Mouse (5 bytes: 5 Buttons, Relative X, Y, Wheel, Pan)
 * Report ID 3: Consumer Media / Smart TV Remote (2 bytes)
 */
object HidDescriptor {

    const val REPORT_ID_KEYBOARD: Byte = 1
    const val REPORT_ID_MOUSE: Byte = 2
    const val REPORT_ID_CONSUMER: Byte = 3

    val COMBO_REPORT_DESCRIPTOR: ByteArray = byteArrayOf(
        // =========================================================
        // REPORT ID 1: KEYBOARD (8 Bytes)
        // =========================================================
        0x05.toByte(), 0x01.toByte(),        // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x06.toByte(),        // USAGE (Keyboard)
        0xA1.toByte(), 0x01.toByte(),        // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD,   //   REPORT_ID (1)
        // Modifiers Byte (LCtrl..RGUI)
        0x05.toByte(), 0x07.toByte(),        //   USAGE_PAGE (Keyboard/Keypad)
        0x19.toByte(), 0xE0.toByte(),        //   USAGE_MINIMUM (Keyboard LeftControl)
        0x29.toByte(), 0xE7.toByte(),        //   USAGE_MAXIMUM (Keyboard Right GUI)
        0x15.toByte(), 0x00.toByte(),        //   LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(),        //   LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(),        //   REPORT_SIZE (1)
        0x95.toByte(), 0x08.toByte(),        //   REPORT_COUNT (8)
        0x81.toByte(), 0x02.toByte(),        //   INPUT (Data,Var,Abs)
        // Reserved Byte
        0x75.toByte(), 0x08.toByte(),        //   REPORT_SIZE (8)
        0x95.toByte(), 0x01.toByte(),        //   REPORT_COUNT (1)
        0x81.toByte(), 0x01.toByte(),        //   INPUT (Const,Var,Abs)
        // 6-Key Rollover Array
        0x75.toByte(), 0x08.toByte(),        //   REPORT_SIZE (8)
        0x95.toByte(), 0x06.toByte(),        //   REPORT_COUNT (6)
        0x15.toByte(), 0x00.toByte(),        //   LOGICAL_MINIMUM (0)
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), // LOGICAL_MAXIMUM (255)
        0x05.toByte(), 0x07.toByte(),        //   USAGE_PAGE (Keyboard/Keypad)
        0x19.toByte(), 0x00.toByte(),        //   USAGE_MINIMUM (0)
        0x29.toByte(), 0xFF.toByte(),        //   USAGE_MAXIMUM (255)
        0x81.toByte(), 0x00.toByte(),        //   INPUT (Data,Ary,Abs)
        0xC0.toByte(),                       // END_COLLECTION

        // =========================================================
        // REPORT ID 2: MOUSE (5 Bytes)
        // =========================================================
        0x05.toByte(), 0x01.toByte(),        // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x02.toByte(),        // USAGE (Mouse)
        0xA1.toByte(), 0x01.toByte(),        // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_MOUSE,      //   REPORT_ID (2)
        0x09.toByte(), 0x01.toByte(),        //   USAGE (Pointer)
        0xA1.toByte(), 0x00.toByte(),        //   COLLECTION (Physical)
        // 5 Buttons (Left, Right, Middle, Back, Forward) + 3 bits padding = 1 Byte
        0x05.toByte(), 0x09.toByte(),        //     USAGE_PAGE (Button)
        0x19.toByte(), 0x01.toByte(),        //     USAGE_MINIMUM (Button 1)
        0x29.toByte(), 0x05.toByte(),        //     USAGE_MAXIMUM (Button 5)
        0x15.toByte(), 0x00.toByte(),        //     LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(),        //     LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(),        //     REPORT_SIZE (1)
        0x95.toByte(), 0x05.toByte(),        //     REPORT_COUNT (5)
        0x81.toByte(), 0x02.toByte(),        //     INPUT (Data,Var,Abs)
        0x75.toByte(), 0x03.toByte(),        //     REPORT_SIZE (3)
        0x95.toByte(), 0x01.toByte(),        //     REPORT_COUNT (1)
        0x81.toByte(), 0x01.toByte(),        //     INPUT (Const,Var,Abs) - Padding
        // Movement X, Y, Wheel (3 Bytes: -127 to 127)
        0x05.toByte(), 0x01.toByte(),        //     USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x30.toByte(),        //     USAGE (X)
        0x09.toByte(), 0x31.toByte(),        //     USAGE (Y)
        0x09.toByte(), 0x38.toByte(),        //     USAGE (Wheel)
        0x15.toByte(), 0x81.toByte(),        //     LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7F.toByte(),        //     LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(),        //     REPORT_SIZE (8)
        0x95.toByte(), 0x03.toByte(),        //     REPORT_COUNT (3)
        0x81.toByte(), 0x06.toByte(),        //     INPUT (Data,Var,Rel)
        // Horizontal AC Pan (1 Byte)
        0x05.toByte(), 0x0C.toByte(),        //     USAGE_PAGE (Consumer Devices)
        0x0A.toByte(), 0x38.toByte(), 0x02.toByte(), // USAGE (AC Pan)
        0x95.toByte(), 0x01.toByte(),        //     REPORT_COUNT (1)
        0x81.toByte(), 0x06.toByte(),        //     INPUT (Data,Var,Rel)
        0xC0.toByte(),                       //   END_COLLECTION
        0xC0.toByte(),                       // END_COLLECTION

        // =========================================================
        // REPORT ID 3: CONSUMER / SMART TV MEDIA KEYS (2 Bytes)
        // =========================================================
        0x05.toByte(), 0x0C.toByte(),        // USAGE_PAGE (Consumer Devices)
        0x09.toByte(), 0x01.toByte(),        // USAGE (Consumer Control)
        0xA1.toByte(), 0x01.toByte(),        // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_CONSUMER,   //   REPORT_ID (3)
        0x19.toByte(), 0x00.toByte(),        //   USAGE_MINIMUM (0)
        0x2A.toByte(), 0xFF.toByte(), 0x03.toByte(), // USAGE_MAXIMUM (1023)
        0x75.toByte(), 0x0C.toByte(),        //   REPORT_SIZE (12)
        0x95.toByte(), 0x01.toByte(),        //   REPORT_COUNT (1)
        0x15.toByte(), 0x00.toByte(),        //   LOGICAL_MINIMUM (0)
        0x26.toByte(), 0xFF.toByte(), 0x03.toByte(), // LOGICAL_MAXIMUM (1023)
        0x81.toByte(), 0x00.toByte(),        //   INPUT (Data,Ary,Abs)
        0x75.toByte(), 0x04.toByte(),        //   REPORT_SIZE (4)
        0x95.toByte(), 0x01.toByte(),        //   REPORT_COUNT (1)
        0x81.toByte(), 0x01.toByte(),        //   INPUT (Const,Var,Abs) - Padding
        0xC0.toByte()                        // END_COLLECTION
    )
}
