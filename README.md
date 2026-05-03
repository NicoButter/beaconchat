# BeaconChat 📡

<!-- Language Selection -->
<p align="center">
  <a href="README.md"><strong>🇪🇸 Español</strong></a> •
  <a href="README.en.md">🇬🇧 English</a>
</p>

<!-- LOGO PLACEHOLDER -->
<p align="center">
  <img src="docs/logo.png" alt="BeaconChat Logo" width="200"/>
</p>

> **Una luz. Un celular. Una vida salvada.**

Cuando no hay señal…  
Cuando no hay 3G, 4G, 5G, ni WiFi…  
Cuando estás sepultado, secuestrado, perdido en el mar o atrapado en un derrumbe…  
**Tu celular sigue teniendo una linterna.**  
Y esa linterna puede gritar por vos.

**BeaconChat** convierte tu teléfono en un **faro de emergencia** que:
- Envía mensajes codificados en **código Morse** con la linterna LED
- Usa la **vibración** como canal táctil (para personas sepultadas)
- **Detecta vibraciones** usando el acelerómetro - comunicación sin luz
- Permite que **cualquier otro celular** lo lea con su cámara
- Funciona **100% offline**
- Incluye tu **última ubicación GPS conocida** (Próximamente)

---

## 🆘 Escenarios reales donde BeaconChat salva vidas
- **Terremoto** → persona bajo escombros pone el celular contra la tierra → parpadea "AYUDA"
- **Secuestro** → víctima deja el celular en la ventana → vecino graba y recibe "SECUESTRADO - CALLE 123"
- **Naufragio** → náufrago en balsa → linterna envía "SOCORRO" a kilómetros
- **Apagón masivo** → familia atrapada → vibración + luz = "ESTAMOS VIVOS"

## ⚡ Cómo funciona
1. **Tú** → escribís "AYUDA" → **BeaconChat** → la linterna parpadea en Morse.
2. **Otro celular** → abre **BeaconChat** → apunta la cámara → recibe "AYUDA".

---

## 🚀 Características Técnicas

### 📡 Protocolo de Comunicación Óptica Profesional

BeaconChat implementa un **protocolo robusto de comunicación por luz visible (VLC)** basado en estándares internacionales:

#### 🌟 Fundamentos Técnicos
- **IEEE 802.15.7**: Estándar de comunicación por luz visible
- **NIST**: Especificaciones para robótica de rescate
- **ITU-R M.1677**: Sistemas de radiobalizas de emergencia

#### ⚙️ Características del Protocolo

**Sincronización y Marcadores:**
- ✅ Marcador de inicio (START): Secuencia 300-300-900ms para sincronización automática
- ✅ Marcador de fin (END): Secuencia 600-100ms para confirmar mensaje completo
- ✅ Detección automática de inicio/fin sin intervención manual

**Procesamiento de Señal:**
- ✅ Filtro suavizado exponencial (α = 0.7) elimina jitter de cámara
- ✅ Umbral dinámico adaptativo: min + (max-min) × 0.4
- ✅ Clasificación de pulsos: <80ms ruido, 80-200ms DOT, 200-500ms DASH, >500ms GAP
- ✅ Compatible con cámaras de 30fps estándar

**Robustez:**
- ✅ Funciona con movimiento leve de mano (hasta 10cm/s)
- ✅ Adapta a luz variable (interiores/exteriores)
- ✅ Rechaza ruido de <80ms automáticamente
- ✅ Compatible con cámaras de baja calidad (720p mínimo)

**Timing Optimizado para Cámaras:**
| Símbolo | Duración | Frames @30fps |
|---------|----------|---------------|
| DOT     | 150ms    | ~5 frames     |
| DASH    | 400ms    | ~12 frames    |
| GAP     | 150ms    | ~5 frames     |
| START   | 1500ms   | ~45 frames    |
| END     | 700ms    | ~21 frames    |

📖 **[Ver especificación completa del protocolo →](PROTOCOLO_OPTICO.md)**

---

### 🌍 Soporte Multi-Idioma en Código Morse

**BeaconChat es la primera app de emergencia con soporte nativo para 9 sistemas de escritura diferentes**, permitiendo comunicación en tu idioma materno incluso en crisis.

#### 🗺️ Alfabetos Soportados

