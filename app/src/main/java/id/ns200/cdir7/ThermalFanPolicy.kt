package id.ns200.cdir7

data class ThermalFanSettings(val mode:String,val onCdeg:Int,val offCdeg:Int)
object ThermalFanPolicy{
    const val MIN_OFF_CDEG=5000
    const val MAX_ON_CDEG=15000
    const val MIN_HYSTERESIS_CDEG=300
    fun normalize(mode:String,onCdeg:Int,offCdeg:Int):ThermalFanSettings{
        val m=when(mode.uppercase()){"ON"->"ON";"AUTO"->"AUTO";else->"OFF"}
        val on=onCdeg.coerceIn(MIN_OFF_CDEG+MIN_HYSTERESIS_CDEG,MAX_ON_CDEG)
        return ThermalFanSettings(m,on,offCdeg.coerceIn(MIN_OFF_CDEG,on-MIN_HYSTERESIS_CDEG))
    }
    fun output(s:ThermalFanSettings,tempCdeg:Int,sensorFault:Boolean,previous:Boolean)=when(s.mode){
        "ON"->true
        "AUTO"->when{sensorFault->true;tempCdeg>=s.onCdeg->true;tempCdeg<=s.offCdeg->false;else->previous}
        else->false
    }
}
