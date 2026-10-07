# Contract: acceptance-trace

The final `## Outcome` -> `### Acceptance trace` shape. Frozen so the site board parser and `acceptance-tracer` agree.

Source: the `task-orchestrator` skill (`references/outcome-appendix.md`) + `orchestrator/contracts/outcome-shape.json`.

## Frozen mechanics
- The `## Outcome` appendix has six case-sensitive required `### ` headings, pinned by `contracts/outcome-shape.json`: Build gates, Runtime verify, Acceptance trace, Caveats, Follow-ups, Files touched (+ optional `### Execution log`).
- `### Acceptance trace` verdicts are exactly three: `verified | manual | deferred`. The transient tracer states (`partial`/`missing`/`conflicting`) MUST NOT appear here.
- Deliberate deviation from the older all-gated convention: `build-gated`/`spec-gated`/`screenshot-gated` bullets still land here as `deferred` (their validator owns the final word), but a `test-gated` bullet with a fresh PASS receipt lands as `verified` — the tracer holds a deterministic anchor→receipt binding, not just the fact that a gate exists. A test-gated bullet without that binding never silently becomes `deferred`; it blocks upstream.
- Each trace bullet quotes the first 80 chars of the original acceptance bullet verbatim; the trace is exhaustive over `### Automated` + `### Manual`.
