import fs from 'node:fs';
import path from 'node:path';

const root = path.dirname(new URL(import.meta.url).pathname);
const out = path.join(root, 'generated');
fs.mkdirSync(out, { recursive: true });

const mm = value => +(value * 3.937007874).toFixed(3); // EasyEDA Std uses 10 mil units.
let id = 1000;
const gid = () => `gge${id++}`;

const espLeft = [
  ['1','3V3'],['2','EN'],['3','TPS_ADC'],['4','TEMP_ADC'],['5','TPS_REF_ADC'],
  ['6','HV_C_ADC'],['7','HV_S_ADC'],['8','VBAT_ADC'],['9','GATE_C'],['10','GATE_S'],
  ['11','STROBE'],['12','FAULT_N'],['13','NC'],['14','GND_LOGIC'],['15','FAN_CTL'],
  ['16','NC'],['17','NC'],['18','NC'],['19','5V_LOGIC']
];
const espRight = [
  ['1','GND_LOGIC'],['2','NC'],['3','NC'],['4','UART_TX'],['5','UART_RX'],['6','NC'],
  ['7','GND_LOGIC'],['8','PWM_B'],['9','PWM_A'],['10','BENCH_LOOP'],['11','OEM_SIDE'],
  ['12','OEM_CENTER'],['13','PICKUP_DIG'],['14','BOOT'],['15','NC'],['16','NC'],
  ['17','NC'],['18','NC'],['19','NC']
];

