# Convention plugins and version catalog

Actual definitions live under `build-logic/convention/`; aliases/versions live in
`gradle/libs.versions.toml`, and target/global flags in `gradle.properties` plus conventions.

- Pure KMP, Compose, Koin, Room and shared export modules apply the existing appropriate plugins.
  Read their real plugin list; do not carry a second copied plugin/version matrix in the skill.
- Add global opt-ins in KotlinMultiplatformConventionPlugin only when the task authorizes that
  convention change; do not add source annotations as a bypass or edit conventions incidentally.
- Adding catalog entries, renaming aliases, changing versions or module membership requires scope
  covering the change. Inspect every consumer of an alias before authorized rename/removal.
- Resolve compatibility using authoritative vendor documentation when a version change is requested.
  Existing source controls the pinned baseline, not a skill's historical numeric table.
- Commented flags are inactive. Do not describe `kotlin.native.binary.gc=cms` as enabled merely
  because the text appears in gradle.properties. Profiling/runtime behavior requires actual evidence.
