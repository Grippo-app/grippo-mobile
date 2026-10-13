---
name: task-orchestrator
description: Execute a Grippo repository task through coherent implementation slices, self-review, independent review and scoped verification. Use for multi-owner changes or coordinating authorized agents; works directly from the user request and source.
---

# Local task execution

Apply [project conventions](../../../CLAUDE.md). Resolve material uncertainty
through [task-prep](../task-prep/SKILL.md), then carry the authorized task to completion.
The coordinating agent may implement work; a separate reviewer supplies independence.

## Execution loop

1. Read the actual scope, source and relevant owning skills. Preserve unrelated
   user changes; inspect the working tree before editing and never reset it.
2. Implement the smallest coherent slice that meets a concrete acceptance outcome.
   Update every necessary caller within an authorized migration, rather than leaving
   incompatible callers for a hypothetical follow-up.
3. Delegate only when the user or applicable instructions authorize agents. Give
   each agent a clear outcome, file ownership, prerequisites and verification limits.
   Keep dependent edits sequential; resolve shared-file ownership before writing.
   Subagents must not expand scope or delegate without authorization.
4. Self-review actual source/diff for correctness, simplicity, boundaries, privacy,
   lifecycle and cancellation. Apply each owning skill's mandatory completion gate.
   Feature work also requires analytics screen/control/operation coverage and the
   preservation inventory; a build cannot replace either source gate.
5. Obtain independent review from a permitted reviewer that did not author that
   area. Provide requirements, changed files, decisions and actual checks; ask for
   concrete defects with file/line evidence and severity. A writer's self-review
   is useful but does not count as independent review.
6. Resolve every blocking finding and re-review the changed area. Repeat while
   new actionable evidence appears. If the same unresolved finding returns twice,
   stop dependent work, identify the cause and ask a concise question or report
   the missing capability. Do not loop endlessly or redefine a failure as a pass.
7. Run authorized source/manual/build checks on the resulting revision. If fixes
   change checked code, repeat only checks affected by those fixes. Unavailable
   checks remain unverified with their reason; no fabricated machine reports.

## Completion gate

- Requested outcomes and relevant edge/error/cancellation paths are accounted for.
- Owning semantic gates pass source review; independent review has no unaccepted
  blocking findings. Explicitly retained behavior is recorded as an accepted risk,
  never as a fixed finding or PASS. If independent review is unavailable, report that limitation
  explicitly rather than claiming the review loop completed.
- Every changed caller, dependency and resource is reconciled; no obsolete helper
  or broken reference remains in the changed scope.
- Actual verification results and runtime/platform limitations are recorded.
- The final answer explains behavior changed, verification and remaining material
  risks. No commit, push, deployment or external message is implied by this skill.

Use [validation-gates](../validation-gates/SKILL.md) for cross-cut review. Keep
local reports proportionate to the task; there is no required report schema,
board transition, install scaffold or task file format.
