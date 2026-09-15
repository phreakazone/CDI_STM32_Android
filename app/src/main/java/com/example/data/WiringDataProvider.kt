package com.example.data

import com.example.model.*

object WiringDataProvider {

  val harnessPins: List<HarnessPin> = listOf(
    HarnessPin(
      pinNumber = 1,
      wireColor = "Kosong / NC",
      name = "NC (Not Connected)",
      direction = "Tidak Terhubung",
      completePath = "Jangan disambung ke mana pun. Biarkan pin pigtail diisolasi heat-shrink.",
      destination = "KOSONG",
      status = "KOSONG",
      isConnected = false,
      detailGuide = "Pin cadangan pabrikan. Jangan disolder ke jalur PCB apa pun untuk mencegah crosstalk sinyal."
    ),
    HarnessPin(
      pinNumber = 2,
      wireColor = "Hijau-Putih",
      name = "TPS_A",
      direction = "Input Sensor",
      completePath = "J1.2 -> Header J_TPS Pin 1 & Pin 6 -> Diseleksi sebagai TPS_REF atau TPS_SIG",
      destination = "H_BOTTOM.12 (PA3) atau H_BOTTOM.14 (PA5)",
      status = "CONFIRM TPS",
      isConnected = true,
      detailGuide = "Salah satu dari dua kabel TPS NS200. Hubungkan ke pin 1 dan pin 6 pada header selektor 2x3 J_TPS. Kalibrasi posisi throttle di aplikasi setelah terpasang."
    ),
    HarnessPin(
      pinNumber = 3,
      wireColor = "Hitam-Putih",
      name = "TEMP (Sensor Suhu Mesin)",
      direction = "Input Sensor",
      completePath = "+5V -- 4.7k -- J1.3; J1.3 -- 15k -- TEMP_ADC; TEMP_ADC -- 27k || 10nF -- GND; TEMP_ADC -- 1k -- PA4; BAT54S clamp",
      destination = "H_BOTTOM.13 (PA4 ADC)",
      status = "AKTIF • KALIBRASI 3 TITIK",
      isConnected = true,
      detailGuide = "Rangkaian menyiapkan ADC sensor coolant. Resistor pull-up 4.7k ke 5V membentuk pembagi dengan NTC dan BAT54S melindungi input; firmware R9 mengonversi ADC memakai kalibrasi NTC 3 titik dan mengaktifkan fan fail-safe bila sensor putus/korslet."
    ),
    HarnessPin(
      pinNumber = 4,
      wireColor = "Abu-Abu",
      name = "TPS_B",
      direction = "Input Sensor",
      completePath = "J1.4 -> Header J_TPS Pin 3 & Pin 4 -> Diseleksi sebagai TPS_REF atau TPS_SIG",
      destination = "H_BOTTOM.12 (PA3) atau H_BOTTOM.14 (PA5)",
      status = "CONFIRM TPS",
      isConnected = true,
      detailGuide = "Kabel pasangan TPS NS200. Hubungkan ke pin 3 dan pin 4 pada header selektor J_TPS. Menentukan jalur wiper sinyal vs referensi 5V."
    ),
    HarnessPin(
      pinNumber = 5,
      wireColor = "Cokelat (+12V Kontak)",
      name = "+12V Kontak (Kunci Kontak ON)",
      direction = "Input Daya Utama",
      completePath = "J1.5 -> FMAIN 5A -> DREV (SB560) -> VIN_PROT -> L47uH -> VIN_FILT -> FLOGIC 1A & FHV 3A; juga J_AUDIO_CTL.1",
      destination = "Input Catu Daya Logic & HV Charger",
      status = "AKTIF",
      isConnected = true,
      detailGuide = "Jalur suplai daya utama 12V dari kunci kontak/kill switch. Dilarang menyambung langsung ke pin STM32! Wajib melewati FMAIN 5A, DREV Schottky, TVS SMBJ33A, dan choke L47uH."
    ),
    HarnessPin(
      pinNumber = 6,
      wireColor = "Hitam-Merah",
      name = "COIL_SIDE (Koil Busi Kiri-Kanan)",
      direction = "Output Pulsa HV",
      completePath = "C_SIDE Terminal B; Terminal A ke HV_SIDE; SCR2 BT151 pin2 (Anode+tab) ke HV_SIDE, pin1 (Cathode) ke GND_POWER",
      destination = "H_BOTTOM.11 (PA2) via driver QNS/QPS ke Gate SCR2",
      status = "READY-3-BUSI setelah offset SIDE terukur",
      isConnected = true,
      detailGuide = "Memicu dua busi samping (Side Plugs) NS200. Kapasitor 1uF 630V MKP dengan 4x470k bleeder. Dinonaktifkan pada tahap awal FIRST START demi keamanan mesin."
    ),
    HarnessPin(
      pinNumber = 7,
      wireColor = "Biru-Kuning",
      name = "FAN_RELAY (Kendali Kipas Radiator)",
      direction = "Output Sink",
      completePath = "J1.7 -> Kolektor QFAN (BC547); Emitter GND; Basis 4.7k dari PB5 dan 10k pull-down ke GND",
      destination = "H_TOP.7 (PB5)",
      status = "CONFIRM polaritas relay",
      isConnected = true,
      detailGuide = "Transistor NPN BC547 menarik ground relay kipas saat output MCU HIGH. Firmware saat ini memakai HIGH untuk ON dan AUTO (failsafe); AUTO memakai suhu NTC terkalibrasi dengan histeresis; fault sensor memaksa fan ON."
    ),
    HarnessPin(
      pinNumber = 8,
      wireColor = "NC / Kabel Tambahan",
      name = "OEM_SIDE (Probe Pasif Koil Samping)",
      direction = "Input Monitor Pulsa Pasif",
      completePath = "J1.8 tetap NC dari pabrik. Catatan: Pasang kabel tambahan dari Koil Side (J1.6) -> Resistor 47k 2W -> Modul Optocoupler PC817 (IN2+) -> OUT2 ke PB4",
      destination = "H_TOP.8 (PB4 / OEM_SIDE)",
      status = "OPTIONAL MODUL OEM_LEARN",
      isConnected = false,
      detailGuide = "Pin J1.8 ditandai NC dari pabrik. Untuk alur OEM Learn, pasang kabel tambahan dari Koil Side (J1.6) melalui resistor 47k 2W ke Modul PC817 4-Channel (atau pin 8 pigtail sebagai jalur jumper probe) menuju WeAct PB4."
    ),
    HarnessPin(
      pinNumber = 9,
      wireColor = "NC / Kabel Tambahan",
      name = "OEM_CTR (Probe Pasif Koil Center)",
      direction = "Input Monitor Pulsa Pasif",
      completePath = "J1.9 tetap NC dari pabrik. Catatan: Pasang kabel tambahan dari Koil Center (J1.12) -> Resistor 47k 2W -> Modul Optocoupler PC817 (IN1+) -> OUT1 ke PB3",
      destination = "H_TOP.9 (PB3 / OEM_CTR)",
      status = "OPTIONAL MODUL OEM_LEARN",
      isConnected = false,
      detailGuide = "Pin J1.9 ditandai NC dari pabrik. Untuk alur OEM Learn, pasang kabel tambahan dari Koil Center (J1.12) melalui resistor 47k 2W ke Modul PC817 4-Channel (atau pin 9 pigtail sebagai jalur jumper probe) menuju WeAct PB3."
    ),
    HarnessPin(
      pinNumber = 10,
      wireColor = "Putih-Merah",
      name = "PULSER (Pick-up Coil Trigger)",
      direction = "Input Pulsa Timing",
      completePath = "J1.10 -> 39k 0.5W -> PICKUP_SENSE; VMID divider (10k/10k); BAT54S clamp; LM339 pin 5 (IN1+); pin 4 (VZERO 15k/10k); OUT pin 2 pullup 4.7k ke 3V3, 10M histeresis -> 1k -> PA0",
      destination = "H_BOTTOM.9 (PA0 TIM2_CH1)",
      status = "CONFIRM edge/sudut/pulse",
      isConnected = true,
      detailGuide = "Sinyal pulser pick-up magnet spul. Tegangan bolak-balik AC dibatasi oleh 39k dan BAT54S, diubah menjadi pulsa kotak presisi tinggi oleh komparator LM339 untuk timer input capture STM32."
    ),
    HarnessPin(
      pinNumber = 11,
      wireColor = "Hitam-Kuning",
      name = "GND (Massa Motor)",
      direction = "Input Ground Utama",
      completePath = "GND_STAR pusat ke seluruh ground logic, LM339 pin 12, WeAct G, dan kabel ground PCB power",
      destination = "H_BOTTOM.1 (G) & H_TOP.1 (G)",
      status = "AKTIF",
      isConnected = true,
      detailGuide = "Pusat ground bintang (GND_STAR). Seluruh ground logic dan referensi wajib bertitik temu di sini agar tidak timbul ground loop saat pengapian melonjak."
    ),
    HarnessPin(
      pinNumber = 12,
      wireColor = "Oranye (Fungsi)/Koil NS200",
      name = "COIL_CENTER (Koil Busi Utama Tengah)",
      direction = "Output Pulsa HV Utama",
      completePath = "C_CENTER Terminal B; Terminal A ke HV_CENTER; SCR1 BT151 pin2 (Anode+tab) ke HV_CENTER, pin1 (Cathode) ke GND_POWER",
      destination = "H_BOTTOM.10 (PA1) via driver QNC/QPC ke Gate SCR1",
      status = "FIRST START dan READY",
      isConnected = true,
      detailGuide = "Pemicu koil primer busi tengah (Center Plug) NS200. Menggunakan kapasitor MKP pulsa 1uF 630V dan SCR BT151-600R. Aktif saat FIRST START 220V dan operasi NORMAL 285V."
    )
  )

  val weActPins: List<WeActPin> = listOf(
    // H_TOP Pins (1-15)
    WeActPin("H_TOP", 1, "GND", "Input", "GND_STAR", "Board Ground Logic", "AKTIF", isCritical = true),
    WeActPin("H_TOP", 2, "GND", "-", "Tidak dipakai", "Cadangan Ground", "CADANGAN"),
    WeActPin("H_TOP", 3, "3V3", "Output", "Hanya pull-up / clamp / interlock", "Tegangan Referensi 3.3V", "AKTIF", isCritical = true, warning = "Bukan sumber beban besar! Dilarang menyuplai relay/LED berat."),
    WeActPin("H_TOP", 4, "3V3", "-", "Tidak dipakai", "Cadangan 3.3V", "CADANGAN"),
    WeActPin("H_TOP", 5, "PB7", "Bidir", "UART service RX opsional tanpa ESP", "USART1_RX", "CADANGAN"),
    WeActPin("H_TOP", 6, "PB6", "Output", "UART service TX opsional tanpa ESP", "USART1_TX", "CADANGAN"),
    WeActPin("H_TOP", 7, "PB5", "Output", "PB5 -> Pin IN Modul Relay 1-CH 5V / Resistor 4.7k BC547 -> J1.7", "Fan Relay Control", "AKTIF (Modul Relay / Transistor)"),
    WeActPin("H_TOP", 8, "PB4", "Input", "J1.6 (Side) -> R 47k 2W -> Modul PC817 IN2+ -> OUT2 ke PB4", "OEM Side Pulse Monitor (R8 Learn)", "AKTIF OEM_LEARN", isCritical = true, warning = "Input pulsa koil samping pasif CDI OEM via isolator optocoupler."),
    WeActPin("H_TOP", 9, "PB3", "Input", "J1.12 (Center) -> R 47k 2W -> Modul PC817 IN1+ -> OUT1 ke PB3", "OEM Center Pulse Monitor (R8 Learn)", "AKTIF OEM_LEARN", isCritical = true, warning = "Input pulsa koil utama pasif CDI OEM via isolator optocoupler."),
    WeActPin("H_TOP", 10, "PA15", "-", "Tidak dipakai", "Cadangan", "CADANGAN"),
    WeActPin("H_TOP", 11, "PA10", "Input", "PWM_CLAMP pull-up 4.7k ke 3V3; LOW = fault", "Hardware Fault Shutdown", "AKTIF", isCritical = true, warning = "Mematikan sinyal PWM charger secara instan jika arus trafo atau tegangan HV over."),
    WeActPin("H_TOP", 12, "PE4", "Output", "LED onboard aktif-low", "Indikator Status Board", "AKTIF"),
    WeActPin("H_TOP", 13, "PB1", "-", "Tidak dipakai firmware R7/R8", "Cadangan", "CADANGAN"),
    WeActPin("H_TOP", 14, "PB0", "Input", "VIN_FILT -> 100k -> VBAT_ADC -> 1k -> PB0; 22k || 10nF ke GND dan BAT54S clamp", "ADC1_IN15 Monitor Tegangan Aki", "AKTIF", isCritical = true),
    WeActPin("H_TOP", 15, "GND", "-", "Tidak dipakai", "Cadangan Ground", "CADANGAN"),

    // H_BOTTOM Pins (1-20)
    WeActPin("H_BOTTOM", 1, "GND", "Input", "GND_STAR", "Board Ground Logic", "AKTIF", isCritical = true),
    WeActPin("H_BOTTOM", 2, "5V", "Input", "Modul Buck 5.00V OUT+ (MP1584EN / LM2596)", "Catu Daya Utama 5V STM32", "AKTIF", isCritical = true, warning = "Pastikan output buck disetel tepat 5.00V sebelum dicolokkan ke pin ini!"),
    WeActPin("H_BOTTOM", 3, "5V", "-", "Tidak dipakai", "Cadangan 5V", "CADANGAN"),
    WeActPin("H_BOTTOM", 4, "VBAT", "-", "Jangan hubungkan ke aki motor!", "Cadangan RTC", "DILARANG 12V", isCritical = true, warning = "PERINGATAN BAHAYA: Jangan hubungkan aki 12V ke VBAT! MCU akan langsung terbakar."),
    WeActPin("H_BOTTOM", 5, "PH3", "-", "Tidak dipakai", "Cadangan", "CADANGAN"),
    WeActPin("H_BOTTOM", 6, "PB9", "Output", "PB9 -> 100R -> Gate FQP30N06L; 10k GND; Drain LED negatif", "LED Strobe Timing TDC", "MODE MANUAL"),
    WeActPin("H_BOTTOM", 7, "PB8", "Output", "PB8 -> 1k -> TC4427 pin 4 (INB); pin 5 (OUTB) -> 10R -> Gate QHV2", "TIM1_CH2N PWM Charger B", "AKTIF", isCritical = true),
    WeActPin("H_BOTTOM", 8, "NRST", "Input", "Tombol NRST onboard; ST-Link NRST fallback", "Reset Hardware MCU", "PROGRAM/DEBUG"),
    WeActPin("H_BOTTOM", 9, "PA0", "Input", "Modul LM393 OUT / LM339 pin 2 -> 1k -> PA0", "TIM2_CH1 Pulser Pickup Capture", "AKTIF", isCritical = true),
    WeActPin("H_BOTTOM", 10, "PA1", "Output", "PA1 -> 4.7k -> QNC base; QNC/QPC -> 330R -> BT151 SCR1 pin 3", "Gate Pemicu Koil CENTER", "FIRST START 220V & READY", isCritical = true),
    WeActPin("H_BOTTOM", 11, "PA2", "Output", "PA2 -> 4.7k -> QNS base; QNS/QPS -> 330R -> BT151 SCR2 pin 3", "Gate Pemicu Koil SIDE", "HANYA READY-3-BUSI", isCritical = true),
    WeActPin("H_BOTTOM", 12, "PA3", "Input", "TPS_SIG -> 15k -> TPS_ADC -> 1k -> PA3; 27k || 4.7nF ke GND dan clamp", "ADC Sensor Bukaan Gas (TPS)", "AKTIF setelah selector", isCritical = true),
    WeActPin("H_BOTTOM", 13, "PA4", "Input", "J1.3 network -> 1k -> PA4", "ADC Sensor Suhu Mesin (TEMP)", "AKTIF"),
    WeActPin("H_BOTTOM", 14, "PA5", "Input", "TPS_REF -> 15k -> TPS_REF_MON -> 1k -> PA5; 27k || 4.7nF dan clamp", "ADC Monitor Referensi 5V TPS", "DIAGNOSTIK"),
    WeActPin("H_BOTTOM", 15, "PA6", "Input", "HV_CENTER -> 4x270k seri -> HV_C_FB -> 1k -> PA6; 8.2k || 10nF dan clamp", "ADC Tegangan Bank CENTER", "AKTIF", isCritical = true, warning = "Tegangan tinggi hingga 345V. Wajib 4 resistor 270k seri dan clamp BAT54S."),
    WeActPin("H_BOTTOM", 16, "PA7", "Input", "HV_SIDE -> 4x270k seri -> HV_S_FB -> 1k -> PA7; 8.2k || 10nF dan clamp", "ADC Tegangan Bank SIDE", "AKTIF", isCritical = true),
    WeActPin("H_BOTTOM", 17, "PA8", "-", "Tidak dipakai firmware universal", "Cadangan", "CADANGAN"),
    WeActPin("H_BOTTOM", 18, "PA9", "Output", "PA9 -> 1k -> TC4427 pin 2 (INA); pin 7 (OUTA) -> 10R -> Gate QHV1", "TIM1_CH2 PWM Charger A", "AKTIF", isCritical = true),
    WeActPin("H_BOTTOM", 19, "PB2", "Input", "Dikonfigurasi pulldown oleh board init; tidak dipakai keputusan R8", "Cadangan / test firmware mendatang", "JANGAN JADIKAN INTERLOCK"),
    WeActPin("H_BOTTOM", 20, "GND", "Input", "GND_STAR", "Board Ground Logic", "AKTIF", isCritical = true)
  )

