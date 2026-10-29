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

Design: quote-aware scanning preserves literal operators, joins continuations and
removes shell comments before checking executable substitutions. Newlines separate commands; transparent prefixes are stripped
before checking each command. This guard supplements role instructions and the
host sandbox; it is not a complete shell interpreter or a filesystem sandbox.
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
    "xcrun", "simctl", "adb", "python3", "sleep", "identify", "env", "command", "nice",
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
    r"^(alias\.|include\.|includeif\.|diff\.(?:external|.+\.(?:command|textconv))|core\.(fsmonitor|pager|editor|sshcommand|hookspath|gitproxy|askpass)|"
    r"credential\.|filter\.|url\.|protocol\.|uploadpack\.|receive\.|http\.|ssh\.|sequence\.editor|"
    r"merge\.tool|mergetool\.|difftool\.|gpg\.|pager\.|interactive\.)", re.I)
GIT_SAFE_ENV = {"GIT_DIR", "GIT_WORK_TREE", "GIT_CEILING_DIRECTORIES", "GIT_TERMINAL_PROMPT",
                "GIT_OPTIONAL_LOCKS"}
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

WRAPPER_OPTIONS = {
    "time": ({"-p", "-l", "--portability", "--verbose", "-v"}, {"-f", "--format"}),
    "nohup": (set(), set()),
    "command": ({"-p", "-v", "-V"}, set()),
    "builtin": (set(), set()),
    "env": ({"-i", "--ignore-environment"}, {"-u", "--unset"}),
    "nice": (set(), {"-n", "--adjustment"}),
    "timeout": ({"--preserve-status", "--foreground", "--verbose", "-v"}, {"-s", "--signal", "-k", "--kill-after"}),
    "caffeinate": ({"-d", "-i", "-m", "-s", "-u"}, {"-t", "-w"}),
}


def block(msg: str) -> None:
    sys.stderr.write(f"[bash-guard] blocked: {msg}\n")
    sys.exit(2)


def mask_quotes(cmd: str):
    """Scan quoting, comments and continuations together; markers cannot collide."""
    prefix = "QSPAN"
    while prefix in cmd:
        prefix += "_"
    spans, out, word_start, i = {}, [], True, 0
    while i < len(cmd):
        ch = cmd[i]
        if cmd.startswith("\\\n", i):
            i += 2
            continue
        if ch == "\\":
            out.append(cmd[i:i + 2])
            word_start = False
            i += 2
            continue
        if ch == "#" and word_start:
            while i < len(cmd) and cmd[i] != "\n":
                i += 2 if cmd.startswith("\\\n", i) else 1
            continue
        if ch in "'\"":
            quote, raw = ch, [ch]
            i += 1
            while i < len(cmd) and cmd[i] != quote:
                if quote == '"' and cmd.startswith("\\\n", i):
                    i += 2
                    continue
                size = 2 if quote == '"' and cmd[i] == "\\" else 1
                raw.append(cmd[i:i + size])
                i += size
            if i >= len(cmd):
                block("unclosed shell quote; simplify the command")
            raw.append(quote)
            marker = f"{prefix}{len(spans)}Z"
            spans[marker] = "".join(raw)
            out.append(marker)
            word_start = False
            i += 1
            continue
        out.append(ch)
        word_start = ch.isspace() or ch in ";&|()<>"
        i += 1
    return "".join(out), spans


def has_command_substitution(cmd: str) -> bool:
    """Detect executable substitutions, retaining single-quoted literal data."""
    quote = None
    i = 0
    while i < len(cmd):
        ch = cmd[i]
        if quote == "'":
            if ch == "'":
                quote = None
        elif ch == "\\":
            i += 1
        elif ch == "'" and quote is None:
            quote = "'"
        elif ch == '"':
            quote = None if quote == '"' else '"'
        elif ch == "`" or cmd.startswith("$(", i):
            return True
        i += 1
    return False


def unmask(tok: str, spans, keep_quotes=False):
    if not spans:
        return tok
    return re.sub("|".join(re.escape(marker) for marker in spans),
                  lambda match: spans[match.group(0)] if keep_quotes else spans[match.group(0)][1:-1], tok)


