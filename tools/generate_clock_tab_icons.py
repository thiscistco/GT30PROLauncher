#!/usr/bin/env python3
"""Generate matching dotted clock-tab icons for Android and the web preview.

The small circular dots echo the bundled Doto font. The same grids are used for
both renderers. Run from any directory with: python3 tools/generate_clock_tab_icons.py
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ICONS = {
    # Clear, distinct symbols, drawn on the same 13 by 13 dot grid as Calendar.
    'alarm': [
        '......#......', '.....###.....', '....#...#....', '...#.....#...',
        '...#.....#...', '...#.....#...', '...#.....#...', '..#.......#..',
        '.#.........#.', '.###########.', '.....#.#.....', '......#......',
        '.............',
    ],
    'world': [
        '....#####....', '..##..#..##..', '.#...#.#...#.', '.#...#.#...#.',
        '#...#...#...#', '#...#...#...#', '#############', '#...#...#...#',
        '#...#...#...#', '.#...#.#...#.', '.#...#.#...#.', '..##..#..##..',
        '....#####....',
    ],
    'timer': [
        '..#########..', '...#.....#...', '...#..#..#...', '....#.#.#....',
        '.....###.....', '......#......', '......#......', '.....#.#.....',
        '....#...#....', '...#..#..#...', '...#..#..#...', '..#########..',
        '.............',
    ],
    'stopwatch': [
        '.....###.....', '......#...##.', '....#####..#.', '..##.....##..',
        '..#.......#..', '.#....#....#.', '.#....#....#.', '.#....##...#.',
        '.#......#..#.', '..#.......#..', '..##.....##..', '....#####....',
        '.............',
    ],
}


def generate():
    for name, rows in ICONS.items():
        assert len(rows) == 13 and all(len(row) == 13 and set(row) <= {'.', '#'} for row in rows)
        points = [(x, y) for y, row in enumerate(rows) for x, value in enumerate(row) if value == '#']
        circles = '\n'.join(f'  <circle cx="{x + .5:g}" cy="{y + .5:g}" r=".36" />' for x, y in points)
        svg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 13 13" fill="#ffd49d">\n{circles}\n</svg>\n'
        # A circular path for each dot; Android VectorDrawable has no circle node.
        path = ' '.join(f'M{x + .14:g},{y + .5:g} a.36,.36 0 1,0 .72,0 a.36,.36 0 1,0 -.72,0 z' for x, y in points)
        vector = ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
                  '    android:width="32dp" android:height="32dp"\n'
                  '    android:viewportWidth="13" android:viewportHeight="13">\n'
                  f'    <path android:fillColor="#FFD49D" android:pathData="{path}" />\n'
                  '</vector>\n')
        yield ROOT / f'preview/assets/clock-{name}.svg', svg
        yield ROOT / f'app/src/main/res/drawable/ic_clock_{name}.xml', vector


if __name__ == '__main__':
    for path, content in generate():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding='utf-8')
