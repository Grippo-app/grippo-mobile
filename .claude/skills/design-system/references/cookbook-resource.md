# Add a resource

[Resource policy](resources.md) owns localization/asset constraints;
[accessors](accessors.md) own resolution APIs. Apply this recipe only to the demanded
asset and its actual consumers.

1. Inspect provider build, existing locale directories, producer signature and
   intended caller. Confirm the real source artifact before conversion.
2. Add the resource under its owning convention. Strings/plurals cover existing
   locales and placeholder contracts; fonts map actual files to registered weights.
3. For UI icons, inspect neighboring AppIcon extensions and preserve supplied vector
   paths/viewport with the established cached backing-field convention. Export
   metadata alone is not path data.
4. Integrate through the appropriate AppTokens/StringProvider/UiText boundary.
   Add a missing accessor only for a real consumer requirement; inspect the current
   producer rather than assuming an overload exists.
5. Review consumers, locale/placeholder parity, font registrations and actual asset
   data. Run the relevant resource/platform build when needed under the
   [verification policy](../../../../CLAUDE.md#verification-policy), inspecting
   current task/output wiring before claiming generated identifiers resolve.

For a supplied design-critical icon or image, use its authoritative artifact or an
approved equivalent. If neither is available and replacement affects fidelity,
resolve that design choice through the [scope policy](../../../../CLAUDE.md#when-to-stop-and-ask)
before substituting an unrelated icon or redrawing it.
