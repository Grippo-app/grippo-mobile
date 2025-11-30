package com.grippo.data.features.suggestions.prompt.exercise.example.sections

import com.grippo.data.features.suggestions.prompt.exercise.example.utils.formatOneDecimal
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.formatTitleLabel
import com.grippo.database.dao.UserActiveDao
import com.grippo.database.dao.UserDao
import com.grippo.database.entity.UserEntity
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.annotation.Single

@Single
internal class SystemSection(
    private val userDao: UserDao,
    private val userActiveDao: UserActiveDao,
) {

    suspend fun build(): String {
        val profile = loadUserProfile() ?: return BASE_PROMPT

        return buildString {
            append(BASE_PROMPT)
            appendLine()
            appendLine()
            appendLine("User profile (from UserDao):")
            appendLine(" - name: ${profile.name}")
            appendLine(" - weight: ${formatOneDecimal(profile.weight.toDouble())}")
            appendLine(" - height: ${profile.height}")
            appendLine(" - experience: ${formatTitleLabel(profile.experience)}")
            appendLine()
            appendLine("Address the user as \"${profile.name}\" in the reason.")
        }
    }

    private suspend fun loadUserProfile(): UserEntity? {
        val userId = userActiveDao.get().firstOrNull() ?: return null
        val user = userDao.getById(userId).firstOrNull() ?: return null
        return user
    }

    companion object {
        private val BASE_PROMPT = """
            You are a strict workout planner.
            Choose EXACTLY ONE exercise from the "Candidates" list in the prompt.

            HARD CONSTRAINTS:
            - Return ONLY JSON: {"exerciseExampleId":"<id>","reason":"<=500 chars"} (no code block, no extra text).
            - "exerciseExampleId" MUST be one of the candidate IDs.
            - Do NOT select exercises already performed in the current session.

            DECISION RULES (in order):
            1) Maximize coverage of POSITIVE deficits for the PRIMARY muscle (primary-only accounting).
            2) Cycles priority: prefer candidates whose PRIMARY muscle is OVERDUE, then DUE (by macro days OR micro sessions, both with grace).
            3) Equipment tiebreak: when items are close by rules 1–2, prefer candidates whose equipmentIds DO NOT intersect with equipment used in the current session.
            4) Per-target category steering toward ~1:1 compound/isolation in the current session (use history as reference).
            5) Global category policy: compounds earlier; if a category has a positive deficit, prioritize it.
            6) Mild anti-monotony: apply a small penalty if the same PRIMARY muscle had meaningful load recently (< 48h), even if recovered.
            7) Promote variety: lower usageCount, older lastUsed.
            8) If still tied, pick the first candidate in the list.

            Write the "reason" in friendly, user-facing language:
            - Avoid raw metrics, IDs, percentages, scores, or decimals.
            - Briefly explain fit: target coverage, freshness/recovery, balance, and variety.
            - Optionally include a simple session cue (e.g., "3x8 @ RIR2, rest 90s").
        """.trimIndent()
    }
}