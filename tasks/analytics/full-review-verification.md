# Полный аудит аналитики — 2026-10-12

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

Статус: завершено. Повторное ревью, исправления, проверка скиллов, все существующие
Kotlin test modules и указанные Android/iOS build gates прошли.

Область: toolkit analytics API/core; feature analytics API/authorization;
foundation RetainedAnalytics + ScreenAnalytics; авторизация/use cases/route ID;
DI и Android/iOS интеграция. Соседние crash-reporting/messaging включены в тесты.
Firebase delivery, provider sign-in, запуск Firebase-приложения и uploads не выполняются.

## Ревью и исправления

Основной self-review и независимые проверки core/foundation, авторизации и
документации выполнены в два прохода. Дополнительно проверены CrashReporting,
Messaging, Android Firebase/Swift adapters и bootstrap.

| Находка | Исправление |
|---|---|
| Два NavigationContext в тесте DI создавались без обязательного reason | Явный Initial в ScreenAnalyticsTest |
| Тесты юзкейсов не фиксировали все запросы подготовки и их порядок | Fixture записывает каждый запрос; проверяются полные последовательности и остановка на каждой ошибке |
| Не было проверки фактического Register/Email декоратора и error/navigation pipeline RegistrationViewModel | Тест реальной VM, RegisterUseCase и generated modules, с исправным и падающим reporting |
| Авторизация вручную дублировала настройки KMP test source sets | Общий kotlin.multiplatform.test.convention |
| Тест CAS публикации StateFlow реализует экспериментальный coroutines API без явного согласия | Локальный ExperimentalForInheritanceCoroutinesApi opt-in |
| Активные инструкции повторяли старые варианты, неверно утверждали отсутствие тестов и допускали UI Firebase transport | Консолидированы skill/standard/guide/coverage и связанные UI/DI/data/build инструкции |
| Инструкции путали Apple code с ID token и цельные LoginUseCase с прежними API | Исправлены по текущим credential/use-case контрактам |

В рабочем коде после повторного прохода не найдено подтверждённых дефектов,
требующих дополнительных обёрток или новой архитектуры. Сохранены:

- Один AuthAnalytics entry point, отдельные consumer models, internal feature implementation.
- Цельные login/register операции и бизнес-навигация Splash без telemetry outcomes.
- Stateless annotated providers; mutable scenarios принадлежат component owners.
- Локальный RetainedAnalytics с проверкой identity перед register; прежние keys и wire shape.
- ScreenAnalytics API/internal DI implementation; callbacks и presentation отдельны для каждого binding.
- Изоляция обычных reporting Exceptions, исходных business errors и caller cancellation.
- Явные state/owner invariants, strict schema validation и fatal Errors.
- Ограниченные очереди/diagnostics, SDK вне locks и вызывающего business coroutine,
  без дополнительных scopes/retries/запросов на уровне feature analytics.

## Скиллы и стандарт

Canonical source: [.claude/skills/feature-analytics](../../.claude/skills/feature-analytics/SKILL.md).
Обновлены UI reference, metadata, feature-standard, integration-guide,
authorization-coverage, coverage template и связанные инструкции.
Проверены YAML/frontmatter/UI metadata, ссылки, code fences и примеры актуальных API.
Независимая проверка на примере будущего editor/save сценария выявила и помогла
уточнить applicability persistence, отличие entity/draft ID от FlowId и Restore
как реактивацию существующей поверхности. Пример не интегрировался в продукт.
Штатный Python skill validator требует отсутствующий PyYAML; эквивалентные YAML
проверки выполнены Ruby без установки зависимостей.

## Проверки

Основной прогон завершён exit 0: BUILD SUCCESSFUL, 11m 26s, 4285 tasks.

```sh
./gradlew \
  :toolkit:analytics:api:testAndroidHostTest \
  :toolkit:analytics:core:testAndroidHostTest \
  :analytics:authorization:testAndroidHostTest \
  :ui-core:foundation:testAndroidHostTest \
  :ui-screen-features:authorization:testAndroidHostTest \
  :toolkit:crash-reporting:core:testAndroidHostTest \
  :toolkit:messaging:core:testAndroidHostTest \
  :shared:testAndroidHostTest \
  :toolkit:analytics:api:iosSimulatorArm64Test \
  :toolkit:analytics:core:iosSimulatorArm64Test \
  :analytics:authorization:iosSimulatorArm64Test \
  :ui-core:foundation:iosSimulatorArm64Test \
  :ui-screen-features:authorization:iosSimulatorArm64Test \
  :toolkit:crash-reporting:core:iosSimulatorArm64Test \
  :toolkit:messaging:core:iosSimulatorArm64Test \
  :androidApp:assembleDebug :androidApp:assembleRelease \
  :androidApp:lintDebug :androidApp:lintRelease \
  :shared:assembleSharedDebugXCFramework \
  -x :androidApp:uploadCrashlyticsMappingFileRelease --offline
```

Расширенный прогон завершён exit 0: BUILD SUCCESSFUL, 18m 14s, 2346 tasks.
Он включает остальные существующие Kotlin test modules:

```sh
./gradlew \
  :data-features:feature-api:testAndroidHostTest \
  :data-features:feature-api:iosSimulatorArm64Test \
  :toolkit:connectivity:testAndroidHostTest \
  :toolkit:connectivity:iosSimulatorArm64Test \
  :toolkit:http-client:testAndroidHostTest \
  :ui-screen-features:main:testAndroidHostTest \
  :ui-screen-features:training:testAndroidHostTest \
  :ui-screen-features:exercises:testAndroidHostTest \
  :ui-screen-features:profile:testAndroidHostTest \
  :shared:assembleSharedReleaseXCFramework --offline
```

XML results после выполненных test tasks: 294 Android + 203 iOS; failures/errors/
skipped = 0. Некоторые неизменённые tasks были UP-TO-DATE: результаты относятся
к текущим исходникам; повторные прогоны не суммируются как дополнительные тесты.

| Модуль | Android host | iOS Simulator |
|---|---:|---:|
| toolkit/analytics/api | 16 | 16 |
| toolkit/analytics/core | 36 | 32 |
| analytics/authorization | 19 | 19 |
| ui-core/foundation | 19 | 8 |
| ui-screen-features/authorization | 24 | 24 |
| toolkit/crash-reporting/core | 11 | 11 |
| toolkit/messaging/core | 25 | 24 |
| shared | 24 | — |
| data-features/feature-api | 68 | 68 |
| toolkit/connectivity | 4 | 1 |
| toolkit/http-client | 4 | — |
| ui-screen-features/main | 11 | — |
| ui-screen-features/training | 20 | — |
| ui-screen-features/exercises | 10 | — |
| ui-screen-features/profile | 3 | — |
| **Всего** | **294** | **203** |

Прочерк означает отсутствие соответствующего native test source set, а не
пропущенные тесты. После последнего opt-in изменения Messaging повторно проверен:
exit 0, BUILD SUCCESSFUL, 53s, 136 tasks; предупреждение inheritance opt-in устранено.

```sh
./gradlew :toolkit:messaging:core:testAndroidHostTest \
  :toolkit:messaging:core:iosSimulatorArm64Test --offline
```

Android Debug/Release собраны, включая R8. Android lint Debug/Release: 0 errors,
14 warnings в каждом отчёте (версии инструментов/зависимостей и launcher icon).

Xcode Debug Simulator и unsigned Debug device: exit 0, BUILD SUCCEEDED.

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug \
  -destination 'platform=iOS Simulator,id=8A3E8CAA-5BCC-4A9C-9FB3-610B71034B0F' \
  -disableAutomaticPackageResolution -jobs 1 CODE_SIGNING_ALLOWED=NO build

xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -destination 'generic/platform=iOS' \
  -disableAutomaticPackageResolution -jobs 1 CODE_SIGNING_ALLOWED=NO build
```

Проверены compilation/link Swift SDK adapters и bootstrap с текущим debug
XCFramework. AppIntents metadata extraction пропущена: приложение не содержит
AppIntents.framework. Simulator linker предупреждает об объекте ICU из Skiko
0.150.1, с minOS 18.5 при app deployment target 16.0. Локальная проверка archive
подтвердила совпадение с предсобранным Skiko KLIB: объект содержит только данные,
__text пуст, undefined symbols отсутствуют. Это metadata mismatch upstream;
framework MinimumOSVersion = 15.0. Deployment target и зависимости не менялись,
warning не подавлялся. Runtime на минимальной версии iOS этим не подтверждён.

Shared Debug/Release XCFramework включают ios-arm64 и ios-arm64-simulator.
Swift Release Simulator также завершён exit 0, BUILD SUCCEEDED:

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Release \
  -destination 'platform=iOS Simulator,id=8A3E8CAA-5BCC-4A9C-9FB3-610B71034B0F' \
  -disableAutomaticPackageResolution -jobs 1 CODE_SIGNING_ALLOWED=NO build
```

Финальные source boundary/obsolete symbol checks, YAML/docs links/fences и
git diff --check прошли. Логи сохранены локально:
/tmp/analytics-review-full-gates.log, /tmp/analytics-review-extended-gates.log,
/tmp/analytics-review-messaging-final.log, /tmp/analytics-review-swift-simulator.log,
/tmp/analytics-review-swift-device.log, /tmp/analytics-review-swift-release-simulator.log.

## Ограничения

Сборки выполняются без запуска Firebase-приложения. Реальная доставка, SDK sign-in,
push delivery, symbolication в Console и поведение на физическом устройстве здесь
не проверяются. Android upload исключён отдельным -x; Xcode использует Debug device
и Debug/Release simulator, без Release device symbol-upload phase.
Статическая оценка overhead не является измерением задержек/памяти на устройстве.
Общие compiler warnings о global opt-ins, expect/actual beta и соседнем коде
не скрываются. Среди соседних предупреждений — прежний BackHandler,
generated FeatureApiModule.module shadowing и redundant Elvis в TrainingStreakUseCase.
Итоговая сборка не объявляется warning-free.
