package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.ViewAgenda
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Esp32Pin
import com.example.ui.theme.*

@Composable
fun Esp32HeaderVisualizer(
    esp32Pins: List<Esp32Pin>,
    selectedPin: Esp32Pin?,
    onSelectPin: (Esp32Pin) -> Unit,
    modifier: Modifier = Modifier
) {
    val leftPins = remember(esp32Pins) {
        esp32Pins.filter { it.headerSide == "LEFT" }.sortedBy { it.pinNumber }
    }
    val rightPins = remember(esp32Pins) {
        esp32Pins.filter { it.headerSide == "RIGHT" }.sortedBy { it.pinNumber }
    }
    val scrollState = rememberScrollState()
    var viewMode by remember { mutableStateOf("TABLE") } // "TABLE" (default, no horizontal stretch) or "BOARD"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TechSurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(OutlineDark, SparkAmber.copy(alpha = 0.5f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 1. Compact Header (No wasted vertical space)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(SparkAmber.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, SparkAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "MCU",
                            tint = SparkAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ESP32-WROOM-32D (38 PIN)",
                            style = MaterialTheme.typography.titleSmall,
                            color = SparkAmber,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "DevKitC V4 • 2x19 Pin Header • Logika 3.3V (ADC1)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondaryDark
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SparkAmber.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, SparkAmber)
                ) {
                    Text(
                        text = "38 PIN",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = SparkAmber,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. View Mode Switcher (Tabel Ringkas vs Fisik Board)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C131D), RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Mode Tabel (Pas layar HP, tidak melebar)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewMode = "TABLE" },
                    shape = RoundedCornerShape(6.dp),
                    color = if (viewMode == "TABLE") SparkAmber.copy(alpha = 0.25f) else Color.Transparent,
                    border = if (viewMode == "TABLE") BorderStroke(1.dp, SparkAmber) else null
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewAgenda,
                            contentDescription = null,
                            tint = if (viewMode == "TABLE") SparkAmber else TextSecondaryDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tabel Pinout (19x2)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = if (viewMode == "TABLE") FontWeight.Bold else FontWeight.Medium,
                            color = if (viewMode == "TABLE") Color.White else TextSecondaryDark,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Mode Fisik Board (Visual DevKit)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewMode = "BOARD" },
                    shape = RoundedCornerShape(6.dp),
                    color = if (viewMode == "BOARD") SparkAmber.copy(alpha = 0.25f) else Color.Transparent,
                    border = if (viewMode == "BOARD") BorderStroke(1.dp, SparkAmber) else null
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = null,
                            tint = if (viewMode == "BOARD") SparkAmber else TextSecondaryDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Visual Fisik Board",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = if (viewMode == "BOARD") FontWeight.Bold else FontWeight.Medium,
                            color = if (viewMode == "BOARD") Color.White else TextSecondaryDark,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Content based on View Mode
            if (viewMode == "TABLE") {
                // TABEL PINOUT RINGKAS: 2 Kolom (Header Kiri vs Header Kanan)
                // Sangat pas di layar HP, tidak melebar sama sekali!
                Esp32TwoColumnPinoutTable(
                    leftPins = leftPins,
                    rightPins = rightPins,
                    selectedPin = selectedPin,
                    onSelectPin = onSelectPin
                )
            } else {
                // VISUAL FISIK BOARD (19 pin kiri + 19 pin kanan dalam format horizontal scroll)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Geser ↔ untuk melihat 19 pin",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = TextTertiaryDark,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Header Kiri & Kanan (19x2)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = SparkAmber,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF080D14))
                            .border(1.5.dp, Color(0xFF263342), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // USB Port on Left
                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .height(96.dp)
                                    .background(Color(0xFF243040), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                                    .border(1.dp, Color(0xFF3B4D63), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "USB\nUART0",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, lineHeight = 9.sp),
                                    color = TextPrimaryDark,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Dual Pin Headers: Top row (Left 1-19), Bottom row (Right 1-19)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Left Header (Pins 1-19)
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    leftPins.forEach { pin ->
                                        Esp32PinDot(
                                            pin = pin,
                                            isSelected = pin.headerSide == selectedPin?.headerSide && pin.pinNumber == selectedPin?.pinNumber,
                                            onClick = { onSelectPin(pin) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Silkscreen Center Band
                                Row(
                                    modifier = Modifier
                                        .width(670.dp)
                                        .height(26.dp)
                                        .background(Color(0xFF141923), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0xFF252D3D), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ESP32-WROOM-32D",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                        color = SparkAmber,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "DevKitC V4 • CP2102 • 240MHz",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                                        color = Color(0xFF8FA2B5),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "38 PIN • ADC1 AMAN BLE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Black),
                                        color = SafetyGreen,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Right Header (Pins 1-19)
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    rightPins.forEach { pin ->
                                        Esp32PinDot(
                                            pin = pin,
                                            isSelected = pin.headerSide == selectedPin?.headerSide && pin.pinNumber == selectedPin?.pinNumber,
                                            onClick = { onSelectPin(pin) }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Antenna Zone on Far Right
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(100.dp)
                                    .background(Color(0xFF1F140E), RoundedCornerShape(6.dp))
                                    .border(1.dp, SparkAmber.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Antena",
                                        tint = SparkAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "BLE\nANT\n\nBEBAS\nLOGAM",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.5.sp, lineHeight = 8.sp),
                                        color = SparkAmber,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Selected Pin Quick Detail (If tapped)
            if (selectedPin != null) {
                SelectedPinDetailBanner(pin = selectedPin)
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 5. Clean Legend Dots
            OptInFlowLegend()
        }
    }
}

/**
 * 2-Column Side-by-Side Pinout Table:
 * Displays Left Header (Pins 1-19) and Right Header (Pins 1-19) side-by-side,
 * perfectly fitting any mobile portrait screen with NO horizontal scrolling!
 */
@Composable
private fun Esp32TwoColumnPinoutTable(
    leftPins: List<Esp32Pin>,
    rightPins: List<Esp32Pin>,
    selectedPin: Esp32Pin?,
    onSelectPin: (Esp32Pin) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF090E16),
        border = BorderStroke(1.dp, OutlineDark)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            // Table Header Titles
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "HEADER KIRI (Pin 1–19)",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black),
                    color = SparkAmber,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HEADER KANAN (Pin 1–19)",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black),
                    color = ElectricCyan,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Render each of the 19 rows side-by-side
            for (i in 0 until 19) {
                val leftPin = leftPins.getOrNull(i)
                val rightPin = rightPins.getOrNull(i)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Left pin cell
                    if (leftPin != null) {
                        PinCompactRowCell(
                            pin = leftPin,
                            isSelected = selectedPin?.headerSide == "LEFT" && selectedPin.pinNumber == leftPin.pinNumber,
                            onClick = { onSelectPin(leftPin) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Right pin cell
                    if (rightPin != null) {
                        PinCompactRowCell(
                            pin = rightPin,
                            isSelected = selectedPin?.headerSide == "RIGHT" && selectedPin.pinNumber == rightPin.pinNumber,
                            onClick = { onSelectPin(rightPin) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PinCompactRowCell(
    pin: Esp32Pin,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (pin.functionCategory) {
        "PULSER" -> ElectricCyan
        "GATE" -> RacingLime
        "OEM_LEARN" -> SparkAmber
        "ADC1_SENSOR" -> SensorAmber
        "BENCH" -> HighVoltageRed
        "POWER" -> if (pin.name.contains("GND")) GroundStarGold else ElectricCyan
        else -> Color(0xFF64748B)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(4.dp),
        color = if (isSelected) categoryColor.copy(alpha = 0.25f) else Color(0xFF121A24),
        border = BorderStroke(
            1.dp,
            if (isSelected) categoryColor else Color(0xFF1E2835)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pin Number badge
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0xFF1F2937), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${pin.pinNumber}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Pin Name
            Text(
                text = pin.name.replace("GPIO", "IO").take(10),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                color = if (isSelected) Color.White else categoryColor,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Category tag dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(categoryColor, CircleShape)
            )
        }
    }
}

@Composable
private fun SelectedPinDetailBanner(pin: Esp32Pin) {
    val categoryColor = when (pin.functionCategory) {
        "PULSER" -> ElectricCyan
        "GATE" -> RacingLime
        "OEM_LEARN" -> SparkAmber
        "ADC1_SENSOR" -> SensorAmber
        "BENCH" -> HighVoltageRed
        "POWER" -> if (pin.name.contains("GND")) GroundStarGold else ElectricCyan
        else -> Color(0xFF64748B)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = categoryColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(categoryColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${pin.pinNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${pin.name} (Header ${if (pin.headerSide == "LEFT") "Kiri" else "Kanan"})",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = categoryColor.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = pin.status,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                        color = categoryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Fungsi CDI: ${pin.finalDestination}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = TextPrimaryDark
            )

            Text(
                text = "Jalur Sirkuit: ${pin.fullPath}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = TextSecondaryDark,
                fontFamily = FontFamily.Monospace
            )

            if (pin.warning != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Peringatan",
                        tint = HighVoltageRed,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = pin.warning,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
                        color = HighVoltageRed
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptInFlowLegend() {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        LegendDot(color = ElectricCyan, text = "Pulser (IO4)")
        LegendDot(color = RacingLime, text = "Gate Koil (IO25/26)")
        LegendDot(color = SparkAmber, text = "OEM Tap PC817 (IO16/17)")
        LegendDot(color = SensorAmber, text = "Sensor ADC1 (IO32-39)")
        LegendDot(color = HighVoltageRed, text = "Uji Bangku / Flash SPI")
    }
}

@Composable
private fun Esp32PinDot(
    pin: Esp32Pin,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val pinColor = when (pin.functionCategory) {
        "PULSER" -> ElectricCyan
        "GATE" -> RacingLime
        "OEM_LEARN" -> SparkAmber
        "ADC1_SENSOR" -> SensorAmber
        "BENCH" -> HighVoltageRed
        "POWER" -> if (pin.name.contains("GND")) GroundStarGold else ElectricCyan
        else -> Color(0xFF64748B)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(28.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) pinColor.copy(alpha = 0.4f) else Color(0xFF121822))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color.White else pinColor.copy(alpha = 0.85f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${pin.pinNumber}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }

        Text(
            text = pin.name.replace("GPIO", "IO").take(6),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 6.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace
            ),
            color = if (isSelected) SparkAmber else TextSecondaryDark,
            maxLines = 1
        )
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF101722),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Medium),
                color = TextPrimaryDark,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
