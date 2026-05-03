# BeaconChat 📡

<!-- Language Selection -->
<p align="center">
  <a href="README.md">🇪🇸 Español</a> •
  <a href="README.en.md"><strong>🇬🇧 English</strong></a>
</p>

<!-- LOGO -->
<p align="center">
  <img src="docs/logo.png" alt="BeaconChat Logo" width="200"/>
</p>

> **One light. One phone. One life saved.**

When there's no signal…  
When there's no 3G, 4G, 5G, or WiFi…  
When you're buried, lost at sea, or trapped in rubble…  
**Your phone still has a flashlight.**  
And that flashlight can scream for you.

**BeaconChat** turns your phone into an **emergency beacon** that:
- Sends encoded messages in **Morse code** using the LED flashlight.
- Uses **vibration** as a tactile channel (for people under rubble).
- **Detects vibrations** using the accelerometer for non-visual communication.
- Silently broadcasts a **BLE emergency beacon** — invisible and inaudible.
- Allows **any other phone** to read it with its camera or via BLE scan.
- Generates and scans **QR Codes** for quick data exchange.
- Works **100% offline** with no infrastructure dependency.

---

## 🆘 Real scenarios where BeaconChat saves lives
- **Earthquake** → Person under rubble places phone against the surface → vibration transmits "HELP".
- **Mountain Rescue** → Lost hiker or castaway → flashlight sends "SOS" visible for miles.
- **Massive Blackout** → Proximity communication without mobile networks via ultrasound or light.
- **Security** → Discrete message sending via vibratory patterns.

## ⚡ How it works
1. **Sender** → Type "HELP" → **BeaconChat** → Flashlight/vibration blinks in Morse.
2. **Receiver** → Opens **BeaconChat** → Points camera or places phone → Receives "HELP".

---

## 🚀 Technical Features

### 📡 Optical Communication Protocol (VLC)
BeaconChat implements a robust visible light communication protocol based on international standards:

- **Optimized Timing**: DOT (150ms), DASH (400ms), SYMBOL_SPACE (150ms).
- **Auto Synchronization**: Receiver automatically detects message start via a preamble (300ms ON / 300ms OFF / 900ms ON / 500ms OFF).
- **Dynamic Threshold**: Auto-adjusts to environmental lighting conditions.
- **Noise Filtering**: Exponential smoothing algorithms to eliminate interference.

### 🌎 Multi-Language Morse (9 Alphabets)
Native support for 9 writing systems with automatic language detection:
- **Latin**: Spanish, English, French, German, etc.
- **Cyrillic**, **Greek**, **Hebrew**, **Arabic**, **Japanese (Wabun)**, **Korean (Hangul)**, **Thai**, **Persian**.

### 📥 Receiving Channels
Native support for 9 writing systems with automatic language detection:
- **Latin**: Spanish, English, French, German, etc.
- **Cyrillic**, **Greek**, **Hebrew**, **Arabic**, **Japanese (Wabun)**, **Korean (Hangul)**, **Thai**, **Persian**.

### 🆘 Emergency Emission Channels (`EmergencyManager`)

As of **v0.2.0**, all transmission is orchestrated by a single controller:

```kotlin
// The UI does only this:
controller.startEmergency(EmergencyType.SOS, EmergencyMode.ALL)
```

`EmergencyManager` automatically activates the right channels for the chosen mode:

| Mode | Flashlight | Vibration | Ultrasound | BLE |
|------|:---:|:---:|:---:|:---:|
| `ALL` | ✅ | ✅ | ✅ | ✅ |
| `LIGHT` | ✅ | ❌ | ❌ | ❌ |
| `VIBRATION` | ❌ | ✅ | ❌ | ❌ |
| `SOUND` | ❌ | ❌ | ✅ | ❌ |
| `BLE` | ❌ | ❌ | ❌ | ✅ |
| `DISCREET` | ❌ | ❌ | ❌ | ✅ |

### 📡 BLE Emergency Beacon (NEW)

**Discreet Mode** — silently transmits and detects emergencies via Bluetooth Low Energy, with no light or sound:

- **BleEmitter**: broadcasts a BLE advertisement with the encoded emergency type (UUID `0000BECE-...`)
- **BleScanner**: detects nearby BeaconChat emergency beacons from other devices
- Signal quality estimated by RSSI (Excellent / Good / Weak)
- Ideal for: situations where light betrays position (kidnapping, hostage scenarios)
- Requires `BLUETOOTH_ADVERTISE` / `BLUETOOTH_SCAN` permissions (Android 12+)

