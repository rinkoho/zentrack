# 📘 Guía de Estudio Integral de ZenTrack (De Principiante a Experto)

¡Bienvenido a la arquitectura interna de **ZenTrack**! Esta guía está redactada con explicaciones claras, analogías sencillas y desgloses paso a paso para que cualquier persona que esté comenzando en el mundo de la programación pueda comprender cómo funciona un proyecto real de nivel avanzado.

---

## 🧭 1. Panorama General: ¿Qué es ZenTrack y cómo funciona?

Imagina que ZenTrack es un sistema de dos piezas que conversan entre sí a la velocidad de la luz:
1. **El Teléfono Móvil (Cliente Android en Kotlin):** Es tu control remoto inteligente. Captura cuando tocas la pantalla, cuando pulsas una tecla, mueves un joystick o mueves tu mano frente a la cámara.
2. **La Computadora con Linux (Servidor):** Recibe esas órdenes y le ordena al sistema operativo (o directamente al Kernel de Linux) que mueva el puntero del ratón, escriba letras en un procesador de texto o dispare en un videojuego.

```
       📱 CELULAR (ZenTrack APK)
     ┌────────────────────────────┐
     │ • Trackpad táctil          │
     │ • Teclado mecánico 65%     │
     │ • Mando Xbox Gaming        │
     │ • Cámara Espacial (Visión) │
     └─────────────┬──────────────┘
                   │
         [¿Cómo se comunican?]
                   │
    ┌──────────────┴──────────────┐
    │                             │
    ▼ (Modo Red)                  ▼ (Modo Bluetooth Directo)
Cable USB ADB / Wi-Fi         BLE / Bluetooth Clásico HID
(Puerto 3000, 500 Hz)         (Sin servidor, conecta a Smart TVs,
    │                         Macs, PCs o iPads como ratón real)
    ▼
🦀 SERVIDOR RUST NATIVO (server-rust)
    ├──> 🐧 Linux: /dev/uinput (Kernel Input Subsystem)
    │        ├── Teclado & Ratón de alta resolución
    │        └── Mando Xbox 360 Virtual
    └──> 🪟 Windows 10/11: Win32 API + ViGEmBus
             ├── SendInput con Scan Codes & Auto-repetición
             └── Mando Xbox 360 Virtual (XInput nativo)
```

---

## 🧱 2. Conceptos Clave Explicados con Analogías

Antes de leer el código, repasemos los conceptos que encontrarás a menudo:

* **Jetpack Compose:** Es la herramienta moderna de Android para crear interfaces gráficas. En lugar de diseñar en archivos XML rígidos, describes tu pantalla usando funciones de Kotlin llamadas `@Composable`. Es como armar un castillo con piezas de Lego: defines cómo se ve un botón, una fila o una columna y el sistema lo dibuja reactivamente.
* **WebSocket:** A diferencia de una página web común donde pides algo y esperas a que cargue (HTTP), un WebSocket es como una **llamada telefónica abierta y constante**. Una vez conectado, ambos lados pueden enviarse datos al instante sin esperar.
* **Protocolo Binario:** Si envías un texto diciendo `{"x": 12, "y": -5}`, la computadora debe leer letra por letra, procesar comillas y convertir texto en números (lo cual tarda tiempo). Un paquete binario envía solo los números puros en bytes de memoria (ej. `[01 00 0C 00 FB FF]`). Tarda menos de 0.05 milisegundos.
* **Bluetooth HID (Human Interface Device):** Es el estándar universal que usan los ratones y teclados de fábrica. ZenTrack engaña a cualquier Smart TV, PC o tablet para que crea que tu teléfono es un dispositivo físico USB enchufado por Bluetooth, sin necesidad de instalar nada en la TV.
* **Cifrado AES-256-GCM:** Es una "caja fuerte digital". Cuando escribes tu contraseña en el teclado del teléfono, ZenTrack la cifra matemáticamente en el chip antes de enviarla por el aire o el cable, para que nadie en la red pueda espiarla.
* **/dev/uinput en Linux & SendInput/ViGEmBus en Windows:** Son las puertas de entrada al sistema operativo que permiten a ZenTrack crear periféricos virtuales (teclados con scan codes reales, ratones de alta resolución o mandos de Xbox 360 oficiales) que los juegos y aplicaciones ven exactamente como si fueran cables físicos conectados a la placa base.

