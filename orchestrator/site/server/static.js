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
  if (decoded.split(/[\\/]/).some(function (seg) { return seg.charAt(0) === '.'; })) return null;
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

function isDeniedRelative(relative) {
  if (!relative || relative === '.') return false;
  if (path.isAbsolute(relative) || relative === '..' || relative.startsWith('..' + path.sep)) return true;
  var segments = relative.split(path.sep);
  if (segments.some(function (seg) { return seg.charAt(0) === '.'; })) return true;
  return segments[0] === 'tasks' && segments[1] === 'evidence';
}

// Resolve both ends before reading. This closes the lexical-safe-but-canonical-
// unsafe case where a harmless-looking path is a symlink to a secret dot-path
// or to a file outside the static root.
function confinedRealpath(root, target, done) {
  fs.realpath(root, function (rootErr, realRoot) {
    if (rootErr) { done(rootErr); return; }
    fs.realpath(target, function (targetErr, realTarget) {
      if (targetErr) { done(targetErr); return; }
      var relative = path.relative(realRoot, realTarget);
      if (isDeniedRelative(relative)) {
        var error = new Error('static target is outside the public surface');
        error.code = 'STATIC_TARGET_DENIED';
        done(error);
        return;
      }
      done(null, realRoot, realTarget);
    });
  });
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

function sendConfinedFile(res, root, file) {
  confinedRealpath(root, file, function (resolveErr, _realRoot, realFile) {
    if (resolveErr) { res.writeHead(404); res.end('not found'); return; }
    var flags = fs.constants.O_RDONLY | (fs.constants.O_NOFOLLOW || 0);
    fs.open(realFile, flags, function (openErr, fd) {
      if (openErr) { res.writeHead(404); res.end('not found'); return; }
      fs.fstat(fd, function (statErr, st) {
        if (statErr || !st.isFile()) {
          fs.close(fd, function () {});
          res.writeHead(404); res.end('not found');
          return;
        }
        fs.readFile(fd, function (readErr, buf) {
          fs.close(fd, function () {});
          if (readErr) { res.writeHead(404); res.end('not found'); return; }
          sendBuffer(res, realFile, buf);
        });
      });
    });
  });
}

function serveStatic(req, res, target, root) {
  confinedRealpath(root, target, function (resolveErr, _realRoot, realTarget) {
    if (resolveErr) { res.writeHead(404); res.end('not found'); return; }
    fs.stat(realTarget, function (err, st) {
    if (err) { res.writeHead(404); res.end('not found'); return; }
    if (st.isDirectory()) {
      sendConfinedFile(res, root, path.join(realTarget, 'index.html'));
      return;
    }
    sendConfinedFile(res, root, realTarget);
    });
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