const components = [
  {ref:'J1',value:'NS200 HARNESS SAFE SOLDER 2x6',kind:'harness2x6_safe',x:120,y:148,pins:[
    ['1','NC'],['2','TPS_A'],['3','TEMP_SENSOR'],['4','TPS_B'],['5','IGN_12V'],['6','COIL_SIDE'],
    ['7','FAN_RELAY'],['8','OEM_SIDE_PROBE'],['9','OEM_CENTER_PROBE'],['10','PICKUP_RAW'],['11','GND_STAR'],['12','COIL_CENTER']
  ]},
  {ref:'U1L',value:'ESP32 DEVKITC LEFT 1x19',kind:'single',x:12,y:8,pitch:2.54,pins:espLeft},
  {ref:'U1R',value:'ESP32 DEVKITC RIGHT 1x19',kind:'single',x:37.4,y:8,pitch:2.54,pins:espRight},
  {ref:'FMAIN',value:'5A',kind:'axial',x:25,y:155,pins:[['1','IGN_12V'],['2','VIN_FUSED']]},
  {ref:'DREV',value:'SB560',kind:'axial',x:39,y:155,pins:[['A','VIN_FUSED'],['K','VIN_PROT']]},
  {ref:'TVS_IN',value:'SMBJ33A/P6KE33A',kind:'axial',x:53,y:148,pins:[['A','GND_STAR'],['K','VIN_PROT']]},
  {ref:'L_IN',value:'47uH 5A',kind:'axial',x:53,y:155,pins:[['1','VIN_PROT'],['2','VIN_FILT']]},
  {ref:'C_IN',value:'470uF 35V',kind:'axial',x:67,y:148,pins:[['-','GND_STAR'],['+','VIN_FILT']]},
  {ref:'FLOGIC',value:'1A',kind:'axial',x:67,y:155,pins:[['1','VIN_FILT'],['2','VIN_LOGIC']]},
  {ref:'UBUCK',value:'MP1584 MODULE',kind:'quad',x:82,y:152,pins:[['1','VIN_LOGIC'],['2','GND_LOGIC'],['3','5V_LOGIC'],['4','GND_LOGIC']]},
  {ref:'FHV',value:'3A',kind:'axial',x:103,y:138,pins:[['1','VIN_FILT'],['2','VIN_HV_FUSED']]},
  {ref:'SW1',value:'SERVICE HV',kind:'axial',x:118,y:138,pins:[['1','VIN_HV_FUSED'],['2','VIN_HV']]},
  {ref:'NT1',value:'GROUND STAR LINK',kind:'axial',x:91,y:155,pins:[['1','GND_STAR'],['2','GND_POWER']]},
  {ref:'NT2',value:'LOGIC STAR LINK',kind:'axial',x:91,y:148,pins:[['1','GND_STAR'],['2','GND_LOGIC']]},

  {ref:'U2',value:'LM339N',kind:'dip',x:72,y:12,pitch:2.54,row:7.62,pins:[
    ['1','FAULT_N'],['2','PICKUP_OC'],['3','5V_LOGIC'],['4','PICKUP_ZERO'],['5','PICKUP_SENSE'],
    ['6','ISENSE_FILTERED'],['7','IREF'],['8','HV_C_FB'],['9','VOV_REF'],['10','HV_S_FB'],['11','VOV_REF'],
    ['12','GND_LOGIC'],['13','FAULT_N'],['14','FAULT_N']
  ]},
  {ref:'U3',value:'PC817 CENTER',kind:'quad',x:70,y:45,pins:[['1','OEM_C_LED_A'],['2','GND_POWER'],['3','GND_LOGIC'],['4','OEM_CENTER']]},
  {ref:'U4',value:'PC817 SIDE',kind:'quad',x:82,y:45,pins:[['1','OEM_S_LED_A'],['2','GND_POWER'],['3','GND_LOGIC'],['4','OEM_SIDE']]},
  {ref:'JTPS',value:'TPS SELECT 2x3',kind:'dual',x:55,y:45,pitch:2.54,row:7.62,pins:[
    ['1','TPS_A'],['2','TPS_REF_RAW'],['3','TPS_B'],['4','TPS_B'],['5','TPS_SIG_RAW'],['6','TPS_A']
  ]},
  {ref:'QFAN',value:'BC337-40',kind:'triple',x:55,y:57,pins:[['C','FAN_RELAY'],['B','FAN_BASE'],['E','GND_POWER']]},
  {ref:'DFAN',value:'1N4007',kind:'axial',x:66,y:57,pins:[['A','FAN_RELAY'],['K','VIN_PROT']]},
  {ref:'QSTROBE',value:'FQP30N06L / IRLZ44N',kind:'triple',x:5,y:60,pins:[['G','STROBE_GATE'],['D','STROBE_NEG'],['S','GND_POWER']]},
  {ref:'JSTROBE',value:'LED STROBE 5V 1x2',kind:'header1x2',x:5,y:70,pins:[['1','5V_LOGIC'],['2','STROBE_NEG']]},
  {ref:'JAUDIO',value:'AUDIO EXPANSION 1x2',kind:'header1x2',x:5,y:80,pins:[['1','VIN_PROT'],['2','GND_STAR']]},
  {ref:'JUART',value:'UART SERVICE 1x4',kind:'header1x4',x:20,y:70,pins:[['1','5V_LOGIC'],['2','GND_LOGIC'],['3','UART_TX'],['4','UART_RX']]},
  {ref:'JBENCH',value:'BENCH LOOP ONLY 1x2',kind:'header1x2',x:20,y:82,pins:[['1','GATE_C'],['2','BENCH_LOOP']]},

  {ref:'DBAT_BAT',value:'BAT54S / DUAL SCHOTTKY',kind:'triple',x:5,y:95,pins:[['1','GND_LOGIC'],['2','3V3'],['3','VBAT_SENSE']]},
  {ref:'DBAT_TEMP',value:'BAT54S / DUAL SCHOTTKY',kind:'triple',x:5,y:102,pins:[['1','GND_LOGIC'],['2','3V3'],['3','TEMP_SENSE']]},
  {ref:'DBAT_TPS',value:'BAT54S TPS',kind:'triple',x:5,y:109,pins:[['1','GND_LOGIC'],['2','3V3'],['3','TPS_SIG_DIV']]},
  {ref:'DBAT_TREF',value:'BAT54S TPS REF',kind:'triple',x:5,y:116,pins:[['1','GND_LOGIC'],['2','3V3'],['3','TPS_REF_DIV']]},
  {ref:'DBAT_PICK',value:'BAT54S PICKUP',kind:'triple',x:5,y:123,pins:[['1','GND_LOGIC'],['2','5V_LOGIC'],['3','PICKUP_SENSE']]},
  {ref:'DBAT_HVC',value:'BAT54S HV CENTER',kind:'triple',x:5,y:130,pins:[['1','GND_LOGIC'],['2','3V3'],['3','HV_C_ADC']]},
  {ref:'DBAT_HVS',value:'BAT54S HV SIDE',kind:'triple',x:5,y:137,pins:[['1','GND_LOGIC'],['2','3V3'],['3','HV_S_ADC']]},

  {ref:'U5',value:'TC4427A DIP8',kind:'dip',x:115,y:65,pitch:2.54,row:7.62,pins:[
    ['1','NC'],['2','PWM_A_IN'],['3','GND_POWER'],['4','PWM_B_IN'],['5','DRV_B'],['6','VIN_HV'],['7','DRV_A'],['8','NC']
  ]},
  {ref:'QHV1',value:'IRF3205',kind:'triple',x:132,y:64,pins:[['G','GATE_Q1'],['D','LV_A'],['S','ISENSE']]},
  {ref:'QHV2',value:'IRF3205',kind:'triple',x:132,y:81,pins:[['G','GATE_Q2'],['D','LV_B'],['S','ISENSE']]},
  {ref:'RSENSE',value:'0.05R 5W',kind:'axial',x:120,y:94,pins:[['1','ISENSE'],['2','GND_POWER']]},
  {ref:'DTVS1',value:'1.5KE33A',kind:'axial',x:130,y:115,pins:[['A','ISENSE'],['K','LV_A']]},
  {ref:'DTVS2',value:'1.5KE33A',kind:'axial',x:130,y:122,pins:[['A','ISENSE'],['K','LV_B']]},
  {ref:'T1',value:'EE35 UNIVERSAL GRID 2.54mm',kind:'transformer_ee35',x:150,y:58,pins:[
    ['1','LV_A'],['2','VIN_HV'],['3','LV_B'],['4','HV_AC1'],['5','HV_AC2']
  ]},
  {ref:'DREC1',value:'UF4007',kind:'axial',x:178,y:52,pins:[['A','HV_AC1'],['K','BRIDGE_PLUS']]},
  {ref:'DREC2',value:'UF4007',kind:'axial',x:178,y:59,pins:[['A','HV_AC2'],['K','BRIDGE_PLUS']]},
  {ref:'DREC3',value:'UF4007',kind:'axial',x:178,y:66,pins:[['A','GND_POWER'],['K','HV_AC1']]},
  {ref:'DREC4',value:'UF4007',kind:'axial',x:178,y:73,pins:[['A','GND_POWER'],['K','HV_AC2']]},
  {ref:'DCH_C',value:'UF4007',kind:'axial',x:192,y:52,pins:[['A','BRIDGE_PLUS'],['K','HV_CENTER']]},
  {ref:'DCH_S',value:'UF4007',kind:'axial',x:192,y:73,pins:[['A','BRIDGE_PLUS'],['K','HV_SIDE']]},
  {ref:'C_CENTER',value:'1uF 630V MKP 9x4 HOLE',kind:'hv_cap_9x4',x:148,y:38,pins:[['1','HV_CENTER'],['2','COIL_CENTER']]},
  {ref:'C_SIDE',value:'1uF 630V MKP 9x4 HOLE',kind:'hv_cap_9x4',x:196,y:92,pins:[['1','HV_SIDE'],['2','COIL_SIDE']]},
  {ref:'SCR1',value:'BT151-800R',kind:'triple',x:182,y:133,pins:[['K','GND_POWER'],['A','HV_CENTER'],['G','SCR_GATE_C']]},
  {ref:'SCR2',value:'BT151-800R',kind:'triple',x:203,y:133,pins:[['K','GND_POWER'],['A','HV_SIDE'],['G','SCR_GATE_S']]},
  {ref:'QNC',value:'BC547B',kind:'triple',x:145,y:133,pins:[['C','QPC_BASE_R'],['B','GATE_C_BASE'],['E','GND_POWER']]},
  {ref:'QPC',value:'BC557B',kind:'triple',x:158,y:133,pins:[['C','SCR_GATE_C_R'],['B','QPC_BASE'],['E','VIN_HV']]},
  {ref:'QNS',value:'BC547B',kind:'triple',x:145,y:142,pins:[['C','QPS_BASE_R'],['B','GATE_S_BASE'],['E','GND_POWER']]},
  {ref:'QPS',value:'BC557B',kind:'triple',x:158,y:142,pins:[['C','SCR_GATE_S_R'],['B','QPS_BASE'],['E','VIN_HV']]}
];

