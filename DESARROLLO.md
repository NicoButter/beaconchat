# 📱 BeaconChat - Guía de Desarrollo con Dispositivo Real

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
