# IgniTra CDI R9 — Aplikasi Android

Aplikasi Android resmi untuk konfigurasi, commissioning, telemetry, diagnosis, tuning, BLE OTA, dan buku petunjuk IgniTra CDI ESP32 R9 Modular.

Dokumen ini adalah satu-satunya dokumentasi repository aplikasi. Spesifikasi firmware dan hardware kanonik berada di repository [Firmware_CDI_NS200_ESP32](https://github.com/phreakazone/Firmware_CDI_NS200_ESP32). Repository aplikasi tidak lagi menyimpan generator PCB atau skematik EasyEDA lama.

## Status rilis saat ini

| Item | Nilai |
|---|---|
| Target firmware | IgniTra R9 ESP32 |
| Firmware acuan | 9.5.0, build 20260925 |
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

## Kontak fisik, keyless dan starter

### Profil kendaraan

| Profil | J1.9 | Aturan starter |
|---|---|---|
| NS200 | MODE_REQ/interlock OEM | Interlock OEM tetap dipakai |
| UNIVERSAL_MANUAL | NEUTRAL_IN aktif-rendah | Netral wajib sebelum starter |
| UNIVERSAL_MATIC | MODE_REQ/tidak dipakai | Netral tidak diwajibkan; interlock rem/standar OEM tetap dianjurkan |

### Tombol mesin tunggal

| Kondisi firmware | Label tombol | Aksi |
|---|---|---|
| Kontak OFF, mesin mati | KONTAK ON | `AUX,KEYLESS,ON` |
| Kontak aktif, RPM belum hidup | START ENGINE | `AUX,START,PULSE,1500` |
| Mesin hidup | STOP ENGINE | `AUX,ALL,OFF` |

Status akhir selalu dibaca ulang melalui GET,AUX dan GET,STATUS. ACK tidak dipakai sebagai bukti bahwa relay atau mesin sudah berubah.

Kontak mekanis ON dilaporkan sebagai sumber MECHANICAL. Sesi keyless dilaporkan sebagai KEYLESS. Saat STOP ENGINE, firmware melepas starter, mematikan izin spark/charger HV, lalu melepas relay kontak.

## Timing idle/show

| Mode | Fungsi |
|---|---|
| STANDARD | Map utama tanpa pola tambahan |
| SOFT | Retard ringan dalam jendela yang dipilih |
| RESPONSIVE | Advance ringan, tetap dibatasi PROFILE |
| KUDA | Pola idle deterministik |
| DRUMBAND | Pola show alternatif |
| FOMO | Pola show alternatif |
| CUSTOM | Parameter kustom firmware |

Intensitas 0–10 dan rentang aktif 500–5000 RPM. Efek hanya bekerja pada TPS rendah dan tetap dijepit oleh map/PROFILE. Mode ini bukan pengganti map daya yang benar.

## BLE dan pemulihan setelah background

- Advertising utama: `NS200-CDI`.
- Filter pertama memakai Service UUID; nama dipakai sebagai fallback.
- Command queue hanya memiliki satu command in-flight.
- CRC16 dan sequence diverifikasi sebelum respons diterima.
- Saat aplikasi kembali dari background, koneksi sehat menerima PING.
- Attempt GATT yang menggantung diputus dan dibuka ulang oleh watchdog delapan detik.
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
| Menghubungkan terus setelah minimize | Tunggu watchdog 8 detik; cek Nearby Devices; Putus lalu Konek |
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

