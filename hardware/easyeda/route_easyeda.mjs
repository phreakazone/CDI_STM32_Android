import fs from 'node:fs';
import path from 'node:path';

const root = path.dirname(new URL(import.meta.url).pathname);
const dir = path.join(root, 'generated');
const UNIT = 3.937007874; // EasyEDA Standard: 10 mil units per millimetre.
const GRID_MM = 0.5;
const BOARD_W = 230;
const BOARD_H = 165;
const mm = value => +(value * UNIT).toFixed(3);

function readPads(board) {
  const pads=[];
  for(const lib of board.shape.filter(x=>x.startsWith('LIB~'))) {
    const meta=Object.fromEntries((lib.split('#@$')[0].split('~')[3]||'').split('`').reduce((a,v,i,x)=>i%2?[...a,[]]:[...a,[v,x[i+1]]],[]));
    const textPart=lib.split('#@$').find(x=>x.startsWith('TEXT~P~'));
    const ref=(textPart?.split('~')[10]||meta.package||'PAD');
    for(const item of lib.split('#@$')) {
      if(!item.startsWith('PAD~')) continue;
      const p=item.split('~');
      const net=p[7]||'';
      if(!net || net==='NC') continue;
      pads.push({ref,pin:p[8],net,x:+p[2],y:+p[3]});
    }
  }
  return pads;
}

class Heap {
  constructor(){this.a=[];}
  push(v){const a=this.a;a.push(v);let i=a.length-1;while(i){const p=(i-1)>>1;if(a[p][0]<=v[0])break;a[i]=a[p];i=p;}a[i]=v;}
  pop(){const a=this.a,top=a[0],last=a.pop();if(a.length){let i=0;while(true){let l=i*2+1,r=l+1;if(l>=a.length)break;let c=r<a.length&&a[r][0]<a[l][0]?r:l;if(a[c][0]>=last[0])break;a[i]=a[c];i=c;}a[i]=last;}return top;}
  get length(){return this.a.length;}
}

const cell=(p)=>({x:Math.max(1,Math.min(Math.round(BOARD_W/GRID_MM)-1,Math.round((p.x/UNIT)/GRID_MM))),y:Math.max(1,Math.min(Math.round(BOARD_H/GRID_MM)-1,Math.round((p.y/UNIT)/GRID_MM)))});
const key=(l,x,y)=>`${l},${x},${y}`;
const parseKey=k=>k.split(',').map(Number);
const netClass=n=>/^(HV_|BRIDGE_PLUS|COIL_|BLEED_)/.test(n)?'HV':/^(GND_POWER|VIN_HV|LV_A|LV_B|ISENSE|VIN_|IGN_12V|FAN_RELAY)/.test(n)?'POWER':/^(GND_|3V3|5V_)/.test(n)?'SUPPLY':'SIGNAL';
const widthFor=n=>({HV:1.2,POWER:1.0,SUPPLY:0.5,SIGNAL:0.25})[netClass(n)];
// Routing reservation is intentionally separate from the 6 mm HV-to-logic
// zoning rule.  HV conductors already live in the isolated right-hand zone;
// using a 6 mm A* radius around every HV segment made routing impossible.
const reserveRadius=n=>({HV:2,POWER:2,SUPPLY:0,SIGNAL:0})[netClass(n)];

function compress(points){
  if(points.length<3)return points;
  const out=[points[0]];
  for(let i=1;i<points.length-1;i++){
    const a=out[out.length-1],b=points[i],c=points[i+1];
    if(a.l===b.l&&b.l===c.l&&(a.x===b.x&&b.x===c.x||a.y===b.y&&b.y===c.y))continue;
    out.push(b);
  }
  out.push(points.at(-1));return out;
}

