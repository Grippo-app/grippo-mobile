# Architecture Overview

> The cross-cutting pattern detail (MVI / navigation / dialogs / data flow / error
> pipeline / process death) lives in the per-concern skills (`ui-feature`,
> `data-layer`, `di-modules`); this page is the high-level map only.

## Layers and dependency direction

```
:androidApp / :iosApp                       (thin shells, no business logic)
        ↓
     :shared                                 (composition root; includes every other module)
        ↓
┌───────────────────────────────────────────────────────────────────┐
│ :ui-screen-features:* → :ui-dialog-features:*                    │
│        ↓                       ↓                                 │
│ :ui-core:foundation, :ui-core:state, :ui-core:error              │
│        ↓                       ↓                                 │
│ :design-system:components → :design-system:core                  │
│                              ↓                                   │
│              :design-system:resources:provider                   │
└───────────────────────────────────────────────────────────────────┘
        ↓
:data-features:feature-api                   (the ONLY data-layer surface UI sees)
        ↓
:data-features:<feature>                     (Repository + FeatureImpl, internal)
        ↓
:data-services:{backend, database, datastore, firebase, *-auth}
        ↓
:toolkit:*                                   (http-client, logger, serialization, ...)
```

## Hard rules

(See the platform-build-toolkit skill, references/module-structure.md § directional dependency rules for the full list.)

- A UI module **never** depends on `:data-services:*` directly. The boundary is
  `:data-features:feature-api` (narrow SDK-style exceptions are listed in
  the validation-gates skill, references/forbidden-patterns.md).
- `:data-features:feature-api` is pure interfaces + domain models. It does not
  depend on `:data-services:*`.
- `:toolkit:*` depends on nothing except other `:toolkit:*`. Two narrow,
  pure-type exceptions are tolerated: `:toolkit:http-client` →
  `:ui-core:error:error-provider` (so the response validator can throw `AppError`
  subtypes), and `:toolkit:date-utils` → `:design-system:{core, resources:provider}`
  (locale-aware formatting). Crash reporting via `:data-services:firebase` is
  wired through `:ui-core:foundation`, not toolkit.
- `:design-system:*` does not touch the data layer.
- `:shared` is the only module that imports "everything".

## Module groups (high level)

| Group | Purpose | Examples |
|---|---|---|
| **App shells** | Platform entry points | `:androidApp`, `:iosApp` |
| **`:shared`** | Composition root + root navigator | `Koin.kt`, `RootComponent`, `RootViewModel`, `DialogComponent` |
| **Design system** | Tokens + atomic components | `:design-system:{core, components, preview, resources:*}` |
| **UI core** | Base classes + reusable state | `:ui-core:{foundation, state, error:*}` |
| **UI screen features** | Full-screen flows | `:ui-screen-features:{authorization, home, profile, ...}` |
| **UI dialog features** | Bottom-sheet flows | `:ui-dialog-features:{note-picker, confirmation, ...}` |
| **Data services** | Low-level I/O (HTTP, DB, DataStore, auth) | `:data-services:{backend, database, datastore, ...}` |
| **Data features** | Domain layer (interface + impl) | `:data-features:{feature-api, notes, user, ...}` |
| **Data mappers** | One module per direction | `:data-mappers:{dto-to-entity, entity-to-domain, ...}` |
| **Toolkit** | Platform-aware utilities | `:toolkit:{context, http-client, logger, ...}` |
| **Compose libs** | Reusable Compose widgets outside design system | `:compose-libs:{chart, konfetti, wheel-picker, ...}` |
| **Build logic** | Gradle convention plugins | `:build-logic:convention` |

The detail for each cross-cutting pattern (MVI seven-file template, Decompose
navigation, dialogs, cross-component results, data flow, error pipeline,
process-death restoration) lives in the per-concern skills; this page is the map.
Each owning skill contains the rules in detail, with verbatim API signatures,
package paths, and code shape requirements.
