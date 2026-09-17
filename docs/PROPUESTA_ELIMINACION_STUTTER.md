# Propuesta Técnica — Eliminación del Tirón (Stutter) en la Transmisión HID Bluetooth/BLE de ZenTrack

> **Documento de decisión e implementación.** Destinado a un modelo/ingeniero sucesor facultado para decidir, implementar y validar la solución.
> **Contexto:** ZenTrack, controlador táctil nativo Android (Kotlin + Jetpack Compose) + backend Linux (Node.js + Python/uinput).
> **Código:** `~/Projects/zentrack` — módulos `bluetooth/ZenBluetoothHidManager.kt` (BT Clásico HID) y `bluetooth/ZenBleHidServer.kt` (BLE HOGP).
> **Referencia previa:** `HANDOVER_TECHNICAL_SPEC.md` (diagnóstico del modelo saliente).

---

## 1. Resumen Ejecutivo

El tirón periódico del puntero al controlar una Smart TV o PC por Bluetooth persiste después de desacoplar el hilo táctil de UI del envío de reportes. El diagnóstico previo atribuye el remanente a un **batido de frecuencias** (aliasing) entre el digitalizador táctil (120 Hz) y el pacer de envío (100/125 Hz), con un período de batido de 50 ms.

Tras análisis crítico del estado del arte del propio proyecto, esta propuesta sostiene que:

1. El aliasing 120/100 Hz **existe y es un componente real, pero no es la única —ni necesariamente la principal— causa**.
2. La causa dominante y **accionable a nivel de SDK (sin root)** es la **desigualdad de timing entre reportes consecutivos** (jitter), originada por:
   - `Thread.sleep()` con imprecisión de ±1–10 ms.
   - La llamada síncrona `BluetoothHidDevice.sendReport()` sobre Binder IPC, que puede bloquear 10–25 ms durante retransmisiones L2CAP, distorsionando la cadencia del propio hilo pacificador.
   - La coalescencia de notificaciones BLE por intervalo de conexión del receptor.
3. La solución no es "enviar más rápido", sino **regularizar el reloj de entrega** y **distribuir los deltas de forma continua** para acoplarse a la cadencia de muestreo desconocida del receptor.
4. Los objetivos de rendimiento deben fijarse por **medición empírica** (timestamps de eventos táctiles, latencia real de `sendReport`, intervalo/sniff negociado, A/B perceptivo), no por matemática teórica.

---

## 2. Contexto y Estado Actual

### 2.1 Arquitectura vigente (productor-consumidor)

- **Productor (UI, `TrackpadScreen.kt`):** acumula deltas flotantes en RAM (`pendingDx += dx`) en <0.1 ms. Nunca bloquea.
- **Consumidor (Pacer):** hilo dedicado con `THREAD_PRIORITY_URGENT_DISPLAY` que despierta por `notify()` o por temporizador, lee los acumuladores, aplica filtro sub-píxel, y transmite:
  - BT Clásico: `hid.sendReport()` (síncrono, en el hilo del pacer).
  - BLE: `server.notifyCharacteristicChanged()` (asíncrono, encolado).
- **Cadencia nominal:** BT Clásico 8 ms (125 Hz); BLE 10 ms (100 Hz).

### 2.2 Lo ya resuelto

- Eliminado el congelamiento total de la pantalla táctil por Binder IPC (`recv.awaitResultNoInterrupt()`).
- Latencia de entrada imperceptible en modo Red/USB ADB (<0.2 ms).

### 2.3 Lo pendiente (objetivo de esta propuesta)

- Tirón periódico remanente en Bluetooth Clásico (PC) y BLE (Smart TV).

---

## 3. Análisis Crítico del Diagnóstico Previo

### 3.1 Puntos correctos (se respaldan)

- `BluetoothHidDevice.sendReport()` es una llamada síncrona Binder IPC; ejecutarla en el hilo de UI es un error de diseño.
- El batido temporal $|f_{touch} - f_{pacer}|$ produce acumulación periódica de 2× delta en una misma ventana. La matemática del aliasing es válida.

### 3.2 Vacíos y premisas débiles (se corrigen)

