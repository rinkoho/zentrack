# 📱 ZenTrack (Ultra-Low Latency Trackpad, Mechanical Keyboard & Virtual Gamepad)

**ZenTrack** es una suite de periféricos virtuales de ultra-baja latencia que transforma tu teléfono móvil en un **Trackpad háptico de precisión**, un **Teclado mecánico 65%** y un **Mando virtual gaming oficial de Xbox 360**.

Construido con una arquitectura de grado comercial:
- **Cliente Móvil:** 100% Kotlin + Jetpack Compose con aceleración háptica HD (LRA / RichTap) y cifrado criptográfico por hardware.
- **Servidor PC Nativo:** 100% Rust asíncrono (`tokio` + `axum`), portable y autónomo para **Linux (Wayland / X11)** y **Windows 10 / 11**.
- **Instalador Oficial para Windows:** `ZenTrack-Setup.exe` todo-en-uno con dependencias integradas (ADB + ViGEmBus), sin scripts manuales ni configuración de terminal.

---

## ⚡ Arquitectura y Estado de Módulos

| Módulo | Estado | Plataformas | Descripción Técnica |
| :--- | :---: | :---: | :--- |
| **Trackpad Hi-Res & Gestos** | **COMERCIAL CORE** | Linux & Windows | Protocolo binario 6-bytes a 500Hz, scroll de alta resolución (`REL_WHEEL_HI_RES` / `WHEEL_DELTA`), gestos multitáctiles fluidos. |
| **Teclado Mecánico 65%** | **COMERCIAL CORE** | Linux & Windows | Scan codes reales por hardware (`MapVirtualKeyW`), bucle typematic de auto-repetición continua, cifrado E2EE (AES-256-GCM), soporte AltGr Level-3 ISO (`ñ`, acentos). |
| **Gamepad Virtual Xbox 360** | **COMERCIAL CORE** | Linux (`uinput`) & Windows (`ViGEmBus`) | Emulación nativa de mando Microsoft Xbox 360 (`0x045E:0x028E`). Reconocido al 100% por Steam, emuladores y navegadores (`hardwaretester.com/gamepad`). |
| **Servidor Nativo Rust** | **COMERCIAL CORE** | Linux & Windows | Binario standalone (<2 MB), latencia sub-milisegundo, zero-dependency. Sin Node.js, Python ni herramientas lentas como xdotool. |
| **Instalador Oficial Windows** | **COMERCIAL CORE** | Windows 10 / 11 | Instalador GUI nativo (`ZenTrack-Setup.exe`, 8.9 MB) con ADB de Google y driver ViGEmBus integrados, accesos directos automáticos. |
| **Centro Web de Emparejamiento** | **COMERCIAL CORE** | Web / Localhost | Dashboard reactivo Material Design 3 con barra de salud diagnóstica en vivo (5 indicadores en verde), escaneo QR y API de instalación en 1 clic. |
| **ZenVision (Touchless Pointer)** | **BETA** | Android / PC | Puntero espacial en el aire con cámara frontal (MediaPipe Tasks Vision), filtro OneEuro y pinch-to-click. |
| **Bluetooth Clásico / BLE HOGP** | **LABS / EXPERIMENTAL** | Multiplataforma | Conexión directa de hardware HOGP/HID para Smart TVs y dispositivos sin servidor. |

---

## ✨ Características Principales

