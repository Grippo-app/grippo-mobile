---
name: implement-figma
description: Implement a screen or design-system component from a Figma node so the AI REUSES existing :design-system components + AppTokens.* (informed by the real Figma design) instead of redrawing from Box/Row/Column and raw colors/dp. Use when a task carries a Figma node URL and figmaEnabled is true. Source: orchestrator/figma/skill/SKILL.md; installed to .claude/skills/implement-figma/SKILL.md.
---

# Implement from Figma (MCP + component mappings + tokens)

Repeatable workflow for turning a Figma node into idiomatic Compose that reuses the existing design
system. **Permanent repository rules live in `CLAUDE.md`; the figma workflow rules live in this skill (and the design-system skill for tokens/components).** This skill is the *how*.

## Preconditions

- `figmaEnabled: true` in `orchestrator/project-config.md`; otherwise this skill does not apply.
- The task carries a **Figma node URL** with a `node-id`.
- The project MCP is bound (site Figma tab). **Golden invariant:** you never call Figma directly — a
  spawned `figma:` `claude` session does, and writes JSON into the scoped cache subtree
  (`orchestrator/.cache/figma/screens/<stem>/`); you read that + the committed design/component
  artifacts (Design Component Inventory, Component Mapping Registry, the task's `bindings.json`).

## Workflow

1. **Scope.** Parse the node URL; confirm it has a `node-id` and is the intended component-set / screen
   node (not a stray instance). One task = one screen, one component, or one isolated section.
2. **Design context.** Have a `figma:` session fetch `get_metadata` + `get_design_context` +
   `get_screenshot` + `get_variable_defs` for the node into the task's scoped cache folder
   `orchestrator/.cache/figma/screens/<stem>/` (a `[component]` bullet is pulled into the same
   per-task cache — there is no per-widget global cache). Design components are NOT pulled per
   task either: the committed Design Component Inventory (written by the server-run
   `figma:sync-components` scope from a witnessed `capture.json` + `visual/*.png`) is the
   design-side component truth. Token evidence for a task is captured in server-planned sidecars and
   committed to the shared ingestion outbox. Global token truth is the usage-scoped
   `observed-token-catalog` plus its exact source index and effective binding snapshot; readers resolve
   those artifacts through the sealed generation pointer. This capability does not enumerate file-wide
   Variables, stable Variable IDs, collections, modes, or aliases. For large frames, split into
   sections first — do not pull a whole page. The screens `index.json` node MUST record the node's
   `kind` (from the `## Design` bullet's trailing `[screen|dialog|component|overlay]` tag, default
   `screen`) — at pull time `check-screen-cache.mjs` BLOCKS a non-screen node whose index entry
   omits/mismatches its kind (`KIND_MISSING_IN_INDEX` / `KIND_MISMATCH`); separately, at the screenshot
   gate an `[overlay]` with no representable capture is a fail-closed `UNREPRESENTABLE_OVERLAY` BLOCKER
   (`compare-screenshots.mjs`, gate spec §4).
3. **Component-mapping table.** For each Figma node, resolve "→ which `:design-system` component + params"
   from the task's `bindings.json` `components[]` rows (census-derived from the Component Mapping
   Registry `orchestrator/figma/component-mappings.json` + the Design Component Inventory; keyed by
   `designComponentId` — match by the owning component-set node id, never by display name; ask
   `context-finder`: "which Compose component does this Figma node map to?"). Build:
   `Figma node → component → params/variants → confidence`.
4. **No silent approximation.** For anything unmapped, pick a status and report it — do NOT draw a
   look-alike from `Box`/`Row`/`Column`: `MISSING_MAPPING_EXISTING_COMPONENT` / `MISSING_COMPONENT` /
   `AMBIGUOUS_COMPONENT` / `RAW_LAYOUT_INTENTIONAL` / `DECORATIVE_ELEMENT`.
