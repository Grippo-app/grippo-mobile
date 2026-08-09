import childProcess from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

function run(cwd, command, args) {
  return childProcess.spawnSync(command, args, {
    cwd,
    encoding: 'utf8',
    maxBuffer: 64 * 1024 * 1024
  });
}

function failMessage(command, relativePath, result) {
  const output = [result.stdout, result.stderr].filter(Boolean).join('\n').trim();
  return `${command} failed for ${relativePath}${output ? `\n${output}` : ''}`;
}

function trackedAndUntracked(root) {
  const result = childProcess.spawnSync(
    'git',
    ['ls-files', '-co', '--exclude-standard', '-z'],
    { cwd: root, encoding: null, maxBuffer: 64 * 1024 * 1024 }
  );
  if (result.status !== 0) {
    throw new Error(`git ls-files failed: ${result.stderr.toString('utf8').trim()}`);
  }
  return [...new Set(result.stdout.toString('utf8').split('\0').filter(Boolean))].sort();
}

function main() {
  const rootResult = run(process.cwd(), 'git', ['rev-parse', '--show-toplevel']);
  if (rootResult.status !== 0) throw new Error('not inside a Git repository');
  const root = rootResult.stdout.trim();
  const expectedNodeMajor = fs.readFileSync(path.join(root, '.nvmrc'), 'utf8').trim();
  const actualNodeMajor = process.versions.node.split('.')[0];
  if (actualNodeMajor !== expectedNodeMajor) {
    throw new Error(
      `Node ${expectedNodeMajor} is required by .nvmrc; current runtime is ${process.versions.node}`
    );
  }

  const files = trackedAndUntracked(root).filter((relativePath) => {
    const absolutePath = path.join(root, relativePath);
    return fs.existsSync(absolutePath) && fs.lstatSync(absolutePath).isFile();
  });
  const nodeFiles = files.filter((file) => /\.(?:cjs|js|mjs)$/.test(file));
  const pythonFiles = files.filter((file) => file.endsWith('.py'));
  const shellFiles = files.filter((file) => (
    file.endsWith('.sh') || file === 'orchestrator/skills/checks/hooks/pre-commit'
  ));
  const jsonFiles = files.filter((file) => file.endsWith('.json'));
  const failures = [];

  for (const relativePath of nodeFiles) {
    const result = run(root, process.execPath, ['--check', relativePath]);
    if (result.status !== 0) failures.push(failMessage('node --check', relativePath, result));
  }

  if (pythonFiles.length > 0) {
    const source = [
      'import pathlib, sys',
      'if sys.version_info < (3, 10):',
      '    print("Python 3.10 or newer is required", file=sys.stderr)',
      '    raise SystemExit(2)',
      'failed = False',
      'for item in sys.argv[1:]:',
      '    try:',
      '        compile(pathlib.Path(item).read_bytes(), item, "exec")',
      '    except Exception as error:',
      '        failed = True',
      '        print(f"{item}: {error}", file=sys.stderr)',
      'raise SystemExit(1 if failed else 0)'
    ].join('\n');
    const result = run(root, 'python3', ['-c', source, ...pythonFiles]);
    if (result.status !== 0) {
      const output = [result.stdout, result.stderr].filter(Boolean).join('\n').trim();
      failures.push(`Python compile failed${output ? `\n${output}` : ''}`);
    }
  }

  for (const relativePath of shellFiles) {
    const result = run(root, 'bash', ['-n', relativePath]);
    if (result.status !== 0) failures.push(failMessage('bash -n', relativePath, result));
  }

  for (const relativePath of jsonFiles) {
    try {
      JSON.parse(fs.readFileSync(path.join(root, relativePath), 'utf8'));
    } catch (error) {
      failures.push(`JSON.parse failed for ${relativePath}\n${error.message}`);
    }
  }

  if (failures.length > 0) {
    process.stderr.write(`${failures.join('\n\n')}\n`);
    process.exitCode = 1;
    return;
  }
  process.stdout.write(
    `syntax check passed: Node ${nodeFiles.length}, Python ${pythonFiles.length}, ` +
    `shell ${shellFiles.length}, JSON ${jsonFiles.length}\n`
  );
}

try {
  main();
} catch (error) {
  process.stderr.write(`check-syntax: ${error.message}\n`);
  process.exitCode = 1;
}
