package com.grippo.core.state.profile

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

@Immutable
public data class UserState(
    val id: String,
    val name: String,
    val email: String,
    val height: HeightFormatState,
    val weight: WeightFormatState,
    val createdAt: DateTimeFormatState,
    val experience: ExperienceEnumState,
    val role: RoleEnumState,
    val stats: UserStatsState,
)

public fun stubUser(): UserState = UserState(
    id = Uuid.random().toString(),
    name = "Mark Test",
    email = "max@test.dev",
    height = HeightFormatState.of(Random.nextInt(140, 200)),
    weight = WeightFormatState.of(Random.nextInt(60, 100).toFloat()),
    createdAt = DateTimeFormatState.of(
        value = DateRangePresets.daily().from,
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy
    ),
    experience = ExperienceEnumState.ADVANCED,
    role = RoleEnumState.ADMIN,
    stats = UserStatsState(
        trainingsCount = Random.nextInt(12, 96),
        totalDuration = DurationFormatState.of(Random.nextInt(180, 4_800).minutes),
        totalVolume = VolumeFormatState.of(Random.nextInt(80, 640).toFloat()),
        totalRepetitions = RepetitionsFormatState.of(Random.nextInt(20, 100))
    )
)
