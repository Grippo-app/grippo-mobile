# Repository and Feature ownership

The live layout is `data-features/<feature>/src/commonMain/kotlin/`; public models,
Feature interfaces and UseCases live in `data-features/feature-api/`.

- Repository owns service access, mapping, cache writes and reconciliation. FeatureImpl exposes
  the public contract; a composing UseCase owns complete domain operations spanning Features.
- Observation returns domain Flow from the authoritative local owner. One-shot methods preserve
  their actual Result/throwing shape; do not mechanically convert all methods into one pattern.
- MUST apply the canonical [operation outcome/cache rules](operation-outcomes.md) before API-backed
  cache writes, range reconciliation or deciding that optional readback failure is nonblocking.
  Local drafts/preferences follow their owner contract; they do not require server confirmation.
- Repository never leaks DTO/Entity through Feature contracts. Compose directional mapper calls
  here rather than making mapper modules depend on each other.

Use `data-features/trainings/` and `data-features/user/` as source examples, inspecting their
actual operation semantics rather than copying every dependency or failure policy.

## UseCase shapes (verified)

The 25 public UseCases live in `data-features/feature-api/.../<area>/*UseCase.kt` and are all
registered as `single { }` entries of the hand-written `FeatureApiModule`
(`@Module public class FeatureApiModule { @get:JvmName("module") public val module: ModuleObject = module { ... } }`,
listed in `Koin.init` as `FeatureApiModule().module`; see [DI](../../di-modules/SKILL.md)).
Their shapes differ and each is preserved:

- Result-returning commands (4): `SetTrainingUseCase.execute(training): Result<String?>`,
  `UpdateTrainingUseCase.execute(id, training): Result<String?>`,
  `DeleteTrainingUseCase.execute(id): Result<Unit>`, `UpdateWeightUseCase.execute(value): Result<Unit>`.
- Throwing commands: `LoginUseCase.loginEmail/loginGoogle/loginApple(...): Boolean`,
  `RegisterUseCase.createAccount(email, password): Boolean`, `LogoutUseCase.execute()`,
  `CreateProfileUseCase.execute(profile)`, `DeleteProfileUseCase.execute()`; they unwrap Feature
  results with `getOrThrow()` and a `false` return is a domain outcome, not an error.
- Flow or pure calculators: `UserExerciseExamplesUseCase.execute(params): Flow<...>`,
  `ExerciseValidatorUseCase.observeContext(...)`/`execute(...)` and the metrics use cases
  (`fromTrainings`, `fromExercises`, `digest`, `trainingTimeline`, ...).

A new command use case is `public suspend fun execute(...): Result<T>`; existing ones keep their
shape. ViewModels unwrap a required Result with `getOrThrow()` inside `safeLaunch`
([operation outcomes](operation-outcomes.md)).
