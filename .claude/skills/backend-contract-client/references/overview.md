# Authoritative contract evidence

Use the [accepted evidence classes](../SKILL.md#evidence-trigger-canonical).
For OpenAPI or backend source, use an identified URL/path and record its revision,
environment and the exact operation/schema inspected. Discover an existing task
reference or ask for the missing source; do not guess a production docs path,
repository location or credentials. For an owner-attested contract, retain only
the exact facts stated by the user and record them as the evidence trigger requires.

The read-only [backend-contract-auditor](../../../agents/backend-contract-auditor.md)
gathers evidence and reports drift. When accepted evidence does not establish a
required fact, report that fact as unavailable and name what the user must provide.

- OpenAPI establishes declared structure. Backend source establishes the implementation at its
  inspected revision. If they conflict, name the contradiction; do not silently pick whichever fits mobile.
- An example response or Postman request corroborates observed behavior but does not establish
  required fields, complete enum dictionaries or all nullability cases.
- Current mobile DTO/API source locates consumers and drift; it is not the server authority.
- Never fetch or replay mutating production requests merely to discover the contract. Use read-only
  documentation/source; external effects require authorization covering those effects.
- Record field absence/null/empty semantics when relevant. Mobile nullable DTO discipline is a
  defensive serialization standard and does not mean required server request values may be omitted.

For the precise wire-change trigger and bounded Kotlin-nullability-only exception, MUST use
[the canonical evidence rule](../SKILL.md#evidence-trigger-canonical).
