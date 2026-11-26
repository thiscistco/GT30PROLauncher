// Run with: node tests/preview-circuit-editor.test.js
const fs = require('fs'), vm = require('vm'), assert = require('assert');
const bundledConfig = JSON.parse(fs.readFileSync('preview/assets/circuit-paths.json', 'utf8'));
const cfg = {...bundledConfig, paths: []};
const source = fs.readFileSync('preview/circuit-editor.js', 'utf8');
const store = new Map(); let applied, downloadBlob, downloaded, revoked = false;
function element() {
  return { listeners: {}, disabled: false, hidden: false, attrs: {}, style: {},
    addEventListener(type, cb) { this.listeners[type] = cb; },
    setAttribute(k, v) { this.attrs[k] = v; },
    click() { if (this.listeners.click && !this.disabled) this.listeners.click({}); },
    focus() { this.focused = true; }, select() { this.selected = true; }, remove() {} };
}
function context() {
  return { cleared: [], lines: [], beginPath() {}, moveTo() {}, lineTo(x, y) { this.lines.push([x, y]); }, stroke() {}, arc() {}, fill() {},
    setTransform() {}, translate() {}, scale() {}, clearRect(...rect) { this.cleared.push(rect); } };
}
function initialize(config = cfg) {
  const ids = {};
  for (const id of ['pathDraw','pathEdit','pathErase','pathUndo','pathReset','pathLoadDefault','pathExport','pathStatus','pathWidth','pathWidthValue','pathPalm','pathCodeToggle','pathCodePanel','pathCodeText','pathCodeCopy','pathCodeStatus','tabHome','tabSuper']) ids[id] = element();
  ids.pathCodePanel.hidden = true;
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
  win.setupCircuitEditor(config, (paths, mask) => { applied = {paths, mask}; });
  ids.window = win;
  return ids;
}
function pointer(el, type, x, y, pointerId=1, pointerType='mouse', isPrimary=true) {
  el.listeners[type]({clientX:x, clientY:y, pointerId, pointerType, isPrimary, button:0, preventDefault() {}});
}
function stroke(ids, a, b) { pointer(ids.pathDraw, 'pointerdown', ...a); pointer(ids.pathDraw, 'pointerup', ...b); }
let ids = initialize();
assert.equal(applied.paths, null); assert.equal(ids.pathEdit.disabled, false);
assert.equal(ids.pathCodeToggle.disabled, false); ids.pathCodeToggle.click();
assert.equal(ids.pathCodePanel.hidden, false); assert.equal(ids.pathCodeToggle.attrs['aria-expanded'], 'true');
assert.equal(JSON.parse(ids.pathCodeText.value).paths.length, 0, 'empty configuration can be viewed');
ids.pathEdit.click(); assert.equal(ids.pathDraw.hidden, false);
stroke(ids, [80,200], [120,250]);
assert.equal(applied.paths.length, 3); assert.equal(applied.paths[0].offset, 0); assert.equal(applied.paths[1].offset, .5); assert.equal(applied.paths[2].offset, 1);
const scale = Math.max(300 / cfg.width, 600 / cfg.height);
const dx = (300 - cfg.width * scale) / 2, dy = (600 - cfg.height * scale) / 2;
assert.equal(applied.paths[0].points[0][0], Math.round((80 - 50 - dx) / scale));
assert.equal(applied.paths[0].points[0][1], Math.round((200 - 100 - dy) / scale));
assert.equal(applied.mask.ctx.cleared.length, 4, 'mask protects battery and flex cables');
stroke(ids, [122,251], [160,280]);
assert.equal(applied.paths.length, 3, 'nearby endpoints form one route, with three lights');
assert(applied.paths[0].points.length > 3, 'connected route contains rounded join samples');
for (let i = 1; i < applied.paths[0].points.length; i++)
  assert.notDeepEqual(applied.paths[0].points[i], applied.paths[0].points[i-1], 'rounded join stays continuous without duplicate points');