  val esp32Pins: List<Esp32Pin> = listOf(
    // LEFT HEADER (Sisi Kiri DevKitC V4 - 19 Pin)
    Esp32Pin("LEFT", 1, "3V3", "3V3", "POWER", "Output", "Regulator Onboard 3.3V", "Suplai Referensi ADC & Pull-up", "AKTIF 3.3V", isCritical = true, warning = "Bukan sumber daya beban besar! Maksimal 150mA."),
    Esp32Pin("LEFT", 2, "EN", "NRST", "POWER", "Input", "Tombol EN Onboard", "Reset Hardware ESP32", "RESET"),
    Esp32Pin("LEFT", 3, "GPIO36 (VP)", "PA3", "ADC1_SENSOR", "Input", "TPS_SIG -> R divider -> GPIO36", "ADC1_CH0 Sensor TPS", "AKTIF ADC1", isCritical = true, warning = "Hanya ADC1 yang aman digunakan saat BLE/WiFi aktif. Jangan masukkan tegangan >3.3V."),
    Esp32Pin("LEFT", 4, "GPIO39 (VN)", "PA4", "ADC1_SENSOR", "Input", "TEMP_SIG -> R network -> GPIO39", "ADC1_CH3 Sensor Suhu Radiator", "AKTIF ADC1"),
    Esp32Pin("LEFT", 5, "GPIO34", "PA5", "ADC1_SENSOR", "Input", "TPS_REF -> R divider -> GPIO34", "ADC1_CH6 Monitor Referensi 5V TPS", "DIAGNOSTIK"),
    Esp32Pin("LEFT", 6, "GPIO35", "PA6", "ADC1_SENSOR", "Input", "HV_CENTER -> 4x270k seri -> 1k -> GPIO35", "ADC1_CH7 Feedback Tegangan Bank Center", "AKTIF ADC1", isCritical = true, warning = "Wajib 4 resistor 270k seri + BAT54S clamp ke 3V3! Tegangan koil >300V akan mematikan chip jika bocor."),
    Esp32Pin("LEFT", 7, "GPIO32", "PA7", "ADC1_SENSOR", "Input", "HV_SIDE -> 4x270k seri -> 1k -> GPIO32", "ADC1_CH4 Feedback Tegangan Bank Side", "AKTIF ADC1", isCritical = true),
    Esp32Pin("LEFT", 8, "GPIO33", "PB0", "ADC1_SENSOR", "Input", "VIN_FILT -> 100k -> 1k -> GPIO33; 22k GND", "ADC1_CH5 Monitor Tegangan Aki (VBAT)", "AKTIF ADC1", isCritical = true),
    Esp32Pin("LEFT", 9, "GPIO25", "PA1", "GATE", "Output", "GPIO25 -> 4.7k -> Driver SCR1 -> BT151 Gate", "Gate Center / Koil Tengah (J1.12 via SCR)", "AKTIF GATE CTR", isCritical = true, warning = "Jalur pemicu pengapian koil utama. Wajib gunakan driver buffer dan pulldown 1k ke GND."),
    Esp32Pin("LEFT", 10, "GPIO26", "PA2", "GATE", "Output", "GPIO26 -> 4.7k -> Driver SCR2 -> BT151 Gate", "Gate Side / Koil Samping (J1.6 via SCR)", "AKTIF GATE SIDE", isCritical = true),
    Esp32Pin("LEFT", 11, "GPIO27", "PB9", "SECONDARY", "Output", "GPIO27 -> 100R -> Gate MOSFET Strobo", "Strobo Manual / Timing Light TDC", "MODE MANUAL"),
    Esp32Pin("LEFT", 12, "GPIO14", "PA10", "SECONDARY", "Input", "Pull-up 4.7k ke 3V3; Overvoltage/fault trip = LOW", "Hardware Fault Shutdown (Aktif-Low)", "PROTEKSI", isCritical = true, warning = "Trip aktif-low menghentikan MCPWM charger seketika jika arus trafo atau HV melonjak."),
    Esp32Pin("LEFT", 13, "GPIO12", "PB1", "SECONDARY", "-", "Strapping pin MTDI", "Cadangan (Harus LOW saat boot)", "CADANGAN"),
    Esp32Pin("LEFT", 14, "GND", "GND", "POWER", "Input", "GND_STAR Motor & PCB", "Ground Titik Bintang Logic", "AKTIF", isCritical = true),
    Esp32Pin("LEFT", 15, "GPIO13", "PB5", "SECONDARY", "Output", "GPIO13 -> IN Modul Relay Opto 5V / BC547 -> J1.7", "Relay Kipas Radiator Motor (J1.7)", "AKTIF FAN RELAY"),
    Esp32Pin("LEFT", 16, "GPIO09 (D2)", "-", "SECONDARY", "Bidir", "Flash D2 terhubung internal SPI flash", "JANGAN DIGUNAKAN (SPI FLASH)", "FLASH INTERNAL", isCritical = true, warning = "Pin terhubung ke memori Flash SPI internal ESP32. Jangan hubungkan ke sirkuit eksternal!"),
    Esp32Pin("LEFT", 17, "GPIO10 (D3)", "-", "SECONDARY", "Bidir", "Flash D3 terhubung internal SPI flash", "JANGAN DIGUNAKAN (SPI FLASH)", "FLASH INTERNAL", isCritical = true, warning = "Pin terhubung ke memori Flash SPI internal ESP32. Jangan hubungkan ke sirkuit eksternal!"),
    Esp32Pin("LEFT", 18, "GPIO11 (CMD)", "-", "SECONDARY", "Bidir", "Flash CMD terhubung internal SPI flash", "JANGAN DIGUNAKAN (SPI FLASH)", "FLASH INTERNAL", isCritical = true, warning = "Pin terhubung ke memori Flash SPI internal ESP32. Jangan hubungkan ke sirkuit eksternal!"),
    Esp32Pin("LEFT", 19, "5V (VIN)", "5V", "POWER", "Input", "Modul Buck 5.00V (MP1584EN / LM2596)", "Catu Daya Utama 5.0V Board", "AKTIF 5V", isCritical = true, warning = "Setel step-down tepat 5.00V sebelum disambungkan ke pin 5V (VIN) ESP32!"),

    // RIGHT HEADER (Sisi Kanan DevKitC V4 - 19 Pin)
    Esp32Pin("RIGHT", 1, "GND", "GND", "POWER", "Input", "GND_STAR Motor & PCB", "Ground Titik Bintang Logic", "AKTIF", isCritical = true),
    Esp32Pin("RIGHT", 2, "GPIO23", "-", "SECONDARY", "Bidir", "VSPI MOSI / Cadangan", "Cadangan I/O SPI", "CADANGAN"),
    Esp32Pin("RIGHT", 3, "GPIO22", "-", "SECONDARY", "Bidir", "I2C SCL", "Cadangan Sensor / Display I2C", "CADANGAN"),
    Esp32Pin("RIGHT", 4, "TX0 (GPIO01)", "PB6", "SECONDARY", "Output", "UART TX Console Debug", "Serial Monitor USB 115200bps", "DEBUG"),
    Esp32Pin("RIGHT", 5, "RX0 (GPIO03)", "PB7", "SECONDARY", "Input", "UART RX Console Debug", "Serial Monitor USB 115200bps", "DEBUG"),
    Esp32Pin("RIGHT", 6, "GPIO21", "-", "SECONDARY", "Bidir", "I2C SDA", "Cadangan Sensor / Display I2C", "CADANGAN"),
    Esp32Pin("RIGHT", 7, "GND", "GND", "POWER", "Input", "GND_STAR", "Ground Tambahan", "AKTIF", isCritical = true),
    Esp32Pin("RIGHT", 8, "GPIO19", "PB8", "SECONDARY", "Output", "GPIO19 -> TC4427 INB -> Gate MOSFET QHV2", "Charger Push-Pull B (MCPWM Channel B)", "AKTIF CHARGER B", isCritical = true),
    Esp32Pin("RIGHT", 9, "GPIO18", "PA9", "SECONDARY", "Output", "GPIO18 -> TC4427 INA -> Gate MOSFET QHV1", "Charger Push-Pull A (MCPWM Channel A)", "AKTIF CHARGER A", isCritical = true),
    Esp32Pin("RIGHT", 10, "GPIO05", "PB6", "BENCH", "Input", "Jumper sementara dari GPIO25 (HANYA UJI BANGKU)", "Loopback Self-Test (Uji Bangku SAJA)", "UJI BANGKU SAJA", warning = "DILARANG dihubungkan saat motor berjalan di jalan raya! Hanya untuk kalibrasi bangku tes."),
    Esp32Pin("RIGHT", 11, "GPIO17", "PB4", "OEM_LEARN", "Input", "J1.6 (Side) -> R 47k 2W -> Modul PC817 IN2+ -> OUT2 ke GPIO17", "OEM Tap Side (Sadapan Paralel J1.6 via PC817)", "AKTIF OEM_LEARN", isCritical = true, warning = "PERINGATAN KERAS: DILARANG menyambungkan kabel koil J1.6 langsung ke GPIO17! WAJIB lewat optocoupler PC817."),
    Esp32Pin("RIGHT", 12, "GPIO16", "PB3", "OEM_LEARN", "Input", "J1.12 (Center) -> R 47k 2W -> Modul PC817 IN1+ -> OUT1 ke GPIO16", "OEM Tap Center (Sadapan Paralel J1.12 via PC817)", "AKTIF OEM_LEARN", isCritical = true, warning = "PERINGATAN KERAS: DILARANG menyambungkan kabel koil J1.12 langsung ke GPIO16! WAJIB lewat optocoupler PC817."),
    Esp32Pin("RIGHT", 13, "GPIO04", "PA0", "PULSER", "Input", "J1.10 (Pickup) -> LM393 Komparator / LM339 -> GPIO4", "Pickup Pulser Utama (TIM Capture / GPIO ISR)", "AKTIF PULSER", isCritical = true, warning = "Gunakan komparator LM393 dengan referensi 0.6V dan batas tegangan 3.3V. Jangan masukkan sinyal 5V atau 12V langsung!"),
    Esp32Pin("RIGHT", 14, "GPIO00", "BOOT0", "POWER", "Input", "Tombol BOOT Onboard", "Mode Flash Firmware ESP32", "BOOT/FLASH"),
    Esp32Pin("RIGHT", 15, "GPIO02", "-", "SECONDARY", "Bidir", "LED Onboard & Strapping Pin", "Status LED / Cadangan", "LED ONBOARD"),
    Esp32Pin("RIGHT", 16, "GPIO15", "-", "SECONDARY", "Bidir", "Strapping Pin MTDO", "Cadangan I/O", "CADANGAN"),
    Esp32Pin("RIGHT", 17, "GPIO08 (D1)", "-", "SECONDARY", "Bidir", "Flash D1 terhubung internal SPI flash", "JANGAN DIGUNAKAN (SPI FLASH)", "FLASH INTERNAL", isCritical = true, warning = "Pin terhubung ke memori Flash SPI internal ESP32. Jangan hubungkan ke sirkuit eksternal!"),
    Esp32Pin("RIGHT", 18, "GPIO07 (D0)", "-", "SECONDARY", "Bidir", "Flash D0 terhubung internal SPI flash", "JANGAN DIGUNAKAN (SPI FLASH)", "FLASH INTERNAL", isCritical = true, warning = "Pin terhubung ke memori Flash SPI internal ESP32. Jangan hubungkan ke sirkuit eksternal!"),
    Esp32Pin("RIGHT", 19, "GPIO06 (CLK)", "-", "SECONDARY", "Bidir", "Flash CLK terhubung internal SPI flash", "JANGAN DIGUNAKAN (SPI FLASH)", "FLASH INTERNAL", isCritical = true, warning = "Pin terhubung ke memori Flash SPI internal ESP32. Jangan hubungkan ke sirkuit eksternal!")
  )

