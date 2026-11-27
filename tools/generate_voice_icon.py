#!/usr/bin/env python3
"""Generate the same 13×13 dot-matrix microphone style as Calendar controls."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ROWS=[
 '.............',
 '.....###.....',
 '....#...#....',
 '....#...#....',
 '....#...#....',
 '..#.#...#.#..',
 '..#..###..#..',
 '..#.......#..',
 '...#.....#...',
 '....#####....',
 '......#......',
 '....#####....',
 '.............',
]
def generate():
 assert len(ROWS)==13 and all(len(r)==13 and set(r)<={'.','#'} for r in ROWS)
 pts=[(x,y) for y,row in enumerate(ROWS) for x,c in enumerate(row) if c=='#']
 path=' '.join(f'M{x+.14:g},{y+.5:g} a.36,.36 0 1,0 .72,0 a.36,.36 0 1,0 -.72,0 z' for x,y in pts)
 vector=('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="24dp" android:height="24dp"\n    android:viewportWidth="13" android:viewportHeight="13">\n'
         f'    <path android:fillColor="#FFFFFF" android:pathData="{path}" />\n</vector>\n')
 return ROOT/'app/src/main/res/drawable/ic_voice_command.xml',vector
def generate_svg():
 pts=[(x,y) for y,row in enumerate(ROWS) for x,c in enumerate(row) if c=='#']
 dots='\n'.join(f'  <circle cx="{x+.5:g}" cy="{y+.5:g}" r=".36" />' for x,y in pts)
 svg=f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 13 13" fill="#ffffff">\n{dots}\n</svg>\n'
 return ROOT/'preview/assets/voice-command.svg',svg
if __name__=='__main__':
 for path,content in (generate(),generate_svg()): path.write_text(content,encoding='utf8')
