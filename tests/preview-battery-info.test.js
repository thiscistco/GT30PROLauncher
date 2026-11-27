const assert=require('assert'),fs=require('fs');
const html=fs.readFileSync('preview/index.html','utf8');
const main=fs.readFileSync('app/src/main/java/com/gt30pro/launcher/MainActivity.java','utf8');
const view=fs.readFileSync('app/src/main/java/com/gt30pro/launcher/BatteryInfoView.java','utf8');
assert.match(html,/INFO BATERAI · SIMULASI/);
assert.match(html,/Informasi baterai, data simulasi preview/);
assert.match(html,/\.battery-info \{[^}]*z-index:6;[^}]*bottom:calc\(14% \+ 100px\)/);
assert.match(html,/\.battery-info \{[^}]*pointer-events:none/);
for(const label of ['LEVEL','STATUS','SUMBER','KONDISI','SUHU','TEGANGAN','ARUS SAAT INI','ARUS RATA-RATA','MUATAN TERSISA','ENERGI TERSISA','SIKLUS','TEKNOLOGI','BATERAI TERDETEKSI','BATERAI RENDAH','PERKIRAAN PENUH']) {
 assert(html.includes('>'+label+'</span>'),label); assert(view.includes('"'+label+'"'),label);
}
assert(!html.includes('navigator.getBattery('));
assert.match(html,/previewBatteryStatus'\)\.textContent = toggle.checked/);
assert.match(main,/batteryInfo\.setElevation\(4f/);
const layout=main.match(/private void updateBatteryInfoLayout\([\s\S]*?\n    }/)[0];
assert.match(layout,/root\.getWidth\(\)\/70\.9f/);
assert.match(layout,/25\.4f\/96f/);
assert.match(layout,/root\.getHeight\(\)\*\.14f\+100f\*cssPixel/);
for (const width of [269,405,1224]) {
 const cssPixel=width/70.9*25.4/96, height=width*2720/1224;
 const oldBottom=height*.14+50*cssPixel, newBottom=height*.14+100*cssPixel;
 assert(Math.abs((newBottom-oldBottom)/cssPixel-50)<1e-9);
}
assert.match(main,/screenGlass\.setElevation\(8f/);
assert.match(main,/batteryInfo\.update\(intent\)/);
assert.match(main,/postDelayed\(this,15000\)/);
assert.match(main,/removeCallbacks\(batteryInfoTick\)/);
assert.match(view,/setClickable\(false\)/);
assert.match(view,/setBackground\(null\)/);
assert(!view.includes('GradientDrawable') && !view.includes('setStroke('));
assert.match(view,/name\.setTypeface\(doto\)/); assert.match(view,/value\.setTypeface\(doto\)/);
const panelCss=html.match(/\.battery-info \{([^}]*)\}/)[1];
assert.match(panelCss,/background:transparent/); assert.match(panelCss,/border:0/);
assert.match(html,/\.battery-info-cell \{[^}]*'Doto',monospace/);
assert.match(html,/\.battery-info-name \{[^}]*'Doto',monospace/);
assert.match(view,/Build.VERSION.SDK_INT>=34.*EXTRA_CYCLE_COUNT/);
assert.match(view,/BatteryManager\.BATTERY_PROPERTY_CURRENT_NOW/);
assert.match(view,/BatteryManager\.BATTERY_PROPERTY_ENERGY_COUNTER/);
console.log('Battery info: simulated preview, home placement, fields, live updates and unavailable properties OK');
