# Build STM32WB55 di VS Code + STM32Cube

## Source yang harus dibuka

Gunakan repository lengkap:

[https://github.com/phreakazone/Firmware_CDI_NS200](https://github.com/phreakazone/Firmware_CDI_NS200)

Folder `firmware/stm32wb55` di repository Android hanya lapisan integrasi R9, bukan
proyek mandiri.

```bash
git clone https://github.com/phreakazone/Firmware_CDI_NS200.git
cd Firmware_CDI_NS200
```

Repository lengkap sudah berisi bootloader, startup, linker script, CMSIS/HAL, WPAN,
source CDI, CMake, generator factory image, dan release R8. Proyek sengaja tidak memakai
`.ioc`; jangan menjalankan **Generate Code** dari CubeMX karena dapat menimpa integrasi
khusus CDI/WPAN.

## Tools

Dari extension **STM32Cube for Visual Studio Code** / Bundle Manager, pasang:

- GNU Arm Embedded toolchain; build R8 tercatat memakai 13.2.1;
- CMake 3.22 atau lebih baru;
- Ninja;
- STM32CubeProgrammer;
- CMake Tools.

Restart VS Code setelah toolchain ditambahkan ke PATH.

## Buka proyek

**File → Open Folder** lalu buka root `Firmware_CDI_NS200`, yaitu folder yang berisi:

```text
CMakeLists.txt
STM32WB55CGUX_FLASH.ld
Bootloader/
Core/
CDI/
STM32_WPAN/
Startup/
ThirdParty/
cmake/
```

Bukan hanya folder `Core` atau `CDI`.

## Configure dan build

Repository saat ini tidak menyimpan `CMakePresets.json`, jadi gunakan perintah eksplisit
berikut dari terminal root proyek:

```bash
cmake -S . -B build -G Ninja -DCMAKE_BUILD_TYPE=Debug -DCMAKE_TOOLCHAIN_FILE=cmake/arm-none-eabi-gcc.cmake
cmake --build build
```

Untuk Release:

```bash
cmake -S . -B build-release -G Ninja -DCMAKE_BUILD_TYPE=Release -DCMAKE_TOOLCHAIN_FILE=cmake/arm-none-eabi-gcc.cmake
cmake --build build-release
```

Jika CMake masih memakai compiler PC, hapus hanya folder build yang gagal lalu configure
ulang dengan `CMAKE_TOOLCHAIN_FILE` di atas.

## Hasil build

Konfigurasi Debug menghasilkan file berikut di folder `build`:

```text
NS200_CDI_R8_BOOT.elf
NS200_CDI_R8_BOOT.hex
NS200_CDI_R8_BOOT.bin
NS200_CDI_R8_APP.elf
NS200_CDI_R8_APP.hex
NS200_CDI_R8_APP.bin
NS200_CDI_R8_FACTORY.hex
NS200_CDI_R8_FACTORY.bin
```

- Flash pertama: `NS200_CDI_R8_FACTORY.bin` pada `0x08000000`, atau factory HEX.
- Update lewat BLE Android: hanya `NS200_CDI_R8_APP.bin`.
- Jangan upload `FACTORY.bin` melalui OTA.

## Flash pertama melalui USB DFU

1. Lepaskan harness kendaraan dan seluruh catu selain USB.
2. Tahan **BOOT0**.
3. Tekan-lepas **NRST**.
4. Lepaskan **BOOT0**.
5. Sambungkan USB data.
6. Buka STM32CubeProgrammer → pilih **USB** → Refresh → **USB1** → Connect.
7. Pilih `build/NS200_CDI_R8_FACTORY.bin`.
8. Isi alamat `0x08000000`, aktifkan Verify, lalu Download.
9. Putuskan USB, pastikan BOOT0 LOW, lalu tekan-lepas NRST.

CLI:

```bash
STM32_Programmer_CLI -c port=USB1 -w build/NS200_CDI_R8_FACTORY.bin 0x08000000 -v -s 0x08000000
```

## Flash lewat ST-LINK bila DFU tidak terdeteksi

Hubungkan:

| ST-LINK | STM32WB55 |
|---|---|
| GND | GND |
| SWCLK | PA14 |
| SWDIO | PA13 |
| VTREF | 3V3 |

Jangan sambungkan pin 5 V ST-LINK ketika board sudah diberi daya dari USB/buck.
Programmer USBasp HW-437 bukan programmer STM32 dan tidak dapat menggantikan ST-LINK.

## Wireless stack CPU2

Aplikasi CPU1 tidak akan advertising bila CPU2 kosong atau wireless stack tidak cocok.
Gunakan menu **Wireless Stack** STM32CubeProgrammer untuk memasang BLE full stack dari
paket STM32CubeWB yang sesuai. Jangan mass erase CPU2 tanpa menyiapkan image stack untuk
memulihkannya.

Nama BLE tetap `NS200-CDI-R7` untuk kompatibilitas; versi/fitur dibaca aplikasi melalui
`GET,CAPS`.

## Verifikasi aman

1. Flash pertama tanpa harness, gate, charger, dan koil.
2. Pastikan board boot dan advertising.
3. Uji koneksi serta characteristic menggunakan nRF Connect/LightBlue.
4. Uji Android: `PING`, `GET,CAPS`, telemetry, mode, dan map.
5. Uji output pulser/gate/charger dengan logic analyzer atau osiloskop.
6. Pakai `NS200_CDI_R8_APP.bin` hanya setelah preflight OTA menunjukkan RPM 0,
   output OFF, dan HV di bawah 30 V.

Panduan asli proyek tersedia di
[README_BUILD.md](https://github.com/phreakazone/Firmware_CDI_NS200/blob/main/README_BUILD.md).
