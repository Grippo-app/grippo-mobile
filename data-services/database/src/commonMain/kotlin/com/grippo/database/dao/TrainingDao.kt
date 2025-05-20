package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.database.entity.ExerciseEntity
import com.grippo.database.entity.IterationEntity
import com.grippo.database.entity.TrainingEntity
import com.grippo.database.models.TrainingFull
import kotlinx.coroutines.flow.Flow

@Dao
public interface TrainingDao {

    @Transaction
    public suspend fun insertOrUpdateTrainingFull(trainingFull: TrainingFull) {
        insertOrUpdateTraining(trainingFull.training)

        for (exerciseFull in trainingFull.exercises) {
            insertOrUpdateExercise(exerciseFull.exercise)

            for (iteration in exerciseFull.iterations) {
                insertOrUpdateIteration(iteration)
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdateTraining(training: TrainingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdateExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdateIteration(iteration: IterationEntity)

    @Transaction
    @Query(
        """
        SELECT * FROM training
        WHERE createdAt BETWEEN :from AND :to
        ORDER BY id DESC
        """
    )
    public fun getTrainings(from: String, to: String): Flow<List<TrainingFull>>

    @Transaction
    @Query(
        """
        SELECT * FROM training
        WHERE id = :id
        LIMIT 1
        """
    )
    public fun getTrainingById(id: String): Flow<TrainingFull?>

    @Query("DELETE FROM training")
    public suspend fun deleteTableTraining()

    @Query("DELETE FROM training WHERE id = :id")
    public suspend fun deleteTrainingById(id: String)
}