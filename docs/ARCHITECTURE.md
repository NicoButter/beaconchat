# BeaconChat — Architecture Summary

> A developer-oriented overview of the codebase structure, components, data flow, and known limitations.

---

## 1. High-Level Overview

**BeaconChat** is an offline-first Android emergency communication app that turns a smartphone into a multi-channel signal beacon. It requires no internet, cellular, or Wi-Fi connectivity.

### Core Purpose
Enable communication in disaster scenarios (earthquakes, shipwrecks, blackouts, kidnappings) using only the hardware built into a standard smartphone.

### Main Features

| Feature | Description |
|---|---|
| **Light Transmission** | Sends Morse code via camera flashlight (LED torch) |
| **Light Reception** | Decodes Morse from another device's flashlight using the camera |
| **Vibration Transmission** | Sends Morse through the haptic motor |
| **Vibration Detection** | Reads Morse patterns via the accelerometer (contact-based) |
| **Ultrasound Transmission** | Sends Morse as 18.5 kHz audio tones (inaudible) |
| **QR Code** | Generates and scans QR codes containing messages |
| **BLE Mesh** | Discovers nearby BeaconChat devices via Bluetooth Low Energy |
| **Optical Oscilloscope** | Real-time camera waveform for signal debugging/analysis |
| **Light Radar (LightMap)** | Detects and roughly locates other devices by optical heartbeat |
| **Emergency Screen** | Fullscreen one-tap SOS/HELP/OK/LOCATION transmission |
| **Multi-language Morse** | 9 writing systems: Latin, Cyrillic, Greek, Hebrew, Arabic, Japanese, Korean, Thai, Persian |

---

## 2. Project Structure

```
beaconchat/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml          # Permissions: CAMERA, VIBRATE, BLUETOOTH_*, LOCATION
│       └── java/com/nicobutter/beaconchat/
│           ├── MainActivity.kt          # Entry point, navigation hub
│           ├── data/                    # Persistence layer
│           ├── mesh/                    # Bluetooth LE mesh networking
│           ├── transceiver/             # Signal encoding, decoding, and hardware controllers
│           ├── lightmap/                # Optical scanning (oscilloscope + device radar)
│           └── ui/
│               ├── screens/             # Jetpack Compose screens
│               └── theme/               # Material3 theme (Color, Type, Theme)
├── docs/                                # Technical documentation
├── PSI/                                 # Requirements and architecture specs
└── gradle/libs.versions.toml            # Dependency catalog
```

### Package Responsibilities

| Package | Responsibility |
|---|---|
| `data/` | Persists user callsign via AndroidX DataStore |
| `mesh/` | BLE advertising, scanning, GATT server/client, chat messaging |
| `transceiver/` | All signal encoding/decoding logic + hardware controller abstractions |
| `lightmap/` | Camera-based optical analysis: oscilloscope waveform and peer detection |
| `ui/screens/` | All Compose UI screens — stateful, directly consuming controllers |
| `ui/theme/` | App-wide Material3 color scheme, typography |

---

## 3. Core Components

### Entry Point

| Class | Role |
|---|---|
| `MainActivity` | Single Activity. Instantiates all hardware controllers, manages navigation state via `currentScreen` string variable with a `Scaffold` + `NavigationBar`. Calls `cleanupControllers()` on screen switch. |

### Transceiver Package

