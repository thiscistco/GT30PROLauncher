#!/usr/bin/env python3
"""Icon generation, action wiring and status updates (static; not an Android UI test)."""
from pathlib import Path
import importlib.util
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('voice_icon',root/'tools/generate_voice_icon.py')
generator=importlib.util.module_from_spec(spec); spec.loader.exec_module(generator)
path,expected=generator.generate()
assert path.read_text(encoding='utf8')==expected
svg,svg_expected=generator.generate_svg()
assert svg.read_text(encoding='utf8')==svg_expected
assert len(generator.BARS)==5 and generator.RADIUS==.36
bars=generator.BARS
assert [bar[0] for bar in bars]==[1,3.5,6,8.5,11]
assert [end-start+1 for _,start,end in bars]==[3,9,13,9,3]
assert all(start+end==12 for _,start,end in bars)
icon=ET.parse(path).getroot()
ns='{http://schemas.android.com/apk/res/android}'
assert icon.get(ns+'viewportWidth')=='13' and icon.get(ns+'viewportHeight')=='13'
assert icon.find('path').get(ns+'pathData').count('a.36,.36')==2*len(generator.points())
svgroot=ET.parse(svg).getroot()
assert svgroot.get('viewBox')=='0 0 13 13'
circles=list(svgroot)
assert len(circles)==37
assert [(float(c.get('cx')),float(c.get('cy'))) for c in circles]==generator.points()
assert all(float(c.get('r'))==.36 for c in circles)
java=root/'app/src/main/java/com/gt30pro/launcher'
ctl=(java/'VoiceController.java').read_text(); service=(java/'VoiceService.java').read_text()
assert 'b.setImageDrawable(new VoiceWaveDrawable(a))' in ctl
wave=(java/'VoiceWaveDrawable.java').read_text()
assert 'ValueAnimator.areAnimatorsEnabled()' in wave
assert 'VoiceWaveLogic.heights' in wave and 'canvas.drawCircle' in wave
assert 'public void onRmsChanged(float rms)' in service
assert 'VoiceController.voiceLevel(rms)' in service
assert 'handler.postDelayed(resetRms,350)' in service
assert 'VoiceController.resetWave()' in service and 'resetWave();' in ctl
assert 'boolean home=b.getContext() instanceof MainActivity' in ctl and 'int tint=home ? color' in ctl
assert 'b.setOnClickListener(v -> toggle(a,VoiceCommands.COMMAND))' in ctl
assert 'b.setOnLongClickListener(v -> { menu(a); return true; })' in ctl
assert 'mode==VoiceCommands.COMMAND' in ctl and 'mode==VoiceCommands.TYPING' in ctl
assert 'setContentDescription(label+state+' in ctl and 'WeakHashMap<ImageButton,Integer>' in ctl
assert service.count('VoiceController.refreshButtons()')>=2
assert 'R.drawable.ic_voice_mic' in service # solid notification small icon remains legible
home=(java/'MainActivity.java').read_text()
assert 'VoiceController.setButtonColor(voiceButton,0xFFFFCD6A)' in home
assert 'mic.setElevation(4f*' in home
assert 'new FrameLayout.LayoutParams(micSize,micSize,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL)' in home
assert 'Math.round(root.getHeight() - top + pixelsPerMm + 11f * cssPixel)' in home
assert 'voiceButton.setTranslationX(-40f * cssPixel)' in home
assert 'if (voiceParams.bottomMargin != margin)' in home
assert 'micParams.topMargin' not in home and 'micParams.rightMargin' not in home
# Relative to v0.1.193, move only the voice target 1 preview pixel left
# and 1 preview pixel up, even when the bolt is hidden.
for width,height in ((269,597),(720,1600),(1080,2400),(1224,2720)):
    mm=width/70.9; css=mm*25.4/96
    text_bottom=height-round(height*.012)
    bolt_top=text_bottom-2.75*css-2*mm
    previous_bottom=height-round(height-bolt_top+mm+10*css)
    voice_bottom=height-round(height-bolt_top+mm+11*css)
    assert abs((previous_bottom-voice_bottom)-css)<1.01
    previous_center=width/2-39*css
    voice_center=width/2-40*css
    assert abs((previous_center-voice_center)-css)<1e-9
assert 'VoiceController.button' not in (java/'AppUi.java').read_text()
assert 'VoiceController.button' not in (java/'CalendarActivity.java').read_text()
assert 'VoiceController.attachFloatingButton(this)' in (java/'BottomSwipeActivity.java').read_text()
assert 'Gravity.BOTTOM|android.view.Gravity.RIGHT' in ctl and 'voice_fab' in ctl and 'a instanceof MainActivity' in ctl
print('Voice icon generation, click/long-click, state, theme and lifecycle wiring: OK')
