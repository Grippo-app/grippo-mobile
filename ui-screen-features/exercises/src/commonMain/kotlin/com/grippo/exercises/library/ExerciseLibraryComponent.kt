package com.grippo.exercises.library

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.models.PersistenceKey
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.screen.api.ExercisesRouter

internal class ExerciseLibraryComponent(
    componentContext: ComponentContext,
    private val mode: ExercisesRouter.Mode,
    private val back: () -> Unit,
) : BaseComponent<ExerciseLibraryDirection>(componentContext) {

    override val viewModel: ExerciseLibraryViewModel = componentContext.retainedInstance {
        ExerciseLibraryViewModel(
            mode = mode,
            analytics = getKoin().get(),
            userExerciseExamplesUseCase = getKoin().get(),
            exerciseExampleFeature = getKoin().get(),
            muscleFeature = getKoin().get(),
            dialogController = getKoin().get(),
            initialState = restoreState(DRAFT),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        saveState(DRAFT) { viewModel.state.value }
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ExerciseLibraryDirection) {
        when (direction) {
            is ExerciseLibraryDirection.BackWithResult -> {
                sendResult(key = direction.resultKey, data = direction.value)
                back.invoke()
            }
            ExerciseLibraryDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        ExerciseLibraryScreen(state.value, viewModel)
    }

    private companion object {
        val DRAFT = PersistenceKey("ExerciseLibraryDraft", ExerciseLibraryState.serializer())
    }
}
