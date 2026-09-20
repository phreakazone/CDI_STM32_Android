# IGNITRA CDI R9 — EasyEDA ESP32 38-pin

Status: **Rev A, konektivitas dan penempatan awal**. Berkas pada folder `generated/`
dapat diimpor ke EasyEDA Standard melalui `File > Open > EasyEDA Source`.

Desain ini mengikuti source firmware ESP32 sebagai sumber pin utama:

| Fungsi | ESP32 DevKitC V4 38-pin |
|---|---|
| TPS / suhu / TPS reference | GPIO36 / GPIO39 / GPIO34 |
| HV Center / HV Side / aki | GPIO35 / GPIO32 / GPIO33 |
| Gate Center / Side | GPIO25 / GPIO26 |
| Pulser | GPIO4 |
| OEM Learn Center / Side | GPIO16 / GPIO17 |
| Charger A / B | GPIO18 / GPIO19 |
| Fault active-low | GPIO14 |
| Fan / strobo | GPIO13 / GPIO27 |

## Berkas

- `IGNITRA_CDI_ESP32_architecture.json`: lembar arsitektur EasyEDA.
- `IGNITRA_CDI_ESP32_2L_placement.json`: outline dan placement PCB pabrik 2-layer.
- `IGNITRA_CDI_ESP32_1L_placement.json`: outline dan placement PCB rumahan single-layer.
- `NETLIST.csv`: konektivitas pin komponen yang menjadi sumber pemeriksaan skematik.
- `BOM.csv`: BOM awal berdasarkan netlist.
- `PLACEMENT.csv`: koordinat dan zona setiap footprint.
- `generate_easyeda.mjs`: generator deterministik seluruh berkas di atas.

Kedua PCB menggunakan outline prototipe awal **230 mm x 135 mm** dengan tiga zona.
Ukuran ini sengaja longgar untuk layout single-layer; versi 2-layer dapat dipadatkan
setelah routing dan DRC selesai:

1. `0–100 mm`: logic, ESP32, sensor, OEM Learn, dan fan.
2. `100–145 mm`: power 12 V, TC4427, MOSFET, dan shunt.
3. `145–230 mm`: trafo, penyearah, serta dua bank 345 V.

Antara power dan HV disediakan panduan slot isolasi. Antena ESP32 harus memiliki
keep-out minimal 15 mm tanpa tembaga, transformer, heatsink, atau kabel HV.

## Footprint mekanik yang sudah dikunci

Semua ukuran lubang berikut memakai grid standar **2,54 mm**:

| Komponen | Definisi footprint Rev A |
|---|---|
| T1 EE35 universal | dua baris masing-masing 11 posisi; baris depan posisi 1/6/11 = LV_A/CT/LV_B, baris belakang posisi 1/11 = HV_AC1/HV_AC2; jarak antarbaris 10 pitch atau 25,40 mm |
| C_CENTER dan C_SIDE | area 9 x 4 lubang; dua kaki berjarak 8 pitch atau 20,32 mm dan berada pada garis tengah lebar footprint |
| J1 | pin header THT 2 x 6, pitch pin dan pitch antarbaris 2,54 mm; disolder manual |

Lubang T1 lain tetap berupa pad tanpa net agar footprint dapat menerima variasi kaki
trafo EE35. Pad primer dan sekunder yang dipakai sudah memiliki nomor/net tetap dan
tidak boleh dijumper sembarang.

## Belum boleh dikirim ke pabrik

Footprint T1/C/J1 sudah mengikuti ukuran grid pengguna, tetapi template masih berupa
placement/net dan belum memiliki routing tembaga final. Gerber baru boleh dibuat
setelah routing kedua varian selesai serta lolos pemeriksaan DRC, clearance HV, dan
creepage.

## Aturan routing final

- Clearance semua net `HV_*`, `BRIDGE_PLUS`, `COIL_CENTER`, dan `COIL_SIDE`
  terhadap logic/GND minimal 6 mm; target creepage 8 mm dan gunakan slot bila perlu.
- Jalur primer trafo, VIN_HV, MOSFET, shunt, SCR, dan jalur coil dibuat selebar
  mungkin; untuk single-layer diperkuat kawat tembaga.
- GND logic dan GND power bertemu hanya di `NT1/GND_STAR`, dekat J1.11.
- Jalur pulser dan seluruh ADC tidak boleh sejajar dengan drain MOSFET, AC trafo,
  BRIDGE_PLUS, atau jalur discharge.
- Kapasitor bypass TC4427 ditempatkan langsung di pin 6–3; resistor gate 10 ohm dan
  pulldown 10k ditempatkan tepat di kaki MOSFET.
- Empat resistor 270k setiap feedback HV dipasang seri untuk membagi tegangan kerja.
- Bleeder 4 x 470k dipasang seri pada masing-masing kapasitor bank.
- PCB single-layer hanya memakai BottomLayer; persilangan akan dibuat sebagai jumper
  kawat berlabel `JPWxx`, bukan jejak tembaga silang.

## Koreksi terhadap dokumentasi aplikasi lama

- HV Center adalah GPIO35, HV Side GPIO32, dan VBAT GPIO33 sesuai
  `main/cdi_board_esp32.h` firmware.
- LM339 memakai satu kanal pulser, satu kanal over-current, dan dua kanal OVP.
- Driver fan memakai BC337-40, bukan BC547, agar arus kumparan relay tidak bekerja
  di batas maksimum transistor.
- SCR ditetapkan BT151-800R untuk margin lebih besar pada target 345 V; BT151-600R
  tetap footprint-compatible tetapi bukan pilihan utama Rev A.

## Regenerasi

```bash
node hardware/easyeda/generate_easyeda.mjs
```

Tahap berikutnya adalah menyelesaikan skematik komponen penuh, merutekan 2-layer dan
single-layer secara terpisah, lalu menjalankan DRC sebelum ekspor Gerber.
