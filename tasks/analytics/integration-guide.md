# Analytics core: контракт и подключение фич

## Границы

Фича зависит от `:toolkit:analytics:api` и получает `Analytics` через constructor injection/Koin. Она не использует Firebase, AnalyticsSink, AnalyticsBootstrap или DefaultAnalytics. Общие типы не зависят от UI, Decompose и доменных моделей. Кодирование событий — синхронная чистая функция без IO/побочных действий.

`Analytics.track` принимает AnalyticsEvent, optional FlowContext и OperationId. Сырые имена/maps существуют только в декларациях и транспортном payload. AnalyticsEventSchema регистрируется один раз, используется тот же экземпляр; параметры создаются теми же экземплярами AnalyticsParameterKey. Каталог проверяет все зарегистрированные схемы, включая collisions и общий бюджет параметров Firebase.

Проверка схемы выполняется при создании каталога. Невалидная декларация — ошибка конфигурации приложения. Невалидное событие при разрешённом сборе и невалидная identity в debug приводят к исключению, в release отклоняются. Запрещённые события отсекаются до кодирования; схемы каталога проверяются независимо от consent. В release журнал выключен; debug хранит до 64 записей только с именами событий и статусами, без значений/ID/exception messages.

## Пример будущей фичи (ещё не интегрирован)

```kotlin
enum class SaveMode(override val analyticsCode: String) : AnalyticsCode {
    Create("create"), Edit("edit")
}

class WorkoutSaved(val mode: SaveMode, val exerciseCount: AnalyticsCount) : AnalyticsEvent {
    override val schema = Schema
    override fun parameters() = listOf(Mode.value(mode), Count.value(exerciseCount))

    companion object {
        private val Mode = AnalyticsParameterKey.code<SaveMode>("save_mode")
        private val Count = AnalyticsParameterKey.count("exercise_count")
        val Schema = AnalyticsEventSchema("workout_saved", 1, listOf(Mode, Count))
    }
}

// In the owning feature's annotated module, before Analytics is first resolved:
@Module
class TrainingAnalyticsModule {
    @Single
    @Named("training_analytics")
    internal fun provideSchema(): AnalyticsSchema =
        AnalyticsSchema(events = listOf(WorkoutSaved.Schema))
}
// shared/Koin.kt registers TrainingAnalyticsModule().module.

// Exactly at the successful outcome of the owning operation:
analytics.track(WorkoutSaved(SaveMode.Create, AnalyticsCount(exerciseCount)), flow, operationId)
```

В публичном коде реальной фичи применять explicitApi и её доменные типы. Пример сокращён для чтения. Для величин используются AnalyticsCount, DurationMillis и AnalyticsNumber; decimal допускает только finite. Boolean кодируется в Long 0/1. Enum хранит explicit analyticsCode, не enum.name. Free-form text намеренно не поддерживается. Для иной единицы измерения сначала вводится соответствующий проверяемый тип/ключ, затем схема; не подменять секунды миллисекундами.

Имена схем — lower_snake_case, стабильны между платформами и локалями. Большие ID не включаются в название события. Смена смысла требует новой схемы/обсуждения миграции отчётов; версия сама по себе не делает изменение совместимым. Максимум 15 собственных параметров резервирует место для всех 10 возможных полей общего контекста.

## Контекст

- NavigationContext: текущий экран, предыдущий экран, известная причина перехода, активный диалог. Общие ScreenViewed/DialogViewed уже зарегистрированы. Для экранов есть атомарный presentScreen и opt-in lifecycle.trackScreenViews; фичи пока не подключены. SDK automatic screen reporting выключен в manifest/plist, consent при этом не включается.
- FlowContext: FlowId, конкретный EntryPoint и исходный экран. Передаётся явно через сценарий и при необходимости сериализуется вместе с маршрутом/черновиком. Возврат на экран не меняет origin.
- AnalyticsContext.launchSource: только известный способ внешнего запуска. Кампании/install attribution добавляются отдельной схемой после определения источника данных.
- OperationId связывает попытку и результат. Он не гарантирует deduplication. Успех имеет одного владельца и отправляется после фактического успеха, без привязки к init/ViewModel/render.

