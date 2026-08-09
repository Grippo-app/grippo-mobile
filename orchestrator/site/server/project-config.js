'use strict';

// ---------------------------------------------------------------------------
// Reverse-map orchestrator/project-config.md back
// into the Setup-form shape.
//
// The Setup form's values live ONLY in the gitignored orchestrator/.cache/site/.site-state.json
// (persistence.setupForm). The populated, committed config —
// project-config.md, the documented "single source of truth" — carries the
// same values. deriveState() uses the committed config as the stable base and
// overlays the current editable Setup draft. Reviewer and Figma integration
// fields are then projected from their guarded canonical config readers.
//
// This is the inverse of helpers.buildYaml() in scripts/data/wizard-steps.js —
// keep the two in sync. Mapping (config key -> form field):
//   productName        -> productName
//   productPackage     -> orgName            (com.<org>.<product> -> <org>)
//   backendHost        -> backendHost
//   applicationId      -> applicationId
//   iosFrameworkName   -> iosFrameworkName
//   typefaceFactory    -> typefaceFactory
//   supportedLocales   -> supportedLocales
//   diHandWrittenModules -> authMethods      (GoogleAuthModule->google, AppleAuthModule->apple)
//   iosEnabled / firebaseEnabled / prelaunch -> booleans
//   figmaEnabled       -> figmaEnabled       (boolean; no Setup control — post-bootstrap edits must survive a reset)
//   figmaLibraryUrl     -> figmaLibraryUrl ('' while the placeholder is unfilled)
//
// Two form fields are intentionally NOT recoverable and stay at their defaults:
//   - firstDomain  — a Step-8-only input, never serialized into the config.
//   - the 'email-password' auth method — it produces no hand-written Koin
//     module, so it leaves no trace in diHandWrittenModules. The YAML the form
//     re-emits is identical with or without it, so this round-trips losslessly
//     for everything that actually reaches the config.
// verifyEnabled / backendContractEnabled are not mapped at all: buildYaml pins
// both to `auto` by design, so there is nothing to recover.
// ---------------------------------------------------------------------------

var path       = require('path');
var paths      = require('./paths');
var fsutil     = require('./fsutil');
var validators = require('./validators');

var PLACEHOLDER_RE   = validators.PLACEHOLDER_RE;

// Slice out the YAML frontmatter (between the first two `---` fences). Mirrors
// validateYamlConfigPopulated()'s normalization so a CRLF/BOM-authored config
// parses the same way the gate accepts it.
function extractFrontmatter(text) {
  text = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n').replace(/^\uFEFF/, '');
  if (text.indexOf('---') !== 0) return null;
  var end = text.indexOf('\n---\n', 3);
  if (end < 0) {
    // Accept a closing fence at EOF (no trailing newline).
    var tail = text.search(/\n---\s*$/);
    if (tail < 3) return null;
    end = tail;
  }
  return text.substring(3, end);
}

// Value of a `key: value` line (rest of the line, trimmed), or null. Keys here
// are fixed literals with no regex metacharacters, so interpolation is safe.
function scalar(fm, key) {
  var m = fm.match(new RegExp('^' + key + ':[ \\t]*(.*)$', 'm'));
  return m ? m[1].trim() : null;
}

function bool(fm, key, dflt) {
  var v = scalar(fm, key);
  if (v === null || v === '') return dflt;
  return v === 'true';
}

// Scalar for optional fields that buildYaml emits as `<placeholder>` while
// unset: the placeholder maps back to '' so setup/form state never carries
// placeholder text (buildYaml re-emits the placeholder for '').
function optionalScalar(fm, key) {
  var v = scalar(fm, key);
  if (v === null || /^<[^>]+>$/.test(v)) return '';
  return v;
}

// Parse an inline YAML flow array: "[a, b, c]" -> ['a','b','c']; "[]" -> [].
function parseInlineArray(s) {
  var inner = String(s).replace(/^\[/, '').replace(/\]$/, '').trim();
  if (!inner) return [];
  return inner.split(',').map(function (x) { return x.trim(); }).filter(Boolean);
}

