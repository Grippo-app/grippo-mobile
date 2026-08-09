# Figma split — component census, design-system-first split, preview states

Self-contained reference for `task-prep` Step 5.5 (component census + design-system-first split) and
Step 5.5a (`## Preview states` + screenshot-gate bullets). Both run **inside** Step 5's promotion path,
gated on `figmaEnabled: true`. The base todo shape +
spec-gate bullets live in [`acceptance-anchors.md`](acceptance-anchors.md); the prep flow that calls
into this is [`prep-flow.md`](prep-flow.md). Contract: the figma:screens pull prompt
(`screensPrompt` in `orchestrator/site/scripts/figma-actions.js` — the screen-cache/census
authoring contract; no markdown mirror exists).

---

## Step 5.5 — component census & split (conditional)

**Gate**: `figmaEnabled: true` AND the task has at least one non-`none` `## Design` bullet — **any**
node kind (`[screen]`/`[dialog]`/`[component]`/`[overlay]`). This is the same kind-agnostic predicate
the enforcement layer gates on (orchestrator Step 1b/4.6b, `ship-done`): a `[component]`-only task
still gets the cache pre-flight + census here — the node kind picks the capture harness downstream,
never whether the gates apply. **A Canvas/DrawScope widget is still a `[component]`** — "canvas" is
NOT a bullet tag; the spec gate auto-detects it from the widget's own source (`lib/canvas-detect`)
and handles hoisted tokens itself. Do not invent a `[canvas]` tag: the kind set is closed, so a
`[canvas]` bullet surfaces `UNRECOGNIZED_KIND_TAG` (warn-grade) and silently falls back to
`[screen]`, misrouting the capture harness — the canvas class is never expressed in the bullet. When the gate is closed (non-UI task), this step is a no-op — continue
Step 5's proposal/promotion helper as usual.

When the prompt contains `BOARD PREP POLICY: NO QUESTIONS.`, every owner-remedy
case below is a typed non-question blocker with its exact remediation CTA.
Never invoke `ask`, publish pending questions, guess a mapping, or auto-mutate
the mapping registry. This is action-scoped to Board **Prepare** and does not
change direct task-prep behavior.

**Execution point**: after Step 5's canonical source validation, **before** the
parent todo proposal is passed to `promote` — the parent file's `## Depends on`
is composed here. (The 5.5 number is for citation; the step is part of
the promotion path in both modes.)

1. **Cache check.** Pipe the exact proposed parent Markdown from memory to stdin and run `FIGMA_SCREEN_TASK_FILE=- node orchestrator/figma/scripts/check-screen-cache.mjs <stem> --gate`. The proposed `## Design` section must already be final and must remain byte-identical through the later parent augmentation/promotion. Do not point the gate at the current backlog/pending file: it may not contain the would-be todo design bullets, and validating it would certify stale input. Do not stage a proposal file; `-` is the supported stdin contract. This validates every non-`none` `## Design` bullet against `orchestrator/.cache/figma/screens/<stem>/`, including task-level `index.json`, node-id/url freshness, required spec/instances/context/png artifacts, and dual/dark theme files (contract: `screensPrompt` in `orchestrator/site/scripts/figma-actions.js`). If it exits non-zero, read `orchestrator/.cache/figma/reports/screen-cache-<stem>.json`; release the lock and return:
   > `BLOCKED: screen design cache missing for <screens>. Press "Pull Figma screens" on the task card (figma:screens:<stem> session), or run the figma:screens pull prompt manually (screensPrompt in orchestrator/site/scripts/figma-actions.js — the screen-cache authoring contract), then re-run task-prep on the same stem.`
   Nothing has been written; the task stays in its current column.
