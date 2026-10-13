---
name: validation-gates
description: Review a Grippo change against requirements, architecture, privacy, lifecycle, analytics coverage and state preservation before completion. Use for a diff review or implementation completion; source/manual checks and authorized builds replace no semantic gate.
---

# Validation and review

Read [project conventions](../../../CLAUDE.md), the user's accepted scope and the
actual change set. A review needs an identifiable outcome and footprint; recover
these from the request/source before asking for missing information.

## Mandatory review

1. Trace each requested outcome to actual code and callers. Review happy paths,
   rejected inputs, empty/nullable data, failure, cancellation and lifecycle edges
   relevant to the change. Do not infer runtime success from a keyword scan.
2. Apply [forbidden patterns](references/forbidden-patterns.md) and the owning
   skill's semantic completion gate. Review correctness, simplicity, dependencies,
   privacy and work done in frequently called paths.
3. Feature behavior changes require [analytics](../feature-analytics/SKILL.md):
   actual route/surface → Component → VM → screen identity → hook coverage, eligible
   callbacks, complete-operation results/errors and explicit event omissions.
   Independent screens need exactly one screenPresentation; containers keep null.
   Missing/wrong hooks and duplicate views block completion; an omission cannot waive them.
4. Feature changes require [preservation](../state-preservation/SKILL.md): actual
   input/selection/filter/parent owners, saved drafts, rebuilt sources, secret/runtime
   exclusions, fresh restoration wiring and no shadow flags/caches. Dialog sessions
   remain unsaved; accepted results belong to the screen's draft. No-state changes
   need an impact assessment only.
5. UI/design work compares real consumers and declared design states through
   [visual review](references/visual-review.md). Source compliance does not prove
   rendered fidelity; note states/platforms that could not be inspected.
6. Review real source/diff and run checks within the user's authorization. Builds
   prove compilation; static checks do not prove native lifecycle, SDK delivery,
   generated DI runtime or restoration. Do not create automated test sources,
   dependencies or capture infrastructure without an explicit request.

## Findings and review loop

Report file/line, concrete trigger, consequence and required correction. Severity:
blocking (incorrect behavior, broken contract, privacy leak or mandatory gate),
nonblocking suggestion, or cosmetic nit. Preserve useful evidence from multiple
reviewers; route a shared defect once, at its highest justified severity.

The author self-reviews, then a reviewer independent of that area reviews it.
Fix blocking findings and re-review the changed area. Two rounds returning the
same unresolved issue require a clear cause/question, not an endless retry.
An explicit user decision to retain a known behavior closes that finding as an
accepted risk, not a fix or PASS. Record the trigger and consequence; do not infer
acceptance from silence or use it to waive unrelated mandatory coverage gates.
Unavailable independent review must be disclosed; self-review never substitutes
for it. Follow [local execution](../task-orchestrator/SKILL.md).

## Completion record

Record reviewed scope, unresolved findings, actual checks/results and unverified
runtime/platform behavior. No missing input or unrun check is a PASS. An unavailable
runtime/build check alone does not block completion after the required source gate,
provided its limitation is explicit and no unaccepted source defect remains.
For scope decisions use [when to ask](references/when-to-stop-and-ask.md).
