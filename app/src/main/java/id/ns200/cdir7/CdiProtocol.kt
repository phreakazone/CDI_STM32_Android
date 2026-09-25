package id.ns200.cdir7

import java.util.zip.CRC32

enum class SetupStage(val code: Int, val label: String, val desc: String) {
    BARU(0, "BARU", "Cek catu daya & BLE; kill switch OFF -> ON. HV <30V. Charger & koil OFF"),
    PULSER(1, "PULSER", "Uji input pulser J1.10; PPR=1; gate 80µs; quality >=10"),
    TDC(2, "TDC", "Strobo PB9/GPIO27; sejajarkan tanda 'T'; SAVE TDC ke flash"),
    TPS_CAL(3, "TPS", "Simpan gas tertutup (0%) dan terbuka penuh (100%)"),
    FIRST_START(4, "FIRST START", "Mode aman 220V, CENTER saja, advance <=10°, limiter 3.000 RPM (Otomatis simpan 3s)"),
    READY(5, "READY", "Hidup stabil >=3 detik, simpan CENTER; boot berikutnya otomatis READY")
}

enum class FirmwareRunMode(val code: String, val label: String, val desc: String) {
    OEM_LEARN("OEM_LEARN", "OEM LEARN", "Membaca timing CDI OEM secara pasif melalui optocoupler"),
    MANUAL("MANUAL", "MANUAL", "Setup darurat strobo/TDC saat CDI OEM mati"),
    DIY("DIY", "DIY INDEPENDENT", "Operasi mandiri penuh setelah CDI OEM dicabut fisik");

    companion object {
        fun fromFirmwareCode(code: Int): FirmwareRunMode? = when (code) {
            0 -> MANUAL
            1 -> OEM_LEARN
            2 -> DIY
            else -> null
        }
    }
}

enum class OemLearnState(val code: Int) {
    IDLE(0), ACTIVE(1), COMPLETE(2), ERROR(3);

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code }
    }
}

enum class FirmwareOtaState(val code: Int) {
    IDLE(0), ERASING(1), RECEIVING(2), READY(3), ERROR(4);

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code }
    }
}

enum class HardwareModule(
    val bitMask: Int,
    val id: String,
    val title: String,
    val description: String,
    val pinInfo: String
) {
    SIDE(1, "SIDE", "Modul Koil Kedua (SIDE)", "Output kanal pengapian kedua bertingkat", "J1.6 (COIL_SIDE)"),
    THERMAL(2, "THERMAL", "Modul Sensor Suhu & Fan", "Sensor suhu NTC & kendali relay kipas", "J1.3 (NTC) & J1.7 (Relay)"),
    OEM_LEARN(4, "OEM_LEARN", "Modul Rekam OEM Learn", "Sadapan pasif pulsa optocoupler PC817", "GPIO16 & GPIO17"),
    AUX(8, "AUX", "Modul AUX (Strobe)", "Lampu stroboskop sinkronis TDC (Audio reserved)", "GPIO27 (Strobe)"),
    TPS_DIAG(16, "TPS_DIAG", "Modul TPS Diagnostic", "Memantau sinyal & referensi ADC TPS", "J1.4 & GPIO34 (TPS_REF)");

    companion object {
        // Alias kompatibilitas
        val DUAL_COIL = SIDE
        val THERMAL_FAN = THERMAL
    }
}

enum class SessionPhase(val displayName: String, val allowsWrite: Boolean) {
    DISCONNECTED("Terputus", false),
    SCANNING("Memindai BLE", false),
    CONNECTING("Menghubungkan GATT", false),
    SUBSCRIBING("Mendaftar Notifikasi", false),
    SYNCING("Sinkronisasi Firmware", false),
    NEEDS_BINDING("Menunggu Binding Serial", false),
    READY_READ_ONLY("Mode Terbatas (Read-Only)", false),
    READY_FULL("Siap Penuh (Izin Tulis Aktif)", true),
    DEGRADED("Koneksi Terdegradasi", false)
}

