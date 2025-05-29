package com.grippo.presentation.api.exercise.example.models

import androidx.compose.runtime.Immutable
import kotlin.uuid.Uuid

@Immutable
public data class TutorialState(
    val id: String,
    val title: String,
    val value: String,
    val language: String,
    val author: String?,
    val resourceType: ResourceTypeEnumState,
)

public fun stubTutorial(): TutorialState {
    return TutorialState(
        id = Uuid.random().toString(),
        title = "Tutorial to help you",
        value = "https://google.com",
        language = "UA",
        author = "Mark Jones",
        resourceType = ResourceTypeEnumState.VIDEO
    )
}
