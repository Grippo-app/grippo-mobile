package com.grippo.analytics.training.models

public sealed interface TrainingEvent {
    public sealed interface Recording : TrainingEvent {
        public data object AddExercise : Recording
        public data object EditExercise : Recording
        public data object DeleteExercise : Recording
        public data object ReorderExercises : Recording
        public data object Save : Recording
        public data object DismissRequested : Recording
        public data object StartOpened : Recording
        public data object StartConfirmed : Recording
        public data object LibrarySelected : Recording
        public data object ExerciseUpdated : Recording
        public data object UnfinishedOpened : Recording
        public data object UnfinishedConfirmed : Recording
        public data object CompletionOpened : Recording
        public data object CompletionConfirmed : Recording
        public data object DiscardOpened : Recording
        public data object DiscardConfirmed : Recording
    }
    public sealed interface Exercise : TrainingEvent {
        public data object AddIteration : Exercise
        public data object EditVolume : Exercise
        public data object EditRepetitions : Exercise
        public data object DeleteIteration : Exercise
        public data object ReorderIterations : Exercise
        public data object ReorderFinished : Exercise
        public data object ExampleOpened : Exercise
        public data object ReplaceExample : Exercise
        public data object ExampleSelected : Exercise
        public data object Save : Exercise
        public data object DismissRequested : Exercise
        public data object IterationOpened : Exercise
        public data object IterationConfirmed : Exercise
    }
    public sealed interface CompletionConfirmation : TrainingEvent {
        public data object DurationOpened : CompletionConfirmation
        public data object DurationSelected : CompletionConfirmation
        public data object ConfirmClicked : CompletionConfirmation
        public data object DismissRequested : CompletionConfirmation
    }
    public sealed interface Completed : TrainingEvent {
        public data object SummaryOpened : Completed
        public data object ExerciseOpened : Completed
        public data object DismissRequested : Completed
    }
}
