# Rulebook document schemas

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
  skills, `user-invocable: false` for internal ones; these fields are YAML
  booleans. Body: purpose, when to use, workflow, a named completion/gate/checks
  section (or a pointer there to its owner), references list. References hold
  the long form. Other frontmatter fields require extending this schema and
  the checker together.
- `.claude/agents/<name>.md` frontmatter:
  - `name` equals the file stem.
  - `description` starts with the role and names the trigger ("Use proactively
    after..." or "Use when...").
  - `model`: `inherit` for judgment, `sonnet` for mechanical work in Claude;
    other runtimes use the [launch adapter](../../task-orchestrator/references/agent-runtimes.md).
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
    `readonly-compile` (build-verifier: scoped compilation, no device tools),
    `readonly-build` (runtime-verifier: builds and device tools, no direct writes
    inside the repository) or `no-git-write` (implementers, maintainer) mode.
    Bash access requires the canonical `PreToolUse` command hook with matcher
    `Bash`; keep its structure and command explicit so the checker can validate it.
  - Body: role title and purpose, then Rights, Inputs you must receive, Duties,
    Report format (an optional Severity section may precede Report format).
    `color` is optional UI metadata. Other fields and tool capabilities require
    extending this schema and the checker's allowlists together.
- `.claude/hooks/bash-guard.py` checks Bash commands against the role's mode;
  it supplements the runtime's sandbox and the role's procedural restrictions.
  Change its
  allowlists together with the agent definitions that rely on them and re-run
  its test matrix (see the script header).
- Agent loading and fallback follow the launch adapter; `.agents/skills` links
  and `.codex/agents` TOML are generated projections. Edit canonical metadata
  first, regenerate, then verify discovery/registration separately from runtime
  protection. Client-specific registration never changes the canonical owner.

