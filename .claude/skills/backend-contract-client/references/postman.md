# Postman enrichment and bootstrap

Prefer OpenAPI for structure. Keep optional Postman enrichment under Advanced;
the source popup may select Postman-only bootstrap automatically from a pasted
vendor URL.

## Source modes

- For enrichment, use an OpenAPI environment with
  `postmanEnrichmentUrl`. Treat OpenAPI as the structural authority and apply
  the same environment `authRef` to both URLs only for `authKind: bearer`.
  With `authKind: x-api-key`, fetch enrichment without the credential; a PMAK
  must never be forwarded to an arbitrary enrichment host.
- For a backend with no spec, explicitly create a `sourceKind: postman`
  environment. Treat this as bootstrap mode and publish only a sanitized source
  descriptor, never the raw collection.
- Reject a Postman enrichment URL on a Postman-primary environment. Require a
  future schema version for separate credentials on the two URLs.

## Smart Postman entry

Users may paste a Postman share URL, a team `*.postman.co` workspace/request
URL, a Postman collection API URL, or a direct collection document. The source
popup immediately selects `sourceKind: postman`, requires authentication, and
selects `authKind: x-api-key`; its Test action persists that typed environment,
opens the PMAK credential dialog when needed, and automatically continues the
probe. Resolved sources and collection candidates stay actionable in the same
popup; Apply updates the URL and re-tests, while an invalid or rejected PMAK
reopens the credential dialog instead of exposing a no-op Apply loop. A valid collection uid resolves to
`https://api.getpostman.com/collections/<uid>` during probe. A share URL without
a valid uid may offer a collection picker only when the environment is
non-Local, `authKind` is `x-api-key`, and the guarded credential is a `PMAK-`
key. Listing uses the single code-constant
`https://api.getpostman.com/collections` endpoint with pinned DNS, no redirects,
and at most 20 sanitized uid/title candidates. Bearer credentials are never
sent to this endpoint.

Discovery only writes a runtime `resolution`; it never mutates the environment.
The source popup is the single configuration owner and may infer Postman kind
and credential type from the pasted vendor URL before the probe. The user still
reviews a discovered collection candidate and Applies its concrete API URL
through the guarded manifest flow before refresh.

## Sanitization boundary

Fetch a Collection v2.1 document only in the typed sidecar. Keep the raw bytes
inside its owned staging directory and delete staging on completion. Before
normalization, recursively remove:

- collection/item/request auth blocks;
- headers, cookies, certificates, proxies, variables, and events;
- original requests and response headers/cookies;
- query values.

After normalization, clear generated request/response examples. Retain only
bounded enum-shaped tokens as observed enum evidence. Drop suspicious values
with an explicit warning. Never publish real production bodies, auth material,
raw collections, or staging paths.

## Enrichment behavior

Match requests by method plus normalized path. Enrich only endpoints already
declared by OpenAPI. Warn and ignore unmatched Postman requests; never extend an
OpenAPI-backed structure from a collection.

Record observed nullability and safe enum evidence as lower bounds, not as the
complete server contract. Keep declared OpenAPI enums/types authoritative.

If base OpenAPI fails, fail the probe. If optional enrichment fails after a
valid base, return an `enrichment-unavailable` warning. Require the user to
retry enrichment or acknowledge `refresh-base-without-enrichment`; do not carry
old Postman observations into a newly fetched base silently.

## Postman-only bootstrap

Build endpoints and provisional schemas from sanitized requests/responses.
Mark declared nullability unknown because no declaration exists. Re-running a
Postman-only refresh may add/remove bootstrap endpoints. When an OpenAPI source
later becomes available, switch the environment explicitly and establish a new
OpenAPI generation through Test source and Refresh contract.

Do not model pre-request scripts, cookie choreography, environment expansion,
or arbitrary custom auth. Those require a separate versioned threat model.
