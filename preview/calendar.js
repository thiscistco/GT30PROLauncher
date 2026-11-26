/* Kalender preview: tanggal lokal dan acara lokal browser, tanpa pengingat. */
(function (root, factory) {
  var data = typeof module !== 'undefined' && module.exports ? require('./holiday-data.js') : (root && root.indonesiaHolidays);
  var updater = typeof module !== 'undefined' && module.exports ? require('./holiday-update.js') : (root && root.holidayUpdater);
  var logic = factory(data, updater);
  if (typeof module !== 'undefined' && module.exports) module.exports = logic;
  if (root && root.document) logic.mount(root);
})(typeof window !== 'undefined' ? window : null, function (holidayData, updaterApi) {
  'use strict';
  var months = ['Januari','Februari','Maret','April','Mei','Juni','Juli','Agustus','September','Oktober','November','Desember'];
  var weekdays = ['Sen','Sel','Rab','Kam','Jum','Sab','Min'];
  function daysInMonth(year, month) { return new Date(year, month, 0).getDate(); }
  function leadingDays(year, month) { return (new Date(year, month - 1, 1).getDay() + 6) % 7; }
  function shiftMonth(date, delta) {
    var index = Math.max(1900 * 12, Math.min(2100 * 12 + 11, date.year * 12 + date.month - 1 + delta));
    var year = Math.floor(index / 12), month = index % 12 + 1;
    return {year: year, month: month, day: Math.min(date.day, daysInMonth(year, month))};
  }
  function swipeDirection(dx, dy, threshold) { return Math.abs(dx) < threshold || Math.abs(dx) <= Math.abs(dy) * 1.5 ? 0 : (dx < 0 ? 1 : -1); }
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
  function holidaysFor(key) { return holidayData && holidayData.entries && holidayData.entries[key] || []; }
  function holidayText(entry) {
    var kind = entry.type === 'libur' ? 'Libur nasional' : entry.type === 'cuti' ? 'Cuti bersama' : 'Peringatan (bukan libur nasional)';
    return kind + ' · ' + entry.name;
  }
  function mount(win) {
    var doc = win.document, byId = function (id) { return doc.getElementById(id); };
    var grid = byId('calendarGrid'), form = byId('calendarForm'), selected = today(), records = [];
    var key = 'gt30-calendar-events-v1', writable = true;
    var updater = null, updateInfo = {message:'Data bawaan; belum diperiksa secara online.', checkedAt:0, pending:false};
    try {
      var holidayStorage; try { holidayStorage = win.localStorage; } catch (ignored) { holidayStorage = null; }
      updater = updaterApi.create({initial:holidayData, storage:holidayStorage, now:Date.now,
        fetch:function (url, options) { return win.fetch(url, options); },
        atob:function (value) { return win.atob(value); },
        makeAbort:win.AbortController ? function () { return new win.AbortController(); } : null,
        onChange:function (state) { updateInfo = state; holidayData = state.data; render(); }});
      updateInfo = updater.getState(); holidayData = updateInfo.data;
    } catch (ignored) { updater = null; }
    var note = byId('calendarStorageNote'), error = byId('calendarError');
    var theme = byId('calendarTheme'), calendarPage = byId('calendarPage'), media = win.matchMedia ? win.matchMedia('(prefers-color-scheme: dark)') : null;
    var themeChoice = 'system';
    try { var savedTheme = win.localStorage.getItem('gt30-calendar-theme'); if (['system','light','dark'].indexOf(savedTheme) >= 0) themeChoice = savedTheme; } catch (ignored) {}
    function applyTheme() {
      var dark = themeChoice === 'dark' || (themeChoice === 'system' && media && media.matches);
      if (calendarPage) calendarPage.setAttribute('data-theme', dark ? 'dark' : 'light');
      if (theme) theme.value = themeChoice;
    }
    if (theme) theme.addEventListener('change', function () {
      themeChoice = ['system','light','dark'].indexOf(theme.value) >= 0 ? theme.value : 'system';
      try { win.localStorage.setItem('gt30-calendar-theme', themeChoice); } catch (ignored) {}
      applyTheme();
    });
    if (media) { if (media.addEventListener) media.addEventListener('change', applyTheme); else if (media.addListener) media.addListener(applyTheme); }
    applyTheme();
    var defaultNote = 'Acara disimpan di browser ini; tidak disinkronkan atau diberi pengingat. Menghapus data browser menghapus acara.';
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
    function discardForm() { form.hidden = true; form.reset(); error.hidden = true; }
    function render() {
      var selectedKey = dateKey(selected), todayKey = dateKey(today());
      byId('calendarMonth').textContent = months[selected.month-1] + ' ' + selected.year;
      byId('calendarMonth').setAttribute('aria-label', 'Pilih bulan. ' + months[selected.month-1] + ' ' + selected.year);
      byId('calendarSelected').textContent = label(selected);
      var dayLabel = byId('calendarDayLabel'); if (dayLabel) dayLabel.textContent = selectedKey === todayKey ? 'HARI INI' : 'TANGGAL DIPILIH';
      var holidayDetails = byId('calendarHolidayDetails'); holidayDetails.replaceChildren();
      holidaysFor(selectedKey).forEach(function (entry) {
        var line = doc.createElement('p'); line.className = 'calendar-holiday-detail' + (entry.type === 'libur' ? ' holiday' : '');
        line.textContent = holidayText(entry); holidayDetails.appendChild(line);
      });
      var supported = holidayData && holidayData.years.indexOf(selected.year) !== -1;
      var availableYears = holidayData && holidayData.years ? holidayData.years.join(', ') : 'tidak tersedia';
      var checked = updateInfo.checkedAt ? ' Terakhir diperiksa: ' + new Date(updateInfo.checkedAt).toLocaleString('id-ID') + '.' : '';
      byId('calendarHolidayStatus').textContent = (supported ? 'Data libur tersedia: ' + availableYears + '. ' : 'Data libur belum tersedia untuk tahun ini; tersedia: ' + availableYears + '. ') +
        updateInfo.message + checked + ' Sumber GitHub dikelola manual; jadwal bisa berubah. Peringatan bukan libur nasional, dan cuti mengikuti aturan instansi.';
      byId('calendarRefresh').disabled = updateInfo.pending || !updater;
      var warning = byId('calendarWarning');
      if (warning) {
        warning.textContent = !writable ? 'Penyimpanan kalender bermasalah; perubahan hanya sementara.' : !supported ? 'Data libur belum tersedia untuk tahun ini.' : updateInfo.message.indexOf('Pembaruan gagal') === 0 ? 'Data libur offline; memakai data tersimpan atau bawaan.' : '';
        warning.hidden = !warning.textContent;
      }
      var monthList = byId('calendarMonthHolidays'); monthList.replaceChildren();
      if (supported) {
        Object.keys(holidayData.entries).filter(function (key) { return key.slice(0, 7) === selectedKey.slice(0, 7); }).sort().forEach(function (key) {
          holidaysFor(key).forEach(function (entry) {
            var line = doc.createElement('p'); line.className = 'calendar-holiday-detail' + (entry.type === 'libur' ? ' holiday' : '');
            line.textContent = Number(key.slice(8)) + ' · ' + holidayText(entry); monthList.appendChild(line);
          });
        });
        if (!monthList.children.length) { var none = doc.createElement('p'); none.className = 'clock-muted'; none.textContent = 'Tidak ada data libur atau peringatan bulan ini.'; monthList.appendChild(none); }
      }
      byId('calendarPrev').disabled = selected.year === 1900 && selected.month === 1;
      byId('calendarNext').disabled = selected.year === 2100 && selected.month === 12;
      grid.replaceChildren();
      weekdays.forEach(function (name, index) {
        var day = doc.createElement('span'); day.className = 'calendar-weekday'; day.classList.toggle('weekend', index >= 5); day.textContent = name; grid.appendChild(day);
      });
      for (var empty = 0; empty < leadingDays(selected.year, selected.month); empty++) {
        var spacer = doc.createElement('span'); spacer.setAttribute('aria-hidden', 'true'); grid.appendChild(spacer);
      }
      var total = daysInMonth(selected.year, selected.month);
      for (var number = 1; number <= total; number++) (function (day) {
        var date = {year:selected.year, month:selected.month, day:day}, keyForDay = dateKey(date);
        var has = records.some(function (e) { return e.date === keyForDay; });
        var dayHolidays = holidaysFor(keyForDay), isHoliday = dayHolidays.some(function (entry) { return entry.type === 'libur'; });
        var isObservance = dayHolidays.some(function (entry) { return entry.type === 'peringatan'; });
        var button = doc.createElement('button'); button.type = 'button'; button.className = 'calendar-day';
        button.textContent = day + (has ? '·' : '');
        button.classList.toggle('weekend', (leadingDays(selected.year, selected.month) + day - 1) % 7 >= 5);
        button.classList.toggle('holiday', isHoliday);
        button.classList.toggle('marker-holiday', isHoliday);
        button.classList.toggle('marker-observance', isObservance);
        button.classList.toggle('today', keyForDay === todayKey);
        button.classList.toggle('selected', keyForDay === selectedKey);
        button.setAttribute('aria-label', label(date) + (dayHolidays.length ? ', ' + dayHolidays.map(holidayText).join(', ') : '') + (has ? ', ada acara' : ''));
        button.setAttribute('aria-pressed', String(keyForDay === selectedKey));
        button.addEventListener('click', function () { selected = date; discardForm(); render(); });
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
    function changeMonth(delta) { selected = shiftMonth(selected, delta); discardForm(); render(); }
    win.calendarOnOpen = function () { selected = today(); discardForm(); render(); if (updater) updater.refresh(true); };
    byId('calendarRefresh').addEventListener('click', function () { if (updater) updater.refresh(true); });
    var monthMenu = byId('calendarMonthMenu');
    if (monthMenu) byId('calendarMonth').addEventListener('click', function () { monthMenu.hidden = !monthMenu.hidden; this.setAttribute('aria-expanded', String(!monthMenu.hidden)); });
    function closeMonthMenu() { if (monthMenu) { monthMenu.hidden = true; byId('calendarMonth').setAttribute('aria-expanded','false'); } }
    byId('calendarPrev').addEventListener('click', function () { closeMonthMenu(); changeMonth(-1); });
    byId('calendarNext').addEventListener('click', function () { closeMonthMenu(); changeMonth(1); });
    var gesture = null, suppressClick = false;
    grid.addEventListener('pointerdown', function (e) {
      if (e.isPrimary === false || (e.button != null && e.button !== 0)) { gesture = null; return; }
      var screen = doc.querySelector ? doc.querySelector('.screen') : null, rect = screen && screen.getBoundingClientRect();
      // The right-edge back gesture has priority over calendar swiping.
      if (rect && rect.width > 0 && e.clientX >= rect.right - rect.width * (32 / 445)) { gesture = null; return; }
      var width = rect && rect.width > 0 ? rect.width : 445;
      gesture = {id:e.pointerId,x:e.clientX,y:e.clientY,width:width,moving:false};
    });
    grid.addEventListener('pointermove', function (e) {
      if (!gesture || e.pointerId !== gesture.id) return;
      if (!gesture.moving && swipeDirection(e.clientX-gesture.x,e.clientY-gesture.y,gesture.width*(36/445))) {
        gesture.moving = true;
        if (grid.setPointerCapture) try { grid.setPointerCapture(e.pointerId); } catch (ignored) {}
      }
      if (gesture.moving && e.cancelable) e.preventDefault();
    });
    grid.addEventListener('pointerup', function (e) {
      if (!gesture || e.pointerId !== gesture.id) return;
      var direction = gesture.moving ? swipeDirection(e.clientX-gesture.x,e.clientY-gesture.y,gesture.width*(48/445)) : 0;
      if (gesture.moving) { suppressClick = true; win.setTimeout(function () { suppressClick = false; }, 0); }
      gesture = null; if (direction) { closeMonthMenu(); changeMonth(direction); }
    });
    grid.addEventListener('pointercancel', function () { gesture = null; });
    grid.addEventListener('click', function (e) { if (suppressClick) { e.preventDefault(); e.stopPropagation(); suppressClick = false; } }, true);
    byId('calendarToday').addEventListener('click', function () { selected = today(); discardForm(); render(); });
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
    validTime:validTime, validTitle:validTitle, validDate:validDate, holidaysFor:holidaysFor, swipeDirection:swipeDirection, mount:mount};
});
