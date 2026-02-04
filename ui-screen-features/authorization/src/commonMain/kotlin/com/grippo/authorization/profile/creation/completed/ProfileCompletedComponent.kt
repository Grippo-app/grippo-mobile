package com.grippo.authorization.profile.creation.completed

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.profile.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList

internal class ProfileCompletedComponent(
    componentContext: ComponentContext,
    private val name: String,
    private val weight: Float,
    private val height: Int,
    private val experience: ExperienceEnumState?,
    private val excludedMuscleIds: ImmutableList<String>,
    private val missingEquipmentIds: ImmutableList<String>,
    private val toHome: () -> Unit,
    private val back: () -> Unit,
) : BaseComponent<ProfileCompletedDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileCompletedViewModel(
            name = name,
            weight = weight,
            height = height,
            experience = experience,
            excludedMuscleIds = excludedMuscleIds,
            missingEquipmentIds = missingEquipmentIds,
            userFeature = getKoin().get(),
            createProfileUseCase = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ProfileCompletedDirection) {
        when (direction) {
            ProfileCompletedDirection.Home -> toHome.invoke()
            ProfileCompletedDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileCompletedScreen(state.value, loaders.value, viewModel)
    }
}
