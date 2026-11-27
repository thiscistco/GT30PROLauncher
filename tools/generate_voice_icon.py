#!/usr/bin/env python3
"""Generate Android and preview versions of the five-bar rounded voice waveform."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
# Line caps extend 1.5 units beyond these endpoints; symmetry and ratios follow
# the supplied reference: short outer bars, mid-height inner bars and tall center.
BARS=[(1.5,11.1,12.9),(6.75,6.4,17.6),(12,1.5,22.5),(17.25,6.4,17.6),(22.5,11.1,12.9)]
WIDTH=3

def generate():
 path=' '.join(f'M{x:g},{top:g} L{x:g},{bottom:g}' for x,top,bottom in BARS)
 vector=('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="24dp" android:height="24dp"\n    android:viewportWidth="24" android:viewportHeight="24">\n'
         f'    <path android:fillColor="@android:color/transparent" android:strokeColor="#FFFFFF" android:strokeWidth="{WIDTH}" android:strokeLineCap="round" android:pathData="{path}" />\n</vector>\n')
 return ROOT/'app/src/main/res/drawable/ic_voice_command.xml',vector

def generate_svg():
 bars='\n'.join(f'  <line x1="{x:g}" y1="{top:g}" x2="{x:g}" y2="{bottom:g}" />' for x,top,bottom in BARS)
 svg=f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="{WIDTH}" stroke-linecap="round">\n{bars}\n</svg>\n'
 return ROOT/'preview/assets/voice-command.svg',svg
if __name__=='__main__':
 for path,content in (generate(),generate_svg()): path.write_text(content,encoding='utf8')
