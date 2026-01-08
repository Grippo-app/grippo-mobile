package com.grippo.services.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.grippo.services.database.entity.MuscleEntity
import com.grippo.services.database.entity.MuscleGroupEntity
import com.grippo.services.database.models.MuscleGroupWithMuscles
import kotlinx.coroutines.flow.Flow

@Dao
public interface MuscleDao {

    @Transaction
    @Query("SELECT * FROM muscle_group")
    public fun get(): Flow<List<MuscleGroupWithMuscles>>

    // ────────────── INSERT ──────────────

    @Transaction
    public suspend fun insertOrUpdate(
        groups: List<MuscleGroupEntity>,
        muscles: List<MuscleEntity>
    ) {
        deleteMissingMuscleGroups(groups.map { it.id })
        deleteMissingMuscles(muscles.map { it.id })

        updateExistingMuscleGroups(groups)
        updateExistingMuscles(muscles)

        insertMuscleGroups(groups)
        insertMuscles(muscles)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public suspend fun insertMuscleGroups(groups: List<MuscleGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public suspend fun insertMuscles(muscles: List<MuscleEntity>)

    @Update
    public suspend fun updateExistingMuscleGroups(groups: List<MuscleGroupEntity>)

    @Update
    public suspend fun updateExistingMuscles(muscles: List<MuscleEntity>)

    // ────────────── DELETE ──────────────

    @Query("DELETE FROM muscle_group WHERE id NOT IN (:ids)")
    public suspend fun deleteMissingMuscleGroups(ids: List<String>)

    @Query("DELETE FROM muscle WHERE id NOT IN (:ids)")
    public suspend fun deleteMissingMuscles(ids: List<String>)

    @Query("DELETE FROM muscle_group")
    public suspend fun delete()
}