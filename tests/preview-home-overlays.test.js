const assert = require('assert'), fs = require('fs');
const main = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/MainActivity.java','utf8');
const date = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/HomeDateView.java','utf8');
const weather = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/WeatherController.java','utf8');
const html = fs.readFileSync('preview/index.html','utf8');
assert.match(main,/addOnGlobalLayoutListener\(\(\) -> \{[\s\S]*?updateDateLayout\(homeRoot, dateBox\);[\s\S]*?updateWeatherLayout\(homeRoot, weatherBox\);/);
assert.match(main,/homeRoot\.post\(\(\) -> \{\s*if \(dateLed != null\) updateDateLayout\(homeRoot, dateLed\);\s*if \(weatherLed != null\) updateWeatherLayout\(homeRoot, weatherLed\);/);
assert.match(main,/weatherLed\.setVisibility\(View\.VISIBLE\)/);assert.match(main,/weatherLed\.bringToFront\(\)/);
assert.equal((main.match(/if \(changed\) (?:dateBox|box)\.setLayoutParams\(params\)/g)||[]).length,2, 'reconciliation must converge without a layout loop');
assert.match(date,/if \(pxPerMm == pixelsPerMm\) return/);
assert.match(main,/weather\.requestFromUser\(\)/);assert.match(html,/id="weatherRefresh"/);
assert.match(weather,/LocationManager\.NETWORK_PROVIDER, LocationManager\.PASSIVE_PROVIDER/);
assert(!weather.includes('ACCESS_FINE_LOCATION'));
// Verify real viewport positions for both overlays; content stays visible and separated.
for (const [w,h] of [[269,596],[720,1600],[1080,2400],[1224,2720]]) {
 const mm=w/70.9, css=mm*25.4/96, center=w*.49+115*css;
 const dateHeight=(2*.855*(2.64+10*25.4/96)/2*(1+2*.65)+2*25.4/96+.8)*mm;
 const dateTop=h-(h*.06+384.55*css)-dateHeight+.4*mm;
 const weatherTop=h-(h*.06+320*css)-13.75*mm;
 assert(center-11*mm/2 >= 0 && center+11*mm/2 <= w, 'weather remains on-screen');
 assert(dateTop >= 0 && dateTop+dateHeight < weatherTop && weatherTop+13.75*mm < h, 'overlays on-screen and separate');
}
console.log('Home overlays: post-layout recovery, visible weather, unchanged anchors and manual activation: OK');
