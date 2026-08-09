# Contract: acceptance-tracer-report

The transient per-bullet report `acceptance-tracer` returns during the loop (distinct from the final appendix verdicts).

Source: the `validation-gates` skill (acceptance-tracer).

## Frozen mechanics
- Transient states: `verified | partial | missing | conflicting | build-gated | resource-gated | spec-gated | screenshot-gated | test-gated | manual`.
- `missing`/`partial`/`conflicting` block or route (the loop is not done); the gated states defer to their validator; `manual` routes nothing.
- `test-gated` (a `test:` anchor bullet before evidence): a fresh PASS receipt with a deterministic anchor→test-identity mapping in the sealed summary turns it `verified`; a test that exists but does not prove the bullet is `partial`; an assertion contradicting the bullet is `conflicting`; a failed or stale required lane keeps blocking (never `deferred`). This is the ONE gated class that can finish as `verified` inside the loop — build/spec/screenshot-gated keep their defer-to-validator behavior unchanged.
- Owner-builder routing goes through the planner `Owner builder` mapping (no 2-hop lookup).
