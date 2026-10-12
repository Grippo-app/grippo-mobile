# ScreenAnalytics API/implementation — 2026-10-12

Public analytics/ScreenAnalytics содержит единственный bind(Lifecycle, present).
Internal internal/analytics/ScreenAnalyticsImpl @Single(binds=[ScreenAnalytics])
обнаруживается существующим CoreModule @ComponentScan. DI модуль/ручной DSL
в production не добавлены. Singleton stateless: callback и presentation локальны
каждому bind; resume получает актуальную presentation, pause/destroy закрывает,
destroy отписывает. Reporting Exceptions изолированы; destroyed-lifecycle
invariant и fatal Error не маскируются.

Удалены обе lifecycle extensions и overload navigation/flow. Login/Credential/
Splash bind через getKoin().get<ScreenAnalytics>(); feature analytics не зависит
от foundation implementation. RetainedAnalytics не изменён и не зарегистрирован
в DI. Тестовые исходники lifecycle/error/current facts адаптированы; добавлена
generated DI singleton проверка двух независимых owners. AuthIntegrationTest
использует actual CoreModule binding. Скиллы/стандарт/guide/coverage синхронизированы.

Self-review API/visibility/DI scope/callback state/unsubscribe/error boundaries:
статические проверки API/internal annotations, трёх leaf calls, удаления старых
символов, YAML, docs links/fences и git diff --check прошли. Сборки Android/iOS,
KSP и тесты не запускались; compilation/generated DI/runtime не подтверждены.
