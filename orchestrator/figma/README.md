# `orchestrator/figma/` — the Figma tooling sidecar

The single home for everything Figma. It sits next to
`orchestrator/site/` and `orchestrator/tasks/` and is copied wholesale into generated projects.
It is **plain Node** (the sidecar package and Site server both use Node 22) —
it is **never** part of the Gradle/KMP/app build (Gradle does not read `package.json`), so it
cannot leak into an APK/AAB/XCFramework.

## What this is

The pipeline is **Figma MCP design-context + a component pipeline + a token pipeline + local comparison gates**.
There are no Figma API dependencies here — only plain Node scripts plus `ajv` (JSON-schema validation),
`jimp` (screenshot comparison), and Tree-sitter/Kotlin grammar packages for the local Compose implementation model.

## Golden invariant

The Node server (`orchestrator/site/server/`) and these scripts **never call Figma directly** —
they only read/parse local files. Every actual Figma read (MCP `get_metadata`/`get_variable_defs`/
`get_design_context`/`get_screenshot`) happens inside a spawned `claude` session (a `figma:<verb>` session)
that inherits the project's MCP binding and writes JSON here for the scripts/agents to consume.

## Layout

```
package.json · .nvmrc · .env.example   workspace manifest (the project-root package-lock owns ajv + jimp + tree-sitter + Kotlin grammar)
scripts/      committed   doctor, security grep, hash-verified observed-token/component generation readers, component census, screen/spec/pixel gates, and evidence packaging
scripts/_index/  committed script-manifest.json — sha256 pins for every production gate script (.mjs/.cjs; only generated _index/ is excluded), the central test runner, and the enforcement hook; `figma:doctor` fails on any drift or an unpinned script (regen: `python3 scripts/_index/_generate_script_manifest.py`; pre-commit auto-regens on a staged production script edit)
tests/        committed   41 Figma workspace test entrypoints plus their helpers and calibration/census/spec fixtures; owned by the central test runner and excluded from the production script manifest
skill/        committed   SKILL.md (the implement-figma workflow; installed to .claude/skills/)
token-schemas/ committed  JSON Schemas for the gate/report models (named token-schemas/, not schemas/, to dodge the root `**/schemas/` Room ignore) — includes census/spec-v2/instances, implementation-model, screen-index, report-envelope, gate report schemas, the screenshot-thresholds config schema, the calibration-labels schema, and the per-task bindings schema (bindings.schema.json — the screens/<stem>/bindings.json declaration: schemaVersion 2, `components[]` rows keyed by designComponentId)
screenshot-thresholds.json committed  the ONE source of the screenshot gate's numeric strictness: compare-screenshots derives its env-knob defaults from it, evidence-bundle derives the anti-forgery canon values from the SAME file; pinned by the synthetic calibration corpus (figma:test:calibration)
schemas/      committed   strict observed-source/catalog/health/receipt/binding schemas plus token/project/component v2 comparison, mapping, baseline, and task contracts
runtime/      committed   shared deterministic runtime: run-plan.mjs (the trusted runner child the site server spawns for normalize-capture/token-compare/normalize-component-capture/component-compare), adapter-config/-registry, token-extraction, component-extraction, input-snapshot, canonical-json, glob, typed errors
tokens/       committed   observed-source normalization, deterministic aggregation/binding/comparison, mapping, receipt/outbox, baseline, and error contracts
components/   committed   component pipeline domain logic (capture normalizer, comparator, mapping contract + suggestions, task suggestions, baseline, limits, error codes) + the committed design-component-inventory.json generation artifact (role design-component-inventory; witnessed full-census capture normalized by the server-owned runner; sparse variant tuples, unknown property types stay unsupported; identity designComponentId = figma-component:<fileFp16>:<none|branchFp16>:<nodeId>) with visual evidence under components/visual/<h32>.png (role component-visual-evidence:<h32>) once components are synced
adapters/     committed   built-in extractors by kind: kotlin-compose (tree-sitter semantic parser over the declared authority symbols; capabilities tokens + components — project component identity `<adapterId>:symbol:<fqName>[#disc]`), json-tokens (the non-Kotlin token proof adapter), and component-manifest (the non-Compose component proof adapter)
project-adapters.json committed (product repos)  the owner-declared adapter configuration (token + component capabilities); the first site Sync creates it from the template's standard Step 6 Compose layout, then leaves it product-owned and never overwrites it; manual authoring is only for non-standard layouts
token-mappings.json   committed (product repos)  the design↔project token mapping registry (absent = the exact revision-0 empty registry; every mutation is CAS-guarded)
component-mappings.json committed (product repos)  the design↔project Component Mapping Registry (mappingId `cmap-<24hex>`; owner `visualPolicy.renderClass` lives here; absent = the exact revision-0 empty registry; every mutation is CAS-guarded through the site's Mapping Review or finalization's components phase — never hand-edited)
manifests/    committed   `.gitkeep` placeholder only (no production contract lives here)
.account.json gitignored  bound-account identity (whoami)
```

Runtime caches do **not** live under this dir — they sit in the consolidated repo-root
`orchestrator/.cache/figma/` tree (gitignored at the repo root, not here):

```
orchestrator/.cache/figma/
  reports/      census/spec/gate JSON output (runtime-created); token-comparison/ is the canonical logical home of the token-drift generation artifacts (analysis-index, per-adapter project inventories, mapping-snapshot, comparison, baseline); component-comparison/ is the same for the component-drift domain (analysis-index, project-inventory-<adapter>, mapping-snapshot, comparison, suggestions, task-suggestions — fixed roles that publish together — plus the optional baseline)
  artifacts/screenshot/<stem-segment>/<run-segment>/<seq>-<screen>-<theme>/ (e.g. 001-Home-primary/)   retained hash-checked screenshot-compare evidence: figma.png · actual.png · diff.png · overlay.png · manifest.json (logical stem/run identities stay full in reports; bounded path segments use the shared collision-resistant mapper; retained by SCREENSHOT_ARTIFACT_RETENTION)
  generated/    Kotlin token preview output (this template, no app)
  screens/<stem>/   per-task screen cache: <Screen>.spec.json · .instances.json · .context.json · .png + exact <Screen>[.<variant>].tokens.json observation sidecars + index.json schemaVersion 3 (each variant binds the sidecar bytes/hash and reserved operation sequence) + bindings.json (the declared optional binding artifact, schemaVersion 2: pull seeds screens[], component-census upserts the id-keyed components[] rows from the mapping registry truth, builders fill implFile/composable — builders never author components[] rows; a present malformed file is rejected) — written by the figma:screens:<stem> session from a server-owned immutable capture plan (which then runs normalize-oracle: embedded iOS device chrome is stripped from PNG+spec at the pull boundary and stamped as `chromeCrop`), contract in the implement-figma skill (screen-cache/census contract)
```

## Secrets

`.account.json` (here) and the `orchestrator/.cache/figma/` tree are gitignored **and** denied by the
site's static server (`server/static.js` dotfile/secret denylist) so they cannot be served. Never put a
token in `package.json`, `.env.example`, or the command line. Design context comes only from the
OAuth-bound MCP — there is no REST/token fallback.

## Clearing the project integration

The Figma panel's **Clear integration** action is the supported destructive reset. After explicit
confirmation it removes only this project's Local-scoped `figma` MCP OAuth binding and connector,
the selected `figmaLibraryUrl`, the account receipt and local secret remnants, all Figma runtime
cache/history, the committed generation catalog, token/component mappings, and generated token and
component artifacts. It also removes the stale Setup draft value so the old file cannot reappear.

The reset deliberately keeps product source files, task files and evidence, `figmaEnabled`, and
`orchestrator/figma/project-adapters.json`. It is blocked while a Figma/task session, verification,
sync, publication, or another protected workspace writer is active. Unsafe paths fail closed; the
button never performs a broad unguarded filesystem deletion.

## Scripts

Package aliases are invoked via `npm run <script>` from `orchestrator/figma/`. Helper CLIs without aliases
(`extract-app-tokens.mjs`, `resolve-screen-spec.mjs`) are invoked directly with `node scripts/<file>.mjs`.
Key composite:

`figma:ship-done` is an internal interlock owned by
`orchestrator/tasks/finalize-task.mjs`; a direct invocation has no active
transaction marker/owner and intentionally fails before touching the task.

| Script | What it chains |
|---|---|
| `figma:verify` | Runs the Figma-owned gates, test suites, Site integration contracts, and shipped-task evidence check. Use root `npm run verify:full` for the complete project gate. |

The aggregate task suite runs the process-containment probe on Linux. On
macOS, run `node orchestrator/tasks/tests/test-intake-model-wrapper.mjs` separately on
a dedicated host: combining `sandbox-exec` fork-denial probes with the full
suite can leave a kernel-wait process after a forced test timeout.

## De-scoping a UI task (owner workflow)

When a task's Figma comparison must be deliberately dropped (the design was deleted upstream,
or the owner reclassifies the work as derive-only), run the SANCTIONED tool instead of hand-
editing — it keeps every backstop honest and leaves an audit trail:

```
node scripts/descope-task.mjs <stem> --reason "design deleted upstream" [--yes]
```

Dry-run by default. With `--yes` it (1) rewrites every `## Design` bullet to the audited
`none (<reason>)` form (grammar re-validated; a malformed result aborts), (2) lists residual
body citations it will NOT auto-edit (exit 2 until the operator removes them — the cache
stays, so the de-UI backstops stay armed; a lingering machine snapshot is reported in the
`designComponentId:` grammar), (3) removes `.cache/figma/screens/<stem>/`
CONFINED via ensureContained, and (4) writes a committed receipt under
`orchestrator/tasks/evidence/descope/<stem>.json`. Retiring a component whose design node no
longer exists is a separate, equally auditable act: a CAS `retire-mapping` operation through
the site's Mapping Review — never a hand edit of `component-mappings.json`. Never hand-`rm`
the screens cache either; both paths stay auditable through their sanctioned tools.

## Calibration corpus — adding a disputed REAL pair (owner workflow)

The synthetic corpus (`tests/calibration/recipes.json`) pins the committed
`screenshot-thresholds.json` by construction. When a LIVE gate verdict is disputed (a false
blocker, or a miss a human caught), feed that pair back:

1. The evidence already exists on disk: `orchestrator/.cache/figma/artifacts/screenshot/<stem-segment>/<run-segment>/<seq>-<screen>-<theme>/ (e.g. 001-Home-primary/){figma,actual,diff,overlay}.png` + the run's `screenshot-<stem>.json` report under `.cache/figma/reports/`. The report retains the full logical stem/run id and references the mapped artifact paths.
2. Copy the report into a corpus dir's `reports/` and add the human verdict to its `labels.json` (`{screen, theme?, expect: pass|minor|fail}` — schema: `token-schemas/calibration-labels.schema.json`). Copying `.cache` ephemera into git is a deliberate act: scrub product pixels you don't want published.
3. Sweep: `npm run figma:calibrate-thresholds -- --corpus <dir> --out ledger.json` — it recommends the zero-missed-fail thresholds (global MAJOR floor, worst-zone floor, PASS/MINOR boundary) with the full confusion sweep for the trade-off call.
4. If the numbers move: edit `screenshot-thresholds.json` in its OWN commit with the ledger linked, **bump its top-level `version`** (the gate-policy version — every report and final digest stamps it as `gatePolicyVersion`, so done/ receipts record which strictness regime certified them), and re-pin any shifted synthetic verdicts in `recipes.json` (the `figma:test:calibration` failures list them exactly).

## Gating

The product-facing pipeline is opt-in via `figmaEnabled` in `project-config.md` (default `false`).
When disabled, the sidecar health checks may still run locally, but the
Figma-specific gates no-op or report advisory setup state instead of enforcing UI fidelity.

**Enforcement wiring.** For a `figmaEnabled` product, `figma:doctor` also verifies the
mechanical screenshot-gate net is actually wired — `core.hooksPath` →
`orchestrator/skills/checks/hooks` (the pre-commit `verify-done` gate). It reports as an
advisory `WARN` in a plain run (bootstrap wires it at launch Step 14, after the Step-6.5
doctor) and a hard FAIL under `FIGMA_STRICT_WIRING=1` (the launch Step-14 post-install
verification) — so a product cannot silently ship UI without that net.

**Run-time enforcement (the "un-netted product cannot run tasks" half).** The doctor report is
advisory; the run-time gates are not. While `core.hooksPath` is unwired in a `figmaEnabled`
product: the site server refuses `run` execution (`sessions.runGateError()` —
`POST /api/tasks/actions` with the current typed `run` action returns 409 `figma-net-unwired`; already-queued runs are held
un-claimed, and the header Skills pill shows the wiring command), and the run-loop's Step 0
bootstrap check emits `BLOCKED[figma-wiring]` for runs that bypass the site (a /loop worker or a
manual terminal session). The server never wires git itself (observe-only invariant) — it names
the command. Deliberate opt-out for products that self-manage hooks (`install-skills.sh
--no-hooks`): `FIGMA_WIRING_GATE=0` in the server's environment — the operator then owns chaining
`verify-done` into their own pre-commit.

Fixture tests may set `FIGMA_CACHE_ROOT` to a temp directory; production defaults to
`orchestrator/.cache/figma`. Use the root override for end-to-end evidence fixtures so
compare artifacts and final evidence validate against the same cache-relative paths.
