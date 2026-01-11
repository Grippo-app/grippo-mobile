<div align="center">
  <h2>Grippo Mobile · Kotlin Multiplatform Fitness</h2>
  <p><b>One codebase · Two platforms · Modern architecture</b></p>
  <img src="https://img.shields.io/badge/Kotlin_Multiplatform-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="KMP">
  <img src="https://img.shields.io/badge/Compose_Multiplatform-3DDC84?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose MPP">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/iOS-000000?style=for-the-badge&logo=apple&logoColor=white" alt="iOS">
</div>

#### Requirements to build
```agsl
./gradlew syncSecureConfigs
```

## Overview

Grippo is a fitness companion app built from scratch with Kotlin Multiplatform and Compose
Multiplatform.  
The goal is to provide athletes and gym enthusiasts with **smart workout tracking**
exercise suggestions**, and a **unified UX** across Android and iOS.

###

This project demonstrates:

- ⚙️ Advanced **modular KMP architecture** with feature-based layers
- 🎨 Fully custom **design system** built on Compose Multiplatform
- 📦 Shared **data, networking, and caching** between platforms

### Screenshots

<div align="center">
  <h4>Part 1</h4>
  <a href="https://github.com/user-attachments/assets/b08d0d64-08a3-483b-a04c-08cdb23777de"><img src="https://github.com/user-attachments/assets/b08d0d64-08a3-483b-a04c-08cdb23777de" alt="Screen 1" width="110"></a>
  <a href="https://github.com/user-attachments/assets/51c653ac-f93e-4377-a71b-02c24996c283"><img src="https://github.com/user-attachments/assets/51c653ac-f93e-4377-a71b-02c24996c283" alt="Screen 2" width="110"></a>
  <a href="https://github.com/user-attachments/assets/290e2b39-6bf7-456c-a865-fd4ff362bc97"><img src="https://github.com/user-attachments/assets/290e2b39-6bf7-456c-a865-fd4ff362bc97" alt="Screen 3" width="110"></a>
  <a href="https://github.com/user-attachments/assets/2d34868a-b4fe-42b4-86de-fe99a4ae0c00"><img src="https://github.com/user-attachments/assets/2d34868a-b4fe-42b4-86de-fe99a4ae0c00" alt="Screen 4" width="110"></a>
  <a href="https://github.com/user-attachments/assets/615ceafd-cf07-4bfd-b134-03b0d044b709"><img src="https://github.com/user-attachments/assets/615ceafd-cf07-4bfd-b134-03b0d044b709" alt="Screen 5" width="110"></a>
  <a href="https://github.com/user-attachments/assets/bd31147f-7502-4a30-8c3f-a0191e458b49"><img src="https://github.com/user-attachments/assets/bd31147f-7502-4a30-8c3f-a0191e458b49" alt="Screen 6" width="110"></a>
  <br/>
  <h4>Part 2</h4>
  <a href="https://github.com/user-attachments/assets/2667be13-ec39-4b09-a030-829ca48653e9"><img src="https://github.com/user-attachments/assets/2667be13-ec39-4b09-a030-829ca48653e9" alt="Screen 7" width="110"></a>
  <a href="https://github.com/user-attachments/assets/915cdf11-f944-485d-b565-ae3d2fbd11ed"><img src="https://github.com/user-attachments/assets/915cdf11-f944-485d-b565-ae3d2fbd11ed" alt="Screen 8" width="110"></a>
  <a href="https://github.com/user-attachments/assets/9ee14de9-d856-4da5-9ca8-b0ba5cf0656f"><img src="https://github.com/user-attachments/assets/9ee14de9-d856-4da5-9ca8-b0ba5cf0656f" alt="Screen 9" width="110"></a>
  <a href="https://github.com/user-attachments/assets/fd18ed10-d218-4f71-a88d-2275cd0c70f2"><img src="https://github.com/user-attachments/assets/fd18ed10-d218-4f71-a88d-2275cd0c70f2" alt="Screen 10" width="110"></a>
  <a href="https://github.com/user-attachments/assets/d802a8e4-2d97-4b49-a13b-dd24343515cf"><img src="https://github.com/user-attachments/assets/d802a8e4-2d97-4b49-a13b-dd24343515cf" alt="Screen 11" width="110"></a>
   <a href="https://github.com/user-attachments/assets/b9fcdde0-176e-4abb-8338-05133b171dab"><img src="https://github.com/user-attachments/assets/b9fcdde0-176e-4abb-8338-05133b171dab" alt="Screen 12" width="110"></a>
</div>

### Stack & Highlights

| Area          | Tools / Summary                                                            |
|---------------|----------------------------------------------------------------------------|
| UI            | Compose Multiplatform (shared UI for Android & iOS)                        |
| Architecture  | Modular by feature/layer; Clean-style boundaries; Decompose navigation     |
| DI            | Koin (KSP)                                                                 |
| Networking    | Ktor HTTP client (multiplatform)                                           |
| Persistence   | Room (multiplatform)                                                       |
| Toolkit       | HTTP client, serialization, image loader, connectivity, date utils, logger |
| Design System | Tokens, resources provider, reusable components                            |

### Architecture at a glance

- **App shells**: `androidApp`, `iosApp`
- **Shared core**: `shared` (bootstraps DI, wires features)
- **UI**: `ui-screen-features/*`, `ui-dialog-features/*`, `ui-core/*`, `design-system/*`,
  `compose-libs/*`
- **Data**: `data-features/*`, `data-services/*` (backend, database, ai-agent, firebase, google-auth), `data-mappers/*`
- **Toolkit**: `toolkit/*` (http-client, serialization, logger, connectivity, date-utils, theme,
  localization)
- **Build logic**: `build-logic/*`

### Engineering Highlights

- Designed a scalable modular KMP architecture with isolated feature modules
- Integrated Room database for both Android and iOS targets
- Unified dependency injection and DI graph across native platforms
- Optimized build times using custom Gradle convention plugins
- Built a shared Compose-based design system with adaptive theming

### Setup

1. Clone the repository
2. Open in Android Studio (Kotlin Multiplatform enabled)
3. Build Android: `./gradlew :androidApp:assembleDebug`
4. Build iOS framework: `./gradlew :shared:assembleSharedDebugXCFramework`
5. Run iOS project via Xcode

### Author

**Max Voitenko**  
📫 [voitenko.dev@gmail.com](mailto:voitenko.dev@gmail.com)  
💼 [LinkedIn](https://www.linkedin.com/in/max-voitenko) | [GitHub](https://github.com/voitenkodev)

