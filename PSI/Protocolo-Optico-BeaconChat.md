# Protocolo óptico de BeaconChat — documentación consolidada

> Documento técnico de antecedentes: las afirmaciones y mediciones históricas no constituyen validación física independiente ni conformidad certificada con IEEE o ITU.


---

## Anexo: protocolo óptico y táctil original

> Antecedente conservado para auditoría histórica; resultados declarados y condiciones de 2025 requieren reconfirmación. [Ver archivo original](https://github.com/NicoButter/beaconchat/blob/c364293f42823d863976a5608aede8d3da7a50d3/PROTOCOLO_OPTICO.md).

### Protocolo de Comunicación Óptica - BeaconChat

## 📡 Descripción General

BeaconChat implementa un protocolo estándar de comunicación óptica basado en tecnologías probadas en:
- **Robótica de rescate**
- **Drones autónomos**
- **Beacons ópticos para navegación**
- **Proyectos de investigación universitaria**

Este protocolo permite comunicación confiable mediante luz (LED/flash) en condiciones adversas:
- ✅ Movimiento leve del dispositivo
- ✅ Luz ambiente variable
- ✅ Cámaras de baja calidad
- ✅ Ambientes con polvo/humo
- ✅ Poca visibilidad

---

## 🎯 Estructura del Protocolo

### 1. Marcador de Inicio
Patrón único que indica el comienzo de la transmisión:

```
ON 300ms → OFF 300ms → ON 900ms → OFF 500ms
```

**Propósito:**
- Sincronizar el receptor con el transmisor
- Diferenciar el mensaje de ruido ambiental
- Establecer niveles de referencia de intensidad

### 2. Mensaje en Código Morse
Secuencia de pulsos que representa el texto codificado.

**Tiempos base:**
- **DOT (punto)**: 150ms
- **DASH (raya)**: 400ms (≈2.66x DOT)
- **Espacio entre símbolos**: 150ms
- **Espacio entre letras**: 500ms
- **Espacio entre palabras**: 1000ms

### 3. Marcador de Fin
Patrón único que indica el final de la transmisión:

```
OFF 600ms → ON 100ms
```

**Propósito:**
- Señalar término del mensaje
- Permitir detección de mensajes incompletos
- Facilitar concatenación de múltiples mensajes

---

## ⏱️ Especificación de Tiempos

### Transmisión (MorseEncoder)

| Elemento | Duración | Frames @30fps | Descripción |
|----------|----------|---------------|-------------|
| DOT | 150ms | ~4-5 frames | Pulso corto |
| DASH | 400ms | ~12 frames | Pulso largo |
| Espacio símbolo | 150ms | ~4-5 frames | Entre . y - de una letra |
| Espacio letra | 500ms | ~15 frames | Entre letras |
| Espacio palabra | 1000ms | ~30 frames | Entre palabras |
| Marcador inicio ON₁ | 300ms | ~9 frames | Primer pulso inicio |
| Marcador inicio OFF | 300ms | ~9 frames | Pausa inicio |
| Marcador inicio ON₂ | 900ms | ~27 frames | Segundo pulso inicio |
| Marcador fin OFF | 600ms | ~18 frames | Pausa final |
| Marcador fin ON | 100ms | ~3 frames | Pulso final corto |

### Detección (OpticalOscilloscope)

| Rango | Clasificación | Descripción |
|-------|---------------|-------------|
| < 80ms | RUIDO | Descartado (filtro anti-ruido) |
| 80-200ms | DOT | Punto Morse |
| 200-500ms | DASH | Raya Morse |
| > 500ms | GAP | Espacio entre letras/palabras |

**Umbral de detección:**
- Dinámico, calculado como 40% del rango (max - min) desde el mínimo
- Se adapta automáticamente a condiciones de iluminación

---

## 🔧 Filtrado de Señal

### Filtro Suavizado Exponencial

```kotlin
brightness = 0.7 × prev_brightness + 0.3 × current_brightness
```

**Parámetros:**
- **Factor de suavizado**: 0.7 (70% del valor anterior)
- **Factor de actualización**: 0.3 (30% del valor actual)

**Beneficios:**
1. Elimina jitter de la cámara
2. Reduce ruido de sensores
3. Estabiliza detección ante movimiento
4. Mantiene respuesta rápida a cambios reales
5. Compatible con cámaras de 30fps o menos

### Umbral Dinámico Adaptativo

```kotlin
threshold = min + (max - min) × 0.4
```

**Adaptación automática a:**
- Intensidad del LED transmisor
- Luz ambiente (día/noche)
- Distancia entre dispositivos
- Ángulo de incidencia
- Tipo de superficie reflectante

---

## 📊 Ejemplo de Transmisión

**Texto:** "SOS"

### Codificación Morse:
- **S** = `...` (3 dots)
- **O** = `---` (3 dashes)
- **S** = `...` (3 dots)

### Secuencia completa:

```
[INICIO]
ON 300ms → OFF 300ms → ON 900ms → OFF 500ms

[LETRA S]
ON 150ms (.) → OFF 150ms → 
ON 150ms (.) → OFF 150ms → 
ON 150ms (.) → OFF 500ms (fin letra)

[LETRA O]
ON 400ms (-) → OFF 150ms → 
ON 400ms (-) → OFF 150ms → 
ON 400ms (-) → OFF 500ms (fin letra)

[LETRA S]
ON 150ms (.) → OFF 150ms → 
ON 150ms (.) → OFF 150ms → 
ON 150ms (.) → OFF 600ms (marcador fin)

[FIN]
ON 100ms
```

**Duración total:** ~6.5 segundos

---

## 🎨 Pipeline de Detección

### 1. Captura de Frames
```kotlin
CameraX → ImageAnalysis (modo STRATEGY_KEEP_ONLY_LATEST)
```

### 2. Extracción de Luminosidad
```kotlin
// ROI central (región de interés)
val centerIntensity = calculateCenterIntensity(frame)
// Resultado: valor 0-255
```

### 3. Filtrado
```kotlin
smoothed = 0.7 × previous + 0.3 × current
```

### 4. Umbral Dinámico
```kotlin
if (smoothed > threshold) → ON
else → OFF
```

### 5. Medición Temporal
```kotlin
pulseDuration = endTime - startTime
```

### 6. Clasificación
```kotlin
when {
    duration < 200 → DOT
    duration < 500 → DASH
    else → GAP
}
```

### 7. Decodificación Morse
```kotlin
"..." → S
"---" → O
"..." → S
= "SOS"
```

---

## 🔬 Validación del Protocolo

### Condiciones Probadas

#### ✅ Luz Ambiente
- Interiores con luz artificial
- Exteriores diurnos
- Condiciones nocturnas
- Ambientes con cambios bruscos de iluminación

#### ✅ Movimiento
- Dispositivos fijos
- Movimiento manual leve (±10° vibración)
- Dispositivos en movimiento lento

#### ✅ Hardware
- Cámaras de 15fps a 60fps
- LEDs de diferentes intensidades
- Flash de smartphone estándar

#### ✅ Distancia
- 10cm a 5 metros (óptimo: 50cm-2m)
- Requiere línea de visión directa

---

## 📳 Protocolo de Comunicación Táctil (Vibración)

### 📡 Descripción General

BeaconChat implementa un **protocolo de comunicación táctil** usando el acelerómetro para detectar patrones de vibración Morse. Este canal permite comunicación en situaciones donde la luz no es viable:

- ✅ **Entornos completamente oscuros**
- ✅ **Línea de visión obstruida**
- ✅ **Personas sepultadas bajo escombros**
- ✅ **Comunicación silenciosa sin luz visible**
- ✅ **Transmisión a través de superficies sólidas**

### 🔬 Principio de Funcionamiento

El motor de vibración del smartphone genera aceleración detectable por el acelerómetro:

```
Vibración ON → Aceleración ~2-5 m/s²
Vibración OFF → Aceleración ~0 m/s² (solo gravedad)
```

**Pipeline de detección:**
1. **Captura**: Acelerómetro a ~200Hz (SENSOR_DELAY_FASTEST)
2. **Filtrado**: Eliminar componente de gravedad constante (9.8 m/s²)
3. **Cálculo de magnitud**: √(x² + y² + z²) de aceleración lineal
4. **Suavizado exponencial**: α = 0.7 para eliminar jitter
5. **Normalización**: Escalar a rango 0.0-1.0
6. **Detección de pulsos**: Umbral dinámico al 40% del rango
7. **Decodificación Morse**: Mismo timing que protocolo óptico

### ⚙️ Características Técnicas

#### Filtro Pasa-Alto (Eliminación de Gravedad)

```kotlin
// Gravedad usando filtro pasa-bajo
gravity[i] = 0.8 × gravity[i-1] + 0.2 × accel[i]

// Aceleración lineal
linear[i] = accel[i] - gravity[i]

// Magnitud de vibración
magnitude = √(linear_x² + linear_y² + linear_z²)
```

**Parámetros:**
- **Factor gravedad (α)**: 0.8 (filtro pasa-bajo)
- **Factor suavizado**: 0.7 (eliminar jitter)
- **Frecuencia de muestreo**: ~200Hz

#### Timing de Detección

Usa el **mismo timing que el protocolo óptico** para mantener compatibilidad:

| Rango | Clasificación | Descripción |
|-------|---------------|-------------|
| < 80ms | RUIDO | Descartado (movimientos bruscos) |
| 80-250ms | DOT | Punto Morse |
| 250-600ms | DASH | Raya Morse |
| > 600ms | GAP (letra) | Espacio entre letras |
| > 1500ms | GAP (palabra) | Espacio entre palabras |

#### Umbral Dinámico

```kotlin
threshold = min + (max - min) × 0.4
```

Se adapta automáticamente a:
- Intensidad del motor de vibración
- Distancia entre dispositivos
- Material de la superficie de contacto
- Nivel de ruido ambiental (movimiento)

### 💡 Modo de Uso

**Transmisión:**
1. Abrir pantalla "Transmitir"
2. Escribir mensaje
3. Seleccionar canal "Vibración"
4. Presionar "Transmitir"
5. Dispositivo vibra el patrón Morse

**Recepción:**
1. Abrir pantalla "Detectar"
2. Seleccionar "Vibración"
3. **Colocar ambos dispositivos en contacto directo** (superficie contra superficie)
4. Presionar "Iniciar Detección"
5. El osciloscopio muestra el patrón de vibración en tiempo real
6. Mensaje decodificado aparece automáticamente

### 📊 Visualización en Tiempo Real

El osciloscopio de vibración muestra:
- **Gráfico de magnitud**: Visualización en tiempo real de la intensidad de vibración
- **Umbral dinámico**: Línea amarilla indica nivel de detección
- **Estadísticas**: Min/Max/Promedio/Actual en m/s²
- **Pulsos detectados**: Contador de DOT/DASH identificados
- **Mensaje decodificado**: Texto resultante de la decodificación

### 🎯 Casos de Uso Específicos

#### Persona Sepultada
```
Víctima → Vibra celular contra escombro → Ondas vibratorias se propagan
Rescatista → Acerca celular a superficie → Acelerómetro detecta patrón
Resultado → Mensaje "AYUDA" + ubicación aproximada
```

#### Comunicación Silenciosa
```
Situación de riesgo → No se puede emitir luz
Transmisor → Vibra mensaje en bolsillo/contra pared
Receptor → Detecta vibraciones por contacto
Resultado → Comunicación encubierta sin señales visibles
```

#### Transmisión a Través de Materiales
```
Muro/puerta/ventana → Superficie rígida conduce vibración
Lado A → Vibra mensaje contra superficie
Lado B → Detecta vibración del otro lado
Resultado → Comunicación sin línea de visión
```

### ⚠️ Limitaciones

- **Distancia**: Requiere contacto directo o proximidad < 5cm
- **Superficies**: Mejor con materiales rígidos (madera, metal, vidrio)
- **Ruido**: Sensible a movimiento del dispositivo receptor
- **Batería**: El motor de vibración consume más energía que LED

### 🔬 Referencias Técnicas

- **Android SensorManager**: [TYPE_ACCELEROMETER](https://developer.android.com/reference/android/hardware/Sensor#TYPE_ACCELEROMETER)
- **Filtros de acelerómetro**: [Eliminar gravedad](https://developer.android.com/guide/topics/sensors/sensors_motion#sensors-motion-accel)
- **Procesamiento de señal**: Filtro pasa-alto + suavizado exponencial

---

## 🌍 Soporte Multi-Idioma

BeaconChat soporta **9 sistemas de escritura diferentes** con sus respectivos alfabetos Morse:

### Alfabetos Soportados

| Alfabeto | Idiomas | Código | Bandera | Letras |
|----------|---------|--------|---------|--------|
| **Latino Internacional** | Español, Inglés, Francés, Alemán, Italiano, etc. | ITU-R M.1677 | 🇪🇸 🇬🇧 🇫🇷 🇩🇪 🇮🇹 | 26 + diacríticos |
| **Cirílico** | Ruso, Ucraniano, Bielorruso, Búlgaro, Serbio | ГОСТ | 🇷🇺 🇺🇦 🇧🇾 🇧🇬 🇷🇸 | 33 |
| **Griego** | Griego | Ελληνικό | 🇬🇷 | 24 |
| **Hebreo** | Hebreo | עברי | 🇮🇱 | 22 |
| **Árabe** | Árabe | عربي | 🇸🇦 🇪🇬 🇦🇪 🇲🇦 | 28 |
| **Japonés** | Japonés | Wabun かな | 🇯🇵 | Hiragana |
| **Coreano** | Coreano | 한국어 | 🇰🇷 | Hangul |
| **Tailandés** | Tailandés | ไทย | 🇹🇭 | 44 |
| **Persa** | Persa/Farsi | فارسی | 🇮🇷 | 32 |

### Detección Automática

El sistema detecta automáticamente el idioma configurado en el dispositivo Android (`Locale.getDefault()`) y selecciona el alfabeto Morse correspondiente:

```kotlin
// Detección automática
val encoder = MorseEncoder() // Usa el idioma del sistema

// Sistema en español → Alfabeto Latino
encoder.encode("HOLA") 
// → "....|----|.-..|.-.-" (Latino)

// Sistema en ruso → Alfabeto Cirílico  
encoder.encode("ПРИВЕТ")
// → ".--.|.-.|..|.|.|-.." (Cirílico)

// Sistema en árabe → Alfabeto Árabe
encoder.encode("مرحبا")
// → "--|.-.|....|---|.-" (Árabe)
```

### Identificación Visual con Banderas

Cada alfabeto se identifica con la **bandera del país** correspondiente para confirmación visual inmediata:

**Ejemplos de visualización:**

```
🇪🇸 International Morse (Latin)    - Sistema en español (España)
🇲🇽 International Morse (Latin)    - Sistema en español (México)
🇺🇸 International Morse (Latin)    - Sistema en inglés (USA)
🇷🇺 Cyrillic Morse                 - Sistema en ruso
🇬🇷 Greek Morse                    - Sistema en griego
🇯🇵 Wabun Code (Japanese)          - Sistema en japonés
🇸🇦 Arabic Morse                   - Sistema en árabe
🇮🇱 Hebrew Morse                   - Sistema en hebreo
🇰🇷 Korean Morse                   - Sistema en coreano
🌍 International Morse (Latin)     - Sistema no detectado (fallback)
```

### Variantes Regionales

El sistema distingue entre variantes regionales del mismo idioma:

| Idioma | Variantes Soportadas |
|--------|---------------------|
| **Español** | 🇪🇸 España, 🇲🇽 México, 🇦🇷 Argentina, 🇨🇴 Colombia, 🇨🇱 Chile, 🇵🇪 Perú, 🇻🇪 Venezuela |
| **Inglés** | 🇬🇧 Reino Unido, 🇺🇸 Estados Unidos, 🇨🇦 Canadá, 🇦🇺 Australia, 🇳🇿 Nueva Zelanda |
| **Árabe** | 🇸🇦 Arabia Saudita, 🇪🇬 Egipto, 🇦🇪 Emiratos, 🇲🇦 Marruecos, 🇩🇿 Argelia |
| **Francés** | 🇫🇷 Francia, 🇨🇦 Canadá, 🇧🇪 Bélgica, 🇨🇭 Suiza |
| **Alemán** | 🇩🇪 Alemania, 🇦🇹 Austria, 🇨🇭 Suiza |
| **Portugués** | 🇵🇹 Portugal, 🇧🇷 Brasil |
| **Chino** | 🇨🇳 China, 🇹🇼 Taiwán, 🇭🇰 Hong Kong |

### Caracteres Especiales

Cada alfabeto incluye sus caracteres especiales:

**Latino:**
- Diacríticos: Á, À, Ä, Å, É, È, Ñ, Ö, Ü, Ç
- Puntuación: . , ? ' ! / ( ) & : ; = + - _ " $ @

**Cirílico:**
- Extensiones ucranianas: Є, І, Ї, Ґ
- 33 letras del alfabeto ruso completo

**Árabe:**
- Números orientales: ٠ ١ ٢ ٣ ٤ ٥ ٦ ٧ ٨ ٩
- Números occidentales: 0 1 2 3 4 5 6 7 8 9

**Persa:**
- Números persas: ۰ ۱ ۲ ۳ ۴ ۵ ۶ ۷ ۸ ۹
- Letras específicas del persa: پ چ ژ گ

### Beneficios del Sistema Multi-Idioma

1. **Comunicación global**: Permite transmisión en cualquier idioma del mundo
2. **Detección automática**: Sin configuración manual requerida
3. **Confirmación visual**: Las banderas previenen errores de idioma
4. **Estándares oficiales**: Basado en ITU-R, ГОСТ, Wabun, y otros estándares
5. **Unicode completo**: Soporte total para caracteres especiales
6. **Retrocompatible**: Latino como fallback universal

---

## 🚀 Implementación en BeaconChat

### Archivos Principales

1. **`MorseAlphabet.kt`** (NUEVO)
   - 9 alfabetos Morse completos
   - Detección automática de idioma
   - Mapeo de banderas por país/región
   - Soporte para caracteres especiales

2. **`MorseEncoder.kt`**
   - Codificación de texto a tiempos
   - Inserción de marcadores de protocolo
   - Generación de secuencia ON/OFF
   - Soporte multi-idioma automático

3. **`OpticalOscilloscope.kt`**
   - Análisis de frames de cámara
   - Filtro suavizado exponencial
   - Detección de intensidad central

4. **`PulseDetector.kt`** (inner class)
   - Detección de pulsos ON/OFF
   - Clasificación temporal (DOT/DASH/GAP)
   - Umbral dinámico adaptativo
   - Decodificación Morse multi-idioma

### Flujo de Datos

```
TRANSMISOR:
Texto → MorseEncoder → [Inicio] + Timings + [Fin] → FlashlightController → LED

RECEPTOR:
LED → Cámara → OpticalOscilloscope → Filtro → PulseDetector → Morse → Texto
```

---

## 📈 Métricas de Rendimiento

### Velocidad de Transmisión
- **Caracteres por segundo**: ~0.8-1.2 cps
- **Palabras por minuto**: ~10-15 wpm
- **SOS completo**: ~6.5 segundos

### Tasa de Error
- **Condiciones ideales**: < 5%
- **Movimiento leve**: < 15%
- **Luz variable**: < 20%

### Requisitos Mínimos
- **Cámara**: 15fps o superior
- **CPU**: Análisis en tiempo real requiere procesador moderno
- **Distancia**: 10cm - 5m (óptimo: 50cm - 2m)

---

## 🔧 Configuración Avanzada

### Ajuste de Sensibilidad

Para ambientes **muy oscuros**:
```kotlin
// En OpticalOscilloscope.kt
val threshold = min + (max - min) × 0.3  // Más sensible (30% en vez de 40%)
```

Para ambientes **muy brillantes**:
```kotlin
val threshold = min + (max - min) × 0.5  // Menos sensible (50% en vez de 40%)
```

### Ajuste de Filtro

**Más suavizado** (para mucho ruido):
```kotlin
val SMOOTHING_FACTOR = 0.8f  // 80% anterior, 20% actual
```

**Menos suavizado** (para respuesta más rápida):
```kotlin
val SMOOTHING_FACTOR = 0.6f  // 60% anterior, 40% actual
```

---

## 📚 Referencias

- **IEEE 802.15.7**: Visible Light Communication (VLC)
- **Robotic Rescue Protocols**: NIST Search and Rescue Standards
- **Optical Beacon Navigation**: MIT Media Lab Research
- **Morse Code Standard**: ITU-R M.1677

---

## 🎯 Casos de Uso

### 1. Emergencias
- Señal SOS en situaciones sin conectividad
- Comunicación en desastres naturales
- Rescate en ambientes con humo/polvo

### 2. Investigación
- Experimentos de comunicación óptica
- Proyectos educativos de física/ingeniería
- Pruebas de concepto VLC

### 3. Privacidad
- Comunicación sin radiofrecuencia
- Transferencia de datos en ambientes blindados
- No detectable por interceptores RF

### 4. Automatización
- Control de drones por luz
- Navegación de robots autónomos
- Sincronización de dispositivos IoT

---

## ⚠️ Limitaciones

1. **Línea de visión**: Requiere vista directa entre dispositivos
2. **Velocidad**: Más lento que WiFi/Bluetooth (~1 cps)
3. **Distancia**: Efectivo hasta ~5 metros
4. **Luz solar directa**: Puede interferir con la detección
5. **Batería**: El flash consume energía considerable

---

## 🔮 Futuras Mejoras

- [ ] Implementar checksum CRC-4 para validación
- [ ] Soporte para múltiples canales (RGB)
- [ ] Protocolo binario para mayor velocidad
- [ ] Corrección de errores FEC (Forward Error Correction)
- [ ] Detección de colisiones multi-transmisor
- [ ] Compresión de datos

---

**Versión del Protocolo:** 1.0  
**Fecha:** Diciembre 2024  
**Autor:** BeaconChat Development Team


---

## Anexo: sincronización óptica

> Antecedente conservado para auditoría histórica; resultados declarados y condiciones de 2025 requieren reconfirmación. [Ver archivo original](https://github.com/NicoButter/beaconchat/blob/c364293f42823d863976a5608aede8d3da7a50d3/docs/SINCRONIZACION_OPTICA.md).

### Sincronización Óptica - Guía de Mejoras

**Fecha:** 13 de diciembre de 2025  
**Versión:** 2.0 - Protocolo Sincronizado

---

## Problema Resuelto

La versión anterior tenía **desincronización crítica** entre el transmisor y receptor:

### Timing Antiguo (DESINCRONIZADO)
- **Encoder**: DOT=150ms, DASH=400ms
- **Decoder**: DOT=120ms, DASH=360ms
- **Resultado**: ❌ Imposible decodificar mensajes (ni siquiera "SOS")

### Causa Raíz
1. **Timing inconsistente** entre componentes
2. **Pulsos muy cortos** para cámara a 30fps
3. **Umbral dinámico** demasiado sensible

---

## Solución Implementada

### Nuevo Protocolo Sincronizado (IEEE 802.15.7 + ITU-R M.1677)

#### Timing Unificado
```kotlin
DOT_DURATION    = 200ms    // 6 frames @ 30fps
DASH_DURATION   = 600ms    // 18 frames @ 30fps (ratio 3:1)
SYMBOL_SPACE    = 200ms    // Entre . y -
LETTER_SPACE    = 600ms    // Entre letras (3 DOT)
WORD_SPACE      = 1400ms   // Entre palabras (7 DOT)
```

#### Preámbulo de Sincronización Mejorado
```
LARGO-CORTO-LARGO pattern:
ON 800ms → OFF 400ms → ON 800ms → OFF 800ms
```

**Propósito del preámbulo:**
- Permite al receptor calibrar umbrales automáticamente
- Diferencia señal válida de ruido ambiental
- Establece niveles de referencia (LED ON vs OFF)
- 24+12+24+24 = 84 frames para sincronización

#### Marcador de Fin
```
OFF 1000ms → ON 200ms (1 DOT)
```

---

## Matemática de la Sincronización

### Frame Rate de Cámara: 30 fps
```
1 frame = 33.33ms
6 frames = 200ms  → DOT
18 frames = 600ms → DASH
```

### Detección Confiable
Para que un pulso sea detectado correctamente, necesita **mínimo 6 frames**:
- 1-2 frames: Puede perderse por jitter
- 3-4 frames: Detección dudosa
- **6+ frames**: Detección confiable ✅

### Ratio Morse Estándar (ITU-R M.1677)
```
DASH = 3 × DOT
LETTER_GAP = 3 × DOT
WORD_GAP = 7 × DOT
```

---

## Mejoras en LightDetector

### 1. History Size Aumentado
```kotlin
historySize = 18 frames  // 600ms de histórico
```
Permite calibración estable con los nuevos timing más largos.

### 2. Umbral Dinámico Mejorado
```kotlin
if (max - min > 30) {  // Mayor contraste requerido
    threshold = min + ((max - min) * 40) / 100  // 40% del rango
}
```

**Razones:**
- LED encendido vs apagado: diferencia >30 en luminosidad
- Umbral al 40% favorece detección de luz (bias optimista)
- Reduce falsos positivos de ruido ambiental

### 3. Tolerancia Ampliada
```kotlin
tolerance = 100ms  // ±3 frames de variación permitida
```

Compensa variaciones naturales de frame rate y jitter.

---

## Ejemplo: Transmisión "SOS"

### Timing Completo
```
[PREÁMBULO]
ON  800ms   (24 frames)
OFF 400ms   (12 frames)
ON  800ms   (24 frames)
OFF 800ms   (24 frames)

[S = . . .]
ON  200ms   (6 frames)  - DOT
OFF 200ms   (6 frames)
ON  200ms   (6 frames)  - DOT
OFF 200ms   (6 frames)
ON  200ms   (6 frames)  - DOT
OFF 600ms   (18 frames) - LETTER_GAP

[O = — — —]
ON  600ms   (18 frames) - DASH
OFF 200ms   (6 frames)
ON  600ms   (18 frames) - DASH
OFF 200ms   (6 frames)
ON  600ms   (18 frames) - DASH
OFF 600ms   (18 frames) - LETTER_GAP

[S = . . .]
ON  200ms   (6 frames)  - DOT
OFF 200ms   (6 frames)
ON  200ms   (6 frames)  - DOT
OFF 200ms   (6 frames)
ON  200ms   (6 frames)  - DOT
OFF 1000ms  (30 frames) - END_MARKER

[FIN]
ON  200ms   (6 frames)

TOTAL: ~10.6 segundos
TOTAL FRAMES: ~318 frames @ 30fps
```

---

## Cómo Probar

### 1. Instalación
```bash
# En ambos dispositivos
make deploy
# o
./quick-deploy.sh
```

### 2. Configuración
**Dispositivo A (Transmisor):**
- Abrir BeaconChat
- Ir a "Transmitir"
- Escribir: `SOS`
- Presionar "Enviar con Luz"

**Dispositivo B (Receptor):**
- Abrir BeaconChat
- Ir a "Recibir"
- Apuntar cámara directamente al LED del Dispositivo A
- Presionar "Iniciar Detección"

### 3. Condiciones Óptimas
✅ **SÍ:**
- Ambiente con iluminación moderada
- Distancia: 10-50 cm entre dispositivos
- LED apuntando directamente a cámara
- Cámara estable (no temblar)

❌ **NO:**
- Luz solar directa sobre la cámara
- Distancia >1 metro
- Ángulos muy oblicuos
- Movimiento durante transmisión

### 4. Verificación de Funcionalidad

#### Test 1: SOS (Básico)
```
Texto: SOS
Duración esperada: ~10.6s
Decodificación: "SOS"
```

#### Test 2: HELP (Intermedio)
```
Texto: HELP
Duración esperada: ~19s
Decodificación: "HELP"
```

#### Test 3: Números
```
Texto: 123
Duración esperada: ~24s
Decodificación: "123"
```

---

## Debugging

### Si no detecta nada:
1. **Verificar umbral dinámico:**
   - Observar logs: `threshold`, `min`, `max`
   - Debe haber diferencia >30 entre LED ON y OFF

2. **Verificar frame rate:**
   - Algunos dispositivos usan 24fps o 60fps
   - El protocolo funciona óptimamente a 30fps

3. **Mejorar condiciones:**
   - Reducir distancia
   - Reducir luz ambiental
   - Mejorar alineación LED-cámara

### Si decodifica incorrectamente:
1. **Verificar timing:**
   - DOT debe ser ~200ms (6 frames)
   - DASH debe ser ~600ms (18 frames)

2. **Verificar tolerancia:**
   - Si hay mucho jitter, aumentar `tolerance` a 120ms

3. **Verificar preámbulo:**
   - Debe detectar patrón 800-400-800 para iniciar

---

## Comparación de Velocidad

### Antiguo (Desincronizado)
```
SOS: ~6.5s (NO FUNCIONABA)
```

### Nuevo (Sincronizado)
```
SOS: ~10.6s (FUNCIONA)
```

**Trade-off aceptado:** +63% tiempo de transmisión para **100% confiabilidad**

---

## Fundamento Teórico

### Teorema de Nyquist-Shannon
Para reconstruir una señal, necesitamos muestrear a >2× la frecuencia máxima.

**DOT antiguo (150ms):**
```
Frecuencia = 1/0.15 = 6.67 Hz
Nyquist = 13.33 Hz
Cámara @ 30fps = 30 Hz  ✅ Teóricamente OK
Frames por DOT = 4.5    ⚠️  Muy pocos en práctica
```

**DOT nuevo (200ms):**
```
Frecuencia = 1/0.20 = 5 Hz
Nyquist = 10 Hz
Cámara @ 30fps = 30 Hz  ✅ 3× margen
Frames por DOT = 6      ✅ Confiable
```

### IEEE 802.15.7 (Visible Light Communication)
Recomienda:
- Preámbulo de sincronización distintivo
- Mínimo 5-6 muestras por símbolo
- Umbral adaptativo para condiciones variables
- Marcadores de inicio/fin únicos

---

## Referencias

- **IEEE 802.15.7-2018**: Visible Light Communication Standard
- **ITU-R M.1677**: International Morse Code
- **NIST**: Search and Rescue Communication Protocols

---

## Historial de Cambios

| Fecha | Versión | Cambios |
|-------|---------|---------|
| 2025-12-06 | 1.0 | Implementación inicial |
| 2025-12-13 | 2.0 | Sincronización completa encoder/decoder |

---

**Estado:** ✅ Funcional y probado  
**Próxima mejora:** Adaptación automática de frame rate


---

## Anexo: persistencia de cámara y mejoras

> Antecedente conservado para auditoría histórica; resultados declarados y condiciones de 2025 requieren reconfirmación. [Ver archivo original](https://github.com/NicoButter/beaconchat/blob/c364293f42823d863976a5608aede8d3da7a50d3/docs/CAMERA_LAG_FIX.md).

### Solución al Lag de Persistencia de Cámara

**Fecha:** 13 de diciembre de 2025  
**Problema:** La cámara "ve" luz residual después de que el LED se apaga  
**Estado:** ✅ Solucionado con múltiples técnicas

---

## 🔴 Problema Identificado

### Síntoma
**El LED se apaga instantáneamente, pero la cámara detecta luz durante 2-3 frames adicionales.**

```
LED:     ████████▌                     (apagado en t=0)
Cámara:  ████████████▌                 (sigue viendo luz hasta t=66ms)
         |       |   |
         0ms    33ms 66ms
         LED    Frame Frame
         OFF    +1    +2
```

### Causas Técnicas

#### 1. **Persistencia del Sensor (Sensor Lag)**
Los sensores CMOS tienen tiempo de respuesta:
- **Carga eléctrica residual** en fotodiodos
- **Descarga gradual** en lugar de instantánea
- **Típico: 1-3 frames** de persistencia

#### 2. **Auto-Exposure (AE)**
La cámara ajusta exposición automáticamente:
- **Frame N**: LED brillante → AE ajusta para más brillo
- **Frame N+1**: LED apagado pero **AE todavía configurado para brillante**
- **Frame N+2**: AE empieza a adaptarse
- **Frame N+3**: AE finalmente ajustado

#### 3. **Rolling Shutter**
Cámaras CMOS capturan imagen línea por línea:
```
t=0ms:  Línea 1  ████ (LED ON)
t=5ms:  Línea 2  ████ (LED ON)
t=10ms: Línea 3  ████ (LED OFF ← pero línea 1-2 ya capturadas con ON)
t=15ms: Línea 4  ▌    (LED OFF)
```
**Resultado:** Mezcla de frames ON/OFF en misma imagen

#### 4. **Pipeline de Procesamiento**
```
Sensor → Demosaicing → Denoise → Gamma → YUV → App
 |          |            |         |      |      |
33ms      +5ms        +3ms      +2ms   +2ms   = 45ms total
```
**Latencia pipeline:** 10-20ms adicionales

---

## ✅ Soluciones Implementadas

### 1. **Hysteresis con Confirmación de Frames**

**Concepto:** No cambiar estado inmediatamente. Esperar N frames consecutivos confirmando el cambio.

```kotlin
// Variables de estado
private var stateConfirmationCounter = 0
private val confirmationFramesRequired = 2
private var potentialNewState = false

// Lógica de confirmación
if (potentialState != lastState) {
    if (potentialState == potentialNewState) {
        stateConfirmationCounter++
        if (stateConfirmationCounter >= confirmationFramesRequired) {
            // ✅ CONFIRMADO: 2 frames consecutivos con mismo estado
            lastState = potentialState
            onLightStateChanged(potentialState)
        }
    } else {
        // Estado cambió, resetear contador
        potentialNewState = potentialState
        stateConfirmationCounter = 1
    }
}
```

**Ventajas:**
- Filtra transiciones espurias causadas por lag
- Requiere 2 frames = 66ms para confirmar (aceptable para DOT=200ms)
- Reduce falsos positivos en 95%

**Trade-off:**
- Aumenta latencia de detección en 33-66ms
- Aceptable porque DOT=200ms >> 66ms

---

### 2. **Edge Detection (Detección de Cambios Bruscos)**

**Concepto:** Detectar el cambio de luminosidad, no solo el valor absoluto.

```kotlin
private var previousBrightness = 0
private val edgeThreshold = 20

// Calcular derivada
val brightnessDelta = average.toInt() - previousBrightness
previousBrightness = average.toInt()

// Si hay edge brusco, confirmar más rápido
val requiredFrames = if (Math.abs(brightnessDelta) > edgeThreshold) {
    1 // Edge detectado: ON→OFF o OFF→ON
} else {
    confirmationFramesRequired // Cambio gradual: esperar
}
```

**Ejemplo:**
```
Frame  Brightness  Delta   Estado
1      50          -       OFF
2      200         +150    🔥 EDGE! ON confirmado rápido
3      205         +5      ON (estable)
4      55          -150    🔥 EDGE! OFF confirmado rápido
```

**Ventajas:**
- Detecta transiciones reales (LED ON/OFF) con 1 solo frame
- Ignora cambios graduales (luz ambiente, movimiento)
- Reduce latencia de detección en 50%

---

### 3. **Compensación de Exposición**

**Concepto:** Reducir el tiempo de exposición de la cámara para que responda más rápido.

```kotlin
camera.cameraControl.apply {
    // Reducir exposición en -1 EV
    setExposureCompensation(-1)
}
```

**Efecto:**
- **Exposición normal:** 1/30s (33ms) → frame captura luz durante 33ms
- **Exposición reducida:** 1/60s (16ms) → frame captura luz durante 16ms

**Persistencia reducida:**
```
Normal (-0 EV):   LED OFF → cámara ve luz residual 2-3 frames
Reducida (-1 EV): LED OFF → cámara ve luz residual 1-2 frames
```

**Trade-off:**
- Imagen más oscura (aceptable, solo necesitamos detectar LED)
- No todas las cámaras Android soportan control manual

---

### 4. **Umbral Dinámico Mejorado**

**Concepto:** Requerir mayor contraste para confirmar que es realmente el LED.

```kotlin
if (max - min > 30) {  // Contraste mínimo: 30 unidades de luminosidad
    threshold = min + ((max - min) * 40) / 100
}
```

**Efecto:**
- LED encendido: ~200-255 luminosidad
- LED apagado: ~20-80 luminosidad
- Diferencia: >120 (muy por encima de 30)
- Luz ambiente: diferencia típica <20 → rechazado

**Ventajas:**
- Distingue claramente LED vs luz ambiente
- Reduce falsos positivos de reflejos
- Auto-calibra según condiciones

---

## 📊 Resultados

### Antes (v1.0)
```
LED:    ████▌         ████▌         ████▌
Cámara: ████████▌     ████████▌     ████████▌
        DOT?  DASH?   DOT?  DASH?   ???
        
❌ Imposible decodificar (timing incorrecto)
```

### Después (v2.0)
```
LED:    ████▌         ████▌         ████▌
Cámara: ████▌ ▌       ████▌ ▌       ████▌ ▌
        DOT ✓ GAP     DOT ✓ GAP     DOT ✓
        
✅ Decodificación correcta: "S" (· · ·)
```

---

## 🧪 Validación Experimental

### Test 1: Persistencia de Sensor

**Setup:**
- LED parpadea 1 vez: 200ms ON, 200ms OFF
- Capturar 30 frames (1 segundo @ 30fps)

**Resultados sin optimizaciones:**
```
Frame  Estado LED  Brightness  Estado Detectado
0      OFF         30          OFF
1      OFF         32          OFF
2      ON          240         OFF ← lag 1 frame
3      ON          250         ON
4      ON          255         ON
5      ON          255         ON
6      ON          252         ON
7      OFF         200         ON  ← lag persistencia
8      OFF         150         ON  ← lag persistencia
9      OFF         80          OFF
10     OFF         35          OFF
```
**Lag medido:** 2-3 frames (66-100ms)

**Resultados con hysteresis + edge detection:**
```
Frame  Estado LED  Brightness  Delta   Estado Detectado
0      OFF         30          -       OFF
1      OFF         32          +2      OFF
2      ON          240         +208    OFF (confirmar...)
3      ON          250         +10     ON ✅ (edge confirmado)
4      ON          255         +5      ON
5      ON          255         0       ON
6      ON          252         -3      ON
7      OFF         200         -52     ON (confirmar...)
8      OFF         150         -50     OFF ✅ (edge confirmado)
9      OFF         80          -70     OFF
10     OFF         35          -45     OFF
```
**Lag medido:** 1 frame (33ms) - **Reducción del 50-70%**

---

### Test 2: Decodificación "SOS"

**Con timing sincronizado (DOT=200ms) pero SIN lag fix:**
```
Transmitido:  · · · pause — — — pause · · ·
Decodificado: ·· ·· ·· pause ——— pause ·· ·· ··
Resultado:    ❌ "EEE T EEE" (incorrecto)
```

**Con timing sincronizado Y lag fix:**
```
Transmitido:  · · · pause — — — pause · · ·
Decodificado: · · · pause — — — pause · · ·
Resultado:    ✅ "SOS" (correcto)
```

---

## 🔧 Parámetros de Configuración

### Actuales (Optimizados)
```kotlin
// LightDetector.kt
confirmationFramesRequired = 2  // frames para confirmar cambio
edgeThreshold = 20              // mínimo delta para edge detection
historySize = 18                // frames de histórico para calibración
contrastMinimum = 30            // mínimo contraste LED ON vs OFF

// ReceiverScreen.kt
exposureCompensation = -1       // reducir exposición 1 EV
```

### Si necesitas ajustar:

**Para ambientes MUY oscuros:**
```kotlin
exposureCompensation = 0        // sin compensación
contrastMinimum = 20            // aceptar menor contraste
```

**Para ambientes MUY brillantes:**
```kotlin
exposureCompensation = -2       // reducir 2 EV
contrastMinimum = 50            // requerir mayor contraste
```

**Para cámaras más lentas (<24fps):**
```kotlin
confirmationFramesRequired = 1  // solo 1 frame
edgeThreshold = 15              // más sensible
```

---

## 📚 Referencias Técnicas

### Papers Científicos
1. **"Rolling Shutter Effect in CMOS Image Sensors"**
   - IEEE Transactions on Image Processing
   - Explica el problema del obturador rodante

2. **"Visible Light Communication with LED Flicker Mitigation"**
   - IEEE 802.15.7 Working Group
   - Técnicas de sincronización para VLC

3. **"Camera Sensor Lag Compensation for Real-Time Vision"**
   - Robotics and Autonomous Systems
   - Métodos de predicción y compensación

### Estándares Industriales
- **IEEE 802.15.7-2018:** Visible Light Communication
- **ISO 12232:** Photography — Digital still cameras — Exposure index
- **IETF RFC 8428:** Sensor Measurement Lists (SenML)

---

## 🚀 Próximas Mejoras Potenciales

### 1. Predicción Adaptativa
Usar frames anteriores para predecir el próximo estado:
```kotlin
val predicted = kalmanFilter.predict(history)
if (actual matches predicted) confirmFaster()
```

### 2. Frame Rate Adaptativo
Detectar FPS de cámara y ajustar timing:
```kotlin
val detectedFPS = measureActualFPS()
val adjustedDOT = when {
    detectedFPS < 20 -> 250ms
    detectedFPS > 50 -> 150ms
    else -> 200ms
}
```

### 3. Multi-ROI Analysis
Analizar múltiples regiones para mejor detección:
```kotlin
val regions = [center, topLeft, topRight, bottomLeft, bottomRight]
val consensus = regions.mostCommonState()
```

---

## ✅ Checklist de Verificación

Al probar la app, verificar:

- [ ] LED parpadea limpiamente (sin parpadeos intermedios)
- [ ] Cámara detecta cambios ON→OFF en <100ms
- [ ] No hay falsas detecciones con luz ambiente
- [ ] "SOS" se decodifica correctamente como "SOS"
- [ ] Funciona a distancias de 10-50cm
- [ ] Funciona con iluminación moderada (no sol directo)

---

**Autor:** GitHub Copilot + NicoButter  
**Versión:** 2.0  
**Estado:** Producción ✅
