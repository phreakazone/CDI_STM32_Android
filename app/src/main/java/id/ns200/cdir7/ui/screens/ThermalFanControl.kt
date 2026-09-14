package id.ns200.cdir7.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.ns200.cdir7.CdiViewModel

@Composable fun ThermalFanQuickAccess(vm:CdiViewModel){
    val mode by vm.fanMode.collectAsState();val on by vm.fanOnCdeg.collectAsState()
    val off by vm.fanOffCdeg.collectAsState();val t by vm.telemetry.collectAsState()
    val demo by vm.isSimulationMode.collectAsState()
    Card(Modifier.fillMaxWidth().padding(10.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF172026))){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
            Text("SUHU & FAN CONTROL",fontWeight=FontWeight.Bold,color=Color(0xFF55DDE0))
            Text("Suhu: "+if(t.tempCdeg==Short.MIN_VALUE.toInt())"--" else "%.1f°C".format(t.tempCdeg/100f))
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("OFF","ON","AUTO").forEach{
                Button(onClick={vm.setFanMode(it)},colors=ButtonDefaults.buttonColors(
                    containerColor=if(mode==it)Color(0xFFFF8A00)else Color(0xFF33434D))){Text(it)}}}
            Text("Fan ON: "+on/100f+"°C");Slider(on.toFloat(),{vm.setFanThresholds(it.toInt(),off)},valueRange=5300f..15000f)
            Text("Fan OFF: "+off/100f+"°C");Slider(off.toFloat(),{vm.setFanThresholds(on,it.toInt())},valueRange=5000f..(on-300).coerceAtLeast(5000).toFloat())
            if(demo){Text("Simulasi suhu");Slider((if(t.tempCdeg<0)2500 else t.tempCdeg).toFloat(),{vm.setDemoTemperature(it/100f)},valueRange=2000f..13000f)}
            Text("Gunakan relay/driver low-side + flyback; fan tidak boleh langsung ke pin MCU.",style=MaterialTheme.typography.bodySmall)
        }
    }
}
