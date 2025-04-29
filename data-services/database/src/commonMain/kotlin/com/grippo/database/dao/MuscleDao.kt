package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.database.entity.MuscleEntity
import com.grippo.database.entity.MuscleGroupEntity
import com.grippo.database.models.MuscleGroupWithMuscles

@Dao
public interface MuscleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdateMuscle(muscle: MuscleEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public suspend fun insertOrIgnoreMuscleGroup(muscleGroup: MuscleGroupEntity)

    @Transaction
    @Query("SELECT * FROM muscle WHERE id IN (:ids)")
    public suspend fun getMusclesById(ids: List<String>): List<MuscleEntity>

    @Transaction
    @Query("SELECT * FROM muscle_group")
    public suspend fun getMuscleGroups(): List<MuscleGroupWithMuscles>

    @Query("DELETE FROM muscle")
    public suspend fun deleteTableMuscle()

    @Query("DELETE FROM muscle_group")
    public suspend fun deleteTableMuscleGroup()
}