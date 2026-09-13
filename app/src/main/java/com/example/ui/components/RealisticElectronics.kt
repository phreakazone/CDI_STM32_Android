package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * High-fidelity realistic vector rendering of the WeAct Studio STM32WB55CGU6 board.
 * Exactly replicates the physical board layout:
 * - Type-C USB metal shell on left
 * - Gold through-hole header pins on top (15) and bottom (20)
 * - NRST and BOOT0 buttons
 * - 4-pin SWD/DEBUG header
 * - STM32WB55 QFN MCU chip
 * - E4 User LED
 * - 2.4GHz gold meandering PCB antenna on far right
 */
@Composable
fun RealisticWeActBoard(
  modifier: Modifier = Modifier,
  activePins: Set<String> = emptySet(),
  highlightedPin: String? = null,
  onPinClick: ((String) -> Unit)? = null
) {
  val topPins = listOf("G", "G", "3V3", "3V3", "B7", "B6", "B5", "B4", "B3", "A15", "A10", "E4", "B1", "B0", "G")
  val bottomPins = listOf("G", "5V", "5V", "VB", "H3", "B9", "B8", "NR", "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "B2", "G")

  val pulseAnim = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by pulseAnim.animateFloat(
    initialValue = 0.4f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow"
  )

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = Color(0xFF141913), // WeAct dark olive-black PCB
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.linearGradient(listOf(Color(0xFF2C3827), Color(0xFF182216)))
    ),
    shadowElevation = 6.dp,
    modifier = modifier
      .width(520.dp)
      .height(200.dp)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Background PCB silkscreen & antenna copper area
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. USB-C metallic receptacle on the left
        drawRoundRect(
          color = Color(0xFFCFD8DC),
          topLeft = Offset(0f, h * 0.30f),
          size = Size(42f, h * 0.40f),
          cornerRadius = CornerRadius(6f, 6f)
        )
        // USB inner black slot
        drawRoundRect(
          color = Color(0xFF263238),
          topLeft = Offset(8f, h * 0.36f),
          size = Size(28f, h * 0.28f),
          cornerRadius = CornerRadius(3f, 3f)
        )
        // USB gold pins inside
        for (i in 0..4) {
          drawLine(
            color = Color(0xFFFFD54F),
            start = Offset(14f + i * 4f, h * 0.40f),
            end = Offset(14f + i * 4f, h * 0.60f),
            strokeWidth = 1.8f
          )
        }

        // 2. 2.4GHz Antenna Section on the far right (Gold / FR4 substrate)
        val antLeft = w - 62f
        drawRect(
          color = Color(0xFF7D7242), // FR4 bare laminate color
          topLeft = Offset(antLeft, 0f),
          size = Size(62f, h)
        )
        // Antenna gold meandering serpentine trace
        val goldColor = Color(0xFFFFE082)
        val path = Path().apply {
          moveTo(antLeft + 10f, h * 0.2f)
          lineTo(antLeft + 24f, h * 0.2f)
          lineTo(antLeft + 24f, h * 0.8f)
          lineTo(antLeft + 36f, h * 0.8f)
          lineTo(antLeft + 36f, h * 0.25f)
          lineTo(antLeft + 48f, h * 0.25f)
          lineTo(antLeft + 48f, h * 0.75f)
        }
        drawPath(path, goldColor, style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Gold trace feeder to RF matching network
        drawLine(goldColor, Offset(antLeft - 18f, h * 0.48f), Offset(antLeft + 10f, h * 0.48f), strokeWidth = 2.5f)

        // 3. Central STM32WB55 QFN IC Chip
        val chipCenterX = w * 0.68f
        val chipCenterY = h * 0.50f
        val chipSize = 58f

        // QFN chip body (matte dark grey)
        drawRoundRect(
          color = Color(0xFF202326),
          topLeft = Offset(chipCenterX - chipSize / 2, chipCenterY - chipSize / 2),
          size = Size(chipSize, chipSize),
          cornerRadius = CornerRadius(4f, 4f)
        )
        // Pin 1 dot mark
        drawCircle(
          color = Color(0xFF455A64),
          radius = 3.5f,
          center = Offset(chipCenterX - chipSize / 2 + 7f, chipCenterY - chipSize / 2 + 7f)
        )
        // Silkscreen on chip: STM32WB55
        // Fine gold pin leads around QFN package
        for (i in -2..2) {
          // Top leads
          drawLine(goldColor, Offset(chipCenterX + i * 9f, chipCenterY - chipSize / 2 - 5f), Offset(chipCenterX + i * 9f, chipCenterY - chipSize / 2), strokeWidth = 1.5f)
          // Bottom leads
          drawLine(goldColor, Offset(chipCenterX + i * 9f, chipCenterY + chipSize / 2), Offset(chipCenterX + i * 9f, chipCenterY + chipSize / 2 + 5f), strokeWidth = 1.5f)
          // Left leads
          drawLine(goldColor, Offset(chipCenterX - chipSize / 2 - 5f, chipCenterY + i * 9f), Offset(chipCenterX - chipSize / 2, chipCenterY + i * 9f), strokeWidth = 1.5f)
          // Right leads
          drawLine(goldColor, Offset(chipCenterX + chipSize / 2, chipCenterY + i * 9f), Offset(chipCenterX + chipSize / 2 + 5f, chipCenterY + i * 9f), strokeWidth = 1.5f)
        }

        // 4. NRST & BOOT0 tactile push buttons
        val btnX = w * 0.43f
        // NRST Button (top)
        drawRoundRect(
          color = Color(0xFFCFD8DC),
          topLeft = Offset(btnX - 12f, h * 0.28f),
          size = Size(24f, 20f),
          cornerRadius = CornerRadius(2f, 2f)
        )
        drawCircle(Color(0xFF263238), radius = 6f, center = Offset(btnX, h * 0.28f + 10f))

        // BOOT0 Button (bottom)
        drawRoundRect(
          color = Color(0xFFCFD8DC),
          topLeft = Offset(btnX - 12f, h * 0.55f),
          size = Size(24f, 20f),
          cornerRadius = CornerRadius(2f, 2f)
        )
        drawCircle(Color(0xFF263238), radius = 6f, center = Offset(btnX, h * 0.55f + 10f))

        // 5. 4-Pin SWD / DEBUG header (vertical)
        val dbgX = w * 0.54f
        drawRoundRect(
          color = Color(0xFF101418),
          topLeft = Offset(dbgX - 8f, h * 0.26f),
          size = Size(16f, 66f),
          cornerRadius = CornerRadius(3f, 3f)
        )
        for (i in 0..3) {
          drawCircle(Color(0xFFFFD54F), radius = 3f, center = Offset(dbgX, h * 0.32f + i * 16f))
          drawCircle(Color(0xFF101418), radius = 1.5f, center = Offset(dbgX, h * 0.32f + i * 16f))
        }

        // 6. User LED E4 (blue LED)
        drawRoundRect(
          color = Color(0xFF1E88E5),
          topLeft = Offset(w * 0.59f, h * 0.26f),
          size = Size(8f, 12f),
          cornerRadius = CornerRadius(2f, 2f)
        )

        // 7. Crystal oscillator metal can
        drawRoundRect(
          color = Color(0xFFB0BEC5),
          topLeft = Offset(w * 0.57f, h * 0.65f),
          size = Size(14f, 20f),
          cornerRadius = CornerRadius(3f, 3f)
        )
      }

      // CENTER SILK LABELS (NRST, BOOT0, DEBUG, STM32WB55 - Middle band)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.Center)
          .padding(start = 54.dp, end = 74.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "WeAct\nStudio",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, lineHeight = 10.sp),
          color = Color(0xFF81C784),
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.width(44.dp)
        )

        Spacer(modifier = Modifier.width(18.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("NRST", fontSize = 6.5.sp, color = Color(0xFFECEFF1), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          Spacer(modifier = Modifier.height(20.dp))
          Text("BOOT0", fontSize = 6.5.sp, color = Color(0xFFECEFF1), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.width(18.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("DEBUG", fontSize = 6.5.sp, color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          Text("3V3\nDIO\nCLK\nG", fontSize = 5.5.sp, lineHeight = 8.sp, color = Color(0xFFB0BEC5), fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("STM32WB55", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
          Text("BLE 5.4 / 64MHz", fontSize = 6.sp, color = Color(0xFF90CAF9), fontFamily = FontFamily.Monospace)
        }
      }

      // TOP HEADER PINS (15 Pins - Docked securely to TopStart)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopStart)
          .padding(start = 48.dp, end = 68.dp, top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        topPins.forEachIndexed { index, pinName ->
          val pinId = "H_TOP.${index + 1}"
          val isActive = activePins.contains(pinName) || activePins.contains(pinId)
          val isHighlighted = highlightedPin == pinName || highlightedPin == pinId

          WeActPadPin(
            pinName = pinName,
            pinIndex = index + 1,
            isActive = isActive,
            isHighlighted = isHighlighted,
            pulseAlpha = if (isActive || isHighlighted) pulseAlpha else 1f,
            isTopPin = true,
            onClick = { onPinClick?.invoke(pinId) }
          )
        }
      }

      // BOTTOM HEADER PINS (20 Pins - Docked securely to BottomStart)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomStart)
          .padding(start = 48.dp, end = 68.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        bottomPins.forEachIndexed { index, pinName ->
          val pinId = "H_BOTTOM.${index + 1}"
          val isActive = activePins.contains(pinName) || activePins.contains(pinId)
          val isHighlighted = highlightedPin == pinName || highlightedPin == pinId
          val isDanger = pinName == "VB" // VBAT 12V danger

          WeActPadPin(
            pinName = pinName,
            pinIndex = index + 1,
            isActive = isActive,
            isHighlighted = isHighlighted,
            isDanger = isDanger,
            pulseAlpha = if (isActive || isHighlighted) pulseAlpha else 1f,
            isTopPin = false,
            onClick = { onPinClick?.invoke(pinId) }
          )
        }
      }

      // Antenna label on far right
      Text(
        text = "BLE\nANT",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 10.sp),
        color = Color(0xFF3E2723),
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 4.dp)
      )
    }
  }
}

@Composable
private fun WeActPadPin(
  pinName: String,
  pinIndex: Int,
  isActive: Boolean,
  isHighlighted: Boolean,
  isDanger: Boolean = false,
  pulseAlpha: Float = 1f,
  isTopPin: Boolean = true,
  onClick: () -> Unit
) {
  val goldRing = Color(0xFFFFD54F)
  val activeColor = when {
    isDanger -> HighVoltageRed
    isHighlighted -> ElectricCyan
    isActive -> SafetyGreen
    else -> goldRing
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clickable(onClick = onClick)
      .padding(horizontal = 0.5.dp)
  ) {
    if (isTopPin) {
      // 1. Through-hole gold pad (closest to top outer edge)
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(13.dp)
          .background(
            if (isActive || isHighlighted) activeColor.copy(alpha = 0.25f * pulseAlpha) else Color.Transparent,
            CircleShape
          )
          .border(
            width = if (isActive || isHighlighted) 2.dp else 1.2.dp,
            color = activeColor.copy(alpha = if (isActive || isHighlighted) pulseAlpha else 0.9f),
            shape = CircleShape
          )
      ) {
        Box(
          modifier = Modifier
            .size(4.5.dp)
            .background(Color(0xFF0A0F0D), CircleShape)
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      // 2. Silkscreen Pin Name (facing inward towards center of board)
      Text(
        text = pinName,
        fontSize = 7.sp,
        lineHeight = 9.sp,
        maxLines = 1,
        fontWeight = if (isActive || isHighlighted || isDanger) FontWeight.Black else FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = when {
          isDanger -> HighVoltageRed
          isHighlighted -> ElectricCyan
          isActive -> SafetyGreen
          else -> Color(0xFFCFD8DC)
        }
      )
    } else {
      // 1. Silkscreen Pin Name (facing inward towards center of board)
      Text(
        text = pinName,
        fontSize = 7.sp,
        lineHeight = 9.sp,
        maxLines = 1,
        fontWeight = if (isActive || isHighlighted || isDanger) FontWeight.Black else FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = when {
          isDanger -> HighVoltageRed
          isHighlighted -> ElectricCyan
          isActive -> SafetyGreen
          else -> Color(0xFFCFD8DC)
        }
      )

      Spacer(modifier = Modifier.height(2.dp))

      // 2. Through-hole gold pad (closest to bottom outer edge)
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(13.dp)
          .background(
            if (isActive || isHighlighted) activeColor.copy(alpha = 0.25f * pulseAlpha) else Color.Transparent,
            CircleShape
          )
          .border(
            width = if (isActive || isHighlighted) 2.dp else 1.2.dp,
            color = activeColor.copy(alpha = if (isActive || isHighlighted) pulseAlpha else 0.9f),
            shape = CircleShape
          )
      ) {
        Box(
          modifier = Modifier
            .size(4.5.dp)
            .background(Color(0xFF0A0F0D), CircleShape)
        )
      }
    }
  }
}

/**
 * RealisticEsp32Board:
 * Ultra-realistic rendering of the ESP32-WROOM-32 DevKit V1 board.
 * Matches dimensions (520dp x 175dp) of RealisticWeActBoard for seamless visual switching.
 * Includes:
 * - Micro-USB / USB-C interface & CP2102/CH340 chip
 * - Metal shielding can with ESP-WROOM-32 silkscreen
 * - PCB Inverted-F meander antenna on the right
 * - Tactile buttons for EN and BOOT (GPIO0)
 * - 30-pin dual-row headers with authentic GPIO labeling
 */
@Composable
fun RealisticEsp32Board(
  modifier: Modifier = Modifier,
  activePins: Set<String> = emptySet(),
  highlightedPin: String? = null,
  onPinClick: ((String) -> Unit)? = null
) {
  // Top Row (Left Header on DevKitC V4): 19 pins
  val topPins = listOf("3V3", "EN", "VP", "VN", "34", "35", "32", "33", "25", "26", "27", "14", "12", "GND", "13", "D2", "D3", "CMD", "5V")
  // Bottom Row (Right Header on DevKitC V4): 19 pins
  val bottomPins = listOf("GND", "23", "22", "TX", "RX", "21", "GND", "19", "18", "5", "17", "16", "4", "0", "2", "15", "D1", "D0", "CLK")

  val pulseAnim = rememberInfiniteTransition(label = "pulseEsp")
  val pulseAlpha by pulseAnim.animateFloat(
    initialValue = 0.4f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glowEsp"
  )

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = Color(0xFF0B1015), // ESP32 dark matte black PCB
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.linearGradient(listOf(SparkAmber.copy(alpha = 0.6f), Color(0xFF1E293B)))
    ),
    shadowElevation = 6.dp,
    modifier = modifier
      .width(580.dp)
      .height(200.dp)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Background PCB silkscreen & Metal RF Can
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Micro-USB Port on Left
        drawRoundRect(
          color = Color(0xFFB0BEC5),
          topLeft = Offset(0f, h * 0.32f),
          size = androidx.compose.ui.geometry.Size(36f, h * 0.36f),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
        )
        drawRoundRect(
          color = Color(0xFF263238),
          topLeft = Offset(6f, h * 0.38f),
          size = androidx.compose.ui.geometry.Size(24f, h * 0.24f),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )

        // 2. PCB Inverted-F Meander Antenna on Far Right
        val antLeft = w - 60f
        drawRect(
          color = Color(0xFF1A1208), // Exposed dark substrate
          topLeft = Offset(antLeft, 0f),
          size = androidx.compose.ui.geometry.Size(60f, h)
        )
        // Copper track
        val copperColor = Color(0xFFFFB74D)
        val path = Path().apply {
          moveTo(antLeft + 8f, h * 0.18f)
          lineTo(antLeft + 22f, h * 0.18f)
          lineTo(antLeft + 22f, h * 0.82f)
          lineTo(antLeft + 34f, h * 0.82f)
          lineTo(antLeft + 34f, h * 0.24f)
          lineTo(antLeft + 46f, h * 0.24f)
          lineTo(antLeft + 46f, h * 0.76f)
        }
        drawPath(path, copperColor, style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // 3. Central Metal Shielding Can (ESP-WROOM-32)
        val canLeft = w * 0.44f
        val canTop = h * 0.27f
        val canWidth = w * 0.34f
        val canHeight = h * 0.46f

        // Metal RF shield (Brushed aluminum)
        drawRoundRect(
          color = Color(0xFF37474F),
          topLeft = Offset(canLeft, canTop),
          size = androidx.compose.ui.geometry.Size(canWidth, canHeight),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )
        drawRoundRect(
          color = Color(0xFF78909C),
          topLeft = Offset(canLeft + 3f, canTop + 3f),
          size = androidx.compose.ui.geometry.Size(canWidth - 6f, canHeight - 6f),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )
        drawRoundRect(
          color = Color(0xFF263238),
          topLeft = Offset(canLeft + 6f, canTop + 6f),
          size = androidx.compose.ui.geometry.Size(canWidth - 12f, canHeight - 12f),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
        )

        // 4. Buttons (EN on top-left, BOOT on bottom-left)
        val btnX = w * 0.22f
        // EN Button (Top)
        drawRoundRect(
          color = Color(0xFFCFD8DC),
          topLeft = Offset(btnX - 10f, h * 0.32f),
          size = androidx.compose.ui.geometry.Size(20f, 18f),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
        )
        drawCircle(Color(0xFF37474F), radius = 5f, center = Offset(btnX, h * 0.32f + 9f))

        // BOOT Button (Bottom)
        drawRoundRect(
          color = Color(0xFFCFD8DC),
          topLeft = Offset(btnX - 10f, h * 0.54f),
          size = androidx.compose.ui.geometry.Size(20f, 18f),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
        )
        drawCircle(Color(0xFF37474F), radius = 5f, center = Offset(btnX, h * 0.54f + 9f))

        // 5. USB-to-UART Bridge Chip (CP2102)
        val cpX = w * 0.33f
        val cpY = h * 0.42f
        drawRoundRect(
          color = Color(0xFF1E293B),
          topLeft = Offset(cpX, cpY),
          size = androidx.compose.ui.geometry.Size(28f, 28f),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
        )
      }

      // CENTER SILK LABELS (EN, BOOT, CP2102, ESP-WROOM-32 - strictly middle band)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.Center)
          .padding(start = 50.dp, end = 70.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("EN", fontSize = 6.5.sp, color = Color(0xFFECEFF1), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          Spacer(modifier = Modifier.height(20.dp))
          Text("BOOT", fontSize = 6.5.sp, color = Color(0xFFECEFF1), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.width(36.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("UART", fontSize = 6.5.sp, color = SparkAmber, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          Text("CP2102", fontSize = 6.sp, color = Color(0xFF90A4AE), fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("ESP-WROOM-32", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
          Text("240MHz • BLE 4.2", fontSize = 6.sp, color = SparkAmber, fontFamily = FontFamily.Monospace)
          Text("3.3V Logic (ADC1)", fontSize = 5.5.sp, color = Color(0xFF81C784), fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.width(12.dp))
      }

      // TOP HEADER PINS (15 Pins - Docked securely to TopStart)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopStart)
          .padding(start = 44.dp, end = 64.dp, top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        topPins.forEachIndexed { index, pinName ->
          val pinId = "LEFT.${index + 1}"
          val isMatch = activePins.contains(pinName) ||
              activePins.contains("GPIO$pinName") ||
              activePins.contains(pinId) ||
              (pinName == "VP" && (activePins.contains("36") || activePins.contains("GPIO36"))) ||
              (pinName == "VN" && (activePins.contains("39") || activePins.contains("GPIO39")))
          val isHigh = highlightedPin == pinName ||
              highlightedPin == "GPIO$pinName" ||
              highlightedPin == pinId ||
              (pinName == "VP" && (highlightedPin == "36" || highlightedPin == "GPIO36"))
          val isDanger = pinName in listOf("25", "26", "35", "32")

          WeActPadPin(
            pinName = pinName,
            pinIndex = index + 1,
            isActive = isMatch,
            isHighlighted = isHigh,
            isDanger = isDanger,
            pulseAlpha = if (isMatch || isHigh) pulseAlpha else 1f,
            isTopPin = true,
            onClick = { onPinClick?.invoke(pinId) }
          )
        }
      }

      // BOTTOM HEADER PINS (15 Pins - Docked securely to BottomStart)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomStart)
          .padding(start = 44.dp, end = 64.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        bottomPins.forEachIndexed { index, pinName ->
          val pinId = "RIGHT.${index + 1}"
          val isMatch = activePins.contains(pinName) ||
              activePins.contains("GPIO$pinName") ||
              activePins.contains(pinId) ||
              (pinName == "4" && (activePins.contains("GPIO4") || activePins.contains("PULSER"))) ||
              (pinName == "16" && (activePins.contains("GPIO16") || activePins.contains("OEM_CTR"))) ||
              (pinName == "17" && (activePins.contains("GPIO17") || activePins.contains("OEM_SIDE"))) ||
              (pinName == "18" && (activePins.contains("GPIO18") || activePins.contains("CHARGER_A"))) ||
              (pinName == "19" && (activePins.contains("GPIO19") || activePins.contains("CHARGER_B")))
          val isHigh = highlightedPin == pinName ||
              highlightedPin == "GPIO$pinName" ||
              highlightedPin == pinId ||
              (pinName == "4" && highlightedPin == "GPIO4")
          val isDanger = pinName in listOf("VIN", "4", "16", "17")

          WeActPadPin(
            pinName = pinName,
            pinIndex = index + 1,
            isActive = isMatch,
            isHighlighted = isHigh,
            isDanger = isDanger,
            pulseAlpha = if (isMatch || isHigh) pulseAlpha else 1f,
            isTopPin = false,
            onClick = { onPinClick?.invoke(pinId) }
          )
        }
      }

      // Antenna label on far right
      Text(
        text = "BLE\nANT",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 10.sp),
        color = Color(0xFFFFB74D),
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 4.dp)
      )
    }
  }
}

/**
 * Realistic Dual In-Line Package (DIP) IC Chip (e.g. LM339 DIP-14, TC4427 DIP-8).
 */
@Composable
fun RealisticIcChip(
  ref: String,
  partNumber: String,
  pinCount: Int,
  modifier: Modifier = Modifier,
  activePins: Set<Int> = emptySet(),
  pinLabels: Map<Int, String> = emptyMap()
) {
  val halfCount = pinCount / 2

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
  ) {
    Text(
      text = ref,
      style = MaterialTheme.typography.labelSmall,
      color = SparkAmber,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )

    Spacer(modifier = Modifier.height(3.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      // Left Pins (1 .. halfCount)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..halfCount) {
          val isActive = activePins.contains(i)
          val label = pinLabels[i] ?: "$i"
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = label,
              fontSize = 7.sp,
              fontFamily = FontFamily.Monospace,
              color = if (isActive) ElectricCyan else TextSecondaryDark,
              fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
              modifier = Modifier.width(28.dp),
              textAlign = TextAlign.End
            )
            Spacer(modifier = Modifier.width(2.dp))
            // Silver metal lead
            Box(
              modifier = Modifier
                .width(8.dp)
                .height(3.dp)
                .background(if (isActive) ElectricCyan else Color(0xFFCFD8DC), RoundedCornerShape(1.dp))
            )
          }
        }
      }

      // Central IC Plastic Body
      Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF1E2228),
        shadowElevation = 4.dp,
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.linearGradient(listOf(Color(0xFF374151), Color(0xFF111827)))
        ),
        modifier = Modifier
          .width(52.dp)
          .height((halfCount * 14 + 16).dp)
      ) {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          // Top Notch (semi-circle cutout)
          Box(
            modifier = Modifier
              .width(14.dp)
              .height(6.dp)
              .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
              .background(Color(0xFF0F172A))
          )

          // Laser etched silkscreen
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = partNumber,
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              color = Color(0xFFE2E8F0)
            )
            Text(
              text = "DIP-$pinCount",
              fontSize = 6.sp,
              fontFamily = FontFamily.Monospace,
              color = Color(0xFF94A3B8)
            )
          }

          // Pin 1 dot indent indicator (bottom left relative to notch orientation)
          Box(
            modifier = Modifier
              .padding(bottom = 6.dp, start = 8.dp)
              .align(Alignment.Start)
              .size(4.dp)
              .background(Color(0xFF0F172A), CircleShape)
          )
        }
      }

      // Right Pins (pinCount down to halfCount + 1)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in pinCount downTo (halfCount + 1)) {
          val isActive = activePins.contains(i)
          val label = pinLabels[i] ?: "$i"
          Row(verticalAlignment = Alignment.CenterVertically) {
            // Silver metal lead
            Box(
              modifier = Modifier
                .width(8.dp)
                .height(3.dp)
                .background(if (isActive) ElectricCyan else Color(0xFFCFD8DC), RoundedCornerShape(1.dp))
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = label,
              fontSize = 7.sp,
              fontFamily = FontFamily.Monospace,
              color = if (isActive) ElectricCyan else TextSecondaryDark,
              fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
              modifier = Modifier.width(28.dp),
              textAlign = TextAlign.Start
            )
          }
        }
      }
    }
  }
}

