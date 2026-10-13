# Contract: builder-report

Normalized envelope a builder returns to the orchestrator. The installed reference
has one current protocol: `schemaVersion: 1`. Missing or unknown fields and ambiguous
nested shapes are rejected by `builder-report.schema.json`. A configured external
orchestrator must use the same current schema before consuming these reports.

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
  "developer_runs": [],
  "additional_impact_found": []
}
```

## Pins

- Required keys match the schema above exactly.
- `status: blocked|failed` requires non-empty `blockers`.
- `status: skipped` applies only to a builder the planner assigned no work.
- `additional_impact_found` lists behaviors/consumers discovered during work that
  the plan missed; review them without silently extending authorized scope.
- `developer_runs` records only actual source/static/manual checks and permitted
  builds. It is a claim, not proof that behavior was observed; retain the evidence.
- `scope_deviations` records authorized exceptions and appears in final caveats.
- Builders write only assigned files. Out-of-scope writes are scope findings.
- Follow CLAUDE.md verification policy; this envelope carries no automated execution
  obligations or authority to create infrastructure.
