// Offline-first holiday updates: no private event data is read or sent.
const assert = require('assert');
const sync = require('../preview/holiday-update.js');
const original = require('../preview/holiday-data.js');
const clone = o => JSON.parse(JSON.stringify(o));
const envelope = data => JSON.stringify({encoding:'base64', size:Buffer.byteLength(JSON.stringify(data)), content:Buffer.from(JSON.stringify(data)).toString('base64')});
const response = text => ({ok:true,status:200,text:async()=>text});
class Storage {
  constructor() { this.map = new Map(); }
  getItem(k) { return this.map.has(k) ? this.map.get(k) : null; }
  setItem(k,v) { this.map.set(k,v); }
}
(async function () {
  assert(sync.valid(original));
  const bads = [clone(original),clone(original),clone(original),clone(original),clone(original)];
  bads[0].entries['2026-02-30']=[{type:'libur',name:'Invalid date'}];
  bads[1].entries['2026-08-17'][0].type='tidak dikenal';
  bads[2].revision=0; bads[3].years.push(2026); bads[4].entries['2026-08-17'][0].name='';
  for (const bad of bads) assert(!sync.valid(bad));
  let now=1700000000000, calls=0, payload=original, fail=false, received;
  let storage=new Storage(); storage.setItem('gt30-calendar-events-v1', 'PRIVATE EVENT');
  let make=(store=storage)=>sync.create({initial:original,storage:store,now:()=>now,
    atob:s=>Buffer.from(s,'base64').toString('binary'),
    fetch:async (url,options)=>{calls++;received={url,options};if(fail)throw Error('offline');return response(envelope(payload));}});
  let updater=make(); await updater.refresh(false);
  assert.equal(calls,1); assert.equal(received.url,sync.URL); assert.equal(received.options.credentials,'omit');
  assert(!JSON.stringify(received).includes('PRIVATE EVENT')); assert.equal(storage.getItem('gt30-calendar-events-v1'),'PRIVATE EVENT');
  assert.equal(updater.getState().checkedAt,now); assert.equal(updater.getState().data.revision,1);
  await updater.refresh(false); assert.equal(calls,1,'within 12 hours no automatic request');
  let revised=clone(original); revised.revision=2; revised.entries['2026-08-17'][0].name='Kemerdekaan diperbarui'; payload=revised;
  await updater.refresh(true); assert.equal(calls,2); assert.equal(updater.getState().data.revision,2);
  assert.equal(JSON.parse(storage.getItem(sync.CACHE_KEY)).data.entries['2026-08-17'][0].name,'Kemerdekaan diperbarui');
  let reloaded=make(); assert.equal(reloaded.getState().data.revision,2,'cached data survives reload');
  let cached=storage.getItem(sync.CACHE_KEY); payload=original; await reloaded.refresh(true);
  assert.equal(reloaded.getState().data.revision,2); assert.equal(storage.getItem(sync.CACHE_KEY),cached,'reject downgrade');
  payload=clone(revised); payload.revision=3; payload.years=[2026]; for(const key of Object.keys(payload.entries)) if(key.startsWith('2027')) delete payload.entries[key];
  await reloaded.refresh(true); assert.equal(storage.getItem(sync.CACHE_KEY),cached,'reject loss of a covered year');
  payload=clone(revised); payload.entries['2026-08-17'][0].name='changed without revision';
  await reloaded.refresh(true); assert.equal(storage.getItem(sync.CACHE_KEY),cached,'same revision cannot silently change');
  fail=true; await reloaded.refresh(true); assert.match(reloaded.getState().message,/gagal/); assert.equal(storage.getItem(sync.CACHE_KEY),cached);
  const throwing={getItem(){throw Error('blocked')},setItem(){throw Error('blocked')}};
  fail=false; payload=clone(revised);payload.revision=3; const temporary=make(throwing); await temporary.refresh(true);
  assert.equal(temporary.getState().data.revision,3); assert.match(temporary.getState().message,/sementara/);
  let corrupt=new Storage(); corrupt.setItem(sync.CACHE_KEY,'invalid json'); let fallback=make(corrupt); assert.equal(fallback.getState().data.revision,1);
  now += sync.INTERVAL+1; fail=false; payload=revised; await updater.refresh(false); assert.equal(updater.getState().checkedAt,now);
  assert.equal(storage.getItem('gt30-calendar-events-v1'),'PRIVATE EVENT');
  console.log('Holiday updates: validation, offline fallback, throttling, revision, privacy and cache: OK');
})().catch(e=>{console.error(e);process.exitCode=1});
