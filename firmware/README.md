# Panduan Build Firmware CDI R9 (STM32WB55 & ESP32)

### Konvensi Identitas Bluetooth BLE Permanen
Nama broadcast BLE (*Device Advertising Name*) telah distandardisasi menjadi:
```c
#define DEVICE_NAME "NS200-CDI"
```
Tanpa embel-embel kode versi (`R7`, `R8`, dll.), baik di firmware ESP32 (`main/cdi_ble.c`) maupun STM32WB55. Aplikasi Android memindai dan mengenali perangkat ini secara otomatis.

---

Perlu dipahami:
- Repository di GitHub (`phreakazone/Firmware_CDI_NS200` dan `phreakazone/Firmware_CDI_NS200_ESP32`) telah ditingkatkan ke **Engine R9 v5.0**:
  - `CDI_R5_RPM_POINTS = 32` dan `CDI_R5_TPS_POINTS = 16` (Grid 32x16).
  - `CDI_R5_STORE_VERSION = 5`.
  - Dukungan penuh Dyno Live Trim, Profil Universal, kalibrasi suhu NTC 3-titik, dan partisi dual OTA A/B.
- Kode lapisan bersama R9 ada di folder `/firmware` pada repository Android ini:
  - `firmware/common/include/cdi_firmware.h` (Data model R9 & Protocol v5)
  - `firmware/common/src/cdi_firmware.c` (Core Engine R9, Dyno Trim, Setup Wizard, Strobe, Map 32x16)
  - `firmware/esp32/` (Port ESP32 mandiri dengan NimBLE R9 UUIDs & Partisi OTA)
  - `firmware/stm32wb55/Core/` (Port STM32WB55 R9)

---

## 1. Build ESP32-WROOM-32 (R9)

Ada 2 cara:

### Cara A (Paling Cepat - Langsung dari Folder ini):
Folder `firmware/esp32` sudah merupakan proyek ESP-IDF lengkap dan mandiri (berisi `CMakeLists.txt`, `sdkconfig.defaults`, `partitions.csv` dengan OTA slot, driver NimBLE BLE R9, dan `cdi_firmware.c` R9).

```bash
cd firmware/esp32
idf.py set-target esp32
idf.py build
idf.py -p COM7 flash monitor
```

### Cara B (Menambal ke repo `Firmware_CDI_NS200_ESP32`):
Jika Anda ingin tetap memakai repo hasil clone `phreakazone/Firmware_CDI_NS200_ESP32`:
1. Clone repo:
   ```bash
   git clone https://github.com/phreakazone/Firmware_CDI_NS200_ESP32.git
   ```
2. Timpa file R9 dari folder ini ke repo tersebut:
   - Salin `firmware/common/include/cdi_firmware.h` ke `Firmware_CDI_NS200_ESP32/components/cdi_core/include/` (atau folder include terkait).
   - Salin `firmware/common/src/cdi_firmware.c` ke folder source terkait.
   - Salin `firmware/esp32/main/cdi_ble.c` dan `cdi_ble.h` ke `main/`.
   - Salin `firmware/esp32/main/cdi_port.c` ke `main/`.
   - Salin `firmware/esp32/partitions.csv` ke root folder repo.
3. Build & Flash:
   ```bash
   idf.py set-target esp32
   idf.py build
   idf.py -p COM7 flash monitor
   ```

---

## 2. Build STM32WB55 (R9)

STM32WB55 memerlukan paket CMSIS/HAL vendor ST, linker script, startup, dan WPAN BLE stack (Cortex-M0+) yang ada di repo `phreakazone/Firmware_CDI_NS200`.

### Langkah-langkah:
1. Clone repository lengkap:
   ```bash
   git clone https://github.com/phreakazone/Firmware_CDI_NS200.git
   ```
2. Terapkan patch R9 dari folder `/firmware` ini:
   - **Linux / macOS**:
     ```bash
     ./firmware/patch_stm32_repo.sh /path/ke/Firmware_CDI_NS200
     ```
   - **Windows**:
     ```cmd
     firmware\patch_stm32_repo.bat ..\Firmware_CDI_NS200
     ```
   *(Atau secara manual: timpa `Core/Src/cdi_port.c`, `Core/Inc/cdi_port.h`, serta `cdi_firmware.c` dan `cdi_firmware.h` di folder target).*

3. Build dengan CMake + Ninja:
   ```bash
   cd Firmware_CDI_NS200
   cmake -S . -B build -G Ninja -DCMAKE_BUILD_TYPE=Debug -DCMAKE_TOOLCHAIN_FILE=cmake/arm-none-eabi-gcc.cmake
   cmake --build build
   ```

4. Flashing:
   - **Flash Pertama (Kabel / DFU / ST-Link)**:
     `build/NS200_CDI_R8_FACTORY.bin` di alamat `0x08000000`
   - **Update OTA via Bluetooth Android**:
     `build/NS200_CDI_R8_APP.bin` (jangan gunakan file FACTORY untuk OTA).

