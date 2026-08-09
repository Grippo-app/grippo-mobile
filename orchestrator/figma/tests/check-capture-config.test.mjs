// check-capture-config.test.mjs — pins for the capture-config drift gate + self-heal:
// a class-level @Config with no qualifiers is MISSING for each test; a wrong w/h is DRIFT;
// --gate exits 2 on a BLOCKER; --fix injects a method-level @Config (never touches class-level),
// preserves locale/density segments while replacing only the size, maps dark captures to
// .dark.spec.json, adds the Config import when absent, and re-verifies clean; a spec with no
// capturing @Test is an advisory CAPTURE_TEST_ABSENT (not a hard block — the comparator owns that);
// comments containing captureRoboImage are ignored. R5 pins: the expected qualifier carries the
// DERIVED design locale (spec texts × string resources; `designLocale` declaration wins) — a
// uk design against a locale-less @Config is DRIFT and --fix writes the locale segment; an en
// design is byte-identical under --fix; votable-but-undecided text fails closed as
// CAPTURE_LOCALE_UNDERIVABLE; an RTL locale requires ldrtl; a textless spec enforces geometry
// only (the pre-R5 behavior — the don't-touch-stub-data pins above stay green by construction).
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync, symlinkSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import assert from 'node:assert/strict'
import { CAPTURE_CONFIG_DISCOVERY_KEY, captureConfigDiscovery, captureConfigScopeOmissions } from '../scripts/lib/capture-config-discovery.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const SCRIPT = join(HERE, '..', 'scripts', 'check-capture-config.mjs')
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

const spec = (screen, w, h, theme = 'light') => ({ screen, frameSizeDp: { w, h }, theme, elements: [] })

function ws(setup) {
  const dir = mkdtempSync(join(tmpdir(), 'capcfg-'))
  const sdir = join(dir, 'screens', 'TASK_1_fixture'), code = join(dir, 'code', 'androidHostTest'), reports = join(dir, 'reports')
  mkdirSync(sdir, { recursive: true }); mkdirSync(code, { recursive: true }); mkdirSync(reports, { recursive: true })
  setup({ sdir, code, dir })
  return { dir, sdir, code, reports }
}
function run(w, args, envExtra = {}) {
  const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--code-root', join(w.dir, 'code'), ...args], {
    env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(w.dir, 'screens'), FIGMA_REPORTS_DIR: w.reports, ...envExtra }, encoding: 'utf8',
  })
  const report = JSON.parse(readFileSync(join(w.reports, 'capture-config-TASK_1_fixture.json'), 'utf8'))
  return { status: r.status, out: r.stdout + r.stderr, report }
}

