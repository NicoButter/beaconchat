# 🚀 BeaconChat - Guía Rápida de Despliegue

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
