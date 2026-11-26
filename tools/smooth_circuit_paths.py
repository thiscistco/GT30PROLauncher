#!/usr/bin/env python3
"""Smooth Jalur-2 gently and add a third, counter-moving light per route.

Several averaging passes remove small kinks; corners and endpoints remain fixed.
Vertices move at most two source pixels per axis, then near-collinear points are
simplified within one pixel. Coordinates stay integer for the editor and masks.
Interior corners are rounded locally with quadratic curves while keeping each
independent route and both endpoints intact. The two original light offsets
remain unchanged; offset 1 starts at the opposite end.
Usage: python tools/smooth_circuit_paths.py INPUT.json OUTPUT.json
"""
import json
import math
import sys
from pathlib import Path


def simplify(points, tolerance=1.0):
    # Preserve sharp corners and reversals while removing near-collinear noise.
    anchors = [0]
    for i in range(1, len(points) - 1):
        incoming = [points[i][k] - points[i-1][k] for k in (0, 1)]
        outgoing = [points[i+1][k] - points[i][k] for k in (0, 1)]
        if sum(a*b for a, b in zip(incoming, outgoing)) <= 0:
            anchors.append(i)
    anchors.append(len(points) - 1)
    keep = set(anchors)
    stack = list(zip(anchors, anchors[1:]))
    while stack:
        start, end = stack.pop()
        a, b = points[start], points[end]
        dx, dy = b[0] - a[0], b[1] - a[1]
        length2 = dx*dx + dy*dy
        farthest, distance = None, tolerance
        for i in range(start + 1, end):
            p = points[i]
            t = max(0, min(1, ((p[0]-a[0])*dx + (p[1]-a[1])*dy) / length2)) if length2 else 0
            d = math.hypot(p[0]-a[0]-t*dx, p[1]-a[1]-t*dy)
            if d > distance:
                farthest, distance = i, d
        if farthest is not None:
            keep.add(farthest)
            stack.extend([(start, farthest), (farthest, end)])
    return [points[i] for i in sorted(keep)]


def round_joins(points, radius=2.5):
    # Cut only a short part of each adjoining segment, never connect distinct
    # routes or move their endpoints. Shared endpoints remain exactly shared.
    result = [points[0][:]]
    def add(point):
        point = [math.floor(v + 0.5) for v in point]
        if point != result[-1]:
            result.append(point)
    for previous, corner, following in zip(points, points[1:], points[2:]):
        before = math.dist(previous, corner)
        after = math.dist(corner, following)
        if not before or not after:
            add(corner)
            continue
        cut = min(radius, before / 3, after / 3)
        start = [corner[k] + (previous[k] - corner[k]) * cut / before for k in (0, 1)]
        end = [corner[k] + (following[k] - corner[k]) * cut / after for k in (0, 1)]
        add(start)
        for t in (0.25, 0.5, 0.75):
            add([(1-t)**2 * start[k] + 2*(1-t)*t*corner[k] + t*t*end[k] for k in (0, 1)])
        add(end)
    add(points[-1])
    return result


def smooth(points):
    fixed = {0, len(points) - 1}
    for i in range(1, len(points) - 1):
        incoming = [points[i][k] - points[i-1][k] for k in (0, 1)]
        outgoing = [points[i+1][k] - points[i][k] for k in (0, 1)]
        if sum(a*b for a, b in zip(incoming, outgoing)) <= 0:
            fixed.add(i)
    current = [list(map(float, p)) for p in points]
    for _ in range(4):
        updated = [p[:] for p in current]
        for i in range(1, len(points) - 1):
            if i in fixed:
                continue
            for k in (0, 1):
                value = (current[i-1][k] + 2 * current[i][k] + current[i+1][k]) / 4
                updated[i][k] = max(points[i][k] - 2, min(points[i][k] + 2, value))
        current = updated
    result = [points[0][:]]
    for point in current[1:-1]:
        candidate = [math.floor(v + 0.5) for v in point]
        if candidate != result[-1]:
            result.append(candidate)
    if result[-1] != points[-1]:
        result.append(points[-1][:])
    return round_joins(simplify(result))


def main():
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    data = json.loads(Path(sys.argv[1]).read_text(encoding='utf-8'))
    paths = data['paths']
    if len(paths) % 2:
        raise SystemExit('Expected pairs of user paths with offsets 0 and 0.5.')
    result = []
    for first, second in zip(paths[::2], paths[1::2]):
        if (first['offset'], second['offset']) != (0, 0.5) or any(
                first.get(key) != second.get(key) for key in ('kind', 'width', 'points')):
            raise SystemExit('Each route must contain matching offsets 0 and 0.5.')
        points = smooth(first['points'])
        for offset in (0, 0.5, 1):
            result.append({**first, 'offset': offset, 'points': points})
    data['paths'] = result
    Path(sys.argv[2]).write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
