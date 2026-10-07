# Snapshot pipeline: probe → preview → refresh → generation

Treat a snapshot as a refreshable, committed view of an external moving
contract. Never publish directly from a first fetch.

## Probe

Resolve the selected environment from `environments.json`, including
`authKind` (`bearer` or `x-api-key`, with omitted legacy values normalized to
`bearer`), and the optional credential through `authRef`. Fetch only in
`backend-action.mjs` with these limits:

- 10-second connect timeout and 60-second total timeout;
- 10 MiB response cap and allowed JSON/YAML content types;
- at most three redirects;
- exact configured host on every hop and no HTTPS downgrade;
- Local HTTP only on loopback without auth; remote environments HTTPS-only;
- validate every DNS result and pin the connection to a validated address.

Try the configured source through this strict path before discovery. After a
recognized HTML/content-type or invalid-OpenAPI failure, probe may enter the
bounded `resolving-source` phase. It can inspect same-host documentation,
Swagger configuration, and conventional OpenAPI paths through the pinned
helper. It writes only a sanitized runtime `resolution`; Apply is a separate
guarded manifest mutation and must be followed by a new Test. Refresh does not
discover or change source URLs.

For a Postman source, a valid share/API collection uid resolves to the exact
collection API URL. A non-Local `x-api-key` source with a stored `PMAK-` key may
use the one code-constant, no-redirect `api.getpostman.com` collection listing
endpoint. Bearer is never sent there, and a Postman API key is never sent to an
arbitrary enrichment host.

Validate OpenAPI 3.0/3.1 or Postman Collection v2.1, normalize in an owned
staging directory, and compare with the current snapshot. Build
`sourceFingerprint` from the canonical source descriptor, fetched document
hashes, and normalizer versions. Keep credential bytes out of it and record the
credential revision separately.

Write only a bounded runtime probe report. Include the preview id,
environment/source kind, source and snapshot fingerprints, five-minute expiry,
source summary, deterministic added/changed/removed/potentially-breaking
counters, auth state, and warnings. Do not touch committed artifacts.

## Refresh and TOCTOU fences

Accept a preview id, expected snapshot hash, and allowlisted acknowledgement
codes—never a URL or prompt. Before publication:

1. Re-fetch and normalize the source into new staging.
2. Require the new source fingerprint to equal the preview fingerprint.
3. Require the snapshot hash, environment-manifest revision, and credential
   revision to remain unchanged.
4. Acquire the shared `contract-refresh` project writer lease.
5. Recheck the environment and credential revisions under the lease.

Fail with no pointer change when any fence is stale. On OpenAPI enrichment
failure, require the explicit `refresh-base-without-enrichment`
acknowledgement; never merge new OpenAPI with silently retained old Postman
observations.

## Generation publication

Create a unique generation directory and copy required artifacts into it:

- normalized OpenAPI spec, or a sanitized descriptor for Postman-only mode;
- endpoint inventory;
- the exact complete set of area slices.

Write bounded runtime change/refresh reports, then a generation manifest with
the source fingerprint, environment id, previous/current hashes, exact artifact
roles, project-relative paths, schema versions, sizes, hashes, persistence, and
required flags. Atomically publish `current-generation.json` last.

After publication, resolve the pointer through the normal reader and verify
every required artifact. Roll the pointer back to its previous bytes if this
verification fails. Leave an abandoned non-current generation for guarded
recovery rather than claiming success.

Runtime report retention must not invalidate a generation: change/result
reports are optional runtime roles; normalized source, inventory, and areas are
required committed roles.

## Readers

When a pointer exists, fail closed if the pointer, manifest, path containment,
role set, required size/hash, or area set is invalid. Never fall back around an
invalid pointer. A missing pointer with any generation evidence is also an
error. Without a pointer and generation evidence, report no snapshot. Resolve current
artifact paths with `npm run --silent contract:paths`; never guess the
generation directory.

Use `contract:refresh-openapi` or `contract:refresh-postman` for headless
refresh. Both are strict: source content/reachability, credentials, revisions,
write conflicts, and generation failures stay fatal. The normalizer and Postman
importer are internal staging machinery only.

## Determinism and commit policy

Keep normalization and diff ordering deterministic. Commit the environment
manifest, current pointer, generation manifests, and generation artifacts.
Never commit `.secrets/`, staging, cache, or runtime
reports. Review a refresh as a normal source-control diff.