const passives = [
  ['RVB1','100k','VIN_FILT','VBAT_SENSE'],['RVB2','22k','VBAT_SENSE','GND_LOGIC'],['RVB3','1k','VBAT_SENSE','VBAT_ADC'],
  ['RT1','4.7k','5V_LOGIC','TEMP_SENSOR'],['RT2','15k','TEMP_SENSOR','TEMP_SENSE'],['RT3','27k','TEMP_SENSE','GND_LOGIC'],['RT4','1k','TEMP_SENSE','TEMP_ADC'],
  ['RTPS0','100R','5V_LOGIC','TPS_REF_RAW'],['RTPS1','15k','TPS_SIG_RAW','TPS_SIG_DIV'],['RTPS2','27k','TPS_SIG_DIV','GND_LOGIC'],['RTPS3','1k','TPS_SIG_DIV','TPS_ADC'],
  ['RTPS4','15k','TPS_REF_RAW','TPS_REF_DIV'],['RTPS5','27k','TPS_REF_DIV','GND_LOGIC'],['RTPS6','1k','TPS_REF_DIV','TPS_REF_ADC'],
  ['RPICK1','39k 0.5W','PICKUP_RAW','PICKUP_SENSE'],['RPICK2','10k','PICKUP_SENSE','VMID'],['RPICK3','10M','PICKUP_OC','PICKUP_SENSE'],['RPICK4','4.7k','3V3','PICKUP_OC'],['RPICK5','1k','PICKUP_OC','PICKUP_DIG'],
  ['RVM1','10k','5V_LOGIC','VMID'],['RVM2','10k','VMID','GND_LOGIC'],['RVZ1','15k','5V_LOGIC','PICKUP_ZERO'],['RVZ2','10k','PICKUP_ZERO','GND_LOGIC'],
  ['ROEMC1','12k 0.5W','COIL_CENTER','OEM_C_R1'],['ROEMC2','12k 0.5W','OEM_C_R1','OEM_C_R2'],['ROEMC3','12k 0.5W','OEM_C_R2','OEM_C_R3'],['ROEMC4','12k 0.5W','OEM_C_R3','OEM_C_LED_A'],
  ['ROEMS1','12k 0.5W','COIL_SIDE','OEM_S_R1'],['ROEMS2','12k 0.5W','OEM_S_R1','OEM_S_R2'],['ROEMS3','12k 0.5W','OEM_S_R2','OEM_S_R3'],['ROEMS4','12k 0.5W','OEM_S_R3','OEM_S_LED_A'],
  ['ROEMC5','4.7k','3V3','OEM_CENTER'],['ROEMS5','4.7k','3V3','OEM_SIDE'],
  ['RFAN1','4.7k','FAN_CTL','FAN_BASE'],['RFAN2','10k','FAN_BASE','GND_LOGIC'],
  ['RSTR1','100R','STROBE','STROBE_GATE'],['RSTR2','10k','STROBE_GATE','GND_POWER'],
  ['CBAT','10nF','VBAT_SENSE','GND_LOGIC'],['CTEMP','10nF','TEMP_SENSE','GND_LOGIC'],
  ['CTPSS','4.7nF','TPS_SIG_DIV','GND_LOGIC'],['CTPSR','4.7nF','TPS_REF_DIV','GND_LOGIC'],
  ['CU2','100nF','5V_LOGIC','GND_LOGIC'],
  ['RPWMA','1k','PWM_A','PWM_A_IN'],['RPWMB','1k','PWM_B','PWM_B_IN'],['RGA','10R','DRV_A','GATE_Q1'],['RGB','10R','DRV_B','GATE_Q2'],
  ['RGPD1','10k','GATE_Q1','ISENSE'],['RGPD2','10k','GATE_Q2','ISENSE'],
  ['RPWMPDA','10k','PWM_A_IN','GND_POWER'],['RPWMPDB','10k','PWM_B_IN','GND_POWER'],
  ['CTC1','100nF','VIN_HV','GND_POWER'],['CTC2','10uF 25V','VIN_HV','GND_POWER'],
  ['RI1','120k','3V3','IREF'],['RI2','10k','IREF','GND_LOGIC'],['RIS','100R','ISENSE','ISENSE_FILTERED'],
  ['RVOV1','120k','5V_LOGIC','VOV_REF'],['RVOV2','100k','VOV_REF','GND_LOGIC'],['CVOV','10nF','VOV_REF','GND_LOGIC'],
  ['RFAULT','4.7k','3V3','FAULT_N'],['DCLA','1N4148','PWM_A','FAULT_N'],['DCLB','1N4148','PWM_B','FAULT_N'],
  ['RHVP1','100k','VIN_HV','HV_PRESENT'],['RHVP2','27k','HV_PRESENT','GND_POWER'],
  ['RGC1','4.7k','GATE_C','GATE_C_BASE'],['RGC2','2.2k','QPC_BASE_R','QPC_BASE'],['RGC3','10k','VIN_HV','QPC_BASE'],['RGC4','330R','SCR_GATE_C_R','SCR_GATE_C'],['RGC5','1k','SCR_GATE_C','GND_POWER'],
  ['RGC6','10k','GATE_C_BASE','GND_POWER'],
  ['RGS1','4.7k','GATE_S','GATE_S_BASE'],['RGS2','2.2k','QPS_BASE_R','QPS_BASE'],['RGS3','10k','VIN_HV','QPS_BASE'],['RGS4','330R','SCR_GATE_S_R','SCR_GATE_S'],['RGS5','1k','SCR_GATE_S','GND_POWER'],
  ['RGS6','10k','GATE_S_BASE','GND_POWER']
];

