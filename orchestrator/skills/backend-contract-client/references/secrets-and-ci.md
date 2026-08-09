# Secrets and deferred CI

## Local credential rules

- Store the opaque per-environment credential (a bearer token or a Postman
  `PMAK-` API key, selected by `authKind`) only at
  `orchestrator/api-contract/.secrets/<environment-id>.token`.
- Derive the filename from the `local|dev|stage|prod` allowlist. Require a
  regular, single-link, non-symlink file with mode `0600`, bounded to 1–16 KiB
  of UTF-8 without NUL/CR/LF. Keep the directory at mode `0700`.
- Store only non-secret `authRef: <environment-id>` or `null`, plus `authKind`
  with value `bearer` or `x-api-key`, in `environments.json`. Reject arbitrary
  profile/path names and credential kinds in schema v1.
- Never put a credential in `project-config.md`, `environments.json`, site
  state, localStorage, public API state, prompt, command line, environment
  variable, URL query/userinfo, report, or log.
- Accept a new secret through the guarded local JSON POST only. Clear the input
  immediately after submit and never return the value to the browser.
- Bump and fence the credential revision on every set/delete and on detected
  external file change. Prevent a probe/refresh using an old revision from
  publishing.
- Keep `.secrets/`, `.env`, and `.cache/` gitignored. The static
  server rejects every dot-prefixed path segment as defense in depth.

Treat Postman collections as secret-bearing input. Keep the raw collection in
owned staging and sanitize auth blocks, headers, cookies, variables, events,
and example bodies before any committed artifact or runtime report is built.

## Deferred CI

There is no generated CI workflow today. Continue to use local
`contract:verify`, `contract:doctor`, and `backend-contract-drift` gates.

When CI is added:

- validate pull requests against the committed current generation only;
- keep a scheduled live refresh in a separate credentialed job;
- store that job's credential in the CI secret store, never the repository;
- publish a refreshed generation as an ordinary reviewed diff;
- never expose a secret to forked pull requests or logs.
