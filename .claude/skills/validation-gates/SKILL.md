---
name: validation-gates
description: >-
  Validation and review gate for a finished task — run when you need to
  validate, review, or gate produced code before it merges. Covers
  architecture / MVI / DI / naming / anti-pattern / scope / acceptance /
  build / data-layer / Compose-stability / inputs checks, the two-layer
  machine-validator + agent-reviewer pipeline, and the reviewer-only gaps
  that no validator catches mechanically. Use for "validate this", "review
  before merge", "did this break an anti-pattern", "is scope leaking",
  "trace acceptance", "reviewer-only gaps", or "run the gates".
---

# validation-gates

Owns the VALIDATORS + reviewers and the anti-patterns rule set.
Operational entrypoint — this skill routes, it does not redefine the gates.

## When to use

A task has produced code (builders ran) and must clear the gate before it is
accepted. Use whenever you would otherwise dispatch the validator wave +
reviewer loop, or when asked to review a diff, check an anti-pattern, trace
acceptance criteria, or confirm scope did not leak.

Do NOT use this skill to *write* product code — it only validates and reviews.

## Required inputs

- The task spec (acceptance criteria, footprint) and its `task-intake` kind tags.
- The produced change set (files touched) for the task.
- The planner's `Behavioral edge-cases` list — the reviewer's per-task check items.

If acceptance criteria or the change set are missing, **stop** — a gate cannot
run on an unknown footprint (fail-closed, do not pass by default).

## Workflow

Two layers, both required; neither replaces the other.

1. **Machine validators** — emit `validator-finding`. The three always-on
   validators (`build-validator`, `scope-leak-validator`, `acceptance-tracer`)
   run on EVERY task regardless of intake. Conditional validators run when
   their kind applies; a required conditional that does not run **must emit a
   `skipped` finding with a `skip_reason`** — it fails closed, it does not
   silently vanish.
2. **Independent reviewers** — `internal-reviewer` or the official Codex plugin
   command keep the semantic judgement. Both normalize to `reviewer-output`
   (reviewer-agnostic). The reviewer is the pipeline's **only
   logic-correctness gate**, and it is the only place the reviewer-only gaps are
   enforced.

Routing rules (frozen):
- **Dedup by `(file, rule_id)`** — the same call site from two validators counts
  as one. Known duplicate-finding cases are listed in the gap register and are
  intentionally kept (parity), not collapsed.
- `suggestion_only` / advisory findings are not routed as blocking; high-severity
  = `Critical|Major` (validators) / `Blocker|Critical|Major` per finding severity.
- **A missing required report fails the gate.**
- **Keep the reviewer-only gap register** — `orchestrator/skills/_index/known-gaps.md`
  lists every rule no validator catches mechanically. These MUST be carried as
  explicit reviewer-check items; losing one silently is a behavior regression.

## Stop and ask

- A required conditional validator cannot run and you cannot determine its
  `skip_reason` — stop, do not pass.
- A finding implies a breaking API/enum change, a deletion/relocation, or a
  build-config change — escalate (see `references/when-to-stop-and-ask.md`).
- The reviewer reports a logic-correctness defect the builders cannot self-route.

## References to read

This skill carries its OWN core validator rules. Integration validators also defer to the owning
Figma/backend skill references named below.

- Reference routing (validator/topic → file): `references/index.md`.
- Anti-patterns reference (owned; routing target for `anti-pattern-scanner` and the
  reviewer): `references/forbidden-patterns.md`.
- Stop-and-ask escalation points (owned; for `scope-leak-validator` + reviewer):
  `references/when-to-stop-and-ask.md`.
- Review/verify gate selectors (`codexEnabled` / `verifyEnabled`):
  `references/config-gates.md`.
- Screenshot-fidelity gate (`figma-screenshot-validator` capture spec + frozen
  surface): `references/screenshot-fidelity-gate.md`.
- Spec-fidelity gate (`figma-spec-validator` procedure + the `spec-<stem>.json`
  report final evidence requires): `references/spec-fidelity-gate.md`.
- Reviewer-only gap register (MUST stay enforced):
  `orchestrator/skills/_index/known-gaps.md`.
- Per-validator frozen contracts: `orchestrator/contracts/agents/<validator>.md`.

## Validators / gates

Always-on (every task): `build-validator`, `scope-leak-validator`,
`acceptance-tracer`.

Pre-flight (every task, Step 1a — not conditional): `inputs-resolver`.

Conditional (run when the intake kind applies, fail-closed on skip):
`architecture-validator`, `mvi-contract-validator`, `di-validator`,
`data-layer-validator`, `compose-stability-validator`,
`naming-convention-validator`, `anti-pattern-scanner`,
`figma-component-coverage`, `figma-drift`, `figma-spec-validator`
(`figmaEnabled` + non-`none` `## Design`; runs the machine baseline and authors
the `spec-<stem>.json` report final evidence requires via `write-spec-report.mjs`
— see `references/spec-fidelity-gate.md`), `figma-screenshot-validator`
(`figmaEnabled` + ANY non-`none` `## Design` bullet — any kind
(screen/dialog/component/overlay); missing oracle/capture emits BLOCKER findings
— see `references/screenshot-fidelity-gate.md`), `backend-contract-drift`.

Reviewer gate (semantic, logic-correctness, reviewer-only gaps):
`internal-reviewer` or `Skill(skill: "codex:review", args: "--wait --scope working-tree")`.

## Output contract

- Machine validators → `orchestrator/contracts/validator-finding.md`.
- Acceptance tracer transient per-bullet report →
  `orchestrator/contracts/acceptance-tracer-report.md`.
- Validator-invocation envelope (which ran / were expected, fail-closed) →
  `orchestrator/contracts/validation-run.md`.
- Reviewers → `orchestrator/contracts/reviewer-output.md`.
