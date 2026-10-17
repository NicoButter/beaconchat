# Desarrollo y despliegue de BeaconChat

La ruta de referencia recomendada es `./gradlew testDebugUnitTest assembleDebug` con JDK 17/21. Algunos scripts históricos utilizan rutas Fedora específicas. Se unifican aquí las guías de desarrollo y arranque rápido.


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
