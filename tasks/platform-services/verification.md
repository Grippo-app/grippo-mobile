# Проверка сервисных ядер

Дата: 2026-10-11. Область: CrashReporting/Messaging API/core, SDK adapters, app bootstrap, existing consumer migration. Новых product scenarios нет. Приложения не запускаются; synthetic crash/push/token retrieval/remote SDK sends исключены. Настройки Firebase проекта не менялись.

## Финальная Gradle матрица

```sh
./gradlew \
  :toolkit:crash-reporting:core:testAndroidHostTest \
  :toolkit:messaging:core:testAndroidHostTest \
  :toolkit:crash-reporting:core:iosSimulatorArm64Test \
  :toolkit:messaging:core:iosSimulatorArm64Test \
  :toolkit:analytics:api:testAndroidHostTest \
  :toolkit:analytics:core:testAndroidHostTest \
  :toolkit:analytics:api:iosSimulatorArm64Test \
  :toolkit:analytics:core:iosSimulatorArm64Test \
  :shared:testAndroidHostTest \
  :androidApp:assembleDebug :androidApp:assembleRelease \
  :shared:assembleSharedDebugXCFramework \
  :androidApp:lintDebug :androidApp:lintRelease \
  -x :androidApp:uploadCrashlyticsMappingFileRelease
```

BUILD SUCCESSFUL, 1m 59s. Лог: `/tmp/grippo-services-verification-final.log`. Crash worker использует production Dispatchers.IO с Native extension import; контекст/токен имеют fail-fast bounds.

| Проверка | Android host | Native iOS Simulator |
|---|---:|---:|
| Crash reporting | 10 | 10 |
| Messaging | 16 | 16 |
| Analytics regression | 25 | 23 |
| Shared / DI wiring | 25 | — |
| Всего | 76 | 49 |

XML: failures/errors/skipped = 0 для всех 125 tests. Native tests выполняются в iosSimulatorArm64 runtime. Parallel producers проверяют настоящие Dispatchers Default/IO и платформенные locks; SDK transports заменены fixtures, отправки нет.

Android debug и release/R8 собраны. XCFramework включает iosArm64 и iosSimulatorArm64. Crashlytics mapping generation сохранён, remote upload исключён.

Android lint debug/release: BUILD SUCCESSFUL, 0 Errors/Fatal, по 14 Warning. Все warnings находятся в общих Gradle/version catalog и launcher icons: AndroidGradlePluginVersion, GradleDependency, NewerVersionAvailable, ObsoleteSdkInt, MonochromeLauncherIcon. Изменений dependency versions/icons в этой задаче нет; новых замечаний к cores/adapters нет. Reports: `androidApp/build/reports/lint-results-{debug,release}.xml`.

## Regression red / green

- Duplicate successful callback оставлял Pending: первоначально 10 messaging tests, 1 failure.
- Fetch stranding sync status и cancelled late SDK result: при снятии двух fixes 15 focused messaging tests, 2 failures.
- Oversized CrashContext читался/копировался до лимита: regression failed на прежнем constructor, теперь IllegalArgumentException до чтения элементов.
- SDK submission выполнялась в caller: recordingNeverInvokesSdkInCaller failed на прямой реализации. После IO worker — проходит на Android/native iOS.
- SDK exceptions/active CancellationException не останавливают worker; close при cancelled caller joins worker, future submissions rejected.

## Swift / SDK contract

Generated Objective-C header проверен: sealed interface result subclasses имеют плоские Swift names, enum members lowercase; Messaging SDK type явно qualified из-за совпадения с API protocol. Initial build поймал enum naming errors; они исправлены по header.

Установленный Apple SDK source проверен: FIRCrashlytics.m → FIRCLSExceptionRecordModel → FIRCLSExceptionRecord → FIRCLSUserLoggingWriteAndCheckABFiles использует dispatch_sync/logging queue и synchronous local file write. Поэтому Crash SDK calls выполняются на IO worker вне state lock. Swift adapter использует autoreleasepool, bounded cause traversal/frame conversion и original native addresses.

Финальные `xcodebuild` Debug builds — BUILD SUCCEEDED для обоих targets:

- `-destination 'platform=iOS Simulator,id=8A3E8CAA-5BCC-4A9C-9FB3-610B71034B0F'`; лог `/tmp/grippo-services-swift-verified-simulator.log`.
- `-destination 'generic/platform=iOS'`; лог `/tmp/grippo-services-swift-verified-device.log`.

Оба запуска: `-project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=NO build`. Это проверка настоящих adapters и финального IO framework, включая autoreleasepool. CODE_SIGNING_ALLOWED=NO; Debug device target не запускает Release/iphoneos Crashlytics upload script. Физический запуск/APNs и удалённая доставка не проверяются.

## Source / integration checks

`git diff --check` проходит. Поиск всех legacy provider/bridge/handler symbols и временных probes в Kotlin/Swift/Gradle пуст. Dependency graph: Firebase transport module подключается только Android app. Debug/release merged manifests подтверждают Application bootstrap и единственный nonexported MessagingService с FCM intent filter. В собранном Swift Simulator Info.plist проверены пять disabled analytics defaults. Firebase transport подключается только Android app; consumers используют api. SDK owns fatal capture/collection, persistence, remote delivery. Core queue не durable; Submitted означает возврат SDK method, не подтверждение upload. Authorization nullable boundary имеет explicit optional push policy с диагностикой, а не silent SDK fallback.
