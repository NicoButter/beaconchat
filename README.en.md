# BeaconChat 📡

<!-- Language Selection -->
<p align="center">
  <a href="README.md">🇪🇸 Español</a> •
  <a href="README.en.md"><strong>🇬🇧 English</strong></a>
</p>

<!-- LOGO PLACEHOLDER -->
<p align="center">
  <img src="docs/logo.png" alt="BeaconChat Logo" width="200"/>
  <br>
  <i>(Logo Pending)</i>
</p>

> **One light. One phone. One life saved.**

When there's no signal…  
When there's no 3G, 4G, 5G, or WiFi…  
When you're buried, kidnapped, lost at sea, or trapped in rubble…  
**Your phone still has a flashlight.**  
And that flashlight can scream for you.

**BeaconChat** turns your phone into an **emergency beacon** that:
- Sends encoded messages in **Morse code** using the LED flashlight
- Uses **vibration** as a tactile channel (for buried victims)
- **Detects vibrations** using accelerometer - communication without light
- Allows **any other phone** to read it with its camera
- Works **100% offline**
- Includes your **last known GPS location** (Coming soon)

---

## 🆘 Real scenarios where BeaconChat saves lives
- **Earthquake** → person under rubble places phone against the ground → flashes "HELP"
- **Kidnapping** → victim leaves phone on window → neighbor records and receives "KIDNAPPED - 123 STREET"
- **Shipwreck** → castaway on raft → flashlight sends "SOS" for kilometers
- **Massive blackout** → trapped family → vibration + light = "WE ARE ALIVE"

## ⚡ How it works
1. **You** → type "HELP" → **BeaconChat** → flashlight blinks in Morse.
2. **Another phone** → opens **BeaconChat** → points camera → receives "HELP".

---

## 🚀 Technical Features

### 📡 Professional Optical Communication Protocol

BeaconChat implements a **robust Visible Light Communication (VLC) protocol** based on international standards:

#### 🌟 Technical Foundations
- **IEEE 802.15.7**: Visible Light Communication standard
- **NIST**: Specifications for rescue robotics
- **ITU-R M.1677**: Emergency radiobeacon systems

#### ⚙️ Protocol Features

**Synchronization and Markers:**
- ✅ Start marker (START): 300-300-900ms sequence for automatic synchronization
- ✅ End marker (END): 600-100ms sequence to confirm complete message
- ✅ Automatic start/end detection without manual intervention

**Signal Processing:**
- ✅ Exponential smoothing filter (α = 0.7) eliminates camera jitter
- ✅ Adaptive dynamic threshold: min + (max-min) × 0.4
- ✅ Pulse classification: <80ms noise, 80-200ms DOT, 200-500ms DASH, >500ms GAP
- ✅ Compatible with standard 30fps cameras

**Robustness:**
- ✅ Works with slight hand movement (up to 10cm/s)
- ✅ Adapts to variable light (indoor/outdoor)
- ✅ Automatically rejects <80ms noise
- ✅ Compatible with low-quality cameras (720p minimum)

**Camera-Optimized Timing:**
| Symbol | Duration | Frames @30fps |
|---------|----------|---------------|
| DOT     | 150ms    | ~5 frames     |
| DASH    | 400ms    | ~12 frames    |
| GAP     | 150ms    | ~5 frames     |
| START   | 1500ms   | ~45 frames    |
| END     | 700ms    | ~21 frames    |

📖 **[See complete protocol specification →](PROTOCOLO_OPTICO.md)** *(Spanish)*

---

### 🌍 Multi-Language Morse Code Support

**BeaconChat is the first emergency app with native support for 9 different writing systems**, enabling communication in your native language even during crises.

#### 🗺️ Supported Alphabets

