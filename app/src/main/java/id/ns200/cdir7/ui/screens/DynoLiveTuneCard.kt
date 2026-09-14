package id.ns200.cdir7.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.ns200.cdir7.CdiViewModel
@Composable fun DynoLiveTuneCard(vm:CdiViewModel){
 val active by vm.dynoActive.collectAsState();val trim by vm.dynoTrimDeg.collectAsState()
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
  Text("LIVE REMAP / DYNO",style=MaterialTheme.typography.titleSmall);Text("Trim advance: %.1f°".format(trim))
  Slider(trim,vm::setDynoTrim,valueRange=-10f..10f,enabled=active)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   Button(vm::beginDynoTune,enabled=!active){Text("MULAI")}
   Button({vm.finishDynoTune(true)},enabled=active){Text("COMMIT")}
   OutlinedButton({vm.finishDynoTune(false)},enabled=active){Text("ABORT")}}
  Text("Mulai/commit hanya RPM 0 dan HV <30 V.",style=MaterialTheme.typography.bodySmall)
 }}}
