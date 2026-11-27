// Preview-only comparison of the existing glass artwork and home layering.
const assert=require('assert'),fs=require('fs'),vm=require('vm');
const html=fs.readFileSync('preview/index.html','utf8');
assert.match(html,/<div class="screen glass-over-home">/);
assert.match(html,/id="glassPreviewToggle"[^>]*checked/);
assert.match(html,/Percobaan preview saja/);
assert.match(html,/\.home-screen-glass \{[^}]*z-index:1;[^}]*pointer-events:none/);
assert.match(html,/\.screen\.glass-over-home \.home-screen-glass \{ z-index:7; \}/);
assert.match(html,/\.screen\.glass-over-home \.punch-hole \{ z-index:8; \}/);
assert.match(html,/\.super-page \{[^}]*z-index: 9/);
assert.match(html,/\.path-draw \{[^}]*z-index: 12/);
assert.match(html,/<script src="glass-preview\.js\?v=3"><\/script>/);
const classes=new Set(['glass-over-home']);
const props={};const screen={clientWidth:269,clientHeight:597,style:{setProperty(k,v){props[k]=v;}},classList:{toggle(name,on){if(on) classes.add(name);else classes.delete(name);}}};
const toggle={checked:true,addEventListener(name,fn){assert.equal(name,'change');this.change=fn;}};
let resize;
vm.runInNewContext(fs.readFileSync('preview/glass-preview.js','utf8'),{document:{getElementById:id=>id==='glassPreviewToggle'?toggle:null,querySelector:q=>q==='.screen'?screen:null},window:{addEventListener(name,fn){assert.equal(name,'resize');resize=fn;}}});
assert(Math.abs(parseFloat(props['--home-shift']) - 1120*Math.max(269/1224,597/2720))<1e-9);
screen.clientWidth=612;screen.clientHeight=1000;resize();
assert.equal(parseFloat(props['--home-shift']),560); // cover uses width here
screen.clientWidth=500;screen.clientHeight=1360;resize();
assert.equal(parseFloat(props['--home-shift']),560); // cover uses height here
assert.match(html,/\.battery-value \{[^}]*translateY\(calc\(0px - var\(--home-shift, 0px\)\)\) scale\(1\.2\)/);
assert.match(html,/transform: translate\(-50%, calc\(0px - var\(--home-shift, 0px\)\)\) scale\(1\.2\)/);
assert.match(html,/\.clock-led, \.date-led \{ filter:brightness\(\.55\);/);
assert.match(html,/\.home-weather \{[^}]*brightness\(1\.4\)/);
assert.match(html,/@keyframes datePulse \{ from \{ opacity: 0\.8; \} to \{ opacity: 1;/);
assert(classes.has('glass-over-home'));toggle.checked=false;toggle.change();assert(!classes.has('glass-over-home'));toggle.checked=true;toggle.change();assert(classes.has('glass-over-home'));
console.log('Glass experiment: preview-only layering toggle, touch-through and app pages above glass: OK');
