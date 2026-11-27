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
  function validRange(date, time, endDate, endTime) {
    if (!validDate(date) || !validDate(endDate) || !validTime(time) || !time || !validTime(endTime) || !endTime) return false;
    return date + 'T' + time < endDate + 'T' + endTime && dayNumber(endDate)-dayNumber(date) <= 366;
  }
  function dayNumber(key) { return Math.floor(Date.UTC(+key.slice(0,4),+key.slice(5,7)-1,+key.slice(8,10))/86400000); }
  function fromKey(key) { return {year:+key.slice(0,4),month:+key.slice(5,7),day:+key.slice(8,10)}; }
  function nextOccurrence(startKey, fromKeyValue, unit, interval) {
    if (fromKeyValue < startKey) fromKeyValue = startKey;
    if (unit === 'none') return fromKeyValue === startKey ? startKey : null;
    if (!Number.isInteger(interval) || interval < 1 || interval > 999) return null;
    var start=fromKey(startKey), from=fromKey(fromKeyValue), delta;
    if (unit === 'day') delta=dayNumber(fromKeyValue)-dayNumber(startKey);
    else if (unit === 'week') delta=Math.floor((dayNumber(fromKeyValue)-dayNumber(startKey))/7);
    else if (unit === 'month') delta=(from.year-start.year)*12+from.month-start.month;
    else if (unit === 'year') delta=from.year-start.year;
    else return null;
    var n=Math.max(0,Math.floor(delta/interval));
    for (var i=0;i<2;i++,n++) {
      var shift=n*interval, result;
      if (unit === 'day' || unit === 'week') {
        var d=asDate(start); d.setDate(d.getDate()+shift*(unit === 'week'?7:1)); result=fromDate(d);
      } else {
        var index=start.year*12+start.month-1+shift*(unit === 'year'?12:1);
        var year=Math.floor(index/12), month=index%12+1;
        result={year:year,month:month,day:Math.min(start.day,daysInMonth(year,month))};
      }
      if (result.year>2100) return null;
      var key=dateKey(result); if (key>=fromKeyValue) return key;
    }
    return null;
  }
  function addDays(key, days) { var d=asDate(fromKey(key)); d.setDate(d.getDate()+days); return dateKey(fromDate(d)); }
  var maxReminderMinutes=999*1440;
  function validReminder(value) { return Number.isInteger(value) && value >= -1 && value <= maxReminderMinutes; }
  function reminderLabel(value) {
    if (value === 0) return 'Saat mulai';
    if (value % 1440 === 0) return value/1440 + ' hari sebelum';
    if (value % 60 === 0) return value/60 + ' jam sebelum';
    return value + ' menit sebelum';
  }
  function validEvent(e) {
    if (!e || typeof e.id !== 'string' || !e.id.length || !validDate(e.date) || !validTitle(e.title) || typeof e.time !== 'string' || !validTime(e.time)) return false;
    if (!('endDate' in e)) return true; // v1 legacy record, including untimed events
    return validRange(e.date,e.time,e.endDate,e.endTime) && ['Pribadi','Pekerjaan','Keluarga','Lainnya'].includes(e.type) &&
      ['none','day','week','month','year'].includes(e.repeat) && Number.isInteger(e.interval) && e.interval >= 1 && e.interval <= 999 && validReminder(e.reminder);
  }
  function occurrences(records, first, last) {
    var found=[];
    records.forEach(function (e) {
      var duration=e.endDate ? dayNumber(e.endDate)-dayNumber(e.date) : 0;
      var activeDays=duration > 0 && e.endTime === '00:00' ? duration-1 : duration;
      var current=nextOccurrence(e.date,addDays(first,-activeDays),e.repeat || 'none',e.interval || 1);
      while (current && current <= last) {
        if (addDays(current,activeDays) >= first) found.push({event:e,date:current,endDate:addDays(current,duration)});
        current=nextOccurrence(e.date,addDays(current,1),e.repeat || 'none',e.interval || 1);
      }
    });
    return found.sort(function (a,b) { return a.date.localeCompare(b.date) || (a.event.time||'99:99').localeCompare(b.event.time||'99:99'); });
  }
  function agendaOccurrences(records, selected, actualToday, expanded) {
    var days=expanded ? [actualToday] : weekDays(selected).filter(Boolean);
    return occurrences(records,dateKey(days[0]),dateKey(days[days.length-1]));
  }
  function agendaEvents(records, selected, actualToday, expanded) { return agendaOccurrences(records, selected, actualToday, expanded).map(function (o) { return o.event; }); }
  function holidaysFor(key) { return holidayData && holidayData.entries && holidayData.entries[key] || []; }
  function weekHolidayEntries(date) {
    var result=[];
    weekDays(date).filter(Boolean).forEach(function (day) {
      holidaysFor(dateKey(day)).forEach(function (entry) { result.push({date:day,entry:entry}); });
    });
    return result;
  }
  function holidayKind(entry) { return entry.type === 'libur' ? 'Libur nasional' : entry.type === 'cuti' ? 'Cuti bersama' : 'Peringatan'; }
  function holidayText(entry) {
    return holidayKind(entry) + ' · ' + entry.name;
  }
  function marqueeMetrics(available, width) {
    if (!Number.isFinite(available) || !Number.isFinite(width) || available <= 0 || width <= available + 1) return null;
    var distance=Math.ceil(width-available);
    return {distance:distance, duration:Math.max(5,Math.min(30,2 + distance/24))};
  }
  function updateMarquee(box, track) {
    var metrics=marqueeMetrics(box.clientWidth,track.offsetWidth);
    box.classList.toggle('is-marquee',!!metrics);
    if (metrics) {
      box.style.setProperty('--marquee-distance','-' + metrics.distance + 'px');
      box.style.setProperty('--marquee-duration',metrics.duration + 's');
    }
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
      ['clock','weather','super','drawer','timetable'].forEach(function (prefix) {
        var appPage=byId(prefix+'Page'), appIcon=byId(prefix+'ThemeIcon'), appTheme=byId(prefix+'Theme');
        if (appPage) appPage.setAttribute('data-theme', dark ? 'dark' : 'light');
        if (appIcon) appIcon.setAttribute('class','calendar-theme-icon calendar-theme-'+themeChoice);
        if (appTheme) appTheme.setAttribute('aria-label','Tema aplikasi: '+names[themeChoice]);
        Object.keys(names).forEach(function (key) { var option=byId(prefix+'Theme'+key.charAt(0).toUpperCase()+key.slice(1)); if (option) option.setAttribute('aria-pressed',String(key===themeChoice)); });
      });
      if (themeIcon) themeIcon.setAttribute('class', 'calendar-theme-icon calendar-theme-' + themeChoice);
      if (theme) theme.setAttribute('aria-label', 'Tema: ' + names[themeChoice] + '. Pilih tema aplikasi');
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
    // Reuse the same choice across all built-in pages, without changing the home wallpaper.
    ['clock','weather','super','drawer','timetable'].forEach(function (prefix) {
      var appTheme=byId(prefix+'Theme'), menu=byId(prefix+'ThemeMenu');
      if (!appTheme || !menu) return;
      function close() { menu.hidden=true; appTheme.setAttribute('aria-expanded','false'); }
      appTheme.addEventListener('click',function () { menu.hidden=!menu.hidden; appTheme.setAttribute('aria-expanded',String(!menu.hidden)); });
      Object.keys(names).forEach(function (key) {
        var option=byId(prefix+'Theme'+key.charAt(0).toUpperCase()+key.slice(1)); if (!option) return;
        option.addEventListener('click',function () {
          themeChoice=key; try { win.localStorage.setItem('gt30-calendar-theme',key); } catch (ignored) {}
          applyTheme(); close(); appTheme.focus();
        });
      });
      function escape(event) { if (event.key==='Escape' && !menu.hidden) { event.preventDefault(); event.stopPropagation(); close(); appTheme.focus(); } }
      menu.addEventListener('keydown',escape); appTheme.addEventListener('keydown',escape);
      if (doc.addEventListener) doc.addEventListener('click',function (event) { if (!menu.hidden && appTheme.parentNode && !appTheme.parentNode.contains(event.target)) close(); });
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
    var defaultNote = 'Acara disimpan di browser ini tanpa sinkronisasi. Preview tidak mengirim pengingat; notifikasi tersedia pada aplikasi Android. Menghapus data browser menghapus acara.';
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
    var marqueeNodes=[], marqueeFramePending=false;
    function measureMarquees() {
      marqueeFramePending=false;
      marqueeNodes.forEach(function (item) {
        updateMarquee(item.box,item.track);
      });
    }
    function scheduleMarqueeMeasure() {
      if (marqueeFramePending) return;
      if (win.requestAnimationFrame) { marqueeFramePending=true; win.requestAnimationFrame(measureMarquees); }
      else measureMarquees();
    }
    var marqueeObserver=win.ResizeObserver ? new win.ResizeObserver(scheduleMarqueeMeasure) : null;
    if (win.addEventListener) win.addEventListener('resize',scheduleMarqueeMeasure);
    if (doc.fonts && doc.fonts.ready) doc.fonts.ready.then(scheduleMarqueeMeasure);
    if (doc.fonts && doc.fonts.addEventListener) doc.fonts.addEventListener('loadingdone',scheduleMarqueeMeasure);
    function discardForm() { form.hidden = true; form.reset(); error.hidden = true; }
    function makeEntry(kind, name, info, holidayType) {
      var row=doc.createElement('div'); row.className='clock-entry calendar-event' + (holidayType ? ' calendar-holiday-entry' : '') + (holidayType==='libur' ? ' holiday' : '');
      var main=doc.createElement('button'); main.type='button'; main.className='calendar-entry-main';
      var first=doc.createElement('span'); first.className='calendar-entry-kind'; first.textContent=kind;
      var second=doc.createElement('span'); second.className='calendar-entry-name';
      var track=doc.createElement('span'); track.className='calendar-entry-name-track'; track.textContent=name; second.appendChild(track);
      marqueeNodes.push({box:second,track:track});
      main.appendChild(first); main.appendChild(second);
      main.setAttribute('aria-label', kind + ', ' + name + '. Lihat rincian');
      main.addEventListener('click',function () { win.alert(name + '\n' + info); });
      row.appendChild(main); return row;
    }
    function holidayRow(item) {
      var kind=holidayKind(item.entry), date=label(item.date);
      return makeEntry(kind + ' · ' + date, item.entry.name, kind + '\n' + date, item.entry.type);
    }
    function render() {
      if (marqueeObserver) marqueeObserver.disconnect();
      marqueeNodes=[];
      var selectedKey = dateKey(selected), todayKey = dateKey(today());
      byId('calendarMonthLabel').textContent = months[selected.month-1];
      byId('calendarYear').textContent = String(selected.year);
      byId('calendarYear').setAttribute('aria-label', 'Tahun ' + selected.year);
      byId('calendarMonth').setAttribute('aria-label', months[selected.month-1] + ' ' + selected.year + '. Ketuk untuk hari ini, tekan lama untuk navigasi kalender');
      byId('calendarSelected').textContent = label(selected);
      var dayLabel = byId('calendarDayLabel'); if (dayLabel) dayLabel.textContent = selectedKey === todayKey ? 'HARI INI' : 'TANGGAL DIPILIH';
      var holidayDetails = byId('calendarHolidayDetails'); holidayDetails.replaceChildren();
      // A selected date in month view retains its own holiday detail; the agenda below remains today-only.
      if (expanded && selectedKey !== todayKey) holidaysFor(selectedKey).forEach(function (entry) {
        holidayDetails.appendChild(holidayRow({date:selected,entry:entry}));
      });
      var supported = holidayData && holidayData.years.indexOf(selected.year) !== -1;
      var availableYears = holidayData && holidayData.years ? holidayData.years.join(', ') : 'tidak tersedia';
      var checked = updateInfo.checkedAt ? ' Terakhir diperiksa: ' + new Date(updateInfo.checkedAt).toLocaleString('id-ID') + '.' : '';
      byId('calendarHolidayStatus').textContent = (supported ? 'Data libur tersedia: ' + availableYears + '. ' : 'Data libur belum tersedia untuk tahun ini; tersedia: ' + availableYears + '. ') +
        updateInfo.message + checked + ' Sumber GitHub dikelola manual; jadwal bisa berubah. Cuti mengikuti aturan instansi.';
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
      grid.setAttribute('data-expanded',String(expanded));
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
        var keyForDay = dateKey(date), has = occurrences(records,keyForDay,keyForDay).length > 0;
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
      var holidayItems = expanded ? holidaysFor(todayKey).map(function (entry) { return {date:fromKey(todayKey),entry:entry}; }) : weekHolidayEntries(selected);
      var forDay = agendaOccurrences(records, selected, today(), expanded);
      var todayAgendaLabel=byId('calendarTodayAgendaLabel');
      todayAgendaLabel.hidden=!(expanded && selectedKey !== todayKey && (holidayItems.length || forDay.length));
      todayAgendaLabel.textContent=todayAgendaLabel.hidden ? '' : 'HARI INI · ' + label(fromKey(todayKey));
      holidayItems.forEach(function (item) { list.appendChild(holidayRow(item)); });
      forDay.forEach(function (occurrence) {
        var event=occurrence.event;
        var d=occurrence.date, end=occurrence.endDate;
        var when = Number(d.slice(8)) + ' ' + months[Number(d.slice(5,7))-1].slice(0,3) + ' ' + d.slice(0,4) + (event.time ? ' · ' + event.time : '');
        if (event.endDate) when += ' – ' + Number(end.slice(8)) + ' ' + months[Number(end.slice(5,7))-1].slice(0,3) + ' ' + end.slice(0,4) + ' · ' + event.endTime;
        var details='';
        if (event.endDate) {
          if (event.repeat !== 'none') details='\nSetiap ' + event.interval + ' ' + {day:'hari',week:'minggu',month:'bulan',year:'tahun'}[event.repeat];
          if (event.reminder >= 0) details += (details ? ' · ' : '\n') + reminderLabel(event.reminder);
        }
        var kind=event.type || 'Acara';
        var row = makeEntry(kind + ' · ' + when, event.title, kind + '\n' + when + details, null);
        var remove = doc.createElement('button'); remove.type = 'button'; remove.className = 'clock-action'; remove.textContent = 'Hapus';
        remove.setAttribute('aria-label', 'Hapus acara ' + event.title);
        remove.addEventListener('click', function () {
          if (!win.confirm('Hapus acara "' + event.title + '"? Semua pengulangan juga akan dihapus.')) return;
          records = records.filter(function (item) { return item.id !== event.id; }); persist(); render();
        });
        row.appendChild(remove); list.appendChild(row);
      });
      if (marqueeObserver) marqueeNodes.forEach(function (item) { marqueeObserver.observe(item.box); });
      scheduleMarqueeMeasure();
    }
    function animateGrid(kind) {
      if (typeof grid.animate !== 'function' || (win.matchMedia && win.matchMedia('(prefers-reduced-motion: reduce)').matches)) return;
      grid.animate([{opacity:0,transform:kind===2?'translateY(-8px)':'translateX('+(kind*20)+'px)'},{opacity:1,transform:'translate(0,0)'}],{duration:220,easing:'ease-out'});
    }
    function changePeriod(delta) { selected = expanded ? shiftMonth(selected, delta) : shiftWeek(selected, delta); discardForm(); render(); animateGrid(delta); }
    function toggleExpanded() { expanded=!expanded; closeMonthMenu(); render(); animateGrid(2); }
    win.calendarOnOpen = function () { selected = today(); expanded = false; discardForm(); closeThemeMenu(); closeMonthMenu(); render(); if (updater) updater.refresh(true); };
    byId('calendarExpand').addEventListener('click', toggleExpanded);
    byId('calendarRefresh').addEventListener('click', function () { if (updater) updater.refresh(true); });
    var monthMenu = byId('calendarMonthMenu'), monthButton=byId('calendarMonth');
    var holdTimer=null, holdOrigin=null, suppressMonthClick=false;
    function openMonthMenu(focusFirst) { if (monthMenu) { monthMenu.hidden=false; monthButton.setAttribute('aria-expanded','true'); if (focusFirst) byId('calendarPrev').focus(); } }
    function closeMonthMenu() { if (monthMenu) { monthMenu.hidden = true; monthButton.setAttribute('aria-expanded','false'); } }
    function cancelMonthHold() { if (holdTimer !== null) { win.clearTimeout(holdTimer); holdTimer=null; } }
    monthButton.addEventListener('click', function (event) {
      if (suppressMonthClick) { suppressMonthClick=false; event.preventDefault(); return; }
      closeMonthMenu(); selected=today(); discardForm(); render();
    });
    monthButton.addEventListener('pointerdown', function (event) {
      if (event.isPrimary===false || (event.button != null && event.button!==0)) return;
      cancelMonthHold(); holdOrigin={x:event.clientX,y:event.clientY};
      holdTimer=win.setTimeout(function () { holdTimer=null; suppressMonthClick=true; openMonthMenu(); },500);
    });
    monthButton.addEventListener('pointermove', function (event) {
      if (holdOrigin && (Math.abs(event.clientX-holdOrigin.x)>10 || Math.abs(event.clientY-holdOrigin.y)>10)) cancelMonthHold();
    });
    function endMonthHold() {
      cancelMonthHold(); holdOrigin=null;
      if (suppressMonthClick) win.setTimeout(function () { suppressMonthClick=false; },0);
    }
    monthButton.addEventListener('pointerup',endMonthHold);
    monthButton.addEventListener('pointercancel',endMonthHold);
    monthButton.addEventListener('contextmenu',function (event) { event.preventDefault(); cancelMonthHold(); if (holdOrigin) suppressMonthClick=true; openMonthMenu(); });
    monthButton.addEventListener('keydown',function (event) {
      if (event.key==='ContextMenu' || (event.shiftKey && event.key==='F10')) { event.preventDefault(); openMonthMenu(true); }
      if (event.key==='Escape' && monthMenu && !monthMenu.hidden) { event.preventDefault(); event.stopPropagation(); closeMonthMenu(); monthButton.focus(); }
    });
    if (monthMenu) monthMenu.addEventListener('keydown',function (event) {
      if (event.key==='Escape' && !monthMenu.hidden) { event.preventDefault(); event.stopPropagation(); closeMonthMenu(); monthButton.focus(); }
    });
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
      var dx=e.clientX-gesture.x, dy=e.clientY-gesture.y;
      if (!gesture.moving && (swipeDirection(dx,dy,gesture.width*(36/445)) || (!expanded && dy>=gesture.width*(36/445) && dy>Math.abs(dx)*1.5))) {
        gesture.moving = true;
        if (grid.setPointerCapture) try { grid.setPointerCapture(e.pointerId); } catch (ignored) {}
      }
      if (gesture.moving && e.cancelable) e.preventDefault();
    });
    grid.addEventListener('pointerup', function (e) {
      if (!gesture || e.pointerId !== gesture.id) return;
      var dx=e.clientX-gesture.x, dy=e.clientY-gesture.y;
      var direction = gesture.moving ? swipeDirection(dx,dy,gesture.width*(48/445)) : 0;
      var expand = gesture.moving && !expanded && dy>=gesture.width*(48/445) && dy>Math.abs(dx)*1.5;
      if (gesture.moving) { suppressClick = true; win.setTimeout(function () { suppressClick = false; }, 0); }
      gesture = null; if (direction) { closeMonthMenu(); changePeriod(direction); } else if (expand) toggleExpanded();
    });
    grid.addEventListener('pointercancel', function () { gesture = null; });
    grid.addEventListener('click', function (e) { if (suppressClick) { e.preventDefault(); e.stopPropagation(); suppressClick = false; } }, true);
    byId('calendarAdd').addEventListener('click', function () {
      form.hidden = !form.hidden; error.hidden = true;
      if (!form.hidden) { byId('calendarStartDate').value=dateKey(selected); byId('calendarEndDate').value=dateKey(selected); byId('calendarTime').value='09:00'; byId('calendarEndTime').value='10:00'; byId('calendarRepeat').value='none'; byId('calendarInterval').value='1'; byId('calendarCustomRepeat').hidden=true; byId('calendarCustomRepeatLabel').hidden=true; byId('calendarReminder').value='-1'; byId('calendarCustomReminder').hidden=true; byId('calendarCustomReminderLabel').hidden=true; byId('calendarReminderAmount').value='1'; byId('calendarTitle').focus(); }
    });
    byId('calendarRepeat').addEventListener('change', function () { byId('calendarCustomRepeat').hidden=byId('calendarCustomRepeatLabel').hidden=this.value !== 'custom'; });
    byId('calendarReminder').addEventListener('change', function () { byId('calendarCustomReminder').hidden=byId('calendarCustomReminderLabel').hidden=this.value !== 'custom'; });
    byId('calendarCancel').addEventListener('click', function () { form.hidden = true; error.hidden = true; });
    form.addEventListener('submit', function (event) {
      event.preventDefault();
      var title = byId('calendarTitle').value.trim(), time = byId('calendarTime').value.trim();
      var start=byId('calendarStartDate').value, end=byId('calendarEndDate').value, endTime=byId('calendarEndTime').value;
      var choice=byId('calendarRepeat').value, interval=choice==='custom' ? Number(byId('calendarInterval').value) : 1;
      var repeat=choice==='custom' ? byId('calendarUnit').value : choice;
      var type=byId('calendarType').value, reminderChoice=byId('calendarReminder').value;
      var amount=Number(byId('calendarReminderAmount').value), factor=Number(byId('calendarReminderUnit').value);
      var customValid=reminderChoice!=='custom' || (Number.isInteger(amount) && amount>=1 && amount<=999 && [1,60,1440].includes(factor));
      var reminder=reminderChoice==='custom' ? amount*factor : Number(reminderChoice);
      if (!validTitle(title) || !validRange(start,time,end,endTime) || !['none','day','week','month','year'].includes(repeat) || !Number.isInteger(interval) || interval<1 || interval>999 || !['Pribadi','Pekerjaan','Keluarga','Lainnya'].includes(type) || !customValid || !validReminder(reminder)) {
        error.textContent = 'Isi nama, waktu mulai dan selesai yang valid (maksimum 366 hari), interval pengulangan dan pengingat khusus 1–999.'; error.hidden = false; return;
      }
      var id;
      do { id = Date.now().toString(36) + '-' + Math.random().toString(36).slice(2); }
      while (records.some(function (item) { return item.id === id; }));
      records.push({id:id, date:start, title:title, time:time, endDate:end, endTime:endTime, type:type, repeat:repeat, interval:interval, reminder:reminder});
      persist(); form.reset(); form.hidden = true; error.hidden = true; render();
    });
    var lastToday = dateKey(today());
    win.setInterval(function () { var current = dateKey(today()); if (current !== lastToday) { lastToday = current; render(); } }, 60000);
    render();
  }
  return {daysInMonth:daysInMonth, leadingDays:leadingDays, shiftMonth:shiftMonth, dateKey:dateKey,
    marqueeMetrics:marqueeMetrics, updateMarquee:updateMarquee, validTime:validTime, validTitle:validTitle, validDate:validDate, validRange:validRange, validReminder:validReminder, reminderLabel:reminderLabel, nextOccurrence:nextOccurrence, occurrences:occurrences, agendaEvents:agendaEvents, holidaysFor:holidaysFor, weekHolidayEntries:weekHolidayEntries, shiftWeek:shiftWeek, weekDays:weekDays, swipeDirection:swipeDirection, mount:mount};
});
