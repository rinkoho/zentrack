package com.carlos.zentrack.bluetooth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

enum class ConnectionMode {
    NETWORK,   // Wi-Fi UDP & USB ADB Tunnel (500Hz+ High-Speed Gaming)
    BLUETOOTH  // Universal Bluetooth HID (Smart TV, PC, Mac, iPad, Mobile)
}

/**
 * Unified Input Router: Transparently routes trackpad, keyboard, and gestures
 * to either Binary Network (500Hz USB/UDP), BLE HOGP (Smart TV / Mobile), or Classic Bluetooth HID
 */
object ZenInputRouter {

    var activeMode by mutableStateOf(ConnectionMode.NETWORK)

    val isBluetoothConnected: Boolean
        get() = ZenBleHidServer.isConnected || ZenBluetoothHidManager.isConnected

    val connectedBluetoothDeviceName: String?
        get() = if (ZenBleHidServer.isConnected) ZenBleHidServer.connectedDeviceName else ZenBluetoothHidManager.connectedDeviceName

    val activeBluetoothSubModeName: String
        get() = if (ZenBleHidServer.isConnected) "Smart TV (BLE HOGP)" else "PC / Consola (Classic HID)"

    fun isCurrentActiveConnected(isNetworkConnected: Boolean): Boolean {
        return if (activeMode == ConnectionMode.BLUETOOTH) isBluetoothConnected else isNetworkConnected
    }

    fun getActiveTitle(isNetworkConnected: Boolean): String {
        return if (activeMode == ConnectionMode.BLUETOOTH) {
            if (isBluetoothConnected) (connectedBluetoothDeviceName ?: "Dispositivo Bluetooth") else "Bluetooth Desconectado"
        } else {
            if (isNetworkConnected) "Conectado por Red" else "Red Desconectada"
        }
    }

    fun getActiveSubtitle(isNetworkConnected: Boolean, networkSubtitle: String = "Wi-Fi / USB (500Hz)"): String {
        return if (activeMode == ConnectionMode.BLUETOOTH) {
            if (isBluetoothConnected) {
                if (ZenBleHidServer.isConnected) "Smart TV (BLE HOGP @ 100Hz)" else "PC / Consola (Classic HID @ 125Hz)"
            } else {
                "Toca para emparejar o conectar"
            }
        } else {
            networkSubtitle
        }
    }

    fun toggleMode() {
        activeMode = if (activeMode == ConnectionMode.NETWORK) ConnectionMode.BLUETOOTH else ConnectionMode.NETWORK
    }

    fun sendMouseMove(dx: Float, dy: Float, onSendBinary: (Short, Int, Int) -> Unit) {
        if (activeMode == ConnectionMode.BLUETOOTH && isBluetoothConnected) {
            if (ZenBleHidServer.isConnected) {
                ZenBleHidServer.sendMouseMove(dx, dy)
            } else {
                ZenBluetoothHidManager.sendMouseMove(dx, dy)
            }
        } else {
            onSendBinary(1, dx.toInt(), dy.toInt())
        }
    }

    fun sendMouseButton(button: Int, isDown: Boolean, onSendJson: (String) -> Unit) {
        if (activeMode == ConnectionMode.BLUETOOTH && isBluetoothConnected) {
            if (ZenBleHidServer.isConnected) {
                ZenBleHidServer.sendMouseButton(button, isDown)
            } else {
                ZenBluetoothHidManager.sendMouseButton(button, isDown)
            }
        } else {
            val type = if (isDown) "mousedown" else "mouseup"
            onSendJson(JSONObject().put("type", type).put("button", button).toString())
        }
    }

    fun sendMouseClick(button: Int, onSendJson: (String) -> Unit) {
        if (activeMode == ConnectionMode.BLUETOOTH && isBluetoothConnected) {
            if (ZenBleHidServer.isConnected) {
                ZenBleHidServer.sendMouseButton(button, true)
                ZenBleHidServer.sendMouseButton(button, false)
            } else {
                ZenBluetoothHidManager.sendMouseButton(button, true)
                ZenBluetoothHidManager.sendMouseButton(button, false)
            }
        } else {
            onSendJson(JSONObject().put("type", "click").put("button", button).toString())
        }
    }

