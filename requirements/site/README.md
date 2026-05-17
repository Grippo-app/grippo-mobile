# requirements/site/

## What this is

A single-page static site that turns `requirements/launch.md` and the
sub-agent docs into a step-by-step UI. It walks a developer through
bootstrapping a fresh KMP project from `requirements/` and, once
bootstrap is done, helps them write well-formed task files for the
sub-agent pipeline.

## How to open

```
open requirements/site/index.html
```

Or drag the file into any modern browser. There is no build step — the
site loads under `file://` everywhere (Chrome, Firefox, Safari).

## What it does NOT do

- No filesystem writes — every action is copy-paste; the user runs
  agents in their own terminal / Claude Code session.
- No telemetry, no analytics, no network requests.
- No backend, no dependencies, no CDN, no ES modules.

State (form values, wizard progress, task counter) lives only in
`localStorage` under the key `kmp-wizard-state`. Clearing site data
resets it.

## Panel summary

- **Setup** — collects project values and produces the YAML for
  `requirements/00-overview/03-project-config.md`. Gates the Wizard.
- **Launch Wizard** — Steps 0–14 (plus Step 1.5) with ready-to-paste
  prompts, verify hints, and a sequential "done" checkbox per step.
- **Sub-agents** — one card per builder / validator / helper, install
  commands, and the orchestration flow diagram.
- **Codex Loop** — explainer of the external-review gate, the
  `codexEnabled × installed` matrix, and a radio that updates the
  saved value.
- **Task Form** — produces a valid `TASK_<N>_<title>.md`; gated until
  the wizard is complete.

A floating "What's next" pointer (bottom-right) tracks progress across
panels and links to the next action. Close it to dismiss permanently —
the closed state persists.

## Maintenance

Editing the wizard? Edit files under `requirements/site/scripts/` and
`requirements/site/styles/`. The prompts in `SITE_PROMPTS.md` at the
repo root regenerate the site from scratch if you ever need a clean
rebuild.

### Sync points

The site cannot fetch from disk (it runs under `file://`), so a few
chunks of content are mirrored in JS and must be kept in sync by hand:

- `CONFIG_BODY` in `scripts/panels/setup.js` mirrors the body of
  `requirements/00-overview/03-project-config.md` (everything after the
  YAML frontmatter). The Setup panel's "Download .md" button builds the
  file from `App.helpers.buildYaml(setup) + CONFIG_BODY`. If you edit
  the real `.md` body, update `CONFIG_BODY` too.
- `App.helpers.buildYaml` lives in `scripts/data/wizard-steps.js` and
  is the single source of truth for the YAML frontmatter. All three
  consumers (Setup preview, Setup download, Wizard single-shot prompt)
  call it — do not re-implement.

## Portability

The site lives at `requirements/site/`, so it travels with
`requirements/` automatically when you copy that directory into a new
project — every reference inside the site is relative to
`requirements/site/index.html`.
