#!/usr/bin/env python3
"""Generate matching Doto dot-matrix voice waveforms for Android and preview."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
# Five symmetric dot columns preserve the supplied waveform silhouette.
# Coordinates are within the same 13×13 grid as the other launcher icons.
BARS=[(1,5,7),(3.5,2,10),(6,0,12),(8.5,2,10),(11,5,7)]
RADIUS=.36

def points():
 return [(x+.5,y+.5) for x,start,end in BARS for y in range(start,end+1)]

def generate():
 path=' '.join(f'M{x-RADIUS:g},{y:g} a.36,.36 0 1,0 .72,0 a.36,.36 0 1,0 -.72,0 z' for x,y in points())
 vector=('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="24dp" android:height="24dp"\n    android:viewportWidth="13" android:viewportHeight="13">\n'
         f'    <path android:fillColor="#FFFFFF" android:pathData="{path}" />\n</vector>\n')
 return ROOT/'app/src/main/res/drawable/ic_voice_command.xml',vector

def generate_svg():
 circles='\n'.join(f'  <circle cx="{x:g}" cy="{y:g}" r=".36" />' for x,y in points())
 svg=f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 13 13" fill="#ffffff">\n{circles}\n</svg>\n'
 return ROOT/'preview/assets/voice-command.svg',svg
if __name__=='__main__':
 for path,content in (generate(),generate_svg()): path.write_text(content,encoding='utf8')
