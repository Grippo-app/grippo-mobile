# Отдельный слой аналитики

Статус: реализовано; self-review и Android/iOS проверки завершены.

## Границы

`:analytics:analytics-api` содержит feature interfaces и typed facts/state.
`:analytics:authorization` содержит @Single stateless factory, scenario facades,
internal matrices/events/operation timing/navigation attribution. Ни main, ни tests
этого implementation не зависят от UI, domain или data services. UI main зависит
только от analytics-api; shared подключает annotated implementation и UI bindings.

UI сохраняет StateKeeper keys и snapshots; lifecycle binding остаётся foundation.
Конкретные SDK/domain errors классифицирует UI binding. API операции принимают
suspend blocks, не use cases. Session restoration и начальная загрузка — UI.
Profile load failure возвращается typed result и становится failed terminal;
coroutine cancellation и token exceptions продолжают существующий error path.

17 схем и прежние wire names сохранены. Uncaught session callback failure
имеет failure_stage=session_check, потому что теперь измеряется whole callback;
profile failure result сохраняет profile_load. Старый token_read больше не
заявляется без наблюдения внутреннего этапа. Это расширение допустимых stage codes,
а не смена смысла failure event; отчёты stage учитывают session_check как границу
операции и не трактуют его как token_read. Остальные event keys/codes неизменны.

AuthFlowState wire shape и прежний SerialName сохранены, session snapshot по-прежнему
FlowContext; StateKeeper и route identity остаются у UI. Restored scenarios не
replay flow_started. UI types, formatters и SDK exceptions не выходят в analytics.

## Self-review

- Зависимости: main и tests обоих analytics modules проверены по Gradle declarations/imports; UI/domain/services/platform SDK/Compose/Decompose/Essenty/core исключены. UI commonMain содержит только analytics.analyticsApi, implementation находится в commonTest и shared.
- DI/lifetime: @Single stateless factory + @Single UI classifier, generated modules в composition root. Smoke test проверяет real generated factory resolution, singleton lifetime и разные mutable scenario instances.
- Контракты: API принимает typed facts и suspend blocks, без use cases/routers/formatters. Mutable state и technical events internal; persistence snapshot immutable и сериализуем. Core/Sink остаются отдельной общей механикой.
- Поведение: отсутствие token не объявляется expired, profile failure не становится NoSession/success; существующая redirect policy принадлежит UI. Profile failure result кодируется failed terminal. Whole callback exceptions/cancellation сохраняются. Login/Register preparation requests не менялись.
- Навигация: leaf lifecycle binding передаёт facade presentation callback; общий foundation implementation закрывает presentation. UI route→entry mapping exhaustive. Legacy route и literal legacy state JSON, сохранённый SerialName, StateKeeper keys, restoration without replay проверяются.
- Чистота: старый UI-owned facade/events/schema factories и module удалены. Нет aliases, event dual-write, дополнительной retry policy или coroutine scope ради tracking. Structured async scope только у UI reference data load. SDK/error mapping остаётся одним UI binding; profile classification не создаёт новый classifier на каждой ошибке.
- Wire/privacy: 17 схем, Schema/Key identity, bounded enum codes, несовместимые outcomes и stages проверяются. Credentials, form strings и exceptions не сериализуются. Uncaught session stage честно отражает whole callback, не пытается угадать token-read.

## Проверки

Прежние отчёты относятся к предыдущей архитектуре; текущий стандарт и skill обновлены.

| Набор | Android host | iOS simulator ARM64 | Failures/errors |
|---|---:|---:|---:|
| Независимая analytics implementation | 24 | 24 | 0 |
| UI authorization integration | 15 | 15 | 0 |
| Foundation | 15 | 4 | 0 |
| Shared | 24 | не запускался | 0 |

Полный Gradle run: BUILD SUCCESSFUL, 11m47s, exit 0,
`/tmp/grippo-analytics-layer-complete.log`.

```sh
./gradlew :analytics:authorization:testAndroidHostTest \
  :analytics:authorization:iosSimulatorArm64Test \
  :ui-screen-features:authorization:testAndroidHostTest \
  :ui-screen-features:authorization:iosSimulatorArm64Test \
  :ui-core:foundation:testAndroidHostTest :ui-core:foundation:iosSimulatorArm64Test \
  :shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:assembleRelease \
  :shared:assembleSharedDebugXCFramework :androidApp:lintDebug :androidApp:lintRelease \
  -x :androidApp:uploadCrashlyticsMappingFileRelease
```

