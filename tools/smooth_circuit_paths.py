#!/usr/bin/env python3
"""Gently smooth Jalur-2 without modifying the original or its endpoints.

Each interior point is averaged with its immediate neighbours, with displacement
limited to one source pixel per axis. Sharp turns/reversals are preserved. The
integer output stays compatible with the path editor and asset generators.
Usage: python tools/smooth_circuit_paths.py INPUT.json OUTPUT.json
"""
import json
import math
import sys
from pathlib import Path


def smooth(points):
    result = [points[0][:]]
    for previous, point, following in zip(points, points[1:], points[2:]):
        incoming = [point[i] - previous[i] for i in (0, 1)]
        outgoing = [following[i] - point[i] for i in (0, 1)]
        # Keep deliberately sharp corners and turnarounds in place.
        if sum(a*b for a, b in zip(incoming, outgoing)) <= 0:
            candidate = point[:]
        else:
            candidate = []
            for axis in (0, 1):
                delta = (previous[axis] + following[axis] - 2 * point[axis]) / 4
                delta = max(-1, min(1, delta))
                candidate.append(math.floor(point[axis] + delta + 0.5))
        if candidate != result[-1]:
            result.append(candidate)
    # Retain the exact endpoint, even if smoothing produced the same point.
    if result[-1] != points[-1]:
        result.append(points[-1][:])
    return result


def main():
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    data = json.loads(Path(sys.argv[1]).read_text(encoding='utf-8'))
    for path in data['paths']:
        path['points'] = smooth(path['points'])
    Path(sys.argv[2]).write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
