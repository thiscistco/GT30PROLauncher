// Run with: node tests/preview-clock-countdown.test.js
const fs=require('fs'),vm=require('vm'),assert=require('assert');
const html=fs.readFileSync('preview/index.html','utf8');
const code=html.match(/    function nextPreviewAlarm\(alarm, now\) \{[\s\S]*?(?=    byId\('previewSaveAlarm'\))/);
assert(code);
const context={Date,Math,Number,String,alarms:[]};vm.runInNewContext(code[0],context);
const monday=new Date(2026,10,23,6,30,0).getTime();
assert.equal(context.nextPreviewAlarm({time:'07:00',days:[]},monday),new Date(2026,10,23,7,0).getTime());
assert.equal(context.nextPreviewAlarm({time:'06:00',days:[]},monday),new Date(2026,10,24,6,0).getTime());
assert.equal(context.nextPreviewAlarm({time:'07:00',days:[3]},monday),new Date(2026,10,25,7,0).getTime());
assert.equal(context.previewCountdown(monday+3500,monday),'Dalam 00:00:04');
assert.equal(context.previewCountdown(monday+90061000,monday),'Dalam 1 hari 01:01:01');
assert(html.includes('aria-label="Tambah alarm"'));
assert(html.includes("alarm.enabled=checkbox.checked"));
assert(html.includes("alarms.splice(i, 1); renderAlarms()"));
console.log('Preview alarm countdown: one-off, weekly, day duration and accessible add: OK');
