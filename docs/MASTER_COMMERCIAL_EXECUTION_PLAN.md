# 🚀 ZenTrack: Plan Maestro de Pulido Comercial y Hoja de Ruta Blindada

> **Propósito:** Especificación técnica blindada y ejecutable para agentes de IA autónomos (modo loop / long-running).  
> **Objetivo Comercial:** Transformar **ZenTrack** en un producto comercial premium listo para lanzamiento y venta en **Gumroad / web oficial / itch.io**, con cero cabos sueltos, arquitectura de escritorio nativa (System Tray estilo Sunshine/Apollo, TUI hacker interactivo, Web GUI con animaciones fluidas) y total sanitización de marca y licencias.

---

## 🎯 1. Filosofía del Producto y Reglas de Calidad Comercial

1. **Honestidad Técnica Total:**
   - **Núcleo Comercial Garantizado:** Trackpad háptico 500Hz, Teclado Mecánico 65% (con scan codes de hardware y typematic repeat) y Mando Virtual Xbox 360 (ViGEmBus en Windows / uinput en Linux).
   - **Módulos Experimentales (Labs):** Bluetooth HOGP (Smart TVs) y ZenVision (cámara frontal) deben etiquetarse claramente como *Labs / En Investigación*, evitando vender humo o generar expectativas falsas al comprador.
2. **Estética y Tono de Ingeniería:**
   - Eliminar el exceso de emojis. Usar diseño sobrio, tipografía técnica y formato profesional.
   - Root / KernelSU debe quedar registrado como **100% opcional** para entusiastas; la app opera a 500Hz en cualquier Android estándar.
3. **Cero Fricción en el Sistema Operativo:**
   - La aplicación no debe dejar consolas negras invasivas. En modo normal, vive en el **System Tray (Bandeja del Sistema)**.
   - En modo terminal (`--tui` o invocación manual), despliega un **TUI interactivo hacker** de alta calidad.

---

## 🏗️ 2. Arquitectura Técnica de Ejecución (Detalles para evitar Bloqueos)

### 2.1. Arquitectura de Hilos para el System Tray (Crítico para Evitar Congelamientos)
> ⚠️ **TRAMPA COMÚN:** Los bucles de mensajes de GUI/Tray (Win32 `GetMessage` / Linux `GLib`) son bloqueantes y colisionan si se ejecutan dentro del bucle de eventos asíncrono de Tokio.

* **Patrón Obligatorio:**
  ```
  [ Proceso ZenTrack ]
         │
         ├──> [ Hilo Principal (Main Thread) ] ──> System Tray Event Loop (Win32 / Linux)
         │        ├── Icono en bandeja (junto al reloj)
         │        └── Menú contextual (Web GUI, ADB Toggle, Restart, Quit)
         │
         └──> [ Hilo Secundario (Background Thread) ] ──> Tokio Multi-Thread Runtime
                  ├── Axum HTTP + WebSockets (Puerto 3000)
                  ├── InputDriver (SendInput / uinput / ViGEmBus)
                  └── Health Monitor & ADB Tunnel Watcher
  ```
* **Librerías Recomendadas:** `tray-icon` + `winit`/`tao` (o Win32 directo vía `windows-sys` con `Shell_NotifyIconW` para Windows y Ayatana/AppIndicator para Linux).

### 2.2. Arquitectura del TUI Interactivo (Terminal User Interface)
* **Librería Recomendada:** `ratatui` + `crossterm`.
* **Manejo de Logs sin Deformación de Pantalla:**
  - Redirigir la salida de `tracing` / `println!` a un canal de memoria circular (`VecDeque<String>` de los últimos 100 eventos).
  - El TUI dibuja este buffer en un widget de consola integrado dentro del layout sin desbordar el renderizado.
* **Componentes Visuales del TUI:**
  - *Header:* Título, versión comercial, uptime, CPU/Memoria y puerto activo.
  - *Left Panel:* Código QR de emparejamiento renderizado con bloques Unicode (`▀▄`).
  - *Center Panel:* Dispositivos conectados, latencia estimada, switches de estado (Driver, ADB, Gamepad).
  - *Footer:* Barra de atajos rápidos (`[P] Web Pair`, `[U] USB ADB`, `[R] Restart`, `[T] Tray Mode`, `[Q] Salir`).

