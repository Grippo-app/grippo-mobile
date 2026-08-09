# Spec-fidelity gate — `figma-spec-validator` surface

<!-- Source of truth pinned by orchestrator/contracts/agents/figma-spec-validator.md.
     The final evidence bundle requires the `spec` report this gate writes. Machine baseline verified against
     orchestrator/figma/scripts/extract-compose-model.mjs + compare-screen-spec.mjs; report
     envelope verified against evidence-bundle.mjs REPORT_PREFIXES/DEFAULT_REQUIRED (final
     requires `spec` + `spec-compare` in gate mode with fresh inputHashes). -->

The spec-fidelity gate compares the values the screen code **declares** (resolved
through AppTokens) against the screen's cached Figma value spec
(`orchestrator/.cache/figma/screens/<stem>/<ScreenName>.spec.json`). Text-vs-text:
no rendering, no gradle, never calls Figma (golden invariant). Blocker/Major
findings route to the owner builder; Minor findings are advisory
(suggestion-only → `### Caveats`). The known residual is honest and documented:
emergent layout the code never declares (a missing `fillMaxWidth()`, text
wrapping, `weight` distribution) is OUT of this gate's scope — the
screenshot-fidelity gate covers rendered reality.

- Identity/tool budget: pinned by `orchestrator/contracts/agents/figma-spec-validator.md`
  (`Read, Bash, Grep, Glob`, model sonnet). No code edits, report-only.
- The rendered-pixels half of the comparison is
  [screenshot-fidelity-gate.md](screenshot-fidelity-gate.md); this file owns the
  declared-values half and the `spec-<stem>.json` report the final evidence
  bundle requires.

## Applicability (check first)

The gate applies iff ALL hold — mirroring the mandatory-comparison doctrine
(missing REQUIRED inputs on a UI task are a **BLOCKER**, never a silent skip):

1. `figmaEnabled: true` — `rg -m1 '^figmaEnabled:' orchestrator/project-config.md | awk '{print $2}'`.
2. The task's `## Design` section carries at least one bullet whose value is not
   the literal `none`.
3. `orchestrator/.cache/figma/screens/<stem>/` exists. If (1)+(2) hold but the
   cache is absent, that is a **BLOCKER** (`BLOCKED[figma-screens]: screens cache
   missing — run the figma:screens pull`), not a skip: orchestrator Step 1b owns
   pulling it before the loop reaches the validators.

A task failing (1) or (2) is non-UI **for this spec gate**: report `SKIPPED (not a
Figma UI task)` with zero findings. (A task that cites a Figma node URL / a
`designComponentId:`+`figmaNodeId:`/`frozenStructuralHash:` snapshot or edits a
screen/dialog file yet omits `## Design`
is separately blocked by the `ship-done`/`verify-done` UI-by-evidence backstop, so
skipping it here does not let it ship uncompared.)

**Pre-condition: spec sanity.** `check-spec.mjs --gate` (Step 1b) blocks before
this validator on schema-invalid specs, invalid fills/strokes, placeholders,
filename/screen/theme mismatches, and missing stable identity. Only WARN-only
findings reach this validator; emit one NOTE per WARN-excluded element and do
not duplicate findings for that element.