try {
  // 1. Class-level @Config with no qualifiers → MISSING per test; --gate exits 2.
  const w1 = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(spec('Home', 390, 844)))
    writeFileSync(join(code, 'ScreenshotTest.kt'), [
      'package t', 'import com.github.takahirom.roborazzi.captureRoboImage', 'import org.junit.Test', 'import org.robolectric.annotation.Config', '',
      '@Config(sdk = [34])', 'class ScreenshotTest {', '    @Test', '    fun home() {',
      '        // captureRoboImage("build/outputs/roborazzi/DecoyScreenshot.png") in a comment',
      '        captureRoboImage("build/outputs/roborazzi/HomeScreenshot.png") { Text("hi") }', '    }', '}', '',
    ].join('\n'))
  })
  const r1 = run(w1, ['--gate'])
  check('class-level @Config without qualifiers → CAPTURE_CONFIG_MISSING, --gate exits 2', () => {
    assert.equal(r1.status, 2)
    const hit = r1.report.issues.find((i) => i.issueKind === 'CAPTURE_CONFIG_MISSING' && i.screen === 'Home')
    assert.ok(hit && hit.severity === 'BLOCKER')
    assert.equal(hit.expected, 'w390dp-h844dp')
  })
  check('a captureRoboImage inside a comment is ignored (no phantom Decoy issue)', () => {
    assert.ok(!r1.report.issues.some((i) => i.screen === 'Decoy'))
  })
  check('capture-config report hash-binds the Kotlin source and project locale config', () => {
    assert.ok(r1.report.inputHashes[join(w1.code, 'ScreenshotTest.kt')])
    assert.ok(Object.keys(r1.report.inputHashes).some((p) => p.endsWith('/orchestrator/project-config.md')))
    assert.deepEqual(r1.report.inputs.codeRoots, ['code'])
    assert.equal(r1.report.inputs.testFilesScanned, 1)
    assert.equal(r1.report.inputs.captureDiscovery.digest, r1.report.inputHashes[CAPTURE_CONFIG_DISCOVERY_KEY])
    assert.deepEqual(r1.report.designLocaleEnvOverrides, [])
  })
  check('capture-config discovery digest changes when a new capture-bearing test appears', () => {
    const added = join(w1.code, 'AddedScreenshotTest.kt')
    writeFileSync(added, 'fun added() { captureRoboImage("AddedScreenshot.png") }\n')
    const changed = run(w1, ['--gate'])
    rmSync(added, { force: true })
    assert.notEqual(changed.report.inputHashes[CAPTURE_CONFIG_DISCOVERY_KEY], r1.report.inputHashes[CAPTURE_CONFIG_DISCOVERY_KEY])
  })
  check('capture-config discovery digest includes not-yet-present locale resources and bindings', () => {
    const root = mkdtempSync(join(tmpdir(), 'capcfg-discovery-'))
    try {
      const values = join(root, 'src', 'main', 'res', 'values')
      const screens = join(root, 'screens')
      mkdirSync(values, { recursive: true })
      mkdirSync(screens, { recursive: true })
      writeFileSync(join(values, 'strings.xml'), '<resources/>\n')
      const before = captureConfigDiscovery({ codeRoots: [root], screensDir: screens, supportedLocales: ['en', 'uk'] }).digest
      mkdirSync(join(root, 'src', 'main', 'res', 'values-uk'), { recursive: true })
      writeFileSync(join(root, 'src', 'main', 'res', 'values-uk', 'strings.xml'), '<resources><string name="x">Текст</string></resources>\n')
      writeFileSync(join(screens, 'bindings.json'), '{"schemaVersion":1}\n')
      const after = captureConfigDiscovery({ codeRoots: [root], screensDir: screens, supportedLocales: ['en', 'uk'] }).digest
      assert.notEqual(after, before)
    } finally {
      rmSync(root, { recursive: true, force: true })
    }
  })
  check('narrowed discovery reports canonical capture/resource omissions', () => {
    const root = mkdtempSync(join(tmpdir(), 'capcfg-scope-'))
    try {
      const screens = join(root, 'screens')
      const moduleA = join(root, 'module-a')
      const moduleB = join(root, 'module-b')
      for (const module of [moduleA, moduleB]) {
        const tests = join(module, 'src', 'androidHostTest')
        mkdirSync(tests, { recursive: true })
        mkdirSync(join(module, 'src', 'main', 'res'), { recursive: true })
        writeFileSync(join(tests, 'ScreenshotTest.kt'), 'fun capture() { captureRoboImage("Screen.png") }\n')
      }
      mkdirSync(screens, { recursive: true })
      const effective = captureConfigDiscovery({ codeRoots: [moduleA], screensDir: screens, supportedLocales: ['en'] })
      const canonical = captureConfigDiscovery({ codeRoots: [root], screensDir: screens, supportedLocales: ['en'] })
      const omissions = captureConfigScopeOmissions(effective, canonical)
      assert.deepEqual(omissions.captureFiles, [join(moduleB, 'src', 'androidHostTest', 'ScreenshotTest.kt')])
      assert.deepEqual(omissions.resourceRoots, [join(moduleB, 'src', 'main', 'res')])
    } finally {
      rmSync(root, { recursive: true, force: true })
    }
  })
  check('canonical discovery follows and deduplicates a symlinked product module', () => {
    const root = mkdtempSync(join(tmpdir(), 'capcfg-symlink-scope-'))
    try {
      const canonicalRoot = join(root, 'product')
      const externalModule = join(root, 'external-module')
      const tests = join(externalModule, 'src', 'androidHostTest')
      const resources = join(externalModule, 'src', 'main', 'res')
      const screens = join(root, 'screens')
      mkdirSync(canonicalRoot, { recursive: true })
      mkdirSync(tests, { recursive: true })
      mkdirSync(resources, { recursive: true })
      mkdirSync(screens, { recursive: true })
      writeFileSync(join(tests, 'ScreenshotTest.kt'), 'fun capture() { captureRoboImage("Screen.png") }\n')
      symlinkSync(externalModule, join(canonicalRoot, 'linked-module'), 'dir')
      const canonical = captureConfigDiscovery({ codeRoots: [canonicalRoot], screensDir: screens, supportedLocales: ['en'] })
      const effective = captureConfigDiscovery({ codeRoots: [externalModule, join(canonicalRoot, 'linked-module')], screensDir: screens, supportedLocales: ['en'] })
      assert.deepEqual(canonical.captureFiles, [join(canonicalRoot, 'linked-module', 'src', 'androidHostTest', 'ScreenshotTest.kt')])
      assert.deepEqual(canonical.resourceRoots, [join(canonicalRoot, 'linked-module', 'src', 'main', 'res')])
      assert.deepEqual(captureConfigScopeOmissions(effective, canonical), { captureFiles: [], resourceRoots: [] })
    } finally {
      rmSync(root, { recursive: true, force: true })
    }
  })
  check('missing effective code root is a gate blocker, not an empty scan', () => {
    const missing = join(w1.dir, 'missing-code-root')
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--code-root', missing, '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(w1.dir, 'screens'), FIGMA_REPORTS_DIR: w1.reports }, encoding: 'utf8',
    })
    const report = JSON.parse(readFileSync(join(w1.reports, 'capture-config-TASK_1_fixture.json'), 'utf8'))
    assert.equal(r.status, 2)
    assert.ok(report.issues.some((issue) => issue.issueKind === 'CAPTURE_DISCOVERY_UNREADABLE' && issue.severity === 'BLOCKER'))
  })

  // 2. --fix injects a method-level @Config (class-level untouched), re-verify PASS.
  run(w1, ['--fix'])
  const r1b = run(w1, ['--gate'])
  check('--fix injects method-level @Config; class-level preserved; re-gate PASS', () => {
    const src = readFileSync(join(w1.code, 'ScreenshotTest.kt'), 'utf8')
    assert.match(src, /@Config\(sdk = \[34\]\)\s*\nclass ScreenshotTest/)          // class-level intact
    assert.match(src, /@Test\s*\n\s*@Config\(qualifiers = "w390dp-h844dp"\)\s*\n\s*fun home/)
    assert.equal(r1b.status, 0)
    assert.equal(r1b.report.overall, 'PASS')
  })

  // 3. Drift with locale + density: replace ONLY the size, keep uk-rUA + xxhdpi; map dark → .dark.spec.
  const w2 = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(spec('Home', 390, 844)))
    writeFileSync(join(sdir, 'Home.dark.spec.json'), JSON.stringify(spec('Home', 390, 844, 'dark')))
    writeFileSync(join(code, 'ScreenshotTest.kt'), [
      'package t', 'import com.github.takahirom.roborazzi.captureRoboImage', 'import org.junit.Test', '',   // NO Config import
      'class ScreenshotTest {',
      '    @Test', '    @Config(qualifiers = "uk-rUA-w320dp-h470dp-xxhdpi")', '    fun home() {',
      '        captureRoboImage("build/outputs/roborazzi/HomeScreenshot.png") { Text("hi") }', '    }',
      '    @Test', '    fun homeDark() {',
      '        captureRoboImage("build/outputs/roborazzi/HomeScreenshot.dark.png") { Text("hi") }', '    }', '}', '',
    ].join('\n'))
  })
  check('drift with locale+density → CAPTURE_CONFIG_DRIFT (before fix)', () => {
    const r = run(w2, ['--gate'])
    assert.equal(r.status, 2)
    assert.ok(r.report.issues.some((i) => i.issueKind === 'CAPTURE_CONFIG_DRIFT' && i.theme === 'primary'))
  })
  run(w2, ['--fix'])
  check('--fix replaces only the size (keeps uk-rUA + xxhdpi), fixes dark, adds Config import', () => {
    const src = readFileSync(join(w2.code, 'ScreenshotTest.kt'), 'utf8')
    assert.match(src, /qualifiers = "uk-rUA-w390dp-h844dp-xxhdpi"/)                 // size replaced, locale+density kept
    assert.match(src, /import org\.robolectric\.annotation\.Config/)               // import injected
    assert.match(src, /fun homeDark/)
    const dark = src.slice(src.indexOf('fun homeDark') - 120, src.indexOf('fun homeDark'))
    assert.match(dark, /@Config\(qualifiers = "w390dp-h844dp"\)/)                  // dark got its own method-level config
    assert.equal(run(w2, ['--gate']).status, 0)
  })

  // 3b. ReDoS guard: a very long stub line before the class declaration must not
  // trigger catastrophic backtracking in the class-config scan.
  const w2b = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(spec('Home', 390, 844)))
    const longLine = '// ' + 'x = SomeState(a = 1, b = 2, c = "value", '.repeat(400)
    writeFileSync(join(code, 'ScreenshotTest.kt'), [
      'package t', 'import com.github.takahirom.roborazzi.captureRoboImage', 'import org.junit.Test', 'import org.robolectric.annotation.Config',
      longLine,
      '@Config(sdk = [34])', 'class ScreenshotTest {', '    @Test', '    @Config(qualifiers = "w390dp-h844dp")', '    fun home() {',
      '        captureRoboImage("build/outputs/roborazzi/HomeScreenshot.png") { Text("hi") }', '    }', '}', '',
    ].join('\n'))
  })
  check('long line before the class decl completes fast (ReDoS guard) and verifies clean', () => {
    const r = run(w2b, ['--gate'])
    assert.equal(r.status, 0)
    assert.equal(r.report.overall, 'PASS')
  })

  // 3c. Multi-line @Config must be detected even when the annotation closes on a
  // separate line; otherwise the test class would be skipped fail-open.
  const w3c = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(spec('Home', 390, 844)))
    writeFileSync(join(code, 'ScreenshotTest.kt'), [
      'package t', 'import com.github.takahirom.roborazzi.captureRoboImage', 'import org.junit.Test', 'import org.robolectric.annotation.Config',
      'class ScreenshotTest {', '    @Test', '    @Config(', '        sdk = [34],', '        qualifiers = "w320dp-h470dp"', '    )', '    fun home() {',
      '        captureRoboImage("build/outputs/roborazzi/HomeScreenshot.png") { Text("hi") }', '    }', '}', '',
    ].join('\n'))
  })
  check('multi-line @Config drift is DETECTED (not a fail-open CAPTURE_TEST_ABSENT)', () => {
    const r = run(w3c, ['--gate'])
    assert.equal(r.status, 2)
    assert.ok(r.report.issues.some((i) => i.issueKind === 'CAPTURE_CONFIG_DRIFT' && i.screen === 'Home'))
    assert.ok(!r.report.issues.some((i) => i.issueKind === 'CAPTURE_TEST_ABSENT'))
  })
  check('--fix patches ONLY the qualifiers line of a multi-line @Config; re-gate PASS', () => {
    run(w3c, ['--fix'])
    const src = readFileSync(join(w3c.code, 'ScreenshotTest.kt'), 'utf8')
    assert.match(src, /qualifiers = "w390dp-h844dp"/)
    assert.match(src, /@Config\(\s*\n\s*sdk = \[34\],/)   // multi-line structure preserved
    assert.equal(run(w3c, ['--gate']).status, 0)
  })

  // 3d. A char literal containing a double-quote must not derail the comment/string mask (an
  // unhandled `'"'` used to flip the string state and blank the real code after it).
  const w3d = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(spec('Home', 390, 844)))
    writeFileSync(join(code, 'ScreenshotTest.kt'), [
      'package t', 'import com.github.takahirom.roborazzi.captureRoboImage', 'import org.junit.Test', 'import org.robolectric.annotation.Config',
      'class ScreenshotTest {', "    val q = '\"'  // a quote char literal", '    @Test', '    @Config(sdk = [34])', '    fun home() {',
      '        captureRoboImage("build/outputs/roborazzi/HomeScreenshot.png") { Text("hi") }', '    }', '}', '',
    ].join('\n'))
  })
  check('char literal with an embedded double-quote does not hide the following test', () => {
    const r = run(w3d, ['--gate'])
    assert.equal(r.status, 2)
    assert.ok(r.report.issues.some((i) => i.issueKind === 'CAPTURE_CONFIG_MISSING' && i.screen === 'Home'))
    run(w3d, ['--fix'])
    assert.match(readFileSync(join(w3d.code, 'ScreenshotTest.kt'), 'utf8'), /qualifiers = "w390dp-h844dp"/)
  })

  // 3e. Dual capture (light+dark) in ONE @Test fun → exactly ONE injected method-@Config
  // (two would be a non-repeatable-annotation compile error), re-gate PASS.
  const w3e = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(spec('Home', 390, 844)))
    writeFileSync(join(sdir, 'Home.dark.spec.json'), JSON.stringify(spec('Home', 390, 844, 'dark')))
    writeFileSync(join(code, 'ScreenshotTest.kt'), [
      'package t', 'import com.github.takahirom.roborazzi.captureRoboImage', 'import org.junit.Test', 'import org.robolectric.annotation.Config',
      '@Config(qualifiers = "w320dp-h470dp")', 'class ScreenshotTest {', '    @Test', '    fun both() {',
      '        captureRoboImage("build/outputs/roborazzi/HomeScreenshot.png") { }',
      '        captureRoboImage("build/outputs/roborazzi/HomeScreenshot.dark.png") { }', '    }', '}', '',
    ].join('\n'))
  })
  check('two same-size captures in one fun → ONE injected @Config (no duplicate), re-gate PASS', () => {
    run(w3e, ['--fix'])
    const src = readFileSync(join(w3e.code, 'ScreenshotTest.kt'), 'utf8')
    const methodConfigs = (src.match(/^\s+@Config\(qualifiers = "w390dp-h844dp"\)/gm) || []).length
    assert.equal(methodConfigs, 1, 'exactly one method-level @Config injected')
    assert.equal(run(w3e, ['--gate']).status, 0)
  })

  // 3f. Two captures of DIFFERENT-size screens in one fun → CAPTURE_CONFIG_CONFLICT (report,
  // never silently duplicate-annotate).
  const w3f = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(spec('Home', 390, 844)))
    writeFileSync(join(sdir, 'Tall.spec.json'), JSON.stringify(spec('Tall', 390, 2135)))
    writeFileSync(join(code, 'ScreenshotTest.kt'), [
      'package t', 'import com.github.takahirom.roborazzi.captureRoboImage', 'import org.junit.Test', 'import org.robolectric.annotation.Config',
      'class ScreenshotTest {', '    @Test', '    fun mixed() {',
      '        captureRoboImage("build/outputs/roborazzi/HomeScreenshot.png") { }',
      '        captureRoboImage("build/outputs/roborazzi/TallScreenshot.png") { }', '    }', '}', '',
    ].join('\n'))
  })
  check('different-size captures in one fun → CAPTURE_CONFIG_CONFLICT (not a duplicate @Config)', () => {
    const r = run(w3f, ['--gate'])
    assert.equal(r.status, 2)
    assert.ok(r.report.issues.some((i) => i.issueKind === 'CAPTURE_CONFIG_CONFLICT' && /mixed/.test(i.test)))
  })

  // 3g. A NAMED local fun inside the test body must not be mistaken for the enclosing test.
  const w3g = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(spec('Home', 390, 844)))
    writeFileSync(join(code, 'ScreenshotTest.kt'), [
      'package t', 'import com.github.takahirom.roborazzi.captureRoboImage', 'import org.junit.Test', 'import org.robolectric.annotation.Config',
      'class ScreenshotTest {', '    @Test', '    @Config(qualifiers = "w320dp-h470dp")', '    fun home() {',
      '        fun helper() { println("x") }',
      '        captureRoboImage("build/outputs/roborazzi/HomeScreenshot.png") { }', '    }', '}', '',
    ].join('\n'))
  })
  check('local fun in the body is not the test; drift is on home() and only home() is fixed', () => {
    const r = run(w3g, ['--gate'])
    assert.equal(r.status, 2)
    assert.ok(r.report.issues.some((i) => i.issueKind === 'CAPTURE_CONFIG_DRIFT' && i.test === 'home'))
    run(w3g, ['--fix'])
    const src = readFileSync(join(w3g.code, 'ScreenshotTest.kt'), 'utf8')
    assert.match(src, /@Config\(qualifiers = "w390dp-h844dp"\)\s*\n\s*fun home/)
    assert.match(src, /fun helper\(\) \{ println\("x"\) \}/)   // local fun untouched
    assert.ok(!/@Config[^\n]*\n\s*fun helper/.test(src), 'no @Config injected above the local fun')
  })

  const w3h = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(spec('Home', 390, 844)))
    writeFileSync(join(code, 'ScreenshotTest.kt'), [
      'package t', 'import com.github.takahirom.roborazzi.captureRoboImage',
      'class ScreenshotTest {', '    fun helper() {',
      '        captureRoboImage("build/outputs/roborazzi/HomeScreenshot.png") { }', '    }', '}', '',
    ].join('\n'))
  })
  check('capture outside an @Test is a blocker instead of inheriting the nearest helper', () => {
    const r = run(w3h, ['--gate'])
    assert.equal(r.status, 2)
    assert.ok(r.report.issues.some((i) => i.issueKind === 'CAPTURE_NOT_IN_TEST'))
  })

  // 4. A spec with no capturing @Test → advisory CAPTURE_TEST_ABSENT (not a hard block).
  const w3 = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Lonely.spec.json'), JSON.stringify(spec('Lonely', 390, 844)))
    writeFileSync(join(code, 'ScreenshotTest.kt'), 'package t\nclass ScreenshotTest {}\n')
  })
  check('spec with no capturing @Test → WARN CAPTURE_TEST_ABSENT, --gate does NOT fail', () => {
    const r = run(w3, ['--gate'])
    assert.equal(r.status, 0)   // WARN-only, the comparator owns MISSING_CAPTURE
    assert.ok(r.report.issues.some((i) => i.issueKind === 'CAPTURE_TEST_ABSENT' && i.screen === 'Lonely'))
  })

  // 5. Import-less source (a `package` line, ZERO imports) — --fix must STILL add the Config import
  //    when it injects @Config, else it emits `@Config(...)` with an unresolved `Config` reference
  //    (non-compilable Kotlin) that the re-gate would falsely PASS.
  const w4 = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(spec('Home', 390, 844)))
    writeFileSync(join(code, 'ScreenshotTest.kt'), [
      'package t', '',
      'class ScreenshotTest {', '    @Test', '    fun home() {',
      '        captureRoboImage("build/outputs/roborazzi/HomeScreenshot.png") { }', '    }', '}', '',
    ].join('\n'))
  })
  check('import-less source: --fix adds the Config import (anchored after `package`) + @Config; re-gate PASS', () => {
    assert.equal(run(w4, ['--gate']).status, 2)                                       // CAPTURE_CONFIG_MISSING (no @Config anywhere)
    run(w4, ['--fix'])
    const src = readFileSync(join(w4.code, 'ScreenshotTest.kt'), 'utf8')
    assert.match(src, /import org\.robolectric\.annotation\.Config/)                  // import injected despite zero prior imports
    assert.match(src, /^package t\nimport org\.robolectric\.annotation\.Config/)      // anchored right after the package line
    assert.match(src, /@Config\(qualifiers = "w390dp-h844dp"\)\s*\n\s*fun home/)
    assert.equal(run(w4, ['--gate']).status, 0)                                       // compilable + re-gate PASS
  })

  // 5. W2-2 identity join: a capture named OFF-convention (`FooCardDefaultScreenshot.png` for
  //    spec screen FooCard) is outside deterministic convention-based discovery — but a manifest entry binding
  //    the basename to the screen's nodeId keeps it fully verified, --fix included.
  const w5 = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'FooCard.spec.json'), JSON.stringify(spec('FooCard', 390, 844)))
    const fetchedAt = '2026-01-01T00:00:00.000Z'
    const url = 'https://www.figma.com/design/FileKey?node-id=9-9'
    writeFileSync(join(sdir, 'index.json'), JSON.stringify({ schemaVersion: 3, taskStem: 'TASK_1_fixture', nodes: { FooCard: {
      kind: 'component', url, nodeId: '9:9', fetchedAt,
      variants: [{ id: 'primary', theme: 'light', locale: 'default', platform: 'shared', url, nodeId: '9:9', fetchedAt, imageFile: 'FooCard.png', specFile: 'FooCard.spec.json' }],
    } } }))
    writeFileSync(join(code, 'ScreenshotTest.kt'), [
      'package t', 'import com.github.takahirom.roborazzi.captureRoboImage', 'import org.junit.Test', 'import org.robolectric.annotation.Config', '',
      '@Config(sdk = [34])', 'class ScreenshotTest {', '    @Test', '    fun fooCardDefault() {',
      '        captureRoboImage("build/outputs/roborazzi/FooCardDefaultScreenshot.png") { }', '    }', '}', '',
    ].join('\n'))
  })
  const w5manifest = join(w5.dir, 'manifest.json')
  writeFileSync(w5manifest, JSON.stringify({ captures: [{ captureName: 'FooCardDefaultScreenshot.png', path: join(w5.dir, 'FooCardDefaultScreenshot.png'), nodeId: '9:9', primaryState: true }] }))
  const r5noManifest = run(w5, ['--gate'])
  check('W2-2: off-convention capture WITHOUT an explicit identity binding → only CAPTURE_TEST_ABSENT', () => {
    assert.equal(r5noManifest.status, 0)
    assert.ok(r5noManifest.report.issues.some((i) => i.issueKind === 'CAPTURE_TEST_ABSENT' && i.screen === 'FooCard'))
    assert.ok(!r5noManifest.report.issues.some((i) => i.issueKind === 'CAPTURE_CONFIG_MISSING'))
  })
  const w5bindings = join(w5.sdir, 'bindings.json')
  writeFileSync(w5bindings, JSON.stringify({ schemaVersion: 2, stem: 'TASK_1_fixture', screens: [{ screenName: 'FooCard', nodeId: '9:9', captureBasename: 'FooCardDefaultScreenshot.png' }], components: [] }))
  const r5bindings = run(w5, ['--gate'])
  check('W2-6 identity join: bindings.json verifies an off-convention capture before the driver manifest exists', () => {
    assert.equal(r5bindings.status, 2)
    assert.ok(r5bindings.report.issues.some((i) => i.issueKind === 'CAPTURE_CONFIG_MISSING' && i.screen === 'FooCard'))
    assert.ok(r5bindings.report.inputHashes[w5bindings], 'bindings identity input is hash-bound')
    assert.ok(r5bindings.report.inputHashes[join(w5.sdir, 'index.json')], 'node identity index is hash-bound')
  })
  const r5 = run(w5, ['--gate'], { SCREENSHOT_CAPTURE_MANIFEST: w5manifest })
  check('W2-2 identity join: manifest nodeId binds the off-convention capture → @Config verified (BLOCKER, exit 2)', () => {
    assert.equal(r5.status, 2)
    const hit = r5.report.issues.find((i) => i.issueKind === 'CAPTURE_CONFIG_MISSING' && i.screen === 'FooCard')
    assert.ok(hit, `expected CAPTURE_CONFIG_MISSING for FooCard, got ${JSON.stringify(r5.report.issues)}`)
    assert.ok(!r5.report.issues.some((i) => i.issueKind === 'CAPTURE_TEST_ABSENT'), 'no absent-test WARN once the manifest binds it')
  })
  check('W2-2 identity join: --fix heals the nodeId-bound test; re-gate PASS', () => {
    run(w5, ['--fix'], { SCREENSHOT_CAPTURE_MANIFEST: w5manifest })
    const src = readFileSync(join(w5.code, 'ScreenshotTest.kt'), 'utf8')
    assert.match(src, /@Config\(qualifiers = "w390dp-h844dp"\)\s*\n\s*fun fooCardDefault/)
    assert.equal(run(w5, ['--gate'], { SCREENSHOT_CAPTURE_MANIFEST: w5manifest }).status, 0)
  })

  // 6. W2-2 TEST_UNMATCHED note: a no-spec capture in a file that DOES capture this task's
  //    specs is surfaced on stdout (console note, not a report issue).
  const w6 = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(spec('Home', 390, 844)))
    writeFileSync(join(code, 'ScreenshotTest.kt'), [
      'package t', 'import com.github.takahirom.roborazzi.captureRoboImage', 'import org.junit.Test', 'import org.robolectric.annotation.Config', '',
      'class ScreenshotTest {', '    @Test', '    @Config(qualifiers = "w390dp-h844dp")', '    fun home() {',
      '        captureRoboImage("build/outputs/roborazzi/HomeScreenshot.png") { }', '    }',
      '    @Test', '    fun typo() {',
      '        captureRoboImage("build/outputs/roborazzi/TypoScreenshot.png") { }', '    }', '}', '',
    ].join('\n'))
  })
  const r6 = run(w6, ['--gate'])
  check('W2-2 TEST_UNMATCHED: no-spec capture in a matched file surfaces on stdout, not in the report', () => {
    assert.equal(r6.status, 0)
    assert.match(r6.out, /TEST_UNMATCHED\s+'typo' captures TypoScreenshot\.png/)
    assert.ok(!r6.report.issues.some((i) => i.screen === 'Typo'))
  })

  // --- R5: design-locale derivation ------------------------------------------------------
  // Shared uk+en string-resource fixture; wired via the fixture-only env overrides.
  const localeRes = mkdtempSync(join(tmpdir(), 'capcfg-locale-'))
  mkdirSync(join(localeRes, 'values'), { recursive: true })
  mkdirSync(join(localeRes, 'values-uk'), { recursive: true })
  writeFileSync(join(localeRes, 'values', 'strings.xml'), '<resources><string name="diag">Diagnostics</string><string name="term">Terminals</string></resources>')
  writeFileSync(join(localeRes, 'values-uk', 'strings.xml'), '<resources><string name="diag">Діагностика</string><string name="term">Термінали</string></resources>')
  const LOCALE_ENV = { FIGMA_STRING_RESOURCE_ROOTS: localeRes, FIGMA_SUPPORTED_LOCALES: 'en,uk' }
  const specT = (screen, w, h, texts, theme = 'light') => ({ screen, frameSizeDp: { w, h }, theme, elements: texts.map((t, i) => ({ stableId: `e${i}`, name: `t${i}`, bboxDp: { x: 0, y: 0, w: 10, h: 10 }, text: t })) })
  const ktRightSize = (extraQual = '') => [
    'package t', 'import com.github.takahirom.roborazzi.captureRoboImage', 'import org.junit.Test', 'import org.robolectric.annotation.Config',
    'class ScreenshotTest {', '    @Test', `    @Config(qualifiers = "${extraQual}w412dp-h915dp")`, '    fun home() {',
    '        captureRoboImage("build/outputs/roborazzi/HomeScreenshot.png") { }', '    }', '}', '',
  ].join('\n')

  // R5-1. uk design + locale-less @Config (right geometry) → DRIFT; --fix writes uk-w412dp-h915dp.
  const w7 = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(specT('Home', 412, 915, ['Діагностика', 'Термінали'])))
    writeFileSync(join(code, 'ScreenshotTest.kt'), ktRightSize())
  })
  const r7 = run(w7, ['--gate'], LOCALE_ENV)
  check('R5: uk design + locale-less @Config → CAPTURE_CONFIG_DRIFT with expected uk-w412dp-h915dp, exit 2', () => {
    assert.equal(r7.status, 2)
    const hit = r7.report.issues.find((i) => i.issueKind === 'CAPTURE_CONFIG_DRIFT' && i.screen === 'Home')
    assert.ok(hit, `expected a locale DRIFT, got ${JSON.stringify(r7.report.issues)}`)
    assert.equal(hit.expected, 'uk-w412dp-h915dp')
    assert.match(hit.message, /design language uk/)
    assert.deepEqual(r7.report.designLocale, { language: 'uk', rtl: false, source: 'detected' })
    assert.deepEqual(r7.report.designLocaleEnvOverrides, ['FIGMA_SUPPORTED_LOCALES', 'FIGMA_STRING_RESOURCE_ROOTS'])
    assert.ok(Object.keys(r7.report.inputHashes).some((p) => p.endsWith('/values-uk/strings.xml')))
  })
  check('R5: --fix writes the locale segment (uk-w412dp-h915dp); re-gate PASS', () => {
    run(w7, ['--fix'], LOCALE_ENV)
    const src = readFileSync(join(w7.code, 'ScreenshotTest.kt'), 'utf8')
    assert.match(src, /@Config\(qualifiers = "uk-w412dp-h915dp"\)/)
    assert.equal(run(w7, ['--gate'], LOCALE_ENV).status, 0)
  })

  // R5-2. en design + correct locale-less @Config → --fix is a byte-identical no-op.
  const w8 = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(specT('Home', 412, 915, ['Diagnostics', 'Terminals'])))
    writeFileSync(join(code, 'ScreenshotTest.kt'), ktRightSize())
  })
  check('R5: en design (locale absent = Robolectric en default) → in sync; --fix byte-identical', () => {
    const before = readFileSync(join(w8.code, 'ScreenshotTest.kt'), 'utf8')
    const r = run(w8, ['--gate'], LOCALE_ENV)
    assert.equal(r.status, 0)
    assert.equal(r.report.overall, 'PASS')
    assert.deepEqual(r.report.designLocale, { language: 'en', rtl: false, source: 'detected' })
    run(w8, ['--fix'], LOCALE_ENV)
    assert.equal(readFileSync(join(w8.code, 'ScreenshotTest.kt'), 'utf8'), before)
  })

  // R5-3. Votable text matching no locale + no declaration → fail-closed UNDERIVABLE blocker.
  const w9 = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(specT('Home', 412, 915, ['Completely invented copy', 'Another stub line'])))
    writeFileSync(join(code, 'ScreenshotTest.kt'), ktRightSize())
  })
  check('R5: undecided votable text + no designLocale → CAPTURE_LOCALE_UNDERIVABLE BLOCKER, exit 2', () => {
    const r = run(w9, ['--gate'], LOCALE_ENV)
    assert.equal(r.status, 2)
    const hit = r.report.issues.find((i) => i.issueKind === 'CAPTURE_LOCALE_UNDERIVABLE')
    assert.ok(hit && hit.severity === 'BLOCKER')
    assert.match(hit.message, /designLocale/)
    assert.equal(r.report.designLocale.reason, 'not-confident')
  })
  check('R5: the designLocale declaration overrides an unconfident detection (locale drift + fix)', () => {
    const env = { ...LOCALE_ENV, FIGMA_DESIGN_LOCALE: 'uk' }
    const r = run(w9, ['--gate'], env)
    assert.equal(r.status, 2)
    assert.ok(!r.report.issues.some((i) => i.issueKind === 'CAPTURE_LOCALE_UNDERIVABLE'))
    const hit = r.report.issues.find((i) => i.issueKind === 'CAPTURE_CONFIG_DRIFT')
    assert.ok(hit && hit.expected === 'uk-w412dp-h915dp')
    assert.deepEqual(r.report.designLocale, { language: 'uk', rtl: false, source: 'config' })
    run(w9, ['--fix'], env)
    assert.match(readFileSync(join(w9.code, 'ScreenshotTest.kt'), 'utf8'), /qualifiers = "uk-w412dp-h915dp"/)
    assert.equal(run(w9, ['--gate'], env).status, 0)
  })

  // R5-4. RTL design language → the expected qualifier also carries ldrtl.
  const w10 = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(specT('Home', 412, 915, ['نص عربي', 'سطر آخر'])))
    writeFileSync(join(code, 'ScreenshotTest.kt'), ktRightSize())
  })
  check('R5: RTL locale (ar) → expected ar-ldrtl-…; --fix writes locale + ldrtl', () => {
    const env = { FIGMA_STRING_RESOURCE_ROOTS: localeRes, FIGMA_SUPPORTED_LOCALES: 'en,ar', FIGMA_DESIGN_LOCALE: 'ar' }
    const r = run(w10, ['--gate'], env)
    assert.equal(r.status, 2)
    const hit = r.report.issues.find((i) => i.issueKind === 'CAPTURE_CONFIG_DRIFT')
    assert.ok(hit && hit.expected === 'ar-ldrtl-w412dp-h915dp', `got ${JSON.stringify(hit)}`)
    run(w10, ['--fix'], env)
    assert.match(readFileSync(join(w10.code, 'ScreenshotTest.kt'), 'utf8'), /qualifiers = "ar-ldrtl-w412dp-h915dp"/)
    assert.equal(run(w10, ['--gate'], env).status, 0)
  })

  // R5-5. A correct explicit locale WITH region survives untouched (uk-rUA matches design uk).
  const w11 = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(specT('Home', 412, 915, ['Діагностика', 'Термінали'])))
    writeFileSync(join(code, 'ScreenshotTest.kt'), ktRightSize('uk-rUA-'))
  })
  check('R5: an explicit uk-rUA qualifier matches the uk design — in sync, never churned', () => {
    const before = readFileSync(join(w11.code, 'ScreenshotTest.kt'), 'utf8')
    assert.equal(run(w11, ['--gate'], LOCALE_ENV).status, 0)
    run(w11, ['--fix'], LOCALE_ENV)
    assert.equal(readFileSync(join(w11.code, 'ScreenshotTest.kt'), 'utf8'), before)
  })

  // R5-5b. Legal Android qualifier shapes: the BCP b+ form and mcc/mnc prefixes are parsed,
  // never corrupted. A matching b+uk+UA is in sync; a mismatched one is replaced cleanly; the
  // locale lands AFTER mcc/mnc (Android grammar).
  const w11b = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(specT('Home', 412, 915, ['Діагностика', 'Термінали'])))
    writeFileSync(join(code, 'ScreenshotTest.kt'), ktRightSize('b+uk+UA-'))
  })
  check('R5: a b+uk+UA qualifier matches the uk design — in sync, never corrupted', () => {
    const before = readFileSync(join(w11b.code, 'ScreenshotTest.kt'), 'utf8')
    assert.equal(run(w11b, ['--gate'], LOCALE_ENV).status, 0)
    run(w11b, ['--fix'], LOCALE_ENV)
    assert.equal(readFileSync(join(w11b.code, 'ScreenshotTest.kt'), 'utf8'), before)
  })
  const w11c = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(specT('Home', 412, 915, ['Діагностика', 'Термінали'])))
    writeFileSync(join(code, 'ScreenshotTest.kt'), ktRightSize('mcc310-mnc004-en-rUS-'))
  })
  check('R5: mcc/mnc prefixes are preserved and the locale is fixed AFTER them (Android grammar)', () => {
    const r = run(w11c, ['--gate'], LOCALE_ENV)
    assert.equal(r.status, 2)
    run(w11c, ['--fix'], LOCALE_ENV)
    assert.match(readFileSync(join(w11c.code, 'ScreenshotTest.kt'), 'utf8'), /qualifiers = "mcc310-mnc004-uk-w412dp-h915dp"/)
    assert.equal(run(w11c, ['--gate'], LOCALE_ENV).status, 0)
  })

  // R5-5c. Stray ldrtl on an LTR design language is a mismatch (a mirrored render), fixed by
  // stripping the segment — layout direction is owned in both directions.
  const w11d = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(specT('Home', 412, 915, ['Діагностика', 'Термінали'])))
    writeFileSync(join(code, 'ScreenshotTest.kt'), ktRightSize('uk-ldrtl-'))
  })
  check('R5: stray ldrtl on an LTR design → DRIFT with a named cause; --fix strips it', () => {
    const r = run(w11d, ['--gate'], LOCALE_ENV)
    assert.equal(r.status, 2)
    const hit = r.report.issues.find((i) => i.issueKind === 'CAPTURE_CONFIG_DRIFT')
    assert.ok(hit && /stray ldrtl/.test(hit.message), `got ${JSON.stringify(hit)}`)
    run(w11d, ['--fix'], LOCALE_ENV)
    assert.match(readFileSync(join(w11d.code, 'ScreenshotTest.kt'), 'utf8'), /qualifiers = "uk-w412dp-h915dp"/)
    assert.equal(run(w11d, ['--gate'], LOCALE_ENV).status, 0)
  })

  // R5-6. Textless spec (elements without text) → geometry-only enforcement, no locale signal.
  const w12 = ws(({ sdir, code }) => {
    writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(spec('Home', 412, 915)))
    writeFileSync(join(code, 'ScreenshotTest.kt'), ktRightSize())
  })
  check('R5: textless spec → no locale finding, designLocale reason no-signal', () => {
    const r = run(w12, ['--gate'], LOCALE_ENV)
    assert.equal(r.status, 0)
    assert.ok(!r.report.issues.some((i) => /LOCALE/.test(String(i.issueKind))))
    assert.equal(r.report.designLocale.reason, 'no-signal')
  })
  rmSync(localeRes, { recursive: true, force: true })
} catch (e) {
  fail++; console.log(`${C.red}FAIL${C.reset} check-capture-config setup threw\n     ${e.stack || e.message}`)
}

console.log(`\ncheck-capture-config.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
