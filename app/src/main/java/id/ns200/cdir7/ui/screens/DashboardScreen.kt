package id.ns200.cdir7.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ns200.cdir7.CdiProtocol
import id.ns200.cdir7.CdiViewModel
import id.ns200.cdir7.EnginePrimaryAction
import id.ns200.cdir7.FirmwareRunMode
import id.ns200.cdir7.HardwareModule
import id.ns200.cdir7.McuPlatform
import id.ns200.cdir7.ScreenTab
import id.ns200.cdir7.SessionPhase
import id.ns200.cdir7.ui.components.MotecButton
import id.ns200.cdir7.ui.theme.*
import kotlin.math.*

@Composable
fun DashboardScreen(viewModel: CdiViewModel) {
    val telemetry by viewModel.telemetry.collectAsState()
    val isRevving by viewModel.isRevving.collectAsState()
    val revLimit by viewModel.softRevLimiterRpm.collectAsState()
    val demoThrottleSlider by viewModel.demoThrottleSlider.collectAsState()
    val demoEngineRunning by viewModel.demoEngineRunning.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val packetRate by viewModel.packetRateHz.collectAsState()
    val crcPercent by viewModel.crcValidPercent.collectAsState()
    val telemetryPacketCount by viewModel.telemetryPacketCount.collectAsState()
    val fwMode by viewModel.firmwareMode.collectAsState()
    val targetHv by viewModel.targetHvVoltage.collectAsState()
    val isPro by viewModel.isProVoltageConfigured.collectAsState()
    val selectedPlatform by viewModel.selectedPlatform.collectAsState()
    val isTelemetryStreaming by viewModel.isTelemetryStreaming.collectAsState()
    val isSimulationMode by viewModel.isSimulationMode.collectAsState()
    val moduleStatus by viewModel.moduleStatus.collectAsState()
    val sessionPhase by viewModel.sessionPhase.collectAsState()
    val bindingRecord by viewModel.bindingRecord.collectAsState()
    val firmwareIdentity by viewModel.firmwareIdentity.collectAsState()
    val auxStatus by viewModel.auxStatus.collectAsState()
    val engineAction by viewModel.enginePrimaryAction.collectAsState()
    val pulserPpr by viewModel.pulserPpr.collectAsState()
    val gateDurationUs by viewModel.gateDurationUs.collectAsState()
    val scrollState = rememberScrollState()

    val isSideInstalled = moduleStatus.isInstalled(HardwareModule.DUAL_COIL)
    val isSideActive = moduleStatus.isActive(HardwareModule.DUAL_COIL) || telemetry.sideEnabled
    val isDualCoil = isSideInstalled || isSideActive

    val currentRpm = telemetry.rpm
    val isAtLimiter = telemetry.limiter > 0 || currentRpm >= revLimit
    val isBleConnected = viewModel.bleClient.gattReady

    // Animasi jarum tachometer sangat responsif dengan interpolasi halus.
    // Jika Watchdog UI memicu timeout 500ms, jarum langsung turun ke 0 secara presisi dan tidak pernah menggantung.
    val targetRpmFraction = (currentRpm / 12000f).coerceIn(0f, 1f)
    val animatedRpmFraction by animateFloatAsState(
        targetValue = targetRpmFraction,
        animationSpec = tween(durationMillis = 80, easing = LinearOutSlowInEasing),
        label = "animated_rpm_gauge"
    )

    // Slider tacho interaktif: mengikuti RPM aktual secara dinamis (live BLE maupun simulasi demo)
    var userDragFraction by remember { mutableStateOf<Float?>(null) }
    val actualRpmFraction = (currentRpm.toFloat() / revLimit.toFloat()).coerceIn(0f, 1f)
    val displaySliderValue = userDragFraction ?: actualRpmFraction

    // Limiter warning pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "limiter_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    var isCapacitorExpanded by remember { mutableStateOf(false) }
    var isSimulatorExpanded by remember { mutableStateOf(false) }
    var isTechDataExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .widthIn(max = 680.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Over-voltage warning is driven by firmware telemetry; target follows the active profile.
            if (telemetry.hvCenter >= 360 || (isDualCoil && telemetry.hvSide >= 360)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, RaceRedline, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = RaceRedline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = RaceRedline,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "FAULT TEGANGAN TINGGI: >= 300 V!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = RaceRedline,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Matikan kontak/kill switch, buka SW_SERVICE atau cabut FHV, lalu periksa feedback HV dan clamp.",
                                fontSize = 9.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // BINDING APP DENGAN HARDWARE STATUS BAR
            if (bindingRecord == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SensorAmber.copy(alpha = 0.7f), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = SensorAmber.copy(alpha = 0.10f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = SensorAmber,
                                modifier = Modifier.size(15.dp)
                            )
                            Column {
                                Text(
                                    text = "BINDING APP BELUM TERIKAT (READ-ONLY)",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SensorAmber,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Ikat serial CDI [${firmwareIdentity.serial}] untuk membuka izin tulis konfigurasi.",
                                    fontSize = 8.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        MotecButton(
                            text = "BINDING",
                            onClick = { viewModel.setTab(ScreenTab.BLE) },
                            color = SensorAmber,
                            height = 24.dp
                        )
                    }
                }
            }

            // TACHOMETER CLUSTER CARD (MoTeC i2 style, Compact Layout)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isAtLimiter) RaceRedline else BorderSubtle, RoundedCornerShape(12.dp))
                    .testTag("tacho_cluster_card"),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (telemetry.armed) RacingLime else RaceRedline)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (telemetry.armed) "ARMED & READY" else "DISARMED",
                                color = if (telemetry.armed) RacingLime else RaceRedline,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Soft Rev-Limiter badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isAtLimiter) RaceRedline.copy(alpha = pulseGlow)
                                    else if (currentRpm > revLimit - 800) SensorAmber.copy(alpha = 0.2f)
                                    else SurfacePanel
                                )
                                .border(
                                    1.dp,
                                    if (isAtLimiter) RaceRedline else BorderSubtle,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isAtLimiter) "LIMITER ACTIVE" else "LIMIT: $revLimit RPM",
                                color = if (isAtLimiter) Color.White else MotecOrange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Radial Sweep Tachometer Gauge (Compact)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height
                            val center = Offset(width / 2f, height * 0.94f)
                            val radius = min(width * 0.44f, height * 0.90f)

                            val startAngle = 180f
                            val sweepAngle = 180f

                            // Background Track Arc
                            drawArc(
                                color = BorderSubtle,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Redline Zone Arc (9500 to 12000 RPM)
                            val redlineFraction = (12000f - 9500f) / 12000f
                            val redlineSweep = sweepAngle * redlineFraction
                            val redlineStart = startAngle + sweepAngle - redlineSweep
                            drawArc(
                                color = RaceRedline.copy(alpha = 0.45f),
                                startAngle = redlineStart,
                                sweepAngle = redlineSweep,
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Active Sweep Progress
                            val activeSweep = sweepAngle * animatedRpmFraction

                            val strokeBrush = Brush.sweepGradient(
                                listOf(
                                    ElectricCyan,
                                    MotecOrange,
                                    if (isAtLimiter) RaceRedline else MotecOrange
                                ),
                                center = center
                            )

                            drawArc(
                                brush = strokeBrush,
                                startAngle = startAngle,
                                sweepAngle = activeSweep,
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Ticks & Labels (0..12 kRPM)
                            for (i in 0..12) {
                                val angleRad = Math.toRadians((startAngle + (sweepAngle * (i / 12f))).toDouble())
                                val innerR = radius - 18.dp.toPx()
                                val outerR = radius - 8.dp.toPx()
                                val tickColor = if (i >= 10) RaceRedline else if (i >= 8) SensorAmber else TextSecondary
                                val startP = Offset(
                                    (center.x + innerR * cos(angleRad)).toFloat(),
                                    (center.y + innerR * sin(angleRad)).toFloat()
                                )
                                val endP = Offset(
                                    (center.x + outerR * cos(angleRad)).toFloat(),
                                    (center.y + outerR * sin(angleRad)).toFloat()
                                )
                                drawLine(
                                    color = tickColor,
                                    start = startP,
                                    end = endP,
                                    strokeWidth = if (i % 2 == 0) 2.5.dp.toPx() else 1.2.dp.toPx()
                                )
                            }

                            // Jarum Tachometer Dinamis (Pointer Needle)
                            val needleAngleRad = Math.toRadians((startAngle + (sweepAngle * animatedRpmFraction)).toDouble())
                            val needleLength = radius - 10.dp.toPx()
                            val needleEnd = Offset(
                                (center.x + needleLength * cos(needleAngleRad)).toFloat(),
                                (center.y + needleLength * sin(needleAngleRad)).toFloat()
                            )
                            drawLine(
                                color = if (isAtLimiter) RaceRedline else ElectricCyan,
                                start = center,
                                end = needleEnd,
                                strokeWidth = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawCircle(
                                color = if (isAtLimiter) RaceRedline else MotecOrange,
                                radius = 5.dp.toPx(),
                                center = center
                            )
                        }

                        // Digital RPM Readout & Title
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ENGINE RPM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MotecOrange,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "$currentRpm",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isAtLimiter) RaceRedline else TextPrimary,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Speed / Stage Footer in Cluster
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("STAGE", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(
                                when (telemetry.setupStage) {
                                    4 -> "READY"
                                    3 -> "FIRST START"
                                    2 -> "TDC CAL"
                                    1 -> "PULSER OK"
                                    else -> "INIT"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = RacingLime,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MAP SLOT", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(
                                "MAP ${telemetry.slot + 1}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PICKUP", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(
                                "${telemetry.pickupQuality}%",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

        // CAPACITOR MONITOR CARD (J1.12 Core tunggal atau J1.12+J1.6 Dual Coil)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCapacitorExpanded = !isCapacitorExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = SensorAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isDualCoil) "KAPASITOR DUAL COIL (HV)" else "KAPASITOR CORE (CDI HV)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = SensorAmber
                        )
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = (if (isDualCoil && isSideActive) RacingLime else if (isDualCoil) SensorAmber else ElectricCyan).copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, if (isDualCoil && isSideActive) RacingLime else if (isDualCoil) SensorAmber else ElectricCyan)
                        ) {
                            Text(
                                text = if (!isDualCoil) "J1.12: ${telemetry.hvCenter}V" else "CTR: ${telemetry.hvCenter}V | SIDE: ${telemetry.hvSide}V",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDualCoil && isSideActive) RacingLime else if (isDualCoil) SensorAmber else ElectricCyan,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "TARGET: ${targetHv}V",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                        Text(
                            text = if (isCapacitorExpanded) "▲" else "▼",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (isCapacitorExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isDualCoil) {
                        // 2 Coil Motor: SEJAJAR KANAN KIRI
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Center Capacitor (J1.12)
                            CapacitorMeter(
                                modifier = Modifier.weight(1f),
                                label = "HV CORE / CENTER",
                                pin = "PIN J1.12 • KANAL UTAMA",
                                voltage = telemetry.hvCenter,
                                targetVoltage = targetHv,
                                isFull = telemetry.hvCenter >= 220,
                                isActive = telemetry.centerEnabled
                            )

                            // Side Capacitor (J1.6)
                            CapacitorMeter(
                                modifier = Modifier.weight(1f),
                                label = "HV SIDE",
                                pin = if (isSideActive) "PIN J1.6 • KANAL KEDUA" else "PIN J1.6 • STANDBY",
                                voltage = telemetry.hvSide,
                                targetVoltage = targetHv,
                                isFull = telemetry.hvSide >= 220,
                                isActive = isSideActive && telemetry.sideEnabled
                            )
                        }
                    } else {
                        // 1 Coil Motor: RATA TENGAH (CENTERED) & RAPI
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CapacitorMeter(
                                modifier = Modifier
                                    .widthIn(max = 380.dp)
                                    .fillMaxWidth(),
                                label = "HV CORE / CENTER",
                                pin = "PIN J1.12 • KANAL TUNGGAL",
                                voltage = telemetry.hvCenter,
                                targetVoltage = targetHv,
                                isFull = telemetry.hvCenter >= 220,
                                isActive = telemetry.centerEnabled
                            )
                        }
                    }
                }
            }
        }

        // HARDWARE MODULES STATUS (PAKET HARDWARE FISIK TAMBAHAN)
        HardwareModulesCard(viewModel)

        // TELEMETRY METRICS ROW: ADVANCE ANGLE (°BTDC) & TPS (J1.8) SENSORS (EQUAL SIZE & SYMMETRY)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Card: IGNITION ADVANCE
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "IGNITION ADVANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "%.1f°".format(telemetry.advanceCdeg / 100f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (telemetry.advanceCdeg / 5000f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ElectricCyan,
                            trackColor = SurfacePanel
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PULSER (TDC)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SensorAmber,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "%+.1f°".format(telemetry.triggerCdeg / 100f - 10f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TIMING: BTDC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = ElectricCyan.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, ElectricCyan)
                        ) {
                            Text(
                                text = "SPARK SYNC",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Right Card: Engine Sensors (TPS & Coolant Temp & Aki)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TPS (J1.8)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MotecOrange,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${(telemetry.tps / 10f).toInt()}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (telemetry.tps / 1000f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MotecOrange,
                            trackColor = SurfacePanel
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COOLANT (J1.3)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SensorAmber,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (telemetry.tempCdeg == Short.MIN_VALUE.toInt()) "N/A" else "%.1f°C".format(telemetry.tempCdeg / 100f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val battVolt = telemetry.batteryCv / 100f
                    val (battStatus, battColor) = when {
                        battVolt >= 12.4f -> Pair("NORMAL / CHARGING", RacingLime)
                        battVolt >= 11.8f -> Pair("SIAGA", SensorAmber)
                        else -> Pair("AKI DROP (<11.8V)", RaceRedline)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AKI: %.1fV".format(battVolt),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = battColor,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = battColor.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, battColor)
                        ) {
                            Text(
                                text = battStatus,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = battColor,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        // Satu kontrol nyata; label mengikuti status kontak dan RPM firmware.
        if (isBleConnected) {
            val actionColor = when (engineAction) {
                EnginePrimaryAction.STOP_ENGINE -> RaceRedline
                EnginePrimaryAction.START_ENGINE -> RacingLime
                EnginePrimaryAction.CONTACT_ON -> MotecOrange
            }
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, actionColor, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = auxStatus.contactSource.label.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = actionColor,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (auxStatus.requestIoOk) "AUX interlock siap" else "Periksa U7 / konfigurasi AUX",
                            fontSize = 8.5.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    MotecButton(
                        text = engineAction.label,
                        onClick = viewModel::performPrimaryEngineAction,
                        color = actionColor,
                        icon = when (engineAction) {
                            EnginePrimaryAction.STOP_ENGINE -> Icons.Default.Stop
                            EnginePrimaryAction.START_ENGINE -> Icons.Default.PlayArrow
                            EnginePrimaryAction.CONTACT_ON -> Icons.Default.PowerSettingsNew
                        },
                        height = 34.dp,
                        modifier = Modifier.testTag("dashboard_primary_engine_action")
                    )
                }
            }
        }

        // ENGINE SIMULATION & STARTER PANEL (COLLAPSIBLE / AUTO-HIDE)
        if (!isBleConnected) {
            val isEngineOn = demoEngineRunning && currentRpm > 50
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isEngineOn) RacingLime.copy(alpha = 0.5f) else BorderSubtle,
                        RoundedCornerShape(10.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSimulatorExpanded = !isSimulatorExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isEngineOn) RacingLime else RaceRedline)
                            )
                            Text(
                                text = "KONTROL GAS & STARTER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isEngineOn) RacingLime else SensorAmber
                            )
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = (if (isEngineOn) RacingLime else SensorAmber).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isEngineOn) "IDLE • $currentRpm RPM" else "MATI • 0 RPM",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isEngineOn) RacingLime else SensorAmber,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isSimulatorExpanded) "▲" else "▼",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (isSimulatorExpanded) {
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEngineOn) "Koil memercik • Kontak ON" else "Kunci kontak OFF / 0 RPM",
                                fontSize = 9.5.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            if (!isEngineOn) {
                                MotecButton(
                                    text = "STARTER",
                                    onClick = { viewModel.simulateStartEngine() },
                                    color = RacingLime,
                                    icon = Icons.Default.PlayArrow,
                                    height = 28.dp,
                                    modifier = Modifier.testTag("dashboard_engine_starter_btn")
                                )
                            } else {
                                MotecButton(
                                    text = "STOP MESIN",
                                    onClick = { viewModel.simulateStopEngine() },
                                    color = RaceRedline,
                                    icon = Icons.Default.Stop,
                                    height = 28.dp,
                                    modifier = Modifier.testTag("dashboard_engine_stop_btn")
                                )
                            }
                        }
                    }
                }
            }
        }

        // INTERACTIVE THROTTLE / RPM SLIDER (HOLDS RPM IN DEMO, FOLLOWS REAL MOTORCYCLE IN BLE)
        if (isSimulatorExpanded) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, if (isBleConnected) RacingLime else MotecOrange, RoundedCornerShape(16.dp))
                .shadow(if (displaySliderValue > 0.05f || isRevving) 10.dp else 0.dp, shape = RoundedCornerShape(16.dp), ambientColor = MotecOrange),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = if (isBleConnected) Icons.Default.Speed else Icons.Default.Tune,
                            contentDescription = "Throttle Slider",
                            tint = if (isBleConnected) RacingLime else MotecOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBleConnected) "SLIDER REAL TIME (LIVE MOTOR)" else "SLIDER TACHO (IKUTI RPM)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isBleConnected) RacingLime else MotecOrange,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfacePanel)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = when {
                                isBleConnected -> "LIVE CDI • $currentRpm RPM"
                                isRevving -> "BLIP GAS • $currentRpm RPM"
                                userDragFraction != null -> "GAS ${(userDragFraction!! * 100).toInt()}% • $currentRpm RPM"
                                demoThrottleSlider > 0.01f -> "TAHAN ${(actualRpmFraction * 100).toInt()}% • $currentRpm RPM"
                                !demoEngineRunning || currentRpm <= 50 -> "MESIN MATI • 0 RPM"
                                else -> "IDLE • $currentRpm RPM"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                isBleConnected -> RacingLime
                                isRevving -> RaceRedline
                                userDragFraction != null -> MotecOrange
                                demoThrottleSlider > 0.01f -> MotecOrange
                                !demoEngineRunning || currentRpm <= 50 -> SensorAmber
                                else -> ElectricCyan
                            }
                        )
                    }
                }

                // Interactive Slider
                Slider(
                    value = displaySliderValue,
                    onValueChange = { newVal ->
                        if (!isBleConnected) {
                            userDragFraction = newVal
                            viewModel.setDemoRpmDirect(newVal * revLimit)
                        }
                    },
                    onValueChangeFinished = {
                        userDragFraction = null
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (isBleConnected) RacingLime else MotecOrange,
                        activeTrackColor = if (isBleConnected) RacingLime else MotecOrange,
                        inactiveTrackColor = SurfacePanel
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tacho_throttle_slider")
                )

                // Scale markings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("IDLE 1.4K", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text("4.5K CRUISE", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text("8.0K POWER", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text("$revLimit REDLINE", fontSize = 9.sp, color = RaceRedline, fontFamily = FontFamily.Monospace)
                }

                // Quick Preset RPM Buttons & Momentary Blip (Motec styling)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MotecButton(
                        text = "IDLE",
                        onClick = { viewModel.resetDemoThrottle() },
                        color = ElectricCyan,
                        height = 30.dp,
                        modifier = Modifier.weight(1f)
                    )

                    MotecButton(
                        text = "5K",
                        onClick = { viewModel.setDemoRpmDirect(5000f) },
                        color = TextPrimary,
                        height = 30.dp,
                        modifier = Modifier.weight(1f)
                    )

                    MotecButton(
                        text = "8K",
                        onClick = { viewModel.setDemoRpmDirect(8000f) },
                        color = MotecOrange,
                        height = 30.dp,
                        modifier = Modifier.weight(1f)
                    )

                    MotecButton(
                        text = "LIMITER",
                        onClick = { viewModel.setDemoRpmDirect(revLimit.toFloat()) },
                        color = RaceRedline,
                        height = 30.dp,
                        modifier = Modifier.weight(1.2f)
                    )

                    // Momentary Quick Blip & Hold Gas Button (Motec Box semi-transparan bergaya balap)
                    val blipColor = if (isRevving) RacingLime else MotecOrange
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(30.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(blipColor.copy(alpha = if (isRevving) 0.35f else 0.14f))
                            .border(1.dp, blipColor, RoundedCornerShape(3.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        viewModel.triggerThrottleBlip()
                                    },
                                    onPress = {
                                        val startTime = System.currentTimeMillis()
                                        try {
                                            viewModel.setHoldToRev(true)
                                            tryAwaitRelease()
                                            val duration = System.currentTimeMillis() - startTime
                                            if (duration < 180) {
                                                viewModel.triggerThrottleBlip()
                                            }
                                        } finally {
                                            viewModel.setHoldToRev(false)
                                        }
                                    }
                                )
                            }
                            .testTag("hold_to_rev_blip_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isRevving) Icons.Default.Bolt else Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = blipColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = if (isRevving) "GAS!!" else "BLIP GAS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = blipColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Quick Idle & Demo Reset buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "THROTTLE: ${telemetry.tps / 10f}% • TPS ADC",
                fontSize = 10.5.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MotecButton(
                    text = "RESET RPM",
                    onClick = { viewModel.resetVirtualEngine() },
                    color = SensorAmber,
                    icon = Icons.Default.Refresh,
                    height = 26.dp,
                    testTag = "reset_engine_btn"
                )
                MotecButton(
                    text = "RESET DEMO",
                    onClick = { viewModel.resetDemoCommissioning() },
                    color = MotecOrange,
                    icon = Icons.Default.Refresh,
                    height = 26.dp,
                    testTag = "reset_demo_btn"
                )
            }
        }
        }

        // TECHNICAL DATA GRID & HARDWARE DIAGNOSTICS (MoTeC / AIM Race Studio Style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isTechDataExpanded = !isTechDataExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "DATA TEKNIS & DIAGNOSTIK GATT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "SEQ ${"%05d".format(telemetry.sequence and 0xffff)}",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isTechDataExpanded) "▲" else "▼",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (isTechDataExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Technical Data Grid 2-column key-value
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfacePanel, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TechDataRow(
                            "GATT STATUS",
                            if (isConnected) "CONNECTED (GATT READY)" else connectionStatus.uppercase(),
                            if (isConnected) RacingLime else TextMuted
                        )
                        TechDataRow(
                            "PACKET RATE",
                            when {
                                !isConnected -> "0 Hz (OFFLINE)"
                                telemetryPacketCount < 2L -> "MENUNGGU (${telemetryPacketCount} frame)"
                                else -> "$packetRate Hz (target 18-22 Hz)"
                            },
                            when {
                                !isConnected || packetRate == 0 -> TextMuted
                                packetRate in 18..22 -> RacingLime
                                packetRate in 12..17 || packetRate > 22 -> MotecOrange
                                else -> RaceRedline
                            }
                        )
                        TechDataRow(
                            "CRC VALID",
                            when {
                                !isConnected -> "OFFLINE"
                                telemetryPacketCount == 0L -> "BELUM ADA FRAME"
                                else -> "%.1f%% VALID".format(crcPercent)
                            },
                            when {
                                !isConnected || packetRate == 0 -> TextMuted
                                crcPercent >= 99f -> RacingLime
                                crcPercent >= 95f -> MotecOrange
                                else -> RaceRedline
                            }
                        )
                        TechDataRow(
                            "TELEMETRY RX",
                            when {
                                !isConnected -> "OFFLINE"
                                telemetryPacketCount == 0L -> "NO FRAME"
                                packetRate == 0 -> "STOPPED"
                                else -> "ACTIVE • ${CdiProtocol.compactCounter(telemetryPacketCount)} FRAME"
                            },
                            when {
                                !isConnected -> TextMuted
                                telemetryPacketCount == 0L -> RaceRedline
                                packetRate == 0 -> RaceRedline
                                crcPercent >= 99f -> RacingLime
                                else -> MotecOrange
                            }
                        )
                        TechDataRow("SETUP STAGE", "${telemetry.stage.name} (${telemetry.stage.label})", when (telemetry.setupStage) {
                            5 -> RacingLime
                            4 -> MotecOrange
                            else -> SensorAmber
                        })
                        val oemPinsLabel = if (selectedPlatform == McuPlatform.STM32WB55) "BACA OEM PB3/PB4" else "BACA OEM GPIO16/17"
                        val fanPinLabel = if (selectedPlatform == McuPlatform.STM32WB55) "ACTIVE (PB5 HIGH)" else "ACTIVE (GPIO13 HIGH)"
                        TechDataRow("MODE FIRMWARE", "${fwMode.name} (${if (fwMode == FirmwareRunMode.DIY) "MANDIRI" else if (fwMode == FirmwareRunMode.OEM_LEARN) oemPinsLabel else "MANUAL"})", ElectricCyan)
                        TechDataRow("TARGET TEGANGAN HV", "$targetHv V (${if (isPro) "PRO 345V" else "NORMAL 285V"})", RacingLime)
                        TechDataRow("OUTPUT KOIL", if (isDualCoil) "CTR: ${if (telemetry.centerEnabled) "ON" else "OFF"} | SIDE: ${if (telemetry.sideEnabled) "ON" else "OFF"}" else "CORE J1.12: ${if (telemetry.centerEnabled) "ON" else "OFF"} (1 KOIL)", RacingLime)
                        TechDataRow("PULSER QUALITY", "${telemetry.pickupQuality} / 100 (PPR=$pulserPpr Gate=${gateDurationUs}µs)", if (telemetry.pickupQuality >= 10) RacingLime else RaceRedline)
                        TechDataRow("TRIGGER TIMING", "%.1f° BTDC".format(telemetry.triggerCdeg / 100f), ElectricCyan)
                        TechDataRow("FAN RELAY (J1.7)", if (telemetry.fanEnabled) fanPinLabel else "OFF (LOW)", if (telemetry.fanEnabled) SensorAmber else TextMuted)
                        TechDataRow("FAULT BITS", if (telemetry.faults == 0) "0x0000 (NO FAULT)" else "0x%04X".format(telemetry.faults), if (telemetry.faults == 0) RacingLime else RaceRedline)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
}

