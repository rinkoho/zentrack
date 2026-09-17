package com.carlos.zentrack.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.carlos.zentrack.network.DiscoveredServer
import com.carlos.zentrack.network.ZenDiscoveryManager
import com.carlos.zentrack.preferences.ZenPreferences
import com.carlos.zentrack.theme.ZenThemeConfig

@Composable
fun ServerConnectionDialog(
    show: Boolean,
    currentTheme: ZenThemeConfig,
    onConnect: (ip: String, port: Int, token: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    val context = LocalContext.current
    var discoveredServers by remember { mutableStateOf<List<DiscoveredServer>>(emptyList()) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showManualFields by remember { mutableStateOf(false) }

    var manualIp by remember { mutableStateOf(ZenPreferences.serverIp) }
    var manualPort by remember { mutableStateOf(ZenPreferences.serverPort.toString()) }
    var manualToken by remember { mutableStateOf(ZenPreferences.serverToken) }

    // Start auto-discovery when dialog opens
    DisposableEffect(Unit) {
        ZenDiscoveryManager.startDiscovery(context) { server ->
            if (!discoveredServers.any { it.ip == server.ip && it.port == server.port }) {
                discoveredServers = discoveredServers + server
            }
        }
        onDispose {
            ZenDiscoveryManager.stopDiscovery()
        }
    }

    if (showQrScanner) {
        QrScannerDialog(
            show = true,
            currentTheme = currentTheme,
            onQrDecoded = { ip, port, token ->
                ZenPreferences.serverIp = ip
                ZenPreferences.serverPort = port
                ZenPreferences.serverToken = token
                onConnect(ip, port, token)
                showQrScanner = false
                onDismiss()
            },
            onDismiss = { showQrScanner = false }
        )
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                color = currentTheme.card,
                border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (!ZenPreferences.isConfigured) "👋 BIENVENIDO A ZENTRACK" else "📡 CONECTAR A TU PC",
                                color = currentTheme.primaryAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = if (!ZenPreferences.isConfigured) "Para comenzar, conéctate a tu PC usando una de las siguientes opciones:" else "Detección automática en red Wi-Fi o escaneo QR",
                                color = currentTheme.textMuted,
                                fontSize = 9.sp
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = currentTheme.textMuted)
                        }
                    }

                    // Quick Action Buttons (QR & USB)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // QR Scanner Button
                        Button(
                            onClick = { showQrScanner = true },
                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Escanear QR", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // USB ADB Button
                        OutlinedButton(
                            onClick = {
                                ZenPreferences.usbAdbModeEnabled = true
                                if (ZenPreferences.serverToken.isNotBlank()) {
                                    onConnect("127.0.0.1", ZenPreferences.serverPort, ZenPreferences.serverToken)
                                    onDismiss()
                                } else {
                                    // Auto-fetch token via local ADB reverse endpoint
                                    kotlin.concurrent.thread {
                                        try {
                                            val url = java.net.URL("http://127.0.0.1:${ZenPreferences.serverPort}/status")
                                            val conn = url.openConnection() as java.net.HttpURLConnection
                                            conn.connectTimeout = 1500
                                            conn.readTimeout = 1500
                                            if (conn.responseCode == 200) {
                                                val text = conn.inputStream.bufferedReader().readText()
                                                val json = org.json.JSONObject(text)
                                                val token = json.optString("token", "")
                                                if (token.isNotEmpty()) {
                                                    ZenPreferences.serverToken = token
                                                    ZenPreferences.serverIp = "127.0.0.1"
                                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                        onConnect("127.0.0.1", ZenPreferences.serverPort, token)
                                                        onDismiss()
                                                    }
                                                    return@thread
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("ZenTrack", "USB auto-pair: ${e.message}")
                                        }
                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                            onConnect("127.0.0.1", ZenPreferences.serverPort, ZenPreferences.serverToken)
                                            onDismiss()
                                        }
                                    }
                                }
                            },
                            border = BorderStroke(1.dp, currentTheme.primaryAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Usb, contentDescription = null, tint = currentTheme.primaryAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cable USB", color = currentTheme.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Auto-Discovery Section (Radar)
                    Text(
                        text = "DISPOSITIVOS ENCONTRADOS (RADAR LAN)",
                        color = currentTheme.primaryAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    if (discoveredServers.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            discoveredServers.forEach { server ->
                                Surface(
                                    color = currentTheme.surface.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(Color(0xFF10B981), RoundedCornerShape(4.dp))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(server.name, color = currentTheme.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text("IP: ${server.ip}:${server.port}", color = currentTheme.textMuted, fontSize = 9.sp)
                                        }
                                        Button(
                                            onClick = {
                                                ZenPreferences.serverIp = server.ip
                                                ZenPreferences.serverPort = server.port
                                                ZenPreferences.serverToken = server.token
                                                ZenPreferences.usbAdbModeEnabled = false
                                                onConnect(server.ip, server.port, server.token)
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("CONECTAR", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = currentTheme.surface.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, currentTheme.card),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = currentTheme.primaryAccent,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Buscando servidores ZenTrack en tu red...",
                                    color = currentTheme.textMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Collapsible Manual Configuration
                    HorizontalDivider(color = currentTheme.primaryAccent.copy(alpha = 0.2f), thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showManualFields = !showManualFields },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONFIGURACIÓN MANUAL",
                            color = currentTheme.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (showManualFields) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = currentTheme.textMuted
                        )
                    }

                    if (showManualFields) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualIp,
                                onValueChange = { manualIp = it },
                                label = { Text("IP de la PC (ej: 192.168.1.50)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = currentTheme.primaryAccent,
                                    unfocusedBorderColor = currentTheme.card
                                )
                            )

                            OutlinedTextField(
                                value = manualPort,
                                onValueChange = { manualPort = it },
                                label = { Text("Puerto (3000)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = currentTheme.primaryAccent,
                                    unfocusedBorderColor = currentTheme.card
                                )
                            )

                            OutlinedTextField(
                                value = manualToken,
                                onValueChange = { manualToken = it },
                                label = { Text("Token de Seguridad (32 caracteres)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.primaryClip?.getItemAt(0)?.text?.let {
                                            manualToken = it.toString().trim()
                                        }
                                    }) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Pegar", tint = currentTheme.primaryAccent)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = currentTheme.primaryAccent,
                                    unfocusedBorderColor = currentTheme.card
                                )
                            )

                            Button(
                                onClick = {
                                    val portInt = manualPort.toIntOrNull() ?: 3000
                                    ZenPreferences.serverIp = manualIp
                                    ZenPreferences.serverPort = portInt
                                    ZenPreferences.serverToken = manualToken
                                    onConnect(manualIp, portInt, manualToken)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Guardar & Conectar", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
