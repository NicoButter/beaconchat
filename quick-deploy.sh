#!/bin/bash

# Script de reinstalación rápida (sin recompilar)
# BeaconChat - Quick Deploy

set -e

echo "⚡ BeaconChat - Reinstalación Rápida"
echo "===================================="

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}📦 Reinstalando APK existente...${NC}"
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

echo -e "${BLUE}🚀 Iniciando aplicación...${NC}"
adb shell am start -n com.nicobutter.beaconchat/.MainActivity

echo -e "${GREEN}✅ ¡Listo!${NC}"
