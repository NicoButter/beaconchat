#!/bin/bash

# Script de reinstalación rápida (sin recompilar)
# BeaconChat - Quick Deploy

set -e

echo "⚡ BeaconChat - Reinstalación Rápida"
echo "===================================="

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}📦 Reinstalando APK existente...${NC}"
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo -e "${BLUE}🚀 Iniciando aplicación...${NC}"
adb shell am start -n com.nicobutter.beaconchat/.MainActivity

echo -e "${GREEN}✅ ¡Listo!${NC}"
