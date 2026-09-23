package id.ns200.cdir7

data class FirmwareCapabilities(
    val protocolVersion:Int,val rpmMin:Int,val rpmMax:Int,
    val advanceMinDeg:Float,val advanceMaxDeg:Float,
    val maxRpmPoints:Int,val maxLoadPoints:Int,val mapSlots:Int,
    val maxPulserPpr:Int,val features:Set<String>
){
    companion object{
        fun legacyR8()=FirmwareCapabilities(3,3000,11500,0f,36f,16,8,4,1,setOf("LEGACY_R8"))
        fun demoR9()=FirmwareCapabilities(5,300,30000,-30f,80f,32,16,4,12,
            setOf("FAN","TEMP3","DYNO","PROFILE","OTA"))
        fun parse(f:List<String>):FirmwareCapabilities{
            if(f.size>=10&&f[1].toIntOrNull()!=null){
                return FirmwareCapabilities(
                    f[1].toIntOrNull()?.coerceAtLeast(1)?:5,300,
                    f[2].toIntOrNull()?.coerceIn(1000,30000)?:30000,
                    ((f[3].toIntOrNull()?:-300)/10f).coerceAtLeast(-30f),
                    ((f[4].toIntOrNull()?:800)/10f).coerceAtMost(80f),
                    f[5].toIntOrNull()?.coerceIn(2,32)?:32,
                    f[6].toIntOrNull()?.coerceIn(1,16)?:16,
                    f[7].toIntOrNull()?.coerceIn(1,4)?:4,
                    f[8].toIntOrNull()?.coerceIn(1,12)?:12,
                    f.drop(9).flatMap{it.split('|')}.map{it.trim().uppercase()}.filter{it.isNotEmpty()}.toSet())
            }
            return legacyR8().copy(features=f.drop(1).map{it.trim().uppercase()}.toSet())
        }
    }
}

data class EngineProfile(
    val name: String,
    val rpmMin: Int,
    val rpmMax: Int,
    val advanceMinDeg: Float,
    val advanceMaxDeg: Float,
    val pulserPpr: Int,
    val triggerAngleDeg: Float,
    val description: String = "Profil Karakteristik & Limit Mesin"
) {
    fun clamped(c: FirmwareCapabilities): EngineProfile {
        val minRpm = rpmMin.coerceIn(c.rpmMin, c.rpmMax)
        val minAdvance = advanceMinDeg.coerceIn(c.advanceMinDeg, c.advanceMaxDeg)
        return copy(
            name = name.uppercase().replace(Regex("[^A-Z0-9_]"), "_").take(19).ifBlank { "STD_STREET" },
            rpmMin = minRpm,
            rpmMax = rpmMax.coerceIn(minRpm, c.rpmMax),
            advanceMinDeg = minAdvance,
            advanceMaxDeg = advanceMaxDeg.coerceIn(minAdvance, c.advanceMaxDeg),
            pulserPpr = pulserPpr.coerceIn(1, c.maxPulserPpr),
            triggerAngleDeg = triggerAngleDeg.coerceIn(0f, c.advanceMaxDeg)
        )
    }

    companion object {
        fun standardStreet() = EngineProfile(
            name = "STD_STREET",
            rpmMin = 800,
            rpmMax = 10500,
            advanceMinDeg = 0f,
            advanceMaxDeg = 36f,
            pulserPpr = 1,
            triggerAngleDeg = 35f,
            description = "Profil Standar Harian: Efisien, aman untuk bahan bakar umum, limiter 10.500 RPM."
        )

        fun touringEndurance() = EngineProfile(
            name = "TOURING",
            rpmMin = 800,
            rpmMax = 11500,
            advanceMinDeg = 0f,
            advanceMaxDeg = 38f,
            pulserPpr = 1,
            triggerAngleDeg = 35f,
            description = "Profil Touring Jarak Jauh: Respon putaran menengah stabil, limiter 11.500 RPM."
        )

        fun highRevRacing() = EngineProfile(
            name = "HIGH_REV",
            rpmMin = 1000,
            rpmMax = 15000,
            advanceMinDeg = -5f,
            advanceMaxDeg = 42f,
            pulserPpr = 1,
            triggerAngleDeg = 35f,
            description = "Profil Sirkuit / Balap: Putaran tinggi responsif, limiter hingga 15.000 RPM."
        )

        fun customEngine() = EngineProfile(
            name = "CUSTOM",
            rpmMin = 800,
            rpmMax = 12000,
            advanceMinDeg = -5f,
            advanceMaxDeg = 40f,
            pulserPpr = 1,
            triggerAngleDeg = 35f,
            description = "Profil Pengaturan Mandiri: Sesuai spesifikasi mesin kustom."
        )

        // Aliases untuk backward compatibility
        fun core1Coil() = standardStreet()
        fun dualCoil() = highRevRacing()
        fun thermalFan() = touringEndurance()
        fun proTrack() = highRevRacing()
        fun oemReference() = standardStreet()
        fun universal() = standardStreet()
        fun ns200() = touringEndurance()

        val ALL_PROFILES = listOf(
            standardStreet(),
            touringEndurance(),
            highRevRacing(),
            customEngine()
        )
        val ALL_MODULAR_PROFILES = ALL_PROFILES

        fun parse(f: List<String>): EngineProfile? {
            if (f.size < 8) return null
            return EngineProfile(
                name = f[1],
                rpmMin = f[2].toIntOrNull() ?: return null,
                rpmMax = f[3].toIntOrNull() ?: return null,
                advanceMinDeg = (f[4].toIntOrNull() ?: return null) / 10f,
                advanceMaxDeg = (f[5].toIntOrNull() ?: return null) / 10f,
                pulserPpr = f[6].toIntOrNull() ?: return null,
                triggerAngleDeg = (f[7].toIntOrNull() ?: return null) / 10f
            )
        }
    }
}
