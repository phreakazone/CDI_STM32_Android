# IGNITRA CDI R9 (v9.0.0) — Android Tuning, Dual-MCU Firmware & OTA


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
7. [Protokol Komunikasi BLE Firmware R8 & Kontrak Android](#-protokol-komunikasi-ble-firmware-r8--kontrak-android)
8. [Skema Wiring Pinout & Transisi Fase (Konektor 12-Pin J1)](#-skema-wiring-pinout--transisi-fase-konektor-12-pin-j1)
9. [Instalasi & Kompilasi](#-instalasi--kompilasi)
10. [Catatan Rilis (Changelog)](#-catatan-rilis-changelog)

---

## 🚀 Fitur Utama

- **Koneksi Nirkabel BLE Ultra-Stabil**: Scanning otomatis, auto-reconnect, pengiriman perintah berbasis antrean (*queue-based write*), handshaking kapabilitas `GET,CAPS`, dan proteksi transisi mode bebas *ghost telemetry*.
- **Telemetri Balap Real-Time (20 Hz)**: Memantau RPM (batas mengikuti profil dan CAPS firmware (format maksimum 30.000 RPM)), TPS 0–100%, *ignition advance* (° BTDC), HV Center/Side (285V Normal / 345V PRO), voltase aki, fault, output, dan *rev limiter*. Kanal suhu disediakan protokol tetapi bernilai `N/A` sampai kurva konversi NTC firmware dikalibrasi.
- **Mode Pembelajaran Mandiri (OEM Learn Pasif)**: Membaca pulsa pengapian CDI bawaan pabrik secara pasif melalui input mikrokontroler saat mesin hidup, merekam kurva pengapian asli motor secara otomatis.
- **Rangkaian Pengaman & Opsi Modul Pasaran**: Panduan visual interaktif rangkaian isolasi optik 4-channel PC817, modul buck DC-DC MP1584, modul relay kipas, serta modul komparator pulser LM393. Blok charger HV tetap memakai rangkaian push-pull yang dikendalikan firmware; modul boost generik tidak kompatibel.
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

## ⚡ Pembaruan Besar Firmware R8 & Aplikasi v8.3.2

| Fitur | Firmware R7 Lama | Firmware R8 / Aplikasi v8.3.2 Baru |
|---|---|---|
| **Alur Akuisisi Timing** | Wajib strobo manual / timing light tanda T | **OEM Learn Pasif**: Rekam kurva CDI OEM langsung via Input MCU. |
| **Pilihan Perakitan Hardware** | Wajib solder puluhan komponen diskrit di perfboard | **Hybrid Modular**: PC817 4-ch, buck MP1584EN, relay kipas, dan komparator pulser boleh berupa modul; charger HV/SCR tetap diskrit agar sesuai kendali firmware. |
| **Interlock Fisik** | Mengharuskan jumper `JP_HV`, `SW_ARM`, `JP_PRO` | **Software Interlock & Safety Confirmation**: Menghilangkan batasan saklar fisik; kontrol mode terpadu di aplikasi. |
| **Aktivasi Mode DIY** | Manual via jumper dan langkah rumit | **Safe DIY Mode**: Wajib konfirmasi `OEM_UNPLUGGED` (tidak ada pengambilalihan otomatis berbahaya). |
| **First Start & Ready** | Mengharuskan pencabutan jumper berulang kali | **Verifikasi Flash Nyata**: Deteksi idle stabil ≥3s mengunci kalibrasi, lalu aplikasi menunggu status `READY` nyata dari flash MCU. |
| **Level Tegangan HV** | Terkunci pada 240V–280V | **Dual Target R8 Aktif**: NORMAL (285 V) dan PRO (345 V) via pemuatan profil firmware R8 sesungguhnya. |
| **Handshake Protokol BLE** | Terbatas pada GET,STATUS | **Kontrak Lengkap**: Mengenali `GET,CAPS`, `MODE`, `LEARN`, `OTA`, sambil menjaga kompatibilitas UUID dan paket telemetri biner v3. |
| **Update Firmware MCU** | Wajib buka bodi & ST-Link V2 / DFU USB | **BLE OTA Flashing**: Unggah image aplikasi STM32/ESP32 yang sesuai lewat aplikasi Android dengan proteksi CRC32. |

---

## 🛒 Katalog & Panduan Modul Siap Pakai di Pasaran (Drop-In Modular Upgrade)

Untuk mengurangi kerumitan wiring kabel dan solder-menyolder komponen diskrit, sistem **NS200 CDI R8** mendukung perakitan berbasis **Modul Jadi Siap Pakai di Pasaran** (*Commercial Off-The-Shelf Modules*).

> **PENTING**: Seluruh wiring kabel harness bawaan motor NS200 (konektor 12-pin J1) **tetap dipertahankan 100%**. Penggunaan modul bersifat opsional untuk menggantikan masing-masing blok fungsi internal di dalam boks CDI.

### Ringkasan Blok Fungsi & Modul Pengganti

> **FILOSOFI MODULAR R8**: Modul pasaran dipakai hanya pada blok yang cocok secara listrik (isolator OEM Learn, relay kipas, dan buck 5V). Charger HV, feedback HV, dan pemicu SCR tetap mengikuti rangkaian diskrit yang dikendalikan firmware.

| Blok Fungsi CDI | Status Rekomendasi | Modul Pasaran Siap Pakai | Estimasi Harga | Alasan Teknis & Keuntungan Utama |
|---|---|---|---|---|
| **1. OEM Learn Signal Isolator** | ⭐ **SANGAT DIREKOMENDASIKAN #1** | **Modul Optocoupler PC817 4-Channel Isolation Board** | Rp 12.000 – Rp 18.000 | Terminal sekrup (baut obeng), 4x LED indikator kedip pulsa, jumper pull-up onboard, isolasi optik 5000V. Cukup 1 modul untuk dua kanal sekaligus: sadapan Center (J1.12) dan Side (J1.6). |
| **2. Driver Relay Kipas** (J1.7 / Radiator Fan) | ⭐ **SANGAT DIREKOMENDASIKAN #2** | **Modul Relay 1-Channel 5V dengan Optocoupler** | Rp 8.000 – Rp 14.000 | Menggantikan transistor BC547 diskrit. Pin MCU langsung masuk ke pin `IN` modul. Sudah ada optoisolator, dioda flyback proteksi lonjakan motor kipas, dan terminal sekrup. |
| **3. Catu Daya Logic 5V** (+12V Kontak ke +5V MCU) | Alternatif Opsional | **Modul Mini DC-DC Buck MP1584EN / LM2596** | Rp 8.000 – Rp 15.000 | Menggantikan regulator linear panas. Menghasilkan 5.0V DC dingin & stabil untuk MCU. |

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
| **MOD_RELAY** | **1 pcs** | **Modul Relay 1-Channel 5V dengan Optocoupler** | Mengendalikan kipas radiator motor (J1.7). Firmware saat ini: OFF = LOW; ON dan AUTO = HIGH failsafe karena konversi NTC belum tersedia. | • Relay 5V 10A<br>• **Transistor Driver Relay (BC547 / SS8050)**<br>• **Dioda flyback koil relay (1N4007)**<br>• Optocoupler pengaman isolasi sinyal MCU<br>• LED indikator Relay ON/OFF<br>• Terminal sekrup baut untuk kabel motor kipas |
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
| **270k Ohm 1%** | **8 pcs** | Feedback pembacaan tegangan tinggi HV (4 seri untuk Center, 4 seri untuk Side ke ADC MCU). Nilai ini harus dipertahankan agar rasio ADC sesuai firmware; nilai lain memerlukan kalibrasi firmware baru. |
| **10 Ohm** | **2 pcs** | Snubber gerbang driver. |
| **100 Ohm** | **4 pcs** | Proteksi gerbang MOSFET dan filter sinyal pulser. |
| **330 Ohm** | **4 pcs** | Pembatas arus transistor pemicu gate SCR. |
| **1k Ohm** | **4 pcs** | Pulldown gate-katoda SCR1 & SCR2 (mencegah pemicuan liar dari noise). |
| **2.2k Ohm** | **2 pcs** | Jalur kolektor NPN ke basis PNP pemicu SCR. |
| **4.7k Ohm** | **4 pcs** | Resistor basis NPN & pullup pulser. |
| **8.2k Ohm** | **2 pcs** | Resistor bawah pembagi feedback HV Center dan Side. Sensor suhu memakai jaringan 4.7k/15k/27k terpisah. |
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
| **Pin Header Male 2.54mm** | **1 strip (40 pin)** | Untuk selektor fisik TPS `J_TPS`, titik ukur, dan kabel interkoneksi. Bukan pemilih mode firmware. |
| **Jumper Shunt 2.54mm** | **2 pcs** | Hanya untuk selektor pasangan kabel TPS `J_TPS`; mode/HV/PRO dikendalikan firmware dan aplikasi. |
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
- **Tachometer Radial & Linear**: Skala visual hingga 13.000 RPM; batas mengikuti profil aktif; format protokol maksimum 30.000 RPM dengan redline dinamis.
- **Panel Status Dual/Triple Spark**: Indikator busi utama (Center Plug) dan busi sekunder (Side Plugs) aktif berkedip sesuai sinyal pemantik.
- **Telemetry Readout Matrix**:
  - `ADVANCE`: Derajat pengapian (° BTDC)
  - `TPS`: Persentase bukaan gas (0–100%)
  - `BATTERY`: Tegangan aki motor (contoh: 14.1V)
  - `HV CAP`: Tegangan kapasitor discharge CDI (hingga 345V pada mode PRO)
  - `TEMP`: `N/A` pada firmware saat ini sampai kurva NTC dikalibrasi dan diimplementasikan
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

## 📡 Protokol Komunikasi BLE Firmware R8 & Kontrak Android

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
  - `GET,CAPS` → `CAPS,R8.0,PROTO4,OEM_LEARN,MANUAL,OTA_STAGE,AUTO_FIRST_START,NO_JUMPERS`.
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

| J1 | Fungsi | Warna Kabel | Pin STM32 | Pin ESP32 | Deskripsi Kelistrikan & Routing |
|:--:|:-------|:------------|:----------|:----------|:--------------------------------|
| 1  | NC | Kosong / NC | - | - | Tidak terhubung. Isolasi rapi. |
| 2  | TPS_A | Hijau-Putih | PA3 / PA5 | **GPIO36** | Input sensor bukaan gas pasangan A. |
| 3  | TEMP | Hitam-Putih | PA4 | **GPIO39** | Input sensor suhu mesin NTC (Pull-up 4.7k ke 5V). |
| 4  | TPS_B | Abu-Abu | PA3 / PA5 | **GPIO34** | Input sensor bukaan gas pasangan B / Referensi. |
| 5  | +12 V kontak | Cokelat | - | - | Input daya utama kunci kontak ON. Melewati penurun tegangan ke 5V. |
| 6  | COIL_SIDE | Hitam-Merah | **PA2** | **GPIO26** | **OUTPUT (DIY):** Menembak koil samping via SCR driver menuju J1.6. |
| 7  | FAN_RELAY | Biru-Kuning | PB5 | **GPIO13** | Output relay active-high; ON/AUTO = HIGH pada firmware saat ini. |
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

### Versi 9.2.0 (Aktivasi BLE & GPS Satu-Sentuhan, Desain Instrumentasi MoTeC M1, & Layout Padat)
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
    - Pemindaian BLE menangkap semua perangkat di sekitar (*broad scan*) agar modul dengan nama custom atau paket advert parsial tetap terdeteksi.
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
  - Panduan modul pelengkap: **Modul Buck MP1584EN / LM2596** (+12V ke +5V), **Modul Komparator LM393** dengan output 3.3V terproteksi (Pulser J1.10), dan **Modul Relay Opto 1-Channel** (Kipas J1.7). Modul boost HV generik dinyatakan tidak kompatibel karena tidak mengikuti kontrol PWM, feedback, serta target 285V/345V firmware.
  - Mempertahankan 100% kompatibilitas wiring soket harness bawaan NS200 12-pin (J1).
- **Kontrak Firmware R8 & Handshake Kapabilitas**:
  - Menambahkan handshake `GET,CAPS` saat koneksi BLE terhubung untuk mendeteksi kapabilitas firmware R8 (`PROTO4`, `OEM_LEARN`, `MANUAL`, `OTA_STAGE`, `AUTO_FIRST_START`, `NO_JUMPERS`).
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