data class BindingRecord(
    val serial: String,
    val appInstanceId: String,
    val boundAtEpochMs: Long,
    val firmwareRelease: String,
    val vehicleName: String? = null
)

data class ModuleStatus(
    val installedMask: Int,
    val activeMask: Int,
    val observedMask: Int,
    val faultMask: Int,
    val coreProfile: Int
) {
    fun isInstalled(m: HardwareModule) = (installedMask and m.bitMask) != 0
    fun isActive(m: HardwareModule) = (activeMask and m.bitMask) != 0
    fun isObserved(m: HardwareModule) = (observedMask and m.bitMask) != 0
    fun hasFault(m: HardwareModule) = (faultMask and m.bitMask) != 0

    val profileLabel: String get() = when (coreProfile) {
        0 -> "IgniTra Core • 1 Coil"
        1 -> "Dual Coil (belum aktif)"
        2 -> "IgniTra Core + SIDE • Dual Coil"
        else -> "IgniTra Core • 1 Coil"
    }

    companion object {
        fun defaultCore() = ModuleStatus(
            installedMask = 0,
            activeMask = 0,
            observedMask = 0,
            faultMask = 0,
            coreProfile = 0
        )
    }
}

data class FirmwareVersionInfo(
    val schema: Int = 1,
    val release: String = "UNKNOWN",
    val semver: String = "0.0.0",
    val buildId: String = "0",
    val platform: String = "UNKNOWN",
    val protocolVersion: Int = 0,
    val telemetryVersion: Int = 0
) {
    val displayLabel: String get() = "Firmware $release v$semver • Build $buildId"
}

enum class ContactSource(val code: Int, val label: String) {
    OFF(0, "Kontak OFF"),
    MECHANICAL(1, "Kontak Mekanis"),
    KEYLESS(2, "Keyless");

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: OFF
    }
}

enum class TimingMode(
    val code: Int,
    val label: String,
    val description: String,
    val defaultIntensity: Int,
    val defaultMinRpm: Int,
    val defaultMaxRpm: Int
) {
    STANDARD(0, "STANDARD", "Idle normal tanpa pola tambahan", 0, 700, 1800),
    SOFT(1, "SOFT", "Retard tetap ringan sampai 4° untuk idle lebih lembut", 3, 750, 1650),
    RESPONSIVE(2, "RESPONSIVE", "Advance ringan sampai 2° pada rpm rendah", 4, 900, 2400),
    KUDA(3, "KUDA", "Rumble 8 langkah; retard bertingkat tanpa skip pada setelan bawaan", 6, 850, 1650),
    DRUMBAND(4, "DRUMBAND", "Ritme 12 langkah dengan aksen soft-cut terbatas", 7, 900, 1800),
    FOMO(5, "FOMO", "Ritme 10 langkah agresif dengan soft-cut terbatas", 8, 950, 2000),
    CUSTOM(6, "CUSTOM", "Pola 8 langkah dengan intensitas dan rentang pilihan pengguna", 5, 850, 1800);

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: STANDARD
    }
}

data class TimingStatus(
    val schema: Int = 2,
    val mode: TimingMode = TimingMode.STANDARD,
    val intensity: Int = 0,
    val minRpm: Int = 900,
    val maxRpm: Int = 1800
)

data class AuxStatus(
    val schema: Int = 3,
    val keylessOn: Boolean = false,
    val starterOn: Boolean = false,
    val present: Boolean = false,
    val enabled: Boolean = false,
    val vehicleProfile: Int = 0,
    val requestMask: Int = 0,
    val requestIoOk: Boolean = false,
    val mechanicalOn: Boolean = false,
    val contactSource: ContactSource = ContactSource.OFF,
    val engineRunning: Boolean = false,
    val ignitionAllowed: Boolean = false
) {
    val contactOn: Boolean get() = contactSource != ContactSource.OFF ||
        mechanicalOn || keylessOn || ignitionAllowed
}

