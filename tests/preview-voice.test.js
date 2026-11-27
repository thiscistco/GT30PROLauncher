const assert=require('assert'),fs=require('fs'),vm=require('vm');
const html=fs.readFileSync('preview/index.html','utf8');
const js=fs.readFileSync('preview/voice-preview.js','utf8');
assert.match(html,/<button class="home-voice" id="homeVoice"[^>]*type="button"/);
assert.match(html,/\.home-voice \{[^}]*z-index:6;[^}]*bottom:calc\(1\.2% \+ 12\.75px \+ 3mm\);[^}]*left:calc\(50% - 34px\);[^}]*transform:translateX\(-50%\)/);
assert.match(html,/\.screen\.glass-over-home \.home-screen-glass \{ z-index:7; \}/);
assert.match(html,/\.home-screen-glass \{[^}]*pointer-events:none/);
assert.match(html,/\.home-voice\[hidden\] \{ display:none; \}/);
assert.match(html,/prefers-color-scheme: dark\) \{ \.home-voice \{ color:#fff;/);
assert.match(html,/assets\/voice-command\.svg\?v=2/);
assert.match(html,/<script src="voice-preview\.js\?v=1"><\/script>/);
assert(!/SpeechRecognition|getUserMedia|webkitSpeechRecognition/.test(js));
let handler,observers=[];
let button={hidden:false,addEventListener(type,fn){assert.equal(type,'click');handler=fn;}};
let status={textContent:''};
let ids=['drawerPage','superPage','clockPage','calendarPage','weatherPage','timetablePage','pathDraw'];
let pages=Object.fromEntries(ids.map(id=>[id,{hidden:true}]));
let elements={homeVoice:button,voicePreviewStatus:status,...pages};
class Observer { constructor(fn){this.fn=fn;} observe(el,options){assert.deepEqual(Array.from(options.attributeFilter),['hidden']); observers.push([el,this.fn]);} }
vm.runInNewContext(js,{document:{getElementById(id){return elements[id];}},MutationObserver:Observer});
assert.equal(button.hidden,false); handler(); assert.match(status.textContent,/Preview tidak mengakses mikrofon/);
assert.match(status.textContent,/ketuk ikon untuk perintah suara/);
for (const page of Object.values(pages)) {
  page.hidden=false; observers.find(([el])=>el===page)[1](); assert.equal(button.hidden,true);
  page.hidden=true; observers.find(([el])=>el===page)[1](); assert.equal(button.hidden,false);
}
console.log('Voice preview: icon behind glass, page focus, no microphone and explanatory click: OK');