После добавления literal legacy snapshot fixture выполнен свежий run UI тестов
обеих платформ: BUILD SUCCESSFUL, 29s, exit 0,
`/tmp/grippo-analytics-layer-ui-final.log`. Оба UI XML отчёта новее последнего
изменения test source; 15/15, 0 failures/errors. API/main source после полного run не менялся.

Android Debug/Release и Debug XCFramework device/simulator собраны.
Lint Debug/Release: 0 Error/Fatal, по 14 прежних warnings.
Headers обеих slices проверены по всем internal type declarations нового слоя:
implementation types не экспортируются.

Unsigned Swift Debug simulator: BUILD SUCCEEDED, exit 0,
`/tmp/grippo-analytics-layer-swift-simulator.log`.
Unsigned Swift Debug device: BUILD SUCCEEDED, exit 0,
`/tmp/grippo-analytics-layer-swift-device.log`.

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination 'platform=iOS Simulator,id=8A3E8CAA-5BCC-4A9C-9FB3-610B71034B0F' \
  -disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=NO build
# Device: та же команда с -destination 'generic/platform=iOS'.
```

Остались baseline Swift warnings libicu deployment target и AppIntents metadata.
iOS Release не запускался, чтобы не инициировать symbol upload.
`git diff --check`, skill frontmatter/link checks и static module boundary checks прошли.
Runtime delivery и performance profiling не проверялись.
Firebase sends, sign-in, app launch и mapping/symbol upload не выполняются.


## Повторный строгий аудит после переименования API (2026-10-12)

Проверены все consumer contracts, implementation/event families/matrices,
operation factories, exception preservation, snapshot/navigation state, UI
bindings/call sites, generated DI, Gradle boundaries, platform bootstrap wiring,
стандарт, skill и действующая coverage matrix.

Исправлено:

- Убраны явно добавленные coroutine-test/JSON dependencies из API без test sources.
- Убран serialization compiler plugin из implementation без @Serializable types;
  snapshot serializers генерируются в analytics-api, JSON dependency остаётся в tests.
- Snapshot другого route FlowId отклоняется до reporting; добавлен regression test.
  Совпадающие/legacy snapshots продолжают покрываться существующими tests.
- Руководство использует отдельный слой, internal schema/key matrix и facade;
  примеры leaf binding обновлены. Coverage больше не ссылается на удалённые методы.
- Skill/стандарт описывают соответствие snapshot и route-owned FlowId.

Дополнительных дефектов в проверенном объёме не обнаружено. Общий facade намеренно
проверяет допустимый scenario перед business callback; неверное применение не
имеет fallback. Stateless singleton factory не разделяет mutable scenarios.
Нет дополнительных scopes/retries/IO ради feature telemetry. Wire names и
serialized state shape не изменены; прежний SerialName сохранён намеренно.

Свежая проверка integration: BUILD SUCCESSFUL, 5m49s, exit 0,
`/tmp/grippo-analytics-strict-audit.log`.
Analytics authorization tests: Android 25, iOS simulator 25;
UI authorization tests: Android 15, iOS simulator 15;
shared Android host: 24. Failures/errors: 0.
Android Debug app и Debug XCFramework (device + simulator) собраны.
Оба Debug headers проверены по 24 internal declarations: утечек нет.

Свежая проверка toolkit analytics API/core и foundation Android/iOS tests:
BUILD SUCCESSFUL, 33s, exit 0, `/tmp/grippo-analytics-strict-core.log`.
`git diff --check`, module-boundary/obsolete-reference scans и skill YAML checks
прошли. Сохранены существующие warnings unrelated modules/global opt-ins.

Это локальный audit и targeted validation: Android Release/lint и Swift app builds
в этот повторный прогон не включены; их предыдущие результаты перечислены выше.
Runtime Firebase delivery, app launch, mapping/symbol upload и performance profiling
не выполнялись. Сборки не доказывают отсутствие любого runtime overhead.


## Текущий consumer API

Последующий refactor одного AuthAnalytics entry point с nested scenarios/types
и его свежие проверки описаны в `auth-entrypoint-review.md`. Разделы выше
фиксируют предыдущую структуру и результаты её проверок.