| Sistema | Idiomas | Bandera Visual | Caracteres Especiales |
|---------|---------|----------------|----------------------|
| **Latino** | Español, Inglés, Francés, Alemán, Italiano, Portugués | 🇪🇸 🇬🇧 🇫🇷 🇩🇪 🇮🇹 🇵🇹 | A-Z + Ñ, Ä, Ö, Ü, Ç |
| **Cirílico** | Ruso, Ucraniano, Búlgaro, Serbio | 🇷🇺 🇺🇦 🇧🇬 🇷🇸 | А-Я (33 letras) |
| **Griego** | Griego moderno | 🇬🇷 | Α-Ω (24 letras) |
| **Hebreo** | Hebreo moderno | 🇮🇱 | א-ת (22 letras) |
| **Árabe** | Árabe estándar | 🇸🇦 🇪🇬 🇦🇪 | ا-ي (28 letras) |
| **Japonés Wabun** | Japonés kana | 🇯🇵 | あ-ん (46 kana) |
| **Coreano Hangul** | Coreano | 🇰🇷 | ㄱ-ㅎ, ㅏ-ㅣ |
| **Tailandés** | Tailandés | 🇹🇭 | ก-ฮ (44 consonantes) |
| **Persa** | Farsi | 🇮🇷 | ا-ی (32 letras) |

#### 🎯 Detección Automática
BeaconChat detecta automáticamente el idioma del sistema usando `Locale.getDefault()`:
- Sin configuración manual requerida
- Selección inteligente de alfabeto Morse correcto
- Fallback automático a alfabeto latino si el idioma no está soportado

#### 🚩 Identificación Visual con Banderas
La app muestra la **bandera del país/región** correspondiente al alfabeto seleccionado:
- **Español**: 🇪🇸 España, 🇲🇽 México, 🇦🇷 Argentina, 🇨🇴 Colombia, 🇨🇱 Chile, etc.
- **Inglés**: 🇬🇧 Reino Unido, 🇺🇸 Estados Unidos, 🇨🇦 Canadá, 🇦🇺 Australia
- **Árabe**: 🇸🇦 Arabia Saudita, 🇪🇬 Egipto, 🇦🇪 Emiratos, 🇲🇦 Marruecos
- **Francés**: 🇫🇷 Francia, 🇧🇪 Bélgica, 🇨🇭 Suiza, 🇨🇦 Canadá
- Y más...

#### ✨ Beneficios
- **Comunicación natural**: Escribe en tu idioma materno, incluso bajo estrés
- **Alcance global**: Rescatistas de cualquier país pueden leer el mensaje
- **Sin barreras**: No necesitas saber inglés para pedir ayuda
- **Confirmación visual**: La bandera confirma que el alfabeto es correcto

---

### 📤 Transmisor (Transmitter)
Envía mensajes utilizando múltiples canales físicos:
- **🔦 Linterna (Flashlight):** Transmite mensajes en Código Morse usando el flash de la cámara con protocolo profesional VLC
  - Timing optimizado: DOT 150ms, DASH 400ms
  - Marcadores automáticos de inicio/fin
  - Compatible con estándares IEEE 802.15.7
- **🔊 Sonido (Sound):** Genera tonos de audio para transmitir datos (Morse/FSK)
- **📳 Vibración (Vibration):** Usa el motor háptico para mensajes táctiles en Morse
- **🔳 Código QR:** Genera códigos QR dinámicos que contienen el mensaje y el callsign del usuario

### 📥 Receptor (Receiver)
Decodifica señales del entorno con procesamiento avanzado:
- **📷 Detección de Luz (Osciloscopio Óptico):**
  - Análisis de frames de cámara a 30fps
  - Filtro suavizado exponencial (α = 0.7) para eliminar ruido
  - Umbral dinámico adaptativo para condiciones variables
  - Detección automática de marcadores START/END
  - Decodificación Morse multi-idioma (9 alfabetos)
  - Visualización en tiempo real de intensidad lumínica
- **📳 Detección de Vibración (Osciloscopio Táctil):**
  - Captura vía acelerómetro a ~200Hz
  - Filtro pasa-alto elimina gravedad constante
  - Detección de patrones Morse por contacto directo
  - Funciona en oscuridad total sin línea de visión
  - Ideal para personas sepultadas o comunicación silenciosa
  - Visualización en tiempo real de magnitud de vibración
- **🔍 Escáner QR:** Lee códigos QR generados por otros usuarios de BeaconChat

### 🆘 Canales de Emisión de Emergencia (`EmergencyManager`)

A partir de la versión **v0.2.0**, la transmisión está centralizada en un único controlador:

