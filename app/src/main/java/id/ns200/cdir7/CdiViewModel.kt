package id.ns200.cdir7

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ScreenTab(val title: String, val badge: String) {
    TACHO("Tacho", "CLUSTER"),
    MAPS("Maps", "KURVA"),
    WIRING("Wiring", "WORKSHOP"),
    SETUP("Setup", "KOMISI"),
    SUARA("Suara", "AUDIO"),
    BLE("BLE", "DIAG")
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
            tempCdeg = 0,
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
    private val RX_WINDOW_MS = 5_000L
    private var telemetryWatchdogJob: Job? = null

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

    private var preflightPingOk = false
    private var preflightStatusOk = false
    private var preflightSetupOk = false
    private var preflightTimeoutJob: Job? = null
    private var lastTelemetryPacketAtMs = 0L
    private var setupSyncedThisConnection = false

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

    val otaState: StateFlow<OtaState> = bleClient.otaState
    val connectedDeviceName: StateFlow<String?> = bleClient.connectedDeviceName

    private val cdiPrefs = context.getSharedPreferences("cdi_r8_prefs", Context.MODE_PRIVATE)

    private val _selectedPlatform = MutableStateFlow(
        McuPlatform.fromId(cdiPrefs.getString("mcu_platform", McuPlatform.STM32WB55.id))
    )
    val selectedPlatform: StateFlow<McuPlatform> = _selectedPlatform.asStateFlow()

    fun setMcuPlatform(platform: McuPlatform) {
        _selectedPlatform.value = platform
        cdiPrefs.edit().putString("mcu_platform", platform.id).apply()
        appendLog("Platform Hardware aktif dialihkan ke: ${platform.displayName} (${platform.architecture})")
    }

    // Maps State - 4 Flash Memory Slots (ECO, STREET, RAIN, PRO) with two flash pages & CRC32
    val mapPresets = listOf(
        MapSlotData(
            slot = 0,
            name = "Slot 1: ECO",
            description = "Kurva linear responsif untuk jalan raya, efisiensi BBM dan suhu dingin. Konservatif anchor 5° BTDC/1500 RPM sampai 32°/9800 RPM. Target HV 285V.",
            revLimit = 9800,
            peakAdvance = 32.0f,
            curvePoints = listOf(
                1500 to 5f, 2500 to 14f, 4500 to 24f, 6500 to 30f, 8500 to 32f, 9800 to 28f, 10500 to 10f
            )
        ),
        MapSlotData(
            slot = 1,
            name = "Slot 2: STREET",
            description = "Map standar performa jalanan agresif. Respons gas instan dengan advance maksimum 34° BTDC. Target HV 285V.",
            revLimit = 10500,
            peakAdvance = 34.0f,
            curvePoints = listOf(
                1500 to 6f, 2500 to 16f, 4500 to 26f, 7000 to 33f, 8800 to 34f, 10500 to 30f, 11000 to 12f
            )
        ),
        MapSlotData(
            slot = 2,
            name = "Slot 3: RAIN",
            description = "Kurva aman untuk cuaca basah dan bensin oktan rendah (Low-RON). Mencegah slip dan knocking/detonasi. Target HV 285V.",
            revLimit = 9500,
            peakAdvance = 28.0f,
            curvePoints = listOf(
                1500 to 5f, 2500 to 11f, 4500 to 19f, 6500 to 25f, 8000 to 28f, 9500 to 22f, 10000 to 10f
            )
        ),
        MapSlotData(
            slot = 3,
            name = "Slot 4: PRO",
            description = "Map kompetisi tingkat tinggi 16x8 matrix / 345V PRO. Dipilih melalui konfigurasi tersimpan (tanpa jumper fisik JP_PRO). Advance maksimum 36° BTDC.",
            revLimit = 11500,
            peakAdvance = 36.0f,
            curvePoints = listOf(
                1500 to 8f, 2500 to 18f, 5000 to 29f, 7500 to 35f, 9500 to 36f, 10800 to 34f, 11800 to 14f
            )
        )
    )

    private val _selectedMapSlot = MutableStateFlow(0)
    val selectedMapSlot: StateFlow<Int> = _selectedMapSlot.asStateFlow()

    // Custom Advance Map Points (Default based on technical document anchor: 5° @ 1500 RPM to 34° @ 8500 RPM)
    private val _customAdvancePoints = MutableStateFlow(
        listOf(
            CustomAdvancePoint(500, 0.0f), CustomAdvancePoint(1000, 2.0f),
            CustomAdvancePoint(1500, 5.0f), CustomAdvancePoint(2500, 10.0f),
            CustomAdvancePoint(4000, 18.0f), CustomAdvancePoint(6000, 25.0f),
            CustomAdvancePoint(8000, 31.0f), CustomAdvancePoint(10000, 36.0f)
        )
    )
    val customAdvancePoints: StateFlow<List<CustomAdvancePoint>> = _customAdvancePoints.asStateFlow()

    private val _softRevLimiterRpm = MutableStateFlow(9800)
    val softRevLimiterRpm: StateFlow<Int> = _softRevLimiterRpm.asStateFlow()

    private val _hardRevLimiterRpm = MutableStateFlow(10300)
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
            "NS200 CDI R8 System Initialized.",
            "MoTeC / AIM Telemetry Protocol Engine Ready.",
            "Firmware R8: Dual-Core OEM Learn & Pro 16x8 Engine.",
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
    private val mapReadback = mutableMapOf<Int, Float>()
    private var mapReadbackExpected = 0
    private var mapReadbackTpsRow = 0

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
        _softRevLimiterRpm.value = prefs.getInt("rev_limiter", 9800)
        val savedStage = prefs.getInt("setup_stage", SetupStage.BARU.code)
            .coerceIn(SetupStage.BARU.code, SetupStage.READY.code)
        _quickSetupPage.value = savedStage
        _quickSetupUnlockedStage.value = savedStage
        _telemetry.value = _telemetry.value.copy(
            setupStage = savedStage,
            flags = if (savedStage == SetupStage.READY.code) (_telemetry.value.flags or 0x28) else _telemetry.value.flags,
            outputFlags = if (savedStage == SetupStage.READY.code) 0x03 else if (savedStage == SetupStage.FIRST_START.code) 0x01 else 0x00
        )

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
            appendLog("Scanning for NS200-CDI-R7 BLE...")
            bleClient.connect()
        }
    }

    fun hasBlePermissions() = bleClient.hasPermissions()

    fun startBleScan() {
        _isSimulationMode.value = false
        _isRevving.value = false
        _demoThrottleSlider.value = 0f
        simRpm = 0f
        simTps = 0f
        engineSound.stop()
        appendLog("Scanning BLE devices nearby...")
        bleClient.startScan()
    }

    fun stopBleScan() {
        bleClient.stopScanInternal()
        appendLog("BLE scan dihentikan.")
    }

    @SuppressLint("MissingPermission")
    fun connectBleDevice(device: BluetoothDevice) {
        _isSimulationMode.value = false
        _isRevving.value = false
        _demoThrottleSlider.value = 0f
        simRpm = 0f
        simTps = 0f
        engineSound.stop()
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
            // Enforce hard ceiling of 36.0° BTDC as per documentation
            val bounded = rounded.coerceIn(0.0f, 36.0f)
            current[index] = current[index].copy(advanceDeg = bounded)
            _customAdvancePoints.value = current
            appendLog("Map Custom: ${current[index].rpm} RPM diubah ke ${bounded}° BTDC")
        }
    }

    fun loadCustomPreset(presetKey: String) {
        val presetIndex = listOf("ECO", "STREET", "RAIN", "PRO").indexOf(presetKey)
        if (presetIndex >= 0) {
            val source = mapPresets[presetIndex].curvePoints.sortedBy { it.first }
            val axis = if (presetKey == "PRO")
                listOf(500, 750, 1000, 1500, 2000, 2500, 3000, 4000,
                    5000, 6000, 7000, 8000, 9000, 10000, 11000, 11500)
            else listOf(500, 1000, 1500, 2500, 4000, 6000, 8000, 10000)
            fun sample(rpm: Int): Float {
                if (rpm <= source.first().first) return source.first().second
                if (rpm >= source.last().first) return source.last().second
                val right = source.indexOfFirst { it.first >= rpm }
                val a = source[right - 1]; val b = source[right]
                return a.second + (b.second - a.second) * (rpm - a.first) / (b.first - a.first).toFloat()
            }
            _customAdvancePoints.value = axis.map { CustomAdvancePoint(it, sample(it)) }
            _selectedMapSlot.value = presetIndex
            appendLog("Preset $presetKey dimuat pada grid firmware ${axis.size} titik")
        }
    }

    fun saveCustomMapToMcu(): String? {
        val t = _telemetry.value
        // Safety verification as per page 14: save/load only when RPM=0 and HV < 30V
        if (t.rpm > 0) {
            return "PERINGATAN KESELAMATAN: Mesin terdeteksi hidup (${t.rpm} RPM)! Simpan Flash MCU hanya diizinkan saat mesin mati (RPM = 0) untuk mencegah crash interrupt TIM2."
        }
        if ((t.hvCenter >= 30 || t.hvSide >= 30) && _isConnected.value) {
            return "PERINGATAN KESELAMATAN: Tegangan tinggi kapasitor CDI masih aktif (CENTER: ${t.hvCenter}V, SIDE: ${t.hvSide}V)! Tunggu hingga tegangan < 30 V sebelum menulis flash."
        }

        if (!bleClient.gattReady) return "CDI belum terhubung. Map tidak diklaim tersimpan ke MCU."
        val currentPoints = _customAdvancePoints.value
        val slot = _selectedMapSlot.value
        val isProSlot = slot == 3

        // Firmware R8 Target Grid Specs:
        // Slots 0-2 (Standard): 8 RPM points x 4 TPS rows
        // Slot 3 (PRO): 16 RPM points x 8 TPS rows
        val targetAxis = if (isProSlot) {
            listOf(500, 750, 1000, 1500, 2000, 2500, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 11000, 11500)
        } else {
            listOf(500, 1000, 1500, 2500, 4000, 6000, 8000, 10000)
        }
        val tpsRows = if (isProSlot) 8 else 4

        // Deterministic Resampling onto target firmware RPM axis
        val sortedInput = currentPoints.sortedBy { it.rpm }
        val resampledAdvanceList = targetAxis.map { targetRpm ->
            val advance = when {
                sortedInput.isEmpty() -> 0f
                targetRpm <= sortedInput.first().rpm -> sortedInput.first().advanceDeg
                targetRpm >= sortedInput.last().rpm -> sortedInput.last().advanceDeg
                else -> {
                    val rightIdx = sortedInput.indexOfFirst { it.rpm >= targetRpm }
                    if (rightIdx <= 0) sortedInput.first().advanceDeg
                    else {
                        val pA = sortedInput[rightIdx - 1]
                        val pB = sortedInput[rightIdx]
                        val span = (pB.rpm - pA.rpm).toFloat()
                        if (span <= 0f) pA.advanceDeg
                        else pA.advanceDeg + (pB.advanceDeg - pA.advanceDeg) * ((targetRpm - pA.rpm) / span)
                    }
                }
            }
            advance.coerceIn(0.0f, 36.0f)
        }

        val pointsPayload = currentPoints.joinToString(";") { "${it.rpm}:${(it.advanceDeg * 10).toInt()}" }

        bleClient.send("FEATURE,PRO,${if (isProSlot) "ON" else "OFF"}")
        bleClient.send("LOAD,$slot")
        repeat(tpsRows) { tpsIndex ->
            resampledAdvanceList.forEachIndexed { rpmIndex, advanceDeg ->
                val centiDeg = (advanceDeg * 100f).roundToInt()
                bleClient.send("LIVE,$tpsIndex,$rpmIndex,$centiDeg")
            }
        }
        bleClient.send("SAVE,$slot")
        bleClient.send("GET,META")
        appendLog("BLE Queue R8: FEATURE PRO ${if (isProSlot) "ON" else "OFF"} -> LOAD -> ${tpsRows * targetAxis.size} sel LIVE -> SAVE -> readback")

        context.getSharedPreferences("cdi_r7_prefs", Context.MODE_PRIVATE).edit()
            .putString("custom_map_points", pointsPayload)
            .apply()

        return null
    }

    fun calibrateTpsMin() {
        if (!requireMcuOrDemo("kalibrasi TPS")) return
        val t = _telemetry.value
        if (t.rpm > 0) {
            Toast.makeText(context, "Kalibrasi TPS hanya saat mesin mati (RPM=0)!", Toast.LENGTH_SHORT).show()
            return
        }
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
        val t = _telemetry.value
        if (t.rpm > 0) {
            Toast.makeText(context, "Kalibrasi TPS hanya saat mesin mati (RPM=0)!", Toast.LENGTH_SHORT).show()
            return
        }
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
        val edgeStr = if (isRising) "RISING" else "FALLING"
        if (bleClient.gattReady) {
            bleClient.send("SETUP,EDGE,$edgeStr")
            appendLog("BLE Send: SETUP,EDGE,$edgeStr")
        } else {
            appendLog("Polaritas Pulser Edge diubah: $edgeStr")
        }
    }

    fun selectMapSlot(slot: Int) {
        val bounded = slot.coerceIn(0, 3)
        val preset = mapPresets[bounded]
        if (bleClient.gattReady) {
            val t = _telemetry.value
            if (t.rpm != 0 || t.hvCenter >= 30 || t.hvSide >= 30) {
                Toast.makeText(context, "LOAD ditolak: mesin harus mati dan HV < 30 V", Toast.LENGTH_LONG).show()
                return
            }
            bleClient.send("FEATURE,PRO,${if (bounded == 3) "ON" else "OFF"}")
            bleClient.send("LOAD,$bounded")
            bleClient.send("GET,MODE")
            appendLog("BLE Send R8: FEATURE PRO ${if (bounded == 3) "ON" else "OFF"} -> LOAD,$bounded (${preset.name})")
        } else if (_isSimulationMode.value) {
            _selectedMapSlot.value = bounded
            _softRevLimiterRpm.value = preset.revLimit
            appendLog("Memori Slot $bounded aktif: ${preset.name}")
        } else {
            Toast.makeText(context, "Hubungkan CDI untuk memilih slot", Toast.LENGTH_SHORT).show()
        }
    }

    fun setSoftRevLimiter(rpm: Int) {
        _softRevLimiterRpm.value = rpm.coerceIn(3000, 11500)
    }

    fun setSoftBand(band: Int) {
        _softBandRpm.value = band.coerceIn(100, 1000)
    }

    fun setLimiterType(type: String) {
        _limiterType.value = if (type.equals("HARD", true)) "HARD" else "SOFT"
    }

    fun syncCurveToBle() {
        val slot = _selectedMapSlot.value
        val rpm = _softRevLimiterRpm.value
        val band = _softBandRpm.value

        if (bleClient.gattReady) {
            val t = _telemetry.value
            if (t.rpm != 0 || t.hvCenter >= 30 || t.hvSide >= 30) {
                Toast.makeText(context, "SYNC ditolak: mesin harus mati dan HV < 30 V", Toast.LENGTH_LONG).show()
                return
            }
            bleClient.send("FEATURE,PRO,${if (slot == 3) "ON" else "OFF"}")
            bleClient.send("LOAD,$slot")
            bleClient.send("LIMIT,${_limiterType.value},$rpm,$band")
            bleClient.send("SAVE,$slot")
            bleClient.send("GET,MODE")
            appendLog("BLE Sync R8: FEATURE PRO ${if (slot == 3) "ON" else "OFF"} -> LOAD,$slot -> LIMIT,${_limiterType.value},$rpm,$band -> SAVE,$slot")
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
                "HV belum aman: CENTER ${t.hvCenter} V, SIDE ${t.hvSide} V. Lepas JP_HV dan tunggu <30 V."
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
            appendLog("BLE Send: SETUP,STROBE,${if (active) "ON" else "OFF"} (PB9)")
        } else {
            appendLog("Strobo LED PB9 ${if (active) "AKTIF (basis ${triggerEditBaseCdeg / 100f}°)" else "NONAKTIF"}")
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

    fun checkFlashSafety(action: String): Boolean {
        val t = _telemetry.value
        if (t.rpm > 0) {
            val msg = "DITOLAK: Operasi flash $action dilarang saat mesin berputar (${t.rpm} RPM)!"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            appendLog("SAFETY: $msg")
            return false
        }
        if ((t.hvCenter >= 30 || t.hvSide >= 30) && _isConnected.value) {
            val msg = "DITOLAK: Operasi flash $action ditolak! Kapasitor HV masih aktif (CENTER: ${t.hvCenter}V, SIDE: ${t.hvSide}V). Tunggu HV < 30V!"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            appendLog("SAFETY: $msg")
            return false
        }
        return true
    }

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

    fun requestSetupState() {
        if (bleClient.gattReady) {
            bleClient.send("GET,SETUP")
            appendLog("BLE Send: GET,SETUP")
        }
    }

    fun setPulserEdge(edge: String) { // "FALLING" or "RISING"
        if (!requireMcuOrDemo("pengaturan edge pulser")) return
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
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,PPR,$ppr")
            appendLog("BLE Send: SETUP,PPR,$ppr")
        } else {
            _pulserPpr.value = ppr
            appendLog("Pulser PPR diatur ke: $ppr")
        }
    }

    fun setGateDurationUs(us: Int) {
        if (!requireMcuOrDemo("pengaturan gate SCR")) return
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
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,PICKUP,CONFIRM")
            appendLog("BLE Send: SETUP,PICKUP,CONFIRM")
        } else {
            appendLog("Pulser Pick-up Dikonfirmasi (PPR=1, Gate=80µs). Lanjut ke TDC.")
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
            advanceSetupStage(SetupStage.TPS_CAL.code)
        }
        triggerEditBaseCdeg = triggerCdeg
        _pulserOffsetDeg.value = 0f
        _flashSaved.value = !bleClient.gattReady
        Toast.makeText(context, if (bleClient.gattReady) "MANUAL TDC masuk antrean" else "TDC manual tersimpan di Demo", Toast.LENGTH_SHORT).show()
    }

    fun calibrateTpsClosed() {
        if (!requireMcuOrDemo("kalibrasi TPS tertutup")) return
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,TPS,CLOSED")
            appendLog("BLE Send: SETUP,TPS,CLOSED (Simpan Gas Tertutup 0%)")
        } else {
            appendLog("TPS Gas Tertutup (0%) Disimpan.")
        }
        Toast.makeText(context, if (bleClient.gattReady) "TPS CLOSED masuk antrean" else "TPS CLOSED tersimpan di Demo", Toast.LENGTH_SHORT).show()
    }

    fun calibrateTpsOpen() {
        if (!requireMcuOrDemo("kalibrasi TPS terbuka")) return
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,TPS,OPEN")
            appendLog("BLE Send: SETUP,TPS,OPEN (Simpan Gas Penuh 100%)")
        } else {
            appendLog("TPS Gas Terbuka Penuh (100%) Disimpan. Lanjut ke FIRST START.")
            advanceSetupStage(SetupStage.FIRST_START.code)
        }
        Toast.makeText(context, if (bleClient.gattReady) "TPS OPEN masuk antrean" else "TPS OPEN tersimpan di Demo", Toast.LENGTH_SHORT).show()
    }

    fun prepareFirstStartMode() {
        if (!requireMcuOrDemo("FIRST START")) return
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,FIRST_START")
            appendLog("BLE Send: SETUP,FIRST_START (Mode Aman: 220V, CENTER saja, Max 10° Adv, Limiter 3.000 RPM, Otomatis simpan setelah 3 detik)")
        } else {
            appendLog("Mode FIRST START Siap (220V, CENTER saja, Limiter 3.000 RPM, Otomatis 3 detik)")
            advanceSetupStage(SetupStage.FIRST_START.code)
        }
        Toast.makeText(context, if (bleClient.gattReady) "FIRST START aktif. Hidupkan mesin 3 detik untuk simpan otomatis." else "FIRST START aktif di Demo", Toast.LENGTH_LONG).show()
    }

    fun confirmReadyCenterOnly() {
        if (!requireMcuOrDemo("READY CENTER")) return
        val t = _telemetry.value
        if (bleClient.gattReady) {
            if (t.hvCenter >= 30 || t.hvSide >= 30) {
                Toast.makeText(context, "PERINGATAN: Pastikan HV < 30V sebelum simpan!", Toast.LENGTH_LONG).show()
                return
            }
            markSetupCommandPending()
            bleClient.send("SETUP,READY,CENTER")
            appendLog("BLE Send: SETUP,READY,CENTER (Mode Siap Jalan - Koil CENTER)")
        } else {
            if (t.rpm > 0) {
                Toast.makeText(context, "Matikan mesin terlebih dahulu (RPM 0)!", Toast.LENGTH_SHORT).show()
                return
            }
            _demoEngineRunning.value = true
            simRpm = 1420f
            simTps = 0.02f
            appendLog("Setup Selesai: READY - CENTER Saja. Disimpan Permanen di Flash. Mesin menyala idle ~1.420 RPM siap test ride!")
            advanceSetupStage(SetupStage.READY.code)
        }
        Toast.makeText(context, if (bleClient.gattReady) "READY CENTER masuk antrean; tunggu ACK" else "READY CENTER aktif di Demo (Mesin Idle 1.420 RPM)", Toast.LENGTH_LONG).show()
    }

    fun confirmReadyTripleSpark(sideOffsetCdeg: Int = 0) {
        if (!requireMcuOrDemo("READY tiga busi")) return
        val t = _telemetry.value
        if (bleClient.gattReady) {
            if (t.hvCenter >= 30 || t.hvSide >= 30) {
                Toast.makeText(context, "PERINGATAN: Pastikan HV < 30V sebelum simpan!", Toast.LENGTH_LONG).show()
                return
            }
            markSetupCommandPending()
            bleClient.send("SETUP,READY,THREE,$sideOffsetCdeg")
            appendLog("BLE Send: SETUP,READY,THREE,$sideOffsetCdeg (Mode Triple Spark Terkalibrasi)")
        } else {
            if (t.rpm > 0) {
                Toast.makeText(context, "Matikan mesin terlebih dahulu (RPM 0)!", Toast.LENGTH_SHORT).show()
                return
            }
            _demoEngineRunning.value = true
            simRpm = 1420f
            simTps = 0.02f
            appendLog("Setup Selesai: READY - 3 Busi (Triple Spark DTS-i). Offset SIDE: ${sideOffsetCdeg/100f}°. Mesin menyala idle ~1.420 RPM siap test ride!")
            advanceSetupStage(SetupStage.READY.code)
        }
        Toast.makeText(context, if (bleClient.gattReady) "READY tiga busi masuk antrean; tunggu ACK" else "READY tiga busi aktif di Demo (Mesin Idle 1.420 RPM)", Toast.LENGTH_LONG).show()
    }

    // --- R8 Mode & Flow Controls ---
    fun setFirmwareMode(mode: FirmwareRunMode) {
        if (!requireMcuOrDemo("ganti mode")) return
        val (cPin, sPin) = if (selectedPlatform.value == McuPlatform.STM32WB55) Pair("PB3", "PB4") else Pair("GPIO16", "GPIO17")
        val mcuName = selectedPlatform.value.displayName
        when (mode) {
            FirmwareRunMode.OEM_LEARN -> {
                if (bleClient.gattReady) {
                    markSetupCommandPending()
                    bleClient.send("MODE,OEM_LEARN")
                    bleClient.send("GET,MODE")
                    appendLog("BLE Send: MODE,OEM_LEARN ($cPin/$sPin)")
                } else {
                    _firmwareMode.value = mode
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
                    _firmwareMode.value = mode
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
                    _firmwareMode.value = mode
                    appendLog("Mode: DIY aktif di Demo (OEM terlepas, $mcuName mandiri)")
                }
                Toast.makeText(context, "Mode DIY Aktif (CDI mandiri)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startOemLearn() {
        if (!requireMcuOrDemo("start OEM Learn")) return
        val t = _telemetry.value
        val (cPin, sPin) = if (selectedPlatform.value == McuPlatform.STM32WB55) Pair("PB3", "PB4") else Pair("GPIO16", "GPIO17")
        val mcuName = selectedPlatform.value.displayName
        if (bleClient.gattReady && (t.rpm != 0 || t.hvCenter >= 30 || t.hvSide >= 30)) {
            Toast.makeText(context, "Firmware R8 mensyaratkan RPM 0 dan HV <30 V saat mulai OEM Learn", Toast.LENGTH_LONG).show()
            return
        }
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("MODE,OEM_LEARN")
            bleClient.send("LEARN,START")
            bleClient.send("GET,MODE")
            bleClient.send("GET,LEARN")
            appendLog("BLE Send: MODE,OEM_LEARN & LEARN,START ($cPin/$sPin)")
        } else {
            _isOemLearning.value = true
            _firmwareMode.value = FirmwareRunMode.OEM_LEARN
            _demoEngineRunning.value = true // Mesin hidup via CDI OEM menghasilkan pulsa ke optocoupler
            appendLog("OEM Learn Dimulai: membaca pulsa $cPin/$sPin ($mcuName)...")
        }
        Toast.makeText(context, "OEM Learn Dimulai: Hidupkan mesin dengan CDI OEM ($cPin/$sPin)", Toast.LENGTH_SHORT).show()
    }

    fun stopOemLearn() {
        if (!requireMcuOrDemo("stop OEM Learn")) return
        val t = _telemetry.value
        val mcuName = selectedPlatform.value.displayName
        if (bleClient.gattReady && (t.rpm != 0 || t.hvCenter >= 30 || t.hvSide >= 30)) {
            Toast.makeText(context, "Firmware R8 mensyaratkan RPM 0 dan HV <30 V saat menyimpan OEM Learn", Toast.LENGTH_LONG).show()
            return
        }
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("LEARN,STOP")
            bleClient.send("GET,LEARN")
            appendLog("BLE Send: LEARN,STOP (Simpan Map OEM ke Flash $mcuName)")
        } else {
            _isOemLearning.value = false
            _flashSaved.value = true
            _demoEngineRunning.value = false // Matikan mesin setelah rekaman selesai
            simRpm = 0f
            appendLog("OEM Learn Dihentikan: timing Center & Side tersimpan di flash $mcuName.")
        }
        Toast.makeText(context, "OEM Learn Selesai: Matikan mesin & cabut modul PC817 serta soket CDI OEM", Toast.LENGTH_LONG).show()
    }

    fun confirmOemUnplugged() {
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("MODE,DIY,OEM_UNPLUGGED")
            bleClient.send("GET,MODE")
            appendLog("BLE Send: MODE,DIY,OEM_UNPLUGGED")
        } else {
            _isOemUnpluggedConfirmed.value = true
            _firmwareMode.value = FirmwareRunMode.DIY
            appendLog("Konfirmasi OEM Unplugged Diterima. Mode DIY Aktif.")
            advanceSetupStage(SetupStage.FIRST_START.code)
        }
        Toast.makeText(context, "OEM Unplugged Dikonfirmasi • Mode DIY Aktif", Toast.LENGTH_SHORT).show()
    }

    fun setHvVoltageMode(proMode: Boolean) {
        if (!requireMcuOrDemo("pengaturan tegangan")) return
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
        if (t.rpm > 0) return "Mesin masih berputar (${t.rpm} RPM)! Matikan mesin (RPM = 0)."
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

        if ("OTA_STAGE" !in _mcuCapabilities.value) {
            Toast.makeText(context, "Firmware tidak melaporkan capability OTA_STAGE", Toast.LENGTH_LONG).show()
            return
        }

        if (bleClient.gattReady) {
            appendLog("Memulai OTA BLE untuk file: $fileName (${bytes.size} byte)")
            bleClient.startOta(bytes, _selectedPlatform.value)
        } else {
            Toast.makeText(context, "OTA hanya tersedia saat BLE CDI R8 terhubung", Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelOtaUpload() {
        bleClient.cancelOta()
        appendLog("OTA dibatalkan oleh pengguna")
        Toast.makeText(context, "OTA Dibatalkan", Toast.LENGTH_SHORT).show()
    }

    fun resetSetupWorkflow() {
        if (!requireMcuOrDemo("reset setup")) return
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

    fun setFanMode(mode: String) { // "OFF", "ON", "AUTO"
        if (!requireMcuOrDemo("pengaturan kipas")) return
        if (bleClient.gattReady) {
            markSetupCommandPending()
            bleClient.send("SETUP,FAN,$mode")
            appendLog("BLE Send: SETUP,FAN,$mode")
        } else {
            _fanMode.value = mode
            appendLog("Fan mode: $mode (Simulasi)")
        }
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
            _mcuCapabilities.value = emptySet()
            _isSimulationMode.value = false
            _isRevving.value = false
            _demoThrottleSlider.value = 0f
            simRpm = 0f
            simTps = 0f
            engineSound.stop()
            resetBleStatistics()
            telemetryWatchdogJob = viewModelScope.launch {
                while (_isConnected.value) {
                    delay(1_000)
                    val now = SystemClock.elapsedRealtime()
                    when {
                        _telemetryPacketCount.value == 0L && now - lastTelemetryPacketAtMs >= 3_000L -> {
                            _telemetryRxMessage.value =
                                "TIDAK ADA FRAME • Response 1003 dapat hidup walau Telemetry 1001 tidak notify"
                        }
                        lastTelemetryPacketAtMs > 0L && now - lastTelemetryPacketAtMs >= 1_500L -> {
                            _packetRateHz.value = 0
                            _telemetryRxMessage.value =
                                "TELEMETRY TERHENTI • tidak ada frame baru selama ${(now - lastTelemetryPacketAtMs) / 1000}s"
                        }
                    }
                }
            }
            bleClient.send("GET,CAPS")
            bleClient.send("GET,STATUS")
            bleClient.send("GET,META")
            bleClient.send("GET,SETUP")
            bleClient.send("GET,MODE")
            bleClient.send("GET,LEARN")
            bleClient.send("GET,OTA")
        } else {
            setupSyncedThisConnection = false
            _mcuCapabilities.value = emptySet()
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
        val current = _telemetry.value

        var targetStage = current.setupStage
        // Aplikasi menunggu status READY nyata dari flash MCU (value.ready == true atau MCU setupStage >= 4),
        // bukan berhenti atau menganggap selesai hanya karena bukti FIRST START (firstStartSeconds >= 3) tercatat di RAM.
        if (value.ready || _firmwareSetupStage.value >= 4) {
            targetStage = SetupStage.READY.code
            _firmwareSetupStage.value = targetStage
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
            if (total == 0) 0f
            else validCount * 100f / total

        _packetRateHz.value =
            if (rxSamples.size < 2) {
                0
            } else {
                val duration =
                    rxSamples.last().timestampMs -
                        rxSamples.first().timestampMs

                if (duration <= 0L) 0
                else (
                    (rxSamples.size - 1) * 1000f / duration
                ).roundToInt()
            }
    }

    private fun refreshSetupAfterAck() {
        viewModelScope.launch {
            delay(100)
            requestSetupState()
        }
    }

    override fun onResponse(value: String) {
        appendLog("RX: $value")
        val f = value.split(',')
        when (f.firstOrNull()) {
            "CAPS" -> {
                val caps = f.drop(1).map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()
                _mcuCapabilities.value = caps
                appendLog("MCU Capabilities R8: ${caps.joinToString(", ")}")
            }
            "STATUS" -> if (f.size >= 9) {
                val slot = f[5].toIntOrNull()?.coerceIn(0, 3) ?: _selectedMapSlot.value
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
            }
            "META" -> if (f.size >= 10) {
                _limiterType.value = if (f[3].toIntOrNull() == 1) "HARD" else "SOFT"
                _softRevLimiterRpm.value = f[4].toIntOrNull()?.coerceIn(3000, 11500) ?: _softRevLimiterRpm.value
                _softBandRpm.value = f[5].toIntOrNull()?.coerceIn(100, 1000) ?: _softBandRpm.value
                _targetHvVoltage.value = f[6].toIntOrNull()
                    ?.coerceIn(CdiProtocol.VOLTAGE_FIRST_START, CdiProtocol.VOLTAGE_PRO)
                    ?: _targetHvVoltage.value
                val rpmCount = f[8].toIntOrNull() ?: 0
                val tpsCount = f[9].toIntOrNull() ?: 0
                if (rpmCount in listOf(8, 16) && tpsCount in listOf(4, 8)) requestMapReadback(rpmCount, tpsCount)
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
                val ti = f[1].toIntOrNull(); val ri = f[2].toIntOrNull(); val cdeg = f[3].toIntOrNull()
                if (ti == mapReadbackTpsRow && ri != null && cdeg != null) {
                    mapReadback[ri] = cdeg / 100f
                    if (mapReadback.size == mapReadbackExpected) {
                        val axis = if (mapReadbackExpected == 16)
                            listOf(500,750,1000,1500,2000,2500,3000,4000,5000,6000,7000,8000,9000,10000,11000,11500)
                        else listOf(500,1000,1500,2500,4000,6000,8000,10000)
                        _customAdvancePoints.value = axis.mapIndexed { i, rpm -> CustomAdvancePoint(rpm, mapReadback[i] ?: 0f) }
                        appendLog("Map readback lengkap: ${axis.size} titik")
                    }
                }
            }
            "SETUP" -> if (f.size >= 14) {
                val firmwareStage = f[1].toIntOrNull()?.coerceIn(0, 4)
                    ?: _firmwareSetupStage.value

                val edgeCode = f[2].toIntOrNull() ?: 0
                val trigger = f[3].toIntOrNull()
                    ?: _telemetry.value.triggerCdeg

                val sideOffset = f[4].toIntOrNull() ?: 0
                val ppr = f[5].toIntOrNull()?.coerceIn(1, 4) ?: 1
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
                        (if (_strobeActive.value) 4 else 0),
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
                if (operation == "PONG_R7_2") {
                    preflightPingOk = true
                    updateQuickSetupPreflightProgress()
                }
                if (operation == "TDC_SAVED" || operation == "TDC_MANUAL_SAVED")
                    _flashSaved.value = true
                if (operation !in setOf("LIVE", "OFFSET", "PONG_R7_2"))
                    Toast.makeText(context, "MCU ACK: $operation", Toast.LENGTH_SHORT).show()

                val setupChangingOperations = setOf(
                    "PICKUP_OK",
                    "EDGE_REQUIRES_PICKUP_TDC",
                    "PPR_REQUIRES_TDC",
                    "GATE_US",
                    "TDC_SAVED",
                    "TDC_MANUAL_SAVED",
                    "TPS",
                    "FIRST_START",
                    "READY_CENTER",
                    "READY_THREE",
                    "FAN",
                    "SETUP_RESET"
                )

                when {
                    operation == "MODE" -> bleClient.send("GET,MODE")
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
                        operation == "PONG_R7_2" -> Unit

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

    private fun requestMapReadback(rpmCount: Int, tpsCount: Int) {
        if (!bleClient.gattReady) return
        mapReadback.clear(); mapReadbackExpected = rpmCount; mapReadbackTpsRow = tpsCount - 1
        repeat(rpmCount) { bleClient.send("GET,CELL,$mapReadbackTpsRow,$it") }
    }

    private fun startOemLearnPolling() {
        if (oemLearnPollJob?.isActive == true) return
        oemLearnPollJob = viewModelScope.launch {
            while (isActive && _isOemLearning.value && bleClient.gattReady) {
                delay(750)
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

                // If real BLE is connected, hardware telemetry governs everything unless user explicitly enabled demo simulation
                if (realBleReady && !isSim) {
                    // Motor is on real BLE. If real motor is off (RPM < 100), ensure sound is silent
                    if (_telemetry.value.rpm < 100) {
                        engineSound.stop()
                    }
                    continue
                }

                // If real motorcycle engine is running (RPM > 100 on BLE), prioritize real telemetry.
                // Otherwise (engine off, test bench, or simulation), simulate based on slider/revving.
                val realEngineRunning = realBleReady && _telemetry.value.rpm > 100
                val demoStarterOn = _demoEngineRunning.value
                val shouldSimulate = !realEngineRunning && (isSim || demoStarterOn || revving || slider > 0.01f || simRpm > 50f || simTps > 0.01f)
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
                    val currentHvSide = if (isRunning && _telemetry.value.setupStage != SetupStage.FIRST_START.code) {
                        targetHv + (sin(seq * 0.25) * 5).toInt()
                    } else {
                        0 // Side coil off during FIRST_START or when stopped
                    }
                    val simTelemetry = Telemetry(
                        sequence = seq,
                        rpm = simRpm.toInt().coerceIn(0, 13000),
                        tps = (simTps * 1000).toInt(),
                        advanceCdeg = (finalAdvance * 100).toInt(),
                        batteryCv = if (isRunning) 1380 + (sin(seq * 0.1) * 20).toInt() else 1260,
                        hvCenter = currentHvCenter,
                        hvSide = currentHvSide,
                        tempCdeg = 8200 + (simTps * 500).toInt(),
                        slot = _selectedMapSlot.value,
                        limiter = limiterState,
                        flags = 0x21 or (if (_flashSaved.value) 0x08 else 0x00),
                        faults = 0,
                        setupStage = _telemetry.value.setupStage,
                        outputFlags = (if (currentHvCenter > 0) 0x01 else 0x00) or (if (currentHvSide > 0) 0x02 else 0x00) or (if (_strobeActive.value) 0x04 else 0x00),
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
                } else if (!realEngineRunning) {
                    engineSound.stop()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
        oemLearnPollJob?.cancel()
        bleClient.release()
        engineSound.release()
    }
}
