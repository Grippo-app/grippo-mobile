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

@Dao
public interface TrainingDao {

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
    public suspend fun getTrainings(from: String, to: String): List<TrainingFull>

    @Transaction
    @Query(
        """
        SELECT * FROM training
        WHERE id = :id
        LIMIT 1
        """
    )
    public suspend fun getTrainingById(id: String): TrainingFull?

    @Transaction
    @Query(
        """
        SELECT * FROM training
        WHERE createdAt = (SELECT MAX(createdAt) FROM Training)
        ORDER BY createdAt DESC
        LIMIT 1
        """
    )
    public suspend fun getLastTraining(): TrainingFull?

    @Query("DELETE FROM training")
    public suspend fun deleteTableTraining()

    @Query("DELETE FROM training WHERE id = :id")
    public suspend fun deleteTrainingById(id: String)
}