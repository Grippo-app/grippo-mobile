# Updated dependency compatibility repair

Keep the user-upgraded versions where compatible. Remove the unsupported Intel iOS simulator target, check dependency resolution and migrate API changes surfaced by builds. Preserve release UI and project skills.

1. KMP target and Gradle model: remove iosX64; resolve all common/native dependencies; verify Gradle configuration and metadata compilation.
2. Android: fix compiler/build failures; assemble debug APK and launch on an available emulator.
3. iOS: compile both ARM targets and build XCFramework; build and launch Xcode app on simulator.
4. Review diff and record exact verification, including any host limitations.

Risks: upgraded libraries may require API migrations; actual runtime checks require simulator/emulator availability. Do not reset or broadly downgrade user upgrades.
