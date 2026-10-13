package com.grippo.authorization.profile.creation.excluded.muscles

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.models.PersistenceKey
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import kotlinx.collections.immutable.ImmutableList

internal class ExcludedMusclesComponent(
    componentContext: ComponentContext,
    excludedMuscleIds: ImmutableList<String>,
    private val toMissingEquipment: (excludedMuscleIds: List<String>) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<ExcludedMusclesDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ExcludedMusclesViewModel(
            analytics = getKoin().get(),
            muscleFeature = getKoin().get(),
            initialState = restoreState(DRAFT)
                ?: ExcludedMusclesState(knownMuscleIds = excludedMuscleIds),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        saveState(DRAFT) { viewModel.state.value }
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ExcludedMusclesDirection) {
        when (direction) {
            is ExcludedMusclesDirection.MissingEquipment -> toMissingEquipment.invoke(direction.excludedMuscleIds)
            ExcludedMusclesDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExcludedMusclesScreen(state.value, loaders.value, viewModel)
    }

    private companion object {
        val DRAFT = PersistenceKey("ExcludedMusclesDraft", ExcludedMusclesState.serializer())
    }
}