let px=40, py=68;
for (const [ref,val,a,b] of passives) {
  const power = /^(RPWM|RGA|RGB|RGPD|CTC|RI\d|RIS|RVOV|CVOV|RFAULT|DCL|RHVP)/.test(ref);
  const gate = /^(RGC|RGS)/.test(ref);
  if (power) {
    const n=components.filter(c=>c._group==='powerPassive').length;
    components.push({ref,value:val,kind:'axial',x:103+(n%4)*10,y:8+Math.floor(n/4)*7,pins:[['1',a],['2',b]],_group:'powerPassive'});
  } else if (gate) {
    const n=components.filter(c=>c._group==='gatePassive').length;
    components.push({ref,value:val,kind:'axial',x:145+(n%5)*13,y:112+Math.floor(n/5)*7,pins:[['1',a],['2',b]],_group:'gatePassive'});
  } else {
    components.push({ref,value:val,kind:'axial',x:px,y:py,pins:[['1',a],['2',b]],_group:'logicPassive'});
    px += 11; if (px > 88) { px=40; py+=7; }
  }
}

for (const bank of [['C','HV_CENTER','HV_C_FB','HV_C_ADC'],['S','HV_SIDE','HV_S_FB','HV_S_ADC']]) {
  const [suffix,hv,fb,adc]=bank; let previous=hv;
  for(let n=1;n<=4;n++) { const next=n===4?fb:`HV_${suffix}_DIV${n}`; components.push({ref:`RHV${suffix}${n}`,value:'270k 1%',kind:'axial',x:174+n*10,y:suffix==='C'?8:18,pins:[['1',previous],['2',next]]}); previous=next; }
  components.push({ref:`RHV${suffix}5`,value:'8.2k 1%',kind:'axial',x:184,y:suffix==='C'?25:28,pins:[['1',fb],['2','GND_POWER']]});
  components.push({ref:`RHV${suffix}6`,value:'1k',kind:'axial',x:198,y:suffix==='C'?25:28,pins:[['1',fb],['2',adc]]});
  components.push({ref:`CFB${suffix}`,value:'10nF',kind:'axial',x:160,y:suffix==='C'?25:32,pins:[['1',fb],['2','GND_POWER']]});
}

