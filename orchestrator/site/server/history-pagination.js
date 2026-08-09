'use strict';

// Stable keyset pagination for newest-first history indexes. History writers
// may insert or update rows at the head while a user is paging; an offset
// cursor would then duplicate one boundary row and skip another.

var CURSOR_MAX = 512;
var DATE_LIMIT = 8640000000000000;

function rowKey(row, idField) {
  return {
    time: Date.parse(row.startedAt),
    id: String(row[idField] || '')
  };
}
function compareKeys(left, right) {
  if (left.time !== right.time) return left.time > right.time ? -1 : 1;
  if (left.id === right.id) return 0;
  return left.id > right.id ? -1 : 1;
}
function encodeCursor(key) {
  return Buffer.from(JSON.stringify({ version: 1, time: key.time, id: key.id })).toString('base64url');
}
function decodeCursor(cursor, idPattern) {
  if (typeof cursor !== 'string' || !cursor || cursor.length > CURSOR_MAX ||
      !/^[A-Za-z0-9_-]+$/.test(cursor)) return null;
  try {
    var bytes = Buffer.from(cursor, 'base64url');
    if (bytes.toString('base64url') !== cursor) return null;
    var parsed = JSON.parse(bytes.toString('utf8'));
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed) ||
        Object.keys(parsed).sort().join('\0') !== ['id', 'time', 'version'].join('\0') ||
        parsed.version !== 1 || !Number.isSafeInteger(parsed.time) ||
        parsed.time < -DATE_LIMIT || parsed.time > DATE_LIMIT ||
        !idPattern.test(String(parsed.id || ''))) return null;
    return { time: parsed.time, id: parsed.id };
  } catch (error) { return null; }
}
function page(rows, cursor, limit, idField, idPattern) {
  var ordered = rows.slice().sort(function (left, right) {
    return compareKeys(rowKey(left, idField), rowKey(right, idField));
  });
  var anchor = cursor ? decodeCursor(cursor, idPattern) : null;
  if (cursor && !anchor) return { ok: false, error: 'bad-cursor' };
  var remaining = anchor ? ordered.filter(function (row) {
    return compareKeys(rowKey(row, idField), anchor) > 0;
  }) : ordered;
  var pageRows = remaining.slice(0, limit);
  return {
    ok: true,
    rows: pageRows,
    nextCursor: pageRows.length && pageRows.length < remaining.length
      ? encodeCursor(rowKey(pageRows[pageRows.length - 1], idField)) : null
  };
}

module.exports = { page: page };
