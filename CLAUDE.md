# grippo-mobile

## Project purpose

KMP клиент Grippo — Android + iOS из одной кодовой базы. UI на Compose
Multiplatform, навигация через Decompose. Главный потребитель backend API.

---

## Stack

- Kotlin **2.3.21**, explicit API mode (включён в `KotlinMultiplatformConventionPlugin`).
- Compose Multiplatform **1.10.3** + Kotlin Compose plugin. Метрики и stability config включены через convention plugin.
- Targets: `commonMain`, `androidMain`, `iosMain` (`iosArm64`, `iosSimulatorArm64`, `iosX64`). Android `compileSdk = 36`, `minSdk = 26`. JVM toolchain 19.
- Decompose **3.5** + Essenty (`back-handler`, `state-keeper`, `lifecycle`).
- Koin **4.2** + `koin-annotations` **2.3.1** + KSP. KOIN_CONFIG_CHECK выключен (TODO до новой версии Koin Annotations).
- Ktor **3.4** (Android engine + Darwin engine), `kotlinx-serialization-json` **1.11**.
- Room **2.8** multiplatform, `androidx.sqlite-bundled`, `Bundled SQLite Driver` на iOS, миграции 2→3, 3→4, 4→5 в `:data-services:database`.
- AndroidX DataStore **1.2** (preferences-core).
- Coil **3** (`coil-compose`, `coil-network-ktor3`).
- Firebase: Analytics, Crashlytics, Messaging — только в Android shell.
- Auth: Google (`androidx-credentials` + Google Identity), Apple (custom).
- Build: Gradle с convention plugins в `build-logic/convention/`, version catalog в `gradle/libs.versions.toml`. Type-safe project accessors включены.
- iOS deliverable: статический XCFramework `shared.xcframework` через `ios.swiftpackage.convention` (Decompose api re-exported для Swift, линкуется `-lsqlite3`).

---

## Module layout (architecture)

`settings.gradle.kts` — единственный источник истины по составу модулей.
Группы:

- **App shells** (тонкие, без бизнес-логики)
  - `:androidApp` — Application + MainActivity + DI startup + Firebase setup. Декомпозирует root через `retainedComponent { RootComponent(...) }`. Edge-to-edge, splash screen, deeplink из intent.
  - `:iosApp` — Xcode-проект. Linkает `shared.xcframework` и стартует `RootViewController` из `iosMain`.

- **`:shared`** — composition root.
  - `Koin.kt` — `Koin.init { ... }` стартует все Koin-модули **явно перечислением**. Любой новый модуль добавляется сюда руками.
  - `RootComponent` / `RootViewModel` / `RootScreen` / `RootContract` / `RootDirection` / `RootLoader` / `RootState` — главный stack-навигатор + слушатель токена для разлогина.
  - `DialogComponent` — отдельный slot-навигатор для bottom sheet'ов поверх stack'а.

- **Design system**
  - `:design-system:core` — `AppTokens` (colors, dp, typography, strings).
  - `:design-system:components` — атомарные Composable (`Button`, `Toolbar`, `Input*`, `WeightHistoryChart`, ...).
  - `:design-system:resources:provider` + `:design-system:resources:provider-impl` — `Res.string.*`, `StringProvider` (для не-Composable-доступа из ViewModel).
  - `:design-system:preview` — `@AppPreview`, `PreviewContainer` для preview-функций.

- **UI core**
  - `:ui-core:foundation` — `BaseViewModel`, `BaseComponent`, `BaseScreen`, `OperationManager`, `ResultManager`/`ResultEmitter` (cross-component результаты), `BaseDirection`/`BaseLoader`/`BaseRouter`/`BaseResult`/`ComponentIdentifier`, `collectAsStateMultiplatform` (expect/actual).
  - `:ui-core:state` — переиспользуемые UI-data-классы (`MuscleLoadSummaryState`, `DigestState`, `TrainingStreakState`, formatters: `WeightFormatState`, `EmailFormatState`, `PasswordFormatState`, `DateFormatState`, `UiText`, и т. п.). Каждый класс `@Immutable`, у многих есть `stub*()` функция.
  - `:ui-core:error:error-provider` (interface `ErrorProvider`, `AppError` sealed) и `:ui-core:error:error-provider-impl` (`ErrorProviderImpl`, маппит `AppError` → `AppErrorState` → показывает `DialogConfig.ErrorDisplay`).

- **`:ui-screen-features:*`** — экраны (полноэкранные).
  - `:screen-api` — публичные `*Router` sealed-классы (`@Serializable`) и `Deeplink` enum. Используются Decompose'ом для type-safe навигации **между фичами**.
  - Фичи: `authorization`, `home`, `profile`, `training`, `trainings`, `debug`. Внутри фичи — корневой `*RootComponent` со своим `StackNavigation<*Router>` и **внутренние** под-Component'ы для каждого экрана.

- **`:ui-dialog-features:*`** — bottom sheet потоки (~25 модулей: weight-picker, height-picker, date-picker, exercise, exercise-example, statistics, *-details, *-picker, ...).
  - `:dialog-api` — `DialogConfig` sealed, `DialogController.show(config)`, `DialogProvider`, `DialogModule`.
  - Каждая dialog-feature — самостоятельный `Component`/`ViewModel`/`Screen` пакет, выглядит **идентично screen-feature** за исключением того, что хост — `DialogComponent` вместо `RootComponent`.