for (const [suffix,hv,coil,y] of [['C','HV_CENTER','COIL_CENTER',35],['S','HV_SIDE','COIL_SIDE',79]]) {
  let previous=hv;
  for(let n=1;n<=4;n++) { const next=n===4?coil:`BLEED_${suffix}${n}`; components.push({ref:`RB${suffix}${n}`,value:'470k 0.5W',kind:'axial',x:170+n*11,y,pins:[['1',previous],['2',next]]}); previous=next; }
}

const bom = components.map(c => ({ref:c.ref,qty:1,value:c.value,footprint:c.kind,notes:''}));

function footprint(c) {
  const x=mm(c.x), y=mm(c.y), shapes=[];
  const pads=[];
  const addPad=(p,dx,dy,w=mm(2.6),h=mm(2.6),hole=mm(0.55))=>{
    const px=x+dx,py=y+dy;
    pads.push(`PAD~ELLIPSE~${px}~${py}~${w}~${h}~11~${p[1]}~${p[0]}~${hole}~~0~${gid()}~0~~Y~0~0~0.3~${px},${py}`);
  };
  if(c.kind==='single') c.pins.forEach((p,i)=>addPad(p,0,mm(i*2.54)));
  else if(c.kind==='header1x2'||c.kind==='header1x4') {
    c.pins.forEach((p,i)=>addPad(p,mm(i*2.54),0,mm(2),mm(2),mm(1)));
    shapes.push(`TRACK~0.5~3~~${x-mm(1.27)} ${y-mm(1.27)} ${x+mm((c.pins.length-1)*2.54+1.27)} ${y-mm(1.27)} ${x+mm((c.pins.length-1)*2.54+1.27)} ${y+mm(1.27)} ${x-mm(1.27)} ${y+mm(1.27)} ${x-mm(1.27)} ${y-mm(1.27)}~${gid()}~0`);
  }
  else if(c.kind==='header2x6') {
    c.pins.forEach((p,i)=>addPad(p,mm((i%6)*2.54),mm(i<6?0:2.54),mm(2),mm(2),mm(1)));
    shapes.push(`TRACK~0.5~3~~${x-mm(1.27)} ${y-mm(1.27)} ${x+mm(13.97)} ${y-mm(1.27)} ${x+mm(13.97)} ${y+mm(3.81)} ${x-mm(1.27)} ${y+mm(3.81)} ${x-mm(1.27)} ${y-mm(1.27)}~${gid()}~0`);
  }
  else if(c.kind==='harness2x6_safe') {
    // Logical 2x6 numbering, but NOT a standard 2.54 mm plug-in header.
    // Pins 6/12 carry CDI pulse voltage and are isolated at the far column.
    c.pins.forEach((p,i)=>{
      const column=i%6,row=i<6?0:1;
      addPad(p,mm(column===5?25.4:column*2.54),mm(row*10.16),mm(3.2),mm(3.2),mm(1.1));
    });
    shapes.push(`TRACK~0.5~3~~${x-mm(1.6)} ${y-mm(1.6)} ${x+mm(27)} ${y-mm(1.6)} ${x+mm(27)} ${y+mm(11.76)} ${x-mm(1.6)} ${y+mm(11.76)} ${x-mm(1.6)} ${y-mm(1.6)}~${gid()}~0`);
  }
  else if(c.kind==='dual'||c.kind==='dip') {
    const half=Math.ceil(c.pins.length/2), row=mm(c.row||7.62), pitch=mm(c.pitch||2.54);
    c.pins.forEach((p,i)=> i<half?addPad(p,0,pitch*i):addPad(p,row,pitch*(c.pins.length-1-i)));
  } else if(c.kind==='quad') { const pos=[[0,0],[0,mm(7.62)],[mm(7.62),mm(7.62)],[mm(7.62),0]]; c.pins.forEach((p,i)=>addPad(p,...pos[i])); }
  else if(c.kind==='triple') c.pins.forEach((p,i)=>addPad(p,mm(i*2.54),0));
  else if(c.kind==='transformer_ee35') {
    // Universal EE35 grid. The front row has 11 usable positions with
    // LV_A/CT/LV_B at positions 1/6/11. The rear row has one empty pad at
    // each side and the two HV outputs exactly 10 pitches (25.40 mm) apart.
    const frontNets={0:c.pins[0],5:c.pins[1],10:c.pins[2]};
    const rearNets={1:c.pins[3],11:c.pins[4]};
    for(let i=0;i<11;i++) addPad(frontNets[i]||[`F${i+1}`,''],mm((i+1)*2.54),0,mm(2.8),mm(2.8),mm(1));
    for(let i=0;i<13;i++) addPad(rearNets[i]||[`B${i+1}`,''],mm(i*2.54),mm(25.4),mm(2.8),mm(2.8),mm(1));
    shapes.push(`TRACK~0.8~3~~${x-mm(1.27)} ${y-mm(2.54)} ${x+mm(31.75)} ${y-mm(2.54)} ${x+mm(31.75)} ${y+mm(27.94)} ${x-mm(1.27)} ${y+mm(27.94)} ${x-mm(1.27)} ${y-mm(2.54)}~${gid()}~0`);
  }
  else if(c.kind==='hv_cap_9x4') {
    // Nine holes inclusive gives eight 2.54 mm intervals = 20.32 mm lead pitch.
    addPad(c.pins[0],0,mm(5.08),mm(4),mm(4),mm(.8));
    addPad(c.pins[1],mm(20.32),mm(5.08),mm(4),mm(4),mm(.8));
    shapes.push(`TRACK~0.8~3~~${x-mm(1.27)} ${y} ${x+mm(21.59)} ${y} ${x+mm(21.59)} ${y+mm(10.16)} ${x-mm(1.27)} ${y+mm(10.16)} ${x-mm(1.27)} ${y}~${gid()}~0`);
  }
  else { addPad(c.pins[0],0,0); addPad(c.pins[1],mm(10),0); }
  shapes.push(`TEXT~P~${x}~${y-mm(2)}~0.7~0~0~3~~4.5~${c.ref} ${c.value}~~${gid()}~~0`);
  shapes.push(...pads);
  return `LIB~${x}~${y}~package\`${c.kind}\`value\`${c.value}\`Contributor\`IGNITRA\`~~~${gid()}~1~~~0~#@$${shapes.join('#@$')}`;
}

