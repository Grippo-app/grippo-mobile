# When to stop and ask

> Owned by this skill — the escalation source for `scope-leak-validator` and the
> reviewer. Do not drop a stop point.

> **Scope:** these stop points govern post-bootstrap task work. The `launch.md`
> bootstrap runs unattended — during bootstrap nothing here licenses a question;
> a would-be stop point there ends the run with a `BLOCKED: <reason>` report
> instead (launch.md § "Unattended run — never ask").

Some changes require a deliberate decision, not a confident merge. The architecture has many committed choices; reviewing them takes longer than implementing a workaround. The list below covers the **stop points**.

## Stop and ask before touching

### Modules and structure

- **Adding a new module outside the explicit cookbook/bootstrap flows.** Touches `settings.gradle.kts`, `:shared/build.gradle.kts`, and `:shared/Koin.kt`. Naming and placement set patterns for future modules.
- **Removing or renaming an existing module.** Breaks transitive imports.
- **Reorganizing the dependency graph** (changing what depends on what). The directional rules in the platform-build-toolkit skill, references/module-structure.md are load-bearing.

### Base classes

- **Changing `BaseViewModel`'s API** (adding/removing/renaming `safeLaunch`, `Processing` modes, `withLoader`, etc.). Touches every screen.
- **Changing `BaseComponent`'s lifecycle wiring**. Affects how Components mount, retain, destroy.
- **Changing `BaseComposeScreen`**. Affects every screen.
- **Changing `OperationManager`'s CoroutineExceptionHandler.** The error pipeline depends on it.
- **Changing `ResultManager` / `ResultEmitter` channel semantics.** Single-consumer vs multi-consumer trade-offs are deliberate.

### Navigation

- **Changing `RootRouter`'s top-level routes.** Touches every feature's child factory.
- **Changing the `Deeplink` enum.** External callers (push notifications, custom-url-scheme intents) rely on stable keys.
- **Changing the dialog navigation mechanism** (e.g. moving from slot to stack). Affects every dialog feature.

### Data layer

- **Changing `BackendClient` / `TokenProvider`.** The mutex + retry + circuit-breaker logic is fragile; changes here can break auth-required calls.
- **Changing `<Product>Api`'s `request<T>` helper.** Every endpoint method calls it.
- **Adding a new Room migration.** Schema bumps require coordination with the destructive fallback policy. See the data-layer skill, references/cookbook-room-migration.md.
- **Changing `@Entity`'s `id` type** (e.g. from `String` to `Long`). Cascading impact on DAOs, Packs, mappers.
- **Adding `fallbackToDestructiveMigration(dropAllTables = false)`** (changing the policy). The "drop everything" choice is deliberate.

### Design system

- **Adding a new top-level `AppTokens` category.** Adding a property is fine; a new category restructures the namespace.
- **Renaming or removing a token slot** that's already in use. Touches every consumer.
- **Removing a `:design-system:components` widget** that's still consumed.
- **Adding a new font or typeface.** Bloats the binary; design coordination required.

### Build

- **Bumping Kotlin / AGP / Compose Multiplatform major versions.** Always coordinated.
- **Changing `compileSdk` / `minSdk` / `targetSdk`.** Affects every Android target and may require a Play Store update.
- **Disabling `configuration-cache` to fix a build issue.** Find the offending plugin instead.
- **Disabling `explicitApi()`.** Every public declaration must keep its visibility modifier.
- **Adding a new convention plugin.** Once added, it's hard to remove without breaking module-level scripts.
- **Disabling `fallbackToDestructiveMigration`** on production data.

### DI

- **Changing `:shared/Koin.kt`'s module composition.** Adding is fine (with discussion); removing or reordering can break startup.
- **Adding a new Koin scope.** Custom scopes are a complexity tax — usually `@Single` + `@Factory` suffice.

### Conventions

- **Changing the naming conventions** in the design-system skill, references/naming.md. Renames propagate widely.
- **Changing the file layout for MVI** (the seven-file rule). Existing screens would need rewrites.
- **Changing the mapper directions** (the seven modules). Adding or removing breaks the discoverability property.

### External contracts

- **Changing a DTO that the backend sends.** Coordinate with backend; backend owns the API contract.
- **Renaming a `Deeplink` key.** External marketing campaigns, push payloads, deeplinks-from-server reference these.
- **Changing the iOS XCFramework name (`shared`).** Xcode references break.

## How to ask

Open a discussion (Slack, Linear, GitHub issue — wherever team coordination happens) with:

1. **What you want to change.**
2. **Why** (motivation, alternative ruled out).
3. **Blast radius** — which modules / files / behaviors are affected.
4. **Migration plan** — staged change, single commit, requires deprecation window?
5. **Reversibility** — can it be rolled back if it goes wrong?

For changes that touch shipped users (DB migrations, deeplinks, API contracts), coordination with backend + design + product is mandatory.

## What you can change without asking

- **Adding a screen** in an existing feature module (follow the seven-file pattern).
- **Adding a new use case** in `:data-features:feature-api`.
- **Adding a new repository method** + corresponding feature method.
- **Adding new strings** to `strings.xml`.
- **Adding new color slots** within existing `AppColor` groups.
- **Adding new dimensions** within existing `AppDp` groups.
- **Adding a new dialog** following the existing `DialogConfig` pattern.
- **Adding a new typography token** (`b13Med`-style) when design requests one.
- **Adding a new mapper file** within an existing `:data-mappers:*` direction.
- **Rewriting a `:design-system:components` widget** against its Figma component spec (the variant/property axes of its design entry / frozen binding spec), with at most one canonical call-site migration in `:ui-screen-features:*` per task.
- **Fixing a bug** in any of the above without expanding scope.

## Default to asking

When in doubt, ask. The cost of a 5-minute Slack thread is low; the cost of an unwanted architecture change is high (review cycle + rollback risk).

This template rewards "let me check before I do that" over "I assumed and proceeded".
