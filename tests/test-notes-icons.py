#!/usr/bin/env python3
"""Verify matching matrix icons and accessibility controls in both clients."""
from pathlib import Path
import re,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]; html=(root/'preview/index.html').read_text(); activity=(root/'app/src/main/java/com/gt30pro/launcher/NotesActivity.java').read_text()
for name in ('notes','archive','trash','list','grid','staggered','tag','checklist','done','backup','pin','unpin','restore','color'):
 svg=ET.parse(root/f'preview/assets/notes-{name}.svg').getroot(); nodes=list(svg); assert len(nodes)>5
 vector=ET.parse(root/f'app/src/main/res/drawable/ic_notes_{name}.xml').getroot()
 path=list(vector)[0].get('{http://schemas.android.com/apk/res/android}pathData')
 coords=re.findall(r'M([0-9.]+),([0-9.]+) a\.39,\.39',path)
 assert len(nodes)==len(coords),name
 for node,(x,y) in zip(nodes,coords): assert abs(float(node.get('cx'))-float(x)-.39)<1e-6 and float(node.get('cy'))==float(y)
 assert f'.notes-icon-{name}' in html
for name in ('notesSection','notesLayout','notesAdd','notesTag','notesAddChecklist'):
 assert re.search(r'<(?:button|select)\b[^>]*id="'+name+r'"[^>]*aria-label=',html),name
assert 'aria-label="Selesai"' in html and 'setTooltipText(description)' in activity
assert 'iconGlass(' not in activity
print('Notes icons: matching Android/preview and accessible names OK')