/**
 * Realistic TO-220 Power Package (SCR BT151-600R, MOSFET IRF3205, Regulator L7805).
 * Features metallic heatsink tab, mounting hole, epoxy mold, and 3 distinct stamped leads.
 */
@Composable
fun RealisticTo220(
  ref: String,
  partName: String,
  pin1Label: String,
  pin2Label: String,
  pin3Label: String,
  modifier: Modifier = Modifier,
  isTabHighVoltage: Boolean = false,
  activePins: Set<Int> = emptySet()
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.width(72.dp)
  ) {
    Text(
      text = ref,
      style = MaterialTheme.typography.labelSmall,
      color = if (isTabHighVoltage) HighVoltageRed else SparkAmber,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )

    Spacer(modifier = Modifier.height(2.dp))

    // Metallic Heatsink Tab (Top)
    Surface(
      shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
      color = Color(0xFFCFD8DC),
      modifier = Modifier
        .width(46.dp)
        .height(16.dp)
        .border(1.dp, Color(0xFF90A4AE), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
    ) {
      Box(contentAlignment = Alignment.Center) {
        // Mounting hole
        Box(
          modifier = Modifier
            .size(8.dp)
            .background(Color(0xFF1E293B), CircleShape)
            .border(1.dp, Color(0xFF78909C), CircleShape)
        )
      }
    }

    // High Voltage Tab Warning if applicable
    if (isTabHighVoltage) {
      Surface(
        color = HighVoltageRed,
        shape = RoundedCornerShape(2.dp),
        modifier = Modifier.padding(vertical = 1.dp)
      ) {
        Text(
          text = "TAB=285V HV",
          fontSize = 6.sp,
          color = Color.White,
          fontWeight = FontWeight.Black,
          modifier = Modifier.padding(horizontal = 2.dp)
        )
      }
    }

    // Black Epoxy Molded Body
    Surface(
      shape = RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp),
      color = Color(0xFF212529),
      shadowElevation = 4.dp,
      modifier = Modifier
        .width(50.dp)
        .height(30.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(2.dp)
      ) {
        Text(
          text = partName,
          fontSize = 7.5.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          color = Color.White,
          textAlign = TextAlign.Center
        )
        Text(
          text = "TO-220",
          fontSize = 6.sp,
          color = Color(0xFF9E9E9E),
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // 3 Stamped Metal Leads extending downward
    Row(
      modifier = Modifier.width(46.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Pin 1 (Left)
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
          modifier = Modifier
            .width(4.dp)
            .height(16.dp)
            .background(if (activePins.contains(1)) ElectricCyan else Color(0xFFB0BEC5), RoundedCornerShape(1.dp))
        )
        Text(pin1Label, fontSize = 7.sp, color = if (activePins.contains(1)) ElectricCyan else TextSecondaryDark, fontWeight = FontWeight.Bold)
      }

      // Pin 2 (Center)
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
          modifier = Modifier
            .width(4.dp)
            .height(16.dp)
            .background(if (activePins.contains(2)) SparkAmber else Color(0xFFB0BEC5), RoundedCornerShape(1.dp))
        )
        Text(pin2Label, fontSize = 7.sp, color = if (activePins.contains(2)) SparkAmber else TextSecondaryDark, fontWeight = FontWeight.Bold)
      }

      // Pin 3 (Right)
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
          modifier = Modifier
            .width(4.dp)
            .height(16.dp)
            .background(if (activePins.contains(3)) SafetyGreen else Color(0xFFB0BEC5), RoundedCornerShape(1.dp))
        )
        Text(pin3Label, fontSize = 7.sp, color = if (activePins.contains(3)) SafetyGreen else TextSecondaryDark, fontWeight = FontWeight.Bold)
      }
    }
  }
}

