# `orchestrator/api-contract/` — Backend contract sidecar

This directory owns the project-local Backend source configuration and the
contract-as-knowledge pipeline. It is plain Node 22 tooling and is never part
of the Gradle/KMP/application build.

## Authority and process boundary

- `project-config.md` owns core scalar settings and `backendContractEnabled`.
- Fresh templates intentionally have no `environments.json`, so Integrations →
  Backend starts in the unconfigured state. Adding the first source creates the
  committed manifest and makes that environment the default.
  `environments.json` is the only canonical source list.
- `orchestrator/.cache/site/.site-state.json` stores only the workspace-local
  active environment id; it never changes `defaultEnvironmentId`.
- The site server validates requests, starts an allowlisted sidecar process,
  and reads bounded local reports. It never performs a Backend fetch itself.
- `scripts/backend-action.mjs` is the live-network boundary. Its stdin accepts
  exact typed requests (`contract:probe`, `contract:refresh-openapi`, or
  `contract:refresh-postman`), never a URL, path, command, or prompt supplied
  by the browser.

OpenAPI 3.0/3.1 is the primary structural source. A Postman v2.1 collection
may enrich an OpenAPI source or explicitly bootstrap a Postman-only snapshot.
Raw collections and production examples are not committed.

## Layout

```text
environments.json                         committed non-secret source manifest, created with the first source
contract-schemas/                         source, credential-state, snapshot and generation schemas
scripts/backend-action.mjs                typed probe/refresh network sidecar
scripts/backend-cli.mjs                   headless wrapper over the same typed flow
scripts/resolve-source.mjs                 bounded OpenAPI/Postman source discovery
.secrets/<environment-id>.token           gitignored credential, mode 0600
manifests/current-generation.json          atomic current pointer
manifests/generations/<generation-id>.json committed generation manifests
manifests/generation-artifacts/<id>/       immutable inventory/areas/normalized source
orchestrator/.cache/api-contract/staging/  owned, disposable job staging
orchestrator/.cache/api-contract/reports/  bounded probe/refresh/change/analysis/history reports
orchestrator/.cache/api-contract/mock/     bounded loopback-mock state, index, fixtures and request logs
```

The pointer is authoritative when present. Readers validate its exact shape,
manifest hash, path containment, unique roles, sizes, and all required artifact
hashes. Run `npm run --silent contract:paths` before reading a snapshot; it
returns the validated current inventory, areas directory, and normalized spec
as project-relative paths. Without a pointer, readers report no snapshot; they
never inspect root-level artifacts.

## Sources and credentials

Environment ids are limited to `local`, `dev`, `stage`, and `prod`.
Non-Local sources require HTTPS. Local HTTP is limited to loopback and cannot
use a credential. Source URLs reject userinfo, query strings, and
fragments. Redirects keep the exact configured host, prohibit HTTPS downgrade,
and are rechecked for scheme and IP policy at each hop. The sidecar pins the
connection to the DNS address it validated.

A source may be a direct OpenAPI 3.0/3.1 document, a Postman v2.1 collection,
or a documentation/share page. Probe always tries the configured URL through
the strict source path first. Only after a recognized content-type or format
failure may it run bounded discovery: same-host pages, Swagger configuration,
and conventional OpenAPI paths are checked through the same pinned network
policy. Discovery writes a sanitized `resolution` to the runtime probe report;
it never changes `environments.json`. The source popup owns configuration: its
`Test source` action saves the typed environment, runs the probe without
closing the popup, and shows the terminal result there. A Postman URL selects
Postman plus `x-api-key` authentication immediately. Other URLs are tried
without a secret first; a 401/403 response enables bearer authentication and
opens the credential dialog before the automatic retry. The user must still
review and Apply a resolved URL or collection candidate before refresh.

Postman share and collection URLs on `postman.com`, `www.postman.com`, or a
team `*.postman.co` host are classified as Postman immediately and normalized
to the Postman collection API when they contain a valid collection uid. For an
authenticated non-Local Postman source without a uid, discovery may list
collections from the one code-constant endpoint
`https://api.getpostman.com/collections`. This carve-out requires
`authKind: "x-api-key"`, a stored `PMAK-` key, pinned DNS validation, and no
redirects; bearer credentials are never sent to that host.

Credentials live only in `.secrets/<environment-id>.token`. Files must be
regular, single-link, non-symlink files with mode `0600`; values are bounded
UTF-8 without NUL/CR/LF. The browser can set, replace, or delete a credential,
but no API ever returns the value. Each environment stores `authRef` plus
`authKind` (`bearer` or `x-api-key`; omitted legacy values mean `bearer`). A
bearer credential uses the `Authorization` header. An `x-api-key` credential
uses the `x-api-key` header and must start with `PMAK-`; it is never forwarded
to `postmanEnrichmentUrl`.

