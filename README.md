<div align="center">
  <h2>Grippo Mobile · Kotlin Multiplatform Fitness</h2>
  <p><b>One codebase · Two platforms · Modern architecture</b></p>
  <img src="https://img.shields.io/badge/Kotlin_Multiplatform-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="KMP">
  <img src="https://img.shields.io/badge/Compose_Multiplatform-3DDC84?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose MPP">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/iOS-000000?style=for-the-badge&logo=apple&logoColor=white" alt="iOS">
</div>

### Overview
Grippo Mobile is a modern fitness application built with Kotlin Multiplatform and Compose Multiplatform, delivering a shared UI and business logic for Android and iOS with a clean, modular architecture.

### Screenshots
<div align="center">
  <h4>Android</h4>
  <a href="docs/screenshots/android/1.png"><img src="docs/screenshots/android/1.png" alt="Android 1" width="180"></a>
  <a href="docs/screenshots/android/2.png"><img src="docs/screenshots/android/2.png" alt="Android 2" width="180"></a>
  <a href="docs/screenshots/android/3.png"><img src="docs/screenshots/android/3.png" alt="Android 3" width="180"></a>
  <a href="docs/screenshots/android/4.png"><img src="docs/screenshots/android/4.png" alt="Android 4" width="180"></a>
  <br/>
  <h4>iOS</h4>
  <a href="docs/screenshots/ios/1.png"><img src="docs/screenshots/ios/1.png" alt="iOS 1" width="180"></a>
  <a href="docs/screenshots/ios/2.png"><img src="docs/screenshots/ios/2.png" alt="iOS 2" width="180"></a>
  <a href="docs/screenshots/ios/3.png"><img src="docs/screenshots/ios/3.png" alt="iOS 3" width="180"></a>
  <a href="docs/screenshots/ios/4.png"><img src="docs/screenshots/ios/4.png" alt="iOS 4" width="180"></a>
</div>

### Stack & Highlights
| Area | Tools / Summary |
|---|---|
| UI | Compose Multiplatform (shared UI for Android & iOS) |
| Architecture | Modular by feature/layer; Clean-style boundaries; Decompose navigation |
| DI | Koin (KSP) |
| Networking | Ktor HTTP client (multiplatform) |
| Persistence | Room (multiplatform) |
| Toolkit | HTTP client, serialization, image loader, connectivity, date utils, logger |
| Design System | Tokens, resources provider, reusable components |
| AI | Dedicated AI agent service integrated in data layer |

### Architecture at a glance
- **App shells**: `androidApp`, `iosApp`
- **Shared core**: `shared` (bootstraps DI, wires features)
- **UI**: `ui-screen-features/*`, `ui-dialog-features/*`, `ui-core/*`, `design-system/*`, `compose-libs/*`
- **Data**: `data-features/*`, `data-services/*` (backend, database, ai-agent), `data-mappers/*`
- **Toolkit**: `toolkit/*` (http-client, serialization, logger, connectivity, date-utils, theme, localization)
- **Build logic**: `build-logic/*`

### Contact
- **Email**: [voitenko.dev@gmail.com](mailto:voitenko.dev@gmail.com)
- **LinkedIn**: [Max Voitenko](https://www.linkedin.com/in/max-voitenko)
- **GitHub**: [voitenkodev](https://github.com/voitenkodev)