data class FirmwareIdentityInfo(
    val schema: Int = 1,
    val serial: String = "UNAVAILABLE",
    val serialScheme: String = "SERIAL_V1",
    val bindingPolicy: String = "LOCAL_APP",
    val firmwareEnforced: Boolean = false
)

data class CommissionStatus(
    val stage: Int = 0,
    val nextAction: Int = 1,
    val ready: Boolean = false,
    val advisoryMask: Int = 0
) {
    val nextActionText: String get() = when (nextAction) {
        1 -> "Pilih Pemasangan Core/Dual"
        2 -> "Verifikasi Pickup / Starter"
        3 -> "Kalibrasi TDC Strobo"
        4 -> "Kalibrasi TPS (Closed & Open)"
        5 -> "First Start (Limit 3.000 RPM & 10°)"
        6 -> "Matikan Mesin & Konfirmasi Ready"
        7 -> "Sistem Siap (Ready)"
        8 -> "OEM Learn Lanjutan"
        else -> "Pemeriksaan Sistem"
    }

    val pickupAdvisory get() = (advisoryMask and 1) != 0
    val tdcAdvisory get() = (advisoryMask and 2) != 0
    val tpsAdvisory get() = (advisoryMask and 4) != 0
    val firstStartAdvisory get() = (advisoryMask and 8) != 0
    val sideAdvisory get() = (advisoryMask and 16) != 0
    val thermalAdvisory get() = (advisoryMask and 32) != 0
}

data class FirmwareTempStatus(
    val fanMode: String = "AUTO",
    val onX10: Int = 920,
    val offX10: Int = 860,
    val currentTempX10: Int = -32768,
    val valid: Boolean = false,
    val fanOutput: Boolean = false
) {
    val currentTempC: Float? get() = if (valid && currentTempX10 != -32768) currentTempX10 / 10f else null
}

data class AdcReadings(
    val tpsRaw: Int = 0,
    val tempRaw: Int = 0,
    val tpsRefRaw: Int = 0,
    val hvCenter: Int = 0,
    val hvSide: Int = 0,
    val vbatRaw: Int = 0,
    val hardwareFault: Boolean = false,
    val fanOutput: Boolean = false
)

data class FirmwareModeStatus(
    val mode: FirmwareRunMode,
    val diyUnplugged: Boolean,
    val proEnabled: Boolean,
    val firstStartProven: Boolean
)

data class OemLearnStatus(
    val state: OemLearnState,
    val coveragePercent: Int,
    val acceptedPulses: Int,
    val rejectedPulses: Int,
    val sideSamples: Int,
    val sideOffsetCdeg: Int
)

data class OtaStatusPacket(
    val state: FirmwareOtaState,
    val receivedBytes: Long,
    val expectedBytes: Long,
    val errorCode: Int
)

data class Telemetry(
    val sequence: Int, val rpm: Int, val tps: Int, val advanceCdeg: Int,
    val batteryCv: Int, val hvCenter: Int, val hvSide: Int, val tempCdeg: Int,
    val slot: Int, val limiter: Int, val flags: Int, val faults: Int,
    val setupStage: Int, val outputFlags: Int, val triggerCdeg: Int,
    val pickupQuality: Int, val firstStartSeconds: Int
) {
    val armed get() = flags and 0x01 != 0
    val proEnabled get() = flags and 0x02 != 0
    // Alias kompatibilitas; bit ini berasal dari setup.pro_enabled, bukan jumper.
    val proJumper get() = proEnabled
    val isProVoltage get() = proEnabled
    val hvEnabled get() = flags and 0x04 != 0
    val calibrated get() = flags and 0x08 != 0
    val bleLink get() = flags and 0x10 != 0
    val ready get() = flags and 0x20 != 0
    val firstStart get() = flags and 0x40 != 0
    val centerEnabled get() = outputFlags and 1 != 0
    val sideEnabled get() = outputFlags and 2 != 0
    val strobeEnabled get() = outputFlags and 4 != 0
    val fanEnabled get() = outputFlags and 8 != 0
    val stage get() = SetupStage.entries.find { it.code == setupStage } ?: SetupStage.BARU
    val isHvOver300 get() = hvCenter >= 300 || hvSide >= 300
    val isHvOverLimitWarning get() = hvCenter >= 360 || hvSide >= 360 // PRO R8 target is 345V
    val targetHvVoltage get() = if (isProVoltage) 345 else 285
}

