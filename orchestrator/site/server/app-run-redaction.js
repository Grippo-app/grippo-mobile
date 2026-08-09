'use strict';

var os = require('os');

var SECRET_ASSIGNMENT = /\b([A-Za-z0-9_-]{0,64}(?:token|secret|password|passwd|api[_-]?key|private[_-]?key)[A-Za-z0-9_-]{0,64})\s*[:=]\s*(?:"(?:\\.|[^"])*"|'(?:\\.|[^'])*'|[^\s,;]+)/ig;
var SECRET_ARGUMENT = /(^|[\s,;])(--?[A-Za-z0-9_-]{0,64}(?:token|secret|password|passwd|api[_-]?key|private[_-]?key)[A-Za-z0-9_-]{0,64})[ \t]+(?:"(?:\\.|[^"])*"|'(?:\\.|[^'])*'|[^\s,;]+)/ig;
var AUTHORIZATION = /\bauthorization\s*[:=]\s*[^\r\n]*/ig;
var BEARER = /\bBearer\s+[A-Za-z0-9._~+\/=-]+/ig;
var URI_USERINFO = /\b([A-Za-z][A-Za-z0-9+.-]*:\/\/)[^\/\s:@]+:[^\/\s@]+@/g;
var ANSI = /\u001b(?:\[[0-?]*[ -/]*[@-~]|\][^\u0007]*(?:\u0007|\u001b\\))/g;
var ANDROID_EMULATOR_SERIAL = /\bemulator-\d{1,10}\b/ig;
var APPLE_SIMULATOR_UDID = /\b[A-F0-9]{8}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{12}\b/ig;

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function redact(value) {
  var text = String(value === undefined || value === null ? '' : value)
    .replace(ANSI, '')
    .replace(/\0/g, '\uFFFD')
    .replace(AUTHORIZATION, 'Authorization: [REDACTED]')
    .replace(BEARER, 'Bearer [REDACTED]')
    .replace(URI_USERINFO, '$1[REDACTED]@')
    .replace(ANDROID_EMULATOR_SERIAL, '[DEVICE]')
    .replace(APPLE_SIMULATOR_UDID, '[DEVICE]')
    .replace(SECRET_ASSIGNMENT, function (_, key) { return key + '=[REDACTED]'; })
    .replace(SECRET_ARGUMENT, function (_, prefix, key) {
      return prefix + key + ' [REDACTED]';
    });
  var home = os.homedir();
  if (home) text = text.replace(new RegExp(escapeRegExp(home), 'g'), '$HOME');
  return text.length > 16384 ? text.slice(0, 16384) + '…' : text;
}

function line(value) {
  var cleaned = redact(value)
    .replace(/[\r\n]+/g, ' ')
    .replace(/[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]/g, '\uFFFD');
  if (Buffer.byteLength(cleaned, 'utf8') <= 4096) return cleaned;
  var bounded = '', bytes = 0;
  for (var character of cleaned) {
    var width = Buffer.byteLength(character, 'utf8');
    if (bytes + width > 4096) break;
    bounded += character;
    bytes += width;
  }
  return bounded;
}

module.exports = { redact: redact, line: line };
