const assert=require('assert'),fs=require('fs'),vm=require('vm');
const html=fs.readFileSync('preview/index.html','utf8');
const js=fs.readFileSync('preview/voice-preview.js','utf8');
assert.match(html,/<button class="home-voice" id="homeVoice"[^>]*type="button"/);
assert.match(html,/\.home-voice \{[^}]*z-index:6;[^}]*bottom:calc\(1\.2% \+ 12\.75px \+ 3mm\);[^}]*left:calc\(50% - 39px\);[^}]*transform:translateX\(-50%\)/);
assert.match(html,/\.screen\.glass-over-home \.home-screen-glass \{ z-index:7; \}/);
assert.match(html,/\.home-screen-glass \{[^}]*pointer-events:none/);
assert.match(html,/\.home-voice\[hidden\] \{ display:none; \}/);
assert.match(html,/\.home-voice \{[^}]*color:#ffcd6a/);
assert.equal((html.match(/class="home-voice-bar"/g)||[]).length,5);
assert.match(html,/prefers-reduced-motion: reduce\) \{ \.home-voice\.voice-demo \.home-voice-bar \{ animation:none;/);
assert.match(html,/assets\/voice-command\.svg\?v=3/);
assert.match(html,/<script src="voice-preview\.js\?v=2"><\/script>/);
assert(!/SpeechRecognition|getUserMedia|webkitSpeechRecognition/.test(js));
let handler,observers=[];
let classes=new Set(),timer;
let button={hidden:false,classList:{add(v){classes.add(v);},remove(v){classes.delete(v);}},addEventListener(type,fn){assert.equal(type,'click');handler=fn;}};
let status={textContent:''};
let ids=['drawerPage','superPage','clockPage','calendarPage','weatherPage','timetablePage','pathDraw'];
let pages=Object.fromEntries(ids.map(id=>[id,{hidden:true}]));
let elements={homeVoice:button,voicePreviewStatus:status,...pages};
class Observer { constructor(fn){this.fn=fn;} observe(el,options){assert.deepEqual(Array.from(options.attributeFilter),['hidden']); observers.push([el,this.fn]);} }
vm.runInNewContext(js,{document:{getElementById(id){return elements[id];}},MutationObserver:Observer,setTimeout(fn,delay){assert.equal(delay,2400);timer=fn;return 1;},clearTimeout(){}});
assert.equal(button.hidden,false); handler(); assert.match(status.textContent,/Simulasi animasi saja/); assert.match(status.textContent,/preview tidak mengakses mikrofon/);
assert(classes.has('voice-demo')); timer(); assert(!classes.has('voice-demo'));
assert.match(status.textContent,/ketuk ikon untuk perintah suara/);
for (const page of Object.values(pages)) {
  page.hidden=false; observers.find(([el])=>el===page)[1](); assert.equal(button.hidden,true);
  page.hidden=true; observers.find(([el])=>el===page)[1](); assert.equal(button.hidden,false);
}
console.log('Voice preview: icon behind glass, page focus, no microphone and explanatory click: OK');
