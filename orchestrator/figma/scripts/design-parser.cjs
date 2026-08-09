'use strict';

// Shared, zero-dependency parser for task `## Design` bullets. Used by the
// site (CommonJS, synchronous state path) and by figma tooling scripts. It never
// calls Figma; it only normalizes local task markdown.

var crypto = require('crypto');

var FIGMA_HOST_RE = /(^|\.)figma\.com$/i;

var HTML_BLOCK_TAGS = '(?:address|article|aside|base|basefont|blockquote|body|caption|center|col|colgroup|dd|details|dialog|dir|div|dl|dt|fieldset|figcaption|figure|footer|form|frame|frameset|h[1-6]|head|header|hr|html|iframe|legend|li|link|main|menu|menuitem|nav|noframes|ol|optgroup|option|p|param|search|section|summary|table|tbody|td|tfoot|th|thead|title|tr|track|ul)';
var HTML_TAG_NAME = '[A-Za-z][A-Za-z0-9-]*';
var HTML_ATTR_NAME = '[A-Za-z_:][A-Za-z0-9_.:-]*';
var HTML_ATTR_VALUE = "(?:[^\\s\"'=<>`]+|'[^']*'|\"[^\"]*\")";
var HTML_ATTRIBUTE = '[ \\t]+' + HTML_ATTR_NAME + '(?:[ \\t]*=[ \\t]*' + HTML_ATTR_VALUE + ')?';
var HTML_COMPLETE_OPEN_RE = new RegExp('^ {0,3}<(' + HTML_TAG_NAME + ')(?:' + HTML_ATTRIBUTE + ')*[ \\t]*/?>[ \\t]*$', 'i');
var HTML_COMPLETE_CLOSE_RE = new RegExp('^ {0,3}</' + HTML_TAG_NAME + '[ \\t]*>[ \\t]*$', 'i');
var HTML_TYPE6_RE = new RegExp('^ {0,3}</?' + HTML_BLOCK_TAGS + '(?:[ \\t]|/?>|$)', 'i');

function htmlBlockStart(line, paragraphOpen) {
  if (/^ {0,3}<(?:pre|script|style|textarea)(?:[ \t>]|$)/i.test(line)) return { end: /<\/(?:pre|script|style|textarea)>/i };
  if (/^ {0,3}<!--/.test(line)) return { end: /-->/ };
  if (/^ {0,3}<\?/.test(line)) return { end: /\?>/ };
  // CommonMark HTML block type 4 requires an uppercase ASCII declaration
  // letter. A lowercase `<!foo` is ordinary inline/prose and cannot hide
  // following task structure.
  if (/^ {0,3}<![A-Z]/.test(line)) return { end: />/ };
  if (/^ {0,3}<!\[CDATA\[/.test(line)) return { end: /\]\]>/ };
  if (HTML_TYPE6_RE.test(line)) return { blank: true };
  if (!paragraphOpen) {
    var open = HTML_COMPLETE_OPEN_RE.exec(line);
    if ((open && !/^(?:pre|script|style|textarea)$/i.test(open[1])) || HTML_COMPLETE_CLOSE_RE.test(line)) return { blank: true };
  }
  return null;
}

function masksEntireHtmlBlockLine(block, line) {
  return block.blank ? /^[ \t]*$/.test(line) : block.end.test(line);
}

function escapedPunctuationAt(line, index) {
  var slashes = 0;
  for (var cursor = index - 1; cursor >= 0 && line.charAt(cursor) === '\\'; cursor--) slashes++;
  return slashes % 2 === 1;
}

// CommonMark code spans pair maximal backtick strings of exactly the same
// length. A span may cross physical line endings, but never a block boundary.
// Build the next equal-length run in reverse, then consume pairs in one
// forward pass; unmatched runs remain literal. An escaped run cannot open a
// span, although it can close one (backslash escapes are inert inside code).
function codeSpanIntervalsInRange(source, start, end) {
  var runs = [];
  var cursor = start;
  while (cursor < end) {
    if (source.charAt(cursor) !== '`') {
      cursor++;
      continue;
    }
    var runStart = cursor;
    while (cursor < end && source.charAt(cursor) === '`') cursor++;
    var slashes = 0;
    for (var before = runStart - 1; before >= start && source.charAt(before) === '\\'; before--) slashes++;
    runs.push({ start: runStart, end: cursor, length: cursor - runStart, escaped: slashes % 2 === 1 });
  }

  var nextSame = new Array(runs.length);
  var lastByLength = Object.create(null);
  for (var index = runs.length - 1; index >= 0; index--) {
    var key = String(runs[index].length);
    nextSame[index] = Object.prototype.hasOwnProperty.call(lastByLength, key) ? lastByLength[key] : -1;
    lastByLength[key] = index;
  }

  var intervals = [];
  for (var opener = 0; opener < runs.length;) {
    if (runs[opener].escaped || nextSame[opener] < 0) {
      opener++;
      continue;
    }
    var closer = nextSame[opener];
    intervals.push({ start: runs[opener].start, end: runs[closer].end });
    opener = closer + 1;
  }
  return intervals;
}