  val componentPinouts: List<ComponentPinout> = listOf(
    ComponentPinout(
      ref = "BT151-600R",
      name = "Thyristor / SCR Pengapian (SCR1 & SCR2)",
      packageType = "TO-220",
      ratingSpec = "600V, 12A (Peak 120A)",
      pinLegs = listOf(
        PinLeg("Pin 1 (Kiri)", "Cathode (K)", "Hubungkan ke GND_POWER (Massa Daya)"),
        PinLeg("Pin 2 (Tengah & Tab)", "Anode (A) + Tab Pendingin", "Hubungkan ke Bank HV (HV_CENTER / HV_SIDE)"),
        PinLeg("Pin 3 (Kanan)", "Gate (G)", "Hubungkan ke resistor 330R dari transistor driver pemicu dan 1k resistor pulldown ke Cathode")
      ),
      orientationGuide = "Pegang komponen menghadap sisi tulisan sablon menghadap Anda dengan pin mengarah ke bawah: Kaki 1 di kiri (Cathode), Kaki 2 di tengah (Anode), Kaki 3 di kanan (Gate). Plat pendingin logam (tab belakang) terhubung internal dengan Anode (tegangan tinggi 285V), jangan sampai menyentuh ground!",
      donorPsuRule = "Wajib beli baru kualitas bagus. Jangan gunakan triac AC.",
      safetyNotice = "Tab logam di belakang BT151 bertegangan tinggi (220-345V) saat bekerja. Jangan tempelkan ke heatsink bersama tanpa mika isolator!"
    ),
    ComponentPinout(
      ref = "IRF3205",
      name = "Power N-Channel MOSFET Charger (QHV1 & QHV2)",
      packageType = "TO-220",
      ratingSpec = "55V, 110A, Rds(on) 8mOhm",
      pinLegs = listOf(
        PinLeg("Pin 1 (Kiri)", "Gate (G)", "Dari pin OUT driver TC4427 via resistor 10R, dan resistor 10k pulldown ke Source"),
        PinLeg("Pin 2 (Tengah & Tab)", "Drain (D)", "Ke lilitan trafo ATX (LV_A untuk QHV1, LV_B untuk QHV2) dan katoda TVS 1.5KE33A"),
        PinLeg("Pin 3 (Kanan)", "Source (S)", "Ke jalur ISENSE_TOP (atas resistor RSENSE 0.05 ohm 5W) dan anoda TVS")
      ),
      orientationGuide = "Pegang komponen dengan tulisan menghadap Anda, kaki menghadap ke bawah: Kaki 1 = Gate (kiri), Kaki 2 = Drain (tengah/tab), Kaki 3 = Source (kanan).",
      donorPsuRule = "Bisa diambil dari PSU PC bekas asalkan marking asli IRF3205 dan hasil ukur multitester MOSFET bagus (tidak bocor D-S).",
      safetyNotice = "Wajib pasang heatsink pendingin kecil beraliran udara atau terisolasi."
    ),
    ComponentPinout(
      ref = "TC4427A / TC4427CPA",
      name = "Dual High-Speed MOSFET Gate Driver",
      packageType = "DIP-8",
      ratingSpec = "Tegangan 4.5V - 18V, Arus Puncak 1.5A Non-Inverting",
      pinLegs = listOf(
        PinLeg("Pin 1", "NC", "Tidak terhubung"),
        PinLeg("Pin 2", "INA", "Input PWM A dari PA9 via resistor 1k dan pulldown 10k ke GND"),
        PinLeg("Pin 3", "GND", "Ground Daya (GND_POWER)"),
        PinLeg("Pin 4", "INB", "Input PWM B dari PB8 via resistor 1k dan pulldown 10k ke GND"),
        PinLeg("Pin 5", "OUTB", "Output penggerak Gate QHV2 via resistor 10R"),
        PinLeg("Pin 6", "VDD", "Catu daya driver dari VIN_HV (setelah FHV/SW_SERVICE opsional)"),
        PinLeg("Pin 7", "OUTA", "Output penggerak Gate QHV1 via resistor 10R"),
        PinLeg("Pin 8", "NC", "Tidak terhubung")
      ),
      orientationGuide = "Dilihat dari atas dengan lekukan / titik penanda (notch) berada di atas: Pin 1 sampai 4 di sisi kiri dari atas ke bawah. Pin 5 sampai 8 di sisi kanan dari bawah ke atas.",
      donorPsuRule = "Wajib beli baru. PENTING: Harus varian non-inverting (TC4427 / TC4427A). Jangan pakai TC4426 (inverting)!",
      safetyNotice = "Wajib pasang kapasitor bypass 100nF paralel 10uF langsung menempel rapat di antara kaki Pin 6 (VDD) dan Pin 3 (GND)."
    ),
    ComponentPinout(
      ref = "LM339N / KA339",
      name = "Quad Voltage Comparator (Pulser & Fault Protection)",
      packageType = "DIP-14",
      ratingSpec = "Supply 2V - 36V, Single/Dual Supply",
      pinLegs = listOf(
        PinLeg("Pin 1", "OUT2", "Output Hardware Fault Arus Lebih -> ke jalur PWM_CLAMP"),
        PinLeg("Pin 2", "OUT1", "Output Pulser Pickup -> pullup 4.7k ke 3V3, 10M histeresis, lalu 1k ke PA0"),
        PinLeg("Pin 3", "VCC", "Catu daya +5V Logic (dari LM2596 OUT+)"),
        PinLeg("Pin 4", "IN1-", "Referensi tegangan nol pulser VZERO (+5V -> 15k -> 10k -> GND, tegangan ~2V)"),
        PinLeg("Pin 5", "IN1+", "Sinyal PICKUP_SENSE dari Pulser J1.10 (via resistor 39k dan BAT54S)"),
        PinLeg("Pin 6", "IN2-", "Sense arus ISENSE_TOP (dari resistor RSENSE 0.05 ohm via 100R)"),
        PinLeg("Pin 7", "IN2+", "Referensi batas arus IREF (3V3 -> 120k -> 10k -> GND, tegangan ~0.25V)"),
        PinLeg("Pin 8", "IN3-", "Tegangan umpan balik HV Center (HV_C_FB)"),
        PinLeg("Pin 9", "IN3+", "Referensi tegangan lebih VOV (+5V -> 120k -> 100k -> GND, ~2.27V clamp ~300V)"),
        PinLeg("Pin 10", "IN4-", "Tegangan umpan balik HV Side (HV_S_FB)"),
        PinLeg("Pin 11", "IN4+", "Referensi tegangan lebih VOV (gabung ke Pin 9)"),
        PinLeg("Pin 12", "GND", "Ground Logic Bintang (GND_STAR)"),
        PinLeg("Pin 13", "OUT4", "Output Fault HV Side Overvoltage -> ke jalur PWM_CLAMP"),
        PinLeg("Pin 14", "OUT3", "Output Fault HV Center Overvoltage -> ke jalur PWM_CLAMP")
      ),
      orientationGuide = "Dilihat dari atas dengan lekukan (notch) di atas: Pin 1-7 di sisi kiri dari atas ke bawah. Pin 8-14 di sisi kanan dari bawah ke atas.",
      donorPsuRule = "Bisa menggunakan LM339N / KA339 lepasan PSU PC bekas yang berfungsi normal.",
      safetyNotice = "Solder kapasitor keramik 100nF sedekat mungkin langsung antara Pin 3 (VCC) dan Pin 12 (GND) untuk mencegah osilasi liar pulser."
    ),
    ComponentPinout(
      ref = "BC547B",
      name = "Transistor NPN Sinyal Kecil (QNC, QNS, QFAN)",
      packageType = "TO-92",
      ratingSpec = "45V, 100mA, hFE 200-450",
      pinLegs = listOf(
        PinLeg("Pin 1 (Kiri)", "Collector (C)", "Ke basis transistor PNP penggerak pulsa / beban"),
        PinLeg("Pin 2 (Tengah)", "Base (B)", "Dari pin GPIO STM32 via resistor 4.7k, dan 10k pulldown ke GND"),
        PinLeg("Pin 3 (Kanan)", "Emitter (E)", "Ke Ground (GND)")
      ),
      orientationGuide = "Dilihat dari sisi permukaan datar (ada sablon tulisan BC547B) dengan pin menghadap ke bawah: Urutan kaki dari kiri ke kanan adalah Collector (1), Base (2), Emitter (3).",
      donorPsuRule = "Bisa diambil dari PSU atau beli baru.",
      safetyNotice = "Pastikan tidak terbalik antara Base dan Collector."
    ),
    ComponentPinout(
      ref = "BC557B",
      name = "Transistor PNP Sinyal Kecil Driver Gate (QPC, QPS)",
      packageType = "TO-92",
      ratingSpec = "45V, 100mA, hFE 200-450",
      pinLegs = listOf(
        PinLeg("Pin 1 (Kiri)", "Collector (C)", "Ke Gate SCR BT151 via resistor 330R"),
        PinLeg("Pin 2 (Tengah)", "Base (B)", "Ke Collector BC547 via resistor 2.2k, dan 10k pullup ke VIN_HV"),
        PinLeg("Pin 3 (Kanan)", "Emitter (E)", "Ke tegangan suplai pemicu VIN_HV")
      ),
      orientationGuide = "Dilihat dari sisi datar tulisan dengan pin menghadap ke bawah: Urutan pin dari kiri ke kanan adalah Collector (1), Base (2), Emitter (3).",
      donorPsuRule = "Beli baru atau gunakan stok PNP berkualitas.",
      safetyNotice = "Emitter terhubung ke VIN_HV (12V). Jangan sampai salah menyambung ke pin 3.3V logic!"
    ),
    ComponentPinout(
      ref = "BAT54S",
      name = "Dual Schottky Barrier Diode (SOT-23 Clamp Proteksi ADC)",
      packageType = "SOT-23 (SMD 3 Kaki)",
      ratingSpec = "30V, 200mA, Tegangan Maju Rendah 0.3V",
      pinLegs = listOf(
        PinLeg("Pin 1 (Kiri Bawah)", "Anode D1 (A1)", "Hubungkan ke Ground (GND)"),
        PinLeg("Pin 2 (Kanan Bawah)", "Cathode D2 (K2)", "Hubungkan ke jalur 3.3V (H_TOP.3)"),
        PinLeg("Pin 3 (Tengah Atas)", "Common (K1 + A2)", "Hubungkan ke simpul sinyal ADC yang dilindungi")
      ),
      orientationGuide = "Komponen SMD SOT-23 dilihat dengan dua kaki di bawah dan satu kaki di atas: Kaki kiri bawah = Pin 1 (A1), Kaki kanan bawah = Pin 2 (K2), Kaki tunggal di atas = Pin 3 (Common clamp).",
      donorPsuRule = "Wajib beli baru. Komponen ini adalah tameng utama pelindung pin ADC MCU dari tegangan lonjakan busi.",
      safetyNotice = "Jika menyolder SOT-23 di PCB lubang, solder adapter pin header atau rentangkan kawat kawat jumper halus dengan hati-hati."
    ),
    ComponentPinout(
      ref = "Trafo T1 (ATX PC)",
      name = "Trafo Inti Ferrite Push-Pull Step-Up",
      packageType = "Trafo Utama Bekas PSU Komputer (EE-35 / EI-33)",
      ratingSpec = "Input 12V Push-Pull Center Tap -> Output Sekunder AC 200-300V",
      pinLegs = listOf(
        PinLeg("LV_CT", "Center-Tap Sekunder 5V Lama", "Tersambung ke VIN_HV (12V setelah FHV/SW_SERVICE opsional)"),
        PinLeg("LV_A", "Kaki Kiri 5V Lama", "Tersambung ke Drain MOSFET QHV1"),
        PinLeg("LV_B", "Kaki Kanan 5V Lama", "Tersambung ke Drain MOSFET QHV2"),
        PinLeg("HV_AC1", "Kaki Primer Tegangan Tinggi Lama 1", "Ke input jembatan dioda DREC1 (Anode) & DREC3 (Cathode)"),
        PinLeg("HV_AC2", "Kaki Primer Tegangan Tinggi Lama 2", "Ke input jembatan dioda DREC2 (Anode) & DREC4 (Cathode)")
      ),
      orientationGuide = "Trafo ATX digunakan terbalik: Lilitan 5V center-tap yang dahulu menghasilkan 5V kini menjadi input primer 12V push-pull. Lilitan primer tegangan tinggi 300V lama kini menjadi keluaran sekunder AC tegangan tinggi. Lilitan 3.3V, 12V, dan auxiliary dipotong atau diisolasi.",
      donorPsuRule = "WAJIB trafo utama ATX utuh tanpa dibongkar atau dililit ulang! Dilarang menggunakan trafo raket nyamuk, flyback TV, atau charger hp.",
      safetyNotice = "Keluaran AC menghasilkan tegangan tinggi mematikan hingga target PRO 345V. Jaga jarak isolasi (clearance) minimal 6mm di PCB lubang."
    ),
    ComponentPinout(
      ref = "C_CENTER / C_SIDE",
      name = "Kapasitor Buang Muatan CDI (MKP/MPP Pulse)",
      packageType = "Film Kotak Pitch 22.5mm / 27.5mm",
      ratingSpec = "1.0 uF, Tegangan 630V DC (Pulse Grade dV/dt tinggi)",
      pinLegs = listOf(
        PinLeg("Terminal A", "Sisi Tegangan Tinggi", "Terhubung ke HV_CENTER / HV_SIDE, Anoda SCR BT151, dan 4x resistor bleeder 470k"),
        PinLeg("Terminal B", "Sisi Pigtail Koil NS200", "Terhubung ke J1.12 (Center) / J1.6 (Side) dan ujung resistor bleeder 470k")
      ),
      orientationGuide = "Kapasitor non-polar film. Dipasang kokoh di PCB Power.",
      donorPsuRule = "MUTLAK DILARANG memakai Elko (Elektrolit) atau kapasitor X2 275VAC biasa! Wajib kapasitor polypropylene pulse MKP/MPP rating 630V.",
      safetyNotice = "Menyimpan muatan energi tinggi. Selalu pasang 4x resistor bleeder 470k 0.5W secara seri melintang di kedua terminalnya untuk membuang muatan otomatis saat motor mati."
    ),
    ComponentPinout(
      ref = "J_TPS",
      name = "Header Selektor Pinout Sensor TPS NS200",
      packageType = "Header Pin Male 2x3 (Pitch 2.54mm) + 2 Jumper Shunt",
      ratingSpec = "2.54mm pitch standard",
      pinLegs = listOf(
        PinLeg("Pin 1", "J1.2 (Kabel Hijau-Putih)", "Sisi Baris 1 Kolom 1"),
        PinLeg("Pin 2", "TPS_REF (+5V Referensi)", "Sisi Baris 1 Kolom 2"),
        PinLeg("Pin 3", "J1.4 (Kabel Abu-Abu)", "Sisi Baris 1 Kolom 3"),
        PinLeg("Pin 4", "J1.4 (Kabel Abu-Abu)", "Sisi Baris 2 Kolom 1"),
        PinLeg("Pin 5", "TPS_SIG (Sinyal Wiper ke PA3)", "Sisi Baris 2 Kolom 2"),
        PinLeg("Pin 6", "J1.2 (Kabel Hijau-Putih)", "Sisi Baris 2 Kolom 3")
      ),
      orientationGuide = "Dilihat dari atas: [1: J1.2] [2: TPS_REF] [3: J1.4] di baris atas; [4: J1.4] [5: TPS_SIG] [6: J1.2] di baris bawah. Posisi Jumper A: pasang di (1-2) dan (4-5). Posisi Jumper B: pasang di (2-3) dan (5-6). Dilarang memasang jumper di posisi silang atau keempatnya sekaligus!",
      donorPsuRule = "Header pin male 2.54mm standar dan shunt jumper komputer bekas.",
      safetyNotice = "Pasang jumper hanya setelah memastikan kabel referensi menghasilkan 5V stabil dan kabel sinyal wiper bergerak halus saat handle gas diputar."
    )
  )

