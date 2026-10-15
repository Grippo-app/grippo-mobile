# Design and visual pass records (2026-10-15)

Dated brand-design, design-system and illustration review records moved out of
`analytics/COVERAGE.md` without content changes. They are historical evidence for
the revisions they describe, not current rules; current role-to-token choices live
in [CONSISTENCY.md](../../design-system/CONSISTENCY.md) and the rules in
[brand-design](../../.claude/skills/brand-design/SKILL.md) and
[design-system](../../.claude/skills/design-system/SKILL.md).

## ToggleSelectableCard brand-design application (2026-10-15)

Visual brief: keep repeated selection rows calm beneath the screen header and
above the existing pinned action. Retain the current warm selection surface/outline,
neutral labels, 28 dp shape, reserved switch and color animations in both themes.
Small gains 16 dp horizontal / 12 dp vertical padding; Medium keeps the existing
64 dp image with 12 dp padding and uses icon.secondary for unselected equipment.
Both sizes grow for full labels instead of truncating after two lines.

Usage/state inventory: Small is used only by MuscleSelection in
ExcludedMusclesScreen (profile creation) and ProfileMusclesScreen (profile tab).
Medium is used only by EquipmentRow inside EquipmentSelection in
MissingEquipmentsScreen (profile creation) and ProfileEquipmentsScreen (profile tab).
All four screens use background.screen, fixed group tabs, scrolling rows and a
pinned Continue/Apply action. Muscle hosts also reserve their anatomy illustration.
Selected/unselected, mixed selections, short/long localized labels and both themes
apply. Cards expose no disabled/loading/error branches; an empty catalog renders
no rows. Existing pressed/switch feedback, group-change scroll identity, minimum
heights, callbacks and selection ownership are unchanged.

No analytics/state impact: no event, schema, screen identity, presentation hook,
operation, validation, route or result change; no live-admin synchronization trigger.
The four owning VMs keep selectedGroupId, selected/known equipment or muscle IDs
in their existing drafts and rebuild suggestions. Profile-creation parent values
and local StateKeeper wiring are untouched; no new saved state or runtime hooks.

Preview mapping: main Small covers a short muscle label; main Medium covers wide
Barbell and tall Smith machine artwork, each selected/unselected. LongContent
covers both sizes and states with long Ukrainian fixture labels at 200% font scale
in the existing 360 dp light / 412 dp dark configurations. Existing whole-screen
previews cover the four hosts, mixed/empty selection and the profile Apply loading
state. These fixtures do not establish runtime interaction or restoration.

Verification: self-review reconciled both variants, every production call site and
the four enclosing screens. Public styles/dispatcher and row/switch callback
bindings match the pre-task source; git diff --check passed. Unselected equipment
icon.secondary/card contrast is 6.39:1 in light and 4.51:1 in dark from current
theme values. No additional palette, public API, dependency or automated tests.
Independent review was not performed; no separate reviewer was authorized.

Scoped compilation of components, authorization and profile passed for Android
and iOS Simulator (BUILD SUCCESSFUL, 2m35s;
/private/tmp/grippo-toggle-brand-compile.log). Earlier attempts hit a Gradle cache
packing failure and unresolved Preview imports in concurrently edited components;
the final revision passes without changing those unrelated owners or dependencies.
Studio's shared window changed during inspection; the final revision's
component/whole-screen previews, narrow dark 200% layout, live gestures,
transitions, list-end access and iOS rendering remain
unverified. Existing pre-change renders are not evidence for the final revision.

## Splash brand-design application (2026-10-15)

Visual-only refinement of shared BrandSplash: a larger existing logo and localized
Grippo name form one centered identity group; a bounded warm radial glow fades
to transparency over the neutral screen. Existing light/dark illustration and
text roles are reused without changing their producers. Component-owned logo/glow
dimensions stay in AppDp.brandSplash; no new artwork, font or shared wrapper.

