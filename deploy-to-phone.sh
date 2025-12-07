#!/bin/bash

# Script de despliegue automático a dispositivo Android
# BeaconChat - Deploy to Phone

set -e  # Detener si hay error

echo "🚀 BeaconChat - Despliegue a dispositivo Android"
echo "================================================"

# Configurar Java 21
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}📱 Verificando conexión con dispositivo...${NC}"
if ! adb devices | grep -q "device$"; then
    echo -e "${RED}❌ No se detectó ningún dispositivo Android conectado${NC}"
    echo "Por favor:"
    echo "  1. Conecta tu celular por USB"
    echo "  2. Activa 'Depuración USB' en Opciones de Desarrollador"
    echo "  3. Acepta la autorización en tu celular"
    exit 1
fi

DEVICE=$(adb devices | grep "device$" | head -1 | awk '{print $1}')
echo -e "${GREEN}✓ Dispositivo detectado: $DEVICE${NC}"

echo -e "\n${BLUE}🔨 Compilando APK...${NC}"
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error en la compilación${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Compilación exitosa${NC}"

echo -e "\n${BLUE}📦 Instalando en dispositivo...${NC}"
# Primero intentamos sobrescribir la app existente
adb install -r app/build/outputs/apk/debug/app-debug.apk 2>/dev/null

if [ $? -ne 0 ]; then
    echo -e "${YELLOW}⚠ No se pudo sobrescribir, eliminando versión anterior...${NC}"
    adb uninstall com.nicobutter.beaconchat 2>/dev/null
    echo -e "${YELLOW}↻ Instalando versión nueva...${NC}"
    adb install app/build/outputs/apk/debug/app-debug.apk
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Error en la instalación${NC}"
        exit 1
    fi
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
adb shell am start -n com.nicobutter.beaconchat/.MainActivity

echo -e "\n${GREEN}✅ ¡Despliegue completado exitosamente!${NC}"
echo -e "${YELLOW}📱 BeaconChat ahora está ejecutándose en tu celular${NC}"
