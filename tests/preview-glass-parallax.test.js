const assert=require('assert'),fs=require('fs'),vm=require('vm');
const html=fs.readFileSync('preview/index.html','utf8'),script=fs.readFileSync('preview/glass-parallax.js','utf8');
assert.match(html,/\.glass-parallax \{[^}]*pointer-events:none/);
assert.match(html,/\.screen\.glass-over-home \.glass-parallax \{ z-index:7; \}/);
assert.match(html,/@media \(prefers-reduced-motion:reduce\) \{ \.glass-parallax \{ display:none; \} \}/);
assert(html.includes('Preview tidak membaca sensor perangkat.'));
assert(!/DeviceOrientationEvent|requestPermission|devicemotion/.test(script));
class Element {
 constructor(){this.listeners={};this.value='0';this.checked=true;this.hidden=false;this.disabled=false;this.properties={};this.style={setProperty:(k,v)=>this.properties[k]=v};}
 addEventListener(type,fn){this.listeners[type]=fn;} fire(type,e={}){this.listeners[type](e);}
 getBoundingClientRect(){return {left:10,top:20,width:200,height:400};}
}
const screen=new Element(),layer=new Element(),toggle=new Element(),x=new Element(),y=new Element();
const reduce={matches:false,addEventListener(type,fn){assert.equal(type,'change');this.change=fn;}};
const ids={glassParallax:layer,glassParallaxToggle:toggle,glassTiltX:x,glassTiltY:y};
vm.runInNewContext(script,{document:{querySelector:()=>screen,getElementById:id=>ids[id]},window:{matchMedia:()=>reduce,addEventListener(){}}});
assert.equal(layer.hidden,false); assert.equal(layer.properties['--glass-x'],'0px');
screen.fire('pointermove',{pointerType:'mouse',clientX:210,clientY:20});
assert.equal(x.value,100); assert.equal(y.value,-100); assert.equal(layer.properties['--glass-x'],'7.000000000000001px'); assert.equal(layer.properties['--glass-y'],'-14.000000000000002px');
screen.fire('pointermove',{pointerType:'touch',clientX:10,clientY:420}); assert.equal(x.value,100);
screen.fire('pointerleave',{pointerType:'mouse'}); assert.equal(layer.properties['--glass-x'],'0px');
x.value='50'; y.value='50'; x.fire('input'); assert(Math.abs(parseFloat(layer.properties['--glass-x'])-3.5)<1e-9);
toggle.checked=false; toggle.fire('change'); assert(layer.hidden && x.disabled && y.disabled);
toggle.checked=true; toggle.fire('change'); assert(!layer.hidden && !x.disabled && !y.disabled);
reduce.matches=true; reduce.change(); assert(layer.hidden && x.disabled); reduce.matches=false; reduce.change(); assert(!layer.hidden);
console.log('Glass parallax preview: pointer, sliders, toggle, reduced motion and no sensor access OK');
