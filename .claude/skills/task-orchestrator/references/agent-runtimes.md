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

## Codex and explicit-role fallback

Use the session's subagent tool (for example `spawn_agent`) and provide the
canonical role path plus the common launch contract. This also works when Claude
has a generic agent available but has not loaded the named role yet. A generic
agent without the explicit role, owning skills and gates is only a scout.

Claude frontmatter is not automatically installed as Codex configuration.
Inherit the parent model in Codex unless the user selected a supported override;
`sonnet` is a Claude setting, not a model name to pass to Codex. Pass a complete
brief even when the runtime also inherits conversation history.

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
request a restart that cannot add the capability. Completion still follows the
project's definition of done, with that missing review stated explicitly.

## Runtime references

- [Official Codex subagent documentation](https://learn.chatgpt.com/docs/agent-configuration/subagents)
  describes delegation, inherited permissions and custom-agent configuration.
- [Official AGENTS.md documentation](https://learn.chatgpt.com/docs/agent-configuration/agents-md)
  describes project instruction discovery.