Actual consumers remain SplashScreen (loader follows AppContent membership) and
ProfileCompletedScreen (loader deliberately always visible). Components, retained
VMs, data-object States, routes, back behavior, catalog requests, retry, profile
creation and immediate navigation are unchanged. AuthScreen.Splash and
AuthScreen.Completed keep their existing single screenPresentation hooks.
No analytics/event/schema/consent or user-state impact; no live-admin trigger or
new StateKeeper supplier. Existing Compose animation state remains runtime-only;
the entrance/glow durations, first-frame visible identity and loader are preserved.

Verification: independent source/caller/token reviews found no actionable findings;
all three locale XMLs contain exactly one matching app_name. Components and
authorization compile for Android and iOS Simulator (BUILD SUCCESSFUL, 1m50s;
/private/tmp/grippo-splash-brand-compile.log). Android Studio rendered BrandSplash
with and without the loader, then full SplashScreen and ProfileCompletedScreen,
in light (360 dp / UK) and dark (412 dp / EN) previews. Current logo/name/glow and
bottom loader were inspected without layout-bound overlays; no clipping or overlap
was observed at the existing font scale. No extra small/font-scale variants added.

Full integration initially failed while KSP read a generated training-module KLIB;
the archive was present and passed ZIP integrity inspection. No source/settings
workaround was introduced. Retry passed Android assembleDebug and shared Debug
XCFramework assembly, including iOS device and simulator (BUILD SUCCESSFUL, 11m03s;
/private/tmp/grippo-splash-brand-integration-retry.log).

Authorization screens were edited concurrently during verification (Login,
Registration, User and ProfileCreation) and were left untouched by this task.
Additional authorization compilation passed Android, iOS device and simulator
(BUILD SUCCESSFUL, 1m40s;
/private/tmp/grippo-splash-brand-current-authorization-jbr.log). This scoped check
used Android Studio's existing JBR 25 after the terminal's JDK 19 could not load
an IDE-generated build-plugin class; no project/JDK setting was changed. The
build checks establish the Splash implementation at the checked revisions; they
do not claim verification or repackaging of every ongoing authorization edit.

Native launch timing/motion, system handoff, enlarged-font layout and iOS rendering
remain unexercised; previews do not establish those checks.

## CheckSelectableCard brand-design pass (2026-10-15)

Visual brief: keep choices calm and neutral, concentrate warmth on selection,
and give long experience descriptions their own reading width. Medium/Large use
a decorative circular marker with a permanently reserved 24 dp slot and an 18 dp
check. Its foreground is selection.onAccent, supplied by both existing themes.
Large moves its description below the icon/title row; Small retains compact chip
geometry and clips to its existing shape. No new artwork or feature behavior.

Usage/state inventory reviewed from all seven production files (eight calls):
Large in ExperienceScreen/ProfileExperienceScreen under their headers and pinned
actions; Medium in PrimaryGoalPicker/SecondaryGoalPicker/PeriodPicker dialog lists;
Small in ProfileGoalScreen's multi-select FlowRow and ExerciseLibrary Header's
filter/suggestion LazyRow. All uses retain their existing backgrounds, content,
callback owners, scrolling and inset handling. Applicable states are selected and
unselected, Medium with/without description, long labels/descriptions, both themes
and enlarged fonts. Disabled/loading/error branches are absent from this widget's
contract; actual screen/button loaders remain with their existing owners.

Analytics impact: none. Card public styles/isSelected/onSelect, one original click,
selected semantics, screen identities, events, operations, validation and results
remain unchanged. The visual-only no-impact exception applies; no live admin
synchronization or telemetry changes are required. Preservation impact: none.
Experience/personalization/filter selections remain at their existing VM/draft
owners; accepted picker results, parent drafts and rebuilt catalogs are untouched.
No new state, persistence, dependencies or automated verification infrastructure.

Preview source coverage includes every size selected/unselected, nullable Medium
description, and long Ukrainian fixture text at 200% font scale via AppPreview in
both themes. Fixtures use full width for Medium/Large and content width for Small.
Self-review and independent source review passed after adding the marker's own
private AppPreview. Palette endpoint contrast for the check is 5.27:1 in LightColor
and 8.74:1 in DarkColor. Final build/render evidence is recorded separately below;
source coverage and contrast arithmetic do not establish rendered layout/runtime.

