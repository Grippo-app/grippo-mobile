# Рефакторинг CrashReporting API

| Было | Стало | Роль |
|---|---|---|
| CrashCode | CrashDiagnosticCode | Стабильное диагностическое значение / breadcrumb |
| CrashCode в имени поля | CrashMetadataKey | Имя поля metadata, отдельный тип |
| CrashAttribute | CrashMetadataEntry | Типизированная запись: Code / Count / Flag |
| CrashContext.attributes | CrashReportContext.metadata | Snapshot metadata конкретного отчёта |
| CrashSink | CrashReportingSink | Платформенный транспорт |
| CrashExceptionInfo.typeName | CrashExceptionType.nameOf | Имя типа исключения для native adapter |
| record | reportException | Отправить nonfatal exception в очередь |
| breadcrumb | logBreadcrumb | Записать диагностический breadcrumb |

Публичные пакеты: metadata, context, values, transport, lifecycle. В корне остаётся
фасад CrashReporting. Каждый top-level публичный тип имеет свой файл;
варианты sealed metadata остаются вложенными в свой тип. Приватные Work/Submission
в worker не превращаются в публичные DTO.

Декларации полей фич — именованные CrashMetadataKey<T> с фабриками code/count/flag;
создать запись можно только через key.value(...), а конструкторы вариантов internal.
Ключ определяет тип значения на этапе компиляции; значения CrashDiagnosticCode
имеют другой тип. В core нет предустановленного набора feature metadata, поэтому
рефакторинг не добавляет фиктивные поля в отчёты.

Матрицы правил:

- internal CrashMetadataRules: формат ключей/кодов, максимум 32 metadata entries.
- internal CrashReportingQueuePolicy: default 16, максимум 128, yield после 32 submissions.
- transport CrashStackTracePolicy: максимум 32 cause nodes и 128 native frames.

Wire-значения сохраняются. Переименование source API намеренное, локальные Kotlin
и Swift consumers обновлены без старых aliases. SDK recordException и native
ExceptionModel остались прежними. Swift metadata по-прежнему привязана к отдельному
исключению, без обновления global SDK keys.

Self-review: сохранены исключение cancellation, bounded queue/drop-new, один worker,
отсутствие SDK вызова в caller, исходный Throwable, snapshot и уникальность metadata
по имени ключа (включая разные экземпляры декларации), проверка размера до копирования/чтения, stop/join при отмене caller.
Добавлена проверка недопустимого имени нового CrashMetadataKey.
Статические policy-объекты не добавляют работу на каждый отчёт; нет новых jobs,
reflection-based registration, retries, динамических схем или compatibility aliases.

Проверки завершены:

- Crash core: 11 Android host tests и 11 iOS simulator tests; ошибок и пропусков нет.
- Shared: 24 Android host tests; ошибок и пропусков нет.
- Android assembleDebug/assembleRelease: успешно; mapping upload исключён.
- shared assembleSharedDebugXCFramework: device arm64 и simulator arm64, успешно.
- Xcode unsigned Debug: simulator/device, оба BUILD SUCCEEDED.
- Android lint Debug/Release: 0 ошибок, 14 прежних предупреждений в каждом варианте.
- Экспорт обеих частей XCFramework: metadata keys/context/sink/policy доступны Swift;
  внутренние validation/queue policies и DefaultCrashReporting скрыты.
- git diff --check: без ошибок.

Gradle: `/tmp/grippo-crash-refactor-full.log`, BUILD SUCCESSFUL.
Swift: `/tmp/grippo-crash-refactor-swift-simulator.log`,
`/tmp/grippo-crash-refactor-swift-device.log`.
Лимиты stack policy в Swift читаются один раз на отчёт, вне цикла обхода cause.
SDK selectors recordException/record(exceptionModel:) сохранены.
В BaseViewModel и AuthorizationRepositoryImpl обновлены существующие вызовы;
новые feature hooks не добавлялись. Firebase runtime и запуск приложения не проверялись.

