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
    readonly-compile inspection plus scoped Gradle/Xcode compilation and external logs;
                    no device queries, installation, launch or device control
    readonly-build  readonly plus Gradle/Xcode/simulator/adb/emulator commands and
                    writes to absolute paths outside the repository (verifiers)
    no-git-write    anything except git history/index/remote/stash mutation,
                    gh writes and destructive Gradle/Xcode tasks (implementers)

Exit 0 allows the call. Exit 2 blocks it; the stderr text reaches the agent, so
every block names the rule and an alternative. Test matrix: bash-guard-tests.sh
(run it from the main session; read-only agents cannot execute zsh).

Design: quote-aware scanning preserves literal operators, joins continuations and
removes shell comments before checking executable substitutions. Newlines separate commands; transparent prefixes are stripped
before checking each command. Quote metadata distinguishes literal data from
unresolved expansions in command names, options and inspection scripts. This
guard does not inspect inherited tool configuration or executable/file contents.
It supplements role instructions and the
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
    r"merge\.tool|mergetool\.|difftool\.|gpg\.|pager\.|interactive\.|browser\.|man\.|help\.(?:browser|format)|web\.browser)", re.I)
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


class ShellWord(str):
    def __new__(cls, value, dynamic=False, may_split=False):
        word = super().__new__(cls, value)
        word.dynamic = dynamic
        word.may_split = may_split
        return word


def ansi_quote(cmd, start):
    """Decode the portable ANSI-C quote escapes; reject unsupported dialect forms."""
    escapes = {"a": "\a", "b": "\b", "e": "\x1b", "E": "\x1b", "f": "\f", "n": "\n",
               "r": "\r", "t": "\t", "v": "\v", "\\": "\\", "'": "'", '"': '"', "?": "?"}
    out, i = bytearray(), start
    while i < len(cmd):
        ch = cmd[i]
        if ch == "'":
            return os.fsdecode(bytes(out)), i + 1
        if ch != "\\":
            out.extend(ch.encode("utf-8"))
            i += 1
            continue
        i += 1
        if i == len(cmd):
            break
        ch = cmd[i]
        if ch in escapes:
            value, i = escapes[ch].encode("utf-8"), i + 1
        elif ch == "x":
            match = re.match(r"[0-9a-fA-F]{1,2}", cmd[i + 1:])
            if not match:
                block("ANSI-C hex escapes need one or two hex digits")
            value = bytes([int(match[0], 16)])
            i += 1 + len(match[0])
        elif ch in ("u", "U"):
            block("ANSI-C Unicode escapes differ across installed shells; use literal Unicode text")
        elif ch in "01234567":
            match = re.match(r"[0-7]{1,3}", cmd[i:])
            value, i = bytes([int(match[0], 8) & 255]), i + len(match[0])
        else:
            block("unsupported ANSI-C escape; use literal text or ordinary quotes")
        if value == b"\0":
            block("NUL in an ANSI-C quote cannot be represented as a command argument")
        out.extend(value)
    block("unclosed ANSI-C shell quote")


def expansion_flags(raw):
    """Track unresolved parameters and unquoted word/glob expansion, not regex data."""
    quote, dynamic, may_split, i = None, False, False, 0
    while i < len(raw):
        ch = raw[i]
        if quote == "'":
            if ch == "'":
                quote = None
        elif ch == "\\":
            i += 1
        elif ch == "'" and quote is None:
            quote = "'"
        elif ch == '"':
            quote = None if quote == '"' else '"'
        elif ch == "$" and i + 1 < len(raw) and re.match(r"[\w{@*#?$!<~=^\[-]", raw[i + 1]):
            dynamic = True
            tail = raw[i + 1:]
            multiword = tail[0] in "@=~^" or re.match(r"[A-Za-z_]\w*\[@\]", tail) or (tail.startswith("{") and
                        (tail[1:2] in ("(", "=", "~", "^", "@", "!") or
                         re.match(r"\{[^}]*\[@\]", tail)))
            may_split = may_split or quote is None or bool(multiword)
        elif quote is None and (ch in "*?" or (ch == "[" and "]" in raw[i + 1:]) or
                                     (ch == "{" and re.match(r"[^{}]*(?:,|\.\.)[^{}]*\}", raw[i + 1:]))):
            dynamic = may_split = True
        i += 1
    return dynamic, may_split


