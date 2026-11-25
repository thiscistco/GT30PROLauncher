#!/usr/bin/env python3
"""Correct the two small chip labels in the supplied 1224x2720 render.

Usage: python tools/edit_chip_label.py INPUT.jpeg OUTPUT.png
The original is decoded unchanged outside two small text rectangles; PNG avoids
recompressing the rest of the image. The matching Doto font is bundled by the app.
Requires Pillow.
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont

if len(sys.argv) != 3:
    raise SystemExit(__doc__)
root = Path(__file__).resolve().parents[1]
image = Image.open(sys.argv[1]).convert('RGB')
if image.size != (1224, 2720):
    raise SystemExit('Expected the supplied 1224x2720 image.')

# Only the malformed two lines are replaced; never touch the MediaTek logo,
# the QR-like marking, battery, cable, or surrounding motherboard.
rectangles = [(523, 695, 698, 720), (523, 720, 632, 740)]  # right/bottom exclusive
source = image.copy()

def sample_background(x, y):
    # Average a few neighbouring pixels to avoid extending JPEG/text artifacts
    # vertically through the reconstructed background.
    samples = [source.getpixel((x + dx, y)) for dx in range(-3, 4)]
    return tuple(sum(pixel[c] for pixel in samples) / len(samples) for c in range(3))

for x0, y0, x1, y1 in rectangles:
    for x in range(x0, x1):
        # The QR-like marking begins below y=740 on the right, so never
        # sample from it when filling the longer first line.
        bottom_y = 742 if x < 632 else 720
        top = sample_background(x, 693)
        bottom = sample_background(x, bottom_y)
        for y in range(y0, y1):
            t = (y - 693) / (bottom_y - 693)
            image.putpixel((x, y), tuple(round(a * (1-t) + b * t) for a, b in zip(top, bottom)))

# Render at 4x for clean, consistently sized small Doto lettering.
scale = 4
font = ImageFont.truetype(str(root / 'app/src/main/assets/fonts/Doto.ttf'), 18 * scale)
font.set_variation_by_axes([0, 700])
for text, (x0, y0, x1, y1), x, y in [
        ('Dimensity 8350', rectangles[0], 526, 697),
        ('Ultimate', rectangles[1], 526, 722)]:
    mask = Image.new('L', ((x1-x0)*scale, (y1-y0)*scale), 0)
    ImageDraw.Draw(mask).text(((x-x0)*scale, (y-y0)*scale), text, fill=220, font=font, anchor='lt')
    mask = mask.resize((x1-x0, y1-y0), Image.Resampling.LANCZOS).filter(ImageFilter.GaussianBlur(0.25))
    image.paste((168, 166, 175), (x0, y0, x1, y1), mask)
image.save(sys.argv[2], format='PNG', optimize=True)
