package com.grippo.primary.goal.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState

public class PrimaryGoalPickerComponent(
    componentContext: ComponentContext,
    private val title: String,
    private val initial: GoalPrimaryGoalEnumState?,
    private val onResult: (value: GoalPrimaryGoalEnumState) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<PrimaryGoalPickerDirection>(componentContext) {

    override val viewModel: PrimaryGoalPickerViewModel = componentContext.retainedInstance {
        PrimaryGoalPickerViewModel(
            initial = initial,
            title = title,
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: PrimaryGoalPickerDirection) {
        when (direction) {
            is PrimaryGoalPickerDirection.BackWithResult -> onResult.invoke(direction.value)
            PrimaryGoalPickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        PrimaryGoalPickerScreen(state.value, loaders.value, viewModel)
    }
}
