# `:shared` — composition-root module

> For the `Koin.kt` file detail / `Koin.init` call sites / module-listing rules,
> see [`composition-root.md`](composition-root.md).

> **Illustrative domain.** Code blocks below use `Note` / `Tag` / `User` as the
> generic `<Entity>` / `<RelatedEntity>` for examples. Substitute identifiers from
> your product domain.

`:shared` is the single module that imports **every** other module and wires them
together. It is the only KMP module the app shells (`:androidApp`, `:iosApp`) need
to reach.

`:shared` is the composition root: it imports every other module that needs to be
wired into Koin or referenced by `RootComponent`.

## Responsibilities

1. **Koin composition** — `Koin.init { ... }` lists every Koin module by name.
   Adding a module elsewhere requires adding it to this list.
2. **Root navigator** — `RootComponent`, `RootViewModel`, `RootScreen`,
   `RootContract`, `RootDirection`, `RootLoader`, `RootState`. The primary screen
   stack lives here.
3. **Dialog navigator** — `DialogComponent` hosts the parallel slot navigator for
   bottom sheets.
4. **Auto-logout observer** — `RootViewModel` subscribes to
   `AuthorizationFeature.getToken()` and navigates to login when the token becomes
   null.
5. **Deeplink router** — parses incoming deeplinks and translates them into
   `RootDirection`s. Cold start handled in `enqueueDeeplink`; warm start in
   `applyDeeplink`; mapping in `parseDeeplink`.
6. **Locale/theme installation** — `LaunchedEffect(systemLocaleTag) {
   DateFormatting.install(systemLocaleTag) }` in `RootComponent.Render()` ensures
   `DateTimeUtils` formatters use the current locale.

## File layout

```
shared/
  build.gradle.kts                          // five convention plugins (android.library + kotlin.multiplatform + compose.multiplatform + koin.annotation + ios.swiftpackage)
  src/
    commonMain/kotlin/com/<org>/<product>/shared/
      Koin.kt                               // public object Koin { fun init(...) }
      root/
        RootComponent.kt                    // primary stack navigator (public)
        RootViewModel.kt                    // root state + auto-logout + deeplink fan-out (public)
        RootScreen.kt                       // @Composable, hosts ChildStack
        RootContract.kt                     // callbacks for RootViewModel
        RootDirection.kt                    // sealed Directions
        RootLoader.kt                       // sealed Loaders
        RootState.kt                        // root state class (holds queued deeplink)
      dialog/
        DialogComponent.kt                  // slot navigator parallel to stack
        DialogContract.kt, DialogState.kt, ...
        content/
          DialogContentComponent.kt         // resolves DialogConfig → child Component
          ... (DialogContent{VM, Contract, State, ...})
    iosMain/kotlin/com/<org>/<product>/shared/
      RootViewController.kt                 // rootViewController(root, backDispatcher): UIViewController
```

`Deeplink` (the enum) lives in `:ui-screen-features:screen-api`
(`screen.api.deeplink.Deeplink`), not in `:shared`. `:shared` only consumes it
from `RootViewModel.parseDeeplink`.

## `Koin.kt` — the composition root

```kotlin
public object Koin {
    public fun init(
        appDeclaration: KoinAppDeclaration = {},
    ): KoinApplication = KoinPlatformTools.defaultContext().startKoin {
        appDeclaration()
        modules(
            ContextModule().module,
            DatabaseModule().module,
            DataStoreModule().module,
            BackendModule().module,
            GoogleAuthModule().module,        // optional — register only for the chosen auth providers
            AppleAuthModule().module,         // optional — register only for the chosen auth providers
            CoreModule().module,              // :ui-core:foundation
            DialogModule().module,            // :ui-dialog-features:dialog-api
            AuthorizationFeatureModule().module,
            ErrorModule().module,             // :ui-core:error:error-provider-impl
            UserFeatureModule().module,
            // ... every other feature module
            FeatureApiModule().module,        // :data-features:feature-api (UseCases)
            ConnectivityModule().module,
            LinkOpenerModule().module,
            NotificationManagerModule().module,
            PermissionManagerModule().module,
            ResourcesProviderModule().module, // :design-system:resources:provider-impl
            SerializationModule().module,
            HttpModule().module,
            ImageLoaderModule().module,
        )
    }
}
```

