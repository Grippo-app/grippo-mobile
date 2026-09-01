'use strict';

// Canonical, side-effect-free task provenance contract. The task-state engine,
// deterministic creators and the site projection all depend on this module so
// Source parsing cannot drift across lifecycle boundaries.

const crypto = require('crypto');
const designParser = require('../figma/scripts/design-parser.cjs');
const apiWorkPackage = require('./api-work-package-contract.cjs');

const HASH_RE = /^sha256:[a-f0-9]{64}$/;
const STEM_RE = /^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/;
const TYPE_RE = /^[a-z][a-z0-9-]{0,63}$/;
const SOURCE_TYPES = Object.freeze({
  manual: Object.freeze(['manual', 'architecture-finding']),
  figma: Object.freeze(['design-finding', 'figma-drift', 'figma-missing-component', 'figma-component-split']),
  api: Object.freeze(['api-change', 'api-mismatch', 'api-work-package']),
  'follow-up': Object.freeze(['outcome-follow-up', 'reviewer-follow-up', 'task-split', 'test-foundation-prerequisite'])
});
const SOURCE_FIELDS = Object.freeze(['kind', 'type', 'ref', 'fingerprint']);

function safeTaskStem(value) {
  if (typeof value !== 'string' || value.length > 120) return false;
  const match = /^TASK_([1-9][0-9]*)_[A-Za-z0-9_]+$/.exec(value);
  return !!match && Number.isSafeInteger(Number(match[1]));
}

function sha256(value) {
  return 'sha256:' + crypto.createHash('sha256')
    .update(Buffer.isBuffer(value) ? value : Buffer.from(String(value), 'utf8')).digest('hex');
}

function exactFields(value, fields) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === fields.slice().sort().join('\0');
}

function validRef(value) {
  if (typeof value !== 'string') return false;
  const normalized = value.normalize('NFC');
  const bytes = Buffer.byteLength(value, 'utf8');
  if (normalized !== value || value.trim() !== value || bytes < 1 || bytes > 256) return false;
  if (/[\x00-\x1f\x7f]/.test(value) || /^(?:[A-Za-z]:[\\/]|\/|\\\\)/.test(value)) return false;
  if (/(?:^|[?&;,\s])(?:access[_-]?token|api[_-]?key|authorization|password|secret)=/i.test(value)) return false;
  try {
    const parsed = new URL(value);
    if (parsed.username || parsed.password) return false;
  } catch (_) {}
  return true;
}

function validate(source) {
  if (!exactFields(source, SOURCE_FIELDS)) return null;
  if (typeof source.kind !== 'string' || typeof source.type !== 'string' ||
      typeof source.fingerprint !== 'string' || !TYPE_RE.test(source.type) ||
      !Object.prototype.hasOwnProperty.call(SOURCE_TYPES, source.kind) ||
      !SOURCE_TYPES[source.kind].includes(source.type) || !validRef(source.ref) ||
      !HASH_RE.test(source.fingerprint)) return null;
  if (source.kind === 'follow-up' && !safeTaskStem(source.ref)) return null;
  return {
    kind: source.kind,
    type: source.type,
    ref: source.ref,
    fingerprint: source.fingerprint
  };
}

function render(source) {
  const clean = validate(source);
  if (!clean) throw Object.assign(new Error('task source is invalid'), { code: 'task-source-invalid' });
  return [
    '## Source',
    '',
    '- Kind: ' + clean.kind,
    '- Type: ' + clean.type,
    '- Ref: ' + clean.ref,
    '- Fingerprint: ' + clean.fingerprint
  ].join('\n');
}

function realSourceHeadings(markdown) {
  return designParser.scanAtxHeadings(markdown, 2).headings.filter((heading) => heading.name === 'Source');
}