ScreenId/DialogId/EntryPoint содержат проверяемый стабильный code; фичи объявляют их как константы, не создают из пользовательского ввода. FlowId/OperationId/UserId — непрозрачные ID; синтаксическая проверка не доказывает приватность. UserId не должен быть email, именем, токеном или их хешем. Идентификаторы flow/operation не следует регистрировать как GA4 custom dimensions с высокой кардинальностью.

## Политика сбора и identity

Ядро стартует с Unknown. Unknown и Denied ничего не отправляют и не буферизуют. SDK выключен конфигурацией платформ и получает Unknown при bootstrap. Пока фичи consent/auth не подключены, production analytics остаётся выключенной.

Будущая интеграция consent должна передать подтверждённый выбор через updateCollection; сохранение/восстановление выбора относится к этой фиче, сейчас выбора и persistence нет. Firebase сохраняет setAnalyticsCollectionEnabled/setConsent между запусками и может переопределить значения manifest/plist. Перед включением production нужно спроектировать bootstrap вместе с хранением выбора, чтобы восстановленный SDK policy и политика ядра совпадали с самого старта. Одни static defaults не дают гарантии сброса ранее сохранённого разрешения до SDK auto-init. Текущий bootstrap дополнительно выключает SDK до первого потребителя аналитики.

Все рекламные consent-типы адаптеры оставляют Denied. Grant включает только analytics storage и collection. Denied/Unknown также очищают identity/свойства через явно зарегистрированные property keys. Изменение identity или политики сбора удаляет ожидающие события, чтобы не приписать их новому аккаунту. Даже изменение свойств того же пользователя очищает очередь — осознанная политика приоритета корректной атрибуции; dropped statuses доступны в debug.

При удалении user property из продукта её старый ключ сохраняют в каталоге для явной очистки либо выполняют отдельную миграцию SDK. Простое удаление ключа из каталога не очистит уже сохранённое Firebase значение.

Основные calls SDK выполняет один worker, уступающий dispatcher после 32 единиц работы; bootstrap выполняется до attach, shutdown disable — после join. Вызовы не пересекаются. updateCollection/updateIdentity меняют gate/очередь синхронно, применение к SDK происходит последовательно в worker. Уже начатый SDK-вызов невозможно отозвать; возвращение updateCollection не означает, что Firebase закончил применение. Буфер до attachSink ограничен 128 событиями, drop-new при переполнении. Нет собственного хранения на диске или обещания exactly-once. Успешный send означает только принятие локальным SDK. Если событие извлечено из очереди и до send потеряло актуальность, diagnostic — DiscardedBeforeSend; close также учитывает оставшиеся события.

Исключения send диагностируются, следующие события продолжают обрабатываться. Сбой конфигурации закрывает отправку и очищает очередь; bootstrap/tooling может вызвать retrySinkConfiguration для явной повторной настройки без автоматического replay. Реальная отмена worker остаётся отменой. CancellationException действующего синхронного sink считается transport failure; он не должен незаметно остановить весь worker. Закрытие завершает join/disable даже при отменённом caller. App-lifetime worker не закрывают при уходе экрана в background.

## Проверка новой фичи

1. Карта фактов, обязательных значений, entry points и известных причин; назначить владельца каждого события.
2. Зарегистрировать AnalyticsSchema в Koin с уникальным qualifier. Не менять central core.
3. Проверить catalog и payload fixtures, optional values, ошибочные поля и границы типов.
4. RecordingAnalyticsSink находится в core/commonTest и доступен только тестам с fixtures. Production и preview его не содержат.
5. Проверить false successes, retries, back/restore, parallel flows, cold/warm external launch и account switch.
6. Проверить Android/iOS и Swift compilation, payload fixtures и политику consent. Runtime/DebugView проверки выполняются только при отдельном разрешённом этапе; пользователь исключил Firebase-отправку из текущих проверок.
7. Описать GA4 dimensions/metrics, funnel и условия измерения. Настройка Firebase/GA4/BigQuery отдельно от Kotlin-интеграции.

