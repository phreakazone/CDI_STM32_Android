#!/usr/bin/env bash
# Script untuk menambal source R9 ke repository Firmware_CDI_NS200 (STM32WB55)
set -e

DEST_DIR="$1"
if [ -z "$DEST_DIR" ]; then
    echo "Penggunaan: ./patch_stm32_repo.sh <path_folder_Firmware_CDI_NS200>"
    echo "Contoh: ./patch_stm32_repo.sh ../Firmware_CDI_NS200"
    exit 1
fi

if [ ! -f "$DEST_DIR/CMakeLists.txt" ]; then
    echo "ERROR: Folder $DEST_DIR bukan root proyek Firmware_CDI_NS200!"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMMON_INC="$SCRIPT_DIR/common/include/cdi_firmware.h"
COMMON_SRC="$SCRIPT_DIR/common/src/cdi_firmware.c"
PORT_SRC="$SCRIPT_DIR/stm32wb55/Core/Src/cdi_port.c"
PORT_INC="$SCRIPT_DIR/stm32wb55/Core/Inc/cdi_port.h"

echo "=== Menambal Firmware STM32WB55 ke Versi R9 ==="

# 1. Salin cdi_port.c & cdi_port.h
cp "$PORT_SRC" "$DEST_DIR/Core/Src/cdi_port.c"
cp "$PORT_INC" "$DEST_DIR/Core/Inc/cdi_port.h"
echo "[OK] Core/Src/cdi_port.c & Core/Inc/cdi_port.h berhasil diperbarui."

# 2. Salin cdi_firmware.h & cdi_firmware.c ke lokasi yang sesuai di target repo
if [ -d "$DEST_DIR/CDI" ]; then
    mkdir -p "$DEST_DIR/CDI/Inc" "$DEST_DIR/CDI/Src"
    cp "$COMMON_INC" "$DEST_DIR/CDI/Inc/cdi_firmware.h" 2>/dev/null || cp "$COMMON_INC" "$DEST_DIR/CDI/cdi_firmware.h"
    cp "$COMMON_SRC" "$DEST_DIR/CDI/Src/cdi_firmware.c" 2>/dev/null || cp "$COMMON_SRC" "$DEST_DIR/CDI/cdi_firmware.c"
else
    cp "$COMMON_INC" "$DEST_DIR/Core/Inc/cdi_firmware.h"
    cp "$COMMON_SRC" "$DEST_DIR/Core/Src/cdi_firmware.c"
fi
echo "[OK] cdi_firmware.c & cdi_firmware.h (Core Engine R9) berhasil disalin."

echo "=== SELESAI: Repository STM32 siap dibuild dengan cmake & ninja! ==="
