# validation-gates — reference routing

Self-contained rule pack for the validation/review gate. These files carry the
skill's OWN rules — the gate reads no external rule docs at runtime.

## Validator / topic → reference file

| validator (machine, `validator-finding`) | topic | reference file |
|---|---|---|
| `build-validator` *(always-on)* | compiles / build green | [forbidden-patterns.md](forbidden-patterns.md) § Build |
| `scope-leak-validator` *(always-on)* | task footprint, no out-of-scope edits | [forbidden-patterns.md](forbidden-patterns.md) § Orchestrator scope · [when-to-stop-and-ask.md](when-to-stop-and-ask.md) |
| `acceptance-tracer` *(always-on)* | every acceptance criterion traced to code | task spec acceptance criteria (per-task) |
| `architecture-validator` | module layout, architecture-shape | [forbidden-patterns.md](forbidden-patterns.md) § Architecture-shape |
| `mvi-contract-validator` | State/Action/Event contract | [forbidden-patterns.md](forbidden-patterns.md) § State, § Collections in state |
| `di-validator` | Koin module shape, no hand-DSL / `getKoin().get()` | [forbidden-patterns.md](forbidden-patterns.md) § Dependency Injection |
| `data-layer-validator` | repositories, DTOs, error mapping | [forbidden-patterns.md](forbidden-patterns.md) § Data layer, § Errors |
| `compose-stability-validator` | recomposition / stability, inline `dp`·`Color` | [forbidden-patterns.md](forbidden-patterns.md) § Compose, § Resources |
| `naming-convention-validator` | naming across layers | [forbidden-patterns.md](forbidden-patterns.md) § Architecture-shape (naming) |
| `anti-pattern-scanner` | forbidden-pattern keyword scan | [forbidden-patterns.md](forbidden-patterns.md) (whole reference), incl. § Comments |
| `inputs-resolver` | required task inputs resolved before run | task spec `## Inputs` / intake (per-task) |
| `figma-component-coverage` *(conditional, `figmaEnabled`)* | component mapping/bindings coverage for design-system components | `implement-figma` skill + `contracts/agents/figma-component-coverage.md` |
| `figma-drift` *(conditional, `figmaEnabled`)* | surfaces the published component-drift comparison, suggestion-only (token AND component comparison are the server-run local comparators, not validators) | `implement-figma` skill + `contracts/agents/figma-drift.md` |
| `figma-spec-validator` *(conditional, `figmaEnabled` + non-`none` `## Design`)* | declared code values vs cached Figma screen spec; authors the `spec-<stem>.json` report final evidence requires via `write-spec-report.mjs` | [spec-fidelity-gate.md](spec-fidelity-gate.md) |
| `figma-screenshot-validator` *(conditional, `figmaEnabled` + non-`none` `## Design`)* | visual fidelity: Roborazzi capture vs Figma oracle; missing oracle/capture is a BLOCKER | [screenshot-fidelity-gate.md](screenshot-fidelity-gate.md) |
| `backend-contract-drift` *(conditional, `backendContractEnabled` tri-state)* | `false` skips; `auto` skips only without a snapshot; `true` requires a snapshot and treats a missing one as ERROR | backend-contract-client skill `references/drift.md` + `contracts/agents/backend-contract-drift.md` |

Always-on three run on every task; conditional validators run when the
`task-intake` kind applies and **fail closed** (a required-but-skipped one emits
`status: skipped` + `skip_reason`). Dedup is `(file, rule_id)` at routing.

## Reviewer gate (`reviewer-output`)

| reviewer | role | rule source |
|---|---|---|
| `internal-reviewer` | semantic + logic-correctness review | [forbidden-patterns.md](forbidden-patterns.md) (semantic) + reviewer-only register below |
| official Codex plugin `/codex:review` | external native review; parent normalizes its result to `reviewer-output` | same as above |

The reviewer is the pipeline's **only logic-correctness gate** and the only place
the reviewer-only gaps are enforced. Reviewer selection is governed by
[config-gates.md](config-gates.md) § `codexEnabled`; the runtime-verify gate by
§ `verifyEnabled`.

## Reviewer-only rules (no mechanical validator)

These rules are caught **only by the reviewer** — no validator greps them. They
MUST stay referenced; losing one silently is a behavior regression. The full
register lives at:

→ **[orchestrator/skills/_index/known-gaps.md](../../../../orchestrator/skills/_index/known-gaps.md)** — groups `self-enforced`,
`pattern-orphans`, `duplicate-finding-knowns`; source-linked to
`../../_index/known-gaps.md`.

The individual reviewer-only entries are also flagged inline in
[forbidden-patterns.md](forbidden-patterns.md) (e.g. `buildList {}` without
`.toImmutableList()`, subgrouped `<Product>Api`, Compose-Nav alongside Decompose,
mutable routes, raw `Throwable` from `validateResponse`, PNG vector-candidates,
`@JvmStatic` in `commonMain`, `Channel` over `Flow`, PII logging, multiple `Json`
instances, flat-scalar entity composables, inline `ImageVector.Builder`
placeholders, non-`*Screen` UI beside the seven MVI files, flat scalar pile in
`*State`).

## Reference files

- [forbidden-patterns.md](forbidden-patterns.md) — the owned anti-patterns rule set (complete).
- [screenshot-fidelity-gate.md](screenshot-fidelity-gate.md) — `figma-screenshot-validator` capture spec (Roborazzi `ScreenshotTest.kt`, density/qualifier, capture↔comparator contract) + the validator's frozen surface.
- [spec-fidelity-gate.md](spec-fidelity-gate.md) — `figma-spec-validator` procedure (extract-compose-model + compare-screen-spec machine baseline, four comparison families, severities, the `spec-<stem>.json` report the final evidence bundle requires) + the validator's frozen surface.
- [when-to-stop-and-ask.md](when-to-stop-and-ask.md) — escalation stop points + what-you-can-change-without-asking.
- [config-gates.md](config-gates.md) — `codexEnabled` / `verifyEnabled` gate selectors.
- [orchestrator/skills/_index/known-gaps.md](../../../../orchestrator/skills/_index/known-gaps.md) — reviewer-only gap register (MUST stay enforced).

## Output contracts (unchanged)

- `orchestrator/contracts/validator-finding.md` — normalized machine finding.
- `orchestrator/contracts/validation-run.md` — invocation envelope (which ran / expected, fail-closed).
- `orchestrator/contracts/reviewer-output.md` — shared reviewer output shape.
- `orchestrator/contracts/agents/<validator>.md` — per-validator frozen contracts.
