#!/usr/bin/env python3
"""Isolated structural regression cases; never runs application checks."""
import json
import os
import pathlib
import runpy
import shutil
import subprocess
import sys
import tempfile

root = pathlib.Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parents[2]
checks = []

for container in ('.agents', '.agents/skills', '.codex', '.codex/agents'):
    path = root / container
    if path.is_symlink() or not path.is_dir():
        print('FAIL source native container must be a regular directory:', container)
        raise SystemExit(1)
for entry in ('CLAUDE.md', 'AGENTS.md'):
    if not (root / entry).is_file():
        print('FAIL source rulebook entry must be a regular file:', entry)
        raise SystemExit(1)

def row(source, prefix):
    return next(line for line in source.splitlines() if line.startswith(prefix))

def duplicate(source, prefix):
    line = row(source, prefix)
    return source.replace(line, line + '\n' + line)

def fence(source, prefix):
    line = row(source, prefix)
    return source.replace(line, '```markdown\n' + line + '\n```')

with tempfile.TemporaryDirectory(prefix='grippo-checker-deep-fixture-') as tmp:
    fixture = pathlib.Path(tmp)
    shutil.copytree(root / '.claude', fixture / '.claude', ignore=shutil.ignore_patterns('settings.local.json'))
    for name in ('CLAUDE.md', 'AGENTS.md'):
        shutil.copy2(root / name, fixture / name)
    for native in ('.agents', '.codex'):
        if (root / native).is_dir():
            shutil.copytree(root / native, fixture / native, symlinks=True)
    for child in root.iterdir():
        if child.name not in {'.claude', '.agents', '.codex', 'CLAUDE.md', 'AGENTS.md', '.git'}:
            (fixture / child.name).symlink_to(child, target_is_directory=child.is_dir())

    def run(label, expected, needle=''):
        result = subprocess.run([sys.executable, '-I', str(root / '.claude/hooks/check-rulebook.py')], cwd=fixture, capture_output=True, text=True)
        good = result.returncode == expected and (not needle or needle in result.stdout) and not result.stderr
        checks.append((label, good))
        print(('PASS' if good else 'FAIL'), label, 'exit', result.returncode)
        if not good:
            print(result.stdout, result.stderr)
        return good

    if not run('current valid rulebook', 0):
        raise SystemExit(1)  # Never mutate fixtures made from an invalid source baseline.
    skill = '.claude/skills/di-modules/SKILL.md'
    agent = '.claude/agents/code-reviewer.md'
    mutations = [
        ('missing AGENTS.md', 'AGENTS.md', None, 1, 'missing required rulebook file'),
        ('missing CLAUDE.md', 'CLAUDE.md', None, 1, 'missing required rulebook file'),
        ('duplicate actual roster row', 'CLAUDE.md', lambda s: duplicate(s, '| `scout` |'), 1, 'duplicate agent'),
        ('duplicate actual index row', 'CLAUDE.md', lambda s: duplicate(s, '| [di-modules]'), 1, 'duplicate skill'),
        ('unknown skill field', skill, lambda s: s.replace('name: di-modules', 'name: di-modules\ntoolz: Write'), 1, 'unsupported skill frontmatter'),
        ('removed completion section', skill, lambda s: s.split('## Completion gate')[0], 1, 'explicit completion section'),
        ('quoted optional boolean', skill, lambda s: s.replace('name: di-modules', 'name: di-modules\nuser-invocable: "false"'), 1, 'must be a YAML boolean'),
        ('non-string description', skill, lambda s: '\n'.join('description: true' if line.startswith('description:') else line for line in s.split('\n')), 1, 'non-empty scalar string'),
        ('invalid Unicode surrogate in metadata', skill, lambda s: '\n'.join('description: "\\ud800"' if line.startswith('description:') else line for line in s.split('\n')), 1, 'invalid double-quoted scalar'),
        ('unapproved agent tool', agent, lambda s: s.replace('tools: Read, Grep, Glob, Bash', 'tools: Read, Grep, Glob, Bash, WebFetch'), 1, 'outside its role'),
        ('missing agent duties', agent, lambda s: s.replace('## Duties', '## Work'), 1, 'needs a Duties section'),
        ('misspelled agent field', agent, lambda s: s.replace('model: inherit', 'model: inherit\ntoolz: Bash'), 1, 'unsupported agent frontmatter'),
        ('guard mode drift', '.claude/agents/build-verifier.md', lambda s: s.replace('readonly-compile', 'readonly-build'), 1, 'agent guard must run exact command'),
        ('non-isolated native guard', agent, lambda s: s.replace('python3 -I "$CLAUDE_PROJECT_DIR/', 'python3 "$CLAUDE_PROJECT_DIR/'), 1, 'agent guard must run exact command'),
        ('missing native adapter', '.codex/agents/code-reviewer.toml', None, 1, 'native agent adapter missing or drifted'),
        ('native adapter instructions drift', '.codex/agents/code-reviewer.toml', lambda s: s.replace('Read CLAUDE.md', 'Ignore CLAUDE.md'), 1, 'native agent adapter missing or drifted'),
        ('native capability drift', '.codex/agents/code-reviewer.toml', lambda s: s.replace('apps = false', 'apps = true'), 1, 'native agent adapter missing or drifted'),
        ('completion only inside YAML comment', skill, lambda s: s.replace('## Completion gate', '## Finalization').replace('---\n', '---\n## Completion gate\n', 1), 1, 'explicit completion section'),
        ('role section only inside YAML comment', agent, lambda s: s.replace('## Rights', '## Authority').replace('---\n', '---\n## Rights\n', 1), 1, 'needs a Rights section'),
        ('literal link in YAML comment ignored', skill, lambda s: s.replace('---\n', '---\n# [literal](absent.md)\n', 1), 0, ''),
        ('completion only inside fenced code', skill, lambda s: s.replace('## Completion gate', '```markdown\n## Completion gate\n```'), 1, 'explicit completion section'),
        ('role heading only inside fenced code', agent, lambda s: s.replace('## Rights', '~~~markdown\n## Rights\n~~~'), 1, 'needs a Rights section'),
        ('registry row only inside fenced code', 'CLAUDE.md', lambda s: fence(s, '| `scout` |'), 1, 'agent not in'),
        ('skill row only inside fenced code', 'CLAUDE.md', lambda s: fence(s, '| [di-modules]'), 1, 'skill not in'),
        ('registry row beyond owning table', 'CLAUDE.md', lambda s: s.replace(row(s, '| `scout` |'), '') + '\n' + row(s, '| `scout` |'), 1, 'agent not in'),
        ('registry table boundary preserved across example', 'CLAUDE.md', lambda s: s.replace(row(s, '| `scout` |'), '```text\nexample\n```\n' + row(s, '| `scout` |')), 1, 'agent not in'),
        ('invalid extra agent identity', 'CLAUDE.md', lambda s: s.replace(row(s, '| `scout` |'), '| malformed | role | rights | - |\n' + row(s, '| `scout` |')), 1, 'invalid agent identity'),
        ('invalid extra skill identity', 'CLAUDE.md', lambda s: s.replace(row(s, '| [di-modules]'), '| malformed | topic | trigger |\n' + row(s, '| [di-modules]')), 1, 'invalid skill identity'),
        ('registry example elsewhere is not a duplicate', 'CLAUDE.md', lambda s: s + '\n' + row(s, '| `scout` |'), 0, ''),
        ('index example elsewhere is not a duplicate', 'CLAUDE.md', lambda s: s + '\n' + row(s, '| [di-modules]'), 0, ''),
        ('indented completion heading', skill, lambda s: s.replace('## Completion gate', '   ## Completion gate'), 0, ''),
        ('closing ATX marker on real role section', agent, lambda s: s.replace('## Rights', '## Rights ##'), 0, ''),
        ('closing ATX marker preserves real anchors', skill, lambda s: s.replace('## Completion gate', '## Completion gate ##') + '\n[gate](#completion-gate)\n', 0, ''),
        ('missing actual Markdown target', skill, lambda s: s + '\n[missing](absent.md)\n', 1, 'missing file'),
        ('literal link in fenced example ignored', skill, lambda s: s + '\n```markdown\n[missing](absent.md)\n```\n', 0, ''),
    ]
    for label, relative, mutate, expected, needle in mutations:
        path = fixture / relative
        saved = path.read_bytes()
        if mutate is None:
            path.unlink()
        else:
            path.write_text(mutate(saved.decode()))
        run(label, expected, needle)
        path.write_bytes(saved)

    path = fixture / skill
    saved = path.read_bytes()
    for value in ('true', 'false', 'True', 'FALSE'):
        path.write_text(saved.decode().replace('name: di-modules', 'name: di-modules\nuser-invocable: ' + value))
        run('valid optional boolean ' + value, 0)
    path.write_bytes(saved)
    for relative, needle in [('AGENTS.md', 'missing required'), (skill, 'SKILL.md file'), (agent, 'not a regular file'), ('.claude/skills/ui-feature/SKILL.md', 'not a regular Markdown file')]:
        path = fixture / relative
        saved = path.read_bytes()
        reference = fixture / 'AGENTS.md'
        saved_reference = reference.read_bytes() if relative == '.claude/skills/ui-feature/SKILL.md' else None
        if saved_reference is not None:
            reference.write_bytes(saved_reference + b'\n[gate](.claude/skills/ui-feature/SKILL.md#completion-gate)\n')
        path.unlink()
        path.mkdir()
        run('directory instead of file ' + relative, 1, needle)
        path.rmdir()
        path.write_bytes(saved)
        if saved_reference is not None:
            reference.write_bytes(saved_reference)
    projection = fixture / '.agents/skills/di-modules'
    saved_target = projection.readlink()
    projection.unlink()
    run('missing native skill projection', 1, 'native skill projection must link')
    projection.symlink_to('../../.claude/skills/mappers', target_is_directory=True)
    run('wrong native skill target', 1, 'native skill projection must link')
    projection.unlink()
    projection.mkdir()
    run('copied native directory instead of canonical link', 1, 'native skill projection must link')
    projection.rmdir()
    projection.symlink_to(saved_target, target_is_directory=True)
    for relative in ('.agents/skills/extra', '.codex/agents/extra.toml'):
        extra = fixture / relative
        extra.write_text('extra')
        run('unexpected native member ' + relative, 1, 'unexpected native projection')
        extra.unlink()

    path = fixture / agent
    adapter = fixture / '.codex/agents/code-reviewer.toml'
    saved, saved_adapter = path.read_bytes(), adapter.read_bytes()
    replacement = "description: 'Review contracts: literal # data' # YAML comment"
    path.write_text('\n'.join(replacement if line.startswith('description:') else line for line in saved.decode().split('\n')))
    sync = subprocess.run([sys.executable, '-I', str(fixture / '.claude/hooks/sync-native-adapters.py'), '--write'],
                          cwd=fixture, capture_output=True, text=True)
    good = sync.returncode == 0 and not sync.stderr and 'description = "Review contracts: literal # data"' in adapter.read_text()
    checks.append(('native description uses canonical YAML scalar decoding', good))
    print('PASS' if good else 'FAIL', 'native description uses canonical YAML scalar decoding')
    run('valid quoted and commented description projection', 0)
    path.write_bytes(saved)
    adapter.write_bytes(saved_adapter)

    replacement = 'description: "Review \\u007f controls."'
    path.write_text('\n'.join(replacement if line.startswith('description:') else line for line in saved.decode().split('\n')))
    sync = subprocess.run([sys.executable, '-I', str(fixture / '.claude/hooks/sync-native-adapters.py'), '--write'],
                          cwd=fixture, capture_output=True, text=True)
    good = sync.returncode == 0 and not sync.stderr and 'description = "Review \\u007f controls."' in adapter.read_text() and '\x7f' not in adapter.read_text()
    checks.append(('native description escapes DEL for TOML', good))
    print('PASS' if good else 'FAIL', 'native description escapes DEL for TOML')
    run('valid escaped control description projection', 0)
    path.write_bytes(saved)
    adapter.write_bytes(saved_adapter)

    replacement = "description: 'Review 🚀 launch behavior when requested.'"
    path.write_text('\n'.join(replacement if line.startswith('description:') else line for line in saved.decode().split('\n')))
    sync = subprocess.run([sys.executable, '-I', str(fixture / '.claude/hooks/sync-native-adapters.py'), '--write'],
                          cwd=fixture, capture_output=True, text=True)
    good = sync.returncode == 0 and not sync.stderr and 'description = "Review 🚀 launch behavior when requested."' in adapter.read_text()
    checks.append(('native description preserves non-BMP Unicode as valid TOML text', good))
    print('PASS' if good else 'FAIL', 'native description preserves non-BMP Unicode as valid TOML text')
    run('valid non-BMP Unicode description projection', 0)
    path.write_bytes(saved)
    adapter.write_bytes(saved_adapter)

    adapter.unlink()
    os.mkfifo(adapter)
    try:
        sync = subprocess.run([sys.executable, '-I', str(fixture / '.claude/hooks/sync-native-adapters.py'), '--write'],
                              cwd=fixture, capture_output=True, text=True, timeout=3)
        good = sync.returncode != 0 and 'non-regular adapter' in sync.stderr and 'Traceback' not in sync.stderr
    except subprocess.TimeoutExpired:
        good = False
    adapter.unlink()
    adapter.write_bytes(saved_adapter)
    checks.append(('native sync refuses FIFO without opening it', good))
    print('PASS' if good else 'FAIL', 'native sync refuses FIFO without opening it')

    containers = fixture / '.codex/agents'
    external = fixture / 'external-native-sentinel'
    containers.rename(external)
    containers.symlink_to(external, target_is_directory=True)
    before = {p.name: (p.read_bytes(), p.stat().st_mtime_ns) for p in external.iterdir()}
    suite = subprocess.run([sys.executable, '-I', str(fixture / '.claude/hooks/check-rulebook-tests.py'), str(fixture)],
                           cwd=fixture, capture_output=True, text=True, timeout=10)
    after = {p.name: (p.read_bytes(), p.stat().st_mtime_ns) for p in external.iterdir()}
    good = suite.returncode != 0 and 'source native container' in suite.stdout and before == after and not suite.stderr
    containers.unlink()
    external.rename(containers)
    checks.append(('suite rejects source container links without touching their targets', good))
    print('PASS' if good else 'FAIL', 'suite rejects source container links without touching their targets')

    entry = fixture / 'CLAUDE.md'
    saved_entry = entry.read_bytes()
    entry.write_text(duplicate(saved_entry.decode(), '| `scout` |'))
    suite = subprocess.run([sys.executable, '-I', str(fixture / '.claude/hooks/check-rulebook-tests.py'), str(fixture)],
                           cwd=fixture, capture_output=True, text=True, timeout=10)
    good = suite.returncode != 0 and 'FAIL current valid rulebook' in suite.stdout and 'PASS missing AGENTS.md' not in suite.stdout and 'Traceback' not in suite.stderr
    entry.write_bytes(saved_entry)
    checks.append(('suite stops on an invalid source baseline before mutation scenarios', good))
    print('PASS' if good else 'FAIL', 'suite stops on an invalid source baseline before mutation scenarios')

    (fixture / '.claude/skills').rename(fixture / '.claude/skills-removed')
    run('missing skills root diagnosed without traceback', 1, 'missing .claude/skills directory')