- **`:data-services:*`** — низкоуровневые сервисы и DTO.
  - `:backend` — `GrippoApi` (плоский класс с методами вида `suspend fun <action>(body): Result<T>`), `BackendClient` (Ktor с Auth, Logging, ContentNegotiation, defaultRequest на `https://grippo-app.com`), `TokenProvider` (Auth `Bearer` + refresh с mutex и retry-with-backoff), `ClientLogger`, `dto/<area>/*` — `@Serializable data class`, все поля nullable с `@SerialName`.
  - `:database` — Room `@Database(version = 5)`, все `@Entity`, DAO, миграции, `models/*Pack` (`@Embedded` + `@Relation`), `converters/StringListConverter`, `DatabaseBuilder` (expect/actual: Android — `getDatabasePath`, iOS — `NSDocumentDirectory` + `BundledSQLiteDriver`), **`fallbackToDestructiveMigration(dropAllTables = true)` на обеих платформах — намеренно**.
  - `:datastore` — обёртка над AndroidX DataStore.
  - `:google-auth` / `:apple-auth` — platform-specific обёртки для ID token.
  - `:firebase` — interface `FirebaseProvider` + `Firebase{Analytics,Crashlytics,Messaging}` с Android-implementations (через `FirebaseProvider.setup(...)` в `App.onCreate`); iOS пустой stub.

- **`:data-features:*`** — domain-уровень.
  - `:feature-api` — `*Feature` interfaces (`TrainingFeature`, `UserFeature`, ...) + `*UseCase` (`DeleteTrainingUseCase`, `UpdateWeightUseCase`, `TrainingDigestUseCase`, ...) + domain-models (`Training`, `Exercise`, `Iteration`, `User`, `WeightHistory`, ...). **Только это** видно UI-модулям.
  - Реализации: `:trainings`, `:user`, `:weight-history`, `:goal`, `:muscle`, `:equipment`, `:exercise-examples`, `:exercise-metrics`, `:excluded-muscles`, `:excluded-equipments`, `:authorization`, `:local-settings`. У каждой свой Koin `*FeatureModule(includes = [BackendModule, DatabaseModule])`. Внутри: `data/<X>RepositoryImpl` + `domain/<X>Repository` + `domain/<X>FeatureImpl`. Repository — `@Single(binds = [<X>Repository::class])`, FeatureImpl — `@Single(binds = [<X>Feature::class])`. Repository **internal**, Feature **public**.

- **`:data-mappers:*`** — направленные мапперы. Каждый — отдельный модуль, без cross-direction зависимостей:
  - `:dto-to-entity` — `package com.grippo.dto.entity.<area>`, `Response.toEntityOrNull(): Entity?`, `List<Response>.toEntities()`. **Любая `null`-ность поля логируется через `AppLogger.Mapping.log(value) { "<DTO>.<field> is null" }` и приводит к `return null` для всей сущности.** Всё, что не «opt-in null», обязательно.
  - `:entity-to-domain` — `package com.grippo.entity.domain.<area>`, `Pack.toDomain(): Domain` / `List<Pack>.toDomain()`. Пакетный нюанс — пакеты директорий с точкой (`com/grippo/entity/domain/training/`).
  - `:domain-to-state` — `Domain.toState(): State` / `List<Domain>.toState()`. Хост — `:ui-core:state`, используется в ViewModel-ах перед `update {}`.
  - `:state-to-domain` — обратное направление (когда экран собирает форму).
  - `:domain-to-dto` — `Domain.toBody(): Body` для `POST/PUT` запросов.
  - `:domain-to-entity` — для draft-ов, которые из domain пишутся напрямую в БД минуя бэкенд.
  - `:dto-to-domain` — для случаев, когда БД-кэширование не нужно.

- **`:toolkit:*`** — platform-aware утилиты.
  - `:context` — `NativeContext` (expect/actual, Android держит `Context`, iOS — pure object), `ContextModule`.
  - `:http-client` — `HttpClient` Ktor c `responseValidator` + `ApiErrorParser` (превращает HTTP в `AppError`).
  - `:serialization` — `Json` provider (lenient, ignoreUnknownKeys).
  - `:logger` — `AppLogger` (singleton с `General/Navigation/Network/Mapping`, файловая запись `LogFileWriter` expect/actual: Android `File`, iOS `NSFileManager + fopen`). Особенность — `AppLogger.Mapping.log(value) { msg }: T?` — если value null, лог + caller location, и null прокидывается через `?: return null` в маппере.
  - `:date-utils` — `DateTimeUtils`, `DateRange`, `DateRangeKind`, `DateRangePresets`, `DateFormat`, `DateFormatting.install(locale)` (вызывается из `RootComponent.Render` при смене системной локали).
  - `:theme`, `:localization` — `AppTheme`/`AppLocale.current` (системная тема и локаль через expect/actual).
  - `:image-loader` — Coil setup.
  - `:link-opener`, `:notification-manager`, `:permission-manager`, `:connectivity` — platform expect/actual.

