package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.screens.WiringWorkshopHubScreen
import com.example.viewmodel.WiringViewModel
import id.ns200.cdir7.CdiViewModel
import id.ns200.cdir7.McuPlatform
import id.ns200.cdir7.ScreenTab
import id.ns200.cdir7.Telemetry
import id.ns200.cdir7.ui.screens.*
import id.ns200.cdir7.ui.theme.*

class MainActivity : ComponentActivity() {

    private val cdiViewModel: CdiViewModel by viewModels()
    private val wiringViewModel: WiringViewModel by viewModels()

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                cdiViewModel.toggleConnect()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CdiR7Theme {
                MainAppScreen(
                    cdiViewModel = cdiViewModel,
                    wiringViewModel = wiringViewModel,
                    onRequestPermissions = { checkAndRequestPermissions(triggerConnect = true) }
                )
            }
        }
    }

    private fun checkAndRequestPermissions(triggerConnect: Boolean = true) {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissions.toTypedArray())
        } else if (triggerConnect) {
            cdiViewModel.toggleConnect()
        }
    }
}

@Composable
fun MainAppScreen(
    cdiViewModel: CdiViewModel,
    wiringViewModel: WiringViewModel,
    onRequestPermissions: () -> Unit
) {
    val currentTab by cdiViewModel.currentTab.collectAsState()
    val isConnected by cdiViewModel.isConnected.collectAsState()
    val isSimulation by cdiViewModel.isSimulationMode.collectAsState()
    val isBleBusy by cdiViewModel.isBleBusy.collectAsState()
    val isBleScanning by cdiViewModel.isBleScanning.collectAsState()
    val telemetry by cdiViewModel.telemetry.collectAsState()
    val packetRateHz by cdiViewModel.packetRateHz.collectAsState()
    val verificationProgress by wiringViewModel.verificationProgress.collectAsState()
    val connectedDeviceName by cdiViewModel.connectedDeviceName.collectAsState()
    val selectedPlatform by cdiViewModel.selectedPlatform.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .testTag("main_app_scaffold"),
        topBar = {
            MotorsportTopBar(
                isConnected = isConnected && !isSimulation,
                isSimulation = isSimulation,
                isBleBusy = isBleBusy,
                isBleScanning = isBleScanning,
                telemetry = telemetry,
                packetRateHz = packetRateHz,
                verificationProgress = verificationProgress,
                connectedDeviceName = connectedDeviceName,
                onConnectClick = {
                    if (isBleScanning || isBleBusy || isConnected) {
                        cdiViewModel.toggleConnect()
                    } else {
                        onRequestPermissions()
                    }
                },
                onDemoClick = { cdiViewModel.toggleSimulation() }
            )
        },
        bottomBar = {
            MotorsportBottomNav(
                currentTab = currentTab,
                onTabSelect = { cdiViewModel.setTab(it) }
            )
        },
        containerColor = CarbonDark,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.TACHO -> DashboardScreen(cdiViewModel)
                ScreenTab.MAPS -> MapsScreen(cdiViewModel)
                ScreenTab.WIRING -> WiringWorkshopHubScreen(
                    wiringViewModel = wiringViewModel,
                    cdiViewModel = cdiViewModel
                )
                ScreenTab.SETUP -> SetupScreen(cdiViewModel)
                ScreenTab.SUARA -> SoundScreen(cdiViewModel)
                ScreenTab.BLE -> BleHexScreen(
                    viewModel = cdiViewModel,
                    onRequestPermissions = onRequestPermissions
                )
            }
        }
    }
}

