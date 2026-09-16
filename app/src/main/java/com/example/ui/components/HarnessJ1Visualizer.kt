package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HarnessPin
import com.example.ui.theme.*

@Composable
fun HarnessJ1Visualizer(
  pins: List<HarnessPin>,
  selectedPinNumber: Int?,
  onSelectPin: (HarnessPin) -> Unit,
  modifier: Modifier = Modifier
) {
  val topRowPins = pins.filter { it.pinNumber in 1..6 }.sortedBy { it.pinNumber }
  val bottomRowPins = pins.filter { it.pinNumber in 7..12 }.sortedBy { it.pinNumber }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = TechSurfaceElevated),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.linearGradient(listOf(OutlineDark, ElectricCyan.copy(alpha = 0.4f)))
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "SOKET HARNESS CDI J1 (12 PIN)",
            style = MaterialTheme.typography.labelMedium,
            color = ElectricCyan,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "Tampak depan konektor pigtail (Klip kait/latch di atas)",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
            color = TextSecondaryDark
          )
        }

        Surface(
          shape = RoundedCornerShape(4.dp),
          color = SparkAmber.copy(alpha = 0.15f),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(SparkAmber, SparkAmberDark))
          )
        ) {
          Text(
            text = "12 PIN OK",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = SparkAmber,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Outer Socket Graphic Housing - Auto-fitting and responsive
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF0C131D))
          .border(1.dp, Color(0xFF22354A), RoundedCornerShape(8.dp))
          .padding(horizontal = 4.dp, vertical = 6.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Top Latch Graphic
          Box(
            modifier = Modifier
              .width(64.dp)
              .height(7.dp)
              .background(SparkAmber.copy(alpha = 0.75f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
              .border(0.8.dp, SparkAmber, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "KLIP ATAS",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
              color = Color.Black,
              fontWeight = FontWeight.Black
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Top Row (Pins 1 - 6) using weights to prevent overflow
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            topRowPins.forEach { pin ->
              Box(modifier = Modifier.weight(1f)) {
                PinSocketItem(
                  pin = pin,
                  isSelected = pin.pinNumber == selectedPinNumber,
                  onClick = { onSelectPin(pin) }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Bottom Row (Pins 7 - 12) using weights to prevent overflow
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            bottomRowPins.forEach { pin ->
              Box(modifier = Modifier.weight(1f)) {
                PinSocketItem(
                  pin = pin,
                  isSelected = pin.pinNumber == selectedPinNumber,
                  onClick = { onSelectPin(pin) }
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Compact Orientation Guidance Note
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = SparkAmber,
            modifier = Modifier.size(4.dp)
          ) {}
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Baris 1: Pin 1–6 (Kiri ke Kanan) | Baris 2: Pin 7–12 (Kiri ke Kanan)",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp),
            color = TextTertiaryDark
          )
        }
        Text(
          text = "Pilih pin untuk melihat detail",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp),
          color = ElectricCyan
        )
      }
    }
  }
}

@Composable
private fun PinSocketItem(
  pin: HarnessPin,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val pinColor = when (pin.pinNumber) {
    1 -> Color(0xFF475569) // NC
    5 -> Color(0xFFE65100) // +12V kontak (Cokelat)
    11 -> Color(0xFFFFD600) // GND (Kuning Hitam)
    10 -> Color(0xFF00E5FF) // Pulser (Cyan)
    12 -> Color(0xFFFF9100) // Coil Center (Orange)
    6 -> Color(0xFFFF5252) // Coil Side (Merah Hitam)
    7 -> Color(0xFF448AFF) // Fan (Biru Kuning)
    2, 4 -> Color(0xFF69F0AE) // TPS (Hijau Putih / Abu)
    3 -> Color(0xFFB388FF) // Temp (Hitam Putih)
    8, 9 -> Color(0xFF818CF8) // OEM Learn Probe
    else -> ElectricCyan
  }

  val functionLabel = when (pin.pinNumber) {
    1 -> "NC"
    2 -> "TPS A"
    3 -> "TEMP"
    4 -> "TPS B"
    5 -> "+12V"
    6 -> "SIDE"
    7 -> "FAN"
    8 -> "OEM S"
    9 -> "OEM C"
    10 -> "PULS"
    11 -> "GND"
    12 -> "CTR"
    else -> "${pin.pinNumber}"
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(4.dp))
      .clickable { onClick() }
      .padding(vertical = 1.dp)
  ) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .background(
          if (isSelected) pinColor.copy(alpha = 0.35f) else Color(0xFF131D2A)
        )
        .border(
          width = if (isSelected) 2.dp else 1.dp,
          color = if (isSelected) ElectricCyan else pinColor.copy(alpha = 0.75f),
          shape = CircleShape
        ),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(pinColor)
      )
      Text(
        text = "${pin.pinNumber}",
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 8.sp,
          fontWeight = FontWeight.Black
        ),
        color = if (pinColor == Color(0xFFFFD600) || pinColor == Color(0xFF69F0AE)) Color.Black else Color.White
      )
    }

    Spacer(modifier = Modifier.height(1.dp))

    Text(
      text = functionLabel,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 7.5.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        fontFamily = FontFamily.Monospace
      ),
      color = if (isSelected) ElectricCyan else TextSecondaryDark,
      textAlign = TextAlign.Center,
      maxLines = 1
    )
  }
}

