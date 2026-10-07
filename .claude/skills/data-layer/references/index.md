# Data-layer references — routing table

Self-contained reference pack for the `data-layer` skill. These files carry the skill's own
normative rules and read no external rule docs at runtime — a builder reads here.

Route by task kind; each row lists the file to read first, then any supporting files.

| Task kind | Read first | Also |
|---|---|---|
| New data feature (full recipe) | [`cookbook-data-feature.md`](cookbook-data-feature.md) | [`module-structure.md`](module-structure.md), [`repositories.md`](repositories.md) |
| Repository shape / data flow (UI → network) | [`repositories.md`](repositories.md) | [`module-structure.md`](module-structure.md) |
| `:data-features:*` module shape, feature-api split, domain enums / sealed types / strict typing | [`module-structure.md`](module-structure.md) | [`repositories.md`](repositories.md) |
| Add endpoint + DTO (full recipe) | [`cookbook-endpoint.md`](cookbook-endpoint.md) | [`dtos-and-api.md`](dtos-and-api.md), [`backend-client.md`](backend-client.md) |
| `<Product>Api` flat shape, `request<T>` helper, DTO rules | [`dtos-and-api.md`](dtos-and-api.md) | [`backend-client.md`](backend-client.md) |
| Ktor `BackendClient`, plugins, `invoke(...)`, multi-env | [`backend-client.md`](backend-client.md) | [`dtos-and-api.md`](dtos-and-api.md) |
| Database shape, `DatabaseBuilder`, destructive fallback; Room entities, DAOs, packs, TypeConverters | [`persistence-room.md`](persistence-room.md) | — |
| Room migration (full recipe), migration rules, schema export, `DatabaseMigrations.all` | [`cookbook-room-migration.md`](cookbook-room-migration.md) | [`persistence-room.md`](persistence-room.md) |
| DataStore preference (keys + accessors on Repository) | [`datastore.md`](datastore.md) | [`module-structure.md`](module-structure.md) |
| Token provider, refresh, auto-logout (load-bearing) | [`auth-session.md`](auth-session.md) | [`backend-client.md`](backend-client.md), [`error-pipeline.md`](error-pipeline.md) |
| `:data-services:*` rules (backend/database/datastore/firebase/auth) | [`module-structure.md`](module-structure.md) | — |
| Error pipeline (no raw `Throwable` leak; Repository / UseCase authoring) | [`error-pipeline.md`](error-pipeline.md) | [`repositories.md`](repositories.md) |

## File map

| File | Covers |
|---|---|
| [`module-structure.md`](module-structure.md) | `:data-features:*` + `:data-services:*` module layout, feature-api contents, domain enums/sealed types, strict typing, per-platform pattern, service rules |
| [`repositories.md`](repositories.md) | Data flow layers, Feature interface/impl, Repository interface/impl, common operations, drafts, module DI, anti-patterns |
| [`backend-client.md`](backend-client.md) | Ktor `BackendClient` class shape, plugins (HttpTimeout/Logging/Auth/ContentNegotiation), `defaultRequest`, `invoke(...)`, multi-env |
| [`auth-session.md`](auth-session.md) | `TokenProvider` — bearer header, mutex 401 refresh, `retryWithBackoff`, circuit breaker, auto-logout |
| [`dtos-and-api.md`](dtos-and-api.md) | `<Product>Api` flat one-method-per-endpoint, `request<T>` helper, DTO/Body rules, `ClientLogger` |
| [`persistence-room.md`](persistence-room.md) | `@Database` shape, `DatabaseBuilder` expect/actual, DI, entities, DAOs, `*Pack` models, TypeConverters |
| [`datastore.md`](datastore.md) | DataStore module/factory, Repository integration, keys naming, what goes in DataStore |
| [`error-pipeline.md`](error-pipeline.md) | Where errors originate (network validator, domain, unknown), `AppError` hierarchy, Repository/UseCase authoring rules |
| [`cookbook-data-feature.md`](cookbook-data-feature.md) | End-to-end add-a-data-feature recipe (9 steps) |
| [`cookbook-endpoint.md`](cookbook-endpoint.md) | End-to-end add-an-endpoint recipe + endpoint patterns |
| [`cookbook-room-migration.md`](cookbook-room-migration.md) | Migration rules + add-migration recipe + migration patterns |

## Out of this skill's scope (handed off)

- **Mapper authoring** (`:data-mappers:*` per-direction modules, null-safety) → mapper builder /
  the mappers skill. The DTO↔Entity↔Domain boundary rules a data-layer builder must honor are
  embedded in [`module-structure.md`](module-structure.md) (strict typing) and the cookbooks.
- **Koin composition root / annotations** → the DI skill. The
  `@Single(binds = […])` / `@ComponentScan` / `FeatureApiModule` rules used here are embedded.
- **UI-side error dialog mapping** (`ErrorProvider`, `AppErrorState`) → presentation skill.
