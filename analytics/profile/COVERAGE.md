# Profile analytics coverage

Record owner: [feature-analytics](../../.claude/skills/feature-analytics/SKILL.md)
and [state-preservation](../../.claude/skills/state-preservation/SKILL.md); records
policy: [CLAUDE.md](../../CLAUDE.md#records). Cross-feature contract and
verification limits: [current analytics contract review](../COVERAGE.md#current-analytics-contract-review-2026-10-14);
admin reconciliation: [Live GA4 setup review](../COVERAGE.md#live-ga4-setup-review--2026-10-14).
Rows describe the current code of `:ui-screen-features:profile`, `ProfileRouter`
in `:ui-screen-features:screen-api` and `:analytics:profile`; replace them when
that code changes. Typed dismissal observations use DismissRequested with
`dismiss_requested` wire codes; operation schemas omit visible-screen context and
correlate outcomes by `operation_id`; the typed catalog is not yet in a store build and every schema is version 1.

## Entry paths

- Main Profile tab (`MainRouter.Profile`) creates `ProfileComponent(initial =
  ProfileRouter.Menu, toDebug, close)`.
- `RootRouter.Profile(value)` opens the same component at an arbitrary initial
  route. Current Root directions that push it: `RootDirection.WeightHistory`
  → Body, `RootDirection.Goal` → Goal. The `weight_history` deeplink resolves
  to `RootDirection.WeightHistory`; the other editors are reached only from
  the Menu.
- `ProfileComponent` owns `childStack(serializer = ProfileRouter.serializer(),
  key = "ProfileComponent", handleBackButton = true)` and its own BackCallback;
  its child factory covers all eight `ProfileRouter` variants and passes only
  context and callbacks (no accepted parent values). Each editor is popped by the
  component's event listener; Menu's Debug entry is rendered only for the ADMIN
  role and its destination is outside this module.

## Surfaces and screen hooks

| Route / rendered surface | Component → ViewModel | ProfileScreen / wire identity | Hook |
|---|---|---|---|
| `ProfileRouter.Menu` (main tab default) | ProfileMenuComponent → ProfileMenuViewModel | Menu / `profile_menu` | Typed screenPresentation |
| `ProfileRouter.Body` (menu, WeightHistory deeplink) | ProfileBodyComponent → ProfileBodyViewModel | Body / `profile_body` | Typed screenPresentation |
| `ProfileRouter.Goal` (menu, `RootDirection.Goal`) | ProfileGoalComponent → ProfileGoalViewModel | Goal / `profile_goal` | Typed screenPresentation |
| `ProfileRouter.Experience` | ProfileExperienceComponent → ProfileExperienceViewModel | Experience / `profile_experience` | Typed screenPresentation |
| `ProfileRouter.Muscles` (menu) | ProfileMusclesComponent → ProfileMusclesViewModel | Muscles / `profile_muscles` | Typed screenPresentation |
| `ProfileRouter.Equipments` (menu) | ProfileEquipmentsComponent → ProfileEquipmentsViewModel | Equipments / `profile_equipments` | Typed screenPresentation |
| `ProfileRouter.Settings` (menu Account entry) | ProfileSettingsComponent → ProfileSettingsViewModel | Settings / `profile_settings` | Typed screenPresentation |
| `ProfileRouter.Social` (menu) | ProfileSocialComponent → ProfileSocialViewModel | Social / `profile_social` | Typed screenPresentation |
| Profile graph, any initial `ProfileRouter` | ProfileComponent → ProfileViewModel | Default null | Child stack host; renders only the stack; no analytics dependency |
| Height/Weight/Date/PrimaryGoal/SecondaryGoal/LanguagePicker/Confirmation dialogs | Existing dialog components/VMs | No dedicated profile presentation contract | Runtime overlays; the owning profile VM reports open and accepted result |

All eight `ProfileScreen` values have exactly one `presentScreen` owner, and no
other module injects ProfileAnalytics. The eight leaves resolve the stateless
service inside their `retainedInstance` factory; the container adds no binding
or service resolution. Foundation binds `screenPresentation` before activation,
assigns Initial/Restore and closes on pause/destroy. Alternate initial routes and
restored child stacks use the same owners. Body, Goal, Experience, Muscles,
Equipments, Settings and Social register a leaf BackCallback that invokes the
owning VM; Menu has no Back control or event, so container stack handling and
pop/close stay unreported, and the parent navigation and main tab shell emit no
synthetic menu or Back clicks.

## Callbacks, inputs and operations

| Owner | Observed callbacks → typed event → wire | Complete operations and eligibility |
|---|---|---|
| Menu | Profile/Settings menu target → MenuSelected → `profile_menu_selected` {menu_target = body/goal/experience/muscles/equipments/settings/social/debug} | Startup RefreshUser (User loader), RefreshGoal, RefreshMuscles, RefreshEquipments, each wrapping the explicit `get*()` request; no guards |
| Body | Height/Weight picker open → Body.PickerOpened → `profile_picker_opened` {input_field = height/weight}; each accepted result → draft update, then Body.PickerConfirmed → `profile_picker_confirmed` {input_field, input_status} using the existing formatter status; Apply → `profile_control_clicked` {apply}; toolbar or system Back → Body.DismissRequested → {dismiss_requested} | Startup RefreshWeightHistory and RefreshUser (no loaders). SaveBody after the guards (weight value, height value and user present, otherwise return before Apply) inside `safeLaunch(ApplyBodyChanges)`: conditional `updateWeightUseCase` and `setHeight` direct throwing writes, the existing 500 ms delay and the WeightHistory reminder scheduling; Back follows the block. Apply with unchanged values still completes SaveBody (the UI disables the button) |
| Goal | TargetDate picker open → `profile_picker_opened` {target_date}; accepted result → Goal.TargetDateConfirmed with the formatter status → `profile_picker_confirmed` {target_date, input_status}; Primary/Secondary picker open → {primary/secondary}; Goal.PrimaryConfirmed / SecondaryConfirmed are parameterless (nonnull results encode `input_status=valid`); personalization toggle → Goal.PersonalizationChanged(selected, count) → `profile_personalization_changed` {selected, selected_count}; Save → {save}; Back → {dismiss_requested} | Startup RefreshGoal. `onSave` runs in `safeLaunch(SaveButton)`, returns when no primary goal is selected or the target date is Empty/Invalid against the current goal-validity range (before Save), then SaveGoal wraps `setGoal(request)`; Back follows the block |
| Experience | Selection → Experience.Selected → `profile_experience_selected` {profile_experience = beginner/intermediate/advanced/pro}, emitted again on re-selection; Apply → {apply}; Back → {dismiss_requested} | Startup RefreshUser. SaveExperience wraps `setExperience(...)` after `selected ?: return` (before Apply) inside `safeLaunch(ApplyButton)` |
| Muscles | Group click → Muscles.GroupSelected → `profile_control_clicked` {group_selected}; item toggle → Muscles.SelectionChanged(selected, count) → `profile_selection_changed` {selected, selected_count}; Apply → {apply}; Back → {dismiss_requested} | Startup RefreshMuscles. SaveMuscles wraps `setExcludedMuscles(all - selected)` after the nonempty-catalog guard (before Apply) |
| Equipments | Same facts with `interaction_screen=profile_equipments` | Startup RefreshEquipments. SaveEquipments wraps `setExcludedEquipments(all - selected)` after the nonempty-catalog guard |
| Settings | Language row click → Settings.LanguageOpened → `profile_picker_opened` {input_field = language}; every accepted LanguagePicker result → Settings.LanguageConfirmed → `profile_picker_confirmed` {input_field = language, input_status = valid}, including a confirmation of the already-effective language (parameterless like the Goal Primary/Secondary confirmations); Logout click → `profile_control_clicked` {logout}; deletion confirmation open → {delete_opened}; accepted confirmation result → {delete_confirmed}; Back → {dismiss_requested}; the analytics consent toggle emits nothing; read-only account identity has no interaction/event | SaveLanguage wraps `localSettingsFeature.setLanguage(value.toDomain())` in an unloaded `safeLaunch` inside the picker `onResult` for every accepted result, so confirming the language already shown pins it as an explicit choice (DataStore skips an equal value and ApplicationBootstrap applies only distinct tags). When nothing was stored, that pin can still change `AppLocale.current()` (for example `en-US` → `en`, or an unsupported `de-DE` → `en`), which may discard an in-flight catalog response as `interrupted` (see [home](../home/COVERAGE.md)). The write is a local DataStore commit; the `language` key is an installation preference that `clearUserSettings()` keeps across Logout and DeleteAccount, like consent and `onboarding_shown`; the app-wide UI language switch (ApplicationBootstrap → AppLocale in `:shared`) observes that store outside this VM and is not reported here. Logout wraps the direct-value `logoutUseCase.execute()` in `safeLaunch(LogoutButton)`; DeleteAccount wraps `deleteProfileUseCase.execute()` in `safeLaunch(DeleteAccountButton)` inside the confirmation `onResult`; both use cases call `clearUserSettings()` directly. Account action buttons mutually disable while either operation runs. Identity rebuilds from cached `observeUser()` with no new request or analytics payload. The consent write `setAnalyticsConsent(granted)` is not decorated |
| Social | Link click → Social.LinkClicked → `profile_link_clicked` {social_channel = instagram/tiktok/youtube/threads}; the synchronous any-URL-opened boolean → Social.LinkResult → `profile_link_result` {social_channel, opened}; Back → {dismiss_requested} | None; the synchronous LinkOpener fallback remains business-owned, with no artificial suspend operation |

Personalization categories are grouped into routine, training preferences and recovery/limitations. Disclosure only changes presentation and deliberately emits no analytics event; the existing typed toggle observations still describe accepted selection changes. The editor introduces no analytics-facing symbol changes: events, parameters, screen hooks, operations, encoders and consent retain their contracts.

Every emission follows its eligibility guard; rejected paths emit neither accepted
intent nor operation. Existing callbacks have no additional VM busy guards;
Settings disables both account action buttons while Logout or DeleteAccount is
active, and other UI loader behavior and direct callback acceptance remain unchanged. Refresh operations
wrap the original explicit startup requests, not ongoing observations. Commands
run directly inside the decorator and propagate their original exceptions.
Completed means the whole caller block returned,
including cached/false read outcomes or an accepted Body Apply with no changed
measurement; it does not infer a server write. Reporting exceptions are isolated
(`trackEvent` catches Exception locally; operations rely on the toolkit decorator)
without replacing results or business exceptions; cancellation propagates; fatal
Errors remain fatal. All 32 ProfileEvent variants have a VM emitter and the event
matrix is exhaustive.

Weight-history refresh and mutation readback validate each domain row and its
consumed date, and reject duplicate primary IDs before transactionally replacing
that user's snapshot. A confirmed empty snapshot clears
only that user's rows, so deleted weights do not remain cached. Request, required
readback and storage failures propagate through the existing operation block.
Exclusion requests require the cached active profile; their refresh/readback rejects
missing or duplicate IDs before replacing membership rows. Weight-history refresh
requires an active user ID; goal and weight-history mutations additionally require
the cached active user before their remote writes. Goal absence remains a Boolean
read outcome, while goal writes return Unit.
Nonnull goals must map without losing required enum/personalization data before
cache writes. Equipment/muscle catalogs reject missing fields, wrong group
ownership and duplicate primary IDs before replacing their snapshots; rows with a
type key unknown to this app version are stored and hidden from the domain, so the
selection screens never list them; Apply then sends only visible IDs, which removes a
hidden row's exclusion (the accepted rollout risk in
[operation outcomes](../../.claude/skills/data-layer/references/operation-outcomes.md#operation-outcomes-and-cache-reconciliation-canonical)).

Settings consent control: the project `Toggle` (AppTokens colors, accessible label,
scrollable Settings content, title and description in EN/UK/RU; `enabled` (default true, so existing callers are unchanged) and
`contentDescription` inputs, native Switch semantics and disabled color tokens)
renders `checked = analyticsConsent == true` and is enabled only when the durable
source is known and no AnalyticsConsent write is pending. `onAnalyticsConsentChange`
rejects a pending source, an unchanged value and an ongoing write, then saves with
a direct call inside safeLaunch. Write failures follow the normal error pipeline;
a failed source subscription restarts only after its error dialog is dismissed
and keeps the switch disabled until the source is known again. Consent actions
deliberately emit no tracking event: denial must not require or report a
consent-control interaction. The cross-feature collection policy is in the root
contract review.

## Catalog and reporting decision

`ProfileAnalyticsModule` (`@Module @ComponentScan`) binds `ProfileAnalyticsImpl`
as `ProfileAnalytics` and registers one qualified `profile_analytics` provider
with nine interaction schemas and four operation schemas; `shared/.../Koin.kt`
lists it once, and the module depends only on `analytics-api`. Every interaction
payload starts with `interaction_screen`. Version-1 schemas:

- `profile_control_clicked` [interaction_screen, interaction_action =
  dismiss_requested/apply/save/group_selected/logout/delete_opened/delete_confirmed],
  `profile_menu_selected` [.., menu_target], `profile_picker_opened` [..,
  input_field = height/weight/primary/secondary/target_date/language],
  `profile_picker_confirmed` [.., input_field, input_status = empty/invalid/valid],
  `profile_experience_selected` [.., profile_experience],
  `profile_selection_changed` and `profile_personalization_changed` [.., selected,
  selected_count], `profile_link_clicked` [.., social_channel],
  `profile_link_result` [.., social_channel, opened].
- `profile_task_started` [profile_operation], `profile_task_completed/failed/interrupted`
  [profile_operation, duration_ms]; `failed` drops the error object. Operation
  codes: RefreshUser `refresh_user`, RefreshGoal `refresh_goal`, RefreshMuscles
  `refresh_muscles`, RefreshEquipments `refresh_equipments`, RefreshWeightHistory
  `refresh_weight_history`, SaveBody `save_body`, SaveGoal `save_goal`,
  SaveExperience `save_experience`, SaveMuscles `save_muscles`, SaveEquipments
  `save_equipments`, SaveLanguage `save_language`, Logout `logout`, DeleteAccount
  `delete_account`; all thirteen have VM callers.

The `profile_task` names avoid colliding with authorization's `profile_operation_*`
event names; the parameter key `profile_operation` here and that event family are
different namespaces. Public events are closed observations with direct typed
facts: Body and Goal have separate picker enums, menu targets never contain
navigation routers, and operation values are feature-owned. Picker confirmation is
distinct from input-status change and emits even for an unchanged accepted value.
Counts describe selected items/personalizations, not the complementary excluded
lists. This is additive instrumentation with no historical behavioral backfill;
the `profile_picker_confirmed` payload, fields and status values were unchanged
by the contract follow-up that made Primary/Secondary confirmations parameterless.
The Settings language row reuses the existing `profile_picker_opened` and
`profile_picker_confirmed` schemas (`interaction_screen=profile_settings`) with
the new `input_field` code `language` and the new operation code
`save_language`; no schema was added or changed, and every schema stays version 1.

## Omissions and privacy

No raw measurement, date, name/email, input length, entity/group/item ID, URL,
credential, request/response body or exception message is sent. Goal categories
and personalization content are deliberately omitted (the latter can describe
health and lifestyle limitations); only picker field/status and toggle
selected/count are observed. The selected app language is deliberately omitted
in the same way as the Goal Primary/Secondary choices: LanguageConfirmed carries
no language identity and always encodes `input_status=valid`, and neither the
previous language, an unchanged confirmation nor whether the effective language
was stored or derived from the platform locale is reported. The LanguagePicker
sheet's own selection and dismissal facts belong to `:analytics:language-picker`,
not to this feature. Which fallback URL opened is not reported. No
dedicated dialog view/close/dismissal/cancel reason, error classifier,
referrer/entry attribution, transition/funnel event or navigation-generated click
exists. Scroll and chart gestures and internal unaccepted dialog choices have no
profile Contract callback and stay outside owning-screen observations. Debug's
entry click is covered as a menu target; its destination is outside this module.
Consent, identity and SDK transport remain app/core-owned.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| ProfileComponent | Child stack (`ProfileRouter` configs) | Preserve (navigation) | `childStack(serializer = ProfileRouter.serializer(), key = "ProfileComponent")`; all routers are serializable objects | Initial config from the constructor; no parent-accepted values passed to children |
| ProfileViewModel | `ProfileState` | Runtime only (data object) | None | Nothing to restore |
| Menu | user, goal, excluded muscle/equipment counts | Rebuild | `observeUser`, `observeGoal`, `observeExcludedMuscles`, `observeExcludedEquipments` plus the four refresh operations | Defaults null; loaders drive skeletons |
| Body | `weight`, `height` formats, including accepted picker results | Preserve | `DRAFT = PersistenceKey("ProfileBodyDraft", ProfileBodyState.serializer())`; nullable initialState | A restored snapshot (any value, including Empty/Invalid) skips seeding; a fresh form seeds once from the first nonnull user under Processing.Infinity, only for fields still Empty; picker results update the draft before the event |
| Body | `user` | Rebuild | `@Transient`; `observeUser()` | Required for Apply eligibility and button state; null until the first emission |
| Body | `history` | Rebuild | `@Transient`; `observeWeightHistory()` | Default empty list |
| Body | Height/Weight dialog sessions, ApplyBodyChanges loader, reminder scheduling | Runtime only | DialogConfig built in the VM | Sessions unsaved; the accepted value lives in the draft; the delay and notification inside SaveBody are not restorable |
| Goal | `selectedPrimary`, `selectedSecondary` | Preserve | `DRAFT = PersistenceKey("ProfileGoalDraft", ProfileGoalState.serializer())`; nullable initialState with `ProfileGoalState.initial()` default | Fresh seeding from the first nonnull goal only without an archive; a restored null stays null |
| Goal | `selectedTarget` (DateTimeFormatState) | Preserve | Same draft | Fresh default `limitations.from`, applied only without an archive; seeded from the goal target only while still equal to that default; a restored snapshot keeps its saved formatter/value as-is; the derived `targetDate` is revalidated from the current period’s start through `limitations.to` for display and Save eligibility, including expired targets; an ongoing goal may be closer than the 28-day minimum for a newly picked target. The Screen’s target-date MenuValueRow formats typed dates through DateCompose using the current Compose locale without replacing the raw draft; absent typed values show the localized Select date action |
| Goal | `selectedPersonalization` | Preserve | Same draft, ImmutableListSerializer | Seeded only when empty on a fresh form; toggles update the draft before the event |
| Goal | `limitations` (DateRange) | Rebuild | `@Transient`; current date plus the 28-day minimum horizon | Date-dependent default recomputed per construction, excluded from the archive |
| Goal | `personalization` catalog | Rebuild | `@Transient`; enum entries | Static |
| Goal Screen | Expanded personalization section and category | Runtime only (presentation) | `rememberSaveable` section identifier and category name in the Screen | All three summary sections start closed; one section at a time, switching or closing resets the inner category. Routine exposes its three categories directly; preferences and recovery allow one inner category at a time. No business draft or analytics event for disclosure; selected values always remain in `selectedPersonalization` |
| Goal | Date/Primary/Secondary dialog sessions, SaveButton loader | Runtime only | DialogConfig built in the VM | Unsaved; accepted values in the draft |
| Experience | `selected` | Preserve | `DRAFT = PersistenceKey("ProfileExperienceDraft", ProfileExperienceState.serializer())`; nullable initialState | Fresh seeding from the first nonnull user experience only while still null and without an archive; a restored null is preserved |
| Experience | `suggestions` | Rebuild | `@Transient`; enum entries | Static |
| Muscles | `selectedGroupId` | Preserve | `DRAFT = PersistenceKey("ProfileMusclesDraft", ProfileMusclesState.serializer())`; `restoreState(DRAFT) ?: ProfileMusclesState()` (non-null) | Re-validated against the current catalog, falling back to the first group |
| Muscles | `selectedMuscleIds`, `knownMuscleIds` | Preserve | Same draft, ImmutableListSerializer | Known IDs keep their saved selection, new IDs are seeded as not excluded, IDs absent from a nonempty catalog are dropped; an empty catalog emission updates only suggestions; an absent archive and an all-default snapshot both reconcile through empty known IDs |
| Muscles | `suggestions` | Rebuild | `@Transient`; `combine(observeMuscles(), observeExcludedMuscles())` | Default empty; Apply requires a nonempty catalog |
| Equipments | `selectedGroupId`, `selectedEquipmentIds`, `knownEquipmentIds` | Preserve | `DRAFT = PersistenceKey("ProfileEquipmentsDraft", ProfileEquipmentsState.serializer())`; non-null `?: ProfileEquipmentsState()` | Same reconciliation as muscles |
| Equipments | `suggestions` | Rebuild | `@Transient`; `combine(observeEquipments(), observeExcludedEquipments())` | Default empty; Apply guard |
| Muscles / Equipments | ApplyButton loader | Runtime only | None | Not archived |
| Settings | `analyticsConsent: Boolean?` | Rebuild (durable source, no StateKeeper copy) | `observeAnalyticsConsent()` mapped to Granted, `safeLaunch(processing = Infinity, onError = ::observeAnalyticsConsent)` | Reset to null on every (re)subscription; writes commit immediately through `setAnalyticsConsent(...)`, so no unfinished draft exists; the control is disabled while null or while the AnalyticsConsent loader is active |
| Settings | Effective app language (row value, picker `initial`) | Rebuild (durable source, no StateKeeper copy; not a State field) | The row renders `LanguageEnumState.current()`, i.e. the theme's `AppTokens.strings.localeTag`, which the root sets to `AppLocale.current`; the VM reads `LanguageEnumState.of(AppLocale.current())` when the user opens the picker; the accepted result is always written to the DataStore `language` key, which survives Logout and DeleteAccount | On Android the system splash stays until the stored language is applied (`AppLocale.isResolved`, MainActivity); on iOS the persisted `AppleLanguages` already carries it at launch, so the row and the picker `initial` show the resolved language, including a Settings route restored after process death. If the stored value cannot be read, ApplicationBootstrap resolves to the platform locale and retries; a choice confirmed meanwhile is attempted: if written it wins once a read succeeds, and a failing write reports `save_language` failed. The row switches in the same recomposition as the rest of the UI. Each accepted picker result commits immediately through `setLanguage`, so no unfinished draft exists |
| Settings | `user` (read-only name/email identity) | Rebuild | `UserFeature.observeUser()` mapped through the existing domain-to-state user mapper | Default null; observes cached profile without a new request; no editable draft or StateKeeper copy; missing identity stays explicit and no identity value enters analytics |
| Settings | Confirmation and LanguagePicker dialogs; Logout/DeleteAccount/AnalyticsConsent loaders; the unloaded SaveLanguage job | Runtime only | DialogConfig.Confirmation and DialogConfig.LanguagePicker built in the VM | Unsaved; each accepted result starts its operation inside `onResult`; an unconfirmed picker choice is discarded with the session |
| Social | `ProfileSocialState` | Runtime only (data object) | Link result consumed synchronously | Nothing to restore |
| All owners | Secrets; restoration flags or caches | None | No credentials or tokens in any profile State; no `var`/`lateinit` member in any profile ViewModel; fresh-vs-restored is decided solely by `initialState == null` | Nothing to exclude |

Back/pop destroys the child context and its archive; re-entry constructs a fresh VM
without a draft, so Body/Goal/Experience re-seed from UserFeature/GoalFeature and
Muscles/Equipments reconcile from the catalog and exclusion Flows, the business
sources the previous Save wrote through. No accepted parent values exist to thread
(Menu holds only read-only summaries); profile has no multi-step parent draft.
Dialog sessions are plain DialogConfig instances with live closures, never
serialized. Body's fresh population consumes the first nonnull user once; the
separate lifecycle-aware user and history subscriptions rebuild presentation
without rewriting inputs, and ordinary pause/resume does not restart population.
No edit flags, source caches, new keys or archive schema changes exist; local
suppliers remain the latest source for archive saves; restoration never replays
picker or control callbacks.

## Verification

Source gates were reconciled against the routes, child factory, rendered
surfaces, retained injection, foundation binding, callback guards, operation
callers, draft keys and serializers by a read-only audit (no build was part
of it). Checks recorded for the
implementing revisions (2026-10-13):

- Full module instrumentation: source/diff review and `git diff --check` passed;
  inline static checks confirmed eight unique leaf hooks with retained injection,
  the null container, callers for all twelve operations, complete schema
  registration, unique custom event names across feature catalogs and
  implementation dependency boundaries; generated profile DI registers exactly
  one qualified schema provider and the service binding. The final
  `:androidApp:assembleDebug` and `:shared:compileKotlinIosSimulatorArm64` run
  passed (BUILD SUCCESSFUL); earlier scoped analytics/profile and UI/profile
  compilation also passed; existing global opt-in/deprecation warnings remain.
- Contract follow-up (parameterless Primary/Secondary confirmations): source
  invariants and `git diff --check` passed;
  `:ui-screen-features:profile:compileKotlinIosSimulatorArm64` and
  `:shared:compileKotlinIosSimulatorArm64` passed (BUILD SUCCESSFUL). Device
  runtime and Android compilation were not rerun for that contract-only
  refinement.
- Restoration and Back follow-up (Body BackCallback, first-nonnull seeding):
  scoped `:ui-screen-features:profile:compileAndroidMain`,
  `:analytics:profile:compileAndroidMain` and
  `:ui-screen-features:profile:compileKotlinIosSimulatorArm64` finished with
  BUILD SUCCESSFUL; source/diff review and `git diff --check` passed; generated
  profile service/schema bindings were inspected. Independent source review of
  the profile owners and the Body diff found no blocking defects; it did not
  execute runtime restoration or lifecycle scenarios.
- Settings Toggle repair (2026-10-14): Material Switch replaced by the project
  Toggle with optional `enabled`/`contentDescription`; hardcoded English On/Off
  state descriptions removed; no event population, wire schema, account operation
  or layout change. Toggle previews include enabled/disabled on/off; Material UI
  fidelity/contrast was not verified.

Scoped Goal and Settings observations do not establish feature-wide runtime
coverage. Still unverified: other Android flows and iOS device execution, SDK
delivery, reporting-failure injection, actual cancellation/lifecycle timing,
remaining fresh-process restoration cases across the five drafts (Android
shell-supplied saved state; iOS completed compatible
background checkpoint), the runtime precedence between a leaf BackCallback, the
container BackCallback and the stack's `handleBackButton` (system Back must emit
DismissRequested once and pop once), and `clearUserSettings` behavior beyond the
authorization inventory. iOS compilation does not prove framework linking or
native runtime behavior; no Firebase sign-in, send or upload was performed. Manual
scenarios still required beyond the exercised Goal/Settings paths: save an
unfinished draft in each of the five editors,
destroy the old lifecycle/InstanceKeeper, restore through fresh Components and
confirm selections plus final save arguments, including empty and invalid Body
formats, accepted picker results, an empty first catalog emission, changed
catalogs, a delayed first user observation and pause/resume beyond the foundation
disconnect grace; confirm toolbar/system Back reports once and pops once, and
re-entry after pop initializes from current business data. For the language row:
open the picker with the effective language preselected, confirm a different
language (one picker_opened, one picker_confirmed, one save_language operation,
the row value and the UI switching without restart, except date text that a
ViewModel pre-formatted into State, which keeps the old language until its data
re-emits — an accepted limitation) and the same language
(picker_confirmed and a save_language operation that pins it), dismiss without a result (no
confirmation), the first frame on a fresh install with nothing stored, and the
row after process-death restoration of the Settings route with a stored
language that differs from the system locale.
