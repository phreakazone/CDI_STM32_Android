package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WiringDataProvider
import com.example.model.Esp32Pin
import com.example.model.WeActPin
import com.example.ui.components.Esp32HeaderVisualizer
import com.example.ui.components.WeActHeaderVisualizer
import com.example.ui.theme.*
import com.example.viewmodel.WiringViewModel
import id.ns200.cdir7.McuPlatform
import id.ns200.cdir7.ui.components.MotecButton
import id.ns200.cdir7.ui.theme.TextMuted

@Composable
fun WeActHeaderScreen(
  viewModel: WiringViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val activePlatform = uiState.mcuPlatform

  var selectedFilter by remember { mutableStateOf("SEMUA") }
  val weActPins = WiringDataProvider.weActPins
  val esp32Pins = WiringDataProvider.esp32Pins
  val filterScrollState = rememberScrollState()

  // Unified Filter Logic across both platforms
  val filteredWeActPins = remember(selectedFilter, weActPins) {
    when (selectedFilter) {
      "PULSER" -> weActPins.filter { it.name == "PA0" }
      "GATE" -> weActPins.filter { it.name == "PA1" || it.name == "PA2" }
      "OEM_LEARN" -> weActPins.filter { it.name == "PB3" || it.name == "PB4" }
      "ADC/SENSOR" -> weActPins.filter { it.name in listOf("PA3", "PA4", "PA5", "PA6", "PA7", "PB0") }
      "CHARGER" -> weActPins.filter { it.name == "PB8" || it.name == "PA9" }
      "KRITIS" -> weActPins.filter { it.isCritical || it.warning != null }
      "H_TOP" -> weActPins.filter { it.header == "H_TOP" }
      "H_BOTTOM" -> weActPins.filter { it.header == "H_BOTTOM" }
      else -> weActPins
    }
  }

  val filteredEsp32Pins = remember(selectedFilter, esp32Pins) {
    when (selectedFilter) {
      "PULSER" -> esp32Pins.filter { it.functionCategory == "PULSER" || it.name.contains("GPIO04") }
      "GATE" -> esp32Pins.filter { it.functionCategory == "GATE" }
      "OEM_LEARN" -> esp32Pins.filter { it.functionCategory == "OEM_LEARN" }
      "ADC/SENSOR" -> esp32Pins.filter { it.functionCategory == "ADC1_SENSOR" }
      "CHARGER" -> esp32Pins.filter { it.functionCategory == "CHARGER_PWM" }
      "KRITIS" -> esp32Pins.filter { it.isCritical || it.warning != null }
      "H_TOP", "KIRI" -> esp32Pins.filter { it.headerSide == "LEFT" }
      "H_BOTTOM", "KANAN" -> esp32Pins.filter { it.headerSide == "RIGHT" }
      else -> esp32Pins
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(TechDarkBg)
      .padding(horizontal = 14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
  ) {
    // 1. Compact Platform Bar (No wasted vertical gap)
    item {
      PlatformSwitcherBanner(
        activePlatform = activePlatform,
        onSelectPlatform = { viewModel.setMcuPlatform(it) }
      )
    }

    // 2. Hardware-specific Visualizer (Dual Mode: Tabel Pinout vs Fisik Board)
    item {
      if (activePlatform == McuPlatform.STM32WB55) {
        WeActHeaderVisualizer(
          weActPins = weActPins,
          selectedPin = uiState.selectedWeActPin,
          onSelectPin = { pin ->
            viewModel.selectWeActPin(pin)
          }
        )
      } else {
        Esp32HeaderVisualizer(
          esp32Pins = esp32Pins,
          selectedPin = uiState.selectedEsp32Pin,
          onSelectPin = { pin ->
            viewModel.selectEsp32Pin(pin)
          }
        )
      }
    }

    // 3. Compact Platform Safety Banner
    item {
      if (activePlatform == McuPlatform.ESP32_WROOM) {
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = HighVoltageRed.copy(alpha = 0.10f)),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(HighVoltageRed, SparkAmber))
          )
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = "Peringatan",
              tint = HighVoltageRed,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "PERINGATAN ISOLASI 3.3V ESP32",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = HighVoltageRed
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "ESP32 adalah logika 3.3V murni. Pin Pulser (GPIO04) dan Sadapan OEM (GPIO16/17) WAJIB lewat modul optocoupler PC817. Selalu gunakan ADC1 (GPIO32-39) karena ADC2 dinonaktifkan oleh radio BLE saat koneksi telemetri aktif.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 14.sp),
                color = TextPrimaryDark
              )
            }
          }
        }
      } else {
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = ElectricCyan.copy(alpha = 0.08f)),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(ElectricCyan, MotecOrange))
          )
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = "STM32 Safety",
              tint = ElectricCyan,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "PANDUAN WEACT STM32WB55 (DUAL-CORE ARM CORTEX)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = ElectricCyan
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Header 35 Pin (15 H_TOP & 20 H_BOTTOM). Pulser hardware timer PA0 (TIM2_CH1), Output Gate Koil PA1/PA2, Suplai daya 5.00V wajib ke pin 5V (H_BOTTOM.2). Dilarang menyambungkan aki 12V langsung ke pin VBAT.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 14.sp),
                color = TextSecondaryDark
              )
            }
          }
        }
      }
    }

    // 4. Section Header & Filter Chips
    item {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.FilterList,
              contentDescription = null,
              tint = if (activePlatform == McuPlatform.STM32WB55) ElectricCyan else SparkAmber,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (activePlatform == McuPlatform.STM32WB55) "SPESIFIKASI PINOUT STM32" else "SPESIFIKASI PINOUT ESP32",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
              color = TextPrimaryDark
            )
          }

          Text(
            text = "${if (activePlatform == McuPlatform.STM32WB55) filteredWeActPins.size else filteredEsp32Pins.size} Pin Ditampilkan",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextTertiaryDark,
            fontFamily = FontFamily.Monospace
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontally Scrollable Filter Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(filterScrollState),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val filterOptions = if (activePlatform == McuPlatform.STM32WB55) {
            listOf("SEMUA", "PULSER", "GATE", "OEM_LEARN", "ADC/SENSOR", "CHARGER", "KRITIS", "H_TOP", "H_BOTTOM")
          } else {
            listOf("SEMUA", "PULSER", "GATE", "OEM_LEARN", "ADC/SENSOR", "CHARGER", "KRITIS", "KIRI", "KANAN")
          }

          filterOptions.forEach { filter ->
            val isSelected = selectedFilter == filter
            val activeColor = if (activePlatform == McuPlatform.ESP32_WROOM) SparkAmber else ElectricCyan

            MotecButton(
              text = filter,
              onClick = { selectedFilter = filter },
              color = if (isSelected) activeColor else TextMuted,
              height = 28.dp,
              fontSize = 9.5.sp
            )
          }
        }
      }
    }

    // 5. Pin Cards List
    if (activePlatform == McuPlatform.STM32WB55) {
      items(filteredWeActPins, key = { "${it.header}_${it.pinNumber}" }) { pin ->
        WeActPinCard(
          pin = pin,
          isSelected = pin.header == uiState.selectedWeActPin?.header && pin.pinNumber == uiState.selectedWeActPin?.pinNumber,
          onClick = { viewModel.selectWeActPin(pin) }
        )
      }
    } else {
      items(filteredEsp32Pins, key = { "${it.headerSide}_${it.pinNumber}" }) { pin ->
        Esp32PinCard(
          pin = pin,
          isSelected = pin.headerSide == uiState.selectedEsp32Pin?.headerSide && pin.pinNumber == uiState.selectedEsp32Pin?.pinNumber,
          onClick = { viewModel.selectEsp32Pin(pin) }
        )
      }
    }
  }
}

