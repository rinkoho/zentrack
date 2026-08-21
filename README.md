# 📱 ZenTrack Native (Remote Trackpad & Mechanical Keyboard)

**ZenTrack** es un controlador táctil de ultra-baja latencia y teclado mecánico 65% nativo para **Android (100% Kotlin + Jetpack Compose)** diseñado para controlar tu PC con Linux (BSPWM, Hyprland, X11, Wayland) directamente desde tu celular.

---

## ✨ Características Principales

* ⚡ **Protocolo Binario de 6 Bytes (Sub-Milisegundo):** Transmisión directa de deltas de movimiento y scroll sub-píxel sin la sobrecarga de parseo JSON.
* 🔌 **Conexión por Cable USB ADB (`127.0.0.1:3000`):** Conexión física a latencia ultra-baja de **<0.2ms** sin interferencias o jitter de red Wi-Fi.
* 🚀 **Ajuste KernelSU Root (`zentrack boost` / `zentrack unboost`):** Desbloqueo y fijación de la frecuencia del digitalizador a **`498 Hz – 501 Hz` estables** continuos sin caídas de energía.
* 🔤 **Keycaster OLED Dashboard & Motor osu!lazer:** Visualizador de teclas estilo teclado mecánico custom con animaciones spring de rebote y estabilidad de IDs inmutables `TypedChar`.
* ⌨️ **Mapeo Internacional `AltGr` (Level-3 ISO):** Soporte nativo para `ñ`, `Ñ`, vocales acentuadas (`á`, `é`, `í`, `ó`, `ú`) y símbolos `Shift`.
* 🎯 **Hitboxes sin Zonas Muertas (100% Target Area):** Rediseño del acolchado táctil en `KeyCap.kt` para tipeo ultra-rápido sin fallos.
* 🖐️ **Gesto de 3 Dedos Bidireccional & Inversión de Dirección:** Cambios de escritorio suaves en BSPWM/Hyprland con opción de inversión en Ajustes.
* 🔐 **Cifrado E2EE en Hardware (AES-256-GCM):** Pulsaciones de teclado y contraseñas cifradas en hardware mediante `javax.crypto.Cipher` y `crypto.createDecipheriv`.
* 🐧 **Emulación de Teclado Físico USB en Kernel (`/dev/uinput`):** Scancodes nativos de kernel Linux.
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

## 🚀 Compilación e Instalación (Release)

```bash
cd ~/Projects/remote_trackpad/android
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 📄 Documentación Completa

Toda la documentación técnica se encuentra en tu bóveda de Obsidian:
* **`Architecture.md`**: Diagramas de flujo, motor sub-píxel, E2EE y arquitectura modular Kotlin.
* **`Decisions.md`**: Registro cronológico completo de decisiones de ingeniería.
* **`Remote Trackpad Home.md`**: Ficha técnica y estado del proyecto.
