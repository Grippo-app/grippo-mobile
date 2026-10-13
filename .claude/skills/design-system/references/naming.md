# Grippo naming and file placement

[CLAUDE.md](../../../../CLAUDE.md#code-style-and-naming) owns project naming,
visibility, Kotlin style and package conventions. Preserve existing public APIs;
new conventions do not rename unrelated callers or directory layouts.

| Owner/type | Convention |
|---|---|
| Screen MVI | NameComponent, Contract, State, Direction, Loader, ViewModel, Screen |
| Domain service | XFeature interface; internal XFeatureImpl and XRepositoryImpl |
| New command UseCase | VerbNounUseCase with execute; preserve existing throwing/stream APIs |
| Mapper | Source.toTarget / toTargetOrNull, in its directional area file |
| Composable | PascalCase; widget file matches the principal function |
| Callback | onXAction; preserve the existing Contract vocabulary |
| Resource key | Descriptive snake_case without ic_/img_/bg_ prefixes |
| AppIcon extension | PascalCase ImageVector name |
| Router/config | NameRouter; nested DialogConfig subtype |

Independent new types use separate files. Tightly related sealed variants and
existing coherent model families MAY stay together; MUST NOT split or expand them
mechanically.
Top-level extension families may share their existing mapper/token file. This is
not a rule requiring every function in its own file. New directories use normal
slash-separated packages; preserve existing dotted directories during feature work.

Meaningful State defaults belong in constructors, not a companion Empty snapshot;
required data stays explicit and no-argument construction is not mandatory.
Contract companion Empty supplies no-op callbacks for previews. Use real existing stub data.
Do not introduce a shadow isEdited/dirty/restorePending state to satisfy naming;
actual field ownership and preservation rules determine whether a value exists.
