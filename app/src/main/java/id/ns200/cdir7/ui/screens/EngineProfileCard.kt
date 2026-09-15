package id.ns200.cdir7.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
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
import id.ns200.cdir7.EngineProfile
import id.ns200.cdir7.ui.theme.*

@Composable
fun EngineProfileCard(vm: CdiViewModel) {
    val profile by vm.engineProfile.collectAsState()
    val caps by vm.firmwareCapabilities.collectAsState()

    val isNs200 = profile.name.contains("NS200", ignoreCase = true)
    val isUniversal = profile.name.contains("UNIVERSAL", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Title + Active Profile Badge
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
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MotecOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "ENGINE PROFILE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MotecOrange
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ElectricCyan.copy(alpha = 0.12f))
                        .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = profile.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = ElectricCyan
                    )
                }
            }

            // Telemetry Grid: 4 data tiles (2x2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileSpecTile(
                    modifier = Modifier.weight(1f),
                    label = "RPM LIMIT",
                    value = "${profile.rpmMin} - ${profile.rpmMax}",
                    accentColor = TextPrimary
                )
                ProfileSpecTile(
                    modifier = Modifier.weight(1f),
                    label = "TIMING LIMIT",
                    value = "${profile.advanceMinDeg}°..+${profile.advanceMaxDeg}°",
                    accentColor = SparkAmber
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileSpecTile(
                    modifier = Modifier.weight(1f),
                    label = "PULSER / TRIG",
                    value = "PPR ${profile.pulserPpr} • ${profile.triggerAngleDeg}°",
                    accentColor = TextPrimary
                )
                ProfileSpecTile(
                    modifier = Modifier.weight(1f),
                    label = "FIRMWARE GRID",
                    value = "v${caps.protocolVersion} • ${caps.maxRpmPoints}x${caps.maxLoadPoints} (${caps.mapSlots}S)",
                    accentColor = ElectricCyan
                )
            }

            // Segmented Profile Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfacePanel)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ProfileSegmentButton(
                    modifier = Modifier.weight(1f),
                    title = "NS200 BASE",
                    subtitle = "800-11.5k RPM",
                    selected = isNs200,
                    onClick = { vm.setEngineProfile(EngineProfile.ns200()) }
                )
                ProfileSegmentButton(
                    modifier = Modifier.weight(1f),
                    title = "UNIVERSAL",
                    subtitle = "300-22k RPM",
                    selected = isUniversal,
                    onClick = { vm.setEngineProfile(EngineProfile.universal()) }
                )
            }

            // Disclaimer Footnote
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Batas profil adalah software guardrail, bukan jaminan mekanis mesin/koil.",
                    fontSize = 9.5.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileSpecTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color
) {
    Surface(
        modifier = modifier,
        color = SurfacePanel,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = accentColor
            )
        }
    }
}

@Composable
private fun ProfileSegmentButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) MotecOrange else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (selected) CarbonDark else TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                color = if (selected) CarbonDark.copy(alpha = 0.8f) else TextMuted
            )
        }
    }
}