def require_literal(word):
    if getattr(word, "dynamic", False):
        block("unresolved shell expansion can change this command or control argument; use its literal value")


def require_single_word(word):
    if getattr(word, "may_split", False):
        block("this expansion can produce multiple arguments; use a quoted scalar or literal control argument")


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
            if i + 1 == len(cmd):
                block("unclosed shell escape")
            marker = f"{prefix}{len(spans)}Z"
            spans[marker] = (cmd[i:i + 2], cmd[i + 1])
            out.append(marker)
            word_start = False
            i += 2
            continue
        if ch == "#" and word_start:
            while i < len(cmd) and cmd[i] != "\n":
                i += 2 if cmd.startswith("\\\n", i) else 1
            continue
        if cmd.startswith("$'", i):
            value, i = ansi_quote(cmd, i + 2)
            marker = f"{prefix}{len(spans)}Z"
            spans[marker] = ("'" + value.replace("'", "'\"'\"'") + "'", value)
            out.append(marker)
            word_start = False
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
            raw = "".join(raw)
            spans[marker] = (raw, raw[1:-1])
            out.append(marker)
            word_start = False
            i += 1
            continue
        out.append(ch)
        word_start = ch.isspace() or ch in ";&|()<>"
        i += 1
    return "".join(out), spans


def has_command_substitution(cmd: str) -> bool:
    """Reject executable shell expansion while retaining single-quoted literal data."""
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
        elif ch == "`" or cmd.startswith(("$(", "$[", "${("), i):
            return True
        elif cmd.startswith("${!", i) and not cmd.startswith("${!}", i):
            return True
        elif cmd.startswith("${", i):
            parameter = cmd[i:]
            if re.match(r"\$\{[A-Za-z_]\w*(?:\[[^]]*\])?@P\}", parameter):
                return True  # Bash transformations include prompt evaluation (@P).
            index = re.match(r"\$\{[A-Za-z_]\w*\[([^]]*)\]", parameter)
            if index and not re.fullmatch(r"[0-9]+|[@*]", index[1]):
                return True  # An arithmetic index can recursively evaluate variable contents.
        elif ch == "$":
            index = re.match(r"\$[A-Za-z_]\w*\[([^]]*)\]", cmd[i:])
            if index and not re.fullmatch(r"[0-9]+|[@*]", index[1]):
                return True  # Zsh also permits an unbraced arithmetic subscript.
        i += 1
    return False


def unmask(tok: str, spans, keep_quotes=False):
    if not spans:
        return tok
    return re.sub("|".join(re.escape(marker) for marker in spans),
                  lambda match: spans[match.group(0)][0 if keep_quotes else 1], tok)


