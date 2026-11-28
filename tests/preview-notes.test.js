const assert=require('assert');
const mount=require('../preview/notes.js');
class Element {
  constructor(){ this.listeners={}; this.hidden=false; this.disabled=false; this.value=''; this.children=[]; this.attrs={}; this._text=''; this.className=''; }
  addEventListener(name,fn){ (this.listeners[name] ||= []).push(fn); }
  fire(name){ for(const fn of this.listeners[name]||[]) fn.call(this,{preventDefault(){}}); }
  appendChild(node){this.children.push(node);}
  set textContent(v){this._text=String(v);this.children=[];}
  get textContent(){return this._text+this.children.map(n=>n.textContent).join('');}
  setAttribute(k,v){this.attrs[k]=v;}
  focus(){this.focused=true;}
}
function setup(storage) {
  const ids={}; for(const id of ['notesForm','notesTitle','notesBody','notesList','notesListPanel','notesStatus','notesEmpty','notesAdd','notesAddChecklist','notesTag','notesTagMenu','notesTagValue','notesSectionIcon','notesLayoutIcon','notesEditorHeading','notesSearch','notesSection','notesLayout','notesSaveStatus']) ids[id]=new Element();
  ids.notesForm.hidden=true; ids.notesTagMenu.hidden=true; let confirmation=true, answer=null, timers=new Map(), seq=0;
  const win={document:{getElementById:id=>ids[id],createElement:()=>new Element()},localStorage:storage,confirm:()=>confirmation,prompt:()=>answer,
    setTimeout(fn){let id=++seq;timers.set(id,fn);return id;},clearTimeout(id){timers.delete(id);}};
  mount(win); return {ids,win,flush(){for(const [k,fn] of Array.from(timers)) {timers.delete(k);fn();}},setConfirm(v){confirmation=v},setPrompt(v){answer=v}};
}
const saved=new Map(); let failWrite=false;
const storage={getItem:k=>saved.has(k)?saved.get(k):null,setItem:(k,v)=>{if(failWrite)throw Error('quota');saved.set(k,v);}};
let app=setup(storage),e=app.ids, records=()=>JSON.parse(saved.get('gt30-notes-v1'));
assert(!e.notesEmpty.hidden); e.notesAdd.fire('click'); assert(!e.notesForm.hidden);
app.setPrompt('Kerja'); e.notesTag.fire('click'); e.notesTagMenu.children[0].fire('click'); assert.equal(e.notesTagValue.textContent,'#Kerja'); app.flush(); assert(!saved.has('gt30-notes-v1'));
e.notesBody.value='<script>not executable</script>'; e.notesBody.fire('input'); app.flush();
assert.equal(records().length,1); assert.equal(records()[0].body,'<script>not executable</script>'); assert.deepEqual(records()[0].tags,['Kerja']);
assert(e.notesForm.hidden===false); assert(app.win.notesGoBack()); assert(e.notesForm.hidden); assert.equal(e.notesList.children.length,1);
assert.equal(e.notesList.children[0].children[0].children[0].textContent,'Tanpa judul');
assert.equal(e.notesList.children[0].children[0].children[1].textContent,'<script>not executable</script>');
let id=records()[0].id;
// A second note can select an existing tag and persist it with its first save.
e.notesAdd.fire('click'); e.notesTag.fire('click'); assert.equal(e.notesTagMenu.children[2].textContent,'Kerja'); e.notesTagMenu.children[2].fire('click'); e.notesBody.value='Salinan tag'; e.notesBody.fire('input'); app.flush(); assert.deepEqual(records()[1].tags,['Kerja']); assert(app.win.notesGoBack());
// Remove the extra note so subsequent tests retain a single fixture.
let extra=records()[1].id; saved.set('gt30-notes-v1',JSON.stringify(records().filter(n=>n.id!==extra))); app=setup(storage); e=app.ids;
// Reload data, edit, then force a save by leaving before debounce.
app=setup(storage); e=app.ids; e.notesList.children[0].children[0].fire('click');
e.notesTitle.value='Catatan penting'; e.notesBody.value='Isi baru'; e.notesBody.fire('input'); assert(app.win.notesConfirmLeave());
assert.equal(records()[0].id,id); assert.equal(records()[0].body,'Isi baru');
// Search both body and title; empty queries show all.
e.notesSearch.value='ISI BARU'; e.notesSearch.fire('input'); assert.equal(e.notesList.children.length,1);
e.notesSearch.value='missing'; e.notesSearch.fire('input'); assert.equal(e.notesList.children.length,0);
e.notesSearch.value='penting'; e.notesSearch.fire('input'); assert.equal(e.notesList.children.length,1);
e.notesSearch.value=''; e.notesSearch.fire('input');
// Checklist items are interactive and survive reload.
e.notesList.children[0].children[0].fire('click'); app.setPrompt('Susu'); e.notesAddChecklist.fire('click'); app.flush(); assert(app.win.notesConfirmLeave());
let checkLabel=e.notesList.children[0].children[1]; assert.equal(checkLabel.children[1].textContent,'Susu'); checkLabel.children[0].checked=true; checkLabel.children[0].fire('change'); assert(records()[0].body.includes('☑ Susu'));
// Metadata: pin, label, color, archive, restore, trash, recover, permanently delete.
let actions=()=>e.notesList.children[0].children[e.notesList.children[0].children.length-1].children;
actions()[0].fire('click'); assert.equal(records()[0].pinned,true);
actions()[3].fire('click'); app.setPrompt('Belajar'); e.notesTagMenu.children[0].fire('click'); assert(app.win.notesConfirmLeave()); assert.deepEqual(records()[0].tags,['Kerja','Belajar']);
app.setPrompt('2'); actions()[4].fire('click'); assert.equal(records()[0].color,2);
e.notesLayout.fire('click'); assert.equal(e.notesList.className,'notes-grid'); e.notesLayout.fire('click'); assert.equal(e.notesList.className,'notes-staggered'); e.notesLayout.fire('click'); assert.equal(e.notesList.className,'');
actions()[1].fire('click'); assert.equal(records()[0].state,1); assert.equal(e.notesList.children.length,0);
e.notesSection.value='1'; e.notesSection.fire('change'); assert.equal(e.notesList.children.length,1);
actions()[2].fire('click'); assert.equal(records()[0].state,2); assert.equal(records()[0].previousState,1);
e.notesSection.value='2'; e.notesSection.fire('change'); assert.equal(e.notesList.children.length,1);
actions()[0].fire('click'); assert.equal(records()[0].state,1); assert.equal(e.notesList.children.length,0);
e.notesSection.value='1'; e.notesSection.fire('change'); actions()[1].fire('click'); assert.equal(records()[0].state,0);
e.notesSection.value='0'; e.notesSection.fire('change');
// A failed tag-only change remains in the editor and succeeds on retry.
e.notesList.children[0].children[0].fire('click'); app.setPrompt('Baru'); e.notesTag.fire('click'); e.notesTagMenu.children[0].fire('click'); failWrite=true; app.flush();
assert.deepEqual(records()[0].tags,['Kerja','Belajar']); assert.equal(e.notesTagValue.textContent,'#Kerja  #Belajar  #Baru'); assert(!app.win.notesConfirmLeave());
failWrite=false; assert(app.win.notesConfirmLeave()); assert.deepEqual(records()[0].tags,['Kerja','Belajar','Baru']);
// A failed write keeps editor and existing data; a retry succeeds.
e.notesList.children[0].children[0].fire('click'); e.notesBody.value='Draft belum tersimpan'; e.notesBody.fire('input'); failWrite=true; app.flush();
assert(!e.notesForm.hidden); assert.match(e.notesSaveStatus.textContent,/Gagal menyimpan/); assert.equal(records()[0].body,'Isi baru\n☑ Susu'); assert(!app.win.notesConfirmLeave());
failWrite=false; assert(app.win.notesConfirmLeave()); assert.equal(records()[0].body,'Draft belum tersimpan');
actions()[2].fire('click'); e.notesSection.value='2'; e.notesSection.fire('change'); app.setConfirm(false); actions()[1].fire('click'); assert.equal(records().length,1); app.setConfirm(true); actions()[1].fire('click'); assert.equal(records().length,0);
// Old records migrate in memory without overwriting until a user action.
const legacy=JSON.stringify([{id:'old',title:'A',body:'B',updated:1}]); saved.set('gt30-notes-v1',legacy); app=setup(storage); assert.equal(app.ids.notesList.children.length,1); assert.equal(saved.get('gt30-notes-v1'),legacy);
saved.set('gt30-notes-v1','{broken'); app=setup(storage); assert(app.ids.notesAdd.disabled); assert.match(app.ids.notesStatus.textContent,/tidak ditimpa/); app.ids.notesAdd.fire('click'); assert.equal(saved.get('gt30-notes-v1'),'{broken');
saved.set('gt30-notes-v1',JSON.stringify([{id:'1',title:'x',body:'',updated:1},{id:'1',title:'y',body:'',updated:2}])); app=setup(storage); assert(app.ids.notesAdd.disabled);
const blocked={getItem(){throw Error('unavailable')},setItem(){throw Error('unavailable')}}; app=setup(blocked); assert(app.ids.notesAdd.disabled);
console.log('Notes preview: autosave, search, pin, tags, colors, grid, archive, trash, recovery and failed writes OK');
