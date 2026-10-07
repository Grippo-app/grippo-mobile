# Config gates — `codexEnabled` / `verifyEnabled`

> These two project-config keys select the external-review gate and the
> runtime-verify gate this skill routes.

## `codexEnabled` — external-review gate

Controls the external-review gate. One of:

- `auto` (the committed template and bootstrap default) — orchestrator runs
  `node orchestrator/tasks/reviewer-status.cjs`. When its availability is
  `available`, select the official Codex plugin review command; for
  `unavailable` or `unknown`, select `internal-reviewer` before the attempt
  starts.
- `true` — force the official Codex plugin review command; orchestrator
  hard-fails unless detector availability is exactly `available`.
- `false` — force `internal-reviewer`; skip Codex detection entirely.

A missing, duplicate, placeholder, or invalid `codexEnabled` field is a
configuration error, not an implicit `auto`. Halt before detection and repair
the canonical field through Site Setup.

> Self-review is never a fallback: when the configured reviewer is unavailable the
> orchestrator escalates per this policy — it never reviews its own coordinated
> output. See `forbidden-patterns.md` § Orchestrator scope.

Installed and available are distinct. The shared helper reads active plugin
state only through `claude plugin list --json`, then runs the active official
plugin's bounded, read-only `setup --json` readiness contract. That contract
checks the same CLI/app-server/auth path `/codex:review` will use. The helper
never treats Claude's retained plugin cache as an installation and never runs a
paid/probe review. Once a reviewer is recorded in the attempt's structured
`phase-start`, invocation failure closes that reviewer attempt as failed;
runtime substitution is forbidden.

## `verifyEnabled` — post-validator runtime-verify gate

Controls the post-validator runtime-verify gate (orchestrator Step 4.6 — runs after validators are green, before external review). One of:

- `auto` (default) — orchestrator invokes the Anthropic `verify` skill if the `Skill` tool is available in its runtime; if not, emits a manual-verify hint in the summary and records the gate as `deferred`. No hard fail.
- `true` — force runtime verify; orchestrator hard-fails when the `Skill` tool is unavailable (use this when you want CI-like discipline).
- `false` — skip the runtime-verify gate entirely. Useful for headless / CI environments where the app cannot be launched.
