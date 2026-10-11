# Рефакторинг API аналитики

Wire-имена, версии, обязательность параметров и поведение сохраняются.
Изменения source API намеренные: все локальные потребители обновлены, старых алиасов нет.

| Было | Стало | Назначение |
|---|---|---|
| EventDefinition | AnalyticsEventSchema | Имя, версия и типизированные параметры одного события |
| AnalyticsDefinitions | AnalyticsSchema | Схемы событий и свойства пользователя, регистрируемые фичей |
| ParameterKey | AnalyticsParameterKey | Типизированная декларация параметра |
| UserPropertyKey | AnalyticsUserPropertyKey | Типизированная декларация свойства пользователя |
| event.definition | event.schema | Схема конкретного экземпляра события |
| ScreenViewed.Definition / DialogViewed.Definition | .Schema | Повторно используемая встроенная схема |

Пакеты API: `schema`, `context`, `identity`, `events`, `operation`, `transport`,
`lifecycle`, `values`. В корне остаётся фасад `Analytics`. Один публичный тип на файл;
операционные фабрики и исполняющий extension разделены. Внутренние валидаторы ID
не экспортируются как публичный API.

`core/internal/schema/AnalyticsContextField` — единая матрица имён общих полей;
кодирование и запрет конфликтующих параметров используют те же записи.
`AnalyticsSchemaRules` хранит ограничения и зарезервированные имена транспорта.
Строки в декларациях событий и enum wire-кодов остаются явными: это определения
контракта, а не вычисляемые имена или строки внутри бизнес-логики.

Self-review: сохранены reference identity для регистрации схем и ключей,
лимиты до аллокаций, копии коллекций, @Throws на suspend API для Swift,
порядок контекста, отмена coroutine, владение screen presentation и явный consent.
Матрица и таблицы ограничений создаются однократно, новых jobs, reflection,
регистрации через classpath или динамических схем нет.

Проверки окончательного кода:

- Gradle: BUILD SUCCESSFUL, `/tmp/grippo-analytics-refactor-final.log`.
- Android host tests: 87; iOS simulator tests: 48; ошибок и пропусков нет.
- Android assembleDebug/assembleRelease; Crashlytics mapping upload исключён.
- shared assembleSharedDebugXCFramework: device arm64 и simulator arm64.
- Xcode Debug unsigned: симулятор и устройство, оба BUILD SUCCEEDED.
- Android lint Debug/Release: 0 ошибок, 14 прежних предупреждений в каждом варианте.
- git diff --check: без ошибок.
- Сравнение исходных деклараций: wire-имена встроенных событий/параметров,
  enum-коды launch/navigation сохранены; все прежние публичные типы представлены
  под новыми именами или прежними именами в новых пакетах.
- Заголовки обеих частей XCFramework: публичные схемы экспортированы,
  матрица, правила и внутренние helpers скрыты.

Команда Gradle:

```sh
./gradlew :toolkit:analytics:api:testAndroidHostTest :toolkit:analytics:core:testAndroidHostTest :ui-core:foundation:testAndroidHostTest :toolkit:analytics:api:iosSimulatorArm64Test :toolkit:analytics:core:iosSimulatorArm64Test :ui-core:foundation:iosSimulatorArm64Test :shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:assembleRelease :shared:assembleSharedDebugXCFramework :androidApp:lintDebug :androidApp:lintRelease -x :androidApp:uploadCrashlyticsMappingFileRelease
```

Swift-логи: `/tmp/grippo-analytics-refactor-swift-simulator.log`,
`/tmp/grippo-analytics-refactor-swift-device.log`.
Firebase runtime и запуск приложений не проверялись; фичи не интегрировались.

