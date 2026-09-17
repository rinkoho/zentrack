package com.carlos.zentrack.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.os.Process
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Universal Bluetooth Low Energy (BLE) HOGP Server (HID Over GATT Profile)
 * Specifically engineered for:
 * - Smart TVs (Google TV, Android TV, TCL, Sony, Samsung Tizen, LG webOS)
 * - Micro-stutter-free mobile/PC tracking via unacknowledged GATT notifications (0ms queue lag)
 */
@SuppressLint("MissingPermission")
object ZenBleHidServer {

    private const val TAG = "ZenBleHidServer"

    // 16-bit SIG UUID Converter
    private fun uuid(shortUuid: Int): UUID =
        UUID.fromString(String.format("0000%04X-0000-1000-8000-00805F9B34FB", shortUuid))

    // Bluetooth SIG GATT UUIDs
    val UUID_SERVICE_HID: UUID = uuid(0x1812)
    val UUID_CHAR_HID_INFO: UUID = uuid(0x2A4A)
    val UUID_CHAR_REPORT_MAP: UUID = uuid(0x2A4B)
    val UUID_CHAR_REPORT: UUID = uuid(0x2A4D)
    val UUID_CHAR_CONTROL_POINT: UUID = uuid(0x2A4C)
    val UUID_CHAR_PROTOCOL_MODE: UUID = uuid(0x2A4E)
    val UUID_DESC_CCCD: UUID = uuid(0x2902)
    val UUID_DESC_REPORT_REF: UUID = uuid(0x2908)

    val UUID_SERVICE_DEVICE_INFO: UUID = uuid(0x180A)
    val UUID_CHAR_MANUFACTURER: UUID = uuid(0x2A29)
    val UUID_CHAR_MODEL: UUID = uuid(0x2A24)
    val UUID_CHAR_PNP_ID: UUID = uuid(0x2A50)

    val UUID_SERVICE_BATTERY: UUID = uuid(0x180F)
    val UUID_CHAR_BATTERY_LEVEL: UUID = uuid(0x2A19)

    private var appContext: Context? = null
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null

    // State observable by Compose UI
    var isAdvertising by mutableStateOf(false)
        private set
    var isConnected by mutableStateOf(false)
        private set
    var connectedDeviceName by mutableStateOf<String?>(null)
        private set
    var connectedDeviceAddress by mutableStateOf<String?>(null)
        private set
    var statusMessage by mutableStateOf("BLE HOGP Inicializado")
        private set

    private var connectedDevice: BluetoothDevice? = null

    // HOGP Characteristics references for instant notifications
    private var mouseCharacteristic: BluetoothGattCharacteristic? = null
    private var keyboardCharacteristic: BluetoothGattCharacteristic? = null
    private var consumerCharacteristic: BluetoothGattCharacteristic? = null

    // Pre-allocated Report Buffers (Zero Allocation Hot-Path)
    private val mouseReportBuffer = ByteArray(5)
    private val keyboardReportBuffer = ByteArray(8)
    private val consumerReportBuffer = ByteArray(2)

    private var currentButtonsMask: Byte = 0
    private var currentModifiers: Byte = 0
    private val pressedKeyScancodes = LinkedHashSet<Byte>()

    // High-Precision Monotonic Clock Engine & Decoupled Producer-Consumer
    private var clockEngine: ZenHidClockEngine? = null
    private val deltaDistributor = ZenHidDeltaDistributor()

