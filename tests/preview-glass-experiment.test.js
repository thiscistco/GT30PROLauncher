// Home wallpaper-relative offsets must continue to work without the former glass toggle.
const assert=require('assert'),fs=require('fs'),vm=require('vm');
const html=fs.readFileSync('preview/index.html','utf8');
assert.match(html,/<div class="screen">/);
assert(!html.includes('glassPreviewToggle') && !html.includes('home-screen-glass') && !html.includes('glass-parallax'));
assert.match(html,/\.super-page \{[^}]*z-index: 9/);
assert.match(html,/\.path-draw \{[^}]*z-index: 12/);
assert.match(html,/<script src="home-position\.js\?v=1"><\/script>/);
const props={};const screen={clientWidth:269,clientHeight:597,style:{setProperty(k,v){props[k]=v;}}}; let resize;
vm.runInNewContext(fs.readFileSync('preview/home-position.js','utf8'),{document:{querySelector:q=>q==='.screen'?screen:null},window:{addEventListener(name,fn){assert.equal(name,'resize');resize=fn;}}});
for (const [property, pixels] of [['--clock-shift',2182],['--clock-right',750],['--battery-shift',1470],['--battery-right',495]])
  assert(Math.abs(parseFloat(props[property])-pixels*Math.max(269/1224,597/2720))<1e-9);
screen.clientWidth=612;screen.clientHeight=1000;resize();
assert.equal(parseFloat(props['--clock-shift']),1091); assert.equal(parseFloat(props['--clock-right']),375);
assert.equal(parseFloat(props['--battery-shift']),735); assert.equal(parseFloat(props['--battery-right']),247.5);
screen.clientWidth=500;screen.clientHeight=1360;resize();
assert.equal(parseFloat(props['--clock-shift']),1091); assert.equal(parseFloat(props['--clock-right']),375);
assert.equal(parseFloat(props['--battery-shift']),735); assert.equal(parseFloat(props['--battery-right']),247.5);
assert.match(html,/\.battery-value \{[^}]*translate\(var\(--battery-right, 0px\), calc\(0px - var\(--battery-shift, 0px\)\)\) scale\(1\.215\)/);
assert.match(html,/transform: translate\(calc\(-50% \+ var\(--clock-right, 0px\)\), calc\(0px - var\(--clock-shift, 0px\)\)\) scale\(1\.2\)/);
assert.match(html,/\.home-weather \{[^}]*color: #ffcd6a/);
assert.match(html,/\.battery-led\.charging \{[^}]*color: #ffcd6a/);
console.log('Home positioning: glass removed, proportional offsets and app layers preserved OK');
