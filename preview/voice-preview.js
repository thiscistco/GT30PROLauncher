/* Visual animation demo only; this page never activates the microphone. */
(function () {
  'use strict';
  var button=document.getElementById('homeVoice');
  var fab=document.getElementById('appVoiceFab');
  var status=document.getElementById('voicePreviewStatus');
  if (!button || !status) return;
  var demoTimer;
  function stopDemo() { clearTimeout(demoTimer); button.classList.remove('voice-demo'); }
  if (fab) fab.addEventListener('click', function () { var message='Simulasi saja: preview tidak mengakses mikrofon atau menjalankan perintah suara.'; status.textContent=message; window.alert(message); });
  button.addEventListener('click',function () {
    status.textContent='Simulasi animasi saja: preview tidak mengakses mikrofon. Di APK, ketuk ikon untuk perintah suara lalu ucapkan “yo”. Tekan lama untuk pilihan suara.';
    stopDemo(); button.classList.add('voice-demo'); demoTimer=setTimeout(stopDemo,2400);
  });
  var pages=['drawerPage','superPage','clockPage','calendarPage','weatherPage','timetablePage','pathDraw']
    .map(function (id) { return document.getElementById(id); }).filter(Boolean);
  function update() { button.hidden=pages.some(function (page) { return !page.hidden; }); if(button.hidden) stopDemo(); }
  update();
  if (typeof MutationObserver !== 'undefined') {
    var observer=new MutationObserver(update);
    pages.forEach(function (page) { observer.observe(page,{attributes:true,attributeFilter:['hidden']}); });
  }
})();
