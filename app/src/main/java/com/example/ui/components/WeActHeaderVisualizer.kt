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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GridView
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
import com.example.model.WeActPin
import com.example.ui.theme.*

/**
 * WeAct Studio STM32WB55 Core Board (35 Pin: 15 H_TOP + 20 H_BOTTOM) Visualizer.
 * Mendukung 2 mode tampilan konsisten:
 * 1. Mode Tabel (2 Kolom berdampingan: H_TOP vs H_BOTTOM - pas layar HP, tanpa horizontal scroll)
 * 2. Mode Fisik Board (Visual realistis sesuai foto fisik asli board: USB Type-C, tombol NRST & BOOT0,
 *    SWD Debug header 4-pin, IC STM32WB55 QFN68, sirkuit RF, dan antena emas di ujung kanan).
 */
@Composable
fun WeActHeaderVisualizer(
    weActPins: List<WeActPin>,
    selectedPin: WeActPin?,
    onSelectPin: (WeActPin) -> Unit,
    modifier: Modifier = Modifier
) {
    val topPins = remember(weActPins) {
        weActPins.filter { it.header == "H_TOP" }.sortedBy { it.pinNumber }
    }
    val bottomPins = remember(weActPins) {
        weActPins.filter { it.header == "H_BOTTOM" }.sortedBy { it.pinNumber }
    }
    val scrollState = rememberScrollState()
    var viewMode by remember { mutableStateOf("TABLE") } // "TABLE" or "BOARD"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TechSurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(OutlineDark, ElectricCyan.copy(alpha = 0.5f)))
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
                            .background(ElectricCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "MCU",
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "WEACT STM32WB55 (35 PIN)",
                            style = MaterialTheme.typography.titleSmall,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Dual-Core M4/M0+ • 15 H_TOP + 20 H_BOTTOM • 5V Buck",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondaryDark
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ElectricCyan.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, ElectricCyan)
                ) {
                    Text(
                        text = "35 PIN",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = ElectricCyan,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. View Mode Switcher (Tabel Pinout vs Fisik Board)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C131D), RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Mode Tabel (Pas layar HP, 2 Kolom)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewMode = "TABLE" },
                    shape = RoundedCornerShape(6.dp),
                    color = if (viewMode == "TABLE") ElectricCyan.copy(alpha = 0.25f) else Color.Transparent,
                    border = if (viewMode == "TABLE") BorderStroke(1.dp, ElectricCyan) else null
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewAgenda,
                            contentDescription = null,
                            tint = if (viewMode == "TABLE") ElectricCyan else TextSecondaryDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tabel Pinout (15x20)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = if (viewMode == "TABLE") FontWeight.Bold else FontWeight.Medium,
                            color = if (viewMode == "TABLE") Color.White else TextSecondaryDark,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Mode Fisik Board (Visual WeAct PCB Asli)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewMode = "BOARD" },
                    shape = RoundedCornerShape(6.dp),
                    color = if (viewMode == "BOARD") ElectricCyan.copy(alpha = 0.25f) else Color.Transparent,
                    border = if (viewMode == "BOARD") BorderStroke(1.dp, ElectricCyan) else null
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = null,
                            tint = if (viewMode == "BOARD") ElectricCyan else TextSecondaryDark,
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
                // TABEL PINOUT RINGKAS: 2 Kolom (H_TOP 15 Pin vs H_BOTTOM 20 Pin)
                WeActTwoColumnPinoutTable(
                    topPins = topPins,
                    bottomPins = bottomPins,
                    selectedPin = selectedPin,
                    onSelectPin = onSelectPin
                )
            } else {
                // VISUAL FISIK BOARD WEACT STM32WB55
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Geser ↔ untuk melihat 35 pin",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = TextTertiaryDark,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "H_TOP (15) • H_BOTTOM (20)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = ElectricCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Realistic Physical Board Layout matching Sce9575e02a194eaa9b5783c4f03053fad.png
                    WeActPhysicalBoardCanvas(
                        scrollState = scrollState,
                        topPins = topPins,
                        bottomPins = bottomPins,
                        selectedPin = selectedPin,
                        onSelectPin = onSelectPin
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Selected Pin Quick Detail (If tapped)
            if (selectedPin != null) {
                SelectedWeActPinDetailBanner(pin = selectedPin)
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 5. Clean Legend Dots
            WeActFlowLegend()
        }
    }
}