/**
 * Compact Platform Switcher Header
 * Menggantikan banner besar sebelumnya sehingga tidak ada gap ruang kosong berlebih.
 */
@Composable
fun PlatformSwitcherBanner(
  activePlatform: McuPlatform,
  onSelectPlatform: (McuPlatform) -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(10.dp),
    color = Color(0xFF0D141F),
    border = BorderStroke(1.dp, if (activePlatform == McuPlatform.STM32WB55) ElectricCyan.copy(alpha = 0.35f) else SparkAmber.copy(alpha = 0.35f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 7.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Memory,
          contentDescription = null,
          tint = if (activePlatform == McuPlatform.STM32WB55) ElectricCyan else SparkAmber,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (activePlatform == McuPlatform.STM32WB55) "PLATFORM: WEACT STM32WB55" else "PLATFORM: ESP32-WROOM-32D",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
          color = Color.White,
          fontFamily = FontFamily.Monospace
        )
      }

      // Compact Motec Platform Toggle Buttons
      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        val isStm = activePlatform == McuPlatform.STM32WB55
        MotecButton(
          text = "STM32 (35P)",
          onClick = { onSelectPlatform(McuPlatform.STM32WB55) },
          color = if (isStm) ElectricCyan else TextMuted,
          height = 26.dp,
          fontSize = 9.sp
        )

        val isEsp = activePlatform == McuPlatform.ESP32_WROOM
        MotecButton(
          text = "ESP32 (38P)",
          onClick = { onSelectPlatform(McuPlatform.ESP32_WROOM) },
          color = if (isEsp) SparkAmber else TextMuted,
          height = 26.dp,
          fontSize = 9.sp
        )
      }
    }
  }
}

