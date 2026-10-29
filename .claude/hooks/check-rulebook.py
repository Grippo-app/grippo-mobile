#!/usr/bin/env python3
"""Validate the Grippo AI rulebook: links, anchors, skill/agent frontmatter, roster sync.

Run from the repository root:
    python3 -I .claude/hooks/check-rulebook.py

Checks:
  1. Every relative markdown link outside fenced examples in CLAUDE.md, AGENTS.md, .claude/README.md,
     .claude/skills/**/*.md, .claude/agents/*.md and docs/**/*.md resolves to an
     existing file, and every #anchor matches a heading in the target.
  2. Every .claude/skills/<name>/SKILL.md has supported frontmatter with name ==
     directory, a non-empty description and an explicit completion section.
  3. Every .claude/agents/<name>.md has valid identity, model, tool rights,
     preloaded skills and a canonical PreToolUse Bash guard for its rights.
     Every backticked `.claude/...` path in the body (with {a,b} brace expansion)
     exists.
  4. The actual tables in CLAUDE.md's Skills index and Workflow and agents
     sections match their files; roster skills match each required skill list.
  5. No `.claude/skills/*/agents/openai.yaml` leftovers and no empty skill dirs.
  6. Required entry points exist; CLAUDE.md stays a map: at most 400 lines.
Exit code 1 when any problem is found.
"""
from fnmatch import fnmatchcase
import json
import os
import re
import sys

ROOT = os.getcwd()
problems = []


def rel(p):
    return os.path.relpath(p, ROOT)


def md_files():
    files = [os.path.join(ROOT, f) for f in ("CLAUDE.md", "AGENTS.md", ".claude/README.md") if os.path.isfile(os.path.join(ROOT, f))]
    for base in (".claude/skills", ".claude/agents", "docs"):
        for dp, _, fn in os.walk(os.path.join(ROOT, base)):
            files += [os.path.join(dp, f) for f in fn if f.endswith(".md") and os.path.isfile(os.path.join(dp, f))]
    return sorted(set(files))


def slug(h):
    """GitHub-style heading slug: lowercase, drop punctuation, each space -> '-'."""
    h = h.strip().lower().replace("`", "")
    h = re.sub(r"[^\w\s-]", "", h)
    return h.replace(" ", "-")


_markdown_lines = {}
FRONTMATTER = re.compile(r"---\n(.*?)\n---(?:\n|$)", re.S)


def markdown_lines(path):
    """Read body lines outside metadata/fenced examples, retaining source numbers."""
    if path not in _markdown_lines:
        lines = []
        with open(path, encoding="utf-8") as f:
            text = f.read()
            metadata = FRONTMATTER.match(text)
            metadata_lines = len(metadata.group(0).splitlines()) if metadata else 0
            fence = None
            for number, line in enumerate(text.splitlines(keepends=True), 1):
                if number <= metadata_lines:
                    continue
                marker = re.match(r"^ {0,3}(`{3,}|~{3,})(.*)$", line)
                if marker:
                    run, rest = marker.groups()
                    if fence is None:
                        fence = run
                    elif run[0] == fence[0] and len(run) >= len(fence) and not rest.strip():
                        fence = None
                    lines.append((number, "\n"))
                    continue
                if fence:
                    continue
                lines.append((number, line))
        _markdown_lines[path] = lines
    return _markdown_lines[path]


def atx_heading(line):
    match = re.match(r"^ {0,3}(#{1,6})(?:[ \t]+(.*)|[ \t]*)$", line.rstrip("\r\n"))
    if not match:
        return None
    title = re.sub(r"(?:^|[ \t]+)#+[ \t]*$", "", match.group(2) or "").strip()
    return len(match.group(1)), title


def headings_of(path):
    return [heading for _, line in markdown_lines(path) if (heading := atx_heading(line))]


def section_lines(path, title):
    active = False
    for number, line in markdown_lines(path):
        heading = atx_heading(line)
        if heading and heading[0] <= 2:
            active = heading[0] == 2 and slug(heading[1]) == slug(title)
        elif active:
            yield number, line


def table_cells(line):
    if not re.match(r"^ {0,3}\|", line):
        return None
    cells = re.split(r"(?<!\\)\|", line.strip())[1:]
    if cells and not cells[-1]:
        cells.pop()
    return [cell.strip() for cell in cells]


