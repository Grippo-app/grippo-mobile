# Упрощение подготовки авторизации

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

Статус: реализовано, self-review и проверки завершены.

## Изменение границы

Удалены AuthorizationPreparationExecution/Stage/Outcome и поэтапная обвязка.
LoginUseCase.prepare() и RegisterUseCase.prepare() не знают об аналитике и
возвращают Boolean наличия профиля. SessionPreparation.kt — внутренняя общая
бизнес-политика без telemetry callbacks: профиль → условная загрузка данных →
nullable push token → регистрация при наличии. Наборы запросов Login/Register,
порядок, propagation ошибок и best-effort отсутствие push token сохраняются.

AuthAnalytics остаётся единственным фасадом. Подготовка измеряется целиком,
начальная загрузка splash — самостоятельной операцией. Внутренние этапы ошибок
и push outcome не выдумываются. Подтверждённый auth success не переписывается
при поздней ошибке подготовки. Семейство auth_setup_* заменяет auth_preparation_*;
точное решение о совместимости записано в authorization-coverage.md.

Стандарт и skill обновлены: feature-api не получает telemetry execution ports.
Прежние authorization-audit.md и authorization-integration-review.md являются
историческими отчётами предшествующей реализации; текущая матрица обновлена.

## Self-review

- Корректность: profile getOrThrow → условные data requests → nullable push → updatePushToken getOrThrow; Login сохраняет weight history, Registration её не загружает. Failure прекращает последующие запросы. Никакой новой retry политики.
- Граница: feature-api не импортирует analytics и не вызывает telemetry callbacks. Общая функция подготовки internal и содержит только бизнес-политику. Stateful AuthExecution больше не реализует domain interface.
- Строгость: Login/Registration принимают только ProfilePresent/ProfileRequired, InitialData — Completed; невозможный kind отклоняется до business block и event emission. Failure не сообщает ненаблюдаемый внутренний stage.
- Ошибки/отмена: общий trackOperation сохраняет original error/cancellation, start/terminal и fresh IDs; auth confirmation остаётся отдельным фактом. Parallel splash requests сохраняют coroutineScope и отмену sibling.
- Чистота/расходы: удалены четыре telemetry файла, mapping domain→analytics и отдельные operations на каждый шаг. Нет дополнительных jobs, scopes, callbacks в use cases, dual-write или fallback событий. Runtime profiling не выполнялся.
- Приватность: новые payload содержат только kind/outcome/duration/category; credentials, push tokens и exception messages не добавлены.

## Проверки

Финальный Gradle прогон: `/tmp/grippo-auth-simple-complete.log`.
Команда:

```sh
./gradlew :ui-screen-features:authorization:testAndroidHostTest \
  :ui-screen-features:authorization:iosSimulatorArm64Test \
  :shared:testAndroidHostTest :androidApp:assembleDebug \
  :androidApp:assembleRelease :shared:assembleSharedDebugXCFramework \
  :androidApp:lintDebug :androidApp:lintRelease \
  -x :androidApp:uploadCrashlyticsMappingFileRelease
```

42 auth tests Android host и 42 iOS simulator ARM64: 0 failures/errors.
Матрица outcome проверена для всех допустимых и запрещённых kind/outcome пар.
24 shared Android host tests: 0 failures/errors.
Gradle: BUILD SUCCESSFUL, 6m54s, exit 0. Android Debug/Release и shared Debug
XCFramework для device/simulator собраны. Lint Debug/Release: 0 Error/Fatal,
14 прежних warnings в каждой конфигурации. Обе shared.h не экспортируют удалённые
контракты или внутренние AuthExecution/AuthPreparationKind/AuthPreparationOutcome.

Swift unsigned Debug simulator: BUILD SUCCEEDED, exit 0,
`/tmp/grippo-auth-simple-swift-simulator.log`.
Команда: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,id=8A3E8CAA-5BCC-4A9C-9FB3-610B71034B0F' -disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=NO build`.

Swift unsigned Debug device: BUILD SUCCEEDED, exit 0,
`/tmp/grippo-auth-simple-swift-device.log`.
Команда аналогична, destination `generic/platform=iOS`.
iOS Release не запускается, чтобы не инициировать symbol upload.

Firebase sending,
provider sign-in, mapping/symbol upload и запуск приложения не выполняются.

Swift builds сохранили baseline warnings libicu deployment target и AppIntents metadata.
`git diff --check` и проверка хвостов Kotlin sources прошли.
