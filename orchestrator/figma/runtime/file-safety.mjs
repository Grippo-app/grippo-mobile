// Shared fail-closed filesystem reads for the trusted token/component runtime.
// Project files are untrusted input: a lexical containment check is not enough
// because a path may be replaced after lstat(). Open the exact final entry,
// bind it to the witnessed inode, and prove that neither its identity nor its
// content metadata moved while the bytes were read.
import {
  closeSync, constants, fstatSync, lstatSync, openSync, readSync
} from 'node:fs'
import { dirname, isAbsolute, join, relative, resolve, sep } from 'node:path'

function identity(stat) {
  return {
    dev: String(stat.dev),
    ino: String(stat.ino),
    nlink: String(stat.nlink),
    size: String(stat.size),
    mtimeNs: String(stat.mtimeNs),
    ctimeNs: String(stat.ctimeNs)
  }
}

function sameIdentity(left, right, { includeContent = true } = {}) {
  const a = identity(left)
  const b = identity(right)
  return a.dev === b.dev && a.ino === b.ino && a.nlink === b.nlink &&
    (!includeContent || (a.size === b.size && a.mtimeNs === b.mtimeNs && a.ctimeNs === b.ctimeNs))
}

function assertContained(root, file) {
  const canonicalRoot = resolve(root)
  const canonicalFile = resolve(file)
  const rel = relative(canonicalRoot, canonicalFile)
  if (!rel || isAbsolute(rel) || rel === '..' || rel.startsWith('..' + sep)) {
    throw new Error('input must be a direct descendant of its authority root')
  }
  return { canonicalRoot, canonicalFile }
}

function directoryProofs(root, file) {
  const parent = dirname(file)
  const rel = relative(root, parent)
  const segments = rel ? rel.split(sep) : []
  const proofs = []
  let current = root
  for (let index = -1; index < segments.length; index++) {
    if (index >= 0) current = join(current, segments[index])
    const stat = lstatSync(current, { bigint: true })
    if (!stat.isDirectory() || stat.isSymbolicLink()) {
      throw new Error('input has a symlink or non-directory ancestor')
    }
    proofs.push({ path: current, stat })
  }
  return proofs
}

function verifyDirectoryProofs(proofs) {
  for (const proof of proofs) {
    const current = lstatSync(proof.path, { bigint: true })
    if (!current.isDirectory() || current.isSymbolicLink() ||
        !sameIdentity(proof.stat, current, { includeContent: false })) {
      throw new Error('input ancestor identity changed while reading')
    }
  }
}

// Returns Buffer, or null only when allowMissing is true and the final entry
// is absent. Every other unsafe/unreadable state throws without fallback.
export function readContainedSingleLinkFile({ root, file, maxBytes, allowMissing = false }) {
  const { canonicalRoot, canonicalFile } = assertContained(root, file)
  let ancestors
  try { ancestors = directoryProofs(canonicalRoot, canonicalFile) } catch (error) {
    if (allowMissing && error && error.code === 'ENOENT') return null
    throw error
  }
  let before
  try { before = lstatSync(canonicalFile, { bigint: true }) } catch (error) {
    if (allowMissing && error && error.code === 'ENOENT') return null
    throw error
  }
  if (!before.isFile() || before.isSymbolicLink() || before.nlink !== 1n) {
    throw new Error('input is not a single-link regular file')
  }
  if (before.size > BigInt(maxBytes)) throw new Error(`input exceeds ${maxBytes} bytes`)

  let descriptor
  try {
    descriptor = openSync(canonicalFile, constants.O_RDONLY | (constants.O_NOFOLLOW || 0))
    const opened = fstatSync(descriptor, { bigint: true })
    if (!opened.isFile() || opened.nlink !== 1n || !sameIdentity(before, opened)) {
      throw new Error('input identity changed while opening')
    }
    const expectedSize = Number(opened.size)
    const buffer = Buffer.allocUnsafe(expectedSize + 1)
    let length = 0
    while (length < buffer.length) {
      const count = readSync(descriptor, buffer, length, buffer.length - length, null)
      if (count === 0) break
      length += count
    }
    const bytes = buffer.subarray(0, length)
    const afterRead = fstatSync(descriptor, { bigint: true })
    let afterPath
    try { afterPath = lstatSync(canonicalFile, { bigint: true }) } catch (error) {
      throw new Error('input path disappeared while reading')
    }
    if (bytes.length !== expectedSize || bytes.length > maxBytes ||
        !sameIdentity(opened, afterRead) || afterPath.isSymbolicLink() ||
        !sameIdentity(opened, afterPath)) {
      throw new Error('input changed while reading')
    }
    verifyDirectoryProofs(ancestors)
    return bytes
  } finally {
    if (descriptor !== undefined) closeSync(descriptor)
  }
}
