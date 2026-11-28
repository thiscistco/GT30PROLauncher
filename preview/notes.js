/* Browser-only local notes. Android notes use a separate database. */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) module.exports = factory;
  else factory(root);
})(typeof window === 'undefined' ? null : window, function mountNotes(win) {
  'use strict';
  var doc=win.document, byId=function (id) { return doc.getElementById(id); };
  var key='gt30-notes-v1', notes=[], writable=true, editing=null, lastTitle='', lastBody='', lastTag='', draftTag='', timer=null, section=0, layoutMode=0;
  var form=byId('notesForm'), title=byId('notesTitle'), body=byId('notesBody'), list=byId('notesList'), panel=byId('notesListPanel'), status=byId('notesStatus');
  var search=byId('notesSearch'), sectionSelect=byId('notesSection'), layout=byId('notesLayout'), saveStatus=byId('notesSaveStatus');
  var tagButton=byId('notesTag'), tagMenu=byId('notesTagMenu'), tagValue=byId('notesTagValue');
  if (!form || !title || !body || !list || !panel || !status || !search || !sectionSelect || !layout || !saveStatus || !tagButton || !tagMenu || !tagValue) return;
  function validRecord(n) { return n && typeof n.id==='string' && n.id.length>0 && typeof n.title==='string' && n.title.length<=120 && typeof n.body==='string' && n.body.length<=20000 && typeof n.updated==='number' && Number.isFinite(n.updated) && (n.pinned===undefined || typeof n.pinned==='boolean') && (n.state===undefined || [0,1,2].includes(n.state)) && (n.previousState===undefined || [0,1].includes(n.previousState)) && (n.tag===undefined || (typeof n.tag==='string' && n.tag.length<=60)) && (n.color===undefined || ([0,1,2,3,4].includes(n.color))); }
  function load() {
    try {
      var raw=win.localStorage.getItem(key), value=raw===null?[]:JSON.parse(raw);
      if(!Array.isArray(value) || !value.every(validRecord) || new Set(value.map(function (n) { return n.id; })).size!==value.length) throw new Error('Invalid notes');
      notes=value.map(function(n){return Object.assign({pinned:false,state:0,previousState:0,tag:'',color:0},n);}); writable=true;
      var storedLayout=win.localStorage.getItem('gt30-notes-layout'); layoutMode=storedLayout===null ? (win.localStorage.getItem('gt30-notes-grid')==='1'?1:0) : Number(storedLayout); if(![0,1,2].includes(layoutMode)) layoutMode=0;
    } catch (_) { notes=[]; writable=false; }
    status.textContent=writable ? 'Catatan tersimpan di browser ini, bukan di aplikasi Android. Menghapus data browser menghapus catatan.' : 'Penyimpanan catatan tidak tersedia atau data tidak dapat dibaca. Data lama tidak ditimpa.';
  }
  function persist(next) {
    if (!writable) return false;
    try { win.localStorage.setItem(key,JSON.stringify(next)); notes=next; return true; }
    catch (_) { status.textContent='Tidak dapat menyimpan catatan. Perubahan tetap di editor.'; return false; }
  }
  function render() {
    list.textContent=''; list.className=layoutMode===1?'notes-grid':layoutMode===2?'notes-staggered':'';
    var layoutName='Tampilan: '+['daftar','kotak','staggered'][layoutMode], sectionName='Lokasi: '+['Catatan','Arsip','Sampah'][section];
    layout.setAttribute('aria-label',layoutName); layout.setAttribute('title',layoutName); byId('notesLayoutIcon').className='notes-icon notes-icon-'+['list','grid','staggered'][layoutMode];
    sectionSelect.value=String(section); sectionSelect.setAttribute('aria-label',sectionName); sectionSelect.setAttribute('title',sectionName); byId('notesSectionIcon').className='notes-icon notes-icon-'+['notes','archive','trash'][section];
    var q=search.value.trim().toLocaleLowerCase(), ordered=notes.filter(function(n){return n.state===section && (!q || (n.title+'\n'+n.body+'\n'+n.tag).toLocaleLowerCase().includes(q));}).sort(function(a,b) { return Number(b.pinned)-Number(a.pinned) || b.updated-a.updated; });
    byId('notesEmpty').hidden=ordered.length!==0 || !writable; byId('notesAdd').hidden=section!==0; byId('notesAdd').disabled=!writable;
    ordered.forEach(function(n) {
      var wrap=doc.createElement('div'), button=doc.createElement('button'), heading=doc.createElement('strong'), summary=doc.createElement('span'), actions=doc.createElement('div');
      wrap.className='notes-item notes-color-'+n.color; button.type='button'; button.className='notes-card notes-color-'+n.color;
      heading.textContent=(n.pinned?'📌 ':'')+(n.title.trim() || 'Tanpa judul'); summary.textContent=n.body;
      button.appendChild(heading); if(n.body.trim()) button.appendChild(summary); if(n.tag) { var tag=doc.createElement('span'); tag.textContent='#'+n.tag; button.appendChild(tag); }
      button.setAttribute('aria-label','Buka catatan '+(n.title.trim() || 'Tanpa judul')); button.addEventListener('click',function () { if(n.state!==2) open(n); }); wrap.appendChild(button);
      actions.className='notes-controls';
      function action(label,icon,fn) { var b=doc.createElement('button'), symbol=doc.createElement('span'); b.type='button'; b.className='notes-icon-button'; b.setAttribute('aria-label',label); b.setAttribute('title',label); symbol.className='notes-icon notes-icon-'+icon; symbol.setAttribute('aria-hidden','true'); b.appendChild(symbol); b.addEventListener('click',fn); actions.appendChild(b); }
      if(n.state===2) {
        action('Pulihkan','restore',function(){update(n,{state:n.previousState===1?1:0,previousState:0});});
        action('Hapus permanen','trash',function(){if(win.confirm('Hapus permanen? Catatan tidak dapat dipulihkan.')) { var next=notes.filter(function(x){return x.id!==n.id;}); if(persist(next)) render(); }});
      } else {
        action(n.pinned?'Lepas pin':'Pin',n.pinned?'unpin':'pin',function(){update(n,{pinned:!n.pinned});});
        action(n.state===1?'Buka arsip':'Arsip',n.state===1?'restore':'archive',function(){update(n,{state:n.state===1?0:1});});
        action('Sampah','trash',function(){if(win.confirm('Pindahkan catatan ke Sampah?')) update(n,{previousState:n.state,state:2});});
        action('Tag','tag',function(){var x=win.prompt('Label catatan (maksimal 60 karakter)',n.tag); if(x!==null) update(n,{tag:x.trim().slice(0,60)});});
        action('Warna','color',function(){var x=win.prompt('Warna: 0 bawaan, 1 merah, 2 kuning, 3 hijau, 4 biru',String(n.color)); if(x!==null && /^[0-4]$/.test(x.trim())) update(n,{color:Number(x.trim())});});
      }
      if(n.state!==2) {
        n.body.split('\n').forEach(function(line,index){
          if(!line.startsWith('☐ ') && !line.startsWith('☑ ')) return;
          var label=doc.createElement('label'), check=doc.createElement('input'), caption=doc.createElement('span');
          check.type='checkbox'; check.checked=line.startsWith('☑ '); caption.textContent=line.slice(2);
          check.addEventListener('change',function(){ var lines=n.body.split('\n'); lines[index]=(check.checked?'☑ ':'☐ ')+line.slice(2); var next=notes.map(function(x){return x.id===n.id?Object.assign({},x,{body:lines.join('\n'),updated:Date.now()}):x;}); if(persist(next)) render(); else check.checked=!check.checked; });
          label.appendChild(check); label.appendChild(caption); wrap.appendChild(label);
        });
      }
      wrap.appendChild(actions); list.appendChild(wrap);
    });
  }
  function update(n,values) { var next=notes.map(function(x){return x.id===n.id?Object.assign({},x,values,{updated:Date.now()}):x;}); if(persist(next)) render(); }
  function open(n) {
    editing=n ? n.id : ''; lastTitle=n ? n.title : ''; lastBody=n ? n.body : ''; lastTag=n ? n.tag : ''; draftTag=lastTag; updateTag(); closeTagMenu();
    title.value=lastTitle; body.value=lastBody; panel.hidden=true; form.hidden=false;
    byId('notesEditorHeading').textContent=n?'Edit catatan':'Catatan baru'; saveStatus.textContent='Tersimpan otomatis di browser'; title.focus();
  }
  function updateTag() { tagValue.textContent=draftTag?'#'+draftTag:''; tagValue.setAttribute('aria-label',draftTag?'Tag: '+draftTag:'Tidak ada tag'); }
  function closeTagMenu() { tagMenu.hidden=true; tagButton.setAttribute('aria-expanded','false'); }
  function tagChoice(label,fn) { var b=doc.createElement('button'); b.type='button'; b.className='notes-button'; b.textContent=label; b.addEventListener('click',fn); tagMenu.appendChild(b); }
  function selectTag(value) { draftTag=value; updateTag(); closeTagMenu(); queue(); }
  function showTags() {
    tagMenu.textContent='';
    tagChoice('+ Buat tag baru',function(){var value=win.prompt('Nama tag (maksimal 60 karakter)',''); if(value===null) return; value=value.trim().slice(0,60); if(value) selectTag(value); });
    tagChoice('Tanpa tag',function(){selectTag('');});
    var tags=Array.from(new Set(notes.map(function(n){return n.tag;}).filter(Boolean))); if(draftTag && !tags.includes(draftTag)) tags.push(draftTag);
    tags.sort(function(a,b){return a.localeCompare(b);}); tags.forEach(function(value){tagChoice(value,function(){selectTag(value);});});
    tagMenu.hidden=false; tagButton.setAttribute('aria-expanded','true');
  }
  function cancelTimer() { if(timer!==null) win.clearTimeout(timer); timer=null; }
  function newId() { var id; do { id=Date.now().toString(36)+'-'+Math.random().toString(36).slice(2); } while(notes.some(function(n){return n.id===id;})); return id; }
  function save() {
    cancelTimer(); if(editing===null) return true;
    var t=title.value,b=body.value;
    if(t===lastTitle && b===lastBody && draftTag===lastTag) { saveStatus.textContent='Tersimpan di browser'; return true; }
    if(t.length>120 || b.length>20000) { saveStatus.textContent='Teks melebihi batas.'; return false; }
    if(!editing && !t.trim() && !b.trim()) { saveStatus.textContent='Catatan kosong belum disimpan'; return true; }
    var id=editing || newId(), next=notes.slice(), index=next.findIndex(function(n){return n.id===id;});
    if(editing && index<0) { saveStatus.textContent='Catatan tidak ditemukan; perubahan tetap di editor.'; return false; }
    var n=index<0 ? {id:id,pinned:false,state:0,previousState:0,tag:'',color:0} : next[index];
    var note=Object.assign({},n,{title:t,body:b,tag:draftTag,updated:Date.now()}); if(index<0) next.push(note); else next[index]=note;
    if(!persist(next)) { saveStatus.textContent='Gagal menyimpan; teks masih di editor.'; return false; }
    editing=id; lastTitle=t; lastBody=b; lastTag=draftTag; saveStatus.textContent='Tersimpan di browser'; return true;
  }
  function queue() { cancelTimer(); saveStatus.textContent='Menyimpan…'; timer=win.setTimeout(save,600); }
  function closeEditor() { if(!save()) return false; editing=null; form.hidden=true; panel.hidden=false; render(); byId('notesAdd').focus(); return true; }
  byId('notesAdd').addEventListener('click',function () { if(writable) open(null); });
  tagButton.addEventListener('click',function(){if(tagMenu.hidden) showTags(); else closeTagMenu();});
  title.addEventListener('input',queue); body.addEventListener('input',queue);
  byId('notesAddChecklist').addEventListener('click',function(){ var value=win.prompt('Nama item checklist',''); if(value===null || !value.trim()) return; var addition=(body.value && !body.value.endsWith('\n')?'\n':'')+'☐ '+value.trim().slice(0,200); if(body.value.length+addition.length>20000) { saveStatus.textContent='Catatan terlalu panjang.'; return; } body.value+=addition; queue(); });
  form.addEventListener('submit',function(e) { e.preventDefault(); closeEditor(); });
  search.addEventListener('input',render); sectionSelect.addEventListener('change',function(){section=Number(sectionSelect.value)||0; render();});
  layout.addEventListener('click',function(){layoutMode=(layoutMode+1)%3; try{win.localStorage.setItem('gt30-notes-layout',String(layoutMode));}catch(_){} render();});
  if(doc.addEventListener) doc.addEventListener('visibilitychange',function(){if(doc.hidden) save();});
  win.notesOnOpen=function () { load(); editing=null; form.hidden=true; panel.hidden=false; render(); };
  win.notesGoBack=function () { if(editing===null) return false; closeEditor(); return true; };
  win.notesConfirmLeave=function () { return editing===null || closeEditor(); };
  load(); render();
});
