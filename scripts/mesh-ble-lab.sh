#!/usr/bin/env bash
# Explicit serials only. Does not change radios, permissions or delete application data.
set -euo pipefail
cd "$(dirname "$0")/.."

usage() {
    cat <<'USAGE'
Usage:
  bash scripts/mesh-ble-lab.sh inventory SERIAL [SERIAL...]
  bash scripts/mesh-ble-lab.sh install SERIAL [SERIAL...]
  bash scripts/mesh-ble-lab.sh capture SERIAL OUTPUT.log [SECONDS=180]
  bash scripts/mesh-ble-lab.sh summary INPUT.log

Build the debug APK with JDK 21 first. Capture records only BeaconMesh metadata.
Enable Bluetooth, required permissions and mesh participation manually in the foreground.
USAGE
}

action=${1:---help}
if [[ "$action" == --help || "$action" == -h ]]; then usage; exit 0; fi
shift
case "$action" in
    inventory|install|capture)
        if (( $# == 0 )); then usage >&2; exit 2; fi
        command -v adb >/dev/null
        ;;
    summary)
        if (( $# != 1 )); then usage >&2; exit 2; fi
        exec python3 scripts/mesh-ble-summary.py "$1"
        ;;
    *) usage >&2; exit 2 ;;
esac

require_device() {
    if [[ -z "$1" || "$1" == -* ]]; then echo 'Explicit ADB serial required' >&2; exit 2; fi
    if [[ $(adb -s "$1" get-state) != device ]]; then
        echo "Device is unavailable or unauthorized: $1" >&2; exit 1
    fi
}

if [[ "$action" == capture ]]; then
    if (( $# < 2 || $# > 3 )); then usage >&2; exit 2; fi
    serial=$1
    output=$2
    seconds=${3:-180}
    if [[ ! "$seconds" =~ ^[1-9][0-9]{0,3}$ ]]; then echo 'SECONDS must be 1..9999' >&2; exit 2; fi
    require_device "$serial"
    # -T 1 starts with the latest line, without clearing the user's global logcat buffer.
    set +e
    timeout --signal=INT "$seconds" adb -s "$serial" logcat -v epoch -T 1 BeaconMesh:D '*:S' > "$output"
    result=$?
    set -e
    if [[ "$result" != 0 && "$result" != 124 ]]; then exit "$result"; fi
    echo "Captured BeaconMesh metadata for $serial in $output"
    exit 0
fi

apk=app/build/outputs/apk/debug/app-debug.apk
if [[ "$action" == install ]]; then
    if [[ ! -f "$apk" ]]; then echo 'Build assembleDebug first' >&2; exit 1; fi
    sha256sum "$apk"
fi
# Validate all requested serials before installing on the first device.
for serial in "$@"; do require_device "$serial"; done
for serial in "$@"; do
    if [[ "$action" == install ]]; then
        adb -s "$serial" install -r "$apk"
    else
        echo "serial=$serial"
        echo "host_utc=$(date -u +%FT%TZ)"
        echo "manufacturer=$(adb -s "$serial" shell getprop ro.product.manufacturer | tr -d '\r')"
        echo "model=$(adb -s "$serial" shell getprop ro.product.model | tr -d '\r')"
        echo "android=$(adb -s "$serial" shell getprop ro.build.version.release | tr -d '\r')"
        echo "api=$(adb -s "$serial" shell getprop ro.build.version.sdk | tr -d '\r')"
        adb -s "$serial" shell dumpsys package com.nicobutter.beaconchat | sed -n '/versionCode=/p; /versionName=/p'
    fi
done
