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
assert.match(html,/<script src="glass-preview\.js\?v=1"><\/script>/);
const classes=new Set(['glass-over-home']);
const screen={classList:{toggle(name,on){if(on) classes.add(name);else classes.delete(name);}}};
const toggle={checked:true,addEventListener(name,fn){assert.equal(name,'change');this.change=fn;}};
vm.runInNewContext(fs.readFileSync('preview/glass-preview.js','utf8'),{document:{getElementById:id=>id==='glassPreviewToggle'?toggle:null,querySelector:q=>q==='.screen'?screen:null}});
assert(classes.has('glass-over-home'));toggle.checked=false;toggle.change();assert(!classes.has('glass-over-home'));toggle.checked=true;toggle.change();assert(classes.has('glass-over-home'));
console.log('Glass experiment: preview-only layering toggle, touch-through and app pages above glass: OK');
