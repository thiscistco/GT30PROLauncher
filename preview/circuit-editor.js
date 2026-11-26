/* Preview-only path editor. Coordinates and widths use the wallpaper's pixels. */
(function () {
  'use strict';
  var KEY = 'gt30-circuit-user-paths-v1';
  var MAX_PATHS = 25, MAX_STROKE_POINTS = 600, MAX_PATH_POINTS = 6000;
  var protectedBoxes = [[30,1060,1195,2300], [35,925,330,1060],
    [900,875,1120,1080], [900,40,1080,200]];

  window.setupCircuitEditor = function (cfg, apply) {
    var canvas = document.getElementById('pathDraw'), ctx = canvas.getContext('2d');
    var edit = document.getElementById('pathEdit'), erase = document.getElementById('pathErase');
    var undo = document.getElementById('pathUndo'), reset = document.getElementById('pathReset');
    var exportButton = document.getElementById('pathExport'), status = document.getElementById('pathStatus');
    var widthInput = document.getElementById('pathWidth'), widthValue = document.getElementById('pathWidthValue');
    if (!ctx) { status.textContent = 'Editor tidak didukung browser ini.'; return; }
    var paths = [], width = 10, drawing = null, erasing = null, mode = 'view', history = [], storageAvailable = true;

    function valid(list) {
      return Array.isArray(list) && list.length <= MAX_PATHS && list.every(function (path) {
        return Array.isArray(path) && path.length >= 2 && path.length <= MAX_PATH_POINTS && path.every(function (p, i) {
          return Array.isArray(p) && p.length === 2 && Number.isInteger(p[0]) && Number.isInteger(p[1]) &&
            p[0] >= 0 && p[0] < cfg.width && p[1] >= 0 && p[1] < cfg.height &&
            (!i || p[0] !== path[i-1][0] || p[1] !== path[i-1][1]);
        });
      });
    }
    try {
      var saved = localStorage.getItem(KEY);
      if (saved) {
        var parsed = JSON.parse(saved), savedPaths = Array.isArray(parsed) ? parsed : parsed.paths;
        if (valid(savedPaths)) paths = savedPaths;
        if (!Array.isArray(parsed) && Number.isInteger(parsed.width) && parsed.width >= 4 && parsed.width <= 60) width = parsed.width;
      }
    } catch (error) { storageAvailable = false; }

    function pathSpecs() {
      var result = [];
      paths.forEach(function (points) {
        result.push({ offset: 0, kind: 'copper', width: width, points: points });
        result.push({ offset: 0.5, kind: 'copper', width: width, points: points });
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
      m.strokeStyle = 'rgba(255,105,25,0.65)'; m.lineWidth = width * 2;
      m.shadowColor = 'rgba(255,105,25,0.9)'; m.shadowBlur = 18;
      paths.forEach(function (path) { line(m, path); });
      m.shadowBlur = 0; m.strokeStyle = 'rgba(255,205,106,0.92)'; m.lineWidth = width;
      paths.forEach(function (path) { line(m, path); });
      protectedBoxes.forEach(function (b) { m.clearRect(b[0], b[1], b[2]-b[0]+1, b[3]-b[1]+1); });
      return mask;
    }
    function persist() {
      try { localStorage.setItem(KEY, JSON.stringify({ paths: paths, width: width })); storageAvailable = true; }
      catch (error) { storageAvailable = false; }
    }
    function snapshot() {
      history.push(paths.map(function (path) { return path.map(function (point) { return point.slice(); }); }));
      if (history.length > 30) history.shift();
    }
    function geometry() {
      var box = canvas.getBoundingClientRect();
      var scale = Math.max(box.width / cfg.width, box.height / cfg.height);
      return { box: box, scale: scale, x: (box.width - cfg.width * scale) / 2,
        y: (box.height - cfg.height * scale) / 2 };
    }
    function drawGuides() {
      if (mode === 'view') return;
      var g = geometry(), dpr = Math.min(window.devicePixelRatio || 1, 2);
      if (!g.box.width || !g.box.height) return;
      canvas.width = Math.round(g.box.width * dpr); canvas.height = Math.round(g.box.height * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.translate(g.x, g.y); ctx.scale(g.scale, g.scale);
      ctx.lineJoin = 'round'; ctx.lineCap = 'round'; ctx.lineWidth = width;
      ctx.strokeStyle = '#fff1cd'; ctx.shadowColor = '#ff8c18'; ctx.shadowBlur = 15;
      paths.forEach(function (path) { line(ctx, path); });
      if (drawing && drawing.points.length > 1) line(ctx, drawing.points);
      ctx.shadowBlur = 0; ctx.fillStyle = '#ffb63e';
      paths.forEach(function (path) {
        [path[0], path[path.length - 1]].forEach(function (p) {
          ctx.beginPath(); ctx.arc(p[0], p[1], Math.max(width / 2, 4 / g.scale), 0, Math.PI * 2); ctx.fill();
        });
      });
    }
    function refresh() {
      apply(paths.length ? pathSpecs() : null, paths.length ? makeMask() : null);
      undo.disabled = !history.length; reset.disabled = !paths.length;
      exportButton.disabled = !paths.length; erase.disabled = !paths.length && mode !== 'erase';
      widthInput.value = String(width); widthValue.textContent = width + ' px';
      status.textContent = (paths.length ? paths.length + ' jalur buatan aktif.' : 'Jalur bawaan aktif.') +
        (mode === 'draw' ? ' Tarik dari atau ke titik ujung untuk menyambung.' :
          mode === 'erase' ? ' Sentuh jalur untuk menghapus seluruh jalur.' : '') +
        (storageAvailable ? '' : ' Penyimpanan browser tidak tersedia; unduh sebelum menutup halaman.');
      drawGuides();
    }
    function setMode(value) {
      mode = value; drawing = null; erasing = null; canvas.hidden = value === 'view';
      edit.textContent = value === 'view' ? 'Gambar jalur' : 'Selesai mengedit';
      edit.setAttribute('aria-pressed', String(value !== 'view'));
      erase.setAttribute('aria-pressed', String(value === 'erase'));
      canvas.style.cursor = value === 'erase' ? 'not-allowed' : 'crosshair';
      refresh();
    }
    function toPoint(event) {
      var g = geometry();
      return [Math.max(0, Math.min(cfg.width - 1, Math.round((event.clientX - g.box.left - g.x) / g.scale))),
              Math.max(0, Math.min(cfg.height - 1, Math.round((event.clientY - g.box.top - g.y) / g.scale)))];
    }
    function distance(a, b) { return Math.hypot(a[0]-b[0], a[1]-b[1]); }
    function addPoint(event, force) {
      var point = toPoint(event), last = drawing.points[drawing.points.length - 1], d = distance(point, last);
      if (d > 0 && (force || d >= 5) && drawing.points.length < MAX_STROKE_POINTS) drawing.points.push(point);
    }
    function combineStroke(stroke) {
      var remaining = paths.slice(), result = stroke.slice(), threshold = 12 / geometry().scale;
      // Join the closest available endpoints. Reversing either polyline allows all
      // four endpoint orientations and a new stroke can bridge two old paths.
      while (true) {
        var best = null;
        remaining.forEach(function (path, index) {
          [0, 1].forEach(function (ourEnd) {
            [0, 1].forEach(function (theirEnd) {
              var a = result[ourEnd ? result.length - 1 : 0], b = path[theirEnd ? path.length - 1 : 0];
              var d = distance(a, b);
              if (d <= threshold && (!best || d < best.distance)) best = { index: index, ourEnd: ourEnd, theirEnd: theirEnd, distance: d };
            });
          });
        });
        if (!best) break;
        var other = remaining[best.index];
        if (result.length + other.length - 1 > MAX_PATH_POINTS) return null;
        if (!best.ourEnd) result.reverse();
        if (best.theirEnd) other = other.slice().reverse();
        result[result.length - 1] = other[0]; // snap to the existing endpoint
        result = result.concat(other.slice(1));
        // Snapping can coincide with the penultimate point; never export zero-length segments.
        result = result.filter(function (p, i) { return !i || distance(p, result[i-1]) > 0; });
        remaining.splice(best.index, 1);
      }
      if (remaining.length >= MAX_PATHS || result.length < 2) return null;
      remaining.push(result);
      return remaining;
    }
    function segmentDistance(p, a, b) {
      var dx = b[0] - a[0], dy = b[1] - a[1], len2 = dx*dx + dy*dy;
      var t = len2 ? Math.max(0, Math.min(1, ((p[0]-a[0])*dx + (p[1]-a[1])*dy) / len2)) : 0;
      return Math.hypot(p[0] - a[0] - t*dx, p[1] - a[1] - t*dy);
    }
    function eraseAt(event) {
      var p = toPoint(event), threshold = width / 2 + 12 / geometry().scale, best = -1, bestDistance = threshold;
      paths.forEach(function (path, index) {
        for (var i = 1; i < path.length; i++) {
          var d = segmentDistance(p, path[i-1], path[i]);
          if (d <= bestDistance) { bestDistance = d; best = index; }
        }
      });
      if (best < 0) return;
      if (!erasing.changed) { snapshot(); erasing.changed = true; }
      paths.splice(best, 1); persist(); refresh();
    }
    edit.disabled = false; widthInput.disabled = false;
    edit.setAttribute('aria-pressed', 'false');
    edit.addEventListener('click', function () {
      if (mode === 'view') { document.getElementById('tabHome').click(); setMode('draw'); }
      else setMode('view');
    });
    erase.addEventListener('click', function () {
      if (mode === 'view') document.getElementById('tabHome').click();
      setMode(mode === 'erase' ? 'draw' : 'erase');
    });
    document.getElementById('tabSuper').addEventListener('click', function () { if (mode !== 'view') setMode('view'); });
    undo.addEventListener('click', function () { if (drawing || erasing || !history.length) return; paths = history.pop(); persist(); refresh(); });
    reset.addEventListener('click', function () {
      if (drawing || erasing || !window.confirm('Hapus jalur buatan dan pulihkan jalur bawaan?')) return;
      snapshot(); paths = []; persist(); refresh();
    });
    widthInput.addEventListener('input', function () {
      var n = Number(widthInput.value);
      if (!Number.isInteger(n) || n < 4 || n > 60) return;
      width = n; persist(); refresh();
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
      if (mode === 'view' || event.isPrimary === false || event.button !== 0 || drawing || erasing) return;
      event.preventDefault();
      if (canvas.setPointerCapture) canvas.setPointerCapture(event.pointerId);
      if (mode === 'erase') { erasing = { id: event.pointerId, changed: false, last: toPoint(event) }; eraseAt(event); }
      else drawing = { id: event.pointerId, points: [toPoint(event)] };
    });
    canvas.addEventListener('pointermove', function (event) {
      if (erasing && erasing.id === event.pointerId) {
        event.preventDefault();
        var p = toPoint(event);
        if (distance(p, erasing.last) >= 4 / geometry().scale) { erasing.last = p; eraseAt(event); }
        return;
      }
      if (!drawing || drawing.id !== event.pointerId) return;
      event.preventDefault(); addPoint(event, false); drawGuides();
    });
    canvas.addEventListener('pointerup', function (event) {
      if (erasing && erasing.id === event.pointerId) { event.preventDefault(); erasing = null; return; }
      if (!drawing || drawing.id !== event.pointerId) return;
      event.preventDefault(); addPoint(event, true);
      if (drawing.points.length > 1) {
        var combined = combineStroke(drawing.points);
        if (combined) { snapshot(); paths = combined; persist(); }
        else { drawing = null; refresh(); status.textContent = 'Jalur terlalu panjang atau jumlah jalur penuh. Urungkan atau hapus sebagian.'; return; }
      }
      drawing = null; refresh();
    });
    canvas.addEventListener('pointercancel', function (event) {
      if (drawing && drawing.id === event.pointerId) drawing = null;
      if (erasing && erasing.id === event.pointerId) erasing = null;
      refresh();
    });
    window.addEventListener('resize', drawGuides);
    refresh();
  };
})();