function inlineCodeSpanIntervals(source, lines, lineStarts) {
  var intervals = [];
  var fence = null;
  var htmlBlock = null;
  var paragraphOpen = false;
  var paragraphStart = -1;
  var paragraphEnd = -1;

  function appendRange(start, end) {
    if (start < 0 || end <= start) return;
    var found = codeSpanIntervalsInRange(source, start, end);
    for (var index = 0; index < found.length; index++) intervals.push(found[index]);
  }

  function flushParagraph() {
    appendRange(paragraphStart, paragraphEnd);
    paragraphStart = -1;
    paragraphEnd = -1;
  }

  for (var lineIndex = 0; lineIndex < lines.length; lineIndex++) {
    var line = lines[lineIndex];
    var lineStart = lineStarts[lineIndex];
    var lineEnd = lineStart + line.length;

    if (htmlBlock) {
      flushParagraph();
      if (masksEntireHtmlBlockLine(htmlBlock, line)) htmlBlock = null;
      if (/^[ \t]*$/.test(line)) paragraphOpen = false;
      continue;
    }

    var fenceOpen = /^ {0,3}(`{3,}|~{3,})(.*)$/.exec(line);
    if (fenceOpen && fenceOpen[1][0] === '`' && fenceOpen[2].indexOf('`') >= 0) fenceOpen = null;
    if (!fence && fenceOpen) {
      flushParagraph();
      fence = { character: fenceOpen[1][0], length: fenceOpen[1].length };
      paragraphOpen = false;
      continue;
    }
    if (fence && new RegExp('^ {0,3}' + (fence.character === '`' ? '`' : '~') +
      '{' + fence.length + ',}[ \\t]*$').test(line)) {
      flushParagraph();
      fence = null;
      paragraphOpen = false;
      continue;
    }
    if (fence) {
      flushParagraph();
      continue;
    }

    if (!paragraphOpen && (/^ {4}/.test(line) || /^\t/.test(line))) {
      flushParagraph();
      continue;
    }

    var block = htmlBlockStart(line, paragraphOpen);
    if (block) {
      flushParagraph();
      if (!masksEntireHtmlBlockLine(block, line)) htmlBlock = block;
      paragraphOpen = false;
      continue;
    }

    var blank = /^[ \t]*$/.test(line);
    var atx = /^ {0,3}#{1,6}(?:[ \t]+|$)/.test(line);
    var thematic = /^ {0,3}(?:\*[ \t]*){3,}$/.test(line) || /^ {0,3}(?:-[ \t]*){3,}$/.test(line) ||
      /^ {0,3}(?:_[ \t]*){3,}$/.test(line);
    if (blank || atx || thematic) {
      flushParagraph();
      // ATX heading text is inline content too: a comment-looking sequence in
      // a code span must not leak comment state into following sections.
      if (atx) appendRange(lineStart, lineEnd);
      paragraphOpen = false;
      continue;
    }

    if (paragraphStart < 0) paragraphStart = lineStart;
    paragraphEnd = lineEnd;
    paragraphOpen = true;
  }
  flushParagraph();
  return intervals;
}

function codeSpanContains(intervals, state, position) {
  while (state.index < intervals.length && intervals[state.index].end <= position) state.index++;
  return state.index < intervals.length && intervals[state.index].start <= position &&
    position < intervals[state.index].end;
}

function discardCodeSpansStartedBefore(intervals, state, position) {
  while (state.index < intervals.length && intervals[state.index].start < position) state.index++;
}

function maskInlineHtmlComments(line, startsInsideComment, lineStart, codeIntervals, codeState) {
  var chars = line.split('');
  var cursor = 0;
  var open = !!startsInsideComment;
  while (cursor < line.length) {
    if (open) {
      var close = line.indexOf('-->', cursor);
      var end = close < 0 ? line.length : close + 3;
      for (var index = cursor; index < end; index++) chars[index] = ' ';
      cursor = end;
      // Backtick runs consumed as raw comment text cannot later create a code
      // interval that protects another opener after this real comment.
      discardCodeSpansStartedBefore(codeIntervals, codeState, lineStart + end);
      if (close < 0) return { visible: chars.join(''), open: true };
      open = false;
      continue;
    }
    var next = line.indexOf('<!--', cursor);
    if (next < 0) break;
    if (escapedPunctuationAt(line, next) || codeSpanContains(codeIntervals, codeState, lineStart + next)) {
      cursor = next + 4;
      continue;
    }
    open = true;
    cursor = next;
  }
  return { visible: chars.join(''), open: open };
}

// One CommonMark-aware structural view shared by task-state admission and all
// Figma consumers. It preserves line offsets while blanking fenced code,
// inline comments, and all seven raw-HTML block forms, so every caller agrees
// which `## Design` heading is real.
function structuralText(text) {
  var source = String(text || '').replace(/^\uFEFF/, '').replace(/\r\n?/g, '\n');
  var lines = source.split('\n');
  var lineStarts = [];
  var offset = 0;
  for (var lineIndex = 0; lineIndex < lines.length; lineIndex++) {
    lineStarts.push(offset);
    offset += lines[lineIndex].length + 1;
  }
  var codeIntervals = inlineCodeSpanIntervals(source, lines, lineStarts);
  var codeState = { index: 0 };
  var fence = null;
  var htmlBlock = null;
  var htmlComment = false;
  var paragraphOpen = false;
  return lines.map(function (line, index) {
    if (htmlBlock) {
      var ended = masksEntireHtmlBlockLine(htmlBlock, line);
      if (ended) htmlBlock = null;
      if (/^[ \t]*$/.test(line)) paragraphOpen = false;
      return ' '.repeat(line.length);
    }
    if (htmlComment) {
      // This physical line began inside an inline comment that belongs to the
      // preceding paragraph/container. Even after `-->`, its suffix cannot
      // become block structure on the same line (`-->## Goal` is not an ATX
      // heading). Rescan only to carry a chained comment forward, then mask
      // the complete line while preserving the prior paragraph state.
      var continued = maskInlineHtmlComments(line, true, lineStarts[index], codeIntervals, codeState);
      htmlComment = continued.open;
      return ' '.repeat(line.length);
    }
    // CommonMark permits at most three literal U+0020 spaces before a fenced
    // code delimiter. Four spaces (or a leading tab) make an indented code
    // line, not a fence that can hide later structural headings.
    var fenceOpen = /^ {0,3}(`{3,}|~{3,})(.*)$/.exec(line);
    if (fenceOpen && fenceOpen[1][0] === '`' && fenceOpen[2].indexOf('`') >= 0) fenceOpen = null;
    if (!fence && fenceOpen) {
      fence = { character: fenceOpen[1][0], length: fenceOpen[1].length };
      paragraphOpen = false;
      return ' '.repeat(line.length);
    }
    if (fence && new RegExp('^ {0,3}' + (fence.character === '`' ? '`' : '~') +
      '{' + fence.length + ',}[ \\t]*$').test(line)) {
      fence = null;
      paragraphOpen = false;
      return ' '.repeat(line.length);
    }
    if (fence) return ' '.repeat(line.length);

    // Indented code may start only when no paragraph is open. Its contents
    // are literal and therefore cannot open an HTML comment that masks later
    // task headings. Four spaces or one leading tab are the CommonMark forms.
    if (!paragraphOpen && (/^ {4}/.test(line) || /^\t/.test(line))) {
      return ' '.repeat(line.length);
    }

    var block = htmlBlockStart(line, paragraphOpen);
    if (block) {
      if (!masksEntireHtmlBlockLine(block, line)) htmlBlock = block;
      paragraphOpen = false;
      return ' '.repeat(line.length);
    }

    var maskedComments = maskInlineHtmlComments(line, false, lineStarts[index], codeIntervals, codeState);
    htmlComment = maskedComments.open;
    var visible = maskedComments.visible;
    if (/^[ \t]*$/.test(visible) || /^ {0,3}#{1,6}(?:[ \t]+|$)/.test(visible) ||
        /^ {0,3}(?:\*[ \t]*){3,}$/.test(visible) || /^ {0,3}(?:-[ \t]*){3,}$/.test(visible) ||
        /^ {0,3}(?:_[ \t]*){3,}$/.test(visible)) paragraphOpen = false;
    else paragraphOpen = true;
    return visible;
  }).join('\n');
}

// Parse one CommonMark ATX heading line. The opening sequence may be indented
// by zero to three literal spaces; four spaces are an indented code block, and
// a leading tab likewise does not create a top-level heading. CommonMark also
// permits a whitespace-delimited closing `#` sequence, which is syntax rather
// than part of the heading text (`## Design ##` names the section `Design`).
//
// Keep this tiny primitive shared: task-state lifecycle parsing, dependency
// admission, Outcome detection, and every Figma Design consumer must agree on
// whether an H2 is structural. Callers pass the already-masked structural line
// when they need fenced/raw-HTML awareness.
function stripAtxClosingSequence(raw) {
  // Equivalent to /[ \t]+#+[ \t]*$/, but linear on a bounded superline.
  // An unanchored greedy whitespace regex retries from every whitespace byte
  // when no closing hash exists, which made admission quadratic.
  var trailing = raw.length;
  while (trailing > 0 && (raw.charAt(trailing - 1) === ' ' || raw.charAt(trailing - 1) === '\t')) trailing--;
  var hashes = trailing;
  while (hashes > 0 && raw.charAt(hashes - 1) === '#') hashes--;
  if (hashes === trailing || hashes === 0 ||
      (raw.charAt(hashes - 1) !== ' ' && raw.charAt(hashes - 1) !== '\t')) return raw;
  var whitespace = hashes;
  while (whitespace > 0 && (raw.charAt(whitespace - 1) === ' ' || raw.charAt(whitespace - 1) === '\t')) whitespace--;
  return raw.slice(0, whitespace);
}

function horizontalTrim(raw) {
  var start = 0;
  var end = raw.length;
  while (start < end && (raw.charAt(start) === ' ' || raw.charAt(start) === '\t')) start++;
  while (end > start && (raw.charAt(end - 1) === ' ' || raw.charAt(end - 1) === '\t')) end--;
  return raw.slice(start, end);
}

function parseAtxHeadingLine(line) {
  var match = /^ {0,3}(#{1,6})(?=$|[ \t])([ \t]*)(.*)$/.exec(String(line || ''));
  if (!match) return null;
  var raw = match[2] + match[3];
  // The optional closing sequence must be preceded by whitespace and may be
  // followed only by spaces/tabs. Escaped hashes therefore remain content.
  raw = stripAtxClosingSequence(raw);
  return { level: match[1].length, name: horizontalTrim(raw) };
}

function scanAtxHeadings(markdown, level) {
  var structural = structuralText(markdown);
  var headings = [];
  var start = 0;
  while (start <= structural.length) {
    var newline = structural.indexOf('\n', start);
    var headEnd = newline < 0 ? structural.length : newline;
    var parsed = parseAtxHeadingLine(structural.slice(start, headEnd));
    if (parsed && (level == null || parsed.level === level)) {
      headings.push({
        level: parsed.level,
        name: parsed.name,
        start: start,
        headEnd: headEnd,
        nextLineStart: newline < 0 ? structural.length : newline + 1
      });
    }
    if (newline < 0) break;
    start = newline + 1;
  }
  return { structural: structural, headings: headings };
}

function hasDesignSection(markdown) {
  return scanAtxHeadings(markdown, 2).headings.some(function (heading) {
    return heading.name === 'Design';
  });
}

function hashText(text) {
  return 'sha256:' + crypto.createHash('sha256').update(String(text || '')).digest('hex');
}

function designSection(markdown) {
  var scanned = scanAtxHeadings(markdown, 2);
  var index = -1;
  for (var i = 0; i < scanned.headings.length; i++) {
    if (scanned.headings[i].name === 'Design') { index = i; break; }
  }
  if (index < 0) return '';
  var heading = scanned.headings[index];
  var next = scanned.headings[index + 1] || null;
  var end = scanned.structural.length;
  if (next) {
    // Preserve the stable Design-section byte projection: the heading's
    // newline is excluded, as is the newline immediately before the next H2.
    // Adjacent headings therefore still produce the empty string.
    end = next.start;
    if (end > heading.nextLineStart && scanned.structural.charAt(end - 1) === '\n') end--;
  }
  return scanned.structural.slice(heading.nextLineStart, end);
}

function splitBullet(line) {
  var s = String(line || '').trim();
  if (!s || s.charAt(0) !== '-') return null;
  s = s.replace(/^-\s*/, '').trim();
  var idx = s.indexOf(' — ');
  if (idx < 0) idx = s.indexOf(' - ');
  if (idx < 0) idx = s.indexOf(' -- ');
  if (idx < 0) return null;
  return { screen: s.slice(0, idx).trim(), value: s.slice(idx + (s.slice(idx, idx + 3) === ' — ' ? 3 : s.slice(idx, idx + 4) === ' -- ' ? 4 : 3)).trim() };
}

function normalizeNodeId(raw) {
  return String(raw || '').trim().replace(/-/g, ':');
}

// A `## Design` bullet may carry the node kind as a trailing
// `[screen|dialog|component|overlay]` tag, e.g. `- TerminalMenu [overlay] — <url>`. The
// kind tells the builder which capture harness to scaffold (full-bleed screen vs isolated
// dialog vs wrap-content component vs composed-over-host overlay) and lets the viewer label
// each comparison. Default is `screen`. `overlay` is a node that cannot be captured by an
// isolated full-bleed render (a popup/sheet drawn over a dimmed host) — it is gated
// fail-closed (UNREPRESENTABLE_OVERLAY) until a representable capture exists, so it can
// never ship silently uncompared. Only the recognised kinds are stripped, so a real name
// ending in `[x]` is left intact — but a NEAR-MISS tag (`[sheet]`, `[popup]`, `[modal]`)
// is flagged via `unrecognizedTag` so parseDesign can surface UNRECOGNIZED_KIND_TAG instead
// of silently treating the node as a full-bleed screen named `Menu [sheet]`.
var DESIGN_KINDS = ['screen', 'dialog', 'component', 'overlay'];
function parseScreenKind(rawScreen) {
  var name = String(rawScreen || '').trim();
  var m = /^(.*\S)\s*\[(screen|dialog|component|overlay)\]$/i.exec(name);
  if (m) return { name: m[1].trim(), kind: m[2].toLowerCase() };
  // Unrecognized bracketed single-word tail: keep the FULL name and kind 'screen'
  // (unchanged behavior — a real name may end in `[x]`), but carry the tag so callers
  // can warn. Additive field; existing consumers only read `name`/`kind`.
  var near = /^(.*\S)\s*\[([a-z][a-z0-9-]*)\]$/i.exec(name);
  if (near) return { name: name, kind: 'screen', unrecognizedTag: near[2].toLowerCase() };
  return { name: name, kind: 'screen' };
}

function parseFigmaUrl(raw) {
  var value = String(raw || '').trim();
  var url;
  try { url = new URL(value); } catch (e) { return { ok: false, error: 'invalid url' }; }
  if (url.protocol !== 'https:' || !FIGMA_HOST_RE.test(url.hostname)) return { ok: false, error: 'not figma.com' };
  var pathMatch = /^\/(design|file)\/([A-Za-z0-9]+)(?:\/[^?#]*)?$/.exec(url.pathname);
  if (!pathMatch) return { ok: false, error: 'invalid figma file path' };
  var nodeId = url.searchParams.get('node-id') || '';
  if (!nodeId) return { ok: false, error: 'missing node-id' };
  var decodedNodeId = decodeURIComponent(nodeId);
  if (!/^[0-9]+[:-][0-9]+$/.test(decodedNodeId)) return { ok: false, error: 'invalid node-id' };
  var fileKey = pathMatch[2];
  var canonicalNodeId = decodedNodeId.replace('-', ':');
  return {
    ok: true,
    url: 'https://www.figma.com/' + pathMatch[1] + '/' + fileKey + '?node-id=' + encodeURIComponent(canonicalNodeId.replace(':', '-')),
    fileKey: fileKey,
    nodeId: canonicalNodeId,
    rawNodeId: nodeId
  };
}

function parseThemeValue(value) {
  var v = String(value || '').trim();
  if (!v || /^none\b/i.test(v)) {
    // A `none` value must be EXACTLY `none` or the audited `none (<reason>)` — nothing after.
    // Any other residue (`none <url>`, `none see mock`, `none (why) extra`) fails CLOSED as
    // DESIGN_VALUE_RESIDUE (already in MALFORMED_DESIGN_KINDS): a URL smuggled into an
    // unaudited none-value would otherwise be subtracted from bodyCitesFigmaNode's scan with
    // the rest of the `## Design` section, letting a node-citing UI task self-classify
    // non-UI and ship uncompared with no audited reason (the W1 backstop bypass).
    var noneIssues = [];
    if (v && !/^none\s*(?:\(\s*[^)\s][^)]*\)\s*)?$/i.test(v)) {
      noneIssues.push({ kind: 'DESIGN_VALUE_RESIDUE', value: v, message: 'unparsed text in the none-value ' + JSON.stringify(v) + ' — the ONLY accepted shapes are exactly `none`, or `none (<reason>)` with a non-empty reason that contains no ")" and nothing after the closing paren' });
    }
    return { none: true, themes: {}, issues: noneIssues };
  }

  var tagged = [];
  var issues = [];
  var re = /\b(light|dark):(\S+)/gi;
  var m;
  while ((m = re.exec(v)) != null) tagged.push({ theme: m[1].toLowerCase(), rawUrl: m[2] });

  if (!tagged.length) {
    tagged.push({ theme: 'primary', rawUrl: v });
  } else {
    // At least one light:/dark: tag matched — the value MUST be fully consumed by tagged
    // tokens. Any untagged residue ('light:<url1> darc:<url2>', 'light:<url1> <url2>',
    // '<url1> dark:<url2>') is a one-character typo away from silently dropping a theme
    // from the comparison contract, so it fails closed instead of being discarded.
    var residue = v.replace(/\b(light|dark):(\S+)/gi, ' ').trim();
    if (residue) {
      issues.push({ kind: 'DESIGN_VALUE_RESIDUE', value: residue, message: 'unparsed text next to light:/dark: tags — every token must be a tagged theme URL' });
    }
  }

  var themes = {};
  for (var i = 0; i < tagged.length; i++) {
    var t = tagged[i];
    if (themes[t.theme]) {
      issues.push({ kind: 'DUPLICATE_THEME', theme: t.theme, message: 'duplicate design theme' });
      continue;
    }
    var parsed = parseFigmaUrl(t.rawUrl);
    if (!parsed.ok) {
      issues.push({ kind: 'INVALID_URL', theme: t.theme, value: t.rawUrl, message: parsed.error });
      continue;
    }
    themes[t.theme] = parsed;
  }
  return { none: false, themes: themes, issues: issues };
}

function parseDesign(markdown) {
  var section = designSection(markdown);
  var lines = section.split(/\r?\n/);
  var entries = [];
  var issues = [];
  var gateOverride = null;
  var seenScreens = Object.create(null);
  for (var i = 0; i < lines.length; i++) {
    var split = splitBullet(lines[i]);
    if (!split) {
      var bare = String(lines[i] || '').trim();
      // R2-3 tighten-only per-task gate override: the SEPARATOR-LESS bullet `- gate: strict`
      // forces strict pixel-verdict routing for this task's runs. Recognized ONLY on this
      // no-separator branch, so a legitimate SCREEN whose name merely starts with `Gate:`
      // (`- Gate: Confirm — none (…)` has a separator) keeps its normal entry parse. Any
      // OTHER separator-less `gate:` value is a MALFORMED design (DESIGN_VALUE_RESIDUE,
      // blocked at the cache gate): the weakening direction has no grammar, by construction.
      var gateMatch = /^-+\s*gate\s*:\s*(.*)$/i.exec(bare);
      if (gateMatch) {
        var gateValue = gateMatch[1].trim();
        if (gateValue === 'strict') {
          gateOverride = 'strict';
        } else {
          issues.push({ kind: 'DESIGN_VALUE_RESIDUE', line: i + 1, value: bare, message: 'gate bullet ' + JSON.stringify(bare) + ' — the ONLY accepted form is exactly `- gate: strict` (tighten-only; a task cannot weaken its own pixel gate)' });
        }
        continue;
      }
      // A bullet-shaped line (`-` prefix with content) that has no recognised
      // ` — `/` - `/` -- ` separator is a MALFORMED design entry, not a comment. Record it so
      // the gate can't mistake a typo'd design ref for a non-UI task and let it ship uncompared.
      if (bare.charAt(0) === '-' && bare.replace(/^-+\s*/, '').trim()) {
        issues.push({ kind: 'UNPARSEABLE_DESIGN_BULLET', line: i + 1, value: bare });
      }
      continue;
    }
    var sk = parseScreenKind(split.screen);
    if (!sk.name || /[<>]/.test(sk.name)) {
      issues.push({ kind: 'INVALID_SCREEN', line: i + 1, value: split.screen });
      continue;
    }
    // A second bullet reusing a screen name would be collapsed last-writer-wins by
    // parseDesignSources — the earlier node would silently exit every gate (never pulled,
    // never compared). Still emit the entry; the block comes from the issue.
    if (seenScreens[sk.name]) {
      issues.push({ kind: 'DUPLICATE_SCREEN', screen: sk.name, line: i + 1, message: 'duplicate screen name in one ## Design section' });
    } else {
      seenScreens[sk.name] = true;
    }
    // Near-miss kind tag (`[sheet]`/`[popup]`/...): warn-grade — NOT in hasMalformedDesign
    // (a real name may legitimately end in `[x]`), but surfaced through design.issues so
    // check-screen-cache flags it at prep time instead of a late MISSING_CAPTURE.
    if (sk.unrecognizedTag) {
      issues.push({ kind: 'UNRECOGNIZED_KIND_TAG', screen: sk.name, tag: sk.unrecognizedTag, line: i + 1, message: 'unrecognized kind tag [' + sk.unrecognizedTag + ']; valid kinds: ' + DESIGN_KINDS.join('|') });
    }
    var parsed = parseThemeValue(split.value);
    // W2-5 (warn-grade like UNRECOGNIZED_KIND_TAG — NOT in MALFORMED_DESIGN_KINDS, but
    // surfaced through design.issues so check-screen-cache blocks it at PREP time): a
    // PULLABLE screen whose name steps outside [A-Za-z0-9_] makes the derived capture
    // filename (`<Name>Screenshot.png`) unproducible by a Kotlin test — parens
    // ('Screen (Content)') reproduce the production MISSING_CAPTURE class 30 minutes
    // later at the comparator. nodeId manifest binding can rescue the comparator but
    // never check-capture-config's static join, so the name is fixed at the source.
    // `none` bullets are exempt: no capture will ever derive from their name.
    if (!parsed.none && /[^A-Za-z0-9_]/.test(sk.name)) {
      issues.push({ kind: 'RISKY_SCREEN_NAME', screen: sk.name, line: i + 1, message: 'screen name ' + JSON.stringify(sk.name) + ' contains characters outside [A-Za-z0-9_] — the capture file name derives from it; rename the Figma frame (or the bullet) to a PascalCase name before pulling' });
    }
    entries.push({
      screen: sk.name,
      kind: sk.kind,
      value: split.value,
      none: !!parsed.none,
      themes: parsed.themes,
      line: i + 1,
      issues: parsed.issues
    });
    issues = issues.concat(parsed.issues.map(function (x) {
      return Object.assign({ screen: sk.name, line: i + 1 }, x);
    }));
  }
  return {
    sourceHash: hashText(section),
    entries: entries,
    issues: issues,
    gateOverride: gateOverride,
    hasPullable: entries.some(function (e) { return !e.none && Object.keys(e.themes || {}).length > 0; })
  };
}

function parseDesignSources(markdowns) {
  var byScreen = Object.create(null);
  var order = [];
  var issues = [];
  var hashes = [];
  var gateOverride = null;
  for (var i = 0; i < (markdowns || []).length; i++) {
    var parsed = parseDesign(markdowns[i]);
    hashes.push(parsed.sourceHash);
    issues = issues.concat(parsed.issues);
    if (parsed.gateOverride === 'strict') gateOverride = 'strict';
    for (var j = 0; j < parsed.entries.length; j++) {
      var entry = parsed.entries[j];
      if (!byScreen[entry.screen]) order.push(entry.screen);
      byScreen[entry.screen] = entry;
    }
  }
  var entries = order.map(function (screen) { return byScreen[screen]; });
  return {
    sourceHash: hashText(hashes.join('\n')),
    entries: entries,
    issues: issues,
    gateOverride: gateOverride,
    hasPullable: entries.some(function (e) { return !e.none && Object.keys(e.themes || {}).length > 0; })
  };
}

function hasPullableDesign(markdown) {
  return parseDesign(markdown).hasPullable;
}

// --- W1: UI-classification backstop -----------------------------------------
// The entire pixel-comparison chain keys on hasPullableDesign() (a `## Design` bullet). Ways a
// REAL UI artifact escapes the chain by self-classifying non-UI:
//   (A1) a machine-minted design-system component task (component task suggestions / task-prep
//        figma-split) cites the component-set node (a `designComponentId:` snapshot with
//        `figmaNodeId:`/`frozenStructuralHash:`, or a Figma node URL) in `## Inputs` but writes
//        NO pullable `## Design` bullet;
//   (A2) a hand-authored screen/dialog task cites the Figma node URL or edits a screen/dialog
//        file but omits `## Design`.
// Detection is layered, strongest signal first: (1) a valid Figma node URL ANYWHERE in the body
// (the same strict parseFigmaUrl predicate the `## Design` gate uses) — the primary, naming- and
// token-independent signal; (2) the machine component snapshot (`designComponentId:` plus
// `figmaNodeId:`/`frozenStructuralHash:`, which a doc merely illustrating the field does not
// carry); (3) a screen/dialog filename in
// `### Files touched` — corroborating only. The sole sanctioned opt-out is an explicit,
// human-authored `- <Name> — none (<why>)` bullet — audited, not a bare `— none`.

// (1) A valid Figma node URL anywhere in the body. When we reach here hasPullable is false and
// (in the gate paths) hasMalformedDesign is false, so any valid Figma URL found is necessarily
// OUTSIDE a working `## Design` bullet — i.e. a node cited in `## Inputs`/prose with no bullet.
var FIGMA_URL_TOKEN_RE = /https?:\/\/[^\s`)<>\]]+/gi;
function bodyCitesFigmaNode(markdown) {
  // Scan the body EXCLUDING the `## Design` section: a valid pullable bullet there already means
  // hasPullable=true (the caller returns first), and a URL sitting inside a `— none (…)` reason
  // must not fire. The URL regex is one greedy char class (no nested quantifier) → O(n)/ReDoS-safe
  // on ANY line length, so no line cap (a real node URL can sit on a long line; a cap would leak it).
  var s = structuralText(markdown);
  var design = designSection(s);
  var body = design ? s.split(design).join('\n') : s;
  var lines = body.split(/\r?\n/);
  for (var i = 0; i < lines.length; i++) {
    var toks = lines[i].match(FIGMA_URL_TOKEN_RE);
    if (!toks) continue;
    for (var j = 0; j < toks.length; j++) if (parseFigmaUrl(toks[j]).ok) return true;
  }
  return false;
}

// (2) The machine-minted component snapshot: a block carrying `designComponentId:` (the
// durable stable identity every design-origin component task binding writes into the task
// body) AND a machine anchor field — `figmaNodeId:` (the owning set/main node) OR
// `frozenStructuralHash:` (the frozen spec anchor the binding evidence pins).
// Requiring designComponentId + a machine field (never a lone `figmaNodeId:`) avoids
// false-blocking a doc/spec task that merely illustrates one field, while still catching a
// component task even when its node URL was dropped — such a component cannot be
// pixel-compared until a node URL is supplied, so it must be blocked, not shipped uncompared.
function hasComponentSnapshot(markdown) {
  var s = String(markdown || '');
  var hasStableId = /^[ \t]*designComponentId:[ \t]*figma-component:\S/m.test(s);
  var hasAnchor = /^[ \t]*figmaNodeId:[ \t]*\S/m.test(s) || /^[ \t]*frozenStructuralHash:[ \t]*\S/m.test(s);
  return hasStableId && hasAnchor;
}

// (3) Screen/dialog filename signatures in `### Files touched` — CORROBORATING ONLY (signals 1-2
// are the robust primary net; a node built from Figma almost always CITES its node URL, caught
// there). Deliberately NARROW to the UNAMBIGUOUS screen/dialog suffixes to avoid mass false-blocks:
// `*Content`/`*Page`/bare `*Sheet` (kt) and `*View` (swift — the suffix of EVERY SwiftUI view:
// rows, cells, buttons) were dropped after they false-flagged ordinary non-screen files. The
// residual — a screen file named outside this list with NO node cited — is an accepted gap (rare;
// and there is no oracle to compare a screen that references no Figma node). ReDoS-safe: split each
// line into tokens on delimiters FIRST, then test an ANCHORED suffix (no unbounded `+` before ext).
var UI_KT_SUFFIX_RE = /(?:Screen|Dialog|BottomSheet|Overlay)\.kt$/i;
var UI_SWIFT_SUFFIX_RE = /(?:Screen|Dialog|Overlay)\.swift$/i;
function uiCodeFilesTouched(markdown) {
  var m = String(markdown || '').match(/(?:^|\n)###[ \t]+Files touched[ \t]*\r?\n([\s\S]*?)(?=\r?\n#{1,6}[ \t]|\r?\n---[ \t]*\r?\n|$)/);
  if (!m) return [];
  var out = [];
  var lines = m[1].split(/\r?\n/);
  for (var i = 0; i < lines.length; i++) {
    var toks = lines[i].split(/[\s`|()\[\],]+/);
    for (var j = 0; j < toks.length; j++) {
      var t = toks[j];
      // Skip tokens >300 chars: a real path is short, and an anchored `$` .test() on a huge
      // unbroken token would scan superlinearly (ReDoS guard — replaces the old whole-line cap,
      // which also skipped a real path on a long line).
      if (t && t.length <= 300 && (UI_KT_SUFFIX_RE.test(t) || UI_SWIFT_SUFFIX_RE.test(t)) && out.indexOf(t) < 0) out.push(t);
    }
  }
  return out;
}

// One entry per `### Files touched` bullet: { path, status }. Status is the `created | modified |
// deleted | renamed` word the frozen outcome shape records after the path (outcome-appendix.md).
// The path is read from the first backtick span (the shape's `- `<path>` — <status>`), falling back
// to the first path-like token. Absent status → '' (callers treat that as the lenient class).
function filesTouchedEntries(markdown) {
  var m = String(markdown || '').match(/(?:^|\n)###[ \t]+Files touched[ \t]*\r?\n([\s\S]*?)(?=\r?\n#{1,6}[ \t]|\r?\n---[ \t]*\r?\n|$)/);
  if (!m) return [];
  var out = [];
  var lines = m[1].split(/\r?\n/);
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i];
    var path = null, rest = line;
    var bt = line.match(/`([^`]{1,300})`/);
    if (bt) { path = bt[1].trim(); rest = line.slice(bt.index + bt[0].length); }
    else {
      var bare = line.match(/^\s*[-*][ \t]+(\S{1,300})/);
      if (bare) { path = bare[1]; rest = line.slice(bare.index + bare[0].length); }
    }
    if (!path) continue;
    var st = rest.match(/\b(created|added|new|modified|changed|edited|updated|deleted|removed|renamed|moved)\b/i);
    out.push({ path: path, status: st ? st[1].toLowerCase() : '' });
  }
  return out;
}

// A source path that renders UI, by ARCHITECTURE not filename guesswork: a `.kt`/`.swift` file
// ANYWHERE under a `components/` directory (module-structure: "Non-`*Screen` UI lives in
// `components/`", incl. `:design-system:components`; the template nests widgets as
// `.../design/components/<group>/<Widget>.kt`), OR a screen/dialog/overlay/bottomsheet file (the
// existing suffixes). Path-based, so it catches a feature-local card regardless of its class name
// (`SpeedCard`, `StatRow`, `MacroChip`, …) and regardless of `public`/`internal`. A small set of
// unambiguously non-UI leaf dirs under `components/` (utils/di/mappers/modifiers) is excluded so a
// helper co-located there is not false-flagged as a renderable widget.
// The file's IMMEDIATE-PARENT dir names that are unambiguously NON-renderable helpers (Modifier
// extension factories, DI, data mappers, plain utils). Checked ONLY against the leaf dir, never any
// tail segment: `internal/`, `model/`, `ext/` are DELIBERATELY excluded from this set — the template
// stores real `@Composable` widgets under `.../<group>/internal/<Card>.kt` (an `internal` visibility
// convention, not a non-UI marker), so excluding those would re-open the very escape this catches.
// Erring toward block (a genuine non-UI helper here is escapable with a per-widget `— none`).
var NON_UI_COMPONENT_LEAF_RE = /^(?:utils?|di|mappers?|modifiers?)$/i;
// A `.kt`/`.swift` at ANY depth under a `components/` dir (minus the non-UI leaf dirs). This is the
// Feature-local cards are treated STRICTLY (fail-closed).
function isComponentsCardPath(p) {
  if (!p || p.length > 300 || !/\.(kt|swift)$/i.test(p)) return false;
  if (!/(?:^|\/)components\//i.test(p)) return false;     // somewhere under a components/ dir
  var parts = p.split('/');
  var leafDir = parts.length >= 2 ? parts[parts.length - 2] : '';
  return !NON_UI_COMPONENT_LEAF_RE.test(leafDir);
}
// A screen/dialog/overlay/bottomsheet file — the pre-existing WEAK signal (a screen-file edit may be
// a non-visual copy/callback change), deliberately advisory unless a node is cited.
function isScreenSuffixPath(p) {
  return !!p && p.length <= 300 && (UI_KT_SUFFIX_RE.test(p) || UI_SWIFT_SUFFIX_RE.test(p));
}
function isUiWidgetPath(p) { return isComponentsCardPath(p) || isScreenSuffixPath(p); }

// UI-widget source files the task touched, split into CREATED (a new UI surface that MUST be
// accounted for → block) vs MODIFIED (a possibly non-visual edit → warn). Two different defaults, by
// class: a `components/` CARD is FAIL-CLOSED (created unless the status is an EXPLICIT modification,
// so a new card cannot slip to WARN on an omitted/paraphrased status word — the
// gap); a SCREEN/DIALOG file keeps the deliberate weak default (advisory unless the status is an
// EXPLICIT creation), so a non-visual screen-file edit is not hard-blocked. Deleted/removed drop.
function uiWidgetSourcesTouched(markdown) {
  var created = [], modified = [];
  filesTouchedEntries(markdown).forEach(function (e) {
    var isScreen = isScreenSuffixPath(e.path);
    var isCard = !isScreen && isComponentsCardPath(e.path);
    if (!isScreen && !isCard) return;
    if (/^(?:deleted|removed)$/.test(e.status)) return;                                        // removed: nothing to compare
    var toCreated = isScreen
      ? /^(?:created|added|new)$/.test(e.status)                                               // screen: created ONLY on explicit word
      : !/^(?:modified|changed|edited|updated|renamed|moved)$/.test(e.status);                 // card: created UNLESS explicit modification (fail-closed)
    if (toCreated) { if (created.indexOf(e.path) < 0) created.push(e.path); }
    else if (modified.indexOf(e.path) < 0) modified.push(e.path);
  });
  return { created: created, modified: modified };
}

// Subjects (widget names) of audited `- <Name> — none (<reason>)` bullets — the per-widget opt-out
// targets. Used to clear a CREATED widget only when a `— none` bullet actually names THAT widget.
function auditedNoneSubjects(markdown) {
  return (parseDesign(markdown).entries || [])
    .filter(function (e) { return e && e.none && AUDITED_NONE_RE.test(String(e.value || '')); })
    .map(function (e) { return normComponentName(e.screen); })
    .filter(Boolean);
}
// A created widget file is accounted-for by a none subject when the file's basename (widget name)
// normalizes equal to the subject — `.../components/SpeedCard.kt` ↔ `- SpeedCard — none (…)`.
function createdWidgetAccounted(filePath, noneSubjects) {
  var base = String(filePath || '').split('/').pop().replace(/\.(kt|swift)$/i, '');
  var norm = normComponentName(base);
  return norm && noneSubjects.indexOf(norm) >= 0;
}

// Both values are contractually repo-relative. Normalize only the harmless leading `./` spelling;
// an omitted module prefix is not enough identity and must never match another source implicitly.
function pathMatchesSource(touched, source) {
  var a = String(touched || '').replace(/^\.\//, '');
  var b = String(source || '').replace(/^\.\//, '');
  if (!a || !b) return false;
  return a === b;
}

// Content-based provenance: a component the mapping registry records with a Figma node anchor whose
// `source` file the task TOUCHED — regardless of the task stem. Closes the stem-shaped hole where a
// node-backed component authored under a non-`_component_` stem escaped the stem-only provenance tier.
// W3-6: a provenance entry carrying `figmaNodeRetired {reason, at, by}` is the OWNER's
// auditable retirement of the Figma anchor (design deleted/pivoted upstream — a retired
// mapping keeps its rows via Mapping Review retirement). The provenance tiers
// skip it: a retired node can no longer demand a design bullet for every future touch of the
// component's source. Retirement is the ONLY sanctioned skip besides the derive-only
// carve-out (no figmaNodeId) — a live anchor stays un-opt-out-able.
function provenanceRetired(entry) {
  var r = entry && entry.figmaNodeRetired;
  return !!(r && typeof r === 'object' && String(r.reason || '').trim());
}

function provenanceSourceTouched(markdown, inventory) {
  if (!Array.isArray(inventory)) return null;
  var touched = filesTouchedEntries(markdown).map(function (e) { return e.path; });
  if (!touched.length) return null;
  for (var i = 0; i < inventory.length; i++) {
    var it = inventory[i];
    if (!it || !it.source || it.figmaNodeId == null || !String(it.figmaNodeId).trim()) continue;
    if (provenanceRetired(it)) continue;
    for (var j = 0; j < touched.length; j++) {
      if (pathMatchesSource(touched[j], it.source)) return { nodeId: String(it.figmaNodeId).trim(), component: it.component || it.source };
    }
  }
  return null;
}

// The audited `— none (<reason>)` opt-out — an explicit design bullet whose value is `none`
// AND carries a non-empty parenthetical reason. Task-prep preserves this shape only when
// the source/owner explicitly supplies the no-mock decision; it never infers the opt-out
// from a missing URL. A BARE
// `— none` (no parenthetical) is NOT audited — a real UI task can no longer silently opt out;
// it must state why.
// The SINGLE audited-none predicate regex — `none` followed by a non-empty parenthetical reason.
// hasAuditedNoneOptOut() (the opt-out gate) and auditedNoneCount() (the B2 erosion counter) BOTH
// use it, so the counter can never drift from what actually counts as an audited opt-out. Not
// global (no /g) — safe to reuse across .test() calls (no lastIndex state).
var AUDITED_NONE_RE = /^none\b\s*\(\s*[^)\s][^)]*\)/i;
function hasAuditedNoneOptOut(markdown) {
  var p = parseDesign(markdown);
  return (p.entries || []).some(function (e) {
    return e && e.none && AUDITED_NONE_RE.test(String(e.value || ''));
  });
}

// B2 erosion detector: how many audited `— none (<why>)` opt-out bullets a task declares. A count,
// not a some() — verify-done sums it across done/ to surface a slowly-eroding comparison guarantee
// (every screen opted out one `— none` at a time). Reuses AUDITED_NONE_RE. Parser failures
// propagate to the gate; zero is reserved for a successfully parsed task with no opt-outs.
function auditedNoneCount(markdown) {
  var p = parseDesign(markdown);
  return (p.entries || []).filter(function (e) {
    return e && e.none && AUDITED_NONE_RE.test(String(e.value || ''));
  }).length;
}

// Returns a tiered verdict `{ level, reason }` when a task is UI-by-evidence yet self-classifies
// non-UI (no pullable `## Design`), or null when the task is legitimately non-UI / already on the
// pixel track. Tiers, strongest first:
//   * 'block' (un-opt-out-able) — a real Figma node provably EXISTS: a cited node URL, a machine
//     component snapshot, OR inventory provenance (by stem or by touched `source` .kt). Must compare.
//   * 'block' (opt-out-able) — a newly CREATED UI-widget source file (a `components/` widget or a
//     screen/dialog file) with no design bullet: a new UI surface MUST be accounted for. The audited
//     `— none (<why>)` opt-out rescues a genuinely non-visual / mockless one.
//   * 'warn' — a MODIFIED UI-widget source file with no node cited: advisory (the non-visual-edit
//     tolerance — a copy/callback tweak must not hard-block).
// The optional `opts.inventory` (componentProvenanceEntries() rows from the design inventory +
// mapping registry) + `opts.stem` add a provenance tier: a component the registry records as built
// from a Figma NODE cannot self-declare non-UI even after its in-text snapshot is stripped — the
// surviving provenance is consulted so the visual comparison can never be dropped by editing the
// task text. An entry without a figmaNodeId is the sanctioned derive-only carve-out and is NOT held.
function normComponentName(name) {
  return String(name || '').replace(/[^a-z0-9]/gi, '').toLowerCase();
}
function componentTokenFromStem(stem) {
  // Anchor to the task-TYPE segment: ONLY a genuine `TASK_<n>_component_<name>` stem triggers the
  // provenance tier. A mid-stem `_component_` in a non-component task (e.g.
  // `TASK_8_refactor_component_census_parser`) must NOT match — matching it would false-block an
  // unrelated engineering task. The name may be multi-word (snake_case); normalize it (strip
  // separators) so it compares equal to the inventory's PascalCase `component` value.
  var m = /^task_\d+_component_(.+)$/i.exec(String(stem || ''));
  return m ? normComponentName(m[1]) : null;
}
function provenanceNodeForStem(stem, inventory) {
  var token = componentTokenFromStem(stem);
  if (!token || !Array.isArray(inventory)) return null;
  for (var i = 0; i < inventory.length; i++) {
    var e = inventory[i];
    if (!e || !e.component) continue;
    if (normComponentName(e.component) !== token) continue;
    if (provenanceRetired(e)) continue;
    var nodeId = e.figmaNodeId;
    if (nodeId != null && String(nodeId).trim()) return { nodeId: String(nodeId).trim(), component: e.component };
  }
  return null;
}
function uiTaskWithoutDesign(markdown, opts) {
  var p = parseDesign(markdown);
  if (p.hasPullable) return null;                    // already on the pixel track
  // STRONG signals FIRST — a real Figma node mock provably EXISTS. An audited `— none (no mock)`
  // claims the opposite, so it must NOT suppress these: a cited node URL / component snapshot that
  // is not a pullable bullet must be declared + compared. (Closes the escape where one audited
  // `— none` for a design-less screen disarmed the whole task while a co-resident node-backed
  // screen shipped uncompared — the opt-out is per-screen for FILENAME evidence only, below.)
  if (bodyCitesFigmaNode(markdown)) {
    return { level: 'block', reason: 'task cites a Figma node URL but declares no pullable `## Design` bullet — a screen/component built from a Figma node must be pixel-compared; add a `- <Name> [screen|dialog|component] — <url>` bullet using that node URL, or remove the stale Figma-node citation if the task is not design-backed' };
  }
  if (hasComponentSnapshot(markdown)) {
    return { level: 'block', reason: 'task embeds a Figma component snapshot (`designComponentId:` + node/structural anchor) but declares no pullable `## Design` bullet — a component built from a Figma node must be pixel-compared; add `- <Widget> [component] — <figma node url>` using the component node, or remove the stale snapshot if the task is not design-backed' };
  }
  // PROVENANCE tier (STRONG): the in-text snapshot may be gone, but the mapping registry still
  // records this widget as built from a Figma node — the surviving source of truth. Matched two
  // ways: by the task STEM (`TASK_n_component_<name>`) OR, content-based, by the provenance `source`
  // file appearing in `### Files touched` (so a node-backed component authored under any stem is still
  // held). Placed BEFORE the audited-`none` opt-out so a node-backed component cannot be shipped
  // uncompared by stripping the task and pasting `— none`; the sanctioned skip is only when the
  // provenance entry itself carries no `figmaNodeId` (the derive-only carve-out).
  var prov = opts && (provenanceNodeForStem(opts.stem, opts.inventory) || provenanceSourceTouched(markdown, opts.inventory));
  if (prov) {
    return { level: 'block', reason: 'component ' + prov.component + ' is recorded in the component inventory as built from Figma node ' + prov.nodeId + ', but this task declares no pullable `## Design` bullet — the visual comparison must not be dropped by de-classifying the task; add `- ' + prov.component + ' [component] — <figma node url>` using that node (the comparison re-confirms no visual change even for a non-visual edit)' };
  }
  // STRUCTURAL tier — architecture-aligned, not filename guesswork (uiWidgetSourcesTouched keys on a
  // `components/` dir + the screen/dialog/overlay suffixes). A newly CREATED UI widget is a new UI
  // surface that MUST be accounted for → BLOCK unless a design bullet compares it (then hasPullable
  // is true and we already returned null at the top — a card rendered inside a declared screen is
  // compared transitively) OR a PER-WIDGET audited `- <Widget> — none (<why>)` NAMES it. The opt-out
  // is per-widget, NOT a task-global boolean: one unrelated `— none` must not disarm every co-resident
  // created card.
  var widgets = uiWidgetSourcesTouched(markdown);
  if (widgets.created.length) {
    var noneSubjects = auditedNoneSubjects(markdown);
    var unaccounted = widgets.created.filter(function (f) { return !createdWidgetAccounted(f, noneSubjects); });
    if (unaccounted.length) {
      return { level: 'block', reason: 'task CREATES UI widget source file(s) [' + unaccounted.slice(0, 3).join(', ') + '] with no pullable `## Design` bullet and no audited `— none (<why>)` naming them — a newly-authored UI widget (design-system OR feature-local card) must be pixel-compared; add `- <Widget> [component|screen|dialog] — <figma node url>` to compare it, or a `- <Widget> — none (<why no mock>)` bullet naming THAT widget to record it is non-visual / has no mock' };
    }
  }
  // MODIFIED UI files (a possibly non-visual edit) → advisory WARN, task-global opt-out-able: a
  // genuinely non-visual edit records one audited `— none (<why>)` and ships (the deliberate
  // non-visual-edit tolerance; does not apply to the CREATED tier above or the strong tiers).
  if (hasAuditedNoneOptOut(markdown)) return null;
  if (widgets.modified.length) {
    return { level: 'warn', reason: 'task modifies UI widget source file(s) [' + widgets.modified.slice(0, 3).join(', ') + '] but declares no pullable `## Design` bullet — if this edit changes rendered pixels, add `- <Name> [screen|dialog|component] — <url>` so it is pixel-compared; if it is non-visual (copy/callback/logic) or has no mock, add `- <Name> — none (<why>)` to record that' };
  }
  return null;
}

// The first VALID Figma node URL cited in the body OUTSIDE the `## Design` section (canonical
// form), else '' — used to prefill the B5 paste bullet with the real URL when the task cited one.
function firstCitedFigmaUrl(markdown) {
  var s = structuralText(markdown);
  var design = designSection(s);
  var body = design ? s.split(design).join('\n') : s;
  var lines = body.split(/\r?\n/);
  for (var i = 0; i < lines.length; i++) {
    var toks = lines[i].match(FIGMA_URL_TOKEN_RE);
    if (!toks) continue;
    for (var j = 0; j < toks.length; j++) { var p = parseFigmaUrl(toks[j]); if (p.ok) return p.url; }
  }
  return '';
}

// B5: a ready-to-paste `## Design` bullet for the block message, so a blocked ship offers a
// one-paste fix instead of hand-authoring. Derives a screen Name + kind from the first touched
// screen/dialog FILE (basename minus the recognized suffix), or from a `component:` snapshot field,
// and the actual cited Figma URL when present — else a `<figma node url>` placeholder (angle
// brackets kept: the board's pullable predicate excludes `<`, so an unfilled bullet is never
// mistaken for pullable). The Name is sanitized to contain no `<>` (would trip INVALID_SCREEN) and
// no ` — `/` - ` separator (would make splitBullet mis-split). Returns `- <Name> [kind] — <url>`.
var SUFFIX_KIND = [
  [/BottomSheet$/, 'dialog'],
  [/Overlay$/, 'overlay'],
  [/Dialog$/, 'dialog'],
  [/Screen$/, 'screen'],
];
function sanitizeBulletName(raw) {
  var s = String(raw || '').replace(/[<>]/g, '').replace(/\s+(?:—|--|-)\s+/g, ' ').trim();
  return s;
}
function suggestedDesignBullet(markdown) {
  var uiFiles = uiCodeFilesTouched(markdown);
  var name = '';
  var kind = '';
  if (uiFiles.length) {
    var base = String(uiFiles[0]).replace(/^.*[\\/]/, '').replace(/\.(kt|swift)$/i, '');
    for (var k = 0; k < SUFFIX_KIND.length; k++) {
      if (SUFFIX_KIND[k][0].test(base)) { kind = SUFFIX_KIND[k][1]; base = base.replace(SUFFIX_KIND[k][0], ''); break; }
    }
    name = sanitizeBulletName(base);
  } else {
    var cm = /^[ \t]*component:[ \t]*(\S[^\n]*)$/im.exec(String(markdown || ''));
    if (cm) { name = sanitizeBulletName(cm[1]); if (name) kind = 'component'; }
  }
  if (!name) name = 'Screen';
  if (!kind) kind = 'screen';
  var url = firstCitedFigmaUrl(markdown) || '<figma node url>';
  return '- ' + name + ' [' + kind + '] — ' + url;
}

// A `## Design` section that CLEARLY intends a Figma comparison but is BROKEN: an unparseable
// bullet, an invalid screen name (`[<>]`), an invalid Figma URL, a duplicated screen name
// (last-writer-wins would silently drop a declared node), untagged residue beside light:/dark:
// tags (a typo'd theme tag would silently drop a theme), or a screen whose value is
// neither `none` nor a valid pullable node. The gates MUST treat this as a malformed UI task
// (BLOCK / violation) rather than a non-UI task (skip) — otherwise a single typo makes a real UI
// task fail OPEN past the mandatory screenshot gate. A clean `- Screen — none` (explicit opt-out)
// and a clean pullable bullet are NOT malformed. UNRECOGNIZED_KIND_TAG is deliberately NOT here:
// a real name may end in `[x]`, so it stays warn-grade via design.issues.
// The SINGLE SOURCE OF TRUTH for the malformed-`## Design`-kind set. site/server/state.js
// imports this exact object (not a hand-copied literal) for its board "Design: broken" chip,
// so the chip can never drift from what hasMalformedDesign() actually blocks. Frozen — every
// consumer only reads it. lint.sh check 7c belt-and-suspenders-asserts state.js aliases it.
var MALFORMED_DESIGN_KINDS = Object.freeze({ UNPARSEABLE_DESIGN_BULLET: 1, INVALID_SCREEN: 1, INVALID_URL: 1, DUPLICATE_THEME: 1, DUPLICATE_SCREEN: 1, DESIGN_VALUE_RESIDUE: 1 });
function hasMalformedDesign(markdown) {
  var p = parseDesign(markdown);
  if ((p.issues || []).some(function (x) { return x && MALFORMED_DESIGN_KINDS[x.kind]; })) return true;
  return (p.entries || []).some(function (e) { return e && !e.none && Object.keys(e.themes || {}).length === 0; });
}

module.exports = {
  structuralText: structuralText,
  parseAtxHeadingLine: parseAtxHeadingLine,
  scanAtxHeadings: scanAtxHeadings,
  hasDesignSection: hasDesignSection,
  parseDesign: parseDesign,
  parseDesignSources: parseDesignSources,
  parseFigmaUrl: parseFigmaUrl,
  parseScreenKind: parseScreenKind,
  hasPullableDesign: hasPullableDesign,
  hasMalformedDesign: hasMalformedDesign,
  bodyCitesFigmaNode: bodyCitesFigmaNode,
  hasComponentSnapshot: hasComponentSnapshot,
  auditedNoneCount: auditedNoneCount,
  uiTaskWithoutDesign: uiTaskWithoutDesign,
  provenanceNodeForStem: provenanceNodeForStem,
  provenanceSourceTouched: provenanceSourceTouched,
  uiWidgetSourcesTouched: uiWidgetSourcesTouched,
  suggestedDesignBullet: suggestedDesignBullet,
  normalizeNodeId: normalizeNodeId,
  MALFORMED_DESIGN_KINDS: MALFORMED_DESIGN_KINDS
};
