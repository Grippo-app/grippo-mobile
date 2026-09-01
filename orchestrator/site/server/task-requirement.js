'use strict';

// Canonical Task Details source adapter. It reads task bytes only through the
// task-state engine's bounded snapshot and projects human-facing sections
// without turning Markdown heuristics into a second lifecycle parser.

var crypto = require('crypto');
var taskIntegrity = require('./task-integrity');
var taskSource = require('../../tasks/task-source-contract.cjs');
var apiWorkPackage = require('../../tasks/api-work-package-contract.cjs');
var designParser = require('../../figma/scripts/design-parser.cjs');
var core = require('../../tasks/task-state-core.cjs');

var TEXT_MAX = 8000;
// Deliberately loose. The canonical parser reads the CommonMark structural
// view, where masking only replaces characters with spaces — so a heading it
// names `Questions` always keeps `##` and that literal word on the same raw
// line. A tighter literal would let the two disagree about the rail existing.
var QUESTIONS_HEADING_PREFILTER = new RegExp('^[ \\t]{0,3}##.*' + core.TASK_QUESTIONS_SECTION, 'm');
var LIST_MAX = 100;

function bounded(value, maximum) {
  var text = String(value == null ? '' : value).replace(/\0/g, '').trim();
  return text.length <= maximum ? text : text.slice(0, maximum - 1) + '…';
}

function hash(value) {
  return 'sha256:' + crypto.createHash('sha256').update(String(value || ''), 'utf8').digest('hex');
}

function primaryRecord(group, state) {
  if (!group) return null;
  if (state === 'pending') return group.backlog || null;
  return group[state] || null;
}

function load(stem, options) {
  options = options || {};
  if (!taskSource.safeTaskStem(stem)) return null;
  var validation = options.validation || taskIntegrity.validateAllCached();
  var model = validation && validation._model;
  var metadata = model && model.metadata && model.metadata.get(stem);
  var group = model && model.artifacts && model.artifacts.get(stem);
  if (!metadata || !group) return null;
  var record = primaryRecord(group, metadata.state);
  if (!record || !record.readable || typeof record.text !== 'string') return null;
  var questions = metadata.state === 'pending' && group.pending &&
    group.pending.readable && typeof group.pending.text === 'string'
    ? group.pending : null;
  // A running task cannot own a pending sidecar (the pair classifies as
  // corrupt), so its escalation questions live in a `## Questions` section of
  // the todo body. Both rails project the same questions DTO downstream.
  var taskQuestions = metadata.state === 'todo' ? core.parseTaskQuestions(record.text) : null;
  var taskQuestionsRaw = taskQuestions && taskQuestions.present
    ? record.text.slice(taskQuestions.sectionStart, taskQuestions.sectionEnd) : null;
  return {
    stem: stem,
    validation: validation,
    metadata: metadata,
    group: group,
    record: record,
    raw: record.text,
    bodyRevision: record.contentHash,
    questionsRecord: questions,
    questionsRaw: questions ? questions.text : null,
    questionsRevision: questions ? questions.contentHash : null,
    taskQuestions: taskQuestions,
    taskQuestionsRaw: taskQuestionsRaw,
    taskQuestionsRevision: taskQuestionsRaw === null ? null : hash(taskQuestionsRaw)
  };
}

// Cheap projection for the board read model: a heading prefilter keeps the
// section parser off every card that has no question section at all.
function questionsState(text) {
  var raw = typeof text === 'string' ? text : '';
  if (!QUESTIONS_HEADING_PREFILTER.test(raw)) {
    return { present: false, valid: false, total: 0, unanswered: 0 };
  }
  var parsed = core.parseTaskQuestions(raw);
  var valid = !core.taskQuestionsIssue(parsed);
  return {
    present: parsed.present,
    valid: valid,
    total: parsed.questions.length,
    unanswered: valid ? parsed.questions.filter(function (item) { return !item.answer; }).length : 0
  };
}

function h2Sections(text) {
  var source = String(text || '');
  var outcomeStart = core.outcomeAppendixStart(source);
  if (outcomeStart >= 0) source = source.slice(0, outcomeStart);
  var scanned = core.scanAtxHeadings(source, 2);
  var headings = scanned.headings || [];
  return headings.map(function (heading, index) {
    return {
      name: String(heading.name || '').trim().toLowerCase(),
      label: String(heading.name || '').trim(),
      body: source.slice(heading.headEnd, headings[index + 1] ? headings[index + 1].start : source.length).trim()
    };
  });
}

