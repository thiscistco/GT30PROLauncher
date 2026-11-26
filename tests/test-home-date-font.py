#!/usr/bin/env python3
"""Doto labels should match the visible width of the two-digit date."""
from pathlib import Path
from PIL import ImageFont

font_path = Path(__file__).resolve().parents[1] / 'preview/assets/fonts/Doto.ttf'
large = ImageFont.truetype(str(font_path), 1000)
small = ImageFont.truetype(str(font_path), 650)
for font in (large, small):
    font.set_variation_by_axes([0, 800])

# Android and visible browser glyphs use spacing only between characters.
def width(font, text, gap):
    return sum(font.getlength(ch) for ch in text) + (len(text) - 1) * gap

labels = 'SEN SEL RAB KAM JUM SAB MIN JAN FEB MAR APR MEI JUN JUL AGU SEP OKT NOV DES'.split()
for day in range(1, 32):
    number_width = width(large, f'{day:02}', 100)
    for label in labels:
        label_width = width(small, label, 65)
        assert abs(label_width - number_width) < 0.1, (label, day, label_width, number_width)
print('Home date: all Doto day/month labels match all two-digit dates in visible width: OK')