---

## 📂 3. Desglose Módulo por Módulo del Código

---

### 🚀 Módulo 1: El Núcleo de Arranque
**Archivo:** `MainActivity.kt`

#### ¿Qué hace?
Es la puerta de entrada de la aplicación. Cuando tocas el ícono de ZenTrack en tu teléfono, Android ejecuta este archivo primero.

#### Partes clave del código:
1. **Fijar Pantalla en Horizontal (`SCREEN_ORIENTATION_LANDSCAPE`):**
   ```kotlin
   requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
   ```
   Fuerza la pantalla en horizontal para tener máxima comodidad ergonómica al usar ambas manos.

2. **Overclock de Pantalla (120 Hz / 144 Hz):**
   ```kotlin
   for (mode in display.supportedModes) {
       if (mode.refreshRate > maxRate) {
           maxRate = mode.refreshRate
           maxMode = mode
       }
   }
   params.preferredRefreshRate = maxRate
   ```
   *¿Por qué?* Para que el puntero se sienta como mantequilla pura sin retraso visual, la app busca la tasa de hercios más alta soportada por la pantalla del celular y la activa.

3. **Despacho No-Bufferizado (`requestUnbufferedDispatch`):**
   ```kotlin
   override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
       window.decorView.requestUnbufferedDispatch(ev)
       return super.dispatchTouchEvent(ev)
   }
   ```
   *Analogía:* Por defecto, Android guarda los toques en un "buzón" y los lee cada cierto tiempo para ahorrar batería. `requestUnbufferedDispatch` le dice al procesador: *"No esperes; en cuanto el dedo toque el cristal, dámelo en el microsegundo cero"*.

4. **Inicialización de Componentes:**
   En `onCreate()`, se despiertan los motores: preferencias, sonidos, cifrado con el token de seguridad, vibración háptica y servidores Bluetooth.

---

### 🌐 Módulo 2: Comunicación y Red
**Archivo:** `network/WebSocketManager.kt`

#### ¿Qué hace?
Gestiona el enlace de red entre tu teléfono y tu PC, empaquetando los movimientos del dedo y las pulsaciones de teclas.

#### Partes clave del código:
1. **Conexión Inteligente (Wi-Fi vs USB ADB):**
   ```kotlin
   val isUsbAdb = ZenPreferences.usbAdbModeEnabled
   val targetIp = if (isUsbAdb) "127.0.0.1" else ip
   val serverUri = URI("ws://$targetIp:$port/?token=$token")
   ```
   Si activas el modo USB en Ajustes, se conecta a `127.0.0.1` a través del túnel de cable ADB (`adb reverse tcp:3000 tcp:3000`), eliminando cualquier interferencia de router o micro-cortes de Wi-Fi.

2. **Envío Ultrarrápido en 6 Bytes (`sendBinary`):**
   ```kotlin
   val buffer = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
   buffer.putShort(cmd)      // Byte 0-1: Comando (1 = Mover ratón, 2 = Scroll)
   buffer.putShort(x.toShort()) // Byte 2-3: Desplazamiento horizontal delta X
   buffer.putShort(y.toShort()) // Byte 4-5: Desplazamiento vertical delta Y
   webSocketClient?.send(buffer.array())
   ```
   En lugar de enviar texto largo, reserva exactamente 6 casillas de memoria (`ByteBuffer(6)`). El servidor lo lee en un suspiro.

---

### 🔐 Módulo 3: Seguridad y Criptografía Hardware
**Archivo:** `security/ZenCrypto.kt`

#### ¿Qué hace?
Protege tu privacidad. Cuando escribes contraseñas o comandos sensibles en el teclado virtual, este módulo los vuelve ilegibles antes de salir del teléfono.

