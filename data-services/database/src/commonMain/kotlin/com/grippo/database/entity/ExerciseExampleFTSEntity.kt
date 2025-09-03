package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "exercise_example_fts")
@Fts4
public data class ExerciseExampleFtsEntity(
    val id: String,
    val name: String,
    val description: String
)