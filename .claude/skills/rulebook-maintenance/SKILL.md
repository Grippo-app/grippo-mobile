---
name: rulebook-maintenance
description: Maintain Grippo's AI rulebook - CLAUDE.md, AGENTS.md, .claude/skills, .claude/agents, .claude/hooks and the records policy. Use when adding, correcting, moving or retiring a rule, skill, agent or hook, or when a reviewer finds a contradiction, stale fact or broken link. Owns the ownership map, document schemas, writing rules and the checker.
---

# Rulebook maintenance

The rulebook is a product with one audience: an agent executing a task under
time pressure. Every edit must leave it more decidable, not longer.

## Ownership map

| Topic | Owner |
|---|---|
| Project map, definitions, stack, module map, dependency rules, locked decisions, scope discipline, verification policy, definition of done, records, delivery pointer, roster, skills index | `CLAUDE.md` |
| Entry point for Codex and other tools | `AGENTS.md` |
| Human entry point to the ecosystem | `.claude/README.md` |
| Task clarification and ordering | `task-prep/SKILL.md` |
| Execution loop, delegation, independent review, recurring findings | `task-orchestrator/SKILL.md` |
| Claude Code / Codex role launch, capability mapping and protection | `task-orchestrator/references/agent-runtimes.md` |
| Initial user changes, task delta, concurrent writes and error attribution | `task-orchestrator/references/change-attribution.md` |
| Cross-cut review, final response format | `validation-gates/SKILL.md` |
| Forbidden patterns | `validation-gates/references/forbidden-patterns.md` |
| Kotlin style, naming, packages, ViewModel layout (constructor order, `init`, handlers, `update`, named arguments) | `validation-gates/references/code-style.md` |
| Scope questions | `validation-gates/references/when-to-stop-and-ask.md` |
| Compose signatures, structure, modifier use, stability contracts, memoization, presentation effect lifetimes, hot paths, interaction semantics, full component audits | `compose-components/SKILL.md` + references |
| MVI, base classes, feature composition, navigation, dialogs, results, UI error pipeline, UI state, process death | `ui-feature/references/*.md` |
| Feature/Repository/UseCase, operation outcomes and cache, Room, DataStore, backend client, auth session | `data-layer/references/*.md` |
| Backend evidence gate | `backend-contract-client/SKILL.md` |
| Mapper directions and null guards | `mappers/SKILL.md` + references |
| Koin bindings and composition | `di-modules/SKILL.md` + references |
| Tokens, components, resources, previews, visual checks and evidence | `design-system/references/*.md` (`visual-consistency.md` owns visual evidence) |
| Visual intent, usage/state analysis, design acceptance | `brand-design/references/*.md` |
| Decorative product artwork | `illustrations/SKILL.md` |
| Equipment identifiers, catalog integration, native diagrams and backend task handoff | `add-equipment/SKILL.md` + references (`naming.md` owns the cross-layer identifier mapping) |
| Analytics API, observation boundary, screen coverage gate, admin sync, consent | `feature-analytics/SKILL.md` + references |
| Drafts and restoration | `state-preservation/SKILL.md` |
| Conventions, catalog, new modules, toolkit, iOS framework, secrets, release | `platform-build-toolkit/references/*.md` |
| Build/run/inspect the app | `runtime-verification/SKILL.md` |
| Bug workflow | `bug-investigation/SKILL.md` |
| Git policy | `delivery/SKILL.md` |
| This map, document schemas, checker | `rulebook-maintenance/SKILL.md` |

A topic appears once. If a new rule fits no row, add a row and the owner before
writing the rule anywhere else.

## Document schemas

- Frontmatter descriptions are nonempty YAML strings; quote punctuation or
  implicit values such as numbers when they are intended as text. Simple trailing
  YAML comments are supported by the checker.
- `CLAUDE.md` is loaded into every session. It stays a map (at most 400 lines):
  definitions, tables, pointers. No API listings, no code excerpts longer than a
  line, no second statement of a rule that an owner holds. The checker fails
  above that size.
- `.claude/skills/<name>/SKILL.md` frontmatter: `name` equals the directory name;
  `description` says what the skill owns and when to apply it, in one or two
  sentences that start with a verb ("Implement...", "Review...", "Run..."). Optional
  fields only when needed: `disable-model-invocation: true` for manual-only
  skills, `user-invocable: false` for internal ones. Body: purpose, when to use,
  workflow, completion gate, references list. References hold the long form.
