(function (global) {
  'use strict';
  // Kept in sync with Android WeatherIcons.java.
  var icons = {
    unknown: ['........#....','.....#..#..#.','......####...','.....##...#..','...##..#...#.','..#.....##...','.#........##.','.#..........#','.#..........#','..##########.','.............','.............','.............'],
    sun: ['......#......','..#...#...#..','...#.....#...','.....###.....','....#...#....','...#.....#...','##.#.....#.##','...#.....#...','....#...#....','.....###.....','...#.....#...','..#...#...#..','......#......'],
    cloud: ['.............','.............','.....###.....','....#...#....','...#.....#...','..#.......##.','.#..........#','.#..........#','.#..........#','..##########.','.............','.............','.............'],
    rain: ['.............','.....###.....','....#...#....','...#.....#...','..#.......##.','.#..........#','.#..........#','..##########.','...#...#...#.','..#...#...#..','...#...#...#.','..#...#...#..','.............'],
    storm: ['.....###.....','....#...#....','...#.....#...','..#.......##.','.#..........#','..##########.','......##.....','.....##......','....####.....','......##.....','.....##......','....##.......','.............'],
    snow: ['.....###.....','....#...#....','...#.....#...','..#.......##.','.#..........#','..##########.','.............','..#...#...#..','.###.###.###.','..#...#...#..','.............','.............','.............'],
    fog: ['.............','.....###.....','....#...#....','...#.....#...','..#.......##.','.#..........#','..##########.','.............','.###########.','.............','..#########..','.............','.............'],
    moon: ['.....##......','....#........','...#.........','..#..........','..#..........','..#..........','..#.......#..','..#.......#..','...#.....#...','....#####....','.............','.............','.............']
  };
  function kind(code, day) {
    if (code < 0) return 'unknown';
    if (code === 0) return day ? 'sun' : 'moon';
    if (code === 1 && !day) return 'moon';
    if (code <= 3) return 'cloud';
    if (code === 45 || code === 48) return 'fog';
    if (code >= 95) return 'storm';
    if ((code >= 71 && code <= 77) || code === 85 || code === 86) return 'snow';
    return 'rain';
  }
  function description(code, day) {
    if (code < 0) return 'Cuaca belum tersedia';
    return {sun:'Cerah',moon:'Cerah malam',cloud:'Berawan',fog:'Berkabut',storm:'Badai petir',snow:'Bersalju',rain:'Hujan'}[kind(code,day)];
  }
  if (typeof module !== 'undefined' && module.exports) module.exports = {icons:icons, kind:kind, description:description};
  global.weatherSymbols = {icons:icons, kind:kind, description:description};
  if (!global.document) return;
  var buttons = [document.getElementById('homeWeather'),document.getElementById('superWeather')];
  if (buttons.some(function (b) { return !b; })) return;
  var active = false, loading = false, last = null, locationLabel = null, timer;
  function draw(code, day, temp, status) {
    buttons.forEach(function (button) {
      var svg = button.querySelector('svg');
      while (svg.firstChild) svg.removeChild(svg.firstChild);
      icons[kind(code,day)].forEach(function (row,y) { Array.from(row).forEach(function (c,x) {
        if (c !== '#') return;
        var dot = document.createElementNS('http://www.w3.org/2000/svg','circle');
        dot.setAttribute('cx',x+.5); dot.setAttribute('cy',y+.5); dot.setAttribute('r','.36'); svg.appendChild(dot);
      }); });
      var label = button.querySelector('.weather-temp');
      if (label) label.textContent = temp == null ? '--°' : temp+'°';
      button.setAttribute('aria-label','Cuaca: '+description(code,day)+', '+(temp == null ? 'suhu belum tersedia' : temp+' derajat Celsius')+'. '+'Ketuk untuk membuka Cuaca');
      button.title = 'Buka Cuaca';
    });
    if (global.updateWeatherPage) global.updateWeatherPage(last && last.forecast && code >= 0 && Date.now()-last.time < 2*60*60*1000 ? last.forecast : null, status, locationLabel);
  }
  function schedule() { clearTimeout(timer); if (active && !document.hidden) { var age = last ? Date.now()-last.time : 0; timer = setTimeout(function () { refresh(false); }, Math.max(1000, 30*60*1000-age)); } }
  function failure(message, manual) {
    loading = false;
    if (last && Date.now()-last.time < 2*60*60*1000) draw(last.code,last.day,last.temp, message + ' Data terakhir ditampilkan; lokasinya mungkin berbeda.');
    else draw(-1,true,null,message);
    if (manual) global.alert(message);
    schedule();
  }
  function refresh(manual) {
    if (loading) return;
    if (!global.navigator.geolocation) { failure('Browser tidak mendukung lokasi.',manual); return; }
    loading = true;
    if (last) draw(last.code,last.day,last.temp,'Memperbarui cuaca… Data terakhir mungkin dari lokasi lain.');
    else draw(-1,true,null,'Memperbarui cuaca…');
    global.navigator.geolocation.getCurrentPosition(function (pos) {
      if (document.hidden) { loading = false; return; }
      var lat = (Math.round(pos.coords.latitude*100)/100).toFixed(2), lon = (Math.round(pos.coords.longitude*100)/100).toFixed(2);
      locationLabel = lat + ', ' + lon + ' · perkiraan';
      if (last) draw(last.code,last.day,last.temp,'Memperbarui cuaca… Data terakhir mungkin dari lokasi lain.');
      else draw(-1,true,null,'Memperbarui cuaca…');
      var controller = global.AbortController ? new global.AbortController() : null;
      var fetchTimeout = controller ? setTimeout(function () { controller.abort(); }, 15000) : null;
      fetch('https://api.open-meteo.com/v1/forecast?latitude='+lat+'&longitude='+lon+'&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,is_day,wind_speed_10m,surface_pressure&hourly=temperature_2m,weather_code,precipitation_probability,is_day&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max&forecast_days=7&timezone=auto', controller ? {signal:controller.signal} : {}).then(function (r) {
        if (!r.ok) throw new Error('weather response'); return r.json();
      }).then(function (data) {
        var c = data.current;
        if (!c || typeof c.temperature_2m !== 'number' || !Number.isFinite(c.temperature_2m) || c.temperature_2m < -100 || c.temperature_2m > 70 || !Number.isInteger(c.weather_code) || c.weather_code < 0 || c.weather_code > 99 || (c.is_day !== 0 && c.is_day !== 1)) throw new Error('invalid weather');
        if (fetchTimeout) clearTimeout(fetchTimeout);
        last = {temp:Math.round(c.temperature_2m), code:c.weather_code, day:c.is_day === 1, time:Date.now(), forecast:{current:c, hourly:data.hourly || {}, daily:data.daily || {}}};
        loading = false; draw(last.code,last.day,last.temp,'Diperbarui; ketuk untuk memperbarui lagi'); schedule();
      }).catch(function () { if (fetchTimeout) clearTimeout(fetchTimeout); failure('Cuaca tidak tersedia. Periksa koneksi dan coba lagi.',manual); });
    }, function (error) { if (error && error.code === 1) { last = null; locationLabel = null; } failure('Lokasi tidak tersedia atau izin ditolak. Periksa izin lokasi browser.',manual); }, {enableHighAccuracy:false, timeout:20000, maximumAge:600000});
  }
  function requestFromUser() {
    if (!active && !global.confirm('Lokasi perkiraan akan dikirim ke Open-Meteo untuk mengambil cuaca. Lanjutkan?')) return;
    active = true; refresh(true);
  }
  global.weatherPageOnOpen = function () { if (active && !loading && (!last || Date.now()-last.time >= 30*60*1000)) refresh(false); };
  var refreshButton = document.getElementById('weatherRefresh');
  if (refreshButton) refreshButton.addEventListener('click',requestFromUser);
  var pageRefresh = document.getElementById('weatherPageRefresh');
  if (pageRefresh) pageRefresh.addEventListener('click',requestFromUser);
  if (document.addEventListener) document.addEventListener('visibilitychange',function () {
    if (document.hidden) clearTimeout(timer);
    else if (active && !loading) { if (!last || Date.now()-last.time >= 30*60*1000) refresh(false); else schedule(); }
  });
  draw(-1,true,null,'Ketuk untuk mengaktifkan cuaca');
})(typeof window === 'undefined' ? globalThis : window);
