# Launch-readiness references — routing table

Self-contained reference pack for the `launch-readiness` skill. These files carry the skill's own
normative framing rules, so the bootstrap reads here at runtime — it reads no external rule docs.

The bootstrap **pipeline** itself is not duplicated here: it stays in `orchestrator/launch.md` (the
canonical Step 0–14 playbook, owned by this skill) with its frozen step order pinned by
`orchestrator/contracts/launch-sequence.md`. Read those two for the sequence; read the files below
for the framing rules each step relies on.

Route by task kind; each row lists the file to read first, then any supporting files.

| Task kind | Read first | Also |
|---|---|---|
| The whole pipeline (run once) | `orchestrator/launch.md` (Step 0–14 + half-steps) | `orchestrator/contracts/launch-sequence.md` |
| Frozen step order | `orchestrator/contracts/launch-sequence.md` | `orchestrator/launch.md` (the live sequence) |
| What the project is / non-goals / principles / success criteria | [`project-vision.md`](project-vision.md) | [`architecture-overview.md`](architecture-overview.md) |
| Layers, dependency direction, hard rules, module groups | [`architecture-overview.md`](architecture-overview.md) | [`project-vision.md`](project-vision.md) |
| Populate `project-config.md`; field meanings + fresh-project state | [`project-config.md`](project-config.md) | Step 0 mapping in `orchestrator/launch.md` |
| Placeholders, slot tokens, canonical example types, worked example, module-list policy | [`template-conventions.md`](template-conventions.md) | [`glossary.md`](glossary.md) |
| Recurring terms, cross-cutting types, reserved names | [`glossary.md`](glossary.md) | [`template-conventions.md`](template-conventions.md) |

## File map

| File | Covers |
|---|---|
| [`project-vision.md`](project-vision.md) | What the architecture describes, non-goals, architectural principles, in/out-of-scope, success criteria |
| [`architecture-overview.md`](architecture-overview.md) | Layers + dependency direction diagram, hard rules, module groups (high level) |
| [`project-config.md`](project-config.md) | Config frontmatter, fresh-project state, single source of truth, identity/build + prelaunch/locales/typeface/DI field meanings, updating |
| [`glossary.md`](glossary.md) | Architecture terms, layering vocabulary, cross-cutting types, reserved-names table |
| [`template-conventions.md`](template-conventions.md) | Illustrative domain, slot placeholders, canonical example types + worked example, format-state policy, module-list policy, generic component names, reserved names |

## Out of this skill's scope (handed off)

- **The pipeline mechanics** (per-step build commands, gates, half-step bodies) → `orchestrator/launch.md`
  itself, pinned by `orchestrator/contracts/launch-sequence.md`. This skill owns that playbook; the
  reference files above only carry the framing rules, not a copy of the steps.
- **`codexEnabled` / `verifyEnabled` review-gate fields** → review skills; see
  [`project-config.md`](project-config.md).
- **Figma fields** (`figmaEnabled`, `figmaLibraryUrl`) and **backend-contract fields**
  (`backendContractEnabled` and `api-contract/environments.json`) → figma / backend-contract skills;
  see [`project-config.md`](project-config.md).
- **Per-concern detail** (MVI seven-file template, Decompose navigation, dialogs, data flow, error
  pipeline, process-death restoration) → `ui-feature` / `data-layer` / `di-modules` skills.
  [`architecture-overview.md`](architecture-overview.md) is the high-level map only.
