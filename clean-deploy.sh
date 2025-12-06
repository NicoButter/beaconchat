#!/bin/bash

# Script de despliegue limpio (elimina versión anterior)
# BeaconChat - Clean Deploy

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

echo "🧹 BeaconChat - Despliegue Limpio"
echo "=================================="

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}📱 Verificando conexión con dispositivo...${NC}"
if ! adb devices | grep -q "device$"; then
    echo -e "${RED}❌ No se detectó ningún dispositivo Android${NC}"
    exit 1
fi

DEVICE=$(adb devices | grep "device$" | head -1 | awk '{print $1}')
echo -e "${GREEN}✓ Dispositivo detectado: $DEVICE${NC}"

# Eliminar versión anterior completamente
echo -e "\n${YELLOW}🗑️  Eliminando versión anterior...${NC}"
adb uninstall com.nicobutter.beaconchat 2>/dev/null || echo -e "${YELLOW}No había versión anterior instalada${NC}"

echo -e "\n${BLUE}🔨 Compilando APK...${NC}"
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error en la compilación${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Compilación exitosa${NC}"

echo -e "\n${BLUE}📦 Instalando versión nueva...${NC}"
adb install app/build/outputs/apk/debug/app-debug.apk

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error en la instalación${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Instalación exitosa${NC}"

echo -e "\n${BLUE}🚀 Iniciando BeaconChat en el dispositivo...${NC}"
adb shell am start -n com.nicobutter.beaconchat/.MainActivity

echo -e "\n${GREEN}✅ ¡Despliegue limpio completado!${NC}"
