import { dom } from './dom.js';
import { i18n } from './i18n.js';
import { store } from './store.js';
import { clipboard } from './clipboard.js';
import { runControl } from './run-control.js';
import { tasksApi } from './data/tasks-api.js';

// ----------------------------------------------------------------------
// Figma PULL + DRIFT actions — the typed browser/server contract for everything
// that reads design data from Figma. The Figma integration panel owns binding
// and sync controls; the Design workbench consumes only committed generation
// artifacts. Golden invariant: every live action spawns a `figma:*` Claude
// session; the site server never calls Figma directly.
// ----------------------------------------------------------------------

var el = dom.el;
function t(key, params) { return (i18n && typeof i18n.t === 'function') ? i18n.t(key, params) : key; }

// Exact browser -> server action contract. Run requests send one of these ids;
// the server imports this module and derives the prompt itself, so an
// allowlisted session key can never be paired with arbitrary client text.
export const FIGMA_SESSION_ACTION = Object.freeze({
  WHOAMI: 'whoami',
  FILE_ACCESS: 'file-access',
  SYNC_TOKENS: 'sync-tokens',
  SYNC_COMPONENTS: 'sync-components',
  SCREEN_PULL: 'screen-pull',
  SCREEN_DRIFT: 'screen-drift',
  REBUNDLE: 'rebundle',
  SHIP_DRIFT_SWEEP: 'ship-drift-sweep'
});

