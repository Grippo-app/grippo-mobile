# Backend transport

Inspect `data-services/backend/.../client/BackendClient.kt`, `ClientLogger.kt`, `TokenProvider.kt`
and `toolkit/http-client/`. Transport changes require authorization covering network core.

Keep current HTTP engine, validator, serialization, locale header, timeout and request behavior.
Do not invent hosts or replace authenticated transport for a feature. Shared HTTP errors are
translated by the existing validator/parser; business layers retain their declared result contracts.

Transport/local logs must never contain passwords, authorization codes, provider/ID/access/refresh
or push tokens, Authorization headers, or secret prefixes/hashes. Auth logging is restricted to
method/path/status and never includes headers or request/response bodies. Current BackendClient
uses INFO logging with auth-path exclusion; do not restore ALL/BODY logging as a debug shortcut.
Review actual logging configuration and request paths, not only logger message formatting.

Do not change refresh retry/mutex/circuit-breaker behavior without the user's authorized scope.
