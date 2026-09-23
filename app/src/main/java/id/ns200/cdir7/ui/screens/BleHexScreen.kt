package id.ns200.cdir7.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.location.LocationManagerCompat
import id.ns200.cdir7.CdiProtocol
import id.ns200.cdir7.CdiViewModel
import id.ns200.cdir7.OtaState
import id.ns200.cdir7.ui.components.MotecButton
import id.ns200.cdir7.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LinkQuality(val label: String, val color: Color) {
    STABIL("STABIL", Color(0xFF00E676)),
    CUKUP("CUKUP", Color(0xFFFFB300)),
    BURUK("BURUK", Color(0xFFFF3D00)),
    STANDBY("STANDBY", Color(0xFF00E5FF)),
    TERPUTUS("TERPUTUS", Color(0xFF757575))
}

fun evaluateLinkQuality(
    connected: Boolean,
    rateHz: Int,
    crcPercent: Float
): LinkQuality {
    if (!connected) {
        return LinkQuality.TERPUTUS
    }
    if (rateHz <= 0) {
        return LinkQuality.STANDBY
    }

    return when {
        crcPercent < 90f || rateHz < 8 -> LinkQuality.BURUK
        rateHz in 16..24 && crcPercent >= 98f -> LinkQuality.STABIL
        else -> LinkQuality.CUKUP
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BleHexScreen(
    viewModel: CdiViewModel,
    onRequestPermissions: (((isScan: Boolean, onGranted: (() -> Unit)?) -> Unit))? = null
) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val isSimulation by viewModel.isSimulationMode.collectAsState()
    val isScanning by viewModel.isBleScanning.collectAsState()
    val isBusy by viewModel.isBleBusy.collectAsState()
    val pending by viewModel.pendingCommands.collectAsState()
    val discoveredDevices by viewModel.discoveredBleDevices.collectAsState()
    val rawPacket by viewModel.rawPacket.collectAsState()
    val packetRate by viewModel.packetRateHz.collectAsState()
    val crcPercent by viewModel.crcValidPercent.collectAsState()
    val telemetryPacketCount by viewModel.telemetryPacketCount.collectAsState()
    val telemetryRxMessage by viewModel.telemetryRxMessage.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val logs by viewModel.terminalLogs.collectAsState()
    val otaState by viewModel.otaState.collectAsState()
    val selectedPlatform by viewModel.selectedPlatform.collectAsState()
    val savedDeviceMac by viewModel.savedDeviceMac.collectAsState()
    val bindingRecord by viewModel.bindingRecord.collectAsState()
    val firmwareIdentity by viewModel.firmwareIdentity.collectAsState()
    val firmwareVersionInfo by viewModel.firmwareVersionInfo.collectAsState()
    val sessionPhase by viewModel.sessionPhase.collectAsState()
    val context = LocalContext.current

    val linkQuality = evaluateLinkQuality(isConnected, packetRate, crcPercent)
    var filterOnlyCdi by remember { mutableStateOf(true) }
    var showManualMacDialog by remember { mutableStateOf(false) }
    var manualMacInput by remember { mutableStateOf(savedDeviceMac ?: "") }

    val currentSerial = firmwareIdentity.serial
    val identityReady = currentSerial.isNotBlank() &&
        currentSerial != "UNAVAILABLE" &&
        currentSerial != "IGT-ESP32-UNKNOWN"
    val canBind = isConnected && identityReady &&
        sessionPhase != id.ns200.cdir7.SessionPhase.SYNCING
    val isBound = bindingRecord != null && (bindingRecord?.serial == currentSerial || isSimulation)
    val boundDateStr = remember(bindingRecord?.boundAtEpochMs) {
        val ms = bindingRecord?.boundAtEpochMs ?: 0L
        if (ms > 0L) {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ms))
        } else {
            "Baru saja"
        }
    }

    var showBindDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showUnbindConfirmDialog by remember { mutableStateOf(false) }
    var vehicleNameInput by remember { mutableStateOf("") }

    // Deteksi Layanan Lokasi (GPS) HP - Hanya relevan untuk Android 11 ke bawah (SDK < 31)
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }
    val isGpsEnabled = remember(isScanning, isConnected) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            true // Android 12+ dengan flag neverForLocation resmi bebas GPS
        } else {
            locationManager?.let { LocationManagerCompat.isLocationEnabled(it) } ?: true
        }
    }

    val binFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null && bytes.isNotEmpty()) {
                    val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "APP.bin"
                    viewModel.startOtaUpload(bytes, fileName)
                } else {
                    Toast.makeText(context, "File kosong atau tidak terbaca", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var commandInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Dialog Input MAC Langsung (Koneksi Bebas GPS 100%)
    if (showManualMacDialog) {
        AlertDialog(
            onDismissRequest = { showManualMacDialog = false },
            shape = RoundedCornerShape(3.dp),
            containerColor = CardBackground,
            title = {
                Text(
                    text = "KONEKSI LANGSUNG VIA MAC (BEBAS GPS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MotecOrange,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Koneksi langsung ke MAC address tidak membutuhkan GPS/Layanan Lokasi pada semua versi Android.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    OutlinedTextField(
                        value = manualMacInput,
                        onValueChange = { manualMacInput = it.uppercase() },
                        placeholder = { Text("Contoh: AA:BB:CC:DD:EE:FF", fontSize = 11.sp, color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(3.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = MotecOrange,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SurfacePanel,
                            unfocusedContainerColor = SurfacePanel
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                MotecButton(
                    text = "HUBUNGKAN",
                    onClick = {
                        val cleaned = manualMacInput.trim()
                        if (cleaned.isNotBlank()) {
                            showManualMacDialog = false
                            if (onRequestPermissions != null) {
                                onRequestPermissions(false) { viewModel.connectDirectAddress(cleaned) }
                            } else {
                                viewModel.connectDirectAddress(cleaned)
                            }
                        } else {
                            Toast.makeText(context, "Masukkan MAC address yang valid", Toast.LENGTH_SHORT).show()
                        }
                    },
                    color = RacingLime
                )
            },
            dismissButton = {
                MotecButton(
                    text = "BATAL",
                    onClick = { showManualMacDialog = false },
                    color = TextSecondary
                )
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 740.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ==========================================
            // 1. BLE GATT STATUS & CONTROLS (MOTEC STYLE)
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, linkQuality.color.copy(alpha = 0.8f), RoundedCornerShape(3.dp))
                    .testTag("ble_status_card"),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Header Row: Status Bar & Quality Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(linkQuality.color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isConnected && isSimulation -> "MODE SIMULASI CDI R9"
                                    isConnected && packetRate <= 0 -> "BLE TERHUBUNG • SIAP (STANDBY)"
                                    isConnected -> "BLE TERHUBUNG • ${packetRate} Hz"
                                    isBusy -> "BLE: MENGHUBUNGKAN..."
                                    else -> "BLE OFFLINE / TERPUTUS"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = linkQuality.color,
                                maxLines = 1
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = linkQuality.color.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, linkQuality.color.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = if (isSimulation) "SIMULASI" else linkQuality.label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = linkQuality.color,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Action Buttons 50/50: MOTEC SQUARE BUTTONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MotecButton(
                            text = if (isSimulation) "SIMULASI: AKTIF" else "MODE SIMULASI",
                            onClick = { viewModel.toggleSimulation() },
                            icon = if (isSimulation) Icons.Default.PlayCircle else Icons.Default.PlayArrow,
                            color = if (isSimulation) MotecOrange else TextSecondary,
                            modifier = Modifier.weight(1f),
                            height = 34.dp
                        )

                        MotecButton(
                            text = when {
                                isConnected -> "PUTUS KONEKSI"
                                isBusy -> "BATALKAN"
                                else -> "HUBUNGKAN"
                            },
                            onClick = {
                                if (!isConnected) {
                                    val hasSaved = !savedDeviceMac.isNullOrBlank()
                                    val isScanRequired = !hasSaved
                                    val hasPerms = if (isScanRequired) viewModel.hasBlePermissions() else viewModel.hasConnectPermission()
                                    val isBtEnabled = viewModel.isBluetoothEnabled()

                                    if ((!hasPerms || !isBtEnabled) && onRequestPermissions != null) {
                                        onRequestPermissions(isScanRequired) { viewModel.toggleConnect() }
                                    } else {
                                        viewModel.toggleConnect()
                                    }
                                } else {
                                    viewModel.toggleConnect()
                                }
                            },
                            icon = if (isConnected) Icons.Default.BluetoothDisabled else Icons.Default.Bluetooth,
                            color = if (isConnected) RaceRedline else RacingLime,
                            modifier = Modifier.weight(1f),
                            height = 34.dp
                        )
                    }

                    // Compact GATT Specs Grid (2 columns, minimal whitespace)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfacePanel, RoundedCornerShape(2.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SRV: 7a8f1000... (128-bit)", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text("CMD: 7a8f1002... (Q: $pending)", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TEL: 7a8f1001... ($telemetryPacketCount pkts)", fontSize = 9.sp, color = ElectricCyan, fontFamily = FontFamily.Monospace)
                            Text("RSP: 7a8f1003... (ASCII)", fontSize = 9.sp, color = TechPurple, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RATE: ${packetRate} Hz • PHY 1M", fontSize = 9.sp, color = RacingLime, fontFamily = FontFamily.Monospace)
                            Text("CRC: %.1f%% (%s)".format(crcPercent, linkQuality.label), fontSize = 9.sp, color = linkQuality.color, fontFamily = FontFamily.Monospace)
                        }
                        if (telemetryRxMessage.isNotBlank()) {
                            Text("RX: $telemetryRxMessage", fontSize = 9.sp, color = TextPrimary, fontFamily = FontFamily.Monospace, maxLines = 1)
                        }
                    }
                }
            }

            // ==========================================
            // 2. IDENTITAS PERANGKAT & BINDING APLIKASI (PROTEKSI TULIS)
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isBound) RacingLime.copy(alpha = 0.8f) else if (isConnected) SensorAmber.copy(alpha = 0.8f) else BorderSubtle,
                        RoundedCornerShape(3.dp)
                    )
                    .testTag("device_binding_card"),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isBound) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (isBound) RacingLime else if (isConnected) SensorAmber else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "BINDING APLIKASI & IDENTITAS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBound) RacingLime else if (isConnected) SensorAmber else TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = (if (isBound) RacingLime else if (isConnected) SensorAmber else TextMuted).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (isBound) RacingLime else if (isConnected) SensorAmber else BorderSubtle)
                        ) {
                            Text(
                                text = when {
                                    isBound -> "TERIKAT (BOUND)"
                                    isConnected -> "BELUM TERIKAT (READ-ONLY)"
                                    else -> "OFFLINE"
                                },
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isBound) RacingLime else if (isConnected) SensorAmber else TextSecondary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Identity & Binding Specs Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfacePanel, RoundedCornerShape(2.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SERIAL CDI:", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(
                                text = if (isConnected || isSimulation) currentSerial else (bindingRecord?.serial ?: "TIDAK TERKONEKSI"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("NAMA KENDARAAN:", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(
                                text = bindingRecord?.vehicleName ?: "Belum Terikat",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bindingRecord != null) RacingLime else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("FIRMWARE / PLATFORM:", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "${firmwareVersionInfo.release} ${firmwareVersionInfo.semver} (${firmwareVersionInfo.platform})",
                                fontSize = 9.5.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("STATUS AKSES:", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(
                                text = if (isBound) "IZIN TULIS AKTIF (FULL ACCESS)" else "HANYA BACA (READ-ONLY)",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBound) RacingLime else SensorAmber,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (bindingRecord != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TANGGAL BINDING:", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = boundDateStr,
                                    fontSize = 9.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Action Buttons (Bind, Edit Name, Unbind)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!isBound) {
                            MotecButton(
                                text = "IKAT PERANGKAT (BIND)",
                                onClick = {
                                    vehicleNameInput = bindingRecord?.vehicleName ?: "NS200"
                                    showBindDialog = true
                                },
                                icon = Icons.Default.Link,
                                color = RacingLime,
                                height = 30.dp,
                                modifier = Modifier.weight(1f),
                                testTag = "bind_device_btn",
                                enabled = canBind || isSimulation
                            )
                        } else {
                            MotecButton(
                                text = "UBAH NAMA",
                                onClick = {
                                    vehicleNameInput = bindingRecord?.vehicleName ?: "NS200"
                                    showRenameDialog = true
                                },
                                icon = Icons.Default.Edit,
                                color = ElectricCyan,
                                height = 30.dp,
                                modifier = Modifier.weight(1f),
                                testTag = "edit_vehicle_name_btn"
                            )

                            MotecButton(
                                text = "LEPAS BINDING",
                                onClick = { showUnbindConfirmDialog = true },
                                icon = Icons.Default.LinkOff,
                                color = RaceRedline,
                                height = 30.dp,
                                modifier = Modifier.weight(1f),
                                testTag = "unbind_device_btn"
                            )
                        }
                    }

                    // Explanatory Note (sesuai dokumen section 9)
                    Text(
                        text = "Binding R9.2 adalah proteksi lokal pada aplikasi ini agar konfigurasi pengapian tidak tertukar antar sepeda motor. Tanpa binding yang cocok, akses dibatasi ke mode Read-Only yang aman.",
                        fontSize = 8.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 11.sp
                    )
                }
            }

            // Dialog: Ikat Perangkat (Bind)
            if (showBindDialog) {
                AlertDialog(
                    onDismissRequest = { showBindDialog = false },
                    shape = RoundedCornerShape(4.dp),
                    containerColor = CardBackground,
                    title = {
                        Text(
                            text = "IKAT PERANGKAT KE APLIKASI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RacingLime,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Serial: $currentSerial\nMasukkan nama/tipe sepeda motor untuk identifikasi profil lokal:",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            OutlinedTextField(
                                value = vehicleNameInput,
                                onValueChange = { vehicleNameInput = it },
                                placeholder = { Text("Contoh: Pulsar 200NS Harian", fontSize = 11.sp, color = TextMuted) },
                                singleLine = true,
                                shape = RoundedCornerShape(3.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = RacingLime,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = SurfacePanel,
                                    unfocusedContainerColor = SurfacePanel
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        MotecButton(
                            text = "IKAT (BIND)",
                            onClick = {
                                viewModel.confirmBinding(vehicleNameInput.ifBlank { "NS200" })
                                showBindDialog = false
                            },
                            color = RacingLime
                        )
                    },
                    dismissButton = {
                        MotecButton(
                            text = "BATAL",
                            onClick = { showBindDialog = false },
                            color = TextSecondary
                        )
                    }
                )
            }

            // Dialog: Ubah Nama Kendaraan
            if (showRenameDialog) {
                AlertDialog(
                    onDismissRequest = { showRenameDialog = false },
                    shape = RoundedCornerShape(4.dp),
                    containerColor = CardBackground,
                    title = {
                        Text(
                            text = "UBAH NAMA KENDARAAN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Perbarui nama kendaraan lokal untuk CDI [$currentSerial]:",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            OutlinedTextField(
                                value = vehicleNameInput,
                                onValueChange = { vehicleNameInput = it },
                                singleLine = true,
                                shape = RoundedCornerShape(3.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = ElectricCyan,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = SurfacePanel,
                                    unfocusedContainerColor = SurfacePanel
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        MotecButton(
                            text = "SIMPAN",
                            onClick = {
                                viewModel.updateBoundVehicleName(vehicleNameInput)
                                showRenameDialog = false
                            },
                            color = ElectricCyan
                        )
                    },
                    dismissButton = {
                        MotecButton(
                            text = "BATAL",
                            onClick = { showRenameDialog = false },
                            color = TextSecondary
                        )
                    }
                )
            }

            // Dialog: Konfirmasi Lepas Binding
            if (showUnbindConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showUnbindConfirmDialog = false },
                    shape = RoundedCornerShape(4.dp),
                    containerColor = CardBackground,
                    title = {
                        Text(
                            text = "LEPASKAN BINDING PERANGKAT?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RaceRedline,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    text = {
                        Text(
                            text = "Jika binding dilepas, aplikasi akan kembali ke mode Read-Only untuk perangkat [$currentSerial]. Anda tetap dapat melihat telemetri dan kode fault, tetapi fitur tulis kurva dan konfigurasi modul akan dikunci.",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    confirmButton = {
                        MotecButton(
                            text = "LEPASKAN BINDING",
                            onClick = {
                                viewModel.unbindCurrentDevice()
                                showUnbindConfirmDialog = false
                            },
                            color = RaceRedline
                        )
                    },
                    dismissButton = {
                        MotecButton(
                            text = "BATAL",
                            onClick = { showUnbindConfirmDialog = false },
                            color = TextSecondary
                        )
                    }
                )
            }

            // ==========================================
            // 3. HARDWARE BLE CDI SCANNER (FOKUS UTAMA)
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isScanning) MotecOrange else BorderSubtle, RoundedCornerShape(3.dp))
                    .testTag("ble_scanner_card"),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Scanner Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.BluetoothSearching,
                                contentDescription = null,
                                tint = if (isScanning) MotecOrange else ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "BLE HARDWARE SCANNER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isScanning) MotecOrange else TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isScanning) "Mencari frekuensi radio BLE..." else "${discoveredDevices.size} modul terdeteksi",
                                    fontSize = 9.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Scan / Stop Motec Button
                        MotecButton(
                            text = if (isScanning) "HENTIKAN" else "PINDAI BLE",
                            onClick = {
                                if (isScanning) {
                                    viewModel.stopBleScan()
                                } else {
                                    val hasPerms = viewModel.hasBlePermissions()
                                    val isBtEnabled = viewModel.isBluetoothEnabled()
                                    if ((!hasPerms || !isBtEnabled) && onRequestPermissions != null) {
                                        onRequestPermissions(true) { viewModel.startBleScan() }
                                    } else {
                                        viewModel.startBleScan()
                                    }
                                }
                            },
                            icon = if (isScanning) Icons.Default.Close else Icons.Default.Search,
                            color = if (isScanning) RaceRedline else MotecOrange,
                            height = 32.dp,
                            modifier = Modifier.testTag("start_scan_button")
                        )
                    }

                    // Scanning Progress Bar
                    if (isScanning) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = MotecOrange,
                            trackColor = SurfacePanel
                        )
                    }

                    // GPS Warning & Solusi Bebas GPS jika GPS Mati pada Android < 12 (Android 11 kebawah)
                    if (!isGpsEnabled && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SensorAmber.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                                .border(1.dp, SensorAmber.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.LocationOff, null, tint = SensorAmber, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "Android <12 butuh GPS aktif untuk scan BLE. Atau gunakan Hubungkan MAC (Bebas GPS).",
                                    fontSize = 8.sp,
                                    color = SensorAmber,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                MotecButton(
                                    text = "INPUT MAC",
                                    onClick = { showManualMacDialog = true },
                                    color = ElectricCyan,
                                    height = 22.dp,
                                    fontSize = 8.sp
                                )
                                MotecButton(
                                    text = "AKTIFKAN GPS",
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Buka Pengaturan untuk menyalakan Lokasi", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    color = SensorAmber,
                                    height = 22.dp,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }

                    // Filter Tabs & Direct MAC Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Filter: Semua
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (!filterOnlyCdi) MotecOrange.copy(alpha = 0.18f) else SurfacePanel)
                                    .border(1.dp, if (!filterOnlyCdi) MotecOrange else BorderSubtle, RoundedCornerShape(2.dp))
                                    .clickable { filterOnlyCdi = false }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "SEMUA (${discoveredDevices.size})",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!filterOnlyCdi) MotecOrange else TextSecondary
                                )
                            }

                            // Filter: Hanya CDI
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (filterOnlyCdi) RacingLime.copy(alpha = 0.18f) else SurfacePanel)
                                    .border(1.dp, if (filterOnlyCdi) RacingLime else BorderSubtle, RoundedCornerShape(2.dp))
                                    .clickable { filterOnlyCdi = true }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "CDI TARGET (${discoveredDevices.count { it.isCdiCandidate }})",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (filterOnlyCdi) RacingLime else TextSecondary
                                )
                            }
                        }

                        // Tombol Cepat Input MAC (100% Bebas GPS)
                        MotecButton(
                            text = "KONEK MAC",
                            onClick = { showManualMacDialog = true },
                            icon = Icons.Default.Dialpad,
                            color = ElectricCyan,
                            height = 24.dp,
                            fontSize = 8.5.sp
                        )
                    }

                    val displayedDevices = if (filterOnlyCdi) {
                        discoveredDevices.filter { it.isCdiCandidate }
                    } else {
                        discoveredDevices
                    }

                    // Devices List
                    if (displayedDevices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfacePanel, RoundedCornerShape(2.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = when {
                                        isScanning -> "🔍 Sedang memindai radio BLE..."
                                        filterOnlyCdi && discoveredDevices.isNotEmpty() -> "Tidak ada target CDI di antara ${discoveredDevices.size} perangkat sekitar."
                                        else -> "Belum ada perangkat. Tekan 'PINDAI BLE' atau gunakan 'KONEK MAC'."
                                    },
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (!isScanning) {
                                    Text("Pastikan kontak motor & CDI menyala (No PIN • Auto-reconnect)", fontSize = 8.5.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (item in displayedDevices) {
                                val devName = item.name.ifBlank { "BLE Device" }
                                val devAddr = item.address
                                val isTarget = item.isCdiCandidate

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isTarget) CardHover else SurfacePanel, RoundedCornerShape(2.dp))
                                        .border(1.dp, if (isTarget) RacingLime.copy(alpha = 0.8f) else BorderSubtle, RoundedCornerShape(2.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = devName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isTarget) RacingLime else TextPrimary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(2.dp),
                                                color = if (isTarget) RacingLime.copy(alpha = 0.2f) else CarbonDark,
                                                border = BorderStroke(0.5.dp, if (isTarget) RacingLime else BorderSubtle)
                                            ) {
                                                Text(
                                                    text = if (isTarget) "TARGET CDI" else "BLE LAIN",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isTarget) RacingLime else TextMuted,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "$devAddr • ${item.rssi} dBm",
                                            fontSize = 9.sp,
                                            color = TextSecondary,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    MotecButton(
                                        text = "KONEK",
                                        onClick = {
                                            val hasPerms = viewModel.hasConnectPermission()
                                            val isBtEnabled = viewModel.isBluetoothEnabled()
                                            if ((!hasPerms || !isBtEnabled) && onRequestPermissions != null) {
                                                onRequestPermissions(false) { viewModel.connectBleDevice(item.device) }
                                            } else {
                                                viewModel.connectBleDevice(item.device)
                                            }
                                        },
                                        color = if (isTarget) RacingLime else MotecOrange,
                                        enabled = !isConnected && !isBusy,
                                        height = 26.dp,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. BLE OTA FIRMWARE UPLOADER (COLLAPSIBLE)
            // ==========================================
            var otaCardExpanded by remember { mutableStateOf(otaState !is OtaState.Idle) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (otaState is OtaState.Transferring) ElectricCyan else BorderSubtle, RoundedCornerShape(3.dp))
                    .testTag("ble_ota_card"),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { otaCardExpanded = !otaCardExpanded }
                        ) {
                            Text(
                                text = "PENGUNGGAH FIRMWARE (OTA R9)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MotecOrange,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = if (otaCardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = when (otaState) {
                                is OtaState.Transferring -> ElectricCyan.copy(alpha = 0.18f)
                                is OtaState.Success -> RacingLime.copy(alpha = 0.18f)
                                is OtaState.Error -> RaceRedline.copy(alpha = 0.18f)
                                else -> SurfacePanel
                            },
                            border = BorderStroke(
                                1.dp,
                                when (otaState) {
                                    is OtaState.Transferring -> ElectricCyan
                                    is OtaState.Success -> RacingLime
                                    is OtaState.Error -> RaceRedline
                                    else -> BorderSubtle
                                }
                            )
                        ) {
                            Text(
                                text = when (otaState) {
                                    is OtaState.Idle -> "IDLE"
                                    is OtaState.Preparing -> "SIAP"
                                    is OtaState.Transferring -> "UPLOAD"
                                    is OtaState.Verifying -> "VERIFIKASI"
                                    is OtaState.Success -> "SUKSES"
                                    is OtaState.Error -> "ERROR"
                                },
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = when (otaState) {
                                    is OtaState.Transferring -> ElectricCyan
                                    is OtaState.Success -> RacingLime
                                    is OtaState.Error -> RaceRedline
                                    else -> TextSecondary
                                },
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (otaCardExpanded || otaState !is OtaState.Idle) {
                        val safetyErr = viewModel.checkOtaPreflightSafety()
                        val isSafetyOk = safetyErr == null

                        // Preflight Safety Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSafetyOk) SurfacePanel else RaceRedline.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                                .border(1.dp, if (isSafetyOk) BorderSubtle else RaceRedline.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "RPM=0: ${telemetry.rpm == 0} • KOIL=OFF: ${!telemetry.centerEnabled && !telemetry.sideEnabled} • HV<30V: ${telemetry.hvCenter < 30 && telemetry.hvSide < 30}",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSafetyOk) RacingLime else RaceRedline
                            )
                            Text(
                                text = if (selectedPlatform == id.ns200.cdir7.McuPlatform.STM32WB55) "STM32" else "ESP32",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }

                        // OTA Progress or State
                        when (val state = otaState) {
                            is OtaState.Transferring -> {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${state.bytesTransferred}/${state.totalBytes}B", fontSize = 9.sp, color = ElectricCyan, fontFamily = FontFamily.Monospace)
                                        Text("${(state.progressPercent * 100).toInt()}% (Chunk ${state.chunkIndex + 1}/${state.totalChunks})", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RacingLime, fontFamily = FontFamily.Monospace)
                                    }
                                    LinearProgressIndicator(
                                        progress = { state.progressPercent },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = ElectricCyan,
                                        trackColor = SurfacePanel
                                    )
                                    MotecButton(
                                        text = "BATALKAN OTA",
                                        onClick = viewModel::cancelOtaUpload,
                                        color = RaceRedline,
                                        modifier = Modifier.fillMaxWidth(),
                                        height = 26.dp
                                    )
                                }
                            }
                            is OtaState.Verifying -> {
                                Text("Memverifikasi CRC32 Flash MCU...", fontSize = 9.5.sp, color = ElectricCyan, fontFamily = FontFamily.Monospace)
                            }
                            is OtaState.Success -> {
                                Text("SUKSES: Firmware R9 aktif. ${state.message}", fontSize = 9.5.sp, color = RacingLime, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            is OtaState.Error -> {
                                Text("ERROR OTA: ${state.reason}", fontSize = 9.5.sp, color = RaceRedline, fontFamily = FontFamily.Monospace)
                            }
                            else -> Unit
                        }

                        // Action Buttons
                        if (otaState !is OtaState.Transferring && otaState !is OtaState.Preparing && otaState !is OtaState.Verifying) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MotecButton(
                                    text = "PILIH APP.BIN",
                                    onClick = { binFilePickerLauncher.launch("*/*") },
                                    icon = Icons.Default.UploadFile,
                                    color = MotecOrange,
                                    enabled = isSafetyOk,
                                    modifier = Modifier.weight(1f)
                                )
                                MotecButton(
                                    text = "STATUS OTA",
                                    onClick = { viewModel.sendRawCommand("GET,OTA") },
                                    icon = Icons.Default.Info,
                                    color = ElectricCyan,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. 20-BYTE RAW HEXADECIMAL PACKET STREAM
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(3.dp))
                    .testTag("hex_packet_inspector"),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(3.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "20-BYTE V3 STREAM (${if (rawPacket.getOrNull(3)?.toInt() == 1) "DIAG" else "CORE"})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$packetRate Hz",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RacingLime,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Hex Grid Matrix (4 rows x 5 columns)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfacePanel, RoundedCornerShape(2.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (row in 0..3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "%02X:".format(row * 5),
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                                for (col in 0..4) {
                                    val byteIndex = row * 5 + col
                                    val byteVal = if (byteIndex < rawPacket.size) rawPacket[byteIndex].toInt() and 0xFF else 0
                                    val color = when (byteIndex) {
                                        0, 1, 2 -> ElectricCyan        // Header 15 CD 03
                                        3 -> RacingLime                // Frame kind
                                        4, 5 -> SensorAmber            // Sequence
                                        6, 7 -> MotecOrange            // RPM
                                        8, 9 -> TechPurple             // TPS
                                        10, 11 -> ElectricCyan         // Advance
                                        12, 13 -> SensorAmber          // Battery
                                        14, 15, 16, 17 -> RacingLime   // HV Caps
                                        18, 19 -> RacingLime           // CRC16
                                        else -> TextSecondary
                                    }
                                    Text(
                                        text = "%02X".format(byteVal),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // Field Decoder Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0-2: Magic 15CD03", fontSize = 8.sp, color = ElectricCyan, fontFamily = FontFamily.Monospace)
                        Text("3: Type", fontSize = 8.sp, color = RacingLime, fontFamily = FontFamily.Monospace)
                        Text("4-5: Seq", fontSize = 8.sp, color = SensorAmber, fontFamily = FontFamily.Monospace)
                        Text("6-17: Vars", fontSize = 8.sp, color = MotecOrange, fontFamily = FontFamily.Monospace)
                        Text("18-19: CRC16", fontSize = 8.sp, color = RacingLime, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // ==========================================
            // 5. DIAGNOSTIC TERMINAL LOG & COMMAND SENDER
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(3.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(3.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DIAGNOSTIC TERMINAL LOG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SensorAmber,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${logs.size} baris",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Console Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(CarbonDark, RoundedCornerShape(2.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                            .padding(6.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (logs.isEmpty()) {
                            Text("> Terminal siap • Kirim perintah ASCII di bawah", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        } else {
                            logs.takeLast(20).forEach { line ->
                                Text(
                                    text = "> $line",
                                    fontSize = 9.sp,
                                    color = when {
                                        line.startsWith("TX") -> MotecOrange
                                        line.startsWith("RX") -> ElectricCyan
                                        line.contains("ERR", true) -> RaceRedline
                                        else -> TextSecondary
                                    },
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Command Input Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = commandInput,
                            onValueChange = { commandInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            placeholder = { Text("Command (e.g. PING, LOAD,0)", fontSize = 10.sp, color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(3.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = MotecOrange,
                                unfocusedBorderColor = BorderSubtle,
                                focusedContainerColor = SurfacePanel,
                                unfocusedContainerColor = SurfacePanel
                            )
                        )

                        MotecButton(
                            text = "KIRIM",
                            onClick = {
                                if (commandInput.isNotBlank()) {
                                    viewModel.sendRawCommand(commandInput.trim())
                                    commandInput = ""
                                }
                            },
                            color = MotecOrange,
                            height = 38.dp
                        )
                    }

                    // Quick Command Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("PING", "LOAD,0", "LOAD,1", "SAVE,0", "GET,OTA").forEach { cmd ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(SurfacePanel)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(2.dp))
                                    .clickable { viewModel.sendRawCommand(cmd) }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(cmd, fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
