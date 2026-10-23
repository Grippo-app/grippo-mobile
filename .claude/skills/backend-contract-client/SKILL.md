---
name: backend-contract-client
description: Verify Grippo backend contracts before wire behavior changes in DTOs, GrippoApi or backend-facing mappers; audit mobile/backend drift with authoritative backend evidence.
---

# Grippo backend contract

The backend owns endpoint methods, paths, fields, types, dictionary keys and semantics.
Before a wire change apply the [evidence trigger](#evidence-trigger-canonical) and
identify the facts the accepted evidence establishes. An unavailable contract blocks
only dependent wire decisions; continue independent work and resolve missing facts
with the user. Existing mobile DTOs alone do not prove server behavior.

## Evidence trigger (canonical)

MUST verify authoritative backend evidence for endpoint/method/path/query changes, wire keys,
types, dictionary values, payload meaning, required request values or null/omission/empty behavior.
A Kotlin-nullability-only alignment MAY reuse previously verified wire keys only after source
review proves serialization and supplied call-site values unchanged. Existing mobile source alone
MUST NOT be treated as backend evidence; unknown serialization behavior blocks this exception.

Accepted evidence classes, in order of preference: (1) an OpenAPI/Swagger document or the
backend source at a named revision; (2) an **owner-attested contract** - the user, who owns the
Grippo backend, states in chat the exact method, path, wire keys, types, null/omitted/empty
semantics and error outcomes of the operation. An owner-attested contract is recorded verbatim
in the task report with its date; missing required facts are asked for directly, never
inferred; a later OpenAPI/source that contradicts it is reported, not silently adopted.
DTO collection properties MUST be nullable with `= null`; element nullability/types MUST follow
the verified backend contract, not a blanket nullable-element convention.

## Workflow

1. Read the exact operation and referenced schemas. Compare method/path/query/body/response,
   null-versus-omitted/empty semantics, error outcomes and related consumers.
2. Hand-author mobile changes inside the authorized scope (no codegen). Keep every response/request DTO
   field and collection nullable with default null and explicit exact SerialName. Server-required
   request values are validated before sending; do not relax wire validity.
3. DTOs retain raw wire strings/numbers. Domain promotion and unknown-value handling follow
   [mappers](../mappers/SKILL.md). Preserve exact enum keys; do not infer a dictionary from examples.
4. During a drift audit report evidence and a concrete correction proposal. Apply fixes only when
   the task authorizes them; an authorized fix does not require a separate ticket or second approval.
5. Read actual transport/data consumers and preserve cancellation and the existing logging configuration.

## Completion gate

MUST review each actual wire-behavior delta against named authoritative evidence, required
request construction and all affected mapper/domain consumers. For the bounded nullability-only
exception, MUST record the unchanged-serialization/call-site proof and previously verified keys.
MUST NOT use codegen, fabricated endpoints,
empty-by-default assumptions or compatibility claims based on mobile source alone. A deviation the
audit finds in current source is reported separately from the standard and repaired only inside
the authorized scope. Run the permitted checks under the
[verification policy](../../../CLAUDE.md#verification-policy) and report missing evidence and
unverified runtime/server behavior.

Read [contract evidence](references/overview.md) when selecting/contrasting sources and
[drift review](references/drift.md) when auditing compatibility.
