# Main analytics coverage

Current scope, 2026-10-20: MainRouter → MainComponent → retained MainViewModel →
MainScreen. Home/Calendar/Library/Profile render their existing child owners
(HomeRootComponent, TrainingsRootComponent, ExercisesRootComponent in
`ExercisesRouter.Mode.Browse`, ProfileComponent); the Library tab's screen hook
and callbacks are recorded in [exercises](../exercises/COVERAGE.md). Main is a
container and keeps the default null screenPresentation.

| Owner/callback | Observation | Eligibility and population |
|---|---|---|
| MainViewModel.onSelectTab | TabSelected(tab), main_tab_selected v1 | Existing UI callback; bounded Home/Calendar/Library/Profile mapping (`main_tab` = home/calendar/library/profile), including reselect clicks |
| MainViewModel.onStartTraining | StartTrainingClicked, main_start_training_clicked v1 | Direct central action callback before existing draft/suggestion business flow; UI disables its button during StartTraining loader |
| DraftTraining onStartNew continuation | Deliberate omission | Calls resumeOrStartTraining directly, never synthesizes a second central-button click; DraftTraining owns its real StartNewClicked |
| MainViewModel.toTrainings(route) (Home relay into `MainTabNavigation.open(MainRouter.Calendar(route))`) | Deliberate omission | Programmatic tab opening from Home's last-workout card; Home's TrainingsClicked owns the click and no TabSelected is synthesized |

MainAnalytics is injected inside retainedInstance, exhaustive encoding/catalog and
qualified singleton are registered once in shared. No navigation/config/state field
carries telemetry; no screen view, operation API, raw value or business retry is added.
Back and branch/open/outcome attribution remain explicit omissions. No new busy
policy is imposed: existing callbacks and eligible business operations are preserved.

State impact: no new inputs, filters, selections or draft owner in Main. The
current tab continues to be represented by MainComponent's existing childStack
(`MainRouter.serializer()`, key `MainTabs`; Library restores like any other tab
and Back from it selects Home). `MainRouter.Calendar` carries the opened
`TrainingsRouter` and an `@EncodeDefault` FlowId: `select` (bottom bar) brings
an already opened config of the same tab class to front and keeps its child,
`open` (Home relay) replaces it with a fresh child; the bottom bar highlights by
tab class. The Library child's own draft is owned by the
exercises feature. Durable workout
drafts remain TrainingFeature/DAO-owned; suggestion settings remain LocalSettings.
No telemetry provenance or extra StateKeeper key is introduced.

Manual checks: direct central-button click with/without a draft and with/without a
goal suggestion; StartNew continuation emits no extra central click; each of the
four tabs and a reselect emits its bounded tab; denied consent suppresses all delivery. Reporting
Exceptions must preserve navigation. Verification: current ../COVERAGE.md review.
