package com.example.model

import id.ns200.cdir7.McuPlatform

enum class PcbBoard(val title: String, val size: String, val colorCode: Long) {
  PCB_LOGIC("PCB Logic & Sensor", "7x9 cm Single-Layer", 0xFF00E5FF),
  PCB_POWER("PCB Power & HV", "Minimal 5x7 cm (Clearance >= 6mm)", 0xFFFF9E0B),
  AUDIO_BOARD("Modul Audio Opsional", "Board Terpisah PAM8610", 0xFFB388FF),
  HARNESS_PIGTAIL("Pigtail Harness NS200", "Soket 12-Pin Original", 0xFF00E676)
}

enum class VerificationType {
  MULTIMETER_VOLT,
  MULTIMETER_CONTINUITY,
  MULTIMETER_OHM,
  VISUAL_INSPECTION,
  JUMPER_STATE
}

data class StepComponent(
  val ref: String,
  val name: String,
  val spec: String,
  val pinDescription: String
)

data class PinConnection(
  val id: String,
  val fromNode: String,
  val toNode: String,
  val wireColorHex: Long,
  val wireLabel: String,
  val solderTip: String,
  val isHighVoltage: Boolean = false
)

data class WiringStep(
  val id: String,
  val stageId: Int,
  val stageTitle: String,
  val stepNumber: String,
  val title: String,
  val board: PcbBoard,
  val sourcePin: String,
  val targetPin: String,
  val components: List<StepComponent>,
  val schematicTrace: String,
  val perfboardTips: List<String>,
  val pinLegGuide: String,
  val verificationRequirement: String,
  val verificationType: VerificationType,
  val expectedValue: String,
  val criticalSafetyWarning: String? = null,
  val pinConnections: List<PinConnection> = emptyList()
)

data class HarnessPin(
  val pinNumber: Int,
  val wireColor: String,
  val name: String,
  val direction: String,
  val completePath: String,
  val destination: String,
  val status: String,
  val isConnected: Boolean = true,
  val detailGuide: String
)

data class WeActPin(
  val header: String, // "H_TOP" or "H_BOTTOM"
  val pinNumber: Int,
  val name: String,
  val direction: String,
  val fullPath: String,
  val finalDestination: String,
  val status: String,
  val isCritical: Boolean = false,
  val warning: String? = null
)

data class Esp32Pin(
  val headerSide: String, // "LEFT" or "RIGHT"
  val pinNumber: Int,
  val name: String,
  val stmEquivalent: String,
  val functionCategory: String, // "PULSER", "GATE", "OEM_LEARN", "SECONDARY", "ADC1_SENSOR", "POWER", "BENCH"
  val direction: String,
  val fullPath: String,
  val finalDestination: String,
  val status: String,
  val isCritical: Boolean = false,
  val warning: String? = null
)

data class PinLeg(
  val pinNumber: String,
  val name: String,
  val description: String
)

data class ComponentPinout(
  val ref: String,
  val name: String,
  val packageType: String,
  val ratingSpec: String,
  val pinLegs: List<PinLeg>,
  val orientationGuide: String,
  val donorPsuRule: String,
  val safetyNotice: String? = null
)

data class VerificationRecord(
  val stepId: String,
  val isVerified: Boolean = false,
  val measuredValue: String = "",
  val userNotes: String = "",
  val verifiedTimestamp: Long = 0L
)

data class BomItem(
  val id: String,
  val section: String,
  val ref: String,
  val qty: String,
  val spec: String,
  val source: String,
  val notes: String,
  val isAcquired: Boolean = false
)

data class QuickSetupStep(
  val stepNumber: Int,
  val stageName: String,
  val connectionCondition: String,
  val appAction: String,
  val outputCondition: String,
  val proceedCriteria: String,
  val stopHazard: String
)

/**
 * Dynamically adapts all textual and pin descriptors in a WiringStep
 * to match the selected MCU hardware platform (STM32WB55 vs ESP32-WROOM-32).
 */
