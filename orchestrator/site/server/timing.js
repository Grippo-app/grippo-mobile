'use strict';

// ---------------------------------------------------------------------------
// Task-duration model (Goal 6 — done cards show execution time, with NO
// playbook change). Dependency-free CommonJS so server.js can require it and
// a later server split can move this file unchanged.
//
// The timing map lives in the PERSISTED state file (orchestrator/.cache/site/
// .site-state.json) under `taskTiming`, keyed by task stem:
//
//   taskTiming[stem] = { startedAt, doneAt?, durationMs? }   // ISO strings + ms
//
// reconcile() is a PURE function the SSE poll calls each tick with the prev
// map, the live locks, and INDEX `done[]`. It only ever ADDS information:
//   - First time a lock is observed for a stem with a parseable startedAt and
//     no recorded startedAt yet, stamp startedAt. Locks hold startedAt constant
//     while they live, so the first observation is authoritative; we never
//     overwrite a recorded startedAt (a re-run keeps the original start).
//   - When a stem appears in done[] with a parseable doneAt AND we have a
//     recorded startedAt AND no durationMs yet, compute durationMs once.
//
// A task that finished BEFORE this existed has no recorded startedAt, so it
// gets no duration (the card omits it gracefully). durationMs is only stored
// when finite and >= 0 — a clock-skewed doneAt < startedAt is dropped, never
// rendered as a negative/NaN value.
// ---------------------------------------------------------------------------

function parseMs(iso) {
  if (typeof iso !== 'string' || !iso) return NaN;
  return Date.parse(iso);
}

// prevMap:    { stem: { startedAt, doneAt?, durationMs? } }
// locks:      [{ stem, stage, startedAt }]   (server.js readLocks output)
// done:       [{ stem, doneAt }]             (INDEX.json done[] entries)
// boardStems: [stem]                         (INDEX.json stems, all columns)
// Returns { map, changed }. `map` is a fresh object; callers persist it only
// when `changed` is true. Entries whose stem is no longer on the board (and not
// currently locked) are pruned so the map can't grow without bound.
function reconcile(prevMap, locks, done, boardStems) {
  // Null-prototype so a hand-edited "__proto__" key in the on-disk map can't
  // reach Object.prototype through this working object.
  var map = Object.create(null);
  var stem, rec;
  var i;

  // Stems still relevant this tick: on the board OR actively locked. An entry
  // for any other stem is orphaned (task left the board) and gets dropped.
  var keep = Object.create(null);
  for (i = 0; i < (boardStems || []).length; i++) {
    if (typeof boardStems[i] === 'string') keep[boardStems[i]] = true;
  }
  for (i = 0; i < (locks || []).length; i++) {
    if (locks[i] && typeof locks[i].stem === 'string') keep[locks[i].stem] = true;
  }

  var changed = false;

  // Shallow-clone surviving entries so the result is independent of prevMap.
  if (prevMap && typeof prevMap === 'object') {
    for (stem in prevMap) {
      if (!Object.prototype.hasOwnProperty.call(prevMap, stem)) continue;
      if (!keep[stem]) { changed = true; continue; }   // orphan — prune
      rec = prevMap[stem];
      if (rec && typeof rec === 'object') {
        map[stem] = {
          startedAt: typeof rec.startedAt === 'string' ? rec.startedAt : null,
          doneAt: typeof rec.doneAt === 'string' ? rec.doneAt : null,
          durationMs: typeof rec.durationMs === 'number' && isFinite(rec.durationMs) ? rec.durationMs : null
        };
      }
    }
  }

  for (i = 0; i < (locks || []).length; i++) {
    var lock = locks[i] || {};
    stem = lock.stem;
    if (!stem || !lock.startedAt || isNaN(parseMs(lock.startedAt))) continue;
    if (!map[stem]) {
      map[stem] = { startedAt: lock.startedAt, doneAt: null, durationMs: null };
      changed = true;
    } else if (!map[stem].startedAt) {
      map[stem].startedAt = lock.startedAt;
      changed = true;
    }
  }

  for (i = 0; i < (done || []).length; i++) {
    var d = done[i] || {};
    stem = d.stem;
    if (!stem) continue;
    rec = map[stem];
    if (!rec || !rec.startedAt) continue;        // never saw it run — omit
    if (rec.durationMs != null) continue;         // already computed
    var startMs = parseMs(rec.startedAt);
    var doneMs = parseMs(d.doneAt);
    if (isNaN(startMs) || isNaN(doneMs)) continue;
    var dur = doneMs - startMs;
    if (!isFinite(dur) || dur < 0) continue;      // clock skew — drop, never NaN/negative
    rec.doneAt = d.doneAt;
    rec.durationMs = dur;
    changed = true;
  }

  return { map: map, changed: changed };
}

