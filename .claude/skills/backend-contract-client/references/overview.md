# Authoritative contract evidence

No OpenAPI location is configured in this repository: no file, Gradle property or rule names one.
The evidence source is the OpenAPI/Swagger URL or backend source path the user supplies for the
task; do not invent one (there is no documented docs path under the production host). The
read-only `backend-contract-auditor` agent (`.claude/agents/backend-contract-auditor.md`) gathers
it and reports drift; without a supplied source it reports "evidence unavailable" with the exact
fact the user must provide. Identify revision, environment and the exact operation/schema
inspected. Do not guess a repository path or credentials.

The user owns the backend. An explicit chat statement of an operation's method, path, wire keys,
types, null/omitted/empty semantics and error outcomes is an owner-attested contract: valid
evidence for the facts it states, recorded verbatim with its date in the report
([evidence trigger](../SKILL.md#evidence-trigger-canonical)). It never fills gaps by inference.

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
