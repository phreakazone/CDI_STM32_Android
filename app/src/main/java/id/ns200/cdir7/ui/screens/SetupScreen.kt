package id.ns200.cdir7.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ns200.cdir7.CdiViewModel
import id.ns200.cdir7.FirmwareRunMode
import id.ns200.cdir7.McuPlatform
import id.ns200.cdir7.ScreenTab
import id.ns200.cdir7.SetupStage
import id.ns200.cdir7.Telemetry
import id.ns200.cdir7.ui.theme.*

/**
 * Wizard komisi ringkas pada tab utama Setup.
 *
 * Hanya tahap aktif yang dirender. Tahap TDC sengaja memakai StrobeScreen lama,
 * sedangkan Wiring -> Komisi CDI tetap memakai QuickSetupGuideScreen lengkap.
 * Keduanya berbagi CdiViewModel, state MCU, antrean perintah, dan ACK yang sama.
 */
@Composable
fun McuPinGuidance(
    selectedPlatform: McuPlatform,
    stmPin: String,
    espPin: String,
    warning: String? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (warning != null && selectedPlatform == McuPlatform.ESP32_WROOM) RaceRedline.copy(alpha = 0.12f) else SurfacePanel,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (warning != null && selectedPlatform == McuPlatform.ESP32_WROOM) RaceRedline else BorderSubtle
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedPlatform == McuPlatform.STM32WB55) "PIN TARGET STM32WB55:" else "PIN TARGET ESP32-WROOM:",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedPlatform == McuPlatform.STM32WB55) ElectricCyan else SparkAmber,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (selectedPlatform == McuPlatform.STM32WB55) stmPin else espPin,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (warning != null && selectedPlatform == McuPlatform.ESP32_WROOM) {
                Text(
                    text = "PERINGATAN: $warning",
                    fontSize = 8.5.sp,
                    color = RaceRedline,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun SetupScreen(viewModel: CdiViewModel) {
    val page by viewModel.quickSetupPage.collectAsState()
    val unlocked by viewModel.quickSetupUnlockedStage.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val message by viewModel.quickSetupMessage.collectAsState()
    val selectedPlatform by viewModel.selectedPlatform.collectAsState()
    val stage = SetupStage.entries.getOrNull(page) ?: SetupStage.BARU
    val visibleProgress = maxOf(unlocked, telemetry.setupStage)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
    ) {
        CompactSetupHeader(
            stage = stage,
            visibleProgress = visibleProgress,
            message = message,
            selectedPlatform = selectedPlatform,
            onTogglePlatform = {
                viewModel.setMcuPlatform(
                    if (selectedPlatform == McuPlatform.STM32WB55) McuPlatform.ESP32_WROOM else McuPlatform.STM32WB55
                )
            },
            onSelect = viewModel::selectQuickSetupPage
        )

        val fwMode by viewModel.firmwareMode.collectAsState()

        Box(modifier = Modifier.weight(1f)) {
            when (stage) {
                SetupStage.BARU -> BaruStage(viewModel, telemetry, selectedPlatform)
                SetupStage.PULSER -> PulserStage(viewModel, telemetry, selectedPlatform)
                SetupStage.TDC -> {
                    if (fwMode == FirmwareRunMode.MANUAL) {
                        StrobeScreen(viewModel)
                    } else {
                        OemLearnTdcCheckpointStage(viewModel, telemetry, selectedPlatform)
                    }
                }
                SetupStage.TPS_CAL -> TpsStage(viewModel, telemetry, selectedPlatform)
                SetupStage.FIRST_START -> FirstStartStage(viewModel, telemetry, selectedPlatform)
                SetupStage.READY -> ReadyStage(viewModel, telemetry, selectedPlatform)
            }
        }
    }
}

@Composable
private fun CompactSetupHeader(
    stage: SetupStage,
    visibleProgress: Int,
    message: String,
    selectedPlatform: McuPlatform,
    onTogglePlatform: () -> Unit,
    onSelect: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle),
        color = SurfacePanel
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "SETUP CDI",
                        color = MotecOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = MotecOrange.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "R8",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MotecOrange,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // Platform Toggle Chip
                val isStm = selectedPlatform == McuPlatform.STM32WB55
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onTogglePlatform() },
                    color = if (isStm) ElectricCyan.copy(alpha = 0.15f) else SparkAmber.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isStm) ElectricCyan else SparkAmber
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = if (isStm) ElectricCyan else SparkAmber,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedPlatform.displayName,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isStm) ElectricCyan else SparkAmber
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "⇄",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TAHAP ${stage.code + 1}/6 • ${stage.label}",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    if (stage == SetupStage.READY) "SELESAI" else "IKUTI URUTAN",
                    color = if (stage == SetupStage.READY) RacingLime else SensorAmber,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                SetupStage.entries.forEach { item ->
                    val selected = item == stage
                    val unlocked = item.code <= visibleProgress
                    val completed = item.code < visibleProgress && unlocked
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelect(item.code) },
                        color = when {
                            selected -> MotecOrange.copy(alpha = 0.18f)
                            completed -> RacingLime.copy(alpha = 0.10f)
                            else -> CardBackground
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when {
                                selected -> MotecOrange
                                completed -> RacingLime.copy(alpha = 0.65f)
                                else -> BorderSubtle
                            }
                        )
                    ) {
                        Text(
                            text = "${item.code + 1} ${item.label}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            color = when {
                                selected -> MotecOrange
                                completed -> RacingLime
                                unlocked -> TextSecondary
                                else -> TextMuted
                            },
                            fontSize = 8.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Text(
                text = message,
                color = when {
                    message.startsWith("LULUS") || message.startsWith("DEMO LULUS") -> RacingLime
                    message.startsWith("GAGAL") -> RaceRedline
                    else -> ElectricCyan
                },
                fontSize = 9.sp,
                lineHeight = 12.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun StageBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun StageCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                title,
                color = MotecOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                subtitle,
                color = TextSecondary,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontFamily = FontFamily.Monospace
            )
            content()
        }
    }
}

@Composable
private fun BaruStage(viewModel: CdiViewModel, t: Telemetry, selectedPlatform: McuPlatform) {
    val connected by viewModel.isConnected.collectAsState()
    val demo by viewModel.isSimulationMode.collectAsState()
    val busy by viewModel.quickSetupPreflightBusy.collectAsState()
    val pending by viewModel.setupCommandPending.collectAsState()
    val fwMode by viewModel.firmwareMode.collectAsState()
    val isOemLearning by viewModel.isOemLearning.collectAsState()
    val oemCenterPulses by viewModel.oemCenterPulses.collectAsState()
    val oemSideSamples by viewModel.oemSideSamples.collectAsState()
    val isOemUnpluggedConfirmed by viewModel.isOemUnpluggedConfirmed.collectAsState()
    val isProVoltage by viewModel.isProVoltageConfigured.collectAsState()
    val targetHv by viewModel.targetHvVoltage.collectAsState()

    StageBody {
        StageCard(
            title = "1 • PEMERIKSAAN AWAL",
            subtitle = "Mesin mati, HV < 30V. Aplikasi memeriksa PING, STATUS, SETUP, RPM dan tegangan HV sebelum lanjut."
        ) {
            CompactStatusRow("TARGET MCU", selectedPlatform.displayName, true)
            CompactStatusRow("BLE / STATUS", if (connected || demo) "SIAP" else "BELUM TERHUBUNG", connected || demo)
            CompactStatusRow("RPM", "${t.rpm}", t.rpm == 0)
            CompactStatusRow("HV CENTER", "${t.hvCenter} V", t.hvCenter < 30)
            CompactStatusRow("HV SIDE", "${t.hvSide} V", t.hvSide < 30)

            McuPinGuidance(
                selectedPlatform = selectedPlatform,
                stmPin = "5V logic (H_BOTTOM.2) & GND (H_BOTTOM.1/20)",
                espPin = "VIN 5V (Kiri.19 Pin 19) & GND_STAR (Kiri.14 / Kanan.20)",
                warning = if (selectedPlatform == McuPlatform.ESP32_WROOM) "Jangan sambungkan aki 12V langsung ke pin manapun pada ESP32!" else null
            )

            Button(
                enabled = !busy,
                onClick = viewModel::startQuickSetupPreflight,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MotecOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = CarbonDark)
                    Spacer(Modifier.width(7.dp))
                    Text("MENUNGGU RESPONS MCU...", color = CarbonDark)
                } else {
                    Text("PERIKSA & LANJUT PULSER", color = CarbonDark)
                }
            }
        }

        StageCard(
            title = "KONTROL MODE FIRMWARE R8",
            subtitle = if (selectedPlatform == McuPlatform.STM32WB55) {
                "Pilih alur kerja CDI STM32. Mode DIY mandiri hanya aktif setelah konfirmasi OEM_UNPLUGGED (tidak ada takeover otomatis)."
            } else {
                "Pilih alur kerja CDI ESP32. Mode DIY mandiri hanya aktif setelah konfirmasi OEM_UNPLUGGED (tidak ada takeover otomatis)."
            }
        ) {
            // Mode selector tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    FirmwareRunMode.OEM_LEARN to "OEM LEARN",
                    FirmwareRunMode.MANUAL to "MANUAL",
                    FirmwareRunMode.DIY to "DIY"
                ).forEach { (m, label) ->
                    val isSelected = fwMode == m
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { viewModel.setFirmwareMode(m) },
                        color = if (isSelected) MotecOrange.copy(alpha = 0.2f) else SurfacePanel,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MotecOrange else BorderSubtle
                        )
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 7.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) MotecOrange else TextSecondary
                        )
                    }
                }
            }

            when (fwMode) {
                FirmwareRunMode.OEM_LEARN -> {
                    val centerLabel = if (selectedPlatform == McuPlatform.STM32WB55) "PB3" else "GPIO16"
                    val sideLabel = if (selectedPlatform == McuPlatform.STM32WB55) "PB4" else "GPIO17"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfacePanel, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "ALUR OEM LEARN (BACA TIMING PASIF $centerLabel/$sideLabel)",
                            color = ElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            if (selectedPlatform == McuPlatform.STM32WB55) {
                                "STM32 membaca sinyal pengapian CDI OEM secara pasif melalui PB3 (Center) & PB4 (Side). Mesin hidup menggunakan CDI OEM."
                            } else {
                                "ESP32 membaca sinyal pengapian CDI OEM secara pasif melalui GPIO16 (Center) & GPIO17 (Side) via optocoupler PC817. Mesin hidup menggunakan CDI OEM."
                            },
                            color = TextSecondary,
                            fontSize = 9.sp,
                            lineHeight = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        CompactStatusRow("PULSA OEM CENTER ($centerLabel)", "$oemCenterPulses pulsa", oemCenterPulses > 0)
                        CompactStatusRow("SAMPEL OEM SIDE ($sideLabel)", "$oemSideSamples sampel", oemSideSamples > 0)
                        CompactStatusRow("STATUS BELAJAR", if (isOemLearning) "SEDANG MEREKAM..." else "SIAP", isOemLearning)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                enabled = !isOemLearning && !pending,
                                onClick = viewModel::startOemLearn,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MotecOrange),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(vertical = 5.dp)
                            ) {
                                Text("MULAI LEARN", color = CarbonDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                enabled = isOemLearning && !pending,
                                onClick = viewModel::stopOemLearn,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = RacingLime),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(vertical = 5.dp)
                            ) {
                                Text("SIMPAN & STOP", color = CarbonDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // PANDUAN VISUAL WIRING & RANGKAIAN PENGAMAN SUNTIK KOIL & DAYA
                        Spacer(modifier = Modifier.height(4.dp))
                        OemLearnSafetyWiringGuide(selectedPlatform = selectedPlatform)
                    }
                }
                FirmwareRunMode.DIY -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfacePanel, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "MODE DIY (CDI MANDIRI - TANPA OEM)",
                            color = if (isOemUnpluggedConfirmed) RacingLime else SensorAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            if (isOemUnpluggedConfirmed) {
                                "Soket CDI OEM terkonfirmasi dilepas. CDI STM32 bekerja secara mandiri mengontrol pengapian."
                            } else {
                                "PERHATIAN KESELAMATAN: Mode DIY hanya aktif setelah CDI OEM dicabut dari harness (OEM_UNPLUGGED). Tidak ada takeover otomatis."
                            },
                            color = TextSecondary,
                            fontSize = 9.sp,
                            lineHeight = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (!isOemUnpluggedConfirmed) {
                            Button(
                                enabled = !pending,
                                onClick = viewModel::confirmOemUnplugged,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = RacingLime),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("KONFIRMASI OEM_UNPLUGGED & AKTIFKAN DIY", color = CarbonDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = RacingLime, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("OEM_UNPLUGGED Dikonfirmasi • DIY Aktif", color = RacingLime, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
                FirmwareRunMode.MANUAL -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfacePanel, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "MODE MANUAL (STROBO / TDC DARURAT)",
                            color = ElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Mempertahankan setup strobo/TDC lama untuk kondisi CDI OEM rusak atau mati total.",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Target Voltage Selector R8
            Spacer(Modifier.height(4.dp))
            Text(
                "TARGET TEGANGAN HV R8 (NORMAL 285 V / PRO 345 V)",
                color = SensorAmber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            CompactStatusRow("TEGANGAN TERPILIH", "$targetHv V (${if (isProVoltage) "PRO 345V" else "NORMAL 285V"})", true)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    enabled = !pending,
                    onClick = { viewModel.setHvVoltageMode(false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isProVoltage) MotecOrange else SurfacePanel
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(vertical = 5.dp)
                ) {
                    Text("NORMAL 285 V", color = if (!isProVoltage) CarbonDark else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    enabled = !pending,
                    onClick = { viewModel.setHvVoltageMode(true) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isProVoltage) ElectricCyan else SurfacePanel
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(vertical = 5.dp)
                ) {
                    Text("PRO 345 V", color = if (isProVoltage) CarbonDark else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PulserStage(viewModel: CdiViewModel, t: Telemetry, selectedPlatform: McuPlatform) {
    val pending by viewModel.setupCommandPending.collectAsState()

    StageBody {
        StageCard(
            title = "2 • VERIFIKASI PULSER",
            subtitle = "JP_HV tetap dilepas. Starter 2–3 detik; RPM harus terbaca dan kualitas pulser dianjurkan ≥10."
        ) {
            CompactStatusRow("RPM LIVE", "${t.rpm}", t.rpm > 0)
            CompactStatusRow("PULSER QUALITY", "${t.pickupQuality}/100", t.pickupQuality >= 10)

            McuPinGuidance(
                selectedPlatform = selectedPlatform,
                stmPin = "PA0 (H_BOTTOM.15 TIM2_CH1) via LM339/LM393",
                espPin = "GPIO4 (Kanan.32 Pin 32) via PC817 / LM393",
                warning = if (selectedPlatform == McuPlatform.ESP32_WROOM) "PA0 (GPIO4) wajib lewat optocoupler PC817 / LM393. Dilarang menyambungkan pulser 12V langsung!" else null
            )

            PulserAdvancedSettings(viewModel)
            Button(
                enabled = !pending,
                onClick = viewModel::confirmPulserPickup,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RacingLime),
                shape = RoundedCornerShape(8.dp)
            ) {
                PendingButtonText(pending, "KONFIRMASI PULSER & LANJUT TDC")
            }
        }
    }
}

@Composable
private fun OemLearnTdcCheckpointStage(viewModel: CdiViewModel, t: Telemetry, selectedPlatform: McuPlatform) {
    val pending by viewModel.setupCommandPending.collectAsState()
    val isOemLearning by viewModel.isOemLearning.collectAsState()
    val oemCenterPulses by viewModel.oemCenterPulses.collectAsState()
    val oemSideSamples by viewModel.oemSideSamples.collectAsState()
    val isOemUnpluggedConfirmed by viewModel.isOemUnpluggedConfirmed.collectAsState()
    val centerLabel = if (selectedPlatform == McuPlatform.STM32WB55) "PB3" else "GPIO16"
    val sideLabel = if (selectedPlatform == McuPlatform.STM32WB55) "PB4" else "GPIO17"

    StageBody {
        StageCard(
            title = "3 • CHECKPOINT REKAM TIMING OEM ($centerLabel/$sideLabel)",
            subtitle = "Jalur OEM Learn: Mesin dinyalakan menggunakan CDI bawaan motor. ${selectedPlatform.displayName} merekam pulsa pengapian secara pasif via $centerLabel & $sideLabel. Strobo flywheel manual tidak diperlukan."
        ) {
            CompactStatusRow("PULSA OEM CENTER ($centerLabel)", "$oemCenterPulses pulsa", oemCenterPulses > 0)
            CompactStatusRow("SAMPEL OEM SIDE ($sideLabel)", "$oemSideSamples sampel", oemSideSamples > 0)
            CompactStatusRow("STATUS PEREKAMAN", if (isOemLearning) "SEDANG MEREKAM DARI CDI OEM..." else if (oemCenterPulses > 0) "TEREKAM (${oemCenterPulses} pulsa)" else "SIAP REKAM", isOemLearning || oemCenterPulses > 0)

            McuPinGuidance(
                selectedPlatform = selectedPlatform,
                stmPin = "Center: PB3 (H_TOP.9) | Side: PB4 (H_TOP.8)",
                espPin = "Center: GPIO16 (Kanan.31 Pin 31) | Side: GPIO17 (Kanan.30 Pin 30)",
                warning = if (selectedPlatform == McuPlatform.ESP32_WROOM) "Wajib modul optocoupler PC817 terisolasi! Tegangan induksi koil bisa melonjak >600V!" else null
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    enabled = !isOemLearning && !pending,
                    onClick = viewModel::startOemLearn,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MotecOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("1. MULAI REKAM", color = CarbonDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    enabled = isOemLearning && !pending,
                    onClick = viewModel::stopOemLearn,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = RacingLime),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("2. SIMPAN & STOP", color = CarbonDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        StageCard(
            title = "CHECKPOINT: CABUT OUTPUT OEM",
            subtitle = "Setelah pulsa terekam, matikan mesin dan cabut soket kabel OEM dari koil. CDI ${if (selectedPlatform == McuPlatform.STM32WB55) "STM32" else "ESP32"} akan mengambil alih pengapian secara mandiri (Mode DIY)."
        ) {
            CompactStatusRow("STATUS SOKET OEM", if (isOemUnpluggedConfirmed) "TERCABUT (DIY MANDIRI AKTIF)" else "MENUNGGU PENCABUTAN", isOemUnpluggedConfirmed)

            if (!isOemUnpluggedConfirmed) {
                Button(
                    enabled = !pending,
                    onClick = viewModel::confirmOemUnplugged,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RacingLime),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("KONFIRMASI OEM_UNPLUGGED & AKTIFKAN DIY", color = CarbonDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfacePanel, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = RacingLime, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("OEM DILAPASKAN • Mode DIY Siap Pengujian", color = RacingLime, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        StageCard(
            title = "ALUR BERIKUTNYA",
            subtitle = "Lanjutkan kalibrasi TPS jika belum dilakukan, atau langsung ke pengujian First Start."
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.selectQuickSetupPage(SetupStage.TPS_CAL.code) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("4. KALIBRASI TPS", fontSize = 10.sp, color = MotecOrange)
                }
                Button(
                    enabled = isOemUnpluggedConfirmed,
                    onClick = {
                        viewModel.advanceSetupStage(SetupStage.FIRST_START.code)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isOemUnpluggedConfirmed) RacingLime else SurfacePanel),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("5. FIRST START", fontSize = 10.sp, color = if (isOemUnpluggedConfirmed) CarbonDark else TextMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TpsStage(viewModel: CdiViewModel, t: Telemetry, selectedPlatform: McuPlatform) {
    val pending by viewModel.setupCommandPending.collectAsState()
    val closed by viewModel.tpsClosedAdc.collectAsState()
    val open by viewModel.tpsOpenAdc.collectAsState()

    StageBody {
        StageCard(
            title = "4 • KALIBRASI TPS",
            subtitle = "Mesin mati dan kontak ON. Simpan posisi gas tertutup dahulu, kemudian buka grip penuh dan simpan posisi 100%."
        ) {
            CompactStatusRow("TPS LIVE", "%.1f %%".format(t.tps / 10f), true)
            CompactStatusRow("ADC TERTUTUP", "$closed", closed > 0)
            CompactStatusRow("ADC TERBUKA", "$open", open > closed + 50)

            McuPinGuidance(
                selectedPlatform = selectedPlatform,
                stmPin = "PA3 (H_BOTTOM.12) ADC TPS",
                espPin = "GPIO36 / VP (Kiri.2 ADC1_CH0)",
                warning = if (selectedPlatform == McuPlatform.ESP32_WROOM) "Gunakan HANYA ADC1 (GPIO 32-39). ADC2 nonaktif saat BLE hidup!" else null
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    enabled = !pending,
                    onClick = viewModel::calibrateTpsClosed,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfacePanel),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("1. GAS TUTUP", color = TextPrimary, fontSize = 10.sp) }
                Button(
                    enabled = !pending,
                    onClick = viewModel::calibrateTpsOpen,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MotecOrange),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("2. GAS PENUH", color = CarbonDark, fontSize = 10.sp) }
            }
            if (pending) {
                Text("Menunggu ACK MCU sebelum tombol berikutnya aktif.", color = SensorAmber, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun FirstStartStage(viewModel: CdiViewModel, t: Telemetry, selectedPlatform: McuPlatform) {
    val pending by viewModel.setupCommandPending.collectAsState()
    val firstStartHv by viewModel.firstStartHv.collectAsState()
    val ranLongEnough = t.firstStartSeconds >= 3
    val stoppedAndSafe = t.rpm == 0 && t.hvCenter < 30 && t.hvSide < 30
    val canSaveReady = ranLongEnough && stoppedAndSafe && !pending

    StageBody {
        StageCard(
            title = "5 • FIRST START AMAN (FIRMWARE R8)",
            subtitle = "Mode aman: 220V, CENTER saja, advance ≤10°, limiter 3.000 RPM. Di R8, status otomatis tersimpan setelah stabil 3 detik dan otomatis READY setelah mesin berhenti atau boot berikutnya."
        ) {
            CompactStatusRow("TARGET TEGANGAN", "$firstStartHv V", firstStartHv <= 220)
            CompactStatusRow("DURASI STABIL", "${t.firstStartSeconds} / 3 detik", ranLongEnough)
            CompactStatusRow("STATUS OTOMATIS R8", if (ranLongEnough) "TERPENUHI (≥3s) • OTOMATIS READY SAAT MATI" else "MENUNGGU STABIL (${t.firstStartSeconds}/3s)", ranLongEnough)
            CompactStatusRow("RPM SEKARANG", "${t.rpm}", t.rpm == 0)
            CompactStatusRow("HV CENTER / SIDE", "${t.hvCenter} / ${t.hvSide} V", stoppedAndSafe)

            McuPinGuidance(
                selectedPlatform = selectedPlatform,
                stmPin = "Gate SCR1 Center: PA1 (H_BOTTOM.10)",
                espPin = "Gate SCR1 Center: GPIO25 (Kiri.9 Pin 9)",
                warning = if (selectedPlatform == McuPlatform.ESP32_WROOM) "First Start hanya menyalakan Koil Center (GPIO25). Koil Side (GPIO26) nonaktif hingga siap." else null
            )

            Button(
                enabled = !pending,
                onClick = viewModel::prepareFirstStartMode,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MotecOrange),
                shape = RoundedCornerShape(8.dp)
            ) { PendingButtonText(pending, "SIAPKAN FIRST START") }
        }

        StageCard(
            title = "STATUS READY R8 (OTOMATIS / MANUAL)",
            subtitle = if (!ranLongEnough) {
                "Hidupkan mesin pada idle selama 3 detik. Firmware R8 akan otomatis mengunci kalibrasi aman."
            } else if (!stoppedAndSafe) {
                "Mesin telah stabil 3 detik! Matikan mesin (RPM 0 & HV <30 V) untuk transisi otomatis ke READY."
            } else {
                "Syarat terpenuhi. Sistem otomatis beralih ke READY (atau Anda dapat menekan simpan manual di bawah)."
            }
        ) {
            Button(
                enabled = canSaveReady,
                onClick = viewModel::confirmReadyCenterOnly,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RacingLime),
                shape = RoundedCornerShape(8.dp)
            ) { Text("SIMPAN READY • CENTER SAJA", color = CarbonDark, fontWeight = FontWeight.Bold) }
            Text(
                "Di Firmware R8: Setelah stabil 3 detik, saat mesin berhenti atau boot berikutnya CDI otomatis berstatus READY.",
                color = TextMuted,
                fontSize = 9.sp,
                lineHeight = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ReadyStage(viewModel: CdiViewModel, t: Telemetry, selectedPlatform: McuPlatform) {
    val pending by viewModel.setupCommandPending.collectAsState()

    StageBody {
        StageCard(
            title = "6 • CDI READY",
            subtitle = "Konfigurasi awal tersimpan di flash MCU ${selectedPlatform.displayName}. Boot berikutnya langsung memakai timing dan map tersimpan tanpa firmware lain."
        ) {
            CompactStatusRow("TARGET MCU", selectedPlatform.displayName, true)
            CompactStatusRow("STATUS", if (t.ready) "READY" else "MENUNGGU SYNC", t.ready)
            CompactStatusRow("KOIL CENTER", if (t.centerEnabled) "AKTIF" else "NONAKTIF", t.centerEnabled)
            CompactStatusRow("KOIL SIDE", if (t.sideEnabled) "AKTIF" else "NONAKTIF", !t.sideEnabled)

            McuPinGuidance(
                selectedPlatform = selectedPlatform,
                stmPin = "Center: PA1 (H_BOTTOM.10) | Side: PA2 (H_BOTTOM.11)",
                espPin = "Center: GPIO25 (Kiri.9 Pin 9) | Side: GPIO26 (Kiri.10 Pin 10)"
            )

            Button(
                onClick = { viewModel.setTab(ScreenTab.MAPS) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MotecOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Tune, null, tint = CarbonDark)
                Spacer(Modifier.width(7.dp))
                Text("BUKA MAP PENGAPIAN", color = CarbonDark)
            }
            OutlinedButton(
                enabled = !pending,
                onClick = viewModel::resetSetupWorkflow,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Refresh, null, tint = RaceRedline)
                Spacer(Modifier.width(7.dp))
                Text("RESET SELURUH SETUP", color = RaceRedline)
            }
        }
    }
}

@Composable
private fun CompactStatusRow(label: String, value: String, ok: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfacePanel, RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(
            value,
            color = if (ok) RacingLime else SensorAmber,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun PendingButtonText(pending: Boolean, text: String) {
    if (pending) {
        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = CarbonDark)
        Spacer(Modifier.width(7.dp))
        Text("MENUNGGU ACK MCU...", color = CarbonDark, fontSize = 10.sp)
    } else {
        Icon(Icons.Default.Check, null, tint = CarbonDark, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = CarbonDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Komponen visualisasi interaktif rangkaian pengaman suntik koil (Pin 6 & 12)
 * dan rangkaian daya penyalaan MCU (Pin 5) untuk Mode OEM Learn.
 * Mendukung WeAct STM32WB55 & ESP32-WROOM-32D dengan diagram presisi bebas wrap.
 */
@Composable
private fun OemLearnSafetyWiringGuide(selectedPlatform: McuPlatform) {
    var isExpanded by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val isStm = selectedPlatform == McuPlatform.STM32WB55
    val platformName = if (isStm) "STM32" else "ESP32"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CarbonDark.copy(alpha = 0.85f))
            .border(1.dp, if (selectedTab == 0) RacingLime.copy(alpha = 0.6f) else BorderSubtle, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header dengan tombol lipat (expand/collapse)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = SensorAmber,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "SKEMA RANGKAIAN PENGAMAN & WIRING ($platformName)",
                    color = SensorAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        if (isExpanded) {
            // Kotak Bahaya Tegangan Tinggi
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RaceRedline.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .border(1.dp, RaceRedline.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Warning, null, tint = RaceRedline, modifier = Modifier.size(15.dp))
                    Text(
                        text = "BAHAYA: TEGANGAN DISCHARGE KOIL 200V - 400V+",
                        color = RaceRedline,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = if (isStm) {
                        "DILARANG KERAS menyambung kabel Pin 12 (Center Coil) atau Pin 6 (Side Coil) langsung ke pin STM32! Tegangan induksi dapat melonjak >600V dan akan LANGSUNG MEMBAKAR mikrokontroler STM32WB55. Gunakan salah satu skema pengaman di bawah ini:"
                    } else {
                        "DILARANG KERAS menyambung kabel Pin 12 (Center Coil) atau Pin 6 (Side Coil) langsung ke pin ESP32! Tegangan induksi dapat melonjak >600V dan akan LANGSUNG MEMBAKAR mikrokontroler ESP32-WROOM. Gunakan salah satu skema pengaman di bawah ini:"
                    },
                    color = TextPrimary,
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Tab Selector Scrollable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                listOf(
                    0 to "1. MODUL PC817 4-CH (PLUG & PLAY)",
                    1 to "2. KATALOG MODUL PASARAN",
                    2 to "3. OPTO DISKRIT (SOLDER)",
                    3 to "4. DIVIDER + CLAMP",
                    4 to "5. DAYA $platformName (+12V)"
                ).forEach { (tabIdx, tabTitle) ->
                    val active = selectedTab == tabIdx
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { selectedTab = tabIdx },
                        color = if (active) ElectricCyan.copy(alpha = 0.2f) else SurfacePanel,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (active) ElectricCyan else BorderSubtle
                        )
                    ) {
                        Text(
                            text = tabTitle,
                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 7.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 8.5.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = if (active) ElectricCyan else TextSecondary
                        )
                    }
                }
            }

            // Konten Skema Sesuai Tab
            when (selectedTab) {
                0 -> {
                    // TAB 1: MODUL PC817 4-CHANNEL PLUG & PLAY
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfacePanel, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "MODUL OPTOCOUPLER PC817 4-CHANNEL ($platformName SIAP PAKAI)",
                            color = RacingLime,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Gunakan modul jadi di pasaran untuk mengisolasi tegangan tinggi koil OEM. Dilengkapi sekrup terminal baut, LED indikator pulsa, dan jumper pull-up 3.3V.",
                            color = TextSecondary,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        // Mobile Quick Wire Mapping Card (Scannable, fits any phone screen)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CarbonDark.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "RINGKASAN PINOUT CEPAT ($platformName):",
                                color = SensorAmber,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• IN1+ [R 47kΩ 2W] > J1.9 (OEM Ctr)", color = TextPrimary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text(if (isStm) "OUT1 > PB3 (H_TOP.9)" else "OUT1 > GPIO16 (Pin 31)", color = ElectricCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• IN1- > J1.11 (GND Massa)", color = TextPrimary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text(if (isStm) "OUT2 > PB4 (H_TOP.8)" else "OUT2 > GPIO17 (Pin 30)", color = ElectricCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• IN2+ [R 47kΩ 2W] > J1.8 (OEM Side)", color = TextPrimary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text(if (isStm) "VCC  > 3V3 WeAct" else "VCC  > 3V3 ESP32", color = RacingLime, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• IN2- > J1.11 (GND Massa)", color = TextPrimary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text(if (isStm) "GND  > GND WeAct" else "GND  > GND ESP32", color = RacingLime, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Text(
                                text = "• JUMPER JP1 & JP2: Pasang di posisi VCC (Pull-Up aktif 3.3V)",
                                color = TextMuted,
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Diagram Visual Monospace (Scrollable Horizontal, never wraps)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(CarbonDark)
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            val outHeader = if (isStm) "[TERMINAL OUTPUT STM32WB55] " else "[TERMINAL OUTPUT ESP32-WROOM]"
                            val out1 = if (isStm) "OUT1 ------> PB3 (H_TOP.9)  |" else "OUT1 ------> GPIO16 (Pin 31)|"
                            val out2 = if (isStm) "OUT2 ------> PB4 (H_TOP.8)  |" else "OUT2 ------> GPIO17 (Pin 30)|"
                            val vccPin = if (isStm) "VCC  ------> 3V3 (STM32)    |" else "VCC  ------> 3V3 (ESP Pin 1)|"
                            val gndPin = if (isStm) "GND  ------> GND (STM32)    |" else "GND  ------> GND (Pin 14/20)|"

                            val diagram = "+-------------------------------------------------------------+\n" +
                                    "|     MODUL OPTOCOUPLER PC817 4-CHANNEL ISOLATION BOARD       |\n" +
                                    "+-------------------------------+-----------------------------+\n" +
                                    "|  [TERMINAL INPUT KOIL OEM]    | $outHeader|\n" +
                                    "|                               |                             |\n" +
                                    "|  IN1+ --[ R 47k 2W ]--> J1.9  | $out1\n" +
                                    "|       (Kabel Tambahan OEM Ctr)|               (Pulsa Center)|\n" +
                                    "|  IN1- ----------------> J1.11 | $out2\n" +
                                    "|         (GND Motor Massa)     |               (Pulsa Side)  |\n" +
                                    "|                               |  OUT3 ------> (Cadangan)    |\n" +
                                    "|  IN2+ --[ R 47k 2W ]--> J1.8  |  OUT4 ------> (Cadangan)    |\n" +
                                    "|       (Kabel Tambahan OEM Side|                             |\n" +
                                    "|  IN2- ----------------> J1.11 | $vccPin\n" +
                                    "|         (GND Motor Massa)     | $gndPin\n" +
                                    "+-------------------------------+-----------------------------+\n" +
                                    "| [LED1] [LED2] [LED3] [LED4]   * Indikator Kedip Pulsa       |\n" +
                                    "| [JP1]  [JP2]  [JP3]  [JP4]    * Jumper Level (Set ke VCC)   |\n" +
                                    "+-------------------------------------------------------------+"

                            Text(
                                text = diagram,
                                color = ElectricCyan,
                                fontSize = 7.5.sp,
                                lineHeight = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                softWrap = false
                            )
                        }

                        Text(
                            text = "TUTORIAL SINGKAT & KEUNTUNGAN:\n" +
                                    "1. Beli di Toko Online: Cari 'Modul Optocoupler PC817 4-Channel' (kisaran Rp 12.000 - Rp 18.000).\n" +
                                    "2. Pangkas 85% Solderan: Kabel cukup dikupas dan dikencangkan dengan obeng pada terminal baut sekrup.\n" +
                                    "3. Verifikasi Visual Langsung: LED1 & LED2 onboard akan berkedip saat koil memercik, membuktikan sinyal masuk tanpa osiloskop.\n" +
                                    "4. PIN HARNESS J1.8 & J1.9: Di pabrik ditandai NC (kosong). Tambahkan 2 kabel probe pigtail ke pin J1.9 (OEM Center) dan pin J1.8 (OEM Side) untuk perekaman pasif.\n" +
                                    "5. WAJIB RESISTOR SERI 47kΩ 2W: Karena input koil mencapai 200V-400V, wajib pasang resistor 47kΩ 2 Watt pada kabel sebelum masuk ke IN1+ dan IN2+ agar modul tidak jebol!\n" +
                                    "6. Jumper JP1-JP2: Pasang jumper pada posisi VCC agar output pull-up aktif ke 3.3V $platformName.",
                            color = TextPrimary,
                            fontSize = 8.5.sp,
                            lineHeight = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                1 -> {
                    // TAB 2: KATALOG MODUL PASARAN PENGGANTI SELURUH BLOK SISTEM CDI
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfacePanel, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "KATALOG MODUL PASARAN (DROP-IN MODULAR $platformName)",
                            color = MotecOrange,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Setiap blok fungsi sistem CDI R8 dapat digantikan oleh modul siap pakai di pasaran. Jalur kabel harness 12-pin (J1) motor tetap dipertahankan:",
                            color = TextSecondary,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(CarbonDark)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (isStm) {
                                    "=== [1] MODUL BUCK STEP-DOWN DC-DC (LM2596 / MP1584EN) ===\n" +
                                    "• Menggantikan: Regulator linear panas & elko besar.\n" +
                                    "• Fungsi: Ubah +12V kontak (J1.5) > stabil 5.0V DC dingin untuk Logic WeAct STM32.\n" +
                                    "• Wiring: IN+ ke J1.5, IN- ke J1.11 (GND), OUT+ ke Pin 5V WeAct (H_BOTTOM.2), OUT- ke GND (H_BOTTOM.1).\n\n" +
                                    "=== [2] MODUL OPTOCOUPLER PC817 4-CHANNEL (REKOMENDASI UTAMA #1) ===\n" +
                                    "• Menggantikan: Desain diskrit solderan PC817 / voltage divider terpisah.\n" +
                                    "• Fungsi: Isolasi optik aman tegangan tinggi OEM Learn Center (J1.9 > PB3) dan Side (J1.8 > PB4).\n" +
                                    "• Fitur: Sekrup baut, LED indikator pulsa percikan koil, isolasi tegangan 5000V.\n\n" +
                                    "=== [3] MODUL RELAY 1-CHANNEL 5V + OPTOCOUPLER (REKOMENDASI UTAMA #2) ===\n" +
                                    "• Menggantikan: Rangkaian transistor diskrit BC547, resistor base, & dioda flyback.\n" +
                                    "• Fungsi: Driver relay kipas radiator J1.7 via pin PB5 (H_TOP.7) langsung.\n" +
                                    "• Wiring: VCC ke 5V LM2596, GND ke GND_STAR, IN ke PB5, COM ke J1.7, NO ke GND.\n\n" +
                                    "=== CATATAN MODUL YANG SUDAH DIUJI & DITOLAK (JANGAN DIGUNAKAN) ===\n" +
                                    "• Modul Boost 12V->300-1200V: Arus hanya 2-20mA (kurang untuk 3 busi 10k RPM butuh 100mA+) dan tidak bisa PWM firmware.\n" +
                                    "• Modul Bridge Rectifier Generik: Didesain untuk PLN 50/60Hz, panas drop pada switching 100kHz trafo ATX.\n" +
                                    "• Modul Sensor Tegangan: Rasio pembagi resistor tidak presisi untuk ADC 3.3V firmware."
                                } else {
                                    "=== [1] MODUL BUCK STEP-DOWN DC-DC (LM2596 / MP1584EN) ===\n" +
                                    "• Menggantikan: Regulator linear panas & elko besar.\n" +
                                    "• Fungsi: Ubah +12V kontak (J1.5) > stabil 5.0V DC dingin untuk Logic ESP32.\n" +
                                    "• Wiring: IN+ ke J1.5, IN- ke J1.11 (GND), OUT+ ke Pin 5V/VIN (Pin 19), OUT- ke GND (Pin 14/20).\n\n" +
                                    "=== [2] MODUL OPTOCOUPLER PC817 4-CHANNEL (REKOMENDASI UTAMA #1) ===\n" +
                                    "• Menggantikan: Desain diskrit solderan PC817 / voltage divider terpisah.\n" +
                                    "• Fungsi: Isolasi optik aman tegangan tinggi OEM Learn Center (J1.9 > GPIO16) dan Side (J1.8 > GPIO17).\n" +
                                    "• Fitur: Sekrup baut, LED indikator pulsa percikan koil, isolasi tegangan 5000V.\n\n" +
                                    "=== [3] MODUL RELAY 1-CHANNEL 5V + OPTOCOUPLER (REKOMENDASI UTAMA #2) ===\n" +
                                    "• Menggantikan: Rangkaian transistor diskrit BC547, resistor base, & dioda flyback.\n" +
                                    "• Fungsi: Driver relay kipas radiator J1.7 via pin GPIO13 (Pin 15) langsung.\n" +
                                    "• Wiring: VCC ke 5V LM2596, GND ke GND_STAR, IN ke GPIO13, COM ke J1.7, NO ke GND.\n\n" +
                                    "=== CATATAN MODUL YANG SUDAH DIUJI & DITOLAK (JANGAN DIGUNAKAN) ===\n" +
                                    "• Modul Boost 12V->300-1200V: Arus hanya 2-20mA (kurang untuk 3 busi 10k RPM butuh 100mA+) dan tidak bisa PWM firmware.\n" +
                                    "• Modul Bridge Rectifier Generik: Didesain untuk PLN 50/60Hz, panas drop pada switching 100kHz trafo ATX.\n" +
                                    "• Modul Sensor Tegangan: Rasio pembagi resistor tidak presisi untuk ADC 3.3V firmware."
                                },
                                color = RacingLime,
                                fontSize = 7.5.sp,
                                lineHeight = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "KESIMPULAN MODULARITAS v8.1 (NOL PCB CUSTOM):\n" +
                                    "• Cukup gunakan 2 modul jadi: Modul PC817 4-CH & Modul Relay 1-CH 5V.\n" +
                                    "• Jalur harness motor 12-pin (J1) tetap dipertahankan penuh, perakitan cepat bebas pusing!",
                            color = TextPrimary,
                            fontSize = 8.5.sp,
                            lineHeight = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                2 -> {
                    // TAB 3: OPTOCOUPLER ISOLASI TOTAL (SOLDER DISKRIT)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfacePanel, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "METODE 3: ISOLASI TOTAL DENGAN IC OPTOCOUPLER DISKRIT ($platformName)",
                            color = RacingLime,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Isolasi optik (cahaya) 100% melindungi $platformName dari spike tegangan tinggi CDI OEM.",
                            color = TextSecondary,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        // Diagram ASCII Optocoupler (Scrollable Horizontal)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(CarbonDark)
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            val centerMcu = if (isStm) "WeAct H_TOP.9 (Pin PB3)" else "ESP32 Pin 31 (GPIO16)"
                            val sideMcu = if (isStm) "WeAct H_TOP.8 (Pin PB4)" else "ESP32 Pin 30 (GPIO17)"
                            val vccMcu = if (isStm) "WeAct 3V3 (H_TOP.3)" else "ESP32 3V3 (Pin 1)"
                            val gndMcu = if (isStm) "WeAct H_TOP.1 (Pin GND)" else "ESP32 Pin 14 (GND)"

                            Text(
                                text = "=== [1] JALUR SUNTIK KOIL CENTER (J1.12 ke ${if (isStm) "PB3" else "GPIO16"}) ===\n" +
                                        "Harness J1.12 (Oranye) ---[ R 47kΩ 2W ]---> Pin 1 (Anoda PC817)\n" +
                                        "Harness J1.11 (GND)    ---[ Dioda 1N4148 ]-> Pin 2 (Katoda PC817)\n" +
                                        "                                         (Antiparalel Spike)\n" +
                                        "Pin 4 (Kolektor PC817) -+-> $centerMcu\n" +
                                        "                        |   (Monitor Pulsa Center OEM)\n" +
                                        "$vccMcu --- [ 4.7kΩ Pull-up ]\n" +
                                        "Pin 3 (Emitter PC817)  ---> $gndMcu\n\n" +
                                        "=== [2] JALUR SUNTIK KOIL SIDE (J1.6 ke ${if (isStm) "PB4" else "GPIO17"}) ===\n" +
                                        "Harness J1.6 (Hitam-M)  ---[ R 47kΩ 2W ]---> Pin 1 (Anoda PC817 #2)\n" +
                                        "Harness J1.11 (GND)    ---[ Dioda 1N4148 ]-> Pin 2 (Katoda PC817 #2)\n" +
                                        "Pin 4 (Kolektor PC817) -+-> $sideMcu\n" +
                                        "$vccMcu --- [ 4.7kΩ Pull-up ]\n" +
                                        "Pin 3 (Emitter PC817)  ---> $gndMcu",
                                color = ElectricCyan,
                                fontSize = 8.sp,
                                lineHeight = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                softWrap = false
                            )
                        }

                        // Daftar Komponen
                        Text(
                            text = "DAFTAR KOMPONEN DIBUTUHKAN:\n" +
                                    "• 2x IC Optocoupler PC817 / EL817 / 6N137\n" +
                                    "• 2x Resistor 47 kΩ (WAJIB DAYA BESAR: 2 Watt Metal Film)\n" +
                                    "• 2x Dioda 1N4148 (Dipasang antiparalel antara Pin 1 & 2 Optocoupler)\n" +
                                    "• 2x Resistor 4.7 kΩ 0.25W (Pull-up ke 3V3 $platformName)",
                            color = TextPrimary,
                            fontSize = 8.5.sp,
                            lineHeight = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                3 -> {
                    // TAB 4: VOLTAGE DIVIDER + BAT54S CLAMP
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfacePanel, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "METODE 4: VOLTAGE DIVIDER + CLAMP DIODA ($platformName ALTERNATIF)",
                            color = SensorAmber,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Membagi tegangan dari ~300V menjadi ~3.0V dengan dioda clamp pengaman ke 3.3V.",
                            color = TextSecondary,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        // Diagram ASCII Divider (Scrollable Horizontal)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(CarbonDark)
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            val vInCenter = if (isStm) "PB3 (H_TOP.9)" else "GPIO16 (Pin 31)"
                            val vInSide = if (isStm) "PB4 (H_TOP.8)" else "GPIO17 (Pin 30)"
                            val vClamp = if (isStm) "3V3 (H_TOP.3)" else "3V3 (Pin 1)"

                            Text(
                                text = "=== JALUR CENTER (J1.12 ke ${if (isStm) "PB3" else "GPIO16"}) ===\n" +
                                        "J1.12 (Oranye) ---> [ R1: 100kΩ 1W-2W ] ---+--- [ R3: 1kΩ ] ---> $vInCenter\n" +
                                        "                                           |\n" +
                                        "                                     [ R2: 1.2kΩ ]\n" +
                                        "                                           |\n" +
                                        "                                     J1.11 (GND_STAR)\n" +
                                        "                                           |\n" +
                                        "                                  [ Dioda BAT54S Clamp ]\n" +
                                        "                                  (Katoda ke $vClamp, Anoda ke $vInCenter)\n\n" +
                                        "=== JALUR SIDE (J1.6 ke ${if (isStm) "PB4" else "GPIO17"}) ===\n" +
                                        "J1.6 (Hitam-M) ---> [ R1: 100kΩ 1W-2W ] ---+--- [ R3: 1kΩ ] ---> $vInSide\n" +
                                        "                                           +--- R2 (1.2k) & Clamp ke GND/$vClamp",
                                color = MotecOrange,
                                fontSize = 8.sp,
                                lineHeight = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                softWrap = false
                            )
                        }

                        Text(
                            text = "DAFTAR KOMPONEN DIBUTUHKAN:\n" +
                                    "• 2x Resistor 100 kΩ (1 Watt atau 2 Watt Metal Film)\n" +
                                    "• 2x Resistor 1.2 kΩ (0.25 Watt)\n" +
                                    "• 2x Resistor 1 kΩ (0.25 Watt seri pengaman gerbang MCU)\n" +
                                    "• 2x Dioda Schottky BAT54S / BAT85 (Clamp cepat batas tegangan 3.3V)",
                            color = TextPrimary,
                            fontSize = 8.5.sp,
                            lineHeight = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                4 -> {
                    // TAB 5: CATU DAYA PENYALAAN SAAT MESIN HIDUP DENGAN CDI OEM
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfacePanel, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "SUMBER TEGANGAN PENYALAAN $platformName (+12V KONTAK KE +5V DC)",
                            color = ElectricCyan,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$platformName dan BLE harus menyala saat kunci kontak ON agar aplikasi dapat berkomunikasi dan merekam pulsa saat mesin motor hidup dengan CDI OEM.",
                            color = TextSecondary,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        // Diagram ASCII Power Supply (Scrollable Horizontal)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(CarbonDark)
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            val vout5Pin = if (isStm) "WeAct Pin 5V (H_BOTTOM.2)\n                                                                             atau Port USB-C" else "ESP32 Pin 5V/VIN (Pin 19)\n                                                                             atau Port USB"
                            val voutGndPin = if (isStm) "WeAct Pin GND (H_TOP.1 / H_BOTTOM.1)" else "ESP32 Pin GND (Pin 14 / Pin 20)"

                            Text(
                                text = "Harness J1.5 (Cokelat / +12V Kontak) ---> [ Sekring 2A ] ---> [ VIN+ ]\n" +
                                        "                                                               Modul Step-Down\n" +
                                        "                                                               DC-DC Buck (5V)\n" +
                                        "                                                              (LM2596 / MP1584)\n" +
                                        "                                                               [ VOUT+ (5.0V) ] ---> $vout5Pin\n" +
                                        "Harness J1.11 (Hitam-Kuning / GND)   ------------------------> [ VIN- / GND ]\n" +
                                        "                                                               [ VOUT- (GND) ]  ---> $voutGndPin",
                                color = RacingLime,
                                fontSize = 8.sp,
                                lineHeight = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                softWrap = false
                            )
                        }

                        Text(
                            text = "LANGKAH KONEKSI DAYA SAAT OEM LEARN:\n" +
                                    "1. Sambungkan input regulator step-down ke kabel Kontak J1.5 (+12V) dan Massa J1.11 (GND).\n" +
                                    "2. Pastikan tegangan output regulator disetel stabil di 5.0 Volt DC.\n" +
                                    "3. Hubungkan output 5.0V ke Pin ${if (isStm) "5V WeAct STM32 (H_BOTTOM.2)" else "5V/VIN ESP32 (Pin 19)"} (atau colokkan kabel USB).\n" +
                                    "4. Pastikan Pin GND $platformName terhubung ke GND_STAR motor (J1.11).\n" +
                                    "5. Saat kontak motor diputar ke ON:\n" +
                                    "   • CDI bawaan motor mendapat daya normal.\n" +
                                    "   • $platformName menyala, Bluetooth BLE menyala.\n" +
                                    "   • Buka aplikasi di HP, hubungkan BLE, pilih OEM LEARN, lalu hidupkan mesin!",
                            color = TextPrimary,
                            fontSize = 8.5.sp,
                            lineHeight = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

