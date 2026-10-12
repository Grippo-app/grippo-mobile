# UI provider, retention and lifecycle

Read when adding or changing feature scenario creation, persistence, classifiers,
lifecycle or DI. Repository paths below are relative to the root. Reference:
`ui-screen-features/authorization/src/commonMain/kotlin/com/grippo/authorization/analytics/`.

## Files and bindings

```text
<feature>/analytics/
  <Feature>AnalyticsProvider.kt           internal stateless UI adapter
  <Feature>FailureClassifierImpl.kt       internal classifier, when needed
  <Feature>AnalyticsIntegrationModule.kt  annotated bindings, or existing UI module
```

Use existing annotated bindings where possible. The provider receives the feature
API; it never builds events, performs business requests, changes routes or binds
visibility. The classifier maps concrete SDK/domain exception types to the API
port, never text. Keep its standalone type in a separate file.

Authorization's `AuthAnalyticsIntegrationModule` supplies:

```kotlin
@Single
internal fun provideAuthAnalyticsProvider(analytics: AuthAnalytics): AuthAnalyticsProvider =
    AuthAnalyticsProvider(analytics)

@Single
internal fun provideFailureClassifier(): AuthAnalytics.FailureClassifier =
    AuthFailureClassifierImpl
```

Shared registers its generated `.module` and `AuthAnalyticsModule().module`.
The provider is singleton; its scenarios are not. Store no owner/context/scenario
properties in the provider. Resolve it once at the owning Component before creating
its retained ViewModel, and pass the returned scenario unchanged to children/VMs:

```kotlin
// AuthProcessComponent:
private val analytics = getKoin().get<AuthAnalyticsProvider>()
    .provideAuthorization(componentContext, route)

// SplashComponent (a separate owner):
private val analytics = getKoin().get<AuthAnalyticsProvider>()
    .provideSession(componentContext)
```

## Persisted scenarios

Foundation's
`ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/analytics/RetainedAnalytics.kt`
encapsulates the owner-bound sequence:

1. Consume the snapshot from StateKeeper using the persistent key and serializer.
2. Obtain `retainedSimpleInstance<T>`; create from snapshot only without an existing instance.
3. Validate necessary owner/route identity before registering save.
4. Register saving the same instance used by operations, children and retained VM.

Feature provider supplies the explicit scenario interface and snapshot types:

```kotlin
fun provideSession(context: ComponentContext): AuthAnalytics.Session =
    RetainedAnalytics(context).provide<AuthAnalytics.Session, FlowContext>(
        key = SESSION_STATE_KEY,
        serializer = FlowContext.serializer(),
        create = analytics::session,
        save = { scenario ->
            scenario.save()
        },
    )
```

Authorization additionally maps the observed route in private `mapEntryPoint()`
and converts screen-api `FlowId.value` to toolkit `FlowId`. Its validation checks
`scenario.save().flow.id.value == route.flowId.value`. The scenario implementation
also rejects a mismatched restored snapshot before reporting. These are state/owner
invariants, not ordinary reporting failures; do not swallow corruption or replace
it with a fresh scenario. Preserve serialized shape and legacy `SerialName`.
An unrelated business entity/draft ID is not scenario identity; providers do not
need route arguments when no observed telemetry fact uses them.

StateKeeper string keys differ from in-memory type keys. Keep the existing values
inside the provider's private companion, not public API or event schemas:

```kotlin
private companion object {
    const val AUTH_STATE_KEY = "authorization_analytics_flow"
    const val SESSION_STATE_KEY = "authorization_session_flow"
}
```

Use feature-specific values for new features. `retainedSimpleInstance` uses
`typeOf<T>()`; explicit interface type keeps the key independent of implementation.
One scenario of that type can exist per owner. Design distinct owners/keys if
several are genuinely required; do not accidentally retain a common wrapper type.
Plain `retainedInstance` requires `InstanceKeeper.Instance` and remains appropriate
for ViewModels, not these pure scenarios.