def tokenize(masked: str):
    protected = (masked.replace("\\(", " ESCLPAREN ").replace("\\)", " ESCRPAREN ")
                 .replace("\r\n", " ; ").replace("\n", " ; ").replace("\r", " ; "))
    try:
        lexer = shlex.shlex(protected, posix=True, punctuation_chars=True)
        lexer.whitespace_split = True
        lexer.commenters = ""
        tokens = list(lexer)
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


def check_evidence_path(target):
    if re.search(r"[$`*?\[\]{}]", target) or not os.path.isabs(target):
        block(f"evidence target '{target}' must be a literal absolute path without shell expansions")
    path = os.path.realpath(target)
    if path == PROJECT_DIR or path.startswith(PROJECT_DIR + os.sep):
        block(f"evidence target '{target}' writes inside the repository; use the scratchpad")


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
            check_evidence_path(target)


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
        if prog in WRAPPER_OPTIONS:
            i += 1
            switches, valued = WRAPPER_OPTIONS[prog]
            query = False
            while i < len(seg) and seg[i].startswith("-"):
                option = seg[i]
                if option == "--":
                    i += 1
                    break
                if option in valued:
                    if i + 1 >= len(seg):
                        block(f"{prog} {option} needs a value")
                    i += 2
                elif option in switches or option.split("=", 1)[0] in valued or any(
                        len(flag) == 2 and option.startswith(flag) and len(option) > 2 for flag in valued):
                    query = query or (prog == "command" and option in ("-v", "-V"))
                    i += 1
                elif prog == "nice" and re.fullmatch(r"-\d+", option):
                    i += 1
                elif prog == "caffeinate" and re.fullmatch(r"-[dimsu]+", option):
                    i += 1
                else:
                    block(f"unsupported {prog} option '{option}'; pass the executable directly or use supported inspection options")
            if query:
                return "true", []  # command -v/-V queries names without executing them.
            if prog == "timeout" and i < len(seg) and re.fullmatch(r"(?:\d+(?:\.\d*)?|\.\d+)[smhd]?", seg[i]):
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
        if prog == "xcrun":
            i += 1
            while i < len(seg) and seg[i].startswith("-"):
                option = seg[i]
                if option in ("--sdk", "-sdk", "--toolchain", "-toolchain"):
                    if i + 1 >= len(seg):
                        block("xcrun selector needs a value before the tool name")
                    i += 2
                elif option.startswith(("--sdk=", "--toolchain=")) or option in ("--run", "-r", "--verbose", "-v", "--log", "-l"):
                    i += 1
                else:
                    block("unsupported xcrun option; use an explicit SDK/toolchain and tool name")
            continue
        return prog, seg[i + 1:]
    return None, []


def git_subcommand(args):
    i = 0
    while i < len(args):
        a = args[i]
        if a == "--exec-path":
            return None, []  # Git prints its path and exits without running a subcommand.
        if a.startswith("--exec-path="):
            block("git --exec-path redirects command execution; use the installed git")
        if a.startswith("-c") and a != "-c" and not a.startswith("--"):
            value = a[2:]
            if GIT_DANGEROUS_CONFIG.match(value):
                block(f"'git {a}' can run commands or redirect git; not allowed")
            i += 1
            continue
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


REF_FILTERS = {"--contains", "--no-contains", "--merged", "--no-merged", "--points-at"}


def ref_listing(sub, args):
    """Formatting flags do not put branch/tag into list mode when a name follows."""
    listing = False
    operands = []
    i = 0
    while i < len(args):
        arg = args[i]
        key = arg.split("=", 1)[0]
        if key in ("--no-list", "--no-all", "--no-remotes", "--no-show-current"):
            block("negating git ref-listing mode is outside read-only inspection")
        if key in REF_FILTERS or arg in ("-l", "--list") or (
                sub == "branch" and arg in ("-a", "--all", "-r", "--remotes", "--show-current")):
            listing = True
        if key in REF_FILTERS | {"--sort", "--format"} and "=" not in arg:
            i += 1  # This option's value is not a new branch/tag name.
        elif not arg.startswith("-"):
            operands.append(arg)
        i += 1
    return listing or not operands