| # | Vació / premisa | Corrección propuesta |
| :- | :- | :- |
| 1 | **Premisa "120 Hz táctiles".** Con `zentrack boost` (498–501 Hz) y `requestUnbufferedDispatch`, la tasa real de eventos puede ser ~500 Hz, no 120 Hz. El batido asumido de 20 Hz depende de un dato nunca medido. | Instrumentar `eventTime`/`getHistoricalEventTime` y confirmar la tasa real antes de fijar cualquier cadencia. |
| 2 | **El pacer también bloquea.** El documento aísla solo la UI; `sendReport()` síncrono corre en el hilo del pacer, distorsionando su propia cadencia. | Medir la distribución de latencia de `sendReport()` y tratar el pacer con el mismo rigor que a la UI. |
| 3 | **Se subestima la cadencia del receptor.** TV (60 Hz) y PCs muestrean el puntero a su propio ritmo; alimentar a 100/125 Hz en un receptor de 60 Hz produce su propio batido. | Evaluar cadencias divisoras del receptor (p. ej. 60 Hz) mediante A/B, no por suposición. |
| 4 | **Sniff Mode del receptor.** En BT Clásico el intervalo se negocia en el receptor; los parámetros QoS SDP son una sugerencia, no una garantía. | Esperar un techo realista en BT Clásico y optimizar la entrega dentro del slot negociado. |
| 5 | **Falta de medición.** Todo el "stutter" se modeló algebraicamente y jamás se capturó un trazo real en el enlace vivo. | btsnoop / `dumpsys bluetooth_manager` / trazas Perfetto como fase obligatoria. |
| 6 | **Ruta de notificación < API 33.** `characteristic.value = value` retiene la referencia del mismo `ByteArray` que el pacer muta en el siguiente tick (posible reporte rasgado bajo encolado). | Copiar el reporte antes de notificar en la rama legacy. |

---

## 4. Causa Raíz Propuesta

> **El receptor percibe un desplazamiento por cada reporte que procesa. Si el intervalo entre reportes varía, el desplazamiento que ve varía, y eso se percibe como tirón.**

Las tres fuentes de variación de intervalo, en orden de impacto estimado:

1. **Bloqueo de `sendReport()` (BT Clásico)** — estocástico (10–25 ms) por Binder + retransmisión L2CAP; el pacer desemboca el delta acumulado en ráfaga al desbloquearse.
2. **Imprecisión de `Thread.sleep()` / scheduling** — jitter regular de ±1–10 ms que desplaza la fase del batido.
3. **Coalescencia de notificaciones BLE** por el intervalo de conexión del receptor (típico 15–30 ms si la TV no solicita el corto), agrupando 2–4 reportes en ráfagas.

Por tanto, las soluciones se encuadran en **ingeniería de timing**, no de ancho de banda.

---

## 5. Soluciones Propuestas por Caso

### Caso A — BLE HOGP (Smart TV, gama alta de mejora)

**Objetivo:** entregar en el intervalo de conexión real, con deltas uniformemente distribuidos y sin ráfagas.

Opciones:
- **A1 (Recomendada) — Metrónomo de precisión adaptativo.** Sustituir el ciclo `sleep(target - elapsed)` por un *pacer* con reloj maestro `SystemClock.elapsedRealtimeNanos()`, espera mixta adaptativa (`LockSupport.parkNanos` + busy-wait de reserva cuando el margen restante < 300 µs) y **corrección de fase por integrador** (si se detecta drift acumulado, se corta el siguiente tick en vez de acumular error). Reutiliza la matemática de recuperación de reloj de audio (AAudio/Oboe).
- **A2 — Distribución de delta en ranura (Token Bucket sub-píxel).** En vez de emitir `roundToInt(accumulado)` por tick (ráfagas), dividir el delta pendiente en máscaras del tamaño del intervalo efectivo y emitir deltas pequeños y continuos. Dondequiera que la TV muestree, ve la velocidad correcta.
- **A3 — Notificar inmediatamente en cada `ACTION_MOVE`.** Sin esperar al tick (el stack encola); combinado con A2 limita el retraso acumulado a un solo intervalo.
- **A4 — Copia del buffer en ruta < API 33.** Evitar reportes rasgados bajo encolado.
- **A5 — A/B de cadencia objetivo:** 60 / 90 / 120/100 Hz según el intervalo negociado observado, para encontrar la que mejor se integra en la cadencia de renderizado de la TV.

