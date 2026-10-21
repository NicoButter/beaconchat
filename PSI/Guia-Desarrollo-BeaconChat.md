# Desarrollo y despliegue de BeaconChat

La ruta de referencia recomendada es `./gradlew testDebugUnitTest assembleDebug` con JDK 17/21. Algunos scripts históricos utilizan rutas Fedora específicas. Se unifican aquí las guías de desarrollo y arranque rápido.


## Contrato permanente de UX y navegación · 21/10/2026

Este contrato rige las implementaciones posteriores. Una mejora técnica debe integrarse en su módulo y reutilizar la estructura existente. **No rediseñar pantallas ni cambiar rutas, jerarquía o identidad institucional sin solicitud expresa del usuario.** Los rediseños adicionales se presentan como propuesta y esperan autorización; no se ejecutan como efecto secundario de una función técnica.

### Responsabilidades y rutas

Flujo principal: **Bienvenida institucional → Inicio → acción elegida → Inicio**. `MainActivity` crea e inyecta controladores; `ui/BeaconNavigation.kt` es el único propietario de las rutas de aplicación y el regreso a Inicio. Los módulos reciben callbacks: no pueden convertir una pantalla técnica en el nuevo inicio ni volver a abrir la bienvenida.

| Pantalla/ruta | Responsabilidad | Entrada / salida |
| --- | --- | --- |
| `WelcomeScreen` / WELCOME | Solo logo original BeaconChat, nombre BeaconChat y logo original Vetrabyte. Sin botones, modalidades, permisos, red o transmisiones | Inicio nuevo de la actividad: transición automática de 450 ms a HOME. No reaparece en navegación interna ni al restaurar estado |
| `HomeScreen` / HOME | Verdadero inicio funcional: acciones rápidas locales, emitir, recibir/detectar y acceso separado a Mesh experimental | Sin efectos de emisión al componerse. Atrás desde Inicio conserva la salida normal de Android |
| `EmergencyTransmissionScreen` / EMERGENCY | Señalización local solicitada explícitamente al `EmergencyManager`, estado local y cancelación accesible | Se abre al pulsar una acción rápida. Detener, cerrar y Atrás paran recursos y vuelven a HOME. Pausa o recreación cancela y vuelve a HOME; no se reanuda una emisión automáticamente |
| `TransmitterScreen` / TRANSMIT | Editor, codificación y canal de señales/mensajes; reutiliza el mismo componente de ayuda rápida de Inicio | Acceso desde Inicio o barra. Botón Inicio o Atrás → HOME |
| `ReceiverScreen` / RECEIVE | Recepción por cámara/QR y acceso al detector de vibración | Acceso desde Inicio o barra. Inicio/Atrás → HOME |
| `VibrationDetectorScreen` / VIBRATION_DETECTOR | Detección táctil existente | Subruta de RECEIVE. Su regreso y Atrás → RECEIVE; botón global Inicio → HOME |
| `MeshScreen` / MESH | Chat BLE, participación experimental, laboratorio SOS estructurado y gateway existentes | Acceso separado desde Inicio/barra. Inicio/Atrás → HOME; la salida conserva pausa/cierre de radios y sesiones del módulo |
| SCAN, LIGHTMAP, OSCILLOSCOPE, SETTINGS | Escáner, radar óptico, osciloscopio y configuración existentes | Acceso por la barra avanzada. Inicio/Atrás → HOME |

La barra conserva los siete módulos y su orden: **Transmitir, Recibir, Escáner, Mesh, Radar óptico, Osciloscopio, Configuración**. En anchos pequeños se desplaza horizontalmente y lo indica con texto; no comprime siete objetivos táctiles y etiquetas en 360 dp. Cada módulo conserva sus controles internos. El botón superior «Volver a Inicio» ofrece una salida directa. Migrar la barra a otra jerarquía, agrupar módulos en menús nuevos o rehacer pantallas avanzadas requiere una solicitud adicional.

Las rutas funcionales se guardan con `rememberSaveable`. Restaurar WELCOME o EMERGENCY lleva a HOME: no repite una bienvenida interrumpida ni una señal que ya se detuvo. La bienvenida no es una ruta pública de regreso y EMERGENCY solo se abre con tipo/modo seleccionados explícitamente.

