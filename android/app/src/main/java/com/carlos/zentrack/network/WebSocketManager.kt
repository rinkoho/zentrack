package com.carlos.zentrack.network

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors

class WebSocketManager(
    private val onStateChanged: (Boolean, String) -> Unit
) {
    private var webSocketClient: WebSocketClient? = null
    private val socketExecutor = Executors.newSingleThreadExecutor()

    fun connect(ip: String = "192.168.18.226", port: Int = 3000, token: String = "b8c5838d40a8746d2e79a7212e9f5f02") {
        socketExecutor.execute {
            try {
                val isUsbAdb = com.carlos.zentrack.preferences.ZenPreferences.usbAdbModeEnabled
                val targetIp = if (isUsbAdb) "127.0.0.1" else ip
                val serverUri = URI("ws://$targetIp:$port/?token=$token")
                webSocketClient?.close()

                webSocketClient = object : WebSocketClient(serverUri) {
                    override fun onOpen(handshakedata: ServerHandshake?) {
                        Log.d("ZenTrack", "WebSocket Connected Successfully! Mode: ${if (isUsbAdb) "USB ADB" else "Wi-Fi"}")
                        onStateChanged(true, if (isUsbAdb) "USB (Estable)" else "Wi-Fi (500Hz)")
                    }

                    override fun onMessage(message: String?) {
                        // Incoming server messages handler
                    }

                    override fun onClose(code: Int, reason: String?, remote: Boolean) {
                        Log.w("ZenTrack", "WebSocket Closed: code=$code, reason=$reason")
                        onStateChanged(false, "Desconectado")
                    }

                    override fun onError(ex: Exception?) {
                        Log.e("ZenTrack", "WebSocket Error", ex)
                        onStateChanged(false, "Error de Conexión")
                    }
                }
                webSocketClient?.isTcpNoDelay = true
                webSocketClient?.connect()
            } catch (e: Exception) {
                Log.e("ZenTrack", "Error in connectWebSocket", e)
                onStateChanged(false, "Error en IP")
            }
        }
    }

    fun sendBinary(cmd: Short, x: Int, y: Int) {
        socketExecutor.execute {
            if (webSocketClient?.isOpen == true) {
                try {
                    val buffer = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
                    buffer.putShort(cmd)
                    buffer.putShort(x.toShort())
                    buffer.putShort(y.toShort())
                    webSocketClient?.send(buffer.array())
                } catch (e: Exception) {
                    Log.e("ZenTrack", "Error sending binary packet", e)
                }
            }
        }
    }

    fun sendJson(jsonStr: String) {
        socketExecutor.execute {
            if (webSocketClient?.isOpen == true) {
                try {
                    webSocketClient?.send(jsonStr)
                } catch (e: Exception) {
                    Log.e("ZenTrack", "Error sending JSON packet", e)
                }
            }
        }
    }

    fun close() {
        try {
            webSocketClient?.close()
            socketExecutor.shutdown()
        } catch (e: Exception) {
            Log.e("ZenTrack", "Error closing socket manager", e)
        }
    }
}
