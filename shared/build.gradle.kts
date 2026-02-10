plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("ios.swiftpackage.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android {
        namespace = "com.grippo.shared"
    }

    sourceSets.commonMain.dependencies {
        api(libs.decompose.core)
        api(libs.decompose.extensions)
        api(libs.decompose.back.handler)
        api(libs.decompose.state.keeper)

        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.resources.providerImpl)
        implementation(projects.designSystem.components)

        implementation(projects.toolkit.context)
        implementation(projects.toolkit.localization)
        implementation(projects.toolkit.theme)
        implementation(projects.toolkit.httpClient)
        implementation(projects.toolkit.logger)
        implementation(projects.toolkit.connectivity)
        implementation(projects.toolkit.serialization)
        implementation(projects.toolkit.dateUtils)
        implementation(projects.toolkit.imageLoader)

        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.uiCore.error.errorProviderImpl)

        implementation(projects.dataServices.database)
        implementation(projects.dataServices.backend)
        implementation(projects.dataServices.googleAuth)
        implementation(projects.dataServices.appleAuth)
        api(projects.dataServices.firebase)

        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataFeatures.authorization)
        implementation(projects.dataFeatures.user)
        implementation(projects.dataFeatures.weightHistory)
        implementation(projects.dataFeatures.muscle)
        implementation(projects.dataFeatures.equipment)
        implementation(projects.dataFeatures.excludedMuscles)
        implementation(projects.dataFeatures.excludedEquipments)
        implementation(projects.dataFeatures.trainings)
        implementation(projects.dataFeatures.exerciseExamples)
        implementation(projects.dataFeatures.exerciseMetrics)

        implementation(projects.dataMappers.domainToState)

        implementation(projects.uiScreenFeatures.screenApi)
        implementation(projects.uiScreenFeatures.authorization)
        implementation(projects.uiScreenFeatures.trainings)
        implementation(projects.uiScreenFeatures.profile)
        implementation(projects.uiScreenFeatures.debug)
        implementation(projects.uiScreenFeatures.training)
        implementation(projects.uiScreenFeatures.home)

        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.uiDialogFeatures.weightPicker)
        implementation(projects.uiDialogFeatures.heightPicker)
        implementation(projects.uiDialogFeatures.datePicker)
        implementation(projects.uiDialogFeatures.monthPicker)
        implementation(projects.uiDialogFeatures.draftTraining)
        implementation(projects.uiDialogFeatures.profile)
        implementation(projects.uiDialogFeatures.errorDisplay)
        implementation(projects.uiDialogFeatures.confirmation)
        implementation(projects.uiDialogFeatures.confirmTrainingCompletion)
        implementation(projects.uiDialogFeatures.exerciseExample)
        implementation(projects.uiDialogFeatures.exercise)
        implementation(projects.uiDialogFeatures.iterationPicker)
        implementation(projects.uiDialogFeatures.exerciseExamplePicker)
        implementation(projects.uiDialogFeatures.menuPicker)
        implementation(projects.uiDialogFeatures.statistics)
        implementation(projects.uiDialogFeatures.muscleLoading)
        implementation(projects.uiDialogFeatures.trainingStreak)
        implementation(projects.uiDialogFeatures.trainingProfile)
        implementation(projects.uiDialogFeatures.performanceTrend)
        implementation(projects.uiDialogFeatures.periodPicker)

        implementation(libs.datetime)
        implementation(libs.immutable.collections)

        implementation(compose.ui)
        implementation(compose.material3)
        implementation(compose.foundation)
    }
}
