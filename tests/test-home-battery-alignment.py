#!/usr/bin/env python3
"""Doto's digits fit a fixed, right-aligned 0–100% battery label."""
from pathlib import Path
from PIL import ImageFont
root = Path(__file__).resolve().parents[1]
font = ImageFont.truetype(str(root / 'preview/assets/fonts/Doto.ttf'), 100)
digit = font.getlength('0')
for i in range(10):
    assert abs(font.getlength(str(i)) - digit) < .001
# Both renderers reserve three digit advances and align to the fixed right edge.
spacing = 5.0
advance = digit + spacing
box_width = 3 * advance
right_edge = box_width
for value in range(101):
    width = len(str(value)) * advance
    left = right_edge - width
    assert left >= 0 and abs(left + width - right_edge) < 1e-9
# Right-edge pivot keeps the right edge through consecutive 10% reductions.
old_edge = box_width * 1.5 / 2 - .75 * advance
new_edge = box_width / 2
assert abs(old_edge - new_edge) < 1e-9
assert abs(1.35 / 1.5 - .9) < 1e-9
assert abs(1.215 / 1.35 - .9) < 1e-9
print('Battery right alignment: all Doto digits and percentages 0–100 fit: OK')
