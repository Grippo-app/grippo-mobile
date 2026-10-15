# Change attribution

This reference owns the task baseline and the distinction between initial work,
the task's edits and concurrent changes. A file's dirty status does not identify
who changed it or whether the change belongs to this task.

## Establish a baseline before editing

The main session keeps the baseline in the session scratchpad, outside the
repository. It is temporary working material, not a durable record or a new
repository report.

1. Record `git rev-parse HEAD` and `git status --porcelain=v1 --untracked-files=all`.
   Preserve both status columns: staged and unstaged changes are distinct;
   untracked paths must also be visible.
2. Before the first write to each path, save its current working-tree content
   and its staged content, if present, with their path/type/mode. Record absence
   explicitly; an absent file and an empty file are different. Include existing
   untracked files and staged additions deleted from the working tree.
3. Keep the user's index unchanged during implementation. Do not stage, unstage,
   stash, reset or check out files to make a clean baseline. Requested staging
   or commits follow [delivery](../../delivery/SKILL.md).

Capture content only for paths the task will edit; extend the baseline before
touching another path. A status listing or `git diff --name-only` alone is not a
content baseline. Existing work may be a prerequisite for the task; initial does
not mean unrelated. No initial clean build is required for attribution.

## Track the task's delta

Compare final content with the saved pre-edit content to identify the task's
actual additions, changes and removals. Compare the index with its saved state
as well. `git diff` against HEAD includes initial work and cannot substitute for
this comparison in a pre-dirty file.

Agent briefs and review handoffs carry the baseline's absolute scratchpad path,
initial changes relevant to the slice, owned paths and the task's delta so far.
For a file with initial changes, reviewers inspect both the task-only delta and
the resulting source in context. The final reply names the task's changes and
relevant initial work without taking credit for the latter. No new repository
inventory or dated record is created for this purpose.

## Concurrent writes

Before writing, compare the file with the last version the writer read or wrote.
An unexpected change may belong to the user or another session, including in a
file that was initially clean. Stop writes to that overlapping file and keep the
baseline, the task's last version and the newly observed version in the scratchpad.
Do not restore an older whole-file copy over the new content.

Coordinate ownership before continuing. Merge a clearly separable change while
preserving both contributions; ask the user only when conflicting intent or
inseparable edits leave a real ambiguity. Continue work on unaffected paths.

## Attribute failures from evidence

A build error's path identifies where it surfaced, not who caused it. An unchanged
caller can fail because the task changed its contract; a pre-dirty file can fail
because of the task's new edit. Trace relevant declarations and callers against
the saved content before assigning origin.

Call a failure pre-existing only with evidence that it was already present before
the task's edits. State that evidence. When origin cannot be established, report
the observed failure as origin unknown and state what was checked. A failed check
remains failed regardless of origin. Do not revert user work or run a speculative
clean baseline build to make the classification.

## Completion gate

- Every edited path has pre-edit content or an explicit absence marker.
- Task changes are distinguished from initial and concurrent work by content.
- The task did not change the user's index unless staging or a commit was requested.
- Reviewers received the baseline and task delta; failure attribution names its
  evidence or remains unknown.
