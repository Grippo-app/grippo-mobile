# Contract: test-policy

Why every task must leave executable proof, and where the single machine
authority lives. This document explains rationale and order of operations —
it deliberately copies **no** enum tables: change kinds, evidence classes,
lanes, `test-not-applicable` values, escalation rules and reason codes live
only in `orchestrator/tasks/test-policy.json` (validated and served by
`orchestrator/tasks/task-test-policy-contract.cjs`). Prompts carry the policy
`version`/`policyHash` pointer, never a prose copy; any rendered enumeration
must be generated from — or lint-checked exactly equal to — the machine file.

## The rule

Every task must leave an automated proof of every new or changed observable
behavior and re-verify every affected contract and flow. Coverage units are
behaviors, contracts and risks — never files, lines or the existence of a
`FooTest.kt`. A green build proves compilation, not correctness.

## Why deterministic, not prompt-enforced

The certifying run is a security/durability boundary. Builders may run tests
for feedback, but certification is executed only by the deterministic
caller (`orchestrator/tasks/run-test-certification-request.mjs`) under the
canonical task lock. It drives the receipt producers in
`orchestrator/tasks/run-test-certification.mjs`: allowlisted Gradle task paths,
policy-selected structural gates, sanitized environment, isolated
process group, sealed reports, immutable content-addressed receipts
(`orchestrator/tasks/task-test-receipt-contract.cjs`). A builder report is a
claim; receipts plus the sealed summary whose every field is derived by the
deterministic aggregator (`orchestrator/tasks/task-test-summary-contract.cjs`)
are the evidence. Zero
discovered tests, `NO-SOURCE` where tests are required, all-skipped runs,
cache substitution on the direct tier and fail→pass retries are typed
violations — never a quiet PASS.

## Order of operations per task

1. `task-prep` owns the test-foundation doctor before promotion
   (`orchestrator/tasks/task-test-foundation.mjs`); an absent foundation
   yields exactly one globally deduplicated prerequisite child under the
   bootstrap coordinator's no-clobber marker.
2. The planner proposes behaviors with `test:` anchors; the deterministic
   resolver (`orchestrator/tasks/resolve-test-impact.mjs`) applies the machine
   table after model output and may only widen — the planned impact is sealed
   before builders run.
3. Builders implement production and test changes in their ownership; the
   observed impact is recomputed from the actual content footprint
   (`orchestrator/tasks/content-snapshot.cjs` byte domain) and may only keep
   or widen the plan.
4. Step 4.4 certification executes the required tiers and lanes; the
   acceptance tracer turns `test-gated` bullets into `verified` only through a
   fresh PASS receipt bound to the current snapshot.
5. Finalization re-verifies the sealed summary against the exact pre-Outcome
   task input (`test-task-input` domain) inside the existing finalize
   transaction; adding the Outcome never stales its own evidence, changing
   any pre-Outcome byte does.

## Evidence classes without a producer yet

Two evidence classes exist in the vocabulary but have no sealed producer here,
so nothing can bind them into a summary. Demanding one would not certify a
task, it would block that task forever — so the policy does not demand them:

- `fail-before-pass-after` is declared in `evidenceClasses` and demanded by no
  change kind. A trustworthy red run needs the same test identity executed
  against an exact baseline: a per-task worktree (pipeline improvement 01) or,
  until that lands, a safe isolated copy preserving that baseline. `bugfix`
  therefore certifies on `regression-test` alone; the class returns to its
  `minimumEvidence` in the same cut that lands the producer.
- `coverage` as a required capability still blocks: the Kover lane exists in
  the convention layer and CI, but no sealed coverage artifact reaches the
  summary, so an impact that declares the capability cannot be satisfied.

The gates stay wired for the day those producers land — a change kind that
demands `fail-before-pass-after` still cannot reach `PASS`, and neither can an
impact requiring `coverage`. Both halves are pinned by `policy-only evidence
cannot disappear into a false-green aggregate` in
`orchestrator/tasks/tests/test-test-executor.mjs`; that no kind demands the
red/green class is pinned by `policy semantics` in
`orchestrator/tasks/tests/test-test-policy-contract.mjs`.

## What stays manual

Human pixel verdicts for canvas/glass blind spots, hardware-only behavior,
store/external-account operations, subjective copy approval and owner
acceptance. A test PASS never auto-closes a manual bullet; a testable runtime
behavior never hides in Manual just because a diff can't prove it.

## Non-negotiables

One policy version at a time (unknown versions fail closed); one owner per
source set and Gradle alias; no compatibility aliases, feature-flagged dual
policies or silent fallbacks to guessed task names; `SKIPPED` only as a proven
typed N/A with structural-gate receipts; flaky means BLOCKED, not retried
into green; JVM coverage is labelled `jvm-host-coverage` and never presented
as KMP coverage.
