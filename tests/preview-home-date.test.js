const assert=require('assert'), fs=require('fs');
const date=require('../preview/home-date.js');
let monday=new Date(2026,10,23,12), days=['SEN','SEL','RAB','KAM','JUM','SAB','MIN'];
for(let i=0;i<7;i++) assert.equal(date.format(new Date(2026,10,23+i,12)).weekday,days[i]);
const months=['JAN','FEB','MAR','APR','MEI','JUN','JUL','AGU','SEP','OKT','NOV','DES'];
for(let m=0;m<12;m++) assert.equal(date.format(new Date(2026,m,1,12)).month,months[m]);
assert.deepStrictEqual(date.format(new Date(2026,10,24,12)),{weekday:'SEL',day:'24',month:'NOV',description:'Selasa, 24 November 2026'});
assert.equal(date.format(new Date(2024,1,29,12)).day,'29');
assert.equal(date.format(new Date(2027,0,1,12)).day,'01');
// Run the actual mounted timer across midnight in the local timezone.
const vm=require('vm'), elements={};
for(const id of ['homeDate','homeDateWeekday','homeDateNumber','homeDateMonth']) elements[id]={textContent:'',attrs:{},setAttribute(k,v){this.attrs[k]=v;}};
let current=new Date(2026,11,31,23,59,59), scheduled;
class FakeDate extends Date { constructor(...args){super(...(args.length ? args : [current.getTime()]));} }
const win={document:{getElementById:id=>elements[id]},setTimeout(fn){scheduled=fn;}};
vm.runInNewContext(fs.readFileSync('preview/home-date.js','utf8'),{window:win,Date:FakeDate,String});
assert.equal(elements.homeDateWeekday.textContent,'KAM');assert.equal(elements.homeDateNumber.textContent,'31');assert.equal(elements.homeDateMonth.textContent,'DES');
current=new Date(2027,0,1,0,0,0);scheduled();
assert.equal(elements.homeDateWeekday.textContent,'JUM');assert.equal(elements.homeDateNumber.textContent,'01');assert.equal(elements.homeDateMonth.textContent,'JAN');
assert.equal(elements.homeDate.attrs['aria-label'],'Jumat, 1 Januari 2027');
const html=fs.readFileSync('preview/index.html','utf8'), java=fs.readFileSync('app/src/main/java/com/gt30pro/launcher/MainActivity.java','utf8'), view=fs.readFileSync('app/src/main/java/com/gt30pro/launcher/HomeDateView.java','utf8');
const css=html.match(/\.date-led\s*\{([^}]*)\}/)[1];
assert.match(css,/left:\s*49%/);assert.match(css,/bottom:\s*calc\(6%\s*\+\s*101\.55px\)/);assert.match(css,/font-size:\s*2mm/);assert.match(css,/pointer-events:\s*none/);
assert.match(html,/id="homeDateWeekday"[\s\S]*id="homeDateNumber"[\s\S]*id="homeDateMonth"/);
assert.match(html,/src="home-date\.js\?v=1"/); assert.match(java,/width \* \.49f - boxWidth \/ 2f/);
const dateLayout = java.match(/private void updateDateLayout\([\s\S]*?\n    }/)[0];
assert.match(dateLayout,/float bottom = height \* \.06f \+ 101\.55f \* cssPixel/);
assert.match(java,/dateLed\.setDate\(now\.toInstant\(\)\.atZone\(ZoneId\.systemDefault\(\)\)\.toLocalDate\(\)\)/);
assert.match(view,/setLayerType\(View\.LAYER_TYPE_SOFTWARE/); assert.match(view,/setContentDescription\(HomeDateText\.description/);
assert(!/separatorAlpha/.test(view),'charging must not affect date');
for(const w of [269,720,1224]) {
 const mm=w/70.9, cssPixel=mm*25.4/96, clockCenter=w*.357-.5*cssPixel, dateCenter=w*.49;
 assert(dateCenter - clockCenter > (3.6+5.2)/2*mm,'date must not overlap clock');
 const h=w*2720/1224, oldTop=h-h*.06-1.55*cssPixel, newTop=h-h*.06-101.55*cssPixel;
 assert(Math.abs((oldTop-newTop)-100*cssPixel)<1e-9, 'exactly 100 CSS px upwards on Android');
}
console.log('Preview home date: local abbreviations, layout parity and clock separation: OK');