## Probe, preview, and refresh

`Test source` runs a read-only probe in an owned staging directory. It fetches,
validates, normalizes, fingerprints, and compares the source, then writes only
a runtime probe report. If strict source recognition fails, the same probe may
enter the bounded `resolving-source` phase and return only a resolution for the
user to Apply; refresh never mutates or guesses a source URL. A preview expires
after five minutes and is fenced by environment, source-manifest, credential,
and snapshot revisions.

`Refresh contract` accepts the preview id, re-fetches the documents, compares
the fingerprint again, acquires the project writer lease, rechecks every
revision, and publishes a coherent generation. Required artifacts are copied
first, the generation manifest next, and `current-generation.json` last. A
failed or interrupted refresh leaves the previous pointer active and removes
the unpublished generation it owned. A pointer-less directory containing no
files is recovered as empty interrupted residue; any file, unknown entry, or
unsafe inode remains fail-closed generation evidence.

The visible `Clear integration` action is the deliberate full reset. Its
confirmation removes all Backend environments, local credentials, accepted
generations, and Backend runtime reports. It is revision-fenced, unavailable
while Test or Refresh is running, refuses a foreign project writer before the
first deletion, and returns the UI to first-source setup.

Every successful publication also produces a versioned semantic change set.
Rules are directional: request compatibility is evaluated as the accepted
input domain, while response compatibility is evaluated as the produced output
domain. Stable change ids are derived from the prior/current contract hashes
and semantic evidence. The current change set is part of the generation;
bounded history retains prior sets for task provenance. Project → API review
acknowledgements live in a separate bounded runtime store, so marking a change
reviewed never rewrites classifier evidence or history.

After publication, the trusted local analyzer scans a canonical, VCS-neutral
receipt of supported project source/config files. It writes generation-bound
`implementation-map.json` and `consumer-map.json` reports containing the exact
`committedGenerationId`, contract hash, environment, project-code revision, and
analyzer version. Symlinks, hard links, path collisions, races, and file/count/
byte cap violations fail closed. Test sources are excluded from implementation
evidence. Implementation and consumer discovery are deliberately marked
partial: static route/operation references and Architecture links are useful
evidence, but absence is never presented as proof that an implementation or
consumer does not exist.

Postman data is sanitized before normalization: auth blocks, headers, cookies,
variables, events, and raw source are removed; generated examples are cleared,
and only enum-shaped evidence may survive.

## Commands

After root `npm ci`, run these from this directory:

```sh
npm run contract:doctor
npm run contract:probe
npm run contract:refresh-openapi
npm run contract:refresh-postman
npm run --silent contract:paths
npm run contract:verify
npm run contract:analyze
npm run contract:diff
npm run contract:suggest
```

The refresh commands use `defaultEnvironmentId`; the site uses its active
workspace selection. `normalize.mjs` and `import-postman.mjs` are internal
staging transformers invoked only by the typed refresh flow. There are no
direct URL/file writers and no root snapshot output.

Typed refresh commands are strict. Unreachable sources, invalid schemas,
rejected credentials, configuration/revision errors, write conflicts, and
invalid generations remain non-zero failures and never change the current generation.

## Project → API runtime

The Site exposes generation-bound read models at `/api/api/*`: overview,
endpoint catalog/detail, model detail, semantic changes, diagnostics, and
server-resolved batch task preview/create. A guarded
`POST /api/api/changes/review` records immutable-diff acknowledgements. List
endpoints use opaque cursors,
hard row limits, and response-size caps. The browser submits only current
source ids; task titles, bodies, fingerprints, provenance, and deduplication
are resolved again on the server immediately before publication.

Endpoint details prefer explicit OpenAPI examples after bounded secret
redaction, otherwise they clearly label deterministic generated values.
Diagnostics can start one optional local mock instance for the project. The
worker binds only `127.0.0.1`, receives fixed argv fields, never proxies or
executes scripts, and records only bounded method/path/status/duration rows—no
headers or bodies. Each instance remains pinned to its original generation,
contract hash, and environment until explicitly stopped.

`backendContractEnabled` remains the tri-state consumer gate: `auto` uses a
valid snapshot when present, `true` requires one, and `false` disables contract
consumption. Under `auto`, snapshot absence is non-blocking only for unrelated
greenfield work; endpoint/DTO work waits for a typed refresh. The gate never
weakens source, credential, or generation validation and task text is not a
contract substitute.