data class ProtocolResponse(val sequence: Int, val body: String)

object CdiProtocol {
    const val SERVICE = "7a8f1000-6c9d-4e40-a45f-0b4b4e533230"
    const val TELEMETRY = "7a8f1001-6c9d-4e40-a45f-0b4b4e533230"
    const val COMMAND = "7a8f1002-6c9d-4e40-a45f-0b4b4e533230"
    const val RESPONSE = "7a8f1003-6c9d-4e40-a45f-0b4b4e533230"

    // R8 OTA UUIDs
    const val OTA_DATA = "7a8f1004-6c9d-4e40-a45f-0b4b4e533230"
    const val OTA_STATUS = "7a8f1005-6c9d-4e40-a45f-0b4b4e533230"
    const val OTA_CHUNK_MAX_SIZE = 208
    const val OTA_STATUS_SIZE = 16
    const val OTA_MIN_IMAGE_SIZE = 256
    const val OTA_MAX_IMAGE_SIZE = 0x00100000

    const val TELEMETRY_SIZE = 20
    const val VERSION_3 = 3 // R7 / v3
    const val VERSION_4 = 4 // R8 / v4
    const val VERSION = VERSION_3

    const val KIND_CORE = 0
    const val KIND_DIAGNOSTIC = 1

    // Voltage targets
    const val VOLTAGE_FIRST_START = 220
    const val VOLTAGE_NORMAL = 285
    const val VOLTAGE_PRO = 345

    /**
     * Firmware R7/R8 menyimpan 5 tahap (0..4), sedangkan aplikasi menampilkan
     * 6 halaman karena kalibrasi TPS dibuat sebagai langkah tersendiri.
     * Jangan pernah menampilkan angka tahap firmware secara langsung sebagai
     * SetupStage aplikasi.
     */
    fun wizardStageFromFirmware(
        firmwareStage: Int,
        tpsClosedAdc: Int,
        tpsOpenAdc: Int
    ): Int = when {
        firmwareStage >= 4 -> SetupStage.READY.code
        firmwareStage == 3 -> SetupStage.FIRST_START.code
        firmwareStage == 2 && tpsOpenAdc > tpsClosedAdc + 50 ->
            SetupStage.FIRST_START.code
        firmwareStage == 2 -> SetupStage.TPS_CAL.code
        firmwareStage == 1 -> SetupStage.TDC.code
        else -> SetupStage.BARU.code
    }

    fun emptyTelemetry() = Telemetry(0, 0, 0, 0, 0, 0, 0, Short.MIN_VALUE.toInt(),
        0, 0, 0, 0, SetupStage.BARU.code, 0, 6000, 0, 0)

    fun crc16(data: ByteArray, length: Int = data.size): Int {
        var crc = 0xffff
        repeat(length) { i ->
            crc = crc xor ((data[i].toInt() and 0xff) shl 8)
            repeat(8) { crc = ((crc shl 1) xor if (crc and 0x8000 != 0) 0x1021 else 0) and 0xffff }
        }
        return crc
    }

    fun crc32(data: ByteArray, offset: Int = 0, length: Int = data.size): Long {
        val crc = CRC32()
        crc.update(data, offset, length)
        return crc.value
    }

    fun command(sequence: Int, body: String): ByteArray {
        val payload = "$sequence,$body"
        return "@$payload*%04X\n".format(crc16(payload.toByteArray(Charsets.US_ASCII)))
            .toByteArray(Charsets.US_ASCII)
    }

