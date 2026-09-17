package com.carlos.zentrack

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Display
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.carlos.zentrack.network.WebSocketManager
import com.carlos.zentrack.theme.ClassicWhiteOrangeTheme
import com.carlos.zentrack.theme.findZenThemeByName
import com.carlos.zentrack.ui.screens.MainContainerScreen

class MainActivity : ComponentActivity() {
    private lateinit var socketManager: WebSocketManager
    private var isConnected by mutableStateOf(false)
    private var statusText by mutableStateOf("Conectando...")
    private var activeTheme by mutableStateOf(ClassicWhiteOrangeTheme)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enableFullScreenAndHighRefreshRate()

        com.carlos.zentrack.preferences.ZenPreferences.init(applicationContext)
        com.carlos.zentrack.vision.data.ZenVisionPreferences.init(applicationContext)
        com.carlos.zentrack.audio.ZenSoundEngine.init(applicationContext)
        com.carlos.zentrack.security.ZenCrypto.init("b8c5838d40a8746d2e79a7212e9f5f02")
        com.carlos.zentrack.haptics.ZenHapticsEngine.init(this, window.decorView)
        com.carlos.zentrack.bluetooth.ZenBluetoothHidManager.init(this)
        com.carlos.zentrack.bluetooth.ZenBleHidServer.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val btPermissions = arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.BLUETOOTH_SCAN
            )
            val missing = btPermissions.filter {
                checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) {
                requestPermissions(missing.toTypedArray(), 1001)
            }
        }

        // Load saved preferences
        activeTheme = findZenThemeByName(com.carlos.zentrack.preferences.ZenPreferences.activeThemeName)
        com.carlos.zentrack.audio.ZenSoundEngine.setEnabled(com.carlos.zentrack.preferences.ZenPreferences.keySoundEnabled)
        com.carlos.zentrack.audio.ZenSoundEngine.setProfile(com.carlos.zentrack.preferences.ZenPreferences.soundProfile)
        com.carlos.zentrack.audio.ZenSoundEngine.setVolume(com.carlos.zentrack.preferences.ZenPreferences.soundVolume)

        socketManager = WebSocketManager(
            onStateChanged = { connected, status ->
                runOnUiThread {
                    isConnected = connected
                    statusText = status
                }
            }
        )
        socketManager.connect()

        setContent {
            MaterialTheme {
                MainContainerScreen(
                    isConnected = isConnected,
                    statusText = statusText,
                    activeTheme = activeTheme,
                    onThemeChanged = {
                        activeTheme = it
                        com.carlos.zentrack.preferences.ZenPreferences.activeThemeName = it.name
                    },
                    onReconnect = { socketManager.connect() },
                    onSendBinary = { cmd, x, y ->
                        if (cmd == 1.toShort()) {
                            com.carlos.zentrack.bluetooth.ZenInputRouter.sendMouseMove(x.toFloat(), y.toFloat(), socketManager::sendBinary)
                        } else if (cmd == 2.toShort()) {
                            com.carlos.zentrack.bluetooth.ZenInputRouter.sendMouseScroll(x.toFloat(), y.toFloat(), socketManager::sendBinary)
                        } else {
                            socketManager.sendBinary(cmd, x, y)
                        }
                    },
                    onSendJson = { json ->
                        com.carlos.zentrack.bluetooth.ZenInputRouter.sendRawJson(json, socketManager::sendJson)
                    },
                    onVibrate = { com.carlos.zentrack.haptics.ZenHapticsEngine.vibrateRaw(it, isKeyboard = false) },
                    onVibrateKeyboard = { com.carlos.zentrack.haptics.ZenHapticsEngine.vibrateRaw(it, isKeyboard = true) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::socketManager.isInitialized && !isConnected) {
            socketManager.connect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socketManager.close()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && ev != null) {
            window.decorView.requestUnbufferedDispatch(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun enableFullScreenAndHighRefreshRate() {
        val window = window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val params = window.attributes
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) getDisplay() else @Suppress("DEPRECATION") windowManager.defaultDisplay
            if (display != null) {
                var maxRate = 60.0f
                var maxMode: Display.Mode? = null
                for (mode in display.supportedModes) {
                    if (mode.refreshRate > maxRate) {
                        maxRate = mode.refreshRate
                        maxMode = mode
                    }
                }
                if (maxMode != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    params.preferredDisplayModeId = maxMode.modeId
                }
                params.preferredRefreshRate = maxRate
            }
            window.attributes = params
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        hideSystemUI()
    }

    private fun hideSystemUI() {
        val window = window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            val controller = window.decorView.windowInsetsController
            controller?.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
            controller?.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.post {
                try {
                    window.decorView.requestUnbufferedDispatch(android.view.MotionEvent.ACTION_MOVE)
                } catch (e: Exception) {
                    // Fallback if unsupported on legacy API
                }
            }
        }
    }
}
