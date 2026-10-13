# Contract: acceptance-tracer-report

The transient per-bullet report `acceptance-tracer` returns during the loop (distinct from the final appendix verdicts).

Source: the `validation-gates` skill (acceptance-tracer).

## Frozen mechanics
- Transient states: `verified | partial | missing | conflicting | build-gated | resource-gated | spec-gated | manual`.
- `missing`/`partial`/`conflicting` block or route (the loop is not done); the gated states defer to their validator; `manual` routes nothing.
- Owner-builder routing goes through the planner `Owner builder` mapping (no 2-hop lookup).