Final verification: git diff --check passed. The component compiled for Android
in the final scoped run. Its iOS device/Simulator tasks completed in the earlier
integration run, but assembleSharedDebugXCFramework failed in Debug/Exercises
commonMain metadata (unresolved design-system symbols outside the changed files;
/tmp/grippo-check-card-integration-build.log). The final scoped iOS repeat failed
in ui-core:state with NoSuchFileException for provider's generated Simulator klib
while other builds were active (/tmp/grippo-check-card-scoped-build.log); this is
not a successful final iOS verification. A configuration-cache lock was bypassed
only for that invocation, without editing project settings. Android Studio access
and opening this component were attempted, but its preview refresh was interrupted
by concurrent IDE interaction; final component/whole-screen renders in both themes,
200% font layouts, pressed/color transitions, gestures and iOS runtime remain
unverified. No build artifacts were repaired or unrelated source/reset actions taken.

## ProfileCreationScreen brand skill follow-up (2026-10-15)

Visual-only follow-up against the existing working-tree redesign: the parent
progress marker uses charts.indicator.primary.dominant, matching its LineIndicator
in both themes; User groups Name/Weight/Height with content spacing. The private
progress composition is reused in a filled User whole-screen AppPreview, covering
the existing small light and large dark configurations without a live Component.

No callback, validation, loader, route, progress calculation, screen identity or
telemetry contract changed in this follow-up. The five leaf screenPresentation
hooks and null container hook remain unchanged; no admin synchronization is
triggered. State owners and local draft keys are untouched: parent accepted values
and four editable child drafts stay preserved, catalogs rebuilt, completion
runtime-only. No persistence or analytics additions are needed.

Scoped authorization Android and iOS Simulator compilation passed (15s;
/private/tmp/grippo-profile-brand-build.log). Independent source/diff review found
no blocking defect introduced by this follow-up. It separately identified an
existing pre-follow-up change: EquipmentSelection's key(selectedGroupId) resets
list state on group change, unlike the old inlined list; this follow-up preserves
the current working-tree behavior and does not claim full HEAD-to-tree scroll
parity. Rendered acceptance is unverified: Studio showed loading/render problems,
and the emulator was on Login, not profile creation. Keyboard, enlarged fonts,
flow restoration and iOS rendering were not exercised.

## Profile setup segmented progress (2026-10-15)

The user requested a different visual progress style. ProfileCreationScreen now
uses shared StepProgressIndicator: four fixed rounded segments fill through the
current editable step, with warm selection.accent and neutral border.default in
both themes. The logo marker and its unused dimension were removed; ordinary
LineIndicator metric consumers remain unchanged. Dedicated AppDp dimensions own
segment height and spacing. Representative first/middle/last component previews
and the existing filled User whole-screen preview cover the current theme pair.

Impact assessment: presentation only. Existing stack count determines the same
quarter-step progression, completion still hides shared chrome, and callbacks,
validation, operations, leaf screen hooks and container null hook are untouched.
No telemetry/admin or StateKeeper changes are required. Parent/child drafts,
catalog rebuilding and runtime-only completion retain their existing ownership.
Source review checks fixed geometry, both color producers, accessibility progress
semantics and absence of animation-dependent navigation. Rendered appearance,
keyboard/enlarged-font layout and runtime restoration remain unverified.

Final verification for segmented progress: independent source review found no
remaining blocking findings after moving dimensions to their component owner.
Authorization Android and iOS Simulator compile tasks passed on the resulting
revision (/private/tmp/grippo-step-progress-final-build.log); git diff --check
passed. No automated tests, dependencies or verification hooks were added.

## StepProgressIndicator brand refinement (2026-10-15)

Visual-only follow-up to the segmented progress entry above. The current component
uses its own stepProgressIndicator.fillStart/fillEnd/track color roles in both
concrete themes: orange-to-copper filled capsules and a neutral remaining track,
with 6 dp height and 4 dp gaps. This supersedes the selection.accent/border.default
mapping described in the preceding historical entry; selection and success roles
remain separate.

