const assert = require('assert'), fs = require('fs'), vm = require('vm');
function element() {
  return {children:[], listeners:{}, setAttribute(){}, appendChild(v){this.children.push(v)}, removeChild(){this.children.shift()},
    get firstChild(){return this.children[0]}, addEventListener(event,fn){this.listeners[event]=fn}, querySelector(s){return s==='svg'?this.svg:this.label}};
}
const home=element(), superIcon=element(), refresh=element();
for (const button of [home,superIcon]) { button.svg=element(); button.label=button===home?element():null; }
const doc={getElementById:id=>({homeWeather:home,superWeather:superIcon,weatherPageRefresh:refresh}[id]||null),createElementNS:()=>element()};
let calls=0, permission=true, network=true, observed, consent=false;
const win={document:doc,navigator:{geolocation:{getCurrentPosition(ok,bad){calls++;if(!permission)bad({code:1});else ok({coords:{latitude:-6.12345,longitude:110.98765}})}}},
  confirm:()=>consent, alert(){}, updateWeatherPage(data,status,place){observed={data,status,place}}};
const fetch=()=>network?Promise.resolve({ok:true,json:()=>Promise.resolve({current:{temperature_2m:29,weather_code:61,is_day:1}})}):Promise.reject(Error('offline'));
vm.runInNewContext(fs.readFileSync('preview/weather.js','utf8'),{window:win,document:doc,fetch,setTimeout:()=>1,clearTimeout(){},Date,Math,Number});
const tick=()=>new Promise(resolve=>setImmediate(resolve));
(async()=>{
 assert.equal(calls,0); assert.equal(observed.place,null);
 refresh.listeners.click(); assert.equal(calls,0, 'cancelled consent does not use location');
 consent=true; refresh.listeners.click(); await tick();
 assert.equal(calls,1);assert.equal(observed.place,'-6.12, 110.99 · perkiraan');assert(observed.data);
 network=false;refresh.listeners.click();await tick();assert(observed.data);assert.match(observed.status,/lokasinya mungkin berbeda/);
 permission=false;refresh.listeners.click();await tick();assert.equal(observed.place,null);assert.equal(observed.data,null);assert.match(observed.status,/izin ditolak/);
 console.log('Weather location: consent, rounded coordinates, offline cache caveat and denied permission: OK');
})().catch(e=>{console.error(e);process.exitCode=1});
