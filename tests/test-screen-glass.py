#!/usr/bin/env python3
"""Screen glass stays translucent, reproducible, touch-through and home-only."""
from pathlib import Path
import importlib.util
import xml.etree.ElementTree as ET
from PIL import Image, ImageChops

root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('glass', root / 'tools/generate_screen_glass.py')
mod = importlib.util.module_from_spec(spec); spec.loader.exec_module(mod)
native = root / 'app/src/main/assets/home_screen_glass.png'
preview = root / 'preview/assets/home-screen-glass.png'
assert native.read_bytes() == preview.read_bytes(), 'Android and preview glass differ'
actual = Image.open(native).convert('RGBA'); expected = mod.artwork()
assert actual.size == (1224,2720)
for a,b in zip(actual.split(),expected.split()):
    assert ImageChops.difference(a,b).getbbox() is None, 'Glass asset does not match generator'
alpha = actual.getchannel('A')
assert alpha.getextrema()[0] > 0 and alpha.getextrema()[1] <= 96, 'The glass must never be opaque'
assert alpha.crop((200,300,1024,2300)).getextrema()[1] <= 40, 'Screen interior should remain translucent'
assert (root/'app/src/main/assets/wallpaper_gt30pro.png').read_bytes() == (root/'preview/assets/gt30pro-internals.png').read_bytes()
ns='{http://schemas.android.com/apk/res/android}'
layout=ET.parse(root/'app/src/main/res/layout/activity_main.xml').getroot()
children=list(layout)
ids=[item.get(ns+'id','') for item in children]
assert ids.index('@+id/wallpaper') < ids.index('@+id/amber_glow') < ids.index('@+id/screen_glass')
glass=children[ids.index('@+id/screen_glass')]
assert glass.tag=='ImageView' and glass.get(ns+'clickable')=='false' and glass.get(ns+'focusable')=='false'
assert glass.get(ns+'importantForAccessibility')=='no' and glass.get(ns+'scaleType')=='fitXY'
main=(root/'app/src/main/java/com/gt30pro/launcher/MainActivity.java').read_text()
assert 'glass.setImageBitmap(Wallpapers.loadScreenGlass(this))' in main
assert main.index('glass.setImageBitmap') < main.index('homeRoot.addView(clockBox')
html=(root/'preview/index.html').read_text()
assert html.index('id="wallpaper"') < html.index('id="amberGlow"') < html.index('class="home-screen-glass"') < html.index('id="clockLed"')
assert 'pointer-events:none' in html.split('.home-screen-glass {',1)[1].split('}',1)[0]
assert 'alt="" aria-hidden="true" draggable="false"' in html
assert 'src="assets/home-screen-glass.png?v=1"' in html
print('Screen glass: transparent, reproducible, touch-through, home-only and Android/preview parity: OK')
