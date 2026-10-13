# Contract: planner-output

Frozen output shape of `implementation-planner` ("Output format").
The `task-orchestrator` skill MUST emit a plan with these top-level headings in this exact order, or downstream builders drift on names/paths and the chain breaks at validators.

Source of truth: the `task-orchestrator` skill (`references/planner.md`, Output format) + the per-step pins below.

## Required headings, in order

1. `# Implementation plan — TASK_<N>_<title>`
2. `## Summary` — bullets: builders covered; new files count; modified files count; acceptance bullet counts (Automated structural/resource-gated/build-gated/spec-gated; Manual).
3. `## Names (canonical)` — one sub-section per builder (`### Names (canonical) — owned by <builder>`).
4. `## Files to create` — table `| Path | Class / Object | Builder |`.
5. `## Files to modify` — table `| Path | Change | Builder |`.
6. `## Public signatures` — verbatim Kotlin for NEW public API only.
7. `## Acceptance mapping`
8. `### Automated` — table `| # | Acceptance bullet (verbatim) | Files / changes that satisfy it | Owner builder |` (≥1 row required).
9. `## Behavioral edge-cases (reviewer-check items)` — task-concrete reviewer items or `- none`. Internal; never an acceptance bullet.
10. `## Builder contracts` — one `#### <builder>` section per builder.
11. `## Open assumptions` — bullets or `- none`; `spec-gap`-tagged invented rules surface here.

## Optional heading

- `### Manual` — after `### Automated`; present when runtime or human verification is needed. Non-routing.

## Pins the parser must enforce (fail-closed)

- All 11 required headings present and strictly increasing in line order. Missing or reordered → reject (the planner BLOCKs rather than emitting a partial plan).
- `### Automated` must carry the `Owner builder` column (acceptance-tracer routes off it; no 2-hop lookup).
- Every builder named in an `Owner builder` column for a structural bullet MUST also appear in `## Builder contracts` (consistency rule, implementation-planner.md Step 4 "Consistency rule").
- No unresolved placeholders (`<package-root>`, `<apiClassName>`, `<iosFrameworkName>`) in a real plan — these resolve from `orchestrator/project-config.md`. (Examples retain placeholders to illustrate the shape.)

## Distinct from `execution-plan`

`planner-output` is the planner's file-level contract (names/paths/signatures/acceptance mapping). `execution-plan` is the orchestrator's resolved routing (task class, builder order, validator set, gate sequence) and references this plan.
