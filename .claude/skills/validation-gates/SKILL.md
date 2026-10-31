---
name: validation-gates
description: Review Grippo diffs and completion against the request and owning skill gates. Own cross-cut findings, code style, scope questions and final replies.
---

# Validation and review

Read [CLAUDE.md](../../../CLAUDE.md), the user's accepted scope and the
[task delta against its baseline](../task-orchestrator/references/change-attribution.md).
Use the current source and full working-tree diff as context, not as proof that
every existing edit belongs to this task. A review needs an identifiable outcome
and footprint; recover them from the request and the source before asking.

## Mandatory review

1. Trace each requested outcome to code and callers. Review the happy path,
   rejected input, empty/nullable data, failure, cancellation and lifecycle edges
   relevant to the change. A keyword scan is not a review.
2. Apply [forbidden patterns](references/forbidden-patterns.md) and
   [code style](references/code-style.md), then the owning skill's completion gate
   ([skills index](../../../CLAUDE.md#skills-index)). Review correctness,
   simplicity, dependency direction, privacy and work done in frequently called paths.
3. A behavior change passes the [analytics screen coverage gate](../feature-analytics/SKILL.md#mandatory-screen-coverage-gate)
   and the [preservation gate](../state-preservation/SKILL.md#mandatory-preservation-gate).
   The [admin synchronization gate](../feature-analytics/references/admin-sync.md)
   applies under the trigger it owns; missing admin access leaves that task
   unfinished, not merely unverified.
4. A visual change passes [brand-design acceptance](../brand-design/references/workflow-and-review.md#design-acceptance)
   and the design-system gates; artwork passes the
   [illustration completion gate](../illustrations/SKILL.md#completion). Evidence
   handling is owned by [visual consistency](../design-system/references/visual-consistency.md).
5. Checks follow the [verification policy](../../../CLAUDE.md#verification-policy):
   an available, authorized check must have run; compilation proves no runtime,
   visual or restoration behavior.

## Review completeness

Reconcile the prepared affected-scope inventory against the final source and
actual task delta within the reviewer's role. For each applicable item, inspect
the owning implementation and affected callers: contract migration, significant
states, loading geometry, previews, resources, records and obsolete in-scope
APIs, imports or helpers. Use the owning skills' gates; a healthy item needs no
edit. Extend the inventory if review reveals another affected path.

Return one completed role-scoped report with all findings, covered scope and
explicit gaps. Interim messages can flag blockers or progress, but do not replace
that reconciliation. An incomplete assigned source review cannot receive PASS;
report missing runtime or render evidence separately under its owning policy.
Review coordination and correction batches belong to
[task-orchestrator](../task-orchestrator/SKILL.md#execution-loop).

## Findings

Report `path:line`, the concrete trigger, the consequence and the required
correction at its real owner. Severity:

- **blocking** - incorrect behavior, broken contract, privacy leak, forbidden
  pattern, or a mandatory gate not met;
- **suggestion** - a real improvement that does not block completion;
- **nit** - cosmetic.

A deviation from [code style](references/code-style.md) in changed lines is at
least a suggestion; it is blocking when [forbidden patterns](references/forbidden-patterns.md)
names it.

A defect reported twice is routed once at its highest justified severity. A
behavior the user explicitly kept is an accepted risk with its trigger and
consequence recorded; it is neither a fix nor a pass. Silence is not acceptance
and cannot waive a mandatory gate. The fix-and-re-review loop and the recurring
findings policy are owned by [task-orchestrator](../task-orchestrator/SKILL.md).

## Completion

The [definition of done](../../../CLAUDE.md#definition-of-done) decides.
Record the reviewed scope, unresolved findings, actual checks with results and
the unverified runtime/platform behavior. An unrun check is never a pass. For
scope decisions use [when to stop and ask](references/when-to-stop-and-ask.md).

## Final response format

Before replying to the user, read the [final response format](references/final-response.md).
Independent reviewers use their own role report format.
