const assert=require('assert'),fs=require('fs');
const html=fs.readFileSync('preview/index.html','utf8');
const main=fs.readFileSync('app/src/main/java/com/gt30pro/launcher/MainActivity.java','utf8');
const view=fs.readFileSync('app/src/main/java/com/gt30pro/launcher/BatteryInfoView.java','utf8');
assert.match(html,/INFO BATERAI · SIMULASI/);
assert.match(html,/Informasi baterai, data simulasi preview/);
assert.match(html,/\.battery-info \{[^}]*z-index:6;[^}]*bottom:14%/);
assert.match(html,/\.battery-info \{[^}]*pointer-events:none/);
for(const label of ['LEVEL','STATUS','SUMBER','KONDISI','SUHU','TEGANGAN','ARUS SAAT INI','ARUS RATA-RATA','MUATAN TERSISA','ENERGI TERSISA','SIKLUS','TEKNOLOGI','BATERAI TERDETEKSI','BATERAI RENDAH','PERKIRAAN PENUH']) {
 assert(html.includes('>'+label+'</span>'),label); assert(view.includes('"'+label+'"'),label);
}
assert(!html.includes('navigator.getBattery('));
assert.match(html,/previewBatteryStatus'\)\.textContent = toggle.checked/);
assert.match(main,/batteryInfo\.setElevation\(4f/);
assert.match(main,/screenGlass\.setElevation\(8f/);
assert.match(main,/batteryInfo\.update\(intent\)/);
assert.match(main,/postDelayed\(this,15000\)/);
assert.match(main,/removeCallbacks\(batteryInfoTick\)/);
assert.match(view,/setClickable\(false\)/);
assert.match(view,/Build.VERSION.SDK_INT>=34.*EXTRA_CYCLE_COUNT/);
assert.match(view,/BatteryManager\.BATTERY_PROPERTY_CURRENT_NOW/);
assert.match(view,/BatteryManager\.BATTERY_PROPERTY_ENERGY_COUNTER/);
console.log('Battery info: simulated preview, home placement, fields, live updates and unavailable properties OK');
