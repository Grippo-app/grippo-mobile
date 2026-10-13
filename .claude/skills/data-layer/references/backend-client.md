# Backend transport

Inspect `data-services/backend/.../client/BackendClient.kt`, `ClientLogger.kt`, `TokenProvider.kt`
and `toolkit/http-client/`. Transport changes require authorization covering network core.

Keep current HTTP engine, validator, serialization, locale header, timeout and request behavior.
Do not invent hosts or replace authenticated transport for a feature. Shared HTTP errors are
translated by the existing validator/parser; business layers retain their declared result contracts.

BackendClient uses INFO logging through ClientLogger and AppLogger, including auth requests.
There is no auth-specific response sanitation or request marker.
Review actual logging configuration and request paths, not only logger message formatting.

Do not change refresh retry/mutex/circuit-breaker behavior without the user's authorized scope.
