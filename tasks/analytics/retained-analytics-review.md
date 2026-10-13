# Общий механизм удержания аналитики — 2026-10-12

> Исторический отчёт: описывает реализацию и проверки этого этапа, включая
> позднее заменённые API/provider/retention. Текущие правила —
> [feature-standard](feature-standard.md), фактическое покрытие и scope —
> [авторизация](authorization-coverage.md). Результаты ниже не подтверждают
> последующие изменения.

Добавлен public RetainedAnalytics в UI foundation. provide<T, S> public inline
reified T; все callbacks crossinline, context @PublishedApi internal. Механизм
consume → retainedSimpleInstance<T> → validate → register сохраняет type-key
сценария, latest save и отказ при нарушении identity без fallback replacement.

AuthAnalyticsProvider использует его для Authorization/AuthorizationState и
Session/FlowContext. Mapping, ID conversion/check и прежние StateKeeper keys
остаются private/feature-owned. Helper локальный и owner-bound, не DI singleton.
Foundation экспортирует serialization-core из-за public KSerializer; json остаётся
implementation. Business/core/route contracts не менялись.

Self-review: inline visibility, exact contract type, captures/owner lifetime,
callback ordering, serializer bounds, dependency API, private keys и skill/examples.
Статические проверки helper/provider/keys/dependency, YAML и git diff --check
прошли. Сборки Android/iOS и KSP не запускались; компиляция/runtime
не подтверждены. Общих extensions/base components или дополнительных factory нет.
