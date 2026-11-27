/* Pointer/slider-only simulation; no motion sensors or permission requests. */
(function () {
  'use strict';
  var screen=document.querySelector('.screen'), layer=document.getElementById('glassParallax');
  var toggle=document.getElementById('glassParallaxToggle');
  var xSlider=document.getElementById('glassTiltX'), ySlider=document.getElementById('glassTiltY');
  if(!screen || !layer || !toggle || !xSlider || !ySlider) return;
  var reduced=window.matchMedia ? window.matchMedia('(prefers-reduced-motion: reduce)') : {matches:false};
  function clamp(value) { return Math.max(-1,Math.min(1,value)); }
  function setTilt(x,y) {
    if(!toggle.checked || reduced.matches) return;
    var box=screen.getBoundingClientRect();
    layer.style.setProperty('--glass-x',(clamp(x)*box.width*.035)+'px');
    layer.style.setProperty('--glass-y',(clamp(y)*box.height*.035)+'px');
  }
  function sliders() { setTilt(Number(xSlider.value)/100,Number(ySlider.value)/100); }
  function update() {
    layer.hidden=!toggle.checked || reduced.matches;
    xSlider.disabled=ySlider.disabled=layer.hidden;
    if(!layer.hidden) sliders();
  }
  toggle.addEventListener('change',update);
  xSlider.addEventListener('input',sliders); ySlider.addEventListener('input',sliders);
  screen.addEventListener('pointermove',function (e) {
    if(e.pointerType!=='mouse' && e.pointerType!=='pen') return;
    var box=screen.getBoundingClientRect(); if(!box.width || !box.height) return;
    var x=clamp((e.clientX-box.left)/box.width*2-1), y=clamp((e.clientY-box.top)/box.height*2-1);
    xSlider.value=Math.round(x*100); ySlider.value=Math.round(y*100); setTilt(x,y);
  });
  screen.addEventListener('pointerleave',function (e) {
    if(e.pointerType!=='mouse' && e.pointerType!=='pen') return;
    xSlider.value=ySlider.value=0; sliders();
  });
  window.addEventListener('resize',sliders);
  if(reduced.addEventListener) reduced.addEventListener('change',update);
  else if(reduced.addListener) reduced.addListener(update);
  update();
})();