`RetainedAnalytics` is a local public class with an inline/reified method for
cross-module use. Its context is `@PublishedApi internal` for inline access;
`KSerializer` in the signature requires foundation's `api` serialization-core
dependency. It has no feature/business knowledge or events. Never DI-register
this owner-bound helper. No-persistence scenarios retain directly, without an
invented key or empty snapshot.

## Operations and transitions

Keep the original business block multiline inside the existing safeLaunch.
Short single-expression `update` and simple Flow transformations may stay compact.
No named local function declarations inside `init`, constructors, functions or
lambdas; methods/functions belong at class/file scope. Ordinary callbacks and
DSL lambdas remain allowed. A complete startup operation reused after dismissal
belongs in a private class-level method, without extra scopes or duplication:

```kotlin
val hasProfile = analytics.trackOperation(AuthOperation.Login(Method.Google)) {
    val credential = googleAuthProvider.getUiProvider(context).signIn().getOrThrow()
    loginUseCase.loginGoogle(token = credential.token)
}
```

`hasProfile` belongs to the use case. Login/register include private preparation;
SessionCheck preserves caller `T` without knowing SplashDirection. Failure mapping
is for payloads only; navigation and error handling remain business-owned.

Apply the [shared UI error rules](../../ui-feature/references/error-pipeline.md):
let unhandled original errors reach analytics and `safeLaunch`. Pure `mapError`
supplies only the standard dialog representation; it cannot recover or navigate.
ViewModels and their suspend helpers do not catch failures locally or add
launches/wrappers/stage flags for recovery. Analytics records the original failure
and propagates it to the shared pipeline; business result and navigation remain
outside telemetry. Cancellation stays cancellation.
`onError` runs after dialog dismissal. Keep telemetry classification separate
from presentation mapping and report transitions at their actual navigation owner.

Navigation owners report observed facts with the closed transition model:

```kotlin
analytics.trackTransition(AuthTransition.OpenRegistration)
analytics.trackTransition(AuthTransition.BackToLogin)
analytics.trackTransition(AuthTransition.Exit(ExitReason.UserBack))
```

Exit's reason is mandatory; payload-free variants need no argument. Keep exhaustive
dispatch, exit deduplication and duplicate-transition attribution intact. These
calls report facts; they do not perform navigation.

## Leaf lifecycle

Foundation's public `analytics/ScreenAnalytics` interface exposes
`bind(lifecycle, present)`. Its stateless
`internal/analytics/ScreenAnalyticsImpl` is `@Single(binds = [ScreenAnalytics::class])`
through `CoreModule` component scan. Resolve the API in each leaf's construction:

```kotlin
getKoin().get<ScreenAnalytics>().bind(lifecycle) {
    analytics.presentScreen(Step.Login)
}
```

Each binding owns its callback/presentation. Resume evaluates the callback,
pause/destroy closes, destroy unsubscribes. Reporting Exceptions cannot interrupt
lifecycle; fatal Errors and the destroyed-owner invariant remain explicit.
Restore means reactivation of an existing surface, including foreground, as well
as restoration from state; it does not require a persistence snapshot.
Do not bind during Render, in a VM or at both container and leaf. No legacy
extensions or overloads supplying raw navigation/flow.

## Checks

Use real helper/provider/implementation and generated modules. Cover retention
across recreation, mutate after recreation, save using the new StateKeeper and
restore that latest snapshot without replay. Verify validation before registration,
route mismatch, all route mappings, independent owners/types, session restoration
and singleton provider/API resolution. `RetainedAnalyticsTest`,
`AuthAnalyticsProviderTest` and `ScreenAnalyticsTest` exercise those boundaries.

Check operation result/error preservation, cancellation and reporting-failure
injection at actual owners. Provider tests alone do not prove app runtime integration.
Apply save/restore/mismatch checks only to scenarios with those contracts;
nonpersistent scenarios need direct-retention/recreation/independent-owner checks.
Run tests/platform checks within current authorization and report skipped checks
explicitly; Firebase delivery is a separate permission.
