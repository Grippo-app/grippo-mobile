# Grippo project instructions

This file is a pointer for tools other than Claude Code. Everything it points to
is canonical; it adds no rules of its own.

- Read [CLAUDE.md](CLAUDE.md) first: project map, definitions (authorized scope,
  behavior change, visual change, independent review, permitted check, record),
  dependency rules, locked decisions, verification policy, definition of done,
  records and delivery policy, agent roster and the skills index.
- Rules live in `.claude/skills/<skill>/SKILL.md` and their `references/`; agent
  definitions in `.claude/agents/`; the Bash guard and the rulebook checker in
  `.claude/hooks/`. Do not create parallel copies of any of them.
- Every behavior change applies
  [feature-analytics](.claude/skills/feature-analytics/SKILL.md) and
  [state-preservation](.claude/skills/state-preservation/SKILL.md); every visual
  change applies [brand-design](.claude/skills/brand-design/SKILL.md) and
  [design-system](.claude/skills/design-system/SKILL.md); every completion applies
  [validation-gates](.claude/skills/validation-gates/SKILL.md).
- No automated tests, linters or screenshot tooling are added without an explicit
  user request. Nobody commits on the user's behalf without an explicit request
  ([delivery](.claude/skills/delivery/SKILL.md)).
