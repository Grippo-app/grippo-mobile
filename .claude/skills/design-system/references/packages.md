# Grippo packages

Use the actual neighboring package and source directory as the naming authority.
The root is `com.grippo`; many checked-in directories contain dotted package segments.
Do not move or normalize existing paths as part of a feature change.

| Area | Current package examples |
|---|---|
| UI feature | `com.grippo.authorization.login`, `com.grippo.profile` |
| Foundation | `com.grippo.core.foundation` |
| UI state | `com.grippo.core.state` |
| Design components | `com.grippo.design.components.toolbar` |
| Resource provider | `com.grippo.design.resources.provider` |
| Domain contracts | `com.grippo.data.features.api` |
| Backend DTOs | `com.grippo.services.backend.dto` |
| Toolkit | Inspect the owning module's existing declaration before adding a file |

One MVI package per screen/dialog; extracted feature UI goes in its `components/`
package. Reusable state goes in ui-core:state; business conversions in the owning
mapper direction. Imports between feature implementation modules remain forbidden.
Public API packages remain minimal; helpers keep internal/private visibility.
