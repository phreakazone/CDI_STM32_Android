# Build ESP32-WROOM-32 di VS Code

## Source yang harus dibuka

Gunakan repository lengkap:

[https://github.com/phreakazone/Firmware_CDI_NS200_ESP32](https://github.com/phreakazone/Firmware_CDI_NS200_ESP32)

Folder `firmware/esp32` di repository Android bukan paket build utama.

```bash
git clone https://github.com/phreakazone/Firmware_CDI_NS200_ESP32.git
cd Firmware_CDI_NS200_ESP32
```

Repository lengkap berisi `main.c`, adapter board ESP32, GPTimer, MCPWM, NimBLE,
protokol, OEM Learn, OTA, self-test, `sdkconfig`, dan seluruh source CDI. Build penuh
tercatat berhasil menggunakan **ESP-IDF v6.1** dengan target ESP32 klasik.

## Siapkan extension ESP-IDF

1. Pasang extension **Espressif IDF** di VS Code.
2. Command Palette → **ESP-IDF: Configure ESP-IDF Extension**.
3. Pilih instalasi ESP-IDF v6.1 beserta Python dan tools dari instalasi yang sama.
4. **File → Open Folder** → buka root `Firmware_CDI_NS200_ESP32`, yaitu folder yang
   memiliki `CMakeLists.txt`, `sdkconfig`, dan folder `main`.
5. Command Palette → **ESP-IDF: Set Espressif Device Target** → pilih `esp32`.
6. Command Palette → **ESP-IDF: Select Port to Use** → pilih COM board.

Jangan pilih ESP32-C3/S3. Pin dan peripheral source ini untuk ESP32-WROOM-32 klasik.

## Build

Dari **ESP-IDF: Open ESP-IDF Terminal**:

```bash
idf.py set-target esp32
idf.py build
```

Atau tekan tombol **ESP-IDF: Build Your Project**.

Jika `sdkconfig.defaults` diubah dan Anda ingin membuat konfigurasi dari nol, pindahkan
atau hapus file `sdkconfig` lokal dahulu, lalu jalankan:

```bash
idf.py set-target esp32
idf.py reconfigure
idf.py build
```

`fullclean` hanya membersihkan hasil build; ia tidak selalu menghapus pilihan lama yang
sudah tersimpan di `sdkconfig`.

## Hasil build

File aplikasi utama:

```text
build/ns200_cdi_esp32.bin
```

File flash lengkap dan alamatnya dicatat ESP-IDF di:

```text
build/flasher_args.json
build/flash_args
```

Gunakan `idf.py flash` agar alamat bootloader, partition table, dan aplikasi tidak salah.

## Flash dan serial monitor

Lepaskan gate, charger, koil, fan, dan harness kendaraan. Sambungkan board hanya melalui
USB, lalu:

```bash
idf.py -p COM7 flash monitor
```

Ganti `COM7` dengan port Anda. Keluar dari monitor memakai `Ctrl+]`.

## Verifikasi setelah flash

1. Pastikan tidak terjadi reset loop.
2. Catat reset reason; bedakan brownout, panic, dan watchdog.
3. Scan dengan nRF Connect/LightBlue.
4. Pastikan nama `NS200-CDI-R7` terlihat; nama lama dipertahankan untuk kompatibilitas.
5. Pastikan service UUID `7a8f1000-6c9d-4e40-a45f-0b4b4e533230` serta
   characteristic `...1001` sampai `...1005` tersedia.
6. Baru uji aplikasi Android, telemetry, input pulser, dan output dengan alat ukur.

## Catatan penting OTA

Isi `sdkconfig` yang ada sekarang menunjukkan `CONFIG_PARTITION_TABLE_SINGLE_APP=y`.
Artinya build dan flash lewat USB bisa berhasil, tetapi fungsi `esp_ota_ops` tidak
memiliki slot OTA A/B. Sebelum mencoba upload firmware dari aplikasi Android, proyek
ESP32 harus memakai partition table OTA yang sesuai ukuran flash dan harus dibuild/flash
ulang lewat USB.

Jangan mengirim image OTA sungguhan sebelum jalur tersebut diuji dengan data dummy dan
partition table diverifikasi.

## Error umum

| Error | Tindakan |
|---|---|
| `idf.py` tidak dikenal | buka terminal melalui extension ESP-IDF, bukan terminal biasa |
| target/driver GPIO tidak cocok | pilih target `esp32` klasik |
| opsi Kconfig tidak dikenal | gunakan ESP-IDF v6.1 seperti build terverifikasi |
| file `.c` baru tidak ikut build | tambahkan ke daftar `SRCS` di `main/CMakeLists.txt` |
| board tidak advertising | cek log NimBLE, reset reason, antena/catu, dan UUID |
| brownout | perbaiki buck, kapasitor, dan ground; jangan hanya mematikan proteksi |

Dokumentasi teknis dan prosedur self-test lengkap tetap berada pada
[README firmware ESP32](https://github.com/phreakazone/Firmware_CDI_NS200_ESP32#readme).