* 🦀 **Servidor Standalone Nativo en Rust:** Binario único para Linux (`/dev/uinput`) y Windows (`SendInput` + `vigem-client`).
* 🎮 **Mando Xbox 360 Nativo en Windows:** Integración directa con el bus de emulación virtual **ViGEmBus**. Compatible con todos los juegos de PC con soporte XInput.
* ⌨️ **Teclado Fiel al Hardware:** Inyección de **Scan Codes** físicos con `KEYEVENTF_EXTENDEDKEY` y motor de auto-repetición (*typematic repeat*) a 30 Hz tras 300 ms de presión, eliminando discrepancias en navegadores y juegos.
* 🛡️ **Protección contra Bloqueos en Windows:** Desactivación automática del modo `ENABLE_QUICK_EDIT_MODE` de la consola de Windows para evitar que clicks accidentales congelen la conexión WebSocket.
* ⚡ **Protocolo Binario de 6 Bytes (Sub-Milisegundo):** Empaquetado binario directo `[cmd: i16, x: i16, y: i16]` sobre WebSockets sin sobrecarga de parseo JSON.
* 🔌 **Conexión por Cable USB ADB (`127.0.0.1:3000`):** Latencia física ultra-baja de **<0.2 ms** inmune a congestión o pérdidas de paquetes Wi-Fi.
* 🚀 **Ajuste KernelSU Root (`zentrack boost`):** Desbloqueo y fijación de la frecuencia del digitalizador táctil a **`498 Hz – 501 Hz` estables**.
* 📳 **Motor Háptico `ZenHapticsEngine` (LRA / RichTap):** Retroalimentación háptica HD de alta fidelidad con efectos predefinidos (CLICK, HEAVY_CLICK, TICK) y sliders independientes para trackpad y teclado.
* 🔐 **Cifrado E2EE en Hardware (AES-256-GCM):** Teclas y contraseñas cifradas en hardware móvil (`javax.crypto.Cipher`) y descifradas a nivel de procesador en Rust.
* 🖱️ **Scroll Suave de Alta Resolución:** Eventos `REL_WHEEL_HI_RES` en Linux y `WHEEL_DELTA` en Windows para emulación fluida idéntica a los mejores trackpads físicos.
* 🎨 **23 Temas Auténticos de la Comunidad:** Dracula, Nord, Gruvbox, Tokyo Night, Catppuccin, Monokai Pro, One Dark, Sunset Peach, etc.
* 🚦 **Centro Diagnóstico en Vivo (`pair.html`):** Monitoreo en tiempo real del estado de los 5 pilares de conexión:
  1. Driver de Ratón / Teclado
  2. Driver de Mando Xbox 360 (ViGEmBus)
  3. Herramienta de Conexión en PC (ADB)
  4. Dispositivo Móvil USB (Detección de modelo y estado de autorización)
  5. Túnel Inverso 500Hz (`adb reverse tcp:3000`)

---

## 📦 Descarga e Instalación para Windows 10 / 11

### Opción A: Instalador Automático Oficial (Recomendado)
Descarga y ejecuta **`ZenTrack-Setup.exe`**:
* Instala ZenTrack en `%LOCALAPPDATA%\Programs\ZenTrack` sin requerir permisos de administrador para la app base.
* Comprueba si el driver de mando Xbox (ViGEmBus) está presente; si no lo está, lanza el instalador oficial solicitando elevación de Administrador una sola vez.
* Incluye las herramientas oficiales de conexión USB de Google (`adb.exe`, `AdbWinApi.dll`, `AdbWinUsbApi.dll`).
* Crea accesos directos en el **Escritorio** y en el **Menú Inicio**.
* Inicia el servidor y abre el Centro de Emparejamiento en tu navegador.

### Opción B: Paquete Portable (.zip)
Descarga **`ZenTrack-Windows-x64-Portable.zip`**, descomprímelo en cualquier carpeta y ejecuta `ZenTrack.exe`.

---

## 🛠️ Compilación desde Código Fuente

### 1. Servidor Rust en Linux
```bash
# Compilar binario optimizado para Linux:
cd server-rust
cargo build --release

# Ejecutar:
./target/release/zentrack-server
```

### 2. Compilación Cruzada para Windows desde Linux (MinGW)
```bash
# Instalar toolchain MinGW y target de Rust:
rustup target add x86_64-pc-windows-gnu

# Compilar el servidor de Windows:
cd server-rust
cargo build --target x86_64-pc-windows-gnu --release

# Compilar el instalador oficial:
cd ../tools/installer-windows
cargo build --target x86_64-pc-windows-gnu --release
```

### 3. Aplicación Móvil Android
```bash
cd android
./gradlew assembleDebug

# Instalar en el teléfono conectado por USB:
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.carlos.zentrack/.MainActivity
```

---

## 🔬 Diagnóstico y Pruebas de Funcionamiento

* **Prueba de Mando Xbox en Windows:**
  - Abre el comando de Windows: `joy.cpl` (debe figurar *"Controlador para Microsoft Xbox 360"*).
  - O visita en tu navegador [hardwaretester.com/gamepad](https://hardwaretester.com/gamepad).
* **Prueba de Teclado y Auto-repetición:**
  - Visita [hardwaretester.com/keyboard](https://hardwaretester.com/keyboard) o [key-test.com](https://key-test.com) para verificar que las teclas reportan sus valores físicos (`KeyA`, `Space`, `Enter`) y se auto-repiten al mantenerlas pulsadas.
* **Analizador Forense de Radio:**
  - `tools/zen_bt_diagnostic.py`: Analizador de latencia en microsegundos para radio Bluetooth/BLE.

---

## 📄 Licencia

Este proyecto está bajo licencias de código abierto compatibles (MIT / Apache 2.0 / SIL OFL para fuentes tipográficas).
