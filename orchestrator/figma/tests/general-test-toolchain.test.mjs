#!/usr/bin/env node

// General-test toolchain contract (pipeline improvement 05, Phase 1).
//
// Pins the frozen pinned-stack decisions proven by the Phase-1 compatibility
// spike (AGP 9.0.1 + Kotlin 2.3.21 + Gradle 9.1.0, disposable generated
// fixture) against every canonical template surface, so drift in one place
// fails loud:
//   - the Android host test has exactly ONE enabler call site
//     (`withHostTest`) across the shipped convention sources — the proven
//     duplicate-enabler diagnostic is a configuration failure;
//   - Roborazzi/Robolectric coexist with general tests inside the same
//     single `testAndroidHostTest` task (JDK 21 launcher, record task
//     `recordRoborazziAndroidHostTest`);
//   - the bootstrap Gradle baseline is 9.1.0 (AGP 9.0.1 minimum) on both the
//     launch spec and its wizard mirror — the 8.13 prose is dead;
//   - the version catalog carries the exact pinned stack.
//
// The owner also creates a disposable generated product and invokes the real
// pinned Gradle wrapper. Host/iOS/Compose/Koin/Turbine/Ktor/Roborazzi execute
// on every macOS owner run; Room's device test executes whenever an emulator
// is attached and is mandatory in CI. The exact command is then repeated to
// prove configuration-cache reuse.

import assert from 'node:assert/strict';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, relative, resolve, sep } from 'node:path';
import { spawnSync } from 'node:child_process';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = join(HERE, '..', '..', '..');
const require = createRequire(import.meta.url);
const capabilityContract = require('../../tasks/task-test-capability-contract.cjs');

