# Повторный аудит AuthAnalytics и авторизации

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

## Исправление

ViewModel Login/Registration/Splash сохранялась через InstanceKeeper, а UI binding
каждый раз создавал новый mutable analytics scenario. При пересоздании component
с сохранённой ViewModel операции продолжали использовать старый scenario,
тогда как navigation, screen presentation и новый StateKeeper сохраняли новый.
Для авторизации это также могло повторить auth_flow_started.

AuthAnalyticsBindings теперь удерживает scenario в InstanceKeeper владельца
под отдельным стабильным ключом. Новый StateKeeper регистрирует save того же
экземпляра; его актуальное состояние имеет приоритет перед старым snapshot.
Без retained instance scenario создаётся из StateKeeper snapshot, включая
восстановление после завершения процесса. Retained Authorization дополнительно
проверяет соответствие FlowId маршруту. Несовпадение отклоняется без нового события.

Обёртка private, без ресурсов, jobs, workers, locks или SDK dependencies.
Ключи StateKeeper, публичный API, wire schemas/codes и JSON совместимость сохранены.
Ownership rule добавлено в feature-standard, canonical skill и coverage matrix.

## Self-review и второй проход

Проверены public API, stateless DI entry point, независимость сценариев,
route identity, восстановление snapshot, leaf visibility, navigation attribution,
form validation/privacy, stage/outcome matrices, provider cancellation,
coroutine interruption, сохранение исходных исключений, retry operation IDs,
отделение confirmed authentication от последующей preparation, session decisions,
shared DI и зависимости Android/iOS. Дополнительных подтверждённых дефектов
в этих частях не найдено. Исторический SerialName является persistence contract,
а не забытым старым типом. Нет новых публичных aliases/factories или fallback.

Новые regression tests:
- componentRecreationRetainsScenarioAndSavesItsLatestState: сохранённый scenario
  общий для нового component и старых consumers, продолжение mutations,
  актуальный StateKeeper snapshot, отсутствие повторного flow start/exit,
  сохранённый Session scenario.
- retainedAuthorizationCannotBeReboundToAnotherRoute: новый route FlowId не может
  использовать прежний retained scenario; новое событие не появляется.

## Проверки

Финальный Gradle run после последних source/test правок: BUILD SUCCESSFUL,
2m 5s, exit 0. Log: /tmp/grippo-auth-final-audit-confirmed.log.

```sh
./gradlew :analytics:authorization:testAndroidHostTest \
  :analytics:authorization:iosSimulatorArm64Test \
  :ui-screen-features:authorization:testAndroidHostTest \
  :ui-screen-features:authorization:iosSimulatorArm64Test \
  :shared:testAndroidHostTest :androidApp:assembleDebug :androidApp:assembleRelease \
  :shared:assembleSharedDebugXCFramework :androidApp:lintDebug :androidApp:lintRelease \
  -x :androidApp:uploadCrashlyticsMappingFileRelease
```

| Набор | Android host | iOS simulator ARM64 | Failures/errors/skips |
|---|---:|---:|---:|
| Analytics authorization | 23 | 23 | 0 |
| UI authorization integration | 17 | 17 | 0 |
| Shared | 24 | не запускался | 0 |

Android Debug/Release и Debug XCFramework device/simulator собраны.
Lint: 0 Error/Fatal, по 14 прежних warnings в Debug/Release.
В обоих XCFramework headers отсутствуют internal scenario implementations,
execution/state helpers и новый private RetainedAnalytics.
Skill YAML/frontmatter, Markdown fences, obsolete API/import scans и git diff
--check проходят. Unsigned Swift Debug builds: оба BUILD SUCCEEDED, exit 0.

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination 'platform=iOS Simulator,id=8A3E8CAA-5BCC-4A9C-9FB3-610B71034B0F' \
  -disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=NO build
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination 'generic/platform=iOS' \
  -disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=NO build
```

Logs: /tmp/grippo-auth-deep-swift-simulator.log и
/tmp/grippo-auth-deep-swift-device.log. Прежние предупреждения linker ICU deployment
и AppIntents metadata не препятствуют сборке. Kotlin предупреждения opt-in и
deprecated BackHandler также остались прежними.
Firebase отправка, запуск приложений/provider sign-in и mapping/symbol uploads
не выполняются. Проверки подтверждают код, тестовые сценарии и сборочную
интеграцию; не измеряют runtime performance и доставку в Firebase.
