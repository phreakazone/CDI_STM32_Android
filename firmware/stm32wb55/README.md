# Integrasi firmware STM32WB55 dengan STM32Cube

Target referensi: **WeAct STM32WB55CGU6**. Folder ini berisi core-port HAL, bukan proyek
STM32Cube mandiri. Karena belum ada `.ioc`, startup, linker script, HAL, dan stack
STM32_WPAN, folder `firmware/stm32wb55` tidak dapat langsung dibuka lalu dibuild.

Panduan ini menjelaskan cara membuat proyek Cube yang membungkus source tersebut tanpa
menduplikasi logika CDI di `../common`.

## Status sumber saat ini

Tersedia:

- `Core/Inc/cdi_port.h`;
- `Core/Src/cdi_port.c`;
- core C99 bersama di `../common/include` dan `../common/src`.

Harus dibuat oleh proyek Cube:

- file `.ioc`, clock tree, startup dan linker script;
- HAL GPIO, TIM1, TIM2 dan ADC1;
- STM32_WPAN BLE untuk CPU1 dan wireless stack untuk CPU2;
- custom GATT service;
- implementasi GPIO/ADC/NVM untuk semua hook `CDI_*`;
- konfigurasi flash/NVM yang aman;
- callback timer dan BLE ke port CDI.

OTA STM32 dinonaktifkan secara default. Jangan mengubah
`CDI_STM32_OTA_ENABLE=1` sebelum ada bootloader dual-slot yang tervalidasi.

## Prasyarat

- STM32CubeMX atau STM32CubeIDE.
- VS Code dengan extension STM32 yang menggunakan STM32CubeCLT/CMake, bila ingin build
  dari VS Code.
- Paket **STM32CubeWB** yang cocok dengan versi generator.
- STM32CubeProgrammer.
- ST-LINK dan koneksi SWD.
- Firmware wireless CPU2 STM32WB yang sesuai dengan stack BLE dari CubeWB.
- Board dilepas dari gate, charger, koil, fan dan kelistrikan kendaraan.

## 1. Buat proyek Cube

1. Buat proyek baru untuk **STM32WB55CGU6**, jangan hanya memilih keluarga generik.
2. Simpan proyek pada folder kerja tersendiri; contoh nama `CDI_STM32WB55_R9`.
3. Pilih toolchain STM32CubeIDE atau CMake sesuai extension VS Code yang dipakai.
4. Gunakan SWD untuk debug. PB3/PB4 dipakai fitur OEM Learn, jadi nonaktifkan full JTAG.
5. Aktifkan generation option agar user code dipertahankan saat CubeMX melakukan
   regenerasi.
6. Commit file `.ioc` dan seluruh source generated ke repository setelah konfigurasi
   hardware sudah benar; file tersebut saat ini memang belum tersedia.

## 2. Konfigurasikan clock dan peripheral

Gunakan sumber clock yang memenuhi persyaratan STM32WB BLE. LSE/HSE dan clock RF harus
mengikuti oscillator nyata pada board WeAct; jangan menyalin nilai board lain tanpa
memeriksa skematik.

### Timer

| Peripheral | Konfigurasi minimum | Fungsi |
|---|---|---|
| TIM2 | counter bebas 32-bit, 1 MHz | timestamp pulser dalam mikrodetik |
| TIM2 CH1 / PA0 | input capture rising edge atau EXTI yang membaca TIM2 | referensi pulser |
| TIM1 CH1 | output compare interrupt, counter 1 MHz | jadwal delay ignition |

Dengan timer clock `Ftimer`, atur prescaler menjadi
`(Ftimer / 1.000.000) - 1`. Pastikan hasilnya bilangan bulat dan verifikasi clock timer
setelah konfigurasi APB.

### ADC dan GPIO

| Fungsi | Pin | Mode |
|---|---|---|
| Pulser/reference | PA0 | TIM2_CH1 atau GPIO EXTI rising |
| Gate Center | PA1 | GPIO output ke driver |
| Gate Side | PA2 | GPIO output ke driver |
| Charger A | PA9 | GPIO output ke driver |
| Charger B | PB8 | GPIO output ke driver |
| Fan relay | PB5 | GPIO output ke driver |
| OEM Learn Center | PB3 | GPIO input |
| OEM Learn Side | PB4 | GPIO input |
| TPS | PA3 | ADC1 |
| Suhu | PA4 | ADC1 |
| HV Center | PA6 | ADC1 |
| HV Side | PA7 | ADC1 |
| Input tambahan | PB0 | ADC1 bila dipakai board |

Tetapkan output ignition, charger, dan fan ke kondisi **OFF** sedini mungkin pada startup.
Pin MCU tidak boleh langsung menggerakkan primer koil, kapasitor CDI, relay tanpa driver,
atau sinyal pulser kendaraan.