let checks = 0;
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`); }
function read(relPath) { return readFileSync(join(ROOT, relPath), 'utf8'); }

check('template and product callers keep generated Gradle execution disposable', () => {
  const source = read('orchestrator/figma/tests/general-test-toolchain.test.mjs');
  assert.doesNotMatch(source, /(?:writeFileSync|mkdirSync|rmSync)\(\s*join\(ROOT\b/,
    'the compatibility fixture must never mutate its caller repository');
});

function assertDisposableFixture(fixture) {
  const offset = relative(resolve(ROOT), resolve(fixture));
  assert.ok(offset === '..' || offset.startsWith('..' + sep),
    'generated compatibility builds must live outside the caller repository');
}

const WRAPPER_ARGS = ['wrapper', '--gradle-version', '9.1.0', '--no-daemon'];
const WRAPPER_URL_FAILURE = 'Test of distribution url https://services.gradle.org/distributions/gradle-9.1.0-bin.zip failed';
function bootstrapPinnedGradleWrapper(run) {
  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      return run('gradle', WRAPPER_ARGS);
    } catch (error) {
      // Gradle validates the immutable distribution URL with a 10-second HEAD
      // request before it writes the wrapper. The endpoint can reset that HEAD
      // while the same redirect chain and artifact are healthy. Retry only this
      // exact transport refusal; every other wrapper/configuration failure stays
      // immediate, and three consecutive URL refusals still fail the gate.
      if (!String(error && error.message || error).includes(WRAPPER_URL_FAILURE) || attempt === 3) throw error;
    }
  }
  throw new Error('unreachable wrapper bootstrap state');
}

check('version catalog reference pins the exact proven stack', () => {
  const techStack = read('orchestrator/skills/platform-build-toolkit/references/tech-stack.md');
  for (const pin of [
    'agp = "9.0.1"', 'kotlin = "2.3.21"', 'coroutines = "1.11.0"',
    'compose-plugin = "1.10.3"', 'koin = "4.2.1"', 'room = "2.8.4"',
    'ktor = "3.4.3"', 'roborazzi = "1.64.0"', 'robolectric = "4.16"',
    'junit4 = "4.13.2"'
  ]) assert.ok(techStack.includes(pin), pin);
});

check('exactly one withHostTest enabler call site across shipped convention sources', () => {
  const conventions = read('orchestrator/skills/platform-build-toolkit/references/convention-plugins.md');
  const enablers = conventions.match(/withHostTest\s*\{/g) || [];
  assert.equal(enablers.length, 1,
    'the Android host test compilation has exactly one owner (kmp.test.convention); AGP fails a second withHostTest call at configuration time');
  assert.ok(conventions.includes('withHostTest { }'),
    'the enabler owns creation only');
  assert.ok(conventions.includes('isIncludeAndroidResources = true'),
    'defaults flip reactively through the compilation DSL, never via a second enabler call');
  const deviceEnablers = conventions.match(/withDeviceTest\s*\{/g) || [];
  assert.equal(deviceEnablers.length, 1,
    'the device lane has exactly one internal enabler helper');
});

check('Roborazzi coexists inside the single host task with a narrowly scoped JDK 21 launcher', () => {
  const conventions = read('orchestrator/skills/platform-build-toolkit/references/convention-plugins.md');
  assert.ok(conventions.includes('io.github.takahirom.roborazzi'));
  for (const dep of ['roborazzi', 'robolectric', 'junit4']) {
    assert.ok(conventions.includes(`libs.findLibrary("${dep}")`), dep);
  }
  assert.ok(conventions.includes('JavaLanguageVersion.of(21)'));
  assert.ok(conventions.includes('matching { it.name == "testAndroidHostTest" }'),
    'JDK 21 launcher is scoped to the exact host task, never a broad withType<Test>');
  assert.ok(!/tasks\.withType<Test>\(\)\.configureEach\s*\{\s*\n\s*javaLauncher/.test(conventions),
    'no unnarrowed all-Test JDK override survives');
  assert.ok(conventions.includes('verifyScreenshotToolchain'));
  const gate = read('orchestrator/skills/validation-gates/references/screenshot-fidelity-gate.md');
  assert.ok(gate.includes('recordRoborazziAndroidHostTest'),
    'record task name is the frozen wrapper around testAndroidHostTest');
  const projectConfig = read('orchestrator/project-config.md');
  assert.ok(projectConfig.includes('roborazziRecordTask: recordRoborazziAndroidHostTest'));
});

check('bootstrap Gradle baseline is 9.1.0 on the launch spec and its wizard mirror', () => {
  const launch = read('orchestrator/launch.md');
  const wizard = read('orchestrator/site/scripts/data/wizard-steps.js');
  for (const surface of [launch, wizard]) {
    assert.ok(surface.includes('gradle wrapper --gradle-version 9.1.0'));
    assert.ok(surface.includes('AGP 9.0.1 requires Gradle >= 9.1.0'));
    assert.ok(!surface.includes('8.13'), 'the disproven 8.13 baseline must not survive anywhere');
  }
});

check('foojay stays the only toolchain provisioning path', () => {
  const settingsRef = read('orchestrator/skills/platform-build-toolkit/references/version-catalog-and-settings.md');
  assert.ok(settingsRef.includes('org.gradle.toolchains.foojay-resolver-convention'));
  const conventions = read('orchestrator/skills/platform-build-toolkit/references/convention-plugins.md');
  assert.ok(conventions.includes('never commit `org.gradle.java.installations.paths`'),
    'machine-specific JDK paths stay forbidden');
});

// Typed prerequisite, not a Gradle stack trace five minutes in: the pinned
// AGP fixtures need a real Android SDK. Resolve it from the environment or
// the standard macOS install location; a machine without one fails fast and
// loud — never a silent skip (CI provides ANDROID_HOME on every runner).
function resolveAndroidSdk() {
  const home = process.env.HOME || '';
  const darwinDefault = process.platform === 'darwin' && home ? join(home, 'Library', 'Android', 'sdk') : null;
  const androidSdk = [process.env.ANDROID_HOME, process.env.ANDROID_SDK_ROOT, darwinDefault]
    .find((candidate) => candidate && existsSync(candidate));
  assert.ok(androidSdk,
    'Android SDK prerequisite missing: export ANDROID_HOME (or ANDROID_SDK_ROOT, or install to ~/Library/Android/sdk) — the pinned AGP fixture cannot run without a real SDK');
  assert.ok(existsSync(join(androidSdk, 'platform-tools', 'adb')),
    'Android SDK prerequisite missing: the selected SDK must provide platform-tools/adb');
  return androidSdk;
}

function deviceAttached(androidSdk) {
  const sdkAdb = join(androidSdk, 'platform-tools', 'adb');
  const adb = spawnSync(sdkAdb, ['devices'], { encoding: 'utf8' });
  const hasDevice = adb.status === 0 && /^\S+\s+device$/m.test(String(adb.stdout));
  if (process.env.CI === 'true') assert.equal(hasDevice, true, 'CI fixture requires the configured Android emulator');
  return hasDevice;
}

function generatedFixture() {
  const androidSdk = resolveAndroidSdk();
  const fixture = mkdtempSync(join(tmpdir(), 'mandatory-test-stack-'));
  assertDisposableFixture(fixture);
  const write = (relative, text) => {
    const file = join(fixture, relative);
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, text);
  };
  const run = (executable, args, { expected = 0, timeout = 15 * 60 * 1000 } = {}) => {
    const result = spawnSync(executable, args, {
      cwd: fixture, encoding: 'utf8', timeout, maxBuffer: 32 * 1024 * 1024,
      env: { ...process.env, ANDROID_HOME: androidSdk, GRADLE_OPTS: '-Dorg.gradle.daemon.performance.disable-logging=true' }
    });
    const output = String(result.stdout || '') + String(result.stderr || '');
    assert.equal(result.error, undefined, output);
    assert.equal(result.status, expected, output);
    return output;
  };
  const gradle = (args, options) => run(join(fixture, 'gradlew'), args, options);

  try {
    write('settings.gradle.kts', 'rootProject.name = "wrapper-bootstrap"\n');
    write('build.gradle.kts', '\n');
    bootstrapPinnedGradleWrapper(run);

    write('settings.gradle.kts', `
