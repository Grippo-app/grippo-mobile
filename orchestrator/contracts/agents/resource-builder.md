# Contract: agents/resource-builder

Frozen per-agent contract. Source of truth: the `design-system` skill; this record pins the role's stable surface.

## Identity (pinned)
- name: `resource-builder`
- kind: `builder`
- tools (permission boundary): `Read, Edit, Write, Bash, Grep, Glob`
- model: `sonnet`

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
