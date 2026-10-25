package com.grippo.data.features.api.exercise.example

import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleParams
import com.grippo.data.features.api.exercise.example.models.ExampleScope
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleComponents
import com.grippo.data.features.api.exercise.example.models.UserExerciseExampleRules
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.DraftTraining
import com.grippo.data.features.api.user.UserFeature
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil

public class UserExerciseExamplesUseCase(
    private val excludedMusclesFeature: ExcludedMusclesFeature,
    private val excludedEquipmentsFeature: ExcludedEquipmentsFeature,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val userFeature: UserFeature,
    private val trainingFeature: TrainingFeature,
) {

    public fun execute(params: ExampleParams): Flow<List<ExerciseExample>> {
        return observeRules().flatMapLatest { rules ->
            when (val scope = params.scope) {
                is ExampleScope.All -> observeCatalog(params = params, rules = rules)
                is ExampleScope.SimilarTo -> observeReplacements(
                    params = params,
                    scope = scope,
                    rules = rules,
                )
            }
        }
    }

    private fun observeRules(): Flow<UserExerciseExampleRules> {
        return combine(
            flow = excludedEquipmentsFeature
                .observeExcludedEquipments()
                .map { items -> items.map { it.id }.toSet() },
            flow2 = excludedMusclesFeature
                .observeExcludedMuscles()
                .map { items -> items.map { it.id }.toSet() },
            flow3 = userFeature
                .observeUser()
                .mapNotNull { items -> items?.experience },
            transform = { equipmentIds, muscleIds, experience ->
                UserExerciseExampleRules(
                    excludedEquipmentIds = equipmentIds,
                    excludedMuscleIds = muscleIds,
                    experience = experience
                )
            }
        ).distinctUntilChanged()
    }

    private fun observeCatalog(
        params: ExampleParams,
        rules: UserExerciseExampleRules,
    ): Flow<List<ExerciseExample>> {
        return exerciseExampleFeature.observeExerciseExamples(
            name = params.name,
            scope = params.scope,
            rules = rules,
            page = params.page,
            experience = rules.experience
        )
    }

    private fun observeReplacements(
        params: ExampleParams,
        scope: ExampleScope.SimilarTo,
        rules: UserExerciseExampleRules,
    ): Flow<List<ExerciseExample>> {
        val candidates = exerciseExampleFeature.observeExerciseExamples(
            name = params.name,
            scope = scope,
            rules = rules,
            page = ExamplePage.Unlimited,
            experience = rules.experience
        )

        val target = exerciseExampleFeature.observeExerciseExample(scope.targetId)

        val draft = trainingFeature.getDraftTraining()

        return combine(
            flow = candidates,
            flow2 = target,
            flow3 = draft,
            transform = { examples, targetExample, draftTraining ->
                val resolvedTarget = targetExample ?: return@combine emptyList()

                val shortlist = rankReplacements(
                    candidates = examples,
                    target = resolvedTarget,
                    draft = draftTraining,
                    now = DateTimeUtils.now(),
                )

                params.page.slice(shortlist)
            }
        )
    }

    private fun rankReplacements(
        candidates: List<ExerciseExample>,
        target: ExerciseExample,
        draft: DraftTraining?,
        now: LocalDateTime,
    ): List<ExerciseExample> {
        val draftExampleIds = draft?.exercises.orEmpty().map { it.exerciseExample.id }.toSet()

        return candidates
            .filter { it.value.id !in draftExampleIds }
            .map { candidate -> score(candidate = candidate, target = target, now = now) }
            .sortedWith(
                compareByDescending<Replacement> { it.score }
                    .thenByDescending { it.example.value.lastUsed }
                    .thenByDescending { it.example.value.usageCount }
                    .thenBy { it.example.value.name }
                    .thenBy { it.example.value.id }
            )
            .filterIndexed { index, replacement ->
                index < SHORTLIST_SIZE || replacement.overlap >= MIN_OVERLAP
            }
            .map { it.example }
    }

    private fun score(
        candidate: ExerciseExample,
        target: ExerciseExample,
        now: LocalDateTime,
    ): Replacement {
        val overlap = bundleOverlap(candidate = candidate, target = target)
        val sameLoadModel = candidate.value.weightType == target.value.weightType &&
                sameComponentsFamily(candidate = candidate.components, target = target.components)
        val used = candidate.value.usageCount > 0
        val lastUsed = candidate.value.lastUsed
        val recentlyUsed = lastUsed != null && lastUsed.date.daysUntil(now.date) <= RECENT_DAYS
        val sameCategory = candidate.value.category == target.value.category
        val sameForceType = candidate.value.forceType == target.value.forceType

        val score = overlap * OVERLAP_WEIGHT +
                (if (sameLoadModel) LOAD_MODEL_BONUS else 0.0) +
                (if (used) USED_BONUS else 0.0) +
                (if (recentlyUsed) RECENTLY_USED_BONUS else 0.0) +
                (if (sameCategory) CATEGORY_BONUS else 0.0) +
                (if (sameForceType) FORCE_TYPE_BONUS else 0.0)

        return Replacement(example = candidate, overlap = overlap, score = score)
    }

    private fun sameComponentsFamily(
        candidate: ExerciseExampleComponents,
        target: ExerciseExampleComponents,
    ): Boolean {
        return when (candidate) {
            is ExerciseExampleComponents.External -> target is ExerciseExampleComponents.External
            is ExerciseExampleComponents.BodyOnly -> target is ExerciseExampleComponents.BodyOnly
            is ExerciseExampleComponents.BodyAndExtra -> target is ExerciseExampleComponents.BodyAndExtra
            is ExerciseExampleComponents.BodyAndAssist -> target is ExerciseExampleComponents.BodyAndAssist
        }
    }

    private fun bundleOverlap(candidate: ExerciseExample, target: ExerciseExample): Double {
        val targetTotal = target.bundles.sumOf { it.percentage }
        if (targetTotal <= 0) return 0.0

        val candidatePercentages = candidate.bundles.associate { it.muscle.id to it.percentage }

        val shared = target.bundles.sumOf { bundle ->
            minOf(bundle.percentage, candidatePercentages[bundle.muscle.id] ?: 0)
        }

        return (shared.toDouble() / targetTotal).coerceIn(0.0, 1.0)
    }

    private data class Replacement(
        val example: ExerciseExample,
        val overlap: Double,
        val score: Double,
    )

    private companion object {
        const val OVERLAP_WEIGHT = 3.0
        const val LOAD_MODEL_BONUS = 1.5
        const val USED_BONUS = 1.0
        const val RECENTLY_USED_BONUS = 0.5
        const val CATEGORY_BONUS = 0.5
        const val FORCE_TYPE_BONUS = 0.5
        const val RECENT_DAYS = 60
        const val MIN_OVERLAP = 0.3
        const val SHORTLIST_SIZE = 10
    }
}
