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
        implementation(projects.designSystem.resources)
        implementation(projects.designSystem.components)

        implementation(projects.common.platformCore)
        implementation(projects.common.core)
        implementation(projects.common.logger)
        implementation(projects.common.connectivity)
        implementation(projects.common.error.errorProviderImpl)

        implementation(projects.dataServices.database)
        implementation(projects.dataServices.network)

        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataFeatures.authorization)
        implementation(projects.dataFeatures.user)
        implementation(projects.dataFeatures.settings)
        implementation(projects.dataFeatures.weightHistory)
        implementation(projects.dataFeatures.muscle)
        implementation(projects.dataFeatures.equipment)
        implementation(projects.dataFeatures.excludedMuscles)
        implementation(projects.dataFeatures.excludedEquipments)
        implementation(projects.dataFeatures.trainings)
        implementation(projects.dataFeatures.exerciseExamples)

        implementation(projects.dataMappers.domainMapper)

        implementation(projects.presentationFeatures.presentationApi)
        implementation(projects.presentationFeatures.authorization)
        implementation(projects.presentationFeatures.home)
        implementation(projects.presentationFeatures.profile)
        implementation(projects.presentationFeatures.settings)
        implementation(projects.presentationFeatures.debug)

        implementation(projects.dialogFeatures.dialogApi)
        implementation(projects.dialogFeatures.weightPicker)
        implementation(projects.dialogFeatures.heightPicker)
        implementation(projects.dialogFeatures.datePicker)
        implementation(projects.dialogFeatures.errorDisplay)
        implementation(projects.dialogFeatures.exerciseExample)
        implementation(projects.dialogFeatures.exercise)

        implementation(libs.datetime)
        implementation(libs.immutable.collections)

        implementation(compose.ui)
        implementation(compose.material3)
        implementation(compose.foundation)
    }
}