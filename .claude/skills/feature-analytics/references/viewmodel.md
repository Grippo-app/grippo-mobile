# ViewModel analytics and the foundation screen hook

Read when wiring VM dependencies, screen hooks, events or complete operations,
with the [canonical skill](../SKILL.md). Current examples are the authorization
ViewModels and `ui-core/foundation` BaseComponent/BaseViewModel/ScreenAnalytics.

## Conventional injection

ViewModel constructor parameters follow the canonical
[ViewModel layout](../../validation-gates/references/code-style.md#viewmodel-layout).

The Component only constructs its retained ViewModel and resolves dependencies:

```kotlin
override val viewModel = componentContext.retainedInstance {
    LoginViewModel(analytics = getKoin().get(), /* existing dependencies */)
}
```

Constructor examples are abbreviated. Do not pass analytics in Component
constructors or route factories, add a feature provider, or resolve lifecycle
services in the feature. Routes keep navigation identity; analytics does not read it.
Feature events are emitted only by the owning ViewModel.

The leaf ViewModel receives the same stateless `AuthAnalytics` as other leaves:

```kotlin
internal class LoginViewModel(
    private val analytics: AuthAnalytics,
    /* existing dependencies */
) : BaseViewModel<LoginState, LoginDirection, LoginLoader>(/* existing state */) {
    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(AuthScreen.Login, reason)
    }
}
```

Every independent screen must declare this hook in its owning ViewModel. Current
foundation shape (inspect `ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/`
before relying on it):

- `BaseViewModel.screenPresentation` is `protected open`, typed
  `((NavigationReason) -> ScreenPresentation)?`, default null for containers.
- `BaseViewModel.bindScreen(lifecycle)` is `internal`. `BaseComponent` calls it in
  `doOnCreate` after the retained VM exists, before `attachActivation` and before
  it subscribes to `navigator`, so an init navigation is never delivered before
  the lifecycle is bound.
- `analytics/ScreenAnalytics` is the public interface; `internal/analytics/ScreenAnalyticsImpl`
  is `@Single(binds = [ScreenAnalytics::class]) internal`, discovered by `CoreModule`.
  `bind` fails with `check(lifecycle.state != DESTROYED)`, then subscribes one
  callback per binding: `onResume` presents with `Initial` the first time and
  `Restore` afterwards (`hasPresented` lives in that binding, so a fresh binding
  has no saved provenance and restores no flow/referrer attribution); `onPause`
  closes the presentation; `onDestroy` runs `onPause()` and unsubscribes.
  Exceptions thrown by `present`/`close` are swallowed; telemetry never
  interrupts lifecycle delivery.

Do not infer flow continuity, entry attribution or Action/Back reasons from a
fresh binding.

Graph/container VMs use null: no binding and no ScreenAnalytics resolution. Do not
override a graph as a screen just because it owns a router. VM knows neither
Lifecycle nor ComponentContext and does not observe activationFlow for visibility.
There is no extra scope/job, analytics retention/StateKeeper key or manual binding.
Normal Component/retainedInstance/ViewModel lifecycle ownership is unchanged.

## Screen classification

An independent screen is a separately displayed page with its own content. Its VM
must present the matching typed FeatureScreen; a child screen inside a stack or
tab remains a screen. A host that only selects/renders child screens, forwards
navigation or adds shared chrome stays a container with default null. A router,
ViewModel, BaseComposeScreen wrapper or `*Screen.kt` filename alone proves neither
role. A page with its own content can also own child navigation; review what is
actually displayed. Container events/operations may still observe its real business
callbacks; they do not justify a container screen_view.

Run the [mandatory screen coverage gate](../SKILL.md#mandatory-screen-coverage-gate)
against actual routes, Component factories and rendered bodies.

## Direct typed facts and complete operations

Apply the canonical [observation boundary](../SKILL.md#observation-boundary-must)
before choosing event parameters or changing a caller contract. The examples below
observe facts already available to the owning VM.

```kotlin
analytics.trackEvent(AuthEvent.Login.Submit(method = Method.Google))
val hasProfile = analytics.trackOperation(AuthOperation.Login(method = Method.Google)) {
    val credential = googleAuthProvider.getUiProvider(context).signIn()
    loginUseCase.loginGoogle(token = credential.token)
}

analytics.trackEvent(
    AuthEvent.Login.InputChanged(field = CredentialField.Email, status = InputStatus.Valid)
)
analytics.trackEvent(AuthEvent.User.PickerConfirmed(field = ProfilePicker.Height, status = status))
```

The current [LoginUseCase](../../../../data-features/feature-api/src/commonMain/kotlin/com/grippo/data.features.api/authorization/LoginUseCase.kt)
returns Boolean and throws on failure; credential providers return their account and throw on failure.
Credential acquisition stays inside its decorator. Feature/UseCase commands follow the
[throwing suspend contract](../../data-layer/references/operation-outcomes.md#throwing-suspend-contracts-canonical).

For a throwing command already run through the error pipeline, keep its complete
invocation inside the decorator. Current example from
`ui-screen-features/trainings/.../TrainingsViewModel.kt` (re-inspect before copying):

```kotlin
safeLaunch(recovery = ErrorRecovery.Retry) {
    analytics.trackOperation(operation = TrainingsOperation.DeleteTraining) {
        deleteTrainingUseCase.execute(id)
    }
}
```

Use the existing `safeLaunch` directly, without a local recovery catch or wrapper.
A pure `mapError` changes dialog presentation only. Intent placement, complete
operation retries and both `safeLaunch` overloads follow
[Retry and close](../../ui-feature/references/error-pipeline.md#retry-and-close).
Startup uses the existing VM scope; the foundation runs the next attempt after
the popup's Retry. Follow [coroutine rules](../../validation-gates/references/forbidden-patterns.md#coroutines)
and [Kotlin blocks](../../validation-gates/references/code-style.md#kotlin-blocks)
for cancellation, function declarations and block layout.

Emit in existing Contract→VM callbacks after eligibility/busy guards. Rejected
callbacks emit neither accepted intent nor operation events. Text events compare
old/new status from existing validated UI state, using mapping in UI analytics/;
equal status, including initial Empty→Empty, is omitted without a cache.
Picker confirmations/clicks/selections remain distinct. Raw values are not encoded.
Telemetry never decides validation, navigation, retries or business results.

For dialogs/pickers, open intent and accepted result are separate facts in the
owning VM's existing callbacks. An onDismiss callback may return an observed fact
to that VM; it must not infer confirmation, cancellation or user exit. Shared
host/config callbacks do not construct events. The screen hook does not provide
dedicated dialog_view/close telemetry; record the omission or define that separate
contract within an explicitly requested scope before adding its implementation.

Model constraints and wire encoding follow
[feature contracts](collection-and-core.md#feature-api-and-schema-contracts).

## Event meaning and dismissal

An in-surface edit cancellation (Cancel beside a field) is that control's
`Clicked` fact. Use `DismissRequested` for an observed user request to leave or
close the surface. It records intent, not completed dismissal or cancellation.
Declare it in the feature's `models/` and encode it as
`<feature>_dismiss_requested`.

Keep navigation Back/onBack names when they describe navigation; do not rename
business contracts to match telemetry. Programmatic pop, lifecycle close and
successful completion are not user dismissal requests.

A telemetry rename reconciles public events, internal action types/matrices,
wire encoders, VM callers and current coverage together. Retain no compatibility
alias without a released-consumer requirement.
