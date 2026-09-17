# 📱 ZenTrack (Remote Trackpad, 65% Keyboard & Virtual Gamepad)

**ZenTrack** es un controlador táctil de ultra-baja latencia, teclado mecánico 65% y mando virtual gaming para **Android (100% Kotlin + Jetpack Compose)** y servidor nativo de alto rendimiento en **Rust (Linux & Windows 10/11)**, diseñado para controlar tu PC, Smart TVs y consolas directamente desde tu teléfono móvil.

---

## ⚡ Arquitectura y Estado de Módulos

ZenTrack está diseñado con una arquitectura modular dividida en tres niveles de madurez comercial:

| Módulo | Estado | Plataformas | Descripción Técnica |
| :--- | :---: | :---: | :--- |
| **Trackpad Hi-Res & Gestos** | **COMERCIAL CORE** | Linux & Windows | Protocolo binario 6-bytes, scroll de alta resolución (`REL_WHEEL_HI_RES`), gestos multitoque (1 a 4 dedos) |
| **Teclado Mecánico 65%** | **COMERCIAL CORE** | Linux & Windows | Cifrado E2EE (AES-256-GCM), soporte AltGr Level-3 ISO (`ñ`, acentos), retroalimentación háptica HD RichTap |
| **Gamepad Virtual Steam** | **COMERCIAL CORE** | Linux (uinput) & Windows | Mando analógico estilo Xbox/Steam con sticks WASD, botones de acción y gatillos |
| **Servidor Nativo Rust** | **COMERCIAL CORE** | Linux & Windows | Binario único standalone (<15MB), sub-milisegundo, zero-dependency (no requiere Node.js, Python ni xdotool) |
| **ZenVision (Touchless Pointer)** | **BETA** | Android / PC | Puntero espacial en el aire con cámara frontal (MediaPipe Tasks Vision), filtro OneEuro y pinch-to-click |
| **Bluetooth Clásico / BLE HOGP** | **LABS / EXPERIMENTAL** | Multiplataforma | Conexión directa de hardware HOGP/HID (disponible en rama `experimental/ble-research`) |

---

## ✨ Características Principales

* 🦀 **Servidor Standalone Nativo en Rust:** Ejecutable portátil único para Linux (`/dev/uinput`) y Windows (`SendInput`). Elimina por completo las dependencias de Node.js, Python o utilidades externas como xdotool.
* ⚡ **Protocolo Binario de 6 Bytes (Sub-Milisegundo):** Empaquetado binario directo `[cmd: i16, x: i16, y: i16]` a través de WebSockets sin sobrecarga de parseo JSON.
* 🔌 **Conexión por Cable USB ADB (`127.0.0.1:3000`):** Conexión física a latencia ultra-baja de **<0.2ms** sin interferencias o jitter de radio Wi-Fi.
* 🚀 **Ajuste KernelSU Root (`zentrack boost` / `zentrack unboost`):** Desbloqueo y fijación de la frecuencia del digitalizador a **`498 Hz – 501 Hz` estables** continuos sin caídas de energía.
* 📳 **Motor Háptico `ZenHapticsEngine` (LRA / RichTap):** Retroalimentación háptica HD de alta fidelidad con efectos predefinidos (CLICK, HEAVY_CLICK, TICK) y sliders independientes de intensidad para trackpad y teclado.
* 🔐 **Cifrado E2EE en Hardware (AES-256-GCM):** Pulsaciones de teclado y contraseñas cifradas en hardware móvil (`javax.crypto.Cipher`) y descifradas a nivel de procesador con aceleración criptográfica en Rust.
* 🖱️ **Scroll Suave de Alta Resolución:** Eventos `REL_WHEEL_HI_RES` en Linux y `WHEEL_DELTA` en Windows para emulación fluida idéntica a trackpads de MacBook/Precision Touchpad.
* 🎨 **23 Temas Auténticos de la Comunidad:** Paleta cromática estándar (Dracula, Nord, Gruvbox, Tokyo Night, Catppuccin, Monokai Pro, One Dark, etc.) con transiciones suaves en Compose.
* 🔤 **Keycaster OLED Dashboard:** Visualizador de teclas estilo teclado mecánico custom con animaciones spring y estabilidad de IDs inmutables.
* ⌨️ **Mapeo Internacional `AltGr` (Level-3 ISO):** Soporte nativo para `ñ`, `Ñ`, vocales acentuadas (`á`, `é`, `í`, `ó`, `ú`) y símbolos `Shift`.
* 📱 **Cliente Web de Respaldo Embebido:** Interfaz web completa compilada directamente dentro del ejecutable Rust (`rust-embed`) para acceso inmediato desde cualquier navegador.

---

## 🛠️ Compilación y Ejecución del Servidor Rust

### Linux (Wayland / X11)

Requiere permisos de `/dev/uinput` (generalmente añadir tu usuario al grupo `input`):

```bash
# Compilar el binario release optimizado:
cd server-rust
cargo build --release

# Ejecutar el servidor:
./target/release/zentrack-server
```

Al iniciar, el servidor imprimirá un **código QR en la terminal** para emparejar automáticamente la APK escaneando la IP y el token de seguridad único.

### Windows 10 / 11

```powershell
cd server-rust
cargo build --release
.\target\release\zentrack-server.exe
```

---

## 📱 Compilación de la Aplicación Android

```bash
cd android
./gradlew assembleDebug

# Instalar en el dispositivo conectado por USB:
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.carlos.zentrack/.MainActivity
```

---

## 🔬 Diagnóstico y Herramientas

* **`server-rust/`**: Servidor oficial multiplataforma de producción.
* **`tools/zen_bt_diagnostic.py`**: Analizador de enlace y latencia de radio Bluetooth para pruebas forenses.
* **`docs/HANDOVER_TECHNICAL_SPEC.md`**: Especificación matemática del protocolo, modelado de latencia y arquitectura E2EE.