```kotlin
// La UI hace solo esto:
controller.startEmergency(EmergencyType.SOS, EmergencyMode.ALL)
```

El `EmergencyManager` orquesta automáticamente todos los canales activos según el modo elegido:

| Modo | Linterna | Vibración | Ultrasonido | BLE |
|------|:---:|:---:|:---:|:---:|
| `ALL` (Todo) | ✅ | ✅ | ✅ | ✅ |
| `LIGHT` (Luz) | ✅ | ❌ | ❌ | ❌ |
| `VIBRATION` (Vibración) | ❌ | ✅ | ❌ | ❌ |
| `SOUND` (Ultrasonido) | ❌ | ❌ | ✅ | ❌ |
| `BLE` (Bluetooth) | ❌ | ❌ | ❌ | ✅ |
| `DISCREET` (Discreto) | ❌ | ❌ | ❌ | ✅ |

### 📡 BLE Emergency Beacon (NUEVO)

**Modo Discreto** — Transmite y detecta emergencias silenciosamente via Bluetooth Low Energy sin emitir luz ni sonido:

- **BleEmitter**: Emite un advertisement BLE con el tipo de emergencia codificado (UUID `0000BECE-...`)
- **BleScanner**: Detecta beacons de emergencia de otros dispositivos BeaconChat en el entorno
- Calidad de señal estimada por RSSI (Excelente / Buena / Débil)
- Ideal para: situaciones donde la luz delata la posición (secuestros, rehenes)
- Requiere permiso `BLUETOOTH_ADVERTISE` / `BLUETOOTH_SCAN` (Android 12+)

### 🔍 Buscar Señales (NUEVO)

Nueva pantalla unificada de recepción que combina en paralelo:
- **BLE**: Escaneo pasivo de beacons de emergencia cercanos con indicador de señal
- **Óptico**: Decodificación Morse via cámara en tiempo real (mismo canal que la pantalla Receptor)

### 📡 Bluetooth Mesh (Radar)
- **Descubrimiento de Pares:** Detecta otros dispositivos BeaconChat cercanos mediante Bluetooth Low Energy (BLE).
- **Estado de Red:** Visualiza el Callsign, la calidad de la señal (RSSI) y la última vez que fueron vistos.

## 📸 Capturas de Pantalla
| Transmisor | Receptor | Mesh Radar |
|:---:|:---:|:---:|
| *(Pendiente)* | *(Pendiente)* | *(Pendiente)* |

## 📦 Instalación

### Requisitos
- Android Studio Hedgehog (2023.1.1) o superior
- Android SDK 34
- Gradle 8.2+
- Kotlin 1.9+
- Dispositivo físico con:
  - Android 8.0 (API 26) o superior
  - Cámara trasera con linterna
  - Motor de vibración (opcional)
  - Bluetooth LE (opcional para mesh)

### Instalación Rápida
```bash
# Clonar el repositorio
git clone https://github.com/nicobutter/beaconchat.git
cd beaconchat

# Conectar dispositivo Android por USB (habilitar "Depuración USB")

# Compilar e instalar
./gradlew installDebug

# O usar el Makefile
make install
```

### Despliegue de Producción
```bash
# Build completo con limpieza e instalación
# Guarda APK automáticamente en releases/
make deploy

# Listar APKs generados
make apks

# Build de release (APK firmado)
./gradlew assembleRelease
```

**📦 APKs Generados:**
Cada `make deploy` crea automáticamente dos archivos en `releases/`:
- `BeaconChat_YYYYMMDD_HHMMSS.apk` - Versión con timestamp
- `BeaconChat_latest.apk` - Última versión (sobrescrita cada deploy)

📖 **[Ver guía completa de desarrollo →](QUICKSTART.md)**

---

## 📚 Documentación

### 📖 Documentación Técnica Completa

- **[PROTOCOLO_OPTICO.md](PROTOCOLO_OPTICO.md)** - Especificación completa del protocolo de comunicación óptica
  - Estructura del protocolo con marcadores START/END
  - Pipeline de procesamiento de señal
  - Algoritmos de filtrado y detección
  - Ejemplos de código y cálculos
  - Soporte multi-idioma (9 alfabetos)
  - Tablas de timing optimizado para cámaras

- **[QUICKSTART.md](QUICKSTART.md)** - Guía rápida de desarrollo y despliegue
  - Configuración del entorno
  - Comandos de build y deploy
  - Testing en dispositivos físicos

- **[DESARROLLO.md](DESARROLLO.md)** - Guía completa de desarrollo
  - Arquitectura del proyecto
  - Patrones de diseño utilizados
  - Guías de contribución

