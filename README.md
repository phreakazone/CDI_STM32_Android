# NS200 CDI R8 (v8.3.1) — Modern Android Tuning, Telemetry, Modular Hardware & OTA Suite

Aplikasi Android kendali terpadu untuk unit pengapian **CDI Programmable NS200-CDI-R8** (Bajaj Pulsar 200 DTS-i & Modifikasi Dual/Triple Spark). Menggabungkan kokpit telemetri balap gaya MoTeC, pemetaan kurva pengapian 4-slot dinamis, kalibrasi strobo pulser TDC, mode pembelajaran kurva asli (**OEM Learn Mode**), sistem pengunggah firmware nirkabel (**BLE OTA Firmware Uploader**), alur aktivasi mandiri aman (**Safe DIY Mode**), katalog modul jadi pasaran (*Commercial Off-the-Shelf Drop-in Modules*), bengkel panduan kabel interaktif, diagnostik paket data biner BLE, serta simulator akustik mesin knalpot multi-silinder (*Live Audio Engine Test Bench*).

Mendukung Arsitektur Lintas Platform (*Dual-Platform*): [**WeAct STM32WB55**](https://github.com/phreakazone/Firmware_CDI_NS200) dan [**ESP32 WROOM**](https://github.com/phreakazone/Firmware_CDI_NS200_ESP32).

---

## 📋 Daftar Isi
1. [Fitur Utama](#-fitur-utama)
2. [Pembaruan Besar Firmware R8 & Aplikasi v8.3.1](#-pembaruan-besar-firmware-r8--aplikasi-v831)
3. [Katalog & Panduan Modul Siap Pakai di Pasaran (Drop-In Modular Upgrade)](#-katalog--panduan-modul-siap-pakai-di-pasaran-drop-in-modular-upgrade)
4. [Daftar Belanja Komponen Lengkap (BOM) & Persamaan Substitusi Identik](#-daftar-belanja-komponen-lengkap-bom--persamaan-substitusi-identik)
5. [Arsitektur & Tumpukan Teknologi](#-arsitektur--tumpukan-teknologi)
6. [Detail Modul & Layar](#-detail-modul--layar)
   - [1. Dashboard MoTeC & Tacho Slider](#1-dashboard-motec--tacho-slider)
   - [2. Ignition Maps (4-Slot Timing)](#2-ignition-maps-4-slot-timing)
   - [3. Setup CDI, OEM Learn & Kalibrasi TDC](#3-setup-cdi-oem-learn--kalibrasi-tdc)
   - [4. Pengunggah Firmware BLE OTA (APP.bin)](#4-pengunggah-firmware-ble-ota-appbin)
   - [5. Live Audio Engine Test Bench & MOGE Super Bass](#5-live-audio-engine-test-bench--moge-super-bass)
   - [6. Quick Setup & Wiring Workshop](#6-quick-setup--wiring-workshop)
   - [7. BLE Terminal & Hex Diagnostics](#7-ble-terminal--hex-diagnostics)
7. [Protokol Komunikasi BLE Firmware R8 & Kontrak Android](#-protokol-komunikasi-ble-firmware-r8--kontrak-android)
8. [Skema Wiring Pinout & Transisi Fase (Konektor 12-Pin J1)](#-skema-wiring-pinout--transisi-fase-konektor-12-pin-j1)
9. [Instalasi & Kompilasi](#-instalasi--kompilasi)
10. [Catatan Rilis (Changelog)](#-catatan-rilis-changelog)

---

## 🚀 Fitur Utama

- **Koneksi Nirkabel BLE Ultra-Stabil**: Scanning otomatis, auto-reconnect, pengiriman perintah berbasis antrean (*queue-based write*), handshaking kapabilitas `GET,CAPS`, dan proteksi transisi mode bebas *ghost telemetry*.
- **Telemetri Balap Real-Time (20 Hz)**: Memantau RPM hingga 13.000, bukaan gas (TPS 0–100%), derajat *ignition advance* (° BTDC), tegangan pengisian kapasitor HV Center & Side (hingga 345V PRO), voltase aki (Cv), suhu mesin (°C), dan status *rev limiter*.
- **Mode Pembelajaran Mandiri (OEM Learn Pasif)**: Membaca pulsa pengapian CDI bawaan pabrik secara pasif melalui input mikrokontroler saat mesin hidup, merekam kurva pengapian asli motor secara otomatis.
- **Rangkaian Pengaman & Opsi Modul Pasaran**: Panduan visual interaktif rangkaian isolasi optik 4-channel PC817, modul buck DC-DC MP1584, modul boost HV ZVS 45–390V, serta modul komparator pulser LM393 siap pakai di pasaran untuk memangkas kerumitan perakitan solderan hingga 85%.
- **Pengunggah Firmware BLE OTA**: Memperbarui binary mikrokontroler (`APP.bin`) langsung lewat Bluetooth LE tanpa kabel, dilengkapi verifikasi CRC32 dan interlock keselamatan preflight.
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

## ⚡ Pembaruan Besar Firmware R8 & Aplikasi v8.3.1

| Fitur | Firmware R7 Lama | Firmware R8 / Aplikasi v8.3.1 Baru |
|---|---|---|
| **Alur Akuisisi Timing** | Wajib strobo manual / timing light tanda T | **OEM Learn Pasif**: Rekam kurva CDI OEM langsung via Input MCU. |
| **Pilihan Perakitan Hardware** | Wajib solder puluhan komponen diskrit di perfboard | **Drop-in Modular Ready**: Mendukung modul jadi pasaran (Modul PC817 4-ch, Modul Buck MP1584EN, Modul Boost ZVS HV 45-390V). |
| **Interlock Fisik** | Mengharuskan jumper `JP_HV`, `SW_ARM`, `JP_PRO` | **Software Interlock & Safety Confirmation**: Menghilangkan batasan saklar fisik; kontrol mode terpadu di aplikasi. |
| **Aktivasi Mode DIY** | Manual via jumper dan langkah rumit | **Safe DIY Mode**: Wajib konfirmasi `OEM_UNPLUGGED` (tidak ada pengambilalihan otomatis berbahaya). |
| **First Start & Ready** | Mengharuskan pencabutan jumper berulang kali | **Verifikasi Flash Nyata**: Deteksi idle stabil ≥3s mengunci kalibrasi, lalu aplikasi menunggu status `READY` nyata dari flash MCU. |
| **Level Tegangan HV** | Terkunci pada 240V–280V | **Dual Target R8 Aktif**: NORMAL (285 V) dan PRO (345 V) via pemuatan profil firmware R8 sesungguhnya. |
| **Handshake Protokol BLE** | Terbatas pada GET,STATUS | **Kontrak Lengkap**: Mengenali `GET,CAPS`, `MODE`, `LEARN`, `OTA`, sambil menjaga kompatibilitas UUID dan paket telemetri biner v3. |
| **Update Firmware MCU** | Wajib buka bodi & ST-Link V2 / DFU USB | **BLE OTA Flashing**: Unggah `APP.bin` langsung lewat aplikasi Android dengan proteksi CRC32. |

---

## 🛒 Katalog & Panduan Modul Siap Pakai di Pasaran (Drop-In Modular Upgrade)

Untuk mengurangi kerumitan wiring kabel dan solder-menyolder komponen diskrit, sistem **NS200 CDI R8** mendukung perakitan berbasis **Modul Jadi Siap Pakai di Pasaran** (*Commercial Off-The-Shelf Modules*).

> **PENTING**: Seluruh wiring kabel harness bawaan motor NS200 (konektor 12-pin J1) **tetap dipertahankan 100%**. Penggunaan modul bersifat opsional untuk menggantikan masing-masing blok fungsi internal di dalam boks CDI.

### Ringkasan Blok Fungsi & Modul Pengganti

> **FILOSOFI MODULAR R8**: Rekomendasi utama berfokus pada **2 MODUL JADI PASARAN (NOL PCB CUSTOM)** yang memangkas perakitan paling rumit, sementara blok yang kritis tetap mempertahankan performa komponen teruji.

| Blok Fungsi CDI | Status Rekomendasi | Modul Pasaran Siap Pakai | Estimasi Harga | Alasan Teknis & Keuntungan Utama |
|---|---|---|---|---|
| **1. OEM Learn Signal Isolator** | ⭐ **SANGAT DIREKOMENDASIKAN #1** | **Modul Optocoupler PC817 4-Channel Isolation Board** | Rp 12.000 – Rp 18.000 | Terminal sekrup (baut obeng), 4x LED indikator kedip pulsa, jumper pull-up onboard, isolasi optik 5000V. Cukup 1 modul untuk dua kanal sekaligus: sadapan Center (J1.12) dan Side (J1.6). |
| **2. Driver Relay Kipas** (J1.7 / Radiator Fan) | ⭐ **SANGAT DIREKOMENDASIKAN #2** | **Modul Relay 1-Channel 5V dengan Optocoupler** | Rp 8.000 – Rp 14.000 | Menggantikan transistor BC547 diskrit. Pin MCU langsung masuk ke pin `IN` modul. Sudah ada optoisolator, dioda flyback proteksi lonjakan motor kipas, dan terminal sekrup. |
| **3. Catu Daya Logic 5V** (+12V Kontak ke +5V MCU) | Alternatif Opsional | **Modul Mini DC-DC Buck MP1584EN / LM2596** | Rp 8.000 – Rp 15.000 | Menggantikan regulator linear panas. Menghasilkan 5.0V DC dingin & stabil untuk MCU. |

---

### ⚠️ Evaluasi Modul Pasaran yang DITOLAK (JANGAN DIGUNAKAN)

Setelah pengujian teknis mendalam terhadap karakteristik CDI kapasitif DTS-i, modul-modul berikut **TIDAK DIREKOMENDASIKAN**:

1. ❌ **Modul Boost Converter 12V → 300–1200V untuk Charge Pump HV**:
   - **Penyebab**: Kemampuan arus keluaran modul pasaran ini rata-rata hanya **2 – 20 mA**. Untuk sistem pengapian 3 busi (Triple Spark) pada putaran tinggi (10.000 RPM), sistem membutuhkan arus pengisian kapasitor minimal **80 – 120 mA**. Arus 2–20 mA tidak akan mampu mengisi kapasitor tepat waktu sehingga pengapian akan drop / misfire parah di putaran menengah ke atas. Selain itu, modul boost generik tidak memiliki pin kontrol PWM dari firmware untuk switching dinamis level tegangan 285V (Normal) dan 345V (Mode PRO).
2. ❌ **Modul Bridge Rectifier Generik**:
   - **Penyebab**: Modul penyearah jembatan generik di pasaran dirancang untuk frekuensi jala-jala listrik PLN (50/60 Hz), bukan frekuensi switching tinggi trafo frekuensi tinggi ATX/flyback (~100 kHz). Jika dipaksakan, dioda akan mengalami panas ekstrem (*thermal breakdown*) dan *forward voltage drop* yang tinggi. Gunakan dioda ultrafast diskrit seperti **UF4007** (trr < 75ns).
3. ❌ **Modul SCR / Dimmer AC**:
   - **Penyebab**: Rangkaian gerbang pemicu pada modul dimmer AC dirancang untuk arus bolak-balik AC 220V frekuensi rendah. Tidak responsif untuk pulsa trigger mikrodetik (60–100 µs) discharge kapasitor CDI DC. Tetap gunakan thyristor **BT151-800R** atau **TYN612**.
4. ❌ **Modul Sensor Tegangan Generik (Voltage Divider Module)**:
   - **Penyebab**: Modul sensor tegangan pasaran umumnya menggunakan rasio pembagi tetap (misal 5:1 untuk Arduino 5V 0–25V). Rasio ini tidak cocok dan tidak presisi untuk kalibrasi ADC 3.3V firmware pada pembacaan feedback tegangan HV (300V+), TPS, maupun sensor suhu NTC. Pembagi resistif terkalibrasi diskrit dengan dioda clamp pengaman **BAT54S** jauh lebih presisi dan aman.

---

### Detail Pemasangan Modul Optocoupler PC817 4-Channel (OEM Training / Learn)
Modul ini digunakan HANYA pada Fase 1 (OEM_LEARN) untuk membaca sinyal timing koil pengapian CDI OEM secara pasif dan aman tanpa risiko merusak mikrokontroler.

> **Catatan Sadapan Kabel OEM Learn**:
> Sinyal asli dari koil memiliki tegangan ratusan volt yang akan merusak MCU jika tidak diisolasi. Buat 2 kabel cabang/paralel (*pigtail probe*) dari soket motor:
> - **Kabel Sadap Utama** ➔ Diambil dari sambungan paralel **J1.12** (Koil Center OEM). Masuk ke IN1+ modul PC817 via R 47kΩ 2W.
> - **Kabel Sadap Samping** ➔ Diambil dari sambungan paralel **J1.6** (Koil Side OEM). Masuk ke IN2+ modul PC817 via R 47kΩ 2W.

```text
┌───────────────────────────────────────────────────────────────┐
│       MODUL OPTOCOUPLER PC817 4-CHANNEL ISOLATION BOARD       │
├───────────────────────────────┬───────────────────────────────┤
│   [TERMINAL INPUT KOIL OEM]   │     [TERMINAL OUTPUT MCU]     │
│                               │                               │
│ IN1+ ──[ R 47kΩ 2W ]── J1.12  │ OUT1 ──────> PIN INPUT CTR    │
│      (Kabel Sadapan J1.12)    │      (STM32 PB3 / ESP GPIO16) │
│ IN1- ───────────────── J1.11  │ OUT2 ──────> PIN INPUT SIDE   │
│      (GND Motor Massa)        │      (STM32 PB4 / ESP GPIO17) │
│                               │ OUT3 ──────  (Cadangan)       │
│ IN2+ ──[ R 47kΩ 2W ]── J1.6   │ OUT4 ──────  (Cadangan)       │
│      (Kabel Sadapan J1.6)     │                               │
│ IN2- ───────────────── J1.11  │ VCC  ──────> 3V3 (MCU)        │
│      (GND Motor Massa)        │ GND  ──────> GND (MCU)        │
├───────────────────────────────┴───────────────────────────────┤
│ [LED1] [LED2] [LED3] [LED4]  • Indikator Kedip Pulsa          │
│ [JP1]  [JP2]  [JP3]  [JP4]   • Jumper Output Level (Set VCC)  │
└───────────────────────────────────────────────────────────────┘
```

**Langkah & Tutorial Singkat**:
1. Siapkan 2 buah Resistor **47 kΩ 2 Watt** (wajib daya besar 2 Watt). Pasang secara seri pada kabel sebelum masuk ke terminal `IN1+` dan `IN2+` untuk menahan spike tegangan dari pulsa koil pengapian.
2. Sambungkan terminal `IN1-` dan `IN2-` ke Ground massa motor (`J1.11 GND`).
3. Beri daya modul sisi output dengan menyambungkan `VCC` ke Pin **3V3** MCU dan `GND` ke Pin **GND** MCU.
4. Pasang jumper JP1 & JP2 modul pada posisi **VCC** (pull-up internal aktif ke 3.3V).
5. Sambungkan terminal `OUT1` dan `OUT2` ke pin Input MCU yang sesuai (Lihat Bab Skema Wiring Pinout).
6. Saat mesin dihidupkan dengan CDI OEM, LED1 dan LED2 pada modul akan berkedip mengikuti percikan busi, dan counter pulsa di aplikasi Android akan bergerak naik!

---

## 🛒 Daftar Belanja Komponen Lengkap (BOM) & Panduan Bebas Salah Beli

> 💡 **PRINSIP PENTING (BEBAS KOMPONEN DOBEL)**:
> CDI R8 menggabungkan **Modul Jadi Siap Pakai di Pasaran** untuk blok yang rumit, dan **Komponen Diskrit Khusus** untuk blok pengapian tegangan tinggi (HV & SCR).
> 
> Komponen yang **SUDAH ADA di dalam modul jadi TIDAK PERLU dibeli terpisah**. Jangan membeli transistor, dioda, atau kapasitor untuk rangkaian yang sudah tertanam rapi di modul!

---

### 📦 BAGIAN A: Modul Jadi Siap Pakai di Pasaran (Drop-In Modules)
*Beli modul-modul ini utuh. Seluruh komponen internalnya (IC, transistor driver, dioda pengaman, LED, resistor pendukung, dan terminal baut) **SUDAH LENGKAP** di atas modul papan pabrikan, sehingga Anda **TIDAK PERLU** membelinya lagi secara terpisah.*

| Ref BOM | Jumlah | Modul Jadi Siap Pakai | Fungsi di Rangkaian CDI | Komponen yang SUDAH TERTANAM di Modul *(JANGAN Beli Terpisah!)* |
| :--- | :---: | :--- | :--- | :--- |
| **MOD_MCU** | **1 pcs** | **ESP32-WROOM-32D / 32E DevKitC (38-Pin)** *(atau WeAct STM32WB55CGU6)* | Otak kendali timing pengapian, kalkulasi kurva map, pembaca pulser, dan konektivitas BLE Android. | • Mikrokontroler Dual-Core + BLE onboard<br>• Regulator LDO 3.3V onboard<br>• Osilator Kristal onboard<br>• Tombol EN & BOOT onboard<br>• Port USB Type-C / Micro + IC Serial CH340/CP2102<br>• Kapasitor decoupling & filter daya |
| **MOD_BUCK** | **1 pcs** | **Modul Mini DC-DC Buck MP1584EN** *(atau Modul LM2596 Step Down)* | Menurunkan tegangan aki 12V motor menjadi **5.00V DC stabil & dingin** untuk menyuplai pin 5V board MCU. | • IC Switching Buck Converter<br>• Induktor Choke Ferrite onboard<br>• Kapasitor filter elko / keramik in-out<br>• Trimpot potensiometer pengatur voltase<br>• Dioda freewheeling |
| **MOD_RELAY** | **1 pcs** | **Modul Relay 1-Channel 5V dengan Optocoupler** | Menyalakan / mematikan kipas radiator motor (J1.7) secara otomatis berdasarkan suhu mesin (NTC). | • Relay 5V 10A<br>• **Transistor Driver Relay (BC547 / SS8050)**<br>• **Dioda Flyback peredam lonjakan induktif koil relay (1N4007)**<br>• Optocoupler pengaman isolasi sinyal MCU<br>• LED indikator Relay ON/OFF<br>• Terminal sekrup baut untuk kabel motor kipas |
| **MOD_LEARN** | **1 pcs** | **Modul Optocoupler PC817 4-Channel Isolation Board** | Menyadap pulsa timing CDI bawaan pabrik (OEM Learn) secara pasif tanpa mengganggu CDI asli (J1.12 Center & J1.6 Side). | • **4 buah IC Optocoupler PC817**<br>• 4 buah LED indikator kedip pulsa<br>• Resistor bias & pull-up onboard<br>• Jumper selektor level tegangan output<br>• Terminal sekrup baut (screw terminal) kabel sadapan |

---

### ⚡ BAGIAN B: Komponen Diskrit yang WAJIB Dibeli Terpisah
*Mengapa komponen ini tidak memakai modul jadi? Karena modul boost / inverter DC-DC 400V generik di pasaran arusnya terlalu kecil (hanya 2–20mA, tidak kuat melayani 3 busi di 10.000 RPM yang butuh 80–120mA), dan modul dimmer AC di pasaran tidak responsif terhadap pulsa mikrodetik CDI. Oleh karena itu, blok pengapian dan sensor di bawah ini **TIDAK ADA di modul mana pun** dan wajib dirakit diskrit di papan PCB:*

#### 1. Pelepasan Pengapian Koil Busi (Discharge SCR)
| Ref BOM | Jumlah | Komponen Utama | Rating Listrik | Persamaan / Substitusi Identik | Fungsi Khusus |
| :--- | :---: | :--- | :--- | :--- | :--- |
| **SCR1, SCR2** | **2 pcs** | **BT151-600R** (TO-220) | 600V, 12A RMS, IGT ~15mA | • **BT151-800R** (800V 12A)<br>• **BT152-800R** (16A 800V)<br>• **TYN612 / TYN812** | Saklar pelepasan tegangan tinggi ke koil busi. (1 pcs untuk Busi Tengah, 1 pcs untuk Busi Samping). |
| **C_CENTER, C_SIDE** | **2 pcs** | **1.0 uF 630V MKP / MPP** | Kapasitor Film Polypropylene Pulse Grade | • **1.5 uF 630V MKP** (*api lebih padat*)<br>• **1.0 uF 1000V DC MKP10**<br>• **CBB21 / CBB22 105J 630V** | Penampung energi percikan api busi. (1 pcs untuk Busi Tengah, 1 pcs untuk Busi Samping). |
| **RBLEED_C, RBLEED_S** | **8 pcs** | **470k Ohm 0.5W** | Metal Film / Carbon 0.5W | • 4 pcs **1 Mega Ohm 1W** seri (2 seri per bank)<br>• 8 pcs **510k Ohm 0.5W** | Disolder 4 seri per bank (total 1.88 MΩ) untuk mengosongkan sisa muatan kapasitor saat kontak mati demi keselamatan teknisi. |
| **QNC, QNS** | **2 pcs** | **BC547B** (TO-92 NPN) | 45V 100mA NPN | BC548B / 2N3904 / 2SC1815 | Driver pemicu gate SCR pengapian (1 pcs Center, 1 pcs Side). *Bukan untuk relay kipas!* |
| **QPC, QPS** | **2 pcs** | **BC557B** (TO-92 PNP) | 45V 100mA PNP | BC558B / 2N3906 / 2SA1015 | Penguat arus pemicu gate SCR pengapian (1 pcs Center, 1 pcs Side). |

#### 2. Pembangkit Tegangan Tinggi HV Inverter (Pengecas Kapasitor 285V / 345V)
| Ref BOM | Jumlah | Komponen Utama | Spesifikasi Listrik | Persamaan / Alternatif Donor | Fungsi Khusus |
| :--- | :---: | :--- | :--- | :--- | :--- |
| **QHV1, QHV2** | **2 pcs** | **IRF3205** (TO-220) | MOSFET N-Ch, 55V 110A, RDS(on) 8 mΩ | • **IRFB3077** (75V 120A 3.3 mΩ)<br>• **IRFB3206** (60V 120A)<br>• **IRF1404** (40V 162A) | Pasangan transistor switching push-pull primer trafo (bisa didonor dari sekunder PSU PC bekas). |
| **U4** | **1 pcs** | **TC4427A / TC4427CPA** (DIP-8) | Dual High-Speed MOSFET Driver Non-Inverting 1.5A | • **MIC4427**<br>• **UCC27524 / UCC27424** (TI Dual 4A-5A)<br>• **MCP1407** | Menggerakkan gerbang MOSFET QHV1 & QHV2 secara cepat dan presisi. |
| **T1 (Trafo)** | **1 pcs** | **Trafo ATX PC (EI-33 / EE-35)** | Lilitan sekunder 5V CT dijadikan input primer push-pull | Trafo bekas catu daya komputer ATX | Menaikkan 12V aki menjadi 285V / 345V AC frekuensi tinggi (~50–100 kHz). |
| **DREC1 – DREC4** | **4 pcs** | **UF4007** (DO-41) | 1A 1000V Ultrafast Rectifier (Trr < 75ns) | • **HER108** (1A 1000V 75ns)<br>• **SF18** (1A 1000V)<br>• **MUR1100** | Dioda penyearah jembatan (bridge rectifier) sekunder trafo. *(Dilarang pakai 1N4007 biasa karena akan mendidih di frekuensi tinggi).* |
| **DCH_C, DCH_S** | **2 pcs** | **UF4007** (DO-41) | 1A 1000V Ultrafast Rectifier | HER108 / SF18 / MUR1100 | Dioda pemisah isolasi pengisian Bank Center dan Bank Side agar letupan busi tengah tidak menguras kapasitor busi samping. |
| **TVS_Q1, TVS_Q2** | **2 pcs** | **1.5KE33A** (Through-hole) | TVS Diode 1500W 33V Unidirectional | • **P6KE33A** (600W 33V)<br>• **1.5KE36A / P6KE36A** (36V) | Meredam lonjakan tegangan induktansi bocor di kaki Drain MOSFET. *(Jangan gunakan 27V karena tegangan kerja normal push-pull mencapai ~30V).* |
| **RSENSE** | **1 pcs** | **0.05 Ohm 5W Non-Induktif** | Shunt Resistor pembaca arus trafo | • 2 pcs **0.1 Ohm 5W** paralel<br>• 5 pcs **0.22 Ohm 2W** paralel | Sensor deteksi arus berlebih untuk proteksi instan trafo (PWM Current Limiter). |

#### 3. Rangkaian Masukan Sinyal Pulser, Proteksi & Sensor Analog (Input ADC)
| Ref BOM | Jumlah | Komponen Utama | Rating / Tipe | Persamaan / Alternatif | Fungsi Khusus |
| :--- | :---: | :--- | :--- | :--- | :--- |
| **U2** | **1 pcs** | **LM339N** (DIP-14) | Quad Comparator | • **LM239 / LM139**<br>• **KA339 / HA17339** | Mengubah sinyal sinus AC pulser magnet pickup kruk as (J1.10) menjadi pulsa digital kotak untuk pin input MCU. |
| **DBAT** | **8 pcs** *(atau 16 pcs)* | **BAT54S** (SOT-23 SMD) | Dual Schottky Diode Clamp 30V 200mA | • Jika pakai through-hole: **16 pcs 1N5819 / BAT43 / 1N5711** | Membatasi tegangan seluruh pin ADC MCU agar tidak pernah melebihi 3.3V atau drop di bawah GND saat ada lonjakan kabel bodi motor. |
| **DCL_A, DCL_B** | **2 pcs** | **1N4148** (DO-35) | Dioda Fast Switching 100V 200mA | 1N4448 / 1N914 | Pengaman klem logika proteksi arus trafo ke IC driver. |
| **DREV** | **1 pcs** | **SB560** (DO-201AD) | 5A 60V Schottky Diode | SR560 / SS56 / SB5100 | Pengaman kutub aki terbalik di jalur input daya 12V (J1.5). |
| **TVS_IN** | **1 pcs** | **SMBJ33A / P6KE33A** | TVS Diode 33V Unidirectional | 1.5KE33A / 1.5KE36A | Menyerap lonjakan voltase transien dari spul / regulator kiprok motor. |
| **R_LEARN_EXT** | **2 pcs** | **47k Ohm 2 Watt** *(atau 8 pcs 12k 0.25W seri)* | Resistor Daya Khusus | Disolder seri pada kabel sadapan luar | Dipasang pada kabel sadapan koil motor (J1.12 & J1.6) **SEBELUM** masuk ke terminal input modul optocoupler untuk menahan tegangan kejut 300V. |

#### 4. Resistor Pembagi Tegangan & Bias (Metal Film 1% 0.25W)
| Nilai Resistor | Jumlah Butuh | Penempatan / Rangkaian |
| :--- | :---: | :--- |
| **270k Ohm 1%** | **8 pcs** | Feedback pembacaan tegangan tinggi HV (4 seri untuk Center, 4 seri untuk Side ke ADC MCU). *(Bisa diganti 8 pcs 220k atau 8 pcs 330k).* |
| **10 Ohm** | **2 pcs** | Snubber gerbang driver. |
| **100 Ohm** | **4 pcs** | Proteksi gerbang MOSFET dan filter sinyal pulser. |
| **330 Ohm** | **4 pcs** | Pembatas arus transistor pemicu gate SCR. |
| **1k Ohm** | **4 pcs** | Pulldown gate-katoda SCR1 & SCR2 (mencegah pemicuan liar dari noise). |
| **2.2k Ohm** | **2 pcs** | Jalur kolektor NPN ke basis PNP pemicu SCR. |
| **4.7k Ohm** | **4 pcs** | Resistor basis NPN & pullup pulser. |
| **8.2k Ohm** | **2 pcs** | Pembagi tegangan sensor suhu NTC. |
| **10k Ohm** | **6 pcs** | Pullup basis PNP & pulldown referensi ADC. |
| **15k Ohm** | **2 pcs** | Pembagi tegangan sensor bukaan gas TPS. |
| **22k Ohm** | **2 pcs** | Pembagi tegangan pembaca voltase aki (VBAT). |
| **27k Ohm** | **2 pcs** | Pembagi tegangan pembaca voltase aki (VBAT). |
| **39k Ohm** | **2 pcs** | Histeresis komparator pulser LM339. |
| **100k Ohm** | **2 pcs** | Pembagi ambang batas proteksi komparator. |
| **120k Ohm** | **2 pcs** | Feedback komparator proteksi tegangan lebih (OVP). |

#### 5. Kapasitor Filter & Bypass Diskrit
| Nilai Kapasitor | Jumlah Butuh | Penempatan / Rangkaian |
| :--- | :---: | :--- |
| **4.7nF (472)** | **2 pcs** | Filter derau frekuensi tinggi pada sinyal pulser pickup. |
| **10nF (103)** | **2 pcs** | Filter masukan ADC sensor analog. |
| **100nF / 0.1 uF (104)** | **8 pcs** | Kapasitor bypass VCC IC LM339, TC4427, dan rel tegangan IC. |
| **470 uF 35V / 50V Low-ESR** | **1 pcs** | Elko peredam ripple arus besar pada jalur input daya 12V aki (bisa didonor dari PSU PC). |

#### 6. Soket, Sikring & Aksesoris Perakitan
| Komponen Aksesoris | Jumlah Butuh | Keterangan & Catatan |
| :--- | :---: | :--- |
| **Soket Pigtail J1** | **1 pcs** | Soket 12-pin sambungan CDI khusus NS200 (gunakan kabel sambungan pigtail, jangan memotong kabel asli bodi motor). |
| **Kabel Busi & HV AWG 18-20** | **1 set (~2 meter)** | Kabel tembaga serabut tebal berisolasi tahan tegangan 600V untuk jalur koil busi J1.12, J1.6, dan output sekunder trafo. |
| **Rumah Sikring + Sekring Blade** | **3 set** | 1x **5A** (Sikring Utama FMAIN), 1x **1A** (Sikring Logic MCU FLOGIC), dan 1x **3A** (Sikring Inverter FHV). |
| **Induktor Choke Input (L_IN)** | **1 pcs** | Toroid ferit 47 uH dengan rating arus minimal 5A (bisa diambil langsung dari donor PSU PC). |
| **Pin Header Male 2.54mm** | **1 strip (40 pin)** | Untuk soket jumper konfigurasi, probe uji titik ukur (test point), dan kabel interkoneksi antar modul. |
| **Jumper Shunt 2.54mm** | **4 pcs** | Jumper setting mode & uji kalibrasi. |
| **Papan PCB Lubang Matrix** | **2 keping** | 1 keping ukuran **7 x 9 cm** (Papan Logic MCU & Sensor) dan 1 keping ukuran **5 x 7 cm** (Papan Khusus Power Inverter HV terpisah dengan celah isolasi 6mm). |

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
- **Tachometer Radial & Linear**: Visualisasi jarum analog digital hingga 13.000 RPM dengan indikator redline dinamis.
- **Panel Status Dual/Triple Spark**: Indikator busi utama (Center Plug) dan busi sekunder (Side Plugs) aktif berkedip sesuai sinyal pemantik.
- **Telemetry Readout Matrix**:
  - `ADVANCE`: Derajat pengapian (° BTDC)
  - `TPS`: Persentase bukaan gas (0–100%)
  - `BATTERY`: Tegangan aki motor (contoh: 14.1V)
  - `HV CAP`: Tegangan kapasitor discharge CDI (hingga 345V pada mode PRO)
  - `TEMP`: Estimasi suhu mesin
  - `MODE FIRMWARE`: Indikator mode aktif (OEM_LEARN / MANUAL / DIY)
- **Interactive Tacho Slider**: Pengontrol putaran mesin simulasi di mode demo, dan pengikut RPM motor di mode BLE.
- **Tombol Aksi Cepat**: Preset instan `IDLE (1.4K)`, `5K`, `8K`, `LIMITER`, serta tombol interaktif `BLIP GAS`.

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

### 4. Pengunggah Firmware BLE OTA (APP.bin)
Pembaruan firmware nirkabel terintegrasi di tab BLE Terminal:
- **Interlock Keselamatan Preflight**: Tombol upload terkunci otomatis kecuali syarat aman terpenuhi:
  - `RPM == 0` (mesin wajib mati).
  - `Output Coils == OFF` (koil pemantik tidak aktif).
  - `HV Center & Side < 30V` (kapasitor daya tinggi sudah terkuras aman).
- **Transmisi Chunk Cerdas**: Mengirim binary `APP.bin` per-blok melalui BLE GATT write without response, dilengkapi progress bar persentase, total byte terkirim, dan tombol pembatalan seketika.
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
  - Pilihan mode: `OFF`, `ON`, dan `AUTO` dengan safety interlock.
- **Soket CDI 12-pin**: Kode warna kabel asli NS200, jalur koil sekunder, dan sensor TPS.

### 7. BLE Terminal & Hex Diagnostics
Diagnostik teknis tingkat lanjut:
- **Statistik Paket Real-Time (Sliding Window 5 Detik)**: `packetRateHz` (18–22 Hz) dan integritas `crcValidPercent`.
- **Indikator Kualitas Sambungan BLE (`LinkQuality`)**: STABIL (Hijau), CUKUP (Kuning), BURUK (Merah), TERPUTUS (Abu-abu).
- **Monitor Paket Heksadesimal Mentah**: Menampilkan 20 bytes data v3 lengkap dengan penyorotan warna per field.
- **Konsol Manual Perintah CDI**: Terminal input untuk eksekusi perintah teks ASCII MCU dan log respons.

---

## 📡 Protokol Komunikasi BLE Firmware R8 & Kontrak Android

Aplikasi berkomunikasi melalui BLE GATT Custom Service:

- **Service UUID**: `7a8f1000-6c9d-4e40-a45f-0b4b4e533230`
- **Telemetry Characteristic UUID (Notify 20 Hz)**: `7a8f1001-6c9d-4e40-a45f-0b4b4e533230`
- **Command Characteristic UUID (Write)**: `7a8f1002-6c9d-4e40-a45f-0b4b4e533230`
- **Response Characteristic UUID (Notify ASCII Stream)**: `7a8f1003-6c9d-4e40-a45f-0b4b4e533230`
- **Format Telemetri Paket Biner v3 (20 Bytes)**:
  - `[0..1]` : Magic Header `0x15, 0xCD` (NS200 CDI Identifier)
  - `[2]` : Protocol Version (`0x03`)
  - `[3]` : Frame Kind (`0` = CORE, `1` = EXTRA)
  - `[4..5]` : Sequence Counter (16-bit LE)
  - `[6..7]` : Engine Speed RPM (16-bit LE)
  - `[8..9]` : Throttle Position Sensor ADC/Raw (16-bit LE)
  - `[10..11]` : Spark Advance (centi-degree BTDC, 16-bit LE)
  - `[12..13]` : Battery Voltage (centi-volt, 16-bit LE)
  - `[14..15]` : HV Center Capacitor Voltage (16-bit LE)
  - `[16..17]` : HV Side Capacitor Voltage (16-bit LE)
  - `[18..19]` : CRC16-CCITT Checksum (Polinomial `0x1021`, Initial `0xFFFF`, Final XOR `0x0000`)

### Perintah Teks Firmware R8
- **Handshake Kapabilitas**:
  - `GET,CAPS` : Menanyakan kapabilitas firmware R8 (`CAPS,OK,OEM_LEARN,PRO_HV,OTA`).
- **Mode & Pembelajaran**:
  - `MODE,OEM_LEARN` : Mengaktifkan mode belajar timing pasif dari CDI OEM.
  - `MODE,MANUAL` : Mengaktifkan mode manual/strobo darurat.
  - `MODE,DIY,OEM_UNPLUGGED` : Mengaktifkan operasi CDI mandiri setelah soket OEM dicabut.
  - `LEARN,START` : Memulai perekaman pulsa pengapian OEM.
  - `LEARN,STOP` : Menghentikan perekaman dan menyimpan timing ke flash.
  - Respons pulsa: `@<seq>,LEARN,PULSES,<center_count>,<side_samples>`
- **Pengaturan Tegangan R8**:
  - `SETUP,VOLTAGE,NORMAL` : Mengatur target HV ke 285 V (penggunaan standar/harian).
  - `SETUP,VOLTAGE,PRO` : Mengatur target HV ke 345 V (kompetisi balap).
  - `LOAD,<slot>` : Memuat profil slot pengapian sesuai target voltase aktif.
- **Protokol OTA Firmware**:
  - `OTA,START,<file_size>,<crc32_hex>` : Inisialisasi proses update firmware.
  - Pengiriman paket data biner 208-byte via kanal BLE.
  - `OTA,ABORT` : Pembatalan darurat upload firmware.
- **Perintah Setup & Status Umum**:
  - `GET,STATUS` : Meminta status telemetri lengkap.
  - `GET,SETUP` : Sinkronisasi penuh parameter setup workflow.
  - `GET,META` : Meminta metadata firmware dan build string.
  - `SETUP,EDGE,<FALLING|RISING>` : Mengatur trigger edge pulser.
  - `SETUP,PPR,<1-4>` : Mengatur rasio pulsa per putaran.
  - `SETUP,GATE_US,<60|80|100|120>` : Mengatur durasi pulsa SCR gate.
  - `SETUP,FAN,<OFF|ON|AUTO>` : Mengatur mode kontrol relai kipas radiator.
  - `SETUP,FIRST_START` : Memulai mode first start aman (220V, auto-lock 3s).
  - `SETUP,READY,CENTER` / `SETUP,READY,THREE,<offset>` : Tahap pengaktifan output penuh.
  - `SETUP,RESET,CONFIRM` : Reset alur setup ke tahap awal.

---

## 🔌 Skema Wiring Pinout & Transisi Fase (Konektor 12-Pin J1)

Tabel ini memetakan fungsi kabel harness bawaan motor NS200 ke pin yang tepat untuk platform STM32 maupun ESP32, guna menghilangkan segala bentuk ambiguitas operasional.

| J1 | Fungsi | Warna Kabel | Pin STM32 | Pin ESP32 | Deskripsi Kelistrikan & Routing |
|:--:|:-------|:------------|:----------|:----------|:--------------------------------|
| 1  | NC | Kosong / NC | - | - | Tidak terhubung. Isolasi rapi. |
| 2  | TPS_A | Hijau-Putih | PA3 / PA5 | **GPIO36** | Input sensor bukaan gas pasangan A. |
| 3  | TEMP | Hitam-Putih | PA4 | **GPIO39** | Input sensor suhu mesin NTC (Pull-up 4.7k ke 5V). |
| 4  | TPS_B | Abu-Abu | PA3 / PA5 | **GPIO34** | Input sensor bukaan gas pasangan B / Referensi. |
| 5  | +12 V kontak | Cokelat | - | - | Input daya utama kunci kontak ON. Melewati penurun tegangan ke 5V. |
| 6  | COIL_SIDE | Hitam-Merah | **PA2** | **GPIO26** | **OUTPUT (DIY):** Menembak koil samping via SCR driver menuju J1.6. |
| 7  | FAN_RELAY | Biru-Kuning | PB5 | **GPIO13** | Output kendali relay kipas radiator otomatis. |
| 8  | OEM_SIDE | Kosong / NC | **PB4** | **GPIO17** | **INPUT (Learn):** Menyadap pulsa koil samping pabrik via Optocoupler dari kabel **J1.6**. |
| 9  | OEM_CTR | Kosong / NC | **PB3** | **GPIO16** | **INPUT (Learn):** Menyadap pulsa koil utama pabrik via Optocoupler dari kabel **J1.12**. |
| 10 | PULSER | Putih-Merah | **PA0** | **GPIO4** | Input sensor magnet. Tersambung permanen ke MCU via modul komparator LM393. |
| 11 | GND | Hitam-Kuning | GND | GND | Ground utama massa motor. |
| 12 | COIL_CENTER | Oranye | **PA1** | **GPIO25** | **OUTPUT (DIY):** Menembak koil tengah via SCR driver menuju J1.12. |

### ⚠️ PERHATIAN: Transisi Hardware (Fase LEARN ➔ Fase DIY)
Untuk menghindari benturan arus driver koil dan memastikan keselamatan mikrokontroler, fungsionalitas pin J1.12 dan J1.6 diperlakukan berbeda secara fisik sesuai fasenya.

**FASE 1: Penyadapan Pasif (Mode OEM_LEARN)**
Pada fase ini, **CDI bawaan pabrik (OEM) WAJIB tetap menancap di soket motor** dan mengendalikan mesin. Mikrokontroler bertindak murni sebagai PENDENGAR (Input).
1. **Jalur Input (Wajib Pasang):** Kabel Pulser (J1.10) terhubung permanen ke pin pembaca pulser (PA0 / GPIO4).
   - Kabel Sadap koil tengah diambil dengan cara menyambung paralel kabel dari **J1.12** ➔ Optocoupler PC817 ➔ Pin Pembaca (PB3 / GPIO16).
   - Kabel Sadap koil samping diambil dengan menyambung paralel dari **J1.6** ➔ Optocoupler PC817 ➔ Pin Pembaca (PB4 / GPIO17).
2. **Jalur Output (Wajib Terputus):** Pin penembak koil MCU (PA1/PA2 atau GPIO25/GPIO26) **TIDAK BOLEH** tersambung ke koil. Pin ini dibiarkan menggantung bebas.

**FASE 2: Pengambilalihan Penuh (Mode DIY / FIRST_START)**
Pada fase ini, **CDI bawaan pabrik (OEM) WAJIB dicabut secara fisik dari soket motor**. Mikrokontroler kini bertindak sebagai PENEMBAK (Output) yang mengontrol pengapian secara penuh.
1. **Konfirmasi Cabut CDI Pabrik:** Buka aplikasi Android, ubah mode ke DIY, dan centang konfirmasi bahwa soket OEM telah dilepas (`OEM_UNPLUGGED`).
2. **Jalur Input (Wajib Lepas):** Pin pembaca koil penyadap (PB3/PB4 atau GPIO16/GPIO17) dilepas/diabaikan dari rangkaian karena CDI OEM sudah dicabut.
3. **Jalur Output (Wajib Pasang):** Pin penembak koil MCU (PA1/PA2 atau GPIO25/GPIO26) dihubungkan permanen ke sirkuit SCR menuju soket jalur **J1.12** dan **J1.6** untuk memicu busi secara mandiri.

---

### 📐 Detail Header Pinout MCU (STM32WB55 35-Pin & ESP32 38-Pin)

Menu **Pinout MCU** dalam aplikasi menyediakan visualisasi ganda (**Mode Tabel 2-Kolom Ringkas** dan **Mode Visual Fisik Board**) yang dapat dipilih sesuai preferensi saat perakitan:

#### 1. WeAct Studio STM32WB55 Core Board (35 PIN)
- **Header Atas (H_TOP, 15 Pin)**:
  - `H_TOP.1` - `H_TOP.2`: `GND`
  - `H_TOP.3` - `H_TOP.4`: `3V3` (Output LDO 3.3V internal, max 300mA)
  - `H_TOP.5`: `PB7` (Status LED onboard)
  - `H_TOP.6`: `PB6` (UART1_TX debug telemetri kabel)
  - `H_TOP.7`: `PB5` (Relay Kipas Radiator J1.7 via ULN2003 / Opto)
  - `H_TOP.8`: `PB4` (Sadap Koil Samping OEM Learn via Opto PC817)
  - `H_TOP.9`: `PB3` (Sadap Koil Tengah OEM Learn via Opto PC817)
  - `H_TOP.10`: `PA15` (JTDI / GPIO serbaguna)
  - `H_TOP.11`: `PA10` (UART1_RX debug telemetri kabel)
  - `H_TOP.12`: `PA9` (TC4427 PWM Driver Charger B)
  - `H_TOP.13`: `PA8` (MCO / Jam clock external)
  - `H_TOP.14`: `PB9` (Strobe Light Output / Kalibrasi TDC)
  - `H_TOP.15`: `PB8` (TC4427 PWM Driver Charger A)
- **Header Bawah (H_BOTTOM, 20 Pin)**:
  - `H_BOTTOM.1`: `GND`
  - `H_BOTTOM.2` - `H_BOTTOM.3`: `5V` (Input Catu Daya 5.00V dari Buck Converter MP1584)
  - `H_BOTTOM.4`: `VBAT` (Peringatan keras: Khusus baterai koin RTC 3V, **DILARANG** ke aki 12V!)
  - `H_BOTTOM.5`: `PH3` (BOOT0 pin)
  - `H_BOTTOM.6`: `PB2` (TPS Throttle Backup ADC2)
  - `H_BOTTOM.7`: `PB0` (HV Sense Monitor ADC1_IN15 via pembagi tegangan 100:1)
  - `H_BOTTOM.8`: `PA7` (Temp Sensor ADC1_IN12)
  - `H_BOTTOM.9`: `PA6` (TPS B ADC1_IN11)
  - `H_BOTTOM.10`: `PA5` (TPS A ADC1_IN10)
  - `H_BOTTOM.11`: `PA4` (V_BATT 12V ADC1_IN9)
  - `H_BOTTOM.12`: `PA3` (MAP Sensor ADC1_IN8)
  - `H_BOTTOM.13`: `PA2` (Gate SCR Koil Samping J1.6)
  - `H_BOTTOM.14`: `PA1` (Gate SCR Koil Tengah J1.12)
  - `H_BOTTOM.15`: `PA0` (Pulser Pick-Up J1.10 via Hardware Capture TIM2_CH1)
  - `H_BOTTOM.16` - `H_BOTTOM.17`: `3V3`
  - `H_BOTTOM.18` - `H_BOTTOM.20`: `GND`

#### 2. ESP32-WROOM-32D DevKitC V4 (38 PIN)
- **Header Sisi Kiri (Pin 1–19)**:
  - `Pin 1`: `3V3` | `Pin 2`: `EN` | `Pin 3`: `GPIO36/VP` (TPS A ADC1) | `Pin 4`: `GPIO39/VN` (Temp ADC1)
  - `Pin 5`: `GPIO34` (TPS B ADC1) | `Pin 6`: `GPIO35` (MAP ADC1) | `Pin 7`: `GPIO32` (HV Sense ADC1)
  - `Pin 8`: `GPIO33` (V_BATT 12V Sense ADC1) | `Pin 9`: `GPIO25` (Gate Koil Tengah)
  - `Pin 10`: `GPIO26` (Gate Koil Samping) | `Pin 11`: `GPIO27` (Status LED) | `Pin 12`: `GPIO14` (Strobe Light TDC)
  - `Pin 13`: `GPIO12` (Bootstrap Pin) | `Pin 14`: `GND` | `Pin 15`: `GPIO13` (Relay Kipas Radiator)
  - `Pin 16`: `GPIO09` | `Pin 17`: `GPIO10` | `Pin 18`: `GPIO11` | `Pin 19`: `5V/VIN`
- **Header Sisi Kanan (Pin 20–38)**:
  - `Pin 20`: `GND` | `Pin 21`: `GPIO23` (Bench SPI/MOSI) | `Pin 22`: `GPIO22` (I2C SCL Display)
  - `Pin 23`: `GPIO01/TX0` | `Pin 24`: `GPIO03/RX0` | `Pin 25`: `GPIO21` (I2C SDA Display)
  - `Pin 26`: `GND` | `Pin 27`: `GPIO19` (PWM Charger B) | `Pin 28`: `GPIO18` (PWM Charger A)
  - `Pin 29`: `GPIO05` (Bench Trigger) | `Pin 30`: `GPIO17` (Sadap OEM Samping via PC817)
  - `Pin 31`: `GPIO16` (Sadap OEM Tengah via PC817) | `Pin 32`: `GPIO04` (Pulser LM393 Interrupt)
  - `Pin 33`: `GPIO00` (Bootloader Button) | `Pin 34`: `GPIO02` (Onboard Blue LED)
  - `Pin 35`: `GPIO15` (Bootstrap) | `Pin 36`: `GPIO08` | `Pin 37`: `GPIO07` | `Pin 38`: `GPIO06`

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

### Versi 8.3.1 (Perapihan Total Diagram Wiring & Kesiapan Produksi Dual-Platform STM32 & ESP32)
- **Standardisasi Simbol Panah Diagram Wiring (`>`)**:
  - Mengganti seluruh karakter panah visual grafik (`▶`) yang rentan merusak lebar kolom dan menyebabkan teks menyebar/berantakan di layar kecil dengan simbol ASCII panah standar `>` (misalnya: `Kabel J1.12 > Resistor 47k > PC817 Pin 1`).
  - Mengaktifkan kontainer horizontal scroll (`horizontalScroll`) pada seluruh kotak diagram wiring ASCII skematik (Modul PC817 4-Channel, Rangkaian Diskrit PC817 Dual-Koil, Voltage Divider BAT54S Clamp, dan Power Supply Step-Down Buck Converter) sehingga diagram tetap lurus, rapi, dan tidak terpotong atau wrap sembarangan pada layar ponsel potret.
- **Validasi Keakuratan Jalur Wiring Dinamis Dual-Platform (STM32 & ESP32)**:
  - Memastikan seluruh jalur pengkabelan, pin input/output, dan nama header secara dinamis beradaptasi sesuai mikrokontroler yang dipilih:
    - **STM32WB55 (WeAct 35-Pin)**:
      - Pulser Pickup: `PA0 (H_BOTTOM.15)`
      - Ignition Gates: `PA1 (H_BOTTOM.14)` (Center) & `PA2 (H_BOTTOM.13)` (Side)
      - OEM Learn Inputs: `PB3 (H_TOP.9)` (Center Capture) & `PB4 (H_TOP.8)` (Side Capture)
      - Fan Relay Driver: `PB5 (H_TOP.7)`
      - Power: `5V (H_BOTTOM.2)` & `GND_STAR (H_BOTTOM.1 / H_TOP.1)`
    - **ESP32-WROOM-32D (38-Pin)**:
      - Pulser Pickup: `GPIO4 (Pin 32)`
      - Ignition Gates: `GPIO25 (Pin 9)` (Center) & `GPIO26 (Pin 10)` (Side)
      - OEM Learn Inputs: `GPIO16 (Pin 31)` (Center Capture) & `GPIO17 (Pin 30)` (Side Capture)
      - Fan Relay Driver: `GPIO13 (Pin 15)`
      - Power: `5V/VIN (Pin 19)` & `GND (Pin 14 / Pin 20)`
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
    - **STM32**: Menggunakan `RealisticWeActBoard` dengan pinout STM32WB55 (PA0 pulser, PA1/PA2 gate, PA4 TPS, PB0 VBAT, dll.) dan komparator pulser LM339.
    - **ESP32**: Menggunakan `RealisticEsp32Board` dengan pinout ESP32 (GPIO4 pulser via PC817 opto, GPIO25 center gate, GPIO26 side gate, GPIO36/VP TPS ADC1_CH0, GPIO39/VN Temp ADC1_CH3, GPIO33 VBAT ADC1_CH5, dll.).
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
  - Panduan modul pelengkap: **Modul Buck MP1584EN / LM2596** (+12V ke +5V, menjaga suplai logic WeAct tetap aktif saat mesin mati sesaat pada transisi OEM Learn agar data RAM tidak hilang), **Modul Boost ZVS HV 45-390V** (Pengecas Kapasitor 285V/345V), **Modul Komparator LM393 Speed Sensor** (Pulser Pick-up J1.10), dan **Modul Relay Opto 1-Channel** (Kipas Radiator J1.7).
  - Mempertahankan 100% kompatibilitas wiring soket harness bawaan NS200 12-pin (J1).
- **Kontrak Firmware R8 & Handshake Kapabilitas**:
  - Menambahkan handshake `GET,CAPS` saat koneksi BLE terhubung untuk mendeteksi kapabilitas firmware R8 (`OEM_LEARN`, `PRO_HV`, `OTA`).
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
- **Pengunggah Firmware BLE OTA (`APP.bin`)**:
  - Menu pengunggah binary firmware langsung via BLE GATT di tab Terminal / Hex.
  - Pemeriksaan keselamatan preflight ketat: hanya dapat dimulai saat RPM = 0, koil OFF, dan HV < 30V.
  - Streaming chunk 208-byte dengan verifikasi checksum CRC32 dan tombol pembatalan.
- **Alur OEM Learn Pasif**:
  - Pembacaan sinyal timing asli dari CDI bawaan motor.
  - Indikator counter pulsa Center dan sampel Side secara langsung (@seq,LEARN,PULSES,...).
  - Tombol kontrol *Mulai Learn* dan *Simpan & Stop*.
- **Mode Kontrol Firmware Fleksibel**:
  - Pilihan mode `OEM_LEARN`, `MANUAL` (strobo/TDC darurat), dan `DIY`.
  - Mode DIY dilindungi pengaman konfirmasi wajib `OEM_UNPLUGGED` untuk mencegah aktivasi simultan dengan CDI bawaan.
- **Otomatisasi FIRST START & Status READY**:
  - Deteksi idle stabil ≥3 detik pada mode aman (220V, center saja, limiter 3.000 RPM) otomatis mengunci kalibrasi.
  - Transisi status otomatis ke READY begitu mesin dimatikan atau pada proses booting berikutnya.
- **Dual Target Tegangan Tinggi (HV)**:
  - Dukungan seleksi tegangan HV **NORMAL (285 V)** dan **PRO (345 V)** via perintah BLE `SETUP,VOLTAGE`.
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