/**
 * 2-Column Side-by-Side Pinout Table for WeAct STM32WB55:
 * Menampilkan H_TOP (15 Pin) dan H_BOTTOM (20 Pin) berdampingan rapi tanpa horizontal scroll.
 */
@Composable
private fun WeActTwoColumnPinoutTable(
    topPins: List<WeActPin>,
    bottomPins: List<WeActPin>,
    selectedPin: WeActPin?,
    onSelectPin: (WeActPin) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF090E16),
        border = BorderStroke(1.dp, OutlineDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Table Header Titles
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "HEADER H_TOP (Pin 1–15)",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black),
                    color = ElectricCyan,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HEADER H_BOTTOM (Pin 1–20)",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black),
                    color = SparkAmber,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 20 Baris (H_TOP 15 baris + 5 baris kosong; H_BOTTOM 20 baris penuh)
            for (i in 0 until 20) {
                val topPin = topPins.getOrNull(i)
                val bottomPin = bottomPins.getOrNull(i)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Left: H_TOP
                    if (topPin != null) {
                        WeActPinCompactRowCell(
                            pin = topPin,
                            isSelected = selectedPin?.header == "H_TOP" && selectedPin.pinNumber == topPin.pinNumber,
                            onClick = { onSelectPin(topPin) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Empty slot for H_TOP pins 16-20
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .background(Color(0xFF080C12), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF141C26), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "— (Tanpa Pin) —",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                                color = Color(0xFF334155),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Right: H_BOTTOM
                    if (bottomPin != null) {
                        WeActPinCompactRowCell(
                            pin = bottomPin,
                            isSelected = selectedPin?.header == "H_BOTTOM" && selectedPin.pinNumber == bottomPin.pinNumber,
                            onClick = { onSelectPin(bottomPin) },
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
private fun WeActPinCompactRowCell(
    pin: WeActPin,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = getWeActCategoryColor(pin)

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

            // Pin Name (e.g. PA0, PA1, PB8, 3V3, 5V, GND)
            Text(
                text = pin.name,
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

/**
 * Visual Fisik Board WeAct STM32WB55:
 * Didesain presisi mengacu pada foto fisik board asli (Sce9575e02a194eaa9b5783c4f03053fad.png):
 * - Dark Olive Green / Black PCB
 * - Port USB Type-C metal di kiri
 * - Tombol NRST (atas) dan BOOT0 (bawah)
 * - Header SWD DEBUG 4-Pin: 3V3, DIO, CLK, G
 * - Chip QFN68 STM32WB55 dengan garis silkscreen putih dan dot Pin 1
 * - Jalur RF dan antena emas di ujung kanan (Keepout area)
 * - Label silkscreen asli pada tepi header (G, G, 3V3, 3V3, B7... / G, 5V, 5V, VB, H3...)
 */
@Composable
private fun WeActPhysicalBoardCanvas(
    scrollState: androidx.compose.foundation.ScrollState,
    topPins: List<WeActPin>,
    bottomPins: List<WeActPin>,
    selectedPin: WeActPin?,
    onSelectPin: (WeActPin) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF182017)) // Olive dark PCB color
            .border(2.dp, Color(0xFF2C3B29), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 1. USB Type-C Receptacle on Left (Metal Silver Body)
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(106.dp)
                    .background(Color(0xFFE2E8F0), RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .border(2.dp, Color(0xFF94A3B8), RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(64.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF64748B), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TYPE-C\nUSB",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp, lineHeight = 8.sp),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 2. Buttons & Passives Area
            Column(
                modifier = Modifier
                    .width(52.dp)
                    .height(112.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Button: NRST
                PhysicalButtonWidget(label = "NRST")

                // Small SMD passive indicator
                Box(
                    modifier = Modifier
                        .size(10.dp, 6.dp)
                        .background(Color(0xFFC0A060), RoundedCornerShape(1.dp))
                )

                // Bottom Button: BOOT0
                PhysicalButtonWidget(label = "BOOT0")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 3. SWD DEBUG 4-Pin Header
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .height(112.dp)
                    .background(Color(0xFF111711), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF2A3828), RoundedCornerShape(4.dp))
                    .padding(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "DEBUG",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.5.sp, fontWeight = FontWeight.Bold),
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                listOf("3V3", "DIO", "CLK", "G").forEach { pinLbl ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Color(0xFF000000), CircleShape)
                                .border(1.dp, Color(0xFFD4AF37), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = pinLbl,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
                            color = Color(0xFFB0C4B1),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 4. Main Pin Headers & STM32 MCU Area
            Column(horizontalAlignment = Alignment.Start) {
                // Silkscreen markings & Pins H_TOP (15 Pins)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    topPins.forEach { pin ->
                        WeActPhysicalPinHole(
                            pin = pin,
                            silkscreenLabel = getSilkscreenLabel(pin),
                            isSelected = selectedPin?.header == "H_TOP" && selectedPin.pinNumber == pin.pinNumber,
                            onClick = { onSelectPin(pin) }
                        )
                    }
                    // Spacer to match bottom 20 pins width
                    repeat(5) {
                        Box(modifier = Modifier.width(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Center Area: STM32WB55 QFN68 chip + Silkscreen + Resonator
                Row(
                    modifier = Modifier
                        .width(680.dp)
                        .height(48.dp)
                        .background(Color(0xFF131A12), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF243322), RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left indicator
                    Text(
                        text = "WeAct Studio Core Board",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                        color = Color(0xFFA5B4A3),
                        fontFamily = FontFamily.Monospace
                    )

                    // STM32WB55 Chip representation
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Crystal HSE
                        Box(
                            modifier = Modifier
                                .size(16.dp, 10.dp)
                                .background(Color(0xFF9E9E9E), RoundedCornerShape(2.dp))
                                .border(1.dp, Color(0xFFC0C0C0), RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("32M", fontSize = 5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Chip QFN68 with pin 1 dot
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF1E1E1E), RoundedCornerShape(2.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(Color.White, CircleShape)
                                    .align(Alignment.TopStart)
                            )
                            Text(
                                text = "STM32\nWB55",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp, lineHeight = 7.sp),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // RF specs
                    Text(
                        text = "BLE 5.4 • 64MHz M4 + 32MHz M0+",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Black),
                        color = ElectricCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Silkscreen markings & Pins H_BOTTOM (20 Pins)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    bottomPins.forEach { pin ->
                        WeActPhysicalPinHole(
                            pin = pin,
                            silkscreenLabel = getSilkscreenLabel(pin),
                            isSelected = selectedPin?.header == "H_BOTTOM" && selectedPin.pinNumber == pin.pinNumber,
                            onClick = { onSelectPin(pin) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 5. Golden PCB Antenna Section on Far Right (matching photo Sce9575e02a194eaa9b5783c4f03053fad.png)
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(112.dp)
                    .background(Color(0xFF42442A), RoundedCornerShape(6.dp)) // Tan keepout
                    .border(1.5.dp, Color(0xFFD4AF37), RoundedCornerShape(6.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Antenna meander trace simulation
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(58.dp)
                            .border(2.dp, Color(0xFFE5C158), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "2.4G BLE\nANTENNA\n\nBEBAS\nLOGAM",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp, lineHeight = 7.5.sp),
                        color = Color(0xFFFFE082),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Tactile Button representation (NRST & BOOT0)
 */
@Composable
private fun PhysicalButtonWidget(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp, 20.dp)
                .background(Color(0xFFD1D5DB), RoundedCornerShape(3.dp))
                .border(1.dp, Color(0xFF4B5563), RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(0xFF111827), CircleShape)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.5.sp, fontWeight = FontWeight.Bold),
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Lubang Pin Fisik Header (Gold plated pad dengan silkscreen asli)
 */
@Composable
private fun WeActPhysicalPinHole(
    pin: WeActPin,
    silkscreenLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = getWeActCategoryColor(pin)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(28.dp)
            .clickable { onClick() }
    ) {
        // Silkscreen label on top/bottom
        Text(
            text = silkscreenLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            ),
            color = if (isSelected) Color.White else Color(0xFFF1F5F9),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Gold Through-Hole Pad
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) categoryColor.copy(alpha = 0.5f) else Color(0xFF111612))
                .border(
                    width = if (isSelected) 2.5.dp else 1.5.dp,
                    color = if (isSelected) Color.White else Color(0xFFD4AF37), // Gold pad ring
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Dark inner hole
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Black, CircleShape)
            )
        }

        // Pin Number
        Text(
            text = "${pin.pinNumber}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 6.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace
            ),
            color = if (isSelected) categoryColor else TextTertiaryDark
        )
    }
}

@Composable
private fun SelectedWeActPinDetailBanner(pin: WeActPin) {
    val categoryColor = getWeActCategoryColor(pin)
    val isCritical = pin.isCritical || (pin.warning != null && pin.warning.contains("DILARANG", ignoreCase = true))

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
                        text = "${pin.name} (${pin.header} • Pin ${pin.pinNumber})",
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
private fun WeActFlowLegend() {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        LegendDot(color = ElectricCyan, text = "Pulser PA0")
        LegendDot(color = RacingLime, text = "Gate Koil PA1/PA2")
        LegendDot(color = SparkAmber, text = "OEM Learn PB3/PB4")
        LegendDot(color = SensorAmber, text = "ADC & Suhu")
        LegendDot(color = HighVoltageRed, text = "Kritis / Dilarang 12V")
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

private fun getWeActCategoryColor(pin: WeActPin): Color {
    return when {
        pin.warning != null && pin.warning.contains("DILARANG", ignoreCase = true) -> HighVoltageRed
        pin.name == "PA0" -> ElectricCyan
        pin.name == "PA1" || pin.name == "PA2" -> RacingLime
        pin.name == "PB3" || pin.name == "PB4" -> SparkAmber
        pin.name.startsWith("PA") && (pin.name == "PA3" || pin.name == "PA4" || pin.name == "PA5" || pin.name == "PA6" || pin.name == "PA7") -> SensorAmber
        pin.name == "PB0" || pin.name == "PB2" -> SensorAmber
        pin.name == "PB8" || pin.name == "PA9" -> SparkAmber // Charger PWM
        pin.name.contains("GND") || pin.name == "G" -> GroundStarGold
        pin.name.contains("5V") || pin.name.contains("3V3") -> ElectricCyan
        pin.status == "CADANGAN" -> Color(0xFF64748B)
        else -> ElectricCyan
    }
}

/**
 * Mapping nama pin ke teks silkscreen asli pada PCB STM32WB55 (sesuai foto)
 */
private fun getSilkscreenLabel(pin: WeActPin): String {
    return when (pin.header) {
        "H_TOP" -> when (pin.pinNumber) {
            1, 2, 15 -> "G"
            3, 4 -> "3V3"
            5 -> "B7"
            6 -> "B6"
            7 -> "B5"
            8 -> "B4"
            9 -> "B3"
            10 -> "A15"
            11 -> "A10"
            12 -> "E4"
            13 -> "B1"
            14 -> "B0"
            else -> pin.name
        }
        "H_BOTTOM" -> when (pin.pinNumber) {
            1, 20 -> "G"
            2, 3 -> "5V"
            4 -> "VB"
            5 -> "H3"
            6 -> "B9"
            7 -> "B8"
            8 -> "NR"
            9 -> "A0"
            10 -> "A1"
            11 -> "A2"
            12 -> "A3"
            13 -> "A4"
            14 -> "A5"
            15 -> "A6"
            16 -> "A7"
            17 -> "A8"
            18 -> "A9"
            19 -> "B2"
            else -> pin.name
        }
        else -> pin.name
    }
}
