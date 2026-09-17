package com.carlos.zentrack.ui.components

import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.carlos.zentrack.theme.ZenThemeConfig
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@Composable
fun QrScannerDialog(
    show: Boolean,
    currentTheme: ZenThemeConfig,
    onQrDecoded: (ip: String, port: Int, token: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                color = currentTheme.card,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, currentTheme.primaryAccent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📷 ESCANEAR CÓDIGO QR",
                                color = currentTheme.primaryAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "Apunta al código QR en la pantalla de tu PC",
                                color = currentTheme.textMuted,
                                fontSize = 9.sp
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = currentTheme.textMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Camera Preview with Scanning Reticle
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, currentTheme.primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasCameraPermission) {
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx)
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    val cameraExecutor = Executors.newSingleThreadExecutor()

                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()

                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }

                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()

                                        imageAnalysis.setAnalyzer(cameraExecutor, QrCodeAnalyzer { rawText ->
                                            Log.d("ZenQr", "Scanned raw QR: $rawText")
                                            val (ip, port, token) = parseQrPayload(rawText)
                                            if (ip.isNotEmpty() && token.isNotEmpty()) {
                                                previewView.post {
                                                    onQrDecoded(ip, port, token)
                                                    onDismiss()
                                                }
                                            }
                                        })

                                        try {
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.DEFAULT_BACK_CAMERA,
                                                preview,
                                                imageAnalysis
                                            )
                                        } catch (e: Exception) {
                                            Log.e("ZenQr", "Use case binding failed", e)
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))

                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Futuristic QR viewfinder overlay
                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .border(2.dp, currentTheme.primaryAccent, RoundedCornerShape(16.dp))
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Permiso de cámara requerido",
                                    color = currentTheme.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Otorga permiso de cámara para escanear el QR",
                                    color = currentTheme.textMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "⚡ Conexión instantánea sin escribir IPs ni tokens",
                        color = currentTheme.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        val map = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))
        setHints(map)
    }
    private var isScanned = false

    override fun analyze(imageProxy: ImageProxy) {
        if (isScanned) {
            imageProxy.close()
            return
        }

        val buffer: ByteBuffer = imageProxy.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        val width = imageProxy.width
        val height = imageProxy.height

        val source = PlanarYUVLuminanceSource(
            data, width, height, 0, 0, width, height, false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            val result = reader.decodeWithState(bitmap)
            if (result != null && !isScanned) {
                isScanned = true
                onQrCodeScanned(result.text)
            }
        } catch (_: Exception) {
            // Keep scanning frame
        } finally {
            reader.reset()
            imageProxy.close()
        }
    }
}

fun parseQrPayload(text: String): Triple<String, Int, String> {
    try {
        if (text.startsWith("http://") || text.startsWith("https://")) {
            val uri = Uri.parse(text)
            val ip = uri.host ?: ""
            val port = if (uri.port > 0) uri.port else 3000
            val token = uri.getQueryParameter("token") ?: ""
            return Triple(ip, port, token)
        } else if (text.startsWith("zentrack://")) {
            val uri = Uri.parse(text)
            val ip = uri.getQueryParameter("ip") ?: uri.host ?: ""
            val port = uri.getQueryParameter("port")?.toIntOrNull() ?: 3000
            val token = uri.getQueryParameter("token") ?: ""
            return Triple(ip, port, token)
        } else if (text.length == 32 && !text.contains(" ")) {
            // Raw 32-char token, use current IP
            return Triple(com.carlos.zentrack.preferences.ZenPreferences.serverIp, 3000, text)
        }
    } catch (e: Exception) {
        Log.e("ZenQr", "Error parsing QR payload", e)
    }
    return Triple("", 3000, "")
}
