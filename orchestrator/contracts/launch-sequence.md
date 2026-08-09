# Contract: launch-sequence

The from-scratch bootstrap order. The factory's `launch.md` runs Step 0–14 plus load-bearing half-steps; freezing the sequence guards the `gold-launch-pipeline` parity.

Source: `orchestrator/launch.md`. Compare this inventory against the live launch sequence whenever its headings change.

## Frozen step inventory (in order)
`Step 0, 1, 1.5, 2, 2.5, 3, 4, 4.5, 5, 6, 6.5, 6.6, 7, 7.5, 7.6, 7.7, 8, 9, 10, 11, 12, 13, 14`.

Half-steps are normative, not optional: 2.5 (`.gitignore`), 4.5 (error contracts), 6.5 (Figma sidecar), 6.6 (backend snapshot), 7.5 (dialog-api), 7.6 (error-provider-impl), 7.7 (error-display). Treat any removed/renamed heading as drift against the live `launch.md` sequence.

App shells are produced by Steps 3/11 (NOT by `app-shell-builder`); the data-service scaffold is Step 7's manual equivalent of `data-service-scaffold-builder`; skills install at Step 14.