## 3. Aktifkan BLE STM32WB

Di CubeMX aktifkan middleware STM32_WPAN BLE peripheral dan komponen pendukung yang
dihasilkan CubeWB, termasuk IPCC/HSEM/RTC sesuai template BLE. CPU2 harus berisi wireless
coprocessor firmware yang kompatibel.

Buat custom service berikut:

| Nama | UUID | Properti |
|---|---|---|
| CDI service | `7a8f1000-6c9d-4e40-a45f-0b4b4e533230` | primary service |
| Telemetry | `7a8f1001-6c9d-4e40-a45f-0b4b4e533230` | notify, 20 byte |
| Command | `7a8f1002-6c9d-4e40-a45f-0b4b4e533230` | write |
| Response | `7a8f1003-6c9d-4e40-a45f-0b4b4e533230` | notify |
| OTA data | `7a8f1004-6c9d-4e40-a45f-0b4b4e533230` | write without response |
| OTA status | `7a8f1005-6c9d-4e40-a45f-0b4b4e533230` | notify, 16 byte |

Arahkan event write Command ke:

```c
cdi_stm32_ble_rx(data, length);
```

Arahkan write OTA Data ke:

```c
cdi_stm32_ota_data_rx(data, length);
```

Implementasikan tiga fungsi notify yang dipakai port:

```c
void CDI_BLE_Notify(const uint8_t *data, uint16_t size);
void CDI_BLE_NotifyTelemetry(const uint8_t *data, uint16_t size);
void CDI_BLE_NotifyOtaStatus(const uint8_t *data, uint16_t size);
```

Fungsi harus memeriksa status koneksi dan subscription sebelum mengirim notify. Proses
event BLE generated, misalnya `MX_APPE_Process()`, harus tetap dipanggil di main loop.

## 4. Masukkan source CDI ke proyek

Pilihan paling mudah:

1. Salin `firmware/stm32wb55/Core/Inc/cdi_port.h` ke `Core/Inc`.
2. Salin `firmware/stm32wb55/Core/Src/cdi_port.c` ke `Core/Src`.
3. Tambahkan `firmware/common/include/cdi_firmware.h` ke include path atau salin ke
   `Core/Inc`.
4. Tambahkan `firmware/common/src/cdi_firmware.c` ke target build. Jangan hanya
   menambahkan header.
5. Pastikan project compiler menggunakan C11/C99-compatible mode.

Lebih baik menautkan folder `common` sebagai source/include path agar core ESP32 dan
STM32 tetap satu sumber kebenaran.

## 5. Implementasikan hook board

Buat `Core/Src/cdi_board.c` dan implementasikan seluruh simbol berikut:

| Hook | Kontrak |
|---|---|
| `CDI_SetIgnitionEnable(bool)` | enable/disable jalur ignition dengan kondisi awal OFF |
| `CDI_SetChargerEnable(bool)` | kendalikan kedua charger melalui driver |
| `CDI_SetFanEnable(bool)` | kendalikan transistor/MOSFET/relay fan |
| `CDI_PulseIgnitionGate(uint16_t)` | hasilkan pulsa gate berdurasi mikrodetik tanpa blocking panjang |
| `CDI_ADC_LoadPercent()` | kembalikan TPS/load 0–100% |
| `CDI_ADC_TemperatureRaw()` | kembalikan nilai ADC mentah sensor suhu |
| `CDI_ADC_HvMaximumX10()` | maksimum feedback HV dalam satuan 0,1 V |
| `CDI_NVM_Load(void *, size_t)` | muat blob konfigurasi lengkap atau return false |
| `CDI_NVM_Save(const void *, size_t)` | simpan blob secara atomik dan return status |
| `CDI_BLE_Notify(...)` | notify characteristic Response |
| `CDI_BLE_NotifyTelemetry(...)` | notify characteristic Telemetry |
| `CDI_BLE_NotifyOtaStatus(...)` | notify characteristic OTA Status |

Jangan menulis flash internal setiap iterasi loop. Sediakan minimal 8 KiB untuk konfigurasi
dan gunakan dua page/record dengan CRC serta sequence number agar listrik mati saat save
tidak merusak kedua salinan. Perhatikan page flash yang dipakai wireless stack dan
bootloader.

## 6. Hubungkan callback timer

Jika PA0 memakai TIM2 input capture:

```c
void HAL_TIM_IC_CaptureCallback(TIM_HandleTypeDef *htim)
{
    if (htim->Instance == TIM2 &&
        htim->Channel == HAL_TIM_ACTIVE_CHANNEL_1) {
        cdi_stm32_reference_isr(
            HAL_TIM_ReadCapturedValue(htim, TIM_CHANNEL_1));
    }
}
```

Mulai input capture setelah `cdi_stm32_port_init()`:

```c
HAL_TIM_IC_Start_IT(&htim2, TIM_CHANNEL_1);
```

Hubungkan compare TIM1:

```c
void HAL_TIM_OC_DelayElapsedCallback(TIM_HandleTypeDef *htim)
{
    if (htim->Instance == TIM1 &&
        htim->Channel == HAL_TIM_ACTIVE_CHANNEL_1) {
        cdi_stm32_fire_compare_callback();
    }
}
```

Jangan memanggil kedua jalur EXTI dan input capture untuk satu pulsa, karena RPM akan
terhitung dua kali.

## 7. Inisialisasi dan main loop

Tambahkan context dengan lifetime statis:

```c
static cdi_context_t g_cdi;
```

Setelah HAL, clock, GPIO, ADC, timer, dan BLE selesai diinisialisasi:

```c
cdi_stm32_port_init(&g_cdi);
HAL_TIM_IC_Start_IT(&htim2, TIM_CHANNEL_1);
```

Di loop utama:

```c
while (1)
{
    MX_APPE_Process();
    cdi_stm32_process();
}
```

Jangan menaruh delay panjang dalam loop. `cdi_stm32_process()` mengurus input, state
machine, dan telemetry periodik.

## 8. Build di VS Code

Untuk proyek Cube yang digenerate sebagai CMake:

1. Buka root proyek hasil CubeMX, bukan hanya folder
   `firmware/stm32wb55/Core`.
2. Pilih kit/toolchain ARM dari STM32CubeCLT.
3. Jalankan CMake Configure.
4. Jalankan CMake Build untuk konfigurasi Debug atau Release.

Contoh terminal hanya jika generator proyek memang menyediakan preset CMake:

```bash
cmake --preset Debug
cmake --build --preset Debug
```

Nama preset berbeda antar versi generator. Bila tidak ada `CMakePresets.json`, gunakan
tombol Build extension STM32 atau buka proyek dengan STM32CubeIDE. Jangan memaksakan
perintah di atas pada proyek Makefile.

Artefak biasanya berupa:

```text
build/Debug/CDI_STM32WB55_R9.elf
build/Debug/CDI_STM32WB55_R9.bin
```

Lokasi tepat mengikuti generator/toolchain yang dipilih.

## 9. Flash

1. Pastikan firmware wireless CPU2 yang kompatibel sudah terpasang menggunakan
   STM32CubeProgrammer sesuai petunjuk release CubeWB.
2. Hubungkan ST-LINK: SWDIO, SWCLK, GND, 3V3 reference, dan NRST bila tersedia.
3. Flash ELF/BIN aplikasi CPU1 melalui extension STM32 atau STM32CubeProgrammer.
4. Jangan mass erase CPU2 tanpa rencana memulihkan wireless stack.
5. Jalankan pertama kali tanpa power stage CDI.

## 10. Verifikasi bench

1. Pastikan output ignition, charger, dan fan tetap OFF saat reset.
2. Pastikan device BLE advertising dan dapat tersambung.
3. Uji `PING`, `GET,CAPS`, dan `GET,PROFILE`.
4. Verifikasi telemetry 20 byte.
5. Uji ADC dengan tegangan rendah yang diketahui.
6. Uji fan OFF/ON/AUTO dengan LED atau beban dummy.
7. Berikan pulser terisolasi dan cocokkan RPM terhadap frekuensi serta pulses/rev.
8. Ukur delay dan lebar pulsa gate menggunakan osiloskop.
9. Uji limiter dan FIRST START sebelum menaikkan RPM.
10. Sambungkan power stage hanya setelah semua kondisi gagal/boot menghasilkan output OFF.

## Masalah umum

| Gejala | Penyebab yang mungkin |
|---|---|
| `main.h: No such file` | port dibuild tanpa proyek Cube generated |
| `htim1/htim2 undefined` | nama instance berbeda atau timer belum digenerate |
| `CDI_* undefined reference` | `cdi_board.c`/adapter BLE belum diimplementasikan |
| header `cdi_firmware.h` tidak ditemukan | include path `common/include` belum ditambahkan |
| simbol core CDI undefined | `common/src/cdi_firmware.c` belum masuk target |
| BLE tidak advertising | CPU2 wireless firmware, clock RF, IPCC, atau WPAN belum benar |
| RPM dua kali nilai sebenarnya | EXTI dan input capture sama-sama memanggil ISR |
| fan/advance tidak sesuai | sensor/calibration/profile belum dikonfigurasi dari aplikasi |

## Batas kesiapan

Source dalam repository sekarang adalah **integration layer**, bukan file Cube lengkap.
Firmware STM32 baru siap build setelah proyek Cube generated dan semua hook board di atas
ada. Firmware baru siap kendaraan setelah BLE, timer, ADC, NVM, fail-safe, dan output daya
lulus pengujian bench.
