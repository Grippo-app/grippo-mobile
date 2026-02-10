package com.grippo.duration.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import kotlin.time.Duration

public class DurationPickerComponent(
    componentContext: ComponentContext,
    private val initial: Duration,
    private val onResult: (value: Duration) -> Unit,
    private val back: () -> Unit
) : BaseComponent<DurationPickerDirection>(componentContext) {

    override val viewModel: DurationPickerViewModel = componentContext.retainedInstance {
        DurationPickerViewModel(initial)
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: DurationPickerDirection) {
        when (direction) {
            is DurationPickerDirection.BackWithResult -> onResult.invoke(direction.value)
            DurationPickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        DurationPickerScreen(state.value, loaders.value, viewModel)
    }
}
