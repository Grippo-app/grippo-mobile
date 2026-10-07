# Contract: builder-report

Normalized envelope a builder returns to the orchestrator. Frozen so the
orchestrator can route the next step deterministically (done → next builder;
blocked/failed → surface; skipped → skip). There is one current protocol:
`schemaVersion: 1`. Any other version, missing field, extension field or
ambiguous nested shape is rejected. There is no migration or compatibility
reader.

Canonical owners: `builder-report.schema.json` and
`../tasks/task-builder-report-contract.cjs`. The orchestrator passes the exact
builder JSON bytes to that parser over closed stdin; no report file is used as
an alternate authority.

Source: the builder capabilities across the domain skills + orchestrator
Step 3/4 hand-off. `tests_*`, `developer_runs` and `fail_before_claim` are
builder CLAIMS: the certifying gate matches them against the observed
footprint and executor receipts and never trusts the report itself.

## JSON shape

```json
{
  "schemaVersion": 1,
  "agent": "screen-builder",
  "status": "done|blocked|failed|skipped",
  "files_touched": ["…"],
  "produced_signatures": ["…"],
  "blockers": [],
  "assumptions": [],
  "scope_deviations": [],
  "handoff": {},
  "tests_created": ["…"],
  "tests_modified": ["…"],
  "behavior_anchors": ["test:…"],
  "test_cases": [
    {
      "anchor": "test:…",
      "file": "…",
      "identity": "…",
      "lane": "host"
    }
  ],
  "test_capabilities_added": ["flow"],
  "developer_runs": [],
  "fail_before_claim": null,
  "additional_impact_found": [],
  "test_not_applicable": null
}
```

## Pins (fail-closed)

- Required keys: `schemaVersion, agent, status, files_touched,
  produced_signatures, blockers, assumptions, scope_deviations, handoff, tests_created,
  tests_modified, behavior_anchors, test_cases, test_capabilities_added,
  developer_runs, fail_before_claim, additional_impact_found,
  test_not_applicable`.
- `schemaVersion` is exactly `1`; any other value (or its absence) rejects the
  report.
- `status` ∈ `done|blocked|failed|skipped`.
- `status: blocked|failed` requires non-empty `blockers`.
- `status: skipped` is allowed only for a builder the planner assigned no work
  — it is NEVER a task-level test N/A (`test_not_applicable` belongs to the
  planner's typed impact, and a builder echoing one here does not certify it).
- Every `test_cases[].anchor` appears in `behavior_anchors`; `lane` is a
  machine-policy lane id.
- `test_cases[].identity` is the fully-qualified test identity the runner will
  discover — the evidence gate cross-checks it against executor receipts.
- `fail_before_claim` is `null` or `{ "testIdentity": "…", "reason": "…" }`
  claiming the red run's expected assertion; the certifying red/green pair is
  proven by executor receipts on the immutable baseline, never by this claim.
- `additional_impact_found` lists behaviors/consumers discovered during the
  build that the planned impact missed — the observed impact must widen with
  them (a builder never deletes a required suite from impact).
- `developer_runs` are feedback-only local commands; they certify nothing.
- `scope_deviations` records owner-approved exceptions (Step 4 "necessary
  footprint expansion") — every entry is also surfaced in the final
  `### Caveats`.
- A builder writes only files assigned in its planner section; out-of-scope
  writes are a `scope-leak-validator` finding, not a silent `files_touched`
  entry.
