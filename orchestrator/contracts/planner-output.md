# Contract: planner-output

Frozen output shape of `implementation-planner` ("Output format").
The `task-orchestrator` skill MUST emit a plan with these top-level headings in this exact order, or downstream builders drift on names/paths and the chain breaks at validators.

Source of truth: the `task-orchestrator` skill (`references/planner.md`, Output format) + the per-step pins below.

## Required headings, in order

1. `# Implementation plan — TASK_<N>_<title>`
2. `## Summary` — bullets: builders covered; new files count; modified files count; acceptance bullet counts (Automated structural/resource-gated/build-gated/spec-gated/screenshot-gated/test-gated; Manual).
3. `## Names (canonical)` — one sub-section per builder (`### Names (canonical) — owned by <builder>`).
4. `## Files to create` — table `| Path | Class / Object | Builder |`.
5. `## Files to modify` — table `| Path | Change | Builder |`.
6. `## Public signatures` — verbatim Kotlin for NEW public API only.
7. `## Acceptance mapping`
8. `### Automated` — table `| # | Acceptance bullet (verbatim) | Files / changes that satisfy it | Owner builder |` (≥1 row required).
9. `## Test contract` — the canonical test block in frozen sub-heading order: `### Behavior changes`, `### Tests to create or modify`, `### Regression suites`, `### Platform lanes`, `### Test dependencies`, `### Test applicability`. Per-anchor fields per `planner.md` Step 4b; vocabulary comes from the machine policy (`orchestrator/tasks/test-policy.json`) by version/hash, never prose copies. `### Test applicability` is `- executable` or one typed `- test-not-applicable: <allowlisted value>`.
10. `## Behavioral edge-cases (reviewer-check items)` — task-concrete reviewer items or `- none`. Internal; never an acceptance bullet.
11. `## Builder contracts` — one `#### <builder>` section per builder.
12. `## Open assumptions` — bullets or `- none`; `spec-gap`-tagged invented rules surface here.

## Optional heading

- `### Manual` — between `### Automated` and `## Test contract`; present only when the task has manual bullets. Non-routing.

## Pins the parser must enforce (fail-closed)

- All 12 required headings present and strictly increasing in line order. Missing or reordered → reject (the planner BLOCKs rather than emitting a partial plan).
- `## Test contract` carries all six sub-headings in the frozen order; a `test-not-applicable` value under `### Test applicability` must be one of the machine policy's allowlisted values and excludes any `test:` anchor rows.
- `### Automated` must carry the `Owner builder` column (acceptance-tracer routes off it; no 2-hop lookup).
- Every `test:` anchor in `### Automated` appears under `### Behavior changes`, and vice versa (no orphan anchors in either direction).
- Every builder named in an `Owner builder` column for a structural bullet MUST also appear in `## Builder contracts` (consistency rule, implementation-planner.md Step 4 "Consistency rule").
- No unresolved placeholders (`<package-root>`, `<apiClassName>`, `<iosFrameworkName>`) in a real plan — these resolve from `orchestrator/project-config.md`. (Fixtures keep placeholders deliberately to test shape only.)

## Distinct from `execution-plan`

`planner-output` is the planner's file-level contract (names/paths/signatures/acceptance mapping). `execution-plan` is the orchestrator's resolved routing (task class, builder order, validator set, gate sequence) and references this plan.