#### Partes clave del código:
1. **Derivación de Clave:**
   Usa `SHA-256` sobre tu token secreto para crear una llave simétrica de 256 bits (`SecretKeySpec`).
2. **Cifrado AES-GCM:**
   ```kotlin
   val iv = ByteArray(12)
   secureRandom.nextBytes(iv) // Genera 12 bytes aleatorios (Vector de Inicialización)
   val cipher = Cipher.getInstance("AES/GCM/NoPadding")
   cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
   ```
   *¿Por qué AES-GCM?* No solo cifra los datos para que nadie los lea, sino que además produce un **Auth Tag** (sello de autenticidad). Si un atacante intentara alterar un solo byte en tránsito, el servidor rechaza el paquete automáticamente.

---

### 💾 Módulo 4: Persistencia y Ajustes
**Archivo:** `preferences/ZenPreferences.kt`

#### ¿Qué hace?
Es la memoria a largo plazo de la app. Recuerda tu sensibilidad preferida, el tema que escogiste, el volumen de los switches y la posición de tus botones para que no se borren al cerrar la aplicación.

#### Partes clave del código:
* Utiliza `SharedPreferences` de Android.
* Emplea propiedades con `get()` y `set()` personalizados en Kotlin:
  ```kotlin
  var sensitivity: Float
      get() = prefs.getFloat(KEY_SENSITIVITY, 1.0f)
      set(value) = prefs.edit().putFloat(KEY_SENSITIVITY, value).apply()
  ```
  Esto significa que en cualquier parte de la app, si escribes `ZenPreferences.sensitivity = 1.5f`, Kotlin guarda el cambio en el disco del teléfono de forma transparente.

---

### 📶 Módulo 5: Bluetooth HID y Servidor BLE HOGP (Zero-Host)

Este grupo de módulos hace posible usar ZenTrack en Smart TVs, consolas o PCs sin tener nada instalado en ellas.

1. **`bluetooth/HidDescriptor.kt` (El Carnet de Identidad):**
   * Es un arreglo de bytes hexadecimales que le explica a la computadora qué tipo de aparato se acaba de conectar:
     * **Report ID 1 (8 bytes):** Teclado estándar (1 byte para teclas Ctrl/Shift/Alt, 1 byte reservado, y 6 bytes para hasta 6 teclas presionadas simultáneamente).
     * **Report ID 2 (5 bytes):** Ratón (1 byte para 5 botones: Izquierdo, Derecho, Central, Atrás, Adelante; 2 bytes para movimiento X/Y; 1 byte para rueda vertical y 1 byte para desplazamiento horizontal).
     * **Report ID 3 (2 bytes):** Control remoto multimedia (Subir/Bajar volumen, Mute, Play/Pause).

2. **`bluetooth/HidKeycodeMapper.kt` (El Diccionario de Teclas):**
   * Traduce letras comunes como `"a"` al scancode del estándar USB (`0x04.toByte()`), `"Return"` a `0x28`, o flecha derecha a `0x4F`.

3. **`bluetooth/ZenBluetoothHidManager.kt` (Bluetooth Clásico):**
   * Emplea `BluetoothHidDevice` (Android 9+).
   * **Búferes sin Recolector de Basura (*Zero-Allocation*):** En lugar de crear arreglos nuevos en cada movimiento, reutiliza `mouseReportBuffer`. Esto evita pausas de congelamiento por el recolector de basura (GC) de Android.
   * **Frecuencia de 100 Hz sincronizada:** Alinea la salida con los intervalos de radio del chipset para evitar pérdidas de paquetes.

4. **`bluetooth/ZenBleHidServer.kt` (Bluetooth Low Energy para Smart TVs):**
   * Crea un servidor GATT con el servicio `0x1812` (HOGP). Permite que televisores inteligentes con Android TV, Google TV, LG webOS o Samsung Tizen descubran tu teléfono en su menú de ajustes Bluetooth y lo vinculen al instante.

