// Run with: node tests/preview-super-grid.test.js
const assert = require('assert'), fs = require('fs');
const html = fs.readFileSync('preview/index.html', 'utf8');
const xml = fs.readFileSync('app/src/main/res/layout/activity_super_app.xml', 'utf8');
const java = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/SuperAppActivity.java', 'utf8');
const page = html.match(/<section class="super-page"[\s\S]*?<\/section>/)[0];
const grid = html.match(/\.super-grid\s*\{([^}]*)\}/)[1];
assert.match(grid, /display:\s*grid/);
assert.match(grid, /grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/);
assert(!page.includes('Aplikasi di dalam launcher'));
assert(!page.includes('super-card-text'));
assert.match(page, /id="openClockModule" type="button" aria-label="Jam"><img src="assets\/module-clock\.svg\?v=1" alt="" aria-hidden="true"><\/button>/);
assert.match(page, /id="openCalendarModule" type="button" aria-label="Kalender"><img src="assets\/module-calendar\.svg\?v=1" alt="" aria-hidden="true"><\/button>/);
assert.equal((page.match(/class="super-icon-button"/g) || []).length, 5, 'clock, calendar, timetable, settings and notes remain');
assert.match(page, /id="superWeather"/);
assert.match(page, /id="openSettingsModule"/);
assert.match(page, /id="openNotesModule" type="button" aria-label="Catatan"/);
assert.match(java,/addModule\(container, R\.drawable\.ic_module_notes, "Catatan", NotesActivity\.class\)/);
assert.match(java, /addModule\(container, R\.drawable\.ic_module_settings, "Setelan", LauncherSettingsActivity\.class\)/);
assert.match(page, /id="openTimetableModule" type="button" aria-label="Jadwal Mengajar"/);
assert.match(page, /id="openTimetableModule" type="button" aria-label="Jadwal Mengajar"><img src="assets\/module-timetable\.svg\?v=1" alt="" aria-hidden="true"><\/button>/);
assert.match(java, /addModule\(container, R\.drawable\.ic_module_timetable, "Jadwal Mengajar", TimeTableActivity\.class\)/);
assert(!java.includes('addTimetableModule'));

assert.match(xml, /<GridLayout[\s\S]*?android:id="@\+id\/modules_container"[\s\S]*?android:columnCount="4"/);
assert(!/super_subtitle|modules_subtitle/.test(xml));
assert.match(java, /addModule\(container, R\.drawable\.ic_module_clock, "Jam", ClockActivity\.class\)/);
assert.match(java, /addModule\(container, R\.drawable\.ic_module_calendar, "Kalender", CalendarActivity\.class\)/);
assert.match(java, /button\.setContentDescription\(name\)/);
assert.match(java, /button\.setOnClickListener\(v -> startActivity\(new Intent\(this, activity\)\)\)/);
assert.match(java, /params\.height = cellWidth/);
assert.match(java, /startDragAndDrop/);
assert.match(java, /putString\("order"/);
assert.match(html, /#superGrid > \* \{ aspect-ratio:1; height:auto; \}/);
assert.match(fs.readFileSync('preview/super-reorder.js','utf8'),/localStorage\.setItem/);
assert.match(java, /int cellWidth = Math\.max\(1, width \/ 4\)/);
assert.match(java, /params\.width = cellWidth/);
assert.match(java, /sizeGridCells\(container\)/);
for (const name of ['clock','calendar','timetable','settings','notes']) {
 const svg = fs.readFileSync(`preview/assets/module-${name}.svg`, 'utf8');
 const vector = fs.readFileSync(`app/src/main/res/drawable/ic_module_${name}.xml`, 'utf8');
 assert.match(svg, /fill="#ffffff"/); assert.match(vector, /android:fillColor="#FFFFFF"/);
 const coords = [...svg.matchAll(/<circle cx="([\d.]+)" cy="([\d.]+)" r="\.36"/g)].map(([,x,y])=>[+x,+y]);
 const paths = [...vector.matchAll(/M([\d.]+),([\d.]+) a\.36,\.36/g)].map(([,x,y])=>[+x,+y]);
 assert(coords.length > 30 && coords.length === paths.length);
 coords.forEach(([x,y],i)=>{ assert(Math.abs(x-paths[i][0]-.36)<1e-6); assert.equal(y,paths[i][1]); });
}
console.log('Super App: accessible white dot icons, matching Android/preview four-column grid: OK');

// Module launchers remain plain, while controls inside built-in apps retain glass styling.
const glass=html.match(/:is\(#calendarPage,#clockPage,#weatherPage,#timetablePage,#drawerPage,#superPage,#notesPage\) :is\(([^\n]*)\) \{/)[1];
assert(!glass.includes('super-icon-button') && !glass.includes('super-weather'));
assert(glass.includes('.calendar-icon-button:not(#calendarBack)'));
assert(!glass.includes('app-back') && !glass.includes('app-theme-button') && glass.includes('clock-action'));
assert.match(html,/\.app-voice-fab \{[^}]*right:calc\(4mm \+ 10px\); bottom:calc\(4mm \+ 10px\)/);
