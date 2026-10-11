# Глубокое ревью и чистка сервисов

## Проверенная область

Analytics, CrashReporting, Messaging API/core; Android и Swift adapters; DI/bootstrap; consumer dependencies; документация архитектуры. data-services/firebase сохранён, фичи не расширялись.

## Исправления

1. AnalyticsCatalog строил lookup tables до проверки общего размера и параметров. Теперь сначала проверяются лимиты/schema, затем строятся таблицы. Regression test с oversized AbstractList до фикса падал на чтении элемента, после — отклоняет вход до чтения. Duplicate keys проверяются одним HashSet; requiredNames строятся без промежуточного filter list.
2. Analytics wake.trySend выполнялся под state lock. Immediate dispatcher мог исполнить SDK callback под lock и блокировать другой поток. Host regression test воспроизвёл проблему, затем прошёл. Во всех producer methods wakeup вынесен за lock, worker читает актуальное состояние.
3. Messaging StateFlow observer мог синхронно вызвать следующий token callback; старый callback после этого перезаписывал syncToken. Regression test сначала передал в handler старый token. Последний sync request и канонические token/sync states теперь записываются атомарно под lock, а StateFlow публикуется после освобождения lock. CAS retry повторно читает каноническое состояние и не возвращает уже опубликованное новое состояние к старому.
4. Messaging использует conflated Unit wakeup, token хранится только в latest callback state. Worker читает его под lock. Переупорядоченные wakeups не могут заменить новый token старым. Во втором проходе regression test выявил неявный retry при старом wakeup: latest Available instance теперь служит идентичностью callback request и повторно используется в StateFlow (дополнительный wrapper не создаётся). Worker обрабатывает один request один раз; новый callback того же token после failure может явно повторить попытку. Handler запускается вне producer lock; сохраняются dedup/serial handler/failed status/cancellation и latest-token-wins.
5. Дополнительный host test выявил внешний recursive lock при reentrant StateFlow observer, даже после вынесения wakeup. MutableStateFlow.update публикует состояния вне core lock; внутреннее состояние остаётся под lock, без дополнительного worker/channel и без дополнительных result wrappers. Worker completion использует identity конкретного request.
6. Удалён PushTokenReceiver: Android service и Swift callback используют один MessagingBootstrap.onRegistrationToken. Callback декодируется только в core; исчезли duplicate validation/неосуществимая Unavailable branch/лишний Available wrapper. Типизированный PushTokenDecoder для SDK getToken сохранён.
7. RecordingAnalyticsSink перенесён из commonMain в commonTest и стал internal fixture. Он отсутствует в production artifacts.
8. Analytics.close очищает adapter reference, user identity и navigation context; wake channel отменяется после join. SDK disable сохраняется, in-flight SDK call по-прежнему не отзывается.
9. CrashContext.Empty повторно используется для reports без metadata. Проверка уникальности attributes не строит промежуточные map/distinct lists. Android adapter без metadata вызывает SDK overload recordException(Throwable), который по исходникам установленного Firebase Crashlytics 20.1.1 передаёт Map.of() непосредственно в core; CustomKeysAndValues.Builder/его mutable map не создаются.
10. Firebase adapter API dependencies перенесены из commonMain в androidMain; unused прямой Firebase Messaging dependency androidApp удалён (transport сохраняет SDK). У crash/messaging API без tests убран пустой test convention. Версии библиотек не менялись.
11. CLAUDE.md и integration guides очищены от активных инструкций использовать FirebaseProvider/legacy runtime/preview recording sink. История предыдущих ревью не переписывалась.

## Что сохранено намеренно

