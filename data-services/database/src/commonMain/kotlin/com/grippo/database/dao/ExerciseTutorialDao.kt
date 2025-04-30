package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grippo.database.entity.ExerciseTutorialEntity

@Dao
public interface ExerciseTutorialDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdate(tutorial: ExerciseTutorialEntity)

    @Query("DELETE FROM exercise_tutorial")
    public suspend fun delete()
}