evaluation = runpy.run_path(str(root / '.claude/skills/rulebook-maintenance/evals/evaluate.py'))
grade, select_cases = evaluation['grade'], evaluation['select_cases']
case = {'id': 'sample', 'prompt': 'Plan the sample task.', 'required_skills': ['owner'], 'required_gates': ['review'], 'forbidden_actions': ['device-use']}
valid = {'id': 'sample', 'skills': ['owner'], 'references': [], 'gates': ['review'], 'roles': [], 'actions': [], 'loaded_files': []}
grader_cases = [
    ('valid routing response', [valid], False),
    ('missing routing response', [], True),
    ('duplicate routing response', [valid, valid], True),
    ('unknown routing response', [valid, dict(valid, id='unknown')], True),
    ('invalid response container', {}, True),
    ('invalid response object', [None], True),
    ('invalid response id', [dict(valid, id=[])], True),
    ('empty response id', [dict(valid, id='')], True),
    ('surrogate response id', [dict(valid, id='\ud800')], True),
    ('invalid skill member', [dict(valid, skills=[{}])], True),
    ('invalid actions container', [dict(valid, actions='device-use')], True),
    ('missing required gate', [dict(valid, gates=[])], True),
    ('forbidden side effect', [dict(valid, actions=['device-use'])], True),
]
for label, responses, expected_failure in grader_cases:
    good = bool(grade([case], responses)) == expected_failure
    checks.append((label, good))
    print('PASS' if good else 'FAIL', label)

