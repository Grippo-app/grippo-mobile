# Grippo AI development ecosystem

Human entry point. Nothing here is a rule; the rules live where the links point.

## Layout

```
CLAUDE.md                 map: definitions, module map, policies, roster, skills index (always loaded)
AGENTS.md                 entry point for Codex and other tools
.claude/
  README.md               this overview
  skills/<skill>/         SKILL.md (rule owner) + references/ (long form)
  agents/<agent>.md       shared roles: rights, duties, required skills, report format
  hooks/bash-guard.py     PreToolUse guard for agents' Bash: readonly | readonly-build | no-git-write
  hooks/check-rulebook.py validates links, anchors, frontmatter and the roster
  settings.local.json     personal permissions (not shared)
analytics/COVERAGE.md     cross-feature analytics contract + index; analytics/<feature>/COVERAGE.md per feature
design-system/CONSISTENCY.md  role-to-token choices
docs/records/             dated decision/review records, written on request
```

## How a task flows

1. [Prepare](skills/task-prep/SKILL.md) the scope and dependencies.
2. [Execute and review](skills/task-orchestrator/SKILL.md) the slices.
3. [Verify](skills/runtime-verification/SKILL.md) the authorized checks.
4. [Complete](skills/validation-gates/SKILL.md) and [hand off](skills/delivery/SKILL.md).

[Agent runtimes](skills/task-orchestrator/references/agent-runtimes.md) explains
launching the same roles in Claude Code and Codex.
[Change attribution](skills/task-orchestrator/references/change-attribution.md)
separates the user's initial working state from each task's changes.

Compose implementation and review use [compose-components](skills/compose-components/SKILL.md),
with [design-system](skills/design-system/SKILL.md) and [brand-design](skills/brand-design/SKILL.md)
for visual work. Bugs follow `bug-investigation`; rule changes follow `rulebook-maintenance`.

Equipment additions, native diagrams and backend task prompts follow
[add-equipment](skills/add-equipment/SKILL.md).

## Working on the rulebook

```bash
python3 -I .claude/hooks/check-rulebook.py     # zero problems before reporting a rule change
zsh .claude/hooks/bash-guard-tests.sh          # guard matrix after touching bash-guard.py (main session only; read-only agents cannot run zsh)
```

Role loading and fallback follow agent runtimes above.
