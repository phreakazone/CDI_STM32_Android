package id.ns200.cdir7.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ns200.cdir7.CdiViewModel
import id.ns200.cdir7.EngineSound
import id.ns200.cdir7.ui.components.MotecButton
import id.ns200.cdir7.ui.theme.*
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun SoundScreen(viewModel: CdiViewModel) {
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val soundVolume by viewModel.soundVolume.collectAsState()
    val soundPreset by viewModel.soundPreset.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val isRevving by viewModel.isRevving.collectAsState()
    val demoThrottleSlider by viewModel.demoThrottleSlider.collectAsState()
    val demoEngineRunning by viewModel.demoEngineRunning.collectAsState()
    val scrollState = rememberScrollState()

    var selectedCategoryFilter by remember { mutableStateOf("SEMUA") }
    val categories = listOf("SEMUA", "KAWASAKI", "MOGE CC BESAR", "SUPERSPORT & BALAP", "STANDAR & KUSTOM")

    val filteredPresets = remember(selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "KAWASAKI" -> EngineSound.Preset.entries.filter {
                it == EngineSound.Preset.NINJA250 || it == EngineSound.Preset.ZX25R
            }
            "MOGE CC BESAR" -> EngineSound.Preset.entries.filter {
                it == EngineSound.Preset.MOGE_SUPERBASS ||
                it == EngineSound.Preset.SUPERBIKE1000 || it == EngineSound.Preset.CROSS4 ||
                it == EngineSound.Preset.DUCATI1200 || it == EngineSound.Preset.CRUISER_VTWIN
            }
            "SUPERSPORT & BALAP" -> EngineSound.Preset.entries.filter {
                it == EngineSound.Preset.INLINE4 || it == EngineSound.Preset.INLINE3 ||
                it == EngineSound.Preset.TWIN270 || it == EngineSound.Preset.V4
            }
            "STANDAR & KUSTOM" -> EngineSound.Preset.entries.filter {
                it == EngineSound.Preset.SINGLE || it == EngineSound.Preset.CUSTOM
            }
            else -> EngineSound.Preset.entries
        }
    }

    // File picker launcher for custom audio
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomAudioFile(uri)
        }
    }

    // Audio visualizer wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // MASTER AUDIO SWITCH CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (soundEnabled) RacingLime else BorderSubtle,
                    RoundedCornerShape(14.dp)
                )
                .testTag("master_audio_card"),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (soundEnabled) RacingLime.copy(alpha = 0.2f) else SurfacePanel)
                            .border(1.dp, if (soundEnabled) RacingLime else BorderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Sound Status",
                            tint = if (soundEnabled) RacingLime else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "VIRTUAL SOUND ENGINE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        Text(
                            text = if (soundEnabled) "AKTIF • Sintesis Suara Sesuai RPM" else "MUTED (Tekan Saklar Untuk Aktifkan)",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (soundEnabled) RacingLime else TextMuted
                        )
                    }
                }

                Switch(
                    checked = soundEnabled,
                    onCheckedChange = { viewModel.setSoundEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CarbonDark,
                        checkedTrackColor = RacingLime,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfacePanel
                    )
                )
            }
        }

        // VOLUME SLIDER CARD
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
                        text = "VOLUME SPEAKER HP / BLUETOOTH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MotecOrange,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${(soundVolume * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Slider(
                    value = soundVolume,
                    onValueChange = { viewModel.setSoundVolume(it) },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MotecOrange,
                        activeTrackColor = MotecOrange,
                        inactiveTrackColor = SurfacePanel
                    )
                )
            }
        }

        // LIVE AUDIO ENGINE TEST BENCH WITH INTERACTIVE THROTTLE / RPM SLIDER
        val isBleMotorRunning = viewModel.bleClient.gattReady && telemetry.rpm > 100
        var soundUserDragFraction by remember { mutableStateOf<Float?>(null) }
        val soundActualFraction = (telemetry.rpm.toFloat() / 12000f).coerceIn(0f, 1f)
        val activeTestSlider = soundUserDragFraction ?: soundActualFraction

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, if (isBleMotorRunning) RacingLime else MotecOrange, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
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
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Test Bench",
                            tint = if (isBleMotorRunning) RacingLime else MotecOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE AUDIO ENGINE TEST BENCH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isBleMotorRunning) RacingLime else MotecOrange
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
                                isBleMotorRunning -> "LIVE MOTOR • ${telemetry.rpm} RPM"
                                isRevving -> "BLIP GAS AKTIF!"
                                demoThrottleSlider > 0.01f -> "HOLD ${(demoThrottleSlider * 100).toInt()}% • ${telemetry.rpm} RPM"
                                !demoEngineRunning || telemetry.rpm <= 50 -> "MESIN MATI • 0 RPM"
                                else -> "IDLE • ${telemetry.rpm} RPM"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isBleMotorRunning -> RacingLime
                                isRevving -> RaceRedline
                                demoThrottleSlider > 0.01f -> MotecOrange
                                !demoEngineRunning || telemetry.rpm <= 50 -> SensorAmber
                                else -> ElectricCyan
                            },
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (!isBleMotorRunning && (!demoEngineRunning || telemetry.rpm <= 50)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfacePanel)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mesin sedang mati (0 RPM)",
                            fontSize = 10.sp,
                            color = SensorAmber,
                            fontFamily = FontFamily.Monospace
                        )
                        MotecButton(
                            text = "STARTER",
                            onClick = { viewModel.simulateStartEngine() },
                            color = RacingLime,
                            icon = Icons.Default.PlayArrow,
                            height = 26.dp
                        )
                    }
                }

                // RPM and Preset Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "PUTARAN MESIN (RPM)",
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${telemetry.rpm}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (telemetry.rpm > 9000) RaceRedline else RacingLime,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "THROTTLE: ${(telemetry.tps / 10f).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = soundPreset.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Interactive RPM Throttle Slider (Follows Live RPM & allows manual drag)
                Slider(
                    value = activeTestSlider,
                    onValueChange = { newVal ->
                        if (!isBleMotorRunning) {
                            soundUserDragFraction = newVal
                            viewModel.setDemoRpmDirect(newVal * 12000f)
                        }
                    },
                    onValueChangeFinished = {
                        soundUserDragFraction = null
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (isBleMotorRunning) RacingLime else MotecOrange,
                        activeTrackColor = if (isBleMotorRunning) RacingLime else MotecOrange,
                        inactiveTrackColor = SurfacePanel
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sound_screen_throttle_slider")
                )

                // Scale markings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("IDLE 1.4K", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text("4.5K CRUISE", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text("8.0K POWER", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text("11.5K REDLINE", fontSize = 9.sp, color = RaceRedline, fontFamily = FontFamily.Monospace)
                }

                // Preset Quick Buttons & Momentary Blip Gas Button
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
                        text = "4.5K",
                        onClick = { viewModel.setDemoRpmDirect(4500f) },
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
                        onClick = { viewModel.setDemoRpmDirect(11500f) },
                        color = RaceRedline,
                        height = 30.dp,
                        modifier = Modifier.weight(1.2f)
                    )

                    // Momentary Quick Blip & Hold Gas Button (Simulasi Putar Tuas Gas)
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(30.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isRevving) RaceRedline.copy(alpha = 0.35f)
                                else MotecOrange.copy(alpha = 0.2f)
                            )
                            .border(
                                1.dp,
                                if (isRevving) RaceRedline else MotecOrange,
                                RoundedCornerShape(3.dp)
                            )
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
                            .testTag("hold_to_rev_sound_screen_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRevving) "GAS!!" else "BLIP GAS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isRevving) RaceRedline else MotecOrange,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // PRESET SELECTION HEADER & HORIZONTALLY SCROLLABLE FILTER CHIPS (NO EMPTY GAPS)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PILIHAN PRESET AKUSTIK KNALPOT (${filteredPresets.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = ElectricCyan
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfacePanel)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "AKTIF: ${soundPreset.cylinderCount}",
                    fontSize = 9.sp,
                    color = RacingLime,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Category Filter Chips with Horizontal Scroll (Sharp corners semi-transparent)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { cat ->
                val isFilterSelected = cat == selectedCategoryFilter
                val count = when (cat) {
                    "KAWASAKI" -> 2
                    "MOGE CC BESAR" -> 5
                    "SUPERSPORT & BALAP" -> 4
                    "STANDAR & KUSTOM" -> 2
                    else -> EngineSound.Preset.entries.size
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isFilterSelected) ElectricCyan.copy(alpha = 0.22f) else SurfacePanel)
                        .border(
                            1.dp,
                            if (isFilterSelected) ElectricCyan.copy(alpha = 0.85f) else BorderSubtle,
                            RoundedCornerShape(2.dp)
                        )
                        .clickable { selectedCategoryFilter = cat }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 9.5.sp,
                            fontWeight = if (isFilterSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isFilterSelected) Color.White else TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isFilterSelected) ElectricCyan else SurfacePanel)
                                .border(0.5.dp, if (isFilterSelected) ElectricCyan else BorderSubtle, RoundedCornerShape(1.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isFilterSelected) CarbonDark else TextMuted
                            )
                        }
                    }
                }
            }
        }

        // PRESET CARDS LIST
        filteredPresets.forEach { preset ->
            val isSelected = preset == soundPreset
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        if (isSelected) ElectricCyan else BorderSubtle,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        if (preset == EngineSound.Preset.CUSTOM) {
                            filePicker.launch(arrayOf("audio/mpeg", "audio/wav", "audio/ogg", "audio/*"))
                        } else {
                            viewModel.setSoundPreset(preset)
                        }
                    }
                    .testTag("sound_preset_${preset.name}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) CardHover else CardBackground
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) ElectricCyan else BorderSubtle)
                            )
                            Text(
                                text = preset.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ElectricCyan.copy(alpha = 0.2f))
                                    .border(1.dp, ElectricCyan, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AKTIF",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Badges: Category, Cylinder, Max RPM
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfacePanel)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = preset.category,
                                fontSize = 9.sp,
                                color = RacingLime,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfacePanel)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = preset.cylinderCount,
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfacePanel)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Redline: ${preset.maxRpmDisplay}",
                                fontSize = 9.sp,
                                color = MotecOrange,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = preset.description,
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )

                    // Audition / Test Audio Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        MotecButton(
                            text = if (isSelected) "SUARA AKTIF • DENGARKAN" else "PILIH & TEST SUARA",
                            onClick = {
                                if (preset == EngineSound.Preset.CUSTOM) {
                                    filePicker.launch(arrayOf("audio/mpeg", "audio/wav", "audio/ogg", "audio/*"))
                                } else {
                                    viewModel.setSoundPreset(preset)
                                }
                            },
                            color = if (isSelected) ElectricCyan else TextSecondary,
                            icon = if (isSelected) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                            height = 26.dp
                        )
                    }
                }
            }
        }

        // CUSTOM AUDIO TRACKS MANAGER (Shown Only When Relevant, No Empty Container Box)
        val customTracks by viewModel.customSoundTracks.collectAsState()
        val selectedCustomTrack by viewModel.selectedCustomTrack.collectAsState()

        if (soundPreset == EngineSound.Preset.CUSTOM || selectedCategoryFilter == "STANDAR & KUSTOM") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CUSTOM AUDIO FILE (MP3 / WAV / OGG)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MotecOrange,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Sintesis pitch otomatis mengikuti putaran RPM",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        MotecButton(
                            text = "PILIH BERKAS",
                            onClick = { filePicker.launch(arrayOf("audio/mpeg", "audio/wav", "audio/ogg", "audio/*")) },
                            color = MotecOrange,
                            icon = Icons.Default.Add,
                            height = 28.dp
                        )
                    }

                    if (customTracks.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            customTracks.forEach { track ->
                                val isSel = selectedCustomTrack?.id == track.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) CardHover else SurfacePanel)
                                        .border(1.dp, if (isSel) ElectricCyan else BorderSubtle, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectCustomTrack(track) }
                                    .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.Audiotrack,
                                            contentDescription = "Audio",
                                            tint = if (isSel) ElectricCyan else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = track.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) TextPrimary else TextSecondary,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "Base: ${track.baseRpm} RPM • ${track.format}",
                                                fontSize = 9.sp,
                                                color = TextMuted,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeCustomTrack(track) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RaceRedline, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // BLUETOOTH AMPLIFIER TRANSMISSION ROUTE (MH-M18 & PAM8610 2x10W BTL)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "TRANSMISI AUDIO BLUETOOTH KE AMPLIFIER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensorAmber,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Simulasi Rute Modul MH-M18 & PAM8610 (2x10W BTL)",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Diagram Flow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModuleBox(
                        title = "PHONE / APP",
                        desc = "A2DP Stream",
                        status = if (soundEnabled) "TRANSMITTING" else "IDLE",
                        color = if (soundEnabled) RacingLime else TextMuted
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "to",
                        tint = if (soundEnabled) RacingLime else BorderSubtle,
                        modifier = Modifier.size(16.dp)
                    )

                    ModuleBox(
                        title = "MH-M18",
                        desc = "Lossless BLE RX",
                        status = "PAIRED",
                        color = ElectricCyan
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "to",
                        tint = if (soundEnabled) RacingLime else BorderSubtle,
                        modifier = Modifier.size(16.dp)
                    )

                    ModuleBox(
                        title = "PAM8610",
                        desc = "2x10W BTL",
                        status = "STANDBY",
                        color = MotecOrange
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Audio Waveform Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(SurfacePanel, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val midY = h / 2f
                        val isPlaying = soundEnabled && telemetry.rpm > 500

                        val bars = 36
                        for (i in 0 until bars) {
                            val x = w * (i.toFloat() / bars)
                            val amplitude = if (isPlaying) {
                                val sinVal = abs(sin((i * 0.4f + phase).toDouble())).toFloat()
                                (sinVal * (h * 0.4f) * soundVolume).coerceIn(4f, h * 0.45f)
                            } else 3f

                            drawLine(
                                color = if (isPlaying) ElectricCyan else TextMuted.copy(alpha = 0.3f),
                                start = Offset(x, midY - amplitude),
                                end = Offset(x, midY + amplitude),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Konfigurasi Pin: VCC (5V DC Step-Down), GND (Sasis), L-OUT/R-OUT ke PAM8610 INL/INR.",
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ModuleBox(
    title: String,
    desc: String,
    status: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfacePanel)
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = FontFamily.Monospace)
            Text(desc, fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text(status, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = FontFamily.Monospace)
        }
    }
}
