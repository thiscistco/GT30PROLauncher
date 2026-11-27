/* Match background-size: cover of the 1224 × 2720 wallpaper. */
(function () {
  'use strict';
  var screen=document.querySelector('.screen');
  if (!screen) return;
  // Displacements are in source-image pixels, not CSS pixels.
  function updateShift() {
    var scale=Math.max(screen.clientWidth / 1224, screen.clientHeight / 2720);
    screen.style.setProperty('--clock-shift', (2182 * scale) + 'px');
    screen.style.setProperty('--clock-right', (750 * scale) + 'px');
    screen.style.setProperty('--battery-shift', (1470 * scale) + 'px');
    screen.style.setProperty('--battery-right', (495 * scale) + 'px');
  }
  updateShift();
  if (typeof ResizeObserver !== 'undefined') new ResizeObserver(updateShift).observe(screen);
  else window.addEventListener('resize',updateShift);
})();