def check_git(args) -> None:
    sub, rest = git_subcommand(args)
    if sub is None:
        return
    if sub in GIT_WRITE:
        block(f"'git {sub}' mutates the repository or talks to the remote; subagents never commit, stage, reset, push or fetch. Report the intended change instead")
    if sub == "stash" and (not rest or rest[0] not in ("list", "show")):
        block("'git stash' (any form except list/show) hides the user's uncommitted work; not allowed")
    if sub in ("tag", "branch"):
        mutating = any(a.split("=", 1)[0] in ("-d", "-D", "-m", "-M", "-c", "-C", "--delete", "--move", "--copy", "-u",
                             "--set-upstream-to", "--unset-upstream", "-f", "--force", "-e", "--edit-description") for a in rest)
        if sub == "tag":
            mutating = mutating or any(a in ("-a", "--annotate", "-s", "--sign", "-F", "--file", "--message")
                                      or a.startswith(("--file=", "--message=")) for a in rest)
        else:
            mutating = mutating or any(a == "--create-reflog" or (a.startswith("-u") and not a.startswith("--")) for a in rest)
        if mutating or not ref_listing(sub, rest):
            block(f"creating, deleting, renaming or re-pointing a {sub} is not allowed")
    if sub == "worktree" and any(a in ("add", "remove", "prune", "move", "lock", "unlock", "repair") for a in rest):
        block("worktree mutation is not allowed")
    if sub == "config" and not any(a in ("--get", "--list", "-l", "--get-all", "--get-regexp") for a in rest):
        block("'git config' writes are not allowed")
    if sub == "reflog" and any(a in ("expire", "delete") for a in rest):
        block("reflog mutation is not allowed")
    if sub == "remote":
        action_index = next((i for i, a in enumerate(rest) if not a.startswith("-")), None)
        action = rest[action_index] if action_index is not None else None
        if action in ("add", "remove", "rm", "rename", "set-url", "prune", "update", "set-head", "set-branches"):
            block("'git remote' mutation is not allowed")
        options = rest[action_index + 1:] if action_index is not None else []
        options = options[:options.index("--")] if "--" in options else options
        if action == "show" and "-n" not in options:
            block("git remote show contacts the remote; use 'git remote show -n <name>' for local inspection")
    option_args = rest[:rest.index("--")] if "--" in rest else rest
    if any(a == "--output" or a.startswith("--output=") for a in option_args):
        block("git --output writes a file; pipe to a read-only program instead")
    if any(a in ("--ext-diff", "--textconv", "--open-files-in-pager")
           or a.startswith("--open-files-in-pager=") for a in option_args):
        block("git external diff/text conversion/pager execution is outside read-only inspection")
    if sub == "grep" and any(a.startswith("-O") for a in option_args):
        block("git grep -O can execute an attached pager; use plain git grep")
    if sub not in GIT_READONLY:
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
    choices = {"--console": {"plain", "auto", "rich", "verbose"},
               "--warning-mode": {"all", "fail", "none", "summary"},
               "--dependency": None, "--configuration": None}
    i = 0
    while i < len(args):
        option = args[i]
        if option in choices:
            if i + 1 >= len(args) or args[i + 1].startswith("-") or (
                    choices[option] is not None and args[i + 1] not in choices[option]):
                block(f"{option} needs a supported value")
            args = args[:i] + [option + "=" + args[i + 1]] + args[i + 2:]
        i += 1
    for a in args:
        if a in ("-p", "--project-dir", "-b", "--build-file", "-c", "--settings-file", "--include-build") or re.match(r"^-[pbc].+", a) or a.startswith(("--project-dir=", "--build-file=", "--settings-file=", "--include-build=")):
            block("Gradle project/build redirects are not allowed; build this repository with its configured inputs")
        if a.rsplit(":", 1)[-1] == "syncSecureConfigs":
            block("syncSecureConfigs rewrites tracked configuration; preparation belongs to the main session")
        if a == "clean" or a.endswith(":clean"):
            block("'clean' discards build outputs; build the specific target instead")
        if a.rsplit(":", 1)[-1] in ("build", "assemble", "check", "test"):
            block(f"aggregate task '{a}' is not allowed; use a scoped target such as :androidApp:assembleDebug or :shared:assembleSharedDebugXCFramework")
        if a.startswith(("-D", "-P")) and a not in ("-Dorg.gradle.jvmargs", "-Dorg.gradle.workers.max"):
            block(f"Gradle property '{a}' changes how the build runs; verifiers build with the repository's settings")
        if a in ("--init-script", "-I", "--rerun-tasks", "--no-build-cache") or a.startswith(("--init-script=", "-I")):
            block(f"'{a}' changes how the build runs; not allowed for verifiers")
        if not a.startswith("-") and not re.fullmatch(
                r"(?::[A-Za-z0-9_.-]+)+:(?:assemble[A-Z][A-Za-z0-9]*|compile[A-Z][A-Za-z0-9]*|ksp[A-Z][A-Za-z0-9]*|tasks|dependencies|dependencyInsight)", a):
            block(f"task '{a}' is outside scoped build/inspection targets; tests, linters, publishing and other tasks belong to the main session when explicitly authorized")