    fun sendMouseScroll(dy: Float, dx: Float = 0f, onSendBinary: (Short, Int, Int) -> Unit) {
        if (activeMode == ConnectionMode.BLUETOOTH && isBluetoothConnected) {
            if (ZenBleHidServer.isConnected) {
                ZenBleHidServer.sendMouseScroll(dy, dx)
            } else {
                ZenBluetoothHidManager.sendMouseScroll(dy, dx)
            }
        } else {
            onSendBinary(2, dy.toInt(), dx.toInt())
        }
    }

    fun sendKey(key: String, isDown: Boolean, onSendJson: (String) -> Unit) {
        if (activeMode == ConnectionMode.BLUETOOTH && isBluetoothConnected) {
            if (ZenBleHidServer.isConnected) {
                if (isDown) ZenBleHidServer.sendKeyDown(key) else ZenBleHidServer.sendKeyUp(key)
            } else {
                if (isDown) ZenBluetoothHidManager.sendKeyDown(key) else ZenBluetoothHidManager.sendKeyUp(key)
            }
        } else {
            val type = if (isDown) "keydown" else "keyup"
            onSendJson(JSONObject().put("type", type).put("key", key).toString())
        }
    }

    fun sendMediaKey(action: String, onSendJson: (String) -> Unit) {
        if (activeMode == ConnectionMode.BLUETOOTH && isBluetoothConnected) {
            if (ZenBleHidServer.isConnected) {
                ZenBleHidServer.sendMediaKey(action)
            } else {
                ZenBluetoothHidManager.sendMediaKey(action)
            }
        } else {
            onSendJson(JSONObject().put("type", "media_key").put("key", action).toString())
        }
    }

    fun sendRawJson(json: String, onSendJson: (String) -> Unit) {
        try {
            val obj = JSONObject(json)
            val type = obj.optString("type")
            if (activeMode == ConnectionMode.BLUETOOTH && isBluetoothConnected) {
                when (type) {
                    "keydown" -> {
                        val key = obj.optString("key")
                        if (ZenBleHidServer.isConnected) ZenBleHidServer.sendKeyDown(key) else ZenBluetoothHidManager.sendKeyDown(key)
                    }
                    "keyup" -> {
                        val key = obj.optString("key")
                        if (ZenBleHidServer.isConnected) ZenBleHidServer.sendKeyUp(key) else ZenBluetoothHidManager.sendKeyUp(key)
                    }
                    "mousedown" -> {
                        val btn = obj.optInt("button", 1)
                        if (ZenBleHidServer.isConnected) ZenBleHidServer.sendMouseButton(btn, true) else ZenBluetoothHidManager.sendMouseButton(btn, true)
                    }
                    "mouseup" -> {
                        val btn = obj.optInt("button", 1)
                        if (ZenBleHidServer.isConnected) ZenBleHidServer.sendMouseButton(btn, false) else ZenBluetoothHidManager.sendMouseButton(btn, false)
                    }
                    "click" -> {
                        val btn = obj.optInt("button", 1)
                        if (ZenBleHidServer.isConnected) {
                            ZenBleHidServer.sendMouseButton(btn, true)
                            ZenBleHidServer.sendMouseButton(btn, false)
                        } else {
                            ZenBluetoothHidManager.sendMouseButton(btn, true)
                            ZenBluetoothHidManager.sendMouseButton(btn, false)
                        }
                    }
                    "media_key" -> {
                        val key = obj.optString("key")
                        if (ZenBleHidServer.isConnected) ZenBleHidServer.sendMediaKey(key) else ZenBluetoothHidManager.sendMediaKey(key)
                    }
                    "scroll" -> {
                        val dir = obj.optString("direction", "up")
                        val steps = obj.optInt("steps", 1)
                        val dy = if (dir == "up") -steps.toFloat() * 12f else steps.toFloat() * 12f
                        if (ZenBleHidServer.isConnected) ZenBleHidServer.sendMouseScroll(dy, 0f) else ZenBluetoothHidManager.sendMouseScroll(dy, 0f)
                    }
                    else -> onSendJson(json)
                }
            } else {
                onSendJson(json)
            }
        } catch (e: Exception) {
            onSendJson(json)
        }
    }
}
