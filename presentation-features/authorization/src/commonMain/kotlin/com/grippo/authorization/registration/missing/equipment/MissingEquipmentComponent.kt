package com.grippo.authorization.registration.missing.equipment

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.presentation.api.user.models.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList

internal class MissingEquipmentComponent(
    componentContext: ComponentContext,
    val email: String,
    val password: String,
    val name: String,
    val weight: Float,
    val height: Int,
    val experience: ExperienceEnumState?,
    val excludedMuscleIds: ImmutableList<String>,
    private val toCompleted: () -> Unit
) : BaseComponent<MissingEquipmentDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        MissingEquipmentViewModel(
            email = email,
            password = password,
            name = name,
            weight = weight,
            height = height,
            experience = experience,
            excludedMuscleIds = excludedMuscleIds,
            equipmentFeature = getKoin().get(),
            authorizationFeature = getKoin().get()
        )
    }

    override suspend fun eventListener(direction: MissingEquipmentDirection) {
        when (direction) {
            MissingEquipmentDirection.Completed -> toCompleted.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        MissingEquipmentScreen(state.value, loaders.value, viewModel)
    }
}