5. **`bluetooth/ZenInputRouter.kt` (El Policía de Tráfico):**
   * Es la pieza central que une todo. Cuando tocas la pantalla o pulsas una tecla, no le importa cómo estás conectado; este enrutador mira:
     * Si estás en modo Bluetooth y conectado -> Lo despacha a `ZenBleHidServer` o `ZenBluetoothHidManager`.
     * Si estás en modo Red -> Lo despacha al socket binario de 500 Hz.

---

### 🔊 Módulo 6: Audio y Sonido Mecánico
**Archivos:** `audio/ZenSoundEngine.kt` y `audio/ZenAudioSynthesizer.kt`

#### ¿Qué hace?
Otorga la experiencia acústica táctil de un teclado mecánico premium custom.

1. **`ZenSoundEngine.kt` (Muestras de Audio Reales):**
   * Carga grabaciones reales de audio WAV en memoria usando `SoundPool`.
   * Ofrece 8 perfiles de switches (Cherry Blue, Red, Chocolate, Box Navy, Model M Buckling Spring, Topre, etc.).
   * **Modulación de Pitch:** Al presionar la barra espaciadora (`space`), reproduce el sonido a `0.78f` (tono más grave), simulando la acústica de una barra larga con estabilizadores mecánicos reales.

2. **`ZenAudioSynthesizer.kt` (Síntesis Algorítmica Pura):**
   * Genera el sonido desde cero con fórmulas matemáticas de ondas de seno (`sin(2π * f * t)`) y curvas de caída exponencial (`exp(-t * k)`). No requiere archivos de audio externos.

---

### 📳 Módulo 7: Motor Háptico (Vibración LRA / RichTap)
**Archivo:** `haptics/ZenHapticsEngine.kt`

#### ¿Qué hace?
Controla el motor de vibración háptico del teléfono (motores lineales de eje X como los de Xiaomi, Samsung y Google Pixel).

#### Partes clave del código:
* **Primitivas de Android 11+:** En lugar de hacer que el motor tiemble con un zumbido genérico molesto, llama a formas de onda afinadas por hardware:
  * `PRIMITIVE_CLICK`: Clic nítido como romper una lámina de cristal.
  * `PRIMITIVE_THUD`: Golpe seco para simular fondo de tecla mecánica.
  * `PRIMITIVE_TICK`: Micro-paso sutil para la rueda de desplazamiento del scroll.

---

### 🎨 Módulo 8: Sistema de Temas Dinámicos (23 Rices de Linux)
**Archivos:** `theme/Theme.kt`, `theme/ZenThemeManager.kt` y `theme/ZenGamepadThemeEngine.kt`

#### ¿Qué hace?
Gestiona la estética visual de ZenTrack. Sincroniza la apariencia del teléfono con los temas (*rices*) de tu escritorio en Linux (como Tokyo Night, Pamela, Gruvbox, Catppuccin, Nord, Dracula, etc.).

#### Partes clave del código:
* **`ZenThemeConfig`:** Define los colores del fondo, chasis, bordes, leyendas de teclas y sombras 3D.
* **`rememberAnimatedZenTheme()`:** Cuando cambias de tema (o cuando tu PC cambia de tema automáticamente), no parpadea; interpola los colores suavemente mediante curvas de animación `animateColorAsState` con duración configurable (por defecto 450 ms).

---

### 🖱️ Módulo 9: Pantallas e Interacción Táctil (UI)

#### 1. `ui/screens/TrackpadScreen.kt` (El Touchpad de Alta Precisión)
* **Textura de Hidrogel Mate:** Dibuja mediante un shader procedural (`paperMattePaint`) una textura microscópica suave para un confort visual elegante.
* **Acumuladores Sub-píxel:** Las coordenadas táctiles del teléfono son números decimales (ej. `1.34`), pero los monitores de PC se mueven en píxeles enteros. Si descartas los decimales, el cursor tiembla o se traba. ZenTrack guarda los residuos decimales en `subPixelRemainderX` y los suma al siguiente fotograma, logrando un movimiento 100% suave.
* **Curvas de Aceleración de Ratón:**
  * `exponential`: Movimientos lentos mueven el cursor píxel por píxel con precisión de francotirador; deslizamientos rápidos multiplican la velocidad hasta 3 veces.
  * `linear_offset_cap`: Progresión lineal con tope máximo de velocidad.
