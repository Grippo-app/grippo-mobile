# Grippo AI development ecosystem

Human entry point. Nothing here is a rule; the rules live where the links point.

## Layout

```
CLAUDE.md                 map: definitions, module map, policies, roster, skills index (always loaded)
AGENTS.md                 pointer for other tools
.claude/
  README.md               this overview
  skills/<skill>/         SKILL.md (rule owner) + references/ (long form)
  agents/<agent>.md       subagent definitions: rights, duties, preloaded skills, report format
  hooks/bash-guard.py     PreToolUse guard for agents' Bash: readonly | readonly-build | no-git-write
  hooks/check-rulebook.py validates links, anchors, frontmatter and the roster
  settings.local.json     personal permissions (not shared)
analytics/COVERAGE.md     cross-feature analytics contract + index; analytics/<feature>/COVERAGE.md per feature
design-system/CONSISTENCY.md  role-to-token choices
docs/records/             dated decision/review records, written on request
```

## How a task flows

1. **Clarify** - `task-prep`: scope, acceptance criteria, questions as a quiz,
   slices ordered by real dependencies.
2. **Execute** - `task-orchestrator`: implement slice by slice (yourself or
   through `ui-/data-/design-/platform-implementer`), self-review, then
   independent review by `code-reviewer`, `design-reviewer` and `gate-auditor`
   as the change requires; `scout` for inventories, `backend-contract-auditor`
   before wire changes, `analytics-admin-auditor` for live analytics admin work.
3. **Verify** - `build-verifier` runs the scoped Gradle targets; `runtime-verifier`
   runs the app on an emulator/simulator when the user allows it
   (`runtime-verification` skill).
4. **Complete** - `validation-gates`: definition of done, final reply in Russian,
   records updated, proposed commit message (`delivery`); nobody commits unless asked.

Bugs follow `bug-investigation`; rule changes follow `rulebook-maintenance`.

## Working on the rulebook

```bash
python3 -I .claude/hooks/check-rulebook.py     # zero problems before reporting a rule change
zsh .claude/hooks/bash-guard-tests.sh          # guard matrix after touching bash-guard.py (main session only; read-only agents cannot run zsh)
```

Agents are loaded at session start: after adding or changing a file in
`.claude/agents/`, start a new session before invoking it.
