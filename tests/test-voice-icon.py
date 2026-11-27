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
assert len(generator.BARS)==5 and generator.WIDTH==3
bars=generator.BARS
assert [bar[0] for bar in bars]==[1.5,6.75,12,17.25,22.5]
assert bars[0][1:]==bars[4][1:] and bars[1][1:]==bars[3][1:]
assert all(abs((top+bottom)/2-12)<1e-8 for _,top,bottom in bars)
assert bars[0][2]-bars[0][1] < bars[1][2]-bars[1][1] < bars[2][2]-bars[2][1]
icon=ET.parse(path).getroot()
ns='{http://schemas.android.com/apk/res/android}'
assert icon.get(ns+'viewportWidth')=='24' and icon.get(ns+'viewportHeight')=='24'
assert icon.find('path').get(ns+'pathData').count('M')==5
assert icon.find('path').get(ns+'strokeLineCap')=='round'
svgroot=ET.parse(svg).getroot()
assert svgroot.get('stroke-linecap')=='round' and len(list(svgroot))==5
java=root/'app/src/main/java/com/gt30pro/launcher'
ctl=(java/'VoiceController.java').read_text(); service=(java/'VoiceService.java').read_text()
assert 'R.drawable.ic_voice_command' in ctl
assert 'b.setOnClickListener(v -> toggle(a,VoiceCommands.COMMAND))' in ctl
assert 'b.setOnLongClickListener(v -> { menu(a); return true; })' in ctl
assert 'mode==VoiceCommands.COMMAND' in ctl and 'mode==VoiceCommands.TYPING' in ctl
assert 'setContentDescription(label+state+' in ctl and 'WeakHashMap<ImageButton,Integer>' in ctl
assert service.count('VoiceController.refreshButtons()')>=2
assert 'R.drawable.ic_voice_mic' in service # solid notification small icon remains legible
home=(java/'MainActivity.java').read_text()
assert 'VoiceController.setButtonColor(voiceButton,dark ?' in home
assert 'mic.setElevation(4f*' in home
assert 'new FrameLayout.LayoutParams(micSize,micSize,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL)' in home
assert 'Math.round(root.getHeight() - top + pixelsPerMm)' in home
assert 'voiceButton.setTranslationX(cssPixel)' in home
assert 'if (voiceParams.bottomMargin != margin)' in home
assert 'micParams.topMargin' not in home and 'micParams.rightMargin' not in home
# The position uses the bolt's original coordinates, not the shifted battery text,
# with a 1mm gap between the touch target and bolt even when not charging.
for width,height in ((269,597),(720,1600),(1080,2400),(1224,2720)):
    mm=width/70.9; css=mm*25.4/96
    text_bottom=height-round(height*.012)
    bolt_top=text_bottom-2.75*css-2*mm
    voice_bottom=height-round(height-bolt_top+mm)
    assert abs((bolt_top-voice_bottom)-mm)<.51
    bolt_center=width/2+css
    voice_center=width/2+css
    assert abs(voice_center-bolt_center)<1e-9
assert 'VoiceController.button' in (java/'AppUi.java').read_text()
assert 'VoiceController.button' in (java/'CalendarActivity.java').read_text()
print('Voice icon generation, click/long-click, state, theme and lifecycle wiring: OK')
