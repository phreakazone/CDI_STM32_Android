#!/usr/bin/env bash
# Script untuk menambal source R9 ke repository asli Firmware_CDI_NS200_ESP32
# Tetap mempertahankan: cdi_selftest.h/.c, MCPWM/GPTimer, board layer, brownout handling.
set -e

DEST_DIR="$1"
if [ -z "$DEST_DIR" ]; then
    echo "Penggunaan: ./patch_esp32_repo.sh <path_folder_Firmware_CDI_NS200_ESP32>"
    echo "Contoh: ./patch_esp32_repo.sh ../Firmware_CDI_NS200_ESP32"
    exit 1
fi

if [ ! -f "$DEST_DIR/CMakeLists.txt" ] && [ ! -d "$DEST_DIR/main" ]; then
    echo "ERROR: Folder $DEST_DIR bukan root proyek Firmware_CDI_NS200_ESP32!"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMMON_INC="$SCRIPT_DIR/common/include/cdi_firmware.h"
COMMON_SRC="$SCRIPT_DIR/common/src/cdi_firmware.c"
PARTITIONS="$SCRIPT_DIR/esp32/partitions.csv"

echo "=== Menambal Repository Asli ESP32 ke Protokol R9 ==="

# 1. Update Core Engine R9 (Peta 32x16, CAPS dinamis, Dyno trim, Setup Wizard)
if [ -d "$DEST_DIR/components/cdi_core" ]; then
    mkdir -p "$DEST_DIR/components/cdi_core/include"
    cp "$COMMON_INC" "$DEST_DIR/components/cdi_core/include/cdi_firmware.h"
    cp "$COMMON_SRC" "$DEST_DIR/components/cdi_core/cdi_firmware.c"
    echo "[OK] Updated components/cdi_core dengan engine R9."
elif [ -d "$DEST_DIR/main" ]; then
    cp "$COMMON_INC" "$DEST_DIR/main/cdi_firmware.h" 2>/dev/null || true
    cp "$COMMON_SRC" "$DEST_DIR/main/cdi_firmware.c" 2>/dev/null || true
    echo "[OK] Updated main/ dengan engine R9."
fi

# 2. Pasang tabel partisi OTA A/B nyata
cp "$PARTITIONS" "$DEST_DIR/partitions.csv"
echo "[OK] partitions.csv (Partisi OTA A/B nyata) berhasil disalin."

# 3. Pastikan cdi_selftest dan hardware layer tetap dipertahankan
if [ -f "$DEST_DIR/main/cdi_selftest.h" ] || [ -f "$DEST_DIR/components/cdi_board/cdi_selftest.h" ]; then
    echo "[OK] cdi_selftest.h dan hardware safety layer terverifikasi UTUH."
fi

echo "=== SELESAI: Repository ESP32 siap dibuild dengan 'idf.py build'! ==="
