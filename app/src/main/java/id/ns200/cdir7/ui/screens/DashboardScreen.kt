package id.ns200.cdir7.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ns200.cdir7.CdiViewModel
import id.ns200.cdir7.FirmwareRunMode
import id.ns200.cdir7.ui.theme.*
import kotlin.math.*

@Composable
fun DashboardScreen(viewModel: CdiViewModel) {
    val telemetry by viewModel.telemetry.collectAsState()
    val isRevving by viewModel.isRevving.collectAsState()
    val revLimit by viewModel.softRevLimiterRpm.collectAsState()
    val demoThrottleSlider by viewModel.demoThrottleSlider.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val packetRate by viewModel.packetRateHz.collectAsState()
    val crcPercent by viewModel.crcValidPercent.collectAsState()
    val telemetryPacketCount by viewModel.telemetryPacketCount.collectAsState()
    val fwMode by viewModel.firmwareMode.collectAsState()
    val targetHv by viewModel.targetHvVoltage.collectAsState()
    val isPro by viewModel.isProVoltageConfigured.collectAsState()
    val scrollState = rememberScrollState()

    val currentRpm = telemetry.rpm
    val isAtLimiter = telemetry.limiter > 0 || currentRpm >= revLimit
    val isBleConnected = viewModel.bleClient.gattReady && currentRpm > 100
    val displaySliderValue = if (isBleConnected) (currentRpm.toFloat() / revLimit.toFloat()).coerceIn(0f, 1f) else demoThrottleSlider

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // R7.2 hardware/software limit: fault at 300 V.
        if (telemetry.isHvOverLimitWarning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, RaceRedline, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = RaceRedline.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = RaceRedline,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "FAULT TEGANGAN TINGGI: >= 300 V!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = RaceRedline,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Matikan kontak/kill switch, lepas JP_HV, lalu periksa feedback ADC PA6/PA7 dan rangkaian clamp sebelum melanjutkan.",
                            fontSize = 10.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // TACHOMETER CLUSTER CARD (MoTeC i2 style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isAtLimiter) RaceRedline else BorderSubtle, RoundedCornerShape(16.dp))
                .testTag("tacho_cluster_card"),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (telemetry.armed) RacingLime else RaceRedline)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (telemetry.armed) "ARMED & READY" else "DISARMED",
                            color = if (telemetry.armed) RacingLime else RaceRedline,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Soft Rev-Limiter badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isAtLimiter) RaceRedline.copy(alpha = pulseGlow)
                                else if (currentRpm > revLimit - 800) SensorAmber.copy(alpha = 0.2f)
                                else SurfacePanel
                            )
                            .border(
                                1.dp,
                                if (isAtLimiter) RaceRedline else BorderSubtle,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isAtLimiter) "LIMITER ACTIVE" else "LIMIT: $revLimit RPM",
                            color = if (isAtLimiter) Color.White else MotecOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Radial Sweep Tachometer Gauge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val center = Offset(width / 2f, height * 0.9f)
                        val radius = min(width * 0.45f, height * 0.85f)

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
                            style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
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
                            style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Active Sweep Progress
                        val rpmFraction = (currentRpm / 12000f).coerceIn(0f, 1f)
                        val activeSweep = sweepAngle * rpmFraction

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
                            style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Ticks & Labels (0, 2, 4, 6, 8, 10, 12 kRPM)
                        for (i in 0..12) {
                            val angleRad = Math.toRadians((startAngle + (sweepAngle * (i / 12f))).toDouble())
                            val innerR = radius - 24.dp.toPx()
                            val outerR = radius - 10.dp.toPx()
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
                                strokeWidth = if (i % 2 == 0) 3.dp.toPx() else 1.5.dp.toPx()
                            )
                        }
                    }

                    // Digital RPM Readout
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-10).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$currentRpm",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isAtLimiter) RaceRedline else TextPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "ENGINE RPM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MotecOrange
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
                        Text("STAGE", fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(
                            when (telemetry.setupStage) {
                                4 -> "READY"
                                3 -> "FIRST START"
                                2 -> "TDC CAL"
                                1 -> "PULSER OK"
                                else -> "INIT"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = RacingLime,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MAP SLOT", fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(
                            "MAP ${telemetry.slot + 1}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PICKUP", fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(
                            "${telemetry.pickupQuality}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // DUAL CAPACITOR MONITOR CARD (J1.12 & J1.6)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DUAL CAPACITOR MONITOR (CDI HV)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = SensorAmber
                    )
                    Text(
                        text = "TARGET: 250V",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Center Capacitor (J1.12)
                    CapacitorMeter(
                        modifier = Modifier.weight(1f),
                        label = "CENTER CAP (J1.12)",
                        voltage = telemetry.hvCenter,
                        isFull = telemetry.hvCenter >= 220
                    )

                    // Side Capacitor (J1.6)
                    CapacitorMeter(
                        modifier = Modifier.weight(1f),
                        label = "SIDE CAP (J1.6)",
                        voltage = telemetry.hvSide,
                        isFull = telemetry.hvSide >= 220
                    )
                }
            }
        }

        // TELEMETRY METRICS ROW: ADVANCE ANGLE (°BTDC) & PULSER OFFSET
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Advance Angle Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "IGNITION ADVANCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "%.1f°".format(telemetry.advanceCdeg / 100f),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "BTDC (Pulser: %+.1f°)".format(telemetry.triggerCdeg / 100f - 10f),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Engine Sensors Card (TPS & Coolant Temp)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
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
                    LinearProgressIndicator(
                        progress = { (telemetry.tps / 1000f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MotecOrange,
                        trackColor = SurfacePanel
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "COOLANT (J1.5)",
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
                    Text(
                        text = "Battery: %.1fV".format(telemetry.batteryCv / 100f),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // INTERACTIVE THROTTLE / RPM SLIDER (HOLDS RPM IN DEMO, FOLLOWS REAL MOTORCYCLE IN BLE)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isBleConnected) Icons.Default.Speed else Icons.Default.Tune,
                            contentDescription = "Throttle Slider",
                            tint = if (isBleConnected) RacingLime else MotecOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBleConnected) "SLIDER REAL TIME (IKUTI RPM MOTOR)" else "SLIDER TACHO (TAHAN RPM DEMO)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isBleConnected) RacingLime else MotecOrange
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
                                demoThrottleSlider > 0.01f -> "TAHAN • ${(demoThrottleSlider * 100).toInt()}% ($currentRpm RPM)"
                                else -> "IDLE • $currentRpm RPM"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isBleConnected) RacingLime else ElectricCyan
                        )
                    }
                }

                // Interactive Slider
                Slider(
                    value = displaySliderValue,
                    onValueChange = { newVal ->
                        if (!isBleConnected) {
                            viewModel.setDemoThrottle(newVal)
                        }
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

                // Quick Preset RPM Buttons & Momentary Blip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.resetDemoThrottle() },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfacePanel),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("IDLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ElectricCyan, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = { viewModel.setDemoRpmDirect(5000f) },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfacePanel),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("5K", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = { viewModel.setDemoRpmDirect(8000f) },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfacePanel),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("8K", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MotecOrange, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = { viewModel.setDemoRpmDirect(revLimit.toFloat()) },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfacePanel),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1.2f).height(34.dp)
                    ) {
                        Text("LIMITER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RaceRedline, fontFamily = FontFamily.Monospace)
                    }

                    // Momentary Quick Blip & Hold Gas Button (Simulasi Putar Tuas Gas)
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isRevving) RacingLime else MotecOrange)
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
                        Text(
                            text = if (isRevving) "GAS!!" else "BLIP GAS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = CarbonDark,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Quick Idle / Engine Reset button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "THROTTLE: ${telemetry.tps / 10f}% • TPS ADC",
                fontSize = 11.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            TextButton(
                onClick = { viewModel.resetVirtualEngine() },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.testTag("reset_engine_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Engine",
                    tint = SensorAmber,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "RESET RPM / IDLE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensorAmber,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // TECHNICAL DATA GRID & HARDWARE DIAGNOSTICS (MoTeC / AIM Race Studio Style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TECHNICAL DATA GRID & HARDWARE STATUS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "SEQ: #${telemetry.sequence}",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                            else -> "ACTIVE • #$telemetryPacketCount"
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
                    TechDataRow("MODE FIRMWARE", "${fwMode.name} (${if (fwMode == FirmwareRunMode.DIY) "MANDIRI" else if (fwMode == FirmwareRunMode.OEM_LEARN) "BACA OEM PB3/PB4" else "MANUAL"})", ElectricCyan)
                    TechDataRow("TARGET TEGANGAN HV", "$targetHv V (${if (isPro) "PRO 345V" else "NORMAL 285V"})", RacingLime)
                    TechDataRow("OUTPUT COILS", "CENTER: ${if (telemetry.centerEnabled) "ON" else "OFF"} | SIDE: ${if (telemetry.sideEnabled) "ON" else "OFF"}", RacingLime)
                    TechDataRow("PULSER QUALITY", "${telemetry.pickupQuality} / 100 (PPR=1 Gate=80µs)", if (telemetry.pickupQuality >= 10) RacingLime else RaceRedline)
                    TechDataRow("TRIGGER TIMING", "%.1f° BTDC".format(telemetry.triggerCdeg / 100f), ElectricCyan)
                    TechDataRow("FAN RELAY (J1.7)", if (telemetry.fanEnabled) "ACTIVE (PB5 LOW)" else "OFF (HIGH)", if (telemetry.fanEnabled) SensorAmber else TextMuted)
                    TechDataRow("FAULT BITS", if (telemetry.faults == 0) "0x0000 (NO FAULT)" else "0x%04X".format(telemetry.faults), if (telemetry.faults == 0) RacingLime else RaceRedline)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TechDataRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
        Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = valueColor, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun CapacitorMeter(
    modifier: Modifier = Modifier,
    label: String,
    voltage: Int,
    isFull: Boolean
) {
    Card(
        modifier = modifier.border(1.dp, if (isFull) RacingLime.copy(alpha = 0.5f) else BorderSubtle, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfacePanel),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${voltage}V",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (isFull) TextPrimary else SensorAmber
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isFull) RacingLime.copy(alpha = 0.2f) else SensorAmber.copy(alpha = 0.2f))
                    .border(1.dp, if (isFull) RacingLime else SensorAmber, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isFull) "CHARGED" else "CHARGING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFull) RacingLime else SensorAmber,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
