package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PcbBoard
import com.example.model.WiringStep
import com.example.ui.theme.*
import id.ns200.cdir7.McuPlatform
import id.ns200.cdir7.ui.components.MotecButton
import id.ns200.cdir7.ui.theme.TextMuted
import kotlinx.coroutines.launch

/**
 * FullCumulativeCircuitSimulator:
 * Visual simulator of the entire CDI circuit as it accumulates from Step 1.1 to the complete final build!
 * Fulfills the user requirement:
 * "dan sampai langkah seluruh rangkaian final terbentuk"
 * Allows inspecting the fully assembled system or timeline accumulation.
 */
@Composable
fun FullCumulativeCircuitSimulator(
  currentStep: WiringStep,
  allSteps: List<WiringStep>,
  verifiedStepIds: Set<String>,
  onSelectStep: (Int) -> Unit,
  onClose: () -> Unit,
  platform: McuPlatform = McuPlatform.STM32WB55,
  modifier: Modifier = Modifier
) {
  var showFinal100Percent by remember { mutableStateOf(false) }
  val scrollState = rememberScrollState()
  val coroutineScope = rememberCoroutineScope()
  val density = androidx.compose.ui.platform.LocalDensity.current

  // Determine active stage based on view mode
  val activeStage = if (showFinal100Percent) 6 else currentStep.stageId

  val pulseAnim = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by pulseAnim.animateFloat(
    initialValue = 0.5f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(850, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow"
  )

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = TechSurface),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.linearGradient(
        listOf(ElectricCyan, HighVoltageRed.copy(alpha = 0.6f))
      )
    )
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Top Bar Controls
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Memory,
              contentDescription = null,
              tint = ElectricCyan,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "SIMULATOR RANGKAIAN KUMULATIF LENGKAP",
              style = MaterialTheme.typography.titleSmall,
              color = ElectricCyan,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace
            )
          }
          Text(
            text = if (showFinal100Percent)
              "Tampilan Rangkaian CDI Final Terpasang 100% Siap Mesin"
            else
              "Akumulasi sirkuit hingga Tahap $activeStage (${currentStep.stageTitle})",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = if (showFinal100Percent) SafetyGreen else SparkAmber
          )
        }

        IconButton(onClick = onClose) {
          Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondaryDark)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Mode Selector Tabs (Motec square semi-transparent style)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        MotecButton(
          text = "HINGGA LANGKAH INI (${currentStep.stepNumber})",
          onClick = { showFinal100Percent = false },
          color = if (!showFinal100Percent) ElectricCyan else TextMuted,
          height = 32.dp,
          fontSize = 10.sp,
          modifier = Modifier.weight(1f)
        )

        MotecButton(
          text = "RANGKAIAN FINAL (100%)",
          onClick = { showFinal100Percent = true },
          color = if (showFinal100Percent) SafetyGreen else TextMuted,
          icon = Icons.Default.CheckCircle,
          height = 32.dp,
          fontSize = 10.sp,
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Stage Navigation Quick Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        listOf(
          1 to "Tahap 1: Power & MCU",
          2 to "Tahap 2: Sensor & LM339",
          3 to "Tahap 3: Trafo & Push-Pull",
          4 to "Tahap 4: Rectifier & Dual HV",
          5 to "Tahap 5: Kapasitor & SCR",
          6 to "Tahap 6: Final CDI"
        ).forEach { (stageNum, title) ->
          val isStageActive = stageNum <= activeStage
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isStageActive) ElectricCyan.copy(alpha = 0.15f) else Color(0xFF111927),
            border = BorderStroke(
              1.dp,
              if (isStageActive) ElectricCyan else Color(0xFF223247)
            ),
            modifier = Modifier.clickable {
              val firstStepOfStage = allSteps.indexOfFirst { it.stageId == stageNum }
              if (firstStepOfStage >= 0) {
                showFinal100Percent = false
                onSelectStep(firstStepOfStage)
              }
            }
          ) {
            Text(
              text = title,
              fontSize = 10.sp,
              color = if (isStageActive) ElectricCyan else TextSecondaryDark,
              fontWeight = if (isStageActive) FontWeight.Bold else FontWeight.Normal,
              fontFamily = FontFamily.Monospace,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Quick Section Focus Bar & Navigation
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // 1. Jump to Harness
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF131B26),
          border = BorderStroke(1.dp, Color(0xFF2D3E53)),
          modifier = Modifier
            .weight(1f)
            .height(30.dp)
            .clickable {
              coroutineScope.launch {
                scrollState.animateScrollTo(0)
              }
            }
        ) {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Power, contentDescription = null, tint = SparkAmber, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Soket J1", fontSize = 10.sp, color = SparkAmber, fontWeight = FontWeight.Bold)
          }
        }

        // 2. Jump to MCU (Logic PCB)
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF0F1E29),
          border = BorderStroke(1.2.dp, ElectricCyan),
          modifier = Modifier
            .weight(1.4f)
            .height(30.dp)
            .clickable {
              coroutineScope.launch {
                // Scroll past harness (130dp + 18dp + 14dp padding ~ 162dp)
                val targetPx = with(density) { 158.dp.roundToPx() }
                scrollState.animateScrollTo(targetPx)
              }
            }
        ) {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Memory, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              if (platform == McuPlatform.ESP32_WROOM) "⚡ Fokus MCU (ESP32)" else "⚡ Fokus MCU (STM32)",
              fontSize = 10.sp,
              color = ElectricCyan,
              fontWeight = FontWeight.Black
            )
          }
        }

        // 3. Jump to Power PCB
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF261214),
          border = BorderStroke(1.dp, HighVoltageRed),
          modifier = Modifier
            .weight(1.1f)
            .height(30.dp)
            .clickable {
              coroutineScope.launch {
                val targetPx = with(density) { 750.dp.roundToPx() }
                scrollState.animateScrollTo(targetPx)
              }
            }
        ) {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = HighVoltageRed, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("PCB 2 (HV)", fontSize = 10.sp, color = HighVoltageRed, fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Visual Hint Banner
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Geser horizontal ↔ untuk 30 pin header",
          fontSize = 9.sp,
          color = TextTertiaryDark,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = "Ketuk 'Fokus MCU' untuk ke pin",
          fontSize = 9.sp,
          color = ElectricCyan,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Big Perfboard Simulation Canvas
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0A0F14), // Dark FR4 perfboard background
        border = BorderStroke(1.5.dp, Color(0xFF1E293B)),
        modifier = Modifier
          .fillMaxWidth()
          .height(440.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(scrollState)
            .padding(14.dp)
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.Top
          ) {
            // 1. NS200 J1 HARNESS CONNECTOR (Input / Output Hub)
            HarnessAssemblySection(
              activeStage = activeStage,
              pulseAlpha = pulseAlpha
            )

            // 2. PCB 1: LOGIC & CONTROL BOARD (7 x 9 cm)
            PcbLogicAssemblySection(
              activeStage = activeStage,
              pulseAlpha = pulseAlpha,
              platform = platform
            )

            // 3. SAFETY CLEARANCE PHYSICAL GAP (>= 6mm Barrier)
            SafetyClearanceGapSection()

            // 4. PCB 2: POWER & HIGH VOLTAGE DISCHARGE BOARD (5 x 7 cm)
            PcbPowerAssemblySection(
              activeStage = activeStage,
              pulseAlpha = pulseAlpha
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Technical Specifications & Live Circuit Parameters
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "STATUS RANGKAIAN SAAT INI:",
            fontSize = 9.sp,
            color = SparkAmber,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = if (activeStage >= 5)
              "Semua Blok Terhubung: Catu Daya, Sensor ADC/Pulser, Charger Trafo Push-Pull, Dual Bank HV 285V, & SCR Busi."
            else
              "Blok Tahap $activeStage aktif tersambung. Lanjutkan solder ke langkah berikutnya untuk melengkapi CDI.",
            fontSize = 11.sp,
            color = TextSecondaryDark
          )
        }
      }
    }
  }
}