### 🎓 Referencias Académicas

El protocolo óptico está basado en investigación científica y estándares internacionales:

- **IEEE 802.15.7**: "Short-Range Wireless Optical Communication Using Visible Light"
- **NIST ASTM E2845**: "Standard Practice for Evaluating Emergency Response Robot Capabilities"
- **ITU-R M.1677**: "International Morse code"
- Trabajos de Kahn et al. (Stanford) sobre VLC
- Protocolos de drones de rescate (DJI, Parrot)

---

## � Changelog

### v0.2.0 — Arquitectura Limpia + BLE Emergency (mayo 2026)

**Arquitectura refactorizada por completo.** Se introdujo una separación clara en capas:

```
domain/     → EmergencyType, EmergencyMode, EmergencyState, SignalConfig
emitter/    → SignalEmitter, LightEmitter, VibrationEmitter, SoundEmitter, BleEmitter
scanner/    → SignalScanner, BleScanner
controller/ → EmergencyManager (orquestador central)
```

**Nuevas funcionalidades:**
- ✅ `BleEmitter` — beacon BLE de emergencia silencioso (UUID `0000BECE-...`)
- ✅ `BleScanner` — detección pasiva de emergencias BLE cercanas
- ✅ `EmergencyManager` — orquestador con `StateFlow<EmergencyState>` global
- ✅ `EmergencyMode.DISCREET` — modo solo BLE sin luz ni ruido
- ✅ Pantalla **"Buscar señales"** — BLE + óptico en paralelo
- ✅ UI simplificada: la pantalla de transmisión llama `startEmergency(type, mode)` y nada más
- ✅ `EmergencyType` en capa domain (sin dependencia de Compose): SOS, HELP, TRAPPED, KIDNAPPED, INJURED, OK, LOCATION

**Cambios internos:**
- `EmergencyType` y `EmergencyMethod` eliminados de la capa UI
- `EmergencyMethod` renombrado a `EmergencyMode` y movido a `domain/`
- `TransmitterScreen` delega toda lógica vía callback `onEmergencyTrigger(type, mode)`
- `EmergencyTransmissionScreen` ya no gestiona coroutines ni controladores directamente

### v0.1.0 — Versión inicial (2024)
- Protocolo óptico VLC con sincronización START/END
- Soporte Morse multi-idioma (9 alfabetos)
- Canales: linterna, vibración, ultrasonido, QR
- BLE mesh radar (descubrimiento de pares)
- Decodificación óptica y táctil en tiempo real

---

## �👤 Autor

**Nicolás Butterfield**
- 📧 Email: [nicobutter@gmail.com](mailto:nicobutter@gmail.com)
- 🐙 GitHub: [@nicobutter](https://github.com/nicobutter)

---

## 🤝 Contribuciones

¡Las contribuciones son bienvenidas! BeaconChat es software de emergencia que puede salvar vidas.

### Áreas de Contribución
- 🌍 **Traducción**: Agregar más idiomas/alfabetos Morse
- 📡 **Protocolo**: Mejorar timing, filtros, robustez
- 🎨 **UI/UX**: Diseño más accesible en situaciones de crisis
- 🧪 **Testing**: Pruebas en escenarios reales
- 📖 **Documentación**: Guías, tutoriales, casos de uso

### Proceso
1. Fork el repositorio
2. Crea una rama: `git checkout -b feature/nueva-funcionalidad`
3. Commit: `git commit -m 'feat: descripción'`
4. Push: `git push origin feature/nueva-funcionalidad`
5. Abre un Pull Request

---

## 📜 Licencia

Este proyecto está bajo la licencia MIT. Ver archivo [LICENSE](LICENSE) para más detalles.

**Uso libre para fines humanitarios y de emergencia.**

---

## ⚠️ Descargo de Responsabilidad

BeaconChat es una herramienta de **asistencia** en emergencias, **NO un reemplazo** de sistemas profesionales de rescate.

- ✅ **Úsalo** cuando no hay otras opciones de comunicación
- ✅ **Combínalo** con señales de humo, silbatos, golpes
- ⚠️ **No reemplaza** llamadas al 911 / servicios de emergencia
- ⚠️ **No garantiza** rescate inmediato
- ⚠️ **Requiere** línea de vista entre emisor/receptor

**En emergencia real: 911 / 112 / 999 primero. BeaconChat después.**

---

💡 *¡Vamos a salvar vidas!*
