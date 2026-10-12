# Стандарт интеграции аналитики в skills — 2026-10-12

Canonical source: .claude/skills/feature-analytics/SKILL.md. Полная перепись вместо
append-only дополнений. Linked references/ui-provider.md содержит детали UI wiring,
DI, typed retention, persistence и meaningful tests, без нового runtime framework.

Закреплены one feature entry point + nested scenario/classifier ports, sibling
models, internal implementation responsibility folders, sealed event families,
structured schemas/codes, exact typed trackOperation results, original business
owners/error jobs, consent/privacy, attribution/lifecycle/version compatibility.
UI standard: analytics/AnalyticsProvider.kt (internal stateless class through @Single),
separate classifier file, private companion persistence keys, explicit type-key
retainedSimpleInstance, analytics naming и same instance propagated to children/VM.
Нет shared feature extensions/base analytics components/custom holders. Snapshot
и classifier добавляются по действительной потребности, не как пустая обвязка.

UI skill, DI references, CLAUDE и feature-standard.md синхронизированы. AGENTS уже
обязывает применять canonical analytics skill при изменениях поведения и остаётся
источником автоматического применения. Invocation policy allow_implicit_invocation
сохранена. Older review reports remain historical; active guidance не ссылается
на удалённые retention helpers или root provider placement.

Self-review по actual provider/classifier/module/components/API и двум сценариям:
новая feature operation (own descriptor/result/schema + supplied existing business
block) и изменение route (provider mapping/FlowId/snapshot/coverage/lifecycle).
Инструкции не переносят бизнес-код в analytics и не дают permissions на Firebase.
User limits overrides default build/test requirements; static checks не объявляются
compilation/KSP/runtime verification.

Проверки: Ruby YAML frontmatter/name/description/interface/implicit-policy checks,
Python relative links/fences/obsolete examples/source alignment (7 documents),
git diff --check — exit 0. Bundled quick_validate.py не выполнился из-за отсутствия
PyYAML; frontmatter ограничения проверены отдельно через Ruby YAML + explicit checks,
зависимости не устанавливались. App source в этом изменении не менялся;
Android/iOS builds/tests не запускались.
