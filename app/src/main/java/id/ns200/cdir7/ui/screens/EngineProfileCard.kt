package id.ns200.cdir7.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.ns200.cdir7.CdiViewModel
import id.ns200.cdir7.EngineProfile
@Composable fun EngineProfileCard(vm:CdiViewModel){
 val p by vm.engineProfile.collectAsState();val c by vm.firmwareCapabilities.collectAsState()
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
  Text("ENGINE PROFILE • "+p.name,style=MaterialTheme.typography.titleSmall)
  Text("${p.rpmMin}-${p.rpmMax} RPM • ${p.advanceMinDeg}..${p.advanceMaxDeg}° • PPR ${p.pulserPpr} • trigger ${p.triggerAngleDeg}°")
  Text("Firmware v${c.protocolVersion}: map ${c.maxRpmPoints}x${c.maxLoadPoints}, ${c.mapSlots} slot")
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   OutlinedButton({vm.setEngineProfile(EngineProfile.ns200())}){Text("NS200 BASE")}
   OutlinedButton({vm.setEngineProfile(EngineProfile.universal())}){Text("UNIVERSAL")}}
  Text("Batas profil bukan jaminan mekanis mesin/koil.",style=MaterialTheme.typography.bodySmall)
 }}}
