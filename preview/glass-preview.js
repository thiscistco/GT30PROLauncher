/* Compare home layering; Android uses the default (glass over indicators). */
(function () {
  'use strict';
  var toggle=document.getElementById('glassPreviewToggle');
  var screen=document.querySelector('.screen');
  if (!toggle || !screen) return;
  function update() { screen.classList.toggle('glass-over-home',toggle.checked); }
  toggle.addEventListener('change',update);
  update();

  // Match background-size: cover of the 1224 × 2720 wallpaper. This is
  // a 1470-pixel displacement (1220 + 250) in source-image coordinates, not CSS pixels.
  function updateShift() {
    var scale=Math.max(screen.clientWidth / 1224, screen.clientHeight / 2720);
    screen.style.setProperty('--home-shift', (1470 * scale) + 'px');
  }
  updateShift();
  if (typeof ResizeObserver !== 'undefined') new ResizeObserver(updateShift).observe(screen);
  else window.addEventListener('resize',updateShift);
})();
