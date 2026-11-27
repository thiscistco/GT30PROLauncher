// Run with: node tests/preview-swipe.test.js
const fs = require('fs');
const vm = require('vm');
const assert = require('assert');
const html = fs.readFileSync('preview/index.html', 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
class Element {
  constructor() {
    this.hidden = false; this.checked = true; this.listeners = {}; this.attrs = {};
    this.classList = { toggle() {} };
  }
  addEventListener(type, fn) { (this.listeners[type] ||= []).push(fn); }
  fire(type, event = {}) { for (const fn of this.listeners[type] || []) fn.call(this, event); }
  setAttribute(key, value) { this.attrs[key] = value; }
  focus() {}
  getBoundingClientRect() { return { left: 0, top: 0, bottom: 1000, right: 445, width: 445, height: 1000 }; }
}
const ids = ['chargeToggle','batteryLed','previewBatteryStatus','previewBatterySource','amberGlow','amberToggle','clockHour','clockMin','clockLed','clockPage','clockBack',
  'settingsGlow','settingsThemeSystem','settingsThemeLight','settingsThemeDark','superThemeSystem','superThemeLight','superThemeDark','systemPanelPreview','systemPanelBack','systemPanelTitle','systemPanelNote','superPage','superGrid','previewSettings','openSettingsModule','tabHome','tabSuper','superBack','openClockModule','clockPageTime','clockPageDate',
  'notesPage','notesBack','openNotesModule','calendarPage','calendarBack','openCalendarModule','homeDate','weatherPage','weatherBack','superWeather','homeWeather','timetablePage','timetableBack','openTimetableModule','drawerPage','drawerBack','pathDraw'];
const elements = Object.fromEntries(ids.map(id => [id, new Element()]));
elements.clockPage.hidden = true; elements.calendarPage.hidden = true; elements.weatherPage.hidden = true; elements.superPage.hidden = true;
elements.notesPage.hidden = true; elements.systemPanelPreview.hidden = true; elements.previewSettings.hidden = true; elements.timetablePage.hidden = true; elements.drawerPage.hidden = true; elements.pathDraw.hidden = true;
const screen = new Element(), separator = new Element(), doc = new Element();
doc.getElementById = id => elements[id];
doc.querySelector = selector => selector === '.screen' ? screen : separator;
vm.runInNewContext(script, { document: doc, window: {}, Date, Intl, String, setTimeout() {} });
function swipe(x1, y1, x2, y2) {
  screen.fire('pointerdown', { isPrimary:true, button:0, pointerId:1, clientX:x1, clientY:y1 });
  doc.fire('pointerup', { pointerId:1, clientX:x2, clientY:y2 });
}
elements.tabSuper.fire('click');
assert.strictEqual(elements.superPage.hidden, false);
elements.openSettingsModule.fire('click');
assert.strictEqual(elements.previewSettings.hidden,false); assert.strictEqual(elements.superGrid.hidden,true);
swipe(440, 100, 360, 100); assert.strictEqual(elements.previewSettings.hidden,true); assert.strictEqual(elements.superGrid.hidden,false);
assert.strictEqual(elements.superPage.hidden,false);
swipe(440, 100, 360, 100);
assert.strictEqual(elements.superPage.hidden, true, 'right edge goes back to home');
elements.tabSuper.fire('click');
elements.openClockModule.fire('click');
swipe(440, 100, 360, 100);
assert.strictEqual(elements.clockPage.hidden, true, 'clock closes');
assert.strictEqual(elements.superPage.hidden, false, 'returns to Super App');
elements.openClockModule.fire('click');
swipe(350, 100, 250, 100);
assert.strictEqual(elements.clockPage.hidden, false, 'must start at right edge');
swipe(440, 100, 420, 100);
assert.strictEqual(elements.clockPage.hidden, false, 'short swipe ignored');
swipe(440, 100, 360, 220);
assert.strictEqual(elements.clockPage.hidden, false, 'vertical swipe ignored');
screen.fire('pointerdown', {isPrimary:true,button:0,pointerId:2,clientX:440,clientY:100});
doc.fire('pointercancel', {});
doc.fire('pointerup', {pointerId:2,clientX:350,clientY:100});
assert.strictEqual(elements.clockPage.hidden, false, 'cancelled swipe ignored');
elements.clockBack.fire('click');
elements.openCalendarModule.fire('click');
assert.strictEqual(elements.calendarPage.hidden, false, 'calendar opens');
swipe(440, 100, 360, 100);
assert.strictEqual(elements.calendarPage.hidden, true, 'calendar closes on right-edge swipe');
assert.strictEqual(elements.superPage.hidden, false, 'calendar returns to Super App');
elements.superWeather.fire('click'); assert.strictEqual(elements.weatherPage.hidden,false,'weather opens');
swipe(440, 100, 360, 100); assert.strictEqual(elements.weatherPage.hidden,true,'weather back gesture');
elements.superWeather.fire('click'); swipe(200,950,200,500); assert.strictEqual(elements.weatherPage.hidden,true,'weather bottom swipe returns home');
elements.tabSuper.fire('click'); elements.openCalendarModule.fire('click'); elements.calendarBack.fire('click');
assert.strictEqual(elements.calendarPage.hidden, true, 'calendar back button closes page');
// The home date opens the calendar directly and returns focus to the date.
elements.tabHome.fire('click'); elements.homeDate.fire('click');
assert.strictEqual(elements.calendarPage.hidden, false, 'home date opens calendar');
assert.strictEqual(elements.superPage.hidden, true, 'home date does not require showing Super App');
elements.calendarBack.fire('click'); assert.strictEqual(elements.calendarPage.hidden, true);
elements.homeDate.fire('keydown', {key:'Enter',preventDefault(){}}); assert.strictEqual(elements.calendarPage.hidden,false);
swipe(440,100,360,100); assert.strictEqual(elements.calendarPage.hidden,true,'calendar swipe back to home');
elements.tabSuper.fire('click'); elements.openTimetableModule.fire('click'); assert.strictEqual(elements.timetablePage.hidden,false); elements.timetableBack.fire('click'); assert.strictEqual(elements.timetablePage.hidden,true); assert.strictEqual(elements.superPage.hidden,false);
elements.openNotesModule.fire('click'); assert.strictEqual(elements.notesPage.hidden,false); elements.notesBack.fire('click'); assert.strictEqual(elements.notesPage.hidden,true); assert.strictEqual(elements.superPage.hidden,false);
// Home weather opens the same page without Super App and returns to its origin.
elements.tabHome.fire('click'); let focused=false;
elements.homeWeather.focus=()=>{ focused=true; };
elements.homeWeather.fire('click');
assert.strictEqual(elements.weatherPage.hidden,false,'home weather opens forecast');
assert.strictEqual(elements.superPage.hidden,true,'home weather does not open Super App');
elements.weatherBack.fire('click'); assert.strictEqual(elements.weatherPage.hidden,true); assert(focused,'weather back restores home focus');
elements.homeWeather.fire('click'); swipe(440,100,360,100); assert.strictEqual(elements.weatherPage.hidden,true,'weather swipe back to home');
elements.homeWeather.fire('click'); swipe(200,950,200,500); assert.strictEqual(elements.weatherPage.hidden,true,'weather bottom swipe returns home');
// Any upward swipe on home opens Super App, without requiring fling speed.
elements.tabHome.fire('click');
for (const [x,y] of [[200,950],[200,750],[20,200],[430,500]]) {
  swipe(x,y,x,y-80);
  assert.strictEqual(elements.superPage.hidden,false,'home swipe opens Super App');
  assert.strictEqual(elements.drawerPage.hidden,true,'does not open drawer');
  elements.tabHome.fire('click');
}
swipe(200,500,200,400); assert.strictEqual(elements.superPage.hidden,false);
swipe(200,500,200,400); assert.strictEqual(elements.drawerPage.hidden,false,'two consecutive swipes open drawer');
elements.drawerBack.fire('click'); assert.strictEqual(elements.drawerPage.hidden,true);
swipe(200,500,200,520); assert.strictEqual(elements.systemPanelPreview.hidden,true,'short down ignored');
swipe(100,500,200,560); assert.strictEqual(elements.systemPanelPreview.hidden,true,'diagonal ignored');
swipe(100,300,100,400); assert.strictEqual(elements.systemPanelPreview.hidden,false);
assert.match(elements.systemPanelTitle.textContent,/notifikasi/); elements.systemPanelBack.fire('click');
assert.strictEqual(elements.systemPanelPreview.hidden,true);
swipe(350,300,350,400); assert.strictEqual(elements.systemPanelPreview.hidden,false);
assert.match(elements.systemPanelTitle.textContent,/kontrol/); elements.systemPanelBack.fire('click');
elements.tabSuper.fire('click'); swipe(200, 950, 210, 500);
assert.strictEqual(elements.superPage.hidden, true, 'Super App gesture returns to home');
assert.strictEqual(elements.drawerPage.hidden, true, 'returning home does not open drawer');
elements.tabSuper.fire('click'); elements.openClockModule.fire('click'); swipe(200, 950, 210, 500);
assert.strictEqual(elements.clockPage.hidden, true, 'gesture from clock returns directly home');
assert.strictEqual(elements.superPage.hidden, true);
elements.tabSuper.fire('click'); elements.openCalendarModule.fire('click'); swipe(440, 950, 420, 500);
assert.strictEqual(elements.calendarPage.hidden, true, 'bottom gesture works from right corner');
assert.strictEqual(elements.superPage.hidden, true);
elements.pathDraw.hidden = false; swipe(200,950,200,500); assert.strictEqual(elements.superPage.hidden,true, 'editor gestures remain untouched'); elements.pathDraw.hidden = true;
screen.fire('pointerdown', {isPrimary:true,button:0,pointerId:9,clientX:200,clientY:950});
screen.fire('pointerdown', {isPrimary:false,button:0,pointerId:10,clientX:210,clientY:950});
doc.fire('pointerup',{pointerId:9,clientX:200,clientY:500}); assert.strictEqual(elements.drawerPage.hidden,true,'multi-touch is cancelled');
screen.fire('pointerdown', {isPrimary:true,button:0,pointerId:11,clientX:200,clientY:950});
let prevented=false; screen.fire('touchmove',{touches:[{clientX:202,clientY:900}],cancelable:true,preventDefault(){prevented=true;}});
assert(prevented,'bottom touch drag prevents page scrolling');
doc.fire('pointercancel',{}); doc.fire('pointerup',{pointerId:11,clientX:200,clientY:500}); assert.strictEqual(elements.drawerPage.hidden,true,'cancelled bottom gesture ignored');
console.log('Preview back, bottom gestures, editor and touch cancellation tests passed');
