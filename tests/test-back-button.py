#!/usr/bin/env python3
"""Every launcher page shares Calendar's back arrow, size and placement."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1]
html=(root/'preview/index.html').read_text()
for name, button_class in [('calendar','calendar-icon-button'),('clock','app-back'),('weather','app-back'),('super','app-back'),('drawer','app-back')]:
    match=re.search(r'<button class="'+button_class+r'" id="'+name+r'Back"([^>]*)>(.*?)</button>',html)
    assert match, name
    assert 'aria-label="Kembali"' in match[1] and 'title="Kembali"' in match[1]
    assert match[2] == '<span class="calendar-action-icon calendar-back-icon" aria-hidden="true"></span>'
assert "mask-image:url('assets/calendar-back.svg?v=1')" in html
for selector in ('.calendar-page .calendar-icon-button', '.app-back, .app-theme-button, .app-theme-option'):
    rule=html.split(selector+' {',1)[1].split('}',1)[0]
    for item in ('width:7.62mm','height:7.62mm','padding:0','border:0','background:transparent','place-items:center'):
        assert item in rule, (selector,item)
assert '.app-back { min-height:7.62mm; border-radius:2mm; }' in html
icon=html.split('.calendar-action-icon {',1)[1].split('}',1)[0]
assert 'width:3.81mm' in icon and 'height:3.81mm' in icon
assert 'padding:5mm 4mm 8mm' in html and '.calendar-page { background: #f4f7fa; color: #304355; padding: 5mm 4mm 8mm;' in html
ui=(root/'app/src/main/java/com/gt30pro/launcher/AppUi.java').read_text()
assert 'ImageButton back=backButton(a)' in ui and 'icon(a,R.drawable.ic_calendar_back,"Kembali")' in ui
assert 'back.setOnClickListener(v -> a.finish())' in ui
assert 'new LinearLayout.LayoutParams(dp(a,48),dp(a,48))' in ui
assert 'button.setPadding(dp(a,12),dp(a,12),dp(a,12),dp(a,12))' in ui
calendar=(root/'app/src/main/java/com/gt30pro/launcher/CalendarActivity.java').read_text()
assert 'new AppUi(this).backButton(this)' in calendar and 'new LinearLayout.LayoutParams(dp(48),dp(48))' in calendar
ns='{http://schemas.android.com/apk/res/android}'
vector=ET.parse(root/'app/src/main/res/drawable/ic_calendar_back.xml').getroot()
assert vector.get(ns+'width')=='24dp' and vector.get(ns+'height')=='24dp'
for name in ['activity_super_app.xml','activity_app_drawer.xml']:
    layout=ET.parse(root/'app/src/main/res/layout'/name).getroot()
    page=next(v for v in layout.iter() if v.get(ns+'id')=='@+id/content')
    for attr,val in [('paddingStart','20dp'),('paddingTop','16dp'),('paddingEnd','20dp'),('paddingBottom','24dp')]:
        assert page.get(ns+attr)==val, (name,attr)
for name in ('SuperAppActivity','AppDrawerActivity'):
    src=(root/f'app/src/main/java/com/gt30pro/launcher/{name}.java').read_text()
    assert 'content.setPadding(dp(20), dp(16) + insets.getSystemWindowInsetTop(),' in src
    assert 'dp(20), dp(24) + insets.getSystemWindowInsetBottom())' in src
for name in ('CalendarActivity','ClockActivity','WeatherActivity'):
    src=(root/f'app/src/main/java/com/gt30pro/launcher/{name}.java').read_text()
    assert re.search(r'page\.setPadding\(dp\(20\),\s*dp\(16\)',src), name
print('Back arrow design, target, placement and accessible navigation parity: OK')
