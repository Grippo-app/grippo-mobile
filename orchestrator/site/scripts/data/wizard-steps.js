  // ----------------------------------------------------------------------
  // Helpers shared with the wizard panel and the task form.
  // templates.js is import-free (pure token substitution), so pulling it in
  // here keeps this data module dependency-clean while letting stepPrompt()
  // be the ONE place a step prompt is rendered.
  // ----------------------------------------------------------------------

  import { templates } from '../templates.js';

  export const helpers = {};

  helpers.isAutoSkipped = function (step, setup) {
    return typeof step.skipWhen === 'function' && step.skipWhen(setup || {});
  };

  helpers.wizardComplete = function (state) {
    var setup = (state && state.setup) || {};
    var done = (state && state.progress && state.progress.wizardStepsDone) || [];
    return wizardSteps.every(function (step) {
      return helpers.isAutoSkipped(step, setup) || done.indexOf(step.id) >= 0;
    });
  };

  // Appended to EVERY step prompt by stepPrompt() below — the bootstrap is
  // unattended: a step that ends its turn with a question ("Want me to
  // commit…?") wedges auto-run in awaiting-input until a human answers.
  // Mirrors launch.md § "Unattended run — never ask".
  helpers.autonomyFooter = [
    'Unattended step: never ask the user anything and never end the turn with a',
    'question or an offer ("Want me to…?"). Inputs come from',
    'orchestrator/project-config.md and the step text. Fixes needed to make this',
    'step\'s verification pass are in scope — apply them and list them in the step',
    'report instead of asking; leave them in the working tree with the rest of the',
    'bootstrap output. Resolve decisions by the documented defaults; if a required',
    'input is genuinely missing, end with `BLOCKED: <what is missing>` — a report,',
    'not a question.'
  ].join('\n');

  // THE step-prompt renderer. Every surface that displays, copies, or sends a
  // step prompt (panels/wizard.js and auto-run.js) must go through this so
  // what the user reads, what Copy puts on the clipboard, and what reaches
  // the setup session are byte-identical: build(setup) / promptTemplate via
  // token substitution, plus the unattended-run footer.
  helpers.stepPrompt = function (step, setup) {
    var body = typeof step.build === 'function'
      ? step.build(setup)
      : templates.render(step.promptTemplate || '', setup);
    return body + '\n\n' + helpers.autonomyFooter;
  };

  // ----------------------------------------------------------------------
  // Local utilities for the `build(setup)` steps. The YAML builder lives here
  // (imported by panels/setup.js) so wizard-steps.js has no dependency on a
  // panel script.
  // ----------------------------------------------------------------------

  function safe(v) { return v == null ? '' : String(v); }

  function sortLocales(arr) {
    var src = Array.isArray(arr) ? arr.slice() : ['en'];
    var head = src.indexOf('en') >= 0 ? ['en'] : [];
    var rest = src.filter(function (l) { return l !== 'en'; }).sort();
    return head.concat(rest);
  }

  function derivedProductPackage(setup) {
    var product = safe(setup.productName).toLowerCase();
    if (!product) return '';
    if (setup.orgName) return 'com.' + setup.orgName + '.' + product;
    return 'com.' + product;
  }

  function diHandWritten(setup) {
    var auth = Array.isArray(setup.authMethods) ? setup.authMethods : [];
    var mods = [];
    if (auth.indexOf('google') >= 0) mods.push('GoogleAuthModule');
    if (auth.indexOf('apple') >= 0) mods.push('AppleAuthModule');
    return mods;
  }

  function buildYaml(setup) {
    var pkg = derivedProductPackage(setup) || 'com.<org>.<product>';
    var product = safe(setup.productName) || '<Product>';
    var appId = safe(setup.applicationId) || pkg;
    var host = safe(setup.backendHost) || '<product-domain>';
    var ios = safe(setup.iosFrameworkName) || 'shared';
    var face = safe(setup.typefaceFactory) || '<typeface>';
    var locales = sortLocales(setup.supportedLocales || ['en']);
    var mods = diHandWritten(setup);

    var lines = ['---'];
    lines.push('productName: ' + product);
    lines.push('productPackage: ' + pkg);
    lines.push('apiClassName: ' + product + 'Api');
    lines.push('backendHost: ' + host);
    lines.push('applicationId: ' + appId);
    lines.push('iosFrameworkName: ' + ios);
    lines.push('iosEnabled: ' + (setup.iosEnabled === false ? 'false' : 'true'));
    lines.push('firebaseEnabled: ' + (setup.firebaseEnabled === false ? 'false' : 'true'));
    lines.push('codexEnabled: ' + (setup.codexEnabled != null ? String(setup.codexEnabled) : 'auto'));
    // Pinned to auto by design — no Setup control. Edit orchestrator/project-config.md post-bootstrap to force true/false.
    lines.push('verifyEnabled: auto');
    lines.push('prelaunch: ' + (setup.prelaunch === false ? 'false' : 'true'));
    lines.push('supportedLocales:');
    for (var i = 0; i < locales.length; i++) lines.push('  - ' + locales[i]);
    lines.push('typefaceFactory: ' + face);
    lines.push('featuresWithRootComponentSuffix: []');
    lines.push('diHandWrittenModules: ' + (mods.length === 0 ? '[]' : '[' + mods.join(', ') + ']'));
    lines.push('figmaEnabled: ' + (setup.figmaEnabled === true ? 'true' : 'false'));
    lines.push('figmaLibraryUrl: ' + (safe(setup.figmaLibraryUrl) || '<figma-library-url>'));
    lines.push('screenshotPixelGate: ' + (['strict', 'advisory', 'off'].indexOf(setup.screenshotPixelGate) >= 0 ? setup.screenshotPixelGate : 'strict'));
    // Template-default Gradle task names are not Setup-editable; a product whose module
    // layout differs edits orchestrator/project-config.md post-bootstrap. Emitted so the
    // written config carries the keys explicitly (absent keys fall back to these same values).
    lines.push('androidAssembleTask: :androidApp:assembleDebug');
    lines.push('sharedFrameworkTask: :shared:assembleSharedDebugXCFramework');
    lines.push('roborazziRecordTask: recordRoborazziAndroidHostTest');
    lines.push('moduleCompileTask: compileAndroidMain');
    // Pinned to auto by design — no Setup control; edit orchestrator/project-config.md post-bootstrap to force true/false.
    lines.push('backendContractEnabled: auto');
    lines.push('---');
    return lines.join('\n');
  }

  // Expose the YAML builder so other panels (Setup preview, Setup
  // "Copy as Claude prompt") can reuse the exact same serialization
  // without re-implementing it.
  helpers.buildYaml = buildYaml;

  // ----------------------------------------------------------------------
  // Build functions for steps whose body depends on list-valued or
  // conditional setup fields. Steps using `promptTemplate` go through
  // App.templates.render() at render time; build-based steps emit a final
  // string directly.
  // ----------------------------------------------------------------------

  function buildStep10(setup) {
    var firebase = setup.firebaseEnabled !== false;
    var lines = [];
    lines.push('Step 10 — implement :androidApp');
    lines.push('');
    lines.push('Read first:');
    lines.push('- orchestrator/launch.md (Step 10)');
    lines.push('- the platform-build-toolkit skill (orchestrator/skills/platform-build-toolkit/, its android-app-module reference)');
    lines.push('');
    lines.push('Create:');
    lines.push('');
    if (firebase) {
      lines.push('- `App.kt` — `Application` subclass with `Koin.init { androidContext(this); androidLogger() }` and `FirebaseProvider.setup(this)`.');
    } else {
      lines.push('- `App.kt` — `Application` subclass with `Koin.init { androidContext(this); androidLogger() }`.');
    }
    lines.push('- `MainActivity.kt` — single Activity with `retainedComponent { RootComponent(it, close = ::finishAffinity, deeplink = intent.getStringExtra(LocalNotificationExtras.DEEPLINK)) }` and `setContent { root.Render() }`. The `deeplink` arg is a raw `String?` from the launch intent; the String->Deeplink parse already lives in `RootViewModel.parseDeeplink(raw)`, so do not add a parseDeeplink helper here.');
    lines.push('- `AndroidManifest.xml` — `<application android:name=".App">`, `<activity android:name=".MainActivity" android:exported="true">` with the main launcher intent filter and deeplink filters if any.');
    if (firebase) {
      lines.push('- `build.gradle.kts` — verbatim from the platform-build-toolkit skill (its android-app-module reference) (keep the `google-services` and `firebase-crashlytics` plugin lines).');
    } else {
      lines.push('- `build.gradle.kts` — verbatim from the platform-build-toolkit skill (its android-app-module reference), omitting the `google-services` and `firebase-crashlytics` plugin lines.');
    }
    lines.push('- `proguard-rules.pro` — empty for now (R8 defaults).');
    if (firebase) {
      lines.push('- `google-services.json` — placeholder file; the user will replace it with a real Firebase config later.');
    }
    lines.push('');
    lines.push('Verify: `./gradlew :androidApp:assembleDebug`.');
    return lines.join('\n');
  }

  function buildStep11(setup) {
    var firebase = setup.firebaseEnabled !== false;
    var ios = setup.iosFrameworkName || 'shared';
    var lines = [];
    lines.push('Step 11 — implement :iosApp');
    lines.push('');
    lines.push('Read first:');
    lines.push('- orchestrator/launch.md (Step 11)');
    lines.push('- the platform-build-toolkit skill (orchestrator/skills/platform-build-toolkit/, its ios-app-project reference — the drop-in files)');
    lines.push('');
    lines.push('Build :iosApp from the verbatim drop-in fences in the platform-build-toolkit skill\'s ios-app-project reference: copy each file to its Target path and substitute placeholders, exactly like the base-class reference implementations in Step 4.');
    lines.push('');
    lines.push('Apply the ios-app-project reference:');
    lines.push('1. Create every file from its fence at the listed Target path. Substitute <Product>, <org>, <iosFrameworkName>=' + ios + ' from orchestrator/project-config.md; <bundleId> = the iOS bundle id (same as applicationId from orchestrator/project-config.md — no platform suffix); <TEAM_ID> = your Apple Developer Team ID (empty is fine for the simulator). See the placeholder legend at the top of the ios-app-project reference.');
    if (firebase) {
      lines.push('2. firebaseEnabled=true: use the "project.pbxproj (firebaseEnabled: true)" fence; keep IosFirebase{Analytics,Crashlytics,Messaging}.swift + the placeholder GoogleService-Info.plist; keep the // region firebase-conditional blocks in AppDelegate.swift. Replace GoogleService-Info.plist with the real file from the Firebase console.');
    } else {
      lines.push('2. firebaseEnabled=false: use the "project.pbxproj (firebaseEnabled: false)" fence; omit the IosFirebase*.swift files and GoogleService-Info.plist; strip everything between the // region firebase-conditional / // endregion firebase-conditional markers in AppDelegate.swift.');
    }
    lines.push('3. chmod +x iosApp/run-ios.sh');
    lines.push('4. Build the framework once so Xcode can resolve `import ' + ios + '`:');
    lines.push('   ./gradlew :shared:assemble' + (ios.charAt(0).toUpperCase() + ios.slice(1)) + 'DebugXCFramework');
    lines.push('');
    lines.push('The pbxproj ships a Compile Kotlin Framework run-script (embedAndSignAppleFrameworkForXcode) + FRAMEWORK_SEARCH_PATHS, so the framework rebuilds on every Xcode build (no drag-and-drop). The shared scheme makes the iosApp run config appear in Android Studio. Run headless with ./iosApp/run-ios.sh.');
    return lines.join('\n');
  }

  function buildStep12(setup) {
    var ios = setup.iosFrameworkName || 'shared';
    var iosCap = ios.charAt(0).toUpperCase() + ios.slice(1);
    var iosOn = setup.iosEnabled !== false;
    var lines = [];
    lines.push('Step 12 — verify end-to-end');
    lines.push('');
    lines.push('Read first:');
    lines.push('- orchestrator/launch.md (Step 12)');
    lines.push('');
    lines.push('Run:');
    lines.push('');
    lines.push('```bash');
    if (iosOn) {
      lines.push('./gradlew :shared:assemble' + iosCap + 'DebugXCFramework');
    }
    lines.push('./gradlew :androidApp:assembleDebug');
    lines.push('```');
    lines.push('');
    lines.push('Both must succeed.');
    lines.push('');
    lines.push('Foundation integrity gate — a green assemble does NOT prove the app survives its first error (TODO(...) returns Nothing, so a stubbed load-bearing branch like DialogConfig.ErrorDisplay -> createChild compiles and ships, then crashes with NotImplementedError when any error fires). Fail the bootstrap if a stub survives in foundation source:');
    lines.push('');
    lines.push('```bash');
    lines.push('node orchestrator/site/server/foundation-stub-scan.js');
    lines.push('```');
    lines.push('');
    lines.push('Exit 0 = clean; on hits it prints file:line per stub and exits 1. The scan is comment/string-aware: a comment or string literal that merely mentions TODO(...)/NotImplementedError does not fail the gate — only real code-position stubs do. This is the same implementation behind this step\'s ✓ validator, so the moment this command passes, the wizard marks Step 12 done on its own — do not mark it manually and do not re-derive the scan with a raw rg (raw greps false-positive on comments).');
    lines.push('');
    lines.push('Run the Android app: a screen renders with the toolbar and at least one element from the design system.');
    if (iosOn) {
      lines.push('Run the iOS app — open iosApp/iosApp.xcodeproj in Xcode, pick the iosApp config in Android Studio (the shared scheme surfaces it), or run ./iosApp/run-ios.sh. The same screen renders.');
      lines.push('');
      lines.push('If both succeed, the foundation is complete.');
    } else {
      lines.push('');
      lines.push('iOS targets are disabled in this project — Android verification is sufficient. If iOS is later re-enabled, run `./gradlew :shared:assemble' + iosCap + 'DebugXCFramework` and bring up the iOS app then.');
    }
    return lines.join('\n');
  }

  // ----------------------------------------------------------------------
  // promptTemplate strings — quoted from `orchestrator/launch.md` per the
  // wizard prompt contract. Tokens such as `<Product>`, `<product>`, `<org>`,
  // `<product-domain>`, `<iosFrameworkName>`, `<IosFrameworkName>`,
  // `<firstDomain>`, `<typeface>` are substituted at render time via
  // `App.templates.render(...)`. Meta-placeholders (`<X>`, `<x>`,
  // `<Entity>`) intentionally remain literal — they signal "your domain
  // type here" to the agent.
  // ----------------------------------------------------------------------

  var TPL = {};

  TPL.step2 = [
    'Step 2 — initialize the build system',
    '',
    'Read first:',
    '- orchestrator/launch.md (Step 2)',
    '- the platform-build-toolkit skill (orchestrator/skills/platform-build-toolkit/, its tech-stack and convention-plugins references)',
    '',
    'Create the following in order:',
    '',
    '1. Generate the Gradle wrapper — a host Gradle 9.1.0+ must be on PATH: run `gradle wrapper --gradle-version 9.1.0` (AGP 9.0.1 requires Gradle >= 9.1.0 — AGP\'s own version check fails the build on anything older). This produces `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, and `gradlew.bat`. On an empty directory without a system Gradle install, `./gradlew` does not exist yet — this is the prerequisite for every later `./gradlew`. Install the required Gradle version temporarily if it is not already available; do not copy wrapper artifacts from another project. Confirm `gradle/wrapper/gradle-wrapper.properties` points at the intended distribution URL.',
    '2. `gradle/libs.versions.toml` — verbatim from the platform-build-toolkit skill (its tech-stack reference), with versions left unchanged. Adjust catalog keys if you renamed the product.',
    '3. `gradle.properties` — verbatim from the platform-build-toolkit skill (its tech-stack reference).',
    '4. `settings.gradle.kts` — root settings file. Initially include ONLY:',
    '   ```',
    '   include(":androidApp")',
    '   include(":shared")',
    '   ```',
    '   You will add more modules incrementally.',
    '5. `build-logic/` with the convention plugins from the platform-build-toolkit skill (its convention-plugins reference). Create:',
    '   - `build-logic/settings.gradle.kts`',
    '   - `build-logic/build.gradle.kts`',
    '   - `build-logic/convention/build.gradle.kts` with the `gradlePlugin { plugins { ... } }` registrations.',
    '   - `build-logic/convention/src/main/kotlin/<Name>ConventionPlugin.kt` — the seven core plugins, the opt-in general test foundation (`KmpTestConventionPlugin` + the `Coroutines/Flow/Network/Di/Room/ComposeUi` capability test plugins and `TestCapabilityEntryTask`, registered but applied only by test-bearing modules) and the optional, inert `ScreenshotTestConventionPlugin.kt` (applied by no module until the screenshot gate is enabled).',
    '   - `build-logic/convention/src/main/kotlin/com/<org>/{PluginManagerExtensions.kt, ConfigureJvmToolchain.kt, ProjectExtensions.kt}` (canonical helper names per the toolkit-modules reference).',
    '6. Root `build.gradle.kts` — verification aggregates only (`allHostTests`, `allIosSimulatorTests`, `allAndroidDeviceTests`, `allScreenshotTests`, `allConfiguredTests`, `testCapabilityInventory`), verbatim from the platform-build-toolkit version-catalog reference § "Root build.gradle.kts". It applies no plugins and configures no modules.',
    '7. Run `./gradlew --version` to verify the wrapper resolves. Expected: Gradle 9.1.0. The reported JVM is the host launcher\'s, not the project\'s `jvmToolchain(19)` target (provisioned per-module by the toolchain); the host JVM only needs to satisfy AGP\'s minimum (JDK 17+). If this fails with "no such file or directory", step 1 was not completed — the wrapper jar and launcher scripts are missing.',
    '8. Write the root `.gitignore` per `orchestrator/launch.md` § 2.5 (cover `build/`, `.gradle/`, `.idea/`, `local.properties`, `.DS_Store`, etc.).'
  ].join('\n');

  TPL.step3 = [
    'Step 3 — scaffold the foundation modules',
    '',
    'Read first:',
    '- orchestrator/launch.md (Step 3)',
    '- the platform-build-toolkit skill (orchestrator/skills/platform-build-toolkit/, its module-structure reference — Mandatory infrastructure modules — and gradle-build reference — representative builds)',
    '',
    'In one batch, create the infrastructure modules listed in the platform-build-toolkit skill (its module-structure reference) under "Mandatory infrastructure modules". For each:',
    '',
    '1. Create the directory.',
    '2. Create the `build.gradle.kts` (template from the platform-build-toolkit skill, its gradle-build reference — representative builds). Carve-out: `:design-system:resources:provider` and `:toolkit:notification-manager` must use their full build scripts with `androidLibrary { androidResources.enable = true }`, not the representative-builds template.',
    '3. Create an initial `src/commonMain/kotlin/com/<org>/<product>/<area>/<module>/.gitkeep` (empty package marker).',
    '4. Add `include(":<group>:<name>")` to `settings.gradle.kts`.',
    '',
    'Modules to scaffold (in this order, infrastructure first):',
    '',
    '- `:toolkit:context`',
    '- `:toolkit:logger`',
    '- `:toolkit:serialization`',
    '- `:toolkit:date-utils`',
    '- `:toolkit:http-client`',
    '- `:toolkit:theme`',
    '- `:toolkit:localization`',
    '- `:toolkit:connectivity`',
    '- `:toolkit:image-loader`',
    '- `:toolkit:link-opener`',
    '- `:toolkit:notification-manager`',
    '- `:toolkit:permission-manager`',
    '- `:ui-core:foundation`',
    '- `:ui-core:state`',
    '- `:ui-core:error:error-provider`',
    '- `:ui-core:error:error-provider-impl`',
    '- `:design-system:resources:provider`',
    '- `:design-system:resources:provider-impl`',
    '- `:design-system:core`',
    '- `:design-system:components`',
    '- `:design-system:preview`',
    '- `:data-services:datastore`',
    '- `:data-services:database`',
    '- `:data-services:backend`',
    '- `:data-services:firebase` (only when firebaseEnabled)',
    '- `:data-services:google-auth` (only when Google Sign-In enabled)',
    '- `:data-services:apple-auth` (only when Apple Sign-In enabled)',
    '- `:data-features:feature-api`',
    '- `:data-mappers:dto-to-entity`',
    '- `:data-mappers:entity-to-domain`',
    '- `:data-mappers:dto-to-domain`',
    '- `:data-mappers:domain-to-state`',
    '- `:data-mappers:state-to-domain`',
    '- `:data-mappers:domain-to-entity`',
    '- `:data-mappers:domain-to-dto`',
    '- `:ui-screen-features:screen-api`',
    '- `:ui-dialog-features:dialog-api`',
    '',
    'Verify after the full Step-3 batch: `./gradlew tasks` must succeed without errors. Run it only once all Step-3 `include(...)` lines are in `settings.gradle.kts`, because the type-safe `projects.*` accessors only exist after each sibling module is registered.'
  ].join('\n');

  TPL.step4 = [
    'Step 4 — implement the base classes',
    '',
    'Read first:',
    '- orchestrator/launch.md (Step 4)',
    '- the ui-feature skill (orchestrator/skills/ui-feature/, its base-classes reference)',
    '',
    'Implement `:ui-core:foundation` from the ui-feature skill (its base-classes reference):',
    '',
    '- `BaseViewModel`, `BaseComponent`, `BaseComposeScreen`, `BaseDirection`, `BaseLoader`, `BaseRouter`, `BaseResult`, `ResultKey`, `ResultKeys`, `ComponentIdentifier`, `NoneIdentifier`.',
    '- `OperationManager` (interface + impl).',
    '- `ResultManager` + `ResultEmitter`.',
    '- `collectAsStateMultiplatform` (expect/actual).',
    '- `platformAnimation` + `platformStackAnimator` (expect/actual).',
    '- `CoreModule` (`@Module @ComponentScan`).',
    '',
    'Use the verbatim code from the ui-feature skill (its base-classes reference) as the starting point.',
    '',
    'Verify is deferred: `:ui-core:foundation:assemble` only passes after the error-provider/state dependencies exist, and on Firebase-enabled projects after `FirebaseProvider` lands in Step 7. For Step 4, create the foundation files from the ui-feature base-classes reference and continue to Step 4.5.'
  ].join('\n');

  TPL.step5 = [
    'Step 5 — implement the toolkit',
    '',
    'Read first:',
    '- orchestrator/launch.md (Step 5)',
    '- the platform-build-toolkit skill (orchestrator/skills/platform-build-toolkit/, its toolkit references)',
    '',
    'Implement each `:toolkit:*` module from the platform-build-toolkit skill (its toolkit references):',
    '',
    '- `:toolkit:context` — `NativeContext` expect/actual, `ContextModule`.',
    '- `:toolkit:logger` — `AppLogger`.',
    '- `:toolkit:serialization` — `Json` provider via `SerializationModule`.',
    '- `:toolkit:date-utils` — `DateTimeUtils`, `DateRange`, `DateRangeKind`, `DateRangePresets`, `DateFormat`, `DateFormatting`.',
    '- `:toolkit:http-client` — base `HttpClient` + `ApiErrorParser`.',
    '- `:toolkit:theme`, `:toolkit:localization` — `AppTheme.current`, `AppLocale.current` expect/actual.',
    '- `:toolkit:connectivity` — `Connectivity` interface + Android/iOS impls.',
    '- `:toolkit:notification-manager`, `:toolkit:permission-manager`, `:toolkit:link-opener` — minimal stub impls. Reminder: `:toolkit:notification-manager` ships androidMain `res/`, so its `build.gradle.kts` must be the full build script with `androidLibrary { androidResources.enable = true }`, not the representative-builds template.',
    '- `:toolkit:image-loader` — Coil 3 + Ktor 3.',
    '',
    'Verify after each: the module builds and the Koin module is annotated.'
  ].join('\n');

  TPL.step6 = [
    'Step 6 — implement the design system',
    '',
    'Read first:',
    '- orchestrator/launch.md (Step 6)',
    '- the design-system skill (orchestrator/skills/design-system/, its references)',
    '',
    'Implement `:design-system:resources:provider`:',
    '',
    'Reminder: this module ships `composeResources/`, so its `build.gradle.kts` must be the full build script with `androidLibrary { androidResources.enable = true }`, not the representative-builds template.',
    '',
    '- `AppColor`, `AppDp`, `AppTypography`, `AppString`, `AppDrawable`, `AppIcon` interfaces/objects.',
    '- `LocalAppColors`, `LocalAppDp`, ... CompositionLocals.',
    '- `StringProvider` interface.',
    '- Initial `strings.xml` in `commonMain/composeResources/values/strings.xml` with one key (e.g. `app_name`).',
    '- For every locale in `supportedLocales` beyond `en`, also create `commonMain/composeResources/values-<lang>/strings.xml` with the same key (resource-builder later requires every `supportedLocales` entry to receive each key — see orchestrator/project-config.md).',
    '- Initial drawable, e.g. `ic_back.xml`.',
    '',
    'Implement `:design-system:resources:provider-impl`:',
    '',
    '- `StringProviderImpl` + `ResourcesProviderModule`.',
    '',
    'Implement `:design-system:core`:',
    '',
    '- `AppTokens`.',
    '- `AppTheme` + `ProvideResources`.',
    '- `LightAppColors` + `DarkAppColors` with a minimal but complete set of color slots.',
    '',
    'Implement `:design-system:components` (minimal):',
    '',
    '- `Toolbar`, `BottomSheetToolbar`, `Button`, `BaseComposeScreen`-style widgets.',
    '',
    'Implement `:design-system:preview`:',
    '',
    '- `@AppPreview` multi-preview annotation.',
    '- `PreviewContainer` composable.',
    '- `PreviewContainerScreenshot` — a full-bleed sibling used by the screenshot-fidelity gate; parameterise its `darkTheme` to match the project theme axis.',
    '',
    'Keep the standard module roots and the `AppColor`, `AppDp`, `AppTokens`, `LightAppColors`, and `DarkAppColors` filenames above. When Figma is enabled, the first Sync derives `orchestrator/figma/project-adapters.json` from this layout automatically and then runs the matching local comparison. Do not ask the user to create that file.',
    '',
    'The typeface factory function for this project is `<typeface>` — wire it through `resource-builder` conventions when fonts are added.'
  ].join('\n');

  TPL.step7 = [
    'Step 7 — implement the data layer',
    '',
    'Read first:',
    '- orchestrator/launch.md (Step 7)',
    '- the data-layer skill (orchestrator/skills/data-layer/, its references)',
    '',
    '`:data-services:datastore`:',
    '',
    '- `DataStore<Preferences>` provider with `expect/actual` paths.',
    '',
    '`:data-services:database`:',
    '',
    '- `Database` class with `@Database(version = 1)` and a minimal entity (e.g. `TokenEntity` + `UserActiveEntity` for the auth subsystem).',
    '- `DatabaseBuilder` expect/actual.',
    '- `DatabaseConstructor` expect/actual (Room generates).',
    '- `DatabaseModule` with DAO providers.',
    '',
    '`:data-services:backend`:',
    '',
    '- `BackendClient`, `TokenProvider`, `ClientLogger`, `<Product>Api`.',
    '- `BackendClient.defaultRequest.host = "<product-domain>"`.',
    '- Minimal DTOs: `TokenResponse`, `RefreshBody`, plus the first product\'s response (e.g. `<Entity>Response`).',
    '- `BackendModule`.',
    '',
    '`:data-services:firebase` (only when firebaseEnabled):',
    '',
    '- `FirebaseProvider` interface with `Analytics`, `Crashlytics`, `Messaging` sub-interfaces.',
    '- Android implementation (uses Firebase BOM libs).',
    '- iOS empty stub.'
  ].join('\n');

  TPL.step4_5 = [
    'Step 4.5 — implement the error contracts',
    '',
    'Read first:',
    '- orchestrator/launch.md (Step 4.5)',
    '- the ui-feature skill (orchestrator/skills/ui-feature/, its module-structure reference for `:ui-core:error:error-provider` and `:ui-core:state`, and its error-pipeline reference)',
    '',
    'Step 3 scaffolded `:ui-core:error:error-provider` and `:ui-core:state` as `.gitkeep`. Seed the error types in both now — `:ui-core:foundation` (Step 4), `:toolkit:http-client` (Step 5), and `:ui-dialog-features:dialog-api` (Step 7.5) all depend on them.',
    '',
    '### :ui-core:error:error-provider — AppError + ErrorProvider only',
    '',
    'Inside `src/commonMain/kotlin/com/<org>/<product>/core/error/provider/`:',
    '',
    '- `AppError.kt` — `public sealed class AppError(message, cause) : Exception(message, cause)` with `Network.{NoInternet, Timeout, Expected, Unexpected}`, top-level `Expected(message, description)`, `Unknown : AppError(null)`. Copy verbatim from the ui-feature skill\'s error-pipeline reference (the AppError hierarchy).',
    '- `ErrorProvider.kt` — `public interface ErrorProvider { public suspend fun provide(exception: Throwable, callback: () -> Unit) }`. Interface only — impl ships in `:ui-core:error:error-provider-impl`, set up in Step 7.6.',
    '',
    'Module is deps-free — no `sourceSets.commonMain.dependencies { ... }` block. AppErrorState is intentionally NOT here (it has UI concerns) — it lives in :ui-core:state.',
    '',
    '### :ui-core:state — seed AppErrorState only',
    '',
    '`:ui-core:state` is a large module (UiText, *FormatState, product-specific state); the bootstrap only needs AppErrorState before later steps compile.',
    '',
    'Inside `src/commonMain/kotlin/com/<org>/<product>/core/state/error/`:',
    '',
    '- `AppErrorState.kt` — `public sealed class AppErrorState` mirroring AppError: `Network.{NoInternet, Timeout, Expected, Unexpected}`, top-level `Expected`, `Unknown` (singleton). Constructor params are NOT uniform — they mirror exactly what ErrorProviderImpl passes (see the ui-feature skill\'s error-pipeline reference, the ErrorProviderImpl mapping): `Network.NoInternet(description: String?)`, `Network.Timeout(description: String?)`, and `Network.Unexpected(description: String?)` take ONLY a description (their title is a fixed string surfaced later via the `.title()` accessor in Step 7.7); `Network.Expected(title: String, description: String?)` and top-level `Expected(title: String, description: String?)` take both; `Unknown` is a parameterless singleton. Plain strings, no UiText wrapping in the bootstrap shape.',
    '',
    '`:ui-core:state/build.gradle.kts` template in the ui-feature skill\'s module-structure reference applies as-is.',
    '',
    'Verify: `./gradlew :ui-core:error:error-provider:assemble :ui-core:state:assemble` builds. Re-run `./gradlew :ui-core:foundation:assemble` to confirm Step 4 succeeds (BaseViewModel imports `ErrorProvider`) unless `firebaseEnabled: true`; in that case the full foundation assemble still waits for `FirebaseProvider` in Step 7.'
  ].join('\n');

  TPL.step7_5 = [
    'Step 7.5 — implement the :ui-dialog-features:dialog-api contract module',
    '',
    'Read first:',
    '- orchestrator/launch.md (Step 7.5)',
    '- the ui-feature skill (orchestrator/skills/ui-feature/, its dialogs reference, and its module-structure reference for `:ui-dialog-features:dialog-api`)',
    '',
    'Step 3 scaffolded the module as an empty `.gitkeep`. Fill in its five files so `:shared`\'s DialogComponent (Step 9) and the error pipeline have working contracts.',
    '',
    'Inside `src/commonMain/kotlin/com/<org>/<product>/dialog/api/`:',
    '',
    '- `Constants.kt` — `public val DIALOG_EXIT_ANIMATION_DURATION: Duration = 300.milliseconds`.',
    '- `DialogConfig.kt` — `@Serializable public sealed class DialogConfig(@Transient public open val onDismiss: (() -> Unit)? = null, public open val dismissBySwipe: Boolean = true) { public abstract val key: String; protected fun buildKey(vararg parts: Any?): String /* length-prefixed join */ }`. Seed exactly one subtype — `ErrorDisplay(error: AppErrorState, @Transient onClose: () -> Unit = {})` with `onDismiss = onClose`, `dismissBySwipe = true`, `key = buildKey("ErrorDisplay", error)`. `AppErrorState` comes from `:ui-core:state` (package `core.state.error`, seeded in Step 4.5); the dialog-api `build.gradle.kts` template in the ui-feature skill\'s module-structure reference already includes `implementation(projects.uiCore.state)`. Further subtypes are added per the ui-feature skill\'s dialogs reference (the add-dialog recipe).',
    '- `DialogController.kt` — two interfaces: `interface DialogController { fun show(config: DialogConfig) }` and `interface DialogProvider { val dialog: Flow<DialogConfig> }`. No `dismiss()` method — closing is the host\'s job.',
    '- `DialogModule.kt` — `@Module @ComponentScan public class DialogModule`. Lives in this module (not in :shared).',
    '- `internal/DialogControllerImpl.kt` — `@Single(binds = [DialogController::class, DialogProvider::class]) internal class DialogControllerImpl : DialogController, DialogProvider`. Buffered `Channel<DialogConfig>` relays show() into the consumer Flow.',
    '',
    'Verify: `./gradlew :ui-dialog-features:dialog-api:assemble` builds green.'
  ].join('\n');

  TPL.step7_6 = [
    'Step 7.6 — implement :ui-core:error:error-provider-impl',
    '',
    'Read first:',
    '- orchestrator/launch.md (Step 7.6)',
    '- the ui-feature skill (orchestrator/skills/ui-feature/, its module-structure reference for `:ui-core:error:error-provider-impl`, and its error-pipeline reference)',
    '',
    'Step 3 scaffolded the module as an empty `.gitkeep`. Fill it in now — without this module, `BaseViewModel.safeLaunch { ... }`\'s `inject<ErrorProvider>()` resolves to nothing and the first failing coroutine crashes with a Koin `NoDefinitionFoundException` at runtime. The module closes the loop between the error contract (`:ui-core:error:error-provider`, Step 4.5) and the dialog pipeline (`:ui-dialog-features:dialog-api`, Step 7.5): `ErrorProviderImpl` maps `AppError` → `AppErrorState` and calls `DialogController.show(DialogConfig.ErrorDisplay(...))`.',
    '',
    '### build.gradle.kts',
    '',
    '```kotlin',
    'plugins {',
    '    id("android.library.convention")',
    '    id("kotlin.multiplatform.convention")',
    '    id("koin.annotation.convention")',
    '}',
    '',
    'kotlin {',
    '    android {',
    '        namespace = "com.<org>.<product>.ui.core.error.error.provider.impl"',
    '    }',
    '',
    '    sourceSets.commonMain.dependencies {',
    '        implementation(projects.uiDialogFeatures.dialogApi)',
    '        implementation(projects.uiCore.error.errorProvider)',
    '        implementation(projects.uiCore.state)',
    '    }',
    '}',
    '```',
    '',
    '`projects.uiDialogFeatures.dialogApi` pulls in `DialogController` + `DialogConfig.ErrorDisplay` (the route this module dispatches to); `projects.uiCore.error.errorProvider` pulls in the `ErrorProvider` interface + `AppError` hierarchy this module implements; `projects.uiCore.state` provides the `AppErrorState` subtypes the mapper constructs. The same three deps are listed in the ui-feature skill\'s module-structure reference § `:ui-core:error:error-provider-impl`.',
    '',
    'Inside `src/commonMain/kotlin/com/<org>/<product>/core/error/provider/impl/`:',
    '',
    '- `ErrorModule.kt` — `@Module(includes = [DialogModule::class]) @ComponentScan public class ErrorModule`. Listed in `:shared/Koin.kt` (Step 9) so the `@Single` on `ErrorProviderImpl` is discovered; the `includes = [DialogModule::class]` saves `:shared/Koin.kt` from declaring both modules explicitly — depending on `ErrorModule` automatically pulls `DialogModule`.',
    '- `ErrorProviderImpl.kt` — `@Single(binds = [ErrorProvider::class]) internal class ErrorProviderImpl(val dialogController: DialogController) : ErrorProvider`. Implements `override suspend fun provide(exception: Throwable, callback: () -> Unit)` as an exhaustive `when` over `AppError` subtypes that produces an `AppErrorState`, then calls `dialogController.show(DialogConfig.ErrorDisplay(error = state, onClose = callback))`. Internal — only Koin instantiates it. Branches mirror the ui-feature skill\'s error-pipeline reference (the ErrorProviderImpl when-mapping) verbatim:',
    '',
    '  ```kotlin',
    '  val error = when (exception) {',
    '',
    '      is AppError.Network.NoInternet -> AppErrorState.Network.NoInternet(',
    '          description = exception.message,',
    '      )',
    '',
    '      is AppError.Network.Timeout -> AppErrorState.Network.Timeout(',
    '          description = exception.message,',
    '      )',
    '',
    '      is AppError.Network.Expected -> AppErrorState.Network.Expected(',
    '          title = exception.title,',
    '          description = exception.description,',
    '      )',
    '',
    '      is AppError.Network.Unexpected -> AppErrorState.Network.Unexpected(',
    '          description = exception.message,',
    '      )',
    '',
    '      is AppError.Expected -> AppErrorState.Expected(',
    '          title = exception.message,',
    '          description = exception.description,',
    '      )',
    '',
    '      is AppError.Unknown -> AppErrorState.Unknown',
    '',
    '      else -> AppErrorState.Unknown',
    '  }',
    '  ```',
    '',
    '  The trailing `else -> AppErrorState.Unknown` is intentional — `BackendClient.HttpResponseValidator` may surface a `Throwable` outside the `AppError` hierarchy (a bug, an unmapped library exception); the pipeline still produces a visible dialog rather than swallowing it.',
    '',
    'Verify: `./gradlew :ui-core:error:error-provider-impl:assemble` builds green.'
  ].join('\n');

  TPL.step7_7 = [
    'Step 7.7 — implement :ui-dialog-features:error-display',
    '',
    'Read first:',
    '- orchestrator/launch.md (Step 7.7)',
    '- the ui-feature skill (orchestrator/skills/ui-feature/, its dialogs reference — the add-dialog recipe — and its error-pipeline reference)',
    '',
    'The render target of the error pipeline — NOT an optional feature dialog. ErrorProviderImpl (Step 7.6) maps every Throwable to DialogConfig.ErrorDisplay and calls dialogController.show(...); :shared/DialogContentComponent.createChild (Step 9) builds this Component for it. Without this module, Step 9 is forced to a TODO(...) stub and the FIRST runtime error of any kind crashes the app with NotImplementedError. Scaffold it here — after dialog-api (Step 7.5), before :shared (Step 9).',
    '',
    '1. settings.gradle.kts — `include(":ui-dialog-features:error-display")`.',
    '2. build.gradle.kts — the add-dialog plugin set + deps from the ui-feature skill\'s dialogs reference (android.library + kotlin.multiplatform + compose.multiplatform + koin.annotation conventions; deps projects.uiCore.{foundation,state}, projects.designSystem.{core,resources.provider,preview,components}, compose.foundation, compose.material3, libs.immutable.collections), namespace `com.<org>.<product>.ui.dialog.features.error.display`.',
    '3. Seven MVI files under `com/<org>/<product>/error/display/` (per the ui-feature skill\'s dialogs reference, the add-dialog recipe): `ErrorDisplayState(error: AppErrorState)`; `ErrorDisplayDirection { Back }`; empty Loader; `ErrorDisplayContract { onDismiss(); companion Empty }`; `ErrorDisplayViewModel(error)` with `onDismiss() = navigateTo(Back)`; public `ErrorDisplayComponent(componentContext, error: AppErrorState, back: () -> Unit)`; `ErrorDisplayScreen` = the add-dialog dialog shell rendering `state.error.title()` + optional `state.error.description()` + a Primary dismiss Button (`Res.string.error_dismiss`).',
    '4. Plain-String accessors in :ui-core:state — add `AppErrorStateExt.kt` next to AppErrorState (core.state.error) with `public fun AppErrorState.title(): String` and `public fun AppErrorState.description(): String?` (a when over the subtypes; fallback titles for the NoInternet/Timeout/Unexpected/Unknown cases that carry no title). Bootstrap shape is plain strings; swap to UiText + Res.string later (the design-system skill\'s cookbook-resource reference).',
    '5. Add `<string name="error_dismiss">Got it</string>` to :design-system:resources:provider for every supportedLocale.',
    '',
    'Verify: `./gradlew :ui-core:state:assemble :ui-dialog-features:error-display:assemble` builds green. Step 9 then wires ErrorDisplayComponent into DialogContentComponent.createChild.'
  ].join('\n');

  TPL.step8 = [
    'Step 8 — implement the first feature',
    '',
    'Read first:',
    '- orchestrator/launch.md (Step 8)',
    '- the data-layer skill (orchestrator/skills/data-layer/, its add-data-feature recipe reference)',
    '- the ui-feature skill (orchestrator/skills/ui-feature/, its add-screen reference)',
    '',
    'Pick `<firstDomain>` as the first product domain. Build the simplest end-to-end slice (e.g. a list screen for that domain). Below, `<X>` stands for `<firstDomain>` and `<x>` for its lowercase form.',
    '',
    '1. Domain types in `:data-features:feature-api`:',
    '   - `<X>Feature` interface.',
    '   - `<X>` domain model.',
    '2. Implementation module `:data-features:<x>` (add to `settings.gradle.kts`):',
    '   - `<X>Repository` interface.',
    '   - `<X>RepositoryImpl`.',
    '   - `<X>FeatureImpl`.',
    '   - `<X>FeatureModule`.',
    '3. Mappers in `:data-mappers:*`:',
    '   - `<X>Response.toEntityOrNull()`, `List<>.toEntities()`.',
    '   - `<X>Entity.toDomain()`, `List<>.toDomain()`.',
    '   - `<X>.toState()`, `List<>.toState()`.',
    '4. Screen feature `:ui-screen-features:<x>` (add to `settings.gradle.kts`):',
    '   - `RootRouter` sealed class in `:screen-api` (top-level `BaseRouter` config; see the ui-feature skill\'s module-structure reference (the shared composition root) and its navigation reference for shape) — one entry per top-level feature, initially `<X>`.',
    '   - `<X>Router` in `:screen-api`.',
    '   - Seven MVI files for the list screen.',
    '   - `<X>RootComponent`.'
  ].join('\n');

  TPL.step9 = [
    'Step 9 — implement :shared',
    '',
    'Read first:',
    '- orchestrator/launch.md (Step 9)',
    '- the ui-feature skill (orchestrator/skills/ui-feature/, its module-structure reference — shared composition root + app shells — and its navigation and dialogs references)',
    '- the di-modules skill (orchestrator/skills/di-modules/, its koin-overview and composition-root references)',
    '',
    '`:shared` is the composition root. Create:',
    '',
    '- `Koin.kt` listing every `<X>Module` so far — including `DialogModule().module` (ships in :ui-dialog-features:dialog-api, set up in Step 7.5) and `ErrorModule().module` (ships in :ui-core:error:error-provider-impl, set up in Step 7.6). Neither is re-declared in :shared.',
    '- Under `root/`: `RootComponent`, `RootViewModel`, `RootScreen`, `RootDirection`, `RootContract`, `RootState`, `RootLoader`.',
    '- Under `dialog/` — seven files matching the MVI shape:',
    '  - `DialogComponent.kt` — owns `SlotNavigation<DialogConfig>`; reconciles `sessionConfig`/`innerConfigs` against `DialogViewModel.state`.',
    '  - `DialogContract.kt` — `onClose() / onDismiss(pendingResult) / onRelease(config)` + companion Empty.',
    '  - `DialogState.kt` — `stack: ImmutableList<DialogEntry>`, `phase: SheetPhase { Present, Dismissing, Released }`, `pending: DialogConfig?`.',
    '  - `DialogDirection.kt` — empty sealed interface (state-driven, no directions emitted).',
    '  - `DialogLoader.kt` — empty sealed interface.',
    '  - `DialogViewModel.kt` — consumes `DialogProvider.dialog`, deduplicates by `(class, key)`, fires pending callbacks before clearing stack so chained show() lands as `pending`.',
    '  - `DialogScreen.kt` — `ModalBottomSheet` with `skipPartiallyExpanded`; `dismissBySwipe` read via `rememberUpdatedState`; `BottomSheetToolbar` (back when stack.size > 1, close otherwise); coordinates `sheetState.hide()`/`show()` with `SheetPhase`.',
    '- Under `dialog/content/` — another seven files:',
    '  - `DialogContentComponent.kt` — owns inner `StackNavigation<DialogConfig>`; `createChild(router, ctx)` is exhaustive `when` over DialogConfig subtypes. Also declares `internal sealed class Child(open val component: BaseComponent<*>)` with one data class per subtype. With only `ErrorDisplay` seeded (built in Step 7.7), wire `is DialogConfig.ErrorDisplay -> Child.ErrorDisplay(ErrorDisplayComponent(componentContext = ctx, error = router.error, back = { viewModel.onBack(null) }))` and `data class ErrorDisplay(override val component: ErrorDisplayComponent) : Child(component)`. Add `implementation(projects.uiDialogFeatures.errorDisplay)` to :shared. Do NOT stub this branch with `TODO(...)` — ErrorDisplay is the render target of the error pipeline, so a stub turns the first runtime error into a NotImplementedError crash.',
    '  - `DialogContentContract.kt` — `onBack(pendingResult) ` + companion Empty.',
    '  - `DialogContentState.kt` — `data object DialogContentState`.',
    '  - `DialogContentDirection.kt` — `sealed interface ... { data class Back(val pendingResult: (() -> Unit)? = null) }`.',
    '  - `DialogContentLoader.kt` — empty sealed interface.',
    '  - `DialogContentViewModel.kt` — translates `onBack(pendingResult)` to `navigateTo(Direction.Back(pendingResult))`.',
    '  - `DialogContentScreen.kt` — `AnimatedContent` keyed on `child.keyHashString()`; `rememberSaveableStateHolder()` with cleanup of obsolete keys.',
    '- `Deeplink` enum (empty initially or with one entry) — lives in `:ui-screen-features:screen-api`, not `:shared`.',
    '- iOS bridge `RootViewController.kt` in `iosMain` — `public fun rootViewController(root: RootComponent, backDispatcher: BackDispatcher): UIViewController` (wraps `root.Render()` in `ComposeUIViewController` + `PredictiveBackGestureOverlay`); Swift calls it as `RootViewControllerKt.rootViewController(root:backDispatcher:)`.',
    '',
    '`RootComponent` instantiates `DialogComponent` once and renders it as a sibling of `RootScreen` inside the `AppTheme { ... }` block.',
    '',
    'Verify: `./gradlew :shared:assemble<IosFrameworkName>DebugXCFramework` (if `iosEnabled: false`, run `./gradlew :shared:assemble` instead). The module path is always `:shared:`; only the `<IosFrameworkName>` task-name suffix tracks the framework name.'
  ].join('\n');

  TPL.step13 = [
    'Step 13 — write CLAUDE.md',
    '',
    'Read first:',
    '- orchestrator/launch.md (Step 13)',
    '- the validation-gates skill (orchestrator/skills/validation-gates/, its when-to-stop-and-ask reference)',
    '',
    'Precondition. Check the project root for an existing `CLAUDE.md`. If one is present, never delete or rewrite it and do not ask about it: keep the existing content verbatim and append only the missing sections below under a clearly-marked `## Orchestrator workflow` heading, then note the pre-existing file in the step report. There must be one coherent instruction file at this scope.',
    '',
    '```bash',
    '# Inspect first — never rm an existing CLAUDE.md; append missing sections instead:',
    '[ -f CLAUDE.md ] && head -40 CLAUDE.md',
    '```',
    '',
    'Do not interview the user for content. Derive the product-purpose paragraph from the Setup answers in `orchestrator/project-config.md` (product name + first product domain); write "(filled in later)" for anything they do not cover (positioning, cross-repo rules) — do not invent content.',
    '',
    'At the project root, create `CLAUDE.md` describing:',
    '',
    '- The product purpose (one paragraph — derived from the Setup answers).',
    '- Cross-repo rules ("(filled in later)" unless the Setup answers cover them).',
    '- Communication / decision-making conventions.',
    '- Scope discipline ("don\'t refactor without an explicit request", etc.).',
    '- When to stop and ask (mirror the validation-gates skill\'s when-to-stop-and-ask reference).',
    '- Task workflow pointer: "Tasks flow through the four-column kanban under `orchestrator/tasks/`: `backlog -> task-prep -> pending -> task-prep -> todo -> orchestrator -> done`. Drop free-text ideas in `backlog/`, run `task-prep`; if questions land in `pending/`, answer them and run `task-prep` again; then ask Claude to run the task once it is in `todo/`. The orchestrator moves the file to `done/` on success. See `orchestrator/tasks/README.md`."',
    '- If `figmaEnabled: true`, include a Figma implementation block: run the Figma helper scripts exactly as documented under `orchestrator/figma/`, keep generated artifacts in their documented locations, and do not hand-edit generated token or screen outputs.',
    '- If `backendContractEnabled` is not `false`, include a backend contract block: use `orchestrator/api-contract/` as the source of truth, keep runtime cache outputs under `orchestrator/.cache/api-contract/`, and resolve doctor/verify failures before declaring API work complete.'
  ].join('\n');

  TPL.step14 = [
    'Step 14 — install skills',
    '',
    'Read first:',
    '- orchestrator/launch.md (Step 14)',
    '- orchestrator/README.md (section "Skills — install before first use")',
    '- orchestrator/skills/README.md',
    '',
    'The skills were already installed into `.claude/skills/` during Setup. With the foundation green and the first end-to-end "hello world" feature in place, this step confirms the skills plus the task-board folders + INDEX.json are on disk so the orchestrator is callable — and wires the two enforcement layers (pre-commit hooksPath + the figma-gate CI workflow) that keep `done/` honest.',
    '',
    'If `.claude/skills/` is somehow missing, re-run the install. Use `--symlink` to keep edits in `orchestrator/skills/` live in `.claude/skills/`; omit it for a copy (snapshot, no propagation):',
    '',
    '```bash',
    'bash orchestrator/skills/install-skills.sh . --symlink',
    '```',
    '',
    'Alternative (copy — snapshot, no propagation):',
    '',
    '```bash',
    'bash orchestrator/skills/install-skills.sh .',
    '```',
    '',
    'Verify:',
    '',
    '```bash',
    'ls .claude/skills/   # expect the 11 skill directories, each with SKILL.md + references/',
    '# Stamp the vendored-copy integrity manifest (the drift sensor\'s baseline —',
    '# orchestrator/template-sync/README.md; stamped only here and by an explicit sync --apply).',
    'python3 orchestrator/template-sync/_generate_template_manifest.py',
    '# Seed the derived architecture map before linting — lint.sh\'s arch-map check requires',
    '# orchestrator/.arch-map.json once settings.gradle.kts exists (true post-bootstrap).',
    'python3 orchestrator/tasks/regen-arch.py',
    'bash orchestrator/lint.sh',
    '```',
    '',
    'Make sure the task-board folders exist (they ship empty in `orchestrator/tasks/` and need to be on disk before `task-prep` or the orchestrator runs):',
    '',
    '```bash',
    'mkdir -p orchestrator/tasks/{backlog,pending,todo,done}',
    '[ -f orchestrator/tasks/INDEX.json ] || cat > orchestrator/tasks/INDEX.json <<\'EOF\'',
    '{',
    '  "version": 2,',
    '  "generatedAt": "1970-01-01T00:00:00.000Z",',
    '  "backlog": [],',
    '  "pending": [],',
    '  "todo": [],',
    '  "done": []',
    '}',
    'EOF',
    '```',
    '',
    'Enforcement wiring (REQUIRED — the checklist above is incomplete without it):',
    '',
    '1. Wire the local screenshot-gate net. The tracked pre-commit hook (`orchestrator/skills/checks/hooks/pre-commit`) runs `node orchestrator/figma/scripts/verify-done.mjs` and blocks any commit that leaves a red/uncertified `done/` UI task — but git never executes it until you point git at the tracked hooks dir:',
    '',
    '```bash',
    'git config core.hooksPath orchestrator/skills/checks/hooks',
    '```',
    '',
    'Run it once, now (`install-skills.sh` already runs it automatically when the repo is git-initialized — so this doubles as verification). Verify with a throwaway commit that leaves a red `done/` UI task: it must be rejected. (The deliberate emergency bypass is `git commit --no-verify`; the hook is a no-op when `figmaEnabled: false` — 0 audited.) Until wired, the hook file exists but is inert, and a CI-less local flow ships uncompared UI tasks.',
    '',
    'Confirm `orchestrator/project-config.md` was populated in Step 1.5. From here on, tasks flow `backlog -> task-prep -> pending -> task-prep -> todo -> orchestrator -> done`: drop free-text task ideas in `orchestrator/tasks/backlog/` from the Board panel, run `task-prep`, answer any pending questions, run `task-prep` again, then run the task from `todo/`.'
  ].join('\n');

  // ----------------------------------------------------------------------
  // The 17 step entries (Steps 2..14, including 4.5, 7.5, 7.6, 7.7). Each entry exposes
  // EITHER `promptTemplate` OR `build(setup)`; the wizard panel chooses
  // `build` over `promptTemplate` when both are defined.
  // ----------------------------------------------------------------------

  // Wizard starts at Step 2. Steps 0 (confirm context) and 1.5 (populate
  // orchestrator/project-config.md) are absorbed into the Setup panel's "Copy as
  // Claude prompt" CTA — that single prompt verifies orchestrator/, writes
  // orchestrator/project-config.md and installs the skills. Step 1's up-front read of
  // the full normative reference set is NOT folded into the CTA; it is replaced by
  // the per-step "Read first:" pointers each prompt carries (lazy loading).
  // Numbering still follows launch.md so cross-references (Step N -> file
  // paths inside prompts) stay valid. Steps 6.5 (Figma) and 6.6 (Backend
  // contract) intentionally have no wizard card — they run via the Figma and
  // Backend panels.
  export const wizardSteps = [
    {
      id: '2',
      title: 'Step 2 — initialize the build system',
      hook: 'Without convention plugins green, every later module fails the same way.',
      promptTemplate: TPL.step2,
      verifyHint: './gradlew --version  # expect the pinned Gradle version (host JVM is the launcher\'s, not the jvmToolchain(19) target)',
      skipWhen: null
    },
    {
      id: '3',
      title: 'Step 3 — scaffold the foundation modules',
      hook: 'These modules carry zero business logic but every later one depends on them.',
      promptTemplate: TPL.step3,
      verifyHint: './gradlew tasks',
      skipWhen: null
    },
    {
      id: '4',
      title: 'Step 4 — implement the base classes',
      hook: 'BaseViewModel/BaseComponent are the spine every screen and dialog hangs off.',
      promptTemplate: TPL.step4,
      verifyHint: './gradlew :ui-core:foundation:assemble  # defer until Step 4.5 lands ErrorProvider',
      skipWhen: null
    },
    {
      id: '4.5',
      title: 'Step 4.5 — implement the error contracts',
      hook: 'AppError + ErrorProvider in :ui-core:error:error-provider, plus AppErrorState seeded in :ui-core:state — the types the foundation, http-client, and dialog-api all reference.',
      promptTemplate: TPL.step4_5,
      verifyHint: './gradlew :ui-core:error:error-provider:assemble :ui-core:state:assemble :ui-core:foundation:assemble',
      skipWhen: null
    },
    {
      id: '5',
      title: 'Step 5 — implement the toolkit',
      hook: 'Small, platform-aware utilities the rest of the stack assumes are already there.',
      promptTemplate: TPL.step5,
      verifyHint: './gradlew :toolkit:logger:assemble :toolkit:http-client:assemble :toolkit:date-utils:assemble',
      skipWhen: null
    },
    {
      id: '6',
      title: 'Step 6 — implement the design system',
      hook: 'AppTokens land before any screen, so screens never reference Material3 directly.',
      promptTemplate: TPL.step6,
      verifyHint: './gradlew :design-system:resources:provider:assemble :design-system:core:assemble :design-system:components:assemble',
      skipWhen: null
    },
    {
      id: '7',
      title: 'Step 7 — implement the data layer',
      hook: 'Backend, Database, DataStore — the wiring features will plug into in Step 8.',
      promptTemplate: TPL.step7,
      verifyHint: './gradlew :data-services:database:assemble :data-services:backend:assemble',
      skipWhen: null
    },
    {
      id: '7.5',
      title: 'Step 7.5 — implement the :ui-dialog-features:dialog-api contract module',
      hook: 'Dialog contracts the error pipeline and :shared/DialogComponent depend on.',
      promptTemplate: TPL.step7_5,
      verifyHint: './gradlew :ui-dialog-features:dialog-api:assemble',
      skipWhen: null
    },
    {
      id: '7.6',
      title: 'Step 7.6 — implement :ui-core:error:error-provider-impl',
      hook: 'ErrorProviderImpl closes the AppError → dialog pipeline; without it BaseViewModel.inject<ErrorProvider>() crashes on first failure and Koin\'s ErrorModule is never registered in :shared/Koin.kt.',
      promptTemplate: TPL.step7_6,
      verifyHint: './gradlew :ui-core:error:error-provider-impl:assemble',
      skipWhen: null
    },
    {
      id: '7.7',
      title: 'Step 7.7 — implement :ui-dialog-features:error-display',
      hook: 'The error pipeline\'s render target. Without it, :shared/DialogContentComponent.createChild is a TODO stub and the first runtime error of any kind crashes with NotImplementedError instead of an error sheet.',
      promptTemplate: TPL.step7_7,
      verifyHint: './gradlew :ui-core:state:assemble :ui-dialog-features:error-display:assemble',
      skipWhen: null
    },
    {
      id: '8',
      title: 'Step 8 — implement the first feature',
      hook: 'One end-to-end feature proves the wiring before you build ten.',
      promptTemplate: TPL.step8,
      verifyHint: './gradlew :data-features:<x>:assemble :ui-screen-features:<x>:assemble',
      skipWhen: null
    },
    {
      id: '9',
      title: 'Step 9 — implement :shared',
      hook: 'Composition root — Koin.init lives here, every Module is enumerated explicitly.',
      promptTemplate: TPL.step9,
      verifyHint: './gradlew :shared:assemble<IosFrameworkName>DebugXCFramework  # if iosEnabled: false, run ./gradlew :shared:assemble instead',
      skipWhen: null
    },
    {
      id: '10',
      title: 'Step 10 — implement :androidApp',
      hook: 'Application + MainActivity wire Koin and Decompose into the OS.',
      build: buildStep10,
      verifyHint: './gradlew :androidApp:assembleDebug',
      skipWhen: null
    },
    {
      id: '11',
      title: 'Step 11 — implement :iosApp',
      hook: 'No human handoff — the :iosApp files ship as verbatim drop-in fences; the framework rebuilds on every Xcode build (no drag-and-drop).',
      build: buildStep11,
      verifyHint: './gradlew :shared:assemble<IosFrameworkName>DebugXCFramework  # then ./iosApp/run-ios.sh',
      skipWhen: function (setup) { return setup.iosEnabled === false; },
      skipReasonKey: 'wizard.skipReason.ios'
    },
    {
      id: '12',
      title: 'Step 12 — verify end-to-end',
      hook: 'Both platforms (or just Android when iOS is off) must build green before declaring foundation complete.',
      build: buildStep12,
      verifyHint: './gradlew :androidApp:assembleDebug',
      skipWhen: null
    },
    {
      id: '13',
      title: 'Step 13 — write CLAUDE.md',
      hook: 'Project-level guidance for every future agent/contributor session.',
      promptTemplate: TPL.step13,
      verifyHint: 'ls -la CLAUDE.md',
      skipWhen: null
    },
    {
      id: '14',
      title: 'Step 14 — install skills',
      hook: 'Skills were already installed during Setup; this step confirms .claude/skills/ plus the task-board folders + INDEX.json are on disk, and wires the enforcement layers (pre-commit hooksPath + figma-gate CI) so a red done/ UI task cannot slip past unchecked.',
      promptTemplate: TPL.step14,
      verifyHint: 'ls .claude/skills/ && git config core.hooksPath  # expect the 11 skill directories + orchestrator/skills/checks/hooks',
      skipWhen: null
    }
  ];
