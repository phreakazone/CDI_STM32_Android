package id.ns200.cdir7.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
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
    val moduleStatus by vm.moduleStatus.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Title + Active Profile Badge + Modular Tag
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
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MotecOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "GUARDRAIL PROFIL MESIN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MotecOrange
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ElectricCyan.copy(alpha = 0.12f))
                            .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = profile.name.replace("_", " "),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ElectricCyan
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(RacingLime.copy(alpha = 0.12f))
                            .border(1.dp, RacingLime.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${profile.rpmMax} RPM",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = RacingLime
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

                // Description of active profile
                Text(
                    text = profile.description,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    lineHeight = 13.sp
                )

            // Telemetry Grid: 4 data tiles (2x2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileSpecTile(
                    modifier = Modifier.weight(1f),
                    label = "RENTANG RPM",
                    value = "${profile.rpmMin} - ${profile.rpmMax}",
                    accentColor = TextPrimary
                )
                ProfileSpecTile(
                    modifier = Modifier.weight(1f),
                    label = "TIMING ADVANCE",
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
                    label = "PULSER / TRIGGER",
                    value = "PPR ${profile.pulserPpr} • ${profile.triggerAngleDeg}°",
                    accentColor = TextPrimary
                )
                ProfileSpecTile(
                    modifier = Modifier.weight(1f),
                    label = "GRID FIRMWARE",
                    value = "v${caps.protocolVersion} • ${caps.maxRpmPoints}x${caps.maxLoadPoints} (${caps.mapSlots}S)",
                    accentColor = ElectricCyan
                )
            }

            // Segmented Profile Selector
            Text(
                text = "PILIH KARAKTER / PROFIL MESIN:",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )

            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SurfacePanel)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(3.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EngineProfile.ALL_PROFILES.forEach { item ->
                    val isSelected = profile.name == item.name
                    val (displayTitle, displayTag, displaySubtitle) = when (item.name) {
                        "STD_STREET" -> Triple("STANDAR", "HARIAN", "Max 10.500 RPM")
                        "TOURING" -> Triple("TOURING", "ENDURANCE", "Max 11.500 RPM")
                        "HIGH_REV" -> Triple("HIGH REV", "RACING", "Max 15.000 RPM")
                        "CUSTOM" -> Triple("KUSTOM", "BEBAS", "Spesifikasi Mandiri")
                        else -> Triple(item.name.replace("_", " "), "PROFIL", "${item.rpmMin / 1000}k-${item.rpmMax / 1000}k RPM")
                    }
                    ModularProfileChip(
                        title = displayTitle,
                        subtitle = displaySubtitle,
                        category = displayTag,
                        selected = isSelected,
                        onClick = { vm.setEngineProfile(item) }
                    )
                }
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
                    text = "Batas profil adalah software guardrail proteksi modular, disesuaikan dengan konfigurasi koil dan sensor.",
                    fontSize = 9.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 12.sp
                )
            }
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
private fun ModularProfileChip(
    title: String,
    subtitle: String,
    category: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(if (selected) MotecOrange.copy(alpha = 0.22f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) MotecOrange.copy(alpha = 0.9f) else BorderSubtle,
                RoundedCornerShape(2.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (selected) Color.White else TextPrimary
                )
                Text(
                    text = "[$category]",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (selected) RacingLime else TextMuted
                )
            }
            Text(
                text = subtitle,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (selected) MotecOrange else TextSecondary
            )
        }
    }
}