**Dual-theme validation.** When both `<ScreenName>.spec.json` (light) and
`<ScreenName>.dark.spec.json` exist, run all four comparison families against
each spec independently and report per-theme verdicts. A Blocker or Major in
either theme fails that screen. **A declared dark theme MUST have a dark spec**:
if the `## Design` bullet declares `dark:<url>` but `<ScreenName>.dark.spec.json`
is absent, emit a **Major** (`rule_id=spec.theme`, evidence "bullet declares
dark:<url> but the dark spec is absent — re-run figma:screens:<stem>") and FAIL
the screen. A single-theme bullet (plain URL) legitimately has no dark spec.

## Inputs from the orchestrator prompt

- The task stem (locates the cache + the task file).
- The shared `FIGMA_PIPELINE_RUN_ID` from orchestrator Step 1b. Reuse it for
  every machine report; do not mint a new id inside this validator. (Safety net:
  the id is also file-pinned at `orchestrator/.cache/figma/reports/.run-id-<stem>`
  — an env-less tool invocation reuses the pinned id instead of minting — but
  pass the explicit id anyway so a stale pin is corrected, not inherited.)
- The implementation plan (verbatim — its `## Files to create` / `## Acceptance
  mapping` give each screen's Kotlin files and the **Owner builder** to route to).

If a required input is missing, report `BLOCKED: <which input> missing` and stop.

## Steps

### 0. Run the machine baseline first

From the implementation plan, collect the Kotlin files for the affected
screen/dialog and run. For multi-screen stems, pass one
`--screen-map <ScreenName>=<Screen.kt>` per screen. The value must be an exact
readable path; filenames are never searched under `--impl-root`. A single
screen may use the sole declared `--impl-file`. **Binding declaration:** when
the task's `screens/<stem>/bindings.json` carries `implFile` per screen (the
pull seeds the file; the BUILDER fills `implFile`/`composable` as it lands each
screen — that is the builder's declared binding, not a validator edit), the gate
reads it as the screen map and a bound stem needs zero `--screen-map`
plumbing. A conflicting CLI/env declaration blocks; neither source overrides
the other. Only SCREEN-kind index
nodes need a mapping — `component`-kind nodes (extracted sub-frames pulled as
references) compare against the shared evidence. A spec file absent from a
present `index.json` is a `SPEC_NOT_IN_INDEX` blocker.

Component owner-binding is FILE-first: a component call binds to the screen when
it lives in the screen's declared implementation file AND its owner composable
exactly matches the screen name or the binding's explicit `composable` value.
Calls inside private same-file sub-composables bind through the model's
owner-closure entries (`ownerVia`). A call owned by a DIFFERENT screen's
composable never satisfies this screen, even in the same file.

A Figma component-SET name and a Kotlin composable name are different identity
domains. Every component instance requires an exact `components[]` entry in
the task's `bindings.json` (schemaVersion 2 rows keyed by `designComponentId`:
`setNodeId`, `setName`, `mappingId`, and `implementations[]` with `adapterId`,
`platform`, `projectComponentId`, `sourcePath`; census-derived from the mapping
registry — never hand-authored). This is the only join, and it resolves
**id-first**: a spec element should carry `componentSetNodeId` (the durable
identity); a display name resolves only while it is unique — a name matching
several id-anchored bindings is a `COMPONENT_BINDING_AMBIGUOUS` blocker, and a
missing or partial binding is a `COMPONENT_BINDING_REQUIRED` blocker. Mapping
display labels, normalized names, and same-name guesses are not accepted.
Component calls, owner composables, and declared files then match the binding
exactly (the bound composable name is the `projectComponentId` symbol leaf).

```bash
STEM=<stem>
export FIGMA_PIPELINE_RUN_ID=<same id from orchestrator Step 1b>
IMPL_MODEL="orchestrator/.cache/figma/reports/implementation-model-${STEM}.json"
node orchestrator/figma/scripts/extract-compose-model.mjs \
  --out "$IMPL_MODEL" \
  --file <Screen.kt> [--file <component.kt> ...]
node orchestrator/figma/scripts/compare-screen-spec.mjs "$STEM" \
  --impl-model "$IMPL_MODEL" \
  --impl-file <Screen.kt> [--impl-file <component.kt> ...] \
  [--screen-map <ScreenName>=<Screen.kt> ...] \
  --gate
```

The implementation model and declared-value scanner form one current gate. The
model supplies element/call binding while the source scanner supplies token and
literal evidence; there is no alternate engine selection.

Keep `orchestrator/.cache/figma/reports/spec-compare-<stem>.json`. If it reports
`BLOCKER`, first classify setup/contract blockers
(`MULTI_SCREEN_IMPL_MAPPING_REQUIRED`, missing implementation files/models,
unreadable specs, invalid screen maps) as fail-closed validator-setup findings
with a recovery instruction; do not route those to a builder as implementation
defects. True implementation-mismatch blockers become routed findings. `WARN`
issues become Minor/advisory unless manual inspection proves a Major. Never
ignore a missing report or a failed `--gate`: it is the deterministic baseline
for token-backed color/value evidence plus the implementation-model contract.
The scanner is intentionally bounded — the manual families below still handle
inventory, composed spacing equivalence, component defaults, and routing nuance.

### 1. Resolve the token tables (once)

Read the project's token sources into lookup tables — plain Kotlin literals:

- `AppDp` (design-system core — see the `design-system` skill, `references/dp.md`).
- The `AppColor` implementation(s) (`references/color.md`).
- `AppTypography` styles (size / weight / line height — `references/typography.md`).
- The committed `observed-token-catalog` and exact `token-binding-snapshot` generation artifacts (resolved via
  `orchestrator/figma/scripts/lib/design-inventory.mjs`, mode selected by
  `figmaTokenMode`) — to resolve `{group.token}` refs the spec uses (the same
  source the Kotlin token preview consumes).

### 2. Compare, per screen (every non-`none` `## Design` bullet)

Read `<ScreenName>.spec.json` and the screen's Kotlin files from the plan. Four
comparison families:

| Family | Check | rule_id |
|---|---|---|
| **Inventory** | Every spec element has a code counterpart (a composable call / mapped component); code blocks with no spec counterpart are extras. | `spec.inventory` |
| **Values** | Declared paddings / sizes / radii / gaps vs the spec's numbers, resolved through the token tables. **Composed-equivalence rule:** a spec gap may legitimately be the SUM of adjacent declared paddings / `Arrangement.spacedBy` (spec 16 = code 8+8) — record `equivalent-sum`, it is NOT a finding. A spec value the code leaves to a component default is Minor at most. | `spec.value` |
| **Tokens** | The color / typography slots the code uses resolve to the spec's hex / size / weight ("right family, wrong slot" — e.g. `text.secondary` where the spec says `text.primary` — is a finding). | `spec.token` |
| **Literals** | Inline `dp` / `sp` / `Color(0x…)` in the diff'd screen files. Other validators may flag the same sites under their own rule_ids — the `(file, rule_id)` dedup does NOT collapse across different ids. Emit it anyway — this finding carries the spec value the literal should have been. | `spec.literal` |

### 3. Severities

| Severity | When | Routed? |
|---|---|---|
| **Blocker** | Missing/extra element; wrong component variant. | yes |
| **Major** | Declared value or token contradicts the spec (>2dp off, wrong slot, wrong text style). | yes |
| **Minor** | ≤2dp deviation; stroke/radius nuance; spec value delegated to a component default. | **no — advisory** |

Verdict: **PASS** when zero Blocker + zero Major (Minors allowed — they ship as
`### Caveats` bullets); otherwise **FAIL**.

### 3.5 Element-scoped machine gate — exact binding

Structure — not pixels — is the **primary, threshold-free** signal: "this element
carries this text / token / dp." The machine gate (`compare-screen-spec.mjs` +
`extract-compose-model.mjs`) makes it **element-scoped where it reliably can**:

