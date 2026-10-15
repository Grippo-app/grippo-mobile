#!/usr/bin/env python3
"""Validate the Grippo AI rulebook: links, anchors, skill/agent frontmatter, roster sync.

Run from the repository root:
    python3 -I .claude/hooks/check-rulebook.py

Checks:
  1. Every relative markdown link in CLAUDE.md, AGENTS.md, .claude/README.md,
     .claude/skills/**/*.md, .claude/agents/*.md and docs/**/*.md resolves to an
     existing file, and every #anchor matches a heading in the target.
  2. Every .claude/skills/<name>/SKILL.md has frontmatter with name == directory
     and a non-empty description.
  3. Every .claude/agents/<name>.md has valid identity, model, tool rights,
     preloaded skills and a canonical PreToolUse Bash guard for its rights.
     Every backticked `.claude/...` path in the body (with {a,b} brace expansion)
     exists.
  4. Every agent named in the CLAUDE.md roster table exists as a file, and every
     agent file is listed in the roster.
  5. No `.claude/skills/*/agents/openai.yaml` leftovers and no empty skill dirs.
  6. CLAUDE.md stays a map: at most 400 lines.
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
    files = [os.path.join(ROOT, f) for f in ("CLAUDE.md", "AGENTS.md", ".claude/README.md") if os.path.exists(os.path.join(ROOT, f))]
    for base in (".claude/skills", ".claude/agents", "docs"):
        for dp, _, fn in os.walk(os.path.join(ROOT, base)):
            files += [os.path.join(dp, f) for f in fn if f.endswith(".md")]
    return sorted(set(files))


def slug(h):
    """GitHub-style heading slug: lowercase, drop punctuation, each space -> '-'."""
    h = h.strip().lower().replace("`", "")
    h = re.sub(r"[^\w\s-]", "", h)
    return h.replace(" ", "-")


_anchors = {}


def anchors_of(path):
    if path not in _anchors:
        s = set()
        with open(path, encoding="utf-8") as f:
            in_code = False
            for line in f:
                if line.startswith("```"):
                    in_code = not in_code
                    continue
                if in_code:
                    continue
                m = re.match(r"^(#{1,6})\s+(.*)$", line)
                if m:
                    s.add(slug(m.group(2)))
        _anchors[path] = s
    return _anchors[path]


LINK = re.compile(r"\[([^\]]*)\]\(([^)\s]+)\)")


def check_links():
    for t in md_files():
        with open(t, encoding="utf-8") as f:
            for ln, line in enumerate(f, 1):
                for _, href in LINK.findall(line):
                    if href.startswith(("http://", "https://", "mailto:")):
                        continue
                    path, _, frag = href.partition("#")
                    tgt = os.path.normpath(os.path.join(os.path.dirname(t), path)) if path else t
                    if not os.path.exists(tgt):
                        problems.append(f"missing file: {rel(t)}:{ln} -> {href}")
                        continue
                    if frag and tgt.endswith(".md") and frag not in anchors_of(tgt):
                        problems.append(f"missing anchor: {rel(t)}:{ln} -> {href}")


def frontmatter(path):
    """Read scalar fields and flat lists; retain the nested hooks block verbatim."""
    with open(path, encoding="utf-8") as f:
        text = f.read()
    if not text.startswith("---\n"):
        return None
    match = re.match(r"---\n(.*?)\n---(?:\n|$)", text, flags=re.S)
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
    for d in sorted(os.listdir(base)):
        full = os.path.join(base, d)
        if not os.path.isdir(full):
            continue
        skill = os.path.join(full, "SKILL.md")
        if not os.path.exists(skill):
            problems.append(f"skill dir without SKILL.md: .claude/skills/{d}")
            continue
        fm = frontmatter(skill)
        if not fm:
            problems.append(f"skill without frontmatter: .claude/skills/{d}/SKILL.md")
            continue
        if fm.get("name") != d:
            problems.append(f"skill name mismatch: .claude/skills/{d}/SKILL.md name={fm.get('name')!r}")
        if not isinstance(fm.get("description"), str) or not fm["description"].strip():
            problems.append(f"skill needs a non-empty scalar string description: .claude/skills/{d}/SKILL.md")
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
        fm = frontmatter(path)
        stem = f[:-3]
        if not fm:
            problems.append(f"agent without frontmatter: .claude/agents/{f}")
            continue
        if fm.get("name") != stem:
            problems.append(f"agent name mismatch: .claude/agents/{f} name={fm.get('name')!r}")
        if not isinstance(fm.get("description"), str) or not fm["description"].strip():
            problems.append(f"agent needs a non-empty scalar string description: .claude/agents/{f}")
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
    if not os.path.exists(claude):
        return
    text = open(claude, encoding="utf-8").read()
    listed = set(re.findall(r"^\| `([a-z][a-z0-9-]*)` \|", text, flags=re.M))
    for a in sorted(agents - listed):
        problems.append(f"agent not in CLAUDE.md roster table: {a}")
    for a in sorted(listed - agents):
        problems.append(f"roster lists missing agent: {a}")


def check_map_size(limit=400):
    claude = os.path.join(ROOT, "CLAUDE.md")
    if os.path.exists(claude):
        n = sum(1 for _ in open(claude, encoding="utf-8"))
        if n > limit:
            problems.append(f"CLAUDE.md has {n} lines; the map must stay at most {limit} (move detail to the owning skill)")


def main():
    if not os.path.isfile(os.path.join(ROOT, ".claude/hooks/bash-guard.py")):
        problems.append("missing guard file: .claude/hooks/bash-guard.py must be a regular file")
    check_links()
    skills = check_skills()
    agents = check_agents(skills)
    check_roster(agents)
    check_map_size()
    for p in problems:
        print(p)
    print(f"\n{len(problems)} problem(s); {len(skills)} skills; {len(agents)} agents; {len(md_files())} markdown files checked")
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
