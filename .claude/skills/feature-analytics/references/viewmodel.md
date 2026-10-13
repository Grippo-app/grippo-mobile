# ViewModel analytics and the foundation screen hook

Read with the [canonical skill](../SKILL.md). Current examples are the authorization
ViewModels and `ui-core/foundation` BaseComponent/BaseViewModel/ScreenAnalytics.

## Conventional injection

The Component only constructs its retained ViewModel and resolves dependencies:

```kotlin
override val viewModel = componentContext.retainedInstance {
    LoginViewModel(analytics = getKoin().get(), /* existing dependencies */)
}
```

Constructor examples are abbreviated. Do not pass analytics in Component
constructors or route factories, add a feature provider, or resolve lifecycle
services in the feature. Routes keep navigation identity; analytics does not read it.

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

Every independent screen must declare this hook in its owning ViewModel.
BaseViewModel offers a nullable protected hook with default null for containers. BaseComponent
calls internal bindScreen(lifecycle) during its existing doOnCreate after the
retained VM exists, before activation/navigation bridging. This avoids delivering
an init navigation before its lifecycle is bound. Foundation ScreenAnalytics owns
the per-binding Initial/Restore flag and presentation close on pause/destroy.
First activation is Initial, subsequent activations Restore; fresh bindings do
not restore flow/referrer attribution. Foundation remains API + internal @Single
implementation discovered by CoreModule.

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

Authorization has eight screen hooks and three null-hook hosts:
AuthViewModel, AuthProcessViewModel and ProfileCreationViewModel. The last adds
shared progress decoration, not a separate page. The complete mapping is in
[authorization coverage](../../../../tasks/analytics/authorization-coverage.md).
Profile has eight independent screen hooks and one null-hook ProfileViewModel
container; its screen-only API introduces no event/operation families. See
[Profile coverage](../../../../tasks/analytics/profile-coverage.md).
Dialogs, pickers and overlays do not automatically become screens; their dedicated
presentation telemetry needs the separate contract described below.

Run the [mandatory screen coverage gate](../SKILL.md#mandatory-screen-coverage-gate)
against actual routes, Component factories and rendered bodies before completion.
A new feature needs an inventory of all its screens; a behavior change needs all
screens in the affected scope. Missing hooks and container views emitted alongside their child screens block
completion; default null is not a permitted screen omission.

## Direct typed facts and complete operations

```kotlin
analytics.trackEvent(AuthEvent.Login.Submit(Method.Google))
val hasProfile = analytics.trackOperation(AuthOperation.Login(Method.Google)) {
    val credential = googleAuthProvider.getUiProvider(context).signIn().getOrThrow()
    loginUseCase.loginGoogle(token = credential.token)
}

analytics.trackEvent(
    AuthEvent.Login.InputChanged(CredentialField.Email, InputStatus.Valid)
)
analytics.trackEvent(AuthEvent.User.PickerConfirmed(ProfilePicker.Height, status))
```

The current [LoginUseCase](../../../../data-features/feature-api/src/commonMain/kotlin/com/grippo/data.features.api/authorization/LoginUseCase.kt)
returns Boolean and throws on failure; the credential provider returns Result.
Keep those contracts. For a Result-returning operation, the existing unwrap belongs
inside the decorator, not after it:

```kotlin
val value = analytics.trackOperation(FeatureOperation.Save) {
    saveUseCase.execute(existingValue).getOrThrow()
}
```

This second example is a generic pattern. Reuse the actual business operation and
its result type; a returned Result.failure otherwise looks like a completed block.

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

AuthEvent constructors contain direct typed values, not action wrappers. Credential
and picker families enforce their field enums; fixed Name is implicit. These facts
do not implement toolkit AnalyticsEvent: internal matrix/payload/schema encode wire.

Shared registers only the generated feature implementation module; foundation DI
owns ScreenAnalytics. No feature analytics integration module/classifier/provider
is needed. Verify actual VM screen declarations and the foundation hook/lifecycle,
payloads, operation results/errors/cancellation and reporting isolation. Respect
the permitted verification scope; source checks do not establish runtime correctness.
