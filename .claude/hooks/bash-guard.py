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
    readonly        inspection only (reviewers, scouts, auditors): read-only
                    programs, read-only git, no file writes, no builds, no network
    readonly-build  readonly plus Gradle/Xcode/simulator/adb/emulator commands and
                    writes to absolute paths outside the repository (verifiers)
    no-git-write    anything except git history/index/remote/stash mutation,
                    gh writes and destructive Gradle/Xcode tasks (implementers)

Exit 0 allows the call. Exit 2 blocks it; the stderr text reaches the agent, so
every block names the rule and an alternative. Test matrix: bash-guard-tests.sh
(run it from the main session; read-only agents cannot execute zsh).

Design: quoted spans are masked before tokenizing so a quoted ">" or "&&" is
data, never an operator; newlines are command separators; every simple command
is checked after stripping transparent prefixes (env, nice, timeout, xcrun ...).
"""
import json
import os
import re
import shlex
import sys

READONLY_PROGRAMS = {
    "ls", "cat", "head", "tail", "wc", "grep", "rg", "egrep", "fgrep", "find",
    "sed", "awk", "sort", "uniq", "cut", "tr", "echo", "printf", "pwd", "basename",
    "dirname", "stat", "file", "du", "df", "diff", "cmp", "test", "[", "true",
    "false", "which", "type", "date", "md5", "shasum", "realpath", "readlink",
    "column", "nl", "tac", "rev", "comm", "jq", "xmllint", "strings", "git",
    "xcrun", "adb", "python3", "sleep", "identify", "env", "command", "nice",
    "timeout", "nohup", "time",
}
BUILD_PROGRAMS = {"gradlew", "gradle", "xcodebuild", "xcrun", "adb", "emulator",
                  "simctl", "mkdir", "kill", "pkill", "open", "sips", "plutil"}
GIT_READONLY = {
    "diff", "log", "show", "status", "ls-files", "blame", "grep", "rev-parse",
    "shortlog", "describe", "cat-file", "ls-tree", "help", "version", "stash",
    "tag", "branch", "remote", "worktree", "config", "reflog", "check-ignore",
    "diff-tree", "diff-index", "name-rev", "count-objects", "rev-list",
}
GIT_WRITE = {
    "commit", "push", "reset", "checkout", "switch", "restore", "rebase", "merge",
    "cherry-pick", "revert", "clean", "rm", "mv", "add", "am", "apply", "pull",
    "fetch", "filter-branch", "gc", "prune", "update-ref", "symbolic-ref", "notes",
    "submodule", "init", "clone", "mergetool", "replace", "fast-import",
    "update-index", "read-tree", "write-tree", "commit-tree", "bisect", "ci",
}
GIT_GLOBAL_WITH_VALUE = {"-C", "-c", "--git-dir", "--work-tree", "--namespace",
                         "--exec-path", "--super-prefix", "--config-env"}
GIT_DANGEROUS_CONFIG = re.compile(
    r"^(alias\.|diff\.external|core\.(fsmonitor|pager|editor|sshcommand|hookspath|gitproxy|askpass)|"
    r"credential\.|filter\.|url\.|protocol\.|uploadpack\.|receive\.|http\.|ssh\.|sequence\.editor|"
    r"merge\.tool|mergetool\.|difftool\.|gpg\.|pager\.|interactive\.)", re.I)
GIT_SAFE_ENV = {"GIT_DIR", "GIT_WORK_TREE", "GIT_CEILING_DIRECTORIES", "GIT_TERMINAL_PROMPT",
                "GIT_OPTIONAL_LOCKS", "GIT_TRACE"}
DANGEROUS_ENV = {"PATH", "LD_PRELOAD", "DYLD_INSERT_LIBRARIES", "GRADLE_OPTS", "JAVA_TOOL_OPTIONS",
                 "HOME", "PAGER", "EDITOR", "VISUAL", "XDG_CONFIG_HOME", "LESSOPEN", "LESSCLOSE",
                 "SHELL", "IFS", "PS4", "BASH_ENV", "ENV", "ZDOTDIR", "PYTHONPATH", "PYTHONSTARTUP",
                 "PERL5OPT", "RUBYOPT", "NODE_OPTIONS", "GRADLE_USER_HOME", "JAVA_HOME", "ANDROID_HOME",
                 "ANDROID_SDK_ROOT", "DEVELOPER_DIR"}
INTERPRETERS = {"python", "perl", "ruby", "node", "sh", "bash", "zsh", "eval",
                "exec", "xargs", "source", ".", "osascript", "swift", "kotlin", "java"}
FS_MUTATORS = {"rm", "mv", "cp", "touch", "mkdir", "chmod", "chown", "ln", "tee",
               "truncate", "install", "rmdir", "dd", "sips", "plutil", "yq", "tree"}
SEPARATORS = {"|", "||", "&&", ";", "&", "(", ")", "{", "}", "|&", ";;", "\n"}
PROJECT_DIR = os.path.abspath(os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd())
GH_ALLOWED = {("pr", "view"), ("pr", "list"), ("pr", "diff"), ("pr", "checks"), ("pr", "status"),
              ("issue", "view"), ("issue", "list"), ("repo", "view"), ("auth", "status"),
              ("run", "view"), ("run", "list"), ("api", None), ("--version", None), ("help", None)}

QUOTED = re.compile(r"'[^']*'|\"(?:\\.|[^\"\\])*\"")


def block(msg: str) -> None:
    sys.stderr.write(f"[bash-guard] blocked: {msg}\n")
    sys.exit(2)


def mask_quotes(cmd: str):
    """Replace quoted spans with placeholder words; return (masked, spans)."""
    spans = []

    def repl(m):
        spans.append(m.group(0))
        return f"QSPAN{len(spans) - 1}Z"
    return QUOTED.sub(repl, cmd), spans


def unmask(tok: str, spans):
    def repl(m):
        raw = spans[int(m.group(1))]
        return raw[1:-1] if len(raw) >= 2 else raw
    return re.sub(r"QSPAN(\d+)Z", repl, tok)


def tokenize(masked: str):
    protected = (masked.replace("\\\r\n", " ").replace("\\\n", " ")
                 .replace("\\(", " ESCLPAREN ").replace("\\)", " ESCRPAREN ")
                 .replace("\r\n", " ; ").replace("\n", " ; ").replace("\r", " ; "))
    try:
        tokens = list(shlex.shlex(protected, posix=True, punctuation_chars=True))
    except ValueError as exc:
        block(f"could not parse command ({exc}); simplify the command")
    return tokens


def split_segments(tokens, redirects):
    """Return simple commands as token lists; collect (op, target) redirections."""
    segments, seg = [], []
    i = 0
    while i < len(tokens):
        tok = tokens[i]
        if tok in SEPARATORS:
            if seg:
                segments.append(seg)
            seg = []
            i += 1
            continue
        if any(ch in tok for ch in "<>") and re.fullmatch(r"[\d&]*[<>]+[&|]?", tok):
            target = tokens[i + 1] if i + 1 < len(tokens) else ""
            if target in ("(",):
                block("process substitution is not allowed")
            redirects.append((tok, target))
            i += 2
            continue
        seg.append(tok)
        i += 1
    if seg:
        segments.append(seg)
    return segments


def check_redirects(redirects, spans, mode: str) -> None:
    for op, raw_target in redirects:
        target = unmask(raw_target, spans)
        if "<" in op and ">" not in op:
            continue  # input redirection reads only
        if op.endswith("&") and re.fullmatch(r"\d+|-", target):
            continue  # fd duplication such as 2>&1
        if target == "/dev/null":
            continue
        if mode == "readonly":
            block(f"output redirection to '{target}' writes a file; this agent is read-only")
        if mode == "readonly-build":
            if target.startswith("$") or not os.path.isabs(os.path.expanduser(target)):
                block(f"redirection target '{target}' must be a literal absolute path outside the repository (the guard does not expand shell variables)")
            path = os.path.realpath(os.path.expanduser(target))
            if path == PROJECT_DIR or path.startswith(PROJECT_DIR + os.sep):
                block(f"redirection to '{target}' would write inside the repository; write evidence to the scratchpad")


def strip_prefixes(seg, mode):
    """Drop env assignments and transparent wrapper programs; return (prog, args)."""
    i = 0
    while i < len(seg):
        tok = seg[i]
        m = re.match(r"^([A-Za-z_][A-Za-z0-9_]*)=(.*)$", tok)
        if m:
            key, value = m.group(1), m.group(2)
            if key.startswith("GIT_") and key not in GIT_SAFE_ENV and not (key == "GIT_PAGER" and value in ("", "cat")):
                block(f"environment variable {key} can change git behavior; not allowed")
            if key in DANGEROUS_ENV:
                block(f"environment variable {key} may redirect execution; not allowed")
            i += 1
            continue
        prog = tok.rsplit("/", 1)[-1].lower()
        if prog in ("sudo", "doas"):
            block("privilege escalation is not allowed")
        if prog in ("time", "nohup", "command", "builtin", "env", "nice", "timeout", "caffeinate"):
            i += 1
            # skip the wrapper's own options and option values
            while i < len(seg) and seg[i].startswith("-"):
                if seg[i] in ("-n", "-u", "-s", "-k", "--signal", "--kill-after", "-t"):
                    i += 2
                else:
                    i += 1
            if prog == "timeout" and i < len(seg) and re.fullmatch(r"\d+[smhd]?", seg[i]):
                i += 1
            if prog == "env":
                while i < len(seg) and re.match(r"^[A-Za-z_][A-Za-z0-9_]*=", seg[i]):
                    key, value = seg[i].split("=", 1)
                    if key.startswith("GIT_") and key not in GIT_SAFE_ENV and not (key == "GIT_PAGER" and value in ("", "cat")):
                        block(f"environment variable {key} can change git behavior; not allowed")
                    if key in DANGEROUS_ENV:
                        block(f"environment variable {key} may redirect execution; not allowed")
                    i += 1
            continue
        if prog == "xcrun" and i + 1 < len(seg) and seg[i + 1] in ("git", "python3", "swift", "clang", "sh", "bash"):
            i += 1
            continue
        return prog, seg[i + 1:]
    return None, []


def git_subcommand(args):
    i = 0
    while i < len(args):
        a = args[i]
        if a in GIT_GLOBAL_WITH_VALUE:
            value = args[i + 1] if i + 1 < len(args) else ""
            if a in ("-c", "--config-env") and GIT_DANGEROUS_CONFIG.match(value):
                block(f"'git -c {value}' can run commands or redirect git; not allowed")
            i += 2
            continue
        if any(a.startswith(opt + "=") for opt in GIT_GLOBAL_WITH_VALUE):
            value = a.split("=", 1)[1]
            if a.startswith(("-c=", "--config-env=")) and GIT_DANGEROUS_CONFIG.match(value):
                block(f"'git {a}' can run commands or redirect git; not allowed")
            i += 1
            continue
        if a.startswith("-"):
            i += 1
            continue
        return a, args[i + 1:]
    return None, []


LISTING_FLAGS = {"-l", "--list", "-a", "--all", "-r", "--remotes", "--contains", "--no-contains",
                 "--merged", "--no-merged", "--points-at", "--show-current", "-v", "-vv", "--verbose",
                 "-n", "--sort", "--format", "--column", "--no-column"}


def check_git(args, mode: str) -> None:
    sub, rest = git_subcommand(args)
    if sub is None:
        return
    if sub in GIT_WRITE:
        block(f"'git {sub}' mutates the repository or talks to the remote; subagents never commit, stage, reset, push or fetch. Report the intended change instead")
    if sub == "stash" and (not rest or rest[0] not in ("list", "show")):
        block("'git stash' (any form except list/show) hides the user's uncommitted work; not allowed")
    if sub in ("tag", "branch"):
        listing = any(a in LISTING_FLAGS or a.startswith(("--sort", "--format", "--points-at", "--contains", "--merged")) for a in rest)
        mutating = any(a in ("-d", "-D", "-m", "-M", "-c", "-C", "--delete", "--move", "--copy", "-u",
                             "--set-upstream-to", "--unset-upstream", "-f", "--force", "-e", "--edit-description") for a in rest)
        if mutating or (not listing and any(not a.startswith("-") for a in rest)):
            block(f"creating, deleting, renaming or re-pointing a {sub} is not allowed")
    if sub == "worktree" and any(a in ("add", "remove", "prune", "move", "lock", "unlock", "repair") for a in rest):
        block("worktree mutation is not allowed")
    if sub == "config" and not any(a in ("--get", "--list", "-l", "--get-all", "--get-regexp") for a in rest):
        block("'git config' writes are not allowed")
    if sub == "reflog" and any(a in ("expire", "delete") for a in rest):
        block("reflog mutation is not allowed")
    if sub == "remote" and any(a in ("add", "remove", "rm", "rename", "set-url", "prune", "update", "set-head", "set-branches") for a in rest):
        block("'git remote' mutation is not allowed")
    if any(a == "--output" or a.startswith("--output=") for a in rest):
        block("git --output writes a file; pipe to a read-only program instead")
    if mode in ("readonly", "readonly-build") and sub not in GIT_READONLY:
        block(f"'git {sub}' is outside the read-only set")


def check_gh(args) -> None:
    if not args:
        return
    head = args[0]
    sub = next((a for a in args[1:] if not a.startswith("-")), None)
    joined = " ".join(args)
    if head == "api":
        if not re.search(r"(^|\s)(-X|--method)[ =]GET(\s|$)", joined):
            block("'gh api' may mutate; only explicit GET calls are allowed")
        if any(a in ("--input", "-f", "-F", "--field", "--raw-field") or a.startswith(("--input=", "--field=", "--raw-field=")) for a in args):
            block("'gh api' with a request body is not a read; drop --input/-f/-F")
        return
    if (head, sub) in GH_ALLOWED or (head, None) in GH_ALLOWED and head.startswith("--"):
        return
    block(f"'gh {head} {sub or ''}'.strip() is not in the read-only gh set (pr view/list/diff/checks, issue view/list, repo view, api -X GET)")


def check_gradle(args) -> None:
    for a in args:
        if a == "clean" or a.endswith(":clean"):
            block("'clean' discards build outputs; build the specific target instead")
        if a in ("build", "assemble", "check", "test"):
            block(f"aggregate task '{a}' is not allowed; use a scoped target such as :androidApp:assembleDebug or :shared:assembleSharedDebugXCFramework")
        if a.startswith(("-D", "-P")) and a not in ("-Dorg.gradle.jvmargs", "-Dorg.gradle.workers.max"):
            block(f"Gradle property '{a}' changes how the build runs; verifiers build with the repository's settings")
        if a in ("--init-script", "-I", "--rerun-tasks", "--no-build-cache"):
            block(f"'{a}' changes how the build runs; not allowed for verifiers")


def check_xcodebuild(args) -> None:
    if "clean" in args:
        block("'xcodebuild clean' discards build outputs; build the specific scheme instead")
    if any(a in ("archive", "-exportArchive", "-exportLocalizations", "-importLocalizations") for a in args):
        block("archiving/exporting with xcodebuild is release work, not verification")


def check_readonly_program(prog, args, mode) -> None:
    if prog in INTERPRETERS:
        block(f"'{prog}' can execute arbitrary code; not allowed in {mode} mode")
    if prog == "python3":
        script = args[1] if args[:1] == ["-I"] and len(args) > 1 else None
        hooks_dir = os.path.join(PROJECT_DIR, ".claude", "hooks") + os.sep
        if script is None or not os.path.realpath(os.path.expanduser(script)).startswith(hooks_dir):
            block("python3 is allowed only as 'python3 -I <script under .claude/hooks/>'")
        if any(a == "-c" for a in args):
            block("python3 -c is not allowed")
    if prog == "sed":
        for a in args:
            if a.startswith("-") and "i" in a[1:].replace("E", "").replace("n", "").replace("e", "").replace("r", "").replace("s", ""):
                block("'sed -i' edits files in place; this agent must not write")
        for a in args:
            if not a.startswith("-") and re.search(r"(^|;)\s*[wW]\s*\S", a):
                block("sed 'w' writes a file; not allowed")
    if prog == "awk" and any(("system(" in a or ">" in a or "|" in a or "getline" in a) for a in args):
        block("awk with system()/redirection/getline is not allowed")
    if prog == "sort" and any(a == "-o" or a.startswith("--output") or a.startswith("-o") for a in args):
        block("'sort -o' writes a file; pipe the output instead")
    if prog == "xmllint" and any(a in ("--output", "-o") or a.startswith("--output=") for a in args):
        block("'xmllint --output' writes a file")
    if prog == "find" and any(a in ("-delete", "-exec", "-execdir", "-ok", "-okdir", "-fprint", "-fprint0", "-fprintf", "-fls") for a in args):
        block("find with -delete/-exec/-fprint is not allowed")
    if prog in FS_MUTATORS:
        if mode == "readonly-build" and prog == "mkdir":
            for a in args:
                if a.startswith("-"):
                    continue
                path = os.path.realpath(os.path.expanduser(a))
                if not os.path.isabs(a) or path.startswith(PROJECT_DIR + os.sep):
                    block("mkdir inside the repository is not allowed; create evidence folders in the scratchpad")
            return
        block(f"'{prog}' modifies the filesystem; this agent must not write")
    if mode == "readonly":
        if prog == "xcrun" and args[:2] != ["simctl", "list"]:
            block("only 'xcrun simctl list ...' is allowed in read-only mode")
        if prog == "adb" and args[:1] not in (["devices"], ["shell"]):
            block("only 'adb devices' and read-only 'adb shell' queries are allowed")
    if prog == "adb":
        if args[:1] in (["root"], ["remount"], ["disable-verity"], ["sideload"], ["reboot"]):
            block(f"'adb {args[0]}' changes the device beyond verification needs; not allowed")
        if args[:1] == ["shell"] and any(re.match(r"^(rm|su|root|remount|dd|mkfs|reboot)(\s|$)", a) or " rm " in f" {a} " for a in args[1:]):
            block("adb shell rm/su/root/remount is not allowed")
        if args[:1] == ["pull"] and len(args) >= 3:
            dst = os.path.realpath(os.path.expanduser(args[-1]))
            if not os.path.isabs(args[-1]) or dst.startswith(PROJECT_DIR + os.sep):
                block("adb pull into the repository is not allowed; pull to the scratchpad")
    if prog == "xcrun":
        if args[:1] == ["agvtool"] or args[:2] in (["simctl", "erase"], ["simctl", "delete"]) or (args[:1] == ["simctl"] and "erase" in args[1:3]):
            block("xcrun agvtool / simctl erase|delete modify the project or destroy simulators; not allowed")
    if prog == "emulator" and any(a in ("-wipe-data", "-delete-temp-dir") for a in args):
        block("emulator -wipe-data destroys the AVD state; not allowed")
    if prog not in READONLY_PROGRAMS and not (mode == "readonly-build" and prog in BUILD_PROGRAMS):
        block(f"'{prog}' is not in the {mode} program set")


def main() -> None:
    mode = sys.argv[1] if len(sys.argv) > 1 else "readonly"
    if mode not in ("readonly", "readonly-build", "no-git-write"):
        block(f"unknown guard mode '{mode}'")
    try:
        payload = json.load(sys.stdin)
    except Exception as exc:  # malformed input: fail closed
        block(f"invalid hook payload ({exc})")
    if payload.get("tool_name") != "Bash":
        sys.exit(0)
    tool_input = payload.get("tool_input")
    cmd = tool_input.get("command") if isinstance(tool_input, dict) else None
    if not isinstance(cmd, str) or "\x00" in cmd:
        block("Bash call without a string command; refusing to guess")
    if not cmd.strip():
        sys.exit(0)

    masked, spans = mask_quotes(cmd)
    if mode in ("readonly", "readonly-build") and ("$(" in masked or "`" in masked):
        block("command substitution is not allowed in read-only modes; run the inner command separately")
    if re.search(r"[<>]\(", masked):
        block("process substitution is not allowed")

    tokens = tokenize(masked)
    redirects = []
    segments = split_segments(tokens, redirects)
    check_redirects(redirects, spans, mode)

    for seg in segments:
        seg = [unmask(t, spans) for t in seg]
        prog, args = strip_prefixes(seg, mode)
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
        if prog == "xcodebuild":
            if mode == "readonly":
                block("xcodebuild is reserved for build/runtime verifier agents")
            check_xcodebuild(args)
            continue
        if mode in ("readonly", "readonly-build"):
            check_readonly_program(prog, args, mode)
    sys.exit(0)


if __name__ == "__main__":
    main()