def check_sed(args) -> None:
    """Allow display/substitution expressions, not scripts that write or execute."""
    expressions = []
    positional_expression = True
    i = 0
    while i < len(args):
        arg = args[i]
        if arg in ("-e", "--expression"):
            i += 1
            if i >= len(args):
                block("sed -e needs an inspection expression")
            expressions.append(args[i])
            positional_expression = False
        elif arg.startswith("--expression="):
            expressions.append(arg.split("=", 1)[1])
            positional_expression = False
        elif arg.startswith("-e"):
            expressions.append(arg[2:])
            positional_expression = False
        elif arg.startswith("-"):
            if not re.fullmatch(r"-[nEr]+", arg) and arg not in ("--quiet", "--silent", "--regexp-extended"):
                block("sed accepts only -n/-E/-r and explicit -e inspection expressions; script files and in-place editing are not allowed")
        elif positional_expression:
            expressions.append(arg)
            positional_expression = False
        i += 1
    address = r"(?:[0-9]+|\$|/(?:\\.|[^/\\])*/)(?:,(?:[0-9]+|\$|/(?:\\.|[^/\\])*/))?"
    for expression in expressions:
        if re.fullmatch(r"\s*(?:" + address + r")?\s*!?\s*[pPdDqQ=]\s*", expression):
            continue
        # A complete substitution with only non-executing, non-writing flags.
        if re.fullmatch(r"s([^\w\s\\])(?:\\.|(?!\1)[^\\])*\1(?:\\.|(?!\1)[^\\])*\1[gIp0-9]*", expression):
            continue
        block("sed expression is outside display ranges and plain substitutions; use rg or separate inspection commands")


def check_xcodebuild(args) -> None:
    if "clean" in args:
        block("'xcodebuild clean' discards build outputs; build the specific scheme instead")
    if any(a in ("archive", "-exportArchive", "-exportLocalizations", "-importLocalizations") for a in args):
        block("archiving/exporting with xcodebuild is release work, not verification")


def check_simctl(args, mode) -> None:
    if mode == "readonly":
        if args[:1] != ["list"]:
            block("only simulator listing is allowed in read-only mode")
        return
    if args[:1] in (["erase"], ["delete"]):
        block("simctl erase/delete destroys simulator state; not allowed")
    if "booted" in args[:2]:
        block("simctl actions need the selected simulator identifier, not the ambiguous 'booted' alias")
    if args[:1] == ["io"] and args[2:3] in (["screenshot"], ["recordVideo"]):
        check_evidence_path(args[-1])


