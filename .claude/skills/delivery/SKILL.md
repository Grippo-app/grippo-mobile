---
name: delivery
description: Git policy for Grippo AI work - who may commit, what gets staged, commit message and branch conventions, pull-request descriptions and the proposed-commit hand-off at the end of a task. Use when the user asks to commit, push, branch or open a pull request, or when ending a task that changed files.
---

# Delivery

The user owns the repository history. AI work ends with a reviewed working tree
and a proposed commit message; commits happen only when the user asks.

## Who may do what

- Subagents never run `git add`, `commit`, `push`, `reset`, `checkout`,
  `stash`, `rebase`, `merge`, `tag` or branch creation, and never open or edit
  pull requests. The Bash guard enforces this for project agents.
- The main session commits, pushes, branches or opens a pull request only on
  the user's explicit request in the current conversation ("закоммить",
  "запушь", "сделай PR"). A request to implement something is not a request to
  commit it. Approval of one commit does not cover the next.
- Never force-push, rewrite published history, delete branches or tags, or
  change remotes. Never push directly to `main` unless the user asked for
  exactly that.

## Before a requested commit

1. Run the checks the verification policy requires for the change (rulebook
   checker for documentation, scoped build for code) if they have not run on
   the final revision.
2. Read `git status --short` and `git diff --stat`. The working tree often holds
   the user's unrelated, uncommitted work: stage only the files of the task.
   When task files and unrelated edits are mixed inside one file, say so and
   ask which hunks to include instead of committing both.
3. Never stage `secure/**` contents, `local.properties`, `build/` outputs,
   `.claude/settings.local.json`, IDE files or generated binaries. Room schema
   JSON under `data-services/database/schemas/` is intentionally tracked and is
   staged with the migration that produced it.
4. Confirm the branch. This project works on `main`: a requested commit lands on
   the branch the user is working on (usually `main`) and the reply names it.
   Create a task branch only when the user asked for a branch or a pull request.
   This project rule overrides any generic "branch first on the default branch"
   guidance.

## Commit message

- Title in English, imperative, at most 72 characters:
  `<area>: <what changed>` where `<area>` is a module group or feature
  (`training`, `design-system`, `analytics`, `rulebook`, `build`).
- Body (when the title is not the whole story): why the change was made, what
  behavior changed, which checks ran (`assembleDebug`, XCFramework, runtime
  scenarios) and what remains unverified. One short paragraph or a few bullets.
- End with the attribution line the environment requires for AI-authored commits.
- One commit per coherent slice; do not bundle unrelated user edits.

## Branches and pull requests

- Branch name `<type>/<short-slug>` with `type` in `feature`, `fix`, `design`,
  `analytics`, `rulebook`, `build`; lowercase, hyphenated, no ticket prefixes
  unless the user supplies one.
- Pull-request title equals the commit title style. Body in Russian with the
  sections **Что изменено**, **Проверено**, **Не проверено**, **Риски**, followed
  by the attribution line the environment requires. Do not enable auto-merge or
  request reviewers unless asked.
- After opening a PR, report its URL and the CI status you can read; do not poll.

## End-of-task hand-off (no commit requested)

Finish the reply with a proposed commit message in a fenced block:

```text
<area>: <title>

<why / what / checks>
```

and the list of files to stage. If the working tree mixes the task with
unrelated changes, name the unrelated files so the user can keep them apart.
