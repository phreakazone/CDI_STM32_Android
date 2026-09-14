package id.ns200.cdir7

/**
 * Pilihan platform mikrokontroler hardware CDI NS200 R8.
 * Menghindari salah sambung pengkabelan antara WeAct STM32WB55 dan ESP32-WROOM-32 DevKit.
 */
enum class McuPlatform(
    val id: String,
    val displayName: String,
    val shortName: String,
    val chipLabel: String,
    val architecture: String,
    val pinCountLabel: String,
    val logicVoltage: String,
    val pulserPin: String,
    val pulserPinDescription: String,
    val gateCenterPin: String,
    val gateSidePin: String,
    val oemTapCenterPin: String,
    val oemTapSidePin: String,
    val strobePin: String,
    val fanRelayPin: String,
    val faultPin: String,
    val chargerPinA: String,
    val chargerPinB: String,
    val adctpsPin: String,
    val adctempPin: String,
    val adctpsRefPin: String,
    val adchvcPin: String,
    val adchvsPin: String,
    val adcvbatPin: String,
    val benchLoopbackPin: String,
    val criticalSafetyNotice: String
) {
    STM32WB55(
        id = "stm32wb55",
        displayName = "WeAct STM32WB55",
        shortName = "STM32WB55",
        chipLabel = "STM32WB55CGU6",
        architecture = "ARM Cortex-M4 + Cortex-M0+ BLE",
        pinCountLabel = "35 Pin Header (H_TOP & H_BOTTOM)",
        logicVoltage = "3.3V Logic (Sebagian pin 5V tolerant)",
        pulserPin = "PA0 (H_BOTTOM.9 TIM2_CH1)",
        pulserPinDescription = "PA0 via komparator LM393 dari J1.10 (Pulser Pick-up)",
        gateCenterPin = "PA1 (H_BOTTOM.10)",
        gateSidePin = "PA2 (H_BOTTOM.11)",
        oemTapCenterPin = "PB3 (H_TOP.9)",
        oemTapSidePin = "PB4 (H_TOP.8)",
        strobePin = "PB9 (H_BOTTOM.6)",
        fanRelayPin = "PB5 (H_TOP.7)",
        faultPin = "PA10 (H_TOP.11 Active-Low)",
        chargerPinA = "PA9 (H_BOTTOM.18 TIM1_CH2)",
        chargerPinB = "PB8 (H_BOTTOM.7 TIM1_CH2N)",
        adctpsPin = "PA3 (H_BOTTOM.12 ADC1_IN8)",
        adctempPin = "PA4 (H_BOTTOM.13 ADC1_IN9)",
        adctpsRefPin = "PA5 (H_BOTTOM.14 ADC1_IN10)",
        adchvcPin = "PA6 (H_BOTTOM.15 ADC1_IN11)",
        adchvsPin = "PA7 (H_BOTTOM.16 ADC1_IN12)",
        adcvbatPin = "PB0 (H_TOP.14 ADC1_IN15)",
        benchLoopbackPin = "N/A (firmware STM32 tidak menyediakan loopback self-test)",
        criticalSafetyNotice = "WAJIB gunakan isolasi Optocoupler PC817 pada sadapan OEM (PB3 & PB4) dan komparator pada Pulser (PA0). Jangan alirkan tegangan induksi koil atau aki 12V langsung ke pin STM32."
    ),

    ESP32_WROOM(
        id = "esp32_wroom",
        displayName = "ESP32-WROOM-32 DevKit",
        shortName = "ESP32-WROOM",
        chipLabel = "ESP32-WROOM-32 DevKitC",
        architecture = "Xtensa Dual-Core 240MHz + BLE/WiFi",
        pinCountLabel = "38 Pin Header DevKitC (19+19)",
        logicVoltage = "3.3V Logic Murni (TIDAK 5V Tolerant!)",
        pulserPin = "GPIO4 (RIGHT.13)",
        pulserPinDescription = "GPIO4 via komparator LM393 dari J1.10 (Pulser Pick-up)",
        gateCenterPin = "GPIO25 (LEFT.9)",
        gateSidePin = "GPIO26 (LEFT.10)",
        oemTapCenterPin = "GPIO16 (RIGHT.12)",
        oemTapSidePin = "GPIO17 (RIGHT.11)",
        strobePin = "GPIO27 (LEFT.11)",
        fanRelayPin = "GPIO13 (LEFT.15)",
        faultPin = "GPIO14 (LEFT.12 Active-Low)",
        chargerPinA = "GPIO18 (RIGHT.9 MCPWM Push)",
        chargerPinB = "GPIO19 (RIGHT.8 MCPWM Pull)",
        adctpsPin = "GPIO36 / VP (LEFT.3 ADC1_CH0)",
        adctempPin = "GPIO39 / VN (LEFT.4 ADC1_CH3)",
        adctpsRefPin = "GPIO34 (LEFT.5 ADC1_CH6)",
        adchvcPin = "GPIO35 (LEFT.6 ADC1_CH7)",
        adchvsPin = "GPIO32 (LEFT.7 ADC1_CH4)",
        adcvbatPin = "GPIO33 (LEFT.8 ADC1_CH5)",
        benchLoopbackPin = "GPIO5 (RIGHT.10 Uji Bangku SAJA)",
        criticalSafetyNotice = "PERINGATAN KERAS: Pin pickup (GPIO4) dan kedua pin tap OEM (GPIO16 & GPIO17) BUKAN input 12V atau 285–345V. Semuanya harus lewat rangkaian optocoupler/isolator berimpedansi tinggi sebelum menyentuh GPIO ESP32. ESP32 adalah IC 3.3V murni; satu pulsa bocor dari primer koil (ratusan volt) akan menghancurkan chip seketika. HANYA gunakan ADC1 (GPIO32-39) untuk sensor karena ADC2 tidak aktif saat BLE menyala!"
    );

    companion object {
        fun fromId(id: String?): McuPlatform =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: STM32WB55
    }
}