stroke(ids, [235,320], [200,300]);
assert.equal(applied.paths.length, 6, 'disconnected stroke is a second route');
stroke(ids, [160,280], [200,300]);
assert.equal(applied.paths.length, 3, 'bridging stroke combines both routes');
for (const path of applied.paths) for (let i=1; i<path.points.length; i++) assert.notDeepEqual(path.points[i], path.points[i-1]);
ids.pathUndo.click(); assert.equal(applied.paths.length, 6, 'undo restores both routes after bridge');
ids.pathWidth.value = '36'; ids.pathWidth.listeners.input();
assert.equal(applied.paths[0].width, 36); assert.equal(applied.mask.ctx.lineWidth, 36); assert.equal(ids.pathWidthValue.textContent, '36 px');
ids.pathExport.click(); assert.equal(downloaded, 'jalur-animasi-gt30.json'); assert(revoked);
const exported = JSON.parse(downloadBlob.data.join('')); assert.equal(exported.paths.length, 6); assert.equal(exported.paths[0].width, 36); assert.equal(exported.width, cfg.width);
assert.equal(ids.pathCodeText.value, downloadBlob.data.join(''), 'shown code and downloaded JSON match exactly');
ids.pathCodeCopy.click(); assert.equal(ids.pathCodeText.selected, true); assert.match(ids.pathCodeStatus.textContent, /manual/);
ids.pathErase.click(); assert.equal(ids.pathErase.attrs['aria-pressed'], 'true');
pointer(ids.pathDraw,'pointerdown',110,238); pointer(ids.pathDraw,'pointerup',110,238);
assert.equal(applied.paths.length, 9, 'partial erase splits one route into two and preserves the other');
assert.equal(JSON.parse(ids.pathCodeText.value).paths.length, 9, 'visible code updates after erasing');
ids.pathUndo.click(); assert.equal(applied.paths.length, 6, 'undo restores the uncut route');
assert.equal(JSON.parse(ids.pathCodeText.value).paths.length, 6, 'visible code updates after undo');
ids.pathEdit.click(); assert.equal(ids.pathDraw.hidden, true);
ids = initialize(); assert.equal(applied.paths.length, 6, 'restore stored drawing after reload'); assert.equal(ids.pathWidth.value, '36');
ids.pathEdit.click(); pointer(ids.pathDraw,'pointerdown',90,210); pointer(ids.pathDraw,'pointermove',120,250); pointer(ids.pathDraw,'pointercancel',120,250);
assert.equal(applied.paths.length, 6, 'cancelled stroke is not stored');
ids.tabSuper.click(); assert.equal(ids.pathDraw.hidden, true, 'editor closes on Super App');
ids.pathReset.click(); assert.equal(applied.paths, null, 'restore defaults');
ids.pathUndo.click(); assert.equal(applied.paths.length, 6, 'reset is undoable');
// Existing v1 drawings were stored as a bare array, and remain usable.
store.set('gt30-circuit-user-paths-v1', JSON.stringify([[[100, 200], [200, 300]]]));
ids = initialize(); assert.equal(applied.paths.length, 3); assert.equal(ids.pathWidth.value, '10');
// All four endpoint orientations snap to the existing route.
for (const [a, b] of [[[99,200],[80,240]], [[80,240],[99,200]], [[151,200],[175,240]], [[175,240],[151,200]]]) {
  store.clear(); ids = initialize(); ids.pathEdit.click();
  stroke(ids, [100,200], [150,200]); stroke(ids, a, b);
  assert.equal(applied.paths.length, 3, 'all endpoint orientations must join');
  assert(applied.paths[0].points.length >= 3, 'joined route retains rounded connection');
}
// A single eraser tap cuts only a middle section of a long line.
store.clear(); ids = initialize(); ids.pathEdit.click(); stroke(ids, [80,220], [280,220]); ids.pathErase.click();
pointer(ids.pathDraw, 'pointerdown',180,220); pointer(ids.pathDraw,'pointerup',180,220);
assert.equal(applied.paths.length, 6, 'middle erase creates two remaining paths');
let cutPieces = applied.paths.filter((_, i) => i % 3 === 0).map(p => p.points);
assert.equal(cutPieces[0][0][0], Math.round((80-50-dx)/scale));
assert.equal(cutPieces[1].at(-1)[0], Math.round((280-50-dx)/scale));
assert(cutPieces[0].at(-1)[0] < cutPieces[1][0][0], 'cut must have an actual gap');
ids.pathUndo.click(); assert.equal(applied.paths.length, 3, 'undo rejoins the original path');
// A fast swipe is a continuous eraser, not isolated dots at event positions.
pointer(ids.pathDraw,'pointerdown',130,180); pointer(ids.pathDraw,'pointerup',130,260);
assert.equal(applied.paths.length, 6, 'fast swipe crosses and cuts line');
ids.pathUndo.click(); assert.equal(applied.paths.length, 3);
// Endpoint erasure keeps the other part, never deletes the entire route.
pointer(ids.pathDraw,'pointerdown',80,220); pointer(ids.pathDraw,'pointerup',80,220);
assert.equal(applied.paths.length, 3); assert(applied.paths[0].points[0][0] > Math.round((80-50-dx)/scale));
// Browser prediction contributes to the live guide only, while coalesced actual
// points are smoothed and persisted with both actual endpoints preserved.
store.clear(); ids = initialize(); ids.pathEdit.click();
pointer(ids.pathDraw,'pointerdown',80,200);
ids.pathDraw.listeners.pointermove({clientX:160,clientY:220,pointerId:1,isPrimary:true,button:0,preventDefault(){},
  getCoalescedEvents:()=>[{clientX:100,clientY:220}, {clientX:130,clientY:200}],
  getPredictedEvents:()=>[{clientX:250,clientY:300}]});
