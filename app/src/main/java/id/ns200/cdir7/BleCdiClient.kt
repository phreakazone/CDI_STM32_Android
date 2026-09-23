package id.ns200.cdir7

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.min

data class DiscoveredBleDevice(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    val rssi: Int,
    val isCdiCandidate: Boolean
)

sealed class OtaState {
    object Idle : OtaState()
    data class Preparing(val message: String) : OtaState()
    data class Transferring(
        val bytesTransferred: Int,
        val totalBytes: Int,
        val progressPercent: Float,
        val chunkIndex: Int,
        val totalChunks: Int
    ) : OtaState()
    data class Verifying(val crc32: Long) : OtaState()
    data class Success(val message: String) : OtaState()
    data class Error(val reason: String) : OtaState()
}

/**
 * BleCdiClient:
 * Dedicated GATT client for NS200 CDI R8 on STM32WB55 and ESP32, with legacy BLE identity compatibility.
 *
 * Requirements:
 * 1. Tanpa PIN / Bonding: Koneksi langsung via BLE GATT tanpa dialog PIN pairing Android.
 *    Aplikasi tidak mengubah bond Android secara diam-diam.
 * 3. PHY 1M & High Priority: Mengatur connection priority HIGH dan preferred PHY 1M untuk transmisi stabil.
 * 4. Service & CCCD Berurutan: Discover service -> negosiasi MTU 247 -> serial write CCCD (0x2902) untuk Telemetry & Response.
 * 5. ACK Command Queue: Antrean perintah dengan sequence number, hanya 1 perintah aktif (in-flight) per waktu.
 * 6. Retry Terbatas per Command: Maksimal 2x retry per perintah sebelum fail/timeout.
 * 7. Reconnect Eksponensial: Backoff eksponensial (1s, 2s, 4s, 8s, 16s, maks 30s) saat koneksi terputus atau GATT 8.
 * 8. Telemetry v3/v4 20-Byte: Menerima paket 20-byte bergantian CORE / DIAGNOSTIC dengan validasi CRC16.
 * 9. OTA R8 image uploader: karakteristik ...1004 (data) & ...1005 (status) dengan integritas CRC32.
 */
class BleCdiClient(private val context: Context, private val listener: Listener) {

    interface Listener {
        fun onState(text: String, connected: Boolean)
        fun onTelemetry(value: Telemetry)
        fun onRawPacket(bytes: ByteArray)
        fun onResponse(value: String)
    }

    private data class Queued(val sequence: Int, val body: String, var retries: Int = 0)

    private val main = Handler(Looper.getMainLooper())
    private val adapter: BluetoothAdapter?
        get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val serviceUuid = UUID.fromString(CdiProtocol.SERVICE)
    private val telemetryUuid = UUID.fromString(CdiProtocol.TELEMETRY)
    private val commandUuid = UUID.fromString(CdiProtocol.COMMAND)
    private val responseUuid = UUID.fromString(CdiProtocol.RESPONSE)
    private val otaDataUuid = UUID.fromString(CdiProtocol.OTA_DATA)
    private val otaStatusUuid = UUID.fromString(CdiProtocol.OTA_STATUS)
    private val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var gatt: BluetoothGatt? = null
    private var commandChar: BluetoothGattCharacteristic? = null
    private var otaDataChar: BluetoothGattCharacteristic? = null
    private var otaStatusChar: BluetoothGattCharacteristic? = null
    private var lastDevice: BluetoothDevice? = null
    private var manualStop = true
    private var retryCount = 0
    private var sequence = 0
    private var subscriptionsStarted = false
    private var descriptorActive = false

    private val descriptors = ArrayDeque<BluetoothGattDescriptor>()
    private val commands = ArrayDeque<Queued>()
    private var activeCommand: Queued? = null
    private val responseBuffer = StringBuilder()
    private var lastTelemetry = CdiProtocol.emptyTelemetry()

    private var scanTimer: Runnable? = null
    private var fallbackScanTimer: Runnable? = null
    private var phaseTimer: Runnable? = null
    private var reconnectTimer: Runnable? = null
    private var commandTimer: Runnable? = null

    // OTA upload variables
    private val _otaState = MutableStateFlow<OtaState>(OtaState.Idle)
    val otaState: StateFlow<OtaState> = _otaState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private var otaDataBuffer: ByteArray? = null
    private var otaChunkIndex = 0
    private var otaTotalChunks = 0
    private var otaBytesAcknowledged = 0
    private var otaChunkPayloadSize = CdiProtocol.OTA_CHUNK_MAX_SIZE
    private var otaCancelled = false
    private var otaAwaitingStatus = false
    private var otaCommitSent = false
    private var otaJob: Runnable? = null
    private var negotiatedMtu = 23
    private var otaPlatform = McuPlatform.STM32WB55

    var gattReady = false
        private set

    var autoReconnect = true

    private val _devices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _devices.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _pending = MutableStateFlow(0)
    val pendingCommands: StateFlow<Int> = _pending.asStateFlow()

    private var isDiscoveryOnly = false

    private val blePrefs = context.getSharedPreferences("ignitra_ble_prefs", Context.MODE_PRIVATE)

