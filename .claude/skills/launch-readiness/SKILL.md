---
name: launch-readiness
description: >-
  Bootstrap a project from scratch — launch a new KMP mobile app, set up the
  project, populate project-config, scaffold the foundation modules, and deploy
  the skill toolkit. Use when starting a fresh project from an empty
  directory, running the launch.md pipeline, asking "how do I set up / launch /
  bootstrap this", checking launch readiness, populating project-config.md
  from the Step-0 answers, installing skills, or regenerating the
  architecture map at install time. This is the from-scratch BOOTSTRAP — not the
  post-bootstrap app-shell / data-service one-shot builders.
---

# Launch readiness

Operational entrypoint for from-scratch bootstrap. This skill owns the
`launch.md` pipeline: it scaffolds an empty directory into a project that builds
green on Android + iOS with one end-to-end feature, then installs the toolkit.
This is the routing map over `launch.md`, not new rules.
The app shells (Steps 3/11) and the data-service scaffold (Step 7) are the
**manual** bootstrap equivalents, NOT the post-bootstrap `app-shell-builder` /
`data-service-scaffold-builder` one-shots.

## When to use

- Starting a new project from an empty directory (run `launch.md` once).
- Populating `orchestrator/project-config.md` from the Step-0 answers.
- Scaffolding the mandatory infrastructure modules and the first feature.
- Verifying end-to-end build readiness on both platforms.
- Installing skills and seeding `.arch-map.json` at Step 14.

Re-running the bootstrap on an existing project is out of scope — ongoing growth
goes through the cookbook recipes in the owning skills’ references + the post-bootstrap builders.

## Required inputs

The Step-0 answers (recorded **durably**, not in agent memory — a bootstrap
spans hours/sessions): product name, organization, backend host, application id,
first product domain, locales, auth methods, Firebase on/off. Defaults for
unasked fields (`iosEnabled`, `prelaunch`, `iosFrameworkName`, `figmaEnabled`,
`backendContractEnabled`, …) are listed under Step 0 in `launch.md`. A concrete
temporary backend host is required — no source TODO. Identity values resolve in
Step 0: a populated `orchestrator/project-config.md` first (the site Setup panel
writes it before any step runs); clarifying questions only in the manual flow with
no populated config. After Step 0 an unknown required value is
`BLOCKED: <field>` — the bootstrap never asks past Step 0.

## Workflow

The frozen Step 0–14 sequence + load-bearing half-steps (see Output contract):

1. **Step 0** — gather context (durably record first-domain + auth).
2. **Step 1** — read the framing references under [`references/`](references/index.md)
   (project vision, architecture overview, config, conventions, glossary), then the
   per-concern builder skills the steps below route to. Don't skim.
3. **Step 1.5** — populate `project-config.md`; no required placeholder may
   remain. Do not proceed until fully populated — every builder/validator skill reads it.
4. **Step 2** (+ 2.5 `.gitignore`) — Gradle wrapper, catalog, `gradle.properties`,
   `settings.gradle.kts`, `build-logic/` convention plugins.
5. **Step 3** — scaffold the 35 foundation modules in one batch.
6. **Step 4** + **4.5** (error contracts) — base classes; `:ui-core:error` + state.
7. **Step 5** — toolkit modules. **Step 6** — design system.
8. **Step 6.5** (Figma sidecar, `figmaEnabled`) · **6.6** (backend snapshot,
   `backendContractEnabled ≠ false`) — sidecars only; never enter the build.
9. **Step 7** (data layer) + **7.5** (dialog-api) + **7.6** (error-provider-impl)
   + **7.7** (error-display render target).
10. **Step 8** — the first feature (one domain, one screen). **Step 9** — `:shared`.
11. **Step 10** — `:androidApp`. **Step 11** — `:iosApp` (verbatim drop-in fences
    from the platform-build-toolkit skill (ios-app-project reference), like the base-class reference impls — no Xcode
    handoff).
12. **Step 12** — verify end-to-end (both-platform assemble + foundation-stub gate).
13. **Step 13** — write `CLAUDE.md`. **Step 14** — install skills,
    seed `.arch-map.json`, run lint, create task-board folders.

Build green at every step; substitute `<org>`/`<product>`/`<Product>`/
`<iosFrameworkName>` from `project-config.md`; don't invent code, don't add
deps, don't modify the skill references at runtime.

## Stop and ask