| Class | Role |
|---|---|
| `FlashlightController` | Drives the camera LED torch. Accepts `List<Long>` timing sequences and switches the torch ON/OFF with coroutine-based delays. Uses `Mutex` for concurrency safety. |
| `VibrationController` | Drives the haptic motor. Same `List<Long>` timing interface. Handles Android API compatibility (SDK 26 `VibrationEffect` vs legacy). |
| `SoundController` | Generates a 18.5 kHz sine wave via `AudioTrack`. Same timing interface, tone ON = pulse, silence = gap. |
| `MorseEncoder` | Converts a text string to a `List<Long>` timing sequence. Prepends a synchronisation preamble. Selects the correct Morse alphabet based on `Locale.getDefault()`. |
| `MorseDecoder` | Stateful decoder. Receives `(isLightOn: Boolean, durationMs: Long)` events and accumulates them into dot/dash symbols, then letters, then a full message. Handles the 4-stage preamble detection state machine. |
| `MorseAlphabet` | Static Morse maps for 9 writing systems + locale-to-alphabet routing + flag emoji lookup. |
| `BinaryEncoder` | Alternative encoding: converts each character to 8-bit ASCII binary, emitting 200 ms pulses per bit. |
| `LightDetector` | `ImageAnalysis.Analyzer`. Reads YUV camera frames, computes average brightness in a center ROI, applies an adaptive threshold, and fires `onLightStateChanged(isOn, durationMs)` callbacks. |
| `VibrationDetector` | `SensorEventListener` on the accelerometer at `SENSOR_DELAY_FASTEST` (~200 Hz). Applies a high-pass gravity filter, then exponential smoothing, and fires magnitude callbacks. |
| `VibrationOscilloscope` | Similar to `VibrationDetector` but oriented toward real-time waveform display. |
| `QRScanner` | `ImageAnalysis.Analyzer` using ZXing. Decodes QR codes with 1-second debounce. |

### LightMap Package

| Class | Role |
|---|---|
| `LightScanner` | `ImageAnalysis.Analyzer`. Maintains an intensity ring-buffer for oscilloscope display, computes signal stats (FPS, min/max/avg), detects flash events, and attempts to locate devices by angle. |
| `OpticalOscilloscope` | Full optical decoding pipeline: frame → exponential smoothing (α=0.7) → adaptive threshold → ON/OFF transitions → pulse classification → Morse decoding. Exposes `StateFlow`s for signal data and decoded text. |
| `HeartbeatPattern` | Generates a 3-pulse identification pattern (~500 ms) used by the LightMap radar. |
| `DetectedDevice` | Data class representing a detected optical peer (signal angle, intensity, last-seen timestamp). |

### Mesh Package

| Class | Role |
|---|---|
| `BLEMeshController` | Manages BLE advertising (custom service UUID `0000BEEF-...`) and scanning. Runs a GATT server to receive messages and acts as a GATT client to send them. Exposes `StateFlow`s: `peers`, `messages`, `isAdvertising`, `isScanning`. |
| `MeshPeer` | Data class: Bluetooth address, device name, callsign, RSSI, last-seen timestamp. Includes helpers `getSignalQuality()` and `timeSinceLastSeen()`. |
| `ChatMessage` | Data class representing a single BLE chat message (sender callsign, content, timestamp, direction). |

### Data Package

| Class | Role |
|---|---|
| `UserPreferences` | Wraps AndroidX DataStore. Stores/retrieves user callsign (3–8 chars) and enabled flag as reactive `Flow`s. |

### UI Screens

| Screen | Role |
|---|---|
| `WelcomeScreen` | Onboarding / callsign setup. Entry point before main nav. |
| `TransmitterScreen` | Message input + transmission controls. Supports Flashlight / Vibration / Sound × Morse / ASCII Binary. Loops continuously while active. |
| `ReceiverScreen` | Camera preview + `LightDetector` + `MorseDecoder` pipeline. Switches between light and QR modes. Link to `VibrationDetectorScreen`. |
| `OscilloscopeScreen` | Camera-based waveform display powered by `OpticalOscilloscope`. Shows intensity curve, stats, and decoded text. |
| `LightMapScreen` | Radar-style display of optically detected nearby devices using `LightScanner`. |
| `VibrationDetectorScreen` | Accelerometer waveform + Morse decode via `VibrationDetector`. |
| `MeshScreen` | Displays `BLEMeshController` peers and BLE chat. |
| `SettingsScreen` | Callsign configuration, saved via `UserPreferences`. |
| `EmergencyTransmissionScreen` | Fullscreen emergency beacon. One-tap SOS/HELP/OK/LOCATION via any or all channels simultaneously. |