  val wiringSteps: List<WiringStep> = listOf(
    // TAHAP 1: GROUND BINTANG & CATU DAYA LOGIC
    WiringStep(
      id = "step_1_1",
      stageId = 1,
      stageTitle = "Tahap 1: Catu Daya Logic & Proteksi Dasar",
      stepNumber = "1.1",
      title = "Pembuatan Titik Ground Bintang (GND_STAR) dari J1.11",
      board = PcbBoard.PCB_LOGIC,
      sourcePin = "Harness J1.11 (Hitam-Kuning)",
      targetPin = "Pusat Jalur Tembaga GND_STAR di PCB Logic",
      components = listOf(
        StepComponent("J1.11", "Kabel Harness NS200", "Kabel Hitam-Kuning", "Pin 11 baris bawah pigtail harness"),
        StepComponent("GND_BUS", "Rel Kawat Tembaga 1.0mm", "Tembaga tunggal tebal", "Dibentangkan di sepanjang baris tengah PCB 7x9cm")
      ),
      schematicTrace = "Harness J1.11 (Hitam-Kuning) ===> GND_STAR (Pusat Bintang Massa di PCB Logic)",
      perfboardTips = listOf(
        "Pilih salah satu baris lubang memanjang di PCB lubang 7x9cm untuk dijadikan Bus Ground Utama.",
        "Solder kawat tembaga telanjang tebal (1.0 mm / AWG 18) lurus di sepanjang baris tersebut.",
        "Solder kabel Hitam-Kuning dari pigtail J1.11 langsung ke titik tengah bus tembaga ini.",
        "Beri tanda silkscreen / spidol permanen hitam 'GND_STAR' pada papan PCB."
      ),
      pinLegGuide = "J1.11 adalah pin nomor 11 pada soket pigtail (baris bawah, nomor dua dari kanan bila dilihat dari depan soket dengan klip pengunci di atas).",
      verificationRequirement = "Periksa dengan multimeter mode kontinuitas/buzzer: pin J1.11 harus terhubung langsung ke seluruh titik GND_STAR dengan resistansi < 0.2 Ohm.",
      verificationType = VerificationType.MULTIMETER_CONTINUITY,
      expectedValue = "< 0.2 Ohm (Buzzer Berbunyi)",
      criticalSafetyWarning = "Semua ground rangkaian logic, LM339 pin 12, dan WeAct G wajib terhubung langsung ke GND_STAR ini untuk mencegah lonjakan tegangan!"
    ),
    WiringStep(
      id = "step_1_2",
      stageId = 1,
      stageTitle = "Tahap 1: Catu Daya Logic & Proteksi Dasar",
      stepNumber = "1.2",
      title = "Penyolderan Proteksi Input 12V (FMAIN, DREV, TVS, L_IN, C_IN)",
      board = PcbBoard.PCB_LOGIC,
      sourcePin = "Harness J1.5 (+12V Kontak)",
      targetPin = "Rel Tegangan Bersih VIN_FILT",
      components = listOf(
        StepComponent("FMAIN", "Fuse Sikring Utama", "5A Blade + Rumah Sikring", "Dipasang seri pertama dari J1.5"),
        StepComponent("DREV", "Dioda Schottky Anti-Terbalik", "SB560 (5A 60V)", "Anoda ke FMAIN, Katoda ke VIN_PROT"),
        StepComponent("TVS_IN", "TVS Clamp Surge Aki", "SMBJ33A / P6KE33A", "Katoda ke VIN_PROT, Anoda ke GND_STAR"),
        StepComponent("L_IN", "Induktor Filter Choke", "47uH arus >= 5A (Donasi PSU)", "Menjembatani VIN_PROT ke VIN_FILT"),
        StepComponent("C_IN", "Kapasitor Filter Input", "470uF 35V Low-ESR", "Kaki (+) ke VIN_FILT, Kaki (-) ke GND_STAR")
      ),
      schematicTrace = "J1.5 -> FMAIN 5A -> DREV (Anode) | DREV (Cathode/Garis) -> VIN_PROT -> [TVS_IN ke GND] -> L_IN 47uH -> VIN_FILT -> [C_IN 470uF ke GND]",
      perfboardTips = listOf(
        "Pasang fuse holder 5A di tepi luar PCB dekat jalur masuk kabel J1.5.",
        "Solder Dioda SB560: perhatikan gelang perak (Katoda) menghadap ke arah dalam sirkuit (VIN_PROT).",
        "Solder TVS diode SMBJ33A: kaki katoda (ada garis) ke VIN_PROT, kaki anoda ke jalur GND_STAR.",
        "Pasang choke toroid 47uH bekas PSU PC di antara simpul VIN_PROT dan VIN_FILT.",
        "Solder kapasitor Elko 470uF 35V: perhatikan garis strip minus (-) terhubung ke GND_STAR."
      ),
      pinLegGuide = "SB560: Gelang perak adalah Katoda. TVS: Gelang putih/perak adalah Katoda. Elko: Kaki panjang adalah (+), garis strip putih abu-abu di body adalah (-).",
      verificationRequirement = "Uji kontinuitas dioda: ukur maju DREV (drop ~0.3V - 0.4V), ukur balik harus Open-Loop (OL). Pastikan TIDAK ADA korsleting antara VIN_FILT dan GND_STAR (resistansi harus naik bertahap seiring pengisian kapasitor).",
      verificationType = VerificationType.MULTIMETER_OHM,
      expectedValue = "> 50 kOhm (Tidak korslet ke Ground)",
      criticalSafetyWarning = "Dilarang mem-bypass sikring FMAIN 5A atau dioda DREV. Dioda ini melindungi seluruh mikrokontroler jika polaritas aki motor terbalik!"
    ),
    WiringStep(
      id = "step_1_3",
      stageId = 1,
      stageTitle = "Tahap 1: Catu Daya Logic & Proteksi Dasar",
      stepNumber = "1.3",
      title = "Penyolderan & Kalibrasi Modul Step-Down LM2596 (5.00V Logic)",
      board = PcbBoard.PCB_LOGIC,
      sourcePin = "VIN_FILT (via FLOGIC 1A)",
      targetPin = "Rel Tegangan +5V_LOGIC",
      components = listOf(
        StepComponent("FLOGIC", "Sikring Daya Logic", "Fuse 1A + Socket", "Antara VIN_FILT dan IN+ modul LM2596"),
        StepComponent("U_BUCK_LOGIC", "Modul LM2596 Step-Down", "Input 12V -> Output Disetel 5.00V 1A", "Modul DC-DC mini dengan trimpot multi-turn")
      ),
      schematicTrace = "VIN_FILT -> FLOGIC 1A -> Modul LM2596 (IN+) | GND_STAR -> Modul (IN-) | Modul (OUT+) disetel 5.00V -> +5V_LOGIC | Modul (OUT-) -> GND_STAR",
      perfboardTips = listOf(
        "Solder pin header 4-kaki untuk mendudukkan modul LM2596 di PCB lubang.",
        "Hubungkan IN+ modul ke fuse FLOGIC 1A, dan IN- modul ke GND_STAR.",
        "PENTING: JANGAN sambungkan OUT+ ke sirkuit WeAct dulu sebelum dikalibrasi!",
        "Beri tegangan 12V pada input, ukur kaki OUT+ dengan voltmeter DC, putar trimpot obeng min hingga output tepat terbaca 5.00 Volt."
      ),
      pinLegGuide = "Terminal modul berlabel jelas: IN+ (Input 12V), IN- (Ground Input), OUT+ (+5V Output stabil), OUT- (Ground Output terhubung GND_STAR).",
      verificationRequirement = "Ukur tegangan pada OUT+ modul LM2596 menggunakan multimeter digital skala 20V DC. Nilai harus berada dalam rentang 4.95V - 5.05V sebelum disambung ke WeAct!",
      verificationType = VerificationType.MULTIMETER_VOLT,
      expectedValue = "Tepat 5.00V ± 0.05V",
      criticalSafetyWarning = "Jika output melebihi 5.25V, WeAct STM32 dan IC LM339 akan rusak permanen. Verifikasi tegangan ini wajib mutlak!"
    ),
    WiringStep(
      id = "step_1_4",
      stageId = 1,
      stageTitle = "Tahap 1: Catu Daya Logic & Proteksi Dasar",
      stepNumber = "1.4",
      title = "Pemasangan Header Dudukan WeAct STM32WB55 & Catu Daya MCU",
      board = PcbBoard.PCB_LOGIC,
      sourcePin = "Rel +5V_LOGIC & GND_STAR",
      targetPin = "WeAct H_BOTTOM.2 (5V) & H_BOTTOM.1 / H_TOP.1 (GND)",
      components = listOf(
        StepComponent("SOCKET_MCU", "Female Header Socket 2.54mm", "1x15 pin (H_TOP) & 1x20 pin (H_BOTTOM)", "Dudukan untuk board WeAct agar bisa dicopot"),
        StepComponent("U1", "WeAct STM32WB55CGU6", "Cortex-M4 + BLE 2.4GHz", "MCU pengendali utama pengapian")
      ),
      schematicTrace = "+5V_LOGIC -> H_BOTTOM.2 (Pin 2 WeAct 5V) | GND_STAR -> H_BOTTOM.1 (Pin 1 WeAct G) dan H_TOP.1 (Pin 1 WeAct G)",
      perfboardTips = listOf(
        "Pasang dua baris soket female header 2.54mm di PCB lubang 7x9cm dengan jarak sesuai modul WeAct.",
        "Orientasikan soket: port USB di sebelah kiri dan Antena PCB di sebelah kanan.",
        "PENTING: Kosongkan tembaga dan kabel pada area di bawah antena board WeAct (bebas logam minimal 15mm)!",
        "Solder jalur kabel dari OUT+ LM2596 (5.00V) ke lubang H_BOTTOM.2.",
        "Solder jalur ground dari GND_STAR ke H_BOTTOM.1 dan H_TOP.1.",
        "PIN H_BOTTOM.4 (VBAT) DIBIARKAN KOSONG! JANGAN PERNAH DIHUBUNGKAN KE 12V!"
      ),
      pinLegGuide = "Dilihat dari sisi komponen WeAct (USB kiri, Antena kanan): H_BOTTOM Pin 1 = G, Pin 2 = 5V, Pin 4 = VBAT (JANGAN 12V!). H_TOP Pin 1 = G, Pin 3 = 3V3.",
      verificationRequirement = "Pasang WeAct ke soket, nyalakan sumber 12V kontak. LED indikator merah power pada modul WeAct harus menyala terang, dan ukur tegangan pada H_TOP.3 (pin 3V3) harus terbaca 3.28V - 3.32V.",
      verificationType = VerificationType.MULTIMETER_VOLT,
      expectedValue = "3.30V pada pin 3V3 WeAct",
      criticalSafetyWarning = "H_BOTTOM.4 (VBAT) adalah pin backup baterai koin RTC 3V! Jika tersenggol kabel aki 12V, chip STM32WB55 akan langsung hangus!"
    ),

    // TAHAP 2: INTERLOCK, SENSOR & LOGIC WEACT
    WiringStep(
      id = "step_2_1",
      stageId = 2,
      stageTitle = "Tahap 2: Rangkaian Interlock, Sensor & Proteksi Sinyal",
      stepNumber = "2.1",
      title = "Penyolderan Pembagi Tegangan Monitor Aki (VBAT ADC) ke PB0",
      board = PcbBoard.PCB_LOGIC,
      sourcePin = "VIN_FILT (Tegangan Aki Setelah Filter)",
      targetPin = "WeAct H_TOP.14 (PB0 / ADC1_IN15)",
      components = listOf(
        StepComponent("R_BAT1", "Resistor Pembagi Atas", "100k Ohm 1% 0.25W", "Dari VIN_FILT ke simpul VBAT_ADC"),
        StepComponent("R_BAT2", "Resistor Pembagi Bawah", "22k Ohm 1% 0.25W", "Dari simpul VBAT_ADC ke GND_STAR"),
        StepComponent("C_BAT", "Kapasitor Peredam Noise", "10nF Keramik 50V", "Paralel dengan R_BAT2 ke GND_STAR"),
        StepComponent("R_PROT_BAT", "Resistor Pembatas Arus MCU", "1k Ohm 0.25W", "Dari simpul VBAT_ADC ke PB0"),
        StepComponent("DBAT_BAT", "Dioda Pengaman Clamp", "BAT54S Schottky Dual SOT-23", "Pin 1 ke GND, Pin 2 ke 3V3, Pin 3 ke VBAT_ADC")
      ),
      schematicTrace = "VIN_FILT -> 100k -> VBAT_ADC -> 1k -> H_TOP.14 (PB0) | VBAT_ADC -> [22k || 10nF] -> GND_STAR | BAT54S Clamp",
      perfboardTips = listOf(
        "Tempatkan R_BAT1 (100k) dan R_BAT2 (22k) berdekatan di sebelah soket H_TOP WeAct.",
        "Solder kapasitor keramik 10nF paralel langsung di kaki R_BAT2.",
        "Solder BAT54S: Kaki 1 ke GND_STAR, Kaki 2 ke pin 3V3 (H_TOP.3), Kaki 3 ke simpul VBAT_ADC.",
        "Sambungkan kabel dari resistor 1k ke H_TOP.14 (PB0)."
      ),
      pinLegGuide = "BAT54S: Dua kaki di bawah, satu kaki di atas. Kiri bawah = Pin 1 (GND), Kanan bawah = Pin 2 (3V3), Atas tengah = Pin 3 (Simpul ADC).",
      verificationRequirement = "Beri tegangan aki 12.0V pada input J1.5. Ukur tegangan pada H_TOP.14 (PB0) dengan multimeter: harus terbaca sekitar 2.16V (Rasio 22k / (100k + 22k) = 0.1803 x 12V).",
      verificationType = VerificationType.MULTIMETER_VOLT,
      expectedValue = "2.16V ± 0.1V (pada input 12V)",
      criticalSafetyWarning = "Pastikan tegangan pada PB0 tidak pernah melebihi 3.3V bahkan saat pengisian aki motor mencapai 14.8V!"
    ),
    WiringStep(
      id = "step_2_2",
      stageId = 2,
      stageTitle = "Tahap 2: Rangkaian Interlock, Sensor & Proteksi Sinyal",
      stepNumber = "2.2",
      title = "Pemasangan Jalur Sinyal OEM Learn PB3 (Center) & PB4 (Side)",
      board = PcbBoard.PCB_LOGIC,
      sourcePin = "Harness J1.12 (Center) & J1.6 (Side) via R Seri 47k 2W",
      targetPin = "WeAct H_TOP.9 (PB3) & H_TOP.8 (PB4)",
      components = listOf(
        StepComponent("MOD_PC817", "Modul Optocoupler PC817 4-Channel", "Modul Siap Pakai Pasaran (Drop-In)", "IN1 ke J1.12 via 47k 2W, IN2 ke J1.6 via 47k 2W, OUT1 ke PB3, OUT2 ke PB4"),
        StepComponent("R_DAMP_CTR", "Resistor Seri Peredam Lonjakan Center", "47k Ohm 2W Metal Film / Oxyd", "Diseri pada kabel dari J1.12 (Oranye) sebelum masuk terminal IN1+ modul"),
        StepComponent("R_DAMP_SIDE", "Resistor Seri Peredam Lonjakan Side", "47k Ohm 2W Metal Film / Oxyd", "Diseri pada kabel dari J1.6 (Hitam-Merah) sebelum masuk terminal IN2+ modul")
      ),
      schematicTrace = "J1.12 -> R 47k/2W -> IN1+ [Modul PC817] -> OUT1 -> H_TOP.9 (PB3 OEM_CTR) | J1.6 -> R 47k/2W -> IN2+ [Modul PC817] -> OUT2 -> H_TOP.8 (PB4 OEM_SIDE)",
      perfboardTips = listOf(
        "REKOMENDASI UTAMA MODULAR: Gunakan 'Modul Optocoupler PC817 4-Channel' dengan terminal baut sekrup (Rp 15.000 - Rp 25.000). Tidak perlu custom PCB atau solder IC kecil!",
        "Catatan Firmware R8: Logika interlock hardware fisik lama (R_ARM, JP_PRO) telah sepenuhnya dihapus. Firmware R8 mengontrol ARM, Safe DIY, dan PRO Voltage secara cerdas via software di aplikasi.",
        "Pin PB3 & PB4 kini didedikasikan sebagai probe monitor pasif untuk Mode OEM_LEARN membaca sinyal CDI bawaan motor.",
        "Konektor J1.8 & J1.9 tetap ditandai NC dari pabrik, tetapi dapat diberi kabel tambahan sebagai jumper rapi probe pulsa OEM.",
        "PERINGATAN KESELAMATAN: Wajib pasang resistor seri 47k 2W pada kabel koil sebelum masuk ke modul optocoupler untuk meredam tegangan spike 200V-400V!",
        "Jumper level tegangan pada modul PC817 diatur ke posisi VCC (3.3V WeAct) agar pulsa output berayun bersih 0V - 3.3V ke PB3 dan PB4."
      ),
      pinLegGuide = "Modul PC817: IN1+ (Koil Center J1.12 via 47k 2W), IN1- (GND J1.11), IN2+ (Koil Side J1.6 via 47k 2W), IN2- (GND J1.11). Sisi output: VCC (3V3 WeAct), GND (GND WeAct), OUT1 (PB3), OUT2 (PB4).",
      verificationRequirement = "Nyalakan mesin dengan CDI OEM: LED1 dan LED2 pada modul optocoupler harus berkedip seirama percikan busi. Pada aplikasi, counter pulsa OEM Center (PB3) dan sampel OEM Side (PB4) bertambah naik.",
      verificationType = VerificationType.VISUAL_INSPECTION,
      expectedValue = "LED1 & LED2 Kedip Aktif, Pulsa Terbaca di Aplikasi",
      criticalSafetyWarning = "DILARANG menyambung kabel koil J1.12 dan J1.6 langsung ke pin STM32 tanpa optocoupler dan resistor seri 47k 2W! Tegangan induksi koil akan membakar MCU seketika."
    ),
    WiringStep(
      id = "step_2_3",
      stageId = 2,
      stageTitle = "Tahap 2: Rangkaian Interlock, Sensor & Proteksi Sinyal",
      stepNumber = "2.3",
      title = "Penyolderan Rangkaian Sensor Suhu J1.3 (TEMP ADC) ke PA4",
      board = PcbBoard.PCB_LOGIC,
      sourcePin = "Harness J1.3 (Hitam-Putih)",
      targetPin = "WeAct H_BOTTOM.13 (PA4 / ADC)",
      components = listOf(
        StepComponent("R_TEMP_PU", "Resistor Pull-up Sensor", "4.7k Ohm 1% 0.25W", "Dari +5V_LOGIC ke J1.3"),
        StepComponent("R_TEMP_SERI", "Resistor Seri Filter", "15k Ohm 0.25W", "Dari J1.3 ke simpul TEMP_ADC"),
        StepComponent("R_TEMP_DIV", "Resistor Pembagi Bawah", "27k Ohm 1% 0.25W", "Dari simpul TEMP_ADC ke GND_STAR"),
        StepComponent("C_TEMP", "Kapasitor Filter Sinyal", "10nF Keramik", "Paralel dengan R_TEMP_DIV ke GND_STAR"),
        StepComponent("R_TEMP_IN", "Resistor Proteksi Pin MCU", "1k Ohm 0.25W", "Dari TEMP_ADC ke PA4"),
        StepComponent("DBAT_TEMP", "Dioda Pengaman Clamp", "BAT54S Schottky Dual", "Pin 1 ke GND, Pin 2 ke 3V3, Pin 3 ke TEMP_ADC")
      ),
      schematicTrace = "+5V_LOGIC -> 4.7k -> J1.3; J1.3 -> 15k -> TEMP_ADC -> 1k -> H_BOTTOM.13 (PA4) | TEMP_ADC -> [27k || 10nF] -> GND | BAT54S Clamp",
      perfboardTips = listOf(
        "Rangkaian ini mencocokkan sensor suhu NTC bawaan silinder head Pulsar NS200.",
        "Solder resistor 4.7k pull-up langsung ke rel 5V logic.",
        "Pastikan BAT54S terpasang benar mengapit ke 3V3 dan GND untuk mencegah lonjakan induksi termostat."
      ),
      pinLegGuide = "J1.3 adalah pin nomor 3 baris atas pigtail harness (kabel Hitam-Putih).",
      verificationRequirement = "Sebelum soket harness dicolok ke motor, ukur resistansi ke ground pada J1.3. Saat diberi 5V, tegangan open-circuit di J1.3 harus terbaca 5.0V, dan pada PA4 harus terbaca ~3.21V.",
      verificationType = VerificationType.MULTIMETER_VOLT,
      expectedValue = "Open circuit PA4 = ~3.2V (Clamp bekerja)",
      criticalSafetyWarning = "Firmware saat ini belum memakai suhu untuk proteksi overheat. Jangan abaikan BAT54S clamp dan gunakan proteksi termal motor yang independen."
    ),
    WiringStep(
      id = "step_2_4",
      stageId = 2,
      stageTitle = "Tahap 2: Rangkaian Interlock, Sensor & Proteksi Sinyal",
      stepNumber = "2.4",
      title = "Penyolderan Header J_TPS (2x3) & Jaringan ADC Sinyal Gas",
      board = PcbBoard.PCB_LOGIC,
      sourcePin = "Harness J1.2 & J1.4",
      targetPin = "WeAct H_BOTTOM.12 (PA3 TPS_SIG) & H_BOTTOM.14 (PA5 TPS_REF)",
      components = listOf(
        StepComponent("J_TPS", "Header Selektor TPS", "Male Pin Header 2x3 Pitch 2.54mm", "Dihubungkan ke pin pigtail J1.2 dan J1.4"),
        StepComponent("R_TPS_REF", "Resistor Pembatas Arus Ref", "100R Ohm 0.25W", "Dari +5V_LOGIC ke TPS_REF (Pin 2 header)"),
        StepComponent("DIV_TPS_SIG", "Rangkaian Pembagi TPS_SIG", "15k + 27k || 4.7nF + 1k", "Menghubungkan Pin 5 header ke PA3"),
        StepComponent("DIV_TPS_REF", "Rangkaian Pembagi TPS_REF", "15k + 27k || 4.7nF + 1k", "Menghubungkan Pin 2 header ke PA5"),
        StepComponent("DBAT_TPS", "Dua Pasang BAT54S Clamp", "2x BAT54S SOT-23", "Masing-masing mengamankan simpul PA3 dan PA5")
      ),
      schematicTrace = "J_TPS 2x3: [1=J1.2, 2=TPS_REF, 3=J1.4] / [4=J1.4, 5=TPS_SIG, 6=J1.2]. TPS_SIG -> 15k -> 1k -> PA3. TPS_REF -> 15k -> 1k -> PA5.",
      perfboardTips = listOf(
        "Solder header male 2x3 dengan kokoh di dekat tepi PCB Logic.",
        "Kabel Hijau-Putih (J1.2) disolder gabung ke Pin 1 dan Pin 6.",
        "Kabel Abu-Abu (J1.4) disolder gabung ke Pin 3 dan Pin 4.",
        "Pin 2 adalah jalur tegangan referensi (+5V melalui resistor 100R).",
        "Pin 5 adalah jalur sinyal wiper potentiometer TPS.",
        "Pasang jumper shunt pada Posisi A: Jumper di (1-2) dan (4-5), atau Posisi B: Jumper di (2-3) dan (5-6)."
      ),
      pinLegGuide = "Dilihat dari atas header: Baris 1: Pin 1 (kiri), Pin 2 (tengah), Pin 3 (kanan). Baris 2: Pin 4 (kiri), Pin 5 (tengah), Pin 6 (kanan).",
      verificationRequirement = "Pasang jumper Posisi A. Putar handle gas motor perlahan dari tertutup ke buka penuh: tegangan pada H_BOTTOM.12 (PA3) harus bergerak mulus naik tanpa ada lonjakan putus.",
      verificationType = VerificationType.MULTIMETER_VOLT,
      expectedValue = "Tegangan gas tertutup ~0.4V - 0.9V, buka penuh ~2.2V - 2.8V",
      criticalSafetyWarning = "JANGAN PERNAH memasang jumper di keempat pasang pin sekaligus, karena akan mengkorsletkan tegangan referensi 5V ke sinyal ground!"
    ),
    WiringStep(
      id = "step_2_5",
      stageId = 2,
      stageTitle = "Tahap 2: Rangkaian Interlock, Sensor & Proteksi Sinyal",
      stepNumber = "2.5",
      title = "Penyolderan Rangkaian Komparator Pulser LM339 (J1.10 ke PA0)",
      board = PcbBoard.PCB_LOGIC,
      sourcePin = "Harness J1.10 (Putih-Merah Pulser)",
      targetPin = "WeAct H_BOTTOM.9 (PA0 / TIM2_CH1) & LM339",
      components = listOf(
        StepComponent("U2_SOCKET", "Soket IC DIP-14", "DIP-14 Socket 2.54mm", "Dudukan untuk IC komparator LM339"),
        StepComponent("R_PULSE_IN", "Resistor Input Daya Pulser", "39k Ohm 0.5W Metal Film", "Dari J1.10 ke simpul PICKUP_SENSE"),
        StepComponent("DIV_VMID", "Pembagi Tegangan Bias Nol", "2x 10k Ohm + 10k Ohm ke Sense", "Membias pulser AC di tengah tegangan 2.5V"),
        StepComponent("DIV_VZERO", "Pembagi Referensi Nol Komparator", "15k (ke 5V) + 10k (ke GND)", "Menghasilkan ambang batas ~2.0V pada Pin 4 LM339"),
        StepComponent("R_HYST", "Resistor Histeresis Schmitt", "10M Ohm 0.25W", "Umpan balik positif dari Pin 2 OUT ke Pin 5 IN1+"),
        StepComponent("R_PU_3V3", "Resistor Pull-up Output", "4.7k Ohm ke 3V3 (H_TOP.3)", "Menarik output open-collector Pin 2 ke 3.3V logic"),
        StepComponent("R_TO_MCU", "Resistor Pembatas Arus PA0", "1k Ohm 0.25W", "Dari Pin 2 LM339 ke H_BOTTOM.9 PA0"),
        StepComponent("C_BYPASS", "Kapasitor Decoupling IC", "100nF Keramik Multilayer", "Dipasang langsung menempel di Pin 3 (VCC) dan Pin 12 (GND)")
      ),
      schematicTrace = "J1.10 -> 39k/0.5W -> PICKUP_SENSE [BAT54S clamp & 10k ke VMID] -> LM339 Pin 5 (IN1+) | Pin 4 (IN1-) dari VZERO ~2V | Pin 2 (OUT1) -> 4.7k ke 3V3, 10M ke Pin 5, dan 1k ke PA0",
      perfboardTips = listOf(
        "Pasang soket DIP-14 dengan takik/notch menghadap ke atas.",
        "Solder kaki Pin 3 ke +5V_LOGIC dan Pin 12 ke GND_STAR.",
        "Kapasitor 100nF wajib disolder langsung menempel di antara Pin 3 dan Pin 12 di bawah PCB.",
        "Gunakan resistor 39k dengan rating 0.5 Watt karena menerima pulsa tegangan tinggi saat RPM puncak.",
        "Pasang BAT54S clamp di simpul PICKUP_SENSE (Pin 1 ke GND, Pin 2 ke 3V3, Pin 3 ke PICKUP_SENSE)."
      ),
      pinLegGuide = "LM339 DIP-14: Pin 2 = OUT1 (ke PA0), Pin 3 = VCC (+5V), Pin 4 = IN1- (Referensi), Pin 5 = IN1+ (Pulser), Pin 12 = GND.",
      verificationRequirement = "Ukur tegangan tanpa starter: Pin 4 (IN1-) harus terbaca ~2.0V. Pin 5 (IN1+) harus terbaca ~2.5V (karena bias VMID). Output Pin 2 harus berada pada status HIGH ~3.3V.",
      verificationType = VerificationType.MULTIMETER_VOLT,
      expectedValue = "Pin 2 LM339 = 3.3V (Standby HIGH)",
      criticalSafetyWarning = "Tanpa resistor histeresis 10M, sinyal pulser akan mengalami derau pantulan (contact bounce) yang memicu timing pengapian liar!"
    ),
    WiringStep(
      id = "step_2_6",
      stageId = 2,
      stageTitle = "Tahap 2: Rangkaian Interlock, Sensor & Proteksi Sinyal",
      stepNumber = "2.6",
      title = "Pemasangan Driver Kipas Radiator (PB5 ke J1.7 via Modul Relay 5V / BC547)",
      board = PcbBoard.PCB_LOGIC,
      sourcePin = "WeAct H_TOP.7 (PB5)",
      targetPin = "Harness J1.7 (Biru-Kuning FAN_RELAY)",
      components = listOf(
        StepComponent("MOD_RELAY", "Modul Relay 1-Channel 5V + Optocoupler", "Modul Siap Pakai Pasaran (Drop-In)", "Pin IN ke PB5, VCC ke 5V WeAct, GND ke GND_STAR. Terminal relay NO/COM ke J1.7 & GND"),
        StepComponent("QFAN_DISKRIT", "Opsi Diskrit: Transistor BC547B", "BC547B NPN TO-92 + R 4.7k/10k", "Collector ke J1.7, Emitter ke GND_STAR, Base dari PB5 via 4.7k")
      ),
      schematicTrace = "H_TOP.7 (PB5) ===> Pin IN [Modul Relay 1-CH 5V] | Terminal COM -> GND_STAR | Terminal NO -> J1.7 (Biru-Kuning FAN_RELAY)",
      perfboardTips = listOf(
        "REKOMENDASI MODULAR: Gunakan 'Modul Relay 1-Channel 5V dengan Optocoupler' (Rp 9.000 - Rp 15.000) untuk menggantikan transistor BC547 diskrit.",
        "Kelebihan modul: Pin PB5 WeAct langsung dicolok ke pin IN modul relay tanpa perlu resistor basis diskrit!",
        "Modul relay sudah memiliki optocoupler dan flyback diode bawaan pabrik yang memproteksi STM32 100% dari lonjakan arus induksi motor kipas.",
        "Opsi Diskrit: Jika tetap memakai transistor BC547, hubungkan kaki Collector ke J1.7, Emitter ke GND_STAR, dan Basis ke PB5 via resistor 4.7k."
      ),
      pinLegGuide = "Modul Relay 5V: Pin IN (PB5 WeAct), VCC (5V WeAct), GND (GND_STAR). Sisi output beban: Terminal COM ke Ground aki, Terminal NO ke kabel J1.7 (Biru-Kuning).",
      verificationRequirement = "Beri sinyal uji PB5 aktif (HIGH 3.3V) via aplikasi menu Setup Kipas: Relay harus berbunyi 'klik' dan indikator LED pada modul relay menyala terang.",
      verificationType = VerificationType.MULTIMETER_CONTINUITY,
      expectedValue = "Relay Aktif: Menghubungkan J1.7 ke Ground (Kipas Berputar)",
      criticalSafetyWarning = "Pastikan motor kipas memiliki suplai +12V dari kunci kontak. Modul relay hanya menyambungkan ground (low-side sink) untuk keselamatan."
    ),
    WiringStep(
      id = "step_2_7",
      stageId = 2,
      stageTitle = "Tahap 2: Rangkaian Interlock, Sensor & Proteksi Sinyal",
      stepNumber = "2.7",
      title = "Penyolderan Driver LED Strobo Timing TDC (PB9 ke MOSFET FQP30N06L)",
      board = PcbBoard.PCB_LOGIC,
      sourcePin = "WeAct H_BOTTOM.6 (PB9)",
      targetPin = "Konektor Modul LED Strobo TDC 5V",
      components = listOf(
        StepComponent("Q_STROBE", "Logic-Level N-MOSFET", "FQP30N06L / IRLZ44N TO-220", "Pin 1 Gate, Pin 2 Drain, Pin 3 Source"),
        StepComponent("R_GATE_STR", "Resistor Seri Gate", "100R Ohm 0.25W", "Dari PB9 ke Gate MOSFET"),
        StepComponent("R_PD_STR", "Resistor Pulldown Gate", "10k Ohm 0.25W", "Dari Gate ke GND_STAR"),
        StepComponent("LED_STROBE", "Modul LED Putih 5V 1W", "LED Super Bright dengan resistor bawaan", "Kutub (+) ke +5V_LOGIC, Kutub (-) ke Drain MOSFET")
      ),
      schematicTrace = "H_BOTTOM.6 (PB9) -> 100R -> Gate Q_STROBE -> [10k ke GND] | Source -> GND_STAR | Drain -> LED_STROBE (-)",
      perfboardTips = listOf(
        "Fungsi strobo ini sangat krusial saat prosedur Quick Setup untuk menyelaraskan tanda 'T' pada kruk as.",
        "Solder MOSFET FQP30N06L dengan plat tab menghadap ke belakang.",
        "Hubungkan kaki Source (Pin 3) ke GND_STAR, dan kaki Drain (Pin 2) ke terminal negatif soket LED strobo."
      ),
      pinLegGuide = "FQP30N06L TO-220 (sisi tulisan): Pin 1 (kiri) = Gate, Pin 2 (tengah/tab) = Drain, Pin 3 (kanan) = Source.",
      verificationRequirement = "Beri sinyal uji logika 3.3V ke Gate via kabel jumper: LED strobo harus menyala terang seketika. Lepas sinyal: LED harus mati total.",
      verificationType = VerificationType.VISUAL_INSPECTION,
      expectedValue = "LED Menyala Responsif saat Gate HIGH",
      criticalSafetyWarning = "Driver strobo hanya aktif pada tahap Quick Setup TDC. Matikan kill switch dan buka SW_SERVICE/cabut FHV sebelum bekerja dekat magnet spul."
    ),
    WiringStep(
      id = "step_2_8",
      stageId = 2,
      stageTitle = "Tahap 2: Rangkaian Interlock, Sensor & Proteksi Sinyal",
      stepNumber = "2.8",
      title = "Pemasangan Header J_AUDIO_CTL (Persiapan Ekspansi Audio)",
      board = PcbBoard.PCB_LOGIC,
      sourcePin = "VIN_PROT (Setelah DREV) & GND_STAR",
      targetPin = "Header Terkunci J_AUDIO_CTL 2-Pin di PCB Logic",
      components = listOf(
        StepComponent("J_AUDIO_CTL", "Header Terkunci 2-Pin", "Header Molex / JST-XH 2.54mm 2-pin", "Pin 1 = IGN_12V, Pin 2 = GND_CTL")
      ),
      schematicTrace = "VIN_PROT -> J_AUDIO_CTL.1 (IGN_12V Signal) | GND_STAR -> J_AUDIO_CTL.2 (GND_CTL Reference)",
      perfboardTips = listOf(
        "Pasang header 2-pin ini sekarang pada PCB Logic meskipun Anda belum memasang board audio.",
        "Pin 1 mengambil sinyal 12V dari simpul VIN_PROT (setelah dioda proteksi SB560, sebelum choke).",
        "Sinyal ini hanya membawa arus perintah ON/OFF berarus sangat kecil (kurang dari 2mA) ke transistor board audio.",
        "DILARANG MENGAMBIL DAYA AMPLIFIER DARI HEADER INI!"
      ),
      pinLegGuide = "Konektor 2-pin: Pin 1 = Sinyal Kontak 12V (IGN_12V), Pin 2 = Ground Kontrol (GND_CTL).",
      verificationRequirement = "Ukur tegangan pada J_AUDIO_CTL Pin 1 saat kontak ON: harus terukur tegangan aki (~12V). Saat kontak OFF = 0V.",
      verificationType = VerificationType.MULTIMETER_VOLT,
      expectedValue = "Kontak ON: ~12V | Kontak OFF: 0V",
      criticalSafetyWarning = "Daya modul audio PAM8610 wajib disuplai langsung dari terminal aki melalui fuse 5A tersendiri, bukan dari jalur CDI!"
    ),

    // TAHAP 3: PCB POWER - CHARGER PUSH-PULL & HARDWARE FAULT
    WiringStep(
      id = "step_3_1",
      stageId = 3,
      stageTitle = "Tahap 3: Pemutus Fisik & Rangkaian Charger Push-Pull",
      stepNumber = "3.1",
      title = "Pemasangan Saklar Servis Daya & Sikring FHV 3A (PCB Power)",
      board = PcbBoard.PCB_POWER,
      sourcePin = "VIN_FILT dari PCB Logic",
      targetPin = "Rel Daya VIN_HV & Sikring FHV 3A",
      components = listOf(
        StepComponent("FHV", "Sikring Jalur Charger HV", "Fuse Blade 3A + Holder", "Antara kabel suplai dari VIN_FILT dan rel VIN_HV"),
        StepComponent("SW_SERVICE", "Saklar Pemutus Fisik Servis (Opsional)", "Saklar toggle berarus cukup / link servis", "Pemutus daya mekanis saat pemeliharaan; bukan input firmware"),
        StepComponent("KABEL_ANTAR_BOARD", "Kabel Penghubung Daya 18 AWG", "Kabel serabut silikon fleksibel 18 AWG", "Menghubungkan VIN_FILT PCB Logic ke FHV PCB Power")
      ),
      schematicTrace = "VIN_FILT (PCB Logic) ===> FHV 3A ===> [Saklar Servis / Link] ===> VIN_HV (Rel Suplai Trafo & TC4427)",
      perfboardTips = listOf(
        "Mulai pengerjaan pada PCB lubang kedua (PCB Power ukuran minimal 5x7 cm).",
        "Buat jarak aman (clearance) minimal 6mm antara area tegangan rendah 12V dan area tegangan tinggi 300V!",
        "Firmware R8 tidak membaca JP_HV. SW_SERVICE di sini murni isolator mekanis opsional; keselamatan operasi tetap ditentukan mode, RPM, HV feedback, fault, dan output permission firmware.",
        "Saklar pemutus fisik tetap sangat dianjurkan sebagai isolator daya mekanis saat perakitan atau penyolderan.",
        "Sikring FHV 3A melindungi aki dari bahaya arus berlebih jika terjadi hubung singkat pada push-pull MOSFET."
      ),
      pinLegGuide = "Kabel suplai masuk ke sikring FHV 3A, lalu keluar menuju rel daya charger VIN_HV.",
      verificationRequirement = "Pastikan koneksi daya dari VIN_FILT ke rel VIN_HV terhubung kokoh melalui sekring 3A. Saat kontak motor menyala, tegangan pada rel VIN_HV harus sama dengan tegangan aki.",
      verificationType = VerificationType.MULTIMETER_VOLT,
      expectedValue = "Rel VIN_HV = ~12V saat kontak aktif",
      criticalSafetyWarning = "Selalu cabut sekring FHV atau matikan kontak motor saat menyolder atau menyetel sensor di area daya tinggi!"
    ),
    WiringStep(
      id = "step_3_2",
      stageId = 3,
      stageTitle = "Tahap 3: Pemutus Fisik & Rangkaian Charger Push-Pull",
      stepNumber = "3.2",
      title = "Pembuatan Titik Uji Tegangan Rendah VIN_HV (Opsional)",
      board = PcbBoard.PCB_LOGIC,
      sourcePin = "Rel VIN_HV (PCB Power)",
      targetPin = "Test pad HV_PRESENT untuk multimeter (tidak ke MCU)",
      components = listOf(
        StepComponent("R_HV_PRES1", "Resistor Pembagi Atas", "100k Ohm 0.25W", "Dari VIN_HV ke simpul HV_PRESENT"),
        StepComponent("R_HV_PRES2", "Resistor Pembagi Bawah", "27k Ohm 0.25W", "Dari HV_PRESENT ke GND_STAR")
      ),
      schematicTrace = "VIN_HV -> 100k -> HV_PRESENT (TEST PAD) -> 27k -> GND_STAR",
      perfboardTips = listOf(
        "Rangkaian ini hanya menyediakan titik ukur aman bagi multimeter saat servis; firmware R8 tidak membaca PB2 maupun input VIN_HV terpisah.",
        "Solder komponen ini di PCB Logic bila titik servis dibutuhkan; tarik satu kabel sensor dari rel VIN_HV di PCB Power.",
        "Nilai pembagi 100k dan 27k menghasilkan tegangan ~2.55V saat aki 12V.",
        "Jangan memakai titik ini sebagai syarat interlock. Firmware memakai VBAT, HV_CENTER/HV_SIDE, PA10/GPIO14 fault, mode, dan RPM."
      ),
      pinLegGuide = "Beri label test pad HV_PRESENT dan jangan menghubungkannya ke pin MCU.",
      verificationRequirement = "Ukur test pad HV_PRESENT: saat rel VIN_HV 12V, hasil pembagi harus sekitar 2.5V.",
      verificationType = VerificationType.MULTIMETER_VOLT,
      expectedValue = "Test pad HV_PRESENT: ~2.5V saat VIN_HV 12V",
      criticalSafetyWarning = "Test pad ini bukan input keselamatan firmware dan tidak boleh menggantikan feedback HV PA6/PA7 atau GPIO35/GPIO32."
    ),
    WiringStep(
      id = "step_3_3",
      stageId = 3,
      stageTitle = "Tahap 3: Pemutus Fisik & Rangkaian Charger Push-Pull",
      stepNumber = "3.3",
      title = "Penelusuran Pin Trafo ATX T1 & Pemasangan di PCB Power",
      board = PcbBoard.PCB_POWER,
      sourcePin = "Rel VIN_HV & Drain MOSFET",
      targetPin = "Kaki Lilitan Trafo: LV_CT, LV_A, LV_B, HV_AC1, HV_AC2",
      components = listOf(
        StepComponent("T1", "Trafo Utama ATX PC Bekas", "EE-35 / EI-33 Utuh Tanpa Dililit Ulang", "Dilepas utuh dari power supply komputer")
      ),
      schematicTrace = "VIN_HV ===> LV_CT (Center Tap 5V Lama) | LV_A ===> Drain QHV1 | LV_B ===> Drain QHV2 | HV_AC1 & HV_AC2 ===> Sekunder AC Jembatan Dioda",
      perfboardTips = listOf(
        "Ambil trafo utama terbesar dari PSU komputer bekas. DILARANG MEMBONGKAR INTI FERRITE ATAU MEMBUKA LILITAN!",
        "Lacak di PCB bekas PSU: tiga kaki keluaran 5V yang dahulu menuju sepasang dioda schottky adalah: LV_A, LV_CT (tengah), dan LV_B.",
        "Dua kaki di sisi berlawanan yang dahulu menuju transistor switching tegangan tinggi adalah HV_AC1 dan HV_AC2.",
        "Kaki lilitan 3.3V dan 12V lainnya dipotong pendek dan dibungkus lem bakar / heat-shrink agar tidak korslet.",
        "Solder kaki trafo pada PCB lubang 5x7cm secara kokoh."
      ),
      pinLegGuide = "Nomor kaki trafo berbeda tiap pabrik! Gunakan nama fungsi jalur (LV_CT, LV_A, LV_B, HV_AC1, HV_AC2) hasil pelacakan jalur PCB donor.",
      verificationRequirement = "Ukur dengan multimeter ohm resistansi rendah: antara LV_CT dan LV_A harus sangat rendah (< 0.1 Ohm). Antara LV_CT dan LV_B harus sama rendah (< 0.1 Ohm). Antara sisi LV dan HV_AC harus TERISOLASI TOTAL (> 10 MegaOhm).",
      verificationType = VerificationType.MULTIMETER_OHM,
      expectedValue = "Isolasi Primer ke Sekunder = Tak Terhingga (OL)",
      criticalSafetyWarning = "Jika ada kebocoran atau kontinuitas antara lilitan LV dan HV, jangan gunakan trafo tersebut! Resiko tegangan tinggi 300V menyeberang ke aki!"
    ),
    WiringStep(
      id = "step_3_4",
      stageId = 3,
      stageTitle = "Tahap 3: Pemutus Fisik & Rangkaian Charger Push-Pull",
      stepNumber = "3.4",
      title = "Penyolderan Driver Gate TC4427A (DIP-8) & Kapasitor Bypass",
      board = PcbBoard.PCB_POWER,
      sourcePin = "PA9 (H_BOTTOM.18) & PB8 (H_BOTTOM.7)",
      targetPin = "TC4427A Pin 2 (INA), Pin 4 (INB) -> Pin 7 (OUTA), Pin 5 (OUTB)",
      components = listOf(
        StepComponent("U4_SOCKET", "Soket IC DIP-8", "DIP-8 Socket 2.54mm", "Dudukan untuk TC4427A"),
        StepComponent("U4", "Dual Gate Driver IC", "TC4427A / TC4427CPA DIP-8", "Driver push-pull non-inverting kecepatan tinggi"),
        StepComponent("R_PWM_IN", "Resistor Pembatas PWM", "2x 1k Ohm 0.25W", "Dari PA9 ke INA, dan dari PB8 ke INB"),
        StepComponent("R_PWM_PD", "Resistor Pulldown PWM", "2x 10k Ohm 0.25W", "Dari INA ke GND, dan dari INB ke GND"),
        StepComponent("C_BYPASS_TC", "Kapasitor Bypass Arus Puncak", "100nF Keramik || 10uF Tantalum/Elko", "Solder langsung menempel di Pin 6 (VDD) dan Pin 3 (GND)")
      ),
      schematicTrace = "PA9 -> 1k -> TC4427.Pin2 (INA) -> Pin7 (OUTA) -> 10R -> Gate QHV1 | PB8 -> 1k -> TC4427.Pin4 (INB) -> Pin5 (OUTB) -> 10R -> Gate QHV2 | Pin6 -> VIN_HV | Pin3 -> GND_POWER",
      perfboardTips = listOf(
        "Pasang soket DIP-8 di samping trafo T1 dengan takik notch di atas.",
        "Pin 6 (VDD) dihubungkan langsung ke rel VIN_HV.",
        "Pin 3 (GND) dihubungkan ke GND_POWER.",
        "Sangat krusial: solder kapasitor 100nF paralel dengan 10uF sedekat mungkin (jarak < 5mm) antara Pin 6 dan Pin 3!",
        "Tarik dua kabel sinyal dari PCB Logic: dari H_BOTTOM.18 (PA9) ke resistor 1k INA, dan dari H_BOTTOM.7 (PB8) ke resistor 1k INB."
      ),
      pinLegGuide = "TC4427 DIP-8: Pin 2 = INA, Pin 3 = GND, Pin 4 = INB, Pin 5 = OUTB, Pin 6 = VDD (12V), Pin 7 = OUTA.",
      verificationRequirement = "Lepas IC TC4427 dari soket. Ukur tegangan pada Pin 6 soket: harus sama dengan VIN_HV (12V). Pin 3 harus nol Ohm ke GND_POWER. Pasang IC kembali.",
      verificationType = VerificationType.MULTIMETER_VOLT,
      expectedValue = "Pin 6 VDD = 12.0V | Pin 3 GND = 0.0V",
      criticalSafetyWarning = "JANGAN gunakan IC TC4426 (tipe inverting), karena saat start awal PWM off, kedua gerbang MOSFET akan aktif bersamaan dan langsung jebol!"
    ),
    WiringStep(
      id = "step_3_5",
      stageId = 3,
      stageTitle = "Tahap 3: Pemutus Fisik & Rangkaian Charger Push-Pull",
      stepNumber = "3.5",
      title = "Penyolderan MOSFET IRF3205 (QHV1 & QHV2) & Resistor Shunt Arus",
      board = PcbBoard.PCB_POWER,
      sourcePin = "Output TC4427 & Lilitan Trafo LV_A / LV_B",
      targetPin = "RSENSE (0.05 Ohm 5W) & GND_POWER",
      components = listOf(
        StepComponent("QHV1_QHV2", "Power MOSFET N-Channel", "2x IRF3205 TO-220 Asli", "Switching daya push-pull 100 kHz"),
        StepComponent("R_GATE", "Resistor Redam Dering Gate", "2x 10R Ohm 0.25W", "Dari OUTA ke Gate QHV1, dan OUTB ke Gate QHV2"),
        StepComponent("R_GS_PD", "Resistor Pulldown Gate-Source", "2x 10k Ohm 0.25W", "Langsung di antara pin Gate dan Source masing-masing MOSFET"),
        StepComponent("TVS_Q", "TVS Clamp Spike Drain-Source", "2x 1.5KE33A Unidirectional", "Katoda ke Drain, Anoda ke Source masing-masing MOSFET"),
        StepComponent("RSENSE", "Resistor Sensor Arus Primer", "0.05 Ohm 5W Non-Induktif Keramik", "Dari Source kedua MOSFET (ISENSE_TOP) ke GND_POWER")
      ),
      schematicTrace = "TC4427.Pin7 -> 10R -> Gate QHV1 | TC4427.Pin5 -> 10R -> Gate QHV2 | Drain QHV1 -> LV_A | Drain QHV2 -> LV_B | Source keduanya -> ISENSE_TOP -> RSENSE 0.05R/5W -> GND_POWER",
      perfboardTips = listOf(
        "Pasang kedua MOSFET IRF3205 dengan sirip pendingin kecil terpisah atau terisolasi mika.",
        "Solder resistor pulldown 10k langsung di kaki Gate (Pin 1) ke Source (Pin 3) pada bagian bawah PCB.",
        "Solder TVS 1.5KE33A langsung melintang dari kaki Drain (Pin 2) ke kaki Source (Pin 3).",
        "Satukan kaki Source QHV1 dan QHV2 ke simpul tembaga tebal berlabel ISENSE_TOP.",
        "Solder resistor semen 0.05 Ohm 5 Watt dari ISENSE_TOP ke jalur kawat tembaga tebal GND_POWER."
      ),
      pinLegGuide = "IRF3205 (tulisan depan): Pin 1 (kiri) = Gate, Pin 2 (tengah/tab) = Drain, Pin 3 (kanan) = Source.",
      verificationRequirement = "Ukur resistansi antara Drain dan Source saat gate di-discharge (sentuh gate ke source): resistansi harus mega-ohm (> 100k). Ukur resistansi RSENSE ke ground: harus terukur ~0.05 Ohm.",
      verificationType = VerificationType.MULTIMETER_OHM,
      expectedValue = "Drain-Source OFF > 100 kOhm | RSENSE ~ 0.05 Ohm",
      criticalSafetyWarning = "Pastikan heatsink kedua MOSFET tidak saling bersentuhan atau menyentuh bodi motor! Tab tengah adalah Drain yang berayun 24-30V saat switching!"
    ),
    WiringStep(
      id = "step_3_6",
      stageId = 3,
      stageTitle = "Tahap 3: Pemutus Fisik & Rangkaian Charger Push-Pull",
      stepNumber = "3.6",
      title = "Penyolderan Proteksi Arus Lebih & Hardware Fault Clamp (LM339)",
      board = PcbBoard.PCB_LOGIC,
      sourcePin = "Simpul ISENSE_TOP dari PCB Power",
      targetPin = "WeAct H_TOP.11 (PA10 / PWM_CLAMP active-low)",
      components = listOf(
        StepComponent("DIV_IREF", "Pembagi Referensi Batas Arus", "120k (ke 3V3) + 10k (ke GND)", "Menghasilkan tegangan referensi IREF ~0.25V pada LM339 Pin 7"),
        StepComponent("R_ISENSE_IN", "Resistor Filter Sinyal Arus", "100R Ohm 0.25W", "Dari simpul ISENSE_TOP ke LM339 Pin 6 (IN2-)"),
        StepComponent("DIV_VOV", "Pembagi Referensi Batas Tegangan HV", "120k (ke 5V) + 100k || 10nF (ke GND)", "Menghasilkan tegangan VOV ~2.27V pada LM339 Pin 9 & 11"),
        StepComponent("R_PU_CLAMP", "Resistor Pull-up Jalur Clamp", "4.7k Ohm ke 3V3", "Menarik jalur sinyal PWM_CLAMP ke status HIGH 3.3V"),
        StepComponent("DCL_A_B", "Dioda Fast Clamp Sinyal PWM", "2x 1N4148 Small Signal Diode", "Anoda ke jalur PWM_A & PWM_B, Katoda ke jalur PWM_CLAMP")
      ),
      schematicTrace = "LM339: Pin 1 (OUT2) + Pin 13 (OUT4) + Pin 14 (OUT3) digabung ke simpul PWM_CLAMP -> pullup 4.7k ke 3V3 -> H_TOP.11 (PA10) | PWM_A -> 1N4148 -> PWM_CLAMP | PWM_B -> 1N4148 -> PWM_CLAMP",
      perfboardTips = listOf(
        "Rangkaian ini adalah pengaman hardware murni independen tanpa campur tangan software.",
        "Jika arus trafo melebihi 5A (0.05R x 5A = 0.25V) atau tegangan HV melebihi 300V, komparator LM339 langsung menarik jalur PWM_CLAMP ke Ground.",
        "Dioda 1N4148 langsung menguras sinyal PWM ke ground sehingga MOSFET padam dalam hitungan ratusan nanodetik.",
        "Pin H_TOP.11 (PA10) mendeteksi kondisi ini dan firmware memunculkan notifikasi FAULT."
      ),
      pinLegGuide = "1N4148: Dioda kaca oranye kecil, gelang hitam adalah Katoda (disambung ke simpul PWM_CLAMP).",
      verificationRequirement = "Ukur tegangan pada simpul PWM_CLAMP saat kondisi normal standby: harus terbaca tegangan HIGH stabil 3.28V - 3.30V.",
      verificationType = VerificationType.MULTIMETER_VOLT,
      expectedValue = "Standby Normal = 3.30V (HIGH)",
      criticalSafetyWarning = "Jangan pernah melepas dioda clamp 1N4148 atau menonaktifkan PA10! Rangkaian ini mencegah trafo meledak jika terjadi korsleting sekunder!"
    ),

    // TAHAP 4: PENYEARAH & DUA BANK TEGANGAN TINGGI
    WiringStep(
      id = "step_4_1",
      stageId = 4,
      stageTitle = "Tahap 4: Penyearah Jembatan Ultrafast & Dua Bank HV",
      stepNumber = "4.1",
      title = "Penyolderan Jembatan Dioda Ultrafast DREC1-DREC4 (PCB Power)",
      board = PcbBoard.PCB_POWER,
      sourcePin = "Sekunder Trafo HV_AC1 & HV_AC2",
      targetPin = "Jalur BRIDGE_PLUS & GND_POWER",
      components = listOf(
        StepComponent("DREC1_4", "Dioda Penyearah Cepat Tegangan Tinggi", "4x UF4007 1A 1000V Ultrafast", "Membentuk sirkuit jembatan gelombang penuh")
      ),
      schematicTrace = "DREC1: Anoda HV_AC1, Katoda BRIDGE_PLUS | DREC2: Anoda HV_AC2, Katoda BRIDGE_PLUS | DREC3: Anoda GND_POWER, Katoda HV_AC1 | DREC4: Anoda GND_POWER, Katoda HV_AC2",
      perfboardTips = listOf(
        "PENTING: Gunakan UF4007 (Ultra-Fast 75ns), DILARANG menggunakan 1N4007 biasa (karena 1N4007 lambat 50Hz dan akan sangat panas/rusak pada switching 100kHz!).",
        "Susun 4 dioda membentuk jembatan penyearah (Full Bridge Rectifier).",
        "Beri jarak celah minimal 6mm dari komponen tegangan rendah.",
        "Katoda DREC1 dan DREC2 disatukan menjadi rel BRIDGE_PLUS.",
        "Anoda DREC3 dan DREC4 disatukan ke rel kawat tembaga GND_POWER."
      ),
      pinLegGuide = "UF4007 DO-41: Gelang putih perak adalah Katoda.",
      verificationRequirement = "Uji dioda jembatan dengan mode Diode Multitester: pastikan drop tegangan forward ~0.55V pada tiap dioda dan resistansi balik Open-Loop (OL). Pastikan BRIDGE_PLUS tidak korslet ke ground.",
      verificationType = VerificationType.MULTIMETER_CONTINUITY,
      expectedValue = "Drop Maju ~ 0.55V | Balik OL",
      criticalSafetyWarning = "Tegangan pada simpul BRIDGE_PLUS dapat mencapai 220V–345V DC dan mematikan. Jaga jarak aman minimal 6mm."
    ),
    WiringStep(
      id = "step_4_2",
      stageId = 4,
      stageTitle = "Tahap 4: Penyearah Jembatan Ultrafast & Dua Bank HV",
      stepNumber = "4.2",
      title = "Pemasangan Dioda Pemisah Dua Bank Pengapian (DCH_C & DCH_S)",
      board = PcbBoard.PCB_POWER,
      sourcePin = "Rel BRIDGE_PLUS",
      targetPin = "Rel HV_CENTER & Rel HV_SIDE",
      components = listOf(
        StepComponent("DCH_C", "Dioda Isolasi Bank Center", "UF4007 1A 1000V Ultrafast", "Anoda ke BRIDGE_PLUS, Katoda ke HV_CENTER"),
        StepComponent("DCH_S", "Dioda Isolasi Bank Side", "UF4007 1A 1000V Ultrafast", "Anoda ke BRIDGE_PLUS, Katoda ke HV_SIDE")
      ),
      schematicTrace = "BRIDGE_PLUS ===> [DCH_C Anoda -> Katoda] ===> HV_CENTER (Bank Koil Tengah) | BRIDGE_PLUS ===> [DCH_S Anoda -> Katoda] ===> HV_SIDE (Bank Koil Samping)",
      perfboardTips = listOf(
        "Dua dioda ini memisahkan reservoir muatan Bank Center dan Bank Side.",
        "Saat busi tengah meletup, muatan Bank Side tidak ikut tersedot karena tertahan oleh DCH_S.",
        "Solder kedua dioda UF4007: kedua Anoda digabung ke BRIDGE_PLUS, Katoda DCH_C ke jalur HV_CENTER, Katoda DCH_S ke jalur HV_SIDE."
      ),
      pinLegGuide = "Gelang katoda dioda menghadap ke arah bank kapasitor (HV_CENTER dan HV_SIDE).",
      verificationRequirement = "Ukur resistansi antara HV_CENTER dan HV_SIDE: harus tidak terhubung langsung (terbaca Open Loop OL). Arus hanya bisa mengalir dari BRIDGE_PLUS ke masing-masing bank.",
      verificationType = VerificationType.MULTIMETER_CONTINUITY,
      expectedValue = "Kedua Bank Terisolasi (OL)",
      criticalSafetyWarning = "Kedua bank harus terpisah sempurna agar pengapian multi-busi Pulsar NS200 dapat menyala dengan timing offset yang presisi!"
    ),
    WiringStep(
      id = "step_4_3",
      stageId = 4,
      stageTitle = "Tahap 4: Penyearah Jembatan Ultrafast & Dua Bank HV",
      stepNumber = "4.3",
      title = "Penyolderan Pembagi Tegangan Umpan Balik HV (PA6 & PA7)",
      board = PcbBoard.PCB_POWER,
      sourcePin = "Rel HV_CENTER & Rel HV_SIDE",
      targetPin = "WeAct H_BOTTOM.15 (PA6) & H_BOTTOM.16 (PA7)",
      components = listOf(
        StepComponent("R_HV_DIV_C", "Rantai Resistor Tegangan Tinggi Center", "4x 270k Ohm 1% 0.25W Seri (Total 1.08 M)", "Dari HV_CENTER ke simpul HV_C_FB"),
        StepComponent("R_HV_DIV_S", "Rantai Resistor Tegangan Tinggi Side", "4x 270k Ohm 1% 0.25W Seri (Total 1.08 M)", "Dari HV_SIDE ke simpul HV_S_FB"),
        StepComponent("R_FB_LOW", "Resistor Pembagi Bawah Presisi", "2x 8.2k Ohm 1% 0.25W", "Masing-masing dari FB ke GND_POWER"),
        StepComponent("C_FB_NOISE", "Kapasitor Filter Tegangan Feedback", "2x 10nF Keramik 50V", "Paralel dengan masing-masing resistor 8.2k"),
        StepComponent("R_FB_PROT", "Resistor Pembatas Arus Pin MCU", "2x 1k Ohm 0.25W", "Menuju pin PA6 dan PA7"),
        StepComponent("DBAT_HV", "Dioda Pengaman Tegangan Clamp", "2x BAT54S Schottky Dual", "Melindungi input ADC PA6 dan PA7")
      ),
      schematicTrace = "HV_CENTER -> [270k-270k-270k-270k seri] -> HV_C_FB -> 1k -> H_BOTTOM.15 (PA6) | HV_C_FB -> [8.2k || 10nF] -> GND | BAT54S Clamp. Jalur SIDE identik ke PA7.",
      perfboardTips = listOf(
        "MANDATORI: Mengapa harus 4 resistor 270k seri? Karena satu resistor kecil 0.25W hanya mampu menahan tegangan maksimal 150-200V. Memakai 4 resistor seri membagi beban tegangan ~70V per resistor secara aman tanpa resiko flashover loncatan bunga api!",
        "Susun 4 resistor 270k secara memanjang zig-zag rapi.",
        "Solder BAT54S clamp di simpul HV_C_FB dan HV_S_FB sebelum kabel menuju WeAct."
      ),
      pinLegGuide = "Resistor 270k (Merah-Ungu-Hitam-Oranye-Cokelat). Rasio pembagi: 8.2k / (1080k + 8.2k) = 1 : 132.7. Tegangan 285V menghasilkan ~2.15V di ADC.",
      verificationRequirement = "Ukur total resistansi dari HV_CENTER ke ground: harus terukur tepat ~1.088 MegaOhm (1.08M + 8.2k). Pastikan tidak ada jalur yang korslet ke ground.",
      verificationType = VerificationType.MULTIMETER_OHM,
      expectedValue = "~ 1.08 MegaOhm ± 2%",
      criticalSafetyWarning = "Jika resistor seri ini putus atau tidak terpasang, MCU tidak dapat membaca tegangan bank dan charger akan terus memompa tegangan melebihi batas aman 300V!"
    ),

    // TAHAP 5: DISCHARGE & KOIL NS200
    WiringStep(
      id = "step_5_1",
      stageId = 5,
      stageTitle = "Tahap 5: Rangkaian Pelepasan CDI & Sambungan Koil",
      stepNumber = "5.1",
      title = "Penyolderan Kapasitor Pulsa CDI C_CENTER & Resistor Bleeder (J1.12)",
      board = PcbBoard.PCB_POWER,
      sourcePin = "Rel HV_CENTER",
      targetPin = "Harness J1.12 (COIL_CENTER)",
      components = listOf(
        StepComponent("C_CENTER", "Kapasitor Buang Muatan CDI Utama", "1.0 uF 630V Polypropylene MKP/MPP", "Terminal A ke HV_CENTER, Terminal B ke J1.12"),
        StepComponent("R_BLEED_C", "Rantai Resistor Pembuang Muatan Otomatis", "4x 470k Ohm 0.5W Seri (Total 1.88 M)", "Disolder melintang langsung antara Terminal A dan Terminal B")
      ),
      schematicTrace = "HV_CENTER ===> Terminal A C_CENTER ===> [4x470k Seri Melintang] ===> Terminal B C_CENTER ===> Harness J1.12 (Koil Busi Tengah)",
      perfboardTips = listOf(
        "Pasang kapasitor kotak MKP 1uF 630V dengan kokoh.",
        "Solder rantai 4 resistor 470k 0.5W seri langsung di bawah kaki kapasitor melintang dari Terminal A ke Terminal B.",
        "Fungsi bleeder ini: membuang sisa muatan tegangan tinggi di kapasitor dalam waktu beberapa detik setelah kunci kontak dimatikan.",
        "Hubungkan Terminal B ke kabel pigtail harness J1.12 menggunakan kabel tebal berisolasi 600V."
      ),
      pinLegGuide = "Kapasitor MKP non-polar. J1.12 adalah pin nomor 12 di soket pigtail (sudut kanan bawah soket).",
      verificationRequirement = "Ukur resistansi melintang kaki Terminal A dan Terminal B kapasitor: harus terukur ~1.88 MegaOhm (4x 470k seri).",
      verificationType = VerificationType.MULTIMETER_OHM,
      expectedValue = "~ 1.88 MegaOhm (Bleeder Terpasang)",
      criticalSafetyWarning = "DILARANG keras melepas resistor bleeder 4x470k! Tanpa resistor ini, kapasitor 630V akan tetap menyimpan muatan mematikan meskipun motor sudah dimatikan!"
    ),
    WiringStep(
      id = "step_5_2",
      stageId = 5,
      stageTitle = "Tahap 5: Rangkaian Pelepasan CDI & Sambungan Koil",
      stepNumber = "5.2",
      title = "Penyolderan Kapasitor Pulsa CDI C_SIDE & Resistor Bleeder (J1.6)",
      board = PcbBoard.PCB_POWER,
      sourcePin = "Rel HV_SIDE",
      targetPin = "Harness J1.6 (Hitam-Merah COIL_SIDE)",
      components = listOf(
        StepComponent("C_SIDE", "Kapasitor Buang Muatan CDI Samping", "1.0 uF 630V Polypropylene MKP/MPP", "Terminal A ke HV_SIDE, Terminal B ke J1.6"),
        StepComponent("R_BLEED_S", "Rantai Resistor Pembuang Muatan Otomatis", "4x 470k Ohm 0.5W Seri (Total 1.88 M)", "Disolder melintang langsung antara Terminal A dan Terminal B")
      ),
      schematicTrace = "HV_SIDE ===> Terminal A C_SIDE ===> [4x470k Seri Melintang] ===> Terminal B C_SIDE ===> Harness J1.6 (Koil Busi Samping Kiri-Kanan)",
      perfboardTips = listOf(
        "Rangkaian ini identik dengan Bank Center.",
        "Hubungkan Terminal B C_SIDE ke kabel pigtail harness J1.6 (Hitam-Merah).",
        "Jalur kabel keluaran koil harus dijauhkan minimal 20mm dari kabel sensor pulser J1.10 dan antena WeAct!"
      ),
      pinLegGuide = "J1.6 adalah pin nomor 6 di soket pigtail harness (sudut kanan atas soket).",
      verificationRequirement = "Ukur resistansi melintang Terminal A dan B C_SIDE: harus terukur ~1.88 MegaOhm.",
      verificationType = VerificationType.MULTIMETER_OHM,
      expectedValue = "~ 1.88 MegaOhm (Bleeder Terpasang)",
      criticalSafetyWarning = "Jauhkan kabel keluaran koil J1.6 & J1.12 dari antena WeAct! Sengatan tegangan tinggi dapat merusak modul BLE!"
    ),
    WiringStep(
      id = "step_5_3",
      stageId = 5,
      stageTitle = "Tahap 5: Rangkaian Pelepasan CDI & Sambungan Koil",
      stepNumber = "5.3",
      title = "Pemasangan Thyristor SCR BT151-600R (SCR1 Center & SCR2 Side)",
      board = PcbBoard.PCB_POWER,
      sourcePin = "Rel HV_CENTER & HV_SIDE",
      targetPin = "Rel Kawat Tembaga GND_POWER",
      components = listOf(
        StepComponent("SCR1_SCR2", "Silicon Controlled Rectifier (SCR)", "2x BT151-600R TO-220", "SCR1 untuk Bank Center, SCR2 untuk Bank Side"),
        StepComponent("R_GK_PD", "Resistor Pulldown Gate-Cathode", "2x 1k Ohm 0.25W", "Disolder langsung antara Pin 3 (Gate) dan Pin 1 (Cathode)")
      ),
      schematicTrace = "SCR1: Pin 2 (Anode+tab) ke HV_CENTER, Pin 1 (Cathode) ke GND_POWER, Pin 3 (Gate) via 330R ke driver | SCR2: Pin 2 ke HV_SIDE, Pin 1 ke GND_POWER",
      perfboardTips = listOf(
        "Pasang kedua SCR BT151-600R dengan plat pendingin tab menghadap ke belakang.",
        "PERHATIAN: Tab logam BT151 terhubung internal dengan Pin 2 (Anode bertegangan tinggi 285V)! Jangan sampai tab menempel ke ground atau heatsink tanpa mika isolator!",
        "Solder kaki Pin 1 (Cathode) langsung ke rel tembaga tebal GND_POWER.",
        "Solder resistor 1k langsung di antara kaki Gate (Pin 3) dan Cathode (Pin 1) untuk mencegah pemicuan palsu akibat derau."
      ),
      pinLegGuide = "BT151-600R (sisi tulisan): Pin 1 (kiri) = Cathode (ke GND), Pin 2 (tengah/tab) = Anode (ke HV), Pin 3 (kanan) = Gate.",
      verificationRequirement = "Ukur kontinuitas Pin 1 (Cathode) ke GND_POWER: harus < 0.1 Ohm. Ukur Anode ke Cathode: harus Open Loop (tidak boleh korslet).",
      verificationType = VerificationType.MULTIMETER_CONTINUITY,
      expectedValue = "Cathode ke GND = 0 Ohm | Anode ke Cathode = OL",
      criticalSafetyWarning = "Jika SCR korslet Anode ke Cathode, charger tidak akan bisa mengisi tegangan dan resistor trafo akan panas berlebih."
    ),
    WiringStep(
      id = "step_5_4",
      stageId = 5,
      stageTitle = "Tahap 5: Rangkaian Pelepasan CDI & Sambungan Koil",
      stepNumber = "5.4",
      title = "Penyolderan Rangkaian Driver Gate Pemicu SCR (QNC/QPC & QNS/QPS)",
      board = PcbBoard.PCB_POWER,
      sourcePin = "WeAct H_BOTTOM.10 (PA1 Center) & H_BOTTOM.11 (PA2 Side)",
      targetPin = "Gate SCR1 Pin 3 & Gate SCR2 Pin 3",
      components = listOf(
        StepComponent("QNC_QNS", "Transistor Penguat Pulsa NPN", "2x BC547B TO-92", "Menerima sinyal pulsa 3.3V dari PA1 dan PA2"),
        StepComponent("QPC_QPS", "Transistor Saklar Pemicu Tegangan PNP", "2x BC557B TO-92", "Menyuntikkan pulsa arus pemicu ke Gate SCR"),
        StepComponent("R_BASE_N", "Resistor Basis NPN", "2x 4.7k Ohm 0.25W + 10k pulldown", "Dari PA1 ke Basis QNC, dan PA2 ke Basis QNS"),
        StepComponent("R_COLL_N", "Resistor Kolektor NPN ke Basis PNP", "2x 2.2k Ohm 0.25W", "Menghubungkan Kolektor BC547 ke Basis BC557"),
        StepComponent("R_PULLUP_P", "Resistor Pullup Basis PNP", "2x 10k Ohm 0.25W", "Dari Basis BC557 ke VIN_HV"),
        StepComponent("R_GATE_SCR", "Resistor Pembatas Arus Gate", "2x 330R Ohm 0.5W", "Dari Kolektor BC557 ke Pin 3 Gate SCR")
      ),
      schematicTrace = "PA1 -> 4.7k -> Basis QNC (BC547) -> Kolektor -> 2.2k -> Basis QPC (BC557) -> Kolektor -> 330R -> Gate SCR1 (Center). Rangkaian PA2 identik ke SCR2 (Side).",
      perfboardTips = listOf(
        "Rangkaian driver transistor komplementer NPN-PNP ini bertindak sebagai penembak arus gerbang super cepat (fast rise-time gate pulse).",
        "Emitter BC557 terhubung ke rel VIN_HV (12V).",
        "Kolektor BC557 mengalirkan pulsa arus tajam ~30mA melalui resistor 330R ke Gate SCR untuk memastikan pelepasan muatan terjadi dalam waktu < 1 mikrodetik."
      ),
      pinLegGuide = "BC547 & BC557 (sisi tulisan): Pin 1 (kiri) = Collector, Pin 2 (tengah) = Base, Pin 3 (kanan) = Emitter.",
      verificationRequirement = "Ukur tegangan standby pada Gate SCR (Pin 3) saat motor mati: harus mutlak 0.00V (Gate tidak boleh bocor tegangan saat diam).",
      verificationType = VerificationType.MULTIMETER_VOLT,
      expectedValue = "Standby Gate = 0.00V",
      criticalSafetyWarning = "Jika Gate mendapat tegangan bocor di atas 0.7V saat standby, SCR akan memicu terus-menerus dan sistem CDI terkunci (latch up)!"
    ),

    // TAHAP 6: VERIFIKASI SEBELUM START & QUICK SETUP
    WiringStep(
      id = "step_6_1",
      stageId = 6,
      stageTitle = "Tahap 6: Prosedur Verifikasi Lengkap & Start Motor",
      stepNumber = "6.1",
      title = "Pemeriksaan Mandatori Multimeter Sebelum Colok ke Motor",
      board = PcbBoard.PCB_LOGIC,
      sourcePin = "Seluruh Pin Soket J1 & Rel PCB",
      targetPin = "Multimeter Digital Teknisi",
      components = listOf(
        StepComponent("CHECK_HARNESS", "Pigtail Soket J1 NS200", "Soket 12-pin terpasang rapi", "Verifikasi tidak ada pin tertukar"),
        StepComponent("CHECK_CLEARANCE", "Jarak Celah Tegangan Tinggi", "PCB Power 5x7cm", "Clearance HV >= 6mm bebas serpihan timah"),
        StepComponent("CHECK_ANTENNA", "Area Bebas Logam Antena WeAct", "Ujung kanan board WeAct", "Tidak ada kabel HV, trafo, atau tembaga di radius 15mm")
      ),
      schematicTrace = "Checklist Keselamatan Akhir: 1) Semua GND kembali ke GND_STAR. 2) Tidak ada kontinuitas 12V/HV ke pin ADC. 3) PA1/PA2 atau GPIO25/GPIO26 LOW saat reset. 4) Cabut FHV/buka SW_SERVICE untuk memutus 100% VIN_HV saat servis.",
      perfboardTips = listOf(
        "Bersihkan sisa serpihan timah dan sisa pasta flux di bawah PCB menggunakan sikat gigi dan alkohol isopropil (IPA).",
        "Pastikan sambungan solder mengkilap (tidak ada cold joint / retak).",
        "Bungkus bagian bawah PCB dengan pelat isolator plastik mika sebelum dimasukkan ke dalam kotak CDI.",
        "Gunakan adaptor pigtail soket 12-pin, JANGAN PERNAH memotong kabel harness asli motor NS200!"
      ),
      pinLegGuide = "Gunakan checklist pada tab 'Verifikasi' untuk mencatat nilai ukur sebelum lanjut ke start mesin.",
      verificationRequirement = "Lakukan tes kontinuitas antara jalur HV 285V dan ground: resistansi harus > 1 MOhm (dari resistor feedback). Pastikan tidak ada korsleting langsung 0 Ohm di titik mana pun.",
      verificationType = VerificationType.MULTIMETER_OHM,
      expectedValue = "Semua Jalur Lulus Uji Isolasi",
      criticalSafetyWarning = "STOP SEGERA jika tercium bau terbakar, percikan api, atau komponen panas menyengat saat pertama kali kontak dihidupkan!"
    )
  )