function section(sections, name) {
  var matches = sections.filter(function (item) { return item.name === name; });
  return matches.length === 1 ? matches[0].body : '';
}

function bullets(value, maximum) {
  var out = [];
  String(value || '').split('\n').forEach(function (line) {
    var match = /^[ \t]{0,3}[-*+][ \t]+(.+?)[ \t]*$/.exec(line);
    if (match && out.length < (maximum || LIST_MAX)) out.push(bounded(match[1], 1000));
  });
  return out;
}

function acceptance(value) {
  var text = String(value || '');
  var scanned = core.scanAtxHeadings(text, 3);
  var headings = scanned.headings || [];
  var result = { automated: [], manual: [] };
  if (!headings.length) {
    result.automated = bullets(text);
    return result;
  }
  headings.forEach(function (heading, index) {
    var name = String(heading.name || '').trim().toLowerCase();
    var rows = bullets(text.slice(heading.headEnd, headings[index + 1] ? headings[index + 1].start : text.length));
    if (name === 'automated') result.automated = rows;
    if (name === 'manual') result.manual = rows.filter(function (item) { return item.toLowerCase() !== 'none'; });
  });
  return result;
}

function sources(source) {
  var rows = [];
  if (source.metadata.origin) {
    rows.push({
      id: 'origin-' + hash([
        source.metadata.origin.kind,
        source.metadata.origin.type,
        source.metadata.origin.ref
      ].join('\0')).slice(7, 31),
      kind: source.metadata.origin.kind,
      type: source.metadata.origin.type,
      ref: bounded(source.metadata.origin.ref, 500),
      label: bounded(source.metadata.origin.ref, 500),
      target: null,
      metadata: {}
    });
  }

  // Design is a typed task contract, not free-form Markdown. Reusing the
  // producer/parser keeps invalid or unaudited values from becoming links.
  var design = designParser.parseDesign(source.raw);
  var designCount = 0;
  (design.entries || []).slice(0, 30).forEach(function (entry) {
    if (!entry || entry.none) return;
    Object.keys(entry.themes || {}).sort().forEach(function (theme) {
      if (designCount >= 30) return;
      var node = entry.themes[theme];
      if (!node || !node.ok) return;
      designCount++;
      rows.push({
        id: 'design-' + hash([
          entry.screen, entry.kind, theme, node.fileKey, node.nodeId
        ].join('\0')).slice(7, 31),
        kind: 'figma-node',
        type: 'design-entry',
        ref: bounded(node.url, 500),
        label: bounded([
          entry.screen,
          entry.kind,
          theme,
          node.nodeId
        ].filter(Boolean).join(' · '), 500),
        target: { panel: 'figma', entityId: null },
        metadata: {
          screen: bounded(entry.screen, 200),
          designKind: bounded(entry.kind, 50),
          theme: bounded(theme, 50),
          fileKey: bounded(node.fileKey, 200),
          nodeId: bounded(node.nodeId, 100)
        }
      });
    });
  });

  // API packages are canonical JSON blocks. Their source ids are the durable
  // relation to the generation-bound API catalog; never infer endpoints from
  // display prose in the task body.
  var apiPackage = apiWorkPackage.parse(source.raw);
  if (apiPackage.valid) {
    apiPackage.value.sourceIds.forEach(function (sourceId) {
      rows.push({
        id: 'api-' + hash(sourceId).slice(7, 31),
        kind: 'api',
        type: sourceId.indexOf('api:change:') === 0 ? 'api-change' :
          'api-mismatch',
        ref: bounded(sourceId, 500),
        label: bounded(sourceId, 500),
        target: { panel: 'api', entityId: sourceId },
        metadata: {
          packageId: apiPackage.value.packageId,
          groupKey: apiPackage.value.groupKey
        }
      });
    });
  }
  return rows.slice(0, 60);
}

function requirement(source) {
  var sections = h2Sections(source.raw);
  return {
    goal: bounded(section(sections, 'goal') || source.metadata.title, TEXT_MAX),
    inputs: bullets(section(sections, 'inputs')),
    acceptance: acceptance(section(sections, 'acceptance')),
    outOfScope: bullets(section(sections, 'out of scope')),
    sources: sources(source),
    partial: !section(sections, 'goal') || !section(sections, 'acceptance')
  };
}

