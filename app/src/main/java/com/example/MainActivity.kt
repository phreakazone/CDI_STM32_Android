package com.example

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.ui.screens.WiringWorkshopHubScreen
import com.example.viewmodel.WiringViewModel
import id.ns200.cdir7.CdiViewModel
import id.ns200.cdir7.HardwareModule
import id.ns200.cdir7.ui.components.MotecButton
import id.ns200.cdir7.McuPlatform
import id.ns200.cdir7.ScreenTab
import id.ns200.cdir7.Telemetry
import id.ns200.cdir7.ui.screens.*
import id.ns200.cdir7.ui.theme.*

class MainActivity : ComponentActivity() {

    private val cdiViewModel: CdiViewModel by viewModels()
    private val wiringViewModel: WiringViewModel by viewModels()

    private var pendingPermissionAction: (() -> Unit)? = null

    private val enableLocationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val action = pendingPermissionAction
            pendingPermissionAction = null
            ensureBluetoothEnabled(action)
        }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val action = pendingPermissionAction
                pendingPermissionAction = null
                action?.invoke() ?: cdiViewModel.toggleConnect()
            } else {
                pendingPermissionAction = null
                Toast.makeText(this, "Bluetooth belum diaktifkan", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                val action = pendingPermissionAction
                pendingPermissionAction = null
                // Jika butuh scan dan lokasi belum aktif, tanyakan aktivasi lokasi
                checkLocationAndProceed(true, action)
            } else {
                pendingPermissionAction = null
                Toast.makeText(this, "Izin Bluetooth/Lokasi diperlukan untuk memindai CDI", Toast.LENGTH_SHORT).show()
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
                    onRequestBleAction = { isScan, action -> checkAndRequestPermissions(isScan, action) }
                )
            }
        }

        // Auto-connect saat startup jika fitur diaktifkan
        if (cdiViewModel.autoConnectOnStart.value && !cdiViewModel.savedDeviceMac.value.isNullOrBlank()) {
            cdiViewModel.connectDirectSaved()
        }
    }

    private fun isLocationServiceEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return true
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun checkLocationAndProceed(isScan: Boolean, onGranted: (() -> Unit)?) {
        // Pada Android 12+ (API 31+), BLUETOOTH_SCAN dengan usesPermissionFlags="neverForLocation"
        // secara resmi TIDAK memerlukan Layanan Lokasi (GPS) aktif sama sekali.
        // Pengecekan GPS hanya berlaku untuk Android 11 ke bawah (API < 31).
        if (isScan && Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !isLocationServiceEnabled()) {
            pendingPermissionAction = onGranted
            AlertDialog.Builder(this)
                .setTitle("Layanan Lokasi (GPS) Android 11 Kebawah")
                .setMessage("Keamanan Android versi lama (< Android 12) mengharuskan Layanan Lokasi aktif saat memindai BLE.\n\nTips: Anda juga bisa menggunakan fitur 'KONEKSI MAC LANGSUNG' di menu BLE untuk menghubungkan CDI tanpa menyalakan GPS.")
                .setPositiveButton("AKTIFKAN GPS") { _, _ ->
                    try {
                        enableLocationLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    } catch (e: Exception) {
                        Toast.makeText(this, "Buka Pengaturan HP untuk mengaktifkan Lokasi", Toast.LENGTH_SHORT).show()
                        ensureBluetoothEnabled(onGranted)
                    }
                }
                .setNegativeButton("TETAP SCAN") { _, _ ->
                    ensureBluetoothEnabled(onGranted)
                }
                .setNeutralButton("BATAL") { _, _ ->
                    pendingPermissionAction = null
                }
                .setCancelable(true)
                .show()
        } else {
            ensureBluetoothEnabled(onGranted)
        }
    }

    private fun checkAndRequestPermissions(isScan: Boolean = false, onGranted: (() -> Unit)? = null) {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isScan && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else if (isScan) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissions.isNotEmpty()) {
            pendingPermissionAction = onGranted
            requestPermissionsLauncher.launch(permissions.toTypedArray())
        } else {
            checkLocationAndProceed(isScan, onGranted)
        }
    }

    private fun ensureBluetoothEnabled(onGranted: (() -> Unit)?) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        if (adapter == null) {
            Toast.makeText(this, "Hardware Bluetooth tidak tersedia pada perangkat ini", Toast.LENGTH_SHORT).show()
            return
        }

        if (!adapter.isEnabled) {
            pendingPermissionAction = onGranted
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            try {
                enableBluetoothLauncher.launch(enableBtIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal meminta aktivasi Bluetooth: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            onGranted?.invoke() ?: cdiViewModel.toggleConnect()
        }
    }
}

