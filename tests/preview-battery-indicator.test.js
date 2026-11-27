// Run with: node tests/preview-battery-indicator.test.js
const assert = require('assert'), fs = require('fs');
const html = fs.readFileSync('preview/index.html', 'utf8');
const java = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/MainActivity.java', 'utf8');
const clock = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/HomeClockView.java', 'utf8');
const boltJava = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/BatteryBoltView.java', 'utf8');
const css = html.match(/\.battery-led\s*\{([^}]*)\}/)[1];
assert.match(css, /left:\s*calc\(50%\s*\+\s*1px\)/);
assert.match(css, /bottom:\s*calc\(1\.2%\s*-\s*1px\)/);
assert.match(css, /color:\s*#e9f0f5/);
assert.match(css, /opacity:\s*1/);
assert.match(html, /id="batteryLed" role="img" aria-label="Baterai 85 persen"><span class="battery-value">85<\/span><svg class="battery-bolt"/);
assert.match(java, /int percent = level \* 100 \/ scale/);
assert.match(java, /batteryLed\.setText\(String\.valueOf\(percent\)\)/);
assert.match(java, /batteryLed\.setContentDescription\("Baterai " \+ percent \+ " persen"/);
assert.match(java, /batteryLed\.setText\("85"\)/);
const offset = java.match(/private void updateBatteryOffset\([\s\S]*?\n    }/)[0];
assert.match(offset, /pixelsPerMm = root\.getWidth\(\) \/ 70\.9f/);
assert.match(offset, /cssPixel = pixelsPerMm \* 25\.4f \/ 96f/);
assert.match(offset, /batteryBox\.setTranslationX\(cssPixel\)/);
assert.match(offset, /batteryBox\.setTranslationY\(cssPixel - homeIndicatorShift\(root\)\)/);
assert.match(offset, /float left = root\.getWidth\(\) \/ 2f \+ cssPixel - boltWidth \/ 2f/);
assert.match(offset, /float top = batteryBox\.getBottom\(\) - 2\.75f \* cssPixel - 2f \* pixelsPerMm/);
assert.match(java, /updateBatteryOffset\(homeRoot, batteryLed\)/);
const boltCss = html.match(/\.battery-bolt\s*\{([^}]*)\}/)[1];
assert.match(boltCss, /display:\s*none/);
assert.match(boltCss, /top:\s*calc\(100% - 3\.75px - 2mm\)/);
assert.match(boltCss, /width:\s*2\.8125mm/); assert.match(boltCss, /height:\s*3\.9375mm/);
assert.match(html, /\.battery-led\.charging \.battery-bolt\s*\{\s*display:\s*block/);
assert.match(boltJava, /WIDTH_MM = 2\.8125f/); assert.match(boltJava, /HEIGHT_MM = 3\.9375f/);
assert.match(boltJava, /ROWS = \{6, 12, 12, 30, 6, 12, 8\}/);
// Dimensions are 50% larger than the previous experiment. The center rises
// just enough to keep the enlarged icon visible inside the screen.
assert.equal(2.8125, 1.875 * 1.5); assert.equal(3.9375, 2.625 * 1.5);
assert((-2 + 3.9375 / 2) < (-0.6125 + 2.625 / 2));
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
  const room=height*.012-cssPixel; // battery bottom margin
  assert(Math.abs((cssPixel-.5*cssPixel)-.5*cssPixel)<1e-9, 'battery moved right .5 CSS px');
  assert(Math.abs((.25*cssPixel-(-.75*cssPixel))-cssPixel)<1e-9, 'bolt moved up one CSS px');
  assert(room + 3.75*cssPixel + 2*mm > 3.9375*mm, 'enlarged bolt fits within the screen on the target aspect ratio');
}
const charge = java.match(/private void updateChargeAnim\(\) \{[\s\S]*?\n    }/)[0];
assert.match(charge, /ValueAnimator\.ofFloat\(\.9f, 1f\)/);
assert.match(charge, /setDuration\(900\)/);
assert.match(charge, /setRepeatMode\(ValueAnimator\.REVERSE\)/);
assert.match(charge, /batteryLed\.setAlpha\(alpha\)/);
assert.match(charge, /batteryBolt\.setAlpha\(alpha\)/);
assert.match(charge, /batteryLed\.setTextColor\(light \? 0xFF993800 : 0xFFFFB65C\)/);
assert.match(charge, /batteryLed\.setShadowLayer\(8f, 0f, 0f, light \? 0x44893000 : 0x8CFF6919\)/);
assert.match(charge, /batteryBolt\.setColor\(light \? 0xFF993800 : 0xFFFFB65C\)/);
assert.match(charge, /batteryLed\.setTextColor\(light \? 0xFF263840 : 0xFFE9F0F5\)/);
assert.match(charge, /batteryBolt\.setColor\(light \? 0xFF263840 : 0xFFE9F0F5\)/);
assert.match(boltJava, /void setColor\(int color\)\s*\{\s*dot\.setColor\(color\)/);
assert.match(charge, /batteryBolt\.setVisibility\(View\.VISIBLE\)/);
assert.match(charge, /batteryBolt\.setVisibility\(View\.INVISIBLE\)/);
assert(!/clockLed|dateLed|setTime/.test(charge), 'charging affects the battery indicator only');
assert(!/separatorAlpha/.test(clock), 'clock separator remains steady');
const chargingCss = html.match(/\.battery-led\.charging\s*\{([^}]*)\}/)[1];
assert.match(chargingCss, /color:\s*#ffb65c/);
assert.match(chargingCss, /rgba\(255, 105, 25, 0\.55\)/);
assert.match(chargingCss, /animation:\s*batteryPulse 0\.9s ease-in-out infinite alternate/);
assert.match(boltCss, /fill:\s*currentColor/);
assert.match(html, /@keyframes batteryPulse\s*\{\s*from\s*\{\s*opacity:\s*0\.9;\s*\}\s*to\s*\{\s*opacity:\s*1;/);
assert.match(html, /battery\.classList\.toggle\('charging', toggle\.checked\)/);
assert.match(html, /prefers-reduced-motion: reduce\)\s*\{\s*\.battery-led\.charging\s*\{\s*animation:\s*none/);
console.log('Battery position, live value, Doto-style bolt and battery-only charging animation: OK');
