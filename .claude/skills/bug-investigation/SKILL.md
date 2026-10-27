---
name: bug-investigation
description: Investigate and fix a reported Grippo defect - capture the report, reproduce on emulator/simulator, trace through the MVI/data/network/persistence layers with the app's logs, state the root cause with evidence, fix at the owning layer under its skill, and re-run the reproduction. Use for any bug report, crash, wrong data, broken navigation, lost draft or failed request.
---

# Bug investigation

Establish the root cause from runtime evidence or a traced source path. This
skill connects the report to its owner and verifies the correction with the
checks available under the project's verification policy.

## 1. Capture the report

Record, in the reply as you go: symptom, exact steps, platform and OS version,
build/revision, account state (logged in, fresh install, restored), logs,
screenshots or Crashlytics stack if the user supplied them. Ask only for a fact
that blocks reproduction; otherwise proceed with the most likely reading and
say which.

## 2. Reproduce

Apply the [runtime authorization policy](../../../CLAUDE.md#verification-policy)
and use [runtime-verification](../runtime-verification/SKILL.md) on the authorized
platform. Capture the failing state with a screenshot and the log window. When
runtime access is unavailable or declined, trace the source path, label the root
cause as source-inferred and state which reproduction remains unverified.

## 3. Locate the owner

Trace the surface to its code, layer by layer, and check the usual suspects:

| Layer | Inspect | Usual suspects |
|---|---|---|
| Screen / Contract | `*Screen.kt`, `components/` | wrong state branch rendered, callback not wired, loader never shown |
| ViewModel | `*ViewModel.kt` | `update` lost by capturing `state.value` before a suspend; `Flow.safeLaunch` paused off-screen (1 s grace); failure hidden outside the shared error boundary; navigation collapsed by the conflated channel |
| Component / navigation | `*Component.kt`, `screen-api` routers, `RootComponent` | Direction branch missing; callback not threaded through `createChild`; result key mismatch; non-serializable route payload |
| Dialogs | `DialogConfig`, `DialogContentComponent`, `DialogViewModel` | `hasSameContentAs` rejecting a legitimate re-open; `onResult` on a stale step; session closed by a second `open` |
| Drafts / restoration | `DRAFT` `PersistenceKey`, `@Transient` fields, `ApplicationStateStore.swift` | field not serialized, default recomputed on restore, archive format mismatch, data-backed observer overwriting the draft |
| Error pipeline | `BaseViewModel.sendError`, `ErrorProviderImpl`, `AppError` | wrong `AppError` mapping, `mapError` hiding the original, `ErrorRecovery.onClose` expected to run immediately or to retry, Retry repeating an intent event or a create that already succeeded (needs `ErrorRecovery.Close`) |
| Domain / repository | `*FeatureImpl`, `*RepositoryImpl`, `operation-outcomes` rules | cache written before server success, range reconciliation deleting rows, partial hydration treated as success |
| Network | `BackendClient`, `TokenProvider`, `ResponseValidator` | 4xx mapped to `Expected`, refresh retry/timeout path (timeout is a cancellation, tokens kept), missing `@EncodeDefault` so a field is omitted, wrong `@SerialName` |
| Mapping | `data-mappers/*` | `[MAPPING] ... is null` lines in the log: a required field dropped the row |
| Persistence | Room entities, DAOs, `DatabaseMigrations.all` | migration missing for a schema delta (destructive fallback wiped data), UTC conversion, foreign-key cascade |
| Platform | `expect`/`actual`, shells, permissions, notifications | Android-only or iOS-only actual, manifest permission missing, launch mode for deeplink delivery |

Logs: `AppLogger` categories `General` (errors), `Navigation`, `Network`,
`Mapping` in logcat / simulator log stream and in the app log file; the in-app
Debug screen shows the same file by category.

Known trade-offs that are not bugs: conflated navigation may drop rapid-fire
Directions; dialog sessions are never restored after process death; the
destructive migration fallback is intentional; `AppLogger` dispatches in release
builds. Schema changes still follow the
[migration gate](../data-layer/references/persistence-room.md); the fallback
does not waive it.

## 4. State the root cause

One paragraph: the trigger, the code path (`path:line`), why the behavior
follows from it, and the evidence (log line, screenshot, failing step). If two
causes are plausible, say which evidence separates them and gather it before
fixing.

## 5. Fix at the owner

Apply the owning skill for the layer (ui-feature, data-layer, mappers,
di-modules, design-system, platform-build-toolkit). Fix the cause, not the
symptom: no local `try/catch`, no retry loops, no restoration flags, no extra
launches, no guard booleans. A behavior change triggers the analytics and
preservation gates like any other change. Keep the fix inside authorized scope;
a defect in a locked decision or a backend contract is a question, not a patch.

## 6. Regression gate

1. Re-run the exact reproduction when runtime access is authorized and available;
   otherwise report that scenario as unverified.
2. Run the checks and gates required by the
   [verification policy](../../../CLAUDE.md#verification-policy).
3. Obtain independent review under
   [task execution](../task-orchestrator/SKILL.md#execution-loop).
4. Report the root cause, the fix, what was verified and what remains unverified
   in the final response format.

## Found in passing

Unrelated defects discovered during the investigation are reported under
"Найдено попутно" with `path:line` and the trigger; they are not fixed in the
same task without the user's go-ahead.
