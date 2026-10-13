# Main analytics coverage

Current scope, 2026-10-14: MainRouter → MainComponent → retained MainViewModel →
MainScreen. Home/Calendar/Profile render their existing child owners. Main is a
container and keeps the default null screenPresentation.

| Owner/callback | Observation | Eligibility and population |
|---|---|---|
| MainViewModel.onSelectTab | TabSelected(tab), main_tab_selected v1 | Existing UI callback; bounded Home/Calendar/Profile mapping, including reselect clicks |
| MainViewModel.onStartTraining | StartTrainingClicked, main_start_training_clicked v1 | Direct central action callback before existing draft/suggestion business flow; UI disables its button during StartTraining loader |
| DraftTraining onStartNew continuation | Deliberate omission | Calls resumeOrStartTraining directly, never synthesizes a second central-button click; DraftTraining owns its real StartNewClicked |

MainAnalytics is injected inside retainedInstance, exhaustive encoding/catalog and
qualified singleton are registered once in shared. No navigation/config/state field
carries telemetry; no screen view, operation API, raw value or business retry is added.
Back and branch/open/outcome attribution remain explicit omissions. No new busy
policy is imposed: existing callbacks and eligible business operations are preserved.

State impact: no new inputs, filters, selections or draft owner. The current tab
continues to be represented by MainComponent's existing childStack. Durable workout
drafts remain TrainingFeature/DAO-owned; suggestion settings remain LocalSettings.
No telemetry provenance or extra StateKeeper key is introduced.

Manual checks: direct central-button click with/without a draft and with/without a
goal suggestion; StartNew continuation emits no extra central click; each tab and
reselect emits its bounded tab; denied consent suppresses all delivery. Reporting
Exceptions must preserve navigation. Verification: current ../COVERAGE.md review.
