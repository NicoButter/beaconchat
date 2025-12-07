.PHONY: help deploy quick clean-deploy build devices logs clean uninstall watch apks

# Configuración de Java
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH:=$(JAVA_HOME)/bin:$(PATH)

help: ## Muestra esta ayuda
	@echo "BeaconChat - Comandos Disponibles:"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
	@echo ""

deploy: ## Compila, instala y ejecuta en celular
	@./deploy-to-phone.sh

quick: ## Reinstala APK sin compilar
	@./quick-deploy.sh

clean-deploy: ## Elimina app anterior y hace instalación limpia
	@./clean-deploy.sh

build: ## Solo compila el APK
	@./gradlew assembleDebug

devices: ## Lista dispositivos conectados
	@adb devices -l

logs: ## Muestra logs en tiempo real
	@adb logcat -c && adb logcat -s 'FlashlightController:*' 'SoundController:*' 'VibrationController:*' 'BeaconChat:*' 'MainActivity:*' '*:E'

clean: ## Limpia proyecto
	@./gradlew clean

uninstall: ## Desinstala app del celular
	@adb uninstall com.nicobutter.beaconchat || true

watch: ## Modo desarrollo automático
	@./auto-deploy.sh

restart: ## Reinicia la app en el celular
	@adb shell am force-stop com.nicobutter.beaconchat
	@adb shell am start -n com.nicobutter.beaconchat/.MainActivity
	@echo "✅ App reiniciada"

screenshot: ## Toma captura de pantalla
	@adb shell screencap -p /sdcard/screenshot.png
	@adb pull /sdcard/screenshot.png ./screenshot_$(shell date +%Y%m%d_%H%M%S).png
	@echo "✅ Captura guardada"

install-tools: ## Instala herramientas necesarias
	@echo "Instalando herramientas de desarrollo Android..."
	@sudo dnf install -y android-tools inotify-tools
	@echo "✅ Herramientas instaladas"

apks: ## Lista APKs generados
	@echo "📦 APKs Disponibles:"
	@echo ""
	@if [ -d "releases" ]; then \
		ls -lht releases/*.apk 2>/dev/null | awk '{printf "  %s %s  \033[36m%-30s\033[0m (%s)\n", $$6, $$7, $$9, $$5}' || echo "  No hay APKs generados aún"; \
	else \
		echo "  No hay APKs generados aún"; \
		echo "  Ejecuta 'make deploy' para crear uno"; \
	fi
	@echo ""
