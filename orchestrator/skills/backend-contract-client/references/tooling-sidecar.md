# Contract tooling sidecar

Keep all tooling under `orchestrator/api-contract/`. It is plain Node 22 code,
never part of Gradle or the shipped application.

## Owned paths

| Path | Persistence | Purpose |
|---|---|---|
| `environments.json` | committed, project-specific; absent in a fresh template | created by the first Backend source, then canonical for Local/Dev/Stage/Prod sources |
| `contract-schemas/` | committed | inventory, area, environment, credential-state, generation and pointer schemas |
| `scripts/backend-action.mjs` | committed | exact typed network sidecar |
| `scripts/backend-cli.mjs` | committed | headless wrapper over probe/refresh |
| `scripts/resolve-source.mjs` | committed | pure parsing/classification plus injected bounded discovery orchestration |
| `.secrets/<id>.token` | gitignored | guarded bearer token or Postman API key |
| `manifests/current-generation.json` | committed | atomic current pointer |
| `manifests/generations/*.json` | committed | immutable generation manifests |
| `manifests/generation-artifacts/<id>/` | committed | normalized spec/descriptor, inventory and areas |
| `.cache/api-contract/staging/` | gitignored runtime | owned candidate generations |
| `.cache/api-contract/reports/` | gitignored runtime | bounded probe/refresh/change/history reports |

Root spec/inventory/area artifacts are not part of the contract. Readers use
only the validated current generation pointer and report no snapshot when it is
absent.

Never rename `contract-schemas/` to `schemas/`; generated projects ignore
`**/schemas/` for Room exports.

## Commands

Run `npm ci` once from the project root. The root workspace lock owns Ajv for
JSON Schema validation and YAML for bounded OpenAPI YAML parsing.

| Command | Network | Purpose |
|---|---|---|
| `contract:doctor` | no | validate config, schemas, credentials metadata and current generation |
| `contract:probe` | typed sidecar | test `defaultEnvironmentId`; commit nothing |
| `contract:refresh-openapi` | typed sidecar | probe then publish an OpenAPI generation |
| `contract:refresh-postman` | typed sidecar | explicit Postman-only bootstrap |
| `contract:paths` | no | resolve validated current artifact paths without guessing a generation id |
| `contract:verify` | no | validate the generation-aware current snapshot |
| `contract:diff` | no | DTO/API drift report |
| `contract:suggest` | no | optional endpoint coverage plan |

Do not add a token flag. Do not add URL/file overrides to the typed commands.
Use `--environment local|dev|stage|prod` only to choose an entry already present
in the manifest. Every probe and refresh is strict; source, credential,
revision, write-conflict, and generation failures stay fatal.

Probe is also the only smart-entry action. It attempts the configured source
strictly first. After a recognized source content/format failure it may run
bounded discovery and put a sanitized `resolution` in the runtime report. The
sidecar never applies that result; Integrations → Backend performs the guarded
manifest update and starts a new Test. Refresh never runs discovery.

Environment manifests store `authRef` and `authKind`. `bearer` selects the
`Authorization` header; `x-api-key` selects `x-api-key` and requires a `PMAK-`
value. Discovery stays on the configured host except for the one code-constant,
no-redirect `api.getpostman.com` collection listing endpoint, which is enabled
only for an authenticated non-Local Postman source. Bearer is never sent there,
and Postman API keys are not sent to enrichment URLs.

## Publication contract

Stage every candidate outside committed paths. Re-fetch on refresh, compare the
source fingerprint from probe, verify environment/auth/snapshot revisions,
acquire the shared project writer lease, and recheck the fences immediately
before publication. Copy required artifacts, write the generation manifest,
and publish `current-generation.json` last. Keep the previous pointer active on
any failure.

Validate pointer exact keys, manifest hash, role uniqueness, project-relative
path containment, required flags, sizes, and required artifact hashes on every
read. Runtime reports may disappear after retention without invalidating a
committed snapshot.

## Postman boundary

Keep the raw collection in owned staging only. Remove auth, headers, cookies,
variables, events, certificates, proxies, and original requests before
normalization. Clear generated examples; retain only bounded enum-shaped
evidence. Drop doubtful data with a warning.

## Ignore/static rules

Ignore `.secrets/`, local `node_modules/`, `.env`, and the
root `.cache/`. Exclude `environments.json` and `.secrets/` from template
syncing because both are product-specific. Rely on the static server's
dot-segment denial as defense in depth; never expose secrets through an API.
