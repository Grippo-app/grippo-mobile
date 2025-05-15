package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("token")
public data class TokenEntity(
    @PrimaryKey val id: Int = 0,
    val access: String? = null,
    val refresh: String? = null,
)