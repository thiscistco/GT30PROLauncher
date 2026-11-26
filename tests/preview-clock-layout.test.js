// Run with: node tests/preview-clock-layout.test.js
const fs = require('fs'), assert = require('assert');
const html = fs.readFileSync('preview/index.html', 'utf8');
const java = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/MainActivity.java', 'utf8');
const view = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/HomeClockView.java', 'utf8');
const clock = html.match(/\.clock-led\s*\{([^}]*)\}/)[1];
const sep = html.match(/\.clock-sep\s*\{([^}]*)\}/)[1];
assert.match(clock, /left:\s*calc\(35\.7%\s*-\s*0\.5px\)/);
assert.match(clock, /bottom:\s*calc\(6%\s*\+\s*1\.55px\)/);
assert.match(clock, /font-size:\s*2mm/);
assert.match(clock, /font-weight:\s*800/);
assert.match(clock, /font-variation-settings:\s*'wght'\s*800/);
assert.match(clock, /letter-spacing:\s*0\.2mm/);
assert.match(clock, /padding-bottom:\s*0\.5px/);
assert.match(sep, /height:\s*0\.22mm/);
assert.match(sep, /margin:\s*calc\(0\.461mm - 1px\) 0 calc\(0\.45mm \+ 0\.5px\) 0/);
const digits = html.match(/\.clock-led > span:not\(\.clock-sep\),\s*\.date-led > span\s*\{([^}]*)\}/)[1];
// Both home overlays use the circuit animation's orange halo and golden core.
assert.match(digits, /#ff6919 0%, #ff9d2f 24%, #ffcd6a 50%, #ff9d2f 76%, #ff6919 100%/);
assert.match(digits, /drop-shadow\(0 0 0\.4mm rgba\(255, 105, 25, 0\.65\)\)/);
assert.match(sep, /#ffcd6a 0%, #ff9d2f 55%, #ff6919 90%/);
const dateView = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/HomeDateView.java', 'utf8');
for (const source of [view, dateView]) {
  assert.match(source, /0xFFFF6919, 0xFFFF9D2F, 0xFFFFCD6A, 0xFFFF9D2F, 0xFFFF6919/);
  assert.match(source, /0xA6FF6919/);
}
assert.match(java, /new HomeClockView\(this\)/);
assert.match(java, /new FrameLayout\.LayoutParams\(1, 1, Gravity\.TOP \| Gravity\.LEFT\)/);
assert.match(java, /params\.width = boxWidth/);
assert.match(java, /params\.height = boxHeight/);
assert.match(java, /clockBox\.setElevation\(4f/);
assert.match(java, /clockLed\.setVisibility\(View\.VISIBLE\)/);
assert.match(java, /clockLed\.bringToFront\(\)/);
assert.match(java, /clockLed\.setTime\(t\.substring\(0, 2\), t\.substring\(3, 5\)\)/);
assert.match(java, /clockLed\.setSeparatorAlpha\(\(float\) a\.getAnimatedValue\(\)\)/);
assert.match(java, /width \* \.357f - \.5f \* cssPixel - boxWidth \/ 2f/);
assert.match(java, /height \* \.06f \+ 1\.55f \* cssPixel/);
assert.match(view, /setLayerType\(View\.LAYER_TYPE_SOFTWARE, null\)/);
assert.match(view, /text\.setTextSize\(2f \* pixelsPerMm\)/);
assert.match(view, /text\.setFontVariationSettings\("'wght' 800"\)/);
assert.match(view, /text\.setShadowLayer\(\.4f \* pixelsPerMm/);
assert.match(view, /for \(int i = 0; i < 5; i\+\+\)/);
assert.match(view, /dots\.setAlpha\(Math\.round\(separatorAlpha \* 255\)\)/);
assert.match(view, /drawDigits\(canvas, hour/);
assert.match(view, /drawDigits\(canvas, minute, dotTop \+ \.22f \* pxPerMm \+ \.45f \* pxPerMm \+ \.5f \* cssPx, line\)/);
// Explicit view bounds include glow padding, while the content remains anchored
// to the preview's 35.7% center and 6% + 1.55 CSS px bottom position.
for (const [w,h] of [[269,596],[1224,2720],[720,1600]]) {
  const mm = w/70.9, css = mm*25.4/96;
  const boxWidth = Math.round(3.6*mm), boxHeight = Math.round((5.731+.8)*mm);
  const left = w*.357 - .5*css - boxWidth/2;
  const top = h - (h*.06 + 1.55*css) - boxHeight + .4*mm;
  assert(Math.abs(left + boxWidth/2 - (w*.357 - .5*css)) < 1e-9);
  assert(Math.abs((h-(top+boxHeight-.4*mm)) - (h*.06 + 1.55*css)) < 1e-9);
  // The clock box keeps its former height; only the minute line moves up.
  const dotTop = top + .4*mm + 2.3*mm + .461*mm - css;
  const oldMinuteTop = dotTop + .22*mm + .45*mm + css;
  const minuteTop = dotTop + .22*mm + .45*mm + .5*css;
  assert(Math.abs((oldMinuteTop-minuteTop) - .5*css) < 1e-9);
}
console.log('Explicit Android clock rendering and preview-relative placement: OK');
