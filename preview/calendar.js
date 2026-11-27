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
  function asDate(date) { return new Date(date.year, date.month-1, date.day, 12); }
  function fromDate(d) { return {year:d.getFullYear(),month:d.getMonth()+1,day:d.getDate()}; }
  function shiftWeek(date, delta) {
    var d=asDate(date); d.setDate(d.getDate()+delta*7);
    if (d.getFullYear()<1900) return {year:1900,month:1,day:1};
    if (d.getFullYear()>2100) return {year:2100,month:12,day:31};
    return fromDate(d);
  }
  function weekDays(date) {
    var d=asDate(date); d.setDate(d.getDate()-(d.getDay()+6)%7);
    return Array.from({length:7}, function (_, i) {
      var day=new Date(d.getFullYear(),d.getMonth(),d.getDate()+i,12);
      return day.getFullYear()<1900 || day.getFullYear()>2100 ? null : fromDate(day);
    });
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
  // Untimed events follow timed events on the same date; stable order for ties.
  function agendaEvents(records, selected, actualToday, expanded) {
    var days = expanded ? [dateKey(actualToday)] : weekDays(selected).filter(Boolean).map(dateKey);
    return records.filter(function (event) { return days.indexOf(event.date) !== -1; }).slice().sort(function (a, b) {
      return a.date.localeCompare(b.date) || (a.time || '99:99').localeCompare(b.time || '99:99');
    });
  }
  function holidaysFor(key) { return holidayData && holidayData.entries && holidayData.entries[key] || []; }
  function holidayText(entry) {
    var kind = entry.type === 'libur' ? 'Libur nasional' : entry.type === 'cuti' ? 'Cuti bersama' : 'Peringatan (bukan libur nasional)';
    return kind + ' · ' + entry.name;
  }
  function mount(win) {
    var doc = win.document, byId = function (id) { return doc.getElementById(id); };
    var grid = byId('calendarGrid'), form = byId('calendarForm'), selected = today(), records = [], expanded = false;
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
    var theme = byId('calendarTheme'), themeIcon = byId('calendarThemeIcon'), themeMenu = byId('calendarThemeMenu');
    var calendarPage = byId('calendarPage'), media = win.matchMedia ? win.matchMedia('(prefers-color-scheme: dark)') : null;
    var themeChoice = 'system', names = {system:'Sistem', light:'Terang', dark:'Gelap'};
    var options = {system:byId('calendarThemeSystem'), light:byId('calendarThemeLight'), dark:byId('calendarThemeDark')};
    try { var savedTheme = win.localStorage.getItem('gt30-calendar-theme'); if (Object.prototype.hasOwnProperty.call(names, savedTheme)) themeChoice = savedTheme; } catch (ignored) {}
    function applyTheme() {
      var dark = themeChoice === 'dark' || (themeChoice === 'system' && media && media.matches);
      if (calendarPage) calendarPage.setAttribute('data-theme', dark ? 'dark' : 'light');
      if (themeIcon) themeIcon.setAttribute('class', 'calendar-theme-icon calendar-theme-' + themeChoice);
      if (theme) theme.setAttribute('aria-label', 'Tema: ' + names[themeChoice] + '. Pilih tema kalender');
      Object.keys(options).forEach(function (key) { if (options[key]) options[key].setAttribute('aria-pressed', String(key === themeChoice)); });
    }
    function closeThemeMenu() { if (themeMenu) themeMenu.hidden = true; if (theme) theme.setAttribute('aria-expanded','false'); }
    if (theme && themeMenu) theme.addEventListener('click', function () { themeMenu.hidden = !themeMenu.hidden; theme.setAttribute('aria-expanded', String(!themeMenu.hidden)); });
    Object.keys(options).forEach(function (key) {
      if (!options[key]) return;
      options[key].addEventListener('click', function () {
        themeChoice = key; try { win.localStorage.setItem('gt30-calendar-theme', themeChoice); } catch (ignored) {}
        applyTheme(); closeThemeMenu(); if (theme) theme.focus();
      });
    });
    function themeEscape(event) {
      if (event.key === 'Escape' && themeMenu && !themeMenu.hidden) { event.preventDefault(); event.stopPropagation(); closeThemeMenu(); if (theme) theme.focus(); }
    }
    if (themeMenu) themeMenu.addEventListener('keydown', themeEscape);
    if (theme) theme.addEventListener('keydown', themeEscape);
    if (doc.addEventListener) doc.addEventListener('click', function (event) {
      if (themeMenu && !themeMenu.hidden && theme && theme.parentNode && !theme.parentNode.contains(event.target)) closeThemeMenu();
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
      byId('calendarMonth').textContent = months[selected.month-1];
      byId('calendarYear').textContent = String(selected.year);
      byId('calendarYear').setAttribute('aria-label', 'Tahun ' + selected.year);
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
      var atMin = expanded ? selected.year === 1900 && selected.month === 1 : dateKey(selected) === '1900-01-01';
      var atMax = expanded ? selected.year === 2100 && selected.month === 12 : dateKey(selected) === '2100-12-31';
      byId('calendarPrev').disabled = atMin; byId('calendarNext').disabled = atMax;
      byId('calendarPrev').textContent = expanded ? 'Bulan sebelumnya' : 'Minggu sebelumnya';
      byId('calendarNext').textContent = expanded ? 'Bulan berikutnya' : 'Minggu berikutnya';
      var toggle = byId('calendarExpand');
      toggle.setAttribute('aria-expanded', String(expanded));
      toggle.setAttribute('aria-label', expanded ? 'Tampilkan satu minggu' : 'Tampilkan satu bulan');
      toggle.title = expanded ? 'Tampilkan satu minggu' : 'Tampilkan satu bulan';
      grid.setAttribute('aria-label', expanded ? 'Tanggal bulan ini. Geser untuk mengganti bulan.' : 'Tanggal minggu ini. Geser untuk mengganti minggu.');
      grid.replaceChildren();
      weekdays.forEach(function (name, index) {
        var day = doc.createElement('span'); day.className = 'calendar-weekday'; day.classList.toggle('weekend', index >= 5); day.textContent = name; grid.appendChild(day);
      });
      var slots = [];
      if (expanded) {
        for (var empty=0; empty<leadingDays(selected.year,selected.month); empty++) slots.push(null);
        for (var number=1; number<=daysInMonth(selected.year,selected.month); number++) slots.push({year:selected.year,month:selected.month,day:number});
      } else slots = weekDays(selected);
      slots.forEach(function (date, index) {
        if (!date) { var spacer=doc.createElement('span'); spacer.setAttribute('aria-hidden','true'); grid.appendChild(spacer); return; }
        var keyForDay = dateKey(date), has = records.some(function (e) { return e.date === keyForDay; });
        var dayHolidays = holidaysFor(keyForDay), isHoliday = dayHolidays.some(function (entry) { return entry.type === 'libur'; });
        var isObservance = dayHolidays.some(function (entry) { return entry.type === 'peringatan'; });
        var button = doc.createElement('button'); button.type = 'button'; button.className = 'calendar-day';
        button.textContent = String(date.day);
        button.classList.toggle('weekend', index % 7 >= 5);
        button.classList.toggle('holiday', isHoliday);
        button.classList.toggle('marker-holiday', isHoliday);
        button.classList.toggle('marker-observance', isObservance);
        button.classList.toggle('marker-event', has);
        button.classList.toggle('today', keyForDay === todayKey);
        button.classList.toggle('selected', keyForDay === selectedKey);
        button.setAttribute('aria-label', label(date) + (dayHolidays.length ? ', ' + dayHolidays.map(holidayText).join(', ') : '') + (has ? ', ada acara' : ''));
        button.setAttribute('aria-pressed', String(keyForDay === selectedKey));
        button.addEventListener('click', function () { selected = date; discardForm(); render(); });
        grid.appendChild(button);
      });
      var list = byId('calendarEvents'); list.replaceChildren();
      var forDay = agendaEvents(records, selected, today(), expanded);
      byId('calendarAgendaHeading').textContent = expanded ? 'ACARA HARI INI' : 'ACARA MINGGU TERPILIH';
      if (!forDay.length) {
        var emptyText = doc.createElement('p'); emptyText.className = 'clock-muted'; emptyText.textContent = expanded ? 'Belum ada acara hari ini.' : 'Belum ada acara minggu ini.'; list.appendChild(emptyText);
      }
      forDay.forEach(function (event) {
        var row = doc.createElement('div'); row.className = 'clock-entry calendar-event';
        var name = doc.createElement('span');
        var when = Number(event.date.slice(8)) + ' ' + months[Number(event.date.slice(5,7))-1].slice(0,3) + ' ' + event.date.slice(0,4);
        name.textContent = when + (event.time ? ' · ' + event.time : '') + '\n' + event.title;
        var remove = doc.createElement('button'); remove.type = 'button'; remove.className = 'clock-action'; remove.textContent = 'Hapus';
        remove.setAttribute('aria-label', 'Hapus acara ' + event.title);
        remove.addEventListener('click', function () {
          if (!win.confirm('Hapus acara "' + event.title + '"?')) return;
          records = records.filter(function (item) { return item.id !== event.id; }); persist(); render();
        });
        row.appendChild(name); row.appendChild(remove); list.appendChild(row);
      });
    }
    function changePeriod(delta) { selected = expanded ? shiftMonth(selected, delta) : shiftWeek(selected, delta); discardForm(); render(); }
    win.calendarOnOpen = function () { selected = today(); expanded = false; discardForm(); closeThemeMenu(); closeMonthMenu(); render(); if (updater) updater.refresh(true); };
    byId('calendarExpand').addEventListener('click', function () { expanded = !expanded; closeMonthMenu(); render(); });
    byId('calendarRefresh').addEventListener('click', function () { if (updater) updater.refresh(true); });
    var monthMenu = byId('calendarMonthMenu');
    if (monthMenu) byId('calendarMonth').addEventListener('click', function () { monthMenu.hidden = !monthMenu.hidden; this.setAttribute('aria-expanded', String(!monthMenu.hidden)); });
    function closeMonthMenu() { if (monthMenu) { monthMenu.hidden = true; byId('calendarMonth').setAttribute('aria-expanded','false'); } }
    byId('calendarPrev').addEventListener('click', function () { closeMonthMenu(); changePeriod(-1); });
    byId('calendarNext').addEventListener('click', function () { closeMonthMenu(); changePeriod(1); });
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
      gesture = null; if (direction) { closeMonthMenu(); changePeriod(direction); }
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
    var lastToday = dateKey(today());
    win.setInterval(function () { var current = dateKey(today()); if (current !== lastToday) { lastToday = current; render(); } }, 60000);
    render();
  }
  return {daysInMonth:daysInMonth, leadingDays:leadingDays, shiftMonth:shiftMonth, dateKey:dateKey,
    validTime:validTime, validTitle:validTitle, validDate:validDate, agendaEvents:agendaEvents, holidaysFor:holidaysFor, shiftWeek:shiftWeek, weekDays:weekDays, swipeDirection:swipeDirection, mount:mount};
});
