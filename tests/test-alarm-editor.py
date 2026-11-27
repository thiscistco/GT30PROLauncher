#!/usr/bin/env python3
"""Alarm editor ordering, icon accessibility and native/preview asset parity."""
from pathlib import Path
import importlib.util
import re
root=Path(__file__).resolve().parents[1]
java=(root/'app/src/main/java/com/gt30pro/launcher/ClockActivity.java').read_text()
html=(root/'preview/index.html').read_text()
assert 'add.setPadding(dp(16),dp(16),dp(16),dp(16))' in java
assert 'new LinearLayout.LayoutParams(dp(48),dp(48))' in java
assert '.clock-alarm-add .calendar-action-icon { width:2.54mm; height:2.54mm;' in html
assert 'Simulasi preview; alarm aktif di aplikasi Android.' not in html
assert 'kosong = sekali' not in java and 'kosong = sekali' not in html
assert 'final int[] order={1,2,3,4,5,6,0}' in java
assert 'days[0]^=1<<day' in java and 'new HorizontalScrollView(this)' in java
markup=html.split('<div class="alarm-days" id="previewAlarmDays"',1)[1].split('</div>',1)[0]
assert re.findall(r'value="([0-6])"',markup)==['1','2','3','4','5','6','0']
assert re.findall(r'<span>(\w+)</span>',markup)==['Sen','Sel','Rab','Kam','Jum','Sab','Min']
assert '#clockPage .alarm-days { display:flex; flex-wrap:nowrap;' in html
assert 'R.drawable.ic_alarm_sound' in java and 'R.drawable.ic_alarm_vibrate' in java
assert 'setContentDescription("Getar "' in java and 'updateSoundDescription()' in java
assert 'aria-label="Nada alarm"' in html and 'aria-label="Getar"' in html
assert 'alarmEditor.setVisibility(View.VISIBLE)' in java and 'a.put("days",days[0])' in java
assert 'a.put("vibrate",vibrate[0])' in java and 'a.put("sound",editingSound)' in java
assert 'new TimePickerDialog' not in java
assert 'alarmWheel(23,hour[0],"Jam")' in java and 'alarmWheel(59,minute[0],"Menit")' in java
assert 'a.put("hour",hours.getValue())' in java and 'a.put("minute",minutes.getValue())' in java
assert 'picker.setWrapSelectorWheel(true)' in java and 'requestDisallowInterceptTouchEvent(true)' in java
assert 'R.drawable.ic_alarm_delete' in java and '"Hapus alarm " + label.getText()' in java
assert 'enabled.setText("Aktif")' not in java and 'enabled.setContentDescription("Alarm "' in java
assert java.index('details.addView(countdown)') < java.index('metadata.addView(text(dayLabel')
assert 'if (enabled && !ensureExact()) return;' in java and 'a.put("enabled",enabled)' in java
assert 'alarmHourWheel' in html and 'alarmMinuteWheel' in html and 'role="spinbutton"' in html
assert 'alarm-card-switch' in html and 'alarm-delete-icon' in html
assert "details.appendChild(countdown)" in html and html.index("details.appendChild(countdown)") < html.index("meta.appendChild(text)")
spec=importlib.util.spec_from_file_location('icons',root/'tools/generate_alarm_option_icons.py')
module=importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
for path, content in module.generate(): assert path.read_text()==content, f'stale icon: {path}'
print('Alarm editor: icon controls, Monday-first single-row days, native/preview parity: OK')