function routeBoard(source, layers, singleLayer=false){
  const pads=readPads(source);
  const nets=new Map();
  for(const p of pads){if(!nets.has(p.net))nets.set(p.net,[]);nets.get(p.net).push(p);}
  const padCells=new Map();
  // Reserve the physical pad radius plus default clearance. Two grid cells
  // keep unrelated 0.25 mm tracks clear of the 2.00 mm through-hole pads.
  for(const p of pads){const c=cell(p);for(let dx=-2;dx<=2;dx++)for(let dy=-2;dy<=2;dy++){
    if(dx*dx+dy*dy>4)continue;const k=`${c.x+dx},${c.y+dy}`;
    if(!padCells.has(k))padCells.set(k,new Set());padCells.get(k).add(p.net);
  }}
  const occupied=Array.from({length:layers},()=>new Map());
  const shapes=[];const failed=[];let vias=0,tracks=0,jumpers=0,seq=900000;
  const nextId=()=>`ggeR${seq++}`;
  const inside=(x,y)=>x>0&&y>0&&x<Math.round(BOARD_W/GRID_MM)&&y<Math.round(BOARD_H/GRID_MM);
  const keepout=(x,y)=>{
    const xm=x*GRID_MM,ym=y*GRID_MM;
    const antenna=xm>18&&xm<32&&ym<16;
    const zoneSlot=xm>=142&&xm<=146&&[[5,20],[42,54],[90,108]].some(([a,b])=>ym>=a&&ym<=b);
    const j1Slot=xm>=134&&xm<=141&&ym>=144&&ym<=164;
    return antenna||zoneSlot||j1Slot;
  };
  const blocked=(net,l,x,y,start,goals)=>{
    if(!inside(x,y)||keepout(x,y))return true;
    if((x===start.x&&y===start.y)||goals.has(key(l,x,y)))return false;
    const ps=padCells.get(`${x},${y}`);if(ps&&[...ps].some(n=>n!==net))return true;
    const owner=occupied[l].get(`${x},${y}`);return owner&&owner!==net;
  };
  const find=(net,start,goals)=>{
    const heap=new Heap(),dist=new Map(),prev=new Map();
    const goalPoints=[...goals].map(parseKey);
    const minX=Math.min(...goalPoints.map(p=>p[1])),maxX=Math.max(...goalPoints.map(p=>p[1]));
    const minY=Math.min(...goalPoints.map(p=>p[2])),maxY=Math.max(...goalPoints.map(p=>p[2]));
    // Prefer a primary layer instead of scattering every connection across
    // both layers from its first millimetre.  This greatly reduces via tangles
    // around the ESP32/DIP headers while still allowing crossings.
    for(let l=0;l<layers;l++){const k=key(l,start.x,start.y);const bias=0;dist.set(k,bias);heap.push([bias,bias,k]);}
    let end=null;
    while(heap.length){const [,queuedG,k]=heap.pop();const [l,x,y]=parseKey(k);const g=dist.get(k);if(queuedG!==g)continue;
      if(goals.has(k)){end=k;break;}
      // Orthogonal layer discipline: internal layer 0 is horizontal-preferred,
      // internal layer 1 vertical-preferred. This removes the via-per-obstacle
      // zig-zag produced by the earlier symmetric search.
      const moves=l===0
        ? [[l,x+1,y,1],[l,x-1,y,1],[l,x,y+1,3],[l,x,y-1,3]]
        : [[l,x+1,y,3],[l,x-1,y,3],[l,x,y+1,1],[l,x,y-1,1]];
      if(layers>1)moves.push([1-l,x,y,60]);
      for(const [nl,nx,ny,cost] of moves){if(blocked(net,nl,nx,ny,start,goals))continue;if(nl!==l&&blocked(net,l,nx,ny,start,goals))continue;
        const nk=key(nl,nx,ny),ng=g+cost;if(ng>=(dist.get(nk)??Infinity))continue;dist.set(nk,ng);prev.set(nk,k);const hx=nx<minX?minX-nx:nx>maxX?nx-maxX:0,hy=ny<minY?minY-ny:ny>maxY?ny-maxY:0;heap.push([ng+hx+hy,ng,nk]);}
    }
    if(!end)return null;const out=[];for(let k=end;k;k=prev.get(k)){const [l,x,y]=parseKey(k);out.push({l,x,y});}return out.reverse();
  };
  const reserve=(net,points)=>{const r=reserveRadius(net);for(const p of points)for(let dx=-r;dx<=r;dx++)for(let dy=-r;dy<=r;dy++)if(dx*dx+dy*dy<=r*r){const k=`${p.x+dx},${p.y+dy}`;if(!occupied[p.l].has(k))occupied[p.l].set(k,net);}};
  const reserveVias=(net,points)=>{for(let i=1;i<points.length;i++)if(points[i].l!==points[i-1].l){const p=points[i],r=netClass(net)==='SIGNAL'?1:2;for(let l=0;l<layers;l++)for(let dx=-r;dx<=r;dx++)for(let dy=-r;dy<=r;dy++)if(dx*dx+dy*dy<=r*r){const k=`${p.x+dx},${p.y+dy}`;if(!occupied[l].has(k))occupied[l].set(k,net);}}};
  const emit=(net,path,from,to)=>{
    const pts=compress(path),segments=[];let segment=[pts[0]];
    for(let i=1;i<pts.length;i++){
      if(pts[i].l!==pts[i-1].l){
        segments.push(segment);const p=pts[i];const heavy=/^(HV|POWER)$/.test(netClass(net));const vd=heavy?1.8:netClass(net)==='SUPPLY'?1.4:1.0;const drill=heavy?.9:netClass(net)==='SUPPLY'?.7:.5;shapes.push(`VIA~${mm(p.x*GRID_MM)}~${mm(p.y*GRID_MM)}~${mm(vd)}~${net}~${mm(drill)}~${nextId()}`);vias++;segment=[p];
      }else segment.push(pts[i]);
    }
    segments.push(segment);
    segments.forEach((seg,index)=>{const coords=seg.map(p=>`${mm(p.x*GRID_MM)} ${mm(p.y*GRID_MM)}`);if(index===0)coords.unshift(`${from.x} ${from.y}`);if(to&&index===segments.length-1)coords.push(`${to.x} ${to.y}`);if(coords.length>1){const layer=singleLayer?(seg[0].l===0?2:1):seg[0].l+1;shapes.push(`TRACK~${mm(widthFor(net))}~${layer}~${net}~${coords.join(' ')}~${nextId()}`);tracks++;if(singleLayer&&layer===1)jumpers++;}});
  };
  // Route timing/ADC/gate signals before broad supply buses; supplies are
  // intentionally finished with copper areas and short heavy links.
  const priority={POWER:0,HV:1,SIGNAL:2,SUPPLY:3};
  const span=ps=>{const xs=ps.map(p=>p.x),ys=ps.map(p=>p.y);return Math.max(...xs)-Math.min(...xs)+Math.max(...ys)-Math.min(...ys);};
  const ordered=[...nets].filter(([,ps])=>ps.length>1).sort((a,b)=>
    priority[netClass(a[0])] - priority[netClass(b[0])] || span(a[1])-span(b[1]) || b[1].length-a[1].length
  );
  for(const [net,ps] of ordered){const root=ps[0],goal=cell(root),goals=new Set(Array.from({length:layers},(_,l)=>key(l,goal.x,goal.y)));let first=true;for(const p of ps.slice(1)){const start=cell(p),path=find(net,start,goals);if(path){reserve(net,path);reserveVias(net,path);emit(net,path,p,first?root:null);for(const q of path)goals.add(key(q.l,q.x,q.y));first=false;}else if(singleLayer){
        shapes.push(`TRACK~${mm(.6)}~1~${net}~${p.x} ${p.y} ${p.x} ${root.y} ${root.x} ${root.y}~${nextId()}`);jumpers++;
      }else failed.push(`${net}: ${p.ref}.${p.pin} -> ${root.ref}.${root.pin}`);}}
  const board=structuredClone(source);
  // Idempotent rerun: preserve outline/document tracks and footprint-internal
  // shapes, but discard earlier top-level copper routes and vias.
  board.shape=board.shape.filter(item=>!item.startsWith('VIA~') && !(item.startsWith('TRACK~') && item.split('~')[3]));
  board.shape.push(...shapes);
  board.head.routingStatus=singleLayer?'BOTTOM COPPER + TOP INSULATED WIRE JUMPER PLAN':'ROUTED TWO LAYER';
  return {board,report:{pads:pads.length,nets:nets.size,tracks,vias,jumpers,ground_pours:[],failed}};
}