    fun response(frame: String): ProtocolResponse? {
        val clean = frame.trim()
        val prefix = clean.firstOrNull() ?: return null
        val star = clean.lastIndexOf('*')
        if ((prefix == '@' || prefix == '$') && star > 1 && clean.length >= star + 5) {
            val payload = clean.substring(1, star)
            val supplied = clean.substring(star + 1, star + 5).toIntOrNull(16)
            if (supplied != null && crc16(payload.toByteArray(Charsets.US_ASCII)) == supplied) {
                val comma = payload.indexOf(',')
                return if (comma >= 1 && payload.substring(0, comma).toIntOrNull() != null) {
                    ProtocolResponse(payload.substring(0, comma).toInt(), payload.substring(comma + 1))
                } else {
                    ProtocolResponse(0, payload)
                }
            }
        }

        // Fallback untuk frame ASCII plain dari firmware (tanpa CRC atau log selftest)
        val stripped = clean.removePrefix("@").removePrefix("$")
        val withoutCrc = if (stripped.contains('*')) stripped.substringBefore('*') else stripped
        val comma = withoutCrc.indexOf(',')
        val seq = if (comma >= 1) withoutCrc.substring(0, comma).toIntOrNull() else null
        val body = if (seq != null) withoutCrc.substring(comma + 1) else withoutCrc
        if (body.isNotBlank()) {
            return ProtocolResponse(seq ?: 0, body)
        }
        return null
    }

    fun firmwareMode(body: String): FirmwareModeStatus? {
        val fields = body.split(',')
        if (fields.size < 2 || fields[0] != "MODE") return null
        val mode = when (fields[1].trim().uppercase()) {
            "0", "MANUAL" -> FirmwareRunMode.MANUAL
            "1", "OEM_LEARN", "LEARN" -> FirmwareRunMode.OEM_LEARN
            "2", "DIY" -> FirmwareRunMode.DIY
            else -> fields[1].toIntOrNull()?.let { FirmwareRunMode.fromFirmwareCode(it) }
        } ?: return null
        val unplugged = if (fields.size >= 3) (fields[2].trim().toIntOrNull() == 1 || fields[2].trim().equals("OEM_UNPLUGGED", true) || fields[2].trim().equals("TRUE", true)) else false
        val pro = if (fields.size >= 4) (fields[3].trim().toIntOrNull() == 1 || fields[3].trim().equals("PRO_ON", true) || fields[3].trim().equals("TRUE", true)) else false
        val proven = if (fields.size >= 5) (fields[4].trim().toIntOrNull() == 1 || fields[4].trim().equals("TRUE", true)) else false
        return FirmwareModeStatus(
            mode = mode,
            diyUnplugged = unplugged,
            proEnabled = pro,
            firstStartProven = proven
        )
    }

    fun oemLearnStatus(body: String): OemLearnStatus? {
        val fields = body.split(',')
        if (fields.size < 2 || fields[0] != "LEARN") return null
        val state = when (fields[1].trim().uppercase()) {
            "0", "IDLE" -> OemLearnState.IDLE
            "1", "ACTIVE" -> OemLearnState.ACTIVE
            "2", "COMPLETE" -> OemLearnState.COMPLETE
            "3", "ERROR" -> OemLearnState.ERROR
            else -> fields[1].toIntOrNull()?.let { OemLearnState.fromCode(it) } ?: OemLearnState.IDLE
        }
        val coveragePercent = fields.getOrNull(2)?.trim()?.toIntOrNull()?.coerceIn(0, 100) ?: 0
        val acceptedPulses = fields.getOrNull(3)?.trim()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val rejectedPulses = fields.getOrNull(4)?.trim()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val sideSamples = fields.getOrNull(5)?.trim()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val sideOffsetCdeg = fields.getOrNull(6)?.trim()?.toIntOrNull()?.coerceIn(-3000, 3000) ?: 0
        return OemLearnStatus(
            state = state,
            coveragePercent = coveragePercent,
            acceptedPulses = acceptedPulses,
            rejectedPulses = rejectedPulses,
            sideSamples = sideSamples,
            sideOffsetCdeg = sideOffsetCdeg
        )
    }

