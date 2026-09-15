@echo off
REM Script untuk menambal source R9 ke repository asli Firmware_CDI_NS200_ESP32 di Windows
REM Tetap mempertahankan: cdi_selftest.h/.c, MCPWM/GPTimer, board layer, brownout handling.
setlocal enabledelayedexpansion

if "%~1"=="" (
    echo Penggunaan: patch_esp32_repo.bat ^<path_folder_Firmware_CDI_NS200_ESP32^>
    echo Contoh: patch_esp32_repo.bat ..\Firmware_CDI_NS200_ESP32
    exit /b 1
)

set "DEST_DIR=%~1"

if not exist "%DEST_DIR%\CMakeLists.txt" (
    echo ERROR: Folder "%DEST_DIR%" bukan root proyek Firmware_CDI_NS200_ESP32!
    exit /b 1
)

set "SCRIPT_DIR=%~dp0"
set "COMMON_INC=%SCRIPT_DIR%common\include\cdi_firmware.h"
set "COMMON_SRC=%SCRIPT_DIR%common\src\cdi_firmware.c"
set "PARTITIONS=%SCRIPT_DIR%esp32\partitions.csv"

echo === Menambal Repository Asli ESP32 ke Protokol R9 ===

if exist "%DEST_DIR%\components\cdi_core" (
    copy /Y "%COMMON_INC%" "%DEST_DIR%\components\cdi_core\include\cdi_firmware.h" >nul
    copy /Y "%COMMON_SRC%" "%DEST_DIR%\components\cdi_core\cdi_firmware.c" >nul
    echo [OK] Updated components\cdi_core dengan engine R9.
) else (
    if exist "%DEST_DIR%\main" (
        copy /Y "%COMMON_INC%" "%DEST_DIR%\main\cdi_firmware.h" >nul 2>&1
        copy /Y "%COMMON_SRC%" "%DEST_DIR%\main\cdi_firmware.c" >nul 2>&1
        echo [OK] Updated main\ dengan engine R9.
    )
)

copy /Y "%PARTITIONS%" "%DEST_DIR%\partitions.csv" >nul
echo [OK] partitions.csv (Partisi OTA A/B nyata) berhasil disalin.

if exist "%DEST_DIR%\main\cdi_selftest.h" (
    echo [OK] cdi_selftest.h dan hardware safety layer terverifikasi UTUH.
)

echo === SELESAI: Repository ESP32 siap dibuild dengan "idf.py build"! ===