def table_rows(path, section, header):
    pending, active = False, False
    for number, line in section_lines(path, section):
        cells = table_cells(line)
        if cells == header:
            pending, active = True, False
        elif pending:
            pending = False
            active = bool(cells and len(cells) == len(header)
                          and all(re.fullmatch(r":?-+:?", cell) for cell in cells))
        elif active:
            if cells is None:
                active = False
            else:
                yield number, cells


def anchors_of(path):
    return {slug(title) for _, title in headings_of(path)}


LINK = re.compile(r"\[([^\]]*)\]\(([^)\s]+)\)")


def check_links():
    for t in md_files():
        for ln, line in markdown_lines(t):
            for _, href in LINK.findall(line):
                if href.startswith(("http://", "https://", "mailto:")):
                    continue
                path, _, frag = href.partition("#")
                tgt = os.path.normpath(os.path.join(os.path.dirname(t), path)) if path else t
                if not os.path.exists(tgt):
                    problems.append(f"missing file: {rel(t)}:{ln} -> {href}")
                    continue
                if frag and tgt.endswith(".md"):
                    if not os.path.isfile(tgt):
                        problems.append(f"anchor target is not a regular Markdown file: {rel(t)}:{ln} -> {href}")
                    elif frag not in anchors_of(tgt):
                        problems.append(f"missing anchor: {rel(t)}:{ln} -> {href}")


def frontmatter(path):
    """Read scalar fields and flat lists; retain the nested hooks block verbatim."""
    with open(path, encoding="utf-8") as f:
        text = f.read()
    if not text.startswith("---\n"):
        return None
    match = FRONTMATTER.match(text)
    if not match:
        return None
    data, current = {}, None
    for ln, line in enumerate(match.group(1).splitlines(), 2):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        m = re.match(r"^([A-Za-z][A-Za-z0-9_-]*):\s*(.*)$", line)
        if m:
            current = m.group(1)
            if current in data:
                problems.append(f"duplicate frontmatter field: {rel(path)}:{ln} {current}")
                return None
            raw = m.group(2).strip()
            if raw.startswith("#"):
                raw = ""
            value = scalar(raw, f"{rel(path)}:{ln}")
            if value is None:
                return None
            data[current] = [] if not raw else value
            continue
        if current == "hooks" and isinstance(data[current], list):
            data[current].append(line)
            continue
        item = re.match(r"^  -\s+(.+)$", line)
        if item and current and isinstance(data[current], list):
            value = scalar(item.group(1).strip(), f"{rel(path)}:{ln}")
            if value is None:
                return None
            data[current].append(value)
            continue
        problems.append(f"unsupported frontmatter syntax: {rel(path)}:{ln}; use scalar fields or two-space lists")
        return None
    return data


NON_STRING = object()
YAML_NON_STRING = re.compile(
    r"(?:~|null|true|false|yes|no|on|off|[-+]?(?:\.(?:inf|nan)|0x[0-9a-f_]+|0o[0-7_]+|0b[01_]+|"
    r"[0-9][0-9_]*(?::[0-5]?[0-9])+(?:\.[0-9_]*)?|"
    r"(?:[0-9][0-9_]*(?:\.[0-9_]*)?|\.[0-9_]+)(?:e[-+]?[0-9_]+)?))", re.I)


def scalar(value, where):
    """Read the supported one-line YAML scalar subset, including trailing comments."""
    if value.startswith("'"):
        match = re.fullmatch(r"'((?:[^']|'')*)'(?:[ \t]+#.*)?", value)
        if match:
            return match.group(1).replace("''", "'")
        problems.append(f"invalid single-quoted scalar: {where}; close quotes and double internal apostrophes")
        return None
    if value.startswith('"'):
        try:
            decoded, end = json.JSONDecoder().raw_decode(value)
            if value[end:] and not re.fullmatch(r"[ \t]+#.*", value[end:]):
                raise ValueError("text after closing quote")
            return decoded
        except ValueError:
            problems.append(f"invalid double-quoted scalar: {where}; use JSON-style escapes or YAML single quotes")
            return None
    value = re.split(r"(?:^|[ \t]+)#", value, maxsplit=1)[0].rstrip()
    if value.lower() in ("true", "false"):
        return value.lower() == "true"
    if re.search(r":(?:\s|$)", value) or re.match(r"[-?](?:\s|$)", value) or value.startswith(("[", "]", "{", "}", ",", "&", "*", "!", "|", ">", "@", "`", "%")):
        problems.append(f"unsupported plain YAML scalar: {where}; quote text containing YAML syntax")
        return None
    if YAML_NON_STRING.fullmatch(value) or re.fullmatch(
            r"[0-9]{4}-[0-9]{2}-[0-9]{2}(?:(?:[Tt]| +)[0-9]{1,2}:[0-9]{2}:[0-9]{2}"
            r"(?:\.[0-9]*)?(?: *(?:[Zz]|[-+][0-9]{1,2}(?::[0-9]{2})?))?)?", value):
        return NON_STRING
    return value


