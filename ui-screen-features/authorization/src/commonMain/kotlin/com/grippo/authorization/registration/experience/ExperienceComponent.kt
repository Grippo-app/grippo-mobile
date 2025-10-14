package com.grippo.authorization.registration.experience

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.profile.ExperienceEnumState

internal class ExperienceComponent(
    componentContext: ComponentContext,
    private val toExcludedMuscles: (experience: ExperienceEnumState) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<ExperienceDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ExperienceViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ExperienceDirection) {
        when (direction) {
            is ExperienceDirection.ExcludedMuscles -> toExcludedMuscles.invoke(direction.experience)
            ExperienceDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExperienceScreen(state.value, loaders.value, viewModel)
    }
}