### 2.3. Web GUI (`public/pair.html`) & Mitigación de Windows SmartScreen
* **Animaciones:** Aplicar transiciones CSS fluidas (`cubic-bezier(0.16, 1, 0.3, 1)`) en tarjetas y badges.
* **Componente SmartScreen:** Diseñar un modal/banner elegante desplegable:
  - *Título:* "Aviso de Seguridad en Windows (SmartScreen)"
  - *Explicación clara:* "Al ser un ejecutable independiente de código abierto recién compilado, Windows Defender muestra una pantalla preventiva. Haz clic en **Más información** y luego en **Ejecutar de todas formas**."
  - *Gráfico visual / SVG de los dos pasos requeridos.*

### 2.4. Regulador de Polling Rate (Hz) en Android
* **Ubicación:** `android/app/src/main/java/com/carlos/zentrack/ui/screens/SettingsScreen.kt`.
* **Preferencias:** Guardar en `ZenPreferences.kt` con la clave `pref_network_hz`.
* **Valores:**
  - `500 Hz` (Gaming / USB Cable / Wi-Fi 5GHz) $\rightarrow$ delay 2.0 ms
  - `250 Hz` (Equilibrado / Wi-Fi estándar) $\rightarrow$ delay 4.0 ms
  - `125 Hz` (Redes Inestables / 2.4GHz con interferencias) $\rightarrow$ delay 8.0 ms
  - `60 Hz` (Modo Conservador / Ahorro de batería) $\rightarrow$ delay 16.6 ms
* **Lógica de Despacho:** En `WebSocketManager.kt` / `TrackpadScreen.kt`, dosificar el envío de deltas usando un temporizador o acumulando el delta diferencial para no perder distancia recorrida.

---

## 📋 3. Hojas de Ruta por Fases (Milestones Atómicos)

```
Fase 1: Sanitización e IP  ──>  Fase 2: Hz Slider Android  ──>  Fase 3: System Tray  ──>  Fase 4: TUI & Web GUI  ──>  Fase 5: Build & Verificación
```

### 🔹 Fase 1: Sanitización de Marca, Licencias y README Comercial
- [ ] Eliminar todas las cadenas `gh0stzk` en `public/`, `android/app/src/main/assets/public/` y archivos HTML/CSS.
- [ ] Reemplazar nombres de rices personales por nombres de temas universales (Tokyo Night, Dracula, Nord, Everforest, Rose Pine, etc.).
- [ ] Reescritura comercial de `README.md`:
  - Enfoque en Trackpad, Teclado 65% y Mando Xbox 360.
  - Eliminar exceso de emojis.
  - Bluetooth HOGP y ZenVision catalogados como *Labs / Experimental*.
  - Explicar que Root es opcional.
  - Añadir sección sobre SmartScreen y descarga del instalador autónomo.
- [ ] Eliminar o ignorar carpetas residuales (`backup_trackpad/`, `scratch/`).

### 🔹 Fase 2: Regulador de Frecuencia (Hz) en Android
- [ ] Añadir Slider / RadioGroup de Frecuencia (500Hz, 250Hz, 125Hz, 60Hz) en `SettingsScreen.kt`.
- [ ] Conectar con `ZenPreferences.kt` para persistencia local.
- [ ] Aplicar el throttling / pacing en el emisor de eventos del Trackpad sin pérdida de recorrido.
- [ ] Compilar APK con `./gradlew assembleDebug` y verificar cero errores.

### 🔹 Fase 3: Integración de Bandeja del Sistema (System Tray) en `server-rust`
- [ ] Añadir soporte de System Tray multiplataforma (Windows y Linux) sin dependencias de Python.
- [ ] Desacoplar el hilo de la bandeja del runtime de Tokio (Main Thread = Tray, Tokio = Background).
- [ ] Menú contextual con acciones: Abrir Web GUI, Activar USB ADB, Reiniciar, Salir.
- [ ] Modo de ejecución: Si se inicia normalmente en Windows (o con flag `--tray`), corre silencioso en la bandeja.

