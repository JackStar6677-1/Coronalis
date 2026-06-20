# 📡 Guía de Coronalis — Radioastronomía en DrakesCraft

Coronalis es un addon de Slimefun que te permite construir **observatorios de radioastronomía**: torres con antenas parabólicas conectadas a una consola central que escanea el cielo y produce recompensas de XP.

---

## 🔬 Investigaciones Requeridas

Desbloquéalas en el altar de investigación de Slimefun antes de poder craftear:

| Investigación | Nivel | Desbloquea |
|---|---|---|
| Ingeniería de Radio | 14 | Plato de Antena, Receptor |
| Control PID del Vacío | 24 | Controlador PID, Radiotelescopio |
| Interferometría VLBI | 34 | Celda de Datos |
| Operaciones de Cielo Profundo | 44 | Consola de Control, Eco de Fase |
| Infraestructura de Array | 18 | Cable Coaxial, Núcleo SU |
| Automatización de Observatorio | 36 | Amplificador, Banco de Datos, Calibrador VLBI |
| Estaciones de Escucha | 28 | Torre, Plataforma, Mástil, Plato Parabólico, Repetidor |

---

## 🏗️ Construcción de la Torre (Firewatch)

La torre es la estructura física del observatorio. Los pasos son en orden:

### 1. Anclaje de Cimentación
- Colócalo en el **suelo firme** donde quieras construir.
- Registra al jugador como propietario de la estación.
- **Receta:** Reinforced Plate ×2, Steel Plate ×2, Lead Ingot ×2, Hardened Metal Ingot ×1

### 2. Segmentos de Torre
- Colócalos apilados encima del anclaje, **uno cada 16 bloques** máximo.
- Validan continuidad estructural entre base y plataforma.
- **Receta:** Steel Plate ×2, Copper Ingot ×2, Copper Wire ×2, Scaffolding ×1

### 3. Plataforma de Vigilancia
- Debe estar **≥12 bloques** sobre la cimentación.
- Es el punto habitable y de conexión visual al mástil.
- **Receta:** Reinforced Plate ×2, Steel Plate ×2, Tower Segment ×2, Electric Motor ×1

### 4. Mástil de Radio
- Colócalo **sobre o adyacente** a la plataforma.
- Activa el modo de radiodifusión de la estación. **Obligatorio** para que funcione.
- **Receta:** Copper Wire ×2, Receiver ×1, Steel Plate ×2, Electric Motor ×1, Coaxial Cable ×2

### (Opcional) Plato Parabólico
- Amplía el alcance de enlace entre estaciones de 192 a 512 bloques.
- Requiere línea de visión directa hacia otra estación.
- **Receta:** Aluminum Ingot ×2, Antenna Dish ×1, Copper Wire ×2, Receiver ×1, Steel Plate ×2, Radio Mast ×1

---

## 📡 El Array de Radiotelescopios

Los telescopios son las "antenas" que recogen la señal. Se conectan a la consola por cable coaxial.

### Radiotelescopio (Escucha del Vacío)
- Bloque pasivo, sin GUI propia.
- Colócalo en un radio de **15 bloques horizontal / 3 bloques vertical** desde la Consola de Control.
- Conéctalo a la consola con **Cable Coaxial bloque a bloque**.
- **Máximo 50 telescopios** por red.
- Cada telescopio necesita ser calibrado en **5 parámetros** antes de usarse:

| Parámetro | Descripción |
|---|---|
| Azimut | Orientación horizontal del plato (0–360°) |
| Elevación | Ángulo vertical de la antena (0–90°) |
| Frecuencia | Banda de radio sintonizada |
| Fase | Coherencia con el resto del array |
| Ganancia | Factor de amplificación de señal |

- Un telescopio está **listo** cuando todos los 5 parámetros llegan a 100%.
- **Receta:** Antenna Dish ×2, Receiver ×1, PID Controller ×1, Reinforced Plate ×2, Electric Motor ×2, Coaxial Cable ×1

---

## ⚡ Energía — Signal Units (SU)

El sistema usa su propia unidad de energía llamada **SU (Signal Units)**.

### Fuentes de energía

**Núcleo de Energía SU** (principal):
- Genera **80 SU cada 4 segundos**.
- Debe estar conectado a la Consola por Cable Coaxial.
- Cuantos más núcleos, más SU disponibles.
- **Receta:** Battery ×2, Receiver ×1, Solar Panel ×1, Energy Connector ×2, Reinforced Plate ×2, PID Controller ×1

**Red Slimefun (secundario):**
- La Consola acepta energía eléctrica estándar de Slimefun.
- Capacidad: 8,192 J. Conversión: 4 J = 1 SU.

### Almacenamiento
- Máximo por defecto: **1,000 SU**.
- Cada **Banco de Datos** añade +500 SU al límite máximo.

### Costos de operación