- По одному app-lifetime worker на сервис. SDK IO нельзя возвращать в caller, и усложнять lifecycle ради незамеренного выигрыша от lazy jobs оснований нет.
- Bounded queues, revision guards, cancellation propagation, NonCancellable close, defensive snapshots и explicit SDK consent handling.
- Локальные platform locks. Для трёх небольших примитивов не добавлен отдельный generic concurrency module. State mutations защищены lock; все Flow notifications и SDK/handler calls теперь выполняются вне него. Сам Foundation primitive не менялся ради незамеренного выигрыша.
- Deferred PushTokenHandler resolution в своём Koin Scope: authorization всё ещё зависит от Messaging, eager injection создаст цикл.
- Analytics SDK policy-recovery после конфигурационного сбоя — явное fail-closed поведение, не скрытая подмена результата. SDK getToken failures остаются типизированными, optional push policy авторизации не менялась.
- Android incoming-notification projection и все product feature flows сохранены.

## Ревью-луп

Проход 1: публичные границы и usages → callback/fixture/dependency cleanup.
Проход 2: allocations/lifecycle → early catalog bounds, default empty crash context, SDK empty metadata path, release adapter/identity.
Проход 3: concurrency/reentrancy → четыре воспроизведённых concurrency regression failures (включая regression нового wakeup refactor), исправления, focused green suite.
Проход 4: полный platform verification, APK/manifest/header scan и финальный source/dependency scan завершены.

## Проверки

- 84 Android host tests и 54 native iOS tests: 0 failures/errors/skips.
- Финальный Gradle run: /tmp/grippo-cleanup-final-verification.log, BUILD SUCCESSFUL in 1m 54s. Android Debug/Release, native tests и shared Debug XCFramework успешно собраны после финального Messaging refactor.
- Swift Debug simulator: /tmp/grippo-cleanup-swift-simulator-final.log, BUILD SUCCEEDED.
- Swift Debug unsigned device: /tmp/grippo-cleanup-swift-device-final.log, BUILD SUCCEEDED.
- lintDebug/lintRelease: 0 errors, 14 прежних warnings (versions/SDK/icons) в каждом варианте. Swift warnings прежние: ICU simulator deployment target и отсутствие AppIntents metadata.
- Debug/Release APK dex не содержат RecordingAnalyticsSink и PushTokenReceiver. Merged manifests содержат ровно один com.grippo.services.firebase.MessagingService, exported=false.
- Debug dex method tables не содержат ссылок java.util.Map.of/ofEntries/copyOf: проверен скомпилированный SDK empty-metadata путь для приложения с minSdk 26.
- git diff --check проходит. Активные source/CLAUDE.md не содержат legacy FirebaseProvider/Runtime/receiver/impossible decoder branch; RecordingAnalyticsSink встречается только в commonTest.
- Runtime Firebase send/crash/token retrieval не выполнялись; mapping upload исключён из Gradle, Swift Debug symbol upload не выполнялся. Приложения не устанавливались и не запускались.

Regression red logs: /tmp/grippo-cleanup-red.log (early catalog bound), /tmp/grippo-cleanup-lock-red.log (analytics callback under lock), /tmp/grippo-cleanup-reentrant-red.log (stale token), /tmp/grippo-cleanup-wakeup-red.log (implicit retry), /tmp/grippo-cleanup-outer-lock-probe.log (reentrant outer lock). Все соответствующие regression tests прошли в финальных suites.

## Воспроизведение

```sh
./gradlew :toolkit:analytics:api:testAndroidHostTest :toolkit:analytics:core:testAndroidHostTest :toolkit:crash-reporting:core:testAndroidHostTest :toolkit:messaging:core:testAndroidHostTest :shared:testAndroidHostTest :toolkit:analytics:api:iosSimulatorArm64Test :toolkit:analytics:core:iosSimulatorArm64Test :toolkit:crash-reporting:core:iosSimulatorArm64Test :toolkit:messaging:core:iosSimulatorArm64Test :androidApp:assembleDebug :androidApp:assembleRelease :shared:assembleSharedDebugXCFramework :androidApp:lintDebug :androidApp:lintRelease -x :androidApp:uploadCrashlyticsMappingFileRelease
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,id=8A3E8CAA-5BCC-4A9C-9FB3-610B71034B0F' -disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=NO build
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS' -disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=NO build
```
