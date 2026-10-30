# Scope decisions and questions

[CLAUDE.md](../../../../CLAUDE.md#definitions) defines authorized scope and scope
expansion; [scope discipline](../../../../CLAUDE.md#scope-discipline) lists the
sensitive boundaries (shared foundation, public routes, Room schemas, dependencies
and conventions, app shells, network/auth core, tokens, CompositionLocals,
`DialogConfig`). Check them before changing any of those.

## When to ask

Ask when a change expands the authorized scope, revises a locked decision without
existing explicit approval, or needs a behavior-affecting fact that neither the
request nor the source provides (a product rule, a backend contract, a missing
permission). Everything necessary
to deliver the request - callers, routes, DI registration, resources, coherent
migrations - is already authorized and is not asked about again.

A visual complaint that the source cannot map to one owner (for example "double
frame" where the code draws one) needs a render when available and authorized.
If that evidence is unavailable, state the limit and ask which element and
treatment the user means; do not guess or block clarification on a missing render.

## How to ask

- One short, self-contained question per decision, in the user's language, with
  2-3 alternatives; the recommended option first with its reason and trade-off.
  When no option is defensible, say exactly which fact is missing instead of
  recommending one.
- Batch related questions into one quiz; ask when the decision is needed, not
  only at the end. The user may ask for more or fewer questions; honor that.
- Continue every independent slice while waiting. Never implement the dependent
  slice on an assumed answer; never treat silence, time passing or a preselected
  option as an answer; never ask an answered question again.
- External messages (Slack, email, issue trackers) need explicit authorization;
  preparing a draft does not send it.