### Acciones compartidas y semántica de emergencia

`ui/components/QuickHelpActions.kt` es la única definición de los cuatro accesos rápidos existentes, usada por Inicio y Transmitir: **SOS**, **HELP/Auxilio**, **TRAPPED/Atrapado** y **DISCRETO**. Conservan tipo y modalidad: los tres primeros solicitan `EmergencyMode.ALL`; Discreto solicita SOS en `DISCREET`. Las pantallas no duplican selección de emisores, codificación ni `startEmergency`. Nuevas acciones locales se integran aquí después de definir su intención y alcance; no se agrega otra fila de botones a la bienvenida.

**Señalización local y SOS estructurado mesh son flujos distintos.** Activar ayuda rápida usa `EmergencyManager`; no crea un incidente SOS V1 ni publica al gateway. El laboratorio SOS estructurado se mantiene en Mesh Radar y sigue limitado a ensayos sintéticos/sin posición permitidos. No se transmite GNSS real por esta reorganización. La UI debe distinguir solicitud/emisión local, escritura de enlace, recepción local y confirmación del destino. No presentar una señal activada como mensaje entregado a un servicio de emergencias, atención o rescate. Inicio y emergencia lo dicen expresamente; los canales mostrados son **solicitados**, no una comprobación de su funcionamiento físico.

### Identidad institucional protegida

- `app/src/main/res/drawable/logo.png`: logo original BeaconChat. Se conserva el archivo sin cambios; no sustituirlo por un icono o dibujo generado. Nombre visible **BeaconChat**.
- `assets/vetrabyte-logo.webp`: logo original Vetrabyte. Su copia Android `app/src/main/res/drawable-nodpi/vetrabyte_logo.webp` debe ser idéntica byte por byte; SHA-256 `39c22d641f1ba4cd0535098b452119b703ca211875bf4428945b1ad61b58a33f`.
- Mostrar ambos con `ContentScale.Fit` y proporciones originales (BeaconChat 768:1408; Vetrabyte 790:316). Sin recorte, estiramiento, tintes, filtros, rediseño ni cambios en sus archivos originales. No convertir la bienvenida institucional en panel de acciones, propaganda de una modalidad o una pantalla de carga de red.

### Tamaños, accesibilidad y recursos

Inicio desplaza su contenido verticalmente; evita alturas fijas para texto y botones. El componente rápido usa dos columnas cuando caben y una con texto ampliado. Objetivos táctiles de al menos 48 dp, texto legible con contraste, imágenes con descripción y etiquetas semánticas para herramientas e Inicio. La pantalla de emergencia mantiene Detener y Cerrar fuera del contenido desplazable, también con poco alto disponible. Las nuevas pantallas deben respetar insets del sistema, 360/390 dp, horizontal y escalado de fuente; no resolver un desborde reduciendo artificialmente el texto del sistema.

Cada ruta funcional recibe un `LifecycleOwner` ligado a la actividad y destruido al salir de esa ruta. La cámara de recepción usa este propietario de pantalla, no la actividad completa. Se conservan los `DisposableEffect`, cancelaciones y pausas existentes de cada módulo; abandonar una pantalla no puede dejar emisiones, cámara o escaneo activos. Cambios de rutas llaman a la limpieza de emisores, y la emergencia sigue deteniéndose al salir/pausar. No modificar MeshEngine, GATT, fragmentación, SOS V1, gateway, permisos ni controladores Bluetooth para resolver una decisión visual.

### Revisión de futuras implementaciones

Verificar ruta de entrada y de regreso, ausencia de emisiones en Inicio, bienvenida sin acciones ni permisos, logos originales y ausencia de cambios funcionales de protocolo. Ejecutar `./gradlew testDebugUnitTest assembleDebug` con JDK compatible, `git diff --check` y pruebas de UI disponibles. `NavigationUiTest` prueba la estructura real de UX y su presentación con callbacks de módulos/emisores de ensayo: no sustituye una prueba de radio o sensores. Si hay Android autorizado, instalar por serial explícito y registrar las observaciones; no atribuir pruebas físicas a previews, mocks ni compilación. Mantener los resultados en esta guía; no crear un segundo contrato de UX.


