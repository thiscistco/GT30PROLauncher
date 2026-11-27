/* Compare home layering; Android uses the default (glass over indicators). */
(function () {
  'use strict';
  var toggle=document.getElementById('glassPreviewToggle');
  var screen=document.querySelector('.screen');
  if (!toggle || !screen) return;
  function update() { screen.classList.toggle('glass-over-home',toggle.checked); }
  toggle.addEventListener('change',update);
  update();

  // Match background-size: cover of the 1224 × 2720 wallpaper.
  // Displacements are in source-image pixels, not CSS pixels.
  function updateShift() {
    var scale=Math.max(screen.clientWidth / 1224, screen.clientHeight / 2720);
    screen.style.setProperty('--clock-shift', (2120 * scale) + 'px');
    screen.style.setProperty('--clock-right', (550 * scale) + 'px');
    screen.style.setProperty('--battery-shift', (1470 * scale) + 'px');
    screen.style.setProperty('--battery-right', (485 * scale) + 'px');
  }
  updateShift();
  if (typeof ResizeObserver !== 'undefined') new ResizeObserver(updateShift).observe(screen);
  else window.addEventListener('resize',updateShift);
})();