| System | Languages | Visual Flag | Special Characters |
|---------|---------|----------------|----------------------|
| **Latin** | Spanish, English, French, German, Italian, Portuguese | 🇪🇸 🇬🇧 🇫🇷 🇩🇪 🇮🇹 🇵🇹 | A-Z + Ñ, Ä, Ö, Ü, Ç |
| **Cyrillic** | Russian, Ukrainian, Bulgarian, Serbian | 🇷🇺 🇺🇦 🇧🇬 🇷🇸 | А-Я (33 letters) |
| **Greek** | Modern Greek | 🇬🇷 | Α-Ω (24 letters) |
| **Hebrew** | Modern Hebrew | 🇮🇱 | א-ת (22 letters) |
| **Arabic** | Standard Arabic | 🇸🇦 🇪🇬 🇦🇪 | ا-ي (28 letters) |
| **Japanese Wabun** | Japanese kana | 🇯🇵 | あ-ん (46 kana) |
| **Korean Hangul** | Korean | 🇰🇷 | ㄱ-ㅎ, ㅏ-ㅣ |
| **Thai** | Thai | 🇹🇭 | ก-ฮ (44 consonants) |
| **Persian** | Farsi | 🇮🇷 | ا-ی (32 letters) |

#### 🎯 Automatic Detection
BeaconChat automatically detects the system language using `Locale.getDefault()`:
- No manual configuration required
- Intelligent selection of correct Morse alphabet
- Automatic fallback to Latin alphabet if language not supported

#### 🚩 Visual Identification with Flags
The app displays the **country/region flag** corresponding to the selected alphabet:
- **Spanish**: 🇪🇸 Spain, 🇲🇽 Mexico, 🇦🇷 Argentina, 🇨🇴 Colombia, 🇨🇱 Chile, etc.
- **English**: 🇬🇧 United Kingdom, 🇺🇸 United States, 🇨🇦 Canada, 🇦🇺 Australia
- **Arabic**: 🇸🇦 Saudi Arabia, 🇪🇬 Egypt, 🇦🇪 UAE, 🇲🇦 Morocco
- **French**: 🇫🇷 France, 🇧🇪 Belgium, 🇨🇭 Switzerland, 🇨🇦 Canada
- And more...

#### ✨ Benefits
- **Natural communication**: Write in your native language, even under stress
- **Global reach**: Rescuers from any country can read the message
- **No barriers**: You don't need to know English to ask for help
- **Visual confirmation**: The flag confirms the alphabet is correct

---

### 📤 Transmitter
Sends messages using multiple physical channels:
- **🔦 Flashlight:** Transmits Morse code messages using camera flash with professional VLC protocol
  - Optimized timing: DOT 150ms, DASH 400ms
  - Automatic start/end markers
  - IEEE 802.15.7 standard compliant
- **🔊 Sound:** Generates audio tones to transmit data (Morse/FSK)
- **📳 Vibration:** Uses haptic motor for tactile Morse messages
- **🔳 QR Code:** Generates dynamic QR codes containing the message and user callsign

### 📥 Receiver
Decodes environmental signals with advanced processing:
- **📷 Light Detection (Optical Oscilloscope):**
  - Camera frame analysis at 30fps
  - Exponential smoothing filter (α = 0.7) to eliminate noise
  - Adaptive dynamic threshold for variable conditions
  - Automatic START/END marker detection
  - Multi-language Morse decoding (9 alphabets)
  - Real-time light intensity visualization
- **📳 Vibration Detection (Tactile Oscilloscope):**
  - Capture via accelerometer at ~200Hz
  - High-pass filter removes constant gravity
  - Morse pattern detection via direct contact
  - Works in total darkness without line of sight
  - Ideal for buried victims or silent communication
  - Real-time vibration magnitude visualization
- **🔍 QR Scanner:** Reads QR codes generated by other BeaconChat users

### 📡 Bluetooth Mesh (Radar)
- **Peer Discovery:** Detects nearby BeaconChat devices via Bluetooth Low Energy (BLE)
- **Network Status:** Visualizes Callsign, signal quality (RSSI), and last seen time

