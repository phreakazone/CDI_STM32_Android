package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
  modifier: Modifier = Modifier
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
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
  ) {
    item {
      // Interactive Visualizer Socket
      HarnessJ1Visualizer(
        pins = allPins,
        selectedPinNumber = uiState.selectedHarnessPin?.pinNumber,
        onSelectPin = { pin ->
          viewModel.selectHarnessPin(pin)
        }
      )
    }

    item {
      // Search Box
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Cari Pin (contoh: 12, Coil, Brown, GND, Pulser)") },
        leadingIcon = {
          Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = ElectricCyan)
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = TextSecondaryDark)
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
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      )
    }

    item {
      Text(
        text = "DAFTAR DETAIL 12 PIN KONEKTOR HARNESS CDI NS200",
        style = MaterialTheme.typography.labelMedium,
        color = ElectricCyan,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
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

  // Map harness pin to relevant tutorial step
  val relatedStepNumber = when (pin.pinNumber) {
    12 -> "1.1" // Pin 12 Primary Coil Center
    5 -> "1.2" // Pin 5 +12V Switched Ignition
    11 -> "1.3" // Pin 11 Power Ground Star
    10 -> "2.1" // Pin 10 Pulser + Signal
    2 -> "2.4" // Pin 2 TPS Signal
    4 -> "2.5" // Pin 4 TPS 5V Supply
    3 -> "2.6" // Pin 3 Engine Temp Sensor
    7 -> "3.4" // Pin 7 Radiator Fan Driver
    6 -> "3.2" // Pin 6 Side Plugs Coil
    else -> null
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF132338) else TechSurfaceElevated),
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(borderColor, borderColor.copy(alpha = 0.5f)))),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (pin.pinNumber == 12 || pin.pinNumber == 5) SparkAmber.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.2f),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.linearGradient(listOf(SparkAmber, SparkAmberDark))
            )
          ) {
            Text(
              text = "PIN ${pin.pinNumber}",
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              style = MaterialTheme.typography.labelSmall,
              color = if (pin.pinNumber == 12 || pin.pinNumber == 5) SparkAmber else ElectricCyan,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = pin.name,
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimaryDark,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Warna Kabel: ${pin.wireColor}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = SparkAmber
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = when (pin.status) {
            "DIGUNAKAN" -> SafetyGreen.copy(alpha = 0.15f)
            "KOSONG (NC)" -> Color(0xFF475569).copy(alpha = 0.2f)
            else -> ElectricCyan.copy(alpha = 0.15f)
          }
        ) {
          Text(
            text = pin.status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = when (pin.status) {
              "DIGUNAKAN" -> SafetyGreen
              "KOSONG (NC)" -> Color(0xFF94A3B8)
              else -> ElectricCyan
            },
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Arah & Fungsi: ${pin.direction}",
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
        color = TextPrimaryDark
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Destination Net
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF0D1520))
          .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Tujuan di PCB: ",
          style = MaterialTheme.typography.labelSmall,
          color = TextTertiaryDark
        )
        Text(
          text = pin.destination,
          style = MaterialTheme.typography.labelSmall,
          color = SafetyGreen,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Complete Path
      Text(
        text = "Rangkaian Jalur: ${pin.completePath}",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = TextSecondaryDark
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Detail Guide
      Text(
        text = "Panduan Pemasangan: ${pin.detailGuide}",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = TextTertiaryDark
      )

      // Jump to Tutorial button if related step exists
      if (relatedStepNumber != null) {
        Spacer(modifier = Modifier.height(8.dp))
        id.ns200.cdir7.ui.components.MotecButton(
          text = "BUKA TUTORIAL LANGKAH $relatedStepNumber",
          onClick = { onNavigateToStep(relatedStepNumber) },
          color = SparkAmber,
          icon = Icons.AutoMirrored.Filled.ArrowForward,
          height = 30.dp,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}
