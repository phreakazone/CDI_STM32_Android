package id.ns200.cdir7.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.data.WiringDataProvider
import id.ns200.cdir7.CdiViewModel
import id.ns200.cdir7.McuPlatform
import id.ns200.cdir7.ScreenTab
import id.ns200.cdir7.SetupStage
import id.ns200.cdir7.ui.components.MotecButton
import id.ns200.cdir7.ui.theme.*

data class HarnessPinItem(
    val pin: String,
    val wireColor: String,
    val name: String,
    val target: String,
    val description: String,
    val status: String,
    val isWarning: Boolean = false,
    val isConfirmed: Boolean = false
)

data class WeActPinItem(
    val pin: String,
    val mcuPin: String,
    val function: String,
    val net: String,
    val note: String,
    val isWarning: Boolean = false
)

data class BomItem(
    val section: String,
    val ref: String,
    val qty: String,
    val component: String,
    val source: String,
    val note: String
)

@Composable
fun QuickSetupGuideScreen(viewModel: CdiViewModel) {
    val telemetry by viewModel.telemetry.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val selectedPlatform by viewModel.selectedPlatform.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Alur Quick Setup, 1: Harness J1, 2: Pinout MCU, 3: BOM / Belanja

    val j1ConfirmedMap by viewModel.j1ConfirmedMap.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "QUICK SETUP & WIRING",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = MotecOrange
                )
                Text(
                    text = "NS200-CDI • ${selectedPlatform.displayName}",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        when (telemetry.setupStage) {
                            SetupStage.READY.code -> RacingLime.copy(alpha = 0.2f)
                            SetupStage.FIRST_START.code -> MotecOrange.copy(alpha = 0.2f)
                            else -> SensorAmber.copy(alpha = 0.2f)
                        }
                    )
                    .border(
                        1.dp,
                        when (telemetry.setupStage) {
                            SetupStage.READY.code -> RacingLime
                            SetupStage.FIRST_START.code -> MotecOrange
                            else -> SensorAmber
                        },
                        RoundedCornerShape(3.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "TAHAP: ${telemetry.stage.label}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (telemetry.setupStage) {
                        SetupStage.READY.code -> RacingLime
                        SetupStage.FIRST_START.code -> MotecOrange
                        else -> SensorAmber
                    },
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Sub-Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "1. Alur",
                "2. J1",
                "3. MCU",
                "4. BOM",
                "5. Modul"
            ).forEachIndexed { index, title ->
                val isSel = selectedTab == index
                MotecButton(
                    text = title,
                    onClick = { selectedTab = index },
                    color = if (isSel) MotecOrange else TextMuted,
                    height = 28.dp,
                    fontSize = 9.5.sp,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                )
            }
        }

        when (selectedTab) {
            0 -> QuickSetupFlowView(viewModel, telemetry, selectedPlatform)
            1 -> HarnessJ1View(viewModel, j1ConfirmedMap)
            2 -> McuHeaderView(viewModel)
            3 -> BomShoppingView(selectedPlatform)
            4 -> ModularGuideView(selectedPlatform)
        }
    }
}