**Order does not matter** — Koin resolves dependencies lazily. But **every module
added anywhere must appear in this list** or it will not be registered.

The `appDeclaration: KoinAppDeclaration` parameter is the platform hook:
- Android: `Koin.init { androidContext(this@App); androidLogger() }`
- iOS: `Koin.init {}` (Koin's iOS support requires no extra setup beyond the
  modules)

## `RootComponent` shape

```kotlin
public class RootComponent(
    componentContext: ComponentContext,
    private val close: () -> Unit,
    deeplink: String? = null,
) : BaseComponent<RootDirection>(componentContext) {

    private val dialogComponent = DialogComponent(componentContext)

    override val viewModel: RootViewModel = componentContext.retainedInstance {
        RootViewModel(
            authorizationFeature = getKoin().get(),
            connectivity = getKoin().get(),
            deeplink = deeplink,
        )
    }

    private val navigation = StackNavigation<RootRouter>()

    internal val childStack: Value<ChildStack<RootRouter, Child>> = childStack(
        source = navigation,
        serializer = RootRouter.serializer(),
        initialConfiguration = Auth(AuthRouter.Splash),
        handleBackButton = true,
        key = "RootComponent",
        childFactory = ::createChild,
    )

    private val backCallback = BackCallback(onBack = viewModel::onClose)

    init {
        backHandler.register(backCallback)
    }

    public fun handleDeeplink(deeplink: String) {
        if (childStack.value.active.configuration is RootRouter.Home) viewModel.applyDeeplink(deeplink)
        else viewModel.enqueueDeeplink(deeplink)
    }

    private fun createChild(router: RootRouter, ctx: ComponentContext): Child = when (router) {
        is Auth -> Child.Authorization(AuthComponent(ctx, initial = router.value, toHome = ..., close = ...))
        RootRouter.Home -> Child.Home(HomeRootComponent(ctx, ...))
        is RootRouter.Profile -> Child.Profile(ProfileComponent(ctx, initial = router.value, close = ...))
        is RootRouter.Notes -> Child.Notes(NotesRootComponent(ctx, initial = NotesRouter.Detail(router.id), close = ...))
        // ...
    }

    public sealed class Child(public open val component: BaseComponent<*>) {
        public data class Authorization(override val component: AuthComponent) : Child(component)
        // ...
    }

    override suspend fun eventListener(direction: RootDirection) {
        when (direction) {
            RootDirection.Login -> if (childStack.value.active.instance !is Child.Authorization) {
                navigation.replaceAll(Auth(AuthRouter.AuthProcess))
            }
            RootDirection.Home -> navigation.replaceAll(RootRouter.Home)
            RootDirection.Profile -> navigation.push(RootRouter.Profile(ProfileRouter.Overview))
            is RootDirection.Notes -> navigation.push(RootRouter.Notes(direction.id))
            RootDirection.Back -> navigation.pop()
            RootDirection.Close -> close()
            // ...
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()

        val systemIsDark = AppTheme.current
        val systemLocaleTag = AppLocale.current

        LaunchedEffect(systemLocaleTag) { DateFormatting.install(systemLocaleTag) }

        AppTheme(darkTheme = systemIsDark, localeTag = systemLocaleTag) {
            RootScreen(this, state.value, loaders.value, viewModel)
            dialogComponent.Render()
        }
    }
}
```

Notes:
- `RootComponent` is **public** because the iOS shell (Swift `AppDelegate`)
  instantiates it directly. The Android shell does the same via
  `retainedComponent { ... }`. This is the only `Base*Component` in the project
  that is not `internal`.
- `RootComponent` is the **only** component that owns a `StackNavigation`. Inner
  features have their own private `StackNavigation` instances, but
  `RootComponent`'s is the entry point.
