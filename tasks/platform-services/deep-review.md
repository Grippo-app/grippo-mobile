# Повторный аудит Analytics, CrashReporting и Messaging

Дата: 2026-10-11. Область: API/core, generated Koin registrations, Android SDK transport, Swift SDK transport и startup обоих приложений. Фичи не расширялись; Firebase runtime-запросы и отправка тестовых событий/крашей не выполнялись.

## Исправления

1. **Messaging: ABA-гонка публикации StateFlow.** Конкурентный publisher мог завершить sync, оставив flow равным Synced; запоздавший publisher успешно заменял его на Pending, несмотря на канонический Synced. Один CAS loop этого не предотвращает. `internal/StateFlowPublication.kt` сверяет опубликованное значение с каноническим после CAS и повторяет публикацию при расхождении. Observer по-прежнему вызывается вне state lock; новые workers, очереди, locks или backend retries не добавлены. Детерминированный regression test перехватывает CAS настоящего MutableStateFlow: старый алгоритм дал AssertionError, исправленный проходит на JVM и Kotlin/Native.
2. **Analytics: лимиты каталога до flatten.** DI раньше выделял временные объединённые списки до проверки лимитов. `AnalyticsCatalog.fromDefinitions` сначала считает суммарные размеры через Long, включая два built-in события, проверяет бюджеты 500/25 и выделяет списки с точной ёмкостью. Built-in список переиспользуется. Проверены границы 498/499 feature events и превышение общего лимита properties несколькими группами.
3. **iOS Messaging: APNs доступ на главном потоке.** Kotlin consumer может вызвать getToken с фонового dispatcher, тогда как AppDelegate присваивает APNs token на main. В установленном Firebase SDK `FIRMessaging.m` поле apnsTokenData nonatomic, getter напрямую возвращает его. Swift adapter выполняет APNs проверку и начало асинхронного token request на main; вызов с main не создаёт дополнительного dispatch. Нет sync dispatch или блокирующего ожидания.
4. **iOS remote notification completion.** SDK bridge теперь возвращает `.noData`: приложение не загружает новых данных в этом callback. Комментарий отражает ручное forwarding при `FirebaseAppDelegateProxyEnabled=false`, без ложного утверждения о потере всех silent pushes.

## Повторный self-review

- Public contracts отделены от Firebase. В корне core остаются только annotated Modules; engines, configuration, catalog и locks — Kotlin internal. Generated Koin bindings сохраняют общий singleton consumer/bootstrap, конфигурация принадлежит AnalyticsModule.
- Deferred PushTokenHandler необходим для разрыва DI-цикла Messaging → Authorization → Repository → Messaging; разрешается при callback, не при startup.
- Analytics: bounded queue, consent/identity revision gates, отсутствие автоматического replay, fail-closed SDK configuration; diagnostics выключены в release.
- CrashReporting: bounded background queue, cancellation исключён, SDK calls вне state lock, ошибки transport не отправляются рекурсивно. Android использует per-event keys; Swift ExceptionModel содержит event-local metadata, не изменяет global keys.
- Messaging: один serial handler, latest request identity, dedup успешного token, explicit fresh callback после ошибки; fetch сериализован, revision gate защищает от overwrite callback/close. StateFlow публикация и observer выполняются вне lock.
- Android startup и manifest, Swift Firebase configure/attach порядок, APNs mapping и выключенный swizzling проверены. Firebase data-services transport сохранён.
- Оценка overhead основана на структуре кода и количестве allocations/worker/queue; runtime profiling и измерение энергопотребления не проводились.

## Проверки

- 89 Android host tests и 61 iosSimulatorArm64 tests: failures/errors/skipped = 0. Полный Gradle прогон успешен после пересоздания только generated analytics host-test cache: первый запуск столкнулся с internal Kotlin incremental classpath-diff exception, а не с ошибкой исходников.
- APK Debug: непосредственно в `*/core` только AnalyticsModule, CrashReportingModule и MessagingModule. В merged manifests Debug/Release ровно один MessagingService, exported=false; Analytics default collection/storage и ads consent выключены; debuggable=true только в Debug.
- `:androidApp:assembleDebug`, `:androidApp:assembleRelease`, `:shared:assembleSharedDebugXCFramework`, `:androidApp:lintDebug`, `:androidApp:lintRelease` — BUILD SUCCESSFUL. `:androidApp:uploadCrashlyticsMappingFileRelease` явно исключён. Native Release XCFramework в этом проходе не собирался.
- Lint Debug/Release: 0 errors, 14 warnings на вариант; предупреждения относятся к версиям toolchain/dependencies и launcher resources, вне изменённых сервисов.
- Финальный device Swift header не содержит DefaultAnalytics, DefaultCrashReporting, DefaultMessaging, AnalyticsConfiguration или ApplicationConfigurationModule.
- Финальные `xcodebuild` Debug для iOS Simulator arm64 и generic iOS device, с `CODE_SIGNING_ALLOWED=NO` и актуальным XCFramework: BUILD SUCCEEDED. Приложение не запускалось. `git diff --check` проходит.

Проверки подтверждают локальную интеграцию и компиляцию. Доставка данных в Firebase, APNs на физическом устройстве, provisioning и production symbolication не проверялись; пользователь исключил Firebase runtime-тестирование.
