# validation-gates — trigger fixtures

Realistic prompts that SHOULD activate the `validation-gates` skill (validators + reviewer gate).

- validate this task before it merges
- run the gates on the produced change set
- did this break an anti-pattern?
- trace the acceptance criteria against what was built
- is scope leaking outside the task footprint?
- check the reviewer-only gaps no validator catches mechanically