// --- Figma URL / key parsing (single home — figma.js imports these) ----
export function parseFileKey(input) {
  var s = String(input || '').trim();
  if (!s) return '';
  if (/^[A-Za-z0-9]{8,}$/.test(s)) return s;
  try {
    var u = new URL(s);
    if (u.protocol !== 'https:' || !/^(www\.)?figma\.com$/i.test(u.hostname)) return '';
    var m = /^\/(?:design|file)\/([A-Za-z0-9]+)(?:\/[^?#]*)?$/.exec(u.pathname);
    return m ? m[1] : '';
  } catch (e) {}
  return '';
}
export function fileCandidateDisplayName(candidate) {
  return candidate ? candidate.fileName || candidate.maskedKey || '' : '';
}
// --- state readers ------------------------------------------------------
function figmaObj()   { return store.get().figma || {}; }
function figmaState() { return figmaObj().state || 'unknown'; }
function figmaAccount(){ return figmaObj().account || null; }
function connectorConflict() {
  var g = figmaObj().global || {};
  return !!g.present;
}
function connected() { return figmaState() === 'connected' && !connectorConflict() && !!figmaAccount(); }
export function figmaConnected() { return connected(); }
// BUSY — a turn in flight or paused on a question. A warm-but-idle session
// (turn finished, process winding down) must not keep "export running" lines
// or input locks alive after the terminal already shows the turn done.
function sessionBusy(sess) {
  return !!(sess && sess.running && (sess.awaitingTurn || sess.askedThisTurn));
}
function sessionRunning(keys) {
  var s = store.get().sessions || {};
  return keys.some(function (k) { return sessionBusy(s[k]); });
}
function ago(ms) {
  var d = Date.now() - ms; if (!(d >= 0)) return '';
  var m = Math.floor(d / 60000);
  if (m < 1) return t('figma.ago.now');
  if (m < 60) return t('figma.ago.min', { n: m });
  var h = Math.floor(m / 60); if (h < 24) return t('figma.ago.hour', { n: h });
  return t('figma.ago.day', { n: Math.floor(h / 24) });
}

// --- session prompts (English by invariant — agent-facing) --------------
function whoamiPrompt(nonce) {
  nonce = String(nonce || '');
  return [
    'Identify which Figma account this project\'s Figma MCP connector is authenticated as, and record it.',
    '',
    '1. Call the Figma MCP `whoami` tool on the connected Figma MCP server. It returns the authenticated user\'s handle, email, and plan/seat.',
    '2. Write the result to orchestrator/figma/.account.json under orchestrator/figma/ (create the dir if needed), exactly this shape:',
    '   { "handle": "<handle>", "email": "<email>", "tier": "<plan tier or empty>", "seat": "<seat or empty>", "checkedAt": "<ISO 8601 timestamp>", "verificationNonce": "' + nonce + '" }',
    '   Copy verificationNonce exactly as shown. It binds this result to the current connector episode; do not invent or reuse another value.',
    '3. Print one line: "Figma account: <handle> <email>".',
    '',
    'Strict scope: write ONLY orchestrator/figma/.account.json. Do not touch any other file, anything outside orchestrator/figma/, or any Kotlin/Swift/Gradle files. Never print tokens or secrets.'
  ].join('\n');
}

function fileAccessPrompt(context) {
  var key = parseFileKey(context.figmaFileKey);
  return [
    'Verify access to one exact Figma project file using the connected Figma MCP. This is a typed integration probe, not a design export.',
    '',
    'File key: ' + key,
    'Verification nonce: ' + context.accessNonce,
    'Account fingerprint: ' + context.accountFingerprint,
    '',
    '1. Call Figma MCP get_metadata for the file. Read only enough metadata to confirm the file exists and this account can access it.',
    '2. Write exactly one JSON receipt to ' + context.receiptPath + ' with this exact shape:',
    '   {"schemaVersion":1,"verificationNonce":"' + context.accessNonce + '","fileKey":"' + key + '","accountFingerprint":"' + context.accountFingerprint + '","state":"verified|denied|not-found|quota-blocked","fileName":"<name or empty>","checkedAt":"<UTC YYYY-MM-DDTHH:mm:ssZ or YYYY-MM-DDTHH:mm:ss.sssZ>","reasonCode":"<access-denied|file-not-found|quota-risk or empty>"}',
    '3. Use state verified only after a successful metadata response for this exact key. fileName is optional display metadata, not access evidence: use a file-level name only when the metadata tool explicitly returns one; otherwise write an empty string. Never infer it from a page/frame name or guess an access result.',
    '4. Use only the connected Figma MCP. Do not use a Figma REST API or token fallback.',
    '',
    'Strict scope: write ONLY the receipt path above. Do not export design data, edit project config, write prompts/transcripts, or touch product source. Never print tokens or secrets.'
  ].join('\n');
}

function syncGroupPrompt(group, context) {
  var action = {
    tokens: [
      'Refresh ONLY the exact registered observation sources in the server-owned plan. This provider capability is usage-scoped `node-bound-resolved-variables`; it is not a file-wide Variables census and exposes no stable variable IDs, collections, modes, or alias graph.',
      'Read ' + context.capturePlanPath + ' first, then every listed capture-plan/<bucket>.json. Do not add, omit, rename, or re-bucket sources. For each record call Figma MCP get_variable_defs once for its exact source.nodeId/context. A failed, truncated, rate-limited, permission-degraded, or cancelled source makes the whole refresh fail; never carry old values into a new capture.',
      'Write exactly the listed capture-intake/<bucket>.json files. Each must satisfy observed-token-capture-shard.schema.json. Copy server-issued source identity, captureOperationId, captureSequence, accountFingerprint, connectorRevision, and semanticPreflightHash verbatim. Each capture must satisfy observed-token-source-capture.schema.json:',
      '- observations[] contains only exact returned providerName/rawValue pairs. Add providerType only when the response explicitly returned COLOR/FLOAT/STRING/BOOLEAN/UNKNOWN. Do not infer a type, unit, mode, collection, ID, alias, default, or remote/library origin.',
      '- witness records exact startedAt/finishedAt, nodeId, Figma MCP operation get_variable_defs, sourceCompleteness, providerEnumerationCompleteness `not-available-for-provider`, providerTruncationSignal, truncated, issues, observationCount, accountFingerprint, connectorRevision, optional real providerRevision, and producerVersion.',
      '- captureBytesHash is sha256 of the UTF-8 canonical JSON bytes of the nested capture object using orchestrator/figma/runtime/canonical-json.mjs; it is evidence only. The server independently verifies every hash, plan field, bucket, schema, and semantic invariant before normalization.',
      'An exact successful empty response uses observations:[] and complete-returned-payload. It replaces only that source contribution; it never claims that a Variable was deleted from the Figma file.',
      'Do not write artifacts.json or anything under publication/. The trusted server normalizer alone produces source shards, source index, and observed catalog.'
    ],
    components: [
      'Capture the file\'s COMPONENT SETS and standalone main components plus their bounded resolved-variable observations as one composite operation. Read ' + context.tokenCapturePlanPath + ' before the first provider call. Write ONLY <staging>/capture.json, screenshot PNGs under <staging>/visual/, and every one of the 128 listed component-token-intake/<bucket>.json files (empty buckets use captures:[]). The server normalizes both domains and publishes them atomically. Do NOT write artifacts.json, any normalized/derived file, or anything else.',
      'Census protocol (completeness is the whole contract): read the file version via the Figma MCP get_metadata BEFORE the first entity read; enumerate EVERY page and, per page, every component set and standalone main component (get_metadata); then per entity call the Figma MCP get_design_context for the exact node (on an oversized/truncated response call get_metadata for its children and repeat get_design_context per child); read the file version again AFTER the last read. capture.json must satisfy orchestrator/figma/schemas/design-component-capture.schema.json exactly:',
      '- providerIdentity: { "fileKeyFingerprint": "sha256:<lowercase hex sha256 of the file key>", "branchKey": "<branch id or the literal none>", "libraryOriginPolicy": "local-authoritative" }.',
      '- scope: { "kind": "all-pages" }. pages[]: every page as { pageId, name }.',
      '- entities[]: every component set / standalone main component with { nodeId, pageId, kind ("component-set"|"component"), name, description?, idQuality ("stable" for provider node ids), remote?, published?, hidden?, properties[], variants[], expectedVariantCount, defaultVariantNodeId?, nestedRefs[], boundVariables[], textLayers?[], autoLayout? }.',
      '  properties[]: every component property as { propertyId (provider id when exposed, else the property name with idQuality "name-derived"), name, type ("variant"|"boolean"|"text"|"instance-swap"; anything else stays type "unknown" with providerType evidence — NEVER dropped), idQuality, options (variant only, exact provider order), defaultValue (only when the provider states one), swapTargets (instance-swap only) }.',
      '  variants[]: for a component-set, every REAL child variant as { nodeId, name, assignments: { "<variant propertyId>": "<value>" }, isDefault? }. Record only combinations that actually exist — NEVER expand a Cartesian product, never invent a combination. expectedVariantCount is the provider-stated child count (-1 when it states none). If a child could not be fully read, keep the entity with captureIssue "<why>" instead of guessing.',
      '  nestedRefs[]: nested main-component usages as { targetNodeId and/or targetComponentKey, viaPropertyId (for instance-swap exposure), layerName?, swappable, remote? }. boundVariables[] contains only exact refs covered by the token intake: { observedTokenKey, contextKey, sourceId, providerName, field, layerName?, remote? }. textLayers[]: text layers as { nodeId, name, boundTextPropertyId? }.',
      'Token observation protocol: choose at most 128 page/component capture roots covering every boundVariables ref (page roots are preferred to per-entity calls). For each exact root/context call Figma MCP get_variable_defs once. Each source is kind "component", uses plan scope/account/connector verbatim, and origin { kind:"component-inventory", componentScopeId:<plan.componentSourceScopeId>, captureRootNodeId:<same exact node id as source.nodeId> }. Compute sourceId and bucket with the canonical token identity contract. A source already listed in plan.knownSources must use its exact reserved operation/sequence. Sort all remaining new sourceIds lexicographically and assign the same-position unique entry from plan.newSourceReservations; copy both captureOperationId and captureSequence verbatim. Never reuse one reservation for two sources. Every shard/capture/record must satisfy observed-token-capture-shard.schema.json and observed-token-source-capture.schema.json, including canonical captureBytesHash and semanticPreflightHash. Failed, truncated, permission-degraded, rate-limited or incomplete variable reads make the whole composite capture fail.',
      '- visual[]: screenshots by the bounded evidence policy — for every component the DEFAULT variant (or the standalone component itself) via the Figma MCP get_screenshot, written under <staging>/visual/<safe-name>.png; each entry { entityNodeId, variantNodeId ("null" for a standalone/whole-set shot), role ("default"), file ("visual/<name>.png"), sha256 ("sha256:<hex of the PNG bytes>") }. Do not screenshot every combination; missing visual evidence is honest, a placeholder PNG is a contract violation.',
      '- witness: { startedAt/finishedAt (UTC ms ISO), providerRevisionBefore/After (from the two Figma MCP get_metadata version reads — omit both only if the provider exposes none), consistency ("proven" only when both were read and identical, else "unproven"), completeness ("complete" only when every page was fully enumerated and every entity captured or explicitly carries captureIssue), requestedPageIds/readPageIds (exact page id lists), expectedEntityCount (the provider-stated total, or -1), readEntityCount, truncated, permissionDegraded, limitsHit []. If ANY read failed, was rate-limited, or was cut short: set completeness "incomplete" and the honest flags — never trim the witness to make it look complete.',
      'The server refuses to publish an incomplete/inconsistent component capture, any uncovered boundVariables ref, or any incomplete token source; reading a few known nodes is NOT a census. Failure of either domain publishes neither domain. Do not run any script, do not normalize, do not touch Kotlin/Swift/Gradle or canonical orchestrator/figma files.'
    ]
  }[group];
  var header = [
    'Execute the typed Figma sync scope "' + group + '" into a server-owned staging directory. Do not publish project artifacts directly.',
    '',
    'Figma file key: ' + parseFileKey(context.figmaFileKey),
    'Job id: ' + context.jobId,
    'Input fingerprint: ' + context.inputFingerprint,
    'File key fingerprint: ' + context.fileKeyFingerprint,
    'Staging group directory: ' + context.stagePath,
    ''
  ];
  if (group === 'tokens') {
    return header.concat([
      action.join('\n'),
      'Use get_metadata/get_design_context/get_variable_defs only as needed. Follow Figma MCP rate-limit errors; never fabricate missing data.',
      '',
      'Strict scope: read <staging>/capture-plan.json and <staging>/capture-plan/*.json; write ONLY the exact <staging>/capture-intake/*.json paths listed by that plan. Do not write artifacts.json or publication files, do not edit canonical orchestrator/figma artifacts, the current-generation pointer, project config, tasks, or product source. Never print tokens or secrets.'
    ]).join('\n');
  }
  return header.concat([
    action.join('\n'),
    'Use get_metadata/get_design_context/get_variable_defs/get_screenshot only as needed. Follow Figma MCP rate-limit errors; never fabricate missing data.',
    '',
    'Strict scope: write ONLY <staging>/capture.json and <staging>/visual/*.png below the staging group directory. Do not write artifacts.json (the server owns it for this scope), do not edit canonical orchestrator/figma artifacts, the current-generation pointer, project config, tasks, or product source. Never print tokens or secrets.'
  ]).join('\n');
}

// Pull the per-screen design cache for a task (the spec-gate's input). THIS
// session calls the MCP and writes the curated per-screen cache; builders and
// validators only read the files. The session reads the task file itself, so
// the prompt needs only the stem — the board button works from the backlog,
// pending AND todo columns. This prompt IS the screen-cache authoring contract
// (no markdown mirror exists); its output shape is what check-screen-cache.mjs
// and the implement-figma skill (orchestrator/figma/skill/SKILL.md) consume.
export function screensPrompt(stem, context = {}) {
  return [
    'Pull the per-screen Figma design cache for task ' + stem + ', so the Figma-blind builders and the figma-spec-validator can consume it. THIS session calls the MCP; builders and validators never do.',
    '',
    'Server-issued token capture plan: ' + context.screenTokenPlanPath,
    '',
    'Do this:',
    '1. Read the task file for ' + stem + ' (orchestrator/tasks/todo/' + stem + '.md, or orchestrator/tasks/backlog/' + stem + '.md plus the answered orchestrator/tasks/pending/' + stem + '.questions.md during prep) and extract every "- <ScreenName> — <value>" bullet from the ## Design section. Skip bullets whose value begins with `none` (the explicit `none (<reason>)` escape — a screen with no mock). The value can be: (a) a plain Figma URL — single theme; (b) `light:<url> dark:<url>` — both themes; (c) `dark:<url>` — dark only. Parse accordingly. Read the immutable token capture plan above before any MCP call. Its records are the exact allowed node/context/sidecar set; do not add, omit, rename, or rebind a record.',
    '1b. Freshness: if orchestrator/.cache/figma/screens/' + stem + '/index.json already exists, treat any screen whose fetchedAt (or darkFetchedAt) is older than 14 days as STALE — re-pull it from Figma; never skip a stale screen as "already cached". Every node you pull (or re-pull) gets a fresh fetchedAt/darkFetchedAt in index.json — step 4 mandates those fields for every node.',
    '2. For each screen node, read it via the Figma MCP: get_metadata (node tree + component instances), get_design_context (resolved fills/strokes/sizes/text styles), get_variable_defs (bound variables), and get_screenshot.',
    '2b. Fail-closed on MCP failure: if ANY Figma MCP read fails — needs-auth / authentication error, rate limit, node not found, or node moved — finish the screens you CAN still read, then STOP. Print a per-screen FAILED table with the exact error for each failed screen. NEVER create placeholder PNGs, spec/instances/context files, or index.json entries for a node you did not actually read — a fabricated file becomes the design oracle later gates certify implementations against. If a screen was partially written when the failure hit, DELETE its partial files before stopping, so the cache only ever holds fully-read screens. Before stopping, record the failure in the task journal so Task Details → Activity keeps the diagnosis (best-effort, same log-event.py mechanism as the design-pulled event in step 6a — ignore any failure): python3 orchestrator/tasks/log-event.py ' + stem + ' design-pull-failed --phase design-pull --status fail --meta screens=<comma-separated ScreenNames that FAILED> || true. End your report with the exact remediation per error class: needs-auth → the OWNER reconnects Figma from the site\'s Figma tab (its reconnect/re-auth action re-binds the local MCP server — that is the owner-facing lever); an agent session can equivalently re-authenticate via /mcp → reauthenticate. Then re-run this pull; rate limit → wait for the limit window to reset, then re-run this pull (stale/missing screens are re-pulled, fresh ones are not); node not found/moved → fix that screen\'s URL in the task\'s ## Design section, then re-run this pull.',
    '3. Write, per screen, under orchestrator/.cache/figma/screens/' + stem + '/:',
    '   - <ScreenName>.png — screenshot for a plain URL or light: URL. When dark: URL was provided, write <ScreenName>.dark.png. For dark: only, write the .dark.png file and do NOT invent a primary .png.',
    '   - <ScreenName>.spec.json — the plain/primary (or light:) spec — a CURATED value spec (NOT a raw node dump): { "screen": "<ScreenName>", "frameSizeDp": {"w":n,"h":n}, "theme": "dark|light", "elements": [ { "stableId": "<stable semantic id, e.g. home-title-label>", "figmaNodeId": "<node id when available>", "nodeUrl": "<deep link>", "name": "<role/label>", "text": "<visible text content of the node, VERBATIM - required for every text/glyph node; null/omit for non-text>", "bboxDp": {"x":n,"y":n,"w":n,"h":n}, "fills": ["#RRGGBB" | "{group.token}" | { "tokenRef": "{group.token}", "resolvedValue": "#RRGGBB" }], "textStyle": { "sizeSp": n, "weight": n, "lineHeightSp": n, "case": "<none|as-is|upper|lower|title>", "fontFamily": "<design font family VERBATIM when the design context provides it; OMIT the key when unknown — never guess>" } | null, "cornerRadiusDp": n | null, "strokes": [ { "color": "#RRGGBB | {group.token}", "widthDp": n } ], "gapsToSiblingsDp": { "<stableSiblingId>": n }, "paddingDp": {"l":n,"t":n,"r":n,"b":n} | null, "componentSetName": "<owning component set name when this element is a component instance>", "variantProps": {"Axis":"Value"} } ] }. Set "theme" to the actual node theme you read (a plain URL can be light or dark; do not force it to light). Every element MUST carry stableId or figmaNodeId; stableId should survive label changes and duplicate visible names. Component instance elements MUST repeat componentSetName/variantProps here AND carry componentSetNodeId (the owning component-set node id — the durable identity join; two same-named sets are distinguishable only through it), not only in .instances.json, so compare-screen-spec can verify variants. Values are concrete numbers/hex or a {group.token} bound-variable ref (resolvedValue preferred when available). Do NOT invent values you cannot see — omit unknown optional fields. Text nodes MUST carry their visible content verbatim in "text" (the numbers/strings the design actually shows): the mechanical content-parity check (check-stub-text) searches the code for exactly these strings, so a paraphrase here becomes a false alarm and an omission becomes a blind spot. When dark: URL was provided, write <ScreenName>.dark.spec.json (same curation discipline, same shape, with "theme": "dark"). For dark: only, write only the .dark.spec.json file.',
    '   - <ScreenName>.instances.json — a flat component-instances list: [ { "name": "...", "componentSetName": "...", "figmaNodeId": "<the mandatory OWNING component-SET id — resolve the instance\'s main component to its set; never write the instance\'s OWN node id>", "nodeUrl": "<a deep link to the instance\'s own node — always include this so a MISSING component has a node to cite>", "variantProps": {...}, "bboxDp": {"x":n,"y":n,"w":n,"h":n} } ]. If the owning set id cannot be resolved, mark that screen FAILED and do not write its cache artifacts; names are labels, not identity.',
    '   - <ScreenName>.context.json — the raw-ish design-context capture (reference/debug).',
    '   - <ScreenName>[.<variant>].tokens.json — the exact Figma MCP get_variable_defs result as observed-token-source-capture.schema.json. Use the exact tokensFile in the server plan and copy source/captureOperationId/captureSequence/accountFingerprint/connectorRevision verbatim; write providerName/rawValue exactly and providerType only when returned. For an untagged primary URL the planned context theme is explicitly `unknown`; do not relabel it light/dark after looking at pixels. This is usage-scoped resolved-value evidence, never a Variables census. A failed/truncated read means the variant is FAILED and no cache/index is certified.',
    '4. Write orchestrator/.cache/figma/screens/' + stem + '/index.json using the exact schemaVersion 3 contract: { "schemaVersion": 3, "taskStem": "' + stem + '", "nodes": { "<ScreenName>": { "kind": "screen|dialog|component|overlay", "url": "...", "nodeId": "...", "fetchedAt": "<ISO8601>", "variants": [ { "id": "<exact server-plan variantId>", "theme": "<exact server-plan context.theme>", "locale": "default", "platform": "shared", "url": "...", "nodeId": "...", "fetchedAt": "<ISO8601>", "imageFile": "<ScreenName>.png", "specFile": "<ScreenName>.spec.json", "instancesFile": "<ScreenName>.instances.json", "tokensFile": "<exact server-plan tokensFile>", "tokensHash": "sha256:<exact sidecar bytes>", "captureOperationId": "tokop_<server-issued>", "captureSequence": 1 } ] } } }. Preserve any [screen|dialog|component|overlay] marker from the ## Design bullet; default only an unmarked bullet to "screen". Every node MUST contain a non-empty typed variants array with one entry for every real captured theme/locale/platform combination, plus the matching primary or dark summary fields required by the schema. Copy variant id/theme/locale/platform from the exact plan record; a primary URL therefore remains `unknown-default-shared`/`unknown`. Never emit an entry or comparison for a file that was not actually captured. For a dark-only screen the node carries darkUrl/darkNodeId/darkFetchedAt plus its real dark variant (no primary url/nodeId and no invented light variant).',
    '4b. Atomic writes: write EVERY cache file (steps 3-4, token sidecars and PNGs included) atomically — write to <name>.tmp in the same directory, then rename it over the final name; never write or edit a final filename in place. Write index.json LAST, only after all per-screen files of every screen are in place: the site poller and the gates read this cache concurrently and must never see a half-written screen set.',
    '4c. MANDATORY — normalize device chrome at the pull boundary: node orchestrator/figma/scripts/normalize-oracle.mjs ' + stem,
    '    It strips embedded iOS device chrome (the "9:41" status bar / home indicator) from each oracle PNG+spec pair via a strict deterministic predicate, stamps the spec with an auditable chromeCrop record, and is idempotent (a second run is a byte no-op). A geometry-lookalike strip WITHOUT the name/"9:41" signal is NOT cropped — it prints a warn-grade IOS_CHROME_SUSPECTED naming the element; include any such warning in your final report so the owner decides (rename the Figma layer, or accept the pixels). Never crop anything yourself and never skip this step — the check-screen-cache gate (5a) verifies a stamped spec stayed consistent (CHROME_CROP_* blockers).',
    '4d. Seed the task bindings manifest (the ONE declared binding artifact downstream identity joins read): write orchestrator/.cache/figma/screens/' + stem + '/bindings.json atomically — { "schemaVersion": 2, "stem": "' + stem + '", "screens": [ one entry per index.json node: { "nodeId": "<node.variants[0].nodeId>", "screenName": "<ScreenName>", "kind": "<node.kind>", "captureBasename": "<ScreenName>Screenshot.png" } ], "components": [] }. Do NOT invent implFile/composable and NEVER author components[] rows yourself — component-census derives them from the mapping registry (they are keyed by designComponentId, not by display name). A present malformed bindings file is a hard error; omitted optional fields simply remain unset until their owning step fills them. If the file already exists, MERGE per screenName (preserve any implFile/composable/components already filled) — never wholesale-overwrite a builder\'s bindings.',
    '5. Create/reuse one task run id and use it for every machine report in this pull: FIGMA_PIPELINE_RUN_ID="${FIGMA_PIPELINE_RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)-' + stem + '}"',
    '5a. Run: FIGMA_PIPELINE_RUN_ID="$FIGMA_PIPELINE_RUN_ID" node orchestrator/figma/scripts/check-screen-cache.mjs ' + stem + ' --gate',
    '   This validates the task ## Design bullets against index.json and the required per-screen artifacts. If it fails, fix the cache files and re-run before continuing. "Fix" means re-read from Figma or correct your curation — NEVER fabricate an artifact you could not fetch (rule 2b).',
    '5b. Run: FIGMA_PIPELINE_RUN_ID="$FIGMA_PIPELINE_RUN_ID" node orchestrator/figma/scripts/component-census.mjs ' + stem,
    '   It resolves instances through the Design Component Inventory and Component Mapping Registry, then prints a per-component MAPPED / MISSING / INCOMPLETE / AMBIGUOUS table (machine copy under orchestrator/.cache/figma/reports/).',
    '5c. Run: FIGMA_PIPELINE_RUN_ID="$FIGMA_PIPELINE_RUN_ID" node orchestrator/figma/scripts/check-spec.mjs ' + stem + ' --gate',
    '    Validates the written spec.json files for structural completeness (stable identity, fill format, placeholder strings). If it fails, fix the spec files and re-run before continuing — same rule: re-read from Figma or re-curate, never fabricate (rule 2b).',
    '5d. Run: FIGMA_PIPELINE_RUN_ID="$FIGMA_PIPELINE_RUN_ID" node orchestrator/figma/scripts/evidence-bundle.mjs ' + stem + ' --stage prebuild --fresh',
    '    Writes orchestrator/.cache/figma/reports/evidence-' + stem + '.json with report paths/hashes for downstream agents.',
    '6. Print a final per-screen READY / MISSING table (which of the four per-screen files were written; index.json is one per task) plus the census summary and evidence bundle path. Include check-spec warnings (if any) in the summary.',
    '6a. Record the pull in the task journal so Task Details → Activity shows that design was pulled (best-effort — ignore any failure): python3 orchestrator/tasks/log-event.py ' + stem + ' design-pulled --phase design-pull --status ok --meta screens=<comma-separated ScreenNames you actually pulled, none-skips excluded> || true. This appends ONE line to orchestrator/.cache/tasks/journal/' + stem + '.jsonl and is the only write outside the figma cache dir.',
    '',
    'Strict scope: write screen artifacts ONLY under orchestrator/.cache/figma/screens/' + stem + '/ (step 4c\'s normalize-oracle rewrites its PNG/spec pairs in place there); steps 4c/5a/5b/5c/5d may write machine reports under orchestrator/.cache/figma/reports/, and steps 2b/6a append best-effort journal lines under orchestrator/.cache/tasks/journal/ via log-event.py (the only non-figma-cache writes). Do not touch orchestrator/figma/tokens/, orchestrator/figma/manifests/, any Kotlin/Gradle/Swift files, anything else outside orchestrator/.cache/figma/, or run any build. Never print tokens or secrets.'
  ].join('\n');
}

// Screen-DRIFT check: re-pull ONLY each already-cached screen's spec (structure/metadata) into a
// SHADOW dir and compare against the live spec (the baseline the code was built against). Answers
// "did this design change in Figma since we pulled it?". Lightweight by design — NO images, NO
// instances/context, NO gate re-runs — and it NEVER overwrites the live cache, so a drift check
// cannot disturb the oracle the done screens were certified against. Reuses the figma:screens:<stem>
// session (same Figma binding); the local comparison is check-screen-drift.mjs (no Figma).
function screenDriftPrompt(stem) {
  return [
    'Check whether the Figma design of task ' + stem + "'s already-pulled screens has DRIFTED since we pulled them. THIS session calls the MCP (read-only); it re-fetches ONLY each screen's structure into a shadow dir and never touches the live cache or the app.",
    '',
    'Do this:',
    '1. Read orchestrator/.cache/figma/screens/' + stem + '/index.json. For EACH node already in it (every <ScreenName> and its dark variant when darkUrl/darkNodeId is present), you will re-fetch and compare — do NOT add new screens, do NOT read the task file, do NOT pull anything not already cached.',
    '2. For each cached node, re-read it via the Figma MCP — get_metadata + get_design_context + get_variable_defs ONLY. Do NOT call get_screenshot (no images needed for a structure diff). THIS session calls the MCP; builders and validators never do.',
    '2b. Fail-soft: if a node read fails (needs-auth / rate limit / not found / moved), SKIP that screen (leave it un-compared) and note it in your report — never fabricate a shadow spec. If auth fails entirely, STOP and report needs-auth (owner reconnects Figma from the site Figma tab), same as the pull.',
    '3. Write, per screen, under orchestrator/.cache/figma/screens/' + stem + '/.drift/  (create the .drift subdir):',
    '   - <ScreenName>.spec.json — curate the value spec with the EXACT same shape and discipline as the figma:screens pull (same fields: screen, frameSizeDp, theme, elements[] with stableId/figmaNodeId/name/text/bboxDp/fills/textStyle/cornerRadiusDp/strokes/gapsToSiblingsDp/paddingDp/componentSetName/variantProps). Use the SAME stableId derivation as the original pull so unchanged elements keep the same id (else every element reads as removed+added). When the node has a dark variant, write <ScreenName>.dark.spec.json. NOTHING else — no .png, no .instances.json, no .context.json, no index.json.',
    '3b. Atomic writes: write each shadow spec to <name>.tmp then rename over the final name.',
    '4. Run the LOCAL comparison (no Figma): node orchestrator/figma/scripts/check-screen-drift.mjs ' + stem + '. It diffs each live orchestrator/.cache/figma/screens/' + stem + '/<Screen>.spec.json (baseline) against .drift/<Screen>.spec.json (fresh) and writes orchestrator/.cache/figma/reports/screen-drift-' + stem + '.json (per-screen CLEAN / DRIFTED / NOT_CHECKED + the change list).',
    '5. Print a per-screen table: CLEAN / DRIFTED (with the changes) / SKIPPED. If any screen DRIFTED, say so plainly and point the owner at the site Design tab\'s "Create actualization task" button.',
    '',
    'Strict scope: write ONLY under orchestrator/.cache/figma/screens/' + stem + '/.drift/ (the shadow specs) — NEVER edit or overwrite the live specs/pngs/index.json — plus the one machine report step 4 writes under orchestrator/.cache/figma/reports/. Do not touch anything else, run any build, or print tokens/secrets.'
  ].join('\n');
}

// Evidence re-bundle (LOCAL only — makes NO Figma call; golden invariant). The board's
// Done-modal "Rebuild evidence bundle" button spawns this when the sealed final bundle went
// stale (a report was regenerated after sealing) or half-stated (MIXED_RUNS/SUPERSEDED). It
// re-seals from the PRESERVED gate reports under the task's pinned run id — it never re-runs
// Figma pulls or Gradle and never hand-edits a report (fail-closed: a genuinely missing/red
// gate report means stop-and-say-which, not improvise).
function rebundlePrompt(stem) {
  return [
    'Rebuild the FINAL evidence bundle for task ' + stem + ' from its preserved gate reports. LOCAL ONLY: no Figma/MCP call, no Gradle, no code edits, no hand-editing of any report JSON — if a gate report is genuinely missing or red you STOP and report which one.',
    '',
    'Do this:',
    '1. Pin the run id: use orchestrator/.cache/figma/reports/.run-id-' + stem + ' when present; otherwise read .pipelineRunId from orchestrator/.cache/figma/reports/screenshot-' + stem + '.json (or any present gate report) and export it: export FIGMA_PIPELINE_RUN_ID=<that id>.',
    '2. node orchestrator/figma/scripts/evidence-clean.mjs ' + stem + ' --bundle-only   (removes ONLY the sealed bundle + digest; every gate report and the run-id pin survive).',
    '3. node orchestrator/figma/scripts/evidence-bundle.mjs ' + stem + ' --stage final --fresh',
    '4. If step 3 blocks with REPORT_INPUT_HASH_MISMATCH on the census report (registry entries the census consulted changed after it was written): node orchestrator/figma/scripts/component-census.mjs ' + stem + '   (the pinned run id keeps it on the same run), then repeat step 3 ONCE.',
    '5. If step 3 blocks on anything else, DO NOT improvise: print the bundle issues verbatim and stop — a missing/red gate report means the comparison must re-run through the task pipeline, not be patched here.',
    '6. Report the final overall (PASS/WARN, or the blocking issues verbatim). Best-effort journal line: python3 orchestrator/tasks/log-event.py ' + stem + ' note --phase ship --status ok --detail "evidence rebundled" || true',
    '',
    'Strict scope: the ONLY writes allowed are the ones steps 2-4 make via those scripts (the bundle, the digest, the census report). Never touch screens caches, specs, oracles, thresholds, task files, or any other report.'
  ].join('\n');
}

// Post-ship drift sweep. Unlike screenDriftPrompt (live cache vs shadow, per active
// task), this sweeps SHIPPED (done/) UI tasks: it diffs the COMMITTED ship-receipt spec baseline
// (what the code was certified against) against a fresh shadow re-pull, and marks a task's cert
// stale when its Figma design MOVED after ship. THIS session calls the MCP (read-only, structure
// only, into the shadow dir); the sweep script itself makes NO Figma call (golden invariant).
function shipDriftSweepPrompt() {
  return [
    'Sweep every SHIPPED (done/) UI task for post-ship Figma DRIFT — has the design moved since the task was certified? THIS session calls the MCP (read-only); it re-fetches ONLY each screen\'s structure into a shadow dir and never touches the live cache, the receipts, or the app.',
    '',
    'Do this:',
    '1. Enumerate the candidates: for each orchestrator/tasks/done/<stem>.md that has a pullable `## Design` bullet AND a committed spec baseline dir orchestrator/tasks/evidence/figma-ship/<stem>/specs/ (ship-done writes it). A done task WITHOUT that baseline is skipped — do not re-pull it.',
    '2. For EACH candidate stem, enumerate the screens + node URLs from the DONE task file\'s ## Design section (orchestrator/tasks/done/<stem>.md — committed, and verify-done\'s designHash net pins it to what was certified; same bullet forms as the pull: plain / light:<url> dark:<url> / dark:-only; skip `none` bullets). The live orchestrator/.cache/figma/screens/<stem>/index.json, when present, is a convenient cross-check — but NEVER the only source: the cache is gitignored/ephemeral, and on a wiped cache or fresh clone the sweep must still cover every receipted task. Re-fetch each screen via the Figma MCP — get_metadata + get_design_context + get_variable_defs ONLY (never get_screenshot; no images for a structure diff). Fail-soft: if a node read fails (needs-auth / rate limit / moved), SKIP that screen and note it — never fabricate a spec. If auth fails entirely, STOP and report needs-auth.',
    '3. Write each fresh spec, per screen, under orchestrator/.cache/figma/screens/<stem>/.drift/ (create the .drift subdir), with the EXACT same shape + stableId derivation as the original figma:screens pull (fields: screen, frameSizeDp, theme, elements[] with stableId/figmaNodeId/name/text/bboxDp/fills/textStyle/cornerRadiusDp/strokes/gapsToSiblingsDp/paddingDp/componentSetName/variantProps; <Screen>.dark.spec.json for a dark variant). Atomic writes (<name>.tmp then rename). NOTHING else — no .png/.instances.json/.context.json/index.json.',
    '4. Run the LOCAL sweep (no Figma): node orchestrator/figma/scripts/sweep-done-drift.mjs. It diffs the COMMITTED receipt baseline (evidence/figma-ship/<stem>/specs/) against each .drift/ spec and writes a committed evidence/figma-ship/<stem>/drift-stale-<stem>.json for every task whose design moved (and clears the marker for a task that no longer drifts).',
    '5. Print a per-task table: STALE (with the drifted screens) / clean / skipped. For each STALE task, point the owner at the site Board — the done card shows a "design drifted since ship" notice + a "Create actualization task" button.',
    '',
    'Strict scope: write ONLY the shadow specs under orchestrator/.cache/figma/screens/<stem>/.drift/ and let step 4 write the committed drift-stale markers under orchestrator/tasks/evidence/figma-ship/. NEVER edit the live specs/pngs/index.json, the committed receipt baseline, or the shipped task files. Do not run any build or print tokens/secrets.'
  ].join('\n');
}

// Pure key/action dispatcher shared by the browser and the CommonJS server
// (the server loads this ESM module via import()). Dynamic values come from the
// server-owned project config and runtime state, never from the Run request body.
export function figmaSessionPrompt(key, action, context) {
  context = context || {};
  if (key === 'figma:whoami' && action === FIGMA_SESSION_ACTION.WHOAMI) {
    return whoamiPrompt(context.verificationNonce);
  }
  if (key === 'figma:fileaccess' && action === FIGMA_SESSION_ACTION.FILE_ACCESS) {
    return fileAccessPrompt(context);
  }
  if (key === 'figma:sync-tokens' && action === FIGMA_SESSION_ACTION.SYNC_TOKENS) return syncGroupPrompt('tokens', context);
  if (key === 'figma:sync-components' && action === FIGMA_SESSION_ACTION.SYNC_COMPONENTS) return syncGroupPrompt('components', context);
  if (key === 'figma:shipdriftsweep' && action === FIGMA_SESSION_ACTION.SHIP_DRIFT_SWEEP) {
    return shipDriftSweepPrompt();
  }
  if (key.indexOf('figma:screens:') === 0) {
    var screenStem = key.slice('figma:screens:'.length);
    if (action === FIGMA_SESSION_ACTION.SCREEN_PULL) return screensPrompt(screenStem, context);
    if (action === FIGMA_SESSION_ACTION.SCREEN_DRIFT) return screenDriftPrompt(screenStem);
  }
  if (key.indexOf('figma:rebundle:') === 0 && action === FIGMA_SESSION_ACTION.REBUNDLE) {
    return rebundlePrompt(key.slice('figma:rebundle:'.length));
  }
  return null;
}
