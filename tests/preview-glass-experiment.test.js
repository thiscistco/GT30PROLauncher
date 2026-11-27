// Preview-only comparison of the existing glass artwork and home layering.
const assert=require('assert'),fs=require('fs'),vm=require('vm');
const html=fs.readFileSync('preview/index.html','utf8');
assert.match(html,/<div class="screen glass-over-home">/);
assert.match(html,/id="glassPreviewToggle"[^>]*checked/);
assert.match(html,/APK menggunakan ikon dan label di balik kaca/);
assert.match(html,/\.home-screen-glass \{[^}]*z-index:1;[^}]*pointer-events:none/);
assert.match(html,/\.screen\.glass-over-home \.home-screen-glass \{ z-index:7; \}/);
assert.match(html,/\.screen\.glass-over-home \.punch-hole \{ z-index:8; \}/);
assert.match(html,/\.super-page \{[^}]*z-index: 9/);
assert.match(html,/\.path-draw \{[^}]*z-index: 12/);
assert.match(html,/<script src="glass-preview\.js\?v=11"><\/script>/);
const classes=new Set(['glass-over-home']);
const props={};const screen={clientWidth:269,clientHeight:597,style:{setProperty(k,v){props[k]=v;}},classList:{toggle(name,on){if(on) classes.add(name);else classes.delete(name);}}};
const toggle={checked:true,addEventListener(name,fn){assert.equal(name,'change');this.change=fn;}};
let resize;
vm.runInNewContext(fs.readFileSync('preview/glass-preview.js','utf8'),{document:{getElementById:id=>id==='glassPreviewToggle'?toggle:null,querySelector:q=>q==='.screen'?screen:null},window:{addEventListener(name,fn){assert.equal(name,'resize');resize=fn;}}});
for (const [property, pixels] of [['--clock-shift',2130],['--clock-right',650],['--battery-shift',1470],['--battery-right',495]]) {
  assert(Math.abs(parseFloat(props[property]) - pixels*Math.max(269/1224,597/2720))<1e-9);
}
screen.clientWidth=612;screen.clientHeight=1000;resize();
assert.equal(parseFloat(props['--clock-shift']),1065); // cover uses width here
assert.equal(parseFloat(props['--clock-right']),325);
assert.equal(parseFloat(props['--battery-shift']),735);
assert.equal(parseFloat(props['--battery-right']),247.5);
screen.clientWidth=500;screen.clientHeight=1360;resize();
assert.equal(parseFloat(props['--clock-shift']),1065); // cover uses height here
assert.equal(parseFloat(props['--clock-right']),325);
assert.equal(parseFloat(props['--battery-shift']),735);
assert.equal(parseFloat(props['--battery-right']),247.5);
assert.match(html,/\.battery-value \{[^}]*translate\(var\(--battery-right, 0px\), calc\(0px - var\(--battery-shift, 0px\)\)\) scale\(1\.35\)/);
assert.match(html,/transform: translate\(calc\(-50% \+ var\(--clock-right, 0px\)\), calc\(0px - var\(--clock-shift, 0px\)\)\) scale\(1\.2\)/);
assert.match(html,/\.home-weather \{[^}]*color: #ffcd6a/);
assert.match(html,/\.battery-led\.charging \{[^}]*color: #ffcd6a/);
assert.match(fs.readFileSync('preview/circuit-editor.js','utf8'),/rgba\(255,205,106,0\.92\)/); // same circuit core
assert.match(html,/\.date-led #homeDateWeekday, \.date-led #homeDateMonth \{[^}]*color:#263840/);
assert.match(html,/@keyframes datePulse \{ from \{ opacity: 0\.8; \} to \{ opacity: 1;/);
assert(classes.has('glass-over-home'));toggle.checked=false;toggle.change();assert(!classes.has('glass-over-home'));toggle.checked=true;toggle.change();assert(classes.has('glass-over-home'));
console.log('Glass experiment: preview-only layering toggle, touch-through and app pages above glass: OK');
