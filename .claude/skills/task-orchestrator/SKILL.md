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
3. MUST delegate only when the user or applicable instructions authorize agents. Give
   each agent a clear outcome, file ownership, prerequisites and verification limits.
   Keep dependent edits sequential; resolve shared-file ownership before writing.
   Subagents must not expand scope or delegate without authorization.
4. Self-review actual source/diff for correctness, simplicity, boundaries, privacy,
   lifecycle and cancellation. Apply each owning skill's mandatory completion gate.
   Feature work also requires analytics screen/control/operation coverage and the
   preservation inventory; a build cannot replace either source gate.
5. MUST obtain independent review for behavior, architecture or multi-owner changes
   from a permitted reviewer that did not author that area. Small isolated editorial
   changes require self-review only. Provide requirements, changed files, decisions
   and actual checks; ask for concrete defects with file/line evidence and severity. A writer's self-review
   is useful but does not count as independent review. If no permitted reviewer
   is available, complete the authorized work and available checks, and explicitly
   record independent review as unperformed; MUST NOT claim it passed.
6. Resolve every blocking finding and re-review the changed area. Repeat while
   new actionable evidence appears. If the same unresolved finding returns twice,
   MUST reassess its cause and continue fixing when a local correction is available.
   Ask only for a real product/scope blocker or report a missing capability; two
   rounds are not a hard cap. MUST NOT repeat an unchanged ineffective attempt
   endlessly or redefine a failure as a pass.
7. MUST run relevant source/manual checks on the resulting revision. Builds needed
   to verify the task are permitted within its scope without repeated consent. If fixes
   change checked code, repeat only checks affected by those fixes. Unavailable
   checks remain unverified with their reason; no fabricated machine reports.

## Completion gate

- Requested outcomes and relevant edge/error/cancellation paths are accounted for.
- Owning semantic gates MUST pass source review; completed independent review
  MUST have no unaccepted blocking findings. Explicitly retained behavior is recorded as an accepted risk,
  never as a fixed finding or PASS. If required independent review is unavailable,
  MUST report that limitation explicitly rather than claiming the review loop completed.
- Every changed caller, dependency and resource is reconciled; no obsolete helper
  or broken reference remains in the changed scope.
- Actual verification results and runtime/platform limitations are recorded.
- The final answer explains behavior changed, verification and remaining material
  risks using the canonical [completion response format](../validation-gates/SKILL.md#final-response-format).
  No commit, push, deployment or external message is implied by this skill.

Use [validation-gates](../validation-gates/SKILL.md) for cross-cut review. Keep
reports proportionate to the task. Follow the canonical
[verification policy](../../../CLAUDE.md#verification-policy).