---

## 4. Data Flow

### Transmission: "User presses Send"

```
TransmitterScreen
  │
  ├─ User types message + selects method (Flashlight/Vibration/Sound) + encoding (Morse/Binary)
  │
  ├─ [Morse]  MorseEncoder.encode(text)   → List<Long> timings
  │   or
  │   [Binary] BinaryEncoder.encode(text) → List<Long> timings
  │
  └─ Coroutine loop (while isActive && isTransmitting):
       ├─ FlashlightController.transmit(timings)   → Camera2 torch ON/OFF
       ├─ VibrationController.transmit(timings)    → VibrationEffect ON/OFF
       └─ SoundController.transmit(timings)        → AudioTrack sine wave ON/OFF
```

### Reception: "Camera detects flashlight"

```
ReceiverScreen
  │
  └─ CameraX ImageAnalysis bound to LightDetector
       │
       ├─ Per frame: compute center-ROI brightness
       ├─ Adaptive threshold → isLightOn (bool)
       ├─ State change? → measure durationMs
       │
       └─ MorseDecoder.onLightStateChanged(isLightOn, durationMs)
            │
            ├─ Preamble state machine (4 stages)
            ├─ ON pulse → DOT (100–225ms) or DASH (300–500ms)
            ├─ OFF gap  → symbol / letter / word space
            └─ Append decoded char → decodedMessage (observable State)
```

### BLE Mesh: "User sends BLE message"

```
MeshScreen
  │
  └─ BLEMeshController.sendMessage(text, targetPeer)
       │
       ├─ GATT client connects to peer
       ├─ Writes to MESSAGE_CHARACTERISTIC
       └─ _messages StateFlow updated → UI recomposition
```

---

## 5. Signal System (Optical Protocol)

### Transmission Side

1. `MorseEncoder` converts text to a `List<Long>` of alternating ON/OFF durations.
2. A **synchronisation preamble** is prepended: `ON 300ms → OFF 300ms → ON 900ms → OFF 500ms`.
3. Timing constants:

| Symbol | Duration |
|---|---|
| DOT | 150 ms |
| DASH | 400 ms |
| Symbol gap | 150 ms |
| Letter gap | 500 ms |
| Word gap | 1000 ms |

4. `FlashlightController` iterates through the list with coroutine `delay()`, toggling the torch via `CameraManager.setTorchMode()`.

### Reception Side — `OpticalOscilloscope` Pipeline

```
Raw YUV frame
  → center ROI brightness average
  → exponential smoothing: B = 0.7 × B_prev + 0.3 × B_current  (eliminates camera jitter)
  → adaptive threshold:     T = min + (max − min) × 0.4         (adapts to ambient light)
  → ON/OFF transition detection + wall-clock duration measurement
  → pulse classifier:  <80ms = noise, 80–200ms = DOT, 200–500ms = DASH, >500ms = GAP
  → MorseDecoder preamble detection → message decoding
```

### Protocol Compliance
The protocol is inspired by:
- **IEEE 802.15.7** — Visible Light Communication
- **NIST** — Search and rescue robotics beacons
- **ITU-R M.1677** — Emergency radiobeacon systems

### Vibration Channel
- Transmitter vibrates the haptic motor using the same `List<Long>` timing sequence.
- Receiver reads accelerometer at ~200 Hz (SENSOR_DELAY_FASTEST).
- High-pass filter removes constant 9.8 m/s² gravity component.
- Exponential smoothing (α=0.7) removes jitter.
- Works by physical contact between devices (e.g., both placed on same surface).

---

## 6. Current Limitations

### Architecture

