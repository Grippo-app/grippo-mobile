# System Requirements — KMP Mobile Project

Architecture and conventions for bootstrapping a product-agnostic Kotlin Multiplatform mobile project (Android + iOS, Compose Multiplatform, Decompose, Koin, Ktor, Room).

Everything is driven from a static site — Setup, Launch Wizard, skill install, task board. Open it and follow the steps.

The project root owns the single private npm workspace: `package.json`,
`package-lock.json`, `.npmrc`, and `.nvmrc`. Install dependencies only from the
project root; workspace packages do not carry competing lockfiles.

## What lives where

```
orchestrator/
├── .nvmrc              ← Node 22 pin carried with copied templates and CI
├── README.md           ← you are here (end-user entry)
├── project-config.md   ← the control panel: project identity + pipeline gates (every skill reads it)
├── launch.md           ← the one-time bootstrap prompt (Steps 0–14), long-form
├── lint.sh             ← mechanical drift check for tasks/site/sidecars/arch-map
├── skills/             ← THE WORKFORCE — 11 self-contained skills the AI invokes (see below)
├── tasks/              ← THE WORK QUEUE — backlog/ pending/ todo/ done/ + INDEX.json
├── site/               ← THE CONTROL SURFACE — Node dev server + vanilla-JS SPA
├── figma/              ← THE FIGMA SIDECAR — token/registry pipeline + the implement-figma skill
├── api-contract/       ← THE BACKEND-CONTRACT SIDECAR — OpenAPI/Postman snapshot + drift tooling
├── contracts/          ← frozen capability/output contracts the skills cite + the live outcome-shape.json
└── template-sync/      ← THE DRIFT SENSOR — vendored-copy integrity manifest + checker + manual sync
```

## Run the site

The control surface runs under the repository's Node 22 runtime. Task-state
helpers require Python 3.10 or newer; CI pins the minimum supported Python 3.10
runtime. The Site server itself has zero npm dependencies; its Figma and
Backend children use their sidecar packages. From the project root (the parent
of `orchestrator/`):

```bash
nvm use "$(cat orchestrator/.nvmrc)"
npm ci
npm start
```

Then open <http://localhost:8000/site/>. Override the port with
`PORT=8001 npm start`. See `orchestrator/site/README.md` for details.

Use `npm test` (an alias for `npm run verify:fast`) during development and
`npm run verify:full` before handoff. Focused root commands are `test:syntax`,
`test:site`, `test:api`, `test:app-run`, `test:tooling`, `test:tasks`,
`test:figma`, and `test:crash-recovery`.

Prefer raw markdown? `orchestrator/project-config.md` is the control panel; `orchestrator/launch.md` is the bootstrap wizard in long form; each skill under `orchestrator/skills/<name>/` carries its own normative rules in `references/**` and a `SKILL.md` entrypoint.

## Skills — install before first use

The architecture rules and the agent workforce are packaged as **11 self-contained skills** under `orchestrator/skills/<name>/` (plus `implement-figma`, which lives at `orchestrator/figma/skill/`). They must land in `.claude/skills/` before Claude Code can invoke them. From the project root:

```bash
bash orchestrator/skills/install-skills.sh "$(pwd)"
```

This installs the **11 core skills** into `.claude/skills/<name>/`, the frozen
contracts into `.claude/contracts/`, and the manifest-owned
`.claude/commands/serve-queue.md` + `.claude/launch.json` files. The 12th skill,
`implement-figma`, lives in the Figma sidecar and is **not** installed by this
script — it is deployed separately (copy/symlink
`orchestrator/figma/skill/SKILL.md` → `.claude/skills/implement-figma/`, as
launch Step 6.5 does). After running the script, `install-sync.sh` verifies the
complete deployed surface (11 skills, all references/contracts, command, and
launch file); there are **12** skills only after `implement-figma` is added.

## Task workflow

Tasks flow through `orchestrator/tasks/` as:

```text
backlog -> task-prep -> pending -> task-prep -> todo -> task-orchestrator -> done
```

See `orchestrator/tasks/README.md` for the file shapes, the `task-prep` skill for how a backlog item becomes a structured todo, and the `task-orchestrator` skill for the execution loop.
