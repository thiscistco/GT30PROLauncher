(function () {
  'use strict';
  const grid=document.getElementById('superGrid');
  if (!grid) return;
  const key='gt30_super_order_v1';
  function items() { return Array.from(grid.children); }
  function save() { try { localStorage.setItem(key,JSON.stringify(items().map(x=>x.id))); } catch (_) {} }
  try {
    const stored=JSON.parse(localStorage.getItem(key)||'[]');
    if(Array.isArray(stored)) {
      const original=items(), seen=new Set();
      for(const id of stored) {
        const el=original.find(x=>x.id===id);
        if(el && !seen.has(el)) { grid.appendChild(el); seen.add(el); }
      }
      original.filter(x=>!seen.has(x)).forEach(x=>grid.appendChild(x));
    }
  } catch (_) {}
  let current=null, timer=null, dragging=false, suppress=false, target=null;
  function cancelTimer() { if(timer!==null) clearTimeout(timer); timer=null; }
  function finish() {
    cancelTimer();
    if(current) current.classList.remove('dragging');
    current=null; target=null; dragging=false; window.superDragActive=false;
  }
  grid.addEventListener('pointerdown',function(e) {
    const item=e.target.closest('#superGrid > *');
    if(!item || e.button!==0 || e.isPrimary===false) return;
    finish(); current=item; const startX=e.clientX,startY=e.clientY,id=e.pointerId;
    current._dragStart={x:startX,y:startY,id};
    timer=setTimeout(function() {
      if(!current) return;
      dragging=true; window.superDragActive=true; current.classList.add('dragging');
      document.dispatchEvent(new Event('pointercancel'));
    },550);
  });
  document.addEventListener('pointermove',function(e) {
    if(!current || e.pointerId!==current._dragStart.id) return;
    if(!dragging && Math.hypot(e.clientX-current._dragStart.x,e.clientY-current._dragStart.y)>10) { finish(); return; }
    if(dragging) {
      e.preventDefault();
      const hit=document.elementFromPoint(e.clientX,e.clientY);
      target=hit && hit.closest('#superGrid > *');
    }
  },{passive:false});
  document.addEventListener('pointerup',function(e) {
    if(!current || e.pointerId!==current._dragStart.id) return;
    if(dragging) {
      if(target && target!==current) {
        const list=items(), from=list.indexOf(current), to=list.indexOf(target);
        grid.insertBefore(current,from<to ? target.nextSibling : target); save();
      }
      suppress=true; setTimeout(()=>{suppress=false;},400);
    }
    finish();
  });
  document.addEventListener('pointercancel',function(e) { if(e.isTrusted) finish(); });
  grid.addEventListener('click',function(e) { if(suppress) { e.preventDefault(); e.stopImmediatePropagation(); suppress=false; } },true);
  grid.addEventListener('keydown',function(e) {
    if(!e.altKey || !['ArrowLeft','ArrowRight','ArrowUp','ArrowDown'].includes(e.key)) return;
    const item=e.target.closest('#superGrid > *'), list=items(), from=list.indexOf(item);
    if(from<0) return;
    const delta=e.key==='ArrowLeft'?-1:e.key==='ArrowRight'?1:e.key==='ArrowUp'?-4:4;
    const to=Math.max(0,Math.min(list.length-1,from+delta));
    if(from!==to) { grid.insertBefore(item,from<to?list[to].nextSibling:list[to]); save(); item.focus(); }
    e.preventDefault();
  });
})();