### 🔹 Fase 4: TUI Interactivo Hacker-Style y Rediseño Web
- [ ] Crear modo TUI interactivo para terminal (`--tui` o modo interactivo por defecto en consola).
- [ ] Renderizado de QR, estado de clientes, estadísticas y teclas de atajo.
- [ ] Rediseñar `public/pair.html` con micro-animaciones fluidas y componente ilustrado explicativo de SmartScreen.
- [ ] Sincronizar el estado entre TUI y Web en tiempo real.

### 🔹 Fase 5: Compilación Cruzada y Verificación Integral
- [ ] Compilación Linux: `cargo check` y `cargo build --release` en `server-rust/`.
- [ ] Compilación Windows: `cargo check --target x86_64-pc-windows-gnu` y `cargo build --target x86_64-pc-windows-gnu --release`.
- [ ] Compilación Instalador: Compilar `tools/installer-windows/` con el nuevo binario.
- [ ] Compilación Android: `./gradlew assembleDebug` en `android/`.
- [ ] Git commit limpio y descriptivo en la rama `commercial-v1`.

---

## 🗺️ 4. Mapa de Rutas Indispensables del Repositorio

| Módulo | Ruta Exacta en el Repositorio |
| :--- | :--- |
| **Servidor Rust (Entrada)** | [`server-rust/src/main.rs`](file:///home/carlos/Projects/zentrack/server-rust/src/main.rs) |
| **Servidor Web / APIs** | [`server-rust/src/web.rs`](file:///home/carlos/Projects/zentrack/server-rust/src/web.rs) |
| **Driver Windows (Win32)** | [`server-rust/src/driver/windows.rs`](file:///home/carlos/Projects/zentrack/server-rust/src/driver/windows.rs) |
| **Driver Linux (uinput)** | [`server-rust/src/driver/linux.rs`](file:///home/carlos/Projects/zentrack/server-rust/src/driver/linux.rs) |
| **Telemetría / Salud** | [`server-rust/src/health.rs`](file:///home/carlos/Projects/zentrack/server-rust/src/health.rs) |
| **Instalador Windows** | [`tools/installer-windows/src/main.rs`](file:///home/carlos/Projects/zentrack/tools/installer-windows/src/main.rs) |
| **Web Pairing Center** | [`public/pair.html`](file:///home/carlos/Projects/zentrack/public/pair.html) |
| **Estilos Web** | [`public/style.css`](file:///home/carlos/Projects/zentrack/public/style.css) |
| **Ajustes Android** | [`android/app/src/main/java/com/carlos/zentrack/ui/screens/SettingsScreen.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/ui/screens/SettingsScreen.kt) |
| **Preferencias Android** | [`android/app/src/main/java/com/carlos/zentrack/preferences/ZenPreferences.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/preferences/ZenPreferences.kt) |
| **Enrutador Android** | [`android/app/src/main/java/com/carlos/zentrack/bluetooth/ZenInputRouter.kt`](file:///home/carlos/Projects/zentrack/android/app/src/main/java/com/carlos/zentrack/bluetooth/ZenInputRouter.kt) |
| **Documentación Pública** | [`README.md`](file:///home/carlos/Projects/zentrack/README.md) |
| **Guía Windows** | [`docs/WINDOWS_TECHNICAL_GUIDE.md`](file:///home/carlos/Projects/zentrack/docs/WINDOWS_TECHNICAL_GUIDE.md) |

---

## 🚦 5. Criterios de Aceptación Innegociables (Definition of Done)

Para que el loop de trabajo se considere **100% exitoso**:
1. **0 Menciones a gh0stzk** en todo el árbol de código rastreado por Git (`git grep -i "gh0stzk"` debe retornar vacío).
2. **README Comercial Impecable:** Claro, honesto, sin emojis excesivos y enfocado en la propuesta de valor comercial.
3. **Compilación Limpia Multiplataforma:**
   - Linux: `cargo build --release` finaliza sin errores.
   - Windows: `cargo build --target x86_64-pc-windows-gnu --release` genera el servidor de Windows.
   - Instalador: `ZenTrack-Setup.exe` se compila sin errores.
   - Android: `./gradlew assembleDebug` compila en verde.
4. **Bandeja del Sistema Operativa:** El servidor minimiza a la bandeja del sistema sin dejar ventanas de consola abiertas por defecto.
5. **Ajustes de Hz Operativos:** El control de Hz en Android altera efectivamente la cadencia de reporte.
