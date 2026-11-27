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
assert len(generator.ROWS)==13 and all(len(row)==13 for row in generator.ROWS)
icon=ET.parse(path).getroot()
ns='{http://schemas.android.com/apk/res/android}'
assert icon.get(ns+'viewportWidth')=='13' and icon.get(ns+'viewportHeight')=='13'
assert icon.find('path').get(ns+'pathData').count('a.36,.36') >= 50
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
assert 'VoiceController.button' in (java/'AppUi.java').read_text()
assert 'VoiceController.button' in (java/'CalendarActivity.java').read_text()
print('Voice icon generation, click/long-click, state, theme and lifecycle wiring: OK')
