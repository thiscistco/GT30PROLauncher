/* Preview-only path editor. Coordinates are in the original wallpaper's pixels. */
(function () {
  'use strict';
  var KEY = 'gt30-circuit-user-paths-v1';
  var MAX_PATHS = 25, MAX_POINTS = 600;
  var protectedBoxes = [[30,1060,1195,2300], [35,925,330,1060],
    [900,875,1120,1080], [900,40,1080,200]];

  window.setupCircuitEditor = function (cfg, apply) {
    var canvas = document.getElementById('pathDraw');
    var ctx = canvas.getContext('2d');
    var edit = document.getElementById('pathEdit');
    var undo = document.getElementById('pathUndo');
    var reset = document.getElementById('pathReset');
    var exportButton = document.getElementById('pathExport');
    var status = document.getElementById('pathStatus');
    if (!ctx) { status.textContent = 'Editor tidak didukung browser ini.'; return; }
    var paths = [], drawing = null, editing = false, storageAvailable = true;

    function valid(list) {
      return Array.isArray(list) && list.length <= MAX_PATHS && list.every(function (path) {
        return Array.isArray(path) && path.length >= 2 && path.length <= MAX_POINTS && path.every(function (p, i) {
          return Array.isArray(p) && p.length === 2 && Number.isInteger(p[0]) && Number.isInteger(p[1]) &&
            p[0] >= 0 && p[0] < cfg.width && p[1] >= 0 && p[1] < cfg.height &&
            (!i || p[0] !== path[i-1][0] || p[1] !== path[i-1][1]);
        });
      });
    }
    try {
      var saved = localStorage.getItem(KEY);
      if (saved) { var parsed = JSON.parse(saved); if (valid(parsed)) paths = parsed; }
    } catch (error) { storageAvailable = false; }

    function pathSpecs() {
      var result = [];
      paths.forEach(function (points) {
        result.push({ offset: 0, kind: 'copper', points: points });
        result.push({ offset: 0.5, kind: 'copper', points: points });
      });
      return result;
    }
    function line(context, points) {
      context.beginPath(); context.moveTo(points[0][0], points[0][1]);
      for (var i = 1; i < points.length; i++) context.lineTo(points[i][0], points[i][1]);
      context.stroke();
    }
    function makeMask() {
      var mask = document.createElement('canvas');
      mask.width = cfg.width; mask.height = cfg.height;
      var m = mask.getContext('2d');
      m.lineJoin = 'round'; m.lineCap = 'round';
      // A warm outer glow and brighter amber core, clipped away from protected areas.
      m.strokeStyle = 'rgba(255,105,25,0.65)'; m.lineWidth = 20;
      m.shadowColor = 'rgba(255,105,25,0.9)'; m.shadowBlur = 18;
      paths.forEach(function (path) { line(m, path); });
      m.shadowBlur = 0; m.strokeStyle = 'rgba(255,205,106,0.92)'; m.lineWidth = 10;
      paths.forEach(function (path) { line(m, path); });
      protectedBoxes.forEach(function (b) { m.clearRect(b[0], b[1], b[2]-b[0]+1, b[3]-b[1]+1); });
      return mask;
    }
    function persist() {
      try { localStorage.setItem(KEY, JSON.stringify(paths)); storageAvailable = true; }
      catch (error) { storageAvailable = false; }
    }
    function geometry() {
      var box = canvas.getBoundingClientRect();
      var scale = Math.max(box.width / cfg.width, box.height / cfg.height);
      return { box: box, scale: scale, x: (box.width - cfg.width * scale) / 2,
        y: (box.height - cfg.height * scale) / 2 };
    }
    function drawGuides() {
      if (!editing) return;
      var g = geometry(), dpr = Math.min(window.devicePixelRatio || 1, 2);
      if (!g.box.width || !g.box.height) return;
      canvas.width = Math.round(g.box.width * dpr); canvas.height = Math.round(g.box.height * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.translate(g.x, g.y); ctx.scale(g.scale, g.scale);
      ctx.lineJoin = 'round'; ctx.lineCap = 'round'; ctx.lineWidth = 9;
      ctx.strokeStyle = '#fff1cd'; ctx.shadowColor = '#ff8c18'; ctx.shadowBlur = 15;
      paths.forEach(function (path) { line(ctx, path); });
      if (drawing && drawing.points.length > 1) line(ctx, drawing.points);
    }
    function refresh() {
      apply(paths.length ? pathSpecs() : null, paths.length ? makeMask() : null);
      undo.disabled = !paths.length;
      reset.disabled = !paths.length;
      exportButton.disabled = !paths.length;
      status.textContent = (paths.length ? paths.length + ' jalur buatan aktif.' : 'Jalur bawaan aktif.') +
        (editing ? ' Gambar pada layar; lepaskan untuk menyimpan.' : '') +
        (storageAvailable ? '' : ' Penyimpanan browser tidak tersedia; unduh sebelum menutup halaman.');
      drawGuides();
    }
    function setEditing(value) {
      editing = value; drawing = null; canvas.hidden = !value;
      edit.textContent = value ? 'Selesai menggambar' : 'Gambar jalur';
      edit.setAttribute('aria-pressed', String(value));
      refresh();
    }
    function toPoint(event) {
      var g = geometry();
      return [Math.max(0, Math.min(cfg.width - 1, Math.round((event.clientX - g.box.left - g.x) / g.scale))),
              Math.max(0, Math.min(cfg.height - 1, Math.round((event.clientY - g.box.top - g.y) / g.scale)))];
    }
    function addPoint(event, force) {
      var point = toPoint(event), last = drawing.points[drawing.points.length - 1];
      var distance = Math.hypot(point[0]-last[0], point[1]-last[1]);
      if (distance > 0 && (force || distance >= 5) && drawing.points.length < MAX_POINTS) drawing.points.push(point);
    }
    edit.disabled = false;
    edit.setAttribute('aria-pressed', 'false');
    edit.addEventListener('click', function () {
      if (!editing) document.getElementById('tabHome').click();
      setEditing(!editing);
    });
    document.getElementById('tabSuper').addEventListener('click', function () { if (editing) setEditing(false); });
    undo.addEventListener('click', function () { if (drawing) return; paths.pop(); persist(); refresh(); });
    reset.addEventListener('click', function () {
      if (drawing || !window.confirm('Hapus jalur buatan dan pulihkan jalur bawaan?')) return;
      paths = []; persist(); refresh();
    });
    exportButton.addEventListener('click', function () {
      if (!paths.length) return;
      var data = { width: cfg.width, height: cfg.height, durationMs: cfg.durationMs, radius: cfg.radius, paths: pathSpecs() };
      var url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2) + '\n'], { type: 'application/json' }));
      var link = document.createElement('a'); link.href = url; link.download = 'jalur-animasi-gt30.json';
      document.body.appendChild(link); link.click(); link.remove();
      setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
      status.textContent = 'Berkas diunduh. Kirim jalur-animasi-gt30.json ke chat untuk dimasukkan ke APK.';
    });
    canvas.addEventListener('pointerdown', function (event) {
      if (!editing || event.isPrimary === false || event.button !== 0 || drawing) return;
      if (paths.length >= MAX_PATHS) { status.textContent = 'Maksimal ' + MAX_PATHS + ' jalur. Urungkan jalur untuk menggambar lagi.'; return; }
      event.preventDefault();
      drawing = { id: event.pointerId, points: [toPoint(event)] };
      if (canvas.setPointerCapture) canvas.setPointerCapture(event.pointerId);
    });
    canvas.addEventListener('pointermove', function (event) {
      if (!drawing || drawing.id !== event.pointerId) return;
      event.preventDefault(); addPoint(event, false); drawGuides();
    });
    canvas.addEventListener('pointerup', function (event) {
      if (!drawing || drawing.id !== event.pointerId) return;
      event.preventDefault(); addPoint(event, true);
      if (drawing.points.length > 1) { paths.push(drawing.points); persist(); }
      drawing = null; refresh();
    });
    canvas.addEventListener('pointercancel', function (event) {
      if (drawing && drawing.id === event.pointerId) { drawing = null; refresh(); }
    });
    window.addEventListener('resize', drawGuides);
    refresh();
  };
})();
