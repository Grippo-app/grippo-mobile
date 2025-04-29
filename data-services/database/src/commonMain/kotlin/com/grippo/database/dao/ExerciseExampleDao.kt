package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.database.entity.ExerciseEquipmentEntity
import com.grippo.database.entity.ExerciseExampleBundleEntity
import com.grippo.database.entity.ExerciseExampleEntity
import com.grippo.database.entity.ExerciseTutorialEntity
import com.grippo.database.models.ExerciseExampleFull

@Dao
public interface ExerciseExampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdateExerciseExample(example: ExerciseExampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdateExerciseExampleBundle(bundle: ExerciseExampleBundleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdateExerciseEquipment(equipment: ExerciseEquipmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdateExerciseTutorial(tutorial: ExerciseTutorialEntity)

    @Transaction
    @Query("SELECT * FROM exercise_example WHERE id = :id")
    public suspend fun getExerciseExampleById(id: String): ExerciseExampleFull

    @Transaction
    @Query("SELECT * FROM exercise_example")
    public suspend fun getExerciseExamples(): List<ExerciseExampleFull>

    @Query("DELETE FROM exercise_example")
    public suspend fun deleteTableExerciseExample()

    @Query("DELETE FROM exercise_example_bundle")
    public suspend fun deleteTableExerciseExampleBundle()

    @Query("DELETE FROM exercise_equipment")
    public suspend fun deleteTableExerciseEquipment()

    @Query("DELETE FROM exercise_tutorial")
    public suspend fun deleteTableExerciseTutorial()

    @Query("DELETE FROM exercise_example WHERE id = :id")
    public suspend fun deleteExerciseExampleById(id: String)
}