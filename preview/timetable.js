/* Weekly timetable preview. Data stays in local browser storage. */
(function(root,factory) {
  var api=factory();
  if(typeof module!=='undefined' && module.exports) module.exports=api;
  if(root && root.document) api.mount(root);
})(typeof window!=='undefined'?window:null,function(){
  'use strict';
  var days=['Senin','Selasa','Rabu','Kamis','Jumat'];
  function minutes(s) {
    if(typeof s!=='string' || !/^\d{2}:\d{2}$/.test(s)) return -1;
    var h=+s.slice(0,2),m=+s.slice(3); return h<24 && m<60?h*60+m:-1;
  }
  function validRange(start,end,rows,edit) {
    var a=minutes(start),b=minutes(end); if(a<0 || b<=a)return false;
    return !rows.some(function(row,i){var x=minutes(row.start),y=minutes(row.end);return i!==edit && x>=0 && y>x && a<y && x<b;});
  }
  function sorted(rows) { return rows.slice().sort(function(a,b){return a.start.localeCompare(b.start);}); }
  function mount(win) {
    var doc=win.document, byId=function(id){return doc.getElementById(id);};
    if(!byId('timetablePage')) return;
    var key='gt30-timetable-v1', rows=[], writable=true, editor=byId('ttEditor'), form=byId('ttForm');
    var mode='', index=-1, day=-1, returnFocus=null;
    try { var saved=JSON.parse(win.localStorage.getItem(key)||'[]'); if(Array.isArray(saved)) rows=sorted(saved.filter(function(row){return row && minutes(row.start)>=0 && minutes(row.end)>minutes(row.start);})); }
    catch(e){ writable=false; byId('ttStorage').textContent='Penyimpanan tidak tersedia. Perubahan tidak dapat disimpan.'; }
    function save(next) {
      if(!writable) { byId('ttError').textContent='Penyimpanan tidak tersedia. Perubahan tidak disimpan.'; return false; }
      try { win.localStorage.setItem(key,JSON.stringify(next)); rows=next; return true; }
      catch(e){byId('ttError').textContent='Penyimpanan tidak tersedia. Perubahan tidak disimpan.';return false;}
    }
    function render() {
      var body=byId('ttRows'); body.textContent=''; byId('ttEmpty').hidden=rows.length>0;
      rows.forEach(function(row,i){
        var tr=doc.createElement('tr'), th=doc.createElement('th'), time=doc.createElement('button'); th.scope='row'; time.type='button';time.className='tt-time';time.textContent=row.start+'\n–\n'+row.end;time.setAttribute('aria-label','Ubah jam '+row.start+' sampai '+row.end); time.addEventListener('click',function(){openTime(i,time);});th.appendChild(time);tr.appendChild(th);
        days.forEach(function(name,d){
          var cell=row.cells && row.cells[d] || {}, subject=typeof cell.subject==='string'?cell.subject:'', classroom=typeof cell.class==='string'?cell.class:'';
          var value=[subject,classroom].filter(Boolean).join('\n'), td=doc.createElement('td'), button=doc.createElement('button'); button.type='button';button.className=value?'tt-filled':'';button.textContent=value||'+';button.setAttribute('aria-label',name+', '+row.start+' sampai '+row.end+', '+(value||'kosong')+'. Ubah jadwal');button.addEventListener('click',function(){openCell(i,d,button);});td.appendChild(button);tr.appendChild(td);
        });body.appendChild(tr);
      });
    }
    function begin(kind,i,d,origin) {
      mode=kind;index=i;day=d;returnFocus=origin;editor.hidden=false;byId('ttError').textContent='';
      var time=kind==='time';byId('ttTimeFields').hidden=!time;byId('ttCellFields').hidden=time;byId('ttStart').required=time;byId('ttEnd').required=time;byId('ttDelete').hidden=!time||i<0;
    }
    function openTime(i,origin) {
      begin('time',i,-1,origin);byId('ttEditorTitle').textContent=i<0?'Tambah jam':'Ubah jam';byId('ttStart').value=i<0?'07:00':rows[i].start;byId('ttEnd').value=i<0?'08:00':rows[i].end;byId('ttStart').focus();
    }
    function openCell(i,d,origin) {
      begin('cell',i,d,origin);var cell=rows[i].cells && rows[i].cells[d] || {};
      byId('ttEditorTitle').textContent=days[d]+' · '+rows[i].start+'–'+rows[i].end;byId('ttSubject').value=cell.subject||'';byId('ttClass').value=cell.class||'';byId('ttSubject').focus();
    }
    function close() { if(editor.hidden)return false;editor.hidden=true;byId('ttError').textContent='';if(returnFocus && returnFocus.isConnected) returnFocus.focus();else byId('ttAdd').focus();return true; }
    byId('ttAdd').addEventListener('click',function(){openTime(-1,this);});byId('ttCancel').addEventListener('click',close);
    byId('ttDelete').addEventListener('click',function(){if(index<0 || !win.confirm('Hapus jam dan semua jadwal pada baris ini?'))return;var next=rows.slice();next.splice(index,1);if(save(next)){close();render();}});
    form.addEventListener('submit',function(event){
      event.preventDefault(); var next=JSON.parse(JSON.stringify(rows));
      if(mode==='time') {
        var start=byId('ttStart').value,end=byId('ttEnd').value;
        if(!validRange(start,end,rows,index)){byId('ttError').textContent='Jam selesai harus setelah mulai dan tidak boleh bertabrakan dengan baris lain.';return;}
        if(index<0) next.push({start:start,end:end,cells:[]});else {next[index].start=start;next[index].end=end;} next=sorted(next);
      } else {
        if(!next[index])return; if(!Array.isArray(next[index].cells))next[index].cells=[];
        next[index].cells[day]={subject:byId('ttSubject').value.trim(),class:byId('ttClass').value.trim()};
      }
      if(save(next)){close();render();}
    });
    win.timetableCloseEditor=close;win.timetableOnOpen=render;render();
  }
  return {minutes:minutes,validRange:validRange,sorted:sorted,mount:mount};
});
