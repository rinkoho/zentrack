# 📋 ZenTrack: Propuesta de Implementación y Guía Maestra de Pase de Mando (Handover Técnico)

> **Fecha de Emisión:** 18 de Septiembre de 2026  
> **Rama Activa:** `commercial-v1`  
> **Autor Saliente:** Agente de IA Antigravity (Advanced Agentic Coding)  
> **Destinatario:** Siguiente Agente de IA / Ingeniero de Software a cargo de ZenTrack  
> **Estado del Repositorio:** Código 100% libre de credenciales hardcodeadas, servidor Rust nativo operativo, app Android con onboarding dinámico.  
> **Objetivo del Documento:** Brindar todo el contexto arquitectónico, ubicación de archivos, análisis forense de 4 fallas detectadas por el usuario, protocolo de razonamiento para no cometer errores y especificación exacta de implementación.

---

## 🗺️ 1. Entorno de Ejecución y Mapa de Archivos

### 1.1 Entorno de Hardware y Sistema
* **Host PC (Servidor):**
  - **SO:** Arch Linux x86_64 con `bspwm` y `polybar`.
  - **Firewall Activo:** `ufw` (Bloquea tráfico entrante por defecto si no se autoriza explícitamente el puerto `3000/tcp` o UDP `37020`).
  - **Ruta del Proyecto:** `/home/carlos/Projects/zentrack`
  - **Binario Servidor Rust:** `/home/carlos/Projects/zentrack/server-rust/target/release/zentrack-server`
  - **Wrapper CLI del Sistema:** `/home/carlos/.config/bspwm/bin/zentrack`
* **Dispositivo Móvil (Cliente):**
  - **Modelo:** Xiaomi Redmi Note 14 Pro 5G (`AAJBFIPJWSLJTKCI`).
  - **SO:** Xiaomi HyperOS / Android 14 (API 34/35).
  - **Resolución:** 1220x2712 px (Orientación Landscape fija: 2712x1220 px).
  - **Herramientas de Depuración:** `adb` por cable USB con puerto inverso activo: `adb reverse tcp:3000 tcp:3000`.
  - **Particularidad Crítica de Xiaomi HyperOS:** Los logs de nivel `Log.d(...)` son descartados/silenciados por defecto por el kernel de MIUI para ahorrar batería. **SIEMPRE usar `Log.i(...)` o `Log.w(...)` para diagnósticos en Android.**

---

### 1.2 Mapa de Documentación Existente
Antes de tocar cualquier línea de código, debes conocer la documentación existente en `/home/carlos/Projects/zentrack/docs/`:

1. **[`docs/HANDOVER_TECHNICAL_SPEC.md`](file:///home/carlos/Projects/zentrack/docs/HANDOVER_TECHNICAL_SPEC.md):**
   - *Contenido:* Especificación técnica profunda de la pila Bluetooth (Classic HID vs BLE HOGP `0x1812`). Explica por qué el transporte Smart TV BLE funciona a 100Hz sin *sniff subrating* y cómo se resolvieron colisiones duales.
2. **[`docs/GUIA_ESTUDIO_ZENTRACK.md`](file:///home/carlos/Projects/zentrack/docs/GUIA_ESTUDIO_ZENTRACK.md):**
   - *Contenido:* Guía conceptual de arquitectura del proyecto, protocolos de paquetes binarios (Little Endian de 6 bytes para mouse) y JSON para teclado/gamepad.
3. **[`docs/PROPUESTA_ELIMINACION_STUTTER.md`](file:///home/carlos/Projects/zentrack/docs/PROPUESTA_ELIMINACION_STUTTER.md):**
   - *Contenido:* Análisis matemático de latencia, buffers circulares y reloj monotónico para lograr cero tirones en Android.

---

### 1.3 Módulos de Código y Cómo Funciona Cada Uno

#### 🦀 A. Servidor Rust (`server-rust/`)
* **[`server-rust/src/main.rs`](file:///home/carlos/Projects/zentrack/server-rust/src/main.rs):**
  Punto de entrada. Carga la configuración, inicializa el motor criptográfico AES-GCM, arranca el reactor `uinput` virtual (`VirtualDeviceManager`), lanza el hilo UDP de descubrimiento en segundo plano y monta el servidor Axum HTTP/WebSocket.
* **[`server-rust/src/web.rs`](file:///home/carlos/Projects/zentrack/server-rust/src/web.rs):**
  Rutas web del servidor Axum:
  - `GET /`: Upgrade a WebSocket si viene cabecera `Upgrade: websocket`, o sirve la web legacy. Requiere `?token=...`.
  - `GET /status`: Devuelve JSON con estado, puerto, token actual y clientes conectados (`{"status":"active","port":3000,"token":"...","connectedCount":1}`).
  - `GET /api/devices`: Devuelve la lista de dispositivos descubiertos por radar LAN.
  - `GET /qr.svg`: Renderiza el código QR en formato SVG vectorial. *(Aquí reside el Problema 1)*.
  - `GET /pair`: Sirve la interfaz web cyberpunk de emparejamiento de escritorio (`public/pair.html`).
* **[`server-rust/src/discovery.rs`](file:///home/carlos/Projects/zentrack/server-rust/src/discovery.rs):**
  Motor de auto-descubrimiento UDP en puerto `37020`. Responde al mensaje `{"cmd":"DISCOVER"}` de la app móvil enviando `ANNOUNCE`, emite una baliza `BEACON` cada 3 segundos a `255.255.255.255:37020` y registra los clientes que emiten `CLIENT_ANNOUNCE`.
* **[`server-rust/src/crypto.rs`](file:///home/carlos/Projects/zentrack/server-rust/src/crypto.rs):**
  Cifrado AES-256-GCM. Deriva una clave de 32 bytes con SHA-256 a partir del token de seguridad para descifrar keystrokes confidenciales.
* **[`public/pair.html`](file:///home/carlos/Projects/zentrack/public/pair.html):**
  Dashboard cyberpunk de emparejamiento de escritorio con radar en tiempo real y QR. *(Aquí reside parte del Problema 1)*.

#### 🤖 B. Cliente Android (`android/app/src/main/java/com/carlos/zentrack/`)
* **[`MainActivity.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/MainActivity.kt):**
  Actividad principal en horizontal (Landscape). Inicializa singletons (`ZenPreferences`, `ZenCrypto`, `ZenSoundEngine`, `ZenHapticsEngine`, `ZenBluetoothHidManager`, `ZenBleHidServer`). Maneja la llamada a `socketManager.connect()` si `ZenPreferences.isConfigured` es verdadero.
* **[`preferences/ZenPreferences.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/preferences/ZenPreferences.kt):**
  Almacén `SharedPreferences`. Propiedades clave:
  - `serverIp`: String (vacío por defecto tras `pm clear`).
  - `serverPort`: Int (3000).
  - `serverToken`: String (vacío por defecto tras `pm clear`).
  - `usbAdbModeEnabled`: Boolean (`false` por defecto).
  - `isConfigured`: `serverToken.isNotBlank() && (serverIp.isNotBlank() || usbAdbModeEnabled)`.
* **[`network/WebSocketManager.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/network/WebSocketManager.kt):**
  Cliente WebSocket asíncrono (Java-WebSocket). Si `usbAdbModeEnabled` es true, fuerza la conexión hacia `127.0.0.1`. Informa cambios de estado con `onStateChanged(connected, statusString)`.
* **[`network/ZenDiscoveryManager.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/network/ZenDiscoveryManager.kt):**
  Sondeo y escucha UDP en puerto `37020`. Adquiere `MulticastLock` del `WifiManager` para que el chip Wi-Fi no descarte paquetes UDP en reposo.
* **[`ui/components/QrScannerDialog.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/ui/components/QrScannerDialog.kt):**
  Visor de cámara usando CameraX (`Preview` + `ImageAnalysis`) y decodificador ZXing `MultiFormatReader`.
* **[`ui/components/ServerConnectionDialog.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/ui/components/ServerConnectionDialog.kt):**
  Diálogo modal que ofrece:
  1. Botón "Escanear QR" (abre `QrScannerDialog`).
  2. Botón "Cable USB" (consulta `http://127.0.0.1:3000/status` para obtener el token automáticamente).
  3. Lista de Servidores del Radar LAN (`discoveredServers`).
  4. Formulario desplegable manual (IP, puerto, token).
* **[`ui/screens/MainContainerScreen.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/ui/screens/MainContainerScreen.kt):**
  Contenedor principal con Compose. Maneja la barra superior, el Drawer lateral (`CollapsibleSidebar`) y los modos de la app (Trackpad, Teclado, Mando, Visión).
* **[`ui/screens/TrackpadScreen.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/ui/screens/TrackpadScreen.kt):**
  Superficie táctil de ultra-alta frecuencia (500Hz). En su esquina superior derecha dibuja la píldora de estado de conexión (`Surface` con LED, ícono, texto y badge).

---

## 🚨 2. Análisis Forense de los 4 Problemas Reportados por el Usuario

---

### ❌ PROBLEMA 1: El QR de `zentrack gui` no es detectado por la cámara

#### Ubicación del Código:
- [`server-rust/src/web.rs`](file:///home/carlos/Projects/zentrack/server-rust/src/web.rs#L63-L83)
- [`public/pair.html`](file:///home/carlos/Projects/zentrack/public/pair.html#L105-L121)
- [`android/app/src/main/java/com/carlos/zentrack/ui/components/QrScannerDialog.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/ui/components/QrScannerDialog.kt#L206-L245)

#### Causa Raíz Técnica:
1. **Violación del Estándar ISO/IEC 18004 (Quiet Zone & Polaridad Invertida):**
   En `web.rs`:
   ```rust
   let svg_content = match QrCode::new(url.as_bytes()) {
       Ok(code) => code
           .render::<svg::Color>()
           .min_dimensions(300, 300)
           .dark_color(svg::Color("#ff6b35"))   // ¡Módulos naranja!
           .light_color(svg::Color("#000000"))  // ¡Fondo negro!
           .build(),
   ```
   En `pair.html`:
   ```css
   .qr-box { background: #000; ... }
   ```
   - En el estándar QR, los tres patrones de esquina (*finder patterns*, proporción 1:1:3:1:1) **exigen una "zona de silencio" clara/blanca** alrededor de todo el perímetro de al menos 4 módulos de grosor.
   - Al tener `light_color = #000000` y `.qr-box { background: #000 }`, el borde negro del QR se fusiona con el fondo de la página. El algoritmo de escaneo de ZXing busca transiciones claro-oscuro para delimitar las esquinas. Al no haber transición exterior (negro sobre negro), **las esquinas son matemáticamente invisibles para el detector**.
   - Adicionalmente, el binarizador de luminancia de ZXing (`HybridBinarizer`) transforma el fotograma YUV a escala de grises. El naranja `#ff6b35` tiene luminancia media (~144/255) y el negro es 0. En una pantalla con reflejos o ángulos, el contraste efectivo cae por debajo del umbral de binarización.
2. **Configuración del Lector ZXing en Android:**
   En `QrScannerDialog.kt`, el lector `MultiFormatReader` se inicializó únicamente con:
   ```kotlin
   setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
   ```
   **Falta:** `DecodeHintType.TRY_HARDER` (que permite muestreo más agresivo de píxeles) y fallback a `GlobalHistogramBinarizer` cuando `HybridBinarizer` falla ante fuentes con iluminación no uniforme o moiré de monitores.

---

### ❌ PROBLEMA 2: El QR de `zentrack qr` se detecta pero "no pasa nada"

#### Ubicación del Código:
- [`android/app/src/main/java/com/carlos/zentrack/ui/components/ServerConnectionDialog.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/ui/components/ServerConnectionDialog.kt#L64-L72)
- [`android/app/src/main/java/com/carlos/zentrack/network/WebSocketManager.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/network/WebSocketManager.kt#L23-L30)
- [`android/app/src/main/java/com/carlos/zentrack/ui/components/QrScannerDialog.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/ui/components/QrScannerDialog.kt#L135-L144)

#### Causa Raíz Técnica:
1. **Fuga de Estado del Modo USB (`usbAdbModeEnabled` atascado):**
   - Cuando el usuario prueba previamente el botón "Cable USB", la app guarda `ZenPreferences.usbAdbModeEnabled = true` en almacenamiento persistente.
   - Cuando después escanea el QR en la terminal, `QrScannerDialog` llama a `onQrDecoded`:
     ```kotlin
     onQrDecoded = { ip, port, token ->
         ZenPreferences.serverIp = ip
         ZenPreferences.serverPort = port
         ZenPreferences.serverToken = token
         onConnect(ip, port, token)
         showQrScanner = false
         onDismiss()
     }
     ```
     **¡Error crítico:** Nunca restablece `ZenPreferences.usbAdbModeEnabled = false`!
   - Inmediatamente, `onConnect` invoca `socketManager.connect(ip, port, token)`. Y dentro de `WebSocketManager.kt`:
     ```kotlin
     val isUsbAdb = com.carlos.zentrack.preferences.ZenPreferences.usbAdbModeEnabled
     val targetIp = if (isUsbAdb) "127.0.0.1" else ip
     ```
     Como `isUsbAdb` seguía siendo `true`, **`WebSocketManager` descartó la IP del QR (`192.168.18.226`) e intentó conectarse a `127.0.0.1`!** Si el cable no estaba conectado con el reverse activo, la conexión abortaba de inmediato.
2. **Mutismo Absoluto de Feedback en la Interfaz (Silent Failure):**
   - En `QrScannerDialog.kt`, al leer el QR no se emite ninguna vibración háptica (`onVibrate`) ni sonido.
   - El diálogo de escaneo simplemente desaparece con `onDismiss()`.
   - Si la conexión WebSocket tarda o falla (por ejemplo, si el firewall `ufw` en Arch Linux bloquea el puerto `3000/tcp` sobre la interfaz Wi-Fi), la app queda en `Red Offline` sin ningún mensaje explicativo. Para el usuario, parece que la app "detectó el QR pero no hizo nada".

---

### ❌ PROBLEMA 3: Al conectar por cable, la app muestra LED verde pero dice "Wi-Fi" y "RED"

#### Ubicación del Código:
- [`android/app/src/main/java/com/carlos/zentrack/ui/screens/TrackpadScreen.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/ui/screens/TrackpadScreen.kt#L578-L617)
- [`android/app/src/main/java/com/carlos/zentrack/ui/screens/MainContainerScreen.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/ui/screens/MainContainerScreen.kt#L405-L425)
- [`android/app/src/main/java/com/carlos/zentrack/network/WebSocketManager.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/network/WebSocketManager.kt#L40-L42)

#### Causa Raíz Técnica:
Toda la capa de presentación visual tiene strings e íconos fijos (*hardcoded*) que suponen que toda conexión por WebSocket es Wi-Fi:

1. **En la Píldora Superior Derecha de `TrackpadScreen.kt`:**
   - **Ícono:**
     ```kotlin
     Icon(
         imageVector = if (isBtMode) Icons.Default.Bluetooth else Icons.Default.Wifi,
         // ...
     )
     ```
     *Falla:* Si no es Bluetooth, muestra SIEMPRE `Icons.Default.Wifi`, sin comprobar si `usbAdbModeEnabled` está activo.
   - **Texto Principal:**
     ```kotlin
     Text(
         text = if (isBtMode) { ... } else {
             if (isConnected) "RED 500Hz" else "Red Offline"
         }
     )
     ```
     *Falla:* Ignora el parámetro `statusText` suministrado por el `WebSocketManager` (que sí sabe si es USB). Pinta de forma estática `"RED 500Hz"`.
   - **Badge de Modo:**
     ```kotlin
     Text(text = if (isBtMode) "BT" else "RED")
     ```
     *Falla:* Pinta de forma estática `"RED"` en lugar de `"USB"` cuando está en modo cable.

2. **En el Menú Lateral de `MainContainerScreen.kt`:**
   - **Texto Secundario:**
     ```kotlin
     Text(
         text = if (isBtActive) { ... } else {
             if (isConnected) "Wi-Fi UDP / USB ADB (500Hz)" else "Toca para reintentar"
         }
     )
     ```
     *Falla:* Muestra una frase genérica que menciona `"Wi-Fi UDP"` a pesar de estar conectado por cable USB ADB.

---

### ❌ PROBLEMA 4: La app no solicita los permisos necesarios en tiempo de ejecución

#### Ubicación del Código:
- [`android/app/src/main/java/com/carlos/zentrack/MainActivity.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/MainActivity.kt#L46-L58)
- [`android/app/src/main/java/com/carlos/zentrack/ui/components/QrScannerDialog.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/ui/components/QrScannerDialog.kt#L45-L52, #L170-L190)

#### Causa Raíz Técnica:
1. **Permiso de Cámara (`android.permission.CAMERA`):**
   - En `MainActivity.kt`, el bloque de solicitud de permisos al inicio sólo revisa permisos de Bluetooth (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`). **La cámara no se incluye.**
   - En `QrScannerDialog.kt`, cuando `hasCameraPermission` es falso, muestra un texto estático: *"Permiso de cámara requerido"*, pero **no tiene un launcher de permisos de Android Jetpack Compose (`rememberLauncherForActivityResult(RequestPermission())`)** ni un botón que dispare el diálogo del sistema operativo.
   - *Efecto:* El usuario tiene que salirse de la aplicación, ir a Ajustes de Android -> Aplicaciones -> ZenTrack -> Permisos y activar la cámara a mano.
2. **Permisos de Bluetooth y Dispositivos Cercanos:**
   - En Android 12+ (API 31+), se requieren `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`.
   - En Android 11 o inferior, o para escaneos de red Wi-Fi en ciertas versiones, se requiere `ACCESS_FINE_LOCATION`.
   - Si el usuario descarta el diálogo del sistema al inicio, la app nunca vuelve a pedirlo proactivamente cuando intenta escanear o conectar.

---

## 🧠 3. Protocolo de Razonamiento para la Próxima IA (Evitar Errores)

Para que la implementación sea impecable y no rompas nada existente, debes seguir esta disciplina de razonamiento:

1. **Principio de Exclusión Mutua de Transportes (USB vs Wi-Fi vs Bluetooth):**
   - ZenTrack maneja tres transportes: **Cable USB ADB (`127.0.0.1`)**, **Red Wi-Fi LAN (IP local)** y **Bluetooth (Classic / BLE)**.
   - Cada vez que el usuario elige un transporte (escanear QR, tocar un servidor en el radar LAN o pulsar "Cable USB"), debes **actualizar de forma inequívoca todas las banderas asociadas**:
     - Al escanear QR o elegir servidor Wi-Fi: `ZenPreferences.usbAdbModeEnabled = false`.
     - Al pulsar Cable USB: `ZenPreferences.usbAdbModeEnabled = true`.
   - Nunca asumas que `ZenPreferences` se resetea solo; es persistente.
2. **La Trampa de los Logs en Xiaomi MIUI / HyperOS:**
   - Si colocas `Log.d("Tag", "Mensaje")`, el teléfono Xiaomi descartará el log en release y muchas veces en debug.
   - **Regla:** Usa siempre `Log.i("ZenTrack", ...)` o `Log.w("ZenTrack", ...)`.
3. **La Trampa de la Cámara y ZXing:**
   - El sensor de cámara en smartphones no coincide con la orientación de pantalla (suele estar rotado a 90° o 270°).
   - CameraX maneja la vista previa, pero el búfer YUV entregado a `ImageAnalysis` requiere que ZXing utilice `DecodeHintType.TRY_HARDER` para que busque finder patterns tanto en orientación nativa como rotada.
   - El QR debe tener **fondo blanco y módulos negros** para que el cálculo de umbral de luminancia funcione en cualquier condición de luz.
4. **Validación Cruzada del Firewall (UFW):**
   - Si la app conecta por USB (`127.0.0.1`) pero no por Wi-Fi (`192.168.18.226`), no es necesariamente un bug de Android. Arch Linux tiene `ufw` activo. Comprueba `sudo ufw status`. El puerto `3000/tcp` debe estar permitido. La app debe mostrar un mensaje amigable indicando esto si la conexión Wi-Fi da timeout.
5. **No dar nada por cerrado sin el Ciclo de Prueba Completo:**
   - No basta con compilar (`BUILD SUCCESSFUL`).
   - Debes ejecutar:
     ```bash
     adb shell pm clear com.carlos.zentrack
     adb shell am start -n com.carlos.zentrack/.MainActivity
     ```
   - Tomar captura con `adb exec-out screencap -p > /tmp/screen.png` y verificar visualmente con `view_file` que la UI responda exactamente como se solicitó.

---

## 🛠️ 4. Especificación Técnica de Implementación Paso a Paso

---

### Paso 1: Reparar QR en el Servidor Rust y Dashboard Web

#### A. En [`server-rust/src/web.rs`](file:///home/carlos/Projects/zentrack/server-rust/src/web.rs):
Modificar `qr_svg_handler`:
```rust
async fn qr_svg_handler(State(state): State<AppState>) -> impl IntoResponse {
    let local_ip = local_ip_address::local_ip()
        .map(|ip| ip.to_string())
        .unwrap_or_else(|_| "127.0.0.1".to_string());
    let url = format!(
        "http://{}:{}/?token={}",
        local_ip, state.config.port, state.config.token
    );

    // QR de alto contraste: Fondo blanco puro (#ffffff) y módulos negros (#000000)
    // Esto garantiza cumplimiento con ISO/IEC 18004 y detección instantánea en cualquier cámara.
    let svg_content = match QrCode::new(url.as_bytes()) {
        Ok(code) => code
            .render::<svg::Color>()
            .min_dimensions(320, 320)
            .dark_color(svg::Color("#000000"))
            .light_color(svg::Color("#ffffff"))
            .build(),
        Err(_) => String::new(),
    };

    ([(header::CONTENT_TYPE, "image/svg+xml; charset=utf-8")], svg_content)
}
```

#### B. En [`public/pair.html`](file:///home/carlos/Projects/zentrack/public/pair.html):
Actualizar el estilo de `.qr-box` para que sirva de tarjeta blanca de alto contraste con su quiet zone:
```css
.qr-box {
  background: #ffffff;           /* Fondo blanco puro para contraste óptico */
  border: 3px solid var(--accent);
  border-radius: 14px;
  padding: 16px;                 /* Quiet zone reglamentaria */
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 0 30px var(--accent-glow);
  margin-bottom: 14px;
}
.qr-box img {
  width: 240px;
  height: 240px;
  display: block;
  border-radius: 4px;
}
```

---

### Paso 2: Robustecer el Lector ZXing y Solicitud Reactiva de Permisos de Cámara

#### A. En [`android/app/src/main/java/com/carlos/zentrack/ui/components/QrScannerDialog.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/ui/components/QrScannerDialog.kt):

1. **Launcher reactivo de permisos de cámara en Compose:**
   ```kotlin
   val context = LocalContext.current
   var hasCameraPermission by remember {
       mutableStateOf(
           ContextCompat.checkSelfPermission(
               context,
               android.Manifest.permission.CAMERA
           ) == android.content.pm.PackageManager.PERMISSION_GRANTED
       )
   }

   val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
       contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
   ) { isGranted ->
       hasCameraPermission = isGranted
   }

   // Solicitar automáticamente al abrir el diálogo si no lo tiene
   LaunchedEffect(Unit) {
       if (!hasCameraPermission) {
           permissionLauncher.launch(android.Manifest.permission.CAMERA)
       }
   }
   ```
   Y si el usuario lo rechazó, en la vista alternativa mostrar un botón interactivo:
   ```kotlin
   Button(
       onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
       colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryAccent)
   ) {
       Text("Conceder Permiso de Cámara", color = Color.Black, fontWeight = FontWeight.Bold)
   }
   ```

2. **Lector ZXing con `TRY_HARDER` y fallback de binarizador:**
   ```kotlin
   class QrCodeAnalyzer(
       private val onQrCodeScanned: (String) -> Unit
   ) : ImageAnalysis.Analyzer {
       private val reader = MultiFormatReader().apply {
           val hints = mapOf(
               DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
               DecodeHintType.TRY_HARDER to java.lang.Boolean.TRUE,
               DecodeHintType.CHARACTER_SET to "UTF-8"
           )
           setHints(hints)
       }
       private var isScanned = false

       override fun analyze(imageProxy: ImageProxy) {
           if (isScanned) {
               imageProxy.close()
               return
           }

           val buffer = imageProxy.planes[0].buffer
           val data = ByteArray(buffer.remaining())
           buffer.get(data)
           val width = imageProxy.width
           val height = imageProxy.height

           val source = PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
           
           // Intento 1: HybridBinarizer
           var bitmap = BinaryBitmap(HybridBinarizer(source))
           var result = try { reader.decodeWithState(bitmap) } catch (_: Exception) { null }

           // Intento 2: GlobalHistogramBinarizer (ideal para pantallas con reflejos)
           if (result == null) {
               try {
                   reader.reset()
                   bitmap = BinaryBitmap(com.google.zxing.common.GlobalHistogramBinarizer(source))
                   result = reader.decodeWithState(bitmap)
               } catch (_: Exception) { null }
           }

           if (result != null && !isScanned) {
               isScanned = true
               onQrCodeScanned(result.text)
           }

           reader.reset()
           imageProxy.close()
       }
   }
   ```

---

### Paso 3: Corregir Fuga de Estado, Resetear Modo USB y Feedback al Escanear QR

#### En [`android/app/src/main/java/com/carlos/zentrack/ui/components/ServerConnectionDialog.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/ui/components/ServerConnectionDialog.kt):

En el callback `onQrDecoded`:
```kotlin
if (showQrScanner) {
    QrScannerDialog(
        show = true,
        currentTheme = currentTheme,
        onQrDecoded = { ip, port, token ->
            // Feedback háptico inmediato
            com.carlos.zentrack.haptics.ZenHapticsEngine.vibrateRaw(50L, isKeyboard = false)

            // CRÍTICO: Desactivar modo USB porque la conexión es por Wi-Fi (IP del QR)
            ZenPreferences.usbAdbModeEnabled = false
            ZenPreferences.serverIp = ip
            ZenPreferences.serverPort = port
            ZenPreferences.serverToken = token

            android.util.Log.i("ZenTrack", "QR Decodificado con éxito. Conectando a $ip:$port...")
            onConnect(ip, port, token)
            showQrScanner = false
            onDismiss()
        },
        onDismiss = { showQrScanner = false }
    )
    return
}
```
Y en los servidores descubiertos por el radar LAN (línea ~203), mantener:
```kotlin
ZenPreferences.usbAdbModeEnabled = false
```

---

### Paso 4: Unificar y Corregir Indicadores Visuales de Conexión (USB vs Wi-Fi vs BT)

#### A. En [`android/app/src/main/java/com/carlos/zentrack/ui/screens/TrackpadScreen.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/ui/screens/TrackpadScreen.kt#L540-L620):

Localizar la píldora superior derecha:
```kotlin
val activeMode = com.carlos.zentrack.bluetooth.ZenInputRouter.activeMode
val isBtMode = activeMode == com.carlos.zentrack.bluetooth.ConnectionMode.BLUETOOTH
val isBtConnected = com.carlos.zentrack.bluetooth.ZenInputRouter.isBluetoothConnected
val activeConnected = if (isBtMode) isBtConnected else isConnected
val isUsbMode = com.carlos.zentrack.preferences.ZenPreferences.usbAdbModeEnabled
```

1. **Ícono dinámico:**
   ```kotlin
   Icon(
       imageVector = when {
           isBtMode -> Icons.Default.Bluetooth
           isUsbMode && isConnected -> Icons.Default.Usb
           else -> Icons.Default.Wifi
       },
       contentDescription = null,
       tint = if (activeConnected) currentTheme.primaryAccent else currentTheme.textMuted,
       modifier = Modifier.size(13.dp)
   )
   ```

2. **Texto descriptivo dinámico:**
   ```kotlin
   Text(
       text = when {
           isBtMode -> {
               if (isBtConnected) {
                   (com.carlos.zentrack.bluetooth.ZenInputRouter.connectedBluetoothDeviceName ?: "BT HID")
               } else {
                   "BT Offline"
               }
           }
           isUsbMode -> {
               if (isConnected) "USB ADB (500Hz)" else "USB Desconectado"
           }
           else -> {
               if (isConnected) "Wi-Fi (500Hz)" else "Red Offline"
           }
       },
       color = currentTheme.textPrimary,
       fontSize = 10.sp,
       fontWeight = FontWeight.SemiBold,
       maxLines = 1
   )
   ```

3. **Badge de Modo:**
   ```kotlin
   Text(
       text = when {
           isBtMode -> "BT"
           isUsbMode -> "USB"
           else -> "WIFI"
       },
       color = currentTheme.primaryAccent,
       fontSize = 8.sp,
       fontWeight = FontWeight.Black,
       modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
   )
   ```

#### B. En [`android/app/src/main/java/com/carlos/zentrack/ui/screens/MainContainerScreen.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/ui/screens/MainContainerScreen.kt#L405-L425):

En el panel lateral de estado de conexión:
```kotlin
val isUsbMode = com.carlos.zentrack.preferences.ZenPreferences.usbAdbModeEnabled

Text(
    text = if (isBtActive) {
        if (isBtConn) (com.carlos.zentrack.bluetooth.ZenInputRouter.connectedBluetoothDeviceName ?: "Dispositivo Bluetooth") else "Bluetooth Desconectado"
    } else if (isUsbMode) {
        if (isConnected) "Conexión Cable USB" else "Cable USB Desconectado"
    } else {
        if (isConnected) "Conexión Wi-Fi" else "Wi-Fi Desconectado"
    },
    color = animatedTheme.textPrimary,
    fontSize = 11.sp,
    fontWeight = FontWeight.SemiBold,
    maxLines = 1
)
Text(
    text = if (isBtActive) {
        if (isBtConn) com.carlos.zentrack.bluetooth.ZenInputRouter.activeBluetoothSubModeName else "Toca para emparejar o conectar"
    } else if (isUsbMode) {
        if (isConnected) "Túnel ADB Activo 127.0.0.1 (500Hz)" else "Revisa conexión USB o comando adb reverse"
    } else {
        if (isConnected) "IP: ${com.carlos.zentrack.preferences.ZenPreferences.serverIp} (500Hz)" else "Toca para reintentar o configurar"
    },
    color = if (isCurrentConnected) animatedTheme.primaryAccent else animatedTheme.textMuted,
    fontSize = 8.5.sp
)
```

---

### Paso 5: Solicitud de Permisos Global al Iniciar la App

En [`android/app/src/main/java/com/carlos/zentrack/MainActivity.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/MainActivity.kt#L46-L58):
Ampliar el array de permisos solicitados en `onCreate()` para incluir **Cámara** y **Bluetooth**:
```kotlin
val requiredPermissions = mutableListOf<String>()

// Cámara para escanear QR sin interrupciones manuales
requiredPermissions.add(android.Manifest.permission.CAMERA)

// Permisos Bluetooth para Android 12+ (API 31+)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    requiredPermissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
    requiredPermissions.add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
    requiredPermissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
} else {
    // Para versiones anteriores puede requerirse ubicación para escaneos
    requiredPermissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
}

val missing = requiredPermissions.filter {
    checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
}

if (missing.isNotEmpty()) {
    requestPermissions(missing.toTypedArray(), 1001)
}
```

---

## 🧪 5. Comandos de Compilación, Despliegue y Verificación en Vivo

La próxima IA debe ejecutar esta secuencia de validación paso a paso:

```bash
# 1. Compilar el servidor Rust si hubo cambios en web.rs
cd /home/carlos/Projects/zentrack/server-rust
cargo build --release

# 2. Compilar la aplicación Android
cd /home/carlos/Projects/zentrack/android
./gradlew assembleDebug

# 3. Instalar en el dispositivo conectado
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Probar instalación limpia desde cero (True Fresh-Install Test)
adb shell pm clear com.carlos.zentrack
adb shell am start -n com.carlos.zentrack/.MainActivity

# 5. Capturar pantalla para verificar que solicita permisos y abre el diálogo inicial
sleep 2
adb exec-out screencap -p > /tmp/test_fresh.png

# 6. Probar conexión USB:
# Con el túnel activo: adb reverse tcp:3000 tcp:3000
# Tocar botón Cable USB y verificar que la píldora muestra:
# [Icono USB] USB ADB (500Hz) [USB] con LED verde.
```

---

## 📌 Resumen de Compromisos
1. No usar comandos `cd` sueltos en las herramientas de ejecución; usar siempre la propiedad `Cwd`.
2. Mantener la rama `commercial-v1` y commitear cambios con mensajes descriptivos bajo estándar semántico.
3. No re-introducir credenciales hardcodeadas bajo ninguna circunstancia.
4. Con esta guía, la siguiente IA tiene las causas forenses exactas, las líneas de código afectadas y el plan de solución sin margen de error.