function parse(markdown) {
  if (typeof markdown !== 'string') return { present: false, valid: false, error: 'task-source-text-invalid' };
  const scanText = markdown.replace(/^\uFEFF/, '').replace(/\r\n/g, '\n').replace(/\r/g, '\n');
  const headings = realSourceHeadings(scanText);
  if (!headings.length) return { present: false, valid: false, error: 'task-source-missing' };
  if (headings.length !== 1) return { present: true, valid: false, error: 'task-source-duplicate' };
  if (markdown.startsWith('\uFEFF') || markdown.includes('\r')) {
    return { present: true, valid: false, error: 'task-source-malformed' };
  }
  const normalized = markdown;
  // Canonical placement is byte-shaped in the original LF-only text: immediately
  // after the H1 with one blank line, exact bullet order, and one blank line
  // before the next section/content (or EOF).
  const match = /^(#[^\n]+)\n\n(## Source\n\n- Kind: ([^\n]+)\n- Type: ([^\n]+)\n- Ref: ([^\n]+)\n- Fingerprint: (sha256:[a-f0-9]{64}))(?=\n\n|\n?$)/.exec(normalized);
  if (!match || headings[0].start !== match[1].length + 2) {
    return { present: true, valid: false, error: 'task-source-malformed' };
  }
  const source = validate({ kind: match[3], type: match[4], ref: match[5], fingerprint: match[6] });
  if (!source) return { present: true, valid: false, error: 'task-source-invalid' };
  const result = {
    present: true,
    valid: true,
    source,
    block: match[2],
    start: match[1].length + 2,
    end: match[1].length + 2 + match[2].length
  };
  if (source.kind === 'api' && source.type === 'api-work-package') {
    const metadata = apiWorkPackage.parse(markdown);
    if (!metadata.valid ||
        metadata.start !== result.end + 2 ||
        source.ref !== 'api:package:' + metadata.value.packageId) {
      return {
        present: true,
        valid: false,
        error: 'task-source-api-package-invalid'
      };
    }
    result.package = metadata;
  }
  return result;
}

function injectBody(body, source) {
  const normalized = String(body == null ? '' : body)
    .replace(/\r\n/g, '\n').replace(/\r/g, '\n').replace(/^\n+/, '');
  if (realSourceHeadings(normalized).length) {
    throw Object.assign(new Error('task body already contains a Source section'), { code: 'task-source-conflict' });
  }
  const block = render(source);
  return normalized ? block + '\n\n' + normalized : block;
}

function same(left, right) {
  const a = validate(left), b = validate(right);
  return !!a && !!b && SOURCE_FIELDS.every((field) => a[field] === b[field]);
}

function manualForIntent(intentId, type, ref, fingerprint) {
  const cleanType = type || 'manual';
  const cleanRef = ref || intentId || 'none';
  const source = {
    kind: 'manual', type: cleanType, ref: cleanRef,
    fingerprint: fingerprint || sha256('task-source-v1\0manual\0' + cleanType + '\0' + cleanRef + '\0' + String(intentId || cleanRef))
  };
  if (!validate(source)) throw Object.assign(new Error('manual task source is invalid'), { code: 'task-source-invalid' });
  return source;
}

function followUp(parentStem, type, stableItemId) {
  const source = {
    kind: 'follow-up', type: type || 'task-split', ref: parentStem,
    fingerprint: sha256('task-source-v1\0follow-up\0' + (type || 'task-split') + '\0' + parentStem + '\0' + String(stableItemId || parentStem))
  };
  if (!validate(source)) throw Object.assign(new Error('follow-up task source is invalid'), { code: 'task-source-invalid' });
  return source;
}

// Special contract factory for the globally deduplicated test-foundation
// prerequisite child (§10.3 of the mandatory-test plan). Ref is the FIRST
// parent stem; the stable item id is the exact foundationIntentHash hex, so
// every concurrent prep that computed the same absent-foundation intent
// derives byte-identical Source provenance in the existing task-source-v1
// domain — the creator's global idempotency key reuses the same hex.
function testFoundationPrerequisite(parentStem, foundationIntentHash) {
  const match = /^sha256:([0-9a-f]{64})$/.exec(String(foundationIntentHash || ''));
  if (!match) throw Object.assign(new Error('foundation intent hash grammar'), { code: 'task-source-invalid' });
  return followUp(parentStem, 'test-foundation-prerequisite', match[1]);
}

module.exports = Object.freeze({
  HASH_RE,
  STEM_RE,
  TYPE_RE,
  SOURCE_TYPES,
  SOURCE_FIELDS,
  safeTaskStem,
  sha256,
  validRef,
  validate,
  render,
  realSourceHeadings,
  parse,
  injectBody,
  same,
  manualForIntent,
  followUp,
  testFoundationPrerequisite
});
