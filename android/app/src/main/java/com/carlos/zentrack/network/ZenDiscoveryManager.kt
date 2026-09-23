package com.carlos.zentrack.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

data class DiscoveredServer(
    val name: String,
    val ip: String,
    val port: Int,
    val token: String,
    val platform: String = "linux"
)

object ZenDiscoveryManager {
    private const val TAG = "ZenDiscovery"
    private const val DISCOVERY_PORT = 37020
    private val isDiscovering = AtomicBoolean(false)
    private var discoveryThread: Thread? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun startDiscovery(context: Context? = null, onServerFound: (DiscoveredServer) -> Unit) {
        if (isDiscovering.getAndSet(true)) return

        context?.let { ctx ->
            try {
                val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                multicastLock = wifi?.createMulticastLock("ZenTrackDiscoveryLock")?.apply {
                    setReferenceCounted(true)
                    acquire()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not acquire MulticastLock: ${e.message}")
            }
        }

        discoveryThread = Thread {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket().apply {
                    broadcast = true
                    soTimeout = 2000
                }

                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                val discoverMsg = JSONObject().apply {
                    put("cmd", "DISCOVER")
                    put("device", deviceName)
                }.toString()

                val discoverBytes = discoverMsg.toByteArray(Charsets.UTF_8)
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val sendPacket = DatagramPacket(discoverBytes, discoverBytes.size, broadcastAddr, DISCOVERY_PORT)

                // Send discovery probe to broadcast
                socket.send(sendPacket)

                // Also send directly to configured server IP if known
                val knownIp = com.carlos.zentrack.preferences.ZenPreferences.serverIp
                if (knownIp.isNotEmpty() && knownIp != "127.0.0.1") {
                    try {
                        val serverAddr = InetAddress.getByName(knownIp)
                        socket.send(DatagramPacket(discoverBytes, discoverBytes.size, serverAddr, DISCOVERY_PORT))
                    } catch (_: Exception) {}
                }

                val buffer = ByteArray(2048)
                val receivePacket = DatagramPacket(buffer, buffer.size)

                while (isDiscovering.get()) {
                    try {
                        socket.receive(receivePacket)
                        val responseText = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8)
                        val json = JSONObject(responseText)
                        val cmd = json.optString("cmd")

                        if (cmd == "ANNOUNCE" || cmd == "BEACON") {
                            val name = json.optString("name", "ZenTrack Host")
                            val ip = json.optString("ip", receivePacket.address.hostAddress ?: "")
                            val port = json.optInt("port", 3000)
                            val token = json.optString("token", "")
                            val platform = json.optString("platform", "linux")

                            if (ip.isNotEmpty() && token.isNotEmpty()) {
                                Log.d(TAG, "Found server: $name ($ip:$port)")
                                onServerFound(DiscoveredServer(name, ip, port, token, platform))
                            }
                        }
                    } catch (_: SocketTimeoutException) {
                        if (isDiscovering.get()) {
                            try {
                                socket.send(sendPacket)
                                if (knownIp.isNotEmpty() && knownIp != "127.0.0.1") {
                                    val serverAddr = InetAddress.getByName(knownIp)
                                    socket.send(DatagramPacket(discoverBytes, discoverBytes.size, serverAddr, DISCOVERY_PORT))
                                }
                            } catch (_: Exception) {}
                        }
                    } catch (e: Exception) {
                        if (isDiscovering.get()) {
                            Log.w(TAG, "Receive error: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Discovery error", e)
            } finally {
                socket?.close()
                isDiscovering.set(false)
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun stopDiscovery() {
        isDiscovering.set(false)
        discoveryThread?.interrupt()
        discoveryThread = null
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
        multicastLock = null
    }

    fun announceBroadcast(context: Context? = null) {
        Thread {
            try {
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                Log.d(TAG, "announceBroadcast started for device: $deviceName")
                val socket = DatagramSocket().apply { broadcast = true }
                val announceMsg = JSONObject().apply {
                    put("cmd", "CLIENT_ANNOUNCE")
                    put("device", deviceName)
                }.toString()
                val bytes = announceMsg.toByteArray(Charsets.UTF_8)

                // 1. Broadcast to 255.255.255.255
                try {
                    val addr = InetAddress.getByName("255.255.255.255")
                    socket.send(DatagramPacket(bytes, bytes.size, addr, DISCOVERY_PORT))
                    Log.d(TAG, "Sent broadcast to 255.255.255.255:$DISCOVERY_PORT")
                } catch (e: Exception) {
                    Log.w(TAG, "Error broadcasting: ${e.message}")
                }

                // 2. Direct to known server IP
                val knownIp = com.carlos.zentrack.preferences.ZenPreferences.serverIp
                if (knownIp.isNotEmpty() && knownIp != "127.0.0.1") {
                    try {
                        val serverAddr = InetAddress.getByName(knownIp)
                        socket.send(DatagramPacket(bytes, bytes.size, serverAddr, DISCOVERY_PORT))
                        Log.d(TAG, "Sent direct packet to $knownIp:$DISCOVERY_PORT")
                    } catch (e: Exception) {
                        Log.w(TAG, "Error direct packet: ${e.message}")
                    }
                }

                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to announce broadcast", e)
            }
        }.start()
    }



    fun announceClientToServer(serverIp: String) {
        Thread {
            try {
                val socket = DatagramSocket()
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                val announceMsg = JSONObject().apply {
                    put("cmd", "CLIENT_ANNOUNCE")
                    put("device", deviceName)
                }.toString()
                val bytes = announceMsg.toByteArray(Charsets.UTF_8)
                val addr = InetAddress.getByName(serverIp)
                val packet = DatagramPacket(bytes, bytes.size, addr, DISCOVERY_PORT)
                socket.send(packet)
                socket.close()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to announce client: ${e.message}")
            }
        }.start()
    }
}

