package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import com.example.model.HarnessPin
import com.example.model.adaptToPlatform
import com.example.ui.components.HarnessJ1Visualizer
import com.example.ui.theme.*
import com.example.viewmodel.AppTab
import com.example.viewmodel.WiringViewModel

@Composable
fun HarnessPinsScreen(
  viewModel: WiringViewModel,
  modifier: Modifier = Modifier,
  onNavigateToTutorial: ((String) -> Unit)? = null
) {
  val uiState by viewModel.uiState.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  val allPins = remember(uiState.mcuPlatform) {
    WiringDataProvider.harnessPins.map { it.adaptToPlatform(uiState.mcuPlatform) }
  }

  val filteredPins = remember(searchQuery, allPins) {
    if (searchQuery.isBlank()) allPins
    else {
      val query = searchQuery.lowercase()
      allPins.filter {
        it.pinNumber.toString() == query ||
          it.name.lowercase().contains(query) ||
          it.wireColor.lowercase().contains(query) ||
          it.direction.lowercase().contains(query) ||
          it.destination.lowercase().contains(query) ||
          it.completePath.lowercase().contains(query)
      }
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(TechDarkBg)
      .padding(horizontal = 12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
  ) {
    item {
      // Interactive Visualizer Socket (Compact)
      HarnessJ1Visualizer(
        pins = allPins,
        selectedPinNumber = uiState.selectedHarnessPin?.pinNumber,
        onSelectPin = { pin ->
          viewModel.selectHarnessPin(pin)
        }
      )
    }

    item {
      // Search Box (Compact)
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Cari Pin J1 (contoh: 12, Koil, 12V, GND, Pulser)", fontSize = 11.sp) },
        leadingIcon = {
          Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
            }
          }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = ElectricCyan,
          unfocusedBorderColor = OutlineDark,
          focusedTextColor = TextPrimaryDark,
          unfocusedTextColor = TextPrimaryDark,
          focusedContainerColor = TechSurfaceElevated,
          unfocusedContainerColor = TechSurfaceElevated
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "DAFTAR 12 PIN SOKET HARNESS CDI J1",
          style = MaterialTheme.typography.labelSmall,
          color = ElectricCyan,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = "${filteredPins.size} PIN DITEMUKAN",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
          color = TextTertiaryDark,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    items(filteredPins, key = { it.pinNumber }) { pin ->
      HarnessPinDetailCard(
        pin = pin,
        isSelected = pin.pinNumber == uiState.selectedHarnessPin?.pinNumber,
        onNavigateToStep = { stepNumber ->
          val stepIndex = viewModel.allSteps.indexOfFirst { it.stepNumber == stepNumber }
          if (stepIndex >= 0) {
            viewModel.selectStep(stepIndex)
            viewModel.setTab(AppTab.TUTORIAL)
          }
          onNavigateToTutorial?.invoke(stepNumber)
        },
        onClick = { viewModel.selectHarnessPin(pin) }
      )
    }
  }
}

@Composable
fun HarnessPinDetailCard(
  pin: HarnessPin,
  isSelected: Boolean,
  onNavigateToStep: (String) -> Unit,
  onClick: () -> Unit
) {
  val borderColor = if (isSelected) ElectricCyan else OutlineDark

  // Precise mapping of Bajaj Pulsar NS200 J1 Pins to Wiring Workshop Steps
  val (relatedStepNumber, relatedStepTitle) = when (pin.pinNumber) {
    1  -> Pair("6.1", "Inspeksi Pigtail J1 & Uji Isolasi Multimeter")
    2  -> Pair("2.4", "Header J_TPS & ADC Gas")
    3  -> Pair("2.3", "Sensor Suhu Coolant NTC (PA4)")
    4  -> Pair("2.4", "Header J_TPS & ADC Gas")
    5  -> Pair("1.2", "Proteksi Input Catu Daya 12V")
    6  -> Pair("5.2", "Kapasitor Pulsa Koil Side C_SIDE")
    7  -> Pair("2.6", "Driver Relay Fan Radiator (PB5)")
    8  -> Pair("2.2", "Sinyal OEM Learn Side (PB4)")
    9  -> Pair("2.2", "Sinyal OEM Learn Center (PB3)")
    10 -> Pair("2.5", "Komparator Pulser LM339 (PA0)")
    11 -> Pair("1.1", "Ground Bintang GND_STAR")
    12 -> Pair("5.1", "Kapasitor Pulsa Koil Center C_CENTER")
    else -> Pair("6.1", "Inspeksi Soket Harness J1")
  }

  Card(
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF132338) else TechSurfaceElevated),
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(borderColor, borderColor.copy(alpha = 0.5f)))),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      // Header: PIN number, Wire color, Status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (pin.pinNumber in listOf(5, 6, 12)) SparkAmber.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.2f),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.linearGradient(listOf(SparkAmber, SparkAmberDark))
            )
          ) {
            Text(
              text = "PIN ${pin.pinNumber}",
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
              color = if (pin.pinNumber in listOf(5, 6, 12)) SparkAmber else ElectricCyan,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          Column {
            Text(
              text = pin.name,
              style = MaterialTheme.typography.titleSmall.copy(fontSize = 11.5.sp),
              color = TextPrimaryDark,
              fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(4.dp)
                  .background(SparkAmber, CircleShape)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = pin.wireColor,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = SparkAmber
              )
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(3.dp),
          color = when (pin.status) {
            "DIGUNAKAN" -> SafetyGreen.copy(alpha = 0.15f)
            "KOSONG (NC)" -> Color(0xFF475569).copy(alpha = 0.2f)
            else -> ElectricCyan.copy(alpha = 0.15f)
          }
        ) {
          Text(
            text = pin.status,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = when (pin.status) {
              "DIGUNAKAN" -> SafetyGreen
              "KOSONG (NC)" -> Color(0xFF94A3B8)
              else -> ElectricCyan
            },
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Compact Destination & Circuit Net Box
      Surface(
        shape = RoundedCornerShape(5.dp),
        color = Color(0xFF0D1520),
        border = BorderStroke(0.5.dp, Color(0xFF1F2E40)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(5.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Tujuan PCB: ",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                color = TextTertiaryDark
              )
              Text(
                text = pin.destination,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = SafetyGreen,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }
            Text(
              text = pin.direction,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
              color = TextSecondaryDark
            )
          }

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "Jalur: ${pin.completePath}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp),
            color = TextSecondaryDark
          )

          Spacer(modifier = Modifier.height(1.5.dp))

          Text(
            text = "Panduan: ${pin.detailGuide}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
            color = TextTertiaryDark
          )
        }
      }

      // Action Button: Jump directly to connected step in tutorial
      Spacer(modifier = Modifier.height(5.dp))
      id.ns200.cdir7.ui.components.MotecButton(
        text = "BUKA TUTORIAL LANGKAH $relatedStepNumber: $relatedStepTitle",
        onClick = { onNavigateToStep(relatedStepNumber) },
        color = SparkAmber,
        icon = Icons.AutoMirrored.Filled.ArrowForward,
        height = 26.dp,
        fontSize = 8.5.sp,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}
