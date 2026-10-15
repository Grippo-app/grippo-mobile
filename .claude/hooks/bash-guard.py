#!/usr/bin/env python3
"""PreToolUse guard for Bash calls made by project subagents.

Usage (agent frontmatter):
    hooks:
      PreToolUse:
        - matcher: "Bash"
          hooks:
            - type: command
              command: python3 "$CLAUDE_PROJECT_DIR/.claude/hooks/bash-guard.py" readonly

Modes:
    readonly      only inspection commands (reviewers, scouts, auditors)
    no-git-write  anything except history/remote/index mutation and
                  destructive gradle tasks (implementers, verifiers)

Exit 0 allows the call. Exit 2 blocks it and the stderr text is shown to the
agent, so every block names the rule and the alternative.
"""
import json
import re
import shlex
import sys

READONLY_PROGRAMS = {
    "ls", "cat", "head", "tail", "wc", "grep", "rg", "egrep", "fgrep", "find",
    "sed", "awk", "sort", "uniq", "cut", "tr", "echo", "printf", "pwd", "basename",
    "dirname", "stat", "file", "du", "df", "tree", "diff", "cmp", "test", "[", "true",
    "false", "which", "type", "env", "date", "md5", "shasum", "realpath", "readlink",
    "column", "nl", "tac", "rev", "comm", "jq", "yq", "xmllint", "plutil", "strings",
    "git", "xcrun", "adb", "sips", "identify", "python3",
}
GIT_READONLY = {
    "diff", "log", "show", "status", "ls-files", "blame", "grep", "rev-parse",
    "shortlog", "describe", "cat-file", "ls-tree", "help", "version", "stash",
    "tag", "branch", "remote", "worktree", "config", "reflog", "check-ignore",
    "diff-tree", "diff-index", "name-rev", "count-objects", "rev-list",
}
GIT_WRITE = {
    "commit", "push", "reset", "checkout", "switch", "restore", "rebase", "merge",
    "cherry-pick", "revert", "clean", "rm", "mv", "add", "am", "apply", "pull",
    "filter-branch", "gc", "prune", "update-ref", "symbolic-ref", "notes",
    "submodule", "init", "clone", "mergetool", "replace", "fast-import",
}
INTERPRETERS = {"python", "python3", "perl", "ruby", "node", "sh", "bash", "zsh", "eval", "exec", "xargs", "source", "."}


def block(msg: str) -> None:
    sys.stderr.write(f"[bash-guard] blocked: {msg}\n")
    sys.exit(2)


def segments(cmd: str):
    """Split a shell command into simple-command token lists."""
    try:
        tokens = list(shlex.shlex(cmd, posix=True, punctuation_chars=True))
    except ValueError as exc:
        block(f"could not parse command ({exc}); simplify the command")
    seg, out = [], []
    for tok in tokens:
        if tok in ("|", "||", "&&", ";", "&", "(", ")", "|&"):
            if seg:
                out.append(seg)
            seg = []
        else:
            seg.append(tok)
    if seg:
        out.append(seg)
    return out


def program_of(seg):
    i = 0
    while i < len(seg) and re.match(r"^[A-Za-z_][A-Za-z0-9_]*=", seg[i]):
        i += 1
    if i >= len(seg):
        return None, []
    prog = seg[i].rsplit("/", 1)[-1]
    if prog in ("sudo", "time", "nice", "nohup", "command", "builtin"):
        return program_of(seg[i + 1:])
    return prog, seg[i + 1:]


def check_redirections(cmd: str) -> None:
    # Allow 2>/dev/null, >/dev/null, 2>&1, &>/dev/null; block any other > or >>.
    for m in re.finditer(r"(\d?)(&?)(>>?)(&?)\s*(\S*)", cmd):
        fd, amp_before, op, amp_after, target = m.groups()
        if amp_after:  # 2>&1 style
            continue
        if target == "/dev/null":
            continue
        block(f"output redirection '{m.group(0).strip()}' writes a file; this agent must not write")
    if "<(" in cmd or ">(" in cmd:
        block("process substitution is not allowed")