    // Dedicated Atomic Keyboard Dispatcher
    private val keyboardExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ZenTrack-BleHid-Keyboard").apply {
            priority = Thread.MAX_PRIORITY
        }
    }
    private val keyPressTimeStamps = ConcurrentHashMap<String, Long>()
    private const val MIN_KEY_DWELL_MS = 25L

    // Descriptor CCCD Storage
    private val cccdValues = ConcurrentHashMap<String, ByteArray>()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
            statusMessage = "Emitiendo señal BLE HID (Visible en Smart TV y PC)"
            Log.i(TAG, "BLE HID Advertising Started Successfully: $settingsInEffect")
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            statusMessage = "Fallo al emitir publicidad BLE (Código $errorCode)"
            Log.e(TAG, "BLE HID Advertising Failed: $errorCode")
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            Log.i(TAG, "BLE GATT Connection State Change: device=${device?.name} status=$status newState=$newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    val targetDev = device ?: bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT)?.firstOrNull() ?: connectedDevice
                    connectedDevice = targetDev
                    isConnected = true
                    connectedDeviceName = targetDev?.name ?: "Dispositivo BLE"
                    connectedDeviceAddress = targetDev?.address
                    statusMessage = "Conectado por BLE HOGP a ${targetDev?.name ?: targetDev?.address ?: "Dispositivo"}"
                    resetMouseAccumulator()
                    startBlePacer()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice == null || device == null || connectedDevice?.address == device.address) {
                        connectedDevice = null
                        isConnected = false
                        connectedDeviceName = null
                        connectedDeviceAddress = null
                        statusMessage = "Desconectado de BLE"
                        stopBlePacer()
                        resetMouseAccumulator()
                        // Only resume advertising if in Smart TV BLE mode
                        if (ZenBluetoothHidManager.currentSubMode == BluetoothSubMode.SMART_TV_BLE) {
                            startAdvertising()
                        }
                    }
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val charUuid = characteristic?.uuid ?: run {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                return
            }

            val value: ByteArray? = when (charUuid) {
                UUID_CHAR_HID_INFO -> {
                    // bcdHID: 0x0111 (HID 1.11), bCountryCode: 0, Flags: 0x02 (NormallyConnectable)
                    byteArrayOf(0x11, 0x01, 0x00, 0x02)
                }
                UUID_CHAR_REPORT_MAP -> {
                    HidDescriptor.COMBO_REPORT_DESCRIPTOR
                }
                UUID_CHAR_PROTOCOL_MODE -> {
                    byteArrayOf(0x01) // Report Protocol Mode
                }
                UUID_CHAR_REPORT -> {
                    when (characteristic) {
                        mouseCharacteristic -> mouseReportBuffer
                        keyboardCharacteristic -> keyboardReportBuffer
                        consumerCharacteristic -> consumerReportBuffer
                        else -> byteArrayOf(0)
                    }
                }
                UUID_CHAR_MANUFACTURER -> "ZenTrack".toByteArray(StandardCharsets.UTF_8)
                UUID_CHAR_MODEL -> "ZenTrack Universal HID".toByteArray(StandardCharsets.UTF_8)
                UUID_CHAR_PNP_ID -> {
                    // Vendor ID Source: 0x02 (USB-IF), Vendor ID: 0x046D (Logitech), Product ID: 0xB001, Version: 0x0100
                    byteArrayOf(0x02, 0x6D.toByte(), 0x04, 0x01, 0xB0.toByte(), 0x00, 0x01)
                }
                UUID_CHAR_BATTERY_LEVEL -> byteArrayOf(100) // 100% Battery
                else -> characteristic.value
            }

            if (value != null) {
                val sliced = if (offset < value.size) value.copyOfRange(offset, value.size) else byteArrayOf()
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, sliced)
            } else {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, byteArrayOf())
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            characteristic?.value = value
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            Log.i(TAG, "GATT onServiceAdded: uuid=${service?.uuid} status=$status")
            addNextService()
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor?
        ) {
            val descUuid = descriptor?.uuid
            val parentChar = descriptor?.characteristic

            val value: ByteArray = when (descUuid) {
                UUID_DESC_REPORT_REF -> {
                    when (parentChar) {
                        keyboardCharacteristic -> byteArrayOf(HidDescriptor.REPORT_ID_KEYBOARD, 0x01)
                        mouseCharacteristic -> byteArrayOf(HidDescriptor.REPORT_ID_MOUSE, 0x01)
                        consumerCharacteristic -> byteArrayOf(HidDescriptor.REPORT_ID_CONSUMER, 0x01)
                        else -> byteArrayOf(0x01, 0x01)
                    }
                }
                UUID_DESC_CCCD -> {
                    val key = "${device?.address}_${parentChar?.uuid}_${descUuid}"
                    cccdValues[key] ?: BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
                else -> descriptor?.value ?: byteArrayOf()
            }

            val sliced = if (offset < value.size) value.copyOfRange(offset, value.size) else byteArrayOf()
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, sliced)
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            val key = "${device?.address}_${descriptor?.characteristic?.uuid}_${descriptor?.uuid}"
            if (value != null) {
                cccdValues[key] = value
                descriptor?.value = value
            }
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }
    }

    private val servicesToAdd = ArrayDeque<BluetoothGattService>()
    @Volatile
    private var isAddingServices = false

    @Synchronized
    fun init(context: Context) {
        if (appContext != null && gattServer != null) return
        appContext = context.applicationContext
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        setupGattServer()
    }

    private fun setupGattServer() {
        val manager = bluetoothManager ?: return
        val context = appContext ?: return

        if (gattServer != null) return

        try {
            gattServer = manager.openGattServer(context, gattServerCallback)
            buildGattServices()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up BLE GATT Server", e)
        }
    }

    private fun buildGattServices() {
        if (isAddingServices) return
        servicesToAdd.clear()

        // 1. DEVICE INFORMATION SERVICE (0x180A)
        val devInfoService = BluetoothGattService(UUID_SERVICE_DEVICE_INFO, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        devInfoService.addCharacteristic(
            BluetoothGattCharacteristic(UUID_CHAR_MANUFACTURER, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
        )
        devInfoService.addCharacteristic(
            BluetoothGattCharacteristic(UUID_CHAR_MODEL, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
        )
        devInfoService.addCharacteristic(
            BluetoothGattCharacteristic(UUID_CHAR_PNP_ID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
        )
        servicesToAdd.add(devInfoService)

        // 2. BATTERY SERVICE (0x180F)
        val batteryService = BluetoothGattService(UUID_SERVICE_BATTERY, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val batteryChar = BluetoothGattCharacteristic(
            UUID_CHAR_BATTERY_LEVEL,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        ).apply {
            addDescriptor(BluetoothGattDescriptor(UUID_DESC_CCCD, BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE))
        }
        batteryService.addCharacteristic(batteryChar)
        servicesToAdd.add(batteryService)

        // 3. HUMAN INTERFACE DEVICE SERVICE (0x1812 - HOGP)
        val hidService = BluetoothGattService(UUID_SERVICE_HID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        // HID Info & Protocol Mode
        hidService.addCharacteristic(
            BluetoothGattCharacteristic(UUID_CHAR_HID_INFO, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
        )
        hidService.addCharacteristic(
            BluetoothGattCharacteristic(
                UUID_CHAR_PROTOCOL_MODE,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
            )
        )

        // Report Map (HID Descriptor)
        hidService.addCharacteristic(
            BluetoothGattCharacteristic(UUID_CHAR_REPORT_MAP, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
        )

        // HID Control Point
        hidService.addCharacteristic(
            BluetoothGattCharacteristic(UUID_CHAR_CONTROL_POINT, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE)
        )

        // Input Report 1: KEYBOARD (6KRO)
        val kbChar = BluetoothGattCharacteristic(
            UUID_CHAR_REPORT,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        ).apply {
            addDescriptor(BluetoothGattDescriptor(UUID_DESC_CCCD, BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE))
            val refDesc = BluetoothGattDescriptor(UUID_DESC_REPORT_REF, BluetoothGattDescriptor.PERMISSION_READ).apply {
                value = byteArrayOf(HidDescriptor.REPORT_ID_KEYBOARD, 0x01) // Report ID 1, Type: Input
            }
            addDescriptor(refDesc)
        }
        hidService.addCharacteristic(kbChar)
        keyboardCharacteristic = kbChar

        // Input Report 2: MOUSE (5 Buttons + Pan + Wheel)
        val mouseChar = BluetoothGattCharacteristic(
            UUID_CHAR_REPORT,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        ).apply {
            addDescriptor(BluetoothGattDescriptor(UUID_DESC_CCCD, BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE))
            val refDesc = BluetoothGattDescriptor(UUID_DESC_REPORT_REF, BluetoothGattDescriptor.PERMISSION_READ).apply {
                value = byteArrayOf(HidDescriptor.REPORT_ID_MOUSE, 0x01) // Report ID 2, Type: Input
            }
            addDescriptor(refDesc)
        }
        hidService.addCharacteristic(mouseChar)
        mouseCharacteristic = mouseChar

        // Input Report 3: SMART TV / CONSUMER REMOTE
        val consumerChar = BluetoothGattCharacteristic(
            UUID_CHAR_REPORT,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        ).apply {
            addDescriptor(BluetoothGattDescriptor(UUID_DESC_CCCD, BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE))
            val refDesc = BluetoothGattDescriptor(UUID_DESC_REPORT_REF, BluetoothGattDescriptor.PERMISSION_READ).apply {
                value = byteArrayOf(HidDescriptor.REPORT_ID_CONSUMER, 0x01) // Report ID 3, Type: Input
            }
            addDescriptor(refDesc)
        }
        hidService.addCharacteristic(consumerChar)
        consumerCharacteristic = consumerChar

        servicesToAdd.add(hidService)

        addNextService()
    }

    private fun addNextService() {
        val server = gattServer ?: return
        val nextService = servicesToAdd.poll()
        if (nextService != null) {
            isAddingServices = true
            val ok = server.addService(nextService)
            Log.d(TAG, "addService(${nextService.uuid}) -> ok=$ok")
        } else {
            isAddingServices = false
            Log.i(TAG, "All GATT Services Built and Registered Sequentially: HID 0x1812, DeviceInfo, Battery")
        }
    }

    fun startAdvertising() {
        if (isAdvertising) return
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return

        advertiser = adapter.bluetoothLeAdvertiser ?: run {
            Log.e(TAG, "BluetoothLeAdvertiser not supported on this hardware")
            statusMessage = "BLE Advertiser no soportado en este hardware"
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        // 1. Primary Advertising Packet: Strictly < 31 bytes
        // Only 16-bit Service UUID: 0x1812 (Human Interface Device)
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(UUID_SERVICE_HID))
            .setIncludeDeviceName(false) // Must be false to guarantee < 31 bytes legacy limit!
            .setIncludeTxPowerLevel(false)
            .build()

        // 2. Scan Response: Transmits device name on demand
        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build()

        try {
            advertiser?.stopAdvertising(advertiseCallback)
            advertiser?.startAdvertising(settings, data, scanResponse, advertiseCallback)
            Log.i(TAG, "startAdvertising called with standard 16-bit HID Service 0x1812")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting BLE Advertising", e)
            statusMessage = "Error al iniciar baliza BLE: ${e.message}"
        }
    }

    fun stopAdvertising() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            isAdvertising = false
            statusMessage = "Baliza BLE detenida"
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping BLE Advertising", e)
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        try {
            connectedDevice = device
            gattServer?.connect(device, false)
            statusMessage = "Conectando BLE con ${device.name ?: device.address}..."
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting BLE to device", e)
        }
    }

    fun disconnect() {
        stopBlePacer()
        val device = connectedDevice
        if (device != null) {
            try {
                gattServer?.cancelConnection(device)
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling BLE connection", e)
            }
        }
    }

    // =========================================================================
    // DEDICATED BLE CADENCE PACER THREAD (100Hz / 10ms Standard BLE Rate)
    // Producer-Consumer Architecture: Zero UI Thread Blocking
    // =========================================================================

    // =========================================================================
    // DEDICATED BLE CADENCE ENGINE (100Hz / 10ms Standard BLE Rate)
    // Producer-Consumer Architecture: Zero UI Thread Blocking
    // =========================================================================

    @Synchronized
    private fun startBlePacer() {
        if (clockEngine != null) return
        val engine = ZenHidClockEngine(
            name = "ZenTrack-BleHid",
            targetIntervalNs = 10_000_000L, // 10.0ms = 100Hz Cadence
            onTick = {
                if (deltaDistributor.produceMouseReport(mouseReportBuffer)) {
                    mouseReportBuffer.clone()
                } else {
                    null
                }
            },
            onTransmit = { report ->
                val device = connectedDevice
                val mouseChar = mouseCharacteristic
                if (device != null && mouseChar != null) {
                    notifyCharacteristic(device, mouseChar, report)
                }
            }
        ).apply {
            onReportDropped = { dx, dy, wheel, pan ->
                deltaDistributor.restoreMotion(dx, dy, wheel, pan)
            }
        }
        clockEngine = engine
        engine.start()
        Log.i(TAG, "ZenTrack-BleHid Clock Engine started (100Hz Monotonic)")
    }

    @Synchronized
    private fun stopBlePacer() {
        clockEngine?.stop()
        clockEngine = null
        deltaDistributor.reset()
        Log.i(TAG, "ZenTrack-BleHid Clock Engine stopped")
    }

    private fun resetMouseAccumulator() {
        deltaDistributor.reset()
    }

    // =========================================================================
    // NON-BLOCKING UI TOUCH PRODUCER METHODS (< 0.001ms Execution Time)
    // =========================================================================

    fun sendMouseMove(dx: Float, dy: Float) {
        if (!isConnected || connectedDevice == null) return
        deltaDistributor.pushMotion(dx, dy)
    }

    fun sendMouseScroll(dy: Float, dx: Float = 0f) {
        if (!isConnected || connectedDevice == null) return
        deltaDistributor.pushScroll(dy, dx)
    }

    fun sendMouseButton(button: Int, isDown: Boolean) {
        val mask = when (button) {
            1 -> 0x01.toByte() // Left Click
            3 -> 0x02.toByte() // Right Click
            2 -> 0x04.toByte() // Middle Click
            4 -> 0x08.toByte() // Back
            5 -> 0x10.toByte() // Forward
            else -> 0x00.toByte()
        }

        currentButtonsMask = if (isDown) {
            (currentButtonsMask.toInt() or mask.toInt()).toByte()
        } else {
            (currentButtonsMask.toInt() and mask.toInt().inv()).toByte()
        }
        deltaDistributor.setButtons(currentButtonsMask)
    }

    // =========================================================================
    // HIGH-PRIORITY ATOMIC KEYBOARD DISPATCH (Report ID 1: 8 Bytes, 6KRO)
    // =========================================================================

    fun sendKeyDown(key: String) {
        keyPressTimeStamps[key] = android.os.SystemClock.elapsedRealtime()
        val modMask = HidKeycodeMapper.getModifierMask(key)
        if (modMask != null) {
            currentModifiers = (currentModifiers.toInt() or modMask.toInt()).toByte()
        } else {
            val scancode = HidKeycodeMapper.getScancode(key)
            if (scancode != null) {
                if (pressedKeyScancodes.size >= 6) {
                    val first = pressedKeyScancodes.iterator().next()
                    pressedKeyScancodes.remove(first)
                }
                pressedKeyScancodes.add(scancode)
            }
        }
        dispatchKeyboardReport(isKeyUp = false)
    }

    fun sendKeyUp(key: String) {
        val downTime = keyPressTimeStamps.remove(key) ?: 0L
        val modMask = HidKeycodeMapper.getModifierMask(key)
        if (modMask != null) {
            currentModifiers = (currentModifiers.toInt() and modMask.toInt().inv()).toByte()
        } else {
            val scancode = HidKeycodeMapper.getScancode(key)
            if (scancode != null) {
                pressedKeyScancodes.remove(scancode)
            }
        }
        dispatchKeyboardReport(isKeyUp = true, downTime = downTime)
    }

    private fun dispatchKeyboardReport(isKeyUp: Boolean = false, downTime: Long = 0L) {
        val device = connectedDevice ?: return
        val kbChar = keyboardCharacteristic ?: return

        keyboardReportBuffer[0] = currentModifiers
        keyboardReportBuffer[1] = 0 // Reserved

        var idx = 2
        for (scancode in pressedKeyScancodes) {
            if (idx >= 8) break
            keyboardReportBuffer[idx] = scancode
            idx++
        }
        while (idx < 8) {
            keyboardReportBuffer[idx] = 0
            idx++
        }

        val reportSnapshot = keyboardReportBuffer.copyOf()

        // Atomic Priority: Pause mouse clock emissions for 40ms to keep BLE radio channel clear
        clockEngine?.notifyKeyboardActivity(40L)

        keyboardExecutor.execute {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
            } catch (_: Exception) {}

            // Enforce minimum 25ms key dwell time so host OS registers key without dropping or repeating
            if (isKeyUp && downTime > 0L) {
                val elapsed = android.os.SystemClock.elapsedRealtime() - downTime
                val remainingDwell = MIN_KEY_DWELL_MS - elapsed
                if (remainingDwell > 0L) {
                    try {
                        Thread.sleep(remainingDwell)
                    } catch (_: InterruptedException) {}
                }
            }

            notifyCharacteristic(device, kbChar, reportSnapshot)
        }
    }

    // =========================================================================
    // CONSUMER / SMART TV MEDIA KEYS (Report ID 3: 2 Bytes)
    // =========================================================================

    fun sendMediaKey(action: String) {
        val device = connectedDevice ?: return
        val consumerChar = consumerCharacteristic ?: return

        val usageCode: Int = when (action.lowercase()) {
            "vol_up", "volume_up" -> 0x00E9
            "vol_down", "volume_down" -> 0x00EA
            "mute" -> 0x00E2
            "play_pause", "play" -> 0x00CD
            "next", "next_track" -> 0x00B5
            "prev", "prev_track" -> 0x00B6
            "menu", "home" -> 0x0041
            "back" -> 0x0044
            else -> 0x0000
        }

        consumerReportBuffer[0] = (usageCode and 0xFF).toByte()
        consumerReportBuffer[1] = ((usageCode shr 8) and 0x0F).toByte()

        notifyCharacteristic(device, consumerChar, consumerReportBuffer)

        // Auto-release media key after 25ms
        Timer().schedule(object : TimerTask() {
            override fun run() {
                consumerReportBuffer[0] = 0
                consumerReportBuffer[1] = 0
                notifyCharacteristic(device, consumerChar, consumerReportBuffer)
            }
        }, 25L)
    }

    private fun notifyCharacteristic(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        val server = gattServer ?: return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                server.notifyCharacteristicChanged(device, characteristic, false, value)
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = value.clone()
                @Suppress("DEPRECATION")
                server.notifyCharacteristicChanged(device, characteristic, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying characteristic ${characteristic.uuid}", e)
        }
    }
}