pluginManagement { repositories { google(); gradlePluginPortal(); mavenCentral() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0" }
rootProject.name = "mandatory-test-stack"
include(":core", ":ui")
`.trimStart());
    write('gradle.properties', 'org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1g\n');
    write('build.gradle.kts', `
import java.security.MessageDigest
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.3.21" apply false
    id("com.android.kotlin.multiplatform.library") version "9.0.1" apply false
    id("org.jetbrains.compose") version "1.10.3" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("io.github.takahirom.roborazzi") version "1.64.0" apply false
}

val inventoryModel = """{"domain":"test-capability-inventory","generatedBy":":testCapabilityInventory","modules":[{"capabilities":["base","coroutines","di","flow","network","room"],"lanes":{"android-device":{"compilation":"deviceTest","sourceSet":"androidDeviceTest","taskPath":":core:connectedAndroidDeviceTest"},"host":{"compilation":"hostTest","sourceSet":"androidHostTest","taskPath":":core:testAndroidHostTest"},"ios-simulator":{"compilation":"test","sourceSet":"iosSimulatorArm64Test","taskPath":":core:iosSimulatorArm64Test"}},"path":":core"},{"capabilities":["base","compose-ui","screenshot"],"lanes":{"host":{"compilation":"hostTest","sourceSet":"androidHostTest","taskPath":":ui:testAndroidHostTest"},"ios-simulator":{"compilation":"test","sourceSet":"iosSimulatorArm64Test","taskPath":":ui:iosSimulatorArm64Test"},"screenshot":{"compilation":"hostTest","sourceSet":"androidHostTest","taskPath":":ui:verifyRoborazziAndroidHostTest"}},"path":":ui"}],"version":1}"""
val digest = MessageDigest.getInstance("SHA-256").digest(("test-capability-inventory\\u0000" + inventoryModel).toByteArray())
val inventoryHash = "sha256:" + digest.joinToString("") { "%02x".format(it) }
val inventoryJson = inventoryModel.dropLast(1) + ",\\\"inventoryHash\\\":\\\"" + inventoryHash + "\\\"}"
abstract class InventoryTask : DefaultTask() {
    @get:Input abstract val content: Property<String>
    @get:OutputFile abstract val outputFile: RegularFileProperty
    @TaskAction fun writeInventory() {
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(content.get() + "\\n")
    }
}
tasks.register<InventoryTask>("testCapabilityInventory") {
    content.set(inventoryJson)
    outputFile.set(layout.buildDirectory.file("test-capability/inventory.json"))
}
`.trimStart());
    write('core/build.gradle.kts', `
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}
kotlin {
    androidLibrary {
        namespace = "probe.core"
        compileSdk = 36
        minSdk = 23
        withHostTest { }
        withDeviceTest { }
    }
    iosSimulatorArm64()
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
            implementation("app.cash.turbine:turbine:1.2.1")
            implementation("io.ktor:ktor-client-mock:3.4.3")
            implementation("io.insert-koin:koin-core:4.2.1")
        }
        getByName("androidHostTest").dependencies {
            implementation("io.insert-koin:koin-test:4.2.1")
        }
        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test"))
            implementation("androidx.room:room-testing:2.8.4")
            implementation("androidx.test:runner:1.7.0")
            implementation("androidx.test:core:1.7.0")
            implementation("junit:junit:4.13.2")
        }
    }
}
`.trimStart());
    write('core/src/commonTest/kotlin/probe/CoreProbeTest.kt', `