- **Binding.** A spec element that is a component INSTANCE (`componentSetName`) is bound
  to ONE code call by callee + owner (`boundCallFor`). The tree-sitter model records that
  call's structured **args** (`extract-compose-model.mjs`: inline `N.dp`, multi-arg
  padding, `Arrangement.spacedBy(N.dp)`, `AppTokens.*`/`AppColor.*` refs, `Color(0x…)`
  literals), so the check is against THAT element's own arguments, not a file-wide bag.
- **Element-precise signals are advisory WARN — never a false BLOCKER.** The bound call's
  colour/dp args are an UNDIFFERENTIATED bag (the model records each `Color(0x…)` /
  `N.dp` without knowing which PARAMETER it was passed to), so an element-precise contradiction
  cannot be proven without per-arg binding — a hard block would false-block correct code
  (proven: a coincidental ripple/gradient raw colour, or a call-site margin ≠ a
  component-internal padding). The **strict** "wrong/missing token" and "missing component"
  BLOCKERs are the pre-existing FILE-SCOPED checks (already element-named); the element-scoped
  layer only ADDS location + advisory hints:
  - `ELEMENT_HARDCODED_COLOR_HINT` (WARN): a bound call passes the raw hex a fill token
    resolves to AND does not use that token at the call. Alpha is compared (a translucent
    scrim never matches a solid fill), and it is suppressed when the call already uses the
    token (a coincidental sibling raw is then not this fill's hardcode).
  - `ELEMENT_DP_DIVERGENCE` (WARN): the bound call's dp args match none of the spec dp NOR
    any composed **subset sum** (composed-equivalence). Advisory because a component's spec dp
    (cornerRadius, internal padding) is usually INTERNAL, not a call arg. An EMPTY dp arg set
    (delegated to the component default) is not flagged.
  - `COMPONENT_COLOR_DELEGATED` (WARN): a spec element that is a BOUND (id-anchored
    binding-matched) component instance (or an instance-INTERNAL node, `I…;…` figmaNodeId)
    whose fill token does not resolve in the screen's evidence — the token binding lives in
    the reused component's OWN source and is certified by the component's own gates, so the
    screen is not falsely blocked for internals it cannot change. Advisory: never routed to
    the screen builder (it dedups with the component's own gate findings via the routing
    guard below); it rides the reviewed-WARN `### Caveats` path. **Ordering is load-bearing:**
    the `HARDCODED_COLOR_FOR_TOKEN` contradiction is checked FIRST and stays a BLOCKER —
    delegation can never launder a hardcode. When the component's source is in the evidence
    scope AND uses the expected token, the element clears entirely (no WARN at all).
- **Fallback → WARN (the load-bearing fail-safe).** When the element cannot be bound to a
  SINGLE call (ambiguous, absent, or a partial/failed model), the element-precise pass is
  skipped and only the file-scoped checks stand, plus an advisory
  `ELEMENT_BINDING_UNRESOLVED` WARN — **never a false BLOCKER**, so an unresolvable binding
  can never deadlock a builder (they cannot force the model to resolve). Each comparison
  row records `binding: resolved | unresolved | n/a`.
- **Canvas/DrawScope widgets — hoisted-token handling (NOT a class exemption, NOT a downgrade).**
  A Canvas widget cannot call the `@Composable` `AppTokens.*` accessor inside a draw lambda, so it
  hoists `val c = AppTokens.colors.group` and uses `c.leaf` — the full token path never appears
  verbatim, which would false-BLOCKER as `MISSING_COLOR_TOKEN`. This is fixed by **evidence alias
  resolution** (extract-app-tokens `resolveAliases`, wired only from this gate): it recovers the
  full path from a real `val NAME = AppTokens.…` + `NAME.tail` pair — and only that pair — so a
  correct canvas widget is a clean PASS, identical to a declarative one. A TYPE-ANNOTATED hoist
  (`val c: AppColor.TextColors = AppTokens.colors.text`) resolves the same way. When the gate
  consumes PRE-EXTRACTED evidence via `FIGMA_APP_TOKENS`, the extraction must have run with
  `--resolve-aliases` (`node scripts/extract-app-tokens.mjs --resolve-aliases …`) — an
  alias-less pre-extract silently revives the canvas false-BLOCKER on that input path. The resolver is
  scope-sound: an alias is cut at the first point its name is re-bound (a second `val`/`var`, a
  `for`/lambda/destructuring/fn parameter), so a shadowed `NAME.leaf` cannot fabricate a key that
  masks a real mismatch. The gate is NOT softened for canvas: a genuinely missing token stays a
  `MISSING_COLOR_TOKEN` BLOCKER, `HARDCODED_COLOR_FOR_TOKEN` stays a BLOCKER for every class, and an
  unbindable hoist form (a `with(group){…}` block or an alias-of-alias) also stays a BLOCKER — the
  builder uses a direct `val` hoist. The canvas class is purely a **label**: the report records
  `widgetClasses: { <screen>: "canvas" }` (surfaced by evidence-bundle as
  `verifiedAs: "canvas-component"`) for the audit trail; it changes no verdict.

### 4. Output

Standard findings shape, one row per Blocker/Major:

```
(file=<Screen.kt path>, rule_id=spec.<family>, severity=Blocker|Major,
 evidence="spec: <value> @ <element> — code declares <value> @ <file:line>",
 routed_to=<Owner builder from the plan>)
```

Plus a separate `### Advisory (Minor — not routed)` list — the orchestrator
copies each as one `### Caveats` bullet (suggestion-only channel). Plus a
per-screen verdict table (`<ScreenName>: PASS (n minors) | FAIL (n findings)`).

Author the machine copy (`orchestrator/.cache/figma/reports/spec-<stem>.json`,
transient — `.cache/figma/reports/*` is gitignored) **via the CLI, never by
hand-assembling the envelope**:

```bash
node orchestrator/figma/scripts/write-spec-report.mjs "$STEM" \
  --screen "HomeScreen=PASS" \
  --screen "DetailScreen=FAIL:wrong token slot" \
  --issue  "MAJOR:spec.token:DetailScreen:text.secondary where spec says text.primary" \
  --issue  "MINOR:spec.value:HomeScreen:2dp radius nuance"
```

- One `--screen "Name=PASS|FAIL[:note]"` per compared screen (every non-`none`
  `## Design` bullet). One `--issue "SEVERITY:rule_id:screen:message"` per §3
  finding — `SEVERITY` is the agent taxonomy (`BLOCKER|MAJOR|MINOR`,
  case-insensitive; stored UPPERCASE), `rule_id` is one of the `spec.*` ids —
  the §2 families (`spec.inventory|spec.value|spec.token|spec.literal`) plus
  `spec.theme` (the dual-theme missing-dark-spec Major) — the message may
  contain `:`. For many findings, `--verdict-file verdicts.json` takes the
  same data as JSON (`{screens:[{screen,verdict,note?,minors?}],
  issues:[{severity,ruleId,screen,message,file?}]}`) instead of the flags.
- The CLI owns every clerical field that used to be hand-typed: it computes
  `overall` (§3 rule: `BLOCKER` on any routed Blocker/Major finding — a
  Major-only run IS `BLOCKER`; `WARN` on Minors only; `PASS` when clean), the
  envelope counts under the severity taxonomy, the file-pinned
  `pipelineRunId`, the `gatePolicyVersion` stamp, and the sha256
  `inputHashes` pin of `spec-compare-<stem>.json` (+ the implementation model
  the baseline recorded, when present). It schema-validates against
  `token-schemas/spec-report.schema.json` BEFORE writing and writes
  atomically.
- It also enforces verdict↔finding consistency up front: a FAIL screen with
  no routed finding, a routed finding on a PASS screen, an issue naming an
  undeclared screen, or a `minors` count unbacked by MINOR issue rows are
  rejected with an actionable message — inconsistencies the bundle either
  surfaced late as a fail-closed blocker or, worse, could NOT see at all (a
  FAIL verdict row beside an empty `issues[]` shipped as a PASS that
  certified nothing).
- It REFUSES to run when `spec-compare-<stem>.json` is missing: the agent
  report certifies on top of the Step-0 machine baseline, never instead of it.
- Component-internal escalations (routing guard below) are NOT `--issue` rows —
  they go to the orchestrator as escalation bullets.
- Re-running the CLI after a fix-cycle `compare-screen-spec` re-run re-pins the
  fresh baseline hash automatically (hand-computed hashes went stale silently).

The final evidence bundle hard-requires this report in `mode: "gate"` with
non-empty `inputHashes` under the shared `pipelineRunId` — all guaranteed by
the CLI.

**Routing guard:** a mismatch rooted *inside* a design-system component (the
spec disagrees with the component's own internals, not with the screen's usage
of it) is NOT routed to the screen builder — emit it as an escalation bullet:
`component <X> diverges from its design spec — separate design-system task`. The
one-caller rule stays intact; screens never mutate the design system.

## What this gate MUST NOT do

- No code edits — it reports; builders fix.
- No gradle, no rendering, no screenshots, no Figma/MCP calls. Text comparison only.
- Do not fail the gate on Minors, on `equivalent-sum` cases, or on the
  documented residual (emergent layout) — flagging un-declared layout behaviour
  is a false positive by definition.
- Do not run the component census or mutate the mapping registry — component
  coverage is task-prep Step 5.5's and orchestrator Step 1b's concern.
- Do not mine `done/` tasks for precedent on what this gate accepts — they are
  immutable records certified by their stamped `gatePolicyVersion`. The committed gate scripts +
  `screenshot-thresholds.json` are the only authorities.
- Do not route DS-component-internal divergence to a screen builder (escalate
  per the routing guard).

## Appendix: raw report envelope (debugging only)

What `write-spec-report.mjs` emits — for READING a produced report or debugging
a bundle rejection, not for authoring (hand-authoring still validates at the
bundle, but every field below is a clerical trap the CLI exists to own):

```json
{
  "schemaVersion": 1,
  "gatePolicyVersion": 1,
  "taskStem": "<stem>",
  "pipelineRunId": "<the file-pinned run id>",
  "mode": "gate",
  "inputs": { "specCompareReport": "reports/spec-compare-<stem>.json" },
  "inputHashes": { "reports/spec-compare-<stem>.json": "sha256:<hash>" },
  "overall": "PASS|WARN|BLOCKER",
  "blockingCount": 0,
  "warningCount": 0,
  "issues": [
    { "severity": "MAJOR", "issueKind": "spec.token", "screen": "<ScreenName>", "message": "…" }
  ],
  "reportPath": "reports/spec-<stem>.json",
  "generatedAt": "<ISO8601>",
  "screens": [{ "screen": "<ScreenName>", "verdict": "PASS|FAIL", "note": "…" }],
  "authoredBy": "write-spec-report-cli"
}
```

Gotchas the CLI computes for you: `issues[].severity` is the UPPERCASE envelope
enum (title-case fails `report-envelope.schema.json` → `REPORT_SCHEMA_INVALID`,
plus `REPORT_BODY_SCHEMA_INVALID` since `spec-report.schema.json` allOf-refs
the envelope); `blockingCount` counts only BLOCKER-class
severities while `MAJOR`/`MINOR` count toward `warningCount`, yet `overall` is
`BLOCKER` whenever any routed Blocker/Major finding exists — a Major-only run
records `overall: "BLOCKER"` with `blockingCount: 0`; `screens` must be
non-empty (an empty list certifies nothing and
`token-schemas/spec-report.schema.json` rejects it); `inputHashes` must be
non-empty at final and re-verified against the LIVE files, so a stale
hand-computed hash blocks as `REPORT_INPUT_HASH_MISMATCH`.
