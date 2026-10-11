package com.grippo.exercises.library

import androidx.compose.runtime.Immutable

@Immutable
internal interface ExerciseLibraryContract {
    fun onExerciseExampleSelectClick(id: String)
    fun onSuggestionsClick()
    fun onMuscleGroupClick(id: String)
    fun onQueryChange(value: String)
    fun onLoadNextPage()
    fun onBack()

    @Immutable
    companion object Empty : ExerciseLibraryContract {
        override fun onExerciseExampleSelectClick(id: String) {}
        override fun onSuggestionsClick() {}
        override fun onMuscleGroupClick(id: String) {}
        override fun onQueryChange(value: String) {}
        override fun onLoadNextPage() {}
        override fun onBack() {}
    }
}
