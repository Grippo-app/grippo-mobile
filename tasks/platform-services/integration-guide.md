# Crash reporting и Messaging

## Границы

- `toolkit/crash-reporting/api`: CrashReporting, CrashSink, CrashCode, CrashContext/CrashAttribute.
- `toolkit/crash-reporting/core`: bounded queue до/после bootstrap и serial submission. SDK calls выполняет один background IO worker вне state lock. Установленный iOS SDK использует synchronous local file persistence; caller/UI этим не занимается. Transport не вызывает core повторно; отдельной coroutine на каждый report нет.
- `toolkit/messaging/api`: Messaging, PushToken, PushTokenResult, PushTokenHandler, MessagingBootstrap. Отсутствие готовности и ошибка различаются. Token — секрет; toString redacted.
- `toolkit/messaging/core`: один app-lifetime worker, latest-token-wins очередь, serial backend handler, observable sync status. No implicit retry. Новый callback может повторить failed sync; SDK/продукт могут явно повторить getToken.
- `data-services/firebase`: SDK transport и Android service. Shared/фичи не используют FirebaseProvider. Swift реализует экспортированные API, core не экспортируется.

## Crash reports

`CrashReporting.record(error, CrashContext(listOf(CrashAttribute.Count(CrashCode("attempt_count"), 2))))` сохраняет original throwable/stack. Context — snapshot, максимум 32 unique keys; только stable codes, nonnegative Long и Boolean. Нет произвольных maps/values. CancellationException не записывается. До и после bootstrap очередь максимум 16 единиц (reports/breadcrumbs) плюс одна in-flight, drop-new; счётчики не содержат payload. Suspend close завершает worker даже при отменённом caller, очищает очередь; новые submissions отклоняются. Уже начатый вызов SDK завершается перед возвратом close. Sink errors не ломают пользовательское действие и считаются отдельно, без recursive reporting/replay.

Android использует per-event CustomKeysAndValues, а не глобальный setCustomKey. Swift ExceptionModel сохраняет native frames и event-specific context в reason: custom key filters для этого контекста на iOS не обещаются. Native fatal capture, Crashlytics consent/configuration, symbol upload и remote delivery остаются ответственностью SDK/platform; новые consent/UI/auth integrations не добавлены. Не передавать личные данные в exception messages; typed context не очищает произвольный Throwable.

## Messaging

`getToken()` возвращает Available / Unavailable(NotInitialized, PlatformNotReady, Closed) / Failed(SdkFailure, InvalidToken). Отмена caller propagates; SDK error не выдаётся за NotReady. APNs mapping/permission/rendering остаются platform flows, обработка токена входит через validated receiver. Одновременно выполняется максимум один getToken; refresh/close выигрывает против старого fetch. Callback sync serial, промежуточные tokens conflated; нет неограниченных launch на каждый refresh, логов токена или собственной durable queue.

В существующем AuthorizationFeature пока сохранён nullable string контракт. Repository делает явное преобразование: Available → value; Unavailable → null; Failed → nonfatal report без токена + null. Это продуктовая best-effort политика optional push registration: SDK failure не срывает успешный login. SDK/core сохраняет explicit typed failure; token handler проверяет Result backend через getOrThrow и публикует Failed status. Account/session ownership и unregister остаются existing authorization responsibility; следующий этап должен рассматривать их вместе с login/logout, а не добавлять скрытые retries в core.

Koin.init возвращает instance-bound ApplicationBootstrap с контрактами AnalyticsBootstrap, CrashReportingBootstrap и MessagingBootstrap. Android подключает SDK в App.onCreate; iOS создаёт bootstrap в AppDelegate.init и подключает SDK после FirebaseApp.configure в didFinishLaunching. Глобальных Runtime/lookup нет; consumers получают constructor injection (BaseViewModel использует существующий Koin pattern). Каждый core регистрируется собственным аннотированным Koin-модулем. Handler разрешается в scope MessagingModule при callback для разрыва DI цикла authorization ↔ messaging. Android service не владеет token coroutine: app-lifetime core обрабатывает callback независимо от уничтожения service.

Проверки только local fake transports + compilation/builds. Не запускать test crash/token retrieval/remote push для этой приёмки.

SDK boundary сверена с официальными [Android Crashlytics docs](https://firebase.google.com/docs/crashlytics/android/customize-crash-reports), [Apple Crashlytics docs](https://firebase.google.com/docs/crashlytics/ios/customize-crash-reports) и [Apple FCM setup](https://firebase.google.com/docs/cloud-messaging/ios/get-started). Firebase Crashlytics skill использован для SDK contract review; provisioning/project settings не менялись.

TokenSyncStatus относится к последнему callback sync target; `Synced` означает успешное завершение PushTokenHandler. Это не отдельная гарантия durable delivery, session binding или отсутствия позднего server-side эффекта после HTTP timeout. Получение token и callback sync — разные операции: getToken не выполняет backend registration. Существующие login/register use cases выполняют её явно; поэтому dedup callback не заменяет registration при смене аккаунта.

Crash disposition Queued означает помещение в RAM queue, Submitted — успешное возвращение synchronous SDK method. Это не подтверждение remote delivery или успешной записи SDK на диск. State lock не удерживается во время SDK work; production dispatcher IO, dispatcher injection предназначена для tests.

Swift crash adapter оборачивает submission в autoreleasepool: временные Foundation/SDK objects освобождаются на каждой записи независимо от caller/background thread.

## Callback boundary

Android MessagingService и Swift MessagingDelegate вызывают MessagingBootstrap.onRegistrationToken. Проверка raw SDK string выполняется один раз в общем core; отдельного PushTokenReceiver и platform-side callback decoder нет. PushTokenDecoder остаётся только для результата SDK getToken. Core хранит latest callback token, а канал содержит только Unit wakeups; изменение порядка wakeups не меняет выбранный token.

Один callback request обрабатывается один раз, включая failure. Conflated Unit wakeup не запускает retries; новый callback с тем же token после failure является новой явной попыткой. Available instance повторно используется в StateFlow и как identity request; revision fetch не влияет на sync-request identity.

Публикация token/syncStatus Flow выполняется вне core lock через CAS и последующую сверку с каноническим состоянием. Сверка устраняет ABA-гонку, когда конкурентная публикация равного значения не меняет StateFlow. Observer может синхронно вызвать callback/getToken/close без удержания producer lock. syncStatus относится к обработке последнего валидного callback; SDK fetch/invalid token outcome наблюдается через token result.

## Структура ядра и конфигурация

Каждый сервис имеет публичные контракты в `api`, публичный `*Module` в корне `core` и закрытые реализации в `core/internal`. Папка `internal` подкреплена Kotlin-видимостью: engines, locks, catalog, configuration и diagnostic types объявлены `internal`. Shared подключает только `AnalyticsModule().module`, `CrashReportingModule().module`, `MessagingModule().module` и использует API bootstrap-контракты.

`AnalyticsModule` сам регистрирует внутреннюю `AnalyticsConfiguration` через `@Single`; её `expect/actual` factory находится в `analytics/core/internal`. Android читает `Application.applicationInfo.flags` (`FLAG_DEBUGGABLE`) из собственного Koin scope; отсутствие Application — ошибка wiring без fallback. iOS использует `Platform.isDebugBinary` связанного Kotlin framework. Xcode Debug/Release подключают соответствующий Debug/Release XCFramework. Модуля конфигурации приложения и параметра `analyticsDebug` у `Koin.init` больше нет.
