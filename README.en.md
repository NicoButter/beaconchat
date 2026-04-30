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
- Allows **any other phone** to read it with its camera.
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
- **📷 Optical Oscilloscope**: Morse decoding via camera @30fps.
- **📳 Tactile Oscilloscope**: Vibration decoding via accelerometer @200Hz.
- **🔍 QR Scanner**: Instant reading of QR encoded messages.

### 📤 Transmission Channels
- **🔦 Flashlight**: LED flash with precise millisecond control.
- **📳 Vibrator**: Tactile Morse patterns.
- **🔊 Ultrasound**: Data transmission via inaudible frequencies (Experimental).
- **🔳 QR Generator**: QR code creation with message and user Callsign.

## 📸 Screenshots
*(Coming Soon)*

## 📦 Installation

### Requirements
- Android 8.0 (API 26) or higher.
- Camera with Flash.
- Accelerometer (for vibration detection).

### Build (Developers)
]0;lordcommander@fedora:~/proyectos_2024/beaconchat]3008;start=ada8e335-a458-41d6-8ed6-9e283e3f7f18;machineid=7fc180b5808f4d5daa1e8fa9073d9c54;user=lordcommander;hostname=fedora;bootid=ae3be654-f1ec-4aad-b2cf-a64a576866db;pid=00000000000000196865;type=shell;cwd=/home/lordcommander/proyectos_2024/beaconchat\Starting a Gradle Daemon, 2 incompatible and 1 stopped Daemons could not be reused, use --status for details

> Configure project :app
WARNING: The option setting 'android.defaults.buildfeatures.buildconfig=true' is deprecated.
The current default is 'false'.
It will be removed in version 10.0 of the Android Gradle plugin.
To keep using this feature, add the following to your module-level build.gradle files:
    android.buildFeatures.buildConfig = true
or from Android Studio, click: `Refactor` > `Migrate BuildConfig to Gradle Build Files`.

> Task :app:preBuild UP-TO-DATE
> Task :app:preDebugBuild UP-TO-DATE
> Task :app:mergeDebugNativeDebugMetadata NO-SOURCE
> Task :app:generateDebugBuildConfig UP-TO-DATE
> Task :app:checkDebugAarMetadata UP-TO-DATE
> Task :app:processDebugNavigationResources UP-TO-DATE
> Task :app:compileDebugNavigationResources UP-TO-DATE
> Task :app:generateDebugResValues UP-TO-DATE
> Task :app:mapDebugSourceSetPaths UP-TO-DATE
> Task :app:generateDebugResources UP-TO-DATE
> Task :app:mergeDebugResources UP-TO-DATE
> Task :app:packageDebugResources UP-TO-DATE
> Task :app:parseDebugLocalResources UP-TO-DATE
> Task :app:createDebugCompatibleScreenManifests UP-TO-DATE
> Task :app:extractDeepLinksDebug UP-TO-DATE
> Task :app:processDebugMainManifest UP-TO-DATE
> Task :app:processDebugManifest UP-TO-DATE
> Task :app:processDebugManifestForPackage UP-TO-DATE
> Task :app:processDebugResources UP-TO-DATE
> Task :app:javaPreCompileDebug UP-TO-DATE
> Task :app:mergeDebugShaders UP-TO-DATE
> Task :app:compileDebugShaders NO-SOURCE
> Task :app:generateDebugAssets UP-TO-DATE
> Task :app:mergeDebugAssets UP-TO-DATE
> Task :app:compressDebugAssets UP-TO-DATE
> Task :app:checkDebugDuplicateClasses UP-TO-DATE
> Task :app:desugarDebugFileDependencies UP-TO-DATE
> Task :app:mergeExtDexDebug UP-TO-DATE
> Task :app:mergeLibDexDebug UP-TO-DATE
> Task :app:mergeDebugJniLibFolders UP-TO-DATE
> Task :app:mergeDebugNativeLibs UP-TO-DATE
> Task :app:stripDebugDebugSymbols UP-TO-DATE
> Task :app:validateSigningDebug UP-TO-DATE
> Task :app:writeDebugAppMetadata UP-TO-DATE
> Task :app:writeDebugSigningConfigVersions UP-TO-DATE

> Task :app:compileDebugKotlin

> Task :app:compileDebugKotlin FAILED

[Incubating] Problems report is available at: file:///home/lordcommander/proyectos_2024/beaconchat/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.13/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.
31 actionable tasks: 1 executed, 30 up-to-date

---
Designed for resilience. **BeaconChat** 2024.
