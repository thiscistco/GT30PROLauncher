#!/usr/bin/env python3
"""Alarm editor ordering, icon accessibility and native/preview asset parity."""
from pathlib import Path
import importlib.util
import re
root=Path(__file__).resolve().parents[1]
java=(root/'app/src/main/java/com/gt30pro/launcher/ClockActivity.java').read_text()
html=(root/'preview/index.html').read_text()
assert 'add.setPadding(dp(18),dp(18),dp(18),dp(18))' in java
assert 'new LinearLayout.LayoutParams(dp(48),dp(48))' in java
assert '.clock-alarm-add .calendar-action-icon { width:1.905mm; height:1.905mm;' in html
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
assert java.index('timeRow.addView(soundIcon') < java.index('details.addView(countdown)') < java.index('details.addView(text(dayLabel')
assert 'if (mask == 0) return ""' in java and 'if (a.optInt("days") != 0)' in java
assert 'if (enabled && !ensureExact()) return;' in java and 'a.put("enabled",enabled)' in java
assert 'alarmHourWheel' in html and 'alarmMinuteWheel' in html and 'role="spinbutton"' in html
assert 'alarm-card-switch' in html and 'alarm-delete-icon' in html
assert html.index('timeRow.appendChild(icon)') < html.index('details.appendChild(countdown)') < html.index('details.appendChild(text)')
assert 'if (alarm.days.length) { var text' in html
assert 'R.drawable.ic_calendar_cancel,"Batal"' in java and 'id="previewCancelAlarm" type="button" aria-label="Batal"' in html
assert 'if (existing != null)' in java and '"Hapus alarm",() -> deleteAlarm(existing)' in java
assert 'id="previewEditorDelete" type="button" aria-label="Hapus alarm"' in html
assert 'hud.setEditorOpen(true); positionTabs(); panelScroll.scrollTo(0,0);' in java
assert 'hud.setEditorOpen(tab >= 0); positionTabs();' in java
assert 'dp(80),dp(120)' in java and '100cqh - 78.6mm' in html
# A target-sized stage reserves at least enough vertical room for the editor.
width,height=405,854; gap=8; dial=min(width-2*gap,height-2*gap-480)
panel_height=height-(gap+dial+4+64)-16
editor_content=8+48+16+48+8+120+8+48+8+48
assert panel_height>=editor_content
# Seven 48dp day targets and their gaps fit the target editor width.
assert width-32-16 >= 7*48+6*3
spec=importlib.util.spec_from_file_location('icons',root/'tools/generate_alarm_option_icons.py')
module=importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
for path, content in module.generate(): assert path.read_text()==content, f'stale icon: {path}'
print('Alarm editor: icon controls, Monday-first single-row days, native/preview parity: OK')