package probe
import app.cash.turbine.test
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
class CoreProbeTest {
    @Test fun pinnedLibrariesExecute() = runTest {
        flowOf("turbine").test { assertEquals("turbine", awaitItem()); awaitComplete() }
        val engine = MockEngine { respondOk("ktor") }
        assertEquals("MockEngine", engine::class.simpleName)
        assertNotNull(module { single { "koin" } })
    }
}
`.trimStart());
    write('core/src/androidHostTest/kotlin/probe/KoinProbeTest.kt', `
package probe
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.verify.MissingKoinDefinitionException
import org.koin.test.verify.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
class ProbeId(val raw: String)
class ProbeRepo(val id: ProbeId, val label: String)
class KoinProbeTest {
    // Runtime-injected parameter definition: verify() must accept it only
    // when the parameter type is DECLARED, and must fail loud when it is not
    // — the declaration can never hide a genuinely missing binding.
    private val parameterized = module {
        single { "label" }
        single { (id: ProbeId) -> ProbeRepo(id, get()) }
    }
    @Test fun graphVerifies() { module { single { "verified" } }.verify() }
    @Test fun injectedParameterVerifiesWhenDeclared() {
        parameterized.verify(extraTypes = listOf(ProbeId::class))
    }
    @Test fun undeclaredInjectedParameterFailsVerify() {
        assertFailsWith<MissingKoinDefinitionException> { parameterized.verify() }
    }
    @Test fun injectedParameterResolvesAtRuntime() {
        val app = koinApplication { modules(parameterized) }
        assertEquals("n1", app.koin.get<ProbeRepo> { parametersOf(ProbeId("n1")) }.id.raw)
    }
}
`.trimStart());
    write('core/src/androidDeviceTest/kotlin/probe/RoomDeviceProbeTest.kt', `
