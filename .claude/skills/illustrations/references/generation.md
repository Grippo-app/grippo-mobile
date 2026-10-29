# Illustration generation

Apply the [visual identity](../SKILL.md#visual-identity) before generation.
Apply the [completion gate](../SKILL.md#completion) to the exported result.

New raster artwork is produced with the image-generation tool available in the
session, if any; inspect its actual capabilities rather than assuming a provider
or tool name.
Generation runs in the main session (project agents have no MCP image tools); the
`design-implementer` integrates the exported asset.
When no tool is available, report generation as unavailable and deliver the
guidance and prompt below instead; do not fall back silently to a different image
service or model. Inspect the tool's actual reference and transparency inputs
before relying on them.

Supply the approved local image as the style reference through the tool's
supported reference mechanism. Distinguish the style reference from the edit
target. For a new subject, explicitly request that subject rather than preserving
the reference's dumbbell. For a refinement or variant, preserve the target's
subject and change only the requested properties. MUST inspect an edit target
before editing it.

Choose the screen role, subject, aspect ratio and intended display size before
generation. Default to a transparent square cutout for a single object, as in
onboarding. Wider compositions are appropriate when their real container needs
them; do not stretch a square image to fill a banner. For a series, reuse one
style prompt and reference while changing the subject-specific composition.

Suggested prompt core, adapted to the actual request:

> Use the attached Grippo artwork as the visual style reference. Create [subject]
> for [actual screen purpose], not a UI mockup. Premium sculptural 3D product
> illustration, matte midnight charcoal bodies, warm orange metallic accents,
> subtle copper highlights and beveled edges, soft studio light and gentle warm
> rim light. [Meaningful supporting element, if needed.] Balanced [aspect ratio]
> composition, clear silhouette, entire subject visible with generous margins.
> Transparent background and a soft floating shadow. No text, numbers, logos or
> watermark. Match the reference's materials and color balance, not its subject.

Cutouts MUST end up with real alpha: request transparency through whatever input
the tool offers, preserve the alpha through export and verify it in the converted
WebP. A checkerboard, white backdrop or painted black background is not
transparency. For an explicitly requested full-background scene, adapt the prompt
accordingly. Do not generate additional variants without a concrete quality
problem or user request.
