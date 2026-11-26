/* Kalender preview: tanggal lokal dan acara lokal browser, tanpa pengingat. */
(function (root, factory) {
  var logic = factory();
  if (typeof module !== 'undefined' && module.exports) module.exports = logic;
  if (root && root.document) logic.mount(root);
})(typeof window !== 'undefined' ? window : null, function () {
  'use strict';
  var months = ['Januari','Februari','Maret','April','Mei','Juni','Juli','Agustus','September','Oktober','November','Desember'];
  var weekdays = ['Min','Sen','Sel','Rab','Kam','Jum','Sab'];
  function daysInMonth(year, month) { return new Date(year, month, 0).getDate(); }
  function leadingDays(year, month) { return new Date(year, month - 1, 1).getDay(); }
  function shiftMonth(date, delta) {
    var index = Math.max(1900 * 12, Math.min(2100 * 12 + 11, date.year * 12 + date.month - 1 + delta));
    var year = Math.floor(index / 12), month = index % 12 + 1;
    return {year: year, month: month, day: Math.min(date.day, daysInMonth(year, month))};
  }
  function pad(value) { return String(value).padStart(2, '0'); }
  function dateKey(date) { return String(date.year).padStart(4, '0') + '-' + pad(date.month) + '-' + pad(date.day); }
  function label(date) { return date.day + ' ' + months[date.month-1] + ' ' + date.year; }
  function today() { var d = new Date(); return {year: d.getFullYear(), month: d.getMonth()+1, day: d.getDate()}; }
  function validTime(value) { return value === '' || (/^\d{2}:\d{2}$/.test(value) && Number(value.slice(0,2)) < 24 && Number(value.slice(3)) < 60); }
  function validTitle(value) { return typeof value === 'string' && value.trim().length > 0 && value.trim().length <= 80; }
  function validDate(value) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
    var y = Number(value.slice(0,4)), m = Number(value.slice(5,7)), d = Number(value.slice(8,10));
    return y >= 1900 && y <= 2100 && m >= 1 && m <= 12 && d >= 1 && d <= daysInMonth(y,m);
  }
  function validEvent(e) {
    return e && typeof e.id === 'string' && e.id.length > 0 && validDate(e.date) &&
      validTitle(e.title) && typeof e.time === 'string' && validTime(e.time);
  }
  function mount(win) {
    var doc = win.document, byId = function (id) { return doc.getElementById(id); };
    var grid = byId('calendarGrid'), form = byId('calendarForm'), selected = today(), records = [];
    var key = 'gt30-calendar-events-v1', writable = true;
    var note = byId('calendarStorageNote'), error = byId('calendarError');
    var defaultNote = 'Acara disimpan di browser ini; tidak ada sinkronisasi atau pengingat.';
    function showStorageNote() {
      note.textContent = writable ? defaultNote : 'Penyimpanan kalender tidak tersedia atau data tidak dapat dibaca; perubahan hanya sementara dan data lama tidak ditimpa.';
    }
    try {
      var saved = win.localStorage.getItem(key);
      if (saved !== null) {
        var parsed = JSON.parse(saved);
        if (!Array.isArray(parsed) || !parsed.every(validEvent)) throw new Error('Invalid calendar data');
        records = parsed;
      }
    } catch (e) { writable = false; }
    showStorageNote();
    function persist() {
      if (!writable) return;
      try { win.localStorage.setItem(key, JSON.stringify(records)); }
      catch (e) { writable = false; showStorageNote(); }
    }
    function render() {
      var selectedKey = dateKey(selected), todayKey = dateKey(today());
      byId('calendarMonth').textContent = months[selected.month-1] + ' ' + selected.year;
      byId('calendarSelected').textContent = label(selected);
      byId('calendarPrev').disabled = selected.year === 1900 && selected.month === 1;
      byId('calendarNext').disabled = selected.year === 2100 && selected.month === 12;
      grid.replaceChildren();
      weekdays.forEach(function (name) {
        var day = doc.createElement('span'); day.className = 'calendar-weekday'; day.textContent = name; grid.appendChild(day);
      });
      for (var empty = 0; empty < leadingDays(selected.year, selected.month); empty++) {
        var spacer = doc.createElement('span'); spacer.setAttribute('aria-hidden', 'true'); grid.appendChild(spacer);
      }
      var total = daysInMonth(selected.year, selected.month);
      for (var number = 1; number <= total; number++) (function (day) {
        var date = {year:selected.year, month:selected.month, day:day}, keyForDay = dateKey(date);
        var has = records.some(function (e) { return e.date === keyForDay; });
        var button = doc.createElement('button'); button.type = 'button'; button.className = 'calendar-day';
        button.textContent = day + (has ? '·' : '');
        button.classList.toggle('today', keyForDay === todayKey);
        button.classList.toggle('selected', keyForDay === selectedKey);
        button.setAttribute('aria-label', label(date) + (has ? ', ada acara' : ''));
        button.setAttribute('aria-pressed', String(keyForDay === selectedKey));
        button.addEventListener('click', function () { selected = date; render(); });
        grid.appendChild(button);
      })(number);
      var list = byId('calendarEvents'); list.replaceChildren();
      var forDay = records.filter(function (e) { return e.date === selectedKey; });
      if (!forDay.length) {
        var emptyText = doc.createElement('p'); emptyText.className = 'clock-muted'; emptyText.textContent = 'Belum ada acara pada tanggal ini.'; list.appendChild(emptyText);
      }
      forDay.forEach(function (event) {
        var row = doc.createElement('div'); row.className = 'clock-entry calendar-event';
        var name = doc.createElement('span'); name.textContent = (event.time ? event.time + ' · ' : '') + event.title;
        var remove = doc.createElement('button'); remove.type = 'button'; remove.className = 'clock-action'; remove.textContent = 'Hapus';
        remove.setAttribute('aria-label', 'Hapus acara ' + event.title);
        remove.addEventListener('click', function () {
          if (!win.confirm('Hapus acara "' + event.title + '"?')) return;
          records = records.filter(function (item) { return item.id !== event.id; }); persist(); render();
        });
        row.appendChild(name); row.appendChild(remove); list.appendChild(row);
      });
    }
    byId('calendarPrev').addEventListener('click', function () { selected = shiftMonth(selected, -1); render(); });
    byId('calendarNext').addEventListener('click', function () { selected = shiftMonth(selected, 1); render(); });
    byId('calendarToday').addEventListener('click', function () { selected = today(); render(); });
    byId('calendarAdd').addEventListener('click', function () { form.hidden = !form.hidden; error.hidden = true; if (!form.hidden) byId('calendarTitle').focus(); });
    byId('calendarCancel').addEventListener('click', function () { form.hidden = true; error.hidden = true; });
    form.addEventListener('submit', function (event) {
      event.preventDefault();
      var title = byId('calendarTitle').value.trim(), time = byId('calendarTime').value.trim();
      if (!validTitle(title) || !validTime(time)) { error.textContent = 'Isi nama acara dan gunakan waktu HH:mm yang valid.'; error.hidden = false; return; }
      var id;
      do { id = Date.now().toString(36) + '-' + Math.random().toString(36).slice(2); }
      while (records.some(function (item) { return item.id === id; }));
      records.push({id:id, date:dateKey(selected), title:title, time:time});
      persist(); form.reset(); form.hidden = true; error.hidden = true; render();
    });
    render();
  }
  return {daysInMonth:daysInMonth, leadingDays:leadingDays, shiftMonth:shiftMonth, dateKey:dateKey,
    validTime:validTime, validTitle:validTitle, validDate:validDate, mount:mount};
});