package probe
import androidx.room.testing.MigrationTestHelper
import kotlin.test.Test
import kotlin.test.assertEquals
class RoomDeviceProbeTest {
    @Test fun roomTestingRuntimeLoadsOnDevice() {
        assertEquals("MigrationTestHelper", MigrationTestHelper::class.java.simpleName)
    }
}
`.trimStart());
    write('ui/build.gradle.kts', `
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.takahirom.roborazzi")
}
kotlin {
    androidLibrary {
        namespace = "probe.ui"
        compileSdk = 36
        minSdk = 23
        withHostTest { isIncludeAndroidResources = true }
    }
    iosSimulatorArm64()
    sourceSets {
        commonTest.dependencies { implementation(kotlin("test")); implementation(compose.runtime) }
        getByName("androidHostTest").dependencies {
            implementation("io.github.takahirom.roborazzi:roborazzi:1.64.0")
            implementation("junit:junit:4.13.2")
        }
    }
}
val toolchains = extensions.getByType<JavaToolchainService>()
tasks.withType<Test>().matching { it.name == "testAndroidHostTest" }.configureEach {
    javaLauncher.set(toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}
`.trimStart());
    write('ui/src/commonTest/kotlin/probe/ComposeProbeTest.kt', `
package probe
import androidx.compose.runtime.mutableStateOf
import kotlin.test.Test
import kotlin.test.assertEquals
class ComposeProbeTest {
    @Test fun composeRuntimeExecutes() { assertEquals("compose", mutableStateOf("compose").value) }
}
`.trimStart());
    write('ui/src/androidHostTest/kotlin/probe/RoborazziProbeTest.kt', `
package probe
import com.github.takahirom.roborazzi.RoborazziOptions
import kotlin.test.Test
import kotlin.test.assertTrue
class RoborazziProbeTest {
    @Test fun roborazziRuntimeLoads() {
        assertTrue(RoborazziOptions::class.qualifiedName!!.contains("Roborazzi"))
    }
}
`.trimStart());

    const version = gradle(['--version', '--no-daemon']);
    assert.match(version, /Gradle 9\.1\.0/);
    const tasks = gradle(['tasks', '--all', '--configuration-cache', '--no-daemon']);
    for (const task of ['core:testAndroidHostTest', 'core:iosSimulatorArm64Test',
      'core:connectedAndroidDeviceTest', 'ui:recordRoborazziAndroidHostTest']) {
      assert.ok(tasks.includes(task), task);
    }
    const lanes = [':core:testAndroidHostTest', ':ui:testAndroidHostTest',
      ':ui:recordRoborazziAndroidHostTest', ':core:compileAndroidDeviceTestSources'];
    if (process.platform === 'darwin') lanes.push(':core:iosSimulatorArm64Test', ':ui:iosSimulatorArm64Test');
    const first = gradle([...lanes, '--configuration-cache', '--stacktrace', '--no-daemon']);
    assert.match(first, /> Task :core:testAndroidHostTest/);
    assert.match(first, /> Task :ui:recordRoborazziAndroidHostTest/);
    if (process.platform === 'darwin') assert.match(first, /> Task :core:iosSimulatorArm64Test/);
    const second = gradle([...lanes, '--configuration-cache', '--stacktrace', '--no-daemon']);
    assert.match(second, /Reusing configuration cache|Configuration cache entry reused/);
    assert.ok(existsSync(join(fixture, 'core/build/test-results/testAndroidHostTest/TEST-probe.KoinProbeTest.xml')));
    assert.ok(existsSync(join(fixture, 'ui/build/test-results/roborazzi/androidHostTest/results-summary.json')));
    if (process.platform === 'darwin') {
      assert.ok(existsSync(join(fixture, 'core/build/test-results/iosSimulatorArm64Test/TEST-probe.CoreProbeTest.xml')));
    }

    gradle(['testCapabilityInventory', '--configuration-cache', '--no-daemon']);
    const inventory = JSON.parse(readFileSync(join(fixture, 'build/test-capability/inventory.json'), 'utf8'));
    capabilityContract.validateInventory(inventory);
    assert.ok(capabilityContract.allowedTaskPaths(inventory).includes(':core:connectedAndroidDeviceTest'));

    const hasDevice = deviceAttached(androidSdk);
    if (hasDevice) {
      const device = gradle([':core:connectedAndroidDeviceTest', '--configuration-cache', '--stacktrace', '--no-daemon']);
      assert.match(device, /Starting 1 tests|Finished 1 tests|connectedAndroidDeviceTest/);
      assert.ok(existsSync(join(fixture,
        'core/build/outputs/androidTest-results/connected/androidMain')));
    }

    const production = gradle([':core:dependencies', '--configuration', 'androidCompileClasspath', '--no-daemon']);
    for (const testOnly of ['app.cash.turbine', 'kotlinx-coroutines-test', 'ktor-client-mock', 'koin-test', 'room-testing']) {
      assert.ok(!production.includes(testOnly), 'test dependency leaked into production: ' + testOnly);
    }
    const wrongAlias = spawnSync(join(fixture, 'gradlew'), [':core:androidDeviceTests', '--no-daemon'], {
      cwd: fixture, encoding: 'utf8', timeout: 5 * 60 * 1000,
      env: { ...process.env, ANDROID_HOME: androidSdk }
    });
    assert.notEqual(wrongAlias.status, 0, 'guessed device alias must fail');
    assert.match(String(wrongAlias.stdout) + String(wrongAlias.stderr), /task 'androidDeviceTests' not found/i,
      'the alias refusal must not be satisfied by an unrelated SDK/environment failure');
    const coreBuild = readFileSync(join(fixture, 'core/build.gradle.kts'), 'utf8');
    write('core/build.gradle.kts', coreBuild.replace('withHostTest { }', 'withHostTest { }\n        withHostTest { }'));
    const duplicate = spawnSync(join(fixture, 'gradlew'), ['tasks', '--no-daemon'], {
      cwd: fixture, encoding: 'utf8', timeout: 5 * 60 * 1000, maxBuffer: 8 * 1024 * 1024,
      env: { ...process.env, ANDROID_HOME: androidSdk }
    });
    assert.notEqual(duplicate.status, 0, 'duplicate host enabler must fail configuration');
    assert.match(String(duplicate.stdout) + String(duplicate.stderr), /host tests have already been enabled|withHostTest/i);
  } finally {
    rmSync(fixture, { recursive: true, force: true });
  }
}

check('disposable pinned-stack product executes real lanes and reuses configuration cache', generatedFixture);

// ---------------------------------------------------------------------------
// Room migration fixture (pipeline improvement 05 leftover closure).
//
// Proves on the exact pinned stack (AGP-KMP 9.0.1 + Kotlin 2.3.21 + KSP 2.3.4
// + room-gradle-plugin/room-compiler 2.8.4) that:
//   - the KSP2 pipeline exports versioned Room schemas through the room
//     plugin's schemaDirectory on a KMP androidLibrary module (v1 and v2 are
//     produced by two real compiles);
//   - a REAL v1→v2 migration executes on the device lane and preserves data,
//     validated by Room's own open-time schema verification (a wrong or
//     missing ALTER fails the open loud);
//   - a missing required migration fails closed (IllegalStateException);
//   - the destructive fallback path is observably different (data wiped), so
//     the migration proof can never silently ride the destructive shortcut.
//
// Deliberate boundary at this pin: androidx.room.testing MigrationTestHelper
// on Android loads schemas ONLY from instrumentation-APK assets (verified
// against the shipped 2.8.4 bytecode: AndroidMigrationTestHelper.loadSchema
// reads instrumentation/target-context assets; the File-based constructor
// routes the file to the DATABASE path, not the schema lookup), while the
// AGP-KMP deviceTest component exposes no assets surface
// (variant.deviceTests[..].sources.assets == null, no conventional source
// dir, an empty merge task). Wiring assets through internal intermediates
// would be a forbidden shim, so the helper-based json-bundle comparison
// stays a documented product-side remain until the pin gains a deviceTest
// assets surface.
// ---------------------------------------------------------------------------

function roomMigrationFixture() {
  const androidSdk = resolveAndroidSdk();
  const fixture = mkdtempSync(join(tmpdir(), 'room-migration-probe-'));
  assertDisposableFixture(fixture);
  const write = (relative, text) => {
    const file = join(fixture, relative);
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, text);
  };
  const run = (executable, args, { expected = 0, timeout = 15 * 60 * 1000 } = {}) => {
    const result = spawnSync(executable, args, {
      cwd: fixture, encoding: 'utf8', timeout, maxBuffer: 32 * 1024 * 1024,
      env: { ...process.env, ANDROID_HOME: androidSdk, GRADLE_OPTS: '-Dorg.gradle.daemon.performance.disable-logging=true' }
    });
    const output = String(result.stdout || '') + String(result.stderr || '');
    assert.equal(result.error, undefined, output);
    assert.equal(result.status, expected, output);
    return output;
  };
  const gradle = (args, options) => run(join(fixture, 'gradlew'), args, options);

  const dbV1 = `
package probe.db
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
@Entity(tableName = "note") data class Note(@PrimaryKey val id: Long, val title: String)
@Dao interface NoteDao { @Insert fun insert(note: Note); @Query("SELECT COUNT(*) FROM note") fun count(): Long }
@Database(entities = [Note::class], version = 1, exportSchema = true)
abstract class ProbeDatabase : RoomDatabase() { abstract fun notes(): NoteDao }
`.trimStart();

  const dbV2 = `
package probe.db
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
@Entity(tableName = "note") data class Note(@PrimaryKey val id: Long, val title: String, val label: String)
@Dao interface NoteDao { @Insert fun insert(note: Note); @Query("SELECT COUNT(*) FROM note") fun count(): Long }
@Database(entities = [Note::class], version = 2, exportSchema = true)
abstract class ProbeDatabase : RoomDatabase() {
    abstract fun notes(): NoteDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE note ADD COLUMN label TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
`.trimStart();

  const placeholderDeviceTest = `
package probe.db
import kotlin.test.Test
import kotlin.test.assertTrue
class PlaceholderProbeTest { @Test fun compiles() { assertTrue(true) } }
`.trimStart();

  const migrationDeviceTest = `
package probe.db
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
class MigrationProbeTest {
    private fun seedV1(context: Context, name: String) {
        context.deleteDatabase(name)
        val raw = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        raw.execSQL("CREATE TABLE IF NOT EXISTS note (id INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL)")
        raw.execSQL("INSERT INTO note(id, title) VALUES (1, 'first')")
        raw.version = 1
        raw.close()
    }
    @Test fun migrationPreservesRows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        seedV1(context, "migration-probe.db")
        val db = Room.databaseBuilder(context, ProbeDatabase::class.java, "migration-probe.db")
            .addMigrations(ProbeDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        assertEquals(1L, db.notes().count())
        db.close()
    }
    @Test fun missingMigrationFailsClosed() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        seedV1(context, "missing-migration-probe.db")
        val db = Room.databaseBuilder(context, ProbeDatabase::class.java, "missing-migration-probe.db")
            .allowMainThreadQueries()
            .build()
        assertFailsWith<IllegalStateException> { db.notes().count() }
        db.close()
    }
    @Test fun destructiveFallbackWipesInsteadOfMigrating() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        seedV1(context, "destructive-probe.db")
        val db = Room.databaseBuilder(context, ProbeDatabase::class.java, "destructive-probe.db")
            .fallbackToDestructiveMigration(true)
            .allowMainThreadQueries()
            .build()
        assertEquals(0L, db.notes().count())
        db.close()
    }
}
`.trimStart();

  try {
    write('settings.gradle.kts', 'rootProject.name = "wrapper-bootstrap"\n');
    write('build.gradle.kts', '\n');
    bootstrapPinnedGradleWrapper(run);

    write('settings.gradle.kts', `