/**
 * 1. Harness Section: 12-pin plug NS200 (Full 12 Pins with status indicators & scroll)
 */
@Composable
private fun HarnessAssemblySection(
  activeStage: Int,
  pulseAlpha: Float
) {
  // All 12 Pins of Bajaj Pulsar 200NS CDI Harness J1
  data class HarnessPinItem(
    val pin: String,
    val colorLabel: String,
    val func: String,
    val requiredStage: Int?,
    val isOemProbe: Boolean = false,
    val isNc: Boolean = false
  )

  val all12Pins = remember {
    listOf(
      HarnessPinItem("J1.1", "KOSONG / NC", "Tidak Terhubung", null, isNc = true),
      HarnessPinItem("J1.2", "HIJAU-PUTIH", "TPS A (Sensor Gas)", 2),
      HarnessPinItem("J1.3", "HITAM-PUTIH", "Suhu Coolant (NTC)", 2),
      HarnessPinItem("J1.4", "ABU-ABU", "TPS B (Sensor Gas)", 2),
      HarnessPinItem("J1.5", "ORANYE", "+12V Kontak Kunci", 1),
      HarnessPinItem("J1.6", "HITAM-MERAH", "Koil Samping HV (2 Busi)", 5),
      HarnessPinItem("J1.7", "BIRU-KUNING", "Kontrol Relay Fan", 2),
      HarnessPinItem("J1.8", "NC / KABEL", "OEM Side Probe (PB4)", 2, isOemProbe = true),
      HarnessPinItem("J1.9", "NC / KABEL", "OEM Ctr Probe (PB3)", 2, isOemProbe = true),
      HarnessPinItem("J1.10", "PUTIH-MERAH", "Pulser Spul Magnet", 2),
      HarnessPinItem("J1.11", "HITAM-KUNING", "Massa Utama (GND Star)", 1),
      HarnessPinItem("J1.12", "ORANYE-KUNING", "Koil Center HV (Busi Utama)", 5)
    )
  }

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = Color(0xFF131B26),
    border = BorderStroke(1.dp, Color(0xFF2D3E53)),
    modifier = Modifier
      .width(152.dp)
      .height(410.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 6.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Header Section with 12 PIN badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "SOKET J1",
          fontSize = 9.sp,
          fontWeight = FontWeight.Black,
          color = SparkAmber,
          fontFamily = FontFamily.Monospace
        )
        Surface(
          shape = RoundedCornerShape(3.dp),
          color = SparkAmber.copy(alpha = 0.2f),
          border = BorderStroke(0.5.dp, SparkAmber.copy(alpha = 0.6f))
        ) {
          Text(
            text = "12 PIN",
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Black,
            color = SparkAmber,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Text(
        text = "Bajaj Pulsar NS200",
        fontSize = 7.sp,
        color = TextTertiaryDark,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Scrollable 12-Pin List (Fixes truncation so all 12 pins are clearly visible)
      Column(
        verticalArrangement = Arrangement.spacedBy(3.5.dp),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .verticalScroll(rememberScrollState())
      ) {
        all12Pins.forEach { item ->
          val isPinActive = when {
            item.isNc -> false
            item.isOemProbe -> activeStage >= 2
            item.requiredStage != null -> activeStage >= item.requiredStage
            else -> false
          }

          val statusText = when {
            item.isNc -> "KOSONG"
            isPinActive -> "AKTIF"
            item.isOemProbe -> "OEM LEARN"
            item.requiredStage != null -> "TAHAP ${item.requiredStage}"
            else -> "STANDBY"
          }

          val statusColor = when {
            item.isNc -> Color(0xFF64748B)
            isPinActive -> SafetyGreen
            item.isOemProbe -> ElectricCyan
            else -> SparkAmber.copy(alpha = 0.8f)
          }

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isPinActive) Color(0xFF18283D) else Color(0xFF0F1722),
            border = BorderStroke(
              0.8.dp,
              if (isPinActive) ElectricCyan.copy(alpha = 0.8f) else Color(0xFF1E293B)
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Status dot
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .background(statusColor, CircleShape)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = item.pin,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPinActive) ElectricCyan else TextPrimaryDark,
                    fontFamily = FontFamily.Monospace
                  )
                  Text(
                    text = statusText,
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    fontFamily = FontFamily.Monospace
                  )
                }
                Text(
                  text = item.colorLabel,
                  fontSize = 6.5.sp,
                  color = if (isPinActive) TextPrimaryDark else TextTertiaryDark,
                  maxLines = 1
                )
                Text(
                  text = item.func,
                  fontSize = 6.sp,
                  color = if (isPinActive) Color(0xFF81D4FA) else TextTertiaryDark,
                  maxLines = 1
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "↕ Gulir 12 Pin • Klip Atas",
        fontSize = 6.5.sp,
        color = TextTertiaryDark,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}

/**
 * 2. PCB 1: Logic Board (7 x 9 cm)
 */
@Composable
private fun PcbLogicAssemblySection(
  activeStage: Int,
  pulseAlpha: Float,
  platform: McuPlatform = McuPlatform.STM32WB55
) {
  val isEsp = platform == McuPlatform.ESP32_WROOM
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (isEsp) Color(0xFF0F141C) else Color(0xFF0E1A17), // Dark slate for ESP32, dark green for STM32
    border = BorderStroke(2.dp, if (isEsp) SparkAmber.copy(alpha = 0.8f) else Color(0xFF1E4D2B)),
    modifier = Modifier
      .width(550.dp)
      .height(400.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (isEsp) "PCB 1: LOGIC & CONTROL (ESP32)" else "PCB 1: LOGIC & CONTROL (STM32)",
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          color = if (isEsp) SparkAmber else SafetyGreen,
          fontFamily = FontFamily.Monospace
        )
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = if (isEsp) SparkAmber.copy(alpha = 0.2f) else Color(0xFF064E3B)
        ) {
          Text(
            text = if (isEsp) "ESP32 3.3V • TERISOLASI" else "VOLTASE AMAN <= 12V",
            fontSize = 7.sp,
            color = if (isEsp) SparkAmber else Color(0xFFA7F3D0),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // MCU Board: Switch dynamically based on selected platform
      if (isEsp) {
        RealisticEsp32Board(
          activePins = when {
            activeStage >= 5 -> setOf("VIN", "GND", "3V3", "4", "13", "14", "16", "17", "18", "19", "25", "26", "27", "32", "33", "34", "35", "VP", "VN")
            activeStage >= 4 -> setOf("VIN", "GND", "3V3", "4", "13", "14", "16", "17", "18", "19", "27", "32", "33", "34", "35", "VP", "VN")
            activeStage >= 3 -> setOf("VIN", "GND", "3V3", "4", "13", "14", "16", "17", "18", "19", "27", "33", "34", "VP", "VN")
            activeStage >= 2 -> setOf("VIN", "GND", "3V3", "4", "13", "16", "17", "27", "33", "34", "VP", "VN")
            else -> setOf("VIN", "GND", "3V3")
          },
          highlightedPin = if (activeStage >= 2) "4" else "VIN"
        )
      } else {
        RealisticWeActBoard(
          activePins = when {
            activeStage >= 5 -> setOf("G", "5V", "3V3", "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A9", "A10", "B0", "B3", "B4", "B5", "B8", "B9")
            activeStage >= 4 -> setOf("G", "5V", "3V3", "A0", "A3", "A4", "A5", "A6", "A7", "A9", "A10", "B0", "B3", "B4", "B5", "B8", "B9")
            activeStage >= 3 -> setOf("G", "5V", "3V3", "A0", "A3", "A4", "A5", "A9", "A10", "B0", "B3", "B4", "B5", "B8", "B9")
            activeStage >= 2 -> setOf("G", "5V", "3V3", "A0", "A3", "A4", "A5", "B0", "B3", "B4", "B5", "B9")
            else -> setOf("G", "5V", "3V3")
          },
          highlightedPin = if (activeStage >= 2) "A0" else "5V"
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Lower Logic Components Row (LM2596, LM339/PC817, Protection, MOSFET Strobe)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // LM2596 Module Card
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Color(0xFF0D47A1),
          border = BorderStroke(1.dp, Color(0xFF42A5F5)),
          modifier = Modifier.width(90.dp).height(74.dp)
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("LM2596 STEP-DOWN", fontSize = 6.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text("12V IN -> 5.00V OUT", fontSize = 6.sp, color = Color(0xFF90CAF9), fontFamily = FontFamily.Monospace)
            Text(if (isEsp) "Catu VIN ESP32" else "Catu Daya MCU", fontSize = 5.5.sp, color = SafetyGreen)
          }
        }

        // Pulser Conditioning: LM339 (STM32) vs PC817 / LM393 (ESP32)
        if (activeStage >= 2) {
          if (isEsp) {
            RealisticIcChip(
              ref = "U2",
              partNumber = "PC817",
              pinCount = 4,
              activePins = setOf(1, 2, 3, 4),
              pinLabels = mapOf(1 to "Spul+", 2 to "GND_M", 3 to "GND", 4 to "GPIO4")
            )
          } else {
            RealisticIcChip(
              ref = "U2",
              partNumber = "LM339N",
              pinCount = 14,
              activePins = setOf(2, 3, 4, 5, 12),
              pinLabels = mapOf(2 to "PA0", 3 to "5V", 5 to "Spul", 12 to "GND")
            )
          }
        } else {
          // Placeholder ghost outline
          GhostComponentBox(name = if (isEsp) "U2: PC817 (Pulser)" else "U2: LM339 (Pulser)", width = 60.dp, height = 74.dp)
        }

        // Resistors and Diodes conditioning (BAT54S, R_BAT1, R_BAT2)
        if (activeStage >= 2) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RealisticDiode(ref = "BAT54S", partName = "CLAMP", isGlass = true)
            RealisticResistor(
              ref = "R_BAT",
              valueText = if (isEsp) "100k/1k+22k" else "100k/22k",
              colorBands = listOf(Color.Red, Color.Red, Color.Black)
            )
          }
        } else {
          GhostComponentBox(name = "Sensor R/D", width = 60.dp, height = 74.dp)
        }

        // Strobe Timing MOSFET (FQP30N06L) & Fan Transistor
        if (activeStage >= 2) {
          RealisticTo220(ref = "Q_STR", partName = "FQP30N06", pin1Label = "G", pin2Label = "D", pin3Label = "S")
        } else {
          GhostComponentBox(name = "Q_STR Strobe", width = 50.dp, height = 74.dp)
        }
      }
    }
  }
}

