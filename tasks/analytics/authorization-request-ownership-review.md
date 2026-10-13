# Авторизация: запросы у владельцев фичи

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

## Изменения

Удалены SessionRestoration.kt и SessionPreparation.kt. Проверка сессии и
параллельная загрузка справочников — private методы SplashViewModel. Последовательность
подготовки авторизации выполняется напрямую в LoginUseCase и RegisterUseCase.
Порядок запросов, проверка наличия профиля, nullable push token, исключения,
cancellation и маршруты сохранены. Analytics по-прежнему оборачивает реальные
операции и не выполняет запросы или решения navigation.

Остальные top-level функции авторизации проверены: analytics bindings переводят
типы/сохраняют scenario; Composable функции строят UI. Других вынесенных таким
образом business orchestration helpers в авторизации нет. Новый delegate не добавлен.

Стандарт, skill и coverage уточнены: instrumentation не является причиной
выносить бизнес-запросы из ViewModel/use case в top-level helpers/delegates.

## Проверки

После последних source edits: Gradle BUILD SUCCESSFUL, 2m 38s, exit 0.
Android Debug и Debug XCFramework device/simulator собраны.
Skill frontmatter, active stale helper references и git diff --check проверены.
Прежние Kotlin opt-in/deprecated BackHandler warnings остаются.
Unsigned Swift Debug simulator/device: оба BUILD SUCCEEDED, exit 0.
Logs: /tmp/grippo-splash-owner-swift-simulator.log и
/tmp/grippo-splash-owner-swift-device.log. Simulator destination:
platform=iOS Simulator,id=8A3E8CAA-5BCC-4A9C-9FB3-610B71034B0F;
device: generic/platform=iOS. Обе команды xcodebuild использовали
-project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug
-disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=NO build.
Прежние ICU deployment/AppIntents warnings сохраняются.

```sh
./gradlew :androidApp:assembleDebug :shared:assembleSharedDebugXCFramework
```

Финальный log: /tmp/grippo-splash-owner-confirmed.log.

Firebase отправка, provider sign-in, запуск приложений и mapping/symbol uploads
не выполняются. Изменения сохраняют публичные signatures и wire contract;
выбраны Android Debug, Debug XCFramework и Swift Debug
simulator/device builds. Release/lint повторно не запускаются для этого локального
перемещения неизменных запросов; предыдущий аудит их проверил.
