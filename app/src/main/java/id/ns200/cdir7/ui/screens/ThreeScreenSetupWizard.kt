package id.ns200.cdir7.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
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
import id.ns200.cdir7.FirmwareRunMode
import id.ns200.cdir7.HardwareModule
import id.ns200.cdir7.ScreenTab
import id.ns200.cdir7.SessionPhase
import id.ns200.cdir7.ui.components.MotecButton
import id.ns200.cdir7.ui.theme.*

enum class SetupWizardTab(val index: Int, val title: String, val subtitle: String) {
    PEMASANGAN(0, "1. PEMASANGAN", "Core / Dual / Lanjutan"),
    PEMERIKSAAN(1, "2. PEMERIKSAAN", "Pulser • TDC • TPS"),
    FIRST_START_READY(2, "3. FIRST START / READY", "Uji Pertama & Kunci Flash")
}

@Composable
fun ThreeScreenSetupWizard(viewModel: CdiViewModel) {
    val commissionStatus by viewModel.commissionStatus.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val moduleStatus by viewModel.moduleStatus.collectAsState()
    val setupCanWrite by viewModel.setupCanWrite.collectAsState()
    val writeBlockReason by viewModel.setupWriteBlockReason.collectAsState()
    val sessionPhase by viewModel.sessionPhase.collectAsState()
    val isSimulationMode by viewModel.isSimulationMode.collectAsState()

    // Tab state: default matches commissionStatus stage if available
    var selectedTab by remember { mutableStateOf(SetupWizardTab.PEMASANGAN) }

    LaunchedEffect(commissionStatus.stage) {
        when (commissionStatus.stage) {
            0 -> selectedTab = SetupWizardTab.PEMASANGAN
            in 1..3 -> if (selectedTab == SetupWizardTab.PEMASANGAN) selectedTab = SetupWizardTab.PEMERIKSAAN
            else -> if (selectedTab != SetupWizardTab.FIRST_START_READY) selectedTab = SetupWizardTab.FIRST_START_READY
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
    ) {
        // Quick access thermal / fan header
        ThermalFanQuickAccess(viewModel)

        // Top Wizard Navigation Bar (3 Screens)
        ThreeScreenHeader(
            selectedTab = selectedTab,
            commissionStage = commissionStatus.stage,
            isReady = commissionStatus.ready,
            isDemo = isSimulationMode,
            onResetDemo = viewModel::resetDemoCommissioning,
            onSelectTab = { selectedTab = it }
        )

        // Command Guard safety warning banner
        if (!setupCanWrite && writeBlockReason != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                color = RaceRedline.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, RaceRedline.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = RaceRedline,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "COMMAND GUARD • setup_can_write() DIBLOKIR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = RaceRedline,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = writeBlockReason ?: "",
                            fontSize = 8.5.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (sessionPhase == SessionPhase.NEEDS_BINDING || sessionPhase == SessionPhase.READY_READ_ONLY) {
                        Spacer(modifier = Modifier.width(6.dp))
                        MotecButton(
                            text = "BINDING",
                            onClick = { viewModel.setTab(ScreenTab.BLE) },
                            color = SensorAmber,
                            height = 26.dp
                        )
                    }
                }
            }
        }

        // Active Screen Body
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                SetupWizardTab.PEMASANGAN -> LayarPemasangan(
                    viewModel = viewModel,
                    onNextScreen = { selectedTab = SetupWizardTab.PEMERIKSAAN }
                )
                SetupWizardTab.PEMERIKSAAN -> LayarPemeriksaan(
                    viewModel = viewModel,
                    onNextScreen = { selectedTab = SetupWizardTab.FIRST_START_READY }
                )
                SetupWizardTab.FIRST_START_READY -> LayarFirstStartReady(
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun ThreeScreenHeader(
    selectedTab: SetupWizardTab,
    commissionStage: Int,
    isReady: Boolean,
    isDemo: Boolean,
    onResetDemo: () -> Unit,
    onSelectTab: (SetupWizardTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle),
        color = SurfacePanel
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "SETUP CDI TIGA LAYAR",
                        color = MotecOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = if (isReady) RacingLime.copy(alpha = 0.2f) else SensorAmber.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isReady) "READY (FLASH TERKUNCI)" else "TAHAP $commissionStage",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isReady) RacingLime else SensorAmber,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isDemo) {
                        MotecButton(
                            text = "RESET DEMO",
                            onClick = onResetDemo,
                            color = SensorAmber,
                            height = 24.dp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = CardBackground,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = if (isDemo) "MODE DEMO" else "FIRMWARE R9.2 (ESP32)",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDemo) SensorAmber else TextSecondary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 3 Screen Selector Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SetupWizardTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .clickable { onSelectTab(tab) },
                        color = if (isSelected) MotecOrange.copy(alpha = 0.22f) else CardBackground,
                        shape = RoundedCornerShape(3.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MotecOrange else BorderSubtle
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = tab.title,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = tab.subtitle,
                                fontSize = 7.5.sp,
                                color = if (isSelected) MotecOrange else TextMuted,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// LAYAR 1 — PEMASANGAN
// =========================================================================
@Composable
private fun LayarPemasangan(
    viewModel: CdiViewModel,
    onNextScreen: () -> Unit
) {
    val moduleStatus by viewModel.moduleStatus.collectAsState()
    val commissionStatus by viewModel.commissionStatus.collectAsState()
    val setupCanWrite by viewModel.setupCanWrite.collectAsState()
    val firmwareMode by viewModel.firmwareMode.collectAsState()

    var oemRemovedChecked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Introduction & Harness pinout guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePanel),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "LAYAR 1: PEMASANGAN HARNESS & KOIL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MotecOrange,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Pilih konfigurasi instalasi fisik sesuai paket hardware yang terpasang pada motor NS200 / DTS-i.",
                    fontSize = 9.5.sp,
                    color = TextSecondary,
                    lineHeight = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = CardBackground,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("J1.12 CORE", fontSize = 8.sp, color = RacingLime, fontWeight = FontWeight.Bold)
                            Text("Koil CENTER (Busi Tengah Utama)", fontSize = 8.5.sp, color = TextPrimary)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = CardBackground,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("J1.6 SIDE", fontSize = 8.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                            Text("Koil SIDE (Busi Kiri & Kanan)", fontSize = 8.5.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Hardware Terpasang: lima switch independen sesuai MODULES R9.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePanel),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "HARDWARE TERPASANG",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = ElectricCyan,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Aktifkan hanya modul fisik yang benar-benar dipasang. Setiap perubahan dikirim satu per satu dan diverifikasi ulang melalui MODULES.",
                    fontSize = 8.5.sp,
                    color = TextSecondary,
                    lineHeight = 11.sp
                )
                HardwareModule.entries.forEach { module ->
                    val installed = moduleStatus.isInstalled(module)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBackground, RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = module.title,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (installed) RacingLime else TextPrimary
                            )
                            Text(
                                text = module.pinInfo,
                                fontSize = 7.5.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Switch(
                            checked = installed,
                            onCheckedChange = { viewModel.toggleModuleInstalled(module) },
                            enabled = setupCanWrite,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = RacingLime,
                                checkedTrackColor = RacingLime.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }
        }

        // Checklist Konfirmasi CDI OEM
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (oemRemovedChecked) RacingLime.copy(alpha = 0.1f) else SurfacePanel,
            border = BorderStroke(1.dp, if (oemRemovedChecked) RacingLime else SensorAmber),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { oemRemovedChecked = !oemRemovedChecked }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = oemRemovedChecked,
                    onCheckedChange = { oemRemovedChecked = it },
                    colors = CheckboxDefaults.colors(checkedColor = RacingLime)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "KONFIRMASI: CDI OEM Bawaan Sudah Dilepas",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (oemRemovedChecked) RacingLime else TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Soket CDI motor terhubung langsung ke CDI IgniTra (OEM_REMOVED).",
                        fontSize = 8.5.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Pilihan 1: Ganti CDI OEM — Core 1 Coil (Default)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePanel),
            border = BorderStroke(1.dp, if (commissionStatus.stage >= 1 && moduleStatus.coreProfile == 0) RacingLime else BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. GANTI CDI OEM — CORE (1 COIL)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = ElectricCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = RacingLime.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "DEFAULT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = RacingLime,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Hanya menggunakan Koil CENTER pada Pin J1.12. Jalur SIDE tidak difiring. Cocok untuk pengujian dasar atau motor bersistem 1 busi/CDI standar.",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    lineHeight = 12.sp
                )
                MotecButton(
                    text = "KONFIRMASI PASANG CORE 1-COIL",
                    onClick = {
                        viewModel.installCoreOemRemoved()
                    },
                    enabled = oemRemovedChecked && setupCanWrite,
                    color = ElectricCyan,
                    height = 36.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Pilihan 2: Ganti CDI OEM — Dual Coil (jika SIDE dipasang)
        val isSideInstalled = moduleStatus.isInstalled(HardwareModule.SIDE)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePanel),
            border = BorderStroke(1.dp, if (commissionStatus.stage >= 1 && moduleStatus.coreProfile == 1) RacingLime else BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. GANTI CDI OEM — DUAL COIL",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = MotecOrange,
                        fontFamily = FontFamily.Monospace
                    )
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = if (isSideInstalled) RacingLime.copy(alpha = 0.15f) else SensorAmber.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isSideInstalled) "MODUL SIDE TERPASANG" else "MODUL SIDE OFF",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSideInstalled) RacingLime else SensorAmber,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Mengaktifkan firing ganda: J1.12 Koil CENTER dan J1.6 Koil SIDE (Busi Kiri/Kanan DTS-i). Modul independen SIDE harus diaktifkan.",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    lineHeight = 12.sp
                )

                if (!isSideInstalled) {
                    MotecButton(
                        text = "PASANG MODUL HARDWARE SIDE TERLEBIH DAHULU",
                        onClick = { viewModel.toggleModuleInstalled(HardwareModule.SIDE) },
                        color = SensorAmber,
                        height = 32.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                MotecButton(
                    text = "KONFIRMASI PASANG DUAL COIL",
                    onClick = {
                        viewModel.installDualOemRemoved()
                    },
                    enabled = oemRemovedChecked && isSideInstalled && setupCanWrite,
                    color = MotecOrange,
                    height = 36.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Pilihan 3: Setup Lanjutan (OEM Learn / Manual / DIY)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePanel),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "3. SETUP LANJUTAN (OEM LEARN / MANUAL / DIY)",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = SparkAmber,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Pilih mode kerja firmware jika Anda ingin merekam CDI bawaan motor atau konfigurasi tuning mandiri.",
                    fontSize = 9.sp,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FirmwareRunMode.entries.forEach { mode ->
                        val isModeSelected = firmwareMode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { viewModel.setFirmwareMode(mode) },
                            color = if (isModeSelected) SparkAmber.copy(alpha = 0.2f) else CardBackground,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, if (isModeSelected) SparkAmber else BorderSubtle)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = mode.label,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isModeSelected) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Navigation button to Layar 2
        MotecButton(
            text = "LANJUT KE LAYAR 2: PEMERIKSAAN SENSOR",
            onClick = onNextScreen,
            color = RacingLime,
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            height = 38.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// =========================================================================
// LAYAR 2 — PEMERIKSAAN
// =========================================================================
@Composable
private fun LayarPemeriksaan(
    viewModel: CdiViewModel,
    onNextScreen: () -> Unit
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val commissionStatus by viewModel.commissionStatus.collectAsState()
    val adcReadings by viewModel.adcReadings.collectAsState()
    val pickupEdge by viewModel.pickupEdge.collectAsState()
    val gateDurationUs by viewModel.gateDurationUs.collectAsState()
    val pulserOffsetDeg by viewModel.pulserOffsetDeg.collectAsState()
    val tpsClosedAdc by viewModel.tpsClosedAdc.collectAsState()
    val tpsOpenAdc by viewModel.tpsOpenAdc.collectAsState()
    val setupCanWrite by viewModel.setupCanWrite.collectAsState()

    var manualOffsetInput by remember { mutableStateOf(pulserOffsetDeg) }
    val tpsSpan = tpsOpenAdc - tpsClosedAdc
    val isTpsCalibrated = tpsSpan >= 1200

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // -------------------------------------------------------------
        // KARTU 1: PICKUP PULSER
        // -------------------------------------------------------------
        val isPickupDetected = telemetry.rpm > 0 || telemetry.pickupQuality > 0
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePanel),
            border = BorderStroke(1.dp, if (isPickupDetected) RacingLime else SensorAmber)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. PEMERIKSAAN PICKUP PULSER",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = MotecOrange,
                        fontFamily = FontFamily.Monospace
                    )
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = if (isPickupDetected) RacingLime.copy(alpha = 0.2f) else SensorAmber.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isPickupDetected) "TERDETEKSI" else "BELUM TERDETEKSI",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPickupDetected) RacingLime else SensorAmber,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Advisory Alert jika ada
                if (commissionStatus.pickupAdvisory) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = RaceRedline.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, RaceRedline),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = RaceRedline, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "ADVISORY: Polaritas pickup terbalik atau noise pulser tinggi! Balik polaritas (FALLING/RISING).",
                                fontSize = 8.sp,
                                color = RaceRedline,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Data values tile
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    InspectionDataBox(Modifier.weight(1f), "RAW US", "${gateDurationUs} µs")
                    InspectionDataBox(Modifier.weight(1f), "POLARITAS", pickupEdge)
                    InspectionDataBox(Modifier.weight(1f), "CRANKING RPM", "${telemetry.rpm} RPM")
                    InspectionDataBox(Modifier.weight(1f), "NOISE / KUALITAS", "${telemetry.pickupQuality}%")
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MotecButton(
                        text = "BALIK TEPI POLARITAS",
                        onClick = {
                            val newEdge = if (pickupEdge.equals("FALLING", ignoreCase = true)) "RISING" else "FALLING"
                            viewModel.setPulserEdge(newEdge)
                        },
                        enabled = setupCanWrite,
                        color = SensorAmber,
                        height = 34.dp,
                        modifier = Modifier.weight(1f)
                    )
                    MotecButton(
                        text = "SIMPAN PICKUP",
                        onClick = viewModel::confirmPulserPickup,
                        enabled = setupCanWrite,
                        color = RacingLime,
                        height = 34.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // KARTU 2: KALIBRASI TDC
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePanel),
            border = BorderStroke(1.dp, if (!commissionStatus.tdcAdvisory) RacingLime else BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. KALIBRASI TITIK MATI ATAS (TDC)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = ElectricCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "TRIGGER: ${telemetry.triggerCdeg / 100f}° BTDC",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (commissionStatus.tdcAdvisory) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SensorAmber.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SensorAmber),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = SensorAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "ADVISORY: Sudut referensi TDC belum terverifikasi! Gunakan strobo J1.18 atau input manual.",
                                fontSize = 8.sp,
                                color = SensorAmber,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InspectionDataBox(Modifier.weight(1f), "TRIGGER DASAR", "${telemetry.triggerCdeg / 100f}° BTDC")
                    InspectionDataBox(Modifier.weight(1f), "OFFSET MANUAL", "${manualOffsetInput}°")
                }

                // Offset stepper buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GESER:", fontSize = 8.5.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    listOf(-1.0f, -0.5f, 0.0f, 0.5f, 1.0f).forEach { step ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(3.dp))
                                .clickable {
                                    manualOffsetInput = if (step == 0f) 0f else (manualOffsetInput + step).coerceIn(-5f, 5f)
                                },
                            color = CardBackground,
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = if (step > 0) "+$step°" else if (step == 0f) "0°" else "$step°",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (step == 0f) MotecOrange else TextPrimary,
                                modifier = Modifier.padding(vertical = 4.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MotecButton(
                        text = "AKTIFKAN STROBO (AUX)",
                        onClick = { viewModel.sendRawCommand("SETUP,STROBE,ON") },
                        enabled = setupCanWrite,
                        color = SparkAmber,
                        height = 34.dp,
                        modifier = Modifier.weight(1f)
                    )
                    MotecButton(
                        text = "SIMPAN STROBO",
                        onClick = viewModel::saveTdcStrobe,
                        enabled = setupCanWrite,
                        color = ElectricCyan,
                        height = 34.dp,
                        modifier = Modifier.weight(1f)
                    )
                }

                MotecButton(
                    text = "SIMPAN TDC MANUAL (${manualOffsetInput}° BTDC)",
                    onClick = { viewModel.saveManualTdc(manualOffsetInput) },
                    enabled = setupCanWrite,
                    color = RacingLime,
                    height = 34.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // -------------------------------------------------------------
        // KARTU 3: TPS BASELINE
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePanel),
            border = BorderStroke(1.dp, if (isTpsCalibrated) RacingLime else BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. TPS BASELINE (0% - 100%)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = SparkAmber,
                        fontFamily = FontFamily.Monospace
                    )
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = if (isTpsCalibrated) RacingLime.copy(alpha = 0.2f) else SensorAmber.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isTpsCalibrated) "TERKALIBRASI" else "BELUM (OPSIONAL)",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTpsCalibrated) RacingLime else SensorAmber,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                if (commissionStatus.tpsAdvisory || (tpsSpan in 1..1199)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SensorAmber.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SensorAmber),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = SensorAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "ADVISORY: Bentang TPS < 1200 ADC count (${tpsSpan}). Pastikan sensor TPS terhubung dengan baik.",
                                fontSize = 8.sp,
                                color = SensorAmber,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    InspectionDataBox(Modifier.weight(1f), "ADC AKTIF", "${adcReadings.tpsRaw}")
                    InspectionDataBox(Modifier.weight(1f), "0% ADC", "$tpsClosedAdc")
                    InspectionDataBox(Modifier.weight(1f), "100% ADC", "$tpsOpenAdc")
                    InspectionDataBox(Modifier.weight(1f), "BENTANG SPAN", "$tpsSpan")
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MotecButton(
                        text = "SET TUTUP (0%)",
                        onClick = viewModel::calibrateTpsClosed,
                        enabled = setupCanWrite,
                        color = SparkAmber,
                        height = 34.dp,
                        modifier = Modifier.weight(1f)
                    )
                    MotecButton(
                        text = "SET BUKA (100%)",
                        onClick = viewModel::calibrateTpsOpen,
                        enabled = setupCanWrite,
                        color = RacingLime,
                        height = 34.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Navigation button to Layar 3
        MotecButton(
            text = "LANJUT KE LAYAR 3: FIRST START & READY",
            onClick = onNextScreen,
            color = RacingLime,
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            height = 38.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// =========================================================================
// LAYAR 3 — FIRST START DAN READY
// =========================================================================
@Composable
private fun LayarFirstStartReady(
    viewModel: CdiViewModel
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val commissionStatus by viewModel.commissionStatus.collectAsState()
    val moduleStatus by viewModel.moduleStatus.collectAsState()
    val setupCanWrite by viewModel.setupCanWrite.collectAsState()
    val isReady = commissionStatus.ready

    val isEngineStopped = telemetry.rpm == 0
    val isHvDischarged = telemetry.hvCenter < 30 && telemetry.hvSide < 30
    val canTriggerFirstStart = isEngineStopped && isHvDischarged && setupCanWrite

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Syarat Keselamatan Awal
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePanel),
            border = BorderStroke(1.dp, if (isEngineStopped && isHvDischarged) RacingLime else RaceRedline)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SYARAT KESELAMATAN SEBELUM FIRST START",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = MotecOrange,
                    fontFamily = FontFamily.Monospace
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SafetyCheckBadge(Modifier.weight(1f), "MESIN MATI (RPM 0)", "${telemetry.rpm} RPM", isEngineStopped)
                    SafetyCheckBadge(Modifier.weight(1f), "HV DISCHARGE (<30V)", "C:${telemetry.hvCenter}V S:${telemetry.hvSide}V", isHvDischarged)
                }
            }
        }

        // Langkah 1: Aktivasi First Start
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePanel),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "LANGKAH 1: AKTIFKAN MODE AMAN FIRST START",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = ElectricCyan,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "CDI masuk mode proteksi: Kapasitor dibatasi 220V, hanya Koil CENTER aktif, advance pengapian dikunci max 10°, rev limiter 3.000 RPM.",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    lineHeight = 12.sp
                )
                MotecButton(
                    text = "KIRIM PERINTAH FIRST START",
                    onClick = viewModel::prepareFirstStartMode,
                    enabled = canTriggerFirstStart,
                    color = ElectricCyan,
                    height = 36.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Langkah 2: Monitoring Uji Hidup Mesin
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePanel),
            border = BorderStroke(1.dp, if (telemetry.rpm > 500) RacingLime else BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "LANGKAH 2: HIDUPKAN MESIN (STABIL MINIMAL 3 DETIK)",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = SparkAmber,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Nyalakan mesin tanpa memutar selongsong gas berlebihan. Amati kestabilan idle selama minimal 3 detik.",
                    fontSize = 9.sp,
                    color = TextSecondary
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InspectionDataBox(Modifier.weight(1f), "PUTARAN MESIN", "${telemetry.rpm} RPM")
                    InspectionDataBox(Modifier.weight(1f), "TEGANGAN HV", "${telemetry.hvCenter} V")
                    InspectionDataBox(Modifier.weight(1f), "ADVANCE PENGAPIAN", "${telemetry.advanceCdeg / 100f}°")
                }
            }
        }

        // Langkah 3: Matikan Mesin & Kunci Komisi Final
        val canFinalizeReady = isEngineStopped && isHvDischarged && setupCanWrite
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePanel),
            border = BorderStroke(1.dp, if (isReady) RacingLime else BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "LANGKAH 3: SIMPAN KOMISI FINAL (KUNCI FLASH)",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = RacingLime,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Matikan mesin terlebih dahulu hingga RPM 0 dan HV <30V. Kemudian pilih tombol komisi sesuai konfigurasi koil:",
                    fontSize = 9.sp,
                    color = TextSecondary
                )

                MotecButton(
                    text = "SIMPAN READY • CORE (1 COIL)",
                    onClick = viewModel::confirmReadyCenterOnly,
                    enabled = canFinalizeReady,
                    color = ElectricCyan,
                    height = 36.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                val isSideEnabled = moduleStatus.isInstalled(HardwareModule.SIDE)
                MotecButton(
                    text = "SIMPAN READY • DUAL COIL (SIDE AKTIF)",
                    onClick = { viewModel.confirmReadyDual(0) },
                    enabled = canFinalizeReady && isSideEnabled,
                    color = MotecOrange,
                    height = 36.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Status Final: Hanya ditampilkan jika COMMISSION.ready == 1
        if (isReady) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = RacingLime.copy(alpha = 0.15f),
                border = BorderStroke(1.5.dp, RacingLime),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = RacingLime,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "KOMISI CDI SELESAI & AMAN JALAN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = RacingLime,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Seluruh kalibrasi pulser, TDC, dan koil telah terkunci permanen di memori Flash ESP32.",
                        fontSize = 9.sp,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    if (viewModel.isSimulationMode.collectAsState().value || !viewModel.isConnected.collectAsState().value) {
                        Spacer(modifier = Modifier.height(4.dp))
                        MotecButton(
                            text = "RESET SIMULASI DEMO KE AWAL",
                            onClick = viewModel::resetDemoCommissioning,
                            color = SensorAmber,
                            height = 32.dp
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// UI HELPER COMPONENTS
// -------------------------------------------------------------
@Composable
private fun InspectionDataBox(
    modifier: Modifier,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        color = CardBackground,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun SafetyCheckBadge(
    modifier: Modifier,
    label: String,
    value: String,
    ok: Boolean
) {
    Surface(
        modifier = modifier,
        color = if (ok) RacingLime.copy(alpha = 0.12f) else RaceRedline.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, if (ok) RacingLime else RaceRedline)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (ok) RacingLime else RaceRedline,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
