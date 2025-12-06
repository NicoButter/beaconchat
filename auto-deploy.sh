#!/bin/bash

# Script de desarrollo automático - detecta cambios y despliega
# BeaconChat - Auto Deploy on Save

echo "👁️  BeaconChat - Modo Desarrollo Automático"
echo "==========================================="
echo ""
echo "Vigilando cambios en el código..."
echo "Presiona Ctrl+C para detener"
echo ""

# Verificar si inotify-tools está instalado
if ! command -v inotifywait &> /dev/null; then
    echo "⚠️  inotify-tools no está instalado"
    echo "Instalando..."
    sudo dnf install -y inotify-tools
fi

# Contador de despliegues
DEPLOY_COUNT=0

while true; do
    # Esperar cambios en archivos .kt
    inotifywait -q -e modify,create -r app/src/main/java/ --include '\.kt$'
    
    DEPLOY_COUNT=$((DEPLOY_COUNT + 1))
    echo ""
    echo "🔄 Cambio detectado! Desplegando... (#$DEPLOY_COUNT)"
    echo "----------------------------------------"
    
    # Ejecutar despliegue completo
    if ./deploy-to-phone.sh; then
        echo "✅ Despliegue #$DEPLOY_COUNT completado"
    else
        echo "❌ Error en despliegue #$DEPLOY_COUNT"
    fi
    
    echo ""
    echo "👁️  Esperando más cambios..."
    echo ""
    
    # Pequeña pausa para evitar múltiples despliegues
    sleep 2
done
