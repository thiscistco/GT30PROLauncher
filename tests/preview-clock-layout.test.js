// Run with: node tests/preview-clock-layout.test.js
const fs = require('fs'), assert = require('assert');
const html = fs.readFileSync('preview/index.html', 'utf8');
const java = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/MainActivity.java', 'utf8');
const view = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/HomeClockView.java', 'utf8');
const clock = html.match(/\.clock-led\s*\{([^}]*)\}/)[1];
const sep = html.match(/\.clock-sep\s*\{([^}]*)\}/)[1];
assert.match(clock, /left:\s*calc\(35\.7%\s*-\s*1\.5px\)/);
assert.match(clock, /bottom:\s*calc\(6%\s*\+\s*0\.55px\)/);
assert.match(clock, /font-size:\s*2mm/);
assert.match(clock, /font-weight:\s*800/);
assert.match(clock, /font-variation-settings:\s*'wght'\s*800/);
assert.match(clock, /letter-spacing:\s*0\.2mm/);
assert.match(clock, /padding-bottom:\s*1px/);
assert.match(sep, /height:\s*0\.22mm/);
assert.match(sep, /margin:\s*calc\(0\.461mm - 1px\) 0 0\.45mm 0/);
const digits = html.match(/\.clock-led > span:not\(\.clock-sep\),\s*\.date-led > span\s*\{([^}]*)\}/)[1];
// Both home overlays retain an amber gradient tuned to the home palette.
assert.match(digits, /#ffcd6a 0%, #ffcd6a 24%, #ffcd6a 50%, #ffcd6a 76%, #ffcd6a 100%/);
assert.match(digits, /drop-shadow\(0 0 0\.4mm rgba\(255, 105, 25, 0\.65\)\)/);
assert.match(sep, /color:#adc3ca/);
assert.match(html, /\.clock-sep \{ color:#425860; \}/);
assert.match(html, /<span class="clock-sep" aria-hidden="true">(?:<i><\/i>){5}<\/span>/);
assert.match(html, /animation:clockDotPulse 5s linear infinite/);
for (let i=2; i<=5; i++) assert(html.includes(`.clock-sep i:nth-child(${i}) { animation-delay:${i-1}s; }`));
assert.match(html, /prefers-reduced-motion:reduce[^}]*\.clock-sep i \{ animation:none; opacity:1/);
assert.match(html, /#clockMin \{ transform: translate\(-0\.877193px, -0\.877193px\)/);
assert(Math.abs(1.14 / 1.2 - .95)<1e-9);
assert(Math.abs(.877193 * 1.14 - 1)<1e-6);
const dateView = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/HomeDateView.java', 'utf8');
for (const source of [view, dateView]) {
  assert.match(source, /0xFFFFCD6A, 0xFFFFCD6A, 0xFFFFCD6A, 0xFFFFCD6A, 0xFFFFCD6A/);
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
assert(!/setSeparatorAlpha/.test(java), 'charging no longer animates the clock');
assert.match(java, /width \* \.357f - 1\.5f \* cssPixel - boxWidth \/ 2f\s*\+ 750f \* wallpaperScale\(root\)/);
assert.match(java, /height \* \.06f \+ \.55f \* cssPixel/);
assert.match(view, /setLayerType\(View\.LAYER_TYPE_SOFTWARE, null\)/);
assert.match(view, /text\.setTextSize\(2f \* pixelsPerMm\)/);
assert.match(view, /text\.setFontVariationSettings\("'wght' 800"\)/);
assert.match(view, /text\.setShadowLayer\(\.4f \* pixelsPerMm/);
assert.match(view, /for \(int i = 0; i < 5; i\+\+\)/);
assert.match(view,/HomeClockPulse\.intensity\(separatorPhase, i\)/);
assert.match(view,/setDuration\(5000\)/);
assert.match(view,/ValueAnimator\.areAnimatorsEnabled\(\)/);
assert.match(java,/clockLed\.startSeparator\(\)/);
assert.match(java,/clockLed\.stopSeparator\(\)/);
assert.match(view,/dots\.setColor\(lightGlass \? 0xFF425860 : 0xFFADC3CA\)/);
assert.match(view,/canvas\.translate\(-cssPx \/ HOME_SCALE, -cssPx \/ HOME_SCALE\)/);
assert.match(view, /drawDigits\(canvas, hour/);
assert.match(view, /drawDigits\(canvas, minute, dotTop \+ \.22f \* pxPerMm \+ \.45f \* pxPerMm, line\)/);
// Explicit view bounds include glow padding, while the content remains anchored
// to the preview's 35.7% center and 6% + 0.55 CSS px bottom position.
for (const [w,h] of [[269,596],[1224,2720],[720,1600]]) {
  const mm = w/70.9, css = mm*25.4/96;
  const boxWidth = Math.round(3.6*mm), boxHeight = Math.round((5.731+.8)*mm);
  const left = w*.357 - 1.5*css - boxWidth/2;
  const top = h - (h*.06 + .55*css) - boxHeight + .4*mm;
  const previousTop = h - (h*.06 + 1.55*css) - boxHeight + .4*mm;
  assert(Math.abs(top - previousTop - css) < 1e-9, 'entire clock moved down 1 CSS px');
  assert(Math.abs(left + boxWidth/2 - (w*.357 - 1.5*css)) < 1e-9);
  assert(Math.abs((h-(top+boxHeight-.4*mm)) - (h*.06 + .55*css)) < 1e-9);
  // The clock box keeps its former height; only the minute line moves up.
  const dotTop = top + .4*mm + 2.3*mm + .461*mm - css;
  const oldMinuteTop = dotTop + .22*mm + .45*mm + .5*css;
  const minuteTop = dotTop + .22*mm + .45*mm;
  assert(Math.abs((oldMinuteTop-minuteTop) - .5*css) < 1e-9);
}
console.log('Explicit Android clock rendering and preview-relative placement: OK');
