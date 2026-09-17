package com.carlos.zentrack.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.carlos.zentrack.bluetooth.*
import com.carlos.zentrack.theme.ZenThemeConfig

@Composable
fun BluetoothPairingDialog(
    show: Boolean,
    theme: ZenThemeConfig,
    onDismiss: () -> Unit
) {
    if (!show) return

    val context = LocalContext.current
    var macInput by remember { mutableStateOf("") }
    var selectedRightTab by remember { mutableIntStateOf(0) } // 0 = Vinculados, 1 = Buscar Nuevos
    val keyboardController = LocalSoftwareKeyboardController.current

    // Android 12+ Bluetooth Permissions Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            ZenBluetoothHidManager.init(context)
            ZenBluetoothHidManager.registerHidApp()
            ZenBluetoothHidManager.updatePairedDevices()
            ZenBleHidServer.init(context)
        }
    }

    // Check permissions on open
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN
            )
            val missing = permissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) {
                permissionLauncher.launch(permissions)
            } else {
                ZenBluetoothHidManager.updatePairedDevices()
                ZenBluetoothHidManager.registerHidApp()
                ZenBleHidServer.init(context)
            }
        } else {
            ZenBluetoothHidManager.updatePairedDevices()
            ZenBluetoothHidManager.registerHidApp()
            ZenBleHidServer.init(context)
        }
    }

    // Stop scanning when dialog closes
    DisposableEffect(Unit) {
        onDispose {
            ZenBluetoothHidManager.stopDiscovery()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = theme.card,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.92f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // 1. TOP HEADER BAR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = theme.primaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Conectividad Bluetooth Universal & Red",
                                    color = theme.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Smart TVs (Google TV/TCL), PC, Mac, iPad & Celulares (Sin software receptor)",
                                    color = theme.textMuted,
                                    fontSize = 8.5.sp
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = theme.textMuted)
                        }
                    }

                    HorizontalDivider(
                        color = theme.chasisBorder.copy(alpha = 0.4f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    // 2. TWO-COLUMN LANDSCAPE LAYOUT
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ==========================================
                        // LEFT COLUMN: MODES, SUB-MODES & STATUS
                        // ==========================================
                        Column(
                            modifier = Modifier
                                .weight(0.46f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            // Primary Mode Selector (Network 500Hz vs Bluetooth HID)
                            Surface(
                                color = theme.background.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, theme.chasisBorder.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    // Tab 1: Network Turbo
                                    Surface(
                                        color = if (ZenInputRouter.activeMode == ConnectionMode.NETWORK) theme.primaryAccent else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { ZenInputRouter.activeMode = ConnectionMode.NETWORK }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 5.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Speed,
                                                contentDescription = null,
                                                tint = if (ZenInputRouter.activeMode == ConnectionMode.NETWORK) Color.Black else theme.textMuted,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Red Turbo (500Hz)",
                                                color = if (ZenInputRouter.activeMode == ConnectionMode.NETWORK) Color.Black else theme.textMuted,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Tab 2: Bluetooth HID
                                    Surface(
                                        color = if (ZenInputRouter.activeMode == ConnectionMode.BLUETOOTH) theme.primaryAccent else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { ZenInputRouter.activeMode = ConnectionMode.BLUETOOTH }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 5.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Bluetooth,
                                                contentDescription = null,
                                                tint = if (ZenInputRouter.activeMode == ConnectionMode.BLUETOOTH) Color.Black else theme.textMuted,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Bluetooth HID",
                                                color = if (ZenInputRouter.activeMode == ConnectionMode.BLUETOOTH) Color.Black else theme.textMuted,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Clean Sub-Mode Switcher (Anti-Collision: Classic vs Smart TV BLE)
                            if (ZenInputRouter.activeMode == ConnectionMode.BLUETOOTH) {
                                Surface(
                                    color = theme.background.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, theme.chasisBorder.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "PERFIL BLUETOOTH ACTIVO (SELECCIONAR TIPO):",
                                            color = theme.secondaryAccent,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Submode 1: Classic HID
                                            val isClassicSelected = ZenBluetoothHidManager.currentSubMode == BluetoothSubMode.CLASSIC_HID
                                            Surface(
                                                color = if (isClassicSelected) theme.primaryAccent.copy(alpha = 0.2f) else theme.background.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, if (isClassicSelected) theme.primaryAccent else theme.chasisBorder.copy(alpha = 0.2f)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        ZenBluetoothHidManager.setSubMode(BluetoothSubMode.CLASSIC_HID)
                                                    }
                                            ) {
                                                Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Devices, contentDescription = null, tint = if (isClassicSelected) theme.primaryAccent else theme.textMuted, modifier = Modifier.size(11.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Estándar (PC/Móvil)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isClassicSelected) theme.primaryAccent else theme.textPrimary)
                                                    }
                                                    Text("PC, Mac, Honor, Consolas", fontSize = 6.5.sp, color = theme.textMuted)
                                                }
                                            }

                                            // Submode 2: Smart TV BLE
                                            val isBleSelected = ZenBluetoothHidManager.currentSubMode == BluetoothSubMode.SMART_TV_BLE
                                            Surface(
                                                color = if (isBleSelected) Color(0xFF38BDF8).copy(alpha = 0.2f) else theme.background.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, if (isBleSelected) Color(0xFF38BDF8) else theme.chasisBorder.copy(alpha = 0.2f)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        ZenBluetoothHidManager.setSubMode(BluetoothSubMode.SMART_TV_BLE)
                                                    }
                                            ) {
                                                Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Tv, contentDescription = null, tint = if (isBleSelected) Color(0xFF38BDF8) else theme.textMuted, modifier = Modifier.size(11.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Smart TV (BLE)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isBleSelected) Color(0xFF38BDF8) else theme.textPrimary)
                                                    }
                                                    Text("TCL, Google TV, Android TV", fontSize = 6.5.sp, color = theme.textMuted)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Connection Status Card
                            val isBleConnected = ZenBleHidServer.isConnected
                            val isClassicConnected = ZenBluetoothHidManager.isConnected
                            val isAnyConnected = isBleConnected || isClassicConnected
                            val connectedName = if (isBleConnected) ZenBleHidServer.connectedDeviceName else ZenBluetoothHidManager.connectedDeviceName

                            val currentStatus = when {
                                isBleConnected -> "Conectado por BLE HOGP a $connectedName"
                                isClassicConnected -> "Conectado por Bluetooth a $connectedName"
                                ZenBleHidServer.isAdvertising -> "Emitiendo señal BLE (0x1812) para Smart TV"
                                else -> ZenBluetoothHidManager.statusMessage
                            }

                            val modeDetail = when {
                                isBleConnected -> "Perfil BLE HOGP Activo (Cero tirones)"
                                isClassicConnected -> "Perfil Clásico Activo (Mouse + Teclado 6KRO)"
                                else -> "Búscalo como 'ZenTrack Keyboard & Mouse' o usa el escáner"
                            }

                            Surface(
                                color = theme.background.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isAnyConnected) Color(0xFF10B981) else theme.chasisBorder.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    if (isAnyConnected) Color(0xFF10B981) else if (ZenBleHidServer.isAdvertising) Color(0xFF38BDF8) else Color(0xFFEAB308),
                                                    CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = currentStatus,
                                                color = theme.textPrimary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = modeDetail,
                                                color = theme.textMuted,
                                                fontSize = 7.5.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    if (isAnyConnected) {
                                        Button(
                                            onClick = {
                                                ZenBluetoothHidManager.disconnect()
                                                ZenBleHidServer.disconnect()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)),
                                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Text("Desconectar", color = Color(0xFFEF4444), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Action Buttons: Discoverable & System Settings
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            permissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.BLUETOOTH_CONNECT,
                                                    Manifest.permission.BLUETOOTH_ADVERTISE,
                                                    Manifest.permission.BLUETOOTH_SCAN
                                                )
                                            )
                                        }
                                        if (ZenBluetoothHidManager.currentSubMode == BluetoothSubMode.CLASSIC_HID) {
                                            ZenBluetoothHidManager.makeDiscoverable(context, durationSeconds = 300)
                                        } else {
                                            ZenBleHidServer.startAdvertising()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.Black, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (ZenBluetoothHidManager.currentSubMode == BluetoothSubMode.CLASSIC_HID) "Hacer Visible (300s)" else "Emitir a TV (BLE)",
                                        color = Color.Black,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { ZenBluetoothHidManager.openBluetoothSettings(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.background.copy(alpha = 0.8f)),
                                    border = BorderStroke(1.dp, theme.chasisBorder.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = theme.textPrimary, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ajustes BT", color = theme.textPrimary, fontSize = 8.5.sp)
                                }
                            }

                            // Direct Connection by MAC Address (For Google TV / Hidden devices)
                            Surface(
                                color = theme.background.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, theme.chasisBorder.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "CONEXIÓN DIRECTA POR MAC (SI ESTÁ OCULTO)",
                                        color = theme.secondaryAccent,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = macInput,
                                            onValueChange = { macInput = it },
                                            placeholder = { Text("Ej. 38:65:04:47:97:59", fontSize = 8.sp, color = theme.textMuted) },
                                            singleLine = true,
                                            textStyle = LocalTextStyle.current.copy(
                                                fontSize = 8.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = theme.textPrimary
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = theme.primaryAccent,
                                                unfocusedBorderColor = theme.chasisBorder.copy(alpha = 0.4f),
                                                focusedContainerColor = theme.background.copy(alpha = 0.6f),
                                                unfocusedContainerColor = theme.background.copy(alpha = 0.6f)
                                            ),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = {
                                                keyboardController?.hide()
                                                if (macInput.isNotBlank()) {
                                                    ZenInputRouter.activeMode = ConnectionMode.BLUETOOTH
                                                    ZenBluetoothHidManager.connectByMacAddress(macInput)
                                                }
                                            }),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                        )

                                        Button(
                                            onClick = {
                                                keyboardController?.hide()
                                                if (macInput.isNotBlank()) {
                                                    ZenInputRouter.activeMode = ConnectionMode.BLUETOOTH
                                                    ZenBluetoothHidManager.connectByMacAddress(macInput)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("Conectar", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Vertical Separator
                        VerticalDivider(
                            color = theme.chasisBorder.copy(alpha = 0.35f),
                            thickness = 0.5.dp,
                            modifier = Modifier.fillMaxHeight()
                        )

                        // ==========================================
                        // RIGHT COLUMN: TABS (VINCULADOS vs BUSCAR)
                        // ==========================================
                        Column(
                            modifier = Modifier
                                .weight(0.54f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Tab Selector Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Tab 1: Vinculados
                                Surface(
                                    color = if (selectedRightTab == 0) theme.primaryAccent.copy(alpha = 0.2f) else theme.background.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (selectedRightTab == 0) theme.primaryAccent else theme.chasisBorder.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedRightTab = 0 }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 5.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Vinculados (${ZenBluetoothHidManager.pairedDevices.size})",
                                            color = if (selectedRightTab == 0) theme.primaryAccent else theme.textMuted,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Tab 2: Descubridor / Buscar Cercanos
                                Surface(
                                    color = if (selectedRightTab == 1) Color(0xFF38BDF8).copy(alpha = 0.2f) else theme.background.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (selectedRightTab == 1) Color(0xFF38BDF8) else theme.chasisBorder.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedRightTab = 1 }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 5.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (ZenBluetoothHidManager.isScanning) {
                                            CircularProgressIndicator(
                                                color = Color(0xFF38BDF8),
                                                strokeWidth = 1.5.dp,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = "Buscar Cercanos (${ZenBluetoothHidManager.discoveredDevices.size})",
                                            color = if (selectedRightTab == 1) Color(0xFF38BDF8) else theme.textMuted,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Refresh / Scan Action Button
                                IconButton(
                                    onClick = {
                                        if (selectedRightTab == 0) {
                                            ZenBluetoothHidManager.updatePairedDevices()
                                            ZenBluetoothHidManager.registerHidApp()
                                        } else {
                                            if (ZenBluetoothHidManager.isScanning) {
                                                ZenBluetoothHidManager.stopDiscovery()
                                            } else {
                                                ZenBluetoothHidManager.startDiscovery()
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = if (selectedRightTab == 1 && ZenBluetoothHidManager.isScanning) Icons.Default.Stop else Icons.Default.Refresh,
                                        contentDescription = "Acción",
                                        tint = if (selectedRightTab == 1 && ZenBluetoothHidManager.isScanning) Color(0xFFEF4444) else theme.primaryAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            // Container Box for Selected Tab
                            Surface(
                                color = theme.background.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, theme.chasisBorder.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                if (selectedRightTab == 0) {
                                    // ==========================================
                                    // TAB 1: PAIRED DEVICES LIST
                                    // ==========================================
                                    if (ZenBluetoothHidManager.pairedDevices.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.BluetoothSearching,
                                                    contentDescription = null,
                                                    tint = theme.textMuted,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "No hay dispositivos vinculados aún.",
                                                    color = theme.textPrimary,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "1. Pulsa en 'Buscar Cercanos' arriba para descubrir tu TV o PC.\n2. O pulsa 'Hacer Visible' a la izquierda para que te encuentren.",
                                                    color = theme.textMuted,
                                                    fontSize = 8.sp,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                    lineHeight = 12.sp
                                                )
                                            }
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            items(ZenBluetoothHidManager.pairedDevices) { device ->
                                                val isConnected = device.address == ZenBluetoothHidManager.connectedDeviceAddress ||
                                                        device.address == ZenBleHidServer.connectedDeviceAddress

                                                PairedDeviceRow(
                                                    device = device,
                                                    isConnected = isConnected,
                                                    theme = theme,
                                                    onConnect = {
                                                        ZenInputRouter.activeMode = ConnectionMode.BLUETOOTH
                                                        if (ZenBluetoothHidManager.currentSubMode == BluetoothSubMode.CLASSIC_HID) {
                                                            ZenBluetoothHidManager.connectToDevice(device)
                                                        } else {
                                                            ZenBleHidServer.connectToDevice(device)
                                                        }
                                                    },
                                                    onDisconnect = {
                                                        ZenBluetoothHidManager.disconnect()
                                                        ZenBleHidServer.disconnect()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // ==========================================
                                    // TAB 2: DISCOVERED DEVICES (RADAR SCANNER)
                                    // ==========================================
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Scan control bar inside tab
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(theme.chasisBorder.copy(alpha = 0.2f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (ZenBluetoothHidManager.isScanning) "Escaneando señales de radio..." else "Dispositivos encontrados cerca:",
                                                color = if (ZenBluetoothHidManager.isScanning) Color(0xFF38BDF8) else theme.textMuted,
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )

                                            Button(
                                                onClick = {
                                                    if (ZenBluetoothHidManager.isScanning) {
                                                        ZenBluetoothHidManager.stopDiscovery()
                                                    } else {
                                                        ZenBluetoothHidManager.startDiscovery()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (ZenBluetoothHidManager.isScanning) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF38BDF8).copy(alpha = 0.2f)
                                                ),
                                                border = BorderStroke(0.8.dp, if (ZenBluetoothHidManager.isScanning) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                                shape = RoundedCornerShape(5.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                modifier = Modifier.height(20.dp)
                                            ) {
                                                Text(
                                                    text = if (ZenBluetoothHidManager.isScanning) "Detener" else "Escanear Ahora",
                                                    fontSize = 7.5.sp,
                                                    color = if (ZenBluetoothHidManager.isScanning) Color(0xFFEF4444) else Color(0xFF38BDF8),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        if (ZenBluetoothHidManager.discoveredDevices.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    if (ZenBluetoothHidManager.isScanning) {
                                                        CircularProgressIndicator(
                                                            color = Color(0xFF38BDF8),
                                                            strokeWidth = 2.dp,
                                                            modifier = Modifier.size(26.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                            text = "Buscando Smart TVs, PCs y Celulares...",
                                                            color = Color(0xFF38BDF8),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = "Asegúrate de que el Bluetooth de tu TV/PC esté activado.",
                                                            color = theme.textMuted,
                                                            fontSize = 7.5.sp
                                                        )
                                                    } else {
                                                        Icon(
                                                            Icons.Default.Radar,
                                                            contentDescription = null,
                                                            tint = theme.textMuted,
                                                            modifier = Modifier.size(26.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            text = "Descubridor listo para buscar.",
                                                            color = theme.textPrimary,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "Pulsa 'Escanear Ahora' arriba para detectar tu Smart TV TCL o PC.",
                                                            color = theme.textMuted,
                                                            fontSize = 8.sp,
                                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                items(ZenBluetoothHidManager.discoveredDevices) { item ->
                                                    DiscoveredDeviceRow(
                                                        item = item,
                                                        theme = theme,
                                                        onConnect = {
                                                            ZenInputRouter.activeMode = ConnectionMode.BLUETOOTH
                                                            ZenBluetoothHidManager.connectToDiscoveredDevice(item.device)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairedDeviceRow(
    device: BluetoothDevice,
    isConnected: Boolean,
    theme: ZenThemeConfig,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    @SuppressLint("MissingPermission")
    val name = try {
        device.name ?: "Dispositivo (${device.address.takeLast(5)})"
    } catch (e: SecurityException) {
        "Dispositivo Bluetooth"
    }

    val isTv = name.contains("tv", ignoreCase = true) ||
            name.contains("tcl", ignoreCase = true) ||
            name.contains("beyond", ignoreCase = true) ||
            name.contains("chromecast", ignoreCase = true) ||
            name.contains("bravia", ignoreCase = true) ||
            name.contains("samsung", ignoreCase = true) ||
            name.contains("lg", ignoreCase = true)

    val isPhone = name.contains("honor", ignoreCase = true) ||
            name.contains("phone", ignoreCase = true) ||
            name.contains("celular", ignoreCase = true) ||
            name.contains("xiaomi", ignoreCase = true) ||
            name.contains("galaxy", ignoreCase = true)

    val isPc = name.contains("pc", ignoreCase = true) ||
            name.contains("laptop", ignoreCase = true) ||
            name.contains("desktop", ignoreCase = true) ||
            name.contains("windows", ignoreCase = true) ||
            name.contains("mac", ignoreCase = true)

    Surface(
        color = if (isConnected) theme.primaryAccent.copy(alpha = 0.14f) else theme.card.copy(alpha = 0.7f),
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(
            1.dp,
            if (isConnected) theme.primaryAccent else theme.chasisBorder.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    color = if (isConnected) theme.primaryAccent.copy(alpha = 0.2f) else theme.background.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                isTv -> Icons.Default.Tv
                                isPhone -> Icons.Default.Smartphone
                                isPc -> Icons.Default.Computer
                                else -> Icons.Default.Bluetooth
                            },
                            contentDescription = null,
                            tint = if (isConnected) theme.primaryAccent else theme.textPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            color = theme.textPrimary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (isConnected) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                        }
                    }
                    Text(
                        text = device.address,
                        color = theme.textMuted,
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (isConnected) {
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)),
                    border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(5.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Desconectar", color = Color(0xFFEF4444), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent),
                    shape = RoundedCornerShape(5.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Conectar", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DiscoveredDeviceRow(
    item: DiscoveredBluetoothDevice,
    theme: ZenThemeConfig,
    onConnect: () -> Unit
) {
    val name = item.name
    val isTv = name.contains("tv", ignoreCase = true) ||
            name.contains("tcl", ignoreCase = true) ||
            name.contains("beyond", ignoreCase = true) ||
            name.contains("chromecast", ignoreCase = true) ||
            name.contains("bravia", ignoreCase = true) ||
            name.contains("samsung", ignoreCase = true) ||
            name.contains("lg", ignoreCase = true)

    val isPhone = name.contains("honor", ignoreCase = true) ||
            name.contains("phone", ignoreCase = true) ||
            name.contains("celular", ignoreCase = true) ||
            name.contains("xiaomi", ignoreCase = true) ||
            name.contains("galaxy", ignoreCase = true)

    val isPc = name.contains("pc", ignoreCase = true) ||
            name.contains("laptop", ignoreCase = true) ||
            name.contains("desktop", ignoreCase = true) ||
            name.contains("windows", ignoreCase = true) ||
            name.contains("mac", ignoreCase = true)

    Surface(
        color = theme.card.copy(alpha = 0.7f),
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, theme.chasisBorder.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    color = theme.background.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                isTv -> Icons.Default.Tv
                                isPhone -> Icons.Default.Smartphone
                                isPc -> Icons.Default.Computer
                                else -> Icons.Default.Bluetooth
                            },
                            contentDescription = null,
                            tint = if (isTv) Color(0xFF38BDF8) else theme.primaryAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            color = theme.textPrimary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (item.isBonded) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Vinculado",
                                color = Color(0xFF10B981),
                                fontSize = 6.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.address,
                            color = theme.textMuted,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (item.rssi != Short.MIN_VALUE) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${item.rssi} dBm",
                                color = theme.primaryAccent.copy(alpha = 0.8f),
                                fontSize = 7.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent),
                shape = RoundedCornerShape(5.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text(
                    text = if (item.isBonded) "Conectar" else "Vincular",
                    color = Color.Black,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
