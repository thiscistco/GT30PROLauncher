// Run with: node tests/preview-battery-indicator.test.js
const assert = require('assert'), fs = require('fs');
const html = fs.readFileSync('preview/index.html', 'utf8');
const java = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/MainActivity.java', 'utf8');
const clock = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/HomeClockView.java', 'utf8');
const boltJava = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/BatteryBoltView.java', 'utf8');
const css = html.match(/\.battery-led\s*\{([^}]*)\}/)[1];
assert.match(css, /left:\s*calc\(50%\s*\+\s*0\.5px\)/);
assert.match(css, /bottom:\s*calc\(1\.2%\s*-\s*1px\)/);
assert.match(css, /color:\s*#ffffff/);
assert.match(css, /opacity:\s*0\.6/);
assert.match(html, /id="batteryLed" role="img" aria-label="Baterai 85 persen">85<svg class="battery-bolt"/);
assert.match(java, /int percent = level \* 100 \/ scale/);
assert.match(java, /batteryLed\.setText\(String\.valueOf\(percent\)\)/);
assert.match(java, /batteryLed\.setContentDescription\("Baterai " \+ percent \+ " persen"/);
assert.match(java, /batteryLed\.setText\("85"\)/);
const offset = java.match(/private void updateBatteryOffset\([\s\S]*?\n    }/)[0];
assert.match(offset, /pixelsPerMm = root\.getWidth\(\) \/ 70\.9f/);
assert.match(offset, /cssPixel = pixelsPerMm \* 25\.4f \/ 96f/);
assert.match(offset, /batteryBox\.setTranslationX\(\.5f \* cssPixel\)/);
assert.match(offset, /batteryBox\.setTranslationY\(cssPixel\)/);
assert.match(offset, /float top = batteryBox\.getBottom\(\) \+ 1\.25f \* cssPixel/);
assert.match(java, /updateBatteryOffset\(homeRoot, batteryLed\)/);
const boltCss = html.match(/\.battery-bolt\s*\{([^}]*)\}/)[1];
assert.match(boltCss, /display:\s*none/);
assert.match(boltCss, /top:\s*calc\(100% \+ 0\.25px\)/);
assert.match(boltCss, /width:\s*1mm/); assert.match(boltCss, /height:\s*1\.4mm/);
assert.match(html, /\.battery-led\.charging \.battery-bolt\s*\{\s*display:\s*block/);
assert.match(boltJava, /WIDTH_MM = 1f/); assert.match(boltJava, /HEIGHT_MM = 1\.4f/);
assert.match(boltJava, /ROWS = \{6, 12, 12, 30, 6, 12, 8\}/);
// Compare every dotted cell in the preview SVG with the Android matrix.
const svg = html.match(/<svg class="battery-bolt"[\s\S]*?<\/svg>/)[0];
const svgRows = Array(7).fill(0);
for (const [, x, y] of svg.matchAll(/<circle cx="([\d.]+)" cy="([\d.]+)" r="0\.32"\/>/g)) {
  const col = Number(x)-.5, row = Number(y)-.5;
  assert(Number.isInteger(col) && col>=0 && col<5 && Number.isInteger(row) && row>=0 && row<7);
  svgRows[row] |= 1 << (4-col);
}
assert.deepStrictEqual(svgRows, [6, 12, 12, 30, 6, 12, 8]);
for (const width of [269, 720, 1224]) {
  const mm=width/70.9, cssPixel=mm*25.4/96, height=width*2720/1224;
  const room=height*.012-cssPixel; // new battery bottom margin
  assert(room > .25*cssPixel + 1.4*mm, 'bolt fits below battery on the target aspect ratio');
}
const charge = java.match(/private void updateChargeAnim\(\) \{[\s\S]*?\n    }/)[0];
assert.match(charge, /ValueAnimator\.ofFloat\(\.21f, \.6f\)/);
assert.match(charge, /setDuration\(900\)/);
assert.match(charge, /setRepeatMode\(ValueAnimator\.REVERSE\)/);
assert.match(charge, /batteryLed\.setAlpha\(alpha\)/);
assert.match(charge, /batteryBolt\.setAlpha\(alpha\)/);
assert.match(charge, /batteryBolt\.setVisibility\(View\.VISIBLE\)/);
assert.match(charge, /batteryBolt\.setVisibility\(View\.INVISIBLE\)/);
assert(!/clockLed|dateLed|setTime/.test(charge), 'charging affects the battery indicator only');
assert(!/separatorAlpha/.test(clock), 'clock separator remains steady');
assert.match(html, /\.battery-led\.charging\s*\{\s*animation:\s*batteryPulse 0\.9s ease-in-out infinite alternate/);
assert.match(html, /@keyframes batteryPulse\s*\{\s*from\s*\{\s*opacity:\s*0\.21;\s*\}\s*to\s*\{\s*opacity:\s*0\.6;/);
assert.match(html, /battery\.classList\.toggle\('charging', toggle\.checked\)/);
assert.match(html, /prefers-reduced-motion: reduce\)\s*\{\s*\.battery-led\.charging\s*\{\s*animation:\s*none/);
console.log('Battery position, live value, Doto-style bolt and battery-only charging animation: OK');