## 📸 Screenshots
| Transmitter | Receiver | Mesh Radar |
|:---:|:---:|:---:|
| *(Pending)* | *(Pending)* | *(Pending)* |

## 📦 Installation

### Requirements
- Android Studio Hedgehog (2023.1.1) or higher
- Android SDK 34
- Gradle 8.2+
- Kotlin 1.9+
- Physical device with:
  - Android 8.0 (API 26) or higher
  - Rear camera with flashlight
  - Vibration motor (optional)
  - Bluetooth LE (optional for mesh)

### Quick Installation
```bash
# Clone repository
git clone https://github.com/nicobutter/beaconchat.git
cd beaconchat

# Connect Android device via USB (enable "USB Debugging")

# Build and install
./gradlew installDebug

# Or use Makefile
make install
```

### Production Deployment
```bash
# Full build with cleanup
make deploy

# Release build (signed APK)
./gradlew assembleRelease
```

📖 **[See complete development guide →](QUICKSTART.md)** *(Spanish)*

---

## 📚 Documentation

### 📖 Complete Technical Documentation

- **[PROTOCOLO_OPTICO.md](PROTOCOLO_OPTICO.md)** - Complete optical communication protocol specification *(Spanish)*
  - Protocol structure with START/END markers
  - Signal processing pipeline
  - Filtering and detection algorithms
  - Code examples and calculations
  - Multi-language support (9 alphabets)
  - Camera-optimized timing tables

- **[QUICKSTART.md](QUICKSTART.md)** - Quick development and deployment guide *(Spanish)*
  - Environment setup
  - Build and deploy commands
  - Physical device testing

- **[DESARROLLO.md](DESARROLLO.md)** - Complete development guide *(Spanish)*
  - Project architecture
  - Design patterns used
  - Contribution guidelines

### 🎓 Academic References

The optical protocol is based on scientific research and international standards:

- **IEEE 802.15.7**: "Short-Range Wireless Optical Communication Using Visible Light"
- **NIST ASTM E2845**: "Standard Practice for Evaluating Emergency Response Robot Capabilities"
- **ITU-R M.1677**: "International Morse code"
- Works by Kahn et al. (Stanford) on VLC
- Rescue drone protocols (DJI, Parrot)

---

## 👤 Author

**Nicolás Butterfield**
- 📧 Email: [nicobutter@gmail.com](mailto:nicobutter@gmail.com)
- 🐙 GitHub: [@nicobutter](https://github.com/nicobutter)

---

## 🤝 Contributions

Contributions are welcome! BeaconChat is emergency software that can save lives.

### Contribution Areas
- 🌍 **Translation**: Add more languages/Morse alphabets
- 📡 **Protocol**: Improve timing, filters, robustness
- 🎨 **UI/UX**: More accessible design in crisis situations
- 🧪 **Testing**: Real scenario testing
- 📖 **Documentation**: Guides, tutorials, use cases

### Process
1. Fork the repository
2. Create a branch: `git checkout -b feature/new-feature`
3. Commit: `git commit -m 'feat: description'`
4. Push: `git push origin feature/new-feature`
5. Open a Pull Request

---

## 📜 License

This project is licensed under the MIT License. See [LICENSE](LICENSE) file for details.

**Free use for humanitarian and emergency purposes.**

---

## ⚠️ Disclaimer

BeaconChat is an **assistance** tool in emergencies, **NOT a replacement** for professional rescue systems.

- ✅ **Use it** when there are no other communication options
- ✅ **Combine it** with smoke signals, whistles, knocking
- ⚠️ **Does not replace** calls to 911 / emergency services
- ⚠️ **Does not guarantee** immediate rescue
- ⚠️ **Requires** line of sight between transmitter/receiver

**In a real emergency: 911 / 112 / 999 first. BeaconChat after.**

---

💡 *Let's save lives!*
