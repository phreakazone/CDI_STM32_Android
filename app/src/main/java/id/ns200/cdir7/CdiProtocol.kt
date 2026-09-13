package id.ns200.cdir7

import java.util.zip.CRC32

enum class SetupStage(val code: Int, val label: String, val desc: String) {
    BARU(0, "BARU", "Cek catu daya & BLE; kill switch OFF -> ON. HV <30V. Charger & koil OFF"),
    PULSER(1, "PULSER", "Uji input pulser J1.10; PPR=1; gate 80µs; quality >=10"),
    TDC(2, "TDC", "Strobo PB9; sejajarkan tanda 'T'; SAVE TDC ke flash"),
    TPS_CAL(3, "TPS", "Simpan gas tertutup (0%) dan terbuka penuh (100%)"),
    FIRST_START(4, "FIRST START", "Mode aman 220V, CENTER saja, advance <=10°, limiter 3.000 RPM (Otomatis simpan 3s)"),
    READY(5, "READY", "Hidup stabil >=3 detik, simpan CENTER; boot berikutnya otomatis READY")
}

enum class FirmwareRunMode(val code: String, val label: String, val desc: String) {
    OEM_LEARN("OEM_LEARN", "OEM LEARN", "Membaca timing CDI OEM secara pasif via optocoupler (PB3/PB4 STM32 atau GPIO16/17 ESP32)"),
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
    // Alias dipertahankan agar UI lintas-platform 8.3.1 tidak perlu dirombak.
    // Pada firmware R8 bit ini berasal dari setup.pro_enabled, bukan jumper.
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
    const val OTA_IMAGE_VERSION = 80200L
    const val OTA_MIN_IMAGE_SIZE = 256
    const val OTA_MAX_IMAGE_SIZE = 0x00030000

    const val TELEMETRY_SIZE = 20
    const val VERSION_3 = 3 // R7 / v3
    const val VERSION_4 = 4 // R8 / v4
    const val VERSION = 4

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

    fun emptyTelemetry() = Telemetry(0, 0, 0, 0, 0, 0, 0, 0,
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
        if (prefix != '@' && prefix != '$') return null
        val star = clean.lastIndexOf('*')
        if (star <= 1 || clean.length < star + 5) return null
        val payload = clean.substring(1, star)
        val supplied = clean.substring(star + 1, star + 5).toIntOrNull(16) ?: return null
        if (crc16(payload.toByteArray(Charsets.US_ASCII)) != supplied) return null
        val comma = payload.indexOf(',')
        if (comma < 1) return null
        return ProtocolResponse(payload.substring(0, comma).toIntOrNull() ?: return null,
            payload.substring(comma + 1))
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

    fun toHexDump(bytes: ByteArray) = bytes.joinToString(" ") { "%02X".format(it) }
}
