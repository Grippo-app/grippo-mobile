package com.grippo.data.features.suggestions.prompt.exercise.example

import com.grippo.ai.AiService
import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.data.features.api.suggestion.models.ExerciseExampleSuggestion
import com.grippo.database.dao.DraftTrainingDao
import com.grippo.database.dao.ExerciseExampleDao
import com.grippo.database.dao.TrainingDao
import com.grippo.database.dao.UserActiveDao
import com.grippo.database.dao.UserDao
import com.grippo.database.domain.exercise.equipment.toDomain
import com.grippo.database.models.DraftTrainingPack
import com.grippo.database.models.ExerciseExamplePack
import com.grippo.database.models.TrainingPack
import com.grippo.date.utils.DateTimeUtils
import com.grippo.logger.AppLogger
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days

internal class ExerciseExampleSuggestionPromptBuilder(
    private val aiService: AiService,
    private val draftTrainingDao: DraftTrainingDao,
    private val trainingDao: TrainingDao,
    private val exerciseExampleDao: ExerciseExampleDao,
    private val userDao: UserDao,
    private val userActiveDao: UserActiveDao,
) {

    suspend fun suggest(now: LocalDateTime = DateTimeUtils.now()): ExerciseExampleSuggestion? {
        val catalog = loadExampleCatalog() ?: return null
        val signals = buildPredictionSignals(now, catalog)
        val candidates = selectCandidateContexts(catalog, signals)
        if (candidates.isEmpty()) return null

        val prompt = buildPrompt(now, signals, candidates)

        AppLogger.AI.prompt(prompt)

        val answer = aiService.ask(prompt, SYSTEM_PROMPT)

        AppLogger.AI.answer(answer)

        return parseSuggestedExerciseId(answer)
    }

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
            ?.mapNotNull { it.toContextOrNull() }
            ?: return null

        return ExampleCatalog(
            contexts = contexts,
            byId = contexts.associateBy { it.id }
        )
    }

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

        val trainingSummaries = rawTrainings
            .sortedByDescending { it.training.createdAt }
            .take(RECENT_TRAININGS_LIMIT)
            .map { it.toSummary(catalog.byId) }

        val todayHistory = trainingSummaries
            .filter { it.performedAt.date == now.date }
            .flatMap { it.exercises }
            .filterNot { sessionExercises.any { session -> session.exampleId == it.exampleId } }

        val performedIds = (sessionExercises + todayHistory)
            .map { it.exampleId }
            .toSet()

        val categoryStats = computeCategoryStats(trainingSummaries, sessionExercises)
        val muscleTargets = computeMuscleTargets(trainingSummaries, sessionExercises)

        return PredictionSignals(
            session = sessionExercises,
            stage = sessionExercises.size,
            trainings = trainingSummaries,
            historicalToday = todayHistory,
            performedExampleIds = performedIds,
            muscleLoads = computeMuscleLoads(trainingSummaries, catalog.byId),
            weekdayPatterns = computeWeekdayPatterns(trainingSummaries, catalog.byId),
            categoryStats = categoryStats,
            muscleTargets = muscleTargets,
            muscleDeficits = muscleTargets.associate { it.id to it.deficit },
            forceMix = sessionExercises.groupingBy { it.forceType }.eachCount(),
            weightMix = sessionExercises.groupingBy { it.weightType }.eachCount(),
            experienceMix = sessionExercises.groupingBy { it.experience }.eachCount()
        )
    }

    private fun selectCandidateContexts(
        catalog: ExampleCatalog,
        signals: PredictionSignals
    ): List<ExampleContext> {
        val comparator =
            exampleComparator(signals.stage, signals.muscleDeficits, signals.categoryStats)
        val filtered = catalog.contexts
            .filterNot { it.id in signals.performedExampleIds }
            .sortedWith(comparator)

        val shortlist = filtered.take(MAX_CANDIDATE_COUNT)
        if (shortlist.isNotEmpty()) return shortlist

        return catalog.contexts
            .sortedWith(comparator)
            .take(MAX_CANDIDATE_COUNT)
    }

    private fun buildPrompt(
        now: LocalDateTime,
        signals: PredictionSignals,
        candidates: List<ExampleContext>
    ): String {
        val positiveTargets = signals.muscleTargets
            .filter { it.deficit > DEFICIT_EPS }
            .take(MAX_MUSCLE_TARGET_LINES)
        val muscleTargetMap = signals.muscleTargets.associateBy { it.id }
        val dominantExperience = signals.experienceMix.dominantKey()?.let { formatTitleLabel(it) }

        return buildString {
            renderIntro(now)
            renderSession(signals)
            renderCategoryBalance(signals.categoryStats)
            renderMuscleTargets(positiveTargets)
            renderMix("Force mix", signals.forceMix)
            renderMix("Weight mix", signals.weightMix)
            renderMix("Experience mix", signals.experienceMix)
            renderTodayHistory(signals)
            renderGuidelines(dominantExperience)
            renderRecentTrainings(signals.trainings)
            renderMuscleLoads(signals)
            renderWeekdayHabits(signals.weekdayPatterns)
            renderCandidates(candidates, muscleTargetMap)
        }
    }

    private fun parseSuggestedExerciseId(raw: String): ExerciseExampleSuggestion? {
        val sanitized = raw.trim()
        if (sanitized.isEmpty()) return null

        val parsed = runCatching { json.parseToJsonElement(sanitized) }.getOrNull()
        if (parsed is JsonObject) {
            val id = parsed["exerciseExampleId"]
                ?.let { it as? JsonPrimitive }
                ?.contentOrNull
                ?.takeIf { it.isNotBlank() }

            val reason = parsed["reason"]
                ?.let { it as? JsonPrimitive }
                ?.contentOrNull
                ?.takeIf { it.isNotBlank() }

            if (id != null && reason != null) {
                return ExerciseExampleSuggestion(id = id, reason = reason)
            }
        }

        val id = EXERCISE_ID_REGEX.find(sanitized)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }

        val reason = REASON_REGEX.find(sanitized)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }

        return if (id != null && reason != null) {
            ExerciseExampleSuggestion(id = id, reason = reason)
        } else {
            null
        }
    }

    private fun StringBuilder.renderIntro(now: LocalDateTime) {
        appendLine("Goal: select the best next exercise example for the user.")
        appendLine("Reply ONLY with JSON {\"exerciseExampleId\":\"id\",\"reason\":\"short\"}.")
        appendLine()
        appendLine("Today: ${now.date} (${now.dayOfWeek.shortName()})")
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
            ).joinToString(separator = "/")
            appendLine(" - ${exercise.displayName}${sessionMuscleSummary(exercise)} ($tags)")
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
                " - ${target.name}: avg ${formatOneDecimal(target.average)}, " +
                        "done ${target.current} (needs +${formatOneDecimal(target.deficit)})"
            )
        }
        val focusNames = positiveTargets
            .take(MAX_PRIMARY_FOCUS)
            .joinToString { it.name }
        appendLine("Primary focus: $focusNames")
    }

    private fun StringBuilder.renderMix(label: String, mix: Map<String, Int>) {
        if (mix.isEmpty() || mix.size <= 1) return

        appendLine()
        appendLine("$label:")
        mix.entries
            .sortedByDescending { it.value }
            .forEach { (key, value) -> appendLine(" - ${formatTitleLabel(key)}: $value") }
    }

    private fun StringBuilder.renderTodayHistory(signals: PredictionSignals) {
        if (signals.historicalToday.isEmpty()) return

        appendLine()
        appendLine("Earlier today (history):")
        signals.historicalToday.take(MAX_TODAY_EXERCISES).forEach { exercise ->
            appendLine(" - ${exercise.displayName}${sessionMuscleSummary(exercise)}")
        }
        if (signals.historicalToday.size > MAX_TODAY_EXERCISES) {
            appendLine(" - ... and ${signals.historicalToday.size - MAX_TODAY_EXERCISES} more")
        }
    }

    private fun StringBuilder.renderGuidelines(dominantExperience: String?) {
        appendLine()
        appendLine("Guidelines:")
        appendLine(" - Prefer compounds early; switch to isolation once compounds hit average.")
        appendLine(" - Cover muscles with positive deficits first.")
        appendLine(" - Avoid exercises already completed in this session.")
        appendLine(" - Respect user exclusions: skip banned muscles or equipment.")
        dominantExperience?.let { appendLine(" - Keep difficulty around the $it level.") }
    }

    private fun StringBuilder.renderRecentTrainings(trainings: List<TrainingSummary>) {
        appendLine()
        appendLine("Recent trainings:")
        if (trainings.isEmpty()) {
            appendLine(" - none recorded in lookback window")
            return
        }

        trainings.forEachIndexed { index, training ->
            append(" ${index + 1}. ${training.performedAt} (${training.dayOfWeek.shortName()}): ")
            append(
                training.exercises
                    .take(MAX_TRAINING_EXERCISES)
                    .joinToString { it.displayName + sessionMuscleSummary(it) }
            )
            if (training.exercises.size > MAX_TRAINING_EXERCISES) {
                append(" +${training.exercises.size - MAX_TRAINING_EXERCISES} more")
            }
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

    private fun StringBuilder.renderWeekdayHabits(weekdayPatterns: List<String>) {
        if (weekdayPatterns.isEmpty()) return

        appendLine()
        appendLine("Weekday habits:")
        weekdayPatterns.take(MAX_WEEKDAY_LINES).forEach { appendLine(" - $it") }
    }

    private fun StringBuilder.renderCandidates(
        contexts: List<ExampleContext>,
        muscleTargetMap: Map<String, MuscleTarget>
    ) {
        appendLine()
        appendLine("Candidates:")
        contexts.forEach { context ->
            val tags = listOf(
                context.category,
                context.forceType,
                context.weightType,
                context.experience
            ).joinToString(separator = "/")

            val targetNote = context.primaryMuscleId()
                ?.let { muscleTargetMap[it] }
                ?.let { target ->
                    when {
                        target.deficit > DEFICIT_EPS ->
                            " (helps +${formatOneDecimal(target.deficit)} on ${target.name})"

                        target.deficit < -DEFICIT_EPS ->
                            " (over +${formatOneDecimal(-target.deficit)} on ${target.name})"

                        else -> ""
                    }
                }.orEmpty()

            append(
                " - ${context.id}: ${context.displayName}; muscles ${contextMuscleSummary(context)}; " +
                        "tags $tags; usage ${context.usageCount}; lastUsed ${formatLastUsed(context.lastUsed)}$targetNote"
            )
            appendLine()
        }
    }

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

    private fun sessionMuscleSummary(exercise: ExerciseSummary): String {
        if (exercise.muscles.isEmpty()) return ""
        val dominant = exercise.muscles
            .take(MAX_MUSCLES_PER_EXERCISE)
            .joinToString { "${it.name}(${it.percentage})" }
        return " [$dominant]"
    }

    private fun contextMuscleSummary(context: ExampleContext): String {
        if (context.muscles.isEmpty()) return "-"
        return context.muscles
            .take(MAX_MUSCLES_PER_EXERCISE)
            .joinToString { "${it.name}(${it.percentage})" }
    }

    private fun formatLastUsed(lastUsed: LocalDateTime?): String {
        return lastUsed?.toString() ?: "never"
    }

    private fun DraftTrainingPack.toSessionSummaries(
        exampleContextMap: Map<String, ExampleContext>
    ): List<ExerciseSummary> {
        return exercises.mapNotNull { pack ->
            val context =
                exampleContextMap[pack.exercise.exerciseExampleId] ?: return@mapNotNull null
            if (context.id.isBlank()) return@mapNotNull null
            if (context.displayName.isBlank()) return@mapNotNull null
            if (context.muscles.isEmpty()) return@mapNotNull null

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
                experience = experience
            )
        }
    }

    private fun TrainingPack.toSummary(
        exampleContextMap: Map<String, ExampleContext>
    ): TrainingSummary {
        val performedAt = DateTimeUtils.toLocalDateTime(training.createdAt)
        val exercises = exercises.mapNotNull { pack ->
            val context =
                exampleContextMap[pack.exercise.exerciseExampleId] ?: return@mapNotNull null
            if (context.id.isBlank()) return@mapNotNull null
            if (context.displayName.isBlank()) return@mapNotNull null
            if (context.muscles.isEmpty()) return@mapNotNull null

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
                experience = experience
            )
        }

        return TrainingSummary(
            performedAt = performedAt,
            dayOfWeek = performedAt.date.dayOfWeek,
            exercises = exercises
        )
    }

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

        val average = total.mapValues { (category, sum) ->
            val occ = occurrences[category] ?: trainings.size.coerceAtLeast(1)
            sum.toDouble() / occ
        }

        val current = session.groupingBy { it.category }.eachCount()
        return CategoryStats(average = average, current = current)
    }

    private fun computeMuscleTargets(
        trainings: List<TrainingSummary>,
        session: List<ExerciseSummary>
    ): List<MuscleTarget> {
        val totals = mutableMapOf<String, Int>()
        val occurrences = mutableMapOf<String, Int>()
        val names = mutableMapOf<String, String>()

        trainings.forEach { training ->
            val primaryCounts = training.exercises
                .mapNotNull { it.muscles.firstOrNull() }
                .groupingBy { it.id }
                .eachCount()

            primaryCounts.forEach { (id, count) ->
                totals[id] = (totals[id] ?: 0) + count
                occurrences[id] = (occurrences[id] ?: 0) + 1
            }

            training.exercises
                .mapNotNull { it.muscles.firstOrNull() }
                .forEach { share ->
                    if (!names.containsKey(share.id)) {
                        names[share.id] = share.name
                    }
                }
        }

        session
            .mapNotNull { it.muscles.firstOrNull() }
            .forEach { share ->
                if (!names.containsKey(share.id)) {
                    names[share.id] = share.name
                }
            }

        val currentCounts = session
            .mapNotNull { it.muscles.firstOrNull()?.id }
            .groupingBy { it }
            .eachCount()

        val ids = (totals.keys + currentCounts.keys).toSet()
        if (ids.isEmpty()) return emptyList()

        return ids.map { id ->
            val average = totals[id]?.let { total ->
                val occ = occurrences[id] ?: trainings.size.coerceAtLeast(1)
                total.toDouble() / occ
            } ?: 0.0

            val current = currentCounts[id] ?: 0
            val name = names[id] ?: id

            MuscleTarget(
                id = id,
                name = name,
                average = average,
                current = current
            )
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
                    if (musclesSeen.add(muscle.name)) {
                        load.sessions += 1
                    }
                }
            }
        }

        return loads.values
            .sortedByDescending { it.total }
            .take(MAX_MUSCLE_SUMMARY)
    }

    private fun computeWeekdayPatterns(
        trainings: List<TrainingSummary>,
        exampleContextMap: Map<String, ExampleContext>
    ): List<String> {
        val patterns = mutableMapOf<DayOfWeek, MutableMap<String, Int>>()

        trainings.forEach { training ->
            val perMuscle = patterns.getOrPut(training.dayOfWeek) { mutableMapOf() }
            training.exercises.forEach { exercise ->
                val context = exampleContextMap[exercise.exampleId] ?: return@forEach
                context.muscles.forEach { muscle ->
                    val current = perMuscle[muscle.name] ?: 0
                    perMuscle[muscle.name] = current + muscle.percentage
                }
            }
        }

        return patterns.entries
            .sortedBy { it.key.ordinal }
            .map { (day, muscles) ->
                val topMuscles = muscles.entries
                    .sortedByDescending { it.value }
                    .take(MAX_WEEKDAY_MUSCLES)
                    .joinToString { "${it.key}(${it.value})" }
                "${day.shortName()}: $topMuscles"
            }
    }

    private fun exampleComparator(
        stage: Int,
        muscleDeficits: Map<String, Double>,
        categoryStats: CategoryStats
    ): Comparator<ExampleContext> {
        return Comparator { left, right ->
            val muscleCompare = compareMuscleDeficit(left, right, muscleDeficits)
            if (muscleCompare != 0) return@Comparator muscleCompare

            val categoryCompare = categoryStats.priorityFor(left.category, stage)
                .compareTo(categoryStats.priorityFor(right.category, stage))
            if (categoryCompare != 0) return@Comparator categoryCompare

            val usageCompare = left.usageCount.compareTo(right.usageCount)
            if (usageCompare != 0) return@Comparator usageCompare

            val lastUsedCompare = compareLastUsed(left.lastUsed, right.lastUsed)
            if (lastUsedCompare != 0) return@Comparator lastUsedCompare

            left.displayName.compareTo(right.displayName)
        }
    }

    private fun compareMuscleDeficit(
        left: ExampleContext,
        right: ExampleContext,
        muscleDeficits: Map<String, Double>
    ): Int {
        val leftDeficit = left.deficitScore(muscleDeficits)
        val rightDeficit = right.deficitScore(muscleDeficits)
        if (abs(leftDeficit - rightDeficit) < DEFICIT_EPS) return 0
        return rightDeficit.compareTo(leftDeficit)
    }

    private fun compareLastUsed(left: LocalDateTime?, right: LocalDateTime?): Int {
        return when {
            left == null && right == null -> 0
            left == null -> -1
            right == null -> 1
            else -> left.compareTo(right)
        }
    }

    private fun ExerciseExamplePack.toContextOrNull(): ExampleContext? {
        val value = example.toDomain() ?: return null
        if (value.id.isBlank()) return null
        if (value.name.isBlank()) return null

        val muscles = bundles
            .mapNotNull { pack ->
                val percentage = pack.bundle.percentage
                val muscleId = pack.muscle.id
                val muscleName = pack.muscle.name
                when {
                    percentage <= 0 -> null
                    muscleId.isBlank() -> null
                    muscleName.isBlank() -> null
                    else -> MuscleShare(
                        id = muscleId,
                        name = muscleName,
                        percentage = percentage
                    )
                }
            }
            .sortedByDescending { it.percentage }
            .take(MAX_MUSCLES_PER_EXERCISE)
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

    private fun Map<String, Int>.dominantKey(): String? {
        return entries.maxByOrNull { it.value }?.key
    }

    private fun DayOfWeek.shortName(): String {
        return name.lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private data class PredictionSignals(
        val session: List<ExerciseSummary>,
        val stage: Int,
        val trainings: List<TrainingSummary>,
        val historicalToday: List<ExerciseSummary>,
        val performedExampleIds: Set<String>,
        val muscleLoads: List<MuscleLoad>,
        val weekdayPatterns: List<String>,
        val categoryStats: CategoryStats,
        val muscleTargets: List<MuscleTarget>,
        val muscleDeficits: Map<String, Double>,
        val forceMix: Map<String, Int>,
        val weightMix: Map<String, Int>,
        val experienceMix: Map<String, Int>
    )

    private data class ExampleCatalog(
        val contexts: List<ExampleContext>,
        val byId: Map<String, ExampleContext>
    )

    private data class TrainingSummary(
        val performedAt: LocalDateTime,
        val dayOfWeek: DayOfWeek,
        val exercises: List<ExerciseSummary>
    )

    private data class ExerciseSummary(
        val exampleId: String,
        val displayName: String,
        val muscles: List<MuscleShare>,
        val category: String,
        val forceType: String,
        val weightType: String,
        val experience: String
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
        val current: Int
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
        fun deficitScore(muscleDeficits: Map<String, Double>): Double {
            return muscles.sumOf { share ->
                val deficit = muscleDeficits[share.id]?.takeIf { it > 0.0 } ?: 0.0
                deficit * (share.percentage / 100.0)
            }
        }

        fun primaryMuscleId(): String? = muscles.firstOrNull()?.id
    }

    private data class MuscleShare(
        val id: String,
        val name: String,
        val percentage: Int
    )

    private companion object {
        private const val HISTORY_LOOKBACK_DAYS = 45
        private const val RECENT_TRAININGS_LIMIT = 6
        private const val MAX_SESSION_EXERCISES = 6
        private const val MAX_TODAY_EXERCISES = 6
        private const val MAX_TRAINING_EXERCISES = 6
        private const val MAX_MUSCLES_PER_EXERCISE = 3
        private const val MAX_MUSCLE_SUMMARY = 6
        private const val MAX_MUSCLE_TARGET_LINES = 4
        private const val MAX_PRIMARY_FOCUS = 2
        private const val MAX_WEEKDAY_MUSCLES = 2
        private const val MAX_WEEKDAY_LINES = 4
        private const val MAX_CANDIDATE_COUNT = 8
        private const val EARLY_STAGE_COMPOUND_LIMIT = 2
        private const val DEFICIT_EPS = 0.1

        private val EXERCISE_ID_REGEX = "\\\"exerciseExampleId\\\"\\s*:\\s*\\\"([^\\\"]+)".toRegex()
        private val REASON_REGEX = "\\\"reason\\\"\\s*:\\s*\\\"([^\\\"]+)".toRegex()

        private val json = Json { ignoreUnknownKeys = true }

        private val SYSTEM_PROMPT = """
            You are a focused workout planner. Pick one exercise example ID that complements the current session, balances muscle deficits, keeps compound lifts early, and respects weekday habits. Respond with compact JSON {"exerciseExampleId":"<id>","reason":"<=120 chars"} and nothing else.
        """.trimIndent()
    }
}