def check_readonly_program(prog, args, mode) -> None:
    if prog in INTERPRETERS:
        block(f"'{prog}' can execute arbitrary code; not allowed in {mode} mode")
    if prog == "python3":
        script = args[1] if args[:1] == ["-I"] and len(args) > 1 else None
        checker = os.path.realpath(os.path.join(PROJECT_DIR, ".claude", "hooks", "check-rulebook.py"))
        if script is None or os.path.realpath(os.path.expanduser(script)) != checker or len(args) != 2:
            block("python3 is allowed only as 'python3 -I .claude/hooks/check-rulebook.py'")
    if prog == "rg" and any(a in ("--pre", "--hostname-bin") or a.startswith(("--pre=", "--hostname-bin=")) for a in args):
        block("rg preprocessors and hostname helpers execute commands; use plain rg")
    if prog == "adb":
        # Inspect the operation after adb's global device selector as well.
        selected_emulator = args[:1] == ["-s"]
        if args[:1] == ["-s"]:
            if len(args) < 3 or not re.fullmatch(r"emulator-[0-9]+", args[1]):
                block("adb -s must select a verified emulator serial")
            args = args[2:]
        elif args[:1] == ["-e"]:
            if mode == "readonly-build":
                block("adb actions need an explicit -s emulator-<port> serial; -e does not identify the selected device")
            args = args[1:]
        if args and args[0].startswith("-"):
            block("unsupported adb selector; use -s emulator-<port> or -e")
        if mode == "readonly-build" and args[:1] != ["devices"] and not selected_emulator:
            block("adb actions need an explicit -s emulator-<port> serial; inspect adb devices first")
    if prog == "sed":
        check_sed(args)
    if prog == "awk" and any((re.search(r"\bsystem\b|\bgetline\b|@(?:include|load)", a)
                              or ">" in a or "|" in a or a == "-f" or a.startswith(("-f", "--file"))) for a in args):
        block("awk with system()/redirection/getline is not allowed")
    if prog == "sort" and any(a == "-o" or a.startswith("--output") or a.startswith("-o") for a in args):
        block("'sort -o' writes a file; pipe the output instead")
    if prog == "sort" and any(a == "--compress-program" or a.startswith("--compress-program=") for a in args):
        block("sort compression helpers execute external commands; use plain sort")
    if prog == "file" and any(a in ("-C", "--compile") for a in args):
        block("file -C compiles and writes a magic database; use plain file inspection")
    if prog == "xmllint" and any(a in ("--output", "-o") or a.startswith("--output=") for a in args):
        block("'xmllint --output' writes a file")
    if prog == "find" and any(a in ("-delete", "-exec", "-execdir", "-ok", "-okdir", "-fprint", "-fprint0", "-fprintf", "-fls") for a in args):
        block("find with -delete/-exec/-fprint is not allowed")
    if prog in FS_MUTATORS:
        if mode == "readonly-build" and prog == "mkdir":
            for a in args:
                if a.startswith("-"):
                    continue
                check_evidence_path(a)
            return
        block(f"'{prog}' modifies the filesystem; this agent must not write")
    if mode == "readonly":
        if prog == "adb":
            query = args[1:]
            if args[:1] == ["shell"]:
                remote_command = " ".join(query)
                if re.search(r"[\r\n;&|<>`$]", remote_command):
                    block("adb shell queries cannot contain shell operators, newlines or expansions")
                try:
                    query = shlex.split(remote_command)
                except ValueError:
                    block("could not parse adb shell query")
                read_query = (query[:1] in (["getprop"], ["pidof"])
                              or query in (["dumpsys"], ["dumpsys", "battery"], ["dumpsys", "activity", "activities"],
                                           ["dumpsys", "window", "windows"])
                              or query[:2] in (["settings", "get"], ["settings", "list"], ["pm", "list"], ["pm", "path"])
                              or query in (["wm", "size"], ["wm", "density"]))
                if not read_query:
                    block("adb shell is limited to read-only device queries; actions belong to runtime verification")
            elif args[:1] != ["devices"]:
                block("only 'adb devices' and read-only 'adb shell' queries are allowed")
    if prog == "adb":
        if args[:1] in (["root"], ["remount"], ["disable-verity"], ["sideload"], ["reboot"]):
            block(f"'adb {args[0]}' changes the device beyond verification needs; not allowed")
        if args[:1] == ["shell"] and any(re.match(r"^(rm|su|root|remount|dd|mkfs|reboot)(\s|$)", a) or " rm " in f" {a} " for a in args[1:]):
            block("adb shell rm/su/root/remount is not allowed")
        if args[:1] == ["pull"]:
            if len(args) != 3 or args[1].startswith("-"):
                block("adb pull needs one remote source and one explicit scratchpad destination; implicit destinations and option forms are not allowed")
            check_evidence_path(args[-1])
    if prog == "simctl":
        check_simctl(args, mode)
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
    if not isinstance(payload, dict):
        block("hook payload must be a JSON object")
    if payload.get("tool_name") != "Bash":
        sys.exit(0)
    tool_input = payload.get("tool_input")
    cmd = tool_input.get("command") if isinstance(tool_input, dict) else None
    if not isinstance(cmd, str) or "\x00" in cmd:
        block("Bash call without a string command; refusing to guess")
    if not cmd.strip():
        sys.exit(0)

    masked, spans = mask_quotes(cmd)
    if has_command_substitution(unmask(masked, spans, keep_quotes=True)):
        block("command substitution can hide restricted operations; run the inner command separately")
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
            check_git(args)
            continue
        if prog == "gh":
            if mode == "readonly":
                block("gh uses the network; read-only shell roles inspect local sources")
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
