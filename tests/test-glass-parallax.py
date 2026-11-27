#!/usr/bin/env python3
"""Parallax must not move glass artwork or controls, or listen in background."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
java=root/'app/src/main/java/com/gt30pro/launcher'
main=(java/'MainActivity.java').read_text(); ctl=(java/'GlassParallaxController.java').read_text(); view=(java/'GlassParallaxView.java').read_text()
assert 'screenGlass.setElevation(8f *' in main and 'reflection.setElevation(9f *' in main
assert 'screenGlass.setTranslation' not in main and 'screenGlass.setRotation' not in main
assert 'setClickable(false)' in view and 'setFocusable(false)' in view and 'IMPORTANT_FOR_ACCESSIBILITY_NO' in view
assert 'TYPE_GRAVITY' in ctl and 'TYPE_ACCELEROMETER' in ctl and 'registerListener(this,sensor,50000)' in ctl
assert 'unregisterListener(this)' in ctl and 'ValueAnimator.areAnimatorsEnabled()' in ctl
assert 'if (glassParallax != null) glassParallax.stop();' in main
assert 'homeResumed' in main and 'power.isPowerSaveMode()' in main and 'isGlassParallaxEnabled()' in main
assert 'menu_glass_parallax_off' in main and 'putBoolean("glass_parallax"' in main
assert 'glassParallax.recenter()' in main and 'motion.reset()' in ctl
assert 'getDefaultDisplay().getRotation()' in ctl
assert 'matrix.setTranslate' in view and 'canvas.drawRect' in view
print('Glass parallax: lifecycle, opt-out, fallback, accessibility, power and static glass artwork OK')
