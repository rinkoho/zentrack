package com.carlos.zentrack.vision.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.carlos.zentrack.theme.ZenThemeConfig
import com.carlos.zentrack.ui.components.BatteryBadge
import com.carlos.zentrack.vision.data.VisionFrameResult
import com.carlos.zentrack.vision.data.ZenVisionPreferences
import com.carlos.zentrack.vision.engine.HandLandmarkerHelper
import com.carlos.zentrack.vision.engine.KinematicMotionPredictor
import com.carlos.zentrack.vision.engine.PinchGestureEngine
import com.carlos.zentrack.vision.ui.components.VisionCameraPreview
import com.carlos.zentrack.vision.ui.components.VisionHudOverlay
import com.carlos.zentrack.vision.ui.components.VisionSettingsDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun VisionScreen(
    currentTheme: ZenThemeConfig,
    isConnected: Boolean,
    statusText: String,
    onOpenDrawer: () -> Unit,
    onReconnect: () -> Unit,
    onSendBinary: (Short, Int, Int) -> Unit,
    onSendJson: (String) -> Unit,
    onVibrate: (Long) -> Unit
) {
    val context = LocalContext.current

    // Preferences & UI State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var isPipMode by remember { mutableStateOf(ZenVisionPreferences.isPipMode) }
    var lensFacing by remember { mutableIntStateOf(ZenVisionPreferences.lensFacing) }
    var showSkeleton by remember { mutableStateOf(ZenVisionPreferences.showSkeleton) }
    var showTelemetry by remember { mutableStateOf(ZenVisionPreferences.showTelemetry) }
    var showFingertipLabels by remember { mutableStateOf(ZenVisionPreferences.showFingertipLabels) }
    var mirrorFront by remember { mutableStateOf(ZenVisionPreferences.mirrorFront) }
    var delegate by remember { mutableIntStateOf(ZenVisionPreferences.delegate) }
    var minConfidence by remember { mutableFloatStateOf(ZenVisionPreferences.minDetectionConfidence) }
    var maxHands by remember { mutableIntStateOf(ZenVisionPreferences.maxNumHands) }
    var renderMode by remember { mutableStateOf(ZenVisionPreferences.renderMode) }
    var isAirMouseEnabled by remember { mutableStateOf(ZenVisionPreferences.isAirMouseEnabled) }
    var visionSensitivity by remember { mutableFloatStateOf(ZenVisionPreferences.visionSensitivity) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var frameResult by remember { mutableStateOf(VisionFrameResult()) }

    // Kinematic Motion Predictor (500 Hz Forward Extrapolator)
    val motionPredictor = remember { KinematicMotionPredictor() }

    // Sub-Pixel Pinch-to-Click Engine
    val pinchEngine = remember {
        PinchGestureEngine(
            onPinchDown = {
                onVibrate(15L)
                onSendJson("{\"type\":\"mousedown\",\"button\":1}")
            },
            onPinchUp = {
                onVibrate(10L)
                onSendJson("{\"type\":\"mouseup\",\"button\":1}")
            }
        )
    }

    // MediaPipe Hand Landmarker Helper instance
    val handLandmarkerHelper = remember(delegate, minConfidence, maxHands) {
        HandLandmarkerHelper(
            context = context,
            minHandDetectionConfidence = minConfidence,
            minHandTrackingConfidence = minConfidence,
            minHandPresenceConfidence = minConfidence,
            maxNumHands = maxHands,
            currentDelegate = delegate,
            landmarkerListener = object : HandLandmarkerHelper.LandmarkerListener {
                override fun onError(error: String, errorCode: Int) {
                    // Handled internally
                }

                override fun onResults(result: VisionFrameResult) {
                    frameResult = result
                }
            }
        )
    }

    // Feed new anchor frames to the Kinematic Predictor & Pinch Engine
    LaunchedEffect(frameResult) {
        val hand = frameResult.hands.firstOrNull()
        if (hand != null && hand.indexTip != null) {
            motionPredictor.updateAnchor(hand.indexTip!!.x, hand.indexTip!!.y, frameResult.timestamp)
            pinchEngine.processHand(hand)
        } else {
            motionPredictor.reset()
            pinchEngine.reset()
        }
    }

    // 500 Hz High-Frequency Peripheral Emitter Thread (2ms loop)
    LaunchedEffect(isAirMouseEnabled, lensFacing, mirrorFront, visionSensitivity) {
        if (!isAirMouseEnabled) return@LaunchedEffect
        val isMirror = (lensFacing == CameraSelector.LENS_FACING_FRONT) && mirrorFront

        withContext(Dispatchers.Default) {
            while (isActive) {
                val delta = motionPredictor.evaluateIncrementalDelta(
                    leadTimeMs = 28.0f,
                    sensitivity = visionSensitivity,
                    mirrorX = isMirror
                )

                if (delta != null) {
                    // Send via fast 6-byte binary packet (CMD 1 = MOVE)
                    onSendBinary(1, delta.first, delta.second)
                }

                delay(2L)
            }
        }
    }

    DisposableEffect(handLandmarkerHelper) {
        onDispose {
            handLandmarkerHelper.clearHandLandmarker()
            motionPredictor.reset()
            pinchEngine.reset()
        }
    }

    if (!hasCameraPermission) {
        // Camera Permission Request Screen
        CameraPermissionRequestView(
            currentTheme = currentTheme,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onOpenDrawer = onOpenDrawer
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.background)
    ) {
        if (!isPipMode) {
            // ==========================================
            // FULLSCREEN CAMERA / SKELETON MODE
            // ==========================================
            if (renderMode == "skeleton") {
                // GAMING / ULTRA-LOW LATENCY: Camera feeds ML in background without rendering video to screen
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .background(Color.Transparent)
                ) {
                    VisionCameraPreview(
                        lensFacing = lensFacing,
                        renderMode = renderMode,
                        handLandmarkerHelper = handLandmarkerHelper
                    )
                }

                // Cyberpunk Background Pattern + HUD Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(currentTheme.background)
                ) {
                    VisionHudOverlay(
                        frameResult = frameResult,
                        currentTheme = currentTheme,
                        showSkeleton = showSkeleton,
                        showFingertipLabels = showFingertipLabels,
                        mirrorX = (lensFacing == CameraSelector.LENS_FACING_FRONT) && mirrorFront
                    )
                }
            } else {
                // HD or PERFORMANCE CAMERA PREVIEW
                Box(modifier = Modifier.fillMaxSize()) {
                    VisionCameraPreview(
                        lensFacing = lensFacing,
                        renderMode = renderMode,
                        handLandmarkerHelper = handLandmarkerHelper
                    )

                    VisionHudOverlay(
                        frameResult = frameResult,
                        currentTheme = currentTheme,
                        showSkeleton = showSkeleton,
                        showFingertipLabels = showFingertipLabels,
                        mirrorX = (lensFacing == CameraSelector.LENS_FACING_FRONT) && mirrorFront
                    )
                }
            }
        } else {
            // ==========================================
            // PICTURE-IN-PICTURE (PiP) MODE + DASHBOARD
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Panel: Spatial Telemetry & Angle Diagnostics
                Surface(
                    color = currentTheme.card,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    VisionTelemetryDashboard(
                        frameResult = frameResult,
                        currentTheme = currentTheme
                    )
                }

                // Right Panel: Floating PiP Camera Box
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, currentTheme.primaryAccent),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        VisionCameraPreview(
                            lensFacing = lensFacing,
                            handLandmarkerHelper = handLandmarkerHelper
                        )

                        VisionHudOverlay(
                            frameResult = frameResult,
                            currentTheme = currentTheme,
                            showSkeleton = showSkeleton,
                            showFingertipLabels = showFingertipLabels,
                            mirrorX = (lensFacing == CameraSelector.LENS_FACING_FRONT) && mirrorFront
                        )

                        // Mini PiP Tag
                        Surface(
                            color = currentTheme.primaryAccent,
                            shape = RoundedCornerShape(bottomEnd = 8.dp),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                text = "LIVE CAM",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // FLOATING ACTION BAR & TELEMETRY HUD (TOP)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Controls: Drawer + Mode Badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drawer Button
                Surface(
                    color = currentTheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {
                            onVibrate(15L)
                            onOpenDrawer()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = currentTheme.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // ZenVision Title Badge
                Surface(
                    color = currentTheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    color = if (frameResult.hands.isNotEmpty()) currentTheme.primaryAccent else currentTheme.textMuted,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ZENVISIÓN",
                            color = currentTheme.textPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Right Controls: Camera Flip, PiP Toggle, Settings, Status
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Telemetry Badge
                if (showTelemetry) {
                    Surface(
                        color = currentTheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.3f)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${frameResult.fps} FPS",
                                color = currentTheme.primaryAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "|",
                                color = currentTheme.textMuted.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${frameResult.inferenceTimeMs}ms",
                                color = currentTheme.secondaryAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Air Mouse Quick Toggle Button (500 Hz Binary Emitter)
                Surface(
                    color = if (isAirMouseEnabled) currentTheme.primaryAccent else currentTheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .height(36.dp)
                        .clickable {
                            onVibrate(15L)
                            isAirMouseEnabled = !isAirMouseEnabled
                            ZenVisionPreferences.isAirMouseEnabled = isAirMouseEnabled
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Mouse,
                            contentDescription = "Air Mouse",
                            tint = if (isAirMouseEnabled) Color.Black else currentTheme.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAirMouseEnabled) "MOUSE ON" else "MOUSE OFF",
                            color = if (isAirMouseEnabled) Color.Black else currentTheme.textPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Render Mode Quick Selector (HD -> 480p -> Gaming -> HD)
                Surface(
                    color = when (renderMode) {
                        "skeleton" -> currentTheme.secondaryAccent
                        "performance" -> currentTheme.primaryAccent
                        else -> currentTheme.surface.copy(alpha = 0.85f)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .height(36.dp)
                        .clickable {
                            onVibrate(15L)
                            renderMode = when (renderMode) {
                                "hd" -> "performance"
                                "performance" -> "skeleton"
                                else -> "hd"
                            }
                            ZenVisionPreferences.renderMode = renderMode
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (renderMode) {
                                "skeleton" -> "🎮 GAMING"
                                "performance" -> "⚡ 480P"
                                else -> "🎥 HD"
                            },
                            color = when (renderMode) {
                                "skeleton", "performance" -> Color.Black
                                else -> currentTheme.textPrimary
                            },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Camera Flip Button (Front / Back)
                Surface(
                    color = currentTheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {
                            onVibrate(15L)
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                CameraSelector.LENS_FACING_BACK
                            } else {
                                CameraSelector.LENS_FACING_FRONT
                            }
                            ZenVisionPreferences.lensFacing = lensFacing
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Cameraswitch,
                            contentDescription = "Cambiar Cámara",
                            tint = currentTheme.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // PiP Toggle Button
                Surface(
                    color = if (isPipMode) currentTheme.primaryAccent else currentTheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {
                            onVibrate(15L)
                            isPipMode = !isPipMode
                            ZenVisionPreferences.isPipMode = isPipMode
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPipMode) Icons.Default.Fullscreen else Icons.Default.PictureInPicture,
                            contentDescription = "PiP Mode",
                            tint = if (isPipMode) Color.Black else currentTheme.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // ZenVision Settings Button
                Surface(
                    color = currentTheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {
                            onVibrate(15L)
                            showSettingsDialog = true
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Ajustes ZenVision",
                            tint = currentTheme.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Battery Badge
                BatteryBadge(currentTheme = currentTheme)
            }
        }
    }

    // Modal Settings Dialog
    if (showSettingsDialog) {
        VisionSettingsDialog(
            currentTheme = currentTheme,
            lensFacing = lensFacing,
            showSkeleton = showSkeleton,
            showTelemetry = showTelemetry,
            mirrorFront = mirrorFront,
            delegate = delegate,
            minConfidence = minConfidence,
            maxHands = maxHands,
            renderMode = renderMode,
            onLensFacingChanged = {
                lensFacing = it
                ZenVisionPreferences.lensFacing = it
            },
            onShowSkeletonChanged = {
                showSkeleton = it
                ZenVisionPreferences.showSkeleton = it
            },
            onShowTelemetryChanged = {
                showTelemetry = it
                ZenVisionPreferences.showTelemetry = it
            },
            onMirrorFrontChanged = {
                mirrorFront = it
                ZenVisionPreferences.mirrorFront = it
            },
            onDelegateChanged = {
                delegate = it
                ZenVisionPreferences.delegate = it
            },
            onMinConfidenceChanged = {
                minConfidence = it
                ZenVisionPreferences.minDetectionConfidence = it
            },
            onMaxHandsChanged = {
                maxHands = it
                ZenVisionPreferences.maxNumHands = it
            },
            onRenderModeChanged = {
                renderMode = it
                ZenVisionPreferences.renderMode = it
            },
            isAirMouseEnabled = isAirMouseEnabled,
            visionSensitivity = visionSensitivity,
            onAirMouseEnabledChanged = {
                isAirMouseEnabled = it
                ZenVisionPreferences.isAirMouseEnabled = it
            },
            onVisionSensitivityChanged = {
                visionSensitivity = it
                ZenVisionPreferences.visionSensitivity = it
            },
            onDismissRequest = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun VisionTelemetryDashboard(
    frameResult: VisionFrameResult,
    currentTheme: ZenThemeConfig
) {
    val hand = frameResult.hands.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "MATRIZ DE TELEMETRÍA ESPACIAL",
            color = currentTheme.primaryAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Estado de Mano
            Surface(
                color = currentTheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("MANO ACTIVA", color = currentTheme.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (hand != null) (if (hand.isRightHand) "DERECHA [R]" else "IZQUIERDA [L]") else "SIN DETECCIÓN",
                        color = if (hand != null) currentTheme.primaryAccent else currentTheme.textMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Card 2: Confianza
            Surface(
                color = currentTheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("PRECISIÓN", color = currentTheme.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (hand != null) "${(hand.confidence * 100).toInt()}%" else "---",
                        color = currentTheme.secondaryAccent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Pinch Distance Metric (Index Tip [8] vs Thumb Tip [4])
        val pinchDistance = remember(hand) {
            if (hand != null && hand.indexTip != null && hand.thumbTip != null) {
                val dx = hand.indexTip!!.x - hand.thumbTip!!.x
                val dy = hand.indexTip!!.y - hand.thumbTip!!.y
                val dz = hand.indexTip!!.z - hand.thumbTip!!.z
                sqrt(dx * dx + dy * dy + dz * dz)
            } else 0f
        }

        Surface(
            color = currentTheme.surface,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("DISTANCIA PINCH (ÍNDICE - PULGAR)", color = currentTheme.textMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (pinchDistance < 0.08f && hand != null) "PINCH CLICK [ON]" else "ABIERTO",
                        color = if (pinchDistance < 0.08f && hand != null) currentTheme.primaryAccent else currentTheme.textMuted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (1.0f - (pinchDistance / 0.3f).coerceIn(0f, 1f)) },
                    color = currentTheme.primaryAccent,
                    trackColor = currentTheme.background,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
            }
        }

        // Live Normalized Landmark Coordinate Table
        Surface(
            color = currentTheme.surface,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("COORDENADAS 3D DE PUNTOS CLAVE", color = currentTheme.textMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                if (hand != null && hand.landmarks.size >= 21) {
                    val idx = hand.indexTip
                    val thb = hand.thumbTip
                    val wrs = hand.wrist
                    Text("• Wrist [0]:    X: %.3f | Y: %.3f | Z: %.3f".format(wrs?.x ?: 0f, wrs?.y ?: 0f, wrs?.z ?: 0f), color = currentTheme.textPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("• Thumb [4]:    X: %.3f | Y: %.3f | Z: %.3f".format(thb?.x ?: 0f, thb?.y ?: 0f, thb?.z ?: 0f), color = currentTheme.secondaryAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("• Index [8]:    X: %.3f | Y: %.3f | Z: %.3f".format(idx?.x ?: 0f, idx?.y ?: 0f, idx?.z ?: 0f), color = currentTheme.primaryAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                } else {
                    Text("Mueve tu mano frente a la cámara para inspeccionar vectores...", color = currentTheme.textMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionRequestView(
    currentTheme: ZenThemeConfig,
    onRequestPermission: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.background),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = currentTheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, currentTheme.primaryAccent.copy(alpha = 0.4f)),
            shadowElevation = 24.dp,
            modifier = Modifier.width(380.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = currentTheme.primaryAccent,
                    modifier = Modifier.size(42.dp)
                )

                Text(
                    text = "PERMISO DE CÁMARA REQUERIDO",
                    color = currentTheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "ZenVision procesa la visión artificial (MediaPipe Edge ML) localmente en tu teléfono para el seguimiento espacial de manos con cero latencia.",
                    color = currentTheme.textMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = currentTheme.primaryAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CONCEDER ACCESO A CÁMARA", fontSize = 11.5.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
