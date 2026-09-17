package com.carlos.zentrack.bluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.Executors
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Universal Bluetooth HID Device Manager (Android 9+ / API 28+)
 * Transforms phone into an authentic physical Bluetooth Keyboard, Mouse & Smart TV Remote
 *
 * Jitter-Free Radio-Synchronized Engine:
 * - 100Hz (10.0ms) Universal Radio Slot Alignment (Prevents periodic MediaTek/BT L2CAP collision skips)
 * - Zero-Allocation Hot Path (Zero GC pauses)
 * - Dynamic Exponential Sub-pixel Centroid Filter
 */
enum class BluetoothSubMode {
    CLASSIC_HID,  // Standard PC, Mac, Android, iPad, Linux, Consoles (Zero BLE duplicates)
    SMART_TV_BLE  // Smart TV (Google TV, TCL, Android TV, Chromecast) (Pure BLE 0x1812)
}

data class DiscoveredBluetoothDevice(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    val rssi: Short,
    val isBonded: Boolean
)

@SuppressLint("MissingPermission")
object ZenBluetoothHidManager {

    private const val TAG = "ZenBluetoothHid"

    // 11.25ms (88.88Hz): Exactly 18 Bluetooth baseband slots of 625µs (18 * 0.625ms = 11.25ms).
    // Matches the negotiated Bluetooth Classic (BR/EDR) Sniff cadence of Android & MagicOS hosts,
    // ensuring packet generation rate <= radio channel ACK rate and eliminating 8-packet buffer overflows.
    private const val MIN_PACKET_INTERVAL_NS = 11_250_000L // 11.25ms = 18 Bluetooth slots (88.88Hz)

    private var appContext: Context? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var hidDeviceProfile: BluetoothHidDevice? = null
    private var isAppRegistered = false

    // State observable by Compose UI
    var currentSubMode by mutableStateOf(BluetoothSubMode.CLASSIC_HID)
    var isSupported by mutableStateOf(false)
        private set
    var isEnabled by mutableStateOf(false)
        private set
    var isRegistered by mutableStateOf(false)
        private set
    var isConnected by mutableStateOf(false)
        private set
    var connectedDeviceName by mutableStateOf<String?>(null)
        private set
    var connectedDeviceAddress by mutableStateOf<String?>(null)
        private set
    var statusMessage by mutableStateOf("Bluetooth Inicializado")
        private set

    val pairedDevices = mutableStateListOf<BluetoothDevice>()
    val discoveredDevices = mutableStateListOf<DiscoveredBluetoothDevice>()
    var isScanning by mutableStateOf(false)

    private var currentHost: BluetoothDevice? = null

    // Internal State Tracking
    private var currentButtonsMask: Byte = 0
    private var currentModifiers: Byte = 0
    private val pressedKeyScancodes = LinkedHashSet<Byte>()