def names_of(fm, key):
    value = fm.get(key, "")
    if isinstance(value, list):
        return [v.strip() for v in value if v.strip()] if all(isinstance(v, str) for v in value) else []
    return [x.strip() for x in value.split(",") if x.strip()] if isinstance(value, str) else []


WRITE_TOOLS = {"Edit", "Write", "NotebookEdit"}
LOCAL_TOOLS = {"Read", "Grep", "Glob", "Bash"} | WRITE_TOOLS
VERIFIERS = {"build-verifier", "runtime-verifier"}
MCP_ALLOWANCES = {
    "analytics-admin-auditor": ("mcp__claude-in-chrome__*",),
    "runtime-verifier": ("mcp__Claude_Code_iOS_Simulator__control", "mcp__Claude_Code_iOS_Simulator__build"),
}


def check_agent_rights(path, stem, fm):
    label = rel(path)
    if fm.get("model") not in ("inherit", "sonnet"):
        problems.append(f"agent needs model inherit or sonnet: {label}")
    tools = set(names_of(fm, "tools"))
    denied = set(names_of(fm, "disallowedTools"))
    if not tools or any(not re.fullmatch(r"(?:[A-Za-z][A-Za-z0-9_]*|mcp__[A-Za-z0-9_*-]+)", t) for t in tools):
        problems.append(f"agent needs a non-empty tool allowlist: {label}")
    supported = LOCAL_TOOLS | ({"WebFetch"} if stem == "backend-contract-auditor" else set())
    unknown = tools - supported - {t for t in tools if t.startswith("mcp__")}
    if unknown:
        problems.append(f"agent allows tools outside its role's capabilities: {label}; remove {', '.join(sorted(unknown))}")
    if "Agent" not in denied or "Agent" in tools:
        problems.append(f"agent must exclude Agent from tools and include it in disallowedTools: {label}")
    mcp_tools = {t for t in tools if t.startswith("mcp__")}
    extra_mcp = {t for t in mcp_tools if not any(fnmatchcase(t, p) for p in MCP_ALLOWANCES.get(stem, ()))}
    if extra_mcp:
        problems.append(f"agent allows MCP tools outside its role's capabilities: {label}; remove {', '.join(sorted(extra_mcp))}")
    if (stem not in MCP_ALLOWANCES or not mcp_tools) and "mcp__*" not in denied:
        problems.append(f"agent without MCP tools must disallow mcp__*: {label}")
    readonly = stem == "scout" or stem in VERIFIERS or stem.endswith(("-reviewer", "-auditor"))
    if readonly and (tools & WRITE_TOOLS or not WRITE_TOOLS <= denied):
        problems.append(f"read-only agent must exclude Edit, Write and NotebookEdit from tools and disallow them: {label}")
    mode = "readonly-build" if stem in VERIFIERS else "no-git-write" if tools & WRITE_TOOLS and not readonly else "readonly"
    check_agent_guard(path, fm.get("hooks"), mode)


def check_agent_guard(path, lines, mode):
    expected = f'python3 "$CLAUDE_PROJECT_DIR/.claude/hooks/bash-guard.py" {mode}'
    label = rel(path)
    # Accept the one canonical hook, with YAML quoting and consistent indentation.
    # Extra keys (async, alternate events, etc.) can change whether the guard runs.
    patterns = (r" +PreToolUse:(?: +#.*)?", r" +- matcher: (.+)", r" +hooks:(?: +#.*)?", r" +- type: (.+)", r" +command: (.+)")
    matches = [re.fullmatch(p, line) for p, line in zip(patterns, lines)] if isinstance(lines, list) else []
    if len(matches) != 5 or len(lines) != 5 or not all(matches):
        problems.append(f"agent needs one PreToolUse Bash command hook: {label}; command: {expected}")
        return
    indent = [len(line) - len(line.lstrip(" ")) for line in lines]
    if not (indent[1] > indent[0] and indent[2] == indent[1] + 2 and indent[3] > indent[2] and indent[4] == indent[3] + 2):
        problems.append(f"invalid Bash guard hook indentation: {label}")
        return
    matcher = scalar(matches[1].group(1), f"{label} hook matcher")
    hook_type = scalar(matches[3].group(1), f"{label} hook type")
    command = scalar(matches[4].group(1), f"{label} hook command")
    if None in (matcher, hook_type, command):
        return
    if matcher != "Bash" or hook_type != "command":
        problems.append(f"agent guard must match Bash and use type command: {label}")
    if command != expected:
        problems.append(f"agent guard must run exact command: {label}; expected: {expected}")


