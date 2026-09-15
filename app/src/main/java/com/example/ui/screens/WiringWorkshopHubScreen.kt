package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FullCumulativeCircuitSimulator
import com.example.ui.theme.*
import com.example.viewmodel.WiringViewModel
import id.ns200.cdir7.CdiViewModel
import id.ns200.cdir7.McuPlatform
import id.ns200.cdir7.ui.screens.QuickSetupGuideScreen
import id.ns200.cdir7.ui.theme.CarbonDark
import id.ns200.cdir7.ui.theme.CardBackground
import id.ns200.cdir7.ui.theme.BorderSubtle
import id.ns200.cdir7.ui.theme.SurfacePanel
import id.ns200.cdir7.ui.theme.MotecOrange
import id.ns200.cdir7.ui.theme.RacingLime
import id.ns200.cdir7.ui.theme.SensorAmber
import id.ns200.cdir7.ui.theme.TextMuted
import id.ns200.cdir7.ui.components.MotecButton

enum class WorkshopSubTab(
    val title: String,
    val icon: ImageVector,
    val badge: String
) {
    STEPS("Langkah Solder", Icons.Default.Build, "18 STEP"),
    PCB_SIM("Simulator PCB", Icons.Default.GridView, "25x30"),
    HARNESS_J1("Soket J1", Icons.Default.Cable, "16 PIN"),
    MCU_PINOUT("Pinout MCU", Icons.Default.Memory, "PINOUT"),
    BOM_LIST("Daftar Belanja", Icons.Default.ShoppingCart, "BOM"),
    PINOUT_LIB("Katalog Part", Icons.Default.Layers, "SPECS"),
    COMMISSION("Komisi CDI", Icons.Default.CheckCircle, "STAGE 0-5")
}

@Composable
fun WiringWorkshopHubScreen(
    wiringViewModel: WiringViewModel,
    cdiViewModel: CdiViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(WorkshopSubTab.STEPS) }
    val currentStep by wiringViewModel.currentStep.collectAsState()
    val verificationRecords by wiringViewModel.verificationRecords.collectAsState()
    val verificationProgress by wiringViewModel.verificationProgress.collectAsState()
    val wiringUiState by wiringViewModel.uiState.collectAsState()
    val activePlatform = wiringUiState.mcuPlatform

    val verifiedStepIds = remember(verificationRecords) {
        verificationRecords.filter { it.value.isVerified }.keys
    }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
    ) {
        // Platform Selection Header Strip
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle),
            color = Color(0xFF0F1722)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PLATFORM:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (activePlatform == McuPlatform.STM32WB55) "STM32" else "ESP32",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (activePlatform == McuPlatform.STM32WB55) ElectricCyan else SparkAmber,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // STM32 Toggle
                    val isStm = activePlatform == McuPlatform.STM32WB55
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .clickable {
                                wiringViewModel.setMcuPlatform(McuPlatform.STM32WB55)
                                cdiViewModel.setMcuPlatform(McuPlatform.STM32WB55)
                            },
                        color = if (isStm) ElectricCyan.copy(alpha = 0.22f) else Color.Transparent,
                        border = BorderStroke(1.dp, if (isStm) ElectricCyan else BorderSubtle),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = "STM32",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (isStm) ElectricCyan else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // ESP32 Toggle
                    val isEsp = activePlatform == McuPlatform.ESP32_WROOM
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .clickable {
                                wiringViewModel.setMcuPlatform(McuPlatform.ESP32_WROOM)
                                cdiViewModel.setMcuPlatform(McuPlatform.ESP32_WROOM)
                            },
                        color = if (isEsp) SparkAmber.copy(alpha = 0.22f) else Color.Transparent,
                        border = BorderStroke(1.dp, if (isEsp) SparkAmber else BorderSubtle),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = "ESP32",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (isEsp) SparkAmber else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Workshop Sub-navigation bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle),
            color = SurfacePanel
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WorkshopSubTab.entries.forEach { subTab ->
                    val isSelected = activeSubTab == subTab
                    val pillColor = when (subTab) {
                        WorkshopSubTab.STEPS -> ElectricCyan
                        WorkshopSubTab.PCB_SIM -> SensorAmber
                        WorkshopSubTab.HARNESS_J1 -> RacingLime
                        WorkshopSubTab.MCU_PINOUT -> if (activePlatform == McuPlatform.ESP32_WROOM) SparkAmber else ElectricCyan
                        WorkshopSubTab.BOM_LIST -> MotecOrange
                        WorkshopSubTab.PINOUT_LIB -> ElectricCyan
                        WorkshopSubTab.COMMISSION -> RacingLime
                    }
                    val badgeText = when (subTab) {
                        WorkshopSubTab.STEPS -> "${verificationProgress.first}/${verificationProgress.second}"
                        WorkshopSubTab.MCU_PINOUT -> if (activePlatform == McuPlatform.STM32WB55) "35 PIN" else "38 PIN"
                        else -> subTab.badge
                    }

                    MotecButton(
                        text = subTab.title,
                        onClick = { activeSubTab = subTab },
                        color = if (isSelected) pillColor else TextMuted,
                        icon = subTab.icon,
                        height = 28.dp,
                        fontSize = 9.5.sp,
                        badge = badgeText,
                        testTag = "subtab_${subTab.name.lowercase()}",
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Active content screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (activeSubTab) {
                WorkshopSubTab.STEPS -> {
                    TutorialStepScreen(viewModel = wiringViewModel)
                }
                WorkshopSubTab.PCB_SIM -> {
                    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        FullCumulativeCircuitSimulator(
                            currentStep = currentStep,
                            allSteps = wiringViewModel.allSteps,
                            verifiedStepIds = verifiedStepIds,
                            onSelectStep = { wiringViewModel.selectStep(it) },
                            onClose = { activeSubTab = WorkshopSubTab.STEPS },
                            platform = activePlatform
                        )
                    }
                }
                WorkshopSubTab.HARNESS_J1 -> {
                    HarnessPinsScreen(viewModel = wiringViewModel)
                }
                WorkshopSubTab.MCU_PINOUT -> {
                    WeActHeaderScreen(viewModel = wiringViewModel)
                }
                WorkshopSubTab.BOM_LIST -> {
                    BomChecklistScreen(viewModel = wiringViewModel)
                }
                WorkshopSubTab.PINOUT_LIB -> {
                    ComponentLibraryScreen(viewModel = wiringViewModel)
                }
                WorkshopSubTab.COMMISSION -> {
                    QuickSetupGuideScreen(viewModel = cdiViewModel)
                }
            }
        }
    }
}
