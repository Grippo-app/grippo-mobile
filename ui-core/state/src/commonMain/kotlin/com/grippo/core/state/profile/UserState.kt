package com.grippo.core.state.profile

import androidx.compose.runtime.Immutable
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDateTime
import kotlin.random.Random
import kotlin.uuid.Uuid

@Immutable
public data class UserState(
    val id: String,
    val name: String,
    val height: Int,
    val weight: Float,
    val createdAt: LocalDateTime,
    val experience: ExperienceEnumState
)

public fun stubUser(): UserState = UserState(
    id = Uuid.random().toString(),
    name = "Mark Test",
    height = Random.nextInt(140, 200),
    weight = Random.nextInt(60, 100).toFloat(),
    createdAt = DateTimeUtils.thisDay().from,
    experience = ExperienceEnumState.INTERMEDIATE
)