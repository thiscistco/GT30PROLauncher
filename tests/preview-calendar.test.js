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
  fire(type,extra={}){if(this.listeners[type]) this.listeners[type].call(this,{preventDefault(){},stopPropagation(){},...extra});}
}
const key='gt30-calendar-events-v1', storage=new Map(); let confirmAnswer=true;
function createUI(){
 const ids = {};
 for (const id of ['calendarGrid','calendarForm','calendarStorageNote','calendarError','calendarMonth','calendarSelected','calendarPrev','calendarNext','calendarEvents','calendarToday','calendarAdd','calendarCancel','calendarTitle','calendarTime','calendarHolidayDetails','calendarHolidayStatus','calendarMonthHolidays','calendarRefresh','calendarPage','calendarTheme','calendarMonthMenu','calendarWarning','calendarDayLabel']) ids[id]=new Element();
 ids.calendarMonthMenu.hidden=true; ids.calendarForm.hidden=true; ids.calendarForm.reset=()=>{ids.calendarTitle.value='';ids.calendarTime.value='';};
 const doc={getElementById:id=>ids[id],createElement:()=>new Element()};
 const media={matches:true,addEventListener(type,fn){this.change=fn;}};
 const win={document:doc,localStorage:{getItem:k=>storage.has(k)?storage.get(k):null,setItem:(k,v)=>storage.set(k,v)},confirm:()=>confirmAnswer,matchMedia:()=>media,setTimeout};
 calendar.mount(win); ids._win=win; ids._media=media; return ids;
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
assert.match(html,/src="calendar\.js\?v=7"/);
assert.match(html,/\.calendar-weekday\.weekend, \.calendar-day\.weekend, \.calendar-day\.holiday \{ color: #b3263b; \}/);
assert.equal(calendar.holidaysFor('2026-08-17')[0].type,'libur');
assert.equal(calendar.holidaysFor('2026-04-21')[0].type,'peringatan');
assert.equal(calendar.holidaysFor('2026-03-20')[0].type,'cuti');
assert.deepEqual(calendar.holidaysFor('2028-08-17'),[]);
function navigateTo(ui, year, month) {
  const now=new Date(); const delta=(year-now.getFullYear())*12+month-(now.getMonth()+1);
  for(let i=0;i<Math.abs(delta);i++) ui[delta>0?'calendarNext':'calendarPrev'].fire('click');
}
let aug=createUI(); navigateTo(aug,2026,8);
let holiday=aug.calendarGrid.children.find(e=>e.textContent==='17');
assert(holiday.classes.has('holiday')); assert(holiday.classes.has('marker-holiday')); assert(!holiday.classes.has('marker-observance')); assert.match(holiday.attrs['aria-label'],/Libur nasional.*Kemerdekaan/);
holiday.fire('click'); assert.match(aug.calendarHolidayDetails.children[0].textContent,/Libur nasional.*Kemerdekaan/);
assert(aug.calendarMonthHolidays.children.some(e=>/14 · Peringatan/.test(e.textContent)));
let pramuka=aug.calendarGrid.children.find(e=>e.textContent==='14');
assert(pramuka.classes.has('marker-observance') && !pramuka.classes.has('marker-holiday')); pramuka.fire('click');
assert(aug.calendarGrid.children.find(e=>e.textContent==='14').classes.has('marker-observance'), 'marker remains when selected');
let ordinary=aug.calendarGrid.children.find(e=>e.textContent==='18');
assert(!ordinary.classes.has('marker-observance') && !ordinary.classes.has('marker-holiday'));
const dataset=require('../preview/holiday-data.js');
dataset.entries['2026-08-17'].push({type:'peringatan',name:'Uji dua jenis'});
let both=createUI(); navigateTo(both,2026,8); let bothDay=both.calendarGrid.children.find(e=>e.textContent==='17');
assert(bothDay.classes.has('marker-holiday') && bothDay.classes.has('marker-observance')); dataset.entries['2026-08-17'].pop();
let april=createUI(); navigateTo(april,2026,4); let kartini=april.calendarGrid.children.find(e=>e.textContent==='21');
assert(!kartini.classes.has('holiday')); kartini.fire('click'); assert.match(april.calendarHolidayDetails.children[0].textContent,/bukan libur nasional.*Kartini/);
let march=createUI(); navigateTo(march,2026,3); let cuti=march.calendarGrid.children.find(e=>e.textContent==='20');
assert(!cuti.classes.has('holiday') && !cuti.classes.has('marker-holiday') && !cuti.classes.has('marker-observance')); cuti.fire('click'); assert.match(march.calendarHolidayDetails.children[0].textContent,/Cuti bersama/);
let missing=createUI(); navigateTo(missing,2028,1); assert.match(missing.calendarHolidayStatus.textContent,/belum tersedia/);
assert(missing.calendarGrid.children.every(e=>!e.classes.has('marker-holiday')&&!e.classes.has('marker-observance')));
assert.match(html,/marker-holiday::after/); assert.match(html,/marker-observance::after/); assert.match(html,/Titik merah: libur nasional · titik amber: peringatan/);
storage.clear(); let events=createUI(); navigateTo(events,2026,8);
events.calendarGrid.children.find(e=>e.textContent==='17').fire('click');
events.calendarAdd.fire('click'); events.calendarTitle.value='Acara'; events.calendarForm.fire('submit');
let eventHoliday=events.calendarGrid.children.find(e=>e.textContent==='17·');
assert(eventHoliday.classes.has('marker-holiday')); assert.match(eventHoliday.attrs['aria-label'],/ada acara/);
events.calendarEvents.children[0].children[1].fire('click');
assert(events.calendarGrid.children.find(e=>e.textContent==='17').classes.has('marker-holiday'));
console.log('Calendar preview: date rules, holidays, local persistence, deletion, safe text and navigation: OK');

const nativeCalendar=fs.readFileSync('app/src/main/java/com/gt30pro/launcher/CalendarActivity.java','utf8');
const mainActivity=fs.readFileSync('app/src/main/java/com/gt30pro/launcher/MainActivity.java','utf8');
assert.match(html,/class="calendar-card"/);assert.match(html,/radial-gradient\(circle 3\.2mm/);
assert.match(html,/id="homeDate" role="button" tabindex="0"/);
assert.match(mainActivity,/dateBox\.setOnClickListener\(v -> startActivity\(new Intent\(this, CalendarActivity\.class\)\)\)/);
assert.match(nativeCalendar,/dayBackground\(/);assert.match(nativeCalendar,/0xFFF4F7FA/);assert.match(nativeCalendar,/fonts\/Doto\.ttf/);
assert.equal(calendar.swipeDirection(-60,4,48),1);assert.equal(calendar.swipeDirection(60,4,48),-1);
assert.equal(calendar.swipeDirection(12,2,48),0);assert.equal(calendar.swipeDirection(60,55,48),0);
assert.match(nativeCalendar,/refreshHolidays\(true\)/);assert.match(nativeCalendar,/new MonthGrid\(\)/);
assert.match(html,/id="calendarTheme"/);assert.match(html,/id="calendarMonthMenu"[^>]*hidden/);
assert(!html.includes('id="calendarPrev" type="button" aria-label="Bulan sebelumnya">&lt;'));


storage.clear(); const interactive=createUI();
assert.equal(interactive.calendarTheme.value,'system'); assert.equal(interactive.calendarPage.attrs['data-theme'],'dark');
interactive._media.matches=false;interactive._media.change();assert.equal(interactive.calendarPage.attrs['data-theme'],'light');
interactive.calendarTheme.value='dark';interactive.calendarTheme.fire('change');assert.equal(storage.get('gt30-calendar-theme'),'dark');
interactive._media.change();assert.equal(interactive.calendarPage.attrs['data-theme'],'dark');
const initialMonth=interactive.calendarMonth.textContent;
function drag(x,y,x2,y2) {
 const e={pointerId:9,isPrimary:true,button:0,clientX:x,clientY:y,cancelable:true};
 interactive.calendarGrid.fire('pointerdown',e);interactive.calendarGrid.fire('pointermove',{...e,clientX:x2,clientY:y2});interactive.calendarGrid.fire('pointerup',{...e,clientX:x2,clientY:y2});
}
drag(200,100,120,100);assert.notEqual(interactive.calendarMonth.textContent,initialMonth);
drag(120,100,200,100);assert.equal(interactive.calendarMonth.textContent,initialMonth);
drag(200,100,190,180);assert.equal(interactive.calendarMonth.textContent,initialMonth);
interactive.calendarMonth.fire('click');assert(!interactive.calendarMonthMenu.hidden);interactive.calendarNext.fire('click');assert(interactive.calendarMonthMenu.hidden);
interactive.calendarToday.fire('click');interactive.calendarAdd.fire('click');interactive.calendarTitle.value='Draft';
interactive.calendarGrid.children.find(e=>e.attrs['aria-pressed']==='false').fire('click');assert(interactive.calendarForm.hidden);assert.equal(interactive.calendarTitle.value,'');
console.log('Calendar preview: swipe, theme selection, system mode and discarded draft: OK');
