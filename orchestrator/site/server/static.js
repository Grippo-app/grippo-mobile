'use strict';

// ---------------------------------------------------------------------------
// Tiny static file server (ORCHESTRATOR_DIR as docroot). safeResolve is the
// path-traversal guard — it confines
// every resolved path under the docroot AND denies dot-paths (dotfiles/
// dot-dirs) so secrets under the docroot — orchestrator/figma/.account.json,
// .cache/ (plus .git/.env) — can never be served. The docroot
// has no legitimate dot-path assets.
// ---------------------------------------------------------------------------

var fs   = require('fs');
var path = require('path');

var MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js':   'application/javascript; charset=utf-8',
  '.css':  'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.md':   'text/markdown; charset=utf-8',
  '.svg':  'image/svg+xml',
  '.png':  'image/png',
  '.jpg':  'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.ico':  'image/x-icon',
  '.txt':  'text/plain; charset=utf-8'
};

function safeResolve(root, urlPath) {
  var decoded;
  try { decoded = decodeURIComponent(urlPath); } catch (e) { return null; }
  decoded = decoded.replace(/^\/+/, '');
  // Security denylist: never serve a path with a dot-segment (dotfile or dot-dir).
  // Covers orchestrator/figma/.account.json, .cache/, and .git/.env.
  if (decoded.split('/').some(function (seg) { return seg.charAt(0) === '.'; })) return null;
  // Never raw-serve the committed evidence tree (tasks/evidence/**): ship receipts +
  // frozen matrices embed absolute local filesystem paths (report inputHashes/inputs keys)
  // that the /api/figma/* readers scrub before echoing — a raw static serve would bypass
  // that scrubbing. It is NOT a dot-path (it is committed, on purpose), so the dot-denylist
  // above misses it; nothing legitimate serves from here (the board reads evidence only via
  // /api/figma/evidence + /api/figma/compare-artifact). Kept narrow to the evidence subtree.
  if (/^tasks\/evidence(\/|$)/.test(decoded)) return null;
  var target = path.normalize(path.join(root, decoded));
  // Confine to docroot.
  if (target !== root && !target.startsWith(root + path.sep)) return null;
  return target;
}

function sendBuffer(res, file, buf) {
  var ext = path.extname(file).toLowerCase();
  res.writeHead(200, {
    'content-type': MIME[ext] || 'application/octet-stream',
    'cache-control': 'no-store'
  });
  res.end(buf);
}

function sendFile(res, file) {
  fs.readFile(file, function (err, buf) {
    if (err) { res.writeHead(404); res.end('not found'); return; }
    sendBuffer(res, file, buf);
  });
}

function serveStatic(req, res, target) {
  fs.stat(target, function (err, st) {
    if (err) { res.writeHead(404); res.end('not found'); return; }
    if (st.isDirectory()) {
      var idx = path.join(target, 'index.html');
      fs.stat(idx, function (e2, st2) {
        if (e2 || !st2.isFile()) { res.writeHead(404); res.end('not found'); return; }
        sendFile(res, idx);
      });
      return;
    }
    sendFile(res, target);
  });
}

module.exports = {
  safeResolve: safeResolve,
  serveStatic: serveStatic,
  // Reused by purpose-built readers that serve a single vetted file from a
  // dot-dir safeResolve denies (e.g. figma-screens.js streaming a cached PNG
  // out of .cache/figma/screens/ — the path is whitelisted by that module, not here).
  sendFile: sendFile,
  sendBuffer: sendBuffer
};