### 🔍 Signal Scanner Screen (NEW)

New unified reception screen combining two channels in parallel:
- **BLE**: passive scan for nearby emergency beacons with signal strength indicator
- **Optical**: real-time Morse decoding via camera (same engine as the Receiver screen)

### 📥 Receiving Channels
- **📷 Optical Oscilloscope**: Morse decoding via camera @30fps.
- **📳 Tactile Oscilloscope**: Vibration decoding via accelerometer @200Hz.
- **🔍 QR Scanner**: Instant reading of QR encoded messages.

### 📤 Transmission Channels
- **🔦 Flashlight**: LED flash with precise millisecond control.
- **📳 Vibrator**: Tactile Morse patterns.
- **🔊 Ultrasound**: Data transmission via inaudible frequencies (Experimental).
- **📡 BLE Beacon**: Silent emergency broadcast via Bluetooth Low Energy.
- **🔳 QR Generator**: QR code creation with message and user Callsign.

### 📡 Bluetooth Mesh (Radar)
- **Peer Discovery**: Detects nearby BeaconChat devices via Bluetooth Low Energy.
- **Network Status**: Displays Callsign, signal quality (RSSI), and last-seen timestamp.

## 📸 Screenshots
*(Coming Soon)*

## 📦 Installation

### Requirements
- Android 8.0 (API 26) or higher.
- Camera with Flash.
- Accelerometer (for vibration detection).

### Build (Developers)
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📋 Changelog

### v0.2.0 — Clean Architecture + BLE Emergency (May 2026)

**Full architectural refactor.** Clear layer separation introduced:

```
domain/     → EmergencyType, EmergencyMode, EmergencyState, SignalConfig
emitter/    → SignalEmitter, LightEmitter, VibrationEmitter, SoundEmitter, BleEmitter
scanner/    → SignalScanner, BleScanner
controller/ → EmergencyManager (central orchestrator)
```

**New features:**
- ✅ `BleEmitter` — silent BLE emergency beacon (UUID `0000BECE-...`)
- ✅ `BleScanner` — passive detection of nearby BLE emergencies
- ✅ `EmergencyManager` — orchestrator with global `StateFlow<EmergencyState>`
- ✅ `EmergencyMode.DISCREET` — BLE-only mode, no light or sound
- ✅ **"Signal Scanner" screen** — BLE + optical reception in parallel
- ✅ Simplified UI: transmission screen calls `startEmergency(type, mode)` and nothing else
- ✅ `EmergencyType` in domain layer (no Compose dependency): SOS, HELP, TRAPPED, KIDNAPPED, INJURED, OK, LOCATION

**Internal changes:**
- `EmergencyType` and `EmergencyMethod` removed from the UI layer
- `EmergencyMethod` renamed to `EmergencyMode` and moved to `domain/`
- `TransmitterScreen` delegates all logic via `onEmergencyTrigger(type, mode)` callback
- `EmergencyTransmissionScreen` no longer manages coroutines or controllers directly

### v0.1.0 — Initial release (2024)
- VLC optical protocol with START/END synchronization
- Multi-language Morse support (9 alphabets)
- Channels: flashlight, vibration, ultrasound, QR
- BLE mesh radar (peer discovery)
- Real-time optical and tactile decoding

---

## 👤 Author

**Nicolás Butterfield**
- 📧 Email: [nicobutter@gmail.com](mailto:nicobutter@gmail.com)
- 🐙 GitHub: [@nicobutter](https://github.com/nicobutter)

---

## 🤝 Contributing

Contributions are welcome! BeaconChat is emergency software that can save lives.

### Areas
- 🌍 **Translations**: Add more languages/Morse alphabets
- 📡 **Protocol**: Improve timing, filters, robustness
- 🎨 **UI/UX**: More accessible design for crisis situations
- 🧪 **Testing**: Tests in real scenarios
- 📖 **Docs**: Guides, tutorials, use cases

---

## 📜 License

MIT License. See [LICENSE](LICENSE) for details.

**Free to use for humanitarian and emergency purposes.**

---
Designed for resilience. **BeaconChat** 2026.
