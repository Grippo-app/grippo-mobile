# Contract: validator-finding

Normalized validator finding. Frozen so the orchestrator's dedup/routing stays stable across validator implementations.

Source: the `validation-gates` skill (finding fields `file, rule_id, severity, evidence, routed_to, fix`; severity scale Blocker/Critical/Major/Minor/Advisory; high-severity = Blocker|Critical|Major).

## JSON shape

```json
{
  "validator": "mvi-contract-validator",
  "version": 1,
  "status": "pass|fail|skipped",
  "required": true,
  "condition": "task touches UI state",
  "skip_reason": null,
  "disabled_by": null,
  "file": "shared/feature/foo/Bar.kt",
  "line": 42,
  "rule_id": "mvi.contract.empty-completeness",
  "severity": "Blocker|Critical|Major|Minor|Advisory",
  "evidence": "…",
  "routed_to": "screen-builder",
  "fix": "…",
  "blocking": true,
  "suggestion_only": false,
  "dedup_key": "shared/feature/foo/Bar.kt::mvi.contract.empty-completeness"
}
```

## Pins (fail-closed)

- Required keys: `validator, version, status, file, rule_id, severity, evidence, routed_to, fix, dedup_key`.
- `status` ∈ `pass|fail|skipped`; `severity` ∈ `Blocker|Critical|Major|Minor|Advisory`.
- **`status: skipped` requires a non-null `skip_reason`** (a required conditional validator cannot vanish silently — it fails closed without an explicit reason).
- `suggestion_only`/advisory findings are NOT routed as blocking.
- `dedup_key` = `<file>::<rule_id>` (orchestrator dedups by `(file, rule_id)`).
