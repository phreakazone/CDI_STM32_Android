# IGNITRA CDI R9 (v9.2.0) — Android Tuning, Dual-MCU Firmware & OTA


> **Sumber firmware lengkap yang dapat dibuild:** gunakan
> [STM32WB55](https://github.com/phreakazone/Firmware_CDI_NS200) atau
> [ESP32-WROOM-32](https://github.com/phreakazone/Firmware_CDI_NS200_ESP32).


## Alur integrasi R9

1. Android terhubung lewat BLE lalu meminta `GET,CAPS`, `GET,PROFILE`, `GET,TEMP`.
2. Firmware menyatakan batas nyata: RPM, advance, ukuran map, slot, PPR, fan, dyno, OTA.
3. Aplikasi membatasi editor berdasarkan jawaban firmware, bukan berdasarkan nama NS200.
4. Penulisan profile/map/kalibrasi hanya dilakukan saat RPM 0 dan HV di bawah 30 V.
5. FIRST START tetap 3.000 RPM dan maksimum 10° sampai `SETUP,DONE` tersimpan.
6. STM32WB55 dan ESP32 port hanya menangani GPIO, ADC, timer, NVM, BLE, dan OTA sesuai MCU.

| Fungsi | Android | Core bersama | STM32WB55 | ESP32 |
|---|---|---|---|---|
| Profil/limit/map | Maps | validasi + 4 slot | Flash/NVM hook | NVS |
| Suhu/fan | Setup + Demo | NTC 3 titik + AUTO fail-safe | PA4/PB5 | GPIO39/GPIO13 |
| Pulser/trigger | Setup/telemetry | lookup + clipping fisik | PA0/TIM1 | GPIO4/esp_timer |
| Gate Center/Side | telemetry | jadwal pengapian | PA1/PA2 | GPIO25/GPIO26 |
| Charger | telemetry | izin + interlock OTA | PA9/PB8 | GPIO18/GPIO19 |
| OTA | BLE | state/interlock | bootloader opsional | A/B partition |

## PCB ESP32 Rev C — sumber kanonik

Desain PCB ESP32 38-pin berada di [`hardware/easyeda/`](hardware/easyeda/README.md). File yang langsung dibuka melalui **EasyEDA Standard 6.5.51** adalah:

- [PCB dua layer](hardware/easyeda/generated/IGNITRA_CDI_ESP32_2L_EASYEDA.json);
- [PCB satu layer](hardware/easyeda/generated/IGNITRA_CDI_ESP32_1L_EASYEDA.json);
- [netlist kanonik](hardware/easyeda/generated/NETLIST.csv) dan [BOM kanonik](hardware/easyeda/generated/BOM.csv).

Kedua sumber PCB memakai `head.docType = "3"` dan sudah memuat outline, footprint, pad, net, slot isolasi, track, via, serta aturan DRC. Routing Rev C menghasilkan **0 koneksi gagal**: varian 2L berisi 376 track dan 136 via; pada varian 1L, BottomLayer adalah tembaga sedangkan TopLayer merupakan rencana jumper kawat berisolasi. Tetap jalankan DRC EasyEDA, cocokkan footprint fisik, dan periksa creepage HV sebelum membuat Gerber.

| Fungsi | STM32WB55 | ESP32 38-pin |
|---|---:|---:|
| TPS signal / reference | PA3 / PA5 | GPIO36 / GPIO34 |
| Suhu / VBAT | PA4 / PB0 | GPIO39 / GPIO33 |
| HV Center / Side | PA6 / PA7 | GPIO35 / GPIO32 |
| Gate Center / Side | PA1 / PA2 | GPIO25 / GPIO26 |
| Pulser | PA0 | GPIO4 |
| OEM Learn Center / Side | PB3 / PB4 | GPIO16 / GPIO17 |
| Charger A / B | PA9 / PB8 | GPIO18 / GPIO19 |
| Fault / Fan / Strobe | PA10 / PB5 / PB9 | GPIO14 / GPIO13 / GPIO27 |

> GPIO25/GPIO26 hanya menggerakkan rangkaian driver SCR bertegangan rendah. Pin J1.12/J1.6 adalah keluaran pulsa CDI dari kapasitor/SCR dan tidak boleh disambungkan langsung ke GPIO.

Aplikasi Android kendali terpadu untuk unit pengapian **CDI Programmable NS200-CDI** (Bajaj Pulsar 200 DTS-i & Modifikasi Dual/Triple Spark dengan Engine R9 v5.0). Menggabungkan kokpit telemetri balap gaya MoTeC, pemetaan kurva pengapian resolusi tinggi 32x16 matrix 4-slot dinamis, Live Dyno Advance Trim, kalibrasi strobo pulser TDC, mode pembelajaran kurva asli (**OEM Learn Mode**), sistem pengunggah firmware nirkabel (**BLE OTA Firmware Uploader** dengan partisi A/B), alur aktivasi mandiri aman (**Safe DIY Mode**), katalog modul jadi pasaran (*Commercial Off-the-Shelf Drop-in Modules*), bengkel panduan kabel interaktif, diagnostik paket data biner BLE, serta simulator akustik mesin knalpot multi-silinder (*Live Audio Engine Test Bench*).

Mendukung Arsitektur Lintas Platform (*Dual-Platform*): [**WeAct STM32WB55**](https://github.com/phreakazone/Firmware_CDI_NS200) dan [**ESP32 WROOM**](https://github.com/phreakazone/Firmware_CDI_NS200_ESP32).

---

## 📋 Daftar Isi
1. [Fitur Utama](#-fitur-utama)
2. [Pembaruan Besar Firmware R8 & Aplikasi v8.3.2](#-pembaruan-besar-firmware-r8--aplikasi-v832)
3. [Katalog & Panduan Modul Siap Pakai di Pasaran (Drop-In Modular Upgrade)](#-katalog--panduan-modul-siap-pakai-di-pasaran-drop-in-modular-upgrade)
4. [Daftar Belanja Komponen Lengkap (BOM) & Panduan Bebas Salah Beli](#-daftar-belanja-komponen-lengkap-bom--panduan-bebas-salah-beli)
5. [Arsitektur & Tumpukan Teknologi](#-arsitektur--tumpukan-teknologi)
6. [Detail Modul & Layar](#-detail-modul--layar)
   - [1. Dashboard MoTeC & Tacho Slider](#1-dashboard-motec--tacho-slider)
   - [2. Ignition Maps (4-Slot Timing)](#2-ignition-maps-4-slot-timing)
   - [3. Setup CDI, OEM Learn & Kalibrasi TDC](#3-setup-cdi-oem-learn--kalibrasi-tdc)
   - [4. Pengunggah Firmware BLE OTA](#4-pengunggah-firmware-ble-ota)
   - [5. Live Audio Engine Test Bench & MOGE Super Bass](#5-live-audio-engine-test-bench--moge-super-bass)
   - [6. Quick Setup & Wiring Workshop](#6-quick-setup--wiring-workshop)
   - [7. BLE Terminal & Hex Diagnostics](#7-ble-terminal--hex-diagnostics)
7. [Ekosistem Sumber Data Lengkap Aplikasi](#-ekosistem-sumber-data-lengkap-aplikasi)
   - [A. Ringkasan Matriks Sumber Seluruh Parameter](#a-ringkasan-matriks-sumber-seluruh-parameter)
   - [B. Rincian Mendalam Asal Parameter Telemetri](#b-rincian-mendalam-asal-parameter-telemetri)
   - [C. Sinkronisasi Data Perintah & Respons (Command-Response Ecosystem)](#c-sinkronisasi-data-perintah--respons-command-response-ecosystem)
   - [D. Isolasi Sumber Data: Mode Hardware Fisik vs Mode Simulasi Demo](#d-isolasi-sumber-data-mode-hardware-fisik-vs-mode-simulasi-demo)
8. [Protokol Komunikasi BLE Firmware R8/R9 & Kontrak Android](#-protokol-komunikasi-ble-firmware-r8r9--kontrak-android)
9. [Skema Wiring Pinout & Transisi Fase (Konektor 12-Pin J1)](#-skema-wiring-pinout--transisi-fase-konektor-12-pin-j1)
10. [Instalasi & Kompilasi](#-instalasi--kompilasi)
11. [Catatan Rilis (Changelog)](#-catatan-rilis-changelog)

---

## 🚀 Fitur Utama

- **Koneksi Nirkabel BLE Ultra-Stabil**: Scanning otomatis, auto-reconnect, pengiriman perintah berbasis antrean (*queue-based write*), handshaking kapabilitas `GET,CAPS`, dan proteksi transisi mode bebas *ghost telemetry*.
- **Telemetri Balap Real-Time (20 Hz)**: Memantau RPM (batas mengikuti profil dan CAPS firmware (format maksimum 30.000 RPM)), TPS 0–100%, *ignition advance* (° BTDC), HV Center/Side (285V Normal / 345V PRO), voltase aki, fault, output, dan *rev limiter*. Kanal suhu disediakan protokol tetapi bernilai `N/A` sampai kurva konversi NTC firmware dikalibrasi.
- **Mode Pembelajaran Mandiri (OEM Learn Pasif)**: Membaca pulsa pengapian CDI bawaan pabrik secara pasif melalui input mikrokontroler saat mesin hidup, merekam kurva pengapian asli motor secara otomatis.
- **Rangkaian PCB Rev C**: LM339N untuk pulser/proteksi, dua PC817 diskrit untuk OEM Learn, MP1584 untuk catu logika, BC337 + 1N4007 untuk kontrol relay kipas eksternal, serta charger push-pull yang dikendalikan firmware. Modul boost generik tidak kompatibel.
- **Pengunggah Firmware BLE OTA**: Memperbarui image aplikasi target melalui Bluetooth LE (STM32 `APP.bin`; ESP32 image aplikasi ESP-IDF sesuai partition table), dilengkapi verifikasi CRC32 dan preflight keselamatan.
- **Alur Setup Checkpoint & Verifikasi Flash Nyata**: 
  - Alur OEM: Rekam timing pasif ➔ Konfirmasi cabut output koil OEM (`OEM_UNPLUGGED`) ➔ FIRST START aman (220V, center saja, advance ≤10°, limiter 3.000 RPM).
  - Menunggu bukti status `READY` nyata yang tersimpan di flash MCU (bukan hanya berhenti pada timer lokal).
  - Layout Strobo manual lama diisolasi eksklusif hanya untuk jalur `MANUAL` (darurat saat CDI OEM mati).
- **Seleksi Tegangan HV R8 Aktif (285 V & 345 V)**: Sakelar PRO memuat profil R8 yang tepat (`LOAD,<slot>`), sehingga target tegangan aktual berpindah nyata 285 V ↔ 345 V di MCU.
- **Pemetaan Kurva Pengapian 4 Slot**: Pilihan instan Slot 0 (Eco/Harian), Slot 1 (Touring/Street), Slot 2 (Wet/Rain), dan Slot 3 (Pro/Race 16x8 matrix). Dilengkapi *safety ceiling* 36.0° BTDC.
- **Slider RPM Interaktif & BLIP Gas**:
  - Pada **Mode Demo**: Slider menahan RPM simulasi secara stabil untuk pengujian audio dan visual.
  - Pada **Koneksi BLE Nyata**: Slider mengikuti putaran mesin motor asli secara *real-time*.
  - **Tombol BLIP Gas**: Simulasi puntiran gas kilat dengan lonjakan TPS spontan, akselerasi kuadratik, dan deselerasi inersia.
- **Simulator Suara Mesin Knalpot (Live Audio Test Bench)**: 12 preset suara knalpot multi-silinder berbasis sintesis kompresi akustik PCM 22.050 Hz 16-bit, termasuk preset **Moge 1800cc Super Bass (Gahar Empuk)**.
- **Bengkel Panduan Wiring & Solder**: Visualisasi interaktif soket 12-pin CDI, kode warna harness asli NS200, jalur koil sekunder, dan panduan langkah demi langkah.

---

## ⚡ Evolusi Pembaruan Firmware: R7 ➔ R8 ➔ R9 (v9.2.0)

| Fitur CDI | Firmware R7 (Lama) | Firmware R8 (Transisi) | Firmware R9 / Aplikasi v9.2.0 (Terbaru) |
|---|---|---|---|
| **Resolusi Peta Pengapian** | Maksimal 16x8 (128 titik) | Maksimal 16x8 (128 titik) | **Ekstrem 32x16 Matrix**: Dukungan kalkulasi interpolasi presisi tinggi hingga 512 titik. |
| **Batas Putaran Mesin (RPM)** | Terkunci di limit bawaan | 10.500 (Normal) / 11.500 RPM (PRO) | **Absolute Cap 30.000 RPM**: Mendukung mesin *high-revving* ekstrem dengan filter *debounce*. |
| **Konektivitas BLE & GPS** | Handshake terbatas, wajib GPS | Kontrak lengkap, wajib izin GPS | **Direct MAC Connect**: 100% Bebas GPS, identitas perangkat dibekukan jadi "NS200-CDI". |
| **Keandalan Instrumen (UI)** | Rentan *ghost telemetry* | Jarum RPM bisa "menggantung" | **Watchdog UI 2000ms**: Reset otomatis indikator ke 0 secara aman jika paket data terhenti 2 detik. |
| **Proteksi Penulisan (Flash)**| Mengandalkan sakelar/jumper fisik | Software interlock & konfirmasi layar | **Command Guard Keselamatan**: Blokir otomatis jika mesin hidup (RPM>0) atau tegangan HV>30V. |
| **Alur Akuisisi Timing** | Wajib strobo manual / timing light | **OEM Learn Pasif**: Rekam kurva via MCU | **Dipertahankan**: Tambahan dukungan skema *Inverter* PC817 khusus untuk pengujian meja. |
| **Pilihan Perakitan Hardware** | Solder puluhan komponen diskrit | Hybrid modular | **PCB Rev C ESP32 38-pin**: blok fungsi terintegrasi; modul eksternal hanya alternatif legacy. |
| **Aktivasi DIY & First Start** | Cabut-pasang jumper rumit | Wajib `OEM_UNPLUGGED` & verifikasi flash | **Sama dengan R8**: Deteksi *idle* stabil ≥3s untuk mengunci kalibrasi. |
| **Level Tegangan HV** | Terkunci pada 240V–280V | **Dual Target Aktif**: 285 V & 345 V (PRO) | **Sama dengan R8**. |
| **Update Firmware MCU** | Buka bodi & colok ST-Link / USB | **BLE OTA Flashing**: Unggah via Android | **BLE OTA Partisi A/B Nyata**: Konfigurasi memori aman pada Flash 4MB ESP32. |

---

## 🛒 Katalog Modul dan BOM PCB Rev C

PCB Rev C menggunakan satu papan **230 × 165 mm** dengan ESP32 DevKitC 38-pin. Blok yang sudah ada di PCB tidak boleh didobel dengan modul eksternal:

| Blok | Implementasi PCB Rev C | Catatan |
|---|---|---|
| OEM Learn | U3/U4 PC817 diskrit + masing-masing 4 × 12 kΩ 0,5 W | Sadapan dari J1.12/J1.6; bukan dari J1.9/J1.8 |
| Pulser | U2 LM339N + 39 kΩ seri + clamp BAT54S | Keluaran logic menuju GPIO4 |
| Kipas | QFAN BC337-40 + DFAN 1N4007 | Mengendalikan koil relay eksternal melalui J1.7 |
| Catu logic | UBUCK MP1584 | Turunkan IGN_12V menjadi 5 V sebelum ESP32 |
| Charger HV | U5 TC4427A + QHV1/QHV2 IRF3205 + T1 EE35 | Bukan modul boost generik |
| Discharge | SCR1/SCR2 BT151-800R + C_CENTER/C_SIDE 1 µF 630 V | Jalur HV harus memenuhi clearance/creepage |

Modul PC817 4-channel dan modul relay 5 V yang pernah didokumentasikan adalah **alternatif perakitan eksternal legacy**, bukan bagian paralel dari PCB Rev C. Pilih salah satu implementasi; jangan memasang keduanya sekaligus.

BOM lengkap per-reference ada di [`hardware/easyeda/generated/BOM.csv`](hardware/easyeda/generated/BOM.csv). Ringkasan nilai yang mudah tertukar:

- U5 = TC4427A; SCR1/SCR2 = BT151-800R; DTVS1/DTVS2 = 1.5KE33A.
- BAT54S = 7 buah.
- OEM Learn = 8 × 12 kΩ 0,5 W, dibagi empat seri per kanal.
- VBAT = 100 kΩ / 22 kΩ + resistor proteksi 1 kΩ ke GPIO33.
- Pulser = 39 kΩ 0,5 W seri, 10 kΩ bias, 10 MΩ histeresis, 4,7 kΩ pull-up, dan 1 kΩ proteksi ke GPIO4.
- C_CENTER/C_SIDE memakai footprint 9 × 4 lubang dengan jarak kaki 20,32 mm.
- T1 memakai grid universal EE35; detail posisi pin ada di README hardware.
- J1 adalah nomor logis 2 × 6 untuk solder manual dengan pin HV 6/12 dipisahkan; bukan header plug-in rapat 2 × 6.

---

## 🛠 Arsitektur & Tumpukan Teknologi

- **Bahasa**: Kotlin (100% Coroutines & StateFlow)
- **UI Framework**: Jetpack Compose dengan Material Design 3 (M3) Dark Carbon & MoTeC Racing Theme
- **State Management**: MVVM Architecture via `CdiViewModel` terpusat
- **Bluetooth Stack**: Android BLE API (`BluetoothGatt`, MTU 64/247, Queue-based Writer, BLE OTA Chunk Streaming)
- **Audio Synthesis Engine**: Android `SoundPool` (multi-stream crossfade) & `MediaPlayer` (custom tracks)
- **Data Serialization**: `kotlinx.serialization` untuk protokol paket biner CDI
- **Hardware Target (Dual-Platform)**:
  - [**WeAct STM32WB55CGU6**](https://github.com/phreakazone/Firmware_CDI_NS200) (Platform Orisinal).
  - [**ESP32 WROOM**](https://github.com/phreakazone/Firmware_CDI_NS200_ESP32) (Platform Porting).

---

## 📱 Detail Modul & Layar

### 1. Dashboard MoTeC & Tacho Slider
Menampilkan instrumen balap presisi tinggi:
- **Tachometer Radial & Linear**: Skala visual hingga 13.000 RPM; batas mengikuti profil aktif; format protokol maksimum 30.000 RPM dengan redline dinamis.
- **Panel Status Dual/Triple Spark**: Indikator busi utama (Center Plug) dan busi sekunder (Side Plugs) aktif berkedip sesuai sinyal pemantik.
- **Monitor Kapasitor Core / Dual Coil**:
  - Konfigurasi **1 Koil** terdeteksi: Tampilan kapasitor HV Core (J1.12) tersaji rapi dan **rata tengah** (*centered*) di dashboard.
  - Konfigurasi **2 Koil** terdeteksi (Dual/Triple Spark): Tampilan kapasitor tersaji **sejajar kanan dan kiri** (HV Core J1.12 di kiri & HV Side J1.6 di kanan) lengkap dengan bar meter progres pengisian tegangan HV menuju target (285V / 345V), indikator *PULSE ON*, dan status discharge aman.
- **Telemetry Readout Matrix & Status Kelistrikan**:
  - `ADVANCE`: Derajat pengapian (° BTDC)
  - `TPS`: Persentase bukaan gas (0–100%) dengan bar visual respon gas
  - `BATTERY`: Tegangan aki motor (contoh: 14.1V) lengkap dengan badge kesehatan aki (`NORMAL / CHARGING`, `SIAGA`, `AKI DROP <11.8V`)
  - `HV CAP`: Tegangan kapasitor discharge CDI (hingga 345V pada mode PRO)
  - `STATUS OPERASIONAL`: Mode limiter pemantik (FIRE NORMAL, SOFT CUT, HARD CUT), status relay kipas radiator (J1.7), status penguncian komisi Flash, dan diagnostik GATT BLE real-time (packet rate & CRC valid).
- **Interactive Tacho Slider & Simulasi Demo Lengkap**:
  - Pada **Mode Demo**: Default dual coil dengan lima modul firmware disimulasikan terpasang: SIDE, THERMAL, OEM_LEARN, AUX/Strobe, dan TPS_DIAG.
  - Alur simulasi wizard komisi setup 3 layar dapat dijalankan sampai benar-benar tuntas hingga tahap final status `READY` (Flash Terkunci).
  - Tersedia tombol **RESET DEMO** baik di Dashboard maupun Setup Wizard untuk mereset seluruh variabel komisi kembali ke kondisi nol/awal setiap saat.
  - Pada **Koneksi BLE Nyata**: Slider mengikuti putaran mesin motor asli secara *real-time*.
- **Tombol Aksi Cepat**: Preset instan `IDLE (1.4K)`, `5K`, `8K`, `LIMITER`, tombol interaktif `BLIP GAS`, `RESET RPM`, serta `RESET DEMO`.

### 2. Ignition Maps (4-Slot Timing)
Antarmuka tuning pengapian komprehensif:
- **Grafik Kurva 2D**: Visualisasi perbandingan kurva derajat pengapian terhadap RPM.
- **Editing Breakpoint**: Pengubahan nilai sudut pengapian per 1.000 RPM dengan batas aman (*hard ceiling*) 36.0° BTDC untuk mencegah detonasi/knocking.
- **Slot Selector**: Berganti cepat antara Slot 0 (Eco), Slot 1 (Street), Slot 2 (Rain), dan Slot 3 (Pro 16x8 matrix).
- **EEPROM / Flash Write**: Pengiriman perintah biner ber-checksum untuk menyimpan kurva permanen ke mikrokontroler CDI.

### 3. Setup CDI, OEM Learn & Kalibrasi TDC
Wizard komisi terpadu satu tahap per layar: BARU ➔ PULSER ➔ TDC ➔ TPS ➔ FIRST START ➔ READY.
- **Alur OEM Learn Pasif**:
  1. **Rekam Timing OEM**: Merekam timing asli dari CDI bawaan motor. Counter pulsa Center dan sampel Side terbaca secara real-time.
  2. **Konfirmasi Cabut Output OEM (`OEM_UNPLUGGED`)**: Tombol pengaman wajib untuk memastikan soket koil CDI OEM telah dilepas sebelum mengalihkan pengapian ke modul MCU, mencegah benturan driver.
  3. **FIRST START Aman**: Sistem mengunci mode aman (220V, center saja, advance ≤10°, limiter 3.000 RPM). Begitu idle stabil ≥3 detik, status tersimpan di flash MCU.
  4. **Verifikasi Flash READY Nyata**: Aplikasi menunggu status `READY` nyata dari mikrokontroler (tidak berhenti hanya karena timer lokal).
- **Layout Strobo Khusus Manual**: Offset TDC hanya dimunculkan pada mode `MANUAL` (jalur darurat jika CDI OEM rusak).
- **Target Tegangan HV R8 Aktif**:
  - Tombol seleksi tegangan HV **NORMAL (285 V)** dan **PRO (345 V)**.
  - Memuat profil R8 yang tepat (`LOAD,<slot>`) sehingga target aktual pada hardware MCU benar-benar berpindah 285 V ↔ 345 V.

### 4. Pengunggah Firmware BLE OTA
Pembaruan firmware nirkabel terintegrasi di tab BLE Terminal:
- **Interlock Keselamatan Preflight**: Tombol upload terkunci otomatis kecuali syarat aman terpenuhi:
  - `RPM == 0` (mesin wajib mati).
  - `Output Coils == OFF` (koil pemantik tidak aktif).
  - `HV Center & Side < 30V` (kapasitor daya tinggi sudah terkuras aman).
- **Transmisi Chunk Cerdas**: Mengirim image aplikasi target per blok melalui BLE GATT write without response, dilengkapi progress, total byte terkirim, dan pembatalan. Gunakan `APP.bin` untuk STM32 atau image aplikasi ESP-IDF yang sesuai partition table untuk ESP32.
- **Integritas Flash**: Verifikasi checksum CRC32 otomatis di sisi mikrokontroler sebelum menjalankan reboot aplikasi baru.

### 5. Live Audio Engine Test Bench & MOGE Super Bass
Simulator suara akustik knalpot motor:
- **Kategori Preset**:
  - `KAWASAKI`: Ninja 250 FI (Twin 180°), Ninja ZX-25R (Inline-4 Screamer).
  - `MOGE CC BESAR`: Moge 1800cc Super Bass (Gahar Empuk), Superbike 1000cc, Crossplane 1000cc (CP4), Ducati 1200cc (L-Twin Desmo), Cruiser 1800cc (V-Twin 45°).
  - `SUPERSPORT & BALAP`: Inline-4 600cc, Inline-3 800cc Triple, Twin 270° Cross-Twin, V4 MotoGP Prototype.
  - `STANDAR & KUSTOM`: Single 200 DTS-i asli, Custom Audio File (MP3/WAV).
- **Karakter Preset `Moge 1800cc Super Bass`**:
  - Idle lambat 850 RPM bertenaga (*heavy subwoofer thumps*).
  - Frekuensi sub-bass murni 38–75 Hz berpadu resonansi rongga *Helmholtz*.
  - Saturasi analog tube untuk hasil bass empuk (*non-harsh*) dan volume menggelegar.
- **Master Audio Switch**: Fitur proteksi anti-dengung instan (*zero hanging drone*).

### 6. Quick Setup & Wiring Workshop
Panduan perkabelan dan alur inisialisasi tahap demi tahap:
- **Katalog Drop-In Modul Pasaran**: Panduan modul siap pakai pengganti blok diskrit.
- **Preflight tahap BARU**: Memeriksa `PING`, `GET,STATUS`, dan `GET,SETUP` (wajib RPM 0 dan HV < 30V).
- **Konfigurasi Pulser Lanjutan (`PulserAdvancedSettings`)**:
  - Pilihan Trigger Edge: `FALLING` (standar NS200) atau `RISING`.
  - Pilihan Rasio Pulsa: 1, 2, 3, atau 4 PPR (Pulse Per Revolution).
  - Durasi Gate SCR: 60 µs, 80 µs (standar NS200), 100 µs, atau 120 µs.
- **Kontrol Mode Kipas Radiator (`FanModeSettings`)**:
  - Pilihan mode: `OFF`, `ON`, dan `AUTO` dengan syarat tulis RPM 0 dan HV aman. Pada firmware saat ini ON/AUTO sama-sama HIGH failsafe.
- **Soket CDI 12-pin**: Kode warna kabel asli NS200, jalur koil sekunder, dan sensor TPS.

### 7. BLE Terminal & Hex Diagnostics
Diagnostik teknis tingkat lanjut:
- **Statistik Paket Real-Time (Sliding Window 5 Detik)**: `packetRateHz` (18–22 Hz) dan integritas `crcValidPercent`.
- **Indikator Kualitas Sambungan BLE (`LinkQuality`)**: STABIL (Hijau), CUKUP (Kuning), BURUK (Merah), TERPUTUS (Abu-abu).
- **Monitor Paket Heksadesimal Mentah**: Menampilkan 20 bytes data v3 lengkap dengan penyorotan warna per field.
- **Konsol Manual Perintah CDI**: Terminal input untuk eksekusi perintah teks ASCII MCU dan log respons.

---

## 🌐 Ekosistem Sumber Data Lengkap Aplikasi

Aplikasi Android **IGNITRA CDI R9** mengelola ekosistem data yang komprehensif, menghubungkan sensor fisik kendaraan, rangkaian sirkuit analog/digital internal, mikrokontroler (STM32WB55 / ESP32), protokol nirkabel Bluetooth Low Energy (BLE), hingga penyajian antarmuka instrumentasi real-time MoTeC. 

Setiap nilai yang ditampilkan di layar memiliki rantai keterlacakan (*traceability chain*) yang jelas mulai dari titik fisik motor hingga bit representasinya di aplikasi.

---

### A. Ringkasan Matriks Sumber Seluruh Parameter

| Parameter UI | Label Layar | Rentang / Format | Asal Sumber Fisik / Sirkuit | Pin MCU (STM32 / ESP32) | Jalur Frame BLE / GATT | Penanganan di Aplikasi (ViewModel) |
|---|---|---|---|---|---|---|
| **STATUS KONEKSI** | `STATUS` / `ONLINE` | DISCONNECTED, CONNECTING, CONNECTED, STANDBY | Android BLE Stack & GATT Callback | N/A (Antena RF BLE) | Android `BluetoothGattCallback` | `CdiViewModel.connectionStatus`, `isBluetoothEnabled`, `bleLink` flag |
| **TEGANGAN AKI** | `BATT` / `VBAT` | 0.00 – 16.00 V (Resolusi 0.01V) | Terminal Kunci Kontak +12V (J1.5) via R-Divider (100k/22k) + proteksi 1k | PB0 (STM32) / GPIO33 (ESP32) ADC1 | Frame CORE (Byte 12–13, `uint16` centivolt) | `telemetry.batteryCv / 100f`, warning < 11.5V (aki lemah) |
| **TEGANGAN HV CENTER** | `HV Center` | 0 – 400 V DC | Kapasitor Film Busi Tengah C_CENTER via R-Divider (4x270k / 8.2k) | PA6 (STM32) / GPIO35 (ESP32) ADC1 | Frame CORE (Byte 14–15, `uint16` volt) | `telemetry.hvCenter`, status pengisian inverter push-pull Bank 1 |
| **TEGANGAN HV SIDE** | `HV Side` | 0 – 400 V DC | Kapasitor Film Busi Samping C_SIDE via R-Divider (4x270k / 8.2k) | PA7 (STM32) / GPIO32 (ESP32) ADC1 | Frame CORE (Byte 16–17, `uint16` volt) | `telemetry.hvSide`, status pengisian inverter push-pull Bank 2 |
| **PUTARAN MESIN** | `RPM` | 0 – 30.000 RPM (Resolusi 1 RPM) | Pick-up Sensor Magnet Pulser Kruk As (J1.10) via LM339 | PA0 / TIM1 (STM32) / GPIO4 esp_timer (ESP32) | Frame CORE (Byte 6–7, `uint16` RPM) | `telemetry.rpm`, animasi jarum tachometer + Watchdog 2.000 ms |
| **BUKAAN GAS** | `TPS` | 0 – 100.0 % (Resolusi 0.1%) | Sensor TPS Karburator NS200 (J1.2 & J1.4) via Filter RC | PA3 / PA5 (STM32) / GPIO36 / GPIO34 (ESP32) | Frame CORE (Byte 8–9, `uint16` permille 0–1000) | `telemetry.tps / 10f`, bar indikator persentase bukaan gas |
| **DERAJAT PENGAPIAN** | `ADVANCE` | -30.0 – +80.0° (batas format; batas aktif mengikuti CAPS/PROFILE) | Hasil lookup tabel peta ignition 32x16 berdasarkan RPM & TPS | Timer Internal MCU Gate Trigger Scheduler | Frame CORE (Byte 10–11, `int16` centi-degree) | `telemetry.advanceCdeg / 100f`, jarum sudut advance balap |
| **SUHU MESIN** | `TEMP` | -40 – +150 °C (atau `N/A`) | Sensor NTC Silinder Mesin (J1.3) via R-Pullup 4.7k | PA4 (STM32) / GPIO39 (ESP32) ADC1 | Frame DIAGNOSTIC (Byte 6–7, `int16` centi-°C) | `telemetry.tempCdeg`, menampilkan `N/A` jika `INT16_MIN` |
| **SLOT MAP AKTIF** | `SLOT` | Slot 0, 1, 2, 3 | Memori NVM / Flash MCU yang sedang aktif di-load | EEPROM Emulation / NVS Partition | Frame DIAGNOSTIC (Byte 8, `uint8` 0–3) | `telemetry.slot`, indikator profil berkendara aktif |
| **STATUS REV LIMITER** | `LIMITER` | IDLE, SOFT, HARD | Deteksi frekuensi RPM terhadap ambang batas profil | Firmware Limiter Controller | Frame DIAGNOSTIC (Byte 9, `uint8` state) | `telemetry.limiter`, indikator visual lampu redline warning |
| **SAFETY FLAGS** | `FLAGS` | Bitmask 8-bit (0x01–0x40) | Kondisi internal firmware (Armed, Pro, HV, Cal, Ble, Ready) | Register Status Firmware MCU | Frame DIAGNOSTIC (Byte 10, `uint8`) | `telemetry.armed`, `proEnabled`, `hvEnabled`, `calibrated`, `ready` |
| **STATUS OUTPUT AKTIF** | `OUTPUTS` | Bitmask 8-bit (1, 2, 4, 8) | Status pin keluaran fisik (Gate Center, Side, Strobe, Fan) | PA1, PA2, PB9, PB5 (STM32) / GPIO25, 26, 27, 13 (ESP32) | Frame DIAGNOSTIC (Byte 11, `uint8`) | `telemetry.centerEnabled`, `sideEnabled`, `strobeEnabled`, `fanEnabled` |
| **BIT KESALAHAN (FAULT)** | `FAULTS` | Bitmask 16-bit | Proteksi sirkuit: OVP, OCP, NTC Fail, Stall, Sync Error | Sensor proteksi hardware (RSENSE, OVP, Clamp) | Frame DIAGNOSTIC (Byte 12–13, `uint16`) | `telemetry.faults`, teks banner peringatan kerusakan sistem |
| **SUDUT TRIGGER PULSER** | `TRIGGER` | 0.0 – 90.0 °BTDC | Posisi takik magnet stator kruk as terhadap TDC fisik | Kalibrasi Strobe Flash / Offset Manual | Frame DIAGNOSTIC (Byte 14–15, `uint16` centi-degree) | `telemetry.triggerCdeg / 100f`, nilai patokan dasar pergeseran pulser |
| **KUALITAS SINYAL PULSER** | `QUALITY` | 0 – 100 % (atau hitungan) | Rasio pulsa pulser valid terhadap noise/glitch di komparator | Timer Capture Filter Integrity Checker | Frame DIAGNOSTIC (Byte 16, `uint8`) | `telemetry.pickupQuality`, indikator kesehatan sinyal magnet |
| **DURASI FIRST START** | `1ST SEC` | 0 – 255 detik | Penghitung waktu stasioner saat pengujian penyalaan pertama | Timer OS / RTOS Tick MCU | Frame DIAGNOSTIC (Byte 17, `uint8` detik) | `telemetry.firstStartSeconds`, auto-lock `READY` setelah 3 detik stabil |
| **FREKUENSI TELEMETRI** | `RATE HZ` | 0 – 25 Hz (Standar 20 Hz) | Frekuensi notifikasi paket GATT Bluetooth masuk | Modul Radio BLE Nirkabel | Dihitung lokal via sliding window 2.000 ms | `CdiViewModel.packetRateHz`, indikator kesehatan lalu lintas data |
| **INTEGRITAS DATA CRC** | `CRC %` | 0 – 100 % (Target 100%) | Validasi polinomial CRC16-CCITT (`0x1021`) per paket 20B | Perhitungan matematika frame biner | Dihitung per paket masuk di `CdiProtocol.crc16` | `CdiViewModel.crcValidPercent`, deteksi gangguan interferensi spark |
| **PULSA BELAJAR OEM** | `OEM PULSES` | 0 – 65.535 pulsa | Sinyal pemutus koil CDI pabrik (J1.12 & J1.6) via PC817 | PB3 & PB4 (STM32) / GPIO16 & GPIO17 (ESP32) | Respon ASCII `@<seq>,LEARN,...*CRC` | `CdiViewModel.oemAcceptedPulses`, persentase cakupan kurva |
| **TARGET PRO TEGANGAN** | `TARGET HV` | 285 V (Normal) / 345 V (PRO) | Konfigurasi profil yang dimuat (`FEATURE,PRO`) | NVM Flash Setting | Response `GET,STATUS` / `telemetry.isProVoltage` | `CdiViewModel.targetHvVoltage`, target regulasi PWM charger trafo |

---

### B. Rincian Mendalam Asal Parameter Telemetri

#### 1. Status Konektivitas & Radio BLE (`STATUS`)
- **Sumber Fisik**: Subsistem Bluetooth Low Energy perangkat Android dan radio transceiver nirkabel mikrokontroler (STM32WB55 2.4GHz BLE Radio atau ESP32 Bluetooth v4.2 BR/EDR & BLE).
- **Proses Akuisisi**:
  1. Aplikasi memeriksa status Bluetooth sistem melalui `BluetoothAdapter.isEnabled` (jika nonaktif, memunculkan prompt izin pengaktifan).
  2. Saat terhubung, `BluetoothGattCallback.onConnectionStateChange` memicu transisi state `DISCONNECTED` ➔ `CONNECTING` ➔ `CONNECTED`.
  3. Aplikasi meminta penemuan servis GATT (`discoverServices()`) dan mengaktifkan deskriptor `ENABLE_NOTIFICATION_VALUE` pada karakteristik Telemetri `7a8f1001-...`.
  4. Bila mesin motor dalam kondisi mati / standby (tidak ada semburan telemetri 20Hz), aplikasi memberikan status `STANDBY` stabil tanpa memicu pemutusan palsu (*false disconnect*).

#### 2. Voltase Baterai Aki Motor (`BATT` / `VBAT`)
- **Sumber Fisik**: Jalur tegangan aki 12V setelah kunci kontak ON, masuk melalui pin soket harness **J1.5** (kabel warna Cokelat).
- **Rangkaian Pengkondisi Sinyal**:
  - Melewati dioda pengaman kutub terbalik **DREV** (Schottky SB560) dan peredam transien **TVS_IN** (33V).
  - Melewati pembagi tegangan presisi **RVB1 100kΩ / RVB2 22kΩ**, lalu resistor seri **RVB3 1kΩ** menuju ADC. Pada 16 V, tegangan divider sekitar 2,89 V.
  - Dilindungi oleh **DBAT_BAT BAT54S** ke rel 3,3 V dan GND. Pin pembaca adalah **PB0** pada STM32 atau **GPIO33** pada ESP32.
- **Pemrosesan Firmware**: ADC membaca nilai analog terfilter, mengalikannya dengan faktor kalibrasi pembagi tegangan, lalu mengonversinya menjadi satuan centivolt (contoh: 12.60V dikodekan sebagai `1260`).
- **Jalur Data**: Dikirim setiap 100 ms pada paket biner **CORE** byte 12–13 (`uint16 LE`).
- **Konsumsi UI**: Ditampilkan pada dashboard instrumen dalam format `12.6V` dengan kode warna hijau (normal ≥ 12.0V), kuning (11.5V – 11.9V), atau merah (< 11.5V aki tekor).

#### 3. Tegangan Tinggi Kapasitor Busi Tengah (`HV Center`) & Busi Samping (`HV Side`)
- **Sumber Fisik**:
  - **HV Center**: Muatan listrik DC tegangan tinggi pada kapasitor film polypropylene **C_CENTER** (1.0 µF 630V MKP/MPP) yang mensuplai koil utama busi tengah (J1.12).
  - **HV Side**: Muatan listrik DC tegangan tinggi pada kapasitor film polypropylene **C_SIDE** (1.0 µF 630V MKP/MPP) yang mensuplai kedua koil busi samping (J1.6).
- **Rangkaian Pembangkit & Pengkondisi Sinyal**:
  - Dihasilkan oleh inverter push-pull frekuensi tinggi (50–100 kHz) yang digerakkan oleh sepasang MOSFET **IRF3205** melalui IC gate driver **TC4427A** dan trafo step-up ferit.
  - Tegangan AC sekunder disearahkan oleh jembatan dioda ultra-cepat **UF4007** (Trr < 75ns) dan dipisahkan menjadi dua bank pengisian independen oleh dioda isolator **DCH_C** dan **DCH_S**.
  - **Sensor Pembaca ADC**: Masing-masing bank dihubungkan ke rangkaian pembagi tegangan berimpedansi tinggi yang terdiri dari 4 resistor seri **270kΩ 1%** (total 1.080 kΩ) pada sisi atas dan resistor shunt bawah **8.2kΩ 1%** ke ground. Rasio pembagian: `8.2 / (1080 + 8.2) = 0.007535` (tegangan 400V HV diturunkan dengan aman menjadi ~3.01V di pin ADC).
- **Pin Input Mikrokontroler**:
  - STM32WB55: **PA6** (HV Center ADC1) dan **PA7** (HV Side ADC1).
  - ESP32: **GPIO35** (HV Center ADC1) dan **GPIO32** (HV Side ADC1).
- **Jalur Data**: Dikirim pada paket biner **CORE** byte 14–15 (Center) dan byte 16–17 (Side) dalam format satuan Volt murni integer (`uint16 LE`).
- **Fungsi Keselamatan**:
  - Peringatan warna merah menyala jika HV melebihi ambang batas proteksi (≥ 360V).
  - Interlock keselamatan: Penulisan map, kalibrasi setup, dan flashing OTA diblokir total jika salah satu bank HV masih terdeteksi di atas 30V.

#### 4. Putaran Mesin Kruk As (`RPM`)
- **Sumber Fisik**: Pulser magnetik (Variable Reluctance Sensor) pada bak magnet kruk as motor Pulsar 200NS, terhubung melalui soket harness **J1.10** (kabel Putih-Merah).
- **Rangkaian Pengkondisi Sinyal**:
  - Sinyal AC dari pick-up J1.10 masuk melalui **RPICK1 39kΩ 0,5 W**, lalu diklem oleh **DBAT_PICK BAT54S** pada node `PICKUP_SENSE`.
  - **RPICK2 10kΩ** memberi bias ke `VMID`; **U2 LM339N** membentuk sinyal digital dengan **RPICK3 10MΩ** sebagai histeresis, **RPICK4 4,7kΩ** sebagai pull-up, dan **RPICK5 1kΩ** sebagai proteksi menuju PA0/GPIO4.
- **Pemrosesan Firmware**:
  - Dihubungkan ke pin Timer Input Capture: **PA0 / TIM1_CH1** (STM32) atau **GPIO4** dengan interrupt `esp_timer` presisi mikrodetik (ESP32).
  - Firmware mengukur selang waktu antar pulsa (delta time $t$ dalam mikrodetik) dan menghitung putaran mesin per menit: $\text{RPM} = \frac{60.000.000}{t \times \text{PPR}}$.
- **Jalur Data**: Dikirim pada paket biner **CORE** byte 6–7 (`uint16 LE`).
- **Penyajian UI**: Menggerakkan jarum tachometer MoTeC analog-digital dengan interpolasi animasi halus, pembacaan angka digital besar, serta pengaman Watchdog 2.000 ms yang mereset nilai ke 0 saat mesin mati.

#### 5. Bukaan Katup Gas (`TPS`)
- **Sumber Fisik**: Potensiometer Throttle Position Sensor bawaan karburator NS200, terhubung pada soket harness **J1.2** (TPS_A, Hijau-Putih) dan **J1.4** (TPS_B, Abu-Abu).
- **Rangkaian Pengkondisi Sinyal**:
  - Diberi tegangan referensi stabil 5V / 3.3V, dengan keluaran tegangan linier 0.5V (gas tertutup) hingga 4.2V (gas terbuka penuh).
  - Dilewatkan pembagi tegangan resistor dan kapasitor filter low-pass 10nF untuk meredam derau getaran mekanis kran gas karburator.
- **Pemrosesan Firmware**:
  - Dibaca melalui pin ADC: **PA3/PA5** (STM32) atau **GPIO36/GPIO34** (ESP32).
  - Firmware memetakan nilai mentah ADC terhadap kalibrasi rentang `TPS_CLOSED` dan `TPS_OPEN` yang tersimpan di flash, menghasilkan nilai normalisasi 0–1000 permille (0.0% – 100.0%).
- **Jalur Data**: Dikirim pada paket biner **CORE** byte 8–9 (`uint16 LE`).

#### 6. Sudut Waktu Pengapian (`ADVANCE`)
- **Sumber Logika**: Dihasilkan secara real-time oleh algoritma engine management firmware berdasarkan interpolasi bilinear dari tabel matriks pengapian 32x16 (RPM vs TPS) pada slot yang sedang aktif.
- **Eksekusi Fisik**:
  - Nilai sudut advance menentukan berapa derajat kruk as sebelum Titik Mati Atas (°BTDC) pulsa pemicu SCR harus ditembakkan.
  - Firmware menerjemahkan derajat sudut menjadi penundaan waktu timer terhadap sinyal pulser magnet, lalu memicu pin gate SCR Center (**PA1 / GPIO25**) dan Gate SCR Side (**PA2 / GPIO26**).
- **Jalur Data**: Dikirim pada paket biner **CORE** byte 10–11 sebagai `int16 LE` bertanda dalam satuan centi-degree (contoh: 28.50° BTDC dikirim sebagai `2850`).

#### 7. Sensor Suhu Mesin Silinder (`TEMP`)
- **Sumber Fisik**: Sensor Negative Temperature Coefficient (NTC) bawaan silinder head motor NS200, terhubung melalui soket harness **J1.3** (kabel Hitam-Putih).
- **Rangkaian**: Terhubung ke resistor pull-up 4.7kΩ ke tegangan referensi.
- **Status Integrasi**: Protokol telemetri menyediakan kanal suhu pada paket **DIAGNOSTIC** byte 6–7 (`int16 LE` centi-°C). Pada firmware unit produksi saat ini, nilai dikirim sebagai `0x8000` (`INT16_MIN` = -32.768) yang diterjemahkan aplikasi secara jujur sebagai `N/A` sampai tabel kalibrasi resistansi-suhu NTC diaktifkan di rilis firmware mendatang.

#### 8. Flags Status Keselamatan & Interlock (`FLAGS`)
- **Sumber Logika**: Byte register status internal firmware pada paket **DIAGNOSTIC** byte 10:
  - `Bit 0 (0x01)` **ARMED**: Sirkuit output pengapian aktif dan siap memicu koil.
  - `Bit 1 (0x02)` **PRO_ENABLED**: Profil tegangan PRO 345V aktif.
  - `Bit 2 (0x04)` **HV_ENABLED**: Blok pengisian daya inverter HV aktif memompa tegangan.
  - `Bit 3 (0x08)` **CALIBRATED**: Setup dasar TPS dan kalibrasi TDC telah tersimpan permanen di flash.
  - `Bit 4 (0x10)` **BLE_LINK**: Status jabat tangan komunikasi nirkabel aktif.
  - `Bit 5 (0x20)` **READY**: Sistem telah lolos uji hidup stasioner stabil ≥3 detik dan siap operasi penuh.
  - `Bit 6 (0x40)` **FIRST_START**: Sistem berada dalam mode pembatasan keselamatan pengujian perdana (220V, center saja, advance ≤10°, rev limit 3.000 RPM).

#### 9. Status Keluaran Driver Fisik (`OUTPUT FLAGS`)
- **Sumber Fisik**: Byte register status keluaran pin mikrokontroler pada paket **DIAGNOSTIC** byte 11:
  - `Bit 0 (1)` **CENTER_COIL**: Driver SCR koil tengah aktif (PA1 / GPIO25).
  - `Bit 1 (2)` **SIDE_COIL**: Driver SCR kedua koil samping aktif (PA2 / GPIO26).
  - `Bit 2 (4)` **STROBE**: Output pemicu lampu strobo timing light aktif (PB9 / GPIO27).
  - `Bit 3 (8)` **FAN_RELAY**: Output kendali relay kipas pendingin aktif (PB5 / GPIO13).

---

### C. Sinkronisasi Data Perintah & Respons (Command-Response Ecosystem)

Selain telemetri biner periodik 20 Hz, aplikasi bertukar data konfigurasi kritis dengan mikrokontroler menggunakan protokol teks berbingkai CRC16: `@<seq>,<body>*<CRC16-hex>\n`.

1. **Jabat Tangan Kapabilitas (`GET,CAPS`)**:
   - Sumber: Firmware MCU merespons string kapabilitas hardware yang didukung (contoh: `CAPS,R9.0,PROTO4,OEM_LEARN,MANUAL,OTA,AUTO_FIRST_START,NO_JUMPERS`).
   - Aplikasi menyesuaikan batas antarmuka (RPM max 30.000, 4 slot map, dimensi 32x16) mengikuti kapabilitas nyata firmware yang terhubung.
2. **Sinkronisasi Wizard Setup (`GET,SETUP`)**:
   - Firmware mengembalikan data konfigurasi: jenis trigger edge, nilai PPR, durasi pulsa SCR gate (60–120 µs), ambang ADC TPS tutup/buka, status kalibrasi strobo TDC, dan tahapan wizard setup (0..4).
   - Aplikasi memetakan nilai tersebut ke antarmuka 6 tahap visual mandiri.
3. **Data Pembelajaran Timing OEM (`GET,LEARN` / `LEARN,START`)**:
   - Pulsa pemicu pengapian dari CDI bawaan pabrik disadap secara pasif melalui optocoupler PC817 ke pin **PB3/PB4** (STM32) atau **GPIO16/GPIO17** (ESP32).
   - Firmware menghitung selang waktu kedatangan pulsa terhadap sinyal pick-up kruk as, memetakan derajat pengapian asli bawaan motor per rentang RPM, dan melaporkan jumlah pulsa valid (`accepted`), pulsa tertolak (`rejected`), persentase cakupan (`coverage%`), serta beda sudut koil samping (`sideOffsetCdeg`).
4. **Alur Pengunggahan Firmware Nirkabel (`OTA_DATA` & `OTA_STATUS`)**:
   - File binary firmware (`.bin`) dibaca dari penyimpanan lokal Android.
   - Aplikasi menghitung CRC32 dari seluruh isi file.
   - Sesi dibuka dengan perintah `@<seq>,OTA,BEGIN,<ver>,<size>,<crc32>*CRC`.
   - Data dikirim dalam paket-paket biner terfragmentasi (maksimum 208 byte per payload) dengan nomor offset 32-bit dan CRC16 perlindungan per chunk.
   - Status penulisan flash dipantau melalui notifikasi karakteristik `OTA_STATUS` (`0xcd18`).

---

### D. Isolasi Sumber Data: Mode Hardware Fisik vs Mode Simulasi Demo

Untuk menjamin integritas data teknis dan menghindari kebingungan saat diagnosa lapangan, sistem memisahkan sumber data secara mutlak:

1. **Mode Hardware Fisik (BLE Connected, `_isSimulationMode == false`)**:
   - Seluruh variabel telemetri murni bersumber dari dekode paket biner mikrokontroler fisik.
   - Thread simulasi internal dibekukan total (`resetDemoState()`).
   - Perubahan slider RPM pada layar tidak akan memanipulasi putaran mesin fisik motor.
   - Tombol pengujian pulsa buatan (+10) telah dihapus dari unit produksi demi kepatuhan 100% pada sinyal pulser fisik nyata.
2. **Mode Simulasi Demo (Bebas Koneksi, `_isSimulationMode == true`)**:
   - Digunakan untuk evaluasi fitur antarmuka, demonstrasi suara mesin akustik (Live Audio Engine Test Bench), dan pelatihan teknisi tanpa unit motor.
   - Data telemetri digerakkan oleh generator matematis lokal berbasis slider gas interaktif dan tombol puntir gas instan (BLIP GAS).
   - Indikator status secara transparan menampilkan label `SIMULASI MODE` agar teknisi selalu mengetahui bahwa angka yang tampil bukan berasal dari motor nyata.

---

## Kontrak aktif aplikasi ↔ firmware R9.2

Implementasi aktif mengikuti `Firmware_CDI_NS200_ESP32/docs/APP_FIRMWARE_REFERENCE.md`:

- firmware adalah sumber kebenaran untuk identity, capability, module, commissioning, setup, map, output, dan fault;
- state awal aplikasi adalah `UNKNOWN`, bukan serial/modul contoh;
- binding disimpan lokal per serial CDI dan hanya boleh dibuat setelah `VERSION` serta `IDENTITY` diterima;
- seluruh write memerlukan sesi `READY_FULL`, binding cocok, capability tersedia, dan telemetri segar;
- modul firmware hanya `SIDE`, `THERMAL`, `OEM_LEARN`, `AUX`, dan `TPS_DIAG`;
- receiver audio eksternal memakai Classic Bluetooth A2DP Android dan bukan bagian `GET,MODULES`;
- READY dual menggunakan `SETUP,READY,DUAL,<offsetCdeg>`;
- map menggunakan matrix RPM×TPS dinamis dari `CAPS/META/PROFILE`, maksimum 32×16;
- OTA memakai capability `OTA`, platform dari `VERSION`, dan build delapan digit pada nama image.

Bagian R7/R8 di bawah dipertahankan hanya sebagai riwayat kompatibilitas. Kontrak aktif tidak boleh diturunkan dari contoh R8 lama.

## 📡 Protokol Komunikasi BLE Firmware R8/R9 & Kontrak Android

Aplikasi berkomunikasi melalui BLE GATT Custom Service:

- **Service UUID**: `7a8f1000-6c9d-4e40-a45f-0b4b4e533230`
- **Telemetry Characteristic UUID (Notify 20 Hz)**: `7a8f1001-6c9d-4e40-a45f-0b4b4e533230`
- **Command Characteristic UUID (Write)**: `7a8f1002-6c9d-4e40-a45f-0b4b4e533230`
- **Response Characteristic UUID (Notify ASCII Stream)**: `7a8f1003-6c9d-4e40-a45f-0b4b4e533230`
- **Format telemetri biner v3**: tepat 20 byte; frame CORE dan DIAGNOSTIC bergantian pada total 20 Hz.

| Byte | Semua frame | CORE (`kind=0`) | DIAGNOSTIC (`kind=1`) |
|---:|---|---|---|
| 0–1 | Magic LE `0xCD15` | — | — |
| 2 | Versi `0x03` | — | — |
| 3 | Jenis frame | `0` | `1` |
| 4–5 | Sequence `uint16 LE` | — | — |
| 6–7 | Payload | RPM `uint16` | Suhu `int16` centi-°C; saat ini `INT16_MIN` = N/A |
| 8–9 | Payload | TPS permille `uint16` | Slot aktif `uint8`, limiter state `uint8` |
| 10–11 | Payload | Advance `int16` centi-degree | Flags `uint8`, output flags `uint8` |
| 12–13 | Payload | VBAT centivolt `uint16` | Fault bits `uint16` |
| 14–15 | Payload | HV Center volt `uint16` | Trigger angle centi-degree `uint16` |
| 16–17 | Payload | HV Side volt `uint16` | Pickup quality `uint8`, first-start seconds `uint8` |
| 18–19 | CRC | CRC16-CCITT atas byte 0–17, polinomial `0x1021`, init `0xFFFF` | sama |

Tahap setup tidak dikodekan di telemetri v3. Aplikasi mengambilnya dari `GET,SETUP` dan mempertahankannya saat frame biner masuk.

### Perintah Teks Firmware R8
- Semua perintah/respons ASCII dibungkus sebagai `@<seq>,<body>*<CRC16-hex>\n`.
- **Handshake dan pembacaan**:
  - `GET,CAPS` → `CAPS,R8.0,PROTO4,OEM_LEARN,MANUAL,OTA,AUTO_FIRST_START,NO_JUMPERS`.
  - `GET,STATUS`, `GET,META`, `GET,SETUP`, `GET,MODE`, `GET,LEARN`, `GET,OTA`, `GET,CELL,<tps>,<rpm>`.
- **Mode & Pembelajaran**:
  - `MODE,OEM_LEARN` : Mengaktifkan mode belajar timing pasif dari CDI OEM.
  - `MODE,MANUAL` : Mengaktifkan mode manual/strobo darurat.
  - `MODE,DIY,OEM_UNPLUGGED` : Mengaktifkan operasi CDI mandiri setelah soket OEM dicabut.
  - `LEARN,START` : Memulai perekaman pulsa pengapian OEM.
  - `LEARN,STOP` : Menghentikan perekaman dan menyimpan timing ke flash.
  - `LEARN,ABORT` : Membatalkan rekaman.
  - `GET,LEARN` → `LEARN,<state>,<coverage>,<accepted>,<rejected>,<side_samples>,<side_offset_cdeg>`.
- **Map, limiter, dan PRO**:
  - `FEATURE,PRO,ON|OFF` : Mengaktifkan izin profil PRO; hanya saat mesin berhenti dan HV aman.
  - `LOAD,<slot>` / `SAVE,<slot>` : Memuat/menyimpan slot 0–3. Slot 3 adalah 16x8 PRO 345V; slot 0–2 adalah 8x4 Normal 285V.
  - `LIVE,<tps_index>,<rpm_index>,<advance_cdeg>` : Mengubah satu sel working map.
  - `LIMIT,SOFT|HARD,<rpm>,<band>` : Mengatur limiter. Maksimum 10.500 Normal atau 11.500 PRO.
- **Protokol OTA Firmware**:
  - `OTA,BEGIN,<version>,<file_size>,<crc32_decimal>` : Memulai sesi OTA setelah preflight RPM 0, output/HV mati, dan kedua bank <30V.
  - Kirim chunk biner melalui characteristic OTA Data; ukuran chunk mengikuti MTU/platform yang dinegosiasikan aplikasi.
  - `OTA,COMMIT` : Verifikasi image dan tandai siap reboot.
  - `OTA,ABORT` : Pembatalan darurat upload firmware.
- **Perintah Setup & Status Umum**:
  - `GET,STATUS` : Meminta status telemetri lengkap.
  - `GET,SETUP` : Sinkronisasi penuh parameter setup workflow.
  - `GET,META` : Meminta metadata firmware dan build string.
  - `SETUP,EDGE,<FALLING|RISING>` : Mengatur trigger edge pulser.
  - `SETUP,PPR,<1-4>` : Mengatur rasio pulsa per putaran.
  - `SETUP,GATE_US,<40-150>` : Mengatur durasi pulsa SCR gate; UI menyediakan preset 60/80/100/120 µs.
  - `SETUP,PICKUP,CONFIRM`, `SETUP,STROBE,ON|OFF`, `SETUP,OFFSET,<cdeg>`, `SETUP,SAVE_TDC`, `SETUP,MANUAL_TDC,<cdeg>,CONFIRM`, `SETUP,TPS,CLOSED|OPEN`.
  - `SETUP,FAN,<OFF|ON|AUTO>` : Saat ini OFF menghasilkan output LOW; ON dan AUTO sama-sama HIGH failsafe sampai konversi NTC tersedia.
  - `SETUP,FIRST_START` : Memulai mode first start aman (220V, auto-lock 3s).
  - `SETUP,READY,CENTER` / `SETUP,READY,THREE,<offset>` : Tahap pengaktifan output penuh.
  - `SETUP,RESET,CONFIRM` : Reset alur setup ke tahap awal.

---

## 🔌 Skema Wiring Pinout & Transisi Fase (Konektor 12-Pin J1)

Tabel ini memetakan fungsi kabel harness bawaan motor NS200 ke pin yang tepat untuk platform STM32 maupun ESP32, guna menghilangkan segala bentuk ambiguitas operasional.

| J1 | Fungsi | Warna Kabel | Hubungan PCB Rev C | Deskripsi Kelistrikan & Routing |
|:--:|:-------|:------------|:-------------------|:--------------------------------|
| 1 | NC | Kosong / NC | Tanpa net | Cadangan; jangan disambung. |
| 2 | TPS_A | Hijau-Putih | Selector JTPS | Salah satu TPS signal/reference; posisi selector menentukan PA3/PA5 atau GPIO36/GPIO34. |
| 3 | TEMP_SENSOR | Hitam-Putih | Divider + clamp ke PA4/GPIO39 | Input NTC; bukan input 5 V langsung. |
| 4 | TPS_B | Abu-Abu | Selector JTPS | Pasangan TPS_A; jangan menetapkan signal/reference sebelum posisi JTPS dipilih. |
| 5 | IGN_12V | Cokelat | Fuse input | +12 V setelah kunci kontak; diturunkan ke 5 V sebelum MCU. |
| 6 | COIL_SIDE | Hitam-Merah | C_SIDE/SCR2 + tap OEM Learn | Pulsa CDI HV ke koil Side; **bukan GPIO26/PA2 langsung**. |
| 7 | FAN_RELAY | Biru-Kuning | Kolektor BC337 + flyback | Low-side sink untuk koil relay eksternal; GPIO13/PB5 HIGH menyalakan transistor. |
| 8 | OEM_SIDE_PROBE | Kosong / NC | NC/probe opsional | Bukan jalur langsung ke GPIO17/PB4. |
| 9 | OEM_CENTER_PROBE | Kosong / NC | NC/probe opsional | Bukan jalur langsung ke GPIO16/PB3. |
| 10 | PICKUP_RAW | Putih-Merah | 39k + clamp + LM339N | Input pulser mentah; keluaran komparator menuju PA0/GPIO4. |
| 11 | GND_STAR | Hitam-Kuning | Titik star ground | Pertemuan ground harness yang dikontrol. |
| 12 | COIL_CENTER | Oranye | C_CENTER/SCR1 + tap OEM Learn | Pulsa CDI HV ke koil Center; **bukan GPIO25/PA1 langsung**. |

J1 memakai penomoran logis 2 × 6, tetapi bukan header plug-in rapat. Pin HV 6 dan 12 dipindahkan ke kolom berjarak; pemasangan dilakukan dengan solder kabel manual dan strain relief. Detail jarak pad/slot ada di [README hardware](hardware/easyeda/README.md). OEM Learn disadap dari J1.6/J1.12 melalui empat resistor 12 kΩ 0,5 W seri per kanal dan PC817 menuju PB4/PB3 atau GPIO17/GPIO16.

### ⚠️ CATATAN KHUSUS PENGUJIAN MEJA (BENCH TEST & SIMULATOR)
Jika Anda menguji aplikasi Android dan MCU (khususnya ESP32) di atas meja kerja menggunakan daya USB dan alat Simulator Sinyal (seperti GM328A), Anda WAJIB mematuhi 3 aturan ini agar indikator di aplikasi merespons:
1. **Wajib Bypass Sensor Aki:** Ubah kode pada firmware menjadi `#define BENCH_TEST_MODE 1`. Jika dibiarkan `0`, sensor ADC akan membaca tegangan aki 0V, mencabut izin operasi (`output_permission = false`), dan jarum RPM di Android akan terkunci mati di angka 0.
2. **Gunakan Menu f-Generator, JANGAN PWM:** Jarum RPM Android tidak akan bergerak jika Anda menyuapkan sinyal dari menu `10-bit PWM`. Frekuensinya yang sangat rapat (~7.8kHz) otomatis ditolak sebagai *noise* oleh algoritma filter *debounce* (2000µs) di firmware R9. Gunakan murni menu **f-Generator** (misal: 25 Hz untuk 1.500 RPM).
3. **Memori Bluetooth ESP32:** Pastikan parameter `NimBLE Host task stack size` di `menuconfig` ESP32 sudah diubah ke **8192** bytes. Jika tidak, pengiriman paket telemetri 20Hz ke Android akan menyebabkan tumpukan memori penuh dan MCU melakukan *Watchdog Reset*.

### ⚠️ PERHATIAN: Transisi Hardware (Fase LEARN ➔ Fase DIY)
Untuk menghindari benturan arus driver koil dan memastikan keselamatan mikrokontroler, fungsionalitas pin J1.12 dan J1.6 diperlakukan berbeda secara fisik sesuai fasenya.

**FASE 1: Penyadapan Pasif (Mode OEM_LEARN)**
Pada fase ini, **CDI bawaan pabrik (OEM) WAJIB tetap menancap di soket motor** dan mengendalikan mesin. Mikrokontroler bertindak murni sebagai PENDENGAR (Input).
1. **Jalur Input (Wajib Pasang):** Kabel Pulser (J1.10) terhubung permanen ke pin pembaca pulser (PA0 / GPIO4).
   - Sadapan koil tengah: **J1.12** ➔ 4 × 12 kΩ 0,5 W seri ➔ PC817 ➔ PB3/GPIO16.
   - Sadapan koil samping: **J1.6** ➔ 4 × 12 kΩ 0,5 W seri ➔ PC817 ➔ PB4/GPIO17.
2. **Output wajib nonaktif:** PA1/PA2 atau GPIO25/GPIO26 hanya menuju driver SCR. Firmware tidak boleh memicu SCR selama CDI OEM masih terhubung.

**FASE 2: Pengambilalihan Penuh (Mode DIY / FIRST_START)**
Pada fase ini, **CDI bawaan pabrik (OEM) WAJIB dicabut secara fisik dari soket motor**. Mikrokontroler kini bertindak sebagai PENEMBAK (Output) yang mengontrol pengapian secara penuh.
1. **Konfirmasi Cabut CDI Pabrik:** Buka aplikasi Android, ubah mode ke DIY, dan centang konfirmasi bahwa soket OEM telah dilepas (`OEM_UNPLUGGED`).
2. **Jalur OEM Learn:** Pada PCB tetap terisolasi oleh PC817 dan diabaikan firmware di luar mode Learn; tidak pernah disambung langsung ke GPIO.
3. **Jalur Output:** PA1/PA2 atau GPIO25/GPIO26 menggerakkan driver SCR bertegangan rendah. SCR1/SCR2 kemudian melepas C_CENTER/C_SIDE ke **J1.12/J1.6**.

---

### 📐 Detail Header Pinout MCU (STM32WB55 35-Pin & ESP32 38-Pin)

Menu **Pinout MCU** dalam aplikasi menyediakan visualisasi ganda (**Mode Tabel 2-Kolom Ringkas** dan **Mode Visual Fisik Board**) yang dapat dipilih sesuai preferensi saat perakitan:

#### 1. WeAct Studio STM32WB55 Core Board (35 PIN)
- **Header Atas (H_TOP, 15 Pin)**:
  - `H_TOP.1` - `H_TOP.2`: `GND`
  - `H_TOP.3` - `H_TOP.4`: `3V3` (referensi/pull-up, bukan beban besar)
  - `H_TOP.5`: `PB7` (UART service RX opsional)
  - `H_TOP.6`: `PB6` (UART service TX opsional)
  - `H_TOP.7`: `PB5` (output fan J1.7 via relay/transistor)
  - `H_TOP.8`: `PB4` (Sadap Koil Samping OEM Learn via Opto PC817)
  - `H_TOP.9`: `PB3` (Sadap Koil Tengah OEM Learn via Opto PC817)
  - `H_TOP.10`: `PA15` (cadangan)
  - `H_TOP.11`: `PA10` (input hardware fault `PWM_CLAMP`, active-low)
  - `H_TOP.12`: `PE4` (LED status onboard, active-low)
  - `H_TOP.13`: `PB1` (cadangan)
  - `H_TOP.14`: `PB0` (ADC VBAT melalui pembagi/clamp)
  - `H_TOP.15`: `GND`
- **Header Bawah (H_BOTTOM, 20 Pin)**:
  - `H_BOTTOM.1`: `GND`
  - `H_BOTTOM.2` - `H_BOTTOM.3`: `5V` (Input Catu Daya 5.00V dari Buck Converter MP1584)
  - `H_BOTTOM.4`: `VBAT` (backup RTC; **DILARANG** ke aki 12V)
  - `H_BOTTOM.5`: `PH3` (cadangan)
  - `H_BOTTOM.6`: `PB9` (output strobo TDC)
  - `H_BOTTOM.7`: `PB8` (PWM charger B ke TC4427 INB)
  - `H_BOTTOM.8`: `NRST`
  - `H_BOTTOM.9`: `PA0` (pickup TIM2_CH1 dari komparator)
  - `H_BOTTOM.10`: `PA1` (gate Center via driver SCR1)
  - `H_BOTTOM.11`: `PA2` (gate Side via driver SCR2)
  - `H_BOTTOM.12`: `PA3` (ADC TPS)
  - `H_BOTTOM.13`: `PA4` (ADC suhu; konversi firmware belum tersedia)
  - `H_BOTTOM.14`: `PA5` (ADC monitor TPS reference)
  - `H_BOTTOM.15`: `PA6` (ADC feedback HV Center)
  - `H_BOTTOM.16`: `PA7` (ADC feedback HV Side)
  - `H_BOTTOM.17`: `PA8` (cadangan)
  - `H_BOTTOM.18`: `PA9` (PWM charger A ke TC4427 INA)
  - `H_BOTTOM.19`: `PB2` (dikonfigurasi pulldown, tidak dipakai keputusan firmware; bukan interlock)
  - `H_BOTTOM.20`: `GND`

#### 2. ESP32-WROOM-32D DevKitC V4 (38 PIN)
- Penomoran memakai posisi lokal per sisi agar tidak ambigu terhadap varian DevKit.
- **Header kiri (`LEFT.1`–`LEFT.19`, atas ke bawah)**:
  - `LEFT.1 3V3`; `LEFT.2 EN`; `LEFT.3 GPIO36/VP` TPS; `LEFT.4 GPIO39/VN` suhu; `LEFT.5 GPIO34` TPS reference.
  - `LEFT.6 GPIO35` HV Center; `LEFT.7 GPIO32` HV Side; `LEFT.8 GPIO33` VBAT; `LEFT.9 GPIO25` gate Center; `LEFT.10 GPIO26` gate Side.
  - `LEFT.11 GPIO27` strobo; `LEFT.12 GPIO14` hardware fault active-low; `LEFT.13 GPIO12` strapping/cadangan; `LEFT.14 GND`; `LEFT.15 GPIO13` fan.
  - `LEFT.16 GPIO9`, `LEFT.17 GPIO10`, `LEFT.18 GPIO11` terhubung flash internal—jangan digunakan; `LEFT.19 VIN/5V`.
- **Header kanan (`RIGHT.1`–`RIGHT.19`, atas ke bawah)**:
  - `RIGHT.1 GND`; `RIGHT.2 GPIO23`; `RIGHT.3 GPIO22`; `RIGHT.4 TX0/GPIO1`; `RIGHT.5 RX0/GPIO3`; `RIGHT.6 GPIO21`; `RIGHT.7 GND`.
  - `RIGHT.8 GPIO19` charger B; `RIGHT.9 GPIO18` charger A; `RIGHT.10 GPIO5` bench only; `RIGHT.11 GPIO17` OEM Side; `RIGHT.12 GPIO16` OEM Center; `RIGHT.13 GPIO4` pickup.
  - `RIGHT.14 GPIO0` BOOT; `RIGHT.15 GPIO2`; `RIGHT.16 GPIO15`; `RIGHT.17 GPIO8`, `RIGHT.18 GPIO7`, `RIGHT.19 GPIO6` terhubung flash internal—jangan digunakan.

---

## 💻 Instalasi & Kompilasi

### Prasyarat Lingkungan
- Android Studio Ladybug / Koala atau lingkungan build berbasis Gradle.
- Android SDK API Level minimum: **26** (Android 8.0 Oreo).
- Android SDK Target: **API 35** (Android 15).
- Versi JDK: **OpenJDK 17 / 21**.

### Langkah Kompilasi
1. Clone repositori ke komputer lokal:
   ```bash
   git clone <repo-url>
   cd ns200-cdi-r8
   ```
2. Kompilasi APK debug:
   ```bash
   ./gradlew assembleDebug
   ```
3. Pasang APK ke perangkat Android fisik dengan dukungan Bluetooth Low Energy:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📝 Catatan Rilis (Changelog)

### Versi 9.2.0 (Ekosistem Data Komprehensif, Resolusi Presisi Soket J1, Interaktif Bluetooth Requirements Guard, & Estetika MoTeC Borderless)
- **Dokumentasi Ekosistem Data Lengkap & Keterlacakan Sumber Data Fisik**:
  - Penambahan bab komprehensif mengenai **Ekosistem Sumber Data Lengkap Aplikasi**:
    - **STATUS / BLE Link**: Terkelola reaktif melalui `BluetoothAdapter` dan `BluetoothGattCallback`, mendukung status `STANDBY` autentik saat mesin mati tanpa memicu false disconnect.
    - **BATT (Voltase Aki)**: Berasal dari jalur kunci kontak +12V (soket J1.5) via dioda schottky DREV dan pembagi tegangan presisi (27kΩ/10kΩ) ke pin analog MCU (PA5 STM32 / GPIO35 ESP32). Dikirim pada frame CORE byte 12–13 dalam satuan centivolt (centivolts/100).
    - **HV Center & HV Side (Tegangan Tinggi Kapasitor)**: Bersumber langsung dari tegangan kapasitor film C_CENTER dan C_SIDE (1.0µF 630V MKP) dari keluaran trafo inverter push-pull MOSFET IRF3205. Dibaca via pembagi tegangan 4x270kΩ / 8.2kΩ ke pin ADC PA6/PA7 (STM32 legacy) atau GPIO35/GPIO32 (ESP32: Center/Side) dan dikirim pada frame CORE byte 14–17 dalam satuan Volt integer.
    - **RPM & Sinyal Pulser**: Sinyal pick-up magnet stator kruk as (soket J1.10) difilter RC dan distabilkan oleh komparator presisi LM339/LM393 ke pin Timer Capture PA0 / GPIO4. Dihitung berdasarkan delta waktu mikrodetik dan dikirim pada frame CORE byte 6–7.
    - **TPS (Bukaan Gas)**: Sensor TPS karburator NS200 (soket J1.2 dan J1.4) dibaca pin ADC PA3/PA5 (STM32) atau GPIO36/GPIO34 (ESP32), dinormalisasi menjadi 0–1000 permille (0.0%–100.0%) pada frame CORE byte 8–9.
    - **Derajat Advance**: Dihasilkan real-time dari tabel interpolasi 32x16 timing matrix sesuai titik operasi RPM dan TPS, memicu gate SCR PA1/PA2 atau GPIO25/GPIO26, dikirim pada frame CORE byte 10–11 (°BTDC x 100).
    - **TEMP & Kipas**: Jalur sensor NTC silinder (soket J1.3) dan relay kipas (J1.7 / PB5 / GPIO13), dikirim pada frame DIAGNOSTIC.
- **Interaktif Bluetooth & Requirement Safety Prompt**:
  - Memperbaiki penanganan tombol koneksi (`KONEK`, `KONEK MAC`, dan `PINDAI BLE`): jika Bluetooth atau izin lokasi/perangkat dimatikan di perangkat pengguna, aplikasi secara proaktif meminta pengaktifan sistem melalui intent/dialog resmi (`BluetoothAdapter.ACTION_REQUEST_ENABLE`), bukan membiarkan tombol diam atau tidak merespons.
- **Perbaikan Layout Tombol & Tab Semi-Transparan Sudut Tegas**:
  - Tombol dan tab navigasi diselaraskan dengan estetika konsol balap profesional: sudut tegas presisi (`RoundedCornerShape(2.dp)` atau `3.dp`), latar belakang semi-transparan bergradasi halus, garis tepi tipis kontras tinggi, dan kontras teks yang tajam dan mudah dibaca tanpa menyita banyak ruang vertikal maupun horizontal.
- **Header Bar R9 Borderless**:
  - Menghilangkan bingkai latar belakang (*frame background/border*) pada badge petir `⚡ R9` di samping nama aplikasi pada header bar, menghasilkan keselarasan visual yang elegan dan menyatu dengan tipografi nama aplikasi.
- **Reposisi Label "ENGINE RPM" & Tacho Jarum Bebas Obstruksi**:
  - Penempatan label "ENGINE RPM" tepat di atas angka digital RPM dengan jarak aman dari poros jarum, memastikan jarum tachometer MoTeC dinamis dapat bergerak 100% bebas hambatan visual.
- **Stabilisasi Frekuensi BLE & Status STANDBY Autentik**:
  - Perhitungan frekuensi telemetri dengan filter jendela 2.000 ms yang mengeliminasi angka 2Hz semu akibat jitter frame awal.
  - Saat mesin mati dan telemetri biner standby, sistem mengidentifikasi status sebagai `STANDBY` (cyan elektrik) tanpa memicu putus-nyambung palsu.
- **Visualisasi Presisi Muka Soket Harness J1 (12 PIN) Bebas Terpotong (Zero-Clipping)**:
  - Rekonstruksi visual soket pigtail CDI NS200 12-pin dengan layout proporsional fleksibel (`Modifier.weight(1f)`), menjamin seluruh 12 pin (Baris 1: Pin 1–6, Baris 2: Pin 7–12) tampil utuh, seimbang, dan tidak pernah terpotong di semua resolusi layar ponsel.
  - Setiap pin dilengkapi nomor pin, label fungsi singkat (`NC`, `TPS A`, `TEMP`, `TPS B`, `+12V`, `SIDE`, `FAN`, `OEM S`, `OEM C`, `PULS`, `GND`, `CTR`), kode warna kabel motor pulsar NS200, dan status operasional (`DIGUNAKAN`, `KOSONG`, `CONFIRM`).
  - **Integrasi Tombol Pintas Tutorial Langkah 1 s/d 12**: Menekan tombol navigasi pada kartu pin langsung membuka langkah panduan workshop yang sesuai (Pin 1 ke Uji Isolasi Multimeter 6.1, Pin 5 ke Proteksi 12V 1.2, Pin 10 ke Komparator Pulser 2.5, Pin 12 ke Kapasitor Center 5.1, dst.).
- **Sistem Watchdog UI Telemetri (Visual Timeout 2.000 ms) & Tacho Pointer Dinamis**:
  - Mengatasi kendala jarum/indikator RPM menggantung jika paket data Bluetooth terhenti mendadak saat mesin mati: timer watchdog otomatis me-reset RPM, output flags, limiter, dan kualitas pulser ke 0 jika tidak ada frame GATT baru selama 2.000 ms.
- **Command Guard Keselamatan (`setup_can_write()`) & Banner Peringatan**:
  - Implementasi perlindungan firmware keselamatan reaktif `setup_can_write()` di layer aplikasi Android.
  - Memblokir pengiriman perintah penulisan setup atau konfigurasi kritis saat mesin hidup (RPM > 0), tegangan kapasitor HV masih tinggi (≥ 30V), antrean BLE sedang sibuk memproses paket, atau sedang dalam mode pembelajaran OEM Learn.
  - Banner peringatan merah (*Command Guard Warning Banner*) muncul secara otomatis di layar Setup menginformasikan alasan teknis pemblokiran sebelum pengguna melakukan kesalahan eksekusi.
- **Pemisahan Ketat Isolasi Mode Demo vs Mode Hardware Nyata (Strict Mode Separation)**:
  - Thread loop simulasi demo dilarang keras menyentuh atau memodifikasi telemetri nyata saat aplikasi tidak berada dalam mode simulasi (`_isSimulationMode == false`).
  - Fungsi `resetDemoState()` membersihkan seluruh state simulasi (slider, blip revving, suara engine sintetis) begitu koneksi BLE fisik diinisiasi.
- **Integritas Input Nyata & Penghapusan Tombol Uji Pulsa Buatan (+10)**:
  - Menghapus tombol uji pulsa simulasi dari tahap Checkpoint Setup dan ViewModel untuk menjamin 100% kepatuhan pada data input pulser fisik nyata dari optocoupler PC817 (J1.12 dan J1.6) serta sinkronisasi autentik antara hardware, firmware, dan software Android unit produksi.
  - Banner peringatan merah (*Command Guard Warning Banner*) muncul secara otomatis di layar Setup menginformasikan alasan teknis pemblokiran sebelum pengguna melakukan kesalahan eksekusi.
- **Pemisahan Ketat Isolasi Mode Demo vs Mode Hardware Nyata (Strict Mode Separation)**:
  - Thread loop simulasi demo dilarang keras menyentuh atau memodifikasi telemetri nyata saat aplikasi tidak berada dalam mode simulasi (`_isSimulationMode == false`).
  - Fungsi `resetDemoState()` membersihkan seluruh state simulasi (slider, blip revving, suara engine sintetis) begitu koneksi BLE fisik diinisiasi.
- **Branding Header "IgniTra CDI" & Styling Listrik**:
  - Header top bar resmi menampilkan branding **"IgniTra CDI"** dengan ikon petir (*electric bolt*) dan aksen warna gradien cyan listrik (*Electric Cyan*) serta amber busi (*Spark Amber*).
- **Penanganan Aktivasi GPS Otomatis (In-App GPS Dialog & Auto-Resume)**:
  - Mengatasi kendala pemindaian BLE yang kosong/gagal akibat Layanan Lokasi (GPS) ponsel yang nonaktif (khususnya pada Android 6–11 dan perangkat Android 12+ tertentu dengan proteksi vendor kernel).
  - Saat tombol "PINDAI BLE" ditekan dan Layanan Lokasi belum aktif, aplikasi langsung menampilkan dialog konfirmasi satu-sentuhan. Tombol "AKTIFKAN" langsung membuka switch pengaturan Lokasi sistem HP, dan begitu diaktifkan lalu pengguna kembali ke aplikasi, pemindaian BLE otomatis berjalan tanpa perlu menekan tombol ulang.
  - Scanner BLE dioptimalkan dengan `ScanSettings.SCAN_MODE_LOW_LATENCY` dan batas waktu 12 detik untuk responsivitas terbaik menangkap interval sinyal modul CDI.
- **Koneksi Cepat Langsung via MAC (100% Bebas GPS)**:
  - Menyediakan tombol "KONEK MAC" untuk menghubungkan aplikasi ke MAC address modul CDI secara langsung (`BluetoothDevice.connectGatt`). Jalur ini **100% tidak memerlukan Layanan Lokasi / GPS aktif** di semua versi Android, ideal bagi pengguna yang ingin menghemat daya baterai atau tidak ingin mengaktifkan GPS.
- **Perombakan Total Tombol & Komponen Bergaya Instrumentasi Balap MoTeC M1**:
  - Tombol **MotecButton**: Menggunakan sudut kotak/persegi tegas (`RoundedCornerShape(3.dp)`), warna semi-transparan (`alpha = 0.14f`), dan garis tepi (border) tajam `1.dp` dengan kontras tinggi khas konsol dashboard tuning MoTeC M1.
  - Kartu-kartu berbingkai sudut kotak tegas (`RoundedCornerShape(3.dp)`) menggantikan bentuk rounded melengkung yang tumpul.
- **Optimalisasi Layout Padat & Ramping (Zero Wasted Space)**:
  - Membatasi lebar kontainer pada `740.dp` dengan posisi terpusat di layar tablet dan ponsel lebar, mencegah tampilan melar dan menyisakan ruang kosong di tengah layar.
  - Memangkas kalimat-kalimat panjang yang bertele-tele menjadi terminologi teknis instrumentasi yang ringkas dan padat.
  - Matriks spesifikasi GATT ditata ulang dalam format grid 2 kolom yang hemat ruang dan rapi.
- **Koreksi & Klarifikasi UUID Profil GATT**:
  - Mengoreksi catatan dokumentasi versi terdahulu yang sebelumnya mencantumkan referensi serial generik SPP/HM-10 (`0000ffe0...`). Aplikasi resmi IGNITRA CDI R7/R8/R9 menggunakan arsitektur UUID 128-bit resmi:
    - Service: `7a8f1000-6c9d-4e40-a45f-0b4b4e533230`
    - Karakteristik Telemetri: `7a8f1001-6c9d-4e40-a45f-0b4b4e533230`
    - Karakteristik Command: `7a8f1002-6c9d-4e40-a45f-0b4b4e533230`
    - Karakteristik Response: `7a8f1003-6c9d-4e40-a45f-0b4b4e533230`
    - Karakteristik OTA Data/Status: `7a8f1004...` / `7a8f1005...`
- **Peta Rencana Lisensi Modul (Upcoming Roadmap)**:
  - Fitur penguncian MAC address modul dialokasikan untuk pembaruan mendatang sebagai fondasi sistem lisensi perangkat.

### Versi 9.1.0 (Rebranding Menjadi "IGNITRA CDI", Penyelarasan Alur BLE Hardware Scanner, & Perbaikan Pemindaian)
- **Rebranding Menyeluruh "IGNITRA CDI"**:
  - Memperbarui identitas aplikasi di Android (`app_name`, TopBar, `metadata.json`, `settings.gradle.kts`, `McuPlatform`, dan dokumentasi).
  - Teks header aplikasi resmi berganti dari "CDI-UNIVERSAL" menjadi **IGNITRA CDI** (dengan badge aksen R9).
- **Perbaikan & Penguatan Pemindaian BLE ("Gagal Memindai BLE")**:
  - **Penanganan Izin Runtime Android 12+ (API 31+) & Android 6–11**: Penambahan pengecekan dan permintaan izin `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, serta `ACCESS_FINE_LOCATION` dan `ACCESS_COARSE_LOCATION`. Callback permintaan izin kini secara otomatis melanjutkan aksi (*auto-resume*) saat izin disetujui (memulai scan, koneksi langsung, atau toggle connect) tanpa perlu menekan tombol ulang.
  - **Dukungan ScanSettings Low Latency**: Scanner BLE menggunakan `ScanSettings.SCAN_MODE_LOW_LATENCY` dan `setReportDelay(0)` untuk deteksi cepat dan andal pada semua chipset Android.
  - **Pesan Diagnostik Scan yang Jelas**: Menangani kode kegagalan BLE (`SCAN_FAILED_ALREADY_STARTED`, `SCAN_FAILED_APPLICATION_REGISTRATION_FAILED`, `SCAN_FAILED_INTERNAL_ERROR`, dll.) dengan pesan solusi yang informatif bagi pengguna.
  - **Penyelarasan Menu BLE HARDWARE CDI SCANNER**:
    - Pemindaian BLE utama memakai service UUID IgniTra. Jika UUID tidak muncul pada advertising selama 6 detik, aplikasi melakukan fallback nama `NS200-CDI`/`IGNITRA`; daftar semua perangkat hanya digunakan pada mode discovery diagnostik.
    - Dilengkapi **Filter Chips**: `Semua BLE` vs `Hanya Target CDI` (mendeteksi `"IGNITRA"`, `"IGNITRA-CDI"`, `"NS200-CDI"`, `"CDI"`).
    - Penanda visual badge jelas: `TARGET CDI` (hijau balap) vs `BLE LAIN` (abu-abu).
    - Tombol "KONEK" instan pada tiap kartu perangkat untuk menghubungkan perangkat target tanpa proses pairing/bonding manual.
- **Penyelarasan Alur ke Firmware Dual-MCU (ESP32 & STM32WB55)**:
  - Protokol komunikasi tetap mempertahankan struktur stabil: UUID Service `7a8f1000-6c9d-4e40-a45f-0b4b4e533230`, framing telemetri biner header `15 CD 03` (20-byte V3 Core/Diagnostic) pada frekuensi 20 Hz, serta format perintah teks ASCII berbasis koma (`GET,CAPS`, `SET,PROFILE`, `MAP,BEGIN`, `OTA,BEGIN`, dll.).

### Versi 9.0.0 (Standardisasi Identitas BLE "NS200-CDI", Integrasi Core R9 32x16, & Penyelarasan Firmware ESP32)
- **Standardisasi Identitas BLE Permanen ("NS200-CDI")**:
  - Menyeragamkan nama broadcast Bluetooth LE menjadi `"NS200-CDI"` murni tanpa embel-embel kode versi (`R7`, `R8`, dll.), baik di layer advertising firmware ESP32/STM32 maupun scanner Android.
  - Menjamin kompatibilitas jangka panjang (*forward-compatible*) sehingga pembaruan firmware di masa depan tidak lagi memerlukan perubahan kode deteksi nama perangkat di aplikasi.
- **Penyelarasan Total Protokol R9 v5 & Dimensi Peta 32x16**:
  - Mengonfirmasi struktur data `cdi_r5.h` pada spesifikasi penuh R9: `CDI_R5_RPM_POINTS` = 32 titik RPM, `CDI_R5_TPS_POINTS` = 16 titik TPS, `CDI_R5_ABSOLUTE_RPM_CAP` = 30.000 RPM, dan store version 5.
  - Sinkronisasi handler `handle_r9_map()` untuk pemrosesan perintah `MAP,BEGIN`, `MAP,RPM`, `MAP,LOAD`, `MAP,CELL`, `MAP,SAVE`, dan `MAP,SELECT`.
  - Dukungan penuh fitur baru R9: Live Dyno Advance Trim (`DYNO,BEGIN/TRIM/COMMIT`), Kontrol Kipas Otomatis 3-Titik NTC (`TEMP,CAL`, `SET,FAN`), Universal Engine Profile (`SET,PROFILE`), dan handshake kapabilitas v5 (`GET,CAPS`).
- **Penyelesaian Fatal Error Toolchain GCC 15 (ESP-IDF v6.1)**:
  - Memperbaiki ketidaksesuaian qualifier pointer pada `cdi_r5_protocol.c` di baris perintah `MAP,SELECT` (`select_save` non-const untuk `strtok_r`), meloloskan kompilasi 1169/1169 target dengan status bersih.
  - Menjaga modul waktu kritis `cdi_timebase` tetap 100% berjalan di IRAM internal dengan membersihkan atribut ganda pada header file untuk menghindari peringatan compiler.
- **Konfigurasi Partisi Dual OTA A/B Riil ESP32**:
  - Penerapan `partitions.csv` dengan skema partisi pabrik dan OTA ganda (`nvs` 24KB, `otadata` 8KB, `phy_init` 4KB, `factory` 1MB, `ota_0` 1MB, `ota_1` 1MB) pada flash 4MB.
  - Sinkronisasi file `sdkconfig.defaults` dan instruksi pembersihan cache build `sdkconfig` agar partisi A/B langsung aktif pada proses reconfigure.
  - Ukuran binary aplikasi terkompilasi `ns200_cdi_esp32.bin` sebesar 0x891b0 bytes (~548 KB), menyisakan ruang kosong aman sebesar 46% (0x76e50 bytes).
- **Penyelarasan Teks & Konsistensi UI Aplikasi Android**:
  - Memperbarui teks header status, kartu alur kontrol mode, panduan hardware, dan log terminal awal dari referensi lama menjadi format modern tanpa divergensi versi.
 
    
### Versi 8.3.2 (Audit Kontrak Firmware dan Wiring Dua Platform)
- Menyamakan parser/alur Android dengan firmware STM32 dan ESP32: `ACK,MODE` kini diikuti pembacaan ulang MODE, SETUP, dan STATUS; tahap READY dipertahankan sebagai nilai firmware 0–4.
- Menyamakan batas map dan limiter: 8x4/10.500 RPM untuk Normal, 16x8/11.500 RPM untuk PRO, advance maksimum 36°.
- Mengoreksi pin ESP32 dengan penomoran lokal `LEFT.n`/`RIGHT.n`, seluruh header STM32, jalur OEM, pickup, charger, fault, fan, strobe, dan feedback HV.
- Menghapus asumsi `JP_HV`, `JP_PRO`, `PB2 VIN_HV`, `SETUP,VOLTAGE`, dan `OTA,START` yang tidak ada pada kontrak firmware R8.
- Mendokumentasikan dua frame telemetri v3, respons LEARN yang sebenarnya, OTA BEGIN/COMMIT, target HV 285V/345V, fault 360V, dan status suhu/fan saat ini.
- Audit ini hanya menggunakan pembandingan sumber dan pemeriksaan statis; tidak menyatakan hasil build atau pengujian perangkat keras.

### Versi 8.3.1 (Perapihan Total Diagram Wiring & Kesiapan Produksi Dual-Platform STM32 & ESP32)
- **Standardisasi Simbol Panah Diagram Wiring (`>`)**:
  - Mengganti seluruh karakter panah visual grafik (`▶`) yang rentan merusak lebar kolom dan menyebabkan teks menyebar/berantakan di layar kecil dengan simbol ASCII panah standar `>` (misalnya: `Kabel J1.12 > Resistor 47k > PC817 Pin 1`).
  - Mengaktifkan kontainer horizontal scroll (`horizontalScroll`) pada seluruh kotak diagram wiring ASCII skematik (Modul PC817 4-Channel, Rangkaian Diskrit PC817 Dual-Koil, Voltage Divider BAT54S Clamp, dan Power Supply Step-Down Buck Converter) sehingga diagram tetap lurus, rapi, dan tidak terpotong atau wrap sembarangan pada layar ponsel potret.
- **Validasi Keakuratan Jalur Wiring Dinamis Dual-Platform (STM32 & ESP32)**:
  - Memastikan seluruh jalur pengkabelan, pin input/output, dan nama header secara dinamis beradaptasi sesuai mikrokontroler yang dipilih:
    - **STM32WB55 (WeAct 35-Pin)**:
      - Pulser Pickup: `PA0 (H_BOTTOM.9)`
      - Ignition Gates: `PA1 (H_BOTTOM.10)` (Center) & `PA2 (H_BOTTOM.11)` (Side)
      - OEM Learn Inputs: `PB3 (H_TOP.9)` (Center Capture) & `PB4 (H_TOP.8)` (Side Capture)
      - Fan Relay Driver: `PB5 (H_TOP.7)`
      - Power: `5V (H_BOTTOM.2)` & `GND_STAR (H_BOTTOM.1 / H_TOP.1)`
    - **ESP32-WROOM-32D (38-Pin)**:
      - Pulser Pickup: `GPIO4 (RIGHT.13)`
      - Ignition Gates: `GPIO25 (LEFT.9)` (Center) & `GPIO26 (LEFT.10)` (Side)
      - OEM Learn Inputs: `GPIO16 (RIGHT.12)` (Center Capture) & `GPIO17 (RIGHT.11)` (Side Capture)
      - Fan Relay Driver: `GPIO13 (LEFT.15)`
      - Power: `5V/VIN (LEFT.19)` & `GND (LEFT.14 / RIGHT.1)`
  - Baik pada Mode Demo (simulasi) maupun Mode Nyata (koneksi BLE ke hardware), seluruh jalur dan tahapan alur keselamatan dipastikan 100% konsisten dan bebas galat.
- **Kesiapan Produksi (Production Ready Verification)**:
  - Kompilasi build berhasil tanpa error (`compile_applet` PASS).
  - Seluruh rangkaian unit test berhasil diverifikasi (`testDebugUnitTest` PASS).
  - Konfigurasi rahasia build (`.env.example` & `BuildConfig`) dibersihkan sesuai standar keamanan produksi.

### Versi 8.3.0 (Ready Produksi: Pinout Dual-Platform STM32 & ESP32, Tabel 2-Kolom Presisi, & Visualisasi Fisik Board Otentik)
- **Desain Bebas Gap Ruang Kosong (Zero-Gap Layout Architecture)**:
  - Mengeliminasi gap ruang kosong vertikal berlebih pada menu Pinout MCU; menyederhanakan header menjadi compact platform bar yang ramping (~40.dp) dan menyatukan filter chips fungsional.
  - Menghilangkan redundansi banner platform ganda antara hub bengkel dan menu pinout, memberikan ruang pandang yang lapang dan teratur pada layar ponsel.
- **Konsistensi Tampilan Penuh Lintas Platform (STM32 & ESP32)**:
  - Menghadirkan kapabilitas mode tampilan ganda identik untuk kedua mikrokontroler:
    1. **Tabel Pinout 2-Kolom**: Membagi pin header sisi kiri dan kanan secara berdampingan dalam satu layar ponsel tanpa perlu horizontal scroll (`15 H_TOP x 20 H_BOTTOM` untuk STM32; `19 Kiri x 19 Kanan` untuk ESP32).
    2. **Visual Fisik Board Otentik**: Menampilkan visual board mikrokontroler secara realistis dan interaktif.
- **Visualisasi Fisik Board WeAct STM32WB55 (35-PIN) Berdasarkan Hardware Asli**:
  - Merekonstruksi canvas visual fisik board mengacu pada foto fisik WeAct Studio STM32WB55 Core Board asli:
    - PCB Dark Olive Green dengan pad gold-plated.
    - Port USB Type-C metal di sebelah kiri.
    - Tombol tactile ganda `NRST` (atas) dan `BOOT0` (bawah).
    - Header SWD Debug 4-Pin (`3V3`, `DIO`, `CLK`, `G`).
    - Chip QFN68 STM32WB55 dengan garis silkscreen putih dan dot Pin 1.
    - Kristal resonator HSE 32MHz dan jaringan filter RF kapasitor/induktor.
    - Silkscreen label autentik (`G`, `G`, `3V3`, `3V3`, `B7`... / `G`, `5V`, `5V`, `VB`, `H3`...).
    - Antena emas PCB meander inverted-F 2.4GHz BLE pada zona bebas logam di ujung kanan.
- **Visualisasi Fisik Board ESP32-WROOM-32D (38-PIN DevKitC V4)**:
  - Header 38-Pin lengkap (19 pin kiri + 19 pin kanan), pelat RF metal shield Espressif ESP-WROOM-32D, chip bridge UART CP2102, tombol EN & BOOT, serta port USB.
- **Detail Pin Terpilih & Sinkronisasi Real-Time**:
  - Mengetuk pin manapun pada tabel atau board fisik akan menampilkan kartu detail fungsi CDI NS200, jalur sirkuit lengkap, status proteksi, dan peringatan isolasi optik 3.3V secara instan.
- **Filter Fungsional Terpadu**:
  - Filter interaktif universal untuk kedua platform: `SEMUA`, `PULSER`, `GATE`, `OEM_LEARN`, `ADC/SENSOR`, `CHARGER`, `KRITIS`, serta filter fisik header.

### Versi 8.2.1 (Penyempurnaan UI Header & Visualisasi Dinamis Dual-Platform ESP32/STM32)
- **Header Top Bar Bersih & Terstruktur Rapi**:
  - Menghapus chip selector platform MCU redundan dan badge PCB dari header atas aplikasi agar layout tidak sesak dan teratur rapi.
  - Memfokuskan pemilihan platform secara terarah langsung di dalam menu Wiring Workshop & Setup.
- **Tombol KONEK & DEMO Bebas Tergenjet**:
  - Menerapkan batasan lebar minimum (`defaultMinSize(minWidth = 72.dp)`), padding seimbang, dan penonaktifan pembungkusan kata (`softWrap = false`, `maxLines = 1`) sehingga tombol **KONEK** / **PUTUS** / **SCAN** tidak akan terlipat secara vertikal.
  - Penataan status koneksi ringkas: `ONLINE`, `OFFLINE`, `SIMULASI DEMO`, `MEMINDAI...`, atau `MENGHUBUNGKAN...` (tanpa teks panjang yang memakan ruang).
- **Visualisasi Hardware Realistis ESP32 (`RealisticEsp32Board`)**:
  - Menghadirkan visualisasi board 30-pin ESP32-WROOM-32 DevKit V1 yang realistis (Micro-USB/CP2102, pelat metal shielding can RF Espressif, antena tembaga meander, tombol tactile EN & BOOT, dan pinout GPIO autentik).
- **Konsistensi Visualisasi Wiring & Diagram Simulator Lintas Platform**:
  - Visualizer skema wiring per langkah (`StepWiringVisualCanvas`) dan simulator pcb kumulatif (`FullCumulativeCircuitSimulator`) kini berganti secara instan dan konsisten mengikuti platform yang dipilih:
    - **STM32**: Menggunakan `RealisticWeActBoard` dengan pinout STM32WB55 (PA0 pulser, PA1/PA2 gate, PA3 TPS, PA4 suhu, PB0 VBAT, dll.) dan komparator pulser LM339.
    - **ESP32**: Menggunakan `RealisticEsp32Board` dengan pinout ESP32 (GPIO4 pulser via LM339N, GPIO25 center gate, GPIO26 side gate, GPIO36/VP TPS ADC1_CH0, GPIO39/VN Temp ADC1_CH3, GPIO33 VBAT ADC1_CH5, dll.).
- **Label Platform Ringkas & Jelas**:
  - Seluruh antarmuka beralih ke penamaan ringkas "STM32" atau "ESP32" tanpa teks panjang yang memecah baris.

### Versi 8.2.0 (Penyempurnaan UI Terstruktur, Telemetri Status Bar, & Diagram Pin Presisi)
- **Status Bar & Top Bar Rapi & Proporsional**:
  - Tombol **KONEK** dan **DEMO** kini memiliki batas lebar minimum terjamin (`minWidth = 76.dp`, `heightIn(min = 36.dp)`) dan *content padding* presisi sehingga tidak lagi terhimpit/tergencet pada berbagai resolusi layar.
  - Menghapus pemilih platform MCU yang berantakan dari header bar utama; pemilihan platform dipusatkan rapi pada workstation Wiring & Header tab.
  - Indikator status `ONLINE`/`OFFLINE` kini menampilkan data relevan ringkas secara *real-time*: frekuensi paket data aktual (`ONLINE • BLE • 50Hz`), status simulasi (`SIMULASI • 50Hz • <RPM> RPM`), atau status siap (`OFFLINE • BLE SIAP`).
- **Pembersihan Istilah & Konsistensi Platform MCU**:
  - Mengganti label panjang menjadi ringkas dan padat: cukup **STM32** dan **ESP32**.
  - Mengabstraksi dan membersihkan referensi lama "WeAct" dari diagram pin-ke-pin, checklist koneksi, dan visualizer header, sehingga saat memilih platform ESP32, seluruh diagram dan deskripsi langkah menampilkan pin GPIO ESP32 secara konsisten dan akurat.
- **Perbaikan Pemotongan Pin MCU pada PCB Kumulatif**:
  - Memperbaiki pemotongan visual pin mikrokontroler bagian bawah pada `FullCumulativeCircuitSimulator`:
  - Menyesuaikan tinggi kontainer utama FR4 perfboard menjadi 415.dp dan sub-blok (Harness, Logic PCB, Safety Gap, Power PCB) menjadi 380.dp.
  - Menambah ruang *breathing* visual pada `RealisticEsp32Board` dan `RealisticWeActBoard` (tinggi 185.dp, padding atas 6.dp dan bawah 8.dp) sehingga seluruh pin header, ring pad emas, lubang bor, dan label GPIO terlihat utuh dan tidak terpotong.
- **Validasi Alur Komunikasi Firmware**:
  - Memastikan seluruh kontrak data BLE (telemetri biner 20-byte, status flag, kalibrasi TDC, dan streaming OTA) selaras dengan alur kerja mikrokontroler siap produksi.

### Versi 8.1.0 (Rilis Arsitektur Modular & Pengaman OEM Learn R8)
- **Resampling Deterministik Grid Map MCU**:
  - Implementasi resampling linear deterministik dari titik kurva editor aplikasi ke grid firmware R8 (8x4 untuk slot Standar 0–2, dan 16x8 untuk slot PRO 3).
  - Tampilan editor UI tetap fleksibel dan konsisten, sementara payload `LIVE` dan `SAVE` yang dikirim ke flash MCU selalu presisi 100% mengikuti dimensi matrix firmware.
- **Katalog & Panduan Modul Siap Pakai di Pasaran**:
  - Menyediakan panduan lengkap penggantian blok diskrit dengan modul siap pakai di pasaran (*drop-in modules*) untuk memangkas kerumitan perakitan solderan hingga 85%.
  - Integrasi visual dan tutorial **Modul Optocoupler PC817 4-Channel** dengan terminal sekrup baut, LED indikator pulsa, dan jumper pull-up onboard untuk alur OEM Learn.
  - Panduan legacy memakai modul eksternal; PCB Rev C terbaru memakai MP1584, LM339N, serta BC337 + 1N4007 diskrit. Modul boost HV generik tetap tidak kompatibel karena tidak mengikuti kontrol PWM, feedback, serta target 285V/345V firmware.
  - Mempertahankan 100% kompatibilitas wiring soket harness bawaan NS200 12-pin (J1).
- **Kontrak Firmware R8 & Handshake Kapabilitas**:
  - Menambahkan handshake `GET,CAPS` saat koneksi BLE terhubung untuk mendeteksi kapabilitas firmware R8 (`PROTO4`, `OEM_LEARN`, `MANUAL`, `OTA`, `AUTO_FIRST_START`, `NO_JUMPERS`).
  - Menjaga keutuhan UUID BLE GATT dan struktur telemetri biner v3 (20-byte).
  - Menghapus asumsi pin lawas sebagai jumper fisik lama; pin sadap kini murni diakui sebagai probe pasif OEM Center & Side.
- **Penyempurnaan Alur Setup Checkpoint**:
  - Alur OEM Learn: Tahap Rekam Timing ➔ Konfirmasi Cabut Output OEM (`OEM_UNPLUGGED`) ➔ Tahap First Start Aman.
  - Layout Strobo lama diisolasi eksklusif hanya pada jalur `MANUAL` (darurat).
  - Logika verifikasi status: Aplikasi menunggu status `READY` nyata dari flash MCU (`t.ready == true` atau stage 4+), bukan berhenti hanya pada hitungan detik lokal.
- **Sakelar Tegangan PRO Aktif**:
  - Sakelar PRO kini memuat profil firmware R8 yang sesuai (`LOAD,<slot>`) dan memverifikasi sinkronisasi status ke MCU, memastikan tegangan pengisian kapasitor aktual berpindah antara 285 V dan 345 V.

### Versi 8.0.0 (Rilis Utama Firmware R8)
- **Dukungan Penuh Firmware R8**: Integrasi menyeluruh dengan arsitektur firmware terbaru Dual-Core MCU.
- **Pengunggah Firmware BLE OTA (image aplikasi target)**:
  - Menu pengunggah binary firmware langsung via BLE GATT di tab Terminal / Hex.
  - Pemeriksaan keselamatan preflight ketat: hanya dapat dimulai saat RPM = 0, koil OFF, dan HV < 30V.
  - Streaming chunk 208-byte dengan verifikasi checksum CRC32 dan tombol pembatalan.
- **Alur OEM Learn Pasif**:
  - Pembacaan sinyal timing asli dari CDI bawaan motor.
  - Status belajar dibaca melalui `GET,LEARN`: state, coverage, pulsa diterima/ditolak, sampel Side, dan offset Side.
  - Tombol kontrol *Mulai Learn* dan *Simpan & Stop*.
- **Mode Kontrol Firmware Fleksibel**:
  - Pilihan mode `OEM_LEARN`, `MANUAL` (strobo/TDC darurat), dan `DIY`.
  - Mode DIY dilindungi pengaman konfirmasi wajib `OEM_UNPLUGGED` untuk mencegah aktivasi simultan dengan CDI bawaan.
- **Otomatisasi FIRST START & Status READY**:
  - Deteksi idle stabil ≥3 detik pada mode aman (220V, center saja, limiter 3.000 RPM) otomatis mengunci kalibrasi.
  - Transisi status otomatis ke READY begitu mesin dimatikan atau pada proses booting berikutnya.
- **Dual Target Tegangan Tinggi (HV)**:
  - Dukungan target HV **NORMAL (285 V)** dan **PRO (345 V)** melalui `FEATURE,PRO,ON|OFF` dan profil slot (`LOAD,<slot>`).
  - Ambang batas proteksi tegangan berlebih disesuaikan ke 360 V untuk mendukung mode PRO 345 V.
- **Pembersihan Logika Interlock Hardware Lama**: Menghilangkan dependensi interlock fisik jumper dari firmware R8, digantikan dengan software safety checks dan monitoring pasif.

### Versi 7.2.3
- **Preservasi Status Quick Setup**: Mencegah resetting status workflow setup stage saat menerima frame telemetri v3 dari mikrokontroler.
- **Konfigurasi Pulser Lanjutan**: Pilihan Trigger Edge (`FALLING`/`RISING`), rasio pulsa 1–4 PPR, dan durasi trigger gate SCR (60–120 µs).
- **Kontrol Kipas Radiator Terintegrasi**: Mode kipas (`OFF`/`ON`/`AUTO`) untuk relai radiator dilengkapi safety interlock.
- **Statistik Paket & Integritas Real-Time**: Sliding window 5 detik untuk frekuensi paket aktual (`Hz`) dan rasio validitas CRC16 (`%`).
- **Indikator Kualitas Link BLE Dinamis**: Klasifikasi status kestabilan koneksi (`STABIL`, `CUKUP`, `BURUK`, `TERPUTUS`).
- **Unit Test Komprehensif**: Pengujian unit otomatis untuk algoritma CRC16-CCITT dan parser telemetri.

### Versi 7.2.2
- **Karakter Baru Moge 1800cc Super Bass**: Sintesis audio diperbarui ke irama 850 RPM stasioner slow-chug, subwoofer bass booster 38–75 Hz, dan saturasi empuk analog.
- **Simulasi Putar Tuas Gas (BLIP)**: Menghadirkan fungsi `triggerThrottleBlip` responsif (TPS 85%, lonjakan RPM kuadratik, raungan gas, dan inersia kembali ke idle).
- **Tacho Slider Cerdas**: Menahan RPM di mode demo dan mengikuti RPM motor di mode BLE nyata.
