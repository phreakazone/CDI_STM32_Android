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
setelah footprint fisik dikunci:

1. `0–100 mm`: logic, ESP32, sensor, OEM Learn, dan fan.
2. `100–145 mm`: power 12 V, TC4427, MOSFET, dan shunt.
3. `145–230 mm`: trafo, penyearah, serta dua bank 345 V.

Antara power dan HV disediakan panduan slot isolasi. Antena ESP32 harus memiliki
keep-out minimal 15 mm tanpa tembaga, transformer, heatsink, atau kabel HV.

## Belum boleh dikirim ke pabrik

Template placement sengaja belum memiliki routing tembaga final. Tiga ukuran fisik
berikut harus diukur dari komponen yang benar-benar akan dipakai:

| Komponen | Ukuran yang dibutuhkan |
|---|---|
| T1 trafo ATX donor | panjang x lebar badan, jumlah kaki, jarak tiap kaki, serta identifikasi LV_A/CT/LV_B/HV_AC1/HV_AC2 |
| C_CENTER dan C_SIDE | panjang x lebar x tinggi serta jarak dua kaki |
| Soket/pigtail J1 | foto muka dan belakang, pitch pin, jarak antarbaris, serta orientasi pengunci |

Tanpa ukuran ini, memfinalkan lubang bor atau routing akan menghasilkan PCB yang
tidak cocok dengan komponen fisik.

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

Setelah footprint fisik dikonfirmasi, tahap berikutnya adalah mengganti footprint
universal T1/C/J1, menyelesaikan skematik komponen penuh, lalu merutekan 2-layer dan
single-layer secara terpisah dan menjalankan DRC.