BACKTICK_PATH = re.compile(r"`(\.claude/[^`\s]+)`")


def expand_braces(path):
    m = re.search(r"\{([^{}]+)\}", path)
    if not m:
        return [path]
    out = []
    for alt in m.group(1).split(","):
        out += expand_braces(path[:m.start()] + alt.strip() + path[m.end():])
    return out


def check_backtick_paths(path):
    text = open(path, encoding="utf-8").read()
    for raw in BACKTICK_PATH.findall(text):
        for candidate in expand_braces(raw):
            candidate = candidate.rstrip(".,;:")
            if candidate.endswith("/") or "*" in candidate or "<" in candidate:
                continue
            if not os.path.exists(os.path.join(ROOT, candidate)):
                problems.append(f"missing backtick path: {rel(path)} -> {candidate}")


def check_skills():
    base = os.path.join(ROOT, ".claude/skills")
    names = []
    if not os.path.isdir(base):
        problems.append("missing .claude/skills directory")
        return set()
    for d in sorted(os.listdir(base)):
        full = os.path.join(base, d)
        if not os.path.isdir(full):
            continue
        skill = os.path.join(full, "SKILL.md")
        if not os.path.isfile(skill):
            problems.append(f"skill dir without SKILL.md file: .claude/skills/{d}")
            continue
        fm = frontmatter(skill)
        if not fm:
            problems.append(f"skill without frontmatter: .claude/skills/{d}/SKILL.md")
            continue
        if fm.get("name") != d:
            problems.append(f"skill name mismatch: .claude/skills/{d}/SKILL.md name={fm.get('name')!r}")
        if not isinstance(fm.get("description"), str) or not fm["description"].strip():
            problems.append(f"skill needs a non-empty scalar string description: .claude/skills/{d}/SKILL.md")
        unknown = set(fm) - {"name", "description", "disable-model-invocation", "user-invocable"}
        if unknown:
            problems.append(f"unsupported skill frontmatter fields: .claude/skills/{d}/SKILL.md; {', '.join(sorted(unknown))}")
        for key in ("disable-model-invocation", "user-invocable"):
            if key in fm and not isinstance(fm[key], bool):
                problems.append(f"skill {key} must be a YAML boolean: .claude/skills/{d}/SKILL.md")
        if not any(level == 2 and re.search(r"\b(?:gate|gates|completion|checks|hand-off)\b", title, re.I)
                   for level, title in headings_of(skill)):
            problems.append(f"skill needs an explicit completion section or pointer: .claude/skills/{d}/SKILL.md")
        if os.path.exists(os.path.join(full, "agents", "openai.yaml")):
            problems.append(f"stale Codex metadata: .claude/skills/{d}/agents/openai.yaml")
        names.append(d)
    return set(names)


def check_agents(skill_names):
    base = os.path.join(ROOT, ".claude/agents")
    agents = set()
    if not os.path.isdir(base):
        problems.append("missing .claude/agents directory")
        return agents
    for f in sorted(os.listdir(base)):
        if not f.endswith(".md"):
            continue
        path = os.path.join(base, f)
        if not os.path.isfile(path):
            problems.append(f"agent definition is not a regular file: .claude/agents/{f}")
            continue
        fm = frontmatter(path)
        stem = f[:-3]
        if not fm:
            problems.append(f"agent without frontmatter: .claude/agents/{f}")
            continue
        if fm.get("name") != stem:
            problems.append(f"agent name mismatch: .claude/agents/{f} name={fm.get('name')!r}")
        if not isinstance(fm.get("description"), str) or not fm["description"].strip():
            problems.append(f"agent needs a non-empty scalar string description: .claude/agents/{f}")
        unknown = set(fm) - {"name", "description", "model", "color", "tools", "disallowedTools", "skills", "hooks"}
        if unknown:
            problems.append(f"unsupported agent frontmatter fields: .claude/agents/{f}; {', '.join(sorted(unknown))}")
        for heading in ("Rights", "Inputs you must receive", "Duties", "Report format"):
            if not any(level == 2 and re.fullmatch(re.escape(heading) + r"(?:\s+-.*)?", title)
                       for level, title in headings_of(path)):
                problems.append(f"agent needs a {heading} section: .claude/agents/{f}")
        if "skills" in fm:
            if not isinstance(fm["skills"], list) or not fm["skills"]:
                problems.append(f"agent skills must be a non-empty two-space YAML list: .claude/agents/{f}")
            else:
                for s in fm["skills"]:
                    if not isinstance(s, str) or not s.strip():
                        problems.append(f"agent skill name must be a non-empty scalar string: .claude/agents/{f}")
                    elif s not in skill_names:
                        problems.append(f"agent preloads unknown skill: .claude/agents/{f} skills={s}")
        check_agent_rights(path, stem, fm)
        check_backtick_paths(path)
        agents.add(stem)
    return agents