def tokenize(masked: str):
    protected = masked.replace("\r\n", " ; ").replace("\n", " ; ").replace("\r", " ; ")
    fd_prefix = "FDSPAN"
    while fd_prefix in protected:
        fd_prefix += "_"
    # Preserve adjacency: '2>&1' has an IO number; '2 >&1' has an argument.
    protected = re.sub(r"(^|[\s;|&()])(\d+)(?=[<>])",
                       lambda m: m[1] + fd_prefix + m[2] + " ", protected)
    try:
        lexer = shlex.shlex(protected, posix=True, punctuation_chars=True)
        lexer.whitespace_split = True
        lexer.commenters = ""
        tokens = list(lexer)
    except ValueError as exc:
        block(f"could not parse command ({exc}); simplify the command")
    merged, i = [], 0
    while i < len(tokens):
        if tokens[i].startswith(fd_prefix) and tokens[i][len(fd_prefix):].isdigit():
            merged.append(tokens[i][len(fd_prefix):] + tokens[i + 1])
            i += 2
        else:
            merged.append(tokens[i])
            i += 1
    return merged


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
        if mode in ("readonly-compile", "readonly-build"):
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
            if key == "RIPGREP_CONFIG_PATH" and value:
                block("RIPGREP_CONFIG_PATH can load command helpers; use an empty value or plain rg --no-config")
            i += 1
            continue
        require_literal(tok)
        prog = tok.rsplit("/", 1)[-1].lower()
        if prog in ("sudo", "doas"):
            block("privilege escalation is not allowed")
        if prog in WRAPPER_OPTIONS:
            i += 1
            switches, valued = WRAPPER_OPTIONS[prog]
            query = False
            while i < len(seg) and seg[i].startswith("-"):
                option = seg[i]
                require_single_word(option)
                if option == "--":
                    i += 1
                    break
                if option in valued:
                    if i + 1 >= len(seg):
                        block(f"{prog} {option} needs a value")
                    require_single_word(seg[i + 1])
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
                    require_single_word(seg[i])
                    key, value = seg[i].split("=", 1)
                    if key.startswith("GIT_") and key not in GIT_SAFE_ENV and not (key == "GIT_PAGER" and value in ("", "cat")):
                        block(f"environment variable {key} can change git behavior; not allowed")
                    if key in DANGEROUS_ENV:
                        block(f"environment variable {key} may redirect execution; not allowed")
                    if key == "RIPGREP_CONFIG_PATH" and value:
                        block("RIPGREP_CONFIG_PATH can load command helpers; use an empty value or plain rg --no-config")
                    i += 1
            continue
        if prog == "xcrun":
            i += 1
            while i < len(seg) and seg[i].startswith("-"):
                option = seg[i]
                require_single_word(option)
                if option in ("--sdk", "-sdk", "--toolchain", "-toolchain"):
                    if i + 1 >= len(seg):
                        block("xcrun selector needs a value before the tool name")
                    require_single_word(seg[i + 1])
                    i += 2
                elif option.startswith(("--sdk=", "--toolchain=")) or option in ("--run", "-r", "--verbose", "-v", "--log", "-l"):
                    i += 1
                else:
                    block("unsupported xcrun option; use an explicit SDK/toolchain and tool name")
            continue
        return prog, seg[i + 1:]
    return None, []


def check_git_override(value, source):
    require_single_word(source)
    if getattr(source, "dynamic", False) and not re.match(r"^[A-Za-z][A-Za-z0-9.-]*=", value):
        require_literal(source)
    if GIT_DANGEROUS_CONFIG.match(value):
        block("git configuration override can execute helpers or redirect access; use repository settings")