* **Gestos Multitáctiles:**
  * **1 Dedo:** Mueve el puntero. Tap corto hace clic izquierdo. Mantener pulsado bloquea el arrastre (`mousedown`).
  * **2 Dedos:** Desplazamiento vertical y horizontal fluido. Mantener dos dedos activa arrastre con clic derecho.
  * **3 Dedos:** Deslizamiento lateral rápido para alternar escritorios virtuales en BSPWM / Hyprland.

#### 2. `ui/screens/KeyboardScreen.kt` (El Teclado Mecánico 65%)
* Distribución física completa 65% (sin espacios muertos entre hitboxes).
* **Teclas Modificadoras Pegajosas (*Sticky Keys*):**
  * Un toque en `Shift` o `Ctrl` lo deja encendido temporalmente para la siguiente tecla.
  * Una pulsación larga lo deja bloqueado permanentemente (ideal para atajos complejos con una sola mano).
* **Capa Fn:** Al presionar `Fn`, la fila de números se transforma en las teclas de función `F1` a `F12`, y las teclas laterales se vuelven controles de navegación (`Insert`, `Home`, `End`).
* **Soporte ISO AltGr:** Permite tipear vocales con tilde (`á, é, í, ó, ú`) y la letra `ñ` sin dolores de cabeza.

#### 3. `ui/screens/GamepadScreen.kt` (Mando Gaming & Editor de Layout)
* **Mitad Izquierda:** Joystick flotante dinámico. Aparece donde pongas el pulgar y envía comandos WASD (en modo PC) o el stick analógico izquierdo de Xbox (con soporte para presionar L3 al mantener presionado).
* **Mitad Derecha:** Panel de apuntado rápido para cámara de shooters con botón R3 integrado.
* **Overlay Xbox 360:** Incluye botones A, B, X, Y, D-Pad de 8 direcciones, gatillos LT/RT y bumpers LB/RB.
* **Editor de Posición en Pantalla:** Permite mover, agrandar o achicar cada botón individualmente y guardar diferentes perfiles de distribución para cada juego.

#### 4. `ui/screens/HybridScreen.kt` (Modo Dual Híbrido)
* Coloca el Trackpad en la parte superior y el Teclado en la parte inferior, con una barra divisoria ajustable para trabajar y navegar al mismo tiempo en espacios compactos.

---

### 👁️ Módulo 10: ZenVision (Control Espacial de Manos)
**Paquete:** `com.carlos.zentrack.vision.*`

#### ¿Qué hace?
Permite controlar tu computadora moviendo tu mano en el aire frente a la cámara de tu teléfono, estilo Apple Vision Pro o Meta Quest.

```
       CÁMARA DEL CELULAR (Zero-Copy)
                     │
                     ▼
    Google MediaPipe Tasks Vision
    (Localiza 21 coordenadas de la mano)
                     │
                     ▼
             OneEuroFilter (1€)
    (Elimina el temblor de la mano quieta)
                     │
                     ▼
        KinematicMotionPredictor
    (Proyecta la trayectoria hacia el futuro
     compensando la latencia a 500 Hz)
                     │
                     ▼
            PinchGestureEngine
    (Mide la distancia 3D Pulgar-Índice.
     Si se tocan -> Dispara CLIC)
```

