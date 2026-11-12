// Mascota "Billetera Alegre" en three.js. Web Component <gastitos-mascot>, tomado tal cual del handoff
// del rediseño v2 (diseno/v2 en el repo de coordinación). Atributos: mood (happy|neutral|worried),
// covering ("true"|"false"), motion (sobrio|animado), look (cursor|form), pulse (cada cambio salta).
// Carga three desde CDN como el resto de las librerías de la app; se libera en disconnectedCallback.
(function(){
if (customElements.get('gastitos-mascot')) return;
let threeP;
const getThree=()=>threeP||(threeP=import('https://unpkg.com/three@0.160.0/build/three.module.js'));
const C={body:0x14433e,flap:0x2f9384,mint:0x7fc4b8,dark:0x0c2a27,white:0xfbfaf6,arm:0x123b36,cheek:0xd9774a};
function rr(T,w,h,r){const s=new T.Shape(),x=-w/2,y=-h/2;s.moveTo(x+r,y);s.lineTo(x+w-r,y);s.quadraticCurveTo(x+w,y,x+w,y+r);s.lineTo(x+w,y+h-r);s.quadraticCurveTo(x+w,y+h,x+w-r,y+h);s.lineTo(x+r,y+h);s.quadraticCurveTo(x,y+h,x,y+h-r);s.lineTo(x,y+r);s.quadraticCurveTo(x,y,x+r,y);return s;}
function slab(T,w,h,r,d,b){const g=new T.ExtrudeGeometry(rr(T,w,h,r),{depth:d,bevelEnabled:true,bevelSize:b,bevelThickness:b,bevelSegments:6,curveSegments:20});g.center();return g;}
class GastitosMascot extends HTMLElement{
  static get observedAttributes(){return ['pulse'];}
  attributeChangedCallback(n,o,v){if(n==='pulse'&&o!=null&&o!==v)this._pulse=performance.now();}
  a(n,d){return this.getAttribute(n)||d;}
  connectedCallback(){
    Object.assign(this.style,{display:'block',position:'absolute',inset:'0',width:'100%',height:'100%',overflow:'hidden'});
    if(this.parentElement&&getComputedStyle(this.parentElement).position==='static')this.parentElement.style.position='relative';
    this._m={x:0,y:0};
    this._onMove=e=>{const r=this.getBoundingClientRect();const cx=r.left+r.width/2,cy=r.top+r.height/2;
      this._m.x=Math.max(-1,Math.min(1,(e.clientX-cx)/(innerWidth/2)));this._m.y=Math.max(-1,Math.min(1,(e.clientY-cy)/(innerHeight/2)));};
    addEventListener('pointermove',this._onMove);
    this._alive=true;
    getThree().then(T=>{if(this._alive&&!this._r)this._init(T);}).catch(e=>console.warn('three load failed',e));
  }
  disconnectedCallback(){
    this._alive=false;removeEventListener('pointermove',this._onMove);cancelAnimationFrame(this._raf);
    if(this._ro)this._ro.disconnect();
    if(this._r){this._r.dispose();this._r.domElement.remove();this._r=null;}
  }
  _init(T){
    const r=new T.WebGLRenderer({antialias:true,alpha:true});this._r=r;
    r.setPixelRatio(Math.min(devicePixelRatio,2));r.outputColorSpace=T.SRGBColorSpace;
    Object.assign(r.domElement.style,{width:'100%',height:'100%',display:'block'});
    this.appendChild(r.domElement);
    const sc=new T.Scene();const cam=new T.PerspectiveCamera(28,1,.1,100);cam.position.set(0,.3,10);
    sc.add(new T.HemisphereLight(0xffffff,0x20403b,1.4));
    const key=new T.DirectionalLight(0xffffff,2.2);key.position.set(3,5,5);sc.add(key);
    const rim=new T.DirectionalLight(C.mint,1.8);rim.position.set(-4,2,-3);sc.add(rim);
    const mat=(c,ro=.5)=>new T.MeshStandardMaterial({color:c,roughness:ro,metalness:0});
    const root=new T.Group();sc.add(root);
    const B=new T.Group();root.add(B);
    B.add(new T.Mesh(slab(T,2.6,1.9,.4,.8,.14),mat(C.body,.45)));
    const flap=new T.Mesh(slab(T,2.1,.8,.25,.4,.1),mat(C.flap,.5));flap.position.set(-.15,.95,-.2);flap.rotation.z=.12;B.add(flap);
    const F=.54;
    const clasp=new T.Mesh(new T.CapsuleGeometry(.2,.3,8,16),mat(C.mint,.35));clasp.rotation.z=Math.PI/2;clasp.position.set(1.12,-.05,F);clasp.scale.z=.6;B.add(clasp);
    const dot=new T.Mesh(new T.SphereGeometry(.09,16,16),mat(C.dark));dot.position.set(1.22,-.05,F+.1);B.add(dot);
    const eyes=[-.55,.2].map(x=>{const g=new T.Group();g.position.set(x,.22,F);
      const w=new T.Mesh(new T.SphereGeometry(.27,24,24),mat(C.white,.3));w.scale.z=.45;g.add(w);
      const p=new T.Group();p.position.z=.1;g.add(p);
      const pm=new T.Mesh(new T.SphereGeometry(.13,20,20),mat(C.dark,.2));pm.scale.z=.6;p.add(pm);
      const hl=new T.Mesh(new T.SphereGeometry(.04,10,10),new T.MeshBasicMaterial({color:0xffffff}));hl.position.set(.05,.05,.08);p.add(hl);
      B.add(g);return{g,p};});
    const brows=[-.55,.2].map(x=>{const g=new T.Group();g.position.set(x,.6,F+.02);
      const m=new T.Mesh(new T.CapsuleGeometry(.045,.2,6,12),mat(C.dark));m.rotation.z=Math.PI/2;g.add(m);B.add(g);return g;});
    const mouth=new T.Mesh(new T.TorusGeometry(.2,.05,12,32,Math.PI),mat(C.dark));mouth.rotation.z=Math.PI;mouth.position.set(-.17,-.3,F+.02);B.add(mouth);
    const cm=new T.MeshStandardMaterial({color:C.cheek,roughness:.6,transparent:true,opacity:.55});
    [-.95,.6].forEach(x=>{const c=new T.Mesh(new T.SphereGeometry(.13,16,16),cm);c.scale.set(1,.6,.2);c.position.set(x,-.15,F);B.add(c);});
    [-1,1].forEach(s=>{const f=new T.Mesh(new T.CapsuleGeometry(.16,.25,6,12),mat(C.arm));f.rotation.z=Math.PI/2;f.position.set(s*.6-.1,-1.2,.1);B.add(f);});
    const down=new T.Vector3(0,-1,0);
    const arms=[-1,1].map(s=>{const sh=new T.Vector3(s*1.42,-.15,.1);
      const geo=new T.CapsuleGeometry(.11,1,6,12);geo.translate(0,-.5,0);
      const arm=new T.Mesh(geo,mat(C.arm));const pivot=new T.Group();pivot.position.copy(sh);pivot.add(arm);B.add(pivot);
      const hand=new T.Mesh(new T.SphereGeometry(.2,20,20),mat(C.mint,.4));B.add(hand);
      return{s,sh,pivot,arm,hand,cur:new T.Vector3(),init:false};});
    const fit=()=>{const w=this.clientWidth||1,h=this.clientHeight||1;r.setSize(w,h,false);cam.aspect=w/h;
      const tan=Math.tan(T.MathUtils.degToRad(14));cam.position.z=Math.max(4.4/2/tan,4.8/2/tan/cam.aspect);cam.updateProjectionMatrix();};
    this._ro=new ResizeObserver(fit);this._ro.observe(this);fit();
    let last=performance.now(),blinkAt=last+1800;
    const st={mouth:1,brow:0,cover:0,lx:0,ly:0};
    const tick=now=>{
      this._raf=requestAnimationFrame(tick);
      const dt=Math.min(.05,(now-last)/1000);last=now;const t=now/1000;
      const lively=this.a('motion','sobrio')==='animado',mood=this.a('mood','happy'),cover=this.a('covering','false')==='true',look=this.a('look','cursor');
      const ease=1-Math.pow(.002,dt),e2=1-Math.pow(lively?.0004:.01,dt);
      st.cover+=((cover?1:0)-st.cover)*e2;
      st.mouth+=((mood==='happy'?1:mood==='worried'?-.6:.3)-st.mouth)*ease;
      st.brow+=((mood==='worried'?.35:mood==='happy'?-.08:0)-st.brow)*ease;
      let lx=this._m.x,ly=this._m.y;if(look==='form'){lx=.9;ly=.35;}
      st.lx+=(lx-st.lx)*ease;st.ly+=(ly-st.ly)*ease;
      const amp=lively?.14:.04,sp=lively?2.4:1.2;
      let bob=Math.sin(t*sp)*amp,sq=0;
      if(this._pulse){const pt=(now-this._pulse)/1000;if(pt<.5){const k=Math.sin(pt/.5*Math.PI);bob+=(lively?.22:.04)*k;sq=(lively?.07:.015)*Math.sin(pt/.5*Math.PI*2);}}
      B.position.y=bob;B.scale.set(1+sq,1-sq,1+sq);
      B.rotation.z=lively?Math.sin(t*sp*.5)*.05:0;
      root.rotation.y+=(st.lx*(lively?.55:.3)-root.rotation.y)*ease;
      root.rotation.x+=(st.ly*(lively?.25:.12)-root.rotation.x)*ease;
      eyes.forEach(e=>{e.p.position.x=st.lx*.1;e.p.position.y=-st.ly*.09;});
      let bs=1;if(now>blinkAt){const p=(now-blinkAt)/120;if(p<2)bs=Math.abs(1-p);else blinkAt=now+2200+Math.random()*2800;}
      eyes.forEach(e=>e.g.scale.y=Math.max(.08,bs));
      brows[0].rotation.z=st.brow;brows[1].rotation.z=-st.brow;
      brows.forEach(b=>b.position.y=.6+Math.max(0,st.brow)*.12);
      mouth.scale.y=st.mouth;
      arms.forEach((A,i)=>{
        const ep=eyes[i].g.position;
        const rest=new T.Vector3(A.sh.x+A.s*.3,A.sh.y-.85+(lively?Math.sin(t*sp+i)*.05:0),A.sh.z+.2);
        const cv=new T.Vector3(ep.x,ep.y,ep.z+.32);
        if(lively&&i===1)cv.y-=Math.max(0,Math.sin(t*1.6))*.28;
        const tgt=rest.lerp(cv,st.cover);
        if(!A.init){A.cur.copy(tgt);A.init=true;}
        A.cur.lerp(tgt,e2);
        const d=A.cur.clone().sub(A.sh);const len=d.length();
        A.pivot.quaternion.setFromUnitVectors(down,d.normalize());A.arm.scale.set(1,len,1);A.hand.position.copy(A.cur);
      });
      r.render(sc,cam);
    };
    this._raf=requestAnimationFrame(tick);
  }
}
customElements.define('gastitos-mascot',GastitosMascot);
})();
