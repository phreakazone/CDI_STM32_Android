import fs from 'node:fs';
import path from 'node:path';

const root = path.dirname(new URL(import.meta.url).pathname);
const out = path.join(root, 'generated');
const unit = mm => +(mm * 3.937007874).toFixed(3);
const close = (a,b,tolerance=0.02) => Math.abs(a-b) <= tolerance;
const fail = message => { throw new Error(message); };
const check = (condition,message) => { if (!condition) fail(message); };

function pads(board) {
  return board.shape.flatMap(shape => shape.split('#@$'))
    .filter(shape => shape.startsWith('PAD~'))
    .map(shape => {
      const p=shape.split('~');
      return {x:+p[2],y:+p[3],width:+p[4],height:+p[5],layer:p[6],net:p[7],pin:p[8],hole:+p[9]};
    });
}

function padAt(all,net,xMm,yMm) {
  return all.find(p => p.net===net && close(p.x,unit(xMm)) && close(p.y,unit(yMm)));
}

const files=['IGNITRA_CDI_ESP32_2L_EASYEDA.json','IGNITRA_CDI_ESP32_1L_EASYEDA.json'];
const requiredNets=[
  'TPS_ADC','TEMP_ADC','TPS_REF_ADC','HV_C_ADC','HV_S_ADC','VBAT_ADC',
  'GATE_C','GATE_S','STROBE','FAULT_N','FAN_CTL','PWM_A','PWM_B',
  'OEM_SIDE','OEM_CENTER','PICKUP_DIG','COIL_SIDE','COIL_CENTER'
];

for (const file of files) {
  const board=JSON.parse(fs.readFileSync(path.join(out,file),'utf8'));
  check(board.head && typeof board.head==='object',`${file}: head harus object EasyEDA, bukan string legacy`);
  check(board.head.docType==='3',`${file}: head.docType harus string "3"`);
  check(board.head.editorVersion==='6.5.51',`${file}: editorVersion tidak sama dengan format referensi`);
  check(Array.isArray(board.shape),`${file}: shape bukan array`);
  check(Array.isArray(board.layers) && board.layers.some(x=>x.startsWith('10~BoardOutLine~')),`${file}: layer BoardOutLine tidak valid`);
  check(Array.isArray(board.objects) && board.objects.includes('Pad~true~true'),`${file}: daftar objek editor tidak lengkap`);
  check(board.DRCRULE?.Default && Number.isFinite(board.DRCRULE.Default.clearance),`${file}: skema DRCRULE modern tidak ada`);
  check(Array.isArray(board.routerRule?.routerLayers),`${file}: routerRule tidak ada`);
  check(board.netColors && typeof board.netColors==='object',`${file}: netColors tidak ada`);
  check(typeof board.head.routingStatus==='string',`${file}: status routing tidak ada`);
  check(close(board.BBox.width,unit(230)),`${file}: lebar outline bukan 230 mm`);
  check(close(board.BBox.height,unit(165)),`${file}: tinggi outline bukan 165 mm`);
  check(board.shape.some(s=>s.startsWith('TRACK~1~10~~')),`${file}: BoardOutline tidak ditemukan`);
  check(board.shape.filter(s=>s.startsWith('SOLIDREGION~10~~')).length===4,`${file}: jumlah slot NPTH harus 4`);
  const copperTracks=board.shape.filter(s=>s.startsWith('TRACK~') && s.split('~')[3]);
  const vias=board.shape.filter(s=>s.startsWith('VIA~'));
  check(copperTracks.length>=300,`${file}: copper track belum lengkap (${copperTracks.length})`);
  check(vias.length>=1,`${file}: via routing tidak ditemukan`);

  const all=pads(board);
  check(all.length>=381,`${file}: jumlah pad terlalu sedikit (${all.length})`);
  for (const net of requiredNets) check(all.some(p=>p.net===net),`${file}: net ${net} tidak memiliki pad`);

  // J1 low-voltage group and isolated pulse-output pads.
  check(padAt(all,'TPS_A',122.54,148),`${file}: J1.2 TPS_A salah posisi`);
  check(padAt(all,'IGN_12V',130.16,148),`${file}: J1.5 IGN_12V salah posisi`);
  check(padAt(all,'COIL_SIDE',145.4,148),`${file}: J1.6 COIL_SIDE salah posisi`);
  check(padAt(all,'GND_STAR',130.16,158.16),`${file}: J1.11 GND_STAR salah posisi`);
  check(padAt(all,'COIL_CENTER',145.4,158.16),`${file}: J1.12 COIL_CENTER salah posisi`);

  // T1: 11-position primary row, rear outputs 10 pitches apart, one empty pad per side.
  check(padAt(all,'LV_A',152.54,58),`${file}: T1 LV_A salah posisi`);
  check(padAt(all,'VIN_HV',165.24,58),`${file}: T1 center tap salah posisi`);
  check(padAt(all,'LV_B',177.94,58),`${file}: T1 LV_B salah posisi`);
  check(padAt(all,'HV_AC1',152.54,83.4),`${file}: T1 HV_AC1 salah posisi`);
  check(padAt(all,'HV_AC2',177.94,83.4),`${file}: T1 HV_AC2 salah posisi`);
  check(all.some(p=>p.net==='' && close(p.x,unit(150)) && close(p.y,unit(83.4))),`${file}: pad kosong kiri T1 tidak ada`);
  check(all.some(p=>p.net==='' && close(p.x,unit(180.48)) && close(p.y,unit(83.4))),`${file}: pad kosong kanan T1 tidak ada`);

  // The 1 uF MKP bodies use a 9 x 4-hole envelope and 8-pitch lead spacing.
  for (const [a,b,y] of [['HV_CENTER','COIL_CENTER',43.08],['HV_SIDE','COIL_SIDE',97.08]]) {
    const left=all.find(p=>p.net===a && close(p.y,unit(y)) && close(p.width,unit(4)));
    const right=all.find(p=>p.net===b && close(p.y,unit(y)) && close(p.width,unit(4)));
    check(left&&right&&close(right.x-left.x,unit(20.32)),`${file}: pitch kapasitor ${a} bukan 20.32 mm`);
  }

  console.log(`${file}: OK — ${all.length} pads, ${new Set(all.map(p=>p.net).filter(Boolean)).size} nets, ${copperTracks.length} tracks, ${vias.length} vias`);
}

