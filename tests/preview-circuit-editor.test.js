// Run with: node tests/preview-circuit-editor.test.js
const fs = require('fs'), vm = require('vm'), assert = require('assert');
const cfg = JSON.parse(fs.readFileSync('preview/assets/circuit-paths.json', 'utf8'));
const source = fs.readFileSync('preview/circuit-editor.js', 'utf8');
const store = new Map(); let applied, downloadBlob, downloaded, revoked = false;
function element() {
  return { listeners: {}, disabled: false, hidden: false, attrs: {}, style: {},
    addEventListener(type, cb) { this.listeners[type] = cb; },
    setAttribute(k, v) { this.attrs[k] = v; },
    click() { if (this.listeners.click && !this.disabled) this.listeners.click({}); }, remove() {} };
}
function context() {
  return { cleared: [], beginPath() {}, moveTo() {}, lineTo() {}, stroke() {}, arc() {}, fill() {},
    setTransform() {}, translate() {}, scale() {}, clearRect(...rect) { this.cleared.push(rect); } };
}
function initialize() {
  const ids = {};
  for (const id of ['pathDraw','pathEdit','pathErase','pathUndo','pathReset','pathExport','pathStatus','pathWidth','pathWidthValue','tabHome','tabSuper']) ids[id] = element();
  const guide = context(); ids.pathDraw.getContext = () => guide;
  ids.pathDraw.getBoundingClientRect = () => ({left: 50, top: 100, width: 300, height: 600});
  ids.pathDraw.setPointerCapture = () => {};
  const doc = { getElementById: id => ids[id], body: { appendChild() {} },
    createElement(type) {
      const e = element();
      if (type === 'canvas') { const c = context(); e.getContext = () => c; e.ctx = c; }
      if (type === 'a') e.click = () => { downloaded = e.download; };
      return e;
    } };
  const win = { devicePixelRatio: 1, confirm: () => true, addEventListener() {} };
  const storage = { getItem: key => store.get(key) || null, setItem: (key, value) => store.set(key, value) };
  class BlobMock { constructor(data, options) { this.data = data; this.options = options; downloadBlob = this; } }
  const URL = {createObjectURL: () => 'blob:test', revokeObjectURL: () => { revoked = true; }};
  vm.runInNewContext(source, {window: win, document: doc, localStorage: storage, Blob: BlobMock, URL, setTimeout: fn => fn(), Math, Number, JSON});
  win.setupCircuitEditor(cfg, (paths, mask) => { applied = {paths, mask}; });
  return ids;
}
function pointer(el, type, x, y, pointerId=1) {
  el.listeners[type]({clientX:x, clientY:y, pointerId, isPrimary:true, button:0, preventDefault() {}});
}
function stroke(ids, a, b) { pointer(ids.pathDraw, 'pointerdown', ...a); pointer(ids.pathDraw, 'pointerup', ...b); }
let ids = initialize();
assert.equal(applied.paths, null); assert.equal(ids.pathEdit.disabled, false);
ids.pathEdit.click(); assert.equal(ids.pathDraw.hidden, false);
stroke(ids, [80,200], [120,250]);
assert.equal(applied.paths.length, 2); assert.equal(applied.paths[0].offset, 0); assert.equal(applied.paths[1].offset, .5);
const scale = Math.max(300 / cfg.width, 600 / cfg.height);
const dx = (300 - cfg.width * scale) / 2, dy = (600 - cfg.height * scale) / 2;
assert.equal(applied.paths[0].points[0][0], Math.round((80 - 50 - dx) / scale));
assert.equal(applied.paths[0].points[0][1], Math.round((200 - 100 - dy) / scale));
assert.equal(applied.mask.ctx.cleared.length, 4, 'mask protects battery and flex cables');
stroke(ids, [122,251], [160,280]);
assert.equal(applied.paths.length, 2, 'nearby endpoints form one route, with two lights');
assert.equal(applied.paths[0].points.length, 3, 'connected endpoint is snapped, without a gap');
stroke(ids, [235,320], [200,300]);
assert.equal(applied.paths.length, 4, 'disconnected stroke is a second route');
stroke(ids, [160,280], [200,300]);
assert.equal(applied.paths.length, 2, 'bridging stroke combines both routes');
for (const path of applied.paths) for (let i=1; i<path.points.length; i++) assert.notDeepEqual(path.points[i], path.points[i-1]);
ids.pathUndo.click(); assert.equal(applied.paths.length, 4, 'undo restores both routes after bridge');
ids.pathWidth.value = '36'; ids.pathWidth.listeners.input();
assert.equal(applied.paths[0].width, 36); assert.equal(applied.mask.ctx.lineWidth, 36); assert.equal(ids.pathWidthValue.textContent, '36 px');
ids.pathExport.click(); assert.equal(downloaded, 'jalur-animasi-gt30.json'); assert(revoked);
const exported = JSON.parse(downloadBlob.data.join('')); assert.equal(exported.paths.length, 4); assert.equal(exported.paths[0].width, 36); assert.equal(exported.width, cfg.width);
ids.pathErase.click(); assert.equal(ids.pathErase.attrs['aria-pressed'], 'true');
pointer(ids.pathDraw,'pointerdown',90,212); pointer(ids.pathDraw,'pointerup',90,212);
assert.equal(applied.paths.length, 2, 'eraser removes the touched entire route only');
ids.pathUndo.click(); assert.equal(applied.paths.length, 4, 'undo restores erased route');
ids.pathEdit.click(); assert.equal(ids.pathDraw.hidden, true);
ids = initialize(); assert.equal(applied.paths.length, 4, 'restore stored drawing after reload'); assert.equal(ids.pathWidth.value, '36');
ids.pathEdit.click(); pointer(ids.pathDraw,'pointerdown',90,210); pointer(ids.pathDraw,'pointermove',120,250); pointer(ids.pathDraw,'pointercancel',120,250);
assert.equal(applied.paths.length, 4, 'cancelled stroke is not stored');
ids.tabSuper.click(); assert.equal(ids.pathDraw.hidden, true, 'editor closes on Super App');
ids.pathReset.click(); assert.equal(applied.paths, null, 'restore defaults');
ids.pathUndo.click(); assert.equal(applied.paths.length, 4, 'reset is undoable');
// Existing v1 drawings were stored as a bare array, and remain usable.
store.set('gt30-circuit-user-paths-v1', JSON.stringify([[[100, 200], [200, 300]]]));
ids = initialize(); assert.equal(applied.paths.length, 2); assert.equal(ids.pathWidth.value, '10');
// All four endpoint orientations snap to the existing route.
for (const [a, b] of [[[99,200],[80,240]], [[80,240],[99,200]], [[151,200],[175,240]], [[175,240],[151,200]]]) {
  store.clear(); ids = initialize(); ids.pathEdit.click();
  stroke(ids, [100,200], [150,200]); stroke(ids, a, b);
  assert.equal(applied.paths.length, 2, 'all endpoint orientations must join');
  assert.equal(applied.paths[0].points.length, 3);
}
// Crossing paths remain separate; a tap must not accidentally erase both.
store.clear(); ids = initialize(); ids.pathEdit.click();
stroke(ids, [80,200], [160,280]); stroke(ids, [160,200], [80,280]);
assert.equal(applied.paths.length, 4); ids.pathErase.click();
pointer(ids.pathDraw, 'pointerdown',120,240); pointer(ids.pathDraw, 'pointermove',120,240); pointer(ids.pathDraw,'pointerup',120,240);
assert.equal(applied.paths.length, 2, 'one tap at a crossing erases only one route');
console.log('Preview connected paths, bridge, whole-route eraser, width, undo, export, storage and protections: OK');
