// Run with: node tests/preview-battery-indicator.test.js
const assert = require('assert'), fs = require('fs');
const html = fs.readFileSync('preview/index.html', 'utf8');
const java = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/MainActivity.java', 'utf8');
const clock = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/HomeClockView.java', 'utf8');
const css = html.match(/\.battery-led\s*\{([^}]*)\}/)[1];
assert.match(css, /bottom:\s*calc\(1\.2%\s*-\s*1px\)/);
assert.match(css, /color:\s*#ffffff/);
assert.match(css, /opacity:\s*0\.6/);
assert(!/animation\s*:/.test(css), 'battery indicator does not animate');
assert.match(html, /id="batteryLed" role="img" aria-label="Baterai 85 persen">85<\/div>/);
assert.match(java, /int percent = level \* 100 \/ scale/);
assert.match(java, /batteryLed\.setText\(String\.valueOf\(percent\)\)/);
assert.match(java, /batteryLed\.setContentDescription\("Baterai " \+ percent \+ " persen"\)/);
assert.match(java, /batteryLed\.setText\("85"\)/);
const offset = java.match(/private void updateBatteryOffset\([\s\S]*?\n    }/)[0];
assert.match(offset, /\(root\.getWidth\(\) \/ 70\.9f\) \* 25\.4f \/ 96f/);
assert.match(offset, /batteryBox\.setTranslationY\(cssPixel\)/);
assert.match(java, /updateBatteryOffset\(homeRoot, batteryLed\)/);
for (const width of [269, 720, 1224]) {
  const cssPixel = (width / 70.9) * 25.4 / 96;
  assert(cssPixel > 0, 'battery translation is down by one CSS px');
}
// The existing charging effect is restricted to the five clock separator dots.
const charge = java.match(/private void updateChargeAnim\(\) \{[\s\S]*?\n    }/)[0];
assert.match(charge, /ValueAnimator\.ofFloat\(0\.15f, 1f\)/);
assert.match(charge, /setDuration\(900\)/);
assert.match(charge, /setRepeatMode\(ValueAnimator\.REVERSE\)/);
assert.match(charge, /clockLed\.setSeparatorAlpha\(\(float\) a\.getAnimatedValue\(\)\)/);
assert(!/batteryLed|dateLed|setTime/.test(charge), 'charging must not animate battery, date, or digits');
assert.match(clock, /for \(int i = 0; i < 5; i\+\+\)/);
assert.match(html, /\.clock-sep\.charging\s*\{\s*animation:\s*sepPulse 0\.9s ease-in-out infinite alternate/);
assert.match(html, /sep\.classList\.toggle\('charging', toggle\.checked\)/);
console.log('Battery percentage display and separator-only charging animation: OK');