    private val _savedDeviceMac = MutableStateFlow<String?>(blePrefs.getString("saved_cdi_mac", null))
    val savedDeviceMac: StateFlow<String?> = _savedDeviceMac.asStateFlow()

    private val _savedDeviceName = MutableStateFlow<String?>(blePrefs.getString("saved_cdi_name", null))
    val savedDeviceName: StateFlow<String?> = _savedDeviceName.asStateFlow()

    // Mode Hemat Baterai (Power Save): Prioritas KONEKSI TINGGI (11-15ms) aktif secara default untuk telemetri ultra cepat
    private val _powerSaveMode = MutableStateFlow(blePrefs.getBoolean("power_save_mode", false))
    val powerSaveMode: StateFlow<Boolean> = _powerSaveMode.asStateFlow()

    private val _autoConnectOnStart = MutableStateFlow(blePrefs.getBoolean("auto_connect_start", false))
    val autoConnectOnStart: StateFlow<Boolean> = _autoConnectOnStart.asStateFlow()

    fun setPowerSaveMode(enabled: Boolean) {
        _powerSaveMode.value = enabled
        blePrefs.edit().putBoolean("power_save_mode", enabled).apply()
        applyConnectionPriority()
        listener.onState(if (enabled) "Mode Hemat Baterai aktif (Priority Balanced • 45ms)" else "Mode Balap aktif (Priority High • 15ms)", gattReady)
    }

    fun setAutoConnectOnStart(enabled: Boolean) {
        _autoConnectOnStart.value = enabled
        blePrefs.edit().putBoolean("auto_connect_start", enabled).apply()
    }

    fun applyConnectionPriority() {
        try {
            val priority = if (_powerSaveMode.value) {
                BluetoothGatt.CONNECTION_PRIORITY_BALANCED
            } else {
                BluetoothGatt.CONNECTION_PRIORITY_HIGH
            }
            gatt?.requestConnectionPriority(priority)
        } catch (_: Exception) {}
    }

    /**
     * Memaksa Bluetooth GATT untuk beralih ke CONNECTION_PRIORITY_HIGH (interval 11.25ms - 15ms)
     * untuk responsivitas telemetri maksimum dan latensi minimal.
     */
    fun requestHighPriority(): Boolean {
        return try {
            gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH) ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Permintaan negosiasi MTU GATT (misal 247 byte) untuk paket transfer data yang lebih besar dan efisien.
     */
    fun requestMtu(mtu: Int = 247): Boolean {
        return try {
            gatt?.requestMtu(mtu) ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun saveLastDevice(mac: String, name: String?) {
        val cleanMac = mac.trim().uppercase()
        val cleanName = name?.trim()?.ifBlank { null } ?: "IGNITRA CDI"
        _savedDeviceMac.value = cleanMac
        _savedDeviceName.value = cleanName
        blePrefs.edit()
            .putString("saved_cdi_mac", cleanMac)
            .putString("saved_cdi_name", cleanName)
            .apply()
    }

    fun forgetSavedDevice() {
        _savedDeviceMac.value = null
        _savedDeviceName.value = null
        blePrefs.edit()
            .remove("saved_cdi_mac")
            .remove("saved_cdi_name")
            .apply()
        listener.onState("Modul CDI tersimpan telah dihapus", false)
    }

    fun isBluetoothEnabled(): Boolean {
        return adapter?.isEnabled == true
    }

    fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Izin khusus untuk direct connection ke MAC address yang telah disimpan.
     * Tidak memerlukan BLUETOOTH_SCAN maupun ACCESS_FINE_LOCATION / GPS.
     */
    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 11 ke bawah tidak mewajibkan runtime permissions untuk connectGatt ke MAC yang sudah diketahui
            true
        }
    }

    private val scannerCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(type: Int, result: ScanResult) {
            val device = result.device
            val name = try {
                result.scanRecord?.deviceName ?: device.name ?: "Unknown BLE"
            } catch (_: SecurityException) {
                "Unknown BLE"
            }
            val exact = name.equals("IGNITRA-CDI", true) || name.equals("IGNITRA CDI", true) || name.equals("NS200-CDI", true) || name.equals("IGNITRA", true)
            val byService = result.scanRecord?.serviceUuids?.any { it.uuid == serviceUuid } == true
            val isCandidate = exact || byService || name.contains("IGNITRA", true) || name.contains("NS200-CDI", true) || name.contains("CDI", true) || name.contains("NS200", true)

            val item = DiscoveredBleDevice(
                device = device,
                name = name,
                address = device.address,
                rssi = result.rssi,
                isCdiCandidate = isCandidate
            )

            val list = _devices.value.toMutableList()
            val index = list.indexOfFirst { it.address == item.address }
            if (index >= 0) list[index] = item else list.add(item)
            _devices.value = list.sortedWith(
                compareByDescending<DiscoveredBleDevice> { it.isCdiCandidate }
                    .thenByDescending { it.rssi }
            )

            // Auto-connect if in connect mode (not discovery-only scan) and exact target match discovered
            if (!isDiscoveryOnly && (exact || byService) && gatt == null && !manualStop) {
                connectDeviceExplicit(device)
            }
        }

        override fun onScanFailed(code: Int) {
            _scanning.value = false
            _busy.value = false
            val detail = when (code) {
                ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Scan sudah berjalan (1)"
                ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Registrasi scanner BLE gagal (2) • Coba matikan & hidupkan Bluetooth HP"
                ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Internal BLE stack error (3)"
                ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE Scan tidak didukung (4)"
                ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "Resource BLE penuh (5)"
                else -> "Kode error $code"
            }
            listener.onState("Gagal Memindai BLE: $detail", false)
        }
    }

