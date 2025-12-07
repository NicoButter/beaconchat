# Especificación de Requerimientos de Software (ERS)
## BeaconChat - Sistema de Comunicación de Emergencia

**Versión:** 1.0  
**Fecha:** 6 de diciembre de 2025  
**Autor:** NicoButter  
**Estado:** Aprobado

---

## Tabla de Contenidos

1. [Introducción](#1-introducción)
2. [Descripción General](#2-descripción-general)
3. [Requerimientos Funcionales](#3-requerimientos-funcionales)
4. [Requerimientos No Funcionales](#4-requerimientos-no-funcionales)
5. [Interfaces Externas](#5-interfaces-externas)
6. [Restricciones del Sistema](#6-restricciones-del-sistema)
7. [Atributos de Calidad](#7-atributos-de-calidad)
8. [Casos de Uso](#8-casos-de-uso)

---

## 1. Introducción

### 1.1 Propósito
Este documento especifica los requerimientos de software para BeaconChat, una aplicación móvil Android diseñada para comunicación de emergencia cuando no hay infraestructura de telecomunicaciones disponible (red celular, WiFi, Internet).

### 1.2 Alcance
BeaconChat es un sistema de comunicación alternativo que utiliza:
- Luz visible (linterna LED)
- Vibración háptica
- Sonido ultrasónico
- Bluetooth Low Energy (BLE)
- Códigos QR

El sistema permite transmitir y recibir mensajes de texto codificados en múltiples alfabetos Morse internacionales, funcionando completamente offline.

### 1.3 Definiciones, Acrónimos y Abreviaciones

| Término | Definición |
|---------|------------|
| **VLC** | Visible Light Communication - Comunicación por luz visible |
| **BLE** | Bluetooth Low Energy |
| **GATT** | Generic Attribute Profile (Bluetooth) |
| **Morse** | Sistema de codificación de caracteres mediante pulsos |
| **RSSI** | Received Signal Strength Indicator |
| **ITU-R M.1677** | Estándar internacional para sistemas de emergencia |
| **IEEE 802.15.7** | Estándar para comunicación por luz visible |
| **Callsign** | Identificador único del usuario |
| **Mesh** | Red descentralizada peer-to-peer |

### 1.4 Referencias
- IEEE 802.15.7 - Visible Light Communication Standard
- ITU-R M.1677 - Emergency Radiobeacon Systems
- NIST - Rescue Robotics Specifications
- Android SDK Documentation (API 26+)
- Material Design 3 Guidelines

### 1.5 Visión General
Este documento está organizado siguiendo el estándar IEEE 830-1998 para especificación de requerimientos de software, describiendo requisitos funcionales, no funcionales, interfaces, restricciones y atributos de calidad del sistema.

---

## 2. Descripción General

### 2.1 Perspectiva del Producto
BeaconChat es una aplicación móvil standalone que opera sin dependencias de infraestructura externa. Utiliza únicamente el hardware del dispositivo Android (LED, acelerómetro, motor háptico, radio Bluetooth, cámara).

### 2.2 Funciones del Producto

#### 2.2.1 Funciones Principales
1. **Transmisión de Mensajes**
   - Codificación Morse multi-idioma (9 alfabetos)
   - Transmisión por luz LED (VLC)
   - Transmisión por vibración
   - Transmisión por sonido ultrasónico
   - Generación de códigos QR

2. **Recepción de Mensajes**
   - Detección óptica mediante cámara (30 fps)
   - Detección de vibración mediante acelerómetro (200 Hz)
   - Escaneo de códigos QR
   - Decodificación Morse automática

3. **Red Mesh Bluetooth**
   - Descubrimiento de pares cercanos
   - Chat peer-to-peer
   - Broadcast de mensajes
   - Visualización de estado de red (RSSI, última vez visto)

4. **Emergencias**
   - Transmisión SOS de un toque
   - Transmisión HELP de un toque
   - Transmisión multicanal simultánea
   - Mensajes predefinidos de emergencia

### 2.3 Características de Usuarios

| Tipo de Usuario | Características | Nivel Técnico |
|-----------------|-----------------|---------------|
| **Víctima de emergencia** | Persona en situación de peligro, estrés elevado | Mínimo |
| **Rescatista profesional** | Personal capacitado en emergencias | Medio |
| **Radioaficionado** | Usuario con conocimientos técnicos | Alto |
| **Usuario general** | Persona preparada para emergencias | Bajo-Medio |

### 2.4 Restricciones

#### 2.4.1 Restricciones Regulatorias
- Cumplimiento con normas de accesibilidad Android
- Permisos de privacidad (BLUETOOTH, CAMERA, VIBRATE)
- Sin recolección de datos personales

#### 2.4.2 Restricciones de Hardware
- Requiere Android 8.0 (API 26) o superior
- Linterna LED funcional
- Cámara trasera con capacidad de análisis de frames
- Motor de vibración (opcional)
- Bluetooth 4.0+ con BLE (opcional)

#### 2.4.3 Restricciones de Interfaz
- Diseño Material Design 3
- Navegación simple para uso bajo estrés
- Alto contraste para visibilidad en condiciones adversas

### 2.5 Suposiciones y Dependencias

#### 2.5.1 Suposiciones
- El dispositivo tiene batería suficiente
- El usuario puede ver la pantalla del dispositivo
- Existe línea de vista para transmisión óptica (cuando aplica)
- El receptor conoce el protocolo Morse o tiene BeaconChat instalado

#### 2.5.2 Dependencias
- Android SDK 34
- Kotlin 1.8.22+
- Jetpack Compose
- CameraX API
- Bluetooth LE API

---

## 3. Requerimientos Funcionales

### RF-001: Transmisión de Mensajes por Luz

**Prioridad:** Alta  
**Tipo:** Funcional

**Descripción:** El sistema debe permitir transmitir mensajes de texto codificados en Morse mediante la linterna LED del dispositivo.

**Criterios de Aceptación:**
- AC-001.1: El sistema codifica texto a Morse en menos de 100ms
- AC-001.2: La linterna LED parpadea según timing IEEE 802.15.7:
  - DOT: 150ms ± 10ms
  - DASH: 400ms ± 10ms
  - GAP inter-símbolo: 150ms ± 10ms
  - GAP inter-letra: 400ms ± 10ms
  - GAP inter-palabra: 800ms ± 10ms
- AC-001.3: Incluye marcador START (300-300-900ms)
- AC-001.4: Incluye marcador END (600-100ms)
- AC-001.5: Soporta transmisión continua (loop)
- AC-001.6: Soporta transmisión única

**Entrada:** Texto alfanumérico (hasta 200 caracteres)  
**Salida:** Secuencia de pulsos de luz LED  
**Precondiciones:** Dispositivo con linterna LED funcional  
**Postcondiciones:** Linterna apagada al finalizar

---

### RF-002: Recepción de Mensajes por Cámara

**Prioridad:** Alta  
**Tipo:** Funcional

**Descripción:** El sistema debe detectar y decodificar señales Morse transmitidas por luz usando la cámara del dispositivo.

**Criterios de Aceptación:**
- AC-002.1: Analiza frames de cámara a 30 fps mínimo
- AC-002.2: Aplica filtro suavizado exponencial (α = 0.7)
- AC-002.3: Usa umbral dinámico adaptativo: min + (max - min) × 0.4
- AC-002.4: Detecta automáticamente marcador START
- AC-002.5: Detecta automáticamente marcador END
- AC-002.6: Decodifica Morse a texto con tasa de error < 5%
- AC-002.7: Visualiza intensidad lumínica en tiempo real
- AC-002.8: Muestra mensaje decodificado al finalizar

**Entrada:** Stream de video de cámara trasera  
**Salida:** Texto decodificado  
**Precondiciones:** Permisos de cámara otorgados  
**Postcondiciones:** Cámara liberada al salir de pantalla

---

### RF-003: Transmisión por Vibración

**Prioridad:** Media  
**Tipo:** Funcional

**Descripción:** El sistema debe transmitir mensajes codificados en Morse mediante el motor de vibración del dispositivo.

**Criterios de Aceptación:**
- AC-003.1: Usa mismo timing que transmisión por luz
- AC-003.2: Amplitud de vibración constante
- AC-003.3: Soporta transmisión continua y única
- AC-003.4: No interfiere con otras funciones del sistema

**Entrada:** Texto alfanumérico  
**Salida:** Secuencia de pulsos de vibración  
**Precondiciones:** Permiso VIBRATE otorgado  
**Postcondiciones:** Vibración detenida al finalizar

---

### RF-004: Detección de Vibración

**Prioridad:** Media  
**Tipo:** Funcional

**Descripción:** El sistema debe detectar patrones Morse mediante el acelerómetro del dispositivo.

**Criterios de Aceptación:**
- AC-004.1: Muestrea acelerómetro a ~200 Hz
- AC-004.2: Aplica filtro pasa-alto para eliminar gravedad
- AC-004.3: Calcula magnitud vectorial de aceleración
- AC-004.4: Detecta pulsos con umbral dinámico
- AC-004.5: Decodifica patrones Morse
- AC-004.6: Visualiza magnitud de vibración en tiempo real

**Entrada:** Datos del acelerómetro  
**Salida:** Texto decodificado  
**Precondiciones:** Sensor de acelerómetro disponible  
**Postcondiciones:** Sensor liberado al salir

---

### RF-005: Transmisión por Sonido

**Prioridad:** Baja  
**Tipo:** Funcional

**Descripción:** El sistema debe transmitir mensajes mediante tonos de audio.

**Criterios de Aceptación:**
- AC-005.1: Genera tonos a frecuencia configurable (2000-5000 Hz)
- AC-005.2: Usa mismo timing que transmisión por luz
- AC-005.3: Volumen ajustable

**Entrada:** Texto alfanumérico  
**Salida:** Secuencia de tonos de audio  
**Precondiciones:** Hardware de audio funcional  
**Postcondiciones:** Audio detenido al finalizar

---

### RF-006: Red Mesh Bluetooth

**Prioridad:** Media  
**Tipo:** Funcional

**Descripción:** El sistema debe descubrir dispositivos BeaconChat cercanos y permitir comunicación peer-to-peer.

**Criterios de Aceptación:**
- AC-006.1: Advertising BLE con UUID personalizado (0000BEEF...)
- AC-006.2: Incluye callsign en datos de advertising
- AC-006.3: Escanea dispositivos con filtro de servicio
- AC-006.4: Muestra RSSI de cada dispositivo
- AC-006.5: Muestra tiempo desde última vez visto
- AC-006.6: Permite envío de mensajes de texto a peer específico
- AC-006.7: Implementa GATT server para recibir mensajes
- AC-006.8: Almacena historial de mensajes por sesión

**Entrada:** Callsign del usuario  
**Salida:** Lista de pares descubiertos, mensajes enviados/recibidos  
**Precondiciones:** Permisos Bluetooth otorgados, Bluetooth habilitado  
**Postcondiciones:** Advertising y scanning detenidos al salir

---

### RF-007: Generación de Códigos QR

**Prioridad:** Baja  
**Tipo:** Funcional

**Descripción:** El sistema debe generar códigos QR con el mensaje y callsign del usuario.

**Criterios de Aceptación:**
- AC-007.1: Genera QR en formato estándar
- AC-007.2: Incluye mensaje y callsign
- AC-007.3: QR es escaneable por apps estándar
- AC-007.4: Tamaño mínimo 256x256 píxeles

**Entrada:** Mensaje de texto, callsign  
**Salida:** Imagen QR code  
**Precondiciones:** Ninguna  
**Postcondiciones:** Ninguna

---

### RF-008: Escaneo de Códigos QR

**Prioridad:** Baja  
**Tipo:** Funcional

**Descripción:** El sistema debe escanear y decodificar códigos QR generados por BeaconChat.

**Criterios de Aceptación:**
- AC-008.1: Detecta códigos QR en tiempo real
- AC-008.2: Decodifica mensaje y callsign
- AC-008.3: Muestra información del remitente

**Entrada:** Stream de cámara  
**Salida:** Mensaje y callsign decodificados  
**Precondiciones:** Permisos de cámara otorgados  
**Postcondiciones:** Cámara liberada al salir

---

### RF-009: Soporte Multi-Idioma Morse

**Prioridad:** Alta  
**Tipo:** Funcional

**Descripción:** El sistema debe soportar codificación/decodificación Morse en múltiples alfabetos.

**Criterios de Aceptación:**
- AC-009.1: Soporta alfabeto Latino (A-Z, 0-9, Ñ, puntuación)
- AC-009.2: Soporta Cirílico (А-Я)
- AC-009.3: Soporta Griego (Α-Ω)
- AC-009.4: Soporta Hebreo (א-ת)
- AC-009.5: Soporta Árabe (ا-ي)
- AC-009.6: Soporta Japonés Wabun (あ-ん)
- AC-009.7: Soporta Coreano Hangul (ㄱ-ㅎ, ㅏ-ㅣ)
- AC-009.8: Soporta Tailandés (ก-ฮ)
- AC-009.9: Soporta Persa (ا-ی)
- AC-009.10: Detecta automáticamente alfabeto según Locale del sistema
- AC-009.11: Muestra bandera visual del alfabeto seleccionado

**Entrada:** Texto en cualquier alfabeto soportado  
**Salida:** Secuencia Morse correspondiente  
**Precondiciones:** Ninguna  
**Postcondiciones:** Ninguna

---

### RF-010: Transmisión de Emergencia

**Prioridad:** Crítica  
**Tipo:** Funcional

**Descripción:** El sistema debe proporcionar botones de emergencia de un solo toque para SOS y HELP.

**Criterios de Aceptación:**
- AC-010.1: Botón SOS transmite "SOS" en Morse
- AC-010.2: Botón HELP transmite "HELP" en Morse
- AC-010.3: Transmisión por todos los canales simultáneamente (luz + vibración + sonido)
- AC-010.4: Transmisión continua hasta cancelación manual
- AC-010.5: Pantalla fullscreen con feedback visual
- AC-010.6: Muestra tiempo transcurrido
- AC-010.7: Muestra contador de transmisiones
- AC-010.8: Botón de cancelación siempre visible

**Entrada:** Un toque en botón de emergencia  
**Salida:** Transmisión multicanal continua  
**Precondiciones:** Ninguna  
**Postcondiciones:** Todos los canales detenidos al cancelar

---

### RF-011: Configuración de Usuario

**Prioridad:** Media  
**Tipo:** Funcional

**Descripción:** El sistema debe permitir configurar preferencias del usuario.

**Criterios de Aceptación:**
- AC-011.1: Permite configurar callsign (identificador único)
- AC-011.2: Callsign persistente entre sesiones
- AC-011.3: Validación de callsign (alfanumérico, 3-10 caracteres)

**Entrada:** Callsign del usuario  
**Salida:** Configuración almacenada  
**Precondiciones:** Ninguna  
**Postcondiciones:** Configuración guardada en SharedPreferences

---

### RF-012: Radar de Dispositivos

**Prioridad:** Media  
**Tipo:** Funcional

**Descripción:** El sistema debe visualizar dispositivos BeaconChat cercanos en interfaz tipo radar.

**Criterios de Aceptación:**
- AC-012.1: Muestra dispositivos detectados por BLE
- AC-012.2: Visualiza callsign de cada dispositivo
- AC-012.3: Muestra calidad de señal (Excellent/Good/Fair/Weak)
- AC-012.4: Muestra tiempo desde última detección
- AC-012.5: Actualización en tiempo real
- AC-012.6: Animación de barrido tipo radar

**Entrada:** Datos de escaneo BLE  
**Salida:** Visualización de red mesh  
**Precondiciones:** Bluetooth habilitado  
**Postcondiciones:** Ninguna

---

### RF-013: Osciloscopio Óptico

**Prioridad:** Media  
**Tipo:** Funcional

**Descripción:** El sistema debe visualizar intensidad lumínica en tiempo real durante recepción.

**Criterios de Aceptación:**
- AC-013.1: Gráfico en tiempo real de intensidad
- AC-013.2: Visualización de símbolos detectados (DOT/DASH/GAP)
- AC-013.3: Indicador de estado (esperando START/recibiendo/mensaje completo)
- AC-013.4: Tasa de refresco mínima 10 fps

**Entrada:** Stream de cámara  
**Salida:** Gráfico de intensidad y símbolos  
**Precondiciones:** Cámara activa  
**Postcondiciones:** Ninguna

---

### RF-014: Osciloscopio de Vibración

**Prioridad:** Media  
**Tipo:** Funcional

**Descripción:** El sistema debe visualizar magnitud de vibración en tiempo real durante detección.

**Criterios de Aceptación:**
- AC-014.1: Gráfico en tiempo real de magnitud
- AC-014.2: Visualización de símbolos detectados
- AC-014.3: Indicador de umbral dinámico
- AC-014.4: Tasa de refresco mínima 20 fps

**Entrada:** Datos de acelerómetro  
**Salida:** Gráfico de magnitud  
**Precondiciones:** Acelerómetro activo  
**Postcondiciones:** Ninguna

---

## 4. Requerimientos No Funcionales

### RNF-001: Rendimiento

**RNF-001.1 Latencia de Codificación**
- El sistema debe codificar mensajes a Morse en menos de 100ms para textos de hasta 200 caracteres

**RNF-001.2 Precisión de Timing**
- Los pulsos de luz deben tener precisión de ±10ms en su duración

**RNF-001.3 Frame Rate**
- La cámara debe capturar mínimo 30 fps durante recepción óptica

**RNF-001.4 Sampling Rate**
- El acelerómetro debe muestrearse a mínimo 200 Hz durante detección de vibración

**RNF-001.5 Tiempo de Inicio**
- La aplicación debe iniciar en menos de 2 segundos en dispositivos con Android 10+

---

### RNF-002: Confiabilidad

**RNF-002.1 Disponibilidad**
- El sistema debe funcionar 100% offline sin requerir conectividad

**RNF-002.2 Recuperación de Errores**
- El sistema debe manejar excepciones de hardware sin crashear
- Debe mostrar mensajes de error informativos al usuario

**RNF-002.3 Tasa de Error de Decodificación**
- Tasa de error de bits < 5% en condiciones ideales
- Tasa de error < 15% con movimiento de mano moderado

**RNF-002.4 Robustez**
- El sistema debe funcionar en rangos de luz ambiente de 10-100,000 lux
- Debe adaptar umbrales dinámicamente

---

### RNF-003: Usabilidad

**RNF-003.1 Curva de Aprendizaje**
- Usuario nuevo debe poder enviar SOS en menos de 10 segundos
- Interfaz intuitiva sin necesidad de manual

**RNF-003.2 Accesibilidad**
- Cumplimiento con Android Accessibility Guidelines
- Tamaño mínimo de botones: 48dp × 48dp
- Alto contraste en modo emergencia

**RNF-003.3 Feedback Visual**
- Toda acción debe tener feedback visual inmediato (<100ms)
- Estados claramente diferenciables

**RNF-003.4 Idioma**
- Interfaz en español e inglés
- Soporte de 9 alfabetos Morse diferentes

---

### RNF-004: Mantenibilidad

**RNF-004.1 Arquitectura**
- Código organizado en capas (UI, Domain, Data)
- Separación de responsabilidades
- Documentación inline completa (KDoc)

**RNF-004.2 Código**
- Cobertura de documentación: 100% en clases públicas
- Naming conventions: Kotlin style guide
- Máximo 300 líneas por archivo de lógica

**RNF-004.3 Versionado**
- Control de versiones con Git
- Semantic versioning (MAJOR.MINOR.PATCH)

---

### RNF-005: Portabilidad

**RNF-005.1 Compatibilidad de Versiones Android**
- Soporte desde Android 8.0 (API 26) hasta Android 14+ (API 34+)

**RNF-005.2 Dispositivos**
- Teléfonos con pantalla 4.5" - 7"
- Tablets Android (opcional)

**RNF-005.3 Hardware Variado**
- Funciona con cámaras de 720p mínimo
- Adapta a diferentes intensidades de LED
- Opcional: vibración, Bluetooth

---

### RNF-006: Seguridad

**RNF-006.1 Privacidad**
- No recolecta datos personales
- No requiere registro de usuario
- No envía datos a servidores externos

**RNF-006.2 Permisos**
- Solicita solo permisos necesarios (Camera, Bluetooth, Vibrate)
- Explicación clara de uso de cada permiso

**RNF-006.3 Almacenamiento**
- Datos almacenados localmente en SharedPreferences
- No se almacenan mensajes permanentemente

---

### RNF-007: Eficiencia

**RNF-007.1 Consumo de Batería**
- Uso de linterna optimizado (apagado entre símbolos)
- Suspensión de cámara al salir de pantalla
- Wake locks solo cuando es necesario

**RNF-007.2 Uso de Memoria**
- Máximo 100 MB de RAM en uso normal
- Liberación de recursos al cambiar de pantalla

**RNF-007.3 Tamaño de APK**
- APK release < 20 MB

---

## 5. Interfaces Externas

### 5.1 Interfaces de Usuario

#### 5.1.1 Pantalla de Bienvenida
- **Propósito:** Punto de entrada, acceso rápido a emergencias
- **Elementos:**
  - Botón "Transmitir"
  - Botón "Recibir"
  - Botón "SOS" (rojo, prominente)
  - Botón "HELP" (amarillo, prominente)

#### 5.1.2 Pantalla de Transmisión
- **Propósito:** Envío de mensajes personalizados
- **Elementos:**
  - Campo de texto (200 caracteres máx)
  - Selector de método (Luz/Vibración/Sonido)
  - Selector de codificación (Morse/Binario)
  - Indicador de alfabeto Morse activo
  - Botones rápidos (SOS, HELP, OK, AYUDA)
  - Botón "Enviar Once"
  - Botón "Transmitir Continuamente"
  - Indicador de estado de transmisión

#### 5.1.3 Pantalla de Recepción
- **Propósito:** Detección y decodificación de señales
- **Elementos:**
  - Vista de cámara en vivo
  - Botón "Detector de Vibración"
  - Osciloscopio visual
  - Área de mensaje decodificado
  - Indicador de estado (esperando/recibiendo/completo)

#### 5.1.4 Pantalla Mesh/Radar
- **Propósito:** Red peer-to-peer Bluetooth
- **Elementos:**
  - Lista de pares descubiertos
  - Indicador de calidad de señal por peer
  - Botón de escaneo ON/OFF
  - Campo de callsign propio
  - Chat individual con peer seleccionado

#### 5.1.5 Pantalla de Osciloscopio Óptico
- **Propósito:** Visualización de señal lumínica
- **Elementos:**
  - Gráfico de intensidad en tiempo real
  - Símbolos detectados (DOT/DASH/GAP)
  - Mensaje parcialmente decodificado
  - Indicador de umbral

#### 5.1.6 Pantalla de Osciloscopio de Vibración
- **Propósito:** Visualización de señal de vibración
- **Elementos:**
  - Gráfico de magnitud en tiempo real
  - Símbolos detectados
  - Mensaje parcialmente decodificado
  - Umbral dinámico

#### 5.1.7 Pantalla de Configuración
- **Propósito:** Ajustes de usuario
- **Elementos:**
  - Campo de edición de callsign
  - Información de versión
  - Acerca de

#### 5.1.8 Barra de Navegación Inferior
- **Propósito:** Navegación principal
- **Elementos:**
  - 6 botones con íconos y animaciones:
    - Transmit (🔦)
    - Receive (📷)
    - Mesh (📡)
    - Radar (🎯)
    - Scope (📊)
    - Settings (⚙️)
  - Efectos visuales: escala 1.2x, fondo circular, negrita al seleccionar

### 5.2 Interfaces de Hardware

#### 5.2.1 Linterna LED
- **API:** Camera2 API / CameraX
- **Operación:** Encendido/apagado con timing preciso
- **Control:** FlashlightController

#### 5.2.2 Motor de Vibración
- **API:** Vibrator / VibratorManager
- **Operación:** Pulsos con timing preciso
- **Control:** VibrationController

#### 5.2.3 Cámara
- **API:** CameraX ImageAnalysis
- **Operación:** Captura de frames a 30fps, análisis de luminosidad
- **Control:** LightDetector, LightScanner

#### 5.2.4 Acelerómetro
- **API:** SensorManager
- **Operación:** Muestreo a 200Hz, cálculo de magnitud
- **Control:** VibrationOscilloscope

#### 5.2.5 Altavoz
- **API:** AudioTrack
- **Operación:** Generación de tonos sinusoidales
- **Control:** SoundController

#### 5.2.6 Bluetooth LE
- **API:** BluetoothAdapter, BluetoothLeAdvertiser, BluetoothLeScanner, BluetoothGatt
- **Operación:** Advertising, scanning, GATT server/client
- **Control:** BLEMeshController

### 5.3 Interfaces de Software

#### 5.3.1 Sistema Operativo Android
- **Versión mínima:** API 26 (Android 8.0 Oreo)
- **Versión objetivo:** API 34 (Android 14)
- **Permisos requeridos:**
  - `android.permission.CAMERA`
  - `android.permission.VIBRATE`
  - `android.permission.BLUETOOTH` (API < 31)
  - `android.permission.BLUETOOTH_ADMIN` (API < 31)
  - `android.permission.BLUETOOTH_SCAN` (API 31+)
  - `android.permission.BLUETOOTH_ADVERTISE` (API 31+)
  - `android.permission.BLUETOOTH_CONNECT` (API 31+)
  - `android.permission.ACCESS_FINE_LOCATION` (API < 31, para BLE)

#### 5.3.2 Bibliotecas Externas
- **Jetpack Compose:** UI declarativa
- **CameraX:** Abstracción de cámara
- **Kotlin Coroutines:** Concurrencia
- **Material Design 3:** Componentes UI
- **DataStore/SharedPreferences:** Persistencia

### 5.4 Interfaces de Comunicación

#### 5.4.1 Protocolo Óptico (VLC)
- **Estándar:** IEEE 802.15.7 adaptado
- **Formato:** Código Morse con marcadores
- **Timing:**
  - START: 300ms ON, 300ms OFF, 900ms ON
  - DOT: 150ms ON
  - DASH: 400ms ON
  - GAP símbolo: 150ms OFF
  - GAP letra: 400ms OFF
  - GAP palabra: 800ms OFF
  - END: 600ms ON, 100ms OFF

#### 5.4.2 Protocolo Bluetooth (Mesh)
- **Estándar:** Bluetooth 4.0+ LE
- **UUIDs:**
  - Service: `0000BEEF-0000-1000-8000-00805f9b34fb`
  - Chat Service: `0000C4A7-0000-1000-8000-00805f9b34fb`
  - Message Characteristic: `00004D51-0000-1000-8000-00805f9b34fb`
- **Formato de mensaje:** `CALLSIGN:MESSAGE` (UTF-8)
- **MTU:** 512 bytes

---

## 6. Restricciones del Sistema

### 6.1 Restricciones de Diseño

#### 6.1.1 Arquitectura
- **Patrón:** MVVM (Model-View-ViewModel) con Jetpack Compose
- **Lenguaje:** Kotlin 100%
- **UI Framework:** Jetpack Compose (declarativo)

#### 6.1.2 Organización de Código
```
app/src/main/java/com/nicobutter/beaconchat/
├── MainActivity.kt
├── data/
│   └── UserPreferences.kt
├── lightmap/
│   ├── DetectedDevice.kt
│   ├── HeartbeatPattern.kt
│   ├── LightScanner.kt
│   └── OpticalOscilloscope.kt
├── mesh/
│   ├── BLEMeshController.kt
│   ├── ChatMessage.kt
│   └── MeshPeer.kt
├── transceiver/
│   ├── BinaryDecoder.kt
│   ├── BinaryEncoder.kt
│   ├── FlashlightController.kt
│   ├── LightDetector.kt
│   ├── MorseAlphabet.kt
│   ├── MorseDecoder.kt
│   ├── MorseEncoder.kt
│   ├── QRGenerator.kt
│   ├── QRScanner.kt
│   ├── SoundController.kt
│   ├── VibrationController.kt
│   └── VibrationOscilloscope.kt
├── ui/
│   ├── screens/
│   │   ├── EmergencyTransmissionScreen.kt
│   │   ├── LightMapScreen.kt
│   │   ├── MeshScreen.kt
│   │   ├── OscilloscopeScreen.kt
│   │   ├── QRTransmissionScreen.kt
│   │   ├── ReceiverScreen.kt
│   │   ├── SettingsScreen.kt
│   │   ├── TransmitterScreen.kt
│   │   ├── VibrationDetectorScreen.kt
│   │   └── WelcomeScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
```

### 6.2 Restricciones de Implementación

#### 6.2.1 Build System
- **Herramienta:** Gradle 8.13+
- **Plugin Android:** 8.13.1
- **Compilación:** JDK 8 target, JDK 21 para build

#### 6.2.2 Dependencias Principales
```kotlin
// Compose BOM
androidx.compose.bom:2023.06.01

// Core
androidx.core:core-ktx:1.10.1
androidx.lifecycle:lifecycle-runtime-ktx:2.7.0
androidx.activity:activity-compose:1.8.2

// CameraX
androidx.camera:camera-*

// Coroutines
kotlinx.coroutines
```

### 6.3 Restricciones Operacionales

#### 6.3.1 Entorno de Ejecución
- Dispositivo Android físico (no emuladores para funciones críticas)
- Espacio de almacenamiento mínimo: 50 MB
- RAM mínima: 2 GB

#### 6.3.2 Limitaciones
- Alcance óptico: ~100m en oscuridad, ~10m con luz diurna
- Alcance Bluetooth: ~30m en línea de vista
- Mensajes máximos: 200 caracteres
- Duración de transmisión continua: limitada por batería

---

## 7. Atributos de Calidad

### 7.1 Confiabilidad
- **MTBF (Mean Time Between Failures):** > 100 horas de uso continuo
- **Recuperación ante fallos:** < 1 segundo
- **Precisión de decodificación:** > 95% en condiciones ideales

### 7.2 Disponibilidad
- **Tiempo de disponibilidad:** 100% (offline)
- **Degradación gradual:** Funciones opcionales (BLE, vibración) no afectan funciones críticas (luz)

### 7.3 Mantenibilidad
- **Tiempo medio de corrección:** < 2 horas para bugs críticos
- **Modularidad:** Alta (cada controlador es independiente)
- **Documentación:** 100% de APIs públicas documentadas

### 7.4 Portabilidad
- **Independencia de hardware:** Adapta a diferentes sensores/actuadores
- **Compatibilidad hacia atrás:** API 26-34 (8 años de dispositivos)

### 7.5 Eficiencia
- **Tiempo de respuesta:** < 100ms para acciones de usuario
- **Throughput:** 30 caracteres/minuto en Morse estándar
- **Utilización de recursos:** < 100 MB RAM, < 5% CPU en idle

### 7.6 Seguridad
- **Confidencialidad:** No aplicable (transmisión pública)
- **Integridad:** Checksum implícito en estructura Morse
- **Autenticación:** Callsign (no criptográfico)

### 7.7 Usabilidad
- **Curva de aprendizaje:** < 5 minutos para función básica
- **Tasa de error de usuario:** < 10% en uso de emergencia
- **Satisfacción del usuario:** Diseño Material Design 3 moderno

---

## 8. Casos de Uso

### CU-001: Transmisión de SOS de Emergencia

**Actor Principal:** Persona en emergencia  
**Precondiciones:** App instalada y abierta  
**Postcondiciones:** Señal SOS transmitida continuamente

**Flujo Principal:**
1. Usuario abre BeaconChat
2. Usuario presiona botón "SOS" rojo grande
3. Sistema inicia transmisión SOS por todos los canales:
   - Linterna parpadea "· · · — — — · · ·" (SOS en Morse)
   - Motor vibra el mismo patrón
   - Altavoz emite tonos del mismo patrón
4. Pantalla muestra:
   - Animación pulsante roja
   - "TRANSMITIENDO SOS"
   - Tiempo transcurrido
   - Contador de repeticiones
5. Sistema repite transmisión cada 5 segundos
6. Usuario presiona "CANCELAR" para detener
7. Sistema detiene todos los canales

**Flujos Alternativos:**
- **3a.** Si no hay linterna: omite canal de luz
- **3b.** Si no hay vibrador: omite canal de vibración
- **6a.** Batería baja: sistema muestra advertencia pero continúa

**Requerimientos:** RF-010, RNF-003.1

---

### CU-002: Recepción de Mensaje por Luz

**Actor Principal:** Rescatista / Receptor  
**Precondiciones:** Permisos de cámara otorgados  
**Postcondiciones:** Mensaje decodificado y mostrado

**Flujo Principal:**
1. Usuario abre pantalla "Receive"
2. Sistema activa cámara trasera
3. Sistema muestra vista previa de cámara
4. Usuario apunta cámara hacia fuente de luz parpadeante
5. Sistema detecta marcador START
6. Sistema cambia indicador a "RECIBIENDO..."
7. Sistema decodifica símbolos Morse en tiempo real
8. Sistema muestra mensaje parcial mientras decodifica
9. Sistema detecta marcador END
10. Sistema muestra mensaje completo decodificado
11. Sistema reproduce sonido de confirmación

**Flujos Alternativos:**
- **5a.** No detecta START en 30s: muestra "Esperando señal..."
- **7a.** Símbolo no reconocido: muestra "?" en mensaje
- **9a.** Timeout sin END: muestra mensaje parcial con advertencia

**Requerimientos:** RF-002, RF-013

---

### CU-003: Chat Mesh Bluetooth

**Actor Principal:** Usuario con otro dispositivo BeaconChat cercano  
**Precondiciones:** Bluetooth habilitado, permisos otorgados  
**Postcondiciones:** Mensaje enviado/recibido

**Flujo Principal:**
1. Usuario abre pantalla "Mesh"
2. Sistema solicita callsign si no está configurado
3. Usuario ingresa callsign (ej: "RESCUE01")
4. Sistema inicia advertising con callsign
5. Sistema inicia scanning de dispositivos
6. Sistema muestra lista de dispositivos detectados con:
   - Callsign
   - Calidad de señal
   - Tiempo desde última vez visto
7. Usuario selecciona dispositivo de la lista
8. Sistema abre chat con ese dispositivo
9. Usuario escribe mensaje
10. Usuario presiona "Enviar"
11. Sistema conecta vía GATT al dispositivo
12. Sistema envía mensaje en formato "CALLSIGN:MESSAGE"
13. Sistema muestra mensaje en chat con marca "✓"
14. Dispositivo remoto recibe mensaje y responde
15. Sistema muestra mensaje recibido en chat

**Flujos Alternativos:**
- **5a.** No hay dispositivos cercanos: muestra "Escaneando..."
- **11a.** Falla conexión: muestra error "No se pudo enviar"
- **14a.** Dispositivo fuera de alcance: mensaje queda pendiente

**Requerimientos:** RF-006, RF-012

---

### CU-004: Detección de Vibración

**Actor Principal:** Persona enterrada/escondida  
**Precondiciones:** Dispositivo receptor tiene acelerómetro  
**Postcondiciones:** Mensaje decodificado desde vibración

**Flujo Principal:**
1. Usuario abre pantalla "Receive"
2. Usuario presiona "Detector de Vibración"
3. Sistema activa muestreo de acelerómetro a 200Hz
4. Sistema muestra osciloscopio de vibración
5. Usuario presiona dispositivo contra superficie vibrante
6. Sistema detecta pulsos de vibración
7. Sistema calcula magnitud vectorial
8. Sistema aplica filtro y umbral dinámico
9. Sistema clasifica pulsos como DOT/DASH/GAP
10. Sistema decodifica a texto Morse
11. Sistema muestra mensaje decodificado
12. Sistema libera acelerómetro al salir

**Flujos Alternativos:**
- **6a.** Vibración muy débil: ajusta umbral dinámicamente
- **9a.** Patrón no claro: muestra caracteres con "?"

**Requerimientos:** RF-004, RF-014

---

### CU-005: Transmisión en Múltiples Idiomas

**Actor Principal:** Usuario no hispanohablante  
**Precondiciones:** Sistema en locale diferente (ej: árabe, japonés)  
**Postcondiciones:** Mensaje transmitido en alfabeto Morse correcto

**Flujo Principal:**
1. Usuario con sistema en árabe abre BeaconChat
2. Sistema detecta Locale.getDefault() = "ar"
3. Sistema selecciona alfabeto Morse árabe
4. Sistema muestra bandera 🇸🇦 en UI
5. Usuario escribe "مساعدة" (ayuda en árabe)
6. Usuario presiona "Transmitir"
7. Sistema codifica con tabla Morse árabe
8. Sistema transmite señal
9. Receptor con BeaconChat detecta señal
10. Receptor decodifica con tabla árabe
11. Receptor muestra "مساعدة"

**Flujos Alternativos:**
- **2a.** Locale no soportado: usa alfabeto latino por defecto
- **9a.** Receptor en otro idioma: decodifica con su tabla (puede diferir)

**Requerimientos:** RF-009

---

### CU-006: Generación y Escaneo de QR

**Actor Principal:** Usuario sin línea de vista directa  
**Precondiciones:** Permisos de cámara otorgados  
**Postcondiciones:** Mensaje transmitido vía código QR

**Flujo Principal:**
1. Usuario A abre pantalla "Transmit"
2. Usuario A escribe mensaje "Ayuda en edificio 5"
3. Usuario A selecciona método "QR Code"
4. Sistema genera código QR con:
   - Callsign de A
   - Mensaje
5. Sistema muestra QR en pantalla
6. Usuario A acerca dispositivo a ventana
7. Usuario B apunta su BeaconChat al QR
8. Sistema B activa scanner QR
9. Sistema B detecta y decodifica QR
10. Sistema B muestra:
    - "Mensaje de: [CALLSIGN_A]"
    - "Ayuda en edificio 5"

**Flujos Alternativos:**
- **9a.** QR dañado/ilegible: muestra "No se pudo leer QR"
- **10a.** QR no de BeaconChat: ignora

**Requerimientos:** RF-007, RF-008

---

## 9. Matriz de Trazabilidad

| Requisito | Caso de Uso | Componente | Prioridad |
|-----------|-------------|------------|-----------|
| RF-001 | CU-001, CU-005 | FlashlightController | Alta |
| RF-002 | CU-002 | LightDetector, MorseDecoder | Alta |
| RF-003 | CU-001 | VibrationController | Media |
| RF-004 | CU-004 | VibrationOscilloscope | Media |
| RF-005 | CU-001 | SoundController | Baja |
| RF-006 | CU-003 | BLEMeshController | Media |
| RF-007 | CU-006 | QRGenerator | Baja |
| RF-008 | CU-006 | QRScanner | Baja |
| RF-009 | CU-005 | MorseAlphabet, MorseEncoder | Alta |
| RF-010 | CU-001 | EmergencyTransmissionScreen | Crítica |
| RF-011 | CU-003 | UserPreferences | Media |
| RF-012 | CU-003 | LightMapScreen | Media |
| RF-013 | CU-002 | OscilloscopeScreen | Media |
| RF-014 | CU-004 | VibrationDetectorScreen | Media |

---

## 10. Apéndices

### Apéndice A: Glosario de Términos Técnicos

- **DOT (punto):** Pulso corto en código Morse (150ms)
- **DASH (raya):** Pulso largo en código Morse (400ms)
- **GAP:** Espacio entre símbolos/letras/palabras
- **VLC:** Visible Light Communication
- **BLE:** Bluetooth Low Energy
- **GATT:** Generic Attribute Profile (protocolo Bluetooth)
- **RSSI:** Received Signal Strength Indicator
- **Callsign:** Identificador único del usuario
- **Mesh:** Topología de red descentralizada
- **Advertising:** Transmisión Bluetooth para descubrimiento
- **Scanning:** Búsqueda de dispositivos Bluetooth
- **Throughput:** Cantidad de datos transmitidos por unidad de tiempo

### Apéndice B: Estándares y Referencias

#### B.1 Estándares IEEE
- **IEEE 802.15.7-2011:** Short-Range Optical Wireless Communications
- **IEEE 802.15.1:** Wireless Personal Area Networks (Bluetooth)

#### B.2 Estándares ITU
- **ITU-R M.1677:** International Morse Code
- **ITU-R M.493:** Digital selective calling system for use in the maritime mobile service

#### B.3 Códigos Morse Internacionales
- ISO/IEC 1073-1:1976 - Alfabeto Morse original
- Wabun Code (日本語モールス符号) - Morse japonés
- SKATS (스카츠) - Morse coreano

### Apéndice C: Tabla de Timing del Protocolo

| Elemento | Duración (ms) | Tolerancia | Frames @30fps |
|----------|--------------|------------|---------------|
| **Símbolos básicos** |
| DOT | 150 | ±10ms | ~5 |
| DASH | 400 | ±10ms | ~12 |
| GAP inter-símbolo | 150 | ±10ms | ~5 |
| GAP inter-letra | 400 | ±10ms | ~12 |
| GAP inter-palabra | 800 | ±20ms | ~24 |
| **Marcadores** |
| START (ON-OFF-ON) | 300-300-900 | ±20ms | ~45 total |
| END (ON-OFF) | 600-100 | ±20ms | ~21 total |

### Apéndice D: Tabla de Alfabetos Morse Soportados

| Alfabeto | Sistema de Escritura | Caracteres | Ejemplo |
|----------|---------------------|------------|---------|
| Latino | A-Z, 0-9, Ñ | 36+ | "SOS" → ··· ··· ··· |
| Cirílico | А-Я | 33 | "СОС" → ··· ··· ··· |
| Griego | Α-Ω | 24 | "ΣΟΣ" → ··· ··· ··· |
| Hebreo | א-ת | 22 | עזרה |
| Árabe | ا-ي | 28 | مساعدة |
| Japonés | あ-ん (Hiragana) | 46 | たすけて |
| Coreano | ㄱ-ㅎ, ㅏ-ㅣ | 40 | 도움 |
| Tailandés | ก-ฮ | 44 | ช่วยด้วย |
| Persa | ا-ی | 32 | کمک |

---

## Historial de Revisiones

| Versión | Fecha | Autor | Cambios |
|---------|-------|-------|---------|
| 1.0 | 2025-12-06 | NicoButter | Versión inicial completa |

---

## Aprobaciones

| Rol | Nombre | Firma | Fecha |
|-----|--------|-------|-------|
| Autor | NicoButter | | 2025-12-06 |
| Revisor Técnico | | | |
| Aprobador | | | |

---

**Documento Confidencial - Proyecto BeaconChat**  
**Especificación de Requerimientos de Software v1.0**  
**6 de diciembre de 2025**
