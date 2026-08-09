# Backend contract overview

Use this pipeline to make client DTOs, endpoint methods, and mappers follow a
validated committed Backend contract instead of guesses. Keep it gated by
`backendContractEnabled` (`auto`, `true`, or `false`) in `project-config.md`.

## Contract authority

- Treat OpenAPI 3.0/3.1 as the primary structural source.
- Treat Postman v2.1 only as optional evidence enrichment or an explicit
  Advanced bootstrap source when no OpenAPI document exists.
- A fresh template intentionally has no Backend source manifest. Adding the
  first source in Integrations → Backend creates `environments.json` and makes
  that environment the default; do not seed an implicit Local environment.
- Treat `orchestrator/api-contract/environments.json` as the sole canonical
  source list. Each canonical environment records `authRef` and `authKind`
  (`bearer` or `x-api-key`; legacy entries without `authKind` mean `bearer`).
- Treat a valid `manifests/current-generation.json` as authoritative. Resolve
  and validate its generation manifest and required artifact hashes. Locate
  the current paths with `npm run --silent contract:paths`. A missing pointer
  means there is no snapshot; root artifacts are never read.
- Keep DTO generation forbidden. Write defensive all-nullable DTOs by hand and
  use contract findings as suggestions unless a task authorizes a fix.

## Gate semantics

- `auto`: use a valid snapshot when present. Without one, unrelated greenfield
  work may continue and drift reports `SKIPPED (no snapshot)`, but endpoint/DTO
  work is blocked until a typed refresh publishes a generation.
- `true`: require a valid snapshot; block endpoint work instead of inventing a
  missing path or field.
- `false`: skip contract consumption and launch Step 6.6.

Do not treat an `auto` skip or task-authored endpoint shape as a successful
endpoint-build validation when the task actually needs a Backend endpoint.

## Process boundary

Keep the site server and Project → API readers local-only. Let the server
validate exact requests and spawn `scripts/backend-action.mjs`; let only that
sidecar perform live network I/O. Never accept a browser-owned URL, prompt,
command, path, header, or credential for Test/Refresh.

Use these typed actions:

- `contract:probe`: read-only fetch, validate, normalize, fingerprint, and
  preview. It tries the configured source strictly first; after a recognized
  content-type or format failure it may run bounded same-host discovery and
  write a sanitized `resolution` to the runtime report. It never changes the
  manifest; the user must Apply the finding and Test again.
- `contract:refresh-openapi`: re-fetch an OpenAPI environment, prove the
  preview/source/auth/snapshot are current, then publish one generation.
- `contract:refresh-postman`: do the same for an explicit Postman-only
  bootstrap source.

Keep `contract:paths`, `contract:diff`, `contract:suggest`, `contract:verify`, and all Project →
API reads offline. Normalization and Postman import write only inside sidecar-owned staging.

## UI responsibility

Use Integrations → Backend for source configuration, active environment,
authentication status, Test source, preview, Refresh contract, and job health.
`Clear integration` is the explicit destructive recovery path: after its
confirmation it removes every Backend environment, stored credential, accepted
generation, and local Backend report so setup can restart from an empty state.
It is blocked while a probe or refresh is running and holds the project writer
lease across the reset. Empty interrupted generation directories are recovered
automatically; files or unsafe entries still fail closed as real generation
evidence.
Keep Postman settings and Copy prompt under Advanced. Use Project → API for the
endpoint browser, implementation coverage, DTO drift, breaking impact, and
follow-up tasks.

## Security summary

Store only `authRef` and `authKind` in the environment manifest. Store bearer
credentials or Postman API keys in guarded `.secrets/<environment-id>.token`
files. Bearer uses `Authorization`; `x-api-key` uses `x-api-key` and requires a
`PMAK-` value. Never expose a value through public state, DOM rehydration,
reports, logs, prompts, argv, or environment variables.

Require HTTPS for Dev/Stage/Prod; permit Local HTTP only on loopback without a
credential. Reject URL userinfo, query strings, and fragments. Recheck every
redirect hop and pin the connection to a policy-validated DNS address. Keep
discovery on the configured host except for the code-constant, no-redirect
Postman collection listing endpoint; send only a validated PMAK there and
never send bearer credentials. Never send an `x-api-key` credential to a
Postman enrichment URL.
