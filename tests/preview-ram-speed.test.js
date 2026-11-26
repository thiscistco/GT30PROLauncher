// Run with: node tests/preview-ram-speed.test.js
const fs = require('fs'), vm = require('vm'), assert = require('assert');
const html = fs.readFileSync('preview/index.html', 'utf8');
const source = Array.from(html.matchAll(/<script>([\s\S]*?)<\/script>/g)).at(-1)[1];
const cfg = {width:1224, height:2720, durationMs:6000, radius:100,
  paths:[{offset:0, points:[[100,200],[1100,200]]}]};
const frames = [], positions = [];
const ctx = { clearRect() {}, save() {}, restore() {}, translate() {}, scale() {}, beginPath() {}, arc() {}, fill() {}, drawImage() {},
  createRadialGradient(x,y) { positions.push([x,y]); return {addColorStop(){}}; } };
const canvas = {getContext: () => ctx, getBoundingClientRect: () => ({width:269,height:597})};
function element(value) { return {value, checked:true, textContent:'', listeners:{}, addEventListener(type, fn) { this.listeners[type]=fn; }}; }
const ram = element('50'), ramValue = element(), toggle = element(), status = element();
const elements = {amberGlow:canvas, amberToggle:toggle, amberStatus:status, ramSimulation:ram, ramValue, superPage:{hidden:true}, clockPage:{hidden:true}};
const document = {hidden:false, listeners:{}, getElementById:id => elements[id], addEventListener(type, fn) { this.listeners[type] = fn; }};
class ImageMock { set src(v) { queueMicrotask(() => this.onload()); } }
const window = {requestAnimationFrame:fn => frames.push(fn), matchMedia:() => ({matches:false})};
vm.runInNewContext(source, {document, window, Image:ImageMock, Math, Promise, fetch:async () => ({ok:true,json:async () => cfg})});
setImmediate(() => {
  function frame(t) { frames.shift()(t); return positions.at(-1)[0]; }
  function near(a,b) { assert.ok(Math.abs(a-b)<0.001, `${a} != ${b}`); }
  assert.match(ramValue.textContent, /50% · 6,0/);
  near(frame(0), 100); near(frame(3000), 600);
  ram.value='100'; ram.listeners.input(); assert.match(ramValue.textContent, /100% · 3,0/);
  near(frame(3000), 600); // A speed change alone must never move or reset the light.
  near(frame(4500), 1100);
  ram.value='0'; ram.listeners.input(); assert.match(ramValue.textContent, /0% · 9,0/);
  near(frame(4500), 1100); near(frame(9000), 600);
  toggle.checked=false; frames.shift()(10000);
  toggle.checked=true; near(frame(30000), 600); // Inactive time is not accumulated.
  near(frame(34500), 100);
  const beforeHidden = frame(35000);
  document.hidden=true; document.listeners.visibilitychange();
  document.hidden=false; document.listeners.visibilitychange();
  near(frame(70000), beforeHidden); // Browser may stop RAF entirely while hidden.
  console.log('Preview RAM simulation, speed mapping, continuous phase and pause: OK');
});