    private fun u16(a: ByteArray, offset: Int) =
        (a[offset].toInt() and 0xff) or ((a[offset + 1].toInt() and 0xff) shl 8)
    private fun u32(a: ByteArray, offset: Int): Long =
        (a[offset].toLong() and 0xffL) or
            ((a[offset + 1].toLong() and 0xffL) shl 8) or
            ((a[offset + 2].toLong() and 0xffL) shl 16) or
            ((a[offset + 3].toLong() and 0xffL) shl 24)
    private fun s16(a: ByteArray, offset: Int) = u16(a, offset).toShort().toInt()
    private fun put16(a: ByteArray, offset: Int, value: Int) {
        a[offset] = (value and 0xff).toByte(); a[offset + 1] = ((value ushr 8) and 0xff).toByte()
    }
    private fun put32(a: ByteArray, offset: Int, value: Int) {
        a[offset] = (value and 0xff).toByte()
        a[offset + 1] = ((value ushr 8) and 0xff).toByte()
        a[offset + 2] = ((value ushr 16) and 0xff).toByte()
        a[offset + 3] = ((value ushr 24) and 0xff).toByte()
    }

    fun otaDataPacket(offset: Int, payload: ByteArray): ByteArray {
        require(offset >= 0 && (offset and 7) == 0) { "Offset OTA wajib kelipatan 8" }
        require(payload.size in 1..OTA_CHUNK_MAX_SIZE) { "Payload OTA harus 1..$OTA_CHUNK_MAX_SIZE byte" }
        val out = ByteArray(5 + payload.size + 2)
        put32(out, 0, offset)
        out[4] = payload.size.toByte()
        payload.copyInto(out, 5)
        put16(out, out.size - 2, crc16(out, out.size - 2))
        return out
    }

    fun otaStatus(packet: ByteArray): OtaStatusPacket? {
        if (packet.size != OTA_STATUS_SIZE || u16(packet, 0) != 0xcd18 ||
            (packet[2].toInt() and 0xff) != 1 || crc16(packet, 14) != u16(packet, 14)
        ) return null
        val state = FirmwareOtaState.fromCode(packet[3].toInt() and 0xff) ?: return null
        return OtaStatusPacket(
            state = state,
            receivedBytes = u32(packet, 4),
            expectedBytes = u32(packet, 8),
            errorCode = u16(packet, 12)
        )
    }

    /** ESP32 R8 lama mengirim notifikasi OTA ringkas: state,error. */
    fun legacyEsp32OtaStatus(packet: ByteArray): Pair<FirmwareOtaState, Int>? {
        if (packet.size != 2) return null
        val state = FirmwareOtaState.fromCode(packet[0].toInt() and 0xff) ?: return null
        return state to (packet[1].toInt() and 0xff)
    }

    fun telemetry(packet: ByteArray, previous: Telemetry = emptyTelemetry()): Telemetry? {
        val ver = packet.getOrNull(2)?.toInt()?.and(0xff) ?: return null
        if (packet.size != TELEMETRY_SIZE || u16(packet, 0) != 0xcd15 ||
            (ver != VERSION_3 && ver != VERSION_4) ||
            (packet[3].toInt() and 0xff) !in KIND_CORE..KIND_DIAGNOSTIC ||
            crc16(packet, 18) != u16(packet, 18)) return null
        val sequence = u16(packet, 4)
        return if ((packet[3].toInt() and 0xff) == KIND_CORE) previous.copy(
            sequence = sequence, rpm = u16(packet, 6), tps = u16(packet, 8),
            advanceCdeg = s16(packet, 10), batteryCv = u16(packet, 12),
            hvCenter = u16(packet, 14), hvSide = u16(packet, 16)
        ) else previous.copy(
            sequence = sequence, tempCdeg = s16(packet, 6), slot = packet[8].toInt() and 0xff,
            limiter = packet[9].toInt() and 0xff, flags = packet[10].toInt() and 0xff,
            outputFlags = packet[11].toInt() and 0xff, faults = u16(packet, 12),
            triggerCdeg = u16(packet, 14), pickupQuality = packet[16].toInt() and 0xff,
            firstStartSeconds = packet[17].toInt() and 0xff
        )
    }