@Composable
fun MainAppScreen(
    cdiViewModel: CdiViewModel,
    wiringViewModel: WiringViewModel,
    onRequestBleAction: (((isScan: Boolean, onGranted: (() -> Unit)?) -> Unit))? = null
) {
    val currentTab by cdiViewModel.currentTab.collectAsState()
    val isConnected by cdiViewModel.isConnected.collectAsState()
    val isSimulation by cdiViewModel.isSimulationMode.collectAsState()
    val isBleBusy by cdiViewModel.isBleBusy.collectAsState()
    val isBleScanning by cdiViewModel.isBleScanning.collectAsState()
    val telemetry by cdiViewModel.telemetry.collectAsState()
    val packetRateHz by cdiViewModel.packetRateHz.collectAsState()
    val isTelemetryStreaming by cdiViewModel.isTelemetryStreaming.collectAsState()
    val verificationProgress by wiringViewModel.verificationProgress.collectAsState()
    val connectedDeviceName by cdiViewModel.connectedDeviceName.collectAsState()
    val selectedPlatform by cdiViewModel.selectedPlatform.collectAsState()
    val moduleStatus by cdiViewModel.moduleStatus.collectAsState()
    val isSideInstalled = moduleStatus.isInstalled(HardwareModule.DUAL_COIL)
    val isSideActive = moduleStatus.isActive(HardwareModule.DUAL_COIL) || telemetry.sideEnabled
    val isDualCoil = isSideInstalled || isSideActive

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
                isDualCoil = isDualCoil,
                packetRateHz = packetRateHz,
                isTelemetryStreaming = isTelemetryStreaming,
                verificationProgress = verificationProgress,
                connectedDeviceName = connectedDeviceName,
                onConnectClick = {
                    if (isBleScanning || isBleBusy || isConnected) {
                        cdiViewModel.toggleConnect()
                    } else {
                        val hasSaved = !cdiViewModel.savedDeviceMac.value.isNullOrBlank()
                        val isScanRequired = !hasSaved
                        val hasPerms = if (isScanRequired) cdiViewModel.hasBlePermissions() else cdiViewModel.hasConnectPermission()
                        val isBtEnabled = cdiViewModel.isBluetoothEnabled()

                        if (!hasPerms || !isBtEnabled) {
                            onRequestBleAction?.invoke(isScanRequired) {
                                cdiViewModel.toggleConnect()
                            } ?: cdiViewModel.toggleConnect()
                        } else {
                            cdiViewModel.toggleConnect()
                        }
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
                ScreenTab.WIRING -> BukuPetunjukScreen(cdiViewModel)
                ScreenTab.SETUP -> SetupScreen(cdiViewModel)
                ScreenTab.SUARA -> SoundScreen(cdiViewModel)
                ScreenTab.BLE -> BleHexScreen(
                    viewModel = cdiViewModel,
                    onRequestPermissions = onRequestBleAction
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
    isDualCoil: Boolean = true,
    packetRateHz: Int = 0,
    isTelemetryStreaming: Boolean = false,
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
            val infiniteTransition = rememberInfiniteTransition(label = "ElectricPulse")
            val electricGlow by infiniteTransition.animateFloat(
                initialValue = 0.45f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(750, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ElectricGlow"
            )

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
                            // Electric lightning badge
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f * electricGlow + 0.1f))
                                    .border(
                                        1.dp,
                                        Color(0xFF00E5FF).copy(alpha = electricGlow),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Electric Discharge",
                                    tint = if (electricGlow > 0.72f) Color(0xFF00E5FF) else Color(0xFFFFC107),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "IgniTra CDI",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                style = TextStyle(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            Color.White,
                                            Color(0xFF00E5FF),
                                            Color(0xFFFFB300)
                                        )
                                    )
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "⚡ R9",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = MotecOrange
                            )
                        }
                        Text(
                            text = when {
                                isConnected -> {
                                    val dev = if (!connectedDeviceName.isNullOrBlank()) connectedDeviceName else "BLE"
                                    if (isTelemetryStreaming && packetRateHz >= 10) {
                                        "ONLINE • $dev • ${packetRateHz}Hz"
                                    } else {
                                        "ONLINE • $dev • SIAP"
                                    }
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

                // Action buttons: DEMO and CONNECT (Motec square semi-transparent style)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Demo Mode Toggle
                    MotecButton(
                        text = if (isSimulation) "SIM ON" else "DEMO",
                        onClick = onDemoClick,
                        color = if (isSimulation) MotecOrange else TextSecondary,
                        height = 30.dp,
                        fontSize = 11.sp,
                        testTag = "topbar_demo_btn"
                    )

                    // BLE Connect Button (Motec crisp semi-transparent)
                    val connectColor = when {
                        isConnected -> RaceRedline
                        isBleScanning || isBleBusy -> ElectricCyan
                        else -> RacingLime
                    }
                    val connectText = when {
                        isConnected -> "PUTUS"
                        isBleScanning -> "SCAN"
                        isBleBusy -> "BATAL"
                        else -> "KONEK"
                    }
                    val connectIcon = when {
                        isConnected -> Icons.Default.BluetoothConnected
                        isBleScanning || isBleBusy -> Icons.Default.Refresh
                        else -> Icons.Default.Bluetooth
                    }
                    MotecButton(
                        text = connectText,
                        onClick = onConnectClick,
                        color = connectColor,
                        icon = connectIcon,
                        height = 30.dp,
                        fontSize = 11.sp,
                        testTag = "topbar_connect_btn"
                    )
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
                if (isDualCoil) {
                    TelemetryMetricItem(
                        label = "HV SIDE",
                        value = if (isConnected || isSimulation) "${telemetry.hvSide}V" else "---V",
                        color = if (telemetry.hvSide >= 280) RacingLime else MotecOrange
                    )
                } else {
                    TelemetryMetricItem(
                        label = "COIL",
                        value = "1-CTR",
                        color = RacingLime
                    )
                }
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
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0F141C),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScreenTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val icon = when (tab) {
                    ScreenTab.TACHO -> Icons.Default.Speed
                    ScreenTab.MAPS -> Icons.Default.ShowChart
                    ScreenTab.SETUP -> Icons.Default.FactCheck
                    ScreenTab.SUARA -> Icons.Default.VolumeUp
                    ScreenTab.WIRING -> Icons.Default.MenuBook
                    ScreenTab.BLE -> Icons.Default.Bluetooth
                }
                val activeColor = when (tab) {
                    ScreenTab.TACHO -> RacingLime
                    ScreenTab.MAPS -> MotecOrange
                    ScreenTab.SETUP -> SensorAmber
                    ScreenTab.SUARA -> ElectricCyan
                    ScreenTab.WIRING -> ElectricCyan
                    ScreenTab.BLE -> RacingLime
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onTabSelect(tab) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("nav_tab_${tab.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.title,
                        tint = if (isSelected) activeColor else TextSecondary,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) activeColor else TextSecondary
                    )
                }
            }
        }
    }
}