  val quickSetupSteps: List<QuickSetupStep> = listOf(
    QuickSetupStep(
      stepNumber = 1,
      stageName = "1. Flash Firmware",
      connectionCondition = "Hubungkan kabel USB data. STM32: masuk DFU dengan BOOT0/NRST. ESP32: masuk bootloader dengan BOOT/EN bila auto-reset tidak bekerja.",
      appAction = "STM32: flash image R8 ke 0x08000000 dengan STM32CubeProgrammer. ESP32: flash image ESP-IDF sesuai partition table proyek.",
      outputCondition = "Penulisan dan verifikasi image target yang benar berhasil; perangkat reboot normal.",
      proceedCriteria = "BLE mengiklankan NS200-CDI dan menjawab GET,CAPS.",
      stopHazard = "Gagal koneksi: periksa kabel USB Type-C data (bukan kabel charger saja)."
    ),
    QuickSetupStep(
      stepNumber = 2,
      stageName = "2. Logic & Tegangan Catu",
      connectionCondition = "Kill switch OFF (J1.5 = 0V), kontak ON, dan jalur charger diisolasi dengan membuka SW_SERVICE atau mencabut FHV.",
      appAction = "Buka aplikasi Android, scan BLE 'NS200-CDI', lalu hubungkan GATT.",
      outputCondition = "Tegangan aki terbaca akal (11.8V - 12.8V); HV terbaca < 30V; Tahap aplikasi terbaca 'BARU'.",
      proceedCriteria = "Aki terbaca normal & BLE terhubung stabil.",
      stopHazard = "STOP jika VBAT tidak masuk akal atau HV > 30V saat jalur charger telah diisolasi."
    ),
    QuickSetupStep(
      stepNumber = 3,
      stageName = "3. Uji Pulser Pickup",
      connectionCondition = "J1.10 terhubung ke input pickup terisolasi/komparator; SW_SERVICE tetap terbuka atau FHV dicabut; putar starter 2–3 detik.",
      appAction = "Perhatikan indikator Pulser di aplikasi: deteksi Edge (FALLING/RISING), PPR default 1.",
      outputCondition = "Charger dan Koil tetap OFF; Quality sinyal pulser terbaca >= 10.",
      proceedCriteria = "Pickup Quality >= 10 terdeteksi tanpa noise.",
      stopHazard = "STOP jika pulsa hilang/ganda liar: balikkan jumper selector TPS atau periksa histeresis 10M."
    ),
    QuickSetupStep(
      stepNumber = 4,
      stageName = "4. Kalibrasi Sudut TDC Strobo",
      connectionCondition = "Hubungkan LED strobo 5V ke PB9 (STM32) atau GPIO27 (ESP32); arahkan cahaya ke tanda timing.",
      appAction = "Putar starter mesin; perhatikan garis tanda huruf 'T' pada kruk as motor.",
      outputCondition = "Garis 'T' terlihat diam sejajar tepat dengan garis penanda crankcase; tekan 'SAVE TDC'.",
      proceedCriteria = "Garis T sejajar sempurna dan tersimpan di flash.",
      stopHazard = "STOP jika tanda loncat-loncat: jangan lanjutkan ke start mesin."
    ),
    QuickSetupStep(
      stepNumber = 5,
      stageName = "5. Kalibrasi Sensor TPS",
      connectionCondition = "Mesin mati, kontak ON, SW_SERVICE terbuka atau FHV dicabut.",
      appAction = "Buka menu TPS di aplikasi: tekan 'Simpan Gas Tertutup' (0%), lalu pelintir gas penuh dan tekan 'Simpan Gas Penuh' (100%).",
      outputCondition = "Rentang ADC terbaca proporsional dan diterima firmware.",
      proceedCriteria = "Grafik bukaan gas bergerak mulus 0% - 100%.",
      stopHazard = "Jika tegangan diam tidak berubah saat gas dipelintir: ubah jumper J_TPS dari Posisi A ke Posisi B."
    ),
    QuickSetupStep(
      stepNumber = 6,
      stageName = "6. First Start Mesin (220V Center)",
      connectionCondition = "Pilih FIRST START di aplikasi, pasang kembali FHV/tutup SW_SERVICE, dan pastikan APD serta pemadam siap.",
      appAction = "Pencet tombol starter motor. Tegangan HV diatur konservatif 220V, hanya busi CENTER yang memicu, advance maksimal 10 derajat, limiter 3.000 RPM.",
      outputCondition = "Mesin hidup stabil idle minimal 3 detik tanpa gejala backfire atau detonasi.",
      proceedCriteria = "Mesin hidup idle stasioner stabil >= 3 detik.",
      stopHazard = "STOP SEGERA jika kickback/backfire, letupan knalpot, atau tegangan melonjak >= 300V!"
    ),
    QuickSetupStep(
      stepNumber = 7,
      stageName = "7. Konfirmasi READY & Simpan Map",
      connectionCondition = "Matikan kontak, buka SW_SERVICE/cabut FHV, dan pastikan kedua bank HV telah turun < 30V oleh bleeder.",
      appAction = "Tekan 'SIMPAN READY CENTER' di aplikasi Android.",
      outputCondition = "Boot berikutnya langsung mengaktifkan pengapian sesuai kurva timing map.",
      proceedCriteria = "Status tersimpan permanen di Flash MCU dengan CRC valid.",
      stopHazard = "Jangan sentuh PCB saat tegangan HV masih di atas 30V."
    ),
    QuickSetupStep(
      stepNumber = 8,
      stageName = "8. Tuning Lanjutan & Opsi 3-Busi",
      connectionCondition = "Operasi NORMAL (285V); aktifkan busi samping (SIDE J1.6) HANYA setelah offset derajat SIDE diukur dengan lampu strobo pada motor aktual.",
      appAction = "Pantau telemetri langsung: RPM, TPS, tegangan aki, HV Center/Side, fault, dan limiter. Suhu dan status fan harus tampil; lakukan kalibrasi NTC 3 titik sebelum test jalan.",
      outputCondition = "Beda tegangan kedua bank < 50V, tidak ada fault/reset, dan limiter bekerja sesuai slot.",
      proceedCriteria = "Seluruh parameter hijau stabil pada pengetesan jalan bertahap.",
      stopHazard = "NORMAL menargetkan 285V dan PRO menargetkan 345V; firmware memicu fault di atas 360V. Jangan pernah menonaktifkan feedback atau clamp HV."
    )
  )