    fun packetFromTelemetry(t: Telemetry, kind: Int): ByteArray {
        val out = ByteArray(TELEMETRY_SIZE)
        put16(out, 0, 0xcd15); out[2] = VERSION.toByte(); out[3] = kind.toByte()
        put16(out, 4, t.sequence)
        if (kind == KIND_CORE) {
            put16(out, 6, t.rpm); put16(out, 8, t.tps); put16(out, 10, t.advanceCdeg)
            put16(out, 12, t.batteryCv); put16(out, 14, t.hvCenter); put16(out, 16, t.hvSide)
        } else {
            put16(out, 6, t.tempCdeg); out[8] = t.slot.toByte(); out[9] = t.limiter.toByte()
            out[10] = t.flags.toByte(); out[11] = t.outputFlags.toByte(); put16(out, 12, t.faults)
            put16(out, 14, t.triggerCdeg); out[16] = t.pickupQuality.toByte(); out[17] = t.firstStartSeconds.toByte()
        }
        put16(out, 18, crc16(out, 18)); return out
    }

    fun parseVersion(body: String): FirmwareVersionInfo? {
        val f = body.split(',')
        if (f.isEmpty() || f[0] != "VERSION") return null
        return FirmwareVersionInfo(
            schema = f.getOrNull(1)?.toIntOrNull() ?: 1,
            release = f.getOrNull(2)?.trim() ?: "R9",
            semver = f.getOrNull(3)?.trim() ?: "9.2.0",
            buildId = f.getOrNull(4)?.trim() ?: "20260923",
            platform = f.getOrNull(5)?.trim() ?: "ESP32",
            protocolVersion = f.getOrNull(6)?.toIntOrNull() ?: 5,
            telemetryVersion = f.getOrNull(7)?.toIntOrNull() ?: 3
        )
    }

    fun parseTiming(body: String): TimingStatus? {
        val f = body.split(',')
        if (f.size < 6 || f[0] != "TIMING") return null
        return TimingStatus(
            schema = f[1].toIntOrNull() ?: 1,
            mode = TimingMode.fromCode(f[2].toIntOrNull() ?: 0),
            intensity = (f[3].toIntOrNull() ?: 0).coerceIn(0, 10),
            minRpm = (f[4].toIntOrNull() ?: 900).coerceIn(500, 4000),
            maxRpm = (f[5].toIntOrNull() ?: 1800).coerceIn(600, 5000)
        )
    }

    fun parseAux(body: String): AuxStatus? {
        val f = body.split(',')
        if (f.size < 9 || f[0] != "AUX") return null
        val schema = f[1].toIntOrNull() ?: 2
        return AuxStatus(
            schema = schema,
            keylessOn = f[2] == "1",
            starterOn = f[3] == "1",
            present = f[4] == "1",
            enabled = f[5] == "1",
            vehicleProfile = f[6].toIntOrNull() ?: 0,
            requestMask = f[7].toIntOrNull() ?: 0,
            requestIoOk = f[8] == "1",
            mechanicalOn = schema >= 3 && f.getOrNull(9) == "1",
            contactSource = if (schema >= 3) ContactSource.fromCode(f.getOrNull(10)?.toIntOrNull() ?: 0) else
                if (f[2] == "1") ContactSource.KEYLESS else ContactSource.OFF,
            engineRunning = schema >= 3 && f.getOrNull(11) == "1",
            ignitionAllowed = schema >= 3 && f.getOrNull(12) == "1"
        )
    }

