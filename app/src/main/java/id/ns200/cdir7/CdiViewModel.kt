package id.ns200.cdir7

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ScreenTab(val title: String, val badge: String) {
    TACHO("Dashboard", "LIVE"),
    MAPS("Maps", "KURVA"),
    SETUP("Setup", "KOMISI"),
    SUARA("Suara", "AUDIO"),
    WIRING("Buku", "MANUAL"),
    BLE("Perangkat", "DEVICE")
}

data class MapSlotData(
    val slot: Int,
    val name: String,
    val description: String,
    val revLimit: Int,
    val peakAdvance: Float,
    val curvePoints: List<Pair<Int, Float>> // (RPM, Advance Deg)
)

data class CustomAdvancePoint(
    val rpm: Int,
    val advanceDeg: Float
)

class CdiViewModel(application: Application) : AndroidViewModel(application), BleCdiClient.Listener {

    private val context: Context get() = getApplication<Application>().applicationContext
    val bleClient = BleCdiClient(context, this)
    val engineSound = EngineSound(context)

    // Active Screen
    private val _currentTab = MutableStateFlow(ScreenTab.TACHO)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Connection & Simulation
    private val _connectionStatus = MutableStateFlow("BLE Disconnected • Scan atau Hubungkan CDI")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private val _demoEngineRunning = MutableStateFlow(false)
    val demoEngineRunning: StateFlow<Boolean> = _demoEngineRunning.asStateFlow()

    fun simulateStartEngine() {
        _demoEngineRunning.value = true
        _isRevving.value = false
        _demoThrottleSlider.value = 0f
        simTps = 0.02f
        simRpm = 1420f
        appendLog("Demo: Starter ditekan -> Mesin hidup idle ~1.420 RPM")
        Toast.makeText(context, "Starter ON: Mesin hidup stasioner ~1.420 RPM", Toast.LENGTH_SHORT).show()
    }

    fun simulateStopEngine() {
        _demoEngineRunning.value = false
        _isRevving.value = false
        _demoThrottleSlider.value = 0f
        simRpm = 0f
        simTps = 0f
        engineSound.stop()
        val currentT = _telemetry.value
        _telemetry.value = currentT.copy(
            rpm = 0,
            tps = 0,
            hvCenter = 0,
            hvSide = 0,
            outputFlags = 0,
            limiter = 0
        )
        appendLog("Demo: Kunci kontak OFF / Mesin dimatikan -> RPM 0, Kapasitor HV discharge aman ke 0V")
        Toast.makeText(context, "Engine OFF: Mesin mati (0 RPM), HV 0V", Toast.LENGTH_SHORT).show()
    }

    fun toggleEngineStartStop() {
        if (_demoEngineRunning.value) {
            simulateStopEngine()
        } else {
            simulateStartEngine()
        }
    }

    val discoveredBleDevices: StateFlow<List<DiscoveredBleDevice>> = bleClient.discoveredDevices
    val isBleScanning: StateFlow<Boolean> = bleClient.isScanning
    val isBleBusy: StateFlow<Boolean> = bleClient.isBusy
    val pendingCommands: StateFlow<Int> = bleClient.pendingCommands

    // Telemetry State - Default Realistic Cold Standby for Real Hardware Integration
    private val _telemetry = MutableStateFlow(
        Telemetry(
            sequence = 0,
            rpm = 0,
            tps = 0,
            advanceCdeg = 0,
            batteryCv = 0,
            hvCenter = 0,
            hvSide = 0,
            tempCdeg = Short.MIN_VALUE.toInt(),
            slot = 0,
            limiter = 0,
            flags = 0,
            faults = 0,
            setupStage = 0,     // BARU
            outputFlags = 0,
            triggerCdeg = 6000, // default aman/provisional dari firmware; kalibrasikan pada motor
            pickupQuality = 0,
            firstStartSeconds = 0
        )
    )
    val telemetry: StateFlow<Telemetry> = _telemetry.asStateFlow()

    // Raw Hex Packet Stream
    private val _rawPacket = MutableStateFlow(ByteArray(CdiProtocol.TELEMETRY_SIZE))
    val rawPacket: StateFlow<ByteArray> = _rawPacket.asStateFlow()

    private val _packetRateHz = MutableStateFlow(0)
    val packetRateHz: StateFlow<Int> = _packetRateHz.asStateFlow()

    private val _crcValidPercent = MutableStateFlow(0f)
    val crcValidPercent: StateFlow<Float> = _crcValidPercent.asStateFlow()

    private val _telemetryPacketCount = MutableStateFlow(0L)
    val telemetryPacketCount: StateFlow<Long> = _telemetryPacketCount.asStateFlow()

    private val _telemetryRxMessage = MutableStateFlow("OFFLINE • belum berlangganan Telemetry 1001")
    val telemetryRxMessage: StateFlow<String> = _telemetryRxMessage.asStateFlow()

    private data class RxSample(
        val timestampMs: Long,
        val valid: Boolean
    )
    private val rxSamples = ArrayDeque<RxSample>()
    private val RX_WINDOW_MS = 2_000L
    private var telemetryWatchdogJob: Job? = null
    private var demoOemPulseJob: Job? = null

    // Setup StateFlows (Synchronized from GET,SETUP)
    private val _pickupEdge = MutableStateFlow("FALLING")
    val pickupEdge: StateFlow<String> = _pickupEdge.asStateFlow()

    private val _pulserPpr = MutableStateFlow(1)
    val pulserPpr: StateFlow<Int> = _pulserPpr.asStateFlow()

    private val _gateDurationUs = MutableStateFlow(80)
    val gateDurationUs: StateFlow<Int> = _gateDurationUs.asStateFlow()

    private val _tpsClosedAdc = MutableStateFlow(0)
    val tpsClosedAdc: StateFlow<Int> = _tpsClosedAdc.asStateFlow()

    private val _tpsOpenAdc = MutableStateFlow(0)
    val tpsOpenAdc: StateFlow<Int> = _tpsOpenAdc.asStateFlow()

    private val _firstStartHv = MutableStateFlow(220)
    val firstStartHv: StateFlow<Int> = _firstStartHv.asStateFlow()

    private val _fanMode = MutableStateFlow("AUTO")
    val fanMode: StateFlow<String> = _fanMode.asStateFlow()

    private val _sideOffsetCdeg = MutableStateFlow(0)
    val sideOffsetCdeg: StateFlow<Int> = _sideOffsetCdeg.asStateFlow()

    private val _setupCommandPending = MutableStateFlow(false)
    val setupCommandPending: StateFlow<Boolean> = _setupCommandPending.asStateFlow()

    // Tahap mentah yang benar-benar tersimpan di firmware: 0..4.
    private val _firmwareSetupStage = MutableStateFlow(0)
    val firmwareSetupStage: StateFlow<Int> = _firmwareSetupStage.asStateFlow()

    // Halaman wizard aplikasi: 0..5. Terpisah dari state permanen firmware.
    private val _quickSetupPage = MutableStateFlow(SetupStage.BARU.code)
    val quickSetupPage: StateFlow<Int> = _quickSetupPage.asStateFlow()

    private val _quickSetupUnlockedStage = MutableStateFlow(SetupStage.BARU.code)
    val quickSetupUnlockedStage: StateFlow<Int> = _quickSetupUnlockedStage.asStateFlow()

    private val _quickSetupPreflightBusy = MutableStateFlow(false)
    val quickSetupPreflightBusy: StateFlow<Boolean> = _quickSetupPreflightBusy.asStateFlow()

    private val _quickSetupMessage = MutableStateFlow(
        "Tekan PERIKSA & LANJUT. Aplikasi akan memeriksa PING, STATUS, dan SETUP dari MCU."
    )
    val quickSetupMessage: StateFlow<String> = _quickSetupMessage.asStateFlow()

    private val _pickupDiagnosticMessage = MutableStateFlow<String?>(null)
    val pickupDiagnosticMessage: StateFlow<String?> = _pickupDiagnosticMessage.asStateFlow()

    private var preflightPingOk = false
    private var preflightStatusOk = false
    private var preflightSetupOk = false
    private var preflightTimeoutJob: Job? = null
    private var lastTelemetryPacketAtMs = 0L
    private var setupSyncedThisConnection = false
    private var syncPongSeen = false
    private var syncVersionSeen = false
    private var syncIdentitySeen = false
    private var syncCapsSeen = false
    private var syncStatusSeen = false
    private var syncSetupSeen = false

    private var pendingTimeoutJob: Job? = null

    private fun markSetupCommandPending() {
        _setupCommandPending.value = true
        pendingTimeoutJob?.cancel()
        pendingTimeoutJob = viewModelScope.launch {
            delay(5000)
            if (_setupCommandPending.value) {
                _setupCommandPending.value = false
                appendLog("Timeout menunggu respons MCU")
            }
        }
    }

    private fun clearSetupCommandPending() {
        _setupCommandPending.value = false
        pendingTimeoutJob?.cancel()
    }

    private fun resetBleStatistics() {
        rxSamples.clear()
        _packetRateHz.value = 0
        _crcValidPercent.value = 0f
        _telemetryPacketCount.value = 0L
        lastTelemetryPacketAtMs = SystemClock.elapsedRealtime()
        telemetryWatchdogJob?.cancel()
        _telemetryRxMessage.value = if (_isConnected.value) {
            "MENUNGGU • notifikasi Telemetry 1001 belum diterima"
        } else {
            "OFFLINE • belum berlangganan Telemetry 1001"
        }
    }

    // Firmware R8 Modes & Features
    private val _firmwareMode = MutableStateFlow(FirmwareRunMode.MANUAL)
    val firmwareMode: StateFlow<FirmwareRunMode> = _firmwareMode.asStateFlow()

    private val _isOemLearning = MutableStateFlow(false)
    val isOemLearning: StateFlow<Boolean> = _isOemLearning.asStateFlow()

    private val _oemCenterPulses = MutableStateFlow(0)
    val oemCenterPulses: StateFlow<Int> = _oemCenterPulses.asStateFlow()

    private val _oemLearnCoverage = MutableStateFlow(0)
    val oemLearnCoverage: StateFlow<Int> = _oemLearnCoverage.asStateFlow()

    private val _oemRejectedPulses = MutableStateFlow(0)
    val oemRejectedPulses: StateFlow<Int> = _oemRejectedPulses.asStateFlow()

    private val _oemSideSamples = MutableStateFlow(0)
    val oemSideSamples: StateFlow<Int> = _oemSideSamples.asStateFlow()

    private val _isOemUnpluggedConfirmed = MutableStateFlow(false)
    val isOemUnpluggedConfirmed: StateFlow<Boolean> = _isOemUnpluggedConfirmed.asStateFlow()

    private val _targetHvVoltage = MutableStateFlow(CdiProtocol.VOLTAGE_NORMAL)
    val targetHvVoltage: StateFlow<Int> = _targetHvVoltage.asStateFlow()

    private val _isProVoltageConfigured = MutableStateFlow(false)
    val isProVoltageConfigured: StateFlow<Boolean> = _isProVoltageConfigured.asStateFlow()

    private val _mcuCapabilities = MutableStateFlow<Set<String>>(emptySet())
    val mcuCapabilities: StateFlow<Set<String>> = _mcuCapabilities.asStateFlow()

    private val _firmwareCapabilities = MutableStateFlow(FirmwareCapabilities.legacyR8())
    val firmwareCapabilities: StateFlow<FirmwareCapabilities> = _firmwareCapabilities.asStateFlow()

    private val _fanOnCdeg = MutableStateFlow(9000)
    val fanOnCdeg: StateFlow<Int> = _fanOnCdeg.asStateFlow()
    private val _fanOffCdeg = MutableStateFlow(8500)
    val fanOffCdeg: StateFlow<Int> = _fanOffCdeg.asStateFlow()

    private val _engineProfile = MutableStateFlow(EngineProfile.universal())
    val engineProfile: StateFlow<EngineProfile> = _engineProfile.asStateFlow()

    private val _dynoActive = MutableStateFlow(false)
    val dynoActive: StateFlow<Boolean> = _dynoActive.asStateFlow()
    private val _dynoTrimDeg = MutableStateFlow(0f)
    val dynoTrimDeg: StateFlow<Float> = _dynoTrimDeg.asStateFlow()

    val otaState: StateFlow<OtaState> = bleClient.otaState
    val connectedDeviceName: StateFlow<String?> = bleClient.connectedDeviceName
    val savedDeviceMac: StateFlow<String?> = bleClient.savedDeviceMac
    val savedDeviceName: StateFlow<String?> = bleClient.savedDeviceName
    val powerSaveMode: StateFlow<Boolean> = bleClient.powerSaveMode
    val autoConnectOnStart: StateFlow<Boolean> = bleClient.autoConnectOnStart

    private val cdiPrefs = context.getSharedPreferences("cdi_r8_prefs", Context.MODE_PRIVATE)

    private val _selectedPlatform = MutableStateFlow(
        McuPlatform.fromId(cdiPrefs.getString("mcu_platform", McuPlatform.ESP32_WROOM.id))
    )
    val selectedPlatform: StateFlow<McuPlatform> = _selectedPlatform.asStateFlow()

    fun setMcuPlatform(platform: McuPlatform) {
        _selectedPlatform.value = platform
        cdiPrefs.edit().putString("mcu_platform", platform.id).apply()
        appendLog("Platform Hardware aktif dialihkan ke: ${platform.displayName} (${platform.architecture})")
    }

    // ==========================================
    // FIRMWARE R9 MODUL HARDWARE & STATUS SISTEM
    // ==========================================
    private val bindingPrefs = context.getSharedPreferences("ignitra_binding_prefs", Context.MODE_PRIVATE)

    private fun getOrCreateAppInstanceId(): String {
        val existing = bindingPrefs.getString("app_instance_id", null)
        if (!existing.isNullOrBlank()) return existing
        val newId = UUID.randomUUID().toString()
        bindingPrefs.edit().putString("app_instance_id", newId).apply()
        return newId
    }

    private fun bindingKey(serial: String, field: String): String =
        "binding_${serial.replace(Regex("[^A-Za-z0-9_-]"), "_")}_$field"

    private fun loadBindingForSerial(serial: String): BindingRecord? {
        if (serial.isBlank() || serial == "UNAVAILABLE" || serial == "IGT-ESP32-UNKNOWN") return null
        val epoch = bindingPrefs.getLong(bindingKey(serial, "bound_at"), 0L)
        if (epoch <= 0L) {
            // Migrasi satu record lama tanpa menghapus binding perangkat lain.
            val legacySerial = bindingPrefs.getString("bound_serial", null)
            if (legacySerial != serial) return null
            return BindingRecord(
                serial = serial,
                appInstanceId = getOrCreateAppInstanceId(),
                boundAtEpochMs = bindingPrefs.getLong("bound_at", 0L),
                firmwareRelease = bindingPrefs.getString("bound_fw", "R9") ?: "R9",
                vehicleName = bindingPrefs.getString("bound_vehicle", null)
            )
        }
        return BindingRecord(
            serial = serial,
            appInstanceId = getOrCreateAppInstanceId(),
            boundAtEpochMs = epoch,
            firmwareRelease = bindingPrefs.getString(bindingKey(serial, "bound_fw"), "R9") ?: "R9",
            vehicleName = bindingPrefs.getString(bindingKey(serial, "vehicle"), null)
        )
    }

    private val _sessionPhase = MutableStateFlow(SessionPhase.DISCONNECTED)
    val sessionPhase: StateFlow<SessionPhase> = _sessionPhase.asStateFlow()

    private val _bindingRecord = MutableStateFlow<BindingRecord?>(null)
    val bindingRecord: StateFlow<BindingRecord?> = _bindingRecord.asStateFlow()

    fun isSerialBound(serial: String): Boolean = _bindingRecord.value?.serial == serial

