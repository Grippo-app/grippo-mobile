# references — platform-build-toolkit

Self-contained reference pack for the `platform-build-toolkit` skill. These files
carry their **own** build-time rules — the skill reads no external rule docs at
runtime. Read the routing table below to find the file for your task.

## Routing — topic → file

### Tech stack (versions, libraries, gradle.properties)

| Topic | File | Key sections |
| --- | --- | --- |
| Languages, mandatory versions, Android/iOS targets, source-set layout, global `optIn`, explicit-API mode, tooling defaults | [`tech-stack.md`](tech-stack.md) | Languages and versions · Targets — Android / iOS (Kotlin/Native) · Source-set layout · Global optIn · Explicit API mode · Tooling defaults |
| Library inventory (the full `gradle/libs.versions.toml` catalog), decompose-essenty spelling, adding a library, notable choices, version interdependencies | [`tech-stack.md`](tech-stack.md) | Library version catalog (libs.versions.toml) |
| `gradle.properties` full block + per-setting rationale + when to deviate | [`tech-stack.md`](tech-stack.md) | gradle.properties |

### Module structure (graph, dependency rules)

| Topic | File | Key sections |
| --- | --- | --- |
| Module graph, `settings.gradle.kts` reference, mandatory infra modules, optional modules, type-safe project accessors, per-module file convention | [`module-structure.md`](module-structure.md) | Module graph |
| Dependency direction (top→bottom), hard rules, `build.gradle.kts` shape, hidden-transitive-`api` anti-pattern, verifying the rules | [`module-structure.md`](module-structure.md) | Dependency rules |

### Toolkit modules (layout, build-logic, overview)

| Topic | File | Key sections |
| --- | --- | --- |
| `:toolkit:*` per-module list, representative build, per-module build specifics, rules | [`toolkit-modules.md`](toolkit-modules.md) | Toolkit module layout & per-module build specifics |
| `build-logic` / convention-plugin modules (structure, `settings.gradle.kts`, `convention/build.gradle.kts`, rules, when to add a plugin, module-level shape) | [`toolkit-modules.md`](toolkit-modules.md) | build-logic / convention-plugin modules |
| Toolkit overview — module shape, expect/actual-vs-interface, common patterns, rules, when to create a module, graph placement, anti-patterns | [`toolkit-modules.md`](toolkit-modules.md) | Toolkit overview — shape, patterns, rules |

### Toolkit utilities (per-`:toolkit:*` specs)

| Module | File | Sections |
| --- | --- | --- |
| `:toolkit:context` | [`toolkit-utilities-core.md`](toolkit-utilities-core.md) | Shape · Provider · Usage · Rules · Anti-patterns |
| `:toolkit:http-client` | [`toolkit-utilities-core.md`](toolkit-utilities-core.md) | HttpModule · ResponseValidator · ApiErrorParser · Build · Rules · Anti-patterns |
| `:toolkit:serialization` | [`toolkit-utilities-core.md`](toolkit-utilities-core.md) | Module · Config rationale · Build · Usage · When to add a different config · Rules · Anti-patterns |
| `:toolkit:logger` (+ `Mapping.log` null-tracker) | [`toolkit-utilities-core.md`](toolkit-utilities-core.md) | Object signature · Public read API · Mapping.log · Build · Usage · Rules · Anti-patterns |
| `:toolkit:date-utils` | [`toolkit-utilities-core.md`](toolkit-utilities-core.md) | DateTimeUtils · DateRange · DateRangeKind · DateRangePresets · DateFormat · DateFormatting · Usage · Build · Rules · Anti-patterns |
| `:toolkit:connectivity` | [`toolkit-utilities-platform.md`](toolkit-utilities-platform.md) | API · Options · statusUpdates semantics · Usage · Build · Koin module · Anti-patterns |
| `:toolkit:notification-manager` | [`toolkit-utilities-platform.md`](toolkit-utilities-platform.md) | API · NotificationKey · Permissions · Build · Usage · Rules · Anti-patterns |
| `:toolkit:permission-manager` | [`toolkit-utilities-platform.md`](toolkit-utilities-platform.md) | API · Implementation outlines (Android + iOS) · Build · Usage · Rules · Anti-patterns |
| `:toolkit:link-opener` | [`toolkit-utilities-platform.md`](toolkit-utilities-platform.md) | API · Implementations · Usage · Rules · Build · Anti-patterns |
| `:toolkit:theme` / `:toolkit:localization` (`AppTheme.current` / `AppLocale.current`) | [`toolkit-utilities-platform.md`](toolkit-utilities-platform.md) | AppTheme.current · AppLocale.current · Two accessors — why · Build · Rules · Anti-patterns |
| `:toolkit:image-loader` | [`toolkit-utilities-platform.md`](toolkit-utilities-platform.md) | Module shape · Installation · Why Ktor adapter · Caching · Build · Rules · Anti-patterns |
| Recipe: extend / add a `:toolkit:*` module | [`toolkit-cookbook.md`](toolkit-cookbook.md) | 0. Is toolkit the right home? · A. Extend (1-5) · B. Add new module (1-7) · Verify · Common mistakes |