const pcbLayers = variant => [
  `1~TopLayer~#FF0000~true~${variant==='2L'}~true~`,
  '2~BottomLayer~#0000FF~true~true~true~',
  '3~TopSilkLayer~#FFCC00~true~false~true~',
  '4~BottomSilkLayer~#66CC33~true~false~true~',
  '5~TopPasteMaskLayer~#808080~true~false~true~',
  '6~BottomPasteMaskLayer~#800000~true~false~true~',
  '7~TopSolderMaskLayer~#800080~true~false~true~0.3',
  '8~BottomSolderMaskLayer~#AA00FF~true~false~true~0.3',
  '9~Ratlines~#6464FF~true~false~true~',
  '10~BoardOutLine~#FF00FF~true~false~true~',
  '11~Multi-Layer~#C0C0C0~true~false~true~',
  '12~Document~#FFFFFF~true~false~true~',
  '13~TopAssembly~#33CC99~false~false~false~',
  '14~BottomAssembly~#5555FF~false~false~false~',
  '15~Mechanical~#F022F0~false~false~false~',
  '19~3DModel~#66CCFF~false~false~false~',
  '21~Inner1~#999966~false~false~false~~','22~Inner2~#008000~false~false~false~~',
  '23~Inner3~#00FF00~false~false~false~~','24~Inner4~#BC8E00~false~false~false~~',
  '25~Inner5~#70DBFA~false~false~false~~','26~Inner6~#00CC66~false~false~false~~',
  '27~Inner7~#9966FF~false~false~false~~','28~Inner8~#800080~false~false~false~~',
  '29~Inner9~#008080~false~false~false~~','30~Inner10~#15935F~false~false~false~~',
  '31~Inner11~#000080~false~false~false~~','32~Inner12~#00B400~false~false~false~~',
  '33~Inner13~#2E4756~false~false~false~~','34~Inner14~#99842F~false~false~false~~',
  '35~Inner15~#FFFFAA~false~false~false~~','36~Inner16~#99842F~false~false~false~~',
  '37~Inner17~#2E4756~false~false~false~~','38~Inner18~#3535FF~false~false~false~~',
  '39~Inner19~#8000BC~false~false~false~~','40~Inner20~#43AE5F~false~false~false~~',
  '41~Inner21~#C3ECCE~false~false~false~~','42~Inner22~#728978~false~false~false~~',
  '43~Inner23~#39503F~false~false~false~~','44~Inner24~#0C715D~false~false~false~~',
  '45~Inner25~#5A8A80~false~false~false~~','46~Inner26~#2B937E~false~false~false~~',
  '47~Inner27~#23999D~false~false~false~~','48~Inner28~#45B4E3~false~false~false~~',
  '49~Inner29~#215DA1~false~false~false~~','50~Inner30~#4564D7~false~false~false~~',
  '51~Inner31~#6969E9~false~false~false~~','52~Inner32~#9069E9~false~false~false~~',
  '99~ComponentShapeLayer~#00CCCC~false~false~false~',
  '100~LeadShapeLayer~#CC9999~false~false~false~',
  '101~ComponentPolarityLayer~#66FFCC~false~false~false~',
  'Hole~Hole~#222222~false~false~true~',
  'DRCError~DRCError~#FAD609~false~false~true~'
];

const pcbObjects = [
  'All~true~false','Component~true~true','Prefix~true~true','Name~true~false',
  'Track~true~true','Pad~true~true','Via~true~true','Hole~true~true',
  'Copper_Area~true~true','Circle~true~true','Arc~true~true',
  'Solid_Region~true~true','Text~true~true','Image~true~true',
  'Rect~true~true','Dimension~true~true','Protractor~true~true'
];

