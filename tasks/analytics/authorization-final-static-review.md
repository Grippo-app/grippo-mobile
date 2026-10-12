# Авторизация и аналитика: повторный статический аудит — 2026-10-12

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

## Исправления

- AnalyticsProviderTest использовал expected.analyticsCode у consumer EntryPoint,
  который не реализует AnalyticsCode. Fixture заменён на независимые ожидаемые
  wire strings; API не расширялся ради теста. Статический прошлый аудит не выявил
  этот type-level дефект; компиляция текущего кода по-прежнему не проверялась.
- AuthFlow повторно записывал hard-coded Login→Registration / Registration→Login
  даже при уже текущем шаге и после terminal exit. Добавлены короткие idempotence
  guards: повторный/закрытый переход не сбрасывает referrer/reason и не создаёт
  несуществующее StepChanged. UI routing не переносился в analytics; schema meaning
  остаётся фактом реальной смены шага. Добавлен regression test для этих случаев.
- Удалены test convention у analytics-api без tests и неиспользуемая прямая
  coroutines-core dependency у analytics/authorization. Toolkit dependency и
  coroutine test dependency сохраняют нужные потребители; Gradle configuration
  не запускалась. Удалены unused explicit imports и wildcard flow import в test.
- Исправлено formatting consumer EntryPoint, AuthSessionEvent и classifier.
  Необходимый Compose getValue operator import сохранён: текстовый поиск не
  доказывает неиспользование implicit operator/delegate imports.
- Coverage matrix консолидирована в описание текущего состояния вместо накопленных
  append-only изменений. Skill/standard уточняют idempotent transition behaviour и
  пределы static import/type checks. Исторические отчёты не переписывались.

## Рассмотренная интеграция

API/models, scenario factories, internal flows/events/schemas/operation factories,
UI AnalyticsProvider/classifier/annotated module, owner/leaf component ordering,
Login/Credential/Splash ViewModels, existing Login/Register use cases, auth routing,
profile leaf structure, generated module registration source, tests и active skills.

Сохранены closed typed AuthOperation/AuthTransition, required exit reason,
trackOperation/presentScreen separation, bounded classifier без message parsing,
existing safeLaunch/error/cancellation pipeline, provider statelessness,
explicit type-key retention и private companion persistence keys. Provider не
хранит UI context/scenario; owners передают тот же analytics детям и retained VM.
FlowId checks, legacy serialization и 16 schema versions/wire codes сохранены.

Обвязка имеет конкретные роли: provider — UI persistence/lifetime boundary,
classifier — SDK/domain boundary, scenario — typed telemetry contract, flow —
mutable attribution. Новых delegates/workers/scopes/retries/registries не добавлено.
No-build ограничение не даёт оснований заявлять compilation/KSP/runtime correctness
или нулевой overhead; текущие assertions ограничены просмотром и static checks.

## Проверки

Static checks выполнены после исправлений: Kotlin delimiters/import usages,
actual EntryPoint wire fixture alignment, repeated/closed transition guards,
consumer API migration, typed descriptors, provider boundaries/order/keys/DI,
schema fixture names/versions/key order, module dependencies и source convention
правила; YAML/frontmatter/metadata, relative links/fences/example alignment,
git diff --check. Результаты команд — exit 0.

Tests обновлены, но не выполнялись. Android/iOS compilation/builds, KSP, lint и
runtime apps/provider/Firebase delivery не запускались по указанию пользователя.
