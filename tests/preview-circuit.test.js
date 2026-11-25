// Run with: node tests/preview-circuit.test.js
const fs = require('fs');
const vm = require('vm');
const assert = require('assert');
const html = fs.readFileSync('preview/index.html', 'utf8');
const scripts = Array.from(html.matchAll(/<script>([\s\S]*?)<\/script>/g));
const source = scripts[scripts.length - 1][1];
const config = JSON.parse(fs.readFileSync('preview/assets/circuit-paths.json', 'utf8'));
const frames = [], gradients = [];
const ctx = {
  clearRect() {}, save() {}, restore() {}, translate() {}, scale() {},
  beginPath() {}, arc() {}, fill() {}, drawImage() {},
  createRadialGradient(x, y) { gradients.push([x, y]); return { addColorStop() {} }; }
};
const canvas = { width: 0, height: 0, getContext: () => ctx, getBoundingClientRect: () => ({ width: 269, height: 597 }) };
const toggle = { checked: true, listeners: {}, addEventListener(type, fn) { this.listeners[type] = fn; } };
const status = { textContent: '' };
const reduced = { matches: false, listeners: {}, addEventListener(type, fn) { this.listeners[type] = fn; } };
const elements = { amberGlow: canvas, amberToggle: toggle, amberStatus: status, superPage: {hidden:true}, clockPage: {hidden:true} };
const document = { hidden: false, getElementById: id => elements[id] };
class ImageMock { set src(value) { queueMicrotask(() => this.onload()); } }
const window = { requestAnimationFrame: fn => frames.push(fn), devicePixelRatio: 1, matchMedia: () => reduced };
vm.runInNewContext(source, { document, window, fetch: async () => ({ ok:true, json: async () => config }), Image: ImageMock, Math, Promise });
setImmediate(() => {
  assert.match(status.textContent, /Aktif/);
  frames.shift()(0);
  assert.equal(gradients.length, config.paths.length);
  const first = gradients.slice();
  frames.shift()(1200);
  assert.equal(gradients.length, config.paths.length * 2);
  assert.notDeepEqual(first, gradients.slice(config.paths.length), 'light should move along paths');
  toggle.checked = false; toggle.listeners.change();
  assert.match(status.textContent, /dimatikan/);
  frames.shift()(1300);
  assert.equal(gradients.length, config.paths.length * 2, 'disabled animation must not draw');
  toggle.checked = true; reduced.matches = true; reduced.listeners.change();
  assert.match(status.textContent, /kurangi gerakan/);
  frames.shift()(1400);
  assert.equal(gradients.length, config.paths.length * 2, 'reduced motion must not draw');
  console.log('Preview circuit animation, toggle and reduced-motion status: OK');
});