**Contexto técnico:** en un *GATT server* Android no se puede forzar `CONNECTION_PRIORITY_HIGH` (lo solicita el *central*, la TV). Por tanto, el foco es acoplarse al intervalo que la TV negocie.

### Caso B — BT Clásico HID (PC, techo moderado)

**Objetivo:** minimizar el impacto del bloqueo síncrono y regularizar la entrega dentro del slot LF/sniff acordado.

Opciones:
- **B1 (Recomendada) — Ejecución del envío en hilo separado del metrónomo.** Separar el "reloj" (que solo mide y decide) del "transmisor" (que ejecuta el `sendReport()` bloqueante en su propio executor). El bloqueo ya no corrompe la cadencia; el delta pendiente se reparte en el próximo slot.
- **B2 — Calibración empírica del slot.** Medir 200–500 muestras de `nanoTime` alrededor de `sendReport()` en conexión viva; derivar el período efectivo y configurar el metrónomo como subharmónico alineado.
- **B3 — `prctl(PR_SET_TIMERSLACK, 0)` vía JNI** sobre el hilo del pacer (permitido sin root sobre sí mismo) para reducir el redondeo del timer del kernel (validar beneficio empíricamente; en Android el slack no es siempre el valor por defecto del kernel).
- **B4 — Buffering de reportes al receptor:** evaluar envío a 60 Hz exactos con reparto uniforme (menos paquetes, menos coexistencia, posible mejor integración con el poll de entrada del PC).

**Límite declarado:** el sniff interval lo decide el PC/TV. Sin root no se puede forzar. El objetivo es llegar **uniforme** a la tasa que el enlace permita, no "lo más rápido posible".

### Caso C — Red / USB ADB (referencia, sin problema)

Servir como **línea base de calidad**: en modo Red/USB la cadencia es determinista (~500 Hz) y no hay tirón. Cualquier solución BT/BLE debe medirse contra esta referencia.

### Caso D — Sincronización global al VSync (`Choreographer`)

Opción transversal (ya mencionada en el handover) para BLE/Clásico: despachar desde `Choreographer.postFrameCallback()` (VSync 120 Hz) para que el reloj de envío coincida con el muestreo del digitalizador. **Riesgo a evaluar:** el VSync del teléfono no está relacionado con la cadencia de la TV; por sí solo no elimina el batido contra el receptor. Se recomienda sólo combinado con A2/B2.

### Caso E — Algoritmos predictivos (investigación)

Si A/B nocturnos no alcanzan el objetivo, evaluar:
- **Filtro One‑Euro adaptado** para suavizar sin lag (ya existe `OneEuroFilter.kt` en el módulo `vision/`; extraer y reutilizar en la ruta BT).
- **Modelo de degradación de deltas:** en slots donde el receptor claramente no procesó (sin eco), reutilizar la muestra anterior con escala (interpolación) — riesgo de "inventar" movimiento si se aplica mal.

---

## 6. Herramientas y Lenguajes Recomendados

### 6.1 Implementación (cambio de código)

| Capa | Lenguaje / Tecnología | Uso | Justificación |
| :- | :- | :- | :- |
| Motor de timing | **Kotlin** (SDK puro, sin librerías externas) | Metrónomo, token bucket, calibración | Cero riesgo de dependencias; hot path descomprimible. |
| Esperas de precisión | **Java `LockSupport.parkNanos`** (SDK) | Esperas cortas sin `sleep` | Precisión y no bloquea threads pool. |
| Optimización de bajo nivel (opcional) | **NDK / C++ + JNI** | busy-wait de precisión, `clock_gettime`, `prctl(PR_SET_TIMERSLACK)` | Acceso a system clock namespaces y ajustes de timer sin root. |
| Medición en runtime | **Logcat + `Log.d` con `System.nanoTime()`** | Timestamps de eventos táctiles y de envío | Zero-dependency diagnóstico. |
| Trazas del sistema | **Perfetto / `adb shell perfetto`**, `simpleperf` | Scheduler, jitter, GC del proceso | Ver GHOST de latencia y GC en el hot path. |
| Bloqueo de GC | **Kotlin** (sin allocs en el hot path) | Confirmar zero-allocation en pacer | Evitar pausas de GC que destrocen la cadencia. |

### 6.2 Diagnóstico en el enlace (análisis)