- **`:compose-libs:*`** — переиспользуемые Compose-виджеты вне дизайн-системы (`chart`, `konfetti`, `segment-control`, `wheel-picker`).

- **`:build-logic/convention/*`** — Gradle convention plugins. Регистрируются в `build-logic/build.gradle.kts`:
  - `android.application.convention` — Android applicationId/SDK, JVM 19.
  - `android.library.convention` — `com.android.kotlin.multiplatform.library`, KMP Android target c `compileSdk = 36, minSdk = 26, namespace = "com.grippo"`, JVM 19.
  - `kotlin.multiplatform.convention` — `kotlin("multiplatform")`, `explicitApi()`, iosX64/iosArm64/iosSimulatorArm64, applyDefaultHierarchyTemplate, **глобальные `optIn(...)`** (Material3, Foundation, Coroutines, ForeignApi, Decompose Delicate/Experimental, kotlin.time, kotlin.uuid и т.д.).
  - `compose.multiplatform.convention` — Compose plugins, метрики/reports, `stabilityConfigurationFiles = compose-stability.conf` (файл в репо отсутствует — note ниже).
  - `koin.annotation.convention` — KSP, Koin Core + Annotations, `kspCommonMainMetadata`, `KOIN_CONFIG_CHECK = false`.
  - `room.convention` — KSP + Room compiler для Android и всех iOS targets, `room.schemaLocation = $projectDir/schemas`.
  - `ios.swiftpackage.convention` — собирает `XCFramework("shared")`, статический фреймворк, exports Decompose API + `:data-services:firebase`, `linkerOpts: -lsqlite3`.

---

## MVI contract (обязательный для каждого screen и dialog)

Каждый экран/диалог = пакет с **семью файлами** одинакового шаблона:

```
<feature>/
  <Name>Component.kt       // Decompose Component, владеет ViewModel, обрабатывает Direction
  <Name>Contract.kt        // @Immutable interface c on*-callbacks + companion object Empty
  <Name>State.kt           // @Immutable data class | data object | sealed interface
  <Name>Direction.kt       // sealed interface : BaseDirection (навигационные намерения)
  <Name>Loader.kt          // @Immutable sealed interface : BaseLoader (типы async-операций)
  <Name>ViewModel.kt       // BaseViewModel<State, Direction, Loader>, реализует Contract
  <Name>Screen.kt          // @Composable internal fun, принимает (state, loaders, contract)
```

### `BaseViewModel<STATE, DIRECTION : BaseDirection, LOADER : BaseLoader>`

Полное API, которое **обязательно** использовать вместо ручных coroutines/каналов:

- `state: StateFlow<STATE>` — read-only снапшот.
- `protected fun update(updateFunc: (STATE) -> STATE)` — единственный способ изменить state.
- `loaders: StateFlow<ImmutableSet<LOADER>>` — текущие активные операции для UI (button loading, skeleton, etc.).
- `protected suspend fun <T> withLoader(loader: LOADER?, block: suspend () -> T): T` — добавить loader на время блока.
- `navigator: Flow<DIRECTION>` — внутренний `Channel<DIRECTION>(CONFLATED)`. **Намеренно conflated**: rapid события могут схлопываться (см. `memory.md`).
- `protected fun navigateTo(destination: DIRECTION)` — кинуть Direction.
- `protected fun safeLaunch(dispatcher = Dispatchers.Main.immediate, processing = Processing.Infinity, loader = null, onError = {}, block)` — единственный способ стартовать корутину. По умолчанию `Infinity` для обычных операций.
- `protected fun <T> Flow<T>.safeLaunch(dispatcher = Dispatchers.Main.immediate, processing = Processing.WhileActive, onError = {})` — для подписки на потоки. По умолчанию `WhileActive` (поток засыпает на 1с после ухода экрана с экрана и перестаёт коллектиться).
- `protected enum class Processing { WhileActive, Infinity }` — `WhileActive` гасит upstream когда экран не в RESUMED.
- `attachActivation(flow)` / `detachActivation()` — управляются автоматически из `BaseComponent` через lifecycle.
- Ошибка из любого `safeLaunch`-блока ловится, логируется в `AppLogger.General.error`, кладётся в `FirebaseProvider.recordException`, **затем** прокидывается в `ErrorProvider.provide(exception, callback = onError)`. `ErrorProviderImpl` маппит её в `AppErrorState.*` и показывает `DialogConfig.ErrorDisplay`. UI в потоке исключения **не получает** ничего — для пользовательских ошибок use case возвращает `Result<T>` и ViewModel вызывает `.getOrThrow()` внутри `safeLaunch`.
- `onDestroy()` — закрывает `_navigator`, отменяет `coroutineScope`. Жизненным циклом владеет `InstanceKeeper` (через `componentContext.retainedInstance { ... }`), переживает recreate.

### `BaseComponent<DIRECTION : BaseDirection>`