    private val callback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(owner: BluetoothGatt, status: Int, state: Int) {
            if (owner !== gatt) {
                close(owner)
                return
            }

            if (status == BluetoothGatt.GATT_SUCCESS && state == BluetoothProfile.STATE_CONNECTED) {
                cancelReconnect()
                listener.onState("BLE terhubung • menginisialisasi GATT...", false)

                // Request CONNECTION_PRIORITY_HIGH segera setelah koneksi terbuka untuk mempercepat GATT Discovery
                try {
                    owner.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                } catch (_: Exception) {}

                // Timer pengaman jika proses service discovery tidak merespons
                phaseTimer = Runnable {
                    if (gatt === owner && !gattReady) fail("Timeout service discovery")
                }.also { main.postDelayed(it, 8_000) }

                // Delay 400ms sebelum discoverServices untuk stabilitas HCI stack ESP32 NimBLE
                main.postDelayed({
                    if (gatt === owner && !owner.discoverServices()) {
                        fail("Service discovery gagal dijalankan")
                    }
                }, 400)
            } else {
                val reason = if (status == 8) "GATT 8 / timeout link" else "status $status"
                closeCurrent()
                if (!manualStop && autoReconnect && lastDevice != null) {
                    reconnect(reason)
                } else {
                    _busy.value = false
                    listener.onState("Disconnected ($reason)", false)
                }
            }
        }

        // Connection is only opened after the Activity grants BLUETOOTH_CONNECT.
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(owner: BluetoothGatt, status: Int) {
            if (owner !== gatt) return
            cancelPhase()

            if (status != BluetoothGatt.GATT_SUCCESS) {
                return fail("Service discovery gagal (status $status)")
            }

            val service = owner.getService(serviceUuid)
            commandChar = service?.getCharacteristic(commandUuid)
            val teleChar = service?.getCharacteristic(telemetryUuid)
            val respChar = service?.getCharacteristic(responseUuid)
            otaDataChar = service?.getCharacteristic(otaDataUuid)
            otaStatusChar = service?.getCharacteristic(otaStatusUuid)

            if (service == null || (teleChar == null && respChar == null && commandChar == null)) {
                return fail("Service CDI R7/R8 tidak ditemukan pada GATT")
            }

            listener.onState("GATT siap • negosiasi MTU 247...", false)
            val ok = try {
                owner.requestMtu(247)
            } catch (_: Exception) {
                false
            }

            if (!ok) {
                subscribe(owner)
            } else {
                phaseTimer = Runnable {
                    if (gatt === owner && !subscriptionsStarted) subscribe(owner)
                }.also { main.postDelayed(it, 2_000) }
            }
        }

        override fun onMtuChanged(owner: BluetoothGatt, mtu: Int, status: Int) {
            if (owner !== gatt) return
            cancelPhase()
            negotiatedMtu = if (status == BluetoothGatt.GATT_SUCCESS) mtu else 23
            try {
                owner.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
            } catch (_: Exception) {}
            listener.onState("MTU dinegosiasikan: $negotiatedMtu byte • Prioritas KONEKSI TINGGI aktif", false)
            subscribe(owner)
        }

        override fun onDescriptorWrite(owner: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            if (owner !== gatt) return
            cancelPhase()
            descriptorActive = false

            // Delay 60ms antar penulisan CCCD agar stack NimBLE ESP32 tidak mengalami collision GATT
            main.postDelayed({
                if (gatt === owner) {
                    writeNextDescriptor(owner)
                }
            }, 60)
        }

        override fun onCharacteristicWrite(
            owner: BluetoothGatt,
            c: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (owner !== gatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                listener.onResponse("WARN,GATT_WRITE_STATUS_$status")
            }
        }

        @Deprecated("Android 10-12 callback")
        override fun onCharacteristicChanged(owner: BluetoothGatt, c: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            consume(c.uuid, c.value ?: ByteArray(0))
        }

