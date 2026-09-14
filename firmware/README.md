# Panduan build firmware CDI

## Pilih source yang benar

Firmware lengkap yang sudah pernah dibuild berada pada dua repository yang sejak awal
ditautkan oleh aplikasi:

| Target | Source lengkap | Status yang tercatat |
|---|---|---|
| WeAct STM32WB55CGU6 | [Firmware_CDI_NS200](https://github.com/phreakazone/Firmware_CDI_NS200) | R8: CMake, bootloader, startup, linker, HAL/WPAN, release binary; build GNU Arm 13.2.1 terverifikasi |
| ESP32-WROOM-32 | [Firmware_CDI_NS200_ESP32](https://github.com/phreakazone/Firmware_CDI_NS200_ESP32) | R8 port: NimBLE, board layer, GPTimer/MCPWM, OTA source; build ESP-IDF v6.1 terverifikasi |

**Jangan build folder kecil `firmware/stm32wb55` atau `firmware/esp32` di repository
Android ini sebagai firmware produksi.** Folder tersebut adalah pekerjaan integrasi core
R9 dan memang hanya berisi core/port ringkas, bukan pengganti proyek lengkap yang sudah
ada pada dua tautan di atas.

Itulah sebabnya source tampak sangat sedikit: yang terbuka adalah lapisan integrasi R9,
bukan paket firmware R8 lengkap.

## Panduan sesuai VS Code Anda

- [Build ESP32 dengan extension ESP-IDF](esp32/README.md)
- [Build STM32WB55 dengan STM32Cube/CMake](stm32wb55/README.md)

## Ringkasan paling cepat

### ESP32

```bash
git clone https://github.com/phreakazone/Firmware_CDI_NS200_ESP32.git
cd Firmware_CDI_NS200_ESP32
idf.py set-target esp32
idf.py build
idf.py -p COM7 flash monitor
```

Gunakan ESP-IDF **v6.1**, karena versi itu yang dicatat sudah berhasil membuild seluruh
source termasuk `cdi_ble_nimble.c`.

### STM32WB55

```bash
git clone https://github.com/phreakazone/Firmware_CDI_NS200.git
cd Firmware_CDI_NS200
cmake -S . -B build -G Ninja -DCMAKE_BUILD_TYPE=Debug -DCMAKE_TOOLCHAIN_FILE=cmake/arm-none-eabi-gcc.cmake
cmake --build build
```

Flash pertama memakai:

```text
build/NS200_CDI_R8_FACTORY.bin @ 0x08000000
```

Update BLE berikutnya memakai:

```text
build/NS200_CDI_R8_APP.bin
```

Jangan mengirim `FACTORY.bin` melalui menu OTA Android.

## Perbedaan build dan siap kendaraan

Kedua proyek memiliki bukti build source. Itu tidak otomatis membuktikan rangkaian fisik
aman. Sebelum ke kendaraan tetap diperlukan pemeriksaan BLE nyata, pin output, charger,
pulser, sensor, brownout, serta timing/jitter di bench.

Khusus ESP32, konfigurasi `sdkconfig` saat ini tercatat memakai single-app partition.
Build/flash USB tetap dapat dilakukan, tetapi jalur BLE OTA perlu tabel partisi OTA sebelum
boleh dianggap berfungsi. Jangan menganggap keberadaan file `cdi_r8_ota.c` saja sudah
membuat partisi OTA tersedia.
