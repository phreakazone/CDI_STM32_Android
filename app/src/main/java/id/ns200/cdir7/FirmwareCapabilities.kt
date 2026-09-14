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
    val name:String,val rpmMin:Int,val rpmMax:Int,val advanceMinDeg:Float,
    val advanceMaxDeg:Float,val pulserPpr:Int,val triggerAngleDeg:Float
){
    fun clamped(c:FirmwareCapabilities):EngineProfile{
        val minRpm=rpmMin.coerceIn(c.rpmMin,c.rpmMax)
        val minAdvance=advanceMinDeg.coerceIn(c.advanceMinDeg,c.advanceMaxDeg)
        return copy(name=name.uppercase().replace(Regex("[^A-Z0-9_]"),"_").take(19).ifBlank{"UNIVERSAL"},
            rpmMin=minRpm,rpmMax=rpmMax.coerceIn(minRpm,c.rpmMax),
            advanceMinDeg=minAdvance,advanceMaxDeg=advanceMaxDeg.coerceIn(minAdvance,c.advanceMaxDeg),
            pulserPpr=pulserPpr.coerceIn(1,c.maxPulserPpr),triggerAngleDeg=triggerAngleDeg.coerceIn(0f,c.advanceMaxDeg))
    }
    companion object{
        fun universal()=EngineProfile("UNIVERSAL_BASE",300,22000,-15f,60f,1,35f)
        fun ns200()=EngineProfile("NS200_BASE",800,11500,0f,36f,1,35f)
        fun parse(f:List<String>):EngineProfile?{
            if(f.size<8)return null
            return EngineProfile(f[1],f[2].toIntOrNull()?:return null,f[3].toIntOrNull()?:return null,
                (f[4].toIntOrNull()?:return null)/10f,(f[5].toIntOrNull()?:return null)/10f,
                f[6].toIntOrNull()?:return null,(f[7].toIntOrNull()?:return null)/10f)
        }
    }
}