// ---------------------------------------------------------------------------
// Lifecycle spine — Task Details → Activity's column bands (backlog → pending → todo →
// done). A PURE function the SSE poll calls each tick, sibling to reconcile()
// above. Like reconcile() it only ever ADDS: the first time a stem is seen it
// stamps its current column; thereafter a column CHANGE (current != the last
// recorded column) appends a transition entry. The map persists in
// orchestrator/.cache/site/.site-state.json under `taskLifecycle`, keyed by stem:
//
//   taskLifecycle[stem] = [ { column, enteredAt, source } ]   // ordered, append-only
//
// Manual `git mv` moves are caught here exactly like agent-driven ones: the
// server observes INDEX.json (the single source of truth for which column a
// task is in), so a hand-move that no agent logged still lands on the spine —
// with source 'observed' and a poll-granular enteredAt (POLL_MS, ~1.5s). The
// precise per-phase detail (and exact agent-driven transition times) live in
// the per-task journal JSONL the read side fetches separately; this spine only
// needs the column boundaries + their durations, which a poll sample provides.
//
// Entries whose stem has left the board (not in any INDEX column and not
// currently locked) are pruned, mirroring reconcile()'s keep-set so the map
// can't grow without bound. A done task stays on the board (INDEX done[]), so
// its spine is never pruned while the card is visible.
//
//   prevMap:  { stem: [ { column, enteredAt, source } ] }
//   columns:  { backlog:[{stem,..}], pending:[..], todo:[..], done:[..] }  (state.readIndexSnapshot)
//   locks:    [{ stem, stage, startedAt }]
//   now:      ISO8601 string — the poll tick time, used for newly-observed entries
// Returns { map, changed }; callers persist the map only when changed is true.
function reconcileLifecycle(prevMap, columns, locks, now) {
  // Null-prototype so a hand-edited "__proto__" key on disk can't reach
  // Object.prototype through this working object (same guard as reconcile()).
  var map = Object.create(null);
  var changed = false;
  var i, stem;

  // Current column per stem. A task is in exactly one column; if INDEX is
  // momentarily inconsistent across a regen, the last column wins and the next
  // tick self-heals.
  var colOf = Object.create(null);
  var order = ['backlog', 'pending', 'todo', 'done'];
  for (var c = 0; c < order.length; c++) {
    var arr = columns && Array.isArray(columns[order[c]]) ? columns[order[c]] : [];
    for (i = 0; i < arr.length; i++) {
      var s = arr[i] && arr[i].stem;
      if (typeof s === 'string' && s) colOf[s] = order[c];
    }
  }

  // keep = on the board OR actively locked (same prune rule as reconcile()).
  var keep = Object.create(null);
  for (stem in colOf) keep[stem] = true;
  for (i = 0; i < (locks || []).length; i++) {
    if (locks[i] && typeof locks[i].stem === 'string') keep[locks[i].stem] = true;
  }

  // Carry surviving prev entries forward (clone each transition array so the
  // result is independent of prevMap). Orphans (stem left the board) are dropped.
  if (prevMap && typeof prevMap === 'object') {
    for (stem in prevMap) {
      if (!Object.prototype.hasOwnProperty.call(prevMap, stem)) continue;
      if (!keep[stem]) { changed = true; continue; }   // left the board — prune
      var prev = prevMap[stem];
      if (!Array.isArray(prev)) continue;
      var copy = [];
      for (i = 0; i < prev.length; i++) {
        var t = prev[i];
        if (t && typeof t === 'object' && typeof t.column === 'string') {
          copy.push({
            column: t.column,
            enteredAt: typeof t.enteredAt === 'string' ? t.enteredAt : null,
            source: t.source === 'agent' ? 'agent' : 'observed'
          });
        }
      }
      if (copy.length) map[stem] = copy;
    }
  }

  // First-seen → stamp current column; column change → append a transition.
  for (stem in colOf) {
    var col = colOf[stem];
    var rec = map[stem];
    if (!rec) {
      map[stem] = [{ column: col, enteredAt: now, source: 'observed' }];
      changed = true;
    } else {
      var last = rec[rec.length - 1];
      if (!last || last.column !== col) {
        rec.push({ column: col, enteredAt: now, source: 'observed' });
        changed = true;
      }
    }
  }

  return { map: map, changed: changed };
}

module.exports = {
  reconcile: reconcile,
  reconcileLifecycle: reconcileLifecycle
};
