package com.grippo.domain.mapper.exercise.example

import com.grippo.data.features.api.exercise.example.models.Tutorial
import com.grippo.state.exercise.examples.TutorialState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<Tutorial>.toState(): ImmutableList<TutorialState> {
    return map { it.toState() }.toPersistentList()
}

public fun Tutorial.toState(): TutorialState {
    return TutorialState(
        id = id,
        title = title,
        value = value,
        language = language,
        author = author,
        resourceType = resourceType.toState()
    )
}