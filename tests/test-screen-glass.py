#!/usr/bin/env python3
"""Homescreen has no glass overlay or sensor effect; existing positions remain intact."""
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
ns='{http://schemas.android.com/apk/res/android}'
layout=ET.parse(root/'app/src/main/res/layout/activity_main.xml').getroot()
ids=[item.get(ns+'id','') for item in layout]
assert ids.index('@+id/wallpaper') < ids.index('@+id/amber_glow')
assert '@+id/screen_glass' not in ids
java=root/'app/src/main/java/com/gt30pro/launcher'
main=(java/'MainActivity.java').read_text(); html=(root/'preview/index.html').read_text()
assert 'screenGlass' not in main and 'GlassParallax' not in main and 'glass_parallax' not in main
assert not list(java.glob('GlassParallax*.java'))
assert 'loadScreenGlass' not in (java/'Wallpapers.java').read_text()
for asset in ('home_screen_glass.png','home_screen_glass_dark.png'):
    assert not (root/'app/src/main/assets'/asset).exists()
assert 'home-screen-glass' not in html and 'glass-parallax' not in html and 'glass-over-home' not in html
assert 'glassPreviewToggle' not in html and 'glassParallaxToggle' not in html
assert not (root/'preview/glass-parallax.js').exists()
assert (root/'app/src/main/assets/wallpaper_gt30pro.png').read_bytes()==(root/'preview/assets/gt30pro-internals.png').read_bytes()
assert 'Configuration.UI_MODE_NIGHT_MASK' in main and 'updateHomePalette()' in main
assert 'clockLed.setLightGlass(!dark)' in main and 'dateLed.setLightGlass(!dark)' in main
assert 'weatherLed.setLightGlass(!dark)' in main and 'batteryInfo.setLightGlass(!dark)' in main
assert 'onConfigurationChanged(Configuration newConfig)' in main and 'if (homeResumed) updateChargeAnim();' in main
for view in ('clockBox','dateBox','weatherBox'):
    assert view+'.setElevation(4f * getResources().getDisplayMetrics().density)' in main
assert 'Math.max(root.getWidth() / 1224f, root.getHeight() / 2720f)' in main
assert 'clockBox.setTranslationY(top - params.topMargin - 2182f * wallpaperScale(root))' in main
assert 'batteryBox.setTranslationY(cssPixel - 1470f * wallpaperScale(root))' in main
assert '+ 750f * wallpaperScale(root)' in main
assert 'clockBox.setScaleX(1.2f)' in main and 'batteryLed.setScaleX(1.215f)' in main
assert html.index('id="wallpaper"') < html.index('id="amberGlow"') < html.index('id="clockLed"')
assert '<script src="home-position.js?v=1"></script>' in html
print('Homescreen: glass/reflection removed, wallpaper intact, system palette and home positions preserved OK')
