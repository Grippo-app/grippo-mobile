// Bounded, deterministic input snapshot over configured adapter roots.
// Shared foundation: enumerates regular files under repository-contained
// roots, applies the capability's include/exclude globs against
// root-relative POSIX paths, hashes bytes, and detects case-folded path
// collisions (REQ-CODE-009). Symlinks anywhere in scope are a typed unsafe
// error, never silently followed or skipped. Enumeration order never affects
// the result: rows are sorted by repository-relative path.
import { lstatSync, opendirSync } from 'node:fs'
import { isAbsolute, join, relative, resolve, sep } from 'node:path'
import { bytesHash, canonicalHash, compareText } from './canonical-json.mjs'
import { compileGlobSet } from './glob.mjs'
import { typedError } from './typed-error.mjs'
import { ADAPTER_ERROR_CODES } from './error-codes.mjs'
import { readContainedSingleLinkFile } from './file-safety.mjs'

const DIR_ENTRIES_MAX = 4000

function unsafe(detail, path) {
  return typedError(ADAPTER_ERROR_CODES.ADAPTER_INPUT_SNAPSHOT_UNSAFE, detail, { path })
}

function limit(detail, path) {
  return typedError(ADAPTER_ERROR_CODES.ADAPTER_INPUT_SNAPSHOT_LIMIT, detail, { path })
}

function toPosix(value) {
  return value.split(sep).join('/')
}

function directoryStamp(stat) {
  return [stat.dev, stat.ino, stat.mtimeNs, stat.ctimeNs].map(String).join(':')
}

function safeDirectoryChain(projectRoot, target) {
  const rel = relative(projectRoot, target)
  const segments = rel ? rel.split(sep) : []
  let current = projectRoot
  let stat = lstatSync(current, { bigint: true })
  if (!stat.isDirectory() || stat.isSymbolicLink()) throw new Error('project root is not a safe directory')
  for (const segment of segments) {
    current = join(current, segment)
    stat = lstatSync(current, { bigint: true })
    if (!stat.isDirectory() || stat.isSymbolicLink()) throw new Error('configured root has a symlink or non-directory ancestor')
  }
  return stat
}

