// Step 12's foundation-integrity gate: ONE comment/string-aware scan shared by
// the agent-run CLI and the server ✓ validator (validators.js validateStep12).
// The load-bearing property: a comment or string literal that merely MENTIONS
// TODO(...)/NotImplementedError must NOT fail the gate — a raw regex scan
// false-positives on exactly the comments agents write about the trap, which
// permanently wedges the wizard ✓ (the defect this test pins against
// regressing). Only real code-position stubs fail.
import assert from 'node:assert/strict';
import test from 'node:test';
import { mkdtempSync, mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);
const siteDir = join(dirname(fileURLToPath(import.meta.url)), '..');
const scanModulePath = join(siteDir, 'server', 'foundation-stub-scan.js');
const { maskKotlinTrivia, scanFoundationStubs, FOUNDATION_ROOTS } = require(scanModulePath);

function writeKt(root, relPath, content) {
  const abs = join(root, relPath);
  mkdirSync(dirname(abs), { recursive: true });
  writeFileSync(abs, content);
}

function makeFixture() {
  const root = mkdtempSync(join(tmpdir(), 'stub-scan-'));
  test.after(() => rmSync(root, { recursive: true, force: true }));
  return root;
}

test('comment and string mentions never fail the gate; real stubs do', () => {
  const root = makeFixture();
  // Trivia-only mentions — the exact shape that wedged the ✓: comments warning
  // against stubbing, plus every literal form the masker must blank.
  writeKt(root, 'ui-dialog-features/error-display/src/commonMain/kotlin/DialogContentComponent.kt', [
    'package app',
    '// Do NOT stub this branch with TODO("later") — it compiles, then crashes',
    '// with NotImplementedError the first time any error fires.',
    '/* block form: TODO(...) and NotImplementedError are also just prose,',
    '   /* even nested */ still prose */',
    'val message = "user-facing text may say NotImplementedError too"',
    'val raw = """raw strings mentioning TODO(x) stay prose"""',
    'fun createChild(): Child = RealChild()',
  ].join('\n'));
  assert.deepEqual(scanFoundationStubs(root), [],
    'comment/string mentions must scan clean');

  // A real code-position stub in another foundation root must be reported
  // with its file and 1-based line.
  writeKt(root, 'shared/src/commonMain/kotlin/Wiring.kt', [
    'package app',
    'fun wire(): Nothing = TODO("wire me")',
  ].join('\n'));
  const hits = scanFoundationStubs(root);
  assert.equal(hits.length, 1, 'exactly the real stub is reported');
  assert.equal(hits[0].file, join('shared', 'src', 'commonMain', 'kotlin', 'Wiring.kt'));
  assert.equal(hits[0].line, 2);
  assert.match(hits[0].text, /TODO\("wire me"\)/);
});

test('scan scope: commonMain .kt in the four foundation roots, excluded dirs skipped', () => {
  const root = makeFixture();
  // Outside commonMain → out of scope (matches launch.md's globs).
  writeKt(root, 'shared/src/androidMain/kotlin/Android.kt', 'fun x(): Nothing = TODO("android-only")');
  // Non-foundation root → out of scope.
  writeKt(root, 'data-features/notes/src/commonMain/kotlin/Notes.kt', 'fun x(): Nothing = TODO("data layer")');
  // Excluded dir (build output) inside a foundation root → skipped.
  writeKt(root, 'ui-core/build/generated/commonMain/kotlin/Gen.kt', 'fun x(): Nothing = TODO("generated")');
  // Non-Kotlin file → skipped even in scope.
  writeKt(root, 'ui-core/state/src/commonMain/kotlin/notes.md', 'TODO(prose in docs)');
  assert.deepEqual(scanFoundationStubs(root), []);
  assert.deepEqual(FOUNDATION_ROOTS, ['shared', 'ui-core', 'ui-dialog-features', 'ui-screen-features']);
});

test('masker preserves newlines so reported line numbers match the source', () => {
  const source = '/* multi\nline\ncomment */\nval ok = 1\nfun broken(): Nothing = TODO("real")\n';
  const masked = maskKotlinTrivia(source);
  assert.equal(masked.split('\n').length, source.split('\n').length);
  const stubLine = masked.split('\n').findIndex((line) => /TODO\(/.test(line));
  assert.equal(stubLine + 1, 5, 'the real stub stays on its source line after masking');
});

test('CLI and validateStep12 agree: the wizard ✓ flips the moment the gate passes', () => {
  const root = makeFixture();
  writeKt(root, 'ui-screen-features/home/src/commonMain/kotlin/Home.kt', [
    '// TODO(...) in a comment must not block the ✓',
    'fun render(): Nothing = TODO("still stubbed")',
  ].join('\n'));
  const env = { ...process.env, ORCHESTRATOR_PROJECT_ROOT: root };

  const dirty = spawnSync(process.execPath, [scanModulePath], { env, encoding: 'utf8' });
  assert.equal(dirty.status, 1, 'CLI exits 1 while a real stub survives');
  assert.match(dirty.stderr, /Home\.kt:2/, 'CLI reports file:line for the agent');

  // validateStep12 (the ✓ validator) sees the same verdict through the same
  // implementation — spawned so paths.js re-derives PROJECT_ROOT from the env.
  const validatorEval = 'console.log(require(process.argv[1]).STEP_VALIDATORS["12"]())';
  const validatorsPath = join(siteDir, 'server', 'validators.js');
  const dirtyValidator = spawnSync(process.execPath, ['-e', validatorEval, validatorsPath], { env, encoding: 'utf8' });
  assert.equal(dirtyValidator.stdout.trim(), 'false');

  writeKt(root, 'ui-screen-features/home/src/commonMain/kotlin/Home.kt', [
    '// TODO(...) in a comment must not block the ✓',
    'fun render(): Screen = RealScreen()',
  ].join('\n'));
  const clean = spawnSync(process.execPath, [scanModulePath], { env, encoding: 'utf8' });
  assert.equal(clean.status, 0, 'CLI exits 0 once the stub is implemented');
  assert.match(clean.stdout, /OK: no foundation stubs/);
  const cleanValidator = spawnSync(process.execPath, ['-e', validatorEval, validatorsPath], { env, encoding: 'utf8' });
  assert.equal(cleanValidator.stdout.trim(), 'true', 'the ✓ flips with no manual marking');
});
