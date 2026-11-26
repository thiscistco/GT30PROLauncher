/* Preview-only path editor. Coordinates and widths use the wallpaper's pixels. */
(function () {
  'use strict';
  var KEY = 'gt30-circuit-user-paths-v1';
  var PALM_KEY = 'gt30-circuit-stylus-only-v1';
  var MAX_PATHS = 100, MAX_STROKE_POINTS = 600, MAX_PATH_POINTS = 6000;
  var protectedBoxes = [[30,1060,1195,2300], [35,925,330,1060],
    [900,875,1120,1080], [900,40,1080,200]];

  window.setupCircuitEditor = function (cfg, apply) {
    var canvas = document.getElementById('pathDraw'), ctx = canvas.getContext('2d');
    var edit = document.getElementById('pathEdit'), erase = document.getElementById('pathErase');
    var undo = document.getElementById('pathUndo'), reset = document.getElementById('pathReset');
    var loadDefault = document.getElementById('pathLoadDefault');
    var exportButton = document.getElementById('pathExport'), status = document.getElementById('pathStatus');
    var widthInput = document.getElementById('pathWidth'), widthValue = document.getElementById('pathWidthValue');
    var palmInput = document.getElementById('pathPalm');
    var codeToggle = document.getElementById('pathCodeToggle'), codePanel = document.getElementById('pathCodePanel');
    var codeText = document.getElementById('pathCodeText'), codeCopy = document.getElementById('pathCodeCopy');
    var codeStatus = document.getElementById('pathCodeStatus');
    if (!ctx) { status.textContent = 'Editor tidak didukung browser ini.'; return; }
    // Each drawn path has three light offsets. Keep one editable polyline
    // for each unique path, in its original order.
    var defaultPaths = [], seen = Object.create(null), defaultWidth = 10;
    cfg.paths.forEach(function (path) {
      if (path.kind !== 'copper' || !Array.isArray(path.points)) return;
      var key = JSON.stringify(path.points);
      if (!seen[key]) { seen[key] = true; defaultPaths.push(path.points.map(function (p) { return p.slice(); })); }
      if (Number.isInteger(path.width) && path.width >= 4 && path.width <= 60) defaultWidth = path.width;
    });
    function copyPaths(list) { return list.map(function (path) { return path.map(function (p) { return p.slice(); }); }); }
    var paths = copyPaths(defaultPaths), width = defaultWidth, drawing = null, erasing = null, mode = 'view', history = [], storageAvailable = true;

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
    // Keep this preference separate from path storage for backwards compatibility.
    try { palmInput.checked = localStorage.getItem(PALM_KEY) === '1'; }
    catch (error) { storageAvailable = false; }

    function pathSpecs() {
      var result = [];
      paths.forEach(function (points) {
        result.push({ offset: 0, kind: 'copper', width: width, points: points });
        result.push({ offset: 0.5, kind: 'copper', width: width, points: points });
        // Offset 1 starts from the other end and opposes the offset 0 light.
        result.push({ offset: 1, kind: 'copper', width: width, points: points });
      });
      return result;
    }
    function matchesDefault() {
      // Compare data, not object key order, so unedited defaults use the exact
      // shared bitmap also shipped in Android instead of the live-drawn mask.
      function signature(items) { return JSON.stringify(items.map(function (p) { return [p.offset, p.kind, p.width, p.points]; })); }
      return signature(pathSpecs()) === signature(cfg.paths);
    }
    function pathCode() {
      return JSON.stringify({ width: cfg.width, height: cfg.height, durationMs: cfg.durationMs,
        radius: cfg.radius, paths: pathSpecs() }, null, 2) + '\n';
    }
    function syncCode() {
      if (!codePanel.hidden) {
        var value = pathCode();
        if (codeText.value !== value) { codeText.value = value; codeStatus.textContent = ''; }
      }
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
    function snapshot(restoreWidth) {
      history.push({ paths: copyPaths(paths), width: restoreWidth ? width : null });
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
      if (drawing && drawing.points.length > 1) line(ctx, smoothStroke(drawing.points));
      // Predictions are a temporary visual tail, never saved or exported.
      if (drawing && drawing.predicted.length) {
        ctx.globalAlpha = 0.45;
        line(ctx, [drawing.points[drawing.points.length - 1]].concat(drawing.predicted));
        ctx.globalAlpha = 1;
      }
      ctx.shadowBlur = 0; ctx.fillStyle = '#ffb63e';
      paths.forEach(function (path) {
        [path[0], path[path.length - 1]].forEach(function (p) {
          ctx.beginPath(); ctx.arc(p[0], p[1], Math.max(width / 2, 4 / g.scale), 0, Math.PI * 2); ctx.fill();
        });
      });
    }
    function refresh() {
      var isDefault = matchesDefault();
      apply(isDefault ? null : pathSpecs(), !isDefault && paths.length ? makeMask() : null);
      undo.disabled = !history.length; reset.disabled = !paths.length;
      loadDefault.disabled = !defaultPaths.length || isDefault;
      exportButton.disabled = !paths.length; erase.disabled = !paths.length && mode !== 'erase';
      widthInput.value = String(width); widthValue.textContent = width + ' px';
      syncCode();
      status.textContent = (paths.length ? paths.length + (isDefault ? ' jalur awal aktif.' : ' jalur buatan aktif.') : 'Belum ada jalur. Silakan gambar dari awal.') +
        (mode === 'draw' ? ' Tarik dari atau ke titik ujung untuk menyambung.' :
          mode === 'erase' ? ' Usap bagian garis yang ingin dihapus.' : '') +
        (palmInput.checked ? ' Penolakan telapak aktif: khusus stylus.' : '') +
        (storageAvailable ? '' : ' Penyimpanan browser tidak tersedia; unduh sebelum menutup halaman.');
      drawGuides();
    }
    function setMode(value) {
      mode = value; drawing = null; erasing = null; canvas.hidden = value === 'view';
      edit.textContent = value === 'view' ? 'Gambar jalur' : 'Selesai mengedit';
      edit.setAttribute('aria-pressed', String(value !== 'view'));
      erase.setAttribute('aria-pressed', String(value === 'erase'));
      canvas.style.cursor = 'crosshair';
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
    function smoothStroke(points) {
      if (points.length < 3) return points.slice();
      var result = [points[0]];
      function add(p) {
        p = [Math.round(p[0]), Math.round(p[1])];
        if (distance(p, result[result.length - 1]) > 0) result.push(p);
      }
      // One conservative corner-cutting pass rounds jitter without overshooting,
      // and preserves both endpoints for joining and precise finishing.
      for (var i = 1; i < points.length - 1; i++) {
        var prev = points[i-1], p = points[i], next = points[i+1];
        add([p[0] * 0.8 + prev[0] * 0.2, p[1] * 0.8 + prev[1] * 0.2]);
        add([p[0] * 0.8 + next[0] * 0.2, p[1] * 0.8 + next[1] * 0.2]);
      }
      add(points[points.length - 1]);
      return result;
    }
    function roundJoin(left, right) {
      var prev = left[left.length - 2], corner = right[0], next = right[1];
      var before = distance(prev, corner), after = distance(corner, next);
      if (!before || !after) return left.slice(0, -1).concat(right);
      var cut = Math.min(2.5, before / 3, after / 3);
      var start = [0, 1].map(function (k) { return corner[k] + (prev[k] - corner[k]) * cut / before; });
      var end = [0, 1].map(function (k) { return corner[k] + (next[k] - corner[k]) * cut / after; });
      var result = left.slice(0, -1);
      function add(p) {
        p = [Math.round(p[0]), Math.round(p[1])];
        if (!result.length || distance(p, result[result.length - 1]) > 0) result.push(p);
      }
      add(start);
      [0.25, 0.5, 0.75].forEach(function (t) {
        add([0, 1].map(function (k) { return (1-t)*(1-t)*start[k] + 2*(1-t)*t*corner[k] + t*t*end[k]; }));
      });
      add(end);
      right.slice(1).forEach(add);
      return result;
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
        if (result.length + other.length + 4 > MAX_PATH_POINTS) return null;
        if (!best.ourEnd) result.reverse();
        if (best.theirEnd) other = other.slice().reverse();
        result[result.length - 1] = other[0]; // snap to the existing endpoint
        result = roundJoin(result, other);
        // Snapping can coincide with the penultimate point; never export zero-length segments.
        result = result.filter(function (p, i) { return !i || distance(p, result[i-1]) > 0; });
        remaining.splice(best.index, 1);
      }
      if (remaining.length >= MAX_PATHS || result.length < 2) return null;
      remaining.push(result);
      return remaining;
    }
    // Compute the interval of a line segment covered by a swept round eraser.
    // The capsule is the union of two circles and the strip between them.
    function erasedInterval(p, q, a, b, radius) {
      var vx = q[0]-p[0], vy = q[1]-p[1], vv = vx*vx + vy*vy, intervals = [];
      function circle(c) {
        var x = p[0]-c[0], y = p[1]-c[1];
        var qb = 2*(x*vx+y*vy), qc = x*x+y*y-radius*radius;
        var disc = qb*qb - 4*vv*qc;
        if (disc < 0 || !vv) return;
        var root = Math.sqrt(disc), lo = Math.max(0, (-qb-root)/(2*vv)), hi = Math.min(1, (-qb+root)/(2*vv));
        if (lo <= hi) intervals.push([lo, hi]);
      }
      function clip(range, start, delta, low, high) {
        if (Math.abs(delta) < 1e-9) return start >= low && start <= high ? range : null;
        var x = (low-start)/delta, y = (high-start)/delta;
        range = [Math.max(range[0], Math.min(x,y)), Math.min(range[1], Math.max(x,y))];
        return range[0] <= range[1] ? range : null;
      }
      circle(a); circle(b);
      var dx = b[0]-a[0], dy = b[1]-a[1], len2 = dx*dx+dy*dy;
      if (len2) {
        var x = p[0]-a[0], y = p[1]-a[1];
        var range = clip([0,1], x*dx+y*dy, vx*dx+vy*dy, 0, len2);
        if (range) range = clip(range, x*dy-y*dx, vx*dy-vy*dx, -radius*Math.sqrt(len2), radius*Math.sqrt(len2));
        if (range) intervals.push(range);
      }
      if (!intervals.length) return null;
      var lo = Math.min.apply(null, intervals.map(function (i) { return i[0]; }));
      var hi = Math.max.apply(null, intervals.map(function (i) { return i[1]; }));
      return hi - lo > 1e-8 ? [lo, hi] : null;
    }
    function eraseSweep(a, b) {
      var radius = width / 2 + 8 / geometry().scale, pieces = [], changed = false;
      function at(p, q, t) { return [Math.round(p[0]+(q[0]-p[0])*t), Math.round(p[1]+(q[1]-p[1])*t)]; }
      function append(part, p) { if (!part.length || distance(part[part.length-1], p) > 0) part.push(p); }
      function finish(part) { if (part.length >= 2) pieces.push(part); }
      paths.forEach(function (path) {
        var part = [path[0]];
        for (var i = 1; i < path.length; i++) {
          var p = path[i-1], q = path[i], cut = erasedInterval(p, q, a, b, radius);
          if (!cut) { append(part, p); append(part, q); continue; }
          changed = true;
          if (cut[0] > 1e-8) { append(part, p); append(part, at(p,q,cut[0])); }
          finish(part); part = [];
          if (cut[1] < 1-1e-8) { append(part, at(p,q,cut[1])); append(part, q); }
        }
        finish(part);
      });
      if (!changed) return;
      if (!valid(pieces)) { status.textContent = 'Terlalu banyak potongan garis. Urungkan atau hapus lebih banyak bagian sekaligus.'; return; }
      if (!erasing.changed) { snapshot(); erasing.changed = true; }
      paths = pieces; persist(); refresh();
    }
    edit.disabled = false; widthInput.disabled = false; palmInput.disabled = false; codeToggle.disabled = false;
    codeToggle.addEventListener('click', function () {
      codePanel.hidden = !codePanel.hidden;
      codeToggle.setAttribute('aria-expanded', String(!codePanel.hidden));
      codeToggle.textContent = codePanel.hidden ? 'Tampilkan kode jalur' : 'Sembunyikan kode jalur';
      if (!codePanel.hidden) syncCode();
    });
    codeCopy.addEventListener('click', function () {
      syncCode();
      function selectManually() {
        codeText.focus(); codeText.select();
        codeStatus.textContent = 'Penyalinan otomatis tidak tersedia. Salin teks yang dipilih secara manual.';
      }
      try {
        if (!window.navigator || !window.navigator.clipboard || !window.navigator.clipboard.writeText) { selectManually(); return; }
        window.navigator.clipboard.writeText(codeText.value).then(function () {
          codeStatus.textContent = 'Kode disalin. Tempelkan ke chat.';
        }, selectManually);
      } catch (error) { selectManually(); }
    });
    palmInput.addEventListener('change', function () {
      // If enabled mid-gesture, discard an unfinished non-pen stroke; already
      // applied eraser changes remain undoable as a single operation.
      if (palmInput.checked) {
        if (drawing && drawing.type !== 'pen') drawing = null;
        if (erasing && erasing.type !== 'pen') erasing = null;
      }
      try { localStorage.setItem(PALM_KEY, palmInput.checked ? '1' : '0'); }
      catch (error) { storageAvailable = false; }
      refresh();
    });
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
    undo.addEventListener('click', function () {
      if (drawing || erasing || !history.length) return;
      var previous = history.pop(); paths = previous.paths;
      if (previous.width !== null) width = previous.width;
      persist(); refresh();
    });
    loadDefault.addEventListener('click', function () {
      if (drawing || erasing || (paths.length && !window.confirm('Ganti jalur tersimpan dengan jalur awal?'))) return;
      snapshot(true); paths = copyPaths(defaultPaths); width = defaultWidth; persist(); refresh();
    });
    reset.addEventListener('click', function () {
      if (drawing || erasing || !window.confirm('Hapus semua jalur buatan di browser ini?')) return;
      snapshot(); paths = []; persist(); refresh();
    });
    widthInput.addEventListener('input', function () {
      var n = Number(widthInput.value);
      if (!Number.isInteger(n) || n < 4 || n > 60) return;
      width = n; persist(); refresh();
    });
    exportButton.addEventListener('click', function () {
      if (!paths.length) return;
      var url = URL.createObjectURL(new Blob([pathCode()], { type: 'application/json' }));
      var link = document.createElement('a'); link.href = url; link.download = 'jalur-animasi-gt30.json';
      document.body.appendChild(link); link.click(); link.remove();
      setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
      status.textContent = 'Berkas diunduh. Kirim jalur-animasi-gt30.json ke chat untuk dimasukkan ke APK.';
    });
    canvas.addEventListener('pointerdown', function (event) {
      if (mode === 'view') return;
      // Browsers identify an active stylus as pointerType "pen". When enabled,
      // reject other pointer types even when the palm touches before the pen.
      if (palmInput.checked && event.pointerType !== 'pen') { event.preventDefault(); return; }
      if (event.isPrimary === false || event.button !== 0 || drawing || erasing) return;
      event.preventDefault();
      if (canvas.setPointerCapture) canvas.setPointerCapture(event.pointerId);
      if (mode === 'erase') {
        erasing = { id: event.pointerId, type: event.pointerType, changed: false, last: toPoint(event) };
        eraseSweep(erasing.last, erasing.last);
      } else drawing = { id: event.pointerId, type: event.pointerType, points: [toPoint(event)], predicted: [] };
    });
    canvas.addEventListener('pointermove', function (event) {
      if (mode !== 'view' && palmInput.checked && event.pointerType !== 'pen') { event.preventDefault(); return; }
      if (erasing && erasing.id === event.pointerId) {
        event.preventDefault();
        var p = toPoint(event);
        if (distance(p, erasing.last) > 0) { eraseSweep(erasing.last, p); erasing.last = p; }
        return;
      }
      if (!drawing || drawing.id !== event.pointerId) return;
      event.preventDefault();
      var samples = event.getCoalescedEvents ? event.getCoalescedEvents() : [];
      samples.forEach(function (sample) { addPoint(sample, false); });
      addPoint(event, false);
      var predictions = event.getPredictedEvents ? event.getPredictedEvents() : [];
      drawing.predicted = Array.prototype.slice.call(predictions, 0, 8).map(toPoint);
      drawGuides();
    });
    canvas.addEventListener('pointerup', function (event) {
      if (mode !== 'view' && palmInput.checked && event.pointerType !== 'pen') { event.preventDefault(); return; }
      if (erasing && erasing.id === event.pointerId) {
        event.preventDefault();
        var p = toPoint(event);
        if (distance(p, erasing.last) > 0) eraseSweep(erasing.last, p);
        erasing = null; return;
      }
      if (!drawing || drawing.id !== event.pointerId) return;
      event.preventDefault(); addPoint(event, true);
      if (drawing.points.length > 1) {
        var combined = combineStroke(smoothStroke(drawing.points));
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
