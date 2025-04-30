package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.database.entity.ExerciseExampleEntity
import com.grippo.database.models.ExerciseExampleFull
import kotlinx.coroutines.flow.Flow

@Dao
public interface ExerciseExampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdate(example: ExerciseExampleEntity)

    @Transaction
    @Query("SELECT * FROM exercise_example WHERE id = :id")
    public fun getById(id: String): Flow<ExerciseExampleFull>

    @Transaction
    @Query("SELECT * FROM exercise_example")
    public fun getAll(): Flow<List<ExerciseExampleFull>>

    @Query("DELETE FROM exercise_example WHERE id = :id")
    public suspend fun deleteById(id: String)

    @Query("DELETE FROM exercise_example")
    public suspend fun delete()
}