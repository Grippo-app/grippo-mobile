package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grippo.database.entity.ExerciseEquipmentEntity

@Dao
public interface ExerciseEquipmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdate(equipment: ExerciseEquipmentEntity)

    @Query("DELETE FROM exercise_equipment")
    public suspend fun delete()
}