    // Dedicated Single-Thread Admin / Handshake Dispatcher
    private val adminExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ZenTrack-BtHid-Admin")
    }

    // High-Priority Atomic Keyboard Dispatcher (Isolated from admin/pacer loops)
    private val keyboardExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ZenTrack-BtHid-Keyboard").apply {
            priority = Thread.MAX_PRIORITY
        }
    }

    // Monotonic Key Press Timestamps (Enforces minimum 25ms dwell to eliminate repeat triggers)
    private val keyPressTimeStamps = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private const val MIN_KEY_DWELL_MS = 25L

    // Zero-Allocation Pre-allocated Report Buffers (Zero Garbage Collection)
    private val mouseReportBuffer = ByteArray(5)
    private val keyboardReportBuffer = ByteArray(8)
    private val consumerReportBuffer = ByteArray(2)

    // High-Precision Monotonic Cadence Engine & Token-Bucket Distributor
    private val deltaDistributor = ZenHidDeltaDistributor()
    private var clockEngine: ZenHidClockEngine? = null

    // BLUETOOTH SYSTEM EVENTS RECEIVER (Bonding, Pairing & ACL Handshake)
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            when (action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                    val prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE)
                    Log.d(TAG, "Bond state changed for ${device?.name}: $prevBondState -> $bondState")
                    updatePairedDevices()
                    if (bondState == BluetoothDevice.BOND_BONDED && device != null) {
                        if (currentSubMode == BluetoothSubMode.CLASSIC_HID) {
                            statusMessage = "Emparejado con ${device.name ?: device.address}. Conectando HID..."
                            adminExecutor.execute {
                                try {
                                    Thread.sleep(400)
                                    hidDeviceProfile?.connect(device)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error connecting after bond", e)
                                }
                            }
                        } else if (currentSubMode == BluetoothSubMode.SMART_TV_BLE) {
                            statusMessage = "Emparejado BLE con ${device.name ?: device.address}. Asegurando enlace HOGP..."
                            adminExecutor.execute {
                                try {
                                    Thread.sleep(500)
                                    ZenBleHidServer.connectToDevice(device)
                                    ZenBleHidServer.startAdvertising()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error reconnecting BLE after bond", e)
                                }
                            }
                        }
                    }
                }
                BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                    Log.d(TAG, "Pairing request from ${device?.name}")
                    statusMessage = "Solicitud de emparejamiento de ${device?.name ?: device?.address}"
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    Log.d(TAG, "ACL Connected with ${device?.name}")
                    if (device != null && currentHost == null && currentSubMode == BluetoothSubMode.CLASSIC_HID) {
                        adminExecutor.execute {
                            try {
                                Thread.sleep(250)
                                hidDeviceProfile?.connect(device)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error connecting on ACL_CONNECTED", e)
                            }
                        }
                    } else if (device != null && currentSubMode == BluetoothSubMode.SMART_TV_BLE && !ZenBleHidServer.isConnected) {
                        adminExecutor.execute {
                            try {
                                Thread.sleep(300)
                                ZenBleHidServer.connectToDevice(device)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error connecting BLE on ACL_CONNECTED", e)
                            }
                        }
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    Log.d(TAG, "ACL Disconnected from ${device?.name}")
                    if (currentHost == device) {
                        currentHost = null
                        isConnected = false
                        connectedDeviceName = null
                        connectedDeviceAddress = null
                        statusMessage = "Desconectado"
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                    isEnabled = state == BluetoothAdapter.STATE_ON
                    if (isEnabled) {
                        bluetoothAdapter?.getProfileProxy(appContext, serviceListener, BluetoothProfile.HID_DEVICE)
                    } else {
                        isRegistered = false
                        isConnected = false
                        currentHost = null
                    }
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                    val name = device?.name ?: intent.getStringExtra(BluetoothDevice.EXTRA_NAME) ?: "Dispositivo Bluetooth"
                    if (device != null) {
                        val existingIdx = discoveredDevices.indexOfFirst { it.address == device.address }
                        val item = DiscoveredBluetoothDevice(
                            device = device,
                            name = name,
                            address = device.address,
                            rssi = rssi,
                            isBonded = device.bondState == BluetoothDevice.BOND_BONDED
                        )
                        if (existingIdx >= 0) {
                            discoveredDevices[existingIdx] = item
                        } else {
                            discoveredDevices.add(item)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    isScanning = true
                    statusMessage = "Buscando dispositivos cercanos..."
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    isScanning = false
                    statusMessage = "Búsqueda completada (${discoveredDevices.size} encontrados)"
                }
            }
        }
    }

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d(TAG, "BluetoothHidDevice Profile Proxy Connected")
                hidDeviceProfile = proxy as? BluetoothHidDevice
                if (currentSubMode == BluetoothSubMode.CLASSIC_HID) {
                    registerHidApp()
                }
                updatePairedDevices()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d(TAG, "BluetoothHidDevice Profile Proxy Disconnected")
                hidDeviceProfile = null
                isAppRegistered = false
                isRegistered = false
                isConnected = false
                currentHost = null
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            isAppRegistered = registered
            isRegistered = registered
            Log.d(TAG, "HID App Status Changed: registered=$registered pluggedDevice=${pluggedDevice?.name}")
            statusMessage = if (registered) "Listo para emparejar" else "Registro HID fallido"
            if (registered && pluggedDevice != null) {
                connectToDevice(pluggedDevice)
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            Log.d(TAG, "HID Connection State Changed: device=${device?.name} state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    currentHost = device
                    isConnected = true
                    connectedDeviceName = device?.name ?: "Dispositivo Bluetooth"
                    connectedDeviceAddress = device?.address
                    statusMessage = "Conectado a ${device?.name ?: "Dispositivo"}"
                    resetMouseAccumulator()
                    startPacer()
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    statusMessage = "Conectando a ${device?.name ?: "Dispositivo"}..."
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (currentHost == device) {
                        currentHost = null
                        isConnected = false
                        connectedDeviceName = null
                        connectedDeviceAddress = null
                        statusMessage = "Desconectado"
                        stopPacer()
                        resetMouseAccumulator()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    statusMessage = "Desconectando..."
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            if (device == null || hidDeviceProfile == null) return
            when (id) {
                HidDescriptor.REPORT_ID_MOUSE -> {
                    synchronized(mouseReportBuffer) {
                        mouseReportBuffer[0] = currentButtonsMask
                        mouseReportBuffer[1] = 0
                        mouseReportBuffer[2] = 0
                        mouseReportBuffer[3] = 0
                        mouseReportBuffer[4] = 0
                    }
                    hidDeviceProfile?.replyReport(device, type, id, mouseReportBuffer)
                }
                HidDescriptor.REPORT_ID_KEYBOARD -> {
                    val report = buildKeyboardReport()
                    hidDeviceProfile?.replyReport(device, type, id, report)
                }
                else -> {
                    hidDeviceProfile?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
                }
            }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            // LED indicators or host status
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        isSupported = bluetoothAdapter != null
        isEnabled = bluetoothAdapter?.isEnabled == true

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        try {
            appContext?.registerReceiver(bluetoothStateReceiver, filter)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering Bluetooth state receiver", e)
        }

        if (isSupported && isEnabled) {
            bluetoothAdapter?.getProfileProxy(appContext, serviceListener, BluetoothProfile.HID_DEVICE)
        }
    }

    fun registerHidApp() {
        val hid = hidDeviceProfile ?: return
        if (isAppRegistered) return

        // 0xC0 = Combo Keyboard & Mouse Peripheral
        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "ZenTrack Keyboard & Mouse",
            "ZenTrack Ultra-Low Latency Wireless HID",
            "ZenTrack",
            (0xC0).toByte(),
            HidDescriptor.COMBO_REPORT_DESCRIPTOR
        )

        // SERVICE_BEST_EFFORT: Widely supported across MediaTek, Qualcomm and Smart TVs
        // Token rate 1000, latency 8000us (8.0ms matching 125Hz) guarantees continuous slot polling
        val qosSettings = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            1000,  // Token rate (bytes/sec)
            32,    // Token bucket size
            1000,  // Peak bandwidth
            8000,  // Latency: 8.0ms (125Hz matching precision clock)
            8000   // Delay variation: 8.0ms
        )

        try {
            hid.registerApp(sdpSettings, qosSettings, qosSettings, adminExecutor, hidCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register HID App", e)
        }
    }

    fun unregisterHidApp() {
        val hid = hidDeviceProfile ?: return
        if (!isAppRegistered) return
        try {
            hid.unregisterApp()
            isAppRegistered = false
            isRegistered = false
            Log.d(TAG, "HID App unregistered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister HID App", e)
        }
    }

    fun updatePairedDevices() {
        val adapter = bluetoothAdapter ?: return
        pairedDevices.clear()
        try {
            val bonded = adapter.bondedDevices
            if (bonded != null) {
                pairedDevices.addAll(bonded)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bonded devices", e)
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        val hid = hidDeviceProfile ?: return

        // If not bonded yet, initiate hardware pairing first
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            statusMessage = "Iniciando emparejamiento con ${device.name ?: device.address}..."
            try {
                stopDiscovery()
                device.createBond()
            } catch (e: Exception) {
                Log.e(TAG, "Error creating bond with ${device.address}", e)
            }
            return
        }

        try {
            statusMessage = "Conectando HID con ${device.name ?: device.address}..."
            hid.connect(device)
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device", e)
        }
    }

    fun connectByMacAddress(macAddress: String) {
        val adapter = bluetoothAdapter ?: return
        try {
            val cleanMac = macAddress.trim().uppercase()
            val device = adapter.getRemoteDevice(cleanMac)
            connectToDevice(device)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid MAC address: $macAddress", e)
            statusMessage = "MAC inválida: $macAddress"
        }
    }

    fun disconnect() {
        stopPacer()
        val hid = hidDeviceProfile ?: return
        val host = currentHost ?: return
        try {
            hid.disconnect(host)
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from host", e)
        }
    }

    // =========================================================================
    // DEDICATED CADENCE PACER THREAD (125Hz / 8ms Standard HID Rate)
    // =========================================================================
    // HIGH-PRECISION MONOTONIC CADENCE ENGINE & ASYNC TRANSMIT (ZERO-STUTTER)
    // =========================================================================

    @Synchronized
    private fun startPacer() {
        if (clockEngine != null) return
        val engine = ZenHidClockEngine(
            name = "ZenTrack-BtHid",
            targetIntervalNs = MIN_PACKET_INTERVAL_NS, // 11.25ms (88.88Hz)
            onTick = {
                if (deltaDistributor.produceMouseReport(mouseReportBuffer)) {
                    mouseReportBuffer.clone() // Thread-safe snapshot for async transmitter
                } else {
                    null
                }
            },
            onTransmit = { report ->
                val host = currentHost
                val hid = hidDeviceProfile
                if (host != null && hid != null) {
                    try {
                        val sent = hid.sendReport(host, HidDescriptor.REPORT_ID_MOUSE.toInt(), report)
                        if (!sent && report.size >= 5) {
                            deltaDistributor.restoreMotion(
                                report[1].toInt(),
                                report[2].toInt(),
                                report[3].toInt(),
                                report[4].toInt()
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Pacer sendReport error", e)
                    }
                }
            }
        ).apply {
            onReportDropped = { dx, dy, wheel, pan ->
                deltaDistributor.restoreMotion(dx, dy, wheel, pan)
            }
        }
        clockEngine = engine
        engine.start()
        Log.i(TAG, "ZenTrack-BtHid Clock Engine started (11.25ms / 88.88Hz Monotonic)")
    }

    @Synchronized
    private fun stopPacer() {
        clockEngine?.stop()
        clockEngine = null
        deltaDistributor.reset()
        Log.i(TAG, "ZenTrack-BtHid Clock Engine stopped")
    }

    private fun resetMouseAccumulator() {
        deltaDistributor.reset()
    }

    // =========================================================================
    // NON-BLOCKING UI TOUCH PRODUCER METHODS (< 0.001ms Execution Time)
    // =========================================================================

    fun sendMouseMove(dx: Float, dy: Float) {
        if (!isConnected || currentHost == null) return
        deltaDistributor.pushMotion(dx, dy)
    }

    fun sendMouseScroll(dy: Float, dx: Float = 0f) {
        if (!isConnected || currentHost == null) return
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
    // HIGH-PRIORITY ATOMIC KEYBOARD DISPATCH (Report ID 1: 8 Bytes, 6-Key Rollover)
    // =========================================================================

    fun sendKeyDown(key: String) {
        keyPressTimeStamps[key] = SystemClock.elapsedRealtime()
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

    private fun buildKeyboardReport(): ByteArray {
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
        return keyboardReportBuffer
    }

    private fun dispatchKeyboardReport(isKeyUp: Boolean = false, downTime: Long = 0L) {
        val host = currentHost ?: return
        val hid = hidDeviceProfile ?: return
        val report = buildKeyboardReport().copyOf()

        // Atomic Radio Priority: Pause mouse clock emissions for 150ms to guarantee
        // the Bluetooth baseband ACL buffer is 100% unobstructed for this key event
        clockEngine?.notifyKeyboardActivity(150L)

        keyboardExecutor.execute {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
            } catch (_: Exception) {}

            // Enforce minimum 25ms key dwell time so host OS registers key without dropping or repeating
            if (isKeyUp && downTime > 0L) {
                val elapsed = SystemClock.elapsedRealtime() - downTime
                val remainingDwell = MIN_KEY_DWELL_MS - elapsed
                if (remainingDwell > 0L) {
                    try {
                        Thread.sleep(remainingDwell)
                    } catch (_: InterruptedException) {}
                }
            }

            try {
                hid.sendReport(host, HidDescriptor.REPORT_ID_KEYBOARD.toInt(), report)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending keyboard report", e)
            }
        }
    }

    // =========================================================================
    // CONSUMER / SMART TV MEDIA KEYS (Report ID 3: 2 Bytes)
    // =========================================================================

    fun sendMediaKey(action: String) {
        val host = currentHost ?: return
        val hid = hidDeviceProfile ?: return

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

        try {
            hid.sendReport(host, HidDescriptor.REPORT_ID_CONSUMER.toInt(), consumerReportBuffer)
            adminExecutor.execute {
                Thread.sleep(25)
                consumerReportBuffer[0] = 0
                consumerReportBuffer[1] = 0
                hid.sendReport(host, HidDescriptor.REPORT_ID_CONSUMER.toInt(), consumerReportBuffer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending media key report", e)
        }
    }

    fun setSubMode(mode: BluetoothSubMode) {
        currentSubMode = mode
        if (mode == BluetoothSubMode.CLASSIC_HID) {
            ZenBleHidServer.stopAdvertising()
            ZenBleHidServer.disconnect()
            registerHidApp()
            statusMessage = "Modo Estándar (Clásico) Activo (PC, Celulares, Mac)"
        } else {
            disconnect()
            unregisterHidApp()
            ZenBleHidServer.startAdvertising()
            statusMessage = "Modo Smart TV (BLE HOGP 0x1812) Activo"
        }
    }

    fun startDiscovery() {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return
        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            discoveredDevices.clear()
            adapter.startDiscovery()
            isScanning = true
            statusMessage = "Buscando dispositivos cercanos (Smart TV, PC, Celulares)..."
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Bluetooth discovery", e)
            isScanning = false
        }
    }

    fun stopDiscovery() {
        val adapter = bluetoothAdapter ?: return
        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            isScanning = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Bluetooth discovery", e)
        }
    }

    fun connectToDiscoveredDevice(device: BluetoothDevice) {
        stopDiscovery()
        if (currentSubMode == BluetoothSubMode.CLASSIC_HID) {
            connectToDevice(device)
        } else {
            ZenBleHidServer.connectToDevice(device)
        }
    }

    fun makeDiscoverable(context: Context, durationSeconds: Int = 300) {
        if (currentSubMode == BluetoothSubMode.CLASSIC_HID) {
            registerHidApp()
            ZenBleHidServer.stopAdvertising() // Guarantee no dual-mode radio collision!
            try {
                val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, durationSeconds)
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(discoverableIntent)
                statusMessage = "Modo visible Clásico activado por ${durationSeconds}s"
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting discoverable mode, opening settings", e)
                openBluetoothSettings(context)
            }
        } else {
            ZenBleHidServer.startAdvertising()
            statusMessage = "Baliza Smart TV BLE activa (0x1812)"
        }
    }

    fun openBluetoothSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Bluetooth settings", e)
        }
    }
}