5. **Implement.** Reuse the mapped components + existing scaffold/toolbar/navigation. Screens go through
   `screen-builder` (the seven-file MVI, state via mappers/`stub*()` — never flat scalars into entity
   composables). New/changed DS components go through `design-system-component-builder` (Figma-BLIND —
   it builds to the frozen binding spec / design inventory entry, never calling Figma itself).
   **Capture by construction:** for every `figmaEnabled` node with a pulled `## Design` oracle, also scaffold its
   capture — add `id("screenshot.test.convention")` to the module and write `src/androidHostTest/<screen-package>/ScreenshotTest.kt`
   with one `@Test` per `## Design` bullet, derived mechanically from the screen's `@AppPreview` stub
   (swap `PreviewContainer`→`PreviewContainerScreenshot`, capture file `<oracleBase>Screenshot.png`,
   `@Config(qualifiers="w<W>dp-h<H>dp")` from the oracle's `frameSizeDp`; a `dialog`/`component` bullet renders
   that composable in isolation per its node `kind`; an `[overlay]`/host-drawn `[dialog]` uses the two
   overlay capture shapes — gate spec §3.5, composed-over-host vs popup-only).
   **Multi-state is the default:** when the design provides state frames (loaded/empty/error/loading) as separate
   `## Design` bullets, scaffold one `@Test` per state, each vs ITS OWN oracle (gate spec §2.1) — never invent an
   oracle for a state the design does not declare.
   **Explicit binding:** when you write a screen's implementation file, set its `implFile` and `composable`
   in `orchestrator/.cache/figma/screens/<stem>/bindings.json` (the pull seeded the `screens[]` entry) — the
   spec-fidelity gate then reads it as the screen-map so a multi-screen stem needs no `--screen-map` plumbing.
   A CLI/env screen map may supply the same exact declaration; a conflict is a blocker. Multi-screen stems are never mapped from filenames, and a present malformed bindings file is a hard error. Never wholesale-overwrite the file. **Determinism is a wrapper, not memory:** every capture MUST use
   the `PreviewContainerScreenshot` wrapper (frozen clock, animations off, fake image loader, fixed locale/RTL) and
   avoid the forbidden calls (`*.now()`, unseeded `Random`, `rememberInfiniteTransition`, live `AsyncImage`,
   `Locale.getDefault()`) — gate spec §2.2. This gives the screenshot gate an "actual" to compare —
   without it a declared screen yields `MISSING_CAPTURE` — a BLOCKER that blocks `done/`, not a silent skip. Exact recipe: the gate spec §2
   (`orchestrator/skills/validation-gates/references/screenshot-fidelity-gate.md`).
6. **Tokens, not literals.** Every visual value via `AppTokens.colors.*` / `AppTokens.dp.*` /
   `AppTokens.typography.x()` / `AppTokens.icons.X`. No `Color(0x…)`/`.dp`/`.sp` in feature code; if a
   value has no token, surface the nearest-token candidate (report the literal, the closest existing
   `AppTokens.*` entry, and the delta — never silently snap or hardcode). User-facing text
   → `Res.string.*` / `UiText`, never Figma copy as a key.
7. **Build + verify.** Compile the affected modules (the project-config `androidAssembleTask`,
   default `:androidApp:assembleDebug`; + the `sharedFrameworkTask` XCFramework when
   `iosEnabled`; per-module KMP compiles use the `moduleCompileTask` suffix, default
   `compileAndroidMain` — `compileDebugKotlinAndroid` does not exist on KMP library modules);
   check `@AppPreview`. **Value-level parity is gated**: `figma-spec-validator` compares the
   code's declared values (token-resolved) against the screen's cached value spec
   (the spec-fidelity gate — routed by the task-orchestrator skill's `validator-routing` reference).
   **Rendered-pixel parity is MANDATORY** for every `figmaEnabled` task with a non-`none`
   `## Design` bullet: the screen's oracle is pulled by a spawned `figma:screens:<stem>` / `figma:*`
   session and Roborazzi vs the pulled oracle(s) is gated (adaptive per theme — the
   `figma-screenshot-validator` gate, `--gate` mode, routed by the task-orchestrator skill's
   `validator-routing` reference). A declared screen whose oracle/capture is missing is a BLOCKER, not
   a skip; only a non-UI task skips. The `ship-done`/`verify-done` UI-by-evidence backstop
   hard-blocks a task that cites a Figma node URL or a `designComponentId:`+`figmaNodeId:`/
   `frozenStructuralHash:` snapshot yet omits `## Design`; a screen/dialog FILE edit with no
   node cited only draws its advisory NOTE (the accepted filename-tier residual). This is why Step 5 scaffolds the capture by construction. The capture spec
   (`ScreenshotTest.kt`, density/qualifier, capture↔comparator contract) lives in the
   validation-gates skill: `orchestrator/skills/validation-gates/references/screenshot-fidelity-gate.md`.
8. **Report.** % reused components, any missing-mapping statuses, and any hardcoded value introduced.

## Out of scope

Figma Code Connect / `.figma.ts` (rejected — never adopt). Building neighbouring screens unasked. Adding new
top-level token categories (a `design-system-component-builder` stop-and-ask). Publishing to Figma.

## Output contract

Implementation work returns a builder report conforming to
`orchestrator/contracts/builder-report.md`. When an implementation plan drives
the task, its file list and naming table follow
`orchestrator/contracts/planner-output.md`. Figma validation output is routed
through the existing validator contracts (`orchestrator/contracts/validator-finding.md`
and `orchestrator/contracts/validation-run.md`) plus the per-validator frozen
surfaces under `orchestrator/contracts/agents/figma-*.md`.
