// Run with: node tests/preview-clock-layout.test.js
const fs = require('fs'), assert = require('assert');
const html = fs.readFileSync('preview/index.html', 'utf8');
const java = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/MainActivity.java', 'utf8');
const clock = html.match(/\.clock-led\s*\{([^}]*)\}/)[1];
const sep = html.match(/\.clock-sep\s*\{([^}]*)\}/)[1];
assert.match(clock, /left:\s*calc\(35\.7%\s*-\s*0\.5px\)/);
assert.match(clock, /bottom:\s*calc\(6%\s*\+\s*2\.25px\)/);
assert.match(clock, /font-size:\s*2mm/);
assert.match(clock, /letter-spacing:\s*0\.2mm/);
assert.match(sep, /height:\s*0\.22mm/);
assert.match(java, /clockSizePx\s*=\s*width\s*\*\s*\(2f\s*\/\s*70\.9f\)/);
assert.match(java, /setLetterSpacing\(0\.2f\s*\/\s*2f\)/);
assert.match(java, /clockBox\.setTranslationX\(-0\.5f\s*\*\s*cssPixel\)/);
assert.match(java, /clockBox\.setTranslationY\(0\.75f\s*\*\s*cssPixel\)/);
assert.match(java, /params\.rightMargin\s*=\s*Math\.round\(width\s*\*\s*0\.143f\)/);
assert.match(java, /params\.bottomMargin\s*=\s*Math\.round\(height\s*\*\s*0\.06f\s*\+\s*3f\s*\*\s*cssPixel\)/);
// FrameLayout subtracts the full right margin from its centered child.
// Only layout/margin rounding can introduce up to a pixel of variation;
// the requested offsets remain subpixel floats.
for (const [w,h] of [[269,596],[1224,2720],[720,1600]]) {
  const cssPixel = w/(70.9*96/25.4);
  const expectedX = w*.357 - .5*cssPixel;
  const androidX = w/2 - Math.round(w*.143) - .5*cssPixel;
  assert(Math.abs(expectedX-androidX) <= .501, 'center position differs by more than margin rounding');
  const expectedBottom = h*.06 + 2.25*cssPixel;
  const androidBottom = Math.round(h*.06 + 3*cssPixel) - .75*cssPixel;
  assert(Math.abs(expectedBottom-androidBottom) <= .501, 'bottom position differs by more than margin rounding');
  assert(Math.abs(w*(2/70.9) - (2*96/25.4)*cssPixel) < 1e-10);
}
console.log('Clock size and relative subpixel offsets match preview and Android layout: OK');