// options: { projectRoot, roots: [repoRelative...], include: [glob...],
//            exclude: [glob...], limits: { filesMax, fileBytesMax, totalBytesMax },
//            keepText?: boolean }
// Returns { files: [{path, size, hash, text?}], rootsMissing: [...], fingerprint }.
// The fingerprint never includes text — keepText only saves the extractor a
// re-read of the exact bytes that were hashed.
export function takeInputSnapshot(options) {
  const projectRoot = resolve(options.projectRoot)
  const includes = compileGlobSet(options.include)
  const excludes = compileGlobSet(options.exclude || [])
  // A `<prefix>/**` exclude also prunes the whole `<prefix>` directory, so an
  // excluded build tree can never trip enumeration limits it was excluded
  // from. Pruning is derived only from the declared excludes — never invented.
  const excludeDirs = compileGlobSet((options.exclude || [])
    .filter((pattern) => pattern.endsWith('/**'))
    .map((pattern) => pattern.slice(0, -3)))
  const limits = options.limits
  const rows = []
  const rootsMissing = []
  const caseFolded = new Map()
  let totalBytes = 0

  for (const root of options.roots) {
    if (typeof root !== 'string' || !root || root.includes('\\') || isAbsolute(root)) {
      throw unsafe('root must be a repository-relative POSIX path', root)
    }
    const absoluteRoot = resolve(projectRoot, root)
    if (absoluteRoot !== projectRoot && !absoluteRoot.startsWith(projectRoot + sep)) {
      throw unsafe('root escapes the repository', root)
    }
    let rootStat
    try { rootStat = safeDirectoryChain(projectRoot, absoluteRoot) } catch (error) {
      if (error && error.code === 'ENOENT') { rootsMissing.push(root); continue }
      throw unsafe(`root unreadable: ${error.code || error.message}`, root)
    }
    if (rootStat.isSymbolicLink()) throw unsafe('root is a symlink', root)
    if (!rootStat.isDirectory()) throw unsafe('root is not a directory', root)
    walk(absoluteRoot, root)
  }

  function walk(absoluteDir, repoRelativeDir) {
    let before
    let names
    try {
      before = lstatSync(absoluteDir, { bigint: true })
      if (!before.isDirectory() || before.isSymbolicLink()) throw new Error('directory is not safe')
      names = []
      const directory = opendirSync(absoluteDir)
      try {
        let entry
        while ((entry = directory.readSync()) !== null) {
          names.push(entry.name)
          if (names.length > DIR_ENTRIES_MAX) {
            const overflow = new Error(`directory lists more than ${DIR_ENTRIES_MAX} entries`)
            overflow.snapshotLimit = true
            throw overflow
          }
        }
      } finally {
        try { directory.closeSync() } catch (error) {}
      }
      const after = lstatSync(absoluteDir, { bigint: true })
      if (!after.isDirectory() || after.isSymbolicLink() || directoryStamp(before) !== directoryStamp(after)) {
        throw new Error('directory changed while enumerating')
      }
    } catch (error) {
      if (error && error.snapshotLimit) throw limit(error.message, repoRelativeDir)
      throw unsafe(`directory unreadable: ${error.code || error.message}`, repoRelativeDir)
    }
    names.sort()
    for (const name of names) {
      const absolute = join(absoluteDir, name)
      const repoRelative = repoRelativeDir + '/' + name
      let stat
      try { stat = lstatSync(absolute) } catch (error) {
        throw unsafe(`entry unreadable: ${error.code || error.message}`, repoRelative)
      }
      if (stat.isSymbolicLink()) throw unsafe('symlink in configured scope', repoRelative)
      if (stat.isDirectory()) {
        if (!excludeDirs(toPosix(repoRelative))) walk(absolute, repoRelative)
        continue
      }
      if (!stat.isFile()) continue
      if (stat.nlink !== 1) throw unsafe('source file is not single-linked', repoRelative)
      // NFC is the one durable path spelling. This prevents macOS-decomposed
      // names and Linux NFC names from producing different artifact hashes.
      const actualRootRelative = toPosix(repoRelative)
      const rootRelative = actualRootRelative.normalize('NFC')
      if (!includes(rootRelative) || excludes(rootRelative)) continue
      if (stat.size > limits.fileBytesMax) throw limit(`file exceeds ${limits.fileBytesMax} bytes`, repoRelative)
      if (rows.length + 1 > limits.filesMax) throw limit(`scope matches more than ${limits.filesMax} files`)
      totalBytes += stat.size
      if (totalBytes > limits.totalBytesMax) throw limit(`scope exceeds ${limits.totalBytesMax} total bytes`)
      const folded = rootRelative.toLowerCase()
      const priorPath = caseFolded.get(folded)
      if (priorPath && priorPath.actual !== actualRootRelative) {
        throw unsafe(`case/normalization-folded path collision between ${priorPath.normalized} and ${rootRelative}`)
      }
      caseFolded.set(folded, { actual: actualRootRelative, normalized: rootRelative })
      let bytes
      try { bytes = readContainedSingleLinkFile({
        root: projectRoot, file: absolute, maxBytes: limits.fileBytesMax
      }) } catch (error) {
        throw unsafe(`file unreadable: ${error.code || error.message}`, repoRelative)
      }
      if (bytes.length !== stat.size) throw unsafe('file changed size during snapshot', repoRelative)
      const row = { path: rootRelative, size: bytes.length, hash: bytesHash(bytes) }
      if (options.keepText) row.text = bytes.toString('utf8')
      rows.push(row)
    }
  }

  rows.sort((a, b) => compareText(a.path, b.path))
  rootsMissing.sort(compareText)
  return {
    files: rows,
    rootsMissing,
    fingerprint: canonicalHash({
      files: rows.map((row) => ({ path: row.path, size: row.size, hash: row.hash })),
      rootsMissing
    })
  }
}
