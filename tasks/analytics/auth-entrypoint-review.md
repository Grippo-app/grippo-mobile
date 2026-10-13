# Один consumer entry point авторизации

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

## Итоговый контракт

`analytics/analytics-api/.../authorization/AuthAnalytics.kt` — единственный
верхнеуровневый consumer interface авторизации. DI возвращает stateless
AuthAnalytics; authorization()/session() создают независимые mutable scenarios.
Authorization, Session, enums, results, FailureClassifier и AuthorizationState
вложены в AuthAnalytics. Общие toolkit ID/context/presentation контракты используются
в необходимых сигнатурах без дополнительных wrappers.

AuthAnalyticsImpl — @Single; AuthorizationAnalyticsImpl и SessionAnalyticsImpl
internal и scenario-scoped. У каждого сценария свой набор допустимых методов.
Отдельный публичный Factory, AuthSurface, общий AuthNavigation, scenario mode/casts
и старые API files удалены. Авторизация использует Step для persistence и views;
session.presentScreen() не принимает чужие поверхности. Обоим сценариям принадлежит
save(). Annotated module остаётся публичным для shared composition root.

## Self-review и повторный проход

Проверены API visibility/DI lifetime, все consumers и StateKeeper bindings,
flow identity/attribution, callbacks/operation factories, privacy, cancellation,
error identity, compatible failure/outcome matrices и platform bootstrap wiring.

- Сценарии не singleton; shared factory не хранит mutable scenario state.
- Не осталось runtime проверок неправильного режима: соответствующих методов нет
  в контрактах.
- Сохранены 17 wire schemas, enum codes, StateKeeper keys, snapshot fields и
  legacy AuthFlowState SerialName. Архивы StateKeeper
  проходят после перемещения в nested contract. Route FlowId invariant сохранён.
- Analytics layer не импортирует UI/domain/services/SDK. UI main зависит только
  от analytics-api; implementation подключается shared.
- Scenarios оборачивают исходные business callbacks; scopes, retries, SDK calls,
  routing и дополнительная collection policy ради этого refactor не добавлялись.
- В повторном проходе исправлены конфликт EntryPoint/import DI module и suffix Impl
  у внутренних scenario implementations. После последней source правки выполнен
  свежий полный Gradle run и повторные Swift builds.
- Standard, skill, UI skill, coverage, integration guide и CLAUDE.md актуализированы.
  Исторические отчёты предыдущей структуры сохранены как история.

## Свежие проверки

Полный Gradle run: BUILD SUCCESSFUL, 2m, exit 0,
`/tmp/grippo-auth-entrypoint-full.log`:

```sh
./gradlew :androidApp:assembleDebug :androidApp:assembleRelease \
  :shared:assembleSharedDebugXCFramework :androidApp:lintDebug :androidApp:lintRelease \
  -x :androidApp:uploadCrashlyticsMappingFileRelease
```

Android Debug/Release и Debug XCFramework device/simulator собраны.
Lint: 0 Error/Fatal, по 14 прежних warnings в Debug/Release.
Оба Debug headers проверены по 24 internal declarations: утечек нет.
Skill YAML/frontmatter, references/code fences, obsolete API/import scans,
single top-level consumer contract и git diff --check проверены.
Штатный Python skill validator недоступен без PyYAML; YAML проверен Ruby parser.

Unsigned Swift Debug simulator: BUILD SUCCEEDED, exit 0,
`/tmp/grippo-auth-entrypoint-swift-simulator.log`.
Unsigned Swift Debug device: BUILD SUCCEEDED, exit 0,
`/tmp/grippo-auth-entrypoint-swift-device.log`.
Сохранены baseline Swift linker/AppIntents и Kotlin opt-in/deprecation warnings.

Firebase runtime sends, app launch, provider sign-in и mapping/symbol uploads
не выполнялись. Сборки и static review не являются performance profiling.
