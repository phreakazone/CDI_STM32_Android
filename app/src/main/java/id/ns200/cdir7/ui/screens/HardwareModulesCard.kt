package id.ns200.cdir7.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ns200.cdir7.CdiViewModel
import id.ns200.cdir7.HardwareModule
import id.ns200.cdir7.ui.theme.*

/**
 * Kartu Ringkas Modul Hardware IgniTra CDI R9.
 * Menggunakan tata letak ringkas & penamaan pendek untuk menghemat ruang layar.
 * Modul Core, SIDE, THERMAL, OEM_LEARN, AUX, dan TPS_DIAG adalah bit independen.
 */
@Composable
fun HardwareModulesCard(
    viewModel: CdiViewModel,
    modifier: Modifier = Modifier
) {
    val moduleStatus by viewModel.moduleStatus.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val isDemo by viewModel.isSimulationMode.collectAsState()
    val strobeActive by viewModel.strobeActive.collectAsState()
    val isLearning by viewModel.isOemLearning.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }

    // Hitung modul terpasang
    val sideInstalled = moduleStatus.isInstalled(HardwareModule.DUAL_COIL)
    val thermalInstalled = moduleStatus.isInstalled(HardwareModule.THERMAL_FAN)
    val oemInstalled = moduleStatus.isInstalled(HardwareModule.OEM_LEARN)
    val auxInstalled = moduleStatus.isInstalled(HardwareModule.AUX)
    val tpsInstalled = moduleStatus.isInstalled(HardwareModule.TPS_DIAG)

    val activeCount = (if (sideInstalled) 1 else 0) +
            (if (thermalInstalled) 1 else 0) +
            (if (oemInstalled) 1 else 0) +
            (if (auxInstalled) 1 else 0) +
            (if (tpsInstalled) 1 else 0)

    val fanOn = (telemetry.outputFlags and 0x08) != 0
    val tempStr = if (telemetry.tempCdeg == Short.MIN_VALUE.toInt()) "--°C" else "%.0f°C".format(telemetry.tempCdeg / 100f)
    val sideActive = moduleStatus.isActive(HardwareModule.DUAL_COIL) || telemetry.sideEnabled

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .testTag("hardware_modules_compact_card"),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Bar Ringkas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "MODUL HARDWARE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = ElectricCyan
                    )
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = (if (sideInstalled) MotecOrange else RacingLime).copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, (if (sideInstalled) MotecOrange else RacingLime).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = if (activeCount == 0) "CORE 1-COIL (J1.12)" else if (sideInstalled) "CORE + SIDE (DUAL COIL)" else "CORE + $activeCount MODUL",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (sideInstalled) MotecOrange else RacingLime,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isDemo) {
                        Text(
                            text = "DEMO",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MotecOrange
                        )
                    }
                    Text(
                        text = if (isExpanded) "▲" else "▼",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (isExpanded) {
                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)

                // 1. Core 1-Coil (Utama)
                CompactModuleRow(
                    label = "Core",
                    pin = "J1.12",
                    status = if (telemetry.centerEnabled || telemetry.hvCenter >= 50) "Aktif (${telemetry.hvCenter}V)" else "Standby (0V)",
                    active = telemetry.centerEnabled || telemetry.hvCenter >= 50,
                    statusColor = RacingLime
                )

                // 2. Modul SIDE (Koil Kedua)
                CompactModuleRow(
                    label = "SIDE",
                    pin = "J1.6",
                    status = when {
                        !sideInstalled -> "Tidak Dipasang"
                        sideActive -> "Aktif (${telemetry.hvSide}V)"
                        else -> "Belum Aktif (0V)"
                    },
                    active = sideActive,
                    statusColor = if (sideActive) RacingLime else if (sideInstalled) SensorAmber else TextMuted,
                    demoAction = if (isDemo) {
                        if (!sideInstalled) "Pasang" else if (!sideActive) "Aktifkan" else "Lepas"
                    } else null,
                    onDemoClick = if (isDemo) {
                        {
                            if (!sideInstalled) {
                                viewModel.toggleModuleInstalled(HardwareModule.DUAL_COIL)
                            } else if (!sideActive) {
                                viewModel.setModuleActive(HardwareModule.DUAL_COIL, true)
                            } else {
                                viewModel.toggleModuleInstalled(HardwareModule.DUAL_COIL)
                            }
                        }
                    } else null
                )

                // 3. Thermal / Fan
                CompactModuleRow(
                    label = "Thermal",
                    pin = "J1.3/J1.7",
                    status = if (!thermalInstalled) "Tidak Dipasang" else if (fanOn) "Kipas ON ($tempStr)" else "Kipas OFF ($tempStr)",
                    active = thermalInstalled,
                    statusColor = if (thermalInstalled) (if (fanOn) SensorAmber else RacingLime) else TextMuted,
                    demoAction = if (isDemo) (if (thermalInstalled) "Lepas" else "Pasang") else null,
                    onDemoClick = if (isDemo) { { viewModel.toggleModuleInstalled(HardwareModule.THERMAL_FAN) } } else null
                )

                // 4. OEM Learn
                CompactModuleRow(
                    label = "OEM Learn",
                    pin = "PC817",
                    status = when {
                        !oemInstalled -> "Tidak Dipasang"
                        isLearning -> "Merekam Pulsa..."
                        else -> "Siap Rekam"
                    },
                    active = oemInstalled,
                    statusColor = if (isLearning) MotecOrange else if (oemInstalled) ElectricCyan else TextMuted,
                    demoAction = if (isDemo && oemInstalled) (if (isLearning) "Stop" else "Rekam") else if (isDemo) "Pasang" else null,
                    onDemoClick = if (isDemo) {
                        {
                            if (!oemInstalled) {
                                viewModel.toggleModuleInstalled(HardwareModule.OEM_LEARN)
                            } else if (isLearning) {
                                viewModel.stopOemLearn()
                            } else {
                                viewModel.startOemLearn()
                            }
                        }
                    } else null
                )

                // 5. AUX (Strobe)
                CompactModuleRow(
                    label = "AUX",
                    pin = "GPIO27",
                    status = if (!auxInstalled) "Tidak Dipasang" else if (strobeActive) "Strobo ON" else "Strobo OFF",
                    active = auxInstalled,
                    statusColor = if (strobeActive) ElectricCyan else if (auxInstalled) TextSecondary else TextMuted,
                    demoAction = if (auxInstalled) (if (strobeActive) "Off" else "Test") else if (isDemo) "Pasang" else null,
                    onDemoClick = {
                        if (!auxInstalled && isDemo) {
                            viewModel.toggleModuleInstalled(HardwareModule.AUX)
                        } else {
                            viewModel.toggleStrobe(!strobeActive)
                        }
                    }
                )

                // 6. TPS Diag
                CompactModuleRow(
                    label = "TPS Diag",
                    pin = "J1.4",
                    status = if (tpsInstalled) "Gas ${(telemetry.tps / 10f).toInt()}%" else "Tidak Dipasang",
                    active = tpsInstalled,
                    statusColor = if (tpsInstalled) RacingLime else TextMuted,
                    demoAction = if (isDemo) (if (tpsInstalled) "Lepas" else "Pasang") else null,
                    onDemoClick = if (isDemo) { { viewModel.toggleModuleInstalled(HardwareModule.TPS_DIAG) } } else null
                )
            }
        }
    }
}

@Composable
private fun CompactModuleRow(
    label: String,
    pin: String,
    status: String,
    active: Boolean,
    statusColor: Color,
    demoAction: String? = null,
    onDemoClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(SurfacePanel)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (active) statusColor else TextMuted)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )
            Text(
                text = pin,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = status,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = statusColor
            )

            if (demoAction != null && onDemoClick != null) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.6f)),
                    modifier = Modifier.clickable(onClick = onDemoClick)
                ) {
                    Text(
                        text = demoAction,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