const two=JSON.parse(fs.readFileSync(path.join(dir,'IGNITRA_CDI_ESP32_2L_placement.json'),'utf8'));
const one=JSON.parse(fs.readFileSync(path.join(dir,'IGNITRA_CDI_ESP32_1L_placement.json'),'utf8'));
const routed2=routeBoard(two,2,false);
// The home-build variant uses BottomLayer as copper and TopLayer solely as
// insulated hand-wired jumpers.  Both layers are retained in the source so
// EasyEDA can verify electrical completeness; only BottomLayer is fabricated.
const routed1=routeBoard(one,2,true);
if(routed2.report.failed.length || routed1.report.failed.length){
  throw new Error(`Routing incomplete: 2L=${routed2.report.failed.length}, 1L=${routed1.report.failed.length}`);
}
fs.writeFileSync(path.join(dir,'IGNITRA_CDI_ESP32_2L_routed_draft.json'),JSON.stringify(routed2.board,null,2));
fs.writeFileSync(path.join(dir,'IGNITRA_CDI_ESP32_1L_routed_draft.json'),JSON.stringify(routed1.board,null,2));
// Canonical import sources are routed outputs. The generator first recreates
// placement/net sources, then this script replaces the public files only after
// every logical connection has a route.
fs.writeFileSync(path.join(dir,'IGNITRA_CDI_ESP32_2L_EASYEDA.json'),JSON.stringify(routed2.board,null,2));
fs.writeFileSync(path.join(dir,'IGNITRA_CDI_ESP32_1L_EASYEDA.json'),JSON.stringify(routed1.board,null,2));
fs.writeFileSync(path.join(dir,'IGNITRA_CDI_ESP32_2L_placement.json'),JSON.stringify(routed2.board,null,2));
fs.writeFileSync(path.join(dir,'IGNITRA_CDI_ESP32_1L_placement.json'),JSON.stringify(routed1.board,null,2));
fs.writeFileSync(path.join(dir,'ROUTING_REPORT.json'),JSON.stringify({grid_mm:GRID_MM,two_layer:routed2.report,single_layer:routed1.report},null,2)+'\n');

