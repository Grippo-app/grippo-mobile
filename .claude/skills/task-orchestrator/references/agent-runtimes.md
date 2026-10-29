# Running project agents in Claude Code and Codex

The canonical roles are `.claude/agents/<role>.md` in both clients. Read the
selected definition before delegation; its rights, duties, skills and report
format apply regardless of the tool that starts the agent. Runtime configuration
adapts those roles; it does not create another copy of them.

## Common launch contract

The brief names the role file, the task's scope, owned paths, prerequisites,
baseline and task delta, checks authorized and the required report. The agent
reads CLAUDE.md, its role file, every listed `skills:` entry and the references
its duties require before working. Automatic skill preloading satisfies only
the files actually loaded by the client.

The parent checks the tools and permissions exposed in the current session.
Map capabilities, not names: file reading/search, file editing, shell, browser
or simulator control. A tool's presence never broadens the role's rights.
Only the parent delegates; delegated agents do not start other agents.

## Claude Code

Use the registered project role through the session's agent tool when available.
The definition's `model`, `tools`, `disallowedTools`, `skills` and `hooks` are
Claude configuration. Confirm that the requested role and its Bash hook are
available. A newly edited definition may require a new session for native loading;
until then, pass the current role explicitly using the fallback below.

## Codex native discovery and roles

Codex discovers project skills through `.agents/skills/<skill>`, symlinks to the
canonical `.claude/skills/<skill>` directories. Standalone project roles in
`.codex/agents/<role>.toml` point back to `.claude/agents/<role>.md`; they do not
copy its policy. Generate projections after canonical metadata or membership
changes with `python3 -I .claude/hooks/sync-native-adapters.py --write`.
The ordinary rulebook checker rejects missing, extra or drifted projections.

Use the registered project role when the client's subagent tool exposes it.
The adapters inherit the parent model and permissions. They disable memories and
permission-request tools, plus apps/plugins for roles without browser/simulator
rights. Supported feature switches reduce those capabilities only: they do not
install Claude's tool allowlist, Bash hook, editing ban or no-nesting rule.
Apply the canonical role and the procedural protection below for the remaining
boundaries. Never describe the adapter as a per-role read-only sandbox.

Verify discovery in a fresh local session: `codex debug prompt-input` displays the
available skill descriptions without running a model task. Check that each
project skill appears once. For registration, inspect the actual subagent tool's
project types and run a bounded read-only role probe. For enforcement, verify
which tools were actually exposed separately from the guard matrix. TOML parsing
or an adapter file's existence alone does not prove native registration or
runtime enforcement. Report client/trust/usage limitations precisely.

## Explicit-role fallback

If a native project type is not exposed or the session has stale definitions,
use the session's generic subagent tool and provide the canonical role path plus
the common launch contract. A generic agent without its explicit role, owning
skills and gates is only a scout. Inherit the parent model unless the user chose
an available override; `sonnet` is a Claude setting, not a Codex model name.
Pass a complete brief even when conversation history is inherited.

Use native capability restrictions when the session exposes them. When per-role
shell hooks are unavailable, keep the same guard mode as a procedural check:
submit the exact proposed command as JSON to the canonical `bash-guard.py`
(`tool_name: "Bash"`, `tool_input.command: <literal command>`), then execute
that unchanged command in the same repository only if the guard exits 0.
Construct the payload as data with a JSON encoder, never shell interpolation.
A rejection is not permission to repackage the operation in another tool.
If the agent cannot perform this check, it sends shell requests to the parent;
the parent applies the guard and returns the output. File reads and searches
still follow the role's read-only rights.

A shell-only client can use this check-and-run transport from the repository
root. Replace only the literal command and the mode from the role's hook. The
Python transport may only encode the payload, run the guard and conditionally
run that unchanged command; it is not permission for other interpreter work.

```python
import json
import subprocess

command = "git diff --stat"
guard = subprocess.run(
    ["python3", "-I", ".claude/hooks/bash-guard.py", "readonly"],
    input=json.dumps({"tool_name": "Bash", "tool_input": {"command": command}}),
    text=True,
)
if guard.returncode == 0:
    subprocess.run(["/bin/zsh", "-c", command], check=True)
else:
    raise SystemExit(guard.returncode)
```

These role instructions and explicit guard checks are procedural controls;
report them as such. They do not create a separate sandbox or an automatically
installed hook. Keep the host's sandbox and approvals in force. If a required
operation cannot respect the role's rights, leave that operation unperformed,
report the missing capability and continue independent work.

Browser and simulator capabilities may use different providers. Preserve the
role's allowed site, device, action and authorization boundaries when using an
equivalent available tool. A missing named MCP provider alone does not require
a restart; a missing capability is reported with the checks it prevents.

## Independent review

A separately spawned agent qualifies when it edited none of the task's files,
read the reviewer role and owning gates, followed its rights and returned that
role's report against the task delta. The parent verifies those facts and
reports the actual protection used (native hooks or procedural checks).
A role name or an author's self-review alone does not qualify.

## Unavailable delegation

If no subagent capability is available, the main session continues implementation
and runs the available, authorized checks itself, including builds normally sent
to a verifier. Apply the same check scope, device/site boundaries and evidence
requirements; a missing helper does not make an available check unavailable.

Report independent review as unperformed; do not substitute self-review or
request a restart that cannot add the capability. Its absence does not satisfy or
waive independent review under the project's definition of done. Continue the
available work and report the outstanding review; do not claim completion while
that requirement remains unmet.

## Runtime references

- [Official Codex subagent documentation](https://learn.chatgpt.com/docs/agent-configuration/subagents)
  describes delegation, inherited permissions and custom-agent configuration.
- [Official AGENTS.md documentation](https://learn.chatgpt.com/docs/agent-configuration/agents-md)
  describes project instruction discovery.

- [Official Codex skill discovery documentation](https://learn.chatgpt.com/docs/build-skills)
  describes project skill directories and symlinked skill folders.
- [Official Claude subagent documentation](https://code.claude.com/docs/en/sub-agents)
  describes project roles, full skill preloading, tool allowlists and hooks.
