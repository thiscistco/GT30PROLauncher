/* Three-line local date, independent of the browser's language setting. */
(function (root, factory) {
  var api = factory();
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  if (root && root.document) api.mount(root);
})(typeof window !== 'undefined' ? window : null, function () {
  'use strict';
  var days = ['MIN','SEN','SEL','RAB','KAM','JUM','SAB'];
  var fullDays = ['Minggu','Senin','Selasa','Rabu','Kamis','Jumat','Sabtu'];
  var months = ['JAN','FEB','MAR','APR','MEI','JUN','JUL','AGU','SEP','OKT','NOV','DES'];
  var fullMonths = ['Januari','Februari','Maret','April','Mei','Juni','Juli','Agustus','September','Oktober','November','Desember'];
  function format(date) {
    var n = date.getDate(), day = n < 10 ? '0' + n : String(n);
    return {weekday:days[date.getDay()], day:day, month:months[date.getMonth()],
      description:fullDays[date.getDay()] + ', ' + n + ' ' + fullMonths[date.getMonth()] + ' ' + date.getFullYear()};
  }
  function mount(win) {
    var doc = win.document, box = doc.getElementById('homeDate'), last = '';
    function tick() {
      var value = format(new Date());
      if (value.description !== last) {
        doc.getElementById('homeDateWeekday').textContent = value.weekday;
        doc.getElementById('homeDateNumber').textContent = value.day;
        doc.getElementById('homeDateMonth').textContent = value.month;
        box.setAttribute('aria-label', value.description); last = value.description;
      }
      win.setTimeout(tick, 1000);
    }
    tick();
  }
  return {format:format, mount:mount};
});
