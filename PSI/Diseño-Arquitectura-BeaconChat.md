# Diseño de Arquitectura de Software
## BeaconChat - Sistema de Comunicación de Emergencia

**Versión:** 1.0  
**Fecha:** 6 de diciembre de 2025  
**Autor:** NicoButter  
**Estado:** Aprobado

---

## Tabla de Contenidos

1. [Introducción](#1-introducción)
2. [Visión Arquitectónica](#2-visión-arquitectónica)
3. [Arquitectura del Sistema](#3-arquitectura-del-sistema)
4. [Vistas Arquitectónicas](#4-vistas-arquitectónicas)
5. [Componentes Principales](#5-componentes-principales)
6. [Patrones de Diseño](#6-patrones-de-diseño)
7. [Flujo de Datos](#7-flujo-de-datos)
8. [Decisiones Arquitectónicas](#8-decisiones-arquitectónicas)
9. [Diagrama de Despliegue](#9-diagrama-de-despliegue)

---

## 1. Introducción

### 1.1 Propósito
Este documento describe la arquitectura de software de BeaconChat, definiendo la estructura de alto nivel del sistema, sus componentes principales, las relaciones entre ellos y los principios de diseño que guían su implementación.

### 1.2 Alcance
El documento cubre:
- Arquitectura general del sistema
- Organización de capas y componentes
- Patrones arquitectónicos aplicados
- Interfaces entre componentes
- Flujos de datos principales

### 1.3 Audiencia
- Desarrolladores del equipo
- Arquitectos de software
- Revisores técnicos
- Mantenedores del sistema

### 1.4 Referencias
- ERS-BeaconChat.md v1.0
- Casos-de-Uso-BeaconChat.md v1.0
- Android Architecture Components
- Clean Architecture (Robert C. Martin)
- MVVM Pattern Documentation

---

## 2. Visión Arquitectónica

### 2.1 Objetivos Arquitectónicos

#### 2.1.1 Separación de Responsabilidades
- **UI separada de lógica:** Jetpack Compose para UI declarativa
- **Lógica de negocio independiente:** Controladores reutilizables
- **Datos aislados:** Capa de persistencia dedicada

#### 2.1.2 Modularidad
- Componentes independientes y reemplazables
- Acoplamiento bajo entre módulos
- Alta cohesión dentro de módulos

#### 2.1.3 Testabilidad
- Inyección de dependencias implícita
- Componentes fácilmente mockeables
- Separación de UI y lógica

#### 2.1.4 Mantenibilidad
- Código organizado por funcionalidad
- Documentación inline exhaustiva
- Convenciones de código consistentes

#### 2.1.5 Rendimiento
- Operaciones asíncronas con Coroutines
- Gestión eficiente de recursos hardware
- Liberación automática de recursos

### 2.2 Restricciones Arquitectónicas

#### 2.2.1 Plataforma
- Android API 26+ (Android 8.0 Oreo)
- Kotlin como lenguaje principal (100%)
- Jetpack Compose para UI

#### 2.2.2 Hardware
- Dependencia de sensores/actuadores físicos
- Gestión cuidadosa de recursos limitados (batería)
- Sin conectividad de red requerida

#### 2.2.3 Concurrencia
- Single Activity con múltiples Composables
- Coroutines para operaciones asíncronas
- StateFlow para estado reactivo

---

## 3. Arquitectura del Sistema

### 3.1 Estilo Arquitectónico Principal

**Patrón: MVVM (Model-View-ViewModel) con Clean Architecture**

```
┌─────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         Jetpack Compose UI (Views)                   │   │
│  │  • Screens (Composables)                             │   │
│  │  • Theme & Styling                                   │   │
│  │  • Navigation                                        │   │
│  └──────────────────────────────────────────────────────┘   │
│                            ▲                                 │
│                            │ State & Events                  │
│                            ▼                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │            MainActivity (ViewModel)                   │   │
│  │  • State Management                                   │   │
│  │  • Navigation Logic                                   │   │
│  │  • Lifecycle Awareness                                │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ▲
                            │ Use Cases
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                       DOMAIN LAYER                           │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Controllers (Business Logic)             │   │
│  │  • FlashlightController                              │   │
│  │  • VibrationController                               │   │
│  │  • SoundController                                   │   │
│  │  • BLEMeshController                                 │   │
│  │  • LightScanner / LightDetector                      │   │
│  │  • VibrationOscilloscope                             │   │
│  └──────────────────────────────────────────────────────┘   │
│                            ▲                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         Encoders / Decoders (Core Logic)             │   │
│  │  • MorseEncoder / MorseDecoder                       │   │
│  │  • BinaryEncoder / BinaryDecoder                     │   │
│  │  • MorseAlphabet (Multi-language)                    │   │
│  │  • QRGenerator / QRScanner                           │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ▲
                            │ Data Access
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                        DATA LAYER                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Data Sources                             │   │
│  │  • UserPreferences (DataStore/SharedPreferences)     │   │
│  │  • MeshPeer (In-Memory)                              │   │
│  │  • ChatMessage (In-Memory)                           │   │
│  │  • DetectedDevice (In-Memory)                        │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ▲
                            │ Hardware Abstraction
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   HARDWARE ABSTRACTION LAYER                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           Android Framework APIs                      │   │
│  │  • Camera2 / CameraX                                 │   │
│  │  • Vibrator / VibratorManager                        │   │
│  │  • AudioTrack                                        │   │
│  │  • SensorManager (Accelerometer)                     │   │
│  │  • BluetoothAdapter / BluetoothGatt                  │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Capas de la Arquitectura

#### Capa de Presentación (Presentation Layer)
**Responsabilidad:** Interfaz de usuario y manejo de interacciones

**Componentes:**
- **Composables (Screens):** Interfaces declarativas en Jetpack Compose
- **MainActivity:** Punto de entrada y coordinador de navegación
- **Theme:** Definiciones de colores, tipografía y estilos Material Design 3

**Tecnologías:**
- Jetpack Compose
- Material Design 3
- Navigation Component

---

#### Capa de Dominio (Domain Layer)
**Responsabilidad:** Lógica de negocio y casos de uso

**Componentes:**
- **Controllers:** Gestión de hardware y operaciones complejas
- **Encoders/Decoders:** Transformación de datos (texto ↔ Morse ↔ binario)
- **Business Logic:** Algoritmos de detección, filtrado y procesamiento

**Características:**
- Sin dependencias de Android Framework (donde es posible)
- Lógica pura y testeable
- Reutilizable

---

#### Capa de Datos (Data Layer)
**Responsabilidad:** Persistencia y gestión de estado

**Componentes:**
- **UserPreferences:** Configuración persistente del usuario
- **In-Memory State:** Estado temporal de sesión (mensajes, peers)

**Tecnologías:**
- SharedPreferences / DataStore
- Kotlin StateFlow
- Data Classes

---

#### Capa de Abstracción de Hardware (HAL)
**Responsabilidad:** Interfaz con hardware del dispositivo

**Componentes:**
- APIs de Android Framework
- Drivers de sensores/actuadores

---

## 4. Vistas Arquitectónicas

### 4.1 Vista Lógica

#### 4.1.1 Diagrama de Paquetes

```
com.nicobutter.beaconchat/
│
├── MainActivity.kt                    [Entry Point & Navigation]
│
├── ui/                                [Presentation Layer]
│   ├── screens/
│   │   ├── WelcomeScreen.kt           [Pantalla inicial]
│   │   ├── TransmitterScreen.kt       [Transmisión de mensajes]
│   │   ├── ReceiverScreen.kt          [Recepción de mensajes]
│   │   ├── EmergencyTransmissionScreen.kt [SOS/HELP]
│   │   ├── MeshScreen.kt              [Red Bluetooth]
│   │   ├── LightMapScreen.kt          [Radar de dispositivos]
│   │   ├── OscilloscopeScreen.kt      [Visualización luz]
│   │   ├── VibrationDetectorScreen.kt [Detección vibración]
│   │   ├── QRTransmissionScreen.kt    [Generación QR]
│   │   └── SettingsScreen.kt          [Configuración]
│   └── theme/
│       ├── Color.kt                   [Paleta de colores]
│       ├── Theme.kt                   [Tema Material Design]
│       └── Type.kt                    [Tipografía]
│
├── transceiver/                       [Domain Layer - Codificación]
│   ├── MorseEncoder.kt                [Text → Morse timings]
│   ├── MorseDecoder.kt                [Morse → Text]
│   ├── MorseAlphabet.kt               [9 alfabetos internacionales]
│   ├── BinaryEncoder.kt               [Text → ASCII binario]
│   ├── BinaryDecoder.kt               [ASCII binario → Text]
│   ├── FlashlightController.kt        [Control LED]
│   ├── VibrationController.kt         [Control motor háptico]
│   ├── SoundController.kt             [Generación tonos]
│   ├── LightDetector.kt               [Análisis frames cámara]
│   ├── VibrationOscilloscope.kt       [Análisis acelerómetro]
│   ├── QRGenerator.kt                 [Generación QR codes]
│   └── QRScanner.kt                   [Lectura QR codes]
│
├── mesh/                              [Domain Layer - Networking]
│   ├── BLEMeshController.kt           [Bluetooth LE mesh]
│   ├── MeshPeer.kt                    [Modelo de peer]
│   └── ChatMessage.kt                 [Modelo de mensaje]
│
├── lightmap/                          [Domain Layer - Radar]
│   ├── LightScanner.kt                [Escaneo de luces]
│   ├── DetectedDevice.kt              [Modelo de dispositivo]
│   ├── HeartbeatPattern.kt            [Patrón heartbeat]
│   └── OpticalOscilloscope.kt         [Osciloscopio óptico]
│
└── data/                              [Data Layer]
    └── UserPreferences.kt             [Persistencia configuración]
```

#### 4.1.2 Relaciones entre Componentes

```
┌─────────────────┐         ┌──────────────────┐
│ TransmitterScreen│─────────│ FlashlightController│
└─────────────────┘         └──────────────────┘
         │                           │
         │                           │
         ▼                           ▼
┌─────────────────┐         ┌──────────────────┐
│  MorseEncoder   │         │  Camera2 API     │
└─────────────────┘         └──────────────────┘
         │
         │
         ▼
┌─────────────────┐
│ MorseAlphabet   │
└─────────────────┘
```

### 4.2 Vista de Procesos

#### 4.2.1 Diagrama de Flujo: Transmisión de Mensaje

```
┌─────────────┐
│   Usuario   │
│ escribe "SOS"│
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│ TransmitterScreen   │
│ • Valida input      │
│ • Detecta alfabeto  │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│   MorseEncoder      │
│ • encode(text)      │
│ • Returns timings[] │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│FlashlightController │
│ • transmit(timings) │
│ • Loop async        │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│   Camera2 API       │
│ • setTorchMode(ON)  │
│ • delay(duration)   │
│ • setTorchMode(OFF) │
└─────────────────────┘
```

#### 4.2.2 Diagrama de Flujo: Recepción de Mensaje

```
┌─────────────┐
│   Usuario   │
│ abre cámara │
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│  ReceiverScreen     │
│ • Inicia cámara     │
│ • Callback frames   │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│   LightDetector     │
│ • analyze(frame)    │
│ • calcBrightness()  │
│ • applyFilter()     │
│ • detectPulses()    │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│   MorseDecoder      │
│ • classifyPulse()   │
│ • DOT/DASH/GAP      │
│ • decode()          │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│      UI Update      │
│ • Show message      │
│ • Play sound        │
└─────────────────────┘
```

### 4.3 Vista de Desarrollo

#### 4.3.1 Estructura de Directorios

```
beaconchat/
├── app/
│   ├── build.gradle.kts              [Configuración build]
│   ├── proguard-rules.pro            [Ofuscación]
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   [Permisos y metadata]
│       │   ├── java/com/nicobutter/beaconchat/
│       │   │   ├── MainActivity.kt
│       │   │   ├── ui/
│       │   │   ├── transceiver/
│       │   │   ├── mesh/
│       │   │   ├── lightmap/
│       │   │   └── data/
│       │   └── res/
│       │       ├── drawable/          [Iconos vectoriales]
│       │       ├── values/
│       │       │   ├── colors.xml
│       │       │   ├── strings.xml
│       │       │   └── themes.xml
│       │       └── xml/
│       ├── androidTest/              [Tests instrumentados]
│       └── test/                     [Unit tests]
├── gradle/
│   ├── libs.versions.toml            [Catálogo de dependencias]
│   └── wrapper/
├── build.gradle.kts                  [Build raíz]
├── settings.gradle.kts               [Configuración proyecto]
├── Makefile                          [Scripts de build]
├── deploy-to-phone.sh                [Script despliegue]
├── README.md
└── PSI/                              [Documentación]
    ├── ERS-BeaconChat.md
    ├── Casos-de-Uso-BeaconChat.md
    └── Diseño-Arquitectura.md
```

### 4.4 Vista Física (Despliegue)

```
┌─────────────────────────────────────────┐
│      Dispositivo Android Físico         │
│  ┌───────────────────────────────────┐  │
│  │   Android OS (API 26-34)          │  │
│  │  ┌─────────────────────────────┐  │  │
│  │  │   BeaconChat APK            │  │  │
│  │  │  • Dalvik/ART Runtime       │  │  │
│  │  │  • Jetpack Compose UI       │  │  │
│  │  └─────────────────────────────┘  │  │
│  │           ▲         ▲              │  │
│  │           │         │              │  │
│  │  ┌────────┴───┐ ┌──┴──────────┐   │  │
│  │  │ Hardware   │ │ Data Storage│   │  │
│  │  │ • Camera   │ │• SharedPrefs│   │  │
│  │  │ • LED      │ └─────────────┘   │  │
│  │  │ • Vibrator │                    │  │
│  │  │ • Bluetooth│                    │  │
│  │  │ • Sensors  │                    │  │
│  │  └────────────┘                    │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

---

## 5. Componentes Principales

### 5.1 MainActivity

**Propósito:** Coordinador principal y gestor de navegación

**Responsabilidades:**
- Inicializar controladores de hardware
- Gestionar navegación entre screens
- Coordinar ciclo de vida de recursos
- Cleanup de hardware al cambiar de pantalla

**Dependencias:**
```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var flashlightController: FlashlightController
    private lateinit var vibrationController: VibrationController
    private lateinit var soundController: SoundController
    private lateinit var meshController: BLEMeshController
    private lateinit var userPreferences: UserPreferences
}
```

**Ciclo de Vida:**
```
onCreate() → Inicializa controladores
  ↓
setContent() → Configura UI Compose
  ↓
Navigation → Gestiona cambios de pantalla
  ↓
onPause() → Cleanup de recursos
  ↓
onDestroy() → Liberación final
```

---

### 5.2 FlashlightController

**Propósito:** Control del LED de la linterna para transmisión Morse

**Interfaz Pública:**
```kotlin
class FlashlightController(private val context: Context) {
    suspend fun transmit(timings: List<Long>)
    fun turnOn()
    fun turnOff()
    fun cleanup()
}
```

**Algoritmo de Transmisión:**
```
1. Para cada timing en timings[]:
   a. Si índice es par → LED ON
   b. Si índice es impar → LED OFF
   c. Delay(timing[i])
2. Asegurar LED OFF al final
```

**Dependencias:**
- Camera2 API (`CameraManager`)
- Coroutines (operaciones async)

**Gestión de Recursos:**
- Libera cámara en `cleanup()`
- Apaga LED en `onPause()`

---

### 5.3 MorseEncoder

**Propósito:** Codificación de texto a secuencias temporales Morse

**Interfaz Pública:**
```kotlin
class MorseEncoder {
    fun encode(text: String): List<Long>
    fun getAlphabetName(): String
    fun getCurrentLocale(): Locale
}
```

**Algoritmo de Codificación:**
```
1. Detectar idioma del texto (MorseAlphabet.detectScript())
2. Obtener tabla Morse correspondiente
3. Para cada carácter:
   a. Buscar en tabla Morse
   b. Convertir DOT (·) → 150ms ON
   c. Convertir DASH (—) → 400ms ON
   d. Agregar GAP inter-símbolo (150ms OFF)
   e. Agregar GAP inter-letra (400ms OFF)
4. Agregar marcador START al inicio
5. Agregar marcador END al final
6. Retornar List<Long> con timings
```

**Timing Estándar:**
```kotlin
DOT_DURATION = 150L    // ms
DASH_DURATION = 400L   // ms
SYMBOL_GAP = 150L      // ms
LETTER_GAP = 400L      // ms
WORD_GAP = 800L        // ms
```

---

### 5.4 LightDetector

**Propósito:** Análisis de frames de cámara para detectar señales Morse

**Interfaz Pública:**
```kotlin
class LightDetector {
    fun analyze(imageProxy: ImageProxy)
    val decodedMessage: Flow<String>
    val detectionState: Flow<DetectionState>
}
```

**Pipeline de Procesamiento:**
```
ImageProxy (30 fps)
    ↓
[1] Calcular Luminosidad Promedio
    brightness = Σ(Y-plane pixels) / totalPixels
    ↓
[2] Filtro Suavizado Exponencial
    smoothed = α × current + (1-α) × previous
    (α = 0.7)
    ↓
[3] Umbral Dinámico Adaptativo
    threshold = min + (max - min) × 0.4
    ↓
[4] Detectar Transiciones
    ON: brightness > threshold
    OFF: brightness < threshold
    ↓
[5] Medir Duración de Pulsos
    timestamp_on → timestamp_off = pulse_duration
    ↓
[6] Clasificar Pulsos
    80-200ms   → DOT
    200-500ms  → DASH
    >500ms     → GAP
    ↓
[7] Detectar Marcadores
    START: 300-300-900ms
    END: 600-100ms
    ↓
[8] Decodificar a Texto
    MorseDecoder.decode(symbols)
    ↓
Mensaje Decodificado
```

**Optimizaciones:**
- Procesamiento solo de Y-plane (luminosidad)
- Buffer circular de 5 segundos
- Early termination si no hay variación

---

### 5.5 BLEMeshController

**Propósito:** Red mesh Bluetooth LE para comunicación peer-to-peer

**Interfaz Pública:**
```kotlin
class BLEMeshController(private val context: Context) {
    val peers: StateFlow<List<MeshPeer>>
    val messages: StateFlow<List<ChatMessage>>
    val isAdvertising: StateFlow<Boolean>
    val isScanning: StateFlow<Boolean>
    
    fun startAdvertising(callsign: String)
    fun stopAdvertising()
    fun startScanning()
    fun stopScanning()
    fun sendMessage(peerId: String, message: String)
}
```

**Arquitectura Bluetooth LE:**
```
┌──────────────────────────────────────────┐
│          Advertising (Server)             │
│  ┌────────────────────────────────────┐  │
│  │  Service UUID: 0000BEEF-...        │  │
│  │  ┌──────────────────────────────┐  │  │
│  │  │ Service Data: "CALLSIGN"     │  │  │
│  │  └──────────────────────────────┘  │  │
│  └────────────────────────────────────┘  │
│  ┌────────────────────────────────────┐  │
│  │  GATT Server                       │  │
│  │  ┌──────────────────────────────┐  │  │
│  │  │ Chat Service: 0000C4A7-...   │  │  │
│  │  │  └─ Message Char: 00004D51   │  │  │
│  │  │     (WRITE permission)       │  │  │
│  │  └──────────────────────────────┘  │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
              ▲
              │ BLE Connection
              ▼
┌──────────────────────────────────────────┐
│           Scanning (Client)               │
│  ┌────────────────────────────────────┐  │
│  │  Scan Filter:                      │  │
│  │  • Service UUID: 0000BEEF-...      │  │
│  │  • Parse Service Data → Callsign   │  │
│  └────────────────────────────────────┘  │
│  ┌────────────────────────────────────┐  │
│  │  GATT Client (to send messages)   │  │
│  │  1. Connect to peer                │  │
│  │  2. Discover services              │  │
│  │  3. Find Message Characteristic    │  │
│  │  4. Write: "CALLSIGN:MESSAGE"      │  │
│  │  5. Disconnect                     │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

**Flujo de Mensaje:**
```
Usuario A                    Usuario B
   │                            │
   ├──Scan→ Detecta B           │
   │                            │
   ├──Connect GATT──────────────>│
   │                            │
   ├──Discover Services─────────>│
   │                            │
   ├──Write "A:Hola"────────────>│
   │                        [GATT Server]
   │                        onCharWrite()
   │                        ParseMessage()
   │                        ShowInChat()
   │                            │
   │<─────Disconnect─────────────┤
   │                            │
```

---

### 5.6 VibrationOscilloscope

**Propósito:** Detección de patrones Morse mediante acelerómetro

**Interfaz Pública:**
```kotlin
class VibrationOscilloscope {
    fun startDetection()
    fun stopDetection()
    val magnitude: Flow<Float>
    val decodedMessage: Flow<String>
}
```

**Pipeline de Procesamiento:**
```
Accelerometer (200 Hz)
    ↓
Leer x, y, z
    ↓
[1] Filtro Pasa-Alto
    // Eliminar componente de gravedad
    x_filtered = x - gravity_x
    y_filtered = y - gravity_y
    z_filtered = z - gravity_z
    ↓
[2] Calcular Magnitud Vectorial
    magnitude = √(x² + y² + z²)
    ↓
[3] Suavizado Exponencial
    smoothed = α × current + (1-α) × previous
    (α = 0.6)
    ↓
[4] Umbral Dinámico
    threshold = mean + (stddev × 2.5)
    ↓
[5] Detectar Pulsos
    ON: magnitude > threshold
    OFF: magnitude < threshold
    ↓
[6] Clasificar como DOT/DASH/GAP
    ↓
[7] Decodificar a Texto
    ↓
Mensaje Final
```

**Calibración:**
- Primeros 2 segundos: calibración automática
- Calcula baseline de ruido ambiental
- Ajusta umbral dinámicamente

---

### 5.7 UserPreferences

**Propósito:** Persistencia de configuración del usuario

**Interfaz Pública:**
```kotlin
class UserPreferences(context: Context) {
    val callsign: Flow<String>
    
    suspend fun setCallsign(value: String)
}
```

**Tecnología:**
- SharedPreferences (simple y rápido)
- Keys con namespace: `"beaconchat_callsign"`

**Validación:**
- Callsign: 3-10 caracteres alfanuméricos
- Default: "UNKNOWN"

---

## 6. Patrones de Diseño

### 6.1 Patrones Arquitectónicos

#### 6.1.1 MVVM (Model-View-ViewModel)
**Aplicación:**
- **View:** Composables (Screens)
- **ViewModel:** MainActivity (gestión de estado)
- **Model:** Controllers + Data classes

**Beneficios:**
- Separación UI ↔ Lógica
- Testabilidad mejorada
- Estado reactivo con Flow

---

#### 6.1.2 Repository Pattern
**Aplicación:**
- UserPreferences como repository de configuración
- MeshController como repository de peers/messages

**Beneficios:**
- Abstracción de fuente de datos
- Fácil cambio de implementación

---

#### 6.1.3 Observer Pattern
**Aplicación:**
- StateFlow para estado reactivo
- Flow para streams de datos

**Ejemplo:**
```kotlin
val peers: StateFlow<List<MeshPeer>>
// UI observa cambios automáticamente
```

---

### 6.2 Patrones de Diseño

#### 6.2.1 Strategy Pattern
**Aplicación:** Selección de método de transmisión

```kotlin
interface TransmissionStrategy {
    suspend fun transmit(timings: List<Long>)
}

class FlashlightStrategy : TransmissionStrategy
class VibrationStrategy : TransmissionStrategy
class SoundStrategy : TransmissionStrategy
```

---

#### 6.2.2 Singleton Pattern
**Aplicación:** Controladores de hardware

```kotlin
// Implícito en Kotlin object
object MorseAlphabet {
    // Único acceso global a tablas Morse
}
```

---

#### 6.2.3 Builder Pattern
**Aplicación:** Construcción de mensajes complejos

```kotlin
data class ChatMessage(
    val senderId: String,
    val senderName: String,
    val recipientId: String = "",
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean = false
)
```

---

#### 6.2.4 State Pattern
**Aplicación:** Estados de detección

```kotlin
sealed class DetectionState {
    object Idle : DetectionState()
    object WaitingForStart : DetectionState()
    object Receiving : DetectionState()
    data class Complete(val message: String) : DetectionState()
    data class Error(val reason: String) : DetectionState()
}
```

---

## 7. Flujo de Datos

### 7.1 Flujo de Transmisión Completo

```
┌─────────────┐
│   Usuario   │
│             │
│ "SOS"       │
└──────┬──────┘
       │ Input
       ▼
┌─────────────────────────┐
│  TransmitterScreen      │
│  • Composable           │
│  • State Management     │
└──────┬──────────────────┘
       │ onClick()
       ▼
┌─────────────────────────┐
│  MorseEncoder           │
│  • detectScript()       │
│  • getMorseTable()      │
│  • encode(text)         │
└──────┬──────────────────┘
       │ List<Long> timings
       │ [START, 150, 150, ...]
       ▼
┌─────────────────────────┐
│  FlashlightController   │
│  • transmit(timings)    │
│  • launch coroutine     │
└──────┬──────────────────┘
       │ for each timing
       ▼
┌─────────────────────────┐
│  Camera2 API            │
│  • setTorchMode(true)   │
│  • delay(duration)      │
│  • setTorchMode(false)  │
└─────────────────────────┘
       │ Light Output
       ▼
    💡 LED Flash
```

### 7.2 Flujo de Recepción Completo

```
    📷 Cámara
       │ Frame stream (30 fps)
       ▼
┌─────────────────────────┐
│  CameraX ImageAnalysis  │
│  • analyze(ImageProxy)  │
└──────┬──────────────────┘
       │ ImageProxy
       ▼
┌─────────────────────────┐
│  LightDetector          │
│  • calcBrightness()     │
│  • applyFilter()        │
│  • detectTransitions()  │
│  • measurePulses()      │
└──────┬──────────────────┘
       │ List<Pulse>
       │ [DOT, DOT, DOT, DASH, ...]
       ▼
┌─────────────────────────┐
│  MorseDecoder           │
│  • classify(pulse)      │
│  • buildSymbol()        │
│  • decode()             │
└──────┬──────────────────┘
       │ String message
       │ "SOS"
       ▼
┌─────────────────────────┐
│  ReceiverScreen         │
│  • StateFlow update     │
│  • UI recompose         │
└──────┬──────────────────┘
       │ Display
       ▼
┌─────────────┐
│   Usuario   │
│ Ve "SOS"    │
└─────────────┘
```

### 7.3 Flujo de Red Mesh

```
Usuario A                 BLEMeshController A        BLE Stack         BLEMeshController B        Usuario B
    │                            │                       │                      │                      │
    ├─"Hola"───────────────────>│                       │                      │                      │
    │                            │                       │                      │                      │
    │                      [1] Construir mensaje        │                      │                      │
    │                            │ "A:Hola"              │                      │                      │
    │                            │                       │                      │                      │
    │                      [2] Connect GATT             │                      │                      │
    │                            ├──────────────────────>│                      │                      │
    │                            │                       ├──────────────────────>│                      │
    │                            │                       │              [GATT Server]                   │
    │                            │                       │                      │                      │
    │                      [3] Discover Services        │                      │                      │
    │                            ├──────────────────────>│                      │                      │
    │                            │                       ├──────────────────────>│                      │
    │                            │<──────Services────────┤                      │                      │
    │                            │                       │                      │                      │
    │                      [4] Write Characteristic     │                      │                      │
    │                            ├──"A:Hola"────────────>│                      │                      │
    │                            │                       ├──────────────────────>│                      │
    │                            │                       │              onCharWrite()                   │
    │                            │                       │                      ├─Parse "A:Hola"───────>│
    │                            │                       │                      │                "Hola"│
    │                            │                       │                      │                      │
    │                      [5] Disconnect               │                      │                      │
    │                            ├──────────────────────>│                      │                      │
    │                            │                       │                      │                      │
    │<──"✓ Enviado"──────────────┤                       │                      │                      │
```

---

## 8. Decisiones Arquitectónicas

### 8.1 ADR-001: Uso de Jetpack Compose

**Contexto:**
Necesidad de UI declarativa, moderna y mantenible.

**Decisión:**
Adoptar Jetpack Compose como framework de UI único.

**Razones:**
- UI declarativa reduce boilerplate
- State management integrado
- Material Design 3 nativo
- Rendimiento superior a Views XML
- Futuro de Android UI

**Consecuencias:**
- ✅ Código UI más conciso
- ✅ Animaciones más fáciles
- ✅ Mejor rendimiento
- ⚠️ Curva de aprendizaje inicial
- ⚠️ Requiere API 26+

---

### 8.2 ADR-002: Arquitectura MVVM

**Contexto:**
Separar lógica de UI para mejor testabilidad.

**Decisión:**
Implementar patrón MVVM con MainActivity como ViewModel.

**Razones:**
- Separación clara de responsabilidades
- Estado reactivo con StateFlow
- Lifecycle awareness
- Recomendación oficial de Google

**Consecuencias:**
- ✅ Código más testeable
- ✅ UI reactiva automática
- ✅ Menos bugs de sincronización
- ⚠️ Más clases/archivos

---

### 8.3 ADR-003: Controladores de Hardware Dedicados

**Contexto:**
Gestión compleja de recursos hardware limitados.

**Decisión:**
Crear controladores dedicados para cada hardware (Flash, Vibración, Sonido).

**Razones:**
- Encapsulación de lógica hardware
- Reutilización entre screens
- Cleanup centralizado
- Evita conflictos de recursos

**Consecuencias:**
- ✅ Código reutilizable
- ✅ Gestión de recursos centralizada
- ✅ Fácil testing con mocks
- ⚠️ Más abstracciones

---

### 8.4 ADR-004: Morse Multi-Idioma en Objeto Singleton

**Contexto:**
Soporte para 9 alfabetos diferentes.

**Decisión:**
Centralizar tablas Morse en `MorseAlphabet` object (singleton).

**Razones:**
- Único punto de verdad para tablas
- Detección automática de script
- Fácil extensión a nuevos alfabetos
- Sin duplicación de datos

**Consecuencias:**
- ✅ Mantenimiento simplificado
- ✅ Consistencia garantizada
- ✅ Extensibilidad
- ⚠️ Tamaño en memoria (mínimo)

---

### 8.5 ADR-005: StateFlow para Estado Reactivo

**Contexto:**
Necesidad de UI que reaccione a cambios de estado.

**Decisión:**
Usar StateFlow en lugar de LiveData.

**Razones:**
- Compatible con Kotlin Coroutines
- Mejor integración con Compose
- Type-safe
- Más moderno que LiveData

**Consecuencias:**
- ✅ Código más limpio
- ✅ Menos conversiones
- ✅ Mejor rendimiento
- ⚠️ Requiere entender Flow API

---

### 8.6 ADR-006: Sin Base de Datos Persistente

**Contexto:**
Mensajes y peers no necesitan persistencia a largo plazo.

**Decisión:**
Mantener mensajes/peers solo en memoria (StateFlow).

**Razones:**
- App de emergencia, no chat permanente
- Simplicidad arquitectónica
- Mejor rendimiento
- Privacidad (no almacena comunicaciones)

**Consecuencias:**
- ✅ Arquitectura más simple
- ✅ Sin overhead de DB
- ✅ Mejor privacidad
- ⚠️ Datos se pierden al cerrar app

---

### 8.7 ADR-007: Procesamiento de Imagen en Y-Plane Solo

**Contexto:**
Análisis de frames de cámara para detección de luz.

**Decisión:**
Analizar solo el Y-plane (luminosidad) de imágenes YUV.

**Razones:**
- Reducir carga computacional (1/3 de datos)
- Suficiente para detectar ON/OFF
- Mejor rendimiento en tiempo real
- Batería más eficiente

**Consecuencias:**
- ✅ 3x más rápido
- ✅ Menos consumo CPU
- ✅ Suficiente precisión
- ⚠️ No usa color (innecesario)

---

## 9. Diagrama de Despliegue

### 9.1 Arquitectura de Despliegue

```
┌─────────────────────────────────────────────────────────────┐
│                    Dispositivo Android                       │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Sistema Operativo Android                │  │
│  │                   (API 26-34)                         │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │          Android Runtime (ART)                  │  │  │
│  │  │  ┌───────────────────────────────────────────┐  │  │  │
│  │  │  │       BeaconChat.apk                      │  │  │  │
│  │  │  │  ┌─────────────────────────────────────┐  │  │  │  │
│  │  │  │  │  MainActivity + Composables         │  │  │  │  │
│  │  │  │  └─────────────────────────────────────┘  │  │  │  │
│  │  │  │  ┌─────────────────────────────────────┐  │  │  │  │
│  │  │  │  │  Controllers (Flash/Vib/Sound/BLE)  │  │  │  │  │
│  │  │  │  └─────────────────────────────────────┘  │  │  │  │
│  │  │  │  ┌─────────────────────────────────────┐  │  │  │  │
│  │  │  │  │  Encoders/Decoders                  │  │  │  │  │
│  │  │  │  └─────────────────────────────────────┘  │  │  │  │
│  │  │  └───────────────────────────────────────────┘  │  │  │
│  │  └─────────────────────────────────────────────────┘  │  │
│  │                        │                               │  │
│  │  ┌─────────────────────┼───────────────────────────┐  │  │
│  │  │   Android Framework APIs                        │  │  │
│  │  │  ┌──────────────┐  ┌──────────────┐            │  │  │
│  │  │  │ Camera2      │  │ Bluetooth    │            │  │  │
│  │  │  └──────────────┘  └──────────────┘            │  │  │
│  │  │  ┌──────────────┐  ┌──────────────┐            │  │  │
│  │  │  │ Vibrator     │  │ SensorManager│            │  │  │
│  │  │  └──────────────┘  └──────────────┘            │  │  │
│  │  │  ┌──────────────┐  ┌──────────────┐            │  │  │
│  │  │  │ AudioTrack   │  │ SharedPrefs  │            │  │  │
│  │  │  └──────────────┘  └──────────────┘            │  │  │
│  │  └─────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Hardware Físico                          │  │
│  │  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐     │  │
│  │  │ LED    │  │Vibrador│  │ Cámara │  │ BT Radio│    │  │
│  │  │ Flash  │  │ Motor  │  │        │  │         │    │  │
│  │  └────────┘  └────────┘  └────────┘  └────────┘     │  │
│  │  ┌────────┐  ┌────────┐                             │  │
│  │  │Altavoz │  │Aceler. │                             │  │
│  │  └────────┘  └────────┘                             │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │          Almacenamiento Local                         │  │
│  │  /data/data/com.nicobutter.beaconchat/               │  │
│  │    ├── shared_prefs/                                 │  │
│  │    │   └── beaconchat_preferences.xml                │  │
│  │    └── cache/                                        │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 9.2 Distribución de APK

```
Desarrollo                Build                 Distribución
┌─────────┐            ┌─────────┐            ┌─────────┐
│         │            │         │            │         │
│ Código  │───build───>│  APK    │───copy────>│releases/│
│ Fuente  │            │ Debug   │            │ *.apk   │
│         │            │         │            │         │
└─────────┘            └─────────┘            └─────────┘
     │                      │                      │
     │                      │                      │
     ▼                      ▼                      ▼
  Kotlin              assembleDebug          BeaconChat_
  Sources             gradle task            YYYYMMDD_
   + XML                                     HHMMSS.apk
Resources                                          +
                                            BeaconChat_
                                            latest.apk
```

### 9.3 Instalación en Dispositivo

```
Proceso de Instalación:

1. Conexión USB
   ┌──────────┐         USB          ┌──────────┐
   │   PC     │◄──────────────────────┤ Android  │
   │          │                       │          │
   └──────────┘                       └──────────┘

2. ADB (Android Debug Bridge)
   $ adb install releases/BeaconChat_latest.apk

3. Instalación
   APK → Package Manager → Instalación → /data/app/

4. Permisos en Runtime
   App solicita:
   - CAMERA
   - VIBRATE
   - BLUETOOTH_SCAN/ADVERTISE/CONNECT (API 31+)

5. Lanzamiento
   $ adb shell am start -n com.nicobutter.beaconchat/.MainActivity
```

---

## Apéndice A: Diagrama de Clases Principales

```
┌─────────────────────────┐
│    MainActivity         │
│─────────────────────────│
│ - flashlightController  │
│ - vibrationController   │
│ - soundController       │
│ - meshController        │
│ - userPreferences       │
│─────────────────────────│
│ + onCreate()            │
│ + onPause()             │
│ + onDestroy()           │
│ - cleanupControllers()  │
└─────────────────────────┘
         │ uses
         ▼
┌─────────────────────────┐      ┌─────────────────────────┐
│ FlashlightController    │      │  MorseEncoder           │
│─────────────────────────│      │─────────────────────────│
│ - cameraManager         │      │ - morseAlphabet         │
│ - cameraId              │      │─────────────────────────│
│─────────────────────────│      │ + encode(text): List    │
│ + transmit(timings)     │      │ + getAlphabetName()     │
│ + turnOn()              │      │ + detectScript()        │
│ + turnOff()             │      └─────────────────────────┘
│ + cleanup()             │                 │ uses
└─────────────────────────┘                 ▼
         │ uses                  ┌─────────────────────────┐
         ▼                       │   MorseAlphabet         │
┌─────────────────────────┐      │─────────────────────────│
│   Camera2 API           │      │ + LATIN_MORSE           │
└─────────────────────────┘      │ + CYRILLIC_MORSE        │
                                 │ + GREEK_MORSE           │
┌─────────────────────────┐      │ + HEBREW_MORSE          │
│   LightDetector         │      │ + ARABIC_MORSE          │
│─────────────────────────│      │ + JAPANESE_MORSE        │
│ - brightness: Float     │      │ + KOREAN_MORSE          │
│ - threshold: Float      │      │ + THAI_MORSE            │
│ - pulses: List          │      │ + PERSIAN_MORSE         │
│─────────────────────────│      │─────────────────────────│
│ + analyze(ImageProxy)   │      │ + getForLocale()        │
│ - calcBrightness()      │      │ + detectScript()        │
│ - applyFilter()         │      └─────────────────────────┘
│ - detectTransitions()   │
└─────────────────────────┘
         │ uses
         ▼
┌─────────────────────────┐
│   MorseDecoder          │
│─────────────────────────│
│ - symbols: List         │
│─────────────────────────│
│ + decode(pulses): String│
│ + classify(duration)    │
└─────────────────────────┘
```

---

## Apéndice B: Tecnologías y Versiones

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Kotlin** | 1.8.22 | Lenguaje principal |
| **Android Gradle Plugin** | 8.13.1 | Build system |
| **Jetpack Compose** | BOM 2023.06.01 | UI Framework |
| **Compose UI** | - | UI Components |
| **Material 3** | - | Design system |
| **CameraX** | Latest | Abstracción cámara |
| **Lifecycle Runtime** | 2.7.0 | Lifecycle awareness |
| **Activity Compose** | 1.8.2 | Integration Activity-Compose |
| **Core KTX** | 1.10.1 | Kotlin extensions |
| **Coroutines** | Latest | Async operations |
| **StateFlow** | Latest | Reactive state |

---

## Apéndice C: Métricas de Calidad

### Complejidad Ciclomática
| Componente | Complejidad | Evaluación |
|------------|-------------|------------|
| MorseEncoder.encode() | 8 | ✅ Aceptable |
| LightDetector.analyze() | 12 | ⚠️ Considerar refactor |
| BLEMeshController | 15 | ⚠️ Complejo pero necesario |
| MainActivity | 6 | ✅ Buena |

### Acoplamiento
- **Acoplamiento eferente (Ce):** Bajo (4-6 dependencias promedio)
- **Acoplamiento aferente (Ca):** Medio (2-3 dependientes promedio)
- **Inestabilidad (I = Ce/(Ce+Ca)):** 0.6-0.7 (balance adecuado)

### Cohesión
- **LCOM (Lack of Cohesion):** Bajo en todos los módulos
- Cada clase tiene responsabilidad única y bien definida

---

## Historial de Revisiones

| Versión | Fecha | Autor | Cambios |
|---------|-------|-------|---------|
| 1.0 | 2025-12-06 | NicoButter | Versión inicial del diseño arquitectónico |

---

## Aprobaciones

| Rol | Nombre | Firma | Fecha |
|-----|--------|-------|-------|
| Arquitecto de Software | NicoButter | | 2025-12-06 |
| Revisor Técnico | | | |
| Líder de Desarrollo | | | |

---

**Documento Confidencial - Proyecto BeaconChat**  
**Diseño de Arquitectura de Software v1.0**  
**6 de diciembre de 2025**
