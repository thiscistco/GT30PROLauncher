/* Preview-only comparison: the production Android layering is unchanged. */
(function () {
  'use strict';
  var toggle=document.getElementById('glassPreviewToggle');
  var screen=document.querySelector('.screen');
  if (!toggle || !screen) return;
  function update() { screen.classList.toggle('glass-over-home',toggle.checked); }
  toggle.addEventListener('change',update);
  update();
})();