  val bomItems: List<BomItem> = listOf(
    BomItem("b1", "LOGIC", "U1", "1", "WeAct STM32WB55CGU6", "Sudah dimiliki", "WAJIB satu-satunya MCU + BLE", true),
    BomItem("b2", "PROGRAM", "STLINK", "0/1", "ST-Link V2 compatible dengan SWD 3.3V", "Beli bila perlu", "OPSIONAL fallback; flash utama pakai USB DFU board", false),
    BomItem("b3", "LOGIC", "PCB_LOGIC", "1", "PCB lubang 7x9cm single-layer", "Sudah dimiliki", "WAJIB; khusus bagian logic/sensor", true),
    BomItem("b4", "POWER", "PCB_POWER", "1", "PCB lubang min 5x7cm (clearance >=6mm)", "Beli baru", "WAJIB terpisah dari antena board WeAct", false),
    BomItem("b5", "POWER", "T1", "1", "Trafo utama ATX lilitan 5V center-tap utuh", "PSU PC bekas", "WAJIB; tidak dibuka/tidak dililit ulang", false),
    BomItem("b6", "LOGIC", "U2", "1", "LM339N / KA339 DIP-14 5V", "PSU bekas / Beli baru", "WAJIB komparator pulser & fault", false),
    BomItem("b7", "LOGIC", "U_BUCK_LOGIC", "1", "Modul LM2596 adjustable (set 5.00V 1A)", "Beli baru", "WAJIB catu daya 5V WeAct", false),
    BomItem("b8", "POWER", "U4", "1", "TC4427A / TC4427CPA DIP-8 (4.5-18V)", "Beli baru", "WAJIB non-inverting driver gate", false),
    BomItem("b9", "POWER", "QHV1-QHV2", "2", "IRF3205 55V TO-220 Asli", "PSU bekas / Beli baru", "WAJIB switching push-pull 100kHz", false),
    BomItem("b10", "POWER", "SCR1-SCR2", "2", "BT151-600R 600V TO-220", "Beli baru", "WAJIB pemicu busi Center & Side", false),
    BomItem("b11", "POWER", "QNC-QNS", "2", "BC547B TO-92 NPN", "Beli baru", "WAJIB driver pemicu pulsa", false),
    BomItem("b12", "POWER", "QPC-QPS", "2", "BC557B TO-92 PNP", "Beli baru", "WAJIB driver pemicu pulsa", false),
    BomItem("b13", "FAN", "QFAN / MOD_RELAY", "1", "Modul Relay 1-Channel 5V Opto / BC547B NPN", "Beli baru / Modul pasaran", "WAJIB bila kipas radiator dikontrol; sangat disarankan Modul Relay 1-CH 5V", false),
    BomItem("b14", "HV", "DREC1-DREC4", "4", "UF4007 1A 1000V Ultrafast", "Beli baru", "WAJIB jembatan penyearah trafo", false),
    BomItem("b15", "HV", "DCH_C-DCH_S", "2", "UF4007 1A 1000V Ultrafast", "Beli baru", "WAJIB pemisah isolasi Bank Center & Side", false),
    BomItem("b16", "INPUT", "DREV", "1", "SB560 5A 60V Schottky", "PSU bekas / Beli baru", "WAJIB proteksi salah polaritas aki", false),
    BomItem("b17", "INPUT", "TVS_IN", "1", "SMBJ33A / P6KE33A unidirectional", "Beli baru", "WAJIB clamp lonjakan aki 12V", false),
    BomItem("b18", "POWER", "TVS_Q1-Q2", "2", "1.5KE33A / P6KE33A unidirectional", "Beli baru", "WAJIB clamp spike Drain MOSFET", false),
    BomItem("b19", "LOGIC", "DBAT", "8", "BAT54S SOT-23 dual Schottky", "Beli baru", "WAJIB clamp tegangan ke semua pin ADC", false),
    BomItem("b20", "LOGIC", "DCL_A-DCL_B", "2", "1N4148 small signal diode", "PSU / Beli baru", "WAJIB clamp instan proteksi PWM", false),
    BomItem("b21", "HV", "C_CENTER-C_SIDE", "2", "1uF 630V Polypropylene pulse MKP/MPP", "Beli baru", "WAJIB; MUTLAK bukan elko atau X2!", false),
    BomItem("b22", "HV", "RHV_C-RHV_S", "8", "270k 1% 0.25W working >=200V", "Beli baru", "WAJIB 4 seri per jalur ADC feedback", false),
    BomItem("b23", "HV", "RBLEED_C-RBLEED_S", "8", "470k 0.5W working >=200V", "Beli baru", "WAJIB 4 seri per kapasitor pembuang muatan", false),
    BomItem("b24", "POWER", "RSENSE", "1", "0.05 Ohm 5W non-induktif", "Beli baru", "WAJIB shunt pengukur arus trafo", false),
    BomItem("b25", "INPUT", "L_IN", "1", "47uH arus >= 5A", "PSU PC bekas", "WAJIB choke filter catu daya", false),
    BomItem("b26", "INPUT", "C_IN", "1", "470uF 35V low-ESR", "PSU bekas / Beli baru", "WAJIB elko filter input", false),
    BomItem("b27", "POWER", "FMAIN-FLOGIC-FHV", "3", "Fuse Blade + holder 5A / 1A / 3A", "Holder PSU + fuse baru", "WAJIB sikring proteksi berlapis", false),
    BomItem("b28", "CONTROL", "J_TPS", "1", "Header male 2x3 + 2 shunt jumper 2.54mm", "Beli baru", "WAJIB selektor pinout kabel TPS", false),
    BomItem("b29", "OEM_LEARN", "MOD_PC817 / R_DAMP", "1 set", "Modul Optocoupler PC817 4-CH + 2x R 47k 2W", "Beli baru modul pasaran", "WAJIB untuk alur OEM Learn aman PB3 (Center) & PB4 (Side)", false),
    BomItem("b30", "CONTROL", "SW_SERVICE", "1", "Saklar pemutus servis daya / Header 2-Pin", "Beli baru", "OPSIONAL saklar isolator mekanis PCB Power (Interlock dikontrol software R8)", false),
    BomItem("b31", "SETUP", "Q_STROBE", "1", "FQP30N06L logic-level MOSFET TO-220", "Beli baru", "OPSIONAL sangat dianjurkan untuk timing TDC", false),
    BomItem("b32", "SETUP", "LED_STROBE", "1", "Modul LED putih 5V 1W dengan driver", "Beli baru", "OPSIONAL sangat dianjurkan untuk timing TDC", false),
    BomItem("b33", "HARNESS", "J1", "1", "Pigtail pasangan soket CDI 12-pin NS200", "Donor harness / Beli baru", "WAJIB pigtail adaptor; jangan potong kabel motor!", false),
    BomItem("b34", "HARNESS", "J_AUDIO_CTL", "1", "Header terkunci 2-pin JST-XH", "Beli baru", "OPSIONAL dipasang sekarang untuk ekspansi audio", false),
    BomItem("b35", "PASSIVE", "R_SMALL", "1 set", "10R 100R 330R 1k 2.2k 4.7k 8.2k 10k 15k 22k 27k 39k 47k 100k 120k", "Beli kit resistor", "WAJIB ikuti netlist presisi 1%", false),
    BomItem("b36", "PASSIVE", "C_SMALL", "1 set", "4.7nF 10nF 100nF 10uF keramik / film", "PSU + Beli baru", "WAJIB kapasitor filter & bypass", false),
    BomItem("b37", "HARNESS", "WIRE_HV", "1 set", "Kabel isolasi tegangan tinggi 600V + heat-shrink", "Beli baru", "WAJIB jauh dari antena & sensor pulser", false)
  )

