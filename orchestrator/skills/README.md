# orchestrator/skills — the architecture rules + the agent workforce, as skills

## What this is — the skills-only runtime

These **11 self-contained skills** here (plus `implement-figma` at
`orchestrator/figma/skill/`) carry all the architecture rules and the operational
workflow — they are the sole runtime surface. The runtime invokes the installed
skills directly. The live config is
`orchestrator/project-config.md`; lint is `orchestrator/lint.sh` (9 checks); the
gate layer is `checks/` (`run-all.sh`, 14 gates). Deploy with
`install-skills.sh <root>` → `.claude/skills/`, `.claude/contracts/`,
`.claude/commands/serve-queue.md`, and `.claude/launch.json`. Frozen output
schemas the skills cite live in `orchestrator/contracts/`.

The 11 self-contained skills here (+ `implement-figma` at `figma/skill/`) are the
template's runtime architecture surface. The `_index/` manifests and the `checks/`
gate suite keep them coherent and deploy-ready.

## Layout

```
<skill>/SKILL.md + references/**   each of the 11 self-contained skills (rules + workflow)
install-skills.sh                  deploy: installs the 11 skills plus every manifest-owned .claude file
_index/                            manifests + measurement surface
  install-manifest.json            the deployable skill roster and hashed installed-file surface
  validate-install-manifest.py     fail-closed path/hash/inventory check run before installer mutations
  install-surfaces/                canonical queue command + launch configuration
  capabilities.json                operation → skill entrypoint (thin adapter)
  docs-map.json                    normative doc surfaces the gates check
  prompt-surfaces.json             executable prompt factories the site dispatches
  payload-schemas/                 task-op JSON schemas (payload-schemas gate)
  known-gaps.md / trigger-collision.md   reviewer registers
  _generate_install_manifest.py    regenerate install-manifest.json
checks/                            consistency and readiness gates
  run-all.sh                       run the complete gate suite
  links.sh / check_links.py        repo-wide dead relative-link check
  skeleton.sh / reference-hash.sh  SKILL.md shape + reference-hash integrity
  install-sync.sh / capabilities-complete.sh   deploy + capability-closure gates
  payload-schemas.sh / harness-fidelity.sh / trigger-fixtures.sh   contract gates
  self-contained-content.sh        asserts that skills read only their own reference packs
  wiring.sh                         capability, prompt-routing, and installation closure
  docs-map.sh / prompt-surfaces.sh / runtime-readiness.sh   manifest + readiness gates
  doc-counts.sh                    re-derives hard-coded documentation counts from disk
```

The `run-all.sh` gate table is the source of truth for the 14-gate roster.

## Run

```bash
bash orchestrator/skills/checks/run-all.sh
```