#### Los 4 Componentes Clave:
1. **`HandLandmarkerHelper.kt`:** Configura el modelo de inteligencia artificial `hand_landmarker.task` en el hardware de tu teléfono. Corre en la tarjeta gráfica (GPU) para no calentar la CPU.
2. **`OneEuroFilter.kt` (Filtro 1€):** El problema de las cámaras es que las manos humanas siempre tienen un temblor imperceptible que hace temblar el ratón. Este filtro inteligente aplica un suavizado extremo cuando la mano está casi fija, pero quita todo el filtro en cuanto mueves la mano rápido para que no tenga lag.
3. **`KinematicMotionPredictor.kt` (Predicción Cinemática):** Las cámaras de celular funcionan a 30 o 60 fotogramas por segundo (demasiado lentas para jugar). Este componente calcula la velocidad ($V$) y la aceleración ($A$) de la mano y usa la fórmula de la física:
   $$P(t) = P_0 + V \cdot t + \frac{1}{2} A \cdot t^2$$
   Para "adivinar" dónde estará tu mano en los próximos 28 milisegundos, sintetizando un flujo de movimiento constante a **500 Hz** (cada 2 ms).
4. **`PinchGestureEngine.kt` (Gesto de Pellizco):** Mide la distancia euclidiana en el espacio tridimensional entre la punta del pulgar (punto 4) y la punta del índice (punto 8):
   $$D = \sqrt{(x_2 - x_1)^2 + (y_2 - y_1)^2 + (z_2 - z_1)^2}$$
   Si $D < 0.052$, interpreta que hiciste "clic" con los dedos en el aire.

---

### 🐧 Módulo 11: El Servidor Host en Linux
**Archivos:** `server.js`, `uinput_device.py` y `virtual_gamepad.py`

#### ¿Qué hace cada archivo en tu PC?
1. **`server.js`:**
   * Abre el puerto `3000` y escucha la conexión de tu teléfono.
   * Si recibe paquetes binarios de 6 bytes, los lee y los manda de inmediato a mover el ratón.
   * Si recibe un paquete de teclado cifrado con AES-256-GCM, lo descifra con su clave secreta y envía la tecla a Linux.
   * Mantiene un proceso `xdotool` abierto en modo entrada continua (`stdin -`) para evitar la sobrecarga de arrancar procesos nuevos cada vez que tocas la pantalla.
2. **`uinput_device.py`:**
   * Script en Python que habla con `/dev/uinput` del Kernel de Linux.
   * Crea un dispositivo llamado `"ZenTrack Physical Keyboard & Touchpad"`. Esto permite que gestos como el scroll suave de alta resolución funcionen exactamente como el touchpad de una laptop moderna.
3. **`virtual_gamepad.py`:**
   * Crea un mando con los identificadores exactos de Microsoft Xbox 360 (`Vendor ID: 0x045e, Product ID: 0x028e`).
   * Cuando juegas en Linux a través de Steam, emuladores (RPCS3, PCSX2, Yuzu) o Wine, los juegos creen al 100% que tienes un control oficial de Xbox conectado por cable USB.

---

## 🎓 4. Resumen de Flujo de Datos Completo (Ejemplo Práctico)

¿Qué pasa cuando tocas la tecla **'A'** en el teclado de tu celular?
1. El usuario toca la pantalla sobre la tecla 'A' en `KeyCap.kt`.
2. `KeyCap` detecta el toque sin zonas muertas y avisa a `ZenSoundEngine.kt` (reproduce el clic mecánico) y a `ZenHapticsEngine.kt` (dispara un golpe seco en el motor háptico).
3. Se genera un JSON: `{"type": "keydown", "key": "a"}`.
4. El texto pasa por `ZenCrypto.kt` y se transforma en una cadena ininteligible cifrada con AES-256-GCM.
5. `ZenInputRouter.kt` decide la ruta:
   * Si estás en Bluetooth: Traduce la 'A' a scancode `0x04` y lo emite por radio a tu televisor o PC.
   * Si estás en Red: `WebSocketManager.kt` lo envía por el cable USB ADB en menos de 0.2 milisegundos.
6. En la PC, `server.js` recibe el mensaje, valida la autenticidad con el Auth Tag, lo descifra y se lo pasa a `uinput_device.py`.
7. El Kernel de Linux recibe el evento `KEY_A` exactamente igual que si un teclado físico de escritorio hubiera cerrado su circuito eléctrico.

---
*Documento generado para el proyecto ZenTrack.*