def git_subcommand(args):
    i = 0
    while i < len(args):
        a = args[i]
        require_single_word(a)
        if a == "--exec-path":
            return None, []  # Git prints its path and exits without running a subcommand.
        if a.startswith("--exec-path="):
            block("git --exec-path redirects command execution; use the installed git")
        if a == "--help":
            return "help", args[i + 1:]
        if a.startswith("-c") and a != "-c" and not a.startswith("--"):
            value = a[2:]
            check_git_override(value, a)
            i += 1
            continue
        if a in GIT_GLOBAL_WITH_VALUE:
            value = args[i + 1] if i + 1 < len(args) else ""
            require_single_word(value)
            if a in ("-c", "--config-env"):
                check_git_override(value, value)
            i += 2
            continue
        if any(a.startswith(opt + "=") for opt in GIT_GLOBAL_WITH_VALUE):
            value = a.split("=", 1)[1]
            if a.startswith(("-c=", "--config-env=")):
                check_git_override(value, a)
            i += 1
            continue
        require_literal(a)
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
    write_options = {"--delete", "--force", "--create-reflog"}
    if sub == "branch":
        write_options |= {"--move", "--copy", "--set-upstream", "--set-upstream-to",
                          "--unset-upstream", "--edit-description"}
    else:
        write_options |= {"--annotate", "--sign", "--local-user", "--file", "--message"}
    forbidden_options = write_options | {"--no-list", "--no-all", "--no-remotes", "--no-show-current"}
    i = 0
    while i < len(args):
        arg = args[i]
        key = arg.split("=", 1)[0]
        if len(key) > 2 and key.startswith("--") and any(option.startswith(key) for option in forbidden_options):
            block(f"mutating or negating git {sub} options, including abbreviations, are not allowed")
        if arg.startswith("-") and not arg.startswith("--"):
            write_flags = "dDmMcCufe" if sub == "branch" else "dasmFuf"
            if any(flag in write_flags for flag in arg[1:]):
                block(f"mutating git {sub} flags are not allowed inside short-option clusters")
        if key in REF_FILTERS or arg in ("-l", "--list") or (
                sub == "branch" and arg in ("-a", "--all", "-r", "--remotes", "--show-current")):
            listing = True
        if key in REF_FILTERS | {"--sort", "--format"} and "=" not in arg:
            i += 1  # This option's value is not a new branch/tag name.
        elif not arg.startswith("-"):
            operands.append(arg)
        i += 1
    return listing or not operands


def option_values(args, short_values="", long_values=(), short_equals=False,
                  abbreviations=False, single_dash_long=False, terminator=True, short_booleans=()):
    """Separate options, their attached/separate values and the '--' terminator."""
    i = 0
    while i < len(args):
        arg = args[i]
        if arg == "--" and terminator:
            return
        require_single_word(arg)
        if single_dash_long and arg.startswith("-") and not arg.startswith("--") and len(arg) > 2:
            arg = ShellWord("-" + arg, getattr(arg, "dynamic", False))
        if arg.startswith("--"):
            name, separator, value = arg.partition("=")
            if getattr(arg, "dynamic", False) and re.search(r"[$*?\[{}]", name):
                require_literal(arg)
            if abbreviations and name not in long_values:
                matches = [option for option in long_values if option.startswith(name)]
                if len(matches) > 1:
                    block("ambiguous long value option; spell out the full option name")
                if matches:
                    name = matches[0]
            if name in long_values and not separator:
                i += 1
                value = args[i] if i < len(args) else None
                require_single_word(value)
            yield name, value if name in long_values or separator else None
        elif arg.startswith("-"):
            for position, flag in enumerate(arg[1:], 1):
                if getattr(arg, "dynamic", False) and flag in "$*?[{":
                    require_literal(arg)
                if flag in short_booleans and arg[position + 1:position + 2] == "=":
                    yield "-" + flag, arg[position + 2:]
                    break
                if flag in short_values:
                    value = arg[position + 1:]
                    if short_equals and value.startswith("="):
                        value = value[1:]  # pflag accepts -X=GET; getopt keeps '=' as data.
                    if position == len(arg) - 1:
                        i += 1
                        value = args[i] if i < len(args) else None
                        require_single_word(value)
                    yield "-" + flag, value
                    break
                yield "-" + flag, None
        elif getattr(arg, "dynamic", False) and not arg.startswith(("/", "./", "../")):
            require_literal(arg)
        i += 1