| Herramienta | Dónde | Qué revela |
| :- | :- | :- |
| `adb shell dumpsys bluetooth_manager` | Celular | Intervalo de conexión BLE/sniff negociado en vivo. |
| btsnoop (`persist.bluetooth.btsnoop=true`) + **Wireshark** | PC (captura del celular) | Latencia por paquete, retransmisiones, slots reales. |
| `btmon` / `hcidump` | PC receptor (Linux) | Cadencia de recepción HID en el otro extremo. |
| `evtest` / `libinput debug-events` | PC receptor (Linux) | Timestamps de llegada del puntero (jitter medible al milisegundo). |
| Cámara **slow‑mo 240 fps** | TV | Medición perceptiva del cursor en pantalla. |
| **Python** (pandas/matplotlib) | Análisis offline | Distribuciones de IRI (inter‑report interval) y percentiles P50/P95/P99. |
| **Node.js** (proyecto) | Tooling | Scripts utilitarios de la misma base del servidor. |

### 6.3 Automatización de pruebas

- **Robot** / **UiAutomator** (o un `Instrumentation` propio) para inyectar trazos sintéticos deterministas (`ACTION_MOVE` con deltas y tiempos fijos) y hacer A/B reproducible de la cadencia del pacer.
- **Script Python** que procese los logs de latencia y genere el veredicto del A/B (disminución de IRI jitter).

---

## 7. Arquitectura Objetivo de los Componentes Nuevos

```
┌─────────────────────────────────────────────────────────────────┐
│ ZenHidClockSyncer.kt     (NUEVO, paquete bluetooth)             │
│  - Reloj maestro: elaboratedRealtimeNanos                       │
│  - Fase estimada del slot RF (por calibración empírica)         │
│  - Integrador de fase (drift correction)                        │
│  - API: tick() → Boolean (¿es momento de transmitir?)           │
├─────────────────────────────────────────────────────────────────┤
│ ZenHidDeltaDistributor.kt (NUEVO)  → Token bucket sub-píxel      │
│  - Reparte delta pendiente en N máscaras del intervalo          │
│  - Cero allocs en hot path                                      │
├─────────────────────────────────────────────────────────────────┤
│ ZenBluetoothHidManager.kt / ZenBleHidServer.kt (MODIFICADOS)    │
│  - Pacer pasa a config vacío de reloj + transmisor separado     │
│  - Copia de reporte en ruta < API 33                            │
│  - Log de diagnóstico (temporario, gateado por BuildConfig.DEBUG)│
└─────────────────────────────────────────────────────────────────┘
```

**Contratos de interfaz sugeridos:**
- `ZenHidClockSyncer.configure(targetPeriodNs, measuredSlotNs)`.
- `ZenHidClockSyncer.tick(nowNs): Boolean` — decide si emitir.
- `ZenHidDeltaDistributor.push(dx, dy)` y `ZenHidDeltaDistributor.consume(slotCapacity): Pair<Int,Int>`.

---

## 8. Plan de Instrumentación (Fase 0, obligatoria antes de implementar)

1. **Tasa táctil real:** en `TrackpadScreen.kt`, registrar `eventTime` y `historySize` por `ACTION_MOVE` durante 3 s de gesto constante; confirmar si el flujo es 120 Hz, 240 Hz o ~500 Hz.
2. **Latencia de `sendReport()`:** instrumentar el pacer (BT Clásico) con `nanoTime()` alrededor de la llamada (200–500 muestras) y volcar estadísticas: media, P50, P95, máx.
3. **Cadencia del pacer actual:** registrar timestamp de cada envío y calcular la distribución de IRI (jitter real actual como línea base).
4. **Intervalo negociado:** `dumpsys bluetooth_manager` con la TV conectada y con el PC conectado; registrar también el número de slots (btsnoop una vez).
5. **Referencia de calidad:** medir las mismas métricas de IRI en modo Red/USB ADB (la "frontera" de lo perceptible).

Entregable de la fase: tabla con tasas medidas por configuración, para fijar objetivos no arbitrarios.

## 9. Métricas de Éxito y Método de Evaluación

