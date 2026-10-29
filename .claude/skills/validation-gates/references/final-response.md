# Final response format

This format applies to the main session's reply to the user. Subagents report in
the format their definition prescribes; tables belong in record files, not in the
reply. A verifier's evidence table (scenario / expected / observed / screenshot)
is summarized as a list in "Проверено", with the screenshot paths named; the
table itself stays in the agent's report in the session transcript.

Unless the user asks for another format, a completion reply is concise but
informative, in plain Russian, structured as lists under bold section labels.
No tables. Each meaningful item gets enough detail to understand what changed and
why: usually one or two short sentences, with concrete counts or a before/after
example when useful. No vague claims such as "fixed everything". Status emojis
only: ✅ completed/verified, ⚠️ partial, deviation or unverified check, ❌ blocker
or requested work left undone - on the overall status line and status items, not
on section headings; location lists and question options carry no emoji.

Start with one overall status line: ✅ **Задача завершена**,
⚠️ **Завершена частично — <missing requested outcome>**, or
❌ **Задача заблокирована — <specific blocker>**. Choose it by requested outcomes,
not by effort. Blocked means every useful remaining step depends on an external
condition or a user decision, even if part of the work is ready. Partial means
some requested outcomes are ready and useful work can still proceed. An unrun
runtime check alone never makes a task partial; it goes under "Не проверено".
Directly below the status, state any limitation that materially affects using the
result or a decision the user must make; do not repeat it in the sections. This
format never authorizes stopping an actionable task early, and a scope reduction
needs the user's agreement before the reduced task is called complete.

Sections, in this order, omitting empty ones:

- **Сделано** - user-visible results and their purpose, not a file-edit inventory.
  For internal/rule tasks, the resulting guarantee or convention.
- **Где изменил** - affected layers (UI, navigation, data, analytics, project
  rules, ...) and what changed there; link key files; group repetitive edits. A
  short key-file tree only for new or changed structure or on request.
- **Что мешает продолжить** - for blocked work: the missing fact, access or
  dependency, the outcome it prevents and what would unblock it.
- **Проверено** - actual checks, their scope and outcomes; compilation is not
  runtime proof.
- **Отклонения от правил проекта** - actual deviations, the reason, and whether
  the user agreed.
- **Не сделано** - requested outcomes still missing, with the reason if not
  already stated above.
- **Не проверено** - relevant unrun checks and what remains uncertain, separate
  from undone work; only material limitations.
- **Что проверить вручную** - actionable scenarios only when they help the user.
- **Вопросы** - still-unresolved questions, numbered, each with 2-3 options
  labelled (А), (Б), (В); the recommended option first with a brief reason. Ask
  during the work when the answer is needed; this section holds only what is
  still open.
- **Найдено попутно** - unrelated defects discovered during the task, with
  `path:line` and the trigger, left unfixed.

Do not invent questions, repeat answered decisions, include empty success claims
or end with a paragraph repeating the lists. Scale detail to the task: a small fix
may need only "Сделано" and "Проверено"; material limitations survive even in a
short reply. Delivery ends the reply with the proposed commit message from
[delivery](../../delivery/SKILL.md) when files changed and no commit was requested.

See [response examples](response-examples.md) for completed, partial
and blocked tasks; they guide tone and detail, never supply claims.
