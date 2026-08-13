# orchestrator/template-sync — vendored-copy integrity sensor

Products may keep `orchestrator/**` as a vendored copy. Updates are always
explicit: this sidecar reports drift and can apply reviewed template changes,
but never changes a product automatically.

## The three tools

| Tool | What it does | Writes |
|---|---|---|
| `_generate_template_manifest.py` | Hashes every template-owned file under `orchestrator/**` (sha256) into the committed `orchestrator/template-manifest.json`, together with a stamp (`stampCommit` = HEAD of the stamped tree, `-dirty`-suffixed when it differs; the template's commit after a sync `--apply`, the product's own at bootstrap Step 14) and the exclusion rules it was built with | the manifest (or stdout with `--print`) |
| `check-integrity.mjs` | Read-only report inside a product: `modified` / `missing` / `extra` files vs the shipped manifest + stamp age; with `TEMPLATE_ROOT=<path>` also a live diff against the current template | nothing |
| `sync-from-template.sh` | Manual sync, **dry-run by default**: full diff preview, `--apply` copies changed/missing template-owned files and re-stamps; never deletes product files, never touches git | product files + re-stamp (only with `--apply`) |

## Lifecycle rules (load-bearing)

1. **Stamp only at bootstrap and at explicit sync.** launch.md Step 14 stamps the
   fresh copy right after `install-skills.sh`; `sync-from-template.sh --apply`
   re-stamps after copying. **Never** add this generator to a pre-commit hook or
   a daemon — an auto-regen re-baselines local drift as "clean" on every commit
   and the sensor can never report rot again.
2. **The exclusion set is the correctness surface.** It lives ONLY in
   `_generate_template_manifest.py` (`EXCLUDES`); the manifest embeds it and the
   CHECKER reads it back from the artifact, while the sync tool re-derives both
   sides with the same generator — no second copy of the rules anywhere. It must skip everything per-product (`project-config.md`, the four
   task columns + evidence + INDEX.json, `.cache/`, generated
   `figma/project-adapters.json`, populated api-contract snapshots, secrets,
   `.arch-map.json`) or a pristine copy reports red on day
   one and the sensor gets ignored. Keep it in lockstep with the root
   `.gitignore` + launch.md Step 2.5.
3. **An absent manifest is an explicit `unstamped copy` verdict** — the checker
   never guesses and has no fallback.
4. **`TEMPLATE_ROOT` is an env var, not a config key** — a machine-local
   absolute path never belongs in a committed `project-config.md`.
5. **The template repo itself carries NO manifest** (its root `.gitignore`
   blocks an accidental stamp). The template is not a copy; a stale stamp
   inherited by a fresh bootstrap would false-red every new product on day one.
   Inside the template the checker honestly reports `unstamped copy` — expected.
6. **After `--apply` the product manifest is the TEMPLATE-side map**, not a
   re-hash of the product tree — re-hashing would grandfather the product's
   `extra` files into the baseline (masking them forever) and record the
   PRODUCT's own commit as the stamp provenance (actively misleading — the
   reader would hunt for that sha in the template repo).
7. **Unsafe filesystem entries never become invisible.** The generator refuses
   template-owned symlinks and special files instead of stamping around them;
   the read-only checker reports them as modified/extra without following or
   reading through them. Excluded per-product/runtime trees remain excluded.

## Typical flows

```bash
# In a product: how healthy is my copy?
node orchestrator/template-sync/check-integrity.mjs
TEMPLATE_ROOT=<path-to-template-checkout> \
  node orchestrator/template-sync/check-integrity.mjs   # + live template diff

# Pull the template's current state into a product:
bash orchestrator/template-sync/sync-from-template.sh <path-to-template-checkout>            # dry-run
bash orchestrator/template-sync/sync-from-template.sh <path-to-template-checkout> --apply

# Run the source template's script and name the product root explicitly when
# the destination copy does not contain template-sync/:
bash <template>/orchestrator/template-sync/sync-from-template.sh <template> <product-root>
```

Exit codes (`check-integrity.mjs`): `0` clean · `1` drift or unstamped · `2` error.

## Harvest-back (product-side hotfixes → template)

A gate-script fix hand-applied inside a product lives only in that copy; a
one-way `--apply` would overwrite it. The flow that keeps fixes alive:

1. `sync-from-template.sh` (dry-run) marks every to-copy file that ALSO diverged from the
   product's own stamped manifest as **`COPY!` — a locally-modified hotfix candidate** — and
   prints its full `diff -u` (product vs template). A product without a stamp gives no
   hotfix signal: stamp early, stamp always.
2. For each `COPY!` file: port the product-side fix INTO THE TEMPLATE first (with its
   regression test — the template's `figma:verify` must stay green), commit there.
3. Re-run the sync. A byte-identical port makes the line disappear entirely (both sides now
   agree). A NON-byte-identical port (the template absorbed the fix in improved form) keeps
   the `!` — the mark tracks product-vs-STAMP divergence, not template parity, so it cannot
   "drop to plain COPY" until the next `--apply` re-stamps. That is expected: the step-2
   review is what makes the apply safe, not the disappearance of the mark.
4. Never `--apply` while a `COPY!` line has not been through step 2 — that is the
   silent-revert path. Once every hot file's fix is confirmed ported (or deliberately
   rejected) template-ward, `--apply` with the `!` marks still showing is safe and re-stamps.

Rule of thumb for agents/operators: a gate-script edit made in a product mid-task is
mirrored into the template THE SAME DAY (the orchestrator's escalation guidance says the
same) — an unmirrored hotfix re-opens the identical failure in every other copy and in the
next bootstrap.

No npm scripts, no package.json, no dependencies — the tools are invoked
directly (stdlib python3 + zero-dep node), which keeps them out of the
`figma:*`/`contract:*` alias lint and the figma script-manifest pinning net
(this is deliberately NOT a figma-gate script).