| Issue | Impact |
|---|---|
| **No ViewModel / no state hoisting** | All state lives directly inside Composable functions. Screen-level state is lost on recomposition edge cases. Business logic is mixed with UI. |
| **Controllers instantiated in `MainActivity`** | Controllers are passed down as plain parameters through 3–5 levels of Composables (prop drilling). No dependency injection. |
| **Navigation as a `String` variable** | `currentScreen by mutableStateOf("welcome")` is a fragile, stringly-typed router. No back-stack, no deep-link support. |
| **`MorseDecoder` is stateful but not scoped** | The decoder holds mutable state (`currentSymbol`, `preambleStage`) but is created with `remember { }` inside a Composable — its state resets if the screen recomposes from scratch. |
| **`ReceiverScreen` hardcodes Latin Morse** | `MorseDecoder` only decodes Latin alphabet regardless of locale, while `MorseEncoder` supports 9 scripts. |

### Functionality

| Issue | Impact |
|---|---|
| **No end marker / ACK** | The preamble is detected but there is no END-of-message marker check in the decoder; reception ends only by timeout. |
| **GPS location is unimplemented** | README mentions GPS but no code exists for it. |
| **QR transmission not wired to `MorseEncoder`** | QR scanning works, but QR *generation* from a typed message is not visible in any screen. |
| **BLE mesh is discovery-only** | GATT server/client code exists, but mesh *relay* (forwarding messages across hops) is not implemented. |
| **`SoundController` has no receiver** | There is no `SoundDetector` counterpart; ultrasound transmission is one-way. |
| **No error feedback to the user** | Hardware failures (no torch, no vibrator, no BLE) are logged but not surfaced in the UI. |

---

## 7. Suggested Improvements

### 1. Introduce ViewModels
Move all business logic out of Composables. Each screen should have a corresponding `ViewModel`:

```
TransmitterViewModel  — holds transmission state, coroutine scope, encoding logic
ReceiverViewModel     — holds decoder state, camera lifecycle management
MeshViewModel         — wraps BLEMeshController flows
```

### 2. Use a Proper Navigation Library
Replace the `String` state variable with **Jetpack Navigation Compose** (`NavController` + type-safe routes). This gives free back-stack management and future deep-link support.

### 3. Dependency Injection
Use **Hilt** (or at minimum a manual `AppModule` object) to provide controllers as singletons. This eliminates prop-drilling and allows testing with fakes.

### 4. Fix Decoder / Encoder Symmetry
`MorseDecoder` should use `MorseAlphabet` reverse maps per locale, mirroring `MorseEncoder`. Currently only Latin is decoded.

### 5. Add a Sound Receiver
Implement a `SoundDetector` using `AudioRecord` that listens for 18.5 kHz amplitude pulses and feeds them to `MorseDecoder`, closing the ultrasound channel loop.

### 6. Surface Errors in UI
Controllers should expose error `StateFlow`s. Screens should react and show a `Snackbar` or dialog when hardware is unavailable.

### 7. Define an Encoder Interface
```kotlin
interface SignalEncoder {
    fun encode(text: String): List<Long>
}
```
Both `MorseEncoder` and `BinaryEncoder` implement it. `TransmitterScreen` depends on the interface, not the concrete class — easier to test and extend.

### 8. Implement GPS Location
Wire the existing `LOCATION` `EmergencyType` to an actual `FusedLocationProviderClient` call. Encode the last known coordinates into the transmitted message.

---

## Dependency Summary

| Library | Purpose |
|---|---|
| Jetpack Compose + Material3 | UI framework |
| CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) | Camera access and image analysis |
| AndroidX DataStore | Persistent key-value user preferences |
| ZXing (`core` + `zxing-android-embedded`) | QR code generation and scanning |
| Android BLE (`BluetoothLeAdvertiser`, `BluetoothGattServer`) | Mesh networking |
| Kotlin Coroutines | Async transmission and sensor callbacks |
| Android `AudioTrack` | Ultrasound generation |
| Android `SensorManager` | Accelerometer-based vibration detection |