    fun confirmBinding(vehicleName: String? = null) {
        if (!(syncPongSeen && syncVersionSeen && syncIdentitySeen &&
                syncCapsSeen && syncStatusSeen && syncSetupSeen)) {
            _sessionPhase.value = SessionPhase.SYNCING
            return
        }
        val currentSerial = _firmwareIdentity.value.serial
        if (!_isConnected.value || _sessionPhase.value == SessionPhase.SYNCING) {
            Toast.makeText(context, "Tunggu koneksi dan sinkronisasi IDENTITY selesai.", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentSerial == "UNAVAILABLE" || currentSerial == "IGT-ESP32-UNKNOWN" || currentSerial.isBlank()) {
            Toast.makeText(context, "Serial perangkat nyata belum diterima; binding diblokir.", Toast.LENGTH_SHORT).show()
            return
        }
        val appInstanceId = getOrCreateAppInstanceId()
        val record = BindingRecord(
            serial = currentSerial,
            appInstanceId = appInstanceId,
            boundAtEpochMs = System.currentTimeMillis(),
            firmwareRelease = _firmwareVersionInfo.value.release,
            vehicleName = vehicleName ?: "NS200"
        )
        bindingPrefs.edit()
            .putLong(bindingKey(record.serial, "bound_at"), record.boundAtEpochMs)
            .putString(bindingKey(record.serial, "bound_fw"), record.firmwareRelease)
            .putString(bindingKey(record.serial, "vehicle"), record.vehicleName)
            .apply()
        _bindingRecord.value = record
        if (_sessionPhase.value == SessionPhase.NEEDS_BINDING || _sessionPhase.value == SessionPhase.READY_READ_ONLY) {
            _sessionPhase.value = SessionPhase.READY_FULL
        }
        appendLog("BINDING: Serial [${record.serial}] berhasil di-binding ke aplikasi ini.")
        Toast.makeText(context, "Perangkat berhasil di-binding. Izin tulis aktif!", Toast.LENGTH_SHORT).show()
    }

    fun updateBoundVehicleName(newName: String) {
        val current = _bindingRecord.value ?: return
        val cleanName = newName.trim().ifBlank { "NS200" }
        val updated = current.copy(vehicleName = cleanName)
        bindingPrefs.edit()
            .putString(bindingKey(current.serial, "vehicle"), updated.vehicleName)
            .apply()
        _bindingRecord.value = updated
        appendLog("BINDING: Nama kendaraan diubah menjadi [${updated.vehicleName}].")
        Toast.makeText(context, "Nama kendaraan diperbarui ke [${updated.vehicleName}]", Toast.LENGTH_SHORT).show()
    }

    fun unbindCurrentDevice() {
        val serial = _firmwareIdentity.value.serial
        bindingPrefs.edit()
            .remove(bindingKey(serial, "bound_at"))
            .remove(bindingKey(serial, "bound_fw"))
            .remove(bindingKey(serial, "vehicle"))
            .apply()
        _bindingRecord.value = null
        if (_isConnected.value) {
            _sessionPhase.value = SessionPhase.NEEDS_BINDING
        }
        appendLog("BINDING: Binding serial telah dilepas.")
        Toast.makeText(context, "Binding perangkat dilepas. Mode beralih ke Read-Only.", Toast.LENGTH_SHORT).show()
    }

    // State perangkat nyata dimulai UNKNOWN/CORE kosong. Mask demo hanya diisi
    // saat pengguna benar-benar mengaktifkan mode simulasi.
    private val _moduleStatus = MutableStateFlow(ModuleStatus.defaultCore())
    val moduleStatus: StateFlow<ModuleStatus> = _moduleStatus.asStateFlow()

    private val _firmwareVersionInfo = MutableStateFlow(
        FirmwareVersionInfo(
            schema = 1,
            release = McuPlatform.CURRENT_FIRMWARE_RELEASE,
            semver = McuPlatform.CURRENT_FIRMWARE_SEMVER,
            buildId = "20260923",
            platform = "ESP32",
            protocolVersion = 5,
            telemetryVersion = 3
        )
    )
    val firmwareVersionInfo: StateFlow<FirmwareVersionInfo> = _firmwareVersionInfo.asStateFlow()

    private val _firmwareIdentity = MutableStateFlow(FirmwareIdentityInfo())
    val firmwareIdentity: StateFlow<FirmwareIdentityInfo> = _firmwareIdentity.asStateFlow()

    private val _commissionStatus = MutableStateFlow(CommissionStatus())
    val commissionStatus: StateFlow<CommissionStatus> = _commissionStatus.asStateFlow()

    private val _adcReadings = MutableStateFlow(AdcReadings())
    val adcReadings: StateFlow<AdcReadings> = _adcReadings.asStateFlow()

    private val _firmwareTempStatus = MutableStateFlow(FirmwareTempStatus())
    val firmwareTempStatus: StateFlow<FirmwareTempStatus> = _firmwareTempStatus.asStateFlow()

    private fun requireCapability(token: String, action: String): Boolean {
        if (_isSimulationMode.value) return true
        if (token in _mcuCapabilities.value) return true
        Toast.makeText(context, "Firmware tidak mendukung $action ($token).", Toast.LENGTH_LONG).show()
        appendLog("CAPS GUARD: $action ditolak; capability $token tidak tersedia")
        return false
    }

    fun toggleModuleInstalled(module: HardwareModule) {
        if (!requireCapability("MODULE_STATUS", "status modul")) return
        val current = _moduleStatus.value
        val isCurrentlyInstalled = current.isInstalled(module)
        val targetOn = !isCurrentlyInstalled

        if (_isSimulationMode.value) {
            val newInstalled = if (targetOn) (current.installedMask or module.bitMask) else (current.installedMask and module.bitMask.inv())
            val newActive = if (!targetOn) (current.activeMask and module.bitMask.inv()) else current.activeMask
            val newCore = if (module == HardwareModule.SIDE) {
                if (targetOn && (newActive and module.bitMask) != 0) 2 else if (targetOn) 1 else 0
            } else current.coreProfile
            _moduleStatus.value = current.copy(installedMask = newInstalled, activeMask = newActive, coreProfile = newCore)
            appendLog("Demo: Modul [${module.title}] ${if (targetOn) "TERPASANG" else "DILEPAS"}")
            return
        }

        if (!canWrite()) {
            Toast.makeText(context, setupWriteBlockReason.value ?: "Izin tulis diblokir (Read-Only)", Toast.LENGTH_SHORT).show()
            return
        }
        if (!checkSetupWriteSafety("Ubah modul ${module.title}")) return

        val stateStr = if (targetOn) "ON" else "OFF"
        appendLog("Kirim: MODULE,SET,${module.id},$stateStr")
        markSetupCommandPending()
        bleClient.send("MODULE,SET,${module.id},$stateStr")
    }

    fun setModuleActive(module: HardwareModule, active: Boolean) {
        val current = _moduleStatus.value
        if (!current.isInstalled(module)) {
            appendLog("Modul [${module.title}] belum terpasang fisik!")
            return
        }
        if (_isSimulationMode.value) {
            val newActive = if (active) (current.activeMask or module.bitMask) else (current.activeMask and module.bitMask.inv())
            val newCore = if (module == HardwareModule.SIDE) {
                if (active) 2 else 1
            } else current.coreProfile
            _moduleStatus.value = current.copy(activeMask = newActive, coreProfile = newCore)
            appendLog("Demo: Modul [${module.title}] ${if (active) "AKTIF" else "NONAKTIF"}")
            return
        }
        if (!canWrite()) {
            Toast.makeText(context, setupWriteBlockReason.value ?: "Izin tulis diblokir (Read-Only)", Toast.LENGTH_SHORT).show()
            return
        }
        if (!checkSetupWriteSafety("Aktivasi modul ${module.title}")) return
        markSetupCommandPending()
        bleClient.send("MODULE,SET,${module.id},${if (active) "ON" else "OFF"}")
    }

    fun installCoreOemRemoved() {
        if (!requireMcuOrDemo("Pemasangan Core") ||
            !requireCapability("QUICK_INSTALL", "Quick Install")) return
        if (!checkSetupWriteSafety("Pemasangan Core")) return
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,INSTALL,CORE,OEM_REMOVED")
            appendLog("BLE Send: SETUP,INSTALL,CORE,OEM_REMOVED")
        } else {
            val cur = _moduleStatus.value
            _moduleStatus.value = cur.copy(coreProfile = 0)
            _commissionStatus.value = _commissionStatus.value.copy(stage = 1, nextAction = 2)
            appendLog("Demo: Pemasangan Core (1 Coil) tersimpan. Lanjut ke Pemeriksaan Pickup.")
        }
        Toast.makeText(context, if (bleClient.gattReady) "Perintah Pasang Core dikirim" else "Core terpasang di Demo", Toast.LENGTH_SHORT).show()
    }