### Validación de esta normalización

- Base `main`, HEAD `0d375d3`, con el cambio previo de `deploy-to-phone.sh` conservado. No se crearon ramas/worktrees ni se hizo commit/push.
- `JAVA_HOME=/home/lordcommander/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2 ./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest`: **BUILD SUCCESSFUL**, 235 tests JVM, 0 fallos/errores/omitidos; APK principal y APK instrumentada compiladas. Se mantienen advertencias anteriores de BuildConfig/Java 8/Gradle. Lint no ejecutado en esta iteración.
- Sin cambios de protocolos, `mesh/`, `sos/`, emisores, controladores ni manifiesto. Identidad BeaconChat y copia Vetrabyte verificadas contra originales. Contraste calculado del texto blanco sobre fondo principal de emergencia: mínimo 7:1 entre los tipos actuales; no equivale a una auditoría completa de accesibilidad.
- `NavigationUiTest`: nueve casos preparados para estructura institucional, cuatro acciones y salidas, siete rutas avanzadas y propietario de ciclo de vida, restauración sin repetición de bienvenida/emergencia, pausa/reanudación, 360/390 dp, fuente al 200 % y emergencia de alto reducido. Sus callbacks de módulos y emisores son de ensayo: no activan hardware ni demuestran BLE.
- Ejecución UI/inspección física: pendiente de reconexión ADB. Al comenzar se detectó Moto G51 5G `ZY32FJTWV8`, Android 12, 1080×2460 px, 400 dpi, fuente 1,0, rotación automática desactivada/rotación 0. Antes de instalar, el equipo desapareció de ADB; los intentos devolvieron `device not found`. No se atribuye a este dispositivo ningún resultado visual de la APK nueva hasta ejecutar las pruebas.
- Artefactos: `app/build/outputs/apk/debug/app-debug.apk`, APK de UI `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`, reportes JVM `app/build/reports/tests/testDebugUnitTest/`. Comando por serial para la UI, después de instalar ambas APK con `adb -s SERIAL install -r` (añadir `-t` para la APK de test): `adb -s SERIAL shell am instrument -w -r -e class com.nicobutter.beaconchat.NavigationUiTest com.nicobutter.beaconchat.test/androidx.test.runner.AndroidJUnitRunner`. Las capturas se guardan en `files/ux-checks/` del sandbox debug de la app y se pueden extraer con `adb -s SERIAL exec-out run-as com.nicobutter.beaconchat cat files/ux-checks/home-360.png`.
- No quedan propuestas visuales adicionales implementadas fuera de este contrato. Una sustitución de la barra avanzada, nuevos menús o rediseños de los módulos técnicos quedan como propuestas futuras que requieren solicitud expresa.


---

## Anexo: desarrollo en dispositivos reales

