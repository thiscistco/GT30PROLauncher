// Run with: node tests/preview-empty-circuit.test.js
const fs = require('fs'), vm = require('vm'), assert = require('assert');
const html = fs.readFileSync('preview/index.html', 'utf8');
const source = Array.from(html.matchAll(/<script>([\s\S]*?)<\/script>/g)).at(-1)[1];
const cfg = JSON.parse(fs.readFileSync('preview/assets/circuit-paths.json', 'utf8'));
// Empty configurations must remain supported when the user clears every path.
cfg.paths = [];
const frames = [], gradients = [];
const ctx = { clearRect() {}, save() {}, restore() {}, translate() {}, scale() {}, beginPath() {}, arc() {}, fill() {}, drawImage() {},
  createRadialGradient(x,y) { gradients.push([x,y]); return { addColorStop() {} }; } };
const canvas = { getContext: () => ctx, getBoundingClientRect: () => ({width:269, height:597}) };
const toggle = { checked:true, addEventListener() {} }, status = {textContent:''};
const ram = {value:'50', addEventListener() {}}, ramValue = {textContent:''};
const document = { hidden:false, getElementById: id => ({amberGlow:canvas, amberToggle:toggle, amberStatus:status, ramSimulation:ram, ramValue, superPage:{hidden:true}, clockPage:{hidden:true}})[id] };
class ImageMock { set src(value) { queueMicrotask(() => this.onload()); } }
let apply;
const window = {requestAnimationFrame: cb => frames.push(cb), matchMedia: () => ({matches:false}),
  setupCircuitEditor: (data, callback) => { apply = callback; callback(null, null); }};
vm.runInNewContext(source, {document, window, Image:ImageMock, Math, Promise, fetch: async () => ({ok:true,json:async () => cfg})});
setImmediate(() => {
  assert.match(status.textContent, /Belum ada jalur/);
  frames.shift()(0); assert.equal(gradients.length, 0, 'empty default must produce no lights');
  apply([{offset:0, points:[[100,200],[200,200]]}], {});
  assert.match(status.textContent, /Aktif/);
  frames.shift()(1200); assert.equal(gradients.length, 1, 'custom path must animate');
  apply(null, null); assert.match(status.textContent, /Belum ada jalur/);
  frames.shift()(2400); assert.equal(gradients.length, 1, 'clearing paths stops lights');
  console.log('Empty defaults, custom animation and clear status: OK');
});