def check_git(args, mode: str) -> None:
    sub = next((a for a in args if not a.startswith("-")), None)
    if sub is None:
        return
    if sub in GIT_WRITE:
        block(f"'git {sub}' mutates the repository; subagents never commit, stage, reset or push. Report the intended change instead")
    if sub == "stash" and any(a in ("push", "pop", "apply", "drop", "clear", "save") for a in args):
        block("'git stash' mutation is not allowed")
    if sub == "tag" and any(not a.startswith("-") for a in args[args.index("tag") + 1:]):
        block("creating or deleting tags is not allowed")
    if sub == "branch" and any(a in ("-d", "-D", "-m", "-M", "-c", "-C", "--delete", "--move", "--copy") or not a.startswith("-") for a in args[args.index("branch") + 1:]):
        block("creating, renaming or deleting branches is not allowed")
    if sub == "worktree" and any(a in ("add", "remove", "prune", "move", "lock", "unlock") for a in args):
        block("worktree mutation is not allowed")
    if sub == "config" and not any(a in ("--get", "--list", "-l", "--get-all", "--get-regexp") for a in args):
        block("'git config' writes are not allowed")
    if sub == "reflog" and any(a in ("expire", "delete") for a in args):
        block("reflog mutation is not allowed")
    if mode == "readonly" and sub not in GIT_READONLY:
        block(f"'git {sub}' is outside the read-only set")


def check_gh(args) -> None:
    joined = " ".join(args)
    if re.search(r"\b(pr|issue)\s+(create|merge|close|edit|comment|review|reopen|ready|lock)\b", joined):
        block("gh write operations are reserved for the main session acting on the user's request")
    if args[:1] == ["api"] and not re.search(r"(-X|--method)\s+GET\b", joined):
        block("'gh api' may mutate; only explicit GET calls are allowed")
    if args[:1] in (["release"], ["repo"], ["secret"], ["workflow"], ["run"]):
        block(f"'gh {args[0]}' is not allowed for subagents")


def check_gradle(args) -> None:
    for a in args:
        if a == "clean" or a.endswith(":clean"):
            block("'clean' discards build outputs; build the specific target instead")
        if a in ("build", "assemble", "check", "test"):
            block(f"aggregate task '{a}' is not allowed; use a scoped target such as :androidApp:assembleDebug or :shared:assembleSharedDebugXCFramework")


def main() -> None:
    mode = sys.argv[1] if len(sys.argv) > 1 else "readonly"
    try:
        payload = json.load(sys.stdin)
    except Exception as exc:  # malformed input: fail closed
        block(f"invalid hook payload ({exc})")
    if payload.get("tool_name") != "Bash":
        sys.exit(0)
    cmd = (payload.get("tool_input") or {}).get("command") or ""
    if not cmd.strip():
        sys.exit(0)

    if mode == "readonly":
        check_redirections(cmd)
        if "$(" in cmd or "`" in cmd:
            block("command substitution is not allowed in read-only mode")

    for seg in segments(cmd):
        prog, args = program_of(seg)
        if prog is None:
            continue
        if prog == "git":
            check_git(args, mode)
            continue
        if prog == "gh":
            check_gh(args)
            continue
        if prog in ("gradle", "gradlew") or prog.endswith("gradlew"):
            if mode == "readonly":
                block("gradle builds are reserved for build/runtime verifier agents")
            check_gradle(args)
            continue
        if mode == "readonly":
            if prog in INTERPRETERS and prog != "python3":
                block(f"'{prog}' can execute arbitrary code; not allowed in read-only mode")
            if prog == "python3" and args[:1] != ["-I"]:
                block("python3 is allowed only as 'python3 -I <script>' (for example the rulebook link checker)")
            if prog == "sed" and any(a == "-i" or a.startswith("-i") for a in args):
                block("'sed -i' edits files in place; this agent is read-only")
            if prog == "find" and any(a in ("-delete", "-exec", "-execdir", "-ok") for a in args):
                block("find with -delete/-exec is not allowed")
            if prog in ("rm", "mv", "cp", "touch", "mkdir", "chmod", "chown", "ln", "tee", "truncate", "install"):
                block(f"'{prog}' modifies the filesystem; this agent is read-only")
            if prog == "xcrun" and args[:1] != ["simctl"]:
                block("only 'xcrun simctl list ...' is allowed in read-only mode")
            if prog == "xcrun" and args[:2] != ["simctl", "list"]:
                block("only 'xcrun simctl list ...' is allowed in read-only mode")
            if prog == "adb" and args[:1] not in (["devices"], ["shell"]) :
                block("only 'adb devices' and read-only 'adb shell' queries are allowed")
            if prog not in READONLY_PROGRAMS:
                block(f"'{prog}' is not in the read-only program set")
    sys.exit(0)


if __name__ == "__main__":
    main()
