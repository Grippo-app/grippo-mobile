# mappers — trigger fixtures

Realistic prompts that SHOULD activate the `mappers` skill (DTO↔domain↔state directional modules).

- add a DTO→Entity mapper for the profile area
- add the Entity→Domain mapper in the :data-mappers module
- map the Domain→State direction for the exercise list
- the DTO shape changed — update the existing mapper to follow
- handle the nullable DTO fields with the null-and-drop logging rule
- add a State→Domain mapper for the form submission