function board(variant) {
  id=1000;
  const ox=0, oy=0, w=mm(230), h=mm(165);
  const shape=[];
  shape.push(`TRACK~1~10~~${ox} ${oy} ${ox+w} ${oy} ${ox+w} ${oy+h} ${ox} ${oy+h} ${ox} ${oy}~${gid()}~0`);
  shape.push(`TEXT~L~${ox+10}~${oy+15}~1~0~0~3~~10~IGNITRA CDI R9 ESP32 ${variant} - UNROUTED PCB SOURCE~~${gid()}~~0`);
  shape.push(`TEXT~L~${ox+10}~${oy+28}~0.8~0~0~3~~8~HV 345V: clearance copper >=6mm; slots >=2mm; ANTENNA KEEP-OUT 15mm~~${gid()}~~0`);
  // Functional-zone boundaries and isolation-slot guides.
  shape.push(`TRACK~1~12~~${ox+mm(100)} ${oy} ${ox+mm(100)} ${oy+h}~${gid()}~0`);
  shape.push(`TRACK~1~12~~${ox+mm(145)} ${oy} ${ox+mm(145)} ${oy+h}~${gid()}~0`);
  // Segmented isolation slots: gaps are reserved only for explicitly routed
  // feedback, transformer and gate/power crossings.
  for (const [y1,y2] of [[5,20],[42,54],[90,108]]) {
    shape.push(`SOLIDREGION~10~~M ${ox+mm(143)} ${oy+mm(y1)} L ${ox+mm(145)} ${oy+mm(y1)} L ${ox+mm(145)} ${oy+mm(y2)} L ${ox+mm(143)} ${oy+mm(y2)} Z~npth~${gid()}~0`);
  }
  // Isolation slot inside J1: pins 1-5/7-11 stay low-voltage, while
  // pulse-output pads 6 and 12 sit on the HV side of this slot.
  shape.push(`SOLIDREGION~10~~M ${ox+mm(135)} ${oy+mm(145)} L ${ox+mm(140)} ${oy+mm(145)} L ${ox+mm(140)} ${oy+mm(164)} L ${ox+mm(135)} ${oy+mm(164)} Z~npth~${gid()}~0`);
  for(const c of components) shape.push(footprint(c));
  return {
    head:{docType:'3',editorVersion:'6.5.51',newgId:true,c_para:{},hasIdFlag:true,x:'0',y:'0',importFlag:0,transformList:''},
    canvas:`CA~2400~1600~#000000~yes~#FFFFFF~3.937008~1200~800~line~1.968504~mm~7.3504~45~visible~0.393701~0~0~0~none`,
    shape,
    layers:pcbLayers(variant),objects:pcbObjects,
    BBox:{x:ox,y:oy,width:w,height:h}, preference:{hideFootprints:'',hideNets:''},
    DRCRULE:{Default:{trackWidth:variant==='2L'?1.2:2.4,clearance:1.2,viaHoleDiameter:2.4,viaHoleD:0.8},isRealtime:true,isDrcOnRoutingOrPlaceVia:true,checkObjectToCopperarea:true,showDRCRangeLine:true},
    routerRule:{unit:'mm',trackWidth:variant==='2L'?1.2:2.4,trackClearance:1.2,viaHoleD:0.8,viaDiameter:2.4,routerLayers:variant==='2L'?[1,2]:[2],smdClearance:1.2,specialNets:[],nets:[...new Set(components.flatMap(c=>c.pins.map(p=>p[1])).filter(Boolean))],padsCount:components.reduce((n,c)=>n+c.pins.length,0),skipNets:[],realtime:true},
    netColors:{}
  };
}

function schematic() {
  const shape=[];
  const blocks=[
    ['J1 HARNESS',120,180],['POWER INPUT',260,180],['ESP32 DEVKITC',430,180],
    ['SENSORS + LM339',430,310],['TC4427 + PUSH-PULL',610,180],['ATX TRANSFORMER',760,180],
    ['UF4007 BRIDGE',900,180],['HV CENTER BANK',1040,130],['HV SIDE BANK',1040,260],
    ['SCR DRIVERS',850,360]
  ];
  for(const [name,x,y] of blocks){ shape.push(`R~${x}~${y}~10~10~140~65~#FFFFFF~2~0~none~${gid()}~0`); shape.push(`T~L~${x+8}~${y+28}~0~#000080~Arial~9pt~~~~comment~${name}~1~start~${gid()}~0`); }
  const wires=[[260,212,260,212,260,212],[260,212,400,212],[570,212,610,212],[750,212,760,212],[900,212,900,212],[1040,212,1040,162],[970,212,1040,292],[500,245,500,310],[850,392,990,392],[990,392,990,162],[990,392,990,292]];
  for(const p of wires) shape.push(`W~${p.join(' ')}~#008800~2~0~none~${gid()}~0`);
  const notes=[
    ['IGN_12V / GND_STAR',270,205],['GPIO18/19',620,205],['HV_AC',900,205],
    ['GPIO25/26',860,385],['ADC1 ONLY: GPIO36/39/34/35/32/33',430,390],
    ['This architecture sheet accompanies netlist.csv; do not fabricate until routing and DRC are confirmed.',120,500]
  ];
  for(const [t,x,y] of notes) shape.push(`T~L~${x}~${y}~0~#0000FF~Arial~8pt~~~~comment~${t}~1~start~${gid()}~0`);
  const dataStr={
    head:{docType:'1',editorVersion:'6.5.51',newgId:true,c_para:{'Prefix Start':'1'},c_spiceCmd:'null',hasIdFlag:true,uuid:'ignitra-cdi-r9-block-diagram',x:0,y:0,portOfADImportHack:'',importFlag:0,transformList:''},
    canvas:'CA~1200~700~#FFFFFF~yes~#CCCCCC~5~1200~700~line~5~pixel~5~0~0',
    shape,BBox:{x:100,y:100,width:1100,height:500},colors:{}
  };
  return {editorVersion:'6.5.51',docType:'5',title:'IGNITRA CDI R9 Block Diagram',description:'Block diagram only; PCB netlist is authoritative.',colors:{},schematics:[{docType:'1',title:'Architecture',description:'Non-fabrication block diagram',dataStr}]};
}

