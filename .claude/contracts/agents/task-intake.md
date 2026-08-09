# Contract: agents/task-intake

Frozen per-agent contract. Source of truth: the `task-prep` skill; this record pins the role's stable surface.

## Identity (pinned)
- name: `task-intake`
- kind: `helper`
- tools (permission boundary): `Read, Bash, Grep, Glob`
- model: `opus`

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