def check_git(args) -> None:
    sub, rest = git_subcommand(args)
    if sub is None:
        return
    values = REF_FILTERS | {"--sort", "--format"} if sub in ("branch", "tag") else set()
    short_values = ""
    if sub == "grep":
        short_values = "efABCmO"
        values = {"--max-depth", "--context", "--before-context", "--after-context", "--threads",
                  "--max-count", "--regexp", "--file"}
    elif sub in ("diff", "diff-tree", "diff-index"):
        short_values = "OSG"
        values = {"--src-prefix", "--dst-prefix", "--line-prefix", "--word-diff-regex", "--anchored",
                  "--output", "--stat-width", "--stat-name-width", "--stat-graph-width", "--stat-count"}
    elif sub == "config":
        short_values = "ft"
        values = {"--file", "--blob", "--type", "--default", "--comment"}
    elif sub == "cat-file":
        values = {"--path"}
    option_args = [name for name, _ in option_values(rest, short_values, values, abbreviations=sub == "config")]
    if sub in GIT_WRITE:
        block(f"'git {sub}' mutates the repository or talks to the remote; subagents never commit, stage, reset, push or fetch. Report the intended change instead")
    if sub == "stash" and (not rest or rest[0] not in ("list", "show")):
        block("'git stash' (any form except list/show) hides the user's uncommitted work; not allowed")
    if sub in ("tag", "branch"):
        if not ref_listing(sub, rest):
            block(f"creating, deleting, renaming or re-pointing a {sub} is not allowed")
    if sub == "help" and any(a not in {"-h", "-a", "--all", "-g", "--guides", "--config",
                                      "--user-interfaces", "--developer-interfaces", "--config-for-completion",
                                      "--verbose", "--no-verbose"} for a in rest):
        block("git help viewers execute external programs; use 'git <command> -h' or 'git help -a/-g'")
    if sub == "worktree" and any(a in ("add", "remove", "prune", "move", "lock", "unlock", "repair") for a in rest):
        block("worktree mutation is not allowed")
    if sub == "config" and not any(a in ("--get", "--list", "-l", "--get-all", "--get-regexp") for a in option_args):
        block("'git config' writes are not allowed")
    if sub == "reflog" and rest[:1] in (["expire"], ["delete"], ["drop"], ["write"]):
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
    if "--help" in option_args:
        block("git --help opens an external viewer; use 'git <command> -h' for terminal usage")
    if any(a == "--output" or a.startswith("--output=") for a in option_args):
        block("git --output writes a file; pipe to a read-only program instead")
    if any(a in ("--ext-diff", "--textconv", "--open-files-in-pager")
           or a.startswith("--open-files-in-pager=") for a in option_args):
        block("git external diff/text conversion/pager execution is outside read-only inspection")
    if sub == "cat-file" and any(a.startswith("--") and len(a) > 2 and "--filters".startswith(a)
                                 for a in option_args):
        block("git cat-file --filters executes configured smudge filters; inspect the raw blob instead")
    if sub == "grep" and any(a == "-O" or (
            a.startswith("--") and a != "--text" and any(
                option.startswith(a) for option in ("--open-files-in-pager", "--textconv"))) for a in option_args):
        block("git grep pager/textconv options can execute helpers; use plain git grep")
    if sub not in GIT_READONLY:
        block(f"'git {sub}' is outside the read-only set")