// supportedLocales is emitted as a block list:
//   supportedLocales:
//     - en
//     - ru
// but tolerate the inline flow form ("supportedLocales: [en, ru]") too.
function parseLocales(fm) {
  var lines = fm.split('\n');
  var out = [];
  var inList = false;
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i];
    if (!inList) {
      if (!/^supportedLocales:/.test(line)) continue;
      var inline = line.replace(/^supportedLocales:/, '').trim();
      if (inline.indexOf('[') === 0) return parseInlineArray(inline);
      inList = true;
      continue;
    }
    var item = line.match(/^[ \t]+-[ \t]*(.+?)[ \t]*$/);
    if (item) { out.push(item[1]); continue; }
    if (line.trim() === '') continue;   // tolerate a blank line inside the block
    break;                              // first non-indented, non-item line ends it
  }
  return out;
}

// Recover orgName from productPackage. buildYaml emits `com.<org>.<product>`
// when orgName is set, else `com.<product>`, with product = productName.toLowerCase().
// Anchor on that suffix so multi-segment orgs (com.a.b.product) round-trip.
function orgFromPackage(pkg, productName) {
  if (!pkg || pkg.indexOf('com.') !== 0) return '';
  var middle  = pkg.slice(4);                         // strip leading "com."
  var product = String(productName || '').toLowerCase();
  if (!product) {
    // No product to anchor on (defensive — callers pass a non-empty name).
    var segs = middle.split('.');
    return segs.length >= 2 ? segs[0] : '';
  }
  if (middle === product) return '';                  // com.<product> -> no org
  var tail = '.' + product;
  if (middle.length > tail.length && middle.slice(-tail.length) === tail) {
    return middle.slice(0, middle.length - tail.length);   // com.<org>.<product>
  }
  return '';
}

function authFromModules(mods) {
  var out = [];
  for (var i = 0; i < mods.length; i++) {
    if (mods[i] === 'GoogleAuthModule') out.push('google');
    else if (mods[i] === 'AppleAuthModule') out.push('apple');
  }
  return out;
}

// Returns a Setup-form object reconstructed from the committed config, or null
// when there is nothing usable to prefill (file missing, no frontmatter, still
// holding `<placeholder>` tokens, or no productName). Returning null on a
// placeholder/fresh config is what keeps "<Product>" etc. out of the form.
function parseConfigForm() {
  var p = paths.PROJECT_CONFIG_FILE;
  var text = fsutil.readUtf8(p);
  if (text === null || text === undefined) return null;

  var fm = extractFrontmatter(text);
  if (fm === null) return null;
  if (PLACEHOLDER_RE.test(fm)) return null;

  var productName = scalar(fm, 'productName');
  if (!productName) return null;

  var locales = parseLocales(fm);
  if (locales.indexOf('en') === -1) locales = ['en'].concat(locales);

  return {
    productName:      productName,
    orgName:          orgFromPackage(scalar(fm, 'productPackage') || '', productName),
    backendHost:      scalar(fm, 'backendHost') || '',
    applicationId:    scalar(fm, 'applicationId') || '',
    iosFrameworkName: scalar(fm, 'iosFrameworkName') || 'shared',
    typefaceFactory:  scalar(fm, 'typefaceFactory') || 'inter',
    firstDomain:      '',
    supportedLocales: locales,
    authMethods:      authFromModules(parseInlineArray(scalar(fm, 'diHandWrittenModules') || '[]')),
    iosEnabled:       bool(fm, 'iosEnabled', true),
    firebaseEnabled:  bool(fm, 'firebaseEnabled', true),
    prelaunch:        bool(fm, 'prelaunch', true),
    figmaEnabled:      bool(fm, 'figmaEnabled', false),
    screenshotPixelGate: (['strict', 'advisory', 'off'].indexOf(scalar(fm, 'screenshotPixelGate')) >= 0 ? scalar(fm, 'screenshotPixelGate') : 'strict'),
    figmaLibraryUrl:   optionalScalar(fm, 'figmaLibraryUrl')
  };
}

module.exports = {
  parseConfigForm: parseConfigForm
};
