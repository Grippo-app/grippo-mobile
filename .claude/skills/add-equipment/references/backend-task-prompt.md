# Backend task prompt

After an equipment integration leaves server work pending, including a
mobile-only integration, generate a short, filled prompt asking the backend
agent to add the equipment as a distinct, independently selectable item.
A request only for this handoff uses the same procedure.

An artwork-only change needs no backend task. When the matching type and catalog
row are already verified in the target environment, report that no backend work
is needed. Mobile source support alone does not prove backend availability.

## Gather the mobile facts

Read the final mobile implementation and the relevant sources in the
[integration map](integration-map.md). Include:

- Equipment name and a short description of its mechanism.
- The exact, case-sensitive API `type` used in mobile, read from its raw key in
  `EquipmentEnum`; an enum member, resource key or vector name is not that value.
- The exact raw group `type` used in mobile, reconciled with `EquipmentGroupEnum`
  and the equipment's group assignment.
- What the entered weight means, using the implemented hint and the agreed
  equipment semantics: for example total plate weight, weight per side, selected
  stack weight or assistance weight. State included/excluded machine or bar
  weight when that distinction affects entry.
- How it differs from the closest existing equipment, naming those positions
  and their API types so the backend agent can preserve their separate identities.

These are facts about the mobile implementation. Read them from the final
working tree and accepted task decisions; do not substitute a proposed synonym
for the implemented API type or invent missing product semantics. A mobile
display name describes the item; it does not prescribe a server storage field.

## Backend responsibility

Generating the handoff requires no backend repository knowledge or access.
Do not ask the user to select a backend implementation mode or answer questions
about SQL, migrations, the server database, deployment or implementation methods.
The receiving backend agent determines these from its own source and project
rules. Keep those implementation decisions out of the prompt's requirements.

## Compose the task

Fill this short template in the user's language. Brackets mark fields to replace,
not API examples. Keep the description and distinction to one short sentence each.

```text
Add [equipment name] to the Grippo backend as a distinct, independently selectable equipment item.
- Description: [mechanism].
- API type used in mobile: `[exact raw equipment type]`.
- Group type: `[exact raw group type]`.
- Entered weight: [what the value includes and means].
- Difference from [existing names and API types]: [distinction; preserve those items separately].
Determine the backend implementation from your repository's source and project rules.
```

## Final response delivery

For every addition requiring this handoff, the final response MUST contain the
complete, filled backend prompt in the user's language in one fenced `text`
block. The user must be able to copy that block into a backend task directly.
A saved copy or link may supplement it. Sending it or creating another chat
requires the user's explicit instruction under the task-coordination rules.

## Handoff check

- The prompt asks to add a distinct, independently selectable backend item.
- All five mobile facts above are filled; exact equipment and group types agree
  with the final mobile source, and existing similar items remain separate.
- The prompt is short and leaves backend implementation decisions to the
  receiving agent without requiring backend access or infrastructure answers.
- The final response passes [final response delivery](#final-response-delivery).
