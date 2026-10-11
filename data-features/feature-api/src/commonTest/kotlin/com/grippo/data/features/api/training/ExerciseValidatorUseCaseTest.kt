package com.grippo.data.features.api.training

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleComponents
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.training.models.ExerciseArtifacts
import com.grippo.data.features.api.training.models.ExerciseArtifacts.Field
import com.grippo.data.features.api.training.models.ExerciseArtifacts.Reason
import com.grippo.data.features.api.training.models.ExerciseValidationContext
import com.grippo.data.features.api.training.models.ExerciseValidationInput
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.data.features.api.training.models.SetIteration
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExerciseValidatorUseCaseTest {
    private val date = LocalDateTime(2026, 10, 10, 12, 0)
    private val example = ExerciseExampleValue(
        "bench", "Bench", "", null, ExperienceEnum.INTERMEDIATE,
        ForceTypeEnum.PUSH, WeightTypeEnum.FREE, CategoryEnum.COMPOUND, 0, null,
    )

    private val validator = ExerciseValidatorFixture(
        ExerciseExample(example, ExerciseExampleComponents.External(true), emptyList(), emptyList()),
    ).loader

    private fun validate(
        exercise: SetExercise,
        context: ExerciseValidationContext = ExerciseValidationContext(),
    ): ExerciseArtifacts? = validator.execute(exercise, context)

    private fun validate(
        exercise: ExerciseValidationInput,
        context: ExerciseValidationContext = ExerciseValidationContext(),
    ): ExerciseArtifacts? = validator.execute(exercise, context)

    @Test fun emptyExerciseIsClean() {
        assertNull(validate(exercise(emptyList())))
    }

    @Test fun cleanSetsAndOrdinaryProgressionAreClean() {
        for (weights in listOf(listOf(80f, 80f, 100f), listOf(80f, 82f, 100f), listOf(100f, 105f, 110f))) {
            assertNull(validate(exercise(weights.map(::set))), weights.toString())
        }
    }

    @Test fun detectsMissingDigitAfterWarmups() {
        val sets = listOf(20f, 40f, 60f, 80f, 100f, 100f, 10f).map(::set)
        val result = assertNotNull(validate(exercise(sets)))
        assertEquals(listOf(6), result.flaggedIndices())
        val finding = result.iterations.last().artifacts.single()
        assertEquals(Field.ExternalWeight, finding.field)
        assertEquals(100f, finding.reference)
    }

    @Test fun gradualWarmupsAndDropSetsAreClean() {
        for (weights in listOf(
            listOf(20f, 40f, 60f, 80f, 100f, 100f),
            listOf(100f, 100f, 60f, 30f),
            listOf(20f, 100f, 100f),
        )) assertNull(validate(exercise(weights.map(::set))), weights.toString())
    }

    @Test fun tenfoldJumpIsNotSuppressedAsWarmupOrDropSet() {
        for (weights in listOf(listOf(10f, 100f, 100f), listOf(100f, 100f, 10f))) {
            assertEquals(1, assertNotNull(validate(exercise(weights.map(::set)))).flaggedIndices().size)
        }
    }

    @Test fun excessiveWeightAtRatioBoundaryIsDetected() {
        val result = assertNotNull(validate(exercise(listOf(100f, 100f, 300f).map(::set))))
        assertEquals(listOf(2), result.flaggedIndices())
    }

    @Test fun oneOrTwoSetsDoNotInventLocalReference() {
        assertNull(validate(exercise(listOf(set(800f)))))
        assertNull(validate(exercise(listOf(set(80f), set(800f)))))
    }

    @Test fun singlesAndFatigueAreNotRepetitionTypos() {
        assertNull(validate(exercise(listOf(set(100f, 5), set(100f, 5), set(100f, 1)))))
    }

    @Test fun addedDigitInRepetitionsIsDetected() {
        val result = assertNotNull(validate(exercise(listOf(set(100f, 5), set(100f, 5), set(100f, 50)))))
        assertEquals(Field.Repetitions, result.iterations[2].artifacts.single().field)
    }

    @Test fun sameSetCanHaveWeightAndRepetitionFindings() {
        val result = assertNotNull(validate(exercise(listOf(set(100f, 5), set(100f, 5), set(1000f, 50)))))
        assertEquals(setOf(Field.ExternalWeight, Field.Repetitions), result.iterations[2].artifacts.map { it.field }.toSet())
    }

    @Test fun invalidDataIsDetectedWithoutMinimumSamples() {
        val result = assertNotNull(validate(exercise(listOf(set(-1f, 0)))))
        assertEquals(setOf(Reason.Negative, Reason.NonPositive), result.iterations.single().artifacts.map { it.reason }.toSet())
    }

    @Test fun nonFiniteComponentsDoNotPolluteReferences() {
        for (weight in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            val result = assertNotNull(validate(exercise(listOf(set(100f), set(weight), set(100f)))))
            assertEquals(listOf(1), result.flaggedIndices())
            assertEquals(Reason.NonFinite, result.iterations[1].artifacts.single().reason)
        }
    }

    @Test fun detectsLoadArithmeticOverflow() {
        val iteration = body(Float.MAX_VALUE).copy(bodyMultiplier = Float.MAX_VALUE)
        assertTrue(assertNotNull(validate(exercise(listOf(iteration)))).iterations[0].artifacts.any {
            it.field == Field.Load && it.reason == Reason.NonFinite
        })
    }

    @Test fun extraWeightTypoIsNotHiddenByBodyWeight() {
        val sets = listOf(10f, 10f, 1f).map { body(80f).copy(extraWeight = it) }
        val result = assertNotNull(validate(exercise(sets), ExerciseValidationContext(
            components = ExerciseExampleComponents.BodyAndExtra(true, 1f, false),
        )))
        assertEquals(Field.ExtraWeight, result.iterations[2].artifacts.single().field)
    }

    @Test fun assistanceTypoIsDetectedIndependentlyOfEffectiveLoad() {
        val sets = listOf(40f, 40f, 4f).map { body(80f).copy(assistWeight = it) }
        val result = assertNotNull(validate(exercise(sets), ExerciseValidationContext(
            components = ExerciseExampleComponents.BodyAndAssist(true, 1f, false),
        )))
        assertEquals(Field.AssistWeight, result.iterations[2].artifacts.single().field)
    }

    @Test fun zeroOptionalExtraAndAssistanceAreAllowed() {
        for (iteration in listOf(body(80f).copy(extraWeight = 0f), body(80f).copy(assistWeight = 0f))) {
            assertNull(validate(exercise(listOf(iteration))))
        }
    }

    @Test fun machineAssistanceCanExceedBodyWeight() {
        assertNull(validate(exercise(listOf(body(80f).copy(assistWeight = 100f)))))
    }

    @Test fun requiredComponentsAreChecked() {
        val result = assertNotNull(validate(exercise(listOf(set(null))), ExerciseValidationContext(
            components = ExerciseExampleComponents.External(true),
        )))
        assertEquals(Reason.MissingRequired, result.iterations[0].artifacts.single().reason)
    }

    @Test fun optionalExternalWeightCanBeAbsent() {
        assertNull(validate(exercise(listOf(set(null))), ExerciseValidationContext(
            components = ExerciseExampleComponents.External(false),
        )))
    }

    @Test fun incompatibleFieldsAreReported() {
        val result = assertNotNull(validate(exercise(listOf(set(100f).copy(assistWeight = 10f))), ExerciseValidationContext(
            components = ExerciseExampleComponents.External(true),
        )))
        assertEquals(Field.AssistWeight, result.iterations[0].artifacts.single().field)
        assertEquals(Reason.UnexpectedComponent, result.iterations[0].artifacts.single().reason)
    }

    @Test fun multiplierIsCheckedAgainstCatalogWithRoundingTolerance() {
        val context = ExerciseValidationContext(components = ExerciseExampleComponents.BodyOnly(true, 0.666f))
        assertNull(validate(exercise(listOf(body(80f).copy(bodyMultiplier = 0.67f))), context))
        assertEquals(Reason.ComponentMismatch, assertNotNull(validate(exercise(listOf(body(80f))), context))
            .iterations[0].artifacts.single().reason)
    }

    @Test fun bodyWeightRequiresMultiplierEvenWithoutCatalog() {
        val result = assertNotNull(validate(exercise(listOf(body(80f).copy(bodyMultiplier = null)))))
        assertEquals(Field.BodyMultiplier, result.iterations[0].artifacts.single().field)
    }

    @Test fun personalWeightCanDetectUniformBodyWeightTypo() {
        val result = assertNotNull(validate(exercise(List(3) { body(8f) }), ExerciseValidationContext(bodyWeight = 80f)))
        assertTrue(result.iterations.all { it.artifacts.single().reason == Reason.BodyWeightMismatch })
    }

    @Test fun historyDetectsTypoInFirstSet() {
        val result = assertNotNull(validate(exercise(listOf(set(800f))), historyContext()))
        val finding = result.iterations.single().artifacts.single()
        assertEquals(Reason.HistoryOutlier, finding.reason)
        assertEquals(80f, finding.reference)
        assertEquals(3, finding.referenceCount)
    }

    @Test fun uniformTenfoldMistakesAreDetectedWithHistory() {
        for (weight in listOf(8f, 800f)) {
            val result = assertNotNull(validate(exercise(List(3) { set(weight) }), historyContext()))
            assertEquals(listOf(0, 1, 2), result.flaggedIndices())
        }
    }

    @Test fun deliberateNewSchemeAndDeloadAreClean() {
        assertNull(validate(exercise(List(3) { set(240f) }), historyContext()))
        assertNull(validate(exercise(List(3) { set(40f) }), historyContext()))
    }

    @Test fun historyRequiresThreeIndependentSessions() {
        val session = session("same", List(20) { set(80f) })
        assertNull(validate(exercise(listOf(set(800f))), ExerciseValidationContext(history = List(3) { session })))
    }

    @Test fun futureStaleOtherExerciseAndEditedHistoryAreExcluded() {
        val invalid = listOf(
            session("future").copy(createdAt = LocalDateTime(2026, 10, 11, 12, 0)),
            session("stale").copy(createdAt = LocalDateTime(2025, 10, 9, 12, 0)),
            session("other").copy(exerciseExample = example.copy(id = "squat")),
            session("editing"),
        )
        assertNull(validate(exercise(listOf(set(800f))), ExerciseValidationContext(history = invalid, excludedExerciseId = "editing")))
    }

    @Test fun incompatibleRepRangesDoNotCreateHistoricalWarning() {
        assertNull(validate(exercise(listOf(set(800f, 1))), historyContext()))
    }

    @Test fun invalidHistoryCannotEstablishReference() {
        val context = historyContext().copy(history = List(3) { session("bad-$it", listOf(set(Float.NaN))) })
        assertNull(validate(exercise(listOf(set(800f))), context))
    }

    @Test fun allRowsAndOriginalOrderArePreserved() {
        val sets = listOf(set(100f), set(100f), set(1000f))
        val result = assertNotNull(validate(exercise(sets)))
        assertEquals(sets, result.iterations.map { it.iteration })
        assertTrue(result.iterations.take(2).all { it.artifacts.isEmpty() })
    }

    @Test fun checkingMultipleOutliersDoesNotChangeReferenceDuringIteration() {
        val result = assertNotNull(validate(exercise(listOf(100f, 100f, 100f, 1000f, 1000f).map(::set))))
        assertEquals(listOf(3, 4), result.flaggedIndices())
    }

    @Test fun equallySupportedModesDoNotAccuseEachOther() {
        assertNull(validate(exercise(listOf(100f, 100f, 1000f, 1000f).map(::set))))
    }

    @Test fun manyRepetitionsWithMuchLighterLoadAreNotAutomaticallyTypos() {
        assertNull(validate(exercise(listOf(set(20f, 30), set(100f, 5), set(100f, 5)))))
    }

    @Test fun ascendingSequenceCannotHideExcessiveHighWeight() {
        val result = assertNotNull(validate(exercise(listOf(100f, 100f, 300f, 500f).map(::set))))
        assertEquals(listOf(2, 3), result.flaggedIndices())
    }

    @Test fun zeroRequiredExternalWeightIsDetected() {
        val result = assertNotNull(validate(exercise(listOf(set(0f))), ExerciseValidationContext(
            components = ExerciseExampleComponents.External(true),
        )))
        assertEquals(Reason.NonPositive, result.iterations.single().artifacts.single().reason)
    }

    @Test fun missingBodyComponentsAndZeroMultiplierAreDetected() {
        val context = ExerciseValidationContext(components = ExerciseExampleComponents.BodyOnly(true, 1f))
        val missing = assertNotNull(validate(exercise(listOf(set(null))), context))
        assertEquals(setOf(Field.BodyWeight, Field.BodyMultiplier), missing.iterations.single().artifacts.map { it.field }.toSet())
        val zero = assertNotNull(validate(exercise(listOf(body(80f).copy(bodyMultiplier = 0f))), context))
        assertEquals(Reason.NonPositive, zero.iterations.single().artifacts.single().reason)
    }

    @Test fun historyChecksRepetitionTyposAtComparableLoad() {
        val result = assertNotNull(validate(exercise(listOf(set(80f, 50))), historyContext()))
        assertEquals(Field.Repetitions, result.iterations.single().artifacts.single().field)
        assertEquals(Reason.HistoryOutlier, result.iterations.single().artifacts.single().reason)
    }

    @Test fun historyReferenceSeparatesRepeatedWorkingWeightFromWarmups() {
        val context = historyContext().copy(history = List(3) { index ->
            session("warmup-$index", listOf(20f, 40f, 60f, 80f, 100f, 100f).map(::set))
                .copy(createdAt = historyDate(index))
        })
        val finding = assertNotNull(validate(exercise(listOf(set(10f))), context))
            .iterations.single().artifacts.single()
        assertEquals(100f, finding.reference)
    }

    @Test fun sessionWithManySetsDoesNotOutvoteOtherSessions() {
        val context = ExerciseValidationContext(history = listOf(
            session("huge", List(100) { set(800f) }),
            session("normal-1"), session("normal-2"), session("normal-3"),
        ).mapIndexed { index, session -> session.copy(createdAt = historyDate(index)) })
        val result = assertNotNull(validate(exercise(listOf(set(800f))), context))
        assertEquals(80f, result.iterations.single().artifacts.single().reference)
    }

    @Test fun permutationDoesNotChangeHighOutlierDetection() {
        val sets = listOf(set(100f), set(100f), set(1000f))
        for (order in listOf(listOf(0, 1, 2), listOf(2, 0, 1), listOf(1, 2, 0))) {
            val result = assertNotNull(validate(exercise(order.map(sets::get))))
            assertEquals(listOf(1000f), result.iterations.filter { it.artifacts.isNotEmpty() }.map { it.iteration.externalWeight })
        }
    }

    @Test fun tinyLoadDifferencesDoNotBecomeRatioWarnings() {
        assertNull(validate(exercise(listOf(0.1f, 0.1f, 0.4f).map(::set))))
    }

    @Test fun finiteExtremeValuesDoNotOverflowMedian() {
        assertNull(validate(exercise(List(4) { set(Float.MAX_VALUE) })))
    }

    @Test fun corruptHistoryComponentsCannotEstablishReference() {
        val context = ExerciseValidationContext(
            components = ExerciseExampleComponents.External(true),
            history = List(3) { session("corrupt-$it", listOf(set(80f).copy(bodyWeight = 80f, bodyMultiplier = 1f))) },
        )
        assertNull(validate(exercise(listOf(set(800f))), context))
    }

    @Test fun loadModelsFromHistoryAreNotMixed() {
        val context = ExerciseValidationContext(history = List(3) { session("body-$it", listOf(body(80f))) })
        assertNull(validate(exercise(listOf(set(800f))), context))
    }

    @Test fun unknownDateAllowsLocalChecksButCannotUseHistory() {
        val input = ExerciseValidationInput("Bench", example.id, listOf(set(800f)), null)
        assertNull(validate(input, historyContext()))
        val local = input.copy(iterations = listOf(set(80f), set(80f), set(800f)))
        assertEquals(listOf(2), assertNotNull(validate(local)).flaggedIndices())
    }

    @Test fun historyCanConfirmHeavyWorkingSetAfterRepeatedLightWarmups() {
        val context = historyContext().copy(history = List(3) { index ->
            session("heavy-$index", listOf(set(300f, 1))).copy(createdAt = historyDate(index))
        })
        assertNull(validate(exercise(listOf(set(100f), set(100f), set(300f, 1))), context))
    }

    @Test fun multipleExerciseRecordsOnOneDayAreNotIndependentHistory() {
        val context = ExerciseValidationContext(history = List(3) { session("same-day-$it") })
        assertNull(validate(exercise(listOf(set(800f))), context))
    }

    @Test fun grossBodyWeightMismatchWithinUiRangeIsDetected() {
        val result = assertNotNull(validate(exercise(listOf(body(140f))), ExerciseValidationContext(bodyWeight = 80f)))
        assertEquals(Reason.BodyWeightMismatch, result.iterations.single().artifacts.single().reason)
        assertNull(validate(exercise(listOf(body(95f))), ExerciseValidationContext(bodyWeight = 80f)))
    }

    @Test fun simultaneousTenfoldWeightAndRepetitionTyposAreDetectedByHistory() {
        val result = assertNotNull(validate(exercise(listOf(set(800f, 50))), historyContext()))
        assertEquals(setOf(Field.ExternalWeight, Field.Repetitions), result.iterations.single().artifacts.map { it.field }.toSet())
    }

    @Test fun establishedAlternativeSchemeIsNotRejectedByOlderMajority() {
        val context = ExerciseValidationContext(history = List(12) { index ->
            session("scheme-$index", List(3) { set(if (index < 3) 10f else 100f) })
                .copy(createdAt = historyDate(index))
        })
        assertNull(validate(exercise(List(3) { set(10f) }), context))
    }

    @Test fun uniformDigitMistakeIsStillDetectedAfterRecentProgress() {
        val context = ExerciseValidationContext(history = List(3) { index ->
            session("progress-$index", List(3) { set(110f) }).copy(createdAt = historyDate(index))
        })
        val result = assertNotNull(validate(exercise(List(3) { set(800f) }), context))
        assertEquals(listOf(0, 1, 2), result.flaggedIndices())
    }

    @Test fun smallCatalogMultiplierAdjustmentDoesNotInvalidateRecordedSets() {
        val context = ExerciseValidationContext(components = ExerciseExampleComponents.BodyOnly(true, 1f))
        assertNull(validate(exercise(listOf(body(80f).copy(bodyMultiplier = 0.98f))), context))
    }

    @Test fun ambiguousHistoricalModesDoNotCreateAnArtificialReference() {
        val context = ExerciseValidationContext(history = List(3) { index ->
            session("mixed-$index", listOf(set(10f), set(1000f))).copy(createdAt = historyDate(index))
        })
        assertNull(validate(exercise(List(3) { set(10f) }), context))
    }

    @Test fun independentSingleSetSessionsCanEstablishHistory() {
        val context = ExerciseValidationContext(history = List(3) { index ->
            session("single-$index", listOf(set(80f))).copy(createdAt = historyDate(index))
        })
        assertNotNull(validate(exercise(List(3) { set(800f) }), context))
    }

    private fun set(weight: Float?, repetitions: Int = 5): SetIteration =
        SetIteration(null, null, weight, null, null, repetitions)

    private fun body(weight: Float): SetIteration = SetIteration(null, null, null, weight, 1f, 5)

    private fun exercise(sets: List<SetIteration>): SetExercise = SetExercise(
        "Bench", sets, example, sets.sumOf { it.repetitions }, 0f, 0f, date,
    )

    private fun session(id: String, sets: List<SetIteration> = List(3) { set(80f) }): Exercise = Exercise(
        id, "Bench", sets.mapIndexed { index, set ->
            Iteration("$id-$index", set.externalWeight, set.assistWeight, set.extraWeight, set.bodyWeight, set.bodyMultiplier, set.repetitions)
        }, example, LocalDateTime(2026, 10, 9, 12, 0), 0f, 0, 0f,
    )

    private fun historyContext(): ExerciseValidationContext = ExerciseValidationContext(history = List(3) {
        session("session-$it").copy(createdAt = historyDate(it))
    })

    private fun historyDate(index: Int): LocalDateTime = date.date.minus(DatePeriod(days = index + 1)).atTime(12, 0)

    private fun ExerciseArtifacts.flaggedIndices(): List<Int> = iterations.indices.filter { iterations[it].artifacts.isNotEmpty() }
}
