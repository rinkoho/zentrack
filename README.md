# 📱 ZenTrack Native (Remote Trackpad & Mechanical Keyboard v1.0.0)

**ZenTrack** es un controlador táctil de ultra-baja latencia y teclado mecánico 65% nativo para **Android (100% Kotlin + Jetpack Compose)** diseñado para controlar tu PC con Linux (BSPWM, Hyprland, X11, Wayland), Smart TVs y consolas directamente desde tu celular.

---

## ✨ Características Principales

* ⚡ **Protocolo Binario de 6 Bytes (Sub-Milisegundo):** Transmisión directa de deltas de movimiento y scroll sub-píxel sin la sobrecarga de parseo JSON.
* 🔌 **Conexión por Cable USB ADB (`127.0.0.1:3000`):** Conexión física a latencia ultra-baja de **<0.2ms** sin interferencias o jitter de red Wi-Fi.
* 🚀 **Ajuste KernelSU Root (`zentrack boost` / `zentrack unboost`):** Desbloqueo y fijación de la frecuencia del digitalizador a **`498 Hz – 501 Hz` estables** continuos sin caídas de energía.
* 📳 **Motor Háptico `ZenHapticsEngine` (LRA / RichTap):** Retroalimentación háptica HD de alta fidelidad con efectos predefinidos (CLICK, HEAVY_CLICK, TICK) y sliders independientes de intensidad para trackpad y teclado.
* 👁️ **Control Espacial `ZenVision` (MediaPipe Tasks Vision):** Control de puntero touchless en el aire con cámara frontal a 30 FPS, filtro adaptativo OneEuro, predictor cinemático y clics por pellizco (pinch-to-click).
* 🎮 **Gamepad Virtual Nativo Estilo Steam (`virtual_gamepad.py`):** Mando gaming con joystick WASD analógico, apuntado táctil de cámara, emulación física en el kernel de Linux (`/dev/uinput`) y editor de diseño visual en vivo.
* 🔤 **Keycaster OLED Dashboard & Motor osu!lazer:** Visualizador de teclas estilo teclado mecánico custom con animaciones spring de rebote y estabilidad de IDs inmutables `TypedChar`.
* ⌨️ **Mapeo Internacional `AltGr` (Level-3 ISO):** Soporte nativo para `ñ`, `Ñ`, vocales acentuadas (`á`, `é`, `í`, `ó`, `ú`) y símbolos `Shift`.
* 🎯 **Hitboxes sin Zonas Muertas (100% Target Area):** Rediseño del acolchado táctil en `KeyCap.kt` para tipeo ultra-rápido sin fallos.
* 🖐️ **Gesto de 3 Dedos Bidireccional & Inversión de Dirección:** Cambios de escritorio suaves en BSPWM/Hyprland con opción de inversión en Ajustes.
* 🔐 **Cifrado E2EE en Hardware (AES-256-GCM):** Pulsaciones de teclado y contraseñas cifradas en hardware mediante `javax.crypto.Cipher` y `crypto.createDecipheriv`.
* 🐧 **Emulación de Teclado Físico USB en Kernel (`/dev/uinput`):** Scancodes nativos de kernel Linux.
* 📶 **Periférico Físico Bluetooth Clásico HID (API 28+):** Control universal sin software receptor para PCs, Macs, móviles y consolas (SDP Combo Mouse/Keyboard `0xC0`).
* 📺 **Servidor Bluetooth Low Energy HOGP (`0x1812`):** Compatibilidad con Smart TVs modernas (TCL, Google TV, Android TV, webOS, Tizen).
* 📡 **Escáner Bluetooth Inverso Integrado:** Detección de televisores y dispositivos cercanos directamente desde la APK.
* ⚡ **Desacoplamiento Productor-Consumidor (Pacer Anti-Bloqueo):** Hilo en tiempo real a 125Hz/100Hz que aísla la pantalla táctil de bloqueos Binder IPC.
* 🔴 **Soporte Físico Infrarrojo (Consumer IR Blaster):** Control óptico (NEC 38kHz) para encender y apagar la TV incluso en modo Standby.
* 🎨 **23 Temas de gh0stzk rices:** Sincronización cromática en tiempo real entre la PC y la APK móvil.

---

## 🛠️ Comandos CLI del Servidor (`zentrack`)

El ejecutable CLI del sistema reside en `/home/carlos/.config/bspwm/bin/zentrack`:

| Comando | Descripción |
| :--- | :--- |
| `zentrack start` | Inicia el servidor Node.js y activa el túnel USB ADB (`adb reverse tcp:3000 tcp:3000`) |
| `zentrack status` | Muestra el estado del servidor, puerto e IPs conectadas |
| `zentrack boost` | **Activa el modo táctil 500Hz estables sin caídas vía KernelSU Root** |
| `zentrack unboost` | **Restaura el modo de ahorro de batería de fábrica de Xiaomi** |
| `zentrack log` | Muestra los registros del servidor en tiempo real |
| `zentrack stop` | Detiene el servidor y limpia subprocesos asociados |

---

## 🔬 Diagnóstico Forense Bluetooth (`zen_bt_diagnostic.py`)

Para auditar la calidad del enlace de radio Bluetooth, detectar tirones (>30ms), analizar dwell time de teclas y verificar transiciones de energía (Sniff Mode) directamente desde el chip Bluetooth del teléfono con ADB Root:

```bash
# Analizar los últimos 60 segundos de transmisión en vivo:
python3 tools/zen_bt_diagnostic.py --window 60
```

---

## 🚀 Compilación e Instalación (Debug / Release)

```bash
cd ~/Projects/zentrack/android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.carlos.zentrack/.MainActivity
```

---

## 📄 Documentación Técnica Completa & Handover

* **`docs/HANDOVER_TECHNICAL_SPEC.md`**: **Documento maestro de transferencia técnica** para desarrolladores y modelos de IA sucesores (análisis de aliasing, diagnósticos matemáticos del tirón, especificación BLE y Consumer IR).
* **`docs/PROPUESTA_ELIMINACION_STUTTER.md`**: **Propuesta técnica de regularización de reloj** para erradicar el tirón Bluetooth mediante temporizador monotónico compensado (`System.nanoTime()`), acumulación fraccional e instrumentación empírica.
* **`docs/GUIA_ESTUDIO_ZENTRACK.md`**: **Guía de estudio integral** con desglose didáctico módulo por módulo (de principiante a experto).
* **Bóveda de Obsidian (`Documents/ObsidianVault/Project Memory/remote-trackpad/`)**:
  * **`Remote Trackpad Home.md`**: Hub central MOC, ficha técnica y lista de hitos completados y pendientes.
  * **`Architecture.md`**: Diagramas de flujo, motor sub-píxel, E2EE, arquitectura Bluetooth HID y Consumer IR.
  * **`Decisions.md`**: Registro cronológico completo de todas las decisiones de ingeniería (ADRs).
  * **`Handover Spec.md`**: Versión interconectada del documento maestro de transferencia.
  * **`Propuesta Eliminación Stutter.md`**: Versión interconectada de la propuesta técnica anti-jitter.
  * **`Guía de Estudio.md`**: Versión interconectada de la guía didáctica de arquitectura.