    fun parseIdentity(body: String): FirmwareIdentityInfo? {
        val f = body.split(',')
        if (f.isEmpty() || f[0] != "IDENTITY") return null
        return FirmwareIdentityInfo(
            schema = f.getOrNull(1)?.toIntOrNull() ?: 1,
            serial = f.getOrNull(2)?.trim() ?: "IGT-ESP32-UNKNOWN",
            serialScheme = f.getOrNull(3)?.trim() ?: "SERIAL_V1",
            bindingPolicy = f.getOrNull(4)?.trim() ?: "LOCAL_APP",
            firmwareEnforced = f.getOrNull(5)?.trim() == "1"
        )
    }

    fun parseModules(body: String): ModuleStatus? {
        val f = body.split(',')
        if (f.isEmpty() || f[0] != "MODULES") return null
        return ModuleStatus(
            installedMask = f.getOrNull(2)?.toIntOrNull() ?: 0,
            activeMask = f.getOrNull(3)?.toIntOrNull() ?: 0,
            observedMask = f.getOrNull(4)?.toIntOrNull() ?: 0,
            faultMask = f.getOrNull(5)?.toIntOrNull() ?: 0,
            coreProfile = f.getOrNull(6)?.toIntOrNull() ?: 0
        )
    }

    fun parseCommission(body: String): CommissionStatus? {
        val f = body.split(',')
        if (f.isEmpty() || f[0] != "COMMISSION") return null
        return CommissionStatus(
            stage = f.getOrNull(2)?.toIntOrNull() ?: 0,
            nextAction = f.getOrNull(3)?.toIntOrNull() ?: 1,
            ready = f.getOrNull(4)?.trim() == "1",
            advisoryMask = f.getOrNull(5)?.toIntOrNull() ?: 0
        )
    }

    fun parseAdc(body: String): AdcReadings? {
        val f = body.split(',')
        if (f.isEmpty() || f[0] != "ADC") return null
        return AdcReadings(
            tpsRaw = f.getOrNull(1)?.toIntOrNull() ?: 0,
            tempRaw = f.getOrNull(2)?.toIntOrNull() ?: 0,
            tpsRefRaw = f.getOrNull(3)?.toIntOrNull() ?: 0,
            hvCenter = f.getOrNull(4)?.toIntOrNull() ?: 0,
            hvSide = f.getOrNull(5)?.toIntOrNull() ?: 0,
            vbatRaw = f.getOrNull(6)?.toIntOrNull() ?: 0,
            hardwareFault = f.getOrNull(7)?.toIntOrNull() == 1,
            fanOutput = f.getOrNull(8)?.toIntOrNull() == 1
        )
    }

    fun parseTemp(body: String): FirmwareTempStatus? {
        val f = body.split(',')
        if (f.isEmpty() || f[0] != "TEMP") return null
        val rawMode = f.getOrNull(1)?.trim()?.uppercase() ?: "AUTO"
        val mode = when (rawMode) {
            "0", "OFF" -> "OFF"
            "1", "ON" -> "ON"
            else -> "AUTO"
        }
        val onX10 = f.getOrNull(2)?.toIntOrNull() ?: 920
        val offX10 = f.getOrNull(3)?.toIntOrNull() ?: 860
        val currentTempX10 = f.getOrNull(4)?.toIntOrNull() ?: -32768
        val valid = f.getOrNull(5)?.trim() == "1" || (currentTempX10 != -32768)
        val fanOutput = f.getOrNull(6)?.trim() == "1"
        return FirmwareTempStatus(
            fanMode = mode,
            onX10 = onX10,
            offX10 = offX10,
            currentTempX10 = currentTempX10,
            valid = valid,
            fanOutput = fanOutput
        )
    }

    fun parseHardware(body: String): List<String> {
        val f = body.split(',')
        if (f.isEmpty() || (f[0] != "HARDWARE" && f[0] != "HW")) return emptyList()
        return f.drop(2).map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun toHexDump(bytes: ByteArray) = bytes.joinToString(" ") { "%02X".format(it) }
}
