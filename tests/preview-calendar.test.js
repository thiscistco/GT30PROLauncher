// Run with: node tests/preview-calendar.test.js
const assert = require('assert'), fs = require('fs');
const calendar = require('../preview/calendar.js');
assert.equal(calendar.daysInMonth(2024,2), 29);
assert.equal(calendar.daysInMonth(2025,2), 28);
assert.equal(calendar.leadingDays(2026,11), 6);
assert.equal(calendar.leadingDays(2024,1), 0);
for (let month=1; month<=12; month++) assert.equal(calendar.leadingDays(2024,month), (new Date(2024,month-1,1).getDay()+6)%7);
assert.deepEqual(calendar.shiftMonth({year:2024,month:1,day:31},1), {year:2024,month:2,day:29});
assert.deepEqual(calendar.shiftMonth({year:2026,month:12,day:31},1), {year:2027,month:1,day:31});
assert.deepEqual(calendar.shiftMonth({year:1900,month:1,day:31},-1), {year:1900,month:1,day:31});
assert.deepEqual(calendar.shiftMonth({year:2100,month:12,day:31},1), {year:2100,month:12,day:31});
assert(calendar.validTime('') && calendar.validTime('23:59') && !calendar.validTime('24:00') && !calendar.validTime('9:30'));
assert(calendar.validDate('2024-02-29') && !calendar.validDate('2025-02-29') && !calendar.validDate('2101-01-01'));
assert(calendar.validTitle(' Acara ') && !calendar.validTitle('  ') && !calendar.validTitle('x'.repeat(81)));
class Element {
  constructor(){this.hidden=false;this.children=[];this.listeners={};this.attrs={};this.value='';this.textContent='';this.disabled=false;this.classes=new Set();this.classList={toggle:(name,on)=>on?this.classes.add(name):this.classes.delete(name)};}
  addEventListener(type,fn){this.listeners[type]=fn;}
  setAttribute(k,v){this.attrs[k]=v;}
  appendChild(e){this.children.push(e);}
  replaceChildren(){this.children=[];}
  focus(){this.focused=true;}
  fire(type){if(this.listeners[type]) this.listeners[type]({preventDefault(){}});}
}
const key='gt30-calendar-events-v1', storage=new Map(); let confirmAnswer=true;
function createUI(){
 const ids = {};
 for (const id of ['calendarGrid','calendarForm','calendarStorageNote','calendarError','calendarMonth','calendarSelected','calendarPrev','calendarNext','calendarEvents','calendarToday','calendarAdd','calendarCancel','calendarTitle','calendarTime']) ids[id]=new Element();
 ids.calendarForm.hidden=true; ids.calendarForm.reset=()=>{ids.calendarTitle.value='';ids.calendarTime.value='';};
 const doc={getElementById:id=>ids[id],createElement:()=>new Element()};
 const win={document:doc,localStorage:{getItem:k=>storage.has(k)?storage.get(k):null,setItem:(k,v)=>storage.set(k,v)},confirm:()=>confirmAnswer};
 calendar.mount(win); return ids;
}
let ui=createUI();
assert.equal(ui.calendarGrid.children.filter(e=>e.attrs['aria-pressed']==='true').length,1);
assert.deepEqual(ui.calendarGrid.children.slice(0,7).map(e=>e.textContent), ['Sen','Sel','Rab','Kam','Jum','Sab','Min']);
ui.calendarGrid.children.forEach((e,i)=>{ if(i<7 || e.attrs['aria-pressed']!==undefined) assert.equal(e.classes.has('weekend'), i%7>=5, 'weekend color for column '+i%7); });
const weekendDay = ui.calendarGrid.children.find((e,i)=>i>=7 && i%7>=5 && e.attrs['aria-pressed']!==undefined);
weekendDay.fire('click'); assert(ui.calendarGrid.children.find(e=>e.attrs['aria-pressed']==='true').classes.has('weekend'), 'selected weekend stays red');
ui.calendarToday.fire('click');
const selected=ui.calendarSelected.textContent;
ui.calendarAdd.fire('click'); assert.equal(ui.calendarForm.hidden,false);
ui.calendarTitle.value=' <img src=x> '; ui.calendarTime.value='09:30'; ui.calendarForm.fire('submit');
assert.equal(ui.calendarForm.hidden,true); assert.equal(ui.calendarEvents.children.length,1);
assert.equal(ui.calendarEvents.children[0].children[0].textContent,'09:30 · <img src=x>');
assert.equal(JSON.parse(storage.get(key))[0].title,'<img src=x>');
ui=createUI(); assert.equal(ui.calendarSelected.textContent,selected); assert.equal(ui.calendarEvents.children.length,1);
confirmAnswer=false; ui.calendarEvents.children[0].children[1].fire('click'); assert.equal(JSON.parse(storage.get(key)).length,1);
confirmAnswer=true; ui.calendarEvents.children[0].children[1].fire('click'); assert.equal(JSON.parse(storage.get(key)).length,0);
ui.calendarAdd.fire('click'); ui.calendarTitle.value=' '; ui.calendarForm.fire('submit'); assert.equal(ui.calendarError.hidden,false);
const month=ui.calendarMonth.textContent; ui.calendarNext.fire('click'); assert.notEqual(ui.calendarMonth.textContent,month); ui.calendarToday.fire('click'); assert.equal(ui.calendarMonth.textContent,month);
storage.set(key,'not valid json'); ui=createUI(); ui.calendarAdd.fire('click'); ui.calendarTitle.value='Temporary'; ui.calendarForm.fire('submit');
assert.equal(storage.get(key),'not valid json','corrupt saved data must not be overwritten');
assert.match(ui.calendarStorageNote.textContent,/sementara/);
const html=fs.readFileSync('preview/index.html','utf8');
assert.match(html,/id="openCalendarModule"/); assert.match(html,/id="calendarPage"/);
assert.match(html,/src="calendar\.js\?v=2"/);
assert.match(html,/\.calendar-weekday\.weekend, \.calendar-day\.weekend \{ color: #ff6b6b; \}/);
console.log('Calendar preview: date rules, local persistence, deletion, safe text and navigation: OK');