### Gradle build (catalog, settings, convention plugins, representative builds)

| Topic | File | Key sections |
| --- | --- | --- |
| Convention plugins (KotlinMultiplatform / AndroidLibrary / AndroidApplication / ComposeMultiplatform / KoinAnnotation / Room / IosSwiftPackage / optional `screenshot.test.convention`), helpers, module shape, function-body style, anti-patterns | [`convention-plugins.md`](convention-plugins.md) | Plugin matrix · per-plugin sections · Helpers · Module build.gradle.kts shape · Anti-patterns |
| Version catalog (sections, naming, adding a library/plugin, bumping, pitfalls) **and** `settings.gradle.kts` (full file, restricted repos, Foojay resolver, `FAIL_ON_PROJECT_REPOS`, TYPESAFE accessors, module-path conventions, order, add/remove a module) | [`version-catalog-and-settings.md`](version-catalog-and-settings.md) | Version catalog · settings.gradle.kts |
| `:androidApp` build, `:shared` build (`api` vs `implementation`), and all 10 representative `build.gradle.kts` shapes (pure KMP · +Koin · +Compose · +Compose+Koin · dialog feature · data service · data mapper · toolkit date-utils · Room database · compose-libs widget), patterns, per-module checklist | [`gradle-build.md`](gradle-build.md) | :androidApp module build · :shared module build · Representative build.gradle.kts shapes |

### iOS framework & app project

| Topic | File | Key sections |
| --- | --- | --- |
| `IosSwiftPackageConventionPlugin` / XCFramework — what is exported, `isStatic`, `-lsqlite3` linker opt, `iosFrameworkName` transformation, how Xcode consumes the framework, refresh workflow, build tasks, common iOS-build issues, anti-patterns | [`ios-framework.md`](ios-framework.md) | iOS SwiftPackage / XCFramework |
| `:iosApp` Xcode project (verbatim drop-in fences) — Swift sources, Firebase providers, Info.plist / Config.xcconfig / Assets / AppIcon / AccentColor, GoogleService-Info.plist placeholder, xcscheme / xcworkspace / WorkspaceSettings, `run-ios.sh`, and both `project.pbxproj` variants (`firebaseEnabled` true / false) | [`ios-app-project.md`](ios-app-project.md) | How to apply · Swift sources · Firebase providers · Resources & config · Xcode project metadata · Run helper · project.pbxproj (true / false) |

## Not packed here (read via the relevant agent's `## Authoritative reading`)

- `:compose-libs:*` module rules → `compose-lib-builder` agent (Component-vs-`:compose-libs` boundary, the five rules, typical `build.gradle.kts`, when to extract).
- Thin-shell mandate, manifest essentials, and the shared-composition-root ctor
  `RootComponent(componentContext, close, deeplink)` → `app-shell-builder` agent.
