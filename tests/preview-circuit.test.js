// Run with: node tests/preview-circuit.test.js
const fs = require('fs');
const vm = require('vm');
const assert = require('assert');
const html = fs.readFileSync('preview/index.html', 'utf8');
const scripts = Array.from(html.matchAll(/<script>([\s\S]*?)<\/script>/g));
const source = scripts[scripts.length - 1][1];
const config = JSON.parse(fs.readFileSync('preview/assets/circuit-paths.json', 'utf8'));
// Exercise motion using short synthetic paths, independently of the user's drawing.
config.paths = [
  {offset: 0, points: [[100,200], [200,200], [200,400]]},
  {offset: 0.5, points: [[100,200], [200,200], [200,400]]},
  {offset: 0.2, points: [[300,300], [450,500]]}
];
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
  function positionsAt(time) {
    const n = gradients.length;
    frames.shift()(time);
    return gradients.slice(n);
  }
  function near(actual, expected, message) {
    assert.ok(Math.hypot(actual[0] - expected[0], actual[1] - expected[1]) < 0.0001, message);
  }
  config.paths.forEach((path, index) => {
    const end = (1 - path.offset) * config.durationMs;
    const start = (2 - path.offset) * config.durationMs;
    near(positionsAt(end)[index], path.points.at(-1), 'light reaches end of path');
    near(positionsAt(end - 10)[index], positionsAt(end + 10)[index], 'light reverses at end');
    near(positionsAt(start)[index], path.points[0], 'light returns to start of path');
    near(positionsAt(start - 10)[index], positionsAt(start + 10)[index], 'light reverses at start');
  });
  const count = gradients.length;
  toggle.checked = false; toggle.listeners.change();
  assert.match(status.textContent, /dimatikan/);
  frames.shift()(13000);
  assert.equal(gradients.length, count, 'disabled animation must not draw');
  toggle.checked = true; reduced.matches = true; reduced.listeners.change();
  assert.match(status.textContent, /kurangi gerakan/);
  frames.shift()(14000);
  assert.equal(gradients.length, count, 'reduced motion must not draw');
  console.log('Preview circuit reverses at both endpoints; toggle and reduced-motion status: OK');
});