Usage/state inventory: the single production caller is ProfileCreationProgress in
ProfileCreationScreen, centered at half width in the transparent toolbar above
User, Experience, ExcludedMuscles and MissingEquipments. All four share the screen
surface, status-bar inset and fixed toolbar slot; scroll/IME/CTA ownership stays in
the child screens. Filled versus remaining segments, positions 0 through 3, Back
and forward color transitions and both themes are affected. Completed still hides
the chrome. The primitive also accepts the single-step boundary; invalid counts
and indices retain their require guards. No labels, input, focus, disabled or
loading variants are introduced. Private component AppPreviews cover every actual
step and the single-step boundary at the toolbar width; the existing filled User
whole-screen preview remains the representative host fixture.

Analytics/preservation assessment: no behavior, progress calculation, callback,
validation, operation, route, screen identity or telemetry contract changed. The
five leaf screenPresentation hooks and null container hook are untouched. No live
admin synchronization is triggered. Existing parent/child draft values and keys,
rebuilt catalogs and runtime-only completion retain their owners; the stateless
indicator introduces no saved data or synthetic interactions.

Independent review of this task's delta against its initial working-tree snapshot
found no actionable source defects; unrelated concurrent edits were excluded.
Source gates cover the sole caller, state branches, both theme implementations,
token ownership, stable geometry and unchanged progress semantics. Calculated
track/screen contrast is 3.69:1 light and 3.16:1 dark; this is not render evidence.
Final git diff --check passed. No automated test sources, dependencies or production
verification hooks were added.

Compilation remains unverified. The first attempt could not load the existing
JDK-25 convention-plugin class on JDK 19. A repeat using Studio's JDK 25 was
cancelled after more than five minutes during configuration: the daemon thread
dump showed DefaultFileLockManager waiting on a shared cache lock, before the UI
compile tasks ran. Only this task's Gradle wrapper was stopped; concurrent builds
and project configuration were untouched. Requested checks were authorization
Android/iOS Simulator compilation and shared Debug XCFramework assembly; log:
/private/tmp/grippo-step-brand-build.log.

Studio opened both component preview groups and their light/dark configurations,
but initially reported Render problem; refreshing left blank, Out of date frames.
No final component or whole-screen render is verified. Forward/Back animation,
TalkBack/VoiceOver output, font/keyboard layout, restoration and native iOS rendering
were not exercised. These limits do not establish runtime or visual acceptance.

## MenuSection brand-design pass (2026-10-15)

Visual brief: make profile sections easier to scan through h4 headings, a neutral
32 dp icon container with 12 dp corners and a 22 dp glyph, and 12 dp header gaps.
Default retains the existing 28 dp outlined card with 16 dp inset; Subtle retains
its unframed settings role and secondary heading. The icon container uses existing
screen/card surface tokens; context icon colors and neutral values keep their meaning
in LightColor and DarkColor. No new artwork, color role or accepted departure.

Usage/state source inventory: all four production MenuSection calls are in
profile/menu/components/ProfileMenuContent.kt, inside ProfileMenuScreen's LazyColumn,
under UserProfileHeader and the transparent toolbar in Main's Profile tab. Body has
three user-backed rows, Goal one nullable goal row, Restrictions two nullable counts,
and Subtle Settings two rows plus the existing ADMIN-only diagnostic row. Data can
be populated, missing, zero or independently loading; loading wins over row values.
Rows stay available during loading and retain their original callbacks/dividers.
Default/Subtle, optional header icon, short/long localized titles, both themes and
200% text are covered by source and private AppPreview fixtures. No selection,
validation, disabled or inline-error state exists at the section owner; actual
failures stay in the existing VM/error-dialog pipeline. Scroll, bottom navigation,
insets and pressed row feedback retain their existing owners.

Analytics impact: none. ProfileRouter.Menu -> ProfileMenuComponent ->
ProfileMenuViewModel keeps ProfileScreen.Menu screenPresentation, MenuSelected
callbacks and complete refresh operations. Shared sections emit no analytics;
Main/Profile containers keep their existing null hooks. No entry point, eligibility,
validation, result, telemetry schema or live-admin synchronization trigger changed.
Preservation impact: none. ProfileMenuState user/goal/exclusion counts remain rebuilt
from existing feature observations; loaders, callbacks and scroll presentation remain
runtime-owned. No editable draft, accepted picker result, archive field/key or secret
is added or changed. No tests, dependencies or verification infrastructure added.