const architecture=schematic();
fs.writeFileSync(path.join(out,'IGNITRA_CDI_ESP32_BLOCK_DIAGRAM.json'),JSON.stringify(architecture,null,2));
fs.writeFileSync(path.join(out,'IGNITRA_CDI_ESP32_architecture.json'),JSON.stringify(architecture,null,2));
const board2L=board('2L');
const board1L=board('1L');
fs.writeFileSync(path.join(out,'IGNITRA_CDI_ESP32_2L_EASYEDA.json'),JSON.stringify(board2L,null,2));
fs.writeFileSync(path.join(out,'IGNITRA_CDI_ESP32_1L_EASYEDA.json'),JSON.stringify(board1L,null,2));
// Backward-compatible aliases retained for links created before Rev B.
fs.writeFileSync(path.join(out,'IGNITRA_CDI_ESP32_2L_placement.json'),JSON.stringify(board2L,null,2));
fs.writeFileSync(path.join(out,'IGNITRA_CDI_ESP32_1L_placement.json'),JSON.stringify(board1L,null,2));

const csv = rows => rows.map(r=>r.map(v=>`"${String(v).replaceAll('"','""')}"`).join(',')).join('\n')+'\n';
fs.writeFileSync(path.join(out,'BOM.csv'),csv([
  ['Reference','Qty','Value / Part','Footprint class','Status'],
  ...bom.map(x=>[x.ref,x.qty,x.value,x.footprint,['J1','T1','C_CENTER','C_SIDE'].includes(x.ref)?'LOCKED TO USER 2.54mm GRID':'VERIFY PHYSICAL FOOTPRINT'])
]));
fs.writeFileSync(path.join(out,'NETLIST.csv'),csv([
  ['Reference','Pin','Net'],
  ...components.flatMap(c=>c.pins.map(p=>[c.ref,p[0],p[1]]))
]));
fs.writeFileSync(path.join(out,'PLACEMENT.csv'),csv([
  ['Reference','X_mm','Y_mm','Zone','Rotation'],
  ...components.map(c=>[c.ref,c.x,c.y,c.x<100?'LOGIC':c.x<145?'POWER':'HV',0])
]));

const zoneColor = x => x < 100 ? '#16273a' : x < 145 ? '#3a2d16' : '#3a161c';
const svgParts = components.map(c => {
  const width = c.kind === 'single' ? 10 : c.kind === 'transformer_ee35' ? 33 :
    c.kind === 'hv_cap_9x4' ? 23 : c.kind === 'harness2x6_safe' ? 28 : c.kind === 'header2x6' ? 14 :
    c.kind === 'dip' || c.kind === 'dual' ? 12 : 11;
  const height = c.kind === 'single' ? 50 : c.kind === 'transformer_ee35' ? 28 :
    c.kind === 'hv_cap_9x4' ? 11 : c.kind === 'harness2x6_safe' ? 12 : c.kind === 'header2x6' ? 5 :
    c.kind === 'dip' || c.kind === 'dual' ? Math.max(8, c.pins.length * 1.4) : 6;
  return `<g><rect x="${c.x}" y="${c.y}" width="${width}" height="${height}" rx="0.8" fill="${zoneColor(c.x)}" stroke="#d8e6ef" stroke-width="0.35"/><text x="${c.x+0.8}" y="${c.y+2.7}" font-size="2.3" fill="#ffffff">${c.ref}</text></g>`;
}).join('');
const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 230 165" width="2300" height="1650"><rect width="230" height="165" fill="#0a0b0d"/><rect x="0" y="0" width="100" height="165" fill="#0c1c2b"/><rect x="100" y="0" width="45" height="165" fill="#2a210e"/><rect x="145" y="0" width="85" height="165" fill="#2b0e13"/><path d="M100 0V165M145 0V165" stroke="#ffffff" stroke-width="0.4" stroke-dasharray="2 1"/><text x="2" y="163" font-size="3" fill="#6ee7ff">LOGIC</text><text x="102" y="163" font-size="3" fill="#ffc857">POWER 12V</text><text x="147" y="163" font-size="3" fill="#ff6b6b">HV 345V</text>${svgParts}</svg>`;
fs.writeFileSync(path.join(out,'PLACEMENT_PREVIEW.svg'),svg);

console.log(`Generated ${components.length} footprints into ${out}`);
