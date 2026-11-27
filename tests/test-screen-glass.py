#!/usr/bin/env python3
"""Glass variants remain reproducible, translucent and system-aware."""
from pathlib import Path
import importlib.util
import xml.etree.ElementTree as ET
from PIL import Image, ImageChops, ImageStat

root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('glass', root / 'tools/generate_screen_glass.py')
mod = importlib.util.module_from_spec(spec); spec.loader.exec_module(mod)
wall = Image.open(root/'app/src/main/assets/wallpaper_gt30pro.png').convert('RGBA')
assert (root/'app/src/main/assets/wallpaper_gt30pro.png').read_bytes() == (root/'preview/assets/gt30pro-internals.png').read_bytes()
brightness = []
for dark, suffix in [(False, ''), (True, '_dark')]:
    native = root/f'app/src/main/assets/home_screen_glass{suffix}.png'
    preview = root/f'preview/assets/home-screen-glass{suffix.replace("_", "-")}.png'
    assert native.read_bytes() == preview.read_bytes(), 'Android and preview glass differ'
    actual = Image.open(native).convert('RGBA'); expected = mod.artwork(dark)
    assert actual.size == (1224,2720)
    for a,b in zip(actual.split(),expected.split()):
        assert ImageChops.difference(a,b).getbbox() is None, 'Glass asset does not match generator'
    alpha = actual.getchannel('A')
    assert alpha.getextrema() == (191, 191), 'Glass must be 25% transparent (75% opaque)'
    for channel in actual.split()[:3]:
        assert channel.getextrema() == ((0, 0) if dark else (255, 255)), 'Glass must be pure black or white'
    brightness.append(ImageStat.Stat(Image.alpha_composite(wall,actual).convert('L')).mean[0])
assert brightness[0] - brightness[1] > 15, 'Dark and light must be visibly different'
ns='{http://schemas.android.com/apk/res/android}'
layout=ET.parse(root/'app/src/main/res/layout/activity_main.xml').getroot()
children=list(layout); ids=[item.get(ns+'id','') for item in children]
assert ids.index('@+id/wallpaper') < ids.index('@+id/amber_glow') < ids.index('@+id/screen_glass')
glass=children[ids.index('@+id/screen_glass')]
assert glass.tag=='ImageView' and glass.get(ns+'clickable')=='false' and glass.get(ns+'focusable')=='false'
assert glass.get(ns+'importantForAccessibility')=='no' and glass.get(ns+'scaleType')=='fitXY'
main=(root/'app/src/main/java/com/gt30pro/launcher/MainActivity.java').read_text()
assert 'Configuration.UI_MODE_NIGHT_MASK' in main and 'Configuration.UI_MODE_NIGHT_YES' in main
assert 'Wallpapers.loadScreenGlass(this, dark)' in main
assert main.index('updateScreenGlass();') < main.index('homeRoot.addView(clockBox')
assert 'onConfigurationChanged(Configuration newConfig)' in main and 'super.onConfigurationChanged(newConfig);\n        updateScreenGlass();' in main
assert 'super.onResume();\n        homeResumed = true;\n        updateScreenGlass();' in main
assert 'homeResumed = false;' in main and 'if (homeResumed) updateChargeAnim();' in main
assert 'glassDark.booleanValue() != dark' in main
assert 'clockLed.setLightGlass(!dark)' in main and 'dateLed.setLightGlass(!dark)' in main
assert 'weatherLed.setLightGlass(!dark)' in main and 'updateChargeAnim();' in main
for name in ('batteryLed', 'batteryBolt', 'clockBox', 'dateBox', 'weatherBox'):
    assert main.index('homeRoot.addView('+name) > main.index('updateScreenGlass();')
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
activity=next(a for a in manifest.find('application').findall('activity') if a.get(ns+'name')=='.MainActivity')
assert 'uiMode' in activity.get(ns+'configChanges')
loader=(root/'app/src/main/java/com/gt30pro/launcher/Wallpapers.java').read_text()
assert 'dark ? "home_screen_glass_dark.png" : "home_screen_glass.png"' in loader
html=(root/'preview/index.html').read_text()
assert html.index('id="wallpaper"') < html.index('id="amberGlow"') < html.index('class="home-screen-glass light"') < html.index('id="clockLed"')
assert 'pointer-events:none' in html.split('.home-screen-glass {',1)[1].split('}',1)[0]
assert '.home-screen-glass.dark { display:none; }' in html
media=html.split('@media (prefers-color-scheme: dark)',1)[1].split('  }',1)[0]
assert '.home-screen-glass.light { display:none; }' in media and '.home-screen-glass.dark { display:block; }' in media
for name in ['home-screen-glass.png?v=3','home-screen-glass-dark.png?v=2']:
    assert f'src="assets/{name}" alt="" aria-hidden="true" draggable="false"' in html
assert '@media (prefers-color-scheme: light)' in html
for selector in ('.clock-led > span:not(.clock-sep)', '.date-led #homeDateWeekday', '.home-weather', '.battery-led'):
    assert selector in html.split('@media (prefers-color-scheme: light)',1)[1].split('\n  }',1)[0]
print('Screen glass: light/dark artwork, system changes, preview parity and touch-through: OK')
