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
function createUI(openMonth=true){
 const ids = {};
 for (const id of ['calendarGrid','calendarForm','calendarStorageNote','calendarError','calendarMonth','calendarSelected','calendarPrev','calendarNext','calendarEvents','calendarToday','calendarAdd','calendarCancel','calendarTitle','calendarTime','calendarHolidayDetails','calendarHolidayStatus','calendarMonthHolidays','calendarRefresh','calendarPage','calendarTheme','calendarThemeIcon','calendarThemeMenu','calendarThemeSystem','calendarThemeLight','calendarThemeDark','calendarMonthMenu','calendarWarning','calendarDayLabel','calendarExpand','calendarYear','calendarAgendaHeading']) ids[id]=new Element();
 ids.calendarThemeMenu.hidden=true; ids.calendarMonthMenu.hidden=true; ids.calendarForm.hidden=true; ids.calendarForm.reset=()=>{ids.calendarTitle.value='';ids.calendarTime.value='';};
 const doc={getElementById:id=>ids[id],createElement:()=>new Element()};
 const media={matches:true,addEventListener(type,fn){this.change=fn;}};
 const win={document:doc,localStorage:{getItem:k=>storage.has(k)?storage.get(k):null,setItem:(k,v)=>storage.set(k,v)},confirm:()=>confirmAnswer,matchMedia:()=>media,setTimeout,setInterval:()=>0};
 calendar.mount(win); if(openMonth) ids.calendarExpand.fire('click'); ids._win=win; ids._media=media; return ids;
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
assert.match(ui.calendarEvents.children[0].children[0].textContent,/09:30\n<img src=x>/);
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
assert.match(html,/src="calendar\.js\?v=10"/);
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
assert.match(html,/marker-holiday::before/); assert.match(html,/marker-observance::before/); assert(!html.includes('marker-holiday::after')); assert.match(html,/Garis merah: libur nasional/);
storage.clear(); let events=createUI(); navigateTo(events,2026,8);
events.calendarGrid.children.find(e=>e.textContent==='17').fire('click');
events.calendarAdd.fire('click'); events.calendarTitle.value='Acara'; events.calendarForm.fire('submit');
let eventHoliday=events.calendarGrid.children.find(e=>e.textContent==='17');
assert(eventHoliday.classes.has('marker-holiday')); assert(eventHoliday.classes.has('marker-event')); assert.match(eventHoliday.attrs['aria-label'],/ada acara/);
events.calendarExpand.fire('click'); events.calendarEvents.children[0].children[1].fire('click');
assert(events.calendarGrid.children.find(e=>e.textContent==='17').classes.has('marker-holiday'));
console.log('Calendar preview: date rules, holidays, local persistence, deletion, safe text and navigation: OK');

const nativeCalendar=fs.readFileSync('app/src/main/java/com/gt30pro/launcher/CalendarActivity.java','utf8');
const mainActivity=fs.readFileSync('app/src/main/java/com/gt30pro/launcher/MainActivity.java','utf8');
assert.match(html,/class="calendar-card"/);assert.match(html,/calendar-day::before[^}]+border-radius:30%/);
assert.match(html,/id="homeDate" role="button" tabindex="0"/);
assert.match(mainActivity,/dateBox\.setOnClickListener\(v -> startActivity\(new Intent\(this, CalendarActivity\.class\)\)\)/);
assert.match(nativeCalendar,/dayBackground\(/);assert.match(nativeCalendar,/0xFFF4F7FA/);assert.match(nativeCalendar,/fonts\/Doto\.ttf/);
assert.equal(calendar.swipeDirection(-60,4,48),1);assert.equal(calendar.swipeDirection(60,4,48),-1);
assert.equal(calendar.swipeDirection(12,2,48),0);assert.equal(calendar.swipeDirection(60,55,48),0);
assert.match(nativeCalendar,/refreshHolidays\(true\)/);assert.match(nativeCalendar,/new MonthGrid\(\)/);
assert.match(html,/id="calendarTheme"/);assert.match(html,/id="calendarMonthMenu"[^>]*hidden/);
assert(!html.includes('id="calendarPrev" type="button" aria-label="Bulan sebelumnya">&lt;'));


storage.clear(); const interactive=createUI();
assert.match(interactive.calendarTheme.attrs['aria-label'],/Sistem/); assert.match(interactive.calendarThemeIcon.attrs.class,/system/); assert.equal(interactive.calendarPage.attrs['data-theme'],'dark');
interactive._media.matches=false;interactive._media.change();assert.equal(interactive.calendarPage.attrs['data-theme'],'light');
interactive.calendarTheme.fire('click');assert(!interactive.calendarThemeMenu.hidden);assert.equal(interactive.calendarTheme.attrs['aria-expanded'],'true');interactive.calendarThemeDark.fire('click');assert(interactive.calendarThemeMenu.hidden);assert.equal(storage.get('gt30-calendar-theme'),'dark');assert.match(interactive.calendarThemeIcon.attrs.class,/dark/);assert.equal(interactive.calendarThemeDark.attrs['aria-pressed'],'true');
interactive._media.change();assert.equal(interactive.calendarPage.attrs['data-theme'],'dark');
const restored=createUI();assert.match(restored.calendarThemeIcon.attrs.class,/dark/);restored.calendarThemeLight.fire('click');assert.match(restored.calendarThemeIcon.attrs.class,/light/);assert.equal(restored.calendarPage.attrs['data-theme'],'light');restored.calendarThemeSystem.fire('click');assert.match(restored.calendarThemeIcon.attrs.class,/system/);
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

assert.match(html,/id="calendarThemeSystem"[^>]*aria-label="Ikuti perangkat"/);
assert.match(html,/id="calendarThemeLight"[^>]*aria-label="Terang"/);
assert.match(html,/id="calendarThemeDark"[^>]*aria-label="Gelap"/);
assert.match(nativeCalendar,/setContentDescription\("Tema: " \+ themeLabel\(\)/);
assert.match(nativeCalendar,/new PopupWindow\(/);

assert.deepEqual(calendar.weekDays({year:2027,month:1,day:1}),[{year:2026,month:12,day:28},{year:2026,month:12,day:29},{year:2026,month:12,day:30},{year:2026,month:12,day:31},{year:2027,month:1,day:1},{year:2027,month:1,day:2},{year:2027,month:1,day:3}]);
assert.deepEqual(calendar.shiftWeek({year:2024,month:2,day:26},1),{year:2024,month:3,day:4});
assert.deepEqual(calendar.shiftWeek({year:1900,month:1,day:3},-1),{year:1900,month:1,day:1});
assert.deepEqual(calendar.shiftWeek({year:2100,month:12,day:29},1),{year:2100,month:12,day:31});
assert.equal(calendar.weekDays({year:2100,month:12,day:31}).filter(x=>x===null).length,2);
storage.clear();let weekly=createUI(false);
assert.equal(weekly.calendarExpand.attrs['aria-expanded'],'false');assert.equal(weekly.calendarGrid.children.length,14);
const first=weekly.calendarSelected.textContent;
weekly.calendarNext.fire('click');assert.notEqual(weekly.calendarSelected.textContent,first);
weekly.calendarPrev.fire('click');assert.equal(weekly.calendarSelected.textContent,first);
weekly.calendarExpand.fire('click');assert.equal(weekly.calendarExpand.attrs['aria-expanded'],'true');assert(weekly.calendarGrid.children.length>14);
weekly.calendarExpand.fire('click');assert.equal(weekly.calendarGrid.children.length,14);assert.equal(weekly.calendarSelected.textContent,first);
assert.match(html,/id="calendarAdd"[^>]*aria-label="Tambah acara"/);
assert.match(html,/id="calendarBack"[^>]*aria-label="Kembali"/);
assert.match(html,/marker-holiday\.marker-observance\.marker-event::before/); assert.match(html,/calendar-day, \.calendar-day\.today, \.calendar-day\.selected \{[^}]*border:0/);
console.log('Calendar week/month, squircle outlines and icon controls: OK');

const a=[{id:'late',date:'2026-12-31',time:'14:00'}, {id:'untimed',date:'2026-12-31',time:''}, {id:'early',date:'2026-12-31',time:'08:00'}, {id:'next',date:'2027-01-02',time:'09:00'}, {id:'outside',date:'2027-01-04',time:'08:00'}, {id:'today',date:'2028-05-01',time:'10:00'}];
const selectedYear={year:2027,month:1,day:1}, otherToday={year:2028,month:5,day:1};
assert.deepEqual(calendar.agendaEvents(a,selectedYear,otherToday,false).map(x=>x.id),['early','late','untimed','next']);
assert.deepEqual(calendar.agendaEvents(a,selectedYear,otherToday,true).map(x=>x.id),['today']);
assert.equal(a[0].id,'late','agenda sorting must not mutate storage');
assert.equal(calendar.agendaEvents(a.filter(e=>e.id!=='early'),selectedYear,otherToday,false).length,3);
assert.match(html,/id="calendarBack"[\s\S]*?id="calendarYear"[\s\S]*?id="calendarTheme"/);
assert.match(html,/summary aria-label="Info kalender"/);
assert.match(html,/calendar-weekday \{[^}]*font-weight:900/);
console.log('Calendar agenda range, sorting, year and accessibility: OK');
// Exercise rendered agenda and delete outside the currently selected day.
storage.clear();
const realToday=new Date(), todayParts={year:realToday.getFullYear(),month:realToday.getMonth()+1,day:realToday.getDate()};
const visibleWeek=calendar.weekDays(todayParts).filter(Boolean);
const other=visibleWeek.find(d=>calendar.dateKey(d)!==calendar.dateKey(todayParts));
const savedEvents=[{id:'today-ui',date:calendar.dateKey(todayParts),title:'Hari ini',time:'10:00'}, {id:'other-ui',date:calendar.dateKey(other),title:'Hari lain',time:'09:00'}];
storage.set(key,JSON.stringify(savedEvents)); let agendaUi=createUI(false);
assert.equal(agendaUi.calendarEvents.children.length,2); assert.equal(agendaUi.calendarAgendaHeading.textContent,'ACARA MINGGU TERPILIH');
let otherRow=agendaUi.calendarEvents.children.find(row=>row.children[0].textContent.includes('Hari lain'));
assert(otherRow.children[0].textContent.includes(String(other.day))); otherRow.children[1].fire('click');
assert.equal(JSON.parse(storage.get(key)).length,1); assert.equal(agendaUi.calendarEvents.children.length,1);
agendaUi.calendarExpand.fire('click'); agendaUi.calendarNext.fire('click');
assert.equal(agendaUi.calendarEvents.children.length,1); assert.match(agendaUi.calendarEvents.children[0].children[0].textContent,/Hari ini/);
assert.equal(agendaUi.calendarAgendaHeading.textContent,'ACARA HARI INI');
assert(!agendaUi.calendarMonth.textContent.includes(agendaUi.calendarYear.textContent));
console.log('Calendar rendered agenda, cross-day deletion and actual today outside viewed month: OK');