  /**
   * Katalog Modul Jadi Pasaran (Commercial Off-The-Shelf Modules)
   * Alternatif modular pengganti komponen diskrit untuk menyederhanakan perakitan CDI R8
   * tanpa merubah kompatibilitas wiring harness 12-pin (J1).
   */
  val modularDropInModules: List<ModularDropInModule> = listOf(
    ModularDropInModule(
      id = "mod_opto_4ch",
      blockName = "OEM Learn Pasif (PB3 & PB4)",
      moduleName = "Modul Optocoupler PC817 4-Channel Isolation Board",
      replacesDiscrete = "2x IC PC817 diskrit + 2x 1N4148 + 2x resistor pullup 4.7k di perfboard",
      estimatedPriceIdr = "Rp 15.000 - Rp 25.000",
      keyFeatures = "Terminal baut screw, 4x LED indikator pulsa kedip, jumper pull-up VCC onboard, isolasi optik 3000V",
      wiringSummary = "Koil Center J1.12 seri R 47k 2W -> IN1+, Koil Side J1.6 seri R 47k 2W -> IN2+, IN1-/IN2- ke GND, OUT1 ke PB3, OUT2 ke PB4, VCC/GND ke WeAct 3V3/GND"
    ),
    ModularDropInModule(
      id = "mod_buck_5v",
      blockName = "Catu Daya Logic (+12V Kontak ke +5V WeAct)",
      moduleName = "Modul Mini DC-DC Buck Converter MP1584EN / LM2596 Mini",
      replacesDiscrete = "Regulator linear LM7805 panas + heatsink besar + kapasitor elko 100uF",
      estimatedPriceIdr = "Rp 8.000 - Rp 15.000",
      keyFeatures = "Efisiensi 92-96% (sangat dingin), ukuran ultra-mini 22x17mm, output 5.0V presisi (arus hingga 3A)",
      wiringSummary = "IN+ ke J1.5 (+12V Kontak), IN- ke J1.11 (GND), OUT+ ke Pin 5V WeAct / Port USB-C, OUT- ke GND WeAct"
    ),
    ModularDropInModule(
      id = "mod_hv_boost",
      blockName = "DITOLAK: Modul Boost HV Generik",
      moduleName = "TIDAK KOMPATIBEL dengan kontrol charger firmware R8",
      replacesDiscrete = "Tidak boleh menggantikan trafo ATX, TC4427, IRF3205 push-pull, feedback HV, dan clamp hardware",
      estimatedPriceIdr = "Rp 45.000 - Rp 75.000",
      keyFeatures = "Tidak memiliki input PWM dua-fasa, loop feedback firmware, dan perpindahan target 285V/345V yang diwajibkan R8",
      wiringSummary = "JANGAN disambungkan. Gunakan blok charger push-pull sesuai langkah wiring dan feedback HV firmware."
    ),
    ModularDropInModule(
      id = "mod_pulser_comp",
      blockName = "Pengkondisi Sinyal Pulser Spul Magnet (J1.10)",
      moduleName = "Modul Komparator LM393 Speed Sensor / Voltage Comparator",
      replacesDiscrete = "IC LM339 diskrit + resistor pembagi tegangan perfboard + filter RC",
      estimatedPriceIdr = "Rp 6.000 - Rp 12.000",
      keyFeatures = "Trimpot penyetel ambang; output harus open-collector/pull-up 3.3V dan diverifikasi tidak pernah melebihi 3.3V",
      wiringSummary = "VCC/pull-up output ke 3V3, GND ke GND motor, input J1.10 melalui pembatas/clamp, output melalui 1k ke PA0 STM32 atau GPIO4 ESP32"
    ),
    ModularDropInModule(
      id = "mod_fan_relay",
      blockName = "Driver Relay Kipas Radiator (J1.7 / PB5)",
      moduleName = "Modul Relay 1-Channel 5V dengan Optocoupler / Modul MOSFET Driver LR7843",
      replacesDiscrete = "Transistor NPN BC547 diskrit + Dioda 1N4007 flyback + resistor gate",
      estimatedPriceIdr = "Rp 9.000 - Rp 15.000",
      keyFeatures = "Isolasi optik penuh, terminal screw output beban, anti lonjakan arus induktif motor kipas radiator",
      wiringSummary = "VCC ke 5V WeAct, GND ke GND WeAct, IN ke PB5 WeAct. Terminal relay NO & COM menyambungkan kabel J1.7 (Biru-Kuning) ke GND aki"
    )
  )
}

/**
 * Representasi modul jadi siap pakai di pasaran untuk upgrade sistem CDI R8 secara modular.
 */
data class ModularDropInModule(
  val id: String,
  val blockName: String,
  val moduleName: String,
  val replacesDiscrete: String,
  val estimatedPriceIdr: String,
  val keyFeatures: String,
  val wiringSummary: String
)
