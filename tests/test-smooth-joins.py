#!/usr/bin/env python3
"""Verify rounded joins without joining independent animation routes."""
import importlib.util
from pathlib import Path

spec = importlib.util.spec_from_file_location('smooth_circuit_paths', 'tools/smooth_circuit_paths.py')
smoother = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoother)

for points in (
    [[0, 0], [20, 0], [20, 20]],
    [[0, 0], [20, 0], [20, 20], [40, 20]],
    [[0, 0], [5, 0], [0, 0]],
    [[0, 0], [1, 0], [1, 1]],
):
    rounded = smoother.round_joins(points)
    assert rounded[0] == points[0] and rounded[-1] == points[-1]
    assert all(a != b for a, b in zip(rounded, rounded[1:]))
assert len(smoother.round_joins([[0, 0], [20, 0], [20, 20]])) > 3
assert smoother.round_joins([[0, 0], [20, 0]]) == [[0, 0], [20, 0]]
print('Rounded joins preserve endpoints and avoid duplicate segments: OK')
