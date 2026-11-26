(function (global) {
  'use strict';
  function hourStart(times, now) {
    if (!Array.isArray(times) || typeof now !== 'string' || now.length < 13) return -1;
    var hour = now.slice(0, 13);
    return times.findIndex(function (t) { return typeof t === 'string' && t.length >= 13 && t.slice(0,13) >= hour; });
  }
  function number(value, suffix) { return typeof value === 'number' && Number.isFinite(value) ? Math.round(value) + suffix : '--'; }
  if (typeof module !== 'undefined' && module.exports) module.exports = {hourStart:hourStart, number:number};
  if (!global.document) return;
  var doc=global.document, el=function (id) { return doc.getElementById(id); };
  if (!el('weatherPage')) return;
  function node(tag, cls, text) { var n=doc.createElement(tag); if(cls)n.className=cls; if(text!=null)n.textContent=text; return n; }
  function icon(code,day) {
    var svg=doc.createElementNS('http://www.w3.org/2000/svg','svg');svg.setAttribute('viewBox','0 0 13 13');svg.setAttribute('aria-hidden','true');
    var symbol=global.weatherSymbols;
    if (!symbol) return svg;
    symbol.icons[symbol.kind(code,day)].forEach(function(row,y){Array.from(row).forEach(function(c,x){if(c!=='#')return;var dot=doc.createElementNS('http://www.w3.org/2000/svg','circle');dot.setAttribute('cx',x+.5);dot.setAttribute('cy',y+.5);dot.setAttribute('r','.36');svg.appendChild(dot);});});
    return svg;
  }
  function item(o,key,i) { return o && Array.isArray(o[key]) ? o[key][i] : null; }
  function time(t) { return typeof t === 'string' && t.length >= 16 ? t.slice(11,16) : '--:--'; }
  function dayName(date,i) { if(i===0)return 'Hari ini'; if(typeof date!=='string'||!/^\d{4}-\d\d-\d\d$/.test(date))return '--'; var p=date.split('-').map(Number);var d=new Date(p[0],p[1]-1,p[2]);return d.toLocaleDateString('id-ID',{weekday:'short',day:'numeric',month:'short'}); }
  global.updateWeatherPage = function (data,status) {
    var current=data && data.current, available=!!current;
    el('weatherPageStatus').textContent=status;
    el('weatherPageEmpty').hidden=available;el('weatherPageDetails').hidden=!available;
    el('weatherPageTemp').textContent=available?number(current.temperature_2m,'°'):'--°';
    var c=available?current.weather_code:-1, day=!available||current.is_day===1;
    el('weatherPageCondition').textContent=global.weatherSymbols?global.weatherSymbols.description(c,day):'Cuaca belum tersedia';
    el('weatherPageObserved').textContent=available&&typeof current.time==='string'&&current.time.length>=16 ? 'Waktu data: '+current.time.slice(0,10)+' '+time(current.time) : '';
    var mainIcon=el('weatherPageIcon'), fresh=icon(c,day);mainIcon.replaceChildren.apply(mainIcon,Array.from(fresh.childNodes));
    ['weatherHours','weatherDays','weatherDetails'].forEach(function(id){el(id).replaceChildren();});
    if (!available) return;
    var hourly=data.hourly||{}, daily=data.daily||{}, start=hourStart(hourly.time,current.time);
    if(start<0)el('weatherHours').appendChild(node('span','weather-status','Data per jam belum tersedia.'));
    else for(var i=start;i<Math.min(start+24,hourly.time.length);i++) {
      var cell=node('div','weather-hour'); cell.appendChild(node('span','',time(item(hourly,'time',i))));
      cell.appendChild(icon(item(hourly,'weather_code',i) == null ? -1 : item(hourly,'weather_code',i), item(hourly,'is_day',i)!==0));
      cell.appendChild(node('strong','',number(item(hourly,'temperature_2m',i),'°'))); cell.appendChild(node('span','',number(item(hourly,'precipitation_probability',i),'%')));el('weatherHours').appendChild(cell);
    }
    if(!Array.isArray(daily.time)||!daily.time.length)el('weatherDays').appendChild(node('span','weather-status','Data harian belum tersedia.'));
    else for(var j=0;j<Math.min(daily.time.length,7);j++) {
      var row=node('div','weather-daily-row');row.appendChild(node('span','weather-day',dayName(daily.time[j],j))); row.appendChild(icon(item(daily,'weather_code',j)==null?-1:item(daily,'weather_code',j),true));row.appendChild(node('span','weather-rain',number(item(daily,'precipitation_probability_max',j),'%')));row.appendChild(node('span','',number(item(daily,'temperature_2m_max',j),'°')+' / '+number(item(daily,'temperature_2m_min',j),'°')));el('weatherDays').appendChild(row);
    }
    [['TERASA',number(current.apparent_temperature,'°')],['KELEMBAPAN',number(current.relative_humidity_2m,'%')],['ANGIN',number(current.wind_speed_10m,' km/j')],['TEKANAN',number(current.surface_pressure,' hPa')],['MATAHARI TERBIT',time(item(daily,'sunrise',0))],['MATAHARI TERBENAM',time(item(daily,'sunset',0))]].forEach(function(pair){var row=node('div','weather-detail-row');row.appendChild(node('span','',pair[0]));row.appendChild(node('span','',pair[1]));el('weatherDetails').appendChild(row);});
  };
})(typeof window === 'undefined' ? globalThis : window);