- `.claude/agents/<name>.md` frontmatter:
  - `name` equals the file stem.
  - `description` starts with the role and names the trigger ("Use proactively
    after..." or "Use when...").
  - `model`: `inherit` for judgment, `sonnet` for mechanical work in Claude;
    other runtimes use the [launch adapter](../task-orchestrator/references/agent-runtimes.md).
  - `tools`: an allowlist. `disallowedTools`: `mcp__*` for agents that need no
    MCP tools, and `Agent` always (no nesting).
    Only `analytics-admin-auditor` (browser) and `runtime-verifier` (simulator)
    allow MCP tools; other roles exclude them, including ordinary reviewers.
    Their native provider allowances are named in the role definitions; change
    those definitions and the checker's capability allowlists together.
  - `skills`: a YAML list of nonempty required skill names, not a comma-separated
    scalar. Native loading or explicit
    reading follows the launch adapter; the body names required references.
  - `hooks`: the Bash guard in `readonly` (reviewers, scouts, auditors),
    `readonly-build` (verifiers: builds and device tools, no writes inside the
    repository) or `no-git-write` (implementers, maintainer) mode.
    Bash access requires the canonical `PreToolUse` command hook with matcher
    `Bash`; keep its structure and command explicit so the checker can validate it.
  - Body sections in this order: Role, Rights, Inputs you must receive, Duties,
    Report format.
- `.claude/hooks/bash-guard.py` enforces agent rights for Bash; change its
  allowlists together with the agent definitions that rely on them and re-run
  its test matrix (see the script header).
- Agent loading and fallback follow the launch adapter; client-specific
  registration never changes the role's canonical owner.

## Writing rules

- One owner, pointers elsewhere. A pointer is one sentence plus a link; it never
  paraphrases the rule's conditions.
- Every rule has a decidable trigger. Use the definitions in CLAUDE.md
  (behavior change, visual change, scope expansion, independent review,
  permitted check, record, small editorial change, significant state) instead
  of "where appropriate", "as required", "when necessary".
- State code facts as orientation with a source pointer ("inspect
  `path`; current shape: ...") and verify them by opening the file when you
  write them. Do not paste full signatures that will drift; do name exact
  identifiers an agent must search for.
- Prefer a short rule and a short reason over a MUST stack. Use MUST only
  where violation is a defect; use plain sentences for guidance.
- Rules are written in English; the user-facing final reply is in Russian
  (owned by validation-gates). Do not mix languages inside one document.
- No machine protocols, task-board vocabulary, transient IDs or negations of
  removed systems. Do not say what the rulebook is not; say what it is.
- Examples that use invented APIs say so at the top of the file and never mix
  invented and real routes in one code block.
- Records policy: dated narratives belong in `docs/records/` only on request;
  current state belongs in the record files named in CLAUDE.md; a reply is not a
  record.

## Procedures

### Add or change a skill

1. Confirm the topic's owner in the map. Changing an owner means updating the
   map row, the old owner (now a pointer) and every link to the moved anchor.
2. Write or edit the owner. Keep the completion gate explicit and checkable.
3. Update `CLAUDE.md` skills index and, when ownership or triggers changed,
   the `.claude/README.md` overview.
4. Run the checker and re-read the changed documents end to end.

### Add or change an agent

1. Decide read-only versus write, the owned paths, the preloaded skills and
   the report format. Reuse the Bash guard modes; add a new mode only with a
   test row in the guard's matrix.
2. Write `.claude/agents/<name>.md` per the schema; add the roster row in
   `CLAUDE.md`; mention the agent in `task-orchestrator` only if the delegation
   rules change.
3. Run the checker and verify role loading through the launch adapter available
   in the current client; report native loading separately from explicit-role use.

### Retire a rule, skill or agent

Search every link and mention, replace them with the new owner, delete the
file, run the checker. Never leave a tombstone paragraph explaining the removal.

## Checker

```bash
python3 -I .claude/hooks/check-rulebook.py
```

It validates relative links and anchors, skill and agent identities, required
models/tools/exclusions, required skill names, canonical Bash hook structure and
the role's guard mode, plus the roster in `CLAUDE.md`. Read-only roles must exclude
editing tools; agents without allowed MCP tools must block MCP access, and every
agent must exclude delegation. Zero problems is a precondition for reporting a
rulebook change complete. The checker proves configuration structure, not runtime
enforcement or meaning: verify the launch adapter and re-read for contradictions.

## Completion gate

- Ownership map has one row per topic touched; no second statement remains.
- Every changed fact about the code was verified against the file it describes.
- Triggers use the CLAUDE.md definitions; no new vague quantifier was added.
- Checker reports zero problems; changed documents re-read end to end.
- The reply lists the documents changed, the ownership moves and any
  contradiction left open with its proposed owner.
