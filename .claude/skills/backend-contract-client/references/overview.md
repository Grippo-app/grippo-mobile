# Authoritative contract evidence

Use the available Grippo backend OpenAPI/Swagger or backend route/schema source. Identify revision,
environment and the exact operation/schema inspected. Do not guess a repository path or credentials;
locate accessible source or resolve missing evidence with the user.

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
