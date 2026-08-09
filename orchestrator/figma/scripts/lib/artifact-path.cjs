'use strict';

var crypto = require('node:crypto');

// Canonical mapping from logical Figma identities (task stem, pipeline run id,
// screen/theme labels) to ONE bounded, path-safe artifact directory segment.
//
// Logical identities stay full-length in reports and manifests. Only the
// filesystem segment is normalized/truncated. Every writer, verifier, cleanup
// path, and server-side artifact reader must use this function so a valid long
// task stem cannot be written under one path and verified under another.
var ARTIFACT_SEGMENT_MAX = 80;
// Keep the full SHA-256 digest. The bounded segment still retains a short human
// prefix, while collision resistance does not depend on an arbitrary truncated
// hash budget.
var ARTIFACT_HASH_LENGTH = 64;
var WINDOWS_DEVICE_SEGMENT_RE = /^(?:con|prn|aux|nul|com[1-9]|lpt[1-9])$/i;

function cleanedSegment(value) {
  return String(value || '')
    .trim()
    .normalize('NFC')
    .replace(/[^A-Za-z0-9_-]+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '');
}

function artifactSegment(value) {
  var raw = String(value || '').trim().normalize('NFC');
  var cleaned = cleanedSegment(raw);
  if (!raw) throw new TypeError('artifact identity must be non-empty');

  // Preserve every already-safe <=80 segment byte-for-byte. A lossy cleanup or
  // truncation receives a digest suffix, otherwise distinct logical ids such as
  // owner.review-1 / owner-review-1 (or two 160-char ids sharing a prefix)
  // alias the same directory and can overwrite/prune each other's evidence.
  if (cleaned === raw && cleaned.length <= ARTIFACT_SEGMENT_MAX &&
      !WINDOWS_DEVICE_SEGMENT_RE.test(cleaned)) return cleaned;

  var digest = crypto.createHash('sha256').update(raw, 'utf8').digest('hex').slice(0, ARTIFACT_HASH_LENGTH);
  var prefixMax = ARTIFACT_SEGMENT_MAX - ARTIFACT_HASH_LENGTH - 1;
  var prefix = cleaned.slice(0, prefixMax).replace(/-+$/g, '') || 'id';
  return prefix + '-' + digest;
}

module.exports = {
  artifactSegment: artifactSegment
};
