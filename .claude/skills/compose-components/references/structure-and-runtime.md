# Compose structure and runtime rules

The `Sample*` APIs in examples are educational and do not exist in the project.
Inspect real declarations and callers before adapting them. Kotlin naming,
visibility and comments follow [code style](../../validation-gates/references/code-style.md).

## Signatures and data boundaries

- Keep the project convention: a reusable visual composable starts with
  `modifier: Modifier = Modifier`. Then group content/data, style/variant/state/size,
  required `onX` callbacks, and optional overrides/hooks/slots. Keep a principal
  content lambda last when that makes the call readable. Input families may keep
  their principal `onValueChange` lambda last, grouping optional observation hooks
  immediately before it, to support consistent trailing-lambda calls. Preserve defaults and
  named callers during a migration. The feature Screen exception is owned by
  [UI composition](../../ui-feature/references/compose-rules.md#screen-function-signature).
- An entity row/card/header takes its existing `*State` model as one input,
  including nested sub-state, rather than mirroring its fields as scalar parameters.
  Generic primitives remain slot/enum/primitive driven; formatted inputs retain
  the matching `*FormatState`. Do not invent a model solely to wrap a few unrelated
  primitive widget options. Feature model placement belongs to
  [UI state](../../ui-feature/references/state.md).
- Entity component families and caller-owned context slots follow
  [the design-system component family rule](../../design-system/references/components.md#entity-component-families).
- Hoist controlled values and events to their current owner. Keep transient
  focus, gesture and animation state local when it represents presentation.
  Reusable widgets forward neutral callbacks; business decisions and analytics
  remain at their [component contract owner](../../design-system/references/components.md).

When adding a shared explanation, inspect the enclosing hosts for the same copy.
Keep that explanation at one visible owner in each context; distinct methodology
and data-quality notices may describe different facts. Deduplicate existing copy
or composition before adding another presentation switch.

Mutable UI state retained across recompositions uses observable remembered state
or an existing state holder, never a plain local or file-level `var`. Choose
`remember` versus `rememberSaveable` from the actual presentation lifetime;
feature drafts remain with their preservation owner.

## Read from top to bottom

Within a component file, place related contract types before the entry composable,
then private implementation/helpers, then preview fixtures/previews. Keep helpers
in reading order from their callers. Independent type files still follow code style;
do not move a coherent type family solely to enforce this sequence.

A body normally reads as: resolve tokens and presentation values; establish local
UI state/animation/effects; render the root and its children. Put a dependent value
after its input and a branch-only value inside that branch. Tiny expressions can
remain inline; the sequence is a reading aid, not mandatory empty sections.

Use names that explain a role (`isLoading`, `labelColor`, `contentGap`) rather than
temporary mechanics (`value1`, `result`, `flag`). Name an expression when it has
meaning, repeats, or interrupts the layout. Avoid token aliases for every access,
deep chained conditionals and nested `let`/`run` blocks when a local or `when` reads
directly. Resolve related visual choices together; keep small choices close to use.

```kotlin
@Composable
private fun SampleSummaryRow(
    modifier: Modifier = Modifier,
    state: SampleSummaryState,
    onClick: () -> Unit,
) {
    val labelColor = AppTokens.colors.text.primary
    val labelStyle = AppTokens.typography.b14Bold()

    Row(
        modifier = modifier.clickable(enabled = state.enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = state.title, color = labelColor, style = labelStyle)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = state.value, color = labelColor, style = labelStyle)
    }
}
```

The row's label/value pair is readable in place. Extract a private helper when it
owns a coherent contract: a repeated row, independent state or semantics, a gesture,
or nontrivial drawing/measurement. A large function with interleaved unrelated
branches needs those responsibilities separated even if each appears once. A new
helper should make its caller clearer and avoid threading most of the parent's
locals through a long signature. A line-count limit alone is not a reason to split.

Keep a one-use `Text`, `Spacer` or trivial `Row` inline unless it has that separate
responsibility. Avoid wrapper composables that only rename a call, forwarding
layers, and a private style/metrics holder with a factory for a handful of locals.
A cohesive resolved style used by several branches is useful; a parallel style
framework or config object that hides all widget inputs is not.

## Modifiers and layout

Apply the caller's modifier once to the component's outer layout node; each branch
may have its own root when exactly one branch is composed. Children start from
`Modifier` unless the contract explicitly supplies a separate child modifier.
For modifier extensions, preserve the receiver chain once. Do not reuse the root
modifier on children: padding, input or semantics would be applied twice.

Order modifiers for actual measurement, clipping, drawing, hit area and semantics.
Review their interaction in the enclosing composition, including enabled/loading
branches. Use bounded, content-driven sizing and existing inset owners. Intrinsic
measurement, custom Layout and SubcomposeLayout need a real measurement dependency;
neither adding nor replacing them mechanically proves an improvement. Avoid
measurement-to-state feedback when the layout phase can resolve the dependency.

Product spacing, radii, colors and text follow their
[design-system owners](../../design-system/references/index.md); visual hierarchy
and shape relationships follow [brand composition](../../brand-design/references/surface-composition.md),
and animation treatment follows [motion](../../brand-design/references/states-and-motion.md#motion).
Semantic token selection does not establish a rendered outcome.

## Stability and recomposition

`@Immutable` promises deeply immutable observable data, including nested fields;
`@Stable` promises consistent equality, observable public changes and stable public
property types. A sealed parent, `val`, or annotation cannot make mutable data safe.
Callback/composable-slot models may use `@Stable` when that contract holds; do not
mark them immutable just to improve skipping. Feature State purity, constructor
defaults and immutable collections remain with [UI state](../../ui-feature/references/state.md).
Do not promise skipping or speed from an annotation alone.

Use `remember` for expensive pure rendering calculations or required object
identity; include all changing inputs. Simple Boolean/membership checks and cheap
token lookups remain direct. Do not cache a callback or resolved theme/locale value
under `Unit` and freeze updates. Use `derivedStateOf` when rapidly changing
observable inputs produce a less frequently changing result, not for every
derived value. It adds its own observation cost.

```kotlin
val orderedItems = remember(items, comparator) { items.sortedWith(comparator) }
val isLoading = state == SampleDisplayState.Loading
```

The expensive sort and the simple condition have different needs. Keep heavy
business transformation, blocking IO and CPU work at the existing business/data
owner with its dispatcher; a composition effect does not select a worker thread.

## Effects and changing callbacks

Rendering stays free of externally observable side effects. `LaunchedEffect`
serves suspend presentation/system work. Restart an effect when its changing
resource/identity inputs change. Use `DisposableEffect` with matching keys and
cleanup for a registration. Inside a long-lived effect or gesture handler, use
`rememberUpdatedState` for a callback/value that must stay current without
restarting that work; use keys when a restart is required instead. Do not capture
the first callback in `remember` or `pointerInput(Unit)` by accident.

```kotlin
val currentOnEvent by rememberUpdatedState(onEvent)
DisposableEffect(source) {
    val subscription = source.observe { currentOnEvent(it) }
    onDispose { subscription.dispose() }
}
```

Here `source` changes the registration; replacing the callback does not need a
new subscription. The invented source is a presentation observer, not business
observation or navigation; [feature effects](../../ui-feature/references/compose-rules.md#feature-effects)
retain their owner. Keep remembered state and saveable identity tied to the
actual presentation lifetime; do not retain logical selection in a shadow cache.

Before adding or removing `try/finally` around a suspend effect, name what is
committed, what can cancel the work and which cleanup must still run. `finally`
is cleanup, not error recovery; cancellation must still propagate. Do not invoke
success callbacks after cancellation of an uncommitted operation. A host's
already-committed dismissal may require release when a library animation is
interrupted; inspect the pinned implementation and the session/identity guards
before treating that cleanup as redundant. Existing runtime evidence takes
precedence over an assumed absence of animation races.

## Hot paths and lazy content

Identify where repeated work happens: composition, pointer events, measure/layout
or drawing. For frequently changing visual state, read it in an available layout,
graphics-layer or draw lambda when only that phase needs it. Keep expensive paths,
brushes, text preparation or collection transforms out of per-frame/per-pointer
loops when their inputs are unchanged; use keyed remember or a drawing cache
whose invalidation matches those inputs. Do not add caches for all allocations.

Lazy items use actual stable unique identity; never invent an ID or use a mutable
position as entity identity. Use `contentType` for distinct reusable item structures
in mixed lists; unique IDs as content types defeat reuse. Add `animateItem()` only
for intended insert/remove/reorder presentation. Review identity-dependent local
state when items or their ordering change.

Name the concrete repeated work or observed problem behind a performance change.
Lower recomposition counts, a cache or more helpers alone do not prove a speedup.
Preserve readable code and correct invalidation before speculative optimization.

## Interaction and accessibility

The interactive surface owns its action, role, label, selected/toggle/disabled state
and useful hit target. Prefer the existing semantic click/select/toggle primitive
over a raw pointer handler for an ordinary control. An indicator inside an already
interactive parent must not add a duplicate target; independent child actions keep
their own identity. Decorative imagery has no redundant spoken description;
meaningful imagery/actions have localized labels from their resource owner.

Preserve keyboard, focus, IME, selection and loading behavior through callbacks.
Disabled/loading rendering and action availability must agree. Review modifier
ordering and semantics together; avoid clearing/merging semantics merely to hide a
duplicate that should be fixed at its owner. Existing widget-specific interaction
contracts remain in [components](../../design-system/references/components.md).

## Primary references

For a disputed Compose mechanism, check the pinned APIs and the relevant official
guidance; these links explain mechanics and do not override project ownership:

- [Performance best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices)
- [Stability contracts](https://developer.android.com/develop/ui/compose/performance/stability/fix)
- [Effect lifecycle and keys](https://developer.android.com/develop/ui/compose/side-effects)
- [Accessibility semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
