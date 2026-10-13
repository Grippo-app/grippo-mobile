# Platform utility behavior

Inspect common API and both platform implementations in the affected toolkit module, plus its shell
or feature consumer. Do not invent replacement APIs from copied platform examples.

- Connectivity, messaging and notification subscriptions have explicit owners; preserve startup,
  disposal, callback thread and foreground/background semantics.
- Permission and link-opening outcomes are platform facts; features own resulting business decisions.
  Do not model a denied/cancelled outcome as successful completion.
- Theme/localization/image loading use existing design-system/shared integration. Preserve locale
  changes and resource-backed UiText; no new CompositionLocal outside the allowed resource/core owners.
- Android Context/Activity access goes through existing platform adapters; common code stays portable.
- Service callbacks do not log push/provider tokens or send telemetry payloads during verification
  without authorization for those external effects.

For feature behavior apply analytics/state preservation through their canonical skills. Source review
cannot prove OS permission UI, push delivery or background lifecycle; list unrun manual scenarios.