2. **Census.** Run `node orchestrator/figma/scripts/component-census.mjs <stem>`. It classifies each screen's component instances by their OWNING design identity (the component-set / standalone-main node id — display names are labels; two same-named sets are two rows) against the published Design Component Inventory and the Component Mapping Registry (`orchestrator/figma/component-mappings.json`) and prints one status per identity — `MAPPED` / `MISSING` / `INCOMPLETE` / `AMBIGUOUS` / `UNSUPPORTED` / `RETIRED` / `SOURCE_STALE` — plus a transient machine copy at `orchestrator/.cache/figma/reports/census-<stem>.json`. It also upserts every `MAPPED` row into the task's `screens/<stem>/bindings.json` `components[]` (schemaVersion 2, keyed by `designComponentId`); builders and task-prep never author those rows themselves.
3. **AMBIGUOUS / RETIRED / SOURCE_STALE / UNSUPPORTED → owner remedy, never split.** These statuses are registry/inventory conditions only the owner can resolve — splitting would build code against a broken truth. In the normal direct flow, abort the promotion and go to Step 4 (see [`prep-flow.md`](prep-flow.md)): one question per affected identity, naming the remedy. Under `BOARD PREP POLICY: NO QUESTIONS.`, return the same identity and remedy as a typed actionable blocker instead; do not ask —
   - `AMBIGUOUS`: two or more ACTIVE mappings claim one design identity (the census row's `candidates` lists the `cmap-` mapping ids); the owner retires/edits mappings in the site's Mapping Review (Design → Components) until exactly one remains.
   - `RETIRED`: the screens still use an identity whose only mapping is retired; the owner either re-registers a mapping through Mapping Review or the design moves off the component.
   - `SOURCE_STALE`: the screens cache cites an identity absent from the current design inventory — re-pull the screens, or re-sync components (`figma:sync-components`).
   - `UNSUPPORTED`: the identity is captured but the inventory records it as unsupported (unknown provider property types); it cannot be spec-verified — the owner decides how to proceed.
   The census re-runs on the next Mode B pass.
3a. **MISSING with `codeCandidates` → owner pick (reuse or create), never auto-split.** A census row can be
   `MISSING` (no active mapping) yet carry `codeCandidates` — unmapped top-level `@Composable`
   functions in the product's code whose normalized name exactly matches the design component (the row
   is also listed in the report's `reuseCandidates[]`). Splitting here would breed a near-duplicate of
   code that already exists, so this is a human pick. In the normal direct flow,
   abort the promotion and go to Step 4: one
   question per such component, `**Type**: choice`, options =
   `reuse <name> — <file>:<line>` (one per code candidate) + `create — build a new design-system component`.
   Under `BOARD PREP POLICY: NO QUESTIONS.`, return a typed blocker listing the
   candidates and the exact Mapping Review action instead; never choose a
   candidate, create a duplicate, or publish a question.
   On the next Mode B pass:
   - **`reuse` answer** → the pick is registered in the Component Mapping Registry through the site's
     Mapping Review (Design → Components): an `upsert-mapping` operation binding the row's
     `designComponentId` to the picked composable's project component identity
     (`<adapterId>:symbol:<fqName>`), CAS-guarded on `POST /api/design/component-mappings`. Task-prep
     itself never writes `orchestrator/figma/component-mappings.json` — mapping mutations are
     owner-reviewed registry operations, not prep side effects. Re-run the census after the mapping
     lands: the row must now read `MAPPED`; no split.
   - **`create` answer** → fall through to step 4's split exactly as for a plain `MISSING`.
4. **MISSING (with no pending reuse pick — see 3a) / INCOMPLETE → split.** For `INCOMPLETE`, read the row's `detail` FIRST: reasons naming a stale supply truth (`no published analysis`, `adapter scope moved`, a registry bound to another design scope) mean the published project analysis — not the widget — is missing; the remedy is the owner's local component compare (Design → Components, `POST /api/design/component-compare`) and a census re-run. Ask instead of splitting in the normal direct flow; under `BOARD PREP POLICY: NO QUESTIONS.`, return that exact compare-and-rerun remedy as a typed non-question blocker. Split only a genuinely absent/unreadable implementation. Process one child at a time. Task-prep must never calculate `max + i`, claim `.taskno` itself, or create an absent task directly in `todo/`. For each component:
   - Compose the complete todo-ready body below **without its heading or a `## Source` section**. Derive a stable idempotency key `task-prep.split.<sha256(parent-stem + component identity + setNodeId)>`, derive `intentId = intent-<sha256(key)>`, and obtain the exact Source object from `require('./orchestrator/tasks/task-source-contract.cjs').followUp(parentStem, 'task-split', intentId)`. Submit `{version:1,title:"Component <Widget>",body:<body>,key:<key>,originStem:<parent-stem>,source:<that exact object>}` to `orchestrator/tasks/create-backlog.py`. Source is mandatory at this boundary; the creator validates and injects it but never infers it. Never duplicate or hand-author `## Source` in `body`. This is the canonical `absent→backlog` owner and sole number allocator. Under an inherited site session set `CREATE_BACKLOG_PARENT_STEM=<parent-stem>` and preserve the site-provided `ORCHESTRATOR_WRITER_SESSION_ID`, `ORCHESTRATOR_WRITER_LEASE_ID`, and private `ORCHESTRATOR_WRITER_DELEGATION_TOKEN` without printing or copying them; the creator verifies the exact receipt and that its process is inside the recorded site-child ancestry. Under either an exact caller-owned standby lease or a prep-owned direct lease pass all three private receipt fields as `CREATE_BACKLOG_PARENT_WRITER_LEASE_ID`, `CREATE_BACKLOG_PARENT_WRITER_LEASE_TOKEN`, and `CREATE_BACKLOG_PARENT_WRITER_SESSION_ID`, plus `CREATE_BACKLOG_PARENT_STEM`. The creator does **not** acquire another task session: it publishes a short-lived global `task:create-backlog` publication guard, admits only that guard plus the exact parent receipt, and ownership-safely releases the guard after task + INDEX verification. This closes the allocator/INDEX race with a writer for another stem while preserving the one-parent-session rule. Never set the test-only unleased flag. Require the exact `READY` + one JSON result, `ok:true`, and `column:"backlog"`; retain the returned stem/number. A retry reuses the same key, Source object, and receipt rather than allocating another child.
   - Validate the returned child with `validate-task-state.mjs --stem <child> --expect backlog --check-index --json --caller task-prep` and retain its exact `sourceRevision`. Acquire a canonical **child task lock** with `node orchestrator/tasks/task-lock.mjs acquire --stem <child> --stage task-prep --run-id <unique-child-run-id> --session-id <parent-session-id> --owner-kind agent --owner-id task-prep:figma-split:<parent-stem>`. This is runtime ownership for the child mutation, not a second writer lease: the `sessionId` must be the already-verified parent session. Retain the returned `runId`, `sessionId`, and `lockHash` receipt exactly.
   - Read the child's exact backlog bytes into memory, preserve its canonical `## Source` block byte-for-byte, then pipe the complete todo proposal to `node orchestrator/tasks/transition-task-state.mjs promote --stem <child> --authority-stem <parent-stem> --input - --source-revision <exact-child-sourceRevision>` under the same parent writer authority and close stdin. Do not create an intermediate proposal file. The helper admits delegation only for `promote`, only from canonical `backlog`, only when the child's frozen `## Origin` is exactly `- split from <parent-stem>`, Source is Follow-up/task-split with the same parent Ref, and the child's exact `task-prep` lock generation is owned by the parent session. It freezes that authority/lock proof in the recovery marker and rechecks both around every mutation. Require the matching v1 todo receipt before starting the next child.
   - After a successful promotion, release only the exact child lock receipt with `node orchestrator/tasks/task-lock.mjs release --stem <child> --run-id <receipt.runId> --session-id <receipt.sessionId> --expected-hash <receipt.lockHash> --expected-state <promotion.state> --source-revision <promotion.sourceRevision>`. If release was interrupted after detach, run the idempotent `recover-release` command with the same lock tuple plus a fresh green todo-state `state`/`sourceRevision`; never delete a canonical or `.release-*` path by hand. If promotion reports transition recovery required, keep the child lock, run `transition-task-state.mjs recover --stem <child> --authority-stem <parent-stem>` under the same parent authority, and release the lock only after the recovery receipt. On any ordinary pre-mutation failure, obtain a fresh green child state + INDEX receipt and exact-release with that receipt's state/revision before returning the error.
   - This is an explicit `absent→backlog→todo` chain; there is no `absent→todo` exception. If a later child or the parent fails, keep every already-created child in its valid reported column and surface the creation, lock, promotion/recovery, and release receipts for retry/reconciliation — never hand-delete them.
   - The resulting helper-published `orchestrator/tasks/todo/<returned-stem>.md` content is **prep-generated** (a deliberate, bounded exception to "prep only reshapes user input"):
     - `## Goal` — build/refine the `:design-system:components` `<Widget>` widget from its Figma component (the Design Component Inventory entry's variant/property/slot spec — sparse declared variant tuples, never a Cartesian expansion).
     - `## Inputs` — the sample INSTANCE's node URL, read from the census row's `sourceNodeUrl` field (a pullable deep link into the screen frame; the component-SET id is the row's `setNodeId` — two different node identities, never conflate them; when only the id is present, cite BOTH `designComponentId: <census row's designComponentId>` and `figmaNodeId: <setNodeId>` as field lines so the ship-done UI-by-code backstop can anchor on the snapshot — a lone `figmaNodeId:` deliberately does not trigger it); every census row carries `setNodeId` regardless of mapping state, so a `MISSING` component (no active mapping) still has a node anchor to cite. Do NOT derive the URL from a mapping — the census row is the source.
     - `## Design` — a `- <Widget> [component] — <census row's sourceNodeUrl>` bullet (the `sourceNodeUrl` is already a pullable Figma node URL — use it verbatim). This puts the split child **on the pixel-comparison track**: Step 1b / Step 4.6b / the final evidence bundle / `ship-done` all gate on a pullable `## Design` bullet, so a machine-minted component with its node only in `## Inputs` would self-classify **non-UI** and ship uncompared — the exact hole this bullet closes. **Id-only fallback:** when the census row carries no `sourceNodeUrl`, a pullable bullet cannot be formed — omit the `## Design` section and write BOTH snapshot field lines into `## Inputs`: `designComponentId: <census row's designComponentId>` AND `figmaNodeId: <setNodeId>` — as BULLET-LESS lines, no `- ` prefix (the backstop anchors on `^[ \t]*designComponentId:` at line start; a `- designComponentId: …` bullet silently never matches). The snapshot tier requires `designComponentId:` + a machine anchor (`figmaNodeId:` or `frozenStructuralHash:`); a lone `figmaNodeId:` deliberately does NOT fire, to avoid false-blocking docs that merely illustrate the field. With both present the `ship-done`/`verify-done` UI-by-code backstop **blocks** the child until a real node URL is supplied, rather than letting it ship uncompared. Never write a bare `- <Widget> — none`; a component built from a Figma node is not design-less. **Structural tier:** independent of the snapshot, a newly CREATED UI-widget source file (a `.kt`/`.swift` at any depth under a `components/` dir, or a screen/dialog file; read from `### Files touched`) with no pullable `## Design` bullet is BLOCKED unless a **per-widget** audited `- <Name> — none (<why>)` naming that widget accounts for it — this closes the gap where a hand-authored feature-local card (not in the design system, node cited nowhere) shipped uncompared. The created class is fail-closed (an absent/paraphrased status word is treated as created, not modified), and the opt-out is per-widget (one unrelated `— none` does not disarm a co-resident card). A MODIFIED such file is an advisory WARN, task-globally opt-out-able (non-visual-edit tolerance).
     - `## Acceptance` → `### Automated` — the widget file under `design-system/components/`, the `AppColor.<widget>.*` / `AppDp.<widget>.*` slots the design component's token refs name, a **Roborazzi capture `@Test`** for the widget (one per `## Design` bullet/state, wrapped in `PreviewContainerComponent` per the validation-gates screenshot-fidelity-gate §2 — this is what the screenshot gate compares; the `@AppPreview` alone does NOT satisfy it), the standard build-gate line, plus the canonical Figma acceptance anchors:
       `- Spec: <Widget> matches its Figma node values (<sourceNodeUrl>) — \`figma-spec-validator\` green (Minors allowed).`
       and
       `- Screenshot: <Widget> (primary state) has complete Figma oracle/capture evidence and matches its Figma oracle(s) under the project \`screenshotPixelGate\` policy — \`figma-screenshot-validator\` passes.`
       When only the set node id is available, cite `designComponentId: <census row's designComponentId>` + `figmaNodeId: <setNodeId>` in `## Inputs` (both as bullet-less field lines, no `- ` prefix — the snapshot tier needs the pair anchored at line start) and let `ship-done`/`verify-done` block until a pullable node URL is supplied. The raw commands (`compare-screen-spec.mjs`, `compare-screenshots.mjs`) are implementation notes for the validators, not the acceptance bullet shape. Mapping registration is NOT part of the child: a split child is a generic task and generic tasks never auto-map (finalization's `components` phase publishes a mapping only for a server-created design-origin component task carrying frozen binding evidence); after the widget lands, the design↔code mapping is registered through the site's Mapping Review, after which the census reads `MAPPED`.
     - `## Out of scope` — the five defensive bullets + `no caller migration — the consuming screen lands in the parent task`.
     - `## Origin` — supplied as `originStem: <parent-stem>` to the deterministic creator, which writes `- split from <parent-stem>` and verifies `INDEX.splitFrom`. The parent's canonical validator precondition already guarantees a valid stem; do not append lineage by hand.
   - Do **not** touch `orchestrator/figma/component-mappings.json` — every mapping mutation is a CAS-guarded Mapping Review or finalization operation; the census itself is the demand-side coverage check.
5. **Chain the parent.** The parent todo proposal's `## Depends on` lists every
validated component stem returned in (4). Then continue Step 5's transactional
parent promotion.
6. **Verified publications, no manual regen.** Each child creator, child promotion, and final parent promotion independently publishes and verifies fresh INDEX state. Do not run `regen-index.py` between or after them. In the Step-8 promoted block, list every component file under **Files touched**, include the durable creation/promotion receipts, and add a `**Split:**` line naming the dependency chain.

**Splitting policy**: only `MISSING`/`INCOMPLETE` may split — a `MISSING` that carries
`codeCandidates` requires a reuse-or-create owner pick FIRST (step 3a) and splits only on an explicit `create`, and an
`INCOMPLETE` whose `detail` names a stale supply truth requires the owner remedy instead (step 4); `MAPPED` never spawns
tasks, and `AMBIGUOUS`/`RETIRED`/`SOURCE_STALE`/`UNSUPPORTED` are owner remedies (step 3), never
splits. Every instance must carry its owning component-set id; a cache without that identity fails
before census classification. Under Board Prepare those owner picks/remedies are
typed blockers, never pending questions. Multi-screen tasks are NOT auto-split
per screen.

---

## Step 5.5a — `## Preview states` generation (conditional)

**Preview-state gate**: `figmaEnabled: true` in `project-config.md` AND at least one non-`none`
screen-kind `## Design` bullet (`[screen]` or an untagged bullet, because `[screen]` is the default).
Dialog/component/overlay-only tasks still get the cache/spec/screenshot gates, but they do not get a
`## Preview states` section — that section is only screen preview breadth.

When active, after the census split (step 5), append a `## Preview states` section to the todo file with
one bullet per screen. The primary state is the first one listed (compared against the Figma oracle);
additional states are drawn from the screen's known MVI State sealed class — read the existing
`<F><S>State.kt` if the screen exists, otherwise default to `loaded, empty`:

```markdown
## Preview states
- <ScreenName> — loaded (primary, compared vs oracle), empty, loading
```

This section is **advisory** — it signals to `screen-builder` which states to cover with `@AppPreview`
functions and which primary screen state to wire into `ScreenshotTest.kt`. Missing states become Minor
findings in `figma-screenshot-validator`, not Blockers. If `figmaEnabled: false` or the task has no
non-`none` screen-kind `## Design` bullet, omit the section entirely (do not write it — it clutters the
task file when this screen-only hint cannot apply).

**A state the design provides a FRAME for is a `## Design` bullet, not just a `## Preview states` entry.**
`## Preview states` covers preview *breadth* (states with no Figma frame — advisory, Minor if missed). But
when the design ships a distinct loaded / empty / error / loading **frame** (its own node URL), declare that
state as its own `## Design` bullet with a distinct screen name (e.g. `- HomeEmpty — <url>`): it then gets
its own oracle + capture `@Test` and is pixel-compared like any screen — multi-state capture is the gate's
DEFAULT (screenshot-fidelity gate §2.1). Do NOT invent a `## Design` bullet for a state the design does not
provide a frame for — there is nothing to compare, and you never fabricate an oracle.

Separately, for each non-`none` `## Design` node of any kind (`screen|dialog|component|overlay`), add
one **screenshot-gated** acceptance bullet under `### Automated` in the task file:

```markdown
- Screenshot: <NodeName> (primary state) has complete Figma oracle/capture evidence and matches its Figma oracle(s) under the project `screenshotPixelGate` policy — `figma-screenshot-validator` passes.
```

`<NodeName>` MUST be the same name as the `## Design` bullet, the oracle(s)
(`<NodeName>.png` / `.dark.png`), and the capture(s) (`<NodeName>Screenshot.png` / `.dark.png`). For a
screen this is the screen **composable** name (`<F><S>Screen`, e.g. `HomeScreen`). For a dialog,
component, or overlay it is the isolated capture base name. The gate matches it **exactly**; for a
declared design node, a name mismatch that leaves no capture is a **BLOCKER** (`MISSING_CAPTURE`), not
a silent skip — the names must line up so the comparison runs.

**Name charset**: a pullable `<NodeName>` must stay inside `[A-Za-z0-9_]` — the capture
filename derives from it, and a Kotlin test cannot produce parens/spaces/` — `
(`Speed Test (Verified)` is the production `MISSING_CAPTURE` class). The parser flags a
pullable bullet outside that charset as `RISKY_SCREEN_NAME` and the cache gate blocks it at
prep time; rename the frame IN THE BULLET (PascalCase) instead of carrying the Figma display
name verbatim. `none` bullets are exempt (no capture ever derives from them).

This bullet is screenshot-gated — `acceptance-tracer` defers it to `figma-screenshot-validator` (Step
4.6b), exactly as `spec-gated` bullets defer to `figma-spec-validator`. Completeness and anti-forgery
findings always block; pixel-similarity divergence follows `screenshotPixelGate` (`strict` blocks,
`advisory` records WARN, `off` suppresses the similarity verdict).
