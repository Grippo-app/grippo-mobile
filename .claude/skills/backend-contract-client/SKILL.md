---
name: backend-contract-client
description: Verify Grippo backend wire contracts before DTO, GrippoApi or backend-facing mapper changes; audit mobile/backend drift with authoritative backend evidence.
---

# Grippo backend contract

The backend owns endpoint methods, paths, fields, types, dictionary keys and semantics.
Before a wire change identify an available authoritative backend OpenAPI/Swagger document or
backend source and record its environment/revision. An unavailable contract blocks only dependent
wire decisions; continue independent work. Task text and existing mobile DTOs alone do not prove
server behavior. Resolve missing details with the user before implementing them.

## Workflow

1. Read the exact operation and referenced schemas. Compare method/path/query/body/response,
   null-versus-omitted/empty semantics, error outcomes and related consumers.
2. Hand-author mobile changes only within authorized scope. Keep every response/request DTO
   field and collection nullable with default null and explicit exact SerialName. Server-required
   request values are validated before sending; do not relax wire validity.
3. DTOs retain raw wire strings/numbers. Domain promotion and unknown-value handling follow
   [mappers](../mappers/SKILL.md). Preserve exact enum keys; do not infer a dictionary from examples.
4. During a drift audit report evidence and a concrete correction proposal. Apply fixes only when
   the task authorizes them; an authorized fix does not require a separate ticket or second approval.
5. Read actual transport/data consumers and preserve cancellation and the existing logging configuration.

## Completion gate

Review each changed wire field and operation against named authoritative evidence, required
request construction and all affected mapper/domain consumers. No codegen, fabricated endpoints,
empty-by-default assumptions or compatibility claims based on mobile source alone. Identify
existing source deviations separately from the new standard; repair only within task scope.
Run permitted static/manual checks and builds within user scope; no automated test infrastructure.
Report missing evidence and unverified runtime/server behavior.

Read [contract evidence](references/overview.md) when selecting/contrasting sources and
[drift review](references/drift.md) when auditing compatibility.
