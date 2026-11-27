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
assert clock.index('page.addView(stage') < clock.index('stage.addView(tabsScroll')
assert 'panelParams.bottomMargin=dp(16)' in clock and 'stage.addOnLayoutChangeListener' in clock
assert 'ClockHudView.dialSize(stage.getWidth(),stage.getHeight(),density,(tab == 2 ||'   in clock
assert 'panelParams.topMargin=panelTop' in clock and 'top+dp(64)' in clock
assert 'hud.setVisibility(selected == -1 ? View.VISIBLE : View.GONE)' not in clock
assert 'panelScroll.setVisibility(selected == -1 ? View.GONE : View.VISIBLE)' in clock
assert 'if (tab != -1) showTab(-1); else super.onBackPressed();' in clock
assert 'hud.setTime(Calendar.getInstance())' in clock and 'second != lastHudSecond' in clock
assert 'handler.removeCallbacks(tick)' in clock and 'lastHudSecond=-1' in clock
assert 'Calendar.HOUR_OF_DAY' in hud and 'Calendar.MINUTE' in hud and 'Calendar.SECOND' in hud
assert 'String.format(Locale.US,"%02d:%02d:%02d",h,m,s)' in hud
assert 'i<60' in hud and 'i%5==0' in hud and 'second/60' in hud
assert 'ui.ink' in hud and 'ui.card' in hud and 'ui.border' in hud
assert 'float size=dialSize(getWidth(),getHeight(),density,editorOpen)' in hud
assert 'height-2f*gap-(editorOpen ? 480f : 220f)*density' in hud and 'cy=gap+size/2f' in hud
assert 'canvas.drawCircle(cx,cy,radius,paint)' in hud and 'setTextSize(size*.105f)' in hud
assert 'paint.setColor(0xFF19252F); canvas.drawCircle(cx,cy,radius*.78f,paint)' in hud and 'paint.setColor(Color.WHITE); canvas.drawText' in hud
assert 'canvas.drawRoundRect(frame,corner,corner,paint)' in hud and 'clock-hud-inner { fill:#19252f;' in html
assert 'RectF frame=new RectF(gap,gap,getWidth()-gap,getHeight()-gap)' in hud
assert '.clock-hud { position:absolute; inset:1.2mm;' in html and 'background:var(--app-card); border:1px solid var(--app-border); border-radius:3mm' in html
assert 'container-type:size' in html and '100cqh - 37.3mm' in html and '100cqh - 78.6mm' in html
assert '#clockPage .clock-tabs { top:' in html and 'align-items:flex-start' in html
assert 'clock-hud-inner { fill:#19252f;' in html and "fill:#fff; font:800 25.2px" in html
assert 'width:7.62mm; min-width:0; height:7.62mm' in html
assert 'new LinearLayout.LayoutParams(dp(48), dp(48))' in clock
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
# Verify the shared geometry keeps the circle above navigation at common viewport sizes.
for width, height in ((405, 760), (320, 480), (600, 300), (240, 180)):
    gap=min(8, min(width,height)*.05)
    size=max(0,min(width-2*gap,height-2*gap-220))
    circle_bottom=gap+size*.93
    tab_top=min(gap+size+4,height-64)
    assert circle_bottom < tab_top and tab_top+64 <= height
    assert min(height,tab_top+64) >= circle_bottom
assert 'ClockTime.countdown(when,now)' in clock and 'ClockAlarms.nextTime(a,now)' in clock
assert 'R.drawable.ic_calendar_add' in clock and 'setContentDescription("Tambah alarm")' in clock
print('Clock HUD: local HH:mm:ss, 60 analog ticks, marker, lifecycle and bottom navigation: OK')