function optionRows(body) {
  var rows = [];
  String(body || '').split('\n').forEach(function (line) {
    var match = /^[ \t]*[-*+][ \t]+\(([A-Za-z0-9_-]{1,40})\)[ \t]+(.+?)[ \t]*$/.exec(line);
    if (match && rows.length < 30) rows.push({ id: match[1], label: bounded(match[2], 500) });
  });
  return rows;
}

function pendingQuestionsValid(source, parsed, rows) {
  var findings = source.validation && Array.isArray(source.validation.findings)
    ? source.validation.findings : [];
  var canonicalIssue = findings.some(function (item) {
    return item && item.stem === source.stem &&
      (item.severity === 'error' || item.severity === 'blocker') &&
      typeof item.code === 'string' && item.code.indexOf('PENDING_') === 0;
  });
  return !canonicalIssue && parsed.errors.length === 0 &&
    parsed.duplicateFields.length === 0 &&
    parsed.questions.length === rows.length;
}

function taskQuestionsValid(source, parsed) {
  var findings = source.validation && Array.isArray(source.validation.findings)
    ? source.validation.findings : [];
  var canonicalIssue = findings.some(function (item) {
    return item && item.stem === source.stem &&
      (item.severity === 'error' || item.severity === 'blocker') &&
      typeof item.code === 'string' &&
      (item.code.indexOf('TODO_') === 0 || item.code.indexOf('TASK_') === 0);
  });
  return !canonicalIssue && !core.taskQuestionsIssue(parsed);
}

function taskQuestions(source) {
  var parsed = source.taskQuestions;
  var rows = parsed.questions.slice(0, core.MAX_TASK_QUESTIONS).map(function (item) {
    var type = item.types.length === 1 ? item.types[0] : 'text';
    var declaredOptions = item.options.length === 1
      ? item.options[0].split(',').map(function (value) { return value.trim(); }).filter(Boolean)
      : [];
    var labels = Object.create(null);
    // Structural view only: a fenced or HTML-masked bullet must never be able
    // to relabel a real option the owner is about to choose.
    optionRows(core.structuralText(source.raw).slice(item.blockStart, item.blockEnd)).forEach(function (option) {
      labels[option.id] = option.label;
    });
    return {
      id: item.id,
      text: bounded(item.title, 1000),
      type: core.TASK_QUESTION_TYPES.indexOf(type) >= 0 ? type : 'text',
      options: type === 'choice' || type === 'multiselect'
        ? declaredOptions.map(function (id) { return { id: id, label: labels[id] || id }; })
        : [],
      answer: bounded(item.answer, 4000)
    };
  });
  return {
    // The in-body rail has no frontmatter bookkeeping: the round is the number
    // of question blocks published so far, and the section hash is the fence
    // that actually rejects a stale submission.
    round: Math.max(1, rows.length),
    revision: source.taskQuestionsRevision,
    valid: taskQuestionsValid(source, parsed),
    questions: rows
  };
}

function questions(source) {
  if (!source.questionsRaw) {
    return source.taskQuestionsRaw ? taskQuestions(source) : null;
  }
  var parsed = core.parsePending(source.questionsRaw);
  var structural = core.structuralText(source.questionsRaw);
  var matches = [];
  var regex = /^##[ \t]+Q([0-9]+)[ \t]+[—-][ \t]+(.+?)[ \t]*$/gm;
  var match;
  while ((match = regex.exec(structural)) !== null) {
    matches.push({ id: Number(match[1]), title: bounded(match[2], 1000), start: match.index, bodyStart: regex.lastIndex });
  }
  var byId = Object.create(null);
  (parsed.questions || []).forEach(function (item) { byId[item.id] = item; });
  var rows = matches.slice(0, 99).map(function (item, index) {
    var body = structural.slice(item.bodyStart, matches[index + 1] ? matches[index + 1].start : structural.length);
    var meta = byId[item.id] || {};
    var type = meta.types && meta.types.length === 1 ? meta.types[0] : 'text';
    var declaredOptions = meta.options && meta.options.length === 1
      ? meta.options[0].split(',').map(function (value) { return value.trim(); }).filter(Boolean)
      : [];
    var labels = Object.create(null);
    optionRows(body).forEach(function (option) { labels[option.id] = option.label; });
    var answerMarker = /^###[ \t]+Answer[ \t]*$/m.exec(body);
    var answer = answerMarker ? body.slice(answerMarker.index + answerMarker[0].length).trim() : '';
    return {
      id: item.id,
      text: item.title,
      type: ['text', 'choice', 'multiselect'].indexOf(type) >= 0 ? type : 'text',
      options: type === 'choice' || type === 'multiselect'
        ? declaredOptions.map(function (id) {
          return { id: id, label: labels[id] || id };
        }) : [],
      answer: bounded(answer, 4000)
    };
  });
  return {
    round: Number(parsed.fields && parsed.fields.round) || 1,
    revision: source.questionsRevision || hash(source.questionsRaw),
    valid: pendingQuestionsValid(source, parsed, rows),
    questions: rows
  };
}

