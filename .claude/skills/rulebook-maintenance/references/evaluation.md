# Evaluating the rulebook

Run `python3 -I .claude/hooks/check-rulebook.py` for structure and
`python3 -I .claude/hooks/check-rulebook-tests.py` for isolated positive and
negative mutations. The fixtures copy rulebook files into a temporary directory;
application paths are source links for resolving references. Fixture mutations
stay within copied rulebook files; source links are not a filesystem sandbox.
The suite rejects linked native containers before copying and stops if its initial
baseline is invalid. No application checks run.

For routing, loading or authority changes, use the bounded cases in
`../evals/cases.json`. Give an independent read-only agent only the `prompt`
and case ID, the current rules and the response schema; withhold the rubric.
Ask it to select owners, required references, gates, delegation and permitted
checks, without implementing, building, running the app or using admin access.
Preserve the raw response. Normalize equivalent natural-language decisions to
the rubric identifiers in a separate scratchpad JSON, recording each mapping;
never add a decision the response did not make. Treat ambiguous answers as a
review question, not a pass. Manually review reference selection, role suitability
and declared loaded files
against the trace; the script type-checks those fields but does not prove their
semantic correctness. Grade the normalized skill/gate/action decisions with
`python3 -I .claude/skills/rulebook-maintenance/evals/evaluate.py <responses.json>`
from the repository root. By default, responses are required for the full catalog.
For affected cases only, repeat `--case CASE_ID`, for example
`python3 -I .claude/skills/rulebook-maintenance/evals/evaluate.py <responses.json> --case rules-only --case proportional-scout`.
The grader validates the full catalog before selecting cases, rejects unknown or
duplicate selections and responses outside the selection, and reports selected
and total case counts. The catalog uses schema 1, unique nonempty case IDs,
nonempty prompts and string lists for required skills/gates and forbidden actions.
The grader rejects malformed catalogs and responses before scoring. A static
metadata check does not establish model routing. Compare before and after on the
same selected cases, identifying each run's case IDs and rule revision. A targeted
run supports fresh results only for its selected cases. Omit unrun cases or cite
their actual earlier responses and revision as retained evidence. Keep responses
and traces in the task scratchpad, not the repository.

Review failures semantically: required gates and prohibited side effects decide
correctness, not exact wording or a single prescribed sequence. Measure the
UTF-8 bytes of the union of actually loaded documents (count each file once),
and report that unit and the measurement stage explicitly. Routing-stage selected
files measure only routing, not all implementation/review loads. Bytes and lines
are not token measurements.
Native discovery, role registration and enforced tool restrictions are separate
checks. Report which were observed; explicit role use is not native registration.
