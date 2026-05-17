(function () {
  window.App = window.App || {};
  App.data = App.data || {};
  App.helpers = App.helpers || {};

  // ----------------------------------------------------------------------
  // Helpers shared with the wizard panel (P3) and the task form (P6).
  // ----------------------------------------------------------------------

  App.helpers.isAutoSkipped = function (step, setup) {
    return typeof step.skipWhen === 'function' && step.skipWhen(setup || {});
  };

  App.helpers.wizardComplete = function (state) {
    var setup = (state && state.setup) || {};
    var done = (state && state.progress && state.progress.wizardStepsDone) || [];
    return App.data.wizardSteps.every(function (step) {
      return App.helpers.isAutoSkipped(step, setup) || done.indexOf(step.id) >= 0;
    });
  };

  // ----------------------------------------------------------------------
  // Local utilities for the `build(setup)` steps. We re-implement the YAML
  // builder here (mirrors the one in panels/setup.js) so wizard-steps.js
  // has no dependency on a panel script.
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
    var pkg = derivedProductPackage(setup) || 'com.<product>';
    var product = safe(setup.productName) || '<Product>';
    var appId = safe(setup.applicationId) || (pkg + '.android');
    var host = safe(setup.backendHost) || '<product-domain>.com';
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
    lines.push('codexEnabled: ' + (setup.codexEnabled || 'auto'));
    lines.push('prelaunch: ' + (setup.prelaunch === false ? 'false' : 'true'));
    lines.push('supportedLocales:');
    for (var i = 0; i < locales.length; i++) lines.push('  - ' + locales[i]);
    lines.push('typefaceFactory: ' + face);
    lines.push('featuresWithRootComponentSuffix: []');
    lines.push('diHandWrittenModules: ' + (mods.length === 0 ? '[]' : '[' + mods.join(', ') + ']'));
    lines.push('---');
    return lines.join('\n');
  }

  // ----------------------------------------------------------------------
  // Build functions for steps whose body depends on list-valued or
  // conditional setup fields. Steps using `promptTemplate` go through
  // App.templates.render() at render time; build-based steps emit a final
  // string directly.
  // ----------------------------------------------------------------------

  function buildStep0(setup) {
    function fmtList(arr) {
      if (!Array.isArray(arr) || arr.length === 0) return '(none)';
      return arr.join(', ');
    }
    var lines = [];
    lines.push('Step 0 — confirm context');
    lines.push('');
    lines.push('Sanity-check the values the wizard collected before bootstrap begins.');
    lines.push('');
    lines.push('- productName:       ' + safe(setup.productName));
    lines.push('- orgName:           ' + (setup.orgName ? setup.orgName : '(empty — single-org package com.<product>.*)'));
    lines.push('- backendHost:       ' + safe(setup.backendHost));
    lines.push('- applicationId:     ' + safe(setup.applicationId));
    lines.push('- iosFrameworkName:  ' + safe(setup.iosFrameworkName));
    lines.push('- iosEnabled:        ' + (setup.iosEnabled === false ? 'false' : 'true'));
    lines.push('- firebaseEnabled:   ' + (setup.firebaseEnabled === false ? 'false' : 'true'));
    lines.push('- codexEnabled:      ' + safe(setup.codexEnabled || 'auto'));
    lines.push('- prelaunch:         ' + (setup.prelaunch === false ? 'false' : 'true'));
    lines.push('- typefaceFactory:   ' + safe(setup.typefaceFactory));
    lines.push('- firstDomain:       ' + safe(setup.firstDomain));
    lines.push('- supportedLocales:  ' + fmtList(setup.supportedLocales));
    lines.push('- authMethods:       ' + fmtList(setup.authMethods));
    lines.push('');
    lines.push("Reply with 'confirmed' or list anything you'd change.");
    return lines.join('\n');
  }

  function buildStep1_5(setup) {
    var lines = [];
    lines.push('Step 1.5 — populate 03-project-config.md');
    lines.push('');
    lines.push('Read first:');
    lines.push('- requirements/launch.md (Step 1.5)');
    lines.push('- requirements/00-overview/03-project-config.md');
    lines.push('');
    lines.push('Open `requirements/00-overview/03-project-config.md`. Replace the entire');
    lines.push('frontmatter block (between the first two `---` delimiters) with the YAML below.');
    lines.push('');
    lines.push('Verify:');
    lines.push('- No value in the frontmatter still reads `<Product>`, `<product>`, `<org>`,');
    lines.push('  `<product-domain>`, `<typeface>`, or any other placeholder.');
    lines.push('- `featuresWithRootComponentSuffix` and `diHandWrittenModules` reflect the');
    lines.push('  project as it actually intends to ship.');
    lines.push('- `prelaunch: true` for an unreleased project; `firebaseEnabled` matches the');
    lines.push('  Setup answer.');
    lines.push('');
    lines.push('Do NOT proceed to Step 2 until 03-project-config.md is fully populated. Every');
    lines.push('sub-agent invoked later reads this file.');
    lines.push('');
    lines.push(buildYaml(setup));
    return lines.join('\n');
  }

  function buildStep10(setup) {
    var firebase = setup.firebaseEnabled !== false;
    var lines = [];
    lines.push('Step 10 — implement :androidApp');
    lines.push('');
    lines.push('Read first:');
    lines.push('- requirements/launch.md (Step 10)');
    lines.push('- requirements/12-gradle-build/04-android-app-module.md');
    lines.push('');
    lines.push('Create:');
    lines.push('');
    if (firebase) {
      lines.push('- `App.kt` — `Application` subclass with `Koin.init { androidContext(this); androidLogger() }` and `FirebaseProvider.setup(this)`.');
    } else {
      lines.push('- `App.kt` — `Application` subclass with `Koin.init { androidContext(this); androidLogger() }`.');
    }
    lines.push('- `MainActivity.kt` — single Activity with `retainedComponent { RootComponent(it, parseDeeplink(intent)) }` and `setContent { root.Render() }`.');
    lines.push('- `AndroidManifest.xml` — `<application android:name=".App">`, `<activity android:name=".MainActivity" android:exported="true">` with the main launcher intent filter and deeplink filters if any.');
    if (firebase) {
      lines.push('- `build.gradle.kts` — verbatim from `requirements/12-gradle-build/04-android-app-module.md` (keep the `google-services` and `firebase-crashlytics` plugin lines).');
    } else {
      lines.push('- `build.gradle.kts` — verbatim from `requirements/12-gradle-build/04-android-app-module.md`, omitting the `google-services` and `firebase-crashlytics` plugin lines.');
    }
    lines.push('- `proguard-rules.pro` — empty for now (R8 defaults).');
    if (firebase) {
      lines.push('- `google-services.json` — placeholder file; the user will replace it with a real Firebase config later.');
    }
    lines.push('');
    lines.push('Verify: `./gradlew :androidApp:assembleDebug`.');
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
    lines.push('- requirements/launch.md (Step 12)');
    lines.push('');
    lines.push('Run:');
    lines.push('');
    lines.push('```bash');
    if (iosOn) {
      lines.push('./gradlew :' + ios + ':assemble' + iosCap + 'DebugXCFramework');
    }
    lines.push('./gradlew :androidApp:assembleDebug');
    lines.push('```');
    lines.push('');
    lines.push('Both must succeed.');
    lines.push('');
    lines.push('Run the Android app: a screen renders with the toolbar and at least one element from the design system.');
    if (iosOn) {
      lines.push('Run the iOS app (via Xcode): same screen renders.');
      lines.push('');
      lines.push('If both succeed, the foundation is complete.');
    } else {
      lines.push('');
      lines.push('iOS targets are disabled in this project — Android verification is sufficient. If iOS is later re-enabled, run `./gradlew :' + ios + ':assemble' + iosCap + 'DebugXCFramework` and bring up the iOS app then.');
    }
    return lines.join('\n');
  }

  // ----------------------------------------------------------------------
  // promptTemplate strings — quoted from `requirements/launch.md` per the
  // P3 contract. Tokens such as `<Product>`, `<product>`, `<org>`,
  // `<product-domain>`, `<iosFrameworkName>`, `<IosFrameworkName>`,
  // `<firstDomain>`, `<typeface>` are substituted at render time via
  // `App.templates.render(...)`. Meta-placeholders (`<X>`, `<x>`,
  // `<Entity>`) intentionally remain literal — they signal "your domain
  // type here" to the agent.
  // ----------------------------------------------------------------------

  var TPL = {};

  TPL.step1 = [
    'Step 1 — read the requirements',
    '',
    'Read first:',
    '- requirements/README.md',
    '- requirements/launch.md (Step 1)',
    '',
    'Read in order:',
    '',
    '- requirements/00-overview/*',
    '- requirements/01-tech-stack/*',
    '- requirements/12-gradle-build/*',
    '- requirements/02-module-structure/*',
    '- requirements/04-base-classes/*',
    '- requirements/05-design-system/*',
    '- requirements/06-data-layer/*',
    '- requirements/03-architecture-patterns/*',
    '- requirements/07-mappers/*',
    '- requirements/08-dependency-injection/*',
    '- requirements/10-toolkit/*',
    '- requirements/11-state-and-formatters/*',
    '- requirements/09-conventions/*',
    '- requirements/13-anti-patterns/*',
    '- requirements/14-cookbook/*',
    '',
    "Don't skim. The architectural rules are interconnected.",
    '',
    'Reply confirming you have read every listed file before proceeding.'
  ].join('\n');

  TPL.step2 = [
    'Step 2 — initialize the build system',
    '',
    'Read first:',
    '- requirements/launch.md (Step 2)',
    '- requirements/01-tech-stack/02-libraries.md',
    '- requirements/01-tech-stack/03-gradle-properties.md',
    '- requirements/12-gradle-build/01-convention-plugins.md',
    '',
    'Create the following in order:',
    '',
    '1. `gradle/wrapper/gradle-wrapper.properties` — Gradle 8.10+ (or the version matching `agp = 9.0.1`).',
    '2. `gradle/libs.versions.toml` — verbatim from `requirements/01-tech-stack/02-libraries.md`, with versions left unchanged. Adjust catalog keys if you renamed the product.',
    '3. `gradle.properties` — verbatim from `requirements/01-tech-stack/03-gradle-properties.md`.',
    '4. `settings.gradle.kts` — root settings file. Initially include ONLY:',
    '   ```',
    '   include(":androidApp")',
    '   include(":shared")',
    '   ```',
    '   You will add more modules incrementally.',
    '5. `build-logic/` with the convention plugins from `requirements/12-gradle-build/01-convention-plugins.md`. Create:',
    '   - `build-logic/settings.gradle.kts`',
    '   - `build-logic/build.gradle.kts`',
    '   - `build-logic/convention/build.gradle.kts` with the `gradlePlugin { plugins { ... } }` registrations.',
    '   - `build-logic/convention/src/main/kotlin/<each convention plugin>.kt`.',
    '   - `build-logic/convention/src/main/kotlin/com/<org>/<product>/{ApplySafely.kt, ConfigureJvmToolchain.kt, Libs.kt}`.',
    '6. Run `./gradlew --version` to verify Gradle/JVM toolchain. Expected: Gradle 8.10+, JVM 19.'
  ].join('\n');

  TPL.step3 = [
    'Step 3 — scaffold the foundation modules',
    '',
    'Read first:',
    '- requirements/launch.md (Step 3)',
    '- requirements/02-module-structure/01-module-graph.md (Mandatory infrastructure modules)',
    '- requirements/12-gradle-build/06-representative-builds.md',
    '',
    'In one batch, create the infrastructure modules listed in `02-module-structure/01-module-graph.md` under "Mandatory infrastructure modules". For each:',
    '',
    '1. Create the directory.',
    '2. Create the `build.gradle.kts` (template from `12-gradle-build/06-representative-builds.md`).',
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
    'Verify after each batch: `./gradlew tasks` should succeed without errors.'
  ].join('\n');

  TPL.step4 = [
    'Step 4 — implement the base classes',
    '',
    'Read first:',
    '- requirements/launch.md (Step 4)',
    '- requirements/04-base-classes/*',
    '',
    'Implement `:ui-core:foundation` from `requirements/04-base-classes/*`:',
    '',
    '- `BaseViewModel`, `BaseComponent`, `BaseScreen`, `BaseDirection`, `BaseLoader`, `BaseRouter`, `BaseResult`, `ResultKey`, `ResultKeys`, `ComponentIdentifier`, `NoneIdentifier`.',
    '- `OperationManager` (interface + impl).',
    '- `ResultManager` + `ResultEmitter`.',
    '- `collectAsStateMultiplatform` (expect/actual).',
    '- `platformAnimation` + `platformStackAnimator` (expect/actual).',
    '- `CoreModule` (`@Module @ComponentScan`).',
    '',
    'Use the verbatim code from `requirements/04-base-classes/*` as the starting point.',
    '',
    'Verify: `./gradlew :ui-core:foundation:assemble` builds.'
  ].join('\n');

  TPL.step5 = [
    'Step 5 — implement the toolkit',
    '',
    'Read first:',
    '- requirements/launch.md (Step 5)',
    '- requirements/10-toolkit/*',
    '',
    'Implement each `:toolkit:*` module from `requirements/10-toolkit/*`:',
    '',
    '- `:toolkit:context` — `NativeContext` expect/actual, `ContextModule`.',
    '- `:toolkit:logger` — `AppLogger`.',
    '- `:toolkit:serialization` — `Json` provider via `SerializationModule`.',
    '- `:toolkit:date-utils` — `DateTimeUtils`, `DateRange`, `DateRangeKind`, `DateRangePresets`, `DateFormat`, `DateFormatting`.',
    '- `:toolkit:http-client` — base `HttpClient` + `ApiErrorParser`.',
    '- `:toolkit:theme`, `:toolkit:localization` — `AppTheme.current`, `AppLocale.current` expect/actual.',
    '- `:toolkit:connectivity` — `Connectivity` interface + Android/iOS impls.',
    '- `:toolkit:notification-manager`, `:toolkit:permission-manager`, `:toolkit:link-opener` — minimal stub impls.',
    '- `:toolkit:image-loader` — Coil 3 + Ktor 3.',
    '',
    'Verify after each: the module builds and the Koin module is annotated.'
  ].join('\n');

  TPL.step6 = [
    'Step 6 — implement the design system',
    '',
    'Read first:',
    '- requirements/launch.md (Step 6)',
    '- requirements/05-design-system/*',
    '',
    'Implement `:design-system:resources:provider`:',
    '',
    '- `AppColor`, `AppDp`, `AppTypography`, `AppString`, `AppDrawable`, `AppIcon` interfaces/objects.',
    '- `LocalAppColors`, `LocalAppDp`, ... CompositionLocals.',
    '- `StringProvider` interface.',
    '- Initial `strings.xml` in `commonMain/composeResources/values/strings.xml` with one key (e.g. `app_name`).',
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
    '',
    'The typeface factory function for this project is `<typeface>` — wire it through `resource-builder` conventions when fonts are added.'
  ].join('\n');

  TPL.step7 = [
    'Step 7 — implement the data layer',
    '',
    'Read first:',
    '- requirements/launch.md (Step 7)',
    '- requirements/06-data-layer/*',
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

  TPL.step8 = [
    'Step 8 — implement the first feature',
    '',
    'Read first:',
    '- requirements/launch.md (Step 8)',
    '- requirements/14-cookbook/03-add-data-feature.md',
    '- requirements/14-cookbook/01-add-screen.md',
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
    '   - `<X>Router` in `:screen-api`.',
    '   - Seven MVI files for the list screen.',
    '   - `<X>RootComponent`.'
  ].join('\n');

  TPL.step9 = [
    'Step 9 — implement :shared',
    '',
    'Read first:',
    '- requirements/launch.md (Step 9)',
    '- requirements/02-module-structure/04-shared-composition-root.md',
    '- requirements/03-architecture-patterns/02-decompose-navigation.md',
    '- requirements/08-dependency-injection/01-koin-overview.md',
    '',
    '`:shared` is the composition root. Create:',
    '',
    '- `Koin.kt` listing every `<X>Module` so far.',
    '- `RootComponent`, `RootViewModel`, `RootScreen`, `RootDirection`, `RootContract`, `RootState`, `RootLoader`.',
    '- `DialogComponent`, `DialogContentComponent`, `DialogModule`.',
    '- `Deeplink` enum (empty initially or with one entry).',
    '- iOS bridge `RootViewController.kt` (`mainViewController(): UIViewController`).',
    '',
    'Verify: `./gradlew :<iosFrameworkName>:assemble<IosFrameworkName>DebugXCFramework`.'
  ].join('\n');

  TPL.step11 = [
    'Step 11 — implement :iosApp',
    '',
    'Read first:',
    '- requirements/launch.md (Step 11)',
    '',
    'Human handoff. Steps in this section require Xcode GUI interactions (creating the project, drag-and-drop XCFramework). The agent cannot perform these. Prepare the templates as fully as possible, then surface a clear handoff to the user with the exact list of clicks they need to perform.',
    '',
    'Create the Xcode project at `iosApp/` with:',
    '',
    '- `iOSApp.swift` — `@main` struct calling `FirebaseApp.configure()` and `KoinKt.doInit { _ in }`.',
    '- `ContentView.swift` — wraps `mainViewController()` from `:shared`.',
    '- `Info.plist` — basic.',
    '- `GoogleService-Info.plist` — placeholder.',
    '',
    'In Xcode, link `<iosFrameworkName>.xcframework` (drag-and-drop after running the XCFramework assemble task from Step 9).'
  ].join('\n');

  TPL.step13 = [
    'Step 13 — write CLAUDE.md',
    '',
    'Read first:',
    '- requirements/launch.md (Step 13)',
    '- requirements/13-anti-patterns/02-when-to-stop-and-ask.md',
    '',
    'Precondition. If you copied `requirements/` from a reference KMP repo, check the new project\'s root for an existing `CLAUDE.md`. If one is present, delete it — the file you write in this step REPLACES it. Two CLAUDE.md files at the same scope create contradictory rules; Claude Code loads both and applies them in arbitrary order.',
    '',
    '```bash',
    '[ -f CLAUDE.md ] && rm CLAUDE.md',
    '```',
    '',
    'If the existing CLAUDE.md was authored for your new project (i.e. you didn\'t import it from a reference repo), STOP and ask the user before deleting — it may contain real project context.',
    '',
    'Write `CLAUDE.md` only after asking the user for: (1) one paragraph describing the product purpose; (2) any cross-repo rules. If the user has no input, write a minimal scaffold that says "(filled in later)" for those sections — do not invent content.',
    '',
    'At the project root, create `CLAUDE.md` describing:',
    '',
    '- The product purpose (one paragraph — from the user).',
    '- Cross-repo rules (if applicable — from the user; "(filled in later)" otherwise).',
    '- Communication / decision-making conventions.',
    '- Scope discipline ("don\'t refactor without an explicit request", etc.).',
    '- When to stop and ask (mirror `requirements/13-anti-patterns/02-when-to-stop-and-ask.md`).'
  ].join('\n');

  TPL.step14 = [
    'Step 14 — install sub-agents',
    '',
    'Read first:',
    '- requirements/launch.md (Step 14)',
    '- requirements/README.md (section "Sub-agents — install before first use")',
    '- requirements/sub-agents/README.md',
    '',
    'After the foundation builds green and the first end-to-end "hello world" feature is in place, install the sub-agent toolkit so ongoing work can be automated.',
    '',
    'Recommended (symlink — edits in `requirements/sub-agents/` propagate immediately):',
    '',
    '```bash',
    'mkdir -p .claude/agents',
    'ln -sf "$(pwd)/requirements/sub-agents/builders/"*.md   .claude/agents/',
    'ln -sf "$(pwd)/requirements/sub-agents/validators/"*.md .claude/agents/',
    'ln -sf "$(pwd)/requirements/sub-agents/helpers/"*.md    .claude/agents/',
    '```',
    '',
    'Alternative (copy — snapshot, no propagation):',
    '',
    '```bash',
    'mkdir -p .claude/agents',
    'cp requirements/sub-agents/builders/*.md   .claude/agents/',
    'cp requirements/sub-agents/validators/*.md .claude/agents/',
    'cp requirements/sub-agents/helpers/*.md    .claude/agents/',
    '```',
    '',
    'Verify:',
    '',
    '```bash',
    'ls .claude/agents/   # expect orchestrator + builders + validators + helpers',
    'bash requirements/sub-agents/lint.sh',
    '```',
    '',
    'Confirm `requirements/00-overview/03-project-config.md` was populated in Step 1.5. From here on, drop new tasks at `requirements/tasks/TASK_<N>_<title>.md` and ask the parent Claude session to "run task TASK_N_<title>.md". The orchestrator drives the rest.'
  ].join('\n');

  // ----------------------------------------------------------------------
  // The 16 step entries (Steps 0, 1, 1_5, 2..14). Each entry exposes
  // EITHER `promptTemplate` OR `build(setup)`; the wizard panel chooses
  // `build` over `promptTemplate` when both are defined.
  // ----------------------------------------------------------------------

  App.data.wizardSteps = [
    {
      id: '0',
      title: 'Step 0 — confirm context',
      hook: 'Sanity-check the values the wizard collected before the agent starts.',
      build: buildStep0,
      verifyHint: "Agent replies 'confirmed' or you adjust Setup.",
      skipWhen: null
    },
    {
      id: '1',
      title: 'Step 1 — read every requirements chapter',
      hook: 'The architecture rules are interconnected. Skim-first invariably loses fidelity.',
      promptTemplate: TPL.step1,
      verifyHint: 'Agent confirms it read every listed file.',
      skipWhen: null
    },
    {
      id: '1_5',
      title: 'Step 1.5 — populate 03-project-config.md',
      hook: 'Every sub-agent reads this file. Wrong value here = wrong everywhere.',
      build: buildStep1_5,
      verifyHint: "rg '^productName:' requirements/00-overview/03-project-config.md",
      skipWhen: null
    },
    {
      id: '2',
      title: 'Step 2 — initialize the build system',
      hook: 'Without convention plugins green, every later module fails the same way.',
      promptTemplate: TPL.step2,
      verifyHint: './gradlew --version  # expect: Gradle 8.10+, JVM 19',
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
      verifyHint: './gradlew :ui-core:foundation:assemble',
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
      verifyHint: './gradlew :<iosFrameworkName>:assemble<IosFrameworkName>DebugXCFramework  # if iosEnabled: false, run ./gradlew :shared:assemble instead',
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
      hook: 'Xcode handoff — the agent stages the Swift files; the user wires Xcode UI.',
      promptTemplate: TPL.step11,
      verifyHint: 'Xcode build succeeds and the iOS app launches in the simulator.',
      skipWhen: function (setup) { return setup.iosEnabled === false; },
      skipReason: 'iOS targets disabled in Setup — nothing to do for this step.'
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
      title: 'Step 14 — install sub-agents',
      hook: 'Symlinks/copies the agent toolkit into .claude/agents so the orchestrator becomes callable.',
      promptTemplate: TPL.step14,
      verifyHint: 'ls .claude/agents/  # expect orchestrator + builders + validators + helpers',
      skipWhen: null
    }
  ];
})();
