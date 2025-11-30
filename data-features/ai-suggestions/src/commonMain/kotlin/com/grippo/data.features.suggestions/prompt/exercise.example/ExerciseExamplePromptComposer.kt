package com.grippo.data.features.suggestions.prompt.exercise.example

import com.grippo.data.features.suggestions.prompt.exercise.example.model.ExampleContext
import com.grippo.data.features.suggestions.prompt.exercise.example.model.PredictionSignals
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.CandidatesSection
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.CategoryBalanceSection
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.ExperienceMixSection
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.ForceMixSection
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.GuidelinesSection
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.IntroSection
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.MuscleLoadSection
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.MuscleTargetsSection
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.PeriodicHabitsSection
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.RecentTrainingsSection
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.ResidualFatigueSection
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.SessionSection
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.WeightMixSection
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.ExercisePromptSection
import kotlinx.datetime.LocalDateTime

internal class ExerciseExamplePromptComposer(
    now: LocalDateTime,
    signals: PredictionSignals,
    candidates: List<ExampleContext>,
    locale: String
) {
    private val sections: List<ExercisePromptSection> = listOf(
        IntroSection(now, locale),
        SessionSection(signals.session),
        CategoryBalanceSection(signals.categoryStats),
        MuscleTargetsSection(signals.muscleTargets),
        ForceMixSection(signals.forceMix),
        WeightMixSection(signals.weightMix),
        ExperienceMixSection(signals.experienceMix),
        GuidelinesSection(signals.experienceMix, locale),
        RecentTrainingsSection(signals.trainings),
        MuscleLoadSection(signals.muscleLoads, signals.trainings.size),
        ResidualFatigueSection(signals.residualFatigueByMuscle, signals.muscleTargets),
        PeriodicHabitsSection(now, signals.periodicHabits),
        CandidatesSection(now, signals, candidates)
    )

    fun compose(): String = buildString {
        sections.forEach { section -> section.render(this) }
    }
}
