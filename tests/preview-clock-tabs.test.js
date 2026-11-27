// Run with: node tests/preview-clock-tabs.test.js
const fs = require('fs'), vm = require('vm'), assert = require('assert');
const html = fs.readFileSync('preview/index.html', 'utf8');
const java = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/ClockActivity.java', 'utf8');
const names = ['alarm','world','timer','stopwatch'], labels = ['Alarm','Jam Dunia','Timer','Stopwatch'];
const matches = [...html.matchAll(/<button([^>]*data-clock-tab="([^"]+)"[^>]*)>([\s\S]*?)<\/button>/g)];
assert.equal(matches.length, 4);
for (let i=0; i<4; i++) {
  const [, attrs, name, content] = matches[i];
  assert.equal(name, names[i]);
  assert(attrs.includes(`aria-label="${labels[i]}"`));
  assert(attrs.includes(`title="${labels[i]}"`));
  assert(attrs.includes('aria-pressed="false"'));
  assert(content.includes(`src="assets/clock-${name}.svg?v=3"`));
  assert.match(content, /alt="" aria-hidden="true"/);
  assert.equal(content.replace(/<[^>]+>/g, '').trim(), '', 'tabs display icons only');
}
assert(html.indexOf('class="clock-stage"') < html.indexOf('<nav class="clock-tabs"'));
assert.match(html, /#clockPage:not\(\[hidden\]\) \{ display:flex; flex-direction:column; overflow:hidden/);
assert.match(html, /#clockPage \.clock-tabs \{ flex:none/);
assert.match(java, /R\.drawable\.ic_clock_alarm, R\.drawable\.ic_clock_world/);
assert.match(java, /R\.drawable\.ic_clock_timer, R\.drawable\.ic_clock_stopwatch/);
assert.match(java, /item\.setContentDescription\(TAB_NAMES\[i\]\)/);
assert.match(java, /item\.setTooltipText\(TAB_NAMES\[i\]\)/);
assert.match(java, /item\.setSelected\(i == selected\)/);
assert.match(java, /item\.setOnClickListener\(v -> showTab\(tab == index \? -1 : index\)\)/);
assert.match(java, /new LinearLayout\.LayoutParams\(dp\(48\), dp\(48\)\)/);
assert.match(java, /tabs\.setGravity\(Gravity\.RIGHT \| Gravity\.CENTER_VERTICAL\)/);
assert.match(java, /if \(i > 0\) itemParams\.leftMargin = dp\(3\)/);
assert.match(java, /tabsScroll\.setFillViewport\(true\)/);
assert.match(html, /gap:\.48mm/);
assert.match(html, /#clockPage \.clock-tabs button:first-child \{ margin-left:auto/);
assert.match(java, /item\.setPadding\(dp\(12\), dp\(12\), dp\(12\), dp\(12\)\)/);
assert(java.indexOf('page.addView(stage') < java.indexOf('page.addView(tabsScroll'));
const buttons = names.map(name => ({
  name, attrs:{'aria-pressed':'false'}, active:false,
  getAttribute(key){return key==='data-clock-tab' ? this.name : this.attrs[key]},
  setAttribute(key,value){this.attrs[key]=value},
  classList:{toggle(){}}, addEventListener(type,fn){this.click=fn;}
}));
for (const b of buttons) b.classList.toggle = (name, active) => {if(name==='active') b.active=active;};
const panels = names.map(name => ({name, hidden:true, getAttribute(){return this.name}}));
const hud={hidden:false}, scroll={hidden:true,scrollTop:12}, window={};
const document = {querySelectorAll: selector => selector==='[data-clock-tab]' ? buttons : panels};
const byId = id => id==='clockHud' ? hud : scroll;
const init = html.match(/var activeClockTab = null;[\s\S]*?(?=    function updateHud\(\))/);
assert(init);vm.runInNewContext(init[0], {document,byId,window,updateHud(){}});
assert.equal(hud.hidden,false);assert.equal(scroll.hidden,true);
for (let i=0; i<4; i++) {
  buttons[i].click();assert(hud.hidden);assert(!scroll.hidden);
  buttons.forEach((b,j)=>{assert.equal(b.active, i===j);assert.equal(b.attrs['aria-pressed'], i===j?'true':'false');});
  panels.forEach((p,j)=>assert.equal(p.hidden, i!==j));
}
assert(window.clockReturnToHud());assert(!hud.hidden && scroll.hidden);assert(!window.clockReturnToHud());
buttons[0].click();buttons[0].click();assert(!hud.hidden && scroll.hidden);
console.log('Bottom clock navigation: default HUD, four accessible icons, toggle and return: OK');
