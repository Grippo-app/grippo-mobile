import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { TextDecoder } from 'node:util'
import { PROJECT_ROOT } from '../_util.mjs'
import { readContainedSingleLinkFile } from '../../runtime/file-safety.mjs'

const TASK_MARKDOWN_MAX_BYTES = 128 * 1024
const utf8 = new TextDecoder('utf-8', { fatal: true })

// Exact task-body reader shared by the screen-cache and final-evidence gates.
// Production task paths stay anchored to PROJECT_ROOT. An explicitly supplied
// FIGMA_SCREEN_TASK_FILE is fixture/proposal authority and is anchored to its
// own physical parent; "-" is the one bounded stdin form.
export function readTaskMarkdown(file, { explicit = false } = {}) {
  let bytes
  if (file === '-') {
    bytes = readFileSync(0)
    if (bytes.length > TASK_MARKDOWN_MAX_BYTES) {
      throw new Error(`task source exceeds ${TASK_MARKDOWN_MAX_BYTES} bytes`)
    }
  } else {
    const absolute = resolve(file)
    bytes = readContainedSingleLinkFile({
      root: explicit ? dirname(absolute) : PROJECT_ROOT,
      file: absolute,
      maxBytes: TASK_MARKDOWN_MAX_BYTES
    })
  }
  return utf8.decode(bytes)
}
