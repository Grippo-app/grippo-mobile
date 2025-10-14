package com.grippo.data.features.suggestions.prompt.exercise.example

import com.grippo.ai.agent.AiAgentApi
import com.grippo.core.error.provider.AppError
import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.data.features.api.suggestion.models.ExerciseExampleSuggestion
import com.grippo.database.dao.DraftTrainingDao
import com.grippo.database.dao.ExerciseExampleDao
import com.grippo.database.dao.TrainingDao
import com.grippo.database.dao.UserActiveDao
import com.grippo.database.dao.UserDao
import com.grippo.database.models.DraftTrainingPack
import com.grippo.database.models.ExerciseExamplePack
import com.grippo.database.models.TrainingPack
import com.grippo.date.utils.DateTimeUtils
import com.grippo.entity.domain.equipment.toDomain
import com.grippo.toolkit.localization.LocalAppLocale
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days

@Single
internal class ExerciseExampleSuggestionPromptBuilder(
    private val aiAgent: AiAgentApi,
    private val draftTrainingDao: DraftTrainingDao,
    private val trainingDao: TrainingDao,
    private val exerciseExampleDao: ExerciseExampleDao,
    private val userDao: UserDao,
    private val userActiveDao: UserActiveDao,
    private val json: Json,
) {

    suspend fun suggest(now: LocalDateTime = DateTimeUtils.now()): Result<ExerciseExampleSuggestion?> {
        val locale = LocalAppLocale.current()

        val catalog = loadExampleCatalog() ?: return Result.failure(
            AppError.Expected(
                "Invalid example catalog",
                description = null
            )
        )
        val signals = buildPredictionSignals(now, catalog)
        val candidates = selectCandidateContexts(catalog, signals, now)
        if (candidates.isEmpty()) return Result.failure(
            AppError.Expected(
                "Candidates are empty",
                description = null
            )
        )

        val prompt = buildPrompt(now, signals, candidates, locale)
        val answer = aiAgent.ask(prompt, SYSTEM_PROMPT)

        return answer.map { a ->
            // Guardrail: only allow the model to pick from pre-ranked candidates
            val candidateMap = candidates.associateBy { it.id }
            val allowed = candidateMap.keys

            val parsed: ExerciseExampleSuggestion =
                parseSuggestedExerciseId(a, allowed) ?: return@map null
            if (!candidateMap.containsKey(parsed.id)) return@map null
            normalizeSuggestionOrNull(parsed)
        }
    }

    // ---------------------------
    // Catalog
    // ---------------------------
    private suspend fun loadExampleCatalog(): ExampleCatalog? {
        val userId = userActiveDao.get().firstOrNull() ?: return null

        val excludedMuscleIds = userDao.getExcludedMuscles(userId)
            .firstOrNull()
            ?.map { it.id }
            ?.toSet()
            ?: emptySet()

        val excludedEquipmentIds = userDao.getExcludedEquipments(userId)
            .firstOrNull()
            ?.map { it.id }
            ?.toSet()
            ?: emptySet()

        val contexts = exerciseExampleDao
            .getAll(
                excludedEquipmentIds = excludedEquipmentIds,
                excludedMuscleIds = excludedMuscleIds,
                limits = null,
                number = null,
                sorting = ExampleSortingEnum.RecentlyUsed.key
            )
            .firstOrNull()
            ?.mapNotNull { it.toContextOrNullStrict() } // strict pruning, no fallbacks
            ?: return null

        if (contexts.isEmpty()) return null

        return ExampleCatalog(
            contexts = contexts,
            byId = contexts.associateBy { it.id }
        )
    }

    // ---------------------------
    // Signals
    // ---------------------------
    private suspend fun buildPredictionSignals(
        now: LocalDateTime,
        catalog: ExampleCatalog
    ): PredictionSignals {
        val sessionExercises = draftTrainingDao.get()
            .firstOrNull()
            ?.toSessionSummaries(catalog.byId)
            ?: emptyList()

        val lookbackFrom = DateTimeUtils.minus(now, HISTORY_LOOKBACK_DAYS.days)
        val rawTrainings = trainingDao.get(
            from = DateTimeUtils.toUtcIso(lookbackFrom),
            to = DateTimeUtils.toUtcIso(now)
        ).firstOrNull().orEmpty()

        // Sort trainings from newest to oldest and keep only non-empty
        val trainingSummaries = rawTrainings
            .sortedByDescending { it.training.createdAt }
            .take(RECENT_TRAININGS_LIMIT)
            .map { it.toSummary(catalog.byId) }
            .filter { it.exercises.isNotEmpty() }

        val todayHistory = trainingSummaries
            .filter { it.performedAt.date == now.date }
            .flatMap { it.exercises }
            .filterNot { sessionExercises.any { session -> session.exampleId == it.exampleId } }

        val performedIds = (sessionExercises + todayHistory)
            .map { it.exampleId }
            .toSet()

        val usedEquipmentIds: Set<String> =
            sessionExercises.flatMap { it.equipmentIds }.toSet()

        val categoryStats = computeCategoryStats(trainingSummaries, sessionExercises)
        val muscleTargets = computeMuscleTargetsPrimaryOnly(trainingSummaries, sessionExercises)

        val lastLoadByMuscleDateTime = computeLastLoadDateTimeByMuscle(
            trainings = trainingSummaries,
        )

        // Residual fatigue per muscle (robust Q10–Q90 normalization with winsorization, data-only)
        val residualFatigueByMuscle = computeResidualFatigueByMuscle(
            now = now,
            trainings = trainingSummaries
        )

        val periodicHabits = computePeriodicHabits(
            trainings = trainingSummaries,
        )

        val sessionHabits = computeSessionHabits(
            trainings = trainingSummaries,
        )

        return PredictionSignals(
            session = sessionExercises,
            stage = sessionExercises.size,
            trainings = trainingSummaries,
            historicalToday = todayHistory,
            performedExampleIds = performedIds,
            muscleLoads = computeMuscleLoads(trainingSummaries, catalog.byId),
            categoryStats = categoryStats,
            muscleTargets = muscleTargets,
            muscleDeficits = muscleTargets.associate { it.id to it.deficit },
            forceMix = sessionExercises.groupingBy { it.forceType }.eachCount(),
            weightMix = sessionExercises.groupingBy { it.weightType }.eachCount(),
            experienceMix = sessionExercises.groupingBy { it.experience }.eachCount(),
            lastLoadByMuscleDateTime = lastLoadByMuscleDateTime,
            residualFatigueByMuscle = residualFatigueByMuscle,
            periodicHabits = periodicHabits,
            sessionHabits = sessionHabits,
            usedEquipmentIds = usedEquipmentIds
        )
    }

    // ---------------------------
    // Candidate selection (tiers + preferred first)
    // ---------------------------
    private fun selectCandidateContexts(
        catalog: ExampleCatalog,
        signals: PredictionSignals,
        nowDateTime: LocalDateTime
    ): List<ExampleContext> {

        // Adaptive Tier-A threshold: min(staticLimit, Q75 of unrecoveredWeightedShare across catalog)
        val allUnrecShares =
            catalog.contexts.map { it.unrecoveredWeightedShare(signals.residualFatigueByMuscle) }
        val q75Unrec = quantileDouble(allUnrecShares, 0.75).coerceIn(0.0, 1.0)
        val tierALimit = minOf(STRICT_UNREC_WEIGHTED_SHARE_LIMIT, q75Unrec)

        val comparator = exampleComparator(
            stage = signals.stage,
            muscleDeficits = signals.muscleDeficits,
            categoryStats = signals.categoryStats,
            trainings = signals.trainings,
            session = signals.session,
            nowDateTime = nowDateTime,
            lastLoadByMuscleDateTime = signals.lastLoadByMuscleDateTime,
            periodicHabits = signals.periodicHabits,
            sessionHabits = signals.sessionHabits,
            usedEquipmentIds = signals.usedEquipmentIds,
            residualFatigueByMuscle = signals.residualFatigueByMuscle
        )

        fun isPreferred(ctx: ExampleContext): Boolean {
            val pm = ctx.primaryMuscleId() ?: return false
            val st =
                combinedCycleState(pm, nowDateTime, signals.periodicHabits, signals.sessionHabits)
            return st == CycleState.DUE || st == CycleState.OVERDUE
        }

        fun rankWithinTier(seq: Sequence<ExampleContext>): List<ExampleContext> {
            val base = seq
                .filterNot { it.id in signals.performedExampleIds } // do not offer already performed today/this session
                .filter { it.muscles.isNotEmpty() }                  // strict inputs only
                .toList()

            val preferred = base.filter(::isPreferred).sortedWith(comparator)
            if (preferred.size >= MAX_CANDIDATE_COUNT) return preferred.take(MAX_CANDIDATE_COUNT)

            val others = base.filterNot(::isPreferred).sortedWith(comparator)
            return (preferred + others).take(MAX_CANDIDATE_COUNT)
        }

        // Tier A — strict: primary recovered by residual fatigue + bound on weighted unrecovered share
        val tierA = rankWithinTier(
            catalog.contexts.asSequence()
                .filter { it.isPrimaryRecoveredByResidual(signals.residualFatigueByMuscle) }
                .filter { it.unrecoveredWeightedShare(signals.residualFatigueByMuscle) <= tierALimit }
        )
        if (tierA.isNotEmpty()) return tierA

        // Tier B — softer: only require primary recovered by residual fatigue
        val tierB = rankWithinTier(
            catalog.contexts.asSequence()
                .filter { it.isPrimaryRecoveredByResidual(signals.residualFatigueByMuscle) }
        )
        if (tierB.isNotEmpty()) return tierB

        // Tier C — fallback pool (still strictly validated inputs)
        return rankWithinTier(catalog.contexts.asSequence())
    }

    // ---------------------------
    // Prompt
    // ---------------------------
    private fun buildPrompt(
        now: LocalDateTime,
        signals: PredictionSignals,
        candidates: List<ExampleContext>,
        locale: String
    ): String {
        val positiveTargets = signals.muscleTargets
            .filter { it.deficit > DEFICIT_EPS }
            .take(MAX_MUSCLE_TARGET_LINES)

        val muscleTargetMap = signals.muscleTargets.associateBy { it.id }
        val dominantExperience = signals.experienceMix.dominantKey()?.let { formatTitleLabel(it) }

        return buildString {
            renderIntro(now, locale)
            renderSession(signals)
            renderCategoryBalance(signals.categoryStats)
            renderMuscleTargets(positiveTargets)
            renderMix("Force mix", signals.forceMix)
            renderMix("Weight mix", signals.weightMix)
            renderMix("Experience mix", signals.experienceMix)
            renderGuidelines(dominantExperience, locale)
            renderRecentTrainings(signals.trainings)
            renderMuscleLoads(signals)
            renderResidualFatigue(signals)
            renderPeriodicHabits(now, signals.periodicHabits)
            renderCandidates(
                contexts = candidates,
                muscleTargetMap = muscleTargetMap,
                lastLoadByMuscleDateTime = signals.lastLoadByMuscleDateTime,
                nowDateTime = now,
                periodicHabits = signals.periodicHabits,
                residualFatigueByMuscle = signals.residualFatigueByMuscle
            )
        }
    }

    // ---------------------------
    // Parsing LLM output (strict)
    // ---------------------------
    @Serializable
    private data class ModelAnswer(
        val exerciseExampleId: String,
        val reason: String
    )

    private fun extractFirstJsonObject(input: String): String? {
        var depth = 0
        var start = -1
        for (i in input.indices) {
            when (input[i]) {
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }

                '}' -> {
                    if (depth > 0) {
                        depth--
                        if (depth == 0 && start >= 0) {
                            return input.substring(start, i + 1)
                        }
                    }
                }
            }
        }
        return null
    }

    private fun sanitizeRawModelText(raw: String): String {
        return raw
            .replace("```json", "```")
            .replace("```", "")
            .replace('“', '"')
            .replace('”', '"')
            .replace('’', '\'')
            .trim()
    }

    private fun parseSuggestedExerciseId(raw: String): ExerciseExampleSuggestion? {
        val sanitized = sanitizeRawModelText(raw)
        val jsonText = extractFirstJsonObject(sanitized) ?: return null

        val ans = runCatching { json.decodeFromString<ModelAnswer>(jsonText) }.getOrNull()
            ?: run {
                val id = Regex("\"exerciseExampleId\"\\s*:\\s*\"([^\"]+)\"")
                    .find(jsonText)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                val reason = Regex("\"reason\"\\s*:\\s*\"([\\s\\S]*?)\"\\s*[,}]")
                    .find(jsonText)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                if (id.isNotEmpty() && reason.isNotEmpty()) ModelAnswer(id, reason) else null
            }
            ?: return null

        val id = ans.exerciseExampleId.trim()
        val reason = ans.reason.trim()
        if (id.isEmpty() || reason.isEmpty()) return null

        return ExerciseExampleSuggestion(id = id, reason = reason)
    }

    private fun parseSuggestedExerciseId(
        raw: String,
        allowedIds: Set<String>
    ): ExerciseExampleSuggestion? {
        val parsed = parseSuggestedExerciseId(raw) ?: return null
        return if (allowedIds.contains(parsed.id)) parsed else null
    }

    private fun normalizeSuggestionOrNull(
        suggestion: ExerciseExampleSuggestion
    ): ExerciseExampleSuggestion? {
        val id = suggestion.id.trim()
        var reason = suggestion.reason.trim().replace(Regex("\\s+"), " ")
        if (id.isEmpty() || reason.isEmpty()) return null
        if (reason.length > 500) reason = reason.substring(0, 500)
        return suggestion.copy(id = id, reason = reason)
    }

    // ---------------------------
    // Render helpers
    // ---------------------------
    private fun StringBuilder.renderIntro(now: LocalDateTime, locale: String) {
        appendLine("Goal: select the best next exercise example for the user.")
        appendLine("""Reply ONLY with JSON {"exerciseExampleId":"id","reason":"<=500 chars"}.""")
        appendLine()
        appendLine("Locale: $locale (write the reason in this language)")
        appendLine("Now: $now")
        appendLine(
            "Today: ${now.date} (${
                now.dayOfWeek.name.lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            })"
        )
    }

    private fun StringBuilder.renderSession(signals: PredictionSignals) {
        appendLine()
        appendLine("Session so far (${signals.session.size} exercises):")
        if (signals.session.isEmpty()) {
            appendLine(" - none recorded; rely on recent history")
            return
        }

        signals.session.take(MAX_SESSION_EXERCISES).forEach { exercise ->
            val tags = listOf(
                exercise.category,
                exercise.forceType,
                exercise.weightType,
                exercise.experience
            ).joinToString("/")
            val eq = if (exercise.equipmentIds.isNotEmpty()) {
                " | equip ${exercise.equipmentIds.joinToString(",")}"
            } else ""
            appendLine(" - ${exercise.displayName}${sessionMuscleSummary(exercise)} ($tags)$eq")
        }
        if (signals.session.size > MAX_SESSION_EXERCISES) {
            appendLine(" - ... and ${signals.session.size - MAX_SESSION_EXERCISES} more")
        }
    }

    private fun StringBuilder.renderCategoryBalance(categoryStats: CategoryStats) {
        val categories = (categoryStats.average.keys + categoryStats.current.keys).toSet()
        if (categories.isEmpty()) return

        appendLine()
        appendLine("Category balance (done / avg):")
        categories.forEach { key ->
            val avg = categoryStats.average[key] ?: 0.0
            val done = categoryStats.current[key] ?: 0
            val diff = categoryStats.deficit(key)
            val diffLabel = when {
                diff > DEFICIT_EPS -> "+${formatOneDecimal(diff)}"
                diff < -DEFICIT_EPS -> formatOneDecimal(diff)
                else -> "balanced"
            }
            appendLine(" - ${formatTitleLabel(key)}: $done / avg ${formatOneDecimal(avg)} ($diffLabel)")
        }
    }

    private fun StringBuilder.renderMuscleTargets(positiveTargets: List<MuscleTarget>) {
        if (positiveTargets.isEmpty()) return

        appendLine()
        appendLine("Muscle targets to cover:")
        positiveTargets.forEach { target ->
            appendLine(
                " - ${target.name}: avg ${formatOneDecimal(target.average)}, done ${
                    formatOneDecimal(target.current)
                } (needs +${formatOneDecimal(target.deficit)})"
            )
        }
        val focusNames = positiveTargets.take(MAX_PRIMARY_FOCUS).joinToString { it.name }
        appendLine("Primary focus: $focusNames")
    }

    private fun StringBuilder.renderMix(label: String, mix: Map<String, Int>) {
        if (mix.isEmpty() || mix.size <= 1) return
        appendLine()
        appendLine("$label:")
        mix.entries.sortedByDescending { it.value }.forEach { (key, value) ->
            appendLine(" - ${formatTitleLabel(key)}: $value")
        }
    }

    private fun StringBuilder.renderGuidelines(dominantExperience: String?, locale: String) {
        appendLine()
        appendLine("Guidelines:")
        appendLine(" - Cover primary-muscle deficits first (primary-only accounting).")
        appendLine(" - Cycles: prefer due/overdue primary muscles by macro (days) or micro (sessions), both with grace.")
        appendLine(" - Respect recovery windows: skip if primary not recovered; avoid candidates with >60% weighted-unrecovered share (adaptive cap).")
        appendLine(" - Per-target muscle, steer category toward ~1:1 compound/isolation in this session (use history as reference).")
        appendLine(" - Global category: compounds earlier; if a category has a positive deficit, prioritize it.")
        appendLine(" - Mild anti-monotony: small penalty if the same primary muscle had meaningful load recently (< ${ANTI_MONOTONY_HOURS}h).")
        appendLine(" - Equipment tiebreak: prefer candidates whose equipmentIds DO NOT intersect with equipment used in this session.")
        appendLine(" - Promote variety: lower usageCount, older lastUsed. Avoid already performed; respect user exclusions.")
        dominantExperience?.let { appendLine(" - Keep difficulty around the $it level.") }
        appendLine(" - IMPORTANT: Write the `reason` in friendly, user-facing language:")
        appendLine("   * No raw metrics, IDs, percentages, scores, or decimals.")
        appendLine("   * Explain simply why this exercise fits now (target, freshness, balance, variety).")
        appendLine("   * You may include a brief set/rep/rest cue (e.g., \"3x8 @ RIR2, rest 90s\").")
        appendLine("   * Language: use '$locale' for the reason.")
    }

    private fun StringBuilder.renderRecentTrainings(trainings: List<TrainingSummary>) {
        appendLine()
        appendLine("Recent trainings:")
        if (trainings.isEmpty()) {
            appendLine(" - none recorded in lookback window")
            return
        }

        trainings.forEachIndexed { index, tr ->
            val dow = tr.dayOfWeek.name.lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            append(
                " ${index + 1}. ${tr.performedAt} ($dow) " +
                        "[vol ${formatOneDecimal(tr.totalVolume.toDouble())}, reps ${tr.totalRepetitions}, inten ${
                            formatOneDecimal(
                                tr.avgIntensity.toDouble()
                            )
                        }]: "
            )
            append(
                tr.exercises.take(MAX_TRAINING_EXERCISES)
                    .joinToString { it.displayName + sessionMuscleSummary(it) })
            if (tr.exercises.size > MAX_TRAINING_EXERCISES) append(" +${tr.exercises.size - MAX_TRAINING_EXERCISES} more")
            appendLine()
        }
    }

    private fun StringBuilder.renderMuscleLoads(signals: PredictionSignals) {
        appendLine()
        appendLine("Muscle load (last ${signals.trainings.size} sessions):")
        if (signals.muscleLoads.isEmpty()) {
            appendLine(" - insufficient data")
            return
        }
        signals.muscleLoads.forEach { load ->
            appendLine(" - ${load.name}: ${load.total} load units over ${load.sessions} sessions")
        }
    }

    private fun StringBuilder.renderResidualFatigue(signals: PredictionSignals) {
        if (signals.residualFatigueByMuscle.isEmpty()) return
        appendLine()
        appendLine("Residual fatigue by muscle (0..1, higher = less recovered):")
        signals.residualFatigueByMuscle
            .entries
            .sortedByDescending { it.value }
            .take(MAX_MUSCLE_SUMMARY)
            .forEach { (id, rf) ->
                val name = signals.muscleTargets.find { it.id == id }?.name ?: id
                appendLine(" - $name: ${formatOneDecimal(rf)}")
            }
    }

    private fun StringBuilder.renderPeriodicHabits(
        now: LocalDateTime,
        habits: Map<String, PeriodicHabit>
    ) {
        if (habits.isEmpty()) return
        val today = now.date
        val lines = habits.values
            .sortedBy { it.nextDue }
            .take(MAX_PERIODIC_LINES)
            .joinToString("\n") { h ->
                val status = when {
                    h.nextDue < today -> "overdue by ${h.nextDue.daysUntil(today)}d"
                    h.nextDue == today -> "due today"
                    else -> "in ${today.daysUntil(h.nextDue)}d"
                }
                " - ${h.muscleName}: every ~${h.medianIntervalDays}d (last ${h.lastDate}, next ${h.nextDue}, $status, conf ${
                    formatOneDecimal(
                        h.confidence
                    )
                })"
            }
        appendLine()
        appendLine("Periodic habits (per muscle):")
        appendLine(lines)
    }

    private fun StringBuilder.renderCandidates(
        contexts: List<ExampleContext>,
        muscleTargetMap: Map<String, MuscleTarget>,
        lastLoadByMuscleDateTime: Map<String, LocalDateTime>,
        nowDateTime: LocalDateTime,
        periodicHabits: Map<String, PeriodicHabit>,
        residualFatigueByMuscle: Map<String, Double>
    ) {
        appendLine()
        appendLine("Candidates:")
        val tz = USER_TIME_ZONE
        contexts.forEach { context ->
            val tags = listOf(
                context.category,
                context.forceType,
                context.weightType,
                context.experience
            ).joinToString("/")
            val primaryId = context.primaryMuscleId()
            val targetNote = primaryId
                ?.let { muscleTargetMap[it] }
                ?.let { t ->
                    when {
                        t.deficit > DEFICIT_EPS -> " (helps +${formatOneDecimal(t.deficit)} on ${t.name})"
                        t.deficit < -DEFICIT_EPS -> " (over +${formatOneDecimal(-t.deficit)} on ${t.name})"
                        else -> ""
                    }
                }.orEmpty()

            val cycleNote = primaryId?.let { pm ->
                val h = periodicHabits[pm]
                when {
                    h == null -> ""
                    h.nextDue < nowDateTime.date -> " [cycle: overdue]"
                    h.nextDue == nowDateTime.date -> " [cycle: due]"
                    nowDateTime.date.daysUntil(h.nextDue) <= PERIODIC_GRACE_DAYS -> " [cycle: soon]"
                    else -> ""
                }
            }.orEmpty()

            val antiMonotony = primaryId
                ?.let { lastLoadByMuscleDateTime[it] }
                ?.let { last ->
                    val hours = (nowDateTime.toInstant(tz) - last.toInstant(tz)).inWholeHours
                    if (hours in 0..ANTI_MONOTONY_HOURS.toLong()) " [recent<${ANTI_MONOTONY_HOURS}h]" else ""
                }.orEmpty()

            val unrecWeighted = context.unrecoveredWeightedShare(residualFatigueByMuscle)
            val unrecNote =
                if (unrecWeighted > 0.0) " [unrecW ${(unrecWeighted * 100).roundToInt()}%]" else ""

            val equip = if (context.equipmentIds.isNotEmpty()) " | equip ${
                context.equipmentIds.joinToString(",")
            }" else ""

            append(
                " - ${context.id}: ${context.displayName}; muscles ${contextMuscleSummary(context)}; " +
                        "tags $tags; usage ${context.usageCount}; lastUsed ${formatLastUsed(context.lastUsed)}" +
                        targetNote + cycleNote + antiMonotony + unrecNote + equip
            )
            appendLine()
        }
    }

    // ---------------------------
    // Domain mappers (strict)
    // ---------------------------
    private fun DraftTrainingPack.toSessionSummaries(
        exampleContextMap: Map<String, ExampleContext>
    ): List<ExerciseSummary> {
        return exercises.mapNotNull { pack ->
            val context =
                exampleContextMap[pack.exercise.exerciseExampleId] ?: return@mapNotNull null
            if (context.id.isBlank() || context.displayName.isBlank() || context.muscles.isEmpty()) return@mapNotNull null

            val forceType = context.forceType.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val weightType = context.weightType.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val experience = context.experience.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val category = context.category.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            ExerciseSummary(
                exampleId = context.id,
                displayName = context.displayName,
                muscles = context.muscles,
                category = category,
                forceType = forceType,
                weightType = weightType,
                experience = experience,
                equipmentIds = context.equipmentIds
            )
        }
    }

    private fun TrainingPack.toSummary(
        exampleContextMap: Map<String, ExampleContext>
    ): TrainingSummary {
        val performedAt = DateTimeUtils.toLocalDateTime(training.createdAt)

        val exSummaries = exercises.mapNotNull { pack ->
            val context =
                exampleContextMap[pack.exercise.exerciseExampleId] ?: return@mapNotNull null
            if (context.id.isBlank() || context.displayName.isBlank() || context.muscles.isEmpty()) return@mapNotNull null

            val forceType = context.forceType.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val weightType = context.weightType.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val experience = context.experience.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val category = context.category.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            ExerciseSummary(
                exampleId = context.id,
                displayName = context.displayName,
                muscles = context.muscles,
                category = category,
                forceType = forceType,
                weightType = weightType,
                experience = experience,
                equipmentIds = context.equipmentIds
            )
        }

        // intensity = volume / repetitions; fallback to stored intensity if reps==0
        val volume = training.volume
        val reps = training.repetitions
        val intensityFromData = if (reps > 0) volume / reps.toFloat() else training.intensity

        return TrainingSummary(
            performedAt = performedAt,
            dayOfWeek = performedAt.date.dayOfWeek,
            exercises = exSummaries,
            totalVolume = volume,
            totalRepetitions = reps,
            avgIntensity = intensityFromData
        )
    }

    private fun ExerciseExamplePack.toContextOrNullStrict(): ExampleContext? {
        val value = example.toDomain() ?: return null
        if (value.id.isBlank() || value.name.isBlank()) return null

        // Collect raw shares first (no guessing, no fallbacks)
        data class RawShare(
            val id: String,
            val name: String,
            val percentage: Int,
            val recoveryTimeHours: Int?
        )

        val rawShares = bundles.mapNotNull { pack ->
            val pct = pack.bundle.percentage
            val id = pack.muscle.id
            val name = pack.muscle.name
            val rh: Int? = pack.muscle.recoveryTimeHours
            if (pct > 0 && id.isNotBlank() && name.isNotBlank()) {
                RawShare(id, name, pct, rh)
            } else null
        }.sortedByDescending { it.percentage }

        if (rawShares.isEmpty()) return null

        // Require valid PRIMARY (top by percentage has recoveryTimeHours > 0)
        val primary = rawShares.first()
        val primaryRh = primary.recoveryTimeHours
        if (primaryRh == null || primaryRh <= 0) return null

        // Merge duplicates by muscleId, keep only entries with valid recoveryTimeHours > 0
        val merged = buildMap<String, Pair<String, Int>> {
            rawShares.forEach { s ->
                val rh = s.recoveryTimeHours
                if (rh != null && rh > 0) {
                    val prev = this[s.id]
                    val newPct = (prev?.second ?: 0) + s.percentage
                    put(s.id, s.name to newPct)
                }
            }
        }.entries
            .map { (id, pair) -> Triple(id, pair.first, pair.second.coerceAtLeast(0)) }
            .sortedByDescending { it.third }

        if (merged.isEmpty()) return null

        // Cap accumulated total to ≤100% by progressive fill; take top-N
        var remaining = 100
        val muscles = buildList {
            for ((id, name, pctRaw) in merged) {
                if (remaining <= 0) break
                val pct = pctRaw.coerceAtMost(remaining)
                remaining -= pct
                add(
                    MuscleShare(
                        id = id,
                        name = name,
                        percentage = pct,
                        recoveryTimeHours = if (id == primary.id) primaryRh else rawShares.firstOrNull { it.id == id }?.recoveryTimeHours
                    )
                )
            }
        }.take(MAX_MUSCLES_PER_EXERCISE)

        if (muscles.isEmpty()) return null

        val category = value.category.key.takeIf { it.isNotBlank() } ?: return null
        val forceType = value.forceType.key.takeIf { it.isNotBlank() } ?: return null
        val weightType = value.weightType.key.takeIf { it.isNotBlank() } ?: return null
        val experience = value.experience.key.takeIf { it.isNotBlank() } ?: return null

        val equipmentIds = equipments
            .mapNotNull { it.equipment.id.takeIf { id -> id.isNotBlank() } }
            .toSet()

        return ExampleContext(
            id = value.id,
            displayName = value.name,
            muscles = muscles,
            category = category,
            forceType = forceType,
            weightType = weightType,
            experience = experience,
            usageCount = value.usageCount,
            lastUsed = value.lastUsed,
            equipmentIds = equipmentIds,
            value = value
        )
    }

    // ---------------------------
    // Stats & helpers
    // ---------------------------
    private fun computeCategoryStats(
        trainings: List<TrainingSummary>,
        session: List<ExerciseSummary>
    ): CategoryStats {
        val total = mutableMapOf<String, Int>()
        val occurrences = mutableMapOf<String, Int>()

        trainings.forEach { training ->
            if (training.exercises.isEmpty()) return@forEach
            val counts = training.exercises.groupingBy { it.category }.eachCount()
            counts.forEach { (category, count) ->
                total[category] = (total[category] ?: 0) + count
                occurrences[category] = (occurrences[category] ?: 0) + 1
            }
        }

        val average = total.mapValues { (cat, sum) ->
            val occ = occurrences[cat] ?: trainings.size.coerceAtLeast(1)
            sum.toDouble() / occ
        }

        val current = session.groupingBy { it.category }.eachCount()
        return CategoryStats(average = average, current = current)
    }

    private fun computeMuscleTargetsPrimaryOnly(
        trainings: List<TrainingSummary>,
        session: List<ExerciseSummary>
    ): List<MuscleTarget> {

        fun primaryCount(exercises: List<ExerciseSummary>): Map<String, Double> {
            val map = mutableMapOf<String, Double>()
            exercises.forEach { ex ->
                val primary = ex.muscles.firstOrNull() ?: return@forEach
                map[primary.id] = (map[primary.id] ?: 0.0) + 1.0
            }
            return map
        }

        val totals = mutableMapOf<String, Double>()
        val occurrences = mutableMapOf<String, Int>()
        val names = mutableMapOf<String, String>()

        trainings.forEach { training ->
            if (training.exercises.isEmpty()) return@forEach
            val w = primaryCount(training.exercises)
            if (w.isEmpty()) return@forEach

            w.forEach { (id, value) -> totals[id] = (totals[id] ?: 0.0) + value }
            w.keys.forEach { id -> occurrences[id] = (occurrences[id] ?: 0) + 1 }
            training.exercises.mapNotNull { it.muscles.firstOrNull() }.forEach { share ->
                if (!names.containsKey(share.id)) names[share.id] = share.name
            }
        }

        session.mapNotNull { it.muscles.firstOrNull() }.forEach { share ->
            if (!names.containsKey(share.id)) names[share.id] = share.name
        }

        val currentPrimary = primaryCount(session)
        val ids = (totals.keys + currentPrimary.keys).toSet()
        if (ids.isEmpty()) return emptyList()

        return ids.map { id ->
            val occ = occurrences[id] ?: trainings.size.coerceAtLeast(1)
            val average = (totals[id] ?: 0.0) / occ.toDouble()
            val current = currentPrimary[id] ?: 0.0
            val name = names[id] ?: id
            MuscleTarget(id = id, name = name, average = average, current = current)
        }.sortedByDescending { it.deficit }
    }

    private fun computeMuscleLoads(
        trainings: List<TrainingSummary>,
        exampleContextMap: Map<String, ExampleContext>
    ): List<MuscleLoad> {
        val loads = mutableMapOf<String, MuscleLoad>()
        trainings.forEach { training ->
            val musclesSeen = mutableSetOf<String>()
            training.exercises.forEach { exercise ->
                val context = exampleContextMap[exercise.exampleId] ?: return@forEach
                context.muscles.forEach { muscle ->
                    val load = loads.getOrPut(muscle.name) { MuscleLoad(muscle.name, 0, 0) }
                    load.total += muscle.percentage
                    if (musclesSeen.add(muscle.name)) load.sessions += 1
                }
            }
        }
        return loads.values.sortedByDescending { it.total }.take(MAX_MUSCLE_SUMMARY)
    }

    /**
     * Residual fatigue per muscle using only user's data and robust normalization:
     * - For each past training in the lookback window:
     *   contribution = (percentage/100) * normIntensity * clamp01(1 - hoursSince/recoveryHours)
     * - Sum contributions per muscle with per-muscle clamp to ≤1 (winsor).
     * Data-only:
     * - percentage: from bundles (>0)
     * - recoveryHours: from muscle entity (>0); if absent -> ignore that share
     * - intensity: training.intensity or (volume / repetitions) if repetitions > 0
     * - normalization: robust Q10..Q90 over window with winsorization; if flat (Q90≈Q10), use 1.0 for all
     */
    private fun computeResidualFatigueByMuscle(
        now: LocalDateTime,
        trainings: List<TrainingSummary>
    ): Map<String, Double> {
        if (trainings.isEmpty()) return emptyMap()

        // keep only strictly positive finite intensities
        val rawIntensities = trainings.mapNotNull { tr ->
            tr.avgIntensity.takeIf { it.isFinite() && it > 0f }?.toDouble()
        }
        if (rawIntensities.isEmpty()) return emptyMap()

        val sorted = rawIntensities.sorted()
        val q10 = quantileDouble(sorted, 0.10)
        val q90 = quantileDouble(sorted, 0.90)

        fun robustNorm(i: Double): Double {
            if (!(q90 > q10)) return 1.0
            // winsorize to [q10, q90] before scaling
            val iWin = i.coerceIn(q10, q90)
            return ((iWin - q10) / (q90 - q10)).coerceIn(0.0, 1.0)
        }

        val tz = USER_TIME_ZONE
        val residual = mutableMapOf<String, Double>()

        trainings.forEach { tr ->
            val iRaw = tr.avgIntensity.toDouble()
            if (!iRaw.isFinite() || iRaw <= 0.0) return@forEach
            val iNorm = robustNorm(iRaw)
            if (iNorm <= 0.0) return@forEach

            val hoursSince = (now.toInstant(tz) - tr.performedAt.toInstant(tz)).inWholeHours
            tr.exercises.forEach { ex ->
                ex.muscles.forEach { share ->
                    val rh = share.recoveryTimeHours ?: return@forEach
                    if (rh <= 0) return@forEach

                    val decay = 1.0 - (hoursSince.toDouble() / rh.toDouble())
                    if (decay <= 0.0) return@forEach

                    val pct = share.percentage.coerceAtLeast(0).toDouble() / 100.0
                    val contrib = pct * iNorm * clamp01(decay)
                    if (contrib <= 0.0) return@forEach

                    // per-muscle clamp to ≤1 to avoid runaway sums
                    residual[share.id] = ((residual[share.id] ?: 0.0) + contrib).coerceAtMost(1.0)
                }
            }
        }
        return residual
    }

    private fun computeLastLoadDateTimeByMuscle(
        trainings: List<TrainingSummary>,
    ): Map<String, LocalDateTime> {
        val last = mutableMapOf<String, LocalDateTime>()
        trainings.forEach { training ->
            val ts = training.performedAt
            training.exercises.forEach { ex ->
                ex.muscles.forEach { share ->
                    if (share.percentage >= SIGNIFICANT_SHARE_THRESHOLD) {
                        val prev = last[share.id]
                        if (prev == null || prev < ts) last[share.id] = ts
                    }
                }
            }
        }
        return last
    }

    private fun computePeriodicHabits(
        trainings: List<TrainingSummary>,
    ): Map<String, PeriodicHabit> {
        val byMuscleDates = mutableMapOf<String, MutableList<LocalDate>>()
        val names = mutableMapOf<String, String>()

        trainings.sortedBy { it.performedAt }.forEach { tr ->
            val date = tr.performedAt.date
            tr.exercises.forEach { ex ->
                ex.muscles.forEach { share ->
                    if (share.percentage >= SIGNIFICANT_SHARE_THRESHOLD) {
                        val list = byMuscleDates.getOrPut(share.id) { mutableListOf() }
                        if (list.lastOrNull() != date) list.add(date) // de-dup per day
                        names.getOrPut(share.id) { share.name }
                    }
                }
            }
        }

        val habits = mutableMapOf<String, PeriodicHabit>()
        byMuscleDates.forEach { (id, dates) ->
            if (dates.size < PERIODIC_MIN_EVENTS) return@forEach
            val intervals = dates.zipWithNext { a, b -> a.daysUntil(b) }.filter { it > 0 }
            if (intervals.size < PERIODIC_MIN_INTERVALS) return@forEach

            val median = medianInt(intervals).coerceAtLeast(1)
            val recent = intervals.takeLast(PERIODIC_RECENT_WINDOW)
            val recentMedian = medianInt(recent)

            val iqr = iqrInt(intervals).toDouble()
            val spread = if (median > 0) (iqr / median.toDouble()) else 1.0
            val confidence = clamp01(1.0 - spread)

            val alpha = 0.5 * (1.0 - confidence)
            val blended =
                ((1 - alpha) * median + alpha * recentMedian).roundToInt().coerceAtLeast(1)

            val lastDate = dates.last()
            val nextDue = lastDate.plus(blended, DateTimeUnit.DAY)
            habits[id] = PeriodicHabit(
                muscleId = id,
                muscleName = names[id] ?: id,
                medianIntervalDays = blended,
                confidence = confidence,
                lastDate = lastDate,
                nextDue = nextDue
            )
        }
        return habits
    }

    private fun computeSessionHabits(
        trainings: List<TrainingSummary>,
    ): Map<String, SessionHabit> {
        val indicesByMuscle = mutableMapOf<String, MutableList<Int>>()

        trainings.forEachIndexed { idx, tr ->
            tr.exercises.forEach { ex ->
                val primary = ex.muscles.firstOrNull()
                if (primary != null && primary.percentage >= SIGNIFICANT_SHARE_THRESHOLD) {
                    indicesByMuscle.getOrPut(primary.id) { mutableListOf() }.add(idx)
                }
            }
        }

        val habits = mutableMapOf<String, SessionHabit>()
        indicesByMuscle.forEach { (muscleId, rawIdxs) ->
            val idxsAsc = rawIdxs.sorted()
            if (idxsAsc.size < PERIODIC_MIN_EVENTS) return@forEach
            val intervals = idxsAsc.zipWithNext { a, b -> b - a }.filter { it > 0 }
            if (intervals.size < PERIODIC_MIN_INTERVALS) return@forEach

            val median = medianInt(intervals).coerceAtLeast(1)
            val lastSeenIdx = idxsAsc.firstOrNull() ?: -1
            val nextDueIdx = if (lastSeenIdx >= 0) lastSeenIdx + median else median

            habits[muscleId] = SessionHabit(
                muscleId = muscleId,
                medianIntervalSessions = median,
                lastSeenIdx = lastSeenIdx,
                nextDueIdx = nextDueIdx
            )
        }
        return habits
    }

    // ---------------------------
    // Comparator (priorities)
    // ---------------------------
    private fun exampleComparator(
        stage: Int,
        muscleDeficits: Map<String, Double>,
        categoryStats: CategoryStats,
        trainings: List<TrainingSummary>,
        session: List<ExerciseSummary>,
        nowDateTime: LocalDateTime,
        lastLoadByMuscleDateTime: Map<String, LocalDateTime>,
        periodicHabits: Map<String, PeriodicHabit>,
        sessionHabits: Map<String, SessionHabit>,
        usedEquipmentIds: Set<String>,
        residualFatigueByMuscle: Map<String, Double>
    ): Comparator<ExampleContext> {
        return Comparator { left, right ->
            // 1) Primary-muscle deficit (larger deficit ranks first)
            fun primaryDeficit(ctx: ExampleContext): Double {
                val pm = ctx.primaryMuscleId() ?: return 0.0
                return (muscleDeficits[pm] ?: 0.0).coerceAtLeast(0.0)
            }

            val dL = primaryDeficit(left)
            val dR = primaryDeficit(right)
            val deficitCmp = dR.compareTo(dL).takeIf { abs(dL - dR) >= DEFICIT_EPS } ?: 0
            if (deficitCmp != 0) return@Comparator deficitCmp

            // 2) Cycle priority (OVERDUE < DUE < NONE)
            fun cycleRank(ctx: ExampleContext): Int {
                val pm = ctx.primaryMuscleId() ?: return 2
                return when (combinedCycleState(pm, nowDateTime, periodicHabits, sessionHabits)) {
                    CycleState.OVERDUE -> 0
                    CycleState.DUE -> 1
                    CycleState.NONE -> 2
                }
            }

            val cL = cycleRank(left)
            val cR = cycleRank(right)
            val cycleCmp = cL.compareTo(cR)
            if (cycleCmp != 0) return@Comparator cycleCmp

            // 3) Equipment tiebreak (prefer no conflict with used equipment)
            fun equipPenalty(ctx: ExampleContext): Int {
                val conflict = ctx.equipmentIds.any { it in usedEquipmentIds }
                return if (conflict) 1 else 0
            }

            val eL = equipPenalty(left)
            val eR = equipPenalty(right)
            val equipCmp = eL.compareTo(eR)
            if (equipCmp != 0) return@Comparator equipCmp

            // 4) Per-target category steering (~1:1 compound/isolation)
            fun prefMismatch(ctx: ExampleContext): Int {
                val pm = ctx.primaryMuscleId()
                val preferred = preferredCategoryForMuscleNext(pm, trainings, session)
                return if (preferred != null && ctx.category != preferred) 1 else 0
            }

            val pmL = prefMismatch(left)
            val pmR = prefMismatch(right)
            val pmCmp = pmL.compareTo(pmR)
            if (pmCmp != 0) return@Comparator pmCmp

            // 5) Global category policy alignment
            val catCmp = categoryStats.priorityFor(left.category, stage)
                .compareTo(categoryStats.priorityFor(right.category, stage))
            if (catCmp != 0) return@Comparator catCmp

            // 6) Anti-monotony (recent loads → small penalty). Dead-zone: if residual < threshold, no penalty.
            fun mono(ctx: ExampleContext): Int {
                val pm = ctx.primaryMuscleId() ?: return 0
                val residual = (residualFatigueByMuscle[pm] ?: 0.0).coerceIn(0.0, 1.0)
                if (residual < RESIDUAL_DEADZONE) return 0
                return antiMonotonyPenalty(ctx, nowDateTime, lastLoadByMuscleDateTime)
            }

            val mL = mono(left)
            val mR = mono(right)
            val monoCmp = mL.compareTo(mR)
            if (monoCmp != 0) return@Comparator monoCmp

            // 7) Variety: lower usageCount wins
            val useCmp = left.usageCount.compareTo(right.usageCount)
            if (useCmp != 0) return@Comparator useCmp

            // 8) Recency: older lastUsed ranks higher
            val lastUsedCmp = compareLastUsed(left.lastUsed, right.lastUsed)
            if (lastUsedCmp != 0) return@Comparator lastUsedCmp

            // 9) Stable fallback by id (ensures deterministic tie-breaks)
            val idCmp = left.id.compareTo(right.id)
            if (idCmp != 0) return@Comparator idCmp

            // 10) Preserve input order if still tied (stable sort)
            0
        }
    }

    // ---------------------------
    // Cycles (status + penalties)
    // ---------------------------
    private enum class CycleState { NONE, DUE, OVERDUE }

    private fun dayCycleState(
        muscleId: String,
        now: LocalDateTime,
        periodicHabits: Map<String, PeriodicHabit>
    ): CycleState {
        val h = periodicHabits[muscleId] ?: return CycleState.NONE
        val today = now.date
        val due = h.nextDue
        val graceBefore = due.minus(PERIODIC_GRACE_DAYS, DateTimeUnit.DAY)
        return when {
            today > due -> CycleState.OVERDUE
            today >= graceBefore -> CycleState.DUE
            else -> CycleState.NONE
        }
    }

    private fun sessionCycleState(
        muscleId: String,
        sessionHabits: Map<String, SessionHabit>
    ): CycleState {
        val h = sessionHabits[muscleId] ?: return CycleState.NONE
        val lastSeen = h.lastSeenIdx
        if (lastSeen < 0) return CycleState.NONE
        val sessionsSince = lastSeen // current session index is zero
        return when {
            sessionsSince > h.medianIntervalSessions + PERIODIC_GRACE_SESSIONS -> CycleState.OVERDUE
            sessionsSince >= h.medianIntervalSessions - PERIODIC_GRACE_SESSIONS -> CycleState.DUE
            else -> CycleState.NONE
        }
    }

    private fun combinedCycleState(
        muscleId: String,
        now: LocalDateTime,
        periodicHabits: Map<String, PeriodicHabit>,
        sessionHabits: Map<String, SessionHabit>
    ): CycleState {
        val d = dayCycleState(muscleId, now, periodicHabits)
        val s = sessionCycleState(muscleId, sessionHabits)
        return when {
            d == CycleState.OVERDUE || s == CycleState.OVERDUE -> CycleState.OVERDUE
            d == CycleState.DUE || s == CycleState.DUE -> CycleState.DUE
            else -> CycleState.NONE
        }
    }

    private fun antiMonotonyPenalty(
        ctx: ExampleContext,
        nowDateTime: LocalDateTime,
        lastLoadByMuscleDateTime: Map<String, LocalDateTime>
    ): Int {
        val pm = ctx.primaryMuscleId() ?: return 0
        val last = lastLoadByMuscleDateTime[pm] ?: return 0
        val tz = USER_TIME_ZONE
        val hours = (nowDateTime.toInstant(tz) - last.toInstant(tz)).inWholeHours
        return if (hours < ANTI_MONOTONY_HOURS.toLong()) 1 else 0
    }

    // ---------------------------
    // Additional helpers (visibility-wide)
    // ---------------------------
    private fun preferredCategoryForMuscleNext(
        muscleId: String?,
        trainings: List<TrainingSummary>,
        session: List<ExerciseSummary>
    ): String? {
        if (muscleId.isNullOrBlank()) return null

        var histSessions = 0
        var histCompound = 0.0
        var histIsolation = 0.0

        trainings.forEach { tr ->
            var c = 0.0
            var i = 0.0
            tr.exercises.forEach { ex ->
                if (ex.muscles.firstOrNull()?.id == muscleId) {
                    when (ex.category) {
                        CategoryEnum.COMPOUND.key -> c += 1.0
                        CategoryEnum.ISOLATION.key -> i += 1.0
                    }
                }
            }
            if (c > 0.0 || i > 0.0) {
                histSessions += 1
                histCompound += c
                histIsolation += i
            }
        }

        val avgCompound = if (histSessions > 0) histCompound / histSessions.toDouble() else 1.0
        val avgIsolation = if (histSessions > 0) histIsolation / histSessions.toDouble() else 1.0

        var curCompound = 0.0
        var curIsolation = 0.0
        session.forEach { ex ->
            if (ex.muscles.firstOrNull()?.id == muscleId) {
                when (ex.category) {
                    CategoryEnum.COMPOUND.key -> curCompound += 1.0
                    CategoryEnum.ISOLATION.key -> curIsolation += 1.0
                }
            }
        }

        val progCompound = if (avgCompound > 0) curCompound / avgCompound else 1.0
        val progIsolation = if (avgIsolation > 0) curIsolation / avgIsolation else 1.0

        return when {
            progCompound < progIsolation -> CategoryEnum.COMPOUND.key
            progIsolation < progCompound -> CategoryEnum.ISOLATION.key
            else -> null
        }
    }

    // Extension helpers used in selection and rendering
    private fun ExampleContext.unrecoveredWeightedShare(
        residual: Map<String, Double>
    ): Double {
        if (muscles.isEmpty()) return 0.0
        var acc = 0.0
        for (m in muscles) {
            val rf = (residual[m.id] ?: 0.0).coerceIn(0.0, 1.0)
            if (rf > 0.0) {
                acc += (m.percentage.coerceAtLeast(0)).toDouble() / 100.0 * rf
            }
        }
        return acc.coerceIn(0.0, 1.0)
    }

    private fun ExampleContext.isPrimaryRecoveredByResidual(
        residual: Map<String, Double>
    ): Boolean {
        val pm = primaryMuscleId() ?: return true
        val rf = (residual[pm] ?: 0.0).coerceIn(0.0, 1.0)
        return rf <= 0.0
    }

    private fun compareLastUsed(left: LocalDateTime?, right: LocalDateTime?): Int {
        return when {
            left == null && right == null -> 0
            left == null -> -1 // "never" counts as the oldest => higher rank
            right == null -> 1
            else -> left.compareTo(right) // older timestamps (smaller) go first
        }
    }

    // ---------------------------
    // Misc formatters & math utils
    // ---------------------------
    private fun formatOneDecimal(value: Double): String {
        val scaled = (value * 10.0).roundToInt() / 10.0
        val intPart = scaled.toInt()
        return if (abs(scaled - intPart) < 1e-4) intPart.toString() else scaled.toString()
    }

    private fun formatTitleLabel(raw: String): String {
        val normalized = raw.replace('-', ' ').replace('_', ' ')
        val parts = normalized.split(' ').filter { it.isNotBlank() }
        if (parts.isEmpty()) return raw
        return parts.joinToString(" ") { part ->
            val lower = part.lowercase()
            lower.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
        }
    }

    private fun quantileDouble(sortedValues: List<Double>, p: Double): Double {
        if (sortedValues.isEmpty()) return 0.0
        val clampedP = p.coerceIn(0.0, 1.0)
        val rank = clampedP * (sortedValues.size - 1)
        val lo = rank.toInt()
        val hi = (lo + 1).coerceAtMost(sortedValues.lastIndex)
        val frac = rank - lo
        return sortedValues[lo] * (1 - frac) + sortedValues[hi] * frac
    }

    private fun quantileDouble(
        values: List<Double>,
        p: Double,
        alreadySorted: Boolean = false
    ): Double {
        return if (alreadySorted) quantileDouble(values, p) else quantileDouble(values.sorted(), p)
    }

    private fun sessionMuscleSummary(exercise: ExerciseSummary): String {
        if (exercise.muscles.isEmpty()) return ""
        val dominant = exercise.muscles.take(MAX_MUSCLES_PER_EXERCISE)
            .joinToString { "${it.name}(${it.percentage})" }
        return " [$dominant]"
    }

    private fun contextMuscleSummary(context: ExampleContext): String {
        if (context.muscles.isEmpty()) return "-"
        return context.muscles.take(MAX_MUSCLES_PER_EXERCISE)
            .joinToString { "${it.name}(${it.percentage})" }
    }

    private fun formatLastUsed(lastUsed: LocalDateTime?): String = lastUsed?.toString() ?: "never"

    private fun Map<String, Int>.dominantKey(): String? = entries.maxByOrNull { it.value }?.key

    // ---------------------------
    // Data classes
    // ---------------------------
    private data class PredictionSignals(
        val session: List<ExerciseSummary>,
        val stage: Int,
        val trainings: List<TrainingSummary>,
        val historicalToday: List<ExerciseSummary>,
        val performedExampleIds: Set<String>,
        val muscleLoads: List<MuscleLoad>,
        val categoryStats: CategoryStats,
        val muscleTargets: List<MuscleTarget>,
        val muscleDeficits: Map<String, Double>,
        val forceMix: Map<String, Int>,
        val weightMix: Map<String, Int>,
        val experienceMix: Map<String, Int>,
        val lastLoadByMuscleDateTime: Map<String, LocalDateTime>,
        val residualFatigueByMuscle: Map<String, Double>,
        val periodicHabits: Map<String, PeriodicHabit>,
        val sessionHabits: Map<String, SessionHabit>,
        val usedEquipmentIds: Set<String>
    )

    private data class ExampleCatalog(
        val contexts: List<ExampleContext>,
        val byId: Map<String, ExampleContext>
    )

    private data class TrainingSummary(
        val performedAt: LocalDateTime,
        val dayOfWeek: DayOfWeek,
        val exercises: List<ExerciseSummary>,
        val totalVolume: Float,
        val totalRepetitions: Int,
        val avgIntensity: Float
    )

    private data class ExerciseSummary(
        val exampleId: String,
        val displayName: String,
        val muscles: List<MuscleShare>,
        val category: String,
        val forceType: String,
        val weightType: String,
        val experience: String,
        val equipmentIds: Set<String>
    )

    private data class MuscleLoad(
        val name: String,
        var total: Int,
        var sessions: Int
    )

    private data class MuscleTarget(
        val id: String,
        val name: String,
        val average: Double,
        val current: Double
    ) {
        val deficit: Double get() = average - current
    }

    private data class CategoryStats(
        val average: Map<String, Double>,
        val current: Map<String, Int>
    ) {
        fun deficit(category: String): Double {
            val avg = average[category] ?: 0.0
            val done = current[category] ?: 0
            return avg - done
        }

        fun priorityFor(category: String, stage: Int): Int {
            val compoundKey = CategoryEnum.COMPOUND.key
            val isolationKey = CategoryEnum.ISOLATION.key
            return when (category) {
                compoundKey -> when {
                    stage < EARLY_STAGE_COMPOUND_LIMIT -> 0
                    deficit(compoundKey) > DEFICIT_EPS -> 0
                    else -> 1
                }

                isolationKey -> when {
                    stage < EARLY_STAGE_COMPOUND_LIMIT -> 1
                    deficit(isolationKey) > DEFICIT_EPS -> 0
                    else -> 1
                }

                else -> 1
            }
        }
    }

    private data class ExampleContext(
        val id: String,
        val displayName: String,
        val muscles: List<MuscleShare>,
        val category: String,
        val forceType: String,
        val weightType: String,
        val experience: String,
        val usageCount: Int,
        val lastUsed: LocalDateTime?,
        val equipmentIds: Set<String>,
        val value: ExerciseExampleValue
    ) {
        fun primaryMuscleId(): String? = muscles.firstOrNull()?.id
    }

    private data class MuscleShare(
        val id: String,
        val name: String,
        val percentage: Int,
        val recoveryTimeHours: Int? = null
    )

    private data class PeriodicHabit(
        val muscleId: String,
        val muscleName: String,
        val medianIntervalDays: Int,
        val confidence: Double,
        val lastDate: LocalDate,
        val nextDue: LocalDate
    )

    private data class SessionHabit(
        val muscleId: String,
        val medianIntervalSessions: Int,
        val lastSeenIdx: Int,
        val nextDueIdx: Int
    )

    // ---------------------------
    // Tunables
    // ---------------------------
    private companion object {
        private const val HISTORY_LOOKBACK_DAYS = 90
        private const val RECENT_TRAININGS_LIMIT = 16

        private const val MAX_SESSION_EXERCISES = 12
        private const val MAX_TRAINING_EXERCISES = 16
        private const val MAX_MUSCLES_PER_EXERCISE = 4
        private const val MAX_MUSCLE_SUMMARY = 12
        private const val MAX_MUSCLE_TARGET_LINES = 8
        private const val MAX_PRIMARY_FOCUS = 3
        private const val MAX_CANDIDATE_COUNT = 20 // slightly increased

        private const val EARLY_STAGE_COMPOUND_LIMIT = 2
        private const val DEFICIT_EPS = 0.1
        private const val SIGNIFICANT_SHARE_THRESHOLD = 30 // %

        // Strict limit and adaptive cap for Tier-A unrecovered-weighted share
        private const val STRICT_UNREC_WEIGHTED_SHARE_LIMIT = 0.6 // 60%
        private const val RESIDUAL_DEADZONE = 0.10 // anti-monotony dead-zone threshold

        private const val ANTI_MONOTONY_HOURS = 48
        private const val PERIODIC_GRACE_DAYS = 1
        private const val PERIODIC_GRACE_SESSIONS = 1
        private const val PERIODIC_MIN_EVENTS = 3
        private const val PERIODIC_MIN_INTERVALS = 2
        private const val PERIODIC_RECENT_WINDOW = 3
        private const val MAX_PERIODIC_LINES = 8

        // Use a single, consistent TZ. Replace with user's TZ if available in your environment.
        private val USER_TIME_ZONE: TimeZone = TimeZone.currentSystemDefault()

        private val SYSTEM_PROMPT = """
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

        private fun clamp01(v: Double) = when {
            v < 0.0 -> 0.0
            v > 1.0 -> 1.0
            else -> v
        }

        private fun medianInt(values: List<Int>): Int {
            if (values.isEmpty()) return 0
            val sorted = values.sorted()
            val mid = sorted.size / 2
            return if (sorted.size % 2 == 0) ((sorted[mid - 1] + sorted[mid]) / 2.0).roundToInt() else sorted[mid]
        }

        private fun iqrInt(values: List<Int>): Int {
            if (values.size < 4) return (values.maxOrNull() ?: 0) - (values.minOrNull() ?: 0)
            val sorted = values.sorted()
            val q1 = percentile(sorted, 25.0)
            val q3 = percentile(sorted, 75.0)
            return (q3 - q1).roundToInt()
        }

        private fun percentile(sorted: List<Int>, p: Double): Double {
            if (sorted.isEmpty()) return 0.0
            val rank = (p / 100.0) * (sorted.size - 1)
            val lo = rank.toInt()
            val hi = (lo + 1).coerceAtMost(sorted.lastIndex)
            val frac = rank - lo
            return sorted[lo] * (1 - frac) + sorted[hi] * frac
        }
    }
}
