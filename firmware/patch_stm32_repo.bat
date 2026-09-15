@echo off
REM Script untuk menambal source R9 ke repository Firmware_CDI_NS200 (STM32WB55) di Windows
setlocal enabledelayedexpansion

if "%~1"=="" (
    echo Penggunaan: patch_stm32_repo.bat ^<path_folder_Firmware_CDI_NS200^>
    echo Contoh: patch_stm32_repo.bat ..\Firmware_CDI_NS200
    exit /b 1
)

set "DEST_DIR=%~1"

if not exist "%DEST_DIR%\CMakeLists.txt" (
    echo ERROR: Folder "%DEST_DIR%" bukan root proyek Firmware_CDI_NS200!
    exit /b 1
)

set "SCRIPT_DIR=%~dp0"
set "COMMON_INC=%SCRIPT_DIR%common\include\cdi_firmware.h"
set "COMMON_SRC=%SCRIPT_DIR%common\src\cdi_firmware.c"
set "PORT_SRC=%SCRIPT_DIR%stm32wb55\Core\Src\cdi_port.c"
set "PORT_INC=%SCRIPT_DIR%stm32wb55\Core\Inc\cdi_port.h"

echo === Menambal Firmware STM32WB55 ke Versi R9 ===

copy /Y "%PORT_SRC%" "%DEST_DIR%\Core\Src\cdi_port.c" >nul
copy /Y "%PORT_INC%" "%DEST_DIR%\Core\Inc\cdi_port.h" >nul
echo [OK] Core/Src/cdi_port.c dan Core/Inc/cdi_port.h berhasil diperbarui.

if exist "%DEST_DIR%\CDI" (
    if not exist "%DEST_DIR%\CDI\Inc" mkdir "%DEST_DIR%\CDI\Inc"
    if not exist "%DEST_DIR%\CDI\Src" mkdir "%DEST_DIR%\CDI\Src"
    copy /Y "%COMMON_INC%" "%DEST_DIR%\CDI\Inc\cdi_firmware.h" >nul
    copy /Y "%COMMON_SRC%" "%DEST_DIR%\CDI\Src\cdi_firmware.c" >nul
    copy /Y "%COMMON_INC%" "%DEST_DIR%\CDI\cdi_firmware.h" >nul 2>&1
    copy /Y "%COMMON_SRC%" "%DEST_DIR%\CDI\cdi_firmware.c" >nul 2>&1
) else (
    copy /Y "%COMMON_INC%" "%DEST_DIR%\Core\Inc\cdi_firmware.h" >nul
    copy /Y "%COMMON_SRC%" "%DEST_DIR%\Core\Src\cdi_firmware.c" >nul
)
echo [OK] cdi_firmware.c dan cdi_firmware.h (Core Engine R9) berhasil disalin.

echo === SELESAI: Repository STM32 siap dibuild dengan cmake dan ninja! ===
