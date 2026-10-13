# AppPreview and PreviewContainer

Use the existing [preview helpers](../../../../design-system/preview/src/commonMain/kotlin/com/grippo/design.preview/Preview.kt),
not copied annotation/theme/Coil implementations. AppPreview supplies the current
phone/locale configurations; PreviewContainer owns preview tokens/theme and image
handling. Inspect its real signature before choosing an additional theme/config.

- A changed/new screen or widget has at least one private named AppPreview in its
  file, inside PreviewContainer. Screens/dialogs use Contract.Empty; widgets use
  lightweight parameters and existing stub data, never a live ViewModel/Koin.
- Cover meaningful empty/loading/content/error/selection variants relevant to the
  change. Start from existing ui-core state stubs and explicitly override the facts
  that distinguish the variant; keep compared IDs/dates/set counts deterministic.
- Do not add fictional AppPreviewLight/Tablet APIs, a source opt-in already covered
  by conventions or a duplicate provider. New preview facilities need a real
  requested use case, not a generic template suggestion.
- Component previews alone do not establish whole-screen visual quality. Inspect
  consumers, layout/insets, enlarged fonts and declared themes/locale states when
  rendering is permitted; do not infer visual success from compilation.
- Use actual supplied design content for fidelity comparisons. Record unrendered
  states and unavailable platform/runtime inputs. Do not add capture automation,
  test sources/dependencies or production verification hooks.

Follow [visual review](../../validation-gates/references/visual-review.md) and
[visual consistency](visual-consistency.md) for affected whole-screen consumers.
