package id.ns200.cdir7.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
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
import kotlin.math.roundToInt

@Composable
fun DynoLiveTuneCard(vm: CdiViewModel) {
    val active by vm.dynoActive.collectAsState()
    val trim by vm.dynoTrimDeg.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (active) 1.5.dp else 1.dp,
                color = if (active) MotecOrange else BorderSubtle,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (active) CardHover else CardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Title + Live Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (active) RacingLime else MotecOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "LIVE REMAP / DYNO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (active) TextPrimary else MotecOrange
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (active) RacingLime.copy(alpha = 0.15f) else SurfacePanel)
                        .border(1.dp, if (active) RacingLime else BorderSubtle, RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (active) "● LIVE ACTIVE" else "STANDBY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (active) RacingLime else TextMuted
                    )
                }
            }

            // Digital Trim Readout & Precision Step Controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfacePanel,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TRIM ADVANCE",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                        val trimFormatted = if (trim > 0f) "+%.1f°".format(trim) else "%.1f°".format(trim)
                        val trimColor = when {
                            !active -> TextMuted
                            trim == 0f -> RacingLime
                            trim > 0f -> MotecOrange
                            else -> ElectricCyan
                        }
                        Text(
                            text = trimFormatted,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = trimColor
                        )
                    }

                    // Stepper buttons for precision tuning without slider jitter
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DynoStepButton(
                            label = "-0.5°",
                            enabled = active,
                            onClick = { vm.setDynoTrim(((trim - 0.5f) * 10f).roundToInt() / 10f) }
                        )
                        DynoStepButton(
                            label = "0.0°",
                            enabled = active && trim != 0f,
                            onClick = { vm.setDynoTrim(0f) }
                        )
                        DynoStepButton(
                            label = "+0.5°",
                            enabled = active,
                            onClick = { vm.setDynoTrim(((trim + 0.5f) * 10f).roundToInt() / 10f) }
                        )
                    }
                }
            }

            // Precision Trim Slider
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Slider(
                    value = trim,
                    onValueChange = vm::setDynoTrim,
                    valueRange = -10f..10f,
                    enabled = active,
                    colors = SliderDefaults.colors(
                        thumbColor = if (active) MotecOrange else BorderSubtle,
                        activeTrackColor = if (active) MotecOrange else BorderSubtle,
                        inactiveTrackColor = SurfacePanel,
                        disabledThumbColor = BorderSubtle,
                        disabledActiveTrackColor = BorderSubtle,
                        disabledInactiveTrackColor = SurfacePanel
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "-10.0° RETARD",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (active) ElectricCyan else TextMuted
                    )
                    Text(
                        text = "0.0° BASE",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                    Text(
                        text = "+10.0° ADVANCE",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (active) MotecOrange else TextMuted
                    )
                }
            }

            // Action Buttons
            if (!active) {
                Button(
                    onClick = vm::beginDynoTune,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MotecOrange,
                        contentColor = CarbonDark
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MULAI LIVE DYNO TUNE",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { vm.finishDynoTune(true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RacingLime,
                            contentColor = CarbonDark
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "COMMIT MAP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    OutlinedButton(
                        onClick = { vm.finishDynoTune(false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RaceRedline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RaceRedline
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ABORT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Safety rule
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mulai/commit aman: RPM 0 dan HV < 30V",
                    fontSize = 9.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "LIVE OFFSET",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (active) MotecOrange else TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun DynoStepButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (enabled) CardHover else SurfacePanel)
            .border(1.dp, if (enabled) BorderAccent else BorderSubtle, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (enabled) TextPrimary else TextMuted
        )
    }
}

