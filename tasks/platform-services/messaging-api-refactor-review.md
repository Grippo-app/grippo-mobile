# Messaging API refactor review

## Изменения

- Consumer facade: `tokenState`, `tokenSyncStatus`, `fetchToken()`.
- SDK boundary: `PushTokenProvider.fetchToken()` и `PushTokenParser.parse(rawToken)`.
- Backend boundary: `PushTokenSynchronizer.syncToken(token)`; authorization implementation
  переименована и привязана через `@Single(binds = [...])`.
- Startup: `MessagingBootstrap.attachTokenProvider(tokenProvider)` и `onTokenChanged(rawToken)`.
- `PushTokenState`, `PushTokenFailure`, `PushTokenUnavailableReason`, `PushTokenSyncStatus`
  описывают результаты и причины без строковых диспетчеров или compatibility aliases.
- Публичные типы разделены по token/state/synchronization/transport/lifecycle.
  Внутренние token rules и core implementation имеют Kotlin internal visibility.
- Матрица контрактов/состояний: integration-guide.md. Лимит 4096 и redaction description
  централизованы в PushTokenRules; валидный token сканируется один раз.

## Self-review loop

1. Проверены consumer/provider/synchronizer responsibilities и все Kotlin/Swift call sites.
   Исправлены startup labels и flattened Swift names вложенных sealed типов.
2. Проверены validation и privacy: token не trim/normalize, исключение валидации не
   содержит значения, toString redacted, дополнительных payload logs нет. Убрана
   повторная валидация parser → constructor и неиспользуемые imports.
3. Проверен diff state machine: единственный serial worker, conflated Unit wakeups,
   identity callback request, dedup успешного callback, отсутствие implicit retries.
   Fetch не регистрирует backend; revision защищает новое callback/closed состояние.
   SDK/synchronizer и Flow observers выполняются вне state lock. Cancellation caller
   сохраняется; close отменяет worker через NonCancellable cleanup.
4. Проверен DI: deferred synchronizer использует instance-bound Koin scope для
   разрыва authorization ↔ messaging цикла; generated module и singleton aliases сохранены.
5. Сохранены empty/oversized/whitespace/control rejection и единая
   классификация parser/constructor.

`tokenSyncStatus` относится к последнему валидному callback, а не к последнему fetch.
Invalid callback публикует Failed tokenState, но не отменяет уже начатый valid sync.
Synced означает завершение synchronizer, а не remote durable delivery/session ownership.
Эти semantics сохранены; notification permissions/rendering и новые feature hooks не добавлены.

## Проверки

- Gradle full build: BUILD SUCCESSFUL, 3m 43s; Android assembleDebug/assembleRelease
  и shared Debug XCFramework device/simulator.
  Log: `/tmp/grippo-messaging-refactor-full.log`.
- Lint Debug/Release: 0 errors/fatal, 14 existing warnings в каждой конфигурации.
- Xcode Debug iOS Simulator: BUILD SUCCEEDED.
  Log: `/tmp/grippo-messaging-refactor-swift-simulator.log`.
- Xcode Debug generic iOS device: BUILD SUCCEEDED.
  Log: `/tmp/grippo-messaging-refactor-swift-device.log`.
- Generated Objective-C headers обоих XCFramework slices содержат новые public types,
  `attachTokenProvider(tokenProvider:)`, `onTokenChanged(rawToken:)`. PushTokenRules,
  DefaultMessaging, DeferredPushTokenSynchronizer, MessagingLock не экспортируются.
- `git diff --check`: passed.

Существующие SDK/проектные предупреждения не скрывались suppression: Gradle отмечает
FCM SDK token/service deprecations; simulator linker предупреждает о libicu object,
собранном для 18.5 при deployment target 16.0; AppIntents extraction skipped.
Новые версии SDK или deployment target в этом рефакторинге не менялись.
Firebase Console, реальные token requests, push delivery и runtime launches не используются.
Android Crashlytics mapping upload исключён; Xcode builds только Debug без code signing.
Независимое внешнее ревью не заявляется; выполнено два прохода self-review.
