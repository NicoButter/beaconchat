#!/bin/bash

# Script de despliegue automático a dispositivo Android
# BeaconChat - Deploy to Phone

set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

echo "🚀 BeaconChat - Despliegue a dispositivo Android"
echo "================================================"

# Respetar JAVA_HOME si contiene un JDK 21; buscar alternativas instaladas si no.
selected_java_home=
for java_candidate in "${JAVA_HOME:-}" \
    /usr/lib/jvm/java-21-openjdk /usr/lib/jvm/java-21-openjdk-amd64 \
    /usr/lib/jvm/java-21-openjdk-* /usr/lib/jvm/jdk-21* \
    "${GRADLE_USER_HOME:-$HOME/.gradle}"/jdks/*; do
    [[ -x "$java_candidate/bin/java" && -x "$java_candidate/bin/javac" ]] || continue
    java_version=$("$java_candidate/bin/java" -version 2>&1) || continue
    case "$java_version" in
        *'version "21.'*|*'version "21"'*|*'version "21-'*)
            selected_java_home=$java_candidate
            break
            ;;
    esac
done
if [[ -z "$selected_java_home" ]]; then
    echo '❌ No se encontró un JDK 21 válido. Configurá JAVA_HOME con su directorio de instalación.' >&2
    exit 1
fi
export JAVA_HOME="$selected_java_home"
export PATH="$JAVA_HOME/bin:$PATH"
echo "☕ JDK 21: $JAVA_HOME"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}📱 Verificando conexión con dispositivo...${NC}"
device_list=$(adb devices)
mapfile -t devices < <(awk 'NR > 1 && $2 == "device" { print $1 }' <<< "$device_list")
if (( ${#devices[@]} == 0 )); then
    echo -e "${RED}❌ No se detectó ningún dispositivo Android conectado${NC}"
    echo "Por favor:"
    echo "  1. Conecta tu celular por USB"
    echo "  2. Activa 'Depuración USB' en Opciones de Desarrollador"
    echo "  3. Acepta la autorización en tu celular"
    exit 1
fi

if (( $# > 1 )); then
    echo "Uso: $0 [SERIAL_ADB]" >&2
    exit 1
fi
DEVICE=${1:-${ANDROID_SERIAL:-}}
if [[ -z "$DEVICE" ]]; then
    if (( ${#devices[@]} > 1 )); then
        echo "Hay varios dispositivos. Usá: $0 SERIAL_ADB" >&2
        printf '  %s\n' "${devices[@]}" >&2
        exit 1
    fi
    DEVICE=${devices[0]}
fi
if [[ "$DEVICE" == -* ]] || [[ $(adb -s "$DEVICE" get-state) != device ]]; then
    echo "❌ Dispositivo no disponible o sin autorización: $DEVICE" >&2
    exit 1
fi
echo -e "${GREEN}✓ Dispositivo detectado: $DEVICE${NC}"

echo -e "\n${BLUE}🔨 Compilando APK...${NC}"
if ! ./gradlew assembleDebug; then
    echo -e "${RED}❌ Error en la compilación${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Compilación exitosa${NC}"

echo -e "\n${BLUE}📦 Instalando en dispositivo...${NC}"
# Actualizar conservando preferencias e identidad mesh; un fallo no debe borrar datos.
if ! adb -s "$DEVICE" install -r app/build/outputs/apk/debug/app-debug.apk; then
    echo -e "${RED}❌ Error en la instalación; se conservó la aplicación existente${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Instalación exitosa${NC}"

echo -e "\n${BLUE}📦 Copiando APK a la raíz del proyecto...${NC}"
# Crear directorio releases si no existe
mkdir -p releases

# Copiar APK con timestamp
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
APK_NAME="BeaconChat_${TIMESTAMP}.apk"
cp app/build/outputs/apk/debug/app-debug.apk "releases/${APK_NAME}"

# También mantener una copia como "latest"
cp app/build/outputs/apk/debug/app-debug.apk "releases/BeaconChat_latest.apk"

echo -e "${GREEN}✓ APK guardado en:${NC}"
echo -e "  ${YELLOW}releases/${APK_NAME}${NC}"
echo -e "  ${YELLOW}releases/BeaconChat_latest.apk${NC}"

echo -e "\n${BLUE}🚀 Iniciando BeaconChat en el dispositivo...${NC}"
adb -s "$DEVICE" shell am start -n com.nicobutter.beaconchat/.MainActivity

echo -e "\n${GREEN}✅ ¡Despliegue completado exitosamente!${NC}"
echo -e "${YELLOW}📱 BeaconChat ahora está ejecutándose en tu celular${NC}"
