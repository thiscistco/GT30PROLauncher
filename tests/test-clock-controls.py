#!/usr/bin/env python3
"""Clock actions, city ordering, saved-index compatibility and wheel parity."""
from pathlib import Path
import importlib.util,re
root=Path(__file__).resolve().parents[1]
java=(root/'app/src/main/java/com/gt30pro/launcher/ClockActivity.java').read_text()
html=(root/'preview/index.html').read_text()
assert 'R.drawable.ic_calendar_save,"Simpan alarm"' in java
assert 'id="previewSaveAlarm" type="button" aria-label="Simpan alarm"' in html
old=['Jakarta','Makassar','Jayapura','Singapore','Tokyo','Seoul','Dubai','London','Paris','New York','Los Angeles','Sydney']
city_array=re.search(r'private static final String\[\] CITIES = \{([^}]+)\}',java).group(1)
assert re.findall(r'"([^"]+)"',city_array)==old
order=list(map(int,re.search(r'CITY_ORDER = \{([^}]+)\}',java).group(1).split(',')))
assert sorted(order)==list(range(len(old)))
assert [old[i] for i in order]==['Jakarta','Makassar','Jayapura','Singapore','Seoul','Sydney','Tokyo','Dubai','Paris','London','Los Angeles','New York']
assert 'var cityOrder=[0,1,2,3,5,11,4,6,8,7,10,9]' in html
assert 'getString("cities", "0,3,4,7,9")' in java
assert 'setMultiChoiceItems(CITIES' not in java and 'toggle.setOnCheckedChangeListener((v,on) -> {' in java
assert 'MAX_WORLD_CITIES = 5' in java and 'WorldCities.limit(selected,CITY_ORDER,MAX_WORLD_CITIES)' in java
assert 'WorldCities.count(chosen)>=MAX_WORLD_CITIES' in java and 'v.setChecked(false)' in java
assert 'R.drawable.ic_calendar_add,"Pilih kota"' in java
assert 'id="previewCitiesButton" type="button" aria-label="Pilih kota"' in html
assert "cityBox.querySelectorAll('input:checked').length>5" in html
assert 'world-city-time { flex:none; text-align:right' in html
assert '.clock-stage:has([data-clock-panel="world"]:not([hidden]))' in html
assert 'hud.setEditorOpen(selected >= 1)' in java
# At the target phone size, the pinned title and five rows fit without scrolling.
width,height=405,854; gap=8; dial=min(width-2*gap,height-2*gap-480)
panel=height-(gap+dial+4+64)-16
assert panel >= 48+8+5*(56+6)+20

assert 'stage.addView(featureHeader,headerParams)' in java
assert 'headerTop+(tab >= 1 ? dp(48) : 0)' in java
assert 'featureHeader.setVisibility(selected >= 1 ? View.VISIBLE : View.GONE)' in java
assert 'setFeatureTitle("JAM DUNIA")' in java and 'setFeatureTitle("TIMER")' in java and 'setFeatureTitle("STOPWATCH")' in java
assert 'for(int i=0;i<laps.length();i++)' in java and "byId('previewLaps').appendChild(row)" in html
for name in ('world','timer','stopwatch'):
 assert f'data-clock-header="{name}"' in html
assert 'clock-feature-header { display:none; position:absolute;' in html
assert 'time.setGravity(Gravity.END)' in java and 'date.setGravity(Gravity.END)' in java
assert 'lap.addView(lapLine("Total",formatElapsed(total)))' in java
assert 'duration.setGravity(Gravity.END)' in java
assert "['Total',format(value,true)]" in html and 'stopwatch-lap-time { flex:none; text-align:right' in html
assert 'row.setGravity(Gravity.CENTER_VERTICAL)' in java and 'new LinearLayout.LayoutParams(0,-1,1)' in java
assert 'input.setAttribute(\'role\',\'switch\')' in html and 'clock-city-name { flex:1; min-width:0' in html
for value in ('timerHours=alarmWheel(999','timerMinutes=alarmWheel(59','timerSeconds=alarmWheel(59','String.valueOf(timerHours.getValue())','ClockAlarms.scheduleAt(this,ClockAlarms.TIMER_FIRE','ClockAlarms.cancelTimer(this)'):
 assert value in java
for value in ('timerHourWheel','timerMinuteWheel','timerSecondWheel','aria-label="Mulai timer"','aria-label="Batalkan timer"','aria-label="Catat lap"','aria-label="Reset stopwatch"'):
 assert value in html
for name in ('play','pause','stop','lap','reset'):
 assert 'R.drawable.ic_clock_action_'+name in java
spec=importlib.util.spec_from_file_location('clock_actions',root/'tools/generate_clock_action_icons.py')
module=importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
for path,content in module.generate(): assert path.read_text()==content, f'stale icon: {path}'
print('Clock controls: accessible icons, 24h/999h wheels, ordered city switches and saved indices: OK')