const routing=JSON.parse(fs.readFileSync(path.join(out,'ROUTING_REPORT.json'),'utf8'));
check(routing.two_layer.failed.length===0,'ROUTING_REPORT: koneksi 2L masih gagal');
check(routing.single_layer.failed.length===0,'ROUTING_REPORT: koneksi 1L masih gagal');
check(routing.single_layer.jumpers>0,'ROUTING_REPORT: rencana jumper 1L tidak ada');
console.log(`ROUTING_REPORT.json: OK — 2L failed=0; 1L failed=0; jumper segments=${routing.single_layer.jumpers}`);

const diagram=JSON.parse(fs.readFileSync(path.join(out,'IGNITRA_CDI_ESP32_BLOCK_DIAGRAM.json'),'utf8'));
check(diagram.docType==='5' && diagram.editorVersion==='6.5.51','Block diagram: wrapper project EasyEDA tidak valid');
check(Array.isArray(diagram.schematics) && diagram.schematics.length===1,'Block diagram: sheet tidak ada');
check(diagram.schematics[0].dataStr?.head?.docType==='1','Block diagram: dataStr.head.docType harus "1"');
console.log('IGNITRA_CDI_ESP32_BLOCK_DIAGRAM.json: OK — EasyEDA project wrapper docType 5');

const netlist=fs.readFileSync(path.join(out,'NETLIST.csv'),'utf8').trim().split(/\r?\n/);
check(netlist.length===395,`NETLIST.csv: diharapkan 394 koneksi, ditemukan ${netlist.length-1}`);
console.log(`NETLIST.csv: OK — ${netlist.length-1} logical pin connections`);
