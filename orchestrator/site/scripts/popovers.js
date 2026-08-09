// ----------------------------------------------------------------------
// Header status-popover coordinator. The header carries five independent
// pills (status / cli / figma / backend / sessions), each owning its own open-state
// and outside-click handler. Each pill's click calls stopPropagation(), so
// the OTHER pills' document-level outside-click listeners never fire — which
// let every popover stay open at once. This is the single source of "only one
// open at a time": each popover hands its own close-fn to open() when it opens
// (closing whichever was open before) and clears it via close() when it shuts.
// ----------------------------------------------------------------------

var current = null;   // close-fn of the currently-open popover, or null

// Mark `closeSelf`'s popover as the open one, closing the previously-open one.
// Calling the prior close-fn re-enters here via that popover's own close()
// (which only nulls `current` when it still points at itself) — so no recursion
// and no clobbering of the just-opened popover.
function open(closeSelf) {
  if (current && current !== closeSelf) current();
  current = closeSelf;
}

// A popover announcing it has closed. Guarded so a stale close (one popover
// closing after another already became current) can't wipe the live pointer.
function close(closeSelf) {
  if (current === closeSelf) current = null;
}

export const popovers = { open: open, close: close };