rubric_cases = [
    ('duplicate rubric id', [case, case]),
    ('empty rubric', []),
    ('invalid rubric container', None),
    ('invalid rubric object', [None]),
    ('invalid rubric id', [dict(case, id=[])]),
    ('empty rubric id', [dict(case, id='')]),
    ('surrogate rubric id', [dict(case, id='\ud800')]),
    ('surrogate rubric prompt', [dict(case, prompt='\ud800')]),
    ('missing rubric prompt', [{k: v for k, v in case.items() if k != 'prompt'}]),
    ('invalid rubric list', [dict(case, required_skills='owner')]),
    ('invalid rubric list member', [dict(case, forbidden_actions=[{}])]),
    ('missing rubric field', [{'id': 'sample', 'prompt': 'Plan.'}]),
]
for label, cases in rubric_cases:
    good = bool(grade(cases, [valid]))
    checks.append((label, good))
    print('PASS' if good else 'FAIL', label)

other = dict(case, id='other')
for label, catalog, selected_ids, expected_ids, failure in [
    ('selection defaults to the whole catalog', [case, other], None, ['sample', 'other'], False),
    ('selection preserves catalog order', [case, other], ['other', 'sample'], ['sample', 'other'], False),
    ('selection supports affected cases only', [case, other], ['other'], ['other'], False),
    ('selection rejects unknown ids', [case], ['unknown'], [], True),
    ('selection rejects duplicate ids', [case], ['sample', 'sample'], [], True),
    ('selection validates unselected rubric cases', [case, dict(other, prompt=None)], ['sample'], [], True),
]:
    selected, errors = select_cases(catalog, selected_ids)
    good = [entry['id'] for entry in selected] == expected_ids and bool(errors) == failure
    checks.append((label, good))
    print('PASS' if good else 'FAIL', label)

