package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.database.entity.MuscleEntity
import com.grippo.database.entity.MuscleGroupEntity
import com.grippo.database.models.MuscleGroupWithMuscles
import kotlinx.coroutines.flow.Flow

@Dao
public interface MuscleDao {

    @Transaction
    @Query("SELECT * FROM muscle_group")
    public fun get(): Flow<List<MuscleGroupWithMuscles>>

    // ────────────── INSERT ──────────────

    @Transaction
    public suspend fun insertOrReplace(
        groups: List<MuscleGroupEntity>,
        muscles: List<MuscleEntity>
    ) {
        insertMuscleGroups(groups)
        insertMuscles(muscles)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertMuscles(muscles: List<MuscleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertMuscleGroups(muscleGroups: List<MuscleGroupEntity>)

    // ────────────── DELETE ──────────────

    @Query("DELETE FROM muscle_group")
    public suspend fun delete()
}