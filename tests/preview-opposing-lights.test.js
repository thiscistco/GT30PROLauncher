// Run with: node tests/preview-opposing-lights.test.js
const fs = require('fs'), vm = require('vm'), assert = require('assert');
const html = fs.readFileSync('preview/index.html', 'utf8');
const script = Array.from(html.matchAll(/<script>([\s\S]*?)<\/script>/g)).at(-1)[1];
const cfg = JSON.parse(fs.readFileSync('preview/assets/circuit-paths.json', 'utf8'));
// Horizontal route makes position and direction directly measurable.
cfg.paths = [0, .5, 1].map(offset => ({offset, points: [[100,200], [300,200]]}));
const frames = [], lights = [];
const ctx = {clearRect(){}, save(){}, restore(){}, translate(){}, scale(){}, beginPath(){}, arc(){}, fill(){}, drawImage(){},
  createRadialGradient(x, y){lights.push([x,y]); return {addColorStop(){}};}};
const canvas = {getContext:()=>ctx, getBoundingClientRect:()=>({width:269,height:597})};
const els = {amberGlow:canvas, amberToggle:{checked:true,addEventListener(){}}, amberStatus:{textContent:''},
  ramSimulation:{value:'50',addEventListener(){}}, ramValue:{textContent:''}, superPage:{hidden:true}, clockPage:{hidden:true}};
const document = {hidden:false, getElementById:id=>els[id]};
const window = {requestAnimationFrame:fn=>frames.push(fn), matchMedia:()=>({matches:false}), devicePixelRatio:1};
class ImageMock {set src(value){queueMicrotask(()=>this.onload());}}
vm.runInNewContext(script,{document,window,Image:ImageMock,Math,Promise,fetch:async()=>({ok:true,json:async()=>cfg})});
setImmediate(()=>{
  function at(t){const start=lights.length;frames.shift()(t);return lights.slice(start);}
  function close(value, expected){assert(Math.abs(value-expected)<1e-7, `${value} != ${expected}`);}
  for (const [t,first,opposite] of [[0,100,300],[1200,140,260],[3000,200,200],[6000,300,100],[7200,260,140],[12000,100,300]]) {
    const values=at(t); assert.equal(values.length,3); close(values[0][0],first); close(values[2][0],opposite);
  }
  console.log('Preview third light starts opposite and reverses with the first: OK');
});