def check_gh(args) -> None:
    if not args:
        return
    head = args[0]
    sub = next((a for a in args[1:] if not a.startswith("-")), None)
    if head == "api":
        options = list(option_values(args[1:], "XHfFqtp",
                       {"--method", "--header", "--field", "--raw-field", "--input", "--jq",
                        "--template", "--preview", "--hostname", "--cache"}, short_equals=True))
        methods = [value for name, value in options if name in ("-X", "--method")]
        if not methods or any(method != "GET" for method in methods):
            block("'gh api' may mutate; every explicit method must be GET")
        if any(name in ("--input", "-f", "-F", "--field", "--raw-field") for name, _ in options):
            block("'gh api' with a request body is not a read; drop --input/-f/-F")
        return
    if (head, sub) in GH_ALLOWED or (head, None) in GH_ALLOWED and head.startswith("--"):
        json_values = {"--jq", "--json", "--template"}
        repo_values = json_values | {"--repo"}
        grammars = {
            ("pr", "view"): ("Rqt", repo_values),
            ("pr", "list"): ("RaABHqlLSst", repo_values | {"--app", "--assignee", "--author", "--base", "--head", "--label", "--limit", "--search", "--state"}),
            ("pr", "diff"): ("Re", {"--repo", "--exclude", "--color"}),
            ("pr", "checks"): ("Riqt", repo_values | {"--interval"}),
            ("pr", "status"): ("Rqt", repo_values),
            ("issue", "view"): ("Rqt", repo_values),
            ("issue", "list"): ("RaAqlLmSst", repo_values | {"--app", "--assignee", "--author", "--label", "--limit", "--mention", "--milestone", "--search", "--state", "--type"}),
            ("repo", "view"): ("bqt", json_values | {"--branch"}),
            ("run", "view"): ("Rajqt", repo_values | {"--attempt", "--job"}),
            ("run", "list"): ("RbceqLstuw", repo_values | {"--branch", "--commit", "--created", "--event", "--limit", "--status", "--user", "--workflow"}),
            ("auth", "status"): ("h", json_values | {"--hostname"}),
        }
        short_values, long_values = grammars.get((head, sub), ("", set()))
        booleans = {"t"} if (head, sub) == ("auth", "status") else {"w"} - set(short_values)
        options = list(option_values(args, short_values, long_values, short_booleans=booleans, short_equals=True))
        false_values = {"false", "False", "FALSE", "f", "F", "0"}
        if any(name in ({"--web"} | ({"-w"} if "w" not in short_values else set())) and value not in false_values for name, value in options):
            block("gh --web launches an external browser; inspect terminal output instead")
        if (head, sub) == ("auth", "status") and any(name in ("-t", "--show-token") and value not in false_values for name, value in options):
            block("gh auth status --show-token exposes credentials; use the default masked status")
        return
    block(f"'gh {head} {sub or ''}'.strip() is not in the read-only gh set (pr view/list/diff/checks, issue view/list, repo view, api -X GET)")


def check_gradle(args) -> None:
    list(option_values(args, long_values={"--console", "--warning-mode", "--dependency", "--configuration"}))
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
            require_literal(arg)
            expressions.append(arg.split("=", 1)[1])
            positional_expression = False
        elif arg.startswith("-e"):
            require_literal(arg)
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
        require_literal(expression)
        if re.fullmatch(r"\s*(?:" + address + r")?\s*!?\s*[pPdDqQ=]\s*", expression):
            continue
        # A complete substitution with only non-executing, non-writing flags.
        if re.fullmatch(r"s([^\w\s\\])(?:\\.|(?!\1)[^\\])*\1(?:\\.|(?!\1)[^\\])*\1[gIp0-9]*", expression):
            continue
        block("sed expression is outside display ranges and plain substitutions; use rg or separate inspection commands")


def check_xcodebuild(args, mode) -> None:
    values = {"--project", "--workspace", "--scheme", "--configuration", "--sdk", "--destination", "--arch",
              "--derivedDataPath", "--resultBundlePath", "--resultStreamPath", "--jobs", "--target", "--toolchain", "--xcconfig"}
    list(option_values(args, long_values=values, single_dash_long=True, terminator=False))
    for index, arg in enumerate(args):
        if arg in ("-derivedDataPath", "-resultBundlePath", "-resultStreamPath"):
            if index + 1 == len(args):
                block(f"{arg} needs a literal external evidence path")
            check_evidence_path(args[index + 1])
    if "clean" in args:
        block("'xcodebuild clean' discards build outputs; build the specific scheme instead")
    if any(a in ("archive", "-exportArchive", "-exportLocalizations", "-importLocalizations") for a in args):
        block("archiving/exporting with xcodebuild is release work, not verification")
    if any(a in ("test", "test-without-building", "build-for-testing", "install", "installsrc", "installhdrs", "analyze",
                 "-runFirstLaunch", "-downloadPlatform", "-downloadAllPlatforms", "-downloadComponent",
                 "-importPlatform", "-importComponent", "-license", "-allowProvisioningUpdates",
                 "-allowProvisioningDeviceRegistration") for a in args):
        block("Xcode tests, installation, analysis and tool setup are outside scoped compilation")
    if mode == "readonly-compile" and "-showdestinations" in args:
        block("Xcode device destination queries belong to runtime verification")


