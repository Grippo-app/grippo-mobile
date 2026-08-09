// ESM facade over the single CommonJS owner so both the ESM adapter runtime
// and the CommonJS Site publisher use identical Git/worktree identity rules.
import identity from './project-identity.cjs'

export const projectBranchKey = identity.projectBranchKey
