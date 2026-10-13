package com.grippo.shared

import com.grippo.analytics.authorization.AuthAnalyticsModule
import com.grippo.analytics.confirmation.ConfirmationAnalyticsModule
import com.grippo.analytics.goal.setup.suggestion.GoalSetupSuggestionAnalyticsModule
import com.grippo.analytics.error.display.ErrorDisplayAnalyticsModule
import com.grippo.analytics.exercise.example.ExerciseExampleAnalyticsModule
import com.grippo.analytics.exercise.ExerciseAnalyticsModule
import com.grippo.analytics.home.HomeAnalyticsModule
import com.grippo.analytics.date.picker.DatePickerAnalyticsModule
import com.grippo.analytics.draft.training.DraftTrainingAnalyticsModule
import com.grippo.analytics.profile.ProfileAnalyticsModule
import com.grippo.analytics.duration.picker.DurationPickerAnalyticsModule
import com.grippo.analytics.iteration.picker.IterationPickerAnalyticsModule
import com.grippo.analytics.month.picker.MonthPickerAnalyticsModule
import com.grippo.analytics.menu.picker.MenuPickerAnalyticsModule
import com.grippo.analytics.muscle.loading.details.MuscleLoadingDetailsAnalyticsModule
import com.grippo.analytics.performance.trend.details.PerformanceTrendDetailsAnalyticsModule
import com.grippo.analytics.height.picker.HeightPickerAnalyticsModule
import com.grippo.analytics.exercises.ExercisesAnalyticsModule
import com.grippo.analytics.training.TrainingAnalyticsModule
import com.grippo.analytics.trainings.TrainingsAnalyticsModule
import com.grippo.core.error.provider.impl.ErrorModule
import com.grippo.core.foundation.CoreModule
import com.grippo.data.features.api.FeatureApiModule
import com.grippo.data.features.authorization.AuthorizationFeatureModule
import com.grippo.data.features.equipment.EquipmentFeatureModule
import com.grippo.data.features.excluded.equipments.ExcludedEquipmentFeatureModule
import com.grippo.data.features.excluded.muscles.ExcludedMusclesFeatureModule
import com.grippo.data.features.exercise.examples.ExerciseExamplesFeatureModule
import com.grippo.data.features.exercise.metrics.ExerciseMetricsFeatureModule
import com.grippo.data.features.goal.GoalFeatureModule
import com.grippo.data.features.local.settings.LocalSettingsFeatureModule
import com.grippo.data.features.muscle.MusclesFeatureModule
import com.grippo.data.features.trainings.TrainingsFeatureModule
import com.grippo.data.features.user.UserFeatureModule
import com.grippo.data.features.weight.history.WeightHistoryFeatureModule
import com.grippo.design.resources.provider.impl.ResourcesProviderModule
import com.grippo.dialog.api.DialogModule
import com.grippo.services.apple.auth.AppleAuthModule
import com.grippo.services.backend.BackendModule
import com.grippo.services.database.DatabaseModule
import com.grippo.services.datastore.DataStoreModule
import com.grippo.services.google.auth.GoogleAuthModule
import com.grippo.shared.bootstrap.ApplicationBootstrap
import com.grippo.toolkit.analytics.core.AnalyticsModule
import com.grippo.toolkit.connectivity.ConnectivityModule
import com.grippo.toolkit.context.ContextModule
import com.grippo.toolkit.crashreporting.core.CrashReportingModule
import com.grippo.toolkit.http.client.HttpModule
import com.grippo.toolkit.image.loader.ImageLoaderModule
import com.grippo.toolkit.link.opener.LinkOpenerModule
import com.grippo.toolkit.local.notification.NotificationManagerModule
import com.grippo.toolkit.messaging.core.MessagingModule
import com.grippo.toolkit.permission.PermissionManagerModule
import com.grippo.toolkit.serialization.SerializationModule
import org.koin.dsl.KoinAppDeclaration
import org.koin.ksp.generated.module
import org.koin.mp.KoinPlatformTools

public object Koin {
    public fun init(
        appDeclaration: KoinAppDeclaration = {},
    ): ApplicationBootstrap {
        val application = KoinPlatformTools.defaultContext().startKoin {
            appDeclaration()
            modules(
                AnalyticsModule().module,
                AuthAnalyticsModule().module,
                ProfileAnalyticsModule().module,
                ExercisesAnalyticsModule().module,
                DurationPickerAnalyticsModule().module,
                IterationPickerAnalyticsModule().module,
                MonthPickerAnalyticsModule().module,
                MenuPickerAnalyticsModule().module,
                MuscleLoadingDetailsAnalyticsModule().module,
                HeightPickerAnalyticsModule().module,
                PerformanceTrendDetailsAnalyticsModule().module,
                TrainingAnalyticsModule().module,
                TrainingsAnalyticsModule().module,
                HomeAnalyticsModule().module,
                DatePickerAnalyticsModule().module,
                DraftTrainingAnalyticsModule().module,
                ConfirmationAnalyticsModule().module,
                GoalSetupSuggestionAnalyticsModule().module,
                ErrorDisplayAnalyticsModule().module,
                ExerciseExampleAnalyticsModule().module,
                ExerciseAnalyticsModule().module,
                CrashReportingModule().module,
                MessagingModule().module,
                ContextModule().module,
                DatabaseModule().module,
                DataStoreModule().module,
                BackendModule().module,
                GoogleAuthModule().module,
                AppleAuthModule().module,
                CoreModule().module,
                DialogModule().module,
                AuthorizationFeatureModule().module,
                ErrorModule().module,
                UserFeatureModule().module,
                WeightHistoryFeatureModule().module,
                GoalFeatureModule().module,
                MusclesFeatureModule().module,
                EquipmentFeatureModule().module,
                ExerciseExamplesFeatureModule().module,
                ExcludedMusclesFeatureModule().module,
                TrainingsFeatureModule().module,
                ExcludedEquipmentFeatureModule().module,
                FeatureApiModule().module,
                ConnectivityModule().module,
                LinkOpenerModule().module,
                NotificationManagerModule().module,
                PermissionManagerModule().module,
                ResourcesProviderModule().module,
                SerializationModule().module,
                ExerciseMetricsFeatureModule().module,
                LocalSettingsFeatureModule().module,
                HttpModule().module,
                ImageLoaderModule().module,
            )
        }
        return ApplicationBootstrap(
            analytics = application.koin.get(),
            crashReporting = application.koin.get(),
            messaging = application.koin.get(),
        )
    }
}
