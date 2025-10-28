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
  <h4>iOS</h4>
  <a href="https://github.com/user-attachments/assets/6004f07c-9873-400d-a499-86561329c0f2"><img src="https://github.com/user-attachments/assets/6004f07c-9873-400d-a499-86561329c0f2" alt="iOS 1" width="120"></a>
  <a href="https://github.com/user-attachments/assets/168fa3a0-e2a9-42a8-b9a2-9f8d2292646b"><img src="https://github.com/user-attachments/assets/168fa3a0-e2a9-42a8-b9a2-9f8d2292646b" alt="iOS 2" width="120"></a>
  <a href="https://github.com/user-attachments/assets/7093f392-1a1d-4e3a-9f54-adaf4a8c07b7"><img src="https://github.com/user-attachments/assets/7093f392-1a1d-4e3a-9f54-adaf4a8c07b7" alt="iOS 4" width="120"></a>
  <a href="https://github.com/user-attachments/assets/e14437ff-ce3c-413e-b10a-5c02030fa69e"><img src="https://github.com/user-attachments/assets/e14437ff-ce3c-413e-b10a-5c02030fa69e" alt="iOS 6" width="120"></a>
  <a href="https://github.com/user-attachments/assets/15f6e5d1-ffa4-4897-bc31-d3875f28a108"><img src="https://github.com/user-attachments/assets/15f6e5d1-ffa4-4897-bc31-d3875f28a108" alt="iOS 5" width="120"></a>
  <br/>
  <h4>Android</h4>
  <a href="https://github.com/user-attachments/assets/2d29dfa6-08ad-4335-8700-9641b4b29d35"><img src="https://github.com/user-attachments/assets/2d29dfa6-08ad-4335-8700-9641b4b29d35" alt="Android 3" width="120"></a>
  <a href="https://github.com/user-attachments/assets/1d33863e-7faa-41fd-8b2c-83818e00475a"><img src="https://github.com/user-attachments/assets/1d33863e-7faa-41fd-8b2c-83818e00475a" alt="Android 5" width="120"></a>
  <a href="https://github.com/user-attachments/assets/50d2c598-a58b-4e85-9859-78ef8c5a491d"><img src="https://github.com/user-attachments/assets/50d2c598-a58b-4e85-9859-78ef8c5a491d" alt="Android 1" width="120"></a>
  <a href="https://github.com/user-attachments/assets/e0d0544b-eb56-4339-8c0f-a1ac1aa53882"><img src="https://github.com/user-attachments/assets/e0d0544b-eb56-4339-8c0f-a1ac1aa53882" alt="Android 2" width="120"></a>
  <a href="https://github.com/user-attachments/assets/5fbcc87b-0dd7-4379-9437-a60032db982c"><img src="https://github.com/user-attachments/assets/5fbcc87b-0dd7-4379-9437-a60032db982c" alt="Android 4" width="120"></a>
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