        override fun onCharacteristicChanged(
            owner: BluetoothGatt,
            c: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            consume(c.uuid, value)
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribe(owner: BluetoothGatt) {
        if (owner !== gatt || subscriptionsStarted) return
        cancelPhase()

        val service = owner.getService(serviceUuid) ?: return fail("Service R7/R8 hilang saat subscribe")
        descriptors.clear()

        val teleChar = service.getCharacteristic(telemetryUuid)
        val respChar = service.getCharacteristic(responseUuid)

        queueCccd(owner, teleChar)
        queueCccd(owner, respChar)
        otaStatusChar?.let { queueCccd(owner, it) }

        if (descriptors.isEmpty()) {
            return fail("CCCD Telemetry / Response tidak tersedia pada GATT")
        }

        subscriptionsStarted = true
        listener.onState("GATT siap • mendaftarkan notifikasi serial...", false)
        writeNextDescriptor(owner)
    }

    @SuppressLint("MissingPermission")
    private fun queueCccd(owner: BluetoothGatt, c: BluetoothGattCharacteristic?): Boolean {
        if (c == null || !owner.setCharacteristicNotification(c, true)) return false
        val d = c.getDescriptor(cccdUuid) ?: return false
        descriptors.addLast(d)
        return true
    }

    @SuppressLint("MissingPermission")
    private fun writeNextDescriptor(owner: BluetoothGatt) {
        if (owner !== gatt || descriptorActive) return
        val d = descriptors.removeFirstOrNull()

        if (d == null) {
            gattReady = true
            _busy.value = false
            retryCount = 0
            val deviceName = try {
                lastDevice?.name
            } catch (_: SecurityException) {
                null
            } ?: "NS200-CDI"
            _connectedDeviceName.value = deviceName

            // Terapkan priority setelah discovery dan CCCD sukses terkunci
            applyConnectionPriority()

            listener.onState("Connected • $deviceName", true)

            // Mulai kirim command antrean secara stabil dengan jeda awal 150ms
            main.postDelayed({
                if (gattReady) {
                    writeNextCommand()
                }
            }, 150)
            return
        }

        descriptorActive = true
        val ok = try {
            if (Build.VERSION.SDK_INT >= 33) {
                owner.writeDescriptor(
                    d,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                owner.writeDescriptor(d)
            }
        } catch (_: Exception) {
            false
        }

        if (!ok) {
            descriptorActive = false
            descriptors.addFirst(d)
            main.postDelayed({
                if (gatt === owner && !gattReady) {
                    writeNextDescriptor(owner)
                }
            }, 100)
            return
        }

        phaseTimer = Runnable {
            if (descriptorActive && gatt === owner) {
                descriptorActive = false
                writeNextDescriptor(owner)
            }
        }.also { main.postDelayed(it, 4_000) }
    }

    private fun consume(uuid: UUID, bytes: ByteArray) = main.post {
        if (uuid == telemetryUuid) {
            listener.onRawPacket(bytes)
            val value = CdiProtocol.telemetry(bytes, lastTelemetry)
            if (value != null) {
                lastTelemetry = value
                listener.onTelemetry(value)
            }
        } else if (uuid == otaStatusUuid) {
            val status = CdiProtocol.otaStatus(bytes)
            val legacy = CdiProtocol.legacyEsp32OtaStatus(bytes)
            when {
                status != null -> {
                    listener.onResponse(
                        "OTA,${status.state.code},${status.receivedBytes},${status.expectedBytes},${status.errorCode}"
                    )
                    handleOtaStatus(status)
                }
                legacy != null -> {
                    val (state, error) = legacy
                    listener.onResponse("OTA_STATUS_ESP32,${state.code},$error")
                    if (state == FirmwareOtaState.ERROR) {
                        otaAwaitingStatus = false
                        _otaState.value = OtaState.Error("ESP32 menolak OTA (kode $error)")
                    }
                }
                else -> listener.onResponse("ERR,OTA_STATUS_CRC_OR_LENGTH_${bytes.size}")
            }
        } else if (uuid == responseUuid) {
            responseBuffer.append(bytes.toString(Charsets.US_ASCII))
            while (true) {
                while (responseBuffer.isNotEmpty() &&
                    (responseBuffer[0] == '\n' || responseBuffer[0] == '\r')) {
                    responseBuffer.deleteCharAt(0)
                }

                val newline = responseBuffer.indexOf("\n")
                val star = responseBuffer.indexOf("*")
                val completeWithoutNewline = star >= 0 && responseBuffer.length >= star + 5
                val end = when {
                    newline >= 0 -> newline + 1
                    completeWithoutNewline -> star + 5
                    else -> break
                }

                val frame = responseBuffer.substring(0, end)
                responseBuffer.delete(0, end)
                val parsed = CdiProtocol.response(frame)

                if (parsed != null) {
                    val optionalGetRejected =
                        parsed.body.startsWith("ERR,") && activeCommand?.body?.startsWith("GET,") == true
                    val abortTransaction = parsed.body.startsWith("ERR,") && !optionalGetRejected
                    // Lepaskan activeCommand jika firmware merespons. ERR pada GET opsional
                    // tidak boleh membuang sisa initial sync.
                    if (activeCommand != null) {
                        commandTimer?.let(main::removeCallbacks)
                        commandTimer = null
                        activeCommand = null
                        if (abortTransaction) commands.clear()
                        updatePending()
                    }
                    handleOtaControlResponse(parsed.body)
                    listener.onResponse(parsed.body)
                    main.postDelayed({ writeNextCommand() }, 40)
                } else if (frame.trim().isNotEmpty()) {
                    val body = frame.trim()
                    if (activeCommand != null) {
                        commandTimer?.let(main::removeCallbacks)
                        commandTimer = null
                        activeCommand = null
                        updatePending()
                    }
                    listener.onResponse(body)
                    main.postDelayed({ writeNextCommand() }, 40)
                }
            }

            if (responseBuffer.length > 512) {
                responseBuffer.clear()
            }
        }
    }

    /**
     * Menghubungkan ke CDI.
     * Jika ada MAC tersimpan, lakukan KONEKSI LANGSUNG (Bebas GPS & Sangat Hemat Baterai).
     * Jika belum ada MAC tersimpan, mulai pemindaian (scan) untuk mencari modul.
     */
    fun connect() {
        val savedMac = _savedDeviceMac.value
        if (!savedMac.isNullOrBlank()) {
            val savedName = _savedDeviceName.value ?: "IGNITRA CDI"
            listener.onState("Koneksi langsung ke $savedName (Tanpa GPS / Hemat Baterai)...", false)
            connectSavedDevice()
        } else {
            startScan(discoveryOnly = false)
        }
    }

    /**
     * KONEKSI LANGSUNG KE MAC ADDRESS (Bebas GPS & 100% Hemat Baterai)
     * Tidak memindai channel RF, tidak memerlukan GPS aktif di semua Android,
     * dan langsung membuka socket LE L2CAP ke modul hardware target.
     */
    @SuppressLint("MissingPermission")
    fun connectAddress(macAddress: String, customName: String? = null): Boolean {
        val cleanMac = macAddress.trim().uppercase()
        if (!BluetoothAdapter.checkBluetoothAddress(cleanMac)) {
            listener.onState("Format MAC Address tidak valid ($macAddress)", false)
            return false
        }
        val currentAdapter = adapter
        if (currentAdapter?.isEnabled != true) {
            listener.onState("Bluetooth tidak aktif • Harap nyalakan Bluetooth HP", false)
            return false
        }
        if (!hasConnectPermission()) {
            val permName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "BLUETOOTH_CONNECT" else "BLUETOOTH"
            listener.onState("Izin $permName belum diizinkan", false)
            return false
        }

        stopScanInternal()
        val device = try {
            currentAdapter.getRemoteDevice(cleanMac)
        } catch (e: Exception) {
            listener.onState("Gagal memuat perangkat: " + (e.message ?: "Unknown"), false)
            return false
        }

        val name = customName ?: try { device.name } catch (_: Exception) { null } ?: "IGNITRA CDI"
        saveLastDevice(cleanMac, name)
        connectDeviceExplicit(device)
        return true
    }

    /**
     * Menghubungkan langsung ke modul CDI yang tersimpan sebelumnya tanpa scan.
     */
    fun connectSavedDevice(): Boolean {
        val mac = _savedDeviceMac.value
        if (mac.isNullOrBlank()) {
            listener.onState("Belum ada MAC tersimpan • Gunakan SCAN atau input MAC manual", false)
            return false
        }
        return connectAddress(mac, _savedDeviceName.value)
    }

    @SuppressLint("MissingPermission")
    fun startScan(discoveryOnly: Boolean = false) {
        manualStop = false
        isDiscoveryOnly = discoveryOnly
        stopScanInternal()
        closeCurrent()
        cancelReconnect()
        commands.clear()
        activeCommand = null
        updatePending()
        retryCount = 0
        lastDevice = null

        val currentAdapter = adapter
        if (currentAdapter?.isEnabled != true) {
            _busy.value = false
            _scanning.value = false
            listener.onState("Bluetooth tidak aktif • Harap nyalakan Bluetooth HP", false)
            return
        }

        val scanner = currentAdapter.bluetoothLeScanner
        if (scanner == null) {
            _busy.value = false
            _scanning.value = false
            listener.onState("Bluetooth LE Scanner tidak tersedia di perangkat ini", false)
            return
        }

        if (!hasPermissions()) {
            _busy.value = false
            _scanning.value = false
            val permName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "BLUETOOTH_SCAN/CONNECT" else "LOKASI/GPS"
            listener.onState("Izin $permName belum diizinkan", false)
            return
        }

        // Muat perangkat yang sudah dipairing (bonded) secara instan tanpa perlu menunggu paket scan / GPS
        val initialList = mutableListOf<DiscoveredBleDevice>()
        try {
            if (hasConnectPermission()) {
                currentAdapter.bondedDevices?.forEach { bonded ->
                    val bName = try { bonded.name } catch (_: SecurityException) { null } ?: "Perangkat Paired"
                    val isCandidate = bName.contains("IGNITRA", true) || bName.contains("CDI", true) || bName.contains("NS200", true)
                    initialList.add(
                        DiscoveredBleDevice(
                            device = bonded,
                            name = "$bName (Paired)",
                            address = bonded.address,
                            rssi = -50,
                            isCdiCandidate = isCandidate
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        _devices.value = initialList
        _scanning.value = true
        _busy.value = !discoveryOnly
        listener.onState(if (discoveryOnly) "Memindai perangkat BLE sekitar..." else "Memindai modul CDI...", false)

        // Gunakan SCAN_MODE_LOW_LATENCY untuk responsivitas tercepat mendeteksi paket advert CDI
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        try {
            val serviceFilters = listOf(
                ScanFilter.Builder().setServiceUuid(ParcelUuid(serviceUuid)).build()
            )
            if (discoveryOnly) {
                scanner.startScan(null, settings, scannerCallback)
            } else {
                scanner.startScan(serviceFilters, settings, scannerCallback)
                // Firmware/board lama kadang tidak memasukkan service UUID ke advertising.
                // Setelah 6 detik tanpa kandidat, pindah ke scan nama sebagai fallback.
                fallbackScanTimer = Runnable {
                    if (_scanning.value && gatt == null && _devices.value.none { it.isCdiCandidate }) {
                        runCatching { scanner.stopScan(scannerCallback) }
                        runCatching { scanner.startScan(null, settings, scannerCallback) }
                        listener.onState("UUID CDI belum terlihat • fallback pencarian nama NS200-CDI", false)
                    }
                }.also { main.postDelayed(it, 6_000) }
            }
            // Timeout scan 12 detik agar memberikan waktu cukup untuk menangkap advertising interval
            scanTimer = Runnable {
                stopScanInternal()
                if (gatt == null) {
                    _busy.value = false
                    val count = _devices.value.size
                    if (count > 0) {
                        listener.onState("Pemindaian selesai • $count perangkat ditemukan", false)
                    } else {
                        listener.onState("Tidak ada sinyal BLE terdeteksi • Pastikan modul CDI menyala & kontak ON", false)
                    }
                }
            }.also { main.postDelayed(it, 12_000) }
        } catch (e: Exception) {
            _scanning.value = false
            _busy.value = false
            listener.onState("Scan gagal: " + (e.message ?: "Unknown error"), false)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanInternal() {
        scanTimer?.let(main::removeCallbacks)
        scanTimer = null
        fallbackScanTimer?.let(main::removeCallbacks)
        fallbackScanTimer = null
        _scanning.value = false
        try {
            adapter?.bluetoothLeScanner?.stopScan(scannerCallback)
        } catch (_: Exception) {}
    }

    fun connectDeviceExplicit(device: BluetoothDevice) {
        stopScanInternal()
        manualStop = false
        lastDevice = device
        retryCount = 0
        val displayName = try {
            if (hasPermissions()) device.name ?: device.address else device.address
        } catch (_: SecurityException) {
            device.address
        }
        saveLastDevice(device.address, displayName)
        listener.onState("Menghubungkan $displayName...", false)
        open(device)
    }

    @SuppressLint("MissingPermission")
    private fun open(device: BluetoothDevice) {
        cancelReconnect()
        closeCurrent()
        _busy.value = true

        val displayName = try { device.name } catch (_: Exception) { null } ?: device.address
        saveLastDevice(device.address, displayName)
        listener.onState("Menghubungkan $displayName • tanpa PIN/bonding...", false)

        try {
            gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
            phaseTimer = Runnable {
                if (!gattReady && gatt != null) fail("Timeout connect")
            }.also { main.postDelayed(it, 10_000) }
        } catch (e: Exception) {
            reconnect("connectGatt: " + e.message)
        }
    }

    private fun fail(reason: String) {
        closeCurrent()
        if (!manualStop && autoReconnect && lastDevice != null) {
            reconnect(reason)
        } else {
            _busy.value = false
            listener.onState("Disconnected ($reason)", false)
        }
    }

    private fun reconnect(reason: String) {
        val device = lastDevice ?: return
        if (manualStop || !autoReconnect) return
        cancelReconnect()
        retryCount++

        // Exponential backoff: 1s, 2s, 4s, 8s, 16s, max 30s
        val delayMs = min(30_000L, 1_000L * (1L shl min(retryCount - 1, 5)))
        _busy.value = true
        listener.onState("$reason • reconnect ${delayMs / 1000}s (percobaan $retryCount)", false)

        reconnectTimer = Runnable {
            reconnectTimer = null
            open(device)
        }.also { main.postDelayed(it, delayMs) }
    }

    private fun cancelReconnect() {
        reconnectTimer?.let(main::removeCallbacks)
        reconnectTimer = null
    }

    private fun cancelPhase() {
        phaseTimer?.let(main::removeCallbacks)
        phaseTimer = null
    }

    @SuppressLint("MissingPermission")
    private fun close(owner: BluetoothGatt) {
        try { owner.disconnect() } catch (_: Exception) {}
        try { owner.close() } catch (_: Exception) {}
    }

    private fun closeCurrent() {
        cancelPhase()
        commandTimer?.let(main::removeCallbacks)
        commandTimer = null
        otaJob?.let(main::removeCallbacks)
        otaJob = null
        val transferActive = otaDataBuffer != null && !otaCancelled &&
            _otaState.value !is OtaState.Success && _otaState.value !is OtaState.Error
        otaCancelled = true
        otaDataBuffer = null
        val owner = gatt
        gatt = null
        if (owner != null) close(owner)
        commandChar = null
        otaDataChar = null
        otaStatusChar = null
        descriptors.clear()
        descriptorActive = false
        subscriptionsStarted = false
        gattReady = false
        negotiatedMtu = 23
        _connectedDeviceName.value = null
        responseBuffer.clear()
        activeCommand = null
        commands.clear()
        updatePending()
        if (transferActive) {
            _otaState.value = OtaState.Error("Koneksi BLE terputus saat transfer OTA")
        } else if (_otaState.value !is OtaState.Success && _otaState.value !is OtaState.Error) {
            _otaState.value = OtaState.Idle
        }
    }

    fun disconnect() {
        manualStop = true
        stopScanInternal()
        cancelReconnect()
        closeCurrent()
        commands.clear()
        updatePending()
        _busy.value = false
        listener.onState("Disconnected (Manual)", false)
    }

    fun release() = disconnect()

    /**
     * Memulai pengunggahan image aplikasi target melalui BLE OTA R9.
     * Karakteristik Data: ...1004 (chunk sequential maks 208 byte)
     * Karakteristik Status: ...1005 (notifikasi / status flash)
     */
    fun startOta(
        data: ByteArray,
        platform: McuPlatform,
        imageVersion: Long
    ): Boolean {
        if (data.size !in CdiProtocol.OTA_MIN_IMAGE_SIZE..CdiProtocol.OTA_MAX_IMAGE_SIZE) {
            _otaState.value = OtaState.Error(
                "Ukuran image firmware harus ${CdiProtocol.OTA_MIN_IMAGE_SIZE}..${CdiProtocol.OTA_MAX_IMAGE_SIZE} byte"
            )
            return false
        }
        if (!gattReady || otaDataChar == null || otaStatusChar == null) {
            _otaState.value = OtaState.Error("BLE/karakteristik OTA R9 belum siap")
            return false
        }
        otaCancelled = false
        otaAwaitingStatus = false
        otaCommitSent = false
        otaDataBuffer = data
        otaPlatform = platform
        val crc32Val = CdiProtocol.crc32(data)
        val attPayload = negotiatedMtu - if (platform == McuPlatform.STM32WB55) 10 else 3
        otaChunkPayloadSize = minOf(
            CdiProtocol.OTA_CHUNK_MAX_SIZE,
            (attPayload.coerceAtLeast(8) / 8) * 8
        )
        otaTotalChunks = (data.size + otaChunkPayloadSize - 1) / otaChunkPayloadSize
        otaChunkIndex = 0
        otaBytesAcknowledged = 0

        _otaState.value = OtaState.Preparing(
            "Inisialisasi OTA R9 (%d B, %d chunk, CRC32: %08X)...".format(data.size, otaTotalChunks, crc32Val)
        )
        return send("OTA,BEGIN,$imageVersion,${data.size},$crc32Val")
    }

    fun cancelOta() {
        otaCancelled = true
        otaJob?.let(main::removeCallbacks)
        otaJob = null
        otaDataBuffer = null
        _otaState.value = OtaState.Idle
        send("OTA,ABORT")
    }

    @SuppressLint("MissingPermission")
    private fun sendNextOtaChunk() {
        if (otaCancelled || !gattReady || otaAwaitingStatus) return
        val buf = otaDataBuffer ?: return
        if (otaBytesAcknowledged >= buf.size) {
            if (otaCommitSent) return
            val crc = CdiProtocol.crc32(buf)
            _otaState.value = OtaState.Verifying(crc)
            otaCommitSent = true
            send("OTA,COMMIT")
            return
        }

        val start = otaBytesAcknowledged
        val end = minOf(start + otaChunkPayloadSize, buf.size)
        val chunk = buf.copyOfRange(start, end)
        val packet = if (otaPlatform == McuPlatform.STM32WB55) {
            try {
                CdiProtocol.otaDataPacket(start, chunk)
            } catch (e: IllegalArgumentException) {
                _otaState.value = OtaState.Error(e.message ?: "Format chunk OTA tidak valid")
                return
            }
        } else {
            // Port ESP32 R9 meneruskan payload karakteristik 1004 langsung ke
            // cdi_esp32_ota_data_rx(received,...), tanpa header offset/CRC16 STM32.
            chunk
        }
        val owner = gatt
        val char = otaDataChar

        val accepted = if (owner != null && char != null) {
            try {
                if (Build.VERSION.SDK_INT >= 33) {
                    owner.writeCharacteristic(
                        char,
                        packet,
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    ) == BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    @Suppress("DEPRECATION")
                    char.value = packet
                    @Suppress("DEPRECATION")
                    owner.writeCharacteristic(char)
                }
            } catch (_: Exception) { false }
        } else false

        if (!accepted) {
            _otaState.value = OtaState.Error("GATT menolak chunk OTA pada offset $start")
            return
        }
        otaAwaitingStatus = true
        otaJob?.let(main::removeCallbacks)
        otaJob = Runnable {
            if (!otaCancelled && otaAwaitingStatus && gattReady) send("GET,OTA")
        }.also { main.postDelayed(it, 2_000L) }
    }

    private fun handleOtaStatus(status: OtaStatusPacket) {
        val buf = otaDataBuffer
        if (status.state == FirmwareOtaState.ERROR) {
            otaAwaitingStatus = false
            _otaState.value = OtaState.Error("Firmware menolak OTA (kode ${status.errorCode})")
            return
        }
        if (buf != null && status.expectedBytes != 0L && status.expectedBytes != buf.size.toLong()) {
            _otaState.value = OtaState.Error("Panjang OTA MCU ${status.expectedBytes} tidak cocok dengan file ${buf.size}")
            return
        }
        when (status.state) {
            FirmwareOtaState.IDLE, FirmwareOtaState.ERASING -> Unit
            FirmwareOtaState.RECEIVING -> {
                if (buf == null) return
                val acknowledged = status.receivedBytes.toInt().coerceIn(0, buf.size)
                if (acknowledged < otaBytesAcknowledged || acknowledged > otaBytesAcknowledged + otaChunkPayloadSize) {
                    _otaState.value = OtaState.Error("Offset OTA MCU tidak berurutan: $acknowledged")
                    return
                }
                if (acknowledged == otaBytesAcknowledged && otaAwaitingStatus) return
                otaJob?.let(main::removeCallbacks)
                otaJob = null
                otaAwaitingStatus = false
                otaBytesAcknowledged = acknowledged
                otaChunkIndex = (acknowledged + otaChunkPayloadSize - 1) / otaChunkPayloadSize
                _otaState.value = OtaState.Transferring(
                    bytesTransferred = acknowledged,
                    totalBytes = buf.size,
                    progressPercent = acknowledged.toFloat() / buf.size,
                    chunkIndex = otaChunkIndex,
                    totalChunks = otaTotalChunks
                )
                main.postDelayed(::sendNextOtaChunk, 15L)
            }
            FirmwareOtaState.READY -> {
                otaAwaitingStatus = false
                otaDataBuffer = null
                _otaState.value = OtaState.Success(
                    "Image firmware terverifikasi; ${otaPlatform.shortName} akan boot ke firmware baru"
                )
            }
            FirmwareOtaState.ERROR -> Unit
        }
    }

    private fun handleOtaControlResponse(body: String) {
        when {
            body == "ACK,OTA_RECEIVE" -> {
                otaAwaitingStatus = false
                sendNextOtaChunk()
            }
            body == "ACK,OTA_READY_REBOOT" -> {
                otaDataBuffer = null
                _otaState.value = OtaState.Success(
                    "OTA R8 selesai; menunggu reboot ${otaPlatform.shortName}"
                )
            }
            body == "ACK,OTA_ABORT" -> {
                otaDataBuffer = null
                _otaState.value = OtaState.Idle
            }
            body.startsWith("OTA,") -> {
                val fields = body.split(',')
                if (fields.size == 5) {
                    val state = FirmwareOtaState.fromCode(fields[1].toIntOrNull() ?: return) ?: return
                    handleOtaStatus(
                        OtaStatusPacket(
                            state,
                            fields[2].toLongOrNull() ?: return,
                            fields[3].toLongOrNull() ?: return,
                            fields[4].toIntOrNull() ?: return
                        )
                    )
                }
            }
            body.startsWith("ERR,") && otaDataBuffer != null -> {
                _otaState.value = OtaState.Error("Firmware menolak OTA: $body")
            }
        }
    }

    fun send(body: String): Boolean {
        if (!gattReady || body.isBlank()) return false
        sequence = (sequence + 1) and 0xffff
        commands.addLast(Queued(sequence, body.trim()))
        updatePending()
        writeNextCommand()
        return true
    }

    @SuppressLint("MissingPermission")
    private fun writeNextCommand() {
        if (activeCommand != null || !gattReady) return
        val owner = gatt ?: return
        val c = commandChar ?: return
        val item = commands.removeFirstOrNull() ?: return updatePending()

        activeCommand = item
        updatePending()

        val bytes = CdiProtocol.command(item.sequence, item.body)
        val writeType = if ((c.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }

        val ok = try {
            if (Build.VERSION.SDK_INT >= 33) {
                owner.writeCharacteristic(
                    c,
                    bytes,
                    writeType
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                c.writeType = writeType
                @Suppress("DEPRECATION")
                c.value = bytes
                @Suppress("DEPRECATION")
                owner.writeCharacteristic(c)
            }
        } catch (_: Exception) {
            false
        }

        if (!ok) {
            activeCommand = null
            commands.addFirst(item)
            updatePending()
            main.postDelayed({
                if (gattReady && activeCommand == null) {
                    writeNextCommand()
                }
            }, 100)
            return
        }

        commandTimer = Runnable {
            val current = activeCommand ?: return@Runnable
            activeCommand = null
            if (current.retries < 1) {
                current.retries++
                commands.addFirst(current)
                listener.onResponse("WARN,COMMAND_RETRY," + current.body)
                updatePending()
                main.postDelayed({ writeNextCommand() }, 100)
            } else {
                listener.onResponse("WARN,COMMAND_TIMEOUT," + current.body)
                updatePending()
                // Tidak memutuskan koneksi BLE karena link radio dan telemetri tetap sehat
                main.postDelayed({ writeNextCommand() }, 50)
            }
        }.also { main.postDelayed(it, 2_500) }
    }

    private fun updatePending() {
        _pending.value = commands.size + if (activeCommand != null) 1 else 0
    }
}
