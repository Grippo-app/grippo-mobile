'use strict';

// The canonical task lifecycle depends on POSIX dir-fd/openat semantics,
// durable directory fsync, and no-clobber rename/link primitives. WSL reports
// `linux` and is supported; native Windows must fail before any lifecycle
// mutation until an equivalent Win32 handle boundary exists end to end.

const SUPPORTED = Object.freeze(['linux', 'darwin']);

class PlatformUnsupportedError extends Error {
  constructor(platform) {
    super(`canonical task lifecycle is unsupported on native ${platform}; use Linux, macOS, or WSL`);
    this.name = 'PlatformUnsupportedError';
    this.code = 'PLATFORM_UNSUPPORTED';
    this.exitCode = 3;
    this.platform = String(platform || 'unknown').slice(0, 40);
  }
}

function assertCanonicalTaskPlatform(platform = process.platform) {
  if (!SUPPORTED.includes(platform)) throw new PlatformUnsupportedError(platform);
  return platform;
}

module.exports = Object.freeze({ SUPPORTED, PlatformUnsupportedError, assertCanonicalTaskPlatform });