evaluator = root / '.claude/skills/rulebook-maintenance/evals/evaluate.py'
with tempfile.TemporaryDirectory(prefix='grippo-evaluator-input-') as tmp:
    invalid = pathlib.Path(tmp) / 'responses.json'
    invalid.write_text('{')
    for label, path in [('missing response file', invalid.with_name('absent.json')), ('invalid response JSON', invalid)]:
        result = subprocess.run([sys.executable, '-I', str(evaluator), str(path)], capture_output=True, text=True)
        good = result.returncode != 0 and 'Traceback' not in result.stderr and 'error:' in result.stderr
        checks.append((label, good))
        print('PASS' if good else 'FAIL', label)
    invalid.write_text('[{"id":"\\ud800"}]')
    result = subprocess.run([sys.executable, '-I', str(evaluator), str(invalid)], capture_output=True, text=True)
    good = result.returncode != 0 and 'Unicode string id' in result.stdout and 'Traceback' not in result.stderr
    checks.append(('surrogate response ID reports a clean CLI error', good))
    print('PASS' if good else 'FAIL', 'surrogate response ID reports a clean CLI error')
    catalog = json.loads(evaluator.with_name('cases.json').read_text())['cases']
    sample = catalog[0]
    response = dict(valid, id=sample['id'], skills=sample['required_skills'], gates=sample['required_gates'])
    invalid.write_text(json.dumps([response]))
    for label, selection, expected in [
        ('targeted CLI run reports selected scope', ['--case', sample['id']], 0),
        ('targeted CLI rejects unknown selection', ['--case', 'absent-case'], 1),
        ('targeted CLI rejects repeated selection', ['--case', sample['id'], '--case', sample['id']], 1),
        ('full CLI still requires every case', [], 1),
    ]:
        result = subprocess.run([sys.executable, '-I', str(evaluator), str(invalid), *selection], capture_output=True, text=True)
        good = result.returncode == expected and 'selected /' in result.stdout and not result.stderr
        checks.append((label, good))
        print('PASS' if good else 'FAIL', label)
    selection = ['--case', catalog[1]['id']]
    result = subprocess.run([sys.executable, '-I', str(evaluator), str(invalid), *selection], capture_output=True, text=True)
    good = result.returncode == 1 and 'unknown response:' in result.stdout and not result.stderr
    checks.append(('targeted CLI rejects responses outside selection', good))
    print('PASS' if good else 'FAIL', 'targeted CLI rejects responses outside selection')

    shadow = pathlib.Path(tmp) / 'json.py'
    marker = pathlib.Path(tmp) / 'shadow-imported'
    shadow.write_text('from pathlib import Path\nPath(' + repr(str(marker)) + ').touch()\n')
    environment = dict(os.environ, PYTHONPATH=tmp)
    result = subprocess.run([sys.executable, '-I', str(root / '.claude/hooks/bash-guard.py'), 'readonly'],
                            input='{}', env=environment, capture_output=True, text=True)
    good = result.returncode == 2 and not marker.exists() and 'Traceback' not in result.stderr
    checks.append(('isolated native guard ignores shadow imports', good))
    print('PASS' if good else 'FAIL', 'isolated native guard ignores shadow imports')

    crashing_guard = pathlib.Path(tmp) / 'crashing-guard.py'
    guard_source = (root / '.claude/hooks/bash-guard.py').read_text()
    crashing_guard.write_text(guard_source.replace(
        'def main() -> None:\n', 'def main() -> None:\n    raise RuntimeError("fixture validator failure")\n', 1))
    result = subprocess.run([sys.executable, '-I', str(crashing_guard), 'readonly'],
                            input='{}', capture_output=True, text=True)
    good = result.returncode == 2 and 'RuntimeError' in result.stderr and 'Traceback' not in result.stderr
    checks.append(('unexpected guard validation failures reject with exit 2', good))
    print('PASS' if good else 'FAIL', 'unexpected guard validation failures reject with exit 2')

print('Checker scenarios:', len(checks), 'passed:', sum(good for _, good in checks))
raise SystemExit(any(not good for _, good in checks))
