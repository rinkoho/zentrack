# 🌌 ZenTrack: Identidad Visual Tokyo Night & Variantes SVG

Este repositorio contiene la identidad visual oficial de **ZenTrack** en la paleta **Tokyo Night**, enfocada en el concepto seleccionado: **The Zen Vector "Z"**.

---

## 🎨 Paleta Oficial: *Tokyo Night*

| Color | Hex | Rol en la Identidad ZenTrack |
| :--- | :--- | :--- |
| **Dark Night** | `#1A1B26` | Fondo principal (chasis del trackpad y canvas de la app) |
| **Storm Deep** | `#16161E` | Contorno de elevación, sombras y superficies profundas |
| **Azure Blue** | `#7AA2F7` | Cursor de alta precisión, estelas de movimiento y sampling 500 Hz |
| **Electric Cyan** | `#7DCFFF` | Puntos de contacto táctil, scroll suave `REL_WHEEL_HI_RES` |
| **Lilac Purple** | `#BB9AF7` | Ondas hápticas (ZenHapticsEngine / RichTap) y cifrado E2EE |
| **Neon Pink** | `#F7768E` | Acentos de velocidad, modo Gamepad Steam y estado activo |
| **Clean White** | `#C0CAF5` | Tipografía principal y leyendas de teclas |

---

## ⚡ Las 4 Variantes en SVG Puro de *The Zen Vector "Z"*

Todos los archivos se encuentran en la carpeta [docs/branding/svg/](file:///home/carlos/Projects/zentrack/docs/branding/svg/):

### 1. 🎀 [Variante A: Ribbon Flow (Cinta Neón Continua)](file:///home/carlos/Projects/zentrack/docs/branding/svg/zen_vector_z_variant_a_ribbon.svg)
* **Archivo:** `docs/branding/svg/zen_vector_z_variant_a_ribbon.svg` (~2.1 KB)
* **Diseño:** Fiel a la propuesta inicial: una cinta de luz continua con curvatura aerodinámica, gradiente de cuatro colores Tokyo Night y núcleo blanco de alta luminiscencia con resplandor neón (*bloom*).
* **Simbolismo:** Fluidez total del cursor, aceleración con `OneEuroFilter` y cero latencia.

### 2. 🔀 [Variante B: Kinetic Dual-Track (Ejes X-Y)](file:///home/carlos/Projects/zentrack/docs/branding/svg/zen_vector_z_variant_b_dual.svg)
* **Archivo:** `docs/branding/svg/zen_vector_z_variant_b_dual.svg` (~1.9 KB)
* **Diseño:** Dos rieles paralelos independientes (Eje X en Cyan/Azul y Eje Y en Púrpura/Rosa) acompañados de nodos de entrada táctil a 500 Hz en los vértices y retícula cartesiana sub-píxel tenue.
* **Simbolismo:** Las coordenadas táctiles $(X, Y)$ y la conexión simétrica entre el cliente móvil y el servidor Rust.

### 3. 📐 [Variante C: Sharp Precision Monogram](file:///home/carlos/Projects/zentrack/docs/branding/svg/zen_vector_z_variant_c_geometric.svg)
* **Archivo:** `docs/branding/svg/zen_vector_z_variant_c_geometric.svg` (~1.8 KB)
* **Diseño:** Isotipo de ingeniería con cortes limpios a 45º, bisel técnico interior e indicador LED de conexión activa. 
* **Simbolismo:** Precisión matemática de píxel a nivel de kernel y drivers de hardware. Extraordinaria legibilidad para favicons de 16x16 o 32x32 px.

### 4. 📳 [Variante D: Haptic Pulse "Z"](file:///home/carlos/Projects/zentrack/docs/branding/svg/zen_vector_z_variant_d_haptic.svg)
* **Archivo:** `docs/branding/svg/zen_vector_z_variant_d_haptic.svg` (~2.2 KB)
* **Diseño:** La cinta dinámica Z integrada con ondas concéntricas de pulso háptico emanando del punto de impacto en el digitalizador.
* **Simbolismo:** La respuesta física de los actuadores de vibración RichTap / LRA al tocar la pantalla.

---

## 🌐 Cómo Visualizar en el Navegador

Ejecuta en tu terminal:
```bash
xdg-open docs/branding/preview.html
```
Podrás ver las cuatro variantes renderizadas en tiempo real en formato SVG, descargarlas o inspeccionar su código fuente.
