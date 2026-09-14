package id.ns200.cdir7.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ns200.cdir7.CdiProtocol
import id.ns200.cdir7.CdiViewModel
import id.ns200.cdir7.OtaState
import id.ns200.cdir7.ui.theme.*

enum class LinkQuality(val label: String, val color: Color) {
    STABIL("STABIL", Color(0xFF00E676)),
    CUKUP("CUKUP", Color(0xFFFFB300)),
    BURUK("BURUK", Color(0xFFFF3D00)),
    TERPUTUS("TERPUTUS", Color(0xFF757575))
}

fun evaluateLinkQuality(
    connected: Boolean,
    rateHz: Int,
    crcPercent: Float
): LinkQuality {
    if (!connected || rateHz <= 0) {
        return LinkQuality.TERPUTUS
    }

    return when {
        crcPercent < 95f || rateHz < 12 ->
            LinkQuality.BURUK

        rateHz in 18..22 && crcPercent >= 99f ->
            LinkQuality.STABIL

        else ->
            LinkQuality.CUKUP
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BleHexScreen(
    viewModel: CdiViewModel,
    onRequestPermissions: (() -> Unit)? = null
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
    val context = LocalContext.current

    val linkQuality = evaluateLinkQuality(isConnected, packetRate, crcPercent)

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
                    Toast.makeText(context, "File kosong atau tidak dapat dibaca", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var commandInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // BLE GATT STATUS CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    linkQuality.color,
                    RoundedCornerShape(14.dp)
                )
                .testTag("ble_status_card"),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Row: Status Indicator & Link Quality Badge
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
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(linkQuality.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isConnected) {
                                    if (isSimulation) "MODE SIMULASI CDI R9"
                                    else if (telemetryPacketCount < 2L) "BLE CONNECTED • STANDBY"
                                    else "BLE CONNECTED • ${packetRate} Hz"
                                } else "BLE OFFLINE / TERPUTUS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = linkQuality.color,
                                maxLines = 1
                            )
                            Text(
                                text = connectionStatus,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = linkQuality.color.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, linkQuality.color.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = if (isSimulation) "SIMULASI" else linkQuality.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = linkQuality.color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Dedicated Action Buttons Row (Full width, balanced 50/50 weights - never squeezed!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Demo / Simulation mode toggle button
                    OutlinedButton(
                        onClick = { viewModel.toggleSimulation() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isSimulation) MotecOrange else TextSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isSimulation) Icons.Default.PlayCircle else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isSimulation) MotecOrange else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSimulation) "SIMULASI: ON" else "SIMULASI: OFF",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Connect / Disconnect button
                    Button(
                        onClick = {
                            if (!isConnected && !viewModel.hasBlePermissions() && onRequestPermissions != null) {
                                onRequestPermissions()
                            } else {
                                viewModel.toggleConnect()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) RaceRedline else RacingLime
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.BluetoothDisabled else Icons.Default.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = CarbonDark
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isConnected -> "PUTUS BLE"
                                isBusy -> "BATALKAN"
                                else -> "HUBUNGKAN"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonDark,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // GATT Specs
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfacePanel, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GattSpecRow("SERVICE UUID", "7a8f1000-6c9d-4e40-a45f-0b4b4e533230")
                    GattSpecRow("TELEMETRY CHAR", "7a8f1001-... (${telemetryPacketCount} frame • ${packetRate} Hz • v3 20B)")
                    GattSpecRow("COMMAND CHAR", "7a8f1002-... (Antrean ACK: $pending)")
                    GattSpecRow("RESPONSE CHAR", "7a8f1003-... (Notify ASCII Stream)")
                    GattSpecRow(
                        "CRC16 INTEGRITY",
                        if (telemetryPacketCount == 0L) "BELUM ADA FRAME"
                        else "%.1f%% VALID (%s)".format(crcPercent, linkQuality.label)
                    )
                    GattSpecRow("TELEMETRY RX", telemetryRxMessage)
                }
            }
        }

        // BLE OTA FIRMWARE UPLOADER (APP.BIN - R9) CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (otaState is OtaState.Transferring) ElectricCyan else BorderSubtle, RoundedCornerShape(14.dp))
                .testTag("ble_ota_card"),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PENGUNGGAH FIRMWARE BLE (OTA R9)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MotecOrange,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (selectedPlatform == id.ns200.cdir7.McuPlatform.STM32WB55) {
                                "Target: STM32 APP.bin"
                            } else {
                                "Target: ESP32 application image sesuai partition table"
                            },
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (otaState) {
                                    is OtaState.Transferring -> ElectricCyan.copy(alpha = 0.2f)
                                    is OtaState.Success -> RacingLime.copy(alpha = 0.2f)
                                    is OtaState.Error -> RaceRedline.copy(alpha = 0.2f)
                                    else -> SurfacePanel
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (otaState) {
                                is OtaState.Idle -> "IDLE"
                                is OtaState.Preparing -> "MENYIAPKAN"
                                is OtaState.Transferring -> "MENGUNGGAH"
                                is OtaState.Verifying -> "VERIFIKASI"
                                is OtaState.Success -> "SUKSES"
                                is OtaState.Error -> "ERROR"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = when (otaState) {
                                is OtaState.Transferring -> ElectricCyan
                                is OtaState.Success -> RacingLime
                                is OtaState.Error -> RaceRedline
                                else -> TextSecondary
                            }
                        )
                    }
                }

                // Safety Preflight Status
                val safetyErr = viewModel.checkOtaPreflightSafety()
                val isSafetyOk = safetyErr == null
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSafetyOk) SurfacePanel else RaceRedline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, if (isSafetyOk) BorderSubtle else RaceRedline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "SYARAT KESELAMATAN SEBELUM OTA:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSafetyOk) RacingLime else SensorAmber,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• RPM = 0 (${if (telemetry.rpm == 0) "OK: Mesin Mati" else "BAHAYA: ${telemetry.rpm} RPM"})\n" +
                               "• Output OFF (${if (!telemetry.centerEnabled && !telemetry.sideEnabled) "OK: Koil Nonaktif" else "PERINGATAN: Koil Aktif"})\n" +
                               "• HV < 30V (${if (telemetry.hvCenter < 30 && telemetry.hvSide < 30) "OK: Aman (${telemetry.hvCenter}V/${telemetry.hvSide}V)" else "BAHAYA: ${telemetry.hvCenter}V/${telemetry.hvSide}V"})",
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!isSafetyOk) {
                        Text(
                            text = "Update hanya dapat dimulai saat RPM=0, output OFF, dan kedua HV <30 V.",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = RaceRedline,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // OTA Transfer Status & Progress Bar
                when (val state = otaState) {
                    is OtaState.Transferring -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Mengirim: ${state.bytesTransferred} / ${state.totalBytes} Byte",
                                    fontSize = 10.sp,
                                    color = ElectricCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${(state.progressPercent * 100).toInt()}% (Chunk ${state.chunkIndex + 1}/${state.totalChunks})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RacingLime,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            LinearProgressIndicator(
                                progress = { state.progressPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = ElectricCyan,
                                trackColor = SurfacePanel
                            )
                            Button(
                                onClick = viewModel::cancelOtaUpload,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = RaceRedline),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text("BATALKAN PROSES OTA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                    is OtaState.Verifying -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ElectricCyan)
                            Text("Memverifikasi CRC32 Flash MCU...", fontSize = 11.sp, color = ElectricCyan, fontFamily = FontFamily.Monospace)
                        }
                    }
                    is OtaState.Success -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RacingLime.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, RacingLime, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = RacingLime, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("SUKSES! Firmware R9 berhasil diunggah. ${state.message}", fontSize = 10.sp, color = RacingLime, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                    is OtaState.Error -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RaceRedline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, RaceRedline, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = RaceRedline, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("GAGAL OTA: ${state.reason}", fontSize = 10.sp, color = RaceRedline, fontFamily = FontFamily.Monospace)
                        }
                    }
                    else -> Unit
                }

                // File Selection & Upload Buttons
                if (otaState !is OtaState.Transferring && otaState !is OtaState.Preparing && otaState !is OtaState.Verifying) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            enabled = isSafetyOk,
                            onClick = { binFilePickerLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MotecOrange),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, null, tint = CarbonDark, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("PILIH APP.BIN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarbonDark, fontFamily = FontFamily.Monospace)
                        }

                        OutlinedButton(
                            onClick = { viewModel.sendRawCommand("GET,OTA") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan)
                        ) {
                            Icon(Icons.Default.Info, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("STATUS OTA", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // HARDWARE BLE SCANNER & PAIRING CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .testTag("ble_scanner_card"),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BLE HARDWARE CDI SCANNER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MotecOrange,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Target: CDI Universal R9 / STM32WB55 atau ESP32",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = {
                            if (isScanning) {
                                viewModel.stopBleScan()
                            } else {
                                if (!viewModel.hasBlePermissions() && onRequestPermissions != null) {
                                    onRequestPermissions()
                                } else {
                                    viewModel.startBleScan()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) RaceRedline else MotecOrange
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isScanning) "STOP SCAN" else "SCAN BLE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonDark,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (discoveredDevices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfacePanel, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isScanning) "Sedang mencari perangkat CDI sekitar..." else "Tekan 'SCAN BLE' untuk mencari modul CDI hardware nyata.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (item in discoveredDevices) {
                            val devName = item.name.ifBlank { "BLE Device" }
                            val devAddr = item.address
                            val isTarget = item.isCdiCandidate

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isTarget) CardHover else SurfacePanel, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isTarget) RacingLime else BorderSubtle, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = devName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTarget) RacingLime else TextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "$devAddr  (RSSI: ${item.rssi} dBm)",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (!viewModel.hasBlePermissions() && onRequestPermissions != null) {
                                            onRequestPermissions()
                                        } else {
                                            viewModel.connectBleDevice(item.device)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isTarget) RacingLime else MotecOrange
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "KONEK",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CarbonDark,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Tanpa PIN",
                        tint = RacingLime,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tanpa PIN/bonding • PHY 1M • Auto-reconnect eksponensial 1–30s",
                        fontSize = 10.sp,
                        color = RacingLime,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // REAL-TIME 20-BYTE RAW HEXADECIMAL PACKET INSPECTOR
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .testTag("hex_packet_inspector"),
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
                        text = "20-BYTE V3 ${if (rawPacket.getOrNull(3)?.toInt() == 1) "DIAGNOSTIC" else "CORE"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "RATE: $packetRate Hz",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RacingLime,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hex Grid Matrix (20 bytes shown in 4 rows of 5 bytes)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfacePanel, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (row in 0..3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "%02X:".format(row * 5),
                                fontSize = 11.sp,
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
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Field Decoder Legend
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LegendChip("0-2: Magic (15 CD 03)", ElectricCyan)
                    LegendChip("3: Type (0:CORE / 1:DIAG)", RacingLime)
                    LegendChip("4-5: Seq ${telemetry.sequence}", MotecOrange)
                    LegendChip(
                        if (rawPacket.getOrNull(3)?.toInt() == 1) "6-17 DIAG: Suhu/Flags/Fault/TDC"
                        else "6-17 CORE: RPM/TPS/ADV/BAT/HV",
                        TechPurple
                    )
                    LegendChip("18-19: CRC16 OK", RacingLime)
                }
            }
        }

        // TERMINAL CONSOLE LOG & COMMAND SENDER
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "DIAGNOSTIC TERMINAL LOG",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensorAmber,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Console Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(CarbonDark, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    logs.takeLast(20).forEach { line ->
                        Text(
                            text = "> $line",
                            fontSize = 10.sp,
                            color = if (line.startsWith("TX")) MotecOrange else if (line.startsWith("RX")) ElectricCyan else TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Command Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        placeholder = { Text("Ketik command (e.g. PING, LOAD,0)", fontSize = 11.sp, color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = MotecOrange,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SurfacePanel,
                            unfocusedContainerColor = SurfacePanel
                        )
                    )

                    Button(
                        onClick = {
                            viewModel.sendRawCommand(commandInput)
                            commandInput = ""
                        },
                        modifier = Modifier.height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MotecOrange)
                    ) {
                        Text("KIRIM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CarbonDark, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Command Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("PING", "LOAD,0", "LOAD,1", "SAVE,0").forEach { cmd ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfacePanel)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                                .clickable { viewModel.sendRawCommand(cmd) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(cmd, fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun GattSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.36f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            fontSize = 10.sp,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.64f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun LegendChip(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(SurfacePanel, RoundedCornerShape(6.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            fontSize = 9.5.sp,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