fun WiringStep.adaptToPlatform(platform: McuPlatform): WiringStep {
  if (platform != McuPlatform.ESP32_WROOM) return this

  fun String.toEsp(): String = this
    .replace("WeAct Studio STM32WB55CGU6", "ESP32-WROOM-32 DevKit V1")
    .replace("WeAct STM32WB55", "ESP32-WROOM-32")
    .replace("WeAct Studio", "ESP32 DevKit")
    .replace("WeAct board", "ESP32 board")
    .replace("board WeAct", "board ESP32")
    .replace("WeAct", "ESP32")
    .replace("STM32WB55", "ESP32-WROOM-32")
    .replace("STM32", "ESP32")
    .replace("H_BOTTOM.2 (5V)", "VIN (5V In)")
    .replace("H_BOTTOM.1 (G)", "GND")
    .replace("H_TOP.1 (G)", "GND")
    .replace("H_BOTTOM.4 (VBAT)", "3V3 (DILARANG 12V!)")
    .replace("H_BOTTOM.9 (PA0)", "GPIO4 (Pulser ISR)")
    .replace("H_BOTTOM.10 (PA1)", "GPIO25 (Gate Center)")
    .replace("H_BOTTOM.11 (PA2)", "GPIO26 (Gate Side)")
    .replace("H_BOTTOM.12 (PA3)", "GPIO36 / VP (ADC1_CH0 TPS)")
    .replace("H_BOTTOM.13 (PA4)", "GPIO39 / VN (ADC1_CH3 Temp)")
    .replace("H_BOTTOM.14 (PA5)", "GPIO34 (TPS_REF)")
    .replace("H_BOTTOM.15 (PA6)", "GPIO35 (ADC1_CH7 HV_C FB)")
    .replace("H_BOTTOM.16 (PA7)", "GPIO32 (ADC1_CH4 HV_S FB)")
    .replace("H_BOTTOM.18 (PA9)", "GPIO18 (Charger A)")
    .replace("H_BOTTOM.7 (PB8)", "GPIO19 (Charger B)")
    .replace("H_BOTTOM.6 (PB9)", "GPIO27 (Strobe TDC)")
    .replace("H_TOP.14 (PB0)", "GPIO33 (ADC1_CH5 VBAT)")
    .replace("H_TOP.11 (PA10)", "GPIO14 (Hardware Fault)")
    .replace("H_TOP.9 (PB3)", "GPIO16 (OEM Tap Center)")
    .replace("H_TOP.8 (PB4)", "GPIO17 (OEM Tap Side)")
    .replace("H_TOP.7 (PB5)", "GPIO13 (Fan Relay)")
    .replace("H_BOTTOM.2", "VIN")
    .replace("H_BOTTOM.1", "GND")
    .replace("H_TOP.1", "GND")
    .replace("H_TOP.14", "GPIO33")
    .replace("H_TOP.9", "GPIO16")
    .replace("H_TOP.8", "GPIO17")
    .replace("H_TOP.7", "GPIO13")
    .replace("H_TOP.11", "GPIO14")
    .replace("H_BOTTOM.9", "GPIO4")
    .replace("H_BOTTOM.10", "GPIO25")
    .replace("H_BOTTOM.11", "GPIO26")
    .replace("H_BOTTOM.12", "GPIO36(VP)")
    .replace("H_BOTTOM.13", "GPIO39(VN)")
    .replace("H_BOTTOM.14", "GPIO34")
    .replace("H_BOTTOM.15", "GPIO35")
    .replace("H_BOTTOM.16", "GPIO32")
    .replace("H_BOTTOM.18", "GPIO18")
    .replace("H_BOTTOM.7", "GPIO19")
    .replace("H_BOTTOM.6", "GPIO27")
    .replace("PA0", "GPIO4")
    .replace("PA1", "GPIO25")
    .replace("PA2", "GPIO26")
    .replace("PA3", "GPIO36(VP)")
    .replace("PA4", "GPIO39(VN)")
    .replace("PA5", "GPIO34")
    .replace("PA6", "GPIO35")
    .replace("PA7", "GPIO32")
    .replace("PA9", "GPIO18")
    .replace("PA10", "GPIO14")
    .replace("PB0", "GPIO33")
    .replace("PB3", "GPIO16")
    .replace("PB4", "GPIO17")
    .replace("PB5", "GPIO13")
    .replace("PB8", "GPIO19")
    .replace("PB9", "GPIO27")
    .replace("LM339", "PC817 / LM393")

  return this.copy(
    title = this.title.toEsp(),
    sourcePin = this.sourcePin.toEsp(),
    targetPin = this.targetPin.toEsp(),
    schematicTrace = this.schematicTrace.toEsp(),
    pinLegGuide = this.pinLegGuide.toEsp(),
    verificationRequirement = this.verificationRequirement.toEsp(),
    criticalSafetyWarning = this.criticalSafetyWarning?.toEsp(),
    perfboardTips = this.perfboardTips.map { it.toEsp() },
    components = this.components.map { comp ->
      comp.copy(
        name = comp.name.toEsp(),
        spec = comp.spec.toEsp(),
        pinDescription = comp.pinDescription.toEsp()
      )
    }
  )
}

