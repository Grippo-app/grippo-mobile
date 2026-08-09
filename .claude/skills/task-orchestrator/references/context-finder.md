# context-finder — surgical context lookup

Self-contained `context-finder` procedure. Builders ask "where is
X?" — context-finder answers with full paths and line numbers, not a paragraph.
Read-only and fast (haiku-tier). One call = one question = one tight answer.

The miss-signal protocol (`MAP_MISS` / `REGISTRY_MISS` / `CONTRACT_MISS`) is a
frozen mechanic — see
[`orchestrator-loop.md`](../../../contracts/orchestrator-loop.md) ("miss-signal
protocol: not silently dropped"). The orchestrator's consumer-side handling lives
in [`run-loop.md`](run-loop.md) Step 1a; this file is the *producer* side.

## Authoritative reading

None — this is a lookup dispatcher. Its knowledge is the body of this file plus
on-the-fly `grep`/`find` over the bootstrapped project. If a question needs
skill-reference context, route the caller to [`requirements-lookup.md`](requirements-lookup.md)
first, then act on the returned path.

**Read `orchestrator/.arch-map.json` first when it exists.** It is a derived index
of the project skeleton (modules, screens, routes, dialogs, data features, API
surface, DB schema). Use it to jump straight to the exact file before any
`grep`/`find`. It stores **no line numbers and no signatures** — resolve those on
demand by opening the file it points to. The map is a **hint, not authority**:
confirm any fact you return by reading the cited file. On a **miss** (the entry
you need is absent), fall back to a full grep **and prepend `MAP_MISS: <what was
missing>`** so the orchestrator can flag a stale map. The map is purely additive
— when absent (pre-bootstrap, or before the first ship), behave exactly as the
recipes below describe.

**For backend-contract questions, first run `cd orchestrator/api-contract &&
npm run --silent contract:paths`; when it reports a snapshot, read its
`inventory` path.** Answer "does the backend expose `POST /workouts`? what is
its shape?" from the inventory before any grep; resolve field-level detail from
`<areasDir>/<area>.json`. Like the arch map it is a **hint, not authority**
— it reflects the last validated refresh. On a **miss**, prepend
`CONTRACT_MISS: <what was missing>` and recommend Backend Test + Refresh. When
no snapshot exists, report the miss without asserting a server shape; endpoint
work is blocked upstream rather than downgraded to task text.

## Output format

A short structured response, no prose padding:

```
## <Question echo>

| Symbol / File | Path | Line | Note |
|---|---|---|---|
| `ProfileComponent` | ui-screen-features/profile/…/ProfileComponent.kt | 24 | bare-name root, owns StackNavigation<ProfileRouter> |

### Verbatim excerpt (when asked for "show me the signature")
```kotlin
public class ProfileComponent(…) : BaseComponent<ProfileDirection>(componentContext)
```

### Adjacent reading
- orchestrator/skills/ui-feature/references/add-screen.md for the recipe in this area.
```

Match the depth of the question: if asked to list, list; if asked to read,
include the verbatim snippet. Default is one signature + 5 lines of context.

## Tools

`rg` / `grep -rn` for symbol lookup; `fd` for fast file discovery (search from
`.` or a path, never `/`; fall back to `find`); `Read` to confirm a signature;
`Glob` for path patterns.

## Common queries — pre-built recipes

- **`<Feature>Component` / `<Feature>RootComponent`** — `fd
  '(<Feature>Component|<Feature>RootComponent)\.kt' ui-screen-features/`. Bare
  `<Feature>Component` is the default; `<Feature>RootComponent` is reserved for
  features containing a sub-screen with the same name (the authoritative suffixed
  list is `featuresWithRootComponentSuffix` in `project-config.md`).
- **List screen/dialog feature modules** — `rg -n
  "^include\(\":ui-(screen|dialog)-features:" settings.gradle.kts | sort -u`.
- **`<Product>Api` shape** — `fd '^[A-Za-z]*Api\.kt$'
  data-services/backend/…/`; report the class name, section comments, each
  method signature with line numbers.
- **Does `<X>Feature` exist?** — `rg -nl "interface <X>Feature\b"
  data-features/feature-api/`. Match → path + interface signature. No match →
  "Not found. Closest matches:" + fuzzy candidates.
- **`@Database(version = …)`** — `rg -n '@Database\('` +
  `entities = [` + `version = \d+`; return version + entity list.
- **List existing dialogs** — `rg -n "data\s+class\s+…:\s*DialogConfig"
  ui-dialog-features/dialog-api/`; return each subtype + input + `@Transient`
  callback shape.
- **Similar dialog to `<X>`** — pick the closest in shape (single input + single
  result), return the file paths for the builder to mirror.
- **Routes in `<Feature>Router`** — `fd '<Feature>Router\.kt'
  ui-screen-features/screen-api/` + `rg -n "^\s*@Serializable\s+public\s+(data\s+object|data\s+class)"`.
- **Similar screen** — look inside the feature for another sub-screen package;
  mirror its file layout, MVI shape, imports.

### "Which Compose component does this Figma node map to?" (only if `figmaEnabled: true`)

Source of truth is the task's **bindings manifest**
(`orchestrator/.cache/figma/screens/<stem>/bindings.json` — its `components[]`
rows, keyed by `designComponentId`, map each design identity to its
implementations: `projectComponentId` = `<adapterId>:symbol:<fqName>` plus
`sourcePath`), fed by the census (`census-<stem>.json`) from the committed
Component Mapping Registry (`orchestrator/figma/component-mappings.json`) and
the published Design Component Inventory. Match by the **owning component-set
node id** (`setNodeId` / a spec element's `componentSetNodeId`) — the stable
anchor; display names are labels only and two same-named sets are two rows, so
a name-only match is best-effort and must say so. Return the implementation's
`projectComponentId` (its `fqName` leaf) + `sourcePath` + the design entry's
variant axes. If no confident match, prepend `REGISTRY_MISS: <component name>`
so the orchestrator flags a missing component mapping (distinct from `MAP_MISS`
— Step 6c's map regen does not heal it); the builder must NOT approximate with
`Box`/`Row`. **You never call Figma** — you only read the committed
registry/inventory + cache files.

## What you MUST NOT do

- Do not write paragraphs of analysis — answer the question.
- Do not edit any file.
- Do not chase symbols you weren't asked about (no scope creep). One question →
  one answer.
- Do not invent file paths. If `rg` returns nothing, say "Not found" and stop.
- Do not return excerpts longer than the question demands.
