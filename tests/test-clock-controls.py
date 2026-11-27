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
assert 'setMultiChoiceItems(CITIES' not in java and 'toggle.setOnCheckedChangeListener((v,on) -> chosen[index]=on)' in java
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
