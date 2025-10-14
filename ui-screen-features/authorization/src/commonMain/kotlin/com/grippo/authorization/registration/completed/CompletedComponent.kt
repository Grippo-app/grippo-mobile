package com.grippo.authorization.registration.completed

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.profile.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList

internal class CompletedComponent(
    componentContext: ComponentContext,
    private val email: String,
    private val password: String,
    private val name: String,
    private val weight: Float,
    private val height: Int,
    private val experience: ExperienceEnumState?,
    private val excludedMuscleIds: ImmutableList<String>,
    private val missingEquipmentIds: ImmutableList<String>,
    private val toHome: () -> Unit,
    private val back: () -> Unit,
) : BaseComponent<CompletedDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        CompletedViewModel(
            email = email,
            password = password,
            name = name,
            weight = weight,
            height = height,
            experience = experience,
            excludedMuscleIds = excludedMuscleIds,
            missingEquipmentIds = missingEquipmentIds,
            registerUseCase = getKoin().get(),
            userFeature = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: CompletedDirection) {
        when (direction) {
            CompletedDirection.Home -> toHome.invoke()
            CompletedDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        CompletedScreen(state.value, loaders.value, viewModel)
    }
}