# Contract: agents/figma-screenshot-validator

Frozen per-agent contract. Source of truth: the `validation-gates` skill; this record pins the role's stable surface.

## Identity (pinned)
- name: `figma-screenshot-validator`
- kind: `validator`
- tools (permission boundary): `Read, Bash, Grep, Glob`
- model: `sonnet`

## Current review contract

For a declared Figma-backed surface, inspect permitted actual previews/app
screenshots against the design according to
[visual-review.md](../../skills/validation-gates/references/visual-review.md).
Report observations and unavailable states separately. No capture runner,
recording plugin, image baseline or screenshot certificate is required.
Unavailable rendering remains unverified manual acceptance; it is not a visual
PASS. Report observed defects through the normal finding/owner route.

## Frozen surface (per source spec)
The skill that owns this role MUST preserve:
- role semantics and when it runs;
- required inputs (and the BLOCKED message when an input is missing);
- required outputs / output contract;
- stop-and-ask conditions;
- allowed writes and forbidden writes (bounded by the tool budget above);
- required source reads;
- task footprint rules;
- whether it may spawn other agents (tool budget is the hard boundary).

This contract is a FROZEN record. Its `name`/`tools` and rules live in the skills that cite it; treat any change here as a deliberate re-freeze.
