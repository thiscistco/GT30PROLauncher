#!/usr/bin/env python3
"""HUD and fixed navigation stay in parity without changing alarm functionality."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
clock=(root/'app/src/main/java/com/gt30pro/launcher/ClockActivity.java').read_text()
hud=(root/'app/src/main/java/com/gt30pro/launcher/ClockHudView.java').read_text()
html=(root/'preview/index.html').read_text()
assert 'private int tab = -1;' in clock and 'state.getInt("clock_tab",-1)' in clock
assert 'new ClockHudView(this,ui,doto)' in clock
assert 'page.addView(stage,new LinearLayout.LayoutParams(-1,0,1))' in clock
assert clock.index('page.addView(stage') < clock.index('page.addView(tabs')
assert 'hud.setVisibility(selected == -1 ? View.VISIBLE : View.GONE)' in clock
assert 'panelScroll.setVisibility(selected == -1 ? View.GONE : View.VISIBLE)' in clock
assert 'if (tab != -1) showTab(-1); else super.onBackPressed();' in clock
assert 'hud.setTime(Calendar.getInstance())' in clock and 'second != lastHudSecond' in clock
assert 'handler.removeCallbacks(tick)' in clock and 'lastHudSecond=-1' in clock
assert 'Calendar.HOUR_OF_DAY' in hud and 'Calendar.MINUTE' in hud and 'Calendar.SECOND' in hud
assert 'String.format(Locale.US,"%02d:%02d:%02d",h,m,s)' in hud
assert 'i<60' in hud and 'i%5==0' in hud and 'second/60' in hud
assert 'ui.ink' in hud and 'ui.card' in hud and 'ui.border' in hud and 'ui.control' in hud
assert 'canvas.drawRoundRect(frame,corner,corner,paint)' in hud and 'Math.min(12f*density' in hud
assert 'frame.width()*.72f' in hud and 'frame.height()*.48f' in hud and '230f*density' in hud
assert 'setTextSize(size*.11f)' in hud and 'canvas.drawRoundRect(label,labelCorner,labelCorner,paint)' in hud
assert 'class="clock-hud-label"' in html and 'border-radius:3mm' in html
assert 'height:min(48%, 36.5mm)' in html and 'font:800 26.4px' in html
assert 'width:7.62mm; min-width:0; height:7.62mm' in html
assert 'new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER)' in clock
assert 'timerDisplay.setBackground(bg(ui.card))' in clock and 'stopwatchDisplay.setBackground(bg(ui.card))' in clock
assert 'setContentDescription("Jam " + value)' in hud
assert 'ClockAlarms.schedule' in clock and 'timer_draft' in clock and 'saveStopwatch()' in clock
hud_markup=html.split('<div class="clock-hud"',1)[1].split('</svg>',1)[0]
assert len(re.findall(r'<line class="major"',hud_markup))==12
assert len(re.findall(r'<line class="minor"',hud_markup))==48
assert 'id="clockHudText"' in hud_markup and 'id="clockHudMarker"' in hud_markup
assert "now.getHours()" in html and "now.getMinutes()" in html and "now.getSeconds()" in html
assert "window.clockShowHud" in html and "window.clockReturnToHud" in html
assert 'data-clock-panel="alarm" hidden' in html
for name in ('world','timer','stopwatch'):
    assert f'data-clock-panel="{name}" hidden' in html
print('Clock HUD: local HH:mm:ss, 60 analog ticks, marker, lifecycle and bottom navigation: OK')
