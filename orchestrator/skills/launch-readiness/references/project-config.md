# Project config — core scalar source of truth

> The `codexEnabled` / `verifyEnabled` review-gate fields, the Figma fields
> (`figmaEnabled`, `figmaLibraryUrl`), and the backend-contract fields
> (`backendContractEnabled` plus the structured source manifest) are owned by
> the review / figma / backend-contract skills respectively — see those skills
> and the `orchestrator/project-config.md` frontmatter for those fields, and Step 0 of
> `orchestrator/launch.md` for the default each takes when unasked at bootstrap.

The frontmatter of `orchestrator/project-config.md` is the live config block.
Every value in a fresh template is a placeholder or a neutral default:

```yaml
productName: <Product>
productPackage: com.<org>.<product>
apiClassName: <Product>Api
backendHost: <product-domain>
applicationId: com.<org>.<product>
iosFrameworkName: shared
iosEnabled: true
firebaseEnabled: true
codexEnabled: auto
verifyEnabled: auto
prelaunch: true
supportedLocales:
  - en
typefaceFactory: <typeface>
featuresWithRootComponentSuffix: []
diHandWrittenModules: []
figmaEnabled: false
figmaLibraryUrl: <figma-library-url>
screenshotPixelGate: strict
backendContractEnabled: auto
```

## Fresh-project state

Every value in the frontmatter above is a placeholder or a neutral default.
**Before invoking any skill** (`task-orchestrator`, the builder skills, the
validation gates), replace
the required identity/build placeholders with project-specific values per
`orchestrator/launch.md` Step 1.5. The optional Figma placeholder may remain
until its integration is enabled. Fresh projects start without
`orchestrator/api-contract/environments.json`; the Backend panel creates it
when the first source is added, and it owns structured Backend sources from
then on.
`figmaTokenMode` is an optional later field
for projects that need to document variable/style token sourcing; it is not
required in fresh frontmatter. Empty arrays
(`featuresWithRootComponentSuffix: []`, `diHandWrittenModules: []`) stay empty
until the project actually needs them — the orchestrator appends the former on
demand; you append the latter by hand when a module deliberately uses a
hand-written Koin `module { … }` block.

## Delegated sources of truth

Replace every required identity/build value in the frontmatter before the first
bootstrap. Treat domain manifests as delegated canonical configuration:
`orchestrator/api-contract/environments.json` owns Backend source definitions
when present. Do not copy an active environment URL back into project config.

The installed skills (under `.claude/skills/`) read this config before acting. When you
bootstrap a new project, copy this file and update the
required values.

## Field meanings — identity / build

- `productName` — used in class prefixes (`<Product>Api`, `<Product>Component`).
- `productPackage` — Kotlin package root.
- `apiClassName` — name of the flat backend API class.
- `backendHost` — used in `BackendClient.defaultRequest`.
- `applicationId` — Android Play Store id; also used as the iOS bundle id (no
  platform suffix).
- `iosFrameworkName` — XCFramework target name.
- `iosEnabled` — if false, build-validator skips iOS gates.
- `firebaseEnabled` — if false, agents skip Firebase wiring.

## Field meanings — prelaunch / locales / typeface / DI

- `prelaunch` — if true, room-migration-builder allows destructive fallback
  without a migration.
- `supportedLocales` — resource-builder requires every locale to receive each new
  key.
- `typefaceFactory` — resource-builder uses this factory function name when
  registering fonts.
- `featuresWithRootComponentSuffix` — features that use `*RootComponent.kt`
  instead of bare `*Component.kt` (because they have a sub-screen with the same
  name as the feature). Start empty (`[]`) on a fresh project; the orchestrator
  appends a feature name only when its first sub-screen collides with the feature
  root.
- `diHandWrittenModules` — Koin modules that legitimately use the hand-written
  `module { … }` DSL outside annotated `@Single` classes (platform-edge wrappers,
  etc.). `di-validator` reads this list before flagging hand-DSL hits. Start empty
  (`[]`) on a fresh project; append a module name only when a hand-written
  `module { … }` block is deliberately introduced (typically platform-edge
  wrappers like Google/Apple auth).

## Updating

When a value changes (new locale added, app ships and `prelaunch` flips, codex
installed), update this file. The skills read it lazily — no rebuild needed.
