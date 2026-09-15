package id.ns200.cdir7.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ns200.cdir7.CdiViewModel
import id.ns200.cdir7.ui.theme.*

@Composable
fun ThermalFanQuickAccess(vm: CdiViewModel) {
    val mode by vm.fanMode.collectAsState()
    val on by vm.fanOnCdeg.collectAsState()
    val off by vm.fanOffCdeg.collectAsState()
    val t by vm.telemetry.collectAsState()
    val demo by vm.isSimulationMode.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }

    val hasTemp = t.tempCdeg != Short.MIN_VALUE.toInt()
    val tempC = if (hasTemp) t.tempCdeg / 100f else 0f
    val tempFormatted = if (hasTemp) "%.1f°C".format(tempC) else "--.-°C"

    val tempColor = when {
        !hasTemp -> TextMuted
        tempC >= 95f -> RaceRedline
        tempC >= 85f -> MotecOrange
        else -> ElectricCyan
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle),
        color = SurfacePanel
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Compact Telemetry Header Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Icon + Label + Live Temperature Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "SUHU & FAN",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = ElectricCyan
                    )

                    // Live Temperature Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CarbonDark)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tempFormatted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = tempColor
                        )
                    }
                }

                // Right: Segmented Mode Selector + Expand Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Segmented Mode Selector: OFF | ON | AUTO
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(CarbonDark)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(5.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("OFF", "ON", "AUTO").forEach { itemMode ->
                            val isSelected = mode.equals(itemMode, ignoreCase = true)
                            val activeBg = if (itemMode == "AUTO") RacingLime else MotecOrange
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSelected) activeBg else Color.Transparent)
                                    .clickable { vm.setFanMode(itemMode) }
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = itemMode,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) CarbonDark else TextSecondary
                                )
                            }
                        }
                    }

                    // Thresholds / Expand Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (isExpanded) BorderAccent else CarbonDark)
                            .border(1.dp, if (isExpanded) MotecOrange else BorderSubtle, RoundedCornerShape(5.dp))
                            .clickable { isExpanded = !isExpanded }
                            .padding(horizontal = 5.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${on / 100}°/${off / 100}°",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isExpanded) MotecOrange else TextSecondary
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Tutup ambang fan" else "Buka ambang fan",
                                tint = if (isExpanded) MotecOrange else TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Expandable Tuning Panel
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BorderSubtle)
                    )

                    // Dual Threshold Controls (Fan ON and Fan OFF)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Fan ON Box
                        ThresholdAdjustBox(
                            modifier = Modifier.weight(1f),
                            title = "FAN AKTIF (ON)",
                            tempDeg = on / 100f,
                            accentColor = RaceRedline,
                            onMinus = {
                                val newOn = (on - 100).coerceAtLeast(off + 300)
                                vm.setFanThresholds(newOn, off)
                            },
                            onPlus = {
                                val newOn = (on + 100).coerceAtMost(15000)
                                vm.setFanThresholds(newOn, off)
                            }
                        )

                        // Fan OFF Box
                        ThresholdAdjustBox(
                            modifier = Modifier.weight(1f),
                            title = "FAN PADAM (OFF)",
                            tempDeg = off / 100f,
                            accentColor = ElectricCyan,
                            onMinus = {
                                val newOff = (off - 100).coerceAtLeast(5000)
                                vm.setFanThresholds(on, newOff)
                            },
                            onPlus = {
                                val newOff = (off + 100).coerceAtMost(on - 300)
                                vm.setFanThresholds(on, newOff)
                            }
                        )
                    }

                    // Sliders for quick coarse adjustment
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "AMBANG AKTIF: ON ${on / 100}°C • OFF ${off / 100}°C",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                            Text(
                                text = "HYSTERESIS: ${(on - off) / 100}°C",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }
                        Slider(
                            value = on.toFloat(),
                            onValueChange = { vm.setFanThresholds(it.toInt(), off) },
                            valueRange = 5300f..15000f,
                            colors = SliderDefaults.colors(
                                thumbColor = RaceRedline,
                                activeTrackColor = RaceRedline,
                                inactiveTrackColor = CarbonDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                        )
                    }

                    // Simulation Mode Slider if active
                    if (demo) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CarbonDark,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "SIMULASI SUHU DEMO",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = SparkAmber
                                    )
                                    Text(
                                        text = "%.1f°C".format(if (t.tempCdeg < 0) 25f else t.tempCdeg / 100f),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = SparkAmber
                                    )
                                }
                                Slider(
                                    value = (if (t.tempCdeg < 0) 2500 else t.tempCdeg).toFloat(),
                                    onValueChange = { vm.setDemoTemperature(it / 100f) },
                                    valueRange = 2000f..13000f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = SparkAmber,
                                        activeTrackColor = SparkAmber,
                                        inactiveTrackColor = SurfacePanel
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                )
                            }
                        }
                    }

                    // Compact Warning note
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Driver relay low-side + dioda flyback wajib dipasang; fan tidak boleh langsung ke MCU.",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThresholdAdjustBox(
    modifier: Modifier = Modifier,
    title: String,
    tempDeg: Float,
    accentColor: Color,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Surface(
        modifier = modifier,
        color = CarbonDark,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "%.0f°C".format(tempDeg),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(SurfacePanel)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(3.dp))
                            .clickable(onClick = onMinus)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "-1°",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(SurfacePanel)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(3.dp))
                            .clickable(onClick = onPlus)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+1°",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