assert(ids.pathDraw.getContext().lines.some(p => p[0] === Math.round((250-50-dx)/scale)), 'prediction appears in live guide');
pointer(ids.pathDraw,'pointerup',180,230);
let smooth = applied.paths[0].points;
assert(smooth.length > 4, 'corner cutting supplies rounded intermediate points');
assert.equal(smooth[0][0], Math.round((80-50-dx)/scale));
assert.equal(smooth.at(-1)[0], Math.round((180-50-dx)/scale));
assert(smooth.every(p => p[0] < Math.round((250-50-dx)/scale)), 'predicted point must not be saved');
for (let i=1; i<smooth.length; i++) assert.notDeepEqual(smooth[i],smooth[i-1]);
// An eraser passing over a whole stroke removes it; undo restores it.
store.clear(); ids = initialize(); ids.pathEdit.click(); stroke(ids,[80,220],[100,220]); ids.pathErase.click();
pointer(ids.pathDraw,'pointerdown',80,220); pointer(ids.pathDraw,'pointerup',100,220);
assert.equal(applied.paths, null); ids.pathUndo.click(); assert.equal(applied.paths.length, 3);
// Where two paths cross, only the small touched parts of both are erased.
store.clear(); ids = initialize(); ids.pathEdit.click();
stroke(ids,[80,200],[160,280]); stroke(ids,[160,200],[80,280]); ids.pathErase.click();
pointer(ids.pathDraw,'pointerdown',120,240); pointer(ids.pathDraw,'pointerup',120,240);
assert.equal(applied.paths.length, 12, 'crossing yields four preserved line portions');
ids.pathUndo.click(); assert.equal(applied.paths.length, 6);
// Palm rejection is opt-in: ignore touch/mouse before and during a pen stroke.
store.clear(); ids = initialize(); assert.equal(ids.pathPalm.checked, false); ids.pathEdit.click();
ids.pathPalm.checked = true; ids.pathPalm.listeners.change();
assert.match(ids.pathStatus.textContent, /khusus stylus/);
pointer(ids.pathDraw,'pointerdown',80,200,11,'touch');
pointer(ids.pathDraw,'pointermove',100,220,11,'touch');
pointer(ids.pathDraw,'pointerdown',110,210,22,'pen');
pointer(ids.pathDraw,'pointercancel',100,220,11,'touch');
pointer(ids.pathDraw,'pointermove',140,230,22,'pen');
pointer(ids.pathDraw,'pointerup',150,240,22,'pen');
assert.equal(applied.paths.length, 3, 'stylus works while palm touches');
const kept = JSON.stringify(applied.paths);
pointer(ids.pathDraw,'pointerdown',200,300,33,'mouse'); pointer(ids.pathDraw,'pointerup',250,300,33,'mouse');
assert.equal(JSON.stringify(applied.paths), kept, 'mouse is ignored in stylus-only mode');
ids.pathErase.click();
pointer(ids.pathDraw,'pointerdown',130,223,44,'touch'); pointer(ids.pathDraw,'pointerup',130,223,44,'touch');
assert.equal(JSON.stringify(applied.paths), kept, 'palm cannot erase');
pointer(ids.pathDraw,'pointerdown',130,223,55,'pen'); pointer(ids.pathDraw,'pointerup',130,223,55,'pen');
assert.notEqual(JSON.stringify(applied.paths), kept, 'stylus can erase');
ids.pathUndo.click(); assert.equal(JSON.stringify(applied.paths), kept);
ids = initialize(); assert.equal(ids.pathPalm.checked, true, 'palm preference survives reload');
ids.pathPalm.checked = false; ids.pathPalm.listeners.change(); ids.pathEdit.click();
pointer(ids.pathDraw,'pointerdown',200,300,66,'touch'); pointer(ids.pathDraw,'pointerup',250,300,66,'touch');
assert.equal(applied.paths.length, 6, 'touch works after disabling palm rejection');
// Switching modes mid-touch does not commit an accidental line.
store.clear(); ids = initialize(); ids.pathEdit.click();
pointer(ids.pathDraw,'pointerdown',80,200,77,'touch'); pointer(ids.pathDraw,'pointermove',110,230,77,'touch');
ids.pathPalm.checked = true; ids.pathPalm.listeners.change();
pointer(ids.pathDraw,'pointerup',150,240,77,'touch'); assert.equal(applied.paths, null);
// The supplied paths appear as seven editable routes and use the shared mask.
store.clear(); ids = initialize(bundledConfig);
assert.equal(applied.paths, null, 'unedited supplied paths use shared APK mask');
assert.match(ids.pathStatus.textContent, /7 jalur awal/); assert.equal(ids.pathWidth.value, '8');
ids.pathCodeToggle.click(); assert.deepEqual(JSON.parse(ids.pathCodeText.value), bundledConfig);
ids.pathReset.click(); assert.equal(applied.paths.length, 0, 'clear removes even the bundled paths');
assert.match(ids.pathStatus.textContent, /Belum ada/); assert.equal(JSON.parse(ids.pathCodeText.value).paths.length, 0);
ids.pathLoadDefault.click(); assert.equal(applied.paths, null); assert.match(ids.pathStatus.textContent, /7 jalur awal/);
ids.pathUndo.click(); assert.equal(applied.paths.length, 0, 'loading defaults can be undone');
ids = initialize(bundledConfig); assert.equal(applied.paths.length, 0, 'saved empty paths remain empty after reload');
ids.pathLoadDefault.click(); assert.equal(applied.paths, null);
ids.pathWidth.value = '20'; ids.pathWidth.listeners.input(); assert.equal(applied.paths.length, 21);
ids.pathLoadDefault.click(); assert.equal(ids.pathWidth.value, '8'); assert.equal(applied.paths, null);
ids.pathUndo.click(); assert.equal(ids.pathWidth.value, '20'); assert.equal(applied.paths.length, 21);
// Clipboard success and permission denial both leave a usable code panel.
(async () => {
  store.clear(); const ui = initialize(); ui.pathCodeToggle.click();
  let copied = null;
  ui.window.navigator = {clipboard:{writeText: text => { copied = text; return Promise.resolve(); }}};
  ui.pathCodeCopy.click(); await Promise.resolve();
  assert.equal(copied, ui.pathCodeText.value); assert.match(ui.pathCodeStatus.textContent, /disalin/);
  ui.window.navigator.clipboard.writeText = () => Promise.reject(new Error('denied'));
  ui.pathCodeCopy.click(); await Promise.resolve();
  assert.equal(ui.pathCodeText.selected, true); assert.match(ui.pathCodeStatus.textContent, /manual/);
  ui.pathCodeToggle.click(); assert.equal(ui.pathCodePanel.hidden, true); assert.equal(ui.pathCodeToggle.attrs['aria-expanded'], 'false');
  console.log('Preview code, clipboard fallback, palm rejection, erasing, smoothing, joins and storage: OK');
})().catch(error => { console.error(error); process.exitCode = 1; });
