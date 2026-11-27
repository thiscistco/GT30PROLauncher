#!/usr/bin/env python3
"""Shared app visual tokens and semantic, theme-aware integration."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
java=root/'app/src/main/java/com/gt30pro/launcher'
ui=(java/'AppUi.java').read_text(); html=(root/'preview/index.html').read_text(); js=(root/'preview/calendar.js').read_text()
for color in ('FFF4F7FA','FF101820','FF19252F','FF142A3A','FFF5F8FA','FFE3EAF0','FF2A3B49','FF304355','FFDCE8F0'):
    assert color in ui, color
assert 'calendar_appearance' in ui and '"system"' in ui
assert 'Configuration.UI_MODE_NIGHT_MASK' in ui
assert 'setContentDescription(description)' in ui and 'dp(a,48)' in ui
for activity in ('ClockActivity','WeatherActivity','SuperAppActivity','AppDrawerActivity'):
    src=(java/(activity+'.java')).read_text()
    assert 'new AppUi(this)' in src and 'ui.header(this, doto,' in src and 'ui.window(this)' in src, activity
    assert 'extends SwipeBackActivity' in src and 'ui.choice.equals(current.choice)' in src, activity
assert 'AppUi palette = new AppUi(this)' in (java/'CalendarActivity.java').read_text()
clock=(java/'ClockActivity.java').read_text()
for word in ('clock_tab','timer_draft','ClockAlarms.schedule','showWorld','showTimer','showStopwatch','setColorFilter(i == selected ? ui.selectedText : ui.ink)'):
    assert word in clock,word
for prefix in ('clock','weather','super','drawer'):
    for suffix in ('Page','Theme','ThemeIcon','ThemeMenu','ThemeSystem','ThemeLight','ThemeDark'):
        assert f'id="{prefix}{suffix}"' in html, prefix+suffix
    assert f'id="{prefix}Back" type="button" aria-label="Kembali"' in html
assert "['clock','weather','super','drawer']" in js
assert "win.localStorage.setItem('gt30-calendar-theme',key)" in js
assert 'prefers-color-scheme: dark' in js
for css in ('--app-bg:#f4f7fa','--app-bg:#101820','--app-text:#142a3a','--app-text:#f5f8fa','--app-control:#e3eaf0','--app-control:#2a3b49'):
    assert css in html, css
assert 'home-screen-glass.png?v=5' in html and 'home-screen-glass-dark.png?v=4' in html
print('Shared app theme, header, palettes, clock state, preview and unchanged home glass: OK')
