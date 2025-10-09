package com.grippo.data.features.suggestions.data

import com.grippo.ai.AiService
import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.data.features.suggestions.domain.SuggestionsRepository
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.koin.core.annotation.Single
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days

@Single(binds = [SuggestionsRepository::class])
internal class SuggestionsRepositoryImpl(
    private val aiService: AiService,
    private val draftTrainingDao: DraftTrainingDao,
    private val trainingDao: TrainingDao,
    private val exerciseExampleDao: ExerciseExampleDao,
    private val userDao: UserDao,
    private val userActiveDao: UserActiveDao,
) : SuggestionsRepository {

    override suspend fun predictExerciseExample(): ExerciseExampleValue? {
        val now = DateTimeUtils.now()
        val catalog = loadExampleCatalog() ?: return null
        val signals = buildPredictionSignals(now, catalog)

        val candidates = selectCandidateContexts(catalog, signals)
        if (candidates.isEmpty()) return null

        val prompt = buildPrompt(now, signals, candidates)
        val answer = aiService.ask(prompt, SYSTEM_PROMPT.trimIndent())
        val chosenId = parseSuggestedExerciseId(answer)
        return chosenId?.let(catalog.byId::get)?.value
    }

    private suspend fun loadExampleCatalog(): ExampleCatalog? {
        val contexts = exerciseExampleDao
            .get()
            .firstOrNull()
            .orEmpty()
            .mapNotNull { it.toContextOrNull() }
        if (contexts.isEmpty()) return null

        val exclusions = loadUserExclusions()
        val filtered = contexts.filter { context ->
            context.isAllowed(exclusions.muscleIds, exclusions.equipmentIds)
        }
        if (filtered.isEmpty()) return null

        return ExampleCatalog(
            contexts = filtered,
            byId = filtered.associateBy { it.id }
        )
    }

    private suspend fun loadUserExclusions(): UserExclusions {
        val userId = userActiveDao.get().firstOrNull() ?: return UserExclusions.EMPTY

        val muscleIds = userDao.getExcludedMuscles(userId)
            .firstOrNull()
            ?.map { it.id }
            ?.toSet()
            ?: emptySet()

        val equipmentIds = userDao.getExcludedEquipments(userId)
            .firstOrNull()
            ?.map { it.id }
            ?.toSet()
            ?: emptySet()

        if (muscleIds.isEmpty() && equipmentIds.isEmpty()) {
            return UserExclusions.EMPTY
        }

        return UserExclusions(muscleIds, equipmentIds)
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
            .filterNot { summary -> sessionExercises.any { it.exampleId == summary.exampleId } }

        val performedIds = (sessionExercises + todayHistory)
            .map { it.exampleId }
            .toSet()

        val categoryStats = computeCategoryStats(trainingSummaries, sessionExercises)
        val muscleTargets = computeMuscleTargets(trainingSummaries, sessionExercises)
        val muscleDeficits = muscleTargets.associate { it.id to it.deficit }

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
            muscleDeficits = muscleDeficits,
            forceMix = sessionExercises.groupingBy { it.forceType }.eachCount(),
            weightMix = sessionExercises.groupingBy { it.weightType }.eachCount(),
            experienceMix = sessionExercises.groupingBy { it.experience }.eachCount()
        )
    }

    private fun selectCandidateContexts(
        catalog: ExampleCatalog,
        signals: PredictionSignals
    ): List<ExampleContext> {
        val comparator = exampleComparator(signals.stage, signals.muscleDeficits, signals.categoryStats)
        val filtered = catalog.contexts
            .filterNot { it.id in signals.performedExampleIds }
            .sortedWith(comparator)

        val shortlist = filtered.take(MAX_CANDIDATE_COUNT)
        if (shortlist.isNotEmpty()) return shortlist

        return catalog.contexts
            .sortedWith(comparator)
            .take(MAX_CANDIDATE_COUNT)
    }

    private fun TrainingPack.toSummary(exampleContextMap: Map<String, ExampleContext>): TrainingSummary {
        val performedAt = DateTimeUtils.toLocalDateTime(training.createdAt)
        val exercises = exercises.map { pack ->
            val context = exampleContextMap[pack.exercise.exerciseExampleId]
            val example = pack.example
            ExerciseSummary(
                exampleId = pack.exercise.exerciseExampleId,
                displayName = context?.displayName ?: pack.exercise.name,
                muscles = context?.muscles ?: emptyList(),
                category = context?.category ?: example?.category ?: CategoryEnum.COMPOUND.key,
                forceType = context?.forceType ?: example?.forceType ?: DEFAULT_FORCE_TYPE,
                weightType = context?.weightType ?: example?.weightType ?: DEFAULT_WEIGHT_TYPE,
                experience = context?.experience ?: example?.experience ?: DEFAULT_EXPERIENCE
            )
        }

        return TrainingSummary(
            performedAt = performedAt,
            dayOfWeek = performedAt.date.dayOfWeek,
            exercises = exercises
        )
    }

    private fun DraftTrainingPack.toSessionSummaries(
        exampleContextMap: Map<String, ExampleContext>
    ): List<ExerciseSummary> {
        return exercises.map { pack ->
            val exampleId = pack.exercise.exerciseExampleId
            val context = exampleContextMap[exampleId]
            ExerciseSummary(
                exampleId = exampleId,
                displayName = context?.displayName ?: pack.exercise.name,
                muscles = context?.muscles ?: emptyList(),
                category = context?.category ?: pack.example?.category ?: CategoryEnum.COMPOUND.key,
                forceType = context?.forceType ?: pack.example?.forceType ?: DEFAULT_FORCE_TYPE,
                weightType = context?.weightType ?: pack.example?.weightType ?: DEFAULT_WEIGHT_TYPE,
                experience = context?.experience ?: pack.example?.experience ?: DEFAULT_EXPERIENCE
            )
        }
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

        return CategoryStats(
            average = average,
            current = current
        )
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

    private fun buildPrompt(
        now: LocalDateTime,
        signals: PredictionSignals,
        candidateContexts: List<ExampleContext>
    ): String {
        val dominantExperience = signals.experienceMix.dominantKey()?.toTitleLabel()
        val muscleTargetMap = signals.muscleTargets.associateBy { it.id }
        val positiveTargets = signals.muscleTargets
            .filter { it.deficit > DEFICIT_EPS }
            .take(MAX_MUSCLE_TARGET_LINES)

        return buildString {
            appendLine("Goal: select the best next exercise example for the user.")
            appendLine("Reply ONLY with JSON {\"exerciseExampleId\":\"id\",\"reason\":\"short\"}.")
            appendLine()

            appendLine("Today: ${now.date} (${now.dayOfWeek.shortName()})")

            appendLine("Session so far (${signals.session.size} exercises):")
            if (signals.session.isEmpty()) {
                appendLine(" - none recorded; rely on recent history")
            } else {
                signals.session.take(MAX_SESSION_EXERCISES).forEach { exercise ->
                    val tags = listOf(exercise.category, exercise.forceType, exercise.weightType, exercise.experience)
                        .joinToString(separator = "/")
                    appendLine(" - ${exercise.displayName}${exercise.muscleSummary()} ($tags)")
                }
                if (signals.session.size > MAX_SESSION_EXERCISES) {
                    appendLine(" - ... and ${signals.session.size - MAX_SESSION_EXERCISES} more")
                }
            }

            val categoryLines = signals.categoryStats.summaryLines()
            if (categoryLines.isNotEmpty()) {
                appendLine()
                appendLine("Category balance (done / avg):")
                categoryLines.forEach { appendLine(" - $it") }
            }

            if (positiveTargets.isNotEmpty()) {
                appendLine()
                appendLine("Muscle targets to cover:")
                positiveTargets.forEach { target ->
                    appendLine(" - ${target.name}: avg ${target.average.toOneDecimal()}, done ${target.current} (needs +${target.deficit.toOneDecimal()})")
                }
                val focusNames = positiveTargets
                    .take(MAX_PRIMARY_FOCUS)
                    .joinToString { it.name }
                appendLine("Primary focus: $focusNames")
            }

            appendMixSection("Force mix", signals.forceMix)
            appendMixSection("Weight mix", signals.weightMix)
            appendMixSection("Experience mix", signals.experienceMix)

            if (signals.historicalToday.isNotEmpty()) {
                appendLine()
                appendLine("Earlier today (history):")
                signals.historicalToday.take(MAX_TODAY_EXERCISES).forEach { exercise ->
                    appendLine(" - ${exercise.displayName}${exercise.muscleSummary()}")
                }
                if (signals.historicalToday.size > MAX_TODAY_EXERCISES) {
                    appendLine(" - ... and ${signals.historicalToday.size - MAX_TODAY_EXERCISES} more")
                }
            }

            appendLine()
            appendLine("Guidelines:")
            appendLine(" - Prefer compounds early; switch to isolation once compounds hit average.")
            appendLine(" - Cover muscles with positive deficits first.")
            appendLine(" - Avoid exercises already completed in this session.")
            appendLine(" - Respect user exclusions: skip banned muscles or equipment.")
            dominantExperience?.let { appendLine(" - Keep difficulty around the $it level.") }

            appendLine()
            appendLine("Recent trainings:")
            if (signals.trainings.isEmpty()) {
                appendLine(" - none recorded in lookback window")
            } else {
                signals.trainings.forEachIndexed { index, training ->
                    append(" ${index + 1}. ${training.performedAt} (${training.dayOfWeek.shortName()}): ")
                    append(training.exercises.take(MAX_TRAINING_EXERCISES).joinToString { it.displayName + it.muscleSummary() })
                    if (training.exercises.size > MAX_TRAINING_EXERCISES) {
                        append(" +${training.exercises.size - MAX_TRAINING_EXERCISES} more")
                    }
                    appendLine()
                }
            }

            appendLine()
            appendLine("Muscle load (last ${signals.trainings.size} sessions):")
            if (signals.muscleLoads.isEmpty()) {
                appendLine(" - insufficient data")
            } else {
                signals.muscleLoads.forEach { load ->
                    appendLine(" - ${load.name}: ${load.total} load units over ${load.sessions} sessions")
                }
            }

            if (signals.weekdayPatterns.isNotEmpty()) {
                appendLine()
                appendLine("Weekday habits:")
                signals.weekdayPatterns.take(MAX_WEEKDAY_LINES).forEach { appendLine(" - $it") }
            }

            appendLine()
            appendLine("Candidates:")
            candidateContexts.forEach { context ->
                val tags = listOf(context.category, context.forceType, context.weightType, context.experience)
                    .joinToString(separator = "/")
                val targetNote = context.primaryMuscleId()
                    ?.let { muscleTargetMap[it] }
                    ?.let { target ->
                        when {
                            target.deficit > DEFICIT_EPS -> " (helps +${target.deficit.toOneDecimal()} on ${target.name})"
                            target.deficit < -DEFICIT_EPS -> " (over +${(-target.deficit).toOneDecimal()} on ${target.name})"
                            else -> ""
                        }
                    } ?: ""

                append(" - ${context.id}: ${context.displayName}; muscles ${context.muscleSummary()}; tags $tags; usage ${context.usageCount}; lastUsed ${context.lastUsedString()}$targetNote")
                appendLine()
            }
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

            val lastUsedCompare = (left.lastUsed ?: OLDEST_DATE).compareTo(right.lastUsed ?: OLDEST_DATE)
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

    private fun parseSuggestedExerciseId(raw: String): String? {
        val sanitized = raw.trim()
        if (sanitized.isEmpty()) return null

        val parsed = runCatching { json.parseToJsonElement(sanitized) }.getOrNull()
        if (parsed is JsonObject) {
            parsed["exerciseExampleId"]
                ?.let { it as? JsonPrimitive }
                ?.contentOrNull
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }

        val regex = "\\\"exerciseExampleId\\\"\\s*:\\s*\\\"([^\\\"]+)".toRegex()
        return regex.find(sanitized)?.groupValues?.getOrNull(1)
    }

    private fun ExerciseSummary.muscleSummary(): String {
        if (muscles.isEmpty()) return ""
        val dominant = muscles.take(MAX_MUSCLES_PER_EXERCISE).joinToString { "${it.name}(${it.percentage})" }
        return " [$dominant]"
    }

    private fun ExampleContext.muscleSummary(): String {
        if (muscles.isEmpty()) return "-"
        return muscles.take(MAX_MUSCLES_PER_EXERCISE).joinToString { "${it.name}(${it.percentage})" }
    }

    private fun StringBuilder.appendMixSection(label: String, mix: Map<String, Int>) {
        if (mix.isEmpty() || mix.size <= 1) return
        appendLine()
        appendLine("$label:")
        mix.entries
            .sortedByDescending { it.value }
            .forEach { (key, value) ->
                appendLine(" - ${key.toTitleLabel()}: $value")
            }
    }

    private fun Map<String, Int>.dominantKey(): String? {
        return entries.maxByOrNull { it.value }?.key
    }

    private fun DayOfWeek.shortName(): String {
        return name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
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

    private data class UserExclusions(
        val muscleIds: Set<String>,
        val equipmentIds: Set<String>
    ) {
        companion object {
            val EMPTY = UserExclusions(emptySet(), emptySet())
        }
    }

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

        fun summaryLines(): List<String> {
            val keys = (average.keys + current.keys).toSet()
            if (keys.isEmpty()) return emptyList()

            return keys.map { key ->
                val avg = average[key] ?: 0.0
                val done = current[key] ?: 0
                val diff = deficit(key)
                val diffLabel = when {
                    diff > DEFICIT_EPS -> "+${diff.toOneDecimal()}"
                    diff < -DEFICIT_EPS -> diff.toOneDecimal()
                    else -> "balanced"
                }

                "${key.toTitleLabel()}: $done / avg ${avg.toOneDecimal()} ($diffLabel)"
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
        fun isAllowed(excludedMuscleIds: Set<String>, excludedEquipmentIds: Set<String>): Boolean {
            if (muscles.any { it.id in excludedMuscleIds }) return false
            if (equipmentIds.any { it in excludedEquipmentIds }) return false
            return true
        }

        fun deficitScore(muscleDeficits: Map<String, Double>): Double {
            return muscles.sumOf { share ->
                val deficit = muscleDeficits[share.id]?.let { value -> if (value < 0.0) 0.0 else value } ?: 0.0
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

    private fun ExerciseExamplePack.toContextOrNull(): ExampleContext? {
        val value = example.toDomain() ?: return null
        val muscles = bundles
            .sortedByDescending { it.bundle.percentage }
            .take(MAX_MUSCLES_PER_EXERCISE)
            .map {
                MuscleShare(
                    id = it.muscle.id,
                    name = it.muscle.name,
                    percentage = it.bundle.percentage
                )
            }
        val equipmentIds = equipments.map { it.equipment.id }.toSet()

        return ExampleContext(
            id = value.id,
            displayName = value.name,
            muscles = muscles,
            category = value.category.key,
            forceType = value.forceType.key,
            weightType = value.weightType.key,
            experience = value.experience.key,
            usageCount = value.usageCount,
            lastUsed = value.lastUsed,
            equipmentIds = equipmentIds,
            value = value
        )
    }

    private fun ExampleContext.lastUsedString(): String {
        return lastUsed?.toString() ?: "never"
    }

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
        private const val DEFAULT_FORCE_TYPE = "unknown"
        private const val DEFAULT_WEIGHT_TYPE = "unknown"
        private const val DEFAULT_EXPERIENCE = "intermediate"
        private val OLDEST_DATE = LocalDateTime(1970, 1, 1, 0, 0, 0, 0)
        private val json = Json { ignoreUnknownKeys = true }
        private const val SYSTEM_PROMPT = """
            You are a focused workout planner. Pick one exercise example ID that complements the current session, balances muscle deficits, keeps compound lifts early, and respects weekday habits. Respond with compact JSON {"exerciseExampleId":"<id>","reason":"<=120 chars"} and nothing else.
        """
    }
}

private fun Double.toOneDecimal(): String {
    val scaled = (this * 10.0).roundToInt() / 10.0
    val intPart = scaled.toInt()
    return if (abs(scaled - intPart) < 1e-4) {
        intPart.toString()
    } else {
        scaled.toString()
    }
}

private fun String.toTitleLabel(): String {
    val normalized = replace('-', ' ').replace('_', ' ')
    val parts = normalized.split(' ').filter { it.isNotBlank() }
    if (parts.isEmpty()) return this

    return parts.joinToString(" ") { part ->
        val lower = part.lowercase()
        lower.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase() else ch.toString()
        }
    }
}
