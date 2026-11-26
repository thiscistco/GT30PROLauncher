/* Offline-first, curated holiday feed. Personal events never enter this module. */
(function (root, factory) {
  var api = factory();
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  if (root) root.holidayUpdater = api;
})(typeof window !== 'undefined' ? window : null, function () {
  'use strict';
  var URL = 'https://api.github.com/repos/thiscistco/GT30PROLauncher/contents/assets/calendar/indonesia.json?ref=arena%2Ff57090c0-gt30prolauncher';
  var CACHE_KEY = 'gt30-calendar-holidays-v1', INTERVAL = 12 * 60 * 60 * 1000, MAX_BYTES = 512 * 1024;
  function valid(data) {
    if (!data || !Number.isSafeInteger(data.revision) || data.revision < 1 || !Array.isArray(data.years) || !data.years.length || data.years.length > 100 || !data.entries || typeof data.entries !== 'object' || Array.isArray(data.entries)) return false;
    var years = new Set(data.years), seen = new Set();
    if (years.size !== data.years.length || data.years.some(function (y) { return !Number.isInteger(y) || y < 1900 || y > 2100; })) return false;
    var keys = Object.keys(data.entries);
    if (!keys.length || keys.length > 36600) return false;
    for (var i = 0; i < keys.length; i++) {
      var key = keys[i], entries = data.entries[key];
      if (!/^\d{4}-\d{2}-\d{2}$/.test(key)) return false;
      var year = Number(key.slice(0,4)), month = Number(key.slice(5,7)), day = Number(key.slice(8,10));
      if (!years.has(year) || month < 1 || month > 12 || day < 1 || day > new Date(year, month, 0).getDate() || !Array.isArray(entries) || !entries.length || entries.length > 20) return false;
      for (var j = 0; j < entries.length; j++) {
        var e = entries[j];
        if (!e || ['libur','cuti','peringatan'].indexOf(e.type) < 0 || typeof e.name !== 'string' || !e.name.trim() || e.name.length > 160) return false;
        if (e.type === 'libur') seen.add(year);
      }
    }
    return data.years.every(function (y) { return seen.has(y); });
  }
  function covers(next, previous) { return previous.years.every(function (y) { return next.years.indexOf(y) !== -1; }); }
  function create(options) {
    var current = options.initial, storage = options.storage, notify = options.onChange || function () {};
    var lastChecked = 0, lastAttempt = 0, saving = true, message = 'Data bawaan; belum diperiksa secara online.', pending = false;
    function emit() { notify({data:current, checkedAt:lastChecked, message:message, pending:pending, cached:saving}); }
    if (!valid(current)) throw new Error('Invalid bundled holiday data');
    try {
      var saved = JSON.parse(storage.getItem(CACHE_KEY) || 'null');
      if (saved && valid(saved.data) && saved.data.revision >= current.revision && covers(saved.data, current)) {
        current = saved.data;
        lastChecked = Number.isFinite(saved.checkedAt) && saved.checkedAt > 0 ? saved.checkedAt : 0;
        message = 'Menggunakan data tersimpan di browser.';
      }
    } catch (e) { saving = false; }
    function refresh(force) {
      var now = options.now();
      if (pending || (!force && ((lastChecked && now >= lastChecked && now - lastChecked < INTERVAL) || (lastAttempt && now >= lastAttempt && now - lastAttempt < 300000)))) return Promise.resolve();
      lastAttempt = now; pending = true; message = 'Memeriksa data libur online…'; emit();
      var abort = options.makeAbort ? options.makeAbort() : null;
      var timer;
      var timeout = new Promise(function (_, reject) { timer = setTimeout(function () { if (abort) abort.abort(); reject(new Error('timeout')); }, 10000); });
      var request = Promise.resolve().then(function () { return options.fetch(URL, {credentials:'omit', cache:'no-store', signal:abort ? abort.signal : undefined}); })
        .then(function (response) { if (!response.ok) throw new Error('HTTP ' + response.status); return response.text(); })
        .then(function (text) {
          if (text.length > MAX_BYTES * 2) throw new Error('response too large');
          var envelope = JSON.parse(text);
          if (envelope.encoding !== 'base64' || typeof envelope.content !== 'string' || envelope.size > MAX_BYTES) throw new Error('invalid envelope');
          var binary = options.atob(envelope.content.replace(/\s/g,''));
          if (binary.length > MAX_BYTES) throw new Error('data too large');
          var bytes = new Uint8Array(binary.length); for (var i=0; i<binary.length; i++) bytes[i]=binary.charCodeAt(i);
          var parsed = JSON.parse(new TextDecoder('utf-8', {fatal:true}).decode(bytes));
          if (!valid(parsed) || !covers(parsed,current) || parsed.revision < current.revision ||
              (parsed.revision === current.revision && JSON.stringify(parsed) !== JSON.stringify(current))) throw new Error('invalid or older feed');
          return parsed;
        });
      return Promise.race([request, timeout]).then(function (next) {
        clearTimeout(timer); current = next; lastChecked = options.now();
        try { storage.setItem(CACHE_KEY, JSON.stringify({data:current, checkedAt:lastChecked})); saving = true; message = 'Data libur berhasil diperiksa online.'; }
        catch (e) { saving = false; message = 'Data online hanya sementara; penyimpanan browser tidak tersedia.'; }
      }, function () {
        clearTimeout(timer); message = 'Pembaruan gagal; menggunakan data terakhir atau bawaan.';
      }).then(function () { pending = false; emit(); });
    }
    return {getState:function () { return {data:current, checkedAt:lastChecked, message:message, pending:pending, cached:saving}; }, refresh:refresh};
  }
  return {URL:URL, CACHE_KEY:CACHE_KEY, INTERVAL:INTERVAL, valid:valid, covers:covers, create:create};
});
