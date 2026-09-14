# Build firmware ESP32 dengan VS Code + ESP-IDF

Target referensi: **ESP32-WROOM-32**. Buka folder ini sebagai proyek ESP-IDF:

```text
firmware/esp32
```

Jangan membuka repository root lalu menjalankan Build Project, karena `CMakeLists.txt`
proyek ESP-IDF berada di folder ini.

## Status sumber saat ini

Bagian berikut sudah tersedia:

- core CDI bersama di `../common`;
- port GPIO, timer, ADC, NVS dan OTA di `main/cdi_port.c`;
- tabel partisi dual OTA di `partitions.csv`;
- registrasi komponen ESP-IDF di `main/CMakeLists.txt`.

Adapter BLE GATT **belum tersedia**. `cdi_port.c` masih membutuhkan empat fungsi:

```c
void cdi_ble_init(void (*rx)(const uint8_t *data, size_t size));
void cdi_ble_notify(const uint8_t *data, size_t size);
void cdi_ble_notify_telemetry(const uint8_t *data, size_t size);
void cdi_ble_notify_ota_status(const uint8_t *data, size_t size);
```

Jadi build sekarang dapat menyelesaikan kompilasi source, tetapi akan gagal pada tahap
link dengan pesan `undefined reference to cdi_ble_...`. Itu bukan masalah instalasi
ESP-IDF dan jangan diselesaikan dengan menghapus pemanggilan BLE. Aplikasi Android memang
memerlukan service tersebut.

## Prasyarat

- VS Code.
- Extension **Espressif IDF**.
- ESP-IDF 5.x; gunakan satu versi yang sudah terpasang dengan lengkap dan jangan
  mencampur tools dari versi IDF berbeda.
- Python dan toolchain yang dipasang melalui ESP-IDF Tools Installer.
- Kabel USB data dan driver USB-to-UART board.
- Board tidak terhubung ke gate, charger, koil, fan, atau kelistrikan kendaraan saat
  build/flash pertama.

## Menyiapkan extension VS Code

1. Buka Command Palette.
2. Jalankan **ESP-IDF: Configure ESP-IDF Extension**.
3. Pilih instalasi ESP-IDF 5.x, Python environment, dan tools milik instalasi yang sama.
4. Pilih **ESP-IDF: Import ESP-IDF Project** dan arahkan ke `firmware/esp32`.
5. Jalankan **ESP-IDF: Set Espressif Device Target**, lalu pilih `esp32`.
6. Pilih port serial memakai **ESP-IDF: Select Port to Use**.
7. Pilih metode flash UART bawaan board.

Konfigurasi awal dari `sdkconfig.defaults` hanya diterapkan saat `sdkconfig` baru
dibuat. Hapus `sdkconfig` melalui **Full Clean** bila ingin mengulang konfigurasi awal.

## Build dari terminal ESP-IDF

Buka **ESP-IDF: Open ESP-IDF Terminal**, pastikan direktori aktif adalah
`firmware/esp32`, lalu jalankan:

```bash
idf.py set-target esp32
idf.py fullclean
idf.py reconfigure
idf.py build
```

Perintah setara di antarmuka extension:

1. **ESP-IDF: Full Clean Project**
2. **ESP-IDF: Build Your Project**

Build yang benar-benar lengkap baru mungkin setelah adapter BLE di bawah ditambahkan.

## Adapter BLE yang wajib ditambahkan

Tambahkan `main/cdi_ble.c` dan bila perlu `main/cdi_ble.h`, lalu masukkan
`cdi_ble.c` ke daftar `SRCS` di `main/CMakeLists.txt`. Implementasi yang dianjurkan
menggunakan NimBLE peripheral dengan satu koneksi.

| Nama | UUID | Properti | Arah integrasi |
|---|---|---|---|
| CDI service | `7a8f1000-6c9d-4e40-a45f-0b4b4e533230` | primary service | — |
| Telemetry | `7a8f1001-6c9d-4e40-a45f-0b4b4e533230` | notify | `cdi_ble_notify_telemetry()` |
| Command | `7a8f1002-6c9d-4e40-a45f-0b4b4e533230` | write | panggil callback `rx` dari `cdi_ble_init()` |
| Response | `7a8f1003-6c9d-4e40-a45f-0b4b4e533230` | notify | `cdi_ble_notify()` |
| OTA data | `7a8f1004-6c9d-4e40-a45f-0b4b4e533230` | write without response | panggil `cdi_esp32_ota_data_rx()` |
| OTA status | `7a8f1005-6c9d-4e40-a45f-0b4b4e533230` | notify | `cdi_ble_notify_ota_status()` |

Aturan adapter:

- simpan callback command yang diterima oleh `cdi_ble_init()`;
- kirim data characteristic Command ke callback itu tanpa mengubah byte;
- kirim characteristic OTA Data ke `cdi_esp32_ota_data_rx()`, bukan ke callback command;
- hanya memanggil notify jika client sudah subscribe;
- salin payload atau selesaikan notify sebelum buffer pemanggil tidak berlaku;
- mulai advertising lagi setelah disconnect;
- nama perangkat harus konsisten dengan filter/scanner aplikasi Android.

## Konfigurasi partisi

Proyek memakai `partitions.csv`:

| Partisi | Offset | Ukuran |
|---|---:|---:|
| NVS | `0x9000` | `0x6000` |
| OTA metadata | `0xF000` | `0x2000` |
| Factory app | `0x20000` | 1 MiB |
| OTA slot 0 | `0x120000` | 1 MiB |
| OTA slot 1 | `0x220000` | 1 MiB |

Flash minimal adalah 4 MiB. Jangan mengganti tabel ke `single factory app`, karena
firmware memakai `esp_ota_get_next_update_partition()`.

Periksa melalui **ESP-IDF: SDK Configuration Editor**:

- Bluetooth aktif;
- NimBLE aktif dan Bluedroid tidak dipakai;
- custom partition table menunjuk ke `partitions.csv`;
- FreeRTOS tick rate 1000 Hz;
- ukuran flash sesuai modul fisik.

## Pin port ESP32

| Fungsi | Pin |
|---|---|
| Pulser/reference | GPIO4, rising edge |
| Gate Center | GPIO25 |
| Gate Side | GPIO26 |
| Charger A | GPIO18 |
| Charger B | GPIO19 |
| Fan relay | GPIO13 |
| TPS ADC | GPIO36 / ADC1 channel 0 |
| Suhu ADC | GPIO39 / ADC1 channel 3 |
| HV Center | GPIO35 / ADC1 channel 7 |
| HV Side | GPIO32 / ADC1 channel 4 |

Semua input kendaraan memerlukan conditioner dan isolasi yang benar. Semua output daya
memerlukan driver. Nilai `CDI_HV_FULL_SCALE_X10=4000` berarti konversi saat ini
menganggap ADC full-scale sama dengan 400,0 V; sesuaikan hanya setelah rasio pembagi dan
isolasi pengukuran divalidasi.

## Flash dan monitor

Setelah build selesai tanpa error:

```bash
idf.py -p PORT flash monitor
```

Ganti `PORT` dengan port nyata, misalnya `COM7` di Windows atau
`/dev/ttyUSB0` di Linux. Keluar dari monitor dengan `Ctrl+]`.

Lebih aman menggunakan perintah `idf.py flash` daripada menulis offset manual, karena
ESP-IDF memakai alamat dari hasil build dan tabel partisi yang aktif.

Artefak berada di:

```text
build/bootloader/bootloader.bin
build/partition_table/partition-table.bin
build/cdi_universal_r9.bin
build/flasher_args.json
```

## Pemeriksaan setelah flash

1. Pastikan tidak ada reset loop pada serial monitor.
2. Temukan device melalui scanner BLE.
3. Subscribe ke Response, Telemetry, dan OTA Status.
4. Uji `PING`, kemudian `GET,CAPS` dan `GET,PROFILE`.
5. Pastikan telemetry berukuran 20 byte dan berubah saat input ADC diberi sinyal uji.
6. Uji pulser dengan generator sinyal terisolasi.
7. Periksa output gate dan charger menggunakan logic analyzer/osiloskop sebelum
   menyambungkan power stage.
8. Uji OFF, ON, dan AUTO untuk output fan memakai beban dummy/LED driver.

## Masalah umum

| Gejala | Penyebab yang mungkin | Tindakan |
|---|---|---|
| `undefined reference to cdi_ble_...` | adapter BLE belum dibuat | implementasikan `main/cdi_ble.c` |
| `partitions.csv not found` | folder proyek yang dibuka salah | buka `firmware/esp32` |
| OTA begin gagal | tabel partisi bukan dual OTA | full clean dan gunakan konfigurasi default |
| GPIO/ADC tidak sesuai | target bukan ESP32 klasik | pilih `esp32`, bukan C3/S3 |
| Aplikasi tersambung tetapi tidak mendapat data | UUID/properti/subscribe salah | cocokkan tabel GATT di atas |
| Reset saat output aktif | catu/grounding/driver daya buruk | lepas power stage dan uji ulang di bench |

## Yang belum boleh dianggap selesai

Firmware ESP32 belum siap dipasang ke kendaraan sampai adapter BLE benar-benar
diimplementasikan, link berhasil, protokol Android diuji, dan seluruh output diverifikasi
di bench. Lulusnya Android CI tidak membuktikan firmware ini berhasil dikompilasi.
