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

The machine-validator pipeline below applies to configured orchestrator tasks.
For ordinary repository work, review the authorized scope against these semantic
rules and the owning skills; run permitted checks and record unavailable/unrun
checks. This checkout installs reference contracts, not the orchestrator runtime.
Do not require its scaffold or report machine gates as passed without execution.

## When to use

A task has produced code (builders ran) and must clear the gate before it is
accepted. Use whenever you would otherwise dispatch the validator wave +
reviewer loop, or when asked to review a diff, check an anti-pattern, trace
acceptance criteria, or confirm scope did not leak.

Do NOT use this skill to *write* product code — it only validates and reviews.

## Required inputs

- The task spec or authorized review scope (acceptance criteria, footprint);
  include `task-intake` kind tags for an orchestrator task.
- The produced change set (files touched) for the task.
- The planner's `Behavioral edge-cases` list for an orchestrator task; otherwise
  derive relevant checks from the authorized scope and actual source.

If acceptance criteria or the change set are missing, **stop** — a gate cannot
run on an unknown footprint (fail-closed, do not pass by default).

## Workflow

Review coroutine changes against the [project coroutine rules](references/forbidden-patterns.md#coroutines),
including the prohibition on adding manual `ensureActive()` checks in production.

For feature behavior changes, the semantic reviewer reads
[feature-analytics](../feature-analytics/SKILL.md) and assesses the affected coverage
matrix against actual instrumentation and omissions. Review eligible ViewModel
callbacks, whole-operation result/error/cancellation preservation and the shared
foundation screen lifecycle before navigation delivery. Visual-only changes need
an impact assessment, not new events. Use the existing reviewer gate and report.
Apply its [mandatory screen coverage gate](../feature-analytics/SKILL.md#mandatory-screen-coverage-gate):
check every screen of a new feature and every affected screen against actual
routes/rendered surfaces and VM hooks. Missing/wrong hooks, ownerless screen enum
entries or duplicate container/child views fail review; an omissions row cannot
waive required screen coverage. Containers must keep default null.

For new or changed features, also apply the mandatory
[state-preservation gate](../state-preservation/SKILL.md#mandatory-preservation-gate).
Reconcile affected fields with their owners, saved drafts, rebuilt sources and
runtime exclusions; review fresh-component archive tests and secret omission.
Reject private restoration/edit flags, previous-source caches and equivalent
shadow data in State, helpers, providers or mutable closures. Check that fresh
form population runs once, restored Empty/null/default values survive and source
refresh/pause/resume does not overwrite the draft. Lifecycle tests must cross the
actual disconnect grace before resume; see the owning skill's
[draft policy](../state-preservation/SKILL.md#data-backed-form-synchronization).
Follow the user's verification scope and report actual shell limitations.

For a configured orchestrator task, two layers are required; neither replaces the other.

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
- **Keep reviewer-only checks explicit.** This checkout's applicable rules are
  flagged in [forbidden-patterns.md](references/forbidden-patterns.md) and the
  owning skills. A configured orchestrator additionally requires its
  `orchestrator/skills/_index/known-gaps.md` register; verify that input exists.

## Stop and ask

- A required conditional validator cannot run and you cannot determine its
  `skip_reason` — stop, do not pass.
- A finding implies a breaking API/enum change, a deletion/relocation, or a
  build-config change — escalate (see `references/when-to-stop-and-ask.md`).
- The reviewer reports a logic-correctness defect the builders cannot self-route.

## References to read

This skill carries its OWN core validator rules. Integration validators and
feature-specific semantic review also defer to the owning skill references below.

- Reference routing (validator/topic → file): `references/index.md`.
- Anti-patterns reference (owned; routing target for `anti-pattern-scanner` and the
  reviewer): `references/forbidden-patterns.md`.
- Feature behavior analytics impact, coverage and semantic checks:
  [feature-analytics](../feature-analytics/SKILL.md).
- Stop-and-ask escalation points (owned; for `scope-leak-validator` + reviewer):
  `references/when-to-stop-and-ask.md`.
- Review/verify gate selectors (`codexEnabled` / `verifyEnabled`):
  `references/config-gates.md`.
- Screenshot-fidelity gate (`figma-screenshot-validator` capture spec + frozen
  surface): `references/screenshot-fidelity-gate.md`.
- Spec-fidelity gate (`figma-spec-validator` procedure + the `spec-<stem>.json`
  report final evidence requires): `references/spec-fidelity-gate.md`.
- Reviewer-only rules in `references/forbidden-patterns.md` and owning skills;
  configured orchestrator tasks also require their `known-gaps.md` register.
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
