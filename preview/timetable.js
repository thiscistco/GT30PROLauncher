/* Weekly timetable preview. Data stays in local browser storage. */
(function(root,factory) {
  var api=factory();
  if(typeof module!=='undefined' && module.exports) module.exports=api;
  if(root && root.document) api.mount(root);
})(typeof window!=='undefined'?window:null,function(){
  'use strict';
  var days=['Senin','Selasa','Rabu','Kamis','Jumat'];
  var defaults=[['07:00','08:00',''],['08:00','08:40',''],['08:40','09:15',''],['09:15','09:45',''],['09:45','10:30','Istirahat'],['10:30','11:10',''],['11:10','11:50',''],['11:50','12:50','ISHOMA'],['12:50','13:30',''],['13:30','14:10',''],['14:10','14:50',''],['14:50','15:30','']];
  var colorKeys=['biru','hijau','kuning','merah','ungu','jingga'];
  var colors=['#bbddf7','#bfe9ce','#f4e3a4','#f6c7ca','#dcccf4','#f6d4b4'];
  function normalize(s) { return (s||'').trim().replace(/\s+/g,' ').toLowerCase(); }
  function colorIndex(classroom,subject,selected,rows) {
    var manual=colorKeys.indexOf(selected); if(manual>=0)return manual;
    var c=normalize(classroom),s=normalize(subject);
    if(c||s) for(var r=0;r<rows.length;r++) {
      var cells=Array.isArray(rows[r].cells)?rows[r].cells:[];
      for(var d=0;d<cells.length;d++) { var cell=cells[d]; if(!cell)continue;
        if(c===normalize(cell.class) && s===normalize(cell.subject)) { var match=colorKeys.indexOf(cell.color); if(match>=0)return match; }
      }
    }
    var key=c+'\u0000'+s, hash=0; for(var i=0;i<key.length;i++) hash=(Math.imul(hash,31)+key.charCodeAt(i))|0;
    return (hash>>>0)%colors.length;
  }
  function minutes(s) {
    if(typeof s!=='string' || !/^\d{2}:\d{2}$/.test(s)) return -1;
    var h=+s.slice(0,2),m=+s.slice(3); return h<24 && m<60?h*60+m:-1;
  }
  function validRange(start,end,rows,edit) {
    var a=minutes(start),b=minutes(end); if(a<0 || b<=a)return false;
    return !rows.some(function(row,i){var x=minutes(row.start),y=minutes(row.end);return i!==edit && x>=0 && y>x && a<y && x<b;});
  }
  function sorted(rows) { return rows.slice().sort(function(a,b){return a.start.localeCompare(b.start);}); }
  function seed(saved) {
    var rows=JSON.parse(JSON.stringify(saved)), skipped=0;
    defaults.forEach(function(preset){
      var existing=rows.find(function(row){return row.start===preset[0] && row.end===preset[1];});
      if(existing) {
        var hasEntry=Array.isArray(existing.cells) && existing.cells.some(function(cell){return cell && (String(cell.subject||'').trim() || String(cell.class||'').trim());});
        if(preset[2] && !hasEntry && !existing.label)existing.label=preset[2];
        return;
      }
      if(!validRange(preset[0],preset[1],rows,-1)){skipped++;return;}
      var row={start:preset[0],end:preset[1],cells:[]};if(preset[2])row.label=preset[2];rows.push(row);
    });
    return {rows:sorted(rows),skipped:skipped};
  }
  function mount(win) {
    var doc=win.document, byId=function(id){return doc.getElementById(id);};
    if(!byId('timetablePage')) return;
    var key='gt30-timetable-v1', rows=[], writable=true, editor=byId('ttEditor'), form=byId('ttForm');
    var mode='', index=-1, day=-1, returnFocus=null, selectedColor='auto';
    try {
      var saved=JSON.parse(win.localStorage.getItem(key)||'[]'); if(!Array.isArray(saved)) throw Error('invalid data');
      rows=sorted(saved.filter(function(row){return row && minutes(row.start)>=0 && minutes(row.end)>minutes(row.start);}));
      if(!win.localStorage.getItem(key+'-default-v2')) {
        var seeded=seed(rows); win.localStorage.setItem(key,JSON.stringify(seeded.rows)); win.localStorage.setItem(key+'-default-v2','1'); rows=seeded.rows;
        if(seeded.skipped)byId('ttStorage').textContent='Sebagian baris bawaan bertabrakan dengan jadwal lama dan tidak ditambahkan. Jadwal lama tetap tersimpan.';
      }
    }
    catch(e){ writable=false; byId('ttStorage').textContent='Penyimpanan tidak tersedia. Perubahan tidak dapat disimpan.'; }
    function save(next) {
      if(!writable) { byId('ttError').textContent='Penyimpanan tidak tersedia. Perubahan tidak disimpan.'; return false; }
      try { win.localStorage.setItem(key,JSON.stringify(next)); rows=next; return true; }
      catch(e){byId('ttError').textContent='Penyimpanan tidak tersedia. Perubahan tidak disimpan.';return false;}
    }
    function render() {
      var body=byId('ttRows'); body.textContent=''; byId('ttEmpty').hidden=rows.length>0; byId('ttScroll').style.setProperty('--tt-min-height',(6+Math.max(1,rows.length)*6.35)+'mm');
      rows.forEach(function(row,i){
        var tr=doc.createElement('tr'), th=doc.createElement('th'), time=doc.createElement('button'); th.scope='row'; time.type='button';time.className='tt-time';time.textContent=row.start+'\n'+row.end;time.setAttribute('aria-label','Ubah jam '+row.start+' sampai '+row.end); time.addEventListener('click',function(){openTime(i,time);});th.appendChild(time);tr.appendChild(th);
        if(row.label) {
          var td=doc.createElement('td'), label=doc.createElement('button');td.colSpan=5;label.type='button';label.className='tt-break';label.textContent=row.label;label.setAttribute('aria-label',row.label+', '+row.start+' sampai '+row.end+'. Ubah jam dan label');label.addEventListener('click',function(){openTime(i,label);});td.appendChild(label);tr.appendChild(td);
        } else days.forEach(function(name,d){
          var cell=row.cells && row.cells[d] || {}, subject=typeof cell.subject==='string'?cell.subject:'', classroom=typeof cell.class==='string'?cell.class:'';
          var value=[classroom,subject].filter(Boolean).join('\n'), td=doc.createElement('td'), button=doc.createElement('button'); button.type='button';button.className=value?'tt-filled':'';if(value){[classroom,subject].filter(Boolean).forEach(function(text){var line=doc.createElement('span');line.textContent=text;button.appendChild(line);});button.style.backgroundColor=colors[colorIndex(classroom,subject,cell.color,rows)];}button.setAttribute('aria-label',name+', '+row.start+' sampai '+row.end+', '+(value||'kosong')+'. Ubah jadwal');button.addEventListener('click',function(){openCell(i,d,button);});td.appendChild(button);tr.appendChild(td);
        });body.appendChild(tr);
      });
    }
    function begin(kind,i,d,origin) {
      mode=kind;index=i;day=d;returnFocus=origin;editor.hidden=false;byId('ttError').textContent='';
      var time=kind==='time';byId('ttTimeFields').hidden=!time;byId('ttCellFields').hidden=time;byId('ttStart').required=time;byId('ttEnd').required=time;byId('ttDelete').hidden=time&&i<0; byId('ttDelete').setAttribute('aria-label',time?'Hapus jam':'Hapus jadwal');byId('ttDelete').title=time?'Hapus jam':'Hapus jadwal';
    }
    function openTime(i,origin) {
      begin('time',i,-1,origin);byId('ttEditorTitle').textContent=i<0?'Tambah jam':'Ubah jam';byId('ttStart').value=i<0?'07:00':rows[i].start;byId('ttEnd').value=i<0?'08:00':rows[i].end;byId('ttLabel').value=i<0?'':rows[i].label||'';byId('ttAdd').hidden=i<0;byId('ttStart').focus();
    }
    function openCell(i,d,origin) {
      begin('cell',i,d,origin);var cell=rows[i].cells && rows[i].cells[d] || {};
      byId('ttEditorTitle').textContent=days[d]+' · '+rows[i].start+'–'+rows[i].end;byId('ttSubject').value=cell.subject||'';byId('ttClass').value=cell.class||'';selectedColor=colorKeys.includes(cell.color)?cell.color:'auto';renderColors();byId('ttClass').focus();
    }
    function renderColors() {
      var container=byId('ttColors');container.textContent='';
      ['auto'].concat(colorKeys).forEach(function(key,i){
        var button=doc.createElement('button');button.type='button';button.setAttribute('role','radio');button.setAttribute('aria-checked',String(key===selectedColor));button.setAttribute('aria-label',key==='auto'?'Warna otomatis':'Warna '+key);button.title=key==='auto'?'Otomatis':key;button.textContent=key==='auto'?'A':'';if(i)button.style.backgroundColor=colors[i-1];button.addEventListener('click',function(){selectedColor=key;renderColors();container.querySelector('[aria-checked="true"]').focus();});container.appendChild(button);
      });
    }
    function close() { if(editor.hidden)return false;editor.hidden=true;byId('ttError').textContent='';if(returnFocus && returnFocus.isConnected) returnFocus.focus();else byId('timetableBack').focus();return true; }
    byId('ttAdd').addEventListener('click',function(){openTime(-1,byId('timetableBack'));});byId('ttAddEmpty').addEventListener('click',function(){openTime(-1,this);});byId('ttCancel').addEventListener('click',close);
    byId('ttDelete').addEventListener('click',function(){
      if(index<0 || !win.confirm(mode==='time'?'Hapus jam dan semua jadwal pada baris ini?':'Hapus jadwal pada sel ini?'))return;
      var next=JSON.parse(JSON.stringify(rows));
      if(mode==='time')next.splice(index,1);else { if(!Array.isArray(next[index].cells))next[index].cells=[];next[index].cells[day]=null; }
      if(save(next)){close();render();}
    });
    form.addEventListener('submit',function(event){
      event.preventDefault(); var next=JSON.parse(JSON.stringify(rows));
      if(mode==='time') {
        var start=byId('ttStart').value,end=byId('ttEnd').value;
        if(!validRange(start,end,rows,index)){byId('ttError').textContent='Jam selesai harus setelah mulai dan tidak boleh bertabrakan dengan baris lain.';return;}
        if(index<0) next.push({start:start,end:end,cells:[],label:byId('ttLabel').value.trim()});else {next[index].start=start;next[index].end=end;next[index].label=byId('ttLabel').value.trim();} next=sorted(next);
      } else {
        if(!next[index])return; if(!Array.isArray(next[index].cells))next[index].cells=[];
        next[index].cells[day]={class:byId('ttClass').value.trim(),subject:byId('ttSubject').value.trim(),color:selectedColor};
      }
      if(save(next)){close();render();}
    });
    win.timetableCloseEditor=close;win.timetableOnOpen=render;render();
  }
  return {minutes:minutes,validRange:validRange,sorted:sorted,seed:seed,colorIndex:colorIndex,colors:colors,mount:mount};
});