@Composable
fun MotorsportTopBar(
    isConnected: Boolean,
    isSimulation: Boolean,
    isBleBusy: Boolean,
    isBleScanning: Boolean,
    telemetry: Telemetry,
    packetRateHz: Int = 0,
    verificationProgress: Pair<Int, Int>,
    connectedDeviceName: String?,
    onConnectClick: () -> Unit,
    onDemoClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle),
        color = SurfacePanel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and status dot (Clean, concise, and structured)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isConnected -> RacingLime
                                    isSimulation -> MotecOrange
                                    isBleScanning || isBleBusy -> ElectricCyan
                                    else -> RaceRedline
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NS200-CDI",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MotecOrange.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "R8",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MotecOrange,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = when {
                                isConnected -> {
                                    val dev = if (!connectedDeviceName.isNullOrBlank()) connectedDeviceName else "BLE"
                                    if (packetRateHz > 0) "ONLINE • $dev • ${packetRateHz}Hz" else "ONLINE • $dev"
                                }
                                isSimulation -> "SIMULASI • 50Hz • ${telemetry.rpm} RPM"
                                isBleScanning -> "MEMINDAI BLE..."
                                isBleBusy -> "MENGHUBUNGKAN..."
                                else -> "OFFLINE • BLE SIAP"
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                isConnected -> RacingLime
                                isSimulation -> MotecOrange
                                isBleScanning || isBleBusy -> ElectricCyan
                                else -> TextMuted
                            }
                        )
                    }
                }

                // Action buttons: DEMO and CONNECT (Spacious, fixed minimum widths to prevent squeezing)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Demo Mode Toggle
                    OutlinedButton(
                        onClick = onDemoClick,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isSimulation) MotecOrange else TextSecondary
                        ),
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .testTag("topbar_demo_btn")
                    ) {
                        Text(
                            text = if (isSimulation) "SIM ON" else "DEMO",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // BLE Connect Button (Generous minWidth so KONEK is never cramped)
                    Button(
                        onClick = onConnectClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isConnected -> RaceRedline
                                isBleScanning || isBleBusy -> ElectricCyan
                                else -> RacingLime
                            }
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 76.dp)
                            .heightIn(min = 36.dp)
                            .testTag("topbar_connect_btn")
                    ) {
                        Text(
                            text = when {
                                isConnected -> "PUTUS"
                                isBleScanning -> "SCAN"
                                isBleBusy -> "BATAL"
                                else -> "KONEK"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = CarbonDark,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Quick live telemetry bar
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryMetricItem(
                    label = "BATT",
                    value = if (isConnected || isSimulation) "%.1fV".format(telemetry.batteryCv / 100f) else "--.-V",
                    color = if (telemetry.batteryCv < 1150 && (isConnected || isSimulation)) RaceRedline else RacingLime
                )
                TelemetryMetricItem(
                    label = "HV CTR",
                    value = if (isConnected || isSimulation) "${telemetry.hvCenter}V" else "---V",
                    color = if (telemetry.hvCenter >= 280) RacingLime else MotecOrange
                )
                TelemetryMetricItem(
                    label = "HV SIDE",
                    value = if (isConnected || isSimulation) "${telemetry.hvSide}V" else "---V",
                    color = if (telemetry.hvSide >= 280) RacingLime else MotecOrange
                )
                TelemetryMetricItem(
                    label = "IGN ADV",
                    value = if (isConnected || isSimulation) "%.1f°".format(telemetry.advanceCdeg / 100f) else "--.-°",
                    color = ElectricCyan
                )
                TelemetryMetricItem(
                    label = "RPM",
                    value = if (isConnected || isSimulation) "${telemetry.rpm}" else "0",
                    color = if (telemetry.rpm >= 10000) RaceRedline else TextPrimary
                )
            }
        }
    }
}

@Composable
private fun TelemetryMetricItem(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = value,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
fun MotorsportBottomNav(
    currentTab: ScreenTab,
    onTabSelect: (ScreenTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle),
        color = SurfacePanel
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScreenTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val icon = when (tab) {
                    ScreenTab.TACHO -> Icons.Default.Speed
                    ScreenTab.MAPS -> Icons.Default.ShowChart
                    ScreenTab.WIRING -> Icons.Default.Build
                    ScreenTab.SETUP -> Icons.Default.FactCheck
                    ScreenTab.SUARA -> Icons.Default.VolumeUp
                    ScreenTab.BLE -> Icons.Default.Bluetooth
                }
                val activeColor = when (tab) {
                    ScreenTab.TACHO -> RacingLime
                    ScreenTab.MAPS -> MotecOrange
                    ScreenTab.WIRING -> ElectricCyan
                    ScreenTab.SETUP -> SensorAmber
                    ScreenTab.SUARA -> ElectricCyan
                    ScreenTab.BLE -> RacingLime
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTabSelect(tab) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("nav_tab_${tab.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.title,
                        tint = if (isSelected) activeColor else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}
