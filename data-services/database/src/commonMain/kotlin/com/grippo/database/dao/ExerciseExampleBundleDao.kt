package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grippo.database.entity.ExerciseExampleBundleEntity

@Dao
public interface ExerciseExampleBundleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdate(bundle: ExerciseExampleBundleEntity)

    @Query("DELETE FROM exercise_example_bundle")
    public suspend fun delete()
}