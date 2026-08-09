# Reviewer-only gap register

Source: the validation-gates reviewer-check register.
These rules are NOT mechanically validated — they are caught only by the reviewer
(`internal-reviewer` or the official Codex plugin review). The `validation-gates`
skill MUST keep them as explicit reviewer-check items; losing them silently is a
behavior regression.

All entries are currently applicable. Owning skill: `validation-gates`.

## self-enforced
Process rules the producing actor polices; no separate validator.
- `## Outcome appendix` shape — orchestrator writes it (Step 6a); the canonical task-state parser verifies it at read time and projects `malformed`. Source: `validation-gates/references/forbidden-patterns.md` § Outcome appendix.
- `## Orchestrator scope` — orchestrator self-polices "no inline product code" / "no self-review"; user is last-resort gate. Source: `validation-gates/references/forbidden-patterns.md` § Orchestrator scope.

## pattern-orphans
Forbidden patterns no validator catches mechanically — reviewer-only.
- `buildList {}` in state without `.toImmutableList()`. Source: `validation-gates/references/forbidden-patterns.md` § Collections in state.
- Subgrouping `<Product>Api` into `AuthApi`/`NotesApi`. Source: `validation-gates/references/forbidden-patterns.md` § Data layer.
- Compose Navigation alongside Decompose. Source: `validation-gates/references/forbidden-patterns.md` § Navigation.
- Mutable routes (`var` in `*Router.kt`). Source: `validation-gates/references/forbidden-patterns.md` § Navigation.
- Raw `Throwable` from `validateResponse`. Source: `validation-gates/references/forbidden-patterns.md` § Errors.
- PNG icons for vector-candidate use cases. Source: `validation-gates/references/forbidden-patterns.md` § Resources.
- `@JvmStatic` in `commonMain`. Source: `validation-gates/references/forbidden-patterns.md` § Kotlin/common.
- `Channel<>` where `Flow`/state should be used. Source: `validation-gates/references/forbidden-patterns.md` § Coroutine/data flow.
- Logging PII (full tokens/emails). Source: `validation-gates/references/forbidden-patterns.md` § Security/privacy.
- Multiple `Json {}` instances in `commonMain`. Source: `validation-gates/references/forbidden-patterns.md` § Serialization.
- Flat-scalar entity composable (spread scalars not `*State`). Source: `validation-gates/references/forbidden-patterns.md` § Compose.
- Inline `ImageVector.Builder` placeholders in DS component previews. Source: `validation-gates/references/forbidden-patterns.md` § Resources.
- Non-`*Screen` UI flat beside the seven MVI files. Source: `validation-gates/references/forbidden-patterns.md` § Architecture-shape.
- Flat scalar pile in `*State` (15+ primitives / lifecycle booleans). Source: `validation-gates/references/forbidden-patterns.md` § State.

## duplicate-finding-knowns
Same call site flagged by 2+ validators with different `rule_id` (bypasses dedup; kept for parity).
- Forbidden coroutine APIs; `LaunchedEffect(Unit){navigate}`; `mutableStateOf` for logical state;
  immutable-collection violations; `MaterialTheme.*`/material3 in feature code;
  `@PrimaryKey(autoGenerate=true)`; `Flow<Result<>>`; hand-DSL `module{single{}}`;
  `getKoin().get()` in Composable; missing module in `:shared/Koin.kt`; cross-feature imports;
  `var` field in state; `println`/`android.util.Log` in commonMain;
  inline `dp`/`sp`/`Color(0x…)` in screens; `Contract.Empty` completeness (dedup-guarded). Source: `validation-gates/references/forbidden-patterns.md` + validator contracts.