Preview coverage: MenuSection populated Default, Subtle settings without values,
loading/missing values, both styles with long Ukrainian copy at 200%; the existing
MenuValueRow preview covers the iconless section. Whole-screen ProfileMenuScreen
previews cover populated/loading/empty/partial plus ADMIN at 200% and zero counts.
AppPreview supplies UK 360 dp light and EN 412 dp dark contexts. Verification
results and unavailable observations are distinguished below.

Self-review and independent source review found no actionable defects in this
task's delta; all four consumers and Default/Subtle/icon branches were inspected.
The reviewer rechecked the current host after concurrent ProfileMenuScreen spacing
changes. Concurrent MenuValueRow/Menu.Item and other profile preview/header edits
are outside this task's authored delta and are preserved. Public MenuSection
signature and all four production calls match the initial working-tree snapshot.
Current theme token contrast for headings is 14.29:1/12.06:1 in light and
16.07:1/8.33:1 in dark (Default/Subtle); header icon contrast ranges from 4.80:1
to 9.05:1. This arithmetic and source review do not establish rendered fidelity.

Verification: git diff --check passed. Scoped compileAndroidMain and
compileKotlinIosSimulatorArm64 were attempted for design-system:components and
ui-screen-features:profile (/private/tmp/grippo-menu-section-build.log).
Gradle stalled during configuration on the shared cache lock, confirmed in the
daemon thread dump; only this task's wrapper was stopped after several minutes
(exit 143). No successful compilation is claimed. Android Studio was concurrently
used by other preview checks; no final MenuSection or profile render was obtained.
Default/Subtle, loading/missing, iconless and enlarged-text groups remain unrendered
in both themes. Live row presses, scrolling to the last settings item above the
bottom navigation, transitions and iOS runtime rendering remain unexercised.

## Design changelist review (2026-10-15)

Reviewed the current design diff, shared default consumers, both color producers,
feature hosts and their existing state/analytics owners. The visual brief remains
neutral working surfaces, warm action/selection, distinct semantic feedback,
stable selection geometry and readable localized content without new business state.

- Auth/setup: Login registration footer wraps; User header/fields scroll within the
  available height while its CTA retains its existing eligibility and callbacks.
- Data cards: PerformanceMetricCard keeps type, value, status and comparison content
  readable in the existing two-column Home grid and full-width history sheet.
  Deterministic previews cover all four nonempty statuses, Empty, no comparison,
  narrow grid cells and enlarged text. Welcome facts/checklist labels may wrap.
- Controls/feedback: existing invalid-input, loading, tab selection, switch label
  and checklist completion facts now have accessibility semantics. Error visibility
  still follows blur; no validation, callback eligibility or operation meaning changed.
- Token owners: focus reuses border.focus; banners reuse semantic backgrounds;
  selected controls use their foreground pair. Dark checked-switch thumb contrast
  is approximately 8.74:1. BrandHeader keeps only its demonstrated logo variant.

Analytics impact: no event/schema/screen identity, operation, consent, transport or
reporting change. All leaf screenPresentation hooks and container null hooks stay
at their existing VMs. This visual-only review requires no live-admin mutation or
browser access under admin-sync's documented exception.

Preservation impact: no draft field, route input, PersistenceKey or restore precedence
changed. Name/body data, selections, filters and accepted parent values keep their
existing StateKeeper owners; catalogs are rebuilt, passwords stay transient and
live dialog sessions remain unsaved. New scrolling is local presentation state.
Manual fresh-owner restoration, native accessibility and SDK delivery are not
established by source review or compilation.

Self-review and independent review covered components, feature consumers and
color/rule owners. Corrections were re-reviewed with no remaining source blockers.
Build and runtime evidence is recorded in the task result; historical checks above
remain evidence only for their stated revisions.