| Métrica | Cómo se mide | Objetivo |
| :- | :- | :- |
| IRI jitter (STD de intervalos de envío) | Log de `nanoTime` por reporte | Reducción ≥ 60 % vs línea base actual |
| P95 de latencia `sendReport()` | Instrumentación pacer | Reducción ≥ 40 % (o comportamiento estable) |
| Percentiles de retraso de paquetes (btsnoop) | Wireshark/captura | Sin picos > 30 ms recurrentes |
| Suavidad perceptiva | A/B ciego en distintos gestos vía `libinput debug-events` (PC) y slow‑mo 240 fps (TV) | Clasificación perceptiva "sin tirón" en ≥ 9/10 pruebas |
| Consumo / calor en 10 min de uso | `adb shell dumpsys batterystats` | Sin regresión apreciable vs estado previo |

**Método A/B:** mismo teléfono, misma TV/PC, mismo gesto sintético (Robot), alternando builds (actual vs propuesta) con ID ciego. Juez humano sobre video slow‑mo o juez automático sobre IRI de recepción en PC.

---

## 10. Criterios de Decisión (para el modelo/ingeniero entrante)

1. **Si la tasa táctil medida es ~500 Hz:** priorizar A1+A2+A3 (metrónomo + token bucket + notify-inmediato). El aliasing clásico deja de ser estructurado y pasa a dominar el jitter de timing.
2. **Si la tasa táctil medida es 120 Hz:** mantener la hipótesis de aliasing como co-dominate y priorizar además el caso D (VSync) para cancelar el batido 120/100.
3. **Si `sendReport()` muestra P95 > 15 ms:** confirmar que el bloqueo es la causa dominante en BT Clásico y adoptar B1+B2 sin demora (separación reloj/transmisor).
4. **Si la TV negocia intervalo ≥ 30 ms:** moderar las expectativas en BLE; el techo físico es ~33 reportes/s. Ajustar la entrega a ese intervalo, no intentar "más rápido".
5. **Si B (Clásico) no alcanza el objetivo tras B1–B4:** documentar el límite del SDK y considerar, como decisión de producto, promocionar el modo USB/Red como el "modo pro" en la comercialización (ventaja diferencial ya probada, <0.2 ms).

---

## 11. Riesgos, Límites del SDK y Techo Alcanzable

- **Límite innegociable (sin root):**
  - BT Clásico: sniff interval del receptor (no controlable por la app).
  - BLE: conexión negociada por la TV (sin API para forzar prioridad desde un GATT server).
  - `sendReport()` síncrono: no hay alternativa SDK.
- **Riesgo de sobreingeniería:** el caso E (predictivo) solo si A/B demuestran que el timing no basta.
- **Riesgo de regresión:** cambiar la cadencia afecta la percepción; todo cambio debe pasar por el A/B ciego de la sección 9.
- **Privacidad/comercial: ninguna.** Los cambios son puros, apátridas y locales a la app.

---

## 12. Roadmap Recomendado

| Fase | Alcance | Esfuerzo |
| :- | :- | :- |
| **F0** | Instrumentación y medición (sección 8) | 0.5–1 jornada |
| **F1** | `ZenHidClockSyncer` + `ZenHidDeltaDistributor` (A1+A2) + log diagnóstico | 1–2 jornadas |
| **F2** | A3/Opciones B1+B2 según caso dominante medido | 0.5–1 jornada |
| **F3** | A4 (copia < API 33), perfiles de cadencia 60/90/100/125 configurables | 0.5 jornada |
| **F4** | A/B ciego + afinación de parámetros | 1 jornada |
| **F5** | Limpieza de logs, PR, etiqueta de release | 0.5 jornada |

**Regla de oro:** no fijar ningún período del metrónomo hasta tener la tabla de la Fase 0. Medir siempre antes de codificar.

---

## 13. Conclusión

El tirón remanente es un problema de **regularidad del reloj de entrega**, no de velocidad. Con pura API de Android (sin root), la combinación de un **metrónomo de corrección de fase**, un **token bucket sub-píxel** y **calibración empírica del enlace** debería reducir el IRI jitter por encima del umbral perceptible y, en el caso BLE (Smart TV) —el diferenciador de mercado—, acercarse al techo físico del intervalo negociado. Esta es una ventaja sostenible sobre la competencia, incluida la app comercial analizada, que no implementa ninguna de estas técnicas.

---

*Documento elaborado para decisión e implementación por un modelo/ingeniero sucesor. Toda medición precede a toda parametrización.*