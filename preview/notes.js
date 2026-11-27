/* Browser-only local notes. Android notes are stored separately on the device. */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) module.exports = factory;
  else factory(root);
})(typeof window === 'undefined' ? null : window, function mountNotes(win) {
  'use strict';
  var doc=win.document, byId=function (id) { return doc.getElementById(id); };
  var key='gt30-notes-v1', notes=[], writable=true, editing=null, originalTitle='', originalBody='';
  var form=byId('notesForm'), title=byId('notesTitle'), body=byId('notesBody'), list=byId('notesList'), panel=byId('notesListPanel'), status=byId('notesStatus');
  if (!form || !title || !body || !list || !panel || !status) return;
  function validRecord(n) { return n && typeof n.id==='string' && n.id.length>0 && typeof n.title==='string' && n.title.length<=120 && typeof n.body==='string' && n.body.length<=20000 && typeof n.updated==='number' && Number.isFinite(n.updated) && (n.title.trim() || n.body.trim()); }
  function load() {
    try {
      var raw=win.localStorage.getItem(key), value=raw===null?[]:JSON.parse(raw);
      if(!Array.isArray(value) || !value.every(validRecord) || new Set(value.map(function (n) { return n.id; })).size!==value.length) throw new Error('Invalid notes');
      notes=value; writable=true;
    } catch (_) { notes=[]; writable=false; }
    status.textContent=writable ? 'Catatan tersimpan di browser ini, bukan di aplikasi Android. Menghapus data browser menghapus catatan.' : 'Penyimpanan catatan tidak tersedia atau data tidak dapat dibaca. Data lama tidak ditimpa.';
  }
  function persist(next) {
    if (!writable) return false;
    try { win.localStorage.setItem(key,JSON.stringify(next)); notes=next; return true; }
    catch (_) { status.textContent='Tidak dapat menyimpan catatan. Perubahan tetap di editor.'; return false; }
  }
  function render() {
    list.textContent='';
    var ordered=notes.slice().sort(function(a,b) { return b.updated-a.updated; });
    byId('notesEmpty').hidden=ordered.length!==0 || !writable;
    byId('notesAdd').disabled=!writable;
    ordered.forEach(function(n) {
      var button=doc.createElement('button'), heading=doc.createElement('strong'), summary=doc.createElement('span');
      button.type='button'; button.className='notes-card'; heading.textContent=n.title.trim() || 'Tanpa judul'; summary.textContent=n.body;
      button.appendChild(heading); if(n.body.trim()) button.appendChild(summary);
      button.setAttribute('aria-label','Buka catatan '+heading.textContent); button.addEventListener('click',function () { open(n); }); list.appendChild(button);
    });
  }
  function open(n) {
    editing=n ? n.id : ''; originalTitle=n ? n.title : ''; originalBody=n ? n.body : '';
    title.value=originalTitle; body.value=originalBody; panel.hidden=true; form.hidden=false;
    byId('notesDelete').hidden=!n; byId('notesEditorHeading').textContent=n?'Edit catatan':'Catatan baru'; title.focus();
  }
  function closeEditor() { editing=null; form.hidden=true; panel.hidden=false; render(); byId('notesAdd').focus(); }
  function dirty() { return title.value!==originalTitle || body.value!==originalBody; }
  function confirmEditor() { return !dirty() || win.confirm('Buang perubahan catatan yang belum disimpan?'); }
  function newId() { var id; do { id=Date.now().toString(36)+'-'+Math.random().toString(36).slice(2); } while(notes.some(function(n){return n.id===id;})); return id; }
  byId('notesAdd').addEventListener('click',function () { if(writable) open(null); });
  byId('notesCancel').addEventListener('click',function () { if(confirmEditor()) closeEditor(); });
  form.addEventListener('submit',function(e) {
    e.preventDefault(); if(editing===null) return;
    var t=title.value, b=body.value;
    if((!t.trim() && !b.trim()) || t.length>120 || b.length>20000) { status.textContent='Isi judul atau catatan sebelum menyimpan.'; return; }
    var id=editing || newId(), next=notes.slice(), index=next.findIndex(function(n){return n.id===id;});
    if(editing && index<0) { status.textContent='Catatan tidak ditemukan; perubahan tetap di editor.'; return; }
    var note={id:id,title:t,body:b,updated:Date.now()}; if(index<0) next.push(note); else next[index]=note;
    if(persist(next)) { status.textContent='Catatan disimpan di browser ini, bukan di aplikasi Android.'; closeEditor(); }
  });
  byId('notesDelete').addEventListener('click',function () {
    if(!editing || !win.confirm('Hapus catatan ini? Tindakan tidak dapat dibatalkan.')) return;
    var next=notes.filter(function(n){return n.id!==editing;});
    if(next.length===notes.length) { status.textContent='Catatan tidak ditemukan.'; return; }
    if(persist(next)) { status.textContent='Catatan dihapus dari browser ini.'; closeEditor(); }
  });
  win.notesOnOpen=function () { load(); editing=null; form.hidden=true; panel.hidden=false; render(); };
  win.notesGoBack=function () { if(editing===null) return false; if(confirmEditor()) closeEditor(); return true; };
  win.notesConfirmLeave=function () { if(editing===null) return true; if(!confirmEditor()) return false; closeEditor(); return true; };
  load(); render();
});