| Operación | Costo base |
|---|---|
| Slew (mover antenas, manual) | 10 SU |
| Correlación (manual) | 50 SU |
| Calibración (manual) | 20 SU |
| Auto-calibración (VLBI, por tick) | 35 SU/tick |
| Standby telescopios | 2 SU × N telescopios / tick |

---

## 🖥️ La Consola de Control

Es el **cerebro** del observatorio. GUI de 54 slots. Se activa colocándola y conectando los demás componentes con Cable Coaxial.

```
┌─────────────────────────────────────────────────┐
│  [SCOPES] [TARGET] [TELEM] [PID] [FAULT] [EVENT] │  ← Info (solo lectura)
│  [Easy] [Inter×2] [Hard×2] [Legendary×2]         │  ← Selección de objetivo
│                                                   │
│  [SLEW]  [REPAIR]  [AUTO]  [CORRELATE]            │  ← Botones de acción
│                                                   │
│  [INPUT]     [OUTPUT]     [INVITE]                │  ← Items
└─────────────────────────────────────────────────┘
```

### Paneles informativos
- **SCOPES:** Cuántos telescopios hay, cuántos calibrados, eficiencia del array.
- **TARGET:** Objetivo seleccionado, coordenadas Az/El.
- **TELEMETRY:** SU actuales, error de apuntado, estado del PID.
- **PID:** Parámetros del controlador (Kp/Ki/Kd), temperatura, corriente.
- **FAULT:** Estado mecánico — NORMAL / MOTOR_STUCK / PID_OVERLOAD / BEARING_FAILURE.
- **EVENT:** Evento cósmico activo y tiempo restante.

### Botones de acción

| Botón | Efecto | Costo |
|---|---|---|
| **SLEW** | Mueve las antenas 1 paso hacia el objetivo | 10 SU |
| **REPAIR** | Limpia una falla mecánica activa | Gratis |
| **AUTO** | Toggle: alineación + correlación automática cada 2s | — |
| **CORRELATE** | Avanza la correlación manualmente | 50 SU |
| **CALIBRATE** | Calibra un parámetro de un telescopio | 20 SU |
| **INVITE** | Agrega un segundo operador a la consola | — |

---

## 🌌 Objetivos Celestes

Selecciónalos en la fila de botones naranjas de la consola. Cada objetivo tiene:
- Coordenadas Az/El fijas donde apuntar las antenas.
- Número mínimo de telescopios requeridos.
- Multiplicador de XP.

| Objetivo | Dificultad | Az / El | Telescopios Mínimos | XP |
|---|---|---|---|---|
| Nebulosa Cabeza de Caballo | Fácil | 95.1° / 14.5° | 1 | ×1.0 |
| Nebulosa del Cangrejo | Intermedio | 83.6° / 22.0° | 2 | ×1.1 |
| Galaxia de Andrómeda | Intermedio | 310.2° / 41.2° | 2 | ×1.2 |
| Púlsar PSR B1919+21 | Difícil | 295.4° / 50.8° | 3 | ×1.5 |
| Sagitario A* | Difícil | 177.3° / 29.1° | 3 | ×1.6 |
| Exoplaneta Kepler-186f | Legendario | 45.9° / 68.7° | 4 | ×2.0 |
| Agujero Negro M87* | Legendario | 215.0° / 37.8° | 4 | ×2.5 |

---

## 🔧 Módulos Auxiliares (conectar por Cable Coaxial)

### Amplificador Criogénico
- Reduce costos de operación: —6 SU/correlación, —2 SU/slew por amplificador.
- Máximo de reducción: 30 SU en correlación, 6 SU en slew.
- **Receta:** Synthetic Sapphire ×2, Receiver ×1, Copper Wire ×2, Advanced Circuit Board ×1, Reinforced Plate ×2, Coaxial Cable ×1

### Banco de Datos Astronómicos
- Añade **+500 SU** al máximo de almacenamiento de la red.
- **Receta:** Synthetic Diamond ×2, Data Cell ×1, Basic Circuit Board ×2, GPS Transmitter 2 ×1, Reinforced Plate ×2, Coaxial Cable ×1

### Calibrador Automático VLBI
- Calibra los telescopios automáticamente cada tick de red (sin tener que clickar CALIBRATE).
- Progresión: Az → El → Fr → Ph → Ga, rotando entre todos los telescopios.
- Costo: 35 SU/tick continuo mientras esté activo.
- **Receta:** Electric Motor ×2, PID Controller ×1, Advanced Circuit Board ×2, GPS Transmitter 3 ×1, Energy Connector ×2, Signal Core ×1

### Repetidor de Señal
- Amplía el alcance de señal entre estaciones (~200 bloques desde su posición).
- No carga chunks. No transmite entre mundos distintos.
- **Receta:** Receiver ×2, GPS Transmitter 2 ×1, Signal Amplifier ×2, Advanced Circuit Board ×1, Energy Connector ×2, Signal Core ×1

