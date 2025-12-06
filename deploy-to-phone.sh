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
adb install -r app/build/outputs/apk/debug/app-debug.apk

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error en la instalación${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Instalación exitosa${NC}"

echo -e "\n${BLUE}🚀 Iniciando BeaconChat en el dispositivo...${NC}"
adb shell am start -n com.nicobutter.beaconchat/.MainActivity

echo -e "\n${GREEN}✅ ¡Despliegue completado exitosamente!${NC}"
echo -e "${YELLOW}📱 BeaconChat ahora está ejecutándose en tu celular${NC}"