> Antecedente conservado para auditoría histórica; resultados declarados y condiciones de 2025 requieren reconfirmación. [Ver archivo original](https://github.com/NicoButter/beaconchat/blob/c364293f42823d863976a5608aede8d3da7a50d3/DESARROLLO.md).

### 📱 BeaconChat - Guía de Desarrollo con Dispositivo Real

## 🔧 Configuración Inicial

### 1. Preparar tu Celular Android

1. **Activar Opciones de Desarrollador**:
   - Ve a `Ajustes > Acerca del teléfono`
   - Toca 7 veces sobre `Número de compilación`
   - Verás el mensaje "Ahora eres desarrollador"

2. **Activar Depuración USB**:
   - Ve a `Ajustes > Opciones de desarrollador`
   - Activa `Depuración USB`
   - (Opcional) Activa `Instalación vía USB` para instalación más rápida

3. **Conectar al PC**:
   - Conecta tu celular con cable USB
   - Acepta el diálogo de "¿Permitir depuración USB?"
   - Marca "Permitir siempre desde este equipo"

### 2. Verificar Conexión

```bash
adb devices
```

Deberías ver algo como:
```
List of devices attached
ABC123DEF456    device
```

## 🚀 Métodos de Despliegue

### Opción 1: Script Completo (Recomendado)
Compila, instala y ejecuta en un solo comando:

```bash
./deploy-to-phone.sh
```

**Cuándo usar**: Cuando modificas código Kotlin

### Opción 2: Despliegue Rápido
Solo reinstala el APK ya compilado:

```bash
./quick-deploy.sh
```

**Cuándo usar**: Para probar el APK actual sin cambios

### Opción 3: Desde VS Code (MÁS RÁPIDO ⚡)

1. Presiona `Ctrl+Shift+P` (o `Cmd+Shift+P` en Mac)
2. Escribe "Run Task"
3. Selecciona:
   - **BeaconChat: Deploy to Phone (Full)** - Compilar e instalar
   - **BeaconChat: Quick Deploy (No Build)** - Solo instalar

O usa el atajo:
- `Ctrl+Shift+B` → Selecciona la tarea

## 📋 Tareas Disponibles en VS Code

| Tarea | Descripción | Atajo |
|-------|-------------|-------|
| Deploy to Phone (Full) | Compila, instala y ejecuta | `Ctrl+Shift+B` |
| Quick Deploy | Solo instala APK existente | - |
| Build APK Only | Solo compila sin instalar | - |
| Check Connected Devices | Lista dispositivos conectados | - |
| View Logcat | Ver logs en tiempo real | - |
| Uninstall from Phone | Desinstala la app | - |

## 🔍 Ver Logs en Tiempo Real

Para ver los logs mientras pruebas en tu celular:

```bash
adb logcat -c && adb logcat -s 'FlashlightController:*' 'SoundController:*' 'VibrationController:*' 'BeaconChat:*' '*:E'
```

O desde VS Code:
- Ejecuta la tarea **BeaconChat: View Logcat**

## 🐛 Solución de Problemas

### No se detecta el dispositivo

```bash
# Reiniciar servidor ADB
adb kill-server
adb start-server
adb devices
```

### Error de permisos en Linux

```bash
# Agregar reglas udev para Android
sudo usermod -a -G plugdev $USER
sudo apt-get install android-tools-adb
```

### El APK no se instala

```bash
# Desinstalar versión anterior primero
adb uninstall com.nicobutter.beaconchat

# Luego intentar instalar de nuevo
./deploy-to-phone.sh
```

### Ver información detallada del dispositivo

```bash
adb devices -l
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
```

## 🎯 Flujo de Trabajo Recomendado

1. **Desarrollo Inicial**:
   ```bash
   ./deploy-to-phone.sh
   ```

2. **Modificar código Kotlin** → Guardar → Ejecutar tarea `Deploy to Phone (Full)` en VS Code

3. **Probar en celular** → Ver logs con `View Logcat`

4. **Iterar rápidamente**:
   - Modifica código
   - `Ctrl+S` para guardar
   - `Ctrl+Shift+B` → Enter
   - La app se reinstala y abre automáticamente

## 📊 Monitoreo de Recursos

### Ver CPU y memoria
```bash
adb shell top -n 1 | grep beaconchat
```

### Ver batería consumida
```bash
adb shell dumpsys batterystats | grep beaconchat
```

## 🔥 Comandos Útiles

```bash
# Captura de pantalla
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Grabar pantalla (Ctrl+C para detener)
adb shell screenrecord /sdcard/demo.mp4
adb pull /sdcard/demo.mp4

# Reiniciar app
adb shell am force-stop com.nicobutter.beaconchat
adb shell am start -n com.nicobutter.beaconchat/.MainActivity

# Limpiar datos de la app
adb shell pm clear com.nicobutter.beaconchat
```

## ⚡ Desarrollo Ultra-Rápido

Para máxima velocidad, deja abierto un terminal ejecutando:

```bash
# Terminal 1: Compilación automática cuando guardas archivos
while true; do
    inotifywait -e modify -r app/src/
    ./deploy-to-phone.sh
done
```

Cada vez que guardes un archivo, automáticamente:
1. ✅ Compila el código
2. ✅ Instala en tu celular
3. ✅ Abre la app

---

**¡Listo para desarrollar en tiempo real! 🚀📱**


---

## Anexo: guía rápida de despliegue

> Antecedente conservado para auditoría histórica; resultados declarados y condiciones de 2025 requieren reconfirmación. [Ver archivo original](https://github.com/NicoButter/beaconchat/blob/c364293f42823d863976a5608aede8d3da7a50d3/QUICKSTART.md).

### 🚀 BeaconChat - Guía Rápida de Despliegue

## ✅ Dispositivo Detectado
Tu celular está conectado y listo para desarrollo en tiempo real.

## 🎯 3 Formas de Desplegar

### 1️⃣ Makefile (MÁS FÁCIL)
```bash
make deploy    # Compila e instala
make quick     # Solo instala
make watch     # Auto-despliegue al guardar
make logs      # Ver logs en tiempo real
make help      # Ver todos los comandos
```

### 2️⃣ Scripts Directos
```bash
./deploy-to-phone.sh   # Despliegue completo
./quick-deploy.sh      # Reinstalación rápida
./auto-deploy.sh       # Modo automático
```

### 3️⃣ VS Code (RECOMENDADO)
1. `Ctrl + Shift + B` → Ejecutar tarea de build
2. O `Ctrl + Shift + P` → "Run Task" → "BeaconChat: Deploy to Phone"

## ⚡ Desarrollo Ultra-Rápido

### Opción A: Automático con Watch
```bash
make watch
```
Cada vez que guardes un archivo `.kt`, automáticamente:
- ✅ Compila el código
- ✅ Instala en tu celular  
- ✅ Abre la app

### Opción B: Manual con Makefile
```bash
# Editas código en VS Code
# Ctrl+S para guardar
# En terminal:
make deploy
```

### Opción C: Desde VS Code
```bash
# Editas código
# Ctrl+S
# Ctrl+Shift+B → Enter
```

## 📱 Comandos Útiles

```bash
make devices      # Ver dispositivos conectados
make logs         # Ver logs en tiempo real
make restart      # Reiniciar app
make screenshot   # Captura de pantalla
make uninstall    # Desinstalar app
make clean        # Limpiar proyecto
```

## 🐛 Ver Logs Mientras Desarrollas

En una terminal aparte:
```bash
make logs
```

Verás en tiempo real lo que pasa en tu celular:
- FlashlightController logs
- SoundController logs
- VibrationController logs
- Errores de la app

## 🎬 Flujo de Trabajo Recomendado

### Setup Inicial (una vez)
```bash
# Terminal 1: Editor (VS Code ya está abierto)
# Terminal 2: Logs
make logs

# Terminal 3: Auto-deploy (opcional)
make watch
```

### Desarrollo Normal
1. Edita código en VS Code
2. Guarda (`Ctrl+S`)
3. Si usas `make watch`: ¡Listo! Se despliega automáticamente
4. Si no: `make deploy` en terminal

### Testing Rápido
```bash
make deploy    # Primera vez
# Pruebas en celular
# Encuentras bug
# Corriges código
make deploy    # Despliegue de la corrección
```

## 🎨 Ejemplo de Sesión de Desarrollo

```bash
# 1. Abrir proyecto
cd /home/lordcommander/proyectos_2024/beaconchat

# 2. Ver dispositivos
make devices

# 3. Desplegar versión actual
make deploy

# 4. Activar logs en otra terminal
make logs

# 5. Activar auto-deploy en otra terminal (opcional)
make watch

# 6. Desarrollar en VS Code
# Los cambios se despliegan automáticamente si usas watch
# O ejecutas make deploy manualmente
```

## 🔥 Tips Pro

### Compilación más rápida
```bash
# Solo compilar sin instalar
make build

# Instalar el APK ya compilado
make quick
```

### Debug interactivo
```bash
# Ver logs filtrados de tu componente
adb logcat | grep "FlashlightController"

# Ver solo errores
adb logcat *:E

# Limpiar logs y ver frescos
adb logcat -c && adb logcat
```

### Problemas de conexión
```bash
# Reiniciar ADB
adb kill-server && adb start-server

# Ver info del dispositivo
adb shell getprop ro.product.model
```

---

**¡Ya estás listo para desarrollar en tiempo real! 🚀**

Recomendación: Usa `make watch` y deja que el sistema haga el trabajo por ti.
