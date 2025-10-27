package com.grippo.debug

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.toolkit.logger.AppLogger
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap

public class DebugViewModel(
    private val trainingFeature: TrainingFeature,
    private val exampleFeature: ExerciseExampleFeature,
) : BaseViewModel<DebugState, DebugDirection, DebugLoader>(DebugState()),
    DebugContract {

    init {
        loadLogs()
    }

    override fun onBack() {
        navigateTo(DebugDirection.Back)
    }

    override fun clearLogs() {
        safeLaunch {
            AppLogger.clearLogFile()
            loadLogs()
        }
    }

    override fun generateTraining() {
        safeLaunch(loader = DebugLoader.GenerateTraining) {
//            SetTraining()
        }
    }

    override fun onSelect(value: DebugMenu) {
        update { it.copy(selected = value) }
        if (value == DebugMenu.Logger) {
            loadLogs()
        }
    }

    override fun onSelectLogCategory(value: String) {
        update { state ->
            val logger = state.logger
            if (logger.selectedCategory == value) state
            else if (!logger.logsByCategory.containsKey(value)) state
            else state.copy(logger = logger.copy(selectedCategory = value))
        }
    }

    private fun loadLogs() {
        safeLaunch(loader = DebugLoader.Logs) {

            val logsByCategory = AppLogger.logFileContentsByCategory()

            update { state ->
                val categories = logsByCategory.keys.toPersistentList()
                val persistentLogs = logsByCategory
                    .mapValues { (_, messages) -> messages.asReversed().toPersistentList() }
                    .toPersistentMap()

                val selectedCategory = state.logger.selectedCategory
                    ?.takeIf { persistentLogs.containsKey(it) }
                    ?: categories.firstOrNull()

                state.copy(
                    logger = state.logger.copy(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        logsByCategory = persistentLogs
                    )
                )
            }
        }
    }
}
