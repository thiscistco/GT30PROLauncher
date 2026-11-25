// Run with: node tests/preview-circuit-editor.test.js
const fs = require('fs'), vm = require('vm'), assert = require('assert');
const cfg = JSON.parse(fs.readFileSync('preview/assets/circuit-paths.json', 'utf8'));
const source = fs.readFileSync('preview/circuit-editor.js', 'utf8');
const store = new Map(); let applied, downloadBlob, downloaded, revoked = false;
function element() {
  return { listeners: {}, disabled: false, hidden: false, attrs: {},
    addEventListener(type, cb) { this.listeners[type] = cb; },
    setAttribute(k, v) { this.attrs[k] = v; },
    click() { if (this.listeners.click && !this.disabled) this.listeners.click({}); },
    remove() {} };
}
function context() {
  return { cleared: [], beginPath() {}, moveTo() {}, lineTo() {}, stroke() {},
    setTransform() {}, translate() {}, scale() {}, clearRect(...rect) { this.cleared.push(rect); } };
}
function initialize() {
  const ids = {};
  for (const id of ['pathDraw','pathEdit','pathUndo','pathReset','pathExport','pathStatus','tabHome','tabSuper']) ids[id] = element();
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
let ids = initialize();
assert.equal(applied.paths, null); assert.equal(ids.pathEdit.disabled, false);
ids.pathEdit.click(); assert.equal(ids.pathDraw.hidden, false);
pointer(ids.pathDraw, 'pointerdown', 80, 200);
pointer(ids.pathDraw, 'pointermove', 100, 220);
pointer(ids.pathDraw, 'pointerup', 120, 250);
assert.equal(applied.paths.length, 2); assert.equal(applied.paths[0].offset, 0); assert.equal(applied.paths[1].offset, .5);
// object-fit: cover, rather than stretching the image to the screen.
const scale = Math.max(300 / cfg.width, 600 / cfg.height);
const dx = (300 - cfg.width * scale) / 2, dy = (600 - cfg.height * scale) / 2;
assert.equal(applied.paths[0].points[0][0], Math.round((80 - 50 - dx) / scale));
assert.equal(applied.paths[0].points[0][1], Math.round((200 - 100 - dy) / scale));
assert.equal(applied.mask.ctx.cleared.length, 4, 'mask protects battery and flex cables');
ids.pathExport.click(); assert.equal(downloaded, 'jalur-animasi-gt30.json'); assert(revoked);
const exported = JSON.parse(downloadBlob.data.join('')); assert.equal(exported.paths.length, 2); assert.equal(exported.width, cfg.width);
ids.pathEdit.click(); assert.equal(ids.pathDraw.hidden, true);
ids = initialize(); assert.equal(applied.paths.length, 2, 'restore stored drawing after reload');
ids.pathEdit.click(); pointer(ids.pathDraw,'pointerdown',90,210); pointer(ids.pathDraw,'pointermove',120,250); pointer(ids.pathDraw,'pointercancel',120,250);
assert.equal(applied.paths.length, 2, 'cancelled stroke is not stored');
ids.tabSuper.click(); assert.equal(ids.pathDraw.hidden, true, 'editor closes on Super App');
ids.pathUndo.click(); assert.equal(applied.paths, null); assert(ids.pathExport.disabled);
ids.pathEdit.click(); pointer(ids.pathDraw,'pointerdown',80,200); pointer(ids.pathDraw,'pointerup',120,250);
ids.pathReset.click(); assert.equal(applied.paths, null, 'restore defaults');
console.log('Preview drawing, center-crop coordinates, storage, protected mask, export, undo and reset: OK');