- `RootComponent` also registers an Essenty `BackCallback(onBack =
  viewModel::onClose)` against the inherited `backHandler` so the system back
  gesture at the top of the stack maps to `RootDirection.Close`. The `Login`
  branch in `eventListener` is guarded against re-replacing the Auth stack when
  the user is already on `Authorization` (e.g. the token observer firing again).
- The `key = "RootComponent"` argument is important for state restoration.
- The dialog slot navigator is a **sibling** of the stack — both render
  simultaneously, with the slot drawn on top. `LaunchedEffect(systemLocaleTag)`
  lives on `RootComponent.Render()` (above the screen + dialog) so the locale
  install runs once for the whole tree. The `systemIsDark` / `systemLocaleTag`
  locals are read once via `AppTheme.current` / `AppLocale.current` and passed
  both to `LaunchedEffect` and `AppTheme(...)`, so the `@Composable` getters fire
  a single time per recomposition (don't reach for `AppLocale.current` inside the
  `LaunchedEffect` body — that would re-enter the `@Composable` snapshot reader
  from a non-Composable scope).
- Inner feature components are mostly named `<Feature>Component` (e.g.
  `ProfileComponent`, `AuthComponent`) regardless of how many sub-screens they
  own. The `<Feature>RootComponent` suffix is reserved for features whose first
  sub-screen reuses the feature name — `:home` has a `home/HomeComponent`
  sub-screen, so the root is `HomeRootComponent` to avoid a class-name collision.
  It is not a multi-screen-vs-single-screen distinction.

## `RootViewModel` essentials

```kotlin
public class RootViewModel(
    authorizationFeature: AuthorizationFeature,
    connectivity: Connectivity,
    deeplink: String? = null,
) : BaseViewModel<RootState, RootDirection, RootLoader>(RootState(deeplink = deeplink)),
    RootContract {

    init {
        authorizationFeature.getToken()
            .onEach { if (it == null) navigateTo(RootDirection.Login) }
            .safeLaunch()

        connectivity.statusUpdates
            .onEach(::provideConnectionStatus)
            .safeLaunch()
    }

    internal fun enqueueDeeplink(deeplink: String) { update { it.copy(deeplink = deeplink) } }
    internal fun applyDeeplink(deeplink: String) { parseDeeplink(deeplink)?.let { navigateTo(it) } }
    private fun parseDeeplink(raw: String): RootDirection? = when (Deeplink.fromKey(raw)) {
        Deeplink.NoteEditor -> RootDirection.Notes(NotesRouter.Editor)
        Deeplink.NoteArchive -> RootDirection.NoteArchive
        null -> null
    }

    override fun toHome() {
        navigateTo(RootDirection.Home)
        // Cold-start path: when Home becomes active, consume any queued deeplink.
        state.value.deeplink?.let { parseDeeplink(it)?.let { dir -> navigateTo(dir) } }
        update { it.copy(deeplink = null) }
    }

    // RootContract callbacks (back/close, top-level navigation shortcuts) ...
}
```

- `RootViewModel` is **public** for the same reason as `RootComponent` — the iOS
  shell needs to reference its `RootContract` callbacks.
- The deeplink raw string lives in `RootState` until Home becomes active; on cold
  start it's drained inside `toHome()`. The `Deeplink` enum is in
  `:ui-screen-features:screen-api`.

## `RootScreen` essentials

```kotlin
@Composable
internal fun RootScreen(
    component: RootComponent,
    state: RootState,
    loaders: ImmutableSet<RootLoader>,
    contract: RootContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    ChildStack(
        modifier = Modifier.fillMaxSize(),
        stack = component.childStack,
        animation = stackAnimation(selector = { child, _, _, _ -> child.instance.animator() }),
        content = { child -> child.instance.component.Render() },
    )
}

private fun RootComponent.Child.animator(): StackAnimator = when (this) {
    is RootComponent.Child.Authorization -> fade()
    is RootComponent.Child.Home -> fade()
    is RootComponent.Child.Profile -> platformStackAnimator()  // iOS-like slide on iOS; default on Android
    // ...
}
```

`RootScreen` takes the `RootComponent` itself (not just the stack) and reads
`component.childStack` to render the Compose-flavored `ChildStack`. The dialog slot
is rendered separately by `RootComponent.Render` (it sits in an `AppTheme {
RootScreen(...); dialogComponent.Render() }` block).

`platformStackAnimator()` lives in `:ui-core:foundation` (expect/actual). See
the ui-feature skill (`../../ui-feature/references/base-classes.md`).

## `:shared/build.gradle.kts`

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("ios.swiftpackage.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.shared" }

    sourceSets.commonMain.dependencies {
        api(libs.decompose.core)
        api(libs.decompose.extensions)
        api(libs.decompose.back.handler)
        api(libs.decompose.state.keeper)

        // every design-system module
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.resources.providerImpl)
        implementation(projects.designSystem.components)

        // every toolkit module
        implementation(projects.toolkit.context)
        implementation(projects.toolkit.localization)
        implementation(projects.toolkit.theme)
        // ... all toolkit:*

        // every ui-core module
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.uiCore.error.errorProviderImpl)

        // every data-service module
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.datastore)
        implementation(projects.dataServices.backend)
        implementation(projects.dataServices.googleAuth)   // omit if Google Sign-In not selected in Step 0
        implementation(projects.dataServices.appleAuth)    // omit if Apple Sign-In not selected in Step 0
        // region firebase-conditional (firebaseEnabled = true only)
        api(projects.dataServices.firebase)   // exported to iOS for crash bridging
        // endregion firebase-conditional

        // every data-feature module
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataFeatures.authorization)
        implementation(projects.dataFeatures.user)
        // ... all data-features:*

        implementation(projects.dataMappers.domainToState)

        // every ui-screen-features module
        implementation(projects.uiScreenFeatures.screenApi)
        implementation(projects.uiScreenFeatures.authorization)
        // ... all ui-screen-features:*

        // every ui-dialog-features module
        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.uiDialogFeatures.confirmation)
        // ... all ui-dialog-features:*

        implementation(libs.datetime)
        implementation(libs.immutable.collections)

        implementation(compose.ui)
        implementation(compose.material3)
        implementation(compose.foundation)
    }
}
```

Notes:
- `:shared` uses **all five** convention plugins (`android.library` +
  `kotlin.multiplatform` + `compose.multiplatform` + `koin.annotation` +
  `ios.swiftpackage`) (which builds the XCFramework).
- Decompose libraries are exposed via `api(...)` because `:androidApp` / `:iosApp`
  use Decompose types directly.
- `:data-services:firebase` is exposed via `api(...)` because the iOS XCFramework
  re-exports it (Swift code calls into it). When `firebaseEnabled = false` (Step 0
  / `project-config.md`), strip the `api(projects.dataServices.firebase)` line
  between the `// region firebase-conditional` / `// endregion firebase-conditional`
  markers — the module is not scaffolded (see `launch.md` Step 3) and leaving the
  line fails module resolution. Identical marker pattern is used in the ui-feature
  skill (`../../ui-feature/references/base-classes.md` and
  `../../ui-feature/references/module-structure.md`).
- `:data-services:google-auth` and `:data-services:apple-auth` are conditional on
  the auth methods selected in Step 0 — strip whichever line is unused.
  `:shared/Koin.kt` registers `GoogleAuthModule()` / `AppleAuthModule()` only for
  the chosen providers (see [`composition-root.md`](composition-root.md)).
- Every other dependency is `implementation`.

## Adding to `:shared`

When you add a new module elsewhere, you usually need to:

1. Add the module to `settings.gradle.kts`.
2. Add `implementation(projects.<group>.<module>)` to `:shared/build.gradle.kts`
   (unless it's a `:data-features:<x>` impl that `:shared` doesn't need directly).
3. Add the module's Koin `<X>Module` to `:shared/Koin.kt`'s `modules(...)` list
   (if it provides DI).

This is the deliberate cost of an explicit composition root.
