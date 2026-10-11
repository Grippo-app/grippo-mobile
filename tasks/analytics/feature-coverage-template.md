# Матрица аналитики: <Feature>

Статус: proposal / implemented / verified. Owner module: <path>. Catalog module/qualifier: <module>/<name>. Verification report: <path>.

## Контекст сценария

| Точка входа | Реальный триггер | originScreen | Где создаётся/сохраняется FlowContext |
|---|---|---|---|
| <typed entry> | <observed fact> | <known screen or null> | <owner> |

## События и исходы

| Триггер | Typed event / wire name | Owner / точное место | Параметры и типы | Flow / operation | Outcomes | Тест |
|---|---|---|---|---|---|---|
| <action/activation> | <concrete class / stable name> | <single owner> | <required/optional typed values> | <new/reused ID> | <success/failure/cancel/interruption> | <fixture/scenario> |

## Поверхности и переходы

| Leaf screen / dialog | ScreenId / DialogId | Referrer и источник reason | Visibility binding | Back / restore / external test |
|---|---|---|---|---|
| <surface> | <constant> | <navigation owner> | <single binding> | <test> |

## Явные исключения из tracking

| Действие/поле | Почему не собирается |
|---|---|
| <typing, sensitive field, container> | <reason> |

## Приёмка

- [ ] У каждого факта один owner, у каждого result точный смысл business success.
- [ ] Строгие события и причины; все фабрики operation events реализованы.
- [ ] Annotated qualified definitions зарегистрированы через .module; fixtures прошли.
- [ ] Flow переносится и восстанавливается; повторные попытки получают новые operation IDs.
- [ ] Referrer/reason достоверны, показ только у leaf; foreground policy соблюдается.
- [ ] Errors/cancellation, параллельные flows, retries, consent и account switch проверены.
- [ ] Личные/чувствительные значения исключены; нет свободных exception messages.
- [ ] Android/iOS проверки прошли, ограничения записаны.
