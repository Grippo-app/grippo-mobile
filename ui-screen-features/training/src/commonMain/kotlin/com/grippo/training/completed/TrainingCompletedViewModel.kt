package com.grippo.training.completed

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.metrics.MuscleLoadingMessageUseCase
import com.grippo.data.features.api.metrics.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.metrics.TrainingTotalUseCase
import com.grippo.data.features.api.metrics.models.MuscleLoadingMessage
import com.grippo.data.features.api.metrics.models.MuscleLoadingMessageKind
import com.grippo.data.features.api.metrics.models.MuscleLoadingMessageStrength
import com.grippo.data.features.api.metrics.models.MuscleLoadingMessageSuffix
import com.grippo.data.features.api.metrics.models.MuscleLoadingMessageTone
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.TrainingTimelineUseCase
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.metrics.toState
import com.grippo.domain.state.training.toState
import com.grippo.state.domain.training.toDomain
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.LocalDateTime

internal class TrainingCompletedViewModel(
    stage: StageState,
    exercises: List<ExerciseState>,
    trainingFeature: TrainingFeature,
    startAt: LocalDateTime,
    private val trainingTotalUseCase: TrainingTotalUseCase,
    private val dialogController: DialogController,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val trainingTimelineUseCase: TrainingTimelineUseCase,
    private val muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
    private val muscleLoadingMessageUseCase: MuscleLoadingMessageUseCase,
) : BaseViewModel<TrainingCompletedState, TrainingCompletedDirection, TrainingCompletedLoader>(
    TrainingCompletedState()
), TrainingCompletedContract {

    init {
        safeLaunch(loader = TrainingCompletedLoader.SaveTraining) {
            val duration = DateTimeUtils.ago(value = startAt)

            val domainExercises = exercises.toDomain()

            val totals = trainingTotalUseCase
                .fromSetExercises(domainExercises)
                .toState()

            val training = SetTraining(
                exercises = domainExercises,
                duration = duration,
                volume = totals.volume.value ?: 0f,
                intensity = totals.intensity.value ?: 0f,
                repetitions = totals.repetitions.value ?: 0
            )

            val id = when (val allocatedId = stage.id) {
                null -> {
                    val result = trainingFeature
                        .setTraining(training)
                        .getOrThrow() ?: return@safeLaunch
                    result
                }

                else -> {
                    val result = trainingFeature
                        .updateTraining(allocatedId, training)
                        .getOrThrow() ?: return@safeLaunch
                    result
                }
            }

            trainingFeature.deleteDraftTraining().getOrThrow()

            val domain = trainingFeature.observeTraining(id).firstOrNull()

            exerciseExampleFeature.getExerciseExamples().getOrThrow()

            provideTraining(domain)
        }
    }

    private suspend fun provideTraining(value: Training?) {
        value ?: return
        val timeline = trainingTimelineUseCase
            .trainingExercises(value)

        val summary = muscleLoadingSummaryUseCase
            .fromTraining(value)


        val message = muscleLoadingMessageUseCase
            .fromSummary(
                summary = summary,
                isFirstWorkout = false
            )

        update {
            it.copy(
                timeline = timeline.toState(),
                training = value.toState(),
                summary = summary.toState(),
                message = message.toUserText(
                    groupName = { g -> g.name },
                    muscleName = { m -> m.name }
                )
            )
        }
    }

    override fun onExerciseClick(id: String) {
        val dialog = DialogConfig.Exercise(id = id)

        dialogController.show(dialog)
    }

    fun MuscleLoadingMessage.toUserText(
        groupName: (MuscleGroupEnum) -> String,
        muscleName: (MuscleEnum) -> String,
    ): String {
        fun safeIndex(size: Int): Int {
            if (size <= 0) return 0
            val m = variant % size
            return if (m < 0) m + size else m
        }

        fun pick(templates: List<String>): String {
            if (templates.isEmpty()) return ""
            return templates[safeIndex(templates.size)]
        }

        fun namePrimary(): String? {
            val p = primary ?: return null
            return p.muscle?.let(muscleName) ?: groupName(p.group)
        }

        fun namePrimaryGroup(): String? = primary?.let { groupName(it.group) }
        fun nameSecondaryGroup(): String? = secondary?.let { groupName(it.group) }

        fun suffixText(): String {
            val templates = when (suffix) {
                MuscleLoadingMessageSuffix.None -> emptyList()
                MuscleLoadingMessageSuffix.First -> listOf(
                    " · Starting point saved",
                    " · Baseline locked in",
                    " · First session recorded",
                )

                MuscleLoadingMessageSuffix.Regular -> listOf(
                    " · Added to your history",
                    " · Logged for progress",
                    " · Saved for tracking",
                )
            }
            return pick(templates)
        }

        fun mixedText(): String {
            val base = pick(
                when (tone) {
                    MuscleLoadingMessageTone.Compact -> listOf(
                        "Mixed focus",
                        "Varied load",
                        "A bit of everything",
                        "General session",
                    )

                    MuscleLoadingMessageTone.Normal -> listOf(
                        "Mixed focus across groups",
                        "Varied load today",
                        "No single clear driver",
                        "Multiple groups shared the work",
                    )
                }
            )

            val why = pick(
                when (tone) {
                    MuscleLoadingMessageTone.Compact -> listOf(
                        " · Diverse",
                        " · Spread out",
                        " · Wide coverage",
                    )

                    MuscleLoadingMessageTone.Normal -> listOf(
                        " · Diverse coverage across groups",
                        " · Load was spread across several groups",
                        " · Good variety in the session",
                    )
                }
            )

            return base + why
        }

        fun dominantText(): String {
            val a = namePrimary() ?: return mixedText()

            val base = pick(
                when (tone) {
                    MuscleLoadingMessageTone.Compact -> listOf(
                        "{A} led",
                        "Main driver: {A}",
                        "{A} carried it",
                        "{A} stood out",
                        "Heavy on {A}",
                        "{A} took over",
                    )

                    MuscleLoadingMessageTone.Normal -> listOf(
                        "{A} carried this session",
                        "Main driver was {A}",
                        "Most work went into {A}",
                        "{A} clearly stood out today",
                        "The session leaned toward {A}",
                        "{A} did the heavy lifting today",
                    )
                }
            ).replace("{A}", a)

            val why = when (strength) {
                MuscleLoadingMessageStrength.Strong -> pick(
                    when (tone) {
                        MuscleLoadingMessageTone.Compact -> listOf(
                            " · Clear focus",
                            " · Strong signal",
                            " · Big emphasis"
                        )

                        MuscleLoadingMessageTone.Normal -> listOf(
                            " · Clear focus across sets",
                            " · Strong emphasis throughout",
                            " · A clear standout today"
                        )
                    }
                )

                MuscleLoadingMessageStrength.Medium -> pick(
                    when (tone) {
                        MuscleLoadingMessageTone.Compact -> listOf(
                            " · Noticeable",
                            " · Leaning there",
                            " · Slight edge"
                        )

                        MuscleLoadingMessageTone.Normal -> listOf(
                            " · Noticeable emphasis",
                            " · Slightly more work there",
                            " · It had a small edge today"
                        )
                    }
                )

                MuscleLoadingMessageStrength.Light -> pick(
                    when (tone) {
                        MuscleLoadingMessageTone.Compact -> listOf(
                            " · Minor edge",
                            " · Small tilt",
                            " · Light focus"
                        )

                        MuscleLoadingMessageTone.Normal -> listOf(
                            " · Just a small tilt",
                            " · Light emphasis overall",
                            " · A minor edge, not extreme"
                        )
                    }
                )

                MuscleLoadingMessageStrength.None -> ""
            }

            return base + why
        }

        fun dualText(): String {
            val a = namePrimaryGroup() ?: return mixedText()
            val b = nameSecondaryGroup() ?: return mixedText()

            val base = pick(
                when (tone) {
                    MuscleLoadingMessageTone.Compact -> listOf(
                        "{A} + {B} split",
                        "Two-way: {A} & {B}",
                        "{A} with {B}",
                        "{A} + {B} led",
                        "Shared between {A}/{B}",
                    )

                    MuscleLoadingMessageTone.Normal -> listOf(
                        "Two-way session: {A} and {B}",
                        "Most work was shared between {A} and {B}",
                        "{A} led, with strong support from {B}",
                        "The session split between {A} and {B}",
                        "Main focus was {A} + {B}",
                    )
                }
            )
                .replace("{A}", a)
                .replace("{B}", b)

            val why = pick(
                when (tone) {
                    MuscleLoadingMessageTone.Compact -> listOf(
                        " · Shared load",
                        " · Two main drivers",
                        " · Split focus"
                    )

                    MuscleLoadingMessageTone.Normal -> listOf(
                        " · Two main drivers today",
                        " · Load was shared between two groups",
                        " · A clear two-group split"
                    )
                }
            )

            return base + why
        }

        fun balancedText(): String {
            val base = pick(
                when (tone) {
                    MuscleLoadingMessageTone.Compact -> listOf(
                        "Balanced load",
                        "Even spread",
                        "Well-rounded",
                        "No single driver",
                        "Clean balance",
                    )

                    MuscleLoadingMessageTone.Normal -> listOf(
                        "Balanced load across the session",
                        "Work was spread evenly",
                        "A well-rounded session overall",
                        "No single group dominated",
                        "Even distribution across groups",
                    )
                }
            )

            val why = pick(
                when (tone) {
                    MuscleLoadingMessageTone.Compact -> listOf(
                        " · Consistent",
                        " · Smooth",
                        " · Good spread"
                    )

                    MuscleLoadingMessageTone.Normal -> listOf(
                        " · Great for consistent tracking",
                        " · Smooth distribution across groups",
                        " · A good sign of balance"
                    )
                }
            )

            return base + why
        }

        fun emptyText(): String {
            return pick(
                when (tone) {
                    MuscleLoadingMessageTone.Compact -> listOf(
                        "Workout saved",
                        "Session saved",
                        "Logged"
                    )

                    MuscleLoadingMessageTone.Normal -> listOf(
                        "Workout saved successfully",
                        "Session saved",
                        "Workout logged"
                    )
                }
            )
        }

        val base = when (kind) {
            MuscleLoadingMessageKind.DominantGroup -> dominantText()
            MuscleLoadingMessageKind.DualGroup -> dualText()
            MuscleLoadingMessageKind.Balanced -> balancedText()
            MuscleLoadingMessageKind.Mixed -> mixedText()
            MuscleLoadingMessageKind.Empty -> emptyText()
        }

        return base + suffixText()
    }

    override fun onBack() {
        navigateTo(TrainingCompletedDirection.Back)
    }
}