- Делегирует `ComponentContext`, является `KoinComponent`. Создаётся в parent component через `<X>Component(componentContext = ..., ...)`.
- `protected abstract val viewModel: BaseViewModel<*, DIRECTION, *>`. Создаётся через `componentContext.retainedInstance { <X>ViewModel(getKoin().get(), ...) }` — это ключевой паттерн, **зависимости тянутся через `getKoin().get()`**, не через конструктор Component'а.
- На `lifecycle.doOnCreate` — подписывается на `viewModel.navigator` и зовёт `eventListener(direction)`; вызывает `viewModel.attachActivation(lifecycle.asActiveFlow())`.
- На `lifecycle.doOnDestroy` — `detachActivation`, `resultManager.clear`, отменяет coroutineScope.
- `protected abstract suspend fun eventListener(direction: DIRECTION)` — обработчик навигации; обычно `when (direction) { ... }` где Back/Close мапятся на лямбды-конструктора (`back()`/`close()`), а вложенные routes — на `navigation.push(...)` корневого Component.
- `protected fun observeResult<T>(key: ResultKey<T>, onResult)` / `sendResult(key, data)` — cross-component общение поверх Decompose stack (без прокидывания callback'ов вниз).
- `@Composable abstract fun Render()` — обычно: `val state = viewModel.state.collectAsStateMultiplatform()`, `val loaders = viewModel.loaders.collectAsStateMultiplatform()`, `<X>Screen(state.value, loaders.value, viewModel)`.
- `BackCallback(onBack = viewModel::onBack)` регистрируется в `init { backHandler.register(...) }` если экран обрабатывает back самостоятельно.

### `BaseScreen.kt` — корень UI

- `BaseComposeScreen(background, content)` — `Column` c фоном из `AppTokens.colors.background.screen`, `clickable` без indication для clear focus тапом по фону. Все экраны верхнего уровня обёрнуты этим.

### Pattern на каждый экран

Минимальный (statics-only screen, как `ProfileScreen` хост):

```kotlin
@Composable
internal fun ProfileScreen(
    component: ProfileComponent,
    state: ProfileState,
    loaders: ImmutableSet<ProfileLoader>,
    contract: ProfileContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    ChildStackCompose(
        modifier = Modifier.fillMaxSize(),
        stack = component.childStack,
        animation = platformAnimation(),
        content = { child -> child.instance.component.Render() }
    )
}
```

Полный (с состоянием и формой, как `ProfileBodyScreen`):

```kotlin
@Composable
internal fun ProfileBodyScreen(
    state: ProfileBodyState,
    loaders: ImmutableSet<ProfileBodyLoader>,
    contract: ProfileBodyContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    Toolbar(leading = Leading.Back(contract::onBack), ...)
    InputHeight(value = state.height, onClick = contract::onHeightPickerClick)
    InputWeight(value = state.weight, onClick = contract::onWeightPickerClick)
    val buttonState = remember(loaders, state.user, state.weight, state.height) {
        when {
            loaders.contains(ProfileBodyLoader.ApplyBodyChanges) -> ButtonState.Loading
            // ... derive from state
            else -> ButtonState.Enabled
        }
    }
    Button(state = buttonState, onClick = contract::onApplyClick, ...)
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ProfileBodyScreen(
            state = ProfileBodyState(/* stub data */),
            loaders = persistentSetOf(),
            contract = ProfileBodyContract.Empty,
        )
    }
}
```

### Inter-feature навигация: Routers

В `:ui-screen-features:screen-api`:

```kotlin
@Serializable
public sealed class RootRouter : BaseRouter {
    @Serializable public data object Home : RootRouter()
    @Serializable public data class Profile(val value: ProfileRouter) : RootRouter()
    @Serializable public data class Training(val stage: StageState) : RootRouter()
    // ...
}

@Serializable
public sealed class ProfileRouter : BaseRouter {
    @Serializable public data object Body : ProfileRouter()
    @Serializable public data object Settings : ProfileRouter()
    // ...
}
```

Decompose `childStack(serializer = RootRouter.serializer(), initialConfiguration = ..., key = "RootComponent", childFactory = ::createChild)`.
`StateKeeper` сериализует router в `Bundle`/iOS state — поэтому **все Router'ы и payload'ы внутри них (`StageState`, и т. п.) обязаны быть `@Serializable`**.

Внутри-фичевые routes — тоже sealed `*Router` рядом с фичей или в `:screen-api` если шарятся.

`Deeplink` — простой `enum class Deeplink(val key: String)` с `fromKey(key)`. Обработка в `RootViewModel` (`enqueueDeeplink` / `applyDeeplink` / `parseDeeplink`).

### Dialog навигация

Полностью отдельный subgraph, параллельный stack-навигатору экранов:

- `DialogController.show(config: DialogConfig)` — отправить config из любого ViewModel (контроллер инжектится в VM через Koin).
- `DialogConfig` — `@Serializable sealed class`, наследник реализует `override val key: String` через `buildKey(...)` (lengths-prefixed parts, чтобы `String|Int|Range` не конфликтовали).
- `onDismiss: (() -> Unit)?` — `@Transient`, не сериализуется.
- `dismissBySwipe: Boolean = true` — управляет `ModalBottomSheetProperties.shouldDismissOnBackPress` и swipe-to-dismiss.
- `DialogComponent` хостится в `:shared`, использует `SlotNavigation<DialogConfig>` + `childSlot(serializer = DialogConfig.serializer())`. Dialog-VM сам поддерживает **внутренний stack** в state для in-sheet навигации (push/pop без закрытия sheet).
- Pickers возвращают результат через **callback в config'е**: `DialogConfig.WeightPicker(initial = ..., onResult = { value -> update { it.copy(weight = WeightFormatState.of(value)) } })`. Callback также `@Transient`.
- Альтернативный путь: `BaseComponent.observeResult(key, onResult)` + `sendResult(key, data)` через `ResultEmitter`/`ResultManager` — для случаев, когда колбэк нельзя пробросить (например, между несвязанными dialog'ами).

---

## Data layer (полный путь от UI до сети)

UI-модуль зависит **только** от `:data-features:feature-api` и `:ui-core:state` (плюс свои дизайн-системные вещи). UseCase или `*Feature` инжектится в ViewModel через Koin.

```
ViewModel
  → TrainingFeature (interface in :feature-api)
    → TrainingFeatureImpl (in :data-features:trainings, @Single(binds=[TrainingFeature::class]))
      → TrainingRepository (internal interface)
        → TrainingRepositoryImpl (@Single(binds=[TrainingRepository::class]))
          → GrippoApi.<method>(body): Result<DTO>      // HTTP via :data-services:backend
          → TrainingDao.<query>(): Flow<Pack>         // Room via :data-services:database
```

### Repository pattern

`TrainingRepositoryImpl` (sample):

```kotlin
@Single(binds = [TrainingRepository::class])
internal class TrainingRepositoryImpl(
    private val api: GrippoApi,
    private val trainingDao: TrainingDao,
    private val draftTrainingDao: DraftTrainingDao,
    // ...
) : TrainingRepository {

    override fun observeTrainings(start, end): Flow<List<Training>> =
        trainingDao.get(from = DateTimeUtils.toUtcIso(start), to = DateTimeUtils.toUtcIso(end))
            .map { it.toDomain() }   // Pack -> Domain via :entity-to-domain

    override suspend fun getTrainings(start, end): Result<Unit> {
        val response = api.getTrainings(startUtc, endUtc)
        response.onSuccess { r ->
            val actualIds = r.mapNotNull { provideTraining(it) }
            // Reconcile cache: delete stale rows in range, keep what server returned
            if (actualIds.isEmpty()) trainingDao.deleteByCreatedAtRange(startUtc, endUtc)
            else trainingDao.deleteByCreatedAtRangeExceptIds(startUtc, endUtc, actualIds)
        }
        return response.map {}
    }
}
```

Стандартные паттерны:
- **Observe** возвращает `Flow<Domain>` из DAO. Никогда не из API.
- **Get/Set/Update/Delete** возвращает `Result<T>`, ходит в API, на успех обновляет DAO. UI пишет `withLoader { feature.getTrainings(...).getOrThrow() }`.
- **Range reconciliation**: после `getTrainings` чистим всё в диапазоне кроме того, что сервер вернул (`deleteByCreatedAtRangeExceptIds`). Это снимает "удалили на другом девайсе" расхождения.
- **Drafts** живут только в БД (`draftTrainingDao`), не ходят на сервер.

### Backend layer

`GrippoApi` — плоский класс, **один метод на endpoint**, секции через `/* * * * * Auth service * * * * */` комментарии:

```kotlin
@Single
public class GrippoApi internal constructor(private val client: BackendClient) {

    public suspend fun getTrainings(start: String, end: String): Result<List<TrainingResponse>> {
        return request(method = HttpMethod.Get, path = "/trainings",
            queryParams = mapOf("start" to start, "end" to end))
    }

    private suspend inline fun <reified T> request(
        method: HttpMethod, path: String,
        body: Any? = null, queryParams: Map<String, String>? = null,
    ): Result<T> = runCatching {
        client.invoke(method, path, body, queryParams).body()
    }
}
```

`BackendClient` (Ktor):
- `defaultRequest`: `host = "grippo-app.com"`, `URLProtocol.HTTPS`, JSON content-type, `Accept-Language = AppLocale.current()`.
- `HttpTimeout`: 10s (request, connect, socket).
- `Logging` plugin (`LogLevel.ALL`, custom `ClientLogger`).
- `Auth` plugin с custom `TokenProvider : AuthProvider`.
- `withContext(Dispatchers.IO)` оборачивает каждый `invoke`.

`TokenProvider` — bearer + refresh:
- `addRequestHeaders` всегда читает свежий токен из `TokenDao` (через `userActiveDao` для current user) и ставит `Bearer <token>`.
- `refreshToken(response)` — refresh с `Mutex` (один refresh за раз), `withTimeout(10s)`. Параллельные 401-запросы ждут результат через `waitForOngoingRefresh`.
- Refresh идёт через `client.submitForm` с `attributes.put(AuthCircuitBreaker, Unit)` (Ktor сам не пытается refresh-нуть refresh).
- Если backend вернул 401 на refresh → `RefreshUnauthorizedException` → токены удаляются из БД (`tokenDao.delete(userId)`) → `RootViewModel.authorizationFeature.getToken()` увидит `null` и сделает `navigateTo(RootDirection.Login)`.
- `retryWithBackoff(maxAttempts = 3, initialDelay = 500ms, factor = 2.0)` для transient ошибок (но не для `RefreshUnauthorizedException`).

DTOs: `package com.grippo.services.backend.dto.<area>`, файл `*Response.kt` или `*Body.kt`, `@Serializable data class` со всеми полями `@SerialName` и **nullable + default `= null`** (защита от частичных ответов).

### Database layer

`@Database(entities = [...], version = 5, exportSchema = true)` в `Database.kt`. `@TypeConverters(StringListConverter::class)` для `List<String>` → `pipe|delimited|string`.

Entities — плоские `data class` в `package com.grippo.services.database.entity.*`:

```kotlin
@Entity(
    tableName = "training",
    indices = [Index(value = ["profileId"])],
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["profileId"],
        childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)],
)
public data class TrainingEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val duration: Long,
    val createdAt: String,
    // ...
)
```

DAO — `interface` с `@Dao`, `@Query("...")` (raw SQL), `@Insert(onConflict = REPLACE)`, `@Transaction` для multi-step операций. Возвращают `Flow<...>` для observe и `suspend` для модификации.

`@Embedded` + `@Relation` модели — в `models/*Pack.kt`:

```kotlin
public data class TrainingPack(
    @Embedded val training: TrainingEntity,
    @Relation(parentColumn = "id", entityColumn = "trainingId", entity = ExerciseEntity::class)
    val exercises: List<ExercisePack> = emptyList(),
)
```

Миграции — `Migration<N>To<N+1>` объекты в `migrations/`, собраны в `DatabaseMigrations.all`. **Не трогать без запроса** — выехали в проде.

`DatabaseBuilder` (expect/actual):
- Android: `Room.databaseBuilder<Database>(context, name = dbFile.absolutePath).addMigrations(...).fallbackToDestructiveMigration(dropAllTables = true).setQueryCoroutineContext(Dispatchers.IO).build()` + `openHelper.writableDatabase` (warm up).
- iOS: путь — `NSDocumentDirectory + "/grippo_database.db"`, `setDriver(BundledSQLiteDriver())`, тот же `fallbackToDestructiveMigration(dropAllTables = true)`.

### Mappers

7 направлений = 7 модулей. Названия пакетов = `<source>.<target>.<area>`:
- `com.grippo.dto.entity.training` (in `:dto-to-entity`)
- `com.grippo.entity.domain.training` (in `:entity-to-domain`)
- `com.grippo.domain.state.training` (in `:domain-to-state`)
- `com.grippo.state.domain.training` (in `:state-to-domain`)
- `com.grippo.domain.dto.training` (in `:domain-to-dto`)
- `com.grippo.domain.entity.training` (in `:domain-to-entity`)
- `com.grippo.dto.domain.<area>` (in `:dto-to-domain`)

Имя функции — top-level extension: `<Source>.toEntity()`, `<Source>.toDomain()`, `<Source>.toState()`, `<Source>.toBody()`. Множественные — `List<Source>.toEntities()`/`toDomain()`/`toState()`. Nullable варианты — `toEntityOrNull(): T?`.

**Каноничный mapping с null-friendly DTO** (DTO поля nullable из-за защиты, но domain/entity — non-null):

```kotlin
public fun ExerciseResponse.toEntityOrNull(): ExerciseEntity? {
    val entityId = AppLogger.Mapping.log(id) { "ExerciseResponse.id is null" }
        ?: return null
    val entityName = AppLogger.Mapping.log(name) { "ExerciseResponse.name is null" }
        ?: return null
    // ...
    return ExerciseEntity(id = entityId, name = entityName, /* ... */)
}

public fun List<ExerciseResponse>.toEntities(): List<ExerciseEntity> =
    mapNotNull { it.toEntityOrNull() }
```

`AppLogger.Mapping.log(value) { msg }` — если value не null → возвращает value, иначе → пишет `[MAPPING] <msg> (file:line)` и возвращает null. Каждое поле логируется отдельно для диагностики.

---

## Code style and naming

### Файлы и именование

- **Public API классы** — `public` явно (требуется `explicitApi()`).
- **Internal-by-default** — всё, что не часть feature-api, помечается `internal`. Реализации интерфейсов — суффикс `Impl`.
- Composable — `PascalCase`. ViewModel callback — `onXxx` (`onApplyClick`, `onWeightPickerClick`, `onBack`).
- Файл = один класс/интерфейс. Group-файл допустим только для tightly-related sealed классов в одном файле (как `TrainingResponse`/`ExerciseResponse`/`IterationResponse` — одна семья DTO).
- DTO — `@Serializable public data class <Name>Response` или `<Name>Body`, поля `@SerialName("...")`, **все nullable с default `= null`**.
- Domain models — `public data class` с non-null полями.
- Entity — `public data class <Name>Entity` с `@Entity`, `@PrimaryKey`, `@Column(name = "snake_case")` если нужен явный override.
- State — `@Immutable internal data class <Feature><Subscreen>State` (или `data object` если без полей, или `sealed interface` с подвариантами).
- Direction — `internal sealed interface <Feature>Direction : BaseDirection` с `data object`/`data class` подвариантами.
- Loader — `@Immutable internal sealed interface <Feature>Loader : BaseLoader` с `@Immutable data object` подвариантами.
- Contract — `@Immutable internal interface <Feature>Contract { ...; companion object Empty : <Feature>Contract { ... } }`. Empty — для preview.
- Use case — `public class <Verb><Noun>UseCase(...)` с одним `public suspend fun execute(...): Result<T>`.

### Пакеты

- Базовая схема: `com.grippo.<area>.<feature>` (`com.grippo.profile.body`, `com.grippo.authorization.registration.credential`).
- **Существующий разнобой:** некоторые модули в `data-features/`, `data-mappers/`, `data-services/`, `dialog-api`, `weight-picker` используют пакеты с **точкой в имени директории** (`com/grippo/data.features.trainings/`, `com/grippo/dto.entity.training/`, `com/grippo/dialog.api/`, `com/grippo/weight.picker/`). Внутри `package` декларация — обычная (`package com.grippo.data.features.trainings`). Намеренно, не пересматриваем — но **новые** модули пишем без точек в директориях.
- `internal` функции в публичном модуле — в подпакете `internal/`.

### Kotlin

- Coroutines — **только** `safeLaunch` / `Flow.safeLaunch`. Никаких `viewModelScope.launch`, `runBlocking`, `GlobalScope`.
- Collections в State и Loaders — **только** `kotlinx-collections-immutable` (`ImmutableList`, `ImmutableSet`, `PersistentList`). Никаких `List`/`Set`/`MutableList` в `@Immutable`-классах.
- Errors из use case — `Result<T>`, `.getOrThrow()` внутри `safeLaunch`, исключение поднимается в `BaseViewModel.sendError` → `ErrorProvider`.
- Каждый `@Composable internal fun <X>Screen` принимает три аргумента в одном порядке: `state, loaders, contract` (и иногда `component` для cross-feature навигатора).
- `@AppPreview` + `PreviewContainer` для preview. Preview всегда использует `<X>Contract.Empty` и стабовые данные (`stub*()` функции из `:ui-core:state`).
- `remember(...)` для derived button state (`val buttonState = remember(loaders, state.x, state.y) { when { ... } }`).
- Не вызывать `Composable` функции из не-Composable кода через locale-strings — для VM-стороны использовать `StringProvider.get(Res.string.x)` (инжектится).

### Compose specifics

- Recomposition: state классы — `@Immutable`/`@Stable`, коллекции immutable. Compose stability metrics включены в convention plugin.
- Глобальные `optIn`-ы (Material3, Foundation, Coroutines, ForeignApi и т.д.) уже включены в `KotlinMultiplatformConventionPlugin` — **не дублировать в файлах** через `@OptIn`.
- `LaunchedEffect(Unit)` для одноразовых side-effect навигации **запрещён** — навигация идёт через `Direction` + `eventListener`. `LaunchedEffect` допустим только для не-навигационных side-effect'ов (запуск анимации, init системного API).
- `BottomSheetToolbar` для всех bottom sheet'ов; обычный `Toolbar` для экранов.

---

## Dependency injection (Koin)

Каждый модуль с инжектируемым кодом:

```kotlin
@Module(includes = [BackendModule::class, DatabaseModule::class])
@ComponentScan
public class TrainingsFeatureModule
```

- `@Module(includes = [...])` — задаёт транзитивные включения.
- `@ComponentScan` — KSP сканирует пакет за `@Single`/`@Factory`/`@Scoped` в реализациях.
- `@Single(binds = [TrainingFeature::class])` на `internal class TrainingFeatureImpl(...) : TrainingFeature` — единственный способ объявлять реализацию.
- `@Factory` для cheap, per-call инстансов (например, `OperationManager`).
- `@InjectedParam` — для параметров, которые передаются в момент `get { parametersOf(...) }`. Используется `BaseViewModel` для прокидывания `coroutineScope` в `OperationManager` и `ResultManager`.

**Запрещено** руками писать `module { single { ... } }` DSL для новых модулей. Использовать только аннотации.

Все Koin-модули собираются в `:shared/Koin.kt`:

```kotlin
public object Koin {
    public fun init(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
        KoinPlatformTools.defaultContext().startKoin {
            appDeclaration()
            modules(
                ContextModule().module,
                DatabaseModule().module,
                BackendModule().module,
                // ... все модули перечисляются явно
            )
        }
}
```

Новый модуль добавляется в этот список **руками**.

---

## Performance budgets and priorities

Явных budget'ов в коде нет. Дефолтные приоритеты:

- Не блокировать main thread. Все IO/CPU — через `safeLaunch(dispatcher = Dispatchers.IO)` или `withContext(Dispatchers.IO)` (как `BackendClient`).
- Стабильность recomposition: state — `@Immutable`/`@Stable`, immutable collections везде, метрики Compose в `build/compose-metrics`.
- Не ломать XCFramework: любое изменение публичного API в `:shared`/`:ui-core:foundation`/`:design-system`/`:data-features:feature-api` проверяется на iOS — `./gradlew :shared:assembleSharedDebugXCFramework` должен проходить.
- `kotlin.native.binary.gc=cms` (concurrent mark+sweep) включён для уменьшения stop-the-world пауз на iOS.
- `kotlin.native.binary.smallBinary=true` для меньших iOS-фреймворков.
- `org.gradle.workers.max=1` для уменьшения пика памяти при native release-линковке.

---

## Testing strategy

Тестовых source sets и `*.kt` тестов в репозитории нет. Не добавлять без явного запроса.

---

## Locked architectural decisions

- Compose Multiplatform для UI на обеих платформах.
- Decompose 3.5 для навигации и lifecycle (не Compose Navigation, не Voyager).
- Koin 4.2 + annotations + KSP для DI (не Hilt, не Anvil, не Metro).
- Room 2.8 multiplatform для persistence (не SQLDelight).
- Ktor 3.4 для network.
- `kotlinx-serialization` для JSON и Decompose state.
- Pattern Component / Contract / State / Direction / Loader / ViewModel / Screen — обязательный для нового screen/dialog.
- `BaseViewModel` использует CONFLATED `Channel` для navigation — известный trade-off с rapid-событиями.
- `fallbackToDestructiveMigration(dropAllTables = true)` на обеих платформах — намеренно.
- Convention plugins в `build-logic/` — единственный способ конфигурировать модули. Module-level `build.gradle.kts` содержит только `plugins { id("...convention") }` и `kotlin { sourceSets.commonMain.dependencies { ... } }`.
- Все Koin-модули собираются в `:shared/Koin.kt` явно.
- 7 направлений мапперов в 7 модулях `:data-mappers:*`. Inline-конвертация DTO↔Domain↔State в ViewModel запрещена.
- DTO-поля все nullable + default `= null`. Mapping null'ов идёт через `AppLogger.Mapping.log(...) ?: return null`.
- API контракт владеет backend, codegen НЕ используем — DTO в `data-services/backend/dto` пишутся руками.

---

## Scope discipline

- Не двигать модули и не менять `settings.gradle.kts` без запроса. Структура из ~70 модулей — намеренная.
- Не менять `:shared/Koin.kt` порядок/состав без запроса (composition root).
- Не править `build-logic/convention/*` в проходе при правке фичи. Convention plugins — отдельная задача.
- Не менять `gradle/libs.versions.toml` версии библиотек молча.
- Не трогать миграции Room (`Migration*To*.kt`) без явного запроса.
- Не редактировать `secure/` и `local.properties`.
- Не править `TokenProvider` token-refresh logic без запроса (там mutex + circuit breaker + retry).
- Не вводить новые `@OptIn` в исходниках — добавлять глобально в `KotlinMultiplatformConventionPlugin`.

---

## When to stop and ask

- Любой новый модуль или удаление существующего.
- Изменение `BaseViewModel`, `BaseComponent`, `BaseScreen`, `OperationManager`, `ErrorProvider`, `ResultManager`.
- Изменение `RootComponent`/`RootRouter` или публичных `*Router` sealed-классов в `:ui-screen-features:screen-api`.
- Новая Room миграция или изменение `@Entity` схемы.
- Новая зависимость в `gradle/libs.versions.toml`.
- Любое изменение в `:androidApp`/`:iosApp` shell за пределами очевидных багов.
- Изменение DTO в `:data-services:backend/dto/...` — это backend контракт, синхронизировать с `grippo-backend` руками.
- Изменение `BackendClient`/`TokenProvider`/`HttpModule` (network core).
- Добавление поля в `DialogConfig` (нужно правильно сериализовать `key`).

---

## Anti-patterns (что отказываться писать)

- Прямой `viewModelScope.launch`, `runBlocking`, `GlobalScope.launch`, `CoroutineScope().launch`. Только `safeLaunch` / `Flow.safeLaunch`.
- Mutable коллекции (`MutableList`, `mutableStateListOf`, `mutableMapOf`) в `@Immutable` state.
- Бизнес-логика в `Component`, `Screen`, мапперах, или DTO. Логика — в `ViewModel` или domain слое (`*Repository`/`*UseCase`/`*FeatureImpl`).
- Mapping вне `:data-mappers:*`. Inline-конвертация в ViewModel/Screen — запрещена.
- Прямая зависимость UI-модуля от `:data-services:*` — только через `:data-features:*`.
- Создание Koin-модуля через ручной `module { ... }` DSL для нового модуля.
- `LaunchedEffect(Unit)` для навигации — используем `Direction` + `eventListener`.
- Ловля исключений в ViewModel руками (`try { ... } catch { ... }`) — пускай идёт через `safeLaunch` → `ErrorProvider`. Исключение: domain-логика (`Result.onSuccess { ... }` после `api.<x>()`).
- Захардкоженные строки в UI — только через `AppTokens.strings.res(Res.string.x)` (Composable) или `StringProvider.get(...)` (VM).
- Прямой вызов Koin `get()` в Screen — DI идёт только через `componentContext.retainedInstance { <X>ViewModel(getKoin().get(), ...) }` в Component.
- Обращение к `Context`/`Activity` напрямую из `commonMain` — через `:toolkit:context` (`NativeContext`).
- Скрытое схождение feature-модулей через transitive `api` — все нужные зависимости перечислять в своём `build.gradle.kts`.
