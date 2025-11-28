# BeaconChat 📡

<!-- LOGO PLACEHOLDER -->
<p align="center">
  <img src="docs/logo.png" alt="BeaconChat Logo" width="200"/>
  <br>
  <i>(Logo Pendiente)</i>
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

### 📤 Transmisor (Transmitter)
Envía mensajes utilizando múltiples canales físicos:
- **🔦 Linterna (Flashlight):** Transmite mensajes en Código Morse usando el flash de la cámara.
- **🔊 Sonido (Sound):** Genera tonos de audio para transmitir datos (Morse/FSK).
- **📳 Vibración (Vibration):** Usa el motor háptico para mensajes táctiles en Morse.
- **🔳 Código QR:** Genera códigos QR dinámicos que contienen el mensaje y el callsign del usuario.

### 📥 Receptor (Receiver)
Decodifica señales del entorno:
- **📷 Detección de Luz:** Utiliza la cámara para detectar y decodificar señales de luz (Morse/Binario).
- **🔍 Escáner QR:** Lee códigos QR generados por otros usuarios de BeaconChat.

### 📡 Bluetooth Mesh (Radar)
- **Descubrimiento de Pares:** Detecta otros dispositivos BeaconChat cercanos mediante Bluetooth Low Energy (BLE).
- **Estado de Red:** Visualiza el Callsign, la calidad de la señal (RSSI) y la última vez que fueron vistos.

## 📸 Capturas de Pantalla
| Transmisor | Receptor | Mesh Radar |
|:---:|:---:|:---:|
| *(Pendiente)* | *(Pendiente)* | *(Pendiente)* |

## 📦 Instalación
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/nicobutter/beaconchat.git
   ```
2. Abrir en Android Studio.
3. Sincronizar el proyecto con Gradle.
4. Ejecutar en un dispositivo físico.

## 👤 Autor
**Nicolás Butterfield**
- 📧 Email: [nicobutter@gmail.com](mailto:nicobutter@gmail.com)
- 🐙 GitHub: [@nicobutter](https://github.com/nicobutter)

---
Vamos a salvar vidas!*