---

## 🔌 Conectividad — Cable Coaxial de Fase

- Conecta **bloque a bloque** todos los componentes de la red.
- La consola busca componentes en un radio de 15×3×15 bloques vía BFS.
- Si hay un gap en el cable, el componente no se detecta.
- Al colocar o romper un telescopio en radio de 64 bloques, la red se reconstruye automáticamente.
- **Receta:** Copper Wire ×4, Copper Ingot ×2, Silicon ×1

---

## ⚠️ Fallas Mecánicas

Durante las operaciones SLEW puede ocurrir una falla con 1/10 de probabilidad (o 1/4 durante Tormenta Magnética):

| Falla | Efecto |
|---|---|
| **MOTOR_STUCK** | Antenas bloqueadas, no se pueden mover |
| **PID_OVERLOAD** | Controlador saturado, igual bloquea operaciones |
| **BEARING_FAILURE** | Reduce todos los parámetros de calibración a la mitad. Requiere recalibración completa |

**Solución:** Clickar el botón **REPAIR** en la consola (gratis, instantáneo).

---

## 🌠 Eventos Cósmicos

Ocurren automáticamente cada ~5 minutos con 33% de probabilidad. Se anuncian al servidor con duración.

| Evento | Duración | Efecto |
|---|---|---|
| ☀️ **Llamarada Solar** | 60–180s | **Bloquea correlación completamente** |
| 🌊 **Onda Gravitacional** | 60–180s | +100% XP en descubrimientos |
| 📡 **Burst de Púlsar** | 60–180s | Correlación 1.5× más rápida |
| 🧲 **Tormenta Magnética** | 60–180s | Fallas mecánicas 2.5× más probables (1/4) |

---

## 🔄 Flujo de Juego Completo

```
1. CONSTRUIR TORRE
   └─ Cimentación → Segmentos → Plataforma → Mástil

2. COLOCAR TELESCOPIOS (al menos 1, máx 50)
   └─ Conectar cada uno a la Consola con Cable Coaxial

3. COLOCAR ENERGÍA
   └─ Núcleo SU cableado a la Consola (más núcleos = más SU/s)

4. ABRIR CONSOLA
   └─ Seleccionar objetivo astronómico (fila naranja)

5. ALINEAR ANTENAS
   └─ Clickar SLEW hasta que el error sea < 0.1°
   └─ O activar AUTO para que se alinee solo

6. CALIBRAR TELESCOPIOS
   └─ Clickar CALIBRATE hasta que los 5 parámetros lleguen a 100%
   └─ O conectar un Calibrador VLBI y activar AUTO

7. CORRELACIONAR
   └─ Poner 1x Celda de Datos en el slot INPUT
   └─ Clickar CORRELATE hasta llegar al 100%
   └─ O dejar el modo AUTO que lo hace solo

8. RECOLECTAR RECOMPENSA
   └─ Eco de Fase Coronalis aparece en slot OUTPUT
   └─ Clic derecho sobre él para analizarlo y recibir XP
```

---

## 📦 Ítems Consumibles

### Celda de Datos Celestes
- Se inserta en el slot INPUT de la Consola.
- Se consume al completar una correlación.
- **Receta:** Glass ×2, Silicon ×1, Copper Wire ×2, Synthetic Diamond ×1, Basic Circuit Board ×1

### Eco de Fase Coronalis
- Recompensa que sale en el slot OUTPUT al completar una correlación.
- Tiene datos embebidos en el lore: objeto observado, coordenadas, % de coherencia de fase, tier.
- **Clic derecho** para analizarlo → entrega XP.
- XP final = XP base × multiplicador tier × multiplicador evento activo.

---

## 📖 Manual de Campo

El ítem **Manual de Campo Coronalis** (libro crafteable) contiene guías resumidas de construcción, radio, seguridad y operación. Clic derecho para leerlo. No se consume.

---

## 💡 Consejos

- **Empieza con 3 telescopios.** Desbloqueas los objetivos Difíciles (Sagitario A*, Púlsar) que dan ×1.5–1.6 XP.
- **Prioriza el Calibrador VLBI.** Calibrar 5 parámetros × N telescopios manualmente es muy lento.
- **Conecta varios Amplificadores Criogénicos.** Con 5 amplificadores la correlación cuesta solo 20 SU en vez de 50.
- **Activa modo AUTO durante Burst de Púlsar.** La correlación avanza 1.5× más rápido.
- **Prepara Celdas de Datos en stock.** Cada correlación consume una.
- **Si hay SOLAR_FLARE, pausa.** No gastes SU en correlación, espera que pase.
- **El REPAIR es gratis.** Si tienes BEARING_FAILURE, repara inmediatamente antes de intentar correlacionar.
