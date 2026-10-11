# DI и bootstrap сервисов

## Цель

Убрать PlatformServicesModule, PlatformServicesRuntime и AnalyticsRuntime из корня shared. Сохранить core/transport границы и поведение платформ. data-services/firebase не переносится и не удаляется.

## Реализация

- AnalyticsModule, CrashReportingModule и MessagingModule находятся в соответствующих toolkit core, используют существующую koin.annotation.convention и @Module/@Single.
- Koin.kt перечисляет модули, передаёт AnalyticsConfiguration и возвращает ApplicationBootstrap из shared/bootstrap. Bootstrap хранит только три типизированных интерфейса, не контейнер и не конкретные реализации.
- Core реализуют отдельные bootstrap-контракты API; потребители продолжают использовать Analytics/CrashReporting/Messaging.
- MessagingModule захватывает собственный Koin Scope и откладывает разрешение PushTokenSynchronizer до callback. Это сохраняет разрыв authorization ↔ messaging и исключает глобальный lookup.
- Android получает bootstrap при старте; AppDelegate владеет bootstrap на iOS. Firebase.configure остаётся в didFinishLaunching перед подключением адаптеров и установкой Messaging delegate. Swift callback валидируется внутри Messaging core.

## Проверки и ревью

### Саморевью

1. Проверен сгенерированный KSP-код: provider-функции не выводят interface aliases из return type автоматически. Добавлены явные @Single(binds = [...]); тесты подтверждают общие экземпляры consumer/bootstrap/receiver.
2. Проверена изоляция контейнеров: два независимых koinApplication отправляют callback разным fixture handlers. Глобальный контекст для callback не нужен; dependency cycle воспроизводится и успешно разрешается.
3. Проверено влияние тестовой среды: coroutines-test не добавляется в shared, чтобы не менять Main dispatcher существующих Android host tests. Analytics DI проверяется в собственном core/commonTest.
4. Проверены startup ordering и границы: iOS Koin создаётся в AppDelegate.init, SDK подключается после configure и до установки Messaging delegate; root остаётся lazy. ApplicationBootstrap хранит API-интерфейсы. Код очередей/лимитов/dispatcher не изменён.
5. Проверен overhead: нет новых SDK, потоков, worker jobs или callback-coroutines. Используется существующий Koin annotation convention, не менялись версии зависимостей.
6. Swift compile выявил конфликт Objective-C selector у attachSink(sink:) в разных bootstrap interfaces. Параметры названы analyticsSink/crashSink/messagingSink; это создаёт разные selectors и стабильные Swift labels без генерируемых подчёркиваний.
7. Поиск старых Runtime и platformServicesModule в production/test source пуст. data-services/firebase сохранён.

### Проверки

- 79 Android host tests и 51 native iOS tests: 0 failures/errors/skips (analytics API/core, crash core, messaging core; shared host wiring).
- Android assembleDebug/assembleRelease и shared assembleSharedDebugXCFramework: BUILD SUCCESSFUL.
- Android lintDebug/lintRelease: 0 errors, 14 существующих warnings в каждом варианте (версии зависимостей/SDK/icons).
- Финальная Gradle-проверка: /tmp/grippo-bootstrap-final.log, BUILD SUCCESSFUL in 3m 51s.
- Exported Objective-C header проверен: attachSink(analyticsSink:), attachSink(crashSink:), attachSink(messagingSink:), без suffix mangling.
- Swift Debug simulator и unsigned generic iOS device: BUILD SUCCEEDED; логи /tmp/grippo-bootstrap-swift-simulator-final.log и /tmp/grippo-bootstrap-swift-device-final.log.
- Swift warnings совпадают с предыдущими проверками: libicu simulator deployment target 18.5 против app 16.0; AppIntents metadata skipped. Эти зависимости/targets в данном рефакторинге не менялись.
- git diff --check проходит. Старые bootstrap facade/module отсутствуют в исходниках.
- Приложения не запускались; реальные Firebase events/crashes/token retrieval не выполнялись. Android uploadCrashlyticsMappingFileRelease исключён, iOS собирается в Debug без подписи и без symbol upload.

## Воспроизведение

```sh
./gradlew :shared:testAndroidHostTest :toolkit:analytics:core:testAndroidHostTest :toolkit:crash-reporting:core:testAndroidHostTest :toolkit:messaging:core:testAndroidHostTest :toolkit:crash-reporting:core:iosSimulatorArm64Test :toolkit:messaging:core:iosSimulatorArm64Test :toolkit:analytics:core:iosSimulatorArm64Test :androidApp:assembleDebug :androidApp:assembleRelease :shared:assembleSharedDebugXCFramework :androidApp:lintDebug :androidApp:lintRelease -x :androidApp:uploadCrashlyticsMappingFileRelease
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,id=8A3E8CAA-5BCC-4A9C-9FB3-610B71034B0F' -disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=NO build
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS' -disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=NO build
```

Неизменённые analytics API tests также проверены в предыдущем проходе этой задачи на Android и iOS (по 2 теста).
