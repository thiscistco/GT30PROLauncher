const assert=require('assert');
const mount=require('../preview/notes.js');
class Element {
  constructor(){ this.listeners={}; this.hidden=false; this.disabled=false; this.value=''; this.children=[]; this.attrs={}; this._text=''; }
  addEventListener(name,fn){ (this.listeners[name] ||= []).push(fn); }
  fire(name){ for(const fn of this.listeners[name]||[]) fn.call(this,{preventDefault(){}}); }
  appendChild(node){this.children.push(node);}
  set textContent(v){this._text=String(v);this.children=[];}
  get textContent(){return this._text+this.children.map(n=>n.textContent).join('');}
  setAttribute(k,v){this.attrs[k]=v;}
  focus(){this.focused=true;}
}
function setup(storage) {
  const ids={}; for(const id of ['notesForm','notesTitle','notesBody','notesList','notesListPanel','notesStatus','notesEmpty','notesAdd','notesDelete','notesEditorHeading','notesCancel']) ids[id]=new Element();
  ids.notesForm.hidden=true; let confirm=true;
  const win={document:{getElementById:id=>ids[id],createElement:()=>new Element()},localStorage:storage,confirm:()=>confirm};
  mount(win); return {ids,win,setConfirm(value){confirm=value;}};
}
const saved=new Map(); let failWrite=false;
const storage={getItem:k=>saved.has(k)?saved.get(k):null,setItem:(k,v)=>{if(failWrite)throw Error('quota');saved.set(k,v);}};
let app=setup(storage), e=app.ids;
assert(!e.notesEmpty.hidden); e.notesAdd.fire('click'); assert(!e.notesForm.hidden);
e.notesForm.fire('submit'); assert.match(e.notesStatus.textContent,/Isi judul atau catatan/); assert.equal(saved.size,0);
e.notesBody.value='<script>not executable</script>'; e.notesForm.fire('submit');
assert(e.notesForm.hidden); assert.equal(e.notesList.children.length,1);
assert.equal(e.notesList.children[0].children[0].textContent,'Tanpa judul');
assert.equal(e.notesList.children[0].children[1].textContent,'<script>not executable</script>');
let record=JSON.parse(saved.get('gt30-notes-v1'))[0]; assert.equal(record.body,'<script>not executable</script>');
// A reload reads the same notes; edit preserves the stable id.
app=setup(storage); e=app.ids; e.notesList.children[0].fire('click');
e.notesTitle.value='Catatan penting'; e.notesBody.value='Isi baru'; e.notesForm.fire('submit');
assert.equal(JSON.parse(saved.get('gt30-notes-v1'))[0].id,record.id);
assert.equal(e.notesList.children[0].children[0].textContent,'Catatan penting');
e.notesList.children[0].fire('click'); e.notesBody.value='Perubahan belum disimpan';
app.setConfirm(false); assert.equal(app.win.notesGoBack(),true); assert(!e.notesForm.hidden); assert(!app.win.notesConfirmLeave());
app.setConfirm(true); assert(app.win.notesConfirmLeave()); assert(e.notesForm.hidden);
assert.equal(JSON.parse(saved.get('gt30-notes-v1'))[0].body,'Isi baru');
// A quota failure never pretends to save or closes the draft.
e.notesList.children[0].fire('click'); e.notesBody.value='Belum tersimpan'; failWrite=true; e.notesForm.fire('submit');
assert(!e.notesForm.hidden); assert.match(e.notesStatus.textContent,/Tidak dapat menyimpan/); assert.equal(JSON.parse(saved.get('gt30-notes-v1'))[0].body,'Isi baru'); failWrite=false;
e.notesDelete.fire('click'); assert.equal(JSON.parse(saved.get('gt30-notes-v1')).length,0); assert(!e.notesEmpty.hidden);
// Invalid saved data is not overwritten or treated as an empty writable database.
saved.set('gt30-notes-v1','{broken'); app=setup(storage); assert(app.ids.notesAdd.disabled); assert.match(app.ids.notesStatus.textContent,/tidak ditimpa/); app.ids.notesAdd.fire('click'); assert.equal(saved.get('gt30-notes-v1'),'{broken');
saved.set('gt30-notes-v1',JSON.stringify([{id:'1',title:'x',body:'',updated:1},{id:'1',title:'y',body:'',updated:2}]));
app=setup(storage); assert(app.ids.notesAdd.disabled);
const blocked={getItem(){throw Error('unavailable')},setItem(){throw Error('unavailable')}};
app=setup(blocked); assert(app.ids.notesAdd.disabled); assert.match(app.ids.notesStatus.textContent,/tidak tersedia/);
console.log('Notes preview: create, edit, reload, validation, discard, delete, corruption and failed writes OK');
