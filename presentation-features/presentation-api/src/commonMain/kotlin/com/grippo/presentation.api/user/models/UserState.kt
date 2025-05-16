package com.grippo.presentation.api.user.models

import kotlinx.datetime.LocalDateTime

public data class UserState(
    val id: String,
    val name: String,
    val height: Int,
    val weight: Float,
    val createdAt: LocalDateTime,
    val records: Int,
    val workouts: Int,
)