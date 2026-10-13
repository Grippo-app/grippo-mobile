# Adding a resource

[resources.md](resources.md) owns asset/localization policy;
[accessors.md](accessors.md) owns consumer APIs. This recipe does not duplicate them.

1. Inspect the actual provider build, composeResources locale directories, producer
   signature and caller. MUST use real existing paths and identifiers; no assumed
   `supportedLocales` variable or fictional product API.
2. Add the demanded asset. Strings/plurals MUST include every existing locale and
   matching explicit positional placeholders. Plural XML MUST have a resources root
   and the locale's actual quantity categories.
3. For a UI icon, inspect neighboring AppIcon extensions and source the real vector
   paths. Keep its semantic PascalCase name and cached backing-field convention.
   An abbreviated builder with empty paths is never a finished asset. Use the
   supplied source viewport; do not assume that every export uses 24×24 units.
4. For a font, match file names and register actual weights in the existing factory;
   then add only demanded typography tokens. New typefaces follow authorized scope.
5. Use AppTokens in composition, StringProvider outside it, and UiText.Res for
   locale-dependent state. A composable plural requirement needs a real AppString
   wrapper because one is not currently implemented.
6. Review affected callers, locale/placeholder parity and actual asset data. Use a
   relevant scoped resource/platform build when needed under the canonical
   [verification policy](../../../../CLAUDE.md#verification-policy). Generated Res
   accessors resolve after resource generation; MUST NOT report an unrun build or
   assume a task/output exists without inspecting the current wiring.

MUST distinguish vector path data from an export manifest containing only
size/type/fill metadata. Inspect the actual supplied artifact; a filename such as
figma-export.json alone does not prove presence or absence of paths. Resolve an
unavailable design source using the scope quiz before replacing design-critical art.
Reuse a suitable existing icon or supplied vector source. When neither fits, MUST
ask which replacement is acceptable; MUST NOT silently substitute a close-enough
Material icon or draw a replacement for a supplied design.