def check_roster(agents):
    claude = os.path.join(ROOT, "CLAUDE.md")
    if not os.path.isfile(claude):
        return
    rows = list(table_rows(claude, "Workflow and agents", ["Agent", "Role", "Rights", "Skills"]))
    entries = [(number, columns, re.fullmatch(r"`([^`]+)`", columns[0])) for number, columns in rows if columns]
    names = [match.group(1) for _, _, match in entries if match]
    listed = set(names)
    for name in sorted(listed):
        if names.count(name) > 1:
            problems.append(f"duplicate agent in CLAUDE.md roster: {name}")
    for a in sorted(agents - listed):
        problems.append(f"agent not in CLAUDE.md roster table: {a}")
    for a in sorted(listed - agents):
        problems.append(f"roster lists missing agent: {a}")
    for number, columns, match in entries:
        if not match:
            problems.append(f"invalid agent identity in CLAUDE.md roster at line {number}")
            continue
        if match.group(1) not in agents:
            continue
        name = match.group(1)
        if len(columns) != 4:
            problems.append(f"agent roster row needs Agent/Role/Rights/Skills columns: {name} at CLAUDE.md:{number}")
            continue
        listed_skills = {s.strip() for s in columns[-1].split(",") if s.strip() not in ("", "-")}
        fm = frontmatter(os.path.join(ROOT, ".claude", "agents", name + ".md"))
        required_skills = set(names_of(fm or {}, "skills"))
        if listed_skills != required_skills:
            problems.append(f"agent roster skills differ from definition: {name}; expected {', '.join(sorted(required_skills)) or '-'}")


def check_skill_index(skills):
    claude = os.path.join(ROOT, "CLAUDE.md")
    if not os.path.isfile(claude):
        return
    listed = set()
    for number, columns in table_rows(claude, "Skills index", ["Skill", "Owns", "Apply when"]):
        if not columns:
            continue
        match = re.fullmatch(r"\[([^]]+)\]\(\.claude/skills/([^/]+)/SKILL\.md\)", columns[0])
        if not match:
            problems.append(f"invalid skill identity in CLAUDE.md index at line {number}")
            continue
        label, name = match.groups()
        if len(columns) != 3:
            problems.append(f"skill index row needs Skill/Owns/Apply when columns: {name} at CLAUDE.md:{number}")
        if label != name:
            problems.append(f"skill index label differs from directory: {label} -> {name}")
        if name in listed:
            problems.append(f"duplicate skill in CLAUDE.md index: {name}")
        listed.add(name)
    for name in sorted(skills - listed):
        problems.append(f"skill not in CLAUDE.md index: {name}")
    for name in sorted(listed - skills):
        problems.append(f"skill index lists missing skill: {name}")


def check_map_size(limit=400):
    claude = os.path.join(ROOT, "CLAUDE.md")
    if os.path.isfile(claude):
        n = sum(1 for _ in open(claude, encoding="utf-8"))
        if n > limit:
            problems.append(f"CLAUDE.md has {n} lines; the map must stay at most {limit} (move detail to the owning skill)")


def main():
    for path in ("CLAUDE.md", "AGENTS.md", ".claude/README.md", ".claude/hooks/bash-guard.py"):
        if not os.path.isfile(os.path.join(ROOT, path)):
            problems.append(f"missing required rulebook file: {path}")
    check_links()
    skills = check_skills()
    agents = check_agents(skills)
    check_roster(agents)
    check_skill_index(skills)
    check_map_size()
    for p in problems:
        print(p)
    print(f"\n{len(problems)} problem(s); {len(skills)} skills; {len(agents)} agents; {len(md_files())} markdown files checked")
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