Нет записи свободного текста, URLs с query, request bodies, exception messages или телесных/чувствительных значений по умолчанию. Они требуют отдельного решения в схеме. Аналитика не выводит психологическую причину закрытия приложения — только известные действия/ответы.

Первый Granted включает SDK, затем устанавливает identity; очередь собственных событий ждёт завершения обеих операций. Identity update не отзывает consent временно. Это последовательность вызовов SDK, а не атомарная транзакция его автоматических событий.

## Структура ядра и конфигурация

Каждый сервис имеет публичные контракты в `api`, публичный `*Module` в корне `core` и закрытые реализации в `core/internal`. Папка `internal` подкреплена Kotlin-видимостью: engines, locks, catalog, configuration и diagnostic types объявлены `internal`. Shared подключает только `AnalyticsModule().module`, `CrashReportingModule().module`, `MessagingModule().module` и использует API bootstrap-контракты.

`AnalyticsModule` сам регистрирует внутреннюю `AnalyticsConfiguration` через `@Single`; её `expect/actual` factory находится в `analytics/core/internal`. Android читает `Application.applicationInfo.flags` (`FLAG_DEBUGGABLE`) из собственного Koin scope; отсутствие Application — ошибка wiring без fallback. iOS использует `Platform.isDebugBinary` связанного Kotlin framework. Xcode Debug/Release подключают соответствующий Debug/Release XCFramework. Модуля конфигурации приложения и параметра `analyticsDebug` у `Koin.init` больше нет.


## Единый стандарт интеграции фич

Обязательный шаблон: `feature-standard.md`, матрица: `feature-coverage-template.md`. Будущий первый эталон: `authorization-coverage.md`; пока он не реализован.

- Создать FlowContext через `FlowContext.start` один раз в точке входа и передавать/сохранять его в маршруте или сценарии. Не создавать новый flow на каждый Render, шаг или повторную попытку.
- Операции: `AnalyticsOperationEvents<T>` + `analytics.trackOperation(events, flow) { ... }`. Все четыре фабрики обязательны: started, completed, failed, interrupted. Completed классифицирует реальный типизированный result; `Result.failure` не объявляется success автоматически. Обвязка не запускает coroutine и вызывается внутри существующего safeLaunch/use-case.
- Обычная ошибка и CancellationException пробрасываются без замены. Если фабрика error event нарушает контракт, её ошибка добавляется в suppressedExceptions исходной ошибки; successful-path schema defects распространяются в debug как ошибки разработки. Fatal Error не превращается в продуктовый failure.
- Один вызов обвязки = одна попытка с новым OperationId; start/terminal разделяют ID и FlowContext. Длительность измеряется monotonic clock вокруг block, без времени event factories. Эти ID не являются backend idempotency keys.
- Экраны: leaf component один раз регистрирует `lifecycle.trackScreenViews(analytics, navigation = { ... }, flow = { ... })`. Один показ на каждое RESUMED, включая foreground return. Родительский контейнер и Render не отправляют screen_view. NavigationContext каждый раз предоставляет владелец навигации, с актуальным referrer/reason.
- Core presentScreen обновляет навигацию и кодирует screen_view в одном lock, сохраняя LaunchSource. ScreenPresentation.close очищает только текущую presentation; поздний pause/destroy старого компонента не меняет новый экран, даже если screen IDs совпадают. Lifecycle bridge закрывает presentation на pause/destroy.
- Flow start/completion/exit остаются бизнес-событиями фичи: общий слой не выводит причины из onDestroy, смены lifecycle, пропавшего токена или закрытия приложения. Явный flow exit не равен interruption отдельной операции.
