# Visual review

Apply for changed UI/layout/resources or a requested visual audit. Product UI uses
[brand-design](../../brand-design/SKILL.md) for usage/state analysis and composition
acceptance, and [design-system](../../design-system/SKILL.md) for implementation.
Standalone artwork review stays with [illustrations](../../illustrations/SKILL.md).

## Evidence gate

- Apply the [standard viewport scope](../../design-system/references/visual-consistency.md#default-viewport-scope).
  Small-device appearance is not an acceptance blocker or a required review loop.
- MUST reconcile final source against the required
  [usage/state inventory](../../brand-design/references/usage-and-states.md).
  Use [preview coverage](../../design-system/references/previews.md) and
  [whole-screen checks](../../design-system/references/visual-consistency.md) for
  the distinct groups. An isolated component render cannot verify its host.
- For a supplied design, inspect the authoritative node/artifact through available
  supported tools. Record the exact content/state compared; preserve declared
  variant axes and leave missing design facts explicit.
- Record observations for the final revision, fix material mismatches and re-inspect
  affected groups. Distinguish source review, static renders, runtime interaction
  and compilation. A token scan or green build does not establish visual fidelity.
- Identify unrendered/unexercised groups, their unavailable inputs and any unresolved
  design decision. Do not present missing observations as passed checks.

Permitted checks and verification facilities follow
[CLAUDE.md](../../../../CLAUDE.md#verification-policy). Use the canonical
[execution loop](../../task-orchestrator/SKILL.md#execution-loop) for reviewer
eligibility and correction rounds.
