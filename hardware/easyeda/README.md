# IGNITRA CDI R9 — PCB EasyEDA ESP32 38-pin

Status: **Rev B, sumber PCB belum dirutekan**. Format file telah diselaraskan dengan
ekspor EasyEDA Standard 6.5.51 yang valid: PCB memakai `head.docType = "3"` dan
block diagram memakai wrapper project `docType = "5"` dengan sheet `docType = "1"`.
Dua file `*_EASYEDA.json` adalah sumber PCB EasyEDA Standard yang dapat dibuka
langsung. Keduanya sudah memuat
outline, footprint, pad, net, zona fungsi, slot isolasi, dan aturan DRC dasar.
Keduanya **belum** boleh langsung dijadikan Gerber karena routing tembaga,
penyesuaian footprint terhadap komponen fisik, dan DRC tegangan tinggi masih harus
diselesaikan di EasyEDA.

> Peringatan: rangkaian CDI menghasilkan sekitar 345 V dan pulsa pengapian. Salah
> clearance, creepage, footprint, polaritas, atau grounding dapat merusak ESP32,
> komponen kendaraan, dan membahayakan operator. Uji pertama wajib memakai catu
> terbatas arus, beban dummy, pelindung, dan probe yang sesuai tegangan.

## Impor langsung ke EasyEDA Standard

1. Unduh salah satu file berikut; jangan menyalin isi JSON lewat clipboard.
2. Buka EasyEDA Standard, lalu pilih `File > Open > EasyEDA Source`.
3. Pilih:
   - `generated/IGNITRA_CDI_ESP32_2L_EASYEDA.json` untuk PCB pabrik dua layer;
   - `generated/IGNITRA_CDI_ESP32_1L_EASYEDA.json` untuk PCB rumahan satu layer.
4. Setelah PCB terbuka, simpan sebagai project baru agar sumber asli tetap utuh.
5. Jalankan `Design > Check DRC`, selesaikan seluruh ratline, lalu periksa lagi
   clearance/creepage HV secara manual sebelum ekspor Gerber.

File bernama `*_placement.json` adalah alias kompatibilitas dengan isi yang sama.
`IGNITRA_CDI_ESP32_BLOCK_DIAGRAM.json` dan alias lama
`IGNITRA_CDI_ESP32_architecture.json` memakai wrapper project EasyEDA yang valid,
tetapi hanya berisi blok arsitektur—**bukan** skematik listrik untuk fabrikasi.

## Sumber kebenaran pin ESP32

Pemetaan ini harus sama dengan `main/cdi_board_esp32.h` pada firmware:

| Fungsi | Pin ESP32 | Arah / level |
|---|---:|---|
| TPS signal | GPIO36 | ADC1 input, hanya input |
| Suhu | GPIO39 | ADC1 input, hanya input |
| TPS reference | GPIO34 | ADC1 input, hanya input |
| HV Center feedback | GPIO35 | ADC1 input, hanya input |
| HV Side feedback | GPIO32 | ADC1 input |
| Tegangan aki | GPIO33 | ADC1 input |
| Gate Center | GPIO25 | output ke driver SCR, bukan ke J1 langsung |
| Gate Side | GPIO26 | output ke driver SCR, bukan ke J1 langsung |
| LED strobo | GPIO27 | output ke gate MOSFET |
| Hardware fault | GPIO14 | input aktif-rendah |
| Fan control | GPIO13 | output logika aktif-tinggi ke BC337 |
| Charger PWM A / B | GPIO18 / GPIO19 | output ke TC4427 |
| OEM Learn Side / Center | GPIO17 / GPIO16 | input terisolasi PC817 |
| Pulser | GPIO4 | input keluaran komparator LM339 |

Semua ADC tersebut memakai ADC1 agar tetap dapat dibaca ketika Wi-Fi/Bluetooth
ESP32 aktif.

## Konektor J1: nomor logis 2 x 6, footprint aman solder manual

J1 **bukan header plug-in 2 x 6 pitch 2,54 mm standar**. Nomor pin tetap tersusun
sebagai dua baris enam nomor supaya dokumentasi harness mudah dibaca, tetapi pin 6
dan 12 membawa pulsa CDI tegangan tinggi. Keduanya dipindahkan ke kolom terisolasi:

- pin 1–5 dan 7–11: pitch kolom 2,54 mm;
- jarak antardua baris: 10,16 mm;
- pin HV 6 dan 12: pusat pada kolom 25,40 mm;
- jarak pin 5 ke 6 dan pin 11 ke 12: 15,24 mm;
- slot isolasi: x=135–140 mm, y=145–164 mm pada layout 230 x 165 mm;
- pemasangan: kabel disolder satu per satu dan diberi strain relief/isolasi.

| J1 | Nama | Hubungan PCB | Keterangan |
|---:|---|---|---|
| 1 | NC | tanpa net | cadangan, jangan disambung |
| 2 | TPS_A | selector JTPS | salah satu TPS signal/reference |
| 3 | TEMP_SENSOR | divider + clamp ke GPIO39 | input sensor suhu |
| 4 | TPS_B | selector JTPS | pasangan TPS_A; posisi selector menentukan fungsi |
| 5 | IGN_12V | fuse input | +12 V setelah kunci kontak |
| 6 | COIL_SIDE | C_SIDE/SCR2 + tap OEM Learn | pulsa CDI HV ke koil Side; bukan GPIO26 langsung |
| 7 | FAN_RELAY | kolektor BC337 + flyback | low-side sink; relay ON saat GPIO13 tinggi |
| 8 | OEM_SIDE_PROBE | NC/probe opsional | bukan GPIO17 langsung |
| 9 | OEM_CENTER_PROBE | NC/probe opsional | bukan GPIO16 langsung |
| 10 | PICKUP_RAW | 39 kΩ + clamp + LM339 ke GPIO4 | input pulser mentah |
| 11 | GND_STAR | titik ground harness | titik pertemuan ground terkontrol |
| 12 | COIL_CENTER | C_CENTER/SCR1 + tap OEM Learn | pulsa CDI HV ke koil Center; bukan GPIO25 langsung |

