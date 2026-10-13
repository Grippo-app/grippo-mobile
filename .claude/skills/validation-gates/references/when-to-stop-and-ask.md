# Scope decisions and questions

[CLAUDE.md](../../../../CLAUDE.md#when-to-stop-and-ask) owns the list of sensitive
changes and locked decisions. Check it before changing shared foundation,
public routes, modules, schemas, dependencies, shells, network/auth or token APIs.
Do not duplicate a conflicting allowed/forbidden list here.

Existing authorization covers required steps of the user's requested work,
including coherent caller migrations. Ask only when a change expands that scope,
revises a locked decision or needs a behavior-affecting fact unavailable in source.
Missing permissions and backend/product decisions cannot be guessed.

Ask in this chat with one short self-contained question and 2–3 alternatives.
Mark an evidence-supported recommendation and explain its tradeoff. If no default
is defensible, explicitly state which fact is missing. Honor the requested quiz
frequency, avoid repeated confirmation and continue independent work while waiting.
Do not treat silence or a preselected option as an answer. External Slack/email/
issue messages require explicit authorization; preparing a proposal does not send it.