/**
 * Realistic Diode (Power Schottky SB560, Ultrafast UF4007, or Glass 1N4148).
 */
@Composable
fun RealisticDiode(
  ref: String,
  partName: String,
  modifier: Modifier = Modifier,
  isGlass: Boolean = false,
  anodeLabel: String = "Anoda (A)",
  cathodeLabel: String = "Katoda (K)"
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
  ) {
    Text(
      text = ref,
      fontSize = 8.sp,
      color = SparkAmber,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )

    Spacer(modifier = Modifier.height(2.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
      // Anode Lead
      Box(
        modifier = Modifier
          .width(10.dp)
          .height(2.5.dp)
          .background(Color(0xFFCFD8DC))
      )

      // Diode Cylinder
      Surface(
        shape = RoundedCornerShape(3.dp),
        color = if (isGlass) Color(0xFFE65100) else Color(0xFF1E232A),
        shadowElevation = 3.dp,
        border = if (isGlass) BorderStroke(1.dp, Color(0xFFFFB74D)) else null,
        modifier = Modifier
          .width(36.dp)
          .height(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxSize(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Label on body
          Text(
            text = partName,
            fontSize = 6.sp,
            fontWeight = FontWeight.Bold,
            color = if (isGlass) Color.Black else Color.White,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 3.dp)
          )

          // Cathode Band Ring (Silver for plastic, Black for glass)
          Box(
            modifier = Modifier
              .width(5.dp)
              .fillMaxHeight()
              .background(if (isGlass) Color(0xFF111827) else Color(0xFFECEFF1))
          )
        }
      }

      // Cathode Lead
      Box(
        modifier = Modifier
          .width(10.dp)
          .height(2.5.dp)
          .background(Color(0xFFCFD8DC))
      )
    }

    // Pin labels
    Row(
      modifier = Modifier.width(56.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(anodeLabel, fontSize = 6.sp, color = TextTertiaryDark)
      Text(cathodeLabel, fontSize = 6.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
    }
  }
}

/**
 * Realistic MKP Box Film Pulse Capacitor (1.0µF 630V DC).
 */
@Composable
fun RealisticMkpCapacitor(
  ref: String,
  spec: String = "1.0uF 630V",
  color: Color = Color(0xFFB71C1C), // Rich Polypropylene Red
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
  ) {
    Text(
      text = ref,
      style = MaterialTheme.typography.labelSmall,
      color = SparkAmber,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )

    Spacer(modifier = Modifier.height(2.dp))

    // Box body
    Surface(
      shape = RoundedCornerShape(3.dp),
      color = color,
      shadowElevation = 5.dp,
      border = BorderStroke(1.dp, color.copy(alpha = 0.7f)),
      modifier = Modifier
        .width(58.dp)
        .height(34.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(2.dp)
      ) {
        Text(
          text = spec,
          fontSize = 7.5.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          color = Color.White
        )
        Text(
          text = "MKP PULSE",
          fontSize = 6.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          color = Color(0xFFFFD54F)
        )
      }
    }

    // Two sturdy terminal wire leads
    Row(
      modifier = Modifier.width(38.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Box(modifier = Modifier.width(3.5.dp).height(12.dp).background(Color(0xFFCFD8DC), RoundedCornerShape(1.dp)))
      Box(modifier = Modifier.width(3.5.dp).height(12.dp).background(Color(0xFFCFD8DC), RoundedCornerShape(1.dp)))
    }

    Row(
      modifier = Modifier.width(48.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text("Pin A", fontSize = 6.sp, color = TextTertiaryDark)
      Text("Pin B", fontSize = 6.sp, color = TextTertiaryDark)
    }
  }
}

/**
 * Realistic Electrolytic Capacitor (Aluminum Canister with negative stripe).
 */
@Composable
fun RealisticElectrolyticCap(
  ref: String,
  spec: String = "470uF 35V",
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
  ) {
    Text(ref, style = MaterialTheme.typography.labelSmall, color = SparkAmber, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    Spacer(modifier = Modifier.height(2.dp))

    // Canister
    Surface(
      shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
      color = Color(0xFF1565C0), // Deep blue
      shadowElevation = 4.dp,
      modifier = Modifier
        .width(32.dp)
        .height(38.dp)
    ) {
      Row(modifier = Modifier.fillMaxSize()) {
        // Negative stripe on left with "-" signs
        Column(
          modifier = Modifier
            .width(9.dp)
            .fillMaxHeight()
            .background(Color(0xFFECEFF1)),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceAround
        ) {
          Text("-", fontSize = 7.sp, fontWeight = FontWeight.Black, color = Color.Black)
          Text("-", fontSize = 7.sp, fontWeight = FontWeight.Black, color = Color.Black)
          Text("-", fontSize = 7.sp, fontWeight = FontWeight.Black, color = Color.Black)
        }

        // Spec text
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 2.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(spec, fontSize = 6.5.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          Text("Low-ESR", fontSize = 5.sp, color = Color(0xFFBBDEFB), fontFamily = FontFamily.Monospace)
        }
      }
    }

    // Leads (+ longer, - shorter)
    Row(
      modifier = Modifier.width(20.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top
    ) {
      // Negative lead
      Box(modifier = Modifier.width(2.5.dp).height(10.dp).background(Color(0xFFB0BEC5)))
      // Positive lead
      Box(modifier = Modifier.width(2.5.dp).height(14.dp).background(Color(0xFFB0BEC5)))
    }

    Row(modifier = Modifier.width(24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("(-)", fontSize = 6.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
      Text("(+)", fontSize = 6.sp, color = SparkAmber, fontWeight = FontWeight.Bold)
    }
  }
}

/**
 * Realistic Resistor with Axial Color Bands.
 */
@Composable
fun RealisticResistor(
  ref: String,
  valueText: String,
  colorBands: List<Color>,
  modifier: Modifier = Modifier,
  isPowerCement: Boolean = false
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
  ) {
    Text(
      text = ref,
      fontSize = 8.sp,
      color = SparkAmber,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )

    Spacer(modifier = Modifier.height(2.dp))

    if (!isPowerCement) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Left Wire Lead
        Box(modifier = Modifier.width(10.dp).height(2.dp).background(Color(0xFFCFD8DC)))

        // Resistor dumbbell body (Light blue for 1% metal film)
        Surface(
          shape = RoundedCornerShape(3.dp),
          color = Color(0xFF64B5F6),
          shadowElevation = 2.dp,
          modifier = Modifier.width(36.dp).height(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            colorBands.forEach { bandColor ->
              Box(
                modifier = Modifier
                  .width(2.5.dp)
                  .fillMaxHeight()
                  .background(bandColor)
              )
            }
          }
        }

        // Right Wire Lead
        Box(modifier = Modifier.width(10.dp).height(2.dp).background(Color(0xFFCFD8DC)))
      }
    } else {
      // Ceramic cement power resistor (e.g. 0.05R 5W)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(8.dp).height(2.5.dp).background(Color(0xFFCFD8DC)))

        Surface(
          shape = RoundedCornerShape(2.dp),
          color = Color(0xFFECEFF1),
          shadowElevation = 3.dp,
          border = BorderStroke(1.dp, Color(0xFFB0BEC5)),
          modifier = Modifier.width(44.dp).height(18.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(valueText, fontSize = 6.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF263238), fontFamily = FontFamily.Monospace)
          }
        }

        Box(modifier = Modifier.width(8.dp).height(2.5.dp).background(Color(0xFFCFD8DC)))
      }
    }

    Text(valueText, fontSize = 7.sp, color = Color(0xFF90CAF9), fontFamily = FontFamily.Monospace)
  }
}

/**
 * Realistic ATX Transformer T1 with yellow polyester tape bobbin.
 */
@Composable
fun RealisticTransformer(
  ref: String = "T1",
  spec: String = "Trafo ATX Push-Pull",
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
  ) {
    Text(ref, style = MaterialTheme.typography.labelSmall, color = SparkAmber, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    Spacer(modifier = Modifier.height(2.dp))

    // Transformer Assembly
    Surface(
      shape = RoundedCornerShape(4.dp),
      color = Color(0xFF212529), // Dark ferrite E-core
      shadowElevation = 6.dp,
      modifier = Modifier
        .width(88.dp)
        .height(64.dp)
        .border(1.5.dp, Color(0xFF374151), RoundedCornerShape(4.dp))
    ) {
      Row(modifier = Modifier.fillMaxSize()) {
        // Left ferrite clamp
        Box(modifier = Modifier.width(14.dp).fillMaxHeight().background(Color(0xFF1E293B)))

        // Middle bobbin with yellow high-voltage tape
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(Color(0xFFFDD835)) // Yellow tape
            .padding(2.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceAround
        ) {
          Text("HV AC OUT", fontSize = 6.sp, fontWeight = FontWeight.Black, color = Color.Black)
          Text(spec, fontSize = 5.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121), textAlign = TextAlign.Center)
          Text("12V CT IN", fontSize = 6.sp, fontWeight = FontWeight.Black, color = Color.Black)
        }

        // Right ferrite clamp
        Box(modifier = Modifier.width(14.dp).fillMaxHeight().background(Color(0xFF1E293B)))
      }
    }

    // Pin Out Labels
    Row(
      modifier = Modifier.width(88.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text("LV_A", fontSize = 6.sp, color = SparkAmber)
      Text("LV_CT (12V)", fontSize = 6.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
      Text("LV_B", fontSize = 6.sp, color = SparkAmber)
    }
  }
}

/**
 * Realistic Blade Fuse and Socket (e.g. 5A, 3A, 1A).
 */
@Composable
fun RealisticFuse(
  ref: String,
  rating: String = "5A",
  color: Color = Color(0xFFFB8C00),
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
  ) {
    Text(ref, style = MaterialTheme.typography.labelSmall, color = SparkAmber, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    Spacer(modifier = Modifier.height(2.dp))

    // Fuse Blade Body
    Surface(
      shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
      color = color.copy(alpha = 0.85f),
      shadowElevation = 3.dp,
      modifier = Modifier
        .width(26.dp)
        .height(22.dp)
        .border(1.dp, color, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(rating, fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
      }
    }

    // 2 Silver blades extending down
    Row(
      modifier = Modifier.width(18.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Box(modifier = Modifier.width(3.dp).height(8.dp).background(Color(0xFFCFD8DC)))
      Box(modifier = Modifier.width(3.dp).height(8.dp).background(Color(0xFFCFD8DC)))
    }
  }
}
