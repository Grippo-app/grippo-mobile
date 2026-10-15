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
  3. Every .claude/agents/<name>.md has frontmatter with name == file stem, a
     description, every preloaded skill in `skills:` (string or YAML list) exists,
     the guard mode is known, and every backticked `.claude/...` path in the body
     (with {a,b} brace expansion) exists.
  4. Every agent named in the CLAUDE.md roster table exists as a file, and every
     agent file is listed in the roster.
  5. No `.claude/skills/*/agents/openai.yaml` leftovers and no empty skill dirs.
  6. CLAUDE.md stays a map: at most 400 lines.
Exit code 1 when any problem is found.
"""
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
    """Minimal YAML reader: scalar `key: value` and `key:` + `  - item` lists."""
    with open(path, encoding="utf-8") as f:
        text = f.read()
    if not text.startswith("---\n"):
        return None
    end = text.find("\n---", 4)
    if end < 0:
        return None
    block = text[4:end]
    data, current = {}, None
    for line in block.splitlines():
        m = re.match(r"^([A-Za-z][A-Za-z0-9_-]*):\s*(.*)$", line)
        if m:
            current = m.group(1)
            data[current] = m.group(2).strip()
            continue
        item = re.match(r"^\s+-\s+(.+)$", line)
        if item and current and data.get(current, "") in ("", None) or (item and isinstance(data.get(current), list)):
            if not isinstance(data.get(current), list):
                data[current] = []
            data[current].append(item.group(1).strip())
    return data


def skills_of(fm):
    value = fm.get("skills", "")
    if isinstance(value, list):
        return [v.strip() for v in value if v.strip()]
    return [x.strip() for x in value.split(",") if x.strip()]


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
        if not fm.get("description"):
            problems.append(f"skill without description: .claude/skills/{d}/SKILL.md")
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
        if not fm.get("description"):
            problems.append(f"agent without description: .claude/agents/{f}")
        for s in skills_of(fm):
            if s not in skill_names:
                problems.append(f"agent preloads unknown skill: .claude/agents/{f} skills={s}")
        hooks_cmd = re.search(r"bash-guard\.py\" (\w[\w-]*)", open(path, encoding="utf-8").read())
        if hooks_cmd and hooks_cmd.group(1) not in ("readonly", "readonly-build", "no-git-write"):
            problems.append(f"agent uses unknown guard mode: .claude/agents/{f} mode={hooks_cmd.group(1)}")
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