Device evidence for this review: final Android Login was inspected at 360 dp,
200% font scale and Ukrainian app locale in both themes. The email label wraps in
full, registration action remains readable/reachable after scrolling, and the
updated APK completes a cold start. Device font/viewport/theme/app locale were
restored afterward. The initial incremental APK had an incomplete generated
component runtime directory; regenerating that build artifact restored all classes
and removed the observed splash ClassNotFoundException without source/build-policy
workarounds. Setup form, metric grid and other affected states have source/preview
coverage but were not all rendered or exercised on a device. iOS runtime, live
screen-reader output and fresh-owner restoration remain unverified.

## Shared Check consolidation and viewport policy (2026-10-15)

The user's follow-up replaces the card-specific CheckSelectableCardIndicator with
selectors.Check. Large/Medium selectable cards use its passive mode (no callback,
24 dp marker, no extra input/semantics node); their parent retains selected state
and click ownership. OnboardingAnalyticsConsent supplies its original callback,
label and enabled condition, retaining a 48 dp checkbox touch target. Check now
owns one circular 24 dp / 18 dp mark treatment and all its dimension tokens.
Selected/unselected and enabled/disabled previews cover both modes. Toggle remains
the switch control; semantic completion icons are not selection controls.

This is a visual/component ownership change: consent meaning/defaults/storage,
callbacks, screenPresentation hooks, operations and telemetry schemas remain
unchanged. No live-admin synchronization is triggered. No business draft/state,
route, StateKeeper key, restoration order or secret handling changes.

The user explicitly limits default visual acceptance to the standard phone
viewport. The canonical policy is now design-system/references/visual-consistency.md;
AppPreview uses 412 x 915 dp in both themes. Small-device/narrow layouts and default
font/device stress matrices are not acceptance requirements. Historical narrow and
large-font checks above describe their original revisions and are not current rules.

## Final design naming and contract review (2026-10-15)

The follow-up reviews the entire current design changelist, including public API
names, token ownership and actual feature consumers. Earlier review records remain
evidence for their stated revisions; the final contracts below supersede old names.

- ChipInteraction.Static/Clickable describes behavior separately from visual style;
  ChipLeading and leadingSize match the leading icon/marker position. TimelineStyle,
  InputPlaceholder and InputError.None remove inconsistent names. Affected widgets,
  including Toggle, put modifier first; all callers are reconciled.
- Check retains its shared interactive/passive modes with a readable unchecked
  outline in both themes (4.51–6.93:1 against current card/screen backgrounds).
  UserProfileHeader loading dimensions now belong to userCard.headerLoading;
  the obsolete onboardingLogoSize is removed.
- DatePicker removes its unused limitations input. TrainingsViewModel still owns
  date bounds and accepted date actions; disabled widget arrows now also disable.
  Arrow labels are localized in EN/RU/UK. PeriodPicker retains the same callback,
  geometry and tint with one accessible action instead of a nested unnamed button.
- Blank exercise descriptions produce no empty interactive frame; short text has
  no ineffective expand action. Expanded/overflow widget state resets with text.
  FindingGroup uses ImmutableList, and internal metric style stays internal.

Analytics impact: no new feature action, route, screen identity, accepted-operation
meaning, consent or telemetry schema. Calendar/Home callbacks still enter their
existing VMs; no live-admin synchronization is triggered. Preservation impact:
calendar/period selections and user/profile data retain existing owners, routes
and StateKeeper keys. Description expansion is transient widget presentation;
no additional business draft or restoration supplier is introduced.

Self-review and independent re-review resolved the concrete findings with no
remaining source blockers. Acceptance remains 412 x 915 dp at normal font scale;
compilation and device evidence are recorded separately in the task result.

Final verification: Android assembleDebug and shared assembleSharedDebugXCFramework
passed together; the XCFramework contains ios-arm64 and ios-arm64-simulator.
Source/diff, locale keys/placeholders and skill metadata/link checks passed.
The fresh APK cold-started; Login and Registration were inspected in English at
412 x 915 dp, normal font, both themes, including navigation there and back.
Original device settings were restored. Other component/setup/dashboard/picker
states, screen-reader behavior, fresh-owner restoration and iOS runtime were not
exercised in this final pass; source/preview coverage is not runtime evidence.
