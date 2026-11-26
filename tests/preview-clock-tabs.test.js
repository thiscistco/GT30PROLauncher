// Run with: node tests/preview-clock-tabs.test.js
const fs = require('fs'), vm = require('vm'), assert = require('assert');
const html = fs.readFileSync('preview/index.html', 'utf8');
const java = fs.readFileSync('app/src/main/java/com/gt30pro/launcher/ClockActivity.java', 'utf8');
const names = ['alarm','world','timer','stopwatch'], labels = ['Alarm','Dunia','Timer','Stopwatch'];
const matches = [...html.matchAll(/<button([^>]*data-clock-tab="([^"]+)"[^>]*)>([\s\S]*?)<\/button>/g)];
assert.equal(matches.length, 4);
for (let i=0; i<4; i++) {
  const [, attrs, name, content] = matches[i];
  assert.equal(name, names[i]);
  assert(attrs.includes(`aria-label="${labels[i]}"`));
  assert(attrs.includes(`title="${labels[i]}"`));
  assert(attrs.includes(`aria-pressed="${i === 0 ? 'true' : 'false'}"`));
  assert(content.includes(`src="assets/clock-${name}.svg?v=1"`));
  assert.match(content, /alt="" aria-hidden="true"/);
  assert.equal(content.replace(/<[^>]+>/g, '').trim(), '', 'tabs display icons, not text labels');
}
assert.match(java, /R\.drawable\.ic_clock_alarm, R\.drawable\.ic_clock_world/);
assert.match(java, /R\.drawable\.ic_clock_timer, R\.drawable\.ic_clock_stopwatch/);
assert.match(java, /new ImageButton\(this\)/);
assert.match(java, /item\.setContentDescription\(TAB_NAMES\[i\]\)/);
assert.match(java, /item\.setTooltipText\(TAB_NAMES\[i\]\)/);
assert.match(java, /item\.setSelected\(i == selected\)/);
assert.match(java, /item\.setOnClickListener\(v -> showTab\(index\)\)/);
assert.match(java, /new LinearLayout\.LayoutParams\(dp\(52\), dp\(52\)\)/);
assert.match(java, /item\.setPadding\(dp\(13\), dp\(13\), dp\(13\), dp\(13\)\)/);
assert.match(html, /\.clock-tabs button \{[^}]*width:\s*9mm;\s*height:\s*9mm/);
assert.match(html, /\.clock-tabs button img \{[^}]*width:\s*5\.2mm;\s*height:\s*5\.2mm/);
const buttons = names.map((name, i) => ({
  name, attrs:{'aria-pressed':i===0?'true':'false'}, active:i===0,
  getAttribute(key){return key==='data-clock-tab' ? this.name : this.attrs[key]},
  setAttribute(key,value){this.attrs[key]=value},
  classList:{toggle(){}}, addEventListener(type,fn){this.click=fn;}
}));
for (const b of buttons) b.classList.toggle = (name, active) => {if(name==='active') b.active=active;};
const panels = names.map(name => ({name, hidden:name!=='alarm', getAttribute(){return this.name}}));
const document = {querySelectorAll: selector => selector==='[data-clock-tab]' ? buttons : panels};
const init = html.match(/document\.querySelectorAll\('\[data-clock-tab\]'\)\.forEach\(function \(button\) \{[\s\S]*?\n    \}\);/);
assert(init, 'tab event registration exists');
vm.runInNewContext(init[0], {document});
for (let i=0; i<4; i++) {
  buttons[i].click();
  buttons.forEach((b,j)=>{assert.equal(b.active, i===j);assert.equal(b.attrs['aria-pressed'], i===j?'true':'false');});
  panels.forEach((p,j)=>assert.equal(p.hidden, i!==j));
}
console.log('Four accessible icon tabs select the correct clock panels: OK');