@Composable
fun Esp32PinCard(
  pin: Esp32Pin,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val isCritical = pin.isCritical || pin.warning != null

  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isCritical) Color(0xFF241414) else if (isSelected) Color(0xFF221E14) else TechSurfaceElevated
    ),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.linearGradient(
        listOf(
          if (isCritical) HighVoltageRed else if (isSelected) SparkAmber else OutlineDark,
          if (isCritical) HighVoltageOrange else if (isSelected) SparkAmberMuted else OutlineDark
        )
      )
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isCritical) HighVoltageRed.copy(alpha = 0.25f) else SparkAmber.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, if (isCritical) HighVoltageRed else SparkAmber)
          ) {
            Text(
              text = "${pin.headerSide} • Pin ${pin.pinNumber}",
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
              color = if (isCritical) HighVoltageRed else SparkAmber,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Text(
            text = pin.name,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimaryDark,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }

        Surface(
          shape = RoundedCornerShape(4.dp),
          color = when (pin.direction) {
            "Input" -> SparkAmber.copy(alpha = 0.15f)
            "Output" -> RacingLime.copy(alpha = 0.15f)
            else -> Color(0xFF475569).copy(alpha = 0.2f)
          }
        ) {
          Text(
            text = pin.direction,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
            color = when (pin.direction) {
              "Input" -> SparkAmber
              "Output" -> RacingLime
              else -> Color(0xFF94A3B8)
            },
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Ekuivalen STM32 & Net Tujuan
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(4.dp))
          .background(Color(0xFF101622))
          .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Ekuivalen STM32: ",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = TextTertiaryDark
          )
          Text(
            text = pin.stmEquivalent,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = ElectricCyan,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
        Text(
          text = pin.status,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
          color = if (pin.status.contains("AKTIF")) RacingLime else TextTertiaryDark,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Fungsi: ${pin.finalDestination}",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
        color = TextPrimaryDark
      )

      Text(
        text = "Jalur: ${pin.fullPath}",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
        color = TextSecondaryDark,
        fontFamily = FontFamily.Monospace
      )

      // Warning Box
      if (pin.warning != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Card(
          shape = RoundedCornerShape(6.dp),
          colors = CardDefaults.cardColors(containerColor = HighVoltageRed.copy(alpha = 0.15f)),
          border = BorderStroke(1.dp, HighVoltageRed.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = HighVoltageRed,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = pin.warning,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = HighVoltageRed,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }
  }
}

@Composable
fun WeActPinCard(
  pin: WeActPin,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val isVbatDanger = pin.warning?.contains("DILARANG", ignoreCase = true) == true

  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isVbatDanger) Color(0xFF261010) else if (isSelected) Color(0xFF122030) else TechSurfaceElevated
    ),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.linearGradient(
        listOf(
          if (isVbatDanger) HighVoltageRed else if (isSelected) ElectricCyan else OutlineDark,
          if (isVbatDanger) HighVoltageOrange else if (isSelected) ElectricCyanMuted else OutlineDark
        )
      )
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isVbatDanger) HighVoltageRed.copy(alpha = 0.25f) else ElectricCyan.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, if (isVbatDanger) HighVoltageRed else ElectricCyan)
          ) {
            Text(
              text = "${pin.header} • Pin ${pin.pinNumber}",
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
              color = if (isVbatDanger) HighVoltageRed else ElectricCyan,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Text(
            text = pin.name,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimaryDark,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }

        Surface(
          shape = RoundedCornerShape(4.dp),
          color = when (pin.direction) {
            "Input", "INPUT", "INPUT (ANALOG)" -> SparkAmber.copy(alpha = 0.15f)
            "Output", "OUTPUT", "OUTPUT (PWM)" -> SafetyGreen.copy(alpha = 0.15f)
            "POWER" -> HighVoltageRed.copy(alpha = 0.15f)
            "GND" -> GroundStarGold.copy(alpha = 0.15f)
            else -> Color(0xFF475569).copy(alpha = 0.2f)
          }
        ) {
          Text(
            text = pin.direction,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
            color = when (pin.direction) {
              "Input", "INPUT", "INPUT (ANALOG)" -> SparkAmber
              "Output", "OUTPUT", "OUTPUT (PWM)" -> SafetyGreen
              "POWER" -> HighVoltageRed
              "GND" -> GroundStarGold
              else -> Color(0xFF94A3B8)
            },
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Status & Fungsi
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(4.dp))
          .background(Color(0xFF101622))
          .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Fungsi: ${pin.finalDestination}",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
          color = TextPrimaryDark,
          modifier = Modifier.weight(1f)
        )
        Text(
          text = pin.status,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
          color = if (pin.status.contains("AKTIF")) RacingLime else TextTertiaryDark,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Jalur Sirkuit: ${pin.fullPath}",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
        color = TextSecondaryDark,
        fontFamily = FontFamily.Monospace
      )

      if (pin.warning != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Card(
          shape = RoundedCornerShape(6.dp),
          colors = CardDefaults.cardColors(containerColor = HighVoltageRed.copy(alpha = 0.15f)),
          border = BorderStroke(1.dp, HighVoltageRed.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = HighVoltageRed,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = pin.warning,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = HighVoltageRed,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }
  }
}
