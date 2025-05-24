package com.grippo.presentation.api.exercise.example.models

import androidx.compose.runtime.Immutable

@Immutable
public data class TutorialState(
    val id: String,
    val title: String,
    val value: String,
    val language: String,
    val author: String?,
    val resourceType: ResourceTypeEnumState,
)