    fun installDualOemRemoved() {
        if (!requireMcuOrDemo("Pemasangan Dual Coil") ||
            !requireCapability("QUICK_INSTALL", "Quick Install")) return
        if (!checkSetupWriteSafety("Pemasangan Dual Coil")) return
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,INSTALL,DUAL,OEM_REMOVED")
            appendLog("BLE Send: SETUP,INSTALL,DUAL,OEM_REMOVED")
        } else {
            val cur = _moduleStatus.value
            _moduleStatus.value = cur.copy(
                installedMask = cur.installedMask or HardwareModule.SIDE.bitMask,
                coreProfile = 1
            )
            _commissionStatus.value = _commissionStatus.value.copy(stage = 1, nextAction = 2)
            appendLog("Demo: Pemasangan Dual Coil tersimpan. Lanjut ke Pemeriksaan Pickup.")
        }
        Toast.makeText(context, if (bleClient.gattReady) "Perintah Pasang Dual dikirim" else "Dual Coil terpasang di Demo", Toast.LENGTH_SHORT).show()
    }

    fun confirmReadyDual(sideOffsetCdeg: Int = 0) {
        confirmReadyTripleSpark(sideOffsetCdeg)
    }

    // ==========================================
    // STATUS STREAMING & WATCHDOG TELEMETRI
    // ==========================================
    // Watchdog toleransi 2000ms untuk mencegah false-timeout / fluktuasi 2Hz semu.
    // Menjamin stabilitas status online real-time murni dari hardware.
    private val _isTelemetryStreaming = MutableStateFlow(false)
    val isTelemetryStreaming: StateFlow<Boolean> = _isTelemetryStreaming.asStateFlow()

    // ==========================================
    // LOGIKA KEAMANAN TOMBOL (COMMAND GUARDS & WRITE GATE)
    // ==========================================
    private fun isTelemetryFresh(maxAgeMs: Long = 2_000L): Boolean {
        val last = lastTelemetryPacketAtMs
        return last > 0L && SystemClock.elapsedRealtime() - last <= maxAgeMs
    }

    fun canWrite(): Boolean {
        if (_isSimulationMode.value) return true
        if (!_isConnected.value || !isTelemetryFresh()) return false
        if (_sessionPhase.value != SessionPhase.READY_FULL) return false
        val serial = _firmwareIdentity.value.serial
        if (serial == "UNAVAILABLE" || serial == "IGT-ESP32-UNKNOWN" || serial.isBlank()) return false
        val bound = _bindingRecord.value
        return bound != null && bound.serial == serial
    }

    private fun computeSetupWriteBlockReason(
        t: Telemetry,
        connected: Boolean,
        isSim: Boolean,
        pending: Boolean,
        isBusy: Boolean,
        learning: Boolean,
        phase: SessionPhase,
        boundRecord: BindingRecord?,
        identity: FirmwareIdentityInfo
    ): String? {
        if (!connected && !isSim) {
            return "CDI belum terhubung. Hubungkan BLE atau aktifkan Mode Simulasi."
        }
        if (connected && !isSim) {
            if (phase == SessionPhase.SYNCING) {
                return "Sinkronisasi firmware sedang berjalan..."
            }
            if (identity.serial == "UNAVAILABLE") {
                return "Perangkat dalam mode Read-Only (Serial UNAVAILABLE / Firmware dibatasi)."
            }
            if (boundRecord == null || boundRecord.serial != identity.serial) {
                return "Perangkat dalam mode Read-Only. Konfirmasi binding serial [${identity.serial}] di tab Perangkat untuk mengaktifkan izin tulis."
            }
            if (phase == SessionPhase.READY_READ_ONLY) {
                return "Perangkat dalam mode Terbatas (Read-Only)."
            }
        }
        if (t.rpm > 0 && !learning) {
            return "Mesin sedang menyala (${t.rpm} RPM). Matikan mesin (RPM 0) demi aturan keselamatan setup_can_write()!"
        }
        if (connected && !isSim && (t.hvEnabled || t.hvCenter >= 30 || t.hvSide >= 30)) {
            return "Tegangan HV masih aktif (Center ${t.hvCenter}V, Side ${t.hvSide}V). Tunggu kapasitor discharge di bawah 30V."
        }
        if (learning) {
            return "Proses OEM Learn sedang aktif. Selesaikan atau simpan pembelajaran terlebih dahulu."
        }
        if (pending || isBusy) {
            return "Antrean perintah BLE sedang memproses request sebelumnya. Tunggu ACK selesai."
        }
        return null
    }

    val setupWriteBlockReason: StateFlow<String?> = combine(
        combine(_telemetry, _isConnected, _isSimulationMode) { t, conn, sim -> Triple(t, conn, sim) },
        combine(_setupCommandPending, bleClient.isBusy, _isOemLearning) { pending, busy, learning -> Triple(pending, busy, learning) },
        combine(_sessionPhase, _bindingRecord, _firmwareIdentity) { phase, bound, id -> Triple(phase, bound, id) }
    ) { (t, conn, sim), (pending, busy, learning), (phase, bound, id) ->
        computeSetupWriteBlockReason(t, conn, sim, pending, busy, learning, phase, bound, id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val setupCanWrite: StateFlow<Boolean> = setupWriteBlockReason.map { it == null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /**
     * Mengosongkan dan mengisolasi seluruh state simulasi/demo agar tidak pernah merembes ke mode nyata.
     */
    fun resetDemoState() {
        _demoEngineRunning.value = false
        _isRevving.value = false
        _demoThrottleSlider.value = 0f
        simRpm = 0f
        simTps = 0f
        engineSound.stop()
    }

    /**
     * Mereset seluruh simulasi commissioning dan status demo dari awal (Tahap 1: Pemasangan).
     * Memastikan mode demo kembali ke kondisi bawaan: Dual Coil aktif & seluruh modul disimulasikan terpasang.
     */
    fun resetDemoCommissioning() {
        resetDemoState()

        val curT = _telemetry.value
        _telemetry.value = curT.copy(
            rpm = 0,
            tps = 0,
            advanceCdeg = 0,
            hvCenter = 0,
            hvSide = 0,
            setupStage = 0,
            flags = 0,
            outputFlags = 0x03, // Dual coil ready di demo
            limiter = 0,
            pickupQuality = 95
        )

        _commissionStatus.value = CommissionStatus(
            stage = 0,
            nextAction = 1,
            ready = false,
            advisoryMask = 0
        )

        // Reset modul ke default demo: Seluruh 5 modul terpasang & Dual Coil aktif
        _moduleStatus.value = ModuleStatus(
            installedMask = 31, // Seluruh modul: SIDE=1, THERMAL=2, OEM_LEARN=4, AUX=8, TPS_DIAG=16
            activeMask = 27,    // SIDE, THERMAL, AUX, TPS_DIAG aktif
            observedMask = 31,
            faultMask = 0,
            coreProfile = 2     // Dual Coil (Core + SIDE)
        )

        _tpsClosedAdc.value = 820
        _tpsOpenAdc.value = 3940
        _pulserOffsetDeg.value = 0f
        _quickSetupPage.value = 0
        _quickSetupUnlockedStage.value = 0

        context.getSharedPreferences("cdi_r7_prefs", Context.MODE_PRIVATE).edit()
            .putInt("setup_stage", 0)
            .apply()

        appendLog("DEMO RESET: Seluruh simulasi commissioning direset ke Tahap 1 (Pemasangan). Dual Coil & 5 modul aktif.")
        Toast.makeText(context, "Simulasi Demo & Wizard Setup berhasil direset ke awal!", Toast.LENGTH_SHORT).show()
    }

    // Maps State - 4 Flash Memory Slots (ECO, STREET, RAIN, PRO) with two flash pages & CRC32
    val mapPresets = listOf(
        MapSlotData(
            slot = 0,
            name = "Slot 1: ECO",
            description = "Kurva default firmware untuk jalan raya dan efisiensi. Grid Normal 8x4, limiter 9.500 RPM, target HV 285V.",
            revLimit = 9500,
            peakAdvance = 35.0f,
            curvePoints = listOf(
                500 to 0f, 1000 to 1f, 1500 to 4f, 2500 to 9f,
                4000 to 17f, 6000 to 24f, 8000 to 30f, 10000 to 35f
            )
        ),
        MapSlotData(
            slot = 1,
            name = "Slot 2: STREET",
            description = "Map STREET default firmware. Grid Normal 8x4, limiter 9.500 RPM, target HV 285V.",
            revLimit = 9500,
            peakAdvance = 36.0f,
            curvePoints = listOf(
                500 to 0f, 1000 to 2f, 1500 to 5f, 2500 to 10f,
                4000 to 18f, 6000 to 25f, 8000 to 31f, 10000 to 36f
            )
        ),
        MapSlotData(
            slot = 2,
            name = "Slot 3: RAIN",
            description = "Map RAIN default firmware untuk kondisi basah/Low-RON. Grid Normal 8x4, limiter 9.500 RPM, target HV 285V.",
            revLimit = 9500,
            peakAdvance = 34.0f,
            curvePoints = listOf(
                500 to 0f, 1000 to 0f, 1500 to 3f, 2500 to 8f,
                4000 to 16f, 6000 to 23f, 8000 to 29f, 10000 to 34f
            )
        ),
        MapSlotData(
            slot = 3,
            name = "Slot 4: PRO",
            description = "Map PRO default firmware 16x8 / 345V. Diaktifkan lewat FEATURE,PRO,ON tanpa jumper fisik. Limiter default 11.000 RPM.",
            revLimit = 11000,
            peakAdvance = 36.0f,
            curvePoints = listOf(
                500 to 0.5f, 1000 to 2.5f, 1500 to 5.5f, 2500 to 10.5f,
                4000 to 18.5f, 6000 to 25.5f, 8000 to 31.5f, 10000 to 36f, 11500 to 36f
            )
        )
    )

    private val _selectedMapSlot = MutableStateFlow(1)
    val selectedMapSlot: StateFlow<Int> = _selectedMapSlot.asStateFlow()

    private val _activeMapRpmCount = MutableStateFlow(0)
    val activeMapRpmCount: StateFlow<Int> = _activeMapRpmCount.asStateFlow()
    private val _activeMapTpsCount = MutableStateFlow(0)
    val activeMapTpsCount: StateFlow<Int> = _activeMapTpsCount.asStateFlow()

    // Kurva awal mengikuti baris TPS 0% map STREET default firmware.
    private val _customAdvancePoints = MutableStateFlow(
        listOf(
            CustomAdvancePoint(500, 0.0f), CustomAdvancePoint(1000, 2.0f),
            CustomAdvancePoint(1500, 5.0f), CustomAdvancePoint(2500, 10.0f),
            CustomAdvancePoint(4000, 18.0f), CustomAdvancePoint(6000, 25.0f),
            CustomAdvancePoint(8000, 31.0f), CustomAdvancePoint(10000, 36.0f)
        )
    )
    val customAdvancePoints: StateFlow<List<CustomAdvancePoint>> = _customAdvancePoints.asStateFlow()

    private val _customMapLoadAxis = MutableStateFlow(listOf(0, 25, 50, 75, 100))
    val customMapLoadAxis: StateFlow<List<Int>> = _customMapLoadAxis.asStateFlow()

    private val _selectedCustomLoadIndex = MutableStateFlow(0)
    val selectedCustomLoadIndex: StateFlow<Int> = _selectedCustomLoadIndex.asStateFlow()

    private val customMapRows = mutableMapOf<Int, List<CustomAdvancePoint>>()

    private fun rebuildLoadAxis(maxPoints: Int) {
        val count = maxPoints.coerceIn(2, 16)
        val axis = List(count) { index ->
            ((100L * index) / (count - 1).coerceAtLeast(1)).toInt()
        }
        val base = _customAdvancePoints.value
        val oldRows = customMapRows.toMap()
        customMapRows.clear()
        axis.indices.forEach { index ->
            customMapRows[index] = oldRows[index] ?: base.map { it.copy() }
        }
        _customMapLoadAxis.value = axis
        _selectedCustomLoadIndex.value = _selectedCustomLoadIndex.value.coerceIn(axis.indices)
        _customAdvancePoints.value =
            customMapRows[_selectedCustomLoadIndex.value]?.map { it.copy() } ?: base
    }

    fun selectCustomMapLoad(index: Int) {
        val safe = index.coerceIn(_customMapLoadAxis.value.indices)
        customMapRows[_selectedCustomLoadIndex.value] = _customAdvancePoints.value.map { it.copy() }
        _selectedCustomLoadIndex.value = safe
        _customAdvancePoints.value =
            customMapRows[safe]?.map { it.copy() } ?: _customAdvancePoints.value.map { it.copy() }
    }

    private val _softRevLimiterRpm = MutableStateFlow(9500)
    val softRevLimiterRpm: StateFlow<Int> = _softRevLimiterRpm.asStateFlow()

    private val _hardRevLimiterRpm = MutableStateFlow(9500)
    val hardRevLimiterRpm: StateFlow<Int> = _hardRevLimiterRpm.asStateFlow()

    private val _softBandRpm = MutableStateFlow(400)
    val softBandRpm: StateFlow<Int> = _softBandRpm.asStateFlow()

    private val _limiterType = MutableStateFlow("SOFT")
    val limiterType: StateFlow<String> = _limiterType.asStateFlow()

    // J1 Hardware confirmation map
    private val _j1ConfirmedMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val j1ConfirmedMap: StateFlow<Map<String, Boolean>> = _j1ConfirmedMap.asStateFlow()

    // Custom audio track list
    private val _customSoundTracks = MutableStateFlow<List<CustomSoundTrack>>(emptyList())
    val customSoundTracks: StateFlow<List<CustomSoundTrack>> = _customSoundTracks.asStateFlow()

    private val _selectedCustomTrack = MutableStateFlow<CustomSoundTrack?>(null)
    val selectedCustomTrack: StateFlow<CustomSoundTrack?> = _selectedCustomTrack.asStateFlow()

    // Hold-To-Rev State
    private val _isRevving = MutableStateFlow(false)
    val isRevving: StateFlow<Boolean> = _isRevving.asStateFlow()

    private val _demoThrottleSlider = MutableStateFlow(0f)
    val demoThrottleSlider: StateFlow<Float> = _demoThrottleSlider.asStateFlow()

    // Strobo Calibration State
    private val _strobeActive = MutableStateFlow(false)
    val strobeActive: StateFlow<Boolean> = _strobeActive.asStateFlow()

    private val _pulserOffsetDeg = MutableStateFlow(0.0f) // -5.0 to +5.0
    private var triggerEditBaseCdeg = 6000
    val pulserOffsetDeg: StateFlow<Float> = _pulserOffsetDeg.asStateFlow()

    private val _flashSaved = MutableStateFlow(false)
    val flashSaved: StateFlow<Boolean> = _flashSaved.asStateFlow()

    // Sound State
    private val _soundEnabled = MutableStateFlow(false)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _soundVolume = MutableStateFlow(0.85f)
    val soundVolume: StateFlow<Float> = _soundVolume.asStateFlow()

    private val _soundPreset = MutableStateFlow(EngineSound.Preset.SINGLE)
    val soundPreset: StateFlow<EngineSound.Preset> = _soundPreset.asStateFlow()

    // Console logs
    private val _terminalLogs = MutableStateFlow<List<String>>(
        listOf(
            "NS200-CDI System Initialized.",
            "MoTeC / AIM Telemetry Protocol Engine Ready.",
            "Firmware Engine: 32x16 3D Map, Dyno Live Trim, Dual-Core Safety.",
            "Hardware Target: ${_selectedPlatform.value.displayName}."
        )
    )
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    // Simulation job
    private var simulationJob: Job? = null
    private var oemLearnPollJob: Job? = null
    private var simRpm = 1420f
    private var simTps = 0f
    private var simPhase = 0f
    private val mapReadback = mutableMapOf<Pair<Int, Int>, Float>()
    private var mapReadbackRpmCount = 0
    private var mapReadbackTpsCount = 0

    init {
        // Initialize with default raw packet
        _rawPacket.value = CdiProtocol.packetFromTelemetry(_telemetry.value, CdiProtocol.KIND_CORE)

        // Restore sound settings from SharedPreferences
        val prefs = context.getSharedPreferences("cdi_r7_prefs", Context.MODE_PRIVATE)
        val savedPreset = prefs.getString("sound_preset", EngineSound.Preset.SINGLE.name)
        try {
            _soundPreset.value = EngineSound.Preset.valueOf(savedPreset ?: EngineSound.Preset.SINGLE.name)
            engineSound.select(_soundPreset.value)
        } catch (_: Exception) {}

        _soundVolume.value = prefs.getFloat("sound_volume", 0.85f).coerceIn(0f, 1f)
        engineSound.masterVolume = _soundVolume.value
        prefs.getString("custom_audio_uri", null)?.let { saved ->
            runCatching {
                val uri = Uri.parse(saved)
                val track = CustomSoundTrack(
                    "persisted", prefs.getString("custom_audio_name", "Custom") ?: "Custom",
                    uri, prefs.getInt("custom_audio_rpm", 2000), "MP3/WAV/OGG"
                )
                _customSoundTracks.value = listOf(track)
                if (_soundPreset.value == EngineSound.Preset.CUSTOM) selectCustomTrack(track)
            }
        }

        _pulserOffsetDeg.value = prefs.getFloat("pulser_offset", 0.0f)
        _softRevLimiterRpm.value = prefs.getInt("rev_limiter", 9500)
        val savedStage = prefs.getInt("setup_stage", SetupStage.BARU.code)
            .coerceIn(SetupStage.BARU.code, SetupStage.READY.code)
        _quickSetupPage.value = savedStage
        _quickSetupUnlockedStage.value = savedStage

        // Restore Custom Map Points if previously saved
        val savedCustomMap = prefs.getString("custom_map_points", null)
        if (savedCustomMap != null) {
            try {
                val parsed = savedCustomMap.split(";").mapNotNull { part ->
                    val sub = part.split(":")
                    if (sub.size == 2) {
                        CustomAdvancePoint(sub[0].toInt(), sub[1].toFloat() / 10f)
                    } else null
                }
                if (parsed.size >= 4) {
                    _customAdvancePoints.value = parsed
                }
            } catch (_: Exception) {}
        }

        // Start internal ticker for smooth simulation when not connected to hardware
        startSimulationEngine()
    }

    fun setTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun toggleConnect() {
        if (bleClient.gattReady || bleClient.isBusy.value || bleClient.isScanning.value) {
            bleClient.disconnect()
            _isConnected.value = false
            _connectionStatus.value = "Disconnected"
            appendLog("Manual BLE disconnect / cancel requested.")
        } else {
            _isSimulationMode.value = false
            _isRevving.value = false
            _demoThrottleSlider.value = 0f
            simRpm = 0f
            simTps = 0f
            engineSound.stop()
            _isConnected.value = false

            val savedMac = bleClient.savedDeviceMac.value
            if (!savedMac.isNullOrBlank()) {
                val savedName = bleClient.savedDeviceName.value ?: "IGNITRA CDI"
                appendLog("Koneksi langsung ke $savedName [$savedMac] (Mode Hemat Baterai, GPS tidak diperlukan)...")
                bleClient.connectSavedDevice()
            } else {
                appendLog("Memindai IGNITRA CDI BLE...")
                bleClient.connect()
            }
        }
    }

    fun isBluetoothEnabled() = bleClient.isBluetoothEnabled()
    fun hasBlePermissions() = bleClient.hasPermissions()
    fun hasConnectPermission() = bleClient.hasConnectPermission()

    fun connectDirectSaved() {
        val savedMac = bleClient.savedDeviceMac.value
        if (savedMac.isNullOrBlank()) {
            Toast.makeText(context, "Belum ada modul CDI tersimpan", Toast.LENGTH_SHORT).show()
            return
        }
        _isSimulationMode.value = false
        resetDemoState()
        val name = bleClient.savedDeviceName.value ?: "IGNITRA CDI"
        appendLog("Koneksi langsung ke $name [$savedMac] (Mode Hemat Baterai, GPS tidak aktif)...")
        bleClient.connectSavedDevice()
    }

    fun connectDirectAddress(mac: String, name: String? = null) {
        _isSimulationMode.value = false
        resetDemoState()
        appendLog("Koneksi langsung ke MAC: $mac (Bebas GPS / Hemat Baterai)...")
        val ok = bleClient.connectAddress(mac, name)
        if (!ok) {
            Toast.makeText(context, "Format MAC Address tidak valid", Toast.LENGTH_SHORT).show()
        }
    }

    fun forgetSavedDevice() {
        bleClient.forgetSavedDevice()
        appendLog("Modul CDI tersimpan telah dihapus.")
        Toast.makeText(context, "Modul tersimpan telah dihapus", Toast.LENGTH_SHORT).show()
    }

    fun setPowerSaveMode(enabled: Boolean) {
        bleClient.setPowerSaveMode(enabled)
        appendLog(if (enabled) "Mode Hemat Daya aktif (Prioritas Seimbang • Menghemat baterai saat setting)" else "Mode Balap aktif (Prioritas Tinggi • Telemetri Ultra Cepat)")
    }

    fun setAutoConnectOnStart(enabled: Boolean) {
        bleClient.setAutoConnectOnStart(enabled)
    }

    fun startBleScan() {
        _isSimulationMode.value = false
        resetDemoState()
        appendLog("Memindai perangkat BLE sekitar (Hemat Daya)...")
        bleClient.startScan(discoveryOnly = true)
    }

    fun stopBleScan() {
        bleClient.stopScanInternal()
        appendLog("BLE scan dihentikan.")
    }

    @SuppressLint("MissingPermission")
    fun connectBleDevice(device: BluetoothDevice) {
        _isSimulationMode.value = false
        resetDemoState()
        val dName = try {
            if (bleClient.hasPermissions()) device.name ?: device.address else "perangkat BLE"
        } catch (_: SecurityException) {
            "perangkat BLE"
        }
        appendLog("Menghubungkan langsung ke BLE: $dName")
        bleClient.connectDeviceExplicit(device)
    }

    fun toggleSimulation() {
        _isSimulationMode.value = !_isSimulationMode.value
        if (_isSimulationMode.value) {
            if (bleClient.gattReady || bleClient.isBusy.value) bleClient.disconnect()
            _firmwareCapabilities.value = FirmwareCapabilities.demoR9()
            _engineProfile.value = EngineProfile.universal()
            _connectionStatus.value = "SIMULASI AKTIF • Telemetry 20Hz (MoTeC Mode)"
            _isConnected.value = true
            // Default simulasi: Mesin hidup stasioner idle ~1.420 RPM layaknya motor hidup normal
            _demoEngineRunning.value = true
            _isRevving.value = false
            _demoThrottleSlider.value = 0f
            simRpm = 1420f
            simTps = 0.02f
            appendLog("Demo simulation mode activated (Mesin: Hidup Idle ~1.420 RPM).")
            Toast.makeText(context, "Mode Simulasi Aktif: Mesin Hidup Idle ~1.420 RPM", Toast.LENGTH_SHORT).show()
        } else {
            _demoEngineRunning.value = false
            _isRevving.value = false
            _demoThrottleSlider.value = 0f
            simRpm = 0f
            simTps = 0f
            engineSound.stop()
            val currentT = _telemetry.value
            _telemetry.value = currentT.copy(
                rpm = 0,
                tps = 0,
                hvCenter = 0,
                hvSide = 0,
                outputFlags = 0,
                limiter = 0
            )
            _connectionStatus.value = "SIMULASI NONAKTIF • Menunggu Hardware CDI"
            _isConnected.value = false
            appendLog("Demo simulation mode stopped.")
            Toast.makeText(context, "Mode Simulasi Nonaktif", Toast.LENGTH_SHORT).show()
        }
    }

    private var blipJob: Job? = null

    /**
     * Simulasi putar tuas gas sekejap (Quick Throttle Twist / Blip).
     * Mensimulasikan bukaan tuas gas responsif (TPS melesat cepat ke 85% lalu kembali ke nol)
     * lengkap dengan lonjakan RPM spontan, knalpot meraung, dan deselerasi kembali ke idle.
     */
    fun triggerThrottleBlip() {
        if (!bleClient.gattReady && !_isSimulationMode.value) {
            _isSimulationMode.value = true
            _isConnected.value = true
            _connectionStatus.value = "SIMULASI AKTIF • Throttle Blip"
        }
        // Pastikan mesin menyala saat tuas gas diputar
        _demoEngineRunning.value = true

        blipJob?.cancel()
        blipJob = viewModelScope.launch(Dispatchers.Default) {
            _isRevving.value = true
            appendLog("BLIP: Putar tuas gas sekejap (Quick Throttle Twist)")

            val startRpm = if (simRpm < 1200f) 1420f else simRpm
            val maxLimit = _softRevLimiterRpm.value.toFloat().coerceAtLeast(10000f)
            val peakBlipRpm = (startRpm + 4800f).coerceAtMost(maxLimit - 400f)

            // Fase 1: Hentakan bukaan tuas gas (Attack: ~120ms)
            val attackTicks = 5
            for (i in 1..attackTicks) {
                val ratio = i.toFloat() / attackTicks
                val tpsVal = 0.85f * ratio
                simTps = tpsVal
                simRpm = startRpm + (peakBlipRpm - startRpm) * (ratio * ratio)
                delay(24)
            }

            // Fase 2: Puncak raungan gas sejenak (Peak hold: ~90ms)
            delay(90)

            // Fase 3: Tuas gas dilepas kembali ke posisi idle (jangan sentuh slider manual user)
            simTps = 0.02f
            _isRevving.value = false

            // Fase 4: Deselerasi RPM meluruh bertahap sesuai inersia kruk as (Decay: ~360ms)
            val decayTicks = 12
            val currentPeak = simRpm
            val idleTarget = if (_demoEngineRunning.value) 1420f else 0f
            for (i in 1..decayTicks) {
                val progress = i.toFloat() / decayTicks
                val factor = 1.0f - (1.0f - progress).let { it * it }
                simRpm = currentPeak - (currentPeak - idleTarget) * factor
                delay(30)
            }
            simRpm = idleTarget
            appendLog("BLIP selesai: RPM kembali stabil ke idle (~${idleTarget.toInt()} RPM).")
        }
    }

    fun setHoldToRev(pressed: Boolean) {
        if (pressed) {
            blipJob?.cancel()
            _isRevving.value = true
            // Hidupkan mesin jika sedang mati
            _demoEngineRunning.value = true
            if (bleClient.gattReady) {
                appendLog("Hold To Rev hanya audio/simulasi; pengapian nyata tidak diperintah")
            } else if (!_isConnected.value) {
                // If offline and not in simulation, start simulation so user can test sound & gauges
                _isSimulationMode.value = true
                _isConnected.value = true
                _connectionStatus.value = "SIMULASI AKTIF • Hold To Rev"
            }
        } else {
            _isRevving.value = false
            if (_demoThrottleSlider.value <= 0.01f) {
                simTps = if (_demoEngineRunning.value) 0.02f else 0f
            }
            appendLog("Hold To Rev dilepas -> RPM meluruh ke idle")
        }
    }

    fun setDemoThrottle(value: Float) {
        val v = value.coerceIn(0f, 1f)
        _demoThrottleSlider.value = v
        if (v > 0.01f) {
            _demoEngineRunning.value = true
            if (!bleClient.gattReady && !_isSimulationMode.value) {
                _isSimulationMode.value = true
                _isConnected.value = true
                _connectionStatus.value = "SIMULASI AKTIF • Demo Throttle"
            }
        }
    }

    fun setDemoRpmDirect(targetRpm: Float) {
        _demoEngineRunning.value = true
        val maxTarget = _softRevLimiterRpm.value.toFloat().coerceAtLeast(10000f)
        val fraction = ((targetRpm - 1420f) / (maxTarget - 1420f)).coerceIn(0f, 1f)
        setDemoThrottle(fraction)
    }

    fun resetDemoThrottle() {
        _isRevving.value = false
        _demoThrottleSlider.value = 0f
        _demoEngineRunning.value = true
        simTps = 0.02f
        simRpm = 1420f
        appendLog("Throttle di-reset ke IDLE (1.420 RPM)")
    }

    fun resetVirtualEngine() {
        _isRevving.value = false
        _demoThrottleSlider.value = 0f
        _demoEngineRunning.value = true
        simTps = 0.02f
        simRpm = 1420f
        engineSound.stop()
        val currentT = _telemetry.value
        _telemetry.value = currentT.copy(
            rpm = 1420,
            tps = 20,
            hvCenter = if (_isProVoltageConfigured.value) 345 else 285,
            hvSide = if (_isProVoltageConfigured.value) 345 else 285,
            outputFlags = 0x03,
            limiter = 0
        )
        appendLog("Virtual Engine di-reset: Mesin hidup idle ~1.420 RPM.")
        Toast.makeText(context, "Engine Reset: Mesin hidup idle ~1.420 RPM", Toast.LENGTH_SHORT).show()
    }

    fun updateCustomAdvancePoint(index: Int, newAdvance: Float) {
        val current = _customAdvancePoints.value.toMutableList()
        if (index in current.indices) {
            val rounded = (newAdvance * 10f).roundToInt() / 10f
            val caps = _firmwareCapabilities.value
            val bounded = rounded.coerceIn(caps.advanceMinDeg, caps.advanceMaxDeg)
            current[index] = current[index].copy(advanceDeg = bounded)
            _customAdvancePoints.value = current
            customMapRows[_selectedCustomLoadIndex.value] = current.map { it.copy() }
            appendLog("Map Custom: ${current[index].rpm} RPM diubah ke ${bounded}° BTDC")
        }
    }

    fun loadCustomPreset(presetKey: String) {
        val presetIndex = listOf("ECO", "STREET", "RAIN", "PRO").indexOf(presetKey)
        if (presetIndex >= 0) {
            val source = mapPresets[presetIndex].curvePoints.sortedBy { it.first }
            val caps = _firmwareCapabilities.value
            val pointCount = if (presetKey == "PRO") {
                minOf(16, caps.maxRpmPoints)
            } else {
                minOf(8, caps.maxRpmPoints)
            }.coerceAtLeast(2)
            val axis = List(pointCount) { index ->
                caps.rpmMin + ((caps.rpmMax - caps.rpmMin).toLong() * index /
                    (pointCount - 1).coerceAtLeast(1)).toInt()
            }
            fun sample(rpm: Int): Float {
                if (rpm <= source.first().first) return source.first().second
                if (rpm >= source.last().first) return source.last().second
                val right = source.indexOfFirst { it.first >= rpm }
                val a = source[right - 1]; val b = source[right]
                return a.second + (b.second - a.second) * (rpm - a.first) / (b.first - a.first).toFloat()
            }
            val row = axis.map {
                CustomAdvancePoint(it, sample(it).coerceIn(caps.advanceMinDeg, caps.advanceMaxDeg))
            }
            _customAdvancePoints.value = row
            customMapRows.clear()
            _customMapLoadAxis.value.indices.forEach { loadIndex ->
                customMapRows[loadIndex] = row.map { it.copy() }
            }
            _selectedCustomLoadIndex.value = 0
            _selectedMapSlot.value = presetIndex
            appendLog("Preset $presetKey dimuat pada grid firmware ${axis.size} titik")
        }
    }

    fun saveCustomMapToMcu(): String? {
        if (!checkSetupWriteSafety("Simpan map ignition")) {
            return setupWriteBlockReason.value ?: "Izin tulis map diblokir."
        }
        val t = _telemetry.value
        if (t.rpm > 0) return "Simpan map ditolak: mesin harus mati (RPM 0)."
        if ((t.hvEnabled || t.hvCenter >= 30 || t.hvSide >= 30) && _isConnected.value)
            return "Simpan map ditolak: charger OFF dan kedua bank HV harus <30 V."
        if (!bleClient.gattReady) return "CDI belum terhubung. Map tidak diklaim tersimpan ke MCU."

        val caps = _firmwareCapabilities.value
        val slot = _selectedMapSlot.value.coerceIn(0, caps.mapSlots - 1)
        val input = _customAdvancePoints.value.sortedBy { it.rpm }
        val rpmAxis = input.take(caps.maxRpmPoints).map { it.rpm.coerceIn(caps.rpmMin, caps.rpmMax) }
        if (rpmAxis.size < 2) return "Map minimal memerlukan dua titik RPM."
        val loadAxis = _customMapLoadAxis.value.take(caps.maxLoadPoints)
        customMapRows[_selectedCustomLoadIndex.value] = input.map { it.copy() }

        fun sample(target: Int): Float {
            if (target <= input.first().rpm) return input.first().advanceDeg
            if (target >= input.last().rpm) return input.last().advanceDeg
            val right = input.indexOfFirst { it.rpm >= target }
            val p0 = input[right - 1]
            val p1 = input[right]
            return p0.advanceDeg + (p1.advanceDeg - p0.advanceDeg) *
                (target - p0.rpm).toFloat() / (p1.rpm - p0.rpm).toFloat()
        }

        if (caps.protocolVersion >= 5) {
            bleClient.send("MAP,BEGIN,${rpmAxis.size},${loadAxis.size}")
            rpmAxis.forEachIndexed { index, rpm -> bleClient.send("MAP,RPM,$index,$rpm") }
            loadAxis.forEachIndexed { index, load -> bleClient.send("MAP,LOAD,$index,$load") }
            loadAxis.indices.forEach { loadIndex ->
                val row = customMapRows[loadIndex]?.sortedBy { it.rpm } ?: input
                fun sampleRow(target: Int): Float {
                    if (target <= row.first().rpm) return row.first().advanceDeg
                    if (target >= row.last().rpm) return row.last().advanceDeg
                    val right = row.indexOfFirst { it.rpm >= target }
                    val p0 = row[right - 1]
                    val p1 = row[right]
                    return p0.advanceDeg + (p1.advanceDeg - p0.advanceDeg) *
                        (target - p0.rpm).toFloat() / (p1.rpm - p0.rpm).toFloat()
                }
                rpmAxis.forEachIndexed { rpmIndex, rpm ->
                    val value = (sampleRow(rpm)
                        .coerceIn(caps.advanceMinDeg, caps.advanceMaxDeg) * 10f).roundToInt()
                    bleClient.send("MAP,CELL,$rpmIndex,$loadIndex,$value")
                }
            }
            bleClient.send("MAP,SAVE,$slot")
            bleClient.send("GET,PROFILE")
        } else {
            val legacyAxis = if (slot == 3) 16 else 8
            val legacyRows = if (slot == 3) 8 else 4
            bleClient.send("FEATURE,PRO,${if (slot == 3) "ON" else "OFF"}")
            bleClient.send("LOAD,$slot")
            repeat(legacyRows) { loadIndex ->
                input.take(legacyAxis).forEachIndexed { rpmIndex, point ->
                    bleClient.send("LIVE,$loadIndex,$rpmIndex,${(point.advanceDeg * 100f).roundToInt()}")
                }
            }
            bleClient.send("SAVE,$slot")
        }
        appendLog("Map slot ${slot + 1}: ${rpmAxis.size}x${loadAxis.size} dikirim via v${caps.protocolVersion}")
        return null
    }

    fun calibrateTpsMin() {
        if (!requireMcuOrDemo("kalibrasi TPS")) return
        if (!checkSetupWriteSafety("Kalibrasi TPS minimum")) return
        if (bleClient.gattReady) {
            bleClient.send("SETUP,TPS,CLOSED")
            appendLog("BLE Send: SETUP,TPS,CLOSED (Gas tertutup 0% disimpan)")
        } else {
            appendLog("Simulasi TPS Min (Gas Tertutup 0%) Disimpan")
        }
        Toast.makeText(context, if (bleClient.gattReady) "Perintah TPS CLOSED masuk antrean" else "TPS CLOSED tersimpan di Demo", Toast.LENGTH_SHORT).show()
    }

    fun calibrateTpsMax() {
        if (!requireMcuOrDemo("kalibrasi TPS")) return
        if (!checkSetupWriteSafety("Kalibrasi TPS maksimum")) return
        if (bleClient.gattReady) {
            bleClient.send("SETUP,TPS,OPEN")
            appendLog("BLE Send: SETUP,TPS,OPEN (Gas penuh 100% WOT disimpan)")
        } else {
            appendLog("Simulasi TPS Max (Gas Penuh 100% WOT) Disimpan")
        }
        Toast.makeText(context, if (bleClient.gattReady) "Perintah TPS OPEN masuk antrean" else "TPS OPEN tersimpan di Demo", Toast.LENGTH_SHORT).show()
    }

    fun setPulserEdge(isRising: Boolean) {
        if (!requireMcuOrDemo("pengaturan edge pulser")) return
        if (!checkSetupWriteSafety("Pengaturan edge pulser")) return
        val edgeStr = if (isRising) "RISING" else "FALLING"
        if (bleClient.gattReady) {
            bleClient.send("SETUP,EDGE,$edgeStr")
            appendLog("BLE Send: SETUP,EDGE,$edgeStr")
        } else {
            appendLog("Polaritas Pulser Edge diubah: $edgeStr")
        }
    }

    fun selectMapSlot(slot: Int) {
        val bounded = slot.coerceIn(0, _firmwareCapabilities.value.mapSlots - 1)
        val preset = mapPresets[bounded]
        if (bleClient.gattReady) {
            val t = _telemetry.value
            if (t.rpm != 0 || t.hvEnabled || t.hvCenter >= 30 || t.hvSide >= 30) {
                Toast.makeText(context, "LOAD ditolak: mesin harus mati dan HV < 30 V", Toast.LENGTH_LONG).show()
                return
            }
            if (_firmwareCapabilities.value.protocolVersion >= 5) {
                bleClient.send("MAP,SELECT,$bounded")
                bleClient.send("GET,PROFILE")
            } else {
                bleClient.send("FEATURE,PRO,${if (bounded == 3) "ON" else "OFF"}")
                bleClient.send("LOAD,$bounded")
                bleClient.send("GET,MODE")
            }
            appendLog("Map slot $bounded dipilih: ${preset.name}")
        } else if (_isSimulationMode.value) {
            _selectedMapSlot.value = bounded
            _softRevLimiterRpm.value = preset.revLimit
            appendLog("Memori Slot $bounded aktif: ${preset.name}")
        } else {
            Toast.makeText(context, "Hubungkan CDI untuk memilih slot", Toast.LENGTH_SHORT).show()
        }
    }

    fun setSoftRevLimiter(rpm: Int) {
        val caps = _firmwareCapabilities.value
        _softRevLimiterRpm.value = rpm.coerceIn(caps.rpmMin, caps.rpmMax)
    }

    fun setSoftBand(band: Int) {
        _softBandRpm.value = band.coerceIn(50, 3000)
    }

    fun setLimiterType(type: String) {
        _limiterType.value = if (type.equals("HARD", true)) "HARD" else "SOFT"
    }

    fun syncCurveToBle() {
        val slot = _selectedMapSlot.value.coerceIn(0, _firmwareCapabilities.value.mapSlots - 1)
        val caps = _firmwareCapabilities.value
        val rpm = _softRevLimiterRpm.value.coerceIn(caps.rpmMin, caps.rpmMax)
        _softRevLimiterRpm.value = rpm
        val band = _softBandRpm.value

        if (bleClient.gattReady) {
            val t = _telemetry.value
            if (t.rpm != 0 || t.hvEnabled || t.hvCenter >= 30 || t.hvSide >= 30) {
                Toast.makeText(context, "SYNC ditolak: mesin harus mati dan HV < 30 V", Toast.LENGTH_LONG).show()
                return
            }
            if (caps.protocolVersion >= 5) {
                bleClient.send("SET,LIMIT,$rpm")
                bleClient.send("GET,PROFILE")
            } else {
                bleClient.send("FEATURE,PRO,${if (slot == 3) "ON" else "OFF"}")
                bleClient.send("LOAD,$slot")
                bleClient.send("LIMIT,${_limiterType.value},$rpm,$band")
                bleClient.send("SAVE,$slot")
                bleClient.send("GET,MODE")
            }
            appendLog("Limiter $rpm RPM tersinkron via protokol v${caps.protocolVersion}")
        } else if (_isSimulationMode.value) {
            appendLog("Sync Kurva Map $slot (Limiter: $rpm RPM, Band: $band RPM) Disimpan Lokal.")
        } else {
            Toast.makeText(context, "Hubungkan CDI untuk menyinkronkan limiter", Toast.LENGTH_SHORT).show()
            return
        }

        context.getSharedPreferences("cdi_r7_prefs", Context.MODE_PRIVATE).edit()
            .putInt("rev_limiter", rpm)
            .apply()

        Toast.makeText(context, "Kurva Map ${slot + 1} & Rev-Limiter ($rpm RPM) Tersinkronisasi!", Toast.LENGTH_SHORT).show()
    }

    fun setSoundPreset(preset: EngineSound.Preset) {
        _soundPreset.value = preset
        engineSound.select(preset)
        context.getSharedPreferences("cdi_r7_prefs", Context.MODE_PRIVATE).edit()
            .putString("sound_preset", preset.name)
            .apply()
        appendLog("Sound preset switched to: ${preset.label}")
    }

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        engineSound.enabled = enabled
        appendLog("Sound engine ${if (enabled) "ENABLED" else "MUTED"}")
    }

    fun setSoundVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        _soundVolume.value = v
        engineSound.masterVolume = v
        context.getSharedPreferences("cdi_r7_prefs", Context.MODE_PRIVATE).edit()
            .putFloat("sound_volume", v).apply()
    }

    fun setCustomAudioFile(uri: Uri?) {
        if (uri == null) return
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Track_${System.currentTimeMillis()}"
        val newTrack = CustomSoundTrack(
            id = System.currentTimeMillis().toString(),
            name = fileName,
            uri = uri,
            baseRpm = 2000,
            format = "MP3/WAV/OGG"
        )
        val updated = _customSoundTracks.value + newTrack
        _customSoundTracks.value = updated
        context.getSharedPreferences("cdi_r7_prefs", Context.MODE_PRIVATE).edit()
            .putString("custom_audio_uri", uri.toString())
            .putString("custom_audio_name", fileName)
            .putInt("custom_audio_rpm", newTrack.baseRpm).apply()
        selectCustomTrack(newTrack)
    }

    fun selectCustomTrack(track: CustomSoundTrack) {
        _selectedCustomTrack.value = track
        engineSound.setCustom(track.uri, track.baseRpm)
        _soundPreset.value = EngineSound.Preset.CUSTOM
        appendLog("Track Kustom Aktif: ${track.name} (Base: ${track.baseRpm} RPM)")
    }

    fun removeCustomTrack(track: CustomSoundTrack) {
        val updated = _customSoundTracks.value.filter { it.id != track.id }
        _customSoundTracks.value = updated
        if (_selectedCustomTrack.value?.id == track.id) {
            _selectedCustomTrack.value = null
            setSoundPreset(EngineSound.Preset.SINGLE)
        }
    }

    fun advanceSetupStage(targetStageCode: Int) {
        val target = SetupStage.entries.find { it.code == targetStageCode } ?: return
        if (bleClient.gattReady && !_isSimulationMode.value) {
            appendLog("Tahap MCU hanya berubah setelah perintah setup terkait mendapat ACK; target: ${target.label}")
            return
        }
        appendLog("Demo: tahap lokal berubah ke ${target.label}")
        _quickSetupPage.value = target.code
        _quickSetupUnlockedStage.value = maxOf(_quickSetupUnlockedStage.value, target.code)

        // Update local telemetry stage
        val currentT = _telemetry.value
        val updatedFlags = if (target == SetupStage.READY) (currentT.flags or 0x20) else currentT.flags
        val updatedOutputs = when (target) {
            SetupStage.FIRST_START -> 0x01 // CENTER only, SIDE off
            SetupStage.READY -> 0x03       // CENTER & SIDE
            else -> currentT.outputFlags
        }
        val updatedLimiter = if (target == SetupStage.FIRST_START) 3000 else _softRevLimiterRpm.value

        _telemetry.value = currentT.copy(
            setupStage = target.code,
            flags = updatedFlags,
            outputFlags = updatedOutputs,
            limiter = if (target == SetupStage.FIRST_START) 1 else 0
        )
        _rawPacket.value = CdiProtocol.packetFromTelemetry(_telemetry.value, CdiProtocol.KIND_DIAGNOSTIC)

        // Save stage persistently
        context.getSharedPreferences("cdi_r7_prefs", Context.MODE_PRIVATE).edit()
            .putInt("setup_stage", target.code)
            .apply()
    }

    fun selectQuickSetupPage(targetStageCode: Int) {
        val target = SetupStage.entries.find { it.code == targetStageCode } ?: return
        val unlockedThrough = maxOf(_telemetry.value.setupStage, _quickSetupUnlockedStage.value)
        if (target.code > unlockedThrough) {
            _quickSetupMessage.value =
                "Tahap ${target.code + 1} masih terkunci. Selesaikan tahap ${unlockedThrough + 1} terlebih dahulu."
            appendLog("Wizard: ${target.label} masih terkunci")
            return
        }
        _quickSetupPage.value = target.code
    }

    fun startQuickSetupPreflight() {
        if (_isSimulationMode.value) {
            _quickSetupPage.value = SetupStage.PULSER.code
            _quickSetupUnlockedStage.value = maxOf(
                _quickSetupUnlockedStage.value,
                SetupStage.PULSER.code
            )
            _quickSetupMessage.value = "DEMO LULUS • halaman PULSER dibuka tanpa mengubah flash MCU."
            return
        }
        if (!bleClient.gattReady) {
            _quickSetupMessage.value = "GAGAL • GATT belum READY. Hubungkan CDI lewat menu BLE terlebih dahulu."
            appendLog("Quick Setup preflight ditolak: GATT belum READY")
            return
        }

        preflightPingOk = false
        preflightStatusOk = false
        preflightSetupOk = false
        _quickSetupPreflightBusy.value = true
        _quickSetupMessage.value = "MEMERIKSA • menunggu PING + STATUS + SETUP dari MCU..."
        preflightTimeoutJob?.cancel()

        val queued = bleClient.send("PING") &&
            bleClient.send("GET,STATUS") &&
            bleClient.send("GET,SETUP")
        if (!queued) {
            failQuickSetupPreflight("Perintah tidak dapat masuk antrean GATT.")
            return
        }

        appendLog("Quick Setup preflight: PING, GET STATUS, GET SETUP")
        preflightTimeoutJob = viewModelScope.launch {
            delay(10_000)
            if (_quickSetupPreflightBusy.value) {
                val missing = buildList {
                    if (!preflightPingOk) add("PING")
                    if (!preflightStatusOk) add("STATUS")
                    if (!preflightSetupOk) add("SETUP")
                }.joinToString(" + ")
                failQuickSetupPreflight("Timeout; respons belum diterima: $missing.")
            }
        }
    }

    private fun finishQuickSetupPreflightIfReady() {
        if (!_quickSetupPreflightBusy.value ||
            !preflightPingOk || !preflightStatusOk || !preflightSetupOk) return

        val t = _telemetry.value
        when {
            t.rpm > 0 -> failQuickSetupPreflight(
                "RPM masih ${t.rpm}. Matikan mesin; tahap awal hanya diperiksa saat RPM 0."
            )
            t.hvCenter >= 30 || t.hvSide >= 30 -> failQuickSetupPreflight(
                "HV belum aman: CENTER ${t.hvCenter} V, SIDE ${t.hvSide} V. Matikan kontak/kill switch dan tunggu <30 V."
            )
            else -> {
                preflightTimeoutJob?.cancel()
                _quickSetupPreflightBusy.value = false
                _quickSetupPage.value = SetupStage.PULSER.code
                _quickSetupUnlockedStage.value = maxOf(
                    _quickSetupUnlockedStage.value,
                    SetupStage.PULSER.code
                )
                _quickSetupMessage.value = if (_telemetryPacketCount.value == 0L) {
                    "LULUS KONTROL • PING/STATUS/SETUP valid. Telemetry 1001 belum masuk; lanjut ke PULSER, tetapi quality/RPM belum dapat dipantau."
                } else {
                    "LULUS • MCU merespons, HV <30 V, RPM 0, dan Telemetry 1001 aktif. Tahap 2 dibuka."
                }
                appendLog("Quick Setup preflight LULUS; halaman PULSER dibuka")
            }
        }
    }

    private fun updateQuickSetupPreflightProgress() {
        if (!_quickSetupPreflightBusy.value) return
        fun mark(ok: Boolean) = if (ok) "OK" else "MENUNGGU"
        _quickSetupMessage.value =
            "MEMERIKSA • PING ${mark(preflightPingOk)} | " +
                "STATUS ${mark(preflightStatusOk)} | SETUP ${mark(preflightSetupOk)}"
        finishQuickSetupPreflightIfReady()
    }

    private fun failQuickSetupPreflight(reason: String) {
        preflightTimeoutJob?.cancel()
        _quickSetupPreflightBusy.value = false
        _quickSetupMessage.value = "GAGAL • $reason"
        appendLog("Quick Setup preflight GAGAL: $reason")
    }

    fun toggleConfirmPin(pin: String) {
        val current = _j1ConfirmedMap.value.toMutableMap()
        val newState = !(current[pin] ?: false)
        current[pin] = newState
        _j1ConfirmedMap.value = current
        appendLog("Harness $pin konfirmasi: ${if (newState) "CONFIRMED" else "UNCHECK"}")
    }

    fun toggleStrobe(active: Boolean) {
        if (!requireMcuOrDemo("strobo TDC")) return
        if (active && !_strobeActive.value) {
            triggerEditBaseCdeg = _telemetry.value.triggerCdeg.coerceIn(0, 35999)
            _pulserOffsetDeg.value = 0f
        }
        _strobeActive.value = active
        if (bleClient.gattReady) {
            bleClient.send(if (active) "SETUP,STROBE,ON" else "SETUP,STROBE,OFF")
            appendLog("BLE Send: SETUP,STROBE,${if (active) "ON" else "OFF"} (${_selectedPlatform.value.strobePin})")
        } else {
            appendLog("Strobo LED ${_selectedPlatform.value.strobePin} ${if (active) "AKTIF (basis ${triggerEditBaseCdeg / 100f}°)" else "NONAKTIF"}")
        }
    }

    fun adjustPulserOffset(delta: Float) {
        val updated = ((_pulserOffsetDeg.value + delta) * 10f).toInt() / 10f
        setPulserOffset(updated)
    }

    fun setPulserOffset(offset: Float) {
        val clamped = offset.coerceIn(-5.0f, 5.0f)
        _pulserOffsetDeg.value = clamped
        _flashSaved.value = false

        if (bleClient.gattReady && _strobeActive.value) {
            bleClient.send("SETUP,OFFSET,${candidateTriggerCdeg(clamped)}")
        }
    }

    fun checkSetupWriteSafety(action: String): Boolean {
        if (_isConnected.value && !_isSimulationMode.value && !isTelemetryFresh()) {
            val msg = "SAFETY GUARD: Perintah '$action' diblokir! Telemetri tidak segar; kondisi RPM/HV tidak dapat dipastikan."
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            appendLog("GUARD [telemetry_fresh]: $msg")
            return false
        }
        val reason = computeSetupWriteBlockReason(
            _telemetry.value,
            _isConnected.value,
            _isSimulationMode.value,
            _setupCommandPending.value,
            bleClient.isBusy.value,
            _isOemLearning.value,
            _sessionPhase.value,
            _bindingRecord.value,
            _firmwareIdentity.value
        )
        if (reason != null) {
            val msg = "SAFETY GUARD: Perintah '$action' diblokir! $reason"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            appendLog("GUARD [setup_can_write]: $msg")
            return false
        }
        return true
    }

    fun checkFlashSafety(action: String): Boolean = checkSetupWriteSafety(action)

    fun saveCalibrationToFlash(): Boolean {
        if (!checkFlashSafety("Kalibrasi Flash")) return false

        val triggerCdeg = candidateTriggerCdeg(_pulserOffsetDeg.value)
        if (bleClient.gattReady) {
            _flashSaved.value = false
            markSetupCommandPending()
            if (_strobeActive.value) bleClient.send("SETUP,SAVE_TDC")
            else bleClient.send("SETUP,MANUAL_TDC,$triggerCdeg,CONFIRM")
            appendLog("BLE: simpan sudut absolut ${triggerCdeg / 100f}° -> Flash")
        } else if (_isSimulationMode.value) {
            appendLog("Simulasi: kalibrasi lokal ${_pulserOffsetDeg.value}°")
        } else {
            Toast.makeText(context, "CDI belum terhubung; tidak ada data yang ditulis ke flash", Toast.LENGTH_LONG).show()
            return false
        }

        context.getSharedPreferences("cdi_r7_prefs", Context.MODE_PRIVATE).edit()
            .putFloat("pulser_offset", _pulserOffsetDeg.value)
            .apply()

        triggerEditBaseCdeg = triggerCdeg
        _pulserOffsetDeg.value = 0f
        if (!bleClient.gattReady) _flashSaved.value = true
        Toast.makeText(context, if (bleClient.gattReady) "Perintah simpan kalibrasi masuk antrean MCU" else "Kalibrasi simulasi tersimpan lokal", Toast.LENGTH_LONG).show()
        return true
    }

    // --- QUICK SETUP R7 PROTOCOL METHODS ---

    fun pingCdiManual() {
        if (bleClient.gattReady) {
            bleClient.send("PING")
            appendLog("TX: PING")
            _quickSetupMessage.value = "TX: PING dikirim ke MCU... Menunggu respons PONG"
            Toast.makeText(context, "PING dikirim ke modul CDI", Toast.LENGTH_SHORT).show()
        } else if (_isSimulationMode.value) {
            appendLog("TX: PING (Simulasi)")
            appendLog("RX: PONG,CDI-R9-OK*CRC")
            preflightPingOk = true
            _quickSetupMessage.value = "PONG diterima dari MCU Simulasi R9 (Koneksi OK)"
            Toast.makeText(context, "Simulasi PONG diterima (OK)", Toast.LENGTH_SHORT).show()
        } else {
            _quickSetupMessage.value = "CDI Belum Terhubung • Hubungkan lewat tab BLE atau nyalakan DEMO"
            Toast.makeText(context, "CDI Belum Terhubung (Offline)", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestSetupState() {
        requestSetupStateManual()
    }

    fun requestSetupStateManual() {
        if (bleClient.gattReady) {
            bleClient.send("GET,SETUP")
            appendLog("TX: GET,SETUP")
            _quickSetupMessage.value = "TX: GET,SETUP dikirim ke MCU..."
            Toast.makeText(context, "Membaca konfigurasi setup dari MCU...", Toast.LENGTH_SHORT).show()
        } else if (_isSimulationMode.value) {
            appendLog("TX: GET,SETUP (Simulasi)")
            preflightSetupOk = true
            val t = _telemetry.value
            _quickSetupMessage.value = "Setup MCU (Simulasi): TAHAP=${t.stage.label}, PPR=${_pulserPpr.value}, GATE=${_gateDurationUs.value}µs"
            Toast.makeText(context, "Setup Simulasi: ${t.stage.label}", Toast.LENGTH_SHORT).show()
        } else {
            _quickSetupMessage.value = "CDI Belum Terhubung • Hubungkan lewat tab BLE atau nyalakan DEMO"
            Toast.makeText(context, "CDI Belum Terhubung (Offline)", Toast.LENGTH_SHORT).show()
        }
    }

    fun setPulserEdge(edge: String) { // "FALLING" or "RISING"
        if (!requireMcuOrDemo("pengaturan edge pulser")) return
        if (!checkSetupWriteSafety("Pengaturan edge pulser")) return
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,EDGE,$edge")
            appendLog("BLE Send: SETUP,EDGE,$edge")
        } else {
            _pickupEdge.value = edge
            appendLog("Pulser Edge diatur ke: $edge (Simulasi)")
        }
        Toast.makeText(context, "Pulser Edge: $edge", Toast.LENGTH_SHORT).show()
    }

    fun setPulserPpr(ppr: Int) {
        if (!requireMcuOrDemo("pengaturan PPR")) return
        if (!checkSetupWriteSafety("Pengaturan PPR")) return
        val bounded = ppr.coerceIn(1, _firmwareCapabilities.value.maxPulserPpr)
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,PPR,$bounded")
            appendLog("BLE Send: SETUP,PPR,$bounded")
        } else {
            _pulserPpr.value = bounded
            appendLog("Pulser PPR diatur ke: $bounded (Demo)")
        }
    }

    fun setGateDurationUs(us: Int) {
        if (!requireMcuOrDemo("pengaturan gate SCR")) return
        if (!checkSetupWriteSafety("Pengaturan gate SCR")) return
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,GATE_US,$us")
            appendLog("BLE Send: SETUP,GATE_US,$us")
        } else {
            _gateDurationUs.value = us
            appendLog("SCR Gate Duration: ${us}µs")
        }
    }

    fun confirmPulserPickup() {
        if (!requireMcuOrDemo("konfirmasi pickup")) return
        if (!checkSetupWriteSafety("Konfirmasi pickup")) return
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,PICKUP,CONFIRM")
            appendLog("BLE Send: SETUP,PICKUP,CONFIRM")
        } else {
            appendLog("Pulser Pick-up Dikonfirmasi (PPR=1, Gate=80µs). Lanjut ke TDC.")
            _commissionStatus.value = _commissionStatus.value.copy(stage = 2, nextAction = 3, advisoryMask = 0)
            advanceSetupStage(SetupStage.TDC.code)
        }
        Toast.makeText(context, if (bleClient.gattReady) "Konfirmasi pickup masuk antrean" else "Pickup terverifikasi di Demo", Toast.LENGTH_SHORT).show()
    }

    fun saveTdcStrobe() {
        if (!requireMcuOrDemo("simpan TDC strobo")) return
        if (!checkFlashSafety("Simpan TDC Strobo")) return
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,SAVE_TDC")
            appendLog("BLE Send: SETUP,SAVE_TDC (TDC Strobo disimpan ke Flash)")
        } else {
            appendLog("TDC Strobo disimpan ke Flash A/B. Lanjut ke TPS.")
            _commissionStatus.value = _commissionStatus.value.copy(stage = 3, nextAction = 4, advisoryMask = 0)
            advanceSetupStage(SetupStage.TPS_CAL.code)
        }
        _flashSaved.value = !bleClient.gattReady
        Toast.makeText(context, if (bleClient.gattReady) "SAVE TDC masuk antrean" else "TDC tersimpan di Demo", Toast.LENGTH_SHORT).show()
    }

    fun saveManualTdc(offsetDeg: Float) {
        if (!requireMcuOrDemo("simpan TDC manual")) return
        if (!checkFlashSafety("Simpan TDC Manual")) return
        val clamped = offsetDeg.coerceIn(-5.0f, 5.0f)
        _pulserOffsetDeg.value = clamped
        val triggerCdeg = candidateTriggerCdeg(clamped)
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,MANUAL_TDC,$triggerCdeg,CONFIRM")
            appendLog("BLE Send: SETUP,MANUAL_TDC,$triggerCdeg,CONFIRM")
        } else {
            appendLog("TDC Manual Terukur ${clamped}° BTDC disimpan tanpa strobo. Lanjut ke TPS.")
            _commissionStatus.value = _commissionStatus.value.copy(stage = 3, nextAction = 4, advisoryMask = 0)
            advanceSetupStage(SetupStage.TPS_CAL.code)
        }
        triggerEditBaseCdeg = triggerCdeg
        _pulserOffsetDeg.value = 0f
        _flashSaved.value = !bleClient.gattReady
        Toast.makeText(context, if (bleClient.gattReady) "MANUAL TDC masuk antrean" else "TDC manual tersimpan di Demo", Toast.LENGTH_SHORT).show()
    }

    fun calibrateTpsClosed() {
        if (!requireMcuOrDemo("kalibrasi TPS tertutup")) return
        if (!checkSetupWriteSafety("Kalibrasi TPS tertutup")) return
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,TPS,CLOSED")
            appendLog("BLE Send: SETUP,TPS,CLOSED (Simpan Gas Tertutup 0%)")
        } else {
            _tpsClosedAdc.value = 820
            _commissionStatus.value = _commissionStatus.value.copy(stage = 3, nextAction = 4)
            appendLog("TPS Gas Tertutup (0%) Disimpan.")
        }
        Toast.makeText(context, if (bleClient.gattReady) "TPS CLOSED masuk antrean" else "TPS CLOSED tersimpan di Demo", Toast.LENGTH_SHORT).show()
    }

    fun calibrateTpsOpen() {
        if (!requireMcuOrDemo("kalibrasi TPS terbuka")) return
        if (!checkSetupWriteSafety("Kalibrasi TPS terbuka")) return
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,TPS,OPEN")
            appendLog("BLE Send: SETUP,TPS,OPEN (Simpan Gas Penuh 100%)")
        } else {
            _tpsOpenAdc.value = 3940
            _commissionStatus.value = _commissionStatus.value.copy(stage = 4, nextAction = 5, advisoryMask = 0)
            appendLog("TPS Gas Terbuka Penuh (100%) Disimpan. Lanjut ke FIRST START.")
            advanceSetupStage(SetupStage.FIRST_START.code)
        }
        Toast.makeText(context, if (bleClient.gattReady) "TPS OPEN masuk antrean" else "TPS OPEN tersimpan di Demo", Toast.LENGTH_SHORT).show()
    }

    fun prepareFirstStartMode() {
        if (!requireMcuOrDemo("FIRST START")) return
        if (!checkSetupWriteSafety("Aktivasi FIRST START")) return
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,FIRST_START")
            appendLog("BLE Send: SETUP,FIRST_START (Mode Aman: 220V, CENTER saja, Max 10° Adv, Limiter 3.000 RPM, Otomatis simpan setelah 3 detik)")
        } else {
            appendLog("Mode FIRST START Siap (220V, CENTER saja, Limiter 3.000 RPM, Otomatis 3 detik)")
            _commissionStatus.value = _commissionStatus.value.copy(stage = 4, nextAction = 6, advisoryMask = 0)
            advanceSetupStage(SetupStage.FIRST_START.code)
        }
        Toast.makeText(context, if (bleClient.gattReady) "FIRST START aktif. Hidupkan mesin 3 detik untuk simpan otomatis." else "FIRST START aktif di Demo", Toast.LENGTH_LONG).show()
    }

    fun confirmReadyCenterOnly() {
        if (!requireMcuOrDemo("READY CENTER")) return
        val t = _telemetry.value
        if (bleClient.gattReady) {
            if (!checkSetupWriteSafety("Simpan READY CENTER")) return
            markSetupCommandPending()
            bleClient.send("SETUP,READY,CENTER")
            appendLog("BLE Send: SETUP,READY,CENTER (Mode Siap Jalan - Koil CENTER)")
        } else {
            if (t.rpm > 0) {
                Toast.makeText(context, "Matikan mesin terlebih dahulu (RPM 0)!", Toast.LENGTH_SHORT).show()
                return
            }
            val curMod = _moduleStatus.value
            _moduleStatus.value = curMod.copy(
                coreProfile = 0,
                activeMask = curMod.activeMask and HardwareModule.SIDE.bitMask.inv()
            )
            _commissionStatus.value = CommissionStatus(
                stage = 5,
                nextAction = 7,
                ready = true,
                advisoryMask = 0
            )
            _telemetry.value = _telemetry.value.copy(
                setupStage = 4,
                flags = _telemetry.value.flags or 0x20,
                outputFlags = 0x01
            )
            _demoEngineRunning.value = false
            simRpm = 0f
            simTps = 0f
            appendLog("Setup Selesai: READY - Core 1-Coil (J1.12). Disimpan Permanen di Flash & Komisi Selesai!")
            advanceSetupStage(SetupStage.READY.code)
        }
        Toast.makeText(context, if (bleClient.gattReady) "READY CENTER masuk antrean; tunggu ACK" else "READY CENTER aktif di Demo (Komisi Selesai)", Toast.LENGTH_LONG).show()
    }

    fun confirmReadyTripleSpark(sideOffsetCdeg: Int = 0) {
        if (!requireMcuOrDemo("READY dual/tiga busi")) return
        val t = _telemetry.value
        if (bleClient.gattReady) {
            if (!checkSetupWriteSafety("Simpan READY dual/tiga busi")) return
            markSetupCommandPending()
            bleClient.send("SETUP,READY,DUAL,$sideOffsetCdeg")
            appendLog("BLE Send: SETUP,READY,DUAL,$sideOffsetCdeg (Core + SIDE)")
        } else {
            if (t.rpm > 0) {
                Toast.makeText(context, "Matikan mesin terlebih dahulu (RPM 0)!", Toast.LENGTH_SHORT).show()
                return
            }
            val curMod = _moduleStatus.value
            _moduleStatus.value = curMod.copy(
                coreProfile = 2,
                installedMask = curMod.installedMask or HardwareModule.SIDE.bitMask,
                activeMask = curMod.activeMask or HardwareModule.SIDE.bitMask
            )
            _commissionStatus.value = CommissionStatus(
                stage = 5,
                nextAction = 7,
                ready = true,
                advisoryMask = 0
            )
            _telemetry.value = _telemetry.value.copy(
                setupStage = 4,
                flags = _telemetry.value.flags or 0x20,
                outputFlags = 0x03
            )
            _demoEngineRunning.value = false
            simRpm = 0f
            simTps = 0f
            appendLog("Setup Selesai: READY - Dual Coil (Core J1.12 + SIDE J1.6). Disimpan Permanen di Flash & Komisi Selesai!")
            advanceSetupStage(SetupStage.READY.code)
        }
        Toast.makeText(context, if (bleClient.gattReady) "READY Dual Coil masuk antrean; tunggu ACK" else "READY Dual Coil aktif di Demo (Komisi Selesai)", Toast.LENGTH_LONG).show()
    }

    // --- R8 Mode & Flow Controls ---
    fun setFirmwareMode(mode: FirmwareRunMode) {
        if (!requireMcuOrDemo("ganti mode")) return
        val requiredCapability = when (mode) {
            FirmwareRunMode.OEM_LEARN -> "OEM_LEARN"
            FirmwareRunMode.MANUAL -> "MANUAL"
            FirmwareRunMode.DIY -> "DIY"
        }
        if (!requireCapability(requiredCapability, "mode ${mode.name}")) return
        if (!checkSetupWriteSafety("Perubahan mode firmware")) return
        val (cPin, sPin) = if (selectedPlatform.value == McuPlatform.STM32WB55) Pair("PB3", "PB4") else Pair("GPIO16", "GPIO17")
        val mcuName = selectedPlatform.value.displayName
        _firmwareMode.value = mode
        when (mode) {
            FirmwareRunMode.OEM_LEARN -> {
                if (bleClient.gattReady) {
                    markSetupCommandPending()
                    bleClient.send("MODE,OEM_LEARN")
                    bleClient.send("GET,MODE")
                    appendLog("BLE Send: MODE,OEM_LEARN ($cPin/$sPin)")
                } else {
                    appendLog("Mode: OEM_LEARN aktif di Demo ($cPin/$sPin pada $mcuName)")
                }
                Toast.makeText(context, "Mode OEM LEARN Aktif (baca CDI OEM via $cPin/$sPin pada $mcuName)", Toast.LENGTH_SHORT).show()
            }
            FirmwareRunMode.MANUAL -> {
                if (bleClient.gattReady) {
                    markSetupCommandPending()
                    bleClient.send("MODE,MANUAL")
                    bleClient.send("GET,MODE")
                    appendLog("BLE Send: MODE,MANUAL")
                } else {
                    appendLog("Mode: MANUAL aktif di Demo ($mcuName)")
                }
                Toast.makeText(context, "Mode MANUAL Aktif (Strobo/TDC darurat)", Toast.LENGTH_SHORT).show()
            }
            FirmwareRunMode.DIY -> {
                if (!_isOemUnpluggedConfirmed.value) {
                    Toast.makeText(context, "Peringatan: Konfirmasi OEM_UNPLUGGED dahulu sebelum aktifkan DIY!", Toast.LENGTH_LONG).show()
                    return
                }
                if (bleClient.gattReady) {
                    markSetupCommandPending()
                    bleClient.send("MODE,DIY,OEM_UNPLUGGED")
                    bleClient.send("GET,MODE")
                    appendLog("BLE Send: MODE,DIY,OEM_UNPLUGGED")
                } else {
                    appendLog("Mode: DIY aktif di Demo (OEM terlepas, $mcuName mandiri)")
                }
                Toast.makeText(context, "Mode DIY Aktif (CDI mandiri)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startOemLearn() {
        if (!requireMcuOrDemo("start OEM Learn") ||
            !requireCapability("OEM_LEARN", "OEM Learn") ||
            !checkSetupWriteSafety("Mulai OEM Learn")) return
        val (cPin, sPin) = if (selectedPlatform.value == McuPlatform.STM32WB55) Pair("PB3", "PB4") else Pair("GPIO16", "GPIO17")
        val mcuName = selectedPlatform.value.displayName
        // Catatan Keselamatan: OEM Learn mengecualikan blokir RPM > 0 karena mesin sengaja dihidupkan dengan CDI OEM!
        _isOemLearning.value = true
        _firmwareMode.value = FirmwareRunMode.OEM_LEARN
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("MODE,OEM_LEARN")
            bleClient.send("LEARN,START")
            bleClient.send("GET,MODE")
            bleClient.send("GET,LEARN")
            startOemLearnPolling()
            appendLog("BLE Send: MODE,OEM_LEARN & LEARN,START ($cPin/$sPin)")
        } else {
            _demoEngineRunning.value = true
            appendLog("OEM Learn Dimulai: membaca pulsa $cPin/$sPin ($mcuName)...")
            startDemoOemPulseGenerator()
        }
        Toast.makeText(context, "OEM Learn Dimulai: Hidupkan mesin dengan CDI OEM ($cPin/$sPin)", Toast.LENGTH_SHORT).show()
    }

    fun stopOemLearn() {
        if (!requireMcuOrDemo("stop OEM Learn") ||
            !requireCapability("OEM_LEARN", "OEM Learn")) return
        if (!_isSimulationMode.value && !canWrite()) {
            Toast.makeText(context, "Binding/telemetri tidak siap untuk menghentikan OEM Learn.", Toast.LENGTH_LONG).show()
            return
        }
        val mcuName = selectedPlatform.value.displayName
        _isOemLearning.value = false
        demoOemPulseJob?.cancel()
        oemLearnPollJob?.cancel()
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("LEARN,STOP")
            bleClient.send("GET,LEARN")
            appendLog("BLE Send: LEARN,STOP (Simpan Map OEM ke Flash $mcuName)")
        } else {
            _flashSaved.value = true
            _demoEngineRunning.value = false
            simRpm = 0f
            appendLog("OEM Learn Dihentikan: timing Center & Side tersimpan di flash $mcuName.")
        }
        Toast.makeText(context, "OEM Learn Selesai: Matikan mesin & cabut modul PC817 serta soket CDI OEM", Toast.LENGTH_LONG).show()
    }

    private fun startDemoOemPulseGenerator() {
        demoOemPulseJob?.cancel()
        demoOemPulseJob = viewModelScope.launch {
            while (isActive && _isOemLearning.value) {
                delay(200)
                _oemCenterPulses.value = (_oemCenterPulses.value + 2).coerceAtMost(500)
                if (_oemCenterPulses.value >= 4) {
                    _oemSideSamples.value = (_oemSideSamples.value + 1).coerceAtMost(250)
                }
            }
        }
    }

    fun confirmOemUnplugged() {
        if (!checkSetupWriteSafety("Aktivasi DIY/OEM_UNPLUGGED")) return
        _isOemUnpluggedConfirmed.value = true
        _firmwareMode.value = FirmwareRunMode.DIY
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("MODE,DIY,OEM_UNPLUGGED")
            bleClient.send("GET,MODE")
            appendLog("BLE Send: MODE,DIY,OEM_UNPLUGGED")
        } else {
            appendLog("Konfirmasi OEM Unplugged Diterima. Mode DIY Aktif.")
            advanceSetupStage(SetupStage.FIRST_START.code)
        }
        Toast.makeText(context, "OEM Unplugged Dikonfirmasi • Mode DIY Aktif", Toast.LENGTH_SHORT).show()
    }

    fun setHvVoltageMode(proMode: Boolean) {
        if (!requireMcuOrDemo("pengaturan tegangan")) return
        if (!checkSetupWriteSafety("Perubahan profil HV")) return
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("FEATURE,PRO,${if (proMode) "ON" else "OFF"}")
            val currentSlot = if (proMode) 3 else _selectedMapSlot.value.coerceIn(0, 2)
            bleClient.send("LOAD,$currentSlot")
            bleClient.send("GET,MODE")
            bleClient.send("GET,META")
            bleClient.send("GET,STATUS")
            appendLog("BLE Send R8: FEATURE,PRO,${if (proMode) "ON" else "OFF"} -> LOAD,$currentSlot")
        } else {
            _isProVoltageConfigured.value = proMode
            _targetHvVoltage.value = if (proMode) CdiProtocol.VOLTAGE_PRO else CdiProtocol.VOLTAGE_NORMAL
            appendLog("Tegangan HV diubah ke ${_targetHvVoltage.value}V (${if (proMode) "PRO" else "NORMAL"})")
        }
        Toast.makeText(context, "Perintah mode ${if (proMode) "PRO 345 V" else "NORMAL 285 V"} masuk antrean", Toast.LENGTH_SHORT).show()
    }

    fun checkOtaPreflightSafety(): String? {
        val t = _telemetry.value
        if (!_isConnected.value || !bleClient.gattReady) return "CDI belum terhubung penuh."
        if (_sessionPhase.value != SessionPhase.READY_FULL || !canWrite()) return "Binding atau sinkronisasi belum siap."
        if (!isTelemetryFresh()) return "Telemetri basi; kondisi mesin dan HV tidak dapat dipastikan."
        if (t.rpm > 0) return "Mesin masih berputar (${t.rpm} RPM)! Matikan mesin (RPM = 0)."
        if (t.armed) return "Output pengapian masih diizinkan. Nonaktifkan output sebelum OTA."
        if (t.hvEnabled) return "Charger HV masih aktif. Nonaktifkan charger sebelum OTA."
        if (t.hvCenter >= 30 || t.hvSide >= 30) return "Tegangan HV masih tinggi (Center: ${t.hvCenter}V, Side: ${t.hvSide}V)! Tunggu hingga < 30V."
        return null
    }

    fun startOtaUpload(bytes: ByteArray, fileName: String) {
        val safetyErr = checkOtaPreflightSafety()
        if (safetyErr != null) {
            Toast.makeText(context, "Gagal Mulai OTA: $safetyErr", Toast.LENGTH_LONG).show()
            appendLog("OTA Ditolak: $safetyErr")
            return
        }

        if ("OTA" !in _mcuCapabilities.value) {
            Toast.makeText(context, "Firmware tidak melaporkan capability OTA", Toast.LENGTH_LONG).show()
            return
        }

        val version = Regex("""(?:^|[^0-9])(20[0-9]{6})(?:[^0-9]|$)""")
            .find(fileName)?.groupValues?.getOrNull(1)?.toLongOrNull()
        if (version == null) {
            Toast.makeText(
                context,
                "Nama file OTA wajib memuat build 8 digit, contoh ignitra_esp32_20260924.bin",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val platform = when (_firmwareVersionInfo.value.platform.trim().uppercase()) {
            "ESP32" -> McuPlatform.ESP32_WROOM
            "STM32", "STM32WB55" -> McuPlatform.STM32WB55
            else -> {
                Toast.makeText(context, "Platform firmware belum teridentifikasi dari VERSION.", Toast.LENGTH_LONG).show()
                return
            }
        }

        appendLog("Memulai OTA $platform build $version: $fileName (${bytes.size} byte)")
        bleClient.startOta(bytes, platform, version)
    }

    fun cancelOtaUpload() {
        bleClient.cancelOta()
        appendLog("OTA dibatalkan oleh pengguna")
        Toast.makeText(context, "OTA Dibatalkan", Toast.LENGTH_SHORT).show()
    }

    fun resetSetupWorkflow() {
        if (!requireMcuOrDemo("reset setup")) return
        if (!checkSetupWriteSafety("Reset setup")) return
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,RESET,CONFIRM")
            appendLog("BLE Send: SETUP,RESET,CONFIRM (Kembali ke Tahap BARU)")
        } else {
            appendLog("Reset Setup CDI ke Tahap Awal (BARU)")
            advanceSetupStage(SetupStage.BARU.code)
        }
        Toast.makeText(context, if (bleClient.gattReady) "RESET setup masuk antrean" else "Setup Demo kembali ke BARU", Toast.LENGTH_SHORT).show()
    }

    fun setFanMode(mode: String) {
        if (!requireMcuOrDemo("pengaturan kipas") ||
            !requireCapability("FAN", "kontrol kipas") ||
            !checkSetupWriteSafety("Pengaturan kipas")) return
        val normalized = ThermalFanPolicy.normalize(mode, _fanOnCdeg.value, _fanOffCdeg.value)
        _fanMode.value = normalized.mode
        _fanOnCdeg.value = normalized.onCdeg
        _fanOffCdeg.value = normalized.offCdeg
        if (bleClient.gattReady) {
            bleClient.send(if (_firmwareCapabilities.value.protocolVersion >= 5)
                "SET,FAN,${normalized.mode},${normalized.onCdeg / 10},${normalized.offCdeg / 10}"
            else "SETUP,FAN,${normalized.mode}")
            bleClient.send("GET,TEMP")
        }
        appendLog("Fan ${normalized.mode}: ON ${normalized.onCdeg / 100f}°C / OFF ${normalized.offCdeg / 100f}°C")
    }

    fun setFanThresholds(onCdeg: Int, offCdeg: Int) {
        if (!requireCapability("FAN", "kontrol kipas")) return
        val normalized = ThermalFanPolicy.normalize(_fanMode.value, onCdeg, offCdeg)
        _fanOnCdeg.value = normalized.onCdeg
        _fanOffCdeg.value = normalized.offCdeg
        if (bleClient.gattReady) {
            if (!checkSetupWriteSafety("Simpan ambang fan")) return
            bleClient.send("SET,FAN,${normalized.mode},${normalized.onCdeg / 10},${normalized.offCdeg / 10}")
            bleClient.send("GET,TEMP")
        }
    }

    fun setDemoTemperature(celsius: Float) {
        if (_isSimulationMode.value) {
            _telemetry.value = _telemetry.value.copy(tempCdeg = (celsius * 100f).roundToInt())
        }
    }

    fun saveTemperatureCalibration(points: List<Pair<Int, Float>>) {
        if (points.size != 3 || !requireMcuOrDemo("kalibrasi suhu") ||
            !checkSetupWriteSafety("Kalibrasi suhu")) return
        if (bleClient.gattReady) {
            val payload = points.joinToString(",") {
                "${it.first.coerceIn(1, 4094)},${(it.second * 10f).roundToInt()}"
            }
            bleClient.send("TEMP,CAL,$payload")
            bleClient.send("GET,TEMP")
        } else appendLog("Kalibrasi suhu 3 titik disimpan pada Demo")
    }

    fun setEngineProfile(profile: EngineProfile) {
        if (!requireMcuOrDemo("profil mesin") ||
            !requireCapability("PROFILE", "profil mesin") ||
            !checkSetupWriteSafety("Profil mesin")) return
        val safe = profile.clamped(_firmwareCapabilities.value)
        _engineProfile.value = safe
        _pulserPpr.value = safe.pulserPpr
        if (bleClient.gattReady) {
            bleClient.send("SET,PROFILE,${safe.name},${safe.rpmMin},${safe.rpmMax}," +
                "${(safe.advanceMinDeg * 10).roundToInt()},${(safe.advanceMaxDeg * 10).roundToInt()}," +
                "${safe.pulserPpr},${(safe.triggerAngleDeg * 10).roundToInt()}")
            bleClient.send("GET,PROFILE")
        }
        appendLog("Profil aktif: ${safe.name}; PPR ${safe.pulserPpr}; trigger ${safe.triggerAngleDeg}°")
    }

    fun beginDynoTune() {
        if (!requireMcuOrDemo("live remap dyno") ||
            !requireCapability("DYNO", "live remap dyno") ||
            !checkSetupWriteSafety("Mulai dyno")) return
        _dynoActive.value = true
        _dynoTrimDeg.value = 0f
        if (bleClient.gattReady) bleClient.send("DYNO,BEGIN")
    }

    fun setDynoTrim(degrees: Float) {
        if (!_dynoActive.value || !requireCapability("DYNO", "live remap dyno")) return
        if (!_isSimulationMode.value && !canWrite()) return
        val trim = degrees.coerceIn(-10f, 10f)
        _dynoTrimDeg.value = trim
        if (bleClient.gattReady) bleClient.send("DYNO,TRIM,${(trim * 10f).roundToInt()}")
    }

    fun finishDynoTune(commit: Boolean) {
        if (!_dynoActive.value || !requireCapability("DYNO", "live remap dyno")) return
        if (bleClient.gattReady) {
            if (!checkSetupWriteSafety(if (commit) "Commit dyno" else "Batalkan dyno")) return
            bleClient.send(if (commit) "DYNO,COMMIT" else "DYNO,ABORT")
        }
        _dynoActive.value = false
        _dynoTrimDeg.value = 0f
        appendLog(if (commit) "Live trim dyno dikomit ke map" else "Live trim dyno dibatalkan")
    }

    fun sendRawCommand(cmd: String) {
        if (cmd.isBlank()) return
        if (bleClient.gattReady) {
            bleClient.send(cmd.trim())
            appendLog("TX: ${cmd.trim()}")
        } else {
            appendLog("CMD (Offline): ${cmd.trim()}")
            if (cmd.startsWith("PING")) {
                appendLog("RX: PONG,CDI-R7-OK*CRC")
            }
        }
    }

    private fun candidateTriggerCdeg(offsetDeg: Float): Int {
        val value = triggerEditBaseCdeg + (offsetDeg * 100f).roundToInt()
        return ((value % 36000) + 36000) % 36000
    }

    private fun requireMcuOrDemo(action: String): Boolean {
        if (bleClient.gattReady || _isSimulationMode.value) return true
        appendLog("DITOLAK offline: $action memerlukan koneksi CDI atau mode Demo")
        Toast.makeText(context, "Hubungkan CDI untuk $action (atau aktifkan Demo)", Toast.LENGTH_LONG).show()
        return false
    }

    private fun appendLog(line: String) {
        val list = _terminalLogs.value.toMutableList()
        if (list.size > 80) list.removeAt(0)
        list.add(line)
        _terminalLogs.value = list
    }

    // BleCdiClient.Listener implementation
    override fun onState(text: String, connected: Boolean) {
        _connectionStatus.value = text
        _isConnected.value = connected
        if (connected) {
            setupSyncedThisConnection = false
            syncPongSeen = false
            syncVersionSeen = false
            syncIdentitySeen = false
            syncCapsSeen = false
            syncStatusSeen = false
            syncSetupSeen = false
            _mcuCapabilities.value = emptySet()
            _isSimulationMode.value = false
            _isRevving.value = false
            _demoThrottleSlider.value = 0f
            simRpm = 0f
            simTps = 0f
            engineSound.stop()
            resetBleStatistics()
            _pickupDiagnosticMessage.value = "Menghubungkan ke MCU • menunggu verifikasi pulser loopback..."
            telemetryWatchdogJob?.cancel()
            telemetryWatchdogJob = viewModelScope.launch {
                while (_isConnected.value) {
                    delay(300)
                    val now = SystemClock.elapsedRealtime()
                    val silence = if (lastTelemetryPacketAtMs > 0L) now - lastTelemetryPacketAtMs else 0L

                    when {
                        _telemetryPacketCount.value == 0L && silence >= 4_000L -> {
                            _packetRateHz.value = 0
                            _isTelemetryStreaming.value = false
                            _telemetryRxMessage.value =
                                "MENUNGGU FRAME • Telemetry 1001 belum notify (MCU selftest/loopback aktif)"
                        }
                        lastTelemetryPacketAtMs > 0L && silence >= 2_000L -> {
                            // 2000ms tanpa paket telemetri 1001 -> Mesin mati / Telemetry Standby
                            if (_isTelemetryStreaming.value || _packetRateHz.value > 0) {
                                _packetRateHz.value = 0
                                _isTelemetryStreaming.value = false
                                rxSamples.clear()
                                val current = _telemetry.value
                                if (current.rpm > 0 || current.outputFlags != 0 || current.limiter != 0) {
                                    _telemetry.value = current.copy(
                                        rpm = 0,
                                        outputFlags = 0,
                                        limiter = 0,
                                        pickupQuality = 0
                                    )
                                    engineSound.stop()
                                    appendLog("Watchdog UI: 2000ms tanpa paket -> RPM & Indikator 0 (Standby)")
                                }
                            }
                            _telemetryRxMessage.value =
                                "TELEMETRY STANDBY • mesin mati / tidak ada frame baru (${silence / 1000}s)"
                        }
                    }
                }
            }
            _sessionPhase.value = SessionPhase.SYNCING
            viewModelScope.launch {
                delay(80)
                val handshakeQueries = listOf(
                    "PING", "GET,INFO", "GET,VERSION", "GET,IDENTITY",
                    "GET,CAPS", "GET,HARDWARE", "GET,MODULES", "GET,COMMISSION",
                    "GET,SETUP", "GET,STATUS", "GET,META", "GET,PROFILE",
                    "GET,TEMP", "GET,ADC", "GET,MODE", "GET,LEARN", "GET,OTA"
                )
                for (q in handshakeQueries) {
                    if (!_isConnected.value) break
                    bleClient.send(q)
                    delay(40)
                }
                delay(300)
                evaluateSessionPhaseAfterSync()
                delay(5_000)
                if (_isConnected.value && _sessionPhase.value == SessionPhase.SYNCING) {
                    _sessionPhase.value = SessionPhase.DEGRADED
                    appendLog("Sesi DEGRADED: respons wajib handshake belum lengkap.")
                }
            }
        } else {
            _sessionPhase.value = SessionPhase.DISCONNECTED
            telemetryWatchdogJob?.cancel()
            _isTelemetryStreaming.value = false
            _packetRateHz.value = 0
            rxSamples.clear()
            engineSound.stop()
            val currentT = _telemetry.value
            _telemetry.value = currentT.copy(
                rpm = 0,
                outputFlags = 0,
                limiter = 0,
                pickupQuality = 0
            )
            setupSyncedThisConnection = false
            _mcuCapabilities.value = emptySet()
            _firmwareIdentity.value = FirmwareIdentityInfo()
            _firmwareVersionInfo.value = FirmwareVersionInfo()
            _moduleStatus.value = ModuleStatus.defaultCore()
            _bindingRecord.value = null
            oemLearnPollJob?.cancel()
            resetBleStatistics()
            clearSetupCommandPending()
            if (_quickSetupPreflightBusy.value) {
                failQuickSetupPreflight("Koneksi BLE terputus.")
            }
        }
        appendLog("BLE: $text")
    }

    override fun onTelemetry(value: Telemetry) {
        if (!_isSimulationMode.value) {
            _isTelemetryStreaming.value = true
        }
        val current = _telemetry.value

        var targetStage = current.setupStage
        // Aplikasi menunggu status READY nyata dari flash MCU (value.ready == true atau MCU setupStage >= 4),
        // bukan berhenti atau menganggap selesai hanya karena bukti FIRST START (firstStartSeconds >= 3) tercatat di RAM.
        if (value.ready || _firmwareSetupStage.value >= 4) {
            targetStage = SetupStage.READY.code
            _firmwareSetupStage.value = 4 // CDI_R7_STAGE_READY; wizard READY adalah halaman 5.
            _quickSetupUnlockedStage.value = maxOf(_quickSetupUnlockedStage.value, targetStage)
            if (_quickSetupPage.value == SetupStage.FIRST_START.code) {
                _quickSetupPage.value = targetStage
            }
        }

        val merged = value.copy(
            setupStage = targetStage
        )

        _telemetry.value = merged
        _selectedMapSlot.value = merged.slot.coerceIn(0, 3)
        _strobeActive.value = merged.strobeEnabled

        if (merged.pickupQuality < 10 && merged.rpm == 0) {
            _pickupDiagnosticMessage.value = "Belum ada pulsa loopback terdeteksi (cek kabel jumper & sinyal pickup)"
        } else {
            _pickupDiagnosticMessage.value = "Pulser Terdeteksi: ${merged.pickupQuality}/100 • ${merged.rpm} RPM"
        }

        if (!merged.strobeEnabled && _pulserOffsetDeg.value == 0f) {
            triggerEditBaseCdeg = merged.triggerCdeg.coerceIn(0, 35999)
        }

        context.getSharedPreferences(
            "cdi_r7_prefs",
            Context.MODE_PRIVATE
        ).edit()
            .putInt("setup_stage", merged.setupStage)
            .apply()

        if (merged.rpm > 100 && _soundEnabled.value) {
            engineSound.update(merged)
        } else {
            engineSound.stop()
        }
    }

    override fun onRawPacket(bytes: ByteArray) {
        _rawPacket.value = bytes
        _telemetryPacketCount.value += 1L

        val now = SystemClock.elapsedRealtime()
        lastTelemetryPacketAtMs = now

        val valid = CdiProtocol.telemetry(
            packet = bytes,
            previous = _telemetry.value
        ) != null

        if (valid && !_isSimulationMode.value) {
            _isTelemetryStreaming.value = true
        }

        _telemetryRxMessage.value = if (valid) {
            "AKTIF • frame #${_telemetryPacketCount.value} dari Telemetry 1001"
        } else {
            "FRAME DITOLAK • panjang/versi/header/CRC tidak valid (${bytes.size} byte)"
        }

        rxSamples.addLast(RxSample(now, valid))

        while (
            rxSamples.isNotEmpty() &&
            now - rxSamples.first().timestampMs > RX_WINDOW_MS
        ) {
            rxSamples.removeFirst()
        }

        val total = rxSamples.size
        val validCount = rxSamples.count { it.valid }

        _crcValidPercent.value =
            if (total == 0) 100f
            else validCount * 100f / total

        if (rxSamples.size >= 4) {
            val duration = rxSamples.last().timestampMs - rxSamples.first().timestampMs
            if (duration >= 300L) {
                _packetRateHz.value = ((rxSamples.size - 1) * 1000f / duration).roundToInt()
            }
        }
    }

    private fun evaluateSessionPhaseAfterSync() {
        if (!_isConnected.value) {
            _sessionPhase.value = SessionPhase.DISCONNECTED
            return
        }
        val currentSerial = _firmwareIdentity.value.serial
        if (currentSerial == "UNAVAILABLE" || currentSerial == "IGT-ESP32-UNKNOWN") {
            _sessionPhase.value = SessionPhase.READY_READ_ONLY
            appendLog("Sesi: Serial [$currentSerial] -> Mode Terbatas (READY_READ_ONLY)")
        } else {
            val bound = _bindingRecord.value
            if (bound != null && bound.serial == currentSerial) {
                _sessionPhase.value = SessionPhase.READY_FULL
                appendLog("Sesi: Serial [$currentSerial] cocok dengan binding lokal -> Siap Penuh (READY_FULL)")
            } else {
                _sessionPhase.value = SessionPhase.NEEDS_BINDING
                appendLog("Sesi: Serial [$currentSerial] belum di-binding ke ponsel ini -> Menunggu Binding (NEEDS_BINDING)")
            }
        }
    }

    private fun refreshSetupAfterAck() {
        viewModelScope.launch {
            delay(80)
            bleClient.send("GET,COMMISSION")
            delay(25)
            bleClient.send("GET,SETUP")
            delay(25)
            bleClient.send("GET,MODULES")
            delay(25)
            bleClient.send("GET,STATUS")
            delay(25)
            bleClient.send("GET,TEMP")
        }
    }

    override fun onResponse(value: String) {
        appendLog("RX: $value")
        if (value.contains("loopback", ignoreCase = true) ||
            value.contains("selftest", ignoreCase = true) ||
            value.contains("pickup", ignoreCase = true)) {
            _pickupDiagnosticMessage.value = value.trim()
        }
        val f = value.split(',')
        when (f.firstOrNull()) {
            "PONG" -> {
                syncPongSeen = true
                evaluateSessionPhaseAfterSync()
                preflightPingOk = true
                updateQuickSetupPreflightProgress()
                _quickSetupMessage.value = "PONG diterima dari MCU: $value (Komunikasi OK)"
                Toast.makeText(context, "MCU PONG: Komunikasi Aktif!", Toast.LENGTH_SHORT).show()
            }
            "CAPS" -> {
                val parsed = FirmwareCapabilities.parse(f)
                syncCapsSeen = true
                _firmwareCapabilities.value = parsed
                _mcuCapabilities.value = parsed.features
                rebuildLoadAxis(parsed.maxLoadPoints)
                _selectedMapSlot.value = _selectedMapSlot.value.coerceIn(0, parsed.mapSlots - 1)
                _softRevLimiterRpm.value = _softRevLimiterRpm.value.coerceIn(parsed.rpmMin, parsed.rpmMax)
                appendLog("MCU CAPS v${parsed.protocolVersion}: ${parsed.rpmMin}-${parsed.rpmMax} RPM, " +
                    "${parsed.advanceMinDeg}..${parsed.advanceMaxDeg}°, ${parsed.maxRpmPoints}x${parsed.maxLoadPoints}")
                evaluateSessionPhaseAfterSync()
            }
            "TEMP" -> CdiProtocol.parseTemp(value)?.let { tempStatus ->
                _firmwareTempStatus.value = tempStatus
                _fanMode.value = tempStatus.fanMode
                _fanOnCdeg.value = tempStatus.onX10 * 10
                _fanOffCdeg.value = tempStatus.offX10 * 10
            }
            "HARDWARE", "HW" -> {
                val hw = CdiProtocol.parseHardware(value)
                appendLog("MCU Hardware: ${hw.joinToString(", ")}")
            }
            "PROFILE" -> EngineProfile.parse(f)?.let {
                _engineProfile.value = it
                _pulserPpr.value = it.pulserPpr
                _softRevLimiterRpm.value = _softRevLimiterRpm.value.coerceIn(it.rpmMin, it.rpmMax)
            }
            "VERSION" -> CdiProtocol.parseVersion(value)?.let {
                _firmwareVersionInfo.value = it
                syncVersionSeen = true
                when (it.platform.trim().uppercase()) {
                    "ESP32" -> _selectedPlatform.value = McuPlatform.ESP32_WROOM
                    "STM32", "STM32WB55" -> _selectedPlatform.value = McuPlatform.STM32WB55
                }
                appendLog("Firmware: ${it.displayLabel}")
                evaluateSessionPhaseAfterSync()
            }
            "IDENTITY" -> CdiProtocol.parseIdentity(value)?.let {
                _firmwareIdentity.value = it
                syncIdentitySeen = true
                _bindingRecord.value = loadBindingForSerial(it.serial)
                appendLog("Device Identity: ${it.serial} [${it.bindingPolicy}]")
                if (_isConnected.value && _sessionPhase.value != SessionPhase.SYNCING) {
                    evaluateSessionPhaseAfterSync()
                }
            }
            "MODULES" -> CdiProtocol.parseModules(value)?.let {
                _moduleStatus.value = it
                appendLog("Hardware Modules: profile=${it.profileLabel}")
            }
            "COMMISSION" -> CdiProtocol.parseCommission(value)?.let {
                _commissionStatus.value = it
                appendLog("Commission Status: Stage ${it.stage}, Ready=${it.ready}")
            }
            "ADC" -> CdiProtocol.parseAdc(value)?.let {
                _adcReadings.value = it
            }
            "STATUS" -> if (f.size >= 9) {
                syncStatusSeen = true
                val slot = f[5].toIntOrNull()?.coerceIn(0, _firmwareCapabilities.value.mapSlots - 1) ?: _selectedMapSlot.value
                _selectedMapSlot.value = slot
                _telemetry.value = _telemetry.value.copy(
                    rpm = f[1].toIntOrNull() ?: _telemetry.value.rpm,
                    tps = f[2].toIntOrNull() ?: _telemetry.value.tps,
                    hvCenter = f[3].toIntOrNull() ?: _telemetry.value.hvCenter,
                    hvSide = f[4].toIntOrNull() ?: _telemetry.value.hvSide,
                    slot = slot
                )
                f[8].toIntOrNull()?.let { _isProVoltageConfigured.value = it == 1 }
                preflightStatusOk = true
                updateQuickSetupPreflightProgress()
                evaluateSessionPhaseAfterSync()
            }
            "META" -> if (f.size >= 10) {
                _limiterType.value = if (f[3].toIntOrNull() == 1) "HARD" else "SOFT"
                _softRevLimiterRpm.value = f[4].toIntOrNull()?.coerceIn(_firmwareCapabilities.value.rpmMin, _firmwareCapabilities.value.rpmMax) ?: _softRevLimiterRpm.value
                _softBandRpm.value = f[5].toIntOrNull()?.coerceIn(50, 3000) ?: _softBandRpm.value
                _targetHvVoltage.value = f[6].toIntOrNull()
                    ?.coerceIn(CdiProtocol.VOLTAGE_FIRST_START, CdiProtocol.VOLTAGE_PRO)
                    ?: _targetHvVoltage.value
                val rpmCount = f[8].toIntOrNull() ?: 0
                val tpsCount = f[9].toIntOrNull() ?: 0
                val caps = _firmwareCapabilities.value
                _activeMapRpmCount.value =
                    if (rpmCount in 2..caps.maxRpmPoints) rpmCount else 0
                _activeMapTpsCount.value =
                    if (tpsCount in 2..caps.maxLoadPoints) tpsCount else 0
            }
            "MODE" -> CdiProtocol.firmwareMode(value)?.let { status ->
                _firmwareMode.value = status.mode
                _isOemUnpluggedConfirmed.value = status.diyUnplugged
                _isProVoltageConfigured.value = status.proEnabled
                if (status.mode != FirmwareRunMode.OEM_LEARN) {
                    _isOemLearning.value = false
                    oemLearnPollJob?.cancel()
                }
            }
            "CELL" -> if (f.size >= 4) {
                val ti = f[1].toIntOrNull()
                val ri = f[2].toIntOrNull()
                val cdeg = f[3].toIntOrNull()
                if (ti != null && ri != null && cdeg != null &&
                    ti in 0 until mapReadbackTpsCount && ri in 0 until mapReadbackRpmCount
                ) {
                    mapReadback[ti to ri] = cdeg / 100f
                    if (mapReadback.size == mapReadbackRpmCount * mapReadbackTpsCount) {
                        val caps = _firmwareCapabilities.value
                        val currentAxis = _customAdvancePoints.value.map { it.rpm }
                        val axis = if (currentAxis.size == mapReadbackRpmCount) {
                            currentAxis
                        } else {
                            List(mapReadbackRpmCount) { index ->
                                caps.rpmMin + ((caps.rpmMax - caps.rpmMin).toLong() * index /
                                    (mapReadbackRpmCount - 1).coerceAtLeast(1)).toInt()
                            }
                        }
                        rebuildLoadAxis(mapReadbackTpsCount)
                        customMapRows.clear()
                        repeat(mapReadbackTpsCount) { loadIndex ->
                            customMapRows[loadIndex] = axis.mapIndexed { rpmIndex, rpm ->
                                CustomAdvancePoint(rpm, mapReadback[loadIndex to rpmIndex] ?: 0f)
                            }
                        }
                        _selectedCustomLoadIndex.value = 0
                        _customAdvancePoints.value =
                            customMapRows[0]?.map { it.copy() } ?: emptyList()
                        appendLog("Map readback lengkap: ${mapReadbackRpmCount}x${mapReadbackTpsCount}")
                    }
                }
            }
            "SETUP" -> if (f.size >= 14) {
                syncSetupSeen = true
                val firmwareStage = f[1].toIntOrNull()?.coerceIn(0, 4)
                    ?: _firmwareSetupStage.value

                val edgeCode = f[2].toIntOrNull() ?: 0
                val trigger = f[3].toIntOrNull()
                    ?: _telemetry.value.triggerCdeg

                val sideOffset = f[4].toIntOrNull() ?: 0
                val ppr = f[5].toIntOrNull()?.coerceIn(1, _firmwareCapabilities.value.maxPulserPpr) ?: 1
                val gateUs = f[6].toIntOrNull()?.coerceIn(40, 150) ?: 80
                val tpsClosed = f[7].toIntOrNull() ?: 0
                val tpsOpen = f[8].toIntOrNull() ?: 0
                val firstStartHv = f[9].toIntOrNull() ?: 220

                val center = f[10].toIntOrNull() == 1
                val side = f[11].toIntOrNull() == 1
                val fanCode = f[12].toIntOrNull() ?: 2
                val quality = f[13].toIntOrNull() ?: 0
                val wizardStage = CdiProtocol.wizardStageFromFirmware(
                    firmwareStage = firmwareStage,
                    tpsClosedAdc = tpsClosed,
                    tpsOpenAdc = tpsOpen
                )

                _firmwareSetupStage.value = firmwareStage
                if (!setupSyncedThisConnection) {
                    // Firmware adalah sumber kebenaran setelah koneksi baru;
                    // jangan biarkan cache aplikasi lama memalsukan READY.
                    _quickSetupPage.value = wizardStage
                    _quickSetupUnlockedStage.value = wizardStage
                    setupSyncedThisConnection = true
                } else {
                    _quickSetupUnlockedStage.value = maxOf(
                        _quickSetupUnlockedStage.value,
                        wizardStage
                    )
                    if (wizardStage > _quickSetupPage.value) {
                        _quickSetupPage.value = wizardStage
                    }
                }

                _pickupEdge.value =
                    if (edgeCode == 1) "RISING" else "FALLING"

                _sideOffsetCdeg.value = sideOffset
                _pulserPpr.value = ppr
                _gateDurationUs.value = gateUs
                _tpsClosedAdc.value = tpsClosed
                _tpsOpenAdc.value = tpsOpen
                _firstStartHv.value = firstStartHv

                _fanMode.value = when (fanCode) {
                    0 -> "OFF"
                    1 -> "ON"
                    else -> "AUTO"
                }

                _telemetry.value = _telemetry.value.copy(
                    setupStage = wizardStage,
                    triggerCdeg = trigger,
                    outputFlags =
                        (if (center) 1 else 0) or
                        (if (side) 2 else 0) or
                        (if (_strobeActive.value) 4 else 0) or
                        (if (fanCode != 0) 8 else 0),
                    pickupQuality = quality
                )

                context.getSharedPreferences(
                    "cdi_r7_prefs",
                    Context.MODE_PRIVATE
                ).edit()
                    .putInt("setup_stage", wizardStage)
                    .apply()

                if (!_strobeActive.value && _pulserOffsetDeg.value == 0f) {
                    triggerEditBaseCdeg = trigger.coerceIn(0, 35999)
                }

                appendLog(
                    "SETUP sync: MCU=$firmwareStage wizard=$wizardStage edge=${_pickupEdge.value} " +
                        "PPR=$ppr gate=${gateUs}us fan=${_fanMode.value}"
                )
                preflightSetupOk = true
                updateQuickSetupPreflightProgress()
                evaluateSessionPhaseAfterSync()
            }
            "LEARN" -> CdiProtocol.oemLearnStatus(value)?.let { status ->
                _isOemLearning.value = status.state == OemLearnState.ACTIVE
                _oemLearnCoverage.value = status.coveragePercent
                _oemCenterPulses.value = status.acceptedPulses
                _oemRejectedPulses.value = status.rejectedPulses
                _oemSideSamples.value = status.sideSamples
                _sideOffsetCdeg.value = status.sideOffsetCdeg
                if (_isOemLearning.value) startOemLearnPolling() else oemLearnPollJob?.cancel()
            }
            "ACK" -> {
                clearSetupCommandPending()
                val operation = f.getOrNull(1).orEmpty()
                if (operation.startsWith("PONG")) {
                    syncPongSeen = true
                    evaluateSessionPhaseAfterSync()
                    preflightPingOk = true
                    updateQuickSetupPreflightProgress()
                    _quickSetupMessage.value = "MCU ACK: $operation (Komunikasi Aktif)"
                    Toast.makeText(context, "MCU PONG: Komunikasi Aktif!", Toast.LENGTH_SHORT).show()
                }
                if (operation == "TDC_SAVED" || operation == "TDC_MANUAL_SAVED")
                    _flashSaved.value = true
                if (operation !in setOf("LIVE", "OFFSET") && !operation.startsWith("PONG"))
                    Toast.makeText(context, "MCU ACK: $operation", Toast.LENGTH_SHORT).show()

                val setupChangingOperations = setOf(
                    "INSTALL_CORE",
                    "INSTALL_DUAL",
                    "MODULE_SET",
                    "PICKUP_OK",
                    "EDGE",
                    "EDGE_REQUIRES_PICKUP_TDC",
                    "PPR",
                    "PPR_REQUIRES_TDC",
                    "GATE_US",
                    "STROBE",
                    "TDC_SAVED",
                    "TDC_MANUAL_SAVED",
                    "TPS",
                    "TPS_CLOSED",
                    "TPS_OPEN",
                    "FIRST_START",
                    "READY_CENTER",
                    "READY_DUAL",
                    "READY_THREE",
                    "FAN",
                    "SETUP_RESET"
                )

                when {
                    operation == "MODE" -> {
                        val modeParam = f.getOrNull(2)?.trim()?.uppercase()
                        if (modeParam != null) {
                            val parsedMode = when (modeParam) {
                                "0", "MANUAL" -> FirmwareRunMode.MANUAL
                                "1", "OEM_LEARN", "LEARN" -> FirmwareRunMode.OEM_LEARN
                                "2", "DIY" -> FirmwareRunMode.DIY
                                else -> modeParam.toIntOrNull()?.let { FirmwareRunMode.fromFirmwareCode(it) }
                            }
                            if (parsedMode != null) {
                                _firmwareMode.value = parsedMode
                            }
                        }
                        bleClient.send("GET,MODE")
                        bleClient.send("GET,SETUP")
                        bleClient.send("GET,STATUS")
                    }
                    operation == "LEARN_STARTED_PASSIVE" || operation == "LEARN_START" -> {
                        _isOemLearning.value = true
                        _firmwareMode.value = FirmwareRunMode.OEM_LEARN
                        startOemLearnPolling()
                    }
                    operation == "LEARN_ABORTED" || operation == "LEARN_STOP" -> {
                        _isOemLearning.value = false
                        oemLearnPollJob?.cancel()
                        bleClient.send("GET,LEARN")
                    }
                    operation == "LEARN_SAVED" ||
                        operation == "LEARN_SAVED_CENTER" ||
                        operation == "LEARN_SAVED_CENTER_SIDE" ||
                        operation == "OEM_MAP_SAVED" ||
                        operation == "OEM_MAP_LOCK" -> {
                        _isOemLearning.value = false
                        _flashSaved.value = true
                        oemLearnPollJob?.cancel()
                        bleClient.send("GET,LEARN")
                        bleClient.send("GET,SETUP")
                        bleClient.send("GET,META")
                        bleClient.send("GET,STATUS")
                        Toast.makeText(context, "Map OEM Tersimpan Permanen di Flash!", Toast.LENGTH_SHORT).show()
                    }
                    operation == "OEM_UNPLUGGED" || operation == "MODE_DIY" -> {
                        _isOemUnpluggedConfirmed.value = true
                        _firmwareMode.value = FirmwareRunMode.DIY
                        bleClient.send("GET,MODE")
                        bleClient.send("GET,SETUP")
                        advanceSetupStage(SetupStage.FIRST_START.code)
                        Toast.makeText(context, "Soket OEM Dilepas • Mode DIY Mandiri Aktif", Toast.LENGTH_SHORT).show()
                    }
                    operation == "PRO_ON" -> {
                        _isProVoltageConfigured.value = true
                        _targetHvVoltage.value = CdiProtocol.VOLTAGE_PRO
                        bleClient.send("GET,MODE")
                        bleClient.send("GET,META")
                        bleClient.send("GET,STATUS")
                    }
                    operation == "PRO_OFF" -> {
                        _isProVoltageConfigured.value = false
                        _targetHvVoltage.value = CdiProtocol.VOLTAGE_NORMAL
                        bleClient.send("GET,MODE")
                        bleClient.send("GET,META")
                        bleClient.send("GET,STATUS")
                    }
                    operation in setupChangingOperations -> {
                        if (operation == "SETUP_RESET") {
                            _quickSetupPage.value = SetupStage.BARU.code
                            _quickSetupUnlockedStage.value = SetupStage.BARU.code
                            _firmwareSetupStage.value = 0
                        }
                        refreshSetupAfterAck()
                    }

                    operation == "LIVE" ||
                        operation == "OFFSET" ||
                        operation.startsWith("PONG") -> Unit

                    operation.startsWith("LOAD") ||
                        operation.startsWith("SAVE") ||
                        operation == "LIMIT" -> {
                        bleClient.send("GET,META")
                        bleClient.send("GET,STATUS")
                    }

                    else -> Unit
                }
            }
            "ERR" -> {
                clearSetupCommandPending()
                Toast.makeText(context, "MCU menolak: ${f.drop(1).joinToString(",")}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun refreshMapFromFirmware(): String? {
        if (!bleClient.gattReady) return "CDI belum terhubung."
        val rpmCount = _activeMapRpmCount.value
        val tpsCount = _activeMapTpsCount.value
        if (rpmCount < 2 || tpsCount < 2) return "META map belum valid."
        if (bleClient.isBusy.value) return "Antrean BLE masih sibuk."
        requestMapReadback(rpmCount, tpsCount)
        appendLog("Readback map dimulai: $rpmCount×$tpsCount cell")
        return null
    }

    private fun requestMapReadback(rpmCount: Int, tpsCount: Int) {
        if (!bleClient.gattReady) return
        mapReadback.clear()
        mapReadbackRpmCount = rpmCount
        mapReadbackTpsCount = tpsCount
        repeat(tpsCount) { loadIndex ->
            repeat(rpmCount) { rpmIndex ->
                bleClient.send("GET,CELL,$loadIndex,$rpmIndex")
            }
        }
    }

    private fun startOemLearnPolling() {
        if (oemLearnPollJob?.isActive == true) return
        oemLearnPollJob = viewModelScope.launch {
            while (isActive && _isOemLearning.value && bleClient.gattReady) {
                delay(500)
                bleClient.send("GET,LEARN")
            }
        }
    }

    // Internal simulation loop for Hold to Rev & Demo Mode
    private fun startSimulationEngine() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch(Dispatchers.Default) {
            var seq = 0
            while (isActive) {
                delay(50) // 20 Hz

                val revving = _isRevving.value
                val isSim = _isSimulationMode.value
                val realBleReady = bleClient.gattReady
                val slider = _demoThrottleSlider.value

                // Pemisahan Ketat (Strict Mode Separation: Demo vs Real):
                // Jika aplikasi TIDAK sedang berada dalam Mode Simulasi (_isSimulationMode == false),
                // thread simulasi ini DILARANG KERAS menyentuh atau memodifikasi _telemetry.value!
                // Seluruh telemetri di mode nyata murni datang dari paket Bluetooth GATT dan dikawal oleh Watchdog UI (500ms).
                if (!isSim) {
                    if (engineSound.isPlaying && _telemetry.value.rpm < 100) {
                        engineSound.stop()
                    }
                    continue
                }

                val demoStarterOn = _demoEngineRunning.value
                val shouldSimulate = demoStarterOn || revving || slider > 0.01f || simRpm > 50f || simTps > 0.01f
                if (shouldSimulate) {
                    // Update simulated throttle & RPM
                    if (revving) {
                        simTps = (simTps + 0.20f).coerceAtMost(1.0f)
                        val targetRpm = _softRevLimiterRpm.value + 800f
                        simRpm += (targetRpm - simRpm) * 0.22f
                        if (simRpm >= _softRevLimiterRpm.value) {
                            // Limiter stutter flutter
                            val flutter = ((sin(seq * 1.5) * 350f)).toFloat()
                            simRpm = (_softRevLimiterRpm.value - 150f) + flutter
                        }
                    } else if (slider > 0.01f) {
                        // Slider held at specific throttle/RPM
                        simTps += (slider - simTps) * 0.35f
                        val maxTarget = _softRevLimiterRpm.value.toFloat().coerceAtLeast(10000f)
                        val targetRpm = 1420f + slider * (maxTarget - 1200f)
                        simRpm += (targetRpm - simRpm) * 0.28f
                        if (simRpm >= _softRevLimiterRpm.value) {
                            val flutter = ((sin(seq * 1.5) * 350f)).toFloat()
                            simRpm = (_softRevLimiterRpm.value - 150f) + flutter
                        }
                    } else if (demoStarterOn) {
                        // Engine running at idle: throttle decays smoothly to idle position (0.02f)
                        simTps += (0.02f - simTps) * 0.25f
                        val idleTarget = 1420f + (sin(seq * 0.2) * 40f).toFloat()
                        simRpm += (idleTarget - simRpm) * 0.25f
                        if (kotlin.math.abs(simRpm - idleTarget) < 25f) {
                            simRpm = idleTarget
                        }
                    } else {
                        // Engine stopped: throttle decays, RPM snaps to 0
                        simTps = (simTps - 0.25f).coerceAtLeast(0.0f)
                        simRpm = (simRpm - 250f).coerceAtLeast(0f)
                    }

                    if (!demoStarterOn && !revving && slider <= 0.01f && simRpm <= 30f) {
                        simRpm = 0f
                        simTps = 0f
                        engineSound.stop()
                    }

                    simPhase += 0.05f

                    // Calculate advance angle based on active map
                    val activeMap = mapPresets[_selectedMapSlot.value]
                    val baseAdvance = when {
                        simRpm < 2000 -> 12f + (simRpm - 1000f) * 0.005f
                        simRpm < 6000 -> 17f + (simRpm - 2000f) * 0.0035f
                        simRpm < 9000 -> 31f + (simRpm - 6000f) * 0.0015f
                        else -> (activeMap.peakAdvance - ((simRpm - 9000f) * 0.004f)).coerceAtLeast(10f)
                    }
                    val finalAdvance = baseAdvance + _pulserOffsetDeg.value

                    val limiterState = when {
                        simRpm >= _softRevLimiterRpm.value + 300 -> 2 // Hard
                        simRpm >= _softRevLimiterRpm.value -> 1       // Soft
                        else -> 0
                    }

                    // R8 OEM Learn simulation
                    if (_isOemLearning.value) {
                        _oemCenterPulses.value = (_oemCenterPulses.value + 1).coerceAtMost(50)
                        if (_oemCenterPulses.value >= 5) {
                            _oemSideSamples.value = (_oemSideSamples.value + 1).coerceAtMost(30)
                        }
                    }

                    // R8 Automatic FIRST START logic in simulation
                    var fsSeconds = _telemetry.value.firstStartSeconds
                    if (_telemetry.value.setupStage == SetupStage.FIRST_START.code) {
                        if (simRpm > 1000f) {
                            if (seq % 20 == 0) { // ~1s
                                fsSeconds = (fsSeconds + 1).coerceAtMost(10)
                            }
                        } else if (simRpm <= 50f && fsSeconds >= 3) {
                            // Engine stopped after 3s -> automatically READY!
                            advanceSetupStage(SetupStage.READY.code)
                            appendLog("R8 Demo: FIRST START stabil >= 3 detik & mesin berhenti -> Otomatis READY!")
                        }
                    }

                    seq = (seq + 1) and 0xFFFF
                    val isRunning = simRpm > 50f
                    val targetHv = if (_telemetry.value.setupStage == SetupStage.FIRST_START.code) {
                        CdiProtocol.VOLTAGE_FIRST_START // 220V for First Start
                    } else if (_isProVoltageConfigured.value) {
                        345
                    } else {
                        285
                    }
                    val currentHvCenter = if (isRunning) {
                        targetHv + (sin(seq * 0.3) * 4).toInt()
                    } else {
                        0 // Fully discharged safely when engine is stopped!
                    }
                    val dualActive = _moduleStatus.value.isInstalled(HardwareModule.DUAL_COIL) && _moduleStatus.value.isActive(HardwareModule.DUAL_COIL)
                    val thermalInstalled = _moduleStatus.value.isInstalled(HardwareModule.THERMAL_FAN)

                    val currentHvSide = if (isRunning && dualActive && _telemetry.value.setupStage != SetupStage.FIRST_START.code) {
                        targetHv + (sin(seq * 0.25) * 5).toInt()
                    } else {
                        0 // Side coil 0V if dual coil module is not installed, inactive, during FIRST_START or stopped
                    }

                    val simTempCdeg = if (thermalInstalled) {
                        (7500 + (simTps * 1500).toInt() + (simRpm / 150f * 100).toInt()).coerceIn(3200, 11500)
                    } else {
                        Short.MIN_VALUE.toInt() // Sensor tidak terpasang
                    }
                    val fanRelayOn = thermalInstalled && simTempCdeg >= _fanOnCdeg.value

                    val simTelemetry = Telemetry(
                        sequence = seq,
                        rpm = simRpm.toInt().coerceIn(0, _firmwareCapabilities.value.rpmMax),
                        tps = (simTps * 1000).toInt(),
                        advanceCdeg = (finalAdvance * 100).toInt(),
                        batteryCv = if (isRunning) 1380 + (sin(seq * 0.1) * 20).toInt() else 1260,
                        hvCenter = currentHvCenter,
                        hvSide = currentHvSide,
                        tempCdeg = simTempCdeg,
                        slot = _selectedMapSlot.value,
                        limiter = limiterState,
                        flags = 0x21 or (if (_flashSaved.value) 0x08 else 0x00),
                        faults = 0,
                        setupStage = _telemetry.value.setupStage,
                        outputFlags = (if (currentHvCenter > 0) 0x01 else 0x00) or
                            (if (currentHvSide > 0) 0x02 else 0x00) or
                            (if (_strobeActive.value && _moduleStatus.value.isActive(HardwareModule.AUX)) 0x04 else 0x00) or
                            (if (fanRelayOn) 0x08 else 0x00),
                        triggerCdeg = candidateTriggerCdeg(_pulserOffsetDeg.value),
                        pickupQuality = if (isRunning) 99 else 0,
                        firstStartSeconds = fsSeconds
                    )

                    _telemetry.value = simTelemetry
                    _rawPacket.value = CdiProtocol.packetFromTelemetry(
                        simTelemetry,
                        if (seq and 1 == 0) CdiProtocol.KIND_CORE else CdiProtocol.KIND_DIAGNOSTIC
                    )
                    if (simRpm > 100f && _soundEnabled.value) {
                        engineSound.update(simTelemetry)
                    } else {
                        engineSound.stop()
                    }
                } else {
                    engineSound.stop()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        telemetryWatchdogJob?.cancel()
        simulationJob?.cancel()
        oemLearnPollJob?.cancel()
        demoOemPulseJob?.cancel()
        bleClient.release()
        engineSound.release()
    }
}
