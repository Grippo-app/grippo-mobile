package com.grippo.presentation.api.profile.models

import com.grippo.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDateTime
import kotlin.random.Random
import kotlin.uuid.Uuid

public data class UserState(
    val id: String,
    val name: String,
    val height: Int,
    val weight: Float,
    val createdAt: LocalDateTime,
    val records: Int,
    val workouts: Int,
    val experience: ExperienceEnumState
)

public fun stubUser(): UserState = UserState(
    id = Uuid.random().toString(),
    name = "Mark Test",
    height = Random.nextInt(140, 200),
    weight = Random.nextInt(60, 100).toFloat(),
    createdAt = DateTimeUtils.thisDay(),
    records = Random.nextInt(0, 200),
    workouts = Random.nextInt(0, 200),
    experience = ExperienceEnumState.INTERMEDIATE
)