@Composable
private fun QuickSetupFlowView(viewModel: CdiViewModel, t: id.ns200.cdir7.Telemetry, selectedPlatform: McuPlatform) {
    val pulserOffset by viewModel.pulserOffsetDeg.collectAsState()
    val strobeActive by viewModel.strobeActive.collectAsState()
    val flashSaved by viewModel.flashSaved.collectAsState()
    val setupCommandPending by viewModel.setupCommandPending.collectAsState()
    val quickSetupPage by viewModel.quickSetupPage.collectAsState()
    val quickSetupUnlockedStage by viewModel.quickSetupUnlockedStage.collectAsState()
    val preflightBusy by viewModel.quickSetupPreflightBusy.collectAsState()
    val preflightMessage by viewModel.quickSetupMessage.collectAsState()
    val pickupDiagnostic by viewModel.pickupDiagnosticMessage.collectAsState()
    val listState = rememberLazyListState()
    val visibleProgress = maxOf(t.setupStage, quickSetupUnlockedStage)
    var strobeModeChoice by remember { mutableIntStateOf(1) } // 0 = Strobo MCU, 1 = Manual Tanpa Strobo (Default)

    LaunchedEffect(quickSetupPage) {
        listState.animateScrollToItem((2 + quickSetupPage).coerceIn(2, 7))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // TOP SAFETY & INTERLOCK STATUS BAR
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (t.isHvOverLimitWarning) RaceRedline else BorderSubtle, RoundedCornerShape(3.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(3.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STATUS OUTPUT & INTERLOCK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MotecOrange,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "TAHAP: ${t.stage.label.uppercase()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = RacingLime,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InterlockBadge("OUTPUT DIY", t.armed, RacingLime, TextMuted)
                        InterlockBadge("HV AKTIF", t.hvEnabled, RacingLime, TextMuted)
                        InterlockBadge("PRO 345V", t.proEnabled, ElectricCyan, TextMuted)
                        InterlockBadge("CTR KOIL", t.centerEnabled, RacingLime, TextMuted)
                        InterlockBadge("SIDE KOIL", t.sideEnabled, ElectricCyan, TextMuted)
                    }
                }
            }
        }

        // STROBE CLARIFICATION BANNER
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(3.dp)),
                colors = CardDefaults.cardColors(containerColor = ElectricCyan.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(3.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Strobo timing akurat. Mode manual tersedia fallback offset awal 60.0°.",
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                }
            }
        }

        // STAGE 1: BARU (0)
        item {
            StageCard(
                stageNumber = 1,
                title = "BARU : Verifikasi Baterai & BLE",
                isCurrent = quickSetupPage == SetupStage.BARU.code,
                isDone = visibleProgress > SetupStage.BARU.code,
                onSelectStage = { viewModel.selectQuickSetupPage(SetupStage.BARU.code) }
            ) {
                Text(
                    text = "• Kill switch OFF: J1.5 = 0V, HV < 30V.\n" +
                            "• Kontak ON: J1.5 = +12V. Uji komunikasi BLE ke MCU.",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MotecButton(
                        text = "1. PING CDI",
                        onClick = { viewModel.pingCdiManual() },
                        modifier = Modifier.weight(1f),
                        color = ElectricCyan,
                        height = 30.dp
                    )
                    MotecButton(
                        text = "2. BACA SETUP",
                        onClick = { viewModel.requestSetupStateManual() },
                        modifier = Modifier.weight(1f),
                        color = SensorAmber,
                        height = 30.dp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                MotecButton(
                    text = if (preflightBusy) "MEMERIKSA MCU..." else "PERIKSA & LANJUT KE TAHAP 2",
                    onClick = { viewModel.startQuickSetupPreflight() },
                    modifier = Modifier.fillMaxWidth(),
                    color = MotecOrange,
                    enabled = !preflightBusy,
                    height = 32.dp
                )
                if (preflightMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = preflightMessage,
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            preflightMessage.startsWith("LULUS") || preflightMessage.contains("OK") -> RacingLime
                            preflightMessage.startsWith("GAGAL") -> RaceRedline
                            else -> ElectricCyan
                        }
                    )
                }
            }
        }

        // STAGE 2: PULSER (1)
        item {
            StageCard(
                stageNumber = 2,
                title = "PULSER : Pengujian Sensor Pick-up Magnet",
                isCurrent = quickSetupPage == SetupStage.PULSER.code,
                isDone = visibleProgress > SetupStage.PULSER.code,
                onSelectStage = { viewModel.selectQuickSetupPage(SetupStage.PULSER.code) }
            ) {
                Text(
                    text = "• Kabel pulser (J1.10) via signal conditioner ke ${selectedPlatform.pulserPin}.\n" +
                            "• Kualitas Sinyal: ${t.pickupQuality}/100 (Target >= 10).\n" +
                            "• Starter 2-3 detik untuk verifikasi pulser reluktor.",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 13.sp
                )

                if (pickupDiagnostic != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = if (t.pickupQuality >= 10) RacingLime.copy(alpha = 0.12f) else MotecOrange.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, if (t.pickupQuality >= 10) RacingLime.copy(alpha = 0.5f) else MotecOrange.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(
                                text = "DIAGNOSIS FIRMWARE / SELFTEST:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (t.pickupQuality >= 10) RacingLime else MotecOrange,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = pickupDiagnostic ?: "",
                                fontSize = 9.5.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 12.sp
                            )
                            if (t.pickupQuality < 10) {
                                Text(
                                    text = "💡 Petunjuk: Pada meja uji (bench), hubungkan jumper GPIO5 ke ${selectedPlatform.pulserPin}. Pada motor, periksa kabel pulser J1.10 dan putar starter.",
                                    fontSize = 8.5.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                PulserAdvancedSettings(viewModel)

                Spacer(modifier = Modifier.height(8.dp))
                MotecButton(
                    text = if (setupCommandPending) "MENUNGGU MCU..." else "KONFIRMASI PULSER OK & LANJUT TDC",
                    onClick = { viewModel.confirmPulserPickup() },
                    modifier = Modifier.fillMaxWidth(),
                    color = RacingLime,
                    enabled = !setupCommandPending,
                    height = 32.dp,
                    icon = Icons.Default.Check
                )
            }
        }

        // STAGE 3: TDC (2) - EXPLICIT STROBE CHOICE (OPTIONAL)
        item {
            StageCard(
                stageNumber = 3,
                title = "TDC : Kalibrasi Titik Mati Atas",
                isCurrent = quickSetupPage == SetupStage.TDC.code,
                isDone = visibleProgress > SetupStage.TDC.code,
                onSelectStage = { viewModel.selectQuickSetupPage(SetupStage.TDC.code) }
            ) {
                // Choice selector between Strobe vs Manual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MotecButton(
                        text = "DENGAN STROBO LED",
                        onClick = { strobeModeChoice = 0 },
                        modifier = Modifier.weight(1f),
                        color = if (strobeModeChoice == 0) MotecOrange else TextMuted,
                        height = 28.dp
                    )
                    MotecButton(
                        text = "TANPA STROBO (MANUAL)",
                        onClick = { strobeModeChoice = 1 },
                        modifier = Modifier.weight(1f),
                        color = if (strobeModeChoice == 1) RacingLime else TextMuted,
                        height = 28.dp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (strobeModeChoice == 0) {
                    // STROBE HARDWARE OPTION
                    Text(
                        text = "• Hubungkan ${selectedPlatform.strobePin} ke driver MOSFET & LED strobo.\n" +
                                "• Buka baut lubang intip magnet kiri Pulsar 200NS.\n" +
                                "• Starter mesin, sesuaikan hingga garis 'T' sejajar takik.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MotecButton(
                            text = if (strobeActive) "STROBO ON" else "STROBO OFF",
                            onClick = { viewModel.toggleStrobe(!strobeActive) },
                            modifier = Modifier.weight(1f),
                            color = if (strobeActive) SensorAmber else ElectricCyan,
                            height = 30.dp,
                            icon = Icons.Default.FlashOn
                        )
                        MotecButton(
                            text = "SIMPAN TDC STROBO",
                            onClick = { viewModel.saveTdcStrobe() },
                            modifier = Modifier.weight(1f),
                            color = RacingLime,
                            height = 30.dp,
                            enabled = !setupCommandPending
                        )
                    }
                } else {
                    // MANUAL OFFSET OPTION (NO STROBE REQUIRED)
                    Text(
                        text = "• Offset manual terukur (awal: 60.0° BTDC).\n" +
                                "• Nilai disimpan permanen ke flash A/B redundan.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Offset Pulser:", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${if (pulserOffset > 0) "+" else ""}%.1f° BTDC".format(pulserOffset),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = MotecOrange,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Slider(
                        value = pulserOffset,
                        onValueChange = { viewModel.setPulserOffset(it) },
                        valueRange = -5.0f..5.0f,
                        steps = 20,
                        colors = SliderDefaults.colors(thumbColor = MotecOrange, activeTrackColor = MotecOrange)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MotecButton(
                            text = "0.0° STD",
                            onClick = { viewModel.setPulserOffset(0.0f) },
                            modifier = Modifier.weight(1f),
                            color = ElectricCyan,
                            height = 26.dp
                        )
                        MotecButton(
                            text = "+1.5° ADV",
                            onClick = { viewModel.setPulserOffset(1.5f) },
                            modifier = Modifier.weight(1f),
                            color = SensorAmber,
                            height = 26.dp
                        )
                        MotecButton(
                            text = "-1.5° RET",
                            onClick = { viewModel.setPulserOffset(-1.5f) },
                            modifier = Modifier.weight(1f),
                            color = SensorAmber,
                            height = 26.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    MotecButton(
                        text = "SIMPAN TDC & LANJUT TPS",
                        onClick = { viewModel.saveManualTdc(pulserOffset) },
                        modifier = Modifier.fillMaxWidth(),
                        color = RacingLime,
                        enabled = !setupCommandPending,
                        height = 32.dp,
                        icon = Icons.Default.Save
                    )
                }
            }
        }

        // STAGE 4: TPS_CAL (3)
        item {
            StageCard(
                stageNumber = 4,
                title = "TPS_CAL : Kalibrasi Sensor Gas",
                isCurrent = quickSetupPage == SetupStage.TPS_CAL.code,
                isDone = visibleProgress > SetupStage.TPS_CAL.code,
                onSelectStage = { viewModel.selectQuickSetupPage(SetupStage.TPS_CAL.code) }
            ) {
                Text(
                    text = "• Mesin MATI, kunci kontak ON (J1.2 & J1.4).\n" +
                            "• Posisi Gas Saat Ini: ${t.tps / 10f}%",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MotecButton(
                        text = "1. GAS TUTUP (0%)",
                        onClick = { viewModel.calibrateTpsClosed() },
                        modifier = Modifier.weight(1f),
                        color = ElectricCyan,
                        height = 30.dp,
                        enabled = !setupCommandPending
                    )
                    MotecButton(
                        text = "2. GAS PENUH (100%)",
                        onClick = { viewModel.calibrateTpsOpen() },
                        modifier = Modifier.weight(1f),
                        color = MotecOrange,
                        height = 30.dp,
                        enabled = !setupCommandPending
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                MotecButton(
                    text = "LANJUT KE TAHAP 5 (FIRST START) →",
                    onClick = {
                        viewModel.advanceSetupStage(SetupStage.FIRST_START.code)
                        viewModel.selectQuickSetupPage(SetupStage.FIRST_START.code)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = RacingLime,
                    height = 32.dp
                )
            }
        }

        // STAGE 5: FIRST_START (4)
        item {
            StageCard(
                stageNumber = 5,
                title = "FIRST_START : Uji Nyala Pertama",
                isCurrent = quickSetupPage == SetupStage.FIRST_START.code,
                isDone = visibleProgress > SetupStage.FIRST_START.code,
                onSelectStage = { viewModel.selectQuickSetupPage(SetupStage.FIRST_START.code) }
            ) {
                Text(
                    text = "• Failsafe: 220V, CENTER ONLY, Adv <= 10°, Limit 3000 RPM.\n" +
                            "• Tegangan HV: CTR ${t.hvCenter}V, SIDE ${t.hvSide}V (Stabil: ${t.firstStartSeconds}/3s).",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                MotecButton(
                    text = "AKTIFKAN FIRST START SAFETY MODE",
                    onClick = { viewModel.prepareFirstStartMode() },
                    modifier = Modifier.fillMaxWidth(),
                    color = MotecOrange,
                    height = 32.dp,
                    enabled = !setupCommandPending
                )
                Spacer(modifier = Modifier.height(6.dp))
                MotecButton(
                    text = "LANJUT KE TAHAP 6 (READY) →",
                    onClick = {
                        viewModel.advanceSetupStage(SetupStage.READY.code)
                        viewModel.selectQuickSetupPage(SetupStage.READY.code)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = RacingLime,
                    height = 32.dp
                )
            }
        }

        // STAGE 6: READY (5)
        item {
            StageCard(
                stageNumber = 6,
                title = "READY : Operasi Penuh Normal",
                isCurrent = quickSetupPage == SetupStage.READY.code,
                isDone = t.setupStage >= SetupStage.READY.code,
                onSelectStage = { viewModel.selectQuickSetupPage(SetupStage.READY.code) }
            ) {
                Text(
                    text = "• Mesin stabil >= 3 detik. Siap operasi normal.\n" +
                            "• Pilih mode pengapian koil untuk mengaktifkan operasi jalan.",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MotecButton(
                        text = "READY: CENTER",
                        onClick = { viewModel.confirmReadyCenterOnly() },
                        modifier = Modifier.weight(1f),
                        color = ElectricCyan,
                        height = 32.dp,
                        enabled = !setupCommandPending
                    )
                    MotecButton(
                        text = "READY: 3 BUSI",
                        onClick = { viewModel.confirmReadyTripleSpark(0) },
                        modifier = Modifier.weight(1f),
                        color = RacingLime,
                        height = 32.dp,
                        enabled = !setupCommandPending
                    )
                }
            }
        }

        // FAN MODE SETTINGS
        item {
            FanModeSettings(viewModel)
        }

        // FOOTER ACTIONS: RESET SETUP & GO TO CUSTOM MAP WITH WARNING
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(3.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "MANAJEMEN SETUP & ADVANCE MAP CUSTOM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MotecButton(
                            text = "RESET SETUP",
                            onClick = { viewModel.resetSetupWorkflow() },
                            modifier = Modifier.weight(1f),
                            color = RaceRedline,
                            height = 30.dp,
                            enabled = !setupCommandPending,
                            icon = Icons.Default.Refresh
                        )
                        MotecButton(
                            text = "MAP CUSTOM ⚠️",
                            onClick = { viewModel.setTab(ScreenTab.MAPS) },
                            modifier = Modifier.weight(1f),
                            color = MotecOrange,
                            height = 30.dp,
                            icon = Icons.Default.Tune
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StageCard(
    stageNumber: Int,
    title: String,
    isCurrent: Boolean,
    isDone: Boolean,
    onSelectStage: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                when {
                    isCurrent -> MotecOrange
                    isDone -> RacingLime.copy(alpha = 0.6f)
                    else -> BorderSubtle
                },
                RoundedCornerShape(3.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) CardHover else CardBackground
        ),
        shape = RoundedCornerShape(3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelectStage)
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    isDone -> RacingLime.copy(alpha = 0.2f)
                                    isCurrent -> MotecOrange.copy(alpha = 0.25f)
                                    else -> SurfacePanel
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    isDone -> RacingLime
                                    isCurrent -> MotecOrange
                                    else -> BorderSubtle
                                },
                                RoundedCornerShape(2.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = RacingLime,
                                modifier = Modifier.size(13.dp)
                            )
                        } else {
                            Text(
                                text = "$stageNumber",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isCurrent) MotecOrange else TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) MotecOrange else if (isDone) TextPrimary else TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isDone) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = RacingLime.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, RacingLime.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "LULUS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = RacingLime,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else if (isCurrent) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = MotecOrange.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MotecOrange)
                        ) {
                            Text(
                                text = "AKTIF",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MotecOrange,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isCurrent) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (isCurrent) MotecOrange else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isCurrent) {
                Spacer(modifier = Modifier.height(6.dp))
                content()
            }
        }
    }
}

@Composable
private fun HarnessJ1View(viewModel: CdiViewModel, confirmedMap: Map<String, Boolean>) {
    val selectedPlatform by viewModel.selectedPlatform.collectAsState()
    val isStm = selectedPlatform == McuPlatform.STM32WB55

    val j1Pins = listOf(
        HarnessPinItem("J1.1", "NC", "NC", "Tidak Disambung", "Jangan disambung", "KOSONG"),
        HarnessPinItem("J1.2", "Hijau-putih", "TPS_A", if (isStm) "H_BOTTOM.12 (PA3) / H_BOTTOM.14 (PA5)" else "LEFT.3 GPIO36 / LEFT.5 GPIO34", "J_TPS pin1+pin6 -> TPS_REF atau TPS_SIG", "CONFIRM TPS", isWarning = true),
        HarnessPinItem("J1.3", "Hitam-putih", "TEMP", if (isStm) "H_BOTTOM.13 (PA4 ADC)" else "LEFT.4 GPIO39 / VN (ADC1_CH3)", "+5V--4.7k--J1.3; 15k--TEMP_ADC; 27k ke GND; clamp BAT54S", "FISIK AKTIF; TELEMETRI N/A"),
        HarnessPinItem("J1.4", "Abu-abu", "TPS_B", if (isStm) "H_BOTTOM.12 (PA3) / H_BOTTOM.14 (PA5)" else "LEFT.3 GPIO36 / LEFT.5 GPIO34", "J_TPS pin3+pin4 -> TPS_REF atau TPS_SIG", "CONFIRM TPS", isWarning = true),
        HarnessPinItem("J1.5", "+12V kontak", "+12V Kontak", "VIN_PROT / FMAIN5A", "FMAIN5A--DREV--VIN_PROT--L47uH--VIN_FILT (ke FLOGIC dan FHV)", "AKTIF"),
        HarnessPinItem("J1.6", "Hitam-merah", "COIL_SIDE", if (isStm) "H_BOTTOM.11 (PA2 via QNS/QPS)" else "LEFT.10 GPIO26 via Driver SCR2", "Terminal B koil SIDE. SCR2 anode HV_SIDE, cathode GND", "OFFSET WAJIB", isWarning = true),
        HarnessPinItem("J1.7", "Biru-kuning", "FAN_RELAY", if (isStm) "H_TOP.7 (PB5 via Modul Relay / BC547)" else "LEFT.15 GPIO13 via Modul Relay / BC547", "Modul Relay 1-CH 5V pin IN / Kolektor QFAN; coil relay ke +12V kontak", "MODUL PASARAN / DISKRIT", isWarning = true),
        HarnessPinItem("J1.8", "NC (Pabrik) / OEM_SIDE", "OEM_SIDE", if (isStm) "H_TOP.8 (PB4 via PC817)" else "RIGHT.11 GPIO17 via PC817", if (isStm) "Kabel tambahan probe OEM Side -> R 47k 2W -> Modul PC817 IN2+ -> PB4" else "Kabel tambahan probe OEM Side -> R 47k 2W -> Modul PC817 IN2+ -> GPIO17", "PROBE OEM SIDE R8"),
        HarnessPinItem("J1.9", "NC (Pabrik) / OEM_CTR", "OEM_CTR", if (isStm) "H_TOP.9 (PB3 via PC817)" else "RIGHT.12 GPIO16 via PC817", if (isStm) "Kabel tambahan probe OEM Center -> R 47k 2W -> Modul PC817 IN1+ -> PB3" else "Kabel tambahan probe OEM Center -> R 47k 2W -> Modul PC817 IN1+ -> GPIO16", "PROBE OEM CENTER R8"),
        HarnessPinItem("J1.10", "Putih-merah", "PULSER", if (isStm) "H_BOTTOM.9 (PA0 TIM2_CH1)" else "RIGHT.13 GPIO4 via LM393 DOUT", "39k--PICKUP_SENSE atau Modul Komparator LM393 DOUT ke MCU", "CONFIRM EDGE/OFFSET", isWarning = true),
        HarnessPinItem("J1.11", "Hitam-kuning", "GND", if (isStm) "H_BOTTOM.1 G / H_TOP.1 G" else "LEFT.14 / RIGHT.1 GND", "GND_STAR ke logic & power, modul opto/relay GND, dan G board", "AKTIF"),
        HarnessPinItem("J1.12", "Koil Center", "COIL_CENTER", if (isStm) "H_BOTTOM.10 (PA1 via QNC/QPC)" else "LEFT.9 GPIO25 via Driver SCR1", "Terminal B koil CENTER. SCR1 anode HV_CENTER, cathode GND", "FIRST START & READY")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MotecOrange, RoundedCornerShape(3.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(3.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text(
                        text = "ORIENTASI KONEKTOR CDI J1 (12 PIN)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MotecOrange,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Muka soket HARNESS, latch di atas:\n• Baris atas: 1 - 6 | Baris bawah: 7 - 12\n*Gunakan pigtail adaptor, JANGAN MEMOTONG harness motor!",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        items(j1Pins) { pin ->
            val isConfirmed = confirmedMap[pin.pin] ?: false
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isConfirmed) RacingLime else if (pin.isWarning) SensorAmber else BorderSubtle,
                        RoundedCornerShape(3.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = pin.pin,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConfirmed) RacingLime else MotecOrange,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${pin.wireColor} (${pin.name})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "Tujuan: ${pin.target}",
                            fontSize = 9.sp,
                            color = ElectricCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = pin.description,
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = if (isConfirmed) RacingLime.copy(alpha = 0.2f)
                                    else if (pin.isWarning) SensorAmber.copy(alpha = 0.2f)
                                    else SurfacePanel,
                            border = BorderStroke(
                                1.dp,
                                if (isConfirmed) RacingLime else if (pin.isWarning) SensorAmber else BorderSubtle
                            )
                        ) {
                            Text(
                                text = if (isConfirmed) "CONFIRMED" else pin.status,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConfirmed) RacingLime else if (pin.isWarning) SensorAmber else TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        if (pin.isWarning) {
                            Spacer(modifier = Modifier.height(3.dp))
                            MotecButton(
                                text = if (isConfirmed) "BATAL" else "KONFIRMASI",
                                onClick = { viewModel.toggleConfirmPin(pin.pin) },
                                color = if (isConfirmed) RacingLime else SensorAmber,
                                height = 24.dp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun McuHeaderView(viewModel: CdiViewModel) {
    val selectedPlatform by viewModel.selectedPlatform.collectAsState()

    val bottomPins = WiringDataProvider.weActPins.filter { it.header == "H_BOTTOM" }.map { pin ->
        WeActPinItem(pin.pinNumber.toString(), pin.name, pin.direction, pin.fullPath, listOfNotNull(pin.finalDestination, pin.status, pin.warning).joinToString(" • "), pin.isCritical)
    }
    val topPins = WiringDataProvider.weActPins.filter { it.header == "H_TOP" }.map { pin ->
        WeActPinItem(pin.pinNumber.toString(), pin.name, pin.direction, pin.fullPath, listOfNotNull(pin.finalDestination, pin.status, pin.warning).joinToString(" • "), pin.isCritical)
    }
    val esp32LeftPins = WiringDataProvider.esp32Pins.filter { it.headerSide == "LEFT" }.map { pin ->
        WeActPinItem(pin.pinNumber.toString(), pin.name, pin.direction, pin.fullPath, listOfNotNull(pin.finalDestination, pin.status, pin.warning).joinToString(" • "), pin.isCritical)
    }
    val esp32RightPins = WiringDataProvider.esp32Pins.filter { it.headerSide == "RIGHT" }.map { pin ->
        WeActPinItem(pin.pinNumber.toString(), pin.name, pin.direction, pin.fullPath, listOfNotNull(pin.finalDestination, pin.status, pin.warning).joinToString(" • "), pin.isCritical)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Platform Switcher Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfacePanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TARGET HARDWARE:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val isStm = selectedPlatform == McuPlatform.STM32WB55
                        MotecButton(
                            text = "STM32WB55",
                            onClick = { viewModel.setMcuPlatform(McuPlatform.STM32WB55) },
                            color = if (isStm) ElectricCyan else TextMuted,
                            height = 26.dp,
                            fontSize = 9.5.sp
                        )

                        val isEsp = selectedPlatform == McuPlatform.ESP32_WROOM
                        MotecButton(
                            text = "ESP32-WROOM",
                            onClick = { viewModel.setMcuPlatform(McuPlatform.ESP32_WROOM) },
                            color = if (isEsp) SparkAmber else TextMuted,
                            height = 26.dp,
                            fontSize = 9.5.sp
                        )
                    }
                }
            }
        }

        if (selectedPlatform == McuPlatform.STM32WB55) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricCyan, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "POSISI FISIK HEADER WeAct STM32WB55CGU6",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Dilihat dari sisi komponen: USB di KIRI, Antena PCB di KANAN.\n• Area antena WAJIB BEBAS logam, kabel HV, dan tembaga.\n• DILARANG memberi 12V ke pin VB!\n• SWD memakai PA13/PA14.",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            item {
                Text(
                    text = "H_BOTTOM (LUBANG 1 s/d 20 - KIRI KE KANAN)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensorAmber,
                    fontFamily = FontFamily.Monospace
                )
            }

            items(bottomPins) { pin ->
                PinRowCard(pin)
            }

            item {
                Text(
                    text = "H_TOP (LUBANG 1 s/d 15 - KIRI KE KANAN)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensorAmber,
                    fontFamily = FontFamily.Monospace
                )
            }

            items(topPins) { pin ->
                PinRowCard(pin)
            }
        } else {
            // ESP32 WROOM View
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, RaceRedline, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "POSISI FISIK HEADER ESP32-WROOM-32 DEVKIT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = RaceRedline,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "PERINGATAN KERAS ISOLASI 3.3V:\n• ESP32 BUKAN mikrokontroler 5V/12V toleran. Pulser & Sadapan OEM WAJIB PC817!\n• Input Sensor (TPS, HV, Suhu, Aki) HANYA boleh di ADC1 (GPIO 32 - 39). ADC2 nonaktif saat BLE hidup!\n• PA0 di-map ke GPIO4, PA1 (Center) ke GPIO25, PA2 (Side) ke GPIO26.",
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            item {
                Text(
                    text = "HEADER SISI KIRI (LEFT.1 s/d LEFT.19 - ATAS KE BAWAH)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SparkAmber,
                    fontFamily = FontFamily.Monospace
                )
            }

            items(esp32LeftPins) { pin ->
                PinRowCard(pin)
            }

            item {
                Text(
                    text = "HEADER SISI KANAN (RIGHT.1 s/d RIGHT.19 - ATAS KE BAWAH)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SparkAmber,
                    fontFamily = FontFamily.Monospace
                )
            }

            items(esp32RightPins) { pin ->
                PinRowCard(pin)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PinRowCard(pin: WeActPinItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (pin.isWarning) RaceRedline else BorderSubtle, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (pin.isWarning) RaceRedline else SurfacePanel)
                        .border(1.dp, if (pin.isWarning) RaceRedline else BorderSubtle, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pin.pin,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pin.isWarning) CarbonDark else TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${pin.mcuPin} : ${pin.function}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pin.isWarning) RaceRedline else TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = pin.net,
                        fontSize = 10.sp,
                        color = ElectricCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                text = pin.note,
                fontSize = 9.sp,
                color = if (pin.isWarning) RaceRedline else TextMuted,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BomShoppingView(selectedPlatform: McuPlatform = McuPlatform.STM32WB55) {
    val isStm = selectedPlatform == McuPlatform.STM32WB55
    val boms = listOf(
        BomItem("LOGIC", "U1", "1", if (isStm) "WeAct STM32WB55CGU6" else "ESP32-WROOM-32 DevKitC 38-Pin (19+19)", "Sudah dimiliki", if (isStm) "WAJIB satu-satunya MCU ARM Cortex-M4 + BLE" else "MCU Utama 240MHz Dual-Core + BLE"),
        BomItem("MODUL", "MOD_PC817", "1", "Modul Optocoupler PC817 4-Channel", "Beli baru (Rp12-18rb)", if (isStm) "OEM Learn (PB3 J1.9 & PB4 J1.8). Seri R 47k 2W" else "OEM Learn (GPIO16 J1.9 & GPIO17 J1.8). Seri R 47k 2W"),
        BomItem("MODUL", "MOD_RELAY", "1", "Modul Relay 1-Channel 5V + Opto (High/Low)", "Beli baru (Rp6-12rb)", if (isStm) "Driver Kipas Radiator J1.7 (PB5). Pengganti BC547 diskrit" else "Driver Kipas Radiator J1.7 (GPIO13). Pengganti BC547 diskrit"),
        BomItem("POWER", "PCB_POWER", "1", "PCB lubang minimal 5x7cm", "Beli baru", "Clearance HV >= 6mm, terpisah dari antena"),
        BomItem("POWER", "T1", "1", "Trafo utama ATX lilitan 5V CT utuh", "PSU PC bekas", "WAJIB; tidak dibuka/tidak dililit"),
        BomItem("LOGIC", "U2", "1", "LM339N / KA339 DIP-14 5V (atau modul LM393)", "PSU / Beli", "Komparator pulser & overvoltage"),
        BomItem("LOGIC", "U_BUCK", "1", "Modul LM2596 adjustable (in >=35V, out 5V 1A)", "Beli baru", "Catu daya logic"),
        BomItem("POWER", "U4", "1", "TC4427A / TC4427CPA DIP-8", "Beli baru", "WAJIB; jangan ganti TC4427 non-A inverting"),
        BomItem("POWER", "QHV1-2", "2", "IRF3205 55V TO-220 asli", "PSU / Beli", "MOSFET push-pull trafo HV"),
        BomItem("POWER", "SCR1-2", "2", "BT151-600R 600V TO-220", "Beli baru", "Thyristor pemicu koil CENTER & SIDE"),
        BomItem("HV", "C_CAP", "2", "1uF 630V Polypropylene Pulse MKP/MPP", "Beli baru", "WAJIB polypropylene pulse; bukan elko/X2!"),
        BomItem("HV", "DREC1-4", "4", "UF4007 1A 1000V ultrafast", "Beli baru", "Bridge penyearah trafo HV"),
        BomItem("CONTROL", "SW_SVC", "1", "Switch toggle / jumper Service (opsional)", "Kit resistor", "Software Interlock R8 via firmware & BLE"),
        BomItem("HARNESS", "J1", "1", "Pigtail pasangan soket CDI 12-pin NS200", "Donor / Beli", "WAJIB; jangan potong harness motor!")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SensorAmber, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "CATATAN MATERIAL & KOMPONEN KRITIS (MODUL PASARAN v8.1)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SensorAmber,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• Modul Jadi Pasaran: Modul PC817 4-CH & Modul Relay 1-CH 5V mengeliminasi PCB custom & solderan transistor rumit!\n• Kapasitor CDI: Wajib polypropylene pulse 630V MKP/MPP; jangan gunakan elko atau X2!\n• PCB Power HV: Dipisah fisik minimal 6mm dari board logic dan modul.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        items(boms) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "[${item.section}] ${item.ref} (Qty: ${item.qty})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.section == "MODUL") RacingLime else MotecOrange,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = item.component,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = item.note,
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfacePanel)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(item.source, fontSize = 9.sp, color = if (item.section == "MODUL") RacingLime else ElectricCyan, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ModularGuideView(selectedPlatform: McuPlatform) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RacingLime, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "MODUL JADI PASARAN (ZERO PCB CUSTOM)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = RacingLime,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Rekomendasi 2 modul komersial siap pakai untuk menggantikan komponen diskrit, memotong waktu perakitan, dan mencegah kesalahan penyolderan.",
                        fontSize = 10.sp,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Modul 1: PC817 4-Channel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "1. MODUL OPTOCOUPLER PC817 4-CHANNEL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Fungsi: Isolasi HV OEM Training (Center & Side). 1 board modul menangani kedua kanal sekaligus.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfacePanel, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        val isStm = selectedPlatform == McuPlatform.STM32WB55
                        Text(
                            text = if (isStm) {
                                "KONEKSI KABEL (STM32WB55):\n" +
                                "• Sisi Input:\n" +
                                "  - IN1+ : Kabel tambahan J1.9 (OEM Center) > Resistor 47kΩ 2W\n" +
                                "  - IN1- : Ground Motor / Frame (J1.11)\n" +
                                "  - IN2+ : Kabel tambahan J1.8 (OEM Side) > Resistor 47kΩ 2W\n" +
                                "  - IN2- : Ground Motor / Frame (J1.11)\n" +
                                "• Sisi Output (Mikro WeAct):\n" +
                                "  - VCC  : 3.3V WeAct (H_TOP.3/4)\n" +
                                "  - GND  : GND WeAct (GND_STAR H_TOP.1)\n" +
                                "  - OUT1 : PB3 STM32 (H_TOP.9) > OEM Center Capture\n" +
                                "  - OUT2 : PB4 STM32 (H_TOP.8) > OEM Side Capture"
                            } else {
                                "KONEKSI KABEL (ESP32-WROOM-32D):\n" +
                                "• Sisi Input:\n" +
                                "  - IN1+ : Kabel tambahan J1.9 (OEM Center) > Resistor 47kΩ 2W\n" +
                                "  - IN1- : Ground Motor / Frame (J1.11)\n" +
                                "  - IN2+ : Kabel tambahan J1.8 (OEM Side) > Resistor 47kΩ 2W\n" +
                                "  - IN2- : Ground Motor / Frame (J1.11)\n" +
                                "• Sisi Output (Mikro ESP32):\n" +
                                "  - VCC  : 3.3V ESP32 (LEFT.1)\n" +
                                "  - GND  : GND ESP32 (LEFT.14 / RIGHT.1)\n" +
                                "  - OUT1 : GPIO16 ESP32 (RIGHT.12) > OEM Center Capture\n" +
                                "  - OUT2 : GPIO17 ESP32 (RIGHT.11) > OEM Side Capture"
                            },
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = RacingLime,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // Modul 2: Modul Relay 1-Channel 5V
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "2. MODUL RELAY 1-CHANNEL 5V + OPTOCOUPLER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Fungsi: Driver Fan Radiator J1.7. Menggantikan transistor diskrit BC547, diode flyback 1N4007, dan resistor base.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfacePanel, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        val isStm = selectedPlatform == McuPlatform.STM32WB55
                        Text(
                            text = if (isStm) {
                                "KONEKSI KABEL (STM32WB55):\n" +
                                "• Sisi Kontrol:\n" +
                                "  - VCC : 5.0V (dari LM2596 OUT+)\n" +
                                "  - GND : GND_STAR WeAct (H_TOP.1)\n" +
                                "  - IN  : Pin PB5 STM32WB55 (H_TOP.7) langsung\n" +
                                "• Sisi Kontak Relay (Terminal Blok):\n" +
                                "  - COM : Pin J1.7 (Relay Kipas Radiator Motor)\n" +
                                "  - NO  : GND Motor / Frame\n" +
                                "  - NC  : Dibiarkan terbuka\n" +
                                "• Kontinuitas Daya OEM Learn:\n" +
                                "  - LM2596 Step-Down 5V menjaga WeAct STM32 tetap ON saat mesin mati sesaat agar data rekaman OEM Learn di RAM tidak hilang sebelum di-commit."
                            } else {
                                "KONEKSI KABEL (ESP32-WROOM-32D):\n" +
                                "• Sisi Kontrol:\n" +
                                "  - VCC : 5.0V (dari LM2596 OUT+)\n" +
                                "  - GND : GND_STAR ESP32 (LEFT.14 / RIGHT.1)\n" +
                                "  - IN  : GPIO13 ESP32 (LEFT.15) langsung\n" +
                                "• Sisi Kontak Relay (Terminal Blok):\n" +
                                "  - COM : Pin J1.7 (Relay Kipas Radiator Motor)\n" +
                                "  - NO  : GND Motor / Frame\n" +
                                "  - NC  : Dibiarkan terbuka\n" +
                                "• Kontinuitas Daya OEM Learn:\n" +
                                "  - LM2596 Step-Down 5V menjaga ESP32 tetap ON saat mesin mati sesaat agar data rekaman OEM Learn di RAM tidak hilang sebelum di-commit."
                            },
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = RacingLime,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // Modul yang DITOLAK
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RaceRedline.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = RaceRedline.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "MODUL YANG SUDAH DIUJI TAPI DITOLAK (JANGAN DIBELI):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RaceRedline,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• Modul Boost HV 12V->300-1200V: Arus hanya 2-20mA (tidak cukup untuk 3 busi 10.000 RPM butuh >= 80-120mA), dan tegangan tidak bisa dikontrol switching PWM 285V/345V firmware.\n" +
                                "• Modul Bridge Rectifier Generik: Didesain untuk frekuensi 50/60Hz PLN, panas dan drop tegangan pada frekuensi switching trafo 100kHz (Wajib gunakan ultrafast UF4007).\n" +
                                "• Modul Voltage Sensor Generik: Rasio pembagi resistor tidak cocok dengan kalibrasi ADC 3.3V firmware STM32.",
                        fontSize = 9.5.sp,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.5.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PulserAdvancedSettings(viewModel: CdiViewModel) {
    val selectedPpr by viewModel.pulserPpr.collectAsState()
    val selectedGate by viewModel.gateDurationUs.collectAsState()
    val edge by viewModel.pickupEdge.collectAsState()
    val setupCommandPending by viewModel.setupCommandPending.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            "KONFIGURASI PULSER RELUKTOR",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MotecOrange,
            fontFamily = FontFamily.Monospace
        )

        Text("Trigger edge:", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("FALLING", "RISING").forEach { item ->
                MotecButton(
                    text = if (item == "FALLING") "$item (NS200)" else item,
                    onClick = { viewModel.setPulserEdge(item) },
                    enabled = !setupCommandPending,
                    color = if (edge == item) MotecOrange else TextMuted,
                    height = 26.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text("Pulse per revolution: $selectedPpr PPR", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..4).forEach { ppr ->
                MotecButton(
                    text = "$ppr PPR",
                    onClick = { viewModel.setPulserPpr(ppr) },
                    enabled = !setupCommandPending,
                    color = if (selectedPpr == ppr) ElectricCyan else TextMuted,
                    height = 24.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text("SCR gate pulse: $selectedGate µs", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(60, 80, 100, 120).forEach { gate ->
                MotecButton(
                    text = "$gate µs",
                    onClick = { viewModel.setGateDurationUs(gate) },
                    enabled = !setupCommandPending,
                    color = if (selectedGate == gate) RacingLime else TextMuted,
                    height = 24.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            text = "STD NS200: 1 PPR, 80 µs, FALLING. Ubah PPR butuh kalibrasi TDC.",
            fontSize = 9.sp,
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            lineHeight = 11.sp
        )
    }
}

@Composable
private fun FanModeSettings(viewModel: CdiViewModel) {
    val fanMode by viewModel.fanMode.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val setupCommandPending by viewModel.setupCommandPending.collectAsState()
    val selectedPlatform by viewModel.selectedPlatform.collectAsState()

    val safeToChange =
        telemetry.rpm == 0 &&
        !telemetry.hvEnabled &&
        telemetry.hvCenter < 30 &&
        telemetry.hvSide < 30

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(3.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(3.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "KONTROL KIPAS RADIATOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensorAmber,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "AKTIF: $fanMode",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (fanMode) {
                        "ON" -> RacingLime
                        "OFF" -> RaceRedline
                        else -> ElectricCyan
                    },
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                "Relai J1.7 via ${selectedPlatform.fanRelayPin}. OFF: LOW, ON/AUTO: HIGH.",
                fontSize = 9.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("OFF", "ON", "AUTO").forEach { mode ->
                    MotecButton(
                        text = mode,
                        onClick = { viewModel.setFanMode(mode) },
                        enabled = safeToChange && !setupCommandPending,
                        color = if (fanMode == mode) {
                            when (mode) {
                                "ON" -> RacingLime
                                "OFF" -> RaceRedline
                                else -> ElectricCyan
                            }
                        } else TextMuted,
                        height = 26.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!safeToChange) {
                Text(
                    "⚠️ Mesin harus mati & HV < 30V untuk mengubah mode kipas.",
                    color = Color(0xFFFF4444),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun InterlockBadge(label: String, active: Boolean, activeColor: Color, inactiveColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (active) activeColor else inactiveColor)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = if (active) activeColor else TextMuted,
            fontFamily = FontFamily.Monospace
        )
    }
}