def check_simctl(args, mode) -> None:
    for arg in args:
        require_literal(arg)
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
    if mode == "readonly-compile" and prog in {"adb", "emulator", "simctl", "xcrun", "kill", "pkill", "open", "sips", "plutil"}:
        block(f"'{prog}' is outside compilation; device work belongs to runtime-verifier")
    if prog in INTERPRETERS:
        block(f"'{prog}' can execute arbitrary code; not allowed in {mode} mode")
    if prog == "python3":
        script = args[1] if args[:1] == ["-I"] and len(args) > 1 else None
        checker = os.path.realpath(os.path.join(PROJECT_DIR, ".claude", "hooks", "check-rulebook.py"))
        if script is None or os.path.realpath(os.path.expanduser(script)) != checker or len(args) != 2:
            block("python3 is allowed only as 'python3 -I .claude/hooks/check-rulebook.py'")
    if prog == "rg":
        options = option_values(args, "efgtrmABCMjTdE", {"--regexp", "--file", "--glob", "--iglob", "--encoding",
                                "--type", "--type-not", "--type-add", "--type-clear", "--replace", "--threads",
                                "--pre", "--pre-glob", "--hostname-bin", "--context", "--before-context",
                                "--after-context", "--max-count", "--max-depth", "--max-columns", "--sort", "--sortr",
                                "--ignore-file", "--context-separator", "--field-context-separator", "--field-match-separator",
                                "--hyperlink-format", "--dfa-size-limit", "--engine", "--regex-size-limit", "--max-filesize",
                                "--color", "--colors", "--path-separator", "--generate"})
        if any(name in ("--pre", "--hostname-bin") for name, _ in options):
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
        if args:
            require_literal(args[0])
        if args and args[0].startswith("-"):
            block("unsupported adb selector; use -s emulator-<port> or -e")
        if mode == "readonly-build" and args[:1] != ["devices"] and not selected_emulator:
            block("adb actions need an explicit -s emulator-<port> serial; inspect adb devices first")
    if prog == "sed":
        list(option_values(args, "e", {"--expression"}))
        check_sed(args)
    if prog == "awk":
        list(option_values(args, "Fv", {"--field-separator", "--assign"}))
        index = 0
        while index < len(args):
            arg = args[index]
            if arg in ("-F", "-v", "--field-separator", "--assign"):
                index += 2
                continue
            if arg == "--":
                index += 1
                if index < len(args):
                    require_literal(args[index])
                break
            if not arg.startswith("-"):
                require_literal(arg)
                break
            index += 1
    if prog == "awk" and any((re.search(r"\bsystem\b|\bgetline\b|@(?:include|load)", a)
                              or ">" in a or "|" in a or a == "-f" or a.startswith(("-f", "--file"))) for a in args):
        block("awk with system()/redirection/getline is not allowed")
    if prog == "sort":
        options = option_values(args, "kSoTt", {"--key", "--buffer-size", "--output", "--temporary-directory",
                                "--field-separator", "--compress-program", "--batch-size", "--parallel", "--random-source", "--files0-from"}, abbreviations=True)
        if any(name == "-o" or (len(name) > 2 and name.startswith("--") and
               any(option.startswith(name) for option in ("--output", "--compress-program"))) for name, _ in options):
            block("sort output files and compression helpers are outside read-only inspection; pipe plain sort instead")
    if prog == "file":
        options = option_values(args, "mMeFfP", {"--magic-file", "--exclude", "--exclude-quiet", "--separator", "--files-from", "--parameter"}, abbreviations=True)
        if any(name == "-C" or (len(name) > 2 and name.startswith("--") and "--compile".startswith(name)) for name, _ in options):
            block("file -C compiles and writes a magic database; use plain file inspection")
    if prog == "xmllint":
        options = list(option_values(args, "o", {"--output", "--path", "--dtdvalid", "--dtdvalidfpi", "--maxmem",
                                    "--encode", "--pretty", "--pattern", "--relaxng", "--schema", "--schematron", "--xpath"}, single_dash_long=True))
        names = {name for name, _ in options}
        if names & {"--output", "-o", "--shell"}:
            block("xmllint output files and its interactive shell can write; use plain XML inspection")
        network_options = {"--noent", "--valid", "--postvalid", "--dtdvalid", "--dtdvalidfpi", "--loaddtd",
                           "--dtdattr", "--xinclude", "--schema", "--relaxng", "--schematron", "--catalogs"}
        network_options |= {"--c14n", "--c14n11", "--exc-c14n"}
        if "--nonet" not in names and (names & network_options or any(re.match(r"(?:https?|ftp)://", arg, re.I) for arg in args)):
            block("xmllint URI/entity/schema loading needs --nonet; read-only roles cannot use the network")
    if prog == "find":
        data_options = {"-f", "-name", "-iname", "-path", "-ipath", "-regex", "-iregex", "-type", "-size",
                        "-maxdepth", "-mindepth", "-mtime", "-mmin", "-newer", "-user", "-group", "-perm"}
        index = 0
        while index < len(args):
            arg = args[index]
            if getattr(arg, "may_split", False) or (getattr(arg, "dynamic", False) and not arg.startswith(("/", "./", "../"))):
                require_literal(arg)
            if arg in data_options and index + 1 < len(args):
                index += 1
                if getattr(args[index], "may_split", False):
                    block("unquoted find values can introduce predicates; quote the data value")
            index += 1
        if any(a in ("-delete", "-exec", "-execdir", "-ok", "-okdir", "-fprint", "-fprint0", "-fprintf", "-fls") for a in args):
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
    if mode not in ("readonly", "readonly-compile", "readonly-build", "no-git-write"):
        block(f"unknown guard mode '{mode}'")
    try:
        payload = json.load(sys.stdin)
    except Exception as exc:  # malformed input: fail closed
        block(f"invalid hook payload ({exc})")
    if not isinstance(payload, dict):
        block("hook payload must be a JSON object")
    if not isinstance(payload.get("tool_name"), str) or not payload["tool_name"].strip():
        block("hook payload must identify the tool; use tool_name Bash for command checks")
    if payload.get("tool_name") != "Bash":
        sys.exit(0)
    tool_input = payload.get("tool_input")
    cmd = tool_input.get("command") if isinstance(tool_input, dict) else None
    if not isinstance(cmd, str) or "\x00" in cmd or any(0xd800 <= ord(ch) <= 0xdfff for ch in cmd):
        block("Bash call needs a Unicode string command without NUL or surrogate code points")
    if not cmd.strip():
        sys.exit(0)

    masked, spans = mask_quotes(cmd)
    if has_command_substitution(unmask(masked, spans, keep_quotes=True)):
        block("executable or unsupported parameter expansion can hide operations; use literals and simple quoted data")
    if re.search(r"[<>]\(", masked):
        block("process substitution is not allowed")

    tokens = tokenize(masked)
    redirects = []
    segments = split_segments(tokens, redirects)
    check_redirects(redirects, spans, mode)

    for seg in segments:
        seg = [ShellWord(unmask(t, spans), *expansion_flags(unmask(t, spans, keep_quotes=True))) for t in seg]
        prog, args = strip_prefixes(seg, mode)
        if prog is None:
            continue
        if prog == "git":
            check_git(args)
            continue
        if prog == "gh":
            if mode in ("readonly", "readonly-compile"):
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
            check_xcodebuild(args, mode)
            continue
        if mode in ("readonly", "readonly-compile", "readonly-build"):
            check_readonly_program(prog, args, mode)
    sys.exit(0)


if __name__ == "__main__":
    main()
