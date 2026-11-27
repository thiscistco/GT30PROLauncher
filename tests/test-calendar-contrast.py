#!/usr/bin/env python3
"""Check calendar text palettes against the actual rendered backgrounds."""
import re
from pathlib import Path
html = Path('preview/index.html').read_text()
def palette(selector):
    rule = next(r for r in re.findall(selector + r'\s*\{([^}]+)\}', html) if '--cal-bg:' in r)
    return dict(re.findall(r'--(cal-[\w-]+):\s*(#[0-9a-fA-F]+)', rule))
def rgb(v):
    v = v.lstrip('#')
    if len(v) == 3: v = ''.join(c*2 for c in v)
    return [int(v[i:i+2], 16)/255 for i in (0,2,4)]
def lum(v):
    c = [x/12.92 if x <= .04045 else ((x+.055)/1.055)**2.4 for x in rgb(v)]
    return .2126*c[0]+.7152*c[1]+.0722*c[2]
def contrast(a,b):
    a,b = sorted((lum(a),lum(b)))
    return (b+.05)/(a+.05)
for p in (palette(r'\.calendar-page'), palette(r'\.calendar-page\[data-theme="dark"\]')):
    for fg,bgs in {'cal-text':['cal-bg','cal-card','cal-control','cal-day','cal-event'], 'cal-muted':['cal-bg','cal-card','cal-event'], 'cal-weekend':['cal-card','cal-day'], 'cal-selected-text':['cal-select'], 'cal-selected-weekend':['cal-select'], 'cal-observe':['cal-bg','cal-card']}.items():
        for bg in bgs:
            ratio=contrast(p[fg],p[bg]); assert ratio >= 4.5, (fg,bg,ratio)
assert '.calendar-day { color:var(--cal-text); }' in html
assert '.calendar-day.selected { color:var(--cal-selected-text); }' in html
print('Calendar palette contrast >= 4.5:1: OK')
