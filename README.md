# IgniTra CDI R9 — Aplikasi Android

Aplikasi Android resmi untuk konfigurasi, commissioning, telemetry, diagnosis, tuning, BLE OTA, dan buku petunjuk IgniTra CDI ESP32 R9 Modular.

Dokumen ini adalah satu-satunya dokumentasi repository aplikasi. Spesifikasi firmware dan hardware kanonik berada di repository [Firmware_CDI_NS200_ESP32](https://github.com/phreakazone/Firmware_CDI_NS200_ESP32). Repository aplikasi tidak lagi menyimpan generator PCB atau skematik EasyEDA lama.

## Status rilis saat ini

| Item | Nilai |
|---|---|
| Target firmware | IgniTra R9 ESP32 |
| Firmware acuan | 9.6.3, build 20260925 |
| Protocol | 5 |
| Telemetry | v3, 20 byte |
| Android minimum | sesuai `app/build.gradle.kts` |
| Transport CDI | Bluetooth Low Energy |
| Receiver suara | Classic Bluetooth A2DP eksternal |
| UI | Technical Dashboard / MoTeC, tema gelap |

Firmware lama tetap dapat masuk jalur read-only/legacy jika query tambahan tidak didukung. Aplikasi tidak boleh menganggap teks `R9` saja cukup untuk kompatibilitas; keputusan fitur memakai VERSION, CAPS, schema respons, MODULES, dan AUX.

## Fungsi utama

- Dashboard RPM, TPS, advance, tegangan aki, HV Core/Center, HV Side, suhu, fan, output dan fault.
- Empat slot ignition map dengan grid hingga 32 RPM × 16 TPS sesuai CAPS/PROFILE.
- Limiter, live tuning, dyno trim, profile mesin, fan otomatis, kalibrasi suhu, TPS, pickup dan TDC.
- Setup tiga layar: Pemasangan, Pemeriksaan, First Start/Ready.
- Deteksi fisik modul SIDE, THERMAL, OEM Learn, AUX dan TPS Diagnostic.
- Kontrol kontak/keyless/starter satu tombol berdasarkan status nyata firmware.
- Mode timing STANDARD, SOFT, RESPONSIVE, KUDA, DRUMBAND, FOMO dan CUSTOM.
- BLE OTA dengan status transfer dan verifikasi firmware.
- Suara mesin virtual berbasis RPM melalui receiver A2DP eksternal.
- Buku Petunjuk di dalam aplikasi untuk pemasangan, setup dan troubleshooting.
- User agreement tuning lanjutan bersifat peringatan dan pencatatan persetujuan; tidak mengunci map berdasarkan tipe motor.

## Prinsip lintas kendaraan dan kebebasan tuning

Nama produk tetap **IgniTra CDI R9**. NS200 dipertahankan sebagai preset awal dan kendaraan uji, bukan batas arsitektur. Kendaraan lain memakai profil kendaraan tersendiri; transmisi MANUAL/MATIC dan siklus mesin 2T/4T adalah atribut terpisah. Pickup, PPR 1–12, gate 40–150 µs, TDC, limiter, map, live timing, dan profil idle wajib mengikuti mesin nyata. Dashboard selalu menampilkan PPR dan gate yang benar-benar aktif.

Pengguna tetap dapat memakai seluruh rentang yang diiklankan CAPS. Aplikasi hanya memberi peringatan knocking/panas/kickback sebelum tuning; penolakan otomatis dibatasi pada keadaan yang dapat merusak elektronik atau membuat transaksi tidak konsisten, seperti flash saat mesin hidup/HV aktif, FAULT_N, koneksi belum sinkron, dan starter tanpa interlock yang dikonfigurasi.

## Alur aplikasi

### State sesi BLE

| State | Arti | Izin tulis |
|---|---|---|
| DISCONNECTED | Tidak ada CDI | Tidak |
| SCANNING | Mencari perangkat | Tidak |
| CONNECTING | Membuka GATT | Tidak |
| SUBSCRIBING | Mengaktifkan notification | Tidak |
| SYNCING | Membaca kondisi firmware | Tidak |
| NEEDS_BINDING | Serial belum cocok dengan binding lokal | Tidak |
| READY_READ_ONLY | Telemetry/diagnosis tersedia | Tidak |
| READY_FULL | Sinkron dan binding cocok | Mengikuti safety firmware |
| DEGRADED | Query wajib gagal atau paket salah | Tidak |

Mode demo dan sesi perangkat nyata adalah state terpisah. Ketika CDI nyata tersambung, generator demo dihentikan dan UI hanya menampilkan data firmware.

### Handshake setelah connect

Setelah Response dan Telemetry berhasil disubscribe, aplikasi melakukan query berurutan:

```text
PING
GET,INFO
GET,VERSION
GET,IDENTITY
GET,CAPS
GET,HARDWARE
GET,MODULES
GET,COMMISSION
GET,SETUP
GET,STATUS
GET,META
GET,PROFILE
GET,TEMP
GET,ADC
GET,MODE
GET,LEARN
GET,OTA
GET,TIMING
GET,AUX
```

Query tambahan yang tidak didukung firmware lama ditandai unsupported, bukan dianggap sebagai putus koneksi. Tombol tulis hanya aktif ketika sesi READY_FULL, binding cocok, data masih segar, capability tersedia, dan Command Guard mengizinkan.

## Mode Demo realistis

Demo dimulai dari Core baru dalam kondisi standby: mesin mati, aki 12,6 V, HV 0 V, optional module belum terpasang, dan Setup belum selesai. Pengguna dapat memasang modul secara virtual, menjalankan seluruh alur Setup, menyalakan/mematikan mesin, menguji throttle, suara, serta profil timing idle. Stage Demo memakai enum firmware yang sama (`NEW=0`, `PICKUP=1`, `TDC=2`, `FIRST_START=3`, `READY=4`). STANDARD, SOFT, RESPONSIVE, KUDA, DRUMBAND, FOMO, dan CUSTOM benar-benar mengubah osilasi RPM dan advance simulasi sesuai intensitas/rentangnya; bukan hanya mengganti label. State Demo tidak pernah disalin menjadi status perangkat nyata.

## Setup pertama

### 1. Pemasangan

- Pilih Core 1 Coil untuk keluaran J1.12.
- Pilih Dual Coil hanya jika modul SIDE dan keluaran J1.6 benar-benar dipasang.
- Konfirmasi CDI OEM telah dilepas.
- OEM Learn berada di menu lanjutan, bukan syarat pemasangan normal.

Perintah:

```text
SETUP,INSTALL,CORE,OEM_REMOVED
SETUP,INSTALL,DUAL,OEM_REMOVED
```

Status Layar 1 tidak memakai stage pickup sebagai tanda pemasangan. Aplikasi hanya menghijaukan pemasangan setelah `ACK,INSTALL_CORE`/`ACK,INSTALL_DUAL` atau setelah `GET,MODE` mengonfirmasi mode Independen dengan OEM dilepas. Pemilihan metode commissioning opsional sesudahnya tidak memalsukan seolah hardware kembali tercabut. `ERR` dan timeout tidak pernah menaikkan progres; alasan penolakan ditampilkan dalam bahasa pengguna.

Metode commissioning bersifat opsional:

- **Quick Install Core/Dual**: jalur utama; langsung pasang IgniTra dan lanjut pemeriksaan.
- **OEM Learn**: opsional dan memerlukan modul OEM Learn untuk merekam karakter CDI bawaan.
- **Manual**: opsional untuk kalibrasi pickup/TDC langsung tanpa merekam CDI OEM.
- **Independen**: mode operasi normal yang aktif otomatis setelah Quick Install mengonfirmasi CDI OEM dilepas.

Tab berikutnya tetap dapat dibuka sebagai pratinjau, tetapi berwarna amber dan tombol yang membutuhkan prasyarat tetap terkunci. TPS dan modul opsional dapat dikonfigurasi kemudian tanpa membatalkan Setup Core.

### 2. Pemeriksaan

1. Starter beberapa detik untuk membaca pickup.
2. Lepas starter dan tunggu RPM 0 serta HV di bawah 30 V.
3. Simpan edge/pickup.
4. Kalibrasi TDC dengan tanda mekanis yang benar.
5. Simpan TPS CLOSED dan OPEN.

### 3. First Start/Ready

1. Pastikan mesin berhenti dan HV di bawah 30 V.
2. Aktifkan FIRST START.
3. Firmware membatasi 3.000 RPM dan advance maksimum 10°.
4. Setelah mesin stabil, matikan mesin.
5. Tunggu RPM 0 dan HV di bawah 30 V.
6. Simpan READY CENTER atau READY DUAL.
7. UI baru menampilkan selesai bila `COMMISSION.ready=1`.

## Modul hardware

Modul adalah lima bit independen dan dapat dipasang bersamaan.

| Bit | Modul | Tampilan aplikasi |
|---:|---|---|
| 0 | SIDE | HV Side J1.6 dan status dual-coil |
| 1 | THERMAL | Suhu, ambang dan fan |
| 2 | OEM_LEARN | Menu learn lanjutan |
| 3 | AUX | Kontak, starter, strobe dan profil kendaraan |
| 4 | TPS_DIAG | ADC raw, referensi dan diagnosis TPS |

`MODULES,2` membedakan modul hadir secara fisik, aktif, teramati, fault, serta konfigurasi tersimpan. Bila modul dilepas, switch UI mengikuti presentMask dan kembali tidak aktif. configuredMask lama hanya menjadi peringatan; aplikasi tidak boleh memalsukan modul masih terpasang.

Perubahan modul hanya diizinkan saat RPM 0 dan HVC/HVS di bawah 30 V. Setelah perubahan, aplikasi membaca ulang MODULES, SETUP, TEMP dan STATUS.

## Uji ESP32 tanpa Core

Uji koneksi BLE dengan ESP32 saja diperbolehkan untuk memeriksa protokol dan tampilan. Nilai aki/HV/sensor dapat tidak valid karena pembagi tegangan dan pull-down berada pada Core; kondisi itu **bukan** alasan aplikasi memutus GATT. Konfirmasi `SETUP,INSTALL` diproses firmware di worker terpisah dari callback NimBLE agar commit NVS tidak memutus BLE. Untuk komisioning listrik, pengukuran HV, coil, fan, keyless, dan starter, Core/modul fisik tetap wajib terpasang.

## Kontak fisik, keyless dan starter

### Profil kendaraan

| Profil | J1.9 | Aturan starter |
|---|---|---|
| NS200 | MODE_REQ/interlock OEM | Interlock OEM tetap dipakai |
| MANUAL | NEUTRAL_IN aktif-rendah | Netral wajib sebelum starter |
| MATIC | MODE_REQ/tidak dipakai | Netral tidak diwajibkan; interlock rem/standar OEM tetap dipertahankan |

### Tombol mesin tunggal

| Kondisi firmware | Label tombol | Aksi |
|---|---|---|
| Kontak OFF, mesin mati | KONTAK ON | `AUX,KEYLESS,ON` |
| Kontak aktif, RPM belum hidup | START ENGINE | `AUX,START,PULSE,1500` |
| Mesin hidup | STOP ENGINE | `AUX,ALL,OFF` |

Status akhir selalu dibaca ulang melalui GET,AUX dan GET,STATUS. ACK tidak dipakai sebagai bukti bahwa relay atau mesin sudah berubah.

Kontak mekanis ON dilaporkan sebagai sumber MECHANICAL. Sesi keyless dilaporkan sebagai KEYLESS. Saat STOP ENGINE, firmware melepas starter, mematikan izin spark/charger HV, lalu melepas relay kontak.

## Timing idle/show

| Mode | Intensitas bawaan | Rentang bawaan | Fungsi firmware |
|---|---:|---:|---|
| STANDARD | 0/10 | 1150–1700 RPM | Map utama tanpa osilasi tambahan |
| SOFT | 2/10 | 1250–1550 RPM | Retard ringan, maksimum 2° |
| RESPONSIVE | 3/10 | 1200–1700 RPM | Advance ringan, maksimum 2° |
| KUDA | 4/10 | 1200–1600 RPM | Ayunan timing bipolar 8 event |
| DRUMBAND | 5/10 | 1200–1650 RPM | Ayunan timing bipolar 12 event |
| FOMO | 6/10 | 1150–1700 RPM | Ayunan timing bipolar 10 event yang lebih tegas |
| CUSTOM | 4/10 | 1200–1650 RPM | Ayunan bipolar 8 event dengan parameter pengguna |

Metode mengikuti prinsip lumpy-idle ECU: timing diayunkan maju–mundur pada event pengapian, bukan memutus spark. Semua preset bawaan hanya aktif pada TPS ≤5%, dibatasi ±8° dari map aktif, membatalkan retard dekat batas RPM bawah, membatalkan advance dekat batas atas, dan kembali ke map normal di luar jendela. Idle standar NS200 tetap 1350–1450 RPM; nama KUDA/DRUMBAND/FOMO adalah nama profil IgniTra, bukan standar timing universal. Angka preset adalah baseline konservatif yang wajib divalidasi pada prototipe kendaraan sebelum rilis produksi.

Jika modul SIDE tidak terdeteksi, aplikasi menampilkan HV SIDE sebagai N/A dan mengeluarkannya dari Command Guard, Quick Setup, penyimpanan map, serta preflight OTA. Nilai ADC yang tidak memiliki modul tidak boleh dianggap sebagai tegangan nyata.

Referensi baseline: [MaxxECU — Lumpy idle](https://www.maxxecu.com/webhelp/solutions_and_faq-lumpy_idle.html) untuk metode osilasi timing per event tanpa ignition/fuel cut, dan [Pulsar 200NS Service Manual](https://roadsafetymoris.org.in/ns200/bajaj_pulsar_200_nsServiceManual.pdf) untuk idle standar 1350–1450 RPM. Referensi ini tidak mendefinisikan nama KUDA/DRUMBAND/FOMO; ketiganya adalah profil IgniTra yang harus disahkan melalui uji prototipe.

## BLE dan pemulihan setelah background

- Advertising utama: `NS200-CDI`.
- Filter pertama memakai Service UUID; nama dipakai sebagai fallback.
- Command queue hanya memiliki satu command in-flight.
- CRC16 dan sequence diverifikasi sebelum respons diterima.
- Sequence telemetry adalah unsigned 16-bit dan berputar kembali ke 0 setelah 65.535; UI hanya mengganti satu nilai tetap, tidak menambah baris.
- Penghitung frame sesi ditampilkan ringkas (`K/M/B`), sampel rate/CRC hanya menyimpan jendela dua detik, terminal dibatasi 80 entri dan panel hanya merender 20 entri terakhir.
- Saat aplikasi kembali dari background, koneksi sehat harus membalas PING atau mengirim telemetry dalam delapan detik.
- Objek GATT `ready` yang tidak lagi menghasilkan paket dianggap basi, ditutup, diberi jeda pelepasan 600 ms, lalu dibuka ulang otomatis.
- Reconnect otomatis mempertahankan snapshot binding, identitas, modul, dan commissioning; write tetap diblokir sampai GATT kembali siap.
- Status recovery mencantumkan penyebab GATT dan command aktif terakhir, sehingga COMMAND GUARD tidak lagi berubah menjadi pesan binding yang tidak relevan.
- Saat status masih MENGHUBUNGKAN, menekan tombol koneksi memaksa reset GATT dan reconnect; force-close aplikasi atau mematikan MCU tidak diperlukan.
- Jika izin Nearby Devices dicabut saat background, recovery berhenti aman dan meminta izin kembali.

## Notifikasi Android

Status bar hanya digunakan untuk:

- perubahan mesin hidup/mati;
- fault atau interlock keselamatan penting.

Sinkronisasi biasa, perubahan modul, dan pesan setup tetap berada di dalam aplikasi. Android 13+ membutuhkan POST_NOTIFICATIONS. Jika izin dicabut ketika aplikasi berjalan, pemanggilan notifikasi dihentikan tanpa crash.

## Suara mesin dan receiver audio

Suara dibuat oleh aplikasi berdasarkan telemetry RPM:

```text
ESP32 CDI --BLE telemetry--> Android
Android --Classic Bluetooth A2DP--> receiver --> amplifier/speaker
```

Receiver BLE-only tidak dapat menerima audio media Android. Status receiver berasal dari API Android, bukan GET,MODULES. Jangan mengambil arus amplifier dari pin 3V3 ESP32 dan pisahkan antena/catu receiver dari trafo serta jalur coil.

## Pemetaan J1 yang ditampilkan aplikasi

J1 adalah 2×6 dengan kolom kiri 1–6 dan kanan 7–12.

| J1 | Fungsi Rev C |
|---:|---|
| 1 | KEYLESS_REQ tambahan |
| 2 | TPS_A |
| 3 | TEMP_SENSOR |
| 4 | TPS_B |
| 5 | IGN_12V/kontak mekanis |
| 6 | COIL_SIDE |
| 7 | FAN_RELAY |
| 8 | START_REQ tambahan |
| 9 | MODE_REQ atau NEUTRAL_IN |
| 10 | PICKUP_RAW |
| 11 | GND_STAR |
| 12 | COIL_CENTER |

Pada harness NS200 asli J1.1, J1.8 dan J1.9 tetap kosong sampai terminal tambahan dipasang. Detail rangkaian, header modul, BOM dan zona PCB mengikuti README firmware, bukan file hardware lama aplikasi.

Rev C Production Freeze 5 menempatkan JMOD pada rail mekanik enam zona global 1–60 dan mengulang semua zona 1:1 pada carrier modul. Nomor global hanya untuk fabrikasi; aplikasi tetap memakai lima bit presentMask dan nama net/pin lokal yang sama. Z1 SIDE memakai footprint split HV/power/control, sehingga perubahan rail tidak mengubah protokol, telemetry, deteksi modul, atau tampilan aplikasi.

## Struktur source aktif

```text
app/src/main/java/id/ns200/cdir7/
  BleCdiClient.kt          GATT, scan, reconnect, queue dan OTA
  CdiProtocol.kt           parser command/telemetry
  CdiViewModel.kt          state sesi, setup, tuning dan modul
  CdiNotificationHelper.kt notifikasi mesin/fault
  EngineSound.kt           audio virtual
  ui/screens/              dashboard, maps, setup, suara, buku, BLE
```

Package `com.example` masih berisi layar wiring/UI legacy yang dipakai oleh entry/navigation lama. Jangan menghapusnya tanpa audit referensi dan CI.

## Build aplikasi

Prasyarat:

- JDK 17;
- Android SDK sesuai versi pada `gradle/libs.versions.toml`;
- `google-services.json` lokal bila layanan Firebase dipakai;
- debug signing memakai keystore lokal bila tersedia, atau keystore debug standar Android Gradle Plugin.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

CI menjalankan unit test, lint, dan debug APK. Jangan merge bila job `Android R9 CI / verify` merah.

## Troubleshooting singkat

| Gejala | Pemeriksaan |
|---|---|
| Menghubungkan terus setelah minimize | Tunggu health-check maksimum 4 detik; bila perlu ketuk MENGHUBUNGKAN untuk memaksa sesi GATT baru; cek izin Nearby Devices |
| MODULES tidak sesuai | Pastikan resistor DET 1 kΩ modul ke GND_LOGIC dan baca present/configured mask |
| Tombol START tidak muncul | Kontak belum aktif, AUX tidak present, atau status GET,AUX belum sinkron |
| Starter ditolak motor manual | NEUTRAL_IN J1.9 belum aktif |
| RPM nol | Periksa PICKUP_RAW, comparator dan kualitas pickup |
| HV Side nol | Pastikan SIDE present, Dual dipilih dan offset valid |
| Suhu invalid | Kalibrasi tiga titik dan periksa THERMAL |
| Suara tidak keluar receiver | Pastikan receiver A2DP connected dan menjadi media route aktif |

## Aturan pemeliharaan

1. README ini adalah dokumentasi tunggal aplikasi.
2. Perubahan protokol harus diperbarui bersama parser, test, firmware dan README kedua repository.
3. Hardware/PCB tidak disimpan lagi di repository aplikasi.
4. File hasil build, APK lokal, keystore dan konfigurasi rahasia tidak boleh di-commit.
5. UI lama dipertahankan kecuali perubahan sudah diuji dan tidak menghilangkan fungsi ESP32.