function routePreview(board,title,file){
  const u=v=>+(v/UNIT).toFixed(3);
  const esc=v=>String(v).replaceAll('&','&amp;').replaceAll('<','&lt;');
  const tracks=[],vias=[],labels=[];
  for(const item of board.shape){
    if(item.startsWith('TRACK~')){
      const p=item.split('~'),layer=+p[2],net=p[3];
      if(!net || (layer!==1&&layer!==2))continue;
      const q=p[4].trim().split(/\s+/).map(Number),pts=[];
      for(let i=0;i<q.length;i+=2)pts.push(`${u(q[i])},${u(q[i+1])}`);
      tracks.push(`<polyline points="${pts.join(' ')}" fill="none" stroke="${layer===1?'#ff5a5f':'#4da3ff'}" stroke-width="${Math.max(.18,u(+p[1]))}" stroke-linejoin="round" stroke-linecap="round" opacity=".78"><title>${esc(net)}</title></polyline>`);
    }else if(item.startsWith('VIA~')){
      const p=item.split('~');vias.push(`<circle cx="${u(+p[1])}" cy="${u(+p[2])}" r="${u(+p[3])/2}" fill="#d8d8d8" stroke="#111" stroke-width=".12"/>`);
    }else if(item.startsWith('LIB~')){
      const h=item.split('#@$')[0].split('~'),x=u(+h[1]),y=u(+h[2]);
      const textPart=item.split('#@$').find(x=>x.startsWith('TEXT~P~'));
      const ref=textPart?.split('~')[10]||'';
      if(ref)labels.push(`<text x="${x}" y="${Math.max(2,y-2)}" font-size="1.5" fill="#fff">${esc(ref)}</text>`);
    }
  }
  const pads=readPads(board).map(p=>`<circle cx="${u(p.x)}" cy="${u(p.y)}" r="1" fill="#c9c9c9" stroke="#222" stroke-width=".15"><title>${esc(p.net)}</title></circle>`).join('');
  const svg=`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${BOARD_W} ${BOARD_H}" width="2300" height="1650"><rect width="230" height="165" fill="#090b0f"/><rect x="1" y="1" width="98" height="163" fill="#0b2030"/><rect x="100" y="1" width="44" height="163" fill="#2b220e"/><rect x="145" y="1" width="84" height="163" fill="#300f16"/><path d="M100 0V165M145 0V165" stroke="#eee" stroke-width=".25" stroke-dasharray="2 1"/><rect x="143" y="5" width="2" height="15" fill="#000"/><rect x="143" y="42" width="2" height="12" fill="#000"/><rect x="143" y="90" width="2" height="18" fill="#000"/><rect x="135" y="145" width="5" height="19" fill="#000"/>${tracks.join('')}${vias.join('')}${pads}${labels.join('')}<text x="3" y="163" font-size="2.2" fill="#fff">${esc(title)} · TOP red · BOTTOM blue</text></svg>`;
  fs.writeFileSync(path.join(dir,file),svg);
}
routePreview(routed2.board,'IGNITRA 2L ROUTED','ROUTING_PREVIEW_2L.svg');
routePreview(routed1.board,'IGNITRA 1L: TOP = INSULATED JUMPERS','ROUTING_PREVIEW_1L.svg');
console.log(JSON.stringify({two_layer:routed2.report,single_layer:routed1.report},null,2));
