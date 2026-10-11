# Единый стандарт аналитики фич

Статус: общая обвязка реализована и проверена на Android/iOS; ни одна фича автоматически не подключается. Analytics consent по-прежнему обязателен. События до Granted не копятся для последующей отправки.

## Обязательная структура

```text
<owning-feature>/analytics/
  <Feature>AnalyticsModule.kt         публичный annotated DI module
  <Feature>Event.kt                   internal конкретные события/схемы
  <Feature>OperationEvents.kt         internal AnalyticsOperationEvents<T>
  <Feature>Screen.kt                  internal стабильные ScreenId/DialogId
  <Feature>EntryPoint.kt              internal точки входа
  <Feature>Failure.kt                 internal typed этапы/категории
```

Это роли, а не требование создавать пустые файлы. Маленькую связанную модель можно держать вместе. Если контракт нужен между модулями, его минимальная публичная часть принадлежит существующему feature API. UI и data не зависят от реализаций друг друга; событие находится у владельца факта. Не добавлять отдельный Gradle-модуль для каждой фичи, универсальный Reason/Event или обёртку с методом на каждую кнопку.

Фича получает Analytics через constructor injection. UI-компонент разрешает зависимости при retainedInstance; Render не содержит DI, Firebase, tracking или генерации ID. Core/Sink/Bootstrap не используются фичами.

## Контракт события

Конкретный AnalyticsEvent принимает только бизнес-типы: enum/sealed с явным analyticsCode, проверяемые ID/единицы, ограниченный typed result. Кодирование чистое и bounded. Keys закрыты в companion; Schema один и тот же экземпляр во всех вызовах. Названия lower_snake_case, schema version задана явно. Пользовательские данные не участвуют в именах событий или параметров.

До первой резолюции Analytics каждый `<Feature>AnalyticsModule().module` подключается в shared/Koin.kt. В модуле `@Single` + уникальный `@Named("<feature>_analytics")` регистрируют AnalyticsSchema с полным списком Schema. Built-in ScreenViewed/DialogViewed повторно не регистрировать. Полный каталог проверяет collisions, reserved names и лимиты при создании core.

Не менять каталог runtime и не использовать ручной `module { single { ... } }` для новых модулей. Koin module не получает runtime параметры через constructor: установленная генерация .module не сохраняет их.

## Сценарий и операция

FlowContext.start вызывается в реальной точке входа. EntryPoint и originScreen неизменны на протяжении сценария; flow переносится между шагами и восстанавливается через состояние маршрута/черновика. Возврат из background не создаёт новый flow. Каждая новая реальная попытка получает новый OperationId через trackOperation; retry не переиспользует attempt ID. Параллельные сценарии имеют свои contexts.

```kotlin
// Внутри существующего safeLaunch, события задаёт фича:
val result = analytics.trackOperation(operationEvents, flow) {
    useCase.execute()
}
// Навигация следует за block, не определяет его success.
```

AnalyticsOperationEvents<T> требует четыре чистые фабрики: started(), completed(result, duration), failed(error, duration), interrupted(duration). Completed получает возвращённый result, а не автоматически объявленный успех. Если use case возвращает Result, либо его failure раскрывается через getOrThrow в block, либо completed исчерпывающе кодирует success/failure. Нельзя назвать событие succeeded до подтверждения владельца бизнес-факта.

Типизировать provider/user cancellation у провайдера. Она отличается от coroutine interruption при уничтожении экрана/отмене job. Не распознавать отмену или backend error по message/локализованному тексту; не передавать Throwable/message/URL/body в payload. Ошибка разбирается владельцем в категорию и этап. Она продолжает существующий путь safeLaunch → ErrorProvider/CrashReporting.

Не оборачивать UI-кнопку и тот же use case одинаковыми attempt events. Для многоэтапного use case измерять разные факты: подтверждение авторизации, подготовку данных, push registration. Failure второго этапа не отменяет success первого. Явный выход из сценария фиксируется только известным действием, не по onDestroy; аварийное исчезновение восстанавливается в отчёте как отсутствие terminal event, без выдуманного exit reason.

## Показы и навигация

```kotlin
// Один раз в init конечного Component, его child lifecycle:
lifecycle.trackScreenViews(
    analytics = analytics,
    navigation = { navigationOwner.currentContext() },
    flow = { restoredFlow },
)
```

Каждый RESUMED — один screen_view, включая foreground return. Регистрация на уже RESUMED lifecycle даёт один начальный показ. Pause/destroy закрывает presentation и очищает текущую навигацию. Recomposition не является показом. Не регистрировать binding одновременно на контейнере и его leaf; overlays/dialogs имеют отдельно объявленную политику и DialogViewed.

Navigation owner обязан передать фактические screen/referrer/reason при каждой активации: Initial, Action, Back, Restore, External. Referrer отсутствует, если неизвестен. Возврат из background — Restore; возврат navigation back — Back. LaunchSource описывает источник запуска, а EntryPoint — начало бизнес-сценария. Не выводить source notification только по наличию URL и не объявлять истечение сессии по отсутствию токена.

Свой NavigationContext API не генерирует маршруты и не подписывается на всё приложение: это opt-in binding. Прямой presentScreen применяется у UI-хоста с иной моделью видимости; возвращённую ScreenPresentation нужно close при потере видимости. Core сохраняет launch attribution и защищает новый экран от позднего закрытия старого presentation.

Автоматический Firebase screen reporting отключён в Android manifest (`google_analytics_automatic_screen_reporting_enabled=false`) и iOS Info.plist (`FirebaseAutomaticScreenReportingEnabled=false`), чтобы он не дублировал ручной учёт. Ключи проверены по [официальной документации Firebase](https://firebase.google.com/docs/analytics/screenviews). Collection consent остаётся отдельной политикой.

Для операции originScreen берётся из FlowContext; screen_id в каждом payload — текущая активная поверхность на момент track. Поздний результат после ухода с экрана сохраняет flow/operation IDs, но не притворяется, что старый экран всё ещё виден.

## Матрица и проверки

Перед интеграцией скопировать `feature-coverage-template.md`; заполнить триггеры, точку входа, owner, параметры, все outcomes и тест. Новое действие/route/outcome требует строки матрицы или явного объяснения, почему tracking не нужен. Изменение смысла события требует решения по схеме/отчётам; increment version сам по себе не мигрирует GA4.

Минимальные автоматические проверки:

1. Payload fixtures для каждого concrete event/outcome, обязательных/optional параметров; Schema и Key identity совпадают с каталогом.
2. Полный feature catalog зарегистрирован при composition-root smoke test, нет collisions/budget overflow. Pure factories покрывают все допустимые typed variants; error mapping не анализирует текст.
3. Operation: один start/terminal, правильный business success, retries, exception preservation, actual cancellation и shared IDs.
4. Navigation: initial/back/restore, parent/leaf отсутствие дублей, pause/foreground, overlapping presentations и параллельные flows.
5. Consent Unknown/Denied, identity switch и отсутствие чувствительных полей; Android/iOS core tests и app compilation.

Матрица — обязательный артефакт ревью, но не автоматическое доказательство покрытия всего UI. Общая обвязка гарантирует механику подключённых вызовов; забытое подключение фичи должно выявляться её матрицей, fixtures и review. Сбор всех действий приложения и Firebase delivery не заявляются. Firebase runtime/send checks сейчас исключены пользователем.
