# Проверка сервисных ядер

Дата: 2026-10-11. Область: CrashReporting/Messaging API/core, SDK adapters, app bootstrap, existing consumer migration. Новых product scenarios нет. Приложения не запускаются; synthetic crash/push/token retrieval/remote SDK sends исключены. Настройки Firebase проекта не менялись.

## Финальная Gradle матрица

```sh
./gradlew \
  :androidApp:assembleDebug :androidApp:assembleRelease \
  :shared:assembleSharedDebugXCFramework \
  :androidApp:lintDebug :androidApp:lintRelease \
  -x :androidApp:uploadCrashlyticsMappingFileRelease
```

BUILD SUCCESSFUL, 1m 59s. Лог: `/tmp/grippo-services-verification-final.log`. Crash worker использует production Dispatchers.IO с Native extension import; контекст/токен имеют fail-fast bounds.

Android debug и release/R8 собраны. XCFramework включает iosArm64 и iosSimulatorArm64. Crashlytics mapping generation сохранён, remote upload исключён.

Android lint debug/release: BUILD SUCCESSFUL, 0 Errors/Fatal, по 14 Warning. Все warnings находятся в общих Gradle/version catalog и launcher icons: AndroidGradlePluginVersion, GradleDependency, NewerVersionAvailable, ObsoleteSdkInt, MonochromeLauncherIcon. Изменений dependency versions/icons в этой задаче нет; новых замечаний к cores/adapters нет. Reports: `androidApp/build/reports/lint-results-{debug,release}.xml`.

## Исправленные границы

Successful callback корректно обновляет sync status, late отменённый SDK result
не заменяет актуальный token. Oversized CrashReportContext отклоняется до чтения
элементов. SDK submission выполняется на IO worker, вне caller и state lock.
Обычные SDK exceptions не останавливают worker; close joins worker даже при
отменённом caller, future submissions rejected.

## Swift / SDK contract

Generated Objective-C header проверен: sealed interface result subclasses имеют плоские Swift names, enum members lowercase; Messaging SDK type явно qualified из-за совпадения с API protocol. Initial build поймал enum naming errors; они исправлены по header.

Установленный Apple SDK source проверен: FIRCrashlytics.m → FIRCLSExceptionRecordModel → FIRCLSExceptionRecord → FIRCLSUserLoggingWriteAndCheckABFiles использует dispatch_sync/logging queue и synchronous local file write. Поэтому Crash SDK calls выполняются на IO worker вне state lock. Swift adapter использует autoreleasepool, bounded cause traversal/frame conversion и original native addresses.

Финальные `xcodebuild` Debug builds — BUILD SUCCEEDED для обоих targets:

- `-destination 'platform=iOS Simulator,id=8A3E8CAA-5BCC-4A9C-9FB3-610B71034B0F'`; лог `/tmp/grippo-services-swift-verified-simulator.log`.
- `-destination 'generic/platform=iOS'`; лог `/tmp/grippo-services-swift-verified-device.log`.

Оба запуска: `-project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=NO build`. Это проверка настоящих adapters и финального IO framework, включая autoreleasepool. CODE_SIGNING_ALLOWED=NO; Debug device target не запускает Release/iphoneos Crashlytics upload script. Физический запуск/APNs и удалённая доставка не проверяются.

## Source / integration checks

`git diff --check` проходит. Поиск всех legacy provider/bridge/handler symbols и временных probes в Kotlin/Swift/Gradle пуст. Dependency graph: Firebase transport module подключается только Android app. Debug/release merged manifests подтверждают Application bootstrap и единственный nonexported MessagingService с FCM intent filter. В собранном Swift Simulator Info.plist проверены пять disabled analytics defaults. Firebase transport подключается только Android app; consumers используют api. SDK owns fatal capture/collection, persistence, remote delivery. Core queue не durable; Submitted означает возврат SDK method, не подтверждение upload. Authorization nullable boundary имеет explicit optional push policy с диагностикой, а не silent SDK fallback.