OEM Learn tidak masuk melalui J1.8/J1.9. Sinyal dibaca paralel dari jalur
COIL_SIDE/COIL_CENTER melalui masing-masing empat resistor 12 kΩ 0,5 W seri dan
PC817, lalu menuju GPIO17/GPIO16.

## Footprint mekanik terkunci

Semua posisi memakai grid 2,54 mm. Cocokkan terhadap komponen nyata sebelum order.

| Komponen | Definisi Rev B |
|---|---|
| T1 EE35 universal | baris depan 11 posisi: LV_A di 1, CT/VIN_HV di 6, LV_B di 11; baris belakang 13 posisi: pad kosong di 1 dan 13, HV_AC1 di 2, HV_AC2 di 12; jarak pusat kedua output tepat 10 pitch = 25,40 mm; jarak antarbaris 25,40 mm |
| C_CENTER / C_SIDE | area 9 x 4 lubang; jarak kaki 8 pitch = 20,32 mm; pad 4 mm, drill 0,8 mm |
| J1 | nomor logis 2 x 6 dengan dua pin HV terisolasi seperti bagian J1 di atas; pad 3,2 mm, drill 1,1 mm |

Pad T1 tanpa net disediakan untuk variasi bobbin EE35. Pad kosong tidak boleh
dianggap sebagai jumper dan tidak boleh dihubungkan ke lilitan lain.

## Zona PCB dan grounding

Outline awal kedua varian adalah **230 x 165 mm**:

| Rentang X | Zona | Isi utama |
|---|---|---|
| 0–100 mm | LOGIC | ESP32, ADC clamp, LM339, sensor, OEM Learn, fan |
| 100–145 mm | POWER | input 12 V, TC4427, MOSFET, shunt |
| 145–230 mm | HV | EE35, penyearah, kapasitor 630 V, SCR |

`GND_LOGIC` dan `GND_POWER` tidak boleh disatukan sembarang. Keduanya menuju
`GND_STAR` hanya melalui NT2 dan NT1. Titik star berada dekat J1.11. Antena ESP32
memerlukan keep-out minimum 15 mm tanpa tembaga, trafo, heatsink, atau kabel HV.

## Aturan routing wajib

- Net `HV_*`, `BRIDGE_PLUS`, `COIL_CENTER`, dan `COIL_SIDE`: clearance minimum
  6 mm dari logic/ground; target creepage 8 mm atau lebih dan gunakan slot.
- Jalur primer trafo, VIN_HV, MOSFET, shunt, SCR, dan coil dibuat selebar mungkin.
  Untuk varian satu layer, perkuat jalur arus tinggi dengan kawat tembaga.
- Pulser dan semua ADC tidak boleh berjalan paralel dengan drain MOSFET, AC trafo,
  `BRIDGE_PLUS`, atau jalur discharge.
- Bypass TC4427 ditempatkan langsung pada pin supply-ground; resistor gate 10 Ω dan
  pulldown 10 kΩ ditempatkan sedekat mungkin dengan MOSFET.
- Empat resistor 270 kΩ pada setiap feedback HV dan empat resistor 470 kΩ pada
  setiap bleeder harus tetap seri secara fisik untuk membagi tegangan kerja.
- Varian satu layer hanya memakai BottomLayer. Semua persilangan harus berupa
  jumper kawat yang diberi reference dan tercantum di dokumentasi hasil akhir.

## Pemeriksaan sebelum Gerber

- seluruh ratline = 0 dan seluruh net sesuai `NETLIST.csv`;
- DRC = 0 error, termasuk clearance HV yang diperiksa manual;
- ukuran drill/pad T1, MKP, J1, fuse, SCR, MOSFET, dan terminal cocok komponen nyata;
- polaritas diode, TVS, elektrolit, PC817, transistor, SCR, dan konektor benar;
- slot benar-benar diekspor sebagai NPTH/milling pada preview Gerber;
- silkscreen menandai sisi HV, pin 1, polaritas, dan larangan menyentuh;
- Gerber dan drill dibuka ulang di viewer independen sebelum dikirim ke pabrik.

## Berkas pendamping

- `generated/NETLIST.csv`: hubungan reference-pin-net kanonik.
- `generated/BOM.csv`: BOM per reference; status `VERIFY PHYSICAL FOOTPRINT` wajib
  ditutup sebelum fabrikasi.
- `generated/PLACEMENT.csv`: koordinat dan zona footprint.
- `generated/PLACEMENT_PREVIEW.svg`: preview zona/placement, bukan Gerber.
- `generate_easyeda.mjs`: generator deterministik sumber PCB dan tabel.
- `validate_easyeda.mjs`: pemeriksaan struktur, net penting, dan dimensi footprint.

## Regenerasi dan validasi

Tidak memerlukan build aplikasi maupun toolchain firmware:

```bash
node hardware/easyeda/generate_easyeda.mjs
node hardware/easyeda/validate_easyeda.mjs
```

Validasi lokal hanya memeriksa konsistensi sumber. Pemeriksaan akhir tetap harus
dijalankan di EasyEDA setelah routing dan menggunakan ukuran komponen fisik.
