# Общий стандарт аналитики фич: реализация и проверка

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

Дата: 2026-10-12. Scope подтверждён пользователем: общий стандарт и обвязка сначала, **без интеграции авторизации или других фич**.

## Реализовано

- `AnalyticsOperationEvents<T>`: обязательные started/completed/failed/interrupted factories. Бизнес-смысл результата, методы, этапы, ошибки и явная provider cancellation определяет фича через конкретные typed events. Общего события с arbitrary name/map нет.
- `Analytics.trackOperation`: свежий OperationId на попытку, общий FlowContext для start/terminal, monotonic duration вокруг block, текущий coroutine context без нового job/dispatcher/retry. Already-cancelled caller не начинает операцию; cancellation между start/block не запускает бизнес-код; cancelled block не объявляется completed. Первичное исключение сохраняется, failure-event defect добавляется в suppressedExceptions. Completed encoding находится вне catch бизнес-блока, поэтому не порождает второй terminal event.
- FlowId/OperationId factories и FlowContext.start; восстановление сериализованного flow не вызывает новую генерацию ID. Происхождение сценария хранится в контексте, не в глобальном last-entry-point.
- `Analytics.presentScreen` + ScreenPresentation: контекст и screen_view фиксируются в одном state lock, LaunchSource сохраняется. Close очищает только текущую presentation; old/same-screen/duplicate close не повреждают новую. SDK worker сигнализируется после выхода из lock.
- `Lifecycle.trackScreenViews` в `ui-core/foundation/analytics`: opt-in у leaf component, один view на RESUMED, включая foreground return, close на pause/destroy. Подписка на уже RESUMED lifecycle не дублирует начальный показ. Контекст читается при активации; lifecycle callbacks обрабатываются сериализованно на UI thread, как у самого UI lifecycle. Referrer/reason не выводятся эвристикой.
- Правила проекта и integration guide, обязательные feature-standard/coverage-template, proposal матрица авторизации. Feature catalog регистрируется annotated qualified @Single через generated .module; Koin getAll собирает несколько qualified groups.

## Self-review и исправления во время review loop

1. Error/cancellation: исключение от completed factory не должно попадать в catch операции и создавать второе failed событие; сохранена первичная ошибка при дефекте failure factory, проверены обе ситуации.
2. Cancellation timing: добавлены ensureActive до запуска и после возврата block; отмена до/во время операции не превращается в completed.
3. Concurrency: screen name/navigation metadata должны быть одним snapshot. Добавлен atomic presentScreen для согласованного screen_name/screen_id/origin_screen и launch source. Presentation ownership и policy сохранены.
4. SDK defaults: в исходных manifest/plist не было явного отключения automatic screen reporting. Добавлены Boolean false `google_analytics_automatic_screen_reporting_enabled` / `FirebaseAutomaticScreenReportingEnabled`; merged Android manifests и собранный simulator plist проверены. Ключи подтверждены [официальной документацией Firebase](https://firebase.google.com/docs/analytics/screenviews). Это не включает collection consent.
5. Swift exception boundary: публичная suspend-обвязка получила @Throws(Exception::class). Финальные headers для device/simulator конвертируют Exception в NSError; internal executeTrackedOperation и DefaultAnalytics не экспортируются. Generic operation factories предназначены для common Kotlin фич; Swift shell сохраняет SDK bootstrap обязанности.
6. Архитектура/overhead: execution implementation находится в api/internal с internal visibility, core presentation — private внутри internal engine. Нет новых DI runtimes, глобальных registries, event buses, timers, workers, новых Gradle-модулей или библиотечных версий. Используются существующий worker/queue и один lifecycle callback на подключённый leaf. На попытку создаются ID, monotonic mark и feature events; runtime performance profiling не выполнялся.

BaseViewModel, BaseComponent, OperationManager, ErrorProvider, authorization/use cases и shared Koin composition не изменены. Firebase data-services module сохранён.

## Проверки

Финальный Gradle прогон (`/tmp/grippo-feature-standard-verified.log`) завершился BUILD SUCCESSFUL:

```text
:androidApp:assembleDebug
:androidApp:assembleRelease
:shared:assembleSharedDebugXCFramework
:androidApp:lintDebug
:androidApp:lintRelease
-x :androidApp:uploadCrashlyticsMappingFileRelease
```

Lint Debug/Release: 0 errors, 14 existing warnings на вариант. Native Release XCFramework не собирался; Debug XCFramework содержит iosArm64 и iosSimulatorArm64. Оба header содержат новый public API без internal implementations.

Финальная Swift app compilation с актуальным XCFramework и Info.plist: Debug iOS Simulator arm64 и generic iOS device — BUILD SUCCEEDED, CODE_SIGNING_ALLOWED=NO. Логи: /tmp/grippo-feature-standard-swift-simulator-final.log и /tmp/grippo-feature-standard-swift-device-final.log. В собранных device/simulator plist automatic screen reporting — Boolean false. git diff --check проходит.

## Границы результата

Наличие обвязки не включает tracking фич или consent. Матрица обязательна для следующей интеграции, но не заменяет review и не доказывает автоматическое покрытие всего UI. Flow start/exit, правильный business success и classification ошибок имеют одного владельца в фиче; onDestroy не угадывает пользовательскую причину. Start/terminal — локальные вызовы track, не гарантия доставки в Firebase: policy/queue/SDK могут отклонить событие.

Firebase runtime/send/DebugView, запуск приложений, provisioning, APNs delivery и production symbolication не выполнялись. Приложения компилируются без подписи и запуска.
