package com.grippo.analytics.trainings.internal.schema

import com.grippo.analytics.trainings.internal.events.TrainingsPayloadEvent
import com.grippo.analytics.trainings.models.TrainingsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema

internal object TrainingsEventMatrix {
    private val TrainingMenuClicked = AnalyticsEventSchema("trainings_training_menu_clicked", 1, emptyList())
    private val DeleteSelected = AnalyticsEventSchema("trainings_delete_selected", 1, emptyList())
    private val EditSelected = AnalyticsEventSchema("trainings_edit_selected", 1, emptyList())
    private val DetailsSelected = AnalyticsEventSchema("trainings_details_selected", 1, emptyList())
    private val ExerciseClicked = AnalyticsEventSchema("trainings_exercise_clicked", 1, emptyList())
    private val DigestClicked = AnalyticsEventSchema("trainings_digest_clicked", 1, emptyList())
    private val DailySelected = AnalyticsEventSchema("trainings_daily_selected", 1, emptyList())
    private val DatePickerClicked = AnalyticsEventSchema("trainings_date_picker_clicked", 1, emptyList())
    private val DateConfirmed = AnalyticsEventSchema("trainings_date_confirmed", 1, emptyList())
    private val MonthPickerClicked = AnalyticsEventSchema("trainings_month_picker_clicked", 1, emptyList())
    private val MonthConfirmed = AnalyticsEventSchema("trainings_month_confirmed", 1, emptyList())
    private val DailyPeriodSelected = AnalyticsEventSchema("trainings_daily_period_selected", 1, emptyList())
    private val MonthlyPeriodSelected = AnalyticsEventSchema("trainings_monthly_period_selected", 1, emptyList())
    private val NextDateSelected = AnalyticsEventSchema("trainings_next_date_selected", 1, emptyList())
    private val PreviousDateSelected = AnalyticsEventSchema("trainings_previous_date_selected", 1, emptyList())
    private val Back = AnalyticsEventSchema("trainings_back", 1, emptyList())

    val all = listOf(
        TrainingMenuClicked,
        DeleteSelected,
        EditSelected,
        DetailsSelected,
        ExerciseClicked,
        DigestClicked,
        DailySelected,
        DatePickerClicked,
        DateConfirmed,
        MonthPickerClicked,
        MonthConfirmed,
        DailyPeriodSelected,
        MonthlyPeriodSelected,
        NextDateSelected,
        PreviousDateSelected,
        Back,
    )

    fun event(event: TrainingsEvent): AnalyticsEvent = TrainingsPayloadEvent(
        when (event) {
            TrainingsEvent.TrainingMenuClicked -> TrainingMenuClicked
            TrainingsEvent.DeleteSelected -> DeleteSelected
            TrainingsEvent.EditSelected -> EditSelected
            TrainingsEvent.DetailsSelected -> DetailsSelected
            TrainingsEvent.ExerciseClicked -> ExerciseClicked
            TrainingsEvent.DigestClicked -> DigestClicked
            TrainingsEvent.DailySelected -> DailySelected
            TrainingsEvent.DatePickerClicked -> DatePickerClicked
            TrainingsEvent.DateConfirmed -> DateConfirmed
            TrainingsEvent.MonthPickerClicked -> MonthPickerClicked
            TrainingsEvent.MonthConfirmed -> MonthConfirmed
            TrainingsEvent.DailyPeriodSelected -> DailyPeriodSelected
            TrainingsEvent.MonthlyPeriodSelected -> MonthlyPeriodSelected
            TrainingsEvent.NextDateSelected -> NextDateSelected
            TrainingsEvent.PreviousDateSelected -> PreviousDateSelected
            TrainingsEvent.Back -> Back
        },
        emptyList(),
    )
}