pluginManagement { repositories { google(); gradlePluginPortal(); mavenCentral() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0" }
rootProject.name = "room-migration-probe"
include(":db")
`.trimStart());
    write('gradle.properties', 'org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1g\nksp.useKSP2=true\n');
    write('build.gradle.kts', `
plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.3.21" apply false
    id("com.android.kotlin.multiplatform.library") version "9.0.1" apply false
    id("com.google.devtools.ksp") version "2.3.4" apply false
    id("androidx.room") version "2.8.4" apply false
}
`.trimStart());
    write('db/build.gradle.kts', `
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.google.devtools.ksp")
    id("androidx.room")
}
room { schemaDirectory("$projectDir/schemas") }
kotlin {
    androidLibrary {
        namespace = "probe.db"
        compileSdk = 36
        minSdk = 23
        withDeviceTest { }
    }
    sourceSets {
        androidMain.dependencies {
            implementation("androidx.room:room-runtime:2.8.4")
        }
        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test"))
            implementation("junit:junit:4.13.2")
            implementation("androidx.test:runner:1.7.0")
            implementation("androidx.test:core:1.7.0")
            implementation("androidx.room:room-testing:2.8.4")
        }
    }
}
dependencies { add("kspAndroid", "androidx.room:room-compiler:2.8.4") }
`.trimStart());

    // Phase A: the v1 schema is exported by a real KSP compile.
    write('db/src/androidMain/kotlin/probe/db/ProbeDatabase.kt', dbV1);
    write('db/src/androidDeviceTest/kotlin/probe/db/PlaceholderProbeTest.kt', placeholderDeviceTest);
    gradle([':db:kspAndroidMain', '--no-daemon']);
    assert.ok(existsSync(join(fixture, 'db/schemas/probe.db.ProbeDatabase/1.json')), 'v1 schema exported');

    // Phase B: v2 + the real migration and its device tests.
    write('db/src/androidMain/kotlin/probe/db/ProbeDatabase.kt', dbV2);
    rmSync(join(fixture, 'db/src/androidDeviceTest/kotlin/probe/db/PlaceholderProbeTest.kt'));
    write('db/src/androidDeviceTest/kotlin/probe/db/MigrationProbeTest.kt', migrationDeviceTest);
    gradle([':db:kspAndroidMain', '--no-daemon']);
    assert.ok(existsSync(join(fixture, 'db/schemas/probe.db.ProbeDatabase/2.json')), 'v2 schema exported');

    // Phase C: the migration executes on the device lane whenever an emulator
    // is attached, and is mandatory in CI (same policy as the main fixture).
    if (deviceAttached(androidSdk)) {
      const device = gradle([':db:connectedAndroidDeviceTest', '--no-daemon']);
      assert.match(device, /connectedAndroidDeviceTest/);
      assert.ok(existsSync(join(fixture,
        'db/build/outputs/androidTest-results/connected/androidMain')));
    }
  } finally {
    rmSync(fixture, { recursive: true, force: true });
  }
}

check('Room 2.8.4 migrates v1→v2 on the device lane with schemas exported by the pinned KSP pipeline', roomMigrationFixture);

console.log(`general-test-toolchain: ${checks} checks passed`);
