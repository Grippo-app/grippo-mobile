package com.grippo.data.features.api.training

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleComponents
import com.grippo.data.features.api.exercise.metrics.ExerciseMetricsFeature
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.ExerciseArtifacts
import com.grippo.data.features.api.training.models.ExerciseArtifacts.Artifact
import com.grippo.data.features.api.training.models.ExerciseArtifacts.Field
import com.grippo.data.features.api.training.models.ExerciseArtifacts.Reason
import com.grippo.data.features.api.training.models.ExerciseValidationContext
import com.grippo.data.features.api.training.models.ExerciseValidationInput
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.data.features.api.weight.history.WeightHistoryFeature
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus

/** Validates exercise input using loaded catalog, training and body-weight evidence. */
public class ExerciseValidatorUseCase(
    private val trainingFeature: TrainingFeature,
    private val exerciseMetricsFeature: ExerciseMetricsFeature,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val weightHistoryFeature: WeightHistoryFeature,
) {
    /** Observes evidence without refetching it on every set edit. */
    public fun observeContext(
        exampleId: String,
        performedAt: LocalDateTime,
        excludedExerciseId: String?,
    ): Flow<ExerciseValidationContext> {
        val start = performedAt.date.minus(DatePeriod(days = VALIDATION_HISTORY_DAYS)).atTime(0, 0)
        val recent: Flow<List<Exercise>> = flow {
            emit(exerciseMetricsFeature.getRecentExercisesByExampleId(exampleId))
        }
        return combine(
            trainingFeature.observeTrainings(start, performedAt),
            recent,
            exerciseExampleFeature.observeExerciseExample(exampleId).map { example ->
                requireNotNull(example) { "Exercise example $exampleId is unavailable." }
            },
            weightHistoryFeature.observeWeightHistory(),
        ) { trainings, remote, example, weights ->
            val bodyWeight = weights
                // Exercise creation marks its start; a measurement later that day is still relevant.
                .filter { it.createdAt.date <= performedAt.date && it.weight.isFinite() && it.weight > 0f }
                .filter { it.createdAt.date.daysUntil(performedAt.date) in 0..90 }
                .maxByOrNull { it.createdAt }?.weight
            ExerciseValidationContext(
                components = example.components,
                // Recent server records supersede older cached versions of the same exercise.
                history = (remote + trainings.flatMap { it.exercises })
                    .filter { it.exerciseExample.id == exampleId }
                    .distinctBy { it.id },
                excludedExerciseId = excludedExerciseId,
                bodyWeight = bodyWeight,
            )
        }.distinctUntilChanged()
    }

    public fun execute(
        exercise: SetExercise,
        context: ExerciseValidationContext,
    ): ExerciseArtifacts? = execute(
        ExerciseValidationInput(exercise.name, exercise.exerciseExample.id, exercise.iterations, exercise.createdAt),
        context,
    )

    public fun execute(
        exercise: ExerciseValidationInput,
        context: ExerciseValidationContext,
    ): ExerciseArtifacts? {
        val iterations = exercise.iterations
        val invalid = iterations.map { validateComponents(it, context) }
        val validPeers = iterations.filterIndexed { index, _ -> invalid[index].isEmpty() }
        val findings = invalid.map { it.toMutableList() }
        val performedAt = exercise.performedAt
        val history = if (performedAt == null) emptyList() else context.history
            .asSequence()
            .filter { it.exerciseExample.id == exercise.exampleId }
            .filter { it.id != context.excludedExerciseId && it.createdAt < performedAt }
            .filter { it.createdAt.date.daysUntil(performedAt.date) in 0..VALIDATION_HISTORY_DAYS }
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt }
            // The domain model has no trainingId. Same-day records cannot prove independent sessions.
            .distinctBy { it.createdAt.date }
            .take(MAX_HISTORY_REFERENCES)
            .map { it.iterations.map { iteration -> iteration.validationIteration() } }
            .toList()
        val historyContext = context.copy(bodyWeight = null)
        val validHistory = history.map { session ->
            session.filter { validateComponents(it, historyContext).isEmpty() }
        }

        for (field in COMPONENT_FIELDS + Field.Repetitions) {
            iterations.forEachIndexed { index, iteration ->
                if (invalid[index].any { it.field == field }) return@forEachIndexed
                val actual = iteration.value(field) ?: return@forEachIndexed
                if (!actual.isFinite() || actual <= 0f) return@forEachIndexed
                if (field == Field.Repetitions && repetitionsExplainedByLoad(iteration, validPeers)) {
                    return@forEachIndexed
                }
                // Use immutable invalid findings, so checking one row never changes another's evidence.
                val peers = iterations.mapIndexedNotNull { peerIndex, peer ->
                    if (peerIndex == index || invalid[peerIndex].any { it.field == field }) null
                    else peer.value(field)
                }
                val local = validationReference(peers, minimumCount = 2)
                val supportingPeers = peers.count { peer ->
                    peer.isFinite() && abs(peer - actual) <= actual * 0.25f
                }
                val ramp = field in ADDED_WEIGHT_FIELDS && isRamp(index, iterations, field, actual)
                val historicalValues = validHistory.mapNotNull { session ->
                    val comparable = session.filter { sample ->
                        val value = sample.value(field)
                        value != null && value.isFinite() && value > 0f &&
                            sample.sameLoadModel(iteration) && comparableRepetitions(sample, iteration, field)
                    }.mapNotNull { it.value(field) }
                    // Ambiguous session modes cannot establish a representative value.
                    validationReference(comparable, minimumCount = 1)?.value
                }
                val historical = validationReference(historicalValues, minimumCount = 3)
                val historySupportsActual = historicalValues.count { reference ->
                    abs(actual - reference) <= reference * 0.25f
                } >= 3
                if (local != null && local.count > supportingPeers + 1 && !historySupportsActual &&
                    isOutlier(actual, local.value, field, history = false) && !(actual < local.value && ramp)
                ) {
                    findings[index] += outlier(field, actual, local, Reason.LocalOutlier)
                    return@forEachIndexed
                }
                val locallySupported = supportingPeers >= 2
                if (historical != null && !historySupportsActual && !(actual < historical.value && ramp) &&
                    isOutlier(actual, historical.value, field, history = true) &&
                    // Repeated current values cannot independently confirm a gross deviation.
                    (!locallySupported || isGrossHistoryDeviation(actual, historical.value, field))
                ) findings[index] += outlier(field, actual, historical, Reason.HistoryOutlier)
            }
        }
        if (findings.all { it.isEmpty() }) return null
        return ExerciseArtifacts(
            exerciseName = exercise.name,
            iterations = iterations.mapIndexed { index, iteration ->
                ExerciseArtifacts.IterationArtifacts(iteration, findings[index].toList())
            },
        )
    }

    private fun repetitionsExplainedByLoad(actual: SetIteration, iterations: List<SetIteration>): Boolean {
        val load = actual.volume
        if (!load.isFinite() || load <= 0f) return false
        return iterations.count { peer ->
            peer.volume.isFinite() && peer.volume >= load * 2f && peer.repetitions > 0 &&
                actual.repetitions.toDouble() >= peer.repetitions * 4.0 &&
                load.toDouble() * actual.repetitions <= peer.volume.toDouble() * peer.repetitions * 2.0
        } >= 2
    }

    private fun comparableRepetitions(sample: SetIteration, actual: SetIteration, field: Field): Boolean {
        if (field == Field.BodyWeight || field == Field.BodyMultiplier) return true
        val jointIncrease = jointMagnitudeIncrease(sample, actual)
        if (field != Field.Repetitions) {
            return abs(sample.repetitions.toLong() - actual.repetitions.toLong()) <=
                maxOf(2.0, actual.repetitions.toDouble() * 0.25) ||
                (field in ADDED_WEIGHT_FIELDS && jointIncrease)
        }
        val load = actual.volume
        return load.isFinite() && load > 0f && sample.volume.isFinite() &&
            (abs(sample.volume - load) <= load * 0.25f || jointIncrease)
    }

    private fun jointMagnitudeIncrease(sample: SetIteration, actual: SetIteration): Boolean {
        if (actual.repetitions.toDouble() < sample.repetitions * 4.0) return false
        // Simultaneous large increases in weight and repetitions need checking,
        // even though neither field has a comparable rep/load range in history anymore.
        return ADDED_WEIGHT_FIELDS.any { field ->
            val before = sample.value(field)
            val after = actual.value(field)
            before != null && after != null && before > 0f && after.toDouble() / before >= 5.0
        }
    }

    private fun isGrossHistoryDeviation(actual: Float, reference: Float, field: Field): Boolean {
        val ratio = actual.toDouble() / reference.toDouble()
        return ratio >= (if (field in ADDED_WEIGHT_FIELDS) 5.0 else 8.0) || ratio <= 0.125
    }

    private fun isOutlier(actual: Float, reference: Float, field: Field, history: Boolean): Boolean {
        if (field == Field.BodyWeight) return bodyWeightMismatch(actual, reference)
        val ratio = actual.toDouble() / reference.toDouble()
        val minimumDifference = when (field) {
            Field.Repetitions -> 8f
            Field.BodyMultiplier -> 0.05f
            else -> 2.5f
        }
        if (abs(actual - reference) < minimumDifference) return false
        // Low reps may mean singles, fatigue or a failed attempt.
        if (field == Field.Repetitions) return ratio >= 4.0
        return ratio >= 3.0 || ratio <= if (history) 0.125 else 1.0 / 3.0
    }

    private fun isRamp(index: Int, iterations: List<SetIteration>, field: Field, actual: Float): Boolean {
        fun gradual(from: Float?, to: Float?): Boolean =
            from != null && to != null && from.isFinite() && to.isFinite() &&
                from > 0f && to > 0f && from <= to && from / to >= 0.2f

        val next = iterations.getOrNull(index + 1)?.value(field)
        val warmup = next != null && next > actual &&
            (0..index).all { pos -> gradual(iterations[pos].value(field), iterations[pos + 1].value(field)) }
        if (warmup) return true
        val previous = iterations.getOrNull(index - 1)?.value(field)
        return previous != null && previous > actual &&
            (index until iterations.size).all { pos ->
                gradual(iterations[pos].value(field), iterations[pos - 1].value(field))
            }
    }

    private fun outlier(field: Field, actual: Float, reference: ValidationReference, reason: Reason): Artifact =
        Artifact(field, reason, actual, reference.value, reference.count)

    private fun SetIteration.value(field: Field): Float? = when (field) {
        Field.ExternalWeight -> externalWeight
        Field.ExtraWeight -> extraWeight
        Field.AssistWeight -> assistWeight
        Field.BodyWeight -> bodyWeight
        Field.BodyMultiplier -> bodyMultiplier
        Field.Repetitions -> repetitions.toFloat()
        Field.Load -> volume
    }

    private fun Iteration.validationIteration(): SetIteration = SetIteration(
        assistWeight, extraWeight, externalWeight, bodyWeight, bodyMultiplier, repetitions,
    )

    private fun validateComponents(iteration: SetIteration, context: ExerciseValidationContext): List<Artifact> = buildList {
        for (field in COMPONENT_FIELDS) {
            val value = iteration.value(field) ?: continue
            when {
                !value.isFinite() -> add(Artifact(field, Reason.NonFinite, value))
                value < 0f -> add(Artifact(field, Reason.Negative, value))
                value == 0f && field in BODY_FIELDS ->
                    add(Artifact(field, Reason.NonPositive, value))
            }
        }
        if (iteration.repetitions <= 0) {
            add(Artifact(Field.Repetitions, Reason.NonPositive, iteration.repetitions.toFloat()))
        }
        val components = context.components
        if (components != null) {
            val allowed = when (components) {
                is ExerciseExampleComponents.External -> setOf(Field.ExternalWeight)
                is ExerciseExampleComponents.BodyOnly -> BODY_FIELDS
                is ExerciseExampleComponents.BodyAndExtra -> BODY_FIELDS + Field.ExtraWeight
                is ExerciseExampleComponents.BodyAndAssist -> BODY_FIELDS + Field.AssistWeight
            }
            for (field in COMPONENT_FIELDS - allowed) {
                iteration.value(field)?.let { add(Artifact(field, Reason.UnexpectedComponent, it)) }
            }
            val required = when (components) {
                is ExerciseExampleComponents.External -> if (components.required) setOf(Field.ExternalWeight) else emptySet()
                is ExerciseExampleComponents.BodyOnly -> if (components.required) BODY_FIELDS else emptySet()
                is ExerciseExampleComponents.BodyAndExtra ->
                    (if (components.bodyRequired) BODY_FIELDS else emptySet()) +
                        (if (components.extraRequired) setOf(Field.ExtraWeight) else emptySet())
                is ExerciseExampleComponents.BodyAndAssist ->
                    (if (components.bodyRequired) BODY_FIELDS else emptySet()) +
                        (if (components.assistRequired) setOf(Field.AssistWeight) else emptySet())
            }
            for (field in required) {
                if (iteration.value(field) == null) add(Artifact(field, Reason.MissingRequired, null))
                if (field == Field.ExternalWeight && iteration.externalWeight == 0f) {
                    add(Artifact(field, Reason.NonPositive, 0f))
                }
            }
            val multiplier = when (components) {
                is ExerciseExampleComponents.External -> null
                is ExerciseExampleComponents.BodyOnly -> components.multiplier
                is ExerciseExampleComponents.BodyAndExtra -> components.bodyMultiplier
                is ExerciseExampleComponents.BodyAndAssist -> components.bodyMultiplier
            }
            val actual = iteration.bodyMultiplier
            // Allow UI rounding and small catalog changes to previously recorded coefficients.
            if (multiplier != null && multiplier.isFinite() && multiplier > 0f &&
                actual != null && actual.isFinite() && actual > 0f &&
                abs(actual - multiplier) > maxOf(0.011f, multiplier * 0.25f)
            ) add(Artifact(Field.BodyMultiplier, Reason.ComponentMismatch, actual, multiplier))
        }
        if (iteration.bodyWeight != null && iteration.bodyMultiplier == null &&
            none { it.field == Field.BodyMultiplier }
        ) add(Artifact(Field.BodyMultiplier, Reason.MissingRequired, null))

        val reference = context.bodyWeight
        val actual = iteration.bodyWeight
        if (reference != null && reference.isFinite() && reference > 0f &&
            actual != null && actual.isFinite() && actual > 0f &&
            bodyWeightMismatch(actual, reference) && none { it.field == Field.BodyWeight }
        ) add(Artifact(Field.BodyWeight, Reason.BodyWeightMismatch, actual, reference, 1))

        // Negative effective load may be a legitimate machine convention; assistance is
        // not bounded by body weight without machine metadata. Only arithmetic overflow is flagged.
        if (COMPONENT_FIELDS.all { iteration.value(it)?.isFinite() != false } && !iteration.volume.isFinite()) {
            add(Artifact(Field.Load, Reason.NonFinite, iteration.volume))
        }
    }

    /** Body mass is a component measurement, so it does not use the lifting-weight typo threshold. */
    private fun bodyWeightMismatch(actual: Float, reference: Float): Boolean =
        abs(actual - reference) >= 10f &&
            (actual.toDouble() / reference >= 1.5 || actual.toDouble() / reference <= 2.0 / 3.0)

    private data class ValidationReference(val value: Float, val count: Int)

    /** Repeated, coherent values are stronger evidence than a median of unrelated set types. */
    private fun validationReference(values: List<Float>, minimumCount: Int): ValidationReference? {
        val finite = values.filter { it.isFinite() && it > 0f }
        if (finite.size < minimumCount) return null
        val groups = finite.map { center ->
            finite.filter { abs(it - center) <= center * 0.20f }
        }
        val largest = groups.maxOf { it.size }
        if (largest < minimumCount || largest * 2 < finite.size) return null
        val candidates = groups.filter { it.size == largest }.map { median(it) }
        // Equally supported, distant modes cannot establish an expected value.
        if (candidates.max() / candidates.min() > 1.5f) return null
        return ValidationReference(median(candidates), largest)
    }

    private fun median(values: List<Float>): Float {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid]
        else (sorted[mid - 1].toDouble() / 2.0 + sorted[mid].toDouble() / 2.0).toFloat()
    }

    private fun SetIteration.sameLoadModel(other: SetIteration): Boolean =
        (externalWeight != null) == (other.externalWeight != null) &&
            (bodyWeight != null) == (other.bodyWeight != null) &&
            (assistWeight != null) == (other.assistWeight != null)

    private companion object {
        private const val VALIDATION_HISTORY_DAYS = 180
        private const val MAX_HISTORY_REFERENCES = 12
        private val COMPONENT_FIELDS = setOf(
            Field.ExternalWeight, Field.ExtraWeight, Field.AssistWeight, Field.BodyWeight, Field.BodyMultiplier,
        )
        private val BODY_FIELDS = setOf(Field.BodyWeight, Field.BodyMultiplier)
        private val ADDED_WEIGHT_FIELDS = setOf(Field.ExternalWeight, Field.ExtraWeight)
    }
}
