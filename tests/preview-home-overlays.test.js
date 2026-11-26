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
// Both platforms move the weather group another 60 CSS px left and 40 CSS px up.
const weatherCss=html.match(/\.home-weather\s*\{([^}]*)\}/)[1];
const weatherLayout=main.match(/private void updateWeatherLayout\([\s\S]*?\n    }/)[0];
assert.match(weatherCss,/left:\s*calc\(49%\s*\+\s*15px\)/);
assert.match(weatherCss,/bottom:\s*calc\(6%\s*\+\s*460px\)/);
assert.match(weatherLayout,/width \* \.49f \+ 15f \* cssPixel - w \/ 2f/);
assert.match(weatherLayout,/height - \(height \* \.06f \+ 460f \* cssPixel\) - h/);
for (const [w,h] of [[269,596],[720,1600],[1080,2400],[1224,2720]]) {
 const mm=w/70.9, css=mm*25.4/96, dateCenter=w*.49+115*css, center=w*.49+15*css;
 const dateScale=.855*(2.64+10*25.4/96)/2;
 const dateHeight=(2*dateScale*(1+2*.65)+2*25.4/96+.8)*mm;
 const dateTop=h-(h*.06+384.55*css)-dateHeight+.4*mm;
 const weatherTop=h-(h*.06+460*css)-13.75*mm;
 const previousCenter=w*.49+75*css;
 assert(Math.abs((previousCenter-center)-60*css)<1e-9);
 const previousWeatherTop=h-(h*.06+420*css)-13.75*mm;
 assert(Math.abs((previousWeatherTop-weatherTop)-40*css)<1e-9);
 assert(center-11*mm/2 >= 0 && center+11*mm/2 <= w, 'weather remains on-screen');
 assert(dateTop >= 0 && dateTop+dateHeight < h && weatherTop >= 0 && weatherTop+13.75*mm < h, 'both overlays remain visible');
 const dateVisibleWidth=(1.2*2*dateScale+.2*2*dateScale)*mm;
 assert(center+11*mm/2 < dateCenter-dateVisibleWidth/2, 'weather does not cover visible date glyphs');
}
console.log('Home overlays: post-layout recovery, exact weather shift, date separation and manual activation: OK');
