plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("ios.cocoapods.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.decompose.core)
        api(libs.decompose.extensions)
        api(libs.decompose.back.handler)
        api(libs.decompose.state.keeper)

        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.resources.providerImpl)
        implementation(projects.designSystem.components)

        implementation(projects.common.platformCore)
        implementation(projects.common.core)
        implementation(projects.common.state)
        implementation(projects.common.logger)
        implementation(projects.common.connectivity)
        implementation(projects.common.serialization)
        implementation(projects.common.dateUtils)
        implementation(projects.common.error.errorProviderImpl)

        implementation(projects.dataServices.database)
        implementation(projects.dataServices.network)

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
        implementation(projects.dataFeatures.suggestions)

        implementation(projects.dataMappers.domainToState)

        implementation(projects.uiScreenFeatures.screenApi)
        implementation(projects.uiScreenFeatures.authorization)
        implementation(projects.uiScreenFeatures.home)
        implementation(projects.uiScreenFeatures.profile)
        implementation(projects.uiScreenFeatures.debug)
        implementation(projects.uiScreenFeatures.training)

        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.uiDialogFeatures.weightPicker)
        implementation(projects.uiDialogFeatures.heightPicker)
        implementation(projects.uiDialogFeatures.datePicker)
        implementation(projects.uiDialogFeatures.periodPicker)
        implementation(projects.uiDialogFeatures.draftTraining)
        implementation(projects.uiDialogFeatures.errorDisplay)
        implementation(projects.uiDialogFeatures.confirmation)
        implementation(projects.uiDialogFeatures.exerciseExample)
        implementation(projects.uiDialogFeatures.exercise)
        implementation(projects.uiDialogFeatures.iterationPicker)
        implementation(projects.uiDialogFeatures.filterPicker)
        implementation(projects.uiDialogFeatures.exerciseExamplePicker)
        implementation(projects.uiDialogFeatures.menuPicker)

        implementation(libs.datetime)
        implementation(libs.immutable.collections)

        implementation(compose.ui)
        implementation(compose.material3)
        implementation(compose.foundation)
    }
}