@Composable
private fun TechDataRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.42f),
            fontSize = 10.sp,
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.58f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CapacitorMeter(
    modifier: Modifier = Modifier,
    label: String,
    pin: String,
    voltage: Int,
    targetVoltage: Int = 285,
    isFull: Boolean,
    isActive: Boolean = true
) {
    Card(
        modifier = modifier.border(
            1.dp,
            if (isFull) RacingLime.copy(alpha = 0.6f) else BorderSubtle,
            RoundedCornerShape(10.dp)
        ),
        colors = CardDefaults.cardColors(containerColor = SurfacePanel),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = (if (isActive) RacingLime else TextMuted).copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, if (isActive) RacingLime else TextMuted)
                ) {
                    Text(
                        text = if (isActive) "PULSE ON" else "OFF",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) RacingLime else TextMuted,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Text(
                text = pin,
                fontSize = 8.5.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$voltage",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (isFull) TextPrimary else SensorAmber
                )
                Text(
                    text = " V",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            val progress = (voltage / targetVoltage.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isFull) RacingLime else SensorAmber,
                trackColor = CardBackground
            )

            val chargeLabel = when {
                voltage < 30 -> "DISCHARGED (<30V)"
                isFull -> "CHARGED • SIAP TEMBAK"
                else -> "CHARGING (${(progress * 100).toInt()}%)"
            }
            val chargeColor = when {
                voltage < 30 -> TextMuted
                isFull -> RacingLime
                else -> SensorAmber
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(chargeColor.copy(alpha = 0.15f))
                    .border(1.dp, chargeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = chargeLabel,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = chargeColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
