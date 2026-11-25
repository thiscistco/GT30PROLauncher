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
  getBoundingClientRect() { return { right: 445, width: 445 }; }
}
const ids = ['chargeToggle','amberGlow','amberToggle','clockHour','clockMin','clockLed','clockPage','clockBack',
  'superPage','tabHome','tabSuper','superBack','openClockModule','clockPageTime','clockPageDate'];
const elements = Object.fromEntries(ids.map(id => [id, new Element()]));
elements.clockPage.hidden = true; elements.superPage.hidden = true;
const screen = new Element(), separator = new Element(), doc = new Element();
doc.getElementById = id => elements[id];
doc.querySelector = selector => selector === '.screen' ? screen : separator;
vm.runInNewContext(script, { document: doc, Date, Intl, String, setTimeout() {} });
function swipe(x1, y1, x2, y2) {
  screen.fire('pointerdown', { isPrimary:true, button:0, pointerId:1, clientX:x1, clientY:y1 });
  doc.fire('pointerup', { pointerId:1, clientX:x2, clientY:y2 });
}
elements.tabSuper.fire('click');
assert.strictEqual(elements.superPage.hidden, false);
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
console.log('Preview swipe back tests passed');
