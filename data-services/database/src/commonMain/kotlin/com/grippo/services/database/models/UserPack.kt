package com.grippo.services.database.models

import androidx.room.Embedded
import androidx.room.Relation
import com.grippo.services.database.entity.UserEntity
import com.grippo.services.database.entity.UserStatsEntity

public data class UserPack(
    @Embedded val user: UserEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "userId"
    )
    val stats: UserStatsEntity? = null
)
