package com.grippo.analytics.trainings.models

public sealed interface TrainingsEvent {
    public data object TrainingMenuClicked : TrainingsEvent
    public data object DeleteSelected : TrainingsEvent
    public data object EditSelected : TrainingsEvent
    public data object DetailsSelected : TrainingsEvent
    public data object ExerciseClicked : TrainingsEvent
    public data object DigestClicked : TrainingsEvent
    public data object DailySelected : TrainingsEvent
    public data object DatePickerClicked : TrainingsEvent
    public data object DateConfirmed : TrainingsEvent
    public data object MonthPickerClicked : TrainingsEvent
    public data object MonthConfirmed : TrainingsEvent
    public data object DailyPeriodSelected : TrainingsEvent
    public data object MonthlyPeriodSelected : TrainingsEvent
    public data object NextDateSelected : TrainingsEvent
    public data object PreviousDateSelected : TrainingsEvent
    public data object DismissRequested : TrainingsEvent
}