For the bootstrap, "stop" never means a question: after Step 0 the run is
unattended (launch.md § "Unattended run — never ask") — no mid-run
confirmations, no closing "Want me to…?" offers. What would have been a question
is either resolved by a documented default (recorded in the step report) or
stops the run as a failure:

- A required identity/build value is unknown after Step 0 (host, applicationId,
  product) — `BLOCKED: <field>`; do not leave a source TODO.
- An existing root `CLAUDE.md` (Step 13) — never `rm` or rewrite; keep it
  verbatim, append only the missing bootstrap sections, note it in the report.
- A step's verification fails — fix before advancing; don't accumulate broken
  modules. Fixes needed to turn a step green are in scope: apply, re-verify,
  and list them in the report — never ask whether to apply, commit, or keep them.
- A foundation stub (`TODO(…)`/`NotImplementedError`) survives Step 12's gate —
  the bootstrap fails until it is implemented.
- Required reading file missing → `BLOCKED: required reading missing — {missing_files}`.

## References to read

This skill carries its own framing rules under `references/`; route by
topic via [`references/index.md`](references/index.md). The bootstrap pipeline stays
in `launch.md` (owned here) — cite it, don't duplicate it.

| Task kind | Read first |
|---|---|
| Routing table (full map) | [`references/index.md`](references/index.md) |
| The pipeline itself | `orchestrator/launch.md` (Step 0–14 + half-steps) |
| Frozen step order | `orchestrator/contracts/launch-sequence.md` |
| Vision / principles / non-goals / success | [`references/project-vision.md`](references/project-vision.md) |
| Layers / hard rules / module groups | [`references/architecture-overview.md`](references/architecture-overview.md) |
| Project-config fields + fresh-project state | [`references/project-config.md`](references/project-config.md); Step 0 mapping in `launch.md` |
| Placeholders / canonical examples / conventions | [`references/template-conventions.md`](references/template-conventions.md) |
| Terms / cross-cutting types / reserved names | [`references/glossary.md`](references/glossary.md) |
| Module graph (38 mandatory) | the platform-build-toolkit skill, `references/module-structure.md` |
| Build system / convention plugins | the platform-build-toolkit skill, `references/tech-stack.md` + `references/convention-plugins.md` + `references/gradle-build.md` |
| iOS drop-in project | the platform-build-toolkit skill, `references/ios-app-project.md` |
| Skill install commands | `orchestrator/README.md` § "Skills — install before first use" |

## Validators / gates

- **Step 12 end-to-end build gate** — `:shared:assemble<IosFrameworkName>DebugXCFramework`
  + `:androidApp:assembleDebug` both green (Android-only when `iosEnabled: false`).
- **Foundation-stub scan** — `node orchestrator/site/server/foundation-stub-scan.js`
  over `shared/`, `ui-core/`, `ui-dialog-features/`, `ui-screen-features/` commonMain →
  fail if any real code-position `TODO(…)`/`NotImplementedError` survives (the
  `DialogConfig.ErrorDisplay → createChild` route is the canonical trap). The scan is
  comment/string-aware — trivia mentions don't fail it — and is the same
  implementation behind the site's Step-12 ✓, so the wizard marks the step done on
  its own when it passes; don't re-derive it with raw `rg`.
- **Step 14 install gate** — `ls .claude/skills/` ≥ 11 (the skills
  installed by `install-skills.sh`; `implement-figma` deployed separately); `regen-arch.py`
  then `lint.sh` back-to-back (the arch-map freshness check compares `.arch-map.json` vs `settings.gradle.kts`).

## Output contract

The bootstrap step order is frozen by
[`orchestrator/contracts/launch-sequence.md`](../../contracts/launch-sequence.md)
— the full inventory is `Step 0, 1, 1.5, 2, 2.5, 3, 4, 4.5, 5, 6, 6.5, 6.6, 7, 7.5,
7.6, 7.7, 8, 9, 10, 11, 12, 13, 14` (half-steps normative, not optional). This
skill must preserve that order against the frozen
[`launch-sequence.md`](../../contracts/launch-sequence.md) contract and the
live [`orchestrator/launch.md`](../../../orchestrator/launch.md) sequence (if a heading is removed
or renamed, those two disagree).
This is a pipeline playbook, not a builder envelope — it owns no
`builder-report` agents; the app shells and the data-service scaffold are
produced by the manual Steps 3/11 and Step 7, not by the post-bootstrap builders.