function outcome(source) {
  var parsed = source.metadata.outcome;
  if (!parsed) return {
    present: false,
    valid: source.metadata.state !== 'done',
    status: source.metadata.state === 'done' ? 'missing' : 'in-progress',
    completedAt: null,
    reviewer: null,
    acceptance: [],
    caveats: [],
    followUps: [],
    buildGates: [],
    runtimeVerify: [],
    executionLog: [],
    files: [],
    errors: source.metadata.state === 'done' ? ['outcome-missing'] : []
  };
  return {
    present: true,
    valid: parsed.valid === true,
    status: parsed.status,
    completedAt: parsed.completedAt || null,
    reviewer: parsed.reviewer || null,
    acceptance: bullets(parsed.sections && parsed.sections['Acceptance trace']),
    caveats: bullets(parsed.sections && parsed.sections.Caveats),
    followUps: bullets(parsed.sections && parsed.sections['Follow-ups']),
    buildGates: bullets(parsed.sections && parsed.sections['Build gates']),
    runtimeVerify: bullets(parsed.sections && parsed.sections['Runtime verify']),
    executionLog: bullets(parsed.sections && parsed.sections['Execution log']),
    files: (parsed.files || []).slice(0, LIST_MAX).map(function (file) {
      var escaped = String(file).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      var row = bullets(parsed.sections && parsed.sections['Files touched']).find(function (item) {
        return new RegExp('^`' + escaped + '`[ \\t]+—[ \\t]+[a-z]+$').test(item);
      });
      var match = row && /[ \t]+—[ \t]+([a-z]+)$/.exec(row);
      return { path: bounded(file, 1000), change: match ? match[1] : 'modified' };
    }),
    errors: (parsed.errors || []).slice(0, 30)
  };
}

function currentWork(source, row, intake) {
  if (source.metadata.state === 'pending') {
    return { kind: 'questions', questions: questions(source) };
  }
  if (row && row.runtimeStatus && row.runtimeStatus.state === 'awaiting') {
    return {
      kind: 'awaiting-user',
      sessionKey: row.runtimeStatus.sessionKey || null,
      sessionId: row.primaryAction && row.primaryAction.liveSessionId || null,
      sessionRevision: row.primaryAction && row.primaryAction.expectedSessionRevision || null
    };
  }
  // A durable escalation question outranks the generic next-action projection
  // and a lock-only "running" reading, but never a real session: answering the
  // paused turn stays the faster rail. Without a session record the lock alone
  // still reports active, and hiding the form behind it would strand the task.
  var unanswered = null;
  if (source.metadata.state === 'todo' && source.taskQuestionsRaw) {
    var work = taskQuestions(source);
    if (work.valid && work.questions.some(function (item) { return !item.answer; })) unanswered = work;
  }
  // Whenever the resolver offers the answer CTA the form must be here to serve
  // it, even while a queued request or a held lock reads as active. Deriving
  // this from the chosen action keeps the card and the modal from disagreeing.
  var answerCta = !!(row && row.primaryAction && row.primaryAction.kind === 'submit-answers');
  var active = !!(row && row.runtimeStatus && row.runtimeStatus.active);
  if (unanswered && (answerCta || !active)) return { kind: 'questions', questions: unanswered };
  if (active) return { kind: 'running', phase: row.runtimeStatus.phase || null };
  if (source.metadata.state === 'backlog' && intake) {
    return { kind: 'intake', intake: intake };
  }
  return { kind: source.metadata.state === 'done' ? 'result' : 'next-action' };
}

module.exports = Object.freeze({
  load: load,
  h2Sections: h2Sections,
  bullets: bullets,
  acceptance: acceptance,
  requirement: requirement,
  questions: questions,
  questionsState: questionsState,
  outcome: outcome,
  currentWork: currentWork,
  bounded: bounded
});
