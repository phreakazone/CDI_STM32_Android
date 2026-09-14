# Firmware CDI Universal R9

Dokumen ini khusus firmware. Panduan Android tetap berada di [README utama](../README.md).

## Jawaban singkat: kenapa source firmware terlihat sedikit?

R9 memakai **satu core C99 bersama**, bukan menyalin seluruh logika dua kali:

- `common/src/cdi_firmware.c`: map, limiter, advance, FIRST START, suhu/fan,
  telemetry, protokol, dyno, dan state OTA.
- `esp32/main/cdi_port.c`: timer, GPIO, ADC, NVS dan OTA khusus ESP32.
- `stm32wb55/Core/Src/cdi_port.c`: penghubung core ke HAL STM32.

Karena itu adapter per MCU memang pendek. Namun ukuran yang sedikit bukan berarti kedua
folder saat ini sudah merupakan proyek firmware lengkap.

## Status build saat ini

| Bagian | Status | Keterangan |
|---|---|---|
| Core C99 | Ada | Dapat dikompilasi setelah dimasukkan ke target MCU |
| ESP32 CMake/partition/port | Ada | Struktur ESP-IDF tersedia |
| ESP32 BLE GATT | **Belum ada** | Empat fungsi `cdi_ble_*` masih berupa `extern` |
| STM32 port HAL | Ada | Jalur pemanggilan core tersedia |
| STM32 CubeMX `.ioc`, startup, linker, HAL dan wireless stack | **Belum ada** | Folder ini belum proyek STM32Cube mandiri |
| STM32 implementasi board/NVM/BLE | **Belum ada** | Fungsi `CDI_*` masih berupa hook `extern` |
| STM32 bootloader OTA | **Belum ada** | Sengaja dinonaktifkan dengan `CDI_STM32_OTA_ENABLE=0` |
| Android CI | Lulus | Workflow Android **tidak mengompilasi firmware** |

**Kesimpulan:** pada commit R9 saat ini, menekan tombol Build langsung pada folder
`firmware/stm32wb55` belum akan menghasilkan firmware. ESP-IDF juga akan berhenti pada
linker sampai adapter BLE ditambahkan. Hal ini sekarang dinyatakan terbuka agar file yang
belum ada tidak disangka sudah siap flash.

## Struktur direktori

```text
firmware/
├── README.md
├── common/
│   ├── include/cdi_firmware.h
│   └── src/cdi_firmware.c
├── esp32/
│   ├── CMakeLists.txt
│   ├── partitions.csv
│   ├── sdkconfig.defaults
│   ├── README.md
│   └── main/
│       ├── CMakeLists.txt
│       └── cdi_port.c
└── stm32wb55/
    ├── README.md
    └── Core/
        ├── Inc/cdi_port.h
        └── Src/cdi_port.c
```

## Pilih target

- Untuk **ESP32-WROOM-32**, baca [panduan ESP-IDF](esp32/README.md).
- Untuk **WeAct STM32WB55CGU6**, baca [panduan STM32Cube](stm32wb55/README.md).
- Jangan mencampur file port ESP32 ke proyek STM32 atau sebaliknya.
- `common/include` dan `common/src` dipakai oleh keduanya.

## Pin yang menjadi kontrak aplikasi dan firmware

| Fungsi | STM32WB55 | ESP32-WROOM-32 |
|---|---|---|
| Pulser | PA0 / TIM2 | GPIO4 |
| Gate Center | PA1 | GPIO25 |
| Gate Side | PA2 | GPIO26 |
| Charger A/B | PA9 / PB8 | GPIO18 / GPIO19 |
| TPS | PA3 | GPIO36 |
| Suhu | PA4 | GPIO39 |
| HV Center/Side | PA6 / PA7 | GPIO35 / GPIO32 |
| Fan relay | PB5 | GPIO13 |
| OEM Learn Center/Side | PB3 / PB4 | GPIO16 / GPIO17 |

Pin MCU hanya menuju rangkaian conditioner/driver. Pulser, primer koil, aki 12 V,
kapasitor CDI dan motor fan **tidak boleh** disambungkan langsung ke pin MCU.

## Kontrak BLE R9

UUID dasar yang harus dibuat oleh adapter BLE:

| Characteristic | UUID akhir | Arah |
|---|---|---|
| Service | `...1000` | service |
| Telemetry | `...1001` | notify, 20 byte |
| Command | `...1002` | write |
| Response | `...1003` | notify |
| OTA data | `...1004` | write without response |
| OTA status | `...1005` | notify, 16 byte |

UUID lengkap memakai bentuk
`7a8f100X-6c9d-4e40-a45f-0b4b4e533230`.

Android mengirim command dalam frame
`@sequence,COMMAND*CRC16\n`. Adapter port menyerahkan frame ke
`cdi_protocol_exchange()`; response harus dikirim kembali melalui characteristic
Response. Paket telemetry dibuat oleh `cdi_build_telemetry_packet()`.

## Batas format, bukan batas aman mesin

| Data | Rentang format |
|---|---:|
| RPM | 0–30.000 |
| Advance | -30,0° sampai 80,0° |
| Map | maksimum 32 × 16 |
| Slot | 4 |
| Pulser | 1–12 pulsa/rev |

Nilai tersebut hanya kapasitas penyimpanan/protokol. Batas sebenarnya mengikuti profil,
geometri pulser, koil, rangkaian charger dan kemampuan mekanis mesin.

## Urutan aman setelah firmware target benar-benar berhasil dibuat

1. Lepaskan rangkaian gate SCR/IGBT dan charger dari MCU.
2. Flash MCU hanya melalui USB/SWD dengan catu daya terbatas.
3. Pastikan BLE terdeteksi dan `PING`, `GET,CAPS`, `GET,PROFILE` bekerja.
4. Uji pulser memakai sumber sinyal rendah/terisolasi.
5. Uji telemetry, TPS, suhu dan fan tanpa memasang koil.
6. Uji output gate memakai osiloskop/logic analyzer.
7. Baru sambungkan power stage pada bench supply ber-current-limit.
8. FIRST START tetap memakai limiter 3.000 RPM dan advance maksimum 10°.
9. Jangan melakukan flashing pertama dengan sistem terpasang langsung pada kendaraan.

## Artefak yang nanti dihasilkan

Jika target sudah lengkap:

- ESP32:
  - `build/bootloader/bootloader.bin`
  - `build/partition_table/partition-table.bin`
  - `build/cdi_universal_r9.bin`
- STM32:
  - `Debug/<nama-proyek>.elf`
  - `Debug/<nama-proyek>.bin`
  - atau lokasi serupa sesuai generator CubeMX/CMake.

Tidak ada file firmware siap flash yang dihasilkan oleh workflow Android saat ini.
