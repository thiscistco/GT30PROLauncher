const assert=require('assert'), fs=require('fs'), vm=require('vm');
const weather=require('../preview/weather.js');
assert.equal(weather.kind(-1,true),'unknown');assert.equal(weather.kind(0,true),'sun');assert.equal(weather.kind(0,false),'moon');
assert.equal(weather.kind(3,true),'cloud');assert.equal(weather.kind(45,true),'fog');assert.equal(weather.kind(61,true),'rain');assert.equal(weather.kind(71,true),'snow');assert.equal(weather.kind(95,true),'storm');
const javaIcons=fs.readFileSync('app/src/main/java/com/gt30pro/launcher/WeatherIcons.java','utf8');
for (const [name,rows] of Object.entries(weather.icons)) {
 assert.equal(rows.length,13); rows.forEach(row=>assert.match(row,/^[.#]{13}$/));
 const match=javaIcons.match(new RegExp('String\\[\\] '+name.toUpperCase()+' = \\{([^}]+)\\}'));
 assert(match,name); assert.deepEqual([...match[1].matchAll(/"([.#]+)"/g)].map(m=>m[1]),rows);
}
const html=fs.readFileSync('preview/index.html','utf8'),native=fs.readFileSync('app/src/main/java/com/gt30pro/launcher/WeatherController.java','utf8');
assert.match(html,/id="homeWeather"/);assert.match(html,/id="superWeather"/);assert.match(html,/bottom: calc\(6% \+ 460px\)/);
const superButton=html.match(/<button class="weather-widget super-weather"[\s\S]*?<\/button>/)[0];
assert(!superButton.includes('weather-temp'));assert.match(html,/\.super-weather svg \{ width: 8mm; height: 8mm; top: 50%/);
const view=fs.readFileSync('app/src/main/java/com/gt30pro/launcher/WeatherView.java','utf8');
assert.match(view,/if \(home\) \{\s*paint\.setTextSize/);assert.match(native,/pendingManual/);
assert.match(native,/ACCESS_COARSE_LOCATION/);assert(!native.includes('ACCESS_FINE_LOCATION'));assert.match(native,/Math\.round\(loc\.getLatitude\(\)\*100d\)\/100d/);
assert.match(native,/LocationManager\.PASSIVE_PROVIDER/);assert.match(native,/recentLocation\(\)/);assert.match(native,/api\.open-meteo\.com/);assert.match(native,/30 \* 60 \* 1000L/);assert.match(native,/stopLocation\(\)/);
assert.match(fs.readFileSync('app/src/main/res/xml/backup_rules.xml','utf8'),/weather_cache.xml/);
assert.match(fs.readFileSync('app/src/main/res/xml/data_extraction_rules.xml','utf8'),/weather_cache.xml/);
// Mount the actual browser code in a minimal DOM; verify no unsolicited location request,
// explicit consent, rounded-coordinate request, home temperature and both icons updating.
function element() { return {attrs:{}, children:[], textContent:'', title:'', listeners:{}, setAttribute(k,v){this.attrs[k]=String(v)}, appendChild(v){this.children.push(v)}, removeChild(){this.children.shift()}, get firstChild(){return this.children[0]}, addEventListener(k,fn){this.listeners[k]=fn}}; }
const refreshButton=element(), buttons=[element(),element()];buttons.forEach((b,i)=>{b.svg=element();b.label=i===0?element():null;b.querySelector=s=>s==='svg'?b.svg:b.label;});
let requested=0,url='', confirmCalls=0;
const doc={getElementById:id=>id==='homeWeather'?buttons[0]:(id==='superWeather'?buttons[1]:refreshButton),createElementNS:()=>element()};
const win={document:doc,navigator:{geolocation:{getCurrentPosition(ok){requested++;ok({coords:{latitude:-6.12345,longitude:110.98765}});}}},confirm(){confirmCalls++;return true},alert(msg){throw Error(msg)}};
const ctx={window:win,document:doc,fetch:u=>{url=u;return Promise.resolve({ok:true,json:()=>Promise.resolve({current:{temperature_2m:28.6,weather_code:61,is_day:1}})})},setTimeout:()=>1,clearTimeout:()=>{},Date,Math,Number};
vm.runInNewContext(fs.readFileSync('preview/weather.js','utf8'),ctx);
assert.equal(requested,0);assert.equal(buttons[0].label.textContent,'--°');assert.equal(typeof refreshButton.listeners.click,'function');refreshButton.listeners.click();
setImmediate(()=>{assert.equal(confirmCalls,1);assert.equal(requested,1);assert(url.includes('latitude=-6.12&longitude=110.99'));assert.equal(buttons[0].label.textContent,'29°');assert.equal(buttons[1].label,null);assert.equal(buttons[1].listeners.click,undefined,'Super icon opens page via navigation, not a direct location request');assert.equal(buttons[0].listeners.click,undefined,'Home icon does not request location directly');assert.match(buttons[0].attrs['aria-label'],/Ketuk untuk membuka Cuaca/);assert.equal(buttons[0].svg.children.length,buttons[1].svg.children.length);assert(buttons[0].attrs['aria-label'].includes('Hujan'));assert(buttons[1].attrs['aria-label'].includes('29 derajat'));console.log('Weather: consent, rounded location, live icons and home temperature: OK');});
// Denied consent must never request geolocation. A denied permission leaves --°.
{
 const bs=[element(),element()];bs.forEach((b,i)=>{b.svg=element();b.label=i===0?element():null;b.querySelector=s=>s==='svg'?b.svg:b.label;});
 let calls=0, alerts=0, consent=false;
 const refresh2=element(); const doc2={getElementById:id=>id==='homeWeather'?bs[0]:(id==='superWeather'?bs[1]:(id==='weatherPageRefresh'?refresh2:null)),createElementNS:()=>element()};
 const win2={document:doc2,navigator:{geolocation:{getCurrentPosition(ok,fail){calls++;fail({code:1});}}},confirm(){return consent},alert(){alerts++}};
 vm.runInNewContext(fs.readFileSync('preview/weather.js','utf8'),{window:win2,document:doc2,fetch(){throw Error('fetch on denied permission')},setTimeout:()=>1,clearTimeout(){},Date,Math,Number});
 refresh2.listeners.click();assert.equal(calls,0);consent=true;refresh2.listeners.click();assert.equal(calls,1);assert.equal(alerts,1);assert.equal(bs[0].label.textContent,'--°');assert.equal(bs[1].label,null);
}