/**
 * 3. Physical Safety Clearance Barrier
 */
@Composable
private fun SafetyClearanceGapSection() {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = Color(0xFF261214), // Red warning barrier
    border = BorderStroke(1.5.dp, HighVoltageRed),
    modifier = Modifier
      .width(58.dp)
      .height(400.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Icon(Icons.Default.Warning, contentDescription = null, tint = HighVoltageRed, modifier = Modifier.size(16.dp))

      Text(
        text = "ISOLASI\nFISIK\n>=\n6 mm\n\nCELAH\nUDARA\nAMAN",
        fontSize = 7.5.sp,
        fontWeight = FontWeight.Black,
        color = HighVoltageRed,
        textAlign = TextAlign.Center,
        fontFamily = FontFamily.Monospace
      )

      Icon(Icons.Default.Warning, contentDescription = null, tint = HighVoltageRed, modifier = Modifier.size(16.dp))
    }
  }
}

/**
 * 4. PCB 2: Power & HV Discharge Board (5 x 7 cm)
 */
@Composable
private fun PcbPowerAssemblySection(
  activeStage: Int,
  pulseAlpha: Float
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = Color(0xFF1F1012), // PCB dark red/copper high-voltage zone
    border = BorderStroke(2.dp, HighVoltageRed),
    modifier = Modifier
      .width(520.dp)
      .height(400.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "PCB 2: POWER & HV DISCHARGE BOARD (5 x 7 cm)",
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          color = HighVoltageRed,
          fontFamily = FontFamily.Monospace
        )
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = HighVoltageRed
        ) {
          Text(
            text = "DANGER: TEGANGAN TINGGI 285V DC",
            fontSize = 7.sp,
            color = Color.White,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Stage 3 & 4 Power Row: Transformer T1, TC4427, IRF3205, Bridge Rectifier
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Push-Pull Transformer T1 (Stage 3+)
        if (activeStage >= 3) {
          RealisticTransformer(ref = "T1", spec = "ATX EE-35 Push-Pull")
        } else {
          GhostComponentBox(name = "T1: Trafo ATX", width = 88.dp, height = 70.dp)
        }

        // Gate Driver TC4427 DIP-8 (Stage 3+)
        if (activeStage >= 3) {
          RealisticIcChip(
            ref = "U4",
            partNumber = "TC4427A",
            pinCount = 8,
            activePins = setOf(2, 4, 5, 7),
            pinLabels = mapOf(2 to "INA", 4 to "INB", 5 to "OUTB", 7 to "OUTA")
          )
        } else {
          GhostComponentBox(name = "U4: TC4427 Driver", width = 60.dp, height = 70.dp)
        }

        // Push-Pull MOSFETs IRF3205 (Stage 3+)
        if (activeStage >= 3) {
          RealisticTo220(ref = "QHV1", partName = "IRF3205", pin1Label = "G", pin2Label = "D", pin3Label = "S")
          RealisticTo220(ref = "QHV2", partName = "IRF3205", pin1Label = "G", pin2Label = "D", pin3Label = "S")
        } else {
          GhostComponentBox(name = "2x IRF3205", width = 70.dp, height = 70.dp)
        }

        // Ultrafast Rectifier Diodes UF4007 (Stage 4+)
        if (activeStage >= 4) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BRIDGE HV", fontSize = 7.sp, color = SparkAmber, fontWeight = FontWeight.Bold)
            RealisticDiode(ref = "DREC", partName = "UF4007", cathodeLabel = "285V DC")
            RealisticDiode(ref = "DCH_C", partName = "UF4007", cathodeLabel = "HV_C")
          }
        } else {
          GhostComponentBox(name = "Bridge UF4007", width = 60.dp, height = 70.dp)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Stage 5 Discharge Row: Dual MKP Capacitors & Dual SCR BT151
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Dual MKP Capacitors (Stage 5+)
        if (activeStage >= 5) {
          RealisticMkpCapacitor(ref = "C_CENTER", spec = "1.0uF 630V", color = Color(0xFFB71C1C))
          RealisticMkpCapacitor(ref = "C_SIDE", spec = "1.0uF 630V", color = Color(0xFF1565C0))
        } else {
          GhostComponentBox(name = "C_CENTER 1uF MKP", width = 65.dp, height = 65.dp)
          GhostComponentBox(name = "C_SIDE 1uF MKP", width = 65.dp, height = 65.dp)
        }

        // Dual SCR BT151 (Stage 5+)
        if (activeStage >= 5) {
          RealisticTo220(ref = "SCR1", partName = "BT151-600R", pin1Label = "K", pin2Label = "A", pin3Label = "G", isTabHighVoltage = true)
          RealisticTo220(ref = "SCR2", partName = "BT151-600R", pin1Label = "K", pin2Label = "A", pin3Label = "G", isTabHighVoltage = true)
        } else {
          GhostComponentBox(name = "SCR1 BT151", width = 60.dp, height = 65.dp)
          GhostComponentBox(name = "SCR2 BT151", width = 60.dp, height = 65.dp)
        }

        // Bleeder Resistor Bank
        if (activeStage >= 5) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BLEEDER", fontSize = 7.sp, color = SparkAmber, fontWeight = FontWeight.Bold)
            RealisticResistor(ref = "R_BL1", valueText = "470k", colorBands = listOf(Color(0xFFFDD835), Color(0xFF7E57C2), Color(0xFFFDD835)))
            RealisticResistor(ref = "R_BL2", valueText = "470k", colorBands = listOf(Color(0xFFFDD835), Color(0xFF7E57C2), Color(0xFFFDD835)))
          }
        } else {
          GhostComponentBox(name = "Bleeder 470k", width = 60.dp, height = 65.dp)
        }
      }
    }
  }
}

@Composable
private fun GhostComponentBox(
  name: String,
  width: androidx.compose.ui.unit.Dp,
  height: androidx.compose.ui.unit.Dp
) {
  Box(
    modifier = Modifier
      .width(width)
      .height(height)
      .border(1.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
      .background(Color(0xFF0F172A).copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = name,
      fontSize = 6.5.sp,
      color = Color(0xFF64748B),
      textAlign = TextAlign.Center,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.padding(2.dp)
    )
  }
}
