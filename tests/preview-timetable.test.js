const assert=require('assert'),fs=require('fs'), api=require('../preview/timetable.js');
assert.equal(api.minutes('23:59'),1439);assert.equal(api.minutes('24:00'),-1);assert.equal(api.minutes('7:00'),-1);
const a={start:'09:00',end:'10:00',cells:[{subject:'Matematika',class:'X IPA 1'}]}, b={start:'07:00',end:'08:00',cells:[]}, rows=[a,b];
assert(!api.validRange('07:30','08:30',rows,-1));assert(api.validRange('08:00','09:00',rows,-1));assert(!api.validRange('11:00','10:00',rows,-1));assert(api.validRange('09:15','10:15',rows,0));
assert.deepEqual(api.sorted(rows),[b,a]);assert.strictEqual(api.sorted(rows)[1].cells[0].subject,'Matematika');assert.strictEqual(rows[0],a);
const html=fs.readFileSync('preview/index.html','utf8'), java=fs.readFileSync('app/src/main/java/com/gt30pro/launcher/TimeTableActivity.java','utf8');
for(const d of ['Senin','Selasa','Rabu','Kamis','Jumat']){assert(html.includes('>'+d+'</th>'));assert(java.includes('"'+d+'"'));}
assert.match(java,/extends SwipeBackActivity/);assert.match(java,/ui\.header\(this,font,"Jadwal Mengajar"\)/);assert.match(java,/new TimePickerDialog/);assert.match(java,/validRange\(times\[0\],times\[1\],rows,index\)/);assert.match(java,/confirmDelete\(index\)/);assert.match(java,/getSharedPreferences\("timetable"/);
assert.match(fs.readFileSync('app/src/main/AndroidManifest.xml','utf8'),/android:name="\.TimeTableActivity" android:exported="false"/);
for(const file of ['backup_rules.xml','data_extraction_rules.xml']) assert.match(fs.readFileSync('app/src/main/res/xml/'+file,'utf8'),/path="timetable\.xml"/);
console.log('Time Table preview: days, validation, ordering, local data and native integration